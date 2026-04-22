import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Stack<T> {
    private final Deque<T> data;

    public Stack() {
        this.data = new ArrayDeque<>();
    }

    public void push(T value) {
        data.push(value);
    }

    public T pop() {
        return data.pop();
    }

    public T peek() {
        return data.peek();
    }

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public List<T> toBottomTopList() {
        List<T> list = new ArrayList<>(data);
        java.util.Collections.reverse(list);
        return list;
    }
}
