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
import java.util.concurrent.TimeUnit;

import megamek.common.GameBoardTestCase;
import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.moves.MoveStep;
import megamek.common.units.EntityMovementType;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * The hexes a unit has moved must survive a failed maneuver that ends its movement, because they set its target
 * movement modifier (issue #8047). A serious fishtail ends movement and keeps the hexes moved (TO:AR p.24). A skid
 * keeps them too, and the hexes skidded are added on top (TW p.62).
 *
 * <p>Every d6 rolls a 4, so the Failed Maneuver Table roll is 8 plus the modifier passed in. A 50-ton LAM in AirMek
 * mode adds nothing for vehicle type (IntOps:AE p.103) or weight class.</p>
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class FailedManeuverDistanceTest extends GameBoardTestCase {

    private static final int SOUTH = 3;
    private static final int LAM_ID = 7;
    private static final int HEXES_MOVED = 6;
    private static final int MARGIN_OF_FAILURE = 3;
    /** Added to the 8 the dice roll: 8 is a moderate fishtail, 10 a serious fishtail, 12 a skid. */
    private static final int MODERATE_FISHTAIL_MODIFIER = 0;
    private static final int SERIOUS_FISHTAIL_MODIFIER = 2;
    private static final int SKID_MODIFIER = 4;

    static {
        initializeBoard("LONG_CLEAR", """
              size 1 12
              end""");
    }

    private TWGameManager gameManager;

    /** Every d6 rolls a 4. */
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
        setBoard("LONG_CLEAR");
    }

    /** A 50-ton LAM in AirMek mode at elevation 1 in hex 0101, facing south, armored to survive a skid. */
    private LandAirMek airborneAirMek() {
        LandAirMek landAirMek = new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD, LandAirMek.LAM_STANDARD);
        landAirMek.setChassis("Test");
        landAirMek.setModel("LAM");
        landAirMek.setWeight(50.0);
        landAirMek.setOriginalWalkMP(5);
        landAirMek.setOriginalJumpMP(5);
        landAirMek.autoSetInternal();
        for (int location = 0; location < landAirMek.locations(); location++) {
            landAirMek.initializeArmor(30, location);
        }
        landAirMek.setConversionMode(LandAirMek.CONV_MODE_AIR_MEK);
        landAirMek.setOwner(getGame().getPlayer(0));
        landAirMek.setId(LAM_ID);
        getGame().addEntity(landAirMek);
        landAirMek.setPosition(new Coords(0, 0));
        landAirMek.setFacing(SOUTH);
        landAirMek.setElevation(1);
        return landAirMek;
    }

    private static MoveStep stepFacingSouth() {
        MoveStep step = mock(MoveStep.class);
        when(step.getFacing()).thenReturn(SOUTH);
        when(step.getElevation()).thenReturn(1);
        return step;
    }

    /** Fails a one-hexside right turn in the LAM's current hex after it has moved {@link #HEXES_MOVED} hexes. */
    private boolean failTurn(LandAirMek landAirMek, int modifier) {
        return gameManager.processFailedVehicleManeuver(landAirMek,
              landAirMek.getPosition(),
              1,
              stepFacingSouth(),
              false,
              EntityMovementType.MOVE_VTOL_RUN,
              HEXES_MOVED,
              modifier,
              MARGIN_OF_FAILURE);
    }

    @Test
    void seriousFishtailKeepsTheHexesMoved() {
        LandAirMek landAirMek = airborneAirMek();

        boolean movementEnds = failTurn(landAirMek, SERIOUS_FISHTAIL_MODIFIER);

        assertTrue(movementEnds, "a serious fishtail ends movement (TO:AR p.24)");
        assertEquals(new Coords(0, 0), landAirMek.getPosition(), "a fishtail does not move the unit");
        assertEquals(HEXES_MOVED, landAirMek.delta_distance,
              "the 6 hexes already moved still count toward the target movement modifier");
    }

    @Test
    void skidKeepsTheHexesMovedAndAddsTheHexesSkidded() {
        LandAirMek landAirMek = airborneAirMek();

        boolean movementEnds = failTurn(landAirMek, SKID_MODIFIER);

        assertTrue(movementEnds, "a skid ends movement (TO:AR p.24)");
        int hexesSkidded = new Coords(0, 0).distance(landAirMek.getPosition());
        assertEquals(MARGIN_OF_FAILURE, hexesSkidded, "a LAM skids the margin of failure on open ground");
        assertEquals(HEXES_MOVED + hexesSkidded, landAirMek.delta_distance,
              "the 6 hexes moved and the 3 hexes skidded all count toward the target movement modifier (TW p.62)");
    }

    @Test
    void moderateFishtailLeavesTheDistanceToTheRestOfTheMove() {
        LandAirMek landAirMek = airborneAirMek();
        landAirMek.delta_distance = 0;

        boolean movementEnds = failTurn(landAirMek, MODERATE_FISHTAIL_MODIFIER);

        assertFalse(movementEnds, "a moderate fishtail lets the unit keep moving (TO:AR p.24)");
        assertEquals(0, landAirMek.delta_distance,
              "the move goes on, so the distance is recorded when it ends, not here");
    }
}
