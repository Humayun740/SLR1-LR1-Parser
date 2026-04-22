import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParsingTable {
    private final Map<Integer, Map<String, Items.Action>> actionTable;
    private final Map<Integer, Map<String, Integer>> gotoTable;
    private final List<String> conflicts;

    public ParsingTable() {
        this.actionTable = new LinkedHashMap<>();
        this.gotoTable = new LinkedHashMap<>();
        this.conflicts = new ArrayList<>();
    }

    public void setAction(int state, String terminal, Items.Action action) {
        Map<String, Items.Action> row = actionTable.computeIfAbsent(state, k -> new LinkedHashMap<>());
        Items.Action existing = row.get(terminal);
        if (existing != null && !existing.equals(action)) {
            String type = conflictType(existing, action);
            conflicts.add(type + " conflict at state I" + state + ", symbol '" + terminal
                    + "': existing=" + existing + ", incoming=" + action);
            return;
        }
        row.put(terminal, action);
    }

    public void setGoto(int state, String nonTerminal, int target) {
        Map<String, Integer> row = gotoTable.computeIfAbsent(state, k -> new LinkedHashMap<>());
        Integer existing = row.get(nonTerminal);
        if (existing != null && existing != target) {
            conflicts.add("GOTO conflict at state I" + state + ", symbol '" + nonTerminal
                    + "': existing=" + existing + ", incoming=" + target);
            return;
        }
        row.put(nonTerminal, target);
    }

    public Items.Action getAction(int state, String terminal) {
        return actionTable.getOrDefault(state, Collections.emptyMap()).get(terminal);
    }

    public Integer getGoto(int state, String nonTerminal) {
        return gotoTable.getOrDefault(state, Collections.emptyMap()).get(nonTerminal);
    }

    public int getActionEntryCount() {
        int count = 0;
        for (Map<String, Items.Action> row : actionTable.values()) {
            count += row.size();
        }
        return count;
    }

    public int getGotoEntryCount() {
        int count = 0;
        for (Map<String, Integer> row : gotoTable.values()) {
            count += row.size();
        }
        return count;
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public String toFormattedTable(int stateCount, Set<String> terminals, Set<String> nonTerminals, String augmentedStart) {
        List<String> actionCols = new ArrayList<>(terminals);
        if (!actionCols.contains(Items.END_MARKER)) {
            actionCols.add(Items.END_MARKER);
        }

        List<String> gotoCols = new ArrayList<>();
        for (String nonTerminal : nonTerminals) {
            if (!nonTerminal.equals(augmentedStart)) {
                gotoCols.add(nonTerminal);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Parsing Table (ACTION | GOTO)").append(System.lineSeparator());
        builder.append("State");
        for (String terminal : actionCols) {
            builder.append("\t").append(terminal);
        }
        for (String nonTerminal : gotoCols) {
            builder.append("\t").append(nonTerminal);
        }
        builder.append(System.lineSeparator());

        for (int i = 0; i < stateCount; i++) {
            builder.append("I").append(i);
            for (String terminal : actionCols) {
                Items.Action action = getAction(i, terminal);
                builder.append("\t").append(action == null ? "" : action.toString());
            }
            for (String nonTerminal : gotoCols) {
                Integer target = getGoto(i, nonTerminal);
                builder.append("\t").append(target == null ? "" : target);
            }
            builder.append(System.lineSeparator());
        }

        builder.append(System.lineSeparator());
        if (hasConflicts()) {
            builder.append("Conflicts:").append(System.lineSeparator());
            for (String conflict : conflicts) {
                builder.append("- ").append(conflict).append(System.lineSeparator());
            }
        } else {
            builder.append("No conflicts found. Grammar is SLR(1).\n");
        }

        return builder.toString();
    }

    private String conflictType(Items.Action a, Items.Action b) {
        boolean reduceA = a.getType() == Items.ActionType.REDUCE;
        boolean reduceB = b.getType() == Items.ActionType.REDUCE;
        if (reduceA && reduceB) {
            return "Reduce/Reduce";
        }
        return "Shift/Reduce";
    }
}
