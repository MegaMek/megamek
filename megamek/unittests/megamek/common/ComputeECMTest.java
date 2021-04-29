/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur
 * (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import junit.framework.TestCase;
import megamek.common.options.GameOptions;
import megamek.server.SmokeCloud;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 *
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/3/13 8:48 AM
 */
@RunWith(JUnit4.class)
public class ComputeECMTest {
    
    @Test
    public void testEntityGetECMInfo() {
        // Mock Player
        IPlayer mockPlayer = Mockito.mock(IPlayer.class);
        
        // Mock the board
        Board mockBoard = Mockito.mock(Board.class);
        Mockito.when(mockBoard.inSpace()).thenReturn(false);
        
        // Mock Options
        GameOptions mockOptions = Mockito.mock(GameOptions.class);
        Mockito.when(mockOptions.booleanOption(Mockito.anyString()))
                .thenReturn(false);
        Mockito.when(mockOptions.booleanOption("tacops_eccm")).thenReturn(true);
        
        // Mock the game
        Game mockGame = Mockito.mock(Game.class);
        Mockito.when(mockGame.getBoard()).thenReturn(mockBoard);
        Mockito.when(mockGame.getSmokeCloudList()).thenReturn(
                new ArrayList<SmokeCloud>());
        Mockito.when(mockGame.getOptions()).thenReturn(mockOptions);
        Mockito.when(mockGame.getPlayer(Mockito.anyInt())).thenReturn(mockPlayer);
        
        ECMInfo ecmInfo, eccmInfo, testInfoECM, testInfoECCM;
        File f;
        MechFileParser mfp;
        Entity archer;
        
        try {
            f = new File("data/mechfiles/mechs/3039u/Archer ARC-2R.mtf");
            mfp  = new MechFileParser(f);
            archer = mfp.getEntity();
        } catch (Exception exc){
            TestCase.fail(exc.getMessage());
            return;
        }

        MiscType.initializeTypes();
        EquipmentType eType;

        // Test no ECM Info
        ecmInfo = archer.getECMInfo();
        TestCase.assertEquals(null, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        TestCase.assertEquals(null, eccmInfo);
           
        /*********************************************************************/
        // Add ECM        
        eType = EquipmentType.get("ISGuardianECMSuite");
        try {
            archer.addEquipment(eType, Mech.LOC_RT);
        } catch (LocationFullException e) {
            TestCase.fail(e.getMessage());
        }
        
        Coords pos = new Coords(0,0);
        archer.setPosition(pos);
        archer.setOwner(mockPlayer);
        archer.setGame(mockGame);
        
        testInfoECM = new ECMInfo(6, pos, mockPlayer, 1, 0);
        ecmInfo = archer.getECMInfo();
        TestCase.assertEquals(testInfoECM, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        TestCase.assertEquals(null, eccmInfo);
        
        /*********************************************************************/
        // Change mode from ECM to ECCM
        Mounted ecm = null;
        for (Mounted m : archer.getMisc()) {
            if (m.getType().equals(eType)) {
                ecm = m;
            }
        }
        TestCase.assertNotNull(ecm);
        int rv = ecm.setMode("ECCM");
        TestCase.assertEquals(1, rv);
        // Need to update the round  to make the mode switch happen
        archer.newRound(1);
        
        testInfoECCM = new ECMInfo(6, pos, mockPlayer, 0, 0);
        testInfoECCM.setECCMStrength(1);
        ecmInfo = archer.getECMInfo();
        TestCase.assertEquals(null, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        TestCase.assertEquals(testInfoECCM, eccmInfo);
        
        // Add a second ECM
        try {
            archer.addEquipment(eType, Mech.LOC_RT);
        } catch (LocationFullException e) {
            TestCase.fail(e.getMessage());
        }
        ecmInfo = archer.getECMInfo();
        TestCase.assertEquals(testInfoECM, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        TestCase.assertEquals(testInfoECCM, eccmInfo);
        
        /*********************************************************************/
        // Add an Angel ECM
        eType = EquipmentType.get("ISAngelECMSuite");
        try {
            archer.addEquipment(eType, Mech.LOC_LT);
        } catch (LocationFullException e) {
            TestCase.fail(e.getMessage());
        }
        testInfoECM = new ECMInfo(6, pos, mockPlayer, 0, 1);
        ecmInfo = archer.getECMInfo();
        TestCase.assertEquals(testInfoECM, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        TestCase.assertEquals(testInfoECCM, eccmInfo);
        
        // Add a second Angel ECM (adding a second Angel ECM shouldn't have 
        //  any effect)
        try {
            archer.addEquipment(eType, Mech.LOC_LARM);
        } catch (LocationFullException e) {
            TestCase.fail(e.getMessage());
        }
        ecmInfo = archer.getECMInfo();
        TestCase.assertEquals(testInfoECM, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        TestCase.assertEquals(testInfoECCM, eccmInfo);
        
        archer.setGameOptions();
        ecm = null;
        for (Mounted m : archer.getMisc()) {
            if (m.getType().equals(eType)) {
                ecm = m;
            }
        }
        TestCase.assertNotNull(ecm);
        rv = ecm.setMode("ECM & ECCM");
        TestCase.assertEquals(2, rv);
        // Need to update the round  to make the mode switch happen
        archer.newRound(2);

        ecmInfo = archer.getECMInfo();
        TestCase.assertEquals(testInfoECM, ecmInfo);
        eccmInfo = archer.getECCMInfo();
        TestCase.assertEquals(testInfoECCM, eccmInfo);        
        
    }
    

    /**
     *  Basic tests for ECM on ground maps, includes single enemy single ally
     *  single hex. 
     */
    @Test
    public void testBasicECM() {
        
        // Create a player
        IPlayer mockPlayer =  Mockito.mock(IPlayer.class);
        Mockito.when(mockPlayer.isEnemyOf(mockPlayer)).thenReturn(false);
        Mockito.when(mockPlayer.getName()).thenReturn("MockPlayer");
        
        // Create an enemy player
        IPlayer mockEnemy =  Mockito.mock(IPlayer.class);
        Mockito.when(mockEnemy.isEnemyOf(mockEnemy)).thenReturn(false);
        Mockito.when(mockEnemy.getName()).thenReturn("MockEnemy");
        Mockito.when(mockPlayer.isEnemyOf(mockEnemy)).thenReturn(true);
        Mockito.when(mockEnemy.isEnemyOf(mockPlayer)).thenReturn(true);
        
        // Mock the board
        Board mockBoard = Mockito.mock(Board.class);
        Mockito.when(mockBoard.inSpace()).thenReturn(false);
        
        // Mock Options
        GameOptions mockOptions = Mockito.mock(GameOptions.class);
        Mockito.when(mockOptions.booleanOption(Mockito.anyString()))
                .thenReturn(false);
        Mockito.when(mockOptions.booleanOption("tacops_eccm")).thenReturn(true);
        
        // Mock the game
        Game mockGame = Mockito.mock(Game.class);
        Mockito.when(mockGame.getBoard()).thenReturn(mockBoard);
        Mockito.when(mockGame.getSmokeCloudList()).thenReturn(
                new ArrayList<SmokeCloud>());
        Mockito.when(mockGame.getOptions()).thenReturn(mockOptions);
        
        // Create a list of enemies, owned by the mockEnemy
        Vector<Entity> entitiesVector = createECMEnemy(mockEnemy, mockGame);
        Mockito.when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);

        Coords enemyPos;
        ECMInfo enemyECMInfo;
        
        /*********************************************************************/
        // Same Hex Tests
        
        // Attack Entity ECM Info
        Coords aePos = new Coords(6,6);
        ECMInfo aeNullECM = null;
        ECMInfo aeECM = new ECMInfo(6, aePos, mockPlayer, 1, 0);
        ECMInfo aeAngelECM = new ECMInfo(6, aePos, mockPlayer, 0, 1);
        ECMInfo aeECCM = new ECMInfo(6, aePos, mockPlayer, 0, 0);
        aeECCM.setECCMStrength(1);
        ECMInfo aeAngelECCM = new ECMInfo(6, aePos, mockPlayer, 0, 0);
        aeAngelECCM.setAngelECCMStrength(1);
        
        Entity additionalEnemy = Mockito.mock(Tank.class);
        Mockito.when(additionalEnemy.getOwner()).thenReturn(mockEnemy);
        Mockito.when(additionalEnemy.getECMInfo()).thenReturn(null);
        Mockito.when(additionalEnemy.getGame()).thenReturn(mockGame);
        
        Entity additionalAlly = Mockito.mock(Tank.class);
        Mockito.when(additionalAlly.getOwner()).thenReturn(mockPlayer);
        Mockito.when(additionalAlly.getECMInfo()).thenReturn(null);
        Mockito.when(additionalAlly.getGame()).thenReturn(mockGame);
        
        // Attacking Entity
        Entity ae = Mockito.mock(Mech.class);
        entitiesVector.add(ae);
        Mockito.when(ae.getPosition()).thenReturn(aePos);
        Mockito.when(ae.getGame()).thenReturn(mockGame);
        Mockito.when(ae.isINarcedWith(INarcPod.ECM)).thenReturn(false);
        Mockito.when(ae.getOwner()).thenReturn(mockPlayer);
        Mockito.when(ae.getECMInfo()).thenReturn(aeNullECM);
        
        /*********************************************************************/
        // Basic ECM Test
        //  Enemy has ECM, Player has no ECM
        //  Shoud be affected by ECM, no Angel, no ECCM
        boolean result;
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        
        // Basic ECM for Player
        //  Enemy has ECM, Player has ECM
        //  Shoud be affected by ECM, no Angel, no ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        
        // Basic Angel ECM for Player
        //  Enemy has ECM, Player has Angel ECM
        //  Shoud be affected by ECM, no Angel, no ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeAngelECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        
        // Basic ECCM for Player
        //  Enemy has ECM, Player has ECCM
        //  Shoud not be affected by ECM, no Angel, no ECCM
        Mockito.when(ae.getECCMInfo()).thenReturn(aeECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        
        // Basic Angel ECCM for Player
        //  Enemy has ECM, Player has Angel ECCM
        //  Shoud not affected by ECM, no Angel, yes ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeAngelECM);
        Mockito.when(ae.getECCMInfo()).thenReturn(aeAngelECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        
        // Add some Angel ECM to eliminate the ECCM
        enemyPos = new Coords(4,4);
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 0, 1);
        Mockito.when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        entitiesVector.add(additionalEnemy);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        

        
        
        entitiesVector = createAngelEnemy(mockEnemy, mockGame);
        entitiesVector.add(ae);
        Mockito.when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);
        Mockito.when(ae.getECCMInfo()).thenReturn(null);
        
        /*********************************************************************/
        // Basic Angel ECM Test
        // Enemy has Angel ECM, Player has no EC(C)M 
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);    
        
        // Basic Angel ECM for Player
        //  Enemy has Angel ECM, Player has ECM
        //  Shoud be affected by ECM, Angel, no ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        
        // Basic Angel ECM for Player
        //  Enemy has Angel ECM, Player has Angel ECM
        //  Shoud be affected by ECM, no Angel, no ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeAngelECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        
        // Basic ECCM for Player
        //  Enemy has Angel ECM, Player has ECCM
        //  Shoud be affected by ECM, Angel, no ECCM
        Mockito.when(ae.getECCMInfo()).thenReturn(aeECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        
        // Basic Angel ECCM for Player
        //  Enemy has Angel ECM, Player has Angel ECCM
        //  Shoud not affected by ECM, no Angel, no ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeAngelECM);
        Mockito.when(ae.getECCMInfo()).thenReturn(aeAngelECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        
        // Add in another enemy basic ECM
        enemyPos = new Coords(4,4);
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 1, 0);
        Mockito.when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        entitiesVector.add(additionalEnemy);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        
        // Replace basic ECM with Angel
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 0, 1);
        Mockito.when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        
        // Multiple enemy ECM, one player Angel ECCM
        Mockito.when(ae.getECCMInfo()).thenReturn(aeAngelECCM);
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 1, 0);
        Mockito.when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        entitiesVector = createECMEnemy(mockEnemy, mockGame);
        entitiesVector.add(ae);
        entitiesVector.add(additionalEnemy);
        Mockito.when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);
        result = ComputeECM.isAffectedByECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, aePos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, aePos);
        TestCase.assertEquals(true, result);        

    }
    
    /**
     *  Basic tests for ECM on ground maps, includes single enemy single ally
     *  multiple hexes.
     */
    @Test
    public void testBasicECMMultiHex() {
        
        // Create a player
        IPlayer mockPlayer =  Mockito.mock(IPlayer.class);
        Mockito.when(mockPlayer.isEnemyOf(mockPlayer)).thenReturn(false);
        Mockito.when(mockPlayer.getName()).thenReturn("MockPlayer");
        
        // Create an enemy player
        IPlayer mockEnemy =  Mockito.mock(IPlayer.class);
        Mockito.when(mockEnemy.isEnemyOf(mockEnemy)).thenReturn(false);
        Mockito.when(mockEnemy.getName()).thenReturn("MockEnemy");
        Mockito.when(mockPlayer.isEnemyOf(mockEnemy)).thenReturn(true);
        Mockito.when(mockEnemy.isEnemyOf(mockPlayer)).thenReturn(true);
        
        // Mock the board
        Board mockBoard = Mockito.mock(Board.class);
        Mockito.when(mockBoard.inSpace()).thenReturn(false);
        
        // Mock Options
        GameOptions mockOptions = Mockito.mock(GameOptions.class);
        Mockito.when(mockOptions.booleanOption(Mockito.anyString()))
                .thenReturn(false);
        Mockito.when(mockOptions.booleanOption("tacops_eccm")).thenReturn(true);
        
        // Mock the game
        Game mockGame = Mockito.mock(Game.class);
        Mockito.when(mockGame.getBoard()).thenReturn(mockBoard);
        Mockito.when(mockGame.getSmokeCloudList()).thenReturn(
                new ArrayList<SmokeCloud>());
        Mockito.when(mockGame.getOptions()).thenReturn(mockOptions);
        
        // Create a list of enemies, owned by the mockEnemy
        Vector<Entity> entitiesVector = createECMEnemy(mockEnemy, mockGame);
        Mockito.when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);

        Coords enemyPos;
        ECMInfo enemyECMInfo;
        
        /*********************************************************************/
        // Same Hex Tests
        
        // Attack Entity ECM Info
        Coords aePos = new Coords(2,2);
        ECMInfo aeNullECM = null;
        ECMInfo aeECM = new ECMInfo(6, aePos, mockPlayer, 1, 0);
        ECMInfo aeAngelECM = new ECMInfo(6, aePos, mockPlayer, 0, 1);
        ECMInfo aeECCM = new ECMInfo(6, aePos, mockPlayer, 0, 0);
        aeECCM.setECCMStrength(1);
        ECMInfo aeAngelECCM = new ECMInfo(6, aePos, mockPlayer, 0, 0);
        aeAngelECCM.setAngelECCMStrength(1);
        
        Entity additionalEnemy = Mockito.mock(Tank.class);
        Mockito.when(additionalEnemy.getOwner()).thenReturn(mockEnemy);
        Mockito.when(additionalEnemy.getECMInfo()).thenReturn(null);
        Mockito.when(additionalEnemy.getGame()).thenReturn(mockGame);
        
        Entity additionalAlly = Mockito.mock(Tank.class);
        Mockito.when(additionalAlly.getOwner()).thenReturn(mockPlayer);
        Mockito.when(additionalAlly.getECMInfo()).thenReturn(null);
        Mockito.when(additionalAlly.getGame()).thenReturn(mockGame);
        
        // Attacking Entity
        Entity ae = Mockito.mock(Mech.class);
        entitiesVector.add(ae);
        Mockito.when(ae.getPosition()).thenReturn(aePos);
        Mockito.when(ae.getGame()).thenReturn(mockGame);
        Mockito.when(ae.isINarcedWith(INarcPod.ECM)).thenReturn(false);
        Mockito.when(ae.getOwner()).thenReturn(mockPlayer);
        Mockito.when(ae.getECMInfo()).thenReturn(aeNullECM);
        
        Coords targetPos = new Coords(3,20);
        
        /*********************************************************************/
        // Basic ECM Test
        //  Enemy has ECM, Player has no ECM
        //  Shoud be affected by ECM, no Angel, no ECCM
        boolean result;
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        
        // Basic ECM for Player
        //  Enemy has ECM, Player has ECM
        //  Shoud be affected by ECM, no Angel, no ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        
        // Basic Angel ECM for Player
        //  Enemy has ECM, Player has Angel ECM
        //  Shoud be affected by ECM, no Angel, no ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeAngelECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        
        // Basic ECCM for Player
        //  Enemy has ECM, Player has ECCM
        //  Shoud not be affected by ECM, no Angel, no ECCM
        Mockito.when(ae.getECCMInfo()).thenReturn(aeECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        
        // Basic Angel ECCM for Player
        //  Enemy has ECM, Player has Angel ECCM
        //  Shoud not affected by ECM, no Angel, yes ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeAngelECM);
        Mockito.when(ae.getECCMInfo()).thenReturn(aeAngelECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        
        // Add some Angel ECM to eliminate the ECCM
        enemyPos = new Coords(4,4);
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 0, 1);
        Mockito.when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        entitiesVector.add(additionalEnemy);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        

        
        
        entitiesVector = createAngelEnemy(mockEnemy, mockGame);
        entitiesVector.add(ae);
        Mockito.when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);
        Mockito.when(ae.getECCMInfo()).thenReturn(null);
        
        /*********************************************************************/
        // Basic Angel ECM Test
        // Enemy has Angel ECM, Player has no EC(C)M 
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);    
        
        // Basic Angel ECM for Player
        //  Enemy has Angel ECM, Player has ECM
        //  Shoud be affected by ECM, Angel, no ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        
        // Basic Angel ECM for Player
        //  Enemy has Angel ECM, Player has Angel ECM
        //  Shoud be affected by ECM, no Angel, no ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeAngelECM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        
        // Basic ECCM for Player
        //  Enemy has Angel ECM, Player has ECCM
        //  Shoud be affected by ECM, Angel, no ECCM
        Mockito.when(ae.getECCMInfo()).thenReturn(aeECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        
        // Basic Angel ECCM for Player
        //  Enemy has Angel ECM, Player has Angel ECCM
        //  Shoud not affected by ECM, no Angel, no ECCM
        Mockito.when(ae.getECMInfo()).thenReturn(aeAngelECM);
        Mockito.when(ae.getECCMInfo()).thenReturn(aeAngelECCM);
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        
        // Add in another enemy basic ECM
        enemyPos = new Coords(4,4);
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 1, 0);
        Mockito.when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        entitiesVector.add(additionalEnemy);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);
        
        // Replace basic ECM with Angel
        enemyECMInfo = new ECMInfo(6, enemyPos, mockEnemy, 0, 1);
        Mockito.when(additionalEnemy.getECMInfo()).thenReturn(enemyECMInfo);
        
        result = ComputeECM.isAffectedByECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, targetPos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, targetPos);
        TestCase.assertEquals(false, result);

        // Test whether ECCM range is working properly, on acccount of bug #4577
        // Basic ECCM for Player
        //  Enemy has ECM, Player has ECCM, Enemy ECM outside range of ECCM
        //  Shoud be affected by ECM, no Angel, no ECCM
        entitiesVector = new Vector<Entity>();
        Entity enemy1 = Mockito.mock(Mech.class);
        Coords ecm1Pos = new Coords(14,14);
        ECMInfo ecm1 = new ECMInfo(6, ecm1Pos, mockEnemy, 1, 0);
        Mockito.when(enemy1.getOwner()).thenReturn(mockEnemy);
        Mockito.when(enemy1.getECMInfo()).thenReturn(ecm1);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        entitiesVector.add(ae);
        Mockito.when(mockGame.getEntitiesVector()).thenReturn(entitiesVector);

        aeECCM = new ECMInfo(6, aePos, mockPlayer, 0, 0);
        aeECCM.setECCMStrength(1);
        Mockito.when(ae.getECCMInfo()).thenReturn(aeECCM);
        Mockito.when(ae.getECMInfo()).thenReturn(null);
        result = ComputeECM.isAffectedByECM(ae, aePos, ecm1Pos);
        TestCase.assertEquals(true, result);
        result = ComputeECM.isAffectedByAngelECM(ae, aePos, ecm1Pos);
        TestCase.assertEquals(false, result);
        result = ComputeECM.isAffectedByECCM(ae, aePos, ecm1Pos);
        TestCase.assertEquals(true, result);

    }
    
    /**
     * Creates a single enemy with basic ECM owned by the supplied owner and 
     * returning the supplied game.  Other enemies are created without ECM. 
     * 
     * @param owner
     * @param mockGame
     * @return
     */
    private static Vector<Entity> createECMEnemy(IPlayer owner, 
            IGame mockGame) {
        Vector<Entity> entitiesVector = new Vector<Entity>();
        
        // Add Entity with ECM
        Entity enemy1 = Mockito.mock(Mech.class);
        Coords ecm1Pos = new Coords(5,5);
        ECMInfo ecm1 = new ECMInfo(6, ecm1Pos, owner, 1, 0);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(ecm1);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        // Add Entity with ECM out of range
        enemy1 = Mockito.mock(Mech.class);
        ecm1Pos = new Coords(20,20);
        ecm1 = new ECMInfo(6, ecm1Pos, owner, 1, 0);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(ecm1);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        // Add several non-ECM enemies
        enemy1 = Mockito.mock(Tank.class);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(null);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = Mockito.mock(Aero.class);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(null);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = Mockito.mock(BattleArmor.class);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(null);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = Mockito.mock(Mech.class);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(null);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
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
    private static Vector<Entity> createAngelEnemy(IPlayer owner, 
            IGame mockGame) {
        Vector<Entity> entitiesVector = new Vector<Entity>();
        
        // Attacking Entity
        Entity enemy1 = Mockito.mock(Mech.class);
        Coords ecm1Pos = new Coords(5,5);
        ECMInfo ecm1 = new ECMInfo(6, ecm1Pos, owner, 0, 1);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(ecm1);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        // Add Entity with ECM out of range
        enemy1 = Mockito.mock(Mech.class);
        ecm1Pos = new Coords(20,20);
        ecm1 = new ECMInfo(6, ecm1Pos, owner, 1, 0);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(ecm1);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        // Add several non-ECM enemies
        enemy1 = Mockito.mock(Tank.class);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(null);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = Mockito.mock(Aero.class);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(null);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = Mockito.mock(BattleArmor.class);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(null);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        enemy1 = Mockito.mock(Mech.class);
        Mockito.when(enemy1.getOwner()).thenReturn(owner);
        Mockito.when(enemy1.getECMInfo()).thenReturn(null);
        Mockito.when(enemy1.getGame()).thenReturn(mockGame);
        entitiesVector.add(enemy1);
        
        return entitiesVector;
    }
   

    
}
