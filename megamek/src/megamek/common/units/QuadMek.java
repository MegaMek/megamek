/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org), Cord Awtry (kipsta@bs-interactive.com)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.PrintWriter;
import java.io.Serial;
import java.util.List;
import java.util.stream.IntStream;

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.LosEffects;
import megamek.common.MPCalculationSetting;
import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.common.rolls.PilotingRollData;
import megamek.logging.MMLogger;

public class QuadMek extends Mek {
    @Serial
    private static final long serialVersionUID = 7183093787457804717L;
    private static final MMLogger logger = MMLogger.create(QuadMek.class);

    private static final String[] LOCATION_NAMES = { "Head", "Center Torso", "Right Torso", "Left Torso",
                                                     "Front Right Leg", "Front Left Leg", "Rear Right Leg",
                                                     "Rear Left Leg" };

    private static final String[] LOCATION_ABBREVIATIONS = { "HD", "CT", "RT", "LT", "FRL", "FLL", "RRL", "RLL" };

    private static final int[] NUM_OF_SLOTS = { 6, 12, 12, 12, 6, 6, 6, 6 };

    public QuadMek(String inGyroType, String inCockpitType) {
        this(Mek.getGyroTypeForString(inGyroType), Mek.getCockpitTypeForString(inCockpitType));
    }

    public QuadMek() {
        this(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD);
    }

    public QuadMek(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);

        movementMode = EntityMovementMode.QUAD;
        originalMovementMode = EntityMovementMode.QUAD;

