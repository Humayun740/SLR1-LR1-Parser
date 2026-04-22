import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tree {
    public static class Node {
        private final String symbol;
        private final List<Node> children;

        public Node(String symbol) {
            this.symbol = symbol;
            this.children = new ArrayList<>();
        }

        public String getSymbol() {
            return symbol;
        }

        public List<Node> getChildren() {
            return Collections.unmodifiableList(children);
        }

        public void addChild(Node child) {
            children.add(child);
        }

        public String toPrettyString() {
            StringBuilder builder = new StringBuilder();
            render(builder, "", true);
            return builder.toString();
        }

        private void render(StringBuilder builder, String prefix, boolean isTail) {
            builder.append(prefix)
                    .append(isTail ? "`-- " : "|-- ")
                    .append(symbol)
                    .append(System.lineSeparator());
            for (int i = 0; i < children.size(); i++) {
                boolean childTail = (i == children.size() - 1);
                children.get(i).render(builder, prefix + (isTail ? "    " : "|   "), childTail);
            }
        }
    }
}
