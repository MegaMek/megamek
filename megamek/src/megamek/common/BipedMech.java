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

public class BipedMech extends Mech {
    public static final String[] LOCATION_NAMES = {"Head",
    "Center Torso", "Right Torso", "Left Torso",
    "Right Arm", "Left Arm", "Right Leg", "Left Leg"};
    
    public static final String[] LOCATION_ABBRS = {"HD", "CT", "RT",
    "LT", "RA", "LA", "RL", "LL"};
    
    private static final int[] NUM_OF_SLOTS = {6, 12, 12, 12, 12, 12, 6, 6};
    
    public BipedMech() {
        super();
        
        movementMode = IEntityMovementMode.BIPED;
        
        setCritical(LOC_RARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_RARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_RARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_RARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));
        
        setCritical(LOC_LARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_LARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_LARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_LARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));
    }
    
    /**
     * Returns true if the entity can flip its arms
     */
    public boolean canFlipArms() {
        boolean canFlip = !isProne();
        
        if ( hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM) ) {
            canFlip = false;
        } else if ( hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM) ) {
            canFlip = false;
        } else if ( hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM) ) {
            canFlip = false;
        } else if ( hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM) ) {
            canFlip = false;
        }
        
        return canFlip;
    }
    
    /**
     * Returns this entity's walking/cruising mp, factored
     * for heat, leg damage and gravity
     */
    
    public int getWalkMP() {
        return getWalkMP(true);
    }
    
    public int getWalkMP(boolean gravity) {
        int wmp = getOriginalWalkMP();
        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;
                
        for ( int i = 0; i < locations(); i++ ) {
            if ( locationIsLeg(i) ) {
                if ( !isLocationBad(i) ) {
                    if ( legHasHipCrit(i) ) {
                        hipHits++;
                        if (!game.getOptions().booleanOption("maxtech_leg_damage")) {
                            continue;
                        }
                    }
                    actuatorHits += countLegActuatorCrits(i);
                } else {
                    legsDestroyed++;
                }
            }
        }
        
        // leg damage effects
        if(legsDestroyed > 0) {
            wmp = (legsDestroyed == 1) ? 1 : 0;
        } else {
            if(hipHits > 0) {
               if (game.getOptions().booleanOption("maxtech_leg_damage")) {
                 wmp = (hipHits >= 1) ? wmp - (2 * hipHits) : 0;
               } else {
                 wmp = (hipHits == 1) ? (int) Math.ceil( (double) wmp / 2.0) : 0;
               }
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
        
        //gravity
        if (gravity) wmp = applyGravityEffectsOnMP(wmp);
        
        //For sanity sake...
        wmp = Math.max(0, wmp);
        
        return wmp;
    }
    
    /**
     * Returns this mech's running/flank mp modified for leg loss & stuff.
     */
    
    public int getRunMP(boolean gravity) {
        if ( countBadLegs() == 0 ) {
            return super.getRunMP(gravity);
        } else {
            return getWalkMP(gravity);
        }
    }

    /**
     * Returns run MP without considering MASC modified for leg loss & stuff.
     */
       
    public int getRunMPwithoutMASC(boolean gravity) {
        if ( countBadLegs() == 0 ) {
            return super.getRunMPwithoutMASC(gravity);
        } else {
            return getWalkMP(gravity);
        }
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
        initializeInternal(arm, LOC_RARM);
        initializeInternal(arm, LOC_LARM);
        initializeInternal(leg, LOC_RLEG);
        initializeInternal(leg, LOC_LLEG);
    }
    
    /**
     * Add in any piloting skill mods
     */
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        int[] locsToCheck = new int[2];

        locsToCheck[0] = Mech.LOC_RLEG;
        locsToCheck[1] = Mech.LOC_LLEG;
        
        for ( int i = 0; i < locsToCheck.length; i++ ) {
            int loc = locsToCheck[i];
            
            if (isLocationBad(loc)) {
                roll.addModifier(5, getLocationName(loc) + " destroyed");
            } else {
                // check for damaged hip actuators
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                    if (!game.getOptions().booleanOption("maxtech_leg_damage"))
                        continue;
                }
                // upper leg actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Upper Leg Actuator destroyed");
                }
                // lower leg actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Lower Leg Actuator destroyed");
                }
                // foot actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Foot Actuator destroyed");
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

    /**
     * @return The cost in C-Bills of the 'Mech in question.
     */
    public double getCost() {
        // FIXME
        // There should be an implementation here!
        int cost=0;
        cost+=200000;//cockpit
        cost+=50000;//life support
        cost+=weight*2000;//sensors
        int muscCost=this.hasTSM()? 160000 : 2000;
        cost+=muscCost*weight;//musculature
        int structureCost=400;
        if(hasEndo() || hasCompositeStructure()) {
            structureCost=1600;
        }
        if(hasReinforcedStructure()) {
            structureCost=6400;
        }
        cost+=structureCost*weight;//IS
        cost+=2*(150+80+120)*weight;//leg actuators;
        int numOfLowerArmActuators=0;
        int numOfHands=0;
        if ( hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM) ) {
            numOfHands++;
        }
        if ( hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM) ) {
            numOfLowerArmActuators++;
        }
        if ( hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM) ) {
            numOfHands++;
        }
        if ( hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM) ) {
            numOfLowerArmActuators++;
        }
        cost+=(2*100+numOfLowerArmActuators*50+numOfHands*80)*weight;//arm actuators
        int engineCost=5000;
        if(hasXL()) {
            engineCost=20000;
        }
        if(hasLightEngine()) {
            engineCost=15000;
        }
        cost+=(weight*getOriginalWalkMP()*weight*engineCost)/75;//(weight*walk=rating; rating*weight*cost factor = cost of engine.
        cost+=300000*getOriginalWalkMP()*weight;//gyro;
        int freeSinks= hasDoubleHeatSinks()? 0 : 10;//num of sinks we don't pay for
        int sinkCost= hasDoubleHeatSinks()? 6000: 2000;
        cost+=sinkCost*(heatSinks()-freeSinks);//cost of sinks
        double totalArmorWeight = (double) getTotalOArmor() / 16.0f;
        totalArmorWeight/=EquipmentType.getArmorPointMultiplier(armorType,techLevel);
        totalArmorWeight=(double)Math.ceil(totalArmorWeight*2f)/2f;
        cost+=totalArmorWeight*EquipmentType.getArmorCost(armorType);//armor
        cost+=getWeaponsAndEquipmentCost();
        if(isOmni()) {
            cost+=cost*0.25f;
        }
        cost*=(1+(weight/100f));
        return cost;
                       
    }
}
