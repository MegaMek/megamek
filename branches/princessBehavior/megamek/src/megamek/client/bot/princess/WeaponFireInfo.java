/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.util.LogLevel;
import megamek.common.util.Logger;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * WeaponFireInfo is a wrapper around a WeaponAttackAction that includes
 * probability to hit and expected damage
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/14/13 12:46 AM
 */
public class WeaponFireInfo {
    private static final NumberFormat LOG_PER = NumberFormat.getPercentInstance();
    private static final NumberFormat LOG_DEC = DecimalFormat.getInstance();

    private WeaponAttackAction action;
    private Entity shooter;
    private Targetable target;
    private Mounted weapon;
    private double probabilityToHit;
    private int heat;
    private double maxDamage;
    private double expectedDamageOnHit;
    private int damageDirection = -1; // direction damage is coming from relative to target
    private ToHitData toHit = null;
    private double expectedCriticals;
    private double killProbability; // probability to destroy CT or HEAD (ignores criticals)
    private IGame game;
    private EntityState shooterState = null;
    private EntityState targetState = null;

    /**
     * For unit testing.
     */
    protected WeaponFireInfo() {}

    /**
     * This constructs a WeaponFireInfo using an actual {@link WeaponAttackAction} with real to hit values
     *
     * @param shooter The {@link Entity} doing the attacking.
     * @param target The {@link Targetable} of the attack.
     * @param weapon The {@link Mounted} weapon used for the attack.
     * @param game The {@link IGame} in progress.
     */
    WeaponFireInfo(Entity shooter, Targetable target, Mounted weapon, IGame game) {
        this(shooter, null, null, target, null, weapon, game, false);
    }

    /**
     * This constructs a WeaponFireInfo using the best guess of how likely  this is to hit without actually
     * constructing the {@link WeaponAttackAction}
     *
     * @param shooter The {@link Entity} doing the attacking.
     * @param shooterState The current {@link EntityState} of the attacker.
     * @param target The {@link Targetable} of the attack.
     * @param targetState The current {@link EntityState} of the target.
     * @param weapon The {@link Mounted} weapon used for the attack.
     * @param game The {@link IGame} in progress.
     */
    WeaponFireInfo(Entity shooter, EntityState shooterState, Targetable target,
                   EntityState targetState, Mounted weapon, IGame game) {
        this(shooter, shooterState, null, target, targetState, weapon, game, false);
    }

    /**
     * This constructs a WeaponFireInfo using the best guess of how likely an aerospace unit using a strike attack will
     * hit, without actually constructing the {@link WeaponAttackAction}
     *
     * @param shooter The {@link Entity} doing the attacking.
     * @param shooterPath The {@link MovePath} of the attacker.
     * @param target The {@link Targetable} of the attack.
     * @param targetState The current {@link EntityState} of the target.
     * @param weapon The {@link Mounted} weapon used for the attack.
     * @param game The {@link IGame} in progress.
     * @param assumeUnderFlightPath Set TRUE for aerial units performing air-to-ground attacks.
     */
    WeaponFireInfo(Entity shooter, MovePath shooterPath, Targetable target, EntityState targetState,
                   Mounted weapon, IGame game, boolean assumeUnderFlightPath) {
        this(shooter, null, shooterPath, target, targetState, weapon, game, assumeUnderFlightPath);
    }

