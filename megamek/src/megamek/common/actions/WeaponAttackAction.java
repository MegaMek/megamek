/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.actions;

import megamek.MMConstants;
import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.enums.AimingMode;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.Light;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.planetaryconditions.Wind;
import megamek.common.weapons.DiveBombAttack;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;
import megamek.common.weapons.artillery.ArtilleryWeapon;
import megamek.common.weapons.battlearmor.CLBALBX;
import megamek.common.weapons.bayweapons.*;
import megamek.common.weapons.capitalweapons.CapitalMissileWeapon;
import megamek.common.weapons.gaussrifles.GaussWeapon;
import megamek.common.weapons.lasers.ISBombastLaser;
import megamek.common.weapons.lasers.VariableSpeedPulseLaserWeapon;
import megamek.common.weapons.lrms.LRTWeapon;
import megamek.common.weapons.srms.SRTWeapon;
import org.apache.logging.log4j.LogManager;

import java.io.Serializable;
import java.util.*;

/**
 * Represents intention to fire a weapon at the target.
 */
public class WeaponAttackAction extends AbstractAttackAction implements Serializable {
    public static int DEFAULT_VELOCITY = 50;
    private static final long serialVersionUID = -9096603813317359351L;

    public static final int STRATOPS_SENSOR_SHADOW_WEIGHT_DIFF = 100000;

    private int weaponId;
    private int ammoId = -1;
    private EnumSet<AmmoType.Munitions> ammoMunitionType = EnumSet.noneOf(AmmoType.Munitions.class);
    private int ammoCarrier = -1;
    private int aimedLocation = Entity.LOC_NONE;
    private AimingMode aimMode = AimingMode.NONE;
    private int otherAttackInfo = -1;
    private boolean nemesisConfused;
    private boolean swarmingMissiles;
    protected int launchVelocity = DEFAULT_VELOCITY;
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
    private HashMap<String, int[]> bombPayloads = new HashMap<String, int[]>();

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

    // default to attacking an entity
    public WeaponAttackAction(int entityId, int targetId, int weaponId) {
        super(entityId, targetId);
        this.weaponId = weaponId;
        this.bombPayloads.put("internal", new int[BombType.B_NUM]);
        this.bombPayloads.put("external", new int[BombType.B_NUM]);
    }

    public WeaponAttackAction(int entityId, int targetType, int targetId, int weaponId) {
        super(entityId, targetType, targetId);
        this.weaponId = weaponId;
        this.bombPayloads.put("internal", new int[BombType.B_NUM]);
        this.bombPayloads.put("external", new int[BombType.B_NUM]);
    }

    public int getWeaponId() {
        return weaponId;
    }

    public int getAmmoId() {
        return ammoId;
    }

    public EnumSet<AmmoType.Munitions> getAmmoMunitionType() {
        return ammoMunitionType;
    }

    /**
     * Returns the entity id of the unit carrying the ammo used by this attack
     * @return
     */
    public int getAmmoCarrier() {
        return ammoCarrier;
    }

