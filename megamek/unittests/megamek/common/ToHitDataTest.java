/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.options.GameOptions;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToHitDataTest {
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
        when(mockGameOptions.stringOption(OptionsConstants.ALLOWED_TECHLEVEL)).thenReturn("Experimental");
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

    @Test
    void adjustSwarmToHitRemoveTargetMovementMod() {
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target1 = createMek("Target", "TGT-2", "Bob");
        Mek target2 = createMek("Target", "TGT-2", "Charlie");

        attacker.setOwnerId(player1.getId());
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setId(1);
        attacker.setDeployed(true);

        target1.setOwnerId(player2.getId());
        target1.setId(2);
        Coords targetCoords1 = new Coords(5, 5);
        target1.setPosition(targetCoords1);
        target1.setDeployed(true);

        target2.setOwnerId(player2.getId());
        target2.setId(3);
        Coords targetCoords2 = new Coords(5, 6);
        target1.setPosition(targetCoords2);
        target1.setDeployed(true);

        game.addEntities(List.of(attacker, target1, target2));

        ToHitData toHitData = new ToHitData();
        int gunnery = 4;
        int amm = 2;
        int rangeMod = 2;

        toHitData.addModifier(gunnery, "Gunnery Skill");
        toHitData.addModifier(amm, "Attacker ran");
        toHitData.append(Compute.getTargetMovementModifier(game, target1.getId()));
        toHitData.addModifier(rangeMod, "Range Mod");

        // Run adjustment
        toHitData.adjustSwarmToHit();

        assertEquals(3, toHitData.getModifiers().size());
        assertEquals(gunnery + amm + rangeMod, toHitData.getValue());

    }
}
