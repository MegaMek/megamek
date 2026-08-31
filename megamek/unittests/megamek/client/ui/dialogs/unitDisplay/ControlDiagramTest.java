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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;

import megamek.client.ui.dialogs.unitDisplay.AbstractLocationDiagram.DiagramChoice;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the Control tab's diagram: which cards a unit gets, what choosing one does to the borrowed panels, and what a
 * game master gets that a player does not.
 */
@DisplayName("Control tab diagram")
class ControlDiagramTest {

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping GUI tests - no display available");
    }

    /** A Systems panel that records what it is asked, and lists the locations it is told to. */
    private static final class RecordingSystems extends SystemPanel {
        final List<Integer> selected = new ArrayList<>();
        int allEquipmentShown = 0;
        boolean listsLocations = true;

        RecordingSystems(UnitDisplayPanel owner) {
            super(owner);
        }

        @Override
        public boolean selectLocation(int loc) {
            selected.add(loc);
            return listsLocations;
        }

        @Override
        public void showAllEquipment() {
            allEquipmentShown++;
        }
    }

    private static final class Fixture {
        final RecordingSystems systems;
        final PilotPanel crew;
        final ExtraPanel extras;
        final List<Integer> edited = new ArrayList<>();
        final ControlDiagram diagram;

        Fixture(Entity entity, boolean gameMaster) {
            UnitDisplayPanel owner = new UnitDisplayPanel(null, null);
            systems = new RecordingSystems(owner);
            crew = new PilotPanel(owner);
            extras = new ExtraPanel(owner);
            diagram = new ControlDiagram(entity, entity.getGame(), systems, crew, extras, gameMaster, edited::add);
        }
    }

    private static BipedMek createMek(int id) {
        Game game = new Game();
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(id);
        mek.setChassis("Test Mek");
        mek.setModel("Standard");
        mek.setWeight(50);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(16, location);
        }
        mek.initializeRearArmor(5, Mek.LOC_CENTER_TORSO);
        return mek;
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

    private static List<String> labelTexts(Container root) {
        List<String> texts = new ArrayList<>();
        for (JLabel label : descendantsOfType(root, JLabel.class)) {
            texts.add(label.getText());
        }
        return texts;
    }

    @Test
    @DisplayName("A Mek gets a card per location, then the crew and the equipment list")
    void mekGetsACardPerLocation() {
        Fixture fixture = new Fixture(createMek(1), false);

        assertEquals(Mek.LOC_HEAD, fixture.diagram.getShownChoice().location(), "the head is the first location");
        fixture.diagram.selectCard(ControlDiagram.CREW_KEY);
        assertEquals(ControlDiagram.CREW_KEY, fixture.diagram.getShownChoice().key());
        fixture.diagram.selectCard(ControlDiagram.ALL_EQUIPMENT_KEY);
        assertEquals(ControlDiagram.ALL_EQUIPMENT_KEY, fixture.diagram.getShownChoice().key());
        fixture.diagram.selectLocation(Mek.LOC_LEFT_LEG);
        assertEquals(Mek.LOC_LEFT_LEG, fixture.diagram.getShownChoice().location());
    }

    @Test
    @DisplayName("Choosing a location lists its equipment on the Systems panel")
    void choosingALocationDrivesTheSystemsPanel() {
        Fixture fixture = new Fixture(createMek(1), false);
        fixture.systems.selected.clear();

        fixture.diagram.selectLocation(Mek.LOC_RIGHT_ARM);

        assertEquals(List.of(Mek.LOC_RIGHT_ARM), fixture.systems.selected);
        assertTrue(fixture.systems.isVisible(), "the Systems panel is the detail card");
        assertFalse(fixture.crew.isVisible());
        assertEquals(0, fixture.systems.allEquipmentShown);
    }

    @Test
    @DisplayName("Choosing the crew swaps the Pilot panel in for the Systems panel")
    void choosingTheCrewShowsThePilotPanel() {
        Fixture fixture = new Fixture(createMek(1), false);

        fixture.diagram.selectCard(ControlDiagram.CREW_KEY);

        assertTrue(fixture.crew.isVisible());
        assertFalse(fixture.systems.isVisible());

        fixture.diagram.showLocations();
        assertEquals(Mek.LOC_HEAD, fixture.diagram.getShownChoice().location(), "F3 brings the locations back");
        assertTrue(fixture.systems.isVisible());
    }

    @Test
    @DisplayName("A location with nothing of its own to list falls back to all equipment")
    void locationWithoutSlotsFallsBackToAllEquipment() {
        Fixture fixture = new Fixture(createMek(1), false);
        fixture.systems.listsLocations = false;
        fixture.systems.allEquipmentShown = 0;

        fixture.diagram.selectLocation(Mek.LOC_LEFT_TORSO);

        assertEquals(1, fixture.systems.allEquipmentShown);
    }

    @Test
    @DisplayName("Only a game master gets the Edit damage row, and it opens at the card's location")
    void onlyAGameMasterGetsTheEditDamageRow() {
        Fixture player = new Fixture(createMek(1), false);
        assertTrue(descendantsOfType(player.diagram.getCardsPanel(), JButton.class).isEmpty(),
              "a player cannot edit damage");

        Fixture gameMaster = new Fixture(createMek(1), true);
        gameMaster.diagram.selectLocation(Mek.LOC_LEFT_ARM);
        List<JButton> buttons = descendantsOfType(gameMaster.diagram.getCardsPanel(), JButton.class);
        assertEquals(gameMaster.diagram.getEntity().locations(), buttons.size(), "one row per location card");

        for (JButton button : buttons) {
            if (button.isShowing() || button.getParent().isVisible()) {
                button.doClick();
            }
        }
        assertEquals(List.of(Mek.LOC_LEFT_ARM), gameMaster.edited, "the shown card's row opens at its location");
    }

    @Test
    @DisplayName("The cards show armor and structure, and follow the unit when refreshed")
    void cardsFollowTheUnit() {
        BipedMek mek = createMek(1);
        Fixture fixture = new Fixture(mek, false);
        fixture.diagram.selectLocation(Mek.LOC_CENTER_TORSO);
        assertTrue(labelTexts(fixture.diagram.getCardsPanel()).contains("16 / 16"), "front armor");
        assertTrue(labelTexts(fixture.diagram.getCardsPanel()).contains("5 / 5"), "rear armor");

        mek.setArmor(3, Mek.LOC_CENTER_TORSO);
        fixture.diagram.refresh();

        assertTrue(labelTexts(fixture.diagram.getCardsPanel()).contains("3 / 16"), "the card follows the damage");
        assertEquals(Mek.LOC_CENTER_TORSO, fixture.diagram.getShownChoice().location(),
              "a refresh keeps the chosen card");
    }

    @Test
    @DisplayName("A fresh copy of the same unit is shown in place; another unit is not")
    void sameUnitIsRecognised() {
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
    void borrowedPanelsCanBeReattached() {
        Fixture fixture = new Fixture(createMek(1), false);
        Container elsewhere = new Container();
        elsewhere.add(fixture.systems);
        elsewhere.add(fixture.extras);
        assertEquals(elsewhere, fixture.systems.getParent());

        fixture.diagram.attachPanels();

        assertEquals(0, elsewhere.getComponentCount(), "attaching takes them back");
        fixture.diagram.selectLocation(Mek.LOC_LEFT_LEG);
        DiagramChoice shown = fixture.diagram.getShownChoice();
        assertEquals(Mek.LOC_LEFT_LEG, shown.location());
        assertTrue(fixture.systems.isVisible());
    }
}
