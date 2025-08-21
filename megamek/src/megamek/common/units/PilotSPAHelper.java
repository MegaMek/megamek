/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.List;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.autocannons.ACWeapon;
import megamek.common.weapons.autocannons.LBXACWeapon;
import megamek.common.weapons.autocannons.UACWeapon;
import megamek.common.weapons.bayweapons.BayWeapon;

/**
 * This class contains helper methods for Special Pilot Abilities.
 */
public final class PilotSPAHelper {

    /**
     * @return True when the given Mounted equipment is a valid choice for the Weapons Specialist SPA.
     */
    public static boolean isWeaponSpecialistValid(Mounted<?> mounted, @Nullable GameOptions options) {
        return isWeaponSpecialistValid(mounted.getType(), options);
    }

    /**
     * @return True when the given EquipmentType is a valid choice for the Weapons Specialist SPA.
     */
    public static boolean isWeaponSpecialistValid(EquipmentType equipmentType, @Nullable GameOptions options) {
        boolean amsAsWeapon = (options != null)
              && options.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_MANUAL_AMS)
              && (equipmentType.hasFlag(WeaponType.F_AMS));

        return (equipmentType instanceof WeaponType) && !(equipmentType instanceof BayWeapon)
              && (!equipmentType.hasFlag(WeaponType.F_AMS) || amsAsWeapon)
              && !equipmentType.is("Screen Launcher")
              && !equipmentType.hasFlag(WeaponType.F_C3M) && !equipmentType.hasFlag(WeaponType.F_C3MBS)
              && !equipmentType.hasFlag(WeaponType.F_INFANTRY_ATTACK);
    }

    /**
     * Returns a List of distinct (each occurring only once) weapon names of weapons present on the given Entity that
     * are valid choices for the Weapon Specialist SPA.
     *
     * @return A list of weapon names from the given Entity that are valid choices for the Weapon Specialist SPA
     */
    public static List<String> weaponSpecialistValidWeaponNames(Entity entity, @Nullable GameOptions options) {
        return entity.getTotalWeaponList().stream()
              .map(Mounted::getType)
              .filter(mounted -> isWeaponSpecialistValid(mounted, options))
              .map(EquipmentType::getName)
              .distinct()
              .collect(Collectors.toList());
    }

    /**
     * Returns a List of weapons from those present on the given Entity that are valid choices for the Weapon Specialist
     * SPA. Unlike {@link #weaponSpecialistValidWeaponNames(Entity, GameOptions)}, weapons appear in this list as often
     * as they are present on the given Entity.
     *
     * @return A list of weapons from the given Entity that are valid choices for the Weapon Specialist SPA
     */
    public static List<Mounted<?>> weaponSpecialistValidWeapons(Entity entity, @Nullable GameOptions options) {
        return entity.getTotalWeaponList().stream()
              .filter(mounted -> isWeaponSpecialistValid(mounted, options))
              .collect(Collectors.toList());
    }

    /**
     * Returns true when the given Mounted equipment is a valid choice for the Sandblaster SPA, taking into account the
     * given GameOptions, particularly, if TacOps RapidFire Autocannons is in use. When the given GameOptions is null,
     * TacOps RapidFire Autocannons is assumed off. When TacOps RapidFire Autocannons is off, standard ACs are
     * considered invalid.
     *
     * @return True when the given EquipmentType is a valid choice for the Sandblaster SPA.
     */
    public static boolean isSandblasterValid(Mounted<?> mounted, @Nullable GameOptions options) {
        return isSandblasterValid(mounted.getType(), options);
    }

    /**
     * Returns true when the given EquipmentType is a valid choice for the Sandblaster SPA, taking into account the
     * given GameOptions, particularly, if TacOps RapidFire Autocannons is in use. When the given GameOptions is null,
     * TacOps RapidFire Autocannons is assumed off. When TacOps RapidFire Autocannons is off, standard ACs are
     * considered invalid.
     *
     * @return True when the given EquipmentType is a valid choice for the Sandblaster SPA.
     */
    public static boolean isSandblasterValid(EquipmentType equipmentType, @Nullable GameOptions options) {
        boolean rapidFireAC = (options != null)
              && options.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RAPID_AC)
              && (equipmentType instanceof ACWeapon);

        return (equipmentType instanceof WeaponType)
              && ((equipmentType instanceof UACWeapon) || (equipmentType instanceof LBXACWeapon) || rapidFireAC
              || ((WeaponType) equipmentType).getDamage() == WeaponType.DAMAGE_BY_CLUSTER_TABLE);
    }

    /**
     * Returns a List of distinct (each occurring only once) weapon names of weapons present on the given Entity that
     * are valid choices for the Sandblaster SPA.
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
     * Returns a List of weapons from those present on the given Entity that are valid choices for the Sandblaster SPA.
     * Unlike {@link #sandblasterValidWeaponNames(Entity, GameOptions)}, weapons appear in this list as often as they
     * are present on the given Entity.
     *
     * @return A list of weapons from the given Entity that are valid choices for the Sandblaster SPA
     */
    public static List<Mounted<?>> sandblasterValidWeapons(Entity entity, @Nullable GameOptions options) {
        return entity.getTotalWeaponList().stream()
              .filter(mounted -> isSandblasterValid(mounted, options))
              .collect(Collectors.toList());
    }

    private PilotSPAHelper() {
    }
}
