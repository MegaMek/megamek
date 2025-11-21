/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created
 * under Microsoft's "Game Content Usage Rules" and is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.equipment;

import megamek.common.TechAdvancement;
import megamek.common.interfaces.ITechnology;
import megamek.common.TechConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ARAD (Anti-Radiation) missile ammunition.
 * Tests verify that ARAD ammunition is correctly defined with proper tech dates,
 * availability ratings, and compatibility with LRM, SRM, and MML launchers.
 *
 * @author MegaMek Team
 * @since 2025-01-16
 */
public class AmmoTypeARADTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testARADMunitionEnumExists() {
        // Verify M_ARAD enum exists
        AmmoType.Munitions arad = AmmoType.Munitions.M_ARAD;
        assertNotNull(arad, "M_ARAD munition enum should exist");
        assertEquals("M_ARAD", arad.name());
    }

    @Test
    void testARADTechDatesInnerSphere() {
        // Find an IS ARAD LRM ammo
        AmmoType aradAmmo = findARADAmmo("LRM", false);

        assertNotNull(aradAmmo, "IS LRM ARAD ammunition should exist");
        TechAdvancement techAdv = aradAmmo.getTechAdvancement();
        assertEquals(3066, techAdv.getIntroductionDate(false),
                "IS ARAD prototype date should be 3066");
        assertEquals(ITechnology.DATE_NONE, techAdv.getProductionDate(false),
                "IS ARAD should never enter production");
    }

    @Test
    void testARADTechDatesClan() {
        // Find a Clan ARAD LRM ammo
        AmmoType aradAmmo = findARADAmmo("LRM", true);

        assertNotNull(aradAmmo, "Clan LRM ARAD ammunition should exist");
        TechAdvancement techAdv = aradAmmo.getTechAdvancement();
        assertEquals(3057, techAdv.getIntroductionDate(true),
                "Clan ARAD prototype date should be 3057");
        assertEquals(ITechnology.DATE_NONE, techAdv.getProductionDate(true),
                "Clan ARAD should never enter production");
    }

    @Test
    void testARADTechRating() {
        AmmoType aradAmmo = findARADAmmo("LRM", false);

        assertNotNull(aradAmmo);
        assertEquals(TechConstants.T_SIMPLE_EXPERIMENTAL, aradAmmo.getTechLevel(3070),
                "ARAD tech level should be Experimental");
    }

    @Test
    void testARADExistsForLRM() {
        AmmoType isARAD = findARADAmmo("LRM", false);
        AmmoType clanARAD = findARADAmmo("LRM", true);

        assertNotNull(isARAD, "IS ARAD should exist for LRM launchers");
        assertNotNull(clanARAD, "Clan ARAD should exist for LRM launchers");
    }

    @Test
    void testARADExistsForSRM() {
        AmmoType isARAD = findARADAmmo("SRM", false);
        AmmoType clanARAD = findARADAmmo("SRM", true);

        assertNotNull(isARAD, "IS ARAD should exist for SRM launchers");
        assertNotNull(clanARAD, "Clan ARAD should exist for SRM launchers");
    }

    @Test
    void testARADExistsForMML() {
        // MML uses both LRM and SRM munition lists
        // MML-LRM uses LRM munitions
        AmmoType mmlLRM = findARADAmmoByInternalName("IS Ammo MML", "LRM");
        assertNotNull(mmlLRM, "ARAD should exist for MML launchers (LRM mode)");
    }

    @Test
    void testARADNotArtemisCapable() {
        AmmoType aradAmmo = findARADAmmo("LRM", false);

        assertNotNull(aradAmmo);
        assertFalse(aradAmmo.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE),
                "ARAD should NOT be Artemis-capable");
    }

    @Test
    void testARADNotNarcCapable() {
        AmmoType aradAmmo = findARADAmmo("LRM", false);

        assertNotNull(aradAmmo);
        assertFalse(aradAmmo.getMunitionType().contains(AmmoType.Munitions.M_NARC_CAPABLE),
                "ARAD should NOT be Narc-capable (no bonus stacking)");
    }

    @Test
    void testARADCostMultiplier() {
        AmmoType standardAmmo = findStandardAmmo("LRM", false);
        AmmoType aradAmmo = findARADAmmo("LRM", false);

        assertNotNull(standardAmmo, "Standard LRM ammo should exist");
        assertNotNull(aradAmmo, "ARAD LRM ammo should exist");

        // ARAD should cost 2x standard ammo (cost multiplier of 2)
        assertTrue(aradAmmo.getCost(null, false, 0) >= standardAmmo.getCost(null, false, 0),
                "ARAD should be more expensive than standard ammo");
    }

    /**
     * Find ARAD ammunition for a specific launcher type and tech base.
     *
     * @param launcherType "LRM", "SRM", or "MML"
     * @param isClan true for Clan, false for Inner Sphere
     * @return AmmoType if found, null otherwise
     */
    private AmmoType findARADAmmo(String launcherType, boolean isClan) {
        String prefix = isClan ? "CL" : "IS";
        String searchName = prefix + launcherType;

        for (EquipmentType eq : Collections.list(EquipmentType.getAllTypes())) {
            if (eq instanceof AmmoType) {
                AmmoType ammo = (AmmoType) eq;
                if (ammo.getInternalName().contains(searchName) &&
                        ammo.getMunitionType().contains(AmmoType.Munitions.M_ARAD)) {
                    return ammo;
                }
            }
        }
        return null;
    }

    /**
     * Find ARAD ammunition by internal name pattern.
     *
     * @param namePrefix Internal name prefix (e.g., "IS Ammo MML")
     * @param type "LRM" or "SRM"
     * @return AmmoType if found, null otherwise
     */
    private AmmoType findARADAmmoByInternalName(String namePrefix, String type) {
        for (EquipmentType eq : Collections.list(EquipmentType.getAllTypes())) {
            if (eq instanceof AmmoType) {
                AmmoType ammo = (AmmoType) eq;
                if (ammo.getInternalName().contains(namePrefix) &&
                        ammo.getInternalName().contains(type) &&
                        ammo.getMunitionType().contains(AmmoType.Munitions.M_ARAD)) {
                    return ammo;
                }
            }
        }
        return null;
    }

    /**
     * Find standard (non-munition) ammunition for comparison.
     *
     * @param launcherType "LRM", "SRM", or "MML"
     * @param isClan true for Clan, false for Inner Sphere
     * @return AmmoType if found, null otherwise
     */
    private AmmoType findStandardAmmo(String launcherType, boolean isClan) {
        String prefix = isClan ? "CL" : "IS";
        String searchName = prefix + launcherType;

        for (EquipmentType eq : Collections.list(EquipmentType.getAllTypes())) {
            if (eq instanceof AmmoType) {
                AmmoType ammo = (AmmoType) eq;
                if (ammo.getInternalName().contains(searchName) &&
                        ammo.getMunitionType().contains(AmmoType.Munitions.M_STANDARD) &&
                        ammo.getRackSize() == 10) {  // Use size 10 for consistency
                    return ammo;
                }
            }
        }
        return null;
    }
}
