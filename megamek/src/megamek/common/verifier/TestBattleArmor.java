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
 * Author: Jay Lawson (Taharqa)
 */

package megamek.common.verifier;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.WeaponType;


public class TestBattleArmor extends TestEntity {
    
    /**
     * BattleArmor can have a variable number of shots per slot of ammo, this
     * variable defines the maximum number of shots per slot they can have.
     */
    public static int NUM_SHOTS_PER_CRIT = 4;
    
    /**
     * An enumeration that keeps track of the legal armors for BattleArmor.  
     * Each entry consists of the type, which 
     * corresponds to the types defined in <code>EquipmentType</code>.
     * 
     * @author arlith
     *
     */
    public static enum BAArmor{
        STANDARD(EquipmentType.T_ARMOR_BA_STANDARD,0,false),   
        CLAN_STANDARD(EquipmentType.T_ARMOR_BA_STANDARD,0,true),
        STANDARD_PROTOTYPE(EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE,4,false),
        STANDARD_ADVANCED(EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED,5,false),
        STEALTH_BASIC(EquipmentType.T_ARMOR_BA_STEALTH_BASIC,3,false),
        CLAN_STEALTH_BASIC(EquipmentType.T_ARMOR_BA_STEALTH_BASIC,3,true),
        STEALTH(EquipmentType.T_ARMOR_BA_STEALTH,4,false),
        CLAN_STEALTH(EquipmentType.T_ARMOR_BA_STEALTH,4,true),
        STEALTH_IMPROVED(EquipmentType.T_ARMOR_BA_STEALTH_IMP,5,false),
        CLAN_STEALTH_IMPROVED(EquipmentType.T_ARMOR_BA_STEALTH_IMP,5,true),
        STEALTH_PROTOTYPE(EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE,4,false),
        FIRE_RESISTANT(EquipmentType.T_ARMOR_BA_FIRE_RESIST,5,false),
        CLAN_FIRE_RESISTANT(EquipmentType.T_ARMOR_BA_FIRE_RESIST,5,true),
        MIMETIC(EquipmentType.T_ARMOR_BA_MIMETIC,7,false),
        CLAN_MIMETIC(EquipmentType.T_ARMOR_BA_MIMETIC,7,true),
        REFLECTIVE(EquipmentType.T_ARMOR_BA_REFLECTIVE,7,false),
        CLAN_REFLECTIVE(EquipmentType.T_ARMOR_BA_REFLECTIVE,7,true),
        REACTIVE(EquipmentType.T_ARMOR_BA_REACTIVE,7,false),
        CLAN_REACTIVE(EquipmentType.T_ARMOR_BA_REACTIVE,7,true);

        /**
         * The type, corresponding to types defined in 
         * <code>EquipmentType</code>.
         */
        public int type;
        
        /**
         * The number of spaces occupied by the armor type. 
         */
        public int space;
        
        
        /**
         * Denotes whether this armor is Clan or not.
         */
        public boolean isClan;
        
        BAArmor(int t, int s, boolean c){
            type = t;
            space = s;
            isClan = c;
        }
        
        public static int getNumBAArmors(){
            return values().length;
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
        public static BAArmor getArmor(int t, boolean c){
            for (BAArmor a : values()){
                if (a.type == t && a.isClan == c){
                    return a;
                }
            }
            return null;
        }
    }
    
    /**
     * Checks to see if the supplied <code>Mounted</code> is valid to be mounted
     * in the given location on the supplied <code>BattleArmor</code>.
     * 
     * This method will check that there is available space in the given 
     * location make sure that weapon mounting restrictions hold.
     * 
     * @param ba
     * @param newMount
     * @param loc
     * @return
     */
    public static boolean isMountLegal(BattleArmor ba, Mounted newMount, int loc) {
        return isMountLegal(ba, newMount, loc, BattleArmor.LOC_SQUAD);        
    }
    
