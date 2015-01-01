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
      for (Enumeration i = options.getGroups(); i.hasMoreElements();) {
          IOptionGroup group = (IOptionGroup)i.nextElement();
          
          if ( !group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES) )
            continue;
            
          for (Enumeration j = group.getOptions(); j.hasMoreElements();) {
              IOption option = (IOption)j.nextElement();
              
              option.clearValue();
          }
      }
      
    }
    
    public int countAdvantages() {
      int count = 0;
      
      for (Enumeration i = options.getGroups(); i.hasMoreElements();) {
          IOptionGroup group = (IOptionGroup)i.nextElement();
          
          if ( !group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES) )
            continue;
            
          for (Enumeration j = group.getOptions(); j.hasMoreElements();) {
              IOption option = (IOption)j.nextElement();
              
              if ( option.booleanValue() )
                count++;
          }
      }
      
      return count;
    }

    /**
        Returns the LVL3 Rules "Pilot Advantages" this pilot has
    */
    public Enumeration getAdvantages() {
        for (Enumeration i = options.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = (IOptionGroup)i.nextElement();

            if ( group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES) )
                return group.getOptions();
        };

        // no pilot advantages -- return an empty Enumeration
        return new java.util.Vector().elements();
    };

    /**
        Returns a string of all the LVL3 Pilot Advantage "codes" for this pilot,
        using sep as the separator
    */
    public String getAdvantageList(String sep) {
      StringBuffer adv = new StringBuffer();
      
      if (null == sep) {
        sep = "";
      }
      
    for (Enumeration j = getAdvantages(); j.hasMoreElements();) {
        IOption option = (IOption)j.nextElement();

        if ( option.booleanValue() ) {
            if ( adv.length() > 0 ) {
                adv.append(sep);
            }

            adv.append(option.getName());
            if (option.getType() == IOption.STRING ||
                option.getType() == IOption.CHOICE) {
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
        else
            return s.substring(index + 1,s.length());
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
        double multiplier = 1.0;

        if (gunnery < 4) {
            multiplier += 0.20 * (4 - gunnery);
        } else {
            multiplier += 0.10 * (4 - gunnery);
        }
        
        multiplier += 0.05 * (5 - piloting);

        return multiplier;
    }
    
    public int modifyPhysicalDamagaForMeleeSpecialist() {
        if ( !getOptions().booleanOption("melee_specialist") )
            return 0;

        return 1;
    }

    /**
     * Determine if this pilot has abandoned her vehicle.
     *
     * @return  <code>true</code> if the pilot has abandoned her vehicle,
     *          <code>false</code> if the pilot is still in the vehicle.
     */
    public boolean isEjected()
    {
        return this.ejected;
    }

    /**
     * Specify if this pilot has abandoned her vehicle.
     *
     * @param   abandoned the <code>boolean</code> value to set.
     */
    public void setEjected( boolean abandoned )
    {
        this.ejected = abandoned;
    }
}
