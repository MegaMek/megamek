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
package megamek.common.weapons;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.stream.Stream;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Ground ranges (Min/Short/Medium/Long) of the external ordnance missiles, per the TO:AuE weapon and equipment table.
 * The IS and Clan versions share the same ranges. The Clan LAA used to have no minimum range and 6/12/24 brackets.
 */
class BombMissileGroundRangesTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    static Stream<Arguments> bookRanges() {
        Stream.Builder<Arguments> rows = Stream.builder();
        for (String techBase : new String[] { "IS ", "Clan " }) {
            rows.add(Arguments.of(techBase + BombTypeEnum.AAA.getWeaponName(), 6, 12, 18, 24));
            rows.add(Arguments.of(techBase + BombTypeEnum.LAA.getWeaponName(), 7, 14, 21, 28));
            rows.add(Arguments.of(techBase + BombTypeEnum.AS.getWeaponName(), 9, 17, 25, 32));
            rows.add(Arguments.of(techBase + BombTypeEnum.ASEW.getWeaponName(), 7, 14, 21, 28));
        }
        return rows.build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bookRanges")
    void groundRangesMatchTheBook(String internalName, int minimum, int shortRange, int mediumRange,
          int longRange) {
        WeaponType weaponType = assertInstanceOf(WeaponType.class, EquipmentType.get(internalName), internalName);

        assertAll(internalName,
              () -> assertEquals(minimum, weaponType.getMinimumRange(), "minimum"),
              () -> assertEquals(shortRange, weaponType.getShortRange(), "short"),
              () -> assertEquals(mediumRange, weaponType.getMediumRange(), "medium"),
              () -> assertEquals(longRange, weaponType.getLongRange(), "long"));
    }
}
