/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.common.enums.AimingMode;
import megamek.common.enums.MPBoosters;
import megamek.common.options.OptionsConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Andrew Hunter VTOLs are helicopters (more or less.)
 * @since Jun 1, 2005
 */
public class VTOL extends Tank implements IBomber {
    private static final long serialVersionUID = -7406911547399249173L;

    public static final int LOC_ROTOR = 5;
    public static final int LOC_TURRET = 6;
    public static final int LOC_TURRET_2 = 7;
    public static final int LOC_NUM = 8;

    // VTOLs can have at most one (chin) turret, sponsons don't count and dual
    // turrets aren't allowed.
    private static String[] LOCATION_ABBRS = { "BD", "FR", "RS", "LS", "RR",
            "RO", "TU" };
    private static String[] LOCATION_NAMES = { "Body", "Front", "Right",
            "Left", "Rear", "Rotor", "Turret"};

    // critical hits
    public static final int CRIT_COPILOT = 15;
    public static final int CRIT_PILOT = 16;
    public static final int CRIT_ROTOR_DAMAGE = 17;
    public static final int CRIT_ROTOR_DESTROYED = 18;
    public static final int CRIT_FLIGHT_STABILIZER = 19;

    public VTOL() {
        super();
        // need to set elevation to something different than entity
        elevation = 1;
    }


    @Override
    public int getUnitType() {
        return UnitType.VTOL;
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public int getLocTurret() {
        return LOC_TURRET;
    }

    @Override
    public int getLocTurret2() {
        return LOC_TURRET_2;
    }

    protected int[] intBombChoices = new int[BombType.B_NUM];
    protected int[] extBombChoices = new int[BombType.B_NUM];

    private Targetable bombTarget = null;
    private List<Coords> strafingCoords = new ArrayList<>();

    @Override
    public PilotingRollData checkSkid(EntityMovementType moveType, Hex prevHex, EntityMovementType overallMoveType,
            MoveStep prevStep, MoveStep currStep, int prevFacing, int curFacing, Coords lastPos, Coords curPos,
            boolean isInfantry, int distance) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: VTOLs can't skid");
        return roll;
    }

    @Override
    public boolean canCharge() {
        return false;
    }

