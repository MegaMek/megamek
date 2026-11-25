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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rules.RulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import megamek.utils.BoardLoader;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SkidRoadMovementTest extends GameBoardTestCase {
    private final RulesManager previousRules = Game.rulesManager;

    @AfterEach
    void restoreRules() {
        Game.rulesManager = previousRules;
    }

    @ParameterizedTest
    @CsvSource({ "road:3;woods:2;swamp:2, false", "road:4;rubble:1;swamp:2, false", "road:3;mud:1, true" })
    void skiddingVehicleUsesTheRoadAtEachNewHex(String middle, boolean bogs) {
        TWGameManager manager = new TWGameManager();
        manager.setGame(getGame());
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 3
              hex 0101 0 "road:1" ""
              hex 0102 0 "%s" ""
              hex 0103 0 "road:3" ""
              end""".formatted(middle)));
        getGame().addPlayer(0, new Player(0, "Test"));
        Tank tank = new Tank();
        tank.setOwner(getGame().getPlayer(0));
        MoveStep step = getMovePathFor(tank, 0, EntityMovementMode.WHEELED, MoveStepType.FORWARDS).getLastStep();

        manager.processSkid(tank, new Coords(0, 0), 0, 3, 2, step, EntityMovementType.MOVE_RUN);

        assertEquals(bogs, tank.isStuck());
        assertEquals(new Coords(0, bogs ? 1 : 2), tank.getPosition());
    }

    @ParameterizedTest
    @CsvSource({ "road:3, true", "road:4, false", "'', false" })
    void hovercraftResumesAfterSideslippingOverTwoHexes(String firstSlipHex, boolean keepsBonus) throws IOException {
        // At 0104 the hovercraft turns toward 0204 but fails its flanking check.
        // It actually slides straight through 0105 to 0106, which is back on the dirt road.
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 2 6
              hex 0101 0 "road:3" ""
              hex 0201 0 "road:3" ""
              hex 0102 0 "road:3" ""
              hex 0202 0 "road:3" ""
              hex 0103 0 "road:3" ""
              hex 0203 0 "road:3" ""
              hex 0104 0 "road:3" ""
              hex 0204 0 "road:3" ""
              hex 0105 0 "%s" ""
              hex 0205 0 "road:3" ""
              hex 0106 0 "road:3" ""
              hex 0206 0 "road:3" ""
              end""".formatted(firstSlipHex)));
        Tank hover = new Tank();
        TWGameManager manager = setUpMover(hover, EntityMovementMode.HOVER);
        MovePath plotted = new MovePath(getGame(), hover)
              .addStep(MoveStepType.FORWARDS).addStep(MoveStepType.FORWARDS).addStep(MoveStepType.FORWARDS)
              .addStep(MoveStepType.TURN_LEFT).addStep(MoveStepType.FORWARDS)
              .addStep(MoveStepType.TURN_RIGHT).addStep(MoveStepType.FORWARDS);
        assertTrue(plotted.isMoveLegal());
        assertEquals(EntityMovementType.MOVE_RUN, plotted.getLastStepMovementType());
        failSideslipAt(manager, hover, new Coords(0, 3), new Coords(1, 3));

        new MovePathHandler(manager, hover, plotted, null).processMovement();
        assertEquals(new Coords(0, 5), hover.getPosition());
        assertTrue(hover.turnWasInterrupted());
        assertEquals(4, hover.mpUsed, "The failed forward step was never taken");
        assertEquals(keepsBonus, hover.gotPavementOrRoadBonus);

        getGame().changeToNextTurn();
        MovePath resumed = new MovePath(getGame(), hover)
              .addStep(MoveStepType.TURN_RIGHT).addStep(MoveStepType.TURN_RIGHT).addStep(MoveStepType.TURN_RIGHT);
        assertEquals(7, resumed.getMpUsed());
        assertEquals(keepsBonus, resumed.isMoveLegal());
    }

    @ParameterizedTest
    @CsvSource({ "road:3, true", "road:4, false" })
    void sideslipAtFlankAllowanceCanSpendTheRemainingRoadMP(String surface, boolean getsAnotherTurn)
          throws IOException {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 2 8
              hex 0101 0 "road:3" ""
              hex 0201 0 "road:3" ""
              hex 0102 0 "road:3" ""
              hex 0202 0 "road:3" ""
              hex 0103 0 "road:3" ""
              hex 0203 0 "road:3" ""
              hex 0104 0 "road:3" ""
              hex 0204 0 "road:3" ""
              hex 0105 0 "road:3" ""
              hex 0205 0 "road:3" ""
              hex 0106 0 "road:3" ""
              hex 0206 0 "road:3" ""
              hex 0107 0 "%s" ""
              hex 0207 0 "road:3" ""
              hex 0108 0 "road:3" ""
              hex 0208 0 "road:3" ""
              end""".formatted(surface)));
        Tank hover = new Tank();
        TWGameManager manager = setUpMover(hover, EntityMovementMode.HOVER);
        MovePath plotted = new MovePath(getGame(), hover)
              .addStep(MoveStepType.FORWARDS).addStep(MoveStepType.FORWARDS)
              .addStep(MoveStepType.FORWARDS).addStep(MoveStepType.FORWARDS)
              .addStep(MoveStepType.FORWARDS).addStep(MoveStepType.TURN_LEFT).addStep(MoveStepType.FORWARDS);
        assertTrue(plotted.isMoveLegal());
        failSideslipAt(manager, hover, new Coords(0, 5), new Coords(1, 5));

        new MovePathHandler(manager, hover, plotted, null).processMovement();

        assertEquals(new Coords(0, 7), hover.getPosition());
        assertEquals(6, hover.mpUsed);
        assertEquals(getsAnotherTurn, hover.turnWasInterrupted());
        if (getsAnotherTurn) {
            getGame().changeToNextTurn();
            MovePath resumed = new MovePath(getGame(), hover).addStep(MoveStepType.TURN_RIGHT);
            assertTrue(resumed.isMoveLegal());
            assertEquals(7, resumed.getMpUsed());
        }
    }

    @Test
    void recklessTankSkidsFromTheIcyHex() throws IOException {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 5
              hex 0101 0 "pavement:1" ""
              hex 0102 0 "pavement:1" ""
              hex 0103 0 "pavement:1;black_ice:1" ""
              hex 0104 0 "pavement:1" ""
              hex 0105 0 "pavement:1" ""
              end"""));
        Tank tank = new Tank();
        TWGameManager manager = setUpMover(tank, EntityMovementMode.WHEELED);
        Game.rulesManager = new TWRulesManager();
        doReturn(2).when(manager).doSkillCheckWhileMoving(eq(tank), eq(0), eq(new Coords(0, 1)),
              eq(new Coords(0, 2)), any(PilotingRollData.class), eq(false));
        MovePath path = new MovePath(getGame(), tank);
        path.setCareful(false);
        path.addStep(MoveStepType.FORWARDS).addStep(MoveStepType.FORWARDS);

        new MovePathHandler(manager, tank, path, null).processMovement();

        verify(manager).doSkillCheckWhileMoving(eq(tank), eq(0), eq(new Coords(0, 1)), eq(new Coords(0, 2)),
              any(PilotingRollData.class), eq(false));
        assertEquals(new Coords(0, 3), tank.getPosition());
        assertEquals(EntityMovementType.MOVE_SKID, tank.moved);
        assertTrue(tank.isDone());
    }

    @Test
    void mekThatFallsOnIcyPavementSkidsFromItsLandingHex() throws IOException {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 5
              hex 0101 0 "pavement:1" ""
              hex 0102 0 "pavement:1" ""
              hex 0103 0 "pavement:1;black_ice:1" ""
              hex 0104 0 "pavement:1" ""
              hex 0105 0 "pavement:1" ""
              end"""));
        BipedMek mek = new BipedMek();
        TWGameManager manager = setUpMover(mek, EntityMovementMode.BIPED);
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(8, location);
        }
        Game.rulesManager = new TWRulesManager();
        Coords icyHex = new Coords(0, 2);
        doAnswer(invocation -> {
            mek.setProne(true);
            mek.setPosition(icyHex);
            return 2;
        }).when(manager).doSkillCheckWhileMoving(eq(mek), eq(0), eq(new Coords(0, 1)), eq(icyHex),
              any(PilotingRollData.class), eq(true));
        MovePath path = new MovePath(getGame(), mek);
        path.setCareful(false);
        path.addStep(MoveStepType.FORWARDS).addStep(MoveStepType.FORWARDS);

        new MovePathHandler(manager, mek, path, null).processMovement();

        assertTrue(mek.isProne());
        assertEquals(new Coords(0, 3), mek.getPosition());
        assertEquals(EntityMovementType.MOVE_SKID, mek.moved);
    }

    @ParameterizedTest
    @CsvSource({ "pavement:1, true", "road:3, false" })
    void forcedEntryChecksTheSurfaceForBlackIce(String surface, boolean revealsIce) throws IOException {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 4
              hex 0101 0 "%1$s" ""
              hex 0102 0 "%1$s" ""
              hex 0103 0 "%1$s" ""
              hex 0104 0 "%1$s" ""
              end""".formatted(surface)));
        Tank tank = new Tank();
        TWGameManager manager = setUpMover(tank, EntityMovementMode.WHEELED);
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_BLACK_ICE).setValue(true);
        getGame().getPlanetaryConditions().setTemperature(PlanetaryConditions.BLACK_ICE_TEMP);
        MoveStep step = new MovePath(getGame(), tank).addStep(MoveStepType.FORWARDS).getLastStep();
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.d6(1)).thenReturn(6);
            manager.processSkid(tank, new Coords(0, 0), 0, 3, 2, step, EntityMovementType.MOVE_RUN);
            assertEquals(new Coords(0, 2), tank.getPosition());
            assertEquals(revealsIce, getGame().getBoard().getHex(0, 1).containsTerrain(Terrains.BLACK_ICE));
            assertEquals(revealsIce, getGame().getBoard().getHex(0, 2).containsTerrain(Terrains.BLACK_ICE));
            manager.doEntityDisplacement(tank, tank.getPosition(), new Coords(0, 3), null);
            assertEquals(revealsIce, getGame().getBoard().getHex(0, 3).containsTerrain(Terrains.BLACK_ICE));
        }
    }

    @Test
    void controlledSideslipCanStartAnAllRoadMove() throws IOException {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 3 2
              hex 0101 0 "road:1" ""
              hex 0201 0 "road:1" ""
              hex 0301 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              hex 0302 0 "road:1" ""
              end"""));
        Tank hover = new Tank();
        TWGameManager manager = setUpMover(hover, EntityMovementMode.HOVER);
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS)
              .setValue(true);
        MovePath plotted = new MovePath(getGame(), hover).addStep(MoveStepType.LATERAL_LEFT);
        assertTrue(plotted.isMoveLegal());
        failSideslipAt(manager, hover, new Coords(0, 0), new Coords(1, 0));

        new MovePathHandler(manager, hover, plotted, null).processMovement();
        assertEquals(new Coords(2, 1), hover.getPosition());
        assertEquals(plotted.getMpUsed(), hover.mpUsed, "The intentional lateral entry still costs its MP");
        assertTrue(hover.turnWasInterrupted());
        assertTrue(hover.gotPavementOrRoadBonus);
    }

    private TWGameManager setUpMover(Entity unit, EntityMovementMode mode) throws IOException {
        TWGameManager manager = spy(new TWGameManager());
        manager.setGame(getGame());
        ServerFactory.createServer(manager);
        Player player = new Player(0, "Driver");
        getGame().addPlayer(0, player);
        unit.setOwner(player);
        unit.setCrew(new Crew(CrewType.SINGLE));
        unit.setWeight(50);
        unit.autoSetInternal();
        unit.setOriginalWalkMP(4);
        getMovePathFor(unit, 0, mode);
        getGame().setPhase(GamePhase.MOVEMENT);
        getGame().setTurnVector(List.of(new SpecificEntityTurn(player.getId(), unit.getId())));
        getGame().setTurnIndex(0, player.getId());
        return manager;
    }

    private void failSideslipAt(TWGameManager manager, Tank hover, Coords from, Coords to) {
        doReturn(2).when(manager).doSkillCheckWhileMoving(eq(hover), eq(0), eq(from), eq(to),
              any(PilotingRollData.class), eq(false));
    }
}
