/*
 * MegaMek -
 * Copyright (C) 2018 - The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.verifier;

import java.math.BigInteger;
import java.util.*;

import megamek.common.*;
import megamek.common.util.StringUtil;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.capitalweapons.ScreenLauncherWeapon;

/**
 * Validation and construction data for advanced aerospace units (jumpships, warships, space stations)
 * 
 * @author Neoancient
 *
 */
public class TestAdvancedAerospace extends TestAero {
    
    private final Jumpship vessel;

    public enum CapitalArmor{
        PRIMITIVE(EquipmentType.T_ARMOR_PRIMITIVE_AERO, false),   
        STANDARD(EquipmentType.T_ARMOR_AEROSPACE, false),   
        CLAN_STANDARD(EquipmentType.T_ARMOR_AEROSPACE, true),
        IS_IMP_FERRO_ALUM(EquipmentType.T_ARMOR_LC_FERRO_IMP, false),
        CLAN_IMP_FERRO_ALUM(EquipmentType.T_ARMOR_LC_FERRO_IMP, true),
        IS_FERRO_CARBIDE(EquipmentType.T_ARMOR_LC_FERRO_CARBIDE, false),
        CLAN_FERRO_CARBIDE(EquipmentType.T_ARMOR_LC_FERRO_CARBIDE, true),
        IS_LAMELLOR(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE, false),
        CLAN_LAMELLOR(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE, true);

        /**
         * The type, corresponding to types defined in 
         * <code>EquipmentType</code>.
         */
        public int type;
                
        /**
         * Denotes whether this armor is Clan or not.
         */
        public boolean isClan;
        
        CapitalArmor(int t, boolean c) {
            type = t;
            isClan = c;
        }
        
        /**
         * Given an armor type, return the <code>AerospaceArmor</code> instance that
         * represents that type.
         * 
         * @param t  The armor type.
         * @param c  Whether this armor type is Clan or not.
         * @return   The <code>AeroArmor</code> that corresponds to the given 
         *              type or null if no match was found.
         */
        public static CapitalArmor getArmor(int t, boolean c) {
            for (CapitalArmor a : values()) {
                if (a.type == t && a.isClan == c) {
                    return a;
                }
            }
            return null;
        }
        
        /**
         * Calculates and returns the points per ton of the armor type given the
         * weight of the ship. 
         * 
         * @param vessel The ship
         * @return       The number of points of capital armor per ton
         */
        public double pointsPerTon(Jumpship vessel) {
            double ppt = 0.8;
            if (type == EquipmentType.T_ARMOR_LC_FERRO_IMP) {
                ppt += 0.2;
            } else if (type == EquipmentType.T_ARMOR_LC_FERRO_CARBIDE) {
                ppt += 0.4;
            } else if (type == EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE) {
                ppt += 0.6;
            }
            if (vessel.getWeight() >= 250000) {
                ppt -= 0.4;
                if (isClan) {
                    ppt += 0.1;
                }
            } else if (vessel.getWeight() >= 150000) {
                ppt -= 0.2;
                if (isClan) {
                    ppt += 0.1;
                }
            } else if (isClan) {
                ppt += 0.2;
            }
            // Deal with potential rounding errors
            ppt = Math.round(ppt * 10.0) / 10.0;
            if (type == EquipmentType.T_ARMOR_PRIMITIVE_AERO) {
                ppt *= EquipmentType.armorPointMultipliers[EquipmentType.T_ARMOR_PRIMITIVE_AERO];
            }
            return ppt;
        }
        
        /**
         * @return The <code>MiscType</code> for this armor.
         */
        public EquipmentType getArmorEqType() {
            String name = EquipmentType.getArmorTypeName(type, isClan);
            return EquipmentType.get(name);
        }
    }

    /**
     * Filters all capital armor according to given tech constraints
     * 
     * @param techManager Constraints used to filter the possible armor types
     * @return A list of all armors that meet the tech constraints
     */
    public static List<EquipmentType> legalArmorsFor(ITechManager techManager, boolean primitive) {
        if (primitive) {
            return Collections.singletonList(CapitalArmor.PRIMITIVE.getArmorEqType());
        }
        List<EquipmentType> retVal = new ArrayList<>();
        for (CapitalArmor armor : CapitalArmor.values()) {
            final EquipmentType eq = armor.getArmorEqType();
            if ((null != eq) && techManager.isLegal(eq)) {
                retVal.add(eq);
            }
        }
        return retVal;
    }
    
    public static int maxArmorPoints(Jumpship vessel) {
        CapitalArmor a = CapitalArmor.getArmor(vessel.getArmorType(0),
                TechConstants.isClan(vessel.getArmorTechLevel(0)));
        if (null == a) {
            return 0;
        }
        // The ship gets a number of armor points equal to 10% of the SI, rounded normally, per facing.
        double freeSI = Math.round(vessel.get0SI() / 10.0) * 6;
        // Primitive jumpships multiply the armor by a factor of 0.66. Per errata, the armor is calculated
        // based on standard armor then rounded down, and the free SI armor is rounded down separately.
        if (vessel.isPrimitive()) {
            return (int) (Math.floor(CapitalArmor.STANDARD.pointsPerTon(vessel)
                    * maxArmorWeight(vessel) * 0.66)
                + Math.floor(freeSI * 0.66));
        }
        return (int) Math.floor(a.pointsPerTon(vessel) * maxArmorWeight(vessel) + freeSI);
    }
    
