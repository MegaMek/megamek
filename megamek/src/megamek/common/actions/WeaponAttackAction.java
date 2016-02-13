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

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BipedMech;
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
import megamek.common.IAimingModes;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ILocationExposureStatus;
import megamek.common.INarcPod;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.MinefieldTarget;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.PlanetaryConditions;
import megamek.common.Protomech;
import megamek.common.QuadMech;
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
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.ArtilleryCannonWeapon;
import megamek.common.weapons.ArtilleryWeapon;
import megamek.common.weapons.GaussWeapon;
import megamek.common.weapons.ISBombastLaser;
import megamek.common.weapons.ISHGaussRifle;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.LRTWeapon;
import megamek.common.weapons.LaserBayWeapon;
import megamek.common.weapons.MekMortarWeapon;
import megamek.common.weapons.PPCBayWeapon;
import megamek.common.weapons.PulseLaserBayWeapon;
import megamek.common.weapons.SRTWeapon;
import megamek.common.weapons.ScreenLauncherBayWeapon;
import megamek.common.weapons.TSEMPWeapon;
import megamek.common.weapons.VariableSpeedPulseLaserWeapon;

/**
 * Represents intention to fire a weapon at the target.
 */
public class WeaponAttackAction extends AbstractAttackAction implements
        Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -9096603813317359351L;
    private int weaponId;
    private int ammoId = -1;
    private int aimedLocation = Entity.LOC_NONE;
    private int aimMode = IAimingModes.AIM_MODE_NONE;
    private int otherAttackInfo = -1; //
    private boolean nemesisConfused;
    private boolean swarmingMissiles;
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
     * Boolean flag that determiens if this shot was the first one by a
     * particular weapon in a strafing run.  Used to ensure that heat is only
     * added once.
     */
    protected boolean isStrafingFirstShot = false;

    // default to attacking an entity
    public WeaponAttackAction(int entityId, int targetId, int weaponId) {
        super(entityId, targetId);
        this.weaponId = weaponId;
    }

    public WeaponAttackAction(int entityId, int targetType, int targetId,
                              int weaponId) {
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
        return ((WeaponType) getEntity(game).getEquipment(getWeaponId())
                .getType()).hasFlag(WeaponType.F_DIVE_BOMB);
    }

    public int getAltitudeLoss(IGame game) {
        if (isAirToGround(game)) {
            if (((WeaponType) getEntity(game).getEquipment(getWeaponId())
                    .getType()).hasFlag(WeaponType.F_DIVE_BOMB)) {
                return 2;
            }
            if (((WeaponType) getEntity(game).getEquipment(getWeaponId())
                    .getType()).hasFlag(WeaponType.F_ALT_BOMB)) {
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
        return WeaponAttackAction.toHit(game, getEntityId(),
                game.getTarget(getTargetType(), getTargetId()), getWeaponId(),
                getAimedLocation(), getAimingMode(), nemesisConfused,
                swarmingMissiles,
                game.getTarget(getOldTargetType(), getOldTargetId()),
                game.getTarget(getOriginalTargetType(), getOriginalTargetId()),
                isStrafing());
    }

    public ToHitData toHit(IGame game, List<ECMInfo> allECMInfo) {
        return WeaponAttackAction.toHit(game, getEntityId(),
                game.getTarget(getTargetType(), getTargetId()), getWeaponId(),
                getAimedLocation(), getAimingMode(), nemesisConfused,
                swarmingMissiles,
                game.getTarget(getOldTargetType(), getOldTargetId()),
                game.getTarget(getOriginalTargetType(), getOriginalTargetId()),
                isStrafing(), allECMInfo);
    }

    public static ToHitData toHit(IGame game, int attackerId,
            Targetable target, int weaponId, boolean isStrafing) {
        return WeaponAttackAction.toHit(game, attackerId, target, weaponId,
                Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE, false, false,
                null, null, isStrafing);
    }

    public static ToHitData toHit(IGame game, int attackerId,
            Targetable target, int weaponId, int aimingAt, int aimingMode,
            boolean isStrafing) {
        return WeaponAttackAction.toHit(game, attackerId, target, weaponId,
                aimingAt, aimingMode, false, false, null, null, isStrafing);
    }

    private static ToHitData toHit(IGame game, int attackerId,
            Targetable target, int weaponId, int aimingAt, int aimingMode,
            boolean isNemesisConfused, boolean exchangeSwarmTarget,
            Targetable oldTarget, Targetable originalTarget, boolean isStrafing) {
        return WeaponAttackAction.toHit(game, attackerId, target, weaponId,
                aimingAt, aimingMode, isNemesisConfused, exchangeSwarmTarget,
                oldTarget, originalTarget, isStrafing, null);
    }

    /**
     * To-hit number for attacker firing a weapon at the target.
     */
    private static ToHitData toHit(IGame game, int attackerId,
            Targetable target, int weaponId, int aimingAt, int aimingMode,
            boolean isNemesisConfused, boolean exchangeSwarmTarget,
            Targetable oldTarget, Targetable originalTarget, boolean isStrafing,
            List<ECMInfo> allECMInfo) {
        final Entity ae = game.getEntity(attackerId);
        final Mounted weapon = ae.getEquipment(weaponId);

        if (!(weapon.getType() instanceof WeaponType)) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Not a weapon");
        }

        final WeaponType wtype = (WeaponType) weapon.getType();
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
        boolean isWeaponFieldGuns = isAttackerInfantry
                && (weapon.getLocation() == Infantry.LOC_FIELD_GUNS);
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        // 2002-09-16 Infantry weapons have unlimited ammo.
        final boolean usesAmmo = (wtype.getAmmoType() != AmmoType.T_NA)
                                 && !isWeaponInfantry;
        final Mounted ammo = usesAmmo ? weapon.getLinked() : null;
        final AmmoType atype = ammo == null ? null : (AmmoType) ammo.getType();
        final boolean targetInBuilding = Compute.isInBuilding(game, te);
        boolean bMekTankStealthActive = false;
        if ((ae instanceof Mech) || (ae instanceof Tank)) {
            bMekTankStealthActive = ae.isStealthActive();
        }
        boolean isIndirect = (wtype.hasModes() && weapon.curMode().equals(
                "Indirect"))
                             || (wtype instanceof ArtilleryCannonWeapon);
        boolean isInferno = ((atype != null)
                && ((atype.getAmmoType() == AmmoType.T_SRM) || (atype
                        .getAmmoType() == AmmoType.T_MML)) && (atype
                .getMunitionType() == AmmoType.M_INFERNO))
                || (isWeaponInfantry && (wtype.hasFlag(WeaponType.F_INFERNO)));
        boolean isArtilleryDirect = wtype.hasFlag(WeaponType.F_ARTILLERY)
                && (game.getPhase() == IGame.Phase.PHASE_FIRING);
        boolean isArtilleryIndirect = wtype.hasFlag(WeaponType.F_ARTILLERY)
                && ((game.getPhase() == IGame.Phase.PHASE_TARGETING) || (game
                        .getPhase() == IGame.Phase.PHASE_OFFBOARD));
        // hack, otherwise when actually resolves shot labeled impossible.
        boolean isArtilleryFLAK = isArtilleryDirect
                && (te != null)
                && ((((te.getMovementMode() == EntityMovementMode.VTOL) || (te
                        .getMovementMode() == EntityMovementMode.WIGE)) && te
                        .isAirborneVTOLorWIGE()) || (te.isAirborne()))
                && (atype != null)
                && (usesAmmo && (atype.getMunitionType() == AmmoType.M_STANDARD));
        boolean isHaywireINarced = ae.isINarcedWith(INarcPod.HAYWIRE);
        boolean isINarcGuided = false;
        // for attacks where ECM along flight path makes a difference
        boolean isECMAffected = ComputeECM.isAffectedByECM(ae,
                ae.getPosition(), target.getPosition(), allECMInfo);
        // for attacks where only ECM on the target hex makes a difference
        boolean isTargetECMAffected = ComputeECM.isAffectedByECM(ae,
                target.getPosition(), target.getPosition(), allECMInfo);
        boolean isTAG = wtype.hasFlag(WeaponType.F_TAG);
        boolean isHoming = false;
        boolean bHeatSeeking = (atype != null)
                && ((atype.getAmmoType() == AmmoType.T_SRM)
                        || (atype.getAmmoType() == AmmoType.T_MML) || (atype
                        .getAmmoType() == AmmoType.T_LRM))
                && (atype.getMunitionType() == AmmoType.M_HEAT_SEEKING);
        boolean bFTL = (atype != null)
                && ((atype.getAmmoType() == AmmoType.T_MML) || (atype
                        .getAmmoType() == AmmoType.T_LRM))
                && (atype.getMunitionType() == AmmoType.M_FOLLOW_THE_LEADER);

        Mounted mLinker = weapon.getLinkedBy();
        boolean bApollo = ((mLinker != null)
                           && (mLinker.getType() instanceof MiscType)
                           && !mLinker.isDestroyed() && !mLinker.isMissing()
                           && !mLinker.isBreached() && mLinker.getType().hasFlag(
                MiscType.F_APOLLO))
                          && (atype != null)
                          && (atype.getAmmoType() == AmmoType.T_MRM);
        boolean bArtemisV = ((mLinker != null)
                             && (mLinker.getType() instanceof MiscType)
                             && !mLinker.isDestroyed() && !mLinker.isMissing()
                             && !mLinker.isBreached()
                             && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)
                             && !isECMAffected && !bMekTankStealthActive
                             && (atype != null)
                             && (atype.getMunitionType() == AmmoType.M_ARTEMIS_V_CAPABLE));
        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, te);

        // is this attack originating from underwater
        // TODO: assuming that torpedoes are underwater attacks even if fired
        // from surface vessel, awaiting rules clarification
        // http://www.classicbattletech.com/forums/index.php/topic,48744.0.html
        boolean underWater = (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET)
                || (wtype instanceof SRTWeapon) || (wtype instanceof LRTWeapon);

        if (te != null) {
            if (!isTargetECMAffected
                    && te.isINarcedBy(ae.getOwner().getTeam())
                    && (atype != null)
                    && ((atype.getAmmoType() == AmmoType.T_LRM)
                            || (atype.getAmmoType() == AmmoType.T_MML)
                            || (atype.getAmmoType() == AmmoType.T_SRM)
                            || (atype.getAmmoType() == AmmoType.T_NLRM))
                    && (atype.getMunitionType() == AmmoType.M_NARC_CAPABLE)) {
                isINarcGuided = true;
            }
        }
        int toSubtract = 0;
        final int ttype = target.getTargetType();

        ToHitData toHit;
        String reason = WeaponAttackAction.toHitIsImpossible(game, ae, target,
                swarmPrimaryTarget, swarmSecondaryTarget, weapon, atype, wtype,
                ttype, exchangeSwarmTarget, usesAmmo, te, isTAG, isInferno,
                isAttackerInfantry, isIndirect, attackerId, weaponId,
                isArtilleryIndirect, ammo, isArtilleryFLAK, targetInBuilding,
                isArtilleryDirect, isTargetECMAffected, isStrafing);
        if (reason != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, reason);
        }

        // if this is a bombing attack then get the to hit and return
        // TODO: this should probably be its own kind of attack
        if (wtype.hasFlag(WeaponType.F_SPACE_BOMB)) {
            toHit = Compute.getSpaceBombBaseToHit(ae, te, game);
            return toHit;
        }

        // B-Pod firing at infantry in the same hex autohit
        if (wtype.hasFlag(WeaponType.F_B_POD) && (target instanceof Infantry)
            && target.getPosition().equals(ae.getPosition())) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                 "B-Pod firing at infantry");
        }

        long munition = AmmoType.M_STANDARD;
        if (atype != null) {
            munition = atype.getMunitionType();
        }
        if (munition == AmmoType.M_HOMING && ammo.curMode().equals("Homing")) {
            // target type checked later because its different for
            // direct/indirect (BMRr p77 on board arrow IV)
            isHoming = true;
        }
        int targEl;

        if (te == null) {
            targEl = -game.getBoard().getHex(target.getPosition()).depth();
        } else {
            targEl = te.relHeight();
        }

        // TODO: mech making DFA could be higher if DFA target hex is higher
        // BMRr pg. 43, "attacking unit is considered to be in the air
        // above the hex, standing on an elevation 1 level higher than
        // the target hex or the elevation of the hex the attacker is
        // in, whichever is higher."

        // if we're doing indirect fire, find a spotter
        Entity spotter = null;
        boolean narcSpotter = false;
        if (isIndirect) {
            if ((target instanceof Entity)
                && !isTargetECMAffected
                && (te != null)
                && (atype != null)
                && usesAmmo
                && (atype.getMunitionType() == AmmoType.M_NARC_CAPABLE)
                && (te.isNarcedBy(ae.getOwner().getTeam()) || te
                    .isINarcedBy(ae.getOwner().getTeam()))) {
                spotter = te;
                narcSpotter = true;
            } else {
                spotter = Compute.findSpotter(game, ae, target);
            }
            if ((spotter == null)
                    && (atype != null)
                    && ((atype.getAmmoType() == AmmoType.T_LRM)
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
        }

        // EI system
        // 0 if no EI (or switched off)
        // 1 if no intervening light woods
        // 2 if intervening light woods (because target in woods + intervening
        // woods is only +1 total)
        int eistatus = 0;

        boolean mpMelevationHack = false;
        if (usesAmmo
            && (wtype.getAmmoType() == AmmoType.T_LRM)
            && (atype != null)
            && (atype.getMunitionType() == AmmoType.M_MULTI_PURPOSE)
            && (ae.getElevation() == -1)
            && (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET)) {
            mpMelevationHack = true;
            // surface to fire
            ae.setElevation(0);
        }
        // check LOS (indirect LOS is from the spotter)
        LosEffects los;
        ToHitData losMods;

        if (!isIndirect || (spotter == null)) {
            if (!exchangeSwarmTarget) {
                los = LosEffects.calculateLos(game, attackerId, target);
            } else {
                // Swarm should draw LoS between targets, not attacker, since
                // we don't want LoS to be blocked
                if (swarmPrimaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                    los = LosEffects.calculateLos(game,
                            swarmPrimaryTarget.getTargetId(),
                            swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLos(game,
                            swarmSecondaryTarget.getTargetId(),
                            swarmPrimaryTarget);
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
            if ((atype != null)
                    && ((atype.getAmmoType() == AmmoType.T_LRM_TORPEDO)
                            || (atype.getAmmoType() == AmmoType.T_SRM_TORPEDO) || (((atype
                            .getAmmoType() == AmmoType.T_SRM)
                            || (atype.getAmmoType() == AmmoType.T_MRM)
                            || (atype.getAmmoType() == AmmoType.T_LRM) || (atype
                            .getAmmoType() == AmmoType.T_MML)) && (munition == AmmoType.M_TORPEDO)))
                    && (los.getMinimumWaterDepth() < 1)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Torpedos must follow water their entire LOS");
            }
        } else {
            if (!exchangeSwarmTarget) {
                los = LosEffects.calculateLos(game, spotter.getId(), target,
                        true);
            } else {
                // Swarm should draw LoS between targets, not attacker, since
                // we don't want LoS to be blocked
                if (swarmPrimaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                    los = LosEffects.calculateLos(game,
                            swarmPrimaryTarget.getTargetId(),
                            swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLos(game,
                            swarmSecondaryTarget.getTargetId(),
                            swarmPrimaryTarget);
                }
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

        // Leg attacks, Swarm attacks, and
        // Mine Launchers don't use gunnery.
        if (Infantry.LEG_ATTACK.equals(wtype.getInternalName())) {
            toHit = Compute.getLegAttackBaseToHit(ae, te, game);
            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                return toHit;
            }
            if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
                toHit.addModifier(-1, "target is superheavy mech");
            }

        } else if (Infantry.SWARM_MEK.equals(wtype.getInternalName())) {
            toHit = Compute.getSwarmMekBaseToHit(ae, te, game);
            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                return toHit;
            }

            if (te instanceof Tank) {
                toHit.addModifier(-2, "target is vehicle");
            }
            if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
                toHit.addModifier(-1, "target is superheavy mech");
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
                            toHit.addModifier(def, "Defending mechanized BA");
                        }
                    }
                }
            }
        } else if (Infantry.STOP_SWARM.equals(wtype.getInternalName())) {
            // Can't stop if we're not swarming, otherwise automatic.
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                 "End swarm attack.");
        } else if (BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName())) {
            // Mine launchers can not hit infantry.
            toHit = new ToHitData(8, "magnetic mine attack");
        } else if ((atype != null)
                   && (atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB)) {
            if (ae.getPosition().equals(target.getPosition())) {
                // Micro bombs use anti-mech skill
                toHit = new ToHitData(ae.getCrew().getPiloting(),
                                      "anti-mech skill");
                if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
                    toHit.addModifier(-1, "target is superheavy mech");
                }
                return toHit;
            }
            return new ToHitData(TargetRoll.IMPOSSIBLE, "out of range");
        }
        // Swarming infantry always hit their target, but
        // they can only target the Mek they're swarming.
        else if ((te != null) && (ae.getSwarmTargetId() == te.getId())) {
            int side = te instanceof Tank ? ToHitData.SIDE_RANDOM
                                          : ToHitData.SIDE_FRONT;
            if (ae instanceof BattleArmor) {
                if (!Infantry.SWARM_WEAPON_MEK.equals(wtype.getInternalName())
                    && !(wtype instanceof InfantryAttack)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Use the 'Attack Swarmed Mek' attack instead");
                }
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                        "swarming (automatic hit)", ToHitData.HIT_SWARM, side);
            }
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                 "swarming", ToHitData.HIT_SWARM_CONVENTIONAL,
                                 side);
        } else if (isArtilleryFLAK) {
            if (game.getOptions().booleanOption("artillery_skill")) {
                toHit = new ToHitData(ae.getCrew().getArtillery(),
                        "artillery skill");
            } else {
                toHit = new ToHitData(ae.getCrew().getGunnery(),
                        "gunnery skill");
            }
            toHit.addModifier(3, "artillery flak attack");
            if ((te != null) && te.isAirborne()) {
                if (te.getAltitude() > 3) {
                    if (te.getAltitude() > 9) {
                        toHit.addModifier(TargetRoll.IMPOSSIBLE,
                                "airborne aerospace at altitude > 10");
                    } else if (te.getAltitude() > 6) {
                        toHit.addModifier(2,
                                "airborne aerospace at altitude 7-9");
                    } else if (te.getAltitude() > 3) {
                        toHit.addModifier(1,
                                "airborne aerospace at altitude 4-6.");
                    }
                }
            }
        } else {
            toHit = new ToHitData(ae.getCrew().getGunnery(), "gunnery skill");
            if (game.getOptions().booleanOption("rpg_gunnery")) {
                if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                    toHit = new ToHitData(ae.getCrew().getGunneryL(),
                                          "gunnery (L) skill");
                }
                if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    toHit = new ToHitData(ae.getCrew().getGunneryM(),
                                          "gunnery (M) skill");
                }
                if (wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                    toHit = new ToHitData(ae.getCrew().getGunneryB(),
                                          "gunnery (B) skill");
                }
            }
            if (wtype.hasFlag(WeaponType.F_ARTILLERY)
                && game.getOptions().booleanOption("artillery_skill")) {
                toHit = new ToHitData(ae.getCrew().getArtillery(),
                                      "artillery skill");
            }
            if (ae.getTaserFeedBackRounds() > 0) {
                toHit.addModifier(1, "Taser feedback");
            }
            if (ae.getTaserInterferenceRounds() > 0) {
                toHit.addModifier(ae.getTaserInterference(),
                                  "Taser interference");
            }
        }

        // Engineer's fire extinguisher has fixed to hit number,
        // Note that coolant trucks make a regular attack.
        if (wtype.hasFlag(WeaponType.F_EXTINGUISHER)) {
            toHit = new ToHitData(8, "fire extinguisher");
            if (((target instanceof Entity) && ((Entity) target).infernos
                    .isStillBurning())
                || ((target instanceof Tank) && ((Tank) target)
                    .isInfernoFire())) {
                toHit.addModifier(2, "inferno fire");
            }
            if ((Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType())
                && game.getBoard().isInfernoBurning(target.getPosition())) {
                toHit.addModifier(2, "inferno fire");
            }
            return toHit;
        }

        // if we're spotting for indirect fire, add +1
        if (ae.isSpotting()) {
            toHit.addModifier(+1, "attacker is spotting for indirect LRM fire");
        }

        if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
            toHit.addModifier(-1, "target is superheavy mech");
        }

        // fatigue
        if (game.getOptions().booleanOption("tacops_fatigue")
            && ae.getCrew().isGunneryFatigued()) {
            toHit.addModifier(1, "fatigue");
        }

        // If a unit is suffering from electromagnetic interference, they get a
        // blanket +2.
        // Sucks to be them.
        if (ae.isSufferingEMI()) {
            toHit.addModifier(+2, "electromagnetic interference");
        }

        // evading bonuses (
        if ((te != null) && te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), "target is evading");
        }

        // ghost target modifier
        if (game.getOptions().booleanOption("tacops_ghost_target")
            && !isIndirect && !isArtilleryIndirect && !isArtilleryDirect) {
            int ghostTargetMod = Compute.getGhostTargetNumber(ae,
                                                              ae.getPosition(), target.getPosition());
            if ((ghostTargetMod > -1)
                && !((ae instanceof Infantry) && !(ae instanceof BattleArmor))) {
                int bapMod = 0;
                if (ae.hasBAP()) {
                    bapMod = 1;
                }
                int tcMod = 0;
                if (ae.hasTargComp()
                    && wtype.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && !wtype.hasFlag(WeaponType.F_CWS)
                    && !wtype.hasFlag(WeaponType.F_TASER)
                    && (atype != null)
                    && (!usesAmmo || !(((atype.getAmmoType() == AmmoType.T_AC_LBX) || (atype
                                                                                               .getAmmoType() ==
                                                                                       AmmoType.T_AC_LBX_THB)) && (atype
                                                                                                                                                     .getMunitionType() == AmmoType.M_CLUSTER)))) {
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
                    if (game.getOptions().intOption("ghost_target_max") > 0) {
                        mod = Math
                                .min(mod,
                                     game.getOptions().intOption(
                                             "ghost_target_max"));
                    }
                    toHit.addModifier(mod, "ghost targets");
                }
            }
        }

        // Space ECM
        if (game.getBoard().inSpace()
            && game.getOptions().booleanOption("stratops_ecm")) {
            int ecm = ComputeECM.getLargeCraftECM(ae, ae.getPosition(),
                                                  target.getPosition());
            if (!ae.isLargeCraft()) {
                ecm += ComputeECM.getSmallCraftECM(ae, ae.getPosition(),
                                                   target.getPosition());
            }
            ecm = Math.min(4, ecm);
            int eccm = 0;
            if (ae.isLargeCraft()) {
                eccm = ((Aero) ae).getECCMBonus();
            }
            if (ecm > 0) {
                toHit.addModifier(ecm, "ECM");
                if (eccm > 0) {
                    toHit.addModifier(-1 * Math.min(ecm, eccm), "ECCM");
                }
            }
        }

        if (Compute.isGroundToAir(ae, target) && (null != te) && te.isNOE()) {
            if (te.passedWithin(ae.getPosition(), 1)) {
                toHit.addModifier(+1, "target is NOE");
            } else {
                toHit.addModifier(+3, "target is NOE");
            }
        }

        if (Compute.isGroundToAir(ae, target)
            && game.getOptions().booleanOption("stratops_aa_fire")
            && (null != te) && (te instanceof Aero)) {
            int vMod = ((Aero) te).getCurrentVelocity();
            if (game.getOptions().booleanOption("aa_move_mod")) {
                vMod = Math.min(vMod / 2, 4);
            }
            toHit.addModifier(vMod, "velocity");
        }

        // Aeros may suffer from criticals
        if (ae instanceof Aero) {
            Aero aero = (Aero) ae;

            // sensor hits
            int sensors = aero.getSensorHits();

            if (!aero.isCapitalFighter()) {
                if ((sensors > 0) && (sensors < 3)) {
                    toHit.addModifier(sensors, "sensor damage");
                }
                if (sensors > 2) {
                    toHit.addModifier(+5, "sensors destroyed");
                }
            }

            // FCS hits
            int fcs = aero.getFCSHits();

            if ((fcs > 0) && !aero.isCapitalFighter()) {
                toHit.addModifier(fcs * 2, "fcs damage");
            }

            // pilot hits
            int pilothits = aero.getCrew().getHits();
            if ((pilothits > 0) && !aero.isCapitalFighter()) {
                toHit.addModifier(pilothits, "pilot hits");
            }

            // out of control
            if (aero.isOutControlTotal()) {
                toHit.addModifier(+2, "out-of-control");
            }

            if (aero instanceof Jumpship) {
                Jumpship js = (Jumpship) aero;
                int cic = js.getCICHits();
                if (cic > 0) {
                    toHit.addModifier(cic * 2, "CIC damage");
                }
            }

            // targeting mods for evasive action by large craft
            if (aero.isEvading()) {
                toHit.addModifier(+2, "attacker is evading");
            }

            // check for heavy gauss rifle on fighter of small craft
            if ((weapon.getType() instanceof ISHGaussRifle)
                && (ae instanceof Aero) && !(ae instanceof Dropship)
                && !(ae instanceof Jumpship)) {
                toHit.addModifier(+1, "weapon to-hit modifier");
            }

            // check for NOE
            if (Compute.isAirToAir(ae, target)) {
                if (target.isAirborneVTOLorWIGE()) {
                    toHit.addModifier(+5,
                                      "targeting non-aerospace airborne unit");
                }
                if (ae.isNOE()) {
                    if (ae.isOmni()) {
                        toHit.addModifier(+1,
                                          "attacker is flying at NOE (omni)");
                    } else {
                        toHit.addModifier(+2, "attacker is flying at NOE");
                    }
                }
            }

            if (!ae.isAirborne() && !ae.isSpaceborne()) {
                // grounded aero
                if (!(ae instanceof Dropship)) {
                    toHit.addModifier(+2, "grounded aero");
                } else if (!target.isAirborne() && !isArtilleryIndirect) {
                    toHit.addModifier(-2,
                                      "grounded dropships firing on ground units");
                }
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
                            toHit.addModifier(+1, "bay contains heavy laser");
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
                        toHit.addModifier(-2, "barracuda missile");
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
                        toHit.addModifier(-2, "barracuda missile");
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
                        toHit.addModifier(-1, "cluster ammo");
                    }
                }
            }
        }

        if (wtype.hasFlag(WeaponType.F_ANTI_SHIP) && (te != null)
            && (te.getWeight() < 500)) {
            toHit.addModifier(4, "Anti-ship missile at a small target");
        }

        if (wtype.hasFlag(WeaponType.F_MASS_DRIVER)) {
            toHit.addModifier(2, "Mass Driver to-hit Penalty");
        }

        if ((te instanceof Aero) && te.isAirborne()) {

            Aero a = (Aero) te;

            // is the target at zero velocity
            if ((a.getCurrentVelocity() == 0)
                && !(a.isSpheroid() && !game.getBoard().inSpace())) {
                toHit.addModifier(-2, "target is not moving");
            }

            // capital weapon (except missiles) penalties at small targets
            if (wtype.isCapital()
                && (wtype.getAtClass() != WeaponType.CLASS_CAPITAL_MISSILE)
                && (wtype.getAtClass() != WeaponType.CLASS_AR10)
                && !te.isLargeCraft()) {
                // check to see if we are using AAA mode
                int aaaMod = 0;
                if (wtype.hasModes() && weapon.curMode().equals("AAA")) {
                    aaaMod = 2;
                }
                if (wtype.isSubCapital()) {
                    toHit.addModifier(3 - aaaMod,
                                      "sub-capital weapon at small target");
                } else {
                    toHit.addModifier(5 - aaaMod,
                                      "capital weapon at small target");
                }
            }

            // AAA mode makes targeting large craft more difficult
            if (wtype.hasModes() && weapon.curMode().equals("AAA")
                && te.isLargeCraft()) {
                toHit.addModifier(+1, "AAA mode at large craft");
            }

            // check for bracketing mode
            if (wtype.hasModes() && weapon.curMode().equals("Bracket 80%")) {
                toHit.addModifier(-1, "Bracketing 80%");
            }
            if (wtype.hasModes() && weapon.curMode().equals("Bracket 60%")) {
                toHit.addModifier(-2, "Bracketing 60%");
            }
            if (wtype.hasModes() && weapon.curMode().equals("Bracket 40%")) {
                toHit.addModifier(-3, "Bracketing 40%");
            }

            // sensor shadows
            if (game.getOptions().booleanOption("stratops_sensor_shadow")
                && game.getBoard().inSpace()) {
                for (Entity en : Compute.getAdjacentEntitiesAlongAttack(
                        ae.getPosition(), target.getPosition(), game)) {
                    if (!en.isEnemyOf(a) && en.isLargeCraft()
                        && ((en.getWeight() - a.getWeight()) >= -100000.0)) {
                        toHit.addModifier(+1, "Sensor Shadow");
                        break;
                    }
                }
                for (Entity en : game.getEntitiesVector(target.getPosition())) {
                    if (!en.isEnemyOf(a) && en.isLargeCraft() && !en.equals(a)
                        && ((en.getWeight() - a.getWeight()) >= -100000.0)) {
                        toHit.addModifier(+1, "Sensor Shadow");
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
                    toHit.addModifier(+1, "copilot injured");
                } else {
                    toHit.addModifier(+1, "commander injured");
                }
            }
            int sensors = tank.getSensorHits();
            if (sensors > 0) {
                toHit.addModifier(sensors, "sensor damage");
            }
            if (tank.isStabiliserHit(weapon.getLocation())) {
                toHit.addModifier(
                        Compute.getAttackerMovementModifier(game, tank.getId())
                               .getValue(), "stabiliser damage");
            }
        }

        if (ae.hasFunctionalArmAES(weapon.getLocation()) && !weapon.isSplit()) {
            toHit.addModifier(-1, "AES modifer");
        }

        if (ae.hasShield()) {
            // active shield has already been checked as it makes shots
            // impossible
            // time to check passive defense and no defense

            if (ae.hasPassiveShield(weapon.getLocation(),
                                    weapon.isRearMounted())) {
                toHit.addModifier(+2, "weapon hampered by passive shield");
            } else if (ae.hasNoDefenseShield(weapon.getLocation())) {
                toHit.addModifier(+1, "weapon hampered by shield");
            }
        }
        // if we have BAP with MaxTech rules, and there are woods in the
        // way, and we are within BAP range, we reduce the BTH by 1
        if (game.getOptions().booleanOption("tacops_bap")
            && !isIndirect
            && (te != null)
            && ae.hasBAP()
            && (ae.getBAPRange() >= Compute.effectiveDistance(game, ae, te))
            && !ComputeECM.isAffectedByECM(ae, ae.getPosition(),
                                           te.getPosition())
            && (game.getBoard().getHex(te.getPosition())
                    .containsTerrain(Terrains.WOODS)
                || game.getBoard().getHex(te.getPosition())
                       .containsTerrain(Terrains.JUNGLE)
                || (los.getLightWoods() > 0)
                || (los.getHeavyWoods() > 0) || (los.getUltraWoods() > 0))) {
            toHit.addModifier(-1, "target in/behind woods and attacker has BAP");
        }

        // quirks
        if (ae.hasQuirk(OptionsConstants.QUIRK_NEG_SENSOR_GHOSTS)) {
            toHit.addModifier(+1, "sensor ghosts");
        }

        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_POS_ACCURATE)) {
            toHit.addModifier(-1, "accurate weapon");
        }

        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE)) {
            toHit.addModifier(+1, "inaccurate weapon");
        }

        // Has the pilot the appropriate gunnery skill?
        if (ae.getCrew().getOptions().booleanOption("gunnery_laser")
            && wtype.hasFlag(WeaponType.F_ENERGY)) {
            toHit.addModifier(-1, "Gunnery/Energy");
        }

        if (ae.getCrew().getOptions().booleanOption("gunnery_ballistic")
            && wtype.hasFlag(WeaponType.F_BALLISTIC)) {
            toHit.addModifier(-1, "Gunnery/Ballistic");
        }

        if (ae.getCrew().getOptions().booleanOption("gunnery_missile")
            && wtype.hasFlag(WeaponType.F_MISSILE)) {
            toHit.addModifier(-1, "Gunnery/Missile");
        }

        // Is the pilot a weapon specialist?
        if (ae.getCrew().getOptions().stringOption("weapon_specialist")
              .equals(wtype.getName())) {
            toHit.addModifier(-2, "weapon specialist");
        } else if (ae.getCrew().getOptions().booleanOption("specialist")) {
            // aToW style gunnery specialist: -1 to specialized weapon and +1 to
            // all other weapons
            // Note that weapon specialist supercedes gunnery specialization, so
            // if you have
            // a specialization in Medium Lasers and a Laser specialization, you
            // only get the -2 specialization mod
            if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                if (ae.getCrew().getOptions().stringOption("specialist")
                      .equals(Crew.SPECIAL_LASER)) {
                    toHit.addModifier(-1, "Laser Specialization");
                } else {
                    toHit.addModifier(+1, "Unspecialized");
                }
            } else if (wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                if (ae.getCrew().getOptions().stringOption("specialist")
                      .equals(Crew.SPECIAL_BALLISTIC)) {
                    toHit.addModifier(-1, "Ballistic Specialization");
                } else {
                    toHit.addModifier(+1, "Unspecialized");
                }
            } else if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                if (ae.getCrew().getOptions().stringOption("specialist")
                      .equals(Crew.SPECIAL_MISSILE)) {
                    toHit.addModifier(-1, "Missile Specialization");
                } else {
                    toHit.addModifier(+1, "Unspecialized");
                }
            }
        }

        // check for VDNI
        if (ae.getCrew().getOptions().booleanOption("vdni")
            || ae.getCrew().getOptions().booleanOption("bvdni")) {
            toHit.addModifier(-1, "VDNI");
        }

        if ((ae instanceof Infantry) && !(ae instanceof BattleArmor)) {
            // check for pl-masc
            // the rules are a bit vague, but assume that if the infantry didn't
            // move or jumped, then they shouldn't get the penalty
            if (ae.getCrew().getOptions().booleanOption("pl_masc")
                && ((ae.moved == EntityMovementType.MOVE_WALK) || (ae.moved == EntityMovementType.MOVE_RUN))) {
                toHit.addModifier(+1, "PL-MASC");
            }

            // check for cyber eye laser sighting on ranged attacks
            if (ae.getCrew().getOptions().booleanOption("cyber_eye_tele")
                && !(wtype instanceof InfantryAttack)) {
                toHit.addModifier(-1, "MD laser-sighting");
            }
        }

        // industrial cockpit: +1 to hit
        if ((ae instanceof Mech)
            && (((Mech) ae).getCockpitType() == Mech.COCKPIT_INDUSTRIAL)) {
            toHit.addModifier(1,
                              "industrial cockpit without advanced fire control");
        }
        // primitive industrial cockpit: +2 to hit
        if ((ae instanceof Mech)
            && (((Mech) ae).getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            toHit.addModifier(2,
                              "primitive industrial cockpit without advanced fire control");
        }

        // primitive industrial cockpit with advanced firing control: +1 to hit
        if ((ae instanceof Mech)
            && (((Mech) ae).getCockpitType() == Mech.COCKPIT_PRIMITIVE)
            && ((Mech) ae).isIndustrial()) {
            toHit.addModifier(1,
                              "primitive industrial cockpit with advanced fire control");
        }

        if ((ae instanceof SupportTank) || (ae instanceof SupportVTOL)) {
            if (!ae.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)
                && !ae.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                toHit.addModifier(2, "support vehicle without fire control");
            } else if (ae.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)
                       && !(ae.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL))) {
                toHit.addModifier(1, "support vehicle with basic fire control");
            }
        }

        // Do we use Listen-Kill ammo from War of 3039 sourcebook?
        if (!isECMAffected
            && (atype != null)
            && ((atype.getAmmoType() == AmmoType.T_LRM)
                || (atype.getAmmoType() == AmmoType.T_MML) || (atype
                                                                       .getAmmoType() == AmmoType.T_SRM))
            && (atype.getMunitionType() == AmmoType.M_LISTEN_KILL)
            && !((te != null) && te.isClan())) {
            toHit.addModifier(-1, "Listen-Kill ammo");
        }

        // determine some more variables
        int aElev = ae.getElevation();
        int tElev = target.getElevation();
        int aAlt = ae.getAltitude();
        int tAlt = target.getAltitude();
        int distance = Compute.effectiveDistance(game, ae, target);

        if (!isArtilleryIndirect) {
            toHit.append(AbstractAttackAction.nightModifiers(game, target, atype,
                    ae, true));
        }

        TargetRoll weatherToHitMods = new TargetRoll();

        // weather mods (not in space)
        int weatherMod = game.getPlanetaryConditions().getWeatherHitPenalty(ae);
        if ((weatherMod != 0) && !game.getBoard().inSpace()) {
            weatherToHitMods.addModifier(weatherMod, game
                    .getPlanetaryConditions().getWeatherDisplayableName());
        }

        // wind mods (not in space)
        if (!game.getBoard().inSpace()) {
            int windCond = game.getPlanetaryConditions().getWindStrength();
            if (windCond == PlanetaryConditions.WI_MOD_GALE) {
                if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    weatherToHitMods.addModifier(1, PlanetaryConditions
                            .getWindDisplayableName(windCond));
                }
            } else if (windCond == PlanetaryConditions.WI_STRONG_GALE) {
                if (wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                    weatherToHitMods.addModifier(1, PlanetaryConditions
                            .getWindDisplayableName(windCond));
                } else if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    weatherToHitMods.addModifier(2, PlanetaryConditions
                            .getWindDisplayableName(windCond));
                }
            } else if (windCond == PlanetaryConditions.WI_STORM) {
                if (wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                    weatherToHitMods.addModifier(2, PlanetaryConditions
                            .getWindDisplayableName(windCond));
                } else if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    weatherToHitMods.addModifier(3, PlanetaryConditions
                            .getWindDisplayableName(windCond));
                }
            } else if (windCond == PlanetaryConditions.WI_TORNADO_F13) {
                if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                    weatherToHitMods.addModifier(2, PlanetaryConditions
                            .getWindDisplayableName(windCond));
                } else {
                    weatherToHitMods.addModifier(3, PlanetaryConditions
                            .getWindDisplayableName(windCond));
                }
            } else if (windCond == PlanetaryConditions.WI_TORNADO_F4) {
                weatherToHitMods.addModifier(3,
                                             PlanetaryConditions.getWindDisplayableName(windCond));
            }
        }

        // fog mods (not in space)
        if (wtype.hasFlag(WeaponType.F_ENERGY)
            && !game.getBoard().inSpace()
            && (game.getPlanetaryConditions().getFog() == PlanetaryConditions.FOG_HEAVY)) {
            weatherToHitMods.addModifier(1, "heavy fog");
        }

        // blowing sand mods
        if (wtype.hasFlag(WeaponType.F_ENERGY)
            && !game.getBoard().inSpace()
            && game.getPlanetaryConditions().isSandBlowing()
            && (game.getPlanetaryConditions().getWindStrength() > PlanetaryConditions.WI_LIGHT_GALE)) {
            weatherToHitMods.addModifier(1, "blowing sand");
        }

        if (weatherToHitMods.getValue() > 0) {
            if ((ae.getCrew() != null) && ae.getCrew().getOptions().booleanOption("weathered")) {
                weatherToHitMods.addModifier(-1, "weathered");
            }
            toHit.append(weatherToHitMods);
        }


        // gravity mods (not in space)
        if (!game.getBoard().inSpace()) {
            int mod = (int) Math.floor(Math.abs((game.getPlanetaryConditions()
                    .getGravity() - 1.0f) / 0.2f));
            if ((mod != 0)
                && (wtype.hasFlag(WeaponType.F_BALLISTIC) || wtype
                    .hasFlag(WeaponType.F_MISSILE))) {
                toHit.addModifier(mod, "gravity");
            }
        }

        // Electro-Magnetic Interference
        if (game.getPlanetaryConditions().hasEMI()
            && !((ae instanceof Infantry) && !(ae instanceof BattleArmor))) {
            toHit.addModifier(2, "EMI");
        }

        if (ae.isAirborne() && !(ae instanceof Aero)) {
            toHit.addModifier(+2, "dropping");
            toHit.addModifier(+3, "jumping");
        }

        // Handle direct artillery attacks.
        if (isArtilleryDirect) {
            if (!isArtilleryFLAK) {
                toHit.addModifier(4, "direct artillery modifer");
            }
            toHit.append(Compute.getAttackerMovementModifier(game, attackerId));
            toHit.append(losMods);
            toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
            // actuator & sensor damage to attacker
            toHit.append(Compute.getDamageWeaponMods(ae, weapon));
            // heat
            if (ae.getHeatFiringModifier() != 0) {
                toHit.addModifier(ae.getHeatFiringModifier(), "heat");
            }

            // weapon to-hit modifier
            if (wtype instanceof VariableSpeedPulseLaserWeapon) {
                int nRange = ae.getPosition().distance(target.getPosition());
                int[] nRanges = wtype.getRanges(weapon);
                int modifier = wtype.getToHitModifier();

                if (nRange <= nRanges[RangeType.RANGE_SHORT]) {
                    modifier -= RangeType.RANGE_SHORT;
                } else if (nRange <= nRanges[RangeType.RANGE_MEDIUM]) {
                    modifier -= RangeType.RANGE_MEDIUM;
                } else if (nRange <= nRanges[RangeType.RANGE_LONG]) {
                    modifier -= RangeType.RANGE_LONG;
                } else {
                    modifier = 0;
                }

                toHit.addModifier(modifier, "weapon to-hit modifier");
            } else if (wtype instanceof ISBombastLaser) {
                int damage = (int) Math.ceil((Compute.dialDownDamage(weapon,
                                                                     wtype) - 7) / 2);

                if (damage > 0) {
                    toHit.addModifier(damage, "weapon to-hit modifier");
                }
            } else if (wtype.getToHitModifier() != 0) {
                toHit.addModifier(wtype.getToHitModifier(),
                                  "weapon to-hit modifier");
            }

            // ammo to-hit modifier
            if (usesAmmo && (atype != null) && (atype.getToHitModifier() != 0)) {
                toHit.addModifier(atype.getToHitModifier(),
                                  "ammunition to-hit modifier");
            }
            if (isHoming) {
                return new ToHitData(4, "Homing shot");
            }

            if (game.getEntity(attackerId).getOwner().getArtyAutoHitHexes()
                    .contains(target.getPosition())
                && !isArtilleryFLAK) {
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                     "Artillery firing at designated artillery target.");
            }
            return toHit;
        }
        if (isArtilleryIndirect) {
            if (isHoming) {
                return new ToHitData(4, "Homing shot (will miss if TAG misses)");
            }

            if (game.getEntity(attackerId).getOwner().getArtyAutoHitHexes()
                    .contains(target.getPosition())) {
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                     "Artillery firing at designated artillery target.");
            }
            int mod = 7;
            if (ae.getCrew().getOptions().booleanOption("oblique_attacker")) {
                mod--;
            }
            toHit.addModifier(mod, "indirect artillery modifier");
            int adjust = ae.aTracker.getModifier(weapon, target.getPosition());
            if (adjust == TargetRoll.AUTOMATIC_SUCCESS) {
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                     "Artillery firing at target that's been hit before.");
            } else if (adjust != 0) {
                toHit.addModifier(adjust, "adjusted fire");
            }
            if (ae.isAirborne()) {
                if (ae.getAltitude() > 6) {
                    toHit.addModifier(+2, "altitude");
                } else if (ae.getAltitude() > 3) {
                    toHit.addModifier(+1, "altitude");
                }
            }
            return toHit;

        }

        // Attacks against adjacent buildings automatically hit.
        if ((distance == 1)
            && ((target.getTargetType() == Targetable.TYPE_BUILDING)
                || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE) || (target instanceof GunEmplacement)
        )) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                 "Targeting adjacent building.");
        }

        // Attacks against buildings from inside automatically hit.
        if ((null != los.getThruBldg())
            && ((target.getTargetType() == Targetable.TYPE_BUILDING)
                || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE) || (target instanceof GunEmplacement)
        )) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                 "Targeting building from inside (are you SURE this is a good idea?).");
        }

        // add range mods
        toHit.append(Compute.getRangeMods(game, ae, weaponId, target));

        if (ae.hasQuirk(OptionsConstants.QUIRK_POS_ANTI_AIR) && (target instanceof Entity)) {
            if (target.isAirborneVTOLorWIGE() || target.isAirborne()) {
                toHit.addModifier(-2,
                                  "anti-air targetting system vs. aerial unit");
            }
        }

        // air-to-ground strikes apply a +2 mod
        if (Compute.isAirToGround(ae, target)) {
            if (wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                toHit.addModifier(ae.getAltitude(), "bombing altitude");
            } else if (isStrafing) {
                toHit.addModifier(+4, "strafing");
                if (ae.getAltitude() == 1) {
                    toHit.addModifier(+2, "strafing at NOE");
                }
                // Additional Nape-of-Earth restrictions for strafing
                if (ae.getAltitude() == 1) {
                    Coords prevCoords = ae.passedThroughPrevious(target
                            .getPosition());
                    IHex prevHex = game.getBoard().getHex(prevCoords);
                    toHit.append(Compute.getStrafingTerrainModifier(game,
                            eistatus, prevHex));
                }
            } else {
                toHit.addModifier(+2, "air to ground strike");
            }
        }

        // units making air to ground attacks are easier to hit by air-to-air
        // attacks
        if ((null != te) && Compute.isAirToAir(ae, target)) {
            for (Enumeration<EntityAction> i = game.getActions(); i
                    .hasMoreElements(); ) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if ((prevAttack.getEntityId() == te.getId())
                    && prevAttack.isAirToGround(game)) {
                    toHit.addModifier(-3, "target making air-to-ground attack");
                    break;
                }
            }
        }

        // units with the narrow/low profile quirk are harder to hit
        if ((te != null) && te.hasQuirk(OptionsConstants.QUIRK_POS_LOW_PROFILE)) {
            toHit.addModifier(1, "narrow/low profile");
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (!isAttackerInfantry && (te != null) && (te instanceof BattleArmor)) {
            toHit.addModifier(1, "battle armor target");
        }

        // infantry squads are also hard to hit
        if ((te instanceof Infantry) && !(te instanceof BattleArmor)
            && ((Infantry) te).isSquad()) {
            toHit.addModifier(1, "infantry squad target");
        }

        // Ejected MechWarriors are harder to hit
        if ((te != null) && (te instanceof MechWarrior)) {
            toHit.addModifier(2, "ejected MechWarrior target");
        }

        // Indirect fire has a +1 mod
        if (isIndirect) {
            if (ae.getCrew().getOptions().booleanOption("oblique_attacker")) {
                toHit.addModifier(0, "indirect fire");
            } else {
                toHit.addModifier(1, "indirect fire");
            }
        }

        if (wtype instanceof MekMortarWeapon) {
            if (isIndirect) {
                if (spotter == null) {
                    toHit.addModifier(2, "no spotter");
                }
            } else {
                toHit.addModifier(3, "direct fire");
            }
        }

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        if (te != null) {
            ToHitData thTemp = Compute.getTargetMovementModifier(game,
                    target.getTargetId());
            toHit.append(thTemp);
            toSubtract += thTemp.getValue();

            // semiguided ammo negates this modifier, if TAG succeeded
            if ((atype != null)
                && ((atype.getAmmoType() == AmmoType.T_LRM)
                    || (atype.getAmmoType() == AmmoType.T_MML)
                    || (atype.getAmmoType() == AmmoType.T_NLRM)
                    || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                && (atype.getMunitionType() == AmmoType.M_SEMIGUIDED)
                && (te.getTaggedBy() != -1)) {
                int nAdjust = thTemp.getValue();
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust,
                            "Semi-guided ammo vs tagged target"));
                }
            }
            // precision ammo reduces this modifier
            else if ((atype != null)
                     && ((atype.getAmmoType() == AmmoType.T_AC) || (atype
                                                                            .getAmmoType() == AmmoType.T_LAC))
                     && (atype.getMunitionType() == AmmoType.M_PRECISION)) {
                int nAdjust = Math.min(2, thTemp.getValue());
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, "Precision Ammo"));
                }
            }
        }
        if (weapon.isKindRapidFire() && weapon.curMode().equals("Rapid")) {
            toHit.addModifier(1, "AC rapid fire mode");
        }

        // Armor Piercing ammo is a flat +1
        if ((atype != null)
            && ((atype.getAmmoType() == AmmoType.T_AC) || (atype
                                                                   .getAmmoType() == AmmoType.T_LAC))
            && (atype.getMunitionType() == AmmoType.M_ARMOR_PIERCING)) {
            toHit.addModifier(1, "Armor-Piercing Ammo");
        }

        // spotter movement, if applicable
        if (isIndirect) {
            // semiguided ammo negates this modifier, if TAG succeeded
            if ((atype != null)
                && ((atype.getAmmoType() == AmmoType.T_LRM)
                    || (atype.getAmmoType() == AmmoType.T_MML) 
                    || (atype.getAmmoType() == AmmoType.T_NLRM)
                    || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                    && (atype.getMunitionType() == AmmoType.M_SEMIGUIDED)) {
                boolean targetTagged = false;
                // If this is an entity, we can see if it's tagged
                if (te != null) {
                    targetTagged = te.getTaggedBy() != -1;
                } else { // Non entities will require us to look harder
                    for (TagInfo ti : game.getTagInfo()) {
                        if (target.getTargetId() == ti.target.getTargetId()) {
                            targetTagged = true;
                        }
                    }
                }

                if (targetTagged) {
                    toHit.addModifier(-1, "semiguided ignores spotter "
                            + "movement & indirect fire penalties");
                }
            } else if (!narcSpotter && (spotter != null)) {
                toHit.append(Compute.getSpotterMovementModifier(game,
                        spotter.getId()));
                if (spotter.isAttackingThisTurn()) {
                    toHit.addModifier(1,
                            "spotter is making an attack this turn");
                }
            }
        }

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain, not applicable when delivering minefields or bombs
        if (target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER) {
            toHit.append(Compute.getTargetTerrainModifier(game, target,
                    eistatus, inSameBuilding, underWater));
            toSubtract += Compute.getTargetTerrainModifier(game, target,
                    eistatus, inSameBuilding, underWater).getValue();
        }

        // target in water?
        IHex targHex = game.getBoard().getHex(target.getPosition());
        int partialWaterLevel = 1;
        if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
            partialWaterLevel = 2;
        }
        if ((te != null)
            && targHex.containsTerrain(Terrains.WATER)
                && (targHex.terrainLevel(Terrains.WATER) == partialWaterLevel)
                && (targEl == 0)
            && (te.height() > 0)) { // target in partial water 
            los.setTargetCover(los.getTargetCover()
                    | LosEffects.COVER_HORIZONTAL);
            losMods = los.losModifiers(game, eistatus, underWater);
        }

        if ((target instanceof Infantry) && !wtype.hasFlag(WeaponType.F_FLAMER)) {
            if (targHex.containsTerrain(Terrains.FORTIFIED)
                || (((Infantry) target).getDugIn() == Infantry.DUG_IN_COMPLETE)) {
                toHit.addModifier(2, "infantry dug in");
            }
        }

        // add in LOS mods that we've been keeping
        toHit.append(losMods);

        if ((te != null) && te.isHullDown()) {
            if ((te instanceof Mech)
                && (los.getTargetCover() > LosEffects.COVER_NONE)) {
                toHit.addModifier(2, "Hull down target");
            }
            // tanks going Hull Down is different rules then 'Mechs, the
            // direction the attack comes from matters
            else if ((te instanceof Tank)
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
                    toHit.addModifier(2, "Hull down target");
                }
            }
        }

        // secondary targets modifier,
        // if this is not a iNarc Nemesis confused attack
        // Inf field guns don't get secondary target mods, TO pg 311 
        if (!isNemesisConfused && !wtype.hasFlag(WeaponType.F_ALT_BOMB)
                && !isWeaponFieldGuns && !isStrafing) {
            toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
        }

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), "heat");
        }

        // actuator & sensor damage to attacker
        toHit.append(Compute.getDamageWeaponMods(ae, weapon));

        // target immobile
        boolean mekMortarMunitionsIgnoreImmobile = 
                weapon.getType().hasFlag(WeaponType.F_MEK_MORTAR)
                && (atype != null)
                && (atype.getMunitionType() == AmmoType.M_AIRBURST);
        if (!(wtype instanceof ArtilleryCannonWeapon) 
                && !mekMortarMunitionsIgnoreImmobile) {
            ToHitData immobileMod = Compute.getImmobileMod(target, aimingAt,
                    aimingMode);
            // grounded dropships are treated as immobile as well for purpose of
            // the mods
            if ((null != te) && !te.isAirborne() && !te.isSpaceborne()
                && (te instanceof Aero) && ((Aero) te).isSpheroid()) {
                immobileMod = new ToHitData(-4, "immobile dropship");
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
                proneMod = new ToHitData(-2, "target prone and adjacent");
            } else {
                // Harder at range.
                proneMod = new ToHitData(1, "target prone and at range");
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

            toHit.addModifier(modifier, "weapon to-hit modifier");
        } else if (wtype instanceof ISBombastLaser) {
            double damage = Compute.dialDownDamage(weapon, wtype);
            damage = Math.ceil((damage - 7) / 2);

            if (damage > 0) {
                toHit.addModifier((int) damage, "weapon to-hit modifier");
            }
        } else if (wtype.getToHitModifier() != 0) {
            toHit.addModifier(wtype.getToHitModifier(),
                              "weapon to-hit modifier");
        }

        // Check whether we're eligible for a flak bonus...
        boolean isFlakAttack = !game.getBoard().inSpace()
                && (te != null)
                && (te.isAirborne() || te.isAirborneVTOLorWIGE())
                && (atype != null)
                && ((((atype.getAmmoType() == AmmoType.T_AC_LBX)
                        || (atype.getAmmoType() == AmmoType.T_AC_LBX_THB)
                        || (atype.getAmmoType() == AmmoType.T_SBGAUSS))
                        && (atype.getMunitionType() == AmmoType.M_CLUSTER))
                        || (atype.getMunitionType() == AmmoType.M_FLAK)
                        || (atype.getAmmoType() == AmmoType.T_HAG));
        if (isFlakAttack) {
            // ...and if so, which one (HAGs get an extra -1 as per TW p. 136
            // that's not covered by anything else).
            if (atype.getAmmoType() == AmmoType.T_HAG) {
                toHit.addModifier(-3, "HAG flak to-hit modifier");
            } else {
                toHit.addModifier(-2, "flak to-hit modifier");
            }
        }
        // Apply ammo type modifier, if any.
        if (usesAmmo && (atype != null) && (atype.getToHitModifier() != 0)) {
            toHit.addModifier(atype.getToHitModifier(),
                              "ammunition to-hit modifier");
        }

        if ((atype != null)
            && ((atype.getAmmoType() == AmmoType.T_AAA_MISSILE) || (atype
                                                                            .getAmmoType() == AmmoType.T_LAA_MISSILE))
            && Compute.isAirToGround(ae, target)) {
            toHit.addModifier(+4, "AAA missile at ground target");
            if (ae.getAltitude() < 4) {
                toHit.addModifier(+3, "AAA missile below altitude 4");
            }
        }

        // add iNarc bonus
        if (isINarcGuided) {
            toHit.addModifier(-1, "iNarc homing pod");
        }

        // add Artemis V bonus
        if (bArtemisV) {
            toHit.addModifier(-1, "Artemis V FCS");
        }

        if (isHaywireINarced) {
            toHit.addModifier(1, "iNarc Haywire pod");
        }

        // `Screen launchers hit automatically (if in range)
        if ((toHit.getValue() != TargetRoll.IMPOSSIBLE)
            && ((wtype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) || (wtype instanceof ScreenLauncherBayWeapon))) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                 "Screen launchers always hit");
        }

        // Heat Seeking Missles
        if (bHeatSeeking) {
            if (te == null) {
                if ((target.getTargetType() == Targetable.TYPE_BUILDING)
                    || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
                    || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                    || (target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE)
                    || (target instanceof GunEmplacement)) {
                    IHex hexTarget = game.getBoard().getHex(
                            target.getPosition());
                    if (hexTarget.containsTerrain(Terrains.FIRE)) {
                        toHit.addModifier(-2, "ammunition to-hit modifier");
                    }
                }
            } else if ((te.isAirborne())
                       && (toHit.getSideTable() == ToHitData.SIDE_REAR)) {
                toHit.addModifier(-2, "ammunition to-hit modifier");
            } else if (te.heat == 0) {
                toHit.addModifier(1, "ammunition to-hit modifier");
            } else {
                toHit.addModifier(-te.getHeatMPReduction(),
                                  "ammunition to-hit modifier");
            }

            if (LosEffects.hasFireBetween(ae.getPosition(),
                                          target.getPosition(), game)) {
                toHit.addModifier(2, "fire between target and attacker");
            }
        }

        if (bFTL) {
            toHit.addModifier(2, "ammunition to-hit modifier");
        }

        if (bApollo) {
            toHit.addModifier(-1, "Apollo FCS");
        }

        // Heavy infantry have +1 penalty
        if ((ae instanceof Infantry)
            && ae.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
            toHit.addModifier(1, "Heavy Armor");
        }

        // penalty for void sig system
        if (ae.isVoidSigActive()) {
            toHit.addModifier(1, "Void signature active");
        }

        // add targeting computer (except with LBX cluster ammo)
        if ((aimingMode == IAimingModes.AIM_MODE_TARG_COMP)
            && (aimingAt != Entity.LOC_NONE)) {
            if (ae.hasActiveEiCockpit()) {
                if (ae.hasTargComp()) {
                    toHit.addModifier(2,
                                      "aiming with targeting computer & EI system");
                } else {
                    toHit.addModifier(6, "aiming with EI system");
                }
            } else {
                toHit.addModifier(3, "aiming with targeting computer");
            }
        } else {
            // LB-X cluster, HAG flak, flak ammo ineligible for TC bonus
            boolean usesLBXCluster = usesAmmo
                    && (atype != null)
                    && (atype.getAmmoType() == AmmoType.T_AC_LBX
                        || atype.getAmmoType() == AmmoType.T_AC_LBX_THB)
                    && atype.getMunitionType() == AmmoType.M_CLUSTER;
            boolean usesHAGFlak = usesAmmo && (atype != null)
                    && atype.getAmmoType() == AmmoType.T_HAG && isFlakAttack;
            boolean isSBGauss = usesAmmo && (atype != null)
                    && atype.getAmmoType() == AmmoType.T_SBGAUSS;
            boolean isFlakAmmo = usesAmmo && (atype != null)
                    && (atype.getMunitionType() == AmmoType.M_FLAK);
            if (ae.hasTargComp()
                    && wtype.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && !wtype.hasFlag(WeaponType.F_CWS)
                    && !wtype.hasFlag(WeaponType.F_TASER)
                    && (!usesAmmo || !(usesLBXCluster || usesHAGFlak
                            || isSBGauss || isFlakAmmo))) {
                toHit.addModifier(-1, "targeting computer");
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
            if (underWater
                && (targHex.containsTerrain(Terrains.WATER)
                    && (targEl == 0) && (te.height() > 0))) {
                // weapon underwater, target in partial water
                toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                toHit.setCover(LosEffects.COVER_UPPER);
            } else {
                if (game.getOptions().booleanOption("tacops_partial_cover")) {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(los.getTargetCover());
                } else {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(LosEffects.COVER_HORIZONTAL);
                }
                //Set damagable cover state information
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
            //    Only 'mechs can have partial cover - Arlith
        }

        // add penalty for called shots and change hit table, if necessary
        if (game.getOptions().booleanOption("tacops_called_shots")) {
            int call = weapon.getCalledShot().getCall();
            if ((call > CalledShot.CALLED_NONE)
                && (aimingMode != IAimingModes.AIM_MODE_NONE)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "you can't combine aimed shots and called shots");
            }
            switch (call) {
                case CalledShot.CALLED_NONE:
                    break;
                case CalledShot.CALLED_HIGH:
                    toHit.addModifier(+3, "called shot, high");
                    toHit.setHitTable(ToHitData.HIT_ABOVE);
                    break;
                case CalledShot.CALLED_LOW:
                    if (los.getTargetCover() == LosEffects.COVER_HORIZONTAL) {
                        return new ToHitData(TargetRoll.IMPOSSIBLE,
                                             "low called shots not possible in partial cover");
                    }
                    toHit.addModifier(+3, "called shot, low");
                    toHit.setHitTable(ToHitData.HIT_BELOW);
                    break;
                case CalledShot.CALLED_LEFT:
                    // handled by Compute#targetSideTable
                    toHit.addModifier(+3, "called shot, left");
                    break;
                case CalledShot.CALLED_RIGHT:
                    // handled by Compute#targetSideTable
                    toHit.addModifier(+3, "called shot, right");
                    break;
            }
            // If we're making a called shot with swarm LRMs, then the penalty
            // only applies to the original attack.
            if (call != CalledShot.CALLED_NONE) {
                toSubtract += 3;
            }
        }

        // change hit table for surface vessels hit by underwater attacks
        if (underWater && targHex.containsTerrain(Terrains.WATER)
            && (null != te) && te.isSurfaceNaval()) {
            toHit.setHitTable(ToHitData.HIT_UNDERWATER);
        }

        // factor in target side
        if (isAttackerInfantry && (0 == distance)) {
            // Infantry attacks from the same hex are resolved against the
            // front.
            toHit.setSideTable(ToHitData.SIDE_FRONT);
        } else {
            toHit.setSideTable(Compute.targetSideTable(ae, target, weapon
                    .getCalledShot().getCall()));
        }

        // Aeros in atmosphere can hit above and below
        if (Compute.isAirToAir(ae, target)) {
            if ((aAlt - tAlt) > 2) {
                toHit.setHitTable(ToHitData.HIT_ABOVE);
            } else if ((tAlt - aAlt) > 2) {
                toHit.setHitTable(ToHitData.HIT_BELOW);
            } else if (((aAlt - tAlt) > 0)
                       && ((te instanceof Aero) && ((Aero) te).isSpheroid())) {
                toHit.setHitTable(ToHitData.HIT_ABOVE);
            } else if (((aAlt - tAlt) < 0)
                       && ((te instanceof Aero) && ((Aero) te).isSpheroid())) {
                toHit.setHitTable(ToHitData.HIT_BELOW);
            }
        }
        if (Compute.isGroundToAir(ae, target) && ((aAlt - tAlt) > 2)) {
            toHit.setHitTable(ToHitData.HIT_BELOW);
        }

        if (target.isAirborne() && (target instanceof Aero)) {
            if (!(((Aero) target).isSpheroid() && !game.getBoard().inSpace())) {
                // get mods for direction of attack
                int side = toHit.getSideTable();
                // if this is an aero attack using advanced movement rules then
                // determine side differently
                if ((target instanceof Aero) && game.useVectorMove()) {
                    boolean usePrior = false;
                    Coords attackPos = ae.getPosition();
                    if (game.getBoard().inSpace()
                        && ae.getPosition().equals(target.getPosition())) {
                        if (((Aero) ae).shouldMoveBackHex((Aero) target)) {
                            attackPos = ae.getPriorPosition();
                        }
                        usePrior = ((Aero) target).shouldMoveBackHex((Aero) ae);
                    }
                    side = ((Entity) target).chooseSide(attackPos, usePrior);
                }
                if (side == ToHitData.SIDE_FRONT) {
                    toHit.addModifier(+1, "attack against nose");
                }
                if ((side == ToHitData.SIDE_LEFT)
                    || (side == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(+2, "attack against side");
                }
            }
        }

        // deal with grapples
        if (target instanceof Entity) {
            int grapple = ((Entity) target).getGrappled();
            if (grapple != Entity.NONE) {
                if ((grapple == ae.getId())
                    && (((Entity) target).getGrappleSide() == Entity.GRAPPLE_BOTH)) {
                    toHit.addModifier(-4, "target grappled");
                } else if ((grapple == ae.getId())
                           && (((Entity) target).getGrappleSide() != Entity.GRAPPLE_BOTH)) {
                    toHit.addModifier(-2, "target grappled (Chain Whip)");
                } else if (!exchangeSwarmTarget) {
                    toHit.addModifier(1, "CQC, possible friendly fire");
                } else {
                    // this -1 cancels the original +1
                    toHit.addModifier(-1, "friendly fire");
                    return toHit;
                }
            }
        }

        // remove old target movement and terrain mods,
        // add those for new target.
        if (exchangeSwarmTarget) {
            toHit.addModifier(-toSubtract, "original target mods");
            toHit.append(Compute.getImmobileMod(swarmSecondaryTarget, aimingAt,
                    aimingMode));
            toHit.append(Compute.getTargetTerrainModifier(game, game.getTarget(
                    swarmSecondaryTarget.getTargetType(),
                    swarmSecondaryTarget.getTargetId()), eistatus,
                    inSameBuilding, underWater));
            toHit.setCover(LosEffects.COVER_NONE);
            distance = Compute
                    .effectiveDistance(game, ae, swarmSecondaryTarget);

            // We might not attack the new target from the same side as the
            // old, so recalculate; the attack *direction* is still traced from
            // the original source.
            toHit.setSideTable(Compute
                    .targetSideTable(ae, swarmSecondaryTarget));

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
                swarmlos = LosEffects.calculateLos(game,
                        swarmSecondaryTarget.getTargetId(), target);
            } else {
                swarmlos = LosEffects.calculateLos(game,
                        swarmPrimaryTarget.getTargetId(), swarmSecondaryTarget);
            }

            // reset cover
            if (swarmlos.getTargetCover() != LosEffects.COVER_NONE) {
                if (game.getOptions().booleanOption("tacops_partial_cover")) {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(swarmlos.getTargetCover());
                } else {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(LosEffects.COVER_HORIZONTAL);
                }
            }
            // target in water?
            targHex = game.getBoard()
                    .getHex(swarmSecondaryTarget.getPosition());
            targEl = swarmSecondaryTarget.relHeight();

            if (swarmSecondaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                Entity oldEnt = game.getEntity(swarmSecondaryTarget
                        .getTargetId());
                toHit.append(Compute.getTargetMovementModifier(game,
                        oldEnt.getId()));
                // target in partial water
                partialWaterLevel = 1;
                if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
                    partialWaterLevel = 2;
                }
                if (targHex.containsTerrain(Terrains.WATER)
                        && (targHex.terrainLevel(Terrains.WATER) == partialWaterLevel)
                        && (targEl == 0) && (oldEnt.height() > 0)) {
                    toHit.setCover(toHit.getCover()
                            | LosEffects.COVER_HORIZONTAL);
                }
                // Prone
                if (oldEnt.isProne()) {
                    // easier when point-blank
                    if (distance <= 1) {
                        proneMod = new ToHitData(-2,
                                "target prone and adjacent");
                    } else {
                        // Harder at range.
                        proneMod = new ToHitData(1, "target prone and at range");
                    }
                }
                // I-Swarm bonus
                toHit.append(proneMod);
                if (!isECMAffected
                        && (atype != null)
                        && !oldEnt.isEnemyOf(ae)
                        && !(oldEnt.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                Mech.SYSTEM_SENSORS, Mech.LOC_HEAD) > 0)
                        && (atype.getMunitionType() == AmmoType.M_SWARM_I)) {
                    toHit.addModifier(+2,
                            "Swarm-I at friendly unit with intact sensors");
                }
            }
        }

        if (ae.getTsempEffect() == TSEMPWeapon.TSEMP_EFFECT_INTERFERENCE) {
            toHit.addModifier(+2, "attacker has TSEMP interference");
        }

        if (weapon.getType().hasFlag(WeaponType.F_VGL)) {
            Coords c = ae.getPosition().translated(weapon.getFacing());
            if ((target instanceof HexTarget)
                    && target.getPosition().equals(c)) {
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "Vehicular "
                    + "grenade launchers automatically hit all units in "
                    + "the 3 adjacent hexes of their firing arc!");
            }
        }
        
        if ((te instanceof Infantry) && ((Infantry)te).isTakingCover()) {
            if (te.getPosition().direction(ae.getPosition()) == te.getFacing()) {
                toHit.addModifier(+3, "firing through cover");
            }
        }
        
        if ((ae instanceof Infantry) && ((Infantry)ae).isTakingCover()) {
            if (ae.getPosition().direction(te.getPosition()) == ae.getFacing()) {
                toHit.addModifier(+1, "firing through cover");
            }
        }

        // okay!
        return toHit;
    }

    /**
     * To-hit number for attacker firing a generic weapon at the target.  Does
     * not factor in any special weapon or ammo considerations, including range
     * modifiers.  Also does not include gunnery skill.
     */
    public static ToHitData toHit(IGame game, int attackerId,
                                  Targetable target) {
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
        //toHit = new ToHitData(ae.getCrew().getGunnery(), "gunnery skill");
        ToHitData toHit = new ToHitData(0, "base");

        // taser feedback
        if (ae.getTaserFeedBackRounds() > 0) {
            toHit.addModifier(1, "Taser feedback");
        }
        // taser interference
        if (ae.getTaserInterferenceRounds() > 0) {
            toHit.addModifier(ae.getTaserInterference(),
                              "Taser interference");
        }
        // if we're spotting for indirect fire, add +1
        if (ae.isSpotting()) {
            toHit.addModifier(+1, "attacker is spotting for indirect LRM fire");
        }
        // super heavy modifier
        if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
            toHit.addModifier(-1, "target is superheavy mech");
        }
        // fatigue
        if (game.getOptions().booleanOption("tacops_fatigue")
            && ae.getCrew().isGunneryFatigued()) {
            toHit.addModifier(1, "fatigue");
        }
        // If a unit is suffering from electromagnetic interference, they get a
        // blanket +2.
        // Sucks to be them.
        if (ae.isSufferingEMI()) {
            toHit.addModifier(+2, "electromagnetic interference");
        }
        // evading bonuses (
        if ((target.getTargetType() == Targetable.TYPE_ENTITY)
            && te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), "target is evading");
        }
        // Space ECM
        if (game.getBoard().inSpace()
            && game.getOptions().booleanOption("stratops_ecm")) {
            int ecm = ComputeECM.getLargeCraftECM(ae, ae.getPosition(),
                                                  target.getPosition());
            if (!ae.isLargeCraft()) {
                ecm += ComputeECM.getSmallCraftECM(ae, ae.getPosition(),
                                                   target.getPosition());
            }
            ecm = Math.min(4, ecm);
            int eccm = 0;
            if (ae.isLargeCraft()) {
                eccm = ((Aero) ae).getECCMBonus();
            }
            if (ecm > 0) {
                toHit.addModifier(ecm, "ECM");
                if (eccm > 0) {
                    toHit.addModifier(-1 * Math.min(ecm, eccm), "ECCM");
                }
            }
        }

        if (Compute.isGroundToAir(ae, target) && (null != te) && te.isNOE()) {
            if (te.passedWithin(ae.getPosition(), 1)) {
                toHit.addModifier(+1, "target is NOE");
            } else {
                toHit.addModifier(+3, "target is NOE");
            }
        }

        if (Compute.isGroundToAir(ae, target)
            && game.getOptions().booleanOption("stratops_aa_fire")
            && (null != te) && (te instanceof Aero)) {
            int vMod = ((Aero) te).getCurrentVelocity();
            if (game.getOptions().booleanOption("aa_move_mod")) {
                vMod = Math.min(vMod / 2, 4);
            }
            toHit.addModifier(vMod, "velocity");
        }

        // Aeros may suffer from criticals
        if (ae instanceof Aero) {
            Aero aero = (Aero) ae;

            // sensor hits
            int sensors = aero.getSensorHits();

            if (!aero.isCapitalFighter()) {
                if ((sensors > 0) && (sensors < 3)) {
                    toHit.addModifier(sensors, "sensor damage");
                }
                if (sensors > 2) {
                    toHit.addModifier(+5, "sensors destroyed");
                }
            }

            // FCS hits
            int fcs = aero.getFCSHits();

            if ((fcs > 0) && !aero.isCapitalFighter()) {
                toHit.addModifier(fcs * 2, "fcs damage");
            }

            // pilot hits
            int pilothits = aero.getCrew().getHits();
            if ((pilothits > 0) && !aero.isCapitalFighter()) {
                toHit.addModifier(pilothits, "pilot hits");
            }

            // out of control
            if (aero.isOutControlTotal()) {
                toHit.addModifier(+2, "out-of-control");
            }

            if (aero instanceof Jumpship) {
                Jumpship js = (Jumpship) aero;
                int cic = js.getCICHits();
                if (cic > 0) {
                    toHit.addModifier(cic * 2, "CIC damage");
                }
            }

            // targeting mods for evasive action by large craft
            if (aero.isEvading()) {
                toHit.addModifier(+2, "attacker is evading");
            }

            // check for NOE
            if (Compute.isAirToAir(ae, target)) {
                if (target.isAirborneVTOLorWIGE()) {
                    toHit.addModifier(+5,
                                      "targeting non-aerospace airborne unit");
                }
                if (ae.isNOE()) {
                    if (ae.isOmni()) {
                        toHit.addModifier(+1,
                                          "attacker is flying at NOE (omni)");
                    } else {
                        toHit.addModifier(+2, "attacker is flying at NOE");
                    }
                }
            }

            if (!ae.isAirborne() && !ae.isSpaceborne()) {
                // grounded aero
                if (!(ae instanceof Dropship)) {
                    toHit.addModifier(+2, "grounded aero");
                } else if (!target.isAirborne()) {
                    toHit.addModifier(-2,
                                      "grounded dropships firing on ground units");
                }
            }

        }

        if (target.isAirborne() && (target instanceof Aero)) {

            Aero a = (Aero) target;

            // is the target at zero velocity
            if ((a.getCurrentVelocity() == 0)
                && !(a.isSpheroid() && !game.getBoard().inSpace())) {
                toHit.addModifier(-2, "target is not moving");
            }

            // sensor shadows
            if (game.getOptions().booleanOption("stratops_sensor_shadow")
                && game.getBoard().inSpace()) {
                for (Entity en : Compute.getAdjacentEntitiesAlongAttack(
                        ae.getPosition(), target.getPosition(), game)) {
                    if (!en.isEnemyOf(a) && en.isLargeCraft()
                        && ((en.getWeight() - a.getWeight()) >= -100000.0)) {
                        toHit.addModifier(+1, "Sensor Shadow");
                        break;
                    }
                }
                for (Entity en : game.getEntitiesVector(target.getPosition())) {
                    if (!en.isEnemyOf(a) && en.isLargeCraft() && !en.equals(a)
                        && ((en.getWeight() - a.getWeight()) >= -100000.0)) {
                        toHit.addModifier(+1, "Sensor Shadow");
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
                    toHit.addModifier(+1, "copilot injured");
                } else {
                    toHit.addModifier(+1, "commander injured");
                }
            }
            int sensors = tank.getSensorHits();
            if (sensors > 0) {
                toHit.addModifier(sensors, "sensor damage");
            }
        }

        // if we have BAP with MaxTech rules, and there are woods in the
        // way, and we are within BAP range, we reduce the BTH by 1
        if (game.getOptions().booleanOption("tacops_bap")
            && (te != null)
            && ae.hasBAP()
            && (ae.getBAPRange() >= Compute.effectiveDistance(game, ae, te))
            && !ComputeECM.isAffectedByECM(ae, ae.getPosition(),
                                           te.getPosition())
            && (game.getBoard().getHex(te.getPosition())
                    .containsTerrain(Terrains.WOODS)
                || game.getBoard().getHex(te.getPosition())
                       .containsTerrain(Terrains.JUNGLE)
                || (los.getLightWoods() > 0)
                || (los.getHeavyWoods() > 0) || (los.getUltraWoods() > 0))) {
            toHit.addModifier(-1, "target in/behind woods and attacker has BAP");
        }

        // quirks
        if (ae.hasQuirk(OptionsConstants.QUIRK_NEG_SENSOR_GHOSTS)) {
            toHit.addModifier(+1, "sensor ghosts");
        }

        // check for VDNI
        if (ae.getCrew().getOptions().booleanOption("vdni")
            || ae.getCrew().getOptions().booleanOption("bvdni")) {
            toHit.addModifier(-1, "VDNI");
        }

        if ((ae instanceof Infantry) && !(ae instanceof BattleArmor)) {
            // check for pl-masc
            // the rules are a bit vague, but assume that if the infantry didn't
            // move or jumped, then they shouldn't get the penalty
            if (ae.getCrew().getOptions().booleanOption("pl_masc")
                && ((ae.moved == EntityMovementType.MOVE_WALK) || (ae.moved == EntityMovementType.MOVE_RUN))) {
                toHit.addModifier(+1, "PL-MASC");
            }
        }

        // industrial cockpit: +1 to hit
        if ((ae instanceof Mech)
            && (((Mech) ae).getCockpitType() == Mech.COCKPIT_INDUSTRIAL)) {
            toHit.addModifier(1,
                              "industrial cockpit without advanced fire control");
        }
        // primitive industrial cockpit: +2 to hit
        if ((ae instanceof Mech)
            && (((Mech) ae).getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            toHit.addModifier(2,
                              "primitive industrial cockpit without advanced fire control");
        }

        // primitive industrial cockpit with advanced firing control: +1 to hit
        if ((ae instanceof Mech)
            && (((Mech) ae).getCockpitType() == Mech.COCKPIT_PRIMITIVE)
            && ((Mech) ae).isIndustrial()) {
            toHit.addModifier(1,
                              "primitive industrial cockpit with advanced fire control");
        }

        if ((ae instanceof SupportTank) || (ae instanceof SupportVTOL)) {
            if (!ae.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)
                && !ae.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                toHit.addModifier(2, "support vehicle without fire control");
            } else if (ae.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)
                       && !(ae.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL))) {
                toHit.addModifier(1, "support vehicle with basic fire control");
            }
        }

        // determine some more variables
        int aElev = ae.getElevation();
        int tElev = target.getElevation();
        int distance = Compute.effectiveDistance(game, ae, target);

        toHit.append(AbstractAttackAction.nightModifiers(game, target, null,
                                                         ae, true));

        // weather mods (not in space)
        int weatherMod = game.getPlanetaryConditions().getWeatherHitPenalty(ae);
        if ((weatherMod != 0) && !game.getBoard().inSpace()) {
            toHit.addModifier(weatherMod, game.getPlanetaryConditions()
                    .getWeatherDisplayableName());
        }

        // Electro-Magnetic Interference
        if (game.getPlanetaryConditions().hasEMI()
            && !((ae instanceof Infantry) && !(ae instanceof BattleArmor))) {
            toHit.addModifier(2, "EMI");
        }

        if (ae.isAirborne() && !(ae instanceof Aero)) {
            toHit.addModifier(+2, "dropping");
            toHit.addModifier(+3, "jumping");
        }

        // Attacks against adjacent buildings automatically hit.
        if ((distance == 1)
            && ((target.getTargetType() == Targetable.TYPE_BUILDING)
                || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE) || (target instanceof GunEmplacement)
        )) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                 "Targeting adjacent building.");
        }

        // Attacks against buildings from inside automatically hit.
        if ((null != los.getThruBldg())
            && ((target.getTargetType() == Targetable.TYPE_BUILDING)
                || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE) || (target instanceof GunEmplacement)
        )) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                 "Targeting building from inside (are you SURE this is a good idea?).");
        }

        if (ae.hasQuirk(OptionsConstants.QUIRK_POS_ANTI_AIR) && (target instanceof Entity)) {
            if (target.isAirborneVTOLorWIGE() || target.isAirborne()) {
                toHit.addModifier(-2,
                                  "anti-air targetting system vs. aerial unit");
            }
        }

        // air-to-ground strikes apply a +2 mod
        if (Compute.isAirToGround(ae, target)) {
            toHit.addModifier(+2, "air to ground strike");
        }

        // units making air to ground attacks are easier to hit by air-to-air
        // attacks
        if ((null != te) && Compute.isAirToAir(ae, target)) {
            for (Enumeration<EntityAction> i = game.getActions(); i
                    .hasMoreElements(); ) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if ((prevAttack.getEntityId() == te.getId())
                    && prevAttack.isAirToGround(game)) {
                    toHit.addModifier(-3, "target making air-to-ground attack");
                    break;
                }
            }
        }

        // units with the narrow/low profile quirk are harder to hit
        if ((te != null) && te.hasQuirk(OptionsConstants.QUIRK_POS_LOW_PROFILE)) {
            toHit.addModifier(1, "narrow/low profile");
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (!isAttackerInfantry && (te != null) && (te instanceof BattleArmor)) {
            toHit.addModifier(1, "battle armor target");
        }

        // Infantry squads are also hard to hit -- including for other infantry,
        // it seems (the rule is "all attacks"). However, this only applies to
        // proper squads deployed as such.
        if ((te instanceof Infantry) && !(te instanceof BattleArmor)
            && ((Infantry) te).isSquad()) {
            toHit.addModifier(1, "infantry squad target");
        }

        // Ejected MechWarriors are also more difficult targets.
        if ((te != null) && (te instanceof MechWarrior)) {
            toHit.addModifier(2, "ejected MechWarrior target");
        }

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));

        // target movement
        if (te != null) {
            ToHitData thTemp = Compute.getTargetMovementModifier(game,
                                                                 target.getTargetId());
            toHit.append(thTemp);
        }

        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));

        // target terrain, not applicable when delivering minefields or bombs
        if (target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER) {
            toHit.append(Compute.getTargetTerrainModifier(game, target,
                                                          eistatus, inSameBuilding, ae.isUnderwater()));
        }

        // target in water?
        IHex targHex = game.getBoard().getHex(target.getPosition());
        if ((target.getTargetType() == Targetable.TYPE_ENTITY)
            && targHex.containsTerrain(Terrains.WATER)
            && (targHex.terrainLevel(Terrains.WATER) == 1) && (targEl == 0)
            && (te.height() > 0)) { // target
            // in
            // partial
            // water
            los.setTargetCover(los.getTargetCover()
                               | LosEffects.COVER_HORIZONTAL);
            losMods = los.losModifiers(game, eistatus, ae.isUnderwater());
        }

        // add in LOS mods that we've been keeping
        toHit.append(losMods);

        if ((te != null) && te.isHullDown()) {
            if ((te instanceof Mech)
                && (los.getTargetCover() > LosEffects.COVER_NONE)) {
                toHit.addModifier(2, "Hull down target");
            }
            // tanks going Hull Down is different rules then 'Mechs, the
            // direction the attack comes from matters
            else if ((te instanceof Tank)
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
                    toHit.addModifier(2, "Hull down target");
                }
            }
        }

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), "heat");
        }

        // target immobile
        ToHitData immobileMod = Compute.getImmobileMod(target, -1, -1);
        // grounded dropships are treated as immobile as well for purpose of
        // the mods
        if ((null != te) && !te.isAirborne() && !te.isSpaceborne()
            && (te instanceof Aero) && ((Aero) te).isSpheroid()) {
            immobileMod = new ToHitData(-4, "immobile dropship");
        }
        if (immobileMod != null) {
            toHit.append(immobileMod);
        }


        // attacker prone
        if (ae.isProne()) {
            toHit.addModifier(2, "attacker prone");
        }

        // target prone
        ToHitData proneMod = null;
        if ((te != null) && te.isProne()) {
            // easier when point-blank
            if (distance <= 1) {
                // TW, pg. 221: Swarm Mek attacks apply prone/immobile mods as
                // normal.
                proneMod = new ToHitData(-2, "target prone and adjacent");
            } else {
                // Harder at range.
                proneMod = new ToHitData(1, "target prone and at range");
            }
        }
        if (proneMod != null) {
            toHit.append(proneMod);
        }

        // Heavy infantry have +1 penalty
        if ((ae instanceof Infantry)
            && ae.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
            toHit.addModifier(1, "Heavy Armor");
        }

        // penalty for void sig system
        if (ae.isVoidSigActive()) {
            toHit.addModifier(1, "Void signature active");
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
            if (ae.isUnderwater()
                && (targHex.containsTerrain(Terrains.WATER)
                    && (targEl == 0) && (te.height() > 0))) {
                // weapon underwater, target in partial water
                toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                toHit.setCover(LosEffects.COVER_UPPER);
            } else {
                if (game.getOptions().booleanOption("tacops_partial_cover")) {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(los.getTargetCover());
                } else {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(LosEffects.COVER_HORIZONTAL);
                }
                //Set damagable cover state information
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
        if (ae.isUnderwater() && targHex.containsTerrain(Terrains.WATER)
            && (null != te) && te.isSurfaceNaval()) {
            toHit.setHitTable(ToHitData.HIT_UNDERWATER);
        }


        if (target.isAirborne() && (target instanceof Aero)) {
            if (!(((Aero) target).isSpheroid() && !game.getBoard().inSpace())) {
                // get mods for direction of attack
                int side = toHit.getSideTable();
                // if this is an aero attack using advanced movement rules then
                // determine side differently
                if ((target instanceof Aero) && game.useVectorMove()) {
                    boolean usePrior = false;
                    Coords attackPos = ae.getPosition();
                    if (game.getBoard().inSpace()
                        && ae.getPosition().equals(target.getPosition())) {
                        if (((Aero) ae).shouldMoveBackHex((Aero) target)) {
                            attackPos = ae.getPriorPosition();
                        }
                        usePrior = ((Aero) target).shouldMoveBackHex((Aero) ae);
                    }
                    side = ((Entity) target).chooseSide(attackPos, usePrior);
                }
                if (side == ToHitData.SIDE_FRONT) {
                    toHit.addModifier(+1, "attack against nose");
                }
                if ((side == ToHitData.SIDE_LEFT)
                    || (side == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(+2, "attack against side");
                }
            }
        }

        if (ae.getTsempEffect() == TSEMPWeapon.TSEMP_EFFECT_INTERFERENCE) {
            toHit.addModifier(+2, "attacker has TSEMP interference");
        }
        
        if ((te instanceof Infantry) && ((Infantry)te).isTakingCover()) {
            if (te.getPosition().direction(ae.getPosition()) == te.getFacing()) {
                toHit.addModifier(+3, "firing through cover");
            }
        }
        
        if ((ae instanceof Infantry) && ((Infantry)ae).isTakingCover()) {
            if (ae.getPosition().direction(te.getPosition()) == ae.getFacing()) {
                toHit.addModifier(+1, "firing through cover");
            }
        }

        // okay!
        return toHit;
    }

    private static String toHitIsImpossible(IGame game, Entity ae,
            Targetable target, Targetable swarmPrimaryTarget,
            Targetable swarmSecondaryTarget, Mounted weapon, AmmoType atype,
            WeaponType wtype, int ttype, boolean exchangeSwarmTarget,
            boolean usesAmmo, Entity te, boolean isTAG, boolean isInferno,
            boolean isAttackerInfantry, boolean isIndirect, int attackerId,
            int weaponId, boolean isArtilleryIndirect, Mounted ammo,
            boolean isArtilleryFLAK, boolean targetInBuilding,
            boolean isArtilleryDirect, boolean isTargetECMAffected,
            boolean isStrafing) {
        boolean isHoming = false;
        ToHitData toHit = null;

        if (weapon.isSquadSupportWeapon() && (ae instanceof BattleArmor)) {
            if (!((BattleArmor) ae).isTrooperActive(BattleArmor.LOC_TROOPER_1)) {
                return "Squad support mounted weapons cannot fire if " +
                       "Trooper 1 is dead!";
            }
        }

        // BA NARCs and Tasers can only fire at one target in a round
        if ((ae instanceof BattleArmor) &&
            (weapon.getType().hasFlag(WeaponType.F_TASER)
             || wtype.getAmmoType() == AmmoType.T_NARC)) {
            // Go through all of the current actions to see if a NARC or Taser
            //  has been fired
            for (Enumeration<EntityAction> i = game.getActions(); i
                    .hasMoreElements(); ) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                // Is this an attack from this entity to a different target?
                if (prevAttack.getEntityId() == ae.getId() &&
                    prevAttack.getTargetId() != target.getTargetId()) {
                    Mounted prevWeapon =
                            ae.getEquipment(prevAttack.getWeaponId());
                    WeaponType prevWtype = (WeaponType) prevWeapon.getType();
                    if (prevWeapon.getType().hasFlag(WeaponType.F_TASER)
                        && weapon.getType().hasFlag(WeaponType.F_TASER)) {
                        return "BA Tasers must all target the same unit!";
                    }
                    if (prevWtype.getAmmoType() == AmmoType.T_NARC
                        && wtype.getAmmoType() == AmmoType.T_NARC) {
                        return "BA NARCs must all target the same unit!";
                    }
                }
            }
        }

        // BA can only make one AP attack
        if ((ae instanceof BattleArmor)
                && weapon.getType().hasFlag(WeaponType.F_INFANTRY)) {
                final int weapId = ae.getEquipmentNum(weapon);
                // See if this unit has made a previous AP attack
                for (Enumeration<EntityAction> i = game.getActions(); i
                        .hasMoreElements(); ) {
                    Object o = i.nextElement();
                    if (!(o instanceof WeaponAttackAction)) {
                        continue;
                    }
                    WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                    // Is this an attack from this entity
                    if (prevAttack.getEntityId() == ae.getId()) {
                        Mounted prevWeapon =
                                ae.getEquipment(prevAttack.getWeaponId());
                        WeaponType prevWtype = (WeaponType) prevWeapon.getType();
                        if (prevWtype.hasFlag(WeaponType.F_INFANTRY)
                                && (prevAttack.getWeaponId() != weapId)) {
                            return "BA can only make one " +
                                    "anti-personnel attack!";
                        }
                    }
                }
            }

        if (game.getOptions().booleanOption("tacops_tank_crews")
            && (ae instanceof Tank) && ae.isUnjammingRAC()
            && (ae.getCrew().getSize() == 1)) {
            return "Vehicles with only 1 crewman may not take other actions while unjamming";
        }

        // is the attack originating from underwater
        boolean underWater = (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET)
                             || (wtype instanceof SRTWeapon) || (wtype instanceof LRTWeapon);

        if ((ae instanceof Protomech) && ((Protomech) ae).isEDPCharging()
            && wtype.hasFlag(WeaponType.F_ENERGY)) {
            return "ProtoMech is charging EDP";
        }

        // tasers only at non-flying units
        if (wtype.hasFlag(WeaponType.F_TASER)) {
            if (te != null) {
                if (te.isAirborne() || te.isAirborneVTOLorWIGE()) {
                    return "Tasers can't be fired at flying units.";
                }
            } else {
                return "Tasers can only fire at units.";
            }
        }

        if (wtype.hasFlag(WeaponType.F_TSEMP)
                && wtype.hasFlag(WeaponType.F_ONESHOT) && weapon.isFired()) {
            return "One shot TSEMP cannon expended";
        }
        
        if (wtype.hasFlag(WeaponType.F_TSEMP) && weapon.isFired()) {
            return "TSEMP cannon recharging";
        }

        // only leg mounted b-pods can be fired normally
        if (wtype.hasFlag(WeaponType.F_B_POD)) {
            if (!(target instanceof Infantry)) {
                return "B-Pods can't target non-infantry";
            }
            if (ae instanceof BipedMech) {
                if (!((weapon.getLocation() == Mech.LOC_LLEG) || (weapon
                                                                          .getLocation() == Mech.LOC_RLEG))) {
                    return "can fire only leg-mounted B-Pods";
                }
            } else if (ae instanceof QuadMech) {
                if (!((weapon.getLocation() == Mech.LOC_LLEG)
                      || (weapon.getLocation() == Mech.LOC_RLEG)
                      || (weapon.getLocation() == Mech.LOC_LARM) || (weapon
                                                                             .getLocation() == Mech.LOC_RARM))) {
                    return "can fire only leg-mounted B-Pods";
                }
            }
        }
        if (ae.hasShield()
            && ae.hasActiveShield(weapon.getLocation(),
                                  weapon.isRearMounted())) {
            return "Weapon blocked by active shield";
        }
        // If it has a torso-mounted cockpit and two head sensor hits or three
        // sensor hits...
        // It gets a =4 penalty for being blind!
        if ((ae instanceof Mech)
            && (((Mech) ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) {
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                 Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                return "Sensors Completely Destroyed for Torso-Mounted Cockpit";
            }
        }

        // can't fire Indirect LRM with direct LOS
        if (isIndirect
            && game.getOptions().booleanOption("indirect_fire")
            && !game.getOptions().booleanOption("indirect_always_possible")
            && LosEffects.calculateLos(game, ae.getId(), target).canSee()
            && (!game.getOptions().booleanOption("double_blind") || Compute
                .canSee(game, ae, target))
            && !(wtype instanceof ArtilleryCannonWeapon)
            && !(wtype instanceof MekMortarWeapon)) {
            return "Indirect-fire LRM cannot be fired with direct LOS from attacker to target.";
        }

        // If we're lying mines, we can't shoot.
        if (ae.isLayingMines()) {
            return "Can't fire weapons when laying mines";
        }

        // make sure weapon can deliver minefield
        if ((target.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER)
            && !AmmoType.canDeliverMinefield(atype)) {
            return "Weapon can't deliver minefields";
        }
        if ((target.getTargetType() == Targetable.TYPE_FLARE_DELIVER)
                && !(usesAmmo
                        && ((atype.getAmmoType() == AmmoType.T_LRM) 
                                || (atype.getAmmoType() == AmmoType.T_MML)
                                || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR)) 
                                && (atype.getMunitionType() == AmmoType.M_FLARE))) {
            return "Weapon can't deliver flares";
        }
        if ((game.getPhase() == IGame.Phase.PHASE_TARGETING)
            && !isArtilleryIndirect) {
            return "Only indirect artillery can be fired in the targeting phase";
        }
        if ((game.getPhase() == IGame.Phase.PHASE_OFFBOARD) && !isTAG) {
            return "Only TAG can be fired in the offboard attack phase";
        }
        if ((game.getPhase() != IGame.Phase.PHASE_OFFBOARD) && isTAG) {
            return "TAG can only be fired in the offboard attack phase";
        }
        if (isArtilleryDirect
            && ae.isAirborne()) {
            return "Airborne aerospace units cannot make direct-fire artillery attacks";
        }

        if (isArtilleryDirect
            && (Compute.effectiveDistance(game, ae, target) <= 6)) {
            return "Direct-Fire artillery attacks impossible at range <= 6";
        }

        // check called shots
        if (game.getOptions().booleanOption("tacops_called_shots")) {
            String reason = weapon.getCalledShot().isValid(target);
            if (reason != null) {
                return reason;
            }
        }

        if ((atype != null)
            && ((atype.getAmmoType() == AmmoType.T_LRM)
                || (atype.getAmmoType() == AmmoType.T_MML) || (atype
                                                                       .getAmmoType() == AmmoType.T_MEK_MORTAR))
            && ((atype.getMunitionType() == AmmoType.M_THUNDER)
                || (atype.getMunitionType() == AmmoType.M_THUNDER_ACTIVE)
                || (atype.getMunitionType() == AmmoType.M_THUNDER_INFERNO)
                || (atype.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB) || (atype
                                                                                         .getMunitionType() ==
                                                                                 AmmoType.M_THUNDER_AUGMENTED))
            && (target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER)) {
            return "Weapon can only deliver minefields";
        }
        if ((atype != null)
            && ((atype.getAmmoType() == AmmoType.T_LRM) || (atype
                                                                    .getAmmoType() == AmmoType.T_MML))
            && (atype.getMunitionType() == AmmoType.M_FLARE)
            && (target.getTargetType() != Targetable.TYPE_FLARE_DELIVER)) {
            return "Weapon can only deliver flares";
        }

        if (wtype.hasFlag(WeaponType.F_ANTI_SHIP) && !game.getBoard().inSpace()
            && (ae.getAltitude() < 4)) {
            return "Anti-ship missiles can only be used above elevation 3";
        }

        // some weapons can only target infantry
        if (wtype.hasFlag(WeaponType.F_INFANTRY_ONLY)) {
            if (((te != null) && !(te instanceof Infantry))
                || (target.getTargetType() != Targetable.TYPE_ENTITY)) {
                return "Weapon can only be used against infantry";
            }
        }

        // make sure weapon can clear minefield
        if ((target instanceof MinefieldTarget)
            && !AmmoType.canClearMinefield(atype)) {
            return "Weapon can't clear minefields";
        }

        // Arty shots have to be with arty, non arty shots with non arty.
        if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
            // check artillery is targetted appropriately for its ammo
            long munition = AmmoType.M_STANDARD;
            if (atype != null) {
                munition = atype.getMunitionType();
            }
            if (munition == AmmoType.M_HOMING
                    && ammo.curMode().equals("Homing")) {
                // target type checked later because its different for
                // direct/indirect (BMRr p77 on board arrow IV)
                isHoming = true;
            } else if ((ttype != Targetable.TYPE_HEX_ARTILLERY)
                       && (ttype != Targetable.TYPE_MINEFIELD_CLEAR)
                       && !isArtilleryFLAK) {
                return "Weapon must make artillery attacks.";
            }
            if (ae.isAirborne()) {
                if (isArtilleryDirect) {
                    return "Airborne aerospace units can't make direct-fire artillery attacks";
                } else if (isArtilleryIndirect
                           && (wtype.getAmmoType() != AmmoType.T_ARROW_IV)) {
                    return "Airborne aerospace units can't fire non-Arrow-IV artillery.";
                }
            }
        } else {
            // weapon is not artillery
            if (ttype == Targetable.TYPE_HEX_ARTILLERY) {
                return "Weapon can't make artillery attacks.";
            }
        }

        // check the following only if we're not a flight of continuing swarm
        // missiles
        if (!exchangeSwarmTarget) {

            if (!game.getOptions().booleanOption("friendly_fire") 
                    && !isStrafing) {
                // a friendly unit can never be the target of a direct attack.
                // but we do allow vehicle flamers to cool
                if ((target.getTargetType() == Targetable.TYPE_ENTITY)
                        && ((te.getOwnerId() == ae.getOwnerId()) || (te
                                .getOwner().getTeam() == ae.getOwner()
                                .getTeam()))) {
                    if (!(usesAmmo && (atype.getMunitionType() == AmmoType.M_COOLANT))) {
                        return "A friendly unit can never be the target of a direct attack.";
                    }
                }
            }
            // can't target yourself,
            if (ae.equals(te)) {
                return "You can't target yourself";
            }
            // is the attacker even active?
            if (ae.isShutDown() || !ae.getCrew().isActive()) {
                return "Attacker is in no condition to fire weapons.";
            }

            // sensors operational?
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            if ((ae instanceof Mech)
                && (((Mech) ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) {
                sensorHits += ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                 Mech.SYSTEM_SENSORS, Mech.LOC_CT);
                if (sensorHits > 2) {
                    return "Attacker sensors destroyed.";
                }
            } else if ((sensorHits > 1)
                       || ((ae instanceof Mech) && (((Mech) ae).isIndustrial() && (sensorHits == 1)))) {
                return "Attacker sensors destroyed.";
            }
            // weapon operational?
            if (!weapon.canFire(isStrafing)) {
                return "Weapon is not in a state where it can be fired";
            }

            // got ammo?
            if (usesAmmo
                && ((ammo == null) || (ammo.getUsableShotsLeft() == 0))) {
                return "Weapon out of ammo.";
            }

            // Aeros must have enough ammo for the maximum rate of fire because
            // they cannot lower it
            if ((ae instanceof Aero)
                && usesAmmo
                && (ammo != null)
                && (ae.getTotalAmmoOfType(ammo.getType()) < weapon
                    .getCurrentShots())) {
                return "weapon does not have enough ammo.";
            }

            if (ae instanceof Tank) {
                sensorHits = ((Tank) ae).getSensorHits();
                if (sensorHits > 3) {
                    return "Attacker sensors destroyed.";
                }
                if (((Tank) ae).getStunnedTurns() > 0) {
                    return "Crew stunned";
                }
            }
        }

        // Are we dumping that ammo?
        if (usesAmmo && ammo.isDumping()) {
            ae.loadWeaponWithSameAmmo(weapon);
            if ((ammo.getUsableShotsLeft() == 0) || ammo.isDumping()) {
                return "Dumping remaining ammo.";
            }
        }

        if (ae.isEvading() && !(ae instanceof Dropship)
            && !(ae instanceof Jumpship)) {
            return "Attacker is evading.";
        }

        if (ae instanceof Aero) {
            Aero aero = (Aero) ae;
            // FCS hits
            int fcs = aero.getFCSHits();
            if (fcs > 2) {
                return "Fire control system destroyed.";
            }

            if (aero instanceof Jumpship) {
                Jumpship js = (Jumpship) aero;
                int cic = js.getCICHits();
                if (cic > 2) {
                    return "CIC destroyed.";
                }
            }

            // if bombing, then can't do other attacks
            // also for altitude bombing, you must either be the first or be
            // adjacent to a prior one
            boolean adjacentAltBomb = false;
            boolean firstAltBomb = true;
            for (Enumeration<EntityAction> i = game.getActions(); i
                    .hasMoreElements(); ) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                if (prevAttack.getEntityId() == attackerId) {
                    if ((weaponId != prevAttack.getWeaponId())
                        && ae.getEquipment(prevAttack.getWeaponId())
                             .getType().hasFlag(WeaponType.F_SPACE_BOMB)) {
                        return "Already space bombing";
                    }
                    if ((weaponId != prevAttack.getWeaponId())
                        && ae.getEquipment(prevAttack.getWeaponId())
                             .getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
                        return "Already dive bombing";
                    }
                    if ((weaponId != prevAttack.getWeaponId())
                        && ae.getEquipment(prevAttack.getWeaponId())
                             .getType().hasFlag(WeaponType.F_ALT_BOMB)) {
                        // if the current attack is not an altitude bombing then
                        // return
                        if (!wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                            return "Already altitude bombing";
                        }
                        firstAltBomb = false;
                        int distance = prevAttack.getTarget(game).getPosition()
                                                 .distance(target.getPosition());
                        if (distance == 1) {
                            adjacentAltBomb = true;
                        }
                        if (distance == 0) {
                            return "already bombing this hex";
                        }

                    }
                }
            }
            if (wtype.hasFlag(WeaponType.F_ALT_BOMB) && !firstAltBomb
                && !adjacentAltBomb) {
                return "not adjacent to existing altitude bombing attacks";
            }
        }

        // you cannot bracket small craft at short range
        if (wtype.hasModes()
            && (weapon.curMode().equals("Bracket 80%")
                || weapon.curMode().equals("Bracket 60%") || weapon
                .curMode().equals("Bracket 40%"))
            && (target instanceof Aero)
            && !te.isLargeCraft()
            && (RangeType.rangeBracket(
                ae.getPosition().distance(target.getPosition()), wtype
                        .getRanges(weapon), true, false) == RangeType
                        .RANGE_SHORT)) {
            return "small craft cannot be bracketed at short range";
        }

        // you must have enough weapons in your bay to be able to use bracketing
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 80%")
            && (weapon.getBayWeapons().size() < 2)) {
            return "not enough weapons to bracket at this level";
        }
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 60%")
            && (weapon.getBayWeapons().size() < 3)) {
            return "not enough weapons to bracket at this level";
        }
        if (wtype.hasModes() && weapon.curMode().equals("Bracket 40%")
            && (weapon.getBayWeapons().size() < 4)) {
            return "not enough weapons to bracket at this level";
        }

        // Is the weapon blocked by a passenger?
        if (ae.isWeaponBlockedAt(weapon.getLocation(), weapon.isRearMounted())) {
            return "Weapon blocked by passenger.";
        }

        // Can't target an entity conducting a swarm attack.
        if ((te != null) && (Entity.NONE != te.getSwarmTargetId())) {
            return "Target is swarming a Mek.";
        }

        // "Cool" mode for vehicle flamer requires coolant system
        boolean vf_cool = false;
        if ((atype != null)
            && usesAmmo
            && (((AmmoType) ammo.getType()).getMunitionType() == AmmoType.M_COOLANT)) {
            vf_cool = true;
        }

        if (Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType()) {
            if (!wtype.hasFlag(WeaponType.F_EXTINGUISHER) && !vf_cool) {
                return "Weapon can't put out fires";
            }
            IHex hexTarget = game.getBoard().getHex(target.getPosition());
            if (!hexTarget.containsTerrain(Terrains.FIRE)) {
                return "Target is not on fire.";
            }
        } else if (wtype.hasFlag(WeaponType.F_EXTINGUISHER)) {
            if (!(((target instanceof Tank) && ((Tank) target).isOnFire()) || ((target instanceof Entity) && (((Entity) target).infernos
                                                                                                                      .getTurnsLeftToBurn() > 0)))) {
                return "Target is not on fire.";
            }
        }
        // Infantry can't clear woods.
        if (isAttackerInfantry
            && (Targetable.TYPE_HEX_CLEAR == target.getTargetType())) {
            IHex hexTarget = game.getBoard().getHex(target.getPosition());
            if (hexTarget.containsTerrain(Terrains.WOODS)) {
                return "Infantry can not clear woods.";
            }
        }

        // only screen launchers may launch screens (what a coincidence)
        if (Targetable.TYPE_HEX_SCREEN == target.getTargetType()) {
            if (!((wtype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) || (wtype instanceof ScreenLauncherBayWeapon))) {
                return "Only screen launchers may launch screens";
            }
        }

        if ((Targetable.TYPE_HEX_SCREEN != target.getTargetType())
            && ((wtype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) || (wtype instanceof ScreenLauncherBayWeapon))) {
            return "Screen launchers may only target hexes";
        }
        
        if (!(target instanceof HexTarget) && (atype != null)
                && (atype.getMunitionType() == AmmoType.M_MINE_CLEARANCE)) {
            return "Mine clearance munitions may only target hexes!";
        }

        // Some weapons can't cause fires, but Infernos always can.
        if ((vf_cool || (wtype.hasFlag(WeaponType.F_NO_FIRES) && !isInferno))
            && (Targetable.TYPE_HEX_IGNITE == target.getTargetType())) {
            return "Weapon can not cause fires.";
        }

        // only woods and buildings can be set intentionally on fire
        if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
            && game.getOptions().booleanOption("no_ignite_clear")
            && !(game.getBoard().getHex(((HexTarget) target).getPosition())
                     .containsTerrain(Terrains.WOODS)
                 || game.getBoard()
                        .getHex(((HexTarget) target).getPosition())
                        .containsTerrain(Terrains.JUNGLE)
                 || game.getBoard()
                        .getHex(((HexTarget) target).getPosition())
                        .containsTerrain(Terrains.FUEL_TANK) || game
                         .getBoard().getHex(((HexTarget) target).getPosition())
                         .containsTerrain(Terrains.BUILDING))) {
            return "Only woods and building hexes can be set on fire intentionally.";
        }

        // Can't target infantry with Inferno rounds (BMRr, pg. 141).
        // Also, enforce options for keeping vehicles and protos safe
        // if those options are checked.
        if (isInferno
            && (((te instanceof Tank) && game.getOptions().booleanOption(
                "vehicles_safe_from_infernos")) || ((te instanceof Protomech) && game
                .getOptions()
                .booleanOption("protos_safe_from_infernos")))) {
            return "Can not target that unit type with Inferno rounds.";
        }

        // The TAG system cannot target infantry.
        if (isTAG && (te instanceof Infantry)) {
            return "Can not target infantry with TAG.";
        }

        // The TAG system cannot target Airborne Aeros.
        if (isTAG && (te != null) && (te.isAirborne() || te.isSpaceborne())) {
            return "Can not target airborne units with TAG.";
        }

        //Airborne units cannot tag and attack
        //http://bg.battletech.com/forums/index.php?topic=17613.new;topicseen#new
        if (ae.isAirborne() && ae.usedTag()) {
            return "Airborne units cannot fire TAG and other weapons in the same turn.";
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
                        if ((m.getLinked() != null)
                            && (m.getLinked().getUsableShotsLeft() > 0)) {
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
                return "weapon bay out of ammo or otherwise unusable";
            }

            // create an array of booleans of locations
            boolean[] usedFrontArc = new boolean[ae.locations()];
            boolean[] usedRearArc = new boolean[ae.locations()];
            for (int i = 0; i < ae.locations(); i++) {
                usedFrontArc[i] = false;
                usedRearArc[i] = false;
            }

            for (Enumeration<EntityAction> i = game.getActions(); i
                    .hasMoreElements(); ) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                if ((prevAttack.getEntityId() == attackerId)
                    && (weaponId != prevAttack.getWeaponId())) {
                    Mounted prevWeapon = ae.getEquipment(prevAttack
                                                                 .getWeaponId());
                    int loc = prevWeapon.getLocation();
                    boolean rearMount = prevWeapon.isRearMounted();
                    if (game.getOptions().booleanOption("heat_by_bay")) {
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
            if (game.getOptions().booleanOption("heat_by_bay")) {
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

            if (game.getOptions().booleanOption("heat_by_bay")) {
                if ((totalheat + currentHeat) > heatcap) {
                    // FIXME: This is causing weird problems (try firing all the
                    // Suffen's nose weapons)
                    return "heat exceeds capacity";
                }
            } else {
                if (!rearMount) {
                    if (!usedFrontArc[loc]
                        && ((totalheat + currentHeat) > heatcap)
                        && !onlyArc) {
                        return "heat exceeds capacity";
                    }
                } else {
                    if (!usedRearArc[loc]
                        && ((totalheat + currentHeat) > heatcap)
                        && !onlyArc) {
                        return "heat exceeds capacity";
                    }
                }
            }
        } else if (ae instanceof Dropship) {
            int totalheat = 0;

            for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                if ((prevAttack.getEntityId() == attackerId)
                    && (weaponId != prevAttack.getWeaponId())) {
                    Mounted prevWeapon =
                            ae.getEquipment(prevAttack.getWeaponId());
                    totalheat += prevWeapon.getCurrentHeat();
                }
            }

            if ((totalheat + weapon.getCurrentHeat()) > heatcap) {
                return "heat exceeds capacity";
            }
        }

        // MG arrays
        if (wtype.hasFlag(WeaponType.F_MGA) && (weapon.getCurrentShots() == 0)) {
            return "no working MGs remaining";
        }
        if (wtype.hasFlag(WeaponType.F_MGA) && wtype.hasModes()
            && weapon.curMode().equals("Off")) {
            return "MG Array is disabled";
        } else if (wtype.hasFlag(WeaponType.F_MG)) {
            if (ae.hasLinkedMGA(weapon)) {
                return "Machine gun is slaved to array equipment";
            }
        }

        // Handle solo attack weapons.
        if (wtype.hasFlag(WeaponType.F_SOLO_ATTACK)) {
            for (Enumeration<EntityAction> i = game.getActions(); i
                    .hasMoreElements(); ) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if (prevAttack.getEntityId() == attackerId) {
                    // If the attacker fires another weapon, this attack fails.
                    if (weaponId != prevAttack.getWeaponId()) {
                        return "Other weapon attacks declared.";
                    }
                }
            }
        } else if (isAttackerInfantry && !(ae instanceof BattleArmor)) {
            // 0 MP infantry units: move or shoot, except for anti-mech attacks,
            // those are handled above
            if ((ae.getMovementMode() == EntityMovementMode.INF_LEG)
                && (ae.getWalkMP() == 0)
                && (ae.moved != EntityMovementType.MOVE_NONE)) {
                return "Foot platoons with 0 MP can move or shoot, not both";
            }
            if (game.getOptions().booleanOption("tacops_fast_infantry_move")
                && (ae.moved == EntityMovementType.MOVE_RUN)) {
                return "Infantry fast moved this turn and so can not shoot.";
            }
            // check for trying to fire field gun after moving
            if ((weapon.getLocation() == Infantry.LOC_FIELD_GUNS)
                && (ae.moved != EntityMovementType.MOVE_NONE)) {
                return "Can't fire field guns in same turn as moving";
            }
            // check for mixing infantry and field gun attacks
            float fieldGunWeight = 0.0f;
            for (Enumeration<EntityAction> i = game.getActions(); i
                    .hasMoreElements(); ) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                final WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if (prevAttack.getEntityId() == attackerId) {
                    Mounted prevWeapon = ae.getEquipment(prevAttack
                                                                 .getWeaponId());
                    if ((prevWeapon.getType().hasFlag(WeaponType.F_INFANTRY)
                         && (weapon.getLocation() == Infantry.LOC_FIELD_GUNS))
                        || (weapon.getType().hasFlag(WeaponType.F_INFANTRY)
                            && (prevWeapon.getLocation() == Infantry.LOC_FIELD_GUNS))) {
                        return "Can't fire field guns and small arms at the same time.";
                    }
                    if ((weapon.getLocation() == Infantry.LOC_FIELD_GUNS)
                        && (weaponId != prevAttack.getWeaponId())) {
                        fieldGunWeight += prevWeapon.getType().getTonnage(ae);
                    }
                }
            }
            // the total tonnage of field guns fired has to be less than or
            // equal to the men in the platoon
            if (weapon.getLocation() == Infantry.LOC_FIELD_GUNS) {
                if (((Infantry) ae).getShootingStrength() < Math
                        .ceil(fieldGunWeight + wtype.getTonnage(ae))) {
                    return "Not enough men remaining to fire this field gun";
                }
            }
            // BA compact narc: we have one weapon for each trooper, but you
            // can fire only at one target at a time
            if (wtype.getName().equals("Compact Narc")) {
                for (Enumeration<EntityAction> i = game.getActions(); i
                        .hasMoreElements(); ) {
                    EntityAction ea = i.nextElement();
                    if (!(ea instanceof WeaponAttackAction)) {
                        continue;
                    }
                    final WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                    if (prevAttack.getEntityId() == attackerId) {
                        Mounted prevWeapon = ae.getEquipment(prevAttack
                                                                     .getWeaponId());
                        if (prevWeapon.getType().getName()
                                      .equals("Compact Narc")) {
                            if (prevAttack.getTargetId() != target
                                    .getTargetId()) {
                                return "Can fire multiple compact narcs only at one target.";
                            }
                        }
                    }
                }
            }
        }

        // check wind conditions
        int windCond = game.getPlanetaryConditions().getWindStrength();
        if ((windCond == PlanetaryConditions.WI_TORNADO_F13)
            && wtype.hasFlag(WeaponType.F_MISSILE)
            && !game.getBoard().inSpace()) {
            return "No missile fire in a tornado";
        }

        if ((windCond == PlanetaryConditions.WI_TORNADO_F4)
            && !game.getBoard().inSpace()
            && (wtype.hasFlag(WeaponType.F_MISSILE) || wtype
                .hasFlag(WeaponType.F_BALLISTIC))) {
            return "No missile or ballistic fire in an F4 tornado";
        }

        // check if indirect fire is valid
        if (isIndirect && !game.getOptions().booleanOption("indirect_fire")) {
            return "Indirect fire option not enabled";
        }

        if (isIndirect
            && game.getOptions().booleanOption("indirect_fire")
            && !game.getOptions().booleanOption("indirect_always_possible")
            && LosEffects.calculateLos(game, attackerId, target).canSee()
            && (!game.getOptions().booleanOption("double_blind") || Compute
                .canSee(game, ae, target))
            && !(wtype instanceof ArtilleryCannonWeapon)
            && !(wtype instanceof MekMortarWeapon)) {
            return "Indirect fire impossible with direct LOS";
        }

        if (isIndirect && usesAmmo && (atype.getAmmoType() == AmmoType.T_MML)
            && !atype.hasFlag(AmmoType.F_MML_LRM)) {
            return "only LRM ammo can be fired indirectly";
        }

        // hull down vees can't fire front weapons
        if ((ae instanceof Tank) && ae.isHullDown()
            && (weapon.getLocation() == Tank.LOC_FRONT)) {
            return "Nearby terrain blocks front weapons.";
        }

        // BA Micro bombs only when flying
        if ((atype != null)
            && (atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB)) {
            if (!ae.isAirborneVTOLorWIGE()) {
                return "attacker must be at least at elevation 1";
            } else if (target.getTargetType() != Targetable.TYPE_HEX_BOMB) {
                return "must target hex with bombs";
            } else if (ae.getElevation() != 1) {
                return "must be exactly 1 elevation above targeted hex";
            }
        }

        if ((target.getTargetType() == Targetable.TYPE_HEX_BOMB)
            && !(usesAmmo && (atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB))) {
            return "Weapon can't deliver bombs";
        }

        if ((target.getTargetType() == Targetable.TYPE_HEX_AERO_BOMB)
            && !wtype.hasFlag(WeaponType.F_DIVE_BOMB)
            && !wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
            return "Weapon can't be used to bomb";
        }

        if (wtype.hasFlag(WeaponType.F_DIVE_BOMB)
            || wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
            if (ae.getBombs(AmmoType.F_GROUND_BOMB).isEmpty()) {
                return "no bombs left to drop";
            }
            if ((ae instanceof Aero) && ((Aero) ae).isSpheroid()) {
                return "spheroid units cannot make bombing attacks";
            }
            if (!ae.isAirborne()) {
                return "no bombing for grounded units";
            }

            if (target.getTargetType() != Targetable.TYPE_HEX_AERO_BOMB) {
                return "only hexes can be targeted for bomb attacks";
            }
            if (!ae.passedOver(target)) {
                return "bombing only possible along flight path";
            }

            if (wtype.hasFlag(WeaponType.F_DIVE_BOMB)) {
                if (ae.getAltitude() > 5) {
                    return "no dive bombing above altitude 5";
                }
                if (ae.getAltitude() < 3) {
                    return "no dive bombing below altitude 3";
                }
            }
        }

        Entity spotter = null;
        if (isIndirect) {
            if ((target instanceof Entity)
                && !isTargetECMAffected
                && usesAmmo
                && (atype.getMunitionType() == AmmoType.M_NARC_CAPABLE)
                && ((te.isNarcedBy(ae.getOwner().getTeam())) || (te
                    .isINarcedBy(ae.getOwner().getTeam())))) {
                spotter = te;
            } else {
                spotter = Compute.findSpotter(game, ae, target);
            }

            if ((spotter == null) && (atype != null)
                && ((atype.getAmmoType() == AmmoType.T_LRM)
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

            if ((spotter == null) &&
                !(wtype instanceof MekMortarWeapon)
                && !(wtype instanceof ArtilleryCannonWeapon)) {
                return "No available spotter";
            }
        }

        int eistatus = 0;

        boolean multiPurposeelevationHack = false;
        if (usesAmmo
            && (wtype.getAmmoType() == AmmoType.T_LRM)
            && (atype.getMunitionType() == AmmoType.M_MULTI_PURPOSE)
            && (ae.getElevation() == -1)
            && (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET)) {
            multiPurposeelevationHack = true;
            // surface to fire
            ae.setElevation(0);
        }

        // check LOS (indirect LOS is from the spotter)
        LosEffects los;
        ToHitData losMods;
        if (!isIndirect || (isIndirect && (spotter == null))) {
            if (!exchangeSwarmTarget) {
                los = LosEffects.calculateLos(game, attackerId, target);
            } else {
                // Swarm should draw LoS between targets, not attacker, since
                // we don't want LoS to be blocked
                if (swarmPrimaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                    los = LosEffects.calculateLos(game,
                            swarmPrimaryTarget.getTargetId(),
                            swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLos(game,
                            swarmSecondaryTarget.getTargetId(),
                            swarmPrimaryTarget);
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
            if (!exchangeSwarmTarget) {
                los = LosEffects.calculateLos(game, spotter.getId(), target);
            } else {
                // Swarm should draw LoS between targets, not attacker, since
                // we don't want LoS to be blocked
                if (swarmPrimaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                    los = LosEffects.calculateLos(game,
                            swarmPrimaryTarget.getTargetId(),
                            swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLos(game,
                            swarmSecondaryTarget.getTargetId(),
                            swarmPrimaryTarget);
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

        if (multiPurposeelevationHack) {
            // and descend back to depth 1
            ae.setElevation(-1);
        }

        // if LOS is blocked, block the shot
        if ((losMods.getValue() == TargetRoll.IMPOSSIBLE)
                && !isArtilleryIndirect) {
            return losMods.getDesc();
        }

        // http://www.classicbattletech.com/forums/index.php/topic,47618.0.html
        // anything outside of visual range requires a "sensor lock" in order to
        // direct fire
        if (game.getOptions().booleanOption("double_blind")
            && !Compute.inVisualRange(game, ae, target)
            && !(Compute.inSensorRange(game, ae, target, null) // Can shoot at something in sensor range
                    && (te != null) && te.hasSeenEntity(ae.getOwner())) // if it has been spotted by another unit
            && !isArtilleryIndirect && !isIndirect) {
            boolean networkSee = false;
            if (ae.hasC3() || ae.hasC3i() || ae.hasActiveNovaCEWS()) {
                // c3 units can fire if any other unit in their network is in
                // visual or sensor range
                for (Entity en : game.getEntitiesVector()) {
                    if (!en.isEnemyOf(ae) && en.onSameC3NetworkAs(ae)
                        && Compute.canSee(game, en, target)) {
                        networkSee = true;
                        break;
                    }
                }
            }
            if (!networkSee) {
                if (!Compute.inSensorRange(game, ae, target, null)) {
                    return "outside of visual and sensor range";
                } else {
                    return "target has not been spotted";
                }
            }
        }

        // Weapon in arc?
        if (!Compute.isInArc(game, attackerId, weaponId, target)
            && (!Compute.isAirToGround(ae, target) || isArtilleryIndirect)) {
            return "Target not in arc.";
        }

        if (Compute.isAirToGround(ae, target) && !isArtilleryIndirect) {
            if ((ae.getAltitude() > 5) && !wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                return "attacker is too high";
            }
            if ((ae.getAltitude() > 3) && isStrafing) {
                return "attacker is too high";
            }
            // Additional Nape-of-Earth restrictions for strafing
            if (ae.getAltitude() == 1 && isStrafing) {
                Vector<Coords> passedThrough = ae.getPassedThrough();
                if ((passedThrough.size() == 0)
                        || passedThrough.get(0).equals(target.getPosition())) {
                    // TW pg 243 says units flying at NOE have a harder time
                    // establishing LoS while strafing and hence have to
                    // consider the adjacent hex along the flight place in the
                    // direction of the attack.  What if there is no adjacent
                    // hex?  The rules don't address this.  We could
                    // theoretically consider last turns movement, but that's
                    // cumbersome, so we'll just assume it's impossible - Arlith
                    return "target is too close to strafe";
                }
                // Otherwise, check for a dead-zone, TW pg 243
                Coords prevCoords = ae.passedThroughPrevious(target
                        .getPosition());
                IHex prevHex = game.getBoard().getHex(prevCoords);
                IHex currHex = game.getBoard().getHex(target.getPosition());
                int prevElev = prevHex.getLevel();
                int currElev = currHex.getLevel();
                if ((prevElev - currElev - target.relHeight()) > 2) {
                    return "target is in dead-zone";
                }
            }

            // Only direct-fire energy weapons can strafe
            EquipmentType wt = weapon.getType();
            boolean isDirectFireEnergy = wt.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && (wt.hasFlag(WeaponType.F_LASER)
                            || wt.hasFlag(WeaponType.F_PPC)
                            || wt.hasFlag(WeaponType.F_PLASMA)
                            || wt.hasFlag(WeaponType.F_PLASMA_MFUK)
                            || wt.hasFlag(WeaponType.F_FLAMER));
            boolean isEnergyBay = (wt instanceof LaserBayWeapon)
                    || (wt instanceof PPCBayWeapon)
                    || (wt instanceof PulseLaserBayWeapon);
            if (isStrafing && !isDirectFireEnergy && !isEnergyBay) {
                return "only direct-fire energy weapons can strafe!";
            }

            // only certain weapons can be used for air to ground attacks
            if (ae instanceof Aero) {
                // Spheroids can't strafe
                if (isStrafing && ((Aero) ae).isSpheroid()) {
                    return "spheroid craft are not allowed to strafe!";
                }
                if (((Aero) ae).isSpheroid()) {
                    if ((weapon.getLocation() != Aero.LOC_AFT)
                        && !weapon.isRearMounted()) {
                        return "only aft and rear mounted weapons can be fired air to ground from spheroid";
                    }
                } else {
                    if ((weapon.getLocation() == Aero.LOC_AFT)
                        || weapon.isRearMounted()) {
                        return "only forward firing weapons can be fired air to ground from an aerodyne";
                    }
                }
            }

            // for air to ground attacks, the target's position must be within
            // the flight path, unless it is an artillery weapon in the nose.
            // http://www.classicbattletech.com/forums/index.php?topic=65110.0
            if (!ae.passedOver(target)) {
                if (!wtype.hasFlag(WeaponType.F_ARTILLERY)) {
                    return "target not along flight path";
                } else if (weapon.getLocation() != Aero.LOC_NOSE) {
                    return "target not along flight path";
                }
            }

            int altitudeLoss = 1;
            if (wtype.hasFlag(WeaponType.F_DIVE_BOMB)) {
                altitudeLoss = 2;
            }
            if (wtype.hasFlag(WeaponType.F_ALT_BOMB) || isStrafing) {
                altitudeLoss = 0;
            }
            //you cant make attacks that would lower you to zero altitude
            if (altitudeLoss >= ae.getAltitude()) {
                return "This attack would cause too much altitude loss";
            }

            // can only make a strike attack against a single target
            if (!isStrafing) {
                for (Enumeration<EntityAction> i = game.getActions(); i
                        .hasMoreElements(); ) {
                    EntityAction ea = i.nextElement();
                    if (!(ea instanceof WeaponAttackAction)) {
                        continue;
                    }
                    WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                    if ((prevAttack.getEntityId() == ae.getId()) && (null != te)
                        && (prevAttack.getTargetId() != te.getId())) {
                        return "attack already declared against another target";
                    }
                }
            }
        }

        // only one ground-to-air attack allowed per turn
        // grounded spheroid dropships dont have this limitation
        if (!ae.isAirborne()
            && !((ae instanceof Dropship) && ((Aero) ae).isSpheroid())) {
            for (Enumeration<EntityAction> i = game.getActions(); i
                    .hasMoreElements(); ) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if (prevAttack.getEntityId() == ae.getId()) {
                    if (prevAttack.isGroundToAir(game)
                        && !Compute.isGroundToAir(ae, target)) {
                        return "ground-to-air attack already declared";
                    }
                    if (!prevAttack.isGroundToAir(game)
                        && Compute.isGroundToAir(ae, target)) {
                        return "ground-to-ground attack already declared";
                    }
                    if (prevAttack.isGroundToAir(game)
                        && Compute.isGroundToAir(ae, target)
                        && (null != te)
                        && (prevAttack.getTargetId() != te.getId())) {
                        return "only one target allowed for all ground-to-air attacks";
                    }
                }
            }
        }

        //air2air and air2ground cannot be combined by any aerospace units
        if (Compute.isAirToAir(ae, target) || Compute.isAirToGround(ae, target)) {
            for (Enumeration<EntityAction> i = game.getActions(); i
                    .hasMoreElements(); ) {
                EntityAction ea = i.nextElement();
                if (!(ea instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                if (prevAttack.getEntityId() != ae.getId()) {
                    continue;
                }
                if (Compute.isAirToAir(ae, target) && prevAttack.isAirToGround(game)) {
                    return "air-to-ground attack already declared";
                }
                if (Compute.isAirToGround(ae, target) && prevAttack.isAirToAir(game)) {
                    return "air-to-air attack already declared";
                }
            }
        }

        if ((target.getAltitude() > 8) && Compute.isGroundToAir(ae, target)) {
            return "cannot target aero units beyond altitude 8";
        }

        if ((ae instanceof Infantry) && Compute.isGroundToAir(ae, target)
                && !wtype.hasFlag(WeaponType.F_INF_AA)
                && !isArtilleryFLAK // Can make GroundToAir Flak attacks 
                && !((atype != null) 
                        && (atype.getAmmoType() == AmmoType.T_AC_LBX))) {
            return "Infantry cannot engage in ground-to-air attacks";
        }

        // Protomech can fire MGA only into front arc, TW page 137
        if (!Compute.isInArc(ae.getPosition(), ae.getFacing(), target,
                             Compute.ARC_FORWARD)
            && wtype.hasFlag(WeaponType.F_MGA)
            && (ae instanceof Protomech)) {
            return "Protomech can fire MGA only into front arc.";
        }

        // for spheroid dropships in atmosphere (and on ground), the rules about
        // firing arcs are more complicated
        // TW errata 2.1
        if ((ae instanceof Aero) && ((Aero) ae).isSpheroid()
            && !game.getBoard().inSpace()) {
            int altDif = target.getAltitude() - ae.getAltitude();
            int distance = Compute.effectiveDistance(game, ae, target, false);
            if (!ae.isAirborne() && (distance == 0)
                && (weapon.getLocation() != Aero.LOC_AFT)) {
                return "Only aft weapons may target units at zero range";
            }
            if ((weapon.getLocation() == Aero.LOC_NOSE)
                && (altDif < 1)
                && !((wtype instanceof ArtilleryWeapon) || wtype
                    .hasFlag(WeaponType.F_ARTILLERY))) {
                return "Target is too low for nose weapons";
            }
            if ((!weapon.isRearMounted() && (weapon.getLocation() != Aero.LOC_AFT))
                && (altDif < 0)) {
                return "Target is too low for front-side weapons";
            }
            if ((weapon.getLocation() == Aero.LOC_AFT)) {
                if (ae.isAirborne() && (altDif > -1)) {
                    return "Target is too high for aft weapons";
                }
            }
            if ((weapon.isRearMounted()) && (altDif > 0)) {
                return "Target is too high for aft-side weapons";
            }
            if (Compute.inDeadZone(game, ae, target)) {
                if ((altDif > 0) && (weapon.getLocation() != Aero.LOC_NOSE)) {
                    return "only nose weapons can target higher units in the dead zone";
                }
                if ((altDif < 0) && (weapon.getLocation() != Aero.LOC_AFT)) {
                    return "only aft weapons can target lower units in the dead zone";
                }
            }

        }

        // Must target infantry in buildings from the inside.
        if (targetInBuilding && (te instanceof Infantry)
            && (null == los.getThruBldg())) {
            return "Attack on infantry crosses building exterior wall.";
        }

        if ((wtype.getAmmoType() == AmmoType.T_NARC)
            || (wtype.getAmmoType() == AmmoType.T_INARC)) {
            if (targetInBuilding) {
                return "Narc pods cannot be fired into or inside buildings.";
            }
            if (target instanceof Infantry) {
                return "Narc pods cannot be used to attack infantry.";
            }
        }

        // attacker partial cover means no leg weapons
        if (los.isAttackerCover() && ae.locationIsLeg(weapon.getLocation())
            && !underWater) {
            return "Nearby terrain blocks leg weapons.";
        }

        // hull down cannot fire any leg weapons
        if (ae.isHullDown()) {
            if (((ae instanceof BipedMech) && ((weapon.getLocation() == Mech.LOC_LLEG) || (weapon
                    .getLocation() == Mech.LOC_RLEG)))
                    || ((ae instanceof QuadMech) && ((weapon.getLocation() == Mech.LOC_LLEG)
                            || (weapon.getLocation() == Mech.LOC_RLEG)
                            || (weapon.getLocation() == Mech.LOC_LARM) || (weapon
                            .getLocation() == Mech.LOC_RARM)))) {
                return "Leg weapons cannot be fired while hull down.";
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
            if (!WeaponAttackAction.isOnlyAttack(game, ae, Infantry.LEG_ATTACK,
                                                 te)) {
                return "Leg attack must be an unit's only attack, and there must not be multiple Leg Attacks.";
            }
        } else if (Infantry.SWARM_MEK.equals(wtype.getInternalName())) {
            toHit = Compute.getSwarmMekBaseToHit(ae, te, game);

            // Return if the attack is impossible.
            if (TargetRoll.IMPOSSIBLE == toHit.getValue()) {
                return toHit.getDesc();
            }
            if (!WeaponAttackAction.isOnlyAttack(game, ae, Infantry.SWARM_MEK,
                                                 te)) {
                return "Swarm attack must be an unit's only attack, and there must not be multiple Swarm Attacks.";
            }
        } else if (Infantry.STOP_SWARM.equals(wtype.getInternalName())) {
            // Can't stop if we're not swarming, otherwise automatic.
            if (Entity.NONE == ae.getSwarmTargetId()) {
                return "Not swarming a Mek.";
            }
        } else if (Infantry.SWARM_WEAPON_MEK.equals(wtype.getInternalName())) {
            // Can't stop if we're not swarming, otherwise automatic.
            if (Entity.NONE == ae.getSwarmTargetId()) {
                return "Not swarming a Mek.";
            }
        } else if (BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName())) {
            // Mine launchers can not hit infantry.
            if (te instanceof Infantry) {
                return "Can not attack infantry.";
            }
        }
        // Swarming infantry always hit their target, but
        // they can only target the Mek they're swarming.
        else if ((te != null) && (ae.getSwarmTargetId() == te.getId())) {
            // Only certain weapons can be used in a swarm attack.
            if (wtype.getDamage() == 0) {
                return "Weapon causes no damage.";
            }
            // Only certain weapons can be used in a swarm attack.
            if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                return "Missile weapons can't be used in swarm attack";
            }
            if (weapon.isBodyMounted()) {
                return "body mounted weapons can't be used in swarm attack";
            }
        } else if (Entity.NONE != ae.getSwarmTargetId()) {
            return "Must target the Mek being swarmed.";
        }

        int distance = Compute.effectiveDistance(game, ae, target);

        // Handle direct artillery attacks.
        if (isArtilleryDirect) {
            if (wtype.hasFlag(WeaponType.F_CRUISE_MISSILE)) {
                return "Cruise Missiles can't be fired directly";
            }
            if (distance > 17) {
                return "Direct artillery attack at range >17 hexes.";
            }
            if (isHoming) {
                if ((te == null) || (te.getTaggedBy() == -1)) {
                    // see BMRr p77 on board arrow IV
                    return "On board homing shot must target a unit tagged this turn";
                }
            }
        }
        if (isArtilleryIndirect) {
            int boardRange = (int) Math.ceil(distance / 17f);
            if (boardRange > wtype.getLongRange()) {
                return "Indirect artillery attack out of range";
            }
            if (((distance <= 17) && !ae.isAirborne())
                && !(losMods.getValue() == TargetRoll.IMPOSSIBLE)) {
                return "Cannot fire indirectly at range <=17 hexes unless no LOS.";
            }
            if (ae.isAirborne() && (ae.getAltitude() >= 10)) {
                return "Cannot fire indirectly at altitude 10";
            }
            if (isHoming) {
                if (ttype != Targetable.TYPE_HEX_ARTILLERY) {
                    return "Off board homing shot must target a map sheet";
                }
            }
        }

        if (ae.getGrappled() != Entity.NONE) {
            int grapple = ae.getGrappled();
            if (grapple != target.getTargetId()) {
                return "Can only attack grappled mech";
            }
            int loc = weapon.getLocation();
            if (((ae instanceof Mech)
                 && (ae.getGrappleSide() == Entity.GRAPPLE_BOTH) && ((loc != Mech.LOC_CT)
                                                                     && (loc != Mech.LOC_LT) && (loc != Mech.LOC_RT) && (loc != Mech.LOC_HEAD)))
                || weapon.isRearMounted()) {
                return "Can only fire head and front torso weapons when grappled";
            }
            if ((ae instanceof Mech)
                && (ae.getGrappleSide() == Entity.GRAPPLE_LEFT)
                && (loc == Mech.LOC_LARM)) {
                return "Cannot Fire Weapon, Snared by Chain Whip";
            }
            if ((ae instanceof Mech)
                && (ae.getGrappleSide() == Entity.GRAPPLE_RIGHT)
                && (loc == Mech.LOC_RARM)) {
                return "Cannot Fire Weapon, Snared by Chain Whip";
            }
        }
        if ((ae.getMovementMode() == EntityMovementMode.WIGE)
            && (ae.getPosition() == target.getPosition())) {
            return "WiGE may not attack target in same hex";
        }

        if ((wtype instanceof GaussWeapon) && wtype.hasModes()
            && weapon.curMode().equals("Powered Down")) {
            return "Weapon is powered down";
        }

        if ((target.getTargetType() == Targetable.TYPE_ENTITY)
            && wtype.hasFlag(WeaponType.F_MASS_DRIVER)
            && (ae instanceof SpaceStation)) {
            if (!ae.getPosition().translated(ae.getFacing(), distance).equals(
                    target.getPosition())) {
                return "Mass Driver is not firing to front";
            }
        }
        
        // Some Mek mortar ammo types can only be aimed at a hex
        if (weapon.getType().hasFlag(WeaponType.F_MEK_MORTAR) && (atype != null)
                && ((atype.getMunitionType() == AmmoType.M_AIRBURST)
                        || (atype.getMunitionType() == AmmoType.M_FLARE)
                        || (atype.getMunitionType() == AmmoType.M_SMOKE))) {
            if (!(target instanceof HexTarget)) {
                return atype.getSubMunitionName()
                        + " munitions must target a hex!";
            }
        }

        if (weapon.getType().hasFlag(WeaponType.F_PPC) 
                && (weapon.getLinkedBy() != null)
                && weapon.getLinkedBy().getType().hasFlag(MiscType.F_PPC_CAPACITOR)
                && weapon.getLinkedBy().pendingMode().equals("Charge")) {
            return "PPCs with charging capacitors cannot fire!";
        }
        
        return null;
    }

    /**
     * Some attacks are the only actions that a particular entity can make
     * during its turn Also, only this unit can make that particular attack.
     */
    private static boolean isOnlyAttack(IGame game, Entity attacker,
                                        String attackType, Entity target) {
        // mechs can only be the target of one leg or swarm attack
        for (Enumeration<EntityAction> actions = game.getActions(); actions
                .hasMoreElements(); ) {
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
                    if (!waaAE.getEquipment(waa.getWeaponId())
                            .getType().getInternalName().equals(attackType)) {
                        return false;
                    }
                }
                Targetable waaTarget = waa.getTarget(game);
                EquipmentType weapType = waaAE.getEquipment(waa.getWeaponId())
                        .getType();
                if (weapType.getInternalName().equals(attackType)
                    && (waaTarget != null) && waaTarget.equals(target)) {
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
     * @param nemesisConfused The nemesisConfused to set.
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
}
