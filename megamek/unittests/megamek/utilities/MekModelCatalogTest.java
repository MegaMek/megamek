/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.client.ui.tileset.UnitModelKey;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;

class MekModelCatalogTest {
    @Test
    void exportedMountsMatchTheLoadedVariantIncludingRearWeapons() throws Exception {
        EquipmentType.initializeTypes();
        Mek atlas = (Mek) new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
        var mounts = MekModelCatalog.mounts(atlas);
        assertEquals(atlas.getEquipment().size(), mounts.size());
        assertEquals(2, mounts.stream().filter(m -> m.family().equals("laser") && m.rear()).count());
        assertTrue(mounts.stream().anyMatch(m -> m.family().equals("missile") && m.rackSize() == 20
              && m.location().equals("LT")));
        for (var mount : mounts) {
            assertEquals(atlas.getEquipment(mount.index()).getType().getInternalName(), mount.internalName());
        }
        String stockKey = UnitModelKey.forEntity(atlas);
        String stockName = atlas.getShortNameRaw();
        atlas.addEquipment(EquipmentType.get("ISSmallLaser"), Mek.LOC_LEFT_ARM);
        assertEquals(stockName, atlas.getShortNameRaw());
        assertNotEquals(stockKey, UnitModelKey.forEntity(atlas), "A same-name refit must not select the stock mesh");
    }
}
