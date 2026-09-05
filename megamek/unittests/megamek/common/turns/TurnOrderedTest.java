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
package megamek.common.turns;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.MMRandom;
import megamek.common.Team;
import megamek.common.compute.Compute;
import megamek.common.game.IGame;
import megamek.common.game.InitiativeRoll;
import megamek.common.interfaces.ITurnOrdered;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TurnOrderedTest {

    @AfterEach
    void restoreRng() {
        // Some tests install a deterministic RNG; make sure the default is restored for everything else.
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    /**
     * Regression test for the tie-break stack overflow: two distinct initiative candidates that share the same
     * {@link InitiativeRoll} instance used to compare equal on every re-roll, so {@code rollInitAndResolveTies}
     * recursed until it threw a {@link StackOverflowError}. It must now terminate.
     */
    @Test
    void sharedInitiativeRollDoesNotStackOverflow() {
        InitiativeRoll shared = new InitiativeRoll();
        FakeTurnOrdered a = new FakeTurnOrdered(shared);
        FakeTurnOrdered b = new FakeTurnOrdered(shared);

        assertDoesNotThrow(() ->
              TurnOrdered.rollInitAndResolveTies(List.of(a, b), null, false, new HashMap<>()));
    }

    /**
     * Two independent candidates must still have their (random) initiative resolved without error. This exercises the
     * normal path and guarantees the tie-break recursion terminates for ordinary rolls.
     */
    @Test
    void independentCandidatesResolveWithoutError() {
        FakeTurnOrdered a = new FakeTurnOrdered(new InitiativeRoll());
        FakeTurnOrdered b = new FakeTurnOrdered(new InitiativeRoll());

        assertDoesNotThrow(() ->
              TurnOrdered.rollInitAndResolveTies(List.of(a, b), null, false, new HashMap<>()));
    }

    @Test
    void threeWayTieResolvesWithoutError() {
        FakeTurnOrdered a = new FakeTurnOrdered(new InitiativeRoll());
        FakeTurnOrdered b = new FakeTurnOrdered(new InitiativeRoll());
        FakeTurnOrdered c = new FakeTurnOrdered(new InitiativeRoll());

        assertDoesNotThrow(() ->
              TurnOrdered.rollInitAndResolveTies(List.of(a, b, c), null, false, new HashMap<>()));
    }

    /**
     * Depth-cap backstop: even two <em>distinct</em> initiative rolls that compare equal on every single re-roll
     * (forced here with a constant RNG) must terminate rather than recurse into a {@link StackOverflowError}. This
     * covers the bound independently of the shared-instance identity guard.
     */
    @Test
    void perpetualTieTerminatesViaDepthCap() {
        // Constant RNG -> every d6 is identical, so two separate rolls tie on every pass forever.
        Compute.setRNG(new ConstantRandom());

        FakeTurnOrdered a = new FakeTurnOrdered(new InitiativeRoll());
        FakeTurnOrdered b = new FakeTurnOrdered(new InitiativeRoll());

        assertDoesNotThrow(() ->
              TurnOrdered.rollInitAndResolveTies(List.of(a, b), null, false, new HashMap<>()));
    }

    /** Deterministic RNG whose every die shows the same face, guaranteeing identical rolls. */
    private static final class ConstantRandom extends MMRandom {
        @Override
        public int randomInt(int maxValue) {
            return 0;
        }

        @Override
        public float randomFloat() {
            return 0f;
        }
    }

    /**
     * Minimal {@link ITurnOrdered} that is neither a Team nor an Entity, so {@code rollInitAndResolveTies} takes its
     * generic path (zero bonus breakdown) and only touches the initiative roll and initiative-compensation bonus.
     */
    private static final class FakeTurnOrdered implements ITurnOrdered {
        private InitiativeRoll initiative;
        private int initCompBonus;

        private FakeTurnOrdered(InitiativeRoll initiative) {
            this.initiative = initiative;
        }

        @Override
        public InitiativeRoll getInitiative() {
            return initiative;
        }

        @Override
        public void setInitiative(InitiativeRoll newRoll) {
            this.initiative = newRoll;
        }

        @Override
        public void clearInitiative(boolean bUseInitComp, Map<Team, Integer> initiativeAptitude) {
            getInitiative().clear();
        }

        @Override
        public int getInitCompensationBonus() {
            return initCompBonus;
        }

        @Override
        public void setInitCompensationBonus(int newBonus) {
            this.initCompBonus = newBonus;
        }

        // ---- Turn-counting members are irrelevant to tie resolution; stub them out. ----

        @Override
        public int getNormalTurns(IGame game) {
            return 0;
        }

        @Override
        public int getOtherTurns() {
            return 0;
        }

        @Override
        public int getEvenTurns() {
            return 0;
        }

        @Override
        public int getMultiTurns(IGame game) {
            return 0;
        }

        @Override
        public int getSpaceStationTurns() {
            return 0;
        }

        @Override
        public int getJumpshipTurns() {
            return 0;
        }

        @Override
        public int getWarshipTurns() {
            return 0;
        }

        @Override
        public int getDropshipTurns() {
            return 0;
        }

        @Override
        public int getSmallCraftTurns() {
            return 0;
        }

        @Override
        public int getTeleMissileTurns() {
            return 0;
        }

        @Override
        public int getAeroTurns() {
            return 0;
        }

        @Override
        public void incrementOtherTurns() {
        }

        @Override
        public void incrementEvenTurns() {
        }

        @Override
        public void incrementMultiTurns(int entityClass) {
        }

        @Override
        public void incrementSpaceStationTurns() {
        }

        @Override
        public void incrementJumpshipTurns() {
        }

        @Override
        public void incrementWarshipTurns() {
        }

        @Override
        public void incrementDropshipTurns() {
        }

        @Override
        public void incrementSmallCraftTurns() {
        }

        @Override
        public void incrementTeleMissileTurns() {
        }

        @Override
        public void incrementAeroTurns() {
        }

        @Override
        public void resetOtherTurns() {
        }

        @Override
        public void resetEvenTurns() {
        }

        @Override
        public void resetMultiTurns() {
        }

        @Override
        public void resetSpaceStationTurns() {
        }

        @Override
        public void resetJumpshipTurns() {
        }

        @Override
        public void resetWarshipTurns() {
        }

        @Override
        public void resetDropshipTurns() {
        }

        @Override
        public void resetSmallCraftTurns() {
        }

        @Override
        public void resetTeleMissileTurns() {
        }

        @Override
        public void resetAeroTurns() {
        }
    }
}
