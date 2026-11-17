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

package megamek.client.ui.clientGUI.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ui.Messages;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that the one-line crew summary used by compact unit tooltips (such as the Choose Target dialog) says when a
 * unit's crew is dead or unconscious, so a vehicle whose crew was killed can be told apart from one that is only
 * immobilized.
 */
class PilotToolTipCrewStatusTest {

    private static final String CREW_DEAD = Messages.getString("BoardView1.Tooltip.CrewDead");
    private static final String CREW_UNCONSCIOUS = Messages.getString("BoardView1.Tooltip.CrewUnconscious");

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private Tank tankInGame() {
        Tank tank = new Tank();
        tank.setCrew(new Crew(CrewType.CREW));
        tank.setGame(new Game());
        return tank;
    }

    @Test
    @DisplayName("A healthy crew gets no status")
    void healthyCrewHasNoStatus() {
        assertEquals("", PilotToolTip.crewStatusDescription(new Crew(CrewType.CREW)));
    }

    @Test
    @DisplayName("A dead crew is reported as dead")
    void deadCrewIsReported() {
        Crew crew = new Crew(CrewType.CREW);
        crew.setDead(true);
        assertEquals(CREW_DEAD, PilotToolTip.crewStatusDescription(crew));
    }

    @Test
    @DisplayName("A crew killed this phase (doomed) is already reported as dead")
    void doomedCrewIsReportedAsDead() {
        Crew crew = new Crew(CrewType.CREW);
        crew.setDoomed(true);
        assertEquals(CREW_DEAD, PilotToolTip.crewStatusDescription(crew));
    }

    @Test
    @DisplayName("An unconscious crew is reported as unconscious")
    void unconsciousCrewIsReported() {
        Crew crew = new Crew(CrewType.CREW);
        crew.setUnconscious(true);
        assertEquals(CREW_UNCONSCIOUS, PilotToolTip.crewStatusDescription(crew));
    }

    @Test
    @DisplayName("A two-person cockpit with one member still able to act gets no status")
    void partlyActiveCrewHasNoStatus() {
        Crew crew = new Crew(CrewType.DUAL);
        crew.setDead(true, 0);
        assertEquals("", PilotToolTip.crewStatusDescription(crew));
    }

    @Test
    @DisplayName("The one-line crew summary names a dead crew")
    void crewLineShowsDeadCrew() {
        Tank tank = tankInGame();
        tank.getCrew().setDead(true);
        String crewLine = PilotToolTip.getPilotTipLine(tank).toString();
        assertTrue(crewLine.contains(CREW_DEAD), "The crew line should say the crew is dead: " + crewLine);
    }

    @Test
    @DisplayName("The one-line crew summary of a healthy crew carries no crew status")
    void crewLineOfHealthyCrewHasNoStatus() {
        Tank tank = tankInGame();
        String crewLine = PilotToolTip.getPilotTipLine(tank).toString();
        assertFalse(crewLine.contains(CREW_DEAD), "A healthy crew line should not say the crew is dead: " + crewLine);
        assertFalse(crewLine.contains(CREW_UNCONSCIOUS),
              "A healthy crew line should not say the crew is unconscious: " + crewLine);
    }
}
