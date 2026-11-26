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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Display-name lookups go through an index instead of scanning every equipment type; it must still return the first
 * type registered with the name.
 */
class EquipmentTypeDisplayNameLookupTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void everyDisplayNameFindsTheSameTypeAsAScan() {
        for (EquipmentType type : EquipmentType.allTypes()) {
            String key = type.getName().toLowerCase(Locale.ROOT);
            EquipmentType expected = EquipmentType.lookupHash.get(key);
            if (expected == null) {
                for (EquipmentType candidate : EquipmentType.allTypes()) {
                    if (candidate.getName().equalsIgnoreCase(key)) {
                        expected = candidate;
                        break;
                    }
                }
            }
            assertSame(expected, EquipmentType.get(type.getName()), type.getName());
        }
    }

    @Test
    void namesThatAreNotEquipmentAreNotFound() {
        assertNull(EquipmentType.get("-Empty-"));
        assertNull(EquipmentType.get("Zzq not an equipment name"));
    }
}
