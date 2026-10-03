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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import megamek.common.RangeType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Regression tests for issues #2084 and #3948: aerospace range brackets of the external ordnance missiles.
 *
 * <p>The AAA, LAA, AS and ASEW missiles deal standard damage but use the capital range brackets (TO:AuE p. 169-171).
 * The Alamo is the reverse: capital damage at the standard range brackets (IO:AE p. 169).</p>
 */
class BombMissileRangeBracketsTest {

    private static final int[] CAPITAL_BRACKETS = { Integer.MIN_VALUE, 12, 24, 40, 50 };
    private static final int[] STANDARD_BRACKETS = { Integer.MIN_VALUE, 6, 12, 20, 25 };

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    static Stream<String> capitalRangeBombMissiles() {
        return Stream.of(
              "IS " + BombTypeEnum.AAA.getWeaponName(),
              "Clan " + BombTypeEnum.AAA.getWeaponName(),
              "IS " + BombTypeEnum.LAA.getWeaponName(),
              "Clan " + BombTypeEnum.LAA.getWeaponName(),
              "IS " + BombTypeEnum.AS.getWeaponName(),
              "Clan " + BombTypeEnum.AS.getWeaponName(),
              "IS " + BombTypeEnum.ASEW.getWeaponName(),
              "Clan " + BombTypeEnum.ASEW.getWeaponName());
    }

    private static WeaponType weaponByName(String internalName) {
        return assertInstanceOf(WeaponType.class, EquipmentType.get(internalName), internalName);
    }

    @ParameterizedTest
    @MethodSource("capitalRangeBombMissiles")
    void bombMissileUsesCapitalBracketsButStaysStandardDamage(String internalName) {
        WeaponType weaponType = weaponByName(internalName);

        assertArrayEquals(CAPITAL_BRACKETS, weaponType.getATRanges(), internalName);
        assertFalse(weaponType.isCapital(), internalName + " must keep standard-scale damage");
    }

    @Test
    void lightAirToAirMissileAtFifteenHexesIsMediumRange() {
        WeaponType lightAirToAir = weaponByName("IS " + BombTypeEnum.LAA.getWeaponName());

        // At standard brackets 15 hexes is Long, which is past the LAA's Medium maximum and so out of range
        int bracket = RangeType.rangeBracket(15, lightAirToAir.getATRanges(), true, false);

        assertEquals(RangeType.RANGE_MEDIUM, bracket);
        assertTrue(bracket <= lightAirToAir.getMaxRange());
        assertEquals(6, lightAirToAir.getRoundMedAV());
    }

    @Test
    void antiShipMissileReachesCapitalLongRange() {
        WeaponType antiShip = weaponByName("IS " + BombTypeEnum.AS.getWeaponName());

        int bracket = RangeType.rangeBracket(35, antiShip.getATRanges(), true, false);

        assertEquals(RangeType.RANGE_LONG, bracket);
        assertTrue(bracket <= antiShip.getMaxRange());
    }

    @Test
    void alamoUsesStandardBracketsButStaysCapitalDamage() {
        WeaponType alamo = weaponByName(BombTypeEnum.ALAMO.getWeaponName());

        assertArrayEquals(STANDARD_BRACKETS, alamo.getATRanges());
        assertTrue(alamo.isCapital(), "Alamo must keep capital-scale damage");
    }

    @Test
    void ordinaryThunderboltKeepsStandardBrackets() {
        assertArrayEquals(STANDARD_BRACKETS, weaponByName("Thunderbolt 5").getATRanges());
    }

    @Test
    void ordinaryCapitalMissileKeepsCapitalBrackets() {
        assertArrayEquals(CAPITAL_BRACKETS,
              weaponByName("Capital Missile Launcher (Killer Whale)").getATRanges());
    }
}
