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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the Inner Sphere Follow The Leader missile ammunition, which was lost with the fall of the Star
 * League and recovered by the Federated Suns and the Lyran Commonwealth in 3046 (Interstellar Operations: Alternate
 * Eras, 3rd printing, Heavy Weapons Ammunition table, p. 55).
 *
 * @see <a href="https://github.com/MegaMek/megamek/issues/8687">MegaMek issue #8687</a>
 */
class FollowTheLeaderAmmoTechAdvancementTest {

    private static final int REINTRODUCTION_YEAR = 3046;

    private static List<AmmoType> innerSphereFollowTheLeaderAmmo;

    @BeforeAll
    static void collectInnerSphereFollowTheLeaderAmmo() {
        EquipmentType.initializeTypes();
        innerSphereFollowTheLeaderAmmo = EquipmentType.allTypes()
              .stream()
              .filter(AmmoType.class::isInstance)
              .map(AmmoType.class::cast)
              .filter(ammoType -> ammoType.getMunitionType().contains(AmmoType.Munitions.M_FOLLOW_THE_LEADER))
              .filter(ammoType -> ammoType.getTechBase() == TechBase.IS)
              .toList();
    }

    @Test
    void innerSphereFollowTheLeaderAmmoExists() {
        assertFalse(innerSphereFollowTheLeaderAmmo.isEmpty(),
              "No Inner Sphere Follow The Leader ammunition was created at all");
    }

    @Test
    void innerSphereFollowTheLeaderAmmoIsReintroducedIn3046() {
        for (AmmoType ammoType : innerSphereFollowTheLeaderAmmo) {
            assertEquals(REINTRODUCTION_YEAR,
                  ammoType.getReintroductionDate(false),
                  ammoType.getName() + " should be reintroduced in " + REINTRODUCTION_YEAR);
        }
    }

    /**
     * The Federated Suns and the Lyran Commonwealth recover the munition in 3046; every other Inner Sphere faction has
     * to wait the standard ten years, so it is still extinct for them in 3050.
     */
    @Test
    void followTheLeaderAmmoIsAvailableToItsReintroductionFactionsFirst() {
        for (AmmoType ammoType : innerSphereFollowTheLeaderAmmo) {
            assertFalse(ammoType.isExtinct(3050, false, Faction.FS),
                  ammoType.getName() + " should be available to the Federated Suns in 3050");
            assertFalse(ammoType.isExtinct(3050, false, Faction.LC),
                  ammoType.getName() + " should be available to the Lyran Commonwealth in 3050");
            assertTrue(ammoType.isExtinct(3050, false, Faction.DC),
                  ammoType.getName() + " should still be extinct for the Draconis Combine in 3050");
            assertFalse(ammoType.isExtinct(3060, false, Faction.DC),
                  ammoType.getName() + " should be available to the Draconis Combine in 3060");
        }
    }

    /**
     * Before the reintroduction date was recorded, the munition was extinct forever after 2770 - the bug reported in
     * issue #8687.
     */
    @Test
    void followTheLeaderAmmoIsNotPermanentlyExtinct() {
        for (AmmoType ammoType : innerSphereFollowTheLeaderAmmo) {
            assertTrue(ammoType.isExtinct(3000, false),
                  ammoType.getName() + " should be extinct in 3000, before its reintroduction");
            assertFalse(ammoType.isExtinct(3050, false),
                  ammoType.getName() + " should no longer be extinct in 3050");
        }
    }
}
