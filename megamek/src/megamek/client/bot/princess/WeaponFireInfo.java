/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import static megamek.common.equipment.AmmoType.FLARE_MUNITIONS;
import static megamek.common.equipment.AmmoType.MINE_MUNITIONS;
import static megamek.common.equipment.AmmoType.Munitions.M_INCENDIARY_LRM;
import static megamek.common.equipment.AmmoType.SMOKE_MUNITIONS;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.enums.TechBase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.BombMounted;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.BombType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BuildingTarget;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.common.weapons.capitalWeapons.CapitalMissileWeapon;
import megamek.common.weapons.handlers.AreaEffectHelper;
import megamek.common.weapons.handlers.DamageFalloff;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.infantry.InfantryWeaponHandler;
import megamek.logging.MMLogger;

/**
 * WeaponFireInfo is a wrapper around a WeaponAttackAction that includes probability to hit and expected damage
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/24/14 2:50 PM
 */
public class WeaponFireInfo {
    private static final MMLogger logger = MMLogger.create(WeaponFireInfo.class);

    private static final NumberFormat LOG_PER = NumberFormat.getPercentInstance();
    private static final NumberFormat LOG_DEC = DecimalFormat.getInstance();
    private static final double[] ZERO_DAMAGE = { 0.0, 0.0, 0.0 };

    private WeaponAttackAction action;
    private Entity shooter;
    private Targetable target;
    private WeaponMounted weapon;
    private AmmoMounted preferredAmmo;
    private double probabilityToHit;
    private int heat;
    private double maxDamage;
    private double maxFriendlyDamage = 0;
    private double maxBuildingDamage = 0;
    private double damageOnHit;
    private int damageDirection = -1; // direction damage is coming from relative to target
    private ToHitData toHit = null;
    private double expectedCriticals;
    private double killProbability; // probability to destroy CT or HEAD (ignores criticalSlots)
    private Game game;
    private EntityState shooterState = null;
    private EntityState targetState = null;
    private Integer updatedFiringMode = null;
    private final Princess owner;

    /**
     * For unit testing.
     */
    protected WeaponFireInfo(final Princess owner) {
        this.owner = owner;
    }

    /**
     * Basic constructor.
     *
     * @param shooter The {@link Entity} doing the attacking.
     * @param target  The {@link Targetable} of the attack.
     * @param weapon  The {@link Mounted} weapon used for the attack.
     * @param ammo    The {@link Mounted} ammo to use for the attack; may be null.
     * @param game    The current {@link Game}
     * @param guess   Set TRUE to estimate the chance to hit rather than doing the full calculation.
     */
    WeaponFireInfo(final Entity shooter,
          final Targetable target,
          final WeaponMounted weapon,
          final AmmoMounted ammo,
          final Game game,
          final boolean guess,
          final Princess owner) {
        this(shooter, null, null, target, null, weapon, ammo, game, false, guess, owner, null);
    }

    /**
     * Constructor including the shooter and target's state information.
     *
     * @param shooter      The {@link Entity} doing the attacking.
     * @param shooterState The current {@link megamek.client.bot.princess.EntityState} of the attacker.
     * @param target       The {@link Targetable} of the attack.
     * @param targetState  The current {@link megamek.client.bot.princess.EntityState} of the target.
     * @param weapon       The {@link Mounted} weapon used for the attack.
     * @param game         The current {@link Game}
     * @param guess        Set TRUE to estimate the chance to hit rather than doing the full calculation.
     */
    WeaponFireInfo(final Entity shooter,
          final EntityState shooterState,
          final Targetable target,
          final EntityState targetState,
          final WeaponMounted weapon,
          final AmmoMounted ammo,
          final Game game,
          final boolean guess,
          final Princess owner) {
        this(shooter, shooterState, null, target, targetState, weapon, ammo, game, false, guess, owner, null);
    }

    /**
     * Constructor for aerospace units performing Strike attacks.
     *
     * @param shooter               The {@link Entity} doing the attacking.
     * @param shooterPath           The {@link MovePath} of the attacker.
     * @param target                The {@link Targetable} of the attack.
     * @param targetState           The current {@link megamek.client.bot.princess.EntityState} of the target.
     * @param weapon                The {@link Mounted} weapon used for the attack.
     * @param game                  The current {@link Game}
     * @param assumeUnderFlightPath Set TRUE for aerial units performing air-to-ground attacks.
     * @param guess                 Set TRUE to estimate the chance to hit rather than doing the full calculation.
     * @param owner                 Instance of the princess owner
     * @param bombPayloads          The bomb payload, as described in WeaponAttackAction.setBombPayload
     */
    WeaponFireInfo(final Entity shooter,
          final MovePath shooterPath,
          final Targetable target,
          final EntityState targetState,
          final WeaponMounted weapon,
          final AmmoMounted ammo,
          final Game game,
          final boolean assumeUnderFlightPath,
          final boolean guess,
          final Princess owner,
          final HashMap<String, BombLoadout> bombPayloads) {
        this(shooter, null, shooterPath, target, targetState, weapon, ammo, game, assumeUnderFlightPath, guess, owner,
              bombPayloads);
    }

