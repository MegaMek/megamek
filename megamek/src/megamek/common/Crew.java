/*
* MegaMek -
* Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.enums.Gender;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.util.CrewSkillSummaryUtil;

import java.io.Serializable;
import java.util.*;

/**
 * Health status, skills, and miscellanea for an Entity crew.
 *
 * While vehicle and vessel crews are treated as a single collective, with one set of skills,
 * some multi-crew cockpits (Tripod, QuadVee, dual, command console) require tracking the health
 * and skills of each crew member independently. These are referred to as "slots" and the slot
 * number corresponds to an array index for the appropriate field.
 */
public class Crew implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = -141169182388269619L;

    private Map<Integer, Map<String, String>> extraData;

    private final CrewType crewType;
    private int size;
    private int currentSize;

    private final String[] names;
    private final String[] nicknames;
    private final Gender[] genders;
    private final boolean[] clanPilots;
    private final Portrait[] portraits;

    private final int[] gunnery;
    private final int[] piloting;
    private final int[] hits; // hits taken

    private final String[] externalId;

    private final boolean[] unconscious;
    private final boolean[] dead;
    // Allow for the possibility that the unit is fielded with less than full crew.
    private final boolean[] missing;

    // The following only apply to the entire crew.
    private boolean doomed; // scheduled to die at end of phase
    private boolean ejected;

    // StratOps fatigue points
    private int fatigue;
    // also need to track turns for fatigue by pilot because some may have later deployment
    private int fatigueCount;

    //region RPG Skills
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

    // Designate the slot index of the crew member that will fill in if the pilot or gunner is incapacitated.
    // This is only relevant for superheavy tripods, as other types have at most a single other option.
    private int backupPilot;
    private int backupGunner;

    // For cockpit command console, we need to track which crew members have acted as the pilot,
    // making them ineligible to provide command bonus the next round.
    private final boolean[] actedThisTurn;
    // The crew slots in a command console can swap roles in the end phase of any turn.
    private boolean swapConsoleRoles;
    //endregion RPG Skills

    // these are only used on the server:
    private final boolean[] koThisRound; // did I go KO this game round?

    // TODO: Allow individual crew to have SPAs, which involves determining which work for individuals
    // and which work for the entire unit.
    private PilotOptions options = new PilotOptions();

    // SPA RangeMaster range bands
    public static final String RANGEMASTER_NONE = "None";
    public static final String RANGEMASTER_MEDIUM = "Medium";
    public static final String RANGEMASTER_LONG = "Long";
    public static final String RANGEMASTER_EXTREME = "Extreme";

    // SPA Human TRO entity types
    public static final String HUMANTRO_NONE = "None";
    public static final String HUMANTRO_MECH = "Mek";
    public static final String HUMANTRO_AERO = "Aero";
    public static final String HUMANTRO_VEE = "Vee";
    public static final String HUMANTRO_BA = "BA";

    // SPA Environmental Specialist types
    public static final String ENVSPC_NONE = "None";
    public static final String ENVSPC_FOG = "Fog";
    public static final String ENVSPC_HAIL = "Hail";
    public static final String ENVSPC_LIGHT = "Light";
    public static final String ENVSPC_RAIN = "Rain";
    public static final String ENVSPC_SNOW = "Snow";
    public static final String ENVSPC_WIND = "Wind";

    public static final String SPECIAL_NONE = "None";
    public static final String SPECIAL_ENERGY = "Energy";
    public static final String SPECIAL_BALLISTIC = "Ballistic";
    public static final String SPECIAL_MISSILE = "Missile";

    //region extraData inner map keys
    public static final String MAP_GIVEN_NAME = "givenName";
    public static final String MAP_SURNAME = "surname";
    public static final String MAP_BLOODNAME = "bloodname";
    public static final String MAP_PHENOTYPE = "phenotype";
    //endregion extraData inner map keys

    /**
     * The number of hits that a pilot can take before he dies.
     */
    public static final int DEATH = 6;

    /**
     * Defines the maximum value a Crew can have in any skill
     */
    public static final int MAX_SKILL = 8;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Creates a nameless P5/G4 crew of the given size.
     *
     * @param crewType the crew type to use.
     */
    public Crew(CrewType crewType) {
        this(crewType, "Unnamed", crewType.getCrewSlots(), 4, 5, Gender.FEMALE, false, null);
    }

    /**
     * @param crewType  the type of crew
     * @param name      the name of the crew or commander.
     * @param size      the crew size.
     * @param gunnery   the crew's Gunnery skill.
     * @param piloting  the crew's Piloting or Driving skill.
     * @param gender    the gender of the crew or commander
     * @param clanPilot   if the crew or commander is a clanPilot
     * @param extraData any extra data passed to be stored with this Crew.
     */
    public Crew(CrewType crewType, String name, int size, int gunnery, int piloting, Gender gender,
                boolean clanPilot, Map<Integer, Map<String, String>> extraData) {
        this(crewType, name, size, gunnery, gunnery, gunnery, piloting, gender, clanPilot, extraData);
    }

    /**
     * @param crewType  the type of crew.
     * @param name      the name of the crew or commander.
     * @param size      the crew size.
     * @param gunneryL  the crew's "laser" Gunnery skill.
     * @param gunneryM  the crew's "missile" Gunnery skill.
     * @param gunneryB  the crew's "ballistic" Gunnery skill.
     * @param piloting  the crew's Piloting or Driving skill.
     * @param gender    the gender of the crew or commander
     * @param clanPilot   if the crew or commander is a clanPilot
     * @param extraData any extra data passed to be stored with this Crew.
     */
    public Crew(CrewType crewType, String name, int size, int gunneryL, int gunneryM, int gunneryB,
                int piloting, Gender gender, boolean clanPilot,
                Map<Integer, Map<String, String>> extraData) {
        this.crewType = crewType;
        this.size = Math.max(size, crewType.getCrewSlots());
        this.currentSize = size;

        this.extraData = extraData;

        int slots = crewType.getCrewSlots();
        names = new String[slots];
        Arrays.fill(getNames(), name);
        nicknames = new String[slots];
        Arrays.fill(getNicknames(), "");
        genders = new Gender[slots];
        Arrays.fill(getGenders(), Gender.RANDOMIZE);
        setGender(gender, 0);
        clanPilots = new boolean[slots];
        Arrays.fill(getClanPilots(), clanPilot);
        portraits = new Portrait[slots];
        for (int i = 0; i < slots; i++) {
            setPortrait(new Portrait(), i);
        }

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
        // and should have no effect on MM (but need to make sure it doesn't screw up MekWars)
        externalId = new String[slots];
        for (int i = 0; i < slots; i++) {
            externalId[i] = UUID.randomUUID().toString();
        }
    }
    //endregion Constructors

    public String[] getNames() {
        return names;
    }

    public String getName() {
        return getNames()[0];
    }

    public String getName(final int pos) {
        return getNames()[pos];
    }

    public void setName(final String name, final int pos) {
        getNames()[pos] = name;
    }

    public String[] getNicknames() {
        return nicknames;
    }

    public String getNickname() {
        return getNicknames()[0];
    }

    public String getNickname(final int pos) {
        return getNicknames()[pos];
    }

    public void setNickname(final String nickname, final int pos) {
        getNicknames()[pos] = nickname;
    }

    public Gender[] getGenders() {
        return genders;
    }

    public Gender getGender() {
        return getGenders()[0];
    }

    public Gender getGender(final int pos) {
        // The randomize return value is used in MekHQ to create new personnel following a battle,
        // and should not be changed without ensuring it doesn't break on that side
        return (pos < getGenders().length) ? getGenders()[pos] : Gender.RANDOMIZE;
    }

    public void setGender(final Gender gender, final int pos) {
        getGenders()[pos] = gender;
    }

    public boolean[] getClanPilots() {
        return clanPilots;
    }

    public boolean isClanPilot() {
        return getClanPilots()[0];
    }

    public boolean isClanPilot(final int position) {
        return (position < getClanPilots().length) ? getClanPilots()[position] : isClanPilot();
    }

    public void setClanPilot(final boolean clanPilot, final int position) {
        getClanPilots()[position] = clanPilot;
    }

    public Portrait[] getPortraits() {
        return portraits;
    }

    public Portrait getPortrait(final int pos) {
        return getPortraits()[pos];
    }

    public void setPortrait(final Portrait portrait, final int pos) {
        getPortraits()[pos] = portrait;
    }

    /**
     * @param pos The slot index for multi-crewed cockpits
     * @return For multi-slot crews, the crew member's name followed by the role. For-slot crews, the
     * crew name only.
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
    
    /**
     * The currentsize of this crew.
     *
     * @return the current number of crew members.
     */
    public int getCurrentSize() {
        return currentSize;
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
    public String getSkillsAsString(boolean rpgSkills) {
        return getSkillsAsString(true, rpgSkills);
    }

    /**
     * @param showPiloting if false, only the gunnery skill is shown (used for protomechs; may be ignored
     *                     for other unit types)
     * @return a String showing the overall skills in the format gunnery/piloting
     */
    public String getSkillsAsString(boolean showPiloting, boolean rpgSkills) {
        if (showPiloting) {
            return CrewSkillSummaryUtil.getPilotSkillSummary(
                    getGunnery(),
                    getGunneryL(),
                    getGunneryM(),
                    getGunneryB(),
                    getPiloting(),
                    rpgSkills);
        } else {
            return CrewSkillSummaryUtil.getGunnerySkillSummary(
                    getGunnery(),
                    getGunneryL(),
                    getGunneryM(),
                    getGunneryB(),
                    rpgSkills);
        }
    }

    /**
     * @return a String showing the skills for a particular slot in the format gunnery/piloting
     */
    public String getSkillsAsString(int pos, boolean rpgSkills) {
        return getSkillsAsString(pos, true, rpgSkills);
    }

    /**
     * @param showPiloting if false, only the gunnery skill is shown (used for protomechs; may be ignored
     *                     for other unit types)
     * @return a String showing the skills for a particular slot in the format gunnery/piloting
     */
    public String getSkillsAsString(int pos, boolean showPiloting, boolean rpgSkills) {
        if (showPiloting) {
            return CrewSkillSummaryUtil.getPilotSkillSummary(
                    getGunnery(pos),
                    getGunneryL(pos),
                    getGunneryM(pos),
                    getGunneryB(pos),
                    getPiloting(pos),
                    rpgSkills);
        } else {
            return CrewSkillSummaryUtil.getGunnerySkillSummary(
                    getGunnery(pos),
                    getGunneryL(pos),
                    getGunneryM(pos),
                    getGunneryB(pos),
                    rpgSkills);
        }
    }

    /**
     * Used to determine whether the death threshold has been passed. As the crew is not dead until
     * each crew member slot is dead, we return the lowest value.
     *
     * @return The damage level of the least damaged crew member.
     */
    public int getHits() {
        return Arrays.stream(hits).min().orElse(0);
    }
    
    /**
     * Uses the table on TO p206 to calculate the number of crew hits based on percentage
     * of total casualties. Used for ejection, boarding actions and such
     * @return 
     */
    public int calculateHits() {
        if (currentSize == 0) {
            //100% casualties
            return 6;
        }
        double percentage = 1 - ((double) currentSize / (double) size);
        int hits = 0;
        if (percentage > 0.05 && percentage <= 0.20) {
            hits = 1;
        } else if (percentage > 0.20 && percentage <= 0.35) {
            hits = 2;
        } else if (percentage > 0.35 && percentage <= 0.50) {
            hits = 3;
        } else if (percentage > 0.50 && percentage <= 0.65) {
            hits = 4;
        } else if (percentage > 0.65 && percentage <= 0.80) {
            hits = 5;
        } else if (percentage > 0.80) {
            hits = 6;
        }
        return hits;
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

    /**
     * Accessor method to set the crew size.
     *
     * @param newSize The new size of this crew.
     */
    public void setSize(int newSize) {
        size = newSize;
    }
    
    /**
     * Accessor method to set the current crew size.
     *
     * @param newSize The new size of this crew.
     */
    public void setCurrentSize(int newSize) {
        currentSize = newSize;
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
     *
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
     *
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
     *
     * @return Whether the crew is scheduled to die at the end of the phase.
     */
    public boolean isDoomed() {
        return doomed;
    }

    /**
     * Doomed status only applies to the crew as a whole.
     *
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
     *
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
     *
     * @return true if all active members of the crew as knocked out this round
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
            return Boolean.TRUE;
        }
        String t = s.substring(index + 1);
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
        String s = getName(pos);
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
        Vector<Report> vDesc = new Vector<>();
        Report r;

        for (int i = 0; i < getSlotCount(); i++) {
            if (missing[i]) {
                continue;
            }
            r = new Report();
            r.type = Report.PUBLIC;
            r.add(getName(i));
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
     * @return A description of the status of a single crew member
     */
    public String getStatusDesc(int pos) {
        if (isMissing(pos)) {
            return "Missing";
        }
        String s = "";
        if (getHits(pos) > 0) {
            s += hits[pos] + ((hits[pos] == 1) ? " hit" : " hits");
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

    public String getExternalIdAsString(int pos) {
        if (pos < externalId.length) {
            return externalId[pos];
        } else {
            return "-1";
        }
    }

    /**
     * Use the first assigned slot as a general id for the crew.
     *
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
     * Sets crew state fields back to defaults. Used by MekHQ to clear game state.
     */
    public void resetGameState() {
        fatigueCount = 0;
        doomed = false;
        ejected = false;
        for (int i = 0; i < crewType.getCrewSlots(); i++) {
            unconscious[i] = false;
            dead[i] = false;
            missing[i] = false;
        }
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
     * Called when the active status of a crew slot changes in a unit with a multi-crew cockpit.
     * If a pilot or gunner is incapacitated, someone else must take over the duties.
     * A pilot or gunner that wakes up will resume normal duties.
     */
    private void activeStatusChanged() {
        //Cockpit command console can be swapped deliberately by the player and should not be changed
        // automatically unless the current pilot becomes inactive.
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
     * Super heavy tripods gain benefits from having a technical officer.
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
     * the end of the turn.
     */
    public boolean getSwapConsoleRoles() {
        return swapConsoleRoles;
    }

    /**
     * Schedules or clears a scheduled swap of roles in a command console-equipped unit.
     *
     * @param swap true for crew slots in a command console to swap roles at the end of the turn,
     *             otherwise false
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

    //region extraData
    public void setExtraData(Map<Integer, Map<String, String>> extraData) {
        this.extraData = extraData;
    }

    public void setExtraDataForCrewMember(int crewIndex, Map<String, String> dataMap) {
        if (this.extraData == null) {
            this.extraData = new HashMap<>();
        }

        this.extraData.put(crewIndex, dataMap);
    }

    public Map<Integer, Map<String, String>> getExtraData() {
        return extraData;
    }

    public Map<String, String> getExtraDataForCrewMember(int crewIndex) {
        if (this.extraData == null) {
            return null;
        } else {
            return extraData.get(crewIndex);
        }
    }

    public String getExtraDataValue(int crewIndex, String key) {
        if (this.extraData == null) {
            return null;
        } else if (this.extraData.get(crewIndex) == null) {
            return null;
        } else {
            return extraData.get(crewIndex).get(key);
        }
    }

    public String writeExtraDataToXMLLine(int pos) {
        Map<String, String> dataRow = getExtraDataForCrewMember(pos);

        if (dataRow != null) {
            StringBuilder output = new StringBuilder();

            output.append("\" extraData=\"");

            boolean first = true;
            for (Map.Entry<String, String> row : dataRow.entrySet()) {
                if (!first) {
                    output.append("|");
                } else {
                    first = false;
                }
                output.append(row.getKey()).append("=").append(row.getValue());
            }

            return output.toString();
        } else {
            return null;
        }
    }
    //endregion extraData

    //region MekWars
    /*
     * Legacy methods used by MekWars
     */
    /**
     * @deprecated by multi-crew cockpits. Replaced by {@link #setHits(int, int)}
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
    //endregion MekWars
}
