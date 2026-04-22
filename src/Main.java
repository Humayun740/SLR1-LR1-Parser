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
        Path inputPath = Path.of(args[1]);
        Path outputDir = args.length >= 3 ? Path.of(args[2]) : Path.of("output");

        try {
            Files.createDirectories(outputDir);
            Grammar grammar = Grammar.fromFile(grammarPath);
            SLRParser parser = new SLRParser(grammar);
            parser.buildCanonicalCollection();
            ParsingTable table = parser.buildParsingTable();

            writeFile(outputDir.resolve("augmented_grammar.txt"), grammar.toFormattedGrammar());
            writeFile(outputDir.resolve("slr_items.txt"), parser.formatStates());
            writeFile(outputDir.resolve("slr_parsing_table.txt"),
                    table.toFormattedTable(parser.getStates().size(), grammar.getTerminals(),
                            grammar.getNonTerminals(), grammar.getStartSymbol()));

            List<String> inputLines = readInputLines(inputPath);
            StringBuilder traceOutput = new StringBuilder();
            StringBuilder treeOutput = new StringBuilder();

            int acceptedCount = 0;
            for (String line : inputLines) {
                List<String> tokens = tokenize(line);
                SLRParser.ParseResult result = parser.parse(tokens);

                traceOutput.append(result.formatTrace(line)).append(System.lineSeparator());
                treeOutput.append("Input: ").append(line).append(System.lineSeparator());
                if (result.isAccepted() && result.getParseTree() != null) {
                    acceptedCount++;
                    treeOutput.append(result.getParseTree().toPrettyString());
                } else {
                    treeOutput.append("No parse tree (input rejected)").append(System.lineSeparator());
                }
                treeOutput.append(System.lineSeparator());
            }

            writeFile(outputDir.resolve("slr_trace.txt"), traceOutput.toString());
            writeFile(outputDir.resolve("parse_trees.txt"), treeOutput.toString());

            String summary = "Grammar loaded from: " + grammarPath + System.lineSeparator()
                    + "Input strings tested: " + inputLines.size() + System.lineSeparator()
                    + "Accepted strings: " + acceptedCount + System.lineSeparator()
                    + "State count: " + parser.getStates().size() + System.lineSeparator()
                    + "Conflicts found: " + table.getConflicts().size() + System.lineSeparator()
                    + (table.hasConflicts() ? "Grammar is NOT SLR(1)." : "Grammar is SLR(1).")
                    + System.lineSeparator();

            writeFile(outputDir.resolve("slr_summary.txt"), summary);

            System.out.println(summary);
            System.out.println("Generated files in: " + outputDir.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<String> readInputLines(Path inputPath) throws IOException {
        List<String> lines = Files.readAllLines(inputPath);
        List<String> result = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
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

    private static void printUsage() {
        System.out.println("Usage: java Main <grammar-file> <input-file> [output-dir]");
        System.out.println("Example:");
        System.out.println("  java Main input/grammar2.txt input/input_valid.txt output");
    }
}
