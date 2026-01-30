/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.actions;

import java.io.Serial;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.ECMInfo;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.ToHitData;
import megamek.common.actions.compute.ComputeAeroAttackerToHitMods;
import megamek.common.actions.compute.ComputeAttackerToHitMods;
import megamek.common.actions.compute.ComputeEnvironmentalToHitMods;
import megamek.common.actions.compute.ComputeTargetToHitMods;
import megamek.common.actions.compute.ComputeTerrainMods;
import megamek.common.actions.compute.ComputeToHit;
import megamek.common.board.CrossBoardAttackHelper;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.INarcPod;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Represents intention to fire a weapon at the target.
 */
public class WeaponAttackAction extends AbstractAttackAction {
    private static final MMLogger LOGGER = MMLogger.create(WeaponAttackAction.class);

    public static int DEFAULT_VELOCITY = 50;
    @Serial
    private static final long serialVersionUID = -9096603813317359351L;
    public static int UNASSIGNED = -1;

    public static final int STRATOPS_SENSOR_SHADOW_WEIGHT_DIFF = 100000;

    private int weaponId;
    private int ammoId = UNASSIGNED;
    private EnumSet<AmmoType.Munitions> ammoMunitionType = EnumSet.noneOf(AmmoType.Munitions.class);
    private int ammoCarrier = UNASSIGNED;
    private int aimedLocation = Entity.LOC_NONE;
    private AimingMode aimMode = AimingMode.NONE;
    private int otherAttackInfo = UNASSIGNED;
    private boolean nemesisConfused;
    private boolean swarmingMissiles;
    protected int launchVelocity = DEFAULT_VELOCITY;
    /**
     * Keeps track of the ID of the current primary target for a swarm missile attack.
     */
    private int oldTargetId = UNASSIGNED;
    /**
     * Keeps track of the Targetable type for the current primary target for a swarm missile attack.
     */
    private int oldTargetType;

    /**
     * Keeps track of the ID of the original target for a swarm missile attack.
     */
    private int originalTargetId = Entity.NONE;

    /**
     * Keeps track of the type of the original target for a swarm missile attack.
     */
    private int originalTargetType;

    private int swarmMissiles = 0;

    // bomb stuff
    private HashMap<String, BombLoadout> bombPayloads = new HashMap<>();

    // equipment that affects this attack (AMS, ECM?, etc.)
    // only used server-side
    private transient List<WeaponMounted> vCounterEquipment;

    /**
     * Boolean flag that determines whether this attack is part of a strafing run.
     */
    private boolean isStrafing = false;

    /**
     * Boolean flag that determines if this shot was the first one by a particular weapon in a strafing run. Used to
     * ensure that heat is only added once.
     */
    protected boolean isStrafingFirstShot = false;

    /**
     * Boolean flag that determines if this shot was fired as part of a pointblank shot from a hidden unit. In this
     * case, to-hit numbers should not be modified for terrain or movement. See TW pg 260
     */
    protected boolean isPointblankShot = false;

    /**
     * Boolean flag that determines if this shot was fired using homing ammunition. Can be checked to allow casting of
     * attack handlers to the proper homing handler.
     */
    protected boolean isHomingShot = false;

    /**
     * Memoize expectedDamage to make repeated calcs faster
     */
    private float expectedDamage = -1.0f;
    private boolean assumedHit = false;

    // default to attacking an entity
    public WeaponAttackAction(int entityId, int targetId, int weaponId) {
        super(entityId, targetId);
        this.weaponId = weaponId;
        bombPayloads.put("internal", new BombLoadout());
        bombPayloads.put("external", new BombLoadout());
    }

    public WeaponAttackAction(int entityId, int targetType, int targetId, int weaponId) {
        super(entityId, targetType, targetId);
        this.weaponId = weaponId;
        bombPayloads.put("internal", new BombLoadout());
        bombPayloads.put("external", new BombLoadout());
    }

