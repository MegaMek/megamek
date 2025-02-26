/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.advancedsearch;

import java.util.Locale;

enum AdvancedSearchEquipmentClass {
    EMPTY, AUTOCANNON, RAC, ULTRA, LIGHT, MACHINE_GUN, GAUSS, BALLISTIC, PLASMA, ENERGY, LASER, PULSE, RE_ENGINEERED, PPC, TASER,
    FLAMER, MISSILE, LRM, MRM, SRM, PHYSICAL, AMS, PRACTICAL_PHYSICAL, INFANTRY_SUIT, PROBE, ECM, CHASSIS_MOD, MOVE_ENHANCE;

    public boolean matches(String name) {
        if (this == EMPTY) {
            return true;
        }
        name = name.toLowerCase(Locale.ROOT);
        if (name.contains("ammo")) {
            return false;
        }
        if (this == PHYSICAL) {
            return PRACTICAL_PHYSICAL.matches(name) || name.contains("backhoe") || name.contains("saw") || name.contains("whip")
                || name.contains("combine") || name.contains("driver") || name.contains("drill") || name.contains("ram")
                || name.contains("cutter") || name.contains("welder") || name.contains("wrecking");
        } else if (this == PRACTICAL_PHYSICAL) {
            return name.contains("claw") || name.contains("flail") || name.contains("hatchet") || name.contains("lance")
                || name.contains("mace") || name.contains("blade") || name.contains("shield") || name.contains("sword")
                || name.contains("talons");
        } else if (this == MISSILE) {
            return name.contains("lrm") || name.contains("mrm") || name.contains("srm");
        } else if (this == RE_ENGINEERED) {
            return name.contains("engineered");
        } else if (this == ENERGY) {
            return LASER.matches(name) || PPC.matches(name) || FLAMER.matches(name);
        } else if (this == MACHINE_GUN) {
            return (name.contains("mg") || name.contains("machine")) && !name.contains("ammo");
        } else if (this == BALLISTIC) {
            return AUTOCANNON.matches(name) || GAUSS.matches(name) || MACHINE_GUN.matches(name);
        } else if (this == RAC) {
            return name.contains("rotary");
        } else if (this == ULTRA) {
            return name.contains("ultraa");
        } else if (this == LIGHT) {
            return name.contains("light auto cannon");
        } else if (this == INFANTRY_SUIT) {
            return (name.contains("suit") || name.contains(" kit") || name.contains(", standard") || name.contains(", concealed")
                || name.contains("clothing") || name.contains("vest") || name.contains("chainmail") || name.contains("parka"))
                && !name.contains("generic") && !name.contains("suite");
        } else if (this == AMS) {
            return name.contains("ams") || name.contains("antimiss");
        } else if (this == PROBE) {
            return name.contains("probe") || name.contains("electronicwarfare") || name.contains("cews") || name.contains("watchdog");
        } else if (this == ECM) {
            return (name.contains("ecm") || name.contains("electronicwarfare") || name.contains("cews") || name.contains("watchdog"))
                && !name.contains("sneak");
        } else if (this == CHASSIS_MOD) {
            return name.contains("chassis") || name.contains("environmental sealing") || name.contains("external power pickup");
        } else if (this == MOVE_ENHANCE) {
            return (name.contains("masc") || name.contains("myomer") || name.contains("charger") || name.contains("tsm"))
                && !name.contains("vest") && !name.contains("suit");
        } else if (name.contains(name().toLowerCase(Locale.ROOT)) && !name.contains("ammo")) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return switch (this) {
            case EMPTY -> "";
            case AUTOCANNON -> "Autocannon";
            case ULTRA -> "Ultra A/C";
            case LIGHT -> "Light A/C";
            case MACHINE_GUN -> "Machine Gun";
            case GAUSS -> "Gauss";
            case BALLISTIC -> "Ballistic";
            case PLASMA -> "Plasma";
            case ENERGY -> "Energy";
            case LASER -> "Laser";
            case PULSE -> "Pulse Laser";
            case RE_ENGINEERED -> "Re-Engineered Laser";
            case PPC -> "PPC";
            case TASER -> "Taser";
            case FLAMER -> "Flamer";
            case MISSILE -> "Missile";
            case PHYSICAL -> "Physical Weapons and Gear";
            case PRACTICAL_PHYSICAL -> "Physical Weapons";
            case INFANTRY_SUIT -> "Infantry Armor Suits";
            case PROBE -> "Active Probes";
            case ECM -> "ECM Systems";
            case CHASSIS_MOD -> "Chassis Modifications";
            case MOVE_ENHANCE -> "Movement Enhancing Gear";
            default -> super.toString();
        };
    }
}
