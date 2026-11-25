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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ui.SharedUtility;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RoadHazards;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.SupportTank;
import megamek.common.units.Tank;
import megamek.common.units.TripodMek;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class RoadVehicleMovementTest {
    private static final Coords START = new Coords(3, 2);
    private static final Coords END = START.translated(3);

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "VTOL", "WIGE" })
    void lowFlightThroughWoodsRequiresRoadTravel(EntityMovementMode mode) {
        for (String road : new String[] { "road:1", "pavement:1", "road:3", "road:4" }) {
            var woods = road + ";woods:2;foliage_elev:2";
            assertFalse(fly(mode, "", woods), "Entering the woods from clear ground on " + road);
            assertFalse(fly(mode, woods, ""), "Leaving the road below the trees on " + road);
            assertTrue(fly(mode, road, woods), "Following the road into woods on " + road);
        }
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "VTOL", "WIGE" })
    void flyingUnderABridgeStillChecksTheWoodsBelowItsDeck(EntityMovementMode mode) {
        String woodedUnderpass = "woods:2;foliage_elev:2;bridge:1:9;bridge_elev:4;bridge_cf:100;road:1";
        assertFalse(fly(mode, "", woodedUnderpass));
        assertTrue(fly(mode, "road:1", woodedUnderpass));
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "VTOL", "WIGE" })
    void lowFlightCanTurnOnAWoodedRoad(EntityMovementMode mode) {
        setRoadBoard("road:1", "road:1;woods:2;foliage_elev:2");
        Tank aircraft = mode.isVTOL() ? new VTOL() : new Tank();
        aircraft.setMovementMode(mode);
        MovePath path = move(aircraft, 1, MoveStepType.FORWARDS, MoveStepType.TURN_LEFT);
        assertTrue(path.isMoveLegal());
        path.addStep(MoveStepType.FORWARDS);
        assertFalse(path.isMoveLegal(), "Turning on the road does not let the unit leave it below the canopy");
    }

    private boolean fly(EntityMovementMode mode, String from, String to) {
        setRoadBoard(from, to);
        Tank aircraft = mode.isVTOL() ? new VTOL() : new Tank();
        aircraft.setMovementMode(mode);
        return move(aircraft, 1, MoveStepType.FORWARDS).isMoveLegal();
    }

    @ParameterizedTest
    @ValueSource(strings = { "pavement:1", "road:1", "road:3", "road:4" })
    void jumpingWheeledTankCannotUseTheRoadToLandInWoods(String road) {
        setRoadBoard(road, road + ";woods:2");
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.WHEELED);
        tank.setOriginalJumpMP(4);
        assertFalse(move(tank, 0, MoveStepType.START_JUMP, MoveStepType.FORWARDS).isMoveLegal());
    }

    @ParameterizedTest
    @ValueSource(strings = { "pavement:1", "road:1", "road:3", "road:4" })
    void taxiingDropShipChecksRoadsUnderItsWholeFootprint(String road) {
        assertTrue(taxi(road, ""), "Open secondary hex");
        assertFalse(taxi(road, "woods:2"), "Road under the center only");
        assertTrue(taxi(road, road + ";woods:2"), "Secondary hex follows the same road");
    }

    @ParameterizedTest
    @ValueSource(strings = { "pavement:1", "road:1", "road:3", "road:4" })
    void taxiUsesTheSurfaceUnderEachSecondaryHexWithTheCenterOnClearGround(String road) {
        Board board = setRoadBoard("", "");
        board.setHex(START.translated(2), new Hex(0, road, ""));
        board.setHex(END.translated(2), new Hex(0, road + ";woods:2", ""));
        assertTrue(taxiPath().addStep(MoveStepType.TURN_LEFT).isMoveLegal(),
              "The wing can remain on its road while the DropShip turns");

        board = setRoadBoard("", "");
        board.setHex(START.translated(0), new Hex(0, road + ";woods:2", ""));
        assertFalse(taxi(), "The rear of the DropShip would leave its road through woods");
    }

    private boolean taxi(String road, String secondaryTerrain) {
        Board board = setRoadBoard(road, road);
        board.setHex(END.translated(3), new Hex(0, secondaryTerrain, ""));
        return taxi();
    }

    private boolean taxi() {
        return taxiPath().isMoveLegal();
    }

    private MovePath taxiPath() {
        Dropship dropship = new Dropship();
        dropship.setSpheroid(false);
        dropship.land();
        return move(dropship, 0, MoveStepType.FORWARDS);
    }

    @ParameterizedTest
    @ValueSource(strings = { "road:1", "pavement:1", "road:3", "road:4" })
    void wheeledSupportTrailerNeedsARoadThroughLightWoods(String road) {
        assertFalse(tow("", road + ";woods:1"));
        assertFalse(tow(road + ";woods:1", ""), "The wheeled trailer cannot leave these woods off-road");
        assertTrue(tow(road, road + ";woods:1"));
        assertTrue(tow(road + ";woods:1", road));
    }

    private boolean tow(String from, String to) {
        setRoadBoard(from, to);
        Tank tractor = new Tank();
        tractor.setMovementMode(EntityMovementMode.TRACKED);
        SupportTank trailer = new SupportTank();
        trailer.setId(2);
        trailer.setWeight(5);
        trailer.setMovementMode(EntityMovementMode.WHEELED);
        trailer.setPosition(START.translated(0));
        game.addEntity(trailer);
        tractor.addTowedUnit(trailer.getId());
        return move(tractor, 0, MoveStepType.FORWARDS).isMoveLegal();
    }

    @ParameterizedTest
    @CsvSource({ "'', true", "woods:1, false", "woods:1;road:1, true", "woods:1;pavement:1, true",
          "woods:1;road:3, false", "woods:1;road:4, false", "road:3, true", "road:4, true" })
    void landingAndRunwaysRequireClearOrPavedTerrain(String terrain, boolean allowed) {
        Hex hex = new Hex(0, terrain, "");
        Tank wige = new Tank();
        wige.setMovementMode(EntityMovementMode.WIGE);
        assertEquals(allowed, hex.isClearForTakeoff());
        assertEquals(allowed, new VTOL().canLandIn(hex));
        assertEquals(allowed, wige.canLandIn(hex));
    }

    @ParameterizedTest
    @CsvSource({ "road:1, false", "road:3, false", "road:4, false", "pavement:1, true",
          "pavement:1;rubble:1, false" })
    void liftoffPadModifierUsesDedicatedPavement(String terrain, boolean padBonus) {
        setRoadBoard(terrain, "");
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.land();
        move(fighter, 0);
        assertEquals(padBonus, fighter.checkVerticalTakeOff().getDesc().contains("on landing pad"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "road:1", "road:3", "road:4", "pavement:1" })
    void ordinaryRoadsCannotBypassASheerCliffHexside(String road) {
        Board board = setRoadBoard(road, "");
        board.setHex(END, new Hex(1, road + ";cliff_top:1:1", ""));
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.TRACKED);
        assertFalse(move(tank, 0, MoveStepType.FORWARDS).isMoveLegal());

        board = setRoadBoard("", road);
        board.setHex(START, new Hex(1, road + ";cliff_top:1:8", ""));
        MovePath path = move(tank, 0, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        assertTrue(SharedUtility.getPSRList(path).stream()
              .anyMatch(roll -> roll.getDesc().contains("moving down a sheer cliff")));
    }

    @Test
    void anElevatedBridgeDeckSpansTheCliffBelowIt() {
        Board board = setRoadBoard("road:1;bridge:2:9;bridge_elev:1;bridge_cf:100", "");
        board.setHex(END, new Hex(2, "road:1;cliff_top:1:1", ""));
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.TRACKED);
        assertTrue(move(tank, 1, MoveStepType.FORWARDS).isMoveLegal());
        assertTrue(board.getHex(END).crossesCliffTopTowards(board.getHex(START), 0, 0),
              "The cliff still exists for a unit underneath the bridge");
    }

    @Test
    void engineerBridgeEntryFromClearGroundCostsOneMP() {
        setRoadBoard("", "bridge:2:9;bridge_elev:0;bridge_cf:100;water:2");
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.WHEELED);
        MovePath path = move(tank, 0, MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        assertEquals(0, path.getFinalElevation());
        assertFalse(path.getLastStep().isOnlyPavementOrRoad());
        assertEquals(1, path.getMpUsed());

        tank.setOriginalWalkMP(2);
        MovePath running = new MovePath(game, tank);
        running.addStep(MoveStepType.CLIMB_MODE_ON);
        running.addStep(MoveStepType.TURN_LEFT);
        running.addStep(MoveStepType.TURN_RIGHT);
        running.addStep(MoveStepType.FORWARDS);
        assertTrue(running.isMoveLegal(), "Entering the deck at flank speed is not entering water");
        assertEquals(EntityMovementType.MOVE_RUN, running.getLastStepMovementType());
    }

    @Test
    void mudExemptionsDependOnTheUnitAndPreserveOtherBoggingTerrain() {
        Hex mud = new Hex(0, "road:3;mud:1;swamp:1", "");
        for (Entity unit : new Entity[] { new BipedMek(), new TripodMek(), new Dropship() }) {
            assertEquals(TargetRoll.AUTOMATIC_SUCCESS, RoadHazards.bogDownModifier(unit, mud, true, 0));
            assertEquals(0, RoadHazards.bogDownModifier(unit, mud, false, 0), "Off-road swamp remains active");
        }
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.WHEELED);
        assertEquals(-1, RoadHazards.bogDownModifier(tank, mud, true, 0));
    }

    private Board setRoadBoard(String from, String to) {
        Board board = Board.createEmptyBoard(7, 7);
        board.setHex(START, new Hex(0, from, ""));
        board.setHex(END, new Hex(0, to, ""));
        game = new Game();
        game.setBoard(board);
        return board;
    }

    private MovePath move(Entity unit, int elevation, MoveStepType... steps) {
        unit.setId(1);
        unit.setWeight(unit instanceof Dropship ? 2_000 : 50);
        unit.setOriginalWalkMP(8);
        game.addEntity(unit);
        unit.setPosition(START);
        unit.setFacing(3);
        unit.setElevation(elevation);
        MovePath path = new MovePath(game, unit);
        for (MoveStepType step : steps) {
            path.addStep(step);
        }
        return path;
    }
}
