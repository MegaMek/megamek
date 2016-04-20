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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.verifier.SupportVeeStructure;
import megamek.common.weapons.CLChemicalLaserWeapon;
import megamek.common.weapons.VehicleFlamerWeapon;

/**
 * You know what tanks are, silly.
 */
public class Tank extends Entity {
    /**
     *
     */
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
    private ArrayList<Mounted> jammedWeapons = new ArrayList<Mounted>();
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

    // tanks have no critical slot limitations
    private static final int[] NUM_OF_SLOTS = { 25, 25, 25, 25, 25, 25, 25 };

    private static String[] LOCATION_ABBRS = { "BD", "FR", "RS", "LS", "RR",
            "TU", "FT" };
    private static String[] LOCATION_NAMES = { "Body", "Front", "Right",
            "Left", "Rear", "Turret" };

    private static String[] LOCATION_NAMES_DUAL_TURRET = { "Body", "Front",
            "Right", "Left", "Rear", "Rear Turret", "Front Turret" };

    public static float[] BAR_ARMOR_COST_MULT = { 0, 0, 50, 100, 150, 200, 250,
            300, 400, 500, 625 };

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
    private double baseChassisTurretWeight = -1;
    private double baseChassisTurret2Weight = -1;

    /**
     * Keeps track of whether this vehicle has control systems.  Trailers aren't
     * required to have control systems.
     */
    private boolean hasNoControlSystems = false;

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

    /**
     * The attack direction modifier for rolls on the motive system hits table
     * for the given side (as defined in {@link ToHitData}). This will return 0
     * if Tactical Operations vehicle effectiveness rules are in effect or if
     * the side parameter falls outside ToHitData's range of "fixed" side
     * values; in particular, it will return 0 if handed
     * {@link ToHitData#SIDE_RANDOM}.
     *
     * @param side
     *            The attack direction as specified above.
     * @return The appropriate directional roll modifier.
     */
    public int getMotiveSideMod(int side) {
        if (game.getOptions().booleanOption("tacops_vehicle_effective")) {
            return 0;
        }
        switch (side) {
            case ToHitData.SIDE_LEFT:
            case ToHitData.SIDE_RIGHT:
            case ToHitData.SIDE_FRONTLEFT:
            case ToHitData.SIDE_FRONTRIGHT:
            case ToHitData.SIDE_REARLEFT:
            case ToHitData.SIDE_REARRIGHT:
                return 2;
            case ToHitData.SIDE_REAR:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * Returns this entity's walking/cruising mp, factored for heat, extreme
     * temperatures, and gravity.
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        return getWalkMP(gravity, ignoreheat, false);
    }

    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        int j = getOriginalWalkMP();
        if (engineHit) {
            return 0;
        }
        j = Math.max(0, j - motiveDamage);
        j = Math.max(0, j - getCargoMpReduction());
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions()
                    .getMovementMods(this);
            if (weatherMod != 0) {
                j = Math.max(j + weatherMod, 0);
            }
        }

        if (!ignoremodulararmor && hasModularArmor()) {
            j--;
        }
        if (hasWorkingMisc(MiscType.F_DUNE_BUGGY) && (game != null)) {
            j--;
        }

        if (gravity) {
            j = applyGravityEffectsOnMP(j);
        }

        return j;

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
    public boolean canChangeSecondaryFacing() {
        return !m_bHasNoTurret && !isTurretLocked(getLocTurret());
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
    public boolean isImmobile() {
        if ((game != null)
                && game.getOptions().booleanOption("no_immobile_vehicles")) {
            return super.isImmobile();
        }
        return super.isImmobile() || m_bImmobile;
    }

    /**
     * Tanks have all sorts of prohibited terrain.
     */
    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        IHex hex = game.getBoard().getHex(c);
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        boolean hasFlotationHull = hasWorkingMisc(MiscType.F_FLOTATION_HULL);
        boolean isAmphibious = hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS);

