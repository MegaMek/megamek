/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.client.Client;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.common.Player;
import megamek.common.SimpleTechLevel;
import megamek.common.Team;
import megamek.common.TechConstants;
import megamek.common.containers.MunitionTree;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TeamLoadOutGeneratorTest {

    static GameOptions mockGameOptions = mock(GameOptions.class);
    static ClientGUI cg = mock(ClientGUI.class);
    static Client client = mock(Client.class);
    static Game game = new Game();

    static Team team = new Team(0);
    static Player player = new Player(0, "Test");
    static AmmoType mockLRM15AmmoType = (AmmoType) EquipmentType.get("IS LRM 15 Ammo");
    static AmmoType mockAC20AmmoType = (AmmoType) EquipmentType.get("ISAC20 Ammo");
    static AmmoType mockAC5AmmoType = (AmmoType) EquipmentType.get("ISAC5 Ammo");
    static AmmoType mockSRM6AmmoType = (AmmoType) EquipmentType.get("IS SRM 6 Ammo");
    static AmmoType mockMML7LRMAmmoType = (AmmoType) EquipmentType.get("ISMML7 LRM Ammo");
    static AmmoType mockMML7SRMAmmoType = (AmmoType) EquipmentType.get("ISMML7 SRM Ammo");

    // Test version; may be loaded with YAML values or set explicitly
    static LinkedHashMap<String, Object> testMap;

    @BeforeAll
    static void setUpAll() {
        // Need equipment initialized
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
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

        team.addPlayer(player);
        game.addPlayer(0, player);

        testMap = new LinkedHashMap<>();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void generateParameters() {
    }

    @Test
    void generateMunitionTree() {
    }

    @Test
    void reconfigureBotTeam() {
    }

    Mek createMek(String chassis, String model, String crewName) {
        Mek mockMek = new BipedMek();
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

    @Test
    void testReconfigureEntityFallbackAmmoType() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        Mek mockMek = createMek("Mauler", "MAL-1K", "Tyson");
        Mounted<?> bin1 = mockMek.addEquipment(mockAC5AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockAC5AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin3 = mockMek.addEquipment(mockAC5AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin4 = mockMek.addEquipment(mockAC5AmmoType, Mek.LOC_LEFT_TORSO);

        // Create a set of imperatives, some of which won't work
        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Mauler", "MAL-1K", "any", "AC/5", "Inferno:Standard:Smoke:Flak");
        HashMap<String, Object> availMap = tlg.generateValidMunitionsForFactionAndEra("IS");
        tlg.reconfigureEntity(mockMek, mt, availMap);

        // First imperative entry is invalid, so bin1 should get second choice
        // (Standard)
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        // Third choice is invalid, so 2nd bin gets 4th choice, Flak
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_FLAK));
        // Now two bins are left over, so they're filled with the _new_ default,
        // Standard (choice #2)
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(Munitions.M_STANDARD));
    }

    @Test
    void testReconfigureEntityMekNoAmmoTypesRequested() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);

        Mek mockMek = createMek("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted<?> bin1 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);
        MunitionTree mt = new MunitionTree();

        // We expect to see no change in loadouts
        HashMap<String, Object> availMap = tlg.generateValidMunitionsForFactionAndEra("IS");
        tlg.reconfigureEntity(mockMek, mt, availMap);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_STANDARD));
    }

    @Test
    void testReconfigureEntityMekOneAmmoType() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);

        Mek mockMek = createMek("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted<?> bin1 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);

        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Dead-Fire");

        HashMap<String, Object> availMap = tlg.generateValidMunitionsForFactionAndEra("IS");
        // We expect that all bins are set to the desired munition type as only one type
        // is provided
        tlg.reconfigureEntity(mockMek, mt, availMap);
        assertFalse(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_DEAD_FIRE));
        assertFalse(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_DEAD_FIRE));

        // Now reset the ammo
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Standard");
        tlg.reconfigureEntity(mockMek, mt, availMap);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertFalse(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_DEAD_FIRE));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertFalse(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_DEAD_FIRE));
    }

    @Test
    void testReconfigureEntityMekARADAmmoType() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);

        Mek mockMek = createMek("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted<?> bin1 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);

        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Anti-Radiation");

        HashMap<String, Object> availMap = tlg.generateValidMunitionsForFactionAndEra("IS");
        // We expect that all bins are set to the desired munition type as only one type
        // is provided
        tlg.reconfigureEntity(mockMek, mt, availMap);
        assertFalse(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_ARAD));
        assertFalse(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_ARAD));

        // Now reset the ammo
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Standard");
        tlg.reconfigureEntity(mockMek, mt, availMap);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertFalse(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_ARAD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertFalse(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_ARAD));
    }

    @Test
    void testReconfigureEntityMekThreeAmmoTypesFourBins() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);

        Mek mockMek = createMek("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted<?> bin1 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin3 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin4 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);

        MunitionTree mt = new MunitionTree();
        // First, set all bins to Smoke
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Smoke");
        HashMap<String, Object> availMap = tlg.generateValidMunitionsForFactionAndEra("IS");
        tlg.reconfigureEntity(mockMek, mt, availMap);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_SMOKE_WARHEAD));

        // Then reset bins with useful ammo
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Standard", "Dead-Fire", "Heat-Seeking");

        // We expect that all bins are set to the desired munition type as only one type
        // is provided
        tlg.reconfigureEntity(mockMek, mt, availMap);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertFalse(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_DEAD_FIRE));
        assertFalse(((AmmoType) bin3.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(Munitions.M_HEAT_SEEKING));
        // The final bin should be reset to the default, in this case "Standard"
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(Munitions.M_STANDARD));
    }

    @Test
    void testReconfigureTwoEntityMeksGenericAndNamed() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);

        Mek mockMek = createMek("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mek mockMek2 = createMek("Catapult", "CPLT-C1", "John Q. Public");
        Mounted<?> bin1 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin3 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin4 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin5 = mockMek2.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin6 = mockMek2.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin7 = mockMek2.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin8 = mockMek2.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);

        // Set up two loadouts: one for a named pilot, and one for all LRMs on any
        // Catapults
        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Catapult",
              "CPLT-C1",
              "J. Robert Hoppenheimer",
              "LRM-15",
              "Standard",
              "Dead-Fire",
              "Heat-Seeking",
              "Smoke");
        mt.insertImperative("Catapult", "any", "any", "LRM", "Standard", "Swarm", "Semi-guided");

        HashMap<String, Object> availMap = tlg.generateValidMunitionsForFactionAndEra("IS");
        // J. Robert H. should get the first load out
        tlg.reconfigureEntity(mockMek, mt, availMap);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_DEAD_FIRE));
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(Munitions.M_HEAT_SEEKING));
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(Munitions.M_SMOKE_WARHEAD));

        // John Q. should get the generalized load out; last bin should be set to
        // Standard
        tlg.reconfigureEntity(mockMek2, mt, availMap);
        assertTrue(((AmmoType) bin5.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin6.getType()).getMunitionType().contains(Munitions.M_SWARM));
        assertTrue(((AmmoType) bin7.getType()).getMunitionType().contains(Munitions.M_SEMIGUIDED));
        assertTrue(((AmmoType) bin8.getType()).getMunitionType().contains(Munitions.M_STANDARD));
    }

    @Test
    void testReconfigureTeamOfMeks() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        ReconfigurationParameters rp = new ReconfigurationParameters();
        Mek mockMek = createMek("Hunchback", "HBK-4G", "Boomstick");
        Mek mockMek2 = createMek("Hunchback", "HBK-4J", "The Shade");
        Mek mockMek3 = createMek("Kintaro", "KTO-18", "Dragonpunch");
        mockMek.setOwner(player);
        mockMek2.setOwner(player);
        mockMek3.setOwner(player);
        game.setEntity(0, mockMek);
        game.setEntity(1, mockMek2);
        game.setEntity(2, mockMek3);

        // Load ammo in `Meks; locations are for fun
        Mounted<?> bin1 = mockMek.addEquipment(mockAC20AmmoType, Mek.LOC_CENTER_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockAC20AmmoType, Mek.LOC_CENTER_TORSO);
        Mounted<?> bin3 = mockMek2.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin4 = mockMek2.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin5 = mockMek3.addEquipment(mockSRM6AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin6 = mockMek3.addEquipment(mockSRM6AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin7 = mockMek3.addEquipment(mockSRM6AmmoType, Mek.LOC_CENTER_TORSO);

        MunitionTree mt = new MunitionTree();
        HashMap<String, String> imperatives = new HashMap<>();

        // HBK imperatives; both can be inserted at once
        imperatives.put("AC 20", "Caseless");
        imperatives.put("LRM", "Dead-Fire:Standard");
        mt.insertImperatives("Hunchback", "any", "any", imperatives);

        // Kintaro's go under different keys
        mt.insertImperative("Kintaro", "KTO-18", "any", "SRM", "Inferno:Standard");

        tlg.reconfigureEntities(game.getPlayerEntities(player, false), "FS", mt, rp, null);

        // Check loadouts
        // 1. AC20 HBK should have two tons of Caseless
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(Munitions.M_CASELESS));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(Munitions.M_CASELESS));

        // 2. LRM HBK should have one each of Dead-Fire and Standard
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(Munitions.M_DEAD_FIRE));
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(Munitions.M_STANDARD));

        // 3. LRM HBK should have two Infernos and one Standard
        assertTrue(((AmmoType) bin5.getType()).getMunitionType().contains(Munitions.M_INFERNO));
        assertTrue(((AmmoType) bin6.getType()).getMunitionType().contains(Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin7.getType()).getMunitionType().contains(Munitions.M_INFERNO));
    }

    @Test
    void testRandomReconfigureBotTeam() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        Mek mockMek = createMek("Hunchback", "HBK-4G", "Boomstick");
        Mek mockMek2 = createMek("Hunchback", "HBK-4J", "The Shade");
        Mek mockMek3 = createMek("Kintaro", "KTO-18", "Dragonpunch");
        mockMek.setOwner(player);
        mockMek2.setOwner(player);
        mockMek3.setOwner(player);
        game.setEntity(0, mockMek);
        game.setEntity(1, mockMek2);
        game.setEntity(2, mockMek3);

        // Load ammo in `Meks; locations are for fun
        Mounted<?> bin1 = mockMek.addEquipment(mockAC20AmmoType, Mek.LOC_CENTER_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockAC20AmmoType, Mek.LOC_CENTER_TORSO);
        Mounted<?> bin3 = mockMek2.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin4 = mockMek2.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin5 = mockMek3.addEquipment(mockSRM6AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin6 = mockMek3.addEquipment(mockSRM6AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin7 = mockMek3.addEquipment(mockSRM6AmmoType, Mek.LOC_CENTER_TORSO);

        // Just check that the bins are populated still
        tlg.randomizeBotTeamConfiguration(team, "FWL");

        for (Mounted<?> bin : List.of(bin1, bin2, bin3, bin4, bin5, bin6, bin7)) {
            assertNotEquals("", ((AmmoType) bin.getType()).getSubMunitionName());
        }
    }

    @Test
    void testLoadEntityListTwoEntities() {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        Mek mockMek = createMek("Hunchback", "HBK-4G", "Boomstick");
        Mek mockMek2 = createMek("Hunchback", "HBK-4J", "The Shade");
        Mek mockMek3 = createMek("Kintaro", "KTO-18", "Dragonpunch");
        mockMek.setOwner(player);
        mockMek2.setOwner(player);
        mockMek3.setOwner(player);
        game.setEntity(0, mockMek);
        game.setEntity(1, mockMek2);
        game.setEntity(2, mockMek3);

        MunitionTree original = new MunitionTree();
        original.loadEntityList(game.getPlayerEntities(player, false));
        tlg.randomizeBotTeamConfiguration(team, "CCY");
    }

    @Test
    void testReconfigureBotTeamNoEnemyInfo() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        Mek mockMek = createMek("Hunchback", "HBK-4G", "Boomstick");
        Mek mockMek2 = createMek("Hunchback", "HBK-4J", "The Shade");
        Mek mockMek3 = createMek("Kintaro", "KTO-18", "Dragonpunch");
        mockMek.setOwner(player);
        mockMek2.setOwner(player);
        mockMek3.setOwner(player);
        game.setEntity(0, mockMek);
        game.setEntity(1, mockMek2);
        game.setEntity(2, mockMek3);

        // Load ammo in `Meks; locations are for fun
        Mounted<?> bin1 = mockMek.addEquipment(mockAC20AmmoType, Mek.LOC_CENTER_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockAC20AmmoType, Mek.LOC_CENTER_TORSO);
        Mounted<?> bin3 = mockMek2.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin4 = mockMek2.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin5 = mockMek3.addEquipment(mockSRM6AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin6 = mockMek3.addEquipment(mockSRM6AmmoType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> bin7 = mockMek3.addEquipment(mockSRM6AmmoType, Mek.LOC_CENTER_TORSO);

        // Just check that the bins are populated still
        tlg.reconfigureTeam(team, "CL", "");

        for (Mounted<?> bin : List.of(bin1, bin2, bin3, bin4, bin5, bin6, bin7)) {
            assertNotEquals("", ((AmmoType) bin.getType()).getSubMunitionName());
        }
    }

    @Test
    void testReconfigureBotTeamAllArtemis() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        Mek mockMek = createMek("Warhammer", "WHM-6Rb", "Asgard");
        mockMek.addEquipment(EquipmentType.get("IS Artemis IV FCS"), Mek.LOC_RIGHT_TORSO);
        Mek mockMek2 = createMek("Valkyrie", "VLK-QW5", "Wobbles");
        mockMek2.addEquipment(EquipmentType.get("Clan Artemis IV FCS"), Mek.LOC_RIGHT_TORSO);
        Mek mockMek3 = createMek("Cougar", "XR", "Sarandon");
        mockMek3.addEquipment(EquipmentType.get("Clan Artemis V"), Mek.LOC_RIGHT_TORSO);
        mockMek.setOwner(player);
        mockMek2.setOwner(player);
        mockMek3.setOwner(player);
        game.setEntity(0, mockMek);
        game.setEntity(1, mockMek2);
        game.setEntity(2, mockMek3);

        // Load ammo in `Meks; locations are for fun
        Mounted<?> bin1 = mockMek.addEquipment(mockSRM6AmmoType, Mek.LOC_CENTER_TORSO);
        Mounted<?> bin2 = mockMek2.addEquipment(mockMML7LRMAmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin3 = mockMek2.addEquipment(mockMML7SRMAmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin4 = mockMek3.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin5 = mockMek3.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);

        // Just check that the bins are populated still
        tlg.reconfigureTeam(team, "IS", "");

        for (Mounted<?> bin : List.of(bin1, bin2, bin3, bin4, bin5)) {
            assertNotEquals("Standard", ((AmmoType) bin.getType()).getSubMunitionName());
            assertTrue(((AmmoType) bin.getType()).getSubMunitionName().contains("Artemis"));
        }
    }

    // Section: legalityCheck tests
    @Test
    void testAmmoTypeIllegalByTechLevel() {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        AmmoType aType = (AmmoType) EquipmentType.get("IS Arrow IV Ammo");
        AmmoType mType = AmmoType.getMunitionsFor(aType.getAmmoType())
              .stream()
              .filter(m -> m.getSubMunitionName().contains("ADA"))
              .findFirst()
              .orElse(null);
        // Set game tech level to Standard and update generator
        when(mockGameOptions.stringOption(OptionsConstants.ALLOWED_TECH_LEVEL)).thenReturn("Standard");
        tlg.updateOptionValues();

        // Should not be available to anyone
        Assertions.assertFalse(tlg.checkLegality(mType, "CC", "IS", false));
        Assertions.assertFalse(tlg.checkLegality(mType, "FS", "IS", false));
        Assertions.assertFalse(tlg.checkLegality(mType, "IS", "IS", false));
        Assertions.assertFalse(tlg.checkLegality(mType, "CLAN", "CL", false));
        Assertions.assertFalse(tlg.checkLegality(mType, "CLAN", "CL", true));

        // Should be available to everyone
        when(mockGameOptions.stringOption(OptionsConstants.ALLOWED_TECH_LEVEL)).thenReturn("Advanced");
        tlg.updateOptionValues();
        Assertions.assertTrue(tlg.checkLegality(mType, "CC", "IS", false));
        Assertions.assertTrue(tlg.checkLegality(mType, "FS", "IS", false));
        Assertions.assertTrue(tlg.checkLegality(mType, "IS", "IS", false));
        Assertions.assertTrue(tlg.checkLegality(mType, "CLAN", "CL", true));
        Assertions.assertTrue(tlg.checkLegality(mType, "CLAN", "CL", true));
    }

    @Test
    void testAmmoTypeIllegalBeforeCreation() {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        AmmoType aType = (AmmoType) EquipmentType.get("IS Arrow IV Ammo");
        AmmoType mType = AmmoType.getMunitionsFor(aType.getAmmoType())
              .stream()
              .filter(m -> m.getSubMunitionName().contains("ADA"))
              .findFirst()
              .orElse(null);
        // Should be available by default in 3151, including to Clans (using MixTech)
        Assertions.assertTrue(tlg.checkLegality(mType, "CC", "IS", false));
        Assertions.assertTrue(tlg.checkLegality(mType, "FS", "IS", false));
        Assertions.assertTrue(tlg.checkLegality(mType, "IS", "IS", false));
        // Check mixed-tech and regular Clan tech, which should match IS at this point
        Assertions.assertTrue(tlg.checkLegality(mType, "CLAN", "CL", true));
        Assertions.assertFalse(tlg.checkLegality(mType, "CLAN", "CL", false));

        // Set year back to 3025
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(3025);
        tlg.updateOptionValues();
        Assertions.assertFalse(tlg.checkLegality(mType, "CC", "IS", false));
        Assertions.assertFalse(tlg.checkLegality(mType, "FS", "IS", false));
        Assertions.assertFalse(tlg.checkLegality(mType, "IS", "IS", false));
        Assertions.assertFalse(tlg.checkLegality(mType, "CLAN", "CL", true));

        // Move up to 3070. Because of game settings and lack of "Common" year, ADA
        // becomes available
        // everywhere (at least in the IS) immediately after its inception.
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(3070);
        tlg.updateOptionValues();
        Assertions.assertTrue(tlg.checkLegality(mType, "CC", "IS", false));
        Assertions.assertTrue(tlg.checkLegality(mType, "FS", "IS", false));
        Assertions.assertTrue(tlg.checkLegality(mType, "IS", "IS", false));
        Assertions.assertTrue(tlg.checkLegality(mType, "CLAN", "CL", true));
    }

    @Test
    void testMunitionWeightCollectionTopN() {
        MunitionWeightCollection mwc = new MunitionWeightCollection();
        // Default weighting for all munition types.
        // For missiles, "Dead-Fire" is first, followed by "Standard" by default.
        // For other rounds, "Standard" should be first.
        HashMap<String, List<String>> topN = mwc.getTopN(3);

        assertTrue(topN.get("LRM").get(0).contains("Dead-Fire"));
        assertTrue(topN.get("LRM").get(1).contains("Standard"));
        assertTrue(topN.get("SRM").get(0).contains("Dead-Fire"));
        assertTrue(topN.get("SRM").get(1).contains("Standard"));

        assertTrue(topN.get("AC").get(0).contains("Standard"));
        assertTrue(topN.get("Arrow IV").get(0).contains("Standard"));
    }

    @Test
    void testAPMunitionWeightCollectionCutoff() {
        MunitionWeightCollection mwc = new MunitionWeightCollection();
        // Assume we're up against reflective and heavy targets, not fliers
        mwc.increaseAPMunitions();
        mwc.decreaseFlakMunitions();

        HashMap<String, List<String>> cutoff = mwc.getAboveCutoff(0.0);
        assertEquals("Armor-Piercing=3.0", cutoff.get("AC").get(0));
        assertEquals("Standard=2.0", cutoff.get("AC").get(1));
        assertEquals("Caseless=1.0", cutoff.get("AC").get(2));

        assertEquals("Tandem-Charge=3.0", cutoff.get("SRM").get(0));
        assertEquals("Dead-Fire=3.0", cutoff.get("SRM").get(1));
        assertEquals("Standard=2.0", cutoff.get("SRM").get(2));
    }

    @Test
    void testIncreaseAntiTSMWeightOnly() {
        MunitionWeightCollection mwc = new MunitionWeightCollection();
        ArrayList<String> tsmOnly = new ArrayList<>(List.of("Anti-TSM"));
        mwc.increaseMunitions(tsmOnly);
        mwc.increaseMunitions(tsmOnly);
        mwc.increaseMunitions(tsmOnly);
        assertEquals(7.0, mwc.getSrmWeights().get("Anti-TSM"));
        assertEquals("Anti-TSM=7.0", mwc.getAboveCutoff(6.0).get("SRM").get(0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testArtemisDefaultIsNegative(boolean clan) {
        // We don't want to assign Artemis ammo based on weighting, we only want to
        // consider if the launcher is suitable (within TeamLoadOutGenerator)
        // Test for both IS and Clan
        MunitionWeightCollection mwc = new MunitionWeightCollection("FakeFaction", clan);
        assertTrue(mwc.getLrmWeights().containsKey("Artemis-capable"));
        double weight = mwc.getLrmWeights().get("Artemis-capable");
        assertTrue(weight < 0);
    }

    @Test
    void testFedSunsAntiTSMWeightNotZero() {
        // This does not check for era or tech, only default weights
        MunitionWeightCollection mwc = new MunitionWeightCollection("FS", false);
        assertTrue(mwc.getLrmWeights().containsKey("Anti-TSM"));
        double weight = mwc.getLrmWeights().get("Anti-TSM");
        assertTrue(weight > 0);

        // Compare to Lyran Commonwealth weight, which should be 0
        mwc = new MunitionWeightCollection("LC", false);
        assertTrue(mwc.getLrmWeights().containsKey("Anti-TSM"));
        weight = mwc.getLrmWeights().get("Anti-TSM");
        assertTrue(weight <= 0);

        // Compare to Jade Falcon default weight which should be 0
        mwc = new MunitionWeightCollection("JF", true);
        assertTrue(mwc.getLrmWeights().containsKey("Anti-TSM"));
        weight = mwc.getLrmWeights().get("Anti-TSM");
        assertTrue(weight <= 0);
    }

    @Test
    void testUpdatedDeadFireOverride() throws Exception {
        // Insert a new Overrides.Munitions.Dead-Fire entry that applies to all factions equally.
        // We know we can cast this because every level of the map is either a ref to a HashMap,
        // or a value.
        HashMap<String, Object> overrideMap = (HashMap<String, Object>) TeamLoadOutGenerator.searchMap(
              "Overrides");
        HashMap<String, Object> newBranch = new HashMap<>();
        newBranch.put("Dead-Fire", 4.0);
        overrideMap.put("Munitions", newBranch);

        // Now search through the complete tree to get the updated value
        double dfOverride = (double) TeamLoadOutGenerator.searchMap("Overrides.Munitions.Dead-Fire");
        double defaultWeight = (double) TeamLoadOutGenerator.searchMap("Defaults.Munitions.Weight");
        assertTrue(dfOverride > 0);
        assertTrue(defaultWeight == 0.0);
    }

    @Test
    void testNukeToggleDecreasesNukeWeightToZero() {
        ReconfigurationParameters rp = new ReconfigurationParameters();
        rp.nukesBannedForMe = true;
        MunitionWeightCollection mwc = new MunitionWeightCollection();

        // Have the Munition Tree generator use our pre-made mwc so we can see its
        // changes

        Iterator<Entity> entityIterator = game.getTeamEntities(team);
        ArrayList<Entity> ownTeamEntities = new ArrayList<>();
        entityIterator.forEachRemaining(ownTeamEntities::add);
        TeamLoadOutGenerator.generateMunitionTree(rp, ownTeamEntities, "", mwc);

        assertEquals(0.0, mwc.getLongTomWeights().get("Davy Crockett-M"));
        assertEquals(0.0, mwc.getBombWeights().get("AlamoMissile Ammo"));
    }

    @Test
    void testClampAmmoShotsReduceAmmoBinsToZero() throws LocationFullException {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);

        Mek mockMek = createMek("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted<?> bin1 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);

        tlg.clampAmmoShots(mockMek, 0.0f);
        assertEquals(0, bin1.getUsableShotsLeft());
        assertEquals(0, bin2.getUsableShotsLeft());
    }

    @Test
    void testClampAmmoShotsPositiveSmallFloatGivesOneShot() throws LocationFullException {
        // LRM15s carry 8 shots, the clamp function should give 1 shot at 10% / 0.1f
        // ratio
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);

        Mek mockMek = createMek("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted<?> bin1 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);

        tlg.clampAmmoShots(mockMek, 0.1f);
        assertEquals(1, bin1.getUsableShotsLeft());
        assertEquals(1, bin2.getUsableShotsLeft());
    }

    @Test
    void testClampAmmoShotsSetToHalf() throws LocationFullException {
        // LRM15s carry 8 shots, the clamp function should give 4 shot at 40% / 0.5f
        // ratio
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);

        Mek mockMek = createMek("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted<?> bin1 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);

        tlg.clampAmmoShots(mockMek, 0.5f);
        assertEquals(4, bin1.getUsableShotsLeft());
        assertEquals(4, bin2.getUsableShotsLeft());
    }

    @Test
    void testClampAmmoShotsCannotExceedFull() throws LocationFullException {
        // LRM15s carry 8 shots, the clamp function should give 8 shot at 100% or over
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);

        Mek mockMek = createMek("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted<?> bin1 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> bin2 = mockMek.addEquipment(mockLRM15AmmoType, Mek.LOC_RIGHT_TORSO);

        tlg.clampAmmoShots(mockMek, 1.5f);
        assertEquals(8, bin1.getUsableShotsLeft());
        assertEquals(8, bin2.getUsableShotsLeft());
    }

    /**
     * We expect CAP Pirate flights in the 3SW era to mount ordnance only RL-P pods.
     */
    @Test
    void testGenerateExternalOrdnanceCAP3SWEraPirates() {
        // Game setup
        int year = 2875;
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(year);
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        // Bomber info
        int bombUnits = 20;
        boolean airOnly = true;
        boolean isPirate = true;
        int quality = ForceDescriptor.RATING_5;
        String faction = "PIR";
        String techBase = "IS";
        boolean mixedTech = false;
        BombLoadout generatedBombs = tlg.generateExternalOrdnance(bombUnits,
              airOnly,
              isPirate,
              quality,
              year,
              faction,
              techBase,
              mixedTech);
        BombLoadout expected = new BombLoadout();
        expected.put(BombTypeEnum.RLP, bombUnits);
        assertBombLoadoutEquals(expected, generatedBombs);
        if (!expected.equals(generatedBombs)) {
            fail(String.format("Expected %s, but got %s", expected, generatedBombs));
        }
    }

    private void assertBombLoadoutEquals(BombLoadout expected, BombLoadout actual) {
        assertEquals(expected.size(), actual.size(), "BombLoadout sizes don't match");

        for (Map.Entry<BombTypeEnum, Integer> entry : expected.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            int expectedCount = entry.getValue();
            int actualCount = actual.getCount(bombType);

            assertEquals(expectedCount, actualCount,
                  String.format("Bomb count mismatch for %s: expected %d, got %d",
                        bombType.getDisplayName(), expectedCount, actualCount));
        }

        // Check for unexpected bomb types in actual
        for (BombTypeEnum bombType : actual.keySet()) {
            if (!expected.containsKey(bombType)) {
                assertEquals(0, actual.getCount(bombType),
                      String.format("Unexpected bomb type %s with count %d",
                            bombType.getDisplayName(), actual.getCount(bombType)));
            }
        }
    }


    /**
     * We expect CAP Pirate flights in the 3SW era to mount ordnance only RL-P pods.
     */
    @Test
    void testGenerateExternalOrdnanceCAPPostCIEraPirates() {
        // Game setup
        int year = 3075;
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(year);
        BombLoadout generatedBombs = getBombLoadout(year);
        // Should always get some regular rocket launchers
        assertTrue(generatedBombs.getCount(BombTypeEnum.RL) > 0);
        // Should not use RL-Ps when RLs are available
        assertEquals(0, generatedBombs.getCount(BombTypeEnum.RLP));
    }

    private static BombLoadout getBombLoadout(int year) {
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        // Bomber info
        int bombUnits = 20;
        boolean airOnly = true;
        boolean isPirate = true;
        int quality = ForceDescriptor.RATING_5;
        String faction = "PIR";
        String techBase = "IS";
        boolean mixedTech = false;
        return tlg.generateExternalOrdnance(bombUnits,
              airOnly,
              isPirate,
              quality,
              year,
              faction,
              techBase,
              mixedTech);
    }

    /**
     * We expect CAP Pirate flights in the 3SW era to mount ordnance only RL-P pods.
     */
    @Test
    void testGenerateExternalOrdnanceCAP2800Clan() {
        // Game setup
        int year = 2800;
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(year);
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(game);
        // Bomber info
        int bombUnits = 20;
        boolean airOnly = true;
        boolean isPirate = false;
        int quality = ForceDescriptor.RATING_1;
        String faction = "CSJ";
        String techBase = "CL";
        boolean mixedTech = false;
        BombLoadout generatedBombs = tlg.generateExternalOrdnance(bombUnits,
              airOnly,
              isPirate,
              quality,
              year,
              faction,
              techBase,
              mixedTech);
        // Pre-2823, Clan units can take RL-P bombs
        assertTrue(generatedBombs.getCount(BombTypeEnum.RLP) > 0);
    }

    @Test
    void testMapSearchWithKnownGoodDoubleValue() throws Exception {
        testMap = new LinkedHashMap<String, Object>(
              Map.of(
                    "Defaults", Map.of(
                          "Munitions", Map.of(
                            "Dead-Fire", Map.of(
                                  "IS", 2.0)
                          )
                    )
              )
        );
        Object value = TeamLoadOutGenerator.searchMap("Defaults.Munitions.Dead-Fire.IS", testMap);
        assertEquals(2.0, value);
    }

    @Test
    void testMapSearchWithKnownGoodListValue() throws Exception {
        testMap = new LinkedHashMap<String, Object>(
              Map.of(
                    "Prohibited", List.of(
                          "Tandem-Charge",
                          "AlamoMissile Ammo"
                    )
              )
        );
        List<String> prohibitedList = (List<String>) TeamLoadOutGenerator.searchMap("Prohibited", testMap);
        assertEquals(2, prohibitedList.size());
        assertTrue(prohibitedList.contains("Tandem-Charge"));
        assertTrue(prohibitedList.contains("AlamoMissile Ammo"));
    }

    @Test
    void testValidMunitionsGeneratorHasMaxStandardAmmos() {
        // All the "Standard" ammo types should be available in 3151 with all ammo treated as mixed and
        // techLevel set to Advanced.  In practice that means "available bins is set to MAX_INT".
        String faction = "FS";
        int year = 3151;
        int techLevel = TechConstants.T_SIMPLE_ADVANCED;
        SimpleTechLevel legalLevel = SimpleTechLevel.parse(TechConstants.T_SIMPLE_NAMES[techLevel]);
        boolean allowMixed = true;
        boolean eraBased = true;
        boolean showExtinct = false;
        boolean allowNukes = false;

        HashMap<String, Object> availMap = TeamLoadOutGenerator.generateValidMunitionsForFactionAndEra(
              EquipmentType.allTypes(),
              faction,
              year,
              legalLevel,
              allowMixed,
              eraBased,
              showExtinct,
              allowNukes
        );

        for (String weaponName: TeamLoadOutGenerator.TYPE_LIST) {
            HashMap<String, Integer> entries = (HashMap<String, Integer>) availMap.getOrDefault(weaponName, null);
            assertNotEquals(null, entries);
            assertFalse(entries.isEmpty());
            assertTrue(entries.getOrDefault("Standard", 0) == Integer.MAX_VALUE);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"IS", "CC", "CF", "CP", "CS", "DC", "EI", "FC", "FR", "FS", "FW", "LC",
    "MC", "MH", "OA", "TA", "TC", "TH", "RD", "RS", "RA", "RW", "WB", "MERC", "PER",
    "CLAN", "CBR", "CBS", "CCY", "CCC", "CFM", "CGB", "CGS", "CHH", "CIH", "CJF", "CMN", "CNC",
    "CSF", "CSJ", "CSR", "CSV", "CSA", "CWM", "CWF", "CWX", "CWV"})
    void testValidMunitionsGeneratorNoStandardBeforeIntro(String faction) {
        // While some extant ammo types predate even manned spaceflight, the vast majority
        // (even Standard ammo) were created in the 23rd century or later.
        // Confirm that valid munition maps don't include Standard ammo when they shouldn't.
        int year = 2100;
        int techLevel = TechConstants.T_SIMPLE_STANDARD;
        SimpleTechLevel legalLevel = SimpleTechLevel.parse(TechConstants.T_SIMPLE_NAMES[techLevel]);
        boolean allowMixed = false;
        boolean eraBased = true;
        boolean showExtinct = false;
        boolean allowNukes = false;

        HashMap<String, Object> availMap = TeamLoadOutGenerator.generateValidMunitionsForFactionAndEra(
              EquipmentType.allTypes(),
              faction,
              year,
              legalLevel,
              allowMixed,
              eraBased,
              showExtinct,
              allowNukes
        );

        for (String weaponName: TeamLoadOutGenerator.TYPE_LIST) {
            // "Smaller" Artillery existed pre-spaceflight so don't worry about them
            if (List.of("Sniper", "Thumper").contains(weaponName)) {
                continue;
            }
            HashMap<String, Integer> entries = (HashMap<String, Integer>) availMap.getOrDefault(weaponName, null);
            if (entries != null) {
                assertFalse(entries.getOrDefault("Standard", 0) > 0);
            }
        }
    }

    @Test
    void testOverridesAndProhibitionsApplyToWeights() {
        ReconfigurationParameters rp = new ReconfigurationParameters();
        rp.nukesBannedForMe = true;
        MunitionWeightCollection mwc = new MunitionWeightCollection();
        HashMap<String, Object> overrides = new HashMap<String, Object>(
              Map.of(
                  "LRM", Map.of(
                        "Dead-Fire", Map.of(
                              "IS", 5.0)
                  )
              )
        );
        ArrayList<String> prohibited = new ArrayList<>(
              List.of(
                    "Inferno"
              )
        );
        TeamLoadOutGenerator.applyModifiersToWeights(rp, mwc, overrides, prohibited);

        assertEquals(5.0, mwc.getLrmWeights().get("Dead-Fire"));
        assertEquals(0.0, mwc.getSrmWeights().get("Inferno"));
    }
}
