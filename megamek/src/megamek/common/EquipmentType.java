/*
 * MegaMek - Copyright (C) 2002,2003,2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

/*
 * EquipmentType.java
 *
 * Created on April 1, 2002, 1:35 PM
 */

package megamek.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import megamek.common.options.GameOptions;
import megamek.common.weapons.autocannons.HVACWeapon;
import megamek.common.weapons.defensivepods.BPodWeapon;
import megamek.common.weapons.defensivepods.MPodWeapon;
import megamek.common.weapons.ppc.PPCWeapon;
import megamek.server.Server;

/**
 * Represents any type of equipment mounted on a mechs, excluding systems and
 * actuators.
 *
 * @author Ben
 * @version
 */
public class EquipmentType implements ITechnology {
    public static final double TONNAGE_VARIABLE = Float.MIN_VALUE;
    public static final int CRITICALS_VARIABLE = Integer.MIN_VALUE;
    public static final int BV_VARIABLE = Integer.MIN_VALUE;
    public static final int COST_VARIABLE = Integer.MIN_VALUE;

    public static final int T_ARMOR_UNKNOWN = -1;
    public static final int T_ARMOR_STANDARD = 0;
    public static final int T_ARMOR_FERRO_FIBROUS = 1;
    public static final int T_ARMOR_REACTIVE = 2;
    public static final int T_ARMOR_REFLECTIVE = 3;
    public static final int T_ARMOR_HARDENED = 4;
    public static final int T_ARMOR_LIGHT_FERRO = 5;
    public static final int T_ARMOR_HEAVY_FERRO = 6;
    public static final int T_ARMOR_PATCHWORK = 7;
    public static final int T_ARMOR_STEALTH = 8;
    public static final int T_ARMOR_FERRO_FIBROUS_PROTO = 9;
    public static final int T_ARMOR_COMMERCIAL = 10;
    public static final int T_ARMOR_LC_FERRO_CARBIDE = 11;  //Large Craft Only
    public static final int T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE = 12; //Large Craft Only
    public static final int T_ARMOR_LC_FERRO_IMP = 13; //Large Craft Only
    public static final int T_ARMOR_INDUSTRIAL = 14;
    public static final int T_ARMOR_HEAVY_INDUSTRIAL = 15;
    public static final int T_ARMOR_FERRO_LAMELLOR = 16;
    public static final int T_ARMOR_PRIMITIVE = 17;
    public static final int T_ARMOR_EDP = 18;
    public static final int T_ARMOR_ALUM = 19;
    public static final int T_ARMOR_HEAVY_ALUM = 20;
    public static final int T_ARMOR_LIGHT_ALUM = 21;
    public static final int T_ARMOR_STEALTH_VEHICLE = 22;
    public static final int T_ARMOR_ANTI_PENETRATIVE_ABLATION = 23;
    public static final int T_ARMOR_HEAT_DISSIPATING = 24;
    public static final int T_ARMOR_IMPACT_RESISTANT = 25;
    public static final int T_ARMOR_BALLISTIC_REINFORCED = 26;
    public static final int T_ARMOR_FERRO_ALUM_PROTO = 27;
    public final static int T_ARMOR_BA_STANDARD = 28;
    public final static int T_ARMOR_BA_STANDARD_PROTOTYPE = 29;
    public final static int T_ARMOR_BA_STANDARD_ADVANCED = 30;
    public final static int T_ARMOR_BA_STEALTH_BASIC = 31;
    public final static int T_ARMOR_BA_STEALTH = 32;
    public final static int T_ARMOR_BA_STEALTH_IMP = 33;
    public final static int T_ARMOR_BA_STEALTH_PROTOTYPE = 34;
    public final static int T_ARMOR_BA_FIRE_RESIST = 35;
    public final static int T_ARMOR_BA_MIMETIC = 36;
    public final static int T_ARMOR_BA_REFLECTIVE = 37;
    public final static int T_ARMOR_BA_REACTIVE = 38;
    public static final int T_ARMOR_PRIMITIVE_FIGHTER = 39;
    public static final int T_ARMOR_PRIMITIVE_AERO = 40;
    public static final int T_ARMOR_AEROSPACE = 41;

    public static final int T_STRUCTURE_UNKNOWN = -1;
    public static final int T_STRUCTURE_STANDARD = 0;
    public static final int T_STRUCTURE_INDUSTRIAL = 1;
    public static final int T_STRUCTURE_ENDO_STEEL = 2;
    public static final int T_STRUCTURE_ENDO_PROTOTYPE = 3;
    public static final int T_STRUCTURE_REINFORCED = 4;
    public static final int T_STRUCTURE_COMPOSITE = 5;
    public static final int T_STRUCTURE_ENDO_COMPOSITE = 6;

