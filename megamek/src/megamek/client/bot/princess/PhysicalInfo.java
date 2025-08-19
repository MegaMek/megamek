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

import java.text.DecimalFormat;
import java.text.NumberFormat;

import megamek.client.bot.PhysicalOption;
import megamek.common.ToHitData;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.units.TripodMek;
import megamek.logging.MMLogger;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/18/13 1:29 PM
 */
public class PhysicalInfo {
    private final static MMLogger logger = MMLogger.create(PhysicalInfo.class);

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
    private double killProbability; // probability to destroy CT or HEAD (ignores criticalSlots)
    private double utility; // filled out externally
    private final Princess owner;

    /**
     * For unit testing.
     *
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
     * @param shooter            The {@link megamek.common.units.Entity} doing the attacking.
     * @param shooterState       The current {@link megamek.client.bot.princess.EntityState} of the attacker.
     * @param target             The {@link megamek.common.units.Targetable} of the attack.
     * @param targetState        The current {@link megamek.client.bot.princess.EntityState} of the target.
     * @param physicalAttackType The type of attack being made.
     * @param game               The current {@link Game}
     * @param owner              The owning {@link Princess} bot.
     * @param guess              Set TRUE to estimate the chance to hit rather than doing the full calculation.
     */
    PhysicalInfo(Entity shooter, EntityState shooterState, Targetable target, EntityState targetState,
          PhysicalAttackType physicalAttackType, Game game, Princess owner, boolean guess) {

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
     *
     * @return The resulting {@link PhysicalAttackType}.
     */
    protected PhysicalAttackAction buildAction(PhysicalAttackType attackType, int shooterId, Targetable target) {
        if (attackType.isPunch()) {
            int armId = PhysicalAttackType.RIGHT_PUNCH == attackType ? PunchAttackAction.RIGHT : PunchAttackAction.LEFT;
            return new PunchAttackAction(shooterId, target.getTargetType(), target.getId(), armId, false, false, false);
        } else if (attackType.isKick()) {
            int legId = PhysicalAttackType.RIGHT_KICK == attackType ? KickAttackAction.RIGHT : KickAttackAction.LEFT;
            return new KickAttackAction(shooterId, target.getTargetType(), target.getId(), legId);
        } else {
            // todo handle other physical attack types.
            return null;
        }
    }

    /**
     * Basic constructor.
     *
     * @param shooter            The {@link megamek.common.units.Entity} doing the attacking.
     * @param target             The {@link Targetable} of the attack.
     * @param physicalAttackType The type of attack being made.
     * @param game               The current {@link Game}
     * @param owner              The owning {@link Princess} bot.
     * @param guess              Set TRUE to estimate the chance to hit rather than doing the full calculation.
     */
    PhysicalInfo(Entity shooter, Targetable target, PhysicalAttackType physicalAttackType, Game game, Princess owner,
          boolean guess) {
        this(shooter, null, target, null, physicalAttackType, game, owner, guess);
    }

    /**
     * Helper function to determine damage and criticalSlots
     */
    protected void initDamage(PhysicalAttackType physicalAttackType, EntityState shooterState, EntityState targetState,
          boolean guess, Game game) {
        StringBuilder msg = new StringBuilder("Initializing Damage for ").append(getShooter().getDisplayName())
              .append(" ").append(physicalAttackType.toString())
              .append(" at ").append(getTarget().getDisplayName())
              .append(":");

        // Only meks do physical attacks.
        if (!(getShooter() instanceof Mek)) {
            logger.warn(msg.append("\n\tNot a mek!").toString());
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
            setHitData(owner.getFireControl(getShooter()).guessToHitModifierPhysical(getShooter(), shooterState,
                  getTarget(),
                  targetState, getAttackType(), game));
        } else {
            PhysicalAttackAction action = buildAction(physicalAttackType, getShooter().getId(), getTarget());
            setAction(action);
            setHitData(physicalAttackType.isPunch() ? ((PunchAttackAction) action).toHit(game)
                  : ((KickAttackAction) action).toHit(game));
        }

        // Get the attack direction.
        setDamageDirection(targetState, shooterState.getPosition());

        // If we can't hit, set all values to 0 and return.
        if (getHitData().getValue() > 12) {
            logger
                  .info(msg.append("\n\tImpossible toHit: ").append(getHitData().getValue()).toString());
            setProbabilityToHit(0);
            setMaxDamage(0);
            setExpectedCriticals(0);
            setKillProbability(0);
            setExpectedDamageOnHit(0);
            return;
        }

        // Calculate the max damage.
        if (physicalAttackType.isPunch()) {
            if ((getShooter() instanceof BipedMek) || (getShooter() instanceof TripodMek)) {
                setMaxDamage((int) Math.ceil(getShooter().getWeight() / 10.0));
            } else {
                // Only bipeds & tripods can punch.
                logger.warn(msg.append("\n\tnon-biped/tripod trying to punch!").toString());
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

        if (shooterState.hasNaturalAptPiloting()) {
            msg.append("\n\tAttacker has Natural Aptitude Piloting");
        }
        setProbabilityToHit(Compute.oddsAbove(getHitData().getValue(), shooterState.hasNaturalAptPiloting()) / 100.0);

        setExpectedDamageOnHit(getMaxDamage());

        double expectedCriticalHitCount = ProbabilityCalculator.getExpectedCriticalHitCount();

        // there's always the chance of rolling a '2'
        final double ROLL_TWO = 0.028;
        setExpectedCriticals(ROLL_TWO * expectedCriticalHitCount * getProbabilityToHit());
        setKillProbability(0);

        if (!(getTarget() instanceof Mek targetMek)) {
            return;
        }

        // now guess how many critical hits will be done
        for (int i = 0; i <= 7; i++) {
            int hitLoc = i;
            while (targetMek.isLocationBad(hitLoc) && (hitLoc != Mek.LOC_CT)
                  // Need to account for still-active 'Meks with destroyed
                  // heads so as not to spin into an endless loop.
                  && (hitLoc != Mek.LOC_HEAD)) {
                if (hitLoc > 7) {
                    hitLoc = 0;
                }
                hitLoc = Mek.getInnerLocation(hitLoc);
            }
            double hitLocationProbability;
            if (getAttackType().isPunch()) {
                hitLocationProbability = ProbabilityCalculator.getHitProbability_Punch(getDamageDirection(), hitLoc);
            } else { // assume kick
                hitLocationProbability = ProbabilityCalculator.getHitProbability_Kick(getDamageDirection(), hitLoc);
            }
            int targetArmor = targetMek.getArmor(hitLoc, (getDamageDirection() == 3));
            int targetInternals = targetMek.getInternal(hitLoc);
            if (targetArmor < 0) {
                targetArmor = 0; // ignore NA or Destroyed cases
            }
            if (targetInternals < 0) {
                targetInternals = 0;
            }

            // If the location could be destroyed outright...
            if (getExpectedDamageOnHit() > ((targetArmor + targetInternals))) {
                setExpectedCriticals(getExpectedCriticals() + hitLocationProbability * getProbabilityToHit());
                if ((hitLoc == Mek.LOC_HEAD) || (hitLoc == Mek.LOC_CT)) {
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
              + ", Num Criticals: " + LOG_DEC.format(getExpectedCriticals())
              + ", Kill Prob: " + LOG_PER.format(getKillProbability());

    }
}
