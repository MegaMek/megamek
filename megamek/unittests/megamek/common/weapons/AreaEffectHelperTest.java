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

package megamek.common.weapons;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.*;
import megamek.common.options.GameOptions;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static megamek.common.weapons.AreaEffectHelper.calculateDamageFallOff;
import static megamek.common.weapons.AreaEffectHelper.DamageFalloff;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AreaEffectHelperTest {

    static GameOptions mockGameOptions = mock(GameOptions.class);
    static ClientGUI cg = mock(ClientGUI.class);
    static Client client = mock(Client.class);
    static Game game = new Game();

    static Team team1 = new Team(0);
    static Team team2 = new Team(1);
    static Player player1 = new Player(0, "Test1");
    static Player player2 = new Player(1, "Test2");
    static AmmoType mockLTAmmoType = (AmmoType) EquipmentType.get("ISLongTom Ammo");
    static AmmoType mockSniperAmmoType = (AmmoType) EquipmentType.get("ISSniper Ammo");
    static AmmoType mockBombHEAmmoType = (AmmoType) EquipmentType.get("HEBomb");
    static AmmoType mockBombFAEAmmoType = (AmmoType) EquipmentType.get("FABombSmall Ammo");

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

    @Test
    void testShapeBlastRingR2ArtilleryAttackOnBoardNoAETerrainLevel0() {
        game.setBoard(new Board(16,17));
        Coords centerPoint = new Coords(7,7);
        DamageFalloff falloff = calculateDamageFallOff(mockLTAmmoType, 0, false);
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlastRing(
            centerPoint, falloff, 0, false
        );

        // We expect a disk of 1 + 6 + 12 hexes centered around the centerPoint
        assertEquals(19, shape.size());
        assertTrue(shape.containsKey(Map.entry(0, centerPoint)));

        // Now create the disk minus the center point
        shape = AreaEffectHelper.shapeBlastRing(
            centerPoint, falloff, 0,true
        );
        // We expect a disk of 6 + 12 hexes centered around the centerPoint, but no centerPoint
        assertEquals(18, shape.size());
        assertFalse(shape.containsKey(Map.entry(0, centerPoint)));
    }

    @Test
    void testShapeBlastR2ArtilleryAttackOnBoardNoAETerrainLevel0() {
        game.setBoard(new Board(16,17));
        Coords centerPoint = new Coords(7,7);
        AmmoType ammo = mockLTAmmoType;
        DamageFalloff falloff = calculateDamageFallOff(ammo, 0, false);
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
            ammo, centerPoint, falloff, 0, true, false, false, game, false
        );

        // We expect a column of two levels above the target level, plus a disk of 1 + 6 + 12 hexes at level
        assertEquals(21, shape.size());
    }

    @Test
    void testShapeBlastR1ArtilleryAttackOnBoardNoAETerrainLevel0() {
        game.setBoard(new Board(16,17));
        Coords centerPoint = new Coords(7,7);
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
            mockSniperAmmoType,
            centerPoint, 0, true, false, false, game, false
        );

        // We expect a column of one level above the target level, plus a disk of 1 + 6 hexes at level
        assertEquals(8, shape.size());
    }

    @Test
    void testShapeBlastR1ArtilleryAttackOnBoardNoAETerrainLevel2Flak() {
        game.setBoard(new Board(16,17));
        Coords centerPoint = new Coords(7,7);
        int height = 2;
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
            mockSniperAmmoType,
            centerPoint, height, true, true, false, game, false
        );

        // We expect a column of one level above and below the target level,
        // plus a disk of 1 + 6 hexes at level
        // Ref: TO:AR page 153 'Aerospace Units on Ground Mapsheets'
        assertEquals(9, shape.size());
        assertTrue(shape.containsKey(Map.entry(1, centerPoint)));
        assertTrue(shape.containsKey(Map.entry(3, centerPoint)));
    }

    @Test
    void testShapeBlastR0BombAttackOnBoardAETerrainLevel2() {
        int height = 2;
        int expectedHalfDamage = (int) Math.ceil(mockBombHEAmmoType.getDamagePerShot() / 2.0);
        game.setBoard(new Board(16,17));
        Coords centerPoint = new Coords(7,7);
        Hex hex = new Hex(0);
        hex.addTerrain(new Terrain(Terrains.BUILDING, height, true, 63));
        game.getBoard().setHex(centerPoint, hex);

        // This a non-artillery attack.
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
            mockBombHEAmmoType,
            centerPoint, height, false, false, false, game, false
        );

        // We expect a column of one level above and below the target level,
        // plus a disk of 1 + 6 hexes at level
        assertEquals(3, shape.size());
        assertEquals(expectedHalfDamage, shape.get(Map.entry(1, centerPoint)));
        assertEquals(expectedHalfDamage, shape.get(Map.entry(3, centerPoint)));
    }

    @Test
    void testShapeBlastR2BombAttackOnBoardAETerrainLevel2() {
        int height = 2;
        int expectedHalfDamage = (int) Math.ceil(mockBombFAEAmmoType.getDamagePerShot() / 2.0);
        game.setBoard(new Board(16,17));
        Coords centerPoint = new Coords(7,7);
        Hex hex = new Hex(0);
        hex.addTerrain(new Terrain(Terrains.BUILDING, height, true, 63));
        game.getBoard().setHex(centerPoint, hex);

        // This a non-artillery attack.
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
            mockBombFAEAmmoType,
            centerPoint, height, false, false, false, game, false
        );

        // We expect a column of two levels above and below the target level,
        // plus a disk of 6 half-damage hexes above and below the target level,
        // plus a disk of 1 + 6 + 12 hexes at level
        assertEquals(35, shape.size());
        assertEquals(expectedHalfDamage, shape.get(Map.entry(0, centerPoint)));
        assertEquals(mockBombFAEAmmoType.getDamagePerShot(), shape.get(Map.entry(1, centerPoint)));
        assertEquals(mockBombFAEAmmoType.getDamagePerShot(), shape.get(Map.entry(2, centerPoint)));
        assertEquals(mockBombFAEAmmoType.getDamagePerShot(), shape.get(Map.entry(3, centerPoint)));
        assertEquals(expectedHalfDamage, shape.get(Map.entry(4, centerPoint)));
        for (Coords c: centerPoint.allAtDistance(1)) {
            assertEquals(expectedHalfDamage, shape.get(Map.entry(3, c)));
            assertEquals(expectedHalfDamage, shape.get(Map.entry(1, c)));
        }
        for (Coords c: centerPoint.allAtDistance(2)) {
            assertEquals(5, shape.get(Map.entry(2, c)));
        }
    }


}
