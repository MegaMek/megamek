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
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.util.Logger;

/**
 * PhysicalInfo is a wrapper around a PhysicalAttackAction that includes
 * probability to hit and expected damage
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/12/13 8:16 PM
 */
public class PhysicalInfo {
    private Entity shooter;
    private Targetable target;
    private PhysicalAttackAction action;
    private PhysicalAttackType attackType;
    private ToHitData toHit = null;
    private double probabilityToHit;
    private double maxDamage;
    private double expectedDamageOnHit;
    private int damageDirection = -1; // direction damage is coming from relative to target
    private double expectedCriticals;
    private double killProbability; // probability to destroy CT or HEAD (ignores criticals)
    private double utility = -1;
    private EntityState shooterState = null;
    private EntityState targetState = null;
    private IGame game;

    /**
     * For unit testing.
     */
    protected PhysicalInfo() {}

    public PhysicalInfo(Entity shooter, EntityState shooterState, Targetable target, EntityState targetState,
                 PhysicalAttackType physicalAttackType, IGame game) {
        final String METHOD_NAME = "PhysicalInfo(Entity, EntityState, Targetable, EntityState, PhysicalAttackType, IGame)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            setShooter(shooter);
            setShooterState(shooterState);
            setTarget(target);
            setTargetState(targetState);
            setAttackType(physicalAttackType);
            setGame(game);
            initDamage();
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public PhysicalInfo(Entity shooter, Targetable target, PhysicalAttackType physicalAttackType, IGame game) {
        this(shooter, null, target, null, physicalAttackType, game);
    }

    public double calcExpectedDamage() {
        return getProbabilityToHit() * getExpectedDamageOnHit();
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

    public int getDamageDirection() {
        if (damageDirection == -1) {
            damageDirection = calcDamageDirection();
        }
        return damageDirection;
    }

    public int calcDamageDirection() {
        return ((calcAttackDirection() - targetState.getFacing()) + 6) % 6;
    }

    public void setDamageDirection(int damageDirection) {
        this.damageDirection = damageDirection;
    }

    private int calcAttackDirection() {
        return getTargetState().getPosition().direction(getShooterState().getPosition());
    }

    public double getExpectedCriticals() {
        return expectedCriticals;
    }

    public void setExpectedCriticals(double expectedCriticals) {
        this.expectedCriticals = expectedCriticals;
    }

    public double getExpectedDamageOnHit() {
        return expectedDamageOnHit;
    }

    public void setExpectedDamageOnHit(double expectedDamageOnHit) {
        this.expectedDamageOnHit = expectedDamageOnHit;
    }

    public double getKillProbability() {
        return killProbability;
    }

    public void setKillProbability(double killProbability) {
        this.killProbability = killProbability;
    }

    public double getMaxDamage() {
        return maxDamage;
    }

    public void setMaxDamage(double maxDamage) {
        this.maxDamage = maxDamage;
    }

    public double getProbabilityToHit() {
        return probabilityToHit;
    }

    public void setProbabilityToHit(double probabilityToHit) {
        this.probabilityToHit = probabilityToHit;
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

    public ToHitData getToHit() {
        if (toHit == null) {
            calcToHit();
        }
        return toHit;
    }

    protected void calcToHit() {
        toHit = FireControl.guessToHitModifierPhysical(getShooter(), getShooterState(), getTarget(), getTargetState(),
                                                                   getAttackType(), getGame());
    }

    public double getUtility() {
        if (utility == -1) {
            calculateUtility();
        }
        return utility;
    }

    private void setUtility(double utility) {
        this.utility = utility;
    }

    public IGame getGame() {
        return game;
    }

    public void setGame(IGame game) {
        this.game = game;
    }

    public EntityState getShooterState() {
        if ((shooterState == null) && (shooter != null)) {
            shooterState = new EntityState(shooter);
        }
        return shooterState;
    }

    public void setShooterState(EntityState shooterState) {
        this.shooterState = shooterState;
    }

    public EntityState getTargetState() {
        if ((targetState == null) && (target != null)) {
            targetState = new EntityState(target);
        }
        return targetState;
    }

    public void setTargetState(EntityState targetState) {
        this.targetState = targetState;
    }

    /**
     * Helper function to determine damage and criticals
     */
    protected void initDamage() {
        final String METHOD_NAME = "initDamage()";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            if ((PhysicalAttackType.LEFT_PUNCH == getAttackType())
                    || (PhysicalAttackType.RIGHT_PUNCH == getAttackType())) {
                if (shooter instanceof BipedMech) {
                    setMaxDamage(Math.ceil(getShooter().getWeight() / 10.0));
                } else {
                    setMaxDamage(0);
                }
            } else { // assuming kick
                setMaxDamage(Math.floor(getShooter().getWeight()) / 5.0);
            }

            setProbabilityToHit(Compute.oddsAbove(getToHit().getValue()) / 100);
            setExpectedDamageOnHit(getMaxDamage());
            double expectedCriticalHitCount = ProbabilityCalculator.getExpectedCriticalHitCount();

            // there's always the chance of rolling a '2'
            final double ROLL_TWO = 0.028;
            setExpectedCriticals(ROLL_TWO * expectedCriticalHitCount * getProbabilityToHit());
            setKillProbability(0);

            if (!(getTarget() instanceof Mech)) {
                calculateUtility();
                return;
            }

            // now guess how many critical hits will be done
            Mech targetMech = (Mech)getTarget();
            for (int i = 0; i <= 7; i++) {
                int hitLocation = i;
                while (targetMech.isLocationBad(hitLocation) && (hitLocation != Mech.LOC_CT)) {
                    hitLocation++;
                    if (hitLocation > 7) {
                        hitLocation = 0;
                    }
                    hitLocation = Mech.getInnerLocation(hitLocation);
                }
                double hitLocationProbability;
                if ((PhysicalAttackType.RIGHT_PUNCH == getAttackType())
                        || (PhysicalAttackType.LEFT_PUNCH == getAttackType())) {
                    hitLocationProbability = ProbabilityCalculator.getHitProbability_Punch(getDamageDirection(),
                                                                                           hitLocation);
                } else { // assume kick
                    hitLocationProbability = ProbabilityCalculator.getHitProbability_Kick(getDamageDirection(),
                                                                                          hitLocation);
                }
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
            calculateUtility();
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Current bot code requires physical attacks to be given as 'physical
     * option'. This does the necessary conversion
     */
    public PhysicalOption getAsPhysicalOption(Princess owner) {
        final String METHOD_NAME = "getAsPhysicalOption()";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            int optionInteger = 0;
            if (attackType == PhysicalAttackType.RIGHT_PUNCH) {
                optionInteger = PhysicalOption.PUNCH_RIGHT;
            }
            if (attackType == PhysicalAttackType.LEFT_PUNCH) {
                optionInteger = PhysicalOption.PUNCH_LEFT;
            }
            if (attackType == PhysicalAttackType.RIGHT_KICK) {
                optionInteger = PhysicalOption.KICK_RIGHT;
            }
            if (attackType == PhysicalAttackType.LEFT_KICK) {
                optionInteger = PhysicalOption.KICK_LEFT;
            }
            return new PhysicalOption(shooter, target, 0, optionInteger, null);
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * calculates the 'utility' of a physical action.
     */
    protected void calculateUtility() {
        final double damageUtility = 1.0;
        final double criticalUtility = 10.0;
        final double killUtility = 50.0;

        double utility = damageUtility * calcExpectedDamage();
        utility += criticalUtility * getExpectedCriticals();
        utility += killUtility * getKillProbability();
        setUtility(utility);
    }
}