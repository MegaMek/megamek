/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import megamek.common.annotations.Nullable;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.autocannons.ACWeapon;
import megamek.common.weapons.autocannons.LBXACWeapon;
import megamek.common.weapons.autocannons.UACWeapon;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.lrms.StreakLRMWeapon;
import megamek.common.weapons.srms.StreakSRMWeapon;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class contains helper methods for Special Pilot Abilities.
 */
public final class PilotSPAHelper {

    /** @return True when the given Mounted equipment is a valid choice for the Weapons Specialist SPA. */
    public static boolean isWeaponSpecialistValid(Mounted mounted) {
        return isWeaponSpecialistValid(mounted.getType());
    }

    /** @return True when the given EquipmentType is a valid choice for the Weapons Specialist SPA. */
    public static boolean isWeaponSpecialistValid(EquipmentType equipmentType) {
        return (equipmentType instanceof WeaponType) && !(equipmentType instanceof BayWeapon)
                && !equipmentType.hasFlag(WeaponType.F_AMS) && !equipmentType.is("Screen Launcher")
                && !equipmentType.hasFlag(WeaponType.F_C3M) && !equipmentType.hasFlag(WeaponType.F_C3MBS);
    }

    /**
     * Returns a List of distinct (each occuring only once) weapon names of weapons present on the given
     * Entity that are valid choices for the Weapon Specialist SPA.
     *
     * @return A list of weapon names from the given Entity that are valid choices for the Weapon Specialist SPA
     */
    public static List<String> weaponSpecialistValidWeaponNames(Entity entity) {
        return entity.getTotalWeaponList().stream()
                .map(Mounted::getType)
                .filter(PilotSPAHelper::isWeaponSpecialistValid)
                .map(EquipmentType::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns a List of weapons from those present on the given Entity that are valid choices for the
     * Weapon Specialist SPA. Unlike {@link #weaponSpecialistValidWeaponNames(Entity)}, weapons
     * appear in this list as often as they are present on the given Entity.
     *
     * @return A list of weapons from the given Entity that are valid choices for the Weapon Specialist SPA
     */
    public static List<Mounted> weaponSpecialistValidWeapons(Entity entity) {
        return entity.getTotalWeaponList().stream()
                .filter(PilotSPAHelper::isWeaponSpecialistValid)
                .collect(Collectors.toList());
    }

    /**
     * Returns true when the given Mounted equipment is a valid choice for the Sandblaster SPA, taking into account
     * the given GameOptions, particularly, if TacOps RapidFire Autocannons is in use. When the given GameOptions
     * is null, TacOps RapidFire Autocannons is assumed off. When TacOps RapidFire Autocannons is off,
     * standard ACs are considered invalid.
     *
     * @return True when the given EquipmentType is a valid choice for the Sandblaster SPA.
     */
    public static boolean isSandblasterValid(Mounted mounted, @Nullable GameOptions options) {
        return isSandblasterValid(mounted.getType(), options);
    }

    /**
     * Returns true when the given EquipmentType is a valid choice for the Sandblaster SPA, taking into account
     * the given GameOptions, particularly, if TacOps RapidFire Autocannons is in use. When the given GameOptions
     * is null, TacOps RapidFire Autocannons is assumed off. When TacOps RapidFire Autocannons is off,
     * standard ACs are considered invalid.
     *
     * @return True when the given EquipmentType is a valid choice for the Sandblaster SPA.
     */
    public static boolean isSandblasterValid(EquipmentType equipmentType, @Nullable GameOptions options) {
        boolean rapidFireAC = (options != null) && options.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RAPID_AC)
                && (equipmentType instanceof ACWeapon);

        return (equipmentType instanceof WeaponType)
                && ((equipmentType instanceof UACWeapon) || (equipmentType instanceof LBXACWeapon) || rapidFireAC
                || ((WeaponType) equipmentType).damage == WeaponType.DAMAGE_BY_CLUSTERTABLE)
                && !(equipmentType instanceof StreakLRMWeapon) && !(equipmentType instanceof StreakSRMWeapon);
    }

    /**
     * Returns a List of distinct (each occuring only once) weapon names of weapons present on the given
     * Entity that are valid choices for the Sandblaster SPA.
     *
     * @return A list of weapon names from the given Entity that are valid choices for the Sandblaster SPA
     */
    public static List<String> sandblasterValidWeaponNames(Entity entity, @Nullable GameOptions options) {
        return entity.getTotalWeaponList().stream()
                .filter(mounted -> isSandblasterValid(mounted, options))
                .map(Mounted::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns a List of weapons from those present on the given Entity that are valid choices for the
     * Sandblaster SPA. Unlike {@link #sandblasterValidWeaponNames(Entity, GameOptions)}, weapons
     * appear in this list as often as they are present on the given Entity.
     *
     * @return A list of weapons from the given Entity that are valid choices for the Sandblaster SPA
     */
    public static List<Mounted> sandblasterValidWeapons(Entity entity, @Nullable GameOptions options) {
        return entity.getTotalWeaponList().stream()
                .filter(mounted -> isSandblasterValid(mounted, options))
                .collect(Collectors.toList());
    }

    private PilotSPAHelper() { }
}