/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.utils.JsonReader;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

class UnitModelSelectionTest {
    @Test
    void unidentifiedContactsNeverResolveModelIdentity() {
        Entity hidden = mock(Entity.class);
        MekTileset tileset = mock(MekTileset.class);
        assertNull(UnitModelSelection.capture(hidden, -1, true, tileset));
        verifyNoInteractions(hidden, tileset);
    }

    @Test
    void formationsUseSurvivingPersonnelAndStaySmall() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        MekTileset tileset = mock(MekTileset.class);
        when(tileset.modelFor(infantry, -1)).thenReturn("units/infantry/model.json");
        assertEquals(6, UnitModelSelection.capture(infantry, -1, false, tileset).figures());
        infantry.setInternal(4, ConvInfantry.LOC_INFANTRY);
        assertEquals(2, UnitModelSelection.capture(infantry, -1, false, tileset).figures());
        infantry.setInternal(0, ConvInfantry.LOC_INFANTRY);
        assertEquals(0, UnitModelSelection.capture(infantry, -1, false, tileset).figures());
        BattleArmor armor = mock(BattleArmor.class);
        when(armor.locations()).thenReturn(6);
        for (int loc = 1; loc <= 5; loc++) {
            when(armor.getInternal(loc)).thenReturn(1);
        }
        when(tileset.modelFor(armor, -1)).thenReturn("units/battle-armor/model.json");
        assertEquals(3, UnitModelSelection.capture(armor, -1, false, tileset).figures());
        when(armor.getInternal(3)).thenReturn(-1);
        assertEquals(2, UnitModelSelection.capture(armor, -1, false, tileset).figures());
        assertEquals(4, UnitModelSelection.figures(100, 4));
    }

    @Test
    void unknownVariantsFallBackAndZeroStrengthHasAnEmptyFormation() {
        var mek = new JsonReader().parse("""
              {"kind":"mek", "fallback":"body.g3dj", "variants":{"Atlas AS7-D":"as7-d.g3dj"}}
              """);
        assertEquals("as7-d.g3dj", GpuUnitModels.selectModel(mek, "Atlas AS7-D", 0));
        assertEquals("body.g3dj", GpuUnitModels.selectModel(mek, "Custom loadout", 0));
        var infantry = new JsonReader().parse("""
              {"kind":"formation", "fallback":"one.g3dj", "formations":{"0":"empty.g3dj", "6":"six.g3dj"}}
              """);
        assertEquals("empty.g3dj", GpuUnitModels.selectModel(infantry, "Rifle Platoon", 0));
        assertEquals("six.g3dj", GpuUnitModels.selectModel(infantry, "Rifle Platoon", 6));
    }
}
