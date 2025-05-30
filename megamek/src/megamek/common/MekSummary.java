/*
 * MekSummary.java - Copyright (C) 2002-2004 Josh Yockey
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

import java.awt.Image;
import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.client.ui.Base64Image;
import megamek.codeUtilities.StringUtility;
import megamek.common.alphaStrike.ASArcSummary;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.ASSpecialAbilityCollection;
import megamek.common.alphaStrike.ASSpecialAbilityCollector;
import megamek.common.alphaStrike.ASUnitType;
import megamek.common.alphaStrike.AlphaStrikeHelper;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.options.IOptionInfo;
import megamek.common.options.Quirks;
import megamek.logging.MMLogger;

/**
 * The MekSummary of a unit offers compiled information about the unit without having to load the file.
 */
public class MekSummary implements Serializable, ASCardDisplayable {
    private static final MMLogger logger = MMLogger.create(MekSummary.class);

    private String name;
    private String chassis;
    private String clanChassisName;
    private String model;
    private int mulId;
    private String unitType;
    private String unitSubType;
    private String fullAccurateUnitType;
    private Long entityType;
    private Base64Image fluffImage = new Base64Image();
    private boolean omni;
    private boolean military;
    private boolean mountedInfantry;
    private int tankTurrets;
    private File sourceFile;
    private String source;
    private boolean invalid;
    private String techLevel;
    private int techLevelCode;
    private String techBase;
    private boolean failedToLoadEquipment;
    private String entryName; // for files in zips
    private int year;
    private int type;
    private int[] altTypes = new int[] { TechConstants.T_IS_TW_NON_BOX, TechConstants.T_IS_ADVANCED,
                                         TechConstants.T_IS_EXPERIMENTAL }; // tech level constant at standard, advanced, and experimental rules
    // levels
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
    private boolean doomedOnGround;
    private boolean doomedInAtmosphere;
    private boolean doomedInSpace;
    private boolean doomedInExtremeTemp;
    private boolean doomedInVacuum;
    private boolean clan;
    private boolean support;
    private int walkMp;
    private int runMp;
    private int jumpMp;
    private EntityMovementMode moveMode;
    private int totalArmor;
    private int totalInternal;
    private int cockpitType;
    private String engineName;
    private int engineType;
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
    private int mekBays;
    private int mekDoors;
    private double mekUnits;
    private int heavyVehicleBays;
    private int heavyVehicleDoors;
    private double heavyVehicleUnits;
    private int lightVehicleBays;
    private int lightVehicleDoors;
    private double lightVehicleUnits;
    private int protoMekBays;
    private int protoMekDoors;
    private double protoMekUnits;
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
     * Each location can have a separate armor type, but this is used for search purposes. We really only care about
     * which types are present.
     */
    private final HashSet<Integer> armorTypeSet;

    /** The armor type for each location. */
    private int[] armorLoc;

    /** The armor tech type for each location. */
    private int[] armorLocTech;

    /** A unique list of the names of the equipment mounted on this unit. */
    private Vector<String> equipmentNames;

    /**
     * The number of times the piece of equipment in the corresponding equipmentNames list appears.
     */
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
    private UnitRole role = UnitRole.UNDETERMINED;

