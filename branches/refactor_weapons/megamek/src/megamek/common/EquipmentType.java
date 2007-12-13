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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    public static final int     T_ARMOR_COMMERCIAL          = 10;

    public static final int     T_STRUCTURE_UNKNOWN         = -1;
    public static final int     T_STRUCTURE_STANDARD        = 0;
    public static final int     T_STRUCTURE_ENDO_STEEL      = 1;
    public static final int     T_STRUCTURE_ENDO_PROTOTYPE  = 2;
    public static final int     T_STRUCTURE_REINFORCED      = 3;
    public static final int     T_STRUCTURE_COMPOSITE       = 4;
    public static final int     T_STRUCTURE_INDUSTRIAL      = 5;

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

    private Vector<String>      namesVector = new Vector<String>();

    protected float     tonnage = 0;
    protected int       criticals = 0;

    protected boolean   explosive = false;
    protected boolean   hittable = true; // if false, reroll critical hits
    
    /** can the crits for this be spread over locations? */
    protected boolean   spreadable = false;
    protected int       toHitModifier = 0;
    protected int       techLevel = TechConstants.T_TECH_UNKNOWN;

    protected long       flags = 0;
    protected int       subType = 0;


    protected double     bv = 0; // battle value point system
    protected double    cost = 0; // The C-Bill cost of the item.

    /**
     * what modes can this equipment be in?
     */
    protected Vector<EquipmentMode> modes = null;
    
    /**
     * can modes be switched instantly, or at end of turn?
     */
    protected boolean instantModeSwitch = true;
    
    // static list of eq
    protected static Vector<EquipmentType> allTypes;
    protected static Hashtable<String,EquipmentType> lookupHash;

    /** Creates new EquipmentType */
    public EquipmentType() {

    }

    public void setFlags(long inF) {
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
    
    public long getFlags() {
        return flags;
    }
    
    public boolean hasFlag(long flag) {
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
        return 0;
    }

    /**
     * 
     * @return <code>Enumeration</code> of the <code>EquipmentMode</code> 
     * that this type of equipment can be in
     */
    public Enumeration<EquipmentMode> getModes() {
        if (modes != null) {
            return modes.elements();
        }
        
		return new Enumeration<EquipmentMode>() {
		    public boolean hasMoreElements() {
		        return false;
		    }
		    public EquipmentMode nextElement() {
		        return null;
		    }
		    
		};
    }

    /**
     * Sets the modes that this type of equipment can be in. By default the EquipmentType
     * doesn't have the modes, so don't try to call this method with null or empty argument. 
     * @param modes non null, non empty list of available mode names.
     */
    protected void setModes(String[] modes) {
        megamek.debug.Assert.assertTrue(modes != null && modes.length >= 0, 
                "List of modes must not be null or empty");
        if(modes != null) {
	        Vector<EquipmentMode> newModes = new Vector<EquipmentMode>(modes.length);
	        for (int i = 0 ,l = modes.length; i < l; i++) {
	            newModes.addElement(EquipmentMode.getMode(modes[i]));
	        }
	        this.modes = newModes;
        } else {
        	this.modes = new Vector<EquipmentMode>(0);
        }
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
     * @see #hasModes()
     */
    public EquipmentMode getMode(int modeNum) {
        megamek.debug.Assert.assertTrue(modes != null && modeNum >= 0 && modeNum < modes.size());
        return modes.elementAt(modeNum);
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
        return EquipmentType.lookupHash.get(key);
    }

    public Enumeration<String> getNames() {
        return namesVector.elements();
    }

    public static void initializeTypes() {
        if (null==EquipmentType.allTypes) {
            EquipmentType.allTypes = new Vector<EquipmentType>();
            EquipmentType.lookupHash = new Hashtable<String,EquipmentType>();
            
            // will I need any others?
            //WeaponType.initializeTypes();
            initializeWeaponTypes();
            AmmoType.initializeTypes();
            MiscType.initializeTypes();
        }
    }
    
    public static Enumeration<EquipmentType> getAllTypes() {
        if (null == EquipmentType.allTypes) {
            EquipmentType.initializeTypes();
        }
        return EquipmentType.allTypes.elements();
    }
    
    protected static void addType(EquipmentType type) {
        if (null == EquipmentType.allTypes) {
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
        addType(new ISSnubNosePPC());
        addType(new ISLightPPC());
        addType(new ISHeavyPPC());
        addType(new ISHERPPC());
        addType(new ISSupportPPC());
        addType(new CLSupportPPC());
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
        //LACs
        addType(new ISLAC2());
        addType(new ISLAC5());
        //Gausses
        addType(new ISGaussRifle());
        addType(new ISGaussRiflePrototype());
        addType(new CLGaussRifle());
        addType(new ISLGaussRifle());
        addType(new ISHGaussRifle());
        addType(new CLHAG20());
        addType(new CLHAG30());
        addType(new CLHAG40());
        addType(new CLAPGaussRifle());
        //MGs
        addType(new ISMG());
        addType(new ISLightMG());
        addType(new ISHeavyMG());
        addType(new ISMGA());
        addType(new ISLightMGA());
        addType(new ISHeavyMGA());
        addType(new CLMG());
        addType(new CLLightMG());
        addType(new CLHeavyMG());
        addType(new CLMGA());
        addType(new CLLightMGA());
        addType(new CLHeavyMGA());
        //LRMs
        addType(new ISLRM1());
        addType(new ISLRM2());
        addType(new ISLRM3());
        addType(new ISLRM4());
        addType(new ISLRM5());
        addType(new ISLRM10());
        addType(new ISLRM15());
        addType(new ISLRM20());
        addType(new ISLRM5OS());
        addType(new ISLRM10OS());
        addType(new ISLRM15OS());
        addType(new ISLRM20OS());
        addType(new CLLRM1());
        addType(new CLLRM2());
        addType(new CLLRM3());
        addType(new CLLRM4());
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
        addType(new ISSRM1());
        addType(new ISSRM2());
        addType(new ISSRM2());
        addType(new ISSRM4());
        addType(new ISSRM5());
        addType(new ISSRM6());
        addType(new ISSRM2OS());
        addType(new ISSRM4OS());
        addType(new ISSRM6OS());
        addType(new CLSRM1());
        addType(new CLSRM2());
        addType(new CLSRM3());
        addType(new CLSRM4());
        addType(new CLSRM5());
        addType(new CLSRM6());
        addType(new CLSRM2OS());
        addType(new CLSRM4OS());
        addType(new CLSRM6OS());
        addType(new CLAdvancedSRM1());
        addType(new CLAdvancedSRM2());
        addType(new CLAdvancedSRM3());
        addType(new CLAdvancedSRM4());
        addType(new CLAdvancedSRM5());
        addType(new CLAdvancedSRM6());
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
        addType(new ISRL1());
        addType(new ISRL2());
        addType(new ISRL3());
        addType(new ISRL4());
        addType(new ISRL5());
        addType(new ISRL10());
        addType(new ISRL15());
        addType(new ISRL20());
        //ATMs
        addType(new CLATM3());
        addType(new CLATM6());
        addType(new CLATM9());
        addType(new CLATM12());
        //MRMs
        addType(new ISMRM1());
        addType(new ISMRM2());
        addType(new ISMRM3());
        addType(new ISMRM4());
        addType(new ISMRM5());
        addType(new ISMRM10());
        addType(new ISMRM20());
        addType(new ISMRM30());
        addType(new ISMRM40());
        addType(new ISMRM10OS());
        addType(new ISMRM20OS());
        addType(new ISMRM30OS());
        addType(new ISMRM40OS());
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
        //MMLs
        addType(new ISMML3());
        addType(new ISMML5());
        addType(new ISMML7());
        addType(new ISMML9());
        //Arty
        addType(new ISLongTom());
        addType(new ISThumper());
        addType(new ISSniper());
        addType(new ISArrowIV());
        addType(new CLLongTom());
        addType(new CLSniper());
        addType(new CLThumper());
        addType(new CLArrowIV());
        //MFUK weapons
        addType(new CLPlasmaRifle());
        addType(new CLRAC2());
        addType(new CLRAC5());
        addType(new CLRAC10());
        addType(new CLRAC20());
        //misc lvl3 stuff
        addType(new ISRailGun());
        //MapPack Solaris VII
        addType(new ISMagshotGaussRifle());
        addType(new CLMagshotGaussRifle());
        //Thunderbolts
        addType(new ISThunderBolt5());
        addType(new ISThunderBolt10());
        addType(new ISThunderBolt15());
        addType(new ISThunderBolt20());
        //Infantry Attacks
        addType(new LegAttack());
        addType(new SwarmAttack());
        addType(new StopSwarmAttack());
        //Protomech Weapons
        addType(new CLPROSRM1());
        addType(new CLPROSRM2());
        addType(new CLPROSRM3());
        addType(new CLPROSRM4());
        addType(new CLPROSRM5());
        addType(new CLPROSRM6());
        addType(new CLPROSRT1());
        addType(new CLPROSRT2());
        addType(new CLPROSRT3());
        addType(new CLPROSRT4());
        addType(new CLPROSRT5());
        addType(new CLPROSRT6());
        addType(new CLPROStreakSRM1());
        addType(new CLPROStreakSRM2());
        addType(new CLPROStreakSRM3());
        addType(new CLPROStreakSRM4());
        addType(new CLPROStreakSRM5());
        addType(new CLPROStreakSRM6());
        addType(new CLPROLRM1());
        addType(new CLPROLRM2());
        addType(new CLPROLRM3());
        addType(new CLPROLRM4());
        addType(new CLPROLRM5());
        addType(new CLPROLRM6());
        addType(new CLPROLRM7());
        addType(new CLPROLRM8());
        addType(new CLPROLRM9());
        addType(new CLPROLRM10());
        addType(new CLPROLRM11());
        addType(new CLPROLRM12());
        addType(new CLPROLRM13());
        addType(new CLPROLRM14());
        addType(new CLPROLRM15());
        addType(new CLPROLRM16());
        addType(new CLPROLRM17());
        addType(new CLPROLRM18());
        addType(new CLPROLRM19());
        addType(new CLPROLRM20());
        addType(new CLPROLRT1());
        addType(new CLPROLRT2());
        addType(new CLPROLRT3());
        addType(new CLPROLRT4());
        addType(new CLPROLRT5());
        addType(new CLPROLRT6());
        addType(new CLPROLRT7());
        addType(new CLPROLRT8());
        addType(new CLPROLRT9());
        addType(new CLPROLRT10());
        addType(new CLPROLRT11());
        addType(new CLPROLRT12());
        addType(new CLPROLRT13());
        addType(new CLPROLRT14());
        addType(new CLPROLRT15());
        addType(new CLPROLRT16());
        addType(new CLPROLRT17());
        addType(new CLPROLRT18());
        addType(new CLPROLRT19());
        addType(new CLPROLRT20());
        
        //Infantry Weapons
        addType(new InfantryRifleWeapon());
        addType(new InfantrySRMWeapon());
        addType(new InfantryInfernoSRMWeapon());
        addType(new InfantryLRMWeapon());
        addType(new InfantryLaserWeapon());
        addType(new InfantryFlamerWeapon());
        addType(new InfantryMGWeapon());
        addType(new ISFireExtinguisher());
        addType(new CLFireExtinguisher());
        
        //plasma weapons
        addType(new ISPlasmaRifle());
        addType(new CLPlasmaCannon());
        
        //BA weapons
        addType(new CLSmallLaser());
        addType(new ISLightRecoillessRifle());
        addType(new ISMediumRecoillessRifle());
        addType(new ISHeavyRecoillessRifle());
        addType(new CLLightRecoillessRifle());
        addType(new CLMediumRecoillessRifle());
        addType(new CLHeavyRecoillessRifle());
        addType(new CLBearhunterSuperheavyAC());
        addType(new ISKingDavidLightGaussRifle());
        addType(new ISDavidLightGaussRifle());
        addType(new ISFiredrakeNeedler());
        addType(new ISBAPlasmaRifle());
        addType(new ISHeavyMortar());
        addType(new ISLightMortar());
        addType(new ISAutoGrenadeLauncher());
        addType(new ISCompactNarc());
        addType(new ISMineLauncher());
        
    }    

    public static int getArmorType(String inType) {
        EquipmentType et = EquipmentType.get(inType);
        if (et != null) {
            for (int x=0; x<armorNames.length; x++) {
                if (armorNames[x].equals(et.getInternalName()))
                    return x;
            }
        }
        return T_ARMOR_UNKNOWN;
    }

    public static  String getArmorTypeName(int armorType) {
        if ((armorType < 0) || (armorType >= armorNames.length))
            return "UNKNOWN";
        return armorNames[armorType];
    }

    public static int getStructureType(String inType) {
        EquipmentType et = EquipmentType.get(inType);
        if (et != null) {
            for (int x=0; x<structureNames.length; x++) {
                if (structureNames[x].equals(et.getInternalName()))
                    return x;
            }
        }
        return T_STRUCTURE_UNKNOWN;
    }

    public static  String getStructureTypeName(int structureType) {
        if ((structureType < 0) || (structureType >= structureNames.length))
            return "UNKNOWN";
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
    
    /**
     * stuff like hatchets, which depend on an unknown quality (usually tonnage of the unit.)
     * entity is whatever has this item
     */
    public int resolveVariableCost(Entity entity) {
        int cost=0;
        if(this instanceof MiscType) {
            if(this.hasFlag(MiscType.F_MASC)) {
                if (hasSubType(MiscType.S_SUPERCHARGER)) {
                    Engine e = entity.getEngine();
                    if(e == null) {
                    	cost = 0;
                    } else {
                    	cost = e.getRating() * 10000;
                    }
                } else {
                    int mascTonnage=0;
                    if (this.getInternalName().equals("ISMASC")) {
                        mascTonnage = Math.round(entity.getWeight() / 20.0f);
                    } else if (this.getInternalName().equals("CLMASC")) {
                        mascTonnage = Math.round(entity.getWeight() / 25.0f);
                    }
                    cost=entity.getEngine().getRating() * mascTonnage * 1000;
                }
            } else if(this.hasFlag(MiscType.F_TARGCOMP)) {
                int tCompTons=0;
                float fTons = 0.0f;
                for(Mounted mo : entity.getWeaponList()) {
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
            } else if (this.hasFlag(MiscType.F_CLUB)
                    && this.hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
                int bladeTons=(int) Math.ceil(0.5f+Math.ceil(entity.getWeight() / 20.0));
                cost=(1+bladeTons)*10000;
            }
        } else {
            if(cost==0) {
                //if we don't know what it is...
                System.out.println("I don't know how much " + this.name + " costs.");
            }
        }
        return cost;
    }

    public boolean equals(EquipmentType e) {
        if (e != null && this.internalName.equals(e.internalName))
            return true;
        return false;
    }
    
    public static void writeEquipmentDatabase(File f) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(f));
            w.write("Megamek Equipment Database");
            w.newLine();
            w.write("This file can be regenerated with java -jar MegaMek.jar -eqdb ");
            w.write(f.toString());
            w.newLine();
            w.write("Type,Tech,Rules,Name,Aliases");
            w.newLine();
            for(Enumeration<EquipmentType> e=EquipmentType.getAllTypes();e.hasMoreElements();) {
                EquipmentType type = e.nextElement();
                if(type instanceof AmmoType) {
                    w.write("A,");
                }
                else if(type instanceof WeaponType) {
                    w.write("W,");
                }
                else {
                    w.write("M,");
                }
                switch(type.getTechLevel()) {
                case TechConstants.T_IS_LEVEL_2:
                case TechConstants.T_IS_LEVEL_2_ALL:
                    w.write("IS,2,");
                    break;
                case TechConstants.T_IS_LEVEL_3:
                    w.write("IS,3,");
                    break;
                case TechConstants.T_CLAN_LEVEL_2:
                    w.write("Clan,2,");
                    break;
                case TechConstants.T_CLAN_LEVEL_3:
                    w.write("Clan,3,");
                    break;
                default:
                    w.write("Any,1,");
                    break;
                }
                for(Enumeration<String> names = type.getNames();names.hasMoreElements();) {
                    String name = names.nextElement();
                    w.write(name + ",");
                }
                w.newLine();
            }
            w.flush();
            w.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
