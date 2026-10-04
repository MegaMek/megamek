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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Supplier;

import megamek.common.CriticalSlot;
import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.bays.BattleArmorBay;
import megamek.common.bays.Bay;
import megamek.common.bays.HeavyVehicleBay;
import megamek.common.bays.InfantryBay;
import megamek.common.bays.LightVehicleBay;
import megamek.common.bays.MekBay;
import megamek.common.bays.ProtoMekBay;
import megamek.common.bays.SuperHeavyVehicleBay;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.PlatoonType;
import megamek.common.units.ProtoMek;
import megamek.common.units.QuadMek;
import megamek.common.units.QuadVee;
import megamek.common.units.SuperHeavyTank;
import megamek.common.units.Tank;
import megamek.common.units.TripodMek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks every ground unit type that can mount a grounded DropShip under its own power against the bay that can carry
 * it and against one that cannot (TW p.89-90: the DropShip must contain an appropriate bay for the type of unit
 * mounting). VTOLs are not covered: they cannot mount under their own power and are loaded by cranes instead (TW p.90).
 * <p>
 * Layout, one hex wide: the unit starts at (0, 0) facing south. For the walking checks the grounded DropShip is centred
 * at (0, 3) facing north, so its footprint covers (0, 2) to (0, 4) and (0, 1) is the hex beside it. For the infantry
 * checks it is centred at (0, 2), so the infantry already starts beside it.
 * </p>
 */
class MountBayMatrixTest extends GameBoardTestCase {

    private static final int MOVING_UNIT_ID = 5;
    private static final Coords HEX_BESIDE_DROPSHIP = new Coords(0, 1);
    private static final Coords DROPSHIP_CENTRE = new Coords(0, 3);
    private static final Coords DROPSHIP_CENTRE_BESIDE_START = new Coords(0, 2);

