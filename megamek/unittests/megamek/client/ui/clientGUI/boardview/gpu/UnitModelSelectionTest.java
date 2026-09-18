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
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
        when(armor.getShortNameRaw()).thenReturn("Battle Armor");
        when(armor.getMovementMode()).thenReturn(EntityMovementMode.INF_JUMP);
        assertEquals(3, UnitModelSelection.capture(armor, -1, false, tileset).figures());
        assertEquals("Battle Armor", UnitModelSelection.capture(armor, -1, false, tileset).variant());
        when(armor.getInternal(3)).thenReturn(-1);
        assertEquals(2, UnitModelSelection.capture(armor, -1, false, tileset).figures());
        assertEquals(4, UnitModelSelection.figures(100, 4));
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = {
          "INF_LEG", "INF_MOTORIZED", "INF_JUMP", "TRACKED", "WHEELED", "HOVER", "INF_UMU"
    })
    void conventionalInfantryAppearanceUsesItsMovementMode(EntityMovementMode movement) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setChassis("Renamed rifle platoon");
        infantry.setMovementMode(movement);
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        MekTileset tileset = mock(MekTileset.class);
        when(tileset.modelFor(infantry, -1)).thenReturn("units/infantry/model.json");
        var selection = UnitModelSelection.capture(infantry, -1, false, tileset);
        assertEquals(movement.name(), selection.variant());
        assertEquals(6, selection.figures());
        // Casualties change the compressed formation, not its vehicle/jump identity.
        infantry.setInternal(9, ConvInfantry.LOC_INFANTRY);
        selection = UnitModelSelection.capture(infantry, -1, false, tileset);
        assertEquals(movement.name(), selection.variant());
        assertEquals(3, selection.figures());
    }

    @Test
    void movementFormationsRetainLegacyAndUnsupportedModeFallbacks() {
        var descriptor = new JsonReader().parse("""
              {"kind":"formation", "fallback":"one.g3dj",
               "formations":{"0":"empty.g3dj", "3":"foot-three.g3dj", "6":"foot-six.g3dj"},
               "movementFormations":{
                 "TRACKED":{"0":"tracked-empty.g3dj", "3":"tracked-three.g3dj", "6":"tracked-six.g3dj"},
                 "HOVER":{"6":"hover-six.g3dj"},
                 "INF_JUMP":{"6":"jump-six.g3dj"}}}
              """);
        assertEquals("tracked-three.g3dj", GpuUnitModels.selectModel(descriptor, "TRACKED", 3));
        assertEquals("tracked-six.g3dj", GpuUnitModels.selectModel(descriptor, "TRACKED", 6));
        assertEquals("tracked-empty.g3dj", GpuUnitModels.selectModel(descriptor, "TRACKED", 0));
        assertEquals("hover-six.g3dj", GpuUnitModels.selectModel(descriptor, "HOVER", 6));
        assertEquals("jump-six.g3dj", GpuUnitModels.selectModel(descriptor, "INF_JUMP", 6));
        assertEquals("empty.g3dj", GpuUnitModels.selectModel(descriptor, "HOVER", 0));
        assertEquals("foot-six.g3dj", GpuUnitModels.selectModel(descriptor, "INF_UMU", 6));
        assertEquals("foot-six.g3dj", GpuUnitModels.selectModel(descriptor, "Rifle Platoon", 6));
        assertEquals("one.g3dj", GpuUnitModels.selectModel(descriptor, "HOVER", 8));
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
        assertEquals("six.g3dj", GpuUnitModels.selectModel(infantry, "TRACKED", 6));
    }
}