    /**
     *  Computes the maximum number armor level in tons
     *   
     */
    public static double maxArmorWeight(Jumpship vessel) {
        // max armor tonnage is based on SI weight
        if (vessel.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            // SI weight (SI/1000) / 50
            return floor(vessel.get0SI() * vessel.getWeight() / 50000.0, Ceil.HALFTON);
        } else if (vessel.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            // SI weight (SI/100) / 3 + 60
            return floor(vessel.get0SI() * vessel.getWeight() / 300.0 + 60, Ceil.HALFTON);
        } else {
            // SI weight (SI/150) / 12
            return floor(vessel.get0SI() * vessel.getWeight() / 1800.0, Ceil.HALFTON);
        }
    }
    
    public static double armorPointsPerTon(Jumpship vessel, int at, boolean clan) {
        CapitalArmor arm = CapitalArmor.getArmor(at, clan);
        if (null != arm) {
            return arm.pointsPerTon(vessel);
        } else {
            return 0;
        }
    }
    
    /**
     * Computes the amount of weight required for fire control systems and power distribution
     * systems for exceeding the base limit of weapons per firing arc.
     * 
     * @param vessel The advanced aerospace unit in question
     * @return   Returns a <code>double</code> array, where each element corresponds to a 
     *           location and the value is the extra tonnage required by exceeding the base
     *           allotment
     */
    public static double[] extraSlotCost(Jumpship vessel) {
        int slotsPerArc = 12;
        int arcs = vessel.locations();
        if (vessel.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            slotsPerArc = 20;
        } else if (vessel.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            slotsPerArc = 20;
        }
        int[] weaponsPerArc = new int[arcs];
        double[] weaponTonnage = new double[arcs];
        boolean hasNC3 = vessel.hasWorkingMisc(MiscType.F_NAVAL_C3);

        for (Mounted m : vessel.getEquipment()) {
            if (usesWeaponSlot(vessel, m.getType())) {
                int arc = m.getLocation();
                if (arc < 0) {
                    continue;
                }
                if ((m.getType() instanceof WeaponType)
                        && m.getType().hasFlag(WeaponType.F_MASS_DRIVER)) {
                    weaponsPerArc[arc] += 10;
                } else {
                    weaponsPerArc[arc]++;
                }
                weaponTonnage[arc] += m.getTonnage();
            }
        }
        double[] retVal = new double[arcs];
        for (int arc = 0; arc < arcs; arc++) {
            int excess = (weaponsPerArc[arc] - 1) / slotsPerArc;
            if (excess > 0) {
                retVal[arc] = ceil(excess * weaponTonnage[arc] / 10.0, Ceil.HALFTON);
            }
            if (hasNC3) {
                retVal[arc] *= 2;
            }
        }
        return retVal;
    }
    
    public static int getMinTonnage(Jumpship vessel) {
        switch (vessel.getDriveCoreType()) {
            case Jumpship.DRIVE_CORE_COMPACT:
                return 100000;
            case Jumpship.DRIVE_CORE_SUBCOMPACT:
                return 5000;
            case Jumpship.DRIVE_CORE_NONE:
                return 2000;
            case Jumpship.DRIVE_CORE_STANDARD:
            case Jumpship.DRIVE_CORE_PRIMITIVE:
            default:
                return 50000;
        }
    }
    
    public static int getWeightIncrement(Jumpship vessel) {
        switch (vessel.getDriveCoreType()) {
            case Jumpship.DRIVE_CORE_COMPACT:
                return 10000;
            case Jumpship.DRIVE_CORE_SUBCOMPACT:
                return 100;
            case Jumpship.DRIVE_CORE_NONE:
                return 500;
            case Jumpship.DRIVE_CORE_STANDARD:
            case Jumpship.DRIVE_CORE_PRIMITIVE:
            default:
                return 1000;
        }
    }
    
    /**
     * Computes the weight of the engine.
     * 
     * @param vessel  The ship
     * @return        The weight of the engine in tons
     */
    public static double calculateEngineTonnage(Jumpship vessel) {
        if (vessel.hasStationKeepingDrive()) {
            return round(vessel.getWeight() * 0.012, Ceil.HALFTON);
        } else if (vessel.isPrimitive()) {
            return round(vessel.getWeight() * vessel.getOriginalWalkMP()
                    * primitiveEngineMultiplier(vessel.getOriginalBuildYear()), Ceil.HALFTON);
        } else {
            return round(vessel.getWeight() * vessel.getOriginalWalkMP() * 0.06, Ceil.HALFTON);
        }
    }
    
