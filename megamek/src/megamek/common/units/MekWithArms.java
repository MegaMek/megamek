/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.MPCalculationSetting;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.MekArms;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;

/**
 * This class acts as a superclass to BipedMek and TripodMek to unify code that is shared between them, most of it
 * arm-related as both have two arms while QuadMeks have none.
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
        return hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM) || hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LEFT_ARM)
              || hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM) || hasSystem(Mek.ACTUATOR_LOWER_ARM,
              Mek.LOC_RIGHT_ARM);
    }

    @Override
    public List<Integer> getDefaultPickupLocations() {
        List<Integer> result = new ArrayList<>();

        if (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM)
              && (getCarriedObject(Mek.LOC_LEFT_ARM) == null)
              && !isLocationBad(Mek.LOC_LEFT_ARM)) {
            result.add(Mek.LOC_LEFT_ARM);
        }
        if (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM)
              && (getCarriedObject(Mek.LOC_RIGHT_ARM) == null)
              && !isLocationBad(Mek.LOC_RIGHT_ARM)) {
            result.add(Mek.LOC_RIGHT_ARM);
        }

        return result;
    }

    @Override
    public List<Integer> getValidHalfWeightPickupLocations(ICarryable cargo) {
        return (cargo.getTonnage() <= (getWeight() / 20)) ? getDefaultPickupLocations() : Collections.emptyList();
    }

    @Override
    public double maxGroundObjectTonnage() {
        double heavyLifterMultiplier = hasAbility(OptionsConstants.PILOT_HVY_LIFTER) ? 1.5 : 1.0;
        double tsmModifier = getTSMPickupModifier();
        return unmodifiedMaxGroundObjectTonnage() * heavyLifterMultiplier * tsmModifier;
    }

    /**
     * The ground object tonnage this unit can lift, unmodified by TSM or Heavy Lifter
     *
     * @return the tonnage this mek can lift based on its weight and how many arms it has
     */
    public double unmodifiedMaxGroundObjectTonnage() {
        double percentage = 0.0;

        if (hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM) && (getCarriedObject(Mek.LOC_LEFT_ARM) == null)) {
            percentage += 0.05;
        }
        if (hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM) && (getCarriedObject(Mek.LOC_RIGHT_ARM) == null)) {
            percentage += 0.05;
        }

        return getWeight() * percentage;
    }

    @Override
    public boolean hasShield() {
        for (MiscMounted m : getMisc()) {
            MiscType type = m.getType();
            if (((m.getLocation() == Mek.LOC_LEFT_ARM) || (m.getLocation() == Mek.LOC_RIGHT_ARM))
                  && type.isShield()
                  && !m.isInoperable()
                  && (getInternal(m.getLocation()) > 0)) {
                for (int slot = 0; slot < getNumberOfCriticalSlots(m.getLocation()); slot++) {
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
              && (!isLocationBad(Mek.LOC_RIGHT_ARM) || !isLocationBad(Mek.LOC_LEFT_ARM));
    }

    @Override
    public int getBraceMPCost() {
        return 1;
    }

    @Override
    public List<Integer> getValidBraceLocations() {
        List<Integer> validLocations = new ArrayList<>(List.of(Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_ARM));
        validLocations.removeIf(this::isLocationBad);
        return validLocations;
    }

    @Override
    public boolean hasFunctionalArmAES(int location) {
        if ((location != Mek.LOC_RIGHT_ARM) && (location != Mek.LOC_LEFT_ARM)) {
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
                  && m.getType().hasFlag(MiscTypeFlag.S_RETRACTABLE_BLADE)
                  && !m.curMode().equals("extended")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasVibroblades() {
        return hasVibrobladesInLocation(Mek.LOC_RIGHT_ARM) || hasVibrobladesInLocation(Mek.LOC_LEFT_ARM);
    }

    /**
     * Returns true if this Mek has a vibroblade in the given location.
     *
     * @param location The location to check
     *
     * @return boolean true if the Mek has a vibroblade in the location, false otherwise
     */
    public boolean hasVibrobladesInLocation(int location) {
        if ((location != Mek.LOC_RIGHT_ARM) && (location != Mek.LOC_LEFT_ARM)) {
            return false;
        }

        for (int slot = 0; slot < getNumberOfCriticalSlots(location); slot++) {
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
        if ((location != Mek.LOC_RIGHT_ARM) && (location != Mek.LOC_LEFT_ARM)) {
            return 0;
        }

        for (int slot = 0; slot < getNumberOfCriticalSlots(location); slot++) {
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
                if (miscType.hasFlag(MiscTypeFlag.S_VIBRO_LARGE)) {
                    return 7;
                } else if (miscType.hasFlag(MiscTypeFlag.S_VIBRO_MEDIUM)) {
                    return 5;
                } else {
                    return 3;
                }
            }
        }
        return 0;
    }

    @Override
    public int getNumberOfShields(MiscTypeFlag shieldSize) {

        int raShield = 0;
        int laShield = 0;

        for (MiscMounted misc : getMisc()) {
            MiscType type = misc.getType();
            if (type.hasFlag(MiscType.F_CLUB) && (type.hasFlag(shieldSize))) {
                // ok so we have a shield of certain size. know which arm is it.
                if (misc.getLocation() == Mek.LOC_RIGHT_ARM) {
                    raShield = 1;
                }
                if (misc.getLocation() == Mek.LOC_LEFT_ARM) {
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
        if ((location != Mek.LOC_RIGHT_ARM) && (location != Mek.LOC_LEFT_ARM)) {
            return false;
        }

        if (isShutDown() || (getCrew().isKoThisRound() || getCrew().isUnconscious())) {
            return false;
        }

        for (int slot = 0; slot < getNumberOfCriticalSlots(location); slot++) {
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
            case Mek.LOC_LEFT_ARM, Mek.LOC_LEFT_TORSO -> !rear && hasPassiveShield(Mek.LOC_LEFT_ARM);
            case Mek.LOC_RIGHT_ARM, Mek.LOC_RIGHT_TORSO -> !rear && hasPassiveShield(Mek.LOC_RIGHT_ARM);
            default -> false;
        };
    }

    @Override
    public boolean hasPassiveShield(int location) {
        if (isShutDown() || (getCrew().isKoThisRound() || getCrew().isUnconscious())) {
            return false;
        }

        if ((location != Mek.LOC_RIGHT_ARM) && (location != Mek.LOC_LEFT_ARM)) {
            return false;
        }

        for (int slot = 0; slot < getNumberOfCriticalSlots(location); slot++) {
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

        if ((location != Mek.LOC_RIGHT_ARM) && (location != Mek.LOC_LEFT_ARM)) {
            return false;
        }

        for (int slot = 0; slot < getNumberOfCriticalSlots(location); slot++) {
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
        return (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM) && (getCarriedObject(Mek.LOC_LEFT_ARM) == null))
              ||
              (hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM) && (getCarriedObject(Mek.LOC_RIGHT_ARM) == null))
              ||
              super.canPickupGroundObject();
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
        // PLAYTEST2 Standing has -1 PSR
        if (gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
            roll.addModifier(-1, "Trying to stand");
        }

        if (hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
            roll.addModifier(2, "no/minimal arms");
            return;
        }

        if (gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_ATTEMPTING_STAND)) {
            for (int loc : List.of(Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_ARM)) {
                if (isLocationBad(loc)) {
                    roll.addModifier(2, getLocationName(loc) + " destroyed");
                } else {
                    if (!hasWorkingSystem(Mek.ACTUATOR_HAND, loc)) {
                        roll.addModifier(1, getLocationName(loc) + " hand Actuator missing/destroyed");
                    } else if (!hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, loc)) {
                        roll.addModifier(1, getLocationName(loc) + " lower Actuator missing/destroyed");
                    } else if (!hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, loc)) {
                        roll.addModifier(1, getLocationName(loc) + " upper Actuator missing/destroyed");
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

    @Override
    public boolean canPerformGroundSalvageOperations() {
        return hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM) &&
              hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM);
    }

    @Override
    public void addIntrinsicTransporters() {
        setMekArms();
        super.addIntrinsicTransporters();
    }

    /**
     * Add transporter for mek's arms for externally carried cargo
     */
    public void setMekArms() {
        if (getTransports().stream().noneMatch(transporter -> transporter instanceof MekArms)) {
            addTransporter(new MekArms(this));
        }
    }
}
