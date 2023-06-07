/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.enums.MPBoosters;
import megamek.common.options.OptionsConstants;

/**
 * Quad Mek that can convert into either tracked or wheeled vehicle mode.
 *
 * @author Neoancient
 */
public class QuadVee extends QuadMech {
    private static final long serialVersionUID = 1283551018632228647L;

    public static final int CONV_MODE_MECH    = 0;
    public static final int CONV_MODE_VEHICLE = 1;

    public static final int SYSTEM_CONVERSION_GEAR = 15;

    public static final String[] systemNames = { "Life Support", "Sensors",
            "Cockpit", "Engine", "Gyro", null, null, "Shoulder", "Upper Arm",
            "Lower Arm", "Hand", "Hip", "Upper Leg", "Lower Leg", "Foot",
            "Conversion Gear"};

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

        setCritical(LOC_RARM, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
        setCritical(LOC_LARM, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
        setCritical(LOC_RLEG, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
        setCritical(LOC_LLEG, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
    }

    @Override
    public String getSystemName(int index) {
        if (index == SYSTEM_GYRO) {
            return Mech.getGyroDisplayString(gyroType);
        }
        if (index == SYSTEM_COCKPIT) {
            return Mech.getCockpitDisplayString(cockpitType);
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
            return MOTIVE_STRING[MOTIVE_UNKNOWN];
        }
        return MOTIVE_STRING[motiveType];
    }

    public String getMotiveTypeString() {
        return getMotiveTypeString(getMotiveType());
    }

    public static int getMotiveTypeForString(String inType) {
        if ((inType == null) || (inType.length() < 1)) {
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
    return new TechAdvancement(TECH_BASE_CLAN)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
            .setClanAdvancement(3130, 3135, DATE_NONE, DATE_NONE, DATE_NONE)
            .setClanApproximate(true)
            .setPrototypeFactions(F_CHH)
            .setProductionFactions(F_CHH)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    /**
     * This is used to identify Mechs that have tracks mounted as industrial equipment.
     */
    @Override
    public boolean hasTracks() {
        return false;
    }

    /**
     * Current MP is calculated differently depending on whether the QuadVee is in Mech
     * or vehicle mode. During conversion we use the mode we started in:
     * bg.battletech.com/forums/index.php?topic=55261.msg1271935#msg1271935
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if (getConversionMode() == CONV_MODE_VEHICLE) {
            return getCruiseMP(gravity, ignoreheat, ignoremodulararmor);
        } else {
            return super.getWalkMP(gravity, ignoreheat, ignoremodulararmor);
        }
    }

    /**
     * In vehicle mode the QuadVee ignores actuator and hip criticals, but is subject to track/wheel
     * damage and various effects of vehicle motive damage.
     */
    public int getCruiseMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int wmp = getOriginalWalkMP();
        //Bonus for wheeled movement
        if (getMotiveType() == MOTIVE_WHEEL) {
            wmp++;
        }

        // If a leg or its track/wheel is destroyed, it is treated as major motive system damage,
        // which we are interpreting as a cumulative 1/2 MP.
        // bg.battletech.com/forums/index.php?topic=55261.msg1271935#msg1271935
        int badTracks = 0;
        for (int loc = 0; loc < locations(); loc++) {
            if (locationIsLeg(loc)
                    && (isLocationBad(loc) || getCritical(loc, 5).isHit())) {
                badTracks++;
            }
        }

        if (badTracks == 4) {
            return 0;
        } else if (badTracks > 1) {
            wmp = wmp / (1 << badTracks);
        }

        // Now apply modifiers
        if (!ignoremodulararmor && hasModularArmor()) {
            wmp--;
        }

        if (!ignoreheat) {
            // factor in heat
            if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT)) {
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
        }
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if (weatherMod != 0) {
                wmp = Math.max(wmp + weatherMod, 0);
            }

            if(getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)
                    && (game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WE_NONE)
                    && (game.getPlanetaryConditions().getWindStrength() == PlanetaryConditions.WI_TORNADO_F13)) {
                wmp += 1;
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

    @Override
    public int getSprintMP() {
        if (getConversionMode() == CONV_MODE_VEHICLE && (game == null || !game.getOptions()
                .booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS))) {
            return getRunMP();
        }
        return getSprintMP(true, false, false);
    }

    @Override
    public int getSprintMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if (getConversionMode() == CONV_MODE_VEHICLE && (game == null || !game.getOptions()
                .booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS))) {
            return getRunMP(gravity, ignoreheat, ignoremodulararmor);
        }
        return super.getSprintMP(gravity, ignoreheat, ignoremodulararmor);
    }

    @Override
    public int getSprintMPwithoutMASC(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        if (getConversionMode() == CONV_MODE_VEHICLE) {
            if (game == null || !game.getOptions()
                    .booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS)) {
                return getRunMPwithoutMASC(gravity, ignoreheat, ignoremodulararmor);
            } else {
                return (int) Math.ceil(getWalkMP(gravity, ignoreheat,
                        ignoremodulararmor) * 2.0);
            }
        } else {
            return super.getSprintMPwithoutMASC(gravity, ignoreheat, ignoremodulararmor);
        }
    }

