package map.gen;

import java.util.Objects;

/**
 * a pair for a hashmap
 * @param <K> the first type
 * @param <V> the second type
 */
public class Pair<K, V> {

    /**
     * first value in pair
     */
    private final K first;
    /**
     * second value in pair
     */
    private final V second;

    /**
     *
     * @param first
     * @param second
     */
    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

    public K getFirst() {
        return first;
    }

    public V getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;
        return (first == pair.first && second == pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "Pair{" + "first=" + first + ", second=" + second + '}';
    }
}
