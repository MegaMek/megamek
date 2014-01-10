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

import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;


public class TestBattleArmor extends TestEntity {
    
    /**
     * An enumeration that keeps track of the legal armors for BattleArmor.  
     * Each entry consists of the type, which 
     * corresponds to the types defined in <code>EquipmentType</code>.
     * 
     * @author arlith
     *
     */
    public static enum BAArmor{
        STANDARD(EquipmentType.T_ARMOR_STANDARD,false),   
        CLAN_STANDARD(EquipmentType.T_ARMOR_STANDARD,true),
        STANDARD_PROTOTYPE(EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE,false),
        STANDARD_ADVANCED(EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED,false),
        STEALTH_BASIC(EquipmentType.T_ARMOR_BA_STEALTH_BASIC,false),
        STEALTH(EquipmentType.T_ARMOR_BA_STEALTH,false),
        STEALTH_IMPROVED(EquipmentType.T_ARMOR_BA_STEALTH_IMP,false),
        STEALTH_PROTOTYPE(EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE,false),
        FIRE_RESISTANT(EquipmentType.T_ARMOR_BA_FIRE_RESIST,false),
        MIMETIC(EquipmentType.T_ARMOR_BA_MIMETIC,false);

        /**
         * The type, corresponding to types defined in 
         * <code>EquipmentType</code>.
         */
        public int type;
        
        /**
         * Denotes whether this armor is Clan or not.
         */
        public boolean isClan;
        
        BAArmor(int t, boolean c){
            type = t;
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
        public static BAArmor getArmor(int t, boolean c){
            for (BAArmor a : values()){
                if (a.type == t && a.isClan == c){
                    return a;
                }
            }
            return null;
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
    
    //@Override
    //public float calculateWeight() {
    //    return infantry.getWeight();
    //}
    
}