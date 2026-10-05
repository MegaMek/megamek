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

package megamek.common.util;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.ConvInfantry;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.infantry.support.srm.WithdrawnInfernoSrmLaunchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that a game saved before the Inferno SRM launchers were withdrawn (TechManual pp. 350-352 errata) still loads.
 * A platoon serializes its weapons inline, so such a save names the deleted launcher class and its internal name.
 */
class WithdrawnInfernoSrmSaveGameTest {

    private static final String PLAIN_LAUNCHER_CLASS =
          "megamek.common.weapons.infantry.support.srm.InfantrySupportSRMStandardWeapon";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    @DisplayName("a save naming the withdrawn Inferno launcher loads the plain launcher, still declared Inferno")
    void saveWithWithdrawnLauncherLoadsDeclaredInferno() {
        ConvInfantry platoon = new ConvInfantry();
        platoon.setPrimaryWeapon((InfantryWeapon) EquipmentType.get("Needler Rifle"));
        platoon.setSecondaryWeapon((InfantryWeapon) EquipmentType.get("InfantryStandardSRM"));
        String currentSave = SerializationHelper.getSaveGameXStream().toXML(platoon);

        // Rewrite the save as an older version wrote it: the deleted class and its internal name
        String olderSave = currentSave.replace(PLAIN_LAUNCHER_CLASS, WithdrawnInfernoSrmLaunchers.STANDARD_LAUNCHER_CLASS)
              .replace("<secondName>InfantryStandardSRM</secondName>",
                    "<secondName>InfantryStandardSRMInferno</secondName>");
        assertTrue(olderSave.contains(WithdrawnInfernoSrmLaunchers.STANDARD_LAUNCHER_CLASS),
              "The test save should name the deleted class: " + olderSave);

        ConvInfantry loaded = (ConvInfantry) SerializationHelper.getLoadSaveGameXStream().fromXML(olderSave);
        loaded.restore();

        assertSame(EquipmentType.get("InfantryStandardSRM"), loaded.getSecondaryWeapon(),
              "The withdrawn launcher should come back as the registered plain launcher");
        assertTrue(loaded.firesInfernoSrms(), "A platoon saved with the Inferno launcher should stay on Inferno");
    }
}
