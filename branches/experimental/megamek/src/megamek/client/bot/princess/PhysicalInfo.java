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

import megamek.client.bot.PhysicalOption;
import megamek.common.BipedMech;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.TripodMech;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.logging.LogLevel;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at gmail dot com)
 * @since 12/18/13 1:29 PM
 */
public class PhysicalInfo {
    private static final NumberFormat LOG_PER = NumberFormat.getPercentInstance();
    private static final NumberFormat LOG_DEC = DecimalFormat.getInstance();

    private Entity shooter;
    private Targetable target;
    private PhysicalAttackAction action;
    private PhysicalAttackType attackType;
    private ToHitData hitData;
    private double probabilityToHit;
    private double maxDamage;
    private double expectedDamageOnHit;
    private int damageDirection; // direction damage is coming from relative to target
    private double expectedCriticals;
    private double killProbability; // probability to destroy CT or HEAD (ignores criticals)
    private double utility; // filled out externally
    private Princess owner;

    /**
     * For unit testing.
     *
     * @param owner
     */
    protected PhysicalInfo(Princess owner) {
        this.owner = owner;
    }

    public double getExpectedDamage() {
        return getProbabilityToHit() * getExpectedDamageOnHit();
    }

    /**
     * Constructor including the shooter and target's state information.
     *
     * @param shooter            The {@link megamek.common.Entity} doing the attacking.
     * @param shooterState       The current {@link megamek.client.bot.princess.EntityState} of the attacker.
     * @param target             The {@link megamek.common.Targetable} of the attack.
     * @param targetState        The current {@link megamek.client.bot.princess.EntityState} of the target.
     * @param physicalAttackType The type of attack being made.
     * @param game               The {@link megamek.common.IGame} in progress.
     * @param owner              The owning {@link Princess} bot.
     * @param guess              Set TRUE to estimate the chance to hit rather than doing the full calculation.
     */
    PhysicalInfo(Entity shooter, EntityState shooterState, Targetable target, EntityState targetState,
                 PhysicalAttackType physicalAttackType, IGame game, Princess owner, boolean guess) {

        this.owner = owner;

        setShooter(shooter);
        setTarget(target);
        setAttackType(physicalAttackType);
        initDamage(physicalAttackType, shooterState, targetState, guess, game);
    }

    /**
     * Builds a new {@link PhysicalAttackAction} from the given parameters.
     *
     * @param attackType The {@link PhysicalAttackType} of the attack.
     * @param shooterId  The ID of the attacking unit.
     * @param target     The unit being attacked.
     * @return The resulting {@link PhysicalAttackType}.
     */
    protected PhysicalAttackAction buildAction(PhysicalAttackType attackType, int shooterId, Targetable target) {
        if (attackType.isPunch()) {
            int armId = PhysicalAttackType.RIGHT_PUNCH == attackType ? PunchAttackAction.RIGHT : PunchAttackAction.LEFT;
            return new PunchAttackAction(shooterId, target.getTargetType(), target.getTargetId(), armId, false, false);
        } else if (attackType.isKick()) {
            int legId = PhysicalAttackType.RIGHT_KICK == attackType ? KickAttackAction.RIGHT : KickAttackAction.LEFT;
            return new KickAttackAction(shooterId, target.getTargetType(), target.getTargetId(), legId);
        } else {
            // todo handle other physical attack types.
            return null;
        }
    }

    /**
     * Basic constructor.
     *
     * @param shooter            The {@link megamek.common.Entity} doing the attacking.
     * @param target             The {@link megamek.common.Targetable} of the attack.
     * @param physicalAttackType The type of attack being made.
     * @param game               The {@link megamek.common.IGame} in progress.
     * @param owner              The owning {@link Princess} bot.
     * @param guess              Set TRUE to estimate the chance to hit rather than doing the full calculation.
     */
    PhysicalInfo(Entity shooter, Targetable target, PhysicalAttackType physicalAttackType, IGame game, Princess owner,
                 boolean guess) {
        this(shooter, null, target, null, physicalAttackType, game, owner, guess);
    }