        switch (movementMode) {
            case TRACKED:
                if (!isSuperHeavy()) {
                    return (hex.terrainLevel(Terrains.WOODS) > 1)
                            || ((hex.terrainLevel(Terrains.WATER) > 0)
                                    && !hex.containsTerrain(Terrains.ICE)
                                    && !hasFlotationHull && !isAmphibious)
                            || hex.containsTerrain(Terrains.JUNGLE)
                            || (hex.terrainLevel(Terrains.MAGMA) > 1)
                            || (hex.terrainLevel(Terrains.ROUGH) > 1);
                } else {
                    return (hex.terrainLevel(Terrains.WOODS) > 1)
                            || ((hex.terrainLevel(Terrains.WATER) > 0)
                                    && !hex.containsTerrain(Terrains.ICE)
                                    && !hasFlotationHull && !isAmphibious)
                            || hex.containsTerrain(Terrains.JUNGLE)
                            || (hex.terrainLevel(Terrains.MAGMA) > 1);
                }
            case WHEELED:
                if (!isSuperHeavy()) {
                    return hex.containsTerrain(Terrains.WOODS)
                            || hex.containsTerrain(Terrains.ROUGH)
                            || ((hex.terrainLevel(Terrains.WATER) > 0)
                                    && !hex.containsTerrain(Terrains.ICE)
                                    && !hasFlotationHull && !isAmphibious)
                            || hex.containsTerrain(Terrains.RUBBLE)
                            || hex.containsTerrain(Terrains.MAGMA)
                            || hex.containsTerrain(Terrains.JUNGLE)
                            || (hex.terrainLevel(Terrains.SNOW) > 1)
                            || (hex.terrainLevel(Terrains.GEYSER) == 2);
                } else {
                    return hex.containsTerrain(Terrains.WOODS)
                            || hex.containsTerrain(Terrains.ROUGH)
                            || ((hex.terrainLevel(Terrains.WATER) > 0)
                                    && !hex.containsTerrain(Terrains.ICE)
                                    && !hasFlotationHull && !isAmphibious)
                            || hex.containsTerrain(Terrains.RUBBLE)
                            || hex.containsTerrain(Terrains.MAGMA)
                            || hex.containsTerrain(Terrains.JUNGLE)
                            || (hex.terrainLevel(Terrains.GEYSER) == 2);
                }
            case HOVER:
                if (!isSuperHeavy()) {
                    return hex.containsTerrain(Terrains.WOODS)
                            || hex.containsTerrain(Terrains.JUNGLE)
                            || (hex.terrainLevel(Terrains.MAGMA) > 1)
                            || (hex.terrainLevel(Terrains.ROUGH) > 1);
                } else {
                    return hex.containsTerrain(Terrains.WOODS)
                            || hex.containsTerrain(Terrains.JUNGLE)
                            || (hex.terrainLevel(Terrains.MAGMA) > 1);
                }

            case NAVAL:
            case HYDROFOIL:
                return (hex.terrainLevel(Terrains.WATER) <= 0)
                        || hex.containsTerrain(Terrains.ICE);
            case SUBMARINE:
                return (hex.terrainLevel(Terrains.WATER) <= 0);
            case WIGE:
                return (hex.containsTerrain(Terrains.WOODS) || (hex
                        .containsTerrain(Terrains.BUILDING)))
                        && !(currElevation > hex
                                .maxTerrainFeatureElevation(game.getBoard()
                                        .inAtmosphere()));
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
        m_bImmobile |= m_bImmobileHit;
        super.applyDamage();
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        // If MASC was used last turn, increment the counter,
        // otherwise decrement. Then, clear the counter
        if (usedMASC) {
            nMASCLevel++;
            bMASCWentUp = true;
        } else {
            nMASCLevel = Math.max(0, nMASCLevel - 1);
            if (bMASCWentUp) {
                nMASCLevel = Math.max(0, nMASCLevel - 1);
                bMASCWentUp = false;
            }
        }

        // Clear the MASC flag
        usedMASC = false;

        // check for crew stun
        if (m_nStunnedTurns > 0) {
            m_nStunnedTurns--;
        }

        // reset turret facing, if not jammed
        if (!m_bTurretLocked) {
            setSecondaryFacing(getFacing());
        }
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
                return "Cruised";
            case MOVE_RUN:
                return "Flanked";
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
                return "C";
            case MOVE_RUN:
                return "F";
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
            switch (mounted.getFacing()) {
                case 0:
                    return Compute.ARC_HEXSIDE_0;
                case 1:
                    return Compute.ARC_HEXSIDE_1;
                case 2:
                    return Compute.ARC_HEXSIDE_2;
                case 3:
                    return Compute.ARC_HEXSIDE_3;
                case 4:
                    return Compute.ARC_HEXSIDE_4;
                case 5:
                    return Compute.ARC_HEXSIDE_5;
            }
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
                if (game.getOptions().booleanOption("tacops_vehicle_arcs")) {
                    return Compute.ARC_NOSE;
                }
            case LOC_TURRET:
                if (game.getOptions().booleanOption("tacops_vehicle_arcs")) {
                    return Compute.ARC_TURRET;
                }
                return Compute.ARC_FORWARD;
            case LOC_TURRET_2:
                // Doubles as chin turret location for VTOLs, for which
                // Tank.LOC_TURRET == magic number 5 == VTOL.LOC_ROTOR.
                if (game.getOptions().booleanOption("tacops_vehicle_arcs")) {
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
                if (game.getOptions().booleanOption("tacops_vehicle_arcs")) {
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
                if (game.getOptions().booleanOption("tacops_vehicle_arcs")) {
                    return Compute.ARC_LEFT_BROADSIDE;
                }
                return Compute.ARC_LEFTSIDE;
            case LOC_REAR:
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_REAR;
                }
                if (game.getOptions().booleanOption("tacops_vehicle_arcs")) {
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
        if (getEquipment(weaponId).getLocation() == getLocTurret()) {
            return true;
        }
        return false;
    }

    /**
     * Rolls up a hit location
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode, int cover) {
        int nArmorLoc = LOC_FRONT;
        boolean bSide = false;
        boolean bRear = false;
        boolean ignoreTurret = m_bHasNoTurret
                || (table == ToHitData.HIT_UNDERWATER);
        int motiveMod = getMotiveSideMod(side);
        setPotCrit(HitData.EFFECT_NONE);
        if (isHullDown()) {
            // which direction a tank moved in before going hull down is
            // important
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
                    // it
                    // went Hull Down in go to turret if one exists.
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
                    nArmorLoc = side;
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
        if ((aimedLocation != LOC_NONE)
                && (aimingMode != IAimingModes.AIM_MODE_NONE)) {

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
                    if (game.getOptions().booleanOption("vehicles_threshold")) {
                        setPotCrit(HitData.EFFECT_CRITICAL);
                    } else {
                        rv.setEffect(HitData.EFFECT_CRITICAL);
                    }
                    break;
                case 3:
                    if (game.getOptions().booleanOption("vehicles_threshold")) {
                        setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                    rv.setMotiveMod(motiveMod);
                    break;
                case 4:
                    if (game.getOptions().booleanOption("vehicles_threshold")) {
                        setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                    rv.setMotiveMod(motiveMod);
                    break;
                case 5:
                    if (bSide) {
                        if (game.getOptions().booleanOption(
                                "vehicles_threshold")) {
                            setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                            rv = new HitData(LOC_FRONT);
                        } else {
                            rv = new HitData(LOC_FRONT, false,
                                    HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                        }
                    } else if (bRear) {
                        if (game.getOptions().booleanOption(
                                "vehicles_threshold")) {
                            setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                            rv = new HitData(LOC_LEFT);
                        } else {
                            rv = new HitData(LOC_LEFT, false,
                                    HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                        }
                    } else {
                        if (game.getOptions().booleanOption(
                                "vehicles_threshold")) {
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
                                    "tacops_vehicle_effective")) {
                        if (game.getOptions().booleanOption(
                                "vehicles_threshold")) {
                            setPotCrit(HitData.EFFECT_CRITICAL);
                        } else {
                            rv.setEffect(HitData.EFFECT_CRITICAL);
                        }
                    }
                    break;
                case 9:
                    if (game.getOptions().booleanOption(
                            "tacops_vehicle_effective")) {
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
                                    "vehicles_threshold")) {
                                setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                                rv = new HitData(LOC_REAR);
                            } else {
                                rv = new HitData(LOC_REAR, false,
                                        HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                            }
                        } else if (bRear) {
                            if (game.getOptions().booleanOption(
                                    "vehicles_threshold")) {
                                setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                                rv = new HitData(LOC_RIGHT);
                            } else {
                                rv = new HitData(LOC_RIGHT, false,
                                        HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                            }
                        } else {
                            if (game.getOptions().booleanOption(
                                    "vehicles_threshold")) {
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
                                "vehicles_threshold")) {
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
                                        "vehicles_threshold")) {
                                    setPotCrit(HitData.EFFECT_CRITICAL);
                                    rv = new HitData(LOC_TURRET_2);
                                } else {
                                    rv = new HitData(LOC_TURRET_2, false,
                                            HitData.EFFECT_CRITICAL);
                                }
                            } else {
                                if (game.getOptions().booleanOption(
                                        "vehicles_threshold")) {
                                    setPotCrit(HitData.EFFECT_CRITICAL);
                                    rv = new HitData(LOC_TURRET);
                                } else {
                                    rv = new HitData(LOC_TURRET, false,
                                            HitData.EFFECT_CRITICAL);
                                }
                            }
                        } else {
                            if (game.getOptions().booleanOption(
                                    "vehicles_threshold")) {
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
        return rollHitLocation(table, side, LOC_NONE,
                IAimingModes.AIM_MODE_NONE, LosEffects.COVER_NONE);
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

    /**
     * Calculates the battle value of this mech
     */
    @Override
    public int calculateBattleValue() {
        if (useManualBV) {
            return manualBV;
        }

        return calculateBattleValue(false, false);
    }

    /**
     * Calculates the battle value of this tank
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        if (useManualBV) {
            return manualBV;
        }
        if (isCarcass() && !ignorePilot) {
            return 0;
        }
        bvText = new StringBuffer(
                "<HTML><BODY><CENTER><b>Battle Value Calculations For ");

        bvText.append(getChassis());
        bvText.append(" ");
        bvText.append(getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);

        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        boolean blueShield = false;
        // a blueshield system means a +0.2 on the armor and internal modifier,
        // like for mechs
        if (hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
            blueShield = true;
        }

        bvText.append(startTable);
        double armorMultiplier = 1.0;

        for (int loc = 1; loc < locations(); loc++) {
            int modularArmor = 0;
            for (Mounted mounted : getEquipment()) {
                if ((mounted.getType() instanceof MiscType)
                        && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)
                        && (mounted.getLocation() == loc)) {
                    modularArmor += mounted.getBaseDamageCapacity()
                            - mounted.getDamageTaken();
                }
            }
            // total armor points

            switch (getArmorType(loc)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    armorMultiplier = 0.5;
                    break;
                case EquipmentType.T_ARMOR_HARDENED:
                    armorMultiplier = 2.0;
                    break;
                case EquipmentType.T_ARMOR_REACTIVE:
                case EquipmentType.T_ARMOR_REFLECTIVE:
                case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                    armorMultiplier = 1.5;
                    break;
                case EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE:
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                    armorMultiplier = 1.2;
                    break;
                default:
                    armorMultiplier = 1.0;
                    break;
            }

            if (hasWorkingMisc(MiscType.F_BLUE_SHIELD)) {
                armorMultiplier += 0.2;
            }
            bvText.append(startRow);
            bvText.append(startColumn);

            int armor = getArmor(loc) + modularArmor;
            bvText.append("Total Armor " + this.getLocationAbbr(loc) + " ("
                    + armor + ") x ");
            bvText.append(armorMultiplier);
            bvText.append(" x ");
            bvText.append(getBARRating(loc));
            bvText.append("/10");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            double armorBV = (getArmor(loc) + modularArmor) * armorMultiplier * (getBARRating(loc) / 10);
            bvText.append(armorBV);
            dbv += armorBV;
            bvText.append(endColumn);
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total modified armor BV x 2.5 ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        dbv *= 2.5;
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total I.S. Points x 1.5 x Blue Shield Multipler");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(getTotalInternal());
        bvText.append(" x 1.5 x ");
        bvText.append((blueShield ? 1.2 : 1));
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(getTotalInternal() * 1.5 * (blueShield ? 1.2 : 1));
        bvText.append(endColumn);
        bvText.append(endRow);
        // total internal structure
        dbv += getTotalInternal() * 1.5 * (blueShield ? 1.2 : 1);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Defensive Equipment");
        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        double amsAmmoBV = 0;
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();
            if ((atype.getAmmoType() == AmmoType.T_AMS) || (atype.getAmmoType() == AmmoType.T_APDS)) {
                amsAmmoBV += atype.getBV(this);
            }
        }
        double amsBV = 0;
        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && (etype
                    .hasFlag(WeaponType.F_AMS) || etype
                    .hasFlag(WeaponType.F_B_POD) || etype.hasFlag(WeaponType.F_M_POD)))) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(endRow);
                dEquipmentBV += etype.getBV(this);
                WeaponType wtype = (WeaponType) etype;
                if ((wtype.hasFlag(WeaponType.F_AMS)
                        && (wtype.getAmmoType() == AmmoType.T_AMS)) || (wtype.getAmmoType() == AmmoType.T_APDS)) {
                    amsBV += etype.getBV(this);
                }
            } else if (((etype instanceof MiscType) && (etype
                    .hasFlag(MiscType.F_ECM)
                    || etype.hasFlag(MiscType.F_AP_POD)
                    || etype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || etype.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || etype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || etype.hasFlag(MiscType.F_BULLDOZER)
                    || etype.hasFlag(MiscType.F_CHAFF_POD) || etype
                        .hasFlag(MiscType.F_BAP)))
                    || etype.hasFlag(MiscType.F_MINESWEEPER)) {
                MiscType mtype = (MiscType) etype;
                double bv = mtype.getBV(this, mounted.getLocation());
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(bv);
                dEquipmentBV += bv;
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        if (amsAmmoBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);

            bvText.append("AMS Ammo (to a maximum of AMS BV)");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+");
            bvText.append(Math.min(amsBV, amsAmmoBV));
            bvText.append(endColumn);
            bvText.append(endRow);
            dEquipmentBV += Math.min(amsBV, amsAmmoBV);
        }
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(dEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);

        dbv += dEquipmentBV;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        double typeModifier;
        switch (getMovementMode()) {
            case TRACKED:
                typeModifier = 0.9;
                break;
            case WHEELED:
                typeModifier = 0.8;
                break;
            case HOVER:
            case VTOL:
            case WIGE:
                typeModifier = 0.7;
                break;
            case NAVAL:
            case RAIL:
                typeModifier = 0.6;
                break;
            default:
                typeModifier = 0.6;
        }

        if (!(this instanceof SupportTank)
                && (hasWorkingMisc(MiscType.F_LIMITED_AMPHIBIOUS)
                        || hasWorkingMisc(MiscType.F_DUNE_BUGGY)
                        || hasWorkingMisc(MiscType.F_FLOTATION_HULL)
                        || hasWorkingMisc(MiscType.F_VACUUM_PROTECTION)
                        || hasWorkingMisc(MiscType.F_ENVIRONMENTAL_SEALING) || hasWorkingMisc(MiscType.F_ARMORED_MOTIVE_SYSTEM))) {
            typeModifier += .1;
        } else if (hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS)
                && !(this instanceof SupportTank)) {
            typeModifier += .2;
        }
        bvText.append(startColumn);
        bvText.append("x Body Type Modier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("x ");
        bvText.append(typeModifier);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);

        dbv *= typeModifier;

        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("x Target Movement modifier");
        bvText.append(endColumn);
        // adjust for target movement modifier
        double tmmRan = Compute.getTargetMovementModifier(
                getRunMP(false, true, true), this instanceof VTOL,
                this instanceof VTOL, game).getValue();
        // for the future, when we implement jumping tanks
        double tmmJumped = (getJumpMP() > 0) ? Compute.
                getTargetMovementModifier(getJumpMP(), true, false, game).
                getValue() : 0;
        if (hasStealth()) {
            tmmRan += 2;
            tmmJumped += 2;
        }
        if (getMovementMode() == EntityMovementMode.WIGE) {
            tmmRan += 1;
            tmmJumped += 1;
        }
        double tmmFactor = 1 + (Math.max(tmmRan, tmmJumped) / 10);
        dbv *= tmmFactor;

        bvText.append(startColumn);
        bvText.append("x ");
        bvText.append(tmmFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        double weaponBV = 0;

        // figure out base weapon bv
        double weaponsBVFront = 0;
        double weaponsBVRear = 0;
        boolean hasTargComp = hasTargComp();
        double targetingSystemBVMod = 1.0;

        if ((this instanceof SupportTank) || (this instanceof SupportVTOL)) {
            if (hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                targetingSystemBVMod = 1.0;
            } else if (hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)) {
                targetingSystemBVMod = .9;
            } else {
                targetingSystemBVMod = .8;
            }
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapons");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<String, Double>();
        for (Mounted mounted : getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double dBV = wtype.getBV(this);

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            if (wtype.hasFlag(WeaponType.F_B_POD)) {
                continue;
            }
            if (wtype.hasFlag(WeaponType.F_M_POD)) {
                continue;
            }
            String weaponName = wtype.getName();
            if (mounted.getLinkedBy() != null) {
                // check to see if the weapon is a PPC and has a Capacitor
                // attached to it
                if (wtype.hasFlag(WeaponType.F_PPC)) {
                    dBV += ((MiscType) mounted.getLinkedBy().getType()).getBV(
                            this, mounted);
                    weaponName = weaponName.concat(" with Capacitor");
                }
            }


            // calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgaBV = 0;
                for (Mounted possibleMG : getWeaponList()) {
                    if (possibleMG.getType().hasFlag(WeaponType.F_MG)
                            && (possibleMG.getLocation() == mounted
                                    .getLocation())) {
                        mgaBV += possibleMG.getType().getBV(this);
                    }
                }
                dBV = mgaBV * 0.67;
            }

            bvText.append(weaponName);
            bvText.append(" ");
            bvText.append(dBV);

            // artemis bumps up the value
            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                    bvText.append(" x 1.2 Artemis");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                    bvText.append(" x 1.3 Artemis V");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                    bvText.append(" x 1.15 Apollo");
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.25;
                    bvText.append(" x 1.25 RISC Laser Pulse Module");
                }
            }
            if (hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                dBV *= 0.8;
                bvText.append(" x 0.8 Drone OS");
            }


            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.25;
                bvText.append(" x 1.25 Direct Fire and TC");
            } else if ((this instanceof SupportTank)
                    && !wtype.hasFlag(WeaponType.F_INFANTRY)) {
                dBV *= targetingSystemBVMod;
                bvText.append(" x ");
                bvText.append(targetingSystemBVMod);
                bvText.append(" Targeting System");
            }
            bvText.append(endColumn);
            bvText.append(startColumn);
            if (mounted.getLocation() == (this instanceof SuperHeavyTank ? SuperHeavyTank.LOC_REAR
                    : this instanceof LargeSupportTank ? LargeSupportTank.LOC_REAR
                            : LOC_REAR)) {
                weaponsBVRear += dBV;
                bvText.append(" Rear");
            } else if (mounted.getLocation() == LOC_FRONT) {
                weaponsBVFront += dBV;
                bvText.append(" Front");
            } else {
                weaponBV += dBV;
                bvText.append(" Side/Turret");
            }
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !((wtype.getAmmoType() == AmmoType.T_PLASMA)
                    || (wtype.getAmmoType() == AmmoType.T_VEHICLE_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_HEAVY_FLAMER) || (wtype
                    .getAmmoType() == AmmoType.T_CHEMICAL_LASER)))
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype
                        .getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this)
                            + weaponsForExcessiveAmmo.get(key));
                }
            }
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(dBV);
            bvText.append(endColumn);

            bvText.append(endRow);

            bvText.append(startRow);
            bvText.append(startColumn);
        }

        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);

        if (weaponsBVFront > weaponsBVRear) {
            weaponBV += weaponsBVFront;
            weaponBV += (weaponsBVRear * 0.5);
        } else {
            weaponBV += weaponsBVRear;
            weaponBV += (weaponsBVFront * 0.5);
        }

        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(weaponBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);

        bvText.append("Ammo BV");
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on
        // our team
        double tagBV = 0;
        Map<String, Double> ammo = new HashMap<String, Double>();
        ArrayList<String> keys = new ArrayList<String>();
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getUsableShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if ((atype.getAmmoType() == AmmoType.T_AMS) || (atype.getAmmoType() == AmmoType.T_APDS)) {
                continue;
            }

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }

            bvText.append(atype.getName());
            bvText.append(endColumn);
            bvText.append(startColumn);

            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED)
                    || (atype.getMunitionType() == AmmoType.M_HOMING)) {
                IPlayer tmpP = getOwner();
                // Okay, actually check for friendly TAG.
                if (tmpP != null) {
                    if (tmpP.hasTAG()) {
                        tagBV += atype.getBV(this);
                    } else if ((tmpP.getTeam() != IPlayer.TEAM_NONE)
                            && (game != null)) {
                        for (Enumeration<Team> e = game.getTeams(); e
                                .hasMoreElements();) {
                            Team m = e.nextElement();
                            if (m.getId() == tmpP.getTeam()) {
                                if (m.hasTAG(game)) {
                                    tagBV += atype.getBV(this);
                                    bvText.append("Tag: ");
                                    bvText.append(atype.getBV(this));
                                }
                                // A player can't be on two teams.
                                // If we check his team and don't give the
                                // penalty, that's it.
                                break;
                            }
                        }
                    }
                }
            }
            String key = atype.getAmmoType() + ":" + atype.getRackSize();
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, atype.getBV(this));
            } else {
                ammo.put(key, atype.getBV(this) + ammo.get(key));
            }
            bvText.append("BV: ");
            bvText.append(atype.getBV(this));
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
        }
        bvText.append(endColumn);
        bvText.append(endRow);
        // excessive ammo rule:
        // only count BV for ammo for a weapontype until the BV of all weapons
        // of that
        // type on the mech is reached
        for (String key : keys) {
            // They dont exist in either hash then dont bother adding nulls.
            if (!ammo.containsKey(key)
                    || !weaponsForExcessiveAmmo.containsKey(key)) {
                continue;
            }
            if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                ammoBV += weaponsForExcessiveAmmo.get(key);
            } else {
                ammoBV += ammo.get(key);
            }
        }
        ammoBV *= targetingSystemBVMod;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(ammoBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        weaponBV += ammoBV;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM -
        // BMR p152)
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Offensive Equipment");
        bvText.append(endColumn);
        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        double oEquipmentBV = 0;
        for (Mounted mounted : getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if ((mtype.hasFlag(MiscType.F_ECM) && !mtype
                    .hasFlag(MiscType.F_WATCHDOG))
                    || mtype.hasFlag(MiscType.F_AP_POD)
                    || mtype.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_CHAFF_POD)
                    || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)
                    || mtype.hasFlag(MiscType.F_MINESWEEPER)) {
                continue;
            }
            double bv = mtype.getBV(this, mounted.getLocation());
            // we need to special case watchdog, because it has both offensive
            // and defensive BV
            if (mtype.hasFlag(MiscType.F_WATCHDOG)) {
                bv = 7;
            }
            oEquipmentBV += bv;
            bvText.append(mtype.getName());
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(bv);
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
        }

        bvText.append(endRow);
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(oEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        weaponBV += oEquipmentBV;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("+ weight / 2");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(getWeight());
        bvText.append(" / 2 ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(getWeight() / 2);
        bvText.append(endColumn);
        bvText.append(endRow);

        weaponBV += getWeight() / 2;

        // adjust further for speed factor
        double runMP = getRunMP(false, true, true);

        // Trains use cruise instead of flank MP for speed factor
        if (getMovementMode() == EntityMovementMode.RAIL) {
            runMP = getWalkMP(false, true, true);
        }
        // trailers have original run MP of 0, but should count at 1 for speed
        // factor calculation
        if (getOriginalRunMP() == 0) {
            runMP = 1;
        }
        double speedFactor = Math
                .pow(1 + (((runMP + (Math.round(getJumpMP(false) / 2.0))) - 5)
                 / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        obv = weaponBV * speedFactor;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("+ weapons bv * speed factor");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(weaponBV);
        bvText.append(" * ");
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(obv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("--------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        double finalBV;
        if (useGeometricMeanBV()) {
            finalBV = 2 * Math.sqrt(obv * dbv);
            if (finalBV == 0) {
                finalBV = dbv + obv;
            }
        } else {
            finalBV = dbv + obv;
        }
        double totalBV = finalBV;

        if (hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
            finalBV *= 0.95;
            finalBV = Math.round(finalBV);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total BV * Drone Operating System Modifier");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(totalBV);
            bvText.append(" * ");
            bvText.append("0.95");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(" = ");
            bvText.append(finalBV);
            bvText.append(endColumn);
            bvText.append(endRow);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);

            bvText.append("-------------");
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Final BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        if (!ignoreC3) {
            xbv += getExtraC3BV((int) Math.round(finalBV));
        }

        finalBV = Math.round(finalBV + xbv);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot && (null != getCrew())) {
            pilotFactor = getCrew().getBVSkillMultiplier(game);
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Final BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(retVal);
        bvText.append(endColumn);
        bvText.append(endRow);

        return retVal;
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
        IHex hex = game.getBoard().getHex(getPosition());
        if ((null != hex) && (getMovementMode() == EntityMovementMode.WHEELED)
                && (hex.terrainLevel(Terrains.SNOW) == 1)) {
            prd.addModifier(1, "thin snow");
        }

        // VDNI bonus?
        if (getCrew().getOptions().booleanOption("vdni")
                && !getCrew().getOptions().booleanOption("bvdni")) {
            prd.addModifier(-1, "VDNI");
        }

        if (hasModularArmor()) {
            prd.addModifier(1, "Modular Armor");
        }

        return prd;
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<Report>();

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

    /**
     * Tanks don't have MASC
     */
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        return super.getRunMP(gravity, ignoreheat, ignoremodulararmor);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getRunMP(boolean, boolean, boolean)
     */
    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        if (hasArmedMASC()) {
            return (getWalkMP(gravity, ignoreheat, ignoremodulararmor) * 2);
        }
        return getRunMPwithoutMASC(gravity, ignoreheat, ignoremodulararmor);
    }

    @Override
    public int getHeatCapacity() {
        return 999;
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
                && !(game.getOptions().booleanOption("no_hover_charge") && (EntityMovementMode.HOVER == getMovementMode()))
                && !(EntityMovementMode.WIGE == getMovementMode())
                && !(getStunnedTurns() > 0);
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

    private void addCostDetails(double cost, double[] costs) {
        bvText = new StringBuffer();
        ArrayList<String> left = new ArrayList<String>();

        if (isSupportVehicle()) {
            left.add("Chassis");
        }
        left.add("Engine");
        left.add("Armor");
        if (isSupportVehicle()) {
            left.add("Final Structural Cost");
        } else {
            left.add("Internal Structure");
            left.add("Control Systems");
        }
        left.add("Power Amplifiers");
        left.add("Heat Sinks");
        left.add("Turret");
        left.add("Equipment");
        if (!isSupportVehicle()) {
            left.add("Lift Equipment");
        }
        left.add("Omni Multiplier");
        left.add("Tonnage Multiplier");
        if (!isSupportVehicle()) {

            left.add("Flotation Hull/Vacuum Protection/Environmental Sealing multiplier");
            left.add("Off-Road Multiplier");
        }

        NumberFormat commafy = NumberFormat.getInstance();

        bvText.append("<HTML><BODY><CENTER><b>Cost Calculations For ");
        bvText.append(getChassis());
        bvText.append(" ");
        bvText.append(getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append(startTable);
        // find the maximum length of the columns.
        for (int l = 0; l < left.size(); l++) {

            if (l == 8) {
                getWeaponsAndEquipmentCost(true);
            }else {
                if (left.get(l).equals("Final Structural Cost")) {
                    bvText.append(startRow);
                    bvText.append(startColumn);
                    bvText.append(endColumn);
                    bvText.append(startColumn);
                    bvText.append("-------------");
                    bvText.append(endColumn);
                    bvText.append(endRow);
                }
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(left.get(l));
                bvText.append(endColumn);
                bvText.append(startColumn);

                if (costs[l] == 0) {
                    bvText.append("N/A");
                } else if (costs[l] < 0) {
                    bvText.append("x ");
                    bvText.append(commafy.format(-costs[l]));
                } else {
                    bvText.append(commafy.format(costs[l]));

                }
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total Cost:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(commafy.format(cost));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(endTable);
        bvText.append("</BODY></HTML>");
    }

    @Override
    public double getCost(boolean ignoreAmmo) {
        double[] costs = new double[13 + locations()];
        int i = 0;
        // Chassis cost for Support Vehicles
        if (isSupportVehicle()) {
            double chassisCost = 2500 * SupportVeeStructure.getWeightStructure(this);
            if (hasMisc(MiscType.F_AMPHIBIOUS)) {
                chassisCost *= 1.25;
            }
            if (hasMisc(MiscType.F_ARMORED_CHASSIS)) {
                chassisCost *= 2.0;
            }
            if (hasMisc(MiscType.F_BICYCLE)) {
                chassisCost *= 0.75;
            }
            if (hasMisc(MiscType.F_CONVERTIBLE)) {
                chassisCost *= 1.1;
            }
            if (hasMisc(MiscType.F_DUNE_BUGGY)) {
                chassisCost *= 1.25;
            }
            if (hasMisc(MiscType.F_ENVIRONMENTAL_SEALING)) {
                chassisCost *= 1.75;
            }
            if (hasMisc(MiscType.F_HYDROFOIL)) {
                chassisCost *= 1.1;
            }
            if (hasMisc(MiscType.F_MONOCYCLE)) {
                chassisCost *= 1.3;
            }
            if (hasMisc(MiscType.F_OFF_ROAD)) {
                chassisCost *= 1.2;
            }
            if (hasMisc(MiscType.F_PROP)) {
                chassisCost *= 0.75;
            }
            if (hasMisc(MiscType.F_SNOWMOBILE)) {
                chassisCost *= 1.3;
            }
            if (hasMisc(MiscType.F_STOL_CHASSIS)) {
                chassisCost *= 1.5;
            }
            if (hasMisc(MiscType.F_SUBMERSIBLE)) {
                chassisCost *= 3.5;
            }
            if (hasMisc(MiscType.F_TRACTOR_MODIFICATION)) {
                chassisCost *= 1.1;
            }
            if (hasMisc(MiscType.F_TRAILER_MODIFICATION)) {
                chassisCost *= 0.75;
            }
            if (hasMisc(MiscType.F_ULTRA_LIGHT)) {
                chassisCost *= 1.5;
            }
            if (hasMisc(MiscType.F_VSTOL_CHASSIS)) {
                chassisCost *= 2;
            }
            costs[i++] = chassisCost;
        }

        // Engine Costs
        double engineCost;
        if (isSupportVehicle()) {
            engineCost = 5000 * getEngine().getWeightEngine(this);
            switch (getEngine().getEngineType()) {
                case Engine.STEAM:
                    engineCost *= 0.8;
                    break;
                case Engine.COMBUSTION_ENGINE:
                    engineCost *= 1.0;
                    break;
                case Engine.BATTERY:
                    engineCost *= 1.2;
                    break;
                case Engine.FUEL_CELL:
                    engineCost *= 1.4;
                    break;
                case Engine.SOLAR:
                    engineCost *= 1.6;
                    break;
                case Engine.FISSION:
                    engineCost *= 3;
                    break;
                case Engine.NORMAL_ENGINE:
                    engineCost *= 2;
                    break;
            }
        } else {
            engineCost = (getEngine().getBaseCost() *
                    getEngine().getRating() * weight) / 75.0;
        }
        costs[i++] = engineCost;

        // armor
        if (isSupportVehicle()) {
            int totalArmorPoints = 0;
            for (int loc = 0; loc < locations(); loc++) {
                totalArmorPoints += getOArmor(loc);
            }
            costs[i++] = totalArmorPoints *
                    BAR_ARMOR_COST_MULT[getBARRating(LOC_BODY)];
        } else {
            if (hasPatchworkArmor()) {
                for (int loc = 0; loc < locations(); loc++) {
                    costs[i++] = getArmorWeight(loc)
                            * EquipmentType.getArmorCost(armorType[loc]);
                }

            } else {
                costs[i++] = getArmorWeight()
                        * EquipmentType.getArmorCost(armorType[0]);
            }
        }

        // Compute final structural cost
        int structCostIdx = 0;
        if (isSupportVehicle()) {
            structCostIdx = i++;
            costs[structCostIdx] = 0;
            for (int c = 0; c < structCostIdx; c++) {
                costs[structCostIdx] += costs[c];
            }
            double techRatingMultiplier = 0.5 + (getStructuralTechRating() * 0.25);
            costs[structCostIdx] *= techRatingMultiplier;
        } else {
            // IS has no variations, no Endo etc.
            costs[i++] = (weight / 10.0) * 10000;
            double controlWeight = Math.ceil(weight * 0.05 * 2.0) / 2.0; // ?
            // should be rounded up to nearest half-ton
            costs[i++] = 10000 * controlWeight;
        }

        double freeHeatSinks = engine.getWeightFreeEngineHeatSinks();
        int sinks = 0;
        double turretWeight = 0;
        double paWeight = 0;
        for (Mounted m : getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if (wt.hasFlag(WeaponType.F_LASER) || wt.hasFlag(WeaponType.F_PPC)) {
                sinks += wt.getHeat();
                paWeight += wt.getTonnage(this) / 10.0;
            }
            if (!hasNoTurret() && (m.getLocation() == getLocTurret())) {
                turretWeight += wt.getTonnage(this) / 10.0;
            }
            if (!hasNoDualTurret() && (m.getLocation() == getLocTurret2())) {
                turretWeight += wt.getTonnage(this) / 10.0;
            }
        }
        paWeight = Math.ceil(paWeight * 2) / 2;
        if (engine.isFusion()) {
            paWeight = 0;
        }
        turretWeight = Math.ceil(turretWeight * 2) / 2;
        costs[i++] = 20000 * paWeight;
        costs[i++] = 2000 * Math.max(0, sinks - freeHeatSinks);
        costs[i++] = turretWeight * 5000;

        costs[i++] = getWeaponsAndEquipmentCost(ignoreAmmo);

        if (!isSupportVehicle()) {
            double diveTonnage;
            switch (movementMode) {
            case HOVER:
            case HYDROFOIL:
            case VTOL:
            case SUBMARINE:
            case WIGE:
                diveTonnage = Math.ceil(weight / 5.0) / 2.0;
                break;
            default:
                diveTonnage = 0.0;
                break;
            }
            if (movementMode != EntityMovementMode.VTOL) {
                costs[i++] = diveTonnage * 20000;
            } else {
                costs[i++] = diveTonnage * 40000;
            }
        }

        double cost = 0; // calculate the total
        for (int x = structCostIdx; x < i; x++) {
            cost += costs[x];
        }
        if (isOmni()) { // Omni conversion cost goes here.
            cost *= 1.25;
            costs[i++] = -1.25;
        } else {
            costs[i++] = 0;
        }


        double multiplier = 1.0;
        switch (movementMode) {
            case HOVER:
            case SUBMARINE:
                multiplier += weight / 50.0;
                break;
            case HYDROFOIL:
                multiplier += weight / 75.0;
                break;
            case NAVAL:
            case WHEELED:
                multiplier += weight / 200.0;
                break;
            case TRACKED:
                multiplier += weight / 100.0;
                break;
            case VTOL:
                multiplier += weight / 30.0;
                break;
            case WIGE:
                multiplier += weight / 25.0;
                break;
            default:
        }
        cost *= multiplier;
        costs[i++] = -multiplier;

        if (!isSupportVehicle()) {
            if (hasWorkingMisc(MiscType.F_FLOTATION_HULL)
                    || hasWorkingMisc(MiscType.F_VACUUM_PROTECTION)
                    || hasWorkingMisc(MiscType.F_ENVIRONMENTAL_SEALING)) {
                cost *= 1.25;
                costs[i++] = -1.25;

            }
            if (hasWorkingMisc(MiscType.F_OFF_ROAD)) {
                cost *= 1.2;
                costs[i++] = -1.2;
            }
        }


        addCostDetails(cost, costs);
        return Math.round(cost);
    }

    @Override
    public boolean doomedInVacuum() {
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_VACUUM_PROTECTION)) {
                return false;
            }
            if ((m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                return false;
            }
        }
        return true;
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

    @Override
    /**
     * Checks to see if a Tank is capable of going hull-down.  This is true if
     * hull-down rules are enabled and the Tank is in a fortified hex.
     *
     *  @return True if hull-down is enabled and the Tank is in a fortified hex.
     */
    public boolean canGoHullDown() {
        // MoveStep line 2179 performs this same check
        // performing it here will allow us to disable the Hulldown button
        // if the movement is illegal
        IHex occupiedHex = game.getBoard().getHex(getPosition());
        return occupiedHex.containsTerrain(Terrains.FORTIFIED)
                && game.getOptions().booleanOption("tacops_hull_down");
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

    public void setEngine(Engine e) {
        engine = e;
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
                || (game.getOptions().booleanOption("vehicles_threshold")
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
                        if (getSensorHits() < 4) {
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
                        if (getLoadedUnits().size() > 0) {
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
                            return CRIT_ENGINE;
                        }
                    case 11:
                        for (Mounted m : getAmmo()) {
                            if (!m.isDestroyed() && !m.isHit()
                                    && (m.getLocation() != Entity.LOC_NONE)) {
                                return CRIT_AMMO;
                            }
                        }
                    case 12:
                        if (getEngine().isFusion() && !engineHit) {
                            return CRIT_ENGINE;
                        } else if (!getEngine().isFusion()) {
                            return CRIT_FUEL_TANK;
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
                        if (getLoadedUnits().size() > 0) {
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
                            return CRIT_ENGINE;
                        }
                    case 12:
                        if (getEngine().isFusion() && !engineHit) {
                            return CRIT_ENGINE;
                        } else if (!getEngine().isFusion()) {
                            return CRIT_FUEL_TANK;
                        }
                }
            }
        }
        return CRIT_NONE;
    }

    /**
     * OmniVehicles have handles for Battle Armor squads to latch onto. Please
     * note, this method should only be called during this Tank's construction.
     * <p/>
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
        Vector<Transporter> et = new Vector<Transporter>(getTransports());
        for (Transporter t : et) {
            if (t instanceof BattleArmorHandlesTank) {
                removeTransporter(t);
            }
        }
        if (game.getOptions().booleanOption("ba_grab_bars")) {
            addTransporter(new BattleArmorHandlesTank());
        } else {
            addTransporter(new ClampMountTank());
        }
    }

    /**
     * Tanks can't spot when stunned.
     */
    @Override
    public boolean canSpot() {
        return super.canSpot() && (getStunnedTurns() == 0);
    }

    public void addJammedWeapon(Mounted weapon) {
        jammedWeapons.add(weapon);
    }

    public ArrayList<Mounted> getJammedWeapons() {
        return jammedWeapons;
    }

    public void resetJammedWeapons() {
        jammedWeapons = new ArrayList<Mounted>();
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
                m.setBreached(false); // not destroyed, just
                // unpowered
            }
        }
    }

    public boolean isEngineHit() {
        return engineHit;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 1 + getExtraCommGearTons();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getIniBonus()
     */
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
        if (game.getOptions().booleanOption("tacops_vehicle_arcs")) {
            return Compute.ARC_NOSE;
        }
        return super.getForwardArc();
    }

    /**
     * see {@link Entity#getRearArc()}
     */
    @Override
    public int getRearArc() {
        if (game.getOptions().booleanOption("tacops_vehicle_arcs")) {
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
    }

    public void unlockTurret() {
        m_bTurretLocked = false;
    }

    /**
     * Determine if this unit has an active and working stealth system. (stealth
     * can be active and not working when under ECCM)
     * <p/>
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
     * <p/>
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
                    usedSlots += mount.getType().getTankslots(this);
                    addedCargo = true;
                    continue;
                } else {
                    continue;
                }
            }
            if (!((mount.getType() instanceof AmmoType) || Arrays.asList(
                    EquipmentType.armorNames).contains(
                    mount.getType().getName()))) {
                usedSlots += mount.getType().getTankslots(this);
            }
        }
        // JJs take just 1 slot
        if (this.getJumpMP(false) > 0) {
            usedSlots++;
        }
        // different engines take different amounts of slots
        if (getEngine().isFusion()) {
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
        if (getEngine().hasFlag(Engine.LARGE_ENGINE)) {
            usedSlots++;
        }
        if (getEngine().getEngineType() == Engine.COMPACT_ENGINE) {
            usedSlots--;
        }
        // for ammo, each type of ammo takes one slots, regardless of
        // submunition type
        Map<String, Boolean> foundAmmo = new HashMap<String, Boolean>();
        for (Mounted ammo : getAmmo()) {
            // don't count oneshot ammo
            if ((ammo.getLocation() == Entity.LOC_NONE)
                    && (ammo.getBaseShotsLeft() == 1)) {
                continue;
            }
            AmmoType at = (AmmoType) ammo.getType();
            if (foundAmmo.get(at.getAmmoType() + ":" + at.getRackSize()) == null) {
                usedSlots++;
                foundAmmo.put(at.getAmmoType() + ":" + at.getRackSize(), true);
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
                    usedSlots++;
                    break;
                case EquipmentType.T_ARMOR_STEALTH:
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
        if (hasArmedMASC()) {
            return getRunMPwithoutMASC() + "(" + getRunMP() + ")";
        }
        return Integer.toString(getRunMP());
    }

    /**
     * Determine the stealth modifier for firing at this unit from the given
     * range. If the value supplied for <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown.
     * <p/>
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

        boolean isInfantry = (ae instanceof Infantry)
                && !(ae instanceof BattleArmor);
        // Stealth or null sig must be active.
        if (!isStealthActive()) {
            result = new TargetRoll(0, "stealth not active");
        }
        // Determine the modifier based upon the range.
        else {
            switch (range) {
                case RangeType.RANGE_MINIMUM:
                case RangeType.RANGE_SHORT:
                    if (isStealthActive() && !isInfantry) {
                        result = new TargetRoll(0, "stealth");
                    } else {
                        // must be infantry
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_MEDIUM:
                    if (isStealthActive() && !isInfantry) {
                        result = new TargetRoll(1, "stealth");
                    } else {
                        // must be infantry
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_LONG:
                case RangeType.RANGE_EXTREME:
                case RangeType.RANGE_LOS:
                    if (isStealthActive() && !isInfantry) {
                        result = new TargetRoll(2, "stealth");
                    } else {
                        // must be infantry
                        result = new TargetRoll(0, "infantry ignore stealth");
                    }
                    break;
                case RangeType.RANGE_OUT:
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown range constant: " + range);
            }
        }

        // Return the result.
        return result;
    } // End public TargetRoll getStealthModifier( char )

    @Override
    /**
     * returns the battle force structure points for a vehicle.
     * Composite and Reinforced structures are Mech only, so we don't need to worry
     */
    public int getBattleForceStructurePoints() {
        int struct = 0;
        for (int i = 0; i < getLocationNames().length; i++) {
            struct += this.getInternal(i);
        }
        return (int) Math.ceil(struct / 10.0);
    }

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
    public boolean isCrippled(boolean checkCrew) {
        if ((getArmor(LOC_FRONT) < 1) && (getOArmor(LOC_FRONT) > 0)) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Front armor destroyed.");
            }
            return true;
        }
        if ((getArmor(LOC_RIGHT) < 1) && (getOArmor(LOC_RIGHT) > 0)) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Right armor destroyed.");
            }
            return true;
        }
        if ((getArmor(LOC_LEFT) < 1) && (getOArmor(LOC_LEFT) > 0)) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Left armor destroyed.");
            }
            return true;
        }
        if (!hasNoTurret() && ((getArmor(getLocTurret()) < 1) && (getOArmor(getLocTurret()) > 0))) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Turret destroyed.");
            }
            return true;
        }

        if (!hasNoDualTurret() && ((getArmor(getLocTurret2()) < 1) && (getOArmor(getLocTurret2()) > 0))) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Front Turret destroyed.");
            }
            return true;
        }
        if ((getArmor(LOC_REAR) < 1) && (getOArmor(LOC_REAR) > 0)) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Rear armor destroyed.");
            }
            return true;
        }

        if (isPermanentlyImmobilized(checkCrew)) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out
                        .println(getDisplayName() + " CRIPPLED: Immobilized.");
            }
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
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: has no more viable weapons.");
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isCrippled() {
        return isCrippled(true);
    }

    @Override
    public boolean isDmgHeavy() {
        if (((double) getWalkMP() / getOriginalJumpMP()) <= 0.5) {
            return true;
        }

        if ((getArmorRemainingPercent() <= 0.33) && (getArmorRemainingPercent() != IArmorState.ARMOR_NA)) {
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
        if (getWalkMP() < getOriginalWalkMP()) {
            return true;
        }

        if ((getArmorRemainingPercent() <= 0.8) && (getArmorRemainingPercent() != IArmorState.ARMOR_NA)) {
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

    public boolean isSuperHeavy() {
        return false;
    }

    /**
     * Tanks go Hull Down slightly differently, this method accounts for this
     *
     * @see megamek.common.Entity#setHullDown(boolean)
     */
    @Override
    public void setHullDown(boolean down) {
        super.setHullDown(down);
        if ((getMovedBackwards() == true) && (down == true)) {
            m_bBackedIntoHullDown = true;
        } else if ((getMovedBackwards() == false) && (down == true)) {
            m_bBackedIntoHullDown = false;
        } else if (down == false) {
            m_bBackedIntoHullDown = false;
        }
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
        return game.getOptions().booleanOption("vehicles_can_eject")
                && getCrew().isActive()
                && !hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT);
    }

    public double getBaseChassisTurretWeight() {
        return baseChassisTurretWeight;
    }

    public void setBaseChassisTurretWeight(double baseChassisTurretWeight) {
        this.baseChassisTurretWeight = baseChassisTurretWeight;
    }

    public double getBaseChassisTurret2Weight() {
        return baseChassisTurret2Weight;
    }

    public void setBaseChassisTurret2Weight(double baseChassisTurret2Weight) {
        this.baseChassisTurret2Weight = baseChassisTurret2Weight;
    }

    public boolean hasNoControlSystems() {
        return hasNoControlSystems;
    }

    public void setHasNoControlSystems(boolean hasNoControlSystems) {
        this.hasNoControlSystems = hasNoControlSystems;
    }
}
