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
    public static final int EFFECT_NONE = 0;
    public static final int EFFECT_CRITICAL =                0x0001;
    public static final int EFFECT_VEHICLE_MOVE_DAMAGED =    0x0002;
    public static final int EFFECT_GUN_EMPLACEMENT_WEAPONS = 0x0004;
    public static final int EFFECT_GUN_EMPLACEMENT_TURRET =  0x0008;
    public static final int EFFECT_GUN_EMPLACEMENT_CREW =    0x0010;
    public static final int EFFECT_NO_CRITICALS =            0x0020;
    
    public static final int DAMAGE_NONE = -1;
    public static final int DAMAGE_PHYSICAL = -2;
    public static final int DAMAGE_ENERGY = -3;
    public static final int DAMAGE_MISSLE = -4;
    public static final int DAMAGE_BALLISTIC = -5;
    
    private int location;
    private boolean rear;
    private int effect;
    private boolean hitAimedLocation = false;
    private int specCritMod = 0;
    private int motiveMod = 0;
    private int glancing = 0;
    private boolean fromFront = true; // True if attack came in through hex in
                                      // front of target
    // in case of usage of Edge it is document what the previous location was
    private HitData undoneLocation = null;
    private boolean fallDamage = false; //did the damage come from a fall?
    private int damageType = HitData.DAMAGE_NONE;
    
    
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
    
    public HitData(int location, boolean rear, int effect, boolean hitAimedLocation, int specCrit) 
    {
        this(location, rear, effect, hitAimedLocation, specCrit, true, HitData.DAMAGE_NONE);
        
    }
    
    public HitData (int location, boolean rear, int effect, boolean hitAimedLocation, int specCrit, boolean fromWhere, int damageType) {
        this.location = location;
        this.rear = rear;
        this.effect = effect;
        this.hitAimedLocation = hitAimedLocation;
        this.specCritMod = specCrit;
        this.fromFront = fromWhere;
        this.damageType = damageType;
        
    }
    
    public HitData (int location, boolean rear, int effect, boolean hitAimedLocation, int specCrit, boolean fromWhere, long damageType) {
        this.location = location;
        this.rear = rear;
        this.effect = effect;
        this.hitAimedLocation = hitAimedLocation;
        this.specCritMod = specCrit;
        this.fromFront = fromWhere;
    }
    
    public void setFromFront (boolean dir) {
        fromFront = dir;
    }
    
    public boolean isFromFront () {
        return fromFront;
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
    
    public int glancingMod () {
        return glancing;
    }

    public int getSpecCritMod() {
        return specCritMod;
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
    
    public int getMotiveMod() {
        return motiveMod;
    }
    
    public void setMotiveMod(int mod) {
        motiveMod = mod;
    }
    
    public void setEffect(int effect) {
        this.effect= effect;
    }
    
    public void setSpecCritmod(int val) {
        specCritMod = val;
    }

    public boolean hitAimedLocation() {
        return hitAimedLocation;
    }

    public HitData getUndoneLocation () {
        return undoneLocation;
    }
    
    public void setUndoneLocation(HitData previousLocation) {
        undoneLocation = previousLocation;
    }
    
    public void makeFallDamage(boolean fall){
        this.fallDamage = fall;
        this.damageType = HitData.DAMAGE_PHYSICAL;
    }
    
    public boolean isFallDamage(){
        return fallDamage;
    }

    public int getDamageType() {
        return damageType;
    }

    
    public void setDamageType(int type) {
        this.damageType = type;
    }
}
