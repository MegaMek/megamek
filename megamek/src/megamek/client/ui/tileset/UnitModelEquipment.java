/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.tileset;

import java.util.List;

import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.weapons.bayWeapons.BayWeapon;

/** Shared offline/live mount identity. Captures values only; never hands mutable Mounted objects to the renderer. */
public final class UnitModelEquipment {
    private UnitModelEquipment() { }

    public record Mount(int index, String internalName, String location, String secondLocation, boolean rear,
          boolean omniPod, double size, EquipmentModelPolicy policy, String family, List<Integer> members) {
        public Mount {
            members = List.copyOf(members);
        }
    }

    public static Mount describe(Entity entity, Mounted<?> mounted) {
        var policy = mounted.isWeaponGroup() ? EquipmentModelPolicy.MEMBERS : EquipmentModelPolicy.forType(mounted.getType());
        List<Integer> members = (policy == EquipmentModelPolicy.MEMBERS) && (mounted instanceof WeaponMounted weapon)
              ? weapon.getBayWeapons().stream().map(Mounted::getEquipmentNum).toList() : List.of();
        return new Mount(mounted.getEquipmentNum(), mounted.getType().getInternalName(),
              location(entity, mounted.getLocation()), location(entity, mounted.getSecondLocation()),
              mounted.isRearMounted(), mounted.isOmniPodMounted(), mounted.getSize(), policy, family(mounted.getType()), members);
    }

    private static String location(Entity entity, int location) {
        return location < 0 ? "" : entity.getLocationAbbr(location);
    }

    public static List<String> armsWith(Mek mek, int actuator) {
        return java.util.stream.IntStream.of(Mek.LOC_LEFT_ARM, Mek.LOC_RIGHT_ARM)
              .filter(location -> mek.hasSystem(actuator, location)).mapToObj(mek::getLocationAbbr).toList();
    }

    /** Broad art families only. Exact dimensions and placement belong to the authored chassis recipe. */
    public static String family(EquipmentType type) {
        if (type instanceof WeaponType weapon) {
            // Grouping devices and synthetic attacks do not have independent weapon geometry.
            if ((weapon instanceof BayWeapon) || weapon.hasFlag(WeaponType.F_MGA)
                  || weapon.hasFlag(WeaponTypeFlag.INTERNAL_REPRESENTATION)) {
                return "internal";
            } else if (weapon.hasFlag(WeaponType.F_MISSILE) || weapon.hasFlag(WeaponType.F_CRUISE_MISSILE)
                  || weapon.hasFlag(WeaponType.F_ARROW_IV) || (weapon.getAmmoType() == AmmoTypeEnum.ARROW_IV_BOMB)) {
                // Missile artillery must precede tube artillery; neither requires F_MISSILE.
                return "missile";
            } else if (weapon.hasFlag(WeaponType.F_PPC)) {
                return "ppc";
            } else if (weapon.hasFlag(WeaponType.F_LASER)) {
                return "laser";
            } else if (weapon.hasFlag(WeaponType.F_MG)) {
                return "machine-gun";
            } else if (weapon.hasFlag(WeaponType.F_BALLISTIC) || weapon.hasFlag(WeaponType.F_ARTILLERY)) {
                return "ballistic";
            } else if (weapon.hasFlag(WeaponType.F_FLAMER)) {
                return "flamer";
            } else if (weapon.hasFlag(WeaponType.F_TAG) || weapon.hasFlag(WeaponType.F_C3M)
                  || weapon.hasFlag(WeaponType.F_C3MBS)) {
                return "sensor";
            } else if (weapon.hasFlag(WeaponType.F_PLASMA) || weapon.hasFlag(WeaponType.F_ENERGY)) {
                return "energy";
            } else if (weapon.hasFlag(WeaponType.F_EXTINGUISHER)) {
                return "extinguisher";
            } else if (weapon.hasFlag(WeaponType.F_INFANTRY) && weapon.hasFlag(WeaponType.F_INF_POINT_BLANK)) {
                // Check ranged families first: some pistols and archaic bows also have infantry flags.
                return "infantry-melee";
            }
            // These launchers do not carry any of the broad weapon-family flags.
            return switch (weapon.getAmmoType()) {
                case SCREEN_LAUNCHER -> "screen-launcher";
                case BA_MICRO_BOMB -> "bomb";
                case MINE -> "mine";
                case null, default -> "unmapped-weapon";
            };
        }
        if (type instanceof MiscType misc) {
            if (misc.hasFlag(MiscType.F_JUMP_JET)) {
                return "jump-jet";
            } else if (misc.hasFlag(MiscType.F_PHYSICAL_WEAPON)) {
                return physicalFamily(misc);
            }
        }
        return "internal";
    }

    /** Physical equipment includes claws, shields and industrial tools, not just F_CLUB weapons. */
    private static String physicalFamily(MiscType misc) {
        if (misc.hasFlag(MiscType.F_SHIELD)) {
            return "shield";
        } else if (misc.hasFlag(MiscType.F_HAND_WEAPON) && misc.hasFlag(MiscTypeFlag.S_CLAW)) {
            return "claw";
        } else if (misc.hasFlag(MiscType.F_SPIKES)) {
            return "spikes";
        } else if (misc.hasFlag(MiscType.F_TALON)) {
            return "talons";
        } else if (misc.hasFlag(MiscTypeFlag.S_HATCHET)) {
            return "hatchet";
        } else if (misc.hasAnyFlag(MiscTypeFlag.S_SWORD, MiscTypeFlag.S_RETRACTABLE_BLADE,
              MiscTypeFlag.S_VIBRO_SMALL, MiscTypeFlag.S_VIBRO_MEDIUM, MiscTypeFlag.S_VIBRO_LARGE)) {
            return "blade";
        } else if (misc.hasFlag(MiscTypeFlag.S_MACE)) {
            return "mace";
        } else if (misc.hasFlag(MiscTypeFlag.S_LANCE)) {
            return "lance";
        } else if (misc.hasFlag(MiscTypeFlag.S_FLAIL)) {
            return "flail";
        } else if (misc.hasFlag(MiscTypeFlag.S_CHAIN_WHIP)) {
            return "chain-whip";
        } else if (misc.hasFlag(MiscTypeFlag.S_WRECKING_BALL)) {
            return "wrecking-ball";
        } else if (misc.hasAnyFlag(MiscTypeFlag.S_CHAINSAW, MiscTypeFlag.S_DUAL_SAW, MiscTypeFlag.S_BUZZSAW)) {
            return "saw";
        } else if (misc.hasFlag(MiscTypeFlag.S_BACKHOE)) {
            return "backhoe";
        } else if (misc.hasFlag(MiscTypeFlag.S_COMBINE)) {
            return "combine";
        } else if (misc.hasFlag(MiscTypeFlag.S_PILE_DRIVER)) {
            return "pile-driver";
        } else if (misc.hasFlag(MiscTypeFlag.S_MINING_DRILL)) {
            return "mining-drill";
        } else if (misc.hasFlag(MiscTypeFlag.S_ROCK_CUTTER)) {
            return "rock-cutter";
        } else if (misc.hasFlag(MiscTypeFlag.S_SPOT_WELDER)) {
            return "spot-welder";
        } else if (misc.hasAnyFlag(MiscTypeFlag.S_CLUB, MiscTypeFlag.S_TREE_CLUB)) {
            return "club";
        }
        return "unmapped-melee";
    }
}
