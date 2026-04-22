import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        Path grammarPath = Path.of(args[0]);
        Path inputPath   = Path.of(args[1]);
        Path outputDir   = args.length >= 3 ? Path.of(args[2]) : Path.of("output");

        try {
            Files.createDirectories(outputDir);
            Grammar grammar    = Grammar.fromFile(grammarPath);
            List<String> inputLines = readInputLines(inputPath);

            // ── SLR(1) ──────────────────────────────────────────────────────────────
            long slrStart = System.currentTimeMillis();
            SLRParser slrParser = new SLRParser(grammar);
            slrParser.buildCanonicalCollection();
            ParsingTable slrTable = slrParser.buildParsingTable();
            long slrBuildMs = System.currentTimeMillis() - slrStart;

            writeFile(outputDir.resolve("augmented_grammar.txt"), grammar.toFormattedGrammar());
            writeFile(outputDir.resolve("slr_items.txt"), slrParser.formatStates());
            writeFile(outputDir.resolve("slr_parsing_table.txt"),
                    slrTable.toFormattedTable(slrParser.getStates().size(), grammar.getTerminals(),
                            grammar.getNonTerminals(), grammar.getStartSymbol()));

            StringBuilder slrTrace = new StringBuilder();
            StringBuilder slrTrees = new StringBuilder();
            int slrAccepted = 0;

            long slrParseStart = System.currentTimeMillis();
            for (String line : inputLines) {
                List<String> tokens = tokenize(line);
                SLRParser.ParseResult result = slrParser.parse(tokens);
                slrTrace.append(result.formatTrace(line)).append(System.lineSeparator());
                appendTreeEntry(slrTrees, line, result);
                if (result.isAccepted()) slrAccepted++;
            }
            long slrParseMs = System.currentTimeMillis() - slrParseStart;

            writeFile(outputDir.resolve("slr_trace.txt"), slrTrace.toString());
            writeFile(outputDir.resolve("parse_trees.txt"), slrTrees.toString());

            // ── LR(1) ───────────────────────────────────────────────────────────────
            long lr1Start = System.currentTimeMillis();
            LR1Parser lr1Parser = new LR1Parser(grammar);
            lr1Parser.buildCanonicalCollection();
            ParsingTable lr1Table = lr1Parser.buildParsingTable();
            long lr1BuildMs = System.currentTimeMillis() - lr1Start;

            writeFile(outputDir.resolve("lr1_items.txt"), lr1Parser.formatStates());
            writeFile(outputDir.resolve("lr1_parsing_table.txt"),
                    lr1Table.toFormattedTable(lr1Parser.getStates().size(), grammar.getTerminals(),
                            grammar.getNonTerminals(), grammar.getStartSymbol()));

            StringBuilder lr1Trace = new StringBuilder();
            StringBuilder lr1Trees = new StringBuilder();
            int lr1Accepted = 0;

            long lr1ParseStart = System.currentTimeMillis();
            for (String line : inputLines) {
                List<String> tokens = tokenize(line);
                SLRParser.ParseResult result = lr1Parser.parse(tokens);
                lr1Trace.append(result.formatTrace(line)).append(System.lineSeparator());
                appendTreeEntry(lr1Trees, line, result);
                if (result.isAccepted()) lr1Accepted++;
            }
            long lr1ParseMs = System.currentTimeMillis() - lr1ParseStart;

            writeFile(outputDir.resolve("lr1_trace.txt"), lr1Trace.toString());
            writeFile(outputDir.resolve("lr1_parse_trees.txt"), lr1Trees.toString());

            // ── Summaries ────────────────────────────────────────────────────────────
            String slrSummary = buildSummary("SLR(1)", grammarPath, inputLines.size(),
                    slrAccepted, slrParser.getStates().size(), slrTable, slrBuildMs, slrParseMs);
            String lr1Summary = buildSummary("LR(1)", grammarPath, inputLines.size(),
                    lr1Accepted, lr1Parser.getStates().size(), lr1Table, lr1BuildMs, lr1ParseMs);
            writeFile(outputDir.resolve("slr_summary.txt"), slrSummary);
            writeFile(outputDir.resolve("lr1_summary.txt"), lr1Summary);

            // ── Comparison ───────────────────────────────────────────────────────────
            String comparison = buildComparison(grammarPath, inputLines.size(),
                    slrParser, lr1Parser, slrTable, lr1Table,
                    slrBuildMs, lr1BuildMs, slrParseMs, lr1ParseMs,
                    slrAccepted, lr1Accepted);
            writeFile(outputDir.resolve("comparison.txt"), comparison);

            printSection("AUGMENTED GRAMMAR", grammar.toFormattedGrammar());
            printSection("SLR(1) ITEMS", slrParser.formatStates());
            printSection("SLR(1) PARSING TABLE",
                    slrTable.toFormattedTable(slrParser.getStates().size(), grammar.getTerminals(),
                            grammar.getNonTerminals(), grammar.getStartSymbol()));
            printSection("SLR(1) PARSE TRACE", slrTrace.toString());
            printSection("SLR(1) PARSE TREES", slrTrees.toString());
            printSection("LR(1) ITEMS", lr1Parser.formatStates());
            printSection("LR(1) PARSING TABLE",
                    lr1Table.toFormattedTable(lr1Parser.getStates().size(), grammar.getTerminals(),
                            grammar.getNonTerminals(), grammar.getStartSymbol()));
            printSection("LR(1) PARSE TRACE", lr1Trace.toString());
            printSection("LR(1) PARSE TREES", lr1Trees.toString());
            printSection("COMPARISON", comparison);
            System.out.println("Output files written to: " + outputDir.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // ── Parse tree output with reduction steps ───────────────────────────────
    private static void appendTreeEntry(StringBuilder out, String line, SLRParser.ParseResult result) {
        String label = line.isEmpty() ? "<empty>" : line;
        out.append("Input: ").append(label).append(System.lineSeparator());

        if (result.isAccepted() && result.getParseTree() != null) {
            // Reduction steps (extract reduce actions from trace)
            out.append("Reduction steps:").append(System.lineSeparator());
            int reduceNum = 1;
            for (Items.ParsingStep step : result.getSteps()) {
                if (step.getAction().startsWith("reduce")) {
                    out.append("  ").append(reduceNum++).append(". ").append(step.getAction())
                            .append(System.lineSeparator());
                }
            }
            out.append("Parse tree:").append(System.lineSeparator());
            out.append(result.getParseTree().toPrettyString());
        } else {
            out.append("No parse tree (input rejected)").append(System.lineSeparator());
        }
        out.append(System.lineSeparator());
    }

    // ── Summary ──────────────────────────────────────────────────────────────
    private static String buildSummary(String parserName, Path grammarPath, int tested,
                                       int accepted, int stateCount, ParsingTable table,
                                       long buildMs, long parseMs) {
        StringBuilder sb = new StringBuilder();
        sb.append(parserName).append(" Parser Summary").append(System.lineSeparator());
        sb.append("Grammar loaded from: ").append(grammarPath).append(System.lineSeparator());
        sb.append("Input strings tested: ").append(tested).append(System.lineSeparator());
        sb.append("Accepted strings: ").append(accepted).append(System.lineSeparator());
        sb.append("State count: ").append(stateCount).append(System.lineSeparator());
        sb.append("Conflicts found: ").append(table.getConflicts().size()).append(System.lineSeparator());
        if (table.hasConflicts()) {
            sb.append("Grammar is NOT ").append(parserName).append(".").append(System.lineSeparator());
            sb.append("Conflicts:").append(System.lineSeparator());
            for (String c : table.getConflicts()) {
                sb.append("  - ").append(c).append(System.lineSeparator());
            }
        } else {
            sb.append("Grammar is ").append(parserName).append(".").append(System.lineSeparator());
        }
        sb.append("Table build time: ").append(buildMs).append(" ms").append(System.lineSeparator());
        sb.append("Parse time: ").append(parseMs).append(" ms").append(System.lineSeparator());
        return sb.toString();
    }

    // ── Comparison (Task 3.1 + 3.2) ──────────────────────────────────────────
    private static String buildComparison(Path grammarPath, int tested,
                                          SLRParser slrParser, LR1Parser lr1Parser,
                                          ParsingTable slrTable, ParsingTable lr1Table,
                                          long slrBuildMs, long lr1BuildMs,
                                          long slrParseMs, long lr1ParseMs,
                                          int slrAccepted, int lr1Accepted) {
        int slrStates   = slrParser.getStates().size();
        int lr1States   = lr1Parser.getStates().size();
        int slrActions  = slrTable.getActionEntryCount();
        int lr1Actions  = lr1Table.getActionEntryCount();
        int slrGotos    = slrTable.getGotoEntryCount();
        int lr1Gotos    = lr1Table.getGotoEntryCount();

        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================").append(System.lineSeparator());
        sb.append("  PART 3: PARSER COMPARISON & ANALYSIS").append(System.lineSeparator());
        sb.append("=================================================================").append(System.lineSeparator());
        sb.append("Grammar: ").append(grammarPath).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        // ── Task 3.1: Conflict Analysis ───────────────────────────────────────
        sb.append("-----------------------------------------------------------------").append(System.lineSeparator());
        sb.append("  TASK 3.1: CONFLICT ANALYSIS").append(System.lineSeparator());
        sb.append("-----------------------------------------------------------------").append(System.lineSeparator());
        sb.append("Both parsers were run on the same grammar.").append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("SLR(1) conflicts: ").append(slrTable.getConflicts().size()).append(System.lineSeparator());
        if (slrTable.hasConflicts()) {
            for (String c : slrTable.getConflicts()) {
                sb.append("  - ").append(c).append(System.lineSeparator());
            }
        } else {
            sb.append("  (none)").append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());

        sb.append("LR(1) conflicts: ").append(lr1Table.getConflicts().size()).append(System.lineSeparator());
        if (lr1Table.hasConflicts()) {
            for (String c : lr1Table.getConflicts()) {
                sb.append("  - ").append(c).append(System.lineSeparator());
            }
        } else {
            sb.append("  (none)").append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());

        if (slrTable.hasConflicts() && !lr1Table.hasConflicts()) {
            sb.append("RESULT: LR(1) resolves the SLR(1) conflict(s).").append(System.lineSeparator());
            sb.append("This grammar is LR(1) but NOT SLR(1).").append(System.lineSeparator());
            sb.append("Reason: LR(1) uses specific per-item lookaheads instead of the").append(System.lineSeparator());
            sb.append("  coarser FOLLOW sets, which avoids false conflicts.").append(System.lineSeparator());
        } else if (!slrTable.hasConflicts() && !lr1Table.hasConflicts()) {
            sb.append("RESULT: Both parsers handle this grammar without conflicts.").append(System.lineSeparator());
            sb.append("NOTE: To demonstrate LR(1) superiority, run with grammar3.txt").append(System.lineSeparator());
            sb.append("  (Start -> L = R | R  /  L -> * R | id  /  R -> L).").append(System.lineSeparator());
        } else if (slrTable.hasConflicts() && lr1Table.hasConflicts()) {
            sb.append("RESULT: Both parsers have conflicts — the grammar is ambiguous.").append(System.lineSeparator());
            sb.append("Ambiguous grammars cannot be parsed conflict-free by any LR parser.").append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());

        // ── Task 3.2: Performance Comparison ─────────────────────────────────
        sb.append("-----------------------------------------------------------------").append(System.lineSeparator());
        sb.append("  TASK 3.2: PERFORMANCE COMPARISON").append(System.lineSeparator());
        sb.append("-----------------------------------------------------------------").append(System.lineSeparator());
        sb.append(System.lineSeparator());

        String fmt = "  %-42s %-12s %-12s%n";
        sb.append(String.format(fmt, "Metric", "SLR(1)", "LR(1)"));
        sb.append("  ").append("-".repeat(68)).append(System.lineSeparator());
        sb.append(String.format(fmt, "Number of states", slrStates, lr1States));
        sb.append(String.format(fmt, "ACTION table entries (memory proxy)", slrActions, lr1Actions));
        sb.append(String.format(fmt, "GOTO table entries (memory proxy)", slrGotos, lr1Gotos));
        sb.append(String.format(fmt, "Total table entries", slrActions + slrGotos, lr1Actions + lr1Gotos));
        sb.append(String.format(fmt, "Table build time (ms)", slrBuildMs, lr1BuildMs));
        sb.append(String.format(fmt, "Parse time for " + tested + " strings (ms)", slrParseMs, lr1ParseMs));
        sb.append(String.format(fmt, "Strings accepted", slrAccepted, lr1Accepted));
        sb.append(String.format(fmt, "Conflicts found", slrTable.getConflicts().size(), lr1Table.getConflicts().size()));
        sb.append(System.lineSeparator());

        sb.append("  Analysis:").append(System.lineSeparator());
        if (lr1States > slrStates) {
            sb.append("  - LR(1) generates ").append(lr1States - slrStates)
                    .append(" more states (").append(lr1States).append(" vs ").append(slrStates)
                    .append(") because each state encodes exact lookahead sets.").append(System.lineSeparator());
        } else {
            sb.append("  - Both parsers generated the same number of states for this grammar.").append(System.lineSeparator());
        }
        sb.append("  - LR(1) table has ").append(lr1Actions + lr1Gotos)
                .append(" entries vs SLR(1)'s ").append(slrActions + slrGotos)
                .append(" entries.").append(System.lineSeparator());
        sb.append("  - SLR(1): faster to build (fewer states), less memory, but weaker.").append(System.lineSeparator());
        sb.append("  - LR(1): stronger parser power at the cost of more states and memory.").append(System.lineSeparator());
        sb.append(System.lineSeparator());

        // ── Theory summary ────────────────────────────────────────────────────
        sb.append("-----------------------------------------------------------------").append(System.lineSeparator());
        sb.append("  KEY ALGORITHMIC DIFFERENCES").append(System.lineSeparator());
        sb.append("-----------------------------------------------------------------").append(System.lineSeparator());
        sb.append("  SLR(1) REDUCE rule: if [A -> α .] is in state I,").append(System.lineSeparator());
        sb.append("    add REDUCE(A->α) for ALL terminals in FOLLOW(A).").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("  LR(1) REDUCE rule: if [A -> α ., a] is in state I,").append(System.lineSeparator());
        sb.append("    add REDUCE(A->α) ONLY for lookahead terminal a.").append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("  This precision means LR(1) can avoid conflicts that SLR(1) cannot,").append(System.lineSeparator());
        sb.append("  making it strictly more powerful: every SLR(1) grammar is LR(1),").append(System.lineSeparator());
        sb.append("  but not every LR(1) grammar is SLR(1).").append(System.lineSeparator());

        return sb.toString();
    }

    private static List<String> readInputLines(Path inputPath) throws IOException {
        List<String> lines = Files.readAllLines(inputPath);
        List<String> result = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.startsWith("#")) {
                continue;
            }
            result.add(line);
        }
        return result;
    }

    private static List<String> tokenize(String line) {
        String cleaned = line.trim();
        if (cleaned.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(cleaned.split("\\s+")));
    }

    private static void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content);
    }

    private static void printSection(String title, String content) {
        String bar = "=".repeat(70);
        System.out.println(bar);
        System.out.println("  " + title);
        System.out.println(bar);
        System.out.println(content);
    }

    private static void printUsage() {
        System.out.println("Usage: java Main <grammar-file> <input-file> [output-dir]");
        System.out.println("  java Main input/grammar2.txt input/input_valid.txt output");
        System.out.println("  java Main input/grammar3.txt input/input_valid.txt output   # LR(1) superiority demo");
    }
}
