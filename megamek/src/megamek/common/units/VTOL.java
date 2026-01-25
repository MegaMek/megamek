/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.units.Terrains.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import megamek.common.CompositeTechLevel;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.MPCalculationSetting;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.MPBoosters;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Andrew Hunter VTOLs are helicopters (more or less.)
 * @since Jun 1, 2005
 */
public class VTOL extends Tank implements IBomber {
    @Serial
    private static final long serialVersionUID = -7406911547399249173L;

    public static final int LOC_ROTOR = 5;
    public static final int LOC_TURRET = 6;
    public static final int LOC_TURRET_2 = 7;

    // VTOLs can have at most one (chin) turret, sponsons don't count and dual
    // turrets aren't allowed.
    private final static String[] LOCATION_ABBREVIATIONS = { "BD", "FR", "RS", "LS", "RR", "RO", "TU" };
    private static final String[] LOCATION_NAMES = { "Body", "Front", "Right", "Left", "Rear", "Rotor", "Turret" };

    // critical hits
    public static final int CRIT_COPILOT = 15;
    public static final int CRIT_PILOT = 16;
    public static final int CRIT_ROTOR_DAMAGE = 17;
    public static final int CRIT_ROTOR_DESTROYED = 18;
    public static final int CRIT_FLIGHT_STABILIZER = 19;

    public VTOL() {
        super();
        // need to set elevation to something different from entity
        elevation = 1;
    }

    @Override
    public int getUnitType() {
        return UnitType.VTOL;
    }

    @Override
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
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

    protected BombLoadout extBombChoices = new BombLoadout();

    private Targetable bombTarget = null;
    private final List<Coords> strafingCoords = new ArrayList<>();

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
    public boolean isLocationProhibited(Coords c, int testBoardId, int elevation) {
        if (!game.hasBoardLocation(c, testBoardId)) {
            return true;
        }

        Hex hex = game.getHex(c, testBoardId);

        if (hex.containsAnyTerrainOf(IMPASSABLE, SPACE, SKY)) {
            return true;
        }

        if (elevation < 0) {
            return true;
        }

        if ((elevation == 0) && hex.hasDepth1WaterOrDeeper() && !hex.containsTerrain(ICE) && !hasFlotationHull()) {
            return true;
        }

        if (hex.containsTerrain(BUILDING) && (elevation < hex.terrainLevel(BLDG_ELEV))) {
            return true;
        }

        if (hex.hasVegetation() && !hex.containsTerrain(ROAD) && (elevation <= hex.vegetationCeiling())) {
            return true;
        }

        if (isHidden()) {
            if (hex.containsTerrain(PAVEMENT) || hex.containsTerrain(ROAD)) {
                return true;
            }
            if ((hex.terrainLevel(BRIDGE_ELEV) == elevation) && hex.containsTerrain(BRIDGE)) {
                return true;
            }
            if (hex.containsTerrain(WATER) && (elevation == 0)) {
                return true;
            }
            return elevation > 0;
        }

        return false;
    }

