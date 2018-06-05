/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.UUID;
import java.util.Vector;

import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;

/**
 *  Health status, skills, and miscellanea for an Entity crew.
 * 
 *  While vehicle and vessel crews are treated as a single collective, with one set of skills,
 *  some multi-crew cockpits (Tripod, QuadVee, dual, command console) require tracking the health
 *  and skills of each crew member independently. These are referred to as "slots" and the slot
 *  number corresponds to an array index for the appropriate field.
 *
 */

public class Crew implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -141169182388269619L;
    
    private final CrewType crewType;
    private int size;

    private final String[] name;
    private final int[] gunnery;
    private final int[] piloting;
    private final int[] hits; // hits taken

    private final String[] nickname;

    private final String[] externalId;

    private final boolean[] unconscious;
    private final boolean[] dead;
    //Allow for the possibility that the unit is fielded with less than full crew.
    private final boolean[] missing;
    
    //The following only apply to the entire crew.
    private boolean doomed; // scheduled to die at end of phase
    private boolean ejected;

    // StratOps fatigue points
    private int fatigue;
    //also need to track turns for fatigue by pilot because some may have later deployment
    private int fatigueCount;
    
    /**
     * Additional RPG Skills **
     */
    // MW3e uses 3 different gunnery skills
    private final int[] gunneryL;
    private final int[] gunneryM;
    private final int[] gunneryB;

    // Separate artillery skill
    private final int[] artillery;

    // init bonuses
    // bonus for individual initiative
    private int initBonus;
    // commander bonus
    private int commandBonus;

    // a toughness bonus that is applied to all KO checks
    private final int[] toughness;
    
    private int pilotPos;
    private int gunnerPos;
    
    //Designate the slot index of the crew member that will fill in if the pilot or gunner is incapacitated.
    //This is only relevant for superheavy tripods, as other types have at most a single other option.
    private int backupPilot;
    private int backupGunner;

    //For cockpit command console, we need to track which crew members have acted as the pilot, making them
    //ineligible to provide command bonus the next round.
    private final boolean[] actedThisTurn;
    //The crew slots in a command console can swap roles in the end phase of any turn.
    private boolean swapConsoleRoles;

    /**
     * End RPG Skills **
     */

    // these are only used on the server:
    private final boolean[] koThisRound; // did I go KO this game round?

    //TODO: Allow individual crew to have SPAs, which involves determining which work for individuals
    //and which work for the entire unit.
    private PilotOptions options = new PilotOptions();

    // pathway to pilot portrait
    public static final String ROOT_PORTRAIT = "-- General --";
    public static final String PORTRAIT_NONE = "None";
    private final String[] portraitCategory;
    private final String[] portraitFileName;

    //SPA RangeMaster range bands
    public static final String RANGEMASTER_NONE = "None";
    public static final String RANGEMASTER_MEDIUM = "Medium";
    public static final String RANGEMASTER_LONG = "Long";
    public static final String RANGEMASTER_EXTREME = "Extreme";
    public static final String RANGEMASTER_LOS = "Line of Sight";

    // SPA Human TRO entity types
    public static final String HUMANTRO_NONE = "None";
    public static final String HUMANTRO_MECH = "Mek";
    public static final String HUMANTRO_AERO = "Aero";
    public static final String HUMANTRO_VEE = "Vee";
    public static final String HUMANTRO_BA = "BA";

    public static final String SPECIAL_NONE = "None";
    public static final String SPECIAL_ENERGY = "Energy";
    public static final String SPECIAL_BALLISTIC = "Ballistic";
    public static final String SPECIAL_MISSILE = "Missile";

    private static double[][] bvMod = new double[][]
            {
                    {2.8, 2.63, 2.45, 2.28, 2.01, 1.82, 1.75, 1.67, 1.59},
                    {2.56, 2.4, 2.24, 2.08, 1.84, 1.60, 1.58, 1.51, 1.44},
                    {2.24, 2.1, 1.96, 1.82, 1.61, 1.4, 1.33, 1.31, 1.25},
                    {1.92, 1.8, 1.68, 1.56, 1.38, 1.2, 1.14, 1.08, 1.06},
                    {1.6, 1.5, 1.4, 1.3, 1.15, 1.0, 0.95, 0.9, 0.85},
                    {1.50, 1.35, 1.26, 1.17, 1.04, 0.90, 0.86, 0.81, 0.77},
                    {1.43, 1.33, 1.19, 1.11, 0.98, 0.85, 0.81, 0.77, 0.72},
                    {1.36, 1.26, 1.16, 1.04, 0.92, 0.80, 0.76, 0.72, 0.68},
                    {1.28, 1.19, 1.1, 1.01, 0.86, 0.75, 0.71, 0.68, 0.64},
            };
    private static double[][] alternateBvMod = new double[][]{
            {2.70, 2.52, 2.34, 2.16, 1.98, 1.80, 1.75, 1.67, 1.59},
            {2.40, 2.24, 2.08, 1.98, 1.76, 1.60, 1.58, 1.51, 1.44},
            {2.10, 1.96, 1.82, 1.68, 1.54, 1.40, 1.33, 1.31, 1.25},
            {1.80, 1.68, 1.56, 1.44, 1.32, 1.20, 1.14, 1.08, 1.06},
            {1.50, 1.40, 1.30, 1.20, 1.10, 1.00, 0.95, 0.90, 0.85},
            {1.50, 1.35, 1.26, 1.17, 1.04, 0.90, 0.86, 0.81, 0.77},
            {1.43, 1.33, 1.19, 1.11, 0.98, 0.85, 0.81, 0.77, 0.72},
            {1.36, 1.26, 1.16, 1.04, 0.92, 0.80, 0.76, 0.72, 0.68},
            {1.28, 1.19, 1.10, 1.01, 0.86, 0.75, 0.71, 0.68, 0.64},
    };

    /**
     * The number of hits that a pilot can take before he dies.
     */
    static public final int DEATH = 6;

    /**
     * Defines the maximum value a Crew can have in any skill
     */
    static public final int MAX_SKILL = 8;
    
    /**
     * Creates a nameless P5/G4 crew of the given size.
     *
     * @param size the crew size.
     */
    public Crew(CrewType crewType) {
        this(crewType, "Unnamed", crewType.getCrewSlots(), 4, 5);
    }

    /**
     * @deprecated by multi-crew cockpit support. Replaced by {@link #Crew(CrewType, String, int, int, int)}.
     * 
     * Creates a basic crew for a self-piloted unit. Using this constructor for a naval vessel will
     * result in a secondary target modifier for additional targets past the first.
     * 
     * @param name
     * @param size
     * @param gunnery
     * @param piloting
     */
    @Deprecated
    public Crew(String name, int size, int gunnery, int piloting) {
        this(CrewType.SINGLE, name, size, gunnery, gunnery, gunnery, piloting);
    }

    /**
     * @param crewType the type of crew
     * @param name     the name of the crew or commander.
     * @param size     the crew size.
     * @param gunnery  the crew's Gunnery skill.
     * @param piloting the crew's Piloting or Driving skill.
     */
    public Crew(CrewType crewType, String name, int size, int gunnery, int piloting) {
        this(crewType, name, size, gunnery, gunnery, gunnery, piloting);
    }

    /**
     * @param crewType the type of crew.
     * @param name     the name of the crew or commander.
     * @param size     the crew size.
     * @param gunneryL the crew's "laser" Gunnery skill.
     * @param gunneryM the crew's "missile" Gunnery skill.
     * @param gunneryB the crew's "ballistic" Gunnery skill.
     * @param piloting the crew's Piloting or Driving skill.
     */
    public Crew(CrewType crewType, String name, int size, int gunneryL, int gunneryM, int gunneryB,
            int piloting) {
        this.crewType = crewType;
        this.size = Math.max(size, crewType.getCrewSlots());

        int slots = crewType.getCrewSlots();
        this.name = new String[slots];
        Arrays.fill(this.name, name);
        this.nickname = new String[slots];
        Arrays.fill(this.nickname, "");

        int avGunnery = (int) Math.round((gunneryL + gunneryM + gunneryB) / 3.0);
        this.gunnery = new int[slots];
        Arrays.fill(this.gunnery, avGunnery);
        this.gunneryL = new int[slots];
        Arrays.fill(this.gunneryL, gunneryL);
        this.gunneryB = new int[slots];
        Arrays.fill(this.gunneryB, gunneryB);
        this.gunneryM = new int[slots];
        Arrays.fill(this.gunneryM, gunneryM);
        this.artillery = new int[slots];
        Arrays.fill(this.artillery, avGunnery);
        this.piloting = new int[slots];
        Arrays.fill(this.piloting, piloting);
        
        initBonus = 0;
        commandBonus = 0;
        hits = new int[slots];
        unconscious = new boolean[slots];
        dead = new boolean[slots];
        missing = new boolean[slots];
        koThisRound = new boolean[slots];
        fatigue = 0;
        toughness = new int[slots];
        portraitCategory = new String[slots];
        Arrays.fill(portraitCategory, ROOT_PORTRAIT);
        portraitFileName = new String[slots];
        Arrays.fill(portraitFileName, PORTRAIT_NONE);

        options.initialize();

        pilotPos = crewType.getPilotPos();
        gunnerPos = crewType.getGunnerPos();
        
        //For 2-slot crews, this will designate the other crew member as backup. For superheavy tripods,
        //this will designate the pilot and gunner as backups for each other.
        if (getSlotCount() > 1) {
            backupPilot = 1 - pilotPos;
            backupGunner = 1 - gunnerPos;
        }
        actedThisTurn = new boolean[slots];
        resetActedFlag();
        
        //set a random UUID for external ID, this will help us sort enemy salvage and prisoners in MHQ
        //and should have no effect on MM (but need to make sure it doesnt screw up MekWars)
        externalId = new String[slots];
        for (int i = 0; i < slots; i++) {
            externalId[i] = UUID.randomUUID().toString();
        }
    }

    public String getName() {
        return name[0];
    }
    
    public String getName(int pos) {
        return name[pos];
    }
    
    public String getNickname() {
        return nickname[0];
    }
    
    public String getNickname(int pos) {
        return nickname[pos];
    }
    
    /**
     * @param pos The slot index for multi-crewed cockpits
     * @return    For multi-slot crews, the crew member's name followed by the role. For-slot crews, the
     *            crew name only.
     */
    public String getNameAndRole(int pos) {
        if (getSlotCount() < 2) {
            return getName(pos);
        }
        return getName(pos) + " (" + crewType.getRoleName(pos) + ")";
    }

    /**
     * The size of this crew.
     *
     * @return the number of crew members.
     */
    public int getSize() {
        return size;
    }
    
    public CrewType getCrewType() {
        return crewType;
    }
    
    /**
     * @return The number of crew members that are tracked individually
     */
    public int getSlotCount() {
        return crewType.getCrewSlots();
    }

    public int getGunnery() {
        return gunnery[gunnerPos];
    }

    public int getGunnery(int pos) {
        return gunnery[pos];
    }
    
    public int getGunneryL() {
        return gunneryL[gunnerPos];
    }

    public int getGunneryL(int pos) {
        return gunneryL[pos];
    }
    
    public int getGunneryM() {
        return gunneryM[gunnerPos];
    }

    public int getGunneryM(int pos) {
        return gunneryM[pos];
    }
    
    public int getGunneryB() {
        return gunneryB[gunnerPos];
    }
    
    public int getGunneryB(int pos) {
        return gunneryB[pos];
    }
    
    public int getArtillery() {
        return artillery[gunnerPos];
    }
    
    public int getArtillery(int pos) {
        return artillery[pos];
    }
    
    public int getPiloting() {
        return piloting[pilotPos];
    }
    
    public int getPiloting(int pos) {
        return piloting[pos];
    }

    /**
     * LAMs use a different skill in AirMech mode depending on whether they are grounded or airborne.
     */
    public int getPiloting(EntityMovementType moveType) {
        return piloting[pilotPos];
    }

    /**
     * @return a String showing the overall skills in the format gunnery/piloting
     */
    public String getSkillsAsString() {
        return getSkillsAsString(true);
    }
    
    /**
     * @param showPiloting if false, only the gunnery skill is shown (used for protomechs; may be ignored
     *                     for other unit types)
     * @return a String showing the overall skills in the format gunnery/piloting
     */
    public String getSkillsAsString(boolean showPiloting) {
        StringBuilder sb = new StringBuilder();
        sb.append(getGunnery());
        if (showPiloting) {
            sb.append("/").append(getPiloting());
        }
        return sb.toString();
    }
    
    /**
     * @return a String showing the skills for a particular slot in the format gunnery/piloting
     */
    public String getSkillsAsString(int pos) {
        return getSkillsAsString(pos, true);
    }
    
    /**
     * @param showPiloting if false, only the gunnery skill is shown (used for protomechs; may be ignored
     *                     for other unit types)
     * @return a String showing the skills for a particular slot in the format gunnery/piloting
     */
    public String getSkillsAsString(int pos, boolean showPiloting) {
        StringBuilder sb = new StringBuilder();
        sb.append(getGunnery(pos));
        if (showPiloting) {
            sb.append("/").append(getPiloting(pos));
        }
        return sb.toString();
    }
    
    /**
     * Used to determine whether the death threshold has been passed. As the crew is not dead until
     * each crew member slot is dead, we return the lowest value.
     * 
     * @return The damage level of the least damaged crew member.
     */
    //TODO: The boarding actions rules reflect casualties to overall crew size by tracking hits.
    public int getHits() {
        return Arrays.stream(hits).min().orElse(0);
    }

    public int getHits(int pos) {
        return hits[pos];
    }

    public int getInitBonus() {
        return initBonus;
    }

    public int getCommandBonus() {
        return commandBonus;
    }

    public void setName(String name, int pos) {
        this.name[pos] = name;
    }

    public void setNickname(String nick, int pos) {
        nickname[pos] = nick;
    }

    /**
     * Accessor method to set the crew size.
     *
     * @param newSize The new size of this crew.
     */
    public void setSize(int newSize) {
        size = newSize;
    }

    public void setGunnery(int gunnery, int pos) {
        this.gunnery[pos] = gunnery;
    }

    public void setGunneryL(int gunnery, int pos) {
        gunneryL[pos] = gunnery;
    }

    public void setGunneryM(int gunnery, int pos) {
        gunneryM[pos] = gunnery;
    }

    public void setGunneryB(int gunnery, int pos) {
        gunneryB[pos] = gunnery;
    }

    public void setArtillery(int artillery, int pos) {
        this.artillery[pos] = artillery;
    }

    public void setPiloting(int piloting, int pos) {
        this.piloting[pos] = piloting;
    }

    public void setHits(int hits, int pos) {
        // Ejected pilots stop taking hits.
        if (!ejected && !missing[pos]) {
            this.hits[pos] = hits;
            if (hits >= DEATH) {
                setDead(true, pos);
            }
        }
    }
    
    public void setInitBonus(int bonus) {
        initBonus = bonus;
    }

    public void setCommandBonus(int bonus) {
        commandBonus = bonus;
    }
    
    /**
     * The crew is considered unconscious as a whole if none are active and at least one is not dead.
     * @return Whether at least one crew member is alive but none are conscious.
     */
    public boolean isUnconscious() {
        return !isDead() && !isActive();
    }

    public boolean isUnconscious(int pos) {
        return unconscious[pos] && !dead[pos] && hits[pos] < DEATH;
    }
    
    public void setUnconscious(boolean unconscious) {
        for (int i = 0; i < getSlotCount(); i++) {
            setUnconscious(unconscious, i);
        }
    }

    public void setUnconscious(boolean unconscious, int pos) {
        this.unconscious[pos] = unconscious;
        if (getSlotCount() > 1) {
            activeStatusChanged();
        }
    }

    /**
     * The crew is considered dead as a whole if all members are dead.
     * @return Whether all members of the crew are dead.
     */
    public boolean isDead() {
        for (int i = 0; i < this.getSlotCount(); i++) {
            if (!dead[i]) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isDead(int pos) {
        return dead[pos];
    }
    
    public void setDead(boolean dead) {
        if (!ejected) {
            for (int i = 0; i < getSlotCount(); i++) {
                setDead(dead, i);
            }
        }
    }

    public void setDead(boolean dead, int pos) {
        // Ejected pilots stop taking hits.
        if (!ejected && !missing[pos]) {
            this.dead[pos] = dead;
            if (dead) {
                hits[pos] = 6;
            }
            if (getSlotCount() > 1) {
                activeStatusChanged();
            }
        }
    }
    
    /**
     * @return Whether the unit was fielded without a crew member in the slot.
     */
    public boolean isMissing(int pos) {
        return missing[pos];
    }
    
    /**
     * Allows a unit with a multi-crew cockpit to fielded with less than a full crew. Does not apply
     * to collective crew (vehicles, infantry, large craft). 
     */
    public void setMissing(boolean missing, int pos) {
        this.missing[pos] = missing;
        activeStatusChanged();
    }

    /**
     * Doomed status only applies to the crew as a whole.
     * @return Whether the crew is scheduled to die at the end of the phase.
     */
    public boolean isDoomed() {
        return doomed;
    }

    /**
     * Doomed status only applies to the crew as a whole.
     * @param doomed Whether the crew is scheduled to die at the end of the phase.
     */
    public void setDoomed(boolean doomed) {
        // Ejected pilots stop taking hits.
        if (!ejected) {
            this.doomed = doomed;
            if (doomed) {
                for (int i = 0; i < getSlotCount(); i++) {
                    hits[i] = 6;
                }
            }
        }
    }

    /**
     * The crew as a whole is considered active if any member is active.
     * @return Whether the crew has at least one active member.
     */
    public boolean isActive() {
        for (int i = 0; i < getSlotCount(); i++) {
            if (isActive(i)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isActive(int pos) {
        return !unconscious[pos] && !dead[pos] && !missing[pos];
    }
    
    /**
     * The crew as a whole is considered ko this round if all active members are ko this round.
     * @return
     */
    public boolean isKoThisRound() {
        for (int i = 0; i < getSlotCount(); i++) {
            if (isActive(i) && !koThisRound[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean isKoThisRound(int pos) {
        return koThisRound[pos];
    }
    
    /**
     * Set ko value for all slots.
     * 
     * @param koThisRound Whether the crew will go unconscious during this round.
     */
    public void setKoThisRound(boolean koThisRound) {
        Arrays.fill(this.koThisRound, koThisRound);
    }

    public void setKoThisRound(boolean koThisRound, int pos) {
        this.koThisRound[pos] = koThisRound;
    }

    public void setOptions(PilotOptions options) {
        this.options = options;
    }

    public PilotOptions getOptions() {
        return options;
    }

    public void clearOptions() {
        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                option.clearValue();
            }
        }

    }

    public void clearOptions(String grpKey) {
        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(grpKey)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                option.clearValue();
            }
        }

    }

    public int countOptions() {
        return options.count();
    }

    public int countOptions(String grpKey) {
        return options.count(grpKey);
    }

    /**
     * Returns the options of the given category that this pilot has
     */
    public Enumeration<IOption> getOptions(String grpKey) {
        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(grpKey)) {
                return group.getOptions();
            }
        }

        // no pilot advantages -- return an empty Enumeration
        return new Vector<IOption>().elements();
    }

    /**
     * Returns a string of all the option "codes" for this pilot, for a given
     * group, using sep as the separator
     */
    public String getOptionList(String sep, String grpKey) {
        return options.getOptionListString(sep, grpKey);
    }

    // Helper function to reverse getAdvantageList() above
    public static String parseAdvantageName(String s) {
        s = s.trim();
        int index = s.indexOf(" ");
        if (index == -1) {
            index = s.length();
        }
        return s.substring(0, index);
    }

    // Helper function to reverse getAdvantageList() above
    public static Object parseAdvantageValue(String s) {
        s = s.trim();
        int index = s.indexOf(" ");
        if (index == -1) {
            return new Boolean(true);
        }
        String t = s.substring(index + 1, s.length());
        Object result;
        try {
            result = Integer.valueOf(t);
        } catch (NumberFormatException e) {
            result = t;
        } // try-catch
        return result;
    }

    /**
     * Overall crew description, using the name of the first crew member in the case of multi-crew cockpits.
     */
    public String getDesc() {
        return getDesc(0);
    }
    
    public String getDesc(int pos) {
        if (isMissing(pos)) {
            return "[missing]";
        }
        String s = new String(name[pos]);
        if (hits[pos] > 0) {
            s += " (" + hits[pos] + " hit(s)";
            if (isUnconscious(pos)) {
                s += " [ko]";
            } else if (isDead(pos)) {
                s += " [dead]";
            }
            s += ")";
        } else if (isUnconscious(pos)) {
            s += " [ko]";
        } else if (isDead(pos)) {
            s += " [dead]";
        }
        return s;
    }

    /**
     * Crew summary report used for victory phase.
     * 
     * @param gunneryOnly Do not show the piloting skill
     */
    public Vector<Report> getDescVector(boolean gunneryOnly) {
        Vector<Report> vDesc = new Vector<Report>();
        Report r;

        for (int i = 0; i < getSlotCount(); i++) {
            if (missing[i]) {
                continue;
            }
            r = new Report();
            r.type = Report.PUBLIC;
            r.add(name[i]);
            if (getSlotCount() > 1) {
                r.add(" (" + crewType.getRoleName(i) + ")");
            }
            if (gunneryOnly) {
                r.messageId = 7050;
                r.add(getGunnery(i));
            } else {
                r.messageId = 7045;
                r.add(getGunnery(i));
                r.add(getPiloting(i));
            }
    
            if ((hits[i] > 0) || isUnconscious(i) || isDead(i)) {
                Report r2 = new Report();
                r2.type = Report.PUBLIC;
                if (hits[i] > 0) {
                    r2.messageId = 7055;
                    r2.add(hits[i]);
                    if (isUnconscious(i)) {
                        r2.messageId = 7060;
                        r2.choose(true);
                    } else if (isDead(i)) {
                        r2.messageId = 7060;
                        r2.choose(false);
                    }
                } else if (isUnconscious(i)) {
                    r2.messageId = 7065;
                    r2.choose(true);
                } else if (isDead(i)) {
                    r2.messageId = 7065;
                    r2.choose(false);
                }
                r.newlines = 0;
                vDesc.addElement(r);
                vDesc.addElement(r2);
            } else {
                vDesc.addElement(r);
            }
        }
        return vDesc;
    }

    /**
     * Returns whether this pilot has non-standard piloting or gunnery values
     */
    public boolean isCustom() {
        return (getGunnery() != 4) || (getPiloting() != 5);
    }

    /**
     * Returns the BV multiplier for this pilot's gunnery/piloting
     *
     * @param game
     */
    public double getBVSkillMultiplier(IGame game) {
        return getBVSkillMultiplier(true, game);
    }

    /**
     * Returns the BV multiplier for this pilot's gunnery/piloting
     *
     * @param usePiloting whether or not to use the default value non-anti-mech
     *                    infantry/BA should not use the anti-mech skill
     * @param game
     */
    public double getBVSkillMultiplier(boolean usePiloting, IGame game) {
        int pilotVal = getPiloting();
        if (!usePiloting) {
            pilotVal = 5;
        }
        return getBVImplantMultiplier() * getBVSkillMultiplier(getGunnery(), pilotVal, game);
    }

    public double getBVImplantMultiplier() {

        // get highest level
        int level = 1;
        if (options.booleanOption(OptionsConstants.MD_PAIN_SHUNT)) {
            level = 2;
        }
        if (options.booleanOption(OptionsConstants.MD_VDNI)) {
            level = 3;
        }
        if (options.booleanOption(OptionsConstants.MD_BVDNI)) {
            level = 5;
        }

        double mod = (level / 4.0) + 0.75;

        return mod;

    }

    /**
     * Returns the BV multiplyer for a pilots gunnery/piloting. This function is
     * static to evaluate the BV of a unit, even when they have not yet been
     * assinged a pilot.
     *
     * @param gunnery  the gunnery skill of the pilot
     * @param piloting the piloting skill of the pilot
     * @return a multiplier to the BV of whatever unit the pilot is piloting.
     */
    public static double getBVSkillMultiplier(int gunnery, int piloting) {
        return getBVSkillMultiplier(gunnery, piloting, null);
    }

    public static double getBVSkillMultiplier(int gunnery, int piloting, IGame game) {
        if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVANCED_ALTERNATE_PILOT_BV_MOD)) {
            return alternateBvMod[Math.max(Math.min(8, gunnery), 0)][Math.max(Math.min(8, piloting), 0)];
        }
        return bvMod[Math.max(Math.min(8, gunnery), 0)][Math.max(Math.min(8, piloting), 0)];
    }

    public int modifyPhysicalDamagaForMeleeSpecialist() {
        if (!getOptions().booleanOption(OptionsConstants.PILOT_MELEE_SPECIALIST)) {
            return 0;
        }

        return 1;
    }

    public boolean hasEdgeRemaining() {
        return (getOptions().intOption(OptionsConstants.EDGE) > 0);
    }

    public void decreaseEdge() {
        IOption edgeOption = getOptions().getOption(OptionsConstants.EDGE);
        edgeOption.setValue((Integer) edgeOption.getValue() - 1);
    }

    /**
     * Determine if this pilot has abandoned her vehicle.
     *
     * @return <code>true</code> if the pilot has abandoned her vehicle,
     * <code>false</code> if the pilot is still in the vehicle.
     */
    public boolean isEjected() {
        return ejected;
    }

    /**
     * Specify if this pilot has abandoned her vehicle.
     *
     * @param abandoned the <code>boolean</code> value to set.
     */
    public void setEjected(boolean abandoned) {
        ejected = abandoned;
    }

    /**
     * @return a string description of the gunnery skills when using RPG
     */
    public String getGunneryRPG() {
        return "" + gunneryL + "(L)/" + gunneryM + "(M)/" + gunneryB + "(B)";
    }

    /**
     * for sensor ops, so these might be easily expanded later for rpg
     */
    public int getSensorOps() {
        if (getPiloting() > -1) {
            return getPiloting();
        }
        return getGunnery();
    }

    private int getPilotingFatigueTurn() {
        int turn = 20;
        if (getPiloting() > 5) {
            turn = 10;
        } else if (getPiloting() > 3) {
            turn = 14;
        } else if (getPiloting() > 1) {
            turn = 17;
        }

        // get fatigue point modifiers
        int mod = (int) Math.min(Math.max(0, Math.ceil(fatigue / 4.0) - 1), 4);
        turn = turn - mod;

        return turn;
    }

    public boolean isPilotingFatigued() {
        return fatigueCount >= getPilotingFatigueTurn();
    }

    private int getGunneryFatigueTurn() {
        int turn = 20;
        if (getPiloting() > 5) {
            turn = 14;
        } else if (getPiloting() > 3) {
            turn = 17;
        }

        // get fatigue point modifiers
        int mod = (int) Math.min(Math.max(0, Math.ceil(fatigue / 4.0) - 1), 4);
        turn = turn - mod;

        return turn;

    }

    public boolean isGunneryFatigued() {
        if (getPiloting() < 2) {
            return false;
        }
        return fatigueCount >= getGunneryFatigueTurn();
    }

    /**
     * @return A description of the status of crew as a whole
     */
    public String getStatusDesc() {
        String s = new String("");
        if (getHits() > 0) {
            s += getHits() + " hits";
            if (isUnconscious()) {
                s += " (KO)";
            } else if (isDead()) {
                s += " (dead)";
            }
        }
        return s;
    }

    /**
     * @return A description of the status of a single crew member
     */
    public String getStatusDesc(int pos) {
        if (isMissing(pos)) {
            return "Missing";
        }
        String s = new String("");
        if (getHits(pos) > 0) {
            s += hits[pos] + " hits";
            if (isUnconscious(pos)) {
                s += " (KO)";
            } else if (isDead(pos)) {
                s += " (dead)";
            }
        }
        return s;
    }

    public void setExternalIdAsString(String i, int pos) {
        externalId[pos] = i;
    }

    public void setExternalId(int i, int pos) {
        externalId[pos] = Integer.toString(i);
    }

    public String getExternalIdAsString(int pos) {
        return externalId[pos];
    }
    
    /**
     * Use the first assigned slot as a general id for the crew.
     * @return The id of the first slot that is not set to "-1" 
     */
    public String getExternalIdAsString() {
        for (int i = 0; i < getSlotCount(); i++) {
            if (!externalId[i].equals("-1")) {
                return externalId[i];
            }
        }
        return "-1";
    }

    public int getExternalId(int pos) {
        return Integer.parseInt(externalId[pos]);
    }

    public void setPortraitCategory(String name, int pos) {
        portraitCategory[pos] = name;
    }

    public String getPortraitCategory(int pos) {
        return portraitCategory[pos];
    }

    public void setPortraitFileName(String name, int pos) {
        portraitFileName[pos] = name;
    }

    public String getPortraitFileName(int pos) {
        return portraitFileName[pos];
    }

    public int getToughness(int pos) {
        return toughness[pos];
    }

    public void setToughness(int t, int pos) {
        toughness[pos] = t;
    }

    public int getFatigue() {
        return fatigue;
    }

    public void setFatigue(int i) {
        fatigue = i;
    }

    public void incrementFatigueCount() {
        fatigueCount++;
    }
    
    /**
     * Sets fatigue counter back to zero.
     */
    public void resetFatigue() {
        fatigueCount = 0;
    }

    public int rollGunnerySkill() {
        if (getOptions().booleanOption(OptionsConstants.PILOT_APTITUDE_GUNNERY)) {
            return Compute.d6(3, 2);
        }

        return Compute.d6(2);
    }

    public int rollPilotingSkill() {
        if (getOptions().booleanOption(OptionsConstants.PILOT_APTITUDE_PILOTING)) {
            return Compute.d6(3, 2);
        }

        return Compute.d6(2);
    }
    
    public int getCurrentPilotIndex() {
        return pilotPos;
    }
    
    public int getCurrentGunnerIndex() {
        return gunnerPos;
    }
    
    public int getBackupPilotPos() {
        return backupPilot;
    }
    
    public void setBackupPilotPos(int pos) {
        backupPilot = pos;
    }
    
    public int getBackupGunnerPos() {
        return backupGunner;
    }
    
    public void setBackupGunnerPos(int pos) {
        backupGunner = pos;
    }
    
    /**
     * Set the pilot slot. If a multicrew cockpit uses the same crew member as both pilot and gunner
     * (i.e. cockpit command console), sets the gunner as well.
     * 
     * @param pos The slot index to set as pilot.
     */
    public void setCurrentPilot(int pos) {
        pilotPos = pos;
        if (crewType.getPilotPos() == crewType.getGunnerPos()) {
            gunnerPos = pos;
        }
        actedThisTurn[pos] = true;
    }
    
    /**
     * Set the gunner slot. If a multicrew cockpit uses the same crew member as both pilot and gunner
     * (i.e. cockpit command console), sets the pilot as well.
     * 
     * @param pos The slot index to set as gunner.
     */
    public void setCurrentGunner(int pos) {
        gunnerPos = pos;
        if (crewType.getPilotPos() == crewType.getGunnerPos()) {
            pilotPos = pos;
        }
        actedThisTurn[pos] = true;
    }
    
    /**
     * Called when the active status of a crew slot changes in a unit with a multi-crew cockpit.
     * If a pilot or gunner is incapacitated, someone else must take over the duties.
     * A pilot or gunner that wakes up will resume normal duties.
     */
    private void activeStatusChanged() {
        //Cockpit command console can be swapped deliberately by the player and should not be changed
        //automatically unless the current pilot becomes inactive.
        if (crewType.equals(CrewType.COMMAND_CONSOLE)
                && isActive(getCurrentPilotIndex())) {
            return;
        }
        //Start by checking whether the default pilot is available. If not, check the designated backup.
        //If still not available, select the first active slot. If none are active, it does not matter
        //which slot is designated and the value is not changed.
        if (isActive(crewType.getPilotPos())) {
            pilotPos = crewType.getPilotPos();
        } else if (isActive(backupPilot)) {
            pilotPos = backupPilot;
        } else {
            for (int i = 0; i < getSlotCount(); i++) {
                if (isActive(i)) {
                    pilotPos = i;
                    break;
                }
            }
        }
        if (isActive(crewType.getGunnerPos())) {
            gunnerPos = crewType.getGunnerPos();
        } else if (isActive(backupGunner)) {
            gunnerPos = backupGunner;
        } else {
            for (int i = 0; i < getSlotCount(); i++) {
                if (isActive(i)) {
                    gunnerPos = i;
                    break;
                }
            }
        }
    }
    
    /**
     * When assigning skills randomly, we want to make sure the skills are assigned to the most
     * appropriate position in crews where the pilot and gunner are separate.
     * We're going to do it the simpler way and reassign the piloting and gunnery
     * skills individually, resulting in a more specialized crew.
     */
    public void sortRandomSkills() {
        if (crewType.getCrewSlots() < 2 || crewType.getPilotPos() == crewType.getGunnerPos()) {
            return;
        }
        int lowest = MAX_SKILL;
        int pos = -1;
        if (!missing[crewType.getPilotPos()]) {
            for (int i = 0; i < getSlotCount(); i++) {
                if (piloting[i] < lowest) {
                    lowest = piloting[i];
                    pos = i;
                }
            }
            if (pos >= 0) {
                piloting[pos] = piloting[crewType.getPilotPos()];
                piloting[crewType.getPilotPos()] = lowest;
            }
        }
        lowest = MAX_SKILL;
        pos = -1;
        if (!missing[crewType.getGunnerPos()]) {
            for (int i = 0; i < getSlotCount(); i++) {
                if (gunnery[i] < lowest) {
                    lowest = gunnery[i];
                    pos = i;
                }
            }
            if (pos >= 0) {
                gunnery[pos] = gunnery[crewType.getGunnerPos()];
                gunnery[crewType.getGunnerPos()] = lowest;
            }
        }
    }
    
    /**
     * Tripods and QuadVees get special benefits if the dedicated pilot is active.
     * 
     * @return Whether a Mek has a separate pilot who is active. 
     */
    public boolean hasDedicatedPilot() {
        return isActive(crewType.getPilotPos())
                && crewType.getGunnerPos() != crewType.getPilotPos();
    }
    
    /**
     * Tripods and QuadVees get special benefits if the dedicated gunner is active.
     * 
     * @return Whether a Mek has a separate gunner who is active. 
     */
    public boolean hasDedicatedGunner() {
        return isActive(crewType.getGunnerPos())
                && crewType.getGunnerPos() != crewType.getPilotPos();
    }
    
    /**
     * Superheavy tripods gain benefits from having a technical officer.
     * 
     * @return Whether the tech officer is alive and conscious.
     */
    public boolean hasActiveTechOfficer() {
        return crewType.getTechPos() > 0 && isActive(crewType.getTechPos());
    }
    
    /**
     * Cockpit command console provides commander init bonus if both crew members are active
     * (also requires advanced fire control and heavy/assault unit, which is not checked here).
     * Though the positions are named "pilot" and "commander" they can switch positions in the end
     * phase of any turn so we need to check whichever is not currently acting as pilot.
     * 
     * @return Whether the unit has a commander that is not also acting as pilot currently or in the previous turn.
     */
    public boolean hasActiveCommandConsole() {
        int commandPos = 1 - getCurrentPilotIndex();
        return crewType.equals(CrewType.COMMAND_CONSOLE) && isActive(commandPos) && !actedThisTurn[commandPos];
    }
    
    /**
     * Called after the initiative bonus for the round has been calculated.
     */
    public void resetActedFlag() {
        Arrays.fill(actedThisTurn, false);
        actedThisTurn[getCurrentPilotIndex()] = true;
        actedThisTurn[getCurrentGunnerIndex()] = true;
    }
    
    /**
     * @return Whether the crew members in a command console-equipped unit are scheduled to swap roles at
     *         the end of the turn. 
     */
    public boolean getSwapConsoleRoles() {
        return swapConsoleRoles;
    }
    
    /**
     * Schedules or clears a scheduled swap of roles in a command console-equipped unit.
     * @param swap
     */
    public void setSwapConsoleRoles(boolean swap) {
        swapConsoleRoles = swap;
    }

    /**
     * Checks whether a role swap is scheduled for a command-console equipped unit and (if the new pilot
     * is active) performs the swap. The swap flag is cleared regardless of whether a swap took place.
     * 
     * @return True if a swap was performed, otherwise false.
     */
    public boolean doConsoleRoleSwap() {
        if (swapConsoleRoles && crewType.equals(CrewType.COMMAND_CONSOLE)) {
            int newPilotIndex = 1 - getCurrentPilotIndex();
            if (isActive(newPilotIndex)) {
                setCurrentPilot(newPilotIndex);
                swapConsoleRoles = false;
                return true;
            }
        }
        swapConsoleRoles = false;
        return false;
    }
    
    /*
     * Legacy methods used by MekWars 
     */
    
    /**
     * @deprecated by multi-crew cockpits. Replaced by {@link #setHits(int)}
     */  
    @Deprecated
    public void setHits(int hits) {
        setHits(hits, 0);
    }
    
    /**
     * Sets the piloting skill of the crew's default pilot.
     */  
    public void setPiloting(int piloting) {
        setPiloting(piloting, crewType.getPilotPos());
    }
    
    /**
     * Sets the gunnery skill of the crew's default gunner.
     */  
    public void setGunnery(int gunnery) {
        setGunnery(gunnery, crewType.getGunnerPos());
    }
}