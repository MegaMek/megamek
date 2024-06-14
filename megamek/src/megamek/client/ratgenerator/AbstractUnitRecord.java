/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ratgenerator;

import java.util.HashSet;

import megamek.common.UnitType;

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
     * Returns availability value modified for rating differential on +/- rating, and adjusted for
     * the first few years before introduction (field experiments, etc.) and immediately after
     * introduction (low rate initial production).
     *
     * @param initialAv        AvailabilityRating for the chassis or model.
     * @param formationRating  force equipment rating.
     * @param ratingLevels     number of equipment rating levels used by the faction.
     * @param year             game year
     * @return                 adjusted availability rating.
     */
    public int calcAvailability(AvailabilityRating initialAv, int formationRating, int ratingLevels, int year) {
        int avRating = initialAv.adjustForRating(formationRating, ratingLevels);

        if (year < introYear - 2) {
            return 0;
        } else if (year <= introYear) {
            avRating -= 2;
        } else if (year <= introYear + 1) {
            avRating -= 1;
        }

        return Math.max(avRating, 0);
    }



    public String getChassis() {
        return chassis;
    }
    public void setChassis(String chassis) {
        this.chassis = chassis;
    }
    public final String getChassisKey() {
        if (omni) {
            return clan? chassis + "[" + UnitType.getTypeName(unitType) + "]ClanOmni" :
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
        switch (typeName) {
            case "Mek":
                return UnitType.MEK;
            case "Tank":
                return UnitType.TANK;
            case "BattleArmor":
                return UnitType.BATTLE_ARMOR;
            case "Infantry":
                return UnitType.INFANTRY;
            case "ProtoMek":
                return UnitType.PROTOMEK;
            case "VTOL":
                return UnitType.VTOL;
            case "Naval":
                return UnitType.NAVAL;
            case "Gun Emplacement":
                return UnitType.GUN_EMPLACEMENT;
            case "Conventional Fighter":
                return UnitType.CONV_FIGHTER;
            case "AeroSpaceFighter":
                return UnitType.AEROSPACEFIGHTER;
            case "Aero":
                return UnitType.AERO;
            case "Small Craft":
                return UnitType.SMALL_CRAFT;
            case "Dropship":
                return UnitType.DROPSHIP;
            case "Jumpship":
                return UnitType.JUMPSHIP;
            case "Warship":
                return UnitType.WARSHIP;
            case "Space Station":
                return UnitType.SPACE_STATION;
            default:
                return -1;
        }
    }
}

