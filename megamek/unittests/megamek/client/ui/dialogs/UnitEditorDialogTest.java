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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.mock;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Polygon;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.unitDisplay.ArmorPanel;
import megamek.client.ui.dialogs.unitEditor.CheckCritPanel;
import megamek.client.ui.widget.picmap.PMSimplePolygonArea;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.Crew;
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

    /**
     * The crew hits control stops one short of the six hits that kill (TW p.41), so a gamemaster wounds and
     * revives crew here but cannot kill a pilot outright; destroying a unit is what the Destroy Unit button does.
     */
    @Test
    void crewHitsCannotReachTheHitsThatKill() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        new Game().addEntity(entity);
        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);

        JSpinner crewHits = findCrewHitsSpinner(dialog);
        assertNotNull(crewHits, "the dialog has no crew hits control");
        assertEquals(Crew.DEATH - 1, ((SpinnerNumberModel) crewHits.getModel()).getMaximum(),
              "the crew hits control can reach the hits that kill");
        dialog.dispose();
    }

    /**
     * A crew member killed in play must come back when their hits are lowered. Crew.setHits kills at six hits but
     * never undoes it, so the dialog has to clear the death itself.
     */
    @Test
    void loweringHitsRevivesADeadCrewMember() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        new Game().addEntity(entity);
        entity.getCrew().setHits(Crew.DEATH, 0);
        assertTrue(entity.getCrew().isDead(), "the test needs a dead crew member to revive");

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        JSpinner crewHits = findCrewHitsSpinner(dialog);
        assertNotNull(crewHits);
        crewHits.setValue(2);
        clickOkay(dialog);

        assertFalse(entity.getCrew().isDead(), "the crew member was not revived");
        assertEquals(2, entity.getCrew().getHits(0), "the crew hits were not applied");
        dialog.dispose();
    }

    /**
     * There is nothing in play to destroy in the lobby, where an unwanted unit is simply removed, so the button is
     * only offered once the game has started.
     */
    @Test
    void destroyUnitIsOnlyOfferedInGame() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        Game game = new Game();
        game.addEntity(entity);
        Client client = mock(Client.class);

        game.setPhase(GamePhase.LOUNGE);
        UnitEditorDialog lobbyDialog = new UnitEditorDialog(new JFrame(), entity, true, client);
        JButton lobbyButton = findButton(lobbyDialog.getContentPane(),
              Messages.getString("UnitEditorDialog.destroyUnit"));
        assertNotNull(lobbyButton, "the dialog has no Destroy Unit button");
        assertFalse(lobbyButton.isEnabled(), "Destroy Unit was offered in the lobby");
        lobbyDialog.dispose();

        game.setPhase(GamePhase.MOVEMENT);
        UnitEditorDialog gameDialog = new UnitEditorDialog(new JFrame(), entity, true, client);
        JButton gameButton = findButton(gameDialog.getContentPane(),
              Messages.getString("UnitEditorDialog.destroyUnit"));
        assertNotNull(gameButton);
        assertTrue(gameButton.isEnabled(), "Destroy Unit was not offered in game");
        gameDialog.dispose();
    }

    /** Without a client there is no server to destroy the unit, as in MekHQ and MegaMekLab. */
    @Test
    void destroyUnitIsNotOfferedWithoutAClient() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        Game game = new Game();
        game.addEntity(entity);
        game.setPhase(GamePhase.MOVEMENT);

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);

        JButton destroy = findButton(dialog.getContentPane(), Messages.getString("UnitEditorDialog.destroyUnit"));
        assertNotNull(destroy);
        assertFalse(destroy.isEnabled(), "Destroy Unit was offered without a client to destroy through");
        dialog.dispose();
    }

    /**
     * A gamemaster can refill an ammo bin. Critting the bin empties it, since a destroyed bin holds nothing, and
     * taking the crit off restores the bin and the shots that were in it.
     */
    @Test
    void aGameMasterCanRefillAnAmmoBin() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        new Game().addEntity(entity);
        AmmoMounted ammoBin = entity.getAmmo().getFirst();
        int equipmentNumber = entity.getEquipmentNum(ammoBin);
        ammoBin.setShotsLeft(3);

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        JSpinner shots = dialog.controlsForTesting().ammoShots.get(equipmentNumber);
        assertNotNull(shots, "the gamemaster was given no way to set the shots in a bin");
        assertEquals(3, shots.getValue(), "the bin's shots were not read from the unit");

        // critting the bin empties it and locks the count
        CheckCritPanel crit = dialog.controlsForTesting().equipCrits.get(equipmentNumber);
        crit.setHits(1);
        assertEquals(0, shots.getValue(), "critting the bin did not empty it");
        assertFalse(shots.isEnabled(), "the shots of a destroyed bin can still be set");

        // taking the crit off restores the bin and what was in it
        crit.setHits(0);
        assertEquals(3, shots.getValue(), "the bin's shots were not restored with the bin");
        assertTrue(shots.isEnabled(), "the restored bin's shots cannot be set");

        shots.setValue(11);
        clickOkay(dialog);

        assertFalse(ammoBin.isDestroyed(), "the bin was not restored on the unit");
        assertEquals(11, ammoBin.getBaseShotsLeft(), "the bin was not refilled on the unit");
        dialog.dispose();
    }

    /** MekHQ counts what is in a bin itself, as campaign stock, so the editor does not offer to set it there. */
    @Test
    void ammoCannotBeSetWithoutTheGameMasterTools() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        new Game().addEntity(entity);

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity);

        assertTrue(dialog.controlsForTesting().ammoShots.isEmpty(),
              "ammo can be set without the gamemaster tools, which MekHQ opens the editor without");
        dialog.dispose();
    }

    /** Heat is set through the diagram's own heat scale, so the editor must apply it to the unit. */
    @Test
    void heatIsAppliedToTheUnit() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        new Game().addEntity(entity);
        assertTrue(entity.tracksHeat(), "the test needs a unit that tracks heat");

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        JSpinner heat = findSpinnerAfterLabel(dialog, Messages.getString("UnitEditorDialog.heat"));
        assertNotNull(heat, "the dialog has no heat control");
        heat.setValue(12);
        clickOkay(dialog);

        assertEquals(12, entity.heat, "the heat was not applied");
        dialog.dispose();
    }

    /** Presses the dialog's Okay button, which is what applies the edits to the unit. */
    private static void clickOkay(UnitEditorDialog dialog) {
        JButton okay = findButton(dialog.getContentPane(), Messages.getString("Okay"));
        assertNotNull(okay, "the dialog has no Okay button");
        okay.doClick();
    }

    private static JSpinner findCrewHitsSpinner(UnitEditorDialog dialog) {
        return findSpinnerAfterLabel(dialog, Messages.getString("UnitEditorDialog.crewHits"));
    }

    /** Finds the spinner that sits next to the given label, which is how the dialog lays out its controls. */
    private static JSpinner findSpinnerAfterLabel(UnitEditorDialog dialog, String labelText) {
        return findSpinnerAfterLabel(dialog.getContentPane(), labelText);
    }

    private static JSpinner findSpinnerAfterLabel(Container container, String labelText) {
        Component[] components = container.getComponents();
        for (int index = 0; index < components.length; index++) {
            if ((components[index] instanceof JLabel label)
                  && label.getText().contains(labelText)
                  && (index + 1 < components.length)
                  && (components[index + 1] instanceof JSpinner spinner)) {
                return spinner;
            }
            if (components[index] instanceof Container child) {
                JSpinner found = findSpinnerAfterLabel(child, labelText);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton findButton(Container container, String text) {
        for (Component component : container.getComponents()) {
            if ((component instanceof JButton button) && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton found = findButton(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