    /**
     * Checks to see if the supplied <code>Mounted</code> is valid to be mounted
     * in the given location on the supplied <code>BattleArmor</code> for the
     * specified suit in the squad.
     * 
     * This method will check that there is available space in the given 
     * location make sure that weapon mounting restrictions hold.
     * 
     * @param ba
     * @param newMount
     * @param loc
     * @param trooper
     * @return
     */
    public static boolean isMountLegal(BattleArmor ba, Mounted newMount,
            int loc, int trooper) {
        int numUsedCrits = 0;
        int numAntiMechWeapons = 0;
        int numAntiPersonnelWeapons = 0;
        for (Mounted m : ba.getEquipment()){
            if (m.getBaMountLoc() == loc 
                    && (m.getLocation() == trooper 
                        || m.getLocation() == BattleArmor.LOC_SQUAD)){
                if (m.getType().isSpreadable()){
                    numUsedCrits++;
                } else {
                    numUsedCrits += m.getType().getCriticals(ba);
                }
                if (m.getType() instanceof WeaponType){
                    if (m.getType().hasFlag(WeaponType.F_INFANTRY)){
                        numAntiPersonnelWeapons++;
                    } else {
                        numAntiMechWeapons++;
                    }
                }
            }
        }
        
        // Do we have free space to mount this equipment?
        int newCrits;
        if (newMount.getType().isSpreadable()){
            newCrits = 1;
        } else {
            newCrits = newMount.getType().getCriticals(ba);
        }
        if ((numUsedCrits + newCrits) <= ba.getNumCrits(loc)) {
            // Weapons require extra criticism
            if (newMount.getType() instanceof WeaponType){
                if (newMount.getType().hasFlag(WeaponType.F_INFANTRY)){
                    if ((numAntiPersonnelWeapons + 1) <= 
                            ba.getNumAllowedAntiPersonnelWeapons(loc,trooper)){
                        return true;
                    } else {
                        return false;
                    }                     
                } else {
                    if ((numAntiMechWeapons + 1) <= 
                            ba.getNumAllowedAntiMechWeapons(loc)){
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
    
    private BattleArmor ba;
    
    public TestBattleArmor(BattleArmor armor, TestEntityOption option,
            String fileString) {
        super(option, null, null, null);
        this.ba = armor;
        this.fileString = fileString;
    }
    
    @Override
    public Entity getEntity() {
        return ba;
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
        return false;
    }

    @Override
    public float getWeightControls() {
        return 0;
    }

    @Override
    public float getWeightMisc() {
        return 0;
    }

    @Override
    public float getWeightHeatSinks() {
        return 0;
    }

    @Override
    public float getWeightEngine() {
        return 0;
    }
    
    @Override
    public float getWeightStructure() {
        float tons = 0;
        switch(ba.getWeightClass()) {
        case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
            if(ba.isClan()) {
                tons += 0.13;
            } else {
                tons += 0.08;
            }
            tons += ba.getOriginalWalkMP() * .025;
            if(ba.getMovementMode() == EntityMovementMode.INF_UMU) {
                tons += ba.getOriginalJumpMP() * .045;
            }
            else if(ba.getMovementMode() == EntityMovementMode.VTOL) {
                tons += ba.getOriginalJumpMP() * .03;
            } else {
                tons += ba.getOriginalJumpMP() * .025;
            }
            break;
        case EntityWeightClass.WEIGHT_LIGHT:
            if(ba.isClan()) {
                tons += 0.15;
            } else {
                    tons += 0.1;
            }
            tons += ba.getOriginalWalkMP()  * .03;
            if(ba.getMovementMode() == EntityMovementMode.INF_UMU) {
                tons += ba.getOriginalJumpMP() * .045;
            }
            else if(ba.getMovementMode() == EntityMovementMode.VTOL) {
                tons += ba.getOriginalJumpMP() * .04;
            } else {
                tons += ba.getOriginalJumpMP() * .025;
            }
            break;
        case EntityWeightClass.WEIGHT_MEDIUM:
            if(ba.isClan()) {
                tons += 0.25;
            } else {
                    tons += 0.175;
            }
            tons += ba.getOriginalWalkMP()  * .04;
            if(ba.getMovementMode() == EntityMovementMode.INF_UMU) {
                tons += ba.getOriginalJumpMP() * .085;
            }
            else if(ba.getMovementMode() == EntityMovementMode.VTOL) {
                tons += ba.getOriginalJumpMP() * .06;
            } else {
                tons += ba.getOriginalJumpMP() * .05;
            }
            break;
        case EntityWeightClass.WEIGHT_HEAVY:
            if(ba.isClan()) {
                tons += 0.4;
            } else {
                    tons += 0.3;
            }
            tons += ba.getOriginalWalkMP()  * .08;
            if(ba.getMovementMode() == EntityMovementMode.INF_UMU) {
                tons += ba.getOriginalJumpMP() * .16;
            }
            else {
                tons += ba.getOriginalJumpMP() * .125;
            }
            break;
        case EntityWeightClass.WEIGHT_ASSAULT:
            if(ba.isClan()) {
                tons += 0.7;
            } else {
                    tons += 0.55;
            }
            tons += ba.getOriginalWalkMP()  * .16;       
            tons += ba.getOriginalJumpMP() * .25;
            break;
        }
        return tons;
    }
    
    @Override
    public float getWeightArmor() {
        return ba.getOArmor(1)
                * EquipmentType.getBaArmorWeightPerPoint(ba.getArmorType(1),
                        ba.isClan());
    }
    
    @Override
    public boolean hasDoubleHeatSinks() {
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        return 0;
    }

    @Override
    public String printWeightMisc() {
        return null;
    }

    @Override
    public String printWeightControls() {
        return null;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return false;
    }

    @Override
    public boolean correctEntity(StringBuffer buff, boolean ignoreAmmo) {
        return false;
    }

    @Override
    public StringBuffer printEntity() {
        return null;
    }

    @Override
    public String getName() {
        return "Battle Armor: " + ba.getDisplayName();    
    }

    @Override
    public float getWeightPowerAmp() {
        return 0;
    }
    
    /**
     * Performs the same functionality as <code>TestEntity.getWeightMiscEquip
     * </code> but only considers equipment mounted on the specified trooper.
     * That is, only misc equipment that is squad mounted or on the specific 
     * trooper is added.
     * 
     * @param trooper
     * @return
     */
    public float getWeightMiscEquip(int trooper) {
        float weightSum = 0.0f;
        for (Mounted m : getEntity().getMisc()) {
            MiscType mt = (MiscType) m.getType();
            // If this equipment isn't mounted on the squad or this particular
            //  trooper, skip it
            if (m.getLocation() != BattleArmor.LOC_SQUAD 
                    && (m.getLocation() != trooper
                            || trooper == BattleArmor.LOC_SQUAD)){
                continue;
            }
            
            // Equipment assigned to this trooper but not mounted shouldn't be
            //  counted, unless it's squad-level equipment
            if (m.getLocation() == trooper && trooper != BattleArmor.LOC_SQUAD 
                    && m.getBaMountLoc() == BattleArmor.MOUNT_LOC_NONE){
                continue;
            }
            
            if (mt.hasFlag(MiscType.F_ENDO_STEEL)
                    || mt.hasFlag(MiscType.F_ENDO_COMPOSITE)
                    || mt.hasFlag(MiscType.F_ENDO_STEEL_PROTO)
                    || mt.hasFlag(MiscType.F_ENDO_COMPOSITE)
                    || mt.hasFlag(MiscType.F_COMPOSITE)
                    || mt.hasFlag(MiscType.F_INDUSTRIAL_STRUCTURE)
                    || mt.hasFlag(MiscType.F_REINFORCED)
                    || mt.hasFlag(MiscType.F_FERRO_FIBROUS)
                    || mt.hasFlag(MiscType.F_FERRO_FIBROUS_PROTO)
                    || mt.hasFlag(MiscType.F_FERRO_LAMELLOR)
                    || mt.hasFlag(MiscType.F_LIGHT_FERRO)
                    || mt.hasFlag(MiscType.F_HEAVY_FERRO)
                    || mt.hasFlag(MiscType.F_REACTIVE)
                    || mt.hasFlag(MiscType.F_REFLECTIVE)
                    || mt.hasFlag(MiscType.F_HARDENED_ARMOR)
                    || mt.hasFlag(MiscType.F_PRIMITIVE_ARMOR)
                    || mt.hasFlag(MiscType.F_COMMERCIAL_ARMOR)
                    || mt.hasFlag(MiscType.F_INDUSTRIAL_ARMOR)
                    || mt.hasFlag(MiscType.F_HEAVY_INDUSTRIAL_ARMOR)
                    || mt.hasFlag(MiscType.F_ANTI_PENETRATIVE_ABLATIVE)
                    || mt.hasFlag(MiscType.F_HEAT_DISSIPATING)
                    || mt.hasFlag(MiscType.F_IMPACT_RESISTANT)
                    || mt.hasFlag(MiscType.F_BALLISTIC_REINFORCED)
                    || mt.hasFlag(MiscType.F_HEAT_SINK)
                    || mt.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                    || mt.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                continue;
            }
            weightSum += mt.getTonnage(getEntity(), m.getLocation());
        }
        return weightSum;
    }
    
    public float getWeightWeapon(int trooper) {
        float weight = 0.0f;
        for (Mounted m : getEntity().getWeaponList()) {
            // If this equipment isn't mounted on the squad or this particular
            //  trooper, skip it
            if (m.getLocation() != BattleArmor.LOC_SQUAD 
                    && (m.getLocation() != trooper
                            || trooper == BattleArmor.LOC_SQUAD)){
                continue;
            }
            
            // Equipment assigned to this trooper but not mounted shouldn't be
            //  counted, unless it's squad-level equipment
            if (m.getLocation() == trooper && trooper != BattleArmor.LOC_SQUAD 
                    && m.getBaMountLoc() == BattleArmor.MOUNT_LOC_NONE){
                continue;
            }
            
            WeaponType wt = (WeaponType) m.getType();
            if (m.isDWPMounted()){
                weight += wt.getTonnage(getEntity()) * 0.75;
            } else {
                weight += wt.getTonnage(getEntity());
            }
        }
        return weight;
    }
    
    public float getWeightAmmo(int trooper) {
        float weight = 0.0f;
        for (Mounted m : getEntity().getAmmo()) {

            // If this equipment isn't mounted on the squad or this particular
            //  trooper, skip it
            if (m.getLocation() != BattleArmor.LOC_SQUAD 
                    && (m.getLocation() != trooper
                            || trooper == BattleArmor.LOC_SQUAD)){
                continue;
            }
            
            /// Equipment assigned to this trooper but not mounted shouldn't be
            //  counted, unless it's squad-level equipment
            if (m.getLocation() == trooper && trooper != BattleArmor.LOC_SQUAD 
                    && m.getBaMountLoc() == BattleArmor.MOUNT_LOC_NONE){
                continue;
            }

            AmmoType mt = (AmmoType) m.getType();
            weight += (mt.getKgPerShot() * m.getBaseShotsLeft())/1000.0;
        }
        return weight;
    }

    /**
     * There are some cases where we need to know the weight of an individual
     * trooper in the BattleArmor squad, this method provides that.
     * @param trooper
     * @return
     */
    public float calculateWeight(int trooper) {
        float weight = 0;
        weight += getWeightStructure();
        weight += getWeightArmor();

        weight += getWeightMiscEquip(trooper);
        weight += getWeightWeapon(trooper);
        weight += getWeightAmmo(trooper);

        return weight;
    }
    
    public float calculateWeight() {
        float totalWeight = 0.0f;
        for (int i = 1; i < ba.getTroopers(); i++){
            totalWeight += calculateWeight(i);
        }
        return totalWeight;
    }
    
}
