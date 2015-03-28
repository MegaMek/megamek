/*
 * 
 */
package pathfinder.elements;

import java.util.Arrays;
import java.util.List;

/**
 * Enumeration of possible facing in six directions (plus 'None'),
 * with utility methods.
 * @author kwirkyj
 */
public enum Facing {
    N, NE, SE, S, SW, NW, NONE;
    
    private static final List<Facing> facings = Arrays.asList(Facing.values());
    
    /**
     * Enum value from an index.
     * Added for completeness, try to not use.
     * @param i index of facing
     * @return NONE iff (i &lt; 0 || i &gt; 5);
     *         else N..NW mapped clockwise to 0..5.
     */
    public static Facing valueOf(final int i) {
        return (i < 0 || i >= values().length) ? NONE : values()[i];
    }
    
    /**
     * Get nominal integer representation of a facing.
     * Added for completeness, try to not use.
     * @param f Facing
     * @return int (0..6), mapped clockwise starting 0 at N; 6 -> NONE.
     */
    public static int intOf(final Facing f) {
        return facings.indexOf((Facing) f);
    }
    
    /**
     * Next facing in a clockwise turn.
     * @return NONE iff NONE; else, clockwise facing.
     */
    public Facing nextClockwise() {
        return (this == NONE) ? NONE 
                : facings.get((facings.indexOf((Facing) this) + 1) % 6);
    }
    
    /**
     * Next facing in a counter-clockwise turn.
     * @return NONE iff NONE; else, ccw facing.
     */
    public Facing nextCounterClockwise() {
        if (this == NONE) {
            return NONE;
        }
        int i = facings.indexOf((Facing) this) - 1;
        i = (i < 0) ? i + 6 : i;
        return facings.get(i);
    }
    
    /**
     * Opposing facing.
     * @return NONE iff NONE; else, opposite face.
     */
    public Facing opposite() {
        return (this == NONE) ? NONE 
                : facings.get((facings.indexOf((Facing) this) + 3) % 6);
    }
}
