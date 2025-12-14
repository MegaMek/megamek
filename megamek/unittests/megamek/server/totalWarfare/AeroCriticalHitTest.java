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

import java.io.File;
import java.io.IOException;

import megamek.common.DamageInfo;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Entity;
import megamek.server.Server;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for aerospace critical hit mechanics per Strategic Operations rules.
 * <p>
 * Per SO p.116: - Standard aerospace: Threshold criticals trigger when damage EXCEEDS threshold (>) - Capital fighters:
 * Threshold = 1, so > 1 (i.e., 2+) triggers crit ("at least 15 points of standard-scale damage" = 2 capital damage) -
 * Fatal threshold: divisor of 2, target 10+
 */
class AeroCriticalHitTest {

    private final TWGameManager gameManager = new TWGameManager();
    private TWDamageManagerModular damageManager;
    private Game game;
    private Player player;
    private Server server;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws IOException {
        game = gameManager.getGame();
        GameOptions gameOptions = new GameOptions();
        game.setOptions(gameOptions);

        damageManager = new TWDamageManagerModular(gameManager, game);
        gameManager.setDamageManager(damageManager);

        server = ServerFactory.createServer(gameManager);
        player = new Player(1, "Test");
        game.addPlayer(1, player);
    }

    @AfterEach
    void tearDown() {
        server.die();
    }

    Entity loadEntityFromFile(String filename) throws EntityLoadingException {
        String resourcesPath = "testresources/megamek/common/units/";
        File file = new File(resourcesPath + filename);
        MekFileParser parser = new MekFileParser(file);
        return parser.getEntity();
    }

    AeroSpaceFighter loadASF(String filename) throws EntityLoadingException {
        AeroSpaceFighter asf = (AeroSpaceFighter) loadEntityFromFile(filename);
        asf.setId(game.getNextEntityId());
        game.addEntity(asf);
        asf.setOwner(player);
        return asf;
    }

    @Nested
    @DisplayName("Threshold Critical Trigger Tests")
    class ThresholdCriticalTriggerTests {

        /**
         * For standard (non-capital) aerospace, damage must EXCEED threshold to trigger crit. Cheetah F-11 has 18 wing
         * armor, threshold = ceiling(18/10) = 2. 2 damage does NOT trigger (2 > 2 = false).
         */
        @Test
        @DisplayName("Exactly threshold damage does NOT trigger critical (standard aerospace)")
        void testExactlyThresholdDamageNoTrigger() throws EntityLoadingException {
            // Cheetah F-11 has 18 left wing armor, threshold = ceiling(18/10) = 2
            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Verify threshold is 2 for the wing
            assertEquals(2, asf.getThresh(AeroSpaceFighter.LOC_LEFT_WING),
                  "Fighter should have threshold of 2 (ceiling of 18/10)");

            // Deal exactly 2 standard damage - at threshold
            HitData hit = new HitData(AeroSpaceFighter.LOC_LEFT_WING);
            DamageInfo damageInfo = new DamageInfo(asf, hit, 2);
            damageManager.damageEntity(damageInfo);

            // Standard aerospace: damage must EXCEED threshold (>)
            // 2 > 2 = FALSE, so no crit
            assertFalse(asf.wasCritThresh(),
                  "Exactly 2 damage (equals threshold) should NOT trigger critical for standard aerospace");
        }

        /**
         * This test verifies that 1 damage (below threshold of 2) does NOT trigger check.
         */
        @Test
        @DisplayName("Below threshold damage does NOT trigger critical")
        void testBelowThresholdDamageNoCheck() throws EntityLoadingException {
            // Cheetah F-11 has 18 left wing armor, threshold = ceiling(18/10) = 2
            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Verify threshold is 2
            assertEquals(2, asf.getThresh(AeroSpaceFighter.LOC_LEFT_WING));

            // Deal 1 standard damage - below threshold
            HitData hit = new HitData(AeroSpaceFighter.LOC_LEFT_WING);
            DamageInfo damageInfo = new DamageInfo(asf, hit, 1);
            damageManager.damageEntity(damageInfo);

            // 1 damage is below threshold of 2, should NOT trigger
            assertFalse(asf.wasCritThresh(),
                  "1 damage (below threshold of 2) should NOT trigger critical check");
        }