    /**
     * @param year The original construction year of the jumpship chassis
     * @return     The engine weight multiplier for the primitive jumpship.
     */
    public static double primitiveEngineMultiplier(int year) {
        if (year >= 2300) {
            return 0.06;
        } else if (year >= 2251) {
            return 0.066;
        } else if (year >= 2201) {
            return 0.084;
        } else if (year >= 2151) {
            return 0.102;
        } else {
            return 0.12;
        }
    }
    
    /**
     * @param vessel An advanced aerospace unit
     * @return       The number of heat sinks that are accounted for in the engine weight.
     */
    public static int weightFreeHeatSinks(Jumpship vessel) {
        return (int) Math.floor(45 + Math.sqrt(calculateEngineTonnage(vessel)
                * (vessel.isPrimitive() ?  1 : 2)));
    }
    
    /**
     * The maximum number of docking hardpoints (collars) depends on the tonnage of the vessel. Naval
     * Repair Facilities and Dropshuttle bays count against the total and reduce the maximum number
     * by two each.
     * 
     * @param vessel The ship
     * @return       The maximum number of docking hardpoints (collars) that can be mounted on the ship.
     */
    public static int getMaxDockingHardpoints(Jumpship vessel) {
        // minimum of 50ktons
        if (vessel.getWeight() < 50000) {
            return 0;
        }
        int max = (int) Math.ceil(vessel.getWeight() / 50000);
        for (Bay bay : vessel.getTransportBays()) {
            max -= bay.hardpointCost();
        }
        return Math.max(max, 0);
    }
    
    /**
     * @param vessel The ship
     * @return       The maximum number of gravity decks that can be mounted on the ship.
     */
    public static int getMaxGravDecks(Jumpship vessel) {
        return 3 + (int) Math.ceil(vessel.getWeight() / 100000);
    }
    
    /**
     * @param vessel The ship
     * @return       The maximum diameter (in meters) for a grav deck mounted on the unit.
     */
    public static int getMaxGravDeckDiameter(Jumpship vessel) {
        // TacOps, p. 314 says that space stations can mount grav decks up to 1500 m, but the
        // weight breakdown gives the largest category as 250-1000 m. I went with the larger
        // size here.
        if (vessel.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            return 1500;
        }
        return 250;
    }
    
    /**
     * @return Minimum crew requirements based on unit type and equipment crew requirements.
     */
    public static int minimumBaseCrew(Jumpship vessel) {
        int crew;
        if (vessel.hasETypeFlag(Entity.ETYPE_WARSHIP)
                || vessel.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            crew = 45 + (int) Math.ceil(vessel.getWeight() / 5000);
        } else {
            crew = 6 + (int) Math.ceil(vessel.getWeight() / 20000);
        }
        for (Mounted m : vessel.getMisc()) {
            crew += equipmentCrewRequirements(m);
        }
        return crew;
    }
        
    /**
     * One gunner is required for each capital weapon and each six standard scale weapons, rounding up
     * @return The vessel's minimum gunner requirements.
     */
    public static int requiredGunners(Jumpship vessel) {
        int capitalWeapons = 0;
        int stdWeapons = 0;
        for (Mounted m : vessel.getTotalWeaponList()) {
            if ((m.getType() instanceof BayWeapon)
                    || (((WeaponType) m.getType()).getLongRange() <= 1)) {
                continue;
            }
            if (m.getType().hasFlag(WeaponType.F_MASS_DRIVER)) {
                capitalWeapons += 10;
            } else if (((WeaponType) m.getType()).isCapital()
                    || (m.getType() instanceof ScreenLauncherWeapon)) {
                capitalWeapons++;
            } else {
                stdWeapons++;
            }
        }
        return capitalWeapons + (int) Math.ceil(stdWeapons / 6.0);
    }
    
    public TestAdvancedAerospace(Jumpship vessel, TestEntityOption option, String fs) {
        super(vessel, option, fs);
        
        this.vessel = vessel;
    }

    @Override
    public Entity getEntity() {
        return vessel;
    }

    @Override
    public boolean isTank() {
        return false;
    }

    @Override
    public boolean isMech() {
        return false;
    }
    
    @Override
    public boolean isAero() {
        return true;
    }
    
    @Override
    public boolean isSmallCraft() {
        return false;
    }
    
    @Override
    public boolean isAdvancedAerospace() {
        return true;
    }

    @Override
    public double getWeightControls() {
        if (vessel.isPrimitive()) {
            return ceil(vessel.getWeight()
                    * primitiveControlMultiplier(vessel.getOriginalBuildYear()), Ceil.TON);
        } else if (vessel.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            return ceil(vessel.getWeight() * 0.001, Ceil.TON);
        } else {
            return ceil(vessel.getWeight() * 0.0025, Ceil.TON);
        }
    }

    /**
     * @param year The original construction year of the jumpship chassis
     * @return     The control weight multiplier for the primitive jumpship.
     */
    public static double primitiveControlMultiplier(int year) {
        if (year >= 2300) {
            return 0.0025;
        } else if (year >= 2251) {
            return 0.00275;
        } else if (year >= 2201) {
            return 0.0035;
        } else if (year >= 2151) {
            return 0.005;
        } else {
            return 0.00625;
        }
    }
    
