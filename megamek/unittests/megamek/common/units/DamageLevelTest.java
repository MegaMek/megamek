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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
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
            aero.setOSI(10);

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
            fixedWing.setOSI(5);

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
            aero.setOSI(10);

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

    /**
     * Regression tests for issue #8381: bay-mounted craft (DropShips, etc.) were incorrectly reported as lightly
     * damaged at full health. A weapon bay never links its ammunition through {@code getLinked()} - the ammo lives in
     * the bay's member weapons - so {@code Mounted#isCrippled()} always flagged a fully-loaded ammo bay as crippled,
     * inflating the inoperable-weapon ratio used by {@link Aero#isDmgLight()}.
     *
     * @see megamek.common.equipment.Mounted#isCrippled()
     */
    @Nested
    @DisplayName("Weapon Bays Should Not Inflate Damage Level")
    class WeaponBayDamageLevelTests {

        private static final String LRM20_NAME = "ISLRM20";
        private static final String LRM20_AMMO_NAME = "IS Ammo LRM-20";

        @Test
        @DisplayName("DropShip with a fully-loaded ammo bay should return DMG_NONE")
        void loadedAmmoBayShouldNotBeDamaged() throws LocationFullException {
            Dropship dropship = buildDropshipWithLrmBay();

            WeaponMounted bay = dropship.getWeaponBayList().get(0);
            assertFalse(bay.isCrippled(), "A fully-loaded weapon bay should not be crippled");
            assertEquals(Entity.DMG_NONE, dropship.getDamageLevel(false),
                  "An undamaged, fully-loaded DropShip should have damage level DMG_NONE");
        }

        @Test
        @DisplayName("DropShip whose bay weapons are all out of ammo should report the bay as crippled")
        void emptyAmmoBayShouldBeCrippled() throws LocationFullException {
            Dropship dropship = buildDropshipWithLrmBay();

            // Empty every ammo bin so the bay's member weapons are genuinely out of ammunition.
            for (AmmoMounted ammo : dropship.getAmmo()) {
                ammo.setShotsLeft(0);
            }

            WeaponMounted bay = dropship.getWeaponBayList().get(0);
            assertTrue(bay.isCrippled(), "A weapon bay whose every weapon is out of ammo should be crippled");
            assertEquals(Entity.DMG_CRIPPLED, dropship.getDamageLevel(false),
                  "A DropShip whose only weapons are all out of ammo should be crippled");
        }

        /**
         * Builds a minimal, undamaged DropShip carrying a single LRM 20 bay (one launcher plus a full ammo bin) in the
         * nose, with full armor and structural integrity.
         */
        private Dropship buildDropshipWithLrmBay() throws LocationFullException {
            Dropship dropship = new Dropship();
            dropship.setOSI(30);
            for (int loc = 0; loc < dropship.locations(); loc++) {
                dropship.initializeArmor(40, loc);
            }

            WeaponType lrm20Type = (WeaponType) EquipmentType.get(LRM20_NAME);
            AmmoType lrm20AmmoType = (AmmoType) EquipmentType.get(LRM20_AMMO_NAME);

            WeaponMounted launcher = (WeaponMounted) dropship.addEquipment(lrm20Type, Aero.LOC_NOSE);
            WeaponMounted bay = (WeaponMounted) dropship.addEquipment(lrm20Type.getBayType(), Aero.LOC_NOSE);
            AmmoMounted ammo = (AmmoMounted) dropship.addEquipment(lrm20AmmoType, Aero.LOC_NOSE);

            bay.addWeaponToBay(launcher);
            bay.addAmmoToBay(ammo);
            launcher.setLinked(ammo);

            dropship.initMilitary();
            return dropship;
        }
    }
}