        /**
         * This test verifies that 3 damage (above threshold) triggers check. Standard aerospace: damage > threshold
         * triggers crit.
         */
        @Test
        @DisplayName("Above threshold damage triggers critical")
        void testAboveThresholdDamageTriggersCheck() throws EntityLoadingException {
            // Cheetah F-11 has 18 left wing armor, threshold = ceiling(18/10) = 2
            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Deal 3 standard damage - above threshold of 2
            HitData hit = new HitData(AeroSpaceFighter.LOC_LEFT_WING);
            DamageInfo damageInfo = new DamageInfo(asf, hit, 3);
            damageManager.damageEntity(damageInfo);

            // 3 > 2 = TRUE, crit triggered
            assertTrue(asf.wasCritThresh(),
                  "3 damage (exceeds threshold of 2) should trigger critical check");
        }
    }

    @Nested
    @DisplayName("Fatal Threshold Calculation Tests (SO p.116)")
    class FatalThresholdCalculationTests {

        /**
         * Per SO p.116: Fatal Threshold = max(2, ceiling(Total Capital Armor / 4)) Fatal threshold roll: 2d6 + (damage
         * - threshold) / 2, destroys on 10+
         */
        @Test
        @DisplayName("Fatal threshold calculation - minimum of 2")
        void testFatalThresholdMinimumTwo() throws EntityLoadingException {
            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Fatal threshold should be at least 2
            assertTrue(asf.getFatalThresh() >= 2,
                  "Fatal threshold should be at least 2 for fighters per SO p.116");
        }

        /**
         * Per SO p.116: Fatal Threshold = max(2, ceiling(Total Capital Armor / 4))
         */
        @Test
        @DisplayName("Fatal threshold calculation matches formula")
        void testFatalThresholdCalculation() throws EntityLoadingException {
            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Calculate expected: ceiling(capitalArmor / 4), minimum 2
            int capitalArmor = asf.getCapArmor();
            int expectedThreshold = Math.max(2, (int) Math.ceil(capitalArmor / 4.0));

            assertEquals(expectedThreshold, asf.getFatalThresh(),
                  "Fatal threshold should be max(2, ceiling(capitalArmor/4))");
        }

        /**
         * Verify autoSetFatalThresh() correctly recalculates threshold.
         */
        @Test
        @DisplayName("autoSetFatalThresh recalculates correctly")
        void testAutoSetFatalThresh() throws EntityLoadingException {
            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            int capitalArmor = asf.getCapArmor();
            int expectedThreshold = Math.max(2, (int) Math.ceil(capitalArmor / 4.0));

            // Force recalculation
            asf.autoSetFatalThresh();

            assertEquals(expectedThreshold, asf.getFatalThresh(),
                  "autoSetFatalThresh should correctly calculate threshold");
        }
    }

    @Nested
    @DisplayName("Standard Fighter Damage Threshold Tests")
    class StandardFighterThresholdTests {

        /**
         * Verify non-capital fighter threshold is calculated as ceiling(armor/10). Cheetah F-11 has: Nose=22, LW=18,
         * RW=18, Aft=22 Thresholds: ceiling(22/10)=3, ceiling(18/10)=2, ceiling(18/10)=2, ceiling(22/10)=3
         */
        @Test
        @DisplayName("Non-capital fighter threshold is ceiling(armor/10)")
        void testNonCapitalFighterThreshold() throws EntityLoadingException {
            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Cheetah F-11 armor: Nose=22, Wings=18, Aft=22
            // Non-capital fighters calculate threshold as ceiling(armor/10)
            assertEquals(3, asf.getThresh(AeroSpaceFighter.LOC_NOSE),
                  "Nose threshold should be ceiling(22/10) = 3");
            assertEquals(2, asf.getThresh(AeroSpaceFighter.LOC_LEFT_WING),
                  "Left wing threshold should be ceiling(18/10) = 2");
            assertEquals(2, asf.getThresh(AeroSpaceFighter.LOC_RIGHT_WING),
                  "Right wing threshold should be ceiling(18/10) = 2");
            assertEquals(3, asf.getThresh(AeroSpaceFighter.LOC_AFT),
                  "Aft threshold should be ceiling(22/10) = 3");
        }
    }

