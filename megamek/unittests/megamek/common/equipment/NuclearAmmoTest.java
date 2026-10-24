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
package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.generator.TeamLoadOutGenerator;
import megamek.common.SimpleTechLevel;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Nuclear ammo is what the nuclear weapons game option controls. Davy Crockett-M rounds count as nuclear even though
 * they are a munition of ordinary Long Tom ammo and carry no nuclear flag.
 */
class NuclearAmmoTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static AmmoType longTomMunition(Munitions munition) {
        for (AmmoType ammoType : AmmoType.getMunitionsFor(AmmoTypeEnum.LONG_TOM)) {
            if (ammoType.getMunitionType().contains(munition)) {
                return ammoType;
            }
        }
        return null;
    }

    private static boolean legalForLoadOut(AmmoType ammoType, boolean allowNukes) {
        return TeamLoadOutGenerator.checkLegality(ammoType, "FS", "IS", true, 3150, SimpleTechLevel.UNOFFICIAL,
              false, true, allowNukes);
    }

    @Test
    void davyCrockettRoundsAreNuclear() {
        AmmoType davyCrockett = longTomMunition(Munitions.M_DAVY_CROCKETT_M);
        assertNotNull(davyCrockett, "Long Tom has a Davy Crockett-M munition");
        assertFalse(davyCrockett.hasFlag(AmmoType.F_NUCLEAR), "the round carries no nuclear flag");
        assertTrue(davyCrockett.isNuclear());
    }

    @Test
    void nuclearCapitalMissilesAreNuclear() {
        assertTrue(((AmmoType) EquipmentType.get("Ammo Alamo")).isNuclear());
    }

    @Test
    void standardArtilleryIsNotNuclear() {
        assertFalse(longTomMunition(Munitions.M_STANDARD).isNuclear());
    }

    @Test
    void loadOutGeneratorLeavesDavyCrockettOutWhenNukesAreOff() {
        AmmoType davyCrockett = longTomMunition(Munitions.M_DAVY_CROCKETT_M);
        assertFalse(legalForLoadOut(davyCrockett, false), "nuclear weapons off: not loaded");
        assertTrue(legalForLoadOut(davyCrockett, true), "nuclear weapons on: may be loaded");
    }
}