    static {
        initializeBoard("COLUMN_BOARD", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end""");
    }

    /**
     * One unit type, the bay that carries it and a bay that does not.
     *
     * @param name        label shown in the test report
     * @param unit        builds the moving unit, fully configured
     * @param carryingBay a bay this unit can mount into
     * @param wrongBay    a bay this unit cannot mount into
     */
    record MountCase(String name, Supplier<Entity> unit, Supplier<Bay> carryingBay, Supplier<Bay> wrongBay) {
        @Override
        public String toString() {
            return name;
        }
    }

    static List<MountCase> walkingMountCases() {
        return List.of(
              new MountCase("Biped Mek / Mek bay", () -> mek(new BipedMek()), MountBayMatrixTest::mekBay,
                    MountBayMatrixTest::lightVehicleBay),
              new MountCase("Quad Mek / Mek bay", () -> mek(new QuadMek()), MountBayMatrixTest::mekBay,
                    MountBayMatrixTest::lightVehicleBay),
              new MountCase("Tripod Mek / Mek bay", () -> mek(new TripodMek()), MountBayMatrixTest::mekBay,
                    MountBayMatrixTest::lightVehicleBay),
              new MountCase("LAM in Mek mode / Mek bay", MountBayMatrixTest::landAirMek, MountBayMatrixTest::mekBay,
                    MountBayMatrixTest::lightVehicleBay),
              new MountCase("QuadVee in vehicle mode / heavy vehicle bay", MountBayMatrixTest::quadVeeVehicleMode,
                    MountBayMatrixTest::heavyVehicleBay, MountBayMatrixTest::mekBay),
              new MountCase("ProtoMek / ProtoMek bay", MountBayMatrixTest::protoMek,
                    () -> new ProtoMekBay(1, 1, 1), MountBayMatrixTest::mekBay),
              new MountCase("Light tracked tank / light vehicle bay",
                    () -> tank(new Tank(), EntityMovementMode.TRACKED, 40), MountBayMatrixTest::lightVehicleBay,
                    MountBayMatrixTest::mekBay),
              new MountCase("Light hover tank / light vehicle bay",
                    () -> tank(new Tank(), EntityMovementMode.HOVER, 30), MountBayMatrixTest::lightVehicleBay,
                    MountBayMatrixTest::mekBay),
              new MountCase("Heavy wheeled tank / heavy vehicle bay",
                    () -> tank(new Tank(), EntityMovementMode.WHEELED, 80), MountBayMatrixTest::heavyVehicleBay,
                    MountBayMatrixTest::lightVehicleBay),
              new MountCase("Superheavy tank / superheavy vehicle bay",
                    () -> tank(new SuperHeavyTank(), EntityMovementMode.TRACKED, 150),
                    () -> new SuperHeavyVehicleBay(2, 1, 1), MountBayMatrixTest::heavyVehicleBay));
    }

    static List<MountCase> infantryMountCases() {
        return List.of(
              new MountCase("Foot infantry / foot infantry bay", () -> infantry(EntityMovementMode.INF_LEG),
                    () -> new InfantryBay(2, 1, 1, PlatoonType.FOOT), MountBayMatrixTest::mekBay),
              new MountCase("Jump infantry / jump infantry bay", () -> infantry(EntityMovementMode.INF_JUMP),
                    () -> new InfantryBay(2, 1, 1, PlatoonType.JUMP), MountBayMatrixTest::mekBay),
              new MountCase("Motorized infantry / motorized infantry bay",
                    () -> infantry(EntityMovementMode.INF_MOTORIZED),
                    () -> new InfantryBay(2, 1, 1, PlatoonType.MOTORIZED), MountBayMatrixTest::mekBay),
              new MountCase("Mechanized infantry / mechanized infantry bay",
                    () -> infantry(EntityMovementMode.TRACKED),
                    () -> new InfantryBay(2, 1, 1, PlatoonType.MECHANIZED), MountBayMatrixTest::mekBay),
              new MountCase("Battle armor / battle armor bay", MountBayMatrixTest::battleArmor,
                    () -> new BattleArmorBay(2, 1, 1, false, false), MountBayMatrixTest::mekBay));
    }

    // ---- Units that walk up and mount: Meks, ProtoMeks and vehicles (TW p.90) ----

    @ParameterizedTest(name = "{0}")
    @MethodSource("walkingMountCases")
    void clickingTransportWithCarryingBayStopsBesideItAndMounts(MountCase mountCase) {
        Dropship dropShip = placeDropShip(mountCase.carryingBay().get(), DROPSHIP_CENTRE);
        MovePath movePath = walkSouth(mountCase, 2);
        Entity movingUnit = movePath.getEntity();

        Entity transport = MountPathHelper.trimToMountableTransport(movePath, DROPSHIP_CENTRE, 0, getGame());

        assertSame(dropShip, transport, "The clicked DropShip should be the transport to mount");
        assertEquals(HEX_BESIDE_DROPSHIP, movePath.getFinalCoords(), "The path should stop beside the DropShip");
        assertEquals(1, movePath.length(), "The step inside the DropShip should be removed");
        assertTrue(Compute.getMountableUnits(movingUnit, movePath.getFinalCoords(), 0, movePath.getFinalElevation(),
              getGame()).contains(dropShip), "The DropShip should be mountable from beside it");

        movePath.addStep(MoveStepType.MOUNT, dropShip);
        movePath.clipToPossible();
        assertEquals(2, movePath.length(), "The walk and the mount step should survive clipping");
        assertEquals(MoveStepType.MOUNT, movePath.getLastStep().getType(), "The path should end by mounting");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("walkingMountCases")
    void clickingTransportWithWrongBayLeavesPathAlone(MountCase mountCase) {
        placeDropShip(mountCase.wrongBay().get(), DROPSHIP_CENTRE);
        MovePath movePath = walkSouth(mountCase, 2);
        Entity movingUnit = movePath.getEntity();

        Entity transport = MountPathHelper.trimToMountableTransport(movePath, DROPSHIP_CENTRE, 0, getGame());

        assertNull(transport, "A DropShip without a suitable bay is not a mount target");
        assertEquals(2, movePath.length(), "The path should not be trimmed");
        assertTrue(Compute.getMountableUnits(movingUnit, HEX_BESIDE_DROPSHIP, 0, movePath.getFinalElevation(),
              getGame()).isEmpty(), "Nothing should be mountable without a suitable bay");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("walkingMountCases")
    void mountingThroughFriendlyStackSurvivesClipping(MountCase mountCase) {
        Dropship dropShip = placeDropShip(mountCase.carryingBay().get(), DROPSHIP_CENTRE);
        placeFriendlyMek(30);
        placeFriendlyMek(31);
        MovePath movePath = walkSouth(mountCase, 1);

        assertFalse(movePath.getLastStep().isLegalEndPos(),
              "Two friendly Meks in the hex should make it an illegal place to end the move");

        movePath.addStep(MoveStepType.MOUNT, dropShip);
        movePath.clipToPossible();

        assertEquals(2, movePath.length(), "Moving through the friendly stack and mounting should survive clipping");
        assertEquals(MoveStepType.MOUNT, movePath.getLastStep().getType(), "The path should end by mounting");
    }

    @Test
    @DisplayName("A Mek without enough Walking MP left to pay the mounting cost cannot mount (TW p.90)")
    void mountingCostIsEnforcedOnThePath() {
        Dropship dropShip = placeDropShip(mekBay(), DROPSHIP_CENTRE);
        BipedMek walkOneMek = new BipedMek();
        walkOneMek.setId(MOVING_UNIT_ID);
        walkOneMek.setWeight(50);
        walkOneMek.setOriginalWalkMP(1);
        MovePath movePath = walkSouth(walkOneMek, 1);

        movePath.addStep(MoveStepType.MOUNT, dropShip);
        movePath.clipToPossible();

        assertEquals(1, movePath.length(), "Walk 1: 1 MP spent + 1 to mount = 2, so the mount step is cut");
        assertEquals(MoveStepType.FORWARDS, movePath.getLastStep().getType(), "Only the walk should remain");
    }

    // ---- Infantry mount as though the DropShip were a Large Support Vehicle (TW p.89, p.223-224) ----

    @ParameterizedTest(name = "{0}")
    @MethodSource("infantryMountCases")
    void infantryThatHasNotMovedMounts(MountCase mountCase) {
        Dropship dropShip = placeDropShip(mountCase.carryingBay().get(), DROPSHIP_CENTRE_BESIDE_START);
        MovePath movePath = walkSouth(mountCase, 0);
        Entity infantry = movePath.getEntity();

        assertTrue(Compute.getMountableUnits(infantry, movePath.getFinalCoords(), 0, movePath.getFinalElevation(),
              getGame()).contains(dropShip), "The DropShip should be mountable from the infantry's starting hex");

        movePath.addStep(MoveStepType.MOUNT, dropShip);
        movePath.clipToPossible();

        assertEquals(1, movePath.length(), "Mounting without moving first should survive clipping");
        assertEquals(MoveStepType.MOUNT, movePath.getLastStep().getType(), "The path should be the mount step");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("infantryMountCases")
    void infantryThatMovedCannotMount(MountCase mountCase) {
        Dropship dropShip = placeDropShip(mountCase.carryingBay().get(), DROPSHIP_CENTRE);
        MovePath movePath = walkSouth(mountCase, 1);

        movePath.addStep(MoveStepType.MOUNT, dropShip);
        movePath.clipToPossible();

        assertEquals(1, movePath.length(), "Infantry that walked a hex must not be able to mount the same turn");
        assertEquals(MoveStepType.FORWARDS, movePath.getLastStep().getType(), "Only the walk should remain");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("infantryMountCases")
    void clickingTransportLeavesInfantryPathAlone(MountCase mountCase) {
        placeDropShip(mountCase.carryingBay().get(), DROPSHIP_CENTRE);
        MovePath movePath = walkSouth(mountCase, 2);

        Entity transport = MountPathHelper.trimToMountableTransport(movePath, DROPSHIP_CENTRE, 0, getGame());

        assertNull(transport, "Infantry may legally end a move in a DropShip hex, so the path is left alone");
        assertEquals(2, movePath.length(), "The path should not be trimmed");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("infantryMountCases")
    void infantryWithWrongBayCannotMount(MountCase mountCase) {
        placeDropShip(mountCase.wrongBay().get(), DROPSHIP_CENTRE_BESIDE_START);
        MovePath movePath = walkSouth(mountCase, 0);
        Entity infantry = movePath.getEntity();

        assertTrue(Compute.getMountableUnits(infantry, movePath.getFinalCoords(), 0, movePath.getFinalElevation(),
              getGame()).isEmpty(), "Nothing should be mountable without a suitable bay");
    }

    @Test
    @DisplayName("Jump infantry that jumps beside the DropShip cannot then mount (TW p.223)")
    void jumpInfantryCannotMountAfterJumping() {
        Dropship dropShip = placeDropShip(new InfantryBay(2, 1, 1, PlatoonType.JUMP), DROPSHIP_CENTRE);
        MovePath movePath = walkSouth(infantry(EntityMovementMode.INF_JUMP), MoveStepType.START_JUMP,
              MoveStepType.FORWARDS);

        assertEquals(HEX_BESIDE_DROPSHIP, movePath.getFinalCoords(), "The jump should land beside the DropShip");

        movePath.addStep(MoveStepType.MOUNT, dropShip);
        movePath.clipToPossible();

        assertEquals(2, movePath.length(), "The jump stays but the mount step is cut");
    }

    @Test
    @DisplayName("Infantry cannot mount a DropShip standing on a different level (TW p.224)")
    void infantryNeedTheSameLevel() {
        initializeBoard("RAISED_DROPSHIP_BOARD", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 1 "" ""
              hex 0103 1 "" ""
              hex 0104 1 "" ""
              hex 0105 1 "" ""
              end""");
        setBoard("RAISED_DROPSHIP_BOARD");
        addPlayer();
        Dropship dropShip = createDropShip(new InfantryBay(2, 1, 1, PlatoonType.FOOT), DROPSHIP_CENTRE_BESIDE_START);
        Entity infantry = infantry(EntityMovementMode.INF_LEG);
        infantry.setOwner(getGame().getPlayer(0));
        infantry.setDeployed(true);
        MovePath movePath = getMovePathFor(infantry, Integer.MAX_VALUE, null);

        assertFalse(Compute.getMountableUnits(infantry, movePath.getFinalCoords(), 0, movePath.getFinalElevation(),
              getGame()).contains(dropShip), "A one-level difference is too much for infantry");
    }