    @Override
    public int getOriginalSprintMPwithoutMASC() {
        if (getConversionMode() == CONV_MODE_VEHICLE && (game == null || !game.getOptions()
                .booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS))) {
            return getOriginalRunMP();
        } else {
            return (int) Math.ceil(getOriginalWalkMP() * 2.0);
        }
    }
    /*
     * No jumping in vehicle mode.
     */
    @Override
    public int getJumpMP(boolean gravity, boolean ignoremodulararmor) {
        if (getConversionMode() == CONV_MODE_VEHICLE || convertingNow) {
            return 0;
        }
        return super.getJumpMP(gravity, ignoremodulararmor);
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
     * QuadVees cannot benefit from MASC in vehicle mode
     */
    @Override
    public MPBoosters getArmedMPBoosters() {
        MPBoosters mpBoosters = super.getArmedMPBoosters();
        if (getConversionMode() != CONV_MODE_VEHICLE) {
            return  mpBoosters;
        }

        switch (mpBoosters) {
            case MASC_AND_SUPERCHARGER:
                return MPBoosters.SUPERCHARGER_ONLY;
            case MASC_ONLY:
                return MPBoosters.NONE;
            default:
                return mpBoosters;
        }
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
     * Overrides to return false in vehicle mode. Technically it still has a hip crit, but it has no
     * effect.
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
            setConversionMode(CONV_MODE_MECH);
        }
        super.setMovementMode(mode);
    }

    @Override
    public void setConversionMode(int mode) {
        if (mode == getConversionMode()) {
            return;
        }
        if (mode == CONV_MODE_MECH) {
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
    public boolean isEligibleForPavementBonus() {
        // Since pavement bonus only applies if driving on pavement the entire turn,
        // there is no pavement bonus unless it spends the entire turn in vehicle mode.
        return getConversionMode() == CONV_MODE_VEHICLE && !convertingNow;
    }

    @Override
    public boolean canFall(boolean gyroLegDamage) {
        // QuadVees cannot fall due to failed PSR in vehicle mode.
        return getConversionMode() == CONV_MODE_MECH || convertingNow;
    }

    /**
     * The cost to convert between quad and vehicle modes.
     * @return
     */
    public int conversionCost() {
        int cost = 2;
        // Base cost 2, +1 for each damaged leg actuator, conversion equipment, or track slot
        for (int loc = LOC_RARM; loc <= LOC_LLEG; loc++) {
            for (int slot = 0; slot < 5; slot++) {
                if (getCritical(loc, slot).isHit()) {
                    cost++;
                }
            }
        }
        for (Mounted m : getMisc()) {
            if (m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_TRACKS)) {
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
     * Can this mech torso twist in the given direction?
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

        // In 'Mech mode the torso rotation can be limited by gyro damage.
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
        if (getCrew().hasDedicatedPilot()) {
            roll.addModifier(-1, "dedicated pilot");
        } else {
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
            Hex hex = game.getBoard().getHex(getPosition());
            if ((null != hex) && getMovementMode().isWheeled()
                    && (hex.terrainLevel(Terrains.SNOW) == 1)) {
                roll.addModifier(1, "thin snow");
            }
            // VDNI bonus?
            if (hasAbility(OptionsConstants.MD_VDNI)
                    && !hasAbility(OptionsConstants.MD_BVDNI)) {
                roll.addModifier(-1, "VDNI");
            }
            if (hasQuirk(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)
                    && !hasAbility(OptionsConstants.UNOFF_SMALL_PILOT)) {
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
                && game != null && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TURN_MODE);
    }

    /**
     * If the QuadVee is in vehicle mode (or converting to it) then it follows
     * the rules for tanks going hull-down, which requires a fortified hex.
     *
     *  @return True if hull-down is enabled and the QuadVee is in a fortified hex.
     */
    @Override
    public boolean canGoHullDown() {
        if (getConversionMode() == CONV_MODE_VEHICLE != convertingNow) {
            Hex occupiedHex = game.getBoard().getHex(getPosition());
            return occupiedHex.containsTerrain(Terrains.FORTIFIED)
                    && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN);
        }
        return super.canGoHullDown();
    }

    /**
     * Cannot make any physical attacks in vehicle mode except charging, which is
     * handled in the movement phase.
     */
    @Override
    public boolean isEligibleForPhysical() {
        return getConversionMode() == CONV_MODE_MECH && super.isEligibleForPhysical();
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
        return Entity.ETYPE_MECH | Entity.ETYPE_QUAD_MECH | Entity.ETYPE_QUADVEE;
    }
}
