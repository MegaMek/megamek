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

package megamek.server.totalwarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import megamek.common.DamageInfo;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.weapons.DamageType;
import megamek.server.Server;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TWDamageManagerTest {

    private final String resourcesPath = "testresources/megamek/common/units/";

    private TWGameManager gameMan = new TWGameManager();
    private TWDamageManager oldMan;
    private TWDamageManagerModular newMan;
    private Game game;
    private Player player;
    private Server server;

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws IOException {
        // Set up game with options
        game = gameMan.getGame();
        GameOptions gOp = new GameOptions();
        game.setOptions(gOp);

        // DamageManagers will throw if uninitialized at use
        oldMan = new TWDamageManager(gameMan, game);
        newMan = new TWDamageManagerModular(gameMan, game);

        // Need servers to handle unit destruction (sad face)
        server = ServerFactory.createServer(gameMan);
        // Player for certain checks and messages
        player = new Player(1, "Test");
        game.addPlayer(1, player);
    }

    Entity loadEntityFromFile(String filename) throws FileNotFoundException {
        File file;
        MekFileParser mfParser;
        Entity e;

        try {
            file = new File(resourcesPath + filename);
            mfParser = new MekFileParser(file);
            e = mfParser.getEntity();
        } catch (Exception ex) {
            fail(ex.getMessage());
            return null;
        }

        return e;
    }

    BattleArmor loadBA(String filename) throws FileNotFoundException {
        BattleArmor battleArmor = (BattleArmor) loadEntityFromFile(filename);
        battleArmor.setId(game.getNextEntityId());
        game.addEntity(battleArmor);
        battleArmor.setOwner(player);
        return battleArmor;
    }

    BipedMek loadMek(String filename) throws FileNotFoundException {
        BipedMek mek = (BipedMek) loadEntityFromFile(filename);
        mek.setId(game.getNextEntityId());
        game.addEntity(mek);
        mek.setOwner(player);
        return mek;
    }

    AeroSpaceFighter loadASF(String filename) throws FileNotFoundException {
        AeroSpaceFighter asf = (AeroSpaceFighter) loadEntityFromFile(filename);
        asf.setId(game.getNextEntityId());
        game.addEntity(asf);
        asf.setOwner(player);
        return asf;
    }

    @Test
    void damageBAComparison() throws FileNotFoundException {
        String unit = "Elemental BA [Laser] (Sqd5).blk";
        BattleArmor mek = loadBA(unit);

        // Validate starting armor
        assertEquals(10, mek.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal damage with original method
        HitData hit = new HitData(BattleArmor.LOC_TROOPER_1);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 5);
        oldMan.damageEntity(damageInfo);
        assertEquals(5, mek.getArmor(BattleArmor.LOC_TROOPER_1));

        // Reset for new damage method
        BattleArmor mek2 = loadBA(unit);

        // Validate starting armor
        assertEquals(10, mek2.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal damage with new method
        hit = new HitData(BattleArmor.LOC_TROOPER_1);
        damageInfo = new DamageInfo(mek2, hit, 5);
        newMan.damageEntity(damageInfo);
        assertEquals(5, mek2.getArmor(BattleArmor.LOC_TROOPER_1));
    }

    @Test
    void killBAComparison() throws FileNotFoundException {
        // We need to show that both old and new damagers kill BAs _dead_.

        String unit = "Elemental BA [Laser] (Sqd5).blk";
        BattleArmor mek = loadBA(unit);
        DamageInfo damageInfo;

        // Validate starting armor
        assertEquals(10, mek.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal damage with original method
        for (int count = 0; count <= 15; count++) {
            HitData hit = mek.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
            damageInfo = new DamageInfo(mek, hit, 5);
            oldMan.damageEntity(damageInfo);
        }

        assertTrue(mek.isDoomed());

        // Reset for new damage method
        BattleArmor mek2 = loadBA(unit);

        // Validate starting armor
        assertEquals(10, mek2.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal damage with new method
        for (int count = 0; count <= 15; count++) {
            HitData hit = mek2.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
            damageInfo = new DamageInfo(mek2, hit, 5);
            newMan.damageEntity(damageInfo);
        }
        assertTrue(mek2.isDoomed());
    }

    @Test
    void destroySectionDamageTransferComparison() throws FileNotFoundException {
        // We need to show that both old and new damagers transfer damage correctly.

        String unit = "Crab CRB-20.mtf";
        Mek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(14, mek.getArmor(Mek.LOC_LEFT_ARM));

        // Deal damage with original method; have to explicitly set the damage manager
        // for the game manager due to ping-pong callbacks during ammo explosions and such.
        gameMan.setDamageManager(oldMan);
        HitData hit = new HitData(Mek.LOC_LEFT_ARM);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 49);
        oldMan.damageEntity(damageInfo);
        // Armor is at least DOOMED on destroyed component
        assertTrue(IArmorState.ARMOR_DOOMED >= mek.getArmor(Mek.LOC_LEFT_ARM));
        // Internal Structure should be gone.
        assertTrue(IArmorState.ARMOR_DOOMED >= mek.getInternal(Mek.LOC_LEFT_ARM));
        // Should be marked as destroyed this phase, but not blown off (since already destroyed)
        if (mek.getArmor(Mek.LOC_LEFT_ARM) == IArmorState.ARMOR_DOOMED) {
            // Shouldn't be blown off if armor state is ARMOR_DOOMED
            assertFalse(mek.isLocationBlownOff(Mek.LOC_LEFT_ARM));
            // LT also destroyed
            assertEquals(IArmorState.ARMOR_DOOMED, mek.getArmor(Mek.LOC_LEFT_TORSO));
            assertEquals(IArmorState.ARMOR_DOOMED, mek.getInternal(Mek.LOC_LEFT_TORSO));
            // One damage transfers to CT
            assertEquals(17, mek.getArmor(Mek.LOC_CENTER_TORSO));
        }

        // Reset for new damage method; have to explicitly set the damage manager
        // for the game manager due to ping-pong callbacks during ammo explosions and such.
        gameMan.setDamageManager(newMan);
        Mek mek2 = loadMek(unit);

        // Validate starting armor
        assertEquals(14, mek2.getArmor(Mek.LOC_LEFT_ARM));

        // Deal damage with new method
        // Check for ARMOR_DOOMED _or less_, in case of chain crits
        hit = new HitData(Mek.LOC_LEFT_ARM);
        damageInfo = new DamageInfo(mek2, hit, 49);
        newMan.damageEntity(damageInfo);
        assertTrue(IArmorState.ARMOR_DOOMED >= mek2.getArmor(Mek.LOC_LEFT_ARM));
        assertTrue(IArmorState.ARMOR_DOOMED >= mek2.getInternal(Mek.LOC_LEFT_ARM));
        if (mek2.getArmor(Mek.LOC_LEFT_ARM) == IArmorState.ARMOR_DOOMED) {
            // Shouldn't be blown off if armor state is ARMOR_DOOMED
            assertFalse(mek2.isLocationBlownOff(Mek.LOC_LEFT_ARM));
            assertEquals(IArmorState.ARMOR_DOOMED, mek2.getArmor(Mek.LOC_LEFT_TORSO));
            assertEquals(IArmorState.ARMOR_DOOMED, mek2.getInternal(Mek.LOC_LEFT_TORSO));
            assertEquals(17, mek2.getArmor(Mek.LOC_CENTER_TORSO));
        }
    }

    @ParameterizedTest()
    @ValueSource(strings = { "Original", "Modular" })
    void destroySectionCritTransfers(String manager) throws FileNotFoundException {
        // We need to show that both old and new damage managers transfer damage correctly.
        TWDamageManager damageManager = (manager.equals("Original")) ? oldMan : newMan;

        String unit = "Crab CRB-20.mtf";
        Mek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(14, mek.getArmor(Mek.LOC_LEFT_TORSO));

        // Deal damage with provided method; have to explicitly set the damage manager
        // for the game manager due to ping-pong callbacks during ammo explosions and such.
        gameMan.setDamageManager(damageManager);

        // set phase to Firing
        game.setPhase(GamePhase.FIRING);

        // Deal initial damage
        // Destroy LT
        HitData hit = new HitData(Mek.LOC_LEFT_TORSO);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 26);
        damageManager.damageEntity(damageInfo);
        // Armor is DOOMED on destroyed component
        assertEquals(IArmorState.ARMOR_DOOMED, mek.getArmor(Mek.LOC_LEFT_TORSO));
        // Internal Structure should be gone.
        assertEquals(IArmorState.ARMOR_DOOMED, mek.getInternal(Mek.LOC_LEFT_TORSO));
        // Arm should be marked as blown off
        assertTrue(mek.isLocationBlownOff(Mek.LOC_LEFT_ARM));
        // LT also destroyed
        assertEquals(IArmorState.ARMOR_DOOMED, mek.getArmor(Mek.LOC_LEFT_TORSO));
        assertEquals(IArmorState.ARMOR_DOOMED, mek.getInternal(Mek.LOC_LEFT_TORSO));
        // No damage transfers to CT
        assertEquals(18, mek.getArmor(Mek.LOC_CENTER_TORSO));

        // Rest phase for mek entity to finalize damage (skip actual phase mgmt here)
        gameMan.resetEntityPhase(GamePhase.PHYSICAL);

        // Now destroy the left leg plus 1
        hit = new HitData(Mek.LOC_LEFT_LEG);
        damageInfo = new DamageInfo(mek, hit, 35);
        damageManager.damageEntity(damageInfo);

        // LL now destroyed
        assertEquals(IArmorState.ARMOR_DOOMED, mek.getArmor(Mek.LOC_LEFT_LEG));
        assertEquals(IArmorState.ARMOR_DOOMED, mek.getInternal(Mek.LOC_LEFT_LEG));

        // 1 damage transfers to CT
        assertEquals(17, mek.getArmor(Mek.LOC_CENTER_TORSO));

        // Assert PSR for this unit exists
        assertTrue(game.getPSRs().hasMoreElements());
    }

    @Test
    void damageReactiveArmorBA() throws FileNotFoundException {
        String unit = "Black Wolf BA (ER Pulse) (Sqd5).blk";
        BattleArmor mek = loadBA(unit);

        // Validate starting armor
        assertEquals(11, mek.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal "20" points of damage (should fill 10 circles)
        HitData hit = new HitData(BattleArmor.LOC_TROOPER_1);
        // All AmmoWeaponHandlers (including artillery) use Ballistic damage type
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        // Set areaSatArty so all suits take damage
        DamageInfo damageInfo = new DamageInfo(mek, hit, 20, false, DamageType.NONE,
              false, true);
        newMan.damageEntity(damageInfo);

        assertEquals(1, mek.getArmor(BattleArmor.LOC_TROOPER_1));
    }

    @Test
    void damageMekHardenedArmorNoPSR() throws FileNotFoundException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (33 points of hardened ~= 66 points standard)
        assertEquals(33, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "39" points of damage (should fill 19 circles and half of 1)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        newMan.damageEntity(damageInfo);

        assertEquals(14, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CENTER_TORSO);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 39);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekHardenedArmorWithPSR() throws FileNotFoundException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (33 points of hardened ~= 66 points standard)
        assertEquals(33, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "40" points of damage (should fill 20 circles and cause a PSR)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        newMan.damageEntity(damageInfo);

        assertEquals(13, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekHardenedArmorNoCritAP() throws FileNotFoundException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = loadMek(unit);

        // Deal "20" points of AP damage (should fill 20 circles against Hardened)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO, false, 0, false,
              -1, true, true, HitData.DAMAGE_ARMOR_PIERCING, 0);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 20);
        newMan.damageEntity(damageInfo);

        assertEquals(13, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekStandardArmorCritFromAP() throws FileNotFoundException {
        String unit = "Cyclops CP-10-Z.mtf";
        BipedMek mek = loadMek(unit);

        // Deal "20" points of AP damage (should fill 20 circles against Standard)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO, false, 0, false,
              -1, true, true, HitData.DAMAGE_ARMOR_PIERCING, 0);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 20);
        newMan.damageEntity(damageInfo);

        // Don't check for exact remaining armor as AP may produce fatal crits, but PSRs will remain
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekBallisticReinforcedArmorNoPSR() throws FileNotFoundException {
        String unit = "Dervish DV-11DK.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (25 points of BRA ~= 50 - (1xhits) points standard against some damage types)
        assertEquals(25, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "39" points of damage (should fill 19 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        newMan.damageEntity(damageInfo);

        assertEquals(6, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit2.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 39);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekBallisticReinforcedArmorWithPSR() throws FileNotFoundException {
        String unit = "Dervish DV-11DK.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (25 points of BRA ~= 50 - (1xhits) points standard against some damage types)
        assertEquals(25, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "40" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        newMan.damageEntity(damageInfo);

        assertEquals(5, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekImpactResistantArmorNoPSR() throws FileNotFoundException {
        // Takes 2 damage per 3 full damage dealt
        String unit = "Storm Raider STM-R4.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(17, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "24" points of damage (should fill 16 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 24);
        newMan.damageEntity(damageInfo);

        assertEquals(1, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit2.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 24);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekImpactResistantArmorWithPSR() throws FileNotFoundException {
        // Takes 2 damage per 3 full damage dealt
        String unit = "Storm Raider STM-R4.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(17, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "30" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 30);
        newMan.damageEntity(damageInfo);

        assertEquals(IArmorState.ARMOR_DESTROYED, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekImpactResistantArmorNonphysicalWithPSR() throws FileNotFoundException {
        // Takes normal damage when not physical damage
        String unit = "Storm Raider STM-R4.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(17, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal 20 points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 20);
        newMan.damageEntity(damageInfo);

        assertEquals(IArmorState.ARMOR_DESTROYED, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekFerroLamellorArmorNoPSR() throws FileNotFoundException {
        String unit = "Charger C.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(40, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "24" points of damage (should fill 19? circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 24);
        newMan.damageEntity(damageInfo);

        assertEquals(21, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit2.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 24);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekFerroLamellorArmorWithPSR() throws FileNotFoundException {
        String unit = "Charger C.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(40, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "25" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 25);
        newMan.damageEntity(damageInfo);

        assertEquals(20, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageAeroFerroLamellorArmorCritChecksChecks() throws FileNotFoundException {
        String unit = "Slayer SL-CX1.blk";
        AeroSpaceFighter asf = loadASF(unit);

        // Validate starting armor
        assertEquals(80, asf.getArmor(AeroSpaceFighter.LOC_NOSE));

        // Deal "12" points of damage (should fill 8 circles)
        HitData hit = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(asf, hit, 10);
        newMan.damageEntity(damageInfo);

        assertEquals(72, asf.getArmor(AeroSpaceFighter.LOC_NOSE));
        assertFalse(asf.wasCritThresh());

        // Show that new system does show a check is required for more damage than the threshold
        AeroSpaceFighter asf2 = loadASF(unit);
        HitData hit2 = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit2.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo2 = new DamageInfo(asf2, hit2, 12);
        newMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(asf2));
    }

    @Test
    void damageMekCowlDamageCowlOnly() throws FileNotFoundException {
        String unit = "Cyclops CP-10-Z.mtf";
        BipedMek mek = loadMek(unit);

        // Configure game for Quirks
        IOption quirkOption = game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        quirkOption.setValue(true);

        // Validate starting armor
        assertEquals(9, mek.getArmor(BipedMek.LOC_HEAD));

        // Deal 3 points of damage (should fill 3 circles but leave 9 still)
        HitData hit = new HitData(BipedMek.LOC_HEAD);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 3);
        newMan.damageEntity(damageInfo);

        assertEquals(9, mek.getArmor(BipedMek.LOC_HEAD));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // reset quirk value
        quirkOption.setValue(false);
    }

    @Test
    void damageMekCowlDamageAllHeadArmor() throws FileNotFoundException {
        String unit = "Cyclops CP-10-Z.mtf";
        BipedMek mek = loadMek(unit);

        // Configure game for Quirks
        IOption quirkOption = game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        quirkOption.setValue(true);

        // Validate starting armor
        assertEquals(9, mek.getArmor(BipedMek.LOC_HEAD));
        assertEquals(3, mek.getInternal(BipedMek.LOC_HEAD));

        // Deal 12 points of damage (should fill 12 total (3 + 9) leaving armor at 0 but no crits)
        HitData hit = new HitData(BipedMek.LOC_HEAD);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 12);
        newMan.damageEntity(damageInfo);

        // Armor is _gone_ but not _destroyed)
        assertEquals(0, mek.getArmor(BipedMek.LOC_HEAD));
        assertEquals(3, mek.getInternal(BipedMek.LOC_HEAD));
        assertFalse(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekReactiveArmorNoPSR() throws FileNotFoundException {
        String unit = "Warwolf A.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(35, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "39" points of damage (should fill 19 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        newMan.damageEntity(damageInfo);

        assertEquals(16, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit2.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 24);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekReactiveArmorWithPSR() throws FileNotFoundException {
        String unit = "Warwolf A.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(35, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "40" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        newMan.damageEntity(damageInfo);

        assertEquals(15, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekReflectiveArmorNoPSR() throws FileNotFoundException {
        String unit = "Flashman FLS-10E.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(30, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "39" points of damage (should fill 19 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        newMan.damageEntity(damageInfo);

        assertEquals(11, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit2.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 24);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekReflectiveArmorWithPSR() throws FileNotFoundException {
        String unit = "Flashman FLS-10E.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(30, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "40" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        newMan.damageEntity(damageInfo);

        assertEquals(10, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageAeroReflectiveArmorCritChecks() throws FileNotFoundException {
        String unit = "Seydlitz C.blk";
        AeroSpaceFighter asf = loadASF(unit);

        // Validate starting armor
        assertEquals(18, asf.getArmor(AeroSpaceFighter.LOC_NOSE));

        // Deal "4" points of damage (should fill 2 circles)
        HitData hit = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(asf, hit, 4);
        newMan.damageEntity(damageInfo);

        assertEquals(16, asf.getArmor(AeroSpaceFighter.LOC_NOSE));
        assertFalse(asf.wasCritThresh());

        // Show that new system does show a check is required for more damage than the threshold
        AeroSpaceFighter asf2 = loadASF(unit);
        HitData hit2 = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit2.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo2 = new DamageInfo(asf2, hit2, 5);
        newMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(asf2));
    }

    @Test
    void damageAeroSIWithHalvedDamageTransfer() throws FileNotFoundException {
        String unit = "Seydlitz C.blk";
        AeroSpaceFighter asf = loadASF(unit);

        // Validate starting armor and SI
        assertEquals(11, asf.getArmor(AeroSpaceFighter.LOC_LEFT_WING));
        assertEquals(11, asf.getSI());

        // Deal 13 points of damage (should fill 11 circles and deal 1 SI damage without overflowing and 
        // destroying the unit)
        HitData hit = new HitData(AeroSpaceFighter.LOC_LEFT_WING);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(asf, hit, 13);
        newMan.damageEntity(damageInfo);

        assertEquals(IArmorState.ARMOR_DESTROYED, asf.getArmor(AeroSpaceFighter.LOC_LEFT_WING));
        assertEquals(10, asf.getSI());
        assertTrue(asf.wasCritThresh());
        assertTrue(gameMan.checkForPSRFromDamage(asf));
        assertFalse(asf.isDestroyed());
        assertFalse(asf.isDoomed());
    }
}
