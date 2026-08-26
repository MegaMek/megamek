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
package megamek.client.ui.clientGUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import megamek.common.Player;
import org.junit.jupiter.api.Test;

/**
 * Verifies which players the person at this screen may add units to.
 *
 * <p>The lobby scenario throughout: Dave is at this screen, Blue and Green are remote humans, Princess-1 is a bot
 * Dave is running and Princess-2 is a bot Blue is running.</p>
 */
class UnitRecipientsTest {

    private static final Player DAVE = new Player(0, "Dave");
    private static final Player BLUE = new Player(1, "Blue");
    private static final Player GREEN = new Player(2, "Green");
    private static final Player PRINCESS_ONE = new Player(3, "Princess-1");
    private static final Player PRINCESS_TWO = new Player(4, "Princess-2");

    private static final List<Player> EVERYONE = List.of(DAVE, BLUE, GREEN, PRINCESS_ONE, PRINCESS_TWO);
    private static final Set<String> DAVES_BOTS = Set.of("Princess-1");

    /** @return the names offered, which is what a chooser shows */
    private static List<String> namesOfferedTo(Player localPlayer) {
        return UnitRecipients.availableTo(localPlayer, EVERYONE, DAVES_BOTS).stream().map(Player::getName).toList();
    }

    @Test
    void anOrdinaryPlayerIsOfferedThemselvesAndTheirOwnBots() {
        DAVE.setGameMaster(false);

        assertEquals(List.of("Dave", "Princess-1"), namesOfferedTo(DAVE),
              "without the gamemaster role the choice is your own force and the bots you run, as it always was");
    }

    @Test
    void aGamemasterIsOfferedEveryone() {
        DAVE.setGameMaster(true);

        List<String> offered = namesOfferedTo(DAVE);

        assertTrue(offered.containsAll(List.of("Dave", "Blue", "Green", "Princess-1", "Princess-2")),
              "handing out forces is what a gamemaster does, so every player is a possible recipient");
        assertEquals(5, offered.size(), "and nobody should be offered twice");
    }

    @Test
    void anotherPlayersBotIsOfferedOnlyToAGamemaster() {
        DAVE.setGameMaster(false);
        assertFalse(namesOfferedTo(DAVE).contains("Princess-2"),
              "Blue runs that bot, so stocking it is Blue's business");

        DAVE.setGameMaster(true);
        assertTrue(namesOfferedTo(DAVE).contains("Princess-2"),
              "a gamemaster may stock it along with everyone else");
    }

    @Test
    void theLocalPlayerIsAlwaysFirst() {
        DAVE.setGameMaster(true);

        assertEquals("Dave", namesOfferedTo(DAVE).getFirst(),
              "a chooser landing on its first entry must land on the person using it");
    }

    @Test
    void theLocalPlayerIsNeverOfferedTwice() {
        DAVE.setGameMaster(true);

        assertEquals(1, namesOfferedTo(DAVE).stream().filter("Dave"::equals).count(),
              "the local player is added at the front, so the sweep must not add them again");
    }

    @Test
    void aGamemasterWithNoOneElseInTheGameIsStillOfferedThemselves() {
        DAVE.setGameMaster(true);

        List<Player> recipients = UnitRecipients.availableTo(DAVE, List.of(DAVE), Set.of());

        assertEquals(List.of("Dave"), recipients.stream().map(Player::getName).toList(),
              "there is always someone to add units to, so a chooser is never left empty");
    }

    @Test
    void theListDoublesAsAPermissionCheck() {
        // the unit list buttons act on whoever is highlighted in the player table rather than on a chooser, so they
        // ask this the other way round: is that player someone I am allowed to act for?
        DAVE.setGameMaster(false);
        List<Player> allowedToAnOrdinaryPlayer = UnitRecipients.availableTo(DAVE, EVERYONE, DAVES_BOTS);

        assertTrue(allowedToAnOrdinaryPlayer.contains(DAVE), "your own force is always yours to load and save");
        assertTrue(allowedToAnOrdinaryPlayer.contains(PRINCESS_ONE), "and so is a bot you are running");
        assertFalse(allowedToAnOrdinaryPlayer.contains(BLUE),
              "but another player's force is not, and asking must say so rather than quietly allowing it");

        DAVE.setGameMaster(true);
        assertTrue(UnitRecipients.availableTo(DAVE, EVERYONE, DAVES_BOTS).contains(BLUE),
              "a gamemaster may act for anyone");
    }
}
