/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

/**
 * This is a result from the hit chart.
 */
public class HitData
{
    public static final int        EFFECT_NONE = 0;
    public static final int        EFFECT_CRITICAL = 1;
    public static final int        EFFECT_VEHICLE_MOVE_DESTROYED = 2;
    public static final int        EFFECT_VEHICLE_MOVE_DAMAGED = 3;
    public static final int        EFFECT_VEHICLE_TURRETLOCK = 4;
    
    private int location;
    private boolean rear;
    private int effect;
    private boolean hitAimedLocation = false;
    private int specCritMod = 0;
    private int glancing = 0;
    
    public HitData(int location) {
        this(location, false, EFFECT_NONE, false, 0);
    }
    
    public HitData(int location, boolean rear) {
        this(location, rear, EFFECT_NONE, false, 0);
    }
    
    public HitData(int location, boolean rear, int effects) {
        this(location, rear, effects, false, 0);
    }
    
    public HitData(int location, boolean rear, boolean hitAimedLocation) {
        this(location, rear, EFFECT_NONE, hitAimedLocation, 0);
    }
    
    public HitData(int location, boolean rear, int effect, boolean hitAimedLocation, int specCrit) {
        this.location = location;
        this.rear = rear;
        this.effect = effect;
        this.hitAimedLocation = hitAimedLocation;
        this.specCritMod = specCrit;
    }
    
    public void makeArmorPiercing(AmmoType inType)
    {
        if (inType.getRackSize() == 2)
                specCritMod = -4;
        else if (inType.getRackSize() == 5)
                specCritMod = -3;
        else if (inType.getRackSize() == 10)
                specCritMod = -2;
        else if (inType.getRackSize() == 20)
                specCritMod = -1;
    }
    
    public void makeGlancingBlow () {
        glancing = -2;
    }
    
    public int getSpecCritMod() {
        return specCritMod+glancing;
    }
    
    public int getLocation() {
        return location;
    }
    
    public boolean isRear() {
        return rear;
    }
    
    public int getEffect() {
        return effect;
    }

    public boolean hitAimedLocation() {
        return hitAimedLocation;
    }
}
