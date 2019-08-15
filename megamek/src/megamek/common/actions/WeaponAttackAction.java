/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.Board;
import megamek.common.BombType;
import megamek.common.CalledShot;
import megamek.common.Compute;
import megamek.common.ComputeECM;
import megamek.common.Coords;
import megamek.common.Crew;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.ECMInfo;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityMovementType;
import megamek.common.EquipmentType;
import megamek.common.GunEmplacement;
import megamek.common.HexTarget;
import megamek.common.IAero;
import megamek.common.IAimingModes;
import megamek.common.IBomber;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ILocationExposureStatus;
import megamek.common.INarcPod;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LandAirMech;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.MinefieldTarget;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.PlanetaryConditions;
import megamek.common.Protomech;
import megamek.common.QuadMech;
import megamek.common.QuadVee;
import megamek.common.RangeType;
import megamek.common.SpaceStation;
import megamek.common.SupportTank;
import megamek.common.SupportVTOL;
import megamek.common.TagInfo;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.TripodMech;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.DiveBombAttack;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;
import megamek.common.weapons.artillery.ArtilleryWeapon;
import megamek.common.weapons.bayweapons.LaserBayWeapon;
import megamek.common.weapons.bayweapons.PPCBayWeapon;
import megamek.common.weapons.bayweapons.PulseLaserBayWeapon;
import megamek.common.weapons.bayweapons.ScreenLauncherBayWeapon;
import megamek.common.weapons.gaussrifles.GaussWeapon;
import megamek.common.weapons.gaussrifles.ISHGaussRifle;
import megamek.common.weapons.lasers.ISBombastLaser;
import megamek.common.weapons.lasers.VariableSpeedPulseLaserWeapon;
import megamek.common.weapons.lrms.LRTWeapon;
import megamek.common.weapons.mortars.MekMortarWeapon;
import megamek.common.weapons.other.TSEMPWeapon;
import megamek.common.weapons.srms.SRTWeapon;

/**
 * Represents intention to fire a weapon at the target.
 */
