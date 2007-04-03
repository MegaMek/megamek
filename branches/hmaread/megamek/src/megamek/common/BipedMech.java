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
    
    public BipedMech(String inGyroType, String inCockpitType) {
        this(getGyroTypeForString(inGyroType), getCockpitTypeForString(inCockpitType));
    }

    public BipedMech() {
        this(Mech.GYRO_STANDARD, Mech.COCKPIT_STANDARD);
    }

    public BipedMech(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);

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
                        if (game == null || !game.getOptions().booleanOption("maxtech_leg_damage")) {
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
            if (hipHits > 0) {
               if (game != null && game.getOptions().booleanOption("maxtech_leg_damage")) {
                 wmp = (hipHits >= 1) ? wmp - (2 * hipHits) : 0;
               } else {
                 wmp = (hipHits == 1) ? (int) Math.ceil( wmp / 2.0) : 0;
               }
            }
            wmp -= actuatorHits;
        }
        
        if (hasShield()) {
            wmp -= getNumberOfShields(MiscType.S_SHIELD_LARGE);
            wmp -= getNumberOfShields(MiscType.S_SHIELD_MEDIUM);
        }

        // and we still need to factor in heat!
        wmp -= (heat / 5);
        
        // TSM negates some heat
        if (heat >= 9 && hasTSM()) {
            wmp += 2;
        }
        
        wmp = Math.max(wmp - getCargoMpReduction(), 0);
        
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
        if (countBadLegs() == 0) {
            return super.getRunMP(gravity);
        }
		return getWalkMP(gravity);
    }

    /**
     * Returns run MP without considering MASC modified for leg loss & stuff.
     */
       
    public int getRunMPwithoutMASC(boolean gravity) {
        if (countBadLegs() == 0) {
            return super.getRunMPwithoutMASC(gravity);
        }
		return getWalkMP(gravity);
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

    protected double getArmActuatorCost() {
        double cost = 0;
        int numOfUpperArmActuators=0;
        int numOfLowerArmActuators=0;
        int numOfHands=0;
        if ( hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM) ) {
            numOfHands++;
        }
        if ( hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM) ) {
            numOfLowerArmActuators++;
        }
        if ( hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM) ) {
            numOfUpperArmActuators++;
        }
        if ( hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM) ) {
            numOfHands++;
        }
        if ( hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM) ) {
            numOfLowerArmActuators++;
        }
        if ( hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM) ) {
            numOfUpperArmActuators++;
        }
        cost += numOfUpperArmActuators * weight * 100;
        cost += numOfLowerArmActuators * weight * 50;
        cost += numOfHands * weight * 80;
        return cost;
    }

    protected double getLegActuatorCost() {
        return weight * 150 * 2 + weight * 80 * 2 + weight * 120 * 2;
    }

    public double getArmorWeight() {
        //this roundabout method is actually necessary to avoid rounding weirdness.  Yeah, it's dumb.
        double armorPerTon = 16.0*EquipmentType.getArmorPointMultiplier(armorType,techLevel);
        double weight=0.0;
        for(;((int)Math.round(weight*armorPerTon))<getTotalOArmor();weight+=.5) {}
        return weight;
    }
   
    /**
     * Check to see if a Biped mech has a claw in one of its arms
     * @param location (LOC_RARM or LOC_LARM)
     * @return True/False
     */
    public boolean hasClaw(int location){
        //only arms have claws.
        if ( location != Mech.LOC_RARM && location != Mech.LOC_LARM )
            return false;
        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = this.getCritical(location,slot);
            
            if ( cs == null )
                continue;
            if ( cs.getType() != CriticalSlot.TYPE_EQUIPMENT )
                continue;
            Mounted m = this.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if (type instanceof MiscType && type.hasFlag(MiscType.F_HAND_WEAPON)
                    && type.hasSubType(MiscType.S_CLAW)) {
                return !(m.isDestroyed() || m.isMissing() || m.isBreached());
            }
        }
        return false;
    }
    
    /**
     * Checks to see if this bipmech has any vibro blades on them.
     * 
     *  @return boolean <code>true</code> if the mech has vibroblades <code>false</code> if not.
     */
    public boolean hasVibroblades(){
        int count = 0;
        
        if ( this.hasVibrobladesInLocation(Mech.LOC_RARM) )
            count++;
        if ( this.hasVibrobladesInLocation(Mech.LOC_LARM) )
            count++;
        
        return count > 0;
    }
    
    /**
     * Checks to see if this bipedmech has a vibroblade in this location.
     * @param location
     * @return boolean <code>true</code> if the mech has vibroblades <code>false</code> if not.
     */
    public boolean hasVibrobladesInLocation(int location){
        
        //Only arms have VibroBlades.
        if ( location != Mech.LOC_RARM && location != Mech.LOC_LARM )
            return false;

        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = this.getCritical(location,slot);
            
            if ( cs == null )
                continue;
            if ( cs.getType() != CriticalSlot.TYPE_EQUIPMENT )
                continue;
            Mounted m = this.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if (type instanceof MiscType && ((MiscType)type).isVibroblade()) {
                return !(m.isDestroyed() || m.isMissing() || m.isBreached());
            }
        }
        
        return false;
    }

    /**
     * Checks for Active Vibroblades in <code>location</code> and returns the amount
     * of heat genereated if active.
     * @param location
     * @return <code>int</code> amount of heat genereated by an active vibroblade.
     */
    public int getActiveVibrobladeHeat(int location){
        //Only arms have VibroBlades.
        if ( location != Mech.LOC_RARM && location != Mech.LOC_LARM )
            return 0;

        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = this.getCritical(location,slot);
            
            if ( cs == null )
                continue;
            if ( cs.getType() != CriticalSlot.TYPE_EQUIPMENT )
                continue;
            Mounted m = this.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if (type instanceof MiscType && ((MiscType)type).isVibroblade()
                    && m.curMode().equals("Active")
                    && !(m.isDestroyed() || m.isMissing() || m.isBreached())) {
                MiscType blade = (MiscType)type;
                if ( blade.hasSubType(MiscType.S_VIBRO_LARGE) )
                    return 7;
                if ( blade.hasSubType(MiscType.S_VIBRO_MEDIUM) )
                    return 5;
                //else
                return 3;
            }
        }
        
        return 0;
        
    }
}
