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
import java.util.Vector;

import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;

public class Pilot implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -141169182388269619L;
    private String name;
    private int gunnery;
    private int piloting;
    private int hits; // hits taken

    private boolean unconscious;
    private boolean doomed; // scheduled to die at end of phase
    private boolean dead;
    private boolean ejected;

    // RPG skills
    private int gunneryL;
    private int gunneryM;
    private int gunneryB;

    //init bonus
    //bonus for individual initiative
    private int initBonus;
    //commander bonus
    private int commandBonus;
    
    // these are only used on the server:
    private boolean koThisRound; // did I go KO this game round?

    private PilotOptions options = new PilotOptions();
    
    private static double[][] bvMod = new double[][] {
            {2.8,  2.63, 2.45, 2.28, 2.01, 1.82, 1.75, 1.67, 1.59},
            {2.56, 2.4,  2.24, 2.08, 1.84, 1.60, 1.58, 1.51, 1.44},
            {2.24, 2.1,  1.96, 1.82, 1.61, 1.4,  1.33, 1.31, 1.25},
            {1.92, 1.8,  1.68, 1.56, 1.38, 1.2,  1.14, 1.08, 1.06},
            {1.6,  1.5,  1.4,  1.3,  1.15, 1.0,  0.95, 0.9,  0.85},
            {1.50, 1.35, 1.26, 1.17, 1.04, 0.90, 0.86, 0.81, 0.77},
            {1.43, 1.33, 1.19, 1.11, 0.98, 0.85, 0.81, 0.77, 0.72},
            {1.36, 1.26, 1.16, 1.04, 0.92, 0.80, 0.76, 0.72, 0.68},
            {1.28, 1.19, 1.1,  1.01, 0.86, 0.75, 0.71, 0.68, 0.64},
    };

    /** The number of hits that a pilot can take before he dies. */
    static public final int DEATH = 6;

    public Pilot() {
        this("Unnamed", 4, 5);
    }

    public Pilot(String name, int gunnery, int piloting) {
        this.name = name;
        this.gunnery = gunnery;
        this.gunneryL = gunnery;
        this.gunneryM = gunnery;
        this.gunneryB = gunnery;
        this.piloting = piloting;
        this.initBonus = 0;
        this.commandBonus = 0;
        hits = 0;
        unconscious = false;
        dead = false;
        koThisRound = false;

        options.initialize();
    }

    public Pilot(String name, int gunneryL, int gunneryM, int gunneryB,
            int piloting) {
        this.name = name;
        this.gunnery = (int) Math.round((gunneryL + gunneryM + gunneryB) / 3.0);
        this.gunneryL = gunneryL;
        this.gunneryM = gunneryM;
        this.gunneryB = gunneryB;
        this.piloting = piloting;
        this.initBonus = 0;
        this.commandBonus = 0;
        hits = 0;
        unconscious = false;
        dead = false;
        koThisRound = false;

        options.initialize();
    }

    public String getName() {
        return name;
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
    
    public void setGunnery(int gunnery) {
        this.gunnery = gunnery;
    }

    public void setGunneryL(int gunnery) {
        this.gunneryL = gunnery;
    }

    public void setGunneryM(int gunnery) {
        this.gunneryM = gunnery;
    }

    public void setGunneryB(int gunnery) {
        this.gunneryB = gunnery;
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
        this.initBonus = bonus;
    }
    
    public void setCommandBonus(int bonus) {
        this.commandBonus = bonus;
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

    public void clearAdvantages() {
        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES))
                continue;

            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption option = j.nextElement();

                option.clearValue();
            }
        }

    }

    public int countAdvantages() {
        int count = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES))
                continue;

            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption option = j.nextElement();

                if (option.booleanValue())
                    count++;
            }
        }

        return count;
    }

    /**
     * Returns the LVL3 Rules "Pilot Advantages" this pilot has
     */
    public Enumeration<IOption> getAdvantages() {
        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES))
                return group.getOptions();
        }

        // no pilot advantages -- return an empty Enumeration
        return new Vector<IOption>().elements();
    }

    /**
     * Returns a string of all the LVL3 Pilot Advantage "codes" for this pilot,
     * using sep as the separator
     */
    public String getAdvantageList(String sep) {
        StringBuffer adv = new StringBuffer();

        if (null == sep) {
            sep = "";
        }

        for (Enumeration<IOption> j = getAdvantages(); j.hasMoreElements();) {
            IOption option = j.nextElement();

            if (option.booleanValue()) {
                if (adv.length() > 0) {
                    adv.append(sep);
                }

                adv.append(option.getName());
                if (option.getType() == IOption.STRING
                        || option.getType() == IOption.CHOICE
                        || option.getType() == IOption.INTEGER) {
                    adv.append(" ").append(option.stringValue());
                }
            }
        }

        return adv.toString();
    }

    public String getImplantList(String sep) {
        StringBuffer adv = new StringBuffer();

        if (null == sep) {
            sep = "";
        }

        for (Enumeration<IOption> j = getMDImplants(); j.hasMoreElements();) {
            IOption option = j.nextElement();

            if (option.booleanValue()) {
                if (adv.length() > 0) {
                    adv.append(sep);
                }

                adv.append(option.getName());
                if (option.getType() == IOption.STRING
                        || option.getType() == IOption.CHOICE
                        || option.getType() == IOption.INTEGER) {
                    adv.append(" ").append(option.stringValue());
                }
            }
        }

        return adv.toString();
    }

    // Helper function to reverse getAdvantageList() above
    public static String parseAdvantageName(String s) {
        s = s.trim();
        int index = s.indexOf(" ");
        if (index == -1)
            index = s.length();
        return s.substring(0, index);
    }

    // Helper function to reverse getAdvantageList() above
    public static Object parseAdvantageValue(String s) {
        s = s.trim();
        int index = s.indexOf(" ");
        if (index == -1)
            return new Boolean(true);
        String t = s.substring(index + 1, s.length());
        Object result;
        try {
            result = Integer.valueOf(t);
        } catch (NumberFormatException e) {
            result = t;
        } // try-catch
        return result;
    }

    public void clearMDImplants() {
        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PilotOptions.MD_ADVANTAGES))
                continue;

            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption option = j.nextElement();

                option.clearValue();
            }
        }

    }

    public int countMDImplants() {
        int count = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PilotOptions.MD_ADVANTAGES))
                continue;

            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption option = j.nextElement();

                if (option.booleanValue())
                    count++;
            }
        }

        return count;
    }

    /**
     * Returns the MD Implants this pilot has
     */
    public Enumeration<IOption> getMDImplants() {
        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(PilotOptions.MD_ADVANTAGES))
                return group.getOptions();
        }

        // no pilot advantages -- return an empty Enumeration
        return new Vector<IOption>().elements();
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

        if (hits > 0 || isUnconscious() || isDead()) {
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
        return gunnery != 4 || piloting != 5;
    }

    /**
     * Returns the BV multiplyer for this pilot's gunnery/piloting
     */
    public double getBVSkillMultiplier() {
        return getBVImplantMultiplier()
                * getBVSkillMultiplier(gunnery, piloting);
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
     * @param gunnery the gunnery skill of the pilot
     * @param piloting the piloting skill of the pilot
     * @return a multiplier to the BV of whatever unit the pilot is piloting.
     */
    public static double getBVSkillMultiplier(int gunnery, int piloting) {
        return bvMod[gunnery][piloting];
    }

    public int modifyPhysicalDamagaForMeleeSpecialist() {
        if (!getOptions().booleanOption("melee_specialist"))
            return 0;

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
     *         <code>false</code> if the pilot is still in the vehicle.
     */
    public boolean isEjected() {
        return this.ejected;
    }

    /**
     * Specify if this pilot has abandoned her vehicle.
     * 
     * @param abandoned the <code>boolean</code> value to set.
     */
    public void setEjected(boolean abandoned) {
        this.ejected = abandoned;
    }

    /**
     *  @return a string description of the gunnery skills when
     * using RPG
     */
    public String getGunneryRPG() {
        return "" + gunneryL + "(L)/" + gunneryM + "(M)/" + gunneryB + "(B)";
    }
    
    
    /**
     * for sensor ops, so these might be easily expanded later for rpg
     */
    public int getSensorOps() {
        if(piloting > -1) {
            return piloting;
        } else {
            return gunnery;
        }
    }
    
    public boolean isPilotingFatigued(int turn) {
        
        if(piloting > 5 && turn > 9) {
            return true;
        }
        if(piloting > 3 && turn > 13) {
            return true;
        }
        if(piloting > 1 && turn > 16) {
            return true;
        }
        if(turn > 19) {
            return true;
        }
        return false;
    }
    
    public boolean isGunneryFatigued(int turn) {
        
        if(piloting > 5 && turn > 13) {
            return true;
        }
        if(piloting > 3 && turn > 16) {
            return true;
        }
        if(piloting > 1 && turn > 19) {
            return true;
        }
        return false;
    }
    

}