    @Override
    public double getWeightEngine() {
        return calculateEngineTonnage(vessel);
    }
    
    @Override
    public String printWeightEngine() {
        return StringUtil.makeLength("Engine: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightEngine()) + "\n";
    }
    
    public double getWeightKFDrive() {
        return vessel.getJumpDriveWeight();
    }

    public String printWeightKFDrive() {
        return StringUtil.makeLength("K/F Drive Core: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightKFDrive()) + "\n";
    }
    
    public double getWeightSail() {
        if (!vessel.hasSail()) {
            return 0;
        }
        if (vessel.isPrimitive()) {
            if (vessel.getOriginalBuildYear() < 2230) {
                return Math.ceil(vessel.getWeight() / 2000) + 300;
            } else if (vessel.getOriginalBuildYear() < 2260) {
                return Math.ceil(vessel.getWeight() / 4000) + 150;
            } else if (vessel.getOriginalBuildYear() < 2300) {
                return Math.ceil(vessel.getWeight() / 8000) + 75;
            } else {
                return Math.ceil(vessel.getWeight() / 20000) + 30;
            }
        } else if (vessel.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            return Math.ceil(vessel.getWeight() / 20000) + 30;
        } else {
            // Space stations with energy collection sail use jumpship formula.
            return Math.ceil(vessel.getWeight() / 7500) + 30;
        }
    }

    public String printWeightSail() {
        return StringUtil.makeLength("Jump Sail: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightSail()) + "\n";
    }
    
    @Override
    public double getWeightFuel() {
        // Fuel tanks and pumps add an additional 2%, rounded to the nearest ton
        double pumpWeight = RoundWeight.nextTon(vessel.getFuelTonnage() * 0.02);
        return vessel.getFuelTonnage() + pumpWeight;
    }

    @Override
    public int getCountHeatSinks() {
        return vessel.getHeatSinks();
    }

    @Override
    public double getWeightHeatSinks() {
        return Math.max(vessel.getHeatSinks() - weightFreeHeatSinks(vessel), 0);        
    }

    // Bays can store multiple tons of ammo in a single slot.
    @Override
    public double getWeightAmmo() {
        double weight = 0.0;
        for (Mounted m : getEntity().getAmmo()) {

            // One Shot Ammo
            if (m.getLocation() == Entity.LOC_NONE) {
                continue;
            }

            AmmoType mt = (AmmoType) m.getType();
            int slots = (int) Math.ceil((double) m.getBaseShotsLeft() / mt.getShots());
            weight += ceil(m.getTonnage() * slots, Ceil.HALFTON);
        }
        return weight;
    }
    
    /**
     * @return The combined weight of grav decks mounted on the vessel
     */
    public double getWeightGravDecks() {
        return vessel.getGravDeck() * 50
            + vessel.getGravDeckLarge() * 100
            + vessel.getGravDeckHuge() * 500;
    }
    
    public String printWeightGravDecks() {
        return StringUtil.makeLength("Gravity Decks: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightGravDecks()) + "\n";
    }
    
    /**
     * @return The combined weight of all lifeboats and escape pods
     */
    public double getWeightLifeBoats() {
        // 7 tons each for life boats and escape pods, which includes the 5-ton vehicle and a
        // 2-ton launch mechanism
        return (vessel.getLifeBoats() + vessel.getEscapePods()) * 7;
    }
    
    public String printWeightLifeBoats() {
        return StringUtil.makeLength("Life Boats/Escape Pods: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightLifeBoats()) + "\n";
    }
    
    /**
     * 
     * @return Extra fire control system weight for exceeding base slot limit per firing arc
     */
    public double getWeightFireControl() {
        double weight = 0.0;
        for (double extra : extraSlotCost(vessel)) {
            weight += extra;
        }
        return weight;
    }

    public String printWeightFireControl() {
        return StringUtil.makeLength("Extra Fire Control: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightFireControl()) + "\n";
    }
    
    /**
     * 
     * @return The weight of the lithium fusion battery (0 if not present).
     */
    public double getWeightLFBattery() {
        if (vessel.hasLF()) {
            return vessel.getWeight() * 0.01;
        }
        return 0.0;
    }

    public String printWeightLFBattery() {
        return StringUtil.makeLength("LF Battery: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightLFBattery()) + "\n";
    }
    
    public double getWeightHardpoints() {
        return 1000 * vessel.getDockingCollars().size();
    }
    
    public String printWeightHardpoints() {
        return StringUtil.makeLength("Docking Hardpoints: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightHardpoints()) + "\n";
    }
    
    @Override
    public StringBuffer printWeapon() {
        if (!getEntity().usesWeaponBays()) {
            return super.printWeapon();
        }
        StringBuffer buffer = new StringBuffer();
        for (Mounted m : getEntity().getWeaponBayList()) {
            buffer.append(m.getName()).append(" ")
                .append(getLocationAbbr(m.getLocation()));
            if (m.isRearMounted()) {
                buffer.append(" (R)");
            }
            buffer.append("\n");
            for (Integer wNum : m.getBayWeapons()) {
                final Mounted w = getEntity().getEquipment(wNum);
                buffer.append("   ").append(StringUtil.makeLength(w.getName(),
                        getPrintSize() - 25)).append(w.getTonnage())
                    .append("\n");
            }
            for (Integer aNum : m.getBayAmmo()) {
                final Mounted a = getEntity().getEquipment(aNum);
                double weight = ceil(a.getTonnage()
                        * a.getBaseShotsLeft() / ((AmmoType) a.getType()).getShots(), Ceil.HALFTON);
                buffer.append("   ").append(StringUtil.makeLength(a.getName(),
                        getPrintSize() - 25)).append(weight).append("\n");
            }
        }
        return buffer;
    }

    @Override
    public StringBuffer printAmmo() {
        if (!vessel.usesWeaponBays()) {
            return super.printAmmo();
        }
        return new StringBuffer();
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return vessel.getHeatType() == Aero.HEAT_DOUBLE;
    }

    @Override
    public String printWeightControls() {
        StringBuffer retVal = new StringBuffer(StringUtil.makeLength(
                "Control Systems:", getPrintSize() - 5));
        retVal.append(makeWeightString(getWeightControls()));
        retVal.append("\n");
        return retVal.toString();
    }
        
    @Override
    public String printWeightFuel() {
        StringBuffer retVal = new StringBuffer(StringUtil.makeLength(
                "Fuel: ", getPrintSize() - 5));
        retVal.append(makeWeightString(getWeightFuel()));
        retVal.append("\n");
        return retVal.toString();
    }

    @Override
    public Aero getAero() {
        return vessel;
    }
    
    public Jumpship getAdvancedAerospace() {
        return vessel;
    }

    @Override
    public String printArmorLocProp(int loc, int wert) {
        return " is greater than " + Integer.toString(wert) + "!";
    }

    /**
     * Checks to see if this unit has valid armor assignment.
     * 
     * @param buff
     * @return
     */
    @Override
    public boolean correctArmor(StringBuffer buff) {
        boolean correct = true;
        double maxArmor = maxArmorWeight(vessel);
        if (vessel.getLabArmorTonnage() > maxArmor) {
            buff.append("Total armor, ")
                    .append(vessel.getLabArmorTonnage())
                    .append(" tons, is greater than the maximum: ")
                    .append(maxArmor).append("\n");
            correct = false;
        }

        return correct ;
    }
    
    /**
     * Checks that the heatsink type is a legal value.
     * 
     * @param buff
     * @return
     */
    @Override
    public boolean correctHeatSinks(StringBuffer buff) {
        if ((vessel.getHeatType() != Aero.HEAT_SINGLE) 
                && (vessel.getHeatType() != Aero.HEAT_DOUBLE)) {
            buff.append("Invalid heatsink type!  Valid types are "
                    + Aero.HEAT_SINGLE + " and " + Aero.HEAT_DOUBLE
                    + ".  Found " + vessel.getHeatType() + ".");
            return false;
        }
        return true;
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        boolean correct = true;
        
        if (skip()) {
            return true;
        }
        if (!correctWeight(buff)) {
            buff.insert(0, printTechLevel() + printShortMovement());
            buff.append(printWeightCalculation());
            correct = false;
        }
        if (getCountHeatSinks() < weightFreeHeatSinks(vessel)) {
            buff.append("Heat Sinks:\n");
            buff.append(" Total     " + getCountHeatSinks() + "\n");
            buff.append(" Required  " + weightFreeHeatSinks(vessel)
                    + "\n");
            correct = false;
        }                
        
        if (showCorrectArmor() && !correctArmor(buff)) {
            correct = false;
        }
        if (showFailedEquip() && hasFailedEquipment(buff)) {
            correct = false;
        }
        
        correct &= !hasIllegalTechLevels(buff, ammoTechLvl);
        correct &= !hasIllegalEquipmentCombinations(buff);
        correct &= correctHeatSinks(buff);
        correct &= correctCrew(buff);
        correct &= correctGravDecks(buff);
        correct &= correctBays(buff);
        correct &= correctCriticals(buff);

        return correct;
    }

    @Override
    public boolean hasIllegalEquipmentCombinations(StringBuffer buff) {
        boolean illegal = false;
        
        // Make sure all bays have at least one weapon and that there are at least
        // ten shots of ammo for each ammo-using weapon in the bay.
        for (Mounted bay : vessel.getWeaponBayList()) {
            if (bay.getBayWeapons().size() == 0) {
                buff.append("Bay ").append(bay.getName()).append(" has no weapons\n");
                illegal = true;
            }
            Map<Integer,Integer> ammoWeaponCount = new HashMap<>();
            Map<Integer,Integer> ammoTypeCount = new HashMap<>();
            for (Integer wNum : bay.getBayWeapons()) {
                final Mounted w = vessel.getEquipment(wNum);
                if (w.isOneShotWeapon()) {
                    continue;
                }
                if (w.getType() instanceof WeaponType) {
                    ammoWeaponCount.merge(((WeaponType) w.getType()).getAmmoType(), 1, Integer::sum);
                } else {
                    buff.append(w.getName()).append(" in bay ").append(bay.getName()).append(" is not a weapon\n");
                    illegal = true;
                }
            }
            for (Integer aNum : bay.getBayAmmo()) {
                final Mounted a = vessel.getEquipment(aNum);
                if (a.getType() instanceof AmmoType) {
                    ammoTypeCount.merge(((AmmoType) a.getType()).getAmmoType(), a.getUsableShotsLeft(),
                            Integer::sum);
                } else {
                    buff.append(a.getName()).append(" in bay ").append(bay.getName()).append(" is not ammo\n");
                    illegal = true;
                }
            }
            for (Integer at : ammoWeaponCount.keySet()) {
                if (at != AmmoType.T_NA) {
                    int needed = ammoWeaponCount.get(at) * 10;
                    if ((at == AmmoType.T_AC_ULTRA) || (at == AmmoType.T_AC_ULTRA_THB)) {
                        needed *= 2;
                    } else if ((at == AmmoType.T_AC_ROTARY)) {
                        needed *= 6;
                    }
                    if (!ammoTypeCount.containsKey(at)
                            || ammoTypeCount.get(at) < needed) {
                        buff.append("Bay ").append(bay.getName()).append(" does not have the minimum 10 shots of ammo for each weapon\n");
                        illegal = true;
                        break;
                    }
                }
            }
            for (Integer at : ammoTypeCount.keySet()) {
                if (!ammoWeaponCount.containsKey(at)) {
                    buff.append("Bay ").append(bay.getName()).append(" has ammo for a weapon not in the bay\n");
                    illegal = true;
                    break;
                }
            }
        }
        
        // Count lateral weapons to make sure both sides match
        Map<EquipmentType,Integer> leftFwd = new HashMap<>();
        Map<EquipmentType,Integer> leftAft = new HashMap<>();
        Map<EquipmentType,Integer> leftBroad = new HashMap<>();
        Map<EquipmentType,Integer> rightFwd = new HashMap<>();
        Map<EquipmentType,Integer> rightAft = new HashMap<>();
        Map<EquipmentType,Integer> rightBroad = new HashMap<>();
        Map<Integer,Integer> massDriversPerArc = new HashMap<>();
        
        BigInteger typeFlag = MiscType.F_JS_EQUIPMENT;
        if (vessel.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            typeFlag = MiscType.F_WS_EQUIPMENT;
        } else if (vessel.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            typeFlag = MiscType.F_SS_EQUIPMENT;
        }
        for (Mounted m : vessel.getEquipment()) {
            if (m.getType() instanceof MiscType) {
                if (!m.getType().hasFlag(typeFlag)) {
                    buff.append("Cannot mount " + m.getType().getName() + "\n");
                    illegal = true;
                }
            } else if (m.getType() instanceof WeaponType) {
                if (m.getType().hasFlag(WeaponType.F_MASS_DRIVER)
                        && !vessel.hasETypeFlag(Entity.ETYPE_WARSHIP)
                        && !vessel.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
                    buff.append("A mass driver may not be mounted on a Jumpship.\n");
                    illegal = true;
                }
                switch (m.getLocation()) {
                    case Jumpship.LOC_FLS:
                        leftFwd.merge(m.getType(), 1, Integer::sum);
                        break;
                    case Jumpship.LOC_FRS:
                        rightFwd.merge(m.getType(), 1, Integer::sum);
                        break;
                    case Jumpship.LOC_ALS:
                        leftAft.merge(m.getType(), 1, Integer::sum);
                        break;
                    case Jumpship.LOC_ARS:
                        rightAft.merge(m.getType(), 1, Integer::sum);
                        break;
                    case Warship.LOC_LBS:
                        leftBroad.merge(m.getType(), 1, Integer::sum);
                        break;
                    case Warship.LOC_RBS:
                        rightBroad.merge(m.getType(), 1, Integer::sum);
                        break;
                }
                if (!isAeroWeapon(m.getType(), vessel)) {
                    buff.append("Cannot mount " + m.getType().getName() + "\n");
                    illegal = true;
                }
                if ((m.getType().hasFlag(WeaponType.F_MASS_DRIVER)
                        && !(m.getType() instanceof BayWeapon))) {
                    massDriversPerArc.merge(m.getLocation(), 1, Integer::sum);
                    int at = ((WeaponType) m.getType()).getAmmoType();
                    if ((at == AmmoType.T_HMASS) && vessel.getWeight() < 2000000) {
                        buff.append("Minimum vessel tonnage for ")
                            .append(m.getType().getName()).append(" is 2,000,000 tons\n");
                        illegal = true;
                    } else if ((at == AmmoType.T_MMASS) && vessel.getWeight() < 1500000) {
                        buff.append("Minimum vessel tonnage for ")
                            .append(m.getType().getName()).append(" is 1,500,000 tons\n");
                        illegal = true;
                    } else if ((at == AmmoType.T_LMASS) && vessel.getWeight() < 750000) {
                        buff.append("Minimum vessel tonnage for ")
                            .append(m.getType().getName()).append(" is 750,000 tons\n");
                        illegal = true;
                    }
                }
            }
        }
        boolean lateralMatch = true;
        // Forward weapons
        for (EquipmentType eq : leftFwd.keySet()) {
            if (!rightFwd.containsKey(eq) || !leftFwd.get(eq).equals(rightFwd.get(eq))) {
                lateralMatch = false;
                break;
            }
        }
        if (lateralMatch) {
            //We've already checked counts, so in the reverse direction we only need to see if there's
            //anything not found on the other side.
            for (EquipmentType eq : rightFwd.keySet()) {
                if (!leftFwd.containsKey(eq)) {
                    lateralMatch = false;
                    break;
                }
            }
        }
        // Aft weapons
        if (lateralMatch) {
            for (EquipmentType eq : leftAft.keySet()) {
                if (!rightAft.containsKey(eq) || !leftAft.get(eq).equals(rightAft.get(eq))) {
                    lateralMatch = false;
                    break;
                }
            }
        }
        if (lateralMatch) {
            for (EquipmentType eq : rightAft.keySet()) {
                if (!leftAft.containsKey(eq)) {
                    lateralMatch = false;
                    break;
                }
            }
        }
        // Broadside (warships)
        if (lateralMatch) {
            for (EquipmentType eq : leftBroad.keySet()) {
                if (!rightBroad.containsKey(eq) || !leftBroad.get(eq).equals(rightBroad.get(eq))) {
                    lateralMatch = false;
                    break;
                }
            }
        }
        if (lateralMatch) {
            for (EquipmentType eq : rightBroad.keySet()) {
                if (!leftBroad.containsKey(eq)) {
                    lateralMatch = false;
                    break;
                }
            }
        }
        if (!lateralMatch) {
            buff.append("Left and right side weapon loads do not match.\n");
            illegal = true;
        }
        
        for (Map.Entry<Integer, Integer> entry : massDriversPerArc.entrySet()) {
            if (vessel.hasETypeFlag(Entity.ETYPE_WARSHIP)
                    && (entry.getKey() != Warship.LOC_NOSE)
                    && (entry.getValue() > 0)) {
                buff.append("A warship may only mount a mass driver in the nose firing arc.\n");
                illegal = true;
            } else if (entry.getValue() > 1) {
                buff.append("A ship may not mount more than one mass driver in a firing arc.\n");
                illegal = true;
            }
        }

        int bayDoors = 0;
        for (Bay bay : vessel.getTransportBays()) {
            bayDoors += bay.getDoors();
            if (bay.getDoors() == 0) {
                BayData data = BayData.getBayType(bay);
                if ((data != null) && !data.isCargoBay() && !data.isInfantryBay()) {
                    buff.append("Transport bays other than cargo and infantry require at least one door.\n");
                    illegal = true;
                }
            }
        }
        if (bayDoors > maxBayDoors(vessel)) {
            buff.append("Exceeds maximum number of bay doors.\n");
            illegal = true;
        }

        return illegal;
    }
    
    /**
     * Checks that the unit meets minimum crew and quarters requirements.
     * @param buffer Where to write messages explaining failures.
     * @return  true if the crew data is valid.
     */
    public boolean correctCrew(StringBuffer buffer) {
        boolean illegal = false;
        int crewSize = vessel.getNCrew() - vessel.getBayPersonnel();
        int reqCrew = minimumBaseCrew(vessel) + requiredGunners(vessel);
        if (crewSize < reqCrew) {
            buffer.append("Requires " + reqCrew + " crew and only has " + crewSize + "\n");
            illegal = true;
        }
        if (vessel.getNOfficers() < Math.ceil(reqCrew / 6.0)) {
            buffer.append("Requires at least " + (int) Math.ceil(reqCrew / 6.0) + " officers\n");
            illegal = true;
        }
        crewSize += vessel.getNPassenger();
        crewSize += vessel.getNMarines();
        crewSize += vessel.getNBattleArmor();
        int quarters = 0;
        for (Bay bay : vessel.getTransportBays()) {
            Quarters q = Quarters.getQuartersForBay(bay);
            if (null != q) {
                quarters += bay.getCapacity();
            }
        }
        if (quarters < crewSize) {
            buffer.append("Requires quarters for " + crewSize + " crew but only has " + quarters + "\n");
            illegal = true;
        }
        return !illegal;
    }

    /**
     * Checks that the unit does not exceed the maximum number or size of gravity decks.
     * @param buffer Where to write messages explaining failures.
     * @return  true if the crew data is valid.
     */
    public boolean correctGravDecks(StringBuffer buffer) {
        boolean illegal = false;
        if (vessel.getGravDecks().size() > getMaxGravDecks(vessel)) {
            buffer.append("Exceeds maximum " + getMaxGravDecks(vessel) + " gravity decks.\n");
            illegal = true;
        }
        int maxSize = Jumpship.GRAV_DECK_LARGE_MAX;
        if (vessel.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            maxSize = Jumpship.GRAV_DECK_HUGE_MAX;
        }
        for (Integer dia : vessel.getGravDecks()) {
            if (dia > maxSize) {
                buffer.append("Maximum grav deck diameter is " + maxSize + "\n");
                illegal = true;
                break;
            }
        }
        return !illegal;
    }
    
    /**
     * Checks that the unit does not have more than one naval repair facility or dropshuttle bay
     * per facing, all are assigned a facing, and non-stations have no more than one repair facility.
     * 
     * @param buffer Where to write messages explaining failures.
     * @return  true if the bay data is valid
     */
    public boolean correctBays(StringBuffer buffer) {
        boolean legal = true;
        
        Set<Integer> facings = new HashSet<>();
        int repairCount = 0;
        for (Bay bay : vessel.getTransportBays()) {
            if (bay.hardpointCost() > 0) {
                if ((bay.getFacing() < 0) || (bay.getFacing() >= Warship.LOC_LBS)) {
                    buffer.append(bay.getType() + " is not assigned a legal armor facing.\n");
                    legal = false;
                } else if (facings.contains(bay.getFacing())) {
                    buffer.append("Exceeds maximum of one repair facility or dropshuttle bay per armor facing.\n");
                    legal = false;
                }
                facings.add(bay.getFacing());
                if (bay instanceof NavalRepairFacility) {
                    repairCount++;
                }
            }
        }
        if ((repairCount > 1) && !vessel.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            buffer.append("Only a space station may mount multiple naval repair facilities.\n");
            legal = false;
        }
        
        return legal;
    }
    
    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Advanced Aerospace: ").append(vessel.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");        
        buff.append(printTechLevel());
        buff.append("Intro year: ").append(getEntity().getYear()).append("\n");
        buff.append(printSource());
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true)) {
            buff.append("Weight: ").append(getWeight()).append(" (")
                    .append(calculateWeight()).append(")\n");
        }
        buff.append(printWeightCalculation()).append("\n");
        buff.append(printArmorPlacement());
        correctArmor(buff);
        buff.append(printLocations());
        correctCriticals(buff);

        // printArmor(buff);
        printFailedEquipment(buff);
        return buff;
    }
    
    @Override
    public double calculateWeight() {
        double weight = 0;
        weight += getWeightStructure();
        weight += getWeightEngine();
        weight += getWeightKFDrive();
        weight += getWeightLFBattery();
        weight += getWeightSail();
        weight += getWeightControls();
        weight += getWeightFuel();
        weight += getWeightHeatSinks();
        weight += getWeightArmor();
        weight += getWeightFireControl();

        weight += getWeightMiscEquip();
        weight += getWeightWeapon();
        weight += getWeightAmmo();

        weight += getWeightCarryingSpace();
        weight += getWeightHardpoints();
        weight += getWeightQuarters();
        weight += getWeightGravDecks();
        weight += getWeightLifeBoats();

        return weight;
    }

    @Override
    public String printWeightCalculation() {
        return printWeightStructure() + printWeightEngine()
                + printWeightKFDrive() + printWeightLFBattery()
                + printWeightSail() + printWeightControls()
                + printWeightFuel() + printWeightHeatSinks()
                + printWeightArmor() + printWeightFireControl()
                + printWeightCarryingSpace() + printWeightHardpoints()
                + printWeightQuarters() + printWeightGravDecks()
                + printWeightLifeBoats()
                + "Equipment:\n"
                + printMiscEquip() + printWeapon() + printAmmo();
    }
    
    @Override
    public String printLocations() {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < getEntity().locations(); i++) {
            String locationName = getEntity().getLocationName(i);
            buff.append(locationName).append(":").append("\n");
            for (int j = 0; j < getEntity().getNumberOfCriticals(i); j++) {
                CriticalSlot slot = getEntity().getCritical(i, j);
                if (slot == null) {
                    j = getEntity().getNumberOfCriticals(i);                    
                } else if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                        buff.append(j).append(". UNKNOWN SYSTEM NAME");
                        buff.append("\n");
                } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    EquipmentType e = getEntity().getEquipmentType(slot);
                    buff.append(j).append(". ").append(e.getInternalName());
                    buff.append("\n");
                }
            }
        }

        double[] extra = extraSlotCost(vessel);
        for (int i = 0; i < extra.length; i++) {
            if (extra[i] > 0) {
                if (i < getEntity().locations()) {
                    buff.append(getLocationAbbr(i));
                } else {
                    buff.append(getLocationAbbr(i - 3)).append(" (R)");
                }
                buff.append(" requires ").append(extra[i]).append(" tons of additional fire control.\n");
            }
        }

        return buff.toString();
    }
    
    @Override
    public String getName() {
        if (vessel.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            return "Warship: " + vessel.getDisplayName();
        } else if (vessel.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            return "Space Station: " + vessel.getDisplayName();
        } else {
            return "Jumpship: " + vessel.getDisplayName();
        }
    }
    
}
