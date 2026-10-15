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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;

import megamek.common.enums.TechBase;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.enums.AmmoTypeFlag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks the battle armor missile munitions against Total Warfare p.229: SRMs may fire Standard, Inferno, Torpedo or
 * (Clan only) Multi-Purpose rounds; LRMs may fire Standard, Torpedo or (Clan only) Multi-Purpose rounds. No TO:AUE
 * missile munition is allowed (TO:AUE p.179).
 */
class BattleArmorMissileMunitionsTest {

    @BeforeAll
    static void setUpAll() {
        EquipmentType.initializeTypes();
    }

    private static List<AmmoType> allAmmoTypes() {
        List<AmmoType> ammoTypes = new ArrayList<>();
        for (Enumeration<EquipmentType> types = EquipmentType.getAllTypes(); types.hasMoreElements(); ) {
            if (types.nextElement() instanceof AmmoType ammoType) {
                ammoTypes.add(ammoType);
            }
        }
        return ammoTypes;
    }

    @Test
    void battleArmorSrmRoundsMatchTotalWarfare() {
        EnumSet<Munitions> allowed = EnumSet.of(Munitions.M_STANDARD, Munitions.M_INFERNO, Munitions.M_TORPEDO,
              Munitions.M_MULTI_PURPOSE);
        for (AmmoType ammoType : allAmmoTypes()) {
            if (ammoType.hasFlag(AmmoTypeFlag.F_BATTLEARMOR) && ammoType.getAmmoType() == AmmoTypeEnum.SRM) {
                assertTrue(allowed.containsAll(ammoType.getMunitionType()), ammoType.getInternalName());
            }
        }
    }

    /**
     * Regression test for #9225: Incendiary LRM variants were generated for battle armor LRM rounds.
     */
    @Test
    void battleArmorLrmRoundsMatchTotalWarfare() {
        EnumSet<Munitions> allowed = EnumSet.of(Munitions.M_STANDARD, Munitions.M_TORPEDO, Munitions.M_MULTI_PURPOSE);
        for (AmmoType ammoType : allAmmoTypes()) {
            if (ammoType.hasFlag(AmmoTypeFlag.F_BATTLEARMOR) && ammoType.getAmmoType() == AmmoTypeEnum.LRM) {
                assertTrue(allowed.containsAll(ammoType.getMunitionType()), ammoType.getInternalName());
            }
        }
    }

    @Test
    void multiPurposeRoundsAreClanBattleArmorOnly() {
        for (AmmoType ammoType : allAmmoTypes()) {
            if (ammoType.getMunitionType().contains(Munitions.M_MULTI_PURPOSE)) {
                assertTrue(ammoType.hasFlag(AmmoTypeFlag.F_BATTLEARMOR), ammoType.getInternalName());
                assertTrue(ammoType.getTechBase() == TechBase.CLAN, ammoType.getInternalName());
            }
        }
    }

    /**
     * ProtoMeks share BattleMek missile ammo; the forum ruling (topic 91104) limits them to the Total Warfare list.
     * ProtoMeks may not install Artemis IV (TechManual p.207), so Artemis-capable rounds are refused too.
     */
    @Test
    void protoMekMissileMunitionsFollowTotalWarfare() {
        EnumSet<Munitions> allowed = EnumSet.of(Munitions.M_STANDARD, Munitions.M_INFERNO, Munitions.M_MULTI_PURPOSE,
              Munitions.M_TORPEDO);
        boolean foundDeadFire = false;
        boolean foundArtemis = false;
        for (AmmoType ammoType : allAmmoTypes()) {
            if ((ammoType.getAmmoType() == AmmoTypeEnum.SRM) || (ammoType.getAmmoType() == AmmoTypeEnum.LRM)) {
                assertEquals(allowed.containsAll(ammoType.getMunitionType()),
                      AmmoType.isAllowedForBattleArmorAndProtoMeks(ammoType), ammoType.getInternalName());
                foundDeadFire |= ammoType.getMunitionType().contains(Munitions.M_DEAD_FIRE);
                foundArtemis |= ammoType.getMunitionType().contains(Munitions.M_ARTEMIS_CAPABLE);
            }
        }
        assertTrue(foundDeadFire, "expected BattleMek Dead-Fire SRM/LRM rounds to be checked");
        assertTrue(foundArtemis, "expected Artemis-capable SRM/LRM rounds to be checked");
        assertTrue(AmmoType.isAllowedForBattleArmorAndProtoMeks((AmmoType) EquipmentType.get("Clan Ammo SRM-2")));
    }
}
