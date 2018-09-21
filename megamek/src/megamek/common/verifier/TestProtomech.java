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

}
