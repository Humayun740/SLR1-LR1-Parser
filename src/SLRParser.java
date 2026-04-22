import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SLRParser {
    private final Grammar grammar;
    private final List<Set<Items.LR0Item>> states;
    private final Map<Integer, Map<String, Integer>> transitions;
    private ParsingTable parsingTable;

    public SLRParser(Grammar grammar) {
        this.grammar = grammar;
        this.states = new ArrayList<>();
        this.transitions = new LinkedHashMap<>();
    }

    public void buildCanonicalCollection() {
        states.clear();
        transitions.clear();

        Items.Production augmentedProduction = grammar.getProductions().get(0);
        Set<Items.LR0Item> start = closure(setOf(new Items.LR0Item(augmentedProduction, 0)));
        states.add(start);

        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(0);

        while (!queue.isEmpty()) {
            int stateId = queue.poll();
            Set<Items.LR0Item> state = states.get(stateId);

            for (String symbol : grammar.getGrammarSymbols()) {
                Set<Items.LR0Item> next = goTo(state, symbol);
                if (next.isEmpty()) {
                    continue;
                }

                int nextId = indexOfState(next);
                if (nextId == -1) {
                    states.add(next);
                    nextId = states.size() - 1;
                    queue.add(nextId);
                }

                transitions.computeIfAbsent(stateId, k -> new LinkedHashMap<>()).put(symbol, nextId);
            }
        }
    }

    public ParsingTable buildParsingTable() {
        if (states.isEmpty()) {
            buildCanonicalCollection();
        }

        ParsingTable table = new ParsingTable();
        Map<String, Set<String>> followSets = grammar.computeFollowSets();

        for (int i = 0; i < states.size(); i++) {
            Set<Items.LR0Item> state = states.get(i);

            for (Items.LR0Item item : state) {
                String nextSymbol = item.symbolAfterDot();
                if (nextSymbol != null) {
                    Integer target = transitions.getOrDefault(i, Map.of()).get(nextSymbol);
                    if (target == null) {
                        continue;
                    }
                    if (grammar.getTerminals().contains(nextSymbol)) {
                        table.setAction(i, nextSymbol, Items.Action.shift(target));
                    } else if (grammar.getNonTerminals().contains(nextSymbol)) {
                        table.setGoto(i, nextSymbol, target);
                    }
                    continue;
                }

                Items.Production production = item.getProduction();
                if (production.getLhs().equals(grammar.getStartSymbol())) {
                    table.setAction(i, Items.END_MARKER, Items.Action.accept());
                    continue;
                }

                Set<String> follow = followSets.getOrDefault(production.getLhs(), Set.of());
                for (String terminal : follow) {
                    table.setAction(i, terminal, Items.Action.reduce(production));
                }
            }
        }

        parsingTable = table;
        return table;
    }

    public ParseResult parse(List<String> inputTokens) {
        if (parsingTable == null) {
            buildParsingTable();
        }

        List<String> tokens = new ArrayList<>(inputTokens);
        if (tokens.isEmpty() || !tokens.get(tokens.size() - 1).equals(Items.END_MARKER)) {
            tokens.add(Items.END_MARKER);
        }

        Stack<Integer> stateStack = new Stack<>();
        Stack<String> symbolStack = new Stack<>();
        Stack<Tree.Node> nodeStack = new Stack<>();

        stateStack.push(0);

        int inputIndex = 0;
        int stepNumber = 1;
        List<Items.ParsingStep> trace = new ArrayList<>();

        while (true) {
            int state = stateStack.peek();
            String lookahead = tokens.get(inputIndex);
            Items.Action action = parsingTable.getAction(state, lookahead);

            if (action == null) {
                trace.add(new Items.ParsingStep(stepNumber,
                        formatStack(symbolStack, stateStack),
                        formatInput(tokens, inputIndex),
                        "error"));
                return new ParseResult(false, trace, null,
                        "No ACTION entry for state I" + state + " and symbol '" + lookahead + "'.");
            }

            if (action.getType() == Items.ActionType.SHIFT) {
                int nextState = action.getTargetState();
                trace.add(new Items.ParsingStep(stepNumber,
                        formatStack(symbolStack, stateStack),
                        formatInput(tokens, inputIndex),
                        "shift " + nextState));

                symbolStack.push(lookahead);
                nodeStack.push(new Tree.Node(lookahead));
                stateStack.push(nextState);
                inputIndex++;
            } else if (action.getType() == Items.ActionType.REDUCE) {
                Items.Production production = action.getReduceProduction();
                trace.add(new Items.ParsingStep(stepNumber,
                        formatStack(symbolStack, stateStack),
                        formatInput(tokens, inputIndex),
                        "reduce " + production.toRuleString()));

                Tree.Node parent = new Tree.Node(production.getLhs());
                List<Tree.Node> children = new ArrayList<>();
                int popCount = production.getRhs().size();

                for (int i = 0; i < popCount; i++) {
                    stateStack.pop();
                    symbolStack.pop();
                    children.add(nodeStack.pop());
                }

                for (int i = children.size() - 1; i >= 0; i--) {
                    parent.addChild(children.get(i));
                }

                if (production.isEpsilonProduction()) {
                    parent.addChild(new Tree.Node(Items.EPSILON));
                }

                int gotoState = stateStack.peek();
                Integer nextState = parsingTable.getGoto(gotoState, production.getLhs());
                if (nextState == null) {
                    return new ParseResult(false, trace, null,
                            "Missing GOTO for state I" + gotoState + " and non-terminal '"
                                    + production.getLhs() + "'.");
                }

                symbolStack.push(production.getLhs());
                nodeStack.push(parent);
                stateStack.push(nextState);
            } else {
                trace.add(new Items.ParsingStep(stepNumber,
                        formatStack(symbolStack, stateStack),
                        formatInput(tokens, inputIndex),
                        "accept"));
                Tree.Node tree = nodeStack.isEmpty() ? null : nodeStack.peek();
                return new ParseResult(true, trace, tree, "accepted");
            }

            stepNumber++;
        }
    }

    public String formatStates() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < states.size(); i++) {
            builder.append("I").append(i).append(":").append(System.lineSeparator());
            List<Items.LR0Item> sortedItems = new ArrayList<>(states.get(i));
            sortedItems.sort(Comparator
                    .comparing((Items.LR0Item it) -> it.getProduction().getId())
                    .thenComparing(Items.LR0Item::getDotPosition));

            for (Items.LR0Item item : sortedItems) {
                builder.append("  ").append(item.formatWithDot()).append(System.lineSeparator());
            }

            Map<String, Integer> edges = transitions.getOrDefault(i, Map.of());
            if (!edges.isEmpty()) {
                builder.append("  GOTO transitions:").append(System.lineSeparator());
                List<Map.Entry<String, Integer>> entries = new ArrayList<>(edges.entrySet());
                entries.sort(Map.Entry.comparingByKey());
                for (Map.Entry<String, Integer> edge : entries) {
                    builder.append("    ").append(edge.getKey()).append(" -> I")
                            .append(edge.getValue()).append(System.lineSeparator());
                }
            }

            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    public List<Set<Items.LR0Item>> getStates() {
        return states;
    }

    public Map<Integer, Map<String, Integer>> getTransitions() {
        return transitions;
    }

    private Set<Items.LR0Item> closure(Set<Items.LR0Item> items) {
        Set<Items.LR0Item> closure = new LinkedHashSet<>(items);
        boolean changed;

        do {
            changed = false;
            List<Items.LR0Item> snapshot = new ArrayList<>(closure);
            for (Items.LR0Item item : snapshot) {
                String symbol = item.symbolAfterDot();
                if (symbol == null || !grammar.getNonTerminals().contains(symbol)) {
                    continue;
                }
                for (Items.Production production : grammar.getProductionsFor(symbol)) {
                    Items.LR0Item candidate = new Items.LR0Item(production, 0);
                    if (closure.add(candidate)) {
                        changed = true;
                    }
                }
            }
        } while (changed);

        return closure;
    }

    private Set<Items.LR0Item> goTo(Set<Items.LR0Item> state, String symbol) {
        Set<Items.LR0Item> moved = new LinkedHashSet<>();
        for (Items.LR0Item item : state) {
            if (symbol.equals(item.symbolAfterDot())) {
                moved.add(item.advanceDot());
            }
        }
        if (moved.isEmpty()) {
            return moved;
        }
        return closure(moved);
    }

    private int indexOfState(Set<Items.LR0Item> candidate) {
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i).equals(candidate)) {
                return i;
            }
        }
        return -1;
    }

    private Set<Items.LR0Item> setOf(Items.LR0Item item) {
        Set<Items.LR0Item> set = new LinkedHashSet<>();
        set.add(item);
        return set;
    }

    private String formatInput(List<String> tokens, int startIndex) {
        return String.join(" ", tokens.subList(startIndex, tokens.size()));
    }

    private String formatStack(Stack<String> symbolStack, Stack<Integer> stateStack) {
        List<String> symbols = symbolStack.toBottomTopList();
        List<Integer> statesList = stateStack.toBottomTopList();

        StringBuilder builder = new StringBuilder();
        builder.append(statesList.get(0));
        for (int i = 0; i < symbols.size(); i++) {
            builder.append(" ").append(symbols.get(i));
            builder.append(" ").append(statesList.get(i + 1));
        }
        return builder.toString();
    }

    public static class ParseResult {
        private final boolean accepted;
        private final List<Items.ParsingStep> steps;
        private final Tree.Node parseTree;
        private final String message;

        public ParseResult(boolean accepted, List<Items.ParsingStep> steps, Tree.Node parseTree, String message) {
            this.accepted = accepted;
            this.steps = steps;
            this.parseTree = parseTree;
            this.message = message;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public List<Items.ParsingStep> getSteps() {
            return steps;
        }

        public Tree.Node getParseTree() {
            return parseTree;
        }

        public String getMessage() {
            return message;
        }

        public String formatTrace(String inputLabel) {
            StringBuilder builder = new StringBuilder();
            builder.append("Input: ").append(inputLabel).append(System.lineSeparator());
            builder.append("Step   | Stack                                    | Input                     | Action")
                    .append(System.lineSeparator());
            builder.append("-------+------------------------------------------+---------------------------+----------------")
                    .append(System.lineSeparator());
            for (Items.ParsingStep step : steps) {
                builder.append(step).append(System.lineSeparator());
            }
            builder.append("Result: ").append(accepted ? "accepted" : "rejected")
                    .append(" (" + message + ")")
                    .append(System.lineSeparator());
            return builder.toString();
        }
    }
}
