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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.InfantryWeaponMounted;
import megamek.common.equipment.Mounted;
import megamek.common.options.GameOptions;
import megamek.common.units.ConvInfantry;
import megamek.common.weapons.Weapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers Inferno munitions on conventional infantry SRM launchers.
 *
 * <p>TW p. 143 gives an SRM infantry platoon a number of inferno missiles equal to its Damage Value divided by
 * two, rounded down. Per the TechManual pp. 350-352 errata the platoon declares Inferno or standard munitions before
 * the battle, so the choice lives on the platoon and holds for the whole battle rather than being a firing mode. The
 * Inferno SRM launchers themselves are withdrawn. The incendiary support weapons keep their Damage and Heat modes,
 * which only convert damage to heat.</p>
 */
class InfantryInfernoSRMHandlerTest {

    private static final String HEAVY_SRM_LAUNCHER = "InfantryHeavySRM";
    private static final String ASSAULT_RIFLE = "InfantryAssaultRifle";
    private static final String INCENDIARY_GRENADE_LAUNCHER = "InfantryAutoGLInferno";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static List<String> modeNamesOf(EquipmentType equipment) {
        List<String> modeNames = new ArrayList<>();
        Enumeration<EquipmentMode> modes = equipment.getModes();
        while (modes.hasMoreElements()) {
            modeNames.add(modes.nextElement().getName());
        }
        return modeNames;
    }

    private static InfantryWeapon weapon(String internalName) {
        EquipmentType equipment = EquipmentType.get(internalName);
        assertInstanceOf(InfantryWeapon.class, equipment, internalName + " should be a registered infantry weapon");
        return (InfantryWeapon) equipment;
    }

    private static ConvInfantry platoon(InfantryWeapon primary, InfantryWeapon secondary) {
        ConvInfantry platoon = new ConvInfantry();
        platoon.setPrimaryWeapon(primary);
        platoon.setSecondaryWeapon(secondary);
        return platoon;
    }

    @Test
    @DisplayName("An incendiary weapon still offers Damage and Heat")
    void incendiaryWeaponKeepsDamageAndHeat() {
        InfantryWeapon incendiary = weapon(INCENDIARY_GRENADE_LAUNCHER);

        // Incendiary weapons carry no modes until a game hands them its options.
        incendiary.adaptToGameOptions(new GameOptions());

        List<String> modeNames = modeNamesOf(incendiary);
        assertTrue(modeNames.contains(Weapon.MODE_FLAMER_DAMAGE) && modeNames.contains(Weapon.MODE_FLAMER_HEAT),
              "Incendiary weapons convert damage to heat; modes are " + modeNames);
    }

    @Test
    @DisplayName("Every mode a platoon's mount counts can also be read back")
    void everyCountedModeIsReadable() {
        // An infantry mount combines the modes of its primary and secondary weapons, so it can offer modes its
        // own type does not have. Counting with getModesCount() and then reading from the type walks off the end
        // of the type's list, which crashed the right-click Modes menu on any platoon whose primary weapon has no
        // modes of its own.
        InfantryWeapon secondaryWithModes = weapon(INCENDIARY_GRENADE_LAUNCHER);
        secondaryWithModes.adaptToGameOptions(new GameOptions());

        ConvInfantry platoon = new ConvInfantry();
        InfantryWeaponMounted mount = new InfantryWeaponMounted(platoon, weapon(ASSAULT_RIFLE), secondaryWithModes);

        assertTrue(mount.getModesCount() > 0,
              "The mount should pick up the grenade launcher's modes even though its own type has none");
        for (int position = 0; position < mount.getModesCount(); position++) {
            final int index = position;
            assertNotNull(assertDoesNotThrow(() -> mount.getMode(index),
                        "Mode " + index + " is counted, so it must be readable"),
                  "Mode " + index + " should not be null");
        }
    }

    @Test
    @DisplayName("A platoon with an ordinary SRM launcher starts on standard and can declare Inferno")
    void ordinarySrmPlatoonDefaultsToStandard() {
        ConvInfantry platoon = platoon(weapon(ASSAULT_RIFLE), weapon(HEAVY_SRM_LAUNCHER));

        assertTrue(platoon.hasSrmLauncher(), "The heavy SRM launcher is an SRM launcher");
        assertFalse(platoon.firesInfernoSrms(), "An ordinary SRM platoon should start with standard munitions");

        platoon.setInfernoSrmsDeclared(true);
        assertTrue(platoon.firesInfernoSrms(), "Declaring Inferno before the battle should load Inferno munitions");
    }

    @Test
    @DisplayName("A platoon without an SRM launcher never fires Inferno, even if declared")
    void platoonWithoutSrmNeverFiresInferno() {
        ConvInfantry platoon = platoon(weapon(ASSAULT_RIFLE), null);
        platoon.setInfernoSrmsDeclared(true);

        assertFalse(platoon.hasSrmLauncher(), "An assault rifle is not an SRM launcher");
        assertFalse(platoon.firesInfernoSrms(), "Only SRM launchers carry Inferno munitions");
    }

    @Test
    @DisplayName("The platoon's weapon mount counts as an SRM launcher when either of its weapons is one")
    void combinedMountWithSrmIsAnSrmLauncherMount() {
        ConvInfantry platoon = new ConvInfantry();
        Mounted<?> rifleWithSrm = new InfantryWeaponMounted(platoon, weapon(ASSAULT_RIFLE),
              weapon(HEAVY_SRM_LAUNCHER));
        Mounted<?> srmWithRifle = new InfantryWeaponMounted(platoon, weapon(HEAVY_SRM_LAUNCHER),
              weapon(ASSAULT_RIFLE));
        Mounted<?> rifleOnly = Mounted.createMounted(platoon, weapon(ASSAULT_RIFLE));

        assertTrue(InfantryWeapon.isSrmLauncherMount(rifleWithSrm), "SRM launcher as the other weapon");
        assertTrue(InfantryWeapon.isSrmLauncherMount(srmWithRifle), "SRM launcher as the range weapon");
        assertFalse(InfantryWeapon.isSrmLauncherMount(rifleOnly), "No SRM launcher on the mount");
        assertFalse(InfantryWeapon.isSrmLauncherMount(null), "No mount at all");
    }
}
