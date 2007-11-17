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

import megamek.common.options.IOptionGroup;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;

public class Pilot
    implements Serializable
{
    private String      name;
    private int         gunnery;
    private int         piloting;
    private int         hits; // hits taken
      
    private boolean     unconscious;
    private boolean     doomed;  // scheduled to die at end of phase
    private boolean     dead;
    private boolean     ejected;
    
    // these are only used on the server:
    private int rollsNeeded; // how many KO rolls needed this turn
    private boolean koThisRound; // did I go KO this game round?
    
    private PilotOptions options = new PilotOptions();

    /** The number of hits that a pilot can take before he dies. */
    static public final int DEATH       = 6;
    
    public Pilot() {
        this("Unnamed", 4, 5);
    }
  
    public Pilot(String name, int gunnery, int piloting) {
        this.name = name;
        this.gunnery = gunnery;
        this.piloting = piloting;
        hits = 0;
        unconscious = false;
        dead = false;
        rollsNeeded = 0;
        koThisRound = false;
        
        options.initialize();
    }
  
    public String getName() {
        return name;
    }
  
    public int getGunnery() {
        return gunnery;
    }
  
    public int getPiloting() {
        return piloting;
    }
  
    public int getHits() {
        return hits;
    }
    
    public void setGunnery(int gunnery) {
        this.gunnery = gunnery;
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
                rollsNeeded = 0;
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
                rollsNeeded = 0;
                hits = 6;
            }
        }
    }
    
    public boolean isActive() {
        return !unconscious && !dead;
    }
    
    public int getRollsNeeded() {
        // Ejected pilots don't need to roll.
        if (!ejected) {
            return rollsNeeded;
        }
        return 0;
    }
    
    public void setRollsNeeded(int rollsNeeded) {
        // Ejected pilots stop taking hits.
        if (!ejected) {
            if ( !doomed ) {
                this.rollsNeeded = rollsNeeded;
            }
        }
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
      for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
          IOptionGroup group = i.nextElement();
          
          if ( !group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES) )
            continue;
            
          for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
              IOption option = j.nextElement();
              
              option.clearValue();
          }
      }
      
    }
    
    public int countAdvantages() {
      int count = 0;
      
      for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
          IOptionGroup group = i.nextElement();
          
          if ( !group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES) )
            continue;
            
          for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
              IOption option = j.nextElement();
              
              if ( option.booleanValue() )
                count++;
          }
      }
      
      return count;
    }

    /**
        Returns the LVL3 Rules "Pilot Advantages" this pilot has
    */
    public Enumeration<IOption> getAdvantages() {
        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if ( group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES) )
                return group.getOptions();
        }

        // no pilot advantages -- return an empty Enumeration
        return new Vector<IOption>().elements();
    }

    /**
        Returns a string of all the LVL3 Pilot Advantage "codes" for this pilot,
        using sep as the separator
    */
    public String getAdvantageList(String sep) {
      StringBuffer adv = new StringBuffer();
      
      if (null == sep) {
        sep = "";
      }
      
    for (Enumeration<IOption> j = getAdvantages(); j.hasMoreElements();) {
        IOption option = j.nextElement();

        if ( option.booleanValue() ) {
            if ( adv.length() > 0 ) {
                adv.append(sep);
            }

            adv.append(option.getName());
            if (option.getType() == IOption.STRING ||
                option.getType() == IOption.CHOICE ||
                option.getType() == IOption.INTEGER ) {
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
        return s.substring(0,index);
    }

    // Helper function to reverse getAdvantageList() above
    public static Object parseAdvantageValue(String s) {
        s = s.trim();
        int index = s.indexOf(" ");
        if (index == -1)
            return new Boolean(true);
		String t = s.substring(index + 1,s.length());
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
        }
        else if (isUnconscious()) {
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
       return getBVSkillMultiplier(gunnery, piloting);
    }
    
    /**
     * Returns the BV multiplyer for a pilots gunnery/piloting.
     * This function is static to evaluate the BV of a unit, even
     * when they have not yet been assinged a pilot.
     * 
     * @param gunnery the gunnery skill of the pilot
     * @param piloting the piloting skill of the pilot
     * @return a multiplier to the BV of whatever unit the pilot is piloting.
     */
    public static double getBVSkillMultiplier(int gunnery, int piloting) {
        double pilotingMod = 1.0;
        double gunneryMod = 1.0;

        switch (gunnery) {
        case 0:
            gunneryMod += 0.82;
            break;
        case 1:
        case 2:
        case 3:
        case 4:
            gunneryMod += 0.2 * (4 - gunnery);
            break;
        case 5:
        case 6:
        case 7:
        case 8:
            gunneryMod += 0.05 * (3 - gunnery);
        }
        switch (piloting) {
        case 0:
        case 1:
        case 2:
        case 3:
            pilotingMod += 0.1 * (6 - piloting);
            break;
        case 4:
            pilotingMod += 0.15;
            break;
        case 5:
        case 6:
        case 7:
        case 8:
            pilotingMod += 0.05 * (5 - piloting);
            break;
        }
        return Math.round(gunneryMod * pilotingMod * 100) / 100.0;
    }
    
    public int modifyPhysicalDamagaForMeleeSpecialist() {
        if ( !getOptions().booleanOption("melee_specialist") )
            return 0;

        return 1;
    }

    public boolean hasEdgeRemaining() {    
        return (getOptions().intOption("edge") > 0); 
    }
    
    public void decreaseEdge() {
        IOption edgeOption = getOptions().getOption("edge");
        edgeOption.setValue((Integer)edgeOption.getValue() - 1);
    }
    
    /**
     * Determine if this pilot has abandoned her vehicle.
     *
     * @return  <code>true</code> if the pilot has abandoned her vehicle,
     *          <code>false</code> if the pilot is still in the vehicle.
     */
    public boolean isEjected() {
        return this.ejected;
    }

    /**
     * Specify if this pilot has abandoned her vehicle.
     *
     * @param   abandoned the <code>boolean</code> value to set.
     */
    public void setEjected( boolean abandoned ) {
        this.ejected = abandoned;
    }
}
