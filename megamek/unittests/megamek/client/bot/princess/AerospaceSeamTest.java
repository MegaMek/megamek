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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.bot.princess.FireControl.FireControlType;
import megamek.client.bot.princess.PathRanker.PathRankerType;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proves the aerospace seams are invisible to Princess.
 *
 * <p>The whole gating decision for this work rests on one claim: adding the {@code Aerospace} ranker and
 * fire control slots changes nothing about how Princess plays. Princess registers its existing objects in
 * those slots, so the proof is object identity - an airborne fighter must resolve to the very same instances
 * that a ground unit does, which is what it resolved to before the slots existed.</p>
 *
 * <p>If this test ever fails, Princess has drifted and the change no longer qualifies as CASPAR-only.</p>
 */
class AerospaceSeamTest {

    private static final int BOARD_WIDTH = 20;
    private static final int BOARD_HEIGHT = 20;

    private Princess princess;

    @BeforeEach
    void beforeEach() {
        // No connection is made; the constructor only builds local state, and precognition's thread is
        // deliberately left unstarted.
        princess = new Princess("Guard", "localhost", 0);
        // A Client builds its own Game and holds it final, so the board goes onto that one.
        princess.getGame().setBoard(groundBoard());
        princess.initializePathRankers();
    }

    @AfterEach
    void afterEach() {
        princess.die();
    }

    private static Board groundBoard() {
        Hex[] hexes = new Hex[BOARD_WIDTH * BOARD_HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT, hexes);
        board.setBoardType(BoardType.GROUND);
        return board;
    }

    private Entity airborneFighter() {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setId(1);
        fighter.setGame(princess.getGame());
        fighter.setPosition(new Coords(5, 5));
        fighter.setAltitude(5);
        fighter.setDeployed(true);
        princess.getGame().addEntity(fighter);
        return fighter;
    }

    private Entity groundMek() {
        BipedMek mek = new BipedMek();
        mek.setId(2);
        mek.setGame(princess.getGame());
        mek.setPosition(new Coords(6, 6));
        mek.setDeployed(true);
        princess.getGame().addEntity(mek);
        return mek;
    }

    @Test
    void airborneFighterIsRecognisedAsAtmosphericAerospace() {
        assertTrue(princess.isAtmosphericAerospace(airborneFighter()));
    }

    @Test
    void aGroundMekIsNotAtmosphericAerospace() {
        org.junit.jupiter.api.Assertions.assertFalse(princess.isAtmosphericAerospace(groundMek()));
    }

    @Test
    void princessRanksAFighterWithTheSameRankerItUsesForEverythingElse() {
        assertSame(princess.getPathRanker(PathRankerType.Basic), princess.getPathRanker(airborneFighter()),
              "Princess must resolve a fighter to the ranker it resolved to before the Aerospace slot existed");
    }

    @Test
    void princessShootsFromAFighterWithTheSameFireControlItUsesForEverythingElse() {
        princess.initializeFireControls();
        assertSame(princess.getFireControl(FireControlType.Basic), princess.getFireControl(airborneFighter()),
              "Princess must resolve a fighter to the fire control it resolved to before the Aerospace slot existed");
    }

    @Test
    void princessRegistersTheSameObjectInBothRankerSlots() {
        assertSame(princess.getPathRanker(PathRankerType.Basic), princess.getPathRanker(PathRankerType.Aerospace));
    }

    @Test
    void princessRegistersTheSameObjectInBothFireControlSlots() {
        princess.initializeFireControls();
        assertSame(princess.getFireControl(FireControlType.Basic), princess.getFireControl(FireControlType.Aerospace));
    }

    @Test
    void princessStillUsesTheNewtonianRankerUnderVectorMovement() {
        // Vector movement keeps its own ranker; the atmospheric branch must not steal it.
        Entity fighter = airborneFighter();
        assertNotSame(princess.getPathRanker(PathRankerType.NewtonianAerospace),
              princess.getPathRanker(fighter),
              "without vector movement the fighter is atmospheric");
    }
}
