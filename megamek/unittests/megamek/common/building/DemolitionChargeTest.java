package megamek.common.building;

import org.junit.Assert;
import org.junit.Test;

import megamek.common.Coords;
import megamek.test.TestUtilities;

public class DemolitionChargeTest {

    @Test
    public void testEquals() {
        DemolitionCharge dc1 = new DemolitionCharge(0, 1, new Coords(1, 2));
        DemolitionCharge dc2 = new DemolitionCharge(0, 1, new Coords(1, 2));

        // each must have its identity...
        Assert.assertNotEquals(dc1, dc2);
        // ...which must not be the object identity
        TestUtilities.checkSerializable(dc1);
    }

}
