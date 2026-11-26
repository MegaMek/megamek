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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.GameBoardTestCase;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Issue #9574: a bot Stinger on a bridge never moved again. When checking whether the bridge would collapse, the bot
 * added the Stinger's weight once as the mover and again as a unit already on the bridge, so 20 tons read as 40 and
 * every walking path off the bridge was thrown out.
 */
class BridgeSelfWeightPathTest extends GameBoardTestCase {

    // The bridge from the issue: a depth 1 river at level -3 with the deck 3 levels up, open north and south
    private static final int BRIDGE_CF = 34;
    private static final double STINGER_TONS = 20.0;

    static {
        initializeBoard("BRIDGE", """
              size 1 3
              hex 0101 -3 "water:1;bridge:2:9;bridge_cf:34;bridge_elev:3" ""
              hex 0102 -1 "" ""
              hex 0103 0 "" ""
              end""");
    }

    @BeforeEach
    void setUp() {
        setBoard("BRIDGE");
    }

    /** A 20-ton Mek standing on the bridge deck, facing north, away from the hex it will walk to. */
    private BipedMek stingerOnBridge() {
        BipedMek stinger = new BipedMek();
        getMovePathFor(stinger, 3, null);
        stinger.setWeight(STINGER_TONS);
        stinger.setFacing(0);
        // only deployed units are found standing in a hex
        stinger.setDeployed(true);
        return stinger;
    }

    /** Turns around on the deck, then walks south off the bridge. */
    private MovePath walkOffBridge(Entity mover) {
        MovePath path = new MovePath(getGame(), mover);
        path.addStep(MoveStepType.TURN_RIGHT);
        path.addStep(MoveStepType.TURN_RIGHT);
        path.addStep(MoveStepType.TURN_RIGHT);
        path.addStep(MoveStepType.FORWARDS);
        return path;
    }

    /** A path ranker whose only open question is the building check: no enemy range test, no fall chance. */
    private BasicPathRanker ranker() {
        Princess princess = mock(Princess.class);
        BehaviorSettings behaviorSettings = mock(BehaviorSettings.class);
        when(behaviorSettings.getNumberOfEnemiesToConsiderFacing()).thenReturn(1);
        when(princess.getBehaviorSettings()).thenReturn(behaviorSettings);
        when(princess.getHonorUtil()).thenReturn(mock(IHonorUtil.class));
        FireControlState fireControlState = mock(FireControlState.class);
        when(fireControlState.getAdditionalTargets()).thenReturn(new ArrayList<>());
        when(princess.getFireControlState()).thenReturn(fireControlState);
        when(princess.wantsToFallBack(any())).thenReturn(true);
        when(princess.getMassOfAllInBuilding(any(Game.class), any(Coords.class), anyInt())).thenCallRealMethod();
        when(princess.getMassOfAllInBuilding(any(Game.class), any(Coords.class), anyInt(), any()))
              .thenCallRealMethod();
        BasicPathRanker ranker = spy(new BasicPathRanker(princess));
        doReturn(1.0).when(ranker).getMovePathSuccessProbability(any(MovePath.class));
        return ranker;
    }

    @Test
    @DisplayName("a 20-ton Mek alone on a CF 34 bridge may walk off it")
    void moverOnBridgeCountsItsWeightOnce() {
        BipedMek stinger = stingerOnBridge();
        MovePath walkOff = walkOffBridge(stinger);
        assertTrue(walkOff.isMoveLegal(), "the walk off the bridge is a legal move");

        List<MovePath> validPaths = ranker().validatePaths(List.of(walkOff), getGame(), 9, 0.75);

        assertTrue(validPaths.contains(walkOff),
              "20 tons plus the 10-ton margin is 30, under the bridge's CF of " + BRIDGE_CF
                    + "; the Mek's own weight must not be added a second time");
    }

    @Test
    @DisplayName("another unit already on the bridge still counts against it")
    void otherUnitsOnBridgeStillCount() {
        BipedMek stinger = stingerOnBridge();
        BipedMek second = new BipedMek();
        second.setId(6);
        second.setWeight(STINGER_TONS);
        getGame().addEntity(second);
        second.setPosition(stinger.getPosition());
        second.setElevation(3);
        second.setDeployed(true);
        MovePath walkOff = walkOffBridge(stinger);

        List<MovePath> validPaths = ranker().validatePaths(List.of(walkOff), getGame(), 9, 0.75);

        assertFalse(validPaths.contains(walkOff),
              "20 tons, the 10-ton margin and the other 20-ton Mek make 50, over the bridge's CF of " + BRIDGE_CF);
    }
}
