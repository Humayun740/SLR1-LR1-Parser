import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Items {
    private Items() {
    }

    public static final String EPSILON = "epsilon";
    public static final String END_MARKER = "$";

    public static final class Production {
        private final int id;
        private final String lhs;
        private final List<String> rhs;

        public Production(int id, String lhs, List<String> rhs) {
            this.id = id;
            this.lhs = lhs;
            this.rhs = Collections.unmodifiableList(new ArrayList<>(rhs));
        }

        public int getId() {
            return id;
        }

        public String getLhs() {
            return lhs;
        }

        public List<String> getRhs() {
            return rhs;
        }

        public boolean isEpsilonProduction() {
            return rhs.isEmpty();
        }

        public String toRuleString() {
            return lhs + " -> " + (rhs.isEmpty() ? EPSILON : String.join(" ", rhs));
        }

        @Override
        public String toString() {
            return "(" + id + ") " + toRuleString();
        }
    }

    public static final class LR0Item {
        private final Production production;
        private final int dotPosition;

        public LR0Item(Production production, int dotPosition) {
            this.production = production;
            this.dotPosition = dotPosition;
        }

        public Production getProduction() {
            return production;
        }

        public int getDotPosition() {
            return dotPosition;
        }

        public boolean isDotAtEnd() {
            return dotPosition >= production.getRhs().size();
        }

        public String symbolAfterDot() {
            if (isDotAtEnd()) {
                return null;
            }
            return production.getRhs().get(dotPosition);
        }

        public LR0Item advanceDot() {
            return new LR0Item(production, dotPosition + 1);
        }

        public String formatWithDot() {
            List<String> rhs = production.getRhs();
            List<String> parts = new ArrayList<>();
            for (int i = 0; i <= rhs.size(); i++) {
                if (i == dotPosition) {
                    parts.add(".");
                }
                if (i < rhs.size()) {
                    parts.add(rhs.get(i));
                }
            }
            return production.getLhs() + " -> " + String.join(" ", parts);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LR0Item lr0Item = (LR0Item) o;
            return dotPosition == lr0Item.dotPosition && production.getId() == lr0Item.production.getId();
        }

        @Override
        public int hashCode() {
            return Objects.hash(production.getId(), dotPosition);
        }

        @Override
        public String toString() {
            return formatWithDot();
        }
    }

    public enum ActionType {
        SHIFT,
        REDUCE,
        ACCEPT
    }

    public static final class Action {
        private final ActionType type;
        private final int targetState;
        private final Production reduceProduction;

        private Action(ActionType type, int targetState, Production reduceProduction) {
            this.type = type;
            this.targetState = targetState;
            this.reduceProduction = reduceProduction;
        }

        public static Action shift(int state) {
            return new Action(ActionType.SHIFT, state, null);
        }

        public static Action reduce(Production production) {
            return new Action(ActionType.REDUCE, -1, production);
        }

        public static Action accept() {
            return new Action(ActionType.ACCEPT, -1, null);
        }

        public ActionType getType() {
            return type;
        }

        public int getTargetState() {
            return targetState;
        }

        public Production getReduceProduction() {
            return reduceProduction;
        }

        @Override
        public String toString() {
            if (type == ActionType.SHIFT) {
                return "s" + targetState;
            }
            if (type == ActionType.REDUCE) {
                return "r" + reduceProduction.getId() + " (" + reduceProduction.toRuleString() + ")";
            }
            return "acc";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Action action = (Action) o;
            return targetState == action.targetState && type == action.type
                    && Objects.equals(reduceProduction == null ? null : reduceProduction.getId(),
                    action.reduceProduction == null ? null : action.reduceProduction.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, targetState, reduceProduction == null ? null : reduceProduction.getId());
        }
    }

    public static final class LR1Item {
        private final Production production;
        private final int dotPosition;
        private final String lookahead;

        public LR1Item(Production production, int dotPosition, String lookahead) {
            this.production = production;
            this.dotPosition = dotPosition;
            this.lookahead = lookahead;
        }

        public Production getProduction() {
            return production;
        }

        public int getDotPosition() {
            return dotPosition;
        }

        public String getLookahead() {
            return lookahead;
        }

        public boolean isDotAtEnd() {
            return dotPosition >= production.getRhs().size();
        }

        public String symbolAfterDot() {
            if (isDotAtEnd()) {
                return null;
            }
            return production.getRhs().get(dotPosition);
        }

        public LR1Item advanceDot() {
            return new LR1Item(production, dotPosition + 1, lookahead);
        }

        public String formatWithDot() {
            List<String> rhs = production.getRhs();
            List<String> parts = new ArrayList<>();
            for (int i = 0; i <= rhs.size(); i++) {
                if (i == dotPosition) {
                    parts.add(".");
                }
                if (i < rhs.size()) {
                    parts.add(rhs.get(i));
                }
            }
            return "[" + production.getLhs() + " -> " + String.join(" ", parts) + ", " + lookahead + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LR1Item lr1Item = (LR1Item) o;
            return dotPosition == lr1Item.dotPosition
                    && production.getId() == lr1Item.production.getId()
                    && lookahead.equals(lr1Item.lookahead);
        }

        @Override
        public int hashCode() {
            return Objects.hash(production.getId(), dotPosition, lookahead);
        }

        @Override
        public String toString() {
            return formatWithDot();
        }
    }

    public static final class ParsingStep {
        private final int step;
        private final String stack;
        private final String input;
        private final String action;

        public ParsingStep(int step, String stack, String input, String action) {
            this.step = step;
            this.stack = stack;
            this.input = input;
            this.action = action;
        }

        public int getStep() {
            return step;
        }

        public String getStack() {
            return stack;
        }

        public String getInput() {
            return input;
        }

        public String getAction() {
            return action;
        }

        @Override
        public String toString() {
            return String.format("%-6d | %-40s | %-25s | %s", step, stack, input, action);
        }
    }
}
