package megamek.common.building;

import org.junit.Test;

import megamek.common.Coords;
import megamek.test.TestUtilities;

public class BuildingSectionTest {

    @Test
    public void testSerializable() {
        TestUtilities.checkSerializable(new BuildingSection(new Coords(0,0), BasementType.NONE, 0, 0, 0, false, false));
    }


}

