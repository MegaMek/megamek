/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.cost.CombatVehicleCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MPBoosters;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.weapons.flamers.VehicleFlamerWeapon;
import megamek.common.weapons.lasers.CLChemicalLaserWeapon;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * You know what tanks are, silly.
 */
public class Tank extends Entity {
    private static final long serialVersionUID = -857210851169206264L;
    protected boolean m_bHasNoTurret = false;
    protected boolean m_bTurretLocked = false;
    protected boolean m_bTurretJammed = false;
    protected boolean m_bTurretEverJammed = false;
    protected boolean m_bHasNoDualTurret = false;
    protected boolean m_bDualTurretLocked = false;
    protected boolean m_bDualTurretJammed = false;
    protected boolean m_bDualTurretEverJammed = false;
    private int m_nTurretOffset = 0;
    private int m_nDualTurretOffset = 0;
    private int m_nStunnedTurns = 0;
    private boolean m_bImmobile = false;
    private boolean m_bImmobileHit = false;
    private int burningLocations = 0;
    private boolean m_bBackedIntoHullDown = false;
    protected int motivePenalty = 0;
    protected int motiveDamage = 0;
    private boolean minorMovementDamage = false;
    private boolean moderateMovementDamage = false;
    private boolean heavyMovementDamage = false;
    private boolean infernoFire = false;
    private ArrayList<Mounted> jammedWeapons = new ArrayList<>();
    protected boolean engineHit = false;

    // locations
    public static final int LOC_BODY = 0;
    public static final int LOC_FRONT = 1;
    public static final int LOC_RIGHT = 2;
    public static final int LOC_LEFT = 3;
    public static final int LOC_REAR = 4;
    /** for dual turret tanks, this is the rear turret **/
    public static final int LOC_TURRET = 5;
    /** for dual turret tanks, this is the front turret **/
    public static final int LOC_TURRET_2 = 6;

    // critical hits
    public static final int CRIT_NONE = -1;
    public static final int CRIT_DRIVER = 0;
    public static final int CRIT_WEAPON_JAM = 1;
    public static final int CRIT_WEAPON_DESTROYED = 2;
    public static final int CRIT_STABILIZER = 3;
    public static final int CRIT_SENSOR = 4;
    public static final int CRIT_COMMANDER = 5;
    public static final int CRIT_CREW_KILLED = 6;
    public static final int CRIT_CREW_STUNNED = 7;
    public static final int CRIT_CARGO = 8;
    public static final int CRIT_ENGINE = 9;
    public static final int CRIT_FUEL_TANK = 10;
    public static final int CRIT_AMMO = 11;
    public static final int CRIT_TURRET_JAM = 12;
    public static final int CRIT_TURRET_LOCK = 13;
    public static final int CRIT_TURRET_DESTROYED = 14;

    public static final int CRIT_SENSOR_MAX = 4;

    //Fortify terrain just like infantry
    public static final int DUG_IN_NONE = 0;
    public static final int DUG_IN_FORTIFYING1 = 1;
    public static final int DUG_IN_FORTIFYING2 = 2;
    public static final int DUG_IN_FORTIFYING3 = 3;
    private int dugIn = DUG_IN_NONE;

    // tanks have no critical slot limitations
    private static final int[] NUM_OF_SLOTS = { 25, 25, 25, 25, 25, 25, 25 };

    private static String[] LOCATION_ABBRS = { "BD", "FR", "RS", "LS", "RR",
            "TU", "FT" };
    private static String[] LOCATION_NAMES = { "Body", "Front", "Right",
            "Left", "Rear", "Turret" };

    private static String[] LOCATION_NAMES_DUAL_TURRET = { "Body", "Front",
            "Right", "Left", "Rear", "Rear Turret", "Front Turret" };

    // maps ToHitData - SIDE_X constants to LOC_X constants here for hull down fixed side hit locations
    protected static final Map<Integer, Integer> SIDE_LOC_MAPPING =
        Map.of(ToHitData.SIDE_FRONT, LOC_FRONT,
                ToHitData.SIDE_LEFT, LOC_LEFT,
                ToHitData.SIDE_RIGHT, LOC_RIGHT,
                ToHitData.SIDE_REAR, LOC_REAR);

    @Override
    public int getUnitType() {
        EntityMovementMode mm = getMovementMode();
        return (mm == EntityMovementMode.NAVAL) || (mm == EntityMovementMode.HYDROFOIL) || (mm == EntityMovementMode.SUBMARINE)
             ? UnitType.NAVAL
             : UnitType.TANK;
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        if (!hasNoDualTurret()) {
            return LOCATION_NAMES_DUAL_TURRET;
        }
        return LOCATION_NAMES;
    }

    public int getLocTurret() {
        return LOC_TURRET;
    }

    public int getLocTurret2() {
        return LOC_TURRET_2;
    }

    private int sensorHits = 0;
    private int stabiliserHits = 0;
    private boolean driverHit = false;
    private boolean commanderHit = false;
    //If there is a cockpit command console, the tank does not suffer the effects of the first commander critical,
    // but the command console benefits are lost as the backup has to take command.
    private boolean usingConsoleCommander = false;
    /** Vehicles can be constructed with seating for additional crew. This has no effect on play */
    private int extraCrewSeats = 0;

    // set up some vars for what the critical effects would be
    private int potCrit = CRIT_NONE;
    private boolean overThresh = false;

    // pain-shunted units can take two driver and commander hits and two hits
    // before being killed
    private boolean driverHitPS = false;
    private boolean commanderHitPS = false;
    private boolean crewHitPS = false;

    /**
     * Keeps track of the base weight of the turret for omni tanks.
     */
    public static final int BASE_CHASSIS_TURRET_WT_UNASSIGNED = -1;
    private double baseChassisTurretWeight = BASE_CHASSIS_TURRET_WT_UNASSIGNED;
    private double baseChassisTurret2Weight = BASE_CHASSIS_TURRET_WT_UNASSIGNED;
    private double baseChassisSponsonPintleWeight = BASE_CHASSIS_TURRET_WT_UNASSIGNED;

    /**
     * Flag to indicate unit is constructed as a trailer. Trailers do not require
     * control systems or engines unless they are intended for independent operation.
     */
    private boolean trailer = false;
    /**
     * Keeps track of whether this vehicle has control systems.  Trailers aren't
     * required to have control systems.
     */
    private boolean hasNoControlSystems = false;

    /**
     * Alternate fuel for ICEs that affects operating range
     */
    private FuelType fuelType = FuelType.PETROCHEMICALS;

    @Override
    public CrewType defaultCrewType() {
        return CrewType.CREW;
    }

    public int getPotCrit() {
        return potCrit;
    }

    public void setPotCrit(int crit) {
        potCrit = crit;
    }

    public boolean getOverThresh() {
        return overThresh;
    }

    public void setOverThresh(boolean tf) {
        overThresh = tf;
    }

    public boolean hasNoTurret() {
        return m_bHasNoTurret;
    }

    public boolean hasNoDualTurret() {
        return m_bHasNoDualTurret;
    }

    public int getTurretCount() {
        int tankTurrets = 0;

        if (!hasNoDualTurret()) {
            tankTurrets = 2;
        } else if (!hasNoTurret()) {
            tankTurrets = 1;
        }

        return tankTurrets;
    }

    public void setHasNoTurret(boolean b) {
        m_bHasNoTurret = b;
    }

    public void setHasNoDualTurret(boolean b) {
        m_bHasNoDualTurret = b;
    }

    public int getMotiveDamage() {
        return motiveDamage;
    }

    public void setMotiveDamage(int d) {
        motiveDamage = d;
    }

    public int getMotivePenalty() {
        return motivePenalty;
    }

    public void setMotivePenalty(int p) {
        motivePenalty = p;
    }