    /**
     * This constructs a WeaponFireInfo using the best guess of how likely an aerospace unit using a strike attack will
     * hit, without actually constructing the {@link WeaponAttackAction}
     *
     * @param shooter               The {@link Entity} doing the attacking.
     * @param shooterState          The current {@link megamek.client.bot.princess.EntityState} of the attacker.
     * @param shooterPath           The {@link MovePath} of the attacker.
     * @param target                The {@link Targetable} of the attack.
     * @param targetState           The current {@link megamek.client.bot.princess.EntityState} of the target.
     * @param weapon                The {@link Mounted} weapon used for the attack.
     * @param game                  The current {@link Game}
     * @param assumeUnderFlightPath Set TRUE for aerial units performing air-to-ground attacks.
     * @param guess                 Set TRUE to estimate the chance to hit rather than going through the full
     *                              calculation.
     * @param owner                 Instance of the princess owner
     * @param bombPayloads          The bomb payload, as described in WeaponAttackAction.setBombPayload
     */
    private WeaponFireInfo(final Entity shooter,
          final EntityState shooterState,
          final MovePath shooterPath,
          final Targetable target,
          final EntityState targetState,
          final WeaponMounted weapon,
          final AmmoMounted ammo,
          final Game game,
          final boolean assumeUnderFlightPath,
          final boolean guess,
          final Princess owner,
          final HashMap<String, BombLoadout> bombPayloads) {
        this.owner = owner;

        setShooter(shooter);
        setShooterState(shooterState);
        setTarget(target);
        setTargetState(targetState);
        setWeapon(weapon);
        setAmmo(ammo);
        setGame(game);
        initDamage(shooterPath, assumeUnderFlightPath, guess, bombPayloads);
    }

    protected WeaponAttackAction getAction() {
        return action;
    }

    protected void setAction(final WeaponAttackAction action) {
        this.action = action;
    }

    private int getDamageDirection() {
        if (-1 == damageDirection) {
            damageDirection = calcDamageDirection();
        }
        return damageDirection;
    }

    private int calcDamageDirection() {
        return ((calcAttackDirection() - getTargetState().getFacing()) + 6) % 6;
    }

    private int calcAttackDirection() {
        return getTargetState().getPosition().direction(getShooterState().getPosition());
    }

    double getExpectedCriticals() {
        return expectedCriticals;
    }

    private void setExpectedCriticals(final double expectedCriticals) {
        this.expectedCriticals = expectedCriticals;
    }

    double getDamageOnHit() {
        return damageOnHit;
    }

    private void setDamageOnHit(final double damageOnHit) {
        this.damageOnHit = damageOnHit;
    }

    double getKillProbability() {
        return killProbability;
    }

    private void setKillProbability(final double killProbability) {
        this.killProbability = killProbability;
    }

    double getMaxDamage() {
        return maxDamage;
    }

    private void setMaxDamage(final double... damage) {
        this.maxDamage = damage[0];
        this.maxFriendlyDamage = damage[1];
        this.maxBuildingDamage = damage[2];
    }

    double getMaxFriendlyDamage() {
        return maxFriendlyDamage;
    }

    double getMaxBuildingDamage() {
        return maxBuildingDamage;
    }

    double getProbabilityToHit() {
        return probabilityToHit;
    }

    private void setProbabilityToHit(final double probabilityToHit) {
        this.probabilityToHit = probabilityToHit;
    }

    Entity getShooter() {
        return shooter;
    }

    void setShooter(final Entity shooter) {
        this.shooter = shooter;
    }

    public Targetable getTarget() {
        return target;
    }

    protected void setTarget(final Targetable target) {
        this.target = target;
    }

    public ToHitData getToHit() {
        if (null == toHit) {
            setToHit(calcToHit());
        }
        return toHit;
    }

    protected void setToHit(final ToHitData toHit) {
        this.toHit = toHit;
    }

    ToHitData calcToHit() {
        return owner.getFireControl(getShooter()).guessToHitModifierForWeapon(getShooter(), getShooterState(),
              getTarget(),
              getTargetState(),
              getWeapon(), getAmmo(), getGame());
    }

    private ToHitData calcToHit(final MovePath shooterPath,
          final boolean assumeUnderFlightPath) {
        return owner.getFireControl(getShooter()).guessAirToGroundStrikeToHitModifier(getShooter(), null, getTarget(),
              getTargetState(),
              shooterPath, getWeapon(),
              getAmmo(), getGame(),
              assumeUnderFlightPath);
    }