    public static final String[] armorNames = { "Standard", "Ferro-Fibrous",
            "Reactive", "Reflective", "Hardened", "Light Ferro-Fibrous",
            "Heavy Ferro-Fibrous", "Patchwork", "Stealth",
            "Ferro-Fibrous Prototype", "Commercial", "Ferro-Carbide",
            "Lamellor Ferro-Carbide", "Improved Ferro-Aluminum",
            /* extra space at the end on purpose */ "Industrial ",
            "Heavy Industrial", "Ferro-Lamellor", "Primitive",
            "Electric Discharge ProtoMech", "Ferro-Aluminum",
            "Heavy Ferro-Aluminum", "Light Ferro-Aluminum",
            "Vehicular Stealth", "Anti-Penetrative Ablation",
            "Heat-Dissipating", "Impact-Resistant", "Ballistic-Reinforced",
            "Prototype Ferro-Aluminum", "BA Standard (Basic)",
            "BA Standard (Prototype)", "BA Advanced", "BA Stealth (Basic)",
            "BA Stealth (Standard)", "BA Stealth (Improved)", "BA Stealth (Prototype)",
            "BA Fire Resistant", "BA Mimetic", "BA Laser Reflective (Reflec/Glazed)", "BA Reactive (Blazer)",
            "Primitive Fighter", "Primitive Aerospace", "Standard Aerospace"};


    public static final String[] structureNames = { "Standard", "Industrial",
            "Endo Steel", "Endo Steel Prototype", "Reinforced", "Composite",
            "Endo-Composite" };

    // Assume for now that prototype is not more expensive
    public static final double[] structureCosts = { 400, 300, 1600, 4800, 6400,
            1600, 3200 };

    // Assume for now that prototype is not more expensive
    public static final double[] armorCosts = {
            10000, 20000, 30000, 30000, 15000, 15000, 25000, /* patchwork */0, 50000, 60000,
            3000, 75000, 100000, 50000, 5000, 10000, 35000, 5000, 10000, 20000,
            25000, 15000, 50000, 15000, 25000, 20000, 25000, 60000, 10000, 10000,
            12500, 12000, 15000, 20000, 50000, 10000, 15000, 37000, 37000, 5000,
            5000, 10000};

    public static final double[] armorPointMultipliers = {
            1, 1.12, 1, 1, 0.5, 1.06, 1.24, 1, 1, 1.12,
            1.5, 1.52, 1.72, 1.32, 0.67, 1.0, 0.875, 0.67, 1, 1.12,
            1.24, 1.06, 1, 0.75, 0.625, 0.875, 0.75, 1.12, 0.8, 1.6,
            0.64, 0.48, 0.96, 0.96, 1.6, 0.48, 0.8, 0.88, 0.96, 0.67,
            0.66, 1 };

    public static final double POINT_MULTIPLIER_UNKNOWN = 1;
    public static final double POINT_MULTIPLIER_CLAN_FF = 1.2;
    public static final double POINT_ADDITION_CLAN_FF = 0.08;

    protected String name = null;

    // Short name for RS Printing
    protected String shortName = "";

    protected String internalName = null;

    private Vector<String> namesVector = new Vector<String>();

    protected double tonnage = 0;
    protected int criticals = 0;
    protected int tankslots = 1;

    protected boolean explosive = false;
    protected boolean hittable = true; // if false, reroll critical hits

    /** can the crits for this be spread over locations? */
    protected boolean spreadable = false;
    protected int toHitModifier = 0;

//    protected Map<Integer,Integer> techLevel = new HashMap<>();
    
    protected TechAdvancement techAdvancement = new TechAdvancement();

    protected BigInteger flags = BigInteger.valueOf(0);

    protected long subType = 0;

    protected double bv = 0; // battle value point system
    protected double cost = 0; // The C-Bill cost of the item.

    // For equipment that cannot be pod-mounted on an omni unit
    protected boolean omniFixedOnly = false;

    /**
     * what modes can this equipment be in?
     */
    protected Vector<EquipmentMode> modes = null;

    /**
     * can modes be switched instantly, or at end of turn?
     */
    protected boolean instantModeSwitch = true;
    /**
     * sometimes some modes can be switched at the end of turn and some
     * instantly In that case, the specific end of turn mode names can be added
     * here
     */
    public Vector<String> endTurnModes = new Vector<String>();

