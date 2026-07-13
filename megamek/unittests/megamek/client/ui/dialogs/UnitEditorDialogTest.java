/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.awt.Polygon;
import java.awt.Rectangle;
import javax.swing.JFrame;

import megamek.client.ui.dialogs.unitDisplay.ArmorPanel;
import megamek.client.ui.widget.picmap.PMSimplePolygonArea;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the per-location layout of the unit damage editor: the dialog must build for every unit type, since
 * each type maps its systems to location panels differently, and drawing the armor diagram must leave the unit
 * itself untouched.
 * <p>
 * Note: these tests require a graphical environment and are skipped in headless CI environments.
 */
class UnitEditorDialogTest {

    @BeforeAll
    static void beforeAll() {
        assumeFalse(GraphicsEnvironment.isHeadless(),
              "Skipping GUI tests - no display available in headless environment");
        EquipmentType.initializeTypes();
    }

    @ParameterizedTest
    @CsvSource({
          "Atlas AS7-D, false",             // biped Mek
          "Triskelion TRK-4V, false",       // tripod Mek
          "Boreas C, false",                // QuadVee
          "Shadow Hawk LAM SHD-X2, false",  // LAM
          "Bulldog Medium Tank, true",      // Tank with turret
          "Cobra Transport VTOL, true",     // VTOL
          "Cheetah F-11, true",             // aerospace fighter
          "Leopard (2537), true",           // DropShip
          "Explorer JumpShip, true",        // JumpShip
          "Centaur, true",                  // ProtoMek
          "Hantu AIX-210(Sqd5), true",      // BattleArmor
    })
    void dialogBuildsForEveryUnitType(String unitName, boolean isBlk) {
        Entity entity = MMTestUtilities.getEntityForUnitTesting(unitName, isBlk);
        assertNotNull(entity, "Test unit could not be loaded: " + unitName);
        new Game().addEntity(entity);

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);

        assertNotNull(dialog);
        dialog.dispose();
    }

    /**
     * Drawing the armor diagram writes the dialog's pending values into the unit and puts the unit back
     * afterwards. If the unit were left holding those values, callers that send the unit to the server after the
     * dialog closes would send edits the user never confirmed, even after Cancel.
     */
    @Test
    void drawingTheArmorDiagramLeavesTheUnitUnchanged() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        new Game().addEntity(entity);
        // give the unit some damage, so that a failure to restore cannot be mistaken for an undamaged unit
        entity.setArmor(4, Mek.LOC_CENTER_TORSO, false);
        entity.setArmor(2, Mek.LOC_CENTER_TORSO, true);
        entity.setInternal(7, Mek.LOC_CENTER_TORSO);

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);

        assertEquals(4, entity.getArmor(Mek.LOC_CENTER_TORSO, false), "front armor was changed");
        assertEquals(2, entity.getArmor(Mek.LOC_CENTER_TORSO, true), "rear armor was changed");
        assertEquals(7, entity.getInternal(Mek.LOC_CENTER_TORSO), "internal structure was changed");
        dialog.dispose();
    }

    /**
     * The armor diagram is enlarged in the dialog. Its areas keep their unscaled coordinates, so a click must be
     * scaled back into them, or clicks would land on the wrong location.
     */
    @Test
    void clickingAnEnlargedDiagramFindsTheClickedLocation() {
        ArmorPanel diagram = new ArmorPanel(null, location -> { });
        Polygon area = new Polygon(new int[] { 10, 30, 30, 10 }, new int[] { 10, 10, 30, 30 }, 4);
        PMSimplePolygonArea polygonArea = new PMSimplePolygonArea(area, null, Mek.LOC_HEAD);
        diagram.addElement(polygonArea);

        diagram.setDisplayScale(3.0);

        // At triple size the area's center is drawn beyond the area's own coordinates, so it can only be found
        // there if the click is scaled back into those coordinates.
        Rectangle bounds = polygonArea.getBounds();
        int centerX = bounds.x + (bounds.width / 2);
        int centerY = bounds.y + (bounds.height / 2);
        assertEquals(polygonArea, diagram.getAreaUnder(centerX * 3, centerY * 3),
              "the enlarged area was not found under the point it is drawn at");
        assertNull(diagram.getAreaUnder((bounds.x + bounds.width + 2) * 3, centerY * 3),
              "an area was found past the edge of the enlarged diagram");
    }

    /** Picking a location in the armor diagram must work for every location the unit has. */
    @Test
    void selectingAnyLocationIsHandled() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        new Game().addEntity(entity);
        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);

        for (int location = 0; location < entity.locations(); location++) {
            int selected = location;
            assertDoesNotThrow(() -> dialog.locationSelected(selected), "location " + selected);
        }
        dialog.dispose();
    }
}
