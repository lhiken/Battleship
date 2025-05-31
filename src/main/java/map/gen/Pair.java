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
     * Sets current first and second to parameters first and second
     * @param first is parameter to replace current first
     * @param second is parameter to replace current second
     */
    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

    /**
     * @return first
     */
    public K getFirst() {
        return first;
    }

    /**
     * @return second
     */
    public V getSecond() {
        return second;
    }

    /**
     * Checks if current hashmap pair is equal to o's
     * @param o is object to compare with
     * @return true or false of statement
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;
        return (first == pair.first && second == pair.second);
    }

    /**
     * @return hashcode of first and second
     */
    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    /**
     * @return string of first and second
     */
    @Override
    public String toString() {
        return "Pair{" + "first=" + first + ", second=" + second + '}';
    }
}
