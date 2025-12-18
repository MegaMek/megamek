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

package megamek.common.weapons;

import static megamek.common.weapons.handlers.AreaEffectHelper.calculateDamageFallOff;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.Team;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.weapons.handlers.AreaEffectHelper;
import megamek.common.weapons.handlers.DamageFalloff;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    static AmmoType mockLTClusterAmmoType = (AmmoType) EquipmentType.get("ISLongTom Cluster Ammo");
    static AmmoType mockSniperAmmoType = (AmmoType) EquipmentType.get("ISSniper Ammo");
    static AmmoType mockSniperClusterAmmoType = (AmmoType) EquipmentType.get("ISSniper Cluster Ammo");
    static AmmoType mockThumperClusterAmmoType = (AmmoType) EquipmentType.get("ISThumper Cluster Ammo");
    static AmmoType mockBombHEAmmoType = (AmmoType) EquipmentType.get("HEBomb");
    static AmmoType mockBombClusterAmmoType = (AmmoType) EquipmentType.get("ClusterBomb");
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

    @Test
    void testShapeBlastRingR2ArtilleryAttackOnBoardNoAETerrainLevel0() {
        game.setBoard(new Board(16, 17));
        Coords centerPoint = new Coords(7, 7);
        DamageFalloff falloff = calculateDamageFallOff(mockLTAmmoType, 0, false);
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlastRing(
              centerPoint, falloff, 0, false
        );

        // We expect a disk of 1 + 6 + 12 hexes centered around the centerPoint
        assertEquals(19, shape.size());
        assertTrue(shape.containsKey(Map.entry(0, centerPoint)));

        // Now create the disk minus the center point
        shape = AreaEffectHelper.shapeBlastRing(
              centerPoint, falloff, 0, true
        );
        // We expect a disk of 6 + 12 hexes centered around the centerPoint, but no centerPoint
        assertEquals(18, shape.size());
        assertFalse(shape.containsKey(Map.entry(0, centerPoint)));
    }

    @Test
    void testClusterBombCorrectDamageShape() {
        game.setBoard(new Board(16, 17));
        Coords centerPoint = new Coords(7, 7);

        int height = 0;

        // This a non-artillery attack.
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
              mockBombClusterAmmoType,
              centerPoint, height, false, false, false, game, false
        );

        // Cluster Bomb has R1 and does 5 points of damage across the entire area
        assertEquals(7, shape.size());
        assertEquals(5, shape.get(Map.entry(0, centerPoint)));
        assertEquals(5, shape.get(Map.entry(0, centerPoint.translated(0, 1))));
    }

    @Test
    void testClusterLongTomCorrectDamageShape() {
        game.setBoard(new Board(16, 17));
        Coords centerPoint = new Coords(7, 7);

        // This a Cluster artillery attack.
        AmmoType ammo = mockLTClusterAmmoType;
        DamageFalloff falloff = calculateDamageFallOff(ammo, 0, false);
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
              ammo, centerPoint, falloff, 0, true, false, false, game, false
        );

        // Cluster Long Tom has R1 and does  20/10 damage, plus 10 damage @ 1 level above center
        assertEquals(8, shape.size());
        assertEquals(20, shape.get(Map.entry(0, centerPoint)));
        assertEquals(10, shape.get(Map.entry(1, centerPoint)));
        assertEquals(10, shape.get(Map.entry(0, centerPoint.translated(0, 1))));
    }

    @Test
    void testClusterSniperCorrectDamageShape() {
        game.setBoard(new Board(16, 17));
        Coords centerPoint = new Coords(7, 7);

        // This a Cluster artillery attack.
        AmmoType ammo = mockSniperClusterAmmoType;
        DamageFalloff falloff = calculateDamageFallOff(ammo, 0, false);
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
              ammo, centerPoint, falloff, 0, true, false, false, game, false
        );

        // Cluster Sniper has R1 and does  15/5 damage, plus 5 damage @ 1 level above center
        assertEquals(8, shape.size());
        assertEquals(15, shape.get(Map.entry(0, centerPoint)));
        assertEquals(5, shape.get(Map.entry(1, centerPoint)));
        assertEquals(5, shape.get(Map.entry(0, centerPoint.translated(0, 1))));
    }

    @Test
    void testClusterThumperCorrectDamageShape() {
        game.setBoard(new Board(16, 17));
        Coords centerPoint = new Coords(7, 7);

        // This a Cluster artillery attack.
        AmmoType ammo = mockThumperClusterAmmoType;
        DamageFalloff falloff = calculateDamageFallOff(ammo, 0, false);
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
              ammo, centerPoint, falloff, 0, true, false, false, game, false
        );

        // Cluster Thumper has R1 and does  10/1 damage.  No level 1 damage because vertical falloff
        // is defined as D - 10 / level, not "R1 falloff".
        assertEquals(7, shape.size());
        assertEquals(10, shape.get(Map.entry(0, centerPoint)));
        assertNull(shape.get(Map.entry(1, centerPoint)));
        assertEquals(1, shape.get(Map.entry(0, centerPoint.translated(0, 1))));
    }

    @Test
    void testShapeBlastR2ArtilleryAttackOnBoardNoAETerrainLevel0() {
        game.setBoard(new Board(16, 17));
        Coords centerPoint = new Coords(7, 7);
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
        game.setBoard(new Board(16, 17));
        Coords centerPoint = new Coords(7, 7);
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
              mockSniperAmmoType,
              centerPoint, 0, true, false, false, game, false
        );

        // We expect a column of one level above the target level, plus a disk of 1 + 6 hexes at level
        assertEquals(8, shape.size());
    }

    @Test
    void testShapeBlastR1ArtilleryAttackOnBoardNoAETerrainLevel2Flak() {
        game.setBoard(new Board(16, 17));
        Coords centerPoint = new Coords(7, 7);
        int height = 2;
        HashMap<Map.Entry<Integer, Coords>, Integer> shape = AreaEffectHelper.shapeBlast(
              mockSniperAmmoType,
              centerPoint, height, true, true, false, game, false
        );

        // We expect a column of one level above and below the target level,
        // plus a disk of 1 + 6 hexes at level
        // Ref: TO:AR page 153 'Aerospace Units on Ground Map sheets'
        assertEquals(9, shape.size());
        assertTrue(shape.containsKey(Map.entry(1, centerPoint)));
        assertTrue(shape.containsKey(Map.entry(3, centerPoint)));
    }

    @Test
    void testShapeBlastR0BombAttackOnBoardAETerrainLevel2() {
        int height = 2;
        int expectedHalfDamage = (int) Math.ceil(mockBombHEAmmoType.getDamagePerShot() / 2.0);
        game.setBoard(new Board(16, 17));
        Coords centerPoint = new Coords(7, 7);
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
        game.setBoard(new Board(16, 17));
        Coords centerPoint = new Coords(7, 7);
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
        for (Coords c : centerPoint.allAtDistance(1)) {
            assertEquals(expectedHalfDamage, shape.get(Map.entry(3, c)));
            assertEquals(expectedHalfDamage, shape.get(Map.entry(1, c)));
        }
        for (Coords c : centerPoint.allAtDistance(2)) {
            assertEquals(5, shape.get(Map.entry(2, c)));
        }
    }

    /**
     * Helper to create a mock entity with all the stubbing needed for Report.addDesc()
     */
    private Entity createMockEntityForReports(boolean isHidden) {
        Entity entity = mock(Entity.class);
        when(entity.getId()).thenReturn(1);
        when(entity.isHidden()).thenReturn(isHidden);
        when(entity.isDoomed()).thenReturn(true); // End damage loop immediately
        when(entity.hasUndamagedCriticalSlots()).thenReturn(false);

        // For Report.addDesc()
        when(entity.getShortName()).thenReturn("Test Unit");
        when(entity.getOwner()).thenReturn(null);
        Crew mockCrew = mock(Crew.class);
        when(mockCrew.getSize()).thenReturn(1);
        when(mockCrew.getNickname()).thenReturn("");
        when(entity.getCrew()).thenReturn(mockCrew);

        // For damage application
        HitData mockHit = new HitData(0);
        when(entity.rollHitLocation(anyInt(), anyInt())).thenReturn(mockHit);
        when(entity.sideTable(any(Coords.class))).thenReturn(ToHitData.SIDE_FRONT);

        return entity;
    }

    @Test
    void testExplosionRevealsHiddenUnit() {
        // Setup: Create a mock hidden entity
        Entity hiddenUnit = createMockEntityForReports(true);

        // Mock game manager
        TWGameManager mockGameManager = mock(TWGameManager.class);
        when(mockGameManager.damageEntity(any(), any(), anyInt(), anyBoolean(), any(), anyBoolean(), anyBoolean()))
              .thenReturn(new Vector<>());

        Vector<Report> reports = new Vector<>();
        Coords position = new Coords(5, 5);

        // Act: Apply explosion damage
        AreaEffectHelper.applyExplosionClusterDamageToEntity(
              hiddenUnit, 10, 5, position, reports, mockGameManager);

        // Assert: Reveal report (9963) should be generated
        boolean hasRevealReport = reports.stream().anyMatch(r -> r.messageId == 9963);
        assertTrue(hasRevealReport, "Should generate reveal report (9963) for hidden unit");
    }

    @Test
    void testExplosionDoesNotGenerateRevealReportForVisibleUnit() {
        // Setup: Create a mock non-hidden entity
        Entity visibleUnit = createMockEntityForReports(false);

        // Mock game manager
        TWGameManager mockGameManager = mock(TWGameManager.class);
        when(mockGameManager.damageEntity(any(), any(), anyInt(), anyBoolean(), any(), anyBoolean(), anyBoolean()))
              .thenReturn(new Vector<>());

        Vector<Report> reports = new Vector<>();
        Coords position = new Coords(5, 5);

        // Act: Apply explosion damage
        AreaEffectHelper.applyExplosionClusterDamageToEntity(
              visibleUnit, 10, 5, position, reports, mockGameManager);

        // Assert: No reveal report should be generated for already-visible unit
        boolean hasRevealReport = reports.stream().anyMatch(r -> r.messageId == 9963);
        assertFalse(hasRevealReport, "Should not generate reveal report for visible unit");
    }

}