    @Nested
    @DisplayName("Capital Fighter Threshold Tests (Issue #3084)")
    class CapitalFighterThresholdTests {

        /**
         * Per SO p.116: Fighter squadrons trigger critical hits when "any single attack destroys 2 or more points of
         * the fighter's capital-scale armor" (at least 15 points of standard-scale damage = 2 capital damage).
         * <p>
         * To make "> threshold" work correctly, capital fighter threshold must be 1.
         */
        @Test
        @DisplayName("Capital fighter threshold is 1 per SO p.116")
        void testCapitalFighterThresholdIsOne() throws EntityLoadingException {
            // Enable capital fighter option
            IOption capitalFighterOption = game.getOptions()
                  .getOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER);
            capitalFighterOption.setValue(true);

            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Capital fighters should have threshold of 1 for all locations
            // This makes "> 1" trigger on 2+ capital damage per SO p.116
            assertEquals(1, asf.getThresh(AeroSpaceFighter.LOC_NOSE),
                  "Capital fighter threshold should be 1 (so >1 triggers on 2+ capital damage)");
            assertEquals(1, asf.getThresh(AeroSpaceFighter.LOC_LEFT_WING),
                  "Capital fighter threshold should be 1");
            assertEquals(1, asf.getThresh(AeroSpaceFighter.LOC_RIGHT_WING),
                  "Capital fighter threshold should be 1");
            assertEquals(1, asf.getThresh(AeroSpaceFighter.LOC_AFT),
                  "Capital fighter threshold should be 1");

            // Reset option
            capitalFighterOption.setValue(false);
        }

        /**
         * Per SO p.116: 2+ capital damage (15+ standard) should trigger threshold critical. With threshold = 1, damage
         * of 2 exceeds threshold (2 > 1 = true).
         */
        @Test
        @DisplayName("2 capital damage triggers threshold critical (Issue #3084)")
        void testTwoCapitalDamageTriggersThresholdCrit() throws EntityLoadingException {
            // Enable capital fighter option
            IOption capitalFighterOption = game.getOptions()
                  .getOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER);
            capitalFighterOption.setValue(true);

            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Verify threshold is 1
            assertEquals(1, asf.getThresh(AeroSpaceFighter.LOC_LEFT_WING));

            // Deal 2 capital damage (simulating 15-24 standard damage converted to capital)
            HitData hit = new HitData(AeroSpaceFighter.LOC_LEFT_WING);
            hit.setCapital(true);  // Mark as capital-scale damage
            DamageInfo damageInfo = new DamageInfo(asf, hit, 2);
            damageManager.damageEntity(damageInfo);

            // 2 > 1 = TRUE, threshold critical should trigger
            assertTrue(asf.wasCritThresh(),
                  "2 capital damage (>1 threshold) should trigger critical per SO p.116");