public class WeaponAttackAction extends AbstractAttackAction implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -9096603813317359351L;
    
    public static final int STRATOPS_SENSOR_SHADOW_WEIGHT_DIFF = 100000;
    
    private int weaponId;
    private int ammoId = -1;
    private int aimedLocation = Entity.LOC_NONE;
    private int aimMode = IAimingModes.AIM_MODE_NONE;
    private int otherAttackInfo = -1; //
    private boolean nemesisConfused;
    private boolean swarmingMissiles;
    protected int launchVelocity = 50;
    /**
     * Keeps track of the ID of the current primary target for a swarm missile
     * attack.
     */
    private int oldTargetId = -1;

    /**
     * Keeps track of the Targetable type for the current primary target for a
     * swarm missile attack.
     */
    private int oldTargetType;

    /**
     * Keeps track of the ID of the original target for a swarm missile attack.
     */
    private int originalTargetId = Entity.NONE;

    /**
     * Keeps track of the type of the original target for a swarm missile
     * attack.
     */
    private int originalTargetType;

    private int swarmMissiles = 0;

    // bomb stuff
    private int[] bombPayload = new int[BombType.B_NUM];

    // equipment that affects this attack (AMS, ECM?, etc)
    // only used server-side
    private transient ArrayList<Mounted> vCounterEquipment;

    /**
     * Boolean flag that determines whether or not this attack is part of a
     * strafing run.
     */
    private boolean isStrafing = false;

    /**
     * Boolean flag that determines if this shot was the first one by a
     * particular weapon in a strafing run. Used to ensure that heat is only
     * added once.
     */
    protected boolean isStrafingFirstShot = false;

    /**
     * Boolean flag that determines if this shot was fired as part of a
     * pointblank shot from a hidden unit. In this case, to-hit numbers should
     * not be modified for terrain or movement. See TW pg 260
     */
    protected boolean isPointblankShot = false;
    
    /**
     * Boolean flag that determines if this shot was fired using homing ammunition.
     * Can be checked to allow casting of attack handlers to the proper homing handler.
     */
    protected boolean isHomingShot = false;
    
    /**
     * Boolean flag that determines if this shot was fired using a weapon with special to-hit handling.
     * Allows this waa to bypass all the standard to-hit modifier checks
     */
    protected static boolean specialResolution = false;

    protected static boolean isSpecialResolution() {
        return specialResolution;
    }

    protected static void setSpecialResolution(boolean specialResolution) {
        WeaponAttackAction.specialResolution = specialResolution;
    }

    // default to attacking an entity
    public WeaponAttackAction(int entityId, int targetId, int weaponId) {
        super(entityId, targetId);
        this.weaponId = weaponId;
    }

    public WeaponAttackAction(int entityId, int targetType, int targetId, int weaponId) {
        super(entityId, targetType, targetId);
        this.weaponId = weaponId;
    }

    public int getWeaponId() {
        return weaponId;
    }

    public int getAmmoId() {
        return ammoId;
    }

    public int getAimedLocation() {
        return aimedLocation;
    }

    public int getAimingMode() {
        return aimMode;
    }

    public ArrayList<Mounted> getCounterEquipment() {
        return vCounterEquipment;
    }

    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
    }

    public void setAmmoId(int ammoId) {
        this.ammoId = ammoId;
    }

    public void setAimedLocation(int aimedLocation) {
        this.aimedLocation = aimedLocation;
    }

    public void setAimingMode(int aimMode) {
        this.aimMode = aimMode;
    }

    public void addCounterEquipment(Mounted m) {
        if (vCounterEquipment == null) {
            vCounterEquipment = new ArrayList<Mounted>();
        }
        vCounterEquipment.add(m);
    }

    public void setOtherAttackInfo(int newInfo) {
        otherAttackInfo = newInfo;
    }

    public int getOtherAttackInfo() {
        return otherAttackInfo;
    }

    public boolean isAirToGround(IGame game) {
        return Compute.isAirToGround(getEntity(game), getTarget(game));
    }

    public boolean isAirToAir(IGame game) {
        return Compute.isAirToAir(getEntity(game), getTarget(game));
    }

    public boolean isGroundToAir(IGame game) {
        return Compute.isGroundToAir(getEntity(game), getTarget(game));
    }

    public boolean isDiveBomb(IGame game) {
        return ((WeaponType) getEntity(game).getEquipment(getWeaponId()).getType()).hasFlag(WeaponType.F_DIVE_BOMB);
    }

    public int getAltitudeLoss(IGame game) {
        if (isAirToGround(game)) {
            if (((WeaponType) getEntity(game).getEquipment(getWeaponId()).getType()).hasFlag(WeaponType.F_DIVE_BOMB)) {
                return 2;
            }
            if (((WeaponType) getEntity(game).getEquipment(getWeaponId()).getType()).hasFlag(WeaponType.F_ALT_BOMB)) {
                return 0;
            }
            if (isStrafing) {
                return 0;
            } else {
                return 1;
            }
        }
        return 0;
    }

    public ToHitData toHit(IGame game) {
        return WeaponAttackAction.toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()),
                getWeaponId(), getAimedLocation(), getAimingMode(), nemesisConfused, swarmingMissiles,
                game.getTarget(getOldTargetType(), getOldTargetId()),
                game.getTarget(getOriginalTargetType(), getOriginalTargetId()), isStrafing(), isPointblankShot());
    }

    public ToHitData toHit(IGame game, List<ECMInfo> allECMInfo) {
        return WeaponAttackAction.toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()),
                getWeaponId(), getAimedLocation(), getAimingMode(), nemesisConfused, swarmingMissiles,
                game.getTarget(getOldTargetType(), getOldTargetId()),
                game.getTarget(getOriginalTargetType(), getOriginalTargetId()), isStrafing(), isPointblankShot(),
                allECMInfo);
    }

    public static ToHitData toHit(IGame game, int attackerId, Targetable target, int weaponId, boolean isStrafing) {
        return WeaponAttackAction.toHit(game, attackerId, target, weaponId, Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE,
                false, false, null, null, isStrafing, false);
    }

    public static ToHitData toHit(IGame game, int attackerId, Targetable target, int weaponId, int aimingAt,
            int aimingMode, boolean isStrafing) {
        return WeaponAttackAction.toHit(game, attackerId, target, weaponId, aimingAt, aimingMode, false, false, null,
                null, isStrafing, false);
    }

    public static ToHitData toHit(IGame game, int attackerId, Targetable target, int weaponId, int aimingAt,
            int aimingMode, boolean isNemesisConfused, boolean exchangeSwarmTarget, Targetable oldTarget,
            Targetable originalTarget, boolean isStrafing, boolean isPointblankShot) {
        return WeaponAttackAction.toHit(game, attackerId, target, weaponId, aimingAt, aimingMode, isNemesisConfused,
                exchangeSwarmTarget, oldTarget, originalTarget, isStrafing, isPointblankShot, null);
    }

    /**
     * To-hit number for attacker firing a weapon at the target.
     */
    private static ToHitData toHit(IGame game, int attackerId, Targetable target, int weaponId, int aimingAt,
            int aimingMode, boolean isNemesisConfused, boolean exchangeSwarmTarget, Targetable oldTarget,
            Targetable originalTarget, boolean isStrafing, boolean isPointblankShot, List<ECMInfo> allECMInfo) {
        final Entity ae = game.getEntity(attackerId);
        final Mounted weapon = ae.getEquipment(weaponId);
        
        final WeaponType wtype = (WeaponType) weapon.getType();
        
        // This is ok to keep here. No need to process anything further if we're not using a weapon somehow
        if (!(wtype instanceof WeaponType)) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, Messages.getString("WeaponAttackAction.NotAWeapon"));
        }

        Targetable swarmSecondaryTarget = target;
        Targetable swarmPrimaryTarget = oldTarget;
        if (exchangeSwarmTarget) {
            // this is a swarm attack against a new target
            // first, exchange original and new targets to get all mods
            // as if firing against original target.
            // at the end of this function, we remove target terrain
            // and movement mods, and add those for the new target
            Targetable tempTarget = target;
            target = originalTarget;
            originalTarget = tempTarget;
        }
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        boolean isAttackerInfantry = ae instanceof Infantry;
        
        boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
        
        boolean isWeaponFieldGuns = isAttackerInfantry && (weapon.getLocation() == Infantry.LOC_FIELD_GUNS);
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        // 2002-09-16 Infantry weapons have unlimited ammo.
        
        final boolean usesAmmo = (wtype.getAmmoType() != AmmoType.T_NA) && !isWeaponInfantry;
        
        final Mounted ammo = usesAmmo ? weapon.getLinked() : null;
        
        final AmmoType atype = ammo == null ? null : (AmmoType) ammo.getType();
        
        long munition = AmmoType.M_STANDARD;
        if (atype != null) {
            munition = atype.getMunitionType();
        }
        
        final boolean targetInBuilding = Compute.isInBuilding(game, te);
        
        boolean bMekTankStealthActive = false;
        if ((ae instanceof Mech) || (ae instanceof Tank)) {
            bMekTankStealthActive = ae.isStealthActive();
        }
        boolean isIndirect = (wtype.hasModes() && weapon.curMode().equals("Indirect"));
        
        boolean isInferno = ((atype != null)
                && ((atype.getAmmoType() == AmmoType.T_SRM)
                        || (atype.getAmmoType() == AmmoType.T_SRM_IMP)
                        || (atype.getAmmoType() == AmmoType.T_MML))
                && (atype.getMunitionType() == AmmoType.M_INFERNO))
                || (isWeaponInfantry && (wtype.hasFlag(WeaponType.F_INFERNO)));
        
        boolean isArtilleryDirect = wtype.hasFlag(WeaponType.F_ARTILLERY)
                && (game.getPhase() == IGame.Phase.PHASE_FIRING);
        
        boolean isArtilleryIndirect = wtype.hasFlag(WeaponType.F_ARTILLERY) 
                && ((game.getPhase() == IGame.Phase.PHASE_TARGETING)
                        || (game.getPhase() == IGame.Phase.PHASE_OFFBOARD));
        
        boolean isBearingsOnlyMissile = (weapon.isInBearingsOnlyMode())
                            && ((game.getPhase() == IGame.Phase.PHASE_TARGETING)
                                    || (game.getPhase() == IGame.Phase.PHASE_FIRING));
        
        boolean isCruiseMissile = weapon.getType().hasFlag(WeaponType.F_CRUISE_MISSILE);
        
        // hack, otherwise when actually resolves shot labeled impossible.
        boolean isArtilleryFLAK = isArtilleryDirect && (te != null)
                && ((((te.getMovementMode() == EntityMovementMode.VTOL)
                        || (te.getMovementMode() == EntityMovementMode.WIGE)) && te.isAirborneVTOLorWIGE())
                        || (te.isAirborne()))
                && (atype != null) && (usesAmmo && (atype.getMunitionType() == AmmoType.M_STANDARD));
        
        boolean isHaywireINarced = ae.isINarcedWith(INarcPod.HAYWIRE);
        
        boolean isINarcGuided = false;
        
        // for attacks where ECM along flight path makes a difference
        boolean isECMAffected = ComputeECM.isAffectedByECM(ae, ae.getPosition(), target.getPosition(), allECMInfo);
        
        // for attacks where only ECM on the target hex makes a difference
        boolean isTargetECMAffected = ComputeECM.isAffectedByECM(ae, target.getPosition(), target.getPosition(),
                allECMInfo);
        
        boolean isTAG = wtype.hasFlag(WeaponType.F_TAG);
        
        // target type checked later because its different for
        // direct/indirect (BMRr p77 on board arrow IV)
        boolean isHoming = (munition == AmmoType.M_HOMING && ammo.curMode().equals("Homing"));

        boolean bHeatSeeking = (atype != null)
                && ((atype.getAmmoType() == AmmoType.T_SRM)
                        || (atype.getAmmoType() == AmmoType.T_SRM_IMP)
                        || (atype.getAmmoType() == AmmoType.T_MML)
                        || (atype.getAmmoType() == AmmoType.T_LRM)
                        || (atype.getAmmoType() == AmmoType.T_LRM_IMP))
                && (munition == AmmoType.M_HEAT_SEEKING);
        
        boolean bFTL = (atype != null)
                && ((atype.getAmmoType() == AmmoType.T_MML) 
                        || (atype.getAmmoType() == AmmoType.T_LRM)
                        || (atype.getAmmoType() == AmmoType.T_LRM_IMP))
                && (munition == AmmoType.M_FOLLOW_THE_LEADER);

        Mounted mLinker = weapon.getLinkedBy();
               
        boolean bApollo = ((mLinker != null) && (mLinker.getType() instanceof MiscType) && !mLinker.isDestroyed()
                && !mLinker.isMissing() && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_APOLLO))
                && (atype != null) && (atype.getAmmoType() == AmmoType.T_MRM);
        
        boolean bArtemisV = ((mLinker != null) && (mLinker.getType() instanceof MiscType) && !mLinker.isDestroyed()
                && !mLinker.isMissing() && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)
                && !isECMAffected && !bMekTankStealthActive && (atype != null)
                && (munition == AmmoType.M_ARTEMIS_V_CAPABLE));
        
        if (ae.usesWeaponBays()) {
            for (int wId : weapon.getBayWeapons()) {
                Mounted bayW = ae.getEquipment(wId);
                Mounted bayWAmmo = bayW.getLinked();

                if (bayWAmmo == null) {
                    //At present, all weapons below using mLinker use ammo, so this won't be a problem
                    continue;
                }
                AmmoType bAmmo = (AmmoType) bayWAmmo.getType();
                
                //If we're using optional rules and firing Arrow Homing missiles from a bay...
                isHoming = bAmmo != null && bAmmo.getMunitionType() == AmmoType.M_HOMING;
                
                //If the artillery bay is firing cruise missiles, they have some special rules
                //It is possible to combine cruise missiles and other artillery in a bay, so
                //set this to true if any of the weapons are cruise missile launchers.
                if (bayW.getType().hasFlag(WeaponType.F_CRUISE_MISSILE)) {
                    isCruiseMissile = true;
                }

                mLinker = bayW.getLinkedBy();
                bApollo = ((mLinker != null) && (mLinker.getType() instanceof MiscType) && !mLinker.isDestroyed()
                        && !mLinker.isMissing() && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_APOLLO))
                        && (bAmmo != null) && (bAmmo.getAmmoType() == AmmoType.T_MRM);
                
                bArtemisV = ((mLinker != null) && (mLinker.getType() instanceof MiscType) && !mLinker.isDestroyed()
                        && !mLinker.isMissing() && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)
                        && !isECMAffected && !bMekTankStealthActive && (atype != null)
                        && (bAmmo != null) && (bAmmo.getMunitionType() == AmmoType.M_ARTEMIS_V_CAPABLE));
            }
        }
        
        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, te);
        
        //Set up the target's relative elevation/depth
        int targEl;

        if (te == null) {
            targEl = -game.getBoard().getHex(target.getPosition()).depth();
        } else {
            targEl = te.relHeight();
        }

        // is this attack originating from underwater
        // TODO: assuming that torpedoes are underwater attacks even if fired
        // from surface vessel, awaiting rules clarification
        // http://www.classicbattletech.com/forums/index.php/topic,48744.0.html
        boolean underWater = (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET)
                || (wtype instanceof SRTWeapon) || (wtype instanceof LRTWeapon);

        if (te != null) {
            if (!isTargetECMAffected && te.isINarcedBy(ae.getOwner().getTeam()) && (atype != null)
                    && ((atype.getAmmoType() == AmmoType.T_LRM)
                            || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                            || (atype.getAmmoType() == AmmoType.T_MML)
                            || (atype.getAmmoType() == AmmoType.T_SRM)
                            || (atype.getAmmoType() == AmmoType.T_SRM_IMP) 
                            || (atype.getAmmoType() == AmmoType.T_NLRM))
                    && (munition == AmmoType.M_NARC_CAPABLE)) {
                isINarcGuided = true;
            }
        }
        int toSubtract = 0;
        
        //Convenience variable to test the targetable type value
        final int ttype = target.getTargetType();
      
        // if we're doing indirect fire, find a spotter
        Entity spotter = null;
        boolean narcSpotter = false;
        if (isIndirect && !ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)) {
            if ((target instanceof Entity) && !isTargetECMAffected && (te != null) && (atype != null) && usesAmmo
                    && (munition == AmmoType.M_NARC_CAPABLE)
                    && (te.isNarcedBy(ae.getOwner().getTeam()) || te.isINarcedBy(ae.getOwner().getTeam()))) {
                spotter = te;
                narcSpotter = true;
            } else {
                spotter = Compute.findSpotter(game, ae, target);
            }
            if ((spotter == null) && (atype != null)
                    && ((atype.getAmmoType() == AmmoType.T_LRM)
                            || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                            || (atype.getAmmoType() == AmmoType.T_MML)
                            || (atype.getAmmoType() == AmmoType.T_NLRM)
                            || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                    && (munition == AmmoType.M_SEMIGUIDED)) {
                for (TagInfo ti : game.getTagInfo()) {
                    if (target.getTargetId() == ti.target.getTargetId()) {
                        spotter = game.getEntity(ti.attackerId);
                    }
                }
            }
        }

        // EI system
        // 0 if no EI (or switched off)
        // 1 if no intervening light woods
        // 2 if intervening light woods (because target in woods + intervening
        // woods is only +1 total)
        int eistatus = 0;

        boolean mpMelevationHack = false;
        if (usesAmmo 
                && ((wtype.getAmmoType() == AmmoType.T_LRM) || (wtype.getAmmoType() == AmmoType.T_LRM_IMP))  
                && (atype != null)
                && (munition == AmmoType.M_MULTI_PURPOSE) 
                && (ae.getElevation() == -1)
                && (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET)) {
            mpMelevationHack = true;
            // surface to fire
            ae.setElevation(0);
        }
        
        // check LOS (indirect LOS is from the spotter)
        LosEffects los;
        ToHitData losMods;
        if (isIndirect && ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)
                && !underWater) {
            los = new LosEffects();
            losMods = new ToHitData();
        } else if (!isIndirect || (spotter == null)) {
            if (!exchangeSwarmTarget) {
                los = LosEffects.calculateLos(game, attackerId, target);
            } else {
                // Swarm should draw LoS between targets, not attacker, since
                // we don't want LoS to be blocked
                if (swarmPrimaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                    los = LosEffects.calculateLos(game, swarmPrimaryTarget.getTargetId(), swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLos(game, swarmSecondaryTarget.getTargetId(), swarmPrimaryTarget);
                }
            }

            if (ae.hasActiveEiCockpit()) {
                if (los.getLightWoods() > 0) {
                    eistatus = 2;
                } else {
                    eistatus = 1;
                }
            }

            if ((wtype instanceof MekMortarWeapon) && isIndirect) {
                los.setArcedAttack(true);
            }

            losMods = los.losModifiers(game, eistatus, underWater);
        } else {
            if (exchangeSwarmTarget) {
                // Swarm should draw LoS between targets, not attacker, since
                // we don't want LoS to be blocked
                if (swarmPrimaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                    los = LosEffects.calculateLos(game, swarmPrimaryTarget.getTargetId(), swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLos(game, swarmSecondaryTarget.getTargetId(), swarmPrimaryTarget);
                }
            } else {
                //For everything else, set up a plain old LOS
                los = LosEffects.calculateLos(game, spotter.getId(), target, true);
            }

            // do not count attacker partial cover in indirect fire
            los.setAttackerCover(LosEffects.COVER_NONE);

            if (!narcSpotter && spotter.hasActiveEiCockpit()) {
                if (los.getLightWoods() > 0) {
                    eistatus = 2;
                } else {
                    eistatus = 1;
                }
            }

            if (wtype instanceof MekMortarWeapon) {
                los.setArcedAttack(true);
            }

            losMods = los.losModifiers(game, underWater);
        }
        if (mpMelevationHack) {
            // return to depth 1
            ae.setElevation(-1);
        }
        
        //Set up our initial toHit data
        ToHitData toHit = new ToHitData();
        
        //Check to see if this attack is impossible and return the reason code
        String reasonImpossible = WeaponAttackAction.toHitIsImpossible(game, ae, te, target, swarmPrimaryTarget, swarmSecondaryTarget,
                weapon, ammo, atype, wtype, ttype, los, exchangeSwarmTarget, usesAmmo, isTAG, isInferno, isAttackerInfantry,
                isIndirect, attackerId, weaponId, isArtilleryIndirect, isArtilleryFLAK, targetInBuilding,
                isArtilleryDirect, isTargetECMAffected, isStrafing, isBearingsOnlyMissile, isCruiseMissile);
        if (reasonImpossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, reasonImpossible);
        }
        
        //Check to see if this attack is automatically successful and return the reason code
        String reasonAutoHit = WeaponAttackAction.toHitIsAutomatic(game, ae, te, target, swarmPrimaryTarget, swarmSecondaryTarget,
                weapon, ammo, atype, wtype, ttype, los, usesAmmo, exchangeSwarmTarget, isTAG, isInferno, isAttackerInfantry,
                isIndirect, attackerId, weaponId, isArtilleryIndirect, isArtilleryFLAK, targetInBuilding,
                isArtilleryDirect, isTargetECMAffected, isStrafing, isBearingsOnlyMissile, isCruiseMissile);
        if (reasonAutoHit != null) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, reasonAutoHit);
        }
        
        //Is this an infantry leg/swarm attack?
        toHit = handleSwarmAttacks(game, ae, target, ttype, toHit, wtype);
        if (isSpecialResolution()) {
            return toHit;
        }
        
        //Artillery attack?
        toHit = handleArtilleryAttacks(game, ae, target, ttype, losMods, toHit, wtype, weapon, atype, isArtilleryDirect,
                isArtilleryFLAK, isHoming, isArtilleryIndirect, usesAmmo);
        if (isSpecialResolution()) {
            return toHit;
        }
        
        //Check to see if this attack was made with a weapon that has special to-hit rules
        toHit = handleSpecialWeaponAttacks(game, ae, target, ttype, los, toHit, wtype, atype);
        if (isSpecialResolution()) {
            return toHit;
        }
        
        //This attack has now tested possible and doesn't follow any weird special rules,
        //so let's start adding up the to-hit numbers
        
        //Start with the attacker's weapon skill
        toHit = new ToHitData(ae.getCrew().getGunnery(), Messages.getString("WeaponAttackAction.GunSkill"));
        if (game.getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
            if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                toHit = new ToHitData(ae.getCrew().getGunneryL(), Messages.getString("WeaponAttackAction.GunESkill"));
            }
            if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                toHit = new ToHitData(ae.getCrew().getGunneryM(), Messages.getString("WeaponAttackAction.GunMSkill"));
            }
            if (wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                toHit = new ToHitData(ae.getCrew().getGunneryB(), Messages.getString("WeaponAttackAction.GunBSkill"));
            }
        }
        if (wtype.hasFlag(WeaponType.F_ARTILLERY) && game.getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
            toHit = new ToHitData(ae.getCrew().getArtillery(), Messages.getString("WeaponAttackAction.ArtySkill"));
        }
        
        //Mine launchers have their own base to-hit, but can still be affected by terrain and movement modifiers
        //thus, they don't qualify for special weapon handling
        if (BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName())) {
            toHit = new ToHitData(8, Messages.getString("WeaponAttackAction.MagMine"));
        }

        // B-Pod firing at infantry in the same hex autohit
        if (wtype.hasFlag(WeaponType.F_B_POD) && (target instanceof Infantry)
                && target.getPosition().equals(ae.getPosition())) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.BPodAtInf"));
        }

        // TODO: mech making DFA could be higher if DFA target hex is higher
        // BMRr pg. 43, "attacking unit is considered to be in the air
        // above the hex, standing on an elevation 1 level higher than
        // the target hex or the elevation of the hex the attacker is
        // in, whichever is higher."
        
        // Store the thruBldg state, for later processing
        toHit.setThruBldg(los.getThruBldg());
        
        // Collect the modifiers for the environment
        toHit = compileEnvironmentalToHitMods(game, ae, target, wtype, atype, toHit, isArtilleryIndirect);
        
        // Collect the modifiers for the crew/pilot
        toHit = compileCrewToHitMods(game, ae, te, toHit, wtype);
        
        // Collect the modifiers for the attacker's condition/actions
        if (ae != null) {
            //Conventional fighter, Aerospace and fighter LAM attackers
            if (ae instanceof IAero) {
                toHit = compileAeroAttackerToHitMods(game, ae, target, toHit, wtype, weapon, munition, isArtilleryIndirect);
            //Everyone else
            } else {
                toHit = compileAttackerToHitMods(game, ae, target, toHit, wtype, weapon, atype, munition,
                        isArtilleryDirect, isArtilleryIndirect, isIndirect, usesAmmo);
            }
        }

        // determine some more variables
        int aElev = ae.getElevation();
        int tElev = target.getElevation();
        int aAlt = ae.getAltitude();
        int tAlt = target.getAltitude();
        int distance = Compute.effectiveDistance(game, ae, target);
        
        if (isBearingsOnlyMissile) {
            if (game.getPhase() == IGame.Phase.PHASE_TARGETING && distance >= RangeType.RANGE_BEARINGS_ONLY_MINIMUM) {
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.BoMissileHex"));
            }
            if (game.getPhase() == IGame.Phase.PHASE_TARGETING && distance < RangeType.RANGE_BEARINGS_ONLY_MINIMUM) {
                return new ToHitData(TargetRoll.AUTOMATIC_FAIL, Messages.getString("WeaponAttackAction.BoMissileMinRange"));
            } 
        }

        // Attacks against adjacent buildings automatically hit.
        if ((distance == 1) && ((ttype == Targetable.TYPE_BUILDING)
                || (ttype == Targetable.TYPE_BLDG_IGNITE)
                || (ttype == Targetable.TYPE_FUEL_TANK)
                || (ttype == Targetable.TYPE_FUEL_TANK_IGNITE)
                || (target instanceof GunEmplacement))) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.AdjBuilding"));
        }

        // Attacks against buildings from inside automatically hit.
        if ((null != los.getThruBldg()) && ((ttype == Targetable.TYPE_BUILDING)
                || (ttype == Targetable.TYPE_BLDG_IGNITE)
                || (ttype == Targetable.TYPE_FUEL_TANK)
                || (ttype == Targetable.TYPE_FUEL_TANK_IGNITE)
                || (target instanceof GunEmplacement))) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.InsideBuilding"));
        }
        
        placeholder

        // air-to-ground strikes apply a +2 mod
        if (Compute.isAirToGround(ae, target)
                || (ae.isMakingVTOLGroundAttack())) {
            if (wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                toHit.addModifier(ae.getAltitude(), Messages.getString("WeaponAttackAction.BombAltitude"));
                if (ae.hasAbility(OptionsConstants.GUNNERY_GOLDEN_GOOSE)) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GoldenGoose"));
                }
            } else if (isStrafing) {
                toHit.addModifier(+4, Messages.getString("WeaponAttackAction.Strafing"));
                if (ae.getAltitude() == 1) {
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.StrafingNoe"));
                }
                // Additional Nape-of-Earth restrictions for strafing
                if (ae.getAltitude() == 1) {
                    Coords prevCoords = ae.passedThroughPrevious(target.getPosition());
                    IHex prevHex = game.getBoard().getHex(prevCoords);
                    toHit.append(Compute.getStrafingTerrainModifier(game, eistatus, prevHex));
                }
            } else {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AtgStrike"));
                if (ae.hasAbility(OptionsConstants.GUNNERY_GOLDEN_GOOSE)) {
                    if (wtype.hasFlag(WeaponType.F_DIVE_BOMB)) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GoldenGoose"));
                    } else {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GoldenGoose"));
                    }
                }
            }
        }

        // units making air to ground attacks are easier to hit by air-to-air
        // attacks
        if ((null != te) && Compute.isAirToAir(ae, target)) {
            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if ((prevAttack.getEntityId() == te.getId()) && prevAttack.isAirToGround(game)) {
                    toHit.addModifier(-3, Messages.getString("WeaponAttackAction.TeGroundAttack"));
                    break;
                }
            }
        }

        // units with the narrow/low profile quirk are harder to hit
        if ((te != null) && te.hasQuirk(OptionsConstants.QUIRK_POS_LOW_PROFILE)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.LowProfile"));
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (!isAttackerInfantry && (te != null) && (te instanceof BattleArmor)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.BaTarget"));
        }

        // infantry squads are also hard to hit
        if ((te instanceof Infantry) && !(te instanceof BattleArmor) && ((Infantry) te).isSquad()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.SquadTarget"));
        }

        // Ejected MechWarriors are harder to hit
        if ((te != null) && (te instanceof MechWarrior)) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.MwTarget"));
        }

        // Indirect fire has a +1 mod
        if (isIndirect) {
            if (ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)) {
                toHit.addModifier(0, Messages.getString("WeaponAttackAction.Indirect"));
            } else {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.Indirect"));
            }
        }

        if (wtype instanceof MekMortarWeapon) {
            if (isIndirect) {
                if (spotter == null) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.NoSpotter"));
                }
            } else {
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.DirectMortar"));
            }
        }

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement - ignore for pointblank shots from hidden units
        if ((te != null) && !isPointblankShot) {
            ToHitData thTemp = Compute.getTargetMovementModifier(game, target.getTargetId());
            toHit.append(thTemp);
            toSubtract += thTemp.getValue();

            // semiguided ammo negates this modifier, if TAG succeeded
            if ((atype != null) && ((atype.getAmmoType() == AmmoType.T_LRM) 
                    || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_MML)
                    || (atype.getAmmoType() == AmmoType.T_NLRM) 
                    || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                    && (munition == AmmoType.M_SEMIGUIDED) && (te.getTaggedBy() != -1)) {
                int nAdjust = thTemp.getValue();
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, Messages.getString("WeaponAttackAction.SemiGuidedTag")));
                }
            }
            // precision ammo reduces this modifier
            else if ((atype != null)
                    && ((atype.getAmmoType() == AmmoType.T_AC) 
                            || (atype.getAmmoType() == AmmoType.T_LAC)
                            || (atype.getAmmoType() == AmmoType.T_AC_IMP)
                            || (atype.getAmmoType() == AmmoType.T_PAC))
                    && (munition == AmmoType.M_PRECISION)) {
                int nAdjust = Math.min(2, thTemp.getValue());
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, Messages.getString("WeaponAttackAction.Precision")));
                }
            }
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_KIND_RAPID_AC) 
                && weapon.curMode().equals("Rapid")) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AcRapid"));
        }

        // Armor Piercing ammo is a flat +1
        if ((atype != null)
                && ((atype.getAmmoType() == AmmoType.T_AC) 
                        || (atype.getAmmoType() == AmmoType.T_LAC)
                        || (atype.getAmmoType() == AmmoType.T_AC_IMP)
                        || (atype.getAmmoType() == AmmoType.T_PAC))
                && (munition == AmmoType.M_ARMOR_PIERCING)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.ApAmmo"));
        }

        // spotter movement, if applicable
        if (isIndirect) {
            // semiguided ammo negates this modifier, if TAG succeeded
            if ((atype != null) && ((atype.getAmmoType() == AmmoType.T_LRM)
                    || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_MML)
                    || (atype.getAmmoType() == AmmoType.T_NLRM) 
                    || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                    && (munition == AmmoType.M_SEMIGUIDED)) {

                if (Compute.isTargetTagged(target, game)) {
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SemiGuidedIndirect"));
                }
            } else if (!narcSpotter && (spotter != null)) {
                toHit.append(Compute.getSpotterMovementModifier(game, spotter.getId()));
                if (spotter.isAttackingThisTurn() && !spotter.getCrew().hasActiveCommandConsole() && 
                        !Compute.isTargetTagged(target, game)) {
                    toHit.addModifier(1, Messages.getString("WeaponAttackAction.SpotterAttacking"));
                }
            }
        }

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain, not applicable when delivering minefields or bombs
        // also not applicable in pointblank shots from hidden units
        if ((ttype != Targetable.TYPE_MINEFIELD_DELIVER) && !isPointblankShot) {
            toHit.append(Compute.getTargetTerrainModifier(game, target, eistatus, inSameBuilding, underWater));
            toSubtract += Compute.getTargetTerrainModifier(game, target, eistatus, inSameBuilding, underWater)
                    .getValue();
        }

        // target in water?
        IHex targHex = game.getBoard().getHex(target.getPosition());
        int partialWaterLevel = 1;
        if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
            partialWaterLevel = 2;
        }
        if ((te != null) && targHex.containsTerrain(Terrains.WATER)
                && (targHex.terrainLevel(Terrains.WATER) == partialWaterLevel) && (targEl == 0) && (te.height() > 0)) { // target
                                                                                                                        // in
                                                                                                                        // partial
                                                                                                                        // water
            los.setTargetCover(los.getTargetCover() | LosEffects.COVER_HORIZONTAL);
            losMods = los.losModifiers(game, eistatus, underWater);
        }

        if ((target instanceof Infantry) && !wtype.hasFlag(WeaponType.F_FLAMER)) {
            if (targHex.containsTerrain(Terrains.FORTIFIED)
                    || (((Infantry) target).getDugIn() == Infantry.DUG_IN_COMPLETE)) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.DugInInf"));
            }
        }

        // add in LOS mods that we've been keeping
        toHit.append(losMods);

        if ((te != null) && te.isHullDown()) {
            if ((te instanceof Mech) && !(te instanceof QuadVee && te.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)
                    && (los.getTargetCover() > LosEffects.COVER_NONE)) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.HullDown"));
            }
            // tanks going Hull Down is different rules then 'Mechs, the
            // direction the attack comes from matters
            else if ((te instanceof Tank || (te instanceof QuadVee && te.getConversionMode() == QuadVee.CONV_MODE_VEHICLE))
                    && targHex.containsTerrain(Terrains.FORTIFIED)) {
                // TODO make this a LoS mod so that attacks will come in from
                // directions that grant Hull Down Mods
                int moveInDirection;

                if (!((Tank) te).isBackedIntoHullDown()) {
                    moveInDirection = ToHitData.SIDE_FRONT;
                } else {
                    moveInDirection = ToHitData.SIDE_REAR;
                }

                if ((te.sideTable(ae.getPosition()) == moveInDirection)
                        || (te.sideTable(ae.getPosition()) == ToHitData.SIDE_LEFT)
                        || (te.sideTable(ae.getPosition()) == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.HullDown"));
                }
            }
        }

        // secondary targets modifier,
        // if this is not a iNarc Nemesis confused attack
        // Inf field guns don't get secondary target mods, TO pg 311
        if (!isNemesisConfused && !wtype.hasFlag(WeaponType.F_ALT_BOMB) && !isWeaponFieldGuns && !isStrafing) {
            toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
        }

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), Messages.getString("WeaponAttackAction.Heat"));
        }

        // actuator & sensor damage to attacker
        toHit.append(Compute.getDamageWeaponMods(ae, weapon));

        // target immobile
        boolean mekMortarMunitionsIgnoreImmobile = weapon.getType().hasFlag(WeaponType.F_MEK_MORTAR) && (atype != null)
                && (munition == AmmoType.M_AIRBURST);
        if (!(wtype instanceof ArtilleryCannonWeapon) && !mekMortarMunitionsIgnoreImmobile) {
            ToHitData immobileMod = Compute.getImmobileMod(target, aimingAt, aimingMode);
            // grounded dropships are treated as immobile as well for purpose of
            // the mods
            if ((null != te) && !te.isAirborne() && !te.isSpaceborne() && (te instanceof Dropship)
                    && ((Aero) te).isSpheroid()) {
                immobileMod = new ToHitData(-4, Messages.getString("WeaponAttackAction.ImmobileDs"));
            }
            if (immobileMod != null) {
                toHit.append(immobileMod);
                toSubtract += immobileMod.getValue();
            }
        }

        // attacker prone
        toHit.append(Compute.getProneMods(game, ae, weaponId));

        // target prone
        ToHitData proneMod = null;
        if ((te != null) && te.isProne()) {
            // easier when point-blank
            if (distance <= 1) {
                // TW, pg. 221: Swarm Mek attacks apply prone/immobile mods as
                // normal.
                proneMod = new ToHitData(-2, Messages.getString("WeaponAttackAction.ProneAdj"));
            } else {
                // Harder at range.
                proneMod = new ToHitData(1, Messages.getString("WeaponAttackAction.ProneRange"));
            }
        }
        if (proneMod != null) {
            toHit.append(proneMod);
            toSubtract += proneMod.getValue();
        }

        // weapon to-hit modifier
        if (wtype instanceof VariableSpeedPulseLaserWeapon) {
            int nRange = ae.getPosition().distance(target.getPosition());
            int[] nRanges = wtype.getRanges(weapon);
            int modifier = wtype.getToHitModifier();

            if (nRange <= nRanges[RangeType.RANGE_SHORT]) {
                modifier += RangeType.RANGE_SHORT;
            } else if (nRange <= nRanges[RangeType.RANGE_MEDIUM]) {
                modifier += RangeType.RANGE_MEDIUM;
            } else if (nRange <= nRanges[RangeType.RANGE_LONG]) {
                modifier += RangeType.RANGE_LONG;
            } else {
                modifier = 0;
            }

            toHit.addModifier(modifier, Messages.getString("WeaponAttackAction.WeaponMod"));
        } else if (wtype instanceof ISBombastLaser) {
            double damage = Compute.dialDownDamage(weapon, wtype);
            damage = Math.ceil((damage - 7) / 2);

            if (damage > 0) {
                toHit.addModifier((int) damage, Messages.getString("WeaponAttackAction.WeaponMod"));
            }
        } else if (wtype.getToHitModifier() != 0) {
            toHit.addModifier(wtype.getToHitModifier(), Messages.getString("WeaponAttackAction.WeaponMod"));
        }

        // Check whether we're eligible for a flak bonus...
        boolean isFlakAttack = !game.getBoard().inSpace() && (te != null)
                && (te.isAirborne() || te.isAirborneVTOLorWIGE()) && (atype != null)
                && ((((atype.getAmmoType() == AmmoType.T_AC_LBX) || (atype.getAmmoType() == AmmoType.T_AC_LBX_THB)
                        || (atype.getAmmoType() == AmmoType.T_SBGAUSS))
                        && (munition == AmmoType.M_CLUSTER))
                        || (munition == AmmoType.M_FLAK) || (atype.getAmmoType() == AmmoType.T_HAG));
        if (isFlakAttack) {
            // ...and if so, which one (HAGs get an extra -1 as per TW p. 136
            // that's not covered by anything else).
            if (atype.getAmmoType() == AmmoType.T_HAG) {
                toHit.addModifier(-3, Messages.getString("WeaponAttackAction.HagFlak"));
            } else {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Flak"));
            }
        }
        // Apply ammo type modifier, if any.
        if (usesAmmo && (atype != null) && (atype.getToHitModifier() != 0)) {
            toHit.addModifier(atype.getToHitModifier(),
                    atype.getSubMunitionName() + Messages.getString("WeaponAttackAction.AmmoMod"));
        }

        if ((atype != null)
                && ((atype.getAmmoType() == AmmoType.T_AAA_MISSILE) || (atype.getAmmoType() == AmmoType.T_LAA_MISSILE))
                && Compute.isAirToGround(ae, target)) {
            toHit.addModifier(+4, Messages.getString("WeaponAttackAction.AaaGroundAttack"));
            if (ae.getAltitude() < 4) {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.AaaLowAlt"));
            }
        }

        // add iNarc bonus
        if (isINarcGuided) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.iNarcHoming"));
        }

        // add Artemis V bonus
        if (bArtemisV) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ArtemisV"));
        }

        if (isHaywireINarced) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.iNarcHaywire"));
        }

        // `Screen launchers hit automatically (if in range)
        if ((toHit.getValue() != TargetRoll.IMPOSSIBLE) && ((wtype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)
                || (wtype instanceof ScreenLauncherBayWeapon))) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.ScreenAutoHit"));
        }

        if (bFTL) {
            toHit.addModifier(2,atype.getSubMunitionName()
                    + Messages.getString("WeaponAttackAction.AmmoMod"));
        }

        if (bApollo) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ApolloFcs"));
        }

        // Heavy infantry have +1 penalty
        if ((ae instanceof Infantry) && ae.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.HeavyArmor"));
        }

        // penalty for void sig system
        if (ae.isVoidSigActive()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeVoidSig"));
        }

        // add targeting computer (except with LBX cluster ammo)
        if ((aimingMode == IAimingModes.AIM_MODE_TARG_COMP) && (aimingAt != Entity.LOC_NONE)) {
            if (ae.hasActiveEiCockpit()) {
                if (ae.hasTargComp()) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.AimWithTCompEi"));
                } else {
                    toHit.addModifier(6, Messages.getString("WeaponAttackAction.AimWithEiOnly"));
                }
            } else {
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.AimWithTCompOnly"));
            }
        } else {
            // LB-X cluster, HAG flak, flak ammo ineligible for TC bonus
            boolean usesLBXCluster = usesAmmo && (atype != null)
                    && (atype.getAmmoType() == AmmoType.T_AC_LBX || atype.getAmmoType() == AmmoType.T_AC_LBX_THB)
                    && munition == AmmoType.M_CLUSTER;
            boolean usesHAGFlak = usesAmmo && (atype != null) && atype.getAmmoType() == AmmoType.T_HAG && isFlakAttack;
            boolean isSBGauss = usesAmmo && (atype != null) && atype.getAmmoType() == AmmoType.T_SBGAUSS;
            boolean isFlakAmmo = usesAmmo && (atype != null) && (munition == AmmoType.M_FLAK);
            if (ae.hasTargComp() && wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && !wtype.hasFlag(WeaponType.F_CWS)
                    && !wtype.hasFlag(WeaponType.F_TASER)
                    && (!usesAmmo || !(usesLBXCluster || usesHAGFlak || isSBGauss || isFlakAmmo))) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TComp"));
            }
        }

        // Change hit table for elevation differences inside building.
        if ((null != los.getThruBldg()) && (aElev != tElev)) {

            // Tanks get hit in a random side.
            if (target instanceof Tank) {
                toHit.setSideTable(ToHitData.SIDE_RANDOM);
            } else if (target instanceof Mech) {
                // Meks have special tables for shots from above and below.
                if (aElev > tElev) {
                    toHit.setHitTable(ToHitData.HIT_ABOVE);
                } else {
                    toHit.setHitTable(ToHitData.HIT_BELOW);
                }
            }

        }

        // Change hit table for partial cover, accomodate for partial
        // underwater(legs)
        if (los.getTargetCover() != LosEffects.COVER_NONE) {
            if (underWater && (targHex.containsTerrain(Terrains.WATER) && (targEl == 0) && (te.height() > 0))) {
                // weapon underwater, target in partial water
                toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                toHit.setCover(LosEffects.COVER_UPPER);
            } else {
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER)) {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(los.getTargetCover());
                } else {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(LosEffects.COVER_HORIZONTAL);
                }
                // Set damagable cover state information
                toHit.setDamagableCoverTypePrimary(los.getDamagableCoverTypePrimary());
                toHit.setCoverLocPrimary(los.getCoverLocPrimary());
                toHit.setCoverDropshipPrimary(los.getCoverDropshipPrimary());
                toHit.setCoverBuildingPrimary(los.getCoverBuildingPrimary());
                toHit.setDamagableCoverTypeSecondary(los.getDamagableCoverTypeSecondary());
                toHit.setCoverLocSecondary(los.getCoverLocSecondary());
                toHit.setCoverDropshipSecondary(los.getCoverDropshipSecondary());
                toHit.setCoverBuildingSecondary(los.getCoverBuildingSecondary());
            }
            // XXX what to do about GunEmplacements with partial cover?
            // Only 'mechs can have partial cover - Arlith
        }

        // add penalty for called shots and change hit table, if necessary
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS)) {
            int call = weapon.getCalledShot().getCall();
            if ((call > CalledShot.CALLED_NONE) && (aimingMode != IAimingModes.AIM_MODE_NONE)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.CantAimAndCallShots"));
            }
            switch (call) {
            case CalledShot.CALLED_NONE:
                break;
            case CalledShot.CALLED_HIGH:
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.CalledHigh"));
                toHit.setHitTable(ToHitData.HIT_ABOVE);
                break;
            case CalledShot.CALLED_LOW:
                if (los.getTargetCover() == LosEffects.COVER_HORIZONTAL) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.CalledLowPartCover"));
                }
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.CalledLow"));
                toHit.setHitTable(ToHitData.HIT_BELOW);
                break;
            case CalledShot.CALLED_LEFT:
                // handled by Compute#targetSideTable
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.CalledLeft"));
                break;
            case CalledShot.CALLED_RIGHT:
                // handled by Compute#targetSideTable
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.CalledRight"));
                break;
            }
            // If we're making a called shot with swarm LRMs, then the penalty
            // only applies to the original attack.
            if (call != CalledShot.CALLED_NONE) {
                toSubtract += 3;
            }
        }

        // change hit table for surface vessels hit by underwater attacks
        if (underWater && targHex.containsTerrain(Terrains.WATER) && (null != te) && te.isSurfaceNaval()) {
            toHit.setHitTable(ToHitData.HIT_UNDERWATER);
        }

        // factor in target side
        if (isAttackerInfantry && (0 == distance)) {
            // Infantry attacks from the same hex are resolved against the
            // front.
            toHit.setSideTable(ToHitData.SIDE_FRONT);
        } else {
            toHit.setSideTable(Compute.targetSideTable(ae, target, weapon.getCalledShot().getCall()));
        }

        // Heat Seeking Missles
        if (bHeatSeeking) {
            if (te == null) {
                if ((ttype == Targetable.TYPE_BUILDING)
                        || (ttype == Targetable.TYPE_BLDG_IGNITE)
                        || (ttype == Targetable.TYPE_FUEL_TANK)
                        || (ttype == Targetable.TYPE_FUEL_TANK_IGNITE)
                        || (target instanceof GunEmplacement)) {
                    IHex hexTarget = game.getBoard().getHex(
                            target.getPosition());
                    if (hexTarget.containsTerrain(Terrains.FIRE)) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.AmmoMod"));
                    }
                }
            } else if ((te.isAirborne())
                    && (toHit.getSideTable() == ToHitData.SIDE_REAR)) {
                toHit.addModifier(-2, atype.getSubMunitionName()
                        + Messages.getString("WeaponAttackAction.AmmoMod"));
            } else if (te.heat == 0) {
                toHit.addModifier(1, atype.getSubMunitionName()
                        + Messages.getString("WeaponAttackAction.AmmoMod"));
            } else {
                toHit.addModifier(-te.getHeatMPReduction(),
                        atype.getSubMunitionName()
                                + Messages.getString("WeaponAttackAction.AmmoMod"));
            }

            if (LosEffects.hasFireBetween(ae.getPosition(),
                    target.getPosition(), game)) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.HsmThruFire"));
            }
        }

        // Aeros in atmosphere can hit above and below
        if (Compute.isAirToAir(ae, target)) {
            if ((aAlt - tAlt) > 2) {
                toHit.setHitTable(ToHitData.HIT_ABOVE);
            } else if ((tAlt - aAlt) > 2) {
                toHit.setHitTable(ToHitData.HIT_BELOW);
            } else if (((aAlt - tAlt) > 0) && (te.isAero() && ((IAero) te).isSpheroid())) {
                toHit.setHitTable(ToHitData.HIT_ABOVE);
            } else if (((aAlt - tAlt) < 0) && (te.isAero() && ((IAero) te).isSpheroid())) {
                toHit.setHitTable(ToHitData.HIT_BELOW);
            }
        }
        if (Compute.isGroundToAir(ae, target) && ((aAlt - tAlt) > 2)) {
            toHit.setHitTable(ToHitData.HIT_BELOW);
        }

        if (target.isAirborne() && target.isAero()) {
            if (!(((IAero) target).isSpheroid() && !game.getBoard().inSpace())) {
                // get mods for direction of attack
                int side = toHit.getSideTable();
                // if this is an aero attack using advanced movement rules then
                // determine side differently
                if (game.useVectorMove()) {
                    boolean usePrior = false;
                    Coords attackPos = ae.getPosition();
                    if (game.getBoard().inSpace() && ae.getPosition().equals(target.getPosition())) {
                        int moveSort = Compute.shouldMoveBackHex(ae, (Entity)target);
                        if (moveSort < 0) {
                            attackPos = ae.getPriorPosition();
                        }
                        usePrior = moveSort > 0;
                    }
                    side = ((Entity) target).chooseSide(attackPos, usePrior);
                }
                if (side == ToHitData.SIDE_FRONT) {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeroNoseAttack"));
                }
                if ((side == ToHitData.SIDE_LEFT) || (side == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeroSideAttack"));
                }
            }
        }

        // deal with grapples
        if (target instanceof Entity) {
            int grapple = ((Entity) target).getGrappled();
            if (grapple != Entity.NONE) {
                if ((grapple == ae.getId()) && (((Entity) target).getGrappleSide() == Entity.GRAPPLE_BOTH)) {
                    toHit.addModifier(-4, Messages.getString("WeaponAttackAction.Grappled"));
                } else if ((grapple == ae.getId()) && (((Entity) target).getGrappleSide() != Entity.GRAPPLE_BOTH)) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GrappledByChain"));
                } else if (!exchangeSwarmTarget) {
                    toHit.addModifier(1, Messages.getString("WeaponAttackAction.FireIntoMelee"));
                } else {
                    // this -1 cancels the original +1
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.FriendlyFire"));
                    return toHit;
                }
            }
        }

        // remove old target movement and terrain mods,
        // add those for new target.
        if (exchangeSwarmTarget) {
            toHit.addModifier(-toSubtract, Messages.getString("WeaponAttackAction.OriginalTargetMods"));
            toHit.append(Compute.getImmobileMod(swarmSecondaryTarget, aimingAt, aimingMode));
            toHit.append(Compute.getTargetTerrainModifier(game,
                    game.getTarget(swarmSecondaryTarget.getTargetType(), swarmSecondaryTarget.getTargetId()), eistatus,
                    inSameBuilding, underWater));
            toHit.setCover(LosEffects.COVER_NONE);
            distance = Compute.effectiveDistance(game, ae, swarmSecondaryTarget);

            // We might not attack the new target from the same side as the
            // old, so recalculate; the attack *direction* is still traced from
            // the original source.
            toHit.setSideTable(Compute.targetSideTable(ae, swarmSecondaryTarget));

            // Secondary swarm LRM attacks are never called shots even if the
            // initial one was.
            if (weapon.getCalledShot().getCall() != CalledShot.CALLED_NONE) {
                weapon.getCalledShot().reset();
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            }

            LosEffects swarmlos;
            // TO makes it seem like the terrain modifers should be between the
            // attacker and the secondary target, but we have received rules
            // clarifications on the old forums indicating that this is correct
            if (swarmPrimaryTarget.getTargetType() != Targetable.TYPE_ENTITY) {
                swarmlos = LosEffects.calculateLos(game, swarmSecondaryTarget.getTargetId(), target);
            } else {
                swarmlos = LosEffects.calculateLos(game, swarmPrimaryTarget.getTargetId(), swarmSecondaryTarget);
            }

            // reset cover
            if (swarmlos.getTargetCover() != LosEffects.COVER_NONE) {
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER)) {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(swarmlos.getTargetCover());
                } else {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(LosEffects.COVER_HORIZONTAL);
                }
            }
            // target in water?
            targHex = game.getBoard().getHex(swarmSecondaryTarget.getPosition());
            targEl = swarmSecondaryTarget.relHeight();

            if (swarmSecondaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                Entity oldEnt = game.getEntity(swarmSecondaryTarget.getTargetId());
                toHit.append(Compute.getTargetMovementModifier(game, oldEnt.getId()));
                // target in partial water
                partialWaterLevel = 1;
                if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
                    partialWaterLevel = 2;
                }
                if (targHex.containsTerrain(Terrains.WATER)
                        && (targHex.terrainLevel(Terrains.WATER) == partialWaterLevel) && (targEl == 0)
                        && (oldEnt.height() > 0)) {
                    toHit.setCover(toHit.getCover() | LosEffects.COVER_HORIZONTAL);
                }
                // Prone
                if (oldEnt.isProne()) {
                    // easier when point-blank
                    if (distance <= 1) {
                        proneMod = new ToHitData(-2, Messages.getString("WeaponAttackAction.ProneAdj"));
                    } else {
                        // Harder at range.
                        proneMod = new ToHitData(1, Messages.getString("WeaponAttackAction.ProneRange"));
                    }
                }
                // I-Swarm bonus
                toHit.append(proneMod);
                if (!isECMAffected && (atype != null) && !oldEnt.isEnemyOf(ae)
                        && !(oldEnt.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD) > 0)
                        && (munition == AmmoType.M_SWARM_I)) {
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.SwarmIFriendly"));
                }
            }
        }
        
        //Attacker affected by Taser or TSEMP
        if (ae.getTsempEffect() == TSEMPWeapon.TSEMP_EFFECT_INTERFERENCE) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeTsemped"));
        }
        
        if (ae.getTaserFeedBackRounds() > 0) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeTaserFeedback"));
        }
        if (ae.getTaserInterferenceRounds() > 0) {
            toHit.addModifier(ae.getTaserInterference(), Messages.getString("WeaponAttackAction.AeHitByTaser"));
        }

        if (weapon.getType().hasFlag(WeaponType.F_VGL)) {
            int facing = weapon.getFacing();
            if (ae.isSecondaryArcWeapon(ae.getEquipmentNum(weapon))) {
                facing = (facing + ae.getSecondaryFacing()) % 6;
            }
            Coords c = ae.getPosition().translated(facing);
            if ((target instanceof HexTarget) && target.getPosition().equals(c)) {
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.Vgl"));
            }
        }

        if ((te instanceof Infantry) && ((Infantry) te).isTakingCover()) {
            if (te.getPosition().direction(ae.getPosition()) == te.getFacing()) {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.FireThruCover"));
            }
        }

        if ((ae instanceof Infantry) && ((Infantry) ae).isTakingCover()) {
            if (ae.getPosition().direction(te.getPosition()) == ae.getFacing()) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.FireThruCover"));
            }
        }

        // okay!
        return toHit;
    }

    /**
     * To-hit number for attacker firing a generic weapon at the target. Does
     * not factor in any special weapon or ammo considerations, including range
     * modifiers. Also does not include gunnery skill.
     */
    public static ToHitData toHit(IGame game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);

        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        boolean isAttackerInfantry = ae instanceof Infantry;
        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, te);

        int targEl;
        if (te == null) {
            targEl = game.getBoard().getHex(target.getPosition()).floor();
        } else {
            targEl = te.relHeight();
        }

        // EI system
        // 0 if no EI (or switched off)
        // 1 if no intervening light woods
        // 2 if intervening light woods (because target in woods + intervening
        // woods is only +1 total)
        int eistatus = 0;

        // check LOS (indirect LOS is from the spotter)
        LosEffects los = LosEffects.calculateLos(game, attackerId, target);

        if (ae.hasActiveEiCockpit()) {
            if (los.getLightWoods() > 0) {
                eistatus = 2;
            } else {
                eistatus = 1;
            }
        }

        ToHitData losMods = los.losModifiers(game, eistatus, ae.isUnderwater());
        ToHitData toHit = new ToHitData(0, Messages.getString("WeaponAttackAction.BaseToHit"));

        // taser feedback
        if (ae.getTaserFeedBackRounds() > 0) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeTaserFeedback"));
        }
        // taser interference
        if (ae.getTaserInterferenceRounds() > 0) {
            toHit.addModifier(ae.getTaserInterference(), Messages.getString("WeaponAttackAction.AeHitByTaser"));
        }
        // if we're spotting for indirect fire, add +1
        if (ae.isSpotting() && !ae.getCrew().hasActiveCommandConsole()) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeSpotting"));
        }
        // super heavy modifier
        if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMech"));
        }
        // fatigue
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_FATIGUE)
                && ae.getCrew().isGunneryFatigued()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.Fatigue"));
        }
        // If a unit is suffering from electromagnetic interference, they get a
        // blanket +2.
        // Sucks to be them.
        if (ae.isSufferingEMI()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.EMI"));
        }
        // evading bonuses (
        if ((target.getTargetType() == Targetable.TYPE_ENTITY) && te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), Messages.getString("WeaponAttackAction.TeEvading"));
        }
        if (Compute.isGroundToAir(ae, target) && (null != te) && te.isNOE()) {
            if (te.passedWithin(ae.getPosition(), 1)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.TeNoe"));
            } else {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.TeNoe"));
            }
        }

        if (Compute.isGroundToAir(ae, target)
                && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_AA_FIRE) && (null != te)
                && te.isAero()) {
            int vMod = ((IAero) te).getCurrentVelocity();
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AA_MOVE_MOD)) {
                vMod = Math.min(vMod / 2, 4);
            }
            toHit.addModifier(vMod, Messages.getString("WeaponAttackAction.TeVelocity"));
        }

        // Damage effects for Aero, including LAMs.
        if (ae.isAero()) {
            // pilot hits
            int pilothits = ae.getCrew().getHits();
            if ((pilothits > 0) && !ae.isCapitalFighter()) {
                toHit.addModifier(pilothits, Messages.getString("WeaponAttackAction.PilotHits"));
            }

            // out of control
            if (((IAero)ae).isOutControlTotal()) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeroOoc"));
            }

            // targeting mods for evasive action by large craft
            if (ae.isEvading()) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeEvading"));
            }

            // check for NOE
            if (Compute.isAirToAir(ae, target)) {
                if (target.isAirborneVTOLorWIGE()) {
                    toHit.addModifier(+5, Messages.getString("WeaponAttackAction.TeNonAeroAirborne"));
                }
                if (ae.isNOE()) {
                    if (ae.isOmni()) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeOmniNoe"));
                    } else {
                        toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeNoe"));
                    }
                }
            }

            if (!ae.isAirborne() && !ae.isSpaceborne()) {
                // grounded aero
                if (!(ae instanceof Dropship)) {
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.GroundedAero"));
                } else if (!target.isAirborne()) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GroundedDs"));
                }
            }
        }
        
        // Aeros may suffer from criticals
        if (ae instanceof Aero) {
            Aero aero = (Aero) ae;

            // sensor hits
            int sensors = aero.getSensorHits();

            if (!aero.isCapitalFighter()) {
                if ((sensors > 0) && (sensors < 3)) {
                    toHit.addModifier(sensors, Messages.getString("WeaponAttackAction.SensorDamage"));
                }
                if (sensors > 2) {
                    toHit.addModifier(+5, Messages.getString("WeaponAttackAction.SensorDestroyed"));
                }
            }

            // FCS hits
            int fcs = aero.getFCSHits();

            if ((fcs > 0) && !aero.isCapitalFighter()) {
                toHit.addModifier(fcs * 2, Messages.getString("WeaponAttackAction.FcsDamage"));
            }

            if (aero instanceof Jumpship) {
                Jumpship js = (Jumpship) aero;
                int cic = js.getCICHits();
                if (cic > 0) {
                    toHit.addModifier(cic * 2, Messages.getString("WeaponAttackAction.CicDamage"));
                }
            }
        }

        if (target.isAirborne() && target.isAero()) {

            IAero a = (IAero) target;

            // is the target at zero velocity
            if ((a.getCurrentVelocity() == 0) && !(a.isSpheroid() && !game.getBoard().inSpace())) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.ImmobileAero"));
            }

            // sensor shadows
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SENSOR_SHADOW)
                    && game.getBoard().inSpace()) {
                for (Entity en : Compute.getAdjacentEntitiesAlongAttack(ae.getPosition(), target.getPosition(), game)) {
                    if (!en.isEnemyOf((Entity)a) && en.isLargeCraft() && ((en.getWeight()
                            - ((Entity)a).getWeight()) >= -100000.0)) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorShadow"));
                        break;
                    }
                }
                for (Entity en : game.getEntitiesVector(target.getPosition())) {
                    if (!en.isEnemyOf((Entity)a) && en.isLargeCraft() && !en.equals((Entity) a)
                            && ((en.getWeight() - ((Entity)a).getWeight()) >= -100000.0)) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorShadow"));
                        break;
                    }
                }
            }

        }
        
        // Vehicles may suffer from criticals
        if (ae instanceof Tank) {
            Tank tank = (Tank) ae;
            if (tank.isCommanderHit()) {
                if (ae instanceof VTOL) {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.CopilotHit"));
                } else {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.CmdrHit"));
                }
            }
            int sensors = tank.getSensorHits();
            if (sensors > 0) {
                toHit.addModifier(sensors, Messages.getString("WeaponAttackAction.SensorDamage"));
            }
        }

        // if we have BAP with MaxTech rules, and there are woods in the
        // way, and we are within BAP range, we reduce the BTH by 1
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_BAP) && (te != null) && ae.hasBAP()
                && (ae.getBAPRange() >= Compute.effectiveDistance(game, ae, te))
                && !ComputeECM.isAffectedByECM(ae, ae.getPosition(), te.getPosition())
                && (game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.WOODS)
                        || game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.JUNGLE)
                        || (los.getLightWoods() > 0) || (los.getHeavyWoods() > 0) || (los.getUltraWoods() > 0))) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BAPInWoods"));
        }

        // quirks
        if (ae.hasQuirk(OptionsConstants.QUIRK_NEG_SENSOR_GHOSTS)) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorGhosts"));
        }

        // check for VDNI
        if (ae.hasAbility(OptionsConstants.MD_VDNI)
                || ae.hasAbility(OptionsConstants.MD_BVDNI)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.Vdni"));
        }

        if ((ae instanceof Infantry) && !(ae instanceof BattleArmor)) {
            // check for pl-masc
            // the rules are a bit vague, but assume that if the infantry didn't
            // move or jumped, then they shouldn't get the penalty
            if (ae.hasAbility(OptionsConstants.MD_PL_MASC)
                    && ((ae.moved == EntityMovementType.MOVE_WALK) || (ae.moved == EntityMovementType.MOVE_RUN))) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.PlMasc"));
            }
        }

        // industrial cockpit: +1 to hit
        if ((ae instanceof Mech) && (((Mech) ae).getCockpitType() == Mech.COCKPIT_INDUSTRIAL)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.IndustrialNoAfc"));
        }
        // primitive industrial cockpit: +2 to hit
        if ((ae instanceof Mech) && (((Mech) ae).getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.PrimIndustrialNoAfc"));
        }

        // primitive industrial cockpit with advanced firing control: +1 to hit
        if ((ae instanceof Mech) && (((Mech) ae).getCockpitType() == Mech.COCKPIT_PRIMITIVE)
                && ((Mech) ae).isIndustrial()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.PrimIndustrialAfc"));
        }
        //Bonus to gunnery if both crew members are active; a pilot who takes the gunner's role get +1.
        if (ae instanceof Mech && ((Mech)ae).getCockpitType() == Mech.COCKPIT_DUAL) {
            if (!ae.getCrew().isActive(ae.getCrew().getCrewType().getGunnerPos())) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.GunnerHit"));                
            } else if (ae.getCrew().hasDedicatedGunner()) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.DualCockpit"));
            }
        }
        //The pilot or technical officer can take over the gunner's duties but suffers a +2 penalty.
        if ((ae instanceof TripodMech || ae instanceof QuadVee) && !ae.getCrew().hasDedicatedGunner()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.GunnerHit"));
        }
        if (ae instanceof QuadVee && ae.isConvertingNow()) {
            toHit.addModifier(+3, Messages.getString("WeaponAttackAction.QuadVeeConverting"));
        }

        if ((ae instanceof SupportTank) || (ae instanceof SupportVTOL)) {
            if (!ae.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)
                    && !ae.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.SupVeeNoFc"));
            } else if (ae.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)
                    && !(ae.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL))) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.SupVeeBfc"));
            }
        }

        // determine some more variables
        int aElev = ae.getElevation();
        int tElev = target.getElevation();
        int distance = Compute.effectiveDistance(game, ae, target);

        toHit.append(AbstractAttackAction.nightModifiers(game, target, null, ae, true));

        // weather mods (not in space)
        int weatherMod = game.getPlanetaryConditions().getWeatherHitPenalty(ae);
        if ((weatherMod != 0) && !game.getBoard().inSpace()) {
            toHit.addModifier(weatherMod, game.getPlanetaryConditions().getWeatherDisplayableName());
        }

        // Electro-Magnetic Interference
        if (game.getPlanetaryConditions().hasEMI() && !((ae instanceof Infantry) && !(ae instanceof BattleArmor))) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.EMI"));
        }

        if (ae.isAirborne() && !ae.isAero()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.Dropping"));
            toHit.addModifier(+3, Messages.getString("WeaponAttackAction.Jumping"));
        }

        // Attacks against adjacent buildings automatically hit.
        if ((distance == 1) && ((target.getTargetType() == Targetable.TYPE_BUILDING)
                || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE)
                || (target instanceof GunEmplacement))) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.AdjBuilding"));
        }

        // Attacks against buildings from inside automatically hit.
        if ((null != los.getThruBldg()) && ((target.getTargetType() == Targetable.TYPE_BUILDING)
                || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE)
                || (target instanceof GunEmplacement))) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.InsideBuilding"));
        }

        if (ae.hasQuirk(OptionsConstants.QUIRK_POS_ANTI_AIR) && (target instanceof Entity)) {
            if (target.isAirborneVTOLorWIGE() || target.isAirborne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.AaVsAir"));
            }
        }

        // air-to-ground strikes apply a +2 mod
        if (Compute.isAirToGround(ae, target)
                || (ae.isBomber() && ((IBomber)ae).isVTOLBombing())) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AtgStrike"));
        }

        // units making air to ground attacks are easier to hit by air-to-air
        // attacks
        if ((null != te) && Compute.isAirToAir(ae, target)) {
            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if ((prevAttack.getEntityId() == te.getId()) && prevAttack.isAirToGround(game)) {
                    toHit.addModifier(-3, Messages.getString("WeaponAttackAction.TeGroundAttack"));
                    break;
                }
            }
        }

        // units with the narrow/low profile quirk are harder to hit
        if ((te != null) && te.hasQuirk(OptionsConstants.QUIRK_POS_LOW_PROFILE)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.LowProfile"));
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (!isAttackerInfantry && (te != null) && (te instanceof BattleArmor)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.BaTarget"));
        }

        // Infantry squads are also hard to hit -- including for other infantry,
        // it seems (the rule is "all attacks"). However, this only applies to
        // proper squads deployed as such.
        if ((te instanceof Infantry) && !(te instanceof BattleArmor) && ((Infantry) te).isSquad()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.SquadTarget"));
        }

        // Ejected MechWarriors are also more difficult targets.
        if ((te != null) && (te instanceof MechWarrior)) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.MwTarget"));
        }

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        if (te != null) {
            ToHitData thTemp = Compute.getTargetMovementModifier(game, target.getTargetId());
            toHit.append(thTemp);
        }

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain, not applicable when delivering minefields or bombs
        if (target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER) {
            toHit.append(Compute.getTargetTerrainModifier(game, target, eistatus, inSameBuilding, ae.isUnderwater()));
        }

        // target in water?
        IHex targHex = game.getBoard().getHex(target.getPosition());
        if ((target.getTargetType() == Targetable.TYPE_ENTITY) && targHex.containsTerrain(Terrains.WATER)
                && (targHex.terrainLevel(Terrains.WATER) == 1) && (targEl == 0) && (te.height() > 0)) { // target
            // in
            // partial
            // water
            los.setTargetCover(los.getTargetCover() | LosEffects.COVER_HORIZONTAL);
            losMods = los.losModifiers(game, eistatus, ae.isUnderwater());
        }

        // add in LOS mods that we've been keeping
        toHit.append(losMods);

        if ((te != null) && te.isHullDown()) {
            if ((te instanceof Mech && !(te instanceof QuadVee && te.getConversionMode() == QuadVee.CONV_MODE_VEHICLE))
                    && (los.getTargetCover() > LosEffects.COVER_NONE)) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.HullDown"));
            }
            // tanks going Hull Down is different rules then 'Mechs, the
            // direction the attack comes from matters
            else if ((te instanceof Tank || (te instanceof QuadVee && te.getConversionMode() == QuadVee.CONV_MODE_VEHICLE))
                    && targHex.containsTerrain(Terrains.FORTIFIED)) {
                // TODO make this a LoS mod so that attacks will come in from
                // directions that grant Hull Down Mods
                int moveInDirection = ToHitData.SIDE_FRONT;

                if (!((Tank) te).isBackedIntoHullDown()) {
                    moveInDirection = ToHitData.SIDE_FRONT;
                } else {
                    moveInDirection = ToHitData.SIDE_REAR;
                }

                if ((te.sideTable(ae.getPosition()) == moveInDirection)
                        || (te.sideTable(ae.getPosition()) == ToHitData.SIDE_LEFT)
                        || (te.sideTable(ae.getPosition()) == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.HullDown"));
                }
            }
        }

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), Messages.getString("WeaponAttackAction.Heat"));
        }

        // target immobile
        ToHitData immobileMod = Compute.getImmobileMod(target, -1, -1);
        // grounded dropships are treated as immobile as well for purpose of
        // the mods
        if ((null != te) && !te.isAirborne() && !te.isSpaceborne() && (te instanceof Aero)
                && ((Aero) te).isSpheroid()) {
            immobileMod = new ToHitData(-4, Messages.getString("WeaponAttackAction.ImmobileDs"));
        }
        if (immobileMod != null) {
            toHit.append(immobileMod);
        }

        // attacker prone
        if (ae.isProne()) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.AeProne"));
        }

        // target prone
        ToHitData proneMod = null;
        if ((te != null) && te.isProne()) {
            // easier when point-blank
            if (distance <= 1) {
                // TW, pg. 221: Swarm Mek attacks apply prone/immobile mods as
                // normal.
                proneMod = new ToHitData(-2, Messages.getString("WeaponAttackAction.ProneAdj"));
            } else {
                // Harder at range.
                proneMod = new ToHitData(1, Messages.getString("WeaponAttackAction.ProneRange"));
            }
        }
        if (proneMod != null) {
            toHit.append(proneMod);
        }

        // Heavy infantry have +1 penalty
        if ((ae instanceof Infantry) && ae.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.HeavyArmor"));
        }

        // penalty for void sig system
        if (ae.isVoidSigActive()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeVoidSig"));
        }

        // Change hit table for elevation differences inside building.
        if ((null != los.getThruBldg()) && (aElev != tElev)) {

            // Tanks get hit in a random side.
            if (target instanceof Tank) {
                toHit.setSideTable(ToHitData.SIDE_RANDOM);
            } else if (target instanceof Mech) {
                // Meks have special tables for shots from above and below.
                if (aElev > tElev) {
                    toHit.setHitTable(ToHitData.HIT_ABOVE);
                } else {
                    toHit.setHitTable(ToHitData.HIT_BELOW);
                }
            }

        }

        // Change hit table for partial cover, accomodate for partial
        // underwater(legs)
        if (los.getTargetCover() != LosEffects.COVER_NONE) {
            if (ae.isUnderwater() && (targHex.containsTerrain(Terrains.WATER) && (targEl == 0) && (te.height() > 0))) {
                // weapon underwater, target in partial water
                toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                toHit.setCover(LosEffects.COVER_UPPER);
            } else {
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER)) {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(los.getTargetCover());
                } else {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(LosEffects.COVER_HORIZONTAL);
                }
                // Set damagable cover state information
                toHit.setDamagableCoverTypePrimary(los.getDamagableCoverTypePrimary());
                toHit.setCoverLocPrimary(los.getCoverLocPrimary());
                toHit.setCoverDropshipPrimary(los.getCoverDropshipPrimary());
                toHit.setCoverBuildingPrimary(los.getCoverBuildingPrimary());
                toHit.setDamagableCoverTypeSecondary(los.getDamagableCoverTypeSecondary());
                toHit.setCoverLocSecondary(los.getCoverLocSecondary());
                toHit.setCoverDropshipSecondary(los.getCoverDropshipSecondary());
                toHit.setCoverBuildingSecondary(los.getCoverBuildingSecondary());
            }
        }

        // change hit table for surface vessels hit by underwater attacks
        if (ae.isUnderwater() && targHex.containsTerrain(Terrains.WATER) && (null != te) && te.isSurfaceNaval()) {
            toHit.setHitTable(ToHitData.HIT_UNDERWATER);
        }

        if (target.isAirborne() && target.isAero()) {
            if (!(((IAero) target).isSpheroid() && !game.getBoard().inSpace())) {
                // get mods for direction of attack
                int side = toHit.getSideTable();
                // if this is an aero attack using advanced movement rules then
                // determine side differently
                if (game.useVectorMove()) {
                    boolean usePrior = false;
                    Coords attackPos = ae.getPosition();
                    if (game.getBoard().inSpace() && ae.getPosition().equals(target.getPosition())) {
                        int moveSort = Compute.shouldMoveBackHex(ae, (Entity)target);
                        if (moveSort < 0) {
                            attackPos = ae.getPriorPosition();
                        }
                        usePrior = moveSort > 0;
                    }
                    side = ((Entity) target).chooseSide(attackPos, usePrior);
                }
                if (side == ToHitData.SIDE_FRONT) {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeroNoseAttack"));
                }
                if ((side == ToHitData.SIDE_LEFT) || (side == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeroSideAttack"));
                }
            }
        }

        if (ae.getTsempEffect() == TSEMPWeapon.TSEMP_EFFECT_INTERFERENCE) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeTsemped"));
        }

        if ((te instanceof Infantry) && ((Infantry) te).isTakingCover()) {
            if (te.getPosition().direction(ae.getPosition()) == te.getFacing()) {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.FireThruCover"));
            }
        }

        if ((ae instanceof Infantry) && ((Infantry) ae).isTakingCover()) {
            if (ae.getPosition().direction(te.getPosition()) == ae.getFacing()) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.FireThruCover"));
            }
        }

        // okay!
        return toHit;
    }

    private static String toHitIsImpossible(IGame game, Entity ae, Entity te, Targetable target, Targetable swarmPrimaryTarget,
            Targetable swarmSecondaryTarget, Mounted weapon, Mounted ammo, AmmoType atype, WeaponType wtype, int ttype,
            LosEffects los, boolean exchangeSwarmTarget, boolean usesAmmo, boolean isTAG, boolean isInferno,
            boolean isAttackerInfantry, boolean isIndirect, int attackerId, int weaponId, boolean isArtilleryIndirect,
            boolean isArtilleryFLAK, boolean targetInBuilding, boolean isArtilleryDirect,
            boolean isTargetECMAffected, boolean isStrafing, boolean isBearingsOnlyMissile, boolean isCruiseMissile) {
        boolean isHoming = false;
        ToHitData toHit = null;

        if ((target instanceof Entity) && ((Entity)target).isHidden()) {
            return Messages.getString("WeaponAttackAction.NoFireAtHidden");
        }

        if (weapon.isSquadSupportWeapon() && (ae instanceof BattleArmor)) {
            if (!((BattleArmor) ae).isTrooperActive(BattleArmor.LOC_TROOPER_1)) {
                return Messages.getString("WeaponAttackAction.NoSquadSupport");
            }
        }

        // BA NARCs and Tasers can only fire at one target in a round
        if ((ae instanceof BattleArmor)
                && (weapon.getType().hasFlag(WeaponType.F_TASER) || wtype.getAmmoType() == AmmoType.T_NARC)) {
            // Go through all of the current actions to see if a NARC or Taser
            // has been fired
            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                // Is this an attack from this entity to a different target?
                if (prevAttack.getEntityId() == ae.getId() && prevAttack.getTargetId() != target.getTargetId()) {
                    Mounted prevWeapon = ae.getEquipment(prevAttack.getWeaponId());
                    WeaponType prevWtype = (WeaponType) prevWeapon.getType();
                    if (prevWeapon.getType().hasFlag(WeaponType.F_TASER)
                            && weapon.getType().hasFlag(WeaponType.F_TASER)) {
                        return Messages.getString("WeaponAttackAction.BATaserSameTarget");
                    }
                    if (prevWtype.getAmmoType() == AmmoType.T_NARC && wtype.getAmmoType() == AmmoType.T_NARC) {
                        return Messages.getString("WeaponAttackAction.BANarcSameTarget");
                    }
                }
            }
        }

        // BA can only make one AP attack
        if ((ae instanceof BattleArmor) && weapon.getType().hasFlag(WeaponType.F_INFANTRY)) {
            final int weapId = ae.getEquipmentNum(weapon);
            // See if this unit has made a previous AP attack
            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                // Is this an attack from this entity
                if (prevAttack.getEntityId() == ae.getId()) {
                    Mounted prevWeapon = ae.getEquipment(prevAttack.getWeaponId());
                    WeaponType prevWtype = (WeaponType) prevWeapon.getType();
                    if (prevWtype.hasFlag(WeaponType.F_INFANTRY) && (prevAttack.getWeaponId() != weapId)) {
                        return Messages.getString("WeaponAttackAction.OnlyOneBAAPAttack");
                    }
                }
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_TANK_CREWS) && (ae instanceof Tank) && ae.isUnjammingRAC()
                && (ae.getCrew().getSize() == 1)) {
            return Messages.getString("WeaponAttackAction.VeeSingleCrew");
        }

        // is the attack originating from underwater
        boolean underWater = (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET)
                || (wtype instanceof SRTWeapon) || (wtype instanceof LRTWeapon);
        
        // Torpedos must remain in the water over their whole path to the target
        if ((atype != null)
                && ((atype.getAmmoType() == AmmoType.T_LRM_TORPEDO)
                        || (atype.getAmmoType() == AmmoType.T_SRM_TORPEDO)
                        || (((atype.getAmmoType() == AmmoType.T_SRM)
                                || (atype.getAmmoType() == AmmoType.T_SRM_IMP)
                                || (atype.getAmmoType() == AmmoType.T_MRM)
                                || (atype.getAmmoType() == AmmoType.T_LRM)
                                || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                                || (atype.getAmmoType() == AmmoType.T_MML)) && (atype.getMunitionType() == AmmoType.M_TORPEDO)))
                && (los.getMinimumWaterDepth() < 1)) {
            return Messages.getString("WeaponAttackAction.TorpOutOfWater");
        }

        if ((ae instanceof Protomech) && ((Protomech) ae).isEDPCharging() && wtype.hasFlag(WeaponType.F_ENERGY)) {
            return Messages.getString("WeaponAttackAction.ChargingEDP");
        }

        // tasers only at non-flying units
        if (wtype.hasFlag(WeaponType.F_TASER)) {
            if (te != null) {
                if (te.isAirborne() || te.isAirborneVTOLorWIGE()) {
                    return Messages.getString("WeaponAttackAction.NoTaserAtAirborne");
                }
            } else {
                return Messages.getString("WeaponAttackAction.TaserOnlyAtUnit");
            }
        }

        if (wtype.hasFlag(WeaponType.F_TSEMP) && wtype.hasFlag(WeaponType.F_ONESHOT) && weapon.isFired()) {
            return Messages.getString("WeaponAttackAction.OneShotTSEMP");
        }

        if (wtype.hasFlag(WeaponType.F_TSEMP) && weapon.isFired()) {
            return Messages.getString("WeaponAttackAction.TSEMPRecharging");
        }

        // only leg mounted b-pods can be fired normally
        if (wtype.hasFlag(WeaponType.F_B_POD)) {
            if (!(target instanceof Infantry)) {
                return Messages.getString("WeaponAttackAction.BPodOnlyAtInf");
            }
            if (ae instanceof BipedMech) {
                if (!((weapon.getLocation() == Mech.LOC_LLEG) || (weapon.getLocation() == Mech.LOC_RLEG))) {
                    return Messages.getString("WeaponAttackAction.OnlyLegBPod");
                }
            } else if (ae instanceof QuadMech) {
                if (!((weapon.getLocation() == Mech.LOC_LLEG) || (weapon.getLocation() == Mech.LOC_RLEG)
                        || (weapon.getLocation() == Mech.LOC_LARM) || (weapon.getLocation() == Mech.LOC_RARM))) {
                    return Messages.getString("WeaponAttackAction.OnlyLegBPod");
                }
            }
        }
        if (ae.hasShield() && ae.hasActiveShield(weapon.getLocation(), weapon.isRearMounted())) {
            return Messages.getString("WeaponAttackAction.ActiveShieldBlocking");
        }
        // If it has a torso-mounted cockpit and two head sensor hits or three
        // sensor hits...
        // It gets a =4 penalty for being blind!
        if ((ae instanceof Mech) && (((Mech) ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) {
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                return Messages.getString("WeaponAttackAction.SensorsDestroyedTMC");
            }
        }

        // can't fire Indirect LRM with direct LOS
        if (isIndirect && game.getOptions().booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)
                && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_INDIRECT_ALWAYS_POSSIBLE)
                && LosEffects.calculateLos(game, ae.getId(), target).canSee()
                && (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                        || Compute.canSee(game, ae, target))
                && !(wtype instanceof ArtilleryCannonWeapon) && !(wtype instanceof MekMortarWeapon)) {
            return Messages.getString("WeaponAttackAction.NoIndirectWithLOS");
        }

        // If we're lying mines, we can't shoot.
        if (ae.isLayingMines()) {
            return Messages.getString("WeaponAttackAction.BusyLayingMines");
        }

        // make sure weapon can deliver minefield
        if ((target.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER) && !AmmoType.canDeliverMinefield(atype)) {
            return Messages.getString("WeaponAttackAction.NoMinefields");
        }
        if ((target.getTargetType() == Targetable.TYPE_FLARE_DELIVER) && !(usesAmmo
                && ((atype.getAmmoType() == AmmoType.T_LRM) 
                        || (atype.getAmmoType() == AmmoType.T_MML)
                        || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                        || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                && (atype.getMunitionType() == AmmoType.M_FLARE))) {
            return Messages.getString("WeaponAttackAction.NoFlares");
        }
        if ((game.getPhase() == IGame.Phase.PHASE_TARGETING) && (!(isArtilleryIndirect || isBearingsOnlyMissile))) {
            return Messages.getString("WeaponAttackAction.NotValidForTargPhase");
        }
        if ((game.getPhase() == IGame.Phase.PHASE_OFFBOARD) && !isTAG) {
            return Messages.getString("WeaponAttackAction.OnlyTagInOffboard");
        }
        if ((game.getPhase() != IGame.Phase.PHASE_OFFBOARD) && isTAG) {
            return Messages.getString("WeaponAttackAction.TagOnlyInOffboard");
        }
        if (isArtilleryDirect && ae.isAirborne()) {
            return Messages.getString("WeaponAttackAction.NoAeroDirectArty");
        }

        if (isArtilleryDirect && (Compute.effectiveDistance(game, ae, target) <= 6)) {
            return Messages.getString("WeaponAttackAction.TooShortForDirectArty");
        }
        
        // check called shots
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS)) {
            String reason = weapon.getCalledShot().isValid(target);
            if (reason != null) {
                return reason;
            }
        }

        if ((atype != null)
                && ((atype.getAmmoType() == AmmoType.T_LRM) 
                        || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                        || (atype.getAmmoType() == AmmoType.T_MML)
                        || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                && ((atype.getMunitionType() == AmmoType.M_THUNDER)
                        || (atype.getMunitionType() == AmmoType.M_THUNDER_ACTIVE)
                        || (atype.getMunitionType() == AmmoType.M_THUNDER_INFERNO)
                        || (atype.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB)
                        || (atype.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED))
                && (target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER)) {
            return Messages.getString("WeaponAttackAction.OnlyMinefields");
        }
        if ((atype != null) && ((atype.getAmmoType() == AmmoType.T_LRM)
                || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                || (atype.getAmmoType() == AmmoType.T_MML))
                && (atype.getMunitionType() == AmmoType.M_FLARE)
                && (target.getTargetType() != Targetable.TYPE_FLARE_DELIVER)) {
            return Messages.getString("WeaponAttackAction.OnlyFlare");
        }
        
        // Anti ship missiles can't be launched from altitude 3 or lower
        if (wtype.hasFlag(WeaponType.F_ANTI_SHIP) && !game.getBoard().inSpace() && (ae.getAltitude() < 4)) {
            return Messages.getString("WeaponAttackAction.TooLowForASM");
        }
        
        // ASEW Missiles cannot be launched in an atmosphere
        if ((wtype.getAmmoType() == AmmoType.T_ASEW_MISSILE)
                && !ae.isSpaceborne()) {
            return Messages.getString("WeaponAttackAction.ASEWAtmo");
        }

        // some weapons can only target infantry
        if (wtype.hasFlag(WeaponType.F_INFANTRY_ONLY)) {
            if (((te != null) && !(te instanceof Infantry)) || (target.getTargetType() != Targetable.TYPE_ENTITY)) {
                return Messages.getString("WeaponAttackAction.TargetOnlyInf");
            }
        }

        // make sure weapon can clear minefield
        if ((target instanceof MinefieldTarget) && !AmmoType.canClearMinefield(atype)) {
            return Messages.getString("WeaponAttackAction.CantClearMines");
        }

        // Arty shots have to be with arty, non arty shots with non arty.
        if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
            // check artillery is targetted appropriately for its ammo
            long munition = AmmoType.M_STANDARD;
            if (atype != null) {
                munition = atype.getMunitionType();
            }
            if (munition == AmmoType.M_HOMING && ammo.curMode().equals("Homing")) {
                // target type checked later because its different for
                // direct/indirect (BMRr p77 on board arrow IV)
                isHoming = true;
            } else if ((ttype != Targetable.TYPE_HEX_ARTILLERY) && (ttype != Targetable.TYPE_MINEFIELD_CLEAR)
                    && !isArtilleryFLAK) {
                return Messages.getString("WeaponAttackAction.ArtyAttacksOnly");
            }
            if (ae.isAirborne()) {
                if (isArtilleryDirect) {
                    return Messages.getString("WeaponAttackAction.NoAeroDirectArty");
                } else if (isArtilleryIndirect) {
                    if (ae.getAltitude() > 9) {
                        return Messages.getString("WeaponAttackAction.TooHighForArty");
                    }
                    if (ae.usesWeaponBays()) {
                        //For Dropships
                        for (int wId : weapon.getBayWeapons()) {
                            Mounted bayW = ae.getEquipment(wId);
                            // check the loaded ammo for the Arrow IV flag
                            Mounted bayWAmmo = bayW.getLinked();
                            AmmoType bAType = (AmmoType) bayWAmmo.getType();
                            if (bAType.getAmmoType() != AmmoType.T_ARROW_IV) {
                                return Messages.getString("WeaponAttackAction.OnlyArrowArty");
                            }
                        }
                    } else if (wtype.getAmmoType() != AmmoType.T_ARROW_IV) {
                        //For Fighters, LAMs, Small Craft and VTOLs
                        return Messages.getString("WeaponAttackAction.OnlyArrowArty");
                    }
                }
            }
        } else if (weapon.isInBearingsOnlyMode()) {             
            //We don't really need to do anything here. This just prevents these weapons from returning impossible.
        } else {
            // weapon is not artillery
            if (ttype == Targetable.TYPE_HEX_ARTILLERY) {
                return Messages.getString("WeaponAttackAction.NoArtyAttacks");
            }
        }

        // check the following only if we're not a flight of continuing swarm
        // missiles
        if (!exchangeSwarmTarget) {

            if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE) && !isStrafing) {
                // a friendly unit can never be the target of a direct attack.
                // but we do allow vehicle flamers to cool
                if ((target.getTargetType() == Targetable.TYPE_ENTITY) && !te.getOwner().isEnemyOf(ae.getOwner())) {
                    if (!(usesAmmo && (atype.getMunitionType() == AmmoType.M_COOLANT))) {
                        return Messages.getString("WeaponAttackAction.NoFriendlyTarget");
                    }
                }
            }
            // can't target yourself,
            if (ae.equals(te)) {
                return Messages.getString("WeaponAttackAction.NoSelfTarget");
            }
            // is the attacker even active?
            if (ae.isShutDown() || !ae.getCrew().isActive()) {
                return Messages.getString("WeaponAttackAction.AttackerNotReady");
            }

            // sensors operational?
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            if ((ae instanceof Mech) && (((Mech) ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) {
                sensorHits += ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_CT);
                if (sensorHits > 2) {
                    return Messages.getString("WeaponAttackAction.SensorsDestroyed");
                }
            } else if ((sensorHits > 1)
                    || ((ae instanceof Mech) && (((Mech) ae).isIndustrial() && (sensorHits == 1)))) {
                return Messages.getString("WeaponAttackAction.SensorsDestroyed");
            }
            // weapon operational?
            if (!weapon.canFire(isStrafing)) {
                return Messages.getString("WeaponAttackAction.WeaponNotReady");
            }

            // got ammo?
            if (usesAmmo && ((ammo == null) || (ammo.getUsableShotsLeft() == 0))) {
                return Messages.getString("WeaponAttackAction.OutOfAmmo");
            }

            // Aeros must have enough ammo for the maximum rate of fire because
            // they cannot lower it
            if (ae.isAero() && usesAmmo && (ammo != null)
                    && (ae.getTotalAmmoOfType(ammo.getType()) < weapon.getCurrentShots())) {
                return Messages.getString("WeaponAttackAction.InsufficientAmmo");
            }
            
            if ((ae instanceof LandAirMech) && (ae.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER)
                    && usesAmmo && ammo != null && !((AmmoType)ammo.getType()).canAeroUse(game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS))) {
                return Messages.getString("WeaponAttackAction.InvalidAmmoForFighter");
            }

            if (ae instanceof Tank) {
                sensorHits = ((Tank) ae).getSensorHits();
                if (sensorHits > 3) {
                    return Messages.getString("WeaponAttackAction.SensorsDestroyed");
                }
                if (((Tank) ae).getStunnedTurns() > 0) {
                    return Messages.getString("WeaponAttackAction.CrewStunned");
                }
            }
        }

        // Are we dumping that ammo?
        if (usesAmmo && ammo.isDumping()) {
            ae.loadWeaponWithSameAmmo(weapon);
            if ((ammo.getUsableShotsLeft() == 0) || ammo.isDumping()) {
                return Messages.getString("WeaponAttackAction.DumpingAmmo");
            }
        }

        if (ae.isEvading() && !(ae instanceof Dropship) && !(ae instanceof Jumpship)) {
            return Messages.getString("WeaponAttackAction.AeEvading");
        }

        if (ae instanceof Aero) {
            Aero aero = (Aero) ae;
            // FCS hits
            int fcs = aero.getFCSHits();
            if (fcs > 2) {
                return Messages.getString("WeaponAttackAction.FCSDestroyed");
            }

            if (aero instanceof Jumpship) {
                Jumpship js = (Jumpship) aero;
                int cic = js.getCICHits();
                if (cic > 2) {
                    return Messages.getString("WeaponAttackAction.CICDestroyed");
                }
            }
        }
        
        if (ae.isAero()) {
            // if bombing, then can't do other attacks
            // also for altitude bombing, you must either be the first or be
            // adjacent to a prior one
            boolean adjacentAltBomb = false;
            boolean firstAltBomb = true;
            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                if (prevAttack.getEntityId() == attackerId) {
                    if ((weaponId != prevAttack.getWeaponId())
                            && ae.getEquipment(prevAttack.getWeaponId()).getType().hasFlag(WeaponType.F_SPACE_BOMB)) {
                        return Messages.getString("WeaponAttackAction.BusySpaceBombing");
                    }
                    if ((weaponId != prevAttack.getWeaponId())
                            && ae.getEquipment(prevAttack.getWeaponId()).getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
                        return Messages.getString("WeaponAttackAction.BusyDiveBombing");
                    }
                    if ((weaponId != prevAttack.getWeaponId())
                            && ae.getEquipment(prevAttack.getWeaponId()).getType().hasFlag(WeaponType.F_ALT_BOMB)) {
                        // if the current attack is not an altitude bombing then
                        // return
                        if (!wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                            return Messages.getString("WeaponAttackAction.BusyAltBombing");
                        }
                        firstAltBomb = false;
                        int distance = prevAttack.getTarget(game).getPosition().distance(target.getPosition());
                        if (distance == 1) {
                            adjacentAltBomb = true;
                        }
                        if (distance == 0) {
                            return Messages.getString("WeaponAttackAction.AlreadyBombingHex");
                        }

                    }
                }
            }
            if (wtype.hasFlag(WeaponType.F_ALT_BOMB) && !firstAltBomb && !adjacentAltBomb) {
                return Messages.getString("WeaponAttackAction.BombNotInLine");
            }
        }

        // you cannot bracket small craft at short range
        if (wtype.hasModes()
                && (weapon.curMode().equals("Bracket 80%") || weapon.curMode().equals("Bracket 60%")
                        || weapon.curMode().equals("Bracket 40%"))
                && target.isAero() && !te.isLargeCraft()
                && (RangeType.rangeBracket(ae.getPosition().distance(target.getPosition()), wtype.getRanges(weapon),
                        true, false) == RangeType.RANGE_SHORT)) {
            return Messages.getString("WeaponAttackAction.TooCloseForSCBracket");
        }

        // you must have enough weapons in your bay to be able to use bracketing
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 80%") && (weapon.getBayWeapons().size() < 2)) {
            return Messages.getString("WeaponAttackAction.BayTooSmallForBracket");
        }
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 60%") && (weapon.getBayWeapons().size() < 3)) {
            return Messages.getString("WeaponAttackAction.BayTooSmallForBracket");
        }
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 40%") && (weapon.getBayWeapons().size() < 4)) {
            return Messages.getString("WeaponAttackAction.BayTooSmallForBracket");
        }
        
        // If you're an aero, can't fire an AMS Bay or a Point Defense bay in PD Mode
        if (wtype.hasFlag(WeaponType.F_AMSBAY)) {
            return Messages.getString("WeaponAttackAction.AutoWeapon");
        } else if (wtype.hasModes() && weapon.curMode().equals("Point Defense")) {
            return Messages.getString("WeaponAttackAction.PDWeapon");
        }
        
        // Is the weapon blocked by a tractor/trailer?
        if (ae.getTowing() != Entity.NONE || ae.getTowedBy() != Entity.NONE) {
            if (ae.isWeaponBlockedByTowing(weapon.getLocation(), ae.getSecondaryFacing(), weapon.isRearMounted())) {
                return Messages.getString("WeaponAttackAction.TrailerBlock");
            }
        }
        
        // Is the weapon blocked by a passenger?
        if (ae.isWeaponBlockedAt(weapon.getLocation(), weapon.isRearMounted())) {
            return Messages.getString("WeaponAttackAction.PassengerBlock");
        }

        // Can't target an entity conducting a swarm attack.
        if ((te != null) && (Entity.NONE != te.getSwarmTargetId())) {
            return Messages.getString("WeaponAttackAction.TargetSwarming");
        }

        // "Cool" mode for vehicle flamer requires coolant system
        boolean vf_cool = false;
        if ((atype != null) && usesAmmo && (((AmmoType) ammo.getType()).getMunitionType() == AmmoType.M_COOLANT)) {
            vf_cool = true;
        }

        if (Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType()) {
            if (!wtype.hasFlag(WeaponType.F_EXTINGUISHER) && !vf_cool) {
                return Messages.getString("WeaponAttackAction.InvalidForFirefighting");
            }
            IHex hexTarget = game.getBoard().getHex(target.getPosition());
            if (!hexTarget.containsTerrain(Terrains.FIRE)) {
                return Messages.getString("WeaponAttackAction.TargetNotBurning");
            }
        } else if (wtype.hasFlag(WeaponType.F_EXTINGUISHER)) {
            if (!(((target instanceof Tank) && ((Tank) target).isOnFire())
                    || ((target instanceof Entity) && (((Entity) target).infernos.getTurnsLeftToBurn() > 0)))) {
                return Messages.getString("WeaponAttackAction.TargetNotBurning");
            }
        }
        // Infantry can't clear woods.
        if (isAttackerInfantry && (Targetable.TYPE_HEX_CLEAR == target.getTargetType())) {
            IHex hexTarget = game.getBoard().getHex(target.getPosition());
            if (hexTarget.containsTerrain(Terrains.WOODS)) {
                return Messages.getString("WeaponAttackAction.NoInfantryWoodsClearing");
            }
        }

        // only screen launchers may launch screens (what a coincidence)
        if (Targetable.TYPE_HEX_SCREEN == target.getTargetType()) {
            if (!((wtype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) || (wtype instanceof ScreenLauncherBayWeapon))) {
                return Messages.getString("WeaponAttackAction.ScreenLauncherOnly");
            }
        }

        if ((Targetable.TYPE_HEX_SCREEN != target.getTargetType())
                && ((wtype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)
                        || (wtype instanceof ScreenLauncherBayWeapon))) {
            return Messages.getString("WeaponAttackAction.ScreenHexOnly");
        }

        if (!(target instanceof HexTarget) && (atype != null)
                && (atype.getMunitionType() == AmmoType.M_MINE_CLEARANCE)) {
            return Messages.getString("WeaponAttackAction.MineClearHexOnly");
        }

        // Some weapons can't cause fires, but Infernos always can.
        if ((vf_cool || (wtype.hasFlag(WeaponType.F_NO_FIRES) && !isInferno))
                && (Targetable.TYPE_HEX_IGNITE == target.getTargetType())) {
            return Messages.getString("WeaponAttackAction.WeaponCantIgnite");
        }

        // only woods and buildings can be set intentionally on fire
        if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                && game.getOptions().booleanOption(OptionsConstants.ADVANCED_NO_IGNITE_CLEAR)
                && !(game.getBoard().getHex(((HexTarget) target).getPosition()).containsTerrain(Terrains.WOODS)
                        || game.getBoard().getHex(((HexTarget) target).getPosition()).containsTerrain(Terrains.JUNGLE)
                        || game.getBoard().getHex(((HexTarget) target).getPosition())
                                .containsTerrain(Terrains.FUEL_TANK)
                        || game.getBoard().getHex(((HexTarget) target).getPosition())
                                .containsTerrain(Terrains.BUILDING))) {
            return Messages.getString("WeaponAttackAction.CantIntentionallyBurn");
        }

        // Can't target infantry with Inferno rounds (BMRr, pg. 141).
        // Also, enforce options for keeping vehicles and protos safe
        // if those options are checked.
        if (isInferno && (((te instanceof Tank)
                && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_SAFE_FROM_INFERNOS))
                || ((te instanceof Protomech)
                        && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_PROTOS_SAFE_FROM_INFERNOS)))) {
            return Messages.getString("WeaponAttackAction.CantShootWithInferno");
        }

        // The TAG system cannot target infantry.
        if (isTAG && (te instanceof Infantry)) {
            return Messages.getString("WeaponAttackAction.CantTAGInf");
        }

        // The TAG system cannot target Airborne Aeros.
        if (isTAG && (te != null) && (te.isAirborne() || te.isSpaceborne())) {
            return Messages.getString("WeaponAttackAction.CantTAGAero");
        }

        // Airborne units cannot tag and attack
        // http://bg.battletech.com/forums/index.php?topic=17613.new;topicseen#new
        if (ae.isAirborne() && ae.usedTag()) {
            return Messages.getString("WeaponAttackAction.AeroCantTAGAndShoot");
        }

        // limit large craft to zero net heat and to heat by arc
        final int heatcap = ae.getHeatCapacity();
        if (ae.usesWeaponBays() && (weapon.getBayWeapons().size() > 0)) {
            int totalheat = 0;

            // first check to see if there are any usable weapons
            boolean useable = false;
            for (int wId : weapon.getBayWeapons()) {
                Mounted m = ae.getEquipment(wId);
                WeaponType bayWType = ((WeaponType) m.getType());
                boolean bayWUsesAmmo = (bayWType.getAmmoType() != AmmoType.T_NA);
                if (m.canFire()) {
                    if (bayWUsesAmmo) {
                        if ((m.getLinked() != null) && (m.getLinked().getUsableShotsLeft() > 0)) {
                            useable = true;
                            break;
                        }
                    } else {
                        useable = true;
                        break;
                    }
                }
            }
            if (!useable) {
                return Messages.getString("WeaponAttackAction.BayNotReady");
            }

            // create an array of booleans of locations
            boolean[] usedFrontArc = new boolean[ae.locations()];
            boolean[] usedRearArc = new boolean[ae.locations()];
            for (int i = 0; i < ae.locations(); i++) {
                usedFrontArc[i] = false;
                usedRearArc[i] = false;
            }

            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                // Strafing attacks only count heat for first shot
                if (prevAttack.isStrafing() && !prevAttack.isStrafingFirstShot()) {
                    continue;
                }
                if ((prevAttack.getEntityId() == attackerId) && (weaponId != prevAttack.getWeaponId())) {
                    Mounted prevWeapon = ae.getEquipment(prevAttack.getWeaponId());
                    int loc = prevWeapon.getLocation();
                    boolean rearMount = prevWeapon.isRearMounted();
                    if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_HEAT_BY_BAY)) {
                        for (int bwId : prevWeapon.getBayWeapons()) {
                            totalheat += ae.getEquipment(bwId).getCurrentHeat();
                        }
                    } else {
                        if (!rearMount) {
                            if (!usedFrontArc[loc]) {
                                totalheat += ae.getHeatInArc(loc, rearMount);
                                usedFrontArc[loc] = true;
                            }
                        } else {
                            if (!usedRearArc[loc]) {
                                totalheat += ae.getHeatInArc(loc, rearMount);
                                usedRearArc[loc] = true;
                            }
                        }
                    }
                }
            }

            // now check the current heat
            int loc = weapon.getLocation();
            boolean rearMount = weapon.isRearMounted();
            int currentHeat = ae.getHeatInArc(loc, rearMount);
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_HEAT_BY_BAY)) {
                currentHeat = 0;
                for (int bwId : weapon.getBayWeapons()) {
                    currentHeat += ae.getEquipment(bwId).getCurrentHeat();
                }
            }
            // check to see if this is currently the only arc being fired
            boolean onlyArc = true;
            for (int nLoc = 0; nLoc < ae.locations(); nLoc++) {
                if (nLoc == loc) {
                    continue;
                }
                if (usedFrontArc[nLoc] || usedRearArc[nLoc]) {
                    onlyArc = false;
                    break;
                }
            }

            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_HEAT_BY_BAY)) {
                if ((totalheat + currentHeat) > heatcap) {
                    // FIXME: This is causing weird problems (try firing all the
                    // Suffen's nose weapons)
                    return Messages.getString("WeaponAttackAction.HeatOverCap");
                }
            } else {
                if (!rearMount) {
                    if (!usedFrontArc[loc] && ((totalheat + currentHeat) > heatcap) && !onlyArc) {
                        return Messages.getString("WeaponAttackAction.HeatOverCap");
                    }
                } else {
                    if (!usedRearArc[loc] && ((totalheat + currentHeat) > heatcap) && !onlyArc) {
                        return Messages.getString("WeaponAttackAction.HeatOverCap");
                    }
                }
            }
        } else if (ae instanceof Dropship) {
            int totalheat = 0;

            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                if ((prevAttack.getEntityId() == attackerId) && (weaponId != prevAttack.getWeaponId())) {
                    Mounted prevWeapon = ae.getEquipment(prevAttack.getWeaponId());
                    totalheat += prevWeapon.getCurrentHeat();
                }
            }

            if ((totalheat + weapon.getCurrentHeat()) > heatcap) {
                return Messages.getString("WeaponAttackAction.HeatOverCap");
            }
        }

        // MG arrays
        if (wtype.hasFlag(WeaponType.F_MGA) && (weapon.getCurrentShots() == 0)) {
            return Messages.getString("WeaponAttackAction.NoWorkingMGs");
        }
        if (wtype.hasFlag(WeaponType.F_MGA) && wtype.hasModes() && weapon.curMode().equals("Off")) {
            return Messages.getString("WeaponAttackAction.MGArrayOff");
        } else if (wtype.hasFlag(WeaponType.F_MG)) {
            if (ae.hasLinkedMGA(weapon)) {
                return Messages.getString("WeaponAttackAction.MGPartOfArray");
            }
        }
        // Check to see if another solo weapon was fired
        boolean hasSoloAttack = false;
        String soloWeaponName = "";
        for (EntityAction ea : game.getActionsVector()) {
            if ((ea.getEntityId() == attackerId) && (ea instanceof WeaponAttackAction)) {
                WeaponAttackAction otherWAA = (WeaponAttackAction) ea;
                final Mounted otherWeapon = ae.getEquipment(otherWAA.getWeaponId());

                if (!(otherWeapon.getType() instanceof WeaponType)) {
                    continue;
                }
                final WeaponType otherWtype = (WeaponType) otherWeapon.getType();
                hasSoloAttack |= (otherWtype.hasFlag(WeaponType.F_SOLO_ATTACK) && otherWAA.getWeaponId() != weaponId);
                if (hasSoloAttack) {
                    soloWeaponName = otherWeapon.getName();
                    break;
                }
            }
        }
        if (hasSoloAttack) {
            return String.format(Messages.getString("WeaponAttackAction.CantFireWithOtherWeapons"), soloWeaponName);
        }
        
        // Handle solo attack weapons.
        if (wtype.hasFlag(WeaponType.F_SOLO_ATTACK)) {
            for (EntityAction ea : game.getActionsVector()) {
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if (prevAttack.getEntityId() == attackerId) {
                    // If the attacker fires another weapon, this attack fails.
                    if (weaponId != prevAttack.getWeaponId()) {
                        return Messages.getString("WeaponAttackAction.CantMixAttacks");
                    }
                }
            }
        } else if (isAttackerInfantry && !(ae instanceof BattleArmor)) {
            // 0 MP infantry units: move or shoot, except for anti-mech attacks,
            // those are handled above
            if ((ae.getMovementMode() == EntityMovementMode.INF_LEG) && (ae.getWalkMP() == 0)
                    && (ae.moved != EntityMovementType.MOVE_NONE)) {
                return Messages.getString("WeaponAttackAction.0MPInf");
            }
            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_FAST_INFANTRY_MOVE)
                    && (ae.moved == EntityMovementType.MOVE_RUN)) {
                return Messages.getString("WeaponAttackAction.CantShootAndFastMove");
            }
            // check for trying to fire field gun after moving
            if ((weapon.getLocation() == Infantry.LOC_FIELD_GUNS) && (ae.moved != EntityMovementType.MOVE_NONE)) {
                return Messages.getString("WeaponAttackAction.CantMoveAndFieldGun");
            }
            // check for mixing infantry and field gun attacks
            double fieldGunWeight = 0.0;
            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                final WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if (prevAttack.getEntityId() == attackerId) {
                    Mounted prevWeapon = ae.getEquipment(prevAttack.getWeaponId());
                    if ((prevWeapon.getType().hasFlag(WeaponType.F_INFANTRY)
                            && (weapon.getLocation() == Infantry.LOC_FIELD_GUNS))
                            || (weapon.getType().hasFlag(WeaponType.F_INFANTRY)
                                    && (prevWeapon.getLocation() == Infantry.LOC_FIELD_GUNS))) {
                        return Messages.getString("WeaponAttackAction.FieldGunOrSAOnly");
                    }
                    if ((weapon.getLocation() == Infantry.LOC_FIELD_GUNS) && (weaponId != prevAttack.getWeaponId())) {
                        fieldGunWeight += prevWeapon.getType().getTonnage(ae);
                    }
                }
            }
            // the total tonnage of field guns fired has to be less than or
            // equal to the men in the platoon
            if (weapon.getLocation() == Infantry.LOC_FIELD_GUNS) {
                if (((Infantry) ae).getShootingStrength() < Math.ceil(fieldGunWeight + wtype.getTonnage(ae))) {
                    return Messages.getString("WeaponAttackAction.NoFieldGunCrew");
                }
            }
            // BA compact narc: we have one weapon for each trooper, but you
            // can fire only at one target at a time
            if (wtype.getName().equals("Compact Narc")) {
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                    EntityAction ea = i.nextElement();
                    if (!(ea instanceof WeaponAttackAction)) {
                        continue;
                    }
                    final WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                    if (prevAttack.getEntityId() == attackerId) {
                        Mounted prevWeapon = ae.getEquipment(prevAttack.getWeaponId());
                        if (prevWeapon.getType().getName().equals("Compact Narc")) {
                            if (prevAttack.getTargetId() != target.getTargetId()) {
                                return Messages.getString("WeaponAttackAction.OneTargetForCNarc");
                            }
                        }
                    }
                }
            }
        }

        // check wind conditions
        int windCond = game.getPlanetaryConditions().getWindStrength();
        if ((windCond == PlanetaryConditions.WI_TORNADO_F13) && wtype.hasFlag(WeaponType.F_MISSILE)
                && !game.getBoard().inSpace()) {
            return Messages.getString("WeaponAttackAction.NoMissileTornado");
        }

        if ((windCond == PlanetaryConditions.WI_TORNADO_F4) && !game.getBoard().inSpace()
                && (wtype.hasFlag(WeaponType.F_MISSILE) || wtype.hasFlag(WeaponType.F_BALLISTIC))) {
            return Messages.getString("WeaponAttackAction.F4Tornado");
        }

        // check if indirect fire is valid
        if (isIndirect && !game.getOptions().booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            return Messages.getString("WeaponAttackAction.IndirectFireOff");
        }

        if (isIndirect && game.getOptions().booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)
                && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_INDIRECT_ALWAYS_POSSIBLE)
                && LosEffects.calculateLos(game, attackerId, target).canSee()
                && (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                        || Compute.canSee(game, ae, target))
                && !(wtype instanceof ArtilleryCannonWeapon) && !(wtype instanceof MekMortarWeapon)) {
            return Messages.getString("WeaponAttackAction.NoIndirectWithLOS");
        }

        if (isIndirect && usesAmmo && (atype.getAmmoType() == AmmoType.T_MML) && !atype.hasFlag(AmmoType.F_MML_LRM)) {
            return Messages.getString("WeaponAttackAction.NoIndirectSRM");
        }

        // hull down vees can't fire front weapons
        if ((ae instanceof Tank) && ae.isHullDown() && (weapon.getLocation() == Tank.LOC_FRONT)) {
            return Messages.getString("WeaponAttackAction.FrontBlockedByTerrain");
        }

        // BA Micro bombs only when flying
        if ((atype != null) && (atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB)) {
            if (!ae.isAirborneVTOLorWIGE()) {
                return Messages.getString("WeaponAttackAction.MinimumAlt1");
            } else if (target.getTargetType() != Targetable.TYPE_HEX_BOMB) {
                return Messages.getString("WeaponAttackAction.BombTargetHexOnly");
            } else if (ae.getElevation() != 1) {
                return Messages.getString("WeaponAttackAction.ExactlyAlt1");
            }
        }

        if ((target.getTargetType() == Targetable.TYPE_HEX_BOMB)
                && !(usesAmmo && (atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB))) {
            return Messages.getString("WeaponAttackAction.InvalidForBombing");
        }

        if ((target.getTargetType() == Targetable.TYPE_HEX_AERO_BOMB) && !wtype.hasFlag(WeaponType.F_DIVE_BOMB)
                && !wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
            return Messages.getString("WeaponAttackAction.InvalidForBombing");
        }

        if (wtype.hasFlag(WeaponType.F_DIVE_BOMB) || wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
            if (ae.getBombs(AmmoType.F_GROUND_BOMB).isEmpty()) {
                return Messages.getString("WeaponAttackAction.OutOfBombs");
            }
            if (ae.isAero() && ((IAero) ae).isSpheroid()) {
                return Messages.getString("WeaponAttackAction.NoSpheroidBombing");
            }
            if (!ae.isAirborne() && !ae.isAirborneVTOLorWIGE()) {
                return Messages.getString("WeaponAttackAction.GroundedAeroCantBomb");
            }

            if (target.getTargetType() != Targetable.TYPE_HEX_AERO_BOMB) {
                return Messages.getString("WeaponAttackAction.BombTargetHexOnly");
            }
            if (!ae.passedOver(target)) {
                return Messages.getString("WeaponAttackAction.CantBombOffFlightPath");
            }

            if (wtype.hasFlag(WeaponType.F_DIVE_BOMB)) {
                if (ae.getAltitude() > DiveBombAttack.DIVE_BOMB_MAX_ALTITUDE) {
                    return Messages.getString("WeaponAttackAction.TooHighForDiveBomb");
                }
                if (ae.isAero()) {
                    int altLoss = ((IAero) ae).getAltLossThisRound();
                    if ((ae.getAltitude() + altLoss) < DiveBombAttack.DIVE_BOMB_MIN_ALTITUDE) {
                        return Messages.getString("WeaponAttackAction.TooLowForDiveBomb");
                    }
                }
            }
        }

        Entity spotter = null;
        if (isIndirect) {
            if ((target instanceof Entity) && !isTargetECMAffected && usesAmmo
                    && (atype.getMunitionType() == AmmoType.M_NARC_CAPABLE)
                    && ((te.isNarcedBy(ae.getOwner().getTeam())) || (te.isINarcedBy(ae.getOwner().getTeam())))) {
                spotter = te;
            } else {
                spotter = Compute.findSpotter(game, ae, target);
            }

            if ((spotter == null) && (atype != null)
                    && ((atype.getAmmoType() == AmmoType.T_LRM) 
                            || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                            || (atype.getAmmoType() == AmmoType.T_MML)
                            || (atype.getAmmoType() == AmmoType.T_NLRM)
                            || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                    && (atype.getMunitionType() == AmmoType.M_SEMIGUIDED)) {
                for (TagInfo ti : game.getTagInfo()) {
                    if (target.getTargetId() == ti.target.getTargetId()) {
                        spotter = game.getEntity(ti.attackerId);
                    }
                }
            }

            if ((spotter == null) && !(wtype instanceof MekMortarWeapon) && !(wtype instanceof ArtilleryCannonWeapon)
                    && !ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)) {
                return Messages.getString("WeaponAttackAction.NoSpotter");
            }
        }

        int eistatus = 0;

        boolean multiPurposeelevationHack = false;
        if (usesAmmo 
                && ((wtype.getAmmoType() == AmmoType.T_LRM) || (wtype.getAmmoType() == AmmoType.T_LRM_IMP)) 
                && (atype.getMunitionType() == AmmoType.M_MULTI_PURPOSE)
                && (ae.getElevation() == -1)
                && (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET)) {
            multiPurposeelevationHack = true;
            // surface to fire
            ae.setElevation(0);
        }

        // check LOS (indirect LOS is from the spotter)
        ToHitData losMods;
        if (isIndirect && ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)
                && !underWater) {
            // Assume that no LOS mods apply
            losMods = new ToHitData();
            
        } else if (!isIndirect || (isIndirect && (spotter == null))) {
            if (ae.hasActiveEiCockpit()) {
                if (los.getLightWoods() > 0) {
                    eistatus = 2;
                } else {
                    eistatus = 1;
                }
            }

            if ((wtype instanceof MekMortarWeapon) && isIndirect) {
                los.setArcedAttack(true);
            }
            
            losMods = los.losModifiers(game, eistatus, underWater);
        } else {
            if (!exchangeSwarmTarget) {
                los = LosEffects.calculateLos(game, spotter.getId(), target);
            } else {
                // Swarm should draw LoS between targets, not attacker, since
                // we don't want LoS to be blocked
                if (swarmPrimaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                    los = LosEffects.calculateLos(game, swarmPrimaryTarget.getTargetId(), swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLos(game, swarmSecondaryTarget.getTargetId(), swarmPrimaryTarget);
                }
            }

            // do not count attacker partial cover in indirect fire
            los.setAttackerCover(LosEffects.COVER_NONE);

            if (spotter.hasActiveEiCockpit()) {
                if (los.getLightWoods() > 0) {
                    eistatus = 2;
                } else {
                    eistatus = 1;
                }
            }

            if ((wtype instanceof MekMortarWeapon) && isIndirect) {
                los.setArcedAttack(true);
            }

            losMods = los.losModifiers(game, underWater);
        }

        // If the attacker and target are in the same building & hex, they can
        // always attack each other, TW pg 175.
        if ((los.getThruBldg() != null) && los.getTargetPosition().equals(ae.getPosition())) {
            return null;
        }

        if (multiPurposeelevationHack) {
            // and descend back to depth 1
            ae.setElevation(-1);
        }

        // if LOS is blocked, block the shot
        if ((losMods.getValue() == TargetRoll.IMPOSSIBLE) && !isArtilleryIndirect) {
            return losMods.getDesc();
        }
        
        //If using SO advanced sensors, the firing unit or one on its NC3 network must have a valid firing solution
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)
                && game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                && ae.isSpaceborne()) {
            boolean networkFiringSolution = false;
            //Check to see if the attacker has a firing solution. Naval C3 networks share targeting data
            if (ae.hasNavalC3()) {
                for (Entity en : game.getEntitiesVector()) {
                    if (en != ae && !en.isEnemyOf(ae) && en.onSameC3NetworkAs(ae) && ae.hasFiringSolutionFor(te.getId())) {
                        networkFiringSolution = true;
                        break;
                    }
                }
            }
            if (!networkFiringSolution) {
                //If we don't check for target type here, we can't fire screens and missiles at hexes...
                if (target.getTargetType() == Targetable.TYPE_ENTITY && (te != null && !ae.hasFiringSolutionFor(te.getId())))  {
                    return Messages.getString("WeaponAttackAction.NoFiringSolution");
                }
            }
        }

        // http://www.classicbattletech.com/forums/index.php/topic,47618.0.html
        // anything outside of visual range requires a "sensor lock" in order to
        // direct fire
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)
                && !Compute.inVisualRange(game, ae, target)
                && !(Compute.inSensorRange(game, ae, target, null) // Can shoot
                                                                   // at
                                                                   // something
                                                                   // in sensor
                                                                   // range
                        && (te != null) && te.hasSeenEntity(ae.getOwner())) // if
                                                                            // it
                                                                            // has
                                                                            // been
                                                                            // spotted
                                                                            // by
                                                                            // another
                                                                            // unit
                && !isArtilleryIndirect && !isIndirect && !isBearingsOnlyMissile) {
            boolean networkSee = false;
            if (ae.hasC3() || ae.hasC3i() || ae.hasNavalC3()|| ae.hasActiveNovaCEWS()) {
                // c3 units can fire if any other unit in their network is in
                // visual or sensor range
                for (Entity en : game.getEntitiesVector()) {
                    if (!en.isEnemyOf(ae) && en.onSameC3NetworkAs(ae) && Compute.canSee(game, en, target)) {
                        networkSee = true;
                        break;
                    }
                }
            }
            if (!networkSee) {
                if (!Compute.inSensorRange(game, ae, target, null)) {
                    return Messages.getString("WeaponAttackAction.NoSensorTarget");
                } else {
                    return Messages.getString("WeaponAttackAction.TargetNotSpotted");
                }
            }
        }

        // Weapon in arc?
        if (!Compute.isInArc(game, attackerId, weaponId, target)
                && (!Compute.isAirToGround(ae, target) || isArtilleryIndirect)
                && !ae.isMakingVTOLGroundAttack()
                && !ae.isOffBoard()) {
            return Messages.getString("WeaponAttackAction.OutOfArc");
        }

        if (Compute.isAirToGround(ae, target) && !isArtilleryIndirect && !ae.isDropping()) {
            if ((ae.getAltitude() > 5) && !wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                return Messages.getString("WeaponAttackAction.AttackerTooHigh");
            }
            if ((ae.getAltitude() > 3) && isStrafing) {
                return Messages.getString("WeaponAttackAction.AttackerTooHigh");
            }
            // Additional Nape-of-Earth restrictions for strafing
            if (ae.getAltitude() == 1 && isStrafing) {
                Vector<Coords> passedThrough = ae.getPassedThrough();
                if ((passedThrough.size() == 0) || passedThrough.get(0).equals(target.getPosition())) {
                    // TW pg 243 says units flying at NOE have a harder time
                    // establishing LoS while strafing and hence have to
                    // consider the adjacent hex along the flight place in the
                    // direction of the attack. What if there is no adjacent
                    // hex? The rules don't address this. We could
                    // theoretically consider last turns movement, but that's
                    // cumbersome, so we'll just assume it's impossible - Arlith
                    return Messages.getString("WeaponAttackAction.TooCloseForStrafe");
                }
                // Otherwise, check for a dead-zone, TW pg 243
                Coords prevCoords = ae.passedThroughPrevious(target.getPosition());
                IHex prevHex = game.getBoard().getHex(prevCoords);
                IHex currHex = game.getBoard().getHex(target.getPosition());
                int prevElev = prevHex.getLevel();
                int currElev = currHex.getLevel();
                if ((prevElev - currElev - target.relHeight()) > 2) {
                    return Messages.getString("WeaponAttackAction.DeadZone");
                }
            }

            // Only direct-fire energy weapons can strafe
            EquipmentType wt = weapon.getType();
            boolean isDirectFireEnergy = (wt.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && (wt.hasFlag(WeaponType.F_LASER) || wt.hasFlag(WeaponType.F_PPC)
                            || wt.hasFlag(WeaponType.F_PLASMA) || wt.hasFlag(WeaponType.F_PLASMA_MFUK)))
                    || wt.hasFlag(WeaponType.F_FLAMER);
            // Note: flamers are direct fire energy, but don't have the flag,
            // so they won't work with targeting computers
            boolean isEnergyBay = (wt instanceof LaserBayWeapon) || (wt instanceof PPCBayWeapon)
                    || (wt instanceof PulseLaserBayWeapon);
            if (isStrafing && !isDirectFireEnergy && !isEnergyBay) {
                return Messages.getString("WeaponAttackAction.StrafeDirectEnergyOnly");
            }

            // only certain weapons can be used for air to ground attacks
            if (ae.isAero()) {
                // Spheroids can't strafe
                if (isStrafing && ((IAero) ae).isSpheroid()) {
                    return Messages.getString("WeaponAttackAction.NoSpheroidStrafing");
                }
                if (((IAero) ae).isSpheroid()) {
                    if ((weapon.getLocation() != Aero.LOC_AFT) && !weapon.isRearMounted()) {
                        return Messages.getString("WeaponAttackAction.InvalidDSAtgArc");
                    }
                } else if (ae instanceof LandAirMech) {
                    if ((weapon.getLocation() == Mech.LOC_LLEG)
                            || (weapon.getLocation() == Mech.LOC_RLEG)
                            || weapon.isRearMounted()) {
                        return Messages.getString("WeaponAttackAction.InvalidAeroDSAtgArc");
                    }
                } else {
                    if ((weapon.getLocation() == Aero.LOC_AFT) || weapon.isRearMounted()) {
                        return Messages.getString("WeaponAttackAction.InvalidAeroDSAtgArc");
                    }
                }
            }

            // for air to ground attacks, the target's position must be within
            // the flight path, unless it is an artillery weapon in the nose.
            // http://www.classicbattletech.com/forums/index.php?topic=65110.0
            if (!ae.passedOver(target)) {
                if (!wtype.hasFlag(WeaponType.F_ARTILLERY)) {
                    return Messages.getString("WeaponAttackAction.NotOnFlightPath");
                } else if (weapon.getLocation() != Aero.LOC_NOSE) {
                    return Messages.getString("WeaponAttackAction.NotOnFlightPath");
                }
            }

            int altitudeLoss = 1;
            if (wtype.hasFlag(WeaponType.F_DIVE_BOMB)) {
                altitudeLoss = 2;
            }
            if (wtype.hasFlag(WeaponType.F_ALT_BOMB) || isStrafing) {
                altitudeLoss = 0;
            }
            int altLossThisRound = 0;
            if (ae.isAero()) {
                altLossThisRound = ((IAero) ae).getAltLossThisRound();
            }
            // you cant make attacks that would lower you to zero altitude
            if (altitudeLoss >= (ae.getAltitude() + altLossThisRound)) {
                return Messages.getString("WeaponAttackAction.TooMuchAltLoss");
            }

            // can only make a strike attack against a single target
            if (!isStrafing) {
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                    EntityAction ea = i.nextElement();
                    if (!(ea instanceof WeaponAttackAction)) {
                        continue;
                    }

                    WeaponAttackAction prevAttk = (WeaponAttackAction) ea;
                    if ((prevAttk.getEntityId() == ae.getId()) && (prevAttk.getTargetId() != target.getTargetId())
                            && !wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                        return Messages.getString("WeaponAttackAction.CantSplitFire");
                    }
                }
            }
        } else if ((ae instanceof VTOL) && isStrafing) {
            EquipmentType wt = weapon.getType();
            if (!(wt.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && (wt.hasFlag(WeaponType.F_LASER) || wt.hasFlag(WeaponType.F_PPC)
                            || wt.hasFlag(WeaponType.F_PLASMA) || wt.hasFlag(WeaponType.F_PLASMA_MFUK)))
                    || wt.hasFlag(WeaponType.F_FLAMER)) {
                return Messages.getString("WeaponAttackAction.StrafeDirectEnergyOnly");
            }
            if (weapon.getLocation() != VTOL.LOC_FRONT
                    && weapon.getLocation() != VTOL.LOC_TURRET
                    && weapon.getLocation() != VTOL.LOC_TURRET_2) {
                return Messages.getString("WeaponAttackAction.InvalidStrafingArc");
            }
                
        }
        
        // LAMs carrying certain types of bombs that require a weapon have attacks that cannot
        // be used in mech mode.
        if ((ae instanceof LandAirMech)
                && (ae.getConversionMode() == LandAirMech.CONV_MODE_MECH)
                && wtype.hasFlag(WeaponType.F_BOMB_WEAPON)
                && wtype.getAmmoType() != AmmoType.T_RL_BOMB
                && !wtype.hasFlag(WeaponType.F_TAG)) {
            return Messages.getString("WeaponAttackAction.NoBombInMechMode");
        }

        // only one ground-to-air attack allowed per turn
        // grounded spheroid dropships dont have this limitation
        if (!ae.isAirborne() && !((ae instanceof Dropship) && ((Aero) ae).isSpheroid())) {
            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if (prevAttack.getEntityId() == ae.getId()) {
                    if (prevAttack.isGroundToAir(game) && !Compute.isGroundToAir(ae, target)) {
                        return Messages.getString("WeaponAttackAction.AlreadyGtaAttack");
                    }
                    if (!prevAttack.isGroundToAir(game) && Compute.isGroundToAir(ae, target)) {
                        return Messages.getString("WeaponAttackAction.AlreadyGtgAttack");
                    }
                    if (prevAttack.isGroundToAir(game) && Compute.isGroundToAir(ae, target) && (null != te)
                            && (prevAttack.getTargetId() != te.getId())) {
                        return Messages.getString("WeaponAttackAction.OneTargetForGta");
                    }
                }
            }
        }

        // air2air and air2ground cannot be combined by any aerospace units
        if (Compute.isAirToAir(ae, target) || Compute.isAirToGround(ae, target)) {
            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if (prevAttack.getEntityId() != ae.getId()) {
                    continue;
                }
                if (Compute.isAirToAir(ae, target) && prevAttack.isAirToGround(game)) {
                    return Messages.getString("WeaponAttackAction.AlreadyAtgAttack");
                }
                if (Compute.isAirToGround(ae, target) && prevAttack.isAirToAir(game)) {
                    return Messages.getString("WeaponAttackAction.AlreadyAtaAttack");
                }
            }
        }

        if ((target.getAltitude() > 8) && Compute.isGroundToAir(ae, target)) {
            return Messages.getString("WeaponAttackAction.AeroTooHighForGta");
        }

        boolean isWeaponFieldGuns = isAttackerInfantry && (weapon.getLocation() == Infantry.LOC_FIELD_GUNS);
        if ((ae instanceof Infantry) && Compute.isGroundToAir(ae, target) && !wtype.hasFlag(WeaponType.F_INF_AA)
                && !isArtilleryFLAK // Can make GroundToAir Flak attacks)
                && !isWeaponFieldGuns) {
            return Messages.getString("WeaponAttackAction.NoInfantryGta");
        }

        // Protomech can fire MGA only into front arc, TW page 137
        if (!Compute.isInArc(ae.getPosition(), ae.getFacing(), target, Compute.ARC_FORWARD)
                && wtype.hasFlag(WeaponType.F_MGA) && (ae instanceof Protomech)) {
            return Messages.getString("WeaponAttackAction.ProtoMGAOnlyFront");
        }

        // for spheroid dropships in atmosphere (and on ground), the rules about
        // firing arcs are more complicated
        // TW errata 2.1
        if ((ae instanceof Aero) && ((Aero) ae).isSpheroid() && !game.getBoard().inSpace()) {
            int altDif = target.getAltitude() - ae.getAltitude();
            int distance = Compute.effectiveDistance(game, ae, target, false);
            if (!ae.isAirborne() && (distance == 0) && (weapon.getLocation() != Aero.LOC_AFT)) {
                return Messages.getString("WeaponAttackAction.OnlyAftAtZero");
            }
            if ((weapon.getLocation() == Aero.LOC_NOSE) && (altDif < 1)
                    && !((wtype instanceof ArtilleryWeapon) || wtype.hasFlag(WeaponType.F_ARTILLERY))) {
                return Messages.getString("WeaponAttackAction.TooLowForNose");
            }
            if ((!weapon.isRearMounted() && (weapon.getLocation() != Aero.LOC_AFT)) && (altDif < 0)
                    && !((wtype instanceof ArtilleryWeapon) || wtype.hasFlag(WeaponType.F_ARTILLERY))) {
                return Messages.getString("WeaponAttackAction.TooLowForFrontSide");
            }
            if ((weapon.getLocation() == Aero.LOC_AFT)) {
                if (ae.isAirborne() && (altDif > -1)) {
                    return Messages.getString("WeaponAttackAction.TooHighForAft");
                }
            }
            if ((weapon.isRearMounted()) && (altDif > 0)) {
                return Messages.getString("WeaponAttackAction.TooHighForAftSide");
            }
            if (Compute.inDeadZone(game, ae, target)) {
                if ((altDif > 0) && (weapon.getLocation() != Aero.LOC_NOSE)) {
                    return Messages.getString("WeaponAttackAction.OnlyNoseInDeadZone");
                }
                if ((altDif < 0) && (weapon.getLocation() != Aero.LOC_AFT)) {
                    return Messages.getString("WeaponAttackAction.OnlyAftInDeadZone");
                }
            }

        }

        // Must target infantry in buildings from the inside.
        if (targetInBuilding && (te instanceof Infantry)
                && (null == los.getThruBldg())) {
            return Messages.getString("WeaponAttackAction.CantShootThruBuilding");
        }

        if ((wtype.getAmmoType() == AmmoType.T_NARC) || (wtype.getAmmoType() == AmmoType.T_INARC)) {
            if (targetInBuilding) {
                return Messages.getString("WeaponAttackAction.NoNarcInBuilding");
            }
            if (target instanceof Infantry) {
                return Messages.getString("WeaponAttackAction.CantNarcInfantry");
            }
        }

        // attacker partial cover means no leg weapons
        if (los.isAttackerCover() && ae.locationIsLeg(weapon.getLocation()) && !underWater) {
            return Messages.getString("WeaponAttackAction.LegBlockedByTerrain");
        }

        // hull down cannot fire any leg weapons
        if (ae.isHullDown()) {
            if (((ae instanceof BipedMech)
                    && ((weapon.getLocation() == Mech.LOC_LLEG) || (weapon.getLocation() == Mech.LOC_RLEG)))
                    || ((ae instanceof QuadMech) && ((weapon.getLocation() == Mech.LOC_LLEG)
                            || (weapon.getLocation() == Mech.LOC_RLEG) || (weapon.getLocation() == Mech.LOC_LARM)
                            || (weapon.getLocation() == Mech.LOC_RARM)))) {
                return Messages.getString("WeaponAttackAction.NoLegHullDown");
            }
        }

        if (wtype.hasFlag(WeaponType.F_SPACE_BOMB)) {
            toHit = Compute.getSpaceBombBaseToHit(ae, te, game);
            // Return if the attack is impossible.
            if (TargetRoll.IMPOSSIBLE == toHit.getValue()) {
                return toHit.getDesc();
            }
        }

        // Leg attacks, Swarm attacks, and
        // Mine Launchers don't use gunnery.
        if (Infantry.LEG_ATTACK.equals(wtype.getInternalName())) {
            toHit = Compute.getLegAttackBaseToHit(ae, te, game);

            // Return if the attack is impossible.
            if (TargetRoll.IMPOSSIBLE == toHit.getValue()) {
                return toHit.getDesc();
            }
            if (!WeaponAttackAction.isOnlyAttack(game, ae, Infantry.LEG_ATTACK, te)) {
                return Messages.getString("WeaponAttackAction.LegAttackOnly");
            }
        } else if (Infantry.SWARM_MEK.equals(wtype.getInternalName())) {
            toHit = Compute.getSwarmMekBaseToHit(ae, te, game);

            // Return if the attack is impossible.
            if (TargetRoll.IMPOSSIBLE == toHit.getValue()) {
                return toHit.getDesc();
            }
            if (!WeaponAttackAction.isOnlyAttack(game, ae, Infantry.SWARM_MEK, te)) {
                return Messages.getString("WeaponAttackAction.SwarmAttackOnly");
            }
        } else if (Infantry.STOP_SWARM.equals(wtype.getInternalName())) {
            // Can't stop if we're not swarming, otherwise automatic.
            if (Entity.NONE == ae.getSwarmTargetId()) {
                return Messages.getString("WeaponAttackAction.NotSwarming");
            }
        } else if (Infantry.SWARM_WEAPON_MEK.equals(wtype.getInternalName())) {
            // Can't stop if we're not swarming, otherwise automatic.
            if (Entity.NONE == ae.getSwarmTargetId()) {
                return Messages.getString("WeaponAttackAction.NotSwarming");
            }
        } else if (BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName())) {
            // Mine launchers can not hit infantry.
            if (te instanceof Infantry) {
                return Messages.getString("WeaponAttackAction.CantShootInfantry");
            }
        }
        // Swarming infantry always hit their target, but
        // they can only target the Mek they're swarming.
        else if ((te != null) && (ae.getSwarmTargetId() == te.getId())) {
            // Only certain weapons can be used in a swarm attack.
            if (wtype.getDamage() == 0) {
                return Messages.getString("WeaponAttackAction.0DamageWeapon");
            }
            // Only certain weapons can be used in a swarm attack.
            if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                return Messages.getString("WeaponAttackAction.NoMissileWhenSwarming");
            }
            if (weapon.isBodyMounted()) {
                return Messages.getString("WeaponAttackAction.NoBodyWhenSwarming");
            }
        } else if (Entity.NONE != ae.getSwarmTargetId()) {
            return Messages.getString("WeaponAttackAction.MustTargetSwarmed");
        }

        int distance = Compute.effectiveDistance(game, ae, target);

        // Handle direct artillery attacks.
        if (isArtilleryDirect) {
            if (isCruiseMissile) {
                return Messages.getString("WeaponAttackAction.NoDirectCruiseMissile");
            }
            if (distance > Board.DEFAULT_BOARD_HEIGHT) {
                return Messages.getString("WeaponAttackAction.TooLongForDirectArty");
            }
            if (isHoming) {
                if ((te == null) || (te.getTaggedBy() == -1)) {
                    // see BMRr p77 on board arrow IV
                    return Messages.getString("WeaponAttackAction.MustTargetTagged");
                }
            }
        }
        if (isArtilleryIndirect) {
            int boardRange = (int) Math.ceil(distance / 17f);
            if (boardRange > wtype.getLongRange()) {
                return Messages.getString("WeaponAttackAction.OutOfRange");
            }
            if (((distance <= Board.DEFAULT_BOARD_HEIGHT) && !ae.isAirborne()) && !(losMods.getValue() == TargetRoll.IMPOSSIBLE)) {
                return Messages.getString("WeaponAttackAction.TooShortForIndirectArty");
            }
            if (ae.isAirborne() && (ae.getAltitude() >= 10)) {
                return Messages.getString("WeaponAttackAction.AeroTooHighForFlak");
            }
            if (isHoming) {
                if (ttype != Targetable.TYPE_HEX_ARTILLERY) {
                    return Messages.getString("WeaponAttackAction.HomingMapsheetOnly");
                }
            }
        }
        if (isBearingsOnlyMissile) {
            //this is an arbitrary number. You shouldn't ever get this message.
            if (distance > RangeType.RANGE_BEARINGS_ONLY_OUT) {
                return Messages.getString("WeaponAttackAction.OutOfRange");
            }
            if (ttype != Targetable.TYPE_HEX_ARTILLERY) {
                return Messages.getString("WeaponAttackAction.BOHexOnly");
            }
        }

        if (ae.getGrappled() != Entity.NONE) {
            int grapple = ae.getGrappled();
            if (grapple != target.getTargetId()) {
                return Messages.getString("WeaponAttackAction.MustTargetGrappled");
            }
            int loc = weapon.getLocation();
            if (((ae instanceof Mech) && (ae.getGrappleSide() == Entity.GRAPPLE_BOTH)
                    && ((loc != Mech.LOC_CT) && (loc != Mech.LOC_LT) && (loc != Mech.LOC_RT) && (loc != Mech.LOC_HEAD)))
                    || weapon.isRearMounted()) {
                return Messages.getString("WeaponAttackAction.CantFireWhileGrappled");
            }
            if ((ae instanceof Mech) && (ae.getGrappleSide() == Entity.GRAPPLE_LEFT) && (loc == Mech.LOC_LARM)) {
                return Messages.getString("WeaponAttackAction.CantShootWhileChained");
            }
            if ((ae instanceof Mech) && (ae.getGrappleSide() == Entity.GRAPPLE_RIGHT) && (loc == Mech.LOC_RARM)) {
                return Messages.getString("WeaponAttackAction.CantShootWhileChained");
            }
        }
        if ((ae.getMovementMode() == EntityMovementMode.WIGE) && (ae.getPosition() == target.getPosition())) {
            return Messages.getString("WeaponAttackAction.ZeroRangeTarget");
        }

        if ((wtype instanceof GaussWeapon) && wtype.hasModes() && weapon.curMode().equals("Powered Down")) {
            return Messages.getString("WeaponAttackAction.WeaponNotReady");
        }

        if ((target.getTargetType() == Targetable.TYPE_ENTITY) && wtype.hasFlag(WeaponType.F_MASS_DRIVER)
                && (ae instanceof SpaceStation)) {
            if (!ae.getPosition().translated(ae.getFacing(), distance).equals(target.getPosition())) {
                return Messages.getString("WeaponAttackAction.MassDriverFrontOnly");
            }
        }

        // Some Mek mortar ammo types can only be aimed at a hex
        if (weapon.getType().hasFlag(WeaponType.F_MEK_MORTAR) && (atype != null)
                && ((atype.getMunitionType() == AmmoType.M_AIRBURST) || (atype.getMunitionType() == AmmoType.M_FLARE)
                        || (atype.getMunitionType() == AmmoType.M_SMOKE))) {
            if (!(target instanceof HexTarget)) {
                return String.format(Messages.getString("WeaponAttackAction.AmmoAtHexOnly"), atype.getSubMunitionName());
            }
        }

        if (weapon.getType().hasFlag(WeaponType.F_PPC) && (weapon.getLinkedBy() != null)
                && weapon.getLinkedBy().getType().hasFlag(MiscType.F_PPC_CAPACITOR)
                && weapon.getLinkedBy().pendingMode().equals("Charge")) {
            return Messages.getString("WeaponAttackAction.PPCCharging");
        }

        toHit = Compute.getProneMods(game, ae, weaponId);
        if ((toHit != null) && toHit.getValue() == ToHitData.IMPOSSIBLE) {
            return toHit.getDesc();
        }

        return null;
    }
    
    private static String toHitIsAutomatic(IGame game, Entity ae, Entity te, Targetable target, Targetable swarmPrimaryTarget,
            Targetable swarmSecondaryTarget, Mounted weapon, Mounted ammo, AmmoType atype, WeaponType wtype, int ttype,
            LosEffects los, boolean exchangeSwarmTarget, boolean usesAmmo, boolean isTAG, boolean isInferno,
            boolean isAttackerInfantry, boolean isIndirect, int attackerId, int weaponId, boolean isArtilleryIndirect,
            boolean isArtilleryFLAK, boolean targetInBuilding, boolean isArtilleryDirect,
            boolean isTargetECMAffected, boolean isStrafing, boolean isBearingsOnlyMissile, boolean isCruiseMissile) {
        boolean isHoming = false;
        ToHitData toHit = null;
        
        return null;
    }

    /**
     * Some attacks are the only actions that a particular entity can make
     * during its turn Also, only this unit can make that particular attack.
     */
    private static boolean isOnlyAttack(IGame game, Entity attacker, String attackType, Entity target) {
        // mechs can only be the target of one leg or swarm attack
        for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements();) {
            EntityAction ea = actions.nextElement();
            if (ea instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) ea;
                Entity waaAE = waa.getEntity(game);
                if (waaAE == null) {
                    continue;
                }
                if (waaAE.equals(attacker)) {
                    // If there is an attack by this unit that is not the attack
                    // type, fail.
                    if (!waaAE.getEquipment(waa.getWeaponId()).getType().getInternalName().equals(attackType)) {
                        return false;
                    }
                }
                Targetable waaTarget = waa.getTarget(game);
                EquipmentType weapType = waaAE.getEquipment(waa.getWeaponId()).getType();
                if (weapType.getInternalName().equals(attackType) && (waaTarget != null) && waaTarget.equals(target)) {
                    if (!waaAE.equals(attacker)) {
                        // If there is an attack by another unit that has this
                        // attack type against the same target, fail.
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * @return Returns the nemesisConfused.
     */
    public boolean isNemesisConfused() {
        return nemesisConfused;
    }

    /**
     * @param nemesisConfused
     *            The nemesisConfused to set.
     */
    public void setNemesisConfused(boolean nemesisConfused) {
        this.nemesisConfused = nemesisConfused;
    }

    public boolean isSwarmingMissiles() {
        return swarmingMissiles;
    }

    public void setSwarmingMissiles(boolean swarmingMissiles) {
        this.swarmingMissiles = swarmingMissiles;
    }

    public void setOldTargetId(int id) {
        oldTargetId = id;
    }

    public int getOldTargetId() {
        return oldTargetId;
    }

    public void setOldTargetType(int t) {
        oldTargetType = t;
    }

    public int getOldTargetType() {
        return oldTargetType;
    }

    public void setOriginalTargetId(int id) {
        originalTargetId = id;
    }

    public int getOriginalTargetId() {
        return originalTargetId;
    }

    public void setOriginalTargetType(int t) {
        originalTargetType = t;
    }

    public int getOriginalTargetType() {
        return originalTargetType;
    }

    public int getSwarmMissiles() {
        return swarmMissiles;
    }

    public void setSwarmMissiles(int swarmMissiles) {
        this.swarmMissiles = swarmMissiles;
    }

    public int[] getBombPayload() {
        return bombPayload;
    }

    /**
     * 
     * @param load This is the "bomb payload". It's an array indexed by the constants declared in BombType.
     * Each element indicates how many types of that bomb should be fired.
     */
    public void setBombPayload(int[] load) {
        bombPayload = load;
    }

    public boolean isStrafing() {
        return isStrafing;
    }

    public void setStrafing(boolean isStrafing) {
        this.isStrafing = isStrafing;
    }

    public boolean isStrafingFirstShot() {
        return isStrafingFirstShot;
    }

    public void setStrafingFirstShot(boolean isStrafingFirstShot) {
        this.isStrafingFirstShot = isStrafingFirstShot;
    }

    public boolean isPointblankShot() {
        return isPointblankShot;
    }

    public void setPointblankShot(boolean isPointblankShot) {
        this.isPointblankShot = isPointblankShot;
    }

    public boolean isHomingShot() {
        return isHomingShot;
    }

    public void setHomingShot(boolean isHomingShot) {
        this.isHomingShot = isHomingShot;
    }
    
    /**
     * Needed by teleoperated missiles
     * @param velocity - an integer representing initial velocity
     */
    public void setLaunchVelocity(int velocity) {
        this.launchVelocity = velocity;
    }
    
    //This is a stub. ArtilleryAttackActions actually need to use it
    public void updateTurnsTilHit(IGame game) {        
    }
    
    /**
     * Convenience method that compiles the ToHit modifiers applicable to the weather or other special environmental
     * effects. These affect everyone on the board.
     * @param game  The current game
     * @param ae    The attacking entity
     * @param target The Targetable object being attacked
     * 
     * @param wtype The WeaponType of the weapon being used
     * @param atype The AmmoType being used for this attack
     * 
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * 
     * @param isArtilleryIndirect  flag that indicates whether this is an indirect-fire artillery attack
     */
    private static ToHitData compileEnvironmentalToHitMods(IGame game, Entity ae, Targetable target, WeaponType wtype, 
            AmmoType atype, ToHitData toHit, boolean isArtilleryIndirect) {
        // Night combat modifiers
        if (!isArtilleryIndirect) {
            toHit.append(AbstractAttackAction.nightModifiers(game, target, atype, ae, true));
        }

        TargetRoll weatherToHitMods = new TargetRoll();

        // weather mods (not in space)
        int weatherMod = game.getPlanetaryConditions().getWeatherHitPenalty(ae);
        if ((weatherMod != 0) && !game.getBoard().inSpace()) {
            weatherToHitMods.addModifier(weatherMod, game.getPlanetaryConditions().getWeatherDisplayableName());
        }

        // wind mods (not in space)
        if (!game.getBoard().inSpace()) {
            int windCond = game.getPlanetaryConditions().getWindStrength();
            if (windCond == PlanetaryConditions.WI_MOD_GALE) {
                if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    weatherToHitMods.addModifier(1, PlanetaryConditions.getWindDisplayableName(windCond));
                }
            } else if (windCond == PlanetaryConditions.WI_STRONG_GALE) {
                if (wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                    weatherToHitMods.addModifier(1, PlanetaryConditions.getWindDisplayableName(windCond));
                } else if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    weatherToHitMods.addModifier(2, PlanetaryConditions.getWindDisplayableName(windCond));
                }
            } else if (windCond == PlanetaryConditions.WI_STORM) {
                if (wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                    weatherToHitMods.addModifier(2, PlanetaryConditions.getWindDisplayableName(windCond));
                } else if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    weatherToHitMods.addModifier(3, PlanetaryConditions.getWindDisplayableName(windCond));
                }
            } else if (windCond == PlanetaryConditions.WI_TORNADO_F13) {
                if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                    weatherToHitMods.addModifier(2, PlanetaryConditions.getWindDisplayableName(windCond));
                } else {
                    weatherToHitMods.addModifier(3, PlanetaryConditions.getWindDisplayableName(windCond));
                }
            } else if (windCond == PlanetaryConditions.WI_TORNADO_F4) {
                weatherToHitMods.addModifier(3, PlanetaryConditions.getWindDisplayableName(windCond));
            }
        }

        // fog mods (not in space)
        if (wtype.hasFlag(WeaponType.F_ENERGY) && !game.getBoard().inSpace()
                && (game.getPlanetaryConditions().getFog() == PlanetaryConditions.FOG_HEAVY)) {
            weatherToHitMods.addModifier(1, Messages.getString("WeaponAttackAction.HeavyFog"));
        }

        // blowing sand mods
        if (wtype.hasFlag(WeaponType.F_ENERGY) && !game.getBoard().inSpace()
                && game.getPlanetaryConditions().isSandBlowing()
                && (game.getPlanetaryConditions().getWindStrength() > PlanetaryConditions.WI_LIGHT_GALE)) {
            weatherToHitMods.addModifier(1, Messages.getString("WeaponAttackAction.BlowingSand"));
        }

        if (weatherToHitMods.getValue() > 0) {
            if ((ae.getCrew() != null) && ae.hasAbility(OptionsConstants.UNOFF_WEATHERED)) {
                weatherToHitMods.addModifier(-1, Messages.getString("WeaponAttackAction.Weathered"));
            }
            toHit.append(weatherToHitMods);
        }

        // gravity mods (not in space)
        if (!game.getBoard().inSpace()) {
            int mod = (int) Math.floor(Math.abs((game.getPlanetaryConditions().getGravity() - 1.0f) / 0.2f));
            if ((mod != 0) && (wtype.hasFlag(WeaponType.F_BALLISTIC) || wtype.hasFlag(WeaponType.F_MISSILE))) {
                toHit.addModifier(mod, Messages.getString("WeaponAttackAction.Gravity"));
            }
        }

        // Electro-Magnetic Interference
        if (game.getPlanetaryConditions().hasEMI() && !((ae instanceof Infantry) && !(ae instanceof BattleArmor))) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.EMI"));
        }
        return toHit;
    }
    
    /**
     * Convenience method that compiles the ToHit modifiers applicable to the weapon being fired
     * Got a heavy large laser that gets a +1 TH penalty?  You'll find that here.
     * Bonuses related to the attacker's condition?  Ammunition being used?  Those are in other methods.
     * 
     * @param ae    The attacking entity
     * 
     * @param weapon The Mounted weapon being used for this attack
     * @param wtype The WeaponType of the weapon being used
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param toHit The running total ToHitData for this WeaponAttackAction
     */
    private static ToHitData compileWeaponToHitMods(Entity ae, Mounted weapon, WeaponType wtype, Targetable target,
            int ttype, ToHitData toHit) {
        if (ae == null || wtype == null || weapon == null) {
            // Can't calculate weapon mods without a valid weapon and an attacker to fire it
            return toHit;
        }
        Entity te = null;
        if (target != null && ttype == Targetable.TYPE_ENTITY) {
            //Some of these weapons only target valid entities
            te = (Entity) target;
        }
        
        // +4 for trying to fire ASEW or antiship missile at a target of < 500 tons
        if ((wtype.hasFlag(WeaponType.F_ANTI_SHIP) || wtype.getAmmoType() == AmmoType.T_ASEW_MISSILE)
                && (te != null) && (te.getWeight() < 500)) {
            toHit.addModifier(4, Messages.getString("WeaponAttackAction.TeTooSmallForASM"));
        }
        
        // AAA mode makes targeting large craft more difficult
        if (wtype.hasModes() && weapon.curMode().equals("AAA") && te.isLargeCraft()) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AAALaserAtShip"));
        }

        // Bracketing modes
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 80%")) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.Bracket80"));
        }
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 60%")) {
            toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Bracket60"));
        }
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 40%")) {
            toHit.addModifier(-3, Messages.getString("WeaponAttackAction.Bracket40"));
        }
        
        // Capital ship mass driver penalty. YOU try hitting a maneuvering target with a spinal-mount weapon!
        if (wtype.hasFlag(WeaponType.F_MASS_DRIVER)) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.MassDriver"));
        }
        
        // Capital missiles in waypoint launch mode
        if (weapon.isInWaypointLaunchMode()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.WaypointLaunch"));
        }
        
        // Capital weapon (except missiles) penalties at small targets
        if (wtype.isCapital() && (wtype.getAtClass() != WeaponType.CLASS_CAPITAL_MISSILE)
                && (wtype.getAtClass() != WeaponType.CLASS_AR10) && !te.isLargeCraft()) {
            // check to see if we are using AAA mode
            int aaaMod = 0;
            if (wtype.hasModes() && weapon.curMode().equals("AAA")) {
                aaaMod = 2;
            }
            if (wtype.isSubCapital()) {
                toHit.addModifier(3 - aaaMod, Messages.getString("WeaponAttackAction.SubCapSmallTe"));
            } else {
                toHit.addModifier(5 - aaaMod, Messages.getString("WeaponAttackAction.CapSmallTe"));
            }
        }
        
        // quirks
        
        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_POS_ACCURATE)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.AccWeapon"));
        }

        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE)) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.InAccWeapon"));
        }

        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_POS_STABLE_WEAPON) && (ae.moved == EntityMovementType.MOVE_RUN)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.StableWeapon"));
        }
        
        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_MISREPAIRED)) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.MisrepairedWeapon"));
        }
        
        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_MISREPLACED)) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.MisreplacedWeapon"));
        }
        return toHit;
    }
    
    /**
     * Convenience method that compiles the ToHit modifiers applicable to the ammunition being used
     * Using precision AC rounds that get a -1 TH bonus?  You'll find that here.
     * Bonuses related to the attacker's condition?  Using a weapon with a TH penalty?  Those are in other methods.
     * 
     * @param game The current game
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * 
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param atype The AmmoType being used for this attack
     * @param munition  Long indicating the munition type flag being used, if applicable
     * 
     * @param isECMAffected flag that indicates whether the target is inside an ECM bubble
     */
    private ToHitData compileAmmoToHitMods(IGame game, Entity ae, Targetable target, int ttype, ToHitData toHit,
            WeaponType wtype, Mounted weapon, AmmoType atype, long munition, boolean isECMAffected) {
        if (ae == null || atype == null || weapon == null) {
            // Can't calculate ammo mods without valid ammo and an attacker to fire it
            return toHit;
        }
        Entity te = null;
        if (target != null && ttype == Targetable.TYPE_ENTITY) {
            //Some ammo can only target valid entities
            te = (Entity) target;
        }
        // Do we use Listen-Kill ammo from War of 3039 sourcebook?
        if (!isECMAffected && (atype != null)
                && ((atype.getAmmoType() == AmmoType.T_LRM) 
                        || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                        || (atype.getAmmoType() == AmmoType.T_MML)
                        || (atype.getAmmoType() == AmmoType.T_SRM)
                        || (atype.getAmmoType() == AmmoType.T_SRM_IMP))
                && (munition == AmmoType.M_LISTEN_KILL) && !((te != null) && te.isClan())) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ListenKill"));
        }
        return toHit;
    }
    
    /**
     * Convenience method that compiles the ToHit modifiers applicable to the attacker's condition
     * Attacker has damaged sensors?  You'll find that here.
     * Defender's a superheavy mech?  Using a weapon with a TH penalty?  Those are in other methods.
     * 
     * @param game The current game
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * 
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param atype The AmmoType being used for this attack
     * @param munition  Long indicating the munition type flag being used, if applicable
     * 
     * @param isArtilleryDirect  flag that indicates whether this is a direct-fire artillery attack
     * @param isArtilleryIndirect  flag that indicates whether this is an indirect-fire artillery attack
     * @param isIndirect  flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param usesAmmo  flag that indicates whether or not the WeaponType being used is ammo-fed
     */
    placeholder
    private static ToHitData compileAttackerToHitMods(IGame game, Entity ae, Targetable target, ToHitData toHit,
            WeaponType wtype, Mounted weapon, AmmoType atype, long munition,
            boolean isArtilleryDirect, boolean isArtilleryIndirect, boolean isIndirect, boolean usesAmmo) {
        // Modifiers related to an action the attacker is taking
        
        // Quadvee converting to a new mode
        if (ae instanceof QuadVee && ae.isConvertingNow()) {
            toHit.addModifier(+3, Messages.getString("WeaponAttackAction.QuadVeeConverting"));
        }
        
        // If we're spotting for indirect fire, add +1 (no penalty with second pilot in command console)
        if (ae.isSpotting() && !ae.getCrew().hasActiveCommandConsole()) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeSpotting"));
        }
        
        // Per SO p22, dropping units get hit with a +2 dropping penalty AND the +3 Jumping penalty 
        if (ae.isAirborne() && !ae.isAero()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.Dropping"));
            toHit.addModifier(+3, Messages.getString("WeaponAttackAction.Jumping"));
        }
        
        // Special effects (like tasers) affecting the attacker
        
        // If a unit is suffering from electromagnetic interference, they get a
        // blanket +2.
        // Sucks to be them.
        if (ae.isSufferingEMI()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.EMI"));
        }
        
        // Ghost target modifier
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_GHOST_TARGET) && !isIndirect
                && !isArtilleryIndirect && !isArtilleryDirect) {
            int ghostTargetMod = Compute.getGhostTargetNumber(ae, ae.getPosition(), target.getPosition());
            if ((ghostTargetMod > -1) && !((ae instanceof Infantry) && !(ae instanceof BattleArmor))) {
                int bapMod = 0;
                if (ae.hasBAP()) {
                    bapMod = 1;
                }
                int tcMod = 0;
                if (ae.hasTargComp() && wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && !wtype.hasFlag(WeaponType.F_CWS)
                        && !wtype.hasFlag(WeaponType.F_TASER) && (atype != null)
                        && (!usesAmmo || !(((atype.getAmmoType() == AmmoType.T_AC_LBX)
                                || (atype.getAmmoType() == AmmoType.T_AC_LBX_THB))
                                && (munition == AmmoType.M_CLUSTER)))) {
                    tcMod = 2;
                }
                int ghostTargetMoF = (ae.getCrew().getSensorOps() + ghostTargetMod)
                        - (ae.getGhostTargetOverride() + bapMod + tcMod);
                if (ghostTargetMoF > 1) {
                    // according to this rules clarification the +4 max is on
                    // the PSR not on the to-hit roll
                    // http://www.classicbattletech.com/forums/index.php?topic=66036.0
                    // unofficial rule to cap the ghost target to-hit penalty
                    int mod = ghostTargetMoF / 2;
                    if (game.getOptions().intOption(OptionsConstants.ADVANCED_GHOST_TARGET_MAX) > 0) {
                        mod = Math.min(mod, game.getOptions().intOption(OptionsConstants.ADVANCED_GHOST_TARGET_MAX));
                    }
                    toHit.addModifier(mod, Messages.getString("WeaponAttackAction.GhostTargets"));
                }
            }
        }
        
        // Special Equipment that that attacker possesses
        
        // Attacker has an AES system
        if (ae.hasFunctionalArmAES(weapon.getLocation()) && !weapon.isSplit()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.AES"));
        }
        
        // industrial cockpit: +1 to hit
        if ((ae instanceof Mech) && (((Mech) ae).getCockpitType() == Mech.COCKPIT_INDUSTRIAL)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.IndustrialNoAfc"));
        }
        // primitive industrial cockpit: +2 to hit
        if ((ae instanceof Mech) && (((Mech) ae).getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.PrimIndustrialNoAfc"));
        }

        // primitive industrial cockpit with advanced firing control: +1 to hit
        if ((ae instanceof Mech) && (((Mech) ae).getCockpitType() == Mech.COCKPIT_PRIMITIVE)
                && ((Mech) ae).isIndustrial()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.PrimIndustrialAfc"));
        }
        
        // Support vehicle basic/advanced fire control systems
        if ((ae instanceof SupportTank) || (ae instanceof SupportVTOL)) {
            if (!ae.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)
                    && !ae.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.SupVeeNoFc"));
            } else if (ae.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)
                    && !(ae.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL))) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.SupVeeBfc"));
            }
        }
        
        // Is the attacker hindered by a shield?
        if (ae.hasShield()) {
            // active shield has already been checked as it makes shots
            // impossible
            // time to check passive defense and no defense

            if (ae.hasPassiveShield(weapon.getLocation(), weapon.isRearMounted())) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.PassiveShield"));
            } else if (ae.hasNoDefenseShield(weapon.getLocation())) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Shield"));
            }
        }
        
        //Critical damage effects
        
        // Vehicle criticals
        if (ae instanceof Tank) {
            Tank tank = (Tank) ae;
            int sensors = tank.getSensorHits();
            if (sensors > 0) {
                toHit.addModifier(sensors, Messages.getString("WeaponAttackAction.SensorDamage"));
            }
            if (tank.isStabiliserHit(weapon.getLocation())) {
                toHit.addModifier(Compute.getAttackerMovementModifier(game, tank.getId()).getValue(),
                        "stabiliser damage");
            }
        }
        
        // Quirks
        
        // Anti-air targeting quirk vs airborne unit
        if (ae.hasQuirk(OptionsConstants.QUIRK_POS_ANTI_AIR) && (target instanceof Entity)) {
            if (target.isAirborneVTOLorWIGE() || target.isAirborne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.AaVsAir"));
            }
        }
        
        // Sensor ghosts quirk
        if (ae.hasQuirk(OptionsConstants.QUIRK_NEG_SENSOR_GHOSTS)) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorGhosts"));
        }

        return toHit;
    }
    
    /**
     * Convenience method that compiles the ToHit modifiers applicable to the attacker's condition, 
     * if the attacker is an aero
     * Attacker has damaged sensors?  You'll find that here.
     * Defender's a superheavy mech?  Using a weapon with a TH penalty?  Those are in other methods.
     * 
     * @param game The current game
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * 
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param munition  Long indicating the munition type flag being used, if applicable
     * 
     * @param isArtilleryIndirect  flag that indicates whether this is an indirect-fire artillery attack
     */
    private static ToHitData compileAeroAttackerToHitMods(IGame game, Entity ae, Targetable target, ToHitData toHit,
            WeaponType wtype, Mounted weapon, long munition, boolean isArtilleryIndirect) {
        if (ae == null || target == null) {
            //Null guard
            return toHit;
        }
        // Modifiers for aero units, including fighter LAMs
        if (ae instanceof IAero) {
            IAero aero = (IAero) ae;
        
            // pilot hits
            int pilothits = ae.getCrew().getHits();
            if ((pilothits > 0) && !ae.isCapitalFighter()) {
                toHit.addModifier(pilothits, Messages.getString("WeaponAttackAction.PilotHits"));
            }

            // check for heavy gauss rifle on fighter of small craft
            // Arguably a weapon effect, except that it only applies when used by a fighter (isn't recoil fun?)
            // So it's here instead of with other weapon mods that apply across the board
            if ((wtype instanceof ISHGaussRifle) && !(ae instanceof Dropship)
                    && !(ae instanceof Jumpship)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.WeaponAttackAction.FighterHeavyGauss"));
            }
            
            // Space ECM
            if (game.getBoard().inSpace() && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
                int ecm = ComputeECM.getLargeCraftECM(ae, ae.getPosition(), target.getPosition());
                if (!ae.isLargeCraft()) {
                    ecm += ComputeECM.getSmallCraftECM(ae, ae.getPosition(), target.getPosition());
                }
                ecm = Math.min(4, ecm);
                int eccm = 0;
                if (ae.isLargeCraft()) {
                    eccm = ((Aero) ae).getECCMBonus();
                }
                if (ecm > 0) {
                    toHit.addModifier(ecm, Messages.getString("WeaponAttackAction.ECM"));
                    if (eccm > 0) {
                        toHit.addModifier(-1 * Math.min(ecm, eccm), Messages.getString("WeaponAttackAction.ECCM"));
                    }
                }
            }

            // +4 attack penalty for locations hit by ASEW missiles
            if (ae instanceof Dropship) {
                Dropship d = (Dropship) ae;
                int loc = weapon.getLocation();
                if (d.getASEWAffected(loc) > 0) {
                    toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeArcAsewAffected"));
                }            
            } else if (ae instanceof Jumpship) {
                Jumpship j = (Jumpship) ae;
                int loc = weapon.getLocation();
                if (j.getASEWAffected(loc) > 0) {
                    toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeArcAsewAffected"));
                } 
            } else {
                if (ae.getASEWAffected() > 0) {
                    toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeAsewAffected"));
                }
            }
            // Altitude-related mods for air-to-air combat
            if (Compute.isAirToAir(ae, target)) {
                if (target.isAirborneVTOLorWIGE()) {
                    toHit.addModifier(+5, Messages.getString("WeaponAttackAction.TeNonAeroAirborne"));
                }
                if (ae.isNOE()) {
                    if (ae.isOmni()) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeOmniNoe"));
                    } else {
                        toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeNoe"));
                    }
                }
            }
            // grounded aero
            if (!ae.isAirborne() && !ae.isSpaceborne()) {
                if (!(ae instanceof Dropship)) {
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.GroundedAero"));
                } else if (!target.isAirborne() && !isArtilleryIndirect) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GroundedDs"));
                }
            }
            // out of control
            if (aero.isOutControlTotal()) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeroOoc"));
            }
        }
        // Situational modifiers for aero units, not including LAMs.
        if (ae instanceof Aero) {
            Aero aero = (Aero) ae;

            // sensor hits
            int sensors = aero.getSensorHits();

            if (!ae.isCapitalFighter()) {
                if ((sensors > 0) && (sensors < 3)) {
                    toHit.addModifier(sensors, Messages.getString("WeaponAttackAction.SensorDamage"));
                }
                if (sensors > 2) {
                    toHit.addModifier(+5, Messages.getString("WeaponAttackAction.SensorDestroyed"));
                }
            }

            // FCS hits
            int fcs = aero.getFCSHits();

            if ((fcs > 0) && !aero.isCapitalFighter()) {
                toHit.addModifier(fcs * 2, Messages.getString("WeaponAttackAction.FcsDamage"));
            }
            
            // CIC hits
            if (aero instanceof Jumpship) {
                Jumpship js = (Jumpship) aero;
                int cic = js.getCICHits();
                if (cic > 0) {
                    toHit.addModifier(cic * 2, Messages.getString("WeaponAttackAction.CicDamage"));
                }
            }

            // targeting mods for evasive action by large craft
            // Per TW, this does not apply when firing Capital Missiles
            if (aero.isEvading() &&
                    (!(wtype.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE
                            || wtype.getAtClass() == WeaponType.CLASS_AR10
                            || wtype.getAtClass() == WeaponType.CLASS_TELE_MISSILE))) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeEvading"));
            }

            // check for particular kinds of weapons in weapon bays
            if (ae.usesWeaponBays()) {

                // any heavy lasers
                if (wtype.getAtClass() == WeaponType.CLASS_LASER) {
                    for (int wId : weapon.getBayWeapons()) {
                        Mounted bweap = ae.getEquipment(wId);
                        WeaponType bwtype = (WeaponType) bweap.getType();
                        if ((bwtype.getInternalName().contains("Heavy"))
                                && (bwtype.getInternalName().contains("Laser"))) {
                            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.HeavyLaserInBay"));
                            break;
                        }
                    }
                }
                // barracuda missiles
                else if (wtype.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE) {
                    boolean onlyBarracuda = true;
                    for (int wId : weapon.getBayWeapons()) {
                        Mounted bweap = ae.getEquipment(wId);
                        Mounted bammo = bweap.getLinked();
                        if (bammo != null) {
                            AmmoType batype = (AmmoType) bammo.getType();
                            if (batype.getAmmoType() != AmmoType.T_BARRACUDA) {
                                onlyBarracuda = false;
                            }
                        }
                    }
                    if (onlyBarracuda) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Barracuda"));
                    }
                }
                // barracuda missiles in an AR10 launcher (must all be
                // barracuda)
                else if (wtype.getAtClass() == WeaponType.CLASS_AR10) {
                    boolean onlyBarracuda = true;
                    for (int wId : weapon.getBayWeapons()) {
                        Mounted bweap = ae.getEquipment(wId);
                        Mounted bammo = bweap.getLinked();
                        if (bammo != null) {
                            AmmoType batype = (AmmoType) bammo.getType();
                            if (!batype.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                                onlyBarracuda = false;
                            }
                        }
                    }
                    if (onlyBarracuda) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Barracuda"));
                    }
                }
                // LBX cluster
                else if (wtype.getAtClass() == WeaponType.CLASS_LBX_AC) {
                    boolean onlyCluster = true;
                    for (int wId : weapon.getBayWeapons()) {
                        Mounted bweap = ae.getEquipment(wId);
                        Mounted bammo = bweap.getLinked();
                        if (bammo != null) {
                            AmmoType batype = (AmmoType) bammo.getType();
                            if (batype.getMunitionType() != AmmoType.M_CLUSTER) {
                                onlyCluster = false;
                                break;
                            }
                        }
                    }
                    if (onlyCluster) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ClusterAmmo"));
                    }
                }
            }
        }
        return toHit;
    }
    
    /**
     * Convenience method that compiles the ToHit modifiers applicable to the attacker's crew/pilot
     * Pilot wounded?  Has an SPA?  You'll find that here.
     * Defender's a superheavy mech?  Using a weapon with a TH penalty?  Those are in other methods.
     * 
     * @param game The current game
     * @param ae The Entity making this attack
     * @param te The target Entity
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * 
     * @param wtype The WeaponType of the weapon being used
     * 
     */
    private static ToHitData compileCrewToHitMods(IGame game, Entity ae, Entity te, ToHitData toHit, WeaponType wtype) {
        //Now for modifiers affecting the attacker's crew
        
        // Bonuses for dual cockpits, etc
        //Bonus to gunnery if both crew members are active; a pilot who takes the gunner's role get +1.
        if (ae instanceof Mech && ((Mech)ae).getCockpitType() == Mech.COCKPIT_DUAL) {
            if (!ae.getCrew().isActive(ae.getCrew().getCrewType().getGunnerPos())) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.GunnerHit"));                
            } else if (ae.getCrew().hasDedicatedGunner()) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.DualCockpit"));
            }
        }

        //The pilot or technical officer can take over the gunner's duties but suffers a +2 penalty.
        if ((ae instanceof TripodMech || ae instanceof QuadVee) && !ae.getCrew().hasDedicatedGunner()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.GunnerHit"));
        }
        
        // fatigue
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_FATIGUE)
                && ae.getCrew().isGunneryFatigued()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.Fatigue"));
        }
        
        // Manei Domini Upgrades
        
        // VDNI
        if (ae.hasAbility(OptionsConstants.MD_VDNI)
                || ae.hasAbility(OptionsConstants.MD_BVDNI)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.Vdni"));
        }

        if ((ae instanceof Infantry) && !(ae instanceof BattleArmor)) {
            // check for pl-masc
            // the rules are a bit vague, but assume that if the infantry didn't
            // move or jumped, then they shouldn't get the penalty
            if (ae.hasAbility(OptionsConstants.MD_PL_MASC)
                    && ((ae.moved == EntityMovementType.MOVE_WALK) || (ae.moved == EntityMovementType.MOVE_RUN))) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.PlMasc"));
            }

            // check for cyber eye laser sighting on ranged attacks
            if (ae.hasAbility(OptionsConstants.MD_CYBER_IMP_LASER)
                    && !(wtype instanceof InfantryAttack)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.MdEye"));
            }
        }
        
        // SPAs
        
        // Unofficial weapon class specialist - Does not have an unspecialized penalty 
        if (ae.hasAbility(OptionsConstants.UNOFF_GUNNERY_LASER)
                && wtype.hasFlag(WeaponType.F_ENERGY)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GunESkill"));
        }

        if (ae.hasAbility(OptionsConstants.UNOFF_GUNNERY_BALLISTIC)
                && wtype.hasFlag(WeaponType.F_BALLISTIC)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GunBSkill"));
        }

        if (ae.hasAbility(OptionsConstants.UNOFF_GUNNERY_MISSILE)
                && wtype.hasFlag(WeaponType.F_MISSILE)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GunMSkill"));
        }

        // Is the pilot a weapon specialist?
        if (ae.hasAbility(OptionsConstants.GUNNERY_WEAPON_SPECIALIST, wtype.getName())) {
            toHit.addModifier(-2, Messages.getString("WeaponAttackAction.WeaponSpec"));
        } else if (ae.hasAbility(OptionsConstants.GUNNERY_SPECIALIST)) {
            // aToW style gunnery specialist: -1 to specialized weapon and +1 to
            // all other weapons
            // Note that weapon specialist supersedes gunnery specialization, so
            // if you have
            // a specialization in Medium Lasers and a Laser specialization, you
            // only get the -2 specialization mod
            if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                if (ae.hasAbility(OptionsConstants.GUNNERY_SPECIALIST, Crew.SPECIAL_ENERGY)) {
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.EnergySpec"));
                } else {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Unspec"));
                }
            } else if (wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                if (ae.hasAbility(OptionsConstants.GUNNERY_SPECIALIST, Crew.SPECIAL_BALLISTIC)) {
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BallisticSpec"));
                } else {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Unspec"));
                }
            } else if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                if (ae.hasAbility(OptionsConstants.GUNNERY_SPECIALIST, Crew.SPECIAL_MISSILE)) {
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.MissileSpec"));
                } else {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Unspec"));
                }
            }
        }
        
        // Target SPAs
        if (te != null) {
            // Shaky Stick -  Target gets a +1 bonus against Ground-to-Air attacks
            if (te.hasAbility(OptionsConstants.PILOT_SHAKY_STICK) && te.isAirborne()
                    && !ae.isAirborne() && !ae.isAirborneVTOLorWIGE()) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.ShakyStick"));
            }
            // Urban Guerrilla - Target gets a +1 bonus in any sort of urban terrain
            if (te.hasAbility(OptionsConstants.INFANTRY_URBAN_GUERRILLA)
                    && (game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.PAVEMENT)
                            || game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.ROAD)
                            || game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.RUBBLE)
                            || game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.BUILDING)
                            || game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.ROUGH))) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.UrbanGuerilla"));
            }
            // Forest Ranger - Target gets a +1 bonus in wooded terrain when moving at walking speed or greater
            if (te.hasAbility(OptionsConstants.PILOT_TM_FOREST_RANGER)
                    && (game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.WOODS)
                       || game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.JUNGLE))
                    && te.moved == EntityMovementType.MOVE_WALK) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.ForestRanger"));
            }
            // Swamp Beast - Target gets a +1 bonus in mud/swamp terrain when running/flanking
            if (te.hasAbility(OptionsConstants.PILOT_TM_SWAMP_BEAST)
                    && (game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.MUD)
                        || game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.SWAMP))
                    && te.moved == EntityMovementType.MOVE_RUN) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SwampBeast"));
            }
        }
        
        // Vehicle crew hits
        if (ae instanceof Tank) {
            Tank tank = (Tank) ae;
            if (tank.isCommanderHit()) {
                if (ae instanceof VTOL) {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.CopilotHit"));
                } else {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.CmdrHit"));
                }
            }
        }
    }
    
    /**
     * Convenience method that compiles the ToHit modifiers applicable to the defender's condition
     * -4 for shooting at an immobile target?  You'll find that here.
     * Attacker strafing?  Using a weapon with a TH penalty?  Those are in other methods.
     * 
     * @param game The current game
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param los The calculated LOS between attacker and target
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * 
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param atype The AmmoType being used for this attack
     * @param munition  Long indicating the munition type flag being used, if applicable
     * 
     * @param isArtilleryDirect  flag that indicates whether this is a direct-fire artillery attack
     * @param isArtilleryIndirect  flag that indicates whether this is an indirect-fire artillery attack
     * @param isIndirect  flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param usesAmmo  flag that indicates whether or not the WeaponType being used is ammo-fed
     */
    private ToHitData compileTargetToHitMods(IGame game, Entity ae, Targetable target, int ttype, LosEffects los,
            ToHitData toHit, WeaponType wtype, AmmoType atype, boolean isIndirect) {
        if (ae == null || target == null) {
            // Can't handle these attacks without a valid attacker and target
            return toHit;
        }
        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            //Some of these weapons only target valid entities
            te = (Entity) target;
        }
        
        // Modifiers related to a special action the target is taking
        
        // evading bonuses
        if ((te != null) && te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), Messages.getString("WeaponAttackAction.TeEvading"));
        }
        
        // Special effects (like Heat) affecting the target
        
        // Special Equipment that that target possesses
        
        // Standard Movement and Position modifiers
        
        // Add range mods - If the attacker and target are in the same building
        // & hex, range mods don't apply (and will cause the shot to fail)
        if ((los.getThruBldg() == null) || !los.getTargetPosition().equals(ae.getPosition())) {
            toHit.append(Compute.getRangeMods(game, ae, weaponId, target));
        }
        
        // Ground-to-air attacks against a target flying at NOE
        if (Compute.isGroundToAir(ae, target) && (null != te) && te.isNOE()) {
            if (te.passedWithin(ae.getPosition(), 1)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.TeNoe"));
            } else {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.TeNoe"));
            }
        }

        // Ground-to-air attacks against a target flying at any other altitude (if StratOps Velocity mods are on)
        if (Compute.isGroundToAir(ae, target)
                && game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_AA_FIRE) && (null != te)
                && (te.isAero())) {
            int vMod = ((IAero) te).getCurrentVelocity();
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AA_MOVE_MOD)) {
                vMod = Math.min(vMod / 2, 4);
            }
            toHit.addModifier(vMod, Messages.getString("WeaponAttackAction.TeVelocity"));
        }
        
        // Terrain and Line of Sight
        
        // if we have BAP and there are woods in the
        // way, and we are within BAP range, we reduce the BTH by 1
        // Per TacOps errata, this bonus also applies to all units on the same C3 network
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_BAP) && !isIndirect && (te != null)
                && ae.hasBAP() && (ae.getBAPRange() >= Compute.effectiveDistance(game, ae, te))
                && !ComputeECM.isAffectedByECM(ae, ae.getPosition(), te.getPosition())
                && (game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.WOODS)
                        || game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.JUNGLE)
                        || (los.getLightWoods() > 0) || (los.getHeavyWoods() > 0) || (los.getUltraWoods() > 0))) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BAPInWoods"));
        }
        
        // Unit-specific modifiers
        
        // -1 to hit a SuperHeavy mech
        if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMech"));
        }
        
        // Aerospace target modifiers
        if (te != null && te.isAero() && te.isAirborne()) {
            IAero a = (IAero) te;

            // is the target at zero velocity
            if ((a.getCurrentVelocity() == 0) && !(a.isSpheroid() && !game.getBoard().inSpace())) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.ImmobileAero"));
            }

            // Target hidden in the sensor shadow of a larger spacecraft
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SENSOR_SHADOW)
                    && game.getBoard().inSpace()) {
                for (Entity en : Compute.getAdjacentEntitiesAlongAttack(ae.getPosition(), target.getPosition(), game)) {
                    if (!en.isEnemyOf(te) && en.isLargeCraft() && ((en.getWeight() - te.getWeight()) >= -STRATOPS_SENSOR_SHADOW_WEIGHT_DIFF)) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorShadow"));
                        break;
                    }
                }
                for (Entity en : game.getEntitiesVector(target.getPosition())) {
                    if (!en.isEnemyOf(te) && en.isLargeCraft() && !en.equals((Entity) a)
                            && ((en.getWeight() - te.getWeight()) >= -STRATOPS_SENSOR_SHADOW_WEIGHT_DIFF)) {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorShadow"));
                        break;
                    }
                }
            }
        }
        
        return toHit;
    }
    
    /**
     * If you're using a weapon that does something totally special and doesn't apply mods like everything else, look here
     * 
     * @param game The current game
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param los The calculated LOS between attacker and target
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * 
     * @param wtype The WeaponType of the weapon being used
     * @param atype The AmmoType being used for this attack
     */
    private static ToHitData handleSpecialWeaponAttacks(IGame game, Entity ae,
            Targetable target, int ttype, LosEffects los, ToHitData toHit, WeaponType wtype, AmmoType atype) {
        setSpecialResolution(false);
        if (ae == null) {
            //*Should* be impossible at this point in the process
            return toHit;
        }
        
        Entity te = null;
        if (target != null && ttype == Targetable.TYPE_ENTITY) {
            //Some of these weapons only target valid entities
            te = (Entity) target;
        }
        
        // Battle Armor bomb racks (Micro bombs) use gunnery skill and no other mods per TWp228 2018 errata
        if ((atype != null) && (atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB)) {
            if (ae.getPosition().equals(target.getPosition())) {
                toHit = new ToHitData(ae.getCrew().getPiloting(), Messages.getString("WeaponAttackAction.GunSkill"));
            } else { 
                toHit = new ToHitData(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.OutOfRange"));
            }
            setSpecialResolution(true);
            return toHit;
        }
        
        // Engineer's fire extinguisher has fixed to hit number,
        // Note that coolant trucks make a regular attack.
        if (wtype.hasFlag(WeaponType.F_EXTINGUISHER)) {
            toHit = new ToHitData(8, Messages.getString("WeaponAttackAction.FireExt"));
            if (((target instanceof Entity) && ((Entity) target).infernos.isStillBurning())
                    || ((target instanceof Tank) && ((Tank) target).isInfernoFire())) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.PutOutInferno"));
            }
            if ((target.getTargetType() == Targetable.TYPE_HEX_EXTINGUISH)
                    && game.getBoard().isInfernoBurning(target.getPosition())) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.PutOutInferno"));
            }
            setSpecialResolution(true);
            return toHit;
        }
        
        // if this is a space bombing attack then get the to hit and return
        if (wtype.hasFlag(WeaponType.F_SPACE_BOMB)) {
            if (te != null) {
                toHit = Compute.getSpaceBombBaseToHit(ae, te, game);
                setSpecialResolution(true);
                return toHit;
            }
        }
        //If we get here, no special weapons apply. Return the input data and continue on
        return toHit;
    }
    
    /**
     * Convenience method that compiles the ToHit modifiers applicable to swarm attacks
     * 
     * @param game The current game
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * 
     * @param wtype The WeaponType of the weapon being used
     */
    private static ToHitData handleSwarmAttacks(IGame game, Entity ae, Targetable target,
            int ttype, ToHitData toHit, WeaponType wtype)  {
        if (ae == null) {
            //*Should* be impossible at this point in the process
            return toHit;
        }
        setSpecialResolution(false);
        if (target == null || ttype != Targetable.TYPE_ENTITY) {
            //Can only swarm a valid entity target
            return toHit;
        }
        Entity te = (Entity) target;
        // Leg attacks and Swarm attacks have their own base toHit values
        if (Infantry.LEG_ATTACK.equals(wtype.getInternalName())) {
            toHit = Compute.getLegAttackBaseToHit(ae, te, game);
            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                setSpecialResolution(true);
                return toHit;
            }
            if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMech"));
            }

        } else if (Infantry.SWARM_MEK.equals(wtype.getInternalName())) {
            toHit = Compute.getSwarmMekBaseToHit(ae, te, game);
            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                setSpecialResolution(true);
                return toHit;
            }

            if (te instanceof Tank) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.TeVehicle"));
            }
            if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMech"));
            }

            // If the defender carries mechanized BA, they can fight off the
            // swarm
            if (te != null) {
                for (Entity e : te.getExternalUnits()) {
                    if (e instanceof BattleArmor) {
                        BattleArmor ba = (BattleArmor) e;
                        int def = ba.getShootingStrength();
                        int att = ((Infantry) ae).getShootingStrength();
                        if (!(ae instanceof BattleArmor)) {
                            if (att >= 28) {
                                att = 5;
                            } else if (att >= 24) {
                                att = 4;
                            } else if (att >= 21) {
                                att = 3;
                            } else if (att >= 18) {
                                att = 2;
                            } else {
                                att = 1;
                            }
                        }
                        def = (def + 2) - att;
                        if (def > 0) {
                            toHit.addModifier(def, Messages.getString("WeaponAttackAction.DefendingBA"));
                        }
                    }
                }
            }
            setSpecialResolution(true);
            return toHit;
        } else if (Infantry.STOP_SWARM.equals(wtype.getInternalName())) {
            // Can't stop if we're not swarming, otherwise automatic.
            setSpecialResolution(true);
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.EndSwarm"));
        }
        // Swarming infantry always hit their target, but
        // they can only target the Mek they're swarming.
        else if ((te != null) && (ae.getSwarmTargetId() == te.getId())) {
            int side = te instanceof Tank ? ToHitData.SIDE_RANDOM : ToHitData.SIDE_FRONT;
            if (ae instanceof BattleArmor) {
                if (!Infantry.SWARM_WEAPON_MEK.equals(wtype.getInternalName()) && !(wtype instanceof InfantryAttack)) {
                    setSpecialResolution(true);
                    return new ToHitData(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.WrongSwarmUse"));
                }
                setSpecialResolution(true);
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.SwarmingAutoHit"), ToHitData.HIT_SWARM,
                        side);
            }
            setSpecialResolution(true);
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.SwarmingAutoHit"), ToHitData.HIT_SWARM_CONVENTIONAL, side);
        }
        //If we get here, no swarm attack applies
        return toHit;
    }
    
    /**
     * Convenience method that compiles the ToHit modifiers applicable to artillery attacks
     * 
     * @param game The current game
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * 
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param atype The AmmoType being used for this attack
     * 
     * @param isArtilleryDirect  flag that indicates whether this is a direct-fire artillery attack
     * @param isArtilleryFLAK   flag that indicates whether this is a flak artillery attack
     * @param isArtilleryIndirect  flag that indicates whether this is an indirect-fire artillery attack
     * @param isHoming flag that indicates whether this is a homing missile/copperhead shot
     * @param usesAmmo  flag that indicates if the WeaponType being used is ammo-fed
     */
    private static ToHitData handleArtilleryAttacks(IGame game, Entity ae, Targetable target, int ttype, 
            ToHitData losMods, ToHitData toHit, WeaponType wtype, Mounted weapon, AmmoType atype, 
            boolean isArtilleryDirect, boolean isArtilleryFLAK, boolean isArtilleryIndirect, boolean isHoming,
            boolean usesAmmo) {
        setSpecialResolution(false);
        
        Entity te = null;
        if (target != null && ttype == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        
        //Homing warheads just need a flat 4 to seek out a successful TAG
        if (isHoming) {  
            setSpecialResolution(true);
            return new ToHitData(4, Messages.getString("WeaponAttackAction.HomingArty"));
        }
        
        //Don't bother adding up modifiers if the target hex has been hit before
        if (game.getEntity(ae.getId()).getOwner().getArtyAutoHitHexes().contains(target.getPosition())
                && !isArtilleryFLAK) {
            setSpecialResolution(true);
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.ArtyDesTarget"));
        }
        
        // Handle direct artillery attacks.
        if (isArtilleryDirect) {
            //If an airborne unit occupies the target hex, standard artillery ammo makes a flak attack against it
            //TN is a flat 3 + the altitude mod + the attacker's weapon skill
            if (isArtilleryFLAK && te != null) {
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.ArtyFlak"));
                if (te != null && te.isAirborne()) {
                    if (te.getAltitude() > 3) {
                        if (te.getAltitude() > 9) {
                            toHit.addModifier(3, Messages.getString("WeaponAttackAction.AeroTeAlt10"));
                        } else if (te.getAltitude() > 6) {
                            toHit.addModifier(2, Messages.getString("WeaponAttackAction.AeroTeAlt79"));
                        } else if (te.getAltitude() > 3) {
                            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeroTeAlt46"));
                        }
                    }
                }
                setSpecialResolution(true);
                return toHit;
            } else {
                //All other direct fire artillery attacks
                toHit.addModifier(4, Messages.getString("WeaponAttackAction.DirectArty"));
                toHit.append(Compute.getAttackerMovementModifier(game, ae.getId()));
                toHit.append(losMods);
                toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
                // actuator & sensor damage to attacker
                toHit.append(Compute.getDamageWeaponMods(ae, weapon));
                // heat
                if (ae.getHeatFiringModifier() != 0) {
                    toHit.addModifier(ae.getHeatFiringModifier(), Messages.getString("WeaponAttackAction.Heat"));
                }

                // weapon to-hit modifier
                if (wtype.getToHitModifier() != 0) {
                    toHit.addModifier(wtype.getToHitModifier(), Messages.getString("WeaponAttackAction.WeaponMod"));
                }

                // ammo to-hit modifier
                if (usesAmmo && (atype != null) && (atype.getToHitModifier() != 0)) {
                    toHit.addModifier(atype.getToHitModifier(),
                            atype.getSubMunitionName()
                                    + Messages.getString("WeaponAttackAction.AmmoMod"));
                }
            }
            setSpecialResolution(true);
            return toHit;
        }
        //And now for indirect artillery fire
        if (isArtilleryIndirect) {
            int mod = 7;
            if (ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)) {
                mod--;
            }
            toHit.addModifier(mod, Messages.getString("WeaponAttackAction.IndirectArty"));
            int adjust = ae.aTracker.getModifier(weapon, target.getPosition());
            boolean spotterIsForwardObserver = ae.aTracker.getSpotterHasForwardObs();
            if (adjust == TargetRoll.AUTOMATIC_SUCCESS) {
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                        "Artillery firing at target that's been hit before.");
            } else if (adjust != 0) {
                toHit.addModifier(adjust, Messages.getString("WeaponAttackAction.AdjustedFire"));
                if (spotterIsForwardObserver) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.FooSpotter"));
                }
            }
            if (ae.isAirborne()) {
                if (ae.getAltitude() > 6) {
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.Altitude"));
                } else if (ae.getAltitude() > 3) {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Altitude"));
                }
            }
        }
        //If we get here, this isn't an artillery attack
        return toHit;
    }
}
