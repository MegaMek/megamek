/*
 * MechSummary.java - Copyright (C) 2002-2004 Josh Yockey
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

/**
 * The MechSummary of a unit offers compiled information about the unit without having to load the file.
 */
public class MechSummary implements Serializable {

    private String name;
    private String chassis;
    private String model;
    private int mulId;
    private String unitType;
    private String unitSubType;
    private String fullAccurateUnitType;
    private File sourceFile;
    private String entryName; // for files in zips
    private int year;
    private int type;
    private int[] altTypes = new int[] { TechConstants.T_IS_TW_NON_BOX, TechConstants.T_IS_ADVANCED,
            TechConstants.T_IS_EXPERIMENTAL }; // tech level constant at standard, advanced, and experimental rules levels
    private double tons;
    private int bv;

    /** The full cost of the unit (including ammo). */
    private long cost;

    /** The dry cost of the unit (excluding ammo). */
    private long dryCost;
    private long altCost;
    private long modified; // for comparison when loading
    private String level;
    private int advTechYear; // year after which the unit is advanced level
    private int stdTechYear; // year after which the unit is standard level
    private boolean canon;
    private boolean clan;
    private boolean support;
    private int walkMp;
    private int runMp;
    private int jumpMp;
    private int totalArmor;
    private int totalInternal;
    private int cockpitType;
    private String engineName;
    private int gyroType;
    private String myomerName;

    /** For BattleArmor, we want to know the weight of an individual suit. */
    private double twSuitTons;
    private double toSuitTons;
    private double suitWeight;

    /** The type of internal structure on this unit **/
    private int internalsType;
    
    /**
     * Each location can have a separate armor type, but this is used for search purposes. We really
     * only care about which types are present.
     */
    private final HashSet<Integer> armorTypeSet;
    
    /** The armor type for each location. */
    private int[] armorLoc;
    
    /** The armor tech type for each location. */
    private int[] armorLocTech;

    /** A unique list of the names of the equipment mounted on this unit. */
    private Vector<String> equipmentNames;

    /** The number of times the piece of equipment in the corresponding equipmentNames list appears. */
    private Vector<Integer> equipmentQuantities;