            // Reset option
            capitalFighterOption.setValue(false);
        }

        /**
         * Per SO p.116: 1 capital damage (10-14 standard) should NOT trigger threshold critical. With threshold = 1,
         * damage of 1 does not exceed threshold (1 > 1 = false).
         */
        @Test
        @DisplayName("1 capital damage does NOT trigger threshold critical")
        void testOneCapitalDamageNoThresholdCrit() throws EntityLoadingException {
            // Enable capital fighter option
            IOption capitalFighterOption = game.getOptions()
                  .getOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER);
            capitalFighterOption.setValue(true);

            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Verify threshold is 1
            assertEquals(1, asf.getThresh(AeroSpaceFighter.LOC_LEFT_WING));

            // Deal 1 capital damage (simulating 10-14 standard damage converted to capital)
            HitData hit = new HitData(AeroSpaceFighter.LOC_LEFT_WING);
            hit.setCapital(true);  // Mark as capital-scale damage
            DamageInfo damageInfo = new DamageInfo(asf, hit, 1);
            damageManager.damageEntity(damageInfo);

            // 1 > 1 = FALSE, threshold critical should NOT trigger
            assertFalse(asf.wasCritThresh(),
                  "1 capital damage (not >1 threshold) should NOT trigger critical");

            // Reset option
            capitalFighterOption.setValue(false);
        }
    }

    @Nested
    @DisplayName("Weapon Group SingleAV Threshold Tests (SO p.116)")
    class WeaponGroupSingleAVThresholdTests {

        /**
         * Per SO p.116: When a weapon bay fires multiple weapons, threshold critical checks use only a single weapon's
         * damage value (singleAV), not the total damage.
         * <p>
         * Example: 22 Medium Lasers hit for 110 total damage, but threshold check uses only 5 damage (one ML). If
         * threshold is 3, 5 > 3 triggers crit, but if threshold is 6, then 5 > 6 is false and no crit.
         */
        @Test
        @DisplayName("Weapon group uses singleAV for threshold, not total damage")
        void testWeaponGroupUsesSingleAVForThreshold() throws EntityLoadingException {
            // Cheetah F-11 has 22 nose armor, threshold = ceiling(22/10) = 3
            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Verify threshold is 3 for nose
            assertEquals(3, asf.getThresh(AeroSpaceFighter.LOC_NOSE),
                  "Nose threshold should be ceiling(22/10) = 3");

            // Simulate a weapon bay attack: 10 total damage, but singleAV = 2
            // Total damage (10) exceeds threshold (3), but singleAV (2) does NOT
            HitData hit = new HitData(AeroSpaceFighter.LOC_NOSE);
            hit.setSingleAV(2);  // Single weapon damage is only 2
            DamageInfo damageInfo = new DamageInfo(asf, hit, 10);
            damageManager.damageEntity(damageInfo);

            // Threshold check should use singleAV (2), not total damage (10)
            // 2 > 3 = FALSE, so no crit should trigger
            assertFalse(asf.wasCritThresh(),
                  "With singleAV=2 and threshold=3, no crit should trigger (2 > 3 = false)");

            // Verify full damage was still applied (armor should be reduced by 10)
            assertEquals(12, asf.getArmor(AeroSpaceFighter.LOC_NOSE),
                  "Full 10 damage should be applied even though threshold uses singleAV");
        }

        /**
         * Verify that when singleAV exceeds threshold, critical is triggered.
         */
        @Test
        @DisplayName("Weapon group triggers crit when singleAV exceeds threshold")
        void testWeaponGroupTriggersCritWhenSingleAVExceedsThreshold() throws EntityLoadingException {
            // Cheetah F-11 has 18 wing armor, threshold = ceiling(18/10) = 2
            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Verify threshold is 2 for wing
            assertEquals(2, asf.getThresh(AeroSpaceFighter.LOC_LEFT_WING),
                  "Wing threshold should be ceiling(18/10) = 2");

            // Simulate a weapon bay attack: 15 total damage, singleAV = 5
            // Both total (15) and singleAV (5) exceed threshold (2)
            HitData hit = new HitData(AeroSpaceFighter.LOC_LEFT_WING);
            hit.setSingleAV(5);  // Single weapon damage is 5 (e.g., one ML)
            DamageInfo damageInfo = new DamageInfo(asf, hit, 15);
            damageManager.damageEntity(damageInfo);

            // Threshold check uses singleAV (5), which exceeds threshold (2)
            // 5 > 2 = TRUE, crit should trigger
            assertTrue(asf.wasCritThresh(),
                  "With singleAV=5 and threshold=2, crit should trigger (5 > 2 = true)");
        }

        /**
         * Verify that without singleAV set, total damage is used for threshold check.
         */
        @Test
        @DisplayName("Without singleAV, total damage is used for threshold check")
        void testWithoutSingleAVUsesTotalDamage() throws EntityLoadingException {
            // Cheetah F-11 has 18 wing armor, threshold = ceiling(18/10) = 2
            AeroSpaceFighter asf = loadASF("Cheetah F-11.blk");

            // Deal 5 damage without setting singleAV (simulating non-weapon-group attack)
            HitData hit = new HitData(AeroSpaceFighter.LOC_LEFT_WING);
            // Note: NOT setting singleAV, so it defaults to -1
            DamageInfo damageInfo = new DamageInfo(asf, hit, 5);
            damageManager.damageEntity(damageInfo);

            // Without singleAV, total damage (5) is used for threshold check
            // 5 > 2 = TRUE, crit should trigger
            assertTrue(asf.wasCritThresh(),
                  "Without singleAV set, total damage (5) should be used for threshold check");
        }
    }
}
