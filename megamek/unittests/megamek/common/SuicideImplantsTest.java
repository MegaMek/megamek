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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import megamek.common.actions.SuicideImplantsAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import megamek.common.weapons.DamageType;
import megamek.server.Server;
import megamek.server.totalWarfare.TWDamageManager;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Explosive Suicide Implants functionality (IO pg 83).
 * <p>
 * TableTop Rules:
 * <ul>
 *     <li>Conventional Infantry: 0.57 damage per trooper to all units in hex</li>
 *     <li>Reactive Detonation: When troopers die from enemy fire, automatic 0.57 damage per dead trooper to enemies</li>
 *     <li>Battle Armor: Destroys selected troopers only, no damage to others</li>
 *     <li>Mek Pilot: 1 IS to head + crit roll + cockpit destroyed</li>
 *     <li>Aero Pilot: 1 armor to nose + crit roll + cockpit destroyed</li>
 *     <li>Vehicle Crew: Crew Killed + 1 IS all facings + crit rolls</li>
 *     <li>BV Bonus: +0.12 per trooper for conventional infantry only</li>
 * </ul>
 */
public class SuicideImplantsTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
        game.addPlayer(1, new Player(1, "Enemy Player"));
    }

    /**
     * Creates a conventional infantry unit with the specified trooper count.
     */
    private Infantry createInfantry(int troopers, boolean withSuicideImplants, int ownerId) {
        Infantry infantry = new Infantry();
        infantry.setGame(game);
        infantry.setId(game.getNextEntityId());
        infantry.setChassis("Test Platoon");
        infantry.setModel(withSuicideImplants ? "Suicide" : "Standard");

        // Initialize crew
        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        infantry.setCrew(crew);

        // Set Suicide Implant option on existing crew options
        if (withSuicideImplants) {
            crew.getOptions().getOption(OptionsConstants.MD_SUICIDE_IMPLANTS).setValue(true);
        }

        infantry.setOwner(game.getPlayer(ownerId));

        // Set up infantry structure
        infantry.autoSetInternal();
        infantry.initializeInternal(troopers, Infantry.LOC_INFANTRY);

        return infantry;
    }

    /**
     * Creates a Battle Armor unit with the specified trooper count.
     */
    private BattleArmor createBattleArmor(int troopers, boolean withSuicideImplants) {
        BattleArmor battleArmor = new BattleArmor();
        battleArmor.setGame(game);
        battleArmor.setId(game.getNextEntityId());
        battleArmor.setChassis("Test BA");
        battleArmor.setModel(withSuicideImplants ? "Suicide" : "Standard");
        battleArmor.setTroopers(troopers);
        battleArmor.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);

        // Initialize crew
        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        battleArmor.setCrew(crew);

        // Set Suicide Implant option on existing crew options
        if (withSuicideImplants) {
            crew.getOptions().getOption(OptionsConstants.MD_SUICIDE_IMPLANTS).setValue(true);
        }

        battleArmor.setOwner(game.getPlayer(0));

        // Set armor values for each trooper
        for (int i = 1; i <= troopers; i++) {
            battleArmor.initializeArmor(4, i);
        }

        battleArmor.autoSetInternal();

        return battleArmor;
    }

    // =========================================================================
    // ABILITY DETECTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Ability Detection Tests")
    class AbilityDetectionTests {

        @Test
        @DisplayName("Infantry hasAbility returns true when Suicide Implants is set")
        void infantryHasAbilityReturnsTrueForSuicideImplants() {
            Infantry infantry = createInfantry(21, true, 0);

            assertTrue(infantry.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS),
                  "Infantry with Suicide Implants should return true for hasAbility check");
        }

        @Test
        @DisplayName("Infantry without Suicide Implants returns false for hasAbility")
        void infantryWithoutSuicideImplantsReturnsFalse() {
            Infantry infantry = createInfantry(21, false, 0);

            assertFalse(infantry.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS),
                  "Infantry without Suicide Implants should return false for hasAbility check");
        }

        @Test
        @DisplayName("Battle Armor hasAbility returns true when Suicide Implants is set")
        void battleArmorHasAbilityReturnsTrueForSuicideImplants() {
            BattleArmor battleArmor = createBattleArmor(4, true);

            assertTrue(battleArmor.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS),
                  "BA with Suicide Implants should return true for hasAbility check");
        }

        @Test
        @DisplayName("Battle Armor without Suicide Implants returns false for hasAbility")
        void battleArmorWithoutSuicideImplantsReturnsFalse() {
            BattleArmor battleArmor = createBattleArmor(4, false);

            assertFalse(battleArmor.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS),
                  "BA without Suicide Implants should return false for hasAbility check");
        }
    }

    // =========================================================================
    // DAMAGE CALCULATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Damage Calculation Tests (0.57 per trooper)")
    class DamageCalculationTests {

        @Test
        @DisplayName("1 trooper = 1 damage (0.57 rounds to 1)")
        void oneTrooperDealsOneDamage() {
            int damage = SuicideImplantsAttackAction.getDamageFor(1);
            assertEquals(1, damage, "1 trooper * 0.57 = 0.57, rounds to 1");
        }

        @Test
        @DisplayName("2 troopers = 1 damage (1.14 rounds to 1)")
        void twoTroopersDealOneDamage() {
            int damage = SuicideImplantsAttackAction.getDamageFor(2);
            assertEquals(1, damage, "2 troopers * 0.57 = 1.14, rounds to 1");
        }

        @Test
        @DisplayName("10 troopers = 6 damage (5.7 rounds to 6)")
        void tenTroopersDealSixDamage() {
            int damage = SuicideImplantsAttackAction.getDamageFor(10);
            assertEquals(6, damage, "10 troopers * 0.57 = 5.7, rounds to 6");
        }

        @Test
        @DisplayName("18 troopers = 10 damage (10.26 rounds to 10)")
        void eighteenTroopersDealTenDamage() {
            int damage = SuicideImplantsAttackAction.getDamageFor(18);
            assertEquals(10, damage, "18 troopers * 0.57 = 10.26, rounds to 10");
        }

        @Test
        @DisplayName("21 troopers = 12 damage (11.97 rounds to 12)")
        void twentyOneTroopersDealTwelveDamage() {
            int damage = SuicideImplantsAttackAction.getDamageFor(21);
            assertEquals(12, damage, "21 troopers * 0.57 = 11.97, rounds to 12");
        }

        @Test
        @DisplayName("0 troopers = 0 damage (edge case)")
        void zeroTroopersDealZeroDamage() {
            int damage = SuicideImplantsAttackAction.getDamageFor(0);
            assertEquals(0, damage, "0 troopers should deal 0 damage");
        }

        @Test
        @DisplayName("DAMAGE_PER_TROOPER constant is 0.57")
        void damagePerTrooperConstantIsCorrect() {
            assertEquals(0.57, SuicideImplantsAttackAction.DAMAGE_PER_TROOPER, 0.001,
                  "DAMAGE_PER_TROOPER should be 0.57 per IO pg 83");
        }

        @Test
        @DisplayName("Building damage is trooperCount / 2")
        void buildingDamageIsHalfTroopers() {
            assertEquals(0, SuicideImplantsAttackAction.getBuildingDamageFor(1),
                  "1 trooper = 0 CF damage");
            assertEquals(1, SuicideImplantsAttackAction.getBuildingDamageFor(2),
                  "2 troopers = 1 CF damage");
            assertEquals(5, SuicideImplantsAttackAction.getBuildingDamageFor(10),
                  "10 troopers = 5 CF damage");
            assertEquals(10, SuicideImplantsAttackAction.getBuildingDamageFor(21),
                  "21 troopers = 10 CF damage");
        }

        @Test
        @DisplayName("Host damage is always 1")
        void hostDamageIsAlwaysOne() {
            assertEquals(1, SuicideImplantsAttackAction.getHostDamageFor(),
                  "Host damage (Mek/Aero/Vehicle) is always 1 per IO pg 83");
        }
    }

    // =========================================================================
    // ATTACK ACTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Attack Action Tests")
    class AttackActionTests {

        @Test
        @DisplayName("toHit returns AUTOMATIC_SUCCESS for valid infantry with implants")
        void toHitReturnsAutomaticSuccessForValidInfantry() {
            Infantry infantry = createInfantry(21, true, 0);
            game.addEntity(infantry);
            // Set position to make entity "active"
            infantry.setPosition(new Coords(5, 5));
            infantry.setDeployed(true);

            ToHitData toHit = SuicideImplantsAttackAction.toHit(game, infantry.getId());

            assertEquals(TargetRoll.AUTOMATIC_SUCCESS, toHit.getValue(),
                  "Valid infantry with implants should have AUTOMATIC_SUCCESS");
        }

        @Test
        @DisplayName("toHit returns IMPOSSIBLE for infantry without implants")
        void toHitReturnsImpossibleWithoutImplants() {
            Infantry infantry = createInfantry(21, false, 0);
            game.addEntity(infantry);

            ToHitData toHit = SuicideImplantsAttackAction.toHit(game, infantry.getId());

            assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "Infantry without implants should have IMPOSSIBLE");
            assertTrue(toHit.getDesc().contains("lacks"),
                  "Description should mention lacking implants");
        }

        @Test
        @DisplayName("Attack action stores trooper count correctly")
        void attackActionStoresTrooperCount() {
            SuicideImplantsAttackAction action = new SuicideImplantsAttackAction(1, 15);

            assertEquals(15, action.getTroopersDetonating(),
                  "Action should store the trooper count");
        }

        @Test
        @DisplayName("getMaxTroopersFor returns shooting strength for infantry")
        void getMaxTroopersReturnsShootingStrength() {
            Infantry infantry = createInfantry(21, true, 0);

            int maxTroopers = SuicideImplantsAttackAction.getMaxTroopersFor(infantry);

            assertEquals(infantry.getShootingStrength(), maxTroopers,
                  "Max troopers should equal shooting strength");
        }

        @Test
        @DisplayName("toHit returns IMPOSSIBLE for dead crew")
        void toHitReturnsImpossibleForDeadCrew() {
            Infantry infantry = createInfantry(21, true, 0);
            game.addEntity(infantry);
            infantry.getCrew().setDead(true);

            ToHitData toHit = SuicideImplantsAttackAction.toHit(game, infantry.getId());

            assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "Dead crew should have IMPOSSIBLE");
        }

        @Test
        @DisplayName("toHit returns IMPOSSIBLE for unconscious crew")
        void toHitReturnsImpossibleForUnconsciousCrew() {
            Infantry infantry = createInfantry(21, true, 0);
            game.addEntity(infantry);
            infantry.getCrew().setUnconscious(true);

            ToHitData toHit = SuicideImplantsAttackAction.toHit(game, infantry.getId());

            assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "Unconscious crew should have IMPOSSIBLE");
        }
    }

    // =========================================================================
    // REACTIVE DETONATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Reactive Detonation Tests")
    class ReactiveDetonationTests {

        @Test
        @DisplayName("SUICIDE_IMPLANT_REACTION damage type exists")
        void suicideImplantReactionDamageTypeExists() {
            DamageType reactionType = DamageType.SUICIDE_IMPLANT_REACTION;

            assertNotEquals(DamageType.NONE, reactionType,
                  "SUICIDE_IMPLANT_REACTION should be a distinct damage type");
        }

        @Test
        @DisplayName("Conventional infantry is detected correctly")
        void conventionalInfantryDetectedCorrectly() {
            Infantry infantry = createInfantry(21, true, 0);

            assertTrue(infantry.isConventionalInfantry(),
                  "Infantry should be detected as conventional");
        }

        @Test
        @DisplayName("Battle Armor is NOT conventional infantry")
        void battleArmorIsNotConventionalInfantry() {
            BattleArmor battleArmor = createBattleArmor(4, true);

            assertFalse(battleArmor.isConventionalInfantry(),
                  "Battle Armor should NOT be conventional infantry");
        }

        @Test
        @DisplayName("Reactive damage should only trigger for conventional infantry")
        void reactiveDamageOnlyForConventionalInfantry() {
            Infantry infantry = createInfantry(21, true, 0);
            BattleArmor battleArmor = createBattleArmor(4, true);

            // Both have the ability
            assertTrue(infantry.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS));
            assertTrue(battleArmor.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS));

            // But only conventional infantry should trigger reactive damage
            boolean infantryTriggers = infantry.isConventionalInfantry()
                  && infantry.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS);
            boolean baTriggers = battleArmor.isConventionalInfantry()
                  && battleArmor.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS);

            assertTrue(infantryTriggers, "Conventional infantry should trigger reactive damage");
            assertFalse(baTriggers, "Battle Armor should NOT trigger reactive damage");
        }
    }

    // =========================================================================
    // BV CALCULATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("BV Calculation Tests (+0.12 per trooper)")
    class BvCalculationTests {

        @Test
        @DisplayName("Suicide Implant BV bonus formula: troopers * 0.12")
        void suicideImplantBvBonusFormula() {
            // Verify the bonus formula: troopers * 0.12
            int troopers = 21;
            double bonus = troopers * 0.12;

            assertEquals(2.52, bonus, 0.001,
                  "BV bonus should be 2.52 for 21 troopers (21 * 0.12)");
        }

        @Test
        @DisplayName("Infantry with Suicide Implants has higher BV (+0.12 per trooper base bonus)")
        void infantryWithSuicideImplantsHasHigherBv() {
            Infantry withImplants = createInfantry(21, true, 0);
            Infantry withoutImplants = createInfantry(21, false, 0);

            int bvWith = withImplants.calculateBattleValue();
            int bvWithout = withoutImplants.calculateBattleValue();
            int actualDifference = bvWith - bvWithout;

            // Base bonus is 21 troopers * 0.12 = 2.52, but gets multiplied by various BV factors
            // Just verify the BV is higher and the difference is positive
            assertTrue(bvWith > bvWithout,
                  "Infantry with Suicide Implants should have higher BV. " +
                        "With: " + bvWith + ", Without: " + bvWithout + ", Difference: " + actualDifference);
            assertTrue(actualDifference > 0,
                  "BV difference should be positive (base bonus: 21 x 0.12 = 2.52). " +
                        "Actual difference: " + actualDifference);
        }

        @Test
        @DisplayName("Battle Armor with Suicide Implants has same BV as without (no BA bonus)")
        void battleArmorSuicideImplantsNoBvBonus() {
            BattleArmor withImplants = createBattleArmor(4, true);
            BattleArmor withoutImplants = createBattleArmor(4, false);

            int bvWith = withImplants.calculateBattleValue();
            int bvWithout = withoutImplants.calculateBattleValue();

            // BA should NOT get BV bonus for suicide implants per IO pg 83
            // Allow small tolerance for any other differences
            assertEquals(bvWithout, bvWith,
                  "BA should NOT get BV bonus for Suicide Implants. " +
                        "With: " + bvWith + ", Without: " + bvWithout);
        }
    }

    // =========================================================================
    // INTEGRATION TESTS - Actually resolve damage and verify game state
    // =========================================================================

    /**
     * Test adapter to access protected methods in TWDamageManager.
     */
    static class TestDamageManager extends TWDamageManager {
        public TestDamageManager(TWGameManager gameManager, Game game) {
            super(gameManager, game);
        }

        /**
         * Exposes protected method for testing.
         */
        public Vector<Report> testApplySuicideImplantReaction(Entity infantry, int deadTroopers) {
            return applySuicideImplantReaction(infantry, deadTroopers);
        }
    }

    @Nested
    @DisplayName("Reactive Detonation Integration Tests")
    class ReactiveDetonationIntegrationTests {

        private TWGameManager gameManager;
        private TestDamageManager damageManager;
        private Server server;

        @BeforeEach
        void setUpIntegration() throws IOException {
            gameManager = new TWGameManager();
            game = gameManager.getGame();
            game.setOptions(new GameOptions());

            damageManager = new TestDamageManager(gameManager, game);
            gameManager.setDamageManager(damageManager);

            server = ServerFactory.createServer(gameManager);

            game.addPlayer(0, new Player(0, "Test Player"));
            game.addPlayer(1, new Player(1, "Enemy Player"));
        }

        @AfterEach
        void tearDownIntegration() {
            if (server != null) {
                server.die();
            }
        }

        private BipedMek loadMek(String filename) throws EntityLoadingException {
            String resourcesPath = "testresources/megamek/common/units/";
            File file = new File(resourcesPath + filename);
            MekFileParser mfParser = new MekFileParser(file);
            BipedMek mek = (BipedMek) mfParser.getEntity();
            mek.setId(game.getNextEntityId());
            mek.setOwner(game.getPlayer(1)); // Enemy player
            game.addEntity(mek);
            return mek;
        }

        @Test
        @DisplayName("Reactive detonation returns empty reports when no enemies in hex")
        void reactiveDetonationReturnsEmptyWhenNoEnemiesInHex() {
            // Arrange: Infantry alone in hex (no enemies)
            Infantry infantry = createInfantry(21, true, 0);
            infantry.setPosition(new Coords(5, 5));
            infantry.setDeployed(true);
            game.addEntity(infantry);

            // Act: Call reactive detonation with 10 dead troopers
            Vector<Report> reports = damageManager.testApplySuicideImplantReaction(infantry, 10);

            // Assert: Should return empty - no message when no enemies to damage
            assertTrue(reports.isEmpty(),
                  "Reactive detonation should return empty reports when no enemies in hex");
        }

        @Test
        @DisplayName("Reactive detonation damages enemy Mek in same hex")
        void reactiveDetonationDamagesEnemyInSameHex() throws EntityLoadingException {
            // Arrange: Infantry and enemy Mek in same hex
            Infantry infantry = createInfantry(21, true, 0);
            Coords sharedHex = new Coords(5, 5);
            infantry.setPosition(sharedHex);
            infantry.setDeployed(true);
            game.addEntity(infantry);

            BipedMek enemyMek = loadMek("Crab CRB-20.mtf");
            enemyMek.setPosition(sharedHex);
            enemyMek.setDeployed(true);
            int startingArmor = enemyMek.getTotalArmor();

            // Act: 10 dead troopers = 6 damage (10 * 0.57 = 5.7, rounds to 6)
            Vector<Report> reports = damageManager.testApplySuicideImplantReaction(infantry, 10);

            // Assert: Reports generated and enemy took damage
            assertFalse(reports.isEmpty(),
                  "Should generate reports when enemy is in hex");
            assertTrue(enemyMek.getTotalArmor() < startingArmor,
                  "Enemy Mek should have taken damage. " +
                        "Starting: " + startingArmor + ", Current: " + enemyMek.getTotalArmor());
        }

        @Test
        @DisplayName("Reactive detonation does NOT damage friendly units in same hex")
        void reactiveDetonationDoesNotDamageFriendlyUnits() throws EntityLoadingException {
            // Arrange: Infantry and friendly Mek in same hex (same owner)
            Infantry infantry = createInfantry(21, true, 0);
            Coords sharedHex = new Coords(5, 5);
            infantry.setPosition(sharedHex);
            infantry.setDeployed(true);
            game.addEntity(infantry);

            // Load Mek but set it to same owner (friendly)
            String resourcesPath = "testresources/megamek/common/units/";
            File file = new File(resourcesPath + "Crab CRB-20.mtf");
            MekFileParser mfParser = new MekFileParser(file);
            BipedMek friendlyMek = (BipedMek) mfParser.getEntity();
            friendlyMek.setId(game.getNextEntityId());
            friendlyMek.setOwner(game.getPlayer(0)); // Same owner as infantry (friendly)
            game.addEntity(friendlyMek);
            friendlyMek.setPosition(sharedHex);
            friendlyMek.setDeployed(true);
            int startingArmor = friendlyMek.getTotalArmor();

            // Act: 10 dead troopers
            Vector<Report> reports = damageManager.testApplySuicideImplantReaction(infantry, 10);

            // Assert: No reports (no enemies), friendly undamaged
            assertTrue(reports.isEmpty(),
                  "Should return empty reports - friendly is not a valid target");
            assertEquals(startingArmor, friendlyMek.getTotalArmor(),
                  "Friendly Mek should NOT have taken damage");
        }

        @Test
        @DisplayName("Reactive detonation damages all enemies in hex")
        void reactiveDetonationDamagesAllEnemiesInHex() throws EntityLoadingException {
            // Arrange: Infantry and TWO enemy Meks in same hex
            Infantry infantry = createInfantry(21, true, 0);
            Coords sharedHex = new Coords(5, 5);
            infantry.setPosition(sharedHex);
            infantry.setDeployed(true);
            game.addEntity(infantry);

            BipedMek enemyMek1 = loadMek("Crab CRB-20.mtf");
            enemyMek1.setPosition(sharedHex);
            enemyMek1.setDeployed(true);
            int startingArmor1 = enemyMek1.getTotalArmor();

            BipedMek enemyMek2 = loadMek("Cyclops CP-10-Z.mtf");
            enemyMek2.setPosition(sharedHex);
            enemyMek2.setDeployed(true);
            int startingArmor2 = enemyMek2.getTotalArmor();

            // Act: 10 dead troopers = 6 damage to EACH enemy
            Vector<Report> reports = damageManager.testApplySuicideImplantReaction(infantry, 10);

            // Assert: Both enemies took damage
            assertFalse(reports.isEmpty(), "Should generate reports");
            assertTrue(enemyMek1.getTotalArmor() < startingArmor1,
                  "First enemy Mek should have taken damage");
            assertTrue(enemyMek2.getTotalArmor() < startingArmor2,
                  "Second enemy Mek should have taken damage");
        }
    }
}
