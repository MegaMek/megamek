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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import megamek.common.enums.TechRating;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
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
 * Tests for Damage Interrupt Circuit (DIC) functionality per IO p.39.
 * <p>
 * TableTop Rules:
 * <ul>
 *     <li>Working: Internal explosions cause 1 pilot damage instead of 2</li>
 *     <li>Protects all crew in multi-seat cockpits (Command Console, Tripod)</li>
 *     <li>Disabled by: Life Support critical hit OR any hit rolling "2" on hit location table</li>
 *     <li>When disabled: +1 to all PSR until repaired</li>
 *     <li>Available: IS only, BattleMeks and IndustrialMeks only</li>
 *     <li>Cost: 150 C-bills per pilot seat</li>
 * </ul>
 */
public class DamageInterruptCircuitTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    /**
     * Creates a BipedMek with the specified configuration.
     */
    private Mek createMek(boolean withDIC, boolean dicDisabled) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(1);
        mek.setChassis("Test Mek");
        mek.setModel(withDIC ? "DIC" : "Standard");
        mek.setWeight(50);

        // Initialize crew
        Crew crew = new Crew(CrewType.SINGLE);
        mek.setCrew(crew);
        mek.setOwner(game.getPlayer(0));

        // Add DIC equipment if requested
        if (withDIC) {
            try {
                EquipmentType dicType = EquipmentType.get("DamageInterruptCircuit");
                mek.addEquipment(dicType, Entity.LOC_NONE);
            } catch (Exception e) {
                throw new RuntimeException("Failed to add DIC equipment", e);
            }
            if (dicDisabled) {
                mek.setDICDisabled(true);
            }
        }

        return mek;
    }

    @Nested
    @DisplayName("Equipment Definition Tests")
    class EquipmentDefinitionTests {

        @Test
        @DisplayName("DIC equipment should exist and have correct properties")
        void dicEquipmentExists() {
            EquipmentType dicType = EquipmentType.get("DamageInterruptCircuit");

            assertNotNull(dicType, "DIC equipment should exist");
            assertTrue(dicType instanceof MiscType, "DIC should be MiscType");

            MiscType dicMisc = (MiscType) dicType;
            assertTrue(dicMisc.hasFlag(MiscType.F_DAMAGE_INTERRUPT_CIRCUIT),
                  "DIC should have F_DAMAGE_INTERRUPT_CIRCUIT flag");
            assertTrue(dicMisc.hasFlag(MiscType.F_MEK_EQUIPMENT),
                  "DIC should have F_MEK_EQUIPMENT flag");
            assertEquals(0, dicMisc.getTonnage(null), "DIC should have 0 tonnage");
            assertEquals(0, dicMisc.getBaseCriticalSlots(), "DIC should have 0 crits");
        }
    }

    @Nested
    @DisplayName("State Tracking Tests")
    class StateTrackingTests {

        @Test
        @DisplayName("Mek without DIC should report no DIC")
        void mekWithoutDIC() {
            Mek mek = createMek(false, false);

            assertFalse(mek.hasDamageInterruptCircuit(), "Mek without DIC should report no DIC");
            assertFalse(mek.hasWorkingDIC(), "Mek without DIC should not have working DIC");
        }

        @Test
        @DisplayName("Mek with working DIC should report working DIC")
        void mekWithWorkingDIC() {
            Mek mek = createMek(true, false);

            assertTrue(mek.hasDamageInterruptCircuit(), "Mek with DIC should report has DIC");
            assertFalse(mek.isDICDisabled(), "New DIC should not be disabled");
            assertTrue(mek.hasWorkingDIC(), "Mek with enabled DIC should have working DIC");
        }

        @Test
        @DisplayName("Mek with disabled DIC should report disabled DIC")
        void mekWithDisabledDIC() {
            Mek mek = createMek(true, true);

            assertTrue(mek.hasDamageInterruptCircuit(), "Mek should still have DIC installed");
            assertTrue(mek.isDICDisabled(), "DIC should be disabled");
            assertFalse(mek.hasWorkingDIC(), "Disabled DIC should not be working");
        }

        @Test
        @DisplayName("DIC disabled state can be toggled")
        void dicDisabledStateCanBeToggled() {
            Mek mek = createMek(true, false);

            assertFalse(mek.isDICDisabled(), "DIC should start enabled");

            mek.setDICDisabled(true);
            assertTrue(mek.isDICDisabled(), "DIC should be disabled after setDICDisabled(true)");
            assertFalse(mek.hasWorkingDIC(), "Disabled DIC should not be working");

            mek.setDICDisabled(false);
            assertFalse(mek.isDICDisabled(), "DIC should be enabled after setDICDisabled(false)");
            assertTrue(mek.hasWorkingDIC(), "Re-enabled DIC should be working");
        }
    }

    @Nested
    @DisplayName("PSR Modifier Tests")
    class PsrModifierTests {

        @Test
        @DisplayName("Disabled DIC should add +1 to PSR")
        void disabledDicAddsPsrModifier() {
            Mek mek = createMek(true, true);

            PilotingRollData rollWithDisabledDic = new PilotingRollData(mek.getId(), 0, "test");
            rollWithDisabledDic = mek.addEntityBonuses(rollWithDisabledDic);

            // Check that the DIC modifier is present
            boolean foundDicModifier = false;
            for (int i = 0; i < rollWithDisabledDic.getModifiers().size(); i++) {
                if (rollWithDisabledDic.getModifiers().get(i).description().contains("Damage Interrupt Circuit")) {
                    foundDicModifier = true;
                    assertEquals(1, rollWithDisabledDic.getModifiers().get(i).value(),
                          "DIC disabled modifier should be +1");
                    break;
                }
            }
            assertTrue(foundDicModifier, "Disabled DIC should add PSR modifier");
        }

        @Test
        @DisplayName("Working DIC should not add PSR modifier")
        void workingDicNoPsrModifier() {
            Mek mek = createMek(true, false);

            PilotingRollData rollWithWorkingDic = new PilotingRollData(mek.getId(), 0, "test");
            rollWithWorkingDic = mek.addEntityBonuses(rollWithWorkingDic);

            // Check that no DIC modifier is present
            for (int i = 0; i < rollWithWorkingDic.getModifiers().size(); i++) {
                assertFalse(rollWithWorkingDic.getModifiers().get(i).description().contains("Damage Interrupt Circuit"),
                      "Working DIC should not add PSR modifier");
            }
        }

        @Test
        @DisplayName("Mek without DIC should not add DIC PSR modifier")
        void noDicNoPsrModifier() {
            Mek mek = createMek(false, false);

            PilotingRollData roll = new PilotingRollData(mek.getId(), 0, "test");
            roll = mek.addEntityBonuses(roll);

            // Check that no DIC modifier is present
            for (int i = 0; i < roll.getModifiers().size(); i++) {
                assertFalse(roll.getModifiers().get(i).description().contains("Damage Interrupt Circuit"),
                      "Mek without DIC should not have DIC PSR modifier");
            }
        }
    }

    @Nested
    @DisplayName("TAC Detection Tests")
    class TacDetectionTests {

        private TWGameManager gameManager;
        private TWDamageManager damageManager;
        private Game testGame;
        private Server server;

        @BeforeEach
        void setUpDamageManager() throws IOException {
            gameManager = new TWGameManager();
            testGame = gameManager.getGame();
            testGame.setOptions(new GameOptions());
            damageManager = new TWDamageManager(gameManager, testGame);
            server = ServerFactory.createServer(gameManager);
            testGame.addPlayer(0, new Player(0, "Test Player"));
        }

        @AfterEach
        void tearDownServer() {
            if (server != null) {
                server.die();
            }
        }

        private Mek createMekForDamageTest(boolean withDIC) {
            BipedMek mek = new BipedMek();
            mek.setGame(testGame);
            mek.setId(testGame.getNextEntityId());
            mek.setChassis("Test Mek");
            mek.setModel(withDIC ? "DIC" : "Standard");
            mek.setWeight(50);

            // Initialize locations with armor
            for (int loc = 0; loc < mek.locations(); loc++) {
                mek.initializeArmor(20, loc);
                mek.initializeInternal(10, loc);
            }

            Crew crew = new Crew(CrewType.SINGLE);
            mek.setCrew(crew);
            mek.setOwner(testGame.getPlayer(0));
            testGame.addEntity(mek);

            if (withDIC) {
                try {
                    EquipmentType dicType = EquipmentType.get("DamageInterruptCircuit");
                    mek.addEquipment(dicType, Entity.LOC_NONE);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to add DIC equipment", e);
                }
            }

            return mek;
        }

        @Test
        @DisplayName("TAC (roll of 2) should disable DIC")
        void tacDisablesDic() {
            Mek mek = createMekForDamageTest(true);

            assertTrue(mek.hasWorkingDIC(), "DIC should start working");

            // Create hit with TAC effect (EFFECT_CRITICAL indicates roll of 2)
            HitData hit = new HitData(Mek.LOC_CENTER_TORSO);
            hit.setEffect(HitData.EFFECT_CRITICAL);

            DamageInfo damageInfo = new DamageInfo(mek, hit, 5);
            damageManager.damageEntity(damageInfo);

            assertTrue(mek.isDICDisabled(), "DIC should be disabled after TAC");
            assertFalse(mek.hasWorkingDIC(), "DIC should not be working after TAC");
        }

        @Test
        @DisplayName("Normal hit should not disable DIC")
        void normalHitDoesNotDisableDic() {
            Mek mek = createMekForDamageTest(true);

            assertTrue(mek.hasWorkingDIC(), "DIC should start working");

            // Create normal hit without TAC effect
            HitData hit = new HitData(Mek.LOC_CENTER_TORSO);
            // No EFFECT_CRITICAL set

            DamageInfo damageInfo = new DamageInfo(mek, hit, 5);
            damageManager.damageEntity(damageInfo);

            assertFalse(mek.isDICDisabled(), "DIC should remain enabled after normal hit");
            assertTrue(mek.hasWorkingDIC(), "DIC should still be working after normal hit");
        }

        @Test
        @DisplayName("TAC on Mek without DIC should not cause errors")
        void tacOnMekWithoutDic() {
            Mek mek = createMekForDamageTest(false);

            assertFalse(mek.hasDamageInterruptCircuit(), "Mek should not have DIC");

            // Create hit with TAC effect
            HitData hit = new HitData(Mek.LOC_CENTER_TORSO);
            hit.setEffect(HitData.EFFECT_CRITICAL);

            DamageInfo damageInfo = new DamageInfo(mek, hit, 5);
            // Should not throw any exceptions
            damageManager.damageEntity(damageInfo);

            assertFalse(mek.hasDamageInterruptCircuit(), "Mek still should not have DIC");
        }

        @Test
        @DisplayName("TAC on already disabled DIC should not cause errors")
        void tacOnAlreadyDisabledDic() {
            Mek mek = createMekForDamageTest(true);
            mek.setDICDisabled(true);

            assertTrue(mek.isDICDisabled(), "DIC should start disabled");

            // Create hit with TAC effect
            HitData hit = new HitData(Mek.LOC_CENTER_TORSO);
            hit.setEffect(HitData.EFFECT_CRITICAL);

            DamageInfo damageInfo = new DamageInfo(mek, hit, 5);
            // Should not throw any exceptions
            damageManager.damageEntity(damageInfo);

            assertTrue(mek.isDICDisabled(), "DIC should remain disabled");
        }
    }

    @Nested
    @DisplayName("Cost Calculation Tests")
    class CostCalculationTests {

        @Test
        @DisplayName("DIC base cost should be 150 C-bills")
        void dicBaseCost() {
            EquipmentType dicType = EquipmentType.get("DamageInterruptCircuit");
            assertNotNull(dicType, "DIC equipment should exist");

            // Per IO p.39: Cost is 150 C-bills per pilot seat
            // The base cost in MiscType is 150
            assertEquals(150, dicType.getRawCost(), 0.01, "DIC base cost should be 150 C-bills");
        }

        @Test
        @DisplayName("Single crew Mek should have 1 crew slot for DIC cost calculation")
        void singleCrewSlotCount() {
            Mek mek = createMek(true, false);

            // Single pilot = 1 crew slot
            assertEquals(1, mek.getCrew().getCrewType().getCrewSlots(),
                  "Single pilot mek should have 1 crew slot");
        }

        @Test
        @DisplayName("Command Console Mek should have 2 crew slots for DIC cost calculation")
        void commandConsoleCrewSlotCount() {
            BipedMek mek = new BipedMek();
            mek.setGame(game);
            mek.setId(1);
            mek.setChassis("Test Mek");
            mek.setModel("DIC Command");
            mek.setWeight(50);

            // Set up dual crew (Command Console has 2 crew slots)
            Crew crew = new Crew(CrewType.COMMAND_CONSOLE);
            mek.setCrew(crew);
            mek.setOwner(game.getPlayer(0));

            // Command Console = 2 crew slots, so DIC cost would be 300 C-bills
            assertEquals(2, mek.getCrew().getCrewType().getCrewSlots(),
                  "Command Console mek should have 2 crew slots");
        }

        @Test
        @DisplayName("hasDamageInterruptCircuit should return true for Mek with DIC")
        void hasDicReturnsTrueWithDic() {
            Mek mek = createMek(true, false);

            // This is the method used by MekCostCalculator to check for DIC
            assertTrue(mek.hasDamageInterruptCircuit(),
                  "hasDamageInterruptCircuit should return true for Mek with DIC");
        }

        @Test
        @DisplayName("hasDamageInterruptCircuit should return false for Mek without DIC")
        void hasDicReturnsFalseWithoutDic() {
            Mek mek = createMek(false, false);

            // This is the method used by MekCostCalculator to check for DIC
            assertFalse(mek.hasDamageInterruptCircuit(),
                  "hasDamageInterruptCircuit should return false for Mek without DIC");
        }
    }

    @Nested
    @DisplayName("Tech Restriction Tests")
    class TechRestrictionTests {

        @Test
        @DisplayName("DIC should be Inner Sphere tech base")
        void dicIsInnerSphereTech() {
            EquipmentType dicType = EquipmentType.get("DamageInterruptCircuit");
            assertNotNull(dicType, "DIC equipment should exist");

            // DIC should be IS-only, not available to Clan
            assertFalse(dicType.isClan(), "DIC should not be Clan tech");
        }

        @Test
        @DisplayName("DIC should be Mek-only equipment")
        void dicIsMekOnly() {
            EquipmentType dicType = EquipmentType.get("DamageInterruptCircuit");
            assertNotNull(dicType, "DIC equipment should exist");

            assertTrue(dicType instanceof MiscType, "DIC should be MiscType");
            MiscType dicMisc = (MiscType) dicType;

            assertTrue(dicMisc.hasFlag(MiscType.F_MEK_EQUIPMENT),
                  "DIC should have F_MEK_EQUIPMENT flag");
        }

        @Test
        @DisplayName("DIC should not be hittable")
        void dicIsNotHittable() {
            EquipmentType dicType = EquipmentType.get("DamageInterruptCircuit");
            assertNotNull(dicType, "DIC equipment should exist");

            assertTrue(dicType instanceof MiscType, "DIC should be MiscType");
            MiscType dicMisc = (MiscType) dicType;

            assertFalse(dicMisc.isHittable(), "DIC should not be hittable (0 crits, non-hittable equipment)");
        }

        @Test
        @DisplayName("DIC should have correct tech rating")
        void dicHasCorrectTechRating() {
            EquipmentType dicType = EquipmentType.get("DamageInterruptCircuit");
            assertNotNull(dicType, "DIC equipment should exist");

            // Per IO p.39: Tech Rating E
            assertEquals(TechRating.E, dicType.getTechRating(), "DIC should have tech rating E");
        }

        @Test
        @DisplayName("DIC should have correct rules reference")
        void dicHasCorrectRulesRef() {
            EquipmentType dicType = EquipmentType.get("DamageInterruptCircuit");
            assertNotNull(dicType, "DIC equipment should exist");

            assertTrue(dicType.getRulesRefs().contains("39, IO"),
                  "DIC should reference IO p.39");
        }
    }
}
