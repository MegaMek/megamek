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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for anti-mek attack calculations in {@link Compute}.
 * <p>
 * These tests verify that leg attacks and swarm attacks correctly check the isBurdened() status for Battle Armor units
 * with body-mounted missiles.
 * <p>
 * Per TacOps rules, Inner Sphere BA with body-mounted missile launchers cannot make anti-mek attacks until the launcher
 * is jettisoned.
 */
class AntiMekAttackTest {

    private Game game;
    private GameOptions mockGameOptions;
    private Player player1;
    private Player player2;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        mockGameOptions = mock(GameOptions.class);
        game.setOptions(mockGameOptions);

        // Set up game options
        Option mockTrueBoolOpt = mock(Option.class);
        Option mockFalseBoolOpt = mock(Option.class);
        when(mockTrueBoolOpt.booleanValue()).thenReturn(true);
        when(mockFalseBoolOpt.booleanValue()).thenReturn(false);
        when(mockGameOptions.getOption(anyString())).thenReturn(mockFalseBoolOpt);
        when(mockGameOptions.booleanOption(eq(OptionsConstants.BASE_FRIENDLY_FIRE))).thenReturn(false);

        // Set up players
        player1 = new Player(0, "Attacker");
        player2 = new Player(1, "Defender");
        game.addPlayer(0, player1);
        game.addPlayer(1, player2);
    }

    /**
     * Creates an Inner Sphere Battle Armor unit. Uses spy to allow mocking isBurdened() for controlled test behavior.
     */
    private BattleArmor createISBattleArmor(int troopers, boolean burdened) {
        BattleArmor battleArmor = spy(new BattleArmor());
        battleArmor.setGame(game);
        battleArmor.setId(game.getNextEntityId());
        battleArmor.setChassis("Test IS BA");
        battleArmor.setModel(burdened ? "With Missiles" : "Standard");
        battleArmor.setTroopers(troopers);
        battleArmor.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        battleArmor.setTechLevel(TechConstants.T_IS_TW_NON_BOX);

        // Initialize crew with piloting skill
        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, 0);
        crew.setPiloting(5, 0);
        battleArmor.setCrew(crew);

        battleArmor.setOwner(player1);

        // Set armor values for each trooper location
        for (int i = 1; i <= troopers; i++) {
            battleArmor.initializeArmor(4, i);
        }
        battleArmor.autoSetInternal();

        // Mock isBurdened() to return the desired state
        when(battleArmor.isBurdened()).thenReturn(burdened);

        return battleArmor;
    }

    /**
     * Creates a Clan Battle Armor unit (not affected by isBurdened).
     */
    private BattleArmor createClanBattleArmor(int troopers) {
        BattleArmor battleArmor = spy(new BattleArmor());
        battleArmor.setGame(game);
        battleArmor.setId(game.getNextEntityId());
        battleArmor.setChassis("Test Clan BA");
        battleArmor.setModel("Elemental");
        battleArmor.setTroopers(troopers);
        battleArmor.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        battleArmor.setTechLevel(TechConstants.T_CLAN_TW);

        // Initialize crew with piloting skill
        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, 0);
        crew.setPiloting(5, 0);
        battleArmor.setCrew(crew);

        battleArmor.setOwner(player1);

        // Set armor values for each trooper location
        for (int i = 1; i <= troopers; i++) {
            battleArmor.initializeArmor(4, i);
        }
        battleArmor.autoSetInternal();

        // Clan BA is never burdened
        when(battleArmor.isBurdened()).thenReturn(false);

        return battleArmor;
    }

    /**
     * Creates a target Mek.
     */
    private Mek createTargetMek() {
        Mek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setChassis("Target");
        mek.setModel("Mek");
        mek.setWeight(50);

        Crew crew = new Crew(CrewType.SINGLE);
        mek.setCrew(crew);

        mek.setOwner(player2);
        mek.autoSetInternal();

        return mek;
    }

    /**
     * Creates conventional infantry.
     */
    private Infantry createConventionalInfantry(int troopers) {
        Infantry infantry = new Infantry();
        infantry.setGame(game);
        infantry.setId(game.getNextEntityId());
        infantry.setChassis("Test Platoon");
        infantry.setModel("Standard");

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, 0);
        crew.setPiloting(5, 0);
        infantry.setCrew(crew);

        infantry.setOwner(player1);
        infantry.autoSetInternal();
        infantry.initializeInternal(troopers, Infantry.LOC_INFANTRY);

        return infantry;
    }

    @Nested
    @DisplayName("isBurdened() Mock Verification")
    class IsBurdenedTests {

        @Test
        @DisplayName("Mock burdened BA returns true for isBurdened()")
        void mockedBurdenedBaReturnsTrue() {
            BattleArmor battleArmor = createISBattleArmor(4, true);

            assertTrue(battleArmor.isBurdened(),
                  "Mocked burdened BA should return true for isBurdened()");
        }

        @Test
        @DisplayName("Mock unburdened BA returns false for isBurdened()")
        void mockedUnburdenedBaReturnsFalse() {
            BattleArmor battleArmor = createISBattleArmor(4, false);

            assertFalse(battleArmor.isBurdened(),
                  "Mocked unburdened BA should return false for isBurdened()");
        }

        @Test
        @DisplayName("Clan BA always returns false for isBurdened()")
        void clanBaAlwaysUnburdened() {
            BattleArmor battleArmor = createClanBattleArmor(5);

            assertFalse(battleArmor.isBurdened(),
                  "Clan BA should never be burdened");
        }
    }

    @Nested
    @DisplayName("Leg Attack Tests")
    class LegAttackTests {

        @Test
        @DisplayName("IS BA without missiles can leg attack")
        void isBattleArmorWithoutMissilesCanLegAttack() {
            BattleArmor attacker = createISBattleArmor(4, false);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            ToHitData toHit = Compute.getLegAttackBaseToHit(attacker, defender, game);

            assertNotEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "IS BA without body-mounted missiles should be able to leg attack");
        }

        @Test
        @DisplayName("IS BA with body-mounted missiles cannot leg attack")
        void isBattleArmorWithMissilesCannotLegAttack() {
            BattleArmor attacker = createISBattleArmor(4, true);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            // Verify the BA is burdened first
            assertTrue(attacker.isBurdened(), "Test setup: BA should be burdened");

            ToHitData toHit = Compute.getLegAttackBaseToHit(attacker, defender, game);

            assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "IS BA with body-mounted missiles should not be able to leg attack");
            assertTrue(toHit.getDesc().contains("jettison"),
                  "Reason should mention launcher not jettisonned");
        }

        @Test
        @DisplayName("Clan BA can leg attack (never burdened)")
        void clanBattleArmorCanLegAttack() {
            BattleArmor attacker = createClanBattleArmor(5);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            // Verify Clan BA is not burdened
            assertFalse(attacker.isBurdened(), "Test setup: Clan BA should not be burdened");

            ToHitData toHit = Compute.getLegAttackBaseToHit(attacker, defender, game);

            assertNotEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "Clan BA should be able to leg attack (never burdened)");
        }

        @Test
        @DisplayName("Conventional infantry can leg attack")
        void conventionalInfantryCanLegAttack() {
            Infantry attacker = createConventionalInfantry(22);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            ToHitData toHit = Compute.getLegAttackBaseToHit(attacker, defender, game);

            assertNotEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "Conventional infantry with 22+ troopers should be able to leg attack");
        }
    }

    @Nested
    @DisplayName("Swarm Attack Tests")
    class SwarmAttackTests {

        @Test
        @DisplayName("IS BA without missiles can swarm attack")
        void isBattleArmorWithoutMissilesCanSwarmAttack() {
            BattleArmor attacker = createISBattleArmor(4, false);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            ToHitData toHit = Compute.getSwarmMekBaseToHit(attacker, defender, game);

            assertNotEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "IS BA without body-mounted missiles should be able to swarm attack");
        }

        @Test
        @DisplayName("IS BA with body-mounted missiles cannot swarm attack")
        void isBattleArmorWithMissilesCannotSwarmAttack() {
            BattleArmor attacker = createISBattleArmor(4, true);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            // Verify the BA is burdened first
            assertTrue(attacker.isBurdened(), "Test setup: BA should be burdened");

            ToHitData toHit = Compute.getSwarmMekBaseToHit(attacker, defender, game);

            assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "IS BA with body-mounted missiles should not be able to swarm attack");
            assertTrue(toHit.getDesc().contains("jettison"),
                  "Reason should mention launcher not jettisonned");
        }

        @Test
        @DisplayName("Clan BA can swarm attack (never burdened)")
        void clanBattleArmorCanSwarmAttack() {
            BattleArmor attacker = createClanBattleArmor(5);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            // Verify Clan BA is not burdened
            assertFalse(attacker.isBurdened(), "Test setup: Clan BA should not be burdened");

            ToHitData toHit = Compute.getSwarmMekBaseToHit(attacker, defender, game);

            assertNotEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "Clan BA should be able to swarm attack (never burdened)");
        }

        @Test
        @DisplayName("Conventional infantry can swarm attack")
        void conventionalInfantryCanSwarmAttack() {
            Infantry attacker = createConventionalInfantry(22);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            ToHitData toHit = Compute.getSwarmMekBaseToHit(attacker, defender, game);

            assertNotEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
                  "Conventional infantry with 22+ troopers should be able to swarm attack");
        }
    }

    @Nested
    @DisplayName("Consistency Tests - Leg and Swarm should behave identically")
    class ConsistencyTests {

        @Test
        @DisplayName("Burdened BA: both leg and swarm should be impossible")
        void burdenedBattleArmorBothAttacksImpossible() {
            BattleArmor attacker = createISBattleArmor(4, true);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            assertTrue(attacker.isBurdened(), "Test setup: BA should be burdened");

            ToHitData legAttack = Compute.getLegAttackBaseToHit(attacker, defender, game);
            ToHitData swarmAttack = Compute.getSwarmMekBaseToHit(attacker, defender, game);

            assertEquals(TargetRoll.IMPOSSIBLE, legAttack.getValue(),
                  "Leg attack should be impossible for burdened BA");
            assertEquals(TargetRoll.IMPOSSIBLE, swarmAttack.getValue(),
                  "Swarm attack should be impossible for burdened BA");
        }

        @Test
        @DisplayName("Unburdened BA: both leg and swarm should be possible")
        void unburdenedBattleArmorBothAttacksPossible() {
            BattleArmor attacker = createISBattleArmor(4, false);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            assertFalse(attacker.isBurdened(), "Test setup: BA should not be burdened");

            ToHitData legAttack = Compute.getLegAttackBaseToHit(attacker, defender, game);
            ToHitData swarmAttack = Compute.getSwarmMekBaseToHit(attacker, defender, game);

            assertNotEquals(TargetRoll.IMPOSSIBLE, legAttack.getValue(),
                  "Leg attack should be possible for unburdened BA");
            assertNotEquals(TargetRoll.IMPOSSIBLE, swarmAttack.getValue(),
                  "Swarm attack should be possible for unburdened BA");
        }

        @Test
        @DisplayName("Clan BA: both leg and swarm should be possible")
        void clanBattleArmorBothAttacksPossible() {
            BattleArmor attacker = createClanBattleArmor(5);
            Mek defender = createTargetMek();

            attacker.setPosition(new Coords(5, 5));
            defender.setPosition(new Coords(5, 5));

            game.addEntity(attacker);
            game.addEntity(defender);

            assertFalse(attacker.isBurdened(), "Test setup: Clan BA should not be burdened");

            ToHitData legAttack = Compute.getLegAttackBaseToHit(attacker, defender, game);
            ToHitData swarmAttack = Compute.getSwarmMekBaseToHit(attacker, defender, game);

            assertNotEquals(TargetRoll.IMPOSSIBLE, legAttack.getValue(),
                  "Leg attack should be possible for Clan BA");
            assertNotEquals(TargetRoll.IMPOSSIBLE, swarmAttack.getValue(),
                  "Swarm attack should be possible for Clan BA");
        }
    }
}
