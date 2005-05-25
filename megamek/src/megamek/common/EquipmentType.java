/*
 * MegaMek - Copyright (C) 2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
 * EquipmentType.java
 *
 * Created on April 1, 2002, 1:35 PM
 */

package megamek.common;

import java.util.*;

/**
 * Represents any type of equipment mounted on a mechs, excluding systems and 
 * actuators.
 *
 * @author  Ben
 * @version 
 */
public class EquipmentType {
    
    public static final float TONNAGE_VARIABLE = Float.MIN_VALUE;
    public static final int CRITICALS_VARIABLE = Integer.MIN_VALUE;
    public static final int BV_VARIABLE = Integer.MIN_VALUE;
    
    
    protected String    name = null;

    protected String    internalName = null;

    private Vector      namesVector = new Vector();

    protected float     tonnage = 0;
    protected int       criticals = 0;
    protected int       techType = TechConstants.T_IS_LEVEL_1;
    
    protected boolean   explosive = false;
    protected boolean   hittable = true; // if false, reroll critical hits
    // can the crits for this be spread over locations?
    protected boolean   spreadable = false;
    protected int       toHitModifier = 0;

    protected int       flags = 0;
      
    protected double     bv = 0; // battle value point system
    
    /**
     * what modes can this equipment be in?
     */
    protected Vector modes = null;
    
    /**
     * can modes be switched instantly, or at end of turn?
     */
    protected boolean instantModeSwitch = true;
    
    // static list of eq
    protected static Vector allTypes;
    protected static Hashtable lookupHash;

    /** Creates new EquipmentType */
    public EquipmentType() {
        ;
    }

    public String getName() {
        return name;
    }
    
    public String getDesc() {
        String result = EquipmentMessages.getString("EquipmentType."+name);
        if (result != null)
            return result;
        else
            return name;
    }
    
    public String getInternalName() {
        return internalName;
    }
    
    public float getTonnage(Entity entity) {
        return tonnage;
    }

    public int getCriticals(Entity entity) {
        return criticals;
    }
    
    public int getTechType() {
        return techType;
    }
    
    public boolean isExplosive() {
        return explosive;
    }
    
    public boolean isHittable() {
        return hittable;
    }
    
    // like margarine!
    public boolean isSpreadable() { 
        return spreadable;
    }
    
    public int getToHitModifier() {
        return toHitModifier;
    }
    
    public int getFlags() {
        return flags;
    }
    
    public boolean hasFlag(int flag) {
        return (flags & flag) != 0;
    }

    public double getBV(Entity entity) {
        return bv;
    }
    
    public Enumeration getModes() {
        return modes.elements();
    }

    public boolean hasModes() {
        return modes != null;
    }

    public int getModesCount() {
        if (modes != null)
            return modes.size();
        else
            return 0;
    }
    
    protected void setModes(String[] modes) {
        Vector newModes  = null;
        if (modes != null) {
            newModes = new Vector(modes.length);
            for (int i = 0 ,l = modes.length; i < l; i++) {
                newModes.addElement(EquipmentMode.getMode(modes[i]));
            }
        }
        this.modes = newModes;
    }

    public EquipmentMode getMode(int mode) {
        megamek.debug.Assert.assertTrue(modes != null && mode >= 0 && mode < modes.size());
        return (EquipmentMode)modes.elementAt(mode);
    }
    
    public void setInstantModeSwitch(boolean b) {
        instantModeSwitch = b;
    }
    
    public boolean hasInstantModeSwitch() {
        return instantModeSwitch;
    }

    public void setInternalName(String s) {
        internalName = s;
        addLookupName(s);
    }

    public void addLookupName(String s) {
        EquipmentType.lookupHash.put(s, this); // static variable
        namesVector.addElement(s); // member variable
    }

    public static EquipmentType get(String key) {
        if ( null == EquipmentType.lookupHash ) {
            EquipmentType.initializeTypes();
        }
        return (EquipmentType) EquipmentType.lookupHash.get(key);
    }

    public Enumeration getNames() {
        return namesVector.elements();
    }

    public static void initializeTypes() {
        EquipmentType.allTypes = new Vector();
        EquipmentType.lookupHash = new Hashtable();
        
        // will I need any others?
        WeaponType.initializeTypes();
        AmmoType.initializeTypes();
        MiscType.initializeTypes();
    }
    
    public static Enumeration getAllTypes() {
        if ( null == EquipmentType.allTypes ) {
            EquipmentType.initializeTypes();
        }
        return EquipmentType.allTypes.elements();
    }
    
    protected static void addType(EquipmentType type) {
        if ( null == EquipmentType.allTypes ) {
            EquipmentType.initializeTypes();
        }
        EquipmentType.allTypes.addElement(type);
    }
    
}