    // static list of eq
    protected static Vector<EquipmentType> allTypes;
    protected static Hashtable<String, EquipmentType> lookupHash;
    
    /**
    * Keeps track of page numbers for rules references.
    */
    public String rulesRefs = "";

    /** Creates new EquipmentType */
    public EquipmentType() {
        // default constructor
    }

    public void setFlags(BigInteger inF) {
        flags = inF;
    }

    public long getSubType() {
        return subType;
    }
    
    public void setSubType(int newFlags) {
        subType = newFlags;
    }

    public void addSubType(int newFlag) {
        subType |= newFlag;
    }

    public boolean hasSubType(long testFlag) {
        return (subType & testFlag) != 0;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        String result = EquipmentMessages.getString("EquipmentType." + name);
        if (result != null) {
            return result;
        }
        return name;
    }

    public String getInternalName() {
        return internalName;
    }
    
    public String getRulesRefs() {
        return rulesRefs;
    }

    /**
     * @deprecated The old tech progression system has been replaced by the TechAdvancement class.
     */
    @Deprecated
    public Map<Integer, Integer> getTechLevels() {
        Map<Integer,Integer> techLevel = new HashMap<>();
        if (isUnofficial()) {
            if (techAdvancement.getTechBase() == TECH_BASE_CLAN) {
                techLevel.put(techAdvancement.getIntroductionDate(true), TechConstants.T_CLAN_UNOFFICIAL);
            } else {
                techLevel.put(techAdvancement.getIntroductionDate(true), TechConstants.T_IS_UNOFFICIAL);
            }
            return techLevel;
        }
        if (techAdvancement.getPrototypeDate(true) > 0) {
            techLevel.put(techAdvancement.getPrototypeDate(true), TechConstants.T_CLAN_EXPERIMENTAL);
        }
        if (techAdvancement.getPrototypeDate(false) > 0) {
            techLevel.put(techAdvancement.getPrototypeDate(false), TechConstants.T_IS_EXPERIMENTAL);
        }
        if (techAdvancement.getProductionDate(true) > 0) {
            techLevel.put(techAdvancement.getProductionDate(true), TechConstants.T_CLAN_ADVANCED);
        }
        if (techAdvancement.getProductionDate(false) > 0) {
            techLevel.put(techAdvancement.getProductionDate(false), TechConstants.T_IS_ADVANCED);
        }
        if (techAdvancement.getTechBase() == TECH_BASE_ALL
                && techAdvancement.getCommonDate() > 0) {
            techLevel.put(techAdvancement.getCommonDate(true), TechConstants.T_TW_ALL);
        } else if (techAdvancement.getCommonDate(true) > 0) {
            techLevel.put(techAdvancement.getCommonDate(true), TechConstants.T_CLAN_TW);
        } else if (techAdvancement.getCommonDate(false) > 0) {
            techLevel.put(techAdvancement.getCommonDate(false),
                    isIntroLevel()? TechConstants.T_INTRO_BOXSET : TechConstants.T_IS_TW_NON_BOX);
        }
        return techLevel;
    }

    public int getTechLevel(int date) {
        return techAdvancement.getTechLevel(date);
    }
    
    public int getTechLevel(int date, boolean clan) {
        return techAdvancement.getTechLevel(date, clan);
    }
    
    public SimpleTechLevel getStaticTechLevel() {
        if (null != techAdvancement.getStaticTechLevel()) {
            return techAdvancement.getStaticTechLevel();
        } else {
            return techAdvancement.guessStaticTechLevel(rulesRefs);
        }
    }

    public double getTonnage(Entity entity) {
        return getTonnage(entity, Entity.LOC_NONE);
    }

    public double getTonnage(Entity entity, int location) {
        return tonnage;
    }

    public void setTonnage(double tonnage) {
        this.tonnage = tonnage;
    }

    public int getCriticals(Entity entity) {
        return criticals;
    }

    public int getTankslots(Entity entity) {
        return tankslots;
    }

    public boolean isExplosive(Mounted mounted) {
        return isExplosive(mounted, false);
    }

    public boolean isExplosive(Mounted mounted, boolean ignoreCharge) {
        if(null == mounted) {
            return explosive;
        }
        
        // Special case: discharged M- and B-pods shouldn't explode.
        if (((this instanceof MPodWeapon) || (this instanceof BPodWeapon))
                && ((mounted.getLinked() == null) || (mounted.getLinked()
                        .getUsableShotsLeft() == 0))) {
            return false;
        }


        // special case: RISC laser pulse module are only explosive when the
        // laser they're linked to is working
        if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
            if ((mounted.getLinked() == null) || mounted.getLinked().isInoperable()) {
                return false;
            }
        }

