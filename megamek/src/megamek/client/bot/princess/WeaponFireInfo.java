/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.AmmoType.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.BombMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.capitalweapons.CapitalMissileWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.infantry.InfantryWeaponHandler;
import megamek.logging.MMLogger;

import static megamek.common.AmmoType.*;

/**
 * WeaponFireInfo is a wrapper around a WeaponAttackAction that includes
 * probability to hit and expected damage
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/24/14 2:50 PM
 */
public class WeaponFireInfo {
    private static final MMLogger logger = MMLogger.create(WeaponFireInfo.class);

    private static final NumberFormat LOG_PER = NumberFormat.getPercentInstance();
    private static final NumberFormat LOG_DEC = DecimalFormat.getInstance();

    private WeaponAttackAction action;
    private Entity shooter;
    private Targetable target;
    private WeaponMounted weapon;
    private AmmoMounted preferredAmmo;
    private double probabilityToHit;
    private int heat;
    private double maxDamage;
    private double expectedDamageOnHit;
    private int damageDirection = -1; // direction damage is coming from relative to target
    private ToHitData toHit = null;
    private double expectedCriticals;
    private double killProbability; // probability to destroy CT or HEAD (ignores criticals)
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
     * @param shooter The {@link megamek.common.Entity} doing the attacking.
     * @param target  The {@link megamek.common.Targetable} of the attack.
     * @param weapon  The {@link megamek.common.Mounted} weapon used for the attack.
     * @param ammo    The {@link megamek.common.Mounted} ammo to use for the attack;
     *                may be null.
     * @param game    The current {@link Game}
     * @param guess   Set TRUE to estimate the chance to hit rather than doing the
     *                full calculation.
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
     * @param shooter      The {@link megamek.common.Entity} doing the attacking.
     * @param shooterState The current
     *                     {@link megamek.client.bot.princess.EntityState} of the
     *                     attacker.
     * @param target       The {@link megamek.common.Targetable} of the attack.
     * @param targetState  The current
     *                     {@link megamek.client.bot.princess.EntityState} of the
     *                     target.
     * @param weapon       The {@link megamek.common.Mounted} weapon used for the
     *                     attack.
     * @param game         The current {@link Game}
     * @param guess        Set TRUE to estimate the chance to hit rather than doing
     *                     the full calculation.
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
     * @param shooter               The {@link megamek.common.Entity} doing the
     *                              attacking.
     * @param shooterPath           The {@link megamek.common.MovePath} of the
     *                              attacker.
     * @param target                The {@link megamek.common.Targetable} of the
     *                              attack.
     * @param targetState           The current
     *                              {@link megamek.client.bot.princess.EntityState}
     *                              of the target.
     * @param weapon                The {@link megamek.common.Mounted} weapon used
     *                              for the attack.
     * @param game                  The current {@link Game}
     * @param assumeUnderFlightPath Set TRUE for aerial units performing
     *                              air-to-ground attacks.
     * @param guess                 Set TRUE to estimate the chance to hit rather
     *                              than doing the full calculation.
     * @param owner                 Instance of the princess owner
     * @param bombPayloads          The bomb payload, as described in
     *                              WeaponAttackAction.setBombPayload
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
            final HashMap<String, int[]> bombPayloads) {
        this(shooter, null, shooterPath, target, targetState, weapon, ammo, game, assumeUnderFlightPath, guess, owner,
                bombPayloads);
    }

    /**
     * This constructs a WeaponFireInfo using the best guess of how likely an
     * aerospace unit using a strike attack will
     * hit, without actually constructing the {@link WeaponAttackAction}
     *
     * @param shooter               The {@link megamek.common.Entity} doing the
     *                              attacking.
     * @param shooterState          The current
     *                              {@link megamek.client.bot.princess.EntityState}
     *                              of the attacker.
     * @param shooterPath           The {@link megamek.common.MovePath} of the
     *                              attacker.
     * @param target                The {@link megamek.common.Targetable} of the
     *                              attack.
     * @param targetState           The current
     *                              {@link megamek.client.bot.princess.EntityState}
     *                              of the target.
     * @param weapon                The {@link megamek.common.Mounted} weapon used
     *                              for the attack.
     * @param game                  The current {@link Game}
     * @param assumeUnderFlightPath Set TRUE for aerial units performing
     *                              air-to-ground attacks.
     * @param guess                 Set TRUE to estimate the chance to hit rather
     *                              than going through the full
     *                              calculation.
     * @param owner                 Instance of the princess owner
     * @param bombPayloads          The bomb payload, as described in
     *                              WeaponAttackAction.setBombPayload
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
            final HashMap<String, int[]> bombPayloads) {
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

    double getExpectedDamageOnHit() {
        return expectedDamageOnHit;
    }

    private void setExpectedDamageOnHit(final double expectedDamageOnHit) {
        this.expectedDamageOnHit = expectedDamageOnHit;
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

    private void setMaxDamage(final double maxDamage) {
        this.maxDamage = maxDamage;
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
        return getProbabilityToHit() * getExpectedDamageOnHit();
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

    private WeaponAttackAction buildBombAttackAction(final HashMap<String, int[]> bombPayloads) {
        final WeaponAttackAction diveBomb = new WeaponAttackAction(getShooter().getId(),
                getTarget().getTargetType(),
                getTarget().getId(),
                getShooter().getEquipmentNum(getWeapon()));

        diveBomb.setBombPayloads(bombPayloads);

        return diveBomb;
    }

    double computeExpectedDamage() {
        // bombs require some special consideration
        if (weapon.isGroundBomb()) {
            return computeExpectedBombDamage(getShooter(), weapon, getTarget().getPosition());
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
                return 0D;
            }

            // Handle woods blocking cluster shots
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_WOODS_COVER)) {
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
                        weapon.getType().getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE
                    );
                    blockedByWoods |= weapon.getType().getRackSize() <= woodsLevel
                        || weapon.getType().getDamage() <= woodsLevel;
                    blockedByWoods |= preferredAmmo.getType().getMunitionType().contains(
                        AmmoType.Munitions.M_CLUSTER
                    );

                    if (blockedByWoods) {
                        return 0D;
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
            for (WeaponMounted bayWeapon : weapon.getBayWeapons()) {
                WeaponType weaponType = bayWeapon.getType();
                int maxRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
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
                        case WeaponType.DAMAGE_BY_CLUSTERTABLE:
                        case WeaponType.DAMAGE_ARTILLERY:
                        case WeaponType.DAMAGE_SPECIAL:
                        case WeaponType.DAMAGE_VARIABLE:
                            bayDamage += weaponType.getRackSize();
                            break;
                        default:
                            bayDamage += weaponType.getDamage();
                    }
                }
            }
            return bayDamage;
        }

        // For clan plasma cannon, assume 7 "damage".
        final WeaponType weaponType = (WeaponType) weapon.getType();
        if (weaponType.hasFlag(WeaponType.F_PLASMA) &&
                TechAdvancement.TECH_BASE_CLAN == weaponType.getTechBase()) {
            return 7D;
        }

        // artillery and cluster table use the rack size as the base damage amount,
        // but we'll roll an "average" cluster for the given weapon size to estimate
        // damage.
        if ((weaponType.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE) ||
                (weaponType.getDamage() == WeaponType.DAMAGE_ARTILLERY)) {
            // Assume average cluster size for this weapon, unless it has Streak
            // capabilities
            if (!List.of(AmmoType.T_SRM_STREAK, AmmoType.T_LRM_STREAK, AmmoType.T_IATM)
                    .contains(weaponType.getAmmoType())) {
                int rs = weaponType.getRackSize();
                return Compute.calculateClusterHitTableAmount(7, rs);
            }
        }

        // infantry weapons use number of troopers multiplied by weapon damage,
        // with # troopers counting as 1 for support vehicles
        if ((weaponType.getDamage() == WeaponType.DAMAGE_VARIABLE) &&
                (weaponType instanceof InfantryWeapon)) {
            int numTroopers = (shooter instanceof Infantry) ? ((Infantry) shooter).getShootingStrength() : 1;
            return InfantryWeaponHandler.calculateBaseDamage(shooter, weapon, weaponType) * numTroopers;
        }

        // this is a special case - if we're considering hitting a swarmed target
        // that's basically our only option
        if (weaponType.getInternalName() == Infantry.SWARM_WEAPON_MEK) {
            return 1;
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
            return dmg;
        }

        return weaponType.getDamage();
    }

    /**
     * Aerospace units need to think carefully before firing TAGs at ground targets,
     * because this
     * precludes firing _any_ other weapons this turn.
     *
     * @return expected damage of firing a TAG weapon, in light of other options.
     */
    double computeAeroExpectedTAGDamage() {
        // If TAG damage exceeds the attacking unit's own max damage capacity, go for
        // it!
        return computeExpectedTAGDamage(true);
    }

    /**
     * For some ToHitData results, add additional modifiers that may impact the actual
     * probability of hitting with this weapon.
     * Prime example: Homing artillery, which requires a friendly TAG-equipped unit be able to hit the
     *                target at the appropriate time.
     * @param realToHitData
     * @return
     */
    ToHitData postProcessToHit(ToHitData realToHitData) {
        boolean isHoming = preferredAmmo != null && preferredAmmo.isHomingAmmoInHomingMode();
        if (isHoming) {
            String msg = realToHitData.getCumulativePlainDesc();
            ToHitData thd;
            if (game.getPhase() != GamePhase.FIRING) {
                // Check if any spotters can help us out...
                Entity te = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target : null;
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
        }

        return realToHitData;
    }

    /**
     * Generalized computation of hitting with TAG given current guidable muniitions
     * in play
     *
     * @param exclusiveWithOtherWeapons true if Aero, false otherwise.
     * @return
     */
    double computeExpectedTAGDamage(boolean exclusiveWithOtherWeapons) {
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

        return Math.max(utility, 0);
    }

    /**
     * Compute the heat output by firing a given weapon.
     * Contains special logic for bay weapons when using individual bay heat.
     * TODO: Make some kind of assumption about variable-heat weapons?
     *
     * @param weapon The weapon to check.
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
     * Worker function to compute expected bomb damage given the shooter
     *
     * @param shooter   The unit making the attack.
     * @param weapon    The weapon being used in the attack.
     * @param bombedHex The target hex.
     * @return The expected damage of the attack.
     */
    private double computeExpectedBombDamage(final Entity shooter, final Mounted<?> weapon, final Coords bombedHex) {
        double damage = 0D; // lol double damage I wish

        // for dive attacks, we can pretty much assume that we're going to drop
        // everything we've got on the poor scrubs in this hex
        if (weapon.getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
            for (final BombMounted bomb : shooter.getBombs(BombType.F_GROUND_BOMB)) {
                final int damagePerShot = bomb.getType().getDamagePerShot();

                // some bombs affect a blast radius, so we take that into account
                final List<Coords> affectedHexes = new ArrayList<>();

                int blastRadius = BombType.getBombBlastRadius(bomb.getType().getInternalName());
                for (int radius = 0; radius <= blastRadius; radius++) {
                    affectedHexes.addAll(bombedHex.allAtDistance(radius));
                }

                // now we go through all affected hexes and add up the damage done
                for (final Coords coords : affectedHexes) {
                    for (final Entity currentVictim : game.getEntitiesVector(coords)) {
                        if (currentVictim.getOwner().getTeam() != shooter.getOwner().getTeam()) {
                            damage += damagePerShot;
                        } else { // we prefer not to blow up friendlies if we can help it
                            damage -= damagePerShot;
                        }
                    }
                }
            }
        }

        damage = damage * getProbabilityToHit();

        return damage;
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
            final HashMap<String, int[]> bombPayloads) {

        final StringBuilder msg = new StringBuilder("Initializing Damage for ").append(getShooter().getDisplayName())
                .append(" firing ").append(getWeapon().getDesc())
                .append(" at ").append(getTarget().getDisplayName())
                .append(":");

        // Set up the attack action and calculate the chance to hit.
        if ((null == bombPayloads) || (0 == bombPayloads.get("external").length)) {
            setAction(buildWeaponAttackAction());
        } else {
            setAction(buildBombAttackAction(bombPayloads));
        }

        // Set ammoId here so we can tell toHitCalc which ammo to use for calculations;
        // later overwritten.
        getWeaponAttackAction().setAmmoId(shooter.getEquipmentNum(this.getAmmo()));

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
            setMaxDamage(0);
            setHeat(0);
            setExpectedCriticals(0);
            setKillProbability(0);
            setExpectedDamageOnHit(0);
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
        // Expected damage is the chance of hitting * the max damage
        setExpectedDamageOnHit(getProbabilityToHit() * getMaxDamage());

        msg.append("\n\tMax Damage: ").append(LOG_DEC.format(maxDamage));
        msg.append("\n\tExpected Damage: ").append(LOG_DEC.format(expectedDamageOnHit));

        // If expected damage from Aero tagging is zero, return out - save attacks for
        // later.
        if (weapon.getType().hasFlag(WeaponType.F_TAG) && shooter.isAero() && getExpectedDamageOnHit() <= 0) {
            logger
                    .debug(msg.append("\n\tAerospace TAG attack not advised at this juncture").toString());
            setProbabilityToHit(0);
            setMaxDamage(0);
            setHeat(0);
            setExpectedCriticals(0);
            setKillProbability(0);
            setExpectedDamageOnHit(0);
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
                    (Mek.LOC_CT != hitLocation)) {

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
            if (getExpectedDamageOnHit() > ((targetArmor + targetInternals))) {
                setExpectedCriticals(getExpectedCriticals() + (hitLocationProbability * getProbabilityToHit()));
                if (Mek.LOC_CT == hitLocation) {
                    setKillProbability(getKillProbability() + (hitLocationProbability * getProbabilityToHit()));
                } else if ((Mek.LOC_HEAD == hitLocation) &&
                        (Mek.COCKPIT_TORSO_MOUNTED != targetMek.getCockpitType())) {
                    setKillProbability(getKillProbability() + (hitLocationProbability * getProbabilityToHit()));
                }

                // If the armor can be breached, but the location not destroyed...
            } else if (getExpectedDamageOnHit() > (targetArmor)) {
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
                : ", Ammo: " + ((AmmoType) getAmmo().getType()).getSubMunitionName();
        return getWeapon().getName() + " P. Hit: " + LOG_PER.format(getProbabilityToHit())
                + ", Max Dam: " + LOG_DEC.format(getMaxDamage())
                + ", Exp. Dam: " + LOG_DEC.format(getExpectedDamageOnHit())
                + ", Num Crits: " + LOG_DEC.format(getExpectedCriticals())
                + ", Kill Prob: " + LOG_PER.format(getKillProbability())
                + ammoClause;

    }

    /**
     * The updated firing mode, if any of the weapon involved in this attack.
     * Null if no update required.
     */
    public Integer getUpdatedFiringMode() {
        return updatedFiringMode;
    }

    public void setUpdatedFiringMode(int mode) {
        updatedFiringMode = mode;
    }
}
