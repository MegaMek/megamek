/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for damage level calculations across entity types. Specifically tests that units with zero starting armor are
 * not incorrectly flagged as damaged when they have taken no actual damage.
 *
 * @see Entity#getDamageLevel()
 * @see Entity#isDmgHeavy()
 * @see Entity#isDmgModerate()
 * @see Entity#isDmgLight()
 */
class DamageLevelTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Nested
    @DisplayName("Zero Armor Units Should Not Show As Damaged")
    class ZeroArmorDamageLevelTests {

        @Test
        @DisplayName("AeroSpaceFighter with zero armor should return DMG_NONE")
        void aeroWithZeroArmorShouldNotBeDamaged() {
            // Arrange - create an AeroSpaceFighter with zero armor in all locations
            AeroSpaceFighter aero = new AeroSpaceFighter();
            initializeAeroWithZeroArmor(aero);

            // Act & Assert - should not be flagged as any damage level
            assertFalse(aero.isDmgLight(), "Zero armor aero should not be lightly damaged");
            assertFalse(aero.isDmgModerate(), "Zero armor aero should not be moderately damaged");
            assertFalse(aero.isDmgHeavy(), "Zero armor aero should not be heavily damaged");
            assertEquals(Entity.DMG_NONE, aero.getDamageLevel(false),
                  "Zero armor aero should have damage level DMG_NONE");
        }

        @Test
        @DisplayName("FixedWingSupport with zero armor should return DMG_NONE")
        void fixedWingSupportWithZeroArmorShouldNotBeDamaged() {
            // Arrange - create a FixedWingSupport with zero armor
            FixedWingSupport fixedWing = new FixedWingSupport();
            initializeFixedWingSupportWithZeroArmor(fixedWing);

            // Act & Assert - should not be flagged as any damage level
            assertFalse(fixedWing.isDmgLight(), "Zero armor fixed wing should not be lightly damaged");
            assertFalse(fixedWing.isDmgModerate(), "Zero armor fixed wing should not be moderately damaged");
            assertFalse(fixedWing.isDmgHeavy(), "Zero armor fixed wing should not be heavily damaged");
            assertEquals(Entity.DMG_NONE, fixedWing.getDamageLevel(false),
                  "Zero armor fixed wing should have damage level DMG_NONE");
        }

        @Test
        @DisplayName("Tank with zero armor should return DMG_NONE")
        void tankWithZeroArmorShouldNotBeDamaged() {
            // Arrange - create a Tank with zero armor in all locations
            Tank tank = new Tank();
            initializeTankWithZeroArmor(tank);

            // Act & Assert - should not be flagged as any damage level
            assertFalse(tank.isDmgLight(), "Zero armor tank should not be lightly damaged");
            assertFalse(tank.isDmgModerate(), "Zero armor tank should not be moderately damaged");
            assertFalse(tank.isDmgHeavy(), "Zero armor tank should not be heavily damaged");
            assertEquals(Entity.DMG_NONE, tank.getDamageLevel(false),
                  "Zero armor tank should have damage level DMG_NONE");
        }

        @Test
        @DisplayName("ConvFighter with zero armor should return DMG_NONE")
        void convFighterWithZeroArmorShouldNotBeDamaged() {
            // Arrange - create a ConvFighter with zero armor
            ConvFighter convFighter = new ConvFighter();
            initializeAeroWithZeroArmor(convFighter);

            // Act & Assert - should not be flagged as any damage level
            assertFalse(convFighter.isDmgLight(), "Zero armor conv fighter should not be lightly damaged");
            assertFalse(convFighter.isDmgModerate(), "Zero armor conv fighter should not be moderately damaged");
            assertFalse(convFighter.isDmgHeavy(), "Zero armor conv fighter should not be heavily damaged");
            assertEquals(Entity.DMG_NONE, convFighter.getDamageLevel(false),
                  "Zero armor conv fighter should have damage level DMG_NONE");
        }

        /**
         * Initializes an Aero with zero armor values.
         */
        private void initializeAeroWithZeroArmor(Aero aero) {
            // Set up internal structure first (required for valid entity)
            aero.initializeSI(10);

            // Set zero armor for all locations
            for (int loc = 0; loc < aero.locations(); loc++) {
                aero.initializeArmor(0, loc);
            }
        }

        /**
         * Initializes a FixedWingSupport with zero armor values.
         */
        private void initializeFixedWingSupportWithZeroArmor(FixedWingSupport fixedWing) {
            // Set up internal structure first
            fixedWing.initializeSI(5);

            // Set zero armor for all locations
            for (int loc = 0; loc < fixedWing.locations(); loc++) {
                fixedWing.initializeArmor(0, loc);
            }
        }

        /**
         * Initializes a Tank with zero armor values.
         */
        private void initializeTankWithZeroArmor(Tank tank) {
            // Set initial internal structure
            for (int loc = 0; loc < tank.locations(); loc++) {
                tank.initializeInternal(10, loc);
            }

            // Set zero armor for all locations
            for (int loc = 0; loc < tank.locations(); loc++) {
                tank.initializeArmor(0, loc);
            }

            // Set original walk MP to avoid division by zero in motive damage check
            tank.setOriginalWalkMP(4);
        }
    }

    @Nested
    @DisplayName("Damaged Units Should Show Correct Damage Level")
    class DamagedUnitTests {

        @Test
        @DisplayName("Aero with armor damage should show correct damage level")
        void aeroWithArmorDamageShouldShowDamage() {
            // Arrange - create an AeroSpaceFighter with normal armor
            AeroSpaceFighter aero = new AeroSpaceFighter();
            aero.initializeSI(10);

            // Give it some armor
            for (int loc = 0; loc < aero.locations(); loc++) {
                aero.initializeArmor(10, loc);
            }

            // Verify undamaged state first
            assertEquals(Entity.DMG_NONE, aero.getDamageLevel(false),
                  "Undamaged aero should have DMG_NONE");

            // Now damage it significantly (reduce armor to trigger heavy damage)
            for (int loc = 0; loc < aero.locations(); loc++) {
                aero.setArmor(1, loc); // ~10% armor remaining
            }

            // Act & Assert - should now show as damaged
            assertEquals(Entity.DMG_HEAVY, aero.getDamageLevel(false),
                  "Heavily damaged aero should have DMG_HEAVY");
        }

        @Test
        @DisplayName("Tank with armor damage should show correct damage level")
        void tankWithArmorDamageShouldShowDamage() {
            // Arrange - create a Tank with normal armor
            Tank tank = new Tank();
            tank.setOriginalWalkMP(4);

            for (int loc = 0; loc < tank.locations(); loc++) {
                tank.initializeInternal(10, loc);
                tank.initializeArmor(10, loc);
            }

            // Verify undamaged state first
            assertEquals(Entity.DMG_NONE, tank.getDamageLevel(false),
                  "Undamaged tank should have DMG_NONE");

            // Now damage it significantly
            for (int loc = 0; loc < tank.locations(); loc++) {
                tank.setArmor(1, loc); // ~10% armor remaining
            }

            // Act & Assert - should now show as damaged
            assertEquals(Entity.DMG_HEAVY, tank.getDamageLevel(false),
                  "Heavily damaged tank should have DMG_HEAVY");
        }
    }
}
