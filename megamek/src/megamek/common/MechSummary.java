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

import megamek.common.alphaStrike.*;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * The MechSummary of a unit offers compiled information about the unit without having to load the file.
 */
public class MechSummary implements Serializable, ASCardDisplayable {

    private String name;
    private String chassis;
    private String model;
    private int mulId;
    private String unitType;
    private String unitSubType;
    private String fullAccurateUnitType;
    private Long entityType;
    private boolean omni;
    private boolean military;
    private int tankTurrets;
    private File sourceFile;
    private String source;
    private boolean invalid;
    private String techLevel;
    private String techBase;
    private boolean failedToLoadEquipment;
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
    private String extinctRange;
    private boolean canon;
    private boolean patchwork;
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
    private int lowerArms;
    private int hands;
    private double troopCarryingSpace;
    private int aSFBays;
    private int aSFDoors;
    private double aSFUnits;
    private int smallCraftBays;
    private int smallCraftDoors;
    private double smallCraftUnits;
    private int dockingCollars;
    private int mechBays;
    private int mechDoors;
    private double mechUnits;
    private int heavyVehicleBays;
    private int heavyVehicleDoors;
    private double heavyVehicleUnits;
    private int lightVehicleBays;
    private int lightVehicleDoors;
    private double lightVehicleUnits;
    private int protoMecheBays;
    private int protoMechDoors;
    private double protoMechUnits;
    private int battleArmorBays;
    private int battleArmorDoors;
    private double battleArmorUnits;
    private int infantryBays;
    private int infantryDoors;
    private double infantryUnits;
    private int superHeavyVehicleBays;
    private int superHeavyVehicleDoors;
    private double superHeavyVehicleUnits;
    private int dropshuttleBays;
    private int dropshuttleDoors;
    private double dropshuttelUnits;
    private int battleArmorHandles;
    private double cargoBayUnits;
    private int navalRepairFacilities;

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

    private String quirkNames;
    private String weaponQuirkNames;

    // AlphaStrike values
    private int pointValue = 0;
    private ASUnitType asUnitType = ASUnitType.UNKNOWN;
    private int size = 0;
    private int tmm = 0;
    private Map<String, Integer> movement = new LinkedHashMap<>();
    private String primaryMovementMode = "";
    private ASDamageVector standardDamage = ASDamageVector.ZERO;
    private int overheat = 0;
    private ASArcSummary frontArc = new ASArcSummary();
    private ASArcSummary leftArc = new ASArcSummary();
    private ASArcSummary rightArc = new ASArcSummary();
    private ASArcSummary rearArc = new ASArcSummary();
    private int threshold;
    private int fullArmor;
    private int fullStructure;
    private int squadSize;
    private ASSpecialAbilityCollection specialAbilities = new ASSpecialAbilityCollection();
    private UnitRole role;