    @Override
    public int getMaxElevationChange() {
        return UNLIMITED_JUMP_DOWN;
    }

    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        Hex hex = game.getBoard().getHex(c);
        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if (hex.containsTerrain(Terrains.PAVEMENT)
                    || hex.containsTerrain(Terrains.ROAD)) {
                return true;
            }
            // Can't deploy on a bridge
            if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation)
                    && hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
            // Can't deploy on the surface of water
            if (hex.containsTerrain(Terrains.WATER) && (currElevation == 0)) {
                return true;
            }
            // Airborne units can't deploy hidden
            if (currElevation > 0) {
                return true;
            }
        }

        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Tank#isRepairable()
     */
    @Override
    public boolean isRepairable() {
        boolean retval = isSalvage();
        int loc = Tank.LOC_FRONT;
        while (retval && (loc < VTOL.LOC_ROTOR)) {
            int loc_is = this.getInternal(loc);
            loc++;
            retval = (loc_is != IArmorState.ARMOR_DOOMED)
                    && (loc_is != IArmorState.ARMOR_DESTROYED);
        }
        return retval;
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
                                   int cover) {
        int nArmorLoc = LOC_FRONT;
        boolean bSide = false;
        if (side == ToHitData.SIDE_LEFT) {
            nArmorLoc = LOC_LEFT;
            bSide = true;
        } else if (side == ToHitData.SIDE_RIGHT) {
            nArmorLoc = LOC_RIGHT;
            bSide = true;
        } else if (side == ToHitData.SIDE_REAR) {
            nArmorLoc = LOC_REAR;
        }
        HitData rv = new HitData(nArmorLoc);
        boolean bHitAimed = false;
        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {
            int roll = Compute.d6(2);
            if ((5 < roll) && (roll < 9)) {
                rv = new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
                bHitAimed = true;
            }
        }
        if (!bHitAimed) {
            switch (Compute.d6(2)) {
                case 2:
                    rv.setEffect(HitData.EFFECT_CRITICAL);
                    break;
                case 3:
                    rv = new HitData(LOC_ROTOR, false, HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    break;
                case 4:
                    if (m_bHasNoTurret) {
                        rv = new HitData(LOC_ROTOR, false, HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv = new HitData(LOC_TURRET);
                    }
                    break;
                case 5:
                    if (bSide) {
                        rv = new HitData(LOC_FRONT);
                    } else {
                        rv = new HitData(LOC_RIGHT);
                    }
                    break;
                case 6:
                case 7:
                    break;
                case 8:
                    if (bSide && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_EFFECTIVE)) {
                        rv.setEffect(HitData.EFFECT_CRITICAL);
                    }
                    break;
                case 9:
                    if (bSide) {
                        rv = new HitData(LOC_REAR);
                    } else {
                        rv = new HitData(LOC_LEFT);
                    }
                    break;
                case 10:
                case 11:
                    rv = new HitData(LOC_ROTOR, false, HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    break;
                case 12:
                    rv = new HitData(LOC_ROTOR, false, HitData.EFFECT_CRITICAL
                            | HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
            }
        }
        if (table == ToHitData.HIT_SWARM) {
            rv.setEffect(rv.getEffect() | HitData.EFFECT_CRITICAL);
        }
        return rv;
    }

    @Override
    public boolean doomedInVacuum() {
        return true;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return true;
    }

    @Override
    public boolean isBomber() {
        return (game != null)
                && (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VTOL_ATTACKS));
    }

    @Override
    public int availableBombLocation(int cost) {
        return LOC_FRONT;
    }

    @Override
    public int getMaxExtBombPoints() {
        return (int) Math.round(getWeight() / 5);
    }
    @Override
    public int getMaxIntBombPoints() {
        return 0;
    }


    @Override
    public int getMaxBombPoints() {
        return getMaxExtBombPoints();
    }

    @Override
    public int[] getIntBombChoices() {
        return intBombChoices.clone();
    }

    @Override
    public void setIntBombChoices(int[] bc) {
    }

    @Override
    public int[] getExtBombChoices() {
        return extBombChoices.clone();
    }

    @Override
    public void setExtBombChoices(int[] bc) {
        if (bc.length == extBombChoices.length) {
            extBombChoices = bc;
        }
    }

    @Override
    public void clearBombChoices() {
        Arrays.fill(extBombChoices, 0);
    }

    @Override
    public int reduceMPByBombLoad(int t) {
        // Per TacOps errata v3.0, movement reduction is per bomb rather than per 5 bomb points
        return Math.max(0, (t - (int) this.getBombs().stream().filter(m -> (m.getUsableShotsLeft() > 0)).count()));
    }

    @Override
    public void setUsedInternalBombs(int b){
        // Do nothing
    }

    @Override
    public void increaseUsedInternalBombs(int b){
        // Do nothing
    }

    @Override
    public int getUsedInternalBombs() {
        // Currently not possible
        return 0;
    }

    @Override
    public Targetable getVTOLBombTarget() {
        return bombTarget;
    }

    @Override
    public void setVTOLBombTarget(Targetable t) {
        bombTarget = t;
    }

    public List<Coords> getStrafingCoords() {
        return strafingCoords;
    }

    @Override
    public boolean isMakingVTOLGroundAttack() {
        return bombTarget != null || !strafingCoords.isEmpty();
    }

    @Override
    public boolean isNightwalker() {
        return false;
    }

    @Override
    public boolean doomedInSpace() {
        return true;
    }

    @Override
    public void setOnFire(boolean inferno) {
        super.setOnFire(inferno);
        extinguishLocation(LOC_ROTOR);
    }

    /**
     * get the type of critical caused by a critical roll, taking account of existing damage
     *
     * @param roll the final dice roll
     * @param loc the hit location
     * @param damagedByFire whether or not the critical was caused by fire,
     *      which is distinct from damage for unofficial thresholding purposes.
     * @return a critical type
     */
    @Override
    public int getCriticalEffect(int roll, int loc, boolean damagedByFire) {
        if (roll > 12) {
            roll = 12;
        }
        if ((roll < 6)
                || (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)
                        && !getOverThresh() && !damagedByFire)) {
            return CRIT_NONE;
        }
        for (int i = 0; i < 2; i++) {
            if (i > 0) {
                roll = 6;
            }
            if (loc == LOC_FRONT) {
                switch (roll) {
                    case 6:
                        if (!isDriverHit()) {
                            return CRIT_COPILOT;
                        } else if (!getCrew().isDead() && !getCrew().isDoomed()) {
                            return CRIT_CREW_KILLED;
                        }
                    case 7:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isJammed() && !m.isHit()
                                    && !m.jammedThisPhase()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 8:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 9:
                        if (getSensorHits() < Tank.CRIT_SENSOR_MAX) {
                            return CRIT_SENSOR;
                        }
                    case 10:
                        if (!isCommanderHit()) {
                            return CRIT_PILOT;
                        } else if (!getCrew().isDead() && !getCrew().isDoomed()) {
                            return CRIT_CREW_KILLED;
                        }
                    case 11:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 12:
                        if (!getCrew().isDead() && !getCrew().isDoomed()) {
                            return CRIT_CREW_KILLED;
                        }
                }
            } else if (loc == LOC_REAR) {
                switch (roll) {
                    case 6:
                        if (!getLoadedUnits().isEmpty()) {
                            return CRIT_CARGO;
                        }
                    case 7:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isJammed() && !m.isHit()
                                    && !m.jammedThisPhase()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 8:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 9:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 10:
                        if (getSensorHits() < Tank.CRIT_SENSOR_MAX) {
                            return CRIT_SENSOR;
                        }
                    case 11:
                        if (!engineHit) {
                            return (hasEngine() ? CRIT_ENGINE : CRIT_NONE);
                        }
                    case 12:
                        if (hasEngine()) {
                            if (getEngine().isFusion() && !engineHit) {
                                return CRIT_ENGINE;
                            } else if (!getEngine().isFusion()) {
                                return CRIT_FUEL_TANK;
                            }
                        } else {
                            return CRIT_NONE;
                        }
                }
            } else if (loc == LOC_ROTOR) {
                switch (roll) {
                    case 6:
                    case 7:
                    case 8:
                        if (!isImmobile()) {
                            return CRIT_ROTOR_DAMAGE;
                        }
                    case 9:
                    case 10:
                        if (!isStabiliserHit(loc)) {
                            return CRIT_FLIGHT_STABILIZER;
                        }
                    case 11:
                    case 12:
                        return CRIT_ROTOR_DESTROYED;
                }
            } else if (loc == LOC_TURRET) {
                switch (roll) {
                    case 6:
                        return CRIT_STABILIZER;
                    case 7:
                        return CRIT_TURRET_JAM;
                    case 8:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isJammed() && !m.isHit()
                                    && !m.jammedThisPhase()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 9:
                        return CRIT_TURRET_LOCK;
                    case 10:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 11:
                        for (Mounted m : getAmmo()) {
                            if (!m.isDestroyed() && !m.isHit() && (m.getLocation() != Entity.LOC_NONE)) {
                                return CRIT_AMMO;
                            }
                        }
                    case 12:
                        return CRIT_TURRET_DESTROYED;
                }
            } else {
                switch (roll) {
                    case 6:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isJammed() && !m.isHit()
                                    && !m.jammedThisPhase()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 7:
                        if (!getLoadedUnits().isEmpty()) {
                            return CRIT_CARGO;
                        }
                    case 8:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 9:
                        // TODO : fix for new TW rules
                        // roll 1d6, 1-3, defending player
                        // chooses which weapon gets destroyed
                        // 4-6: attacker chooses which weapon gets destroyed
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 10:
                        if (!engineHit) {
                            return (hasEngine() ? CRIT_ENGINE : CRIT_NONE);
                        }
                    case 11:
                        for (Mounted m : getAmmo()) {
                            if (!m.isDestroyed() && !m.isHit() && (m.getLocation() != Entity.LOC_NONE)) {
                                return CRIT_AMMO;
                            }
                        }
                    case 12:
                        if (hasEngine()) {
                            if (getEngine().isFusion() && !engineHit) {
                                return CRIT_ENGINE;
                            } else if (!getEngine().isFusion()) {
                                return CRIT_FUEL_TANK;
                            }
                        } else {
                            return CRIT_NONE;
                        }
                }
            }
        }
        return CRIT_NONE;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        if (motivePenalty > 0) {
            prd.addModifier(motivePenalty, "Steering Damage");
        }
        if (isDriverHit()) {
            prd.addModifier(2, "pilot injured");
        }
        if (isStabiliserHit(LOC_ROTOR)) {
            prd.addModifier(3, "flight stabiliser damaged");
        }

        // VDNI bonus?
        if (hasAbility(OptionsConstants.MD_VDNI)
                && !hasAbility(OptionsConstants.MD_BVDNI)) {
            prd.addModifier(-1, "VDNI");
        }

        return prd;
    }


    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        bombTarget = null;
        strafingCoords.clear();
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();

        if (engineHit || isLocationBad(LOC_ROTOR)) {
            return 0;
        }

        mp = Math.max(0, mp - motiveDamage);

        if (!mpCalculationSetting.ignoreCargo) {
            mp = Math.max(0, mp - getCargoMpReduction(this));
        }

        if (!mpCalculationSetting.ignoreWeather && (null != game)) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);

            if (getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_SNOW)) {
                if (game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WE_ICE_STORM) {
                    mp += 2;
                }

                if ((game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WE_SLEET)
                        || (game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WE_LIGHT_SNOW)
                        || (game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WE_MOD_SNOW)
                        || (game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WE_HEAVY_SNOW)
                        || (game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WE_SNOW_FLURRIES)) {
                    mp += 1;
                }
            }
        }

        if (!mpCalculationSetting.ignoreModularArmor && hasModularArmor()) {
            mp--;
        }

        if (hasWorkingMisc(MiscType.F_DUNE_BUGGY)) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreCargo) {
            mp = reduceMPByBombLoad(mp);
        }

        if (!mpCalculationSetting.ignoreGravity) {
            mp = applyGravityEffectsOnMP(mp);
        }

        return mp;
    }

    @Override
    public MPBoosters getMPBoosters(boolean onlyArmed) {
        for (Mounted m : getEquipment()) {
            if (!m.isInoperable() && (m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_MASC)) {
                return MPBoosters.VTOL_JET_BOOSTER;
            }
        }
        return MPBoosters.NONE;
    }

    @Override
    public PilotingRollData checkSideSlip(EntityMovementType moveType,
                                          Hex prevHex, EntityMovementType overallMoveType,
                                          MoveStep prevStep, int prevFacing, int curFacing, Coords lastPos,
                                          Coords curPos, int distance, boolean speedBooster) {
        PilotingRollData roll = super.checkSideSlip(moveType, prevHex, overallMoveType, prevStep, prevFacing,
                curFacing, lastPos, curPos, distance, speedBooster);
        if (speedBooster) {
            roll.addModifier(3, "used VTOL Jet Booster");
        }
        return roll;
    }

        @Override
    public int height() {
        if (isSuperHeavy()) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean isSuperHeavy() {
        return getWeight() > 30;
    }

    @Override
    public int locations() {
        if (m_bHasNoTurret) {
            return 6;
        } else {
            return 7;
        }
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_TANK | Entity.ETYPE_VTOL;
    }

    public static TechAdvancement getChinTurretTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, 3079, 3080).setApproximate(false, true, false)
                .setTechRating(RATING_B).setAvailability(RATING_F, RATING_F, RATING_F, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        if (!hasNoTurret()) {
            ctl.addComponent(getChinTurretTA());
        }
    }

    /**
     * Used to determine the draw priority of different Entity subclasses.
     * This allows different unit types to always be draw above/below other
     * types.
     *
     * @return
     */
    @Override
    public int getSpriteDrawPriority() {
        return 8;
    }
}
