/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import java.util.HashSet;

import megamek.common.units.UnitType;

/**
 * Base functionality for chassis and model records for RAT generator.
 *
 * @author Neoancient
 */
public class AbstractUnitRecord {

    protected String chassis;
    protected boolean omni;
    protected boolean clan;
    protected int unitType;
    protected int introYear;
    protected HashSet<String> includedFactions;

    public AbstractUnitRecord(String chassis) {
        this.chassis = chassis;
        unitType = UnitType.MEK;
        omni = false;
        clan = false;
        includedFactions = new HashSet<>();
    }

    /**
     * Adjusts availability rating for +/- dynamic. Also reduces availability by introduction year, with 1 year before
     * heavily reduced for pre-production prototypes and first year slightly reduced for working out initial
     * production.
     *
     * @param avRating     The AvailabilityRecord for the chassis or model.
     * @param equipRating  The force equipment rating.
     * @param ratingLevels The number of equipment rating levels used by the faction.
     * @param year         The game year
     *
     * @return The adjusted availability rating.
     */
    public int calcAvailability(AvailabilityRating avRating, int equipRating, int ratingLevels, int year) {

        int retVal;
        if (!avRating.hasMultipleRatings()) {
            retVal = avRating.adjustForRating(equipRating, ratingLevels);
        } else {
            retVal = avRating.getAvailability(equipRating);
        }

        // Pre-production prototypes are heavily reduced
        if (year == introYear - 1) {
            retVal -= 2;
        }

        // Initial production year is slightly reduced
        if (year == introYear) {
            retVal -= 1;
        }

        return Math.max(retVal, 0);
    }

    public String getChassis() {
        return chassis;
    }

    public void setChassis(String chassis) {
        this.chassis = chassis;
    }

    public final String getChassisKey() {
        if (omni) {
            return clan ? chassis + "[" + UnitType.getTypeName(unitType) + "]ClanOmni" :
                  chassis + "[" + UnitType.getTypeName(unitType) + "]ISOmni";
        }
        return chassis + "[" + UnitType.getTypeName(unitType) + "]";
    }

    public String getKey() {
        return getChassisKey();
    }

    public int getUnitType() {
        return unitType;
    }

    public void setUnitType(int type) {
        unitType = type;
    }

    public void setUnitType(String type) {
        unitType = parseUnitType(type);
    }

    public boolean isOmni() {
        return omni;
    }

    public void setOmni(boolean omni) {
        this.omni = omni;
    }

    public boolean isClan() {
        return clan;
    }

    public void setClan(boolean clan) {
        this.clan = clan;
    }

    public int getIntroYear() {
        return introYear;
    }

    public void setIntroYear(int year) {
        this.introYear = year;
    }

    public HashSet<String> getIncludedFactions() {
        return includedFactions;
    }

    @Override
    public String toString() {
        return getKey();
    }

    public static int parseUnitType(String typeName) {
        return switch (typeName) {
            case "Mek" -> UnitType.MEK;
            case "Tank" -> UnitType.TANK;
            case "BattleArmor" -> UnitType.BATTLE_ARMOR;
            case "Infantry" -> UnitType.INFANTRY;
            case "ProtoMek" -> UnitType.PROTOMEK;
            case "VTOL" -> UnitType.VTOL;
            case "Naval" -> UnitType.NAVAL;
            case "Gun Emplacement" -> UnitType.GUN_EMPLACEMENT;
            case "Conventional Fighter" -> UnitType.CONV_FIGHTER;
            case "AeroSpaceFighter" -> UnitType.AEROSPACE_FIGHTER;
            case "Aero" -> UnitType.AERO;
            case "Small Craft" -> UnitType.SMALL_CRAFT;
            case "Dropship" -> UnitType.DROPSHIP;
            case "Jumpship" -> UnitType.JUMPSHIP;
            case "Warship" -> UnitType.WARSHIP;
            case "Space Station" -> UnitType.SPACE_STATION;
            case "Handheld Weapon" -> UnitType.HANDHELD_WEAPON;
            case "Mobile Structure" -> UnitType.MOBILE_STRUCTURE;
            case "Building Entity" -> UnitType.ADVANCED_BUILDING;
            default -> -1;
        };
    }
}