    /**
     * This constructs a WeaponFireInfo using the best guess of how likely an aerospace unit using a strike attack will
     * hit, without actually constructing the {@link WeaponAttackAction}
     *
     * @param shooter The {@link Entity} doing the attacking.
     * @param shooterState The current {@link EntityState} of the attacker.
     * @param shooterPath The {@link MovePath} of the attacker.
     * @param target The {@link Targetable} of the attack.
     * @param targetState The current {@link EntityState} of the target.
     * @param weapon The {@link Mounted} weapon used for the attack.
     * @param game The {@link IGame} in progress.
     * @param assumeUnderFlightPath Set TRUE for aerial units performing air-to-ground attacks.
     */
    WeaponFireInfo (Entity shooter, EntityState shooterState, MovePath shooterPath, Targetable target,
                    EntityState targetState, Mounted weapon, IGame game, boolean assumeUnderFlightPath) {
        final String METHOD_NAME =
                "WeaponFireInfo(Entity, EntityState, MovePath, Targetable, EntityState, Mounted, IGame, boolean)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            setShooter(shooter);
            setShooterState(shooterState);
            setTarget(target);
            setTargetState(targetState);
            setWeapon(weapon);
            setGame(game);
            initDamage(shooterPath, assumeUnderFlightPath);
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    protected double calcExpectedDamage() {
        return probabilityToHit * expectedDamageOnHit;
    }

    protected WeaponAttackAction getAction() {
        return action;
    }

    protected void setAction(WeaponAttackAction action) {
        this.action = action;
    }

    public int getDamageDirection() {
        if (damageDirection == -1) {
            damageDirection = calcDamageDirection();
        }
        return damageDirection;
    }

    public int calcDamageDirection() {
        return ((calcAttackDirection() - getTargetState().getFacing()) + 6) % 6;
    }

    protected void setDamageDirection(int damageDirection) {
        this.damageDirection = damageDirection;
    }

    private int calcAttackDirection() {
        return getTargetState().getPosition().direction(getShooterState().getPosition());
    }

    public double getExpectedCriticals() {
        return expectedCriticals;
    }

    protected void setExpectedCriticals(double expectedCriticals) {
        this.expectedCriticals = expectedCriticals;
    }

    public double getExpectedDamageOnHit() {
        return expectedDamageOnHit;
    }

    protected void setExpectedDamageOnHit(double expectedDamageOnHit) {
        this.expectedDamageOnHit = expectedDamageOnHit;
    }

    public double getKillProbability() {
        return killProbability;
    }

    protected void setKillProbability(double killProbability) {
        this.killProbability = killProbability;
    }

    public double getMaxDamage() {
        return maxDamage;
    }

    protected void setMaxDamage(double maxDamage) {
        this.maxDamage = maxDamage;
    }

    public double getProbabilityToHit() {
        return probabilityToHit;
    }

    protected void setProbabilityToHit(double probabilityToHit) {
        this.probabilityToHit = probabilityToHit;
    }

    public Entity getShooter() {
        return shooter;
    }

    protected void setShooter(Entity shooter) {
        this.shooter = shooter;
    }

    public Targetable getTarget() {
        return target;
    }

    protected void setTarget(Targetable target) {
        this.target = target;
    }

    public ToHitData getToHit() {
        if (toHit == null) {
            setToHit(calcToHit());
        }
        return toHit;
    }

    public ToHitData getToHit(MovePath shooterPath, boolean assumeUnderFlightPath) {
        if (toHit == null) {
            setToHit(calcToHit(shooterPath, assumeUnderFlightPath));
        }
        return toHit;
    }

    protected void setToHit(ToHitData toHit) {
        this.toHit = toHit;
    }

    protected ToHitData calcToHit() {
        return FireControl.guessToHitModifier(getShooter(), getShooterState(), getTarget(), getTargetState(),
                                               getWeapon(), getGame());
    }

    protected ToHitData calcToHit(MovePath shooterPath, boolean assumeUnderFlightPath) {
        return FireControl.guessAirToGroundStrikeToHitModifier(getShooter(), getTarget(), getTargetState(),
                                                                shooterPath, getWeapon(), getGame(),
                                                                assumeUnderFlightPath);
    }

    public IGame getGame() {
        return game;
    }

    protected void setGame(IGame game) {
        this.game = game;
    }

    public EntityState getShooterState() {
        if (shooterState == null) {
            shooterState = new EntityState(getShooter());
        }
        return shooterState;
    }

    protected void setShooterState(EntityState shooterState) {
        this.shooterState = shooterState;
    }

    public EntityState getTargetState() {
        if (targetState == null) {
            targetState = new EntityState(target);
        }
        return targetState;
    }

    protected void setTargetState(EntityState targetState) {
        this.targetState = targetState;
    }

    protected void setWeapon(Mounted weapon) {
        this.weapon = weapon;
    }

    protected void setHeat(int heat) {
        this.heat = heat;
    }

    public int getHeat() {
        return heat;
    }

    public Mounted getWeapon() {
        return weapon;
    }

    public double getExpectedDamage() {
        return getProbabilityToHit() * getExpectedDamageOnHit();
    }

    protected WeaponAttackAction buildWeaponAttackAction() {
        return new WeaponAttackAction(getShooter().getId(), getTarget().getTargetType(), getTarget().getTargetId(),
                                      getShooter().getEquipmentNum(getWeapon()));
    }

    protected double computeExpectedDamage() {
        if (getTarget() instanceof Entity) {
           return Compute.getExpectedDamage(getGame(), getAction(), true);
        }
        return ((WeaponType)weapon.getType()).getDamage();
    }

    /*
     * Helper function that calculates expected damage
     */
    protected void initDamage(MovePath shooterPath, boolean assumeUnderFlightPath) {
        final String METHOD_NAME = "initDamage(MovePath, boolean)";

        StringBuilder msg = new StringBuilder("Initializing Damage for ").append(getShooter().getDisplayName())
                .append(" firing ").append(getWeapon().getDesc()).append(" at ").append(getTarget().getDisplayName())
                .append(":");

        try {
            // Set up the attack action and calculate the chance to hit.
            setAction(buildWeaponAttackAction());
            if (shooterPath != null) {
                setToHit(calcToHit(shooterPath, assumeUnderFlightPath));
            } else {
                setToHit(calcToHit());
            }

            // Calculate the direction of damage.
            calcDamageDirection();

            // If we can't hit, set everything zero and return..
            if (getToHit().getValue() > 12) {
                Logger.log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg.append("\n\tImpossible toHit: ")
                                                                       .append(getToHit().getValue()).toString());
                setProbabilityToHit(0);
                setMaxDamage(0);
                setHeat(0);
                setExpectedCriticals(0);
                setKillProbability(0);
                setExpectedDamageOnHit(0);
                return;
            }

            setProbabilityToHit(Compute.oddsAbove(getToHit().getValue()) / 100);
            msg.append("\n\tHit Chance: ").append(LOG_PER.format(getProbabilityToHit()));

            setHeat(((WeaponType)getWeapon().getType()).getHeat());
            msg.append("\n\tHeat: ").append(getHeat());

            setExpectedDamageOnHit(computeExpectedDamage());
            setMaxDamage(getExpectedDamageOnHit());
            msg.append("\n\tMax Damage: ").append(LOG_DEC.format(maxDamage));

            double expectedCriticalHitCount =ProbabilityCalculator.getExpectedCriticalHitCount();

            // there's always the chance of rolling a '2'
            final double ROLL_TWO = 0.028;
            setExpectedCriticals(ROLL_TWO * expectedCriticalHitCount * getProbabilityToHit());

            setKillProbability(0);
            if (!(getTarget() instanceof Mech)) {
                return;
            }

            // now guess how many critical hits will be done
            Mech targetMech = (Mech)getTarget();

            // Loop through hit locations.
            for (int i = 0; i <= 7; i++) {
                int hitLocation = i;
                while (targetMech.isLocationBad(hitLocation) && (hitLocation != Mech.LOC_CT)) {
                    if (hitLocation > 7) {
                        hitLocation = 0;
                    }
                    hitLocation = Mech.getInnerLocation(hitLocation);
                }
                double hitLocationProbability =
                        ProbabilityCalculator.getHitProbability(getDamageDirection(), hitLocation);
                int targetArmor = targetMech.getArmor(hitLocation, (getDamageDirection() == 3));
                int targetInternals = targetMech.getInternal(hitLocation);
                if (targetArmor < 0) {
                    targetArmor = 0; // ignore NA or Destroyed cases
                }
                if (targetInternals < 0) {
                    targetInternals = 0;
                }

                // If the location could be destroyed outright...
                if (getExpectedDamageOnHit() > ((targetArmor + targetInternals))) {
                    setExpectedCriticals(getExpectedCriticals() + (hitLocationProbability * getProbabilityToHit()));
                    if ((Mech.LOC_HEAD == hitLocation) || (Mech.LOC_CT == hitLocation)) {
                        setKillProbability(getKillProbability() + (hitLocationProbability * getProbabilityToHit()));
                    }

                    // If the armor can be breached, but the location not destroyed...
                } else if (getExpectedDamageOnHit() > (targetArmor)) {
                    setExpectedCriticals(getExpectedCriticals() +
                                         (hitLocationProbability * getProbabilityToHit() * expectedCriticalHitCount));
                }
            }
        } finally {
            Logger.log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg.toString());
        }
    }

    public WeaponAttackAction getWeaponAttackAction() {
        final String METHOD_NAME = "getWeaponAttackAction(IGame)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            if (getAction() != null) {
                return getAction();
            }
            setAction(new WeaponAttackAction(getShooter().getId(), getTarget().getTargetId(),
                                             getShooter().getEquipmentNum(getWeapon())));
            if (getAction() == null) {
                setProbabilityToHit(0);
                return null;
            }
            setProbabilityToHit(Compute.oddsAbove(getAction().toHit(getGame()).getValue()) / 100.0);
            return getAction();
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    String getDebugDescription() {
        return getWeapon().getName() + " P. Hit: " + LOG_PER.format(getProbabilityToHit())
                + ", Max Dam: " + LOG_DEC.format(getMaxDamage())
                + ", Exp. Dam: " + LOG_DEC.format(getExpectedDamageOnHit())
                + ", Num Crits: " + LOG_DEC.format(getExpectedCriticals())
                + ", Kill Prob: " + LOG_PER.format(getKillProbability());

    }

}