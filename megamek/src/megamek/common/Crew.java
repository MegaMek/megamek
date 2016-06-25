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
import java.util.Enumeration;
import java.util.UUID;
import java.util.Vector;

import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;

public class Crew implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -141169182388269619L;
    private String name;
    private int size;
    private int gunnery;
    private int piloting;
    private int hits; // hits taken

    private String nickname;

    private String externalId = "-1";

    private boolean unconscious;
    private boolean doomed; // scheduled to die at end of phase
    private boolean dead;
    private boolean ejected;

    // StratOps fatigue points
    private int fatigue;
    //also need to track turns for fatigue by pilot because some may have later deployment
    private int fatigueCount;

    /**
     * Additional RPG Skills **
     */
    // MW3e uses 3 different gunnery skills
    private int gunneryL;
    private int gunneryM;
    private int gunneryB;

    // Separate artillery skill
    private int artillery;

    // init bonuses
    // bonus for individual initiative
    private int initBonus;
    // commander bonus
    private int commandBonus;

    // a toughness bonus that is applied to all KO checks
    private int toughness;

    /**
     * End RPG Skills **
     */

    // these are only used on the server:
    private boolean koThisRound; // did I go KO this game round?

    private PilotOptions options = new PilotOptions();

    // pathway to pilot portrait
    public static final String ROOT_PORTRAIT = "-- General --";
    public static final String PORTRAIT_NONE = "None";
    private String portraitCategory = ROOT_PORTRAIT;
    private String portraitFileName = PORTRAIT_NONE;

    public static final String SPECIAL_NONE = "None";
    public static final String SPECIAL_LASER = "Laser";
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
    public Crew(int size) {
        this("Unnamed", 1, 4, 5);
    }

    /**
     * @param name     the name of the crew or commander.
     * @param size     the crew size.
     * @param gunnery  the crew's Gunnery skill.
     * @param piloting the crew's Piloting or Driving skill.
     */
    public Crew(String name, int size, int gunnery, int piloting) {
        this(name, size, gunnery, gunnery, gunnery, piloting);
    }

    /**
     * @param name     the name of the crew or commander.
     * @param size     the crew size.
     * @param gunneryL the crew's "laser" Gunnery skill.
     * @param gunneryM the crew's "missile" Gunnery skill.
     * @param gunneryB the crew's "ballistic" Gunnery skill.
     * @param piloting the crew's Piloting or Driving skill.
     */
    public Crew(String name, int size, int gunneryL, int gunneryM, int gunneryB, int piloting) {
        this.name = name;
        this.size = size;
        nickname = "";
        gunnery = (int) Math.round((gunneryL + gunneryM + gunneryB) / 3.0);
        this.gunneryL = gunneryL;
        this.gunneryM = gunneryM;
        this.gunneryB = gunneryB;
        artillery = gunnery;
        this.piloting = piloting;
        initBonus = 0;
        commandBonus = 0;
        hits = 0;
        unconscious = false;
        dead = false;
        koThisRound = false;
        fatigue = 0;

        options.initialize();
        
        //set a random UUID for external ID, this will help us sort enemy salvage and prisoners in MHQ
        //and should have no effect on MM (but need to make sure it doesnt screw up MekWars)
        externalId = UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public String getNickname() {
        return nickname;
    }

    /**
     * The size of this crew.
     *
     * @return the number of crew members.
     */
    public int getSize() {
        return size;
    }

    public int getGunnery() {
        return gunnery;
    }

    public int getGunneryL() {
        return gunneryL;
    }

    public int getGunneryM() {
        return gunneryM;
    }

    public int getGunneryB() {
        return gunneryB;
    }

    public int getArtillery() {
        return artillery;
    }

    public int getPiloting() {
        return piloting;
    }

    public int getHits() {
        return hits;
    }

    public int getInitBonus() {
        return initBonus;
    }

    public int getCommandBonus() {
        return commandBonus;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNickname(String nick) {
        nickname = nick;
    }

    /**
     * Accessor method to set the crew size.
     *
     * @param newSize The new size of this crew.
     */
    public void setSize(int newSize) {
        size = newSize;
    }

    public void setGunnery(int gunnery) {
        this.gunnery = gunnery;
    }

    public void setGunneryL(int gunnery) {
        gunneryL = gunnery;
    }

    public void setGunneryM(int gunnery) {
        gunneryM = gunnery;
    }

    public void setGunneryB(int gunnery) {
        gunneryB = gunnery;
    }

    public void setArtillery(int artillery) {
        this.artillery = artillery;
    }

    public void setPiloting(int piloting) {
        this.piloting = piloting;
    }

    public void setHits(int hits) {
        // Ejected pilots stop taking hits.
        if (!ejected) {
            this.hits = hits;
        }
    }

    public void setInitBonus(int bonus) {
        initBonus = bonus;
    }

    public void setCommandBonus(int bonus) {
        commandBonus = bonus;
    }

    public boolean isUnconscious() {
        return unconscious;
    }

    public void setUnconscious(boolean unconscious) {
        this.unconscious = unconscious;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        // Ejected pilots stop taking hits.
        if (!ejected) {
            this.dead = dead;
            if (dead) {
                hits = 6;
            }
        }
    }

    public boolean isDoomed() {
        return doomed;
    }

    public void setDoomed(boolean b) {
        // Ejected pilots stop taking hits.
        if (!ejected) {
            doomed = b;
            if (doomed) {
                hits = 6;
            }
        }
    }

    public boolean isActive() {
        return !unconscious && !dead;
    }

    public boolean isKoThisRound() {
        return koThisRound;
    }

    public void setKoThisRound(boolean koThisRound) {
        this.koThisRound = koThisRound;
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
        int count = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                if (option.booleanValue()) {
                    count++;
                }
            }
        }

        return count;
    }

    public int countOptions(String grpKey) {
        int count = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(grpKey)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                if (option != null && option.booleanValue()) {
                    count++;
                }
            }
        }

        return count;
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
        StringBuffer adv = new StringBuffer();

        if (null == sep) {
            sep = "";
        }

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            if (!group.getKey().equalsIgnoreCase(grpKey)) {
                continue;
            }
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                if ((option != null) && option.booleanValue()) {
                    if (adv.length() > 0) {
                        adv.append(sep);
                    }

                    adv.append(option.getName());
                    if ((option.getType() == IOption.STRING) || (option.getType() == IOption.CHOICE) || (option.getType() == IOption.INTEGER)) {
                        adv.append(" ").append(option.stringValue());
                    }
                }
            }
        }

        return adv.toString();
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

    public String getDesc() {
        String s = new String(name);
        if (hits > 0) {
            s += " (" + hits + " hit(s)";
            if (isUnconscious()) {
                s += " [ko]";
            } else if (isDead()) {
                s += " [dead]";
            }
            s += ")";
        } else if (isUnconscious()) {
            s += " [ko]";
        } else if (isDead()) {
            s += " [dead]";
        }
        return s;
    }

    public Vector<Report> getDescVector(boolean gunneryOnly) {
        Vector<Report> vDesc = new Vector<Report>();
        Report r;

        r = new Report();
        r.type = Report.PUBLIC;
        r.add(name);
        if (gunneryOnly) {
            r.messageId = 7050;
            r.add(getGunnery());
        } else {
            r.messageId = 7045;
            r.add(getGunnery());
            r.add(getPiloting());
        }

        if ((hits > 0) || isUnconscious() || isDead()) {
            Report r2 = new Report();
            r2.type = Report.PUBLIC;
            if (hits > 0) {
                r2.messageId = 7055;
                r2.add(hits);
                if (isUnconscious()) {
                    r2.messageId = 7060;
                    r2.choose(true);
                } else if (isDead()) {
                    r2.messageId = 7060;
                    r2.choose(false);
                }
            } else if (isUnconscious()) {
                r2.messageId = 7065;
                r2.choose(true);
            } else if (isDead()) {
                r2.messageId = 7065;
                r2.choose(false);
            }
            r.newlines = 0;
            vDesc.addElement(r);
            vDesc.addElement(r2);
        } else {
            vDesc.addElement(r);
        }
        return vDesc;
    }

    /**
     * Returns whether this pilot has non-standard piloting or gunnery values
     */
    public boolean isCustom() {
        return (gunnery != 4) || (piloting != 5);
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
        int pilotVal = piloting;
        if (!usePiloting) {
            pilotVal = 5;
        }
        return getBVImplantMultiplier() * getBVSkillMultiplier(gunnery, pilotVal, game);
    }

    public double getBVImplantMultiplier() {

        // get highest level
        int level = 1;
        if (options.booleanOption("pain_shunt")) {
            level = 2;
        }
        if (options.booleanOption("vdni")) {
            level = 3;
        }
        if (options.booleanOption("bvdni")) {
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
        if ((game != null) && game.getOptions().booleanOption("alternate_pilot_bv_mod")) {
            return alternateBvMod[Math.max(Math.min(8, gunnery), 0)][Math.max(Math.min(8, piloting), 0)];
        }
        return bvMod[Math.max(Math.min(8, gunnery), 0)][Math.max(Math.min(8, piloting), 0)];
    }

    public int modifyPhysicalDamagaForMeleeSpecialist() {
        if (!getOptions().booleanOption("melee_specialist")) {
            return 0;
        }

        return 1;
    }

    public boolean hasEdgeRemaining() {
        return (getOptions().intOption("edge") > 0);
    }

    public void decreaseEdge() {
        IOption edgeOption = getOptions().getOption("edge");
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
        if (piloting > -1) {
            return piloting;
        }
        return gunnery;
    }

    private int getPilotingFatigueTurn() {
        int turn = 20;
        if (piloting > 5) {
            turn = 10;
        } else if (piloting > 3) {
            turn = 14;
        } else if (piloting > 1) {
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
        if (piloting > 5) {
            turn = 14;
        } else if (piloting > 3) {
            turn = 17;
        }

        // get fatigue point modifiers
        int mod = (int) Math.min(Math.max(0, Math.ceil(fatigue / 4.0) - 1), 4);
        turn = turn - mod;

        return turn;

    }

    public boolean isGunneryFatigued() {
        if (piloting < 2) {
            return false;
        }
        return fatigueCount >= getGunneryFatigueTurn();
    }

    public String getStatusDesc() {
        String s = new String("");
        if (hits > 0) {
            s += hits + " hits";
            if (isUnconscious()) {
                s += " (KO)";
            } else if (isDead()) {
                s += " (dead)";
            }
        }
        return s;
    }

    public void setExternalIdAsString(String i) {
        externalId = i;
    }

    public void setExternalId(int i) {
        externalId = Integer.toString(i);
    }

    public String getExternalIdAsString() {
        return externalId;
    }

    public int getExternalId() {
        return Integer.parseInt(externalId);
    }

    public void setPortraitCategory(String name) {
        portraitCategory = name;
    }

    public String getPortraitCategory() {
        return portraitCategory;
    }

    public void setPortraitFileName(String name) {
        portraitFileName = name;
    }

    public String getPortraitFileName() {
        return portraitFileName;
    }

    public int getToughness() {
        return toughness;
    }

    public void setToughness(int t) {
        toughness = t;
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
}
