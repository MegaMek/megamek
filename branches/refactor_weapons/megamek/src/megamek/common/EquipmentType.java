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
import megamek.common.weapons.*;
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
    
    // what modes can this equipment be in?
    protected String[] m_saModes = null;
    
    // can modes be switched instantly, or at end of turn?
    protected boolean m_bInstantModeSwitch = true;
    
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
    
    public String[] getModes() {
        return m_saModes;
    }
    
    public void setModes(String[] sa) {
        m_saModes = sa;
    }
    
    public boolean hasModes() {
        return m_saModes != null;
    }
    
    public void setInstantModeSwitch(boolean b) {
        m_bInstantModeSwitch = b;
    }
    
    public boolean hasInstantModeSwitch() {
        return m_bInstantModeSwitch;
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
        EquipmentType equip=(EquipmentType) EquipmentType.lookupHash.get(key);
        
        return equip;
        
    }

    public Enumeration getNames() {
        return namesVector.elements();
    }
    //FIXME won't work with dynamic weapon types as written
    public static void initializeTypes() {
        EquipmentType.allTypes = new Vector();
        EquipmentType.lookupHash = new Hashtable();
        
        // will I need any others?
       // WeaponType.initializeTypes();
        initializeWeaponTypes();
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
    private static void initializeWeaponTypes() {
        //Laser types
    	addType(new ISMediumLaser());
    	addType(new ISLargeLaser());
    	addType(new ISSmallLaser());
    	addType(new ISLargePulseLaser());
    	addType(new ISERLargeLaser());
    	addType(new ISERMediumLaser());
    	addType(new ISMediumPulseLaser());
    	addType(new ISSmallPulseLaser());
    	addType(new ISERSmallLaser());
    	addType(new CLERLargeLaser());
    	addType(new CLHeavyLargeLaser());
    	addType(new CLLargePulseLaser());
    	addType(new CLERMediumLaser());
    	addType(new CLHeavyMediumLaser());
    	addType(new CLMediumPulseLaser());
    	addType(new CLERSmallLaser());
    	addType(new CLSmallPulseLaser());
    	addType(new CLHeavySmallLaser());
    	addType(new CLERMicroLaser());
    	addType(new CLMicroPulseLaser());
    	//PPC types
    	addType(new ISPPC());
    	addType(new ISERPPC());
    	addType(new CLERPPC());
    }
    
}
