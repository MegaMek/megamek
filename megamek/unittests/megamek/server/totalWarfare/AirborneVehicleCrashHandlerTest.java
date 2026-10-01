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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import megamek.common.GameBoardTestCase;
import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.moves.MoveStep;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.SupportTank;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins how airborne WiGE crashes resolve today, through the {@link TWGameManager} entry points that other code
 * uses. Issue #9102 first moves the crash code into {@link AirborneVehicleCrashHandler} without changing behaviour;
 * these tests must pass unchanged before and after that move. Later #9102 changes update them on purpose.
 */
class AirborneVehicleCrashHandlerTest extends GameBoardTestCase {

    private static final double WIGE_TONNAGE = 30.0;
    private static final int ARMOR_PER_LOCATION = 40;
    private static final int SOUTH = 3;

    static {
        initializeBoard("CLEAR", """
              size 1 1
              hex 0101 0 "" ""
              end""");

        initializeBoard("DEEP_WATER", """
              size 1 1
              hex 0101 0 "water:2" ""
              end""");

        // Clear, clear, then a level 3 hill the sideslipping WiGE crashes into
        initializeBoard("HILL_AHEAD", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 3 "" ""
              end""");
    }

    private TWGameManager gameManager;

    /** Dice that always roll the middle: every d6 is a 4, so 2D6 is 8 and no critical or special result fires. */
    private static final class MiddleDice extends MMRandom {
        @Override
        public int randomInt(int maxValue) {
            return maxValue / 2;
        }

        @Override
        public float randomFloat() {
            return 0.5f;
        }
    }

    @AfterEach
    void restoreDice() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    @BeforeEach
    void setUp() throws IOException {
        Compute.setRNG(new MiddleDice());
        gameManager = new TWGameManager();
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        if (getGame().getPlayer(0) == null) {
            getGame().addPlayer(0, new Player(0, "Test"));
        }
    }

    /** A 30-ton combat WiGE, airborne at elevation 1 in hex 0101, with armor deep enough that crashes never kill it. */
    private SupportTank airborneWiGE() {
        SupportTank wige = new SupportTank();
        wige.setChassis("Test");
        wige.setModel("WiGE");
        wige.setMovementMode(EntityMovementMode.WIGE);
        wige.setWeight(WIGE_TONNAGE);
        wige.setOriginalWalkMP(8);
        wige.autoSetInternal();
        for (int location = 0; location < wige.locations(); location++) {
            wige.initializeArmor(ARMOR_PER_LOCATION, location);
        }
        wige.setCrew(new Crew(CrewType.SINGLE));
        wige.setOwner(getGame().getPlayer(0));
        wige.setId(5);
        getGame().addEntity(wige);
        wige.setPosition(new Coords(0, 0));
        wige.setFacing(SOUTH);
        wige.setElevation(1);
        return wige;
    }

    @Test
    void crashOverClearGroundLandsAndTakesFallingDamage() {
        setBoard("CLEAR");
        SupportTank wige = airborneWiGE();
        int armorBefore = wige.getTotalArmor();

        gameManager.crashVTOLorWiGE(wige);

        assertEquals(0, wige.getElevation(), "the crashed WiGE is on the ground");
        assertFalse(wige.isDoomed());
        // round(30 / 10) x (1 level fallen + 1)
        assertEquals(6, armorBefore - wige.getTotalArmor());
    }

    @Test
    void crashIntoDeepWaterDestroysTheWiGE() {
        setBoard("DEEP_WATER");
        SupportTank wige = airborneWiGE();

        gameManager.crashVTOLorWiGE(wige);

        assertTrue(wige.isDoomed(), "a crash into water destroys the WiGE today (TW says water counts as clear)");
    }

    @Test
    void sideslipIntoAHillCrashesWithTheLeftoverSkidDistance() {
        setBoard("HILL_AHEAD");
        SupportTank wige = airborneWiGE();
        int armorBefore = wige.getTotalArmor();
        MoveStep step = mock(MoveStep.class);
        when(step.getFacing()).thenReturn(SOUTH);

        // Sideslip 2 hexes south: 0102 is clear, 0103 is a level 3 hill
        gameManager.processSkid(wige, new Coords(0, 0), 1, SOUTH, 2, step, EntityMovementType.MOVE_VTOL_RUN);

        assertEquals(0, wige.getElevation(), "the WiGE crashed and is on the ground");
        // Today the crash uses the skid distance still left (0 here): round(30 / 10) x (0 + 1)
        assertEquals(3, armorBefore - wige.getTotalArmor());
    }
}
