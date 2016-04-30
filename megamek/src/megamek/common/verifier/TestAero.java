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
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.util.StringUtil;
import megamek.common.weapons.CLChemicalLaserWeapon;
import megamek.common.weapons.VehicleFlamerWeapon;

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
        CLAN_STANDARD(EquipmentType.T_ARMOR_STANDARD,0,true),
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
        PRIMITIVE(EquipmentType.T_ARMOR_PRIMITIVE,0,false),        
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
                if (a.type == t && a.isClan == c){
                    return a;
                }
            }
            return null;
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
        if (eType == Entity.ETYPE_CONV_FIGHTER){
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
        
        // XXL engines take up extra space in the aft
        if (a.getEngine() != null 
                && a.getEngine().getEngineType() == Engine.XXL_ENGINE){
            if (a.getEngine().hasFlag(Engine.CLAN_ENGINE)){
                availSpace[Aero.LOC_AFT] -= 2;
            } else {
                availSpace[Aero.LOC_AFT] -= 4;
            }
        }
        return availSpace;
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
        if (eType == Entity.ETYPE_CONV_FIGHTER){
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
     * whereas conventional fighters consume 0.5 fuel points per thrust point 
     * spent up to the maximum safe thrust.  See Strategic Operations pg 34. 
     * 
     * @param aero
     * @return
     */
    public static float calculateMaxTurnsAtSafe(Aero aero){
        int fuelPoints = aero.getFuel();
        float fuelPerTurn;
        if (aero.getEntityType() == Entity.ETYPE_CONV_FIGHTER){
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
        if (aero.getEntityType() == Entity.ETYPE_CONV_FIGHTER){
            fuelPerTurn = aero.getWalkMP() * 0.5f;
            if (aero.getEngine().isFusion()){
                fuelPerTurn += (aero.getRunMP()-aero.getWalkMP()) * 2;
            } else {
                fuelPerTurn += (aero.getRunMP()-aero.getWalkMP());
            }            
        } else {
            fuelPerTurn = aero.getWalkMP() + 
                    (aero.getRunMP()-aero.getWalkMP()) * 2;
        }
        return fuelPoints/fuelPerTurn;       
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
    public double getWeightMisc() {
        // VSTOL equipment weighs extra forr conventional fighters
        if (aero.getEntityType() == Entity.ETYPE_CONV_FIGHTER &&
                aero.isVSTOL()){
            // Weight = tonnage * 0.05 rounded to nearest half ton
            return Math.round(0.05f * aero.getWeight()*2) / 2.0;
        }
        return 0.0f;
    }

    @Override
    public double getWeightPowerAmp() {
        // Conventional Fighters with ICE engines may need a power amp
        if (aero.getEntityType() == Entity.ETYPE_CONV_FIGHTER &&
                aero.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE){
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
            return Math.round(0.1 * weight*2) / 2.0;
        }
        return 0;
    }

    @Override
    public double getWeightControls() {
        // Controls for Aerospace Fighters and Conventional Fighters consists
        //  of the cockpit and the fuel
        double weight;
        if (aero.getEntityType() == Entity.ETYPE_CONV_FIGHTER){
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

    @Override
    public int getCountHeatSinks() {
        return aero.getHeatSinks();
    }

    @Override
    public double getWeightHeatSinks() {
        return aero.getHeatSinks() - engine.getWeightFreeEngineHeatSinks();        
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return aero.getHeatType() == Aero.HEAT_DOUBLE;
    }

    @Override
    public String printWeightMisc() {
        double weight = getWeightMisc();
        if (weight > 0){
            return "VSTOL equipment: " + weight;
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
                    ", is greater than the maximum: " + maxArmorPoints);
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
        if (aero.getEntityType() == Entity.ETYPE_CONV_FIGHTER &&
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
                        && (m.getLinkedBy().getType()
                                .hasFlag(MiscType.F_ARTEMIS) || m.getLinkedBy()
                                .getType().hasFlag(MiscType.F_ARTEMIS_V));
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
                            && wt.getAmmoType() != AmmoType.T_AC_LBX) {
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
        }
        // Conventional Fighters must be heat neutral
        if (aero.getEntityType() == Entity.ETYPE_CONV_FIGHTER){
            int maxWeapHeat = countHeatEnergyWeapons();
            int heatDissipation = 0;
            if (aero.getHeatType() == Aero.HEAT_DOUBLE){
                buff.append("Conventional fighters may only use single " +
                        "heatsinks!");
                return false;
            } 
            heatDissipation = aero.getHeatSinks();
            
            if(maxWeapHeat > heatDissipation) {
                buff.append("Conventional fighters must be able to " +
                        "dissipate all heat from energy weapons! \n" +
                        "Max energy heat: " + maxWeapHeat + 
                        ", max dissipation: " + heatDissipation);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }        
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
        if (getCountHeatSinks() < engine.getWeightFreeEngineHeatSinks()) {
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
        
        correct &= correctControlSystems(buff);
        correct &= !hasIllegalTechLevels(buff, ammoTechLvl);
        correct &= !hasIllegalEquipmentCombinations(buff);
        correct &= correctHeatSinks(buff);
        
        return correct;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Aero: ").append(aero.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");        
        buff.append(printTechLevel());
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
    

    @Override
    public String getName() {
        if (aero.getEntityType() == Entity.ETYPE_CONV_FIGHTER){
            return "Conventional Fighter: " + aero.getDisplayName();
        } else {
            return "Aerospace Fighter: " + aero.getDisplayName();
        }
    }


}
