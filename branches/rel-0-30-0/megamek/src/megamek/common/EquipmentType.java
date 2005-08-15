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
    public static final int COST_VARIABLE = Integer.MIN_VALUE;

    public static final int     T_ENGINE_UNKNOWN            = -1;
    public static final int     T_ENGINE_ICE                = 0;
    public static final int     T_ENGINE_FUSION             = 1;
    public static final int     T_ENGINE_XL                 = 2;
    public static final int     T_ENGINE_LIGHT              = 4; // don't ask
    public static final int     T_ENGINE_XXL                = 3; // don't ask

    public static final int     T_ARMOR_UNKNOWN             = -1;
    public static final int     T_ARMOR_STANDARD            = 0;
    public static final int     T_ARMOR_FERRO_FIBROUS       = 1;
    public static final int     T_ARMOR_REACTIVE            = 2;
    public static final int     T_ARMOR_REFLECTIVE          = 3;
    public static final int     T_ARMOR_HARDENED            = 4;
    public static final int     T_ARMOR_LIGHT_FERRO         = 5;
    public static final int     T_ARMOR_HEAVY_FERRO         = 6;
    public static final int     T_ARMOR_PATCHWORK           = 7;
    public static final int     T_ARMOR_STEALTH             = 8;
    public static final int     T_ARMOR_FERRO_FIBROUS_PROTO = 9;

    public static final int     T_STRUCTURE_UNKNOWN         = -1;
    public static final int     T_STRUCTURE_STANDARD        = 0;
    public static final int     T_STRUCTURE_ENDO_STEEL      = 1;
    public static final int     T_STRUCTURE_ENDO_PROTOTYPE  = 2;
    public static final int     T_STRUCTURE_REINFORCED      = 3;
    public static final int     T_STRUCTURE_COMPOSITE       = 4;

    public static final String[] armorNames = {"Standard",
                                            "Ferro-Fibrous",
                                            "Reactive",
                                            "Reflective",
                                            "Hardened",
                                            "Light Ferro-Fibrous",
                                            "Heavy Ferro-Fibrous",
                                            "Patchwork",
                                            "Stealth",
                                            "Ferro-Fibrous Prototype"};

    public static final String[] structureNames = {"Standard",
                                            "Endo Steel",
                                            "Endo Steel Prototype",
                                            "Reinforced",
                                            "Composite"};

    public static final int[] structureLevels = {1,
                                            2,
                                            3,
                                            3,
                                            3};

    public static final double[] structureCosts = {400,
                                            1600,
                                            1600,   // Assume for now that prototype is not more expensive
                                            6400,
                                            1600};

    public static final double[] armorCosts = {10000,
                                            20000,
                                            30000,
                                            20000,
                                            15000,
                                            15000,
                                            25000,
                                            10000,  // This is obviously wrong...
                                            50000,
                                            20000};   // Assume for now that prototype is not more expensive

    public static final double[] armorPointMultipliers = {1,
                                            1.12,
                                            1,
                                            1,
                                            1,
                                            1.06,
                                            1.24,
                                            1,
                                            1,
                                            1.12};
    public static final double POINT_MULTIPLIER_UNKNOWN = 1;
    public static final double POINT_MULTIPLIER_CLAN_FF = 1.2;

    protected String    name = null;

    protected String    internalName = null;

    private Vector      namesVector = new Vector();

    protected float     tonnage = 0;
    protected int       criticals = 0;

    protected boolean   explosive = false;
    protected boolean   hittable = true; // if false, reroll critical hits
    // can the crits for this be spread over locations?
    protected boolean   spreadable = false;
    protected int       toHitModifier = 0;
    protected int       techLevel = TechConstants.T_TECH_UNKNOWN;

    protected int       flags = 0;

    protected double     bv = 0; // battle value point system
    protected double    cost = 0; // The C-Bill cost of the item.

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

    public int getTechLevel()
    {
        return techLevel;
    }
    
    public float getTonnage(Entity entity) {
        return tonnage;
    }

    public int getCriticals(Entity entity) {
        return criticals;
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

    /**
     * 
     * @return <code>true</code> if this type of equipment has set of modes
     * that it can be in.
     */
    public boolean hasModes() {
        return modes != null;
    }

    /**
     * 
     * @return the number of modes that this type of equipment can be in or
     * <code>0</code> if it doesn't have modes.
     */
    public int getModesCount() {
        if (modes != null)
            return modes.size();
        else
            return 0;
    }

    /**
     * 
     * @return <code>Enumeration</code> of the <code>EquipmentMode</code> 
     * that this type of equipment can be in
     */
    public Enumeration getModes() {
        if (modes != null) {
            return modes.elements();
        } else {
            return new Enumeration() {
                public boolean hasMoreElements() {
                    return false;
                }
                public Object nextElement() {
                    return null;
                }
                
            };
        }
    }

    /**
     * Sets the modes that this type of equipment can be in. By default the EquipmentType
     * doesn't have the modes, so don't try to call this method with null or empty argument. 
     * @param modes non null, non empty list of available mode names.
     */
    protected void setModes(String[] modes) {
        megamek.debug.Assert.assertTrue(modes != null && modes.length >= 0, 
                "List of modes must not be null or empty");
        Vector newModes = new Vector(modes.length);
        for (int i = 0 ,l = modes.length; i < l; i++) {
            newModes.addElement(EquipmentMode.getMode(modes[i]));
        }
        this.modes = newModes;
    }

    /**
     * <p>Returns the mode number <code>modeNum</code> from the list of modes available
     * for this type of equipment. Modes are numbered from <code>0<code> to 
     * <code>getModesCount()-1</code>
     * <p>Fails if this type of the equipment doesn't have modes, or given mode is out of
     * the valid range.    
     * @param modeNum
     * @return mode number <code>modeNum</code> from the list of modes available
     * for this type of equipment.
     * @see getModesCount
     * @see hasModes  
     */
    public EquipmentMode getMode(int modeNum) {
        megamek.debug.Assert.assertTrue(modes != null && modeNum >= 0 && modeNum < modes.size());
        return (EquipmentMode)modes.elementAt(modeNum);
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

    public static int getArmorType(String inType) {
        for (int x=0; x<armorNames.length; x++) {
            if ((armorNames[x].indexOf(inType) >= 0) || (inType.indexOf(armorNames[x]) >= 0))
                return x;
        }
        return T_ARMOR_UNKNOWN;
    }

    public static  String getArmorTypeName(int armorType) {
        if ((armorType < 0) || (armorType >= armorNames.length))
            return null;
        return armorNames[armorType];
    }

    public static int getStructureType(String inType) {
        for (int x=0; x<structureNames.length; x++) {
            if ((structureNames[x].indexOf(inType) >= 0) || (inType.indexOf(structureNames[x]) >= 0))
                return x;
        }
        return T_STRUCTURE_UNKNOWN;
    }

    public static  String getStructureTypeName(int structureType) {
        if ((structureType < 0) || (structureType >= structureNames.length))
            return null;
        return structureNames[structureType];
    }

    /**
     * @return The C-Bill cost of the piece of equipment.
     */
    public double getCost() {
        return cost;
    }

    public static double getArmorCost(int inArmor) {
        if ((inArmor < 0) || (inArmor >= armorCosts.length))
            return -1;
        return armorCosts[inArmor];
    }

    public static double getStructureCost(int inStructure) {
        if ((inStructure < 0) || (inStructure >= structureCosts.length))
            return -1;
        return structureCosts[inStructure];
    }

    public static double getArmorPointMultiplier(int inArmor) {
        return getArmorPointMultiplier(inArmor, TechConstants.T_IS_LEVEL_2);
    }

    public static double getArmorPointMultiplier(int inArmor, int inTechLevel) {
        return getArmorPointMultiplier(inArmor, ((inTechLevel == TechConstants.T_CLAN_LEVEL_2) || (inTechLevel == TechConstants.T_CLAN_LEVEL_3)));
    }

    public static double getArmorPointMultiplier(int inArmor, boolean clanArmor) {
        if ((inArmor < 0) || (inArmor >= armorPointMultipliers.length))
            return POINT_MULTIPLIER_UNKNOWN;
        if ((inArmor == T_ARMOR_FERRO_FIBROUS) && clanArmor)
            return POINT_MULTIPLIER_CLAN_FF;
        return armorPointMultipliers[inArmor];
    }
    
    //stuff like hatchets, which depend on an unknown quality (usually tonnage of the unit.)
    //entity is whatever has this item
    public int resolveVariableCost(Entity entity) {
        //TODO implement this!
        int cost=0;
        if(this instanceof MiscType) {
            if(this.hasFlag(MiscType.F_MASC)) {
                //masc=engine rating*masc tonnage*1000
                int mascTonnage=0;
                if (this.getInternalName().equals("ISMASC")) {
                    mascTonnage = Math.round(entity.getWeight() / 20.0f);
                } else if (this.getInternalName().equals("CLMASC")) {
                    mascTonnage = Math.round(entity.getWeight() / 25.0f);
                }
                cost=mascTonnage*entity.getOriginalWalkMP()*(int)entity.getWeight()*1000;
            } else if(this.hasFlag(MiscType.F_TARGCOMP)) {
                int tCompTons=0;
                float fTons = 0.0f;
                for (Enumeration i = entity.getWeapons(); i.hasMoreElements();)
                {
                    Mounted mo = (Mounted)i.nextElement();
                    WeaponType wt = (WeaponType)mo.getType();
                    if (wt.hasFlag(WeaponType.F_DIRECT_FIRE))
                    fTons += wt.getTonnage(entity);
                }
                if (this.getInternalName().equals("ISTargeting Computer")) {
                    tCompTons=(int)Math.ceil(fTons / 4.0f);
                } else if (this.getInternalName().equals("CLTargeting Computer")) {
                    tCompTons=(int)Math.ceil(fTons / 5.0f);
                }
                cost=tCompTons*10000;
            } else if(this.hasFlag(MiscType.F_HATCHET)) {
                int hatchetTons=(int) Math.ceil(entity.getWeight() / 15.0);
                cost=hatchetTons*5000;
            } else if(this.hasFlag(MiscType.F_SWORD)) {
                int swordTons=(int) Math.ceil(entity.getWeight() / 15.0);
                cost=swordTons*10000;
            }
        } else {
            if(cost==0) {
                //if we don't know what it is...
                System.out.println("I don't know how much " + this.name + " costs.");
            }
        }
        return cost;
    }
}
