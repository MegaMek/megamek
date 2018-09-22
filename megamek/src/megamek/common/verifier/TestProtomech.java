/*
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.ITechManager;
import megamek.common.MiscType;
import megamek.common.Protomech;
import megamek.common.SimpleTechLevel;
import megamek.common.TechConstants;
import megamek.common.verifier.TestBattleArmor.BAArmor;

/**
 * @author Neoancient
 *
 */
public class TestProtomech extends TestEntity {
    
    public enum ProtomechJumpJets {
        JJ_STANDARD ("ProtomechJumpJet", false),
        JJ_EXTENDED ("ExtendedJumpJetSystem", true),
        JJ_UMU ("ProtomechUMU", false);
        
        private final String internalName;
        private final boolean improved;
        
        private ProtomechJumpJets(String internalName, boolean improved) {
            this.internalName = internalName;
            this.improved = improved;
        }
        
        public String getName() {
            return internalName;
        }
        
        public boolean isImproved() {
            return improved;
        }
        
        public static List<EquipmentType> allJJs() {
            return Arrays.stream(values())
                    .map(jj -> EquipmentType.get(jj.internalName))
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * All the protomech armor options. Both of the them.
     *
     */
    public static enum ProtomechArmor {
        STANDARD (EquipmentType.T_ARMOR_STANDARD, true, 0, 0.05),
        EDP (EquipmentType.T_ARMOR_EDP, true, 1, 0.075);

        private final int type;
        private final boolean isClan;
        private final int torsoSlots;
        private final double wtPerPoint;

        ProtomechArmor(int t, boolean c, int slots, double weight) {
            type = t;
            isClan = c;
            torsoSlots = slots;
            wtPerPoint = weight;
        }

        public static int armorCount() {
            return values().length;
        }

        /**
         * Given an armor type, return the {@link ProtomechArmor} instance that
         * represents that type.
         *
         * @param t   The armor type.
         * @param c   Whether this armor type is Clan or not.
         * @return    The {@link ProtomechArmor} that corresponds to the given type
         *            or null if no match was found.
         */
        public static ProtomechArmor getArmor(int t, boolean c) {
            for (ProtomechArmor a : values()) {
                if ((a.type == t) && (a.isClan == c)) {
                    return a;
                }
            }
            return null;
        }
        
        /**
         * @return The {@link MiscType} for this armor.
         */
        public EquipmentType getArmorEqType() {
            String name = EquipmentType.getArmorTypeName(type, isClan);
            return EquipmentType.get(name);
        }
        
        public int getType() {
            return type;
        }
        
        public boolean isClan() {
            return isClan;
        }
        
        public int getTorsoSlots() {
            return torsoSlots;
        }
        
        public double getWtPerPoint() {
            return wtPerPoint;
        }
    }

    /**
     * Filters all protomech armor according to given tech constraints
     * 
     * @param techManager
     * @return A list of all armors that meet the tech constraints
     */
    public static List<EquipmentType> legalArmorsFor(ITechManager techManager) {
        List<EquipmentType> retVal = new ArrayList<>();
        for (ProtomechArmor armor : ProtomechArmor.values()) {
            final EquipmentType eq = armor.getArmorEqType();
            if ((null != eq) && techManager.isLegal(eq)) {
                retVal.add(eq);
            }
        }
        return retVal;
    }
        
    private final Protomech proto;
    private final String fileString;

    public TestProtomech(Protomech proto, TestEntityOption option, String fileString) {
        super(option, proto.getEngine(), getArmor(proto), null);
        this.proto = proto;
        this.fileString = fileString;
    }

    private static Armor[] getArmor(Protomech proto) {
        Armor[] armor = new Armor[proto.locations()];
        for (int i = 0; i < proto.locations(); i++) {
            int type = proto.getArmorType(i);
            int flag = 0;
            if (proto.isClanArmor(i)) {
                flag |= Armor.CLAN_ARMOR;
            }
            armor[i] = new Armor(type, flag);
        }
        return armor;
    }
    
    @Override
    public Entity getEntity() {
        return proto;
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
    public boolean isSmallCraft() {
        return false;
    }

    @Override
    public boolean isAdvancedAerospace() {
        return false;
    }
    
    @Override
    public boolean isProtomech() {
        return true;
    }
    
    @Override
    public double getWeightStructure() {
        return proto.getWeight() * 0.1;
    }
    
    @Override
    public double getWeightEngine() {
        return proto.getEngine().getWeightEngine(proto, Ceil.KILO);
    }

    @Override
    public double getWeightControls() {
        return (proto.getWeight() > 9)? 0.75 : 0.5;
    }

    @Override
    public double getWeightMisc() {
        return 0;
    }

    @Override
    public double getWeightHeatSinks() {
        return getCountHeatSinks() * 0.25;
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        return heatNeutralHSRequirement();
    }
    
    @Override
    public double getWeightAllocatedArmor() {
        ProtomechArmor armor = ProtomechArmor.getArmor(proto.getArmorType(0),
                TechConstants.isClan(proto.getArmorTechLevel(0)));
        double wtPerPoint = 0.0;
        if (null != armor) {
            wtPerPoint = armor.getWtPerPoint();
        }
        return proto.getTotalArmor() * wtPerPoint;
    }

    @Override
    public String printWeightMisc() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String printWeightControls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public StringBuffer printEntity() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getWeightPowerAmp() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    /**
     * Determine the minimum walk MP for the protomech based on configuration
     * 
     * @param proto The protomech
     * @return      The minimum walk MP
     */
    public int getMinimumWalkMP(Protomech proto) {
        if (proto.isGlider()) {
            return 4;
        } else if (proto.isQuad()) {
            return 3;
        } else {
            return 1;
        }
    }
    
    /**
     * Computes the required engine rating
     * 
     * @param proto The protomech
     * @return      The engine rating required for the weight, speed, and configuration
     */
    public static int calcEngineRating(Protomech proto) {
        int moveFactor = (int) Math.ceil(proto.getOriginalWalkMP() * 1.5);
        // More efficient engine use for gliders and quads
        if (proto.isGlider() || proto.isQuad()) {
            moveFactor -= 2;
        }
        return Math.max(1, (int)(moveFactor * proto.getWeight()));
    }

    /**
     * Equipment slot limit by location
     * 
     * @param loc   The Protomech location
     * @param quad  Whether the protomech is a quad
     * @param ultra Whether the protomech is ultraheavy
     * @return      The number of equipment slots in the location
     */
    public static int maxSlotsByLocation(int loc, boolean quad, boolean ultra) {
        switch(loc) {
            case Protomech.LOC_TORSO: {
                int slots = 2;
                if (ultra) {
                    slots++;
                }
                if (quad) {
                    slots += slots;
                }
                return slots;
            }
            case Protomech.LOC_LARM:
            case Protomech.LOC_RARM:
                return quad? 0 : 1;
            case Protomech.LOC_MAINGUN:
                return (quad && ultra)? 2 : 1;
            case Protomech.LOC_HEAD:
            case Protomech.LOC_LEG:
            default:
                return 0;
        }
    }
    
    /**
     * The maximum total weight that can be mounted in a given location.
     * 
     * @param loc   The Protomech location
     * @param quad  Whether the protomech is a quad
     * @param ultra Whether the protomech is ultraheavy
     * @return      The weight limit for that location, in tons.
     */
    public static double maxWeightByLocation(int loc, boolean quad, boolean ultra) {
        switch(loc) {
            case Protomech.LOC_TORSO:
                if (quad) {
                    return ultra? 8.0 : 5.0;
                } else {
                    return ultra? 4.0 : 2.0;
                }
            case Protomech.LOC_LARM:
            case Protomech.LOC_RARM:
                return quad? 0.0 : 0.5;
            case Protomech.LOC_MAINGUN:
                return Double.MAX_VALUE;
            case Protomech.LOC_HEAD:
            case Protomech.LOC_LEG:
            default:
                return 0.0;
        }
    }
    
    private static final int MAX_ARMOR_FACTOR[] = { 15, 17, 22, 24, 33, 35, 40, 42, 51, 53, 58, 60, 65, 67 };
    
    /**
     * Calculate the maximum armor factor based on weight and whether there is a main gun location
     * 
     * @param weight  The weight of the protomech in tons
     * @param mainGun Whether the protomech has a main gun location
     * @return        The maximum total number of armor points
     */
    public static int maxArmorFactor(double weight, boolean mainGun) {
        final int weightIndex = Math.max(0, (int) weight - 2);
        int base = MAX_ARMOR_FACTOR[Math.min(weightIndex, MAX_ARMOR_FACTOR.length - 1)];
        if (mainGun) {
            return base + ((weight > 9)? 6 : 3);
        }
        return base;
    }
}
