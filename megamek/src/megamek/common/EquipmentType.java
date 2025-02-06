/*
 * MegaMek - Copyright (C) 2002-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2018-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.BitSet;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.ArmorType;
import megamek.common.weapons.autocannons.HVACWeapon;
import megamek.common.weapons.defensivepods.BPodWeapon;
import megamek.common.weapons.defensivepods.MPodWeapon;
import megamek.common.weapons.ppc.PPCWeapon;
import megamek.logging.MMLogger;

/**
 * Represents any type of equipment mounted on a 'Mek, excluding systems and
 * actuators.
 *
 * @author Ben
 * @since April 1, 2002, 1:35 PM
 */
public class EquipmentType implements ITechnology {
    private static final MMLogger logger = MMLogger.create(EquipmentType.class);

    public static final double TONNAGE_VARIABLE = Float.MIN_VALUE;
    public static final int CRITICALS_VARIABLE = Integer.MIN_VALUE;
    public static final int BV_VARIABLE = Integer.MIN_VALUE;
    public static final int COST_VARIABLE = Integer.MIN_VALUE;
    /**
     * Default value for support vehicle slot cost. Those that differ from `Meks
     * are assigned a value >= 0
     */
    private static final int MEK_SLOT_COST = -1;

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
    public static final int T_ARMOR_LC_FERRO_CARBIDE = 11; // Large Craft Only
    public static final int T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE = 12; // Large Craft Only
    public static final int T_ARMOR_LC_FERRO_IMP = 13; // Large Craft Only
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
    public static final int T_ARMOR_BA_STANDARD = 28;
    public static final int T_ARMOR_BA_STANDARD_PROTOTYPE = 29;
    public static final int T_ARMOR_BA_STANDARD_ADVANCED = 30;
    public static final int T_ARMOR_BA_STEALTH_BASIC = 31;
    public static final int T_ARMOR_BA_STEALTH = 32;
    public static final int T_ARMOR_BA_STEALTH_IMP = 33;
    public static final int T_ARMOR_BA_STEALTH_PROTOTYPE = 34;
    public static final int T_ARMOR_BA_FIRE_RESIST = 35;
    public static final int T_ARMOR_BA_MIMETIC = 36;
    public static final int T_ARMOR_BA_REFLECTIVE = 37;
    public static final int T_ARMOR_BA_REACTIVE = 38;
    public static final int T_ARMOR_PRIMITIVE_FIGHTER = 39;
    public static final int T_ARMOR_PRIMITIVE_AERO = 40;
    public static final int T_ARMOR_AEROSPACE = 41;
    public static final int T_ARMOR_STANDARD_PROTOMEK = 42;
    public static final int T_ARMOR_SV_BAR_2 = 43;
    public static final int T_ARMOR_SV_BAR_3 = 44;
    public static final int T_ARMOR_SV_BAR_4 = 45;
    public static final int T_ARMOR_SV_BAR_5 = 46;
    public static final int T_ARMOR_SV_BAR_6 = 47;
    public static final int T_ARMOR_SV_BAR_7 = 48;
    public static final int T_ARMOR_SV_BAR_8 = 49;
    public static final int T_ARMOR_SV_BAR_9 = 50;
    public static final int T_ARMOR_SV_BAR_10 = 51;

    public static final int T_STRUCTURE_UNKNOWN = -1;
    public static final int T_STRUCTURE_STANDARD = 0;
    public static final int T_STRUCTURE_INDUSTRIAL = 1;
    public static final int T_STRUCTURE_ENDO_STEEL = 2;
    public static final int T_STRUCTURE_ENDO_PROTOTYPE = 3;
    public static final int T_STRUCTURE_REINFORCED = 4;
    public static final int T_STRUCTURE_COMPOSITE = 5;
    public static final int T_STRUCTURE_ENDO_COMPOSITE = 6;

    public static final String[] structureNames = {
            "Standard",
            "Industrial",
            "Endo Steel",
            "Endo Steel Prototype",
            "Reinforced",
            "Composite",
            "Endo-Composite"
    };

    // Assume for now that prototype is not more expensive
    public static final double[] structureCosts = {
            400,
            300,
            1600,
            4800,
            6400,
            1600,
            3200
    };

    protected String name = null;

    // Short name for RS Printing
    protected String shortName = "";

    protected String internalName = null;

