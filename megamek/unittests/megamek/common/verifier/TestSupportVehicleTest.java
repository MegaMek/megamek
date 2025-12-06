/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.verifier;

import static megamek.common.equipment.EquipmentType.T_ARMOR_FERRO_FIBROUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.enums.TechRating;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.SupportTank;
import megamek.common.util.RoundWeight;
import megamek.common.verifier.TestSupportVehicle.ChassisModification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class TestSupportVehicleTest {

    private static ArrayList<EntityMovementMode> svMovementModes = new ArrayList<>(List.of(EntityMovementMode.TRACKED,
          EntityMovementMode.HOVER, EntityMovementMode.WHEELED, EntityMovementMode.WIGE));

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testChassisModLookup() {
        for (ChassisModification mod : ChassisModification.values()) {
            assertNotNull(mod.equipment);
            assertTrue(mod.equipment.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT));
            assertTrue(mod.equipment.hasFlag(MiscType.F_CHASSIS_MODIFICATION));
        }
    }

    @Test
    void testBAR10ArmorCorrectSlots() {
        SupportTank st = new SupportTank();
        st.setArmorType(EquipmentType.T_ARMOR_SV_BAR_10);
        // Rating E should return CV slots for IS FF
        st.setArmorTechRating(TechRating.E);
        assertEquals(
              2,
              ArmorType.of(T_ARMOR_FERRO_FIBROUS, false).getSupportVeeSlots(st));

        // Rating F should return CV slots for Clan FF
        st.setArmorTechRating(TechRating.F);
        assertEquals(
              1,
              ArmorType.of(T_ARMOR_FERRO_FIBROUS, true).getSupportVeeSlots(st));
    }

    private static SupportTank createValidSupportTank(EntityMovementMode mode, double weight, String armor,
          TechRating techRating, double armorWeight, int engineRating, boolean armoredMod) throws
          LocationFullException {
        SupportTank st = new SupportTank();
        st.setMovementMode(mode);
        st.setWeight(weight);

        ArmorType armorType = (ArmorType) EquipmentType.get(armor);
        st.setArmorType(armorType.getArmorType());
        st.setBARRating(armorType.getBAR());
        st.setArmorTechRating(techRating);
        st.setArmorTonnage(armorWeight);

        int engineFlags = Engine.TANK_ENGINE | Engine.SUPPORT_VEE_ENGINE;
        st.setEngine(new Engine(engineRating, Engine.NORMAL_ENGINE, engineFlags));

        if (armoredMod) {
            st.addEquipment((MiscType) EquipmentType.get("Armored Chassis"), SupportTank.LOC_BODY);
            for (int location : List.of(1, 2, 3, 4, 5, 6)) {
                st.initializeArmor(1, location);
            }
        }

        return st;
    }

    private static List<SupportTank> createValidSupportTanks() throws LocationFullException {
        ArrayList<SupportTank> tanks = new ArrayList<>();

        for (EntityMovementMode mode : svMovementModes) {
            // All tech ratings support BAR 2
            for (TechRating rating : TechRating.values()) {
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 2 Armor", rating, 5.0, 250, false
                      )
                );
            }
            // A* - F support BAR 3 (*Armored Chassis only)
            for (TechRating rating : TechRating.values()) {
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 3 Armor", rating, 5.0, 250, rating.equals(TechRating.A)
                      )
                );
            }
            // B - F support BAR 4
            for (TechRating rating : List.of(TechRating.B, TechRating.C, TechRating.D, TechRating.E, TechRating.F)) {
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 4 Armor", rating, 5.0, 250, false
                      )
                );
            }
            // B* - F support BAR 5
            for (TechRating rating : List.of(TechRating.B, TechRating.C, TechRating.D, TechRating.E, TechRating.F)) {
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 5 Armor", rating, 5.0, 250, rating.equals(TechRating.B)
                      )
                );
            }
            // C - F support BAR 6
            for (TechRating rating : List.of(TechRating.C, TechRating.D, TechRating.E, TechRating.F)) {
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 6 Armor", rating, 5.0, 250, false
                      )
                );
            }
            // C* - F support BAR 7
            for (TechRating rating : List.of(TechRating.C, TechRating.D, TechRating.E, TechRating.F)) {
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 7 Armor", rating, 5.0, 250, rating.equals(TechRating.C)
                      )
                );
            }
            // D* - F support BAR 8
            for (TechRating rating : List.of(TechRating.D, TechRating.E, TechRating.F)) {
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 8 Armor", rating, 5.0, 250, rating.equals(TechRating.D)
                      )
                );
            }
            // D*, E*, F support BAR 9
            for (TechRating rating : List.of(TechRating.D, TechRating.E, TechRating.F)) {
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 9 Armor", rating, 5.0, 250, rating.equals(TechRating.D) || rating.equals(TechRating.E)
                      )
                );
            }
            // D*, E*, F* support BAR 10
            for (TechRating rating : List.of(TechRating.D, TechRating.E, TechRating.F)) {
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 10 Armor", rating, 5.0, 250, true
                      )
                );
            }
        }

        return tanks;
    }

    private static List<SupportTank> createInvalidSupportTanks() throws LocationFullException {
        ArrayList<SupportTank> tanks = new ArrayList<>();

        for (EntityMovementMode mode : svMovementModes) {
            for (boolean bool : List.of(true, false)) {
                // TL A Bar 4 is invalid with or without Armoured Chassis
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 4 Armor", TechRating.A, 5.0, 250, bool
                      )
                );
                // TL B Bar 6 is invalid with or without Armoured Chassis
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 6 Armor", TechRating.B, 5.0, 250, bool
                      )
                );
                // TL C Bar 8-10 is invalid with or without Armoured Chassis
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 8 Armor", TechRating.C, 5.0, 250, bool
                      )
                );
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 9 Armor", TechRating.C, 5.0, 250, bool
                      )
                );
                tanks.add(
                      createValidSupportTank(
                            mode, 50.0, "BAR 10 Armor", TechRating.C, 5.0, 250, bool
                      )
                );
            }
            // TL D BAR 8 without A.C. is invalid
            tanks.add(
                  createValidSupportTank(
                        mode, 50.0, "BAR 8 Armor", TechRating.D, 5.0, 250, false
                  )
            );
            // TL D, E BAR 9 without A.C. is invalid
            tanks.add(
                  createValidSupportTank(
                        mode, 50.0, "BAR 9 Armor", TechRating.D, 5.0, 250, false
                  )
            );
            tanks.add(
                  createValidSupportTank(
                        mode, 50.0, "BAR 9 Armor", TechRating.E, 5.0, 250, false
                  )
            );
            // TL D, E, F BAR 10 without A.C. is invalid
            tanks.add(
                  createValidSupportTank(
                        mode, 50.0, "BAR 10 Armor", TechRating.D, 5.0, 250, false
                  )
            );
            tanks.add(
                  createValidSupportTank(
                        mode, 50.0, "BAR 10 Armor", TechRating.E, 5.0, 250, false
                  )
            );
            tanks.add(
                  createValidSupportTank(
                        mode, 50.0, "BAR 10 Armor", TechRating.F, 5.0, 250, false
                  )
            );
        }

        return tanks;
    }

    private static Stream<Arguments> createValidSupportTanksStream() throws LocationFullException {
        List<SupportTank> list = createValidSupportTanks();
        return list.stream().map(t -> Arguments.of(Named.of(
              String.format("%.1f ton %s vee with TL %s BAR %s (%sA.C.)", t.getWeight(),
                    t.getMovementModeAsString(), t.getArmorTechRating().toString(),
                    t.getBARRating(0), (t.hasArmoredChassis() ? "*": "no ")), t))
        );
    }

    private static Stream<Arguments> createInvalidSupportTanksStream() throws LocationFullException {
        List<SupportTank> list = createInvalidSupportTanks();
        return list.stream().map(t -> Arguments.of(Named.of(
              String.format("%.1f ton %s vee with TL %s BAR %s (%sA.C.)", t.getWeight(),
                    t.getMovementModeAsString(), t.getArmorTechRating().toString(),
                    t.getBARRating(0), (t.hasArmoredChassis() ? "*": "no ")), t))
        );
    }

    @ParameterizedTest
    @MethodSource(value = "createValidSupportTanksStream")
    void test_valid_support_tank_armor(SupportTank supportTank) {
        StringBuffer sb = new StringBuffer();
        EntityVerifier entityVerifier = EntityVerifier.getInstance(new File(
              "testresources/data/mekfiles/UnitVerifierOptions.xml"));
        TestEntity testEntity = new TestSupportVehicle(supportTank, entityVerifier.tankOption, null);

        boolean result = testEntity.correctEntity(sb, supportTank.getTechLevel());
        assertTrue(sb.toString().isEmpty());
        assertTrue(result);
    }

    @ParameterizedTest
    @MethodSource(value = "createInvalidSupportTanksStream")
    void test_invalid_support_tank_armor(SupportTank supportTank) {
        StringBuffer sb = new StringBuffer();
        EntityVerifier entityVerifier = EntityVerifier.getInstance(new File(
              "testresources/data/mekfiles/UnitVerifierOptions.xml"));
        TestEntity testEntity = new TestSupportVehicle(supportTank, entityVerifier.tankOption, null);

        boolean result = testEntity.correctEntity(sb, supportTank.getTechLevel());
        assertFalse(sb.toString().isEmpty());
        assertFalse(result);
    }

    /**
     * Test for GitHub issue #7350: Support Vehicle Engine Weight Rounding
     *
     * Per TM p.133, support vehicle engine weight should round to the nearest half-ton
     * (for vehicles 5+ tons) or nearest kg (for small SVs), not round UP.
     *
     * Test case: 5-ton WiGE with 5/8 movement, ICE engine, tech rating D
     * - baseEngineValue = 0.005 (WiGE, 5+ tons)
     * - movementFactor = 4 + 5*5 = 29
     * - engineWeightMultiplier = 1.5 (ICE at tech rating D)
     * - raw weight = 0.005 * 29 * 1.5 * 5 = 1.0875 tons
     *
     * Expected: 1.0 ton (nearest half-ton)
     * Bug behavior: 1.5 tons (ceiling to next half-ton)
     */
    @Test
    @DisplayName("Issue #7350: SV engine weight rounds to nearest half-ton, not ceiling")
    void testSupportVehicleEngineWeightRoundsToNearestHalfTon() {
        // Create a 5-ton WiGE support vehicle
        SupportTank wige = new SupportTank();
        wige.setMovementMode(EntityMovementMode.WIGE);
        wige.setWeight(5.0);
        wige.setOriginalWalkMP(5); // 5/8 movement

        // Set up ICE engine with tech rating D
        int engineFlags = Engine.TANK_ENGINE | Engine.SUPPORT_VEE_ENGINE;
        Engine iceEngine = new Engine(0, Engine.COMBUSTION_ENGINE, engineFlags);
        wige.setEngine(iceEngine);
        wige.setEngineTechRating(TechRating.D);

        // Calculate expected raw weight: baseEngineValue * movementFactor * multiplier * tonnage
        // baseEngineValue for WiGE 5+ tons = 0.005
        // movementFactor = 4 + 5*5 = 29
        // ICE multiplier at tech rating D = 1.5
        // raw weight = 0.005 * 29 * 1.5 * 5.0 = 1.0875 tons
        double expectedRawWeight = 0.005 * 29 * 1.5 * 5.0;
        assertEquals(1.0875, expectedRawWeight, 0.0001, "Raw engine weight calculation");

        // Engine weight should round to nearest half-ton (1.0), not ceiling (1.5)
        double engineWeight = iceEngine.getWeightEngine(wige);
        assertEquals(1.0, engineWeight, 0.0001,
              "Engine weight should round to nearest half-ton per TM p.133");

        // Also verify that RoundWeight.SV_ENGINE gives the correct result
        assertEquals(1.0, RoundWeight.SV_ENGINE.round(expectedRawWeight, wige), 0.0001,
              "SV_ENGINE rounding should use nearest half-ton");

        // And verify NEXT_HALF_TON would give the wrong (old) result
        assertEquals(1.5, RoundWeight.NEXT_HALF_TON.round(expectedRawWeight, wige), 0.0001,
              "NEXT_HALF_TON (ceiling) would incorrectly give 1.5 tons");
    }
}