    private ToHitData calcRealToHit(final WeaponAttackAction weaponAttackAction) {
        return weaponAttackAction.toHit(getGame(),
              owner.getPrecognition().getECMInfo());
    }

    public Game getGame() {
        return game;
    }

    protected void setGame(final Game game) {
        this.game = game;
    }

    private EntityState getShooterState() {
        if (null == shooterState) {
            shooterState = new EntityState(getShooter());
        }
        return shooterState;
    }

    void setShooterState(final EntityState shooterState) {
        this.shooterState = shooterState;
    }

    private EntityState getTargetState() {
        if (null == targetState) {
            targetState = new EntityState(target);
        }
        return targetState;
    }

    void setTargetState(final EntityState targetState) {
        this.targetState = targetState;
    }

    protected void setWeapon(final WeaponMounted weapon) {
        this.weapon = weapon;
    }

    protected void setAmmo(final AmmoMounted ammo) {
        this.preferredAmmo = ammo;
    }

    protected void setHeat(final int heat) {
        this.heat = heat;
    }

    public int getHeat() {
        return heat;
    }

    public WeaponMounted getWeapon() {
        return weapon;
    }

    public AmmoMounted getAmmo() {
        return preferredAmmo;
    }

    public double getExpectedDamage() {
        return getProbabilityToHit() * getDamageOnHit();
    }

    WeaponAttackAction buildWeaponAttackAction() {
        if (!(getWeapon().getType().hasFlag(WeaponType.F_ARTILLERY)
              || (getWeapon().getType() instanceof CapitalMissileWeapon
              && Compute.isGroundToGround(shooter, target)))) {
            return new WeaponAttackAction(getShooter().getId(), getTarget().getTargetType(), getTarget().getId(),
                  getShooter().getEquipmentNum(getWeapon()));
        } else {
            return new ArtilleryAttackAction(getShooter().getId(), getTarget().getTargetType(), getTarget().getId(),
                  getShooter().getEquipmentNum(getWeapon()), getGame());
        }
    }

    private WeaponAttackAction buildBombAttackAction(final HashMap<String, BombLoadout> bombPayloads) {
        final WeaponAttackAction diveBomb = new WeaponAttackAction(getShooter().getId(),
              getTarget().getTargetType(),
              getTarget().getId(),
              getShooter().getEquipmentNum(getWeapon()));

        diveBomb.setBombPayloads(bombPayloads);

        return diveBomb;
    }

