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
    
    /** can the crits for this be spread over locations? */
    protected boolean   spreadable = false;
    protected int       toHitModifier = 0;
    protected int       techLevel = TechConstants.T_TECH_UNKNOWN;

    protected int       flags = 0;
    protected int       subType = 0;


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

    public void setFlags(int inF) {
        flags = inF;
    }

    public void setSubType(int newFlags) {
        subType = newFlags;
    }

    public void addSubType(int newFlag) {
        subType |= newFlag;
    }

    public boolean hasSubType(int testFlag) {
        return (subType & testFlag) != 0;
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
        if (null == EquipmentType.lookupHash) {
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
        //WeaponType.initializeTypes();
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
        addType(new ISLargeXPulseLaser());
    	addType(new ISERLargeLaser());
        addType(new ISERLargeLaserPrototype());
    	addType(new ISERMediumLaser());
    	addType(new ISMediumPulseLaser());
        addType(new ISMediumPulseLaserPrototype());
        addType(new ISMediumXPulseLaser());
    	addType(new ISSmallPulseLaser());
        addType(new ISSmallXPulseLaser());
    	addType(new ISERSmallLaser());
    	addType(new CLERLargeLaser());
    	addType(new CLHeavyLargeLaser());
    	addType(new CLLargePulseLaser());
        addType(new CLERLargePulseLaser());
    	addType(new CLERMediumLaser());
    	addType(new CLHeavyMediumLaser());
    	addType(new CLMediumPulseLaser());
        addType(new CLERMediumPulseLaser());
    	addType(new CLERSmallLaser());
    	addType(new CLSmallPulseLaser());
        addType(new CLERSmallPulseLaser());
    	addType(new CLHeavySmallLaser());
    	addType(new CLERMicroLaser());
    	addType(new CLMicroPulseLaser());
    	//PPC types
    	addType(new ISPPC());
    	addType(new ISERPPC());
    	addType(new CLERPPC());
    	//Flamers
    	addType(new CLFlamer());
    	addType(new ISFlamer());
        addType(new CLVehicleFlamer());
        addType(new ISVehicleFlamer());
        addType(new ISHeavyFlamer());
    	//Autocannons
    	addType(new ISAC2());
    	addType(new ISAC5());
    	addType(new ISAC10());
    	addType(new ISAC20());
    	//Ultras
    	addType(new ISUAC2());
    	addType(new ISUAC5());
        addType(new ISUAC5Prototype());
    	addType(new ISUAC10());
    	addType(new ISUAC20());
        addType(new ISTHBUAC2());
        addType(new ISTHBUAC10());
        addType(new ISTHBUAC20());
    	addType(new CLUAC2());
    	addType(new CLUAC5());
    	addType(new CLUAC10());
    	addType(new CLUAC20());
    	//LBXs
    	addType(new ISLB2XAC());
    	addType(new ISLB5XAC());
    	addType(new ISLB10XAC());
        addType(new ISLB10XACPrototype());
    	addType(new ISLB20XAC());
    	addType(new CLLB2XAC());
    	addType(new CLLB5XAC());
    	addType(new CLLB10XAC());
    	addType(new CLLB20XAC());
        addType(new ISTHBLB2XAC());
        addType(new ISTHBLB5XAC());
        addType(new ISTHBLB20XAC());
    	//RACs
        addType(new ISRAC2());
        addType(new ISRAC5());
        //Gausses
        addType(new ISGaussRifle());
        addType(new ISGaussRiflePrototype());
        addType(new CLGaussRifle());
        addType(new ISLGaussRifle());
        addType(new ISHGaussRifle());
        //MGs
        addType(new ISMG());
        addType(new CLMG());
        addType(new CLLMG());
        addType(new CLHMG());
        //LRMs
        addType(new ISLRM5());
        addType(new ISLRM10());
        addType(new ISLRM15());
        addType(new ISLRM20());
        addType(new ISLRM5OS());
        addType(new ISLRM10OS());
        addType(new ISLRM15OS());
        addType(new ISLRM20OS());
        addType(new CLLRM5());
        addType(new CLLRM10());
        addType(new CLLRM15());
        addType(new CLLRM20());
        addType(new CLLRM5OS());
        addType(new CLLRM10OS());
        addType(new CLLRM15OS());
        addType(new CLLRM20OS());
        addType(new CLStreakLRM5());
        addType(new CLStreakLRM10());
        addType(new CLStreakLRM15());
        addType(new CLStreakLRM20());
        addType(new CLStreakLRM5OS());
        addType(new CLStreakLRM10OS());
        addType(new CLStreakLRM15OS());
        addType(new CLStreakLRM20OS());
        //LRTs
        addType(new ISLRT5());
        addType(new ISLRT10());
        addType(new ISLRT15());
        addType(new ISLRT20());
        addType(new ISLRT5OS());
        addType(new ISLRT10OS());
        addType(new ISLRT15OS());
        addType(new ISLRT20OS());
        addType(new CLLRT5());
        addType(new CLLRT10());
        addType(new CLLRT15());
        addType(new CLLRT20());
        addType(new CLLRT5OS());
        addType(new CLLRT10OS());
        addType(new CLLRT15OS());
        addType(new CLLRT20OS());
        //SRMs
        addType(new ISSRM2());
        addType(new ISSRM4());
        addType(new ISSRM6());
        addType(new ISSRM2OS());
        addType(new ISSRM4OS());
        addType(new ISSRM6OS());
        addType(new CLSRM2());
        addType(new CLSRM4());
        addType(new CLSRM6());
        addType(new CLSRM2OS());
        addType(new CLSRM4OS());
        addType(new CLSRM6OS());        
        addType(new ISStreakSRM2());
        addType(new ISStreakSRM4());
        addType(new ISStreakSRM6());
        addType(new ISStreakSRM2OS());
        addType(new ISStreakSRM4OS());
        addType(new ISStreakSRM6OS());
        addType(new CLStreakSRM2());
        addType(new CLStreakSRM4());
        addType(new CLStreakSRM6());
        addType(new CLStreakSRM2OS());
        addType(new CLStreakSRM4OS());
        addType(new CLStreakSRM6OS());
        //SRTs
        addType(new ISSRT2());
        addType(new ISSRT4());
        addType(new ISSRT6());
        addType(new ISSRT2OS());
        addType(new ISSRT4OS());
        addType(new ISSRT6OS());
        addType(new CLSRT2());
        addType(new CLSRT4());
        addType(new CLSRT6());
        addType(new CLSRT2OS());
        addType(new CLSRT4OS());
        addType(new CLSRT6OS());     
        //RLs
        addType(new ISRL10());
        addType(new ISRL15());
        addType(new ISRL20());
        //ATMs
        addType(new CLATM3());
        addType(new CLATM6());
        addType(new CLATM9());
        addType(new CLATM12());
        addType(new CLATM3OS());
        addType(new CLATM6OS());
        addType(new CLATM9OS());
        addType(new CLATM12OS());
        //MRMs
        addType(new ISMRM10());
        addType(new ISMRM20());
        addType(new ISMRM30());
        addType(new ISMRM40());
        //NARCs
        addType(new ISNarc());
        addType(new ISNarcOS());
        addType(new CLNarc());
        addType(new CLNarcOS());
        addType(new ISImprovedNarc());
        addType(new ISImprovedNarcOS());
        //AMSs
        addType(new ISAMS());
        addType(new ISLaserAMS());
        addType(new ISLaserAMSTHB());
        addType(new CLAMS());
        addType(new CLLaserAMS());
        //TAGs
        addType(new ISLightTAG());
        addType(new ISTAG());
        addType(new ISC3M());
        addType(new CLLightTAG());
        addType(new CLTAG());
        //Arty
        addType(new ISLongTom());
        addType(new ISThumper());
        addType(new ISSniper());
        addType(new ISArrowIV());
        addType(new CLLongTom());
        addType(new CLSniper());
        addType(new CLThumper());
        addType(new CLArrowIV());
        //LACs
        addType(new ISLAC2());
        addType(new ISLAC5());
        //MFUK weapons
        addType(new CLPlasmaRifle());
        addType(new CLRAC2());
        addType(new CLRAC5());
        addType(new CLRAC10());
        addType(new CLRAC20());
        //Infantry Attacks
        addType(new LegAttack());
        addType(new SwarmAttack());
        addType(new StopSwarmAttack());
        addType(new ISBAMineLauncher());
        //BA Weapons
        addType(new ISBAAutoGL());
        addType(new ISBADavidGaussRifle());
        addType(new ISBATsunamiHeavyGaussRifle());
        addType(new ISBASupportPPC());
        addType(new ISBASRM4());
        addType(new ISBASmallPulseLaser());
        addType(new ISBASmallLaser());
        addType(new CLBAERSmallLaser());
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
            } else if (this.hasFlag(MiscType.F_CLUB)
                    && (this.hasSubType(MiscType.S_HATCHET)
                    || this.hasSubType(MiscType.S_MACE_THB))) {
                int hatchetTons=(int) Math.ceil(entity.getWeight() / 15.0);
                cost=hatchetTons*5000;
            } else if (this.hasFlag(MiscType.F_CLUB)
                    && this.hasSubType(MiscType.S_SWORD)) {
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