    /**
     * Helper function to determine damage and criticals
     */
    protected void initDamage(PhysicalAttackType physicalAttackType, EntityState shooterState, EntityState targetState,
                              boolean guess, IGame game) {
        final String METHOD_NAME = "initDamage(PhysicalAttackType, EntityState, EntityState, boolean, IGame)";

        StringBuilder msg =
                new StringBuilder("Initializing Damage for ").append(getShooter().getDisplayName())
                                                             .append(" ").append(physicalAttackType.toString())
                                                             .append(" at ").append(getTarget().getDisplayName())
                                                             .append(":");

        // Only mechs do physical attacks.
        if (!(getShooter() instanceof Mech)) {
            owner.log(getClass(), METHOD_NAME, LogLevel.WARNING, msg.append("\n\tNot a mech!").toString());
            setProbabilityToHit(0);
            setMaxDamage(0);
            setExpectedCriticals(0);
            setKillProbability(0);
            setExpectedDamageOnHit(0);
            return;
        }

        if (shooterState == null) {
            shooterState = new EntityState(getShooter());
        }
        if (targetState == null) {
            targetState = new EntityState(getTarget());
        }

        // Build the to hit data.
        if (guess) {
            setHitData(owner.getFireControl().guessToHitModifierPhysical(getShooter(), shooterState, getTarget(),
                                                                         targetState, getAttackType(), game));
        } else {
            PhysicalAttackAction action = buildAction(physicalAttackType, getShooter().getId(), getTarget());
            setAction(action);
            setHitData(physicalAttackType.isPunch() ?
                               ((PunchAttackAction) action).toHit(game) :
                               ((KickAttackAction) action).toHit(game));
        }

        // Get the attack direction.
        setDamageDirection(targetState, shooterState.getPosition());

        // If we can't hit, set all values to 0 and return.
        if (getHitData().getValue() > 12) {
            owner.log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg.append("\n\tImpossible toHit: ")
                                                                  .append(getHitData().getValue()).toString());
            setProbabilityToHit(0);
            setMaxDamage(0);
            setExpectedCriticals(0);
            setKillProbability(0);
            setExpectedDamageOnHit(0);
            return;
        }

        // Calculate the max damage.
        if (physicalAttackType.isPunch()) {
            if ((getShooter() instanceof BipedMech) || (getShooter() instanceof TripodMech)) {
                setMaxDamage((int) Math.ceil(getShooter().getWeight() / 10.0));
            } else {
                // Only bipeds & tripods can punch.
                owner.log(getClass(), METHOD_NAME, LogLevel.WARNING,
                          msg.append("\n\tnon-biped/tripod trying to punch!").toString());
                setProbabilityToHit(0);
                setMaxDamage(0);
                setExpectedCriticals(0);
                setKillProbability(0);
                setExpectedDamageOnHit(0);
                return;
            }
        } else { // assuming kick
            setMaxDamage((int) Math.floor(getShooter().getWeight() / 5.0));
        }

        setProbabilityToHit(Compute.oddsAbove(getHitData().getValue()) / 100.0);

        setExpectedDamageOnHit(getMaxDamage());

        double expectedCriticalHitCount = ProbabilityCalculator.getExpectedCriticalHitCount();

        // there's always the chance of rolling a '2'
        final double ROLL_TWO = 0.028;
        setExpectedCriticals(ROLL_TWO * expectedCriticalHitCount * getProbabilityToHit());
        setKillProbability(0);

        if (!(getTarget() instanceof Mech)) {
            return;
        }

