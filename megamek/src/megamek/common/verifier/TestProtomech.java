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

import megamek.common.Entity;
import megamek.common.Protomech;

/**
 * @author Neoancient
 *
 */
public class TestProtomech extends TestEntity {
    
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
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getWeightMisc() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getWeightHeatSinks() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        return heatNeutralHSRequirement();
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
        int engineRating = Math.max(1, (int)(moveFactor * proto.getWeight()));
        // Engine ratings over 40 have to be rounded up to the nearest 5.
        if (engineRating > 40) {
            return (int) Math.ceil(engineRating / 5.0) * 5;
        }
        return engineRating;
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
