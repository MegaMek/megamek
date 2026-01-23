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

package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import megamek.common.DamageInfo;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.LiftHoist;
import megamek.common.equipment.MekArms;
import megamek.common.equipment.Transporter;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.weapons.DamageType;
import megamek.server.Server;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

class TWDamageManagerTest {

    private final TWGameManager gameMan = new TWGameManager();
    private TWDamageManager manager;
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
        manager = new TWDamageManager(gameMan, game);

        // Need servers to handle unit destruction (sad face)
        server = ServerFactory.createServer(gameMan);
        // Player for certain checks and messages
        player = new Player(1, "Test");
        game.addPlayer(1, player);
    }

    @AfterEach
    void tearDown() {
        server.die();
    }

    Entity loadEntityFromFile(String filename) throws EntityLoadingException {
        File file;
        MekFileParser mfParser;
        Entity e;

        String resourcesPath = "testresources/megamek/common/units/";
        file = new File(resourcesPath + filename);
        mfParser = new MekFileParser(file);
        e = mfParser.getEntity();

        return e;
    }

    BattleArmor loadBA(String filename) throws EntityLoadingException {
        BattleArmor battleArmor = (BattleArmor) loadEntityFromFile(filename);
        battleArmor.setId(game.getNextEntityId());
        game.addEntity(battleArmor);
        battleArmor.setOwner(player);
        return battleArmor;
    }

    BipedMek loadMek(String filename) throws EntityLoadingException {
        BipedMek mek = (BipedMek) loadEntityFromFile(filename);
        mek.setId(game.getNextEntityId());
        game.addEntity(mek);
        mek.setOwner(player);
        return mek;
    }

    HandheldWeapon loadHHW(String filename) throws EntityLoadingException {
        HandheldWeapon hhw = (HandheldWeapon) loadEntityFromFile(filename);
        hhw.setId(game.getNextEntityId());
        game.addEntity(hhw);
        hhw.setOwner(player);
        return hhw;
    }

    AeroSpaceFighter loadASF(String filename) throws EntityLoadingException {
        AeroSpaceFighter asf = (AeroSpaceFighter) loadEntityFromFile(filename);
        asf.setId(game.getNextEntityId());
        game.addEntity(asf);
        asf.setOwner(player);
        return asf;
    }

    @Test
    void testDamageBASingle() throws EntityLoadingException {
        String unit = "Elemental BA [Laser] (Sqd5).blk";
        BattleArmor baEntity = loadBA(unit);

        // Validate starting armor
        assertEquals(10, baEntity.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal damage to one trooper
        HitData hit = new HitData(BattleArmor.LOC_TROOPER_1);
        DamageInfo damageInfo = new DamageInfo(baEntity, hit, 5);
        manager.damageEntity(damageInfo);
        assertEquals(5, baEntity.getArmor(BattleArmor.LOC_TROOPER_1));
    }

    @Test
    void testKillBASquad() throws EntityLoadingException {
        // Show that the damageBA method within damageEntity code kills BAs _dead_.

        String unit = "Elemental BA [Laser] (Sqd5).blk";
        BattleArmor baEntity = loadBA(unit);
        DamageInfo damageInfo;

        // Validate starting armor
        assertEquals(10, baEntity.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal repeated damage to the whole BA squad
        for (int count = 0; count <= 15; count++) {
            HitData hit = baEntity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
            damageInfo = new DamageInfo(baEntity, hit, 5);
            manager.damageEntity(damageInfo);
        }

        assertTrue(baEntity.isDoomed());
    }

    @Test
    void testDestroySectionDamageTransfers() throws EntityLoadingException {
        // Confirm that damage transfers appropriately
        String unit = "Crab CRB-20.mtf";
        Mek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(14, mek.getArmor(Mek.LOC_LEFT_ARM));

        // Deal damage to one location and confirm armor is all removed
        gameMan.setDamageManager(manager);
        HitData hit = new HitData(Mek.LOC_LEFT_ARM);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 49);
        manager.damageEntity(damageInfo);
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
    }

    @Test
    void testDestroySectionCritTransfers() throws EntityLoadingException {
        gameMan.setDamageManager(manager);

        String unit = "Crab CRB-20.mtf";
        Mek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(14, mek.getArmor(Mek.LOC_LEFT_TORSO));

        // set phase to Firing
        game.setPhase(GamePhase.FIRING);

        // Deal initial damage
        // Destroy LT
        HitData hit = new HitData(Mek.LOC_LEFT_TORSO);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 26);
        manager.damageEntity(damageInfo);
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
        manager.damageEntity(damageInfo);

        // LL now destroyed
        assertEquals(IArmorState.ARMOR_DOOMED, mek.getArmor(Mek.LOC_LEFT_LEG));
        assertEquals(IArmorState.ARMOR_DOOMED, mek.getInternal(Mek.LOC_LEFT_LEG));

        // 1 damage transfers to CT
        assertEquals(17, mek.getArmor(Mek.LOC_CENTER_TORSO));

        // Assert PSR for this unit exists
        assertTrue(game.getPSRs().hasMoreElements());
    }

    @Test
    void testDamageReactiveArmorBA() throws EntityLoadingException {
        String unit = "Black Wolf BA (ER Pulse) (Sqd5).blk";
        BattleArmor baEntity = loadBA(unit);

        // Validate starting armor
        assertEquals(11, baEntity.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal "20" points of damage (should fill 10 circles)
        HitData hit = new HitData(BattleArmor.LOC_TROOPER_1);
        // All AmmoWeaponHandlers (including artillery) use Ballistic damage type
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        // Set areaSatArty so all suits take damage
        DamageInfo damageInfo = new DamageInfo(baEntity, hit, 20, false, DamageType.NONE,
              false, true);
        manager.damageEntity(damageInfo);

        assertEquals(1, baEntity.getArmor(BattleArmor.LOC_TROOPER_1));
    }

    @Test
    void testDamageMekHardenedArmorNoPSR() throws EntityLoadingException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (33 points of hardened ~= 66 points standard)
        assertEquals(33, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "39" points of damage (should fill 19 circles and half of 1)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        manager.damageEntity(damageInfo);

        assertEquals(14, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekHardenedArmorWithPSR() throws EntityLoadingException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (33 points of hardened ~= 66 points standard)
        assertEquals(33, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "40" points of damage (should fill 20 circles and cause a PSR)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        manager.damageEntity(damageInfo);

        assertEquals(13, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekHardenedArmorNoCritAP() throws EntityLoadingException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = loadMek(unit);

        // Deal "20" points of AP damage (should fill 20 circles against Hardened)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO, false, 0, false,
              -1, true, true, HitData.DAMAGE_ARMOR_PIERCING, 0);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 20);
        manager.damageEntity(damageInfo);

        assertEquals(13, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekStandardArmorCritFromAP() throws EntityLoadingException {
        String unit = "Cyclops CP-10-Z.mtf";
        BipedMek mek = loadMek(unit);

        // Deal "20" points of AP damage (should fill 20 circles against Standard)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO, false, 0, false,
              -1, true, true, HitData.DAMAGE_ARMOR_PIERCING, 0);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 20);
        manager.damageEntity(damageInfo);

        // Don't check for exact remaining armor as AP may produce fatal crits, but PSRs will remain
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekBallisticReinforcedArmorNoPSR() throws EntityLoadingException {
        String unit = "Dervish DV-11DK.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (25 points of BRA ~= 50 - (1x hits) points standard against some damage types)
        assertEquals(25, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "39" points of damage (should fill 19 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        manager.damageEntity(damageInfo);

        assertEquals(6, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekBallisticReinforcedArmorWithPSR() throws EntityLoadingException {
        String unit = "Dervish DV-11DK.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (25 points of BRA ~= 50 - (1x hits) points standard against some damage types)
        assertEquals(25, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "40" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        manager.damageEntity(damageInfo);

        assertEquals(5, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekImpactResistantArmorNoPSR() throws EntityLoadingException {
        // Takes 2 damage per 3 full damage dealt
        String unit = "Storm Raider STM-R4.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(17, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "24" points of damage (should fill 16 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 24);
        manager.damageEntity(damageInfo);

        assertEquals(1, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekImpactResistantArmorWithPSR() throws EntityLoadingException {
        // Takes 2 damage per 3 full damage dealt
        String unit = "Storm Raider STM-R4.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(17, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "30" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 30);
        manager.damageEntity(damageInfo);

        assertEquals(IArmorState.ARMOR_DESTROYED, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekImpactResistantArmorNonphysicalWithPSR() throws EntityLoadingException {
        // Takes normal damage when not physical damage
        String unit = "Storm Raider STM-R4.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(17, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal 20 points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 20);
        manager.damageEntity(damageInfo);

        assertEquals(IArmorState.ARMOR_DESTROYED, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekFerroLamellorArmorNoPSR() throws EntityLoadingException {
        String unit = "Charger C.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(40, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "24" points of damage (should fill 19? circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 24);
        manager.damageEntity(damageInfo);

        assertEquals(21, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekFerroLamellorArmorWithPSR() throws EntityLoadingException {
        String unit = "Charger C.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(40, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "25" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 25);
        manager.damageEntity(damageInfo);

        assertEquals(20, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageAeroFerroLamellorArmorCritChecksChecks() throws EntityLoadingException {
        String unit = "Slayer SL-CX1.blk";
        AeroSpaceFighter asf = loadASF(unit);

        // Validate starting armor (80) and threshold (ceiling(80/10) = 8)
        assertEquals(80, asf.getArmor(AeroSpaceFighter.LOC_NOSE));

        // Deal 9 points of ballistic damage against Ferro-Lamellor (reduces ~20%)
        // 9 * 0.8 = ~7 actual damage, below threshold of 8
        HitData hit = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(asf, hit, 9);
        manager.damageEntity(damageInfo);

        assertEquals(73, asf.getArmor(AeroSpaceFighter.LOC_NOSE));
        assertFalse(asf.wasCritThresh());

        // Show that damage exceeding threshold triggers a check
        // Threshold is 8, need > 8 actual damage. 12 * 0.8 = ~9.6 actual > 8
        AeroSpaceFighter asf2 = loadASF(unit);
        HitData hit2 = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit2.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo2 = new DamageInfo(asf2, hit2, 12);  // ~9-10 actual, exceeds threshold
        manager.damageEntity(damageInfo2);
        assertTrue(asf2.wasCritThresh());
    }

    @Test
    void testDamageMekCowlDamageCowlOnly() throws EntityLoadingException {
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
        manager.damageEntity(damageInfo);

        assertEquals(9, mek.getArmor(BipedMek.LOC_HEAD));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // reset quirk value
        quirkOption.setValue(false);
    }

    @Test
    void testDamageMekCowlDamageAllHeadArmor() throws EntityLoadingException {
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
        manager.damageEntity(damageInfo);

        // Armor is _gone_ but not _destroyed
        assertEquals(0, mek.getArmor(BipedMek.LOC_HEAD));
        assertEquals(3, mek.getInternal(BipedMek.LOC_HEAD));
        assertFalse(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekReactiveArmorNoPSR() throws EntityLoadingException {
        String unit = "Warwolf A.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(35, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "39" points of damage (should fill 19 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        manager.damageEntity(damageInfo);

        assertEquals(16, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekReactiveArmorWithPSR() throws EntityLoadingException {
        String unit = "Warwolf A.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(35, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "40" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        manager.damageEntity(damageInfo);

        assertEquals(15, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekReflectiveArmorNoPSR() throws EntityLoadingException {
        String unit = "Flashman FLS-10E.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(30, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "39" points of damage (should fill 19 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        manager.damageEntity(damageInfo);

        assertEquals(11, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertFalse(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageMekReflectiveArmorWithPSR() throws EntityLoadingException {
        String unit = "Flashman FLS-10E.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(30, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

        // Deal "40" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        manager.damageEntity(damageInfo);

        assertEquals(10, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void testDamageAeroReflectiveArmorCritChecks() throws EntityLoadingException {
        String unit = "Seydlitz C.blk";
        AeroSpaceFighter asf = loadASF(unit);

        // Validate starting armor (18) and threshold (ceiling(18/10) = 2)
        assertEquals(18, asf.getArmor(AeroSpaceFighter.LOC_NOSE));

        // Deal 2 points of energy damage against Reflective (halves energy damage)
        // 2 / 2 = 1 actual damage, below threshold of 2
        HitData hit = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(asf, hit, 2);
        manager.damageEntity(damageInfo);

        assertEquals(17, asf.getArmor(AeroSpaceFighter.LOC_NOSE));
        assertFalse(asf.wasCritThresh());

        // Show that damage exceeding threshold triggers a check
        // Threshold is 2, need > 2 actual damage. 6 / 2 = 3 actual > 2
        AeroSpaceFighter asf2 = loadASF(unit);
        HitData hit2 = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit2.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo2 = new DamageInfo(asf2, hit2, 6);  // 3 actual, exceeds threshold
        manager.damageEntity(damageInfo2);
        assertTrue(asf2.wasCritThresh());
    }

    @Test
    void testDamageAeroSIWithHalvedDamageTransfer() throws EntityLoadingException {
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
        manager.damageEntity(damageInfo);

        assertEquals(IArmorState.ARMOR_DESTROYED, asf.getArmor(AeroSpaceFighter.LOC_LEFT_WING));
        assertEquals(10, asf.getSI());
        assertTrue(asf.wasCritThresh());
        assertTrue(gameMan.checkForPSRFromDamage(asf));
        assertFalse(asf.isDestroyed());
        assertFalse(asf.isDoomed());
    }

    @Nested
    class HandheldWeaponDamageTests {
        String unit = "Quickdraw QKD-8X.mtf";
        String unit2 = "Light Anti-Infantry Weapon.blk";

        static BipedMek mek;

        public static List<Integer> nonArmLocations() {
            List<Integer> nonArmLocations = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                if (i != BipedMek.LOC_LEFT_ARM && i != BipedMek.LOC_RIGHT_ARM) {
                    nonArmLocations.add(i);
                }
            }
            return nonArmLocations;
        }

        @BeforeEach
        void beforeEach() throws EntityLoadingException {
            mek = loadMek(unit);
        }

        @ParameterizedTest
        @ValueSource(ints = { 6 })
        void testDamageMekWithHHWHitHHWLArm(int roll) throws EntityLoadingException {
            // Arrange
            HandheldWeapon hhwInArms = loadHHW(unit2);
            int TARGET = hhwInArms.targetForArmHitToHitCarriedObject();

            Roll mockRoll = mock(Roll.class);
            when(mockRoll.getIntValue()).thenReturn(roll);
            when(mockRoll.isTargetRollSuccess(roll)).thenReturn(roll >= TARGET);

            for (Transporter transporter : mek.getTransports()) {
                if (transporter instanceof MekArms) {
                    transporter.load(hhwInArms);
                }
            }

            // Validate starting armor for Mek (16)
            assertEquals(16, mek.getArmor(BipedMek.LOC_LEFT_ARM));

            // Validate starting armor for HHW (16)
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));

            // Act
            HitData hit = new HitData(BipedMek.LOC_LEFT_ARM);
            DamageInfo damageInfo = new DamageInfo(mek, hit, 10);
            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.rollD6(1)).thenReturn(mockRoll);

                manager.damageEntity(damageInfo);
            }

            // Assert
            // This should also hurt the HHW in the MekArms
            assertEquals(16, mek.getArmor(BipedMek.LOC_LEFT_ARM));
            assertEquals(6, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 4, 5 })
        void testDamageMekWithHHWHitArmsNoHitHHWLArm(int roll) throws EntityLoadingException {
            // Arrange
            HandheldWeapon hhwInArms = loadHHW(unit2);
            int TARGET = hhwInArms.targetForArmHitToHitCarriedObject();

            Roll mockRoll = mock(Roll.class);
            when(mockRoll.getIntValue()).thenReturn(roll);
            when(mockRoll.isTargetRollSuccess(roll)).thenReturn(roll >= TARGET);

            for (Transporter transporter : mek.getTransports()) {
                if (transporter instanceof MekArms) {
                    transporter.load(hhwInArms);
                }
            }

            // Validate starting armor for Mek (16)
            assertEquals(16, mek.getArmor(BipedMek.LOC_LEFT_ARM));

            // Validate starting armor for HHW (16)
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));

            // Act
            HitData hit = new HitData(BipedMek.LOC_LEFT_ARM);
            DamageInfo damageInfo = new DamageInfo(mek, hit, 10);
            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.rollD6(1)).thenReturn(mockRoll);

                manager.damageEntity(damageInfo);
            }

            // Assert
            // This should not hurt the HHW in the MekArms
            assertEquals(6, mek.getArmor(BipedMek.LOC_LEFT_ARM));
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));
        }

        @ParameterizedTest
        @ValueSource(ints = { 6 })
        void testDamageMekWithHHWHitHHWRArm(int roll) throws EntityLoadingException {
            // Arrange
            HandheldWeapon hhwInArms = loadHHW(unit2);
            int TARGET = hhwInArms.targetForArmHitToHitCarriedObject();

            Roll mockRoll = mock(Roll.class);
            when(mockRoll.getIntValue()).thenReturn(roll);
            when(mockRoll.isTargetRollSuccess(roll)).thenReturn(roll >= TARGET);

            for (Transporter transporter : mek.getTransports()) {
                if (transporter instanceof MekArms) {
                    transporter.load(hhwInArms);
                }
            }

            // Validate starting armor for Mek (16)
            assertEquals(16, mek.getArmor(BipedMek.LOC_RIGHT_ARM));

            // Validate starting armor for HHW (16)
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));

            // Act
            HitData hit = new HitData(BipedMek.LOC_RIGHT_ARM);
            DamageInfo damageInfo = new DamageInfo(mek, hit, 10);
            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.rollD6(1)).thenReturn(mockRoll);

                manager.damageEntity(damageInfo);
            }

            // Assert
            // This should also hurt the HHW in the MekArms
            assertEquals(16, mek.getArmor(BipedMek.LOC_RIGHT_ARM));
            assertEquals(6, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 4, 5 })
        void testDamageMekWithHHWHitArmsNoHitHHWRArm(int roll) throws EntityLoadingException {
            // Arrange
            HandheldWeapon hhwInArms = loadHHW(unit2);
            int TARGET = hhwInArms.targetForArmHitToHitCarriedObject();

            Roll mockRoll = mock(Roll.class);
            when(mockRoll.getIntValue()).thenReturn(roll);
            when(mockRoll.isTargetRollSuccess(roll)).thenReturn(roll >= TARGET);

            for (Transporter transporter : mek.getTransports()) {
                if (transporter instanceof MekArms) {
                    transporter.load(hhwInArms);
                }
            }

            // Validate starting armor for Mek (16)
            assertEquals(16, mek.getArmor(BipedMek.LOC_RIGHT_ARM));

            // Validate starting armor for HHW (16)
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));

            // Act
            HitData hit = new HitData(BipedMek.LOC_RIGHT_ARM);
            DamageInfo damageInfo = new DamageInfo(mek, hit, 10);
            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.rollD6(1)).thenReturn(mockRoll);

                manager.damageEntity(damageInfo);
            }

            // Assert
            // This should not hurt the HHW in the MekArms
            assertEquals(6, mek.getArmor(BipedMek.LOC_RIGHT_ARM));
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));
        }

        @ParameterizedTest
        @MethodSource(value = "nonArmLocations")
        void testDamageMekWithHHWHitArmsNoHitNotArm(int location) throws EntityLoadingException {
            // Arrange
            HandheldWeapon hhwInArms = loadHHW(unit2);
            int TARGET = hhwInArms.targetForArmHitToHitCarriedObject();

            // Mock roll should force a hit if we hit the Mek's arms
            Roll mockRoll = mock(Roll.class);
            when(mockRoll.getIntValue()).thenReturn(6);
            when(mockRoll.isTargetRollSuccess(6)).thenReturn(6 >= TARGET);

            for (Transporter transporter : mek.getTransports()) {
                if (transporter instanceof MekArms) {
                    transporter.load(hhwInArms);
                }
            }

            // Get starting armor for Mek
            int startingArmor = mek.getArmor(location);

            // Validate starting armor for HHW (16)
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));

            // Act
            HitData hit = new HitData(location);
            DamageInfo damageInfo = new DamageInfo(mek, hit, 5);
            try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                mockedCompute.when(() -> Compute.rollD6(1)).thenReturn(mockRoll);

                manager.damageEntity(damageInfo);
            }

            // Assert
            // This should not hurt the HHW in the MekArms
            assertEquals(startingArmor - 5, mek.getArmor(location));
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));
        }
    }


    @Nested
    class TransporterDamageTransferTests {
        String unit = "Quickdraw QKD-8X.mtf";
        String unit2 = "Light Anti-Infantry Weapon.blk";

        BipedMek mek;

        @BeforeEach
        void beforeEach() throws EntityLoadingException {
            mek = loadMek(unit);
        }

        @Test
        void testDamageMekLiftHoistHHW() throws EntityLoadingException {

            HandheldWeapon hhwInLiftHoist = loadHHW(unit2);

            for (Transporter transporter : mek.getTransports()) {
                if (transporter instanceof LiftHoist) {
                    transporter.load(hhwInLiftHoist);
                }
            }

            // Validate starting armor for Mek (30)
            assertEquals(30, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

            // Validate starting armor for HHW (16)
            assertEquals(16, hhwInLiftHoist.getArmor(HandheldWeapon.LOC_GUN));

            // Deal "10" points of damage to the Mek
            HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
            DamageInfo damageInfo = new DamageInfo(mek, hit, 10);
            manager.damageEntity(damageInfo);

            // This should also hurt the HHW in the Lift Hoist
            assertEquals(20, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
            assertEquals(6, hhwInLiftHoist.getArmor(HandheldWeapon.LOC_GUN));
            assertFalse(gameMan.checkForPSRFromDamage(mek));
        }

        @Test
        void testDamageMekLiftHoistedHHWAndHHWInArms() throws EntityLoadingException {

            HandheldWeapon hhwInArms = loadHHW(unit2);
            HandheldWeapon hhwInLiftHoist = loadHHW(unit2);

            for (Transporter transporter : mek.getTransports()) {
                if (transporter instanceof MekArms) {
                    transporter.load(hhwInArms);
                }
                if (transporter instanceof LiftHoist) {
                    transporter.load(hhwInLiftHoist);
                }
            }

            // Validate starting armor for Mek (30)
            assertEquals(30, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

            // Validate starting armor for HHWs (16)
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));
            assertEquals(16, hhwInLiftHoist.getArmor(HandheldWeapon.LOC_GUN));

            // Deal "10" points of damage to the Mek
            HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
            DamageInfo damageInfo = new DamageInfo(mek, hit, 10);
            manager.damageEntity(damageInfo);

            // This should also hurt the HHW in the Lift Hoist but not the HHW in the Mek's Arms
            assertEquals(20, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));
            assertEquals(6, hhwInLiftHoist.getArmor(HandheldWeapon.LOC_GUN));
            assertFalse(gameMan.checkForPSRFromDamage(mek));
        }

        @Test
        void testDamageMekArmsHHW() throws EntityLoadingException {

            HandheldWeapon hhwInArms = loadHHW(unit2);

            for (Transporter transporter : mek.getTransports()) {
                if (transporter instanceof MekArms) {
                    transporter.load(hhwInArms);
                }
            }

            // Validate starting armor for Mek (30)
            assertEquals(30, mek.getArmor(BipedMek.LOC_CENTER_TORSO));

            // Validate starting armor for HHW (16)
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));

            // Deal "10" points of damage to the Mek
            HitData hit = new HitData(BipedMek.LOC_CENTER_TORSO);
            DamageInfo damageInfo = new DamageInfo(mek, hit, 10);
            manager.damageEntity(damageInfo);

            // This should not hurt the HHW in the Mek's Arms
            assertEquals(20, mek.getArmor(BipedMek.LOC_CENTER_TORSO));
            assertEquals(16, hhwInArms.getArmor(HandheldWeapon.LOC_GUN));
            assertFalse(gameMan.checkForPSRFromDamage(mek));
        }
    }
}