        // now guess how many critical hits will be done
        Mech targetMech = (Mech) getTarget();
        for (int i = 0; i <= 7; i++) {
            int hitLoc = i;
            while (targetMech.isLocationBad(hitLoc) && (hitLoc != Mech.LOC_CT)) {
                if (hitLoc > 7) {
                    hitLoc = 0;
                }
                hitLoc = Mech.getInnerLocation(hitLoc);
            }
            double hitLocationProbability;
            if (getAttackType().isPunch()) {
                hitLocationProbability = ProbabilityCalculator.getHitProbability_Punch(getDamageDirection(), hitLoc);
            } else { // assume kick
                hitLocationProbability = ProbabilityCalculator.getHitProbability_Kick(getDamageDirection(), hitLoc);
            }
            int targetArmor = targetMech.getArmor(hitLoc, (getDamageDirection() == 3));
            int targetInternals = targetMech.getInternal(hitLoc);
            if (targetArmor < 0) {
                targetArmor = 0; // ignore NA or Destroyed cases
            }
            if (targetInternals < 0) {
                targetInternals = 0;
            }

            // If the location could be destroyed outright...
            if (getExpectedDamageOnHit() > ((targetArmor + targetInternals))) {
                setExpectedCriticals(getExpectedCriticals() + hitLocationProbability * getProbabilityToHit());
                if ((hitLoc == Mech.LOC_HEAD) || (hitLoc == Mech.LOC_CT)) {
                    setKillProbability(getKillProbability() + hitLocationProbability * getProbabilityToHit());
                }

                // If the armor can be breached, but the location not destroyed...
            } else if (getExpectedDamageOnHit() > (targetArmor)) {
                setExpectedCriticals(getExpectedCriticals() +
                                             hitLocationProbability *
                                                     ProbabilityCalculator.getExpectedCriticalHitCount() *
                                                     getProbabilityToHit());
            }
        }
    }

    /**
     * Current bot code requires physical attacks to be given as 'physical option'. This does the necessary conversion
     */
    public PhysicalOption getAsPhysicalOption() {
        int optionInteger = 0;
        if (getAttackType() == PhysicalAttackType.RIGHT_PUNCH) {
            optionInteger = PhysicalOption.PUNCH_RIGHT;
        }
        if (getAttackType() == PhysicalAttackType.LEFT_PUNCH) {
            optionInteger = PhysicalOption.PUNCH_LEFT;
        }
        if (getAttackType() == PhysicalAttackType.RIGHT_KICK) {
            optionInteger = PhysicalOption.KICK_RIGHT;
        }
        if (getAttackType() == PhysicalAttackType.LEFT_KICK) {
            optionInteger = PhysicalOption.KICK_LEFT;
        }
        return new PhysicalOption(getShooter(), getTarget(), 0, optionInteger, null);
    }

    public Entity getShooter() {
        return shooter;
    }

    public void setShooter(Entity shooter) {
        this.shooter = shooter;
    }

    public Targetable getTarget() {
        return target;
    }

    public void setTarget(Targetable target) {
        this.target = target;
    }

    public PhysicalAttackAction getAction() {
        return action;
    }

    public void setAction(PhysicalAttackAction action) {
        this.action = action;
    }

    public PhysicalAttackType getAttackType() {
        return attackType;
    }

    public void setAttackType(PhysicalAttackType attackType) {
        this.attackType = attackType;
    }

    public ToHitData getHitData() {
        return hitData;
    }

    public void setHitData(ToHitData hitData) {
        this.hitData = hitData;
    }

    public double getProbabilityToHit() {
        return probabilityToHit;
    }

    public void setProbabilityToHit(double probabilityToHit) {
        this.probabilityToHit = probabilityToHit;
    }

    public double getMaxDamage() {
        return maxDamage;
    }

    public void setMaxDamage(double maxDamage) {
        this.maxDamage = maxDamage;
    }

    public double getExpectedDamageOnHit() {
        return expectedDamageOnHit;
    }

    public void setExpectedDamageOnHit(double expectedDamageOnHit) {
        this.expectedDamageOnHit = expectedDamageOnHit;
    }

    public int getDamageDirection() {
        return damageDirection;
    }

    public void setDamageDirection(int damageDirection) {
        this.damageDirection = damageDirection;
    }

    protected void setDamageDirection(EntityState targetState, Coords shooterCoords) {
        int fromDirection = targetState.getPosition().direction(shooterCoords);
        setDamageDirection(((fromDirection - targetState.getFacing()) + 6) % 6);
    }

    public double getExpectedCriticals() {
        return expectedCriticals;
    }

    public void setExpectedCriticals(double expectedCriticals) {
        this.expectedCriticals = expectedCriticals;
    }

    public double getKillProbability() {
        return killProbability;
    }

    public void setKillProbability(double killProbability) {
        this.killProbability = killProbability;
    }

    public double getUtility() {
        return utility;
    }

    public void setUtility(double utility) {
        this.utility = utility;
    }

    String getDebugDescription() {
        return getAttackType().toString() + " P. Hit: " + LOG_PER.format(getProbabilityToHit())
                + ", Max Dam: " + LOG_DEC.format(getMaxDamage())
                + ", Exp. Dam: " + LOG_DEC.format(getExpectedDamageOnHit())
                + ", Num Crits: " + LOG_DEC.format(getExpectedCriticals())
                + ", Kill Prob: " + LOG_PER.format(getKillProbability());

    }
}
