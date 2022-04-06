package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import megamek.common.Coords;

@RunWith(JUnit4.class)
public class BotGeometryTest {

    /**
     * Carries out a test of the BotGeometry donut functionality. 
     */
    @Test
    public void testDonut() {
        Coords testCoords = new Coords(0, 0);
        
        List<Coords> resultingCoords = testCoords.allAtDistance(0);
        Assert.assertEquals(1, resultingCoords.size());
        Assert.assertEquals(true, resultingCoords.contains(testCoords));
        
        // for a radius 1 donut, we expect to see 6 hexes.
        resultingCoords = testCoords.allAtDistance(1);
        
        List<Coords> expectedCoords = new ArrayList<>();
        expectedCoords.add(new Coords(1, -1));
        expectedCoords.add(new Coords(1, 0));
        expectedCoords.add(new Coords(0, -1));
        expectedCoords.add(new Coords(0, 1));
        expectedCoords.add(new Coords(-1, 0));
        expectedCoords.add(new Coords(-1, -1));
        
        Assert.assertEquals(6, resultingCoords.size());
        for (int x = 0; x < expectedCoords.size(); x++) {
            Assert.assertEquals(true, resultingCoords.contains(expectedCoords.get(x)));
        }
        
        // for a radius 2 donut we expect to see 12 hexes.
        resultingCoords = testCoords.allAtDistance(2);
        
        expectedCoords = new ArrayList<>();
        expectedCoords.add(new Coords(-2, 0));
        expectedCoords.add(new Coords(0, -2));
        expectedCoords.add(new Coords(1, 1));
        expectedCoords.add(new Coords(-2, 1));
        expectedCoords.add(new Coords(1, -2));
        expectedCoords.add(new Coords(-2, -1));
        expectedCoords.add(new Coords(2, 1));
        expectedCoords.add(new Coords(-1, -2));
        expectedCoords.add(new Coords(2, -1));
        expectedCoords.add(new Coords(2, 0));
        expectedCoords.add(new Coords(0, 2));
        expectedCoords.add(new Coords(-1, 1));
        Assert.assertEquals(12, resultingCoords.size());
        for (int x = 0; x < expectedCoords.size(); x++) {
            Assert.assertEquals(true, resultingCoords.contains(expectedCoords.get(x)));
        }
        
    }
}