    /**
     * Sorting lists of equipment by this string groups and sorts equipment better.
     */
    protected String sortingName;

    private Vector<String> namesVector = new Vector<String>();

    protected double tonnage = 0;
    protected int criticals = 0;
    protected int tankslots = 1;
    protected int svslots = MEK_SLOT_COST;

    protected boolean explosive = false;
    protected boolean hittable = true; // if false, reroll critical hits

    /** can the criticals for this be spread over locations? */
    protected boolean spreadable = false;
    protected int toHitModifier = 0;

    protected TechAdvancement techAdvancement = new TechAdvancement();

    protected EquipmentBitSet flags = new EquipmentBitSet();

    protected long subType = 0;

    protected double bv = 0; // battle value point system
    protected double cost = 0; // The C-Bill cost of the item.

    // For equipment that cannot be pod-mounted on an Omni unit
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

    // static list of equipment
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

    public void setFlags(EquipmentBitSet flags) {
        this.flags = flags;
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

    public String getName(double size) {
        return getName();
    }

    public String getDesc() {
        String result = EquipmentMessages.getString("EquipmentType." + name);
        if (result != null) {
            return result;
        }
        return name;
    }

    public String getDesc(double size) {
        return getDesc();
    }

    public String getInternalName() {
        return internalName;
    }

    public String getRulesRefs() {
        return rulesRefs;
    }

    /**
     * @deprecated The old tech progression system has been replaced by the
     *             TechAdvancement class.
     */
    // @Deprecated This is using the new TechAdvancement class under the hood.
    // Should it really be deprecated now?
    @Deprecated
    public Map<Integer, Integer> getTechLevels() {
        Map<Integer, Integer> techLevel = new HashMap<Integer, Integer>();
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
                    isIntroLevel() ? TechConstants.T_INTRO_BOXSET : TechConstants.T_IS_TW_NON_BOX);
        }

