/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
import java.util.Collections;
import java.util.List;

import megamek.common.equipment.MiscMounted;
import megamek.common.options.OptionsConstants;

/**
 * This class acts as a superclass to BipedMek and TripodMek to unify code that is shared between them, most of it arm-related as both have
 * two arms while QuadMeks have none.
 */
public abstract class MekWithArms extends Mek {

    public MekWithArms(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);
    }

    @Override
    public boolean canFlipArms() {
        return !isProne() && (!hasAnyLowerArmOrHandActuator() || hasQuirk(OptionsConstants.QUIRK_POS_HYPER_ACTUATOR));
    }

    private boolean hasAnyLowerArmOrHandActuator() {
        return hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM) || hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LARM)
            || hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM) || hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RARM);
    }

    @Override
    public List<Integer> getDefaultPickupLocations() {
        List<Integer> result = new ArrayList<>();

        if (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM) && (getCarriedObject(Mek.LOC_LARM) == null) && !isLocationBad(Mek.LOC_LARM)) {
            result.add(Mek.LOC_LARM);
        }
        if (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM) && (getCarriedObject(Mek.LOC_RARM) == null) && !isLocationBad(Mek.LOC_RARM)) {
            result.add(Mek.LOC_RARM);
        }

        return result;
    }

    @Override
    public List<Integer> getValidHalfWeightPickupLocations(ICarryable cargo) {
        return (cargo.getTonnage() <= (getWeight() / 20)) ? getDefaultPickupLocations() : Collections.emptyList();
    }

    @Override
    public double maxGroundObjectTonnage() {
        double percentage = 0.0;

        if (hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM) && (getCarriedObject(Mek.LOC_LARM) == null)) {
            percentage += 0.05;
        }
        if (hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM) && (getCarriedObject(Mek.LOC_RARM) == null)) {
            percentage += 0.05;
        }

        double heavyLifterMultiplier = hasAbility(OptionsConstants.PILOT_HVY_LIFTER) ? 1.5 : 1.0;
        return getWeight() * percentage * heavyLifterMultiplier;
    }

    @Override
    public boolean hasShield() {
        for (MiscMounted m : getMisc()) {
            MiscType type = m.getType();
            if (((m.getLocation() == Mek.LOC_LARM) || (m.getLocation() == Mek.LOC_RARM))
                && type.isShield()
                && !m.isInoperable()
                && (getInternal(m.getLocation()) > 0)) {
                for (int slot = 0; slot < getNumberOfCriticals(m.getLocation()); slot++) {
                    CriticalSlot cs = getCritical(m.getLocation(), slot);
                    if ((cs != null)
                        && (cs.getType() == CriticalSlot.TYPE_EQUIPMENT)
                        && cs.getMount().equals(m) && !cs.isDestroyed()
                        && !cs.isMissing()) {
                        // when all crits of a shield are destroyed, it no longer hinders movement and stuff
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean canBrace() {
        return getCrew().isActive() && !isShutDown() && !isProne()
            // needs to have at least one functional arm
            && (!isLocationBad(Mek.LOC_RARM) || !isLocationBad(Mek.LOC_LARM));
    }

    @Override
    public int getBraceMPCost() {
        return 1;
    }

    @Override
    public List<Integer> getValidBraceLocations() {
        List<Integer> validLocations = new ArrayList<>(List.of(Mek.LOC_RARM, Mek.LOC_LARM));
        validLocations.removeIf(this::isLocationBad);
        return validLocations;
    }

    @Override
    public boolean hasFunctionalArmAES(int location) {
        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
            return false;
        }

        boolean hasAES = false;
        for (MiscMounted mounted : getMisc()) {
            if ((mounted.getLocation() == location)
                && mounted.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM) && mounted.isOperable()) {
                hasAES = true;
            } // AES is destroyed therefore it cannot be used.
            else if ((mounted.getLocation() == location)
                && mounted.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
                return false;
            }
        }

        return hasAES;
    }

    @Override
    public boolean hasRetractedBlade(int loc) {
        for (Mounted<?> m : getEquipment()) {
            if ((m.getLocation() == loc) && !m.isDestroyed() && !m.isBreached()
                && (m.getType() instanceof MiscType)
                && m.getType().hasFlag(MiscType.F_CLUB)
                && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE)
                && !m.curMode().equals("extended")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasVibroblades() {
        return hasVibrobladesInLocation(Mek.LOC_RARM) || hasVibrobladesInLocation(Mek.LOC_LARM);
    }

    /**
     * Returns true if this Mek has a vibroblade in the given location.
     *
     * @param location The location to check
     * @return boolean true if the Mek has a vibroblade in the location, false otherwise
     */
    public boolean hasVibrobladesInLocation(int location) {
        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
            return false;
        }

        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);
            if ((cs == null) || (cs.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                continue;
            }
            Mounted<?> mounted = cs.getMount();
            if (null == mounted) {
                continue;
            }
            EquipmentType type = mounted.getType();
            if ((type instanceof MiscType miscType) && miscType.isVibroblade()) {
                return !(mounted.isDestroyed() || mounted.isMissing() || mounted.isBreached());
            }
        }

        return false;
    }

    @Override
    public int getActiveVibrobladeHeat(int location) {
        return getActiveVibrobladeHeat(location, false);
    }

    @Override
    public int getActiveVibrobladeHeat(int location, boolean ignoreMode) {
        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
            return 0;
        }

        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);
            if ((cs == null) || (cs.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                continue;
            }
            Mounted<?> mounted = cs.getMount();
            if (null == mounted) {
                continue;
            }
            EquipmentType type = mounted.getType();
            if ((type instanceof MiscType miscType) && miscType.isVibroblade()
                && (mounted.curMode().equals("Active") || ignoreMode)
                && mounted.isOperable()) {
                if (miscType.hasSubType(MiscType.S_VIBRO_LARGE)) {
                    return 7;
                } else if (miscType.hasSubType(MiscType.S_VIBRO_MEDIUM)) {
                    return 5;
                } else {
                    return 3;
                }
            }
        }
        return 0;
    }

    @Override
    public int getNumberOfShields(long size) {

        int raShield = 0;
        int laShield = 0;

        for (MiscMounted misc : getMisc()) {
            MiscType type = misc.getType();
            if (type.hasFlag(MiscType.F_CLUB) && (type.hasSubType(size))) {
                // ok so we have a shield of certain size. no which arm is it.
                if (misc.getLocation() == Mek.LOC_RARM) {
                    raShield = 1;
                }
                if (misc.getLocation() == Mek.LOC_LARM) {
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

    @Override
    public boolean hasActiveShield(int location) {
        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
            return false;
        }

        if (isShutDown() || (getCrew().isKoThisRound() || getCrew().isUnconscious())) {
            return false;
        }

        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = getCritical(location, slot);
            if ((cs == null) || (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) || cs.isDamaged()) {
                continue;
            }

            Mounted<?> m = cs.getMount();
            if ((m instanceof MiscMounted miscMounted) && miscMounted.getType().isShield()
                && m.curMode().equals(MiscType.S_ACTIVE_SHIELD)) {
                return miscMounted.getCurrentDamageCapacity(this, m.getLocation()) > 0;
            }
        }
        return false;
    }

    @Override
    public boolean hasPassiveShield(int location, boolean rear) {
        return switch (location) {
            case Mek.LOC_LARM, Mek.LOC_LT -> !rear && hasPassiveShield(Mek.LOC_LARM);
            case Mek.LOC_RARM, Mek.LOC_RT -> !rear && hasPassiveShield(Mek.LOC_RARM);
            default -> false;
        };
    }

    @Override
    public boolean hasPassiveShield(int location) {
        if (isShutDown() || (getCrew().isKoThisRound() || getCrew().isUnconscious())) {
            return false;
        }

        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
            return false;
        }

        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
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
            if ((m instanceof MiscMounted) && ((MiscMounted) m).getType().isShield()
                && m.curMode().equals(MiscType.S_PASSIVE_SHIELD)) {
                return ((MiscMounted) m).getCurrentDamageCapacity(this, m.getLocation()) > 0;
            }
        }
        return false;
    }

    @Override
    public boolean hasNoDefenseShield(int location) {

        if ((location != Mek.LOC_RARM) && (location != Mek.LOC_LARM)) {
            return false;
        }

        for (int slot = 0; slot < getNumberOfCriticals(location); slot++) {
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
            if ((m instanceof MiscMounted)
                && ((MiscMounted) m).getType().isShield()
                && (m.curMode().equals(MiscType.S_NO_SHIELD)
                || isShutDown() ||
                // if he has a shield and the mek is SD or pilot
                // KOed then it goes to no defense mode
                getCrew().isKoThisRound() || getCrew()
                .isUnconscious())) {
                return ((MiscMounted) m).getCurrentDamageCapacity(this, m.getLocation()) > 0;
            }
        }
        return false;
    }

    @Override
    public boolean canPickupGroundObject() {
        return hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM) && (getCarriedObject(Mek.LOC_LARM) == null) ||
            hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM) && (getCarriedObject(Mek.LOC_RARM) == null);
    }

    @Override
    public PilotingRollData checkGetUp(MoveStep step, EntityMovementType moveType) {
        PilotingRollData roll = super.checkGetUp(step, moveType);
        if (roll.getValue() != TargetRoll.CHECK_FALSE) {
            addAttemptStandingPenalties(roll);
        }
        return roll;
    }

    private void addAttemptStandingPenalties(PilotingRollData roll) {
        if (hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
            roll.addModifier(2, "no/minimal arms");
            return;
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_ATTEMPTING_STAND)) {
            for (int loc : List.of(Mek.LOC_RARM, Mek.LOC_LARM)) {
                if (isLocationBad(loc)) {
                    roll.addModifier(2, getLocationName(loc) + " destroyed");
                } else {
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

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        if (countBadLegs() == 0) {
            return super.getRunMP(mpCalculationSetting);
        } else {
            return getWalkMP(mpCalculationSetting);
        }
    }
}