    double[] computeExpectedDamage() {
        // bombs require some special consideration
        if (weapon.isGroundBomb()) {
            // We can only bomb hex targets, officially
            return computeExpectedBombDamage(getShooter(), weapon, getTarget());
        }

        // XXX: update this and other utility munition handling with smarter deployment, a la TAG above
        if (preferredAmmo != null) {
            var munitionType = preferredAmmo.getType().getMunitionType();
            // Handle all 0-damage munitions here
            if (
                  SMOKE_MUNITIONS.containsAll(munitionType) ||
                        FLARE_MUNITIONS.containsAll(munitionType) ||
                        MINE_MUNITIONS.containsAll(munitionType)
            ) {
                return ZERO_DAMAGE;
            }

            // Handle woods blocking cluster shots
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_WOODS_COVER)) {
                // SRMs, LB-X, Flak AC, AC-2 derivatives, MGs, smaller LRMs,
                // and Silver Bullet Gauss are among the weapons
                // that lose all effectiveness if this rule is
                // on and the target is in woods/jungle.
                if (
                      game.getBoard().contains(target.getPosition())
                            && game.getBoard().getHex(target.getPosition()).containsAnyTerrainOf(
                            Terrains.WOODS, Terrains.JUNGLE
                      )
                ) {
                    int woodsLevel = 2 * Math.max(
                          game.getBoard().getHex(target.getPosition()).terrainLevel(Terrains.WOODS),
                          game.getBoard().getHex(target.getPosition()).terrainLevel(Terrains.JUNGLE)
                    );
                    boolean blockedByWoods = (
                          weapon.getType().getDamage() == WeaponType.DAMAGE_BY_CLUSTER_TABLE
                    );
                    blockedByWoods |= weapon.getType().getRackSize() <= woodsLevel
                          || weapon.getType().getDamage() <= woodsLevel;
                    blockedByWoods |= preferredAmmo.getType().getMunitionType().contains(
                          AmmoType.Munitions.M_CLUSTER
                    );

                    if (blockedByWoods) {
                        return ZERO_DAMAGE;
                    }
                }
            }
        }

        // bay weapons require special consideration, by looping through all weapons and
        // adding up the damage
        // A bay's weapons may have different ranges, most noticeable in laser bays,
        // where the damage potential
        // varies with distance to target.
        if (!weapon.getBayWeapons().isEmpty()) {
            int bayDamage = 0;
            int bayFriendly = 0;
            int bayBuilding = 0;
            for (WeaponMounted bayWeapon : weapon.getBayWeapons()) {
                WeaponType weaponType = bayWeapon.getType();
                int maxRange = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
                      ? weaponType.getExtremeRange()
                      : weaponType.getLongRange();
                int targetDistance = getShooter().getPosition().distance(getTarget().getPosition());
                if (shooter.isAirborne() && target.isAirborne()) {
                    targetDistance /= 16;
                }

                // if the particular weapon is within range or we're an aircraft strafing a
                // ground unit
                // then we can count it. Otherwise, it's not going to contribute to damage, and
                // we want
                // to avoid grossly overestimating damage.
                if (targetDistance <= maxRange || shooter.isAirborne() && !target.isAirborne()) {
                    switch (weaponType.getDamage()) {
                        case WeaponType.DAMAGE_ARTILLERY:
                            // Rough estimate of collateral damage
                            bayFriendly += weaponType.getRackSize() * 4;
                            bayBuilding += weaponType.getRackSize() * 8;
                        case WeaponType.DAMAGE_BY_CLUSTER_TABLE:
                        case WeaponType.DAMAGE_SPECIAL:
                        case WeaponType.DAMAGE_VARIABLE:
                            bayDamage += weaponType.getRackSize();
                            break;
                        default:
                            bayDamage += weaponType.getDamage();
                    }
                }
            }
            return new double[] { bayDamage, bayFriendly, bayBuilding };
        }

        // For clan plasma cannon, assume 7 "damage".
        final WeaponType weaponType = weapon.getType();
        if (weaponType.hasFlag(WeaponType.F_PLASMA) &&
              TechBase.CLAN == weaponType.getTechBase()) {
            return new double[] { 7D, 0D, 0D };
        }

        // artillery and cluster table use the rack size as the base damage amount,
        // but we'll roll an "average" cluster for the given weapon size to estimate
        // damage.
        if ((weaponType.getDamage() == WeaponType.DAMAGE_BY_CLUSTER_TABLE) ||
              (weaponType.getDamage() == WeaponType.DAMAGE_ARTILLERY)) {
            // Assume average cluster size for this weapon, unless it has Streak
            // capabilities
            if (!List.of(AmmoType.AmmoTypeEnum.SRM_STREAK, AmmoType.AmmoTypeEnum.LRM_STREAK, AmmoType.AmmoTypeEnum.IATM)
                  .contains(weaponType.getAmmoType())) {
                boolean artillery = (weaponType.getDamage() == WeaponType.DAMAGE_ARTILLERY);
                int rs = weaponType.getRackSize();
                int damage = Compute.calculateClusterHitTableAmount(7, rs);

                // Account for Incendiary-modded munitions when firing on infantry
                if ((target != null && target.isInfantry()) &&
                      (preferredAmmo != null && preferredAmmo.getType() != null)) {
                    AmmoType ammoType = preferredAmmo.getType();
                    if (ammoType.getMunitionType().contains(M_INCENDIARY_LRM)) {
                        damage += Math.ceil(weaponType.getRackSize() / 5.0);
                    }
                }

                return new double[] {
                      damage,
                      (artillery) ? damage * 4 : 0D,
                      (artillery) ? damage * 8 : 0D
                };
            }
        }

        // infantry weapons use number of troopers multiplied by weapon damage,
        // with # troopers counting as 1 for support vehicles
        if ((weaponType.getDamage() == WeaponType.DAMAGE_VARIABLE) &&
              (weaponType instanceof InfantryWeapon)) {
            int numTroopers = (shooter instanceof Infantry) ? ((Infantry) shooter).getShootingStrength() : 1;
            return new double[] { InfantryWeaponHandler.calculateBaseDamage(shooter, weapon, weaponType) * numTroopers,
                                  0D, 0D };
        }

        // this is a special case - if we're considering hitting a swarmed target
        // that's basically our only option
        if (Objects.equals(weaponType.getInternalName(), Infantry.SWARM_WEAPON_MEK)) {
            return new double[] { 1, 0D, 0D };
        }

        // Give an estimation of the utility of TAGging a given target.
        if (weaponType.hasFlag(WeaponType.F_TAG)) {
            // Aero TAG usage needs to take into account incoming Indirect Fire shots from
            // friendlies, homing
            // weapons, and the disutility of foregoing its own other weapons.
            if (weapon.getEntity().isAero() && !target.isAero()) {
                return computeAeroExpectedTAGDamage();
            } else {
                // Other taggers just need to know what hitting with the TAG can expect to deal,
                // damage-wise.
                return computeExpectedTAGDamage(false);
            }

        }

        if (getTarget() instanceof Entity) {
            double dmg = Compute.getExpectedDamage(getGame(), getAction(),
                  true, owner.getPrecognition().getECMInfo());
            if (weaponType.hasFlag(WeaponType.F_PLASMA)) {
                dmg += 3; // Account for potential plasma heat.
            }
            return new double[] { dmg, 0D, 0D };
        }

        return new double[] { weaponType.getDamage(), 0D, 0D };
    }

    /**
     * Aerospace units need to think carefully before firing TAGs at ground targets, because this precludes firing _any_
     * other weapons this turn.
     *
     * @return expected damage of firing a TAG weapon, in light of other options.
     */
    double[] computeAeroExpectedTAGDamage() {
        // If TAG damage exceeds the attacking unit's own max damage capacity, go for
        // it!
        return computeExpectedTAGDamage(true);
    }

    /**
     * For some ToHitData results, add additional modifiers that may impact the actual probability of hitting with this
     * weapon. Prime example: Homing artillery, which requires a friendly TAG-equipped unit be able to hit the target at
     * the appropriate time.
     *
     */
    ToHitData postProcessToHit(ToHitData realToHitData) {
        // If the shot already can't hit, no more processing is required.
        if (realToHitData.cannotSucceed()) {
            return realToHitData;
        }

        // Check if this is a homing shot of some kind first.
        boolean isHoming = preferredAmmo != null && preferredAmmo.isHomingAmmoInHomingMode();
        if (isHoming) {
            String msg = realToHitData.getCumulativePlainDesc();
            ToHitData thd;
            if (game.getPhase() != GamePhase.FIRING) {
                Entity te = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target : null;

                // Can't hit flying Aerospace with Homing
                if (te != null && te.isAirborne()) {
                    return new ToHitData(ToHitData.AUTOMATIC_FAIL, "Aerospace cannot be TAGged, auto-miss");
                }

                // Check if any spotters can help us out...
                Entity spotter = Compute.findTAGSpotter(game, shooter, target, false);
                if (spotter != null) {
                    // Chance of getting a TAG spot is, at base, the spotter's gunnery skill
                    thd = new ToHitData(spotter.getCrew().getGunnery(), msg);
                    // Likelihood of hitting goes up as speed goes down...
                    if (null != te) {
                        thd.append(
                              Compute.getTargetMovementModifier(
                                    te.getRunMP(),
                                    false,
                                    false,
                                    game));
                    }
                    // Replace the 4+ THD with an approximation of our TAG chances.
                    return thd;
                } else {
                    // No chance to TAG means no chance to hit.
                    return new ToHitData(ToHitData.AUTOMATIC_FAIL, msg);
                }
            } else {
                // Firing direct Homing shot
                if (Compute.isTargetTagged(target, game)) {
                    // If the target is already TAGged, it's just the original check.
                    return realToHitData;
                } else {
                    // No more chances to TAG if we're in the Firing phase, so no chance
                    // for Homing shot to hit either.
                    return new ToHitData(ToHitData.AUTOMATIC_FAIL, msg);
                }
            }
        } else if (weapon.isGroundBomb() && !(preferredAmmo == null)
              // If bombing with actual bombs, make sure the target isn't flying too high to catch in the blast!
              && !(weapon.getType().hasFlag(WeaponType.F_TAG) || weapon.getType().hasFlag(WeaponType.F_MISSILE))
        ) {
            // See if we can catch the target in the blast
            Hex hex = game.getBoard().getHex(target.getPosition());
            int targetLevel = (target.getTargetType() == Targetable.TYPE_ENTITY)
                  ? target.getElevation()
                  : ((HexTarget) target).getTargetLevel();

            if (hex != null && hex.containsAnyTerrainOf(Terrains.WATER, Terrains.BLDG_ELEV)) {
                if ((hex.ceiling() + 2) < (hex.getLevel() + targetLevel)) {
                    return new ToHitData(FireControl.TH_NO_TARGETS_IN_BLAST);
                }
            }
        }

        return realToHitData;
    }

    /**
     * Generalized computation of hitting with TAG given current guidable munitions in play
     *
     * @param exclusiveWithOtherWeapons true if Aero, false otherwise.
     *
     */
    double[] computeExpectedTAGDamage(boolean exclusiveWithOtherWeapons) {
        final StringBuilder msg = new StringBuilder("Assessing the expected max damage from ")
              .append(shooter.getDisplayName())
              .append(" using their TAG this turn");

        int myWeaponsDamage = 0;
        if (exclusiveWithOtherWeapons) {
            // We need to know what we're giving up if we fire this TAG...
            myWeaponsDamage = Compute.computeTotalDamage(shooter.getTotalWeaponList());
            msg.append("\nThe unit will be giving up ")
                  .append(myWeaponsDamage)
                  .append(" damage from other weapons");
        }

        int incomingAttacksDamage = owner.computeTeamTagUtility(
              target,
              Compute.computeTotalDamage(owner.computeGuidedWeapons(shooter, target.getPosition())));
        int utility = incomingAttacksDamage - myWeaponsDamage;
        msg.append("\n\tUtility: ").append(utility).append(" damage (Max, estimated)");
        logger.debug(msg.toString());

        return new double[] { Math.max(utility, 0), 0D, 0D };
    }

    /**
     * Compute the heat output by firing a given weapon. Contains special logic for bay weapons when using individual
     * bay heat.
     * TODO: Make some kind of assumption about variable-heat weapons?
     *
     * @param weapon The weapon to check.
     *
     * @return Generated heat.
     */
    int computeHeat(WeaponMounted weapon) {
        // bay weapons require special consideration, by looping through all weapons and
        // adding up the damage
        // A bay's weapons may have different ranges, most noticeable in laser bays,
        // where the damage potential
        // varies with distance to target.
        if (!weapon.getBayWeapons().isEmpty()) {
            int bayHeat = 0;
            for (WeaponMounted bayWeapon : weapon.getBayWeapons()) {
                bayHeat += bayWeapon.getType().getHeat();
            }

            return bayHeat;
        } else {
            return weapon.getType().getHeat();
        }
    }

    /**
     * Worker function to compute bomb damage given the shooter and target hex.
     *
     * @param shooter   The unit making the attack.
     * @param weapon    The weapon being used in the attack.
     * @param targetHex The target hex.
     *
     * @return The array of expected damages of the attack.
     */
    protected double[] computeExpectedBombDamage(final Entity shooter, final Mounted<?> weapon,
          final Targetable targetHex) {
        double damage = 0D, friendlyDamage = 0D, buildingDamage = 0D;
        Coords bombedCoords = targetHex.getPosition();
        int targetLevel = 0;
        if (List.of(Targetable.TYPE_HEX_AERO_BOMB, Targetable.TYPE_HEX_BOMB).contains(targetHex.getTargetType())) {
            targetLevel = ((HexTarget) targetHex).getTargetLevel();
        }

        // for dive attacks, we can pretty much assume that we're going to drop
        // everything we've got on the poor scrubs in this hex
        if (weapon.getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
            // TODO: cache damage values and re-use for each bomb of the same type
            for (final BombMounted bomb : shooter.getBombs(BombType.F_GROUND_BOMB)) {

                DamageFalloff falloff = AreaEffectHelper.calculateDamageFallOff(bomb.getType(), 0, false);
                HashMap<Map.Entry<Integer, Coords>, Integer> blastShape = AreaEffectHelper.shapeBlast(
                      bomb.getType(), bombedCoords, falloff, targetLevel, false, false, false,
                      game, false
                );

                // now we go through all affected hexes and add up the damage done
                // Entries for blast shape are: (absolute level: Coords) : damage
                // e.g. the center of an HE bomb to level 4 of a building on a level 2 hex at (1,3) is:
                //   `(6: (1, 3)): 10`
                List<Entity> hitVictims = new ArrayList<>();
                for (Map.Entry<Integer, Coords> entry : blastShape.keySet()) {
                    Coords coords = entry.getValue();
                    Hex hex = game.getBoard().getHex(coords);
                    // Currently, bombs off board are not supported.
                    if (hex == null) {
                        continue;
                    }
                    // Record collateral damage to buildings
                    if (hex.getTerrain(Terrains.BLDG_ELEV) != null && hex.ceiling() >= entry.getKey()) {
                        // Only approximate building damage here.
                        // Blast damage to buildings is cumulative.
                        buildingDamage += blastShape.get(entry) * 2;
                    }
                    // Record damage to enemy and friendly units
                    // Entities can only be hit once per hex they occupy; blastShape assures we record
                    // highest damage first.
                    for (final Entity currentVictim : game.getEntitiesVector(coords)) {
                        if (currentVictim.isAirborne() || hitVictims.contains(currentVictim)) {
                            continue;
                        }
                        int floor = currentVictim.getElevation() + hex.getLevel();
                        int ceil = currentVictim.relHeight() + hex.getLevel();
                        if (!(floor <= entry.getKey() && entry.getKey() <= ceil)) {
                            continue;
                        }
                        hitVictims.add(currentVictim);
                        // Infantry take at least double damage from AE, if not 4x or 8x.
                        if (currentVictim.getOwner().getTeam() != shooter.getOwner().getTeam()) {
                            damage += (currentVictim.isInfantry() ? 2 : 1) * blastShape.get(entry);
                        } else { // we prefer not to blow up friendlies if we can help it
                            friendlyDamage += (currentVictim.isInfantry() ? 2 : 1) * blastShape.get(entry);
                        }
                    }
                }
            }
        }

        return new double[] { damage, friendlyDamage, buildingDamage };
    }

    /*
     * Helper function that calculates expected damage
     *
     * @param shooterPath The path the attacker has moved.
     *
     * @param assumeUnderFlightPath If TRUE, aero units will not check to make sure
     * the target is under their flight
     * path.
     *
     * @param guess Set TRUE to estimate the chance to hit rather than doing the
     * full calculation.
     */
    void initDamage(@Nullable final MovePath shooterPath,
          final boolean assumeUnderFlightPath,
          final boolean guess,
          final HashMap<String, BombLoadout> bombPayloads) {

        final StringBuilder msg = new StringBuilder("Initializing Damage for ").append(getShooter().getDisplayName())
              .append(" firing ").append(getWeapon().getDesc())
              .append(" at ").append(getTarget().getDisplayName())
              .append(":");

        // Set up the attack action and calculate the chance to hit.
        if ((null == bombPayloads) || (0 == bombPayloads.get("external").getTotalBombs())) {
            setAction(buildWeaponAttackAction());
        } else {
            setAction(buildBombAttackAction(bombPayloads));
        }

        // Set ammoId here so we can tell toHitCalc which ammo to use for calculations;
        // later overwritten.
        getWeaponAttackAction().setAmmoId(shooter.getEquipmentNum(this.getAmmo()));

        // As calcRealToHit engages the actual to-hit calculation code, we need to do
        // bot-related postprocessing on its results rather than inside of the WAA code.
        if (!guess) {
            setToHit(postProcessToHit(calcRealToHit(getWeaponAttackAction())));
        } else if (null != shooterPath) {
            setToHit(calcToHit(shooterPath, assumeUnderFlightPath));
        } else {
            setToHit(calcToHit());
        }
        // If we can't hit, set everything zero and return...
        if (12 < getToHit().getValue()) {
            logger.debug(
                  msg.append("\n\tImpossible toHit: ").append(getToHit().getValue())
                        .append(" (").append(getToHit().getCumulativePlainDesc()).append(")")
                        .append((guess) ? " [guess]" : " [real]"));
            setProbabilityToHit(0);
            setMaxDamage(ZERO_DAMAGE);
            setHeat(0);
            setExpectedCriticals(0);
            setKillProbability(0);
            setDamageOnHit(0);
            return;
        }

        if (getShooterState().hasNaturalAptGun()) {
            msg.append("\n\tAttacker has Natural Aptitude Gunnery");
        }

        setProbabilityToHit(Compute.oddsAbove(getToHit().getValue(), getShooterState().hasNaturalAptGun()) / 100);

        msg.append("\n\tHit Chance: ").append(LOG_PER.format(getProbabilityToHit()));

        // now that we've calculated hit odds, if we're shooting
        // a weapon capable of rapid fire, it's time to decide whether we're going to
        // spin it up
        String currentFireMode = getWeapon().curMode().getName();
        int spinMode = Compute.spinUpCannon(getGame(), getAction(), owner.getSpinUpThreshold());
        if (!currentFireMode.equals(getWeapon().curMode().getName())) {
            setUpdatedFiringMode(spinMode);
        }

        setHeat(computeHeat(weapon));

        msg.append("\n\tHeat: ").append(getHeat());

        setMaxDamage(computeExpectedDamage());
        setDamageOnHit(getMaxDamage());

        msg.append("\n\tMax Damage: ").append(LOG_DEC.format(maxDamage));
        msg.append("\n\tExpected Damage: ").append(LOG_DEC.format(damageOnHit));

        // If expected damage from Aero tagging is zero, return out - save attacks for
        // later.
        if (weapon.getType().hasFlag(WeaponType.F_TAG) && shooter.isAero() && getDamageOnHit() <= 0) {
            logger
                  .debug(msg.append("\n\tAerospace TAG attack not advised at this juncture").toString());
            setProbabilityToHit(0);
            setMaxDamage(ZERO_DAMAGE);
            setHeat(0);
            setExpectedCriticals(0);
            setKillProbability(0);
            setDamageOnHit(0);
            return;
        }

        final double expectedCriticalHitCount = ProbabilityCalculator.getExpectedCriticalHitCount();

        // there's always the chance of rolling a '2'
        final double ROLL_TWO = 0.028;
        setExpectedCriticals(ROLL_TWO * expectedCriticalHitCount * getProbabilityToHit());

        // now guess how many critical hits will be done
        setKillProbability(0);
        Mek targetMek = null;
        Targetable potentialTarget = getTarget();

        if (potentialTarget instanceof Mek potentialTargetMek) {
            targetMek = potentialTargetMek;
        } else if (potentialTarget instanceof HexTarget || potentialTarget instanceof BuildingTarget) {
            Coords c = potentialTarget.getPosition();
            Iterator<Entity> targetEnemies = game.getEnemyEntities(c, this.shooter);
            while (targetEnemies.hasNext()) {
                Entity next = targetEnemies.next();
                if (next instanceof Mek potentialTargetMek) {
                    targetMek = potentialTargetMek;
                    break;
                }
            }
        }
        // No target Mek found; nothing to do
        if (null == targetMek) {
            logger.debug(msg.toString());
            return;
        }

        // A Mek with a torso-mounted cockpit can survive losing its head.
        double headlessOdds = 0.0;

        // Loop through hit locations.
        // todo Targeting tripods.
        for (int i = 0; 7 >= i; i++) {
            int hitLocation = i;

            while (targetMek.isLocationBad(hitLocation) &&
                  (Mek.LOC_CENTER_TORSO != hitLocation)) {

                // Head shots don't travel inward if the head is removed. Instead, a new roll
                // gets made.
                if (Mek.LOC_HEAD == hitLocation) {
                    headlessOdds = ProbabilityCalculator.getHitProbability(getDamageDirection(), Mek.LOC_HEAD);
                    break;
                }

                // Get the next most inward location.
                hitLocation = Mek.getInnerLocation(hitLocation);
            }
            double hitLocationProbability = ProbabilityCalculator.getHitProbability(getDamageDirection(), hitLocation);

            // Account for the possibility of re-rolling a head hit on a headless mek.
            hitLocationProbability += (hitLocationProbability * headlessOdds);

            // Get the armor and internals for this location.
            final int targetArmor = Math.max(0, targetMek.getArmor(hitLocation, (3 == getDamageDirection())));
            final int targetInternals = Math.max(0, targetMek.getInternal(hitLocation));

            // If the location could be destroyed outright...
            if (getExpectedDamage() > ((targetArmor + targetInternals))) {
                setExpectedCriticals(getExpectedCriticals() + (hitLocationProbability * getProbabilityToHit()));
                if (Mek.LOC_CENTER_TORSO == hitLocation) {
                    setKillProbability(getKillProbability() + (hitLocationProbability * getProbabilityToHit()));
                } else if ((Mek.LOC_HEAD == hitLocation) &&
                      (Mek.COCKPIT_TORSO_MOUNTED != targetMek.getCockpitType())) {
                    setKillProbability(getKillProbability() + (hitLocationProbability * getProbabilityToHit()));
                }

                // If the armor can be breached, but the location not destroyed...
            } else if (getExpectedDamage() > (targetArmor)) {
                setExpectedCriticals(getExpectedCriticals() +
                      (hitLocationProbability * getProbabilityToHit() *
                            expectedCriticalHitCount));
            }
        }

        logger.debug(msg.toString());
    }

    WeaponAttackAction getWeaponAttackAction() {
        if (null != getAction()) {
            return getAction();
        }
        if (!(getWeapon().getType().hasFlag(WeaponType.F_ARTILLERY)
              || (getWeapon().getType() instanceof CapitalMissileWeapon
              && Compute.isGroundToGround(shooter, target)))) {
            setAction(new WeaponAttackAction(getShooter().getId(), getTarget().getId(),
                  getShooter().getEquipmentNum(getWeapon())));
        } else {
            setAction(new ArtilleryAttackAction(getShooter().getId(), getTarget().getTargetType(),
                  getTarget().getId(), getShooter().getEquipmentNum(getWeapon()),
                  getGame()));
        }
        if (getAction() == null) {
            setProbabilityToHit(0);
            return null;
        }
        // Set the ammoId for calcs.
        getAction().setAmmoId(shooter.getEquipmentNum(this.getAmmo()));
        setProbabilityToHit(Compute.oddsAbove(getAction().toHit(getGame()).getValue(),
              getShooterState().hasNaturalAptGun()) / 100.0);
        return getAction();
    }

    String getDebugDescription() {
        String ammoClause = (getAmmo() == null) ? ""
              : ", Ammo: " + getAmmo().getType().getSubMunitionName();
        return getWeapon().getName() + " P. Hit: " + LOG_PER.format(getProbabilityToHit())
              + ", Max Dam: " + LOG_DEC.format(getMaxDamage())
              + ", Exp. Dam: " + LOG_DEC.format(getDamageOnHit())
              + ", Num Crits: " + LOG_DEC.format(getExpectedCriticals())
              + ", Kill Prob: " + LOG_PER.format(getKillProbability())
              + ammoClause;

    }

    /**
     * The updated firing mode, if any of the weapon involved in this attack. Null if no update required.
     */
    public Integer getUpdatedFiringMode() {
        return updatedFiringMode;
    }

    public void setUpdatedFiringMode(int mode) {
        updatedFiringMode = mode;
    }
}
