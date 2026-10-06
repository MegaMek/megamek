/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Enumeration;
import java.util.Map;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WeaponTypeTest {

    private final Entity mockEntity = mock(Entity.class);

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testArtemisCompatibleFlag() {
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements(); ) {
            EquipmentType equipmentType = e.nextElement();
            if (equipmentType instanceof WeaponType weaponType) {
                AmmoTypeEnum ammoType = weaponType.getAmmoType();

                assertEquals(equipmentType.hasFlag(WeaponType.F_ARTEMIS_COMPATIBLE),
                      (ammoType == AmmoTypeEnum.LRM)
                            || (ammoType == AmmoTypeEnum.LRM_IMP)
                            || (ammoType == AmmoTypeEnum.MML)
                            || (ammoType == AmmoTypeEnum.SRM)
                            || (ammoType == AmmoTypeEnum.SRM_IMP)
                            || (ammoType == AmmoTypeEnum.NLRM)
                            || (ammoType == AmmoTypeEnum.LRM_TORPEDO)
                            || (ammoType == AmmoTypeEnum.SRM_TORPEDO)
                            || (ammoType == AmmoTypeEnum.NLRM_TORPEDO)
                            || (ammoType == AmmoTypeEnum.LRM_TORPEDO_COMBO)
                            || (ammoType == AmmoTypeEnum.EXLRM));
            }
        }
    }

    private WeaponMounted setupBayWeapon(String name) {
        EquipmentType etype = EquipmentType.get(name);
        WeaponMounted weapon = new WeaponMounted(mockEntity, (WeaponType) etype);
        WeaponMounted bWeapon = new WeaponMounted(mockEntity, (WeaponType) weapon.getType().getBayType());
        bWeapon.addWeaponToBay(mockEntity.getEquipmentNum(weapon));
        when(mockEntity.getWeapon(anyInt())).thenReturn(weapon);

        return bWeapon;
    }

    @Test
    void testWeaponBaysGetCorrectMaxRanges() {
        WeaponMounted ppcBay = setupBayWeapon("ISERPPC");
        WeaponType weaponType = ppcBay.getType();
        assertEquals(RangeType.RANGE_LONG, weaponType.getMaxRange(ppcBay));

        WeaponMounted clanERPulseLargeLaserBay = setupBayWeapon("CLERLargePulseLaser");
        weaponType = clanERPulseLargeLaserBay.getType();
        assertEquals(RangeType.RANGE_LONG, weaponType.getMaxRange(clanERPulseLargeLaserBay));

        WeaponMounted isLargePulseLaserBay = setupBayWeapon("ISLargePulseLaser");
        weaponType = isLargePulseLaserBay.getType();
        assertEquals(RangeType.RANGE_MEDIUM, weaponType.getMaxRange(isLargePulseLaserBay));

        WeaponMounted clanERSmallLaserBay = setupBayWeapon("CLERSmallLaser");
        weaponType = clanERSmallLaserBay.getType();
        assertEquals(RangeType.RANGE_SHORT, weaponType.getMaxRange(clanERSmallLaserBay));

        WeaponMounted isLightGaussRifleBay = setupBayWeapon("ISLightGaussRifle");
        weaponType = isLightGaussRifleBay.getType();
        assertEquals(RangeType.RANGE_EXTREME, weaponType.getMaxRange(isLightGaussRifleBay));
    }

    @Test
    void testVariableSpeedPulseLaserYamlHitModifiers() {
        EquipmentType equipmentType = EquipmentType.get("ISMediumVSPLaser");
        assertNotNull(equipmentType);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) equipmentType.getYamlData().get("stats");
        assertEquals(-3, equipmentType.getToHitModifier(null));
        assertArrayEquals(new int[] { -3, -2, -1 }, (int[]) stats.get("toHitModifier"));
    }

    @Test
    void testYamlExportsSparseAlphaStrikeWeaponProfile() {
        WeaponType erMicroLaser = (WeaponType) EquipmentType.get("CLBAERMicroLaser");
        WeaponType smallLaser = (WeaponType) EquipmentType.get("Small Laser");
        WeaponType mediumLaser = (WeaponType) EquipmentType.get("Medium Laser");
        WeaponType lrm = (WeaponType) EquipmentType.get("ISLRM5");
        WeaponType streakLrm = (WeaponType) EquipmentType.get("CLStreakLRM5");
        WeaponType iatm = (WeaponType) EquipmentType.get("CLIATM3");

        assertNotNull(erMicroLaser);
        assertNotNull(smallLaser);
        assertNotNull(mediumLaser);
        assertNotNull(lrm);
        assertNotNull(streakLrm);
        assertNotNull(iatm);

        Map<String, Object> erMicroLaserProfile = alphaStrikeYamlData(weaponYamlData(erMicroLaser));
        assertArrayEquals(new double[] { 0.2, 0, 0, 0 }, (double[]) erMicroLaserProfile.get("damage"));
        assertEquals(1, erMicroLaserProfile.size());

        Map<String, Object> smallLaserWeapon = weaponYamlData(smallLaser);
        Map<String, Object> smallLaserProfile = alphaStrikeYamlData(smallLaserWeapon);
        assertEquals(true, smallLaserProfile.get("pointDefense"));
        assertEquals(1, smallLaserProfile.size());

        assertEquals(null, weaponYamlData(mediumLaser).get("alphaStrike"));
        Map<String, Object> lrmProfile = alphaStrikeYamlData(weaponYamlData(lrm));
        assertEquals("LRM", lrmProfile.get("battleForceClass"));
        assertFalse(lrmProfile.containsKey("indirectFire"));
        Map<String, Object> iatmProfile = alphaStrikeYamlData(weaponYamlData(iatm));
        assertEquals("IATM", iatmProfile.get("battleForceClass"));
        assertEquals(false, iatmProfile.get("indirectFire"));

        Map<String, Object> streakLrmProfile = alphaStrikeYamlData(weaponYamlData(streakLrm));
        assertArrayEquals(new double[] { 0.5, 0.5, 0.5, 0 }, (double[]) streakLrmProfile.get("damage"));
    }

    @Test
    void testYamlExportsOnlyAlphaStrikeIndirectFireDifferences() {
        for (Enumeration<EquipmentType> types = EquipmentType.getAllTypes(); types.hasMoreElements(); ) {
            EquipmentType equipmentType = types.nextElement();
            if (!(equipmentType instanceof WeaponType weaponType)) {
                continue;
            }

            assertEquals(weaponType.hasFlag(WeaponType.F_INDIRECT_FIRE), weaponType.hasIndirectFire(),
                  weaponType.getInternalName());

            Map<String, Object> alphaStrike = alphaStrikeYamlData(weaponYamlData(weaponType));
            boolean differs = weaponType.isAlphaStrikeIndirectFire() != weaponType.hasIndirectFire();
            assertEquals(differs, alphaStrike != null && alphaStrike.containsKey("indirectFire"),
                  weaponType.getInternalName());
            if (differs) {
                assertEquals(weaponType.isAlphaStrikeIndirectFire(), alphaStrike.get("indirectFire"),
                      weaponType.getInternalName());
            }
        }
    }

    @Test
    void testYamlExportsAerospaceClassAsStringAndMissileArmor() {
        WeaponType mediumLaser = (WeaponType) EquipmentType.get("Medium Laser");
        WeaponType lbx = (WeaponType) EquipmentType.get("ISLBXAC10");
        WeaponType barracuda = (WeaponType) EquipmentType.get("Barracuda");

        assertNotNull(mediumLaser);
        assertNotNull(lbx);
        assertNotNull(barracuda);

        Map<String, Object> mediumLaserData = weaponYamlData(mediumLaser);
        assertEquals("LASER", mediumLaserData.get("atClass"));
        assertFalse(mediumLaserData.containsKey("missileArmor"));

        Map<String, Object> lbxData = weaponYamlData(lbx);
        assertEquals("LBX_AC", lbxData.get("atClass"));
        assertFalse(lbxData.get("atClass") instanceof Number);

        Map<String, Object> barracudaData = weaponYamlData(barracuda);
        assertEquals("CAPITAL_MISSILE", barracudaData.get("atClass"));
        assertEquals(20, barracudaData.get("missileArmor"));
    }

    @Test
    void testYamlOmitsDefaultAerospaceValues() {
        Map<String, Object> weaponData = weaponYamlData(new TestWeaponType(WeaponType.CLASS_NONE, 0));

        assertFalse(weaponData.containsKey("atClass"));
        assertFalse(weaponData.containsKey("missileArmor"));
    }

    @Test
    void testYamlRejectsUnknownAerospaceClass() {
        WeaponType weaponType = new TestWeaponType(Integer.MAX_VALUE, 0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, weaponType::getYamlData);
        assertEquals("Unknown AT class: " + Integer.MAX_VALUE, exception.getMessage());
    }

    @Test
    void testYamlRoundsAlphaStrikeDamageToThreeDecimalPlaces() {
        WeaponType weaponType = new TestAlphaStrikeDamageWeapon(
              1.7000000000000002, 0.06900000000000002, 1.2344, 1.2345);

        Map<String, Object> alphaStrike = alphaStrikeYamlData(weaponYamlData(weaponType));
        assertArrayEquals(new double[] { 1.7, 0.069, 1.234, 1.235 }, (double[]) alphaStrike.get("damage"));
    }

    @Test
    void testYamlRoundsAlphaStrikeDamageBoundariesWithoutNegativeZero() {
        WeaponType weaponType = new TestAlphaStrikeDamageWeapon(-0.0004, 0.0004, 0.0005, -0.0005);

        Map<String, Object> alphaStrike = alphaStrikeYamlData(weaponYamlData(weaponType));
        double[] damage = (double[]) alphaStrike.get("damage");
        assertArrayEquals(new double[] { 0, 0, 0.001, -0.001 }, damage);
        assertEquals(Double.doubleToLongBits(0.0), Double.doubleToLongBits(damage[0]));
    }

    @Test
    void testYamlExportsAlphaStrikeHeatDamageByRangeBand() {
        WeaponType plasmaRifle = (WeaponType) EquipmentType.get("ISPlasmaRifle");
        WeaponType battleArmorPlasmaRifle = (WeaponType) EquipmentType.get("ISBAPlasmaRifle");
        WeaponType plasmaCannon = (WeaponType) EquipmentType.get("CLPlasmaCannon");

        assertNotNull(plasmaRifle);
        assertNotNull(battleArmorPlasmaRifle);
        assertNotNull(plasmaCannon);
        assertArrayEquals(new int[] { 3, 3, 0, 0 }, heatDamage(plasmaRifle));
        assertArrayEquals(new int[] { 2, 2, 0, 0 }, heatDamage(battleArmorPlasmaRifle));
        assertArrayEquals(new int[] { 7, 7, 7, 0 }, heatDamage(plasmaCannon));
    }

    private int[] heatDamage(WeaponType weaponType) {
        return (int[]) alphaStrikeYamlData(weaponYamlData(weaponType)).get("heatDamage");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> weaponYamlData(WeaponType weaponType) {
        return (Map<String, Object>) weaponType.getYamlData().get("weapon");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> alphaStrikeYamlData(Map<String, Object> weaponYamlData) {
        return (Map<String, Object>) weaponYamlData.get("alphaStrike");
    }

    private static class TestWeaponType extends WeaponType {

        TestWeaponType(int atClass, int missileArmor) {
            setName("Test Weapon");
            ammoType = AmmoTypeEnum.NA;
            this.atClass = atClass;
            this.missileArmor = missileArmor;
        }
    }

    private static class TestAlphaStrikeDamageWeapon extends TestWeaponType {

        private final double[] damage;

        TestAlphaStrikeDamageWeapon(double shortDamage, double mediumDamage, double longDamage, double extremeDamage) {
            super(CLASS_NONE, 0);
            damage = new double[] { shortDamage, mediumDamage, longDamage, extremeDamage };
        }

        @Override
        public double getBattleForceDamage(int range, Mounted<?> fcs) {
            if (range == AlphaStrikeElement.SHORT_RANGE) {
                return damage[0];
            } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
                return damage[1];
            } else if (range == AlphaStrikeElement.LONG_RANGE) {
                return damage[2];
            }
            return damage[3];
        }
    }
}
