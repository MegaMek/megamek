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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.WeaponMounted;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link BaConstructionUtil#mountOnApm} marks a Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing)
 * as disposable. This is the shared mounting path used by both the MegaMek lobby and MegaMekLab, so it is what makes a
 * Battle Armor AP-mounted disposable weapon resolve with the disposable rules.
 */
class BaConstructionUtilDisposableTest {

    private BattleArmor battleArmor;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        battleArmor = new BattleArmor();
        battleArmor.setChassis("Test BA");
        battleArmor.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        battleArmor.setSquadSize(4);
        for (int trooper = 1; trooper <= 4; trooper++) {
            battleArmor.initializeArmor(4, trooper);
        }
        battleArmor.autoSetInternal();
    }

    private MiscMounted addApMount() throws Exception {
        MiscMounted apMount = (MiscMounted) battleArmor.addEquipment(
              EquipmentType.get(EquipmentTypeLookup.BA_APM), BattleArmor.LOC_SQUAD);
        apMount.setBaMountLoc(BattleArmor.MOUNT_LOC_BODY);
        return apMount;
    }

    @Test
    @DisplayName("mounting a Disposable Weapon in an AP mount marks it disposable")
    void disposableWeaponIsMarked() throws Exception {
        MiscMounted apMount = addApMount();
        WeaponMounted law = (WeaponMounted) battleArmor.addEquipment(
              EquipmentType.get("Rocket Launcher (LAW)"), BattleArmor.LOC_SQUAD);

        BaConstructionUtil.mountOnApm(law, apMount);

        assertTrue(law.isDisposableWeapon(), "An AP-mounted (1-D) weapon should be marked disposable");
    }

    @Test
    @DisplayName("mounting a normal AP weapon does not mark it disposable")
    void normalApWeaponIsNotMarked() throws Exception {
        MiscMounted apMount = addApMount();
        WeaponMounted autoRifle = (WeaponMounted) battleArmor.addEquipment(
              EquipmentType.get(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE), BattleArmor.LOC_SQUAD);

        BaConstructionUtil.mountOnApm(autoRifle, apMount);

        assertFalse(autoRifle.isDisposableWeapon(), "A normal AP weapon should not be marked disposable");
    }
}