        // special-case. RACs only explode when jammed
        if ((mounted.getType() instanceof WeaponType)
                && (((WeaponType) mounted.getType()).getAmmoType() == AmmoType.T_AC_ROTARY)) {
            if (!mounted.isJammed()) {
                return false;
            }
        }

        // special case. ACs only explode when firing incendiary ammo
        if ((mounted.getType() instanceof WeaponType)
                && ((((WeaponType) mounted.getType()).getAmmoType() == AmmoType.T_AC)
                        || (((WeaponType) mounted.getType()).getAmmoType() == AmmoType.T_LAC)
                        || (((WeaponType) mounted.getType()).getAmmoType() == AmmoType.T_AC_IMP)
                        || (((WeaponType) mounted.getType()).getAmmoType() == AmmoType.T_PAC))) {
            if (!mounted.isUsedThisRound()) {
                return false;
            }
            Mounted ammo = mounted.getLinked();
            if ((ammo == null) || !(ammo.getType() instanceof AmmoType)
                    || (((AmmoType) ammo.getType()).getMunitionType() != AmmoType.M_INCENDIARY_AC)) {
                return false;
            }

            WeaponType wtype = (WeaponType) mounted.getType();
            if ((wtype.getAmmoType() == AmmoType.T_LRM)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO_COMBO)) {
                return false;
            }
        }

        // special case. HVACs only explode when there's ammo left
        if (mounted.getType() instanceof HVACWeapon) {
            if ((mounted.getEntity() == null)
                    || (mounted.getLinked() == null)
                    || (mounted.getEntity().getTotalAmmoOfType(
                            mounted.getLinked().getType()) == 0)) {
                return false;
            }
        }

        // special case. Blue Shield Particle Field Damper only explodes when
        // switched on
        if ((mounted.getType() instanceof MiscType)
                && (mounted.getType().hasFlag(MiscType.F_BLUE_SHIELD) && mounted
                        .curMode().equals("Off"))) {
            return false;
        }

        // special case. PPC with Capacitor only explodes when charged
        if (ignoreCharge) {
            // for BV purposes, we need to ignore the chargedness and check only
            // if there's a capacitor
            if ((mounted.getType() instanceof PPCWeapon)
                    && (mounted.getLinkedBy() != null)) {
                return true;
            }
            if ((mounted.getType() instanceof MiscType)
                    && mounted.getType().hasFlag(MiscType.F_PPC_CAPACITOR)
                    && (mounted.getLinked() != null)) {
                return true;
            }

        }
        if ((mounted.getType() instanceof MiscType)
                && mounted.getType().hasFlag(MiscType.F_PPC_CAPACITOR)
                && !mounted.curMode().equals("Charge")) {
            return false;
        }
        if ((mounted.getType() instanceof PPCWeapon)
                && (mounted.hasChargedCapacitor() == 0)) {
            return false;
        }

        // If we're here, then none of the special cases apply and we should
        // just return our own explosive status.
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

    public BigInteger getFlags() {
        return flags;
    }

    public boolean hasFlag(BigInteger flag) {
        return !(flags.and(flag)).equals(BigInteger.valueOf(0));
    }

    public double getBV(Entity entity) {
        return bv;
    }

    /**
     * @return - whether the equipment must be considered part of the base chassis when
     *           mounted on an omni unit
     */
    public boolean isOmniFixedOnly() {
        return omniFixedOnly;
    }

    /**
     * @return <code>true</code> if this type of equipment has set of modes that
     *         it can be in.
     */
    public boolean hasModes() {
        return (modes != null) && (!modes.isEmpty());
    }

    /**
     * @return the number of modes that this type of equipment can be in or
     *         <code>0</code> if it doesn't have modes.
     */
    public int getModesCount() {
        if (modes != null) {
            return modes.size();
        }
        return 0;
    }

    /**
     * @return <code>Enumeration</code> of the <code>EquipmentMode</code> that
     *         this type of equipment can be in
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
     * Sets the modes that this type of equipment can be in. By default the
     * EquipmentType doesn't have the modes, so don't try to call this method
     * with null or empty argument.
     *
     * @param modes
     *            non null, non empty list of available mode names.
     */
    protected void setModes(String[] modes) {
        assert ((modes != null) && (modes.length >= 0)) : "List of modes must not be null or empty";
        Vector<EquipmentMode> newModes = new Vector<EquipmentMode>(modes.length);
        for (String mode : modes) {
            newModes.addElement(EquipmentMode.getMode(mode));
        }
        this.modes = newModes;
    }

    /**
     * Remove a specific mode from the list of modes.
     *
     * @param mode
     * @return
     */
    public boolean removeMode(String mode) {
        if (modes != null) {
            return modes.remove(EquipmentMode.getMode(mode));
        } else {
            return false;
        }
    }
    
    /**
     * Add a mode to the Equipment
     *
     * @param mode The mode to be added
     * @return true if the mode was added; false if modes was null or the mode was already present
     * @author Simon (Juliez)
     */
    public boolean addMode(String mode) {
        if (modes == null) {
            modes = new Vector<EquipmentMode>();
        }
        if (!modes.contains(EquipmentMode.getMode(mode))) {
            return modes.add(EquipmentMode.getMode(mode));
        } else {
            return false;
        }
    }

    /**
     * Clears the modes that this type of equipment can be in. This is useful
     * where a subtype such as Streak LRMs has no modes, but the supertype of
     * that type such as standard LRMs has modes that do not apply to the
     * subtype
     */
    protected void clearModes() {
        modes = null;
    }

    public void addEndTurnMode(String mode) {
        endTurnModes.add(mode);
    }

    /**
     * Some equipment types might have both instant and next turn mode
     * switching. This method checks for end of turn modes that are kept in a
     * vector of names. It is used by the {@link Mounted#setMode(int)} method to
     * distinguish instant and end of turn switching.
     *
     * @param mode
     *            - the <code>String</code> of the mode name involved in the
     *            switch
     * @return true if the mode name is found in the next turn mode vector
     */
    public boolean isNextTurnModeSwitch(String mode) {
        for (String modeName : endTurnModes) {
            if (modeName.equals(mode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * Returns the mode number <code>modeNum</code> from the list of modes
     * available for this type of equipment. Modes are numbered from
     * <code>0<code> to
     * <code>getModesCount()-1</code>
     * <p>
     * Fails if this type of the equipment doesn't have modes, or given mode is
     * out of the valid range.
     *
     * @param modeNum
     * @return mode number <code>modeNum</code> from the list of modes available
     *         for this type of equipment.
     * @see #hasModes()
     */
    public EquipmentMode getMode(int modeNum) {
        assert ((modes != null) && (modeNum >= 0) && (modeNum < modes.size())) : "Invalid Mode";
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
        EquipmentType.lookupHash.put(s.toLowerCase(), this); // static variable
        namesVector.addElement(s); // member variable
    }

    public static EquipmentType get(String key) {
        if (null == EquipmentType.lookupHash) {
            EquipmentType.initializeTypes();
        }
        return EquipmentType.lookupHash.get(key.toLowerCase());
    }

    public Enumeration<String> getNames() {
        return namesVector.elements();
    }

    public static void initializeTypes() {
        if (null == EquipmentType.allTypes) {
            EquipmentType.allTypes = new Vector<EquipmentType>();
            EquipmentType.lookupHash = new Hashtable<String, EquipmentType>();

            WeaponType.initializeTypes();
            AmmoType.initializeTypes();
            MiscType.initializeTypes();
            BombType.initializeTypes();
            for (EquipmentType et : allTypes) {
                if (et.getTechAdvancement().getStaticTechLevel() == null) {
                    et.getTechAdvancement().setStaticTechLevel(et.getTechAdvancement()
                            .guessStaticTechLevel(et.getRulesRefs()));
                }
            }
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

    public static int getArmorType(EquipmentType et) {
        if (null == et) {
            return T_ARMOR_UNKNOWN;
        }
        for (int x = 0; x < armorNames.length; x++) {
            // Some armor names (Industrial), have a space in the name, so trim
            if (armorNames[x].trim().equals(et.getName().trim())) {
                return x;
            }
        }
        return T_ARMOR_UNKNOWN;
    }

    public static String getArmorTypeName(int armorType) {
        if ((armorType < 0) || (armorType >= armorNames.length)) {
            return "UNKNOWN";
        }
        return armorNames[armorType];
    }

    public static String getArmorTypeName(int armorType, boolean clan) {
        if ((armorType < 0) || (armorType >= armorNames.length)) {
            return "UNKNOWN";
        }
        return clan ? "Clan " + armorNames[armorType] : "IS "
                + armorNames[armorType];
    }
    
    public static int getStructureType(EquipmentType et) {
        if (et == null) {
            return T_STRUCTURE_UNKNOWN;
        }
        for (int x = 0; x < structureNames.length; x++) {
            if (structureNames[x].equals(et.getName())) {
                return x;
            }
        }
        return T_STRUCTURE_UNKNOWN;
    }

    public static String getStructureTypeName(int structureType) {
        if ((structureType < 0) || (structureType >= structureNames.length)) {
            return "UNKNOWN";
        }
        return structureNames[structureType];
    }

    public static String getStructureTypeName(int structureType, boolean clan) {
        if ((structureType < 0) || (structureType >= structureNames.length)) {
            return "UNKNOWN";
        }
        return clan ? "Clan " + structureNames[structureType] : "IS "
                + structureNames[structureType];
    }

    public static String getBaArmorTypeName(int armorType) {
        return getArmorTypeName(armorType);
    }

    public static String getBaArmorTypeName(int armorType, boolean clan) {
        return getArmorTypeName(armorType, clan);
    }

    public static double getBaArmorWeightPerPoint(int type, boolean isClan) {
        switch (type) {
        case T_ARMOR_BA_STANDARD_PROTOTYPE:
            return 0.1;
        case T_ARMOR_BA_STANDARD_ADVANCED:
            return 0.04;
        case T_ARMOR_BA_STEALTH:
            if (isClan) {
                return 0.035;
            }
            return 0.06;
        case T_ARMOR_BA_STEALTH_BASIC:
            if (isClan) {
                return 0.03;
            }
            return 0.055;
        case T_ARMOR_BA_STEALTH_IMP:
            if (isClan) {
                return 0.035;
            }
            return 0.06;
        case T_ARMOR_BA_STEALTH_PROTOTYPE:
            return 0.1;
        case T_ARMOR_BA_FIRE_RESIST:
            return 0.03;
        case T_ARMOR_BA_MIMETIC:
            return 0.05;
        case T_ARMOR_BA_REFLECTIVE:
            if (isClan) {
                return 0.03;
            } else {
                return 0.055;
            }
        case T_ARMOR_BA_REACTIVE:
            if (isClan) {
                return 0.035;
            } else {
                return 0.06;
            }
        case T_ARMOR_BA_STANDARD:
        default:
            if (isClan) {
                return 0.025;
            }
            return 0.05;
        }
    }
    
    /* Armor and structure are stored as integers and standard uses a generic MiscType that
     * does not have its own TechAdvancement.
     */

    protected final static TechAdvancement TA_STANDARD_ARMOR = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2460, 2470, 2470).setApproximate(true, false, false)
            .setTechRating(RATING_D).setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
            .setStaticTechLevel(SimpleTechLevel.INTRO);
    protected final static TechAdvancement TA_STANDARD_STRUCTURE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2430, 2439, 2505).setApproximate(true, false, false).setIntroLevel(true)
            .setTechRating(RATING_D).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.INTRO);
    protected final static TechAdvancement TA_NONE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE).setTechRating(RATING_A)
            .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.INTRO);

    /**
     * Tech advancement for armor based on the armor type index and tech base
     * 
     * @param at   The armor type constant
     * @param clan The armor tech base
     * @return     The tech advancement for the armor
     */
    public static TechAdvancement getArmorTechAdvancement(int at, boolean clan) {
        if (at == T_ARMOR_STANDARD) {
            return TA_STANDARD_ARMOR;
        }
        String armorName = EquipmentType.getArmorTypeName(at, clan);
        EquipmentType armor = EquipmentType.get(armorName);
        if (armor != null) {
            return armor.getTechAdvancement();
        }
        return TA_NONE;
    }

    public static TechAdvancement getStructureTechAdvancement(int at, boolean clan) {
        if (at == T_STRUCTURE_STANDARD) {
            return TA_STANDARD_STRUCTURE;
        }
        String structureName = EquipmentType.getStructureTypeName(at, clan);
        EquipmentType structure = EquipmentType.get(structureName);
        if (structure != null) {
            return structure.getTechAdvancement();
        }
        return TA_NONE;
    }

    /**
     * @return The C-Bill cost of the piece of equipment.
     */
    public double getCost(Entity entity, boolean armored, int loc) {
        return cost;
    }

    public double getRawCost() {
        return cost;
    }

    public TechAdvancement getTechAdvancement() {
        return techAdvancement;
    }

    public int getTechRating() {
        return techAdvancement.getTechRating();
    }

    /**
     * @deprecated Use {@link #calcEraAvailability(int,boolean) calcEraAvailability to get availability
     *      for IS/Clan in a given year, or {@link #getBaseAvailability(int) getBaseAvailability}
     *      to get the base code for the era type, or getBaseEraAvailability to get the base code.
     */
    @Deprecated
    public int getAvailability(int era) {
        return calcEraAvailability(era);
    }

    /**
     * @deprecated Use {@link #getYearAvailabilityName(int,boolean) getYearAvailabilityName}
     *      to get availability for IS or Clan in a specific year,
     *      or {@link #getEraAvailabilityName(int) getEraAvailabilityName to get code(s) for the era.
     */
    @Deprecated
    public String getAvailabilityName(int era) {
        return getEraAvailabilityName(era);
    }
    
    public boolean isClan() {
        return techAdvancement.getTechBase() == TECH_BASE_CLAN;
    }
    
    public boolean isMixedTech() {
        return techAdvancement.getTechBase() == TECH_BASE_ALL;
    }
    
    public int getTechBase() {
        return techAdvancement.getTechBase();
    }

    public static String getEquipDateAsString(int date) {
        if (date == DATE_NONE) {
            return "-";
        } else {
            return Integer.toString(date);
        }

    }
    
    @Override
    public int getIntroductionDate(boolean clan) {
        return techAdvancement.getIntroductionDate(clan);
    }
    
    @Override
    public int getIntroductionDate() {
        return techAdvancement.getIntroductionDate();
    }
    
    @Override
    public int getIntroductionDate(boolean clan, int faction) {
        return techAdvancement.getIntroductionDate(clan, faction);
    }

    @Override
    public int getExtinctionDate(boolean clan) {
        return techAdvancement.getExtinctionDate(clan);
    }

    @Override
    public int getExtinctionDate() {
        return techAdvancement.getExtinctionDate();
    }

    @Override
    public int getExtinctionDate(boolean clan, int faction) {
        return techAdvancement.getExtinctionDate(clan, faction);
    }

    @Override
    public int getReintroductionDate(boolean clan) {
        return techAdvancement.getReintroductionDate(clan);
    }

    @Override
    public int getReintroductionDate() {
        return techAdvancement.getReintroductionDate();
    }

    @Override
    public int getReintroductionDate(boolean clan, int faction) {
        return techAdvancement.getReintroductionDate(clan, faction);
    }

    public static double getArmorCost(int inArmor) {
        if ((inArmor < 0) || (inArmor >= armorCosts.length)) {
            return -1;
        }
        return armorCosts[inArmor];
    }

    public static double getStructureCost(int inStructure) {
        if ((inStructure < 0) || (inStructure >= structureCosts.length)) {
            return -1;
        }
        return structureCosts[inStructure];
    }

    public static double getArmorPointMultiplier(int inArmor) {
        return EquipmentType.getArmorPointMultiplier(inArmor,
                TechConstants.T_IS_TW_NON_BOX);
    }

    public static double getArmorPointMultiplier(int inArmor, int inTechLevel) {
        return EquipmentType
                .getArmorPointMultiplier(
                        inArmor,
                        ((inTechLevel == TechConstants.T_CLAN_TW) || (inTechLevel == TechConstants.T_CLAN_ADVANCED))
                                || (inTechLevel == TechConstants.T_CLAN_EXPERIMENTAL)
                                || (inTechLevel == TechConstants.T_CLAN_UNOFFICIAL));
    }

    public static double getArmorPointMultiplier(int inArmor, boolean clanArmor) {
        if ((inArmor < 0) || (inArmor >= armorPointMultipliers.length)) {
            return POINT_MULTIPLIER_UNKNOWN;
        }
        /*
         * now handled in a single if statement
        if ((inArmor == T_ARMOR_FERRO_FIBROUS) && clanArmor) {
            return POINT_MULTIPLIER_CLAN_FF;
        }
        if ((inArmor == T_ARMOR_ALUM) && clanArmor) {
            return POINT_MULTIPLIER_CLAN_FF;
        }*/
        // Clan armors of these types have a multiplier exactly 0.08 higher than the I.S. variety
        if (clanArmor && ((inArmor == T_ARMOR_ALUM) || (inArmor == T_ARMOR_FERRO_FIBROUS))) {
            return armorPointMultipliers[inArmor] + POINT_ADDITION_CLAN_FF;

        }
        return armorPointMultipliers[inArmor];
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final EquipmentType other = (EquipmentType) obj;
        return Objects.equals(internalName, other.internalName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(internalName);
    }

    public static void writeEquipmentDatabase(File f) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(f));
            w.write("Megamek Equipment Database");
            w.newLine();
            w.write("This file can be regenerated with java -jar MegaMek.jar -eqdb ");
            w.write(f.toString());
            w.newLine();
            w.write("Type,Tech Base,Rules,Name,Aliases");
            w.newLine();
            for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e
                    .hasMoreElements();) {
                EquipmentType type = e.nextElement();
                if (type instanceof AmmoType) {
                    w.write("A,");
                } else if (type instanceof WeaponType) {
                    w.write("W,");
                } else {
                    w.write("M,");
                }
                for (int year : type.getTechLevels().keySet()) {
                    w.write(year
                            + "-"
                            + TechConstants.getTechName(type.getTechLevel(year)));
                }
                w.write(",");
                for (int year : type.getTechLevels().keySet()) {
                    w.write(year
                            + "-"
                            + TechConstants.getLevelName(type
                                    .getTechLevel(year)));
                }
                w.write(",");
                for (Enumeration<String> names = type.getNames(); names
                        .hasMoreElements();) {
                    String name = names.nextElement();
                    w.write(name + ",");
                }
                w.newLine();
            }
            w.flush();
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeEquipmentExtendedDatabase(File f) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(f));
            w.write("Megamek Equipment Extended Database");
            w.newLine();
            w.write("This file can be regenerated with java -jar MegaMek.jar -eqedb ");
            w.write(f.toString());
            w.newLine();
            w.write("Type,Name,Tech Base,Rules,Tech Rating,Introduction Date,Extinction Date,Re-Introduction Date,Tonnage,Crits,Cost,BV,Alias");
            w.newLine();
            for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e
                    .hasMoreElements();) {
                EquipmentType type = e.nextElement();
                if (type instanceof AmmoType) {
                    w.write("A,");
                } else if (type instanceof WeaponType) {
                    w.write("W,");
                } else {
                    w.write("M,");
                }
                w.write(type.getName());
                w.write(",");
                for (int year : type.getTechLevels().keySet()) {
                    w.write(year
                            + "-"
                            + TechConstants.getTechName(type.getTechLevel(year)));
                }
                w.write(",");
                for (int year : type.getTechLevels().keySet()) {
                    w.write(year
                            + "-"
                            + TechConstants.getLevelName(type
                                    .getTechLevel(year)));
                }

                w.write(",");
                w.write(type.getFullRatingName());
                w.write(",");
                w.write(getEquipDateAsString(type.getIntroductionDate()));
                w.write(",");
                w.write(getEquipDateAsString(type.getExtinctionDate()));
                w.write(",");
                w.write(getEquipDateAsString(type.getReintroductionDate()));
                w.write(",");
                if (type.tonnage == EquipmentType.TONNAGE_VARIABLE) {
                    w.write("Variable");
                } else {
                    w.write(Double.toString(type.tonnage));
                }
                w.write(",");
                if (type.criticals == EquipmentType.CRITICALS_VARIABLE) {
                    w.write("Variable");
                } else {
                    w.write(Integer.toString(type.criticals));
                }
                w.write(",");
                if (type.cost == EquipmentType.COST_VARIABLE) {
                    w.write("Variable");
                } else {
                    w.write(Double.toString(type.getCost(null, false, -1)));
                }
                w.write(",");
                if (type.bv == EquipmentType.BV_VARIABLE) {
                    w.write("Variable");
                } else {
                    w.write(Double.toString(type.bv));
                }
                w.write(",");
                for (Enumeration<String> names = type.getNames(); names
                        .hasMoreElements();) {
                    String name = names.nextElement();
                    w.write(name + ",");
                }
                w.newLine();
            }
            w.flush();
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "EquipmentType: " + name;
    }

    protected static GameOptions getGameOptions() {
        if (Server.getServerInstance() == null) {
            return null;
        } else if (Server.getServerInstance().getGame() == null) {
            return null;
        }
        return Server.getServerInstance().getGame().getOptions();
    }

    public String getShortName() {
        if (shortName.trim().length() < 1) {
            return getName();
        }

        return shortName;
    }

    @Override
    public boolean isIntroLevel() {
        return techAdvancement.isIntroLevel();
    }

    @Override
    public boolean isUnofficial() {
        return techAdvancement.isUnofficial();
    }

    @Override
    public int getPrototypeDate() {
        return techAdvancement.getPrototypeDate();
    }

    @Override
    public int getPrototypeDate(boolean clan, int faction) {
        return techAdvancement.getPrototypeDate(clan, faction);
    }

    @Override
    public int getProductionDate() {
        return techAdvancement.getProductionDate();
    }

    @Override
    public int getProductionDate(boolean clan, int faction) {
        return techAdvancement.getProductionDate(clan, faction);
    }

    @Override
    public int getCommonDate() {
        return techAdvancement.getCommonDate();
    }

    @Override
    public int getBaseAvailability(int era) {
        return techAdvancement.getBaseAvailability(era);
    }
}
