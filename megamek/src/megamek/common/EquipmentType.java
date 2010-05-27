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
import java.util.Hashtable;
import java.util.Vector;

/**
 * Represents any type of equipment mounted on a mechs, excluding systems and
 * actuators.
 *
 * @author Ben
 * @version
 */
public class EquipmentType {
    public static final float TONNAGE_VARIABLE = Float.MIN_VALUE;
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
    public static final int T_ARMOR_FERRO_CARBIDE = 11;
    public static final int T_ARMOR_LAMELLOR_FERRO_CARBIDE = 12;
    public static final int T_ARMOR_FERRO_IMP = 13;
    public static final int T_ARMOR_INDUSTRIAL = 14;
    public static final int T_ARMOR_HEAVY_INDUSTRIAL = 15;
    public static final int T_ARMOR_FERRO_LAMELLOR = 16;

    public static final int T_STRUCTURE_UNKNOWN = -1;
    public static final int T_STRUCTURE_STANDARD = 0;
    public static final int T_STRUCTURE_INDUSTRIAL = 1;
    public static final int T_STRUCTURE_ENDO_STEEL = 2;
    public static final int T_STRUCTURE_ENDO_PROTOTYPE = 3;
    public static final int T_STRUCTURE_REINFORCED = 4;
    public static final int T_STRUCTURE_COMPOSITE = 5;
    public static final int T_STRUCTURE_ENDO_COMPOSITE = 6;

    public static final String[] armorNames =
        { "Standard", "Ferro-Fibrous", "Reactive", "Reflective", "Hardened", "Light Ferro-Fibrous", "Heavy Ferro-Fibrous", "Patchwork", "Stealth", "Ferro-Fibrous Prototype", "Commercial", "Ferro-Carbide", "Lamellor Ferro-Carbide", "Improved Ferro-Aluminum", "Industrial", "Heavy Industrial", "Ferro-Lamellor" };

    public static final String[] structureNames =
        { "Standard", "Industrial", "Endo Steel", "Endo Steel Prototype", "Reinforced", "Composite", "Endo-Composite" };

    // Assume for now that prototype is not more expensive
    public static final double[] structureCosts =
        { 400, 300, 1600, 1600, 6400, 3200, 3200 };

    // Assume for now that prototype is not more expensive
    public static final double[] armorCosts =
        { 10000, 20000, 30000, 30000, 15000, 15000, 25000, /*
                                                            * patchwork)
                                                            */50000, 50000, 20000, 3000, 75000, 100000, 50000, 5000, 10000, 35000 };

    public static final double[] armorPointMultipliers =
        { 1, 1.12, 1, 1, 1, 1.06, 1.24, 1, 1, 1.12, 1.5, 1, 1, 1, 0.67, 1.0, 0.875 };
    public static final double POINT_MULTIPLIER_UNKNOWN = 1;
    public static final double POINT_MULTIPLIER_CLAN_FF = 1.2;

    public static final int RATING_A = 0;
    public static final int RATING_B = 1;
    public static final int RATING_C = 2;
    public static final int RATING_D = 3;
    public static final int RATING_E = 4;
    public static final int RATING_F = 5;
    public static final int RATING_X = 6;

    public static final int ERA_SL = 0;
    public static final int ERA_SW = 1;
    public static final int ERA_CLAN = 2;

    public static final int DATE_NONE = -1;

    public static final String[] ratingNames =
        { "A", "B", "C", "D", "E", "F", "X" };

    protected String name = null;

    protected String internalName = null;

    private Vector<String> namesVector = new Vector<String>();

    protected float tonnage = 0;
    protected int criticals = 0;

    protected boolean explosive = false;
    protected boolean hittable = true; // if false, reroll critical hits

    /** can the crits for this be spread over locations? */
    protected boolean spreadable = false;
    protected int toHitModifier = 0;
    protected int techLevel = TechConstants.T_TECH_UNKNOWN;

    protected BigInteger flags = BigInteger.valueOf(0);

    protected long subType = 0;

    protected double bv = 0; // battle value point system
    protected double cost = 0; // The C-Bill cost of the item.

    // fluffy stuff
    protected int techRating = RATING_C;
    protected int[] availRating =
        { RATING_E, RATING_E, RATING_E };
    protected int introDate = DATE_NONE;
    protected int extinctDate = DATE_NONE;
    protected int reintroDate = DATE_NONE;

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

