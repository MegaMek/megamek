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

package megamek.common.weapons.infantry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import megamek.MMConstants;
import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers gaps found by auditing the TechManual infantry weapon tables (pp. 349-352) and the infantry weapon Battle
 * Value table (p. 319) against MegaMek.
 *
 * <p>The stat and BV data itself matched the tables, but two weapon classes existed without ever being registered
 * in {@code WeaponType.initializeTypes()}, so nothing could look them up and no unit could mount them. A third
 * problem was in the rules rather than the data: a platoon that gains Heavy Burst from the primary weapon damage
 * cap only received half of what that feature grants.</p>
 */
class InfantryWeaponTableGapsTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    @DisplayName("The non-incendiary mini grenade is available")
    void miniGrenadeIsRegistered() {
        EquipmentType grenade = EquipmentType.get("InfantryMiniGrenade");

        assertNotNull(grenade, "Grenade (Mini) (Non-Inferno) is a TechManual weapon and must be registered");
        assertInfantryStats(grenade, 0.27, 0);
    }

    @Test
    @DisplayName("The prosthetic rumal/garrote is available")
    void prostheticRumalGarroteIsRegistered() {
        EquipmentType garrote = EquipmentType.get("Prosthetic Rumal/Garrote");

        assertNotNull(garrote, "Twelve of the thirteen prosthetic weapons are registered; this one was omitted");
        assertInfantryStats(garrote, 0.14, 0);
    }

    private void assertInfantryStats(EquipmentType equipment, double expectedDamage, int expectedRange) {
        assertTrue(equipment instanceof InfantryWeapon, equipment.getName() + " should be an infantry weapon");
        InfantryWeapon weapon = (InfantryWeapon) equipment;
        assertEquals(expectedDamage, weapon.getInfantryDamage(), 0.001, "Damage per trooper");
        assertEquals(expectedRange, weapon.getInfantryRange(), "Base range");
    }

    @Test
    @DisplayName("Every weapon over the damage cap grants the Heavy Burst to-hit bonus at range 0")
    void everyWeaponOverTheDamageCapGrantsTheBonus() {
        // TM p. 152: a primary weapon above the cap has its damage reduced and the platoon "automatically gain[s]
        // the Heavy Burst Weapon special feature", which is a -1 to-hit at range 0 as well as +1D6 damage.
        //
        // Written against the data rather than one named weapon on purpose. Every weapon currently above the cap
        // also carries F_INF_BURST outright, so naming one would test the flag and not the rule, and would go
        // stale the moment that weapon's damage changed. This asserts the property the rule actually guarantees,
        // and fails for any weapon added above the cap without the flag.
        List<InfantryWeapon> overCap = new ArrayList<>();
        for (Enumeration<EquipmentType> types = EquipmentType.getAllTypes(); types.hasMoreElements(); ) {
            EquipmentType equipment = types.nextElement();
            if ((equipment instanceof InfantryWeapon weapon)
                  && !weapon.hasFlag(WeaponType.F_INF_SUPPORT)
                  && (weapon.getInfantryDamage() > MMConstants.INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP)) {
                overCap.add(weapon);
            }
        }
        assertFalse(overCap.isEmpty(), "There should be some infantry weapons above the primary damage cap");

        for (InfantryWeapon weapon : overCap) {
            ToHitData atRangeZero = Compute.getInfantryRangeMods(0, weapon, null, null, false);
            assertTrue(hasBurstBonus(atRangeZero),
                  weapon.getName() + " deals " + weapon.getInfantryDamage()
                        + ", above the primary weapon damage cap, so it must grant the Heavy Burst -1 at range 0; "
                        + "modifiers were " + atRangeZero.getDesc());
        }
    }

    @Test
    @DisplayName("A primary weapon under the damage cap gets no Heavy Burst bonus")
    void weaponUnderTheCapGetsNoBonus() {
        InfantryWeapon underCap = (InfantryWeapon) EquipmentType.get("InfantryAssaultRifle");
        assertNotNull(underCap, "The auto-rifle should be registered");
        assertTrue(underCap.getInfantryDamage() <= MMConstants.INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP,
              "This test needs a primary weapon at or below the cap");

        ToHitData atRangeZero = Compute.getInfantryRangeMods(0, underCap, null, null, false);

        assertTrue(!hasBurstBonus(atRangeZero),
              "A weapon under the cap gains no Heavy Burst feature; modifiers were " + atRangeZero.getDesc());
    }

    private boolean hasBurstBonus(ToHitData toHit) {
        return toHit.getDesc() != null && toHit.getDesc().contains("burst fire");
    }
}