    public MechSummary() {
        armorTypeSet = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    @Override
    public String getChassis() {
        return chassis;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public int getMulId() {
        return mulId;
    }

    public String getUnitType() {
        return unitType;
    }

    public boolean isCanon() {
        return canon;
    }

    public boolean isPatchwork() {
        return patchwork;
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

    public boolean getInvalid() {
        return invalid;
    }

    public String getTechLevel() {
        return techLevel;
    }

    public String getTechBase() {
        return techBase;
    }

    public boolean getFailedToLoadEquipment() {
        return failedToLoadEquipment;
    }

    public String getSource() {
        return source;
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

    public long getEntityType() {
        return entityType;
    }

    public boolean getOmni() {
        return omni;
    }

    public boolean getMilitary() {
        return military;
    }

    public int getTankTurrets() {
        return tankTurrets;
    }

    public int getLowerArms() {
        return lowerArms;
    }

    public int getHands() {
        return hands;
    }

    public double getTroopCarryingSpace() {
        return troopCarryingSpace;
    }

    public int getASFBays() {
        return aSFBays;
    }

    public int getASFDoors() {
        return aSFDoors;
    }

    public double getASFUnits() {
        return aSFUnits;
    }

    public int getSmallCraftBays() {
        return smallCraftBays;
    }

    public int getSmallCraftDoors() {
        return smallCraftDoors;
    }

    public double getSmallCraftUnits() {
        return smallCraftUnits;
    }

    public int getDockingCollars() {
        return dockingCollars;
    }

    public int getMechBays() {
        return mechBays;
    }

    public int getMechDoors() {
        return mechDoors;
    }

    public double getMechUnits() {
        return mechUnits;
    }

    public int getHeavyVehicleBays() {
        return heavyVehicleBays;
    }

    public int getHeavyVehicleDoors() {
        return heavyVehicleDoors;
    }

    public double getHeavyVehicleUnits() {
        return heavyVehicleUnits;
    }

    public int getLightVehicleBays() {
        return lightVehicleBays;
    }

    public int getLightVehicleDoors() {
        return lightVehicleDoors;
    }

    public double getLightVehicleUnits() {
        return lightVehicleUnits;
    }

    public int getProtoMecheBays() {
        return protoMecheBays;
    }

    public int getProtoMechDoors() {
        return protoMechDoors;
    }

    public double getProtoMechUnits() {
        return protoMechUnits;
    }

    public int getBattleArmorBays() {
        return battleArmorBays;
    }

    public int getBattleArmorDoors() {
        return battleArmorDoors;
    }

    public double getBattleArmorUnits() {
        return battleArmorUnits;
    }

    public int getInfantryBays() {
        return infantryBays;
    }

    public int getInfantryDoors() {
        return infantryDoors;
    }

    public double getInfantryUnits() {
        return infantryUnits;
    }

    public int getSuperHeavyVehicleBays() {
        return superHeavyVehicleBays;
    }

    public int getSuperHeavyVehicleDoors() {
        return superHeavyVehicleDoors;
    }

    public double getSuperHeavyVehicleUnits() {
        return superHeavyVehicleUnits;
    }

    public int getDropshuttleBays() {
        return dropshuttleBays;
    }

    public int getDropshuttleDoors() {
        return dropshuttleDoors;
    }

    public double getDropshuttelUnits() {
        return dropshuttelUnits;
    }

    public int getBattleArmorHandles() {
        return battleArmorHandles;
    }

    public double getCargoBayUnits() {
        return cargoBayUnits;
    }

    public int getNavalRepairFacilities() {
        return navalRepairFacilities;
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

    @Override
    public int getPointValue() {
        return pointValue;
    }

    @Override
    public ASUnitType getASUnitType() {
        return asUnitType;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getTMM() {
        return tmm;
    }

    @Override
    public Map<String, Integer> getMovement() {
        return movement;
    }

    @Override
    public String getPrimaryMovementMode() {
        return primaryMovementMode;
    }

    @Override
    public ASDamageVector getStandardDamage() {
        return standardDamage;
    }

    @Override
    public int getOV() {
        return overheat;
    }

    @Override
    public ASArcSummary getFrontArc() {
        return frontArc;
    }

    @Override
    public ASArcSummary getLeftArc() {
        return leftArc;
    }

    @Override
    public ASArcSummary getRightArc() {
        return rightArc;
    }

    @Override
    public ASArcSummary getRearArc() {
        return rearArc;
    }

    @Override
    public int getThreshold() {
        return threshold;
    }

    @Override
    public int getFullArmor() {
        return fullArmor;
    }

    @Override
    public int getFullStructure() {
        return fullStructure;
    }

    @Override
    public int getSquadSize() {
        return squadSize;
    }

    @Override
    public ASSpecialAbilityCollection getSpecialAbilities() {
        return specialAbilities;
    }

    @Override
    public UnitRole getRole() {
        return role;
    }

    public void setFullAccurateUnitType(String type) {
        fullAccurateUnitType = type;
    }

    public void setEntityType(long type) {
        entityType = type;
    }

    public void setOmni(boolean b) {
        omni = b;
    }

    public void setMilitary(boolean b) {
        military = b;
    }

    public void setTankTurrets(int i) {
        tankTurrets = i;
    }

    public void setLowerArms(int i) {
        lowerArms = i;
    }

    public void setHands(int i) {
        hands = i;
    }

    public void setTroopCarryingSpace(double d) {
        troopCarryingSpace = d;
    }

    public void setASFBays(int i) {
        aSFBays = i;
    }

    public void setASFDoors(int i) {
        aSFDoors = i;
    }

    public void setASFUnits(double d) {
        aSFUnits = d;
    }

    public void setSmallCraftBays(int i) {
        smallCraftBays = i;
    }

    public void setSmallCraftDoors(int i) {
        smallCraftDoors = i;
    }

    public void setSmallCraftUnits(double d) {
        smallCraftUnits = d;
    }

    public void setDockingCollars(int i) {
        dockingCollars = i;
    }

    public void setMechBays(int i) {
        mechBays = i;
    }

    public void setMechDoors(int i) {
        mechDoors = i;
    }

    public void setMechUnits(double d) {
        mechUnits = d;
    }

    public void setHeavyVehicleBays(int i) {
        heavyVehicleBays = i;
    }

    public void setHeavyVehicleDoors(int i) {
        heavyVehicleDoors = i;
    }

    public void setHeavyVehicleUnits(double d) {
        heavyVehicleUnits = d;
    }

    public void setLightVehicleBays(int i) {
        lightVehicleBays = i;
    }

    public void setLightVehicleDoors(int i) {
        lightVehicleDoors = i;
    }

    public void setLightVehicleUnits(double d) {
        lightVehicleUnits = d;
    }

    public void setProtoMecheBays(int i) {
        protoMecheBays = i;
    }

    public void setProtoMechDoors(int i) {
        protoMechDoors = i;
    }

    public void setProtoMechUnits(double d) {
        protoMechUnits = d;
    }

    public void setBattleArmorBays(int i) {
        battleArmorBays = i;
    }

    public void setBattleArmorDoors(int i) {
        battleArmorDoors = i;
    }

    public void setBattleArmorUnits(double d) {
        battleArmorUnits = d;
    }

    public void setInfantryBays(int i) {
        infantryBays = i;
    }

    public void setInfantryDoors(int i) {
        infantryDoors = i;
    }

    public void setInfantryUnits(double d) {
        infantryUnits = d;
    }

    public void setSuperHeavyVehicleBays(int i) {
        superHeavyVehicleBays = i;
    }

    public void setSuperHeavyVehicleDoors(int i) {
        superHeavyVehicleDoors = i;
    }

    public void setSuperHeavyVehicleUnits(double d) {
        superHeavyVehicleUnits = d;
    }

    public void setDropshuttleBays(int i) {
        dropshuttleBays = i; }

    public void setDropshuttleDoors(int i) {
        dropshuttleDoors = i;
    }

    public void setDropshuttelUnits(double d) {
        dropshuttelUnits = d;
    }

    public void setBattleArmorHandles(int i) {
        battleArmorHandles = i;
    }

    public void setCargoBayUnits(double d) {
        cargoBayUnits = d;
    }

    public void setNavalRepairFacilities(int i) {
        navalRepairFacilities = i;
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

    public void setInvalid(boolean b) {
        this.invalid = b;
    }

    public void setTechLevel(String s) {
        this.techLevel = s;
    }

    public void setTechBase(String s) {
        this.techBase = s;
    }

    public void setFailedToLoadEquipment(boolean b) {
        this.failedToLoadEquipment = b;
    }

    public void setSource(String sSource) {
        this.source = sSource;
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

    public void setPatchwork(boolean patchwork) {
        this.patchwork = patchwork;
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

    public void setQuirkNames(Quirks quirks) {
        quirkNames = "";
        for (final Enumeration<IOptionGroup> optionGroups = quirks.getGroups(); optionGroups.hasMoreElements();) {
            final IOptionGroup group = optionGroups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                final IOption option = options.nextElement();
                if ((option != null) && option.booleanValue()) {
                    if (!quirkNames.contains(option.getDisplayableNameWithValue())) {
                        quirkNames += option.getDisplayableNameWithValue() + ";";
                    }
                }
            }
        }
    }

    public String getQuirkNames() {
        return quirkNames;
    }

    public void setWeaponQuirkNames(Entity entity) {
        HashMap<Integer, WeaponQuirks> wpnQks = new HashMap<>();
        weaponQuirkNames = "";
        for (Mounted m : entity.getWeaponList()) {
            wpnQks.put(entity.getEquipmentNum(m), m.getQuirks());
        }
        Set<Integer> set = wpnQks.keySet();

        Iterator<Integer> iter = set.iterator();
        while (iter.hasNext()) {
            int key = iter.next();
            WeaponQuirks wpnQuirks = wpnQks.get(key);
            for (Enumeration<IOptionGroup> i = wpnQuirks.getGroups(); i.hasMoreElements(); ) {
                IOptionGroup group = i.nextElement();
                for (Enumeration<IOption> j = group.getSortedOptions(); j.hasMoreElements(); ) {
                    IOption option = j.nextElement();
                    if ((option != null) && option.booleanValue()) {
                        if (!weaponQuirkNames.contains(option.getDisplayableNameWithValue())) {
                            weaponQuirkNames += option.getDisplayableNameWithValue() + ";";
                        }
                    }
                }
            }
        }
    }

    public String getWeaponQuirkNames() {
        return weaponQuirkNames;
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

	public String getExtinctRange() {
		return extinctRange;
	}

    public void setAsUnitType(ASUnitType asUnitType) {
        this.asUnitType = asUnitType;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setTmm(int tmm) {
        this.tmm = tmm;
    }

    public void setMovement(Map<String, Integer> movement) {
        this.movement = movement;
    }

    public void setPrimaryMovementMode(String primaryMovementMode) {
        this.primaryMovementMode = primaryMovementMode;
    }

    public void setStandardDamage(ASDamageVector standardDamage) {
        this.standardDamage = standardDamage;
    }

    public void setOverheat(int overheat) {
        this.overheat = overheat;
    }

    public void setFrontArc(ASArcSummary frontArc) {
        this.frontArc = frontArc;
    }

    public void setLeftArc(ASArcSummary leftArc) {
        this.leftArc = leftArc;
    }

    public void setRightArc(ASArcSummary rightArc) {
        this.rightArc = rightArc;
    }

    public void setRearArc(ASArcSummary rearArc) {
        this.rearArc = rearArc;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void setFullArmor(int fullArmor) {
        this.fullArmor = fullArmor;
    }

    public void setFullStructure(int fullStructure) {
        this.fullStructure = fullStructure;
    }

    public void setSquadSize(int squadSize) {
        this.squadSize = squadSize;
    }

    public void setPointValue(int pointValue) {
        this.pointValue = pointValue;
    }

    public void setSpecialAbilities(ASSpecialAbilityCollection specialAbilities) {
        this.specialAbilities = specialAbilities;
    }

    public void setUnitRole(UnitRole role) {
        this.role = role;
    }

	public void setExtinctRange(String extinctRange) {
		this.extinctRange = extinctRange;
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

    @Override
    public boolean showSUA(BattleForceSUA sua) {
        return !AlphaStrikeHelper.hideSpecial(sua, this);
    }

    @Override
    public String formatSUA(BattleForceSUA sua, String delimiter, ASSpecialAbilityCollector collection) {
        return AlphaStrikeHelper.formatAbility(sua, collection, this, delimiter);
    }
}
