/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

/*
 * Author: Reinhard Vicinus
 */

package megamek.common.verifier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Bay;
import megamek.common.CrewQuartersCargoBay;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.FirstClassQuartersCargoBay;
import megamek.common.ITechManager;
import megamek.common.ITechnology;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.SecondClassQuartersCargoBay;
import megamek.common.SmallCraft;
import megamek.common.SteerageQuartersCargoBay;
import megamek.common.WeaponType;
import megamek.common.annotations.Nullable;
import megamek.common.util.StringUtil;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.flamers.VehicleFlamerWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.lasers.CLChemicalLaserWeapon;
import megamek.common.weapons.lrms.LRMWeapon;
import megamek.common.weapons.lrms.LRTWeapon;
import megamek.common.weapons.missiles.MRMWeapon;
import megamek.common.weapons.missiles.RLWeapon;
import megamek.common.weapons.srms.SRMWeapon;
import megamek.common.weapons.srms.SRTWeapon;

/**
 * Class for testing and validating instantiations for Conventional Fighters and
 * Aerospace Fighters.
 * 
 * @author arlith
 *
 */
public class TestAero extends TestEntity {
    private Aero aero = null;
    
    /**
     * An enumeration that keeps track of the legal armors for Aerospace and 
     * Conventional fighters.  Each entry consists of the type, which 
     * corresponds to the types defined in <code>EquipmentType</code> as well
     * as the number of slots the armor takes up.
     * 
     * @author arlith
     *
     */
    public static enum AeroArmor{
        STANDARD(EquipmentType.T_ARMOR_STANDARD,0,false),   
        CLAN_FERRO_ALUM(EquipmentType.T_ARMOR_ALUM,1,true),
        FERRO_LAMELLOR(EquipmentType.T_ARMOR_FERRO_LAMELLOR,2,true),
        CLAN_REACTIVE(EquipmentType.T_ARMOR_REACTIVE,1,true),
        CLAN_REFLECTIVE(EquipmentType.T_ARMOR_REFLECTIVE,1,true),
        ANTI_PENETRATIVE_ABLATION(
                EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION,1,false),
        BALLISTIC_REINFORCED(
                EquipmentType.T_ARMOR_BALLISTIC_REINFORCED,2,false),
        FERRO_ALUM(EquipmentType.T_ARMOR_ALUM,2,false),
        FERRO_PROTO(EquipmentType.T_ARMOR_FERRO_ALUM_PROTO,3,false),        
        HEAVY_FERRO_ALUM(EquipmentType.T_ARMOR_HEAVY_ALUM,4,false),
        LIGHT_FERRO_ALUM(EquipmentType.T_ARMOR_LIGHT_ALUM,1,false),
        PRIMITIVE(EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER,0,false),        
        REACTIVE(EquipmentType.T_ARMOR_REACTIVE,3,false),        
        REFLECTIVE(EquipmentType.T_ARMOR_REFLECTIVE,2,false),
        STEALTH_VEHICLE(EquipmentType.T_ARMOR_STEALTH_VEHICLE,2,false);

        /**
         * The type, corresponding to types defined in 
         * <code>EquipmentType</code>.
         */
        public int type;
        
        /**
         * The number of spaces occupied by the armor type.  Armors that take 
         * up 1 space take up space in the aft, those with 2 take up space in
         * each wing, 3 takes up space in both wings and the aft, 4 takes up
         * space in each possible arc (nose, aft, left wing, right wing).
         */
        public int space;
        
        /**
         * Denotes whether this armor is Clan or not.
         */
        public boolean isClan;
        
        AeroArmor(int t, int s, boolean c){
            type = t;
            space = s;
            isClan = c;
        }
        
        /**
         * Given an armor type, return the <code>AeroArmor</code> instance that
         * represents that type.
         * 
         * @param t  The armor type.
         * @param c  Whether this armor type is Clan or not.
         * @return   The <code>AeroArmor</code> that correspondes to the given 
         *              type or null if no match was found.
         */
        public static AeroArmor getArmor(int t, boolean c){
            for (AeroArmor a : values()){
                if ((a.type == t) && ((a.isClan == c)
                        || (t == EquipmentType.T_ARMOR_STANDARD))) {
                    return a;
                }
            }
            return null;
        }
        
        /**
         * @return The <code>MiscType</code> for this armor.
         */
        public EquipmentType getArmor() {
            String name = EquipmentType.getArmorTypeName(type, isClan);
            return EquipmentType.get(name);
        }
    }
    
    /**
     * Filters all fighter armor according to given tech constraints
     * 
     * @param techManager
     * @return A list of all armors that meet the tech constraints
     */
    public static List<EquipmentType> legalArmorsFor(ITechManager techManager) {
        List<EquipmentType> retVal = new ArrayList<>();
        for (AeroArmor armor : AeroArmor.values()) {
            final EquipmentType eq = armor.getArmor();
            if ((null != eq) && techManager.isLegal(eq)) {
                retVal.add(eq);
            }
        }
        return retVal;
    }
    
