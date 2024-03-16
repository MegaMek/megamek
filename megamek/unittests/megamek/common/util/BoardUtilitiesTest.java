package megamek.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @since 9/3/14 1:44 PM
 */
public class BoardUtilitiesTest {

    @Test
    public void testCraterProfile() {
        int craterRadius = 8;
        int maxDepth = 4;

        // Start at the center;
        int distanceFromCenter = 0;
        int expected = -4;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // One hex from center;
        distanceFromCenter = 1;
        expected = -4;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Three hexes from center;
        distanceFromCenter = 3;
        expected = -4;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Four hexes from center;
        distanceFromCenter = 4;
        expected = -4;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Five hexes from center;
        distanceFromCenter = 5;
        expected = -3;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Six hexes from center;
        distanceFromCenter = 6;
        expected = -3;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Seven hexes from center;
        distanceFromCenter = 7;
        expected = -2;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));

        // Eight hexes from center;
        distanceFromCenter = 8;
        expected = 0;
        assertEquals(expected, BoardUtilities.craterProfile(distanceFromCenter, craterRadius, maxDepth));
    }
}
