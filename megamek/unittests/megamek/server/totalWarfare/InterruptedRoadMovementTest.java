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

import static megamek.common.enums.MoveStepType.BACKWARDS;
import static megamek.common.enums.MoveStepType.FORWARDS;
import static megamek.common.enums.MoveStepType.TURN_RIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.List;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.Minefield;
import megamek.common.moves.MovePath;
import megamek.common.options.OptionsConstants;
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import megamek.utils.BoardLoader;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InterruptedRoadMovementTest extends GameBoardTestCase {

    @ParameterizedTest
    @CsvSource({ "TRACKED, 1, true", "TRACKED, 1, false", "TRACKED, 4, true", "HOVER, 3, true" })
    void roadBonusUsesExecutedStepsAcrossInterruptions(EntityMovementMode mode, int roadLevel, boolean staysOnRoad)
          throws IOException {
        String road = "road:" + roadLevel;
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 5
              hex 0101 0 "%s" ""
              hex 0102 0 "%s" ""
              hex 0103 0 "%s" ""
              hex 0104 0 "%s" ""
              hex 0105 0 "%s" ""
              end""".formatted(road, road, staysOnRoad ? road : "", road, road)));
        TWGameManager manager = new TWGameManager();
        manager.setGame(getGame());
        ServerFactory.createServer(manager);
        Player player = new Player(0, "Driver");
        getGame().addPlayer(0, player);
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_BAP).setValue(true);
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_MINEFIELDS).setValue(true);

        Tank tank = spy(new Tank());
        tank.setCrew(new Crew(CrewType.SINGLE));
        tank.setOwner(player);
        tank.setWeight(50);
        tank.autoSetInternal();
        tank.setOriginalWalkMP(2);
        doReturn(1).when(tank).getBAPRange();
        MovePath first = getMovePathFor(tank, 0, mode, FORWARDS);
        getGame().setPhase(GamePhase.MOVEMENT);
        getGame().setTurnVector(List.of(new SpecificEntityTurn(player.getId(), tank.getId())));
        getGame().setTurnIndex(0, player.getId());

        // The probe detects one minefield at 0102 and a second at 0104, offering movement again each time.
        addDetectableMinefield(new Coords(0, 0));
        addDetectableMinefield(new Coords(0, 4));
        assertTrue(first.getLastStep().isOnlyPavementOrRoad());
        assertFalse(tank.gotPavementOrRoadBonus, "Previewing a road step cannot change the unit's history");
        new MovePathHandler(manager, tank, first, null).processMovement();
        assertEquals(new Coords(0, 1), tank.getPosition());
        assertTrue(tank.turnWasInterrupted());
        assertTrue(tank.gotPavementOrRoadBonus);

        getGame().changeToNextTurn();
        MovePath middle = new MovePath(getGame(), tank).addStep(FORWARDS).addStep(FORWARDS);
        assertTrue(middle.isMoveLegal());
        assertEquals(staysOnRoad, middle.getLastStep().isOnlyPavementOrRoad());
        assertTrue(tank.gotPavementOrRoadBonus, "Plotting a departure from the road cannot spend the bonus");
        new MovePathHandler(manager, tank, middle, null).processMovement();
        assertEquals(new Coords(0, 3), tank.getPosition());
        assertEquals(3, tank.mpUsed);
        assertEquals(staysOnRoad, tank.gotPavementOrRoadBonus);

        getGame().changeToNextTurn();
        // A turn costs 1 MP and cannot qualify for the minimum forward movement allowance.
        MovePath resumed = new MovePath(getGame(), tank).addStep(TURN_RIGHT);
        assertEquals(4, resumed.getMpUsed());
        assertEquals(staysOnRoad, resumed.getLastStep().isOnlyPavementOrRoad());
        assertEquals(staysOnRoad, resumed.isMoveLegal(), "The fourth MP exceeds the tank's normal flank allowance");
    }

    @Test
    void anAssetCanChangeLevelsAfterAnInterruptionBeforeItsPlannedReverse() throws IOException {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 3
              hex 0101 0 "road:1" ""
              hex 0102 0 "road:1" ""
              hex 0103 1 "road:1" ""
              end"""));
        TWGameManager manager = new TWGameManager();
        manager.setGame(getGame());
        ServerFactory.createServer(manager);
        Player player = new Player(0, "Driver");
        getGame().addPlayer(0, player);
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_BAP).setValue(true);
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_MINEFIELDS).setValue(true);

        BattlefieldSupportAsset asset = spy(new BattlefieldSupportAsset());
        asset.setOwner(player);
        asset.setMp(6);
        doReturn(1).when(asset).getBAPRange();
        MovePath planned = getMovePathFor(asset, 0, EntityMovementMode.WHEELED, FORWARDS, BACKWARDS);
        getGame().setPhase(GamePhase.MOVEMENT);
        getGame().setTurnVector(List.of(new SpecificEntityTurn(player.getId(), asset.getId())));
        getGame().setTurnIndex(0, player.getId());
        addDetectableMinefield(new Coords(0, 0));
        assertTrue(planned.isMoveLegal());
        assertTrue(planned.getCoordsSet().contains(new Coords(0, 0)));

        new MovePathHandler(manager, asset, planned, null).processMovement();
        assertEquals(new Coords(0, 1), asset.getPosition());
        assertTrue(asset.turnWasInterrupted());
        assertFalse(asset.getMovedBackwards());
        assertEquals(1, planned.length());
        assertTrue(planned.contains(FORWARDS));
        assertFalse(planned.contains(BACKWARDS));
        assertFalse(planned.getCoordsSet().contains(new Coords(0, 0)));

        getGame().changeToNextTurn();
        assertTrue(new MovePath(getGame(), asset).addStep(FORWARDS).isMoveLegal());
        MovePath reverse = new MovePath(getGame(), asset).addStep(BACKWARDS);
        assertTrue(reverse.isMoveLegal());
        new MovePathHandler(manager, asset, reverse, null).processMovement();
        assertEquals(new Coords(0, 0), asset.getPosition());
        assertTrue(asset.getMovedBackwards());
    }

    private void addDetectableMinefield(Coords position) {
        Minefield minefield = spy(Minefield.createMinefield(position, 1, Minefield.TYPE_CONVENTIONAL, 5));
        doReturn(2).when(minefield).getBAPDetectionTarget();
        getGame().addMinefield(minefield);
    }
}
