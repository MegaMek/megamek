/*
 * MegaMek - 
 *  Copyright (C) 2000-2002 
 *    Ben Mazur (bmazur@sev.org)
 *    Cord Awtry (kipsta@bs-interactive.com)
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

public class QuadMech extends Mech
{
  private static final String[] LOCATION_NAMES = {"Head",
      "Center Torso", "Right Torso", "Left Torso", 
      "Front Right Leg", "Front Left Leg", "Rear Right Leg", "Rear Left Leg"};
  
  private static final String[] LOCATION_ABBRS = {"HD", "CT", "RT",
      "LT", "FRL", "FLL", "RRL", "RLL"};
  
  private static final int[] NUM_OF_SLOTS = {6, 12, 12, 12, 6, 6, 6, 6};
  
  public QuadMech() {
    super();
    
    movementType = Entity.MovementType.QUAD;

    setCritical(LOC_RARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
    setCritical(LOC_RARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
    setCritical(LOC_RARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
    setCritical(LOC_RARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));

    setCritical(LOC_LARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
    setCritical(LOC_LARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
    setCritical(LOC_LARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
    setCritical(LOC_LARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));
  }
  
  /**
   * Returns this entity's walking/cruising mp, factored
   * for heat and leg damage.
   */
    public int getWalkMP() {
      int wmp = getOriginalWalkMP();
      int legsDestroyed = 0;
      int hipHits = 0;
      int actuatorHits = 0;

      for ( int i = 0; i < locations(); i++ ) {    
        if ( locationIsLeg(i) ) {
          if ( !isLocationDestroyed(i) ) {
            if ( legHasHipCrit(i) )
              hipHits++;
            else {
              actuatorHits += countLegActuatorCrits(i);
            }
          } else {
            legsDestroyed++;
          }
        }
      }
      
      // leg damage effects
        if(legsDestroyed > 0) {
          if ( legsDestroyed == 1 )
            wmp--;
          else if ( legsDestroyed == 2 )
            wmp = 1;
          else
            wmp = 0;
        } 
        
        if ( wmp > 0 ) {
          for ( int i = 0; i < hipHits; i++ ) {
            wmp = (int)Math.ceil((double)wmp / 2.0);
          }

          wmp -= actuatorHits;
        }
      
      // and we still need to factor in heat!
        wmp -= (int)(heat / 5);
      
        
        // TSM negates some heat
        if (heat >= 9 && hasTSM()) {
            if (heat == 9) {
                wmp += 2;
            }
            else {
                wmp += 1;
            }
        }
        
      //For sanity sake...
        wmp = Math.max(0, wmp);
        
        return wmp;
    }

  /**
   * Returns this mech's running/flank mp modified for leg loss & stuff.
   */
    public int getRunMP() {
      if ( countDestroyedLegs() <= 1 ) {
        return super.getRunMP();
      } else {
        return getWalkMP();
      }
    }

    /**
     * Returns run MP without considering MASC modified for leg loss & stuff.
     */
    public int getRunMPwithoutMASC() {
        if ( countDestroyedLegs() <= 1 ) {
            return super.getRunMP();
        } else {
            return getWalkMP();
        }
    }

    public boolean canChangeSecondaryFacing() {
      return false;
    }
    
  /**
   * Returns true is the location is a leg
   */
    public boolean locationIsLeg(int loc) {
      return ((loc == Mech.LOC_RLEG) || (loc == Mech.LOC_LLEG) || (loc == Mech.LOC_RARM) || (loc == Mech.LOC_LARM));
    }
    
  /**
   * Sets the internal structure for the mech.
   * 
   * @param head head
   * @param ct center torso
   * @param t right/left torso
   * @param arm right/left arm
   * @param leg right/left leg
   */
    public void setInternal(int head, int ct, int t, int arm, int leg) {
      initializeInternal(head, LOC_HEAD);
      initializeInternal(ct, LOC_CT);
      initializeInternal(t, LOC_RT);
      initializeInternal(t, LOC_LT);
      initializeInternal(leg, LOC_RARM);
      initializeInternal(leg, LOC_LARM);
      initializeInternal(leg, LOC_RLEG);
      initializeInternal(leg, LOC_LLEG);
    }

  /**
   * Returns true is the entity needs a roll to stand up
   */
    public boolean needsRollToStand() {
      if ( countDestroyedLegs() == 0 )
        return false;
      
      return true;
    }

  /**
   * Add in any piloting skill mods
   */
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
      int[] locsToCheck = new int[4];
      int destroyedLegs = 0;
      
      locsToCheck = new int[4];
      locsToCheck[0] = Mech.LOC_RLEG;
      locsToCheck[1] = Mech.LOC_LLEG;
      locsToCheck[2] = Mech.LOC_RARM;
      locsToCheck[3] = Mech.LOC_LARM;
      
      destroyedLegs = countDestroyedLegs();

      if ( destroyedLegs == 0 )
        roll.addModifier(-2, "Quad bonus");
      
      for ( int i = 0; i < locsToCheck.length; i++ ) {
        int loc = locsToCheck[i];
        
        if (isLocationDestroyed(loc)) {
          if ( destroyedLegs > 1 )
            roll.addModifier(5, getLocationName(loc) + " destroyed");
        } else {
            // check for damaged hip actuators
            if (getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0) {
                roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
            } else {
                // upper leg actuators?
                if (getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Upper Leg Actuator destroyed");
                }
                // lower leg actuators?
                if (getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Lower Leg Actuator destroyed");
                }
                // foot actuators?
                if (getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Foot Actuator destroyed");
                }
            }
        }
      }

      return super.addEntityBonuses(roll);
    }
    
  /**
   * Returns a vector of slot counts for all locations
   */
    protected int[] getNoOfSlots() {
      return NUM_OF_SLOTS;
    }
    
  /**
   * Returns a vector of names for all locations
   */
    protected String[] getLocationNames() {
      return LOCATION_NAMES;
    }
    
  /**
   * Returns a vector of abbreviations for all locations
   */
    protected String[] getLocationAbbrs() {
      return LOCATION_ABBRS;
    }
    
}

