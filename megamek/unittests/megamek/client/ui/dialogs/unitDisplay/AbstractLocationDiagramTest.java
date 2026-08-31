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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;

import megamek.client.ui.dialogs.unitDisplay.AbstractLocationDiagram.DiagramChoice;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the diagram-plus-cards scaffold on its own: which cards it builds, how a location is chosen, and what a
 * subclass gets told.
 */
@DisplayName("AbstractLocationDiagram")
class AbstractLocationDiagramTest {

    private static final String CREW_KEY = "crew";

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping GUI tests - no display available");
    }

    /** A diagram with a labelled card per location except the head, plus a Crew entry. */
    private static final class TestDiagram extends AbstractLocationDiagram {
        final List<DiagramChoice> shown = new ArrayList<>();

        TestDiagram(Entity entity) {
            super(entity, entity.getGame());
            buildCards();
        }

        @Override
        protected @Nullable JComponent createLocationCard(int location) {
            return (location == Mek.LOC_HEAD) ? null : new JLabel(getEntity().getLocationName(location));
        }

        @Override
        protected List<DiagramChoice> createExtraChoices() {
            return List.of(DiagramChoice.extra(CREW_KEY, "Crew"));
        }

        @Override
        protected @Nullable JComponent createExtraCard(DiagramChoice choice) {
            return new JLabel(choice.name());
        }

        @Override
        protected void onCardShown(DiagramChoice choice) {
            shown.add(choice);
        }
    }

    private static BipedMek createMek() {
        Game game = new Game();
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(1);
        mek.setChassis("Test Mek");
        mek.setModel("Standard");
        mek.setWeight(50);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.autoSetInternal();
        return mek;
    }

    @Test
    @DisplayName("The first card shows as soon as the cards are built")
    void firstCardShowsOnBuild() {
        TestDiagram diagram = new TestDiagram(createMek());

        DiagramChoice shown = diagram.getShownChoice();
        assertNotNull(shown);
        assertEquals(Mek.LOC_CENTER_TORSO, shown.location(), "the head has no card, so the centre torso is first");
        assertEquals(List.of(shown), diagram.shown);
    }

    @Test
    @DisplayName("Clicking a location on the diagram brings its card forward")
    void clickBringsTheLocationForward() {
        TestDiagram diagram = new TestDiagram(createMek());

        diagram.locationSelected(Mek.LOC_LEFT_LEG);

        assertEquals(Mek.LOC_LEFT_LEG, diagram.getShownChoice().location());
        assertTrue(diagram.selectsOnSingleClick(), "one click is enough on a diagram");
    }

    @Test
    @DisplayName("A location without a card is ignored, not shown")
    void locationWithoutCardIsIgnored() {
        TestDiagram diagram = new TestDiagram(createMek());
        diagram.selectLocation(Mek.LOC_RIGHT_ARM);

        diagram.selectLocation(Mek.LOC_HEAD);

        assertEquals(Mek.LOC_RIGHT_ARM, diagram.getShownChoice().location(), "the head has no card");
    }

    @Test
    @DisplayName("An extra entry such as the crew has a card of its own, tied to no location")
    void extraEntryHasItsOwnCard() {
        TestDiagram diagram = new TestDiagram(createMek());

        diagram.selectCard(CREW_KEY);

        DiagramChoice shown = diagram.getShownChoice();
        assertEquals(CREW_KEY, shown.key());
        assertEquals(Entity.LOC_NONE, shown.location());
    }

    @Test
    @DisplayName("An unknown card key leaves the current card alone")
    void unknownKeyIsIgnored() {
        TestDiagram diagram = new TestDiagram(createMek());
        diagram.selectLocation(Mek.LOC_LEFT_ARM);

        diagram.selectCard("no such card");

        assertEquals(Mek.LOC_LEFT_ARM, diagram.getShownChoice().location());
    }

    @Test
    @DisplayName("Entries compare by key, so the chooser can be set by key alone")
    void choicesCompareByKey() {
        DiagramChoice first = new DiagramChoice("location-3", "Left Torso", 3);
        DiagramChoice same = new DiagramChoice("location-3", "LT", 3);
        DiagramChoice other = DiagramChoice.extra("crew", "Crew");

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNull(other.equals(first) ? other : null, "different keys are different entries");
        assertEquals("Left Torso", first.toString(), "the chooser shows the name");
    }
}
