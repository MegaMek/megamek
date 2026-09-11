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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.TechConstants;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MekAutomaticClanCaseTest {
    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private static Mek mek(int chassisTech, String structure) {
        Mek mek = new BipedMek();
        mek.setWeight(50);
        mek.setTechLevel(chassisTech);
        mek.setMixedTech(true);
        mek.setStructureType(structure);
        return mek;
    }

    @Test
    void clanChassisDoesNotGrantCaseToInnerSphereStructure() throws Exception {
        Mek mek = mek(TechConstants.T_CLAN_EXPERIMENTAL, "IS Standard");
        mek.addEquipment(EquipmentType.get("IS Ammo MG - Full"), Mek.LOC_LEFT_TORSO);
        assertEquals(0, mek.implicitClanCASE());
        mek.addClanCase();
        assertFalse(mek.locationHasCase(Mek.LOC_LEFT_TORSO));
    }

    @Test
    void clanStructureGrantsCaseToInnerSphereChassis() throws Exception {
        Mek mek = mek(TechConstants.T_IS_EXPERIMENTAL, "Clan Endo Steel");
        mek.addEquipment(EquipmentType.get("IS Ammo MG - Full"), Mek.LOC_LEFT_TORSO);
        assertEquals(1, mek.implicitClanCASE());
        mek.addClanCase();
        assertTrue(mek.locationHasCase(Mek.LOC_LEFT_TORSO));
        assertEquals(0, mek.implicitClanCASE());
    }

    @Test
    void donorStructureRespectsExplicitCaseAndLocationOptOuts() throws Exception {
        Mek mek = mek(TechConstants.T_IS_EXPERIMENTAL, "IS Standard");
        mek.setFrankenMek(true);
        var clanStructure = EquipmentType.getStructureFromName(
              EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_ENDO_STEEL, true));
        for (int location : new int[] {Mek.LOC_LEFT_TORSO, Mek.LOC_RIGHT_TORSO, Mek.LOC_RIGHT_ARM}) {
            mek.setFrankenMekStructureType(location, clanStructure);
            mek.addEquipment(EquipmentType.get("IS Ammo MG - Full"), location);
        }
        mek.addEquipment(EquipmentType.get("IS Ammo MG - Full"), Mek.LOC_LEFT_ARM);
        mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.CLAN_CASE), Mek.LOC_LEFT_ARM);
        mek.addEquipment(EquipmentType.get("IS Ammo MG - Full"), Mek.LOC_LEFT_LEG);
        mek.addClanCaseOptOut(Mek.LOC_RIGHT_TORSO);
        assertEquals(2, mek.implicitClanCASE());
        mek.addClanCase();
        assertTrue(mek.locationHasCase(Mek.LOC_LEFT_TORSO));
        assertTrue(mek.locationHasCase(Mek.LOC_RIGHT_ARM));
        assertTrue(mek.locationHasCase(Mek.LOC_LEFT_ARM));
        assertFalse(mek.locationHasCase(Mek.LOC_RIGHT_TORSO));
        assertFalse(mek.locationHasCase(Mek.LOC_LEFT_LEG));
        assertEquals(0, mek.implicitClanCASE());
        long count = mek.getEquipment().stream().filter(m -> m.getType().getInternalName()
              .equals(EquipmentTypeLookup.CLAN_CASE)).count();
        mek.addClanCase();
        assertEquals(count, mek.getEquipment().stream().filter(m -> m.getType().getInternalName()
              .equals(EquipmentTypeLookup.CLAN_CASE)).count());
    }
}