    public int getAimedLocation() {
        return aimedLocation;
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

    public void setAmmoMunitionType(EnumSet<AmmoType.Munitions> ammoMunitionType) {
        this.ammoMunitionType = ammoMunitionType;
    }

    /**
     * Sets the entity id of the ammo carrier for this shot, if different than the firing entity
     * @param entityId
     */
    public void setAmmoCarrier(int entityId) {
        this.ammoCarrier = entityId;
    }

    public void setAimedLocation(int aimedLocation) {
        this.aimedLocation = aimedLocation;
    }

    public AimingMode getAimingMode() {
        return aimMode;
    }

    public void setAimingMode(AimingMode aimMode) {
        this.aimMode = aimMode;
    }

    public void addCounterEquipment(Mounted m) {
        if (vCounterEquipment == null) {
            vCounterEquipment = new ArrayList<>();
        }
        vCounterEquipment.add(m);
    }

    public void setOtherAttackInfo(int newInfo) {
        otherAttackInfo = newInfo;
    }

    public int getOtherAttackInfo() {
        return otherAttackInfo;
    }

    public boolean isAirToGround(Game game) {
        return Compute.isAirToGround(getEntity(game), getTarget(game));
    }

    public boolean isAirToAir(Game game) {
        return Compute.isAirToAir(getEntity(game), getTarget(game));
    }

    public boolean isGroundToAir(Game game) {
        return Compute.isGroundToAir(getEntity(game), getTarget(game));
    }

    public boolean isDiveBomb(Game game) {
        return ((WeaponType) getEntity(game).getEquipment(getWeaponId()).getType()).hasFlag(WeaponType.F_DIVE_BOMB);
    }

    public int getAltitudeLoss(Game game) {
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

    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()),
                getWeaponId(), getAimedLocation(), getAimingMode(), nemesisConfused, swarmingMissiles,
                game.getTarget(getOldTargetType(), getOldTargetId()),
                game.getTarget(getOriginalTargetType(), getOriginalTargetId()), isStrafing(), isPointblankShot(),
                this.ammoId);
    }

    /**
     *
     * @param game
     * @param evenIfAlreadyFired false: an already fired weapon will return a ToHitData with value IMPOSSIBLE
     *                          true: an already fired weapon will return a ToHitData with the value of its chance to hit
     */
    public ToHitData toHit(Game game, boolean evenIfAlreadyFired) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()),
                getWeaponId(), getAimedLocation(), getAimingMode(), nemesisConfused, swarmingMissiles,
                game.getTarget(getOldTargetType(), getOldTargetId()),
                game.getTarget(getOriginalTargetType(), getOriginalTargetId()), isStrafing(), isPointblankShot(), evenIfAlreadyFired, this.ammoId);
    }

    public ToHitData toHit(Game game, List<ECMInfo> allECMInfo) {
        return toHitCalc(game, getEntityId(), game.getTarget(getTargetType(), getTargetId()),
                getWeaponId(), getAimedLocation(), getAimingMode(), nemesisConfused, swarmingMissiles,
                game.getTarget(getOldTargetType(), getOldTargetId()),
                game.getTarget(getOriginalTargetType(), getOriginalTargetId()), isStrafing(), isPointblankShot(),
                allECMInfo, false, this.ammoId);
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target, int weaponId, boolean isStrafing) {
        // Use -1 as ammoId because this method should always use the currently linked ammo for display calcs
        return toHit(game, attackerId, target, weaponId, Entity.LOC_NONE, AimingMode.NONE,
                false, false, null, null, isStrafing, false, -1);
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target, int weaponId,
                                  int aimingAt, AimingMode aimingMode, boolean isStrafing) {
        // Use -1 as ammoId because this method should always use the currently linked ammo for display calcs
        return toHit(game, attackerId, target, weaponId, aimingAt, aimingMode, false,
                false, null, null, isStrafing, false, -1);
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target, int weaponId,
                                  int aimingAt, AimingMode aimingMode, boolean isNemesisConfused,
                                  boolean exchangeSwarmTarget, Targetable oldTarget,
                                  Targetable originalTarget, boolean isStrafing, boolean isPointblankShot,
                                  int ammoId) {
        return toHitCalc(game, attackerId, target, weaponId, aimingAt, aimingMode, isNemesisConfused,
                exchangeSwarmTarget, oldTarget, originalTarget, isStrafing, isPointblankShot, null, false, ammoId);
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target, int weaponId,
                                  int aimingAt, AimingMode aimingMode, boolean isNemesisConfused,
                                  boolean exchangeSwarmTarget, Targetable oldTarget,
                                  Targetable originalTarget, boolean isStrafing, boolean isPointblankShot, boolean evenIfAlreadyFired,
                                  int ammoId) {
        return toHitCalc(game, attackerId, target, weaponId, aimingAt, aimingMode, isNemesisConfused,
                exchangeSwarmTarget, oldTarget, originalTarget, isStrafing, isPointblankShot, null, evenIfAlreadyFired, ammoId);
    }

    /**
     * To-hit number for attacker firing a weapon at the target.
     */
    private static ToHitData toHitCalc(Game game, int attackerId, Targetable target, int weaponId,
                                   int aimingAt, AimingMode aimingMode, boolean isNemesisConfused,
                                   boolean exchangeSwarmTarget, Targetable oldTarget,
                                   Targetable originalTarget, boolean isStrafing,
                                   boolean isPointblankShot, List<ECMInfo> allECMInfo, boolean evenIfAlreadyFired,
                                   int ammoId) {
        final Entity ae = game.getEntity(attackerId);
        final Mounted weapon = ae.getEquipment(weaponId);
        final Mounted linkedAmmo = (ammoId == -1) ? weapon.getLinked() : ae.getEquipment(ammoId);

        final EquipmentType type = weapon.getType();

        // No need to process anything further if we're not using a weapon somehow
        if (!(type instanceof WeaponType)) {
            LogManager.getLogger().error("Trying to make a weapon attack with " + weapon.getName() + " which has type " + type.getName());
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, Messages.getString("WeaponAttackAction.NotAWeapon"));
        }

        if (target == null) {
            LogManager.getLogger().error(attackerId + "Attempting to attack null target");
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, Messages.getString("MovementDisplay.NoTarget"));
        }

        final WeaponType wtype = (WeaponType) type;

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

        boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY) && !ae.isSupportVehicle();

        boolean isWeaponFieldGuns = isAttackerInfantry && (weapon.getLocation() == Infantry.LOC_FIELD_GUNS);
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        // 2002-09-16 Infantry weapons have unlimited ammo.

        final boolean usesAmmo = (wtype.getAmmoType() != AmmoType.T_NA) && !isWeaponInfantry;

        final Mounted ammo = usesAmmo ? linkedAmmo : null;

        final AmmoType atype = ammo == null ? null : (AmmoType) ammo.getType();

        EnumSet<AmmoType.Munitions> munition = EnumSet.of(AmmoType.Munitions.M_STANDARD);
        if (atype != null) {
            munition = atype.getMunitionType();
        }

        final boolean targetInBuilding = Compute.isInBuilding(game, te);

        boolean bMekTankStealthActive = false;
        if ((ae instanceof Mech) || (ae instanceof Tank)) {
            bMekTankStealthActive = ae.isStealthActive();
        }

        boolean isFlakAttack = !game.getBoard().inSpace() && (te != null)
                && Compute.isFlakAttack(ae, te)
                && (wtype instanceof CLBALBX
                    || ((atype != null)
                        && (
                            (((atype.getAmmoType() == AmmoType.T_AC_LBX) || (atype.getAmmoType() == AmmoType.T_AC_LBX_THB)
                                || (atype.getAmmoType() == AmmoType.T_SBGAUSS))
                                && (munition.contains(AmmoType.Munitions.M_CLUSTER))
                            )
                            || munition.contains(AmmoType.Munitions.M_FLAK) || (atype.getAmmoType() == AmmoType.T_HAG)
                            || atype.countsAsFlak()
                        ))
                );

        boolean isIndirect = weapon.hasModes() && (weapon.curMode().isIndirect());

        // BMM p. 31, semi-guided indirect missile attacks vs tagged targets ignore terrain modifiers
        boolean semiGuidedIndirectVsTaggedTarget = isIndirect &&
                (atype != null) && atype.getMunitionType().contains(AmmoType.Munitions.M_SEMIGUIDED) &&
                Compute.isTargetTagged(target, game);

        boolean isInferno = ((atype != null)
                && ((atype.getAmmoType() == AmmoType.T_SRM)
                        || (atype.getAmmoType() == AmmoType.T_SRM_IMP)
                        || (atype.getAmmoType() == AmmoType.T_MML))
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_INFERNO))
                || (isWeaponInfantry && (wtype.hasFlag(WeaponType.F_INFERNO))));

        boolean isArtilleryDirect = (wtype.hasFlag(WeaponType.F_ARTILLERY) ||
                (wtype instanceof CapitalMissileWeapon
                        && Compute.isGroundToGround(ae, target)))
                && game.getPhase().isFiring();

        boolean isArtilleryIndirect = (wtype.hasFlag(WeaponType.F_ARTILLERY) ||
                (wtype instanceof CapitalMissileWeapon
                        && Compute.isGroundToGround(ae, target)))
                && (game.getPhase().isTargeting() || game.getPhase().isOffboard());

        boolean isBearingsOnlyMissile = (weapon.isInBearingsOnlyMode())
                && (game.getPhase().isTargeting() || game.getPhase().isFiring());

        boolean isCruiseMissile = (weapon.getType().hasFlag(WeaponType.F_CRUISE_MISSILE)
                        || (wtype instanceof CapitalMissileWeapon
                                && Compute.isGroundToGround(ae, target)));

        // hack, otherwise when actually resolves shot labeled impossible.
        boolean isArtilleryFLAK = isArtilleryDirect && (te != null)
                && Compute.isFlakAttack(ae, te)
                && (atype != null) && (usesAmmo && (atype.countsAsFlak()));

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
        boolean isHoming = ammo != null && ammo.isHomingAmmoInHomingMode();

        boolean bHeatSeeking = (atype != null)
                && ((atype.getAmmoType() == AmmoType.T_SRM)
                        || (atype.getAmmoType() == AmmoType.T_SRM_IMP)
                        || (atype.getAmmoType() == AmmoType.T_MML)
                        || (atype.getAmmoType() == AmmoType.T_LRM)
                        || (atype.getAmmoType() == AmmoType.T_LRM_IMP))
                && (munition.contains(AmmoType.Munitions.M_HEAT_SEEKING));

        boolean bFTL = (atype != null)
                && ((atype.getAmmoType() == AmmoType.T_MML)
                        || (atype.getAmmoType() == AmmoType.T_LRM)
                        || (atype.getAmmoType() == AmmoType.T_LRM_IMP))
                && (munition.contains(AmmoType.Munitions.M_FOLLOW_THE_LEADER)
                && !ComputeECM.isAffectedByECM(ae, ae.getPosition(), target.getPosition()));

        Mounted mLinker = weapon.getLinkedBy();

        boolean bApollo = ((mLinker != null) && (mLinker.getType() instanceof MiscType) && !mLinker.isDestroyed()
                && !mLinker.isMissing() && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_APOLLO))
                && (atype != null) && (atype.getAmmoType() == AmmoType.T_MRM);

        boolean bArtemisV = ((mLinker != null) && (mLinker.getType() instanceof MiscType) && !mLinker.isDestroyed()
                && !mLinker.isMissing() && !mLinker.isBreached() && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)
                && !isECMAffected && !bMekTankStealthActive && (atype != null)
                && (munition.contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE)));

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
                isHoming = bAmmo != null && bAmmo.getMunitionType().contains(AmmoType.Munitions.M_HOMING);

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
                        && (bAmmo != null) && (bAmmo.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE)));
            }
        }

        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, te);

        //Set up the target's relative elevation/depth
        int targEl;

        if (te == null) {
            Hex hex = game.getBoard().getHex(target.getPosition());

            targEl = hex == null ? 0 : -hex.depth();
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
                    && (munition.contains(AmmoType.Munitions.M_NARC_CAPABLE))) {
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
                    && (munition.contains(AmmoType.Munitions.M_NARC_CAPABLE)
                    && (te.isNarcedBy(ae.getOwner().getTeam()) || te.isINarcedBy(ae.getOwner().getTeam())))) {
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
                    && (munition.contains(AmmoType.Munitions.M_SEMIGUIDED))) {
                for (TagInfo ti : game.getTagInfo()) {
                    if (target.getId() == ti.target.getId()) {
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
                && (munition.contains(AmmoType.Munitions.M_MULTI_PURPOSE))
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
                    los = LosEffects.calculateLos(game, swarmPrimaryTarget.getId(), swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLos(game, swarmSecondaryTarget.getId(), swarmPrimaryTarget);
                }
            }

            if (ae.hasActiveEiCockpit()) {
                if (los.getLightWoods() > 0) {
                    eistatus = 2;
                } else {
                    eistatus = 1;
                }
            }

            if (wtype.hasFlag(WeaponType.F_MORTARTYPE_INDIRECT) && isIndirect) {
                los.setArcedAttack(true);
            }

            losMods = los.losModifiers(game, eistatus, underWater);
        } else {
            if (exchangeSwarmTarget) {
                // Swarm should draw LoS between targets, not attacker, since
                // we don't want LoS to be blocked
                if (swarmPrimaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                    los = LosEffects.calculateLos(game, swarmPrimaryTarget.getId(), swarmSecondaryTarget);
                } else {
                    los = LosEffects.calculateLos(game, swarmSecondaryTarget.getId(), swarmPrimaryTarget);
                }
            } else {
                // For everything else, set up a plain old LOS
                los = LosEffects.calculateLOS(game, spotter, target, true);
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

            if (wtype.hasFlag(WeaponType.F_MORTARTYPE_INDIRECT) || semiGuidedIndirectVsTaggedTarget) {
                los.setArcedAttack(true);
            }

            losMods = los.losModifiers(game, underWater);
        }
        if (mpMelevationHack) {
            // return to depth 1
            ae.setElevation(-1);
        }

        // determine some more variables
        int aElev = ae.getElevation();
        int tElev = target.getElevation();
        int distance = Compute.effectiveDistance(game, ae, target);

        //Set up our initial toHit data
        ToHitData toHit = new ToHitData();

        //Check to see if this attack is impossible and return the reason code
        String reasonImpossible = WeaponAttackAction.toHitIsImpossible(game, ae, attackerId, target, ttype, los, losMods,
                toHit, distance, spotter, wtype, weapon, weaponId, atype, ammo, munition,
                isArtilleryDirect, isArtilleryFLAK, isArtilleryIndirect, isAttackerInfantry, isBearingsOnlyMissile,
                isCruiseMissile, exchangeSwarmTarget, isHoming, isInferno, isIndirect, isStrafing, isTAG,
                targetInBuilding, usesAmmo, underWater, evenIfAlreadyFired);
        if (reasonImpossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, reasonImpossible);
        }

        //Check to see if this attack is automatically successful and return the reason code
        String reasonAutoHit = WeaponAttackAction.toHitIsAutomatic(game, ae, target, ttype, los, distance,
                wtype, weapon, isBearingsOnlyMissile);
        if (reasonAutoHit != null) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, reasonAutoHit);
        }

        SpecialResolutionTracker srt = new SpecialResolutionTracker();
        srt.setSpecialResolution(false);
        //Is this an infantry leg/swarm attack?
        toHit = handleInfantrySwarmAttacks(game, ae, target, ttype, toHit, wtype, srt);
        if (srt.isSpecialResolution()) {
            return toHit;
        }

        //Check to see if this attack was made with a weapon that has special to-hit rules
        toHit = handleSpecialWeaponAttacks(game, ae, target, ttype, los, toHit, wtype, atype, srt);
        if (srt.isSpecialResolution()) {
            return toHit;
        }

        //This attack has now tested possible and doesn't follow any weird special rules,
        //so let's start adding up the to-hit numbers

        //Start with the attacker's weapon skill
        toHit = new ToHitData(ae.getCrew().getGunnery(), Messages.getString("WeaponAttackAction.GunSkill"));
        if (game.getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
            if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                toHit = new ToHitData(ae.getCrew().getGunneryL(), Messages.getString("WeaponAttackAction.GunLSkill"));
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

        // Is this an Artillery attack?
        if (isArtilleryDirect || isArtilleryIndirect) {
            toHit = handleArtilleryAttacks(game, ae, target, ttype, losMods, toHit, wtype, weapon, atype, isArtilleryDirect,
                    isArtilleryFLAK, isArtilleryIndirect, isHoming, usesAmmo, srt);
        }
        if (srt.isSpecialResolution()) {
            return toHit;
        }

        //Mine launchers have their own base to-hit, but can still be affected by terrain and movement modifiers
        //thus, they don't qualify for special weapon handling
        if (BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName())) {
            toHit = new ToHitData(8, Messages.getString("WeaponAttackAction.MagMine"));
        }

        // TODO: mech making DFA could be higher if DFA target hex is higher
        // BMRr pg. 43, "attacking unit is considered to be in the air
        // above the hex, standing on an elevation 1 level higher than
        // the target hex or the elevation of the hex the attacker is
        // in, whichever is higher."
            // Ancient rules - have we implemented this per TW?

        // Store the thruBldg state, for later processing
        toHit.setThruBldg(los.getThruBldg());

        // Collect the modifiers for the environment
        toHit = compileEnvironmentalToHitMods(game, ae, target, wtype, atype, toHit, isArtilleryIndirect);

        // Collect the modifiers for the crew/pilot
        toHit = compileCrewToHitMods(game, ae, te, toHit, weapon);

        // Collect the modifiers for the attacker's condition/actions
        if (ae != null) {
            //Conventional fighter, Aerospace and fighter LAM attackers
            if (ae.isAero()) {
                toHit = compileAeroAttackerToHitMods(game, ae, target, ttype, toHit, aimingAt, aimingMode, eistatus,
                            wtype, weapon, atype, munition, isArtilleryIndirect, isFlakAttack, isNemesisConfused, isStrafing,
                            usesAmmo);
            //Everyone else
            } else {
                toHit = compileAttackerToHitMods(game, ae, target, los, toHit, toSubtract, aimingAt, aimingMode, wtype,
                        weapon, weaponId, atype, munition, isFlakAttack, isHaywireINarced, isNemesisConfused,
                        isWeaponFieldGuns, usesAmmo);
            }
        }

        // "hack" to cover the situation where the target is standing in a short
        // building which provides it partial cover. Unlike other partial cover situations,
        // this occurs regardless of other LOS consideration.
        if (WeaponAttackAction.targetInShortCoverBuilding(target)) {
            Building currentBuilding = game.getBoard().getBuildingAt(target.getPosition());

            LosEffects shortBuildingLos = new LosEffects();
            shortBuildingLos.setTargetCover(LosEffects.COVER_HORIZONTAL);
            shortBuildingLos.setDamagableCoverTypePrimary(LosEffects.DAMAGABLE_COVER_BUILDING);
            shortBuildingLos.setCoverBuildingPrimary(currentBuilding);
            shortBuildingLos.setCoverLocPrimary(target.getPosition());

            los.add(shortBuildingLos);
            toHit.append(shortBuildingLos.losModifiers(game));
        }

        // Collect the modifiers for the target's condition/actions
        toHit = compileTargetToHitMods(game, ae, target, ttype, los, toHit, toSubtract, aimingAt, aimingMode, distance,
                    wtype, weapon, atype, munition, isArtilleryDirect, isArtilleryIndirect, isAttackerInfantry,
                    exchangeSwarmTarget, isIndirect, isPointblankShot, usesAmmo);

        // Collect the modifiers for terrain and line-of-sight. This includes any related to-hit table changes
        toHit = compileTerrainAndLosToHitMods(game, ae, target, ttype, aElev, tElev, targEl, distance, los, toHit,
                    losMods, toSubtract, eistatus, wtype, weapon, weaponId, atype, ammo, munition, isAttackerInfantry,
                    inSameBuilding, isIndirect, isPointblankShot, underWater);

        // If this is a swarm LRM secondary attack, remove old target movement and terrain mods, then
        // add those for new target.
        if (exchangeSwarmTarget) {
            toHit = handleSwarmSecondaryAttacks(game, ae, target, swarmPrimaryTarget, swarmSecondaryTarget, toHit,
                    toSubtract, eistatus, aimingAt, aimingMode, weapon, atype, munition, isECMAffected,
                    inSameBuilding, underWater);
        }

        // Collect the modifiers specific to the weapon the attacker is using
        toHit = compileWeaponToHitMods(game, ae, spotter, target, ttype, toHit, wtype, weapon, atype, ammo, munition,
                    isFlakAttack, isIndirect, narcSpotter);

        // Collect the modifiers specific to the ammo the attacker is using
        toHit = compileAmmoToHitMods(game, ae, target, ttype, toHit, wtype, weapon, atype, munition, bApollo,
                    bArtemisV, bFTL, bHeatSeeking, isECMAffected, isINarcGuided);

        // okay!
        return toHit;
    }

    /**
     * To-hit number for attacker firing a generic weapon at the target. Does
     * not factor in any special weapon or ammo considerations, including range
     * modifiers. Also does not include gunnery skill.
     *
     * @param game The current {@link Game}
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);

        Entity te = null;
        int ttype = target.getTargetType();
        if (ttype == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }

        int aElev = ae.getElevation();
        int tElev = target.getElevation();
        int targEl;
        if (te == null) {
            targEl = game.getBoard().getHex(target.getPosition()).floor();
        } else {
            targEl = te.relHeight();
        }

        int toSubtract = 0;
        int distance = Compute.effectiveDistance(game, ae, target);

        // EI system
        // 0 if no EI (or switched off)
        // 1 if no intervening light woods
        // 2 if intervening light woods (because target in woods + intervening
        // woods is only +1 total)
        int eistatus = 0;

        // Bogus value, since this method doesn't account for weapons but some of its calls do
        int weaponId = WeaponType.WEAPON_NA;

        boolean isAttackerInfantry = ae instanceof Infantry;
        boolean inSameBuilding = Compute.isInSameBuilding(game, ae, te);

        // check LOS
        LosEffects los = LosEffects.calculateLOS(game, ae, target);

        if (ae.hasActiveEiCockpit()) {
            if (los.getLightWoods() > 0) {
                eistatus = 2;
            } else {
                eistatus = 1;
            }
        }

        ToHitData losMods = los.losModifiers(game, eistatus, ae.isUnderwater());
        ToHitData toHit = new ToHitData(0, Messages.getString("WeaponAttackAction.BaseToHit"));

        // Collect the modifiers for the environment
        toHit = compileEnvironmentalToHitMods(game, ae, target, null, null, toHit, false);

        // Collect the modifiers for the crew/pilot
        toHit = compileCrewToHitMods(game, ae, te, toHit, null);

        // Collect the modifiers for the attacker's condition/actions
        if (ae != null) {
            //Conventional fighter, Aerospace and fighter LAM attackers
            if (ae.isAero()) {
                toHit = compileAeroAttackerToHitMods(game, ae, target, ttype, toHit, Entity.LOC_NONE,
                        AimingMode.NONE, eistatus, null, null, null, EnumSet.of(AmmoType.Munitions.M_STANDARD),
                        false, false, false, false, false);
            //Everyone else
            } else {
                toHit = compileAttackerToHitMods(game, ae, target, los, toHit, toSubtract, Entity.LOC_NONE,
                        AimingMode.NONE, null, null, weaponId, null, EnumSet.of(AmmoType.Munitions.M_STANDARD),
                        false, false, false, false, false);
            }
        }

        // Collect the modifiers for the target's condition/actions
        toHit = compileTargetToHitMods(game, ae, target, ttype, los, toHit, toSubtract, Entity.LOC_NONE,
                AimingMode.NONE, distance, null, null, null, EnumSet.of(AmmoType.Munitions.M_STANDARD),
                false, false, isAttackerInfantry, false,
                false, false, false);

        // Collect the modifiers for terrain and line-of-sight. This includes any related to-hit table changes
        toHit = compileTerrainAndLosToHitMods(game, ae, target, ttype, aElev, tElev, targEl, distance, los, toHit,
                    losMods, toSubtract, eistatus, null, null, weaponId, null, null, EnumSet.of(AmmoType.Munitions.M_STANDARD), isAttackerInfantry,
                    inSameBuilding, false, false, false);

        // okay!
        return toHit;
    }


    /**
     * Method that tests each attack to see if it's impossible.
     * If so, a reason string will be returned. A null return means we can continue
     * processing the attack
     *
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param attackerId  The ID number of the attacking entity
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param los The calculated LOS between attacker and target
     * @param losMods ToHitData calculated from the spotter for indirect fire scenarios
     * @param toHit The running total ToHitData for this WeaponAttackAction
     *
     * @param distance  The distance in hexes from attacker to target
     * @param spotter  The spotting entity for indirect fire, if present
     *
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param atype The AmmoType being used for this attack
     * @param ammo The Mounted ammo being used
     * @param munition  Long indicating the munition type flag being used, if applicable
     *
     * @param isArtilleryDirect  flag that indicates whether this is a direct-fire artillery attack
     * @param isArtilleryFLAK  flag that indicates whether or not this is an artillery flak attack against an entity
     * @param isArtilleryIndirect  flag that indicates whether this is an indirect-fire artillery attack
     * @param isAttackerInfantry  flag that indicates whether the attacker is an infantry/BA unit
     * @param isBearingsOnlyMissile  flag that indicates whether this is a bearings-only capital missile attack
     * @param isCruiseMissile  flag that indicates whether this is a cruise missile artillery attack
     * @param exchangeSwarmTarget  flag that indicates whether this is the secondary target of Swarm LRMs
     * @param isHoming  flag that indicates whether this is a homing artillery attack
     * @param isIndirect  flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param isInferno  flag that indicates whether this is an inferno munition attack
     * @param isStrafing  flag that indicates whether this is an aero strafing attack
     * @param isTAG  flag that indicates whether this is a TAG attack
     * @param targetInBuilding  flag that indicates whether or not the target occupies a building hex
     * @param usesAmmo  flag that indicates whether or not the WeaponType being used is ammo-fed
     * @param underWater  flag that indicates whether or not the weapon being used is underwater
     */
    private static String toHitIsImpossible(Game game, Entity ae, int attackerId, Targetable target, int ttype,
            LosEffects los, ToHitData losMods, ToHitData toHit, int distance, Entity spotter,
            WeaponType wtype, Mounted weapon, int weaponId, AmmoType atype, Mounted ammo, EnumSet<AmmoType.Munitions> munition,
            boolean isArtilleryDirect, boolean isArtilleryFLAK, boolean isArtilleryIndirect, boolean isAttackerInfantry,
            boolean isBearingsOnlyMissile, boolean isCruiseMissile, boolean exchangeSwarmTarget, boolean isHoming,
            boolean isInferno, boolean isIndirect, boolean isStrafing, boolean isTAG, boolean targetInBuilding,
            boolean usesAmmo, boolean underWater, boolean evenIfAlreadyFired) {

        // Block the shot if the attacker is null
        if (ae == null) {
            return Messages.getString("WeaponAttackAction.NoAttacker");
        }
        // Or if the target is null
        if (target == null) {
            return Messages.getString("WeaponAttackAction.NoTarget");
        }
        // Without valid toHit data, the rest of this will fail
        if (toHit == null) {
            toHit = new ToHitData();
        }

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            //Some weapons only target valid entities
            te = (Entity) target;
        }

        // If the attacker and target are in the same building & hex, they can
        // always attack each other, TW pg 175.
        if ((los.getThruBldg() != null) && los.getTargetPosition().equals(ae.getPosition())) {
            return null;
        }

        // got ammo?
        if (usesAmmo && ((ammo == null) || (ammo.getUsableShotsLeft() == 0))) {
            return Messages.getString("WeaponAttackAction.OutOfAmmo");
        }

        // are we bracing a location that's not where the weapon is located?
        if (ae.isBracing() && (ae.braceLocation() != weapon.getLocation())) {
            return String.format(Messages.getString("WeaponAttackAction.BracingOtherLocation"),
                    ae.getLocationName(ae.braceLocation()), ae.getLocationName(weapon.getLocation()));
        }

        // Ammo-specific Reasons
        if (atype != null) {
            // Are we dumping that ammo?
            if (usesAmmo && ammo != null && ammo.isDumping()) {
                ae.loadWeaponWithSameAmmo(weapon);
                if ((ammo.getUsableShotsLeft() == 0) || ammo.isDumping()) {
                    return Messages.getString("WeaponAttackAction.DumpingAmmo");
                }
            }
            // make sure weapon can deliver flares
            if ((target.getTargetType() == Targetable.TYPE_FLARE_DELIVER) && !(usesAmmo
                    && ((atype.getAmmoType() == AmmoType.T_LRM)
                            || (atype.getAmmoType() == AmmoType.T_MML)
                            || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                            || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                    && (munition.contains(AmmoType.Munitions.M_FLARE)))) {
                return Messages.getString("WeaponAttackAction.NoFlares");
            }

            // These ammo types can only target hexes for flare delivery
            if (((atype.getAmmoType() == AmmoType.T_LRM)
                    || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_MML))
                    && (atype.getMunitionType().contains(AmmoType.Munitions.M_FLARE))
                    && (target.getTargetType() != Targetable.TYPE_FLARE_DELIVER)) {
                return Messages.getString("WeaponAttackAction.OnlyFlare");
            }

            // Aeros must have enough ammo for the maximum rate of fire because
            // they cannot lower it
            if (ae.isAero() && usesAmmo && ammo != null && weapon != null
                    && (ae.getTotalAmmoOfType(ammo.getType()) < weapon.getCurrentShots())) {
                return Messages.getString("WeaponAttackAction.InsufficientAmmo");
            }

            // Some Mek mortar ammo types can only be aimed at a hex
            if (wtype != null && wtype.hasFlag(WeaponType.F_MEK_MORTAR)
                    && ((atype.getMunitionType().contains(AmmoType.Munitions.M_AIRBURST)) || (atype.getMunitionType().contains(AmmoType.Munitions.M_FLARE))
                            || (atype.getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD)))) {
                if (!(target instanceof HexTarget)) {
                    return String.format(Messages.getString("WeaponAttackAction.AmmoAtHexOnly"), atype.getSubMunitionName());
                }
            }

            // make sure weapon can deliver minefield
            if ((target.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER)
                    && !AmmoType.canDeliverMinefield(atype)) {
                return Messages.getString("WeaponAttackAction.NoMinefields");
            }

            // These ammo types can only target hexes for minefield delivery
            if (((atype.getAmmoType() == AmmoType.T_LRM)
                    || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_MML)
                    || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                    && ((atype.getMunitionType().contains(AmmoType.Munitions.M_THUNDER))
                            || (atype.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_ACTIVE))
                            || (atype.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_INFERNO))
                            || (atype.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB))
                            || (atype.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_AUGMENTED)))
                    && (target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER)) {
                return Messages.getString("WeaponAttackAction.OnlyMinefields");
            }
        }

        // Attacker Action Reasons

        // If the attacker is actively using a shield, weapons in the same location are blocked
        if (weapon != null && ae.hasShield() && ae.hasActiveShield(weapon.getLocation(), weapon.isRearMounted())) {
            return Messages.getString("WeaponAttackAction.ActiveShieldBlocking");
        }

        //is the attacker even active?
        if (ae.isShutDown() || !ae.getCrew().isActive()) {
            return Messages.getString("WeaponAttackAction.AttackerNotReady");
        }

        // If the attacker is involved in a grapple
        if (ae.getGrappled() != Entity.NONE) {
            int grapple = ae.getGrappled();
            // It can only target the unit it is grappling with
            if (grapple != target.getId()) {
                return Messages.getString("WeaponAttackAction.MustTargetGrappled");
            }
            if (weapon != null) {
                int loc = weapon.getLocation();
                // Can't fire arm and leg-mounted weapons while grappling
                if (((ae instanceof Mech) && (ae.getGrappleSide() == Entity.GRAPPLE_BOTH)
                        && ((loc != Mech.LOC_CT) && (loc != Mech.LOC_LT) && (loc != Mech.LOC_RT) && (loc != Mech.LOC_HEAD)))
                        || weapon.isRearMounted()) {
                    return Messages.getString("WeaponAttackAction.CantFireWhileGrappled");
                }
                // If caught by a chain whip, can't use weapons in the affected arm
                if ((ae instanceof Mech) && (ae.getGrappleSide() == Entity.GRAPPLE_LEFT) && (loc == Mech.LOC_LARM)) {
                    return Messages.getString("WeaponAttackAction.CantShootWhileChained");
                }
                if ((ae instanceof Mech) && (ae.getGrappleSide() == Entity.GRAPPLE_RIGHT) && (loc == Mech.LOC_RARM)) {
                    return Messages.getString("WeaponAttackAction.CantShootWhileChained");
                }
            }
        }

        // Only large spacecraft can shoot while evading
        if (ae.isEvading() && !(ae instanceof Dropship) && !(ae instanceof Jumpship)) {
            return Messages.getString("WeaponAttackAction.AeEvading");
        }

        //If we're laying mines, we can't shoot.
        if (ae.isLayingMines()) {
            return Messages.getString("WeaponAttackAction.BusyLayingMines");
        }

        // Attacker prone and unable to fire?
        ToHitData ProneMods = Compute.getProneMods(game, ae, weaponId);
        if ((ProneMods != null) && ProneMods.getValue() == ToHitData.IMPOSSIBLE) {
            return ProneMods.getDesc();
        }

        // WiGE vehicles cannot fire at 0-range targets as they fly overhead
        if ((ae.getMovementMode() == EntityMovementMode.WIGE) && (ae.getPosition() == target.getPosition())) {
            return Messages.getString("WeaponAttackAction.ZeroRangeTarget");
        }

        // Crew Related Reasons

        // Stunned vehicle crews can't make attacks
        if (ae instanceof Tank && ((Tank) ae).getStunnedTurns() > 0) {
            return Messages.getString("WeaponAttackAction.CrewStunned");
        }
        // Vehicles with a single crewman can't shoot and unjam a RAC in the same turn (like mechs...)
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_TANK_CREWS)
                && (ae instanceof Tank) && ae.isUnjammingRAC()
                && (ae.getCrew().getSize() == 1)) {
            return Messages.getString("WeaponAttackAction.VeeSingleCrew");
        }

        // Critical Damage Reasons


        // Aerospace units can't fire if the FCS/CIC is destroyed
        if (ae instanceof Aero) {
            Aero aero = (Aero) ae;
            // FCS hits
            int fcs = aero.getFCSHits();
            if (fcs > 2) {
                return Messages.getString("WeaponAttackAction.FCSDestroyed");
            }
            // JS/WS/SS have CIC instead of FCS
            if (aero instanceof Jumpship) {
                Jumpship js = (Jumpship) aero;
                int cic = js.getCICHits();
                if (cic > 2) {
                    return Messages.getString("WeaponAttackAction.CICDestroyed");
                }
            }
        }
        // Are the sensors operational?
        // Battlemech sensors are destroyed after 2 hits, unless they have a torso-mounted cockpit
        int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
        if ((ae instanceof Mech) && (((Mech) ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) {
            sensorHits += ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if (sensorHits > 2) {
                return Messages.getString("WeaponAttackAction.SensorsDestroyed");
            }
        // Vehicles Sensor Hits
        } else if (ae instanceof Tank) {
            sensorHits = ((Tank) ae).getSensorHits();
            if (sensorHits >= Tank.CRIT_SENSOR) {
                return Messages.getString("WeaponAttackAction.SensorsDestroyed");
            }
        // Industrialmechs and other unit types have destroyed sensors with 2 or more hits
        } else if ((sensorHits > 1)
                || ((ae instanceof Mech) && (((Mech) ae).isIndustrial() && (sensorHits == 1)))) {
            return Messages.getString("WeaponAttackAction.SensorsDestroyed");
        }

        // Invalid Target Reasons

        // a friendly unit can never be the target of a direct attack.
        // but we do allow vehicle flamers to cool. Also swarm missile secondary targets and strafing are exempt.
        if (!game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE) && !isStrafing && !exchangeSwarmTarget) {
            if (te != null && !te.getOwner().isEnemyOf(ae.getOwner())) {
                if (!(usesAmmo && atype != null && (atype.getMunitionType().contains(AmmoType.Munitions.M_COOLANT)))) {
                    return Messages.getString("WeaponAttackAction.NoFriendlyTarget");
                }
            }
        }

        // Can't fire at hidden targets
        if ((target instanceof Entity) && ((Entity) target).isHidden()) {
            return Messages.getString("WeaponAttackAction.NoFireAtHidden");
        }

        // Infantry can't clear woods.
        if (isAttackerInfantry && (Targetable.TYPE_HEX_CLEAR == target.getTargetType())) {
            Hex hexTarget = game.getBoard().getHex(target.getPosition());
            if ((hexTarget != null) && hexTarget.containsTerrain(Terrains.WOODS)) {
                return Messages.getString("WeaponAttackAction.NoInfantryWoodsClearing");
            }
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

        // Only weapons allowed to clear minefields can target a hex for minefield clearance
        if ((target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR) &&
                ((atype == null) || !AmmoType.canClearMinefield(atype))) {
            return Messages.getString("WeaponAttackAction.CantClearMines");
        }

        // Mine Clearance munitions can only target hexes for minefield clearance
        if (!(target instanceof HexTarget) && (atype != null)
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_MINE_CLEARANCE))) {
            return Messages.getString("WeaponAttackAction.MineClearHexOnly");
        }

        // Only screen launchers may target a hex for screen launch
        if (Targetable.TYPE_HEX_SCREEN == target.getTargetType()) {
            if (wtype != null &&
                    (!((wtype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) || (wtype instanceof ScreenLauncherBayWeapon)))) {
                return Messages.getString("WeaponAttackAction.ScreenLauncherOnly");
            }
        }

        // Screen Launchers can only target hexes
        if ((Targetable.TYPE_HEX_SCREEN != target.getTargetType())
                && (wtype != null && ((wtype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)
                        || (wtype instanceof ScreenLauncherBayWeapon)))) {
            return Messages.getString("WeaponAttackAction.ScreenHexOnly");
        }

        // Can't target an entity conducting a swarm attack.
        if ((te != null) && (Entity.NONE != te.getSwarmTargetId())) {
            return Messages.getString("WeaponAttackAction.TargetSwarming");
        }

        //Tasers must target units and can't target flying units
        if (wtype != null && wtype.hasFlag(WeaponType.F_TASER)) {
            if (te != null) {
                if (te.isAirborne() || te.isAirborneVTOLorWIGE()) {
                    return Messages.getString("WeaponAttackAction.NoTaserAtAirborne");
                }
            } else {
                return Messages.getString("WeaponAttackAction.TaserOnlyAtUnit");
            }
        }

        // can't target yourself intentionally, but swarm missiles can come back to bite you
        if (!exchangeSwarmTarget && te != null && ae.equals(te)) {
            return Messages.getString("WeaponAttackAction.NoSelfTarget");
        }

        // Line of Sight and Range Reasons


        // attacker partial cover means no leg weapons
        if (los.isAttackerCover() && weapon != null && ae.locationIsLeg(weapon.getLocation()) && !underWater) {
            return Messages.getString("WeaponAttackAction.LegBlockedByTerrain");
        }

        // Must target infantry in buildings from the inside.
        if (targetInBuilding && (te instanceof Infantry)
                && (null == los.getThruBldg())) {
            return Messages.getString("WeaponAttackAction.CantShootThruBuilding");
        }

        //if LOS is blocked, block the shot except in the case of indirect artillery fire
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
                for (Entity en : game.getC3NetworkMembers(ae)) {
                    if (te != null && en.hasFiringSolutionFor(te.getId())) {
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
        // direct fire. Note that this is for ground combat with tacops sensors rules
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                && !ae.isSpaceborne()
                && !Compute.inVisualRange(game, ae, target)
                && !(Compute.inSensorRange(game, ae, target, null)
                        // Can shoot at something in sensor range if it has
                        // been spotted by another unit
                        && (te != null) && te.hasSeenEntity(ae.getOwner()))
                && !isArtilleryIndirect && !isIndirect && !isBearingsOnlyMissile) {
            boolean networkSee = false;
            if (ae.hasC3() || ae.hasC3i() || ae.hasActiveNovaCEWS()) {
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

        //Torpedos must remain in the water over their whole path to the target
        if ((atype != null)
                && ((atype.getAmmoType() == AmmoType.T_LRM_TORPEDO)
                        || (atype.getAmmoType() == AmmoType.T_SRM_TORPEDO)
                        || (((atype.getAmmoType() == AmmoType.T_SRM)
                                || (atype.getAmmoType() == AmmoType.T_SRM_IMP)
                                || (atype.getAmmoType() == AmmoType.T_MRM)
                                || (atype.getAmmoType() == AmmoType.T_LRM)
                                || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                                || (atype.getAmmoType() == AmmoType.T_MML)) && (atype.getMunitionType().contains(AmmoType.Munitions.M_TORPEDO))))
                && (los.getMinimumWaterDepth() < 1)) {
            return Messages.getString("WeaponAttackAction.TorpOutOfWater");
        }

        //Is the weapon blocked by a passenger?
        if (weapon != null && (ae.isWeaponBlockedAt(weapon.getLocation(), weapon.isRearMounted()))) {
            return Messages.getString("WeaponAttackAction.PassengerBlock");
        }

        //Is the weapon blocked by a tractor/trailer?
        if (weapon != null && (ae.getTowing() != Entity.NONE || ae.getTowedBy() != Entity.NONE)) {
            if (ae.isWeaponBlockedByTowing(weapon.getLocation(), ae.getSecondaryFacing(), weapon.isRearMounted())) {
                return Messages.getString("WeaponAttackAction.TrailerBlock");
            }
        }

        // Phase Reasons

        // Only bearings-only capital missiles and indirect fire artillery can be fired in the targeting phase
        if (game.getPhase().isTargeting() && (!(isArtilleryIndirect || isBearingsOnlyMissile))) {
            return Messages.getString("WeaponAttackAction.NotValidForTargPhase");
        }
        // Only TAG can be fired in the offboard phase
        if (game.getPhase().isOffboard() && !isTAG) {
            return Messages.getString("WeaponAttackAction.OnlyTagInOffboard");
        }
        // TAG can't be fired in any phase but offboard
        if (!game.getPhase().isOffboard() && isTAG) {
            return Messages.getString("WeaponAttackAction.TagOnlyInOffboard");
        }

        // Unit-specific Reasons

        // Airborne units cannot tag and attack
        // http://bg.battletech.com/forums/index.php?topic=17613.new;topicseen#new
        if (ae.isAirborne() && ae.usedTag()) {
            return Messages.getString("WeaponAttackAction.AeroCantTAGAndShoot");
        }

        // Hull Down

        // Hull down mechs cannot fire any leg weapons
        if (ae.isHullDown() && weapon != null) {
            if (((ae instanceof BipedMech)
                    && ((weapon.getLocation() == Mech.LOC_LLEG) || (weapon.getLocation() == Mech.LOC_RLEG)))
                    || ((ae instanceof QuadMech) && ((weapon.getLocation() == Mech.LOC_LLEG)
                            || (weapon.getLocation() == Mech.LOC_RLEG) || (weapon.getLocation() == Mech.LOC_LARM)
                            || (weapon.getLocation() == Mech.LOC_RARM)))) {
                return Messages.getString("WeaponAttackAction.NoLegHullDown");
            }
        }

        // hull down vees can't fire front weapons unless indirect
        if ((ae instanceof Tank) && ae.isHullDown() && (weapon != null) &&
                (weapon.getLocation() == Tank.LOC_FRONT) && !isIndirect) {
            return Messages.getString("WeaponAttackAction.FrontBlockedByTerrain");
        }

        // LAMs in fighter mode are restricted to only the ammo types that Aeros can use
        if ((ae instanceof LandAirMech) && (ae.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER)
                && usesAmmo && ammo != null
                && !((AmmoType) ammo.getType()).canAeroUse(game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS))) {
            return Messages.getString("WeaponAttackAction.InvalidAmmoForFighter");
        }

        // LAMs carrying certain types of bombs that require a weapon have attacks that cannot
        // be used in mech mode.
        if ((ae instanceof LandAirMech)
                && wtype != null
                && (ae.getConversionMode() == LandAirMech.CONV_MODE_MECH)
                && wtype.hasFlag(WeaponType.F_BOMB_WEAPON)
                && wtype.getAmmoType() != AmmoType.T_RL_BOMB
                && !wtype.hasFlag(WeaponType.F_TAG)) {
            return Messages.getString("WeaponAttackAction.NoBombInMechMode");
        }

        // limit large craft to zero net heat and to heat by arc
        final int heatCapacity = ae.getHeatCapacity();
        if (ae.usesWeaponBays() && (weapon != null) && !weapon.getBayWeapons().isEmpty()) {
            int totalHeat = 0;

            // first check to see if there are any usable weapons
            boolean usable = false;
            for (int wId : weapon.getBayWeapons()) {
                Mounted m = ae.getEquipment(wId);
                WeaponType bayWType = ((WeaponType) m.getType());
                boolean bayWUsesAmmo = (bayWType.getAmmoType() != AmmoType.T_NA);
                if (m.canFire()) {
                    if (bayWUsesAmmo) {
                        if ((m.getLinked() != null) && (m.getLinked().getUsableShotsLeft() > 0)) {
                            usable = true;
                            break;
                        }
                    } else {
                        usable = true;
                        break;
                    }
                }
            }
            if (!usable) {
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
                    if (prevWeapon != null) {
                        int loc = prevWeapon.getLocation();
                        boolean rearMount = prevWeapon.isRearMounted();
                        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_HEAT_BY_BAY)) {
                            for (int bwId : prevWeapon.getBayWeapons()) {
                                totalHeat += ae.getEquipment(bwId).getCurrentHeat();
                            }
                        } else {
                            if (!rearMount) {
                                if (!usedFrontArc[loc]) {
                                    totalHeat += ae.getHeatInArc(loc, rearMount);
                                    usedFrontArc[loc] = true;
                                }
                            } else {
                                if (!usedRearArc[loc]) {
                                    totalHeat += ae.getHeatInArc(loc, rearMount);
                                    usedRearArc[loc] = true;
                                }
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
                if ((totalHeat + currentHeat) > heatCapacity) {
                    // FIXME: This is causing weird problems (try firing all the
                    // Suffen's nose weapons)
                    return Messages.getString("WeaponAttackAction.HeatOverCap");
                }
            } else {
                if (!rearMount) {
                    if (!usedFrontArc[loc] && ((totalHeat + currentHeat) > heatCapacity) && !onlyArc) {
                        return Messages.getString("WeaponAttackAction.HeatOverCap");
                    }
                } else {
                    if (!usedRearArc[loc] && ((totalHeat + currentHeat) > heatCapacity) && !onlyArc) {
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

            if (weapon != null && ((totalheat + weapon.getCurrentHeat()) > heatCapacity)) {
                return Messages.getString("WeaponAttackAction.HeatOverCap");
            }
        }

        // Protomechs can't fire energy weapons while charging EDP armor
        if ((ae instanceof Protomech) && ((Protomech) ae).isEDPCharging()
                && wtype != null && wtype.hasFlag(WeaponType.F_ENERGY)) {
            return Messages.getString("WeaponAttackAction.ChargingEDP");
        }

        // for spheroid dropships in atmosphere (and on ground), the rules about
        // firing arcs are more complicated
        // TW errata 2.1

        if ((Compute.useSpheroidAtmosphere(game, ae) ||
                (ae.isAero() && ((IAero) ae).isSpheroid() && (ae.getAltitude() == 0) && game.getBoard().onGround()))
                && (weapon != null)) {
            int range = Compute.effectiveDistance(game, ae, target, false);
            // Only aft-mounted weapons can be fired at range 0 (targets directly underneath)
            if (!ae.isAirborne() && (range == 0) && (weapon.getLocation() != Aero.LOC_AFT)) {
                return Messages.getString("WeaponAttackAction.OnlyAftAtZero");
            }

            int altDif = target.getAltitude() - ae.getAltitude();

            // Nose-mounted weapons can only be fired at targets at least 1 altitude higher
            if ((weapon.getLocation() == Aero.LOC_NOSE) && (altDif < 1)
                    && wtype != null
                    // Unless the weapon is used as artillery
                    && (!(wtype instanceof ArtilleryWeapon || wtype.hasFlag(WeaponType.F_ARTILLERY)
                            || (ae.getAltitude() == 0 && wtype instanceof CapitalMissileWeapon)
                            || isIndirect))) {
                return Messages.getString("WeaponAttackAction.TooLowForNose");
            }
            // Front-side-mounted weapons can only be fired at targets at the same altitude or higher
            if ((!weapon.isRearMounted() && (weapon.getLocation() != Aero.LOC_AFT)) && (altDif < 0)
                    && wtype != null
                    && !((wtype instanceof ArtilleryWeapon) || wtype.hasFlag(WeaponType.F_ARTILLERY))) {
                return Messages.getString("WeaponAttackAction.TooLowForFrontSide");
            }
            // Aft-mounted weapons can only be fired at targets at least 1 altitude lower
            // For grounded spheroids, weapons can only be fired at targets in occupied hexes,
            // but it's not actually possible for a unit to occupy the same hex as a grounded spheroid so
            // we simplify the calculation a bit
            if (weapon.getLocation() == Aero.LOC_AFT) {
                if (altDif > -1) {
                    return Messages.getString("WeaponAttackAction.TooHighForAft");
                }

                // if both targets are on the ground
                // and the target is below the attacker
                // and the attacker is in one of the target's occupied hexes
                // then we can shoot aft weapons at it
                // note that this cannot actually happen in MegaMek currently but is left here for the possible eventuality
                // that overhanging dropships are implemented
                if (!ae.isAirborne() && !target.isAirborne()) {
                    boolean targetInAttackerHex = ae.getOccupiedCoords().contains(target.getPosition()) ||
                            ae.getPosition().equals(target.getPosition());
                    boolean targetBelowAttacker = game.getBoard().getHex(ae.getPosition()).getLevel() >
                            game.getBoard().getHex(target.getPosition()).getLevel() + target.getElevation();

                    if (!targetInAttackerHex || !targetBelowAttacker) {
                        return Messages.getString("WeaponAttackAction.GroundedSpheroidDropshipAftWeaponRestriction");
                    }
                }
            }

            // and aft-side-mounted weapons can only be fired at targets at the same or lower altitude
            if ((weapon.isRearMounted()) && (altDif > 0)) {
                return Messages.getString("WeaponAttackAction.TooHighForAftSide");
            }

            if (Compute.inDeadZone(game, ae, target)) {
                // Only nose weapons can fire at targets in the dead zone at higher altitude
                if ((altDif > 0) && (weapon.getLocation() != Aero.LOC_NOSE)) {
                    return Messages.getString("WeaponAttackAction.OnlyNoseInDeadZone");
                }
                // and only aft weapons can fire at targets in the dead zone at lower altitude
                if ((altDif < 0) && (weapon.getLocation() != Aero.LOC_AFT)) {
                    return Messages.getString("WeaponAttackAction.OnlyAftInDeadZone");
                }
            }

        }

        // Weapon-specific Reasons

        if (weapon != null && wtype != null) {
            // Variable setup

            // "Cool" mode for vehicle flamer requires coolant ammo
            boolean vf_cool = false;
            if (atype != null && ammo != null && (((AmmoType) ammo.getType()).getMunitionType().contains(AmmoType.Munitions.M_COOLANT))) {
                vf_cool = true;
            }

            // Anti-Infantry weapons can only target infantry
            if (wtype.hasFlag(WeaponType.F_INFANTRY_ONLY)) {
                if ((te != null) && !(te instanceof Infantry)) {
                    return Messages.getString("WeaponAttackAction.TargetOnlyInf");
                }
            }

            // Air-to-ground attacks
            if (Compute.isAirToGround(ae, target) && !isArtilleryIndirect && !ae.isDropping()) {
                if (ae.isBomber() && weapon.isInternalBomb() && ((IBomber)ae).getUsedInternalBombs() >= 6) {
                    return Messages.getString("WeaponAttackAction.AlreadyUsedMaxInternalBombs");
                }
                // Can't strike from above altitude 5. Dive bombing uses a different test below
                if ((ae.getAltitude() > 5)
                        && !wtype.hasFlag(WeaponType.F_DIVE_BOMB) && !wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                    return Messages.getString("WeaponAttackAction.AttackerTooHigh");
                }
                // Can't strafe from above altitude 3
                if ((ae.getAltitude() > 3) && isStrafing) {
                    return Messages.getString("WeaponAttackAction.AttackerTooHigh");
                }
                // Additional Nap-of-Earth restrictions for strafing
                if ((ae.getAltitude() == 1) && isStrafing) {
                    Vector<Coords> passedThrough = ae.getPassedThrough();
                    if (passedThrough.isEmpty() || passedThrough.get(0).equals(target.getPosition())) {
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
                    Hex prevHex = game.getBoard().getHex(prevCoords);
                    Hex currHex = game.getBoard().getHex(target.getPosition());
                    int prevElev = prevHex.getLevel();
                    int currElev = currHex.getLevel();
                    if ((prevElev - currElev - target.relHeight()) > 2) {
                        return Messages.getString("WeaponAttackAction.DeadZone");
                    }
                }

                // Only direct-fire energy weapons can strafe
                boolean isDirectFireEnergy = (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)
                        && (wtype.hasFlag(WeaponType.F_LASER) || wtype.hasFlag(WeaponType.F_PPC)
                                || wtype.hasFlag(WeaponType.F_PLASMA) || wtype.hasFlag(WeaponType.F_PLASMA_MFUK)))
                        || wtype.hasFlag(WeaponType.F_FLAMER);
                // Note: flamers are direct fire energy, but don't have the flag,
                // so they won't work with targeting computers
                boolean isEnergyBay = (wtype instanceof LaserBayWeapon) || (wtype instanceof PPCBayWeapon)
                        || (wtype instanceof PulseLaserBayWeapon);
                if (isStrafing && !isDirectFireEnergy && !isEnergyBay) {
                    return Messages.getString("WeaponAttackAction.StrafeDirectEnergyOnly");
                }

                // only certain weapons can be used for air to ground attacks
                if (ae.isAero()) {
                    // Spheroids can't strafe
                    if (isStrafing && ((IAero) ae).isSpheroid()) {
                        return Messages.getString("WeaponAttackAction.NoSpheroidStrafing");
                    }
                    // Spheroid craft can only use aft or aft-side mounted weapons for strike attacks
                    if (((IAero) ae).isSpheroid()) {
                        if ((weapon.getLocation() != Aero.LOC_AFT) && !weapon.isRearMounted()) {
                            return Messages.getString("WeaponAttackAction.InvalidDSAtgArc");
                        }
                    // LAMs can't use leg or rear-mounted weapons
                    } else if (ae instanceof LandAirMech) {
                        if ((weapon.getLocation() == Mech.LOC_LLEG)
                                || (weapon.getLocation() == Mech.LOC_RLEG)
                                || weapon.isRearMounted()) {
                            return Messages.getString("WeaponAttackAction.InvalidAeroDSAtgArc");
                        }
                    } else {
                        // and other types of aero can't use aft or rear-mounted weapons
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

                // Strike attacks cost the attacker 1 altitude
                int altitudeLoss = 1;
                // Dive bombing costs 2 altitude
                if (wtype.hasFlag(WeaponType.F_DIVE_BOMB)) {
                    altitudeLoss = 2;
                }
                // Altitude bombing and strafing cost nothing
                if (wtype.hasFlag(WeaponType.F_ALT_BOMB) || isStrafing) {
                    altitudeLoss = 0;
                }
                int altLossThisRound = 0;
                if (ae.isAero()) {
                    altLossThisRound = ((IAero) ae).getAltLossThisRound();
                }
                // You can't make attacks that would lower you to zero altitude
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
                        if ((prevAttk.getEntityId() == ae.getId()) && (prevAttk.getTargetId() != target.getId())
                                && !wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                            return Messages.getString("WeaponAttackAction.CantSplitFire");
                        }
                    }
                }
            // VTOL Strafing
            } else if ((ae instanceof VTOL) && isStrafing) {
                if (!(wtype.hasFlag(WeaponType.F_DIRECT_FIRE)
                        && (wtype.hasFlag(WeaponType.F_LASER) || wtype.hasFlag(WeaponType.F_PPC)
                                || wtype.hasFlag(WeaponType.F_PLASMA) || wtype.hasFlag(WeaponType.F_PLASMA_MFUK)))
                        || wtype.hasFlag(WeaponType.F_FLAMER)) {
                    return Messages.getString("WeaponAttackAction.StrafeDirectEnergyOnly");
                }
                if (weapon.getLocation() != VTOL.LOC_FRONT
                        && weapon.getLocation() != VTOL.LOC_TURRET
                        && weapon.getLocation() != VTOL.LOC_TURRET_2) {
                    return Messages.getString("WeaponAttackAction.InvalidStrafingArc");
                }
            }

            // Artillery

            // Arty shots have to be with arty, non arty shots with non arty.
            if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {

                // Don't allow Artillery Flak attacks by off-board artillery.
                if (te != null && te.isAirborne() && ae.isOffBoard()) {
                    return Messages.getString("WeaponAttackAction.ArtyAttacksOnly");
                }

                // check artillery is targeted appropriately for its ammo
                // Artillery only targets hexes unless making a direct fire flak shot or using
                // homing ammo.
                if ((ttype != Targetable.TYPE_HEX_ARTILLERY) && (ttype != Targetable.TYPE_MINEFIELD_CLEAR)
                        && !(isArtilleryFLAK || (atype != null && atype.countsAsFlak())) && !isHoming && !target.isOffBoard()) {
                    return Messages.getString("WeaponAttackAction.ArtyAttacksOnly");
                }
                // Airborne units can't make direct-fire artillery attacks
                if (ae.isAirborne()) {
                    if (isArtilleryDirect) {
                        return Messages.getString("WeaponAttackAction.NoAeroDirectArty");
                    } else if (isArtilleryIndirect) {
                        // and can only make indirect artillery attacks at altitude 9 or below
                        if (ae.getAltitude() > 9) {
                            return Messages.getString("WeaponAttackAction.TooHighForArty");
                        }
                        // and finally, can only use Arrow IV artillery
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
                        } else if ((wtype.getAmmoType() != AmmoType.T_ARROW_IV) &&
                                (wtype.getAmmoType() != AmmoType.T_ARROW_IV_BOMB)) {
                            //For Fighters, LAMs, Small Craft and VTOLs
                            return Messages.getString("WeaponAttackAction.OnlyArrowArty");
                        }
                    }
                } else if ((wtype.getAmmoType() == AmmoType.T_ARROW_IV)
                        && atype != null && atype.getMunitionType().contains(AmmoType.Munitions.M_ADA) ) {
                    // Air-Defense Arrow IV can only target airborne enemy units between 1 and 51 hexes away
                    // (same ground map/Low Altitude hex, 1 LAH, or 2 Low Altitude hexes away) and below
                    // altitude 8.
                    if(!(target.isAirborne() || target.isAirborneVTOLorWIGE())){
                        return Messages.getString("WeaponAttackAction.AaaGroundAttack");
                    }
                    if (target.getAltitude() > 8){
                        return Messages.getString("WeaponAttackAction.OutOfRange");
                    }
                    if (distance > Board.DEFAULT_BOARD_HEIGHT * 3){
                        return Messages.getString("WeaponAttackAction.OutOfRange");
                    }
                }
            } else if (weapon.isInBearingsOnlyMode()) {
                // We don't really need to do anything here. This just prevents these weapons
                // from passing the next test erroneously.
            } else if (wtype instanceof CapitalMissileWeapon
                        && Compute.isGroundToGround(ae, target)) {
                // Grounded units firing capital missiles at ground targets must do so as artillery
                if (ttype != Targetable.TYPE_HEX_ARTILLERY) {
                    return Messages.getString("WeaponAttackAction.ArtyAttacksOnly");
                }
            } else {
                // weapon is not artillery
                if (ttype == Targetable.TYPE_HEX_ARTILLERY) {
                    return Messages.getString("WeaponAttackAction.NoArtyAttacks");
                }
            }

            // Direct-fire artillery attacks.
            if (isArtilleryDirect) {
                if (ae.isOffBoard()) {
                    return Messages.getString("WeaponAttackAction.ArtyAttacksOnly");
                }
                // Cruise missiles cannot make direct-fire attacks
                if (isCruiseMissile) {
                    return Messages.getString("WeaponAttackAction.NoDirectCruiseMissile");
                }
                // ADA is _fired_ by artillery but is just a Flak attack, and so bypasses these restrictions
                if (null != atype && !atype.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
                    // Direct fire artillery cannot be fired at less than 6 hexes,
                    // except at ASFs in the air (TO:AR 6th print, p153.)
                    if (!(target.isAirborne()) && (Compute.effectiveDistance(game, ae, target) <= 6)) {
                        return Messages.getString("WeaponAttackAction.TooShortForDirectArty");
                    }
                    // ...or more than 17 hexes
                    if (distance > Board.DEFAULT_BOARD_HEIGHT) {
                        return Messages.getString("WeaponAttackAction.TooLongForDirectArty");
                    }
                }
                if (isHoming) {
                    if ((te == null) || (te.getTaggedBy() == -1)) {
                        // Homing missiles must target a tagged entity
                        return Messages.getString("WeaponAttackAction.MustTargetTagged");
                    }
                }
            }

            // Indirect artillery attacks
            if (isArtilleryIndirect) {
                int boardRange = (int) Math.ceil(distance / 17f);
                int maxRange = wtype.getLongRange();
                // Capital/subcapital missiles have a board range equal to their max space hex range
                if (wtype instanceof CapitalMissileWeapon) {
                    if (wtype.getMaxRange(weapon) == WeaponType.RANGE_EXT) {
                        maxRange = 50;
                    }
                    if (wtype.getMaxRange(weapon) == WeaponType.RANGE_LONG) {
                        maxRange = 40;
                    }
                    if (wtype.getMaxRange(weapon) == WeaponType.RANGE_MED) {
                        maxRange = 24;
                    }
                    if (wtype.getMaxRange(weapon) == WeaponType.RANGE_SHORT) {
                        maxRange = 12;
                    }
                }
                // Maximum range is measured in mapsheets
                if (boardRange > maxRange) {
                    return Messages.getString("WeaponAttackAction.OutOfRange");
                }
                // Indirect shots cannot be made at less than 17 hexes range unless
                // the attacker is airborne or has no line-of-sight
                if (((distance <= Board.DEFAULT_BOARD_HEIGHT) && !ae.isAirborne())
                        && !(losMods.getValue() == TargetRoll.IMPOSSIBLE)) {
                    return Messages.getString("WeaponAttackAction.TooShortForIndirectArty");
                }
                if (isHoming) {
                    // Homing missiles must target a hex (mapsheet)
                    if (ttype != Targetable.TYPE_HEX_ARTILLERY) {
                        return Messages.getString("WeaponAttackAction.HomingMapsheetOnly");
                    }
                }
            }


            // Ballistic and Missile weapons are subject to wind conditions
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            if (conditions.getWind().isTornadoF1ToF3() && wtype.hasFlag(WeaponType.F_MISSILE)
                    && !game.getBoard().inSpace()) {
                return Messages.getString("WeaponAttackAction.NoMissileTornado");
            }
            boolean missleOrBallistic = wtype.hasFlag(WeaponType.F_MISSILE)
                    || wtype.hasFlag(WeaponType.F_BALLISTIC);
            if (conditions.getWind().isTornadoF4()
                    && !game.getBoard().inSpace()
                    && missleOrBallistic) {
                return Messages.getString("WeaponAttackAction.F4Tornado");
            }

            // Battle Armor

            // BA can only make one AP attack
            if ((ae instanceof BattleArmor) && wtype.hasFlag(WeaponType.F_INFANTRY)) {
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
                            if (prevAttack.getTargetId() != target.getId()) {
                                return Messages.getString("WeaponAttackAction.OneTargetForCNarc");
                            }
                        }
                    }
                }
            }

            // BA Mine launchers can not hit infantry
            if (BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName())) {
                if (te instanceof Infantry) {
                    return Messages.getString("WeaponAttackAction.CantShootInfantry");
                }
            }

            // BA NARCs and Tasers can only fire at one target in a round
            if ((ae instanceof BattleArmor)
                    && (wtype.hasFlag(WeaponType.F_TASER) || wtype.getAmmoType() == AmmoType.T_NARC)) {
                // Go through all of the current actions to see if a NARC or Taser
                // has been fired
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                    Object o = i.nextElement();
                    if (!(o instanceof WeaponAttackAction)) {
                        continue;
                    }
                    WeaponAttackAction prevAttack = (WeaponAttackAction) o;
                    // Is this an attack from this entity to a different target?
                    if (prevAttack.getEntityId() == ae.getId() && prevAttack.getTargetId() != target.getId()) {
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

            // BA squad support weapons require that Trooper 1 be alive to use
            if (weapon.isSquadSupportWeapon() && (ae instanceof BattleArmor)) {
                if (!((BattleArmor) ae).isTrooperActive(BattleArmor.LOC_TROOPER_1)) {
                    return Messages.getString("WeaponAttackAction.NoSquadSupport");
                }
            }

            // Bombs and such

            // Anti ship missiles can't be launched from altitude 3 or lower
            if (wtype.hasFlag(WeaponType.F_ANTI_SHIP) && !game.getBoard().inSpace() && (ae.getAltitude() < 4)) {
                return Messages.getString("WeaponAttackAction.TooLowForASM");
            }

            // ASEW Missiles cannot be launched in an atmosphere
            if ((wtype.getAmmoType() == AmmoType.T_ASEW_MISSILE)
                    && !ae.isSpaceborne()) {
                return Messages.getString("WeaponAttackAction.ASEWAtmo");
            }

            if (ae.isAero()) {
                // Can't mix bombing with other attack types
                // also for altitude bombing, the target hex must either be the first in a line
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
                        // You also can't mix and match the 3 different types of bombing:  Space, Dive and Altitude
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
                            if (!wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                                return Messages.getString("WeaponAttackAction.BusyAltBombing");
                            }
                            firstAltBomb = false;
                            int bombDistance = prevAttack.getTarget(game).getPosition().distance(target.getPosition());
                            if (bombDistance == 1) {
                                adjacentAltBomb = true;
                            }
                            // For altitude bombing, prevent targeting the same hex twice
                            if (bombDistance == 0) {
                                return Messages.getString("WeaponAttackAction.AlreadyBombingHex");
                            }

                        }
                    }
                }
                if (wtype.hasFlag(WeaponType.F_ALT_BOMB) && !firstAltBomb && !adjacentAltBomb) {
                    return Messages.getString("WeaponAttackAction.BombNotInLine");
                }
            }

            // Altitude and dive bombing attacks...
            if (wtype.hasFlag(WeaponType.F_DIVE_BOMB) || wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                // Can't fire if the unit is out of bombs
                if (ae.getBombs(AmmoType.F_GROUND_BOMB).isEmpty()) {
                    return Messages.getString("WeaponAttackAction.OutOfBombs");
                }
                // Spheroid Aeros can't bomb
                if (ae.isAero() && ((IAero) ae).isSpheroid()) {
                    return Messages.getString("WeaponAttackAction.NoSpheroidBombing");
                }
                // Grounded Aeros can't bomb
                if (!ae.isAirborne() && !ae.isAirborneVTOLorWIGE()) {
                    return Messages.getString("WeaponAttackAction.GroundedAeroCantBomb");
                }
                // Bomb attacks can only target hexes
                if (target.getTargetType() != Targetable.TYPE_HEX_AERO_BOMB) {
                    return Messages.getString("WeaponAttackAction.BombTargetHexOnly");
                }
                // Can't target a hex that isn't on the flight path
                if (!ae.passedOver(target)) {
                    return Messages.getString("WeaponAttackAction.CantBombOffFlightPath");
                }
                // Dive Bombing can only be conducted if starting between altitude 5 and altitude 3
                if (wtype.hasFlag(WeaponType.F_DIVE_BOMB)) {
                    if (ae.getAltitude() > MMConstants.DIVE_BOMB_MAX_ALTITUDE) {
                        return Messages.getString("WeaponAttackAction.TooHighForDiveBomb");
                    }
                    if (ae.isAero()) {
                        int altLoss = ((IAero) ae).getAltLossThisRound();
                        if ((ae.getAltitude() + altLoss) < MMConstants.DIVE_BOMB_MIN_ALTITUDE) {
                            return Messages.getString("WeaponAttackAction.TooLowForDiveBomb");
                        }
                    }
                }
            }

            // Can't attack bomb hex targets with weapons other than alt/dive bombs
            if ((target.getTargetType() == Targetable.TYPE_HEX_AERO_BOMB) && !wtype.hasFlag(WeaponType.F_DIVE_BOMB)
                    && !wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                return Messages.getString("WeaponAttackAction.InvalidForBombing");
            }

            // BA Micro bombs only when flying
            if ((atype != null) && (atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB)) {
                if (!ae.isAirborneVTOLorWIGE()) {
                    return Messages.getString("WeaponAttackAction.MinimumAlt1");
                // and can only target hexes
                } else if (target.getTargetType() != Targetable.TYPE_HEX_BOMB) {
                    return Messages.getString("WeaponAttackAction.BombTargetHexOnly");
                // and can only be dropped at exactly altitude 1
                } else if (ae.getElevation() != 1) {
                    return Messages.getString("WeaponAttackAction.ExactlyAlt1");
                }
            }

            // Can't attack a Micro Bomb hex target with other weapons
            if ((target.getTargetType() == Targetable.TYPE_HEX_BOMB)
                    && !(usesAmmo && atype != null && (atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB))) {
                return Messages.getString("WeaponAttackAction.InvalidForBombing");
            }

            // Space bombing attacks
            if (wtype.hasFlag(WeaponType.F_SPACE_BOMB) && te != null) {
                toHit = Compute.getSpaceBombBaseToHit(ae, te, game);
                // Return if the attack is impossible.
                if (TargetRoll.IMPOSSIBLE == toHit.getValue()) {
                    return toHit.getDesc();
                }
            }

            // B-Pods

            if (wtype.hasFlag(WeaponType.F_B_POD)) {
                // B-Pods are only effective against infantry
                if (!(target instanceof Infantry)) {
                    return Messages.getString("WeaponAttackAction.BPodOnlyAtInf");
                }
                // Leg-mounted B-Pods can be fired at infantry in the attacker's hex, other locations
                // can only be fired in response to leg/swarm attacks
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

            // Called shots
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS)) {
                String reason = weapon.getCalledShot().isValid(target);
                if (reason != null) {
                    return reason;
                }
            }

            // Capital Mass Drivers can only fire at targets directly in front of the attacker
            if ((target.getTargetType() == Targetable.TYPE_ENTITY) && wtype.hasFlag(WeaponType.F_MASS_DRIVER)
                    && (ae instanceof SpaceStation)) {
                if (!ae.getPosition().translated(ae.getFacing(), Compute.effectiveDistance(game, ae, target)).equals(target.getPosition())) {
                    return Messages.getString("WeaponAttackAction.MassDriverFrontOnly");
                }
            }

            // Capital missiles in bearings-only mode
            if (isBearingsOnlyMissile) {
                //Can't target anything beyond max range of 5,000 hexes
                //This is an arbitrary number. If your map size is really this large, you'll probably crash the game
                if (distance > RangeType.RANGE_BEARINGS_ONLY_OUT) {
                    return Messages.getString("WeaponAttackAction.OutOfRange");
                }
                // Can't fire in bearings-only mode within direct-fire range (50 hexes)
                if (game.getPhase().isTargeting() && distance < RangeType.RANGE_BEARINGS_ONLY_MINIMUM) {
                    return Messages.getString("WeaponAttackAction.BoMissileMinRange");
                }
                // Can't target anything but hexes
                if (ttype != Targetable.TYPE_HEX_ARTILLERY) {
                    return Messages.getString("WeaponAttackAction.BOHexOnly");
                }
            }

            // Capital weapons fire by grounded units
            if (wtype.isSubCapital() || wtype.isCapital()) {
                // Can't fire any but capital/subcapital missiles surface to surface
                // (but VTOL dive bombing is allowed)
                if (Compute.isGroundToGround(ae, target)
                        && !((ae.getMovementMode() == EntityMovementMode.VTOL) && (wtype instanceof DiveBombAttack))
                        && !(wtype instanceof CapitalMissileWeapon)) {
                    return Messages.getString("WeaponAttackAction.NoS2SCapWeapons");
                }
            }

            // Causing Fires

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

            // Conventional Infantry Attacks

            if (isAttackerInfantry && !(ae instanceof BattleArmor)) {
                // 0 MP infantry units: move or shoot, except for anti-mech attacks,
                // those are handled above
                if ((ae.getMovementMode() == EntityMovementMode.INF_LEG) && (ae.getWalkMP() == 0)
                        && (ae.moved != EntityMovementType.MOVE_NONE)) {
                    return Messages.getString("WeaponAttackAction.0MPInf");
                }
                // Can't shoot if platoon used fast movement
                if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_FAST_INFANTRY_MOVE)
                        && (ae.moved == EntityMovementType.MOVE_RUN)) {
                    return Messages.getString("WeaponAttackAction.CantShootAndFastMove");
                }
                // check for trying to fire field gun after moving
                if ((weapon.getLocation() == Infantry.LOC_FIELD_GUNS) && (ae.moved != EntityMovementType.MOVE_NONE)) {
                    return Messages.getString("WeaponAttackAction.CantMoveAndFieldGun");
                }
                // check for mixing infantry and field gun attacks
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
                    }
                }
            }

            // Extinguishing Fires

            // You can use certain types of flamer/sprayer ammo or infantry firefighting engineers
            // to extinguish burning hexes (and units).
            // TODO: This functionality does not appear to be implemented
            if (Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType()) {
                if (!wtype.hasFlag(WeaponType.F_EXTINGUISHER) && !vf_cool) {
                    return Messages.getString("WeaponAttackAction.InvalidForFirefighting");
                }
                Hex hexTarget = game.getBoard().getHex(target.getPosition());
                if ((hexTarget != null) && !hexTarget.containsTerrain(Terrains.FIRE)) {
                    return Messages.getString("WeaponAttackAction.TargetNotBurning");
                }
            } else if (wtype.hasFlag(WeaponType.F_EXTINGUISHER)) {
                if (!(((target instanceof Tank) && ((Tank) target).isOnFire())
                        || ((target instanceof Entity) && (((Entity) target).infernos.getTurnsLeftToBurn() > 0)))) {
                    return Messages.getString("WeaponAttackAction.TargetNotBurning");
                }
            }

            // Gauss weapons using the TacOps powered down rule can't fire
            if ((wtype instanceof GaussWeapon)
                    && weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_GAUSS_POWERED_DOWN)) {
                return Messages.getString("WeaponAttackAction.WeaponNotReady");
            }

            // Ground-to-air attacks

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

            // Can't make ground-to-air attacks against a target above altitude 8
            if ((target.getAltitude() > 8) && Compute.isGroundToAir(ae, target)) {
                return Messages.getString("WeaponAttackAction.AeroTooHighForGta");
            }

            // Infantry can't make ground-to-air attacks, unless using field guns, specialized AA infantry weapons,
            // or direct-fire artillery flak attacks
            boolean isWeaponFieldGuns = isAttackerInfantry && (weapon.getLocation() == Infantry.LOC_FIELD_GUNS);
            if ((ae instanceof Infantry) && Compute.isGroundToAir(ae, target) && !wtype.hasFlag(WeaponType.F_INF_AA)
                    && !isArtilleryFLAK
                    && !isWeaponFieldGuns) {
                return Messages.getString("WeaponAttackAction.NoInfantryGta");
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
                        // Can't mix ground-to-air and ground-to-ground attacks either
                        if (!prevAttack.isGroundToAir(game) && Compute.isGroundToAir(ae, target)) {
                            return Messages.getString("WeaponAttackAction.AlreadyGtgAttack");
                        }
                        // Or split ground-to-air fire across multiple targets
                        if (prevAttack.isGroundToAir(game) && Compute.isGroundToAir(ae, target) && (null != te)
                                && (prevAttack.getTargetId() != te.getId())) {
                            return Messages.getString("WeaponAttackAction.OneTargetForGta");
                        }
                    }
                }
            }

            // Indirect Fire (LRMs)

            // Can't fire Indirect LRM with direct LOS
            if (isIndirect && Compute.indirectAttackImpossible(game, ae, target, wtype, weapon)) {
                return Messages.getString("WeaponAttackAction.NoIndirectWithLOS");
            }

            // Can't fire Indirect LRMs if the option is turned off
            if (isIndirect && !game.getOptions().booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
                return Messages.getString("WeaponAttackAction.IndirectFireOff");
            }

            // Can't fire an MML indirectly when loaded with SRM munitions
            if (isIndirect && usesAmmo
                    && atype != null && (atype.getAmmoType() == AmmoType.T_MML) && !atype.hasFlag(AmmoType.F_MML_LRM)) {
                return Messages.getString("WeaponAttackAction.NoIndirectSRM");
            }

            // Can't fire anything but Mech Mortars and Artillery Cannons indirectly without a spotter
            // unless the attack has the Oblique Attacker SPA
            if (isIndirect) {
                if ((spotter == null) && !(wtype instanceof ArtilleryCannonWeapon)
                        && !ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)
                        && !wtype.hasFlag(WeaponType.F_MORTARTYPE_INDIRECT)) {
                    return Messages.getString("WeaponAttackAction.NoSpotter");
                }
            }

            // Infantry Leg attacks and Swarm attacks
            if (Infantry.LEG_ATTACK.equals(wtype.getInternalName()) && te != null) {
                toHit = Compute.getLegAttackBaseToHit(ae, te, game);

                // Return if the attack is impossible.
                if (TargetRoll.IMPOSSIBLE == toHit.getValue()) {
                    return toHit.getDesc();
                }
                // Out of range?
                if (Compute.effectiveDistance(game, ae, target) > 0) {
                    return Messages.getString("WeaponAttackAction.OutOfRange");
                }
                // Can't combine leg attacks with other attacks
                if (!WeaponAttackAction.isOnlyAttack(game, ae, Infantry.LEG_ATTACK, te)) {
                    return Messages.getString("WeaponAttackAction.LegAttackOnly");
                }
            } else if (Infantry.SWARM_MEK.equals(wtype.getInternalName()) && te != null) {
                toHit = Compute.getSwarmMekBaseToHit(ae, te, game);

                // Return if the attack is impossible.
                if (TargetRoll.IMPOSSIBLE == toHit.getValue()) {
                    return toHit.getDesc();
                }
                // Out of range?
                if (Compute.effectiveDistance(game, ae, target) > 0) {
                    return Messages.getString("WeaponAttackAction.OutOfRange");
                }
                // Can't combine swarm attacks with other attacks
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
            }
            // Swarming infantry always hit their target, but
            // they can only target the Mek they're swarming.
            if ((te != null) && (ae.getSwarmTargetId() == te.getId())) {
                // Weapons that do no damage cannot be used in swarm attacks
                if (wtype.getDamage() == 0) {
                    return Messages.getString("WeaponAttackAction.0DamageWeapon");
                }
                // Missiles and BA body-mounted weapons cannot be used when swarming
                if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    return Messages.getString("WeaponAttackAction.NoMissileWhenSwarming");
                }
                if (weapon.isBodyMounted()) {
                    return Messages.getString("WeaponAttackAction.NoBodyWhenSwarming");
                }
            } else if (Entity.NONE != ae.getSwarmTargetId()) {
                return Messages.getString("WeaponAttackAction.MustTargetSwarmed");
            }

            // MG arrays

            // Can't fire one if none of the component MGs are functional
            if (wtype.hasFlag(WeaponType.F_MGA) && (weapon.getCurrentShots() == 0)) {
                return Messages.getString("WeaponAttackAction.NoWorkingMGs");
            }
            // Or if the array is off
            if (wtype.hasFlag(WeaponType.F_MGA) && weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_AMS_OFF)) {
                return Messages.getString("WeaponAttackAction.MGArrayOff");
            } else if (wtype.hasFlag(WeaponType.F_MG)) {
                // and you can't fire an individual MG if it's in an array
                if (ae.hasLinkedMGA(weapon)) {
                    return Messages.getString("WeaponAttackAction.MGPartOfArray");
                }
            }

            // Protomech can fire MGA only into front arc, TW page 137
            if (!Compute.isInArc(ae.getPosition(), ae.getFacing(), target, Compute.ARC_FORWARD)
                    && wtype.hasFlag(WeaponType.F_MGA) && (ae instanceof Protomech)) {
                return Messages.getString("WeaponAttackAction.ProtoMGAOnlyFront");
            }

            // NARC and iNARC
            if ((wtype.getAmmoType() == AmmoType.T_NARC) || (wtype.getAmmoType() == AmmoType.T_INARC)) {
                // Cannot be used against targets inside buildings
                if (targetInBuilding) {
                    return Messages.getString("WeaponAttackAction.NoNarcInBuilding");
                }
                // and can't be fired at infantry
                if (target instanceof Infantry) {
                    return Messages.getString("WeaponAttackAction.CantNarcInfantry");
                }
            }

            // PPCs linked to capacitors can't fire while charging
            if (weapon.getType().hasFlag(WeaponType.F_PPC) && (weapon.getLinkedBy() != null)
                    && weapon.getLinkedBy().getType().hasFlag(MiscType.F_PPC_CAPACITOR)
                    && weapon.getLinkedBy().pendingMode().equals(Weapon.MODE_PPC_CHARGE)) {
                return Messages.getString("WeaponAttackAction.PPCCharging");
            }

            // Some weapons can only be fired by themselves

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
            }

            // Protomechs cannot fire arm weapons and main gun in the same turn
            if ((ae instanceof Protomech)
                    && ((weapon.getLocation() == Protomech.LOC_MAINGUN)
                    || (weapon.getLocation() == Protomech.LOC_RARM)
                    || (weapon.getLocation() == Protomech.LOC_LARM))) {
                final boolean firingMainGun = weapon.getLocation() == Protomech.LOC_MAINGUN;
                for (EntityAction ea : game.getActionsVector()) {
                    if ((ea.getEntityId() == attackerId) && (ea instanceof WeaponAttackAction)) {
                        WeaponAttackAction otherWAA = (WeaponAttackAction) ea;
                        final Mounted otherWeapon = ae.getEquipment(otherWAA.getWeaponId());
                        if ((firingMainGun && ((otherWeapon.getLocation() == Protomech.LOC_RARM)
                                || (otherWeapon.getLocation() == Protomech.LOC_LARM)))
                                || !firingMainGun && (otherWeapon.getLocation() == Protomech.LOC_MAINGUN)) {
                            return Messages.getString("WeaponAttackAction.CantFireArmsAndMainGun");
                        }
                    }
                }
            }

            // TAG

            // The TAG system cannot target Airborne Aeros.
            if (isTAG && (te != null) && (te.isAirborne() || te.isSpaceborne())) {
                return Messages.getString("WeaponAttackAction.CantTAGAero");
            }

            // The TAG system cannot target infantry.
            if (isTAG && (te != null) && (te instanceof Infantry)) {
                return Messages.getString("WeaponAttackAction.CantTAGInf");
            }

            // TSEMPs

            // Can't fire a one-shot TSEMP more than once
            if (wtype.hasFlag(WeaponType.F_TSEMP) && wtype.hasFlag(WeaponType.F_ONESHOT) && weapon.isFired()) {
                return Messages.getString("WeaponAttackAction.OneShotTSEMP");
            }

            // Can't fire a regular TSEMP while it is recharging
            if (wtype.hasFlag(WeaponType.F_TSEMP) && weapon.isFired()) {
                return Messages.getString("WeaponAttackAction.TSEMPRecharging");
            }

            // Weapon Bays

            // Large Craft weapon bays cannot bracket small craft at short range
            if (weapon.hasModes()
                    && (weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_80) || weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_60)
                            || weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_40))
                    && target.isAero() && te!= null && !te.isLargeCraft()
                    && (RangeType.rangeBracket(ae.getPosition().distance(target.getPosition()), wtype.getRanges(weapon, ammo),
                            true, false) == RangeType.RANGE_SHORT)) {
                return Messages.getString("WeaponAttackAction.TooCloseForSCBracket");
            }

            // you must have enough weapons in your bay to be able to use bracketing
            if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_80) && (weapon.getBayWeapons().size() < 2)) {
                return Messages.getString("WeaponAttackAction.BayTooSmallForBracket");
            }
            if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_60) && (weapon.getBayWeapons().size() < 3)) {
                return Messages.getString("WeaponAttackAction.BayTooSmallForBracket");
            }
            if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_40) && (weapon.getBayWeapons().size() < 4)) {
                return Messages.getString("WeaponAttackAction.BayTooSmallForBracket");
            }

            // If you're an aero, can't fire an AMS Bay at all or a Point Defense bay that's in PD Mode
            if (wtype.hasFlag(WeaponType.F_AMSBAY)) {
                return Messages.getString("WeaponAttackAction.AutoWeapon");
            } else if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_POINT_DEFENSE)) {
                return Messages.getString("WeaponAttackAction.PDWeapon");
            }

            // Weapon in arc?
            if (!Compute.isInArc(game, attackerId, weaponId, target)
                    && (!Compute.isAirToGround(ae, target) || isArtilleryIndirect)
                    && !ae.isMakingVTOLGroundAttack()
                    && !ae.isOffBoard()) {
                return Messages.getString("WeaponAttackAction.OutOfArc");
            }

            // Weapon operational?
            // TODO move to top for early-out if possible, as this is the most common
            // reason shot is impossible
            if ((!evenIfAlreadyFired) && (!weapon.canFire(isStrafing, evenIfAlreadyFired))) {
                return Messages.getString("WeaponAttackAction.WeaponNotReady");
            }
        }

        // If we get here, the shot is possible
        return null;
    }

    /**
     * Method that tests each attack to see if it would automatically hit.
     * If so, a reason string will be returned. A null return means we can continue
     * processing the attack
     *
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param los The calculated LOS between attacker and target
     *
     * @param distance  The distance in hexes from attacker to target
     *
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     *
     * @param isBearingsOnlyMissile  flag that indicates whether this is a bearings-only capital missile attack
     */
    private static String toHitIsAutomatic(Game game, Entity ae, Targetable target, int ttype, LosEffects los,
            int distance, WeaponType wtype, Mounted weapon, boolean isBearingsOnlyMissile) {

        // Buildings

        // Attacks against adjacent buildings automatically hit.
        if ((distance == 1) && ((ttype == Targetable.TYPE_BUILDING)
                || (ttype == Targetable.TYPE_BLDG_IGNITE)
                || (ttype == Targetable.TYPE_FUEL_TANK)
                || (ttype == Targetable.TYPE_FUEL_TANK_IGNITE)
                || (target instanceof GunEmplacement))) {
            return Messages.getString("WeaponAttackAction.AdjBuilding");
        }

        // Attacks against buildings from inside automatically hit.
        if ((null != los.getThruBldg()) && ((ttype == Targetable.TYPE_BUILDING)
                || (ttype == Targetable.TYPE_BLDG_IGNITE)
                || (ttype == Targetable.TYPE_FUEL_TANK)
                || (ttype == Targetable.TYPE_FUEL_TANK_IGNITE)
                || (target instanceof GunEmplacement))) {
            return Messages.getString("WeaponAttackAction.InsideBuilding");
        }

        // Special Weapon Rules

        // B-Pod firing at infantry in the same hex autohit
        if (wtype != null && wtype.hasFlag(WeaponType.F_B_POD) && (target instanceof Infantry)
                && target.getPosition().equals(ae.getPosition())) {
            return Messages.getString("WeaponAttackAction.BPodAtInf");
        }

        // Capital Missiles in bearings-only mode target hexes and always hit them
        if (isBearingsOnlyMissile) {
            if (game.getPhase().isTargeting() && (distance >= RangeType.RANGE_BEARINGS_ONLY_MINIMUM)) {
                return Messages.getString("WeaponAttackAction.BoMissileHex");
            }
        }

        // Screen launchers target hexes and hit automatically (if in range)
        if (wtype != null && ((wtype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)
                || (wtype instanceof ScreenLauncherBayWeapon)) && distance <= wtype.getExtremeRange()) {
            return Messages.getString("WeaponAttackAction.ScreenAutoHit");
        }

        // Vehicular grenade launchers
        if (weapon != null && weapon.getType().hasFlag(WeaponType.F_VGL)) {
            int facing = weapon.getFacing();
            if (ae.isSecondaryArcWeapon(ae.getEquipmentNum(weapon))) {
                facing = (facing + ae.getSecondaryFacing()) % 6;
            }
            Coords c = ae.getPosition().translated(facing);
            if ((target instanceof HexTarget) && target.getPosition().equals(c)) {
                return Messages.getString("WeaponAttackAction.Vgl");
            }
        }

        // If we get here, the shot isn't an auto-hit
        return null;
    }

    /**
     * Some attacks are the only actions that a particular entity can make
     * during its turn Also, only this unit can make that particular attack.
     */
    private static boolean isOnlyAttack(Game game, Entity attacker, String attackType, Entity target) {
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
        int[] bombPayload = new int[BombType.B_NUM];
        for (int i=0; i<bombPayloads.get("internal").length; i++) {
            bombPayload[i] = bombPayloads.get("internal")[i] + bombPayloads.get("external")[i];
        }
        return bombPayload;
    }

    public HashMap<String, int[]> getBombPayloads() {
        return bombPayloads;
    }

    /**
     *
     * @param bpls These are the "bomb payload" for internal and external bomb stores.
     *             It's a HashMap of two arrays, each indexed by the constants declared in BombType.
     * Each element indicates how many types of that bomb should be fired.
     */
    public void setBombPayloads(HashMap<String, int[]> bpls) {
        bombPayloads = (HashMap<String, int[]>) bpls.clone();
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
    public void updateTurnsTilHit(Game game) {
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the weather or other special environmental
     * effects. These affect everyone on the board.
     * @param game The current {@link Game}
     * @param ae The attacking entity
     * @param target The Targetable object being attacked
     * @param wtype The WeaponType of the weapon being used
     * @param atype The AmmoType being used for this attack
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * @param isArtilleryIndirect  flag that indicates whether this is an indirect-fire artillery attack
     */
    private static ToHitData compileEnvironmentalToHitMods(Game game, Entity ae, Targetable target, WeaponType wtype,
                AmmoType atype, ToHitData toHit, boolean isArtilleryIndirect) {
        PlanetaryConditions conditions = game.getPlanetaryConditions();

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // Night combat modifiers
        if (!isArtilleryIndirect) {
            toHit.append(AbstractAttackAction.nightModifiers(game, target, atype, ae, true));
        }

        TargetRoll weatherToHitMods = new TargetRoll();

        // weather mods (not in space)
        int weatherMod = conditions.getWeatherHitPenalty(ae);
        if ((weatherMod != 0) && !game.getBoard().inSpace()) {
            weatherToHitMods.addModifier(weatherMod, conditions.getWeather().toString());
        }

        // wind mods (not in space)
        if (!game.getBoard().inSpace()) {
            if (conditions.getWind().isModerateGale()) {
                if (wtype != null && wtype.hasFlag(WeaponType.F_MISSILE)) {
                    weatherToHitMods.addModifier(1, conditions.getWind().toString());
                }
            } else if (conditions.getWind().isModerateGale()) {
                if (wtype != null && wtype.hasFlag(WeaponType.F_BALLISTIC) && wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    weatherToHitMods.addModifier(1, conditions.getWind().toString());
                } else if (wtype != null && wtype.hasFlag(WeaponType.F_MISSILE)) {
                    weatherToHitMods.addModifier(2, conditions.getWind().toString());
                }
            } else if (conditions.getWind().isStorm()) {
                if (wtype != null && wtype.hasFlag(WeaponType.F_BALLISTIC) && wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    weatherToHitMods.addModifier(2, conditions.getWind().toString());
                } else if (wtype != null && wtype.hasFlag(WeaponType.F_MISSILE)) {
                    weatherToHitMods.addModifier(3, conditions.getWind().toString());
                }
            } else if (conditions.getWind().isTornadoF1ToF3()) {
                if (wtype != null && wtype.hasFlag(WeaponType.F_ENERGY)) {
                    weatherToHitMods.addModifier(2, conditions.getWind().toString());
                } else if (wtype != null && wtype.hasFlag(WeaponType.F_BALLISTIC) && wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    weatherToHitMods.addModifier(3, conditions.getWind().toString());
                }
            } else if (conditions.getWind().isTornadoF4()) {
                weatherToHitMods.addModifier(3, conditions.getWind().toString());
            }
        }

        // fog mods (not in space)
        if (wtype != null
                && wtype.hasFlag(WeaponType.F_ENERGY)
                && !game.getBoard().inSpace()
                && conditions.getFog().isFogHeavy()) {
            weatherToHitMods.addModifier(1, Messages.getString("WeaponAttackAction.HeavyFog"));
        }

        // blowing sand mods
        if (wtype != null
                && wtype.hasFlag(WeaponType.F_ENERGY)
                && !game.getBoard().inSpace()
                && conditions.isBlowingSandActive()) {
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
            int mod = (int) Math.floor(Math.abs((conditions.getGravity() - 1.0f) / 0.2f));
            if ((mod != 0) && wtype != null &&
                    ((wtype.hasFlag(WeaponType.F_BALLISTIC) && wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) || wtype.hasFlag(WeaponType.F_MISSILE))) {
                toHit.addModifier(mod, Messages.getString("WeaponAttackAction.Gravity"));
            }
        }

        // Electro-Magnetic Interference
        if (conditions.getEMI().isEMI()
                && !ae.isConventionalInfantry()) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.EMI"));
        }
        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the weapon being fired
     * Got a heavy large laser that gets a +1 TH penalty?  You'll find that here.
     * Bonuses related to the attacker's condition?  Ammunition being used?  Those are in other methods.
     *
     * @param game The current {@link Game}
     * @param ae    The attacking entity
     * @param spotter   The spotting entity, if using indirect fire
     * @param target The Targetable object being attacked
     * @param ttype  The Targetable object type
     * @param toHit The running total ToHitData for this WeaponAttackAction
     *
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used for this attack
     * @param atype The AmmoType being used for this attack
     * @param munition  Long indicating the munition type flag being used, if applicable
     *
     * @param isFlakAttack  flag that indicates whether the attacker is using Flak against an airborne target
     * @param isIndirect  flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param narcSpotter  flag that indicates whether this spotting entity is using NARC equipment
     */
    private static ToHitData compileWeaponToHitMods(Game game, Entity ae, Entity spotter, Targetable target,
                int ttype, ToHitData toHit, WeaponType wtype, Mounted weapon, AmmoType atype, Mounted ammo, EnumSet<AmmoType.Munitions> munition,
                boolean isFlakAttack, boolean isIndirect, boolean narcSpotter) {
        if (ae == null || wtype == null || weapon == null) {
            // Can't calculate weapon mods without a valid weapon and an attacker to fire it
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            //Some of these weapons only target valid entities
            te = (Entity) target;
        }

        // +4 for trying to fire ASEW or antiship missile at a target of < 500 tons
        if ((wtype.hasFlag(WeaponType.F_ANTI_SHIP) || wtype.getAmmoType() == AmmoType.T_ASEW_MISSILE)
                && (te != null) && (te.getWeight() < 500)) {
            toHit.addModifier(4, Messages.getString("WeaponAttackAction.TeTooSmallForASM"));
        }

        // AAA mode makes targeting large craft more difficult
        if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAP_LASER_AAA) && te != null && te.isLargeCraft()) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AAALaserAtShip"));
        }

        // Bombast Lasers
        if (wtype instanceof ISBombastLaser) {
            double damage = Compute.dialDownDamage(weapon, wtype);
            damage = Math.ceil((damage - 7) / 2);

            if (damage > 0) {
                toHit.addModifier((int) damage, Messages.getString("WeaponAttackAction.WeaponMod"));
            }
        }

        // Bracketing modes
        if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_80)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.Bracket80"));
        }
        if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_60)) {
            toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Bracket60"));
        }
        if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAPITAL_BRACKET_40)) {
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
                && (wtype.getAtClass() != WeaponType.CLASS_AR10) && te != null && !te.isLargeCraft()) {
            // Capital Lasers have an AAA mode for shooting at small targets
            int aaaMod = 0;
            if (weapon.hasModes() && weapon.curMode().equals(Weapon.MODE_CAP_LASER_AAA)) {
                aaaMod = 2;
            }
            if (wtype.isSubCapital()) {
                toHit.addModifier(3 - aaaMod, Messages.getString("WeaponAttackAction.SubCapSmallTe"));
            } else {
                toHit.addModifier(5 - aaaMod, Messages.getString("WeaponAttackAction.CapSmallTe"));
            }
        }

        // Check whether we're eligible for a flak bonus...
        if (isFlakAttack) {
            // ...and if so, which one (HAGs get an extra -1 as per TW p. 136
            // that's not covered by anything else).
            if (atype != null && atype.getAmmoType() == AmmoType.T_HAG) {
                toHit.addModifier(-3, Messages.getString("WeaponAttackAction.HagFlak"));
            } else {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Flak"));
            }
        }

        // Flat to hit modifiers defined in WeaponType
        if (wtype.getToHitModifier() != 0) {
            int modifier = wtype.getToHitModifier();
            if (wtype instanceof VariableSpeedPulseLaserWeapon) {
                int nRange = ae.getPosition().distance(target.getPosition());
                int[] nRanges = wtype.getRanges(weapon, ammo);

                if (nRange <= nRanges[RangeType.RANGE_SHORT]) {
                    modifier += RangeType.RANGE_SHORT;
                } else if (nRange <= nRanges[RangeType.RANGE_MEDIUM]) {
                    modifier += RangeType.RANGE_MEDIUM;
                } else {
                    modifier += RangeType.RANGE_LONG;
                }
            }
            toHit.addModifier(modifier, Messages.getString("WeaponAttackAction.WeaponMod"));
        }

        // Indirect fire (LRMs, mortars and the like) has a +1 mod
        if (isIndirect) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.Indirect"));
            // Unless the attacker has the Oblique Attacker SPA
            if (ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ObliqueAttacker"));
            }
        }

        // Indirect fire suffers a +1 penalty if the spotter is making attacks of its own
        if (isIndirect) {
            // semiguided ammo negates this modifier, if TAG succeeded
            if ((atype != null) && ((atype.getAmmoType() == AmmoType.T_LRM)
                    || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_MML)
                    || (atype.getAmmoType() == AmmoType.T_NLRM)
                    || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                    && (munition.contains(AmmoType.Munitions.M_SEMIGUIDED))) {

                if (Compute.isTargetTagged(target, game)) {
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SemiGuidedIndirect"));
                }
            } else if (!narcSpotter && (spotter != null)) {
                // Unless the target has been tagged, or the spotter has an active command console
                toHit.append(Compute.getSpotterMovementModifier(game, spotter.getId()));
                if (spotter.isAttackingThisTurn() && !spotter.getCrew().hasActiveCommandConsole() &&
                        !Compute.isTargetTagged(target, game)) {
                    toHit.addModifier(1, Messages.getString("WeaponAttackAction.SpotterAttacking"));
                }
            }
        }

        // And if this is a Mech Mortar
        if (wtype.hasFlag(WeaponType.F_MORTARTYPE_INDIRECT)) {
            if (isIndirect) {
                // +2 penalty if there's no spotting entity
                if (spotter == null) {
                    toHit.addModifier(2, Messages.getString("WeaponAttackAction.NoSpotter"));
                }
            } else {
                // +3 penalty for a direct-fire shot
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.DirectMortar"));
            }
        }

        // +1 to hit if the Kinder Rapid-Fire ACs optional rule is turned on, but only Jams on a 2.
        // See TacOps Autocannons for the rest of the rules
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_KIND_RAPID_AC)
                && weapon.curMode().equals(Weapon.MODE_AC_RAPID)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AcRapid"));
        }

        // VSP Lasers
        // Quirks and SPAs now handled in toHit

        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the ammunition being used
     * Using precision AC rounds that get a -1 TH bonus?  You'll find that here.
     * Bonuses related to the attacker's condition?  Using a weapon with a TH penalty?  Those are in other methods.
     *
     * @param game The current {@link Game}
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
     * @param bApollo flag that indicates whether the attacker is using an Apollo FCS for MRMs
     * @param bArtemisV flag that indicates whether the attacker is using an Artemis V FCS
     * @param bFTL flag that indicates whether the attacker is using FTL missiles
     * @param bHeatSeeking flag that indicates whether the attacker is using Heat Seeking missiles
     * @param isECMAffected flag that indicates whether the target is inside an ECM bubble
     * @param isINarcGuided flag that indicates whether the target is broadcasting an iNarc beacon
     */
    private static ToHitData compileAmmoToHitMods(Game game, Entity ae, Targetable target, int ttype, ToHitData toHit,
                WeaponType wtype, Mounted weapon, AmmoType atype, EnumSet<AmmoType.Munitions> munition, boolean bApollo, boolean bArtemisV,
                boolean bFTL, boolean bHeatSeeking, boolean isECMAffected, boolean isINarcGuided) {
        if (ae == null || atype == null) {
            // Can't calculate ammo mods without valid ammo and an attacker to fire it
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            //Some ammo can only target valid entities
            te = (Entity) target;
        }

        // Autocannon Munitions

        // Armor Piercing ammo is a flat +1
        if (((atype.getAmmoType() == AmmoType.T_AC)
                        || (atype.getAmmoType() == AmmoType.T_LAC)
                        || (atype.getAmmoType() == AmmoType.T_AC_IMP)
                        || (atype.getAmmoType() == AmmoType.T_PAC))
                && (munition.contains(AmmoType.Munitions.M_ARMOR_PIERCING))) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.ApAmmo"));
        }

        // Bombs

        // Air-to-air Arrow and Light Air-to-air missiles
        if (((atype.getAmmoType() == AmmoType.T_AAA_MISSILE) || (atype.getAmmoType() == AmmoType.T_LAA_MISSILE))
                && Compute.isAirToGround(ae, target)) {
            // +4 penalty if trying to use one against a ground target
            toHit.addModifier(+4, Messages.getString("WeaponAttackAction.AaaGroundAttack"));
            // +3 additional if the attacker is flying at Altitude 3 or less
            if (ae.getAltitude() < 4) {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.AaaLowAlt"));
            }
        }

        // Flat modifiers defined in AmmoType
        if (atype.getToHitModifier() != 0) {
            toHit.addModifier(atype.getToHitModifier(),
                    atype.getSubMunitionName() + Messages.getString("WeaponAttackAction.AmmoMod"));
        }

        // Missile Munitions

        // Apollo FCS for MRMs
        if (bApollo) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ApolloFcs"));
        }

        // add Artemis V bonus
        if (bArtemisV) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ArtemisV"));
        }

        // Follow-the-leader LRMs
        if (bFTL) {
            toHit.addModifier(2, atype.getSubMunitionName()
                    + Messages.getString("WeaponAttackAction.AmmoMod"));
        }

        // Heat Seeking Missiles
        if (bHeatSeeking) {
            Hex hexTarget = game.getBoard().getHex(target.getPosition());
            // -2 bonus if shooting at burning hexes or buildings
            if (te == null && hexTarget.containsTerrain(Terrains.FIRE)) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.AmmoMod"));
            }
            if (te != null) {
                // -2 bonus if the target is on fire
                if (te.infernos.isStillBurning()) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.AmmoMod"));

                }
                if ((te.isAirborne()) && (toHit.getSideTable() == ToHitData.SIDE_REAR)) {
                    // -2 bonus if shooting an Aero through the rear arc
                    toHit.addModifier(-2, atype.getSubMunitionName()
                            + Messages.getString("WeaponAttackAction.AmmoMod"));
                } else if (te.heat == 0) {
                    // +1 penalty if shooting at a non-heat-tracking unit or a heat-tracking unit at 0 heat
                    toHit.addModifier(1, atype.getSubMunitionName()
                            + Messages.getString("WeaponAttackAction.AmmoMod"));
                } else {
                    // -1 bonus for each -1MP the target would get due to heat
                    toHit.addModifier(-te.getHeatMPReduction(),
                            atype.getSubMunitionName()
                                    + Messages.getString("WeaponAttackAction.AmmoMod"));
                }
            }

            // +2 penalty if shooting into or through a burning hex
            if (LosEffects.hasFireBetween(ae.getPosition(), target.getPosition(), game)) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.HsmThruFire"));
            }
        }

        // Narc-capable missiles homing on an iNarc beacon
        if (isINarcGuided) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.iNarcHoming"));
        }

        // Listen-Kill ammo from War of 3039 sourcebook?
        if (!isECMAffected
                && ((atype.getAmmoType() == AmmoType.T_LRM)
                        || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                        || (atype.getAmmoType() == AmmoType.T_MML)
                        || (atype.getAmmoType() == AmmoType.T_SRM)
                        || (atype.getAmmoType() == AmmoType.T_SRM_IMP))
                && (munition.contains(AmmoType.Munitions.M_LISTEN_KILL)) && !((te != null) && te.isClan())) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.ListenKill"));
        }

        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the attacker's condition
     * Attacker has damaged sensors?  You'll find that here.
     * Defender's a superheavy mech?  Using a weapon with a TH penalty?  Those are in other methods.
     *
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param los The calculated LOS between attacker and target
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * @param toSubtract An int value representing a running total of mods to disregard - used for some special attacks
     *
     * @param aimingAt  An int value representing the location being aimed at
     * @param aimingMode  An int value that determines the reason aiming is allowed
     *
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param weaponId  The id number of the weapon being used - used by some external calculations
     * @param atype The AmmoType being used for this attack
     * @param munition  Long indicating the munition type flag being used, if applicable
     *
     * @param isFlakAttack  flag that indicates whether the attacker is using Flak against an airborne target
     * @param isHaywireINarced  flag that indicates whether the attacker is affected by an iNarc Haywire pod
     * @param isNemesisConfused  flag that indicates whether the attack is affected by an iNarc Nemesis pod
     * @param isWeaponFieldGuns  flag that indicates whether the attack is being made with infantry field guns
     * @param usesAmmo  flag that indicates if the WeaponType being used is ammo-fed
     */
    private static ToHitData compileAttackerToHitMods(Game game, Entity ae, Targetable target,
                                                      LosEffects los, ToHitData toHit,
                                                      int toSubtract, int aimingAt,
                                                      AimingMode aimingMode, WeaponType wtype,
                                                      Mounted weapon, int weaponId, AmmoType atype,
                                                      EnumSet<AmmoType.Munitions> munition, boolean isFlakAttack,
                                                      boolean isHaywireINarced,
                                                      boolean isNemesisConfused,
                                                      boolean isWeaponFieldGuns, boolean usesAmmo) {
        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // if we don't have a weapon, that we are attacking with, then the rest of this is
        // either meaningless or likely to fail
        if (weaponId == WeaponType.WEAPON_NA) {
            return toHit;
        }

        // Modifiers related to an action the attacker is taking

        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, ae.getId()));

        // attacker prone
        if (weaponId > WeaponType.WEAPON_NA) {
            toHit.append(Compute.getProneMods(game, ae, weaponId));
        }

        // add penalty for called shots and change hit table, if necessary
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS)
                && weapon != null) {
            int call = weapon.getCalledShot().getCall();
            if ((call > CalledShot.CALLED_NONE) && !aimingMode.isNone()) {
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

        // Dropping units get hit with a +2 dropping penalty AND the +3 Jumping penalty (SO p22)
        if (ae.isAirborne() && !ae.isAero()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.Dropping"));
            toHit.addModifier(+3, Messages.getString("WeaponAttackAction.Jumping"));
        }

        // Infantry taking cover suffer a +1 penalty
        if ((ae instanceof Infantry) && ((Infantry) ae).isTakingCover()) {
            if (ae.getPosition().direction(target.getPosition()) == ae.getFacing()) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.FireThruCover"));
            }
        }

        // Quadvee converting to a new mode
        if (ae instanceof QuadVee && ae.isConvertingNow()) {
            toHit.addModifier(+3, Messages.getString("WeaponAttackAction.QuadVeeConverting"));
        }

        // we are bracing
        if (ae.isBracing() && (ae.braceLocation() == weapon.getLocation())) {
            toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Bracing"));
        }

        // Secondary targets modifier,
        // if this is not a iNarc Nemesis confused attack
        // Inf field guns don't get secondary target mods, TO pg 311
        if (!isNemesisConfused && !isWeaponFieldGuns) {
            toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
        }

        // if we're spotting for indirect fire, add +1
        if (ae.isSpotting() && !ae.getCrew().hasActiveCommandConsole()
                && game.getTagInfo().stream().noneMatch(inf -> inf.attackerId == ae.getId())) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeSpotting"));
        }

        // Special effects (like tasers) affecting the attacker

        // Attacker is battle armor and affected by BA taser feedback
        if (ae.getTaserFeedBackRounds() > 0) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeTaserFeedback"));
        }

        // If a unit is suffering from electromagnetic interference, they get a
        // blanket +2. Sucks to be them.
        if (ae.isSufferingEMI()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.EMI"));
        }

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), Messages.getString("WeaponAttackAction.Heat"));
        }

        // Attacker hit with an iNarc Haywire pod
        if (isHaywireINarced) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.iNarcHaywire"));
        }

        // Attacker affected by Taser interference
        if (ae.getTaserInterferenceRounds() > 0) {
            toHit.addModifier(ae.getTaserInterference(), Messages.getString("WeaponAttackAction.AeHitByTaser"));
        }

        // Attacker affected by TSEMP interference
        if (ae.getTsempEffect() == MMConstants.TSEMP_EFFECT_INTERFERENCE) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeTsemped"));
        }

        // Special Equipment that that attacker possesses

        // Attacker has an AES system
        if (weapon != null && ae.hasFunctionalArmAES(weapon.getLocation()) && !weapon.isSplit()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.AES"));
        }

        // Heavy infantry have +1 penalty
        if ((ae instanceof Infantry) && ae.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.HeavyArmor"));
        }

        // industrial cockpit: +1 to hit, +2 for primitive
        if ((ae instanceof Mech) && (((Mech) ae).getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.PrimIndustrialNoAfc"));
        } else if ((ae instanceof Mech) && !((Mech) ae).hasAdvancedFireControl()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.IndustrialNoAfc"));
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
        if (ae.hasShield() && weapon != null) {
            // active shield has already been checked as it makes shots
            // impossible
            // time to check passive defense and no defense

            if (ae.hasPassiveShield(weapon.getLocation(), weapon.isRearMounted())) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.PassiveShield"));
            } else if (ae.hasNoDefenseShield(weapon.getLocation())) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Shield"));
            }
        }

        // add targeting computer (except with LBX cluster ammo)
        if (aimingMode.isTargetingComputer() && (aimingAt != Entity.LOC_NONE)) {
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
                    && munition.contains(AmmoType.Munitions.M_CLUSTER);
            boolean usesHAGFlak = usesAmmo && (atype != null) && atype.getAmmoType() == AmmoType.T_HAG && isFlakAttack;
            boolean isSBGauss = usesAmmo && (atype != null) && atype.getAmmoType() == AmmoType.T_SBGAUSS;
            boolean isFlakAmmo = usesAmmo && (atype != null) && (munition.contains(AmmoType.Munitions.M_FLAK));
            if (ae.hasTargComp() && wtype != null && wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && !wtype.hasFlag(WeaponType.F_CWS)
                    && !wtype.hasFlag(WeaponType.F_TASER)
                    && (!usesAmmo || !(usesLBXCluster || usesHAGFlak || isSBGauss || isFlakAmmo))) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TComp"));
            }
        }

        // penalty for an active void signature system
        if (ae.isVoidSigActive()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeVoidSig"));
        }

        // Critical damage effects

        // actuator & sensor damage to attacker (includes partial repairs)
        if (weapon != null) {
            toHit.append(Compute.getDamageWeaponMods(ae, weapon));
        }

        // Vehicle criticals
        if (ae instanceof Tank) {
            Tank tank = (Tank) ae;
            int sensors = tank.getSensorHits();
            if (sensors > 0) {
                toHit.addModifier(sensors, Messages.getString("WeaponAttackAction.SensorDamage"));
            }
            if (weapon != null && tank.isStabiliserHit(weapon.getLocation())) {
                toHit.addModifier(Compute.getAttackerMovementModifier(game, tank.getId()).getValue(),
                        "stabiliser damage");
            }
        }

        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the attacker's condition,
     * if the attacker is an aero
     * Attacker has damaged sensors?  You'll find that here.
     * Defender's a superheavy mech?  Using a weapon with a TH penalty?  Those are in other methods.
     *
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param toHit The running total ToHitData for this WeaponAttackAction
     *
     * @param aimingAt  An int value representing the location being aimed at
     * @param aimingMode  An int value that determines the reason aiming is allowed
     * @param eistatus An int value representing the ei cockpit/pilot upgrade status
     *
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param atype The AmmoType being used for this attack
     * @param munition  Long indicating the munition type flag being used, if applicable
     *
     * @param isArtilleryIndirect  flag that indicates whether this is an indirect-fire artillery attack
     * @param isFlakAttack  flag that indicates whether the attacker is using Flak against an airborne target
     * @param isNemesisConfused  flag that indicates whether the attack is affected by an iNarc Nemesis pod
     * @param isStrafing    flag that indicates whether this is an aero strafing attack
     * @param usesAmmo  flag that indicates if the WeaponType being used is ammo-fed
     */
    private static ToHitData compileAeroAttackerToHitMods(Game game, Entity ae, Targetable target,
                                                          int ttype, ToHitData toHit, int aimingAt,
                                                          AimingMode aimingMode, int eistatus,
                                                          WeaponType wtype, Mounted weapon,
                                                          AmmoType atype, EnumSet<AmmoType.Munitions> munition,
                                                          boolean isArtilleryIndirect,
                                                          boolean isFlakAttack,
                                                          boolean isNemesisConfused,
                                                          boolean isStrafing, boolean usesAmmo) {
        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            //Some of these weapons only target valid entities
            te = (Entity) target;
        }

        // Generic modifiers that apply to airborne and ground attackers

        // actuator & sensor damage to attacker (includes partial repairs)
        if (weapon != null) {
            toHit.append(Compute.getDamageWeaponMods(ae, weapon));
        }

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), Messages.getString("WeaponAttackAction.Heat"));
        }

        // Secondary targets modifier, if this is not a iNarc Nemesis confused attack
        // Also does not apply for altitude bombing or strafing
        if (!isNemesisConfused && wtype != null && !wtype.hasFlag(WeaponType.F_ALT_BOMB) && !isStrafing) {
            toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
        }

        // add targeting computer (except with LBX cluster ammo)
        if (aimingMode.isTargetingComputer() && (aimingAt != Entity.LOC_NONE)) {
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
                    && munition.contains(AmmoType.Munitions.M_CLUSTER);
            boolean usesHAGFlak = usesAmmo && (atype != null) && atype.getAmmoType() == AmmoType.T_HAG && isFlakAttack;
            boolean isSBGauss = usesAmmo && (atype != null) && atype.getAmmoType() == AmmoType.T_SBGAUSS;
            boolean isFlakAmmo = usesAmmo && (atype != null) && (munition.contains(AmmoType.Munitions.M_FLAK));
            if (ae.hasTargComp() && wtype != null && wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && !wtype.hasFlag(WeaponType.F_CWS)
                    && !wtype.hasFlag(WeaponType.F_TASER)
                    && (!usesAmmo || !(usesLBXCluster || usesHAGFlak || isSBGauss || isFlakAmmo))) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TComp"));
            }
        }

        // Modifiers for aero units, including fighter LAMs
        if (ae.isAero()) {
            IAero aero = (IAero) ae;

            // check for heavy gauss rifle on fighter of small craft
            // Arguably a weapon effect, except that it only applies when used by a fighter (isn't recoil fun?)
            // So it's here instead of with other weapon mods that apply across the board
            if ((wtype != null) &&
                    ((wtype.ammoType == AmmoType.T_GAUSS_HEAVY) ||
                    (wtype.ammoType == AmmoType.T_IGAUSS_HEAVY)) &&
                    !(ae instanceof Dropship)
                    && !(ae instanceof Jumpship)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.FighterHeavyGauss"));
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
                if (weapon != null) {
                    int loc = weapon.getLocation();
                    if (d.getASEWAffected(loc) > 0) {
                        toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeArcAsewAffected"));
                    }
                }
            } else if (ae instanceof Jumpship) {
                Jumpship j = (Jumpship) ae;
                if (weapon != null) {
                    int loc = weapon.getLocation();
                    if (j.getASEWAffected(loc) > 0) {
                        toHit.addModifier(4, Messages.getString("WeaponAttackAction.AeArcAsewAffected"));
                    }
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

            // air-to-ground strikes
            if (Compute.isAirToGround(ae, target)
                    || (ae.isMakingVTOLGroundAttack())) {
                // When altitude bombing, add the altitude as a modifier
                if (wtype != null && wtype.hasFlag(WeaponType.F_ALT_BOMB)) {
                    toHit.addModifier(ae.getAltitude(), Messages.getString("WeaponAttackAction.BombAltitude"));
                    // -2 for the Golden Goose SPA
                    if (ae.hasAbility(OptionsConstants.GUNNERY_GOLDEN_GOOSE)) {
                        toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GoldenGoose"));
                    }
                // +4 Modifier for strafing
                } else if (isStrafing) {
                    toHit.addModifier(+4, Messages.getString("WeaponAttackAction.Strafing"));
                    // Additional +2 if flying at Nap-of-Earth
                    if (ae.getAltitude() == 1) {
                        toHit.addModifier(+2, Messages.getString("WeaponAttackAction.StrafingNoe"));
                    }
                    // Additional Nap-of-Earth restrictions for strafing
                    if (ae.getAltitude() == 1) {
                        Coords prevCoords = ae.passedThroughPrevious(target.getPosition());
                        Hex prevHex = game.getBoard().getHex(prevCoords);
                        toHit.append(Compute.getStrafingTerrainModifier(game, eistatus, prevHex));
                    }
                } else {
                    // +2 modifier for striking
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AtgStrike"));
                    if (ae.hasAbility(OptionsConstants.GUNNERY_GOLDEN_GOOSE)) {
                        if (wtype != null && wtype.hasFlag(WeaponType.F_DIVE_BOMB)) {
                            // -2 for the Golden Goose SPA if dive bombing
                            toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GoldenGoose"));
                        } else {
                            // -1 for the Golden Goose SPA on strike attacks
                            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GoldenGoose"));
                        }
                    }
                }
            }
            // units making air to ground attacks are easier to hit by air-to-air
            // attacks
            if (Compute.isAirToAir(ae, target)) {
                for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements();) {
                    EntityAction ea = i.nextElement();
                    if (!(ea instanceof WeaponAttackAction)) {
                        continue;
                    }
                    WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
                    if ((te != null && prevAttack.getEntityId() == te.getId()) && prevAttack.isAirToGround(game)) {
                        toHit.addModifier(-3, Messages.getString("WeaponAttackAction.TeGroundAttack"));
                        break;
                    }
                }
            }
            // grounded aero
            if (!ae.isAirborne() && !ae.isSpaceborne()) {
                if (ae.isFighter()) {
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
            if (aero.isEvading() && wtype != null &&
                    (!(wtype.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE
                            || wtype.getAtClass() == WeaponType.CLASS_AR10
                            || wtype.getAtClass() == WeaponType.CLASS_TELE_MISSILE))) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeEvading"));
            }

            // stratops page 113: ECHO maneuvers for large craft
            if (((aero instanceof Warship) || (aero instanceof Dropship)) &&
                    (aero.getFacing() != aero.getSecondaryFacing())) {
                // if we're computing this for an "attack preview", then we add 2 MP to
                // the mp used, as we haven't used the MP yet. If we're actually processing
                // the attack, then the entity will be marked as 'done' and we have already added
                // the 2 MP, so we don't need to double-count it
                int extraMP = aero.isDone() ? 0 : 2;
                boolean willUseRunMP = aero.mpUsed + extraMP > aero.getWalkMP();
                int mod = willUseRunMP ? 2 : 1;
                toHit.addModifier(mod, Messages.getString("WeaponAttackAction.LargeCraftEcho"));
            }

            // check for particular kinds of weapons in weapon bays
            if (ae.usesWeaponBays() && wtype != null && weapon != null) {

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
                            if (!batype.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) {
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
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param te The target Entity
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * @param weapon The weapon being used (it's type should be WeaponType!)
     *
     */
    private static ToHitData compileCrewToHitMods(Game game, Entity ae, Entity te, ToHitData toHit, Mounted weapon) {

        if (ae == null) {
            // These checks won't work without a valid attacker
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        //Now for modifiers affecting the attacker's crew

        // Bonuses for dual cockpits, etc
        // Bonus to gunnery if both crew members are active; a pilot who takes the gunner's role get +1.
        if (ae instanceof Mech && ((Mech) ae).getCockpitType() == Mech.COCKPIT_DUAL) {
            if (!ae.getCrew().isActive(ae.getCrew().getCrewType().getGunnerPos())) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.GunnerHit"));
            } else if (ae.getCrew().hasDedicatedGunner()) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.DualCockpit"));
            }
        }

        // The pilot or technical officer can take over the gunner's duties but suffers a +2 penalty.
        if ((ae instanceof TripodMech || ae instanceof QuadVee) && !ae.getCrew().hasDedicatedGunner()) {
            toHit.addModifier(+2, Messages.getString("WeaponAttackAction.GunnerHit"));
        }

        // Fatigue
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_FATIGUE)
                && ae.getCrew().isGunneryFatigued()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.Fatigue"));
        }

        // Injuries

        // Aero unit pilot/crew hits
        if (ae instanceof Aero) {
            int pilothits = ae.getCrew().getHits();
            if ((pilothits > 0) && !ae.isCapitalFighter()) {
                toHit.addModifier(pilothits, Messages.getString("WeaponAttackAction.PilotHits"));
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

        // Manei Domini Upgrades

        // VDNI
        if (ae.hasAbility(OptionsConstants.MD_VDNI)
                || ae.hasAbility(OptionsConstants.MD_BVDNI)) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.Vdni"));
        }

        WeaponType wtype = ((weapon != null) && (weapon.getType() instanceof WeaponType)) ? (WeaponType) weapon.getType() : null;

        if (ae.isConventionalInfantry()) {
            // check for cyber eye laser sighting on ranged attacks
            if (ae.hasAbility(OptionsConstants.MD_CYBER_IMP_LASER)
                    && !(wtype instanceof InfantryAttack)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.MdEye"));
            }
        }

        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the defender's condition and actions
     * -4 for shooting at an immobile target?  You'll find that here.
     * Attacker strafing?  Using a weapon with a TH penalty?  Those are in other methods.
     * For simplicity's sake, Quirks and SPAs now get applied here for general cases (elsewhere for Artillery or ADA)
     *
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param los The calculated LOS between attacker and target
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * @param toSubtract An int value representing a running total of mods to disregard - used for some special attacks
     *
     * @param aimingAt  An int value representing the location being aimed at - used by immobile target calculations
     * @param aimingMode  An int value that determines the reason aiming is allowed - used by immobile target calculations
     * @param distance  The distance in hexes from attacker to target
     *
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param atype The AmmoType being used for this attack
     * @param munition  Long indicating the munition type flag being used, if applicable
     *
     * @param isArtilleryDirect  flag that indicates whether this is a direct-fire artillery attack
     * @param isArtilleryIndirect  flag that indicates whether this is an indirect-fire artillery attack
     * @param isAttackerInfantry  flag that indicates whether the attacker is an infantry/BA unit
     * @param exchangeSwarmTarget  flag that indicates whether this is the secondary target of Swarm LRMs
     * @param isIndirect  flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param isPointBlankShot  flag that indicates whether or not this is a PBS by a hidden unit
     * @param usesAmmo  flag that indicates whether or not the WeaponType being used is ammo-fed
     */
    private static ToHitData compileTargetToHitMods(Game game, Entity ae, Targetable target,
                                                    int ttype, LosEffects los, ToHitData toHit,
                                                    int toSubtract, int aimingAt,
                                                    AimingMode aimingMode, int distance,
                                                    WeaponType wtype, Mounted weapon, AmmoType atype,
                                                    EnumSet<AmmoType.Munitions> munition, boolean isArtilleryDirect,
                                                    boolean isArtilleryIndirect,
                                                    boolean isAttackerInfantry,
                                                    boolean exchangeSwarmTarget, boolean isIndirect,
                                                    boolean isPointBlankShot, boolean usesAmmo) {
        if (ae == null || target == null) {
            // Can't handle these attacks without a valid attacker and target
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        //Target's hex
        Hex targHex = game.getBoard().getHex(target.getPosition());

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            //Some weapons only target valid entities
            te = (Entity) target;
        }

        // Modifiers related to a special action the target is taking

        // evading bonuses
        if ((te != null) && te.isEvading()) {
            toHit.addModifier(te.getEvasionBonus(), Messages.getString("WeaponAttackAction.TeEvading"));
        }

        // Hull Down
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

        // Infantry taking cover per TacOps special rules
        if ((te instanceof Infantry) && ((Infantry) te).isTakingCover()) {
            if (te.getPosition().direction(ae.getPosition()) == te.getFacing()) {
                toHit.addModifier(+3, Messages.getString("WeaponAttackAction.FireThruCover"));
            }
        }

        // target prone
        ToHitData proneMod = null;
        if ((te != null) && te.isProne()) {
            // easier when point-blank
            if (distance <= 1) {
                // TW, pg. 221: Swarm Mek attacks apply prone/immobile mods as normal.
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

        // Special effects affecting the target

        // Target grappled?
        if (te != null) {
            int grapple = te.getGrappled();
            if (grapple != Entity.NONE) {
                // -4 bonus if attacking the entity you're grappling
                if ((grapple == ae.getId()) && (te.getGrappleSide() == Entity.GRAPPLE_BOTH)) {
                    toHit.addModifier(-4, Messages.getString("WeaponAttackAction.Grappled"));
                // -2 bonus if grappling the target at range with a chain whip
                } else if ((grapple == ae.getId()) && (te.getGrappleSide() != Entity.GRAPPLE_BOTH)) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.GrappledByChain"));
                // +1 penalty if firing at a target grappled by another unit. This does not apply to Swarm LRMs
                } else if (!exchangeSwarmTarget) {
                    toHit.addModifier(1, Messages.getString("WeaponAttackAction.FireIntoMelee"));
                } else {
                    // this -1 cancels the original +1
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.FriendlyFire"));
                }
            }
        }

        // Special Equipment and Quirks that the target possesses

        // ECM suite generating Ghost Targets
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_GHOST_TARGET) && !isIndirect
                && !isArtilleryIndirect && !isArtilleryDirect) {
            int ghostTargetMod = Compute.getGhostTargetNumber(ae, ae.getPosition(), target.getPosition());
            if ((ghostTargetMod > -1) && !ae.isConventionalInfantry()) {
                int bapMod = 0;
                if (ae.hasBAP()) {
                    bapMod = 1;
                }
                int tcMod = 0;
                if (ae.hasTargComp() && wtype != null
                        && wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && !wtype.hasFlag(WeaponType.F_CWS)
                        && !wtype.hasFlag(WeaponType.F_TASER) && (atype != null)
                        && (!usesAmmo || !(((atype.getAmmoType() == AmmoType.T_AC_LBX)
                                || (atype.getAmmoType() == AmmoType.T_AC_LBX_THB))
                                && (munition.contains(AmmoType.Munitions.M_CLUSTER))))) {
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

        // Movement and Position modifiers

        // target movement - ignore for pointblank shots from hidden units
        if ((te != null) && !isPointBlankShot) {
            ToHitData thTemp = Compute.getTargetMovementModifier(game, target.getId());
            toHit.append(thTemp);
            toSubtract += thTemp.getValue();

            // semiguided ammo negates this modifier, if TAG succeeded
            if ((atype != null) && ((atype.getAmmoType() == AmmoType.T_LRM)
                    || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (atype.getAmmoType() == AmmoType.T_MML)
                    || (atype.getAmmoType() == AmmoType.T_NLRM)
                    || (atype.getAmmoType() == AmmoType.T_MEK_MORTAR))
                    && (munition.contains(AmmoType.Munitions.M_SEMIGUIDED)) && (te.getTaggedBy() != -1)) {
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
                    && (munition.contains(AmmoType.Munitions.M_PRECISION))) {
                int nAdjust = Math.min(2, thTemp.getValue());
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, Messages.getString("WeaponAttackAction.Precision")));
                }
            }
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

        // target immobile
        boolean mekMortarMunitionsIgnoreImmobile = wtype != null && wtype.hasFlag(WeaponType.F_MEK_MORTAR)
                && (atype != null) && (munition.contains(AmmoType.Munitions.M_AIRBURST));
        if (wtype != null && !(wtype instanceof ArtilleryCannonWeapon) && !mekMortarMunitionsIgnoreImmobile) {
            ToHitData immobileMod;
            // grounded dropships are treated as immobile as well for purpose of
            // the mods
            if ((null != te) && !te.isAirborne() && !te.isSpaceborne() && (te instanceof Dropship)) {
                immobileMod = new ToHitData(-4, Messages.getString("WeaponAttackAction.ImmobileDs"));
            } else {
                immobileMod = Compute.getImmobileMod(target, aimingAt, aimingMode);
            }

            if (immobileMod != null) {
                toHit.append(immobileMod);
                toSubtract += immobileMod.getValue();
            }
        }

        // Unit-specific modifiers

        // -1 to hit a SuperHeavy mech
        if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMech"));
        }

        // large support tanks get a -1 per TW
        if ((te != null) && (te.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) && !te.isAirborne() && !te.isSpaceborne()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeLargeSupportUnit"));
        }

        // "grounded small craft" get a -1 per TW
        if ((te instanceof SmallCraft) && (te.getUnitType() == UnitType.SMALL_CRAFT) && !te.isAirborne() && !te.isSpaceborne()) {
            toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeGroundedSmallCraft"));
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if (!isAttackerInfantry && (te instanceof BattleArmor)) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.BaTarget"));
        }

        if ((te instanceof Infantry) && te.isConventionalInfantry()) {
            // infantry squads are also hard to hit
            if (((Infantry) te).isSquad()) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.SquadTarget"));
            }
            InfantryMount mount = ((Infantry) te).getMount();
            if ((mount != null) && (mount.getSize().toHitMod != 0)) {
                toHit.addModifier(mount.getSize().toHitMod, Messages.getString("WeaponAttackAction.MountSize"));
            }
        }

        // pl-masc makes foot infantry harder to hit - IntOps p.84
        if ((te instanceof Infantry) && te.hasAbility(OptionsConstants.MD_PL_MASC)
                && te.getMovementMode().isLegInfantry()
                && te.isConventionalInfantry()) {
            toHit.addModifier(1, Messages.getString("WeaponAttackAction.PlMasc"));
        }

        // Ejected MechWarriors are harder to hit
        if (te instanceof MechWarrior) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.MwTarget"));
        }

        // Aerospace target modifiers
        if (te != null && te.isAero() && te.isAirborne()) {
            IAero a = (IAero) te;

            // is the target at zero velocity
            if ((a.getCurrentVelocity() == 0) && !(a.isSpheroid() && !game.getBoard().inSpace())) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.ImmobileAero"));
            }

            // get mods for direction of attack
            if (!(a.isSpheroid() && !game.getBoard().inSpace())) {
                int side = Compute.targetSideTable(ae.getPosition(), te);

                // +1 if shooting at an aero approaching nose-on
                if (side == ToHitData.SIDE_FRONT) {
                    toHit.addModifier(+1, Messages.getString("WeaponAttackAction.AeroNoseAttack"));
                }
                // +2 if shooting at the side as it flashes by
                if ((side == ToHitData.SIDE_LEFT) || (side == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(+2, Messages.getString("WeaponAttackAction.AeroSideAttack"));
                }
            }

            // Target hidden in the sensor shadow of a larger spacecraft
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SENSOR_SHADOW)
                    && game.getBoard().inSpace()) {
                for (Entity en : Compute.getAdjacentEntitiesAlongAttack(ae.getPosition(), target.getPosition(), game)) {
                    if (!en.isEnemyOf(te) && en.isLargeCraft()
                            && ((en.getWeight() - te.getWeight()) >= -STRATOPS_SENSOR_SHADOW_WEIGHT_DIFF)) {
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

        // Quirks
        processAttackerQuirks(toHit, ae, te, weapon);

        // SPAs
        processAttackerSPAs(toHit, ae, te, weapon, game);
        processDefenderSPAs(toHit, ae, te, weapon, game);

        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the terrain and line of sight (LOS)
     * Woods along the LOS?  Target Underwater?  Partial cover? You'll find that here.
     * Also, if the to-hit table is changed due to cover/angle/elevation, look here.
     * -4 for shooting at an immobile target?  Using a weapon with a TH penalty? Those are in other methods.
     *
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param aElev An int value representing the attacker's elevation
     * @param tElev An int value representing the target's elevation
     * @param targEl An int value representing the target's relative elevation
     * @param distance The distance in hexes from attacker to target
     * @param los The calculated LOS between attacker and target
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * @param losMods A cached set of LOS-related modifiers
     * @param toSubtract An int value representing a running total of mods to disregard - used for some special attacks
     *
     * @param eistatus An int value representing the ei cockpit/pilot upgrade status
     *
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param weaponId  The id number of the weapon being used - used by some external calculations
     * @param atype The AmmoType being used for this attack
     * @param munition  Long indicating the munition type flag being used, if applicable
     *
     * @param inSameBuilding  flag that indicates whether this attack originates from within the same building
     * @param isIndirect  flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param isPointBlankShot  flag that indicates whether or not this is a PBS by a hidden unit
     * @param underWater  flag that indicates whether the weapon being used is underwater
     */
    private static ToHitData compileTerrainAndLosToHitMods(Game game, Entity ae, Targetable target, int ttype, int aElev, int tElev,
                int targEl, int distance, LosEffects los, ToHitData toHit, ToHitData losMods, int toSubtract, int eistatus,
                WeaponType wtype, Mounted weapon, int weaponId, AmmoType atype, Mounted ammo, EnumSet<AmmoType.Munitions> munition, boolean isAttackerInfantry,
                boolean inSameBuilding, boolean isIndirect, boolean isPointBlankShot, boolean underWater) {
        if (ae == null || target == null) {
            // Can't handle these attacks without a valid attacker and target
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        //Target's hex
        Hex targHex = game.getBoard().getHex(target.getPosition());

        boolean targetHexContainsWater = targHex != null && targHex.containsTerrain(Terrains.WATER);
        boolean targetHexContainsFortified = targHex != null && targHex.containsTerrain(Terrains.FORTIFIED);

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            //Some of these weapons only target valid entities
            te = (Entity) target;
        }

        // Add range mods - If the attacker and target are in the same building
        // & hex, range mods don't apply (and will cause the shot to fail)
        // Don't apply this to bomb attacks either, which are going to be at 0 range of necessity
        // Also don't apply to ADA Missiles (range computed separately)
        if (((los.getThruBldg() == null) || !los.getTargetPosition().equals(ae.getPosition()))
                && (wtype != null
                    && (!(wtype.hasFlag(WeaponType.F_ALT_BOMB)
                        || wtype.hasFlag(WeaponType.F_DIVE_BOMB)
                        || (atype != null && atype.getMunitionType().contains(AmmoType.Munitions.M_ADA))))
                && weaponId > WeaponType.WEAPON_NA)) {
            toHit.append(Compute.getRangeMods(game, ae, weapon, ammo, target));
        }

        // add in LOS mods that we've been keeping
        toHit.append(losMods);

        // Attacker Terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, ae.getId()));

        // Target Terrain

        // BMM p. 31, semi-guided indirect missile attacks vs tagged targets ignore terrain modifiers
        boolean semiGuidedIndirectVsTaggedTarget = isIndirect &&
                (atype != null) && atype.getMunitionType().contains(AmmoType.Munitions.M_SEMIGUIDED) &&
                        Compute.isTargetTagged(target, game);

        // TW p.111
        boolean indirectMortarWithoutSpotter = (wtype != null) && wtype.hasFlag(WeaponType.F_MORTARTYPE_INDIRECT)
                && isIndirect && (Compute.findSpotter(game, ae, target) == null);

        // Base terrain calculations, not applicable when delivering minefields or bombs
        // also not applicable in pointblank shots from hidden units
        if ((ttype != Targetable.TYPE_MINEFIELD_DELIVER) && !isPointBlankShot && !semiGuidedIndirectVsTaggedTarget
                && !indirectMortarWithoutSpotter) {
            toHit.append(Compute.getTargetTerrainModifier(game, target, eistatus, inSameBuilding, underWater));
        }

        // Fortified/Dug-In Infantry
        if ((target instanceof Infantry) && wtype != null && !wtype.hasFlag(WeaponType.F_FLAMER)) {
            if (targetHexContainsFortified
                    || (((Infantry) target).getDugIn() == Infantry.DUG_IN_COMPLETE)) {
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.DugInInf"));
            }
        }

        // target in water?
        int partialWaterLevel = 1;
        if (te != null && (te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
            partialWaterLevel = 2;
        }
        if ((te != null) && targetHexContainsWater
                // target in partial water
                && (targHex.terrainLevel(Terrains.WATER) == partialWaterLevel) && (targEl == 0) && (te.height() > 0)) {
            los.setTargetCover(los.getTargetCover() | LosEffects.COVER_HORIZONTAL);
            losMods = los.losModifiers(game, eistatus, underWater);
        }

        // Change hit table for partial cover, accommodate for partial underwater (legs)
        if (los.getTargetCover() != LosEffects.COVER_NONE) {
            if (underWater && (targetHexContainsWater && (targEl == 0)
                    && (te != null && te.height() > 0))) {
                // weapon underwater, target in partial water
                toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                toHit.setCover(LosEffects.COVER_UPPER);
            } else {
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER)) {
                    toHit.setCover(los.getTargetCover());
                } else {
                    toHit.setCover(LosEffects.COVER_HORIZONTAL);
                }
                // If this is a called shot (high) the table has already been set and should be used instead of partial cover.
                if (toHit.getHitTable() != ToHitData.HIT_ABOVE) {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                }
                // Set damageable cover state information
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

        // Special Equipment

        // BAP Targeting rule enabled - TO:AR 6th p.97
        // have line of sight and there are woods in our way
        // we have BAP in range or C3 member has BAP in range
        // we reduce the BTH by 1
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_BAP)) {
            boolean targetWoodsAffectModifier = te != null
                    && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_WOODS_COVER)
                    && (game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.WOODS)
                    || game.getBoard().getHex(te.getPosition()).containsTerrain(Terrains.JUNGLE));
            if (los.canSee() && (targetWoodsAffectModifier || los.thruWoods())) {
                boolean bapInRange = Compute.bapInRange(game, ae, te);
                boolean c3BAP = false;
                if (!bapInRange) {
                    for (Entity en : game.getC3NetworkMembers(ae)) {
                        if (ae.equals(en)) {
                            continue;
                        }
                        bapInRange = Compute.bapInRange(game, en, te);
                        if (bapInRange) {
                            c3BAP = true;
                            break;
                        }
                    }
                }
                if (bapInRange) {
                    if (c3BAP) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BAPInWoodsC3"));
                    } else {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BAPInWoods"));
                    }
                }
            }
        }

        // To-hit table changes with no to-hit modifiers

        // Aeros in air-to-air combat can hit above and below
        if (Compute.isAirToAir(ae, target)) {
            if ((ae.getAltitude() - target.getAltitude()) > 2) {
                toHit.setHitTable(ToHitData.HIT_ABOVE);
            } else if ((target.getAltitude() - ae.getAltitude()) > 2) {
                toHit.setHitTable(ToHitData.HIT_BELOW);
            } else if (((ae.getAltitude() - target.getAltitude()) > 0)
                    && te != null && (te.isAero() && ((IAero) te).isSpheroid())) {
                toHit.setHitTable(ToHitData.HIT_ABOVE);
            } else if (((ae.getAltitude() - target.getAltitude()) < 0)
                    && te != null && (te.isAero() && ((IAero) te).isSpheroid())) {
                toHit.setHitTable(ToHitData.HIT_BELOW);
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

        // Ground-to-air attacks always hit from below
        if (Compute.isGroundToAir(ae, target) && ((target.getAltitude() - ae.getAltitude()) > 2)) {
            toHit.setHitTable(ToHitData.HIT_BELOW);
        }

        // factor in target side
        if (isAttackerInfantry && (0 == distance)) {
            // Infantry attacks from the same hex are resolved against the
            // front.
            toHit.setSideTable(ToHitData.SIDE_FRONT);
        } else {
            if (weapon != null) {
                toHit.setSideTable(Compute.targetSideTable(ae, target, weapon.getCalledShot().getCall()));
            }
        }

        // Change hit table for surface naval vessels hit by underwater attacks
        if (underWater && targetHexContainsWater && (null != te) && te.isSurfaceNaval()) {
            toHit.setHitTable(ToHitData.HIT_UNDERWATER);
        }

        return toHit;
    }

    /**
     * Quick routine to determine if the target should be treated as being in a short building.
     */
    public static boolean targetInShortCoverBuilding(Targetable target) {
        if (target.getTargetType() != Targetable.TYPE_ENTITY) {
            return false;
        }

        Entity targetEntity = (Entity) target;

        Hex targetHex = targetEntity.getGame().getBoard().getHex(target.getPosition());
        if (targetHex == null) {
            return false;
        }

        // the idea here is that we're in a building that provides partial cover
        // if the unit involved is tall (at least 2 levels, e.g. mech or superheavy vehicle)
        // and its height above the hex ceiling (i.e building roof) is 1
        // the height determination takes being prone into account
        return targetHex.containsTerrain(Terrains.BUILDING) &&
                (targetEntity.getHeight() > 0) &&
                (targetEntity.relHeight() == 1);
    }

    /**
     * If you're using a weapon that does something totally special and doesn't apply mods like everything else, look here
     *
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param los The calculated LOS between attacker and target
     * @param toHit The running total ToHitData for this WeaponAttackAction
     *
     * @param wtype The WeaponType of the weapon being used
     * @param atype The AmmoType being used for this attack
     * @param srt  Class that stores whether or not this WAA should return a special resolution
     */
    private static ToHitData handleSpecialWeaponAttacks(Game game, Entity ae, Targetable target, int ttype,
                LosEffects los, ToHitData toHit, WeaponType wtype, AmmoType atype, SpecialResolutionTracker srt) {
        if (ae == null) {
            //*Should* be impossible at this point in the process
            return toHit;
        }

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            //Some of these weapons only target valid entities
            te = (Entity) target;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // Battle Armor bomb racks (Micro bombs) use gunnery skill and no other mods per TWp228 2018 errata
        if ((atype != null) && (atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB)) {
            if (ae.getPosition().equals(target.getPosition())) {
                toHit = new ToHitData(ae.getCrew().getGunnery(), Messages.getString("WeaponAttackAction.GunSkill"));
            } else {
                toHit = new ToHitData(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.OutOfRange"));
            }
            srt.setSpecialResolution(true);
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
            srt.setSpecialResolution(true);
            return toHit;
        }

        // if this is a space bombing attack then get the to hit and return
        if (wtype.hasFlag(WeaponType.F_SPACE_BOMB)) {
            if (te != null) {
                toHit = Compute.getSpaceBombBaseToHit(ae, te, game);
                srt.setSpecialResolution(true);
                return toHit;
            }
        }

        //If we get here, no special weapons apply. Return the input data and continue on
        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to infantry/BA swarm attacks
     *
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param toHit The running total ToHitData for this WeaponAttackAction
     *
     * @param wtype The WeaponType of the weapon being used
     * @param srt  Class that stores whether or not this WAA should return a special resolution
     */
    private static ToHitData handleInfantrySwarmAttacks(Game game, Entity ae, Targetable target,
                int ttype, ToHitData toHit, WeaponType wtype, SpecialResolutionTracker srt)  {
        if (ae == null) {
            //*Should* be impossible at this point in the process
            return toHit;
        }
        if (target == null || ttype != Targetable.TYPE_ENTITY) {
            //Can only swarm a valid entity target
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        Entity te = (Entity) target;
        // Leg attacks and Swarm attacks have their own base toHit values
        if (Infantry.LEG_ATTACK.equals(wtype.getInternalName())) {
            toHit = Compute.getLegAttackBaseToHit(ae, te, game);
            if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMech"));
            }
            srt.setSpecialResolution(true);
            return toHit;

        } else if (Infantry.SWARM_MEK.equals(wtype.getInternalName())) {
            toHit = Compute.getSwarmMekBaseToHit(ae, te, game);
            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                srt.setSpecialResolution(true);
                return toHit;
            }

            if (te instanceof Tank) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.TeVehicle"));
            }
            if ((te instanceof Mech) && ((Mech) te).isSuperHeavy()) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.TeSuperheavyMech"));
            }
            if (te.isProne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.TargetProne"));
            }
            if (te.isImmobile()) {
                toHit.addModifier(-4, Messages.getString("WeaponAttackAction.TargetImmobile"));
            }

            // If the defender carries mechanized BA, they can fight off the
            // swarm
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
            srt.setSpecialResolution(true);
            return toHit;
        } else if (Infantry.STOP_SWARM.equals(wtype.getInternalName())) {
            // Can't stop if we're not swarming, otherwise automatic.
            srt.setSpecialResolution(true);
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.EndSwarm"));
        }
        // Swarming infantry always hit their target, but
        // they can only target the Mek they're swarming.
        else if ((ae.getSwarmTargetId() == te.getId())) {
            int side = te instanceof Tank ? ToHitData.SIDE_RANDOM : ToHitData.SIDE_FRONT;
            if (ae instanceof BattleArmor) {
                if (!Infantry.SWARM_WEAPON_MEK.equals(wtype.getInternalName()) && !(wtype instanceof InfantryAttack)) {
                    srt.setSpecialResolution(true);
                    return new ToHitData(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.WrongSwarmUse"));
                }
                srt.setSpecialResolution(true);
                return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.SwarmingAutoHit"), ToHitData.HIT_SWARM,
                        side);
            }
            srt.setSpecialResolution(true);
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.SwarmingAutoHit"), ToHitData.HIT_SWARM_CONVENTIONAL, side);
        }
        //If we get here, no swarm attack applies
        return toHit;
    }

    /**
     * Method to handle modifiers for swarm missile secondary targets
     *
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param swarmPrimaryTarget The original Targetable object being attacked
     * @param swarmSecondaryTarget The current Targetable object being attacked
     * @param toHit The running total ToHitData for this WeaponAttackAction
     * @param toSubtract An int value representing a running total of mods to disregard
     *
     * @param eistatus An int value representing the ei cockpit/pilot upgrade status - used for terrain calculation
     * @param aimingAt  An int value representing the location being aimed at - used for immobile target
     * @param aimingMode  An int value that determines the reason aiming is allowed - used for immobile target
     *
     * @param weapon The Mounted weapon being used
     * @param atype The AmmoType being used for this attack
     * @param munition  Long indicating the munition type flag being used, if applicable
     *
     * @param isECMAffected flag that indicates whether the target is inside an ECM bubble
     * @param inSameBuilding  flag that indicates whether this attack originates from within the same building
     * @param underWater  flag that indicates whether the weapon being used is underwater
     */
    private static ToHitData handleSwarmSecondaryAttacks(Game game, Entity ae, Targetable target,
                                                         Targetable swarmPrimaryTarget,
                                                         Targetable swarmSecondaryTarget,
                                                         ToHitData toHit, int toSubtract,
                                                         int eistatus, int aimingAt,
                                                         AimingMode aimingMode, Mounted weapon,
                                                         AmmoType atype, EnumSet<AmmoType.Munitions> munition,
                                                         boolean isECMAffected,
                                                         boolean inSameBuilding, boolean underWater) {
        if (ae == null || swarmPrimaryTarget == null || swarmSecondaryTarget == null) {
            // This method won't work without these 3 things
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        toHit.addModifier(-toSubtract, Messages.getString("WeaponAttackAction.OriginalTargetMods"));
        toHit.append(Compute.getImmobileMod(swarmSecondaryTarget, aimingAt, aimingMode));
        toHit.append(Compute.getTargetTerrainModifier(game,
                game.getTarget(swarmSecondaryTarget.getTargetType(), swarmSecondaryTarget.getId()), eistatus,
                inSameBuilding, underWater));
        toHit.setCover(LosEffects.COVER_NONE);

        Hex targHex = game.getBoard().getHex(swarmSecondaryTarget.getPosition());
        int targEl = swarmSecondaryTarget.relHeight();
        int distance = Compute.effectiveDistance(game, ae, swarmSecondaryTarget);

        // We might not attack the new target from the same side as the
        // old, so recalculate; the attack *direction* is still traced from
        // the original source.
        toHit.setSideTable(Compute.targetSideTable(ae, swarmSecondaryTarget));

        // Secondary swarm LRM attacks are never called shots even if the
        // initial one was.
        if (weapon != null && weapon.getCalledShot().getCall() != CalledShot.CALLED_NONE) {
            weapon.getCalledShot().reset();
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }

        LosEffects swarmlos;
        // TO makes it seem like the terrain modifiers should be between the
        // attacker and the secondary target, but we have received rules
        // clarifications on the old forums indicating that this is correct
        if (swarmPrimaryTarget.getTargetType() != Targetable.TYPE_ENTITY) {
            swarmlos = LosEffects.calculateLos(game, swarmSecondaryTarget.getId(), target);
        } else {
            swarmlos = LosEffects.calculateLos(game, swarmPrimaryTarget.getId(), swarmSecondaryTarget);
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
        if (swarmSecondaryTarget.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity oldEnt = game.getEntity(swarmSecondaryTarget.getId());
            toHit.append(Compute.getTargetMovementModifier(game, oldEnt.getId()));
            // target in partial water - depth 1 for most units
            int partialWaterLevel = 1;
            // Depth 2 for superheavy mechs
            if (target != null && (target instanceof Mech) && ((Mech) target).isSuperHeavy()) {
                partialWaterLevel = 2;
            }
            if (targHex.containsTerrain(Terrains.WATER)
                    && (targHex.terrainLevel(Terrains.WATER) == partialWaterLevel) && (targEl == 0)
                    && (oldEnt.height() > 0)) {
                toHit.setCover(toHit.getCover() | LosEffects.COVER_HORIZONTAL);
            }
            // Prone
            ToHitData proneMod = new ToHitData();
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
                    && (munition.contains(AmmoType.Munitions.M_SWARM_I))) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.SwarmIFriendly"));
            }
        }
        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to direct artillery attacks
     *
     * @param game The current {@link Game}
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param ttype  The targetable object type
     * @param toHit The running total ToHitData for this WeaponAttackAction
     *
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     * @param atype The AmmoType being used for this attack
     *
     * @param isArtilleryFLAK   flag that indicates whether this is a flak artillery attack
     * @param usesAmmo  flag that indicates if the WeaponType being used is ammo-fed
     * @param srt  Class that stores whether or not this WAA should return a special resolution
     */
    private static ToHitData artilleryDirectToHit(Game game, Entity ae, Targetable target, int ttype,
        ToHitData losMods, ToHitData toHit, WeaponType wtype, Mounted weapon, AmmoType atype,
         boolean isArtilleryFLAK, boolean usesAmmo, SpecialResolutionTracker srt) {

        if (null == atype) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                    "No ammo type!");
        }
        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_ADA)){
            // Air-Defense Arrow missiles use a simplified to-hit calculation because they:
            // A) are Flak; B) are not Artillery, C) use three ranges (same low-alt hex, 1 hex, 2 hexes)
            // as S/M/L
            // Per TO:AR 6th printing, p153, ADA Missiles should use TW Flak rules rather than TO Artillery Flak rules.
            // Per TW pg 114, all other mods _should_ be included.

            // Special range calc
            int distance = Compute.effectiveDistance(game, ae, target);
            toHit.addModifier(Compute.getADARangeModifier(distance), Messages.getString("WeaponAttackAction.ADARangeBracket"));

            // Return without SRT set so that regular to-hit mods get applied.
            return toHit;
        }

        // ADA has its to-hit mods calculated separately; handle other direct artillery quirk and SPA mods here:
        // Quirks
        processAttackerQuirks(toHit, ae, te, weapon);

        // SPAs
        processAttackerSPAs(toHit, ae, te, weapon, game);
        processDefenderSPAs(toHit, ae, te, weapon, game);

        //If an airborne unit occupies the target hex, standard artillery ammo makes a flak attack against it
        //TN is a flat 3 + the altitude mod + the attacker's weapon skill - 2 for Flak
        //Grounded/destroyed/landed/wrecked ASF/VTOL/WiGE should be treated as normal.
        if ((isArtilleryFLAK || (atype.countsAsFlak())) && te != null) {
            if (te.isAirborne() || te.isAirborneVTOLorWIGE()) {
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.ArtyFlak"));
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.Flak"));
                if (te.getAltitude() > 3) {
                    if (te.getAltitude() > 9) {
                        toHit.addModifier(3, Messages.getString("WeaponAttackAction.AeroTeAlt10"));
                    } else if (te.getAltitude() > 6) {
                        toHit.addModifier(2, Messages.getString("WeaponAttackAction.AeroTeAlt79"));
                    } else if (te.getAltitude() > 3) {
                        toHit.addModifier(1, Messages.getString("WeaponAttackAction.AeroTeAlt46"));
                    }
                }
                srt.setSpecialResolution(true);
                return toHit;
            }
        }

        //All other direct fire artillery attacks
        toHit.addModifier(4, Messages.getString("WeaponAttackAction.DirectArty"));
        toHit.append(Compute.getAttackerMovementModifier(game, ae.getId()));
        toHit.append(losMods);
        toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
        // actuator & sensor damage to attacker
        if (weapon != null) {
            toHit.append(Compute.getDamageWeaponMods(ae, weapon));
        }
        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), Messages.getString("WeaponAttackAction.Heat"));
        }
        // weapon to-hit modifier
        if (wtype.getToHitModifier() != 0) {
            toHit.addModifier(wtype.getToHitModifier(), Messages.getString("WeaponAttackAction.WeaponMod"));
        }
        // ammo to-hit modifier
        if (usesAmmo && (atype.getToHitModifier() != 0)) {
            toHit.addModifier(atype.getToHitModifier(),
                    atype.getSubMunitionName()
                            + Messages.getString("WeaponAttackAction.AmmoMod"));
        }
        srt.setSpecialResolution(true);
        return toHit;
    }

    /**
     * Convenience method that compiles the ToHit modifiers applicable to indirect artillery attacks
     *
     * @param ae The Entity making this attack
     * @param target The Targetable object being attacked
     * @param toHit The running total ToHitData for this WeaponAttackAction
     *
     * @param wtype The WeaponType of the weapon being used
     * @param weapon The Mounted weapon being used
     *
     * @param srt  Class that stores whether or not this WAA should return a special resolution
     */
    private static ToHitData artilleryIndirectToHit(Entity ae, Targetable target,
                  ToHitData toHit, WeaponType wtype, Mounted weapon, SpecialResolutionTracker srt) {

        // See MegaMek/megamek#5168
        int mod = (ae.getPosition().distance(target.getPosition()) <= 17) ? 4 : 7;
        if (ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ATTACKER)) {
            mod--;
        }
        toHit.addModifier(mod, Messages.getString("WeaponAttackAction.IndirectArty"));
        int adjust = 0;
        if (weapon != null) {
            adjust = ae.aTracker.getModifier(weapon, target.getPosition());
        }
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
        // Capital missiles used for surface-to-surface artillery attacks
        // See SO p110
        // Start with a flat +2 modifier
        if (wtype instanceof CapitalMissileWeapon
                && Compute.isGroundToGround(ae, target)) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.SubCapArtillery"));
            // +3 additional modifier if fired underwater
            if (ae.isUnderwater()) {
                toHit.addModifier(3, Messages.getString("WeaponAttackAction.SubCapUnderwater"));
            }
            // +1 modifier if attacker cruised/walked
            if (ae.moved == EntityMovementType.MOVE_WALK) {
                toHit.addModifier(1, Messages.getString("WeaponAttackAction.Walked"));
            } else if (ae.moved == EntityMovementType.MOVE_RUN) {
                // +2 modifier if attacker ran
                toHit.addModifier(2, Messages.getString("WeaponAttackAction.Ran"));
            }
        } else if (ae.isAirborne()) {
            if (ae.getAltitude() > 6) {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.Altitude"));
            } else if (ae.getAltitude() > 3) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Altitude"));
            }
        }
        srt.setSpecialResolution(true);
        return toHit;
    }

        /**
         * Convenience method that compiles the ToHit modifiers applicable to artillery attacks
         *
         * @param game The current {@link Game}
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
         * @param srt  Class that stores whether or not this WAA should return a special resolution
         */
    private static ToHitData handleArtilleryAttacks(Game game, Entity ae, Targetable target, int ttype,
                ToHitData losMods, ToHitData toHit, WeaponType wtype, Mounted weapon, AmmoType atype,
                boolean isArtilleryDirect, boolean isArtilleryFLAK, boolean isArtilleryIndirect, boolean isHoming,
                boolean usesAmmo, SpecialResolutionTracker srt) {
        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }

        //Homing warheads just need a flat 4 to seek out a successful TAG, but Princess needs help
        //judging what a good homing target is.
        if (isHoming) {
            srt.setSpecialResolution(true);
            String msg = Messages.getString("WeaponAttackAction.HomingArty");
            // Check if any spotters can help us out...
            if (Compute.findTAGSpotter(game, ae, target, true) != null) {
                // Likelihood of hitting goes up as speed goes down...
                ToHitData thd = new ToHitData(4, msg);
                if (null != te) {
                    thd.append(
                            Compute.getTargetMovementModifier(
                                    te.getRunMP(),
                                    false,
                                    false,
                                    game)
                    );
                }
                return thd;
            } else {
                return new ToHitData(ToHitData.AUTOMATIC_FAIL, msg);
            }
        }

        //Don't bother adding up modifiers if the target hex has been hit before
        if (game.getEntity(ae.getId()).getOwner().getArtyAutoHitHexes().contains(target.getPosition())
                && !isArtilleryFLAK) {
            srt.setSpecialResolution(true);
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("WeaponAttackAction.ArtyDesTarget"));
        }

        // If we're not skipping To-Hit calculations, ensure that we have a toHit instance
        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // Handle direct artillery attacks.
        if (isArtilleryDirect) {
            return artilleryDirectToHit(game, ae, target, ttype, losMods, toHit, wtype,
                    weapon, atype, isArtilleryFLAK, usesAmmo, srt
            );
        } else if (isArtilleryIndirect) {
            //And now for indirect artillery fire; process quirks and SPAs here or they'll be missed
            // Quirks
            processAttackerQuirks(toHit, ae, te, weapon);

            // SPAs
            processAttackerSPAs(toHit, ae, te, weapon, game);
            processDefenderSPAs(toHit, ae, te, weapon, game);

            return artilleryIndirectToHit(ae, target, toHit, wtype, weapon, srt);
        }

        //If we get here, this isn't an artillery attack
        return toHit;
    }


    public static ToHitData processAttackerQuirks(ToHitData toHit, Entity ae, Targetable target, Mounted weapon){

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

        if (null != weapon) {

            // Flat -1 for Accurate Weapon
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_POS_ACCURATE)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.AccWeapon"));
            }
            // Flat +1 for Inaccurate Weapon
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.InAccWeapon"));
            }
            // Stable Weapon - Reduces running/flanking penalty by 1
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_POS_STABLE_WEAPON) && (ae.moved == EntityMovementType.MOVE_RUN)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.StableWeapon"));
            }
            // +1 for a Misrepaired Weapon - See StratOps Partial Repairs
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_MISREPAIRED)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.MisrepairedWeapon"));
            }
            // +1 for a Misreplaced Weapon - See StratOps Partial Repairs
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_MISREPLACED)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.MisreplacedWeapon"));
            }
        }
        return toHit;
    }

    public static ToHitData processAttackerSPAs(ToHitData toHit, Entity ae, Targetable target, Mounted weapon, Game game){
        PlanetaryConditions conditions = game.getPlanetaryConditions();

        // blood stalker SPA
        if (ae.getBloodStalkerTarget() > Entity.NONE) {
            // Issue #5275 - Attacker with bloodstalker SPA, `target` can be null if a building etc.
            if ((target != null) && (ae.getBloodStalkerTarget() == target.getId())) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BloodStalkerTarget"));
            } else {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.BloodStalkerNonTarget"));
            }
        }

        WeaponType wtype = ((weapon != null) && (weapon.getType() instanceof WeaponType)) ? (WeaponType) weapon.getType() : null;

        if (wtype != null) {
            // Unofficial weapon class specialist - Does not have an unspecialized penalty
            if (ae.hasAbility(OptionsConstants.UNOFF_GUNNERY_LASER)
                    && wtype.hasFlag(WeaponType.F_ENERGY)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GunLSkill"));
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
            if (wtype instanceof BayWeapon
                    && weapon.getBayWeapons().stream().map(ae::getEquipment)
                    .allMatch(w -> ae.hasAbility(OptionsConstants.GUNNERY_WEAPON_SPECIALIST, w.getName()))) {
                // All weapons in a bay must match the specialization
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.WeaponSpec"));
            } else if (ae.hasAbility(OptionsConstants.GUNNERY_WEAPON_SPECIALIST, wtype.getName())) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.WeaponSpec"));
            } else if (ae.hasAbility(OptionsConstants.GUNNERY_SPECIALIST)) {
                // aToW style gunnery specialist: -1 to specialized weapon and +1 to all other weapons
                // Note that weapon specialist supersedes gunnery specialization, so if you have
                // a specialization in Medium Lasers and a Laser specialization, you only get the -2 specialization mod
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

            // SPA Environmental Specialist
            // Could be pattern-matching instanceof in Java 17
            if (target instanceof Entity) {
                Entity te = (Entity) target;

                // Fog Specialist
                if (ae.getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_FOG)
                        && wtype.hasFlag(WeaponType.F_ENERGY)
                        && !game.getBoard().inSpace()
                        && conditions.getFog().isFogHeavy()) {
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.FogSpec"));
                }

                // Light Specialist
                if (ae.getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_LIGHT)) {
                    if (!te.isIlluminated()
                            && conditions.getLight().isDarkerThan(Light.DAY)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.LightSpec"));
                    } else if (te.isIlluminated()
                            && conditions.getLight().isPitchBack()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.LightSpec"));
                    }
                }

                // Rain Specialist
                if (ae.getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_RAIN)) {
                    if (conditions.getWeather().isLightRain()
                            && ae.isConventionalInfantry()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.RainSpec"));
                    }

                    if (conditions.getWeather().isModerateRainOrHeavyRainOrGustingRainOrDownpour()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.RainSpec"));
                    }
                }

                // Snow Specialist
                if (ae.getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_SNOW)) {
                    if (conditions.getWeather().isLightSnow()
                            && ae.isConventionalInfantry()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }

                    if (conditions.getWeather().isIceStorm()
                            && wtype.hasFlag(WeaponType.F_MISSILE)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }

                    if (conditions.getWeather().isModerateSnowOrHeavySnowOrSnowFlurriesOrSleet()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }
                }

                // Wind Specialist
                if (ae.getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)) {
                    if (conditions.getWind().isModerateGale()
                            && wtype.hasFlag(WeaponType.F_MISSILE)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }

                    if (wtype.hasFlag(WeaponType.F_MISSILE)
                            && wtype.hasFlag(WeaponType.F_BALLISTIC)
                            && conditions.getWind().isStrongGaleOrStorm()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.WindSpec"));
                    }

                    if (conditions.getWind().isStrongerThan(Wind.STORM)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.WindSpec"));
                    }
                }
            }
        }

        return toHit;
    }

    public static ToHitData processDefenderSPAs(ToHitData toHit, Entity ae, Entity te, Mounted weapon, Game game){

        if (null == te) {
            return toHit;
        }

        // Shaky Stick -  Target gets a +1 bonus against Ground-to-Air attacks
        if (te.hasAbility(OptionsConstants.PILOT_SHAKY_STICK)
                && (te.isAirborne() || te.isAirborneVTOLorWIGE())
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

        return toHit;
    }

    @Override
    public String toDisplayableString(Client client) {
        if (null == client || null == getTarget(client.getGame())) {
            LogManager.getLogger().warn("Unable to construct WAA displayable string due to null reference");
            return "Attacking Null Target with id " + getTargetId() + " using Weapon with id " + weaponId;
        }
        return "attacking " + getTarget(client.getGame()).getDisplayName() + " with " + getEntity(client.getGame()).getEquipment(weaponId).getName();
    }

    @Override
    public String toSummaryString(final Game game) {
        ToHitData toHit = this.toHit(game, true);
        String table = toHit.getTableDesc();
        final String buffer = toHit.getValueAsString() + ((!table.isEmpty()) ? ' '+table : "");
        final Entity entity = game.getEntity(this.getEntityId());
        final String weaponName =  ((WeaponType) entity.getEquipment(this.getWeaponId()).getType()).getName();
        final String ammoName = ((AmmoType) entity.getEquipment(this.getWeaponId()).getLinked().getType()).getName();
        return weaponName + " [" + ammoName + "] " + Messages.getString("BoardView1.needs") + buffer;
    }
}