    public MechSummary() {
        armorTypeSet = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public String getChassis() {
        return chassis;
    }

    public String getModel() {
        return model;
    }

    public int getMulId() {
        return mulId;
    }

    public String getUnitType() {
        return unitType;
    }

    public boolean isCanon() {
        return canon;
    }

    public boolean isClan() {
        return clan;
    }

    public boolean isSupport() {
        return support;
    }

    public String getUnitSubType() {
        return unitSubType;
    }

    public static String determineETypeName(MechSummary ms) {
        switch (ms.getUnitType()) {
            case "BattleArmor":
            case "Infantry":
                return Entity.getEntityMajorTypeName(Entity.ETYPE_INFANTRY);
            case "VTOL":
                return Entity.getEntityMajorTypeName(Entity.ETYPE_VTOL);
            case "Naval":
            case "Gun Emplacement":
            case "Tank":
                return Entity.getEntityMajorTypeName(Entity.ETYPE_TANK);
            case "Mek":
                return Entity.getEntityMajorTypeName(Entity.ETYPE_MECH);
            case "ProtoMek":
                return Entity.getEntityMajorTypeName(Entity.ETYPE_PROTOMECH);
            case "Space Station":
            case "Jumpship":
            case "Dropship":
            case "Small Craft":
            case "Conventional Fighter":
            case "Aero":
                return Entity.getEntityMajorTypeName(Entity.ETYPE_AERO);
            case "Unknown":
                return Entity.getEntityMajorTypeName(-1);
        }
        return Entity.getEntityMajorTypeName(-1);
    }
    
    // This is here for legacy purposes to not break the API
    @Deprecated
    public static String determineUnitType(Entity e) {
        return UnitType.determineUnitType(e);
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public String getEntryName() {
        return entryName;
    }

    public int getYear() {
        return year;
    }

    public int getType() {
        return type;
    }
    
    public int[] getAltTypes() {
        return altTypes;
    }
    
    public int getType(int year) {
        if (year >= stdTechYear) {
            return altTypes[0];
        } else if (year >= advTechYear) {
            return altTypes[1];
        } else {
            return altTypes[2];
        }
    }

    public String getFullAccurateUnitType() {
        return fullAccurateUnitType;
    }

    public double getTons() {
        return tons;
    }

    public double getTOweight() {
        return toSuitTons;
    }

    public double getTWweight() {
        return twSuitTons;
    }

    public int getBV() {
        return bv;
    }

    public long getCost() {
        return cost;
    }

    public long getDryCost() {
        return dryCost;
    }

    public long getAlternateCost() {
        return altCost;
    }

    public long getModified() {
        return modified;
    }

    public String getLevel() {
        return level;
    }
    
    public int getAdvancedTechYear() {
        return advTechYear;
    }

    public int getStandardTechYear() {
        return stdTechYear;
    }
    
    public String getLevel(int year) {
        if (level.equals("F")) {
            return level;
        }
        if (year >= stdTechYear) {
            if (type == TechConstants.T_INTRO_BOXSET) {
                return level;
            } else {
                return String.valueOf(TechConstants.T_SIMPLE_STANDARD + 1);
            }
        } else if (year >= advTechYear) {
            return String.valueOf(TechConstants.T_SIMPLE_ADVANCED + 1);
        } else {
            return String.valueOf(TechConstants.T_SIMPLE_EXPERIMENTAL + 1);
        }
    }

    public void setFullAccurateUnitType(String type) {
        fullAccurateUnitType = type;
    }

    public void setName(String sName) {
        this.name = sName;
    }

    public void setChassis(String sChassis) {
        this.chassis = sChassis;
    }

    public void setModel(String sModel) {
        this.model = sModel;
    }

    public void setMulId(int mulId) {
        this.mulId = mulId;
    }

    public void setUnitType(String sUnitType) {
        this.unitType = sUnitType;
    }

    public void setSourceFile(File sSourceFile) {
        this.sourceFile = sSourceFile;
    }

    public void setEntryName(String sEntryName) {
        this.entryName = sEntryName;
    }

    public void setYear(int nYear) {
        this.year = nYear;
    }

    public void setType(int nType) {
        this.type = nType;
    }
    
    public void setAltTypes(int[] altTypes) {
        this.altTypes = altTypes;
    }
    
    public void setTons(double nTons) {
        this.tons = nTons;
    }

    public void setTOweight(double TOsuitTons) {
        this.toSuitTons = TOsuitTons;
    }

    public void setTWweight(double TWsuitTons) {
        this.twSuitTons = TWsuitTons;
    }

    public void setCost(long nCost) {
        this.cost = nCost;
    }

    public void setDryCost(long nCost) {
        this.dryCost = nCost;
    }

    public void setAlternateCost(long aCost) {
        this.altCost = aCost;
    }

    public void setBV(int nBV) {
        this.bv = nBV;
    }

    public void setModified(long lModified) {
        this.modified = lModified;
    }

    public void setLevel(String level) {
        this.level = level;
    }
    
    public void setAdvancedYear(int year) {
        advTechYear = year;
    }
    
    public void setStandardYear(int year) {
        stdTechYear = year;
    }

    public void setCanon(boolean canon) {
        this.canon = canon;
    }

    public void setClan(boolean clan) {
        this.clan = clan;
    }

    public void setSupport(boolean support) {
        this.support = support;
    }

    public void setUnitSubType(String subType) {
        unitSubType = subType;
    }

    public int getWeightClass() {
        double tons;
        if (getUnitType().equals("BattleArmor")) {
            tons = getSuitWeight();
        } else {
            tons = getTons();
        }
        if (isSupport()) {
            return EntityWeightClass.getSupportWeightClass(this.tons, unitSubType);
        }
        return EntityWeightClass.getWeightClass(tons, getUnitType());
    }

    public int getWalkMp() {
        return walkMp;
    }

    public void setWalkMp(int walkMp) {
        this.walkMp = walkMp;
    }

    public int getRunMp() {
        return runMp;
    }

    public void setRunMp(int runMp) {
        this.runMp = runMp;
    }

    public int getJumpMp() {
        return jumpMp;
    }

    public void setJumpMp(int jumpMp) {
        this.jumpMp = jumpMp;
    }
    
    /**
     * Given the list of equipment mounted on this unit, parse it into a unique
     * list of names and the number of times that name appears.
     * 
     * @param mountedList A collection of <code>Mounted</code> equipment
     */
    public void setEquipment(List<Mounted> mountedList)
    {
        equipmentNames = new Vector<>(mountedList.size());
        equipmentQuantities = new Vector<>(mountedList.size());
        for (Mounted mnt : mountedList)
        {
            // Ignore weapon groups, as they aren't actually real equipment
            if (mnt.isWeaponGroup()) {
                continue;
            }
            String eqName = mnt.getType().getInternalName();
            int index = equipmentNames.indexOf(eqName);
            if (index == -1) { // We haven't seen this piece of equipment before
                equipmentNames.add(eqName);
                equipmentQuantities.add(1);
            } else { // We've seen this before, update count
                equipmentQuantities.set(index, equipmentQuantities.get(index)+1);
            }               
        }
    }
    
    public Vector<String> getEquipmentNames() {
        return equipmentNames;
    }
    
    public Vector<Integer> getEquipmentQuantities() {
        return equipmentQuantities;
    }

    public void setTotalArmor(int totalArmor) {
        this.totalArmor = totalArmor;
    }

    public int getTotalArmor() {
        return totalArmor;
    }

    public void setTotalInternal(int totalInternal) {
        this.totalInternal = totalInternal;
    }

    public int getTotalInternal() {
        return totalInternal;
    }

    public void setInternalsType(int internalsType) {
        this.internalsType = internalsType;
    }

    public int getInternalsType() {
        return internalsType;
    }

    /**
     * Takes the armor type at all locations and creates a set of the armor 
     * types.
     * 
     * @param locsArmor  An array that stores the armor type at each location.
     */
    public void setArmorType(int[] locsArmor) {
        armorTypeSet.clear();
        for (int value : locsArmor) {
            armorTypeSet.add(value);
        }
    }

    public HashSet<Integer> getArmorType() {
        return armorTypeSet;
    }
    
    public int[] getArmorTypes() {
        return armorLoc;
    }
    
    public void setArmorTypes(int[] al) {
        armorLoc = al;
    }
    
    public int[] getArmorTechTypes() {
        return armorLocTech;
    }
    
    public void setArmorTechTypes(int[] att) {
        armorLocTech = att;
    }

    public void setCockpitType(int cockpitType) {
        this.cockpitType = cockpitType;
    }

    public int getCockpitType() {
        return cockpitType;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public int getGyroType() {
        return gyroType;
    }

    public void setGyroType(int gyroType) {
        this.gyroType = gyroType;
    }

    public String getMyomerName() {
        return myomerName;
    }

    public void setMyomerName(String myomerName) {
        this.myomerName = myomerName;
    }

    public double getSuitWeight() {
        return suitWeight;
    }

    public void setSuitWeight(double suitWeight) {
        this.suitWeight = suitWeight;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final MechSummary other = (MechSummary) obj;
        // we match on chassis + model + unittype + sourcefile
        return Objects.equals(chassis, other.chassis) && Objects.equals(model, other.model)
                && Objects.equals(unitType, other.unitType) && Objects.equals(sourceFile, other.sourceFile);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(chassis, model, unitType, sourceFile);
    }
}
