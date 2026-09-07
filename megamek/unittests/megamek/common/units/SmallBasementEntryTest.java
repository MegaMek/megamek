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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #8902: a small basement (Basements Table result 9, TW p. 179) is a level that only infantry can move down
 * into; ProtoMeks cannot enter it, and it has no effect on Meks or vehicles. A normal one-level basement is still open
 * to ProtoMeks.
 */
class SmallBasementEntryTest extends GameBoardTestCase {

    /** Hex 0101 has a small basement (type ordinal 5); hex 0201 a normal one-level basement (type ordinal 4). */
    private static final Coords SMALL_BASEMENT_HEX = new Coords(0, 0);
    private static final Coords NORMAL_BASEMENT_HEX = new Coords(1, 0);
    private static final int GROUND_LEVEL = 0;

    static {
        initializeBoard("SMALL_BASEMENT_BOARD", """
              size 3 3
              hex 0101 0 "building:2;bldg_cf:50;bldg_elev:1;bldg_basement_type:5" ""
              hex 0201 0 "building:2;bldg_cf:50;bldg_elev:1;bldg_basement_type:4" ""
              end"""
        );
    }

    private Game game;
    private Board board;

    @BeforeEach
    void beforeEach() {
        board = getBoard("SMALL_BASEMENT_BOARD");
        game = new Game();
        game.setBoard(board);
    }

    private <T extends Entity> T inGame(T entity) {
        entity.setGame(game);
        return entity;
    }

    private ProtoMek protoMek() {
        ProtoMek protoMek = inGame(new ProtoMek());
        protoMek.setMovementMode(EntityMovementMode.BIPED);
        return protoMek;
    }

    @Test
    void infantryCanMoveDownIntoASmallBasement() {
        ConvInfantry platoon = inGame(new ConvInfantry());

        assertTrue(platoon.canGoDown(GROUND_LEVEL, SMALL_BASEMENT_HEX, board.getBoardId()));
    }

    @Test
    void protoMekCannotEnterASmallBasement() {
        ProtoMek protoMek = protoMek();

        assertFalse(protoMek.canGoDown(GROUND_LEVEL, SMALL_BASEMENT_HEX, board.getBoardId()));
    }

    @Test
    void protoMekCanStillEnterANormalBasement() {
        ProtoMek protoMek = protoMek();

        assertTrue(protoMek.canGoDown(GROUND_LEVEL, NORMAL_BASEMENT_HEX, board.getBoardId()));
    }

    @Test
    void mekIsUnaffectedByASmallBasement() {
        BipedMek mek = inGame(new BipedMek());

        assertFalse(mek.canGoDown(GROUND_LEVEL, SMALL_BASEMENT_HEX, board.getBoardId()));
    }
}
