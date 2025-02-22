package megamek.common.loaders;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.HandheldWeapon;
import megamek.common.MekFileParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class BLKHandheldWeaponFileTest {

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testLoadHandheldWeaponBLK() throws Exception {
        File f;
        MekFileParser mfp;
        HandheldWeapon e;


        f = new File("testresources/megamek/common/units/TestHandheldWeapon.blk");
        mfp = new MekFileParser(f);
        e = (HandheldWeapon) mfp.getEntity();
        assertEquals(10, e.getOArmor(HandheldWeapon.LOC_GUN), "Failed to load tonnage");
        assertEquals(2, e.getEquipment().size(), "Failed to load equipment");
        assertEquals(6, e.getAmmo().get(0).getOriginalShots(), "Failed to load ammo");
        assertEquals(12.0, e.getWeight(), "Failed to load weight");

    }
}
