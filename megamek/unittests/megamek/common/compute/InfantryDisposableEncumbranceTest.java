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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the ruling that an encumbering (1E) Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing) encumbers a
 * conventional infantry platoon like a secondary support weapon: it adds the point-blank support-weapon to-hit penalty
 * even when the platoon fires its normal weapon.
 */
class InfantryDisposableEncumbranceTest {

    private static final String DRAGONSBANE = "Pulse Laser (Dragonsbane Disposable)";
    private static final String NON_ENCUMBERING_PRIMARY = "InfantryAssaultRifle";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    @DisplayName("an encumbering Disposable Weapon is itself flagged encumbering")
    void dragonsbaneIsEncumbering() {
        InfantryWeapon dragonsbane = (InfantryWeapon) EquipmentType.get(DRAGONSBANE);
        assertEquals(true, dragonsbane.hasFlag(WeaponType.F_INF_ENCUMBER),
              "The Dragonsbane (1E) should carry the encumbering flag");
    }

    @Test
    @DisplayName("an encumbering Disposable Weapon adds the point-blank support-weapon penalty")
    void encumberingDisposableAddsPointBlankPenalty() {
        InfantryWeapon primary = (InfantryWeapon) EquipmentType.get(NON_ENCUMBERING_PRIMARY);
        InfantryWeapon dragonsbane = (InfantryWeapon) EquipmentType.get(DRAGONSBANE);

        int withoutDisposable = Compute.getInfantryRangeMods(0, primary, null, null, false).getValue();
        int withDisposable = Compute.getInfantryRangeMods(0, primary, null, dragonsbane, false).getValue();

        assertEquals(withoutDisposable + 1, withDisposable,
              "An encumbering Disposable Weapon should add +1 at point-blank range");
    }

    @Test
    @DisplayName("a non-encumbering Disposable Weapon adds no point-blank penalty")
    void nonEncumberingDisposableAddsNoPenalty() {
        InfantryWeapon primary = (InfantryWeapon) EquipmentType.get(NON_ENCUMBERING_PRIMARY);
        // Rocket Launcher (LAW) is disposable but not encumbering (crew 1, no F_INF_ENCUMBER)
        InfantryWeapon law = (InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)");

        int withoutDisposable = Compute.getInfantryRangeMods(0, primary, null, null, false).getValue();
        int withDisposable = Compute.getInfantryRangeMods(0, primary, null, law, false).getValue();

        assertEquals(withoutDisposable, withDisposable,
              "A non-encumbering Disposable Weapon should not change the point-blank modifier");
    }
}
