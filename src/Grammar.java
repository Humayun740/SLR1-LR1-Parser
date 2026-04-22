import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Grammar {
    private final List<Items.Production> productions;
    private final Map<String, List<Items.Production>> productionsByLhs;
    private final Set<String> nonTerminals;
    private final Set<String> terminals;
    private final String originalStartSymbol;
    private final String startSymbol;

    private Grammar(List<Items.Production> productions,
                    Set<String> nonTerminals,
                    Set<String> terminals,
                    String originalStartSymbol,
                    String startSymbol) {
        this.productions = productions;
        this.nonTerminals = nonTerminals;
        this.terminals = terminals;
        this.originalStartSymbol = originalStartSymbol;
        this.startSymbol = startSymbol;
        this.productionsByLhs = new LinkedHashMap<>();
        for (Items.Production production : productions) {
            productionsByLhs.computeIfAbsent(production.getLhs(), k -> new ArrayList<>()).add(production);
        }
    }

    public static Grammar fromFile(Path filePath) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            cleaned.add(trimmed);
        }

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Grammar file is empty: " + filePath);
        }

        Set<String> nonTerminals = new LinkedHashSet<>();
        for (String line : cleaned) {
            String[] parts = splitProductionLine(line);
            nonTerminals.add(parts[0].trim());
        }

        String originalStart = splitProductionLine(cleaned.get(0))[0].trim();
        String augmentedStart = generateAugmentedStart(originalStart, nonTerminals);

        List<Items.Production> productions = new ArrayList<>();
        int productionId = 0;

        productions.add(new Items.Production(productionId++, augmentedStart, Collections.singletonList(originalStart)));

        for (String line : cleaned) {
            String[] parts = splitProductionLine(line);
            String lhs = parts[0].trim();
            String rhsText = parts[1].trim();
            String[] alternatives = rhsText.split("\\|");
            for (String alt : alternatives) {
                String altTrim = alt.trim();
                List<String> rhs = parseRhs(altTrim);
                productions.add(new Items.Production(productionId++, lhs, rhs));
            }
        }

        nonTerminals.add(augmentedStart);

        Set<String> terminals = new LinkedHashSet<>();
        for (Items.Production production : productions) {
            for (String symbol : production.getRhs()) {
                if (!nonTerminals.contains(symbol) && !symbol.equals(Items.EPSILON)) {
                    terminals.add(symbol);
                }
            }
        }

        return new Grammar(productions, nonTerminals, terminals, originalStart, augmentedStart);
    }

    private static String[] splitProductionLine(String line) {
        String normalized = line.replaceAll("\\s*-\\s*>\\s*", " -> ").trim();
        String[] parts = normalized.split("->", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid production line: " + line);
        }
        return parts;
    }

    private static List<String> parseRhs(String text) {
        if (text.equals("@") || text.equalsIgnoreCase("epsilon")) {
            return Collections.emptyList();
        }
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        String[] symbols = text.split("\\s+");
        List<String> rhs = new ArrayList<>();
        for (String symbol : symbols) {
            if (symbol.equals("@") || symbol.equalsIgnoreCase("epsilon")) {
                continue;
            }
            rhs.add(symbol);
        }
        return rhs;
    }

    private static String generateAugmentedStart(String originalStart, Set<String> nonTerminals) {
        String candidate = originalStart + "Prime";
        int counter = 1;
        while (nonTerminals.contains(candidate)) {
            candidate = originalStart + "Prime" + counter++;
        }
        return candidate;
    }

    public List<Items.Production> getProductions() {
        return productions;
    }

    public List<Items.Production> getProductionsFor(String lhs) {
        return productionsByLhs.getOrDefault(lhs, Collections.emptyList());
    }

    public Set<String> getNonTerminals() {
        return nonTerminals;
    }

    public Set<String> getTerminals() {
        return terminals;
    }

    public String getOriginalStartSymbol() {
        return originalStartSymbol;
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    public Set<String> getGrammarSymbols() {
        Set<String> symbols = new LinkedHashSet<>();
        symbols.addAll(terminals);
        symbols.addAll(nonTerminals);
        return symbols;
    }

    public Map<String, Set<String>> computeFirstSets() {
        Map<String, Set<String>> first = new LinkedHashMap<>();
        for (String terminal : terminals) {
            first.put(terminal, new LinkedHashSet<>(Collections.singletonList(terminal)));
        }
        for (String nonTerminal : nonTerminals) {
            first.putIfAbsent(nonTerminal, new LinkedHashSet<>());
        }

        boolean changed;
        do {
            changed = false;
            for (Items.Production production : productions) {
                String lhs = production.getLhs();
                Set<String> lhsFirst = first.get(lhs);
                if (production.isEpsilonProduction()) {
                    if (lhsFirst.add(Items.EPSILON)) {
                        changed = true;
                    }
                    continue;
                }

                boolean allNullable = true;
                for (String symbol : production.getRhs()) {
                    Set<String> symbolFirst = first.getOrDefault(symbol,
                            new LinkedHashSet<>(Collections.singletonList(symbol)));
                    for (String token : symbolFirst) {
                        if (!token.equals(Items.EPSILON) && lhsFirst.add(token)) {
                            changed = true;
                        }
                    }
                    if (!symbolFirst.contains(Items.EPSILON)) {
                        allNullable = false;
                        break;
                    }
                }

                if (allNullable && lhsFirst.add(Items.EPSILON)) {
                    changed = true;
                }
            }
        } while (changed);

        return first;
    }

    public Set<String> firstOfSequence(List<String> symbols, Map<String, Set<String>> firstSets) {
        Set<String> result = new LinkedHashSet<>();
        if (symbols.isEmpty()) {
            result.add(Items.EPSILON);
            return result;
        }

        boolean allNullable = true;
        for (String symbol : symbols) {
            Set<String> symbolFirst = firstSets.getOrDefault(symbol,
                    new LinkedHashSet<>(Collections.singletonList(symbol)));
            for (String token : symbolFirst) {
                if (!token.equals(Items.EPSILON)) {
                    result.add(token);
                }
            }
            if (!symbolFirst.contains(Items.EPSILON)) {
                allNullable = false;
                break;
            }
        }

        if (allNullable) {
            result.add(Items.EPSILON);
        }

        return result;
    }

    public Map<String, Set<String>> computeFollowSets() {
        Map<String, Set<String>> firstSets = computeFirstSets();
        Map<String, Set<String>> follow = new LinkedHashMap<>();

        for (String nonTerminal : nonTerminals) {
            follow.put(nonTerminal, new LinkedHashSet<>());
        }
        follow.get(originalStartSymbol).add(Items.END_MARKER);

        boolean changed;
        do {
            changed = false;
            for (Items.Production production : productions) {
                String lhs = production.getLhs();
                List<String> rhs = production.getRhs();

                for (int i = 0; i < rhs.size(); i++) {
                    String symbol = rhs.get(i);
                    if (!nonTerminals.contains(symbol)) {
                        continue;
                    }

                    List<String> beta = rhs.subList(i + 1, rhs.size());
                    Set<String> firstBeta = firstOfSequence(beta, firstSets);
                    for (String t : firstBeta) {
                        if (!t.equals(Items.EPSILON) && follow.get(symbol).add(t)) {
                            changed = true;
                        }
                    }

                    if (beta.isEmpty() || firstBeta.contains(Items.EPSILON)) {
                        for (String t : follow.get(lhs)) {
                            if (follow.get(symbol).add(t)) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);

        return follow;
    }

    public String toFormattedGrammar() {
        StringBuilder builder = new StringBuilder();
        builder.append("Original Start Symbol: ").append(originalStartSymbol).append(System.lineSeparator());
        builder.append("Augmented Start Symbol: ").append(startSymbol).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("Augmented Grammar:").append(System.lineSeparator());
        for (Items.Production production : productions) {
            builder.append(production).append(System.lineSeparator());
        }
        return builder.toString();
    }
}