    private static final TechAdvancement TA_COMBAT_VEHICLE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE, 2470, 2490).setProductionFactions(F_TH)
            .setTechRating(RATING_D).setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
            .setStaticTechLevel(SimpleTechLevel.INTRO);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return TA_COMBAT_VEHICLE;
    }

    //Advanced turrets
    public static TechAdvancement getDualTurretTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, DATE_NONE, 3080)
                .setApproximate(false, false, true)
                .setTechRating(RATING_B).setAvailability(RATING_F, RATING_F, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        if (!hasNoDualTurret()) {
            ctl.addComponent(getDualTurretTA());
        }
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();
        if (engineHit || isImmobile()) {
            return 0;
        }
        if (hasWorkingMisc(MiscType.F_HYDROFOIL)) {
            mp = (int) Math.round(mp * 1.25);
        }
        mp = Math.max(0, mp - motiveDamage);

        if (!mpCalculationSetting.ignoreCargo) {
            mp = Math.max(0, mp - getCargoMpReduction(this));
        }

        if (!mpCalculationSetting.ignoreWeather && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);

            if (getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_SNOW)) {
                if (conditions.getWeather().isIceStorm()) {
                    mp += 2;
                }

                if (conditions.getWeather().isLightSnowOrModerateSnowOrSnowFlurriesOrHeavySnowOrSleet()) {
                    mp += 1;
                }
            }

            if(getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)
                    && conditions.getWeather().isClear()
                    && conditions.getWind().isTornadoF1ToF3()) {
                mp += 1;
            }
        }

        if (!mpCalculationSetting.ignoreModularArmor && hasModularArmor()) {
            mp--;
        }

        if (hasWorkingMisc(MiscType.F_DUNE_BUGGY)) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreGravity) {
            mp = applyGravityEffectsOnMP(mp);
        }

        //If the unit is towing trailers, adjust its walkMP, TW p205
        if (!mpCalculationSetting.ignoreCargo && (null != game) && !getAllTowedUnits().isEmpty()) {
            double trainWeight = getWeight();
            int lowestSuspensionFactor = getSuspensionFactor();
            //Add up the trailers
            for (int id : getAllTowedUnits()) {
                Entity tr = game.getEntity(id);
                if (tr == null) {
                    //this isn't supposed to happen, but it can in rare cases when tr is destroyed
                    continue;
                }
                if (tr instanceof Tank) {
                    Tank trailer = (Tank) tr;
                    if (trailer.getSuspensionFactor() < lowestSuspensionFactor) {
                        lowestSuspensionFactor = trailer.getSuspensionFactor();
                    }
                }
                trainWeight += tr.getWeight();
            }
            mp = (int) ((getEngine().getRating() + lowestSuspensionFactor) / trainWeight);
        }

        return Math.max(mp, 0);
    }

    @Override
    public boolean isEligibleForPavementBonus() {
        return movementMode == EntityMovementMode.TRACKED || movementMode == EntityMovementMode.WHEELED || movementMode == EntityMovementMode.HOVER;
    }

    public boolean isTurretLocked(int turret) {
        if (turret == getLocTurret()) {
            return m_bTurretLocked || m_bTurretJammed;
        } else if (turret == getLocTurret2()) {
            return m_bDualTurretLocked || m_bDualTurretJammed;
        }
        return false;
    }

    public boolean isTurretJammed(int turret) {
        // this is rather a hack but the only idea I came up with.
        // for the first time this is checked. If this is a partially repaired
        // turret it will be jammed.
        // so just set it to jammed and change the partial repair value to
        // false.

        if (getPartialRepairs().booleanOption("veh_locked_turret")) {
            m_bTurretJammed = true;
            m_bDualTurretJammed = true;
            getPartialRepairs().getOption("veh_locked_turret").setValue(false);
        }
        if (turret == getLocTurret()) {
            return m_bTurretJammed;
        } else if (turret == getLocTurret2()) {
            return m_bDualTurretJammed;
        }
        return false;
    }

    /**
     * Returns the number of locations in the entity
     */
    @Override
    public int locations() {
        if (m_bHasNoDualTurret) {
            return m_bHasNoTurret ? 5 : 6;
        }
        return 7;
    }

    @Override
    public int getBodyLocation() {
        return LOC_BODY;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return !(m_bHasNoTurret || isTurretLocked(getLocTurret()) || getAlreadyTwisted());
    }

    @Override
    public boolean isValidSecondaryFacing(int n) {
        return !isTurretLocked(getLocTurret());
    }

    @Override
    public int clipSecondaryFacing(int n) {
        return n;
    }

    @Override
    public void setSecondaryFacing(int sec_facing) {
        if (!isTurretLocked(getLocTurret())) {
            super.setSecondaryFacing(sec_facing);
            if (!m_bHasNoTurret) {
                m_nTurretOffset = sec_facing - getFacing();
            }
        }
    }

    @Override
    public void setFacing(int facing) {
        super.setFacing(facing);
        if (isTurretLocked(getLocTurret())) {
            int nTurretFacing = (facing + m_nTurretOffset + 6) % 6;
            super.setSecondaryFacing(nTurretFacing);
        }
    }

    public int getDualTurretFacing() {
        return (facing + m_nDualTurretOffset + 6) % 6;
    }

    public void setDualTurretOffset(int offset) {
        m_nDualTurretOffset = offset;
    }

    public boolean isStabiliserHit(int loc) {
        return (stabiliserHits & (1 << loc)) == (1 << loc);
    }

    public void setStabiliserHit(int loc) {
        stabiliserHits |= (1 << loc);
    }

    public void clearStabiliserHit(int loc) {
        stabiliserHits &= ~(1 << loc);
    }

    public int getSensorHits() {
        return sensorHits;
    }

    public void setSensorHits(int hits) {
        sensorHits = hits;
    }

    public boolean isDriverHit() {
        return driverHit;
    }

    public void setDriverHit(boolean hit) {
        driverHit = hit;
    }

    public boolean isCommanderHit() {
        return commanderHit;
    }

    public void setCommanderHit(boolean hit) {
        commanderHit = hit;
    }

    public boolean isUsingConsoleCommander() {
        return usingConsoleCommander;
    }

    public void setUsingConsoleCommander(boolean b) {
        usingConsoleCommander = b;
    }

    /**
     * @return Additional seats beyond the minimum crew requirements
     */
    public int getExtraCrewSeats() {
        return extraCrewSeats;
    }

    public void setExtraCrewSeats(int seats) {
        this.extraCrewSeats = seats;
    }

    public boolean isDriverHitPS() {
        return driverHitPS;
    }

    public void setDriverHitPS(boolean hit) {
        driverHitPS = hit;
    }

    public boolean isCommanderHitPS() {
        return commanderHitPS;
    }

    public void setCommanderHitPS(boolean hit) {
        commanderHitPS = hit;
    }

    public boolean isCrewHitPS() {
        return crewHitPS;
    }

    public void setCrewHitPS(boolean hit) {
        crewHitPS = hit;
    }

    public boolean isMovementHit() {
        return m_bImmobile;
    }

    public boolean isMovementHitPending() {
        return m_bImmobileHit;
    }

    /**
     * Marks this tank for immobilization, most likely from a related motive
     * system hit. To <em>actually</em> immobilize it, {@link #applyDamage()}
     * must also be invoked; until then, {@link #isMovementHitPending()} will
     * return true after this but neither {@link #isMovementHit()} nor
     * {@link #isImmobile()} will have been updated yet (because the tank is
     * technically not immobile just <em>yet</em> until damage is actually
     * resolved).
     */
    public void immobilize() {
        m_bImmobileHit = true;
    }

    @Override
    public boolean isImmobile(boolean checkCrew) {
        if ((game != null)
                && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_NO_IMMOBILE_VEHICLES)) {
            return super.isImmobile(checkCrew);
        }
        //Towed trailers need to reference the tractor, or they return Immobile due to 0 MP...
        //We do run into some double-blind entityList differences though, so include a null check
        if (isTrailer() && (getTractor() != Entity.NONE)) {
            return (game.getEntity(getTractor()) != null ? game.getEntity(getTractor()).isImmobile(checkCrew) : super.isImmobile(checkCrew) || m_bImmobile);
        }
        return m_bImmobile || super.isImmobile(checkCrew);
    }

    /**
     * Whether this unit is irreversibly immobilized for the rest of the game.
     * Tanks have some additional criteria.
     */
    @Override
    public boolean isPermanentlyImmobilized(boolean checkCrew) {
        return super.isPermanentlyImmobilized(checkCrew) || isMovementHit();
    }

    /**
     * Per https://bg.battletech.com/forums/index.php/topic,78336.msg1869386.html#msg1869386
     * CVs with working engines and Jump Jets should still have the option to jump during the movement
     * phase, even if reduced to 0 MP by motive hits, or rolling 12 on the Motive System Damage table.
     */
    @Override
    public boolean isImmobileForJump() {
        // *Can* jump unless 0 Jump MP, or 1+ Jump MP but engine is critted, or crew unconscious/dead.
        return super.isImmobile(true) ||
                super.isPermanentlyImmobilized(true) ||
                (getJumpMP() == 0) ||
                isEngineHit();
    }

    @Override
    public boolean hasCommandConsoleBonus() {
        if (!hasWorkingMisc(MiscType.F_COMMAND_CONSOLE) || isCommanderHit() || isUsingConsoleCommander()) {
            return false;
        }
        if (isSupportVehicle()) {
            return getWeightClass() >= EntityWeightClass.WEIGHT_LARGE_SUPPORT
                    && hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL);
        } else {
            return getWeightClass() >= EntityWeightClass.WEIGHT_HEAVY;
        }
    }


    /**
     * Tanks have all sorts of prohibited terrain.
     */
    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        Hex hex = game.getBoard().getHex(c);
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if ((hex.containsTerrain(Terrains.PAVEMENT)
                    || hex.containsTerrain(Terrains.ROAD))
                    && (!hex.containsTerrain(Terrains.BUILDING)
                            && !hex.containsTerrain(Terrains.RUBBLE))) {
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
        }

        boolean hasFlotationHull = hasWorkingMisc(MiscType.F_FLOTATION_HULL);
        boolean isAmphibious = hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS);
        boolean hexHasRoad = hex.containsTerrain(Terrains.ROAD);
        boolean scoutBikeIntoLightWoods = (hex.terrainLevel(Terrains.WOODS) == 1) && hasQuirk(OptionsConstants.QUIRK_POS_SCOUT_BIKE);
        boolean isCrossCountry = hasAbility(OptionsConstants.PILOT_CROSS_COUNTRY);

        // roads allow movement through hexes that you normally couldn't go through
        switch (movementMode) {
            case TRACKED:
                if (isCrossCountry && !isSuperHeavy()) {
                    return ((hex.terrainLevel(Terrains.WATER) > 0)
                                    && !hex.containsTerrain(Terrains.ICE)
                                    && !hasFlotationHull && !isAmphibious)
                            || (hex.terrainLevel(Terrains.MAGMA) > 1);
                }

                if (!isSuperHeavy()) {
                    return ((hex.terrainLevel(Terrains.WOODS) > 1) && !hexHasRoad)
                            || ((hex.terrainLevel(Terrains.WATER) > 0)
                                    && !hex.containsTerrain(Terrains.ICE)
                                    && !hasFlotationHull && !isAmphibious)
                            || (hex.containsTerrain(Terrains.JUNGLE) && !hexHasRoad)
                            || (hex.terrainLevel(Terrains.MAGMA) > 1)
                            || (hex.terrainLevel(Terrains.ROUGH) > 1)
                            || ((hex.terrainLevel(Terrains.RUBBLE) > 5) && !hexHasRoad);
                } else {
                    return ((hex.terrainLevel(Terrains.WOODS) > 1) && !hexHasRoad)
                            || ((hex.terrainLevel(Terrains.WATER) > 0)
                                    && !hex.containsTerrain(Terrains.ICE)
                                    && !hasFlotationHull && !isAmphibious)
                            || (hex.containsTerrain(Terrains.JUNGLE) && !hexHasRoad)
                            || (hex.terrainLevel(Terrains.MAGMA) > 1);
                }
            case WHEELED:
                if (isCrossCountry && !isSuperHeavy()) {
                    return ((hex.terrainLevel(Terrains.WATER) > 0)
                                    && !hex.containsTerrain(Terrains.ICE)
                                    && !hasFlotationHull && !isAmphibious)
                            || hex.containsTerrain(Terrains.MAGMA)
                            || ((hex.terrainLevel(Terrains.SNOW) > 1) && !hexHasRoad)
                            || (hex.terrainLevel(Terrains.GEYSER) == 2);
                }

                if (!isSuperHeavy()) {
                    return (hex.containsTerrain(Terrains.WOODS) && !hexHasRoad && !scoutBikeIntoLightWoods)
                            || (hex.containsTerrain(Terrains.ROUGH) && !hexHasRoad)
                            || ((hex.terrainLevel(Terrains.WATER) > 0)
                                    && !hex.containsTerrain(Terrains.ICE)
                                    && !hasFlotationHull && !isAmphibious)
                            || (hex.containsTerrain(Terrains.RUBBLE) && !hexHasRoad)
                            || hex.containsTerrain(Terrains.MAGMA)
                            || (hex.containsTerrain(Terrains.JUNGLE) && !hexHasRoad)
                            || ((hex.terrainLevel(Terrains.SNOW) > 1) && !hexHasRoad)
                            || (hex.terrainLevel(Terrains.GEYSER) == 2);
                } else {
                    return (hex.containsTerrain(Terrains.WOODS) && !hexHasRoad)
                            || (hex.containsTerrain(Terrains.ROUGH) && !hexHasRoad)
                            || ((hex.terrainLevel(Terrains.WATER) > 0)
                                    && !hex.containsTerrain(Terrains.ICE)
                                    && !hasFlotationHull && !isAmphibious)
                            || (hex.containsTerrain(Terrains.RUBBLE) && !hexHasRoad)
                            || hex.containsTerrain(Terrains.MAGMA)
                            || (hex.containsTerrain(Terrains.JUNGLE) && !hexHasRoad)
                            || (hex.terrainLevel(Terrains.GEYSER) == 2);
                }
            case HOVER:
               if (isCrossCountry && !isSuperHeavy()) {
                    return (hex.terrainLevel(Terrains.MAGMA) > 1);
                }

                if (!isSuperHeavy()) {
                    return (hex.containsTerrain(Terrains.WOODS) && !hexHasRoad && !scoutBikeIntoLightWoods)
                            || (hex.containsTerrain(Terrains.JUNGLE) && !hexHasRoad)
                            || (hex.terrainLevel(Terrains.MAGMA) > 1)
                            || ((hex.terrainLevel(Terrains.ROUGH) > 1) && !hexHasRoad)
                            || ((hex.terrainLevel(Terrains.RUBBLE) > 5) && !hexHasRoad);
                } else {
                    return (hex.containsTerrain(Terrains.WOODS) && !hexHasRoad)
                            || (hex.containsTerrain(Terrains.JUNGLE) && !hexHasRoad)
                            || (hex.terrainLevel(Terrains.MAGMA) > 1);
                }

            case NAVAL:
            case HYDROFOIL:
                // Can only deploy under a bridge if there is sufficient clearance.
                if (hex.containsTerrain(Terrains.BRIDGE)
                        && (getHeight() >= hex.terrainLevel(Terrains.BRIDGE_ELEV) - hex.getLevel())) {
                    return true;
                }
                return (hex.terrainLevel(Terrains.WATER) <= 0)
                        || hex.containsTerrain(Terrains.ICE);
            case SUBMARINE:
                // Submarines get extra clearance equal to the depth of water.
                if (hex.containsTerrain(Terrains.BRIDGE)
                        && (getHeight() >= hex.terrainLevel(Terrains.BRIDGE_ELEV) - hex.floor())) {
                    return true;
                }
                return (hex.terrainLevel(Terrains.WATER) <= 0);
            case WIGE:
                return (hex.containsTerrain(Terrains.WOODS)
                        || hex.containsTerrain(Terrains.JUNGLE))
                        && hex.ceiling() > currElevation;
            default:
                return false;
        }
    }

    public void lockTurret(int turret) {
        if (turret == getLocTurret()) {
            m_bTurretLocked = true;
        } else if (turret == getLocTurret2()) {
            m_bDualTurretLocked = true;
        }

    }

    public void jamTurret(int turret) {
        if (turret == getLocTurret()) {
            m_bTurretEverJammed = true;
            m_bTurretJammed = true;
        } else if (turret == getLocTurret2()) {
            m_bDualTurretEverJammed = true;
            m_bDualTurretJammed = true;
        }

    }

    public void unjamTurret(int turret) {
        if (turret == getLocTurret()) {
            m_bTurretJammed = false;
        } else if (turret == getLocTurret2()) {
            m_bDualTurretJammed = false;
        }
    }

    public boolean isTurretEverJammed(int turret) {
        if (turret == getLocTurret()) {
            return m_bTurretEverJammed;
        } else if (turret == getLocTurret2()) {
            return m_bDualTurretEverJammed;
        }
        return false;
    }

    public int getStunnedTurns() {
        return m_nStunnedTurns;
    }

    public void setStunnedTurns(int turns) {
        m_nStunnedTurns = turns;
    }

    public void stunCrew() {
        if (m_nStunnedTurns == 0) {
            m_nStunnedTurns = 2;
        } else {
            m_nStunnedTurns++;
        }
    }

    @Override
    public void applyDamage() {
        applyMovementDamage();

        super.applyDamage();
    }

    /**
     * Applies movement damage to the Tank.
     */
    public void applyMovementDamage() {
        m_bImmobile |= m_bImmobileHit;

        // Towed trailers need to use the values of the tractor, or they return Immobile due to 0 MP...
        if (isTrailer() && (getTractor() != Entity.NONE) && game.getEntity(getTractor()).hasETypeFlag(Entity.ETYPE_TANK)) {
            Tank Tractor = (Tank) game.getEntity(getTractor());
            m_bImmobile = Tractor.m_bImmobile;
            m_bImmobileHit = Tractor.m_bImmobileHit;
        }
    }

    @Override
    public boolean isEligibleFor(GamePhase phase) {
        if (dugIn != DUG_IN_NONE) {
            return false;
        } else {
            return super.isEligibleFor(phase);
        }
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        incrementMASCAndSuperchargerLevels();

        // check for crew stun
        if (m_nStunnedTurns > 0) {
            m_nStunnedTurns--;
        }

        // reset turret facing, if not jammed
        if (!m_bTurretLocked) {
            setSecondaryFacing(getFacing());
        }

        // Continue to fortify
        if (dugIn != DUG_IN_NONE) {
            dugIn++;
            if (dugIn > DUG_IN_FORTIFYING3) {
                dugIn = DUG_IN_NONE;
            }
        }
    }

    public void setDugIn(int i) {
        dugIn = i;
    }

    public int getDugIn() {
        return dugIn;
    }

    /**
     * Returns the name of the type of movement used. This is tank-specific.
     */
    @Override
    public String getMovementString(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_SKID:
                return "Skidded";
            case MOVE_NONE:
                return "None";
            case MOVE_WALK:
            case MOVE_VTOL_WALK:
                return "Cruised";
            case MOVE_RUN:
            case MOVE_VTOL_RUN:
                return "Flanked";
            case MOVE_SPRINT:
            case MOVE_VTOL_SPRINT:
                return "Sprinted";
            case MOVE_JUMP:
                return "Jumped";
            default:
                return "Unknown!";
        }
    }

    /**
     * Returns the name of the type of movement used. This is tank-specific.
     */
    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_SKID:
                return "S";
            case MOVE_NONE:
                return "N";
            case MOVE_WALK:
            case MOVE_VTOL_WALK:
                return "C";
            case MOVE_RUN:
            case MOVE_VTOL_RUN:
                return "F";
            case MOVE_SPRINT:
            case MOVE_VTOL_SPRINT:
                return "O";
            case MOVE_JUMP:
                return "J";
            default:
                return "?";
        }
    }

    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    @Override
    public int firstArmorIndex() {
        return LOC_FRONT;
    }

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);

        // B-Pods need to be special-cased, the have 360 firing arc
        if ((mounted.getType() instanceof WeaponType)
                && mounted.getType().hasFlag(WeaponType.F_B_POD)) {
            return Compute.ARC_360;
        }
        // VGLs base arc on their facing
        if (mounted.getType().hasFlag(WeaponType.F_VGL)) {
            return Compute.firingArcFromVGLFacing(mounted.getFacing());
        }
        switch (mounted.getLocation()) {
            case LOC_BODY:
                // Body mounted C3Ms fire into the front arc,
                // per
                // http://forums.classicbattletech.com/index.php/topic,9400.0.html
            case LOC_FRONT:
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_FRONT;
                }
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_NOSE;
                }
            case LOC_TURRET:
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_TURRET;
                }
                return Compute.ARC_FORWARD;
            case LOC_TURRET_2:
                // Doubles as chin turret location for VTOLs, for which
                // Tank.LOC_TURRET == magic number 5 == VTOL.LOC_ROTOR.
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_TURRET;
                }
                return Compute.ARC_FORWARD;
            case LOC_RIGHT:
                if (mounted.isSponsonTurretMounted()) {
                    return Compute.ARC_SPONSON_TURRET_RIGHT;
                }
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_RIGHT;
                }
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_RIGHT_BROADSIDE;
                }
                return Compute.ARC_RIGHTSIDE;
            case LOC_LEFT:
                if (mounted.isSponsonTurretMounted()) {
                    return Compute.ARC_SPONSON_TURRET_LEFT;
                }
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_LEFT;
                }
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_LEFT_BROADSIDE;
                }
                return Compute.ARC_LEFTSIDE;
            case LOC_REAR:
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_REAR;
                }
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_AFT;
                }
                return Compute.ARC_REAR;
            default:
                return Compute.ARC_360;
        }
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc. If
     * false, assume it fires into the primary.
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        return getEquipment(weaponId).getLocation() == getLocTurret();
    }

    /**
     * Rolls up a hit location
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
                                   int cover) {
        int nArmorLoc = LOC_FRONT;
        boolean bSide = false;
        boolean bRear = false;
        boolean ignoreTurret = m_bHasNoTurret
                || (table == ToHitData.HIT_UNDERWATER);
        int motiveMod = getMotiveSideMod(side);
        setPotCrit(HitData.EFFECT_NONE);
        if (isHullDown()) {
            // which direction a tank moved in before going hull down is important
            int moveInDirection;
            if (!m_bBackedIntoHullDown) {
                moveInDirection = ToHitData.SIDE_FRONT;
            } else {
                moveInDirection = ToHitData.SIDE_REAR;
            }
            if ((side == moveInDirection) || (side == ToHitData.SIDE_LEFT)
                    || (side == ToHitData.SIDE_RIGHT)) {
                if (!ignoreTurret) {
                    // on a hull down vee, all hits expect for those that come
                    // from the opposite direction to which it entered the hex
                    // it went Hull Down in go to turret if one exists.
                    if (!hasNoDualTurret()) {
                        int roll = Compute.d6() - 2;
                        if (roll <= 3) {
                            nArmorLoc = getLocTurret2();
                        } else {
                            nArmorLoc = getLocTurret();
                        }
                    } else {
                        nArmorLoc = getLocTurret();
                    }
                }
                // If the tank doesn't have turret all hits that don't come from
                // the opposite direction to which it entered the hex it went
                // Hull Down in hit side they come in from
                else {
                    nArmorLoc = SIDE_LOC_MAPPING.get(side);
                }
                // Hull Down tanks don't make hit location rolls
                return new HitData(nArmorLoc);
            }
        }
        if (side == ToHitData.SIDE_LEFT) {
            nArmorLoc = LOC_LEFT;
            bSide = true;
        } else if (side == ToHitData.SIDE_RIGHT) {
            nArmorLoc = LOC_RIGHT;
            bSide = true;
        } else if (side == ToHitData.SIDE_REAR) {
            nArmorLoc = LOC_REAR;
            bRear = true;
        }
        HitData rv = new HitData(nArmorLoc);
        boolean bHitAimed = false;
        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {
            int roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                rv = new HitData(aimedLocation, side == ToHitData.SIDE_REAR,
                        true);
                bHitAimed = true;
            }
        }
        if (!bHitAimed) {
            switch (Compute.d6(2)) {
                case 2:
                    if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_CRITICAL);
                    } else {
                        rv.setEffect(HitData.EFFECT_CRITICAL);
                    }
                    break;
                case 3:
                    if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                    rv.setMotiveMod(motiveMod);
                    break;
                case 4:
                    if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                    rv.setMotiveMod(motiveMod);
                    break;
                case 5:
                    if (bSide) {
                        if (game.getOptions().booleanOption(
                                OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                            setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                            rv = new HitData(LOC_FRONT);
                        } else {
                            rv = new HitData(LOC_FRONT, false,
                                    HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                        }
                    } else if (bRear) {
                        if (game.getOptions().booleanOption(
                                OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                            setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                            rv = new HitData(LOC_LEFT);
                        } else {
                            rv = new HitData(LOC_LEFT, false,
                                    HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                        }
                    } else {
                        if (game.getOptions().booleanOption(
                                OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                            setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                            rv = new HitData(LOC_LEFT);
                        } else {
                            rv = new HitData(LOC_LEFT, false,
                                    HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                        }
                    }
                    rv.setMotiveMod(motiveMod);
                    break;
                case 6:
                case 7:
                    break;
                case 8:
                    if (bSide
                            && !game.getOptions().booleanOption(
                                    OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_EFFECTIVE)) {
                        if (game.getOptions().booleanOption(
                                OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                            setPotCrit(HitData.EFFECT_CRITICAL);
                        } else {
                            rv.setEffect(HitData.EFFECT_CRITICAL);
                        }
                    }
                    break;
                case 9:
                    if (game.getOptions().booleanOption(
                            OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_EFFECTIVE)) {
                        if (bSide) {
                            rv = new HitData(LOC_REAR);
                        } else if (bRear) {
                            rv = new HitData(LOC_RIGHT);
                        } else {
                            rv = new HitData(LOC_LEFT);
                        }
                    } else {
                        if (bSide) {
                            if (game.getOptions().booleanOption(
                                    OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                                setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                                rv = new HitData(LOC_REAR);
                            } else {
                                rv = new HitData(LOC_REAR, false,
                                        HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                            }
                        } else if (bRear) {
                            if (game.getOptions().booleanOption(
                                    OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                                setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                                rv = new HitData(LOC_RIGHT);
                            } else {
                                rv = new HitData(LOC_RIGHT, false,
                                        HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                            }
                        } else {
                            if (game.getOptions().booleanOption(
                                    OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                                setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                                rv = new HitData(LOC_LEFT);
                            } else {
                                rv = new HitData(LOC_LEFT, false,
                                        HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                            }
                        }
                        rv.setMotiveMod(motiveMod);
                    }
                    break;
                case 10:
                    if (!ignoreTurret) {
                        if (!hasNoDualTurret()) {
                            int roll = Compute.d6();
                            if (side == ToHitData.SIDE_FRONT) {
                                roll -= 2;
                            } else if (side == ToHitData.SIDE_REAR) {
                                roll += 2;
                            }
                            if (roll <= 3) {
                                rv = new HitData(LOC_TURRET_2);
                            } else {
                                rv = new HitData(LOC_TURRET);
                            }
                        } else {
                            rv = new HitData(LOC_TURRET);
                        }
                    }
                    break;
                case 11:
                    if (!ignoreTurret) {
                        if (!hasNoDualTurret()) {
                            int roll = Compute.d6();
                            if (side == ToHitData.SIDE_FRONT) {
                                roll -= 2;
                            } else if (side == ToHitData.SIDE_REAR) {
                                roll += 2;
                            }
                            if (roll <= 3) {
                                rv = new HitData(LOC_TURRET_2);
                            } else {
                                rv = new HitData(LOC_TURRET);
                            }
                        } else {
                            rv = new HitData(LOC_TURRET);
                        }
                    }
                    break;
                case 12:
                    if (ignoreTurret) {
                        if (game.getOptions().booleanOption(
                                OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                            setPotCrit(HitData.EFFECT_CRITICAL);
                        } else {
                            rv.setEffect(HitData.EFFECT_CRITICAL);
                        }
                    } else {
                        if (!hasNoDualTurret()) {
                            int roll = Compute.d6();
                            if (side == ToHitData.SIDE_FRONT) {
                                roll -= 2;
                            } else if (side == ToHitData.SIDE_REAR) {
                                roll += 2;
                            }
                            if (roll <= 3) {
                                if (game.getOptions().booleanOption(
                                        OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                                    setPotCrit(HitData.EFFECT_CRITICAL);
                                    rv = new HitData(LOC_TURRET_2);
                                } else {
                                    rv = new HitData(LOC_TURRET_2, false,
                                            HitData.EFFECT_CRITICAL);
                                }
                            } else {
                                if (game.getOptions().booleanOption(
                                        OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                                    setPotCrit(HitData.EFFECT_CRITICAL);
                                    rv = new HitData(LOC_TURRET);
                                } else {
                                    rv = new HitData(LOC_TURRET, false,
                                            HitData.EFFECT_CRITICAL);
                                }
                            }
                        } else {
                            if (game.getOptions().booleanOption(
                                    OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                                setPotCrit(HitData.EFFECT_CRITICAL);
                                rv = new HitData(LOC_TURRET);
                            } else {
                                rv = new HitData(LOC_TURRET, false,
                                        HitData.EFFECT_CRITICAL);
                            }
                        }
                    }
            }
        }
        if (table == ToHitData.HIT_SWARM) {
            rv.setEffect(rv.getEffect() | HitData.EFFECT_CRITICAL);
            setPotCrit(HitData.EFFECT_CRITICAL);
        }
        return rv;
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, AimingMode.NONE, LosEffects.COVER_NONE);
    }

    /**
     * Gets the location that excess damage transfers to
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(LOC_DESTROYED);
    }

    /**
     * Gets the location that is destroyed recursively
     */
    @Override
    public int getDependentLocation(int loc) {
        return LOC_NONE;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        if (motivePenalty > 0) {
            prd.addModifier(motivePenalty, "Steering Damage");
        }
        if (commanderHit) {
            prd.addModifier(1, "commander injured");
        }
        if (driverHit) {
            prd.addModifier(2, "driver injured");
        }

        // are we wheeled and in light snow?
        Hex hex = game.getBoard().getHex(getPosition());
        if ((null != hex) && (getMovementMode() == EntityMovementMode.WHEELED)
                && (hex.terrainLevel(Terrains.SNOW) == 1)) {
            prd.addModifier(1, "thin snow");
        }

        // VDNI bonus?
        if (hasAbility(OptionsConstants.MD_VDNI)
                && !hasAbility(OptionsConstants.MD_BVDNI)) {
            prd.addModifier(-1, "VDNI");
        }

        if (hasModularArmor()) {
            prd.addModifier(1, "Modular Armor");
        }

        return prd;
    }

    @Override
    public boolean usesTurnMode() {
        return game != null && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TURN_MODE);
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7035);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(getCrew().getDescVector(false));
        r = new Report(7070, Report.PUBLIC);
        r.add(getKillNumber());
        vDesc.addElement(r);

        if (isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if (killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if (killer != null) {
                r = new Report(7072, Report.PUBLIC);
                r.addDesc(killer);
            } else {
                r = new Report(7073, Report.PUBLIC);
            }
            vDesc.addElement(r);
        } else if (getCrew().isEjected()) {
            r = new Report(7071, Report.PUBLIC);
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    @Override
    public int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        if (!mpCalculationSetting.ignoreMASC && hasArmedMASC()) {
            return (getWalkMP(mpCalculationSetting) * 2);
        } else {
            return super.getRunMP(mpCalculationSetting);
        }
    }

    @Override
    public int getSprintMP() {
        return getSprintMP(MPCalculationSetting.STANDARD);
    }

    @Override
    public int getSprintMP(MPCalculationSetting mpCalculationSetting) {
        if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS)) {
            if (!mpCalculationSetting.ignoreMASC && hasArmedMASC()) {
                return (int) Math.ceil(getWalkMP(mpCalculationSetting) * 2.5);
            } else {
                return getWalkMP(mpCalculationSetting) * 2;
            }
        } else {
            return getRunMP(mpCalculationSetting);
        }
    }

    @Override
    public int getRunningGravityLimit() {
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS)) {
            return getSprintMP(MPCalculationSetting.NO_GRAVITY);
        } else {
            return getRunMP(MPCalculationSetting.NO_GRAVITY);
        }
    }

    @Override
    public int getHeatCapacity(boolean radicalHeatSinks) {
        return DOES_NOT_TRACK_HEAT;
    }

    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    @Override
    public void autoSetInternal() {
        int nInternal = (int) Math.ceil(weight / 10.0);

        // No internals in the body location.
        initializeInternal(IArmorState.ARMOR_NA, LOC_BODY);

        for (int x = 1; x < locations(); x++) {
            initializeInternal(nInternal, x);
        }
    }

    @Override
    public int getMaxElevationChange() {
        return 1;
    }

    @Override
    public int getMaxElevationDown(int currElevation) {
        // WIGEs can go down as far as they want
        // 50 is a pretty arbitrary max amount, but that's also the
        // highest elevation for VTOLs, so I'll just use that
        if ((currElevation > 0)
                && (getMovementMode() == EntityMovementMode.WIGE)) {
            return 50;
        }
        return super.getMaxElevationDown(currElevation);
    }

    /**
     * Determine if the unit can be repaired, or only harvested for spares.
     *
     * @return A <code>boolean</code> that is <code>true</code> if the unit can
     *         be repaired (given enough time and parts); if this value is
     *         <code>false</code>, the unit is only a source of spares.
     * @see Entity#isSalvage()
     */
    @Override
    public boolean isRepairable() {
        // A tank is repairable if it is salvageable,
        // and none of its body internals are gone.
        boolean retval = isSalvage();
        int loc = Tank.LOC_FRONT;
        while (retval && (loc < getLocTurret())) {
            int loc_is = this.getInternal(loc);
            loc++;
            retval = (loc_is != IArmorState.ARMOR_DOOMED)
                    && (loc_is != IArmorState.ARMOR_DESTROYED);
        }
        return retval;
    }

    @Override
    public boolean canCharge() {
        // Tanks can charge, except Hovers when the option is set, and WIGEs
        return super.canCharge()
                && !(game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_NO_HOVER_CHARGE)
                        && (EntityMovementMode.HOVER == getMovementMode()))
                && !(EntityMovementMode.WIGE == getMovementMode()) && !(getStunnedTurns() > 0);
    }

    @Override
    public boolean canDFA() {
        // Tanks can't DFA
        return false;
    }

    /**
     * @return suspension factor of vehicle
     */
    public int getSuspensionFactor() {
        return getSuspensionFactor(getMovementMode(), weight);
    }

    /**
     * Static method to calculate suspension factor without needing a vehicle
     */
    public static int getSuspensionFactor(EntityMovementMode movementMode, double weight) {
        switch (movementMode) {
            case HOVER:
                if (weight <= 10) {
                    return 40;
                }
                if (weight <= 20) {
                    return 85;
                }
                if (weight <= 30) {
                    return 130;
                }
                if (weight <= 40) {
                    return 175;
                }
                if (weight <= 50) {
                    return 235;
                } else {
                    return 235 + (45 * (int) Math.ceil((weight - 50) / 25f));
                }
            case HYDROFOIL:
                if (weight <= 10) {
                    return 60;
                }
                if (weight <= 20) {
                    return 105;
                }
                if (weight <= 30) {
                    return 150;
                }
                if (weight <= 40) {
                    return 195;
                }
                if (weight <= 50) {
                    return 255;
                }
                if (weight <= 60) {
                    return 300;
                }
                if (weight <= 70) {
                    return 345;
                }
                if (weight <= 80) {
                    return 390;
                }
                if (weight <= 90) {
                    return 435;
                }
                return 480;
            case NAVAL:
            case SUBMARINE:
                if (weight <= 300) {
                    return 30;
                } else {
                    int factor = (int) Math.ceil(weight / 10);
                    factor += factor % 5;
                    return factor;
                }

            case TRACKED:
                return 0;
            case WHEELED:
                if (weight <= 80) {
                    return 20;
                } else {
                    return 40;
                }
            case VTOL:
                if (weight <= 10) {
                    return 50;
                }
                if (weight <= 20) {
                    return 95;
                }
                if (weight <= 30) {
                    return 140;
                } else {
                    return 140 + (45 * (int) Math.ceil((weight - 30) / 20f));
                }

            case WIGE:
                if (weight <= 15) {
                    return 45;
                }
                if (weight <= 30) {
                    return 80;
                }
                if (weight <= 45) {
                    return 115;
                }
                if (weight <= 80) {
                    return 140;
                } else {
                    return 140 + (35 * (int) Math.ceil((weight - 80) / 30f));
                }
            default:
                return 0;
        }
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return CombatVehicleCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        double priceMultiplier = 1.0;
        if (isOmni()) {
            priceMultiplier *= 1.25;
        }
        if (isSupportVehicle()
                && (movementMode.equals(EntityMovementMode.NAVAL)
                || movementMode.equals(EntityMovementMode.HYDROFOIL)
                || movementMode.equals(EntityMovementMode.SUBMARINE))) {
            priceMultiplier *= weight / 100000.0;
        } else {
            switch (movementMode) {
                case HOVER:
                case SUBMARINE:
                    priceMultiplier *= weight / 50.0;
                    break;
                case HYDROFOIL:
                    priceMultiplier *= weight / 75.0;
                    break;
                case NAVAL:
                case WHEELED:
                    priceMultiplier *= weight / 200.0;
                    break;
                case TRACKED:
                    priceMultiplier *= weight / 100.0;
                    break;
                case VTOL:
                    priceMultiplier *= weight / 30.0;
                    break;
                case WIGE:
                    priceMultiplier *= weight / 25.0;
                    break;
                case RAIL:
                case MAGLEV:
                    priceMultiplier *= weight / 250.0;
                    break;
                default:
                    break;
            }
        }
        if (!isSupportVehicle()) {
            if (hasWorkingMisc(MiscType.F_FLOTATION_HULL)
                    || hasWorkingMisc(MiscType.F_VACUUM_PROTECTION)
                    || hasWorkingMisc(MiscType.F_ENVIRONMENTAL_SEALING)) {
                priceMultiplier *= 1.25;

            }
            if (hasWorkingMisc(MiscType.F_OFF_ROAD)) {
                priceMultiplier *= 1.2;
            }
        }
        return priceMultiplier;
    }

    @Override
    public int implicitClanCASE() {
        if (!isClan()) {
            return 0;
        }
        int explicit = 0;
        Set<Integer> caseLocations = new HashSet<>();
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType) && (m.getType().hasFlag(MiscType.F_CASE))) {
                explicit++;
            } else if (m.getType().isExplosive(m)) {
                caseLocations.add(m.getLocation());
                if (m.getSecondLocation() >= 0) {
                    caseLocations.add(m.getSecondLocation());
                }
            }
        }
        return Math.max(0, caseLocations.size() - explicit);
    }

    @Override
    public boolean doomedInExtremeTemp() {
        return false;
    }

    @Override
    public boolean doomedInVacuum() {
        if (hasEngine() && (getEngine().isFusion() || getEngine().getEngineType() == Engine.FISSION
                || getEngine().getEngineType() == Engine.FUEL_CELL)) {
            return !hasEnvironmentalSealing();
        }
        return true;
    }

    @Override
    public boolean hasEnvironmentalSealing() {
        return getMovementMode().equals(EntityMovementMode.SUBMARINE)
                || super.hasEnvironmentalSealing();
    }

    @Override
    public boolean doomedOnGround() {
        return false;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return true;
    }

    @Override
    public boolean doomedInSpace() {
        return true;
    }

    /**
     * Checks to see if a Tank is capable of going hull-down.  This is true if
     * hull-down rules are enabled and the Tank is in a fortified hex.
     *
     *  @return True if hull-down is enabled and the Tank is in a fortified hex.
     */
    @Override
    public boolean canGoHullDown() {
        // MoveStep line 2179 performs this same check
        // performing it here will allow us to disable the Hulldown button
        // if the movement is illegal
        Hex occupiedHex = game.getBoard().getHex(getPosition());
        return occupiedHex.containsTerrain(Terrains.FORTIFIED)
                && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN);
    }

    public void setOnFire(boolean inferno) {
        infernoFire |= inferno;
        burningLocations = (1 << locations()) - 1;
        extinguishLocation(LOC_BODY);
    }

    public boolean isOnFire() {
        return (burningLocations != 0) || infernos.isStillBurning();
    }

    public boolean isInfernoFire() {
        return infernoFire;
    }

    public boolean isLocationBurning(int location) {
        int flag = (1 << location);
        return (burningLocations & flag) == flag;
    }

    public void extinguishLocation(int location) {
        int flag = ~(1 << location);
        burningLocations &= flag;
    }

    /**
     * extinguish all inferno fire on this Tank
     */
    public void extinguishAll() {
        burningLocations = 0;
        infernoFire = false;
        infernos.clear();
    }

    /**
     * adds minor, moderate or heavy movement system damage
     *
     * @param level
     *            a <code>int</code> representing minor damage (1), moderate
     *            damage (2), heavy damage (3), or immobilized (4)
     */
    public void addMovementDamage(int level) {
        switch (level) {
            case 1:
                if (!minorMovementDamage) {
                    minorMovementDamage = true;
                    motivePenalty += level;
                }
                break;
            case 2:
                if (!moderateMovementDamage) {
                    moderateMovementDamage = true;
                    motivePenalty += level;
                }
                motiveDamage++;
                break;
            case 3:
                if (!heavyMovementDamage) {
                    heavyMovementDamage = true;
                    motivePenalty += level;
                }
                int nMP = getOriginalWalkMP() - motiveDamage;
                if (nMP > 0) {
                    motiveDamage = getOriginalWalkMP()
                            - (int) Math.ceil(nMP / 2.0);
                }
                break;
            case 4:
                motiveDamage = getOriginalWalkMP();
                immobilize();
        }
    }

    public boolean hasMinorMovementDamage() {
        return minorMovementDamage;
    }

    public boolean hasModerateMovementDamage() {
        return moderateMovementDamage;
    }

    public boolean hasHeavyMovementDamage() {
        return heavyMovementDamage;
    }

    @Override
    public boolean isNuclearHardened() {
        return true;
    }

    @Override
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted)
            throws LocationFullException {
        if (getEquipmentNum(mounted) == -1) {
            super.addEquipment(mounted, loc, rearMounted);
        }
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(mounted));
        if ((mounted.getType() instanceof MiscType)
                && mounted.getType().hasFlag(MiscType.F_JUMP_JET)) {
            setOriginalJumpMP(getOriginalJumpMP() + 1);
        }
    }

    /**
     * get the type of critical caused by a critical roll, taking account of
     * existing damage
     *
     * @param roll the final dice roll
     * @param loc the hit location
     * @param damagedByFire whether or not the critical was caused by fire,
     *      which is distinct from damage for unofficial thresholding purposes.
     * @return a critical type
     */
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
                        if (!getCrew().isDead() && !getCrew().isDoomed()) {
                            if (!isDriverHit()) {
                                return CRIT_DRIVER;
                            } else if (!isCommanderHit()) {
                                return CRIT_CREW_STUNNED;
                            } else {
                                return CRIT_CREW_KILLED;
                            }
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
                        if (getSensorHits() < CRIT_SENSOR_MAX) {
                            return CRIT_SENSOR;
                        }
                    case 10:
                        if (!getCrew().isDead() && !getCrew().isDoomed()) {
                            if (!isCommanderHit()) {
                                return CRIT_COMMANDER;
                            } else if (!isDriverHit()) {
                                return CRIT_CREW_STUNNED;
                            } else {
                                return CRIT_CREW_KILLED;
                            }
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
                            if (!m.isDestroyed() && !m.isHit()
                                    && (m.getLocation() != Entity.LOC_NONE)) {
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
            } else if ((loc == getLocTurret()) || (loc == getLocTurret2())) {
                switch (roll) {
                    case 6:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 7:
                        if (!isTurretLocked(loc)) {
                            return CRIT_TURRET_JAM;
                        }
                    case 8:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isJammed() && !m.isHit()
                                    && !m.jammedThisPhase()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 9:
                        if (!isTurretLocked(loc)) {
                            return CRIT_TURRET_LOCK;
                        }
                    case 10:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 11:
                        for (Mounted m : getAmmo()) {
                            if (!m.isDestroyed() && !m.isHit()
                                    && (m.getLocation() != Entity.LOC_NONE)) {
                                return CRIT_AMMO;
                            }
                        }
                    case 12:
                        return CRIT_TURRET_DESTROYED;
                }
            } else {
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
                        if (!getCrew().isDead() && !getCrew().isDoomed()) {
                            if (isCommanderHit() && isDriverHit()) {
                                return CRIT_CREW_KILLED;
                            }
                            return CRIT_CREW_STUNNED;
                        }
                    case 9:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 10:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
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
            }
        }
        return CRIT_NONE;
    }

    /**
     * OmniVehicles have handles for Battle Armor squads to latch onto. Please
     * note, this method should only be called during this Tank's construction.
     * <p>
     * Overrides <code>Entity#setOmni(boolean)</code>
     */
    @Override
    public void setOmni(boolean omni) {

        // Perform the superclass' action.
        super.setOmni(omni);

        // Add BattleArmorHandles to OmniMechs.
        if (omni && !hasBattleArmorHandles()) {
            addTransporter(new BattleArmorHandlesTank());
        }
    }

    // Set whether a non-omni should have BA Grab Bars.
    public void setBAGrabBars() {
        if (isOmni()) {
            return;
        }
        // TODO: I really hate this optional rule - what if some units are
        // already loaded?
        // if ba_grab_bars is on, then we need to add battlearmor handles,
        // otherwise clamp mounts
        // but first clear out whatever we have
        Vector<Transporter> et = new Vector<>(getTransports());
        for (Transporter t : et) {
            if (t instanceof BattleArmorHandlesTank) {
                removeTransporter(t);
            }
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_BA_GRAB_BARS)) {
            addTransporter(new BattleArmorHandlesTank());
        } else {
            addTransporter(new ClampMountTank());
        }
    }

    /**
     * Add a transporter for each trailer hitch the unit is equipped with, with a maximum of
     * one each in the front and the rear. Any tractor that does not have an explicit hitch
     * installed as equipment will get a rear-facing transporter.
     */
    public void setTrailerHitches() {
        if (isTractor() && !hasTrailerHitchTransporter()) {
            // look for explicit installed trailer hitches and note location
            boolean front = false;
            boolean rear = false;
            for (Mounted m : getMisc()) {
                if (m.getType().hasFlag(MiscType.F_HITCH)) {
                    if (m.getLocation() == Tank.LOC_FRONT && !isTrailer()) {
                        front = true;
                    } else {
                        rear = true;
                    }
                }
            }
            // Install a transporter anywhere there is an explicit hitch. If no hitch is found,
            // put it in the back.
            if (front) {
                addTransporter(new TankTrailerHitch(false));
            }
            if (rear || !front) {
                addTransporter(new TankTrailerHitch(true));
            }
        }
    }

    /**
     * Check to see if the unit has a trailer hitch transporter already
     * We need this to prevent duplicate transporters being created
     */
    protected boolean hasTrailerHitchTransporter() {
        for (Transporter t : getTransports()) {
            if (t instanceof TankTrailerHitch) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tanks can't spot when stunned.
     */
    @Override
    public boolean canSpot() {
        return super.canSpot() && (getStunnedTurns() == 0);
    }

    /**
     * Convenience function that determines if this tank can issue an "unjam weapon" command.
     * @return True if there are any jammed weapons and the crew isn't stunned
     */
    public boolean canUnjamWeapon() {
        return !getJammedWeapons().isEmpty() && getStunnedTurns() <= 0;
    }

    /**
     * Convenience function that determines if this tank can issue a "clear turret" command.
     * @return True if there are any jammed turrets and the crew isn't stunned
     */
    public boolean canClearTurret() {
        return (m_bTurretJammed || m_bDualTurretJammed) && getStunnedTurns() <= 0;
    }

    public void addJammedWeapon(Mounted weapon) {
        jammedWeapons.add(weapon);
    }

    public ArrayList<Mounted> getJammedWeapons() {
        return jammedWeapons;
    }

    public void resetJammedWeapons() {
        jammedWeapons = new ArrayList<>();
    }

    /**
     * apply the effects of an "engine hit" crit
     */
    public void engineHit() {
        engineHit = true;
        immobilize();
        lockTurret(getLocTurret());
        lockTurret(getLocTurret2());
        for (Mounted m : getWeaponList()) {
            WeaponType wtype = (WeaponType) m.getType();
            if (wtype.hasFlag(WeaponType.F_ENERGY)
            // Chemical lasers still work even after an engine hit.
                    && !(wtype instanceof CLChemicalLaserWeapon)
                    // And presumably vehicle flamers should, too; we can always
                    // remove this again if ruled otherwise.
                    && !(wtype instanceof VehicleFlamerWeapon)) {
                m.setBreached(true); // not destroyed, just unpowered
            }
        }
    }

    public void engineFix() {
        engineHit = false;
        unlockTurret();
        for (Mounted m : getWeaponList()) {
            WeaponType wtype = (WeaponType) m.getType();
            if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                // not destroyed, just unpowered
                m.setBreached(false);
            }
        }
    }

    public boolean isEngineHit() {
        return engineHit;
    }

    @Override
    public int getTotalCommGearTons() {
        return 1 + getExtraCommGearTons();
    }

    @Override
    public int getHQIniBonus() {
        int bonus = super.getHQIniBonus();
        if (((stabiliserHits > 0) && (mpUsedLastRound > 0)) || commanderHit) {
            return 0;
        }
        return bonus;
    }

    @Override
    public boolean hasArmoredEngine() {
        for (int slot = 0; slot < getNumberOfCriticals(LOC_BODY); slot++) {
            CriticalSlot cs = getCritical(LOC_BODY, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM)
                    && (cs.getIndex() == Mech.SYSTEM_ENGINE)) {
                return cs.isArmored();
            }
        }
        return false;
    }

    /**
     * see {@link Entity#getForwardArc()}
     */
    @Override
    public int getForwardArc() {
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
            return Compute.ARC_NOSE;
        }
        return super.getForwardArc();
    }

    /**
     * see {@link Entity#getRearArc()}
     */
    @Override
    public int getRearArc() {
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
            return Compute.ARC_AFT;
        }
        return super.getRearArc();
    }

    public boolean hasMovementDamage() {
        return motivePenalty > 0;
    }

    public void resetMovementDamage() {
        motivePenalty = 0;
        motiveDamage = 0;
        minorMovementDamage = false;
        moderateMovementDamage = false;
        heavyMovementDamage = false;
        m_bImmobileHit = false;
        m_bImmobile = false;
    }

    public void unlockTurret() {
        m_bTurretLocked = false;
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth
     * can be active and not working when under ECCM)
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    @Override
    public boolean isStealthActive() {
        // Try to find a Mek Stealth system.
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_STEALTH)) {

                if (mEquip.curMode().equals("On") && hasActiveECM()) {
                    // Return true if the mode is "On" and ECM is working
                    return true;
                }
            }
        }
        // No Mek Stealth or system inactive. Return false.
        return false;
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth
     * can be active and not working when under ECCM)
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @return <code>true</code> if this unit has a stealth system that is
     *         currently active, <code>false</code> if there is no stealth
     *         system or if it is inactive.
     */
    @Override
    public boolean isStealthOn() {
        // Try to find a Mek Stealth system.
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_STEALTH)) {
                if (mEquip.curMode().equals("On")) {
                    // Return true if the mode is "On"
                    return true;
                }
            }
        }
        // No Mek Stealth or system inactive. Return false.
        return false;
    }

    /**
     * get the total amount of item slots available for this tank
     *
     * @return
     */
    public int getTotalSlots() {
        return 5 + (int) Math.floor(getWeight() / 5);
    }

    /**
     * get the free item slots for this tank
     *
     * @return
     */
    public int getFreeSlots() {
        int availableSlots = getTotalSlots();
        int usedSlots = 0;
        boolean addedCargo = false;
        for (Mounted mount : this.getEquipment()) {
            if ((mount.getType() instanceof MiscType)
                    && mount.getType().hasFlag(MiscType.F_CARGO)) {
                if (!addedCargo) {
                    usedSlots += mount.getType().getTankSlots(this);
                    addedCargo = true;
                }
                continue;
            }
            if ((mount.getType() instanceof MiscType)
                    && (mount.getType().hasFlag(MiscType.F_JUMP_JET)
                        || mount.getType().hasFlag(MiscType.F_FUEL))) {
                // Only one slot each for all jump jets or fuel tanks, added later.
                continue;
            }
            if (!((mount.getType() instanceof AmmoType) || EquipmentType.isArmorType(mount.getType()))) {
                usedSlots += mount.getType().getTankSlots(this);
            }
        }
        // JJs take just 1 slot
        if (this.getJumpMP(MPCalculationSetting.NO_GRAVITY) > 0) {
            usedSlots++;
        }
        // So do fuel tanks
        if (hasWorkingMisc(MiscType.F_FUEL)) {
            usedSlots++;
        }
        // different engines take different amounts of slots
        if (hasEngine() && getEngine().isFusion()) {
            if (getEngine().getEngineType() == Engine.LIGHT_ENGINE) {
                usedSlots++;
            }
            if (getEngine().getEngineType() == Engine.XL_ENGINE) {
                if (getEngine().hasFlag(Engine.CLAN_ENGINE)) {
                    usedSlots++;
                } else {
                    usedSlots += 2;
                }
            }
            if (getEngine().getEngineType() == Engine.XXL_ENGINE) {
                if (getEngine().hasFlag(Engine.CLAN_ENGINE)) {
                    usedSlots += 2;
                } else {
                    usedSlots += 4;
                }
            }
        }
        if (hasEngine() && getEngine().hasFlag(Engine.LARGE_ENGINE)) {
            usedSlots++;
        }
        if (hasEngine() && getEngine().getEngineType() == Engine.COMPACT_ENGINE) {
            usedSlots--;
        }
        // for ammo, each type of ammo takes one slots, regardless of
        // submunition type
        Set<String> foundAmmo = new HashSet<>();
        for (Mounted ammo : getAmmo()) {
            // don't count oneshot ammo
            if (ammo.isOneShotAmmo()) {
                continue;
            }
            AmmoType at = (AmmoType) ammo.getType();
            if (!foundAmmo.contains(at.getAmmoType() + ":" + at.getRackSize())) {
                usedSlots++;
                foundAmmo.add(at.getAmmoType() + ":" + at.getRackSize());
            }
        }
        // if a tank has an infantry bay, add 1 slots (multiple bays take 1 slot
        // total)
        boolean infantryBayCounted = false;
        for (Transporter transport : getTransports()) {
            if (transport instanceof TroopSpace) {
                usedSlots++;
                infantryBayCounted = true;
                break;
            }
        }
        // unit transport bays take 1 slot each
        for (Bay bay : getTransportBays()) {
            if (((bay instanceof BattleArmorBay) || (bay instanceof InfantryBay))
                    && !infantryBayCounted) {
                usedSlots++;
                infantryBayCounted = true;
            } else {
                usedSlots++;
            }
        }
        usedSlots += extraCrewSeats;
        // different armor types take different amount of slots
        if (!hasPatchworkArmor()) {
            int type = getArmorType(1);
            switch (type) {
                case EquipmentType.T_ARMOR_FERRO_FIBROUS:
                    if (TechConstants.isClan(getArmorTechLevel(1))) {
                        usedSlots++;
                    } else {
                        usedSlots += 2;
                    }
                    break;
                case EquipmentType.T_ARMOR_HEAVY_FERRO:
                    usedSlots += 3;
                    break;
                case EquipmentType.T_ARMOR_LIGHT_FERRO:
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_REFLECTIVE:
                case EquipmentType.T_ARMOR_HARDENED:
                case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                    usedSlots++;
                    break;
                case EquipmentType.T_ARMOR_STEALTH_VEHICLE:
                    usedSlots += 2;
                    break;
                case EquipmentType.T_ARMOR_REACTIVE:
                    if (TechConstants.isClan(getArmorTechLevel(1))) {
                        usedSlots++;
                    } else {
                        usedSlots += 2;
                    }
                    break;
                default:
                    break;
            }
        } else {
            for (int loc = 1; loc < locations(); loc++) {
                switch (getArmorType(loc)) {
                    case EquipmentType.T_ARMOR_HEAVY_FERRO:
                        usedSlots += 2;
                        break;
                    case EquipmentType.T_ARMOR_FERRO_FIBROUS:
                    case EquipmentType.T_ARMOR_LIGHT_FERRO:
                    case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                    case EquipmentType.T_ARMOR_REFLECTIVE:
                    case EquipmentType.T_ARMOR_STEALTH_VEHICLE:
                    case EquipmentType.T_ARMOR_REACTIVE:
                    case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                    case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                        usedSlots++;
                        break;
                    default:
                        break;
                }
            }
        }
        return availableSlots - usedSlots;
    }

    @Override
    public void setArmorType(int armType) {
        setArmorType(armType, true);
    }

    public void setArmorType(int armType, boolean addMount) {
        super.setArmorType(armType);
        if ((armType == EquipmentType.T_ARMOR_STEALTH_VEHICLE) && addMount) {
            try {
                this.addEquipment(EquipmentType.get(EquipmentType
                        .getArmorTypeName(
                                EquipmentType.T_ARMOR_STEALTH_VEHICLE, false)),
                        LOC_BODY);
            } catch (LocationFullException e) {
                // this should never happen
            }
        }
    }

    /**
     * Checks if a mech has an armed MASC system. Note that the mech will have
     * to exceed its normal run to actually engage the MASC system
     */
    public boolean hasArmedMASC() {
        for (Mounted m : getEquipment()) {
            if (!m.isDestroyed()
                    && !m.isBreached()
                    && (m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_MASC)
                    && (m.curMode().equals("Armed") || m.getType().hasSubType(
                            MiscType.S_JETBOOSTER))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns this entity's running/flank mp as a string.
     */
    @Override
    public String getRunMPasString() {
        MPBoosters mpBoosters = getMPBoosters();
        if (!mpBoosters.isNone()) {
            String str = getRunMPwithoutMASC() + "(" + getRunMP() + ")";
            if (game != null) {
                MPBoosters armed = getArmedMPBoosters();

                str += (mpBoosters.hasMASC() ? " MASC:" + getMASCTurns()
                        + (armed.hasMASC() ? "(" + getMASCTarget() + "+)" : "(NA)") : "")
                        + (mpBoosters.hasSupercharger() ? " Supercharger:" + getSuperchargerTurns()
                        + (armed.hasSupercharger() ? "(" + getSuperchargerTarget() + "+)" : "(NA)") : "");
            }
            return str;
        }
        return Integer.toString(getRunMP());
    }

    /**
     * Determine the stealth modifier for firing at this unit from the given
     * range. If the value supplied for <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown.
     * <p>
     * Sub-classes are encouraged to override this method.
     *
     * @param range
     *            - an <code>int</code> value that must match one of the
     *            <code>Compute</code> class range constants.
     * @param ae
     *            - entity making the attack
     * @return a <code>TargetRoll</code> value that contains the stealth
     *         modifier for the given range.
     */
    @Override
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // Stealth or null sig must be active.
        if (!isStealthActive()) {
            result = new TargetRoll(0, "stealth not active");
        }
        // Determine the modifier based upon the range.
        else {
            switch (range) {
                case RangeType.RANGE_MINIMUM:
                case RangeType.RANGE_SHORT:
                    if (isStealthActive() && !ae.isConventionalInfantry()) {
                        result = new TargetRoll(0, "stealth");
                    } else {
                        // must be infantry
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_MEDIUM:
                    if (isStealthActive() && !ae.isConventionalInfantry()) {
                        result = new TargetRoll(1, "stealth");
                    } else {
                        // must be infantry
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_LONG:
                case RangeType.RANGE_EXTREME:
                case RangeType.RANGE_LOS:
                    if (isStealthActive() && !ae.isConventionalInfantry()) {
                        result = new TargetRoll(2, "stealth");
                    } else {
                        // must be infantry
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_OUT:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown range constant: " + range);
            }
        }

        // Return the result.
        return result;
    } // End public TargetRoll getStealthModifier( char )

    @Override
    public int getEngineHits() {
        if (isEngineHit()) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String getLocationDamage(int loc) {
        String toReturn = "";
        boolean first = true;
        if (getLocationStatus(loc) == ILocationExposureStatus.BREACHED) {
            toReturn += "BREACH";
            first = false;
        }
        if (isTurretLocked(loc)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Locked";
            first = false;
        }
        if (isStabiliserHit(loc)) {
            if (!first) {
                toReturn += ", ";
            }
            toReturn += "Stabilizer hit";
            first = false;
        }
        return toReturn;
    }

    @Override
    public boolean isCrippled() {
        return isCrippled(true);
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        if ((getArmor(LOC_FRONT) < 1) && (getOArmor(LOC_FRONT) > 0)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Front armor destroyed.");
            return true;
        } else if ((getArmor(LOC_RIGHT) < 1) && (getOArmor(LOC_RIGHT) > 0)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Right armor destroyed.");
            return true;
        } else if ((getArmor(LOC_LEFT) < 1) && (getOArmor(LOC_LEFT) > 0)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Left armor destroyed.");
            return true;
        } else if (!hasNoTurret() && ((getArmor(getLocTurret()) < 1) && (getOArmor(getLocTurret()) > 0))) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Turret destroyed.");
            return true;
        } else if (!hasNoDualTurret() && ((getArmor(getLocTurret2()) < 1) && (getOArmor(getLocTurret2()) > 0))) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Front Turret destroyed.");
            return true;
        } else if ((getArmor(LOC_REAR) < 1) && (getOArmor(LOC_REAR) > 0)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Rear armor destroyed.");
            return true;
        } else if (isPermanentlyImmobilized(checkCrew)) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Immobilized.");
            return true;
        }

        // If this is not a military vehicle, we don't need to do a weapon
        // check.
        if (!isMilitary()) {
            return false;
        }

        // no weapons can fire anymore, can cause no more than 5 points of
        // combined weapons damage,
        // or has no weapons with range greater than 5 hexes
        if (!hasViableWeapons()) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: has no more viable weapons.");
            return true;
        }
        return false;
    }

    @Override
    public boolean isDmgHeavy() {
        // when checking if MP has been reduced, we want to ignore non-damage effects such as weather/gravity
        if (((double) getMotiveDamage() / getOriginalWalkMP()) >= 0.5) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Lightly Damaged: Walk MP less than or equal to half the original Walk MP");
            return true;
        } else if ((getArmorRemainingPercent() <= 0.33) && (getArmorRemainingPercent() != IArmorState.ARMOR_NA)) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Heavily Damaged: Armour Remaining percent of " + getArmorRemainingPercent()
                    + " is less than or equal to 0.33.");
            return true;
        }

        // If this is not a military vehicle, we don't need to do a weapon
        // check.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double) totalInoperable / totalWeapons) >= 0.75;
    }

    @Override
    public boolean isDmgModerate() {
        if ((getArmorRemainingPercent() <= 0.67) && (getArmorRemainingPercent() != IArmorState.ARMOR_NA)) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Moderately Damaged: Armour Remaining percent of " + getArmorRemainingPercent()
                    + " is less than or equal to 0.67.");
            return true;
        }

        // If this is not a military vehicle, we don't need to do a weapon
        // check.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }

        return ((double) totalInoperable / totalWeapons) >= 0.5;
    }

    @Override
    public boolean isDmgLight() {
        // when checking if MP has been reduced, we want to ignore non-damage effects such as weather/gravity
        if (getMotiveDamage() > 0) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Lightly Damaged: Walk MP less than the original Walk MP");
            return true;
        } else if ((getArmorRemainingPercent() <= 0.8) && (getArmorRemainingPercent() != IArmorState.ARMOR_NA)) {
            LogManager.getLogger().debug(getDisplayName()
                    + " Lightly Damaged: Armour Remaining percent of " + getArmorRemainingPercent()
                    + " is less than or equal to 0.8.");
            return true;
        }

        // If this is not a military vehicle, we don't need to do a weapon
        // check.
        if (!isMilitary()) {
            return false;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }

        return ((double) totalInoperable / totalWeapons) >= 0.25;
    }

    /**
     * Returns the mass of the fuel, which is used for calculating operating range.
     * For combat vehicles this is considered part of the engine weight. For support vehicles it is in
     * addition to the engine weight.
     *
     * @return fuel tonnage
     */
    public double getFuelTonnage() {
        if (hasEngine()) {
            // For combat vehicles the fuel tonnage is 10% of the engine.
            return getEngine().getWeightEngine(this) * 0.1;
        }
        return 0.0;
    }

    /**
     * Sets the fuel mass for support vehicles. Has no effect on combat vehicles.
     *
     * @param fuel The mass of the fuel in tons
     */
    public void setFuelTonnage(double fuel) {
        // do nothing
    }

    /**
     * Calculates the operating range of the vehicle based on engine type and fuel mass.
     * Vehicles that do not require fuel report an operating range of {@code Integer.MAX_VALUE}.
     *
     * @return The vehicle's operating range in km
     */
    public int operatingRange() {
        if (getFuelTonnage() <= 0) {
            return 0;
        }
        double fuelUnit = fuelTonnagePer100km();
        if (fuelUnit > 0) {
            int range = (int) (getFuelTonnage() / fuelUnit * 100);
            int fuelTanks = countWorkingMisc(MiscType.F_FUEL);
            if (getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) {
                range += fuelTanks * 600;
            } else if (getEngine().getEngineType() == Engine.FUEL_CELL) {
                range += fuelTanks * 450;
            }
            return range;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Calculates fuel mass based on engine type and mass. Engines that do not require allocating
     * fuel mass return a value of 0.0.
     *
     * @return The fuel mass required for every 100 km of vehicle range.
     */
    public double fuelTonnagePer100km() {
        if (!hasEngine()) {
            return 0.0;
        }
        switch (getEngine().getEngineType()) {
            case Engine.STEAM:
                return getEngine().getWeightEngine(this) * 0.03;
            case Engine.COMBUSTION_ENGINE:
                if (getICEFuelType() == FuelType.PETROCHEMICALS) {
                    return getEngine().getWeightEngine(this) * 0.01;
                } else {
                    return getEngine().getWeightEngine(this) * 0.0125;
                }
            case Engine.BATTERY:
                return getEngine().getWeightEngine(this) * 0.05;
            case Engine.FUEL_CELL:
                return getEngine().getWeightEngine(this) * 0.015;
            default:
                return 0.0;
        }
    }

    /**
     * The type of fuel for internal combustion engines. This has no meaning for
     * other engine types.
     *
     * @return The ICE fuel type
     */
    public FuelType getICEFuelType() {
        return fuelType;
    }

    public void setICEFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    /**
     * Tanks go Hull Down slightly differently, this method accounts for this
     *
     * @see megamek.common.Entity#setHullDown(boolean)
     */
    @Override
    public void setHullDown(boolean down) {
        super.setHullDown(down);
        m_bBackedIntoHullDown = getMovedBackwards() && down;
    }

    /**
     * Returns True if this tank moved backwards before going Hull Down
     *
     * @return
     */
    public boolean isBackedIntoHullDown() {
        return m_bBackedIntoHullDown;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_TANK;
    }

    @Override
    public boolean isEjectionPossible() {
        return game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLES_CAN_EJECT)
                && getCrew().isActive()
                && !hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT);
    }

    /**
     * Used for omni vehicles, which must set the weight of turrets
     * the base chassis, limiting the amount of pod space in the turret.
     *
     * @return The weight of the primary turret
     */
    public double getBaseChassisTurretWeight() {
        return baseChassisTurretWeight;
    }

    /**
     * Sets the fixed weight of the primary turret on a dual-turret omnivehicle.
     *
     * @param baseChassisTurretWeight The weight of the turret
     */
    public void setBaseChassisTurretWeight(double baseChassisTurretWeight) {
        this.baseChassisTurretWeight = baseChassisTurretWeight;
    }

    /**
     * Used for omni vehicles, which must set the weight of turrets
     * the base chassis, limiting the amount of pod space in the turret.
     *
     * @return The weight of the second turret
     */
    public double getBaseChassisTurret2Weight() {
        return baseChassisTurret2Weight;
    }

    /**
     * Sets the fixed weight of the second turret on a dual-turret omnivehicle.
     *
     * @param baseChassisTurret2Weight The weight of the turret
     */
    public void setBaseChassisTurret2Weight(double baseChassisTurret2Weight) {
        this.baseChassisTurret2Weight = baseChassisTurret2Weight;
    }

    /**
     * Used for omni vehicles, which must set the weight of sponson or pintle mounts in
     * the base chassis, limiting the amount of pod space in the turret(s).
     *
     * @return The weight of any pintle mounts (small support vee) or sponson mounts
     *         (combat vee and M/L support vee)
     */
    public double getBaseChassisSponsonPintleWeight() {
        return baseChassisSponsonPintleWeight;
    }

    /**
     * Sets the fixed weight of any pintle (small SV) or sponson (CV, M/L SV) turrets
     * on an omnivehicle.
     *
     * @param baseChassisSponsonPintleWeight The weight of the sponson/pintle turrets.
     */
    public void setBaseChassisSponsonPintleWeight(double baseChassisSponsonPintleWeight) {
        this.baseChassisSponsonPintleWeight = baseChassisSponsonPintleWeight;
    }

    public boolean hasNoControlSystems() {
        return hasNoControlSystems;
    }

    public void setHasNoControlSystems(boolean hasNoControlSystems) {
        this.hasNoControlSystems = hasNoControlSystems;
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
        return 4;
    }

    //Specific road/rail train rules

    /**
     * Used to determine if this vehicle can be towed by a tractor
     *
     * @return Whether the unit is constructed as a trailer
     */
    @Override
    public boolean isTrailer() {
        return trailer;
    }

    /**
     * Marks whether the tank is constructed as a trailer. This has no effect on
     * support vehicles, which determine trailer status by the trailer chassis modification.
     *
     * @param trailer Whether the tank is constructed as a trailer.
     */
    public void setTrailer(boolean trailer) {
        this.trailer = trailer;
    }

    /**
     * Used to determine if this vehicle can be the engine/tractor
     * for a bunch of trailers
     *
     * @return
     */
    @Override
    public boolean isTractor() {
        if (getMovementMode().equals(EntityMovementMode.TRACKED)
                || getMovementMode().equals(EntityMovementMode.WHEELED)) {
            // Any tracked or wheeled combat vehicle can be used as a tractor
            // if it is capable of independent operations or it is equipped with
            // a hitch (making it a trailer that is part of a train).
            return (hasEngine() && !hasNoControlSystems())
                    || hasWorkingMisc(MiscType.F_HITCH);
        }
        return false;
    }

    @Override
    public boolean isCombatVehicle() {
        return !isSupportVehicle();
    }

    @Override
    public boolean getsAutoExternalSearchlight() {
        return true;
    }
}
