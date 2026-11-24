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

package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Light;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for Terrain Master [Nightwalker] movement in poor light (MegaMek issue #9168).
 * <p>
 * CamOps: a Nightwalker ignores darkness MP costs while walking, and pays 1 MP less per hex (minimum 0) once it moves
 * past its walk MP. Each hex is judged on its own, so the hexes walked before the unit starts running stay free. The
 * old code instead stopped the unit from running at all, which made it slower than a pilot without the SPA.
 * <p>
 * The test Mek has 8 walk MP and 12 run MP.
 */
class NightwalkerMovementTest extends GameBoardTestCase {

    private static final int WALK_MP = 8;

    private static final int BOARD_LENGTH = 16;

    static {
        StringBuilder board = new StringBuilder("size 1 " + BOARD_LENGTH + "\n");
        for (int row = 1; row <= BOARD_LENGTH; row++) {
            board.append(String.format("hex 01%02d 0 \"\" \"\"%n", row));
        }
        board.append("end");
        initializeBoard("LONG_CLEAR", board.toString());
    }

    private static BipedMek mek(boolean isNightwalker) {
        BipedMek mek = new BipedMek();
        mek.getCrew().getOptions().getOption(OptionsConstants.PILOT_TM_NIGHTWALKER).setValue(isNightwalker);
        return mek;
    }

    private MovePath moveForwards(BipedMek mek, Light light, int hexes) {
        setBoard("LONG_CLEAR");
        getGame().getPlanetaryConditions().setLight(light);
        MoveStepType[] steps = new MoveStepType[hexes];
        Arrays.fill(steps, MoveStepType.FORWARDS);
        return getMovePathFor(mek, steps);
    }

    @Test
    void ordinaryPilotPaysTheFullMoonlessPenalty() {
        MovePath movePath = moveForwards(mek(false), Light.MOONLESS, 4);

        assertEquals(4 * (1 + 2), movePath.getMpUsed(), "Each hex costs 1 MP plus 2 MP for a moonless night");
        assertEquals(EntityMovementType.MOVE_RUN, movePath.getLastStepMovementType());
    }

    @Test
    void nightwalkerIgnoresTheMoonlessPenaltyWhileWalking() {
        MovePath movePath = moveForwards(mek(true), Light.MOONLESS, WALK_MP);

        assertEquals(WALK_MP, movePath.getMpUsed(), "Hexes within walk MP pay no darkness cost");
        assertEquals(EntityMovementType.MOVE_WALK, movePath.getLastStepMovementType());
    }

    @Test
    void nightwalkerCanRunAndPaysOneLessPastWalkMp() {
        MovePath movePath = moveForwards(mek(true), Light.MOONLESS, WALK_MP + 2);

        assertEquals(WALK_MP + (2 * (1 + 1)), movePath.getMpUsed(),
              "The 8 walked hexes are free; the 2 run hexes pay the moonless 2 MP minus 1");
        assertEquals(EntityMovementType.MOVE_RUN, movePath.getLastStepMovementType(),
              "A Nightwalker must be able to run at night");
    }

    @Test
    void nightwalkerRunsFreeUnderAFullMoon() {
        MovePath movePath = moveForwards(mek(true), Light.FULL_MOON, WALK_MP + 4);

        assertEquals(WALK_MP + 4, movePath.getMpUsed(), "The full moon's 1 MP cost minus 1 leaves nothing to pay");
        assertEquals(EntityMovementType.MOVE_RUN, movePath.getLastStepMovementType());
    }

    @Test
    void nightwalkerOutrunsAnOrdinaryPilotInPitchBlack() {
        MovePath nightwalkerPath = moveForwards(mek(true), Light.PITCH_BLACK, WALK_MP + 1);

        assertEquals(WALK_MP + 1 + 2, nightwalkerPath.getMpUsed(),
              "The one run hex pays the pitch black 3 MP minus 1");
        assertEquals(EntityMovementType.MOVE_RUN, nightwalkerPath.getLastStepMovementType());
    }

    @Test
    void lightSpecialistNightwalkerPaysTheLowerCostWithoutStacking() {
        assertEquals(1, MoveStep.nightwalkerLightPenalty(Light.PITCH_BLACK, true, false, false),
              "Specialist pays 1, Nightwalker pays 2; the lower one applies, not 0");
        assertEquals(1, MoveStep.nightwalkerLightPenalty(Light.MOONLESS, true, false, false),
              "Both reductions give 1 on a moonless night");
        assertEquals(0, MoveStep.nightwalkerLightPenalty(Light.PITCH_BLACK, true, false, true),
              "Walking is always free");
    }

    @Test
    void lightPenaltyTableIsUnchanged() {
        assertEquals(1, MoveStep.lightMovementPenalty(Light.FULL_MOON, false, false));
        assertEquals(0, MoveStep.lightMovementPenalty(Light.FULL_MOON, false, true));
        assertEquals(1, MoveStep.lightMovementPenalty(Light.GLARE, false, false));
        assertEquals(2, MoveStep.lightMovementPenalty(Light.MOONLESS, false, false));
        assertEquals(0, MoveStep.lightMovementPenalty(Light.MOONLESS, false, true));
        assertEquals(1, MoveStep.lightMovementPenalty(Light.MOONLESS, true, false));
        assertEquals(2, MoveStep.lightMovementPenalty(Light.SOLAR_FLARE, false, false));
        assertEquals(3, MoveStep.lightMovementPenalty(Light.PITCH_BLACK, false, false));
        assertEquals(1, MoveStep.lightMovementPenalty(Light.PITCH_BLACK, true, false));
        assertEquals(0, MoveStep.lightMovementPenalty(Light.DUSK, false, false));
        assertEquals(0, MoveStep.lightMovementPenalty(Light.DAY, false, false));
    }

    @Test
    void lightSpecialistAloneIsUnchanged() {
        BipedMek mek = mek(false);
        mek.getCrew().getOptions().getOption(OptionsConstants.MISC_ENV_SPECIALIST)
              .setValue(Crew.ENVIRONMENT_SPECIALIST_LIGHT);

        MovePath movePath = moveForwards(mek, Light.PITCH_BLACK, 3);

        assertEquals(3 * (1 + 1), movePath.getMpUsed(), "A Light specialist pays 1 MP per hex in pitch black");
    }
}
