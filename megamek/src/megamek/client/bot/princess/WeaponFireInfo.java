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

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.logging.LogLevel;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * WeaponFireInfo is a wrapper around a WeaponAttackAction that includes
 * probability to hit and expected damage
 *
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 11/24/14 2:50 PM
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
    private Princess owner;

    /**
     * For unit testing.
     */
    protected WeaponFireInfo(Princess owner) {
        this.owner = owner;
    }

    /**
     * Basic constructor.
     *
     * @param shooter The {@link megamek.common.Entity} doing the attacking.
     * @param target  The {@link megamek.common.Targetable} of the attack.
     * @param weapon  The {@link megamek.common.Mounted} weapon used for the attack.
     * @param game    The {@link megamek.common.IGame} in progress.
     * @param guess   Set TRUE to estimate the chance to hit rather than doing the full calculation.
     */
    WeaponFireInfo(Entity shooter, Targetable target, Mounted weapon, IGame game, boolean guess, Princess owner) {
        this(shooter, null, null, target, null, weapon, game, false, guess, owner, new int[0]);
    }

    /**
     * Constructor including the shooter and target's state information.
     *
     * @param shooter      The {@link megamek.common.Entity} doing the attacking.
     * @param shooterState The current {@link megamek.client.bot.princess.EntityState} of the attacker.
     * @param target       The {@link megamek.common.Targetable} of the attack.
     * @param targetState  The current {@link megamek.client.bot.princess.EntityState} of the target.
     * @param weapon       The {@link megamek.common.Mounted} weapon used for the attack.
     * @param game         The {@link megamek.common.IGame} in progress.
     * @param guess        Set TRUE to estimate the chance to hit rather than doing the full calculation.
     */
    WeaponFireInfo(Entity shooter, EntityState shooterState, Targetable target, EntityState targetState,
                   Mounted weapon, IGame game, boolean guess, Princess owner) {
        this(shooter, shooterState, null, target, targetState, weapon, game, false, guess, owner, new int[0]);
    }

    /**
     * Constructor for aerospace units performing Strike attacks.
     *
     * @param shooter               The {@link megamek.common.Entity} doing the attacking.
     * @param shooterPath           The {@link megamek.common.MovePath} of the attacker.
     * @param target                The {@link megamek.common.Targetable} of the attack.
     * @param targetState           The current {@link megamek.client.bot.princess.EntityState} of the target.
     * @param weapon                The {@link megamek.common.Mounted} weapon used for the attack.
     * @param game                  The {@link megamek.common.IGame} in progress.
     * @param assumeUnderFlightPath Set TRUE for aerial units performing air-to-ground attacks.
     * @param guess                 Set TRUE to estimate the chance to hit rather than doing the full calculation.
     * @param owner                 Instance of the princess owner
     * @param bombPayload           The bomb payload, as described in WeaponAttackAction.setBombPayload
     */
    WeaponFireInfo(Entity shooter, MovePath shooterPath, Targetable target, EntityState targetState,
                   Mounted weapon, IGame game, boolean assumeUnderFlightPath, boolean guess, Princess owner, int[] bombPayload) {
        this(shooter, null, shooterPath, target, targetState, weapon, game, assumeUnderFlightPath, guess, owner, bombPayload);
    }

    /**
     * This constructs a WeaponFireInfo using the best guess of how likely an aerospace unit using a strike attack will
     * hit, without actually constructing the {@link WeaponAttackAction}
     *
     * @param shooter               The {@link megamek.common.Entity} doing the attacking.
     * @param shooterState          The current {@link megamek.client.bot.princess.EntityState} of the attacker.
     * @param shooterPath           The {@link megamek.common.MovePath} of the attacker.
     * @param target                The {@link megamek.common.Targetable} of the attack.
     * @param targetState           The current {@link megamek.client.bot.princess.EntityState} of the target.
     * @param weapon                The {@link megamek.common.Mounted} weapon used for the attack.
     * @param game                  The {@link megamek.common.IGame} in progress.
     * @param assumeUnderFlightPath Set TRUE for aerial units performing air-to-ground attacks.
     * @param guess                 Set TRUE to estimate the chance to hit rather than going through the full
     *                              calculation.
     * @param owner                 Instance of the princess owner
     * @param bombPayload           The bomb payload, as described in WeaponAttackAction.setBombPayload
     */
    private WeaponFireInfo(Entity shooter, EntityState shooterState, MovePath shooterPath, Targetable target,
                   EntityState targetState, Mounted weapon, IGame game, boolean assumeUnderFlightPath,
                   boolean guess, Princess owner, int[] bombPayload) {
        final String METHOD_NAME =
                "WeaponFireInfo(Entity, EntityState, MovePath, Targetable, EntityState, Mounted, IGame, boolean)";
        owner.methodBegin(getClass(), METHOD_NAME);
        this.owner = owner;

        try {
            setShooter(shooter);
            setShooterState(shooterState);
            setTarget(target);
            setTargetState(targetState);
            setWeapon(weapon);
            setGame(game);
            initDamage(shooterPath, assumeUnderFlightPath, guess, bombPayload);
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
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
        return owner.getFireControl().guessToHitModifierForWeapon(getShooter(), getShooterState(), getTarget(),
                                                                  getTargetState(),
                                                                  getWeapon(), getGame());
    }

    protected ToHitData calcToHit(MovePath shooterPath, boolean assumeUnderFlightPath) {
        return owner.getFireControl().guessAirToGroundStrikeToHitModifier(getShooter(), null, getTarget(),
                                                                          getTargetState(),
                                                                          shooterPath, getWeapon(), getGame(),
                                                                          assumeUnderFlightPath);
    }

    protected ToHitData calcRealToHit(WeaponAttackAction weaponAttackAction) {
        return weaponAttackAction.toHit(getGame(), 
                owner.getPrecognition().getECMInfo());
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
    
    protected WeaponAttackAction buildBombAttackAction(int[] bombPayload) {
        WeaponAttackAction diveBomb = new WeaponAttackAction(getShooter().getId(), getTarget().getTargetType(), getTarget().getTargetId(),
                getShooter().getEquipmentNum(getWeapon()));
        
        diveBomb.setBombPayload(bombPayload);
        
        return diveBomb;
    }

    protected double computeExpectedDamage() {
        // bombs require some special consideration
        if(weapon.isGroundBomb()) {
            return computeExpectedBombDamage(getShooter(), weapon, getTarget().getPosition());
        }
        
        // For clan plasma cannon, assume 7 "damage".
        WeaponType weaponType = (WeaponType) weapon.getType();
        if (weaponType.hasFlag(WeaponType.F_PLASMA) &&
                weaponType.getTechLevels().containsValue(TechConstants.T_CLAN_TW)) {
            return 7D;
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
     * Worker function to compute expected bomb damage given the shooter
     * @param shooter
     * @param weapon
     * @param bombedHex
     * @return
     */
    protected double computeExpectedBombDamage(Entity shooter, Mounted weapon, Coords bombedHex) {
        double damage = 0D; //lol double damage I wish
        
        // for dive attacks, we can pretty much assume that we're going to drop everything we've got on the poor scrubs in this hex
        if(weapon.getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
            for(Mounted bomb : shooter.getBombs(BombType.F_GROUND_BOMB)) {
                int damagePerShot = ((BombType) bomb.getType()).getDamagePerShot();
        
                // HE, thunder, laser and inferno bombs just affect the target hex 
                List<Coords> affectedHexes = new ArrayList<Coords>();
                affectedHexes.add(bombedHex);
                
                // a cluster bomb affects all hexes around the target
                if(((BombType) bomb.getType()).getBombType() == BombType.B_CLUSTER) {
                    for(int dir = 0; dir <= 5; dir++) {
                        affectedHexes.add(bombedHex.translated(dir));
                    }
                }
                
                // now we go through all affected hexes and add up the damage done
                for(Coords coords : affectedHexes) {
                    for(Iterator<Entity> iter = game.getEntities(bombedHex); iter.hasNext();) { 
                        Entity currentVictim = iter.next();
                        
                        if(currentVictim.getOwner().getTeam() != shooter.getOwner().getTeam()) {
                            damage += damagePerShot;
                        }
                        else { // we prefer not to blow up friendlies if we can help it
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
     * @param assumeUnderFlightPath If TRUE, aero units will not check to make sure the target is under their flight
     *                              path.
     * @param guess Set TRUE to esitmate the chance to hit rather than doing the full calculation.
     */
    protected void initDamage(@Nullable MovePath shooterPath, boolean assumeUnderFlightPath, boolean guess, int[] bombPayload) {
        final String METHOD_NAME = "initDamage(MovePath, boolean)";

        StringBuilder msg =
                new StringBuilder("Initializing Damage for ").append(getShooter().getDisplayName())
                                                             .append(" firing ").append(getWeapon().getDesc())
                                                             .append(" at ").append(getTarget().getDisplayName())
                                                             .append(":");

        try {
            // Set up the attack action and calculate the chance to hit.
            if(bombPayload.length == 0) {
                setAction(buildWeaponAttackAction());
            }
            else {
                setAction(this.buildBombAttackAction(bombPayload));
            }
            
            if (!guess) {
                setToHit(calcRealToHit(getWeaponAttackAction()));
            } else if (shooterPath != null) {
                setToHit(calcToHit(shooterPath, assumeUnderFlightPath));
            } else {
                setToHit(calcToHit());
            }

            // If we can't hit, set everything zero and return..
            if (getToHit().getValue() > 12) {
                owner.log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg.append("\n\tImpossible toHit: ")
                                                                      .append(getToHit().getValue()).toString());
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

            setHeat(((WeaponType) getWeapon().getType()).getHeat());
            msg.append("\n\tHeat: ").append(getHeat());

            setExpectedDamageOnHit(computeExpectedDamage());
            setMaxDamage(getExpectedDamageOnHit());
            msg.append("\n\tMax Damage: ").append(LOG_DEC.format(maxDamage));

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

            // A mech with a torso-mounted cockpit can survive losing its head.
            double headlessOdds = 0.0;

            // Loop through hit locations.
            // todo Targeting tripods.
            for (int i = 0; i <= 7; i++) {
                int hitLocation = i;

                while (targetMech.isLocationBad(hitLocation) &&
                       (hitLocation != Mech.LOC_CT)) {

                    // Head shots don't travel inward if the head is removed.  Instead, a new roll gets made.
                    if (hitLocation == Mech.LOC_HEAD) {
                        headlessOdds = ProbabilityCalculator.getHitProbability(getDamageDirection(), Mech.LOC_HEAD);
                        break;
                    }

                    // Get the next most inward location.
                    hitLocation = Mech.getInnerLocation(hitLocation);
                }
                double hitLocationProbability =
                        ProbabilityCalculator.getHitProbability(getDamageDirection(), hitLocation);

                // Account for the possibility of re-rolling a head hit on a headless mech.
                hitLocationProbability += (hitLocationProbability * headlessOdds);

                // Get the armor and internals for this location.
                int targetArmor = Math.max(0, targetMech.getArmor(hitLocation, (getDamageDirection() == 3)));
                int targetInternals = Math.max(0, targetMech.getInternal(hitLocation));

                // If the location could be destroyed outright...
                if (getExpectedDamageOnHit() > ((targetArmor + targetInternals))) {
                    setExpectedCriticals(getExpectedCriticals() + (hitLocationProbability * getProbabilityToHit()));
                    if (Mech.LOC_CT == hitLocation) {
                        setKillProbability(getKillProbability() + (hitLocationProbability * getProbabilityToHit()));
                    } else if ((Mech.LOC_HEAD == hitLocation) &&
                               (targetMech.getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED)) {
                        setKillProbability(getKillProbability() + (hitLocationProbability * getProbabilityToHit()));
                    }

                    // If the armor can be breached, but the location not destroyed...
                } else if (getExpectedDamageOnHit() > (targetArmor)) {
                    setExpectedCriticals(getExpectedCriticals() +
                                                 (hitLocationProbability * getProbabilityToHit() *
                                                         expectedCriticalHitCount));
                }
            }
        } finally {
            owner.log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg.toString());
        }
    }

    public WeaponAttackAction getWeaponAttackAction() {
        final String METHOD_NAME = "getWeaponAttackAction(IGame)";
        owner.methodBegin(getClass(), METHOD_NAME);

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
            setProbabilityToHit(Compute.oddsAbove(getAction().toHit(getGame()).getValue(),
                                                  getShooterState().hasNaturalAptGun()) / 100.0);
            return getAction();
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
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