    // Copy constructor, hopefully with enough info to generate same to-hit data.
    public WeaponAttackAction(final WeaponAttackAction other) {
        this(other.getEntityId(), other.getTargetType(), other.getTargetId(), other.getWeaponId());

        aimedLocation = other.aimedLocation;
        aimMode = other.aimMode;
        ammoCarrier = other.ammoCarrier;
        ammoId = other.ammoId;
        ammoMunitionType = other.ammoMunitionType;
        isHomingShot = other.isHomingShot;
        isPointblankShot = other.isPointblankShot;
        isStrafing = other.isStrafing;
        isStrafingFirstShot = other.isStrafingFirstShot;
        launchVelocity = other.launchVelocity;
        nemesisConfused = other.nemesisConfused;
        oldTargetId = other.oldTargetId;
        oldTargetType = other.oldTargetType;
        originalTargetId = other.originalTargetId;
        originalTargetType = other.originalTargetType;
        otherAttackInfo = other.otherAttackInfo;
        swarmingMissiles = other.swarmingMissiles;
        swarmMissiles = other.swarmMissiles;
        weaponId = other.weaponId;
        bombPayloads.put("internal", new BombLoadout(other.bombPayloads.get("internal")));
        bombPayloads.put("external", new BombLoadout(other.bombPayloads.get("external")));
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
     *
     */
    public int getAmmoCarrier() {
        return ammoCarrier;
    }

    public int getAimedLocation() {
        return aimedLocation;
    }

    public List<WeaponMounted> getCounterEquipment() {
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
     * Sets the entity id of the ammo carrier for this shot, if different from the firing entity
     *
     */
    public void setAmmoCarrier(int entityId) {
        ammoCarrier = entityId;
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

    public void addCounterEquipment(WeaponMounted m) {
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
        return Compute.isAirToAir(game, getEntity(game), getTarget(game));
    }

    public boolean isGroundToAir(Game game) {
        return Compute.isGroundToAir(getEntity(game), getTarget(game));
    }

    public boolean isOrbitToSurface(Game game) {
        return CrossBoardAttackHelper.isOrbitToSurface(game, getEntity(game), getTarget(game));
    }

    /**
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public boolean isDiveBomb(Game game) {
        return getEntity(game).getEquipment(getWeaponId()).getType().hasFlag(WeaponType.F_DIVE_BOMB);
    }

    public int getAltitudeLoss(Game game) {
        if (isAirToGround(game)) {
            if (getEntity(game).getEquipment(getWeaponId()).getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
                return 2;
            }
            if (getEntity(game).getEquipment(getWeaponId()).getType().hasFlag(WeaponType.F_ALT_BOMB)) {
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
        return toHit(game,
              getEntityId(),
              game.getTarget(getTargetType(), getTargetId()),
              getWeaponId(),
              getAimedLocation(),
              getAimingMode(),
              nemesisConfused,
              swarmingMissiles,
              game.getTarget(getOldTargetType(), getOldTargetId()),
              game.getTarget(getOriginalTargetType(), getOriginalTargetId()),
              isStrafing(),
              isPointblankShot(),
              UNASSIGNED,
              UNASSIGNED);
    }

    /**
     * @param evenIfAlreadyFired false: an already fired weapon will return a ToHitData with value IMPOSSIBLE true: an
     *                           already fired weapon will return a ToHitData with the value of its chance to hit
     */
    public ToHitData toHit(Game game, boolean evenIfAlreadyFired) {
        return toHit(game,
              getEntityId(),
              game.getTarget(getTargetType(), getTargetId()),
              getWeaponId(),
              getAimedLocation(),
              getAimingMode(),
              nemesisConfused,
              swarmingMissiles,
              game.getTarget(getOldTargetType(), getOldTargetId()),
              game.getTarget(getOriginalTargetType(), getOriginalTargetId()),
              isStrafing(),
              isPointblankShot(),
              evenIfAlreadyFired,
              ammoId,
              ammoCarrier);
    }

    public ToHitData toHit(Game game, List<ECMInfo> allECMInfo) {
        return ComputeToHit.toHitCalc(game,
              getEntityId(),
              game.getTarget(getTargetType(), getTargetId()),
              getWeaponId(),
              getAimedLocation(),
              getAimingMode(),
              nemesisConfused,
              swarmingMissiles,
              game.getTarget(getOldTargetType(), getOldTargetId()),
              game.getTarget(getOriginalTargetType(), getOriginalTargetId()),
              isStrafing(),
              isPointblankShot(),
              allECMInfo,
              false,
              ammoId,
              ammoCarrier);
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target, int weaponId, boolean isStrafing) {
        // Use -1 as ammoId because this method should always use the currently linked
        // ammo for display calculations
        return toHit(game,
              attackerId,
              target,
              weaponId,
              Entity.LOC_NONE,
              AimingMode.NONE,
              false,
              false,
              null,
              null,
              isStrafing,
              false,
              UNASSIGNED,
              UNASSIGNED);
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target, int weaponId, int aimingAt,
          AimingMode aimingMode, boolean isStrafing) {
        // Use -1 as ammoId because this method should always use the currently linked
        // ammo for display calculations
        return toHit(game,
              attackerId,
              target,
              weaponId,
              aimingAt,
              aimingMode,
              false,
              false,
              null,
              null,
              isStrafing,
              false,
              UNASSIGNED,
              UNASSIGNED);
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target, int weaponId, int aimingAt,
          AimingMode aimingMode, boolean isNemesisConfused, boolean exchangeSwarmTarget, Targetable oldTarget,
          Targetable originalTarget, boolean isStrafing, boolean isPointblankShot, int ammoId, int ammoCarrier) {
        return ComputeToHit.toHitCalc(game,
              attackerId,
              target,
              weaponId,
              aimingAt,
              aimingMode,
              isNemesisConfused,
              exchangeSwarmTarget,
              oldTarget,
              originalTarget,
              isStrafing,
              isPointblankShot,
              null,
              false,
              ammoId,
              ammoCarrier);
    }

    public static ToHitData toHit(Game game, int attackerId, Targetable target, int weaponId, int aimingAt,
          AimingMode aimingMode, boolean isNemesisConfused, boolean exchangeSwarmTarget, Targetable oldTarget,
          Targetable originalTarget, boolean isStrafing, boolean isPointblankShot, boolean evenIfAlreadyFired,
          int ammoId, int ammoCarrier) {
        return ComputeToHit.toHitCalc(game,
              attackerId,
              target,
              weaponId,
              aimingAt,
              aimingMode,
              isNemesisConfused,
              exchangeSwarmTarget,
              oldTarget,
              originalTarget,
              isStrafing,
              isPointblankShot,
              null,
              evenIfAlreadyFired,
              ammoId,
              ammoCarrier);
    }

    /**
     * To-hit number for attacker firing a generic weapon at the target. Does not factor in any special weapon or ammo
     * considerations, including range modifiers. Also does not include gunnery skill. However, to accurately decide
     * which targets should appear as "valid", we need to know the weapon mode and ammo type.
     *
     * @param game The current {@link Game}
     */
    public static ToHitData toHit(Game game, int attackerId, Optional<WeaponMounted> weaponOpt,
          Optional<AmmoMounted> ammoOpt, Optional<Entity> spotterOpt,
          Targetable target) {
        final Entity attackingEntity = game.getEntity(attackerId);
        final Entity spotter = spotterOpt.orElse(null);
        final WeaponMounted weapon = weaponOpt.orElse(null);
        final AmmoMounted ammo = ammoOpt.orElse(null);
        final WeaponType weaponType = (weapon != null) ? weapon.getType() : null;
        final AmmoType ammoType = (ammo != null) ? ammo.getType() : null;
        final boolean isIndirect = ((weapon != null) && (weapon.hasModes() && weapon.curMode().isIndirect()));
        final boolean spotting = (spotter != null);

        if (attackingEntity == null) {
            return null;
        }

        Entity targetEntity = null;
        int targetType = target.getTargetType();
        if (targetType == Targetable.TYPE_ENTITY) {
            targetEntity = (Entity) target;
        }

        int aElev = attackingEntity.getElevation();
        int tElev = target.getElevation();
        int targEl;
        if (target instanceof INarcPod) {
            // brush off attached INarcs on oneself; use left arm as a placeholder here as no choice has been made
            return BrushOffAttackAction.toHit(game, attackerId, target, BrushOffAttackAction.LEFT);
        } else if (targetEntity == null) {
            targEl = game.getHexOf(target).floor();
        } else {
            targEl = targetEntity.relHeight();
        }

        int distance = Compute.effectiveDistance(game, attackingEntity, target);

        // EI system
        // 0 if no EI (or switched off)
        // 1 if no intervening light woods
        // 2 if intervening light woods (because target in woods + intervening
        // woods is only +1 total)
        int eiStatus = 0;

        // Possibly set bogus value, since this method doesn't account for weapons but some of its
        // calls do
        int weaponId = (weapon != null) ? attackingEntity.getEquipmentNum(weapon) : WeaponType.WEAPON_NA;

        boolean isAttackerInfantry = attackingEntity instanceof Infantry;
        boolean inSameBuilding = Compute.isInSameBuilding(game, attackingEntity, targetEntity);

        // check LOS; need spotter info to mark some icons correctly
        LosEffects los;
        if (isIndirect) {
            los = LosEffects.calculateLOS(game, spotting ? spotter : attackingEntity, target, spotting);
        } else {
            los = LosEffects.calculateLOS(game, attackingEntity, target);
        }

        if (attackingEntity.hasActiveEiCockpit()) {
            if (los.getLightWoods() > 0) {
                eiStatus = 2;
            } else {
                eiStatus = 1;
            }
        }

        ToHitData losMods = los.losModifiers(game, eiStatus, attackingEntity.isUnderwater());
        ToHitData toHit = new ToHitData(0, Messages.getString("WeaponAttackAction.BaseToHit"));

        // Collect the modifiers for the environment
        toHit = ComputeEnvironmentalToHitMods.compileEnvironmentalToHitMods(game,
              attackingEntity,
              target,
              weaponType,
              ammoType,
              toHit,
              false);

        // Collect the modifiers for the crew/pilot
        toHit = ComputeAttackerToHitMods.compileCrewToHitMods(game, attackingEntity, toHit, weapon);

        // Collect the modifiers for the attacker's condition/actions
        // Conventional fighter, Aerospace and fighter LAM attackers
        if (attackingEntity.isAero()) {
            toHit = ComputeAeroAttackerToHitMods.compileAeroAttackerToHitMods(game,
                  attackingEntity,
                  target,
                  targetType,
                  toHit,
                  Entity.LOC_NONE,
                  AimingMode.NONE,
                  eiStatus,
                  weaponType,
                  weapon,
                  ammoType,
                  EnumSet.of(AmmoType.Munitions.M_STANDARD),
                  false,
                  false,
                  false,
                  false,
                  false);
            // Everyone else
        } else {
            toHit = ComputeAttackerToHitMods.compileAttackerToHitMods(game,
                  attackingEntity,
                  target,
                  los,
                  toHit,
                  Entity.LOC_NONE,
                  AimingMode.NONE,
                  weaponType,
                  weapon,
                  weaponId,
                  ammoType,
                  EnumSet.of(AmmoType.Munitions.M_STANDARD),
                  false,
                  false,
                  false,
                  false,
                  false);
        }

        // Collect the modifiers for the target's condition/actions
        toHit = ComputeTargetToHitMods.compileTargetToHitMods(game,
              attackingEntity,
              target,
              toHit,
              Entity.LOC_NONE,
              AimingMode.NONE,
              distance,
              weaponType,
              weapon,
              ammoType,
              EnumSet.of(AmmoType.Munitions.M_STANDARD),
              false,
              false,
              isAttackerInfantry,
              false,
              isIndirect,
              false,
              false);

        // Collect the modifiers for terrain and line-of-sight. This includes any
        // related to-hit table changes
        toHit = ComputeTerrainMods.compileTerrainAndLosToHitMods(game,
              attackingEntity,
              target,
              targetType,
              aElev,
              tElev,
              targEl,
              distance,
              los,
              toHit,
              losMods,
              eiStatus,
              weaponType,
              weapon,
              weaponId,
              ammoType,
              ammo,
              isAttackerInfantry,
              inSameBuilding,
              isIndirect,
              false,
              false);

        // Add the combined EI terrain reduction as a single modifier (if any was accumulated)
        toHit.finalizeEiModifier();

        // okay!
        return toHit;
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

    public BombLoadout getBombPayload() {
        BombLoadout combined = new BombLoadout(bombPayloads.get("internal"));
        bombPayloads.get("external").forEach((type, count) ->
              combined.merge(type, count, Integer::sum));
        return combined;
    }

    public HashMap<String, BombLoadout> getBombPayloads() {
        return bombPayloads;
    }

    /**
     * @param bombPayloads These are the "bomb payload" for internal and external bomb stores. It's a HashMap of two
     *                     arrays, each indexed by the constants declared in BombType. Each element indicates how many
     *                     types of that bomb should be fired.
     */
    public void setBombPayloads(HashMap<String, BombLoadout> bombPayloads) {
        this.bombPayloads = new HashMap<>();
        for (Map.Entry<String, BombLoadout> entry : bombPayloads.entrySet()) {
            this.bombPayloads.put(entry.getKey(), new BombLoadout(entry.getValue()));
        }
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

    @Deprecated(since = "0.50.07", forRemoval = true) // unused (the field is not accessed directly either)
    public boolean isHomingShot() {
        return isHomingShot;
    }

    public void setHomingShot(boolean isHomingShot) {
        this.isHomingShot = isHomingShot;
    }

    /**
     * Needed by teleoperated missiles
     *
     * @param velocity - an integer representing initial velocity
     */
    public void setLaunchVelocity(int velocity) {
        launchVelocity = velocity;
    }

    // This is a stub. ArtilleryAttackActions actually need to use it
    public void updateTurnsTilHit(Game game) {}

    /**
     * Quick routine to determine if the target should be treated as being in a short building.
     */
    public static boolean targetInShortCoverBuilding(Targetable target) {
        if (target.getTargetType() != Targetable.TYPE_ENTITY) {
            return false;
        }

        Entity targetEntity = (Entity) target;

        Hex targetHex = targetEntity.getGame().getHexOf(target);
        if (targetHex == null) {
            return false;
        }

        // the idea here is that we're in a building that provides partial cover
        // if the unit involved is tall (at least 2 levels, e.g. mek or superheavy
        // vehicle)
        // and its height above the hex ceiling (i.e. building roof) is 1
        // the height determination takes being prone into account
        return targetHex.containsTerrain(Terrains.BUILDING) &&
              (targetEntity.getHeight() > 0) &&
              (targetEntity.relHeight() == 1);
    }

    @Override
    public String toAccessibilityDescription(Client client) {
        if (null == client || null == getTarget(client.getGame())) {
            LOGGER.warn("Unable to construct WAA displayable string due to null reference");
            return "Attacking Null Target with id " + getTargetId() + " using Weapon with id " + weaponId;
        }
        return "attacking " +
              getTarget(client.getGame()).getDisplayName() +
              " with " +
              getEntity(client.getGame()).getEquipment(weaponId).getName();
    }

    @Override
    public String toSummaryString(Game game) {
        ToHitData toHit = toHit(game, true);
        String table = toHit.getTableDesc();
        String buffer = toHit.getValueAsString() + ((!table.isEmpty()) ? ' ' + table : "");
        Entity entity = game.getEntity(getEntityId());
        if (entity != null) {
            String weaponName = entity.getEquipment(getWeaponId()).getType().getName();
            String ammoName = entity.getEquipment(getWeaponId()).getLinked().getType().getName();
            return weaponName + " [" + ammoName + "] " + Messages.getString("BoardView1.needs") + buffer;
        } else {
            return "Invalid attack data";
        }
    }

    public float getExpectedDamage() {
        return expectedDamage;
    }

    public void setExpectedDamage(float expectedDamage) {
        this.expectedDamage = expectedDamage;
    }

    public boolean getAssumedHit() {
        return assumedHit;
    }

    public void setAssumedHit(boolean assumedHit) {
        this.assumedHit = assumedHit;
    }
}