    /**
     * @param aero A large craft
     * @return     The maximum number of bay doors. Aerospace units that are not large craft have
     *             a maximum of zero.
     */
    public static int maxBayDoors(Aero aero) {
        if (aero.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            return 8 + (int)Math.ceil(aero.getWeight() / 100000);
        } else if (aero.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            return 8 + (int)Math.ceil(aero.getWeight() / 75000);
        } else if (aero.hasETypeFlag(Entity.ETYPE_JUMPSHIP)
                || (aero.hasETypeFlag(Entity.ETYPE_DROPSHIP))) {
            return 7 + (int)Math.ceil(aero.getWeight() / 50000);
        } else if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            return aero.isSpheroid()? 4 : 2;
        } else {
            return 0;
        }
    }
    
    public enum Quarters {
        FIRST_CLASS (10, FirstClassQuartersCargoBay.class, size -> new FirstClassQuartersCargoBay(size, 0)),
        STANDARD (7, CrewQuartersCargoBay.class, size -> new CrewQuartersCargoBay(size, 0)),
        SECOND_CLASS (7, SecondClassQuartersCargoBay.class, size -> new SecondClassQuartersCargoBay(size, 0)),
        STEERAGE (5, SteerageQuartersCargoBay.class, size -> new SteerageQuartersCargoBay(size, 0));
        
        private int tonnage;
        private Class<? extends Bay> bayClass;
        private Function<Integer, Bay> init;
        
        Quarters(int tonnage, Class<? extends Bay> bayClass, Function<Integer, Bay> init) {
            this.tonnage = tonnage;
            this.bayClass = bayClass;
            this.init = init;
        }
        
        public int getTonnage() {
            return tonnage;
        }
        
        public static @Nullable Quarters getQuartersForBay(Bay bay) {
            for (Quarters q : values()) {
                if (bay.getClass() == q.bayClass) {
                    return q;
                }
            }
            return null;
        }
        
        public Bay newQuarters(int size) {
            return init.apply(size * tonnage);
        }
    }
    
    /**
     * Defines the maximum engine rating that an Aero can have.
     */
    public static int MAX_ENGINE_RATING = 400;
    
    /**
     * Defines how many spaces each arc has for weapons.
     */
    public static int SLOTS_PER_ARC = 5;
    
    /**
     *  Computes the maximum number of armor points for a given Aero
     *  at the given tonnage.
     *   
     * @param entity_type
     * @param tonnage
     * @return
     */
    public static int maxArmorPoints(Entity aero, double tonnage){
        long eType = aero.getEntityType();
        if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            return TestSmallCraft.maxArmorPoints((SmallCraft)aero);
        } else if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
                return (int)(tonnage * 1);
        } else if (eType == Entity.ETYPE_AERO){
            return (int)(tonnage * 8);
        } else {
            return 0;
        }
    }
    
    /**
     * Computes the available space for each location in the supplied Aero.
     * Aeros can only have so many weapons in each location, and this available
     * space is reduced by the armor type.
     * 
     * @param a  The aero in question
     * @return   Returns an int array, where each element corresponds to a 
     *           location and the value is the number of weapons the Aero can
     *           have in that location
     */
    public static int[] availableSpace(Aero a){
        // Keep track of the max space we have in each arc
        int availSpace[] = 
            {SLOTS_PER_ARC,SLOTS_PER_ARC,SLOTS_PER_ARC,SLOTS_PER_ARC};
        
        // Get the armor type, to determine how much space it uses
        AeroArmor armor = 
                AeroArmor.getArmor(a.getArmorType(Aero.LOC_NOSE),
                                   a.isClanArmor(Aero.LOC_NOSE));
        
        if (armor == null){            
            return null;
        }
        // Remove space for each location until we've allocated the armor
        int spaceUsedByArmor = armor.space;
        int loc = (spaceUsedByArmor != 2) ? Aero.LOC_AFT : Aero.LOC_RWING;
        while (spaceUsedByArmor > 0){
            availSpace[loc]--;
            spaceUsedByArmor--;
            loc--;
            if (loc < 0){
                loc = Aero.LOC_AFT;
            }
        }
        
        // XXL engines take up extra space in the aft in conventional fighters
        if (((a.getEntityType() & Entity.ETYPE_CONV_FIGHTER) != 0)
                && a.hasEngine() && (a.getEngine().getEngineType() == Engine.XXL_ENGINE)) {
            if (a.getEngine().hasFlag(Engine.CLAN_ENGINE)) {
                availSpace[Aero.LOC_AFT] -= 2;
            } else {
                availSpace[Aero.LOC_AFT] -= 4;
            }
        }
        return availSpace;
    }
    
    public static boolean usesWeaponSlot(Entity en, EquipmentType eq) {
        if (eq instanceof WeaponType) {
            return !(eq instanceof BayWeapon);
        }
        if (eq instanceof MiscType) {
            // Equipment that takes up a slot on fighters and small craft, but not large craft.
            if (!en.hasETypeFlag(Entity.ETYPE_DROPSHIP) && !en.hasETypeFlag(Entity.ETYPE_JUMPSHIP)
                    && (eq.hasFlag(MiscType.F_BAP)
                            || eq.hasFlag(MiscType.F_WATCHDOG)
                            || eq.hasFlag(MiscType.F_ECM)
                            || eq.hasFlag(MiscType.F_ANGEL_ECM)
                            || eq.hasFlag(MiscType.F_EW_EQUIPMENT)
                            || eq.hasFlag(MiscType.F_BOOBY_TRAP)
                            || eq.hasFlag(MiscType.F_SENSOR_DISPENSER))) {
                return true;
                
            }
            // Equipment that takes a slot on all aerospace units
            return  eq.hasFlag(MiscType.F_CHAFF_POD)
                    || eq.hasFlag(MiscType.F_SPACE_MINE_DISPENSER)
                    || eq.hasFlag(MiscType.F_MOBILE_HPG)
                    || eq.hasFlag(MiscType.F_RECON_CAMERA)
                    || eq.hasFlag(MiscType.F_HIRES_IMAGER)
                    || eq.hasFlag(MiscType.F_HYPERSPECTRAL_IMAGER)
                    || eq.hasFlag(MiscType.F_INFRARED_IMAGER)
                    || eq.hasFlag(MiscType.F_LOOKDOWN_RADAR);
        }
        return false;
    }
    
    /**
     * Computes the engine rating for the given entity type.
     * 
     * @param entity_type
     * @param tonnage
     * @param desiredSafeThrust
     * @return
     */
    public static int calculateEngineRating(Aero unit, int tonnage, 
            int desiredSafeThrust){
        int rating;
        long eType = unit.getEntityType();
        if (unit.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            rating = (tonnage * desiredSafeThrust);
        } else if (eType == Entity.ETYPE_AERO){
            rating = (tonnage * (desiredSafeThrust - 2));
        } else {
            rating = 0;
        }
        
        if (unit.isPrimitive()){
            double dRating = rating;
            dRating *= 1.2;
            if ((dRating % 5) != 0) {
                dRating = (dRating - (dRating % 5)) + 5;
            }
            rating = (int) dRating;
        }
        return rating;
    }
    
    /**
     * Computes and returns the maximum number of turns the given unit could
     * fly at safe thrust given its fuel payload.  Aerospace fighters consume
     * 1 fuel point per thrust point spent up the the maximum safe thrust, 
     * whereas conventional fighters with turbine engines consume 0.5 fuel
     * points per thrust point spent up to the maximum safe thrust.
     * See Strategic Operations pg 34. 
     * 
     * @param aero
     * @return
     */
    public static float calculateMaxTurnsAtSafe(Aero aero){
        int fuelPoints = aero.getFuel();
        float fuelPerTurn;
        if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)
                && aero.hasEngine()
                && (aero.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            fuelPerTurn = aero.getWalkMP() * 0.5f;
        } else {
            fuelPerTurn = aero.getWalkMP();
        }
        return fuelPoints/fuelPerTurn;        
    }
    
    /**
     * Computes and returns the maximum number of turns the given unit could
     * fly at max thrust given its fuel payload.  Aerospace fighters consume
     * 1 fuel point per thrust point spent up the the maximum safe thrust and
     * 2 fuel points per thrust point afterwards, whereas conventional fighters 
     * with ICE engines consume 0.5 fuel points per thrust point spent up to 
     * the maximum safe thrust and 1 fuel point per thrust up to the maximum 
     * thrust.  Conventional fighters with Fusion engines spend 0.5 fuel points
     * per thrust up to the safe thrust and then 2 fuel points per thrust 
     * afterwards.  See Strategic Operations pg 34.
     * 
     * @param aero
     * @return
     */
    public static float calculateMaxTurnsAtMax(Aero aero){
        int fuelPoints = aero.getFuel();
        float fuelPerTurn;
        if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            fuelPerTurn = aero.getWalkMP() * 0.5f;
            if(aero.hasEngine()) {
                if(aero.getEngine().isFusion()) {
                    fuelPerTurn += (aero.getRunMP()-aero.getWalkMP()) * 2;
                } else {
                    fuelPerTurn += (aero.getRunMP()-aero.getWalkMP());
                }
            }
        } else {
            fuelPerTurn = aero.getWalkMP() + 
                    (aero.getRunMP()-aero.getWalkMP()) * 2;
        }
        return fuelPoints/fuelPerTurn;       
    }    

    public static int weightFreeHeatSinks(Aero aero) {
        if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            return TestSmallCraft.weightFreeHeatSinks((SmallCraft)aero);
        } else if (aero.hasEngine()) {
            return aero.getEngine().getWeightFreeEngineHeatSinks();
        } else {
            return 0;
        }
    }
    
    /**
     * Computes and returns the number of days the unit can spend accelerating at 1G 
     * 
     * @param aero
     * @return
     */
    public static double calculateDaysAt1G(Aero aero) {
        double stratUse = aero.getStrategicFuelUse();
        if (stratUse > 0) {
            return aero.getFuelTonnage() / aero.getStrategicFuelUse();
        } else {
            return 0.0;
        }
    }
    
    /**
     * Computes and returns the number of days the unit can spend accelerating at maximum thrust. 
     * 
     * @param aero
     * @return
     */
    public static double calculateDaysAtMax(Aero aero) {
        double stratUse = aero.getStrategicFuelUse();
        if (stratUse > 0) {
            return aero.getFuelTonnage() / (aero.getStrategicFuelUse() * aero.getRunMP() / 2.0);
        } else {
            return 0.0;
        }
    }
    
    public TestAero(Aero a, TestEntityOption option, String fs) {
        super(option, a.getEngine(), getArmor(a), getStructure(a));
        aero = a;
        fileString = fs;
    }

    private static Structure getStructure(Aero aero) {
        int type = aero.getStructureType();
        return new Structure(type, false, aero.getMovementMode());
    }

    private static Armor[] getArmor(Aero aero) {
        Armor[] armor;
        armor = new Armor[1];
        int type = aero.getArmorType(1);
        int flag = 0;
        if (aero.isClanArmor(1)) {
            flag |= Armor.CLAN_ARMOR;
        }
        armor[0] = new Armor(type, flag);
        return armor;

    }

    @Override
    public Entity getEntity() {
        return aero;
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
    public boolean isJumpship() {
        return false;
    }

    @Override
    public double getWeightMisc() {
        // VSTOL equipment weighs extra forr conventional fighters
        if ((aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) &&
                aero.isVSTOL()){
            // Weight = tonnage * 0.05 rounded up to nearest half ton
            return Math.ceil(0.05 * aero.getWeight()*2) / 2.0;
        }
        return 0.0f;
    }

    @Override
    public double getWeightPowerAmp() {
        // Conventional Fighters with ICE engines may need a power amp
        if ((aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) && aero.hasEngine()
                && (aero.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            double weight = 0;
            for (Mounted m : aero.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_ENERGY) && 
                        !(wt instanceof CLChemicalLaserWeapon) && 
                        !(wt instanceof VehicleFlamerWeapon)) {
                    weight += wt.getTonnage(aero);
                }
                Mounted linkedBy = m.getLinkedBy();
                if ((linkedBy != null) && 
                        (linkedBy.getType() instanceof MiscType) && 
                        linkedBy.getType().hasFlag(MiscType.F_PPC_CAPACITOR)){
                    weight += ((MiscType)linkedBy.getType()).getTonnage(aero);
                }
            }
            // Power amp weighs: 
            //   energy weapon tonnage * 0.1 rounded to nearest half ton
            return Math.ceil(0.1 * weight*2) / 2.0;
        }
        return 0;
    }

    @Override
    public double getWeightControls() {
        // Controls for Aerospace Fighters and Conventional Fighters consists
        //  of the cockpit and the fuel
        double weight;
        if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            // Weight = tonnage * 0.1 rounded to nearest half ton
            weight = Math.round(0.1 * aero.getWeight()*2) / 2.0;
        } else {
            weight = 3.0;
            if (aero.getCockpitType() == Aero.COCKPIT_SMALL) {
                weight = 2.0;
        } else if (aero.getCockpitType() == Aero.COCKPIT_COMMAND_CONSOLE){                       
                weight = 6.0;
            } else if (aero.getCockpitType() == Aero.COCKPIT_PRIMITIVE) {
                weight = 5.0;
            } 
        }   
        return weight;
    }
    
    public double getWeightFuel() {
        return aero.getFuelTonnage();
    }

    /**
     * @return The number of heat sinks required by conventional fighters
     */
    private int getConventionalCountHeatLaserWeapons() {
        int heat = 0;
        for (Mounted m : aero.getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if ((wt.hasFlag(WeaponType.F_LASER) && (wt.getAmmoType() == AmmoType.T_NA))
                    || wt.hasFlag(WeaponType.F_PPC)
                    || wt.hasFlag(WeaponType.F_PLASMA)
                    || wt.hasFlag(WeaponType.F_PLASMA_MFUK)
                    || (wt.hasFlag(WeaponType.F_FLAMER) && (wt.getAmmoType() == AmmoType.T_NA))) {
                heat += wt.getHeat();
            }
            // laser insulator reduce heat by 1, to a minimum of 1
            if (wt.hasFlag(WeaponType.F_LASER) && (m.getLinkedBy() != null)
                    && !m.getLinkedBy().isInoperable()
                    && m.getLinkedBy().getType().hasFlag(MiscType.F_LASER_INSULATOR)) {
                heat -= 1;
                if (heat == 0) {
                    heat++;
                }
            }

            if ((m.getLinkedBy() != null) && (m.getLinkedBy().getType() instanceof
                    MiscType) && m.getLinkedBy().getType().
                    hasFlag(MiscType.F_PPC_CAPACITOR)) {
                heat += 5;
            }
        }
        for (Mounted m : aero.getMisc()) {
            MiscType mtype = (MiscType)m.getType();
            // mobile HPGs count as energy weapons for construction purposes
            if (mtype.hasFlag(MiscType.F_MOBILE_HPG)) {
                heat += 20;
            }
            if (mtype.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                heat += 2;
            }
            if (mtype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)||mtype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)) {
                heat += 12;
            }
        }
        if (aero.getArmorType(1) == EquipmentType.T_ARMOR_STEALTH_VEHICLE) {
            heat += 10;
        }
        return heat;
    }

    @Override
    public int getCountHeatSinks() {
        if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            return getConventionalCountHeatLaserWeapons();
        }
        return aero.getHeatSinks();
    }

    @Override
    public double getWeightHeatSinks() {
        if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            int required = countHeatEnergyWeapons();
            return Math.max(0, required - engine.getWeightFreeEngineHeatSinks());
        } else {
            return Math.max(getCountHeatSinks() - engine.getWeightFreeEngineHeatSinks(), 0);
        }
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return aero.getHeatType() == Aero.HEAT_DOUBLE;
    }

    @Override
    public String printWeightMisc() {
        double weight = getWeightMisc();
        if (weight > 0){
            StringBuffer retVal = new StringBuffer(StringUtil.makeLength(
                    "VSTOL equipment:", getPrintSize() - 5));
            retVal.append(makeWeightString(weight));
            retVal.append("\n");
            return retVal.toString();
        }
        return "";
    }

    @Override
    public String printWeightControls() {
        StringBuffer retVal = new StringBuffer(StringUtil.makeLength(
                aero.getCockpitTypeString() + ":", getPrintSize() - 5));
        retVal.append(makeWeightString(getWeightControls()));
        retVal.append("\n");
        return retVal.toString();
    }
        
    public String printWeightFuel() {
        StringBuffer retVal = new StringBuffer(StringUtil.makeLength(
                "Fuel: ", getPrintSize() - 5));
        retVal.append(makeWeightString(getWeightFuel()));
        retVal.append("\n");
        return retVal.toString();
    }

    public Aero getAero() {
        return aero;
    }

    private int countHeatEnergyWeapons() {
        int heat = 0;
        for (Mounted m : aero.getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if ((wt.hasFlag(WeaponType.F_LASER) 
                    && (wt.getAmmoType() == AmmoType.T_NA))
                        || wt.hasFlag(WeaponType.F_PPC)
                        || wt.hasFlag(WeaponType.F_PLASMA)
                        || wt.hasFlag(WeaponType.F_PLASMA_MFUK)
                        || (wt.hasFlag(WeaponType.F_FLAMER) 
                                && (wt.getAmmoType() == AmmoType.T_NA))) {
                heat += wt.getHeat();
            }
            // laser insulator reduce heat by 1, to a minimum of 1
            Mounted linkedBy = m.getLinkedBy();
            if (wt.hasFlag(WeaponType.F_LASER) && (linkedBy != null)
                    && !linkedBy.isInoperable()
                    && linkedBy.getType().hasFlag(MiscType.F_LASER_INSULATOR)) {
                heat -= 1;
                if (heat == 0) {
                    heat++;
                }
            }

            if ((linkedBy != null) && 
                    (linkedBy.getType() instanceof MiscType) && 
                    linkedBy.getType().hasFlag(MiscType.F_PPC_CAPACITOR)) {
                heat += 5;
            }
        }
        for (Mounted m : aero.getMisc()) {
            MiscType mtype = (MiscType)m.getType();
            // mobile HPGs count as energy weapons for construction purposes
            if (mtype.hasFlag(MiscType.F_MOBILE_HPG)) {
                heat += 20;
            }
        }
        return heat;
    }

    public String printArmorLocProp(int loc, int wert) {
        return " is greater than " + Integer.toString(wert) + "!";
    }

    /**
     * Checks to see if this unit has valid armor assignment.
     * 
     * @param buff
     * @return
     */
    public boolean correctArmor(StringBuffer buff) {
        boolean correct = true;
        int maxArmorPoints = maxArmorPoints(aero,aero.getWeight());
        int armorTotal = 0;
        for (int loc = 0; loc < aero.locations(); loc++) {
            if (aero.getOArmor(loc) > maxArmorPoints) {
                buff.append(printArmorLocation(loc))
                        .append(printArmorLocProp(loc,
                                maxArmorPoints)).append("\n");
                correct = false;
            }
            armorTotal += aero.getOArmor(loc);
        }
        if (armorTotal > maxArmorPoints){
            buff.append("Total armor," + armorTotal + 
                    ", is greater than the maximum: " + maxArmorPoints + "\n");
            correct = false;
        }

        return correct ;
    }
    
    /**
     * Checks that Conventional fighters only have a standard cockpit and that
     * Aerospace fighters have a valid cockpit (standard, small, primitive, 
     * command console).
     * 
     * @param buff
     * @return
     */
    public boolean correctControlSystems(StringBuffer buff){
        if ((aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) &&
                aero.getCockpitType() != Aero.COCKPIT_STANDARD){
            buff.append(
                    "Conventional fighters may only have standard cockpits!");
            return false;
        } else if (aero.getCockpitType() < Aero.COCKPIT_STANDARD || 
                aero.getCockpitType() > Aero.COCKPIT_PRIMITIVE){
            buff.append(
                    "Invalid cockpit type!");
            return false;
        }
        return true;
    }
    
    public void checkCriticalSlotsForEquipment(Entity entity,
            Vector<Mounted> unallocated, Vector<Serializable> allocation,
            Vector<Integer> heatSinks) {
        for (Mounted m : entity.getEquipment()) {
            if (m.getLocation() == Entity.LOC_NONE) {
                if ((m.getType() instanceof AmmoType)
                        && (m.getUsableShotsLeft() <= 1)) {
                    continue;
                }
                if ((entity instanceof Aero) && 
                        (m.getType().getCriticals(entity) == 0)) {
                    continue;
                }
                if (!(m.getType() instanceof MiscType)) {
                    unallocated.addElement(m);
                    continue;
                }
                MiscType mt = (MiscType) m.getType();
                if (!mt.hasFlag(MiscType.F_HEAT_SINK)
                        && !mt.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                        && !mt.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                    unallocated.addElement(m);
                    continue;
                }
            }
        }
    }

    /**
     * For Aerospace and Conventional fighters the only thing we need to ensure
     * is that they do not mount more weapons in each arc then allowed.  They
     * have boundless space for equipment.  Certain armor types reduce the 
     * number of spaces available in each arc.
     * 
     * @param buff  A buffer for error messages
     * @return  True if the mounted weapons are valid, else false
     */
    public boolean correctCriticals(StringBuffer buff) {
        Vector<Mounted> unallocated = new Vector<Mounted>();
        Vector<Serializable> allocation = new Vector<Serializable>();
        Vector<Integer> heatSinks = new Vector<Integer>();
        checkCriticalSlotsForEquipment(aero, unallocated, allocation, heatSinks);
        boolean correct = true;
        
        if (!unallocated.isEmpty()) {
            buff.append("Unallocated Equipment:\n");
            for (Mounted mount : unallocated) {
                buff.append(mount.getType().getInternalName()).append("\n");
            }
            correct = false;
        }
        if (!allocation.isEmpty()) {
            buff.append("Allocated Equipment:\n");
            for (Enumeration<Serializable> serializableEnum = allocation
                    .elements(); serializableEnum.hasMoreElements();) {
                Mounted mount = (Mounted) serializableEnum.nextElement();
                int needCrits = ((Integer) serializableEnum.nextElement())
                        .intValue();
                int aktCrits = ((Integer) serializableEnum.nextElement())
                        .intValue();
                buff.append(mount.getType().getInternalName()).append(" has ")
                        .append(needCrits).append(" Slots, but ")
                        .append(aktCrits).append(" Slots are allocated!")
                        .append("\n");
            }
            correct = false;
        }
        int numWeapons[] = new int[aero.locations()];
        int numBombs = 0;
        
        for (Mounted m : aero.getWeaponList()){
            if (m.getLocation() == Entity.LOC_NONE)
                continue;
            
            // Aeros can't use special munitions except for artemis, exceptions
            //  LBX's must use clusters
            WeaponType wt = (WeaponType)m.getType();
            boolean canHaveSpecialMunitions = 
                    ((wt.getAmmoType() == AmmoType.T_MML)
                    || (wt.getAmmoType() == AmmoType.T_ATM)
                    || (wt.getAmmoType() == AmmoType.T_NARC));
            if (wt.getAmmoType() != AmmoType.T_NA
                    && m.getLinked() != null 
                    && !canHaveSpecialMunitions) {
                EquipmentType linkedType = m.getLinked().getType();
                boolean hasArtemisFCS = m.getLinkedBy() != null
                        && (m.getLinkedBy().getType().hasFlag(MiscType.F_ARTEMIS)
                        || m.getLinkedBy().getType().hasFlag(MiscType.F_ARTEMIS_PROTO)
                        || m.getLinkedBy().getType().hasFlag(MiscType.F_ARTEMIS_V));
                if (linkedType instanceof AmmoType) {
                    AmmoType linkedAT = (AmmoType)linkedType;
                    // Check LBX's
                    if (wt.getAmmoType() == AmmoType.T_AC_LBX && 
                            linkedAT.getMunitionType() != AmmoType.M_CLUSTER) {
                        correct = false;
                        buff.append("Aeros must use cluster munitions!" + 
                                m.getType().getInternalName() + " is using "
                                + linkedAT.getInternalName() + "\n");
                    }
                    // Allow Artemis munitions for artemis-linked launchers
                    if (hasArtemisFCS
                            && linkedAT.getMunitionType() != AmmoType.M_STANDARD
                            && linkedAT.getMunitionType() != AmmoType.M_ARTEMIS_CAPABLE
                            && linkedAT.getMunitionType() != AmmoType.M_ARTEMIS_V_CAPABLE) {
                        correct = false;
                        buff.append("Aero using illegal special missile type!"
                                + m.getType().getInternalName() + " is using "
                                + linkedAT.getInternalName() + "\n");
                    }
                    if (linkedAT.getMunitionType() != AmmoType.M_STANDARD 
                            && !hasArtemisFCS 
                            && wt.getAmmoType() != AmmoType.T_AC_LBX
                    		&& wt.getAmmoType() != AmmoType.T_SBGAUSS){
                        correct = false;
                        buff.append("Aeros may not use special munitions! "
                                + m.getType().getInternalName() + " is using "
                                + linkedAT.getInternalName() + "\n");
                    }
                    
                }
                
                
            }

            
            if (m.getType().hasFlag(AmmoType.F_SPACE_BOMB) 
                    || m.getType().hasFlag(AmmoType.F_GROUND_BOMB)
                    || m.getType().hasFlag(WeaponType.F_DIVE_BOMB)
                    || m.getType().hasFlag(WeaponType.F_ALT_BOMB)
                    || m.getType().hasFlag(WeaponType.F_SPACE_BOMB)){
                numBombs++;
            } else {
                numWeapons[m.getLocation()]++;
            }
        }
        
        int availSpace[] = availableSpace(aero);
        if (availSpace == null){
            buff.append("Invalid armor type! Armor: " + 
                    EquipmentType.armorNames[aero.getArmorType(Aero.LOC_NOSE)]);
            buff.append("\n");
            return false;
        }       
        
        if (numBombs > aero.getMaxBombPoints()){
            buff.append("Invalid number of bombs! Unit can mount "
                    + aero.getMaxBombPoints() + " but " + numBombs
                    + "are present!");
            buff.append("\n");
            return false;
        }
        
        String[] locNames = aero.getLocationNames();
        int loc = Aero.LOC_AFT;
        while (loc >= 0){
            correct &= !(numWeapons[loc] > availSpace[loc]);
            if (numWeapons[loc] > availSpace[loc]){
                buff.append(locNames[loc] + " has " + numWeapons[loc] + 
                        " weapons but it can only fit " + availSpace[loc] + 
                        " weapons!");
                buff.append("\n");
            }
            loc--;
        }        
        
        return correct;
    }
    
    
    /**
     * Checks that the heatsink assignment is legal.  Conventional fighters must
     * have enough heatsinks to dissipate heat from all of their energy weapons
     * and they may only mount standard heatsinks.
     * Aerospace fighters must have at least 10 heatsinks.
     * 
     * @param buff
     * @return
     */
    public boolean correctHeatSinks(StringBuffer buff) {
        if ((aero.getHeatType() != Aero.HEAT_SINGLE) 
                && (aero.getHeatType() != Aero.HEAT_DOUBLE)) {
            buff.append("Invalid heatsink type!  Valid types are "
                    + Aero.HEAT_SINGLE + " and " + Aero.HEAT_DOUBLE
                    + ".  Found " + aero.getHeatType() + ".");
            return false;
        }
        return true;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, aero.getTechLevel());
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        boolean correct = true;
        
        // We only support Convetional Fighters and ASF
        if (aero.getEntityType() == Entity.ETYPE_DROPSHIP || 
                aero.getEntityType() == Entity.ETYPE_SMALL_CRAFT ||
                aero.getEntityType() == Entity.ETYPE_FIGHTER_SQUADRON ||
                aero.getEntityType() == Entity.ETYPE_JUMPSHIP ||
                aero.getEntityType() == Entity.ETYPE_SPACE_STATION){
            System.out.println("TestAero only supports Aerospace Fighters " +
                    "and Conventional fighters.  Supplied unit was a " + 
                    Entity.getEntityTypeName(aero.getEntityType()));
            return true;            
        }
        
        if (skip()) {
            return true;
        }
        if (!correctWeight(buff)) {
            buff.insert(0, printTechLevel() + printShortMovement());
            buff.append(printWeightCalculation());
            correct = false;
        }
        if (!engine.engineValid) {
            buff.append(engine.problem.toString()).append("\n\n");
            correct = false;
        }
        if ((getCountHeatSinks() < engine.getWeightFreeEngineHeatSinks())
                && !aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            buff.append("Heat Sinks:\n");
            buff.append(" Engine    "
                    + engine.integralHeatSinkCapacity(false) + "\n");
            buff.append(" Total     " + getCountHeatSinks() + "\n");
            buff.append(" Required  " + engine.getWeightFreeEngineHeatSinks()
                    + "\n");
            correct = false;
        }                
        
        if (showCorrectArmor() && !correctArmor(buff)) {
            correct = false;
        }
        if (showCorrectCritical() && !correctCriticals(buff)) {
            correct = false;
        }
        if (showFailedEquip() && hasFailedEquipment(buff)) {
            correct = false;
        }
        if (showIncorrectIntroYear() && hasIncorrectIntroYear(buff)) {
            correct = false;
        }
        
        correct &= correctControlSystems(buff);
        correct &= !hasIllegalTechLevels(buff, ammoTechLvl);
        correct &= !hasIllegalEquipmentCombinations(buff);
        correct &= correctHeatSinks(buff);
        
        return correct;
    }

    public boolean isAeroWeapon(EquipmentType eq, Entity en) {
        if (eq instanceof InfantryWeapon) {
            return false;
        }

        WeaponType weapon = (WeaponType) eq;
        
        // small craft only; lacks aero weapon flag
        if (weapon.getAmmoType() == AmmoType.T_C3_REMOTE_SENSOR) {
            return en.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)
                    && !en.hasETypeFlag(Entity.ETYPE_DROPSHIP);
        }

        if (weapon.hasFlag(WeaponType.F_ARTILLERY) && !weapon.hasFlag(WeaponType.F_BA_WEAPON)) {
            return (weapon.getAmmoType() == AmmoType.T_ARROW_IV)
                    || en.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)
                    || en.hasETypeFlag(Entity.ETYPE_JUMPSHIP);
        }
        
        if (weapon.isSubCapital() || (weapon.isCapital() && (weapon.hasFlag(WeaponType.F_MISSILE)))
                || (weapon.getAtClass() == WeaponType.CLASS_SCREEN)) {
            return en.hasETypeFlag(Entity.ETYPE_DROPSHIP)
                    || en.hasETypeFlag(Entity.ETYPE_JUMPSHIP);
        }

        if (weapon.isCapital()) {
            return en.hasETypeFlag(Entity.ETYPE_JUMPSHIP);
        }
        
        if (weapon instanceof BayWeapon) {
            return en.usesWeaponBays();
        }

        if (!weapon.hasFlag(WeaponType.F_AERO_WEAPON)) {
            return false;
        }

        if (((weapon instanceof LRMWeapon) || (weapon instanceof LRTWeapon))
                && (weapon.getRackSize() != 5)
                && (weapon.getRackSize() != 10)
                && (weapon.getRackSize() != 15)
                && (weapon.getRackSize() != 20)) {
            return false;
        }
        if (((weapon instanceof SRMWeapon) || (weapon instanceof SRTWeapon))
                && (weapon.getRackSize() != 2)
                && (weapon.getRackSize() != 4)
                && (weapon.getRackSize() != 6)) {
            return false;
        }
        if ((weapon instanceof MRMWeapon) && (weapon.getRackSize() < 10)) {
            return false;
        }

        if ((weapon instanceof RLWeapon) && (weapon.getRackSize() < 10)) {
            return false;
        }
        
        if (weapon.hasFlag(WeaponType.F_ENERGY)
                || (weapon.hasFlag(WeaponType.F_PLASMA) && (weapon
                        .getAmmoType() == AmmoType.T_PLASMA))) {

            if (weapon.hasFlag(WeaponType.F_ENERGY)
                    && weapon.hasFlag(WeaponType.F_PLASMA)
                    && (weapon.getAmmoType() == AmmoType.T_NA)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Aero: ").append(aero.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");        
        buff.append(printTechLevel());
        buff.append("Intro year: ").append(aero.getYear());
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
        weight += getWeightEngine();
        weight += getWeightControls();
        weight += getWeightFuel();
        weight += getWeightHeatSinks();
        weight += getWeightArmor();
        weight += getWeightMisc();

        weight += getWeightMiscEquip();
        weight += getWeightWeapon();
        weight += getWeightAmmo();
        weight += getWeightPowerAmp();

        weight += getWeightCarryingSpace();

        weight += getArmoredComponentWeight();
        return weight;
    }

    @Override
    public String printWeightCalculation() {
        return printWeightEngine()
                + printWeightControls() + printWeightFuel() 
                + printWeightHeatSinks()
                + printWeightArmor() + printWeightMisc()
                + printWeightCarryingSpace() + "Equipment:\n"
                + printMiscEquip() + printWeapon() + printAmmo();
    }
    
    @Override
    public String printLocations() {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < getEntity().locations(); i++) {
            String locationName = getEntity().getLocationName(i);
            buff.append(locationName + ":");
            buff.append("\n");
            for (int j = 0; j < getEntity().getNumberOfCriticals(i); j++) {
                CriticalSlot slot = getEntity().getCritical(i, j);
                if (slot == null) {
                    j = getEntity().getNumberOfCriticals(i);                    
                } else if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                        buff.append(Integer.toString(j)
                                + ". UNKNOWN SYSTEM NAME");
                        buff.append("\n");
                } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    EquipmentType e = getEntity().getEquipmentType(slot);
                    buff.append(Integer.toString(j) + ". "
                            + e.getInternalName());
                    buff.append("\n");
                }
            }
        }
        return buff.toString();
    }
    
    public double getWeightQuarters() {
        double quartersWeight = 0;
        for (Bay bay : getEntity().getTransportBays()) {
            if (bay.isQuarters()) {
                quartersWeight += bay.getWeight();
            }
        }
        return quartersWeight;
    }

    public String printWeightQuarters() {
        double weight = 0.0;
        for (Bay bay : aero.getTransportBays()) {
            if (bay.isQuarters()) {
                weight += bay.getWeight();
            }
        }
        if (weight > 0) {
            return StringUtil.makeLength("Crew quarters: ", getPrintSize() - 5) + weight + "\n";
        }
        return "";
    }

    @Override
    public String getName() {
        if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            return "Conventional Fighter: " + aero.getDisplayName();
        } else {
            return "Aerospace Fighter: " + aero.getDisplayName();
        }
    }

    /**
     * Calculate the structural integrity weight
     */
    public double getWeightStructure() {
        double tonnage = 0;
        if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            tonnage = aero.getSI() * aero.getWeight();
            if (aero.isSpheroid()) {
                tonnage /= 500;
            } else {
                tonnage /= 200;
            }
        } else if (aero.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            tonnage = aero.getWeight() / 100;
        } else if (aero.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            // SI * weight / 1000, rounded up to half ton
            tonnage = aero.getSI() * aero.getWeight() / 1000;
        } else if (aero.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            tonnage = aero.getWeight() / 150;
        } else {
            // Fighters do not allocate weight to structure
            return 0;
        }
        return Math.ceil(tonnage * 2) / 2.0;
    }

    /**
     * Get the maximum tonnage for the type of unit. Primitive jumpships will use the maximum
     * allowable value for the construction year (Terran Alliance/Hegemony)
     * 
     * @param aero      The unit
     * @return          The maximum tonnage for the type of unit.
     */
    public static int getMaxTonnage(Aero aero) {
        return getMaxTonnage(aero, ITechnology.F_NONE);
    }
    
    /**
     * Get the maximum tonnage for the type of unit
     * 
     * @param aero      The unit
     * @param faction   An ITechnology faction constant used for primitive jumpships. A value
     *                  of F_NONE will use the least restrictive values (TA/TH).
     * @return          The maximum tonnage for the type of unit.
     */
    public static int getMaxTonnage(Aero aero, int faction) {
        if (aero.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            return 2500000;
        } else if (aero.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            return 250000;
        } else if (aero.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            if (aero.isPrimitive()) {
                return getPrimitiveJumpshipMaxTonnage(aero, faction);
            }
            return 500000;
        } else if (aero.hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
            if (aero.isPrimitive()) {
                return getPrimitiveDropshipMaxTonnage(aero);
            }
            return aero.isSpheroid()? 100000 : 35000;
        } else if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)
                || aero.hasETypeFlag(Entity.ETYPE_FIXED_WING_SUPPORT)) {
            return 200;
        } else if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            return 50;
        } else {
            return 100;
        }
    }
    
    /**
     * @param jumpship
     * @return Max tonnage allowed by construction rules.
     */
    public static int getPrimitiveJumpshipMaxTonnage(Aero jumpship, int faction) {
        switch (faction) {
            case ITechnology.F_TA:
            case ITechnology.F_TH:
            case ITechnology.F_NONE:
                if (jumpship.getYear() < 2130) {
                    return 100000;
                } else if (jumpship.getYear() < 2150) {
                    return 150000;
                } else if (jumpship.getYear() < 2165) {
                    return 200000;
                } else if (jumpship.getYear() < 2175) {
                    return 250000;
                } else if (jumpship.getYear() < 2200) {
                    return 350000;
                } else if (jumpship.getYear() < 2300){
                    return 500000;
                } else if (jumpship.getYear() < 2350) {
                    return 1000000;
                } else if (jumpship.getYear() < 2400) {
                    return 1600000;
                } else {
                    return 1800000;
                }
            case ITechnology.F_CC:
            case ITechnology.F_DC:
            case ITechnology.F_FS:
            case ITechnology.F_FW:
            case ITechnology.F_LC:
                if (jumpship.getYear() < 2300){
                    return 350000;
                } else if (jumpship.getYear() < 2350) {
                    return 600000;
                } else if (jumpship.getYear() < 2400) {
                    return 800000;
                } else {
                    return 1000000;
                }
            default:
                if (jumpship.getYear() < 2300){
                    return 300000;
                } else if (jumpship.getYear() < 2350) {
                    return 450000;
                } else if (jumpship.getYear() < 2400) {
                    return 600000;
                } else {
                    return 1000000;
                }
        }
    }
    
    public static int getPrimitiveDropshipMaxTonnage(Aero dropship) {
        if (dropship.getYear() < 2130) {
            return dropship.isSpheroid()? 3000 : 1000; 
        } else if (dropship.getYear() < 2150) {
            return dropship.isSpheroid()? 4000 : 1500; 
        } else if (dropship.getYear() < 2165) {
            return dropship.isSpheroid()? 7000 : 2500; 
        } else if (dropship.getYear() < 2175) {
            return dropship.isSpheroid()? 10000 : 3000; 
        } else if (dropship.getYear() < 2200) {
            return dropship.isSpheroid()? 14000 : 5000; 
        } else if (dropship.getYear() < 2250) {
            return dropship.isSpheroid()? 15000 : 6000; 
        } else if (dropship.getYear() < 2300) {
            return dropship.isSpheroid()? 19000 : 7000; 
        } else if (dropship.getYear() < 2350) {
            return dropship.isSpheroid()? 23000 : 8000; 
        } else if (dropship.getYear() < 2425) {
            return dropship.isSpheroid()? 30000 : 10000; 
        } else {
            return dropship.isSpheroid()? 50000 : 20000; 
        }
    }
}
