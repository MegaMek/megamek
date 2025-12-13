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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.TargetRoll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for MD_DERMAL_CAMO_ARMOR (Dermal Camo Armor) cybernetic implant functionality on Infantry units.
 */
class InfantryDermalCamoArmorTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /**
     * Creates an Infantry unit with the specified movement mode and optional abilities.
     */
    private Infantry createInfantry(EntityMovementMode movementMode, boolean hasDermalCamo, boolean hasTSM) {
        Infantry infantry = new Infantry();
        infantry.setId(1);
        infantry.setMovementMode(movementMode);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("Test Crew", 0);

        PilotOptions options = new PilotOptions();
        if (hasDermalCamo) {
            options.getOption(OptionsConstants.MD_DERMAL_CAMO_ARMOR).setValue(true);
        }
        if (hasTSM) {
            options.getOption(OptionsConstants.MD_TSM_IMPLANT).setValue(true);
        }
        crew.setOptions(options);
        infantry.setCrew(crew);

        return infantry;
    }

    /**
     * Adds an armor kit to the infantry unit.
     */
    private void addArmorKit(Infantry infantry, String armorKitName) throws LocationFullException {
        EquipmentType armorKit = EquipmentType.get(armorKitName);
        if (armorKit != null) {
            infantry.addEquipment(armorKit, Infantry.LOC_INFANTRY);
        }
    }

    @Nested
    @DisplayName("Damage Divisor Tests")
    class DamageDivisorTests {

        @Test
        @DisplayName("Base infantry without augmentations has divisor 1.0")
        void baseInfantryHasDivisorOne() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false, false);

            assertEquals(1.0, infantry.calcDamageDivisor(), 0.001);
        }

        @Test
        @DisplayName("TSM implant alone reduces divisor to 0.5")
        void tsmImplantReducesDivisor() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false, true);

            assertEquals(0.5, infantry.calcDamageDivisor(), 0.001);
        }

        @Test
        @DisplayName("Dermal Camo alone keeps divisor at 1.0")
        void dermalCamoAloneKeepsDivisorOne() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true, false);

            assertEquals(1.0, infantry.calcDamageDivisor(), 0.001);
        }

        @Test
        @DisplayName("Dermal Camo with TSM prevents 0.5 penalty, keeps divisor at 1.0")
        void dermalCamoWithTsmPreventsPenalty() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true, true);

            assertEquals(1.0, infantry.calcDamageDivisor(), 0.001);
        }
    }

    @Nested
    @DisplayName("hasDermalCamoStealth() Tests")
    class HasDermalCamoStealthTests {

        @Test
        @DisplayName("Leg infantry with Dermal Camo and no armor has stealth")
        void legInfantryWithDermalCamoHasStealth() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true, false);

            assertTrue(infantry.hasDermalCamoStealth());
        }

        @Test
        @DisplayName("Jump infantry with Dermal Camo and no armor has stealth")
        void jumpInfantryWithDermalCamoHasStealth() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_JUMP, true, false);

            assertTrue(infantry.hasDermalCamoStealth());
        }

        @Test
        @DisplayName("Motorized infantry with Dermal Camo does NOT have stealth")
        void motorizedInfantryWithDermalCamoNoStealth() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_MOTORIZED, true, false);

            assertFalse(infantry.hasDermalCamoStealth());
        }

        @Test
        @DisplayName("Wheeled infantry with Dermal Camo does NOT have stealth")
        void wheeledInfantryWithDermalCamoNoStealth() {
            Infantry infantry = createInfantry(EntityMovementMode.WHEELED, true, false);

            assertFalse(infantry.hasDermalCamoStealth());
        }

        @Test
        @DisplayName("Leg infantry with Dermal Camo wearing armor kit does NOT have stealth")
        void legInfantryWithArmorKitNoStealth() throws LocationFullException {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true, false);
            addArmorKit(infantry, "Generic Infantry Kit");

            assertFalse(infantry.hasDermalCamoStealth());
        }

        @Test
        @DisplayName("Leg infantry without Dermal Camo does NOT have stealth")
        void legInfantryWithoutDermalCamoNoStealth() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, false, false);

            assertFalse(infantry.hasDermalCamoStealth());
        }
    }

    @Nested
    @DisplayName("Stealth Modifier Tests")
    class StealthModifierTests {

        @Test
        @DisplayName("Stationary leg infantry with Dermal Camo gets +3 modifier")
        void stationaryInfantryGetsPlus3() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true, false);
            infantry.delta_distance = 0;

            TargetRoll result = infantry.getStealthModifier(0, null);

            assertNotNull(result);
            assertEquals(3, result.getValue());
        }

        @Test
        @DisplayName("Leg infantry moved 1 hex with Dermal Camo gets +2 modifier")
        void movedOneHexGetsPlus2() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true, false);
            infantry.delta_distance = 1;

            TargetRoll result = infantry.getStealthModifier(0, null);

            assertNotNull(result);
            assertEquals(2, result.getValue());
        }

        @Test
        @DisplayName("Leg infantry moved 2 hexes with Dermal Camo gets +1 modifier")
        void movedTwoHexesGetsPlus1() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true, false);
            infantry.delta_distance = 2;

            TargetRoll result = infantry.getStealthModifier(0, null);

            assertNotNull(result);
            assertEquals(1, result.getValue());
        }

        @Test
        @DisplayName("Leg infantry moved 3+ hexes with Dermal Camo gets no modifier")
        void movedThreeOrMoreHexesNoModifier() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true, false);
            infantry.delta_distance = 3;

            TargetRoll result = infantry.getStealthModifier(0, null);

            // When moved 3+ hexes, no stealth modifier applies
            assertNotNull(result);
            assertEquals(0, result.getValue());
        }

        @Test
        @DisplayName("Motorized infantry with Dermal Camo gets no stealth modifier")
        void motorizedInfantryNoStealthModifier() {
            Infantry infantry = createInfantry(EntityMovementMode.INF_MOTORIZED, true, false);
            infantry.delta_distance = 0;

            TargetRoll result = infantry.getStealthModifier(0, null);

            // Motorized infantry should not get Dermal Camo stealth
            assertNotNull(result);
            assertEquals(0, result.getValue());
        }

        @Test
        @DisplayName("Leg infantry with armor kit gets no Dermal Camo stealth modifier")
        void infantryWithArmorKitNoStealthModifier() throws LocationFullException {
            Infantry infantry = createInfantry(EntityMovementMode.INF_LEG, true, false);
            addArmorKit(infantry, "Generic Infantry Kit");
            infantry.delta_distance = 0;

            TargetRoll result = infantry.getStealthModifier(0, null);

            // Infantry with armor kit should not get Dermal Camo stealth
            assertNotNull(result);
            assertEquals(0, result.getValue());
        }
    }
}