    // ---- Set-up helpers ----

    private Dropship placeDropShip(Bay bay, Coords centre) {
        setBoard("COLUMN_BOARD");
        addPlayer();
        return createDropShip(bay, centre);
    }

    private void addPlayer() {
        Player player = new Player(0, "Player");
        getGame().addPlayer(player.getId(), player);
    }

    private Dropship createDropShip(Bay bay, Coords centre) {
        Dropship dropShip = new Dropship();
        dropShip.setId(20);
        dropShip.setOwner(getGame().getPlayer(0));
        dropShip.addTransporter(bay);
        getGame().addEntity(dropShip);
        dropShip.setDeployed(true);
        dropShip.setFacing(0);
        dropShip.setAltitude(0);
        dropShip.setPosition(centre);
        return dropShip;
    }

    private void placeFriendlyMek(int id) {
        BipedMek friendlyMek = new BipedMek();
        friendlyMek.setId(id);
        friendlyMek.setWeight(50);
        friendlyMek.setOwner(getGame().getPlayer(0));
        getGame().addEntity(friendlyMek);
        friendlyMek.setDeployed(true);
        friendlyMek.setElevation(0);
        friendlyMek.setPosition(HEX_BESIDE_DROPSHIP);
    }

    private MovePath walkSouth(MountCase mountCase, int hexes) {
        MoveStepType[] steps = new MoveStepType[hexes];
        for (int i = 0; i < hexes; i++) {
            steps[i] = MoveStepType.FORWARDS;
        }
        return walkSouth(mountCase.unit().get(), steps);
    }

