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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.units.TripodMek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/**
 * Core p.127 treats a breached leg as destroyed, including the automatic fall on p.90. TW p.121 instead applies
 * the effects of the breached actuators. In both systems the location and its critical slots can still take damage.
 */
class LegBreachTest {

    private TWGameManager gameManager;
    private Game game;
    private RulesManager previousRules;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        previousRules = Game.rulesManager;
        gameManager = spy(new TWGameManager());
        doNothing().when(gameManager).send(any(Packet.class));
        doNothing().when(gameManager).sendServerChat(anyString());
        game = gameManager.getGame();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.addPlayer(0, new Player(0, "Test"));
        game.setPhase(GamePhase.FIRING);
        Hex[] hexes = Stream.generate(Hex::new).limit(9).toArray(Hex[]::new);
        Arrays.stream(hexes).forEach(hex -> hex.addTerrain(new Terrain(Terrains.WATER, 1)));
        game.setBoard(new Board(3, 3, hexes));
    }

    @AfterEach
    void restoreRules() {
        Game.rulesManager = previousRules;
    }

    private <T extends Mek> T prepareMek(T mek) {
        mek.setId(1);
        mek.setOwner(game.getPlayer(0));
        mek.setWeight(50);
        mek.setOriginalWalkMP(6);
        for (int loc = 0; loc < mek.locations(); loc++) {
            mek.initializeInternal(15, loc);
            mek.initializeArmor(40, loc);
            if (mek.hasRearArmor(loc)) {
                mek.initializeRearArmor(40, loc);
            }
        }
        game.addEntity(mek);
        mek.setPosition(new Coords(1, 1));
        mek.setElevation(-1);
        mek.setDeployed(true);
        return mek;
    }

    private void breach(Mek mek, int loc) {
        // Stripped armor guarantees a breach without a random breach check.
        mek.setArmor(0, loc);
        mek.setLocationStatus(loc, ILocationExposureStatus.WET);
        gameManager.breachCheck(mek, loc, null);
    }

    private static Stream<Arguments> legLocations() {
        Supplier<Mek> biped = BipedMek::new;
        Supplier<Mek> quad = QuadMek::new;
        Supplier<Mek> tripod = TripodMek::new;
        return Stream.of(
              Arguments.of(biped, Mek.LOC_LEFT_LEG),
              Arguments.of(biped, Mek.LOC_RIGHT_LEG),
              Arguments.of(quad, Mek.LOC_LEFT_ARM),
              Arguments.of(quad, Mek.LOC_RIGHT_ARM),
              Arguments.of(quad, Mek.LOC_LEFT_LEG),
              Arguments.of(quad, Mek.LOC_RIGHT_LEG),
              Arguments.of(tripod, Mek.LOC_CENTER_LEG));
    }

    @ParameterizedTest
    @MethodSource("legLocations")
    void coreBreachedLegQueuesOneAutomaticFall(Supplier<Mek> factory, int loc) {
        Mek mek = prepareMek(factory.get());

        breach(mek, loc);

        List<PilotingRollData> rolls = game.getPSRsForEntity(mek);
        assertEquals(1, rolls.size());
        assertEquals(TargetRoll.AUTOMATIC_FAIL, rolls.getFirst().getValue());
        assertEquals(loc, rolls.getFirst().getLocation());
        assertTrue(mek.isLocationBad(loc));
        assertEquals(1, mek.countBadLegs());
    }

    @Test
    void coreBreachedLegActuallyFallsWhenPilotingIsResolved() {
        BipedMek mek = prepareMek(new BipedMek());
        breach(mek, Mek.LOC_LEFT_LEG);

        assertTrue(gameManager.resolvePilotingRolls(mek).stream().anyMatch(report -> report.messageId == 2296),
              "The fall must be automatic, regardless of the pilot's dice");

        assertTrue(mek.isProne());
        assertTrue(mek.hasFallen());
    }

    @Test
    void coreBreachedLegReplacesActuatorPenaltiesWithDestroyedLegPenalties() {
        BipedMek mek = prepareMek(new BipedMek());
        int basePiloting = mek.getBasePilotingRoll().getValue();
        breach(mek, Mek.LOC_LEFT_LEG);

        assertEquals(1, mek.getWalkMP());
        assertEquals(2, mek.getRunMP());
        assertEquals(basePiloting + 4, mek.getBasePilotingRoll().getValue());
        PilotingRollData fall = mek.getBasePilotingRoll();
        game.getPSRsForEntity(mek).forEach(fall::append);
        fall.removeAutos();
        assertEquals(basePiloting + 4, fall.getValue(), "The destroyed-leg modifier must only be counted once");
    }

    @Test
    void coreBreachedLegReplacesEarlierActuatorRollsInThatLeg() {
        BipedMek mek = prepareMek(new BipedMek());
        gameManager.applyCriticalHit(mek, Mek.LOC_LEFT_LEG, mek.getCritical(Mek.LOC_LEFT_LEG, 1), true, 0, false);
        assertEquals(1, game.getPSRsForEntity(mek).size());

        breach(mek, Mek.LOC_LEFT_LEG);

        assertEquals(1, game.getPSRsForEntity(mek).size());
        assertEquals(TargetRoll.AUTOMATIC_FAIL, game.getPSRsForEntity(mek).getFirst().getValue());
    }

    @Test
    void coreBothLegsBreachedMakesMekImmobile() {
        BipedMek mek = prepareMek(new BipedMek());
        breach(mek, Mek.LOC_LEFT_LEG);
        breach(mek, Mek.LOC_RIGHT_LEG);

        assertEquals(2, mek.countBadLegs());
        assertEquals(0, mek.getWalkMP());
        assertTrue(mek.isImmobile());
        assertEquals(TargetRoll.AUTOMATIC_FAIL, mek.getBasePilotingRoll().getValue());
    }

    @Test
    void coreQuadSideTorsoBreachAlsoDisablesFrontLeg() {
        QuadMek mek = prepareMek(new QuadMek());
        breach(mek, Mek.LOC_LEFT_TORSO);

        assertTrue(mek.isLocationBad(Mek.LOC_LEFT_ARM));
        assertEquals(5, mek.getWalkMP());
        assertEquals(1, game.getPSRsForEntity(mek).size());
        assertEquals(TargetRoll.AUTOMATIC_FAIL, game.getPSRsForEntity(mek).getFirst().getValue());
    }

    @Test
    void coreBreachedLegStaysDisabledWithoutRepeatingTheFall() {
        BipedMek mek = prepareMek(new BipedMek());
        breach(mek, Mek.LOC_LEFT_LEG);
        game.resetPSRs(mek);
        mek.applyDamage();

        gameManager.doSetLocationsExposure(mek, game.getHexOf(mek), false, -1);
        mek.setElevation(0);
        gameManager.doSetLocationsExposure(mek, new Hex(), false, 0);

        assertTrue(mek.isLocationBad(Mek.LOC_LEFT_LEG));
        assertEquals(1, mek.getWalkMP());
        assertTrue(game.getPSRsForEntity(mek).isEmpty());
    }

    @Test
    void coreProneMekDoesNotFallAgainWhenItsLegBreaches() {
        BipedMek mek = prepareMek(new BipedMek());
        mek.setProne(true);
        breach(mek, Mek.LOC_LEFT_LEG);

        assertTrue(mek.isLocationBad(Mek.LOC_LEFT_LEG));
        assertTrue(game.getPSRsForEntity(mek).isEmpty());
    }

    @Test
    void coreBreachDuringMovementStopsThePathInTheWaterHex() {
        game.setPhase(GamePhase.MOVEMENT);
        QuadMek mek = prepareMek(new QuadMek());
        Coords start = new Coords(1, 0);
        Coords water = new Coords(1, 1);
        game.getBoard().setHex(start, new Hex());
        mek.setPosition(start);
        mek.setElevation(0);
        mek.setFacing(3);
        mek.setArmor(0, Mek.LOC_LEFT_ARM);
        MovePath path = new MovePath(game, mek);
        path.addStep(MoveStepType.FORWARDS);
        path.addStep(MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        assertEquals(new Coords(1, 2), path.getFinalCoords());

        new MovePathHandler(gameManager, mek, path, null).processMovement();

        assertTrue(mek.isProne());
        assertEquals(water, mek.getPosition(), "The remaining steps cannot execute after the leg floods");
        assertTrue(mek.isDone(), "Losing a leg ends movement, even with MP remaining");
        assertTrue(game.getPSRsForEntity(mek).isEmpty(), "The fall was resolved during movement");
    }

    @Test
    void coreJumpLandingInWaterResolvesTheBreachFallImmediately() throws LocationFullException {
        game.setPhase(GamePhase.MOVEMENT);
        BipedMek mek = prepareMek(new BipedMek());
        Coords start = new Coords(1, 0);
        game.getBoard().setHex(start, new Hex());
        mek.setPosition(start);
        mek.setElevation(0);
        mek.setFacing(3);
        mek.setOriginalJumpMP(1);
        mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.JUMP_JET), Mek.LOC_CENTER_TORSO);
        mek.setArmor(0, Mek.LOC_LEFT_LEG);
        MovePath path = new MovePath(game, mek);
        path.addStep(MoveStepType.START_JUMP);
        path.addStep(MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());

        new MovePathHandler(gameManager, mek, path, null).processMovement();

        assertTrue(mek.isProne());
        assertEquals(new Coords(1, 1), mek.getPosition());
        assertTrue(game.getPSRsForEntity(mek).isEmpty());
    }

    @Test
    void coreBreachedArmDoesNotCauseALegDestructionFall() {
        BipedMek mek = prepareMek(new BipedMek());
        breach(mek, Mek.LOC_LEFT_ARM);

        assertEquals(0, mek.countBadLegs());
        assertEquals(6, mek.getWalkMP());
        assertTrue(game.getPSRsForEntity(mek).isEmpty());
    }

    @Test
    void coreBreachedLegRetainsArmorAndStillAbsorbsDamage() {
        BipedMek mek = prepareMek(new BipedMek());
        mek.setLocationStatus(Mek.LOC_LEFT_LEG, ILocationExposureStatus.WET);
        Roll breachRoll = mock(Roll.class);
        when(breachRoll.getIntValue()).thenReturn(2);
        when(breachRoll.getReport()).thenReturn("2");
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.rollD6(2)).thenReturn(breachRoll);
            gameManager.breachCheck(mek, Mek.LOC_LEFT_LEG, null);
        }
        assertTrue(mek.isLocationBad(Mek.LOC_LEFT_LEG));
        assertEquals(40, mek.getArmor(Mek.LOC_LEFT_LEG));

        gameManager.damageEntity(mek, new HitData(Mek.LOC_LEFT_LEG), 5);

        assertEquals(35, mek.getArmor(Mek.LOC_LEFT_LEG));
        assertEquals(15, mek.getInternal(Mek.LOC_LEFT_LEG));
        assertEquals(40, mek.getArmor(Mek.LOC_LEFT_TORSO), "Damage must not transfer through a merely breached leg");
    }

    @ParameterizedTest
    @ValueSource(strings = { OptionsConstants.RULES_CORE, OptionsConstants.RULES_TW })
    void breachedLegRetainsStructureAndHittableCriticalSlots(String rules) {
        game.initializeRulesManager(rules);
        BipedMek mek = prepareMek(new BipedMek());
        breach(mek, Mek.LOC_LEFT_LEG);
        mek.applyDamage();

        assertEquals(15, mek.getInternal(Mek.LOC_LEFT_LEG));
        assertFalse(mek.isLocationTrulyDestroyed(Mek.LOC_LEFT_LEG));
        assertEquals(ILocationExposureStatus.BREACHED, mek.getLocationStatus(Mek.LOC_LEFT_LEG));
        CriticalSlot hip = mek.getCritical(Mek.LOC_LEFT_LEG, 0);
        assertTrue(hip.isBreached());
        assertTrue(hip.isHittable());
    }

    @Test
    void totalWarfareStillQueuesEachUndamagedActuator() {
        game.initializeRulesManager(OptionsConstants.RULES_TW);
        BipedMek mek = prepareMek(new BipedMek());
        int basePiloting = mek.getBasePilotingRoll().getValue();
        breach(mek, Mek.LOC_LEFT_LEG);

        assertEquals(List.of(0, 1, 1, 1), game.getPSRsForEntity(mek).stream().map(PilotingRollData::getValue).toList());
        assertFalse(mek.isLocationBad(Mek.LOC_LEFT_LEG));
        assertEquals(0, mek.countBadLegs());
        assertEquals(basePiloting + 2, mek.getBasePilotingRoll().getValue());
    }

    @Test
    void totalWarfareDoesNotQueueAnotherRollForAnAlreadyDamagedActuator() {
        game.initializeRulesManager(OptionsConstants.RULES_TW);
        BipedMek mek = prepareMek(new BipedMek());
        mek.getCritical(Mek.LOC_LEFT_LEG, 3).setDestroyed(true);
        breach(mek, Mek.LOC_LEFT_LEG);

        assertEquals(List.of(0, 1, 1), game.getPSRsForEntity(mek).stream().map(PilotingRollData::getValue).toList());
    }
}
