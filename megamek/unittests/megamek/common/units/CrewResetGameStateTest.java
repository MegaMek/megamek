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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link Crew#resetGameState()} is what MekHQ calls between scenarios. Everything a battle can leave on a crew must
 * go, or it carries into the next battle; MekHQ then re-applies each crew member's current injuries
 * (MegaMek/megamek#9719).
 */
class CrewResetGameStateTest {
    private static final int GUNNERY_MODIFIER = 2;
    private static final int MODIFIER_ROUNDS = 3;

    @Test
    void doomedPilotLosesTheBattlesHits() {
        Crew crew = new Crew(CrewType.SINGLE);
        // Being doomed sets every slot to 6 hits, which also marks the pilot dead
        crew.setDoomed(true);

        crew.resetGameState();

        assertEquals(0, crew.getHits());
        assertFalse(crew.isDoomed());
        assertFalse(crew.isDead());
    }

    @Test
    void everyCrewSlotLosesItsHits() {
        Crew crew = new Crew(CrewType.COMMAND_CONSOLE);
        for (int slot = 0; slot < crew.getSlotCount(); slot++) {
            crew.setHits(slot + 1, slot);
        }

        crew.resetGameState();

        for (int slot = 0; slot < crew.getSlotCount(); slot++) {
            assertEquals(0, crew.getHits(slot), "Hits left on slot " + slot);
        }
    }

    @Test
    void roundStateIsCleared() {
        Crew crew = new Crew(CrewType.COMMAND_CONSOLE);
        crew.setKoThisRound(true);
        // Both console seats have flown this battle, so neither can give the command bonus
        crew.setCurrentPilot(1);
        crew.setCurrentPilot(0);
        crew.setSwapConsoleRoles(true);
        assertFalse(crew.hasActiveCommandConsole());

        crew.resetGameState();

        assertTrue(crew.hasActiveCommandConsole(), "The commander seat starts the next battle free to command");
        assertFalse(crew.isKoThisRound());
        assertFalse(crew.getSwapConsoleRoles());
        for (int slot = 0; slot < crew.getSlotCount(); slot++) {
            assertFalse(crew.isKoThisRound(slot), "Knocked out this round on slot " + slot);
        }
    }

    @Test
    void gamemasterSkillModifiersAreCleared() {
        Crew crew = new Crew(CrewType.SINGLE);
        crew.getSkillModifiers().setGunnery(GUNNERY_MODIFIER, MODIFIER_ROUNDS);

        crew.resetGameState();

        assertFalse(crew.getSkillModifiers().isActive());
    }
}