    public MekSummary() {
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
    public String getFullChassis() {
        return chassis + (StringUtility.isNullOrBlank(clanChassisName) ? "" : " (" + clanChassisName + ")");
    }

    public void setClanChassisName(String name) {
        clanChassisName = name;
    }

    public String getClanChassisName() {
        return clanChassisName;
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

    public boolean isDoomedOnGround() {
        return doomedOnGround;
    }

    public boolean isDoomedInAtmosphere() {
        return doomedInAtmosphere;
    }

    public boolean isDoomedInSpace() {
        return doomedInSpace;
    }

    public boolean isDoomedInExtremeTemp() {
        return doomedInExtremeTemp;
    }

    public boolean isDoomedInVacuum() {
        return doomedInVacuum;
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

    public File getSourceFile() {
        return sourceFile;
    }

    public boolean getInvalid() {
        return invalid;
    }

    public String getTechLevel() {
        return techLevel;
    }

    public int getTechLevelCode() {
        return techLevelCode;
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

    public boolean getMountedInfantry() {
        return mountedInfantry;
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

    public int getMekBays() {
        return mekBays;
    }

    public int getMekDoors() {
        return mekDoors;
    }

    public double getMekUnits() {
        return mekUnits;
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

    public int getProtoMekBays() {
        return protoMekBays;
    }

    public int getProtoMekDoors() {
        return protoMekDoors;
    }

    public double getProtoMekUnits() {
        return protoMekUnits;
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
        return (role == null) ? UnitRole.UNDETERMINED : role;
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

    public void setMountedInfantry(boolean b) {
        mountedInfantry = b;
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

    public void setMekBays(int i) {
        mekBays = i;
    }

    public void setMekDoors(int i) {
        mekDoors = i;
    }

    public void setMekUnits(double d) {
        mekUnits = d;
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

    public void setProtoMekBays(int i) {
        protoMekBays = i;
    }

    public void setProtoMekDoors(int i) {
        protoMekDoors = i;
    }

    public void setProtoMekUnits(double d) {
        protoMekUnits = d;
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
        dropshuttleBays = i;
    }

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

    public void setTechLevelCode(int i) {
        this.techLevelCode = i;
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

    public void setDoomedOnGround(boolean doomedOnGround) {
        this.doomedOnGround = doomedOnGround;
    }

    public void setDoomedInAtmosphere(boolean doomedInAtmosphere) {
        this.doomedInAtmosphere = doomedInAtmosphere;
    }

    public void setDoomedInSpace(boolean doomedInSpace) {
        this.doomedInSpace = doomedInSpace;
    }

    public void setDoomedInExtremeTemp(boolean doomedInExtremeTemp) {
        this.doomedInExtremeTemp = doomedInExtremeTemp;
    }

    public void setDoomedInVacuum(boolean doomedInVacuum) {
        this.doomedInVacuum = doomedInVacuum;
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

    public void setFluffImage(String base64image) {
        fluffImage = new Base64Image(base64image);
    }

    @Override
    public @Nullable Image getFluffImage() {
        return fluffImage.getImage();
    }

    /**
     * Given the list of equipment mounted on this unit, parse it into a unique list of names and the number of times
     * that name appears.
     *
     * @param mountedList A collection of <code>Mounted</code> equipment
     */
    public void setEquipment(List<Mounted<?>> mountedList) {
        equipmentNames = new Vector<>(mountedList.size());
        equipmentQuantities = new Vector<>(mountedList.size());
        for (Mounted<?> mnt : mountedList) {
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
                equipmentQuantities.set(index, equipmentQuantities.get(index) + 1);
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
        Set<String> quirkNameList = quirks.getOptionsList()
                                          .stream()
                                          .filter(IOption::booleanValue)
                                          .map(IOptionInfo::getDisplayableNameWithValue)
                                          .collect(Collectors.toSet());
        quirkNames = String.join(";", quirkNameList);
    }

    public String getQuirkNames() {
        return quirkNames;
    }

    public void setWeaponQuirkNames(Entity entity) {
        Set<String> weaponQuirkNameList = new HashSet<>();
        for (Mounted<?> mounted : entity.getEquipment()) {
            weaponQuirkNameList.addAll(mounted.getQuirks()
                                             .getOptionsList()
                                             .stream()
                                             .filter(IOption::booleanValue)
                                             .map(IOptionInfo::getDisplayableNameWithValue)
                                             .collect(Collectors.toSet()));
        }
        weaponQuirkNames = String.join(";", weaponQuirkNameList);
    }

    public String getWeaponQuirkNames() {
        return weaponQuirkNames;
    }

    public void setTotalArmor(int totalArmor) {
        this.totalArmor = totalArmor;
    }

    public EntityMovementMode getMoveMode() {
        return moveMode;
    }

    public void setMoveMode(EntityMovementMode moveMode) {
        this.moveMode = moveMode;
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
     * Takes the armor type at all locations and creates a set of the armor types.
     *
     * @param locsArmor An array that stores the armor type at each location.
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

    public int getEngineType() {
        return engineType;
    }

    public void setEngineType(int engineType) {
        this.engineType = engineType;
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
        final MekSummary other = (MekSummary) obj;
        // we match on chassis + model + unittype + sourcefile
        return Objects.equals(chassis, other.chassis) &&
                     Objects.equals(model, other.model) &&
                     Objects.equals(unitType, other.unitType) &&
                     Objects.equals(sourceFile, other.sourceFile);
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

    /**
     * Loads and returns the entity for this MekSummary. If the entity cannot be loaded, the error is logged and null is
     * returned.
     *
     * @return The loaded entity or null in case of an error
     */
    public @Nullable Entity loadEntity() {
        try {
            return new MekFileParser(sourceFile, entryName).getEntity();
        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }
    }

    /**
     * Loads and returns the entity for the given full name. If the entity cannot be loaded, the error is logged and
     * null is returned. This is a shortcut for first loading the MekSummary using
     * {@link MekSummaryCache#getMek(String)} and then {@link #loadEntity()}.
     *
     * @return The loaded entity or null in case of an error
     */
    public static @Nullable Entity loadEntity(String fullName) {
        try {
            MekSummary ms = MekSummaryCache.getInstance().getMek(fullName);
            if (ms != null) {
                return new MekFileParser(ms.sourceFile, ms.entryName).getEntity();
            } else {
                String message = String.format("MekSummary entry not found for %s", fullName);
                logger.error(message);
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }
}