        return techLevel;
    }

    @Override
    public int getTechLevel(int date) {
        return techAdvancement.getTechLevel(date);
    }

    @Override
    public int getTechLevel(int date, boolean clan) {
        return techAdvancement.getTechLevel(date, clan);
    }

    @Override
    public SimpleTechLevel getStaticTechLevel() {
        if (null != techAdvancement.getStaticTechLevel()) {
            return techAdvancement.getStaticTechLevel();
        } else {
            return techAdvancement.guessStaticTechLevel(rulesRefs);
        }
    }

    /**
     * Calculates the weight of the equipment. If {@code entity} is {@code null},
     * equipment without a fixed weight will return
     * {@link EquipmentType#TONNAGE_VARIABLE}.
     *
     * @param entity The unit the equipment is mounted on
     * @return The weight of the equipment in tons
     */
    public double getTonnage(@Nullable Entity entity) {
        return getTonnage(entity, Entity.LOC_NONE, 1.0);
    }

    /**
     * Calculates the weight of the equipment. If {@code entity} is {@code null},
     * equipment without a fixed weight will return
     * {@link EquipmentType#TONNAGE_VARIABLE}.
     *
     * @param entity The unit the equipment is mounted on
     * @param size   The size of variable-sized equipment
     * @return The weight of the equipment in tons
     */
    public double getTonnage(@Nullable Entity entity, double size) {
        return getTonnage(entity, Entity.LOC_NONE, size);
    }

    /**
     * Calculates the weight of the equipment. If {@code entity} is {@code null},
     * equipment without a fixed weight will return
     * {@link EquipmentType#TONNAGE_VARIABLE}.
     *
     * @param entity   The unit the equipment is mounted on
     * @param location The mount location
     * @param size     The size (for variable-sized equipment)
     * @return The weight of the equipment in tons
     */
    public double getTonnage(Entity entity, int location, double size) {
        return tonnage;
    }

    /**
     * Calculates the weight of the equipment, with the option to override the
     * standard rounding based on unit type. This allows for the optional fractional
     * accounting construction rules. If {@code entity} is {@code null}, equipment
     * without a fixed weight will return {@link EquipmentType#TONNAGE_VARIABLE}.
     *
     * @param entity        The unit the equipment is mounted on
     * @param location      The mount location
     * @param size          The size (for variable-sized equipment)
     * @param defaultMethod The rounding method to use for any variable weight
     *                      equipment. Any equipment that is normally rounded to
     *                      either the half ton or kg based on unit type will have
     *                      this method applied instead.
     * @return The weight of the equipment in tons
     */
    public double getTonnage(Entity entity, int location, double size, RoundWeight defaultMethod) {
        // Default implementation does not deal with variable-weight equipment.
        return getTonnage(entity, location, size);
    }

    void setTonnage(double tonnage) {
        this.tonnage = tonnage;
    }

    public int getCriticals(Entity entity) {
        return getCriticals(entity, 1.0);
    }

    public int getCriticals(Entity entity, double size) {
        return criticals;
    }

    public int getTankSlots(Entity entity) {
        return tankslots;
    }

    public int getSupportVeeSlots(Entity entity) {
        if (svslots == MEK_SLOT_COST) {
            return getCriticals(entity);
        }
        return svslots;
    }

    public boolean isExplosive(Mounted<?> mounted) {
        return isExplosive(mounted, false);
    }

    public boolean isExplosive(Mounted<?> mounted, boolean ignoreCharge) {
        if (null == mounted) {
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
            Mounted<?> ammo = mounted.getLinked();
            if ((ammo == null) || !(ammo.getType() instanceof AmmoType)
                    || (!((AmmoType) ammo.getType()).getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY_AC))) {
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
            // for BV purposes, we need to ignore the charged-ness and check only
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

    public int getToHitModifier(@Nullable Mounted<?> mounted) {
        return toHitModifier;
    }

    public EquipmentBitSet getFlags() {
        return flags;
    }

    public boolean hasFlag(EquipmentFlag flag) {
        return flags.get(flag);
    }

    /**
     * Checks if the equipment has all of the specified flags.
     * @param flag The flags to check
     * @return True if the equipment has all of the specified flags
     */
    public boolean hasFlag(EquipmentBitSet flag) {
        return flags.contains(flag);
    }


    public double getBV(Entity entity) {
        return bv;
    }

    /**
     * @return - whether the equipment must be considered part of the base chassis
     *         when mounted on an omni unit
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
     * Simple way to check if a piece of equipment has a specific usage/firing mode
     *
     * @param modeType The name of the mode to check.
     * @return True or false.
     */
    public boolean hasModeType(String modeType) {
        if (!hasModes()) {
            return false;
        }

        // Avoid Concurrent Modification exception with this one simple trick!
        synchronized (modes) {
            for (Iterator<EquipmentMode> iterator = modes.iterator(); iterator.hasNext(); ) {
                if (iterator.next().getName().equals(modeType)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param mounted The equipment mount. In some cases the moudes are affected by linked equipment.
     * @return the number of modes that this type of equipment can be in or
     *         <code>0</code> if it doesn't have modes.
     */
    public int getModesCount(Mounted<?> mounted) {
        return getModesCount();
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

        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return false;
            }

            @Override
            public EquipmentMode nextElement() {
                return null;
            }

        };
    }

    /**
     * Sets the modes that this type of equipment can be in. By default, the
     * EquipmentType doesn't have the modes, so don't try to call this method
     * with null or empty argument.
     *
     * @param modes non null, non empty list of available mode names.
     */
    protected void setModes(String... modes) {
        Vector<EquipmentMode> newModes = new Vector<>(modes.length);
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
     * @return true if the mode was added; false if modes was null or the mode was
     *         already present
     * @author Simon (Juliez)
     */
    public boolean addMode(String mode) {
        if (modes == null) {
            modes = new Vector<>();
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
     * @param mode - the <code>String</code> of the mode name involved in the
     *             switch
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
     * <code>0</code> to <code>getModesCount() - 1</code>
     * </p>
     *
     * Fails if this type of the equipment doesn't have modes, or given mode is
     * out of the valid range.
     *
     * @param modeNum
     * @return mode number <code>modeNum</code> from the list of modes available
     *         for this type of equipment.
     * @see #hasModes()
     */
    public EquipmentMode getMode(int modeNum) {
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

    /**
     * Returns an EquipmentType having the given internal name or lookup name (but
     * not the "name" which is the display text unless it's equal to the internal
     * name). Internal names may be taken from {@link EquipmentTypeLookup}. Returns
     * null if there is none with that name.
     *
     * @param key The internal name or lookup name
     * @return The EquipmentType with the given internal name or lookup name
     */
    public static @Nullable EquipmentType get(String key) {
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
            EquipmentType.allTypes = new Vector<>();
            EquipmentType.lookupHash = new Hashtable<>();

            WeaponType.initializeTypes();
            AmmoType.initializeTypes();
            MiscType.initializeTypes();
            BombType.initializeTypes();
            SmallWeaponAmmoType.initializeTypes();
            ArmorType.initializeTypes();
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

    /**
     * @return All equipment types as a List. The list is a copy and can safely be
     *         modified.
     */
    public static List<EquipmentType> allTypes() {
        if (EquipmentType.allTypes == null) {
            EquipmentType.initializeTypes();
        }
        return new ArrayList<EquipmentType>(EquipmentType.allTypes);
    }

    protected static void addType(EquipmentType type) {
        if (null == EquipmentType.allTypes) {
            EquipmentType.initializeTypes();
        }
        EquipmentType.allTypes.addElement(type);
    }

    public static int getArmorType(EquipmentType et) {
        if (et instanceof ArmorType) {
            return ((ArmorType) et).getArmorType();
        } else {
            return T_ARMOR_UNKNOWN;
        }
    }

    public static String getArmorTypeName(int armorType) {
        ArmorType armor = ArmorType.of(armorType, false);
        if (armor == null) {
            armor = ArmorType.of(armorType, true);
        }
        if (armor != null) {
            return armor.getName();
        } else {
            return "UNKNOWN";
        }
    }

    public static String getArmorTypeName(int armorType, boolean clan) {
        ArmorType armor = ArmorType.of(armorType, clan);
        if (armor != null) {
            return clan ? "Clan " + armor.getName() : "IS " + armor.getName();
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Convenience method to test whether an EquipmentType instance is armor.
     *
     * @param et The equipment instance to test
     * @return Whether the equipment is an armor type
     */
    public static boolean isArmorType(EquipmentType et) {
        return et instanceof ArmorType;
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
        return clan ? "Clan " + structureNames[structureType]
                : "IS " + structureNames[structureType];
    }

    /**
     * Convenience method to test whether an EquipmentType instance is mek
     * structure. This works by comparing the results of {@link #getName()} to the
     * structure names array and returning {@code true} if there is a match.
     *
     * @param et The equipment instance to test
     * @return Whether the equipment is a structure type
     */
    public static boolean isStructureType(EquipmentType et) {
        return getStructureType(et) != T_STRUCTURE_UNKNOWN;
    }

    /**
     * Gives the weight of a single point of armor at a particular BAR for a
     * given tech level.
     */
    private static final double[][] SV_ARMOR_WEIGHT = {
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { .040, .025, .016, .013, .012, .011 },
            { .060, .038, .024, .019, .017, .016 },
            { .000, .050, .032, .026, .023, .021 },
            { .000, .063, .040, .032, .028, .026 },
            { .000, .000, .048, .038, .034, .032 },
            { .000, .000, .056, .045, .040, .037 },
            { .000, .000, .000, .051, .045, .042 },
            { .000, .000, .000, .057, .051, .047 },
            { .000, .000, .000, .063, .056, .052 }
    };

    /*
     * Armor and structure are stored as integers and standard uses a generic
     * MiscType that does not have its own TechAdvancement.
     */

    protected static final TechAdvancement TA_STANDARD_STRUCTURE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2430, 2439, 2505).setApproximate(true, false, false).setIntroLevel(true)
            .setTechRating(RATING_D).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.INTRO);
    protected static final TechAdvancement TA_NONE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE).setTechRating(RATING_A)
            .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.INTRO);

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
     * For variable-sized equipment this assumes a size of 1.0.
     *
     * @return The C-Bill cost of the piece of equipment.
     */
    public double getCost(Entity entity, boolean armored, int loc) {
        return getCost(entity, armored, loc, 1.0);
    }

    /**
     * @return The C-Bill cost of the piece of equipment.
     */
    public double getCost(Entity entity, boolean armored, int loc, double size) {
        return cost;
    }

    public double getRawCost() {
        return cost;
    }

    /**
     * @return Whether the item weight varies according to the unit it's installed
     *         on
     */
    public boolean isVariableTonnage() {
        return tonnage == TONNAGE_VARIABLE;
    }

    /**
     * @return Whether the item BV varies according to the unit it's installed on
     */
    public boolean isVariableBV() {
        return bv == BV_VARIABLE;
    }

    /**
     * @return Whether the item cost varies according to the unit it's installed on
     */
    public boolean isVariableCost() {
        return cost == COST_VARIABLE;
    }

    public boolean isVariableCriticals() {
        return criticals == CRITICALS_VARIABLE;
    }

    /**
     * @return Whether the item's size is variable independent of external factors
     */
    public boolean isVariableSize() {
        return false;
    }

    /**
     * @return The increment between sizes of variable-sized equipment
     */
    public Double variableStepSize() {
        return 1.0;
    }

    /**
     * @return The maximum size of variable-sized equipment. Items with no maximum
     *         return {@code null}.
     */
    public @Nullable Double variableMaxSize() {
        return null;
    }

    public TechAdvancement getTechAdvancement() {
        return techAdvancement;
    }

    @Override
    public int getTechRating() {
        return techAdvancement.getTechRating();
    }

    @Override
    public boolean isClan() {
        return techAdvancement.getTechBase() == TECH_BASE_CLAN;
    }

    @Override
    public boolean isMixedTech() {
        return techAdvancement.getTechBase() == TECH_BASE_ALL;
    }

    @Override
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

    public static double getStructureCost(int inStructure) {
        if ((inStructure < 0) || (inStructure >= structureCosts.length)) {
            return -1;
        }
        return structureCosts[inStructure];
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
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
            w.write("MegaMek Equipment Database");
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
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public static void writeEquipmentExtendedDatabase(File f) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f));
            bufferedWriter.write("MegaMek Equipment Extended Database");
            bufferedWriter.newLine();
            bufferedWriter.write("This file can be regenerated with java -jar MegaMek.jar -eqedb ");
            bufferedWriter.write(f.toString());
            bufferedWriter.newLine();
            bufferedWriter.write(
                    "Type,Name,Tech Base,Rules,Tech Rating,Static Tech Level,Introduction Date,Prototype Date,Production Date,Common Date,Extinction Date,Re-Introduction Date,Tonnage,CriticalSlots,Cost,BV,RulesRef,Alias");
            bufferedWriter.newLine();
            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes(); equipmentTypes
                    .hasMoreElements();) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if (equipmentType instanceof AmmoType) {
                    bufferedWriter.write("A");
                } else if (equipmentType instanceof WeaponType) {
                    bufferedWriter.write("W");
                } else {
                    bufferedWriter.write("M");
                }

                bufferedWriter.write(",\"");
                bufferedWriter.write(equipmentType.getName());

                // Gather the unique tech levels for this equipment ...
                List<Integer> levels = equipmentType.getTechLevels().keySet().stream()
                        .map(equipmentType::getTechLevel)
                        .sorted() // ordered for ease of use
                        .distinct()
                        .collect(Collectors.toList());

                // ... and use them to output the tech names ...
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                        .map(TechConstants::getTechName)
                        .distinct()
                        .collect(Collectors.joining("/")));

                // ... and associated rules levels.
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                        .map(TechConstants::getLevelName)
                        .distinct()
                        .collect(Collectors.joining("/")));

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getFullRatingName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getStaticTechLevel().toString());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getIntroductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getPrototypeDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getProductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getCommonDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getExtinctionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getReintroductionDateName());

                bufferedWriter.write("\",");
                if (equipmentType.tonnage == EquipmentType.TONNAGE_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.tonnage));
                }

                bufferedWriter.write(",");
                if (equipmentType.criticals == EquipmentType.CRITICALS_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Integer.toString(equipmentType.criticals));
                }

                bufferedWriter.write(",");
                if (equipmentType.cost == EquipmentType.COST_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.getCost(null, false, -1, 1.0)));
                }

                bufferedWriter.write(",");
                if (equipmentType.bv == EquipmentType.BV_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.bv));
                }

                bufferedWriter.write(",\"");
                bufferedWriter.write(equipmentType.getRulesRefs());

                bufferedWriter.write("\",\"");
                for (Enumeration<String> names = equipmentType.getNames(); names.hasMoreElements();) {
                    String name = names.nextElement();
                    bufferedWriter.write(name + ",");
                }
                bufferedWriter.write("\"");
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public static void writeEquipmentWeaponDatabase(File f) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f));
            bufferedWriter.write("MegaMek Equipment Weapon Database");
            bufferedWriter.newLine();
            bufferedWriter.write("This file can be regenerated with java -jar MegaMek.jar -eqwdb ");
            bufferedWriter.write(f.toString());
            bufferedWriter.newLine();
            bufferedWriter.write(
                    "Name,Tech Base,Rules,Tech Rating,Static Tech Level,Introduction Date,Prototype Date,Production Date,Common Date,Extinction Date,Re-Introduction Date,Tonnage,CriticalSlots,Cost,BV,RulesRef,MinimalRange,ShortRange,MediumRange,LongRange,ExtremeRange,ShortWaterRange,MediumWaterRange,LongWaterRange,ExtremeWaterRange,MinimalDamage,ShortDamage,MediumDamage,LongDamage,ExtremeDamage,Alias");
            bufferedWriter.newLine();

            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes(); equipmentTypes
                    .hasMoreElements();) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if (!(equipmentType instanceof WeaponType)) {
                    continue;
                }
                WeaponType weaponType = (WeaponType) equipmentType;

                bufferedWriter.write("\"");
                bufferedWriter.write(weaponType.getName());

                // Gather the unique tech levels for this equipment ...
                List<Integer> levels = weaponType.getTechLevels().keySet().stream()
                        .map(weaponType::getTechLevel)
                        .sorted() // ordered for ease of use
                        .distinct()
                        .collect(Collectors.toList());

                // ... and use them to output the tech names ...
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                        .map(TechConstants::getTechName)
                        .distinct()
                        .collect(Collectors.joining("/")));

                // ... and associated rules levels.
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                        .map(TechConstants::getLevelName)
                        .distinct()
                        .collect(Collectors.joining("/")));

                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getFullRatingName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getStaticTechLevel().toString());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getIntroductionDateName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getPrototypeDateName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getProductionDateName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getCommonDateName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getExtinctionDateName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getReintroductionDateName());
                bufferedWriter.write("\",");
                if (weaponType.tonnage == EquipmentType.TONNAGE_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(weaponType.tonnage));
                }
                bufferedWriter.write(",");
                if (weaponType.criticals == EquipmentType.CRITICALS_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Integer.toString(weaponType.criticals));
                }
                bufferedWriter.write(",");
                if (weaponType.cost == EquipmentType.COST_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(weaponType.getCost(null, false, -1, 1.0)));
                }
                bufferedWriter.write(",");
                if (weaponType.bv == EquipmentType.BV_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(weaponType.bv));
                }

                bufferedWriter.write(",\"");
                bufferedWriter.write(weaponType.getRulesRefs());

                int minimalRange = weaponType.getMinimumRange();
                minimalRange = (minimalRange < 0) ? -1 : minimalRange;
                bufferedWriter.write("\",");
                bufferedWriter.write(Integer.toString(minimalRange));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getShortRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getMediumRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getLongRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getExtremeRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getWShortRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getWMediumRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getWLongRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getWExtremeRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getDamage(RangeType.RANGE_MINIMUM)));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getDamage(RangeType.RANGE_SHORT)));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getDamage(RangeType.RANGE_MEDIUM)));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getDamage(RangeType.RANGE_LONG)));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getDamage(RangeType.RANGE_EXTREME)));

                bufferedWriter.write(",\"");
                for (Enumeration<String> names = weaponType.getNames(); names.hasMoreElements();) {
                    String name = names.nextElement();
                    bufferedWriter.write(name + ",");
                }

                bufferedWriter.write("\"");
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public static void writeEquipmentAmmoDatabase(File f) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f));
            bufferedWriter.write("MegaMek Equipment Armor Database");
            bufferedWriter.newLine();
            bufferedWriter.write("This file can be regenerated with java -jar MegaMek.jar -eqadb ");
            bufferedWriter.write(f.toString());
            bufferedWriter.newLine();
            bufferedWriter.write(
                    "Name,Tech Base,Rules,Tech Rating,Static Tech Level,Introduction Date,Prototype Date,Production Date,Common Date,Extinction Date,Re-Introduction Date,Tonnage,CriticalSlots,Cost,BV,RulesRef,CountAsFlak?,MunitionType,DamagePerShot,RackSize,Shots,AmmoRatio,IsCapital,KgPerShot,AeroUse?,Alias");
            bufferedWriter.newLine();
            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes(); equipmentTypes
                    .hasMoreElements();) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if (!(equipmentType instanceof AmmoType)) {
                    continue;
                }
                AmmoType ammoType = (AmmoType) equipmentType;

                bufferedWriter.write("\"");
                bufferedWriter.write(ammoType.getName());

                // Gather the unique tech levels for this equipment ...
                List<Integer> levels = ammoType.getTechLevels().keySet().stream()
                        .map(ammoType::getTechLevel)
                        .sorted() // ordered for ease of use
                        .distinct()
                        .collect(Collectors.toList());

                // ... and use them to output the tech names ...
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                        .map(TechConstants::getTechName)
                        .distinct()
                        .collect(Collectors.joining("/")));

                // ... and associated rules levels.
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                        .map(TechConstants::getLevelName)
                        .distinct()
                        .collect(Collectors.joining("/")));

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getFullRatingName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getStaticTechLevel().toString());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getIntroductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getPrototypeDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getProductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getCommonDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getExtinctionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getReintroductionDateName());

                bufferedWriter.write("\",");
                if (ammoType.tonnage == EquipmentType.TONNAGE_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(ammoType.tonnage));
                }

                bufferedWriter.write(",");
                if (ammoType.criticals == EquipmentType.CRITICALS_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Integer.toString(ammoType.criticals));
                }

                bufferedWriter.write(",");
                if (ammoType.cost == EquipmentType.COST_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(ammoType.getCost(null, false, -1, 1.0)));
                }

                bufferedWriter.write(",");
                if (ammoType.bv == EquipmentType.BV_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(ammoType.bv));
                }

                bufferedWriter.write(",\"");
                bufferedWriter.write(ammoType.getRulesRefs());

                bufferedWriter.write("\",");
                bufferedWriter.write(Boolean.toString(ammoType.countsAsFlak()));

                bufferedWriter.write(",");
                bufferedWriter.write(ammoType.getMunitionType().toString());

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(ammoType.getDamagePerShot()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(ammoType.getRackSize()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(ammoType.getShots()));

                bufferedWriter.write(",");
                bufferedWriter.write(Double.toString(ammoType.getAmmoRatio()));

                bufferedWriter.write(",");
                bufferedWriter.write(Boolean.toString(ammoType.isCapital()));

                bufferedWriter.write(",");
                bufferedWriter.write(Double.toString(ammoType.getKgPerShot()));

                bufferedWriter.write(",");
                bufferedWriter.write(Boolean.toString(ammoType.canAeroUse()));

                bufferedWriter.write(",\"");
                for (Enumeration<String> names = ammoType.getNames(); names.hasMoreElements();) {
                    String name = names.nextElement();
                    bufferedWriter.write(name + ",");
                }
                bufferedWriter.write("\"");
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public static void writeEquipmentMiscDatabase(File f) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f));
            bufferedWriter.write("MegaMek Equipment Extended Database");
            bufferedWriter.newLine();
            bufferedWriter.write("This file can be regenerated with java -jar MegaMek.jar -eqmdb ");
            bufferedWriter.write(f.toString());
            bufferedWriter.newLine();
            bufferedWriter.write(
                    "Name,Tech Base,Rules,Tech Rating,Static Tech Level,Introduction Date,Prototype Date,Production Date,Common Date,Extinction Date,Re-Introduction Date,Tonnage,CriticalSlots,Cost,BV,RulesRef,Alias");
            bufferedWriter.newLine();
            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes(); equipmentTypes
                    .hasMoreElements();) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if ((equipmentType instanceof AmmoType) || (equipmentType instanceof WeaponType)) {
                    continue;
                }

                bufferedWriter.write("\"");
                bufferedWriter.write(equipmentType.getName());

                // Gather the unique tech levels for this equipment ...
                List<Integer> levels = equipmentType.getTechLevels().keySet().stream()
                        .map(equipmentType::getTechLevel)
                        .sorted() // ordered for ease of use
                        .distinct()
                        .collect(Collectors.toList());

                // ... and use them to output the tech names ...
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                        .map(TechConstants::getTechName)
                        .distinct()
                        .collect(Collectors.joining("/")));

                // ... and associated rules levels.
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                        .map(TechConstants::getLevelName)
                        .distinct()
                        .collect(Collectors.joining("/")));

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getFullRatingName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getStaticTechLevel().toString());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getIntroductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getPrototypeDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getProductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getCommonDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getExtinctionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getReintroductionDateName());

                bufferedWriter.write("\",");
                if (equipmentType.tonnage == EquipmentType.TONNAGE_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.tonnage));
                }

                bufferedWriter.write(",");
                if (equipmentType.criticals == EquipmentType.CRITICALS_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Integer.toString(equipmentType.criticals));
                }

                bufferedWriter.write(",");
                if (equipmentType.cost == EquipmentType.COST_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.getCost(null, false, -1, 1.0)));
                }

                bufferedWriter.write(",");
                if (equipmentType.bv == EquipmentType.BV_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.bv));
                }

                bufferedWriter.write(",\"");
                bufferedWriter.write(equipmentType.getRulesRefs());

                bufferedWriter.write("\",\"");
                for (Enumeration<String> names = equipmentType.getNames(); names.hasMoreElements();) {
                    String name = names.nextElement();
                    bufferedWriter.write(name + ",");
                }

                bufferedWriter.write("\"");
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    @Override
    public String toString() {
        return "[Equipment] " + internalName;
    }

    public String getShortName() {
        return shortName.isBlank() ? getName() : shortName;
    }

    public String getShortName(double size) {
        return getShortName();
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

    /**
     * This does not include heat generated by stealth armor, as that depends on
     * whether it is installed as patchwork and does not appear in the equipment
     * list of all unit types.
     *
     * @return The amount of heat generated by the equipment
     */
    public int getHeat() {
        return 0;
    }

    /**
     * Sorting with the String returned by this method results in an improved
     * ordering and grouping of equipment than by getName(); for example,
     * AC2/5/10/20 will appear in that order instead of the order AC10/2/20/5 and
     * S/M/L Lasers will be grouped together.
     *
     * @return A String similar to getName() but modified to support a better
     *         sorting
     */
    public String getSortingName() {
        return (sortingName != null) ? sortingName : name;
    }

    /**
     * Returns true if this equipment is any of those identified by the given type
     * Strings. The given typeInternalNames are compared to the internal name of
     * this EquipmentType, not the (display) name!
     *
     * Best use the constants defined in EquipmentTypeLookup.
     *
     * @param typeInternalName  An Equipment internal name to check
     * @param typeInternalNames More Equipment internal names to check
     * @return true if the internalName of this equipment matches any of the given
     *         types
     */
    public boolean isAnyOf(String typeInternalName, String... typeInternalNames) {
        return internalName.equals(typeInternalName) || Arrays.asList(typeInternalNames).contains(internalName);
    }

    /**
     * Returns true if this equipment is that identified by the given
     * typeInternalName String. The given typeInternalName is compared to the
     * internal name of this EquipmentType, not the (display) name!
     *
     * Best use the constants defined in EquipmentTypeLookup. Calling this is
     * equivalent to {@link #isAnyOf(String, String...)} with only the one
     * parameter.
     *
     * @param typeInternalName An Equipment internal name to check
     * @return true if the internalName of this equipment matches the given type
     */
    public boolean is(String typeInternalName) {
        return isAnyOf(typeInternalName);
    }

    public static Map<Integer, String> getAllStructureCodeName() {
        Map<Integer, String> result = new HashMap<Integer, String>();

        result.put(T_STRUCTURE_UNKNOWN, getStructureTypeName(T_STRUCTURE_UNKNOWN));
        result.put(T_STRUCTURE_STANDARD, getStructureTypeName(T_STRUCTURE_STANDARD));
        result.put(T_STRUCTURE_INDUSTRIAL, getStructureTypeName(T_STRUCTURE_INDUSTRIAL));
        result.put(T_STRUCTURE_ENDO_STEEL, getStructureTypeName(T_STRUCTURE_ENDO_STEEL));
        result.put(T_STRUCTURE_ENDO_PROTOTYPE, getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE));
        result.put(T_STRUCTURE_REINFORCED, getStructureTypeName(T_STRUCTURE_REINFORCED));
        result.put(T_STRUCTURE_COMPOSITE, getStructureTypeName(T_STRUCTURE_COMPOSITE));
        result.put(T_STRUCTURE_ENDO_COMPOSITE, getStructureTypeName(T_STRUCTURE_ENDO_COMPOSITE));

        return result;
    }
}