    /** Creates new EquipmentType */
    public EquipmentType() {
        // default constructor
    }

    public void setFlags(BigInteger inF) {
        flags = inF;
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

    public int getTechLevel() {
        return techLevel;
    }

    public float getTonnage(Entity entity) {
        return tonnage;
    }

    public void setTonnage(float tonnage) {
        this.tonnage = tonnage;
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
     * @return <code>true</code> if this type of equipment has set of modes that
     *         it can be in.
     */
    public boolean hasModes() {
        return modes != null;
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
        return EquipmentType.lookupHash.get(key.toLowerCase().trim());
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

    public static int getArmorType(String inType) {
        EquipmentType et = EquipmentType.get(inType);
        if (et != null) {
            for (int x = 0; x < armorNames.length; x++) {
                if (armorNames[x].equals(et.getInternalName())) {
                    return x;
                }
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

    public static int getStructureType(String inType) {
        EquipmentType et = EquipmentType.get(inType);
        if (et != null) {
            for (int x = 0; x < structureNames.length; x++) {
                if (structureNames[x].equals(et.getInternalName())) {
                    return x;
                }
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

    /**
     * @return The C-Bill cost of the piece of equipment.
     */
    public double getCost(Entity entity, boolean isArmored) {
        return cost;
    }

    public static String getRatingName(int rating) {
        if ((rating < 0) || (rating > ratingNames.length)) {
            return "U";
        }
        return ratingNames[rating];
    }

    public int getTechRating() {
        return techRating;
    }

    public String getTechRatingName() {
        return EquipmentType.getRatingName(getTechRating());
    }

    public int getAvailability(int era) {
        if ((era < 0) || (era > ERA_CLAN)) {
            return RATING_X;
        }
        return availRating[era];
    }

    public String getAvailabilityName(int era) {
        int avail = getAvailability(era);
        return EquipmentType.getRatingName(avail);
    }

    public int getIntroductionDate() {
        return introDate;
    }

    public int getExtinctionDate() {
        return extinctDate;
    }

    public int getReintruductionDate() {
        return reintroDate;
    }

    public boolean isAvailableIn(int year) {
        if (year < introDate) {
            return false;
        }
        if ((extinctDate == DATE_NONE) || (year < extinctDate)) {
            return true;
        }
        if ((reintroDate == DATE_NONE) || (year < reintroDate)) {
            return false;
        }
        return true;
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
        return EquipmentType.getArmorPointMultiplier(inArmor, TechConstants.T_IS_TW_NON_BOX);
    }

    public static double getArmorPointMultiplier(int inArmor, int inTechLevel) {
        return EquipmentType.getArmorPointMultiplier(inArmor, ((inTechLevel == TechConstants.T_CLAN_TW) || (inTechLevel == TechConstants.T_CLAN_ADVANCED)) || (inTechLevel == TechConstants.T_CLAN_EXPERIMENTAL) || (inTechLevel == TechConstants.T_CLAN_UNOFFICIAL));
    }

    public static double getArmorPointMultiplier(int inArmor, boolean clanArmor) {
        if ((inArmor < 0) || (inArmor >= armorPointMultipliers.length)) {
            return POINT_MULTIPLIER_UNKNOWN;
        }
        if ((inArmor == T_ARMOR_FERRO_FIBROUS) && clanArmor) {
            return POINT_MULTIPLIER_CLAN_FF;
        }
        return armorPointMultipliers[inArmor];
    }

    /**
     * stuff like hatchets, which depend on an unknown quality (usually tonnage
     * of the unit.) entity is whatever has this item
     */
    public int resolveVariableCost(Entity entity, boolean isArmored) {
        int varCost = 0;
        if (this instanceof MiscType) {
            if (hasFlag(MiscType.F_MASC)) {
                if (entity instanceof Protomech) {
                    varCost = Math.round(entity.getEngine().getRating() * 1000 * entity.getWeight() * 0.025f);
                } else if (hasSubType(MiscType.S_SUPERCHARGER)) {
                    Engine e = entity.getEngine();
                    if (e == null) {
                        varCost = 0;
                    } else {
                        varCost = e.getRating() * 10000;
                    }
                } else {
                    int mascTonnage = 0;
                    if (getInternalName().equals("ISMASC")) {
                        mascTonnage = Math.round(entity.getWeight() / 20.0f);
                    } else if (getInternalName().equals("CLMASC")) {
                        mascTonnage = Math.round(entity.getWeight() / 25.0f);
                    }
                    varCost = entity.getEngine().getRating() * mascTonnage * 1000;
                }
            } else if (hasFlag(MiscType.F_TARGCOMP)) {
                int tCompTons = 0;
                float fTons = 0.0f;
                for (Mounted mo : entity.getWeaponList()) {
                    WeaponType wt = (WeaponType) mo.getType();
                    if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                        fTons += wt.getTonnage(entity);
                    }
                }
                if (getInternalName().equals("ISTargeting Computer")) {
                    tCompTons = (int) Math.ceil(fTons / 4.0f);
                } else if (getInternalName().equals("CLTargeting Computer")) {
                    tCompTons = (int) Math.ceil(fTons / 5.0f);
                }
                varCost = tCompTons * 10000;
            } else if (hasFlag(MiscType.F_CLUB) && (hasSubType(MiscType.S_HATCHET) || hasSubType(MiscType.S_MACE_THB))) {
                int hatchetTons = (int) Math.ceil(entity.getWeight() / 15.0);
                varCost = hatchetTons * 5000;
            } else if (hasFlag(MiscType.F_CLUB) && hasSubType(MiscType.S_SWORD)) {
                int swordTons = (int) Math.ceil(entity.getWeight() / 15.0);
                varCost = swordTons * 10000;
            } else if (hasFlag(MiscType.F_CLUB) && hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
                int bladeTons = (int) Math.ceil(0.5f + Math.ceil(entity.getWeight() / 20.0));
                varCost = (1 + bladeTons) * 10000;
            } else if (hasFlag(MiscType.F_TRACKS)) {
                varCost = (int) Math.ceil(500 * entity.getEngine().getRating() * entity.getWeight() / 75);
            } else if (hasFlag(MiscType.F_TALON)) {
                varCost = (int) Math.ceil(getTonnage(entity) * 300);
            }

        } else {
            if (varCost == 0) {
                // if we don't know what it is...
                System.out.println("I don't know how much " + name + " costs.");
            }
        }

        if (isArmored) {
            varCost += 150000 * getCriticals(entity);
        }
        return varCost;
    }

    public boolean equals(EquipmentType e) {
        try {
            if ((e != null) && internalName.equals(e.internalName)) {
                return true;
            }
        } catch (Exception ex) {
            System.err.println(name + "  does not have an internal name set!");
        }
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
            w.write("Type,Tech Base,Rules,Name,Aliases");
            w.newLine();
            for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
                EquipmentType type = e.nextElement();
                if (type instanceof AmmoType) {
                    w.write("A,");
                } else if (type instanceof WeaponType) {
                    w.write("W,");
                } else {
                    w.write("M,");
                }
                w.write(TechConstants.getTechName(type.getTechLevel()));
                w.write(",");
                w.write(TechConstants.getLevelName(type.getTechLevel()));
                w.write(",");
                for (Enumeration<String> names = type.getNames(); names.hasMoreElements();) {
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
            w.write("Type,Tech Base,Rules,Name,Tonnage,Crits,Cost,BV");
            w.newLine();
            for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
                EquipmentType type = e.nextElement();
                if (type instanceof AmmoType) {
                    w.write("A,");
                } else if (type instanceof WeaponType) {
                    w.write("W,");
                } else {
                    w.write("M,");
                }
                w.write(TechConstants.getTechName(type.getTechLevel()));
                w.write(",");
                w.write(TechConstants.getLevelName(type.getTechLevel()));
                w.write(",");
                w.write(type.getName());
                w.write(",");
                if (type.tonnage == EquipmentType.TONNAGE_VARIABLE) {
                    w.write("Variable");
                } else {
                    w.write(Float.toString(type.tonnage));
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
                    w.write(Double.toString(type.getCost(null, false)));
                }
                w.write(",");
                if (type.bv == EquipmentType.BV_VARIABLE) {
                    w.write("Variable");
                } else {
                    w.write(Double.toString(type.bv));
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
        return "EquipmentType: "+name;
    }

}
