/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import megamek.common.options.GameOptions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/3/13 8:48 AM
 */
public class ComputeECMTest {
    @BeforeAll
    public static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    public void testEntityGetECMInfo() {
        // Mock Player
        Player mockPlayer = mock(Player.class);
        
        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);
        
        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);
        when(mockOptions.booleanOption("tacops_eccm")).thenReturn(true);
        
        // Mock the game
        Game mockGame = mock(Game.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getSmokeCloudList()).thenReturn(new ArrayList<>());
        when(mockGame.getOptions()).thenReturn(mockOptions);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);

        File f;
        MechFileParser mfp;
        Entity archer;
        
        try {
            f = new File("data/mechfiles/mechs/3039u/Archer ARC-2R.mtf");
            mfp  = new MechFileParser(f);
            archer = mfp.getEntity();
        } catch (Exception exc) {
            fail(exc.getMessage());
            return;
        }

        MiscType.initializeTypes();

        // Test no ECM Info
        ECMInfo ecmInfo = archer.getECMInfo();
        assertNull(ecmInfo);
        ECMInfo eccmInfo = archer.getECCMInfo();
        assertNull(eccmInfo);
           
        // Add ECM
        EquipmentType eType = EquipmentType.get("ISGuardianECMSuite");
        try {
            archer.addEquipment(eType, Mech.LOC_RT);
        } catch (LocationFullException e) {
            fail(e.getMessage());
        }
        
        Coords pos = new Coords(0, 0);
        archer.setPosition(pos);
        archer.setOwner(mockPlayer);
        archer.setGame(mockGame);

        ECMInfo testInfoECM = new ECMInfo(6, pos, mockPlayer, 1, 0);
        ecmInfo = archer.getECMInfo();
        assertEquals(testInfoECM, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        assertNull(eccmInfo);
        
        // Change mode from ECM to ECCM
        Mounted ecm = null;
        for (Mounted m : archer.getMisc()) {
            if (m.getType().equals(eType)) {
                ecm = m;
            }
        }
        assertNotNull(ecm);
        int rv = ecm.setMode("ECCM");
        assertEquals(1, rv);
        // Need to update the round  to make the mode switch happen
        archer.newRound(1);

        ECMInfo testInfoECCM = new ECMInfo(6, pos, mockPlayer, 0, 0);
        testInfoECCM.setECCMStrength(1);
        ecmInfo = archer.getECMInfo();
        assertNull(ecmInfo);
        eccmInfo = archer.getECCMInfo();
        assertEquals(testInfoECCM, eccmInfo);
        
        // Add a second ECM
        try {
            archer.addEquipment(eType, Mech.LOC_RT);
        } catch (LocationFullException e) {
            fail(e.getMessage());
        }
        ecmInfo = archer.getECMInfo();
        assertEquals(testInfoECM, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        assertEquals(testInfoECCM, eccmInfo);
        
        // Add an Angel ECM
        eType = EquipmentType.get("ISAngelECMSuite");
        try {
            archer.addEquipment(eType, Mech.LOC_LT);
        } catch (LocationFullException e) {
            fail(e.getMessage());
        }
        testInfoECM = new ECMInfo(6, pos, mockPlayer, 0, 1);
        ecmInfo = archer.getECMInfo();
        assertEquals(testInfoECM, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        assertEquals(testInfoECCM, eccmInfo);
        
        // Add a second Angel ECM (adding a second Angel ECM shouldn't have 
        //  any effect)
        try {
            archer.addEquipment(eType, Mech.LOC_LARM);
        } catch (LocationFullException e) {
            fail(e.getMessage());
        }
        ecmInfo = archer.getECMInfo();
        assertEquals(testInfoECM, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        assertEquals(testInfoECCM, eccmInfo);
        
        archer.setGameOptions();
        ecm = null;
        for (Mounted m : archer.getMisc()) {
            if (m.getType().equals(eType)) {
                ecm = m;
            }
        }
        assertNotNull(ecm);
        rv = ecm.setMode("ECM & ECCM");
        assertEquals(2, rv);
        // Need to update the round  to make the mode switch happen
        archer.newRound(2);

        ecmInfo = archer.getECMInfo();
        assertEquals(testInfoECM, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        assertEquals(testInfoECCM, eccmInfo);
    }

    /**
     * Basic tests for ECM on ground maps, includes single enemy single ally single hex.
     */
    @Test
    public void testBasicECM() {
        // Create a player
        Player mockPlayer =  mock(Player.class);
        when(mockPlayer.isEnemyOf(mockPlayer)).thenReturn(false);
        when(mockPlayer.getName()).thenReturn("MockPlayer");
        
        // Create an enemy player
        Player mockEnemy =  mock(Player.class);
        when(mockEnemy.isEnemyOf(mockEnemy)).thenReturn(false);
        when(mockEnemy.getName()).thenReturn("MockEnemy");
        when(mockPlayer.isEnemyOf(mockEnemy)).thenReturn(true);
        when(mockEnemy.isEnemyOf(mockPlayer)).thenReturn(true);
        
        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);
        
        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);
        when(mockOptions.booleanOption("tacops_eccm")).thenReturn(true);
        
        // Mock the game
        Game mockGame = mock(Game.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getSmokeCloudList()).thenReturn(new ArrayList<>());
        when(mockGame.getOptions()).thenReturn(mockOptions);
        
        // Create a list of enemies, owned by the mockEnemy
        Vector<Entity> entitiesVector = createECMEnemy(mockEnemy, mockGame);
        when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);

        // Same Hex Tests
        
        // Attack Entity ECM Info
        Coords aePos = new Coords(6, 6);
        ECMInfo aeECM = new ECMInfo(6, aePos, mockPlayer, 1, 0);
        ECMInfo aeAngelECM = new ECMInfo(6, aePos, mockPlayer, 0, 1);
        ECMInfo aeECCM = new ECMInfo(6, aePos, mockPlayer, 0, 0);
        aeECCM.setECCMStrength(1);
        ECMInfo aeAngelECCM = new ECMInfo(6, aePos, mockPlayer, 0, 0);
        aeAngelECCM.setAngelECCMStrength(1);
        
        Entity additionalEnemy = mock(Tank.class);
        when(additionalEnemy.getOwner()).thenReturn(mockEnemy);
        when(additionalEnemy.getECMInfo()).thenReturn(null);
        when(additionalEnemy.getGame()).thenReturn(mockGame);
        
        Entity additionalAlly = mock(Tank.class);
        when(additionalAlly.getOwner()).thenReturn(mockPlayer);
        when(additionalAlly.getECMInfo()).thenReturn(null);
        when(additionalAlly.getGame()).thenReturn(mockGame);
        
        // Attacking Entity
        Entity ae = mock(Mech.class);
        entitiesVector.add(ae);
        when(ae.getPosition()).thenReturn(aePos);
        when(ae.getGame()).thenReturn(mockGame);
        when(ae.isINarcedWith(INarcPod.ECM)).thenReturn(false);
        when(ae.getOwner()).thenReturn(mockPlayer);
        when(ae.getECMInfo()).thenReturn(null);
        
        // Basic ECM Test
        //  Enemy has ECM, Player has no ECM
        //  Should be affected by ECM, no Angel, no ECCM
        boolean result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Basic ECM for Player
        //  Enemy has ECM, Player has ECM
        //  Should be affected by ECM, no Angel, no ECCM
        when(ae.getECMInfo()).thenReturn(aeECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Basic Angel ECM for Player
        //  Enemy has ECM, Player has Angel ECM
        //  Should be affected by ECM, no Angel, no ECCM
        when(ae.getECMInfo()).thenReturn(aeAngelECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Basic ECCM for Player
        //  Enemy has ECM, Player has ECCM
        //  Should not be affected by ECM, no Angel, no ECCM
        when(ae.getECCMInfo()).thenReturn(aeECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Basic Angel ECCM for Player
        //  Enemy has ECM, Player has Angel ECCM
        //  Should not be affected by ECM, no Angel, yes ECCM
        when(ae.getECMInfo()).thenReturn(aeAngelECM);
        when(ae.getECCMInfo()).thenReturn(aeAngelECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertTrue(result);
        
        // Add some Angel ECM to eliminate the ECCM
        Coords enemyPos = new Coords(4, 4);
        ECMInfo enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 0, 1);
        when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        entitiesVector.add(additionalEnemy);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);

        entitiesVector = createAngelEnemy(mockEnemy, mockGame);
        entitiesVector.add(ae);
        when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);
        when(ae.getECCMInfo()).thenReturn(null);
        
        // Basic Angel ECM Test
        // Enemy has Angel ECM, Player has no EC(C)M 
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Basic Angel ECM for Player
        //  Enemy has Angel ECM, Player has ECM
        //  Should be affected by ECM, Angel, no ECCM
        when(ae.getECMInfo()).thenReturn(aeECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Basic Angel ECM for Player
        //  Enemy has Angel ECM, Player has Angel ECM
        //  Should be affected by ECM, no Angel, no ECCM
        when(ae.getECMInfo()).thenReturn(aeAngelECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Basic ECCM for Player
        //  Enemy has Angel ECM, Player has ECCM
        //  Should be affected by ECM, Angel, no ECCM
        when(ae.getECCMInfo()).thenReturn(aeECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Basic Angel ECCM for Player
        //  Enemy has Angel ECM, Player has Angel ECCM
        //  Should not be affected by ECM, no Angel, no ECCM
        when(ae.getECMInfo()).thenReturn(aeAngelECM);
        when(ae.getECCMInfo()).thenReturn(aeAngelECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Add in another enemy basic ECM
        enemyPos = new Coords(4, 4);
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 1, 0);
        when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        entitiesVector.add(additionalEnemy);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Replace basic ECM with Angel
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 0, 1);
        when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertTrue(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertFalse(result);
        
        // Multiple enemy ECM, one player Angel ECCM
        when(ae.getECCMInfo()).thenReturn(aeAngelECCM);
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 1, 0);
        when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        entitiesVector = createECMEnemy(mockEnemy, mockGame);
        entitiesVector.add(ae);
        entitiesVector.add(additionalEnemy);
        when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        assertTrue(result);
    }
    
    /**
     * Basic tests for ECM on ground maps, includes single enemy single ally multiple hexes.
     */
    @Test
    public void testBasicECMMultiHex() {
        // Create a player
        Player mockPlayer =  mock(Player.class);
        when(mockPlayer.isEnemyOf(mockPlayer)).thenReturn(false);
        when(mockPlayer.getName()).thenReturn("MockPlayer");
        
        // Create an enemy player
        Player mockEnemy =  mock(Player.class);
        when(mockEnemy.isEnemyOf(mockEnemy)).thenReturn(false);
        when(mockEnemy.getName()).thenReturn("MockEnemy");
        when(mockPlayer.isEnemyOf(mockEnemy)).thenReturn(true);
        when(mockEnemy.isEnemyOf(mockPlayer)).thenReturn(true);
        
        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);
        
        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);
        when(mockOptions.booleanOption("tacops_eccm")).thenReturn(true);
        
        // Mock the game
        Game mockGame = mock(Game.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getSmokeCloudList()).thenReturn(new ArrayList<>());
        when(mockGame.getOptions()).thenReturn(mockOptions);
        
        // Create a list of enemies, owned by the mockEnemy
        Vector<Entity> entitiesVector = createECMEnemy(mockEnemy, mockGame);
        when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);

        // Same Hex Tests
        
        // Attack Entity ECM Info
        Coords aePos = new Coords(2, 2);
        ECMInfo aeECM = new ECMInfo(6, aePos, mockPlayer, 1, 0);
        ECMInfo aeAngelECM = new ECMInfo(6, aePos, mockPlayer, 0, 1);
        ECMInfo aeECCM = new ECMInfo(6, aePos, mockPlayer, 0, 0);
        aeECCM.setECCMStrength(1);
        ECMInfo aeAngelECCM = new ECMInfo(6, aePos, mockPlayer, 0, 0);
        aeAngelECCM.setAngelECCMStrength(1);
        
        Entity additionalEnemy = mock(Tank.class);
        when(additionalEnemy.getOwner()).thenReturn(mockEnemy);
        when(additionalEnemy.getECMInfo()).thenReturn(null);
        when(additionalEnemy.getGame()).thenReturn(mockGame);
        
        Entity additionalAlly = mock(Tank.class);
        when(additionalAlly.getOwner()).thenReturn(mockPlayer);
        when(additionalAlly.getECMInfo()).thenReturn(null);
        when(additionalAlly.getGame()).thenReturn(mockGame);
        
        // Attacking Entity
        Entity ae = mock(Mech.class);
        entitiesVector.add(ae);
        when(ae.getPosition()).thenReturn(aePos);
        when(ae.getGame()).thenReturn(mockGame);
        when(ae.isINarcedWith(INarcPod.ECM)).thenReturn(false);
        when(ae.getOwner()).thenReturn(mockPlayer);
        when(ae.getECMInfo()).thenReturn(null);
        
        Coords targetPos = new Coords(3, 20);
        
        // Basic ECM Test
        //  Enemy has ECM, Player has no ECM
        //  Should be affected by ECM, no Angel, no ECCM
        boolean result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        // Basic ECM for Player
        //  Enemy has ECM, Player has ECM
        //  Should be affected by ECM, no Angel, no ECCM
        when(ae.getECMInfo()).thenReturn(aeECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        // Basic Angel ECM for Player
        //  Enemy has ECM, Player has Angel ECM
        //  Should be affected by ECM, no Angel, no ECCM
        when(ae.getECMInfo()).thenReturn(aeAngelECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        // Basic ECCM for Player
        //  Enemy has ECM, Player has ECCM
        //  Should not be affected by ECM, no Angel, no ECCM
        when(ae.getECCMInfo()).thenReturn(aeECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        // Basic Angel ECCM for Player
        //  Enemy has ECM, Player has Angel ECCM
        //  Should not be affected by ECM, no Angel, yes ECCM
        when(ae.getECMInfo()).thenReturn(aeAngelECM);
        when(ae.getECCMInfo()).thenReturn(aeAngelECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertTrue(result);
        
        // Add some Angel ECM to eliminate the ECCM
        Coords enemyPos = new Coords(4, 4);
        ECMInfo enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 0, 1);
        when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        entitiesVector.add(additionalEnemy);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        entitiesVector = createAngelEnemy(mockEnemy, mockGame);
        entitiesVector.add(ae);
        when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);
        when(ae.getECCMInfo()).thenReturn(null);
        
        // Basic Angel ECM Test
        // Enemy has Angel ECM, Player has no EC(C)M 
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        // Basic Angel ECM for Player
        //  Enemy has Angel ECM, Player has ECM
        //  Should be affected by ECM, Angel, no ECCM
        when(ae.getECMInfo()).thenReturn(aeECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        // Basic Angel ECM for Player
        //  Enemy has Angel ECM, Player has Angel ECM
        //  Should be affected by ECM, no Angel, no ECCM
        when(ae.getECMInfo()).thenReturn(aeAngelECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        // Basic ECCM for Player
        //  Enemy has Angel ECM, Player has ECCM
        //  Should be affected by ECM, Angel, no ECCM
        when(ae.getECCMInfo()).thenReturn(aeECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        // Basic Angel ECCM for Player
        //  Enemy has Angel ECM, Player has Angel ECCM
        //  Should not be affected by ECM, no Angel, no ECCM
        when(ae.getECMInfo()).thenReturn(aeAngelECM);
        when(ae.getECCMInfo()).thenReturn(aeAngelECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        // Add in another enemy basic ECM
        enemyPos = new Coords(4, 4);
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 1, 0);
        when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        entitiesVector.add(additionalEnemy);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);
        
        // Replace basic ECM with Angel
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 0, 1);
        when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        assertTrue(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        assertFalse(result);

        // Test whether ECCM range is working properly, on account of bug #4577
        // Basic ECCM for Player
        //  Enemy has ECM, Player has ECCM, Enemy ECM outside range of ECCM
        //  Should be affected by ECM, no Angel, no ECCM
        entitiesVector = new Vector<>();
        Entity enemy1 = mock(Mech.class);
        Coords ecm1Pos = new Coords(14, 14);
        ECMInfo ecm1 = new ECMInfo(6, ecm1Pos, mockEnemy, 1, 0);
        when(enemy1.getOwner()).thenReturn(mockEnemy);
        when(enemy1.getECMInfo()).thenReturn(ecm1);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        entitiesVector.add(ae);
        when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);

        aeECCM = new ECMInfo(6, aePos, mockPlayer, 0, 0);
        aeECCM.setECCMStrength(1);
        when(ae.getECCMInfo()).thenReturn(aeECCM);
        when(ae.getECMInfo()).thenReturn(null);
        result = ComputeECM.isAffectedByECM(ae, aePos, ecm1Pos);
        assertTrue(result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, ecm1Pos);
        assertFalse(result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, ecm1Pos);
        assertTrue(result);
    }
    
    /**
     * Creates a single enemy with basic ECM owned by the supplied owner and 
     * returning the supplied game.  Other enemies are created without ECM. 
     * 
     * @param owner
     * @param mockGame
     * @return
     */
    private static Vector<Entity> createECMEnemy(Player owner, Game mockGame) {
        Vector<Entity> entitiesVector = new Vector<>();
        
        // Add Entity with ECM
        Entity enemy1 = mock(Mech.class);
        Coords ecm1Pos = new Coords(5, 5);
        ECMInfo ecm1 = new ECMInfo(6, ecm1Pos, owner, 1, 0);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(ecm1);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        // Add Entity with ECM out of range
        enemy1 = mock(Mech.class);
        ecm1Pos = new Coords(20, 20);
        ecm1 = new ECMInfo(6, ecm1Pos, owner, 1, 0);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(ecm1);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        // Add several non-ECM enemies
        enemy1 = mock(Tank.class);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(null);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = mock(Aero.class);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(null);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = mock(BattleArmor.class);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(null);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = mock(Mech.class);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(null);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        return entitiesVector;
    }
    
    /**
     * Creates a single enemy with Angel ECM owned by the supplied owner and 
     * returning the supplied game. Other enemies are created without ECM.
     * 
     * @param owner
     * @param mockGame
     * @return
     */    
    private static Vector<Entity> createAngelEnemy(Player owner, Game mockGame) {
        Vector<Entity> entitiesVector = new Vector<>();
        
        // Attacking Entity
        Entity enemy1 = mock(Mech.class);
        Coords ecm1Pos = new Coords(5, 5);
        ECMInfo ecm1 = new ECMInfo(6, ecm1Pos, owner, 0, 1);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(ecm1);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        // Add Entity with ECM out of range
        enemy1 = mock(Mech.class);
        ecm1Pos = new Coords(20, 20);
        ecm1 = new ECMInfo(6, ecm1Pos, owner, 1, 0);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(ecm1);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        // Add several non-ECM enemies
        enemy1 = mock(Tank.class);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(null);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = mock(Aero.class);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(null);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = mock(BattleArmor.class);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(null);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = mock(Mech.class);
        when(enemy1.getOwner()).thenReturn(owner);
        when(enemy1.getECMInfo()).thenReturn(null);
        when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        return entitiesVector;
    }
}
