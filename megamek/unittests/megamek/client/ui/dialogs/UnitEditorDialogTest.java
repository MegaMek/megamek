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
import megamek.client.ui.dialogs.unitEditor.UnitDamagePanelBuilder;
import megamek.client.ui.widget.picmap.PMSimplePolygonArea;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.TemporarySkillModifiers;
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

    /** The buttons that take a unit out of play, which are offered under the same rules. */
    private static final String[] TAKE_OUT_OF_PLAY_BUTTONS = {
          "UnitEditorDialog.withdrawUnit", "UnitEditorDialog.destroyUnit"
    };

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
     * The crew hits control reaches the six hits that kill (TW p.41), so a gamemaster can kill a pilot outright.
     * A unit whose whole crew is dead is then destroyed by the server when the edit reaches it in play.
     */
    @Test
    void crewHitsCanReachTheHitsThatKill() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        new Game().addEntity(entity);
        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);

        JSpinner crewHits = findCrewHitsSpinner(dialog);
        assertNotNull(crewHits, "the dialog has no crew hits control");
        assertEquals(Crew.DEATH, ((SpinnerNumberModel) crewHits.getModel()).getMaximum(),
              "the crew hits control cannot reach the hits that kill");
        crewHits.setValue(Crew.DEATH);
        clickOkay(dialog);
        assertTrue(entity.getCrew().isDead(), "six crew hits did not kill the pilot");
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
    void takingAUnitOutOfPlayIsOnlyOfferedInGame() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        Game game = new Game();
        game.addEntity(entity);
        Client client = mock(Client.class);

        game.setPhase(GamePhase.LOUNGE);
        UnitEditorDialog lobbyDialog = new UnitEditorDialog(new JFrame(), entity, true, client);
        for (String button : TAKE_OUT_OF_PLAY_BUTTONS) {
            JButton lobbyButton = findButton(lobbyDialog.getContentPane(), Messages.getString(button));
            assertNotNull(lobbyButton, "the dialog has no " + button + " button");
            assertFalse(lobbyButton.isEnabled(), button + " was offered in the lobby");
        }
        lobbyDialog.dispose();

        game.setPhase(GamePhase.MOVEMENT);
        UnitEditorDialog gameDialog = new UnitEditorDialog(new JFrame(), entity, true, client);
        for (String button : TAKE_OUT_OF_PLAY_BUTTONS) {
            JButton gameButton = findButton(gameDialog.getContentPane(), Messages.getString(button));
            assertNotNull(gameButton);
            assertTrue(gameButton.isEnabled(), button + " was not offered in game");
        }
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

        for (String button : TAKE_OUT_OF_PLAY_BUTTONS) {
            JButton takeOutOfPlay = findButton(dialog.getContentPane(), Messages.getString(button));
            assertNotNull(takeOutOfPlay);
            assertFalse(takeOutOfPlay.isEnabled(), button + " was offered without a client to reach the server");
        }
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

    /**
     * The unit's conditions moved here from Configure, which is a lobby dialog about how a unit is built and
     * deployed. In play a gamemaster wants the unit's condition, so shutting a unit down is done here.
     */
    @Test
    void theUnitsConditionsAreAppliedToTheUnit() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        new Game().addEntity(entity);
        assertFalse(entity.isShutDown(), "the test needs a running unit to shut down");
        assertFalse(entity.isProne(), "the test needs a standing unit to knock down");

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        dialog.controlsForTesting().chkShutdown.setSelected(true);
        dialog.controlsForTesting().chkProne.setSelected(true);
        clickOkay(dialog);

        assertTrue(entity.isShutDown(), "the unit was not shut down");
        assertTrue(entity.isProne(), "the unit was not knocked prone");
        dialog.dispose();
    }

    /** A unit that was shut down can be started again, which is the reason a gamemaster reaches for this. */
    @Test
    void aShutDownUnitCanBeStartedAgain() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        new Game().addEntity(entity);
        entity.performManualShutdown();
        assertTrue(entity.isShutDown(), "the test needs a shut down unit to start");

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        assertTrue(dialog.controlsForTesting().chkShutdown.isSelected(),
              "the unit's shut down state was not read from the unit");
        dialog.controlsForTesting().chkShutdown.setSelected(false);
        clickOkay(dialog);

        assertFalse(entity.isShutDown(), "the unit was not started again");
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

    /** A gamemaster can hand a crew a temporary skill modifier, applied to the crew's effective skills on Okay. */
    @Test
    void skillModifiersAreAppliedToTheCrew() {
        Entity entity = entityInGame();
        int baseGunnery = entity.getCrew().getGunnery();

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        dialog.controlsForTesting().spnGunneryModifier.setValue(1);
        dialog.controlsForTesting().spnGunneryRounds.setValue(3);
        clickOkay(dialog);

        assertEquals(baseGunnery + 1, entity.getCrew().getGunnery(), "the gunnery modifier was not applied");
        assertEquals(3, entity.getCrew().getSkillModifiers().getGunneryRounds(), "the duration was not applied");
        dialog.dispose();
    }

    /** Each modifier carries a duration of its own: one can count down while another is permanent. */
    @Test
    void eachModifierIsAppliedWithItsOwnDuration() {
        Entity entity = entityInGame();

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        dialog.controlsForTesting().spnGunneryModifier.setValue(1);
        dialog.controlsForTesting().spnGunneryRounds.setValue(2);
        dialog.controlsForTesting().spnPilotingModifier.setValue(-1);
        dialog.controlsForTesting().chkPilotingPermanent.setSelected(true);
        clickOkay(dialog);

        assertEquals(2, entity.getCrew().getSkillModifiers().getGunneryRounds(),
              "the gunnery duration was not applied");
        assertEquals(TemporarySkillModifiers.PERMANENT, entity.getCrew().getSkillModifiers().getPilotingRounds(),
              "the piloting modifier was not applied as permanent");
        dialog.dispose();
    }

    /** The controls read an active modifier back, and setting every delta to zero takes it off the crew. */
    @Test
    void zeroedModifiersClearAnActiveModifier() {
        Entity entity = entityInGame();
        entity.getCrew().getSkillModifiers().set(2, 0, 0, TemporarySkillModifiers.PERMANENT);

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        assertEquals(2, dialog.controlsForTesting().spnGunneryModifier.getValue(),
              "the active modifier was not read from the crew");
        assertTrue(dialog.controlsForTesting().chkGunneryPermanent.isSelected(),
              "a permanent modifier was not read back as permanent");
        dialog.controlsForTesting().spnGunneryModifier.setValue(0);
        clickOkay(dialog);

        assertFalse(entity.getCrew().getSkillModifiers().isActive(), "the modifier was not cleared");
        dialog.dispose();
    }

    /**
     * The skill spinners run from what improves the skill to 0 up to what worsens it to 8, so no offered value is
     * ever lost to the skill range - and an oversized modifier set through /skillMod reads back as what applies.
     */
    @Test
    void skillModifierSpinnerIsBoundedByTheCrewSkill() {
        Entity entity = entityInGame();
        // the Atlas crew is a 4 gunner; an oversized -8 from /skillMod can only ever apply as -4
        entity.getCrew().getSkillModifiers().set(-8, 0, 0, 3);

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        SpinnerNumberModel model = (SpinnerNumberModel) dialog.controlsForTesting().spnGunneryModifier.getModel();

        assertEquals(-4, ((Number) model.getMinimum()).intValue(), "the spinner must bottom out at skill 0");
        assertEquals(4, ((Number) model.getMaximum()).intValue(), "the spinner must top out at skill 8");
        assertEquals(-4, ((Number) model.getValue()).intValue(),
              "an oversized modifier must read back as what applies");
        dialog.dispose();
    }

    /** The initiative modifier only does anything under individual initiative, so its row only appears there. */
    @Test
    void initiativeModifierIsOnlyOfferedUnderIndividualInitiative() {
        Entity entity = entityInGame();
        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        assertNull(dialog.controlsForTesting().spnInitiativeModifier,
              "the initiative modifier was offered without individual initiative");
        dialog.dispose();

        entity.getGame().getOptions().getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE).setValue(true);
        dialog = new UnitEditorDialog(new JFrame(), entity, true);
        assertNotNull(dialog.controlsForTesting().spnInitiativeModifier,
              "the initiative modifier was not offered under individual initiative");
        dialog.dispose();
    }

    /** In the lobby the crew's real skills are edited directly, so the modifier controls are not offered there. */
    @Test
    void skillModifiersAreOnlyOfferedInGame() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        Game game = new Game();
        game.addEntity(entity);
        game.setPhase(GamePhase.LOUNGE);

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);

        assertNull(dialog.controlsForTesting().spnGunneryModifier,
              "the skill modifier controls were offered in the lobby");
        dialog.dispose();
    }

    /**
     * Restore Unit sweeps every spinner to its maximum to repair the unit, which would turn the skill modifier
     * spinners into a +8 skill penalty. It must clear the modifier controls instead.
     */
    @Test
    void restoreUnitClearsTheSkillModifierControls() {
        Entity entity = entityInGame();

        UnitEditorDialog dialog = new UnitEditorDialog(new JFrame(), entity, true);
        dialog.controlsForTesting().spnGunneryModifier.setValue(2);
        dialog.controlsForTesting().chkGunneryPermanent.setSelected(true);
        JButton restore = findButton(dialog.getContentPane(),
              Messages.getString("UnitEditorDialog.preExistingDamage.reset"));
        assertNotNull(restore, "the dialog has no Restore Unit button");
        restore.doClick();

        assertEquals(0, dialog.controlsForTesting().spnGunneryModifier.getValue(),
              "Restore Unit left a skill modifier behind");
        assertFalse(dialog.controlsForTesting().chkGunneryPermanent.isSelected(),
              "Restore Unit left the modifier permanent");
        assertEquals(UnitDamagePanelBuilder.DEFAULT_MODIFIER_ROUNDS,
              dialog.controlsForTesting().spnGunneryRounds.getValue(),
              "Restore Unit swept the duration to its maximum");
        dialog.dispose();
    }

    /** A unit in a game that has started, which is when the gamemaster's skill modifiers are offered. */
    private static Entity entityInGame() {
        Entity entity = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(entity);
        Game game = new Game();
        game.addEntity(entity);
        game.setPhase(GamePhase.MOVEMENT);
        return entity;
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
        return findSpinnerAfterLabelInContainer(dialog.getContentPane(), labelText);
    }

    private static JSpinner findSpinnerAfterLabelInContainer(Container container, String labelText) {
        Component[] components = container.getComponents();
        for (int index = 0; index < components.length; index++) {
            if ((components[index] instanceof JLabel label)
                  && label.getText().contains(labelText)
                  && (index + 1 < components.length)
                  && (components[index + 1] instanceof JSpinner spinner)) {
                return spinner;
            }
            if (components[index] instanceof Container child) {
                JSpinner found = findSpinnerAfterLabelInContainer(child, labelText);
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
            assertDoesNotThrow(() -> dialog.diagramForTesting().locationSelected(selected), "location " + selected);
        }
        dialog.dispose();
    }
}