    @Override
    public boolean isRepairable() {
        boolean retVal = isSalvage();
        int loc = Tank.LOC_FRONT;
        while (retVal && (loc < VTOL.LOC_ROTOR)) {
            int loc_is = this.getInternal(loc);
            loc++;
            retVal = (loc_is != IArmorState.ARMOR_DOOMED)
                  && (loc_is != IArmorState.ARMOR_DESTROYED);
        }
        return retVal;
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
                case 3, 10, 11:
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
                    if (bSide
                          && !gameOptions()
                          .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_EFFECTIVE)) {
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
    public boolean isBomber() {
        return (game != null)
              && gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VTOL_ATTACKS);
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
    public BombLoadout getIntBombChoices() {
        return new BombLoadout(); // Always empty, VTOLs don't have internal bombs
    }

    @Override
    public void setIntBombChoices(BombLoadout bc) {
        // Internal bomb choices are not supported for VTOLs
    }

    @Override
    public BombLoadout getExtBombChoices() {
        return new BombLoadout(extBombChoices);
    }

    @Override
    public void setExtBombChoices(BombLoadout bc) {
        extBombChoices = new BombLoadout(bc);
    }

    @Override
    public void clearBombChoices() {
        extBombChoices.clear();
    }

    @Override
    public int reduceMPByBombLoad(int t) {
        // Per TacOps errata v3.0, movement reduction is per bomb rather than per 5 bomb
        // points
        return Math.max(0, (t - (int) this.getBombs().stream().filter(m -> (m.getUsableShotsLeft() > 0)).count()));
    }

    @Override
    public void setUsedInternalBombs(int b) {
        // Do nothing
    }

    @Override
    public void increaseUsedInternalBombs(int b) {
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
    public void setOnFire(boolean inferno) {
        super.setOnFire(inferno);
        extinguishLocation(LOC_ROTOR);
    }

    /**
     * get the type of critical caused by a critical roll, taking account of existing damage
     *
     * @param roll          the final dice roll
     * @param loc           the hit location
     * @param damagedByFire whether or not the critical was caused by fire, which is distinct from damage for unofficial
     *                      thresholding purposes.
     *
     * @return a critical type
     */
    @Override
    public int getCriticalEffect(int roll, int loc, boolean damagedByFire) {
        if (roll > 12) {
            roll = 12;
        }
        if (roll < 6) {
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
                        for (Mounted<?> m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                  && !m.isJammed() && !m.isHit()
                                  && !m.jammedThisPhase()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 8:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted<?> m : getWeaponList()) {
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
                        for (Mounted<?> m : getWeaponList()) {
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
                        for (Mounted<?> m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                  && !m.isJammed() && !m.isHit()
                                  && !m.jammedThisPhase()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 8:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted<?> m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 9:
                        for (Mounted<?> m : getWeaponList()) {
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
                        for (Mounted<?> m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                  && !m.isJammed() && !m.isHit()
                                  && !m.jammedThisPhase()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 9:
                        return CRIT_TURRET_LOCK;
                    case 10:
                        for (Mounted<?> m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                  && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 11:
                        for (Mounted<?> m : getAmmo()) {
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
                        for (Mounted<?> m : getWeaponList()) {
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
                            for (Mounted<?> m : getWeaponList()) {
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
                        for (Mounted<?> m : getWeaponList()) {
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
                        for (Mounted<?> m : getAmmo()) {
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

        // VDNI bonus? (BVDNI does NOT get piloting bonus due to "neuro-lag" per IO pg 71)
        // When tracking neural interface hardware, require DNI cockpit mod for benefits
        if (hasActiveDNI()) {
            if (hasAbility(OptionsConstants.MD_VDNI) && !hasAbility(OptionsConstants.MD_BVDNI)) {
                prd.addModifier(-1, "VDNI");
            } else if (hasAbility(OptionsConstants.MD_BVDNI)) {
                prd.addModifier(0, "BVDNI (no piloting bonus)");
            }
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

        if (!mpCalculationSetting.ignoreCargo()) {
            mp = Math.max(0, mp - getCargoMpReduction(this));
        }

        if (!mpCalculationSetting.ignoreWeather() && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);

            if (getCrew().getOptions()
                  .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                  .equals(Crew.ENVIRONMENT_SPECIALIST_SNOW)) {
                if (conditions.getWeather().isIceStorm()) {
                    mp += 2;
                }

                if (conditions.getWeather().isLightSnowOrModerateSnowOrSnowFlurriesOrHeavySnowOrSleet()) {
                    mp += 1;
                }
            }
        }

        if (!mpCalculationSetting.ignoreModularArmor() && hasModularArmor()) {
            mp--;
        }

        if (hasWorkingMisc(MiscType.F_DUNE_BUGGY)) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreCargo()) {
            mp = reduceMPByBombLoad(mp);
        }

        if (!mpCalculationSetting.ignoreGravity()) {
            mp = applyGravityEffectsOnMP(mp);
        }

        return mp;
    }

    @Override
    public MPBoosters getMPBoosters(boolean onlyArmed) {
        for (Mounted<?> m : getEquipment()) {
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
        return isSuperHeavy() ? 1 : 0;
    }

    @Override
    public boolean isSuperHeavy() {
        return getWeight() > 30;
    }

    @Override
    public int locations() {
        return m_bHasNoTurret ? 6 : 7;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_TANK | Entity.ETYPE_VTOL;
    }

    public static TechAdvancement getChinTurretTA() {
        return new TechAdvancement(TechBase.ALL)
              .setAdvancement(DATE_PS, 3079, 3080)
              .setApproximate(false, true, false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.D)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        if (!hasNoTurret()) {
            ctl.addComponent(getChinTurretTA());
        }
    }

    @Override
    public int getSpriteDrawPriority() {
        return 8;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(2.366 + 1.177 * Math.log(getWeight())));
    }

    @Override
    public void processPickupStep(MoveStep step, Integer cargoPickupLocation, TWGameManager gameManager,
          Entity entityPickingUpTarget, EntityMovementType overallMoveType) {
        super.processPickupStep(step, cargoPickupLocation, gameManager, entityPickingUpTarget, overallMoveType);

        // VTOL rotors are destroyed if they get picked up :(
        gameManager.addReport(gameManager.applyCriticalHit(this,
              VTOL.LOC_ROTOR,
              new CriticalSlot(CriticalSlot.TYPE_SYSTEM, VTOL.CRIT_ROTOR_DESTROYED),
              false,
              0,
              false));
    }
}
