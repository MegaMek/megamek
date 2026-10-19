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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import megamek.common.GameBoardTestCase;
import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.exceptions.LocationFullException;
import megamek.common.moves.MovePath;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;
import megamek.common.units.SupportTank;
import megamek.common.units.VTOL;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Damage to swarming infantry that is shaken loose (issue #9110, TW p.222 as changed by errata v12.0), tested through
 * the {@link MovePathHandler} and {@link TWGameManager} entry points that reach {@link SwarmShakeOffHandler}.
 */
class SwarmShakeOffHandlerTest extends GameBoardTestCase {

    // A full squad of six, so the all-sixes dice always roll a living trooper for a hit location
    private static final int TROOPERS = 6;
    private static final int TROOPER_ARMOR = 10;
    private static final int VEHICLE_ARMOR = 40;
    private static final int CARRIER_ID = 5;
    private static final int SWARMER_ID = 6;

    static {
        initializeBoard("OPEN_ROW", """
              size 1 6
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              hex 0106 0 "" ""
              end""");
    }

    private TWGameManager gameManager;

    /** Dice that always roll high: every d6 is a 6, so every shake-off roll succeeds and 3D6 is 18. */
    private static final class HighDice extends MMRandom {
        @Override
        public int randomInt(int maxValue) {
            return maxValue - 1;
        }

        @Override
        public float randomFloat() {
            return 0.99f;
        }
    }

    @AfterEach
    void restoreDice() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    @BeforeEach
    void setUp() throws IOException {
        Compute.setRNG(new HighDice());
        gameManager = new TWGameManager();
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        if (getGame().getPlayer(0) == null) {
            getGame().addPlayer(0, new Player(0, "Test"));
        }
        setBoard("OPEN_ROW");
    }

    /** A battle armor squad of six with 10 armor per trooper. Not yet in the game. */
    private BattleArmor newBattleArmor() {
        BattleArmor squad = new BattleArmor();
        squad.setChassis("Test");
        squad.setModel("Squad");
        squad.setChassisType(BattleArmor.CHASSIS_TYPE_BIPED);
        squad.setSquadSize(TROOPERS);
        squad.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        squad.setTechLevel(TechConstants.T_CLAN_TW);
        squad.setMovementMode(EntityMovementMode.INF_LEG);
        squad.autoSetInternal();
        for (int trooper = BattleArmor.LOC_TROOPER_1; trooper <= TROOPERS; trooper++) {
            squad.initializeArmor(TROOPER_ARMOR, trooper);
        }
        squad.setCrew(new Crew(CrewType.INFANTRY_CREW));
        squad.setOwner(getGame().getPlayer(0));
        return squad;
    }

    /** Puts the squad in the game, swarming the carrier. */
    private BattleArmor swarm(Entity carrier, BattleArmor squad) {
        squad.setId(SWARMER_ID);
        getGame().addEntity(squad);
        squad.setPosition(carrier.getPosition());
        squad.setDeployed(true);
        squad.setSwarmTargetId(carrier.getId());
        carrier.setSwarmAttackerId(squad.getId());
        return squad;
    }

    private void armVehicle(Entity vehicle) {
        vehicle.autoSetInternal();
        for (int location = 0; location < vehicle.locations(); location++) {
            vehicle.initializeArmor(VEHICLE_ARMOR, location);
        }
        vehicle.setCrew(new Crew(CrewType.CREW));
        vehicle.setOwner(getGame().getPlayer(0));
    }

    private VTOL newVTOL() {
        VTOL vtol = new VTOL();
        vtol.setMovementMode(EntityMovementMode.VTOL);
        vtol.setChassis("Test");
        vtol.setModel("VTOL");
        vtol.setWeight(30.0);
        vtol.setOriginalWalkMP(8);
        armVehicle(vtol);
        return vtol;
    }

    /** A VTOL in the air at the given elevation in hex 0101, not moving this turn. */
    private VTOL vtolInTheAir(int elevation) {
        VTOL vtol = newVTOL();
        vtol.setId(CARRIER_ID);
        getGame().addEntity(vtol);
        vtol.setPosition(new Coords(0, 0));
        vtol.setElevation(elevation);
        vtol.setDeployed(true);
        return vtol;
    }

    private SupportTank newTrackedVehicle() {
        SupportTank tank = new SupportTank();
        tank.setChassis("Test");
        tank.setModel("Tracked");
        tank.setMovementMode(EntityMovementMode.TRACKED);
        tank.setWeight(30.0);
        tank.setOriginalWalkMP(4);
        armVehicle(tank);
        return tank;
    }

    private int[] trooperArmor(BattleArmor squad) {
        int[] armor = new int[TROOPERS];
        for (int trooper = 0; trooper < TROOPERS; trooper++) {
            armor[trooper] = squad.getArmor(BattleArmor.LOC_TROOPER_1 + trooper);
        }
        return armor;
    }

    private static int[] everyTrooper(int armor) {
        int[] expected = new int[TROOPERS];
        Arrays.fill(expected, armor);
        return expected;
    }

    private boolean reported(int messageId) {
        for (Report report : gameManager.getMainPhaseReport()) {
            if (report.messageId == messageId) {
                return true;
            }
        }
        return false;
    }

    private void shakeOff(Entity carrier, MovePath path) {
        assertTrue(path.isMoveLegal(), "the test path should be legal");
        new MovePathHandler(gameManager, carrier, path, null).processMovement();
        assertEquals(Entity.NONE, carrier.getSwarmAttackerId(), "the swarmers are shaken off");
    }

    @Test
    void battleArmorShakenOffAVtolTakeOnePointPerTrooperPerElevation() {
        VTOL vtol = newVTOL();
        MovePath path = getMovePathFor(vtol, 3, EntityMovementMode.VTOL, MoveStepType.SHAKE_OFF_SWARMERS);
        BattleArmor squad = swarm(vtol, newBattleArmor());

        shakeOff(vtol, path);

        assertEquals(3, vtol.getElevation());
        assertArrayEquals(everyTrooper(TROOPER_ARMOR - 3), trooperArmor(squad),
              "each trooper takes 1 point for each of the VTOL's 3 elevations (TW p.222)");
        assertTrue(reported(2141));
    }

    @Test
    void battleArmorShakenOffAGroundVehicleTakeOnePointPerTrooper() {
        SupportTank tank = newTrackedVehicle();
        MovePath path = getMovePathFor(tank, 0, EntityMovementMode.TRACKED, MoveStepType.SHAKE_OFF_SWARMERS);
        BattleArmor squad = swarm(tank, newBattleArmor());

        shakeOff(tank, path);

        assertArrayEquals(everyTrooper(TROOPER_ARMOR - 1), trooperArmor(squad),
              "each trooper takes 1 point (TW p.222 Vehicles)");
    }

    @Test
    void battleArmorShakenOffAJumpingMekTakeOnePointPerTrooperPerJumpMP() throws LocationFullException {
        BipedMek mek = new BipedMek();
        mek.setChassis("Test");
        mek.setModel("Mek");
        mek.setOriginalJumpMP(3);
        EquipmentType jumpJet = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
        for (int jet = 0; jet < 3; jet++) {
            mek.addEquipment(jumpJet, Mek.LOC_CENTER_TORSO);
        }
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeInternal(15, location);
            mek.initializeArmor(VEHICLE_ARMOR, location);
            if (mek.hasRearArmor(location)) {
                mek.initializeRearArmor(VEHICLE_ARMOR, location);
            }
        }
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(getGame().getPlayer(0));
        MovePath path = getMovePathFor(mek, 0, EntityMovementMode.BIPED, MoveStepType.START_JUMP,
              MoveStepType.FORWARDS, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        BattleArmor squad = swarm(mek, newBattleArmor());

        shakeOff(mek, path);

        assertEquals(3, mek.mpUsed, "the Mek used 3 Jump MP");
        assertArrayEquals(everyTrooper(TROOPER_ARMOR - 3), trooperArmor(squad),
              "each trooper takes 1 point for each Jump MP the Mek used (TW p.222)");
    }

    @Test
    void jumpCapableBattleArmorShakenOffAVtolTakeNoDamage() {
        VTOL vtol = newVTOL();
        MovePath path = getMovePathFor(vtol, 3, EntityMovementMode.VTOL, MoveStepType.SHAKE_OFF_SWARMERS);
        BattleArmor jumpSquad = newBattleArmor();
        jumpSquad.setMovementMode(EntityMovementMode.INF_JUMP);
        jumpSquad.setOriginalJumpMP(3);
        BattleArmor squad = swarm(vtol, jumpSquad);

        shakeOff(vtol, path);

        assertArrayEquals(everyTrooper(TROOPER_ARMOR), trooperArmor(squad),
              "swarmers with Jump movement take no damage (TW p.222)");
        assertTrue(reported(2142));
        assertTrue(squad.isDone(), "the squad still cannot move or shoot this turn");
    }

    @Test
    void conventionalInfantryShakenOffAVtolStillTake3D6() {
        VTOL vtol = newVTOL();
        MovePath path = getMovePathFor(vtol, 3, EntityMovementMode.VTOL, MoveStepType.SHAKE_OFF_SWARMERS);
        ConvInfantry platoon = new ConvInfantry();
        platoon.setOwner(getGame().getPlayer(0));
        platoon.setId(SWARMER_ID);
        getGame().addEntity(platoon);
        platoon.setPosition(new Coords(0, 0));
        platoon.setDeployed(true);
        platoon.setSwarmTargetId(vtol.getId());
        vtol.setSwarmAttackerId(platoon.getId());

        shakeOff(vtol, path);

        assertTrue(reported(2140), "conventional infantry take the fixed 3D6");
        assertFalse(reported(2141));
    }

    @Test
    void battleArmorSwarmingAVtolDestroyedInTheAirTakeOnePointPerTrooperPerElevation() {
        VTOL vtol = vtolInTheAir(2);
        BattleArmor squad = swarm(vtol, newBattleArmor());

        gameManager.addReport(gameManager.destroyEntity(vtol, "test"));

        assertEquals(Entity.NONE, squad.getSwarmTargetId());
        assertArrayEquals(everyTrooper(TROOPER_ARMOR - 2), trooperArmor(squad),
              "a VTOL destroyed at elevation 2 shakes its swarmers loose: 2 points per trooper (TW p.222)");
    }

    @Test
    void battleArmorSwarmingAGroundVehicleThatIsDestroyedTakeNoDamage() {
        SupportTank tank = newTrackedVehicle();
        tank.setId(CARRIER_ID);
        getGame().addEntity(tank);
        tank.setPosition(new Coords(0, 0));
        tank.setDeployed(true);
        BattleArmor squad = swarm(tank, newBattleArmor());

        gameManager.addReport(gameManager.destroyEntity(tank, "test"));

        assertEquals(Entity.NONE, squad.getSwarmTargetId());
        assertArrayEquals(everyTrooper(TROOPER_ARMOR), trooperArmor(squad),
              "swarmers of a unit destroyed on the ground take no damage");
    }

    @Test
    void perTrooperDamageFollowsTheVehicleElevation() {
        SupportTank tank = newTrackedVehicle();
        tank.setId(CARRIER_ID + 1);
        getGame().addEntity(tank);
        tank.setPosition(new Coords(0, 0));
        assertEquals(1, SwarmShakeOffHandler.vehicleShakeOffDamagePerTrooper(tank), "ground vehicle: 1 point");

        VTOL vtol = vtolInTheAir(4);
        assertEquals(4, SwarmShakeOffHandler.vehicleShakeOffDamagePerTrooper(vtol), "VTOL at elevation 4: 4 points");
    }
}
