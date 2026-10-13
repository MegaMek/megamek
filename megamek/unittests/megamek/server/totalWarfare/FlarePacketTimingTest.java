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
package megamek.server.totalWarfare;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Vector;

import megamek.common.board.Board;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;

/**
 * An illumination round lights from the turn it arrives (TO:AR p.152), so its flare must reach the players when the
 * attack phase it landed in ends, not only in the End phase: otherwise their board, and the to-hit numbers they see for
 * the rest of the turn, know nothing of it.
 */
class FlarePacketTimingTest {
    private final Game game = mock(Game.class);
    private final TWGameManager gameManager = mock(TWGameManager.class);
    private final Packet flarePacket = new Packet(PacketCommand.SENDING_FLARES, new Vector<>());

    FlarePacketTimingTest() {
        when(gameManager.getGame()).thenReturn(game);
        when(gameManager.getMainPhaseReport()).thenReturn(new Vector<>());
        when(gameManager.createFlarePacket()).thenReturn(flarePacket);
        when(game.getFlares()).thenReturn(new Vector<>());
        when(game.getOptions()).thenReturn(new GameOptions());
        Board board = mock(Board.class);
        when(board.isGround()).thenReturn(true);
        when(game.getBoard()).thenReturn(board);
        when(game.getBoards()).thenReturn(Map.of(0, board));
    }

    /** After each phase in which artillery lands, the flares go out once the phase's attacks are resolved. */
    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = { "TARGETING", "OFFBOARD", "FIRING" })
    void theFlaresAreSentWhenAnAttackPhaseEnds(GamePhase phase) {
        when(game.getPhase()).thenReturn(phase);

        new TWPhaseEndManager(gameManager).managePhase();

        InOrder order = inOrder(gameManager);
        order.verify(gameManager).handleAttacks();
        order.verify(gameManager).changePhase(any(GamePhase.class));
        order.verify(gameManager).send(flarePacket);
    }

    /** A phase with no attacks sends no flares; they go out in the End phase as before. */
    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = { "MOVEMENT", "PHYSICAL" })
    void aPhaseWithoutArtillerySendsNoFlares(GamePhase phase) {
        when(game.getPhase()).thenReturn(phase);

        new TWPhaseEndManager(gameManager).managePhase();

        verify(gameManager, never()).send(flarePacket);
    }
}
