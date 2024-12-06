/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org), Cord Awtry (kipsta@bs-interactive.com)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.MiscMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;

public class BipedMek extends Mek {
    private static final long serialVersionUID = 4166375446709772785L;

    private static final String[] LOCATION_NAMES =
            { "Head", "Center Torso", "Right Torso", "Left Torso", "Right Arm", "Left Arm", "Right Leg", "Left Leg" };

    private static final String[] LOCATION_ABBRS = { "HD", "CT", "RT", "LT", "RA", "LA", "RL", "LL" };

    private static final int[] NUM_OF_SLOTS = { 6, 12, 12, 12, 12, 12, 6, 6 };

    public BipedMek(String inGyroType, String inCockpitType) {
        this(getGyroTypeForString(inGyroType), getCockpitTypeForString(inCockpitType));
    }

    public BipedMek() {
        this(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD);
    }

    public BipedMek(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);

        movementMode = EntityMovementMode.BIPED;
        originalMovementMode = EntityMovementMode.BIPED;

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
        boolean canFlip = true;

        if (hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM)) {
            canFlip = false;
        } else if (hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LARM)) {
            canFlip = false;
        } else if (hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM)) {
            canFlip = false;
        } else if (hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RARM)) {
            canFlip = false;
        }

        if (hasQuirk(OptionsConstants.QUIRK_POS_HYPER_ACTUATOR)) {
            canFlip = true;
        }

        if (isProne()) {
            canFlip = false;
        }

        return canFlip;
    }

    /**
     * Returns true if the entity can pick up ground objects
     */
    public boolean canPickupGroundObject() {
    	return hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM) && (getCarriedObject(Mek.LOC_LARM) == null) ||
    			hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM) && (getCarriedObject(Mek.LOC_RARM) == null);
    }

    /**
     * The maximum tonnage of ground objects that can be picked up by this unit
     */
    public double maxGroundObjectTonnage() {
    	double percentage = 0.0;

    	if (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM) && (getCarriedObject(Mek.LOC_LARM) == null) &&
    			!isLocationBad(Mek.LOC_LARM)) {
    		percentage += 0.05;
    	}
    	if (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM) && (getCarriedObject(Mek.LOC_RARM) == null) &&
    			!isLocationBad(Mek.LOC_RARM)) {
    		percentage += 0.05;
    	}

    	double heavyLifterMultiplier = hasAbility(OptionsConstants.PILOT_HVY_LIFTER) ? 1.5 : 1.0;

    	return getWeight() * percentage * heavyLifterMultiplier;
    }

    @Override
    public List<Integer> getDefaultPickupLocations() {
    	List<Integer> result = new ArrayList<>();

    	if (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM) && (getCarriedObject(Mek.LOC_LARM) == null) &&
    			!isLocationBad(Mek.LOC_LARM)) {
    		result.add(Mek.LOC_LARM);
    	}
    	if (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM) && (getCarriedObject(Mek.LOC_RARM) == null) &&
    			!isLocationBad(Mek.LOC_RARM)) {
    		result.add(Mek.LOC_RARM);
    	}

    	return result;
    }

    @Override
    public List<Integer> getValidHalfWeightPickupLocations(ICarryable cargo) {
    	List<Integer> result = new ArrayList<>();

    	// if we can pick the object up according to "one handed pick up rules" in TacOps
    	if (cargo.getTonnage() <= (getWeight() / 20)) {
    		if (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM) && (getCarriedObject(Mek.LOC_LARM) == null) &&
        			!isLocationBad(Mek.LOC_LARM)) {
    			result.add(Mek.LOC_LARM);
    		}

    		if (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM) && (getCarriedObject(Mek.LOC_RARM) == null) &&
        			!isLocationBad(Mek.LOC_RARM)) {
    			result.add(Mek.LOC_RARM);
    		}
    	}

    	return result;
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();
        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;

        //A Mek using tracks has its movement reduced by 50% per leg or track destroyed;
        if (getMovementMode().isTracked()) {
            for (Mounted<?> m : getMisc()) {
                if (m.getType().hasFlag(MiscType.F_TRACKS)) {
                    if (m.isHit() || isLocationBad(m.getLocation())) {
                        legsDestroyed++;
                    }
                }
            }
            mp = (mp * (2 - legsDestroyed)) / 2;
        } else {
            for (int i = 0; i < locations(); i++) {
                if (locationIsLeg(i)) {
                    if (!isLocationBad(i)) {
                        if (legHasHipCrit(i)) {
                            hipHits++;
                            if ((game == null) || !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
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
                mp = (legsDestroyed == 1) ? 1 : 0;
            } else {
                if (hipHits > 0) {
                    if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                        mp = mp - 2 * hipHits;
                    } else {
                        mp = (hipHits == 1) ? (int) Math.ceil(mp / 2.0) : 0;
                    }
                }
                mp -= actuatorHits;
            }
        }

        if (hasShield()) {
            mp -= getNumberOfShields(MiscType.S_SHIELD_LARGE);
            mp -= getNumberOfShields(MiscType.S_SHIELD_MEDIUM);
        }

        if (!mpCalculationSetting.ignoreModularArmor && hasModularArmor()) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreHeat) {
            // factor in heat
            if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT)) {
                if (heat < 30) {
                    mp -= (heat / 5);
                } else if (heat >= 49) {
                    mp -= 9;
                } else if (heat >= 43) {
                    mp -= 8;
                } else if (heat >= 37) {
                    mp -= 7;
                } else if (heat >= 31) {
                    mp -= 6;
                } else {
                    mp -= 5;
                }
            } else {
                mp -= (heat / 5);
            }
        }

        // TSM negates some heat, but provides no benefit when using tracks.
        if (((heat >= 9) || mpCalculationSetting.forceTSM) && hasTSM(false)
                && (legsDestroyed == 0) && !movementMode.isTracked()) {
            if (mpCalculationSetting.forceTSM && mpCalculationSetting.ignoreHeat) {
                // When forcing TSM but ignoring heat we must assume heat to be 9 to activate TSM, this adds -1 MP!
                mp += 1;
            } else {
                mp += 2;
            }
        }

        if (!mpCalculationSetting.ignoreCargo) {
            mp = Math.max(mp - getCargoMpReduction(this), 0);
        }

        if (!mpCalculationSetting.ignoreWeather && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);

            if (getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)
                    && conditions.getWeather().isClear()
                    && conditions.getWind().isTornadoF1ToF3()) {
                mp += 1;
            }
        }

        if (!mpCalculationSetting.ignoreGravity) {
            mp = applyGravityEffectsOnMP(mp);
        }

        mp = applyWalkMPEquipmentModifiers(mp);

        return Math.max(0, mp);
    }

    /**
     * Returns this Mek's running/flank mp modified for leg loss and stuff.
     */
    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        if (countBadLegs() == 0) {
            return super.getRunMP(mpCalculationSetting);
        } else {
            return getWalkMP(mpCalculationSetting);
        }
    }

    /**
     * Sets the internal structure for the Mek.
     *
     * @param head head
     * @param ct   center torso
     * @param t    right/left torso
     * @param arm  right/left arm
     * @param leg  right/left leg
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

        locsToCheck[0] = Mek.LOC_RLEG;
        locsToCheck[1] = Mek.LOC_LLEG;

        if (hasFunctionalLegAES()) {
            roll.addModifier(-2, "AES bonus");
        }

        for (int i = 0; i < locsToCheck.length; i++) {
            int loc = locsToCheck[i];

            if (isLocationBad(loc)) {
                roll.addModifier(5, getLocationName(loc) + " destroyed");
            } else {
                // check for damaged hip actuators
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                    if (!game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                        continue;
                    }
                }
                // upper leg actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Upper Leg Actuator destroyed");
                }
                // lower leg actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Lower Leg Actuator destroyed");
                }
                // foot actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_FOOT, loc) > 0) {
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
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Returns a vector of abbreviations for all locations
     */
    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    protected double getArmActuatorCost() {
        double cost = 0;
        int numOfUpperArmActuators = 0;
        int numOfLowerArmActuators = 0;
        int numOfHands = 0;
        if (hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM)) {
            numOfHands++;
        }
        if (hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LARM)) {
            numOfLowerArmActuators++;
        }
        if (hasSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LARM)) {
            numOfUpperArmActuators++;
        }
        if (hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM)) {
            numOfHands++;
        }
        if (hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RARM)) {
            numOfLowerArmActuators++;
        }
        if (hasSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_RARM)) {
            numOfUpperArmActuators++;
        }
        cost += numOfUpperArmActuators * weight * 100;
        cost += numOfLowerArmActuators * weight * 50;
        cost += numOfHands * weight * 80;
        return cost;
    }

    @Override
    protected double getLegActuatorCost() {
        return (weight * 150 * 2) + (weight * 80 * 2) + (weight * 120 * 2);
    }

    /**
     * Checks to see if this bipMek has any vibro blades on them.
     *
     * @return boolean <code>true</code> if the Mek has vibroblades
     * <code>false</code> if not.
     */
    @Override
    public boolean hasVibroblades() {
        int count = 0;

        if (hasVibrobladesInLocation(Mek.LOC_RARM)) {
            count++;
        }
        if (hasVibrobladesInLocation(Mek.LOC_LARM)) {
            count++;
        }

        return count > 0;
    }

    /**
     * Checks to see if this bipedMek has a vibroblade in this location.
     *
     * @param location
     * @return boolean <code>true</code> if the Mek has vibroblades
     * <code>false</code> if not.
     */
    public boolean hasVibrobladesInLocation(int location) {

        // Only arms have VibroBlades.
        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
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
            Mounted<?> m = cs.getMount();
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isVibroblade()) {
                return !(m.isDestroyed() || m.isMissing() || m.isBreached());
            }
        }

        return false;
    }

    /**
     * Does the entity have a retracted blade in the given location
     */
    @Override
    public boolean hasRetractedBlade(int loc) {
        for (Mounted<?> m : getEquipment()) {
            if ((m.getLocation() == loc) && !m.isDestroyed() && !m.isBreached() && (m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_CLUB) && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE) && !m
                    .curMode().equals("extended")) {
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
     * vibroblade.
     */
    @Override
    public int getActiveVibrobladeHeat(int location) {
        return getActiveVibrobladeHeat(location, false);
    }

    @Override
    public int getActiveVibrobladeHeat(int location, boolean ignoreMode) {
        // Only arms have VibroBlades.
        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
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
            Mounted<?> m = cs.getMount();
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isVibroblade() && (m.curMode().equals("Active") ||
                                                                                   ignoreMode) && !(m.isDestroyed()
                                                                                                    || m.isMissing()
                                                                                                    || m.isBreached()
            )) {
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
     * Does the Mek have any shields. a Mek can have up to 2 shields.
     *
     * @return <code>true</code> if unit has a shield crit.
     */
    @Override
    public boolean hasShield() {
        for (Mounted<?> m : getMisc()) {
            boolean isShield = (m.getType() instanceof MiscType)
                    && ((MiscType) m.getType()).isShield();
            if (((m.getLocation() == Mek.LOC_LARM) || (m.getLocation() == Mek.LOC_RARM))
                    && isShield && !m.isInoperable() && (getInternal(m.getLocation()) > 0)) {
                for (int slot = 0; slot < this.getNumberOfCriticals(m.getLocation()); slot++) {
                    CriticalSlot cs = getCritical(m.getLocation(), slot);
                    if ((cs != null)
                            && (cs.getType() == CriticalSlot.TYPE_EQUIPMENT)
                            && cs.getMount().equals(m) && !cs.isDestroyed()
                            && !cs.isMissing()) {
                        // when all crits of a shield are destroyed, it
                        // no longer hinders movement and stuff
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

        for (Mounted<?> m : getMisc()) {
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && type.hasFlag(MiscType.F_CLUB)
                    && (type.hasSubType(size))) {
                // ok so we have a shield of certain size. no which arm is it.
                if (m.getLocation() == Mek.LOC_RARM) {
                    raShield = 1;
                }
                if (m.getLocation() == Mek.LOC_LARM) {
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
     * Does the Mek have an active shield This should only be called after
     * hasShield has been called.
     */
    @Override
    public boolean hasActiveShield(int location, boolean rear) {

        switch (location) {
            case Mek.LOC_CT:
            case Mek.LOC_HEAD:
                // no rear head location so must be rear CT which is not
                // proected by
                // any shield
                if (rear) {
                    return false;
                }
                if (hasActiveShield(Mek.LOC_LARM) || hasActiveShield(Mek.LOC_RARM)) {
                    return true;
                }
                // else
                return false;
            case Mek.LOC_LARM:
            case Mek.LOC_LT:
            case Mek.LOC_LLEG:
                return hasActiveShield(Mek.LOC_LARM);
            default:
                return hasActiveShield(Mek.LOC_RARM);
        }
    }

    /**
     * Does the Mek have an active shield This should only be called by
     * hasActiveShield(location, rear)
     */
    @Override
    public boolean hasActiveShield(int location) {

        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
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

            Mounted<?> m = cs.getMount();
            if ((m instanceof MiscMounted) && ((MiscMounted) m).getType().isShield() && m.curMode().equals(MiscType.S_ACTIVE_SHIELD)) {
                return ((MiscMounted) m).getCurrentDamageCapacity(this, m.getLocation()) > 0;
            }
        }
        return false;
    }

    /**
     * Does the Mek have a passive shield This should only be called after
     * hasShield has been called.
     */
    @Override
    public boolean hasPassiveShield(int location, boolean rear) {

        switch (location) {
            // CT Head and legs are not protected by Passive shields.
            case Mek.LOC_CT:
            case Mek.LOC_HEAD:
            case Mek.LOC_LLEG:
            case Mek.LOC_RLEG:
                return false;
            case Mek.LOC_LARM:
            case Mek.LOC_LT:
                if (rear) {
                    // that
                    return false;
                }
                return hasPassiveShield(Mek.LOC_LARM);
            // RA RT
            default:
                if (rear) {
                    // that
                    return false;
                }
                return hasPassiveShield(Mek.LOC_RARM);
        }
    }

    /**
     * Does the Mek have a passive shield This should only be called by
     * hasPassiveShield(location, rear)
     */
    @Override
    public boolean hasPassiveShield(int location) {

        if (isShutDown() || (getCrew().isKoThisRound() || getCrew().isUnconscious())) {
            return false;
        }

        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
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

            Mounted<?> m = cs.getMount();
            if ((m instanceof MiscMounted) && ((MiscMounted) m).getType().isShield() && m.curMode().equals(MiscType.S_PASSIVE_SHIELD)) {
                return ((MiscMounted) m).getCurrentDamageCapacity(this, m.getLocation()) > 0;
            }
        }
        return false;
    }

    /**
     * Does the Mek have an shield in no defense mode
     */
    @Override
    public boolean hasNoDefenseShield(int location) {

        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
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

            Mounted<?> m = cs.getMount();
            if ((m instanceof MiscMounted) && ((MiscMounted) m).getType().isShield() && (m.curMode().equals(MiscType.S_NO_SHIELD) || isShutDown() || // if
                                                                               // he
                                                                               // has
                                                                               // a
                                                                               // shield
                                                                               // and
                                                                               // the mek is SD or pilot
                                                                               // KOed then it goes to no
                                                                               // defense mode
                                                                               getCrew().isKoThisRound() || getCrew()
                    .isUnconscious())) {
                return ((MiscMounted) m).getCurrentDamageCapacity(this, m.getLocation()) > 0;
            }
        }
        return false;
    }

    /**
     * Checks is Biped Mek has functional AES in the location. Only works for
     * Arms
     */
    @Override
    public boolean hasFunctionalArmAES(int location) {

        boolean hasAES = false;
        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
            return false;
        }

        for (Mounted<?> mounted : getMisc()) {
            if ((mounted.getLocation() == location) && mounted.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM) && !mounted.isDestroyed() && !mounted.isBreached() && !mounted.isMissing()) {
                hasAES = true;
            } // AES is destroyed their for it cannot be used.
            else if ((mounted.getLocation() == location) && mounted.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
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

        for (Mounted<?> mounted : getMisc()) {
            if ((mounted.getLocation() == Mek.LOC_LLEG) || (mounted.getLocation() == Mek.LOC_RLEG)) {
                if (((MiscType) mounted.getType()).hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM) && !mounted
                        .isDestroyed() && !mounted.isBreached() && !mounted.isMissing()) {
                    if (mounted.getLocation() == Mek.LOC_LLEG) {
                        leftLeg = true;
                    } else {
                        rightLeg = true;
                    }
                }// AES is destroyed their for it cannot be used.
                else if (((MiscType) mounted.getType()).hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
                    return false;
                }
            }
        }

        return rightLeg && leftLeg;
    }

    @Override
    public boolean canGoHullDown() {
        // check the option
        boolean retVal = game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN);
        if (!retVal) {
            return false;
        }
        // check the locations
        retVal = (!isLocationBad(Mek.LOC_LLEG) && !isLocationBad(Mek.LOC_RLEG) && !isLocationDoomed(Mek.LOC_LLEG)
                  && !isLocationDoomed(Mek.LOC_RLEG));
        if (!retVal) {
            return false;
        }
        // check the Gyro
        return !isGyroDestroyed();
    }

    @Override
    public PilotingRollData checkGetUp(MoveStep step,
            EntityMovementType moveType) {

        PilotingRollData roll = super.checkGetUp(step, moveType);

        if (roll.getValue() != TargetRoll.CHECK_FALSE) {
            addStandingPenalties(roll);
        }

        return roll;
    }

    public void addStandingPenalties(PilotingRollData roll) {

        if (hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
            roll.addModifier(2, "no/minimal arms");
            return;
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_ATTEMPTING_STAND)) {
            int[] locsToCheck = new int[2];

            locsToCheck[0] = Mek.LOC_RARM;
            locsToCheck[1] = Mek.LOC_LARM;

            for (int i = 0; i < locsToCheck.length; i++) {
                int loc = locsToCheck[i];
                if (isLocationBad(loc)) {
                    roll.addModifier(2, getLocationName(loc) + " destroyed");
                } else {
                    // check for damaged hip actuators
                    if (!hasWorkingSystem(Mek.ACTUATOR_HAND, loc)) {
                        roll.addModifier(1, getLocationName(loc) + " hand Actuator missing/destroyed");
                    } else if (!hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, loc)) {
                        roll.addModifier(1, getLocationName(loc) + " lower Actuator missing/destroyed");
                    } else if (!hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, loc)) {
                        roll.addModifier(1, getLocationName(loc) + " upper ctuator missing/destroyed");
                    } else if (!hasWorkingSystem(Mek.ACTUATOR_SHOULDER, loc)) {
                        roll.addModifier(1, getLocationName(loc) + " shoulder Actuator missing/destroyed");
                    }
                }
            }
        }

    }

    /**
     * @return if this Mek cannot stand up from hulldown
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
        return (i >= 1) || isGyroDestroyed();
    }

    @Override
    public boolean hasMPReducingHardenedArmor() {
        return (armorType[LOC_LLEG] == EquipmentType.T_ARMOR_HARDENED)
               || (armorType[LOC_RLEG] == EquipmentType.T_ARMOR_HARDENED);
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_MEK | Entity.ETYPE_BIPED_MEK;
    }

    /**
     *
     * @return true if this unit is capable of Zweihandering (melee attack with both hands)
     */
    public boolean canZweihander() {
        return (getCrew() != null)
                && hasAbility(OptionsConstants.PILOT_ZWEIHANDER)
                && hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM)
                && hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM)
                && !isLocationBad(Mek.LOC_RARM)
                && !isLocationBad(Mek.LOC_LARM)
                && !weaponFiredFrom(Mek.LOC_LARM)
                && !weaponFiredFrom(Mek.LOC_RARM)
                && !isProne();
    }

    /**
     * Based on the Mek's current damage status, return valid brace locations.
     */
    @Override
    public List<Integer> getValidBraceLocations() {
        List<Integer> validLocations = new ArrayList<>();

        if (!isLocationBad(Mek.LOC_RARM)) {
            validLocations.add(Mek.LOC_RARM);
        }

        if (!isLocationBad(Mek.LOC_LARM)) {
            validLocations.add(Mek.LOC_LARM);
        }

        return validLocations;
    }

    @Override
    public boolean canBrace() {
        return getCrew().isActive()
                && !isShutDown()
                // needs to have at least one functional arm
                && (!isLocationBad(Mek.LOC_RARM)
                || !isLocationBad(Mek.LOC_LARM))
                && !isProne();
    }

    @Override
    public int getBraceMPCost() {
        return 1;
    }
}
