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

package megamek.common.weapons.infantry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Enumeration;

import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.InfantryWeaponMounted;
import megamek.common.options.GameOptions;
import megamek.common.units.ConvInfantry;
import megamek.common.weapons.Weapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the Inferno firing mode on the conventional infantry SRM launcher.
 *
 * <p>TW p. 143 gives an SRM infantry platoon a number of inferno missiles equal to its Damage Value divided by
 * two, rounded down. The launcher therefore offers Inferno and Damage as alternative modes, where the incendiary
 * support weapons offer Damage and Heat.</p>
 */
class InfantryInfernoSRMHandlerTest {

    private static final String INFERNO_SRM_LAUNCHER = "InfantryStandardSRMInferno";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static java.util.List<String> modeNamesOf(EquipmentType equipment) {
        java.util.List<String> modeNames = new java.util.ArrayList<>();
        Enumeration<EquipmentMode> modes = equipment.getModes();
        while (modes.hasMoreElements()) {
            modeNames.add(modes.nextElement().getName());
        }
        return modeNames;
    }

    @Test
    @DisplayName("The Inferno SRM launcher offers Inferno and Damage, not Heat")
    void infernoLauncherOffersInfernoAndDamage() {
        EquipmentType launcher = EquipmentType.get(INFERNO_SRM_LAUNCHER);
        assertNotNull(launcher, "The Inferno SRM launcher should be registered");

        java.util.List<String> modeNames = modeNamesOf(launcher);

        assertTrue(modeNames.contains(Weapon.MODE_INFERNO),
              "True Inferno munitions must offer an Inferno mode; modes are " + modeNames);
        assertTrue(modeNames.contains(Weapon.MODE_FLAMER_DAMAGE),
              "The platoon must still be able to fire ordinary SRM damage; modes are " + modeNames);
        assertEquals(2, modeNames.size(), "Only Inferno and Damage apply here; modes are " + modeNames);
    }

    @Test
    @DisplayName("An incendiary weapon still offers Damage and Heat")
    void incendiaryWeaponKeepsDamageAndHeat() {
        EquipmentType incendiary = EquipmentType.get("InfantryAutoGLInferno");
        assertNotNull(incendiary, "The incendiary auto grenade launcher should be registered");
        assertInstanceOf(InfantryWeapon.class, incendiary);

        // Incendiary weapons carry no modes until a game hands them its options, unlike the Inferno launcher,
        // which sets its own in the constructor.
        ((InfantryWeapon) incendiary).adaptToGameOptions(new GameOptions());

        java.util.List<String> modeNames = modeNamesOf(incendiary);

        assertTrue(modeNames.contains(Weapon.MODE_FLAMER_HEAT),
              "Incendiary weapons convert damage to heat; modes are " + modeNames);
        assertFalse(modeNames.contains(Weapon.MODE_INFERNO),
              "Incendiary weapons carry no Inferno munitions; modes are " + modeNames);
    }

    @Test
    @DisplayName("Every mode a platoon's mount counts can also be read back")
    void everyCountedModeIsReadable() {
        // An infantry mount combines the modes of its primary and secondary weapons, so it can offer modes its
        // own type does not have. Counting with getModesCount() and then reading from the type walks off the end
        // of the type's list, which crashed the right-click Modes menu on any platoon whose primary weapon has no
        // modes of its own.
        InfantryWeapon primaryWithoutModes = (InfantryWeapon) EquipmentType.get("InfantryAssaultRifle");
        InfantryWeapon secondaryWithModes = (InfantryWeapon) EquipmentType.get(INFERNO_SRM_LAUNCHER);
        assertNotNull(primaryWithoutModes, "The assault rifle should be registered");
        assertNotNull(secondaryWithModes, "The Inferno SRM launcher should be registered");

        ConvInfantry platoon = new ConvInfantry();
        InfantryWeaponMounted mount = new InfantryWeaponMounted(platoon, primaryWithoutModes, secondaryWithModes);

        assertTrue(mount.getModesCount() > 0,
              "The mount should pick up the launcher's modes even though its own type has none");
        for (int position = 0; position < mount.getModesCount(); position++) {
            final int index = position;
            assertNotNull(assertDoesNotThrow(() -> mount.getMode(index),
                        "Mode " + index + " is counted, so it must be readable"),
                  "Mode " + index + " should not be null");
        }
    }

    @Test
    @DisplayName("The Inferno launcher keeps its modes whatever the infantry heat option says")
    void infernoLauncherIgnoresTheInfantryHeatOption() {
        EquipmentType launcher = EquipmentType.get(INFERNO_SRM_LAUNCHER);
        assertInstanceOf(InfantryWeapon.class, launcher);

        // The base implementation strips or adds Damage/Heat here depending on the option. This weapon must not
        // move, because its modes are about munitions rather than about converting damage to heat.
        ((InfantryWeapon) launcher).adaptToGameOptions(null);

        java.util.List<String> modeNames = modeNamesOf(launcher);
        assertTrue(modeNames.contains(Weapon.MODE_INFERNO) && modeNames.contains(Weapon.MODE_FLAMER_DAMAGE),
              "The Inferno/Damage pair must survive adaptToGameOptions(); modes are " + modeNames);
        assertEquals(2, modeNames.size(), "No Heat mode should have been added; modes are " + modeNames);
    }
}
