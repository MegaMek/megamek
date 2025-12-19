/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.CrewType;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import megamek.common.units.TripodMek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

class ComputeTest {

    static GameOptions mockGameOptions = mock(GameOptions.class);
    static ClientGUI cg = mock(ClientGUI.class);
    static Client client = mock(Client.class);
    static Game game;

    static Team team1 = new Team(0);
    static Team team2 = new Team(1);
    static Player player1 = new Player(0, "Test1");
    static Player player2 = new Player(1, "Test2");
    static WeaponType mockAC5 = (WeaponType) EquipmentType.get("ISAC5");
    static AmmoType mockAC5AmmoType = (AmmoType) EquipmentType.get("ISAC5 Ammo");
    static AmmoType mockLTAmmoType = (AmmoType) EquipmentType.get("ISLongTom Ammo");

    @BeforeAll
    static void setUpAll() {
        // Need equipment initialized
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        when(cg.getClient()).thenReturn(client);
        when(cg.getClient().getGame()).thenReturn(game);
        game.setOptions(mockGameOptions);

        when(mockGameOptions.booleanOption(eq(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL))).thenReturn(false);
        when(mockGameOptions.stringOption(OptionsConstants.ALLOWED_TECH_LEVEL)).thenReturn("Experimental");
        when(mockGameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED)).thenReturn(true);
        when(mockGameOptions.booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT)).thenReturn(false);
        Option mockTrueBoolOpt = mock(Option.class);
        Option mockFalseBoolOpt = mock(Option.class);
        when(mockTrueBoolOpt.booleanValue()).thenReturn(true);
        when(mockFalseBoolOpt.booleanValue()).thenReturn(false);
        when(mockGameOptions.getOption(anyString())).thenReturn(mockTrueBoolOpt);
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(3151);

        team1.addPlayer(player1);
        team2.addPlayer(player2);
        game.addPlayer(0, player1);
        game.addPlayer(1, player2);
    }

    @AfterEach
    void tearDown() {
    }

    Mek createMek(String chassis, String model, String crewName) {
        // Create a real Mek with some mocked fields
        Mek mockMek = new BipedMek();
        mockMek.setGame(game);
        mockMek.setChassis(chassis);
        mockMek.setModel(model);

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        mockMek.setCrew(mockCrew);

        return mockMek;
    }

    Infantry createInfantry(String chassis, String model, String crewName) {
        // Create a real Infantry unit with some mocked fields
        Infantry mockInfantry = new Infantry();
        mockInfantry.setGame(game);
        mockInfantry.setChassis(chassis);
        mockInfantry.setModel(model);

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        mockInfantry.setCrew(mockCrew);

        return mockInfantry;
    }

    AeroSpaceFighter createASF(String chassis, String model, String crewName) {
        // Create a real AeroSpaceFighter unit with some mocked fields
        AeroSpaceFighter mockAeroSpaceFighter = new AeroSpaceFighter();
        mockAeroSpaceFighter.setGame(game);
        mockAeroSpaceFighter.setChassis(chassis);
        mockAeroSpaceFighter.setModel(model);

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        mockAeroSpaceFighter.setCrew(mockCrew);

        return mockAeroSpaceFighter;
    }

    BuildingEntity createBuildingEntity(String chassis, String model, String crewName) {
        // Create a real BuildingEntity unit with some mocked fields
        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.HARDENED, 2);
        buildingEntity.setGame(game);
        buildingEntity.setChassis(chassis);
        buildingEntity.setModel(model);

        buildingEntity.getInternalBuilding().setBuildingHeight(3);
        buildingEntity.getInternalBuilding().addHex(CubeCoords.ZERO, 60, 15, null, false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(-1.0, 0, 1.0), 60, 15, null, false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(1.0, -1.0, 0), 60, 15, null, false);
        buildingEntity.refreshLocations();
        buildingEntity.refreshAdditionalLocations();

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        buildingEntity.setCrew(mockCrew);

        return buildingEntity;
    }

    @Test
    void noPointBlankShotMek2MekNoHiddenSameOwner() {
        // Basic test: can't PBS from non-hidden unit at own teammate
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        target.setOwnerId(player1.getId());
        target.setId(2);

        assertFalse(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void noPointBlankShotMek2MekNoHiddenDifferentOwner() {
        // Basic test: can't PBS when not hidden
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        target.setOwnerId(player2.getId());
        target.setId(2);

        assertFalse(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void noPointBlankShotMek2MekHiddenDifferentOwnerTooFar() {
        // Basic test: Can't PBS except when at exactly 1 distance from ground target
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(0, 2);
        target.setPosition(targetCoords);

        assertFalse(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void canPointBlankShotMek2MekHiddenDifferentOwner() {
        // Basic test: Can PBS an adjacent enemy while hidden
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(0, 1);
        target.setPosition(targetCoords);

        assertTrue(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void canPointBlankShotInf2MekHiddenDifferentOwner() {
        // Basic test: Infantry can PBS an adjacent enemy while hidden
        Infantry attacker = createInfantry("Attacker Guys", "with pewpews", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(0, 1);
        target.setPosition(targetCoords);

        assertTrue(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void noPointBlankShotInf2AeroHiddenDifferentOwner() {
        // Basic test: Basic Infantry can't fire on aircraft at all!
        Infantry attacker = createInfantry("Attacker Guys", "with pewpews", "Alice");
        AeroSpaceFighter target = createASF("Target ASF", "TGA-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(0, 2);
        target.setPosition(targetCoords);
        target.setAltitude(3);

        assertFalse(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void canPointBlankShotAAInf2AeroHiddenDifferentOwner() throws LocationFullException {
        // Basic test: AA-capable Infantry can fire on aircraft directly overhead!
        Infantry attacker = createInfantry("Attacker Guys", "with AA pewpews", "Alice");
        AeroSpaceFighter target = createASF("Target ASF", "TGA-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.addEquipment(mockAC5AmmoType, Infantry.LOC_FIELD_GUNS);
        attacker.addEquipment(mockAC5, Infantry.LOC_FIELD_GUNS);

        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(0, 0);
        target.setPosition(targetCoords);
        target.setAltitude(3);

        assertTrue(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void allEnemiesOutsideBlastLongTomDirectlyOnMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        assertFalse(Compute.allEnemiesOutsideBlast(target, attacker, mockLTAmmoType, true, false, false, game));
    }

    @Test
    void allEnemiesOutsideBlastLongTomAdjacentToMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        HexTarget hTarget = new HexTarget(new Coords(6, 5), HexTarget.TYPE_HEX_ARTILLERY);
        assertFalse(Compute.allEnemiesOutsideBlast(hTarget, attacker, mockLTAmmoType, true, false, false, game));
    }

    @Test
    void allEnemiesOutsideBlastLongTomTwoFromMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        HexTarget hTarget = new HexTarget(new Coords(6, 4), HexTarget.TYPE_HEX_ARTILLERY);
        assertFalse(Compute.allEnemiesOutsideBlast(hTarget, attacker, mockLTAmmoType, true, false, false, game));
    }

    @Test
    void allEnemiesOutsideBlastLongTomThreeFromMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        HexTarget hTarget = new HexTarget(new Coords(8, 6), HexTarget.TYPE_HEX_ARTILLERY);
        assertTrue(Compute.allEnemiesOutsideBlast(hTarget, attacker, mockLTAmmoType, true, false, false, game));
    }

    @Test
    void allEnemiesOutsideBlastLongTomTwoUnderMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setElevation(2);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        HexTarget hTarget = new HexTarget(new Coords(5, 5), HexTarget.TYPE_HEX_ARTILLERY);
        assertFalse(Compute.allEnemiesOutsideBlast(hTarget, attacker, mockLTAmmoType, true, false, false, game));
    }

    @Test
    void allEnemiesOutsideBlastLongTomThreeUnderMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setElevation(3);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        HexTarget hTarget = new HexTarget(new Coords(5, 5), HexTarget.TYPE_HEX_ARTILLERY);
        assertTrue(Compute.allEnemiesOutsideBlast(hTarget, attacker, mockLTAmmoType, true, false, false, game));
    }

    // Section: Accidental Fall From Above damage
    @Test
    void accidentalFallDamage2Levels50Tons() {
        double weight = 50.0;
        int elevation = 2;
        int expectedDamage = (int) ((weight / 10.0) * elevation);

        Mek faller = createMek("Faller", "ATK-1", "Alice");
        faller.setOwnerId(player1.getId());
        faller.setId(1);
        faller.setWeight(weight);

        assertEquals(expectedDamage, Compute.getAccidentalFallFromAboveDamageFor(faller, elevation));
    }

    @Test
    void accidentalFallDamage5Levels90Tons() {
        double weight = 90.0;
        int elevation = 5;
        int expectedDamage = (int) ((weight / 10.0) * elevation);

        Mek faller = createMek("Faller", "ATK-1", "Alice");
        faller.setOwnerId(player1.getId());
        faller.setId(1);
        faller.setWeight(weight);

        assertEquals(expectedDamage, Compute.getAccidentalFallFromAboveDamageFor(faller, elevation));
    }

    @Test
    void accidentalFallDamage4Levels75Tons() {
        double weight = 75.0;
        int elevation = 4;
        int expectedDamage = 32;

        Mek faller = createMek("Faller", "ATK-1", "Alice");
        faller.setOwnerId(player1.getId());
        faller.setId(1);
        faller.setWeight(weight);

        assertEquals(expectedDamage, Compute.getAccidentalFallFromAboveDamageFor(faller, elevation));
    }

    // Section: Tripod Prone Firing Modifiers (TacOps rules)

    TripodMek createTripodMek(String chassis, String model, int cockpitType) {
        TripodMek tripod = new TripodMek(Mek.GYRO_STANDARD, cockpitType);
        tripod.setGame(game);
        tripod.setChassis(chassis);
        tripod.setModel(model);
        tripod.setWeight(75.0);

        // Initialize armor and internal structure for all locations
        tripod.initializeArmor(10, Mek.LOC_HEAD);
        tripod.initializeArmor(30, Mek.LOC_CENTER_TORSO);
        tripod.initializeArmor(20, Mek.LOC_RIGHT_TORSO);
        tripod.initializeArmor(20, Mek.LOC_LEFT_TORSO);
        tripod.initializeArmor(15, Mek.LOC_RIGHT_ARM);
        tripod.initializeArmor(15, Mek.LOC_LEFT_ARM);
        tripod.initializeArmor(20, Mek.LOC_RIGHT_LEG);
        tripod.initializeArmor(20, Mek.LOC_LEFT_LEG);
        tripod.initializeArmor(20, Mek.LOC_CENTER_LEG);

        tripod.setArmor(10, Mek.LOC_CENTER_TORSO, true); // rear armor

        tripod.initializeInternal(3, Mek.LOC_HEAD);
        tripod.initializeInternal(25, Mek.LOC_CENTER_TORSO);
        tripod.initializeInternal(17, Mek.LOC_RIGHT_TORSO);
        tripod.initializeInternal(17, Mek.LOC_LEFT_TORSO);
        tripod.initializeInternal(12, Mek.LOC_RIGHT_ARM);
        tripod.initializeInternal(12, Mek.LOC_LEFT_ARM);
        tripod.initializeInternal(17, Mek.LOC_RIGHT_LEG);
        tripod.initializeInternal(17, Mek.LOC_LEFT_LEG);
        tripod.initializeInternal(17, Mek.LOC_CENTER_LEG);

        return tripod;
    }

    @Test
    void healthyTripodProneFiringModifierPlusOne() throws LocationFullException {
        // Healthy tripod (all 3 legs, no hip damage) receives +1 modifier per TacOps
        TripodMek tripod = createTripodMek("Ares", "ARS-V1", Mek.COCKPIT_STANDARD);
        tripod.setOwnerId(player1.getId());
        tripod.setId(1);
        tripod.setProne(true);

        // Add a weapon to center torso (valid location for prone firing)
        WeaponType mediumLaser = (WeaponType) EquipmentType.get("ISMediumLaser");
        tripod.addEquipment(mediumLaser, Mek.LOC_CENTER_TORSO);
        int weaponId = tripod.getEquipmentNum(tripod.getWeaponList().get(0));

        ToHitData mods = Compute.getProneMods(game, tripod, weaponId);

        assertNotNull(mods, "Mods should not be null for healthy prone tripod");
        assertEquals(1, mods.getValue(), "Healthy tripod should have +1 prone modifier");
    }

    @Test
    void healthyTripodWithDualCockpitAndGunnerProneFiringModifierZero() throws LocationFullException {
        // Healthy tripod with dual cockpit and dedicated gunner receives +0 modifier
        TripodMek tripod = createTripodMek("Ares", "ARS-V1", Mek.COCKPIT_DUAL);
        tripod.setOwnerId(player1.getId());
        tripod.setId(1);
        tripod.setProne(true);

        // Dual cockpit sets up a crew with pilot and gunner positions
        // Verify crew is set up correctly for dedicated gunner
        Crew dualCrew = new Crew(CrewType.DUAL);
        tripod.setCrew(dualCrew);

        // Add a weapon to center torso (valid location for prone firing)
        WeaponType mediumLaser = (WeaponType) EquipmentType.get("ISMediumLaser");
        tripod.addEquipment(mediumLaser, Mek.LOC_CENTER_TORSO);
        int weaponId = tripod.getEquipmentNum(tripod.getWeaponList().get(0));

        ToHitData mods = Compute.getProneMods(game, tripod, weaponId);

        assertNotNull(mods, "Mods should not be null for prone tripod with dual cockpit");
        assertEquals(0, mods.getValue(), "Healthy tripod with dual cockpit and gunner should have +0 prone modifier");
    }

    @Test
    void damagedTripodWithHipCritProneFiringModifierPlusTwo() throws LocationFullException {
        // Damaged tripod (hip crit) reverts to standard +2 modifier
        TripodMek tripod = createTripodMek("Ares", "ARS-V1", Mek.COCKPIT_STANDARD);
        tripod.setOwnerId(player1.getId());
        tripod.setId(1);
        tripod.setProne(true);

        // Damage the hip actuator on center leg (slot 0 is the hip)
        // setDestroyed is required to make the slot non-operational for getGoodCriticalSlots
        CriticalSlot hipSlot = tripod.getCritical(Mek.LOC_CENTER_LEG, 0);
        assertNotNull(hipSlot, "Hip slot should exist");
        hipSlot.setDestroyed(true);

        // Verify hip crit is detected
        assertTrue(tripod.hasHipCrit(), "Tripod should have hip crit");

        // Add a weapon to center torso (valid location for prone firing)
        WeaponType mediumLaser = (WeaponType) EquipmentType.get("ISMediumLaser");
        tripod.addEquipment(mediumLaser, Mek.LOC_CENTER_TORSO);
        int weaponId = tripod.getEquipmentNum(tripod.getWeaponList().get(0));

        ToHitData mods = Compute.getProneMods(game, tripod, weaponId);

        assertNotNull(mods, "Mods should not be null for damaged prone tripod");
        assertEquals(2, mods.getValue(), "Damaged tripod (hip crit) should have +2 prone modifier");
    }

    @Test
    void damagedTripodWithDestroyedLegProneFiringModifierPlusTwo() throws LocationFullException {
        // Damaged tripod (lost leg) reverts to standard +2 modifier
        TripodMek tripod = createTripodMek("Ares", "ARS-V1", Mek.COCKPIT_STANDARD);
        tripod.setOwnerId(player1.getId());
        tripod.setId(1);
        tripod.setProne(true);

        // Destroy the center leg by setting internal to ARMOR_DESTROYED
        // (destroyLocation uses ARMOR_DOOMED which requires applyDamage to transition)
        tripod.setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_CENTER_LEG);

        // Verify leg is destroyed
        assertTrue(tripod.isLocationBad(Mek.LOC_CENTER_LEG), "Center leg should be destroyed");
        assertEquals(1, tripod.countBadLegs(), "Tripod should have 1 bad leg");

        // Add a weapon to center torso (valid location for prone firing)
        WeaponType mediumLaser = (WeaponType) EquipmentType.get("ISMediumLaser");
        tripod.addEquipment(mediumLaser, Mek.LOC_CENTER_TORSO);
        int weaponId = tripod.getEquipmentNum(tripod.getWeaponList().get(0));

        ToHitData mods = Compute.getProneMods(game, tripod, weaponId);

        assertNotNull(mods, "Mods should not be null for damaged prone tripod");
        assertEquals(2, mods.getValue(), "Damaged tripod (lost leg) should have +2 prone modifier");
    }

    @Test
    void tripodProneLegWeaponCannotFire() throws LocationFullException {
        // Leg-mounted weapons cannot fire when prone
        TripodMek tripod = createTripodMek("Ares", "ARS-V1", Mek.COCKPIT_STANDARD);
        tripod.setOwnerId(player1.getId());
        tripod.setId(1);
        tripod.setProne(true);

        // Add a weapon to center leg (invalid location for prone firing)
        WeaponType mediumLaser = (WeaponType) EquipmentType.get("ISMediumLaser");
        tripod.addEquipment(mediumLaser, Mek.LOC_CENTER_LEG);
        int weaponId = tripod.getEquipmentNum(tripod.getWeaponList().get(0));

        ToHitData mods = Compute.getProneMods(game, tripod, weaponId);

        assertNotNull(mods, "Mods should not be null for leg weapon on prone tripod");
        assertEquals(TargetRoll.IMPOSSIBLE,
              mods.getValue(),
              "Leg-mounted weapon should be impossible to fire when prone");
    }

    @Test
    void tripodProneLeftLegWeaponCannotFire() throws LocationFullException {
        // Left leg-mounted weapons cannot fire when prone
        TripodMek tripod = createTripodMek("Ares", "ARS-V1", Mek.COCKPIT_STANDARD);
        tripod.setOwnerId(player1.getId());
        tripod.setId(1);
        tripod.setProne(true);

        // Add a weapon to left leg (invalid location for prone firing)
        WeaponType mediumLaser = (WeaponType) EquipmentType.get("ISMediumLaser");
        tripod.addEquipment(mediumLaser, Mek.LOC_LEFT_LEG);
        int weaponId = tripod.getEquipmentNum(tripod.getWeaponList().get(0));

        ToHitData mods = Compute.getProneMods(game, tripod, weaponId);

        assertNotNull(mods, "Mods should not be null for left leg weapon on prone tripod");
        assertEquals(TargetRoll.IMPOSSIBLE,
              mods.getValue(),
              "Left leg-mounted weapon should be impossible to fire when prone");
    }

    @Test
    void tripodProneRightLegWeaponCannotFire() throws LocationFullException {
        // Right leg-mounted weapons cannot fire when prone
        TripodMek tripod = createTripodMek("Ares", "ARS-V1", Mek.COCKPIT_STANDARD);
        tripod.setOwnerId(player1.getId());
        tripod.setId(1);
        tripod.setProne(true);

        // Add a weapon to right leg (invalid location for prone firing)
        WeaponType mediumLaser = (WeaponType) EquipmentType.get("ISMediumLaser");
        tripod.addEquipment(mediumLaser, Mek.LOC_RIGHT_LEG);
        int weaponId = tripod.getEquipmentNum(tripod.getWeaponList().get(0));

        ToHitData mods = Compute.getProneMods(game, tripod, weaponId);

        assertNotNull(mods, "Mods should not be null for right leg weapon on prone tripod");
        assertEquals(TargetRoll.IMPOSSIBLE,
              mods.getValue(),
              "Right leg-mounted weapon should be impossible to fire when prone");
    }

    /**
     * Tests for {@link Compute#getRangeMods(Game, megamek.common.units.Entity, WeaponMounted, AmmoMounted, megamek.common.units.Targetable)}
     */
    @Nested
    @DisplayName(value = "getRangeMods Tests")
    class ComputeTestGetRangeMods extends GameBoardTestCase {

        private Entity attacker;
        private Targetable target;
        private WeaponMounted smallLaser;
        private static WeaponType smallLaserType;
        private static WeaponType mediumLaserType;

        static {
            initializeBoard("01_BY_05_NO_OBSTRUCTIONS", """
                  size 1 5
                  hex 0101 0 "" ""
                  hex 0102 0 "" ""
                  hex 0103 0 "" ""
                  hex 0104 0 "" ""
                  hex 0105 0 "" ""
                  end"""
            );

            initializeBoard("03_BY_05_CENTER_HILLS", """
                  size 3 5
                  hex 0101 0 "" ""
                  hex 0201 0 "" ""
                  hex 0301 0 "" ""
                  hex 0102 0 "" ""
                  hex 0202 0 "" ""
                  hex 0302 0 "" ""
                  hex 0103 1 "" ""
                  hex 0203 4 "" ""
                  hex 0303 2 "" ""
                  hex 0104 0 "" ""
                  hex 0204 0 "" ""
                  hex 0304 0 "" ""
                  hex 0105 0 "" ""
                  hex 0205 0 "" ""
                  hex 0305 0 "" ""
                  end""");

            // IS Small Laser: Short 0-1, Medium 2, Long 3
            smallLaserType = (WeaponType) EquipmentType.get("ISSmallLaser");

            mediumLaserType = (WeaponType) EquipmentType.get("ISMediumLaser");
        }

        @Nested
        class ComputeTestGetRangeMods_SimpleTestCases {

            @BeforeEach
            void beforeEach () throws LocationFullException {
                setBoard("01_BY_05_NO_OBSTRUCTIONS");

                // Create attacker with IS Small Laser (position will vary per test)
                attacker = createMek("Attacker", "ATK-1", "Alice");
                attacker.setOwnerId(player1.getId());
                attacker.setId(1);

                // Add IS Small Laser (no ammo needed for energy weapons)
                smallLaser = (WeaponMounted) attacker.addEquipment(smallLaserType, Mek.LOC_RIGHT_TORSO);

                // Create target at fixed position (0, 0)
                Entity targetEntity = createMek("Target", "TGT-2", "Bob");
                targetEntity.setOwnerId(player2.getId());
                targetEntity.setId(2);
                targetEntity.setPosition(new Coords(0, 0));

                target = targetEntity;

                game.addEntity(attacker);
                game.addEntity(targetEntity);
            }

            @Test
            @DisplayName(value = "should return 0 modifier at short range (1 hex)")
            void shouldReturn0Modifier_AtShortRange () {
                // Arrange - Small Laser short range is 0-1 hexes
                attacker.setPosition(new Coords(0, 1)); // 1 hex away from target at (0,0)

                // Act
                ToHitData result = Compute.getRangeMods(game, attacker, smallLaser, null, target);

                // Assert
                assertEquals(0, result.getValue(), "Should have 0 range modifier at short range (1 hex)");
            }

            @Test
            @DisplayName(value = "should return +2 modifier at medium range (2 hexes)")
            void shouldReturn2Modifier_AtMediumRange () {
                // Arrange - Small Laser medium range is 2 hexes
                attacker.setPosition(new Coords(0, 2)); // 2 hexes away from target at (0,0)

                // Act
                ToHitData result = Compute.getRangeMods(game, attacker, smallLaser, null, target);

                // Assert
                assertEquals(2, result.getValue(), "Should have +2 range modifier at medium range (2 hexes)");
            }

            @Test
            @DisplayName(value = "should return +4 modifier at long range (4 hexes)")
            void shouldReturn4Modifier_AtLongRange () {
                // Arrange - Small Laser long range is 3 hexes
                attacker.setPosition(new Coords(0, 3)); // 3 hexes away from target at (0,0)

                // Act
                ToHitData result = Compute.getRangeMods(game, attacker, smallLaser, null, target);

                // Assert
                assertEquals(4, result.getValue(), "Should have +4 range modifier at long range (3 hexes)");
            }
        }

        @Nested
        class ComputeTestGetRangeMods_AbstractBuildingEntityTestCases {
            Entity targetEntity;

            @BeforeEach
            void beforeEach() {
                setBoard("03_BY_05_CENTER_HILLS");

                // Create attacker with IS Medium Laser
                attacker = createBuildingEntity("Attacker", "ATK-1", "Alice");
                attacker.setOwnerId(player1.getId());
                attacker.setId(1);
                attacker.setPosition(new Coords(1, 4));

                // Add IS Small Laser (no ammo needed for energy weapons)
                //smallLaser = (WeaponMounted) attacker.addEquipment(smallLaserType, Mek.LOC_RIGHT_TORSO);

                // Create target
                targetEntity = createMek("Target", "TGT-2", "Bob");
                targetEntity.setOwnerId(player2.getId());
                targetEntity.setId(2);
                targetEntity.setPosition(new Coords(1, 0));

                target = targetEntity;


                game.addEntity(attacker);
                game.addEntity(targetEntity);
            }

            @Test
            @Disabled
            void getRangeModsTargetBehind2LevelHill() throws LocationFullException {
                // Arrange
                targetEntity.setPosition(new Coords(1, 2));
                WeaponMounted mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType, 0);
                mediumLaser.setFacing(0);

                // Act
                ToHitData result = Compute.getRangeMods(game, attacker, mediumLaser, null, target);


                // Assert
                assertEquals(4, result.getValue(), "Should have +4 range modifier at long range (3 hexes)");
            }
        }
    }
}
