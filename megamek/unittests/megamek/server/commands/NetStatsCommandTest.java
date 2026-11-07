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
package megamek.server.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.stats.NetStats;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetStatsCommandTest {

    private static final int CONN = 3;

    private Server server;
    private Game game;
    private NetStats stats;
    private final List<String> chat = new ArrayList<>();
    private boolean timingBefore;

    @BeforeEach
    void setUp() {
        timingBefore = NetStats.isTimingEnabled();
        server = mock(Server.class);
        game = new Game();
        stats = new NetStats();
        when(server.getGame()).thenReturn(game);
        when(server.getNetStats()).thenReturn(stats);
        Player player = new Player(CONN, "Alice");
        game.addPlayer(CONN, player);
        when(server.getPlayer(CONN)).thenReturn(player);
        doAnswer(invocation -> {
            chat.add(invocation.getArgument(1));
            return null;
        }).when(server).sendServerChat(anyInt(), anyString());
    }

    @AfterEach
    void tearDown() {
        NetStats.setTimingEnabled(timingBefore);
    }

    private void run(String... args) {
        String[] full = new String[args.length + 1];
        full[0] = "netstats";
        System.arraycopy(args, 0, full, 1, args.length);
        new NetStatsCommand(server).run(CONN, full);
    }

    @Test
    void isRegisteredUnderItsName() {
        assertEquals("netstats", new NetStatsCommand(server).getName());
        assertTrue(new NetStatsCommand(server).getHelp().contains("/netstats"));
    }

    @Test
    void noArgumentPrintsTheTableToTheCaller() {
        stats.recordSent(PacketCommand.SENDING_ENTITIES, 5000, 2000, 0);
        stats.recordReceived(PacketCommand.PLAYER_READY, 50, 0);

        run();

        assertTrue(chat.get(0).startsWith("command"), chat.get(0));
        assertTrue(chat.get(1).startsWith("SENDING_ENTITIES"), chat.get(1));
        assertTrue(chat.get(2).startsWith("PLAYER_READY"), chat.get(2));
        assertTrue(chat.stream().anyMatch(line -> line.startsWith("total: 2 packets, 2050 bytes")));
        assertTrue(chat.stream().anyMatch(line -> line.startsWith("pump:")));
    }

    @Test
    void allListsEveryCommandSeen() {
        for (int i = 0; i < NetStatsCommand.DEFAULT_TOP + 5; i++) {
            stats.recordSent(PacketCommand.values()[i], 1, 1, 0);
        }

        run();
        int withDefault = chat.size();
        assertTrue(chat.stream().anyMatch(line -> line.startsWith("other")), "the default view folds the rest");
        chat.clear();

        run("all");

        assertTrue(chat.size() > withDefault);
        assertFalse(chat.stream().anyMatch(line -> line.startsWith("other")));
    }

    @Test
    void resetZeroesTheCounters() {
        stats.recordSent(PacketCommand.CHAT, 1, 1, 0);

        run("reset");

        assertEquals(0, stats.sent(PacketCommand.CHAT));
        assertEquals(List.of("Network counters reset."), chat);
    }

    @Test
    void resetIsRefusedToAnObserverWhenCommandsAreRestricted() {
        game.getOptions().getOption(OptionsConstants.BASE_RESTRICT_GAME_COMMANDS).setValue(true);
        server.getPlayer(CONN).setObserver(true);
        game.addPlayer(CONN + 1, new Player(CONN + 1, "Bob")); // a non-observer, so observers are locked out
        stats.recordSent(PacketCommand.CHAT, 1, 1, 0);

        run("reset");

        assertEquals(1, stats.sent(PacketCommand.CHAT), "nothing was reset");
        assertTrue(chat.get(0).contains("not allowed"));
    }

    @Test
    void timingCanBeSwitchedAtRuntime() {
        NetStats.setTimingEnabled(false);

        run("timing", "on");
        assertTrue(NetStats.isTimingEnabled());
        assertEquals("Network timing is now on.", chat.get(0));

        run("timing", "off");
        assertFalse(NetStats.isTimingEnabled());

        chat.clear();
        run("timing");
        assertTrue(chat.get(0).startsWith("Network timing is off."));
    }

    @Test
    void anUnknownOptionExplainsItself() {
        run("bogus");

        assertTrue(chat.get(0).startsWith("Unknown option 'bogus'"));
        assertTrue(chat.get(0).contains("Usage"));
    }
}
