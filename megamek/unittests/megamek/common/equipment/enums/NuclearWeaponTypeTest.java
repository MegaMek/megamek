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
package megamek.common.equipment.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Nuclear ammo maps to the weapon it carries.
 */
class NuclearWeaponTypeTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static AmmoType ammo(String internalName) {
        return (AmmoType) EquipmentType.get(internalName);
    }

    @Test
    void nuclearMissilesMapToTheirWeapon() {
        assertEquals(NuclearWeaponType.ALAMO, NuclearWeaponType.fromAmmo(ammo("Ammo Alamo")));
        assertEquals(NuclearWeaponType.SANTA_ANA, NuclearWeaponType.fromAmmo(ammo("Ammo Santa Anna")));
        assertEquals(NuclearWeaponType.SANTA_ANA, NuclearWeaponType.fromAmmo(ammo("Ammo AR10 Santa Anna")));
        assertEquals(NuclearWeaponType.PEACEMAKER, NuclearWeaponType.fromAmmo(ammo("Ammo Peacemaker")));
        assertEquals(NuclearWeaponType.PEACEMAKER, NuclearWeaponType.fromAmmo(ammo("Ammo AR10 Peacemaker")));
    }

    @Test
    void conventionalAmmoIsNotNuclear() {
        assertNull(NuclearWeaponType.fromAmmo(ammo("ISLRM20 Ammo")));
        assertNull(NuclearWeaponType.fromAmmo(null));
    }
}
