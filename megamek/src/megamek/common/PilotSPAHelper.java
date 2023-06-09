package megamek.common;

import megamek.common.weapons.autocannons.LBXACWeapon;
import megamek.common.weapons.autocannons.UACWeapon;
import megamek.common.weapons.bayweapons.BayWeapon;

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
                && !equipmentType.hasFlag(WeaponType.F_AMS) && !equipmentType.is("Screen Launcher");
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

    /** @return True when the given Mounted equipment is a valid choice for the Sandblaster SPA. */
    public static boolean isSandblasterValid(Mounted mounted) {
        return isSandblasterValid(mounted.getType());
    }

    /** @return True when the given EquipmentType is a valid choice for the Sandblaster SPA. */
    public static boolean isSandblasterValid(EquipmentType equipmentType) {
        return (equipmentType instanceof WeaponType)
                && ((equipmentType instanceof UACWeapon) || (equipmentType instanceof LBXACWeapon)
                || ((WeaponType) equipmentType).damage == WeaponType.DAMAGE_BY_CLUSTERTABLE);
    }

    /**
     * Returns a List of distinct (each occuring only once) weapon names of weapons present on the given
     * Entity that are valid choices for the Sandblaster SPA.
     *
     * @return A list of weapon names from the given Entity that are valid choices for the Sandblaster SPA
     */
    public static List<String> sandblasterValidWeaponNames(Entity entity) {
        return entity.getTotalWeaponList().stream()
                .map(Mounted::getType)
                .filter(PilotSPAHelper::isSandblasterValid)
                .map(EquipmentType::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns a List of weapons from those present on the given Entity that are valid choices for the
     * Sandblaster SPA. Unlike {@link #sandblasterValidWeaponNames(Entity)}, weapons
     * appear in this list as often as they are present on the given Entity.
     *
     * @return A list of weapons from the given Entity that are valid choices for the Sandblaster SPA
     */
    public static List<Mounted> sandblasterValidWeapons(Entity entity) {
        return entity.getTotalWeaponList().stream()
                .filter(PilotSPAHelper::isSandblasterValid)
                .collect(Collectors.toList());
    }

    private PilotSPAHelper() { }
}
