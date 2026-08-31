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
package megamek.client.ui.dialogs.unitDisplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.unitEditor.CheckCritPanel;
import megamek.client.ui.dialogs.unitEditor.UnitDamageControls;
import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Tank;
import megamek.common.units.InfantryCompartment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the Control tab's diagram: that its cards are the GM editor's panels built in live mode, what choosing one
 * does, how it follows the unit, and what a game master gets that a player does not.
 */
@DisplayName("Control tab diagram")
class ControlDiagramTest {

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping GUI tests - no display available");
        EquipmentType.initializeTypes();
    }

    private static final class Fixture {
        final PilotPanel crew;
        final ExtraPanel extras;
        final List<Integer> edited = new ArrayList<>();
        final ControlDiagram diagram;

        Fixture(Entity entity, boolean gameMaster) {
            UnitDisplayPanel owner = new UnitDisplayPanel(null, null);
            crew = new PilotPanel(owner);
            extras = new ExtraPanel(owner);
            diagram = new ControlDiagram(entity, owner, crew, extras, gameMaster, edited::add);
        }
    }

    private static BipedMek createMek(int id) throws Exception {
        Game game = new Game();
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(id);
        mek.setOwner(new Player(0, "Tester"));
        mek.setChassis("Test Mek");
        mek.setModel("Standard");
        mek.setWeight(50);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(16, location);
        }
        mek.initializeRearArmor(5, Mek.LOC_CENTER_TORSO);
        mek.addEquipment(EquipmentType.get("ISMediumLaser"), Mek.LOC_RIGHT_ARM);
        mek.addEquipment(EquipmentType.get("ISGuardianECMSuite"), Mek.LOC_LEFT_TORSO);
        return mek;
    }

    private static List<JButton> editButtons(ControlDiagram diagram) {
        String editText = Messages.getString("UnitDisplay.controlTab.editDamage");
        List<JButton> buttons = new ArrayList<>();
        for (JButton button : descendantsOfType(diagram.getCardsPanel(), JButton.class)) {
            if (editText.equals(button.getText())) {
                buttons.add(button);
            }
        }
        return buttons;
    }

    private static <T extends Component> List<T> descendantsOfType(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                found.addAll(descendantsOfType(container, type));
            }
        }
        return found;
    }

    @Test
    @DisplayName("The cards are the GM editor's location panels, and the crew comes last")
    void cardsAreTheEditorsLocationPanels() throws Exception {
        Fixture fixture = new Fixture(createMek(1), false);
        UnitDamageControls controls = fixture.diagram.getControls();

        assertEquals(Mek.LOC_HEAD, fixture.diagram.getShownChoice().location(), "the head is the first location");
        for (int location = 0; location < Mek.LOC_LEFT_LEG + 1; location++) {
            assertNotNull(controls.locationPanels[location], "panel for location " + location);
        }
        fixture.diagram.selectLocation(Mek.LOC_LEFT_TORSO);
        assertTrue(controls.locationPanels[Mek.LOC_LEFT_TORSO].isVisible(), "the chosen panel is the card showing");
        assertFalse(controls.locationPanels[Mek.LOC_HEAD].isVisible());

        fixture.diagram.selectCard(ControlDiagram.CREW_KEY);
        assertTrue(fixture.crew.isVisible(), "the Pilot panel is the crew card");
        assertFalse(controls.locationPanels[Mek.LOC_LEFT_TORSO].isVisible());
        fixture.diagram.showLocations();
        assertEquals(Mek.LOC_HEAD, fixture.diagram.getShownChoice().location(), "F3 brings the locations back");
    }

    @Test
    @DisplayName("Live mode shows armor, structure and crits without letting them be edited")
    void liveModeShowsButDoesNotEdit() throws Exception {
        BipedMek mek = createMek(1);
        Fixture fixture = new Fixture(mek, false);
        UnitDamageControls controls = fixture.diagram.getControls();

        JSpinner armor = controls.spnArmor[Mek.LOC_CENTER_TORSO];
        assertEquals(16, armor.getValue());
        assertFalse(armor.isEnabled(), "a value only shows the state");
        assertEquals(5, controls.spnRear[Mek.LOC_CENTER_TORSO].getValue());
        assertFalse(controls.spnInternal[Mek.LOC_CENTER_TORSO].isEnabled());
        assertFalse(controls.chkShutdown.isEnabled(), "conditions only show the state");

        for (CheckCritPanel crit : controls.equipCrits.values()) {
            for (JCheckBox box : descendantsOfType(crit, JCheckBox.class)) {
                assertFalse(box.isEnabled(), "crit boxes only show hits");
            }
        }
        for (JCheckBox box : descendantsOfType(controls.gyroCrit, JCheckBox.class)) {
            assertFalse(box.isEnabled(), "system crits too");
        }
    }

    @Test
    @DisplayName("Equipment with modes gets a live switch; ammo shows its shots as text, not a GM spinner")
    void equipmentGetsLiveSwitches() throws Exception {
        Fixture fixture = new Fixture(createMek(1), false);
        UnitDamageControls controls = fixture.diagram.getControls();

        // the Guardian ECM has two modes (ECM / Off), which is the On/Off switch's case; the laser has none
        assertEquals(1, controls.equipmentOnOff.size() + controls.equipmentModes.size(),
              "one switch, for the ECM suite");
        assertTrue(controls.ammoShots.isEmpty(), "shots are a GM spinner; live mode shows them as text");
        assertTrue(controls.ammoDump.isEmpty(), "no ammo on this unit");
    }

    @Test
    @DisplayName("Only a game master gets the Edit damage row, and it opens at the shown card's location")
    void onlyAGameMasterGetsTheEditDamageRow() throws Exception {
        Fixture player = new Fixture(createMek(1), false);
        assertTrue(editButtons(player.diagram).isEmpty(), "a player cannot edit damage");

        Fixture gameMaster = new Fixture(createMek(1), true);
        gameMaster.diagram.selectLocation(Mek.LOC_LEFT_ARM);
        List<JButton> buttons = editButtons(gameMaster.diagram);
        assertEquals(gameMaster.diagram.getEntity().locations(), buttons.size(), "one row per location card");
        for (JButton button : buttons) {
            if (button.getParent().getParent().isVisible()) {
                button.doClick();
            }
        }
        assertEquals(List.of(Mek.LOC_LEFT_ARM), gameMaster.edited);
    }

    @Test
    @DisplayName("A refresh rebuilds the panels from the unit and keeps the chosen card")
    void refreshFollowsTheUnit() throws Exception {
        BipedMek mek = createMek(1);
        Fixture fixture = new Fixture(mek, false);
        fixture.diagram.selectLocation(Mek.LOC_CENTER_TORSO);

        mek.setArmor(3, Mek.LOC_CENTER_TORSO);
        fixture.diagram.refresh();

        UnitDamageControls controls = fixture.diagram.getControls();
        assertEquals(3, controls.spnArmor[Mek.LOC_CENTER_TORSO].getValue(), "rebuilt from the unit");
        assertEquals(Mek.LOC_CENTER_TORSO, fixture.diagram.getShownChoice().location(), "still the chosen card");
        assertTrue(controls.locationPanels[Mek.LOC_CENTER_TORSO].isVisible());
        assertEquals(fixture.diagram.getCardsPanel(), fixture.crew.getParent(), "the crew is a card again");
    }

    @Test
    @DisplayName("A fresh copy of the same unit is shown in place; another unit is not")
    void sameUnitIsRecognised() throws Exception {
        Fixture fixture = new Fixture(createMek(1), false);

        assertTrue(fixture.diagram.showsSameUnit(createMek(1)), "the server sends a new object for the same unit");
        assertFalse(fixture.diagram.showsSameUnit(createMek(2)));
        Tank tank = new Tank();
        tank.setId(1);
        assertFalse(fixture.diagram.showsSameUnit(tank), "same id, different shape");

        fixture.diagram.setEntity(createMek(1));
        assertThrows(IllegalArgumentException.class, () -> fixture.diagram.setEntity(tank));
    }

    @Test
    @DisplayName("The borrowed panels can be taken away and attached again")
    void borrowedPanelsCanBeReattached() throws Exception {
        Fixture fixture = new Fixture(createMek(1), false);
        Container elsewhere = new Container();
        elsewhere.add(fixture.crew);
        elsewhere.add(fixture.extras);
        assertEquals(elsewhere, fixture.crew.getParent());

        fixture.diagram.attachPanels();

        assertEquals(0, elsewhere.getComponentCount(), "attaching takes them back");
        fixture.diagram.selectCard(ControlDiagram.CREW_KEY);
        assertTrue(fixture.crew.isVisible());
        assertEquals(fixture.diagram.getCardsPanel(), fixture.crew.getParent());
    }

    @Test
    @DisplayName("A carrier offers its carried units in the Unit chooser, the Systems tab's unit list")
    void carriedUnitsAreOffered() throws Exception {
        BipedMek carrier = createMek(1);
        Game game = carrier.getGame();
        game.addPlayer(0, carrier.getOwner());
        carrier.addTransporter(new InfantryCompartment(10));
        ConvInfantry platoon = new ConvInfantry();
        platoon.setGame(game);
        platoon.setId(2);
        platoon.setOwner(carrier.getOwner());
        platoon.setChassis("Rifle Platoon");
        platoon.setModel("");
        platoon.setSquadCount(3);
        platoon.setSquadSize(7);
        platoon.autoSetInternal();
        game.addEntity(carrier);
        game.addEntity(platoon);
        carrier.load(platoon);
        assertEquals(1, carrier.getLoadedUnits().size(), "the platoon is aboard");

        ControlTabPanel tab = new ControlTabPanel(new UnitDisplayPanel(null, null));
        tab.displayMek(carrier);

        JComboBox<?> chooser = descendantsOfType(tab, JComboBox.class).getFirst();
        assertEquals(2, chooser.getItemCount(), "the carrier and its passenger");
        assertEquals(carrier, chooser.getItemAt(0));
        assertEquals(platoon, chooser.getItemAt(1));

        chooser.setSelectedIndex(1);
        assertEquals(platoon, descendantsOfType(tab, ControlDiagram.class).getFirst().getEntity(),
              "choosing the passenger shows the passenger");
    }

    @Test
    @DisplayName("A unit that carries nothing has no Unit chooser row")
    void loneUnitHasNoChooserRow() throws Exception {
        ControlTabPanel tab = new ControlTabPanel(new UnitDisplayPanel(null, null));

        tab.displayMek(createMek(1));

        JComboBox<?> chooser = descendantsOfType(tab, JComboBox.class).getFirst();
        assertEquals(1, chooser.getItemCount());
        assertFalse(chooser.getParent().isVisible(), "the row is hidden when there is nothing to choose");
    }
}
