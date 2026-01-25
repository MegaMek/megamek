/*
 * Copyright (c) 2017-2025 - The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;

import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.MPCalculationSetting;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.MPBoosters;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.PilotingRollData;

/**
 * Quad Mek that can convert into either tracked or wheeled vehicle mode.
 *
 * @author Neoancient
 */
public class QuadVee extends QuadMek {
    @Serial
    private static final long serialVersionUID = 1283551018632228647L;

    public static final int CONV_MODE_MEK = 0;
    public static final int CONV_MODE_VEHICLE = 1;

    public static final int SYSTEM_CONVERSION_GEAR = 15;

    public static final String[] systemNames = { "Life Support", "Sensors", "Cockpit", "Engine", "Gyro", null, null,
                                                 "Shoulder", "Upper Arm", "Lower Arm", "Hand", "Hip", "Upper Leg",
                                                 "Lower Leg", "Foot", "Conversion Gear" };

    public static final int MOTIVE_UNKNOWN = -1;
    public static final int MOTIVE_TRACK = 0;
    public static final int MOTIVE_WHEEL = 1;

    public static final String[] MOTIVE_STRING = { "Track", "Wheel" };

    private int motiveType;

    public QuadVee() {
        this(GYRO_STANDARD, MOTIVE_TRACK);
    }