    private MovePath walkSouth(Entity movingUnit, int hexes) {
        MoveStepType[] steps = new MoveStepType[hexes];
        for (int i = 0; i < hexes; i++) {
            steps[i] = MoveStepType.FORWARDS;
        }
        return walkSouth(movingUnit, steps);
    }

    private MovePath walkSouth(Entity movingUnit, MoveStepType... steps) {
        movingUnit.setOwner(getGame().getPlayer(0));
        movingUnit.setDeployed(true);
        return getMovePathFor(movingUnit, Integer.MAX_VALUE, null, steps);
    }

    // Every unit is given its id up front, so GameBoardTestCase keeps the movement mode, MP and weight set here

    private static Entity mek(Mek mek) {
        mek.setId(MOVING_UNIT_ID);
        mek.setWeight(50);
        mek.setOriginalWalkMP(4);
        return mek;
    }

    private static Entity landAirMek() {
        LandAirMek landAirMek = new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD, LandAirMek.LAM_STANDARD);
        landAirMek.setConversionMode(LandAirMek.CONV_MODE_MEK);
        return mek(landAirMek);
    }

    private static Entity quadVeeVehicleMode() {
        QuadVee quadVee = new QuadVee();
        // A QuadVee loaded from a unit file has its tracks in slot 5 of each leg, and getCruiseMP reads that slot for
        // track damage. A bare QuadVee leaves it empty, which throws and makes every step illegal, so fill it
        int[] legLocations = { Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_ARM, Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_LEG };
        for (int legLocation : legLocations) {
            quadVee.setCritical(legLocation, 5,
                  new CriticalSlot(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR));
        }
        quadVee.setConversionMode(QuadVee.CONV_MODE_VEHICLE);
        return mek(quadVee);
    }

    private static Entity protoMek() {
        ProtoMek protoMek = new ProtoMek();
        protoMek.setId(MOVING_UNIT_ID);
        protoMek.setWeight(5);
        protoMek.setOriginalWalkMP(4);
        return protoMek;
    }

    private static Entity tank(Tank tank, EntityMovementMode movementMode, double weight) {
        tank.setId(MOVING_UNIT_ID);
        tank.setMovementMode(movementMode);
        tank.setWeight(weight);
        tank.setOriginalWalkMP(4);
        return tank;
    }

    private static Entity infantry(EntityMovementMode movementMode) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setId(MOVING_UNIT_ID);
        infantry.setMovementMode(movementMode);
        // setMovementMode resets Walking MP, so this has to come after it
        infantry.setOriginalWalkMP(4);
        return infantry;
    }

    private static Entity battleArmor() {
        BattleArmor battleArmor = new BattleArmor();
        battleArmor.setId(MOVING_UNIT_ID);
        battleArmor.setMovementMode(EntityMovementMode.INF_LEG);
        battleArmor.setOriginalWalkMP(4);
        return battleArmor;
    }

    private static Bay mekBay() {
        return new MekBay(2, 1, 1);
    }

    private static Bay lightVehicleBay() {
        return new LightVehicleBay(2, 1, 1);
    }

    private static Bay heavyVehicleBay() {
        return new HeavyVehicleBay(2, 1, 1);
    }
}
