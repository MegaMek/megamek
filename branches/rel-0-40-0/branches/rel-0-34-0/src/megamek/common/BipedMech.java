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
    /**
     *
     */
    private static final long serialVersionUID = 4166375446709772785L;

    public static final String[] LOCATION_NAMES = { "Head", "Center Torso", "Right Torso", "Left Torso", "Right Arm", "Left Arm", "Right Leg", "Left Leg" };

    public static final String[] LOCATION_ABBRS = { "HD", "CT", "RT", "LT", "RA", "LA", "RL", "LL" };

    private static final int[] NUM_OF_SLOTS = { 6, 12, 12, 12, 12, 12, 6, 6 };

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
    @Override
    public boolean canFlipArms() {
        boolean canFlip = !isProne();

        if (hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
            canFlip = false;
        } else if (hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
            canFlip = false;
        } else if (hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
            canFlip = false;
        } else if (hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
            canFlip = false;
        }

        return canFlip;
    }

    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        int wmp = getOriginalWalkMP();
        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;

        for (int i = 0; i < locations(); i++) {
            if (locationIsLeg(i)) {
                if (!isLocationBad(i)) {
                    if (legHasHipCrit(i)) {
                        hipHits++;
                        if ((game == null) || !game.getOptions().booleanOption("tacops_leg_damage")) {
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
        if (legsDestroyed > 0) {
            wmp = (legsDestroyed == 1) ? 1 : 0;
        } else {
            if (hipHits > 0) {
                if ((game != null) && game.getOptions().booleanOption("tacops_leg_damage")) {
                    wmp = (hipHits >= 1) ? wmp - (2 * hipHits) : 0;
                } else {
                    wmp = (hipHits == 1) ? (int) Math.ceil(wmp / 2.0) : 0;
                }
            }
            wmp -= actuatorHits;
        }

        if (hasShield()) {
            wmp -= getNumberOfShields(MiscType.S_SHIELD_LARGE);
            wmp -= getNumberOfShields(MiscType.S_SHIELD_MEDIUM);
        }

        if ( hasModularArmor() ) {
            wmp--;
        }

        if (!ignoreheat) {
            // factor in heat
            if ((game != null) && game.getOptions().booleanOption("tacops_heat")) {
                if (heat < 30) {
                    wmp -= (heat / 5);
                } else if (heat >= 49) {
                    wmp -= 9;
                } else if (heat >= 43) {
                    wmp -= 8;
                } else if (heat >= 37) {
                    wmp -= 7;
                } else if (heat >= 31) {
                    wmp -= 6;
                } else {
                    wmp -= 5;
                }
            } else {
                wmp -= (heat / 5);
            }
            // TSM negates some heat
            if ((heat >= 9) && hasTSM()) {
                wmp += 2;
            }
        }
        wmp = Math.max(wmp - getCargoMpReduction(), 0);
        if(null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if(weatherMod != 0) {
                wmp = Math.max(wmp + weatherMod, 0);
            }
        }
        // gravity
        if (gravity) {
            wmp = applyGravityEffectsOnMP(wmp);
        }

        // For sanity sake...
        wmp = Math.max(0, wmp);

        return wmp;
    }

    /**
     * Returns this mech's running/flank mp modified for leg loss & stuff.
     */
    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat) {
        if (countBadLegs() == 0) {
            return super.getRunMP(gravity, ignoreheat);
        }
        return getWalkMP(gravity, ignoreheat);
    }

    /**
     * Returns run MP without considering MASC modified for leg loss & stuff.
     */

    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat) {
        if (countBadLegs() == 0) {
            return super.getRunMPwithoutMASC(gravity, ignoreheat);
        }
        return getWalkMP(gravity, ignoreheat);
    }

    /**
     * Sets the internal structure for the mech.
     *
     * @param head
     *            head
     * @param ct
     *            center torso
     * @param t
     *            right/left torso
     * @param arm
     *            right/left arm
     * @param leg
     *            right/left leg
     */
    @Override
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
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        int[] locsToCheck = new int[2];

        locsToCheck[0] = Mech.LOC_RLEG;
        locsToCheck[1] = Mech.LOC_LLEG;

        if ( hasFunctionalLegAES() ) {
            roll.addModifier(-2, "AES bonus");
        }

        for (int i = 0; i < locsToCheck.length; i++) {
            int loc = locsToCheck[i];

            if (isLocationBad(loc)) {
                roll.addModifier(5, getLocationName(loc) + " destroyed");
            } else {
                // check for damaged hip actuators
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                    if (!game.getOptions().booleanOption("tacops_leg_damage")) {
                        continue;
                    }
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
    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Returns a vector of names for all locations
     */
    @Override
    protected String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Returns a vector of abbreviations for all locations
     */
    @Override
    protected String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    protected double getArmActuatorCost() {
        double cost = 0;
        int numOfUpperArmActuators = 0;
        int numOfLowerArmActuators = 0;
        int numOfHands = 0;
        if (hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
            numOfHands++;
        }
        if (hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
            numOfLowerArmActuators++;
        }
        if (hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)) {
            numOfUpperArmActuators++;
        }
        if (hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
            numOfHands++;
        }
        if (hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
            numOfLowerArmActuators++;
        }
        if (hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM)) {
            numOfUpperArmActuators++;
        }
        cost += numOfUpperArmActuators * weight * 100;
        cost += numOfLowerArmActuators * weight * 50;
        cost += numOfHands * weight * 80;
        return cost;
    }

    @Override
    protected double getLegActuatorCost() {
        return weight * 150 * 2 + weight * 80 * 2 + weight * 120 * 2;
    }

    /**
     * Check to see if a Biped mech has a claw in one of its arms
     *
     * @param location
     *            (LOC_RARM or LOC_LARM)
     * @return True/False
     */
    public boolean hasClaw(int location) {
        // only arms have claws.
        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return false;
        }
        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);

            if (cs == null) {
                continue;
            }
            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }
            Mounted m = this.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_HAND_WEAPON) && type.hasSubType(MiscType.S_CLAW)) {
                return !(m.isDestroyed() || m.isMissing() || m.isBreached());
            }
        }
        return false;
    }

    /**
     * Checks to see if this bipmech has any vibro blades on them.
     *
     * @return boolean <code>true</code> if the mech has vibroblades
     *         <code>false</code> if not.
     */
    @Override
    public boolean hasVibroblades() {
        int count = 0;

        if (hasVibrobladesInLocation(Mech.LOC_RARM)) {
            count++;
        }
        if (hasVibrobladesInLocation(Mech.LOC_LARM)) {
            count++;
        }

        return count > 0;
    }

    /**
     * Checks to see if this bipedmech has a vibroblade in this location.
     *
     * @param location
     * @return boolean <code>true</code> if the mech has vibroblades
     *         <code>false</code> if not.
     */
    public boolean hasVibrobladesInLocation(int location) {

        // Only arms have VibroBlades.
        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return false;
        }

        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);

            if (cs == null) {
                continue;
            }
            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }
            Mounted m = this.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isVibroblade()) {
                return !(m.isDestroyed() || m.isMissing() || m.isBreached());
            }
        }

        return false;
    }

    /**
     * Does the entity have a retracted blade in the given location
     *
     */
    @Override
    public boolean hasRetractedBlade(int loc) {
        for (Mounted m : getEquipment()) {
            if ((m.getLocation() == loc) && !m.isDestroyed() && !m.isBreached() && (m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_CLUB) && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE) && !m.curMode().equals("extended")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for Active Vibroblades in <code>location</code> and returns the
     * amount of heat genereated if active.
     *
     * @param location
     * @return <code>int</code> amount of heat genereated by an active
     *         vibroblade.
     */
    @Override
    public int getActiveVibrobladeHeat(int location) {
        // Only arms have VibroBlades.
        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return 0;
        }

        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);

            if (cs == null) {
                continue;
            }
            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }
            Mounted m = this.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isVibroblade() && m.curMode().equals("Active") && !(m.isDestroyed() || m.isMissing() || m.isBreached())) {
                MiscType blade = (MiscType) type;
                if (blade.hasSubType(MiscType.S_VIBRO_LARGE)) {
                    return 7;
                }
                if (blade.hasSubType(MiscType.S_VIBRO_MEDIUM)) {
                    return 5;
                }
                // else
                return 3;
            }
        }

        return 0;

    }

    /**
     * Does the mech have any shields. a mech can have up to 2 shields.
     *
     * @return <code>true</code> if unit has a shield crit.
     */
    @Override
    public boolean hasShield() {
        for (Mounted m : getMisc()) {
            EquipmentType type = m.getType();
            if ( ((m.getLocation() == Mech.LOC_LARM) || (m.getLocation() == Mech.LOC_RARM)) && (type instanceof MiscType) && ((MiscType) type).isShield() && (this.getInternal(m.getLocation()) > 0)) {
                for (int slot = 0; slot < this.getNumberOfCriticals(m.getLocation()); slot++) {
                    CriticalSlot cs = getCritical(m.getLocation(), slot);
                    if (cs.getMount().equals(m) && !(cs.isDestroyed())) {
                        // when all crits of a shield are destroyed, it
                        // no longer hinders movemenet and stuff
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check to see how many shields of a certain size a mek has. you can have
     * up to shields per mek. However they can be of different size and each
     * size has its own draw backs. So check each size and add modifers based on
     * the number shields of that size.
     */
    @Override
    public int getNumberOfShields(long size) {

        int raShield = 0;
        int laShield = 0;

        for (Mounted m : getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_CLUB) && (type.hasSubType(size))) {
                // ok so we have a shield of certain size. no which arm is it.
                if (m.getLocation() == Mech.LOC_RARM) {
                    raShield = 1;
                }
                if (m.getLocation() == Mech.LOC_LARM) {
                    laShield = 1;
                }
                // break now.
                if ((raShield > 0) && (laShield > 0)) {
                    return 2;
                }
            }
        }
        return raShield + laShield;
    }

    /**
     * Does the mech have an active shield This should only be called after
     * hasShield has been called.
     */
    @Override
    public boolean hasActiveShield(int location, boolean rear) {

        switch (location) {
        case Mech.LOC_CT:
        case Mech.LOC_HEAD:
            // no rear head location so must be rear CT which is not
            // proected by
            // any shield
            if (rear) {
                return false;
            }
            if (hasActiveShield(Mech.LOC_LARM) || hasActiveShield(Mech.LOC_RARM)) {
                return true;
            }
            // else
            return false;
        case Mech.LOC_LARM:
        case Mech.LOC_LT:
        case Mech.LOC_LLEG:
            return hasActiveShield(Mech.LOC_LARM);
        default:
            return hasActiveShield(Mech.LOC_RARM);
        }
    }

    /**
     * Does the mech have an active shield This should only be called by
     * hasActiveShield(location,rear)
     */
    @Override
    public boolean hasActiveShield(int location) {

        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return false;
        }

        if (isShutDown() || (getCrew().isKoThisRound() || getCrew().isUnconscious())) {
            return false;
        }

        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);

            if (cs == null) {
                continue;
            }

            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }

            if (cs.isDamaged()) {
                continue;
            }

            Mounted m = this.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isShield() && m.curMode().equals(MiscType.S_ACTIVE_SHIELD)) {
                return m.getCurrentDamageCapacity(this, m.getLocation()) > 0;
            }
        }
        return false;
    }

    /**
     * Does the mech have a passive shield This should only be called after
     * hasShield has been called.
     */
    @Override
    public boolean hasPassiveShield(int location, boolean rear) {

        switch (location) {
        // CT Head and legs are not protected by Passive shields.
        case Mech.LOC_CT:
        case Mech.LOC_HEAD:
        case Mech.LOC_LLEG:
        case Mech.LOC_RLEG:
            return false;
        case Mech.LOC_LARM:
        case Mech.LOC_LT:
            if (rear) {
                // that
                return false;
            }
            return hasPassiveShield(Mech.LOC_LARM);
            // RA RT
        default:
            if (rear) {
                // that
                return false;
            }
            return hasPassiveShield(Mech.LOC_RARM);
        }
    }

    /**
     * Does the mech have a passive shield This should only be called by
     * hasPassiveShield(location,rear)
     */
    @Override
    public boolean hasPassiveShield(int location) {

        if (isShutDown() || (getCrew().isKoThisRound() || getCrew().isUnconscious())) {
            return false;
        }

        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return false;
        }

        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);

            if (cs == null) {
                continue;
            }

            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }

            if (cs.isDamaged()) {
                continue;
            }

            Mounted m = this.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isShield() && m.curMode().equals(MiscType.S_PASSIVE_SHIELD)) {
                return m.getCurrentDamageCapacity(this, m.getLocation()) > 0;
            }
        }
        return false;
    }

    /**
     * Does the mech have an shield in no defense mode
     */
    @Override
    public boolean hasNoDefenseShield(int location) {

        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return false;
        }

        for (int slot = 0; slot < this.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);

            if (cs == null) {
                continue;
            }

            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }

            if (cs.isDamaged()) {
                continue;
            }

            Mounted m = this.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isShield() && (m.curMode().equals(MiscType.S_NO_SHIELD) || isShutDown() || // if
                    // he
                    // has
                    // a
                    // shield
                    // and
                    // the mek is SD or pilot
                    // KOed then it goes to no
                    // defense mode
                    getCrew().isKoThisRound() || getCrew().isUnconscious())) {
                return m.getCurrentDamageCapacity(this, m.getLocation()) > 0;
            }
        }
        return false;
    }


    /**
     * Checks is Biped Mek has functional AES in the location.
     * Only works for Arms
     */
    @Override
    public boolean hasFunctionalArmAES(int location) {

        boolean hasAES = false;
        if ( (location != Mech.LOC_RARM) && (location != Mech.LOC_LARM) ) {
            return false;
        }

        for (Mounted mounted : getMisc() ) {
            if ( (mounted.getLocation() == location)
                    && mounted.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)
                    && !mounted.isDestroyed()
                    && !mounted.isBreached()
                    && !mounted.isMissing() ) {
                hasAES = true;
            } //AES is destroyed their for it cannot be used.
            else if ( (mounted.getLocation() == location)
                    && mounted.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM) ) {
                return false;
            }
        }

        return hasAES;
    }

    /**
     * Checks for functional AES in both legs
     */
    @Override
    public boolean hasFunctionalLegAES() {
        boolean rightLeg = false;
        boolean leftLeg = false;

        for (Mounted mounted : getMisc()) {
            if ( (mounted.getLocation() == Mech.LOC_LLEG) || (mounted.getLocation() == Mech.LOC_RLEG) ) {
                if ( ((MiscType)mounted.getType()).hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)
                        && !mounted.isDestroyed()
                        && !mounted.isBreached()
                        && !mounted.isMissing()) {
                    if ( mounted.getLocation() == Mech.LOC_LLEG ) {
                        leftLeg = true;
                    }
                    else {
                        rightLeg = true;
                    }
                }//AES is destroyed their for it cannot be used.
                else if ( ((MiscType)mounted.getType()).hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM) ) {
                    return false;
                }
            }
        }

        return rightLeg && leftLeg;
    }

    @Override
    public boolean canGoHullDown() {
        return game.getOptions().booleanOption("tacops_hull_down")
            && !isLocationBad(Mech.LOC_LLEG)
            && !isLocationBad(Mech.LOC_RLEG)
            && !isLocationDoomed(Mech.LOC_LLEG)
            && !isLocationDoomed(Mech.LOC_RLEG);
    }

    @Override
    public PilotingRollData checkGetUp(MoveStep step) {

        PilotingRollData roll = super.checkGetUp(step);

        if ( game.getOptions().booleanOption("tacops_attempting_stand")
                && (roll.getValue() != TargetRoll.CHECK_FALSE)) {
            addStandingPenalties(roll);
        }

        return roll;
    }


    public PilotingRollData addStandingPenalties(PilotingRollData roll) {


        int[] locsToCheck = new int[2];

        locsToCheck[0] = Mech.LOC_RARM;
        locsToCheck[1] = Mech.LOC_LARM;

        for (int i = 0; i < locsToCheck.length; i++) {
            int loc = locsToCheck[i];
            if (isLocationBad(loc)) {
                roll.addModifier(2, getLocationName(loc) + " destroyed");
            } else {
                // check for damaged hip actuators
                if (!hasWorkingSystem(Mech.ACTUATOR_HAND, loc)) {
                    roll.addModifier(1, getLocationName(loc) + " hand Actuator missing/destroyed");
                } else if (!hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, loc)) {
                    roll.addModifier(1, getLocationName(loc) + " lower Actuator missing/destroyed");
                } else if (!hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, loc)) {
                    roll.addModifier(1, getLocationName(loc) + " upper ctuator missing/destroyed");
                } else if (!hasWorkingSystem(Mech.ACTUATOR_SHOULDER, loc)) {
                    roll.addModifier(1, getLocationName(loc) + " shoulder Actuator missing/destroyed");
                }
            }
        }

        return roll;
    }

    /**
     * @return if this mech cannot stand up from hulldown
     */
    @Override
    public boolean cannotStandUpFromHullDown() {
        int i = 0;
        if (isLocationBad(LOC_LLEG)) {
            i++;
        }
        if (isLocationBad(LOC_RLEG)) {
            i++;
        }
        return (i >= 1)
        || ((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 1) && (getGyroType() != Mech.GYRO_HEAVY_DUTY))
        || ((getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 2) && (getGyroType() == Mech.GYRO_HEAVY_DUTY));
    }
}