    public QuadVee(int inGyroType, int inMotiveType) {
        super(inGyroType, COCKPIT_QUADVEE);

        motiveType = inMotiveType;

        setCritical(LOC_RIGHT_ARM, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
        setCritical(LOC_LEFT_ARM, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
        setCritical(LOC_RIGHT_LEG, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
        setCritical(LOC_LEFT_LEG, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
    }

    @Override
    public String getSystemName(int index) {
        if (index == SYSTEM_GYRO) {
            return Mek.getGyroDisplayString(gyroType);
        }
        if (index == SYSTEM_COCKPIT) {
            return Mek.getCockpitDisplayString(cockpitType);
        }
        return systemNames[index];
    }

    @Override
    public String getRawSystemName(int index) {
        return systemNames[index];
    }

    /**
     * @return MOTIVE_TRACK or MOTIVE_WHEEL
     */
    public int getMotiveType() {
        return motiveType;
    }

    public void setMotiveType(int motiveType) {
        this.motiveType = motiveType;
    }

    public String getMotiveTypeString(int motiveType) {
        if (motiveType < 0 || motiveType >= MOTIVE_STRING.length) {
            return "Unknown";
        }
        return MOTIVE_STRING[motiveType];
    }

    public String getMotiveTypeString() {
        return getMotiveTypeString(getMotiveType());
    }

    public static int getMotiveTypeForString(String inType) {
        if ((inType == null) || (inType.isEmpty())) {
            return MOTIVE_UNKNOWN;
        }
        for (int x = 0; x < MOTIVE_STRING.length; x++) {
            if (inType.equals(MOTIVE_STRING[x])) {
                return x;
            }
        }
        return MOTIVE_UNKNOWN;
    }

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return new TechAdvancement(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F)
              .setClanAdvancement(3130, 3135, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    /**
     * This is used to identify MEKs that have tracks mounted as industrial equipment.
     */
    @Override
    public boolean hasTracks() {
        return false;
    }

    @Override
    public CrewType defaultCrewType() {
        return CrewType.QUADVEE;
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        // Current MP is calculated differently depending on whether the QuadVee is in MEK
        // or vehicle mode. During conversion, we use the mode we started in:
        // bg.battletech.com/forums/index.php?topic=55261.msg1271935#msg1271935
        if (!mpCalculationSetting.ignoreConversion() && (getConversionMode() == CONV_MODE_VEHICLE)) {
            return getCruiseMP(mpCalculationSetting);
        } else {
            return super.getWalkMP(mpCalculationSetting);
        }
    }

    /**
     * In vehicle mode the QuadVee ignores actuator and hip criticalSlots, but is subject to track/wheel damage and
     * various effects of vehicle motive damage.
     */
    public int getCruiseMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();
        if (getMotiveType() == MOTIVE_WHEEL) {
            mp++;
        }

        // If a leg or its track/wheel is destroyed, it reduces movement by a cumulative -1/4 mp per leg.
        // https://bg.battletech.com/forums/index.php?topic=63281.msg1469243#msg1469243
        int badTracks = 0;
        for (int loc = 0; loc < locations(); loc++) {
            if (locationIsLeg(loc) && (isLocationBad(loc) || getCritical(loc, 5).isHit())) {
                badTracks++;
            }
        }

        if (badTracks == 4) {
            return 0;
        } else if (badTracks > 0) {
            mp -= mp * badTracks / 4;
        }

        if (!mpCalculationSetting.ignoreModularArmor() && hasModularArmor()) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreHeat()) {
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

        if (!mpCalculationSetting.ignoreWeather() && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);

            if (getCrew().getOptions()
                  .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                  .equals(Crew.ENVIRONMENT_SPECIALIST_WIND)
                  && conditions.getWeather().isClear()
                  && conditions.getWind().isTornadoF1ToF3()) {
                mp += 1;
            }
        }

        if (!mpCalculationSetting.ignoreGravity()) {
            mp = applyGravityEffectsOnMP(mp);
        }

        return Math.max(0, mp);
    }

    @Override
    public int getSprintMP(MPCalculationSetting mpCalculationSetting) {
        if (!mpCalculationSetting.ignoreConversion() && (getConversionMode() == CONV_MODE_VEHICLE)
              && ((game == null) ||
              !gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS))) {
            return getRunMP(mpCalculationSetting);
        } else {
            return super.getSprintMP(mpCalculationSetting);
        }
    }

    /*
     * No jumping in vehicle mode.
     */
    @Override
    public int getJumpMP(MPCalculationSetting mpCalculationSetting) {
        if (!mpCalculationSetting.ignoreConversion()
              && ((getConversionMode() == CONV_MODE_VEHICLE) || convertingNow)) {
            return 0;
        } else {
            return super.getJumpMP(mpCalculationSetting);
        }
    }

    /*
     * In a QuadVee they're all torso jump jets. But they still don't work in vehicle mode.
     */
    @Override
    public int torsoJumpJets() {
        if (getConversionMode() == CONV_MODE_VEHICLE || convertingNow) {
            return 0;
        }
        return super.torsoJumpJets();
    }

    /**
     * UMUs do not function in vehicle mode
     */
    @Override
    public int getActiveUMUCount() {
        if (getConversionMode() == CONV_MODE_VEHICLE || convertingNow) {
            return 0;
        }
        return super.getActiveUMUCount();
    }

    /**
     * QuadVees cannot benefit from MASC or SuperChargers in vehicle mode
     */
    @Override
    public MPBoosters getArmedMPBoosters() {
        MPBoosters mpBoosters = super.getArmedMPBoosters();
        if (getConversionMode() != CONV_MODE_VEHICLE) {
            return mpBoosters;
        }

        return MPBoosters.NONE;
    }

    /**
     * No movement heat generated in vehicle mode
     */
    @Override
    public int getStandingHeat() {
        if (getConversionMode() == CONV_MODE_VEHICLE && !convertingNow) {
            return 0;
        }
        return super.getStandingHeat();
    }

    @Override
    public int getWalkHeat() {
        if (getConversionMode() == CONV_MODE_VEHICLE && !convertingNow) {
            return 0;
        }
        return super.getWalkHeat();
    }

    @Override
    public int getRunHeat() {
        if (getConversionMode() == CONV_MODE_VEHICLE && !convertingNow) {
            return 0;
        }
        return super.getRunHeat();
    }

    @Override
    public int getSprintHeat() {
        if (getConversionMode() == CONV_MODE_VEHICLE && !convertingNow) {
            return 0;
        }
        return super.getSprintHeat();
    }

    /**
     * Overrides to return false in vehicle mode. Technically it still has a hip crit, but it has no effect.
     */
    @Override
    public boolean hasHipCrit() {
        if (getConversionMode() == CONV_MODE_VEHICLE && !convertingNow) {
            return false;
        }
        return super.hasHipCrit();
    }

    @Override
    public EntityMovementMode nextConversionMode(EntityMovementMode afterMode) {
        if (afterMode.isTrackedOrWheeled()) {
            return originalMovementMode;
        } else if (motiveType == MOTIVE_WHEEL) {
            return EntityMovementMode.WHEELED;
        } else {
            return EntityMovementMode.TRACKED;
        }
    }

    @Override
    public void setMovementMode(EntityMovementMode mode) {
        if (mode.isTrackedOrWheeled()) {
            setConversionMode(CONV_MODE_VEHICLE);
        } else {
            setConversionMode(CONV_MODE_MEK);
        }
        super.setMovementMode(mode);
    }

    @Override
    public void setConversionMode(int mode) {
        if (mode == getConversionMode()) {
            return;
        }
        if (mode == CONV_MODE_MEK) {
            super.setMovementMode(EntityMovementMode.QUAD);
        } else if (mode == CONV_MODE_VEHICLE) {
            super.setMovementMode(motiveType == MOTIVE_WHEEL
                  ? EntityMovementMode.WHEELED : EntityMovementMode.TRACKED);
        } else {
            return;
        }
        super.setConversionMode(mode);
    }

    @Override
    public boolean isEligibleForPavementOrRoadBonus() {
        // Since pavement bonus only applies if driving on pavement the entire turn,
        // there is no pavement bonus unless it spends the entire turn in vehicle mode.
        return getConversionMode() == CONV_MODE_VEHICLE && !convertingNow;
    }

    @Override
    public boolean canFall(boolean gyroLegDamage) {
        // QuadVees cannot fall due to failed PSR in vehicle mode.
        return (getConversionMode() == CONV_MODE_MEK && !isProne()) ||
              (convertingNow && game.getPhase().isMovement() && !isProne());
    }

    /**
     * Computes conversion cost.
     *
     * @return The cost to convert between quad and vehicle modes.
     */
    public int conversionCost() {
        int cost = 2;
        // Base cost 2, +1 for each damaged leg actuator, conversion equipment, or track slot
        for (int loc = LOC_RIGHT_ARM; loc <= LOC_LEFT_LEG; loc++) {
            for (int slot = 0; slot < 5; slot++) {
                if (getCritical(loc, slot).isHit()) {
                    cost++;
                }
            }
        }
        for (MiscMounted m : getMisc()) {
            if (m.getType().hasFlag(MiscType.F_TRACKS)) {
                cost += m.getDamageTaken();
                break;
            }
        }
        return cost;
    }

    /**
     * In vehicle mode the QuadVee is at the same level as the terrain.
     */
    @Override
    public int height() {
        if (getConversionMode() == CONV_MODE_VEHICLE) {
            return 0;
        }
        return super.height();
    }

    @Override
    public int getMaxElevationChange() {
        if (getConversionMode() == CONV_MODE_VEHICLE) {
            return 1;
        }
        return 2;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return true;
    }

    /**
     * Can this mek torso twist in the given direction?
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        if (!canChangeSecondaryFacing()) {
            return dir == 0;
        }
        // Turret rotation always works in vehicle mode.
        if (getConversionMode() == CONV_MODE_VEHICLE) {
            return true;
        }

        // In 'Mek mode the torso rotation can be limited by gyro damage.
        int gyroHits = getGyroHits();
        if (getGyroType() == GYRO_HEAVY_DUTY) {
            gyroHits--;
        }
        // No damage gives full rotation
        if (gyroHits <= 0) {
            return true;
        }
        int rotate = Math.abs(dir - getFacing());
        // The first hit prevents rotating directly to the rear
        if (gyroHits == 1) {
            return rotate != 3;
        }
        // Destroyed gyro limits to normal biped torso rotation
        return rotate <= 1 || rotate == 5;
    }

    /**
     * Add in any piloting skill mods
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        if (!getCrew().hasDedicatedPilot()) {
            roll.addModifier(2, "pilot incapacitated");
        }

        if (getConversionMode() == CONV_MODE_VEHICLE) {
            for (int loc = 0; loc < locations(); loc++) {
                if (locationIsLeg(loc)
                      && (isLocationBad(loc) || getCritical(loc, 5).isHit())) {
                    roll.addModifier(+3, "motive system damage");
                    break;
                }
            }
            // are we wheeled and in light snow?
            Hex hex = game.getHexOf(this);
            if ((null != hex) && getMovementMode().isWheeled()
                  && (hex.terrainLevel(Terrains.SNOW) == 1)) {
                roll.addModifier(1, "thin snow");
            }
            // VDNI bonus? (BVDNI does NOT get piloting bonus due to "neuro-lag" per IO pg 71)
            // When tracking neural interface hardware, require DNI cockpit mod for benefits
            if (hasActiveDNI()) {
                if (hasAbility(OptionsConstants.MD_VDNI) && !hasAbility(OptionsConstants.MD_BVDNI)) {
                    roll.addModifier(-1, "VDNI");
                } else if (hasAbility(OptionsConstants.MD_BVDNI)) {
                    roll.addModifier(0, "BVDNI (no piloting bonus)");
                }
            }
            if (hasQuirk(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)
                  && !hasAbility(OptionsConstants.UNOFFICIAL_SMALL_PILOT)) {
                roll.addModifier(1, "cramped cockpit");
            }

            if (hasHardenedArmor()) {
                roll.addModifier(1, "Hardened Armor");
            }

            if (hasModularArmor()) {
                roll.addModifier(1, "Modular Armor");
            }
            return roll;
        } else {
            return super.addEntityBonuses(roll);
        }
    }

    @Override
    public boolean usesTurnMode() {
        return getConversionMode() == CONV_MODE_VEHICLE && !convertingNow
              && game != null && gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TURN_MODE);
    }

    /**
     * If the QuadVee is in vehicle mode (or converting to it) then it follows the rules for tanks going hull-down,
     * which requires a fortified hex.
     *
     * @return True if hull-down is enabled and the QuadVee is in a fortified hex.
     */
    @Override
    public boolean canGoHullDown() {
        if (getConversionMode() == CONV_MODE_VEHICLE != convertingNow) {
            Hex occupiedHex = game.getHexOf(this);
            return occupiedHex.containsTerrain(Terrains.FORTIFIED)
                  && gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN);
        }
        return super.canGoHullDown();
    }

    /**
     * Cannot make any physical attacks in vehicle mode except charging, which is handled in the movement phase.
     */
    @Override
    public boolean isEligibleForPhysical() {
        return getConversionMode() == CONV_MODE_MEK && super.isEligibleForPhysical();
    }

    @Override
    public String getTilesetModeString() {
        if (getConversionMode() == CONV_MODE_VEHICLE) {
            return "_VEHICLE";
        } else {
            return "";
        }
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_MEK | Entity.ETYPE_QUAD_MEK | Entity.ETYPE_QUADVEE;
    }
}