        setCritical(LOC_RIGHT_ARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_RIGHT_ARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_RIGHT_ARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_RIGHT_ARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));

        setCritical(LOC_LEFT_ARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_LEFT_ARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_LEFT_ARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_LEFT_ARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));
    }

    @Override
    public boolean cannotStandUpFromHullDown() {
        int i = 0;
        if (isLocationBad(LOC_LEFT_ARM)) {
            i++;
        }
        if (isLocationBad(LOC_RIGHT_ARM)) {
            i++;
        }
        if (isLocationBad(LOC_LEFT_LEG)) {
            i++;
        }
        if (isLocationBad(LOC_RIGHT_LEG)) {
            i++;
        }
        return i >= 3;
    }

    // PLAYTEST2 New Method for immobile due to no legs.
    @Override
    public boolean isImmobile() {
        if (gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
            int legsDestroyed = 0;
            int hipHits = 0;
            for (int i = 0; i < locations(); i++) {
                if (locationIsLeg(i)) {
                    if (isLocationBad(i)) {
                        legsDestroyed++;
                    } else if (legHasHipCrit(i)) {
                        hipHits++;
                    }
                }
            }
            if (legsDestroyed == 4 || ((hipHits == 4) && (getJumpMP() == 0))) {
                return true;
            }
        }
        return super.isImmobile();
    }


    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();

        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;
        int flHip = 0;
        int frHip = 0;
        int rlHip = 0;
        int rrHip = 0;
        int flActuators = 0;
        int frActuators = 0;
        int rlActuators = 0;
        int rrActuators = 0;

        // A Mek using tracks has its movement reduced by 25% per leg or track
        // destroyed.
        if (movementMode == EntityMovementMode.TRACKED) {
            for (Mounted<?> m : getMisc()) {
                if (m.getType().hasFlag(MiscType.F_TRACKS)) {
                    if (m.isHit() || isLocationBad(m.getLocation())) {
                        legsDestroyed++;
                    }
                }
            }
            mp = (mp * (4 - legsDestroyed)) / 4;
        } else {
            for (int i : List.of(Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_ARM)) {
                // PLAYTEST2 leg crits and MP
                if (!(game == null) && gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                    if (!isLocationBad(i)) {
                        if (legHasHipCrit(i)) {
                            if (i == Mek.LOC_LEFT_ARM) {
                                flHip++;
                            }
                            if (i == Mek.LOC_RIGHT_ARM) {
                                frHip++;
                            }
                            if (i == Mek.LOC_LEFT_LEG) {
                                rlHip++;
                            }
                            if (i == Mek.LOC_RIGHT_LEG) {
                                rrHip++;
                            }
                            hipHits++;
                            if ((game == null) ||
                                  !gameOptions()
                                        .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE)) {
                                continue;
                            }
                        }
                        if (i == Mek.LOC_LEFT_ARM) {
                            flActuators += countLegActuatorCrits(i);
                        }
                        if (i == Mek.LOC_RIGHT_ARM) {
                            frActuators += countLegActuatorCrits(i);
                        }
                        if (i == Mek.LOC_LEFT_LEG) {
                            rlActuators += countLegActuatorCrits(i);
                        }
                        if (i == Mek.LOC_RIGHT_LEG) {
                            rrActuators += countLegActuatorCrits(i);
                        }
                        actuatorHits += countLegActuatorCrits(i);
                    } else {
                        legsDestroyed++;
                    }
                } else {
                    if (!isLocationBad(i)) {
                        if (legHasHipCrit(i)) {
                            hipHits++;
                            if ((game == null) ||
                                  !gameOptions()
                                        .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE)) {
                                continue;
                            }
                        }
                        actuatorHits += countLegActuatorCrits(i);
                    } else {
                        legsDestroyed++;
                    }
                }
            }
        }
        // leg damage effects
        // PLAYTEST2 adjust for legs 2 and 3.
        if (legsDestroyed > 0) {
            if (legsDestroyed == 1) {
                mp--;
            } else if (game != null && gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                if (legsDestroyed == 4) {mp = 0;}
            } else if (legsDestroyed == 2) {
                mp = 1;
            } else {
                mp = 0;
            }
        }
        // PLAYTEST2 set reduction for hips and legs 2 and 3
        if ((game != null) && gameOptions().booleanOption(OptionsConstants.PLAYTEST_2) && (mp > 0)) {
            if (hipHits == 4) {
                mp = 0;
            } else {
                if (hipHits > 0 || (legsDestroyed > 1 && legsDestroyed < 4)) {
                    int minReduction;
                    int midReduction;
                    int maxReduction;
                    minReduction = (int) Math.ceil(mp / 2.0);
                    midReduction = (int) Math.ceil(minReduction / 2.0);
                    maxReduction = (int) Math.ceil(midReduction / 2.0);

                    if (hipHits == 1 || legsDestroyed == 2) {
                        if (hipHits == 0) {
                            // two legs destroyed, no hip hits
                            mp = mp - minReduction;
                        }
                        if (legsDestroyed < 2) {
                            // 1 or no legs destroyed, just a single hip
                            mp = mp - minReduction;
                        }
                        if (legsDestroyed == 2 && hipHits == 1) {
                            // Two legs and a hip
                            mp = mp - minReduction - midReduction;
                        }
                    } else if (hipHits == 2 || legsDestroyed == 3) {
                        if (hipHits == 0) {
                            // three legs destroyed, no hip hits
                            mp = mp - minReduction - midReduction;
                        }
                        if (legsDestroyed < 2) {
                            // 1 or no legs destroyed, just two hips
                            mp = mp - minReduction - midReduction;
                        }
                        if (legsDestroyed == 2 && hipHits == 2) {
                            // Two legs and two hips
                            mp = mp - minReduction - midReduction - maxReduction;
                        }
                    } else if (hipHits == 3) {
                        // Not possible to have 2 legs destroyed and 3 hips out
                        mp = mp - minReduction - midReduction - maxReduction;
                    }
                    if (flHip == 0) {
                        mp -= flActuators;
                    }
                    if (frHip == 0) {
                        mp -= frActuators;
                    }
                    if (rlHip == 0) {
                        mp -= rlActuators;
                    }
                    if (rrHip == 0) {
                        mp -= rrActuators;
                    }
                } else {
                    mp -= actuatorHits;
                }
            }
        } else {
            if (mp > 0) {
                if (hipHits > 0) {
                    if ((game != null) &&
                          gameOptions()
                                .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE)) {
                        mp = mp - (2 * hipHits);
                    } else {
                        for (int i = 0; i < hipHits; i++) {
                            mp = (int) Math.ceil(mp / 2.0);
                        }
                    }
                }
                mp -= actuatorHits;
            }
        }


        if (!mpCalculationSetting.ignoreModularArmor() && hasModularArmor()) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreChainDrape() && hasChainDrape()) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreHeat()) {
            // factor in heat
            if ((game != null) && gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT)) {
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
        if (((heat >= 9) || mpCalculationSetting.forceTSM()) &&
              hasTSM(false) &&
              (legsDestroyed < 2) &&
              !movementMode.isTracked() &&
              !movementMode.isWheeled()) {
            if (mpCalculationSetting.forceTSM() && mpCalculationSetting.ignoreHeat()) {
                // When forcing TSM but ignoring heat we must assume heat to be 9 to activate
                // TSM, this adds -1 MP!
                mp += 1;
            } else {
                mp += 2;
            }
        }

        if (!mpCalculationSetting.ignoreWeather() && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);

            if (getCrew().getOptions()
                  .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                  .equals(Crew.ENVIRONMENT_SPECIALIST_WIND) &&
                  conditions.getWeather().isClear() &&
                  conditions.getWind().isTornadoF1ToF3()) {
                mp += 1;
            }
        }

        if (!mpCalculationSetting.ignoreGravity()) {
            mp = applyGravityEffectsOnMP(mp);
        }

        return Math.max(0, mp);
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        if (countBadLegs() <= 1 ||
              (this instanceof QuadVee && getConversionMode() == QuadVee.CONV_MODE_VEHICLE && !convertingNow)) {
            return super.getRunMP(mpCalculationSetting);
        } else {
            return getWalkMP(mpCalculationSetting);
        }
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return hasQuirk(OptionsConstants.QUIRK_POS_EXT_TWIST) && !(isProne() || getAlreadyTwisted());
    }

    @Override
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            return (rotate <= 1) || (rotate == 5);
        } else {
            return rotate == 0;
        }
    }

    @Override
    public boolean locationIsLeg(int loc) {
        return ((loc == Mek.LOC_RIGHT_LEG) || (loc == Mek.LOC_LEFT_LEG) || (loc == Mek.LOC_RIGHT_ARM) || (loc
              == Mek.LOC_LEFT_ARM));
    }

    @Override
    public int getWeaponArc(int weaponNumber) {
        final Mounted<?> mounted = getEquipment(weaponNumber);

        // B-Pods need to be special-cased, they have 360 firing arc
        if ((mounted.getType() instanceof WeaponType) && mounted.getType().hasFlag(WeaponType.F_B_POD)) {
            return Compute.ARC_360;
        }
        // VGLs base arc on their facing
        if (mounted.getType().hasFlag(WeaponType.F_VGL)) {
            return Compute.firingArcFromVGLFacing(mounted.getFacing());
        }
        // rear mounted?
        if (mounted.isRearMounted()) {
            return Compute.ARC_REAR;
        }
        // front mounted
        return switch (mounted.getLocation()) {
            case LOC_HEAD, LOC_CENTER_TORSO,
                 LOC_RIGHT_TORSO, LOC_LEFT_TORSO, LOC_RIGHT_LEG, LOC_LEFT_LEG, LOC_LEFT_ARM, LOC_RIGHT_ARM ->
                  Compute.ARC_FORWARD;
            default -> Compute.ARC_360;
        };
    }

    @Override
    public boolean isQuadMek() {
        return true;
    }

    @Override
    public void setInternal(int head, int ct, int t, int arm, int leg) {
        initializeInternal(head, LOC_HEAD);
        initializeInternal(ct, LOC_CENTER_TORSO);
        initializeInternal(t, LOC_RIGHT_TORSO);
        initializeInternal(t, LOC_LEFT_TORSO);
        initializeInternal(leg, LOC_RIGHT_ARM);
        initializeInternal(leg, LOC_LEFT_ARM);
        initializeInternal(leg, LOC_RIGHT_LEG);
        initializeInternal(leg, LOC_LEFT_LEG);
    }

    @Override
    public boolean needsRollToStand() {
        return countBadLegs() != 0;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        int destroyedLegs;

        destroyedLegs = countBadLegs();

        // QuadVees lose the bonus when converting.
        if (destroyedLegs == 0 && !convertingNow) {
            roll.addModifier(-2, "Quad bonus");
        }

        if (hasAbility(OptionsConstants.PILOT_ANIMAL_MIMIC)) {
            roll.addModifier(-1, "Animal Mimicry");
        }

        if (hasFunctionalLegAES()) {
            roll.addModifier(-2, "AES bonus");
        }

        boolean destroyedLegCounted = false;
        for (int loc : List.of(Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_ARM)) {
            // PLAYTEST2 destroyed leg
            if (isLocationBad(loc)) {
                // a quad with 2 destroyed legs acts like a biped with one leg
                // destroyed, so add the +5 only once
                // 3 or more destroyed legs are being taken care in
                // getBasePiloting
                if ((destroyedLegs == 2) && !destroyedLegCounted) {
                    // PLAYTEST2 pilot is +4 now
                    if (gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                        roll.addModifier(4, "2 legs destroyed");
                    } else {
                        roll.addModifier(5, "2 legs destroyed");
                    }
                    destroyedLegCounted = true;
                }
            } else {
                // check for damaged hip actuators
                if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) {
                    // PLAYTEST2 now a +1 not +2
                    if (gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                        roll.addModifier(1, getLocationName(loc) + " Hip Actuator destroyed");
                    } else {
                        roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                    }
                    if (!gameOptions()
                          .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE)) {
                        continue;
                    }
                }
                // upper leg actuators?
                if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Upper Leg Actuator destroyed");
                }
                // lower leg actuators?
                if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Lower Leg Actuator destroyed");
                }
                // foot actuators?
                // PLAYTEST2 no more +1 for foot actuators
                if (!(gameOptions().booleanOption(OptionsConstants.PLAYTEST_2))) {
                    if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_FOOT, loc) > 0) {
                        roll.addModifier(1, getLocationName(loc) + " Foot Actuator destroyed");
                    }
                }
            }
        }

        return super.addEntityBonuses(roll);
    }

    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
    }

    @Override
    protected double getArmActuatorCost() {
        return 0;
    }

    @Override
    protected double getLegActuatorCost() {
        return (weight * 150 * 4) + (weight * 80 * 4) + (weight * 120 * 4);
    }

    @Override
    public String joinLocationAbbr(List<Integer> locations, int limit) {
        // If we need to abbreviate something that occupies all leg locations, simply
        // return "Legs"
        if ((locations.size() > limit) && (locations.size() == 4) && locations.stream().allMatch(this::locationIsLeg)) {
            return "Legs";
        } else {
            return super.joinLocationAbbr(locations, limit);
        }
    }

    @Override
    public HitData innerRollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        int roll;

        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {
            roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }
        }

        if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)) {
            if ((table == ToHitData.HIT_NORMAL) || (table == ToHitData.HIT_PARTIAL_COVER)) {
                roll = Compute.d6(2);
                try {
                    PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();
                    if (pw != null) {
                        pw.print(table);
                        pw.print("\t");
                        pw.print(side);
                        pw.print("\t");
                        pw.println(roll);
                    }
                } catch (Throwable t) {
                    logger.error("", t);
                }

                if (side == ToHitData.SIDE_FRONT) {
                    // normal front hits
                    switch (roll) {
                        case 2:
                            return tac(table, side, Mek.LOC_CENTER_TORSO, cover, false);
                        case 3:
                            return new HitData(Mek.LOC_LEFT_LEG);
                        case 4:
                        case 5:
                            return new HitData(Mek.LOC_LEFT_ARM);
                        case 6:
                            return new HitData(Mek.LOC_LEFT_TORSO);
                        case 7:
                            return new HitData(Mek.LOC_CENTER_TORSO);
                        case 8:
                            return new HitData(Mek.LOC_RIGHT_TORSO);
                        case 9:
                        case 10:
                            return new HitData(Mek.LOC_RIGHT_ARM);
                        case 11:
                            return new HitData(Mek.LOC_RIGHT_LEG);
                        case 12:
                            return new HitData(Mek.LOC_HEAD);
                    }
                } else if (side == ToHitData.SIDE_REAR) {
                    switch (roll) {
                        case 2:
                            return tac(table, side, Mek.LOC_CENTER_TORSO, cover, true);
                        case 3:
                            return new HitData(Mek.LOC_LEFT_ARM, true);
                        case 4:
                        case 5:
                            return new HitData(Mek.LOC_LEFT_LEG, true);
                        case 6:
                            return new HitData(Mek.LOC_LEFT_TORSO, true);
                        case 7:
                            return new HitData(Mek.LOC_CENTER_TORSO, true);
                        case 8:
                            return new HitData(Mek.LOC_RIGHT_TORSO, true);
                        case 9:
                        case 10:
                            return new HitData(Mek.LOC_RIGHT_LEG, true);
                        case 11:
                            return new HitData(Mek.LOC_RIGHT_ARM, true);
                        case 12:
                            return new HitData(Mek.LOC_HEAD, true);
                    }
                } else if (side == ToHitData.SIDE_LEFT) {
                    switch (roll) {
                        case 2:
                            return tac(table, side, Mek.LOC_LEFT_TORSO, cover, false);
                        case 3:
                            return new HitData(Mek.LOC_RIGHT_ARM);
                        case 4:
                        case 5:
                            return new HitData(Mek.LOC_LEFT_ARM);
                        case 6:
                            return new HitData(Mek.LOC_RIGHT_TORSO);
                        case 7:
                            return new HitData(Mek.LOC_LEFT_TORSO);
                        case 8:
                            return new HitData(Mek.LOC_CENTER_TORSO);
                        case 9:
                        case 10:
                            return new HitData(Mek.LOC_LEFT_LEG);
                        case 11:
                            return new HitData(Mek.LOC_RIGHT_LEG);
                        case 12:
                            return new HitData(Mek.LOC_HEAD);
                    }
                } else if (side == ToHitData.SIDE_RIGHT) {
                    switch (roll) {
                        case 2:
                            return tac(table, side, Mek.LOC_RIGHT_TORSO, cover, false);
                        case 3:
                            return new HitData(Mek.LOC_LEFT_ARM);
                        case 4:
                        case 5:
                            return new HitData(Mek.LOC_RIGHT_ARM);
                        case 6:
                            return new HitData(Mek.LOC_CENTER_TORSO);
                        case 7:
                            return new HitData(Mek.LOC_RIGHT_TORSO);
                        case 8:
                            return new HitData(Mek.LOC_LEFT_TORSO);
                        case 9:
                        case 10:
                            return new HitData(Mek.LOC_RIGHT_LEG);
                        case 11:
                            return new HitData(Mek.LOC_LEFT_LEG);
                        case 12:
                            return new HitData(Mek.LOC_HEAD);
                    }
                }
            }
        }

        boolean playtestLocations = gameOptions().booleanOption(OptionsConstants.PLAYTEST_1);

        if (table == ToHitData.HIT_PUNCH) {
            roll = Compute.d6();
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable t) {
                logger.error("", t);
            }

            if (playtestLocations && (side == ToHitData.SIDE_LEFT || side == ToHitData.SIDE_RIGHT)) {
                return getPlaytestSideLocation(table, side, cover);
            }

            if (side == ToHitData.SIDE_FRONT) {
                switch (roll) {
                    case 1:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 2:
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 4:
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 6:
                        return new HitData(Mek.LOC_HEAD, true);
                }
            } else if (side == ToHitData.SIDE_REAR) {
                switch (roll) {
                    case 1:
                        return new HitData(Mek.LOC_LEFT_LEG, true);
                    case 2:
                        return new HitData(Mek.LOC_LEFT_TORSO, true);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO, true);
                    case 4:
                        return new HitData(Mek.LOC_RIGHT_TORSO, true);
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_LEG, true);
                    case 6:
                        return new HitData(Mek.LOC_HEAD, true);
                }
            } else if (side == ToHitData.SIDE_LEFT) {
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 4:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 5:
                        return new HitData(Mek.LOC_LEFT_LEG);
                    case 6:
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_RIGHT) {
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 4:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_LEG);
                    case 6:
                        return new HitData(Mek.LOC_HEAD);
                }
            }
        } else if (table == ToHitData.HIT_KICK) {
            roll = Compute.d6(1);
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable t) {
                logger.error("", t);
            }

            boolean left = (roll <= 3);
            if (side == ToHitData.SIDE_FRONT) {
                return left ? new HitData(Mek.LOC_LEFT_ARM) : new HitData(Mek.LOC_RIGHT_ARM);
            } else if (side == ToHitData.SIDE_REAR) {
                return left ? new HitData(Mek.LOC_LEFT_LEG) : new HitData(Mek.LOC_RIGHT_LEG);
            } else if (side == ToHitData.SIDE_LEFT) {
                return left ? new HitData(Mek.LOC_LEFT_LEG) : new HitData(Mek.LOC_LEFT_ARM);
            } else if (side == ToHitData.SIDE_RIGHT) {
                return left ? new HitData(Mek.LOC_RIGHT_ARM) : new HitData(Mek.LOC_RIGHT_LEG);
            }
        } else if ((table == ToHitData.HIT_SWARM) || (table == ToHitData.HIT_SWARM_CONVENTIONAL)) {
            int effects;
            if (table == ToHitData.HIT_SWARM_CONVENTIONAL) {
                effects = HitData.EFFECT_NONE;
            } else {
                effects = HitData.EFFECT_CRITICAL;
            }
            roll = Compute.d6(2);
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable t) {
                logger.error("", t);
            }
            // Swarm attack locations.
            switch (roll) {
                case 2, 12:
                    return new HitData(Mek.LOC_HEAD, false, effects);
                case 3, 6:
                    return new HitData(Mek.LOC_RIGHT_TORSO, false, effects);
                case 4, 10:
                    return new HitData(Mek.LOC_CENTER_TORSO, true, effects);
                case 5:
                    return new HitData(Mek.LOC_RIGHT_TORSO, true, effects);
                case 7:
                    return new HitData(Mek.LOC_CENTER_TORSO, false, effects);
                case 8, 11:
                    return new HitData(Mek.LOC_LEFT_TORSO, false, effects);
                case 9:
                    return new HitData(Mek.LOC_LEFT_TORSO, true, effects);
            }
        }
        return super.innerRollHitLocation(table, side, aimedLocation, aimingMode, cover);
    }

    @Override
    public boolean removePartialCoverHits(int location, int cover, int side) {
        // treat front legs like legs not arms.

        // Handle upper cover specially, as treating it as a bitmask will lead
        // to every location being covered
        if (cover == LosEffects.COVER_UPPER) {
            return (location != LOC_LEFT_LEG) && (location != LOC_RIGHT_LEG) && (location != LOC_LEFT_ARM) && (location
                  !=
                  LOC_RIGHT_ARM);
        }

        // left and right cover are from attacker's POV.
        // if hitting front arc, need to swap them
        if (side == ToHitData.SIDE_FRONT) {
            if (((cover & LosEffects.COVER_LOW_RIGHT) != 0) &&
                  ((location == Mek.LOC_LEFT_ARM) || (location == Mek.LOC_LEFT_LEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOW_LEFT) != 0) &&
                  ((location == Mek.LOC_RIGHT_ARM) || (location == Mek.LOC_RIGHT_LEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_RIGHT) != 0) &&
                  ((location == Mek.LOC_LEFT_ARM) || (location == Mek.LOC_LEFT_TORSO) || (location
                        == Mek.LOC_LEFT_LEG))) {
                return true;
            }
            return ((cover & LosEffects.COVER_LEFT) != 0) &&
                  ((location == Mek.LOC_RIGHT_ARM) || (location == Mek.LOC_RIGHT_TORSO) || (location
                        == Mek.LOC_RIGHT_LEG));
        } else {
            if (((cover & LosEffects.COVER_LOW_LEFT) != 0) &&
                  ((location == Mek.LOC_LEFT_ARM) || (location == Mek.LOC_LEFT_LEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOW_RIGHT) != 0) &&
                  ((location == Mek.LOC_RIGHT_ARM) || (location == Mek.LOC_RIGHT_LEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LEFT) != 0) &&
                  ((location == Mek.LOC_LEFT_ARM) || (location == Mek.LOC_LEFT_TORSO) || (location
                        == Mek.LOC_LEFT_LEG))) {
                return true;
            }
            return ((cover & LosEffects.COVER_RIGHT) != 0) &&
                  ((location == Mek.LOC_RIGHT_ARM) || (location == Mek.LOC_RIGHT_TORSO) || (location
                        == Mek.LOC_RIGHT_LEG));
        }
    }

    @Override
    public boolean canGoHullDown() {
        // check the option
        boolean retVal = gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN);
        if (!retVal) {
            return false;
        }
        // check the locations
        int[] locations = { Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_ARM, Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG };
        int badLocs = 0;
        for (int loc = locations.length - 1; loc >= 0; loc--) {
            if (isLocationBad(locations[loc]) || isLocationDoomed(locations[loc])) {
                badLocs++;
            }
        }

        return (badLocs < 2) && !isGyroDestroyed();
    }

    @Override
    public boolean isArm(int loc) {
        return false;
    }

    @Override
    public boolean hasMPReducingHardenedArmor() {
        return IntStream.of(LOC_LEFT_LEG, LOC_RIGHT_LEG, LOC_LEFT_ARM, LOC_RIGHT_ARM)
              .anyMatch(i -> (armorType[i] == EquipmentType.T_ARMOR_HARDENED));
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_MEK | Entity.ETYPE_QUAD_MEK;
    }

    @Override
    public boolean hasClaw(int location) {
        return false;
    }

    @Override
    protected int legCount() {
        return 4;
    }
}
