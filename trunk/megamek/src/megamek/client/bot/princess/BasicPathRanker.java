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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.client.bot.princess.BotGeometry.HexLine;
import megamek.common.Aero;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.VTOL;
import megamek.common.logging.LogLevel;

/**
 * A very basic pathranker
 */
public class BasicPathRanker extends PathRanker {

    protected final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance());
    protected final NumberFormat LOG_INT = NumberFormat.getIntegerInstance();
    protected final NumberFormat LOG_PERCENT = NumberFormat.getPercentInstance();

    private FireControl fireControl;
    private PathEnumerator pathEnumerator;
    private Princess owner;

    // the best damage enemies could expect were I not here. Used to determine whether they will target me.
    private Map<Integer, Double> bestDamageByEnemies;

    public BasicPathRanker(Princess owningPrincess) {
        super(owningPrincess);
        final String METHOD_NAME = "BasicPathRanker(Princess)";
        bestDamageByEnemies = new TreeMap<Integer, Double>();
        owner = owningPrincess;
        owner.log(getClass(), METHOD_NAME, LogLevel.DEBUG, "Using " + owner.getBehaviorSettings().getDescription() +
                " behavior");
    }

    protected Princess getOwner() {
        return owner;
    }

    public FireControl getFireControl() {
        return fireControl;
    }

//    public PathEnumerator getPathEnumerator() {
//        return pathEnumerator;
//    }

    public void setFireControl(FireControl fireControl) {
        this.fireControl = fireControl;
    }

    public void setPathEnumerator(PathEnumerator pathEnumerator) {
        this.pathEnumerator = pathEnumerator;
    }

    protected Map<Integer, Double> getBestDamageByEnemies() {
        return bestDamageByEnemies;
    }

    protected Coords getClosestCoordsTo(int unitId, Coords location) {
        return pathEnumerator.unit_movable_areas.get(unitId).getClosestCoordsTo(location);
    }

    protected boolean isInMyLoS(Entity unit, HexLine leftBounds, HexLine rightBounds) {
        return (leftBounds.judgeArea(pathEnumerator.unit_movable_areas.get(unit.getId())) > 0)
                && (rightBounds.judgeArea(pathEnumerator.unit_movable_areas.get(unit.getId())) < 0);
    }

    protected double getMaxDamageAtRange(FireControl fireControl, Entity shooter, int range) {
        return fireControl.getMaxDamageAtRange(shooter, range);
    }

    protected boolean canFlankAndKick(Entity enemy, Coords behind, Coords leftFlank, Coords rightFlank, int myFacing) {
        final String METHOD_NAME = "canFlankAndKick(Entity, Coords, Coords, Coords, int)";
        Set<CoordFacingCombo> enemyFacingSet = pathEnumerator.unit_potential_locations.get(enemy.getId());
        if (enemyFacingSet == null) {
            getOwner().log(getClass(), METHOD_NAME, LogLevel.WARNING, "no facing set for " + enemy.getDisplayName());
            return false;
        }
        return enemyFacingSet.contains(new CoordFacingCombo(behind, myFacing))
                || enemyFacingSet.contains(new CoordFacingCombo(behind, (myFacing + 1) % 6))
                || enemyFacingSet.contains(new CoordFacingCombo(behind, (myFacing + 5) % 6))
                || enemyFacingSet.contains(new CoordFacingCombo(leftFlank, myFacing))
                || enemyFacingSet.contains(new CoordFacingCombo(leftFlank, (myFacing + 4) % 6))
                || enemyFacingSet.contains(new CoordFacingCombo(leftFlank, (myFacing + 5) % 6))
                || enemyFacingSet.contains(new CoordFacingCombo(rightFlank, myFacing))
                || enemyFacingSet.contains(new CoordFacingCombo(rightFlank, (myFacing + 1) % 6))
                || enemyFacingSet.contains(new CoordFacingCombo(rightFlank, (myFacing + 2) % 6));
    }

    /**
     * Guesses a number of things about an enemy that has not yet moved
     * TODO estimated damage is sloppy.  Improve for missile attacks, gun skill, and range
     */
    public EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path) {
        final String METHOD_NAME = "EntityEvaluationResponse evaluateUnmovedEnemy(Entity,MovePath,IGame)";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            //some preliminary calculations
            final double damageDiscount = 0.25;
            EntityEvaluationResponse returnResponse = new EntityEvaluationResponse();

            //Aeros always move after other units, and would require an entirely different evaluation
            //TODO (low priority) implement a way to see if I can dodge aero units
            if (enemy instanceof Aero) {
                return returnResponse;
            }

            Coords finalCoords = path.getFinalCoords();
            int myFacing = path.getFinalFacing();
            Coords behind = finalCoords.translated((myFacing + 3) % 6);
            Coords leftFlank = finalCoords.translated((myFacing + 2) % 6);
            Coords rightFlank = finalCoords.translated((myFacing + 4) % 6);
            Coords closest = getClosestCoordsTo(enemy.getId(), finalCoords);
            int range = closest.distance(finalCoords);

            // I would prefer if the enemy must end its move in my line of fire if so, I can guess that I may do some
            // damage to it (cover notwithstanding).  At the very least, I can force the enemy to take cover on its
            // move.
            HexLine leftBounds;
            HexLine rightBounds;
            if (path.getEntity().canChangeSecondaryFacing()) {
                leftBounds = new HexLine(behind, (myFacing + 2) % 6);
                rightBounds = new HexLine(behind, (myFacing + 4) % 6);
            } else {
                leftBounds = new HexLine(behind, (myFacing + 1) % 6);
                rightBounds = new HexLine(behind, (myFacing + 5) % 6);
            }
            boolean inMyLos = isInMyLoS(enemy, leftBounds, rightBounds);
            if (inMyLos) {
                returnResponse.addToMyEstimatedDamage(getMaxDamageAtRange(fireControl, path.getEntity(), range) *
                        damageDiscount);
            }

            //in general if an enemy can end its position in range, it can hit me
            returnResponse.addToEstimatedEnemyDamage(getMaxDamageAtRange(fireControl, enemy, range) * damageDiscount);

            //It is especially embarrassing if the enemy can move behind or flank me and then kick me
            if (canFlankAndKick(enemy, behind, leftFlank, rightFlank, myFacing)) {
                returnResponse.addToEstimatedEnemyDamage(Math.ceil(enemy.getWeight() / 5.0) * damageDiscount);
            }
            return returnResponse;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    protected RankedPath doAeroSpecificRanking(MovePath movePath, boolean vtol) {
        // stalling is bad.
        if (movePath.getFinalVelocity() == 0 && !vtol) {
            return new RankedPath(-1000d, movePath, "stall");
        }

        // So is crashing.
        if (movePath.getFinalAltitude() < 1) {
            return new RankedPath(-10000d, movePath, "crash");
        }

        // Flying off board should only be done if necessary, but is better than taking a lot of damage.
        if ((movePath.getLastStep() != null) && (movePath.getLastStep().getType() == MoveStepType.RETURN)) {
            if (vtol) {
                return new RankedPath(-5000d, movePath, "off-board");
            }
            return new RankedPath(-5d, movePath, "off-board");
        }

        return null;
    }

    @Override
    protected List<TargetRoll> getPSRList(MovePath path) {
        return super.getPSRList(path);
    }

    @Override
    public double getMovePathSuccessProbability(MovePath movePath) {
        return super.getMovePathSuccessProbability(movePath);
    }

    private double calculateFallMod(double successProbability, StringBuilder formula) {
        double pilotingFailure = (1 - successProbability);
        double fallShame = owner.getBehaviorSettings().getFallShameValue();
        double fallMod = pilotingFailure * (pilotingFailure == 1 ? -1000 : fallShame);
        formula.append("fall mod [").append(LOG_DECIMAL.format(fallMod)).append(" = ")
                .append(LOG_DECIMAL.format(pilotingFailure)).append(" * ").append(LOG_DECIMAL.format(fallShame))
                .append("]");
        return fallMod;
    }

    protected double calculateDamagePotential(Entity enemy, EntityState shooterState, MovePath path, IGame game) {
        return getFireControl().guessBestFiringPlanUnderHeatWithTwists(enemy, shooterState, path.getEntity(),
                new EntityState(path), (enemy.getHeatCapacity() - enemy.heat) + 5, game).utility;
    }

    protected double calculateKickDamagePotential(Entity enemy, MovePath path, IGame game) {

        // if they can kick me, and probably hit, they probably will.
        PhysicalInfo theirKick = new PhysicalInfo(enemy, null, path.getEntity(),
                new EntityState(path), PhysicalAttackType.RIGHT_KICK, game, owner);

        if (theirKick.prob_to_hit <= 0.5) {
            return 0.0;
        }
        return theirKick.expected_damage_on_hit * theirKick.prob_to_hit;
    }

    protected double calculateMyDamagePotential(MovePath path, Entity enemy, IGame game) {
        FiringPlan myFiringPlan;
        if (path.getEntity() instanceof Aero) {
            myFiringPlan = fireControl.guessFullAirToGroundPlan(path.getEntity(), enemy,
                    new EntityState(enemy), path, game, false);
        } else {
            myFiringPlan = fireControl.guessBestFiringPlanWithTwists(path.getEntity(),
                    new EntityState(path), enemy, null, game);
        }
        return myFiringPlan.utility;
    }

    protected double calculateMyKickDamagePotential(MovePath path, Entity enemy, IGame game) {
        PhysicalInfo myKick = new PhysicalInfo(path.getEntity(),
                new EntityState(path), enemy, null, PhysicalAttackType.RIGHT_KICK, game, owner);
        if (myKick.prob_to_hit <= 0.5) {
            return 0;
        }
        return myKick.expected_damage_on_hit * myKick.prob_to_hit;
    }

    protected EntityEvaluationResponse evaluateMovedEnemy(Entity enemy, MovePath path, IGame game) {

        EntityEvaluationResponse returnResponse = new EntityEvaluationResponse();

        // How much damage can they do to me?
        double theirDamagePotential = calculateDamagePotential(enemy, null, path, game);

        // if they can kick me, and probably hit, they probably will.
        theirDamagePotential += calculateKickDamagePotential(enemy, path, game);
        returnResponse.setEstimatedEnemyDamage(theirDamagePotential);

        // How much damage can I do to them?
        returnResponse.setMyEstimatedDamage(calculateMyDamagePotential(path, enemy, game));

        // How much physical damage can I do to them?
        returnResponse.setMyEstimatedPhysicalDamage(calculateMyKickDamagePotential(path, enemy, game));

        return returnResponse;
    }

    // The further I am from a target, the lower this path ranks (weighted by Hyper Aggression.
    private double calculateAggreesionMod(Entity movingUnit, MovePath path, IGame game, StringBuilder formula) {
        double distToEnemy = distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game);
        if ((distToEnemy == 0) && !(movingUnit instanceof Infantry)) {
            distToEnemy = 2;
        }
        double aggression = getOwner().getBehaviorSettings().getHyperAggressionValue();
        double aggressionMod = distToEnemy * aggression;
        formula.append(" - aggressionMod [").append(LOG_DECIMAL.format(aggressionMod)).append(" = ")
                .append(LOG_DECIMAL.format(distToEnemy)).append(" * ").append(LOG_DECIMAL.format(aggression))
                .append("]");
        return aggressionMod;
    }

    // The further I am from my teammates, the lower this path ranks (weighted by Herd Mentality).
    private double calculateHerdingMod(Coords friendsCoords, MovePath path, StringBuilder formula) {
        if (friendsCoords == null) {
            formula.append(" - herdingMod [0 no friends]");
            return 0;
        }

        double distanceToAllies = friendsCoords.distance(path.getFinalCoords());
        double herding = getOwner().getBehaviorSettings().getHerdMentalityValue();
        double herdingMod = distanceToAllies * herding;
        formula.append(" - herdingMod [").append(LOG_DECIMAL.format(herdingMod)).append(" = ")
                .append(LOG_DECIMAL.format(distanceToAllies)).append(" * ").append(LOG_DECIMAL.format(herding))
                .append("]");
        return herdingMod;
    }

    // todo account for damaged locations and face those away from enemy.
    private double calculateFacingMod(Entity movingUnit, IGame game, final MovePath path, StringBuilder formula) {
        final String METHOD_NAME = "calculateFacingMod(Entity, IGame, MovePath, StringBuilder)";

        Entity closest = findClosestEnemy(movingUnit, movingUnit.getPosition(), game);
        Coords toFace = closest == null ?
                game.getBoard().getCenter() :
                closest.getPosition();
        int desiredFacing = (toFace.direction(movingUnit.getPosition()) + 3) % 6;
        int currentFacing = path.getFinalFacing();
        int facingDiff;
        if (currentFacing == desiredFacing) {
            facingDiff = 0;
        } else if ((currentFacing == ((desiredFacing + 1) % 6))
                || (currentFacing == ((desiredFacing + 5) % 6))) {
            facingDiff = 1;
        } else if ((currentFacing == ((desiredFacing + 2) % 6))
                || (currentFacing == ((desiredFacing + 4) % 6))) {
            facingDiff = 2;
        } else {
            facingDiff = 3;
        }
        double facingMod = Math.max(0.0, 50 * (facingDiff - 1));
        formula.append(" - facingMod [").append(LOG_DECIMAL.format(facingMod)).append(" = max(")
                .append(LOG_INT.format(0)).append(", ").append(LOG_INT.format(50)).append(" * {")
                .append(LOG_INT.format(facingDiff)).append(" - ").append(LOG_INT.format(1)).append("})]");
        return facingMod;
    }

    // If I need to flee the board, I want to get closer to my home edge.
    private double calculateSelfPreservationMod(Entity movingUnit, MovePath path, IGame game, StringBuilder formula) {
        if (getOwner().wantsToFlee(movingUnit)) {
            int newDistanceToHome = distanceToHomeEdge(path.getFinalCoords(), getOwner().getHomeEdge(), game);
            double selfPreservation = getOwner().getBehaviorSettings().getSelfPreservationValue();
            double selfPreservationMod = newDistanceToHome * selfPreservation;
            formula.append(" - selfPreservationMod [").append(LOG_DECIMAL.format(selfPreservationMod))
                    .append(" = ").append(LOG_DECIMAL.format(newDistanceToHome)).append(" * ")
                    .append(LOG_DECIMAL.format(selfPreservation)).append("]");
            return selfPreservationMod;
        }
        return 0.0;
    }

    /**
     * A path ranking
     */
    @Override
    public RankedPath rankPath(MovePath path, IGame game, int maxRange, double fallTolerance, int distanceHome,
                               List<Entity> enemies, Coords friendsCoords) {
        final String METHOD_NAME = "rankPath(MovePath, IGame, Targetable, int, double, int, int, List<Entity>, Coords)";

        owner.methodBegin(getClass(), METHOD_NAME);

        Entity movingUnit = path.getEntity();
        StringBuilder formula = new StringBuilder("Calculation: {");

        try {

            if (movingUnit instanceof Aero || movingUnit instanceof VTOL) {
                RankedPath aeroRankedPath = doAeroSpecificRanking(path, (movingUnit instanceof VTOL));
                if (aeroRankedPath != null) {
                    return aeroRankedPath;
                }
            }

            // Copy the path to avoid inadvertent changes.
            MovePath pathCopy = path.clone();

            // Worry about failed piloting rolls (weighted by Fall Shame).
            double successProbability = getMovePathSuccessProbability(pathCopy);
            double utility = -calculateFallMod(successProbability, formula);

            // look at all of my enemies
            double maximumDamageDone = 0;
            double maximumPhysicalDamage = 0;
            double expectedDamageTaken = 0;
            for (Entity enemy : enemies) {

                // Skip units not actually on the board.
                if (enemy.isOffBoard() || (enemy.getPosition() == null)
                        || !game.getBoard().contains(enemy.getPosition())) {
                    continue;
                }

                EntityEvaluationResponse eval;
                if ((!enemy.isSelectableThisTurn()) || enemy.isImmobile()) { //For units that have already moved
                    eval = evaluateMovedEnemy(enemy, pathCopy, game);
                } else { //for units that have not moved this round
                    eval = evaluateUnmovedEnemy(enemy, path);
                }
                if (maximumDamageDone < eval.getMyEstimatedDamage()) {
                    maximumDamageDone = eval.getMyEstimatedDamage();
                }
                if (maximumPhysicalDamage < eval.getMyEstimatedPhysicalDamage()) {
                    maximumPhysicalDamage = eval.getMyEstimatedPhysicalDamage();
                }
                expectedDamageTaken += eval.getEstimatedEnemyDamage();
            }

            // Include damage I can do to strategic targets
            for (int i = 0; i < getOwner().getFireControl().getAdditionalTargets().size(); i++) {
                Targetable target = getOwner().getFireControl().getAdditionalTargets().get(i);
                if (target.isOffBoard() || (target.getPosition() == null)
                        || !game.getBoard().contains(target.getPosition())) {
                    continue; // Skip targets not actually on the board.
                }
                FiringPlan myFiringPlan = fireControl.guessBestFiringPlanWithTwists(path.getEntity(),
                        new EntityState(path), target, null, game);
                double myDamagePotential = myFiringPlan.utility;
                if (myDamagePotential > maximumDamageDone) {
                    maximumDamageDone = myDamagePotential;
                }
                PhysicalInfo myKick = new PhysicalInfo(
                        path.getEntity(), new EntityState(path), target, null,
                        PhysicalAttackType.RIGHT_KICK, game, owner);
                double expectedKickDamage = myKick.expected_damage_on_hit * myKick.prob_to_hit;
                if (expectedKickDamage > maximumPhysicalDamage) {
                    maximumPhysicalDamage = expectedKickDamage;
                }
            }

            // If I cannot kick because I am a clan unit and "No physical attacks for the clans"
            // is enabled, set maximum physical damage for this path to zero.
            if (game.getOptions().booleanOption("no_clan_physical") && path.getEntity().isClan()) {
                maximumPhysicalDamage = 0;
            }

            // I can kick a different target than I shoot, so add physical to total
            // damage after I've looked at all enemies
            maximumDamageDone += maximumPhysicalDamage;

            // My bravery modifier is based on my chance of getting to the firing position (successProbability),
            // how much damage I can do (weighted by bravery), less the damage I might take.
            double braveryValue = getOwner().getBehaviorSettings().getBraveryValue();
            double braveryMod = successProbability * ((maximumDamageDone * braveryValue) - expectedDamageTaken);
            formula.append(" + braveryMod [").append(LOG_DECIMAL.format(braveryMod)).append(" = ")
                    .append(LOG_PERCENT.format(successProbability)).append(" * ((")
                    .append(LOG_DECIMAL.format(maximumDamageDone)).append(" * ")
                    .append(LOG_DECIMAL.format(braveryValue)).append(") - ")
                    .append(LOG_DECIMAL.format(expectedDamageTaken)).append("]");
            utility += braveryMod;

            //noinspection StatementWithEmptyBody
            if (path.getEntity() instanceof Aero) {
                // No idea what original implementation was meant to be.

            } else {

                // The further I am from a target, the lower this path ranks (weighted by Hyper Aggression.
                utility -= calculateAggreesionMod(movingUnit, pathCopy, game, formula);

                // The further I am from my teammates, the lower this path ranks (weighted by Herd Mentality).
                utility -= calculateHerdingMod(friendsCoords, pathCopy, formula);
            }

            // Try to face the enemy.
            double facingMod = calculateFacingMod(movingUnit, game, pathCopy, formula);
            if (facingMod == -10000) {
                return new RankedPath(facingMod, pathCopy, formula.toString());
            }
            utility -= facingMod;

            // If I need to flee the board, I want to get closer to my home edge.
            utility -= calculateSelfPreservationMod(movingUnit, pathCopy, game, formula);

            return new RankedPath(utility, pathCopy, formula.toString());
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }

    }

    @Override
    Entity findClosestEnemy(Entity me, Coords position, IGame game) {
        return super.findClosestEnemy(me, position, game);
    }

    /**
     * Calculate who all other units would shoot at if I weren't around
     */
    @Override
    public void initUnitTurn(Entity unit, IGame game) {
        final String METHOD_NAME = "initUnitTurn(Entity, IGame)";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            bestDamageByEnemies.clear();
            List<Entity> enemies = getEnemies(unit, game);
            ArrayList<Entity> friends = getFriends(unit, game);
            for (Entity e : enemies) {
                double max_damage = 0;
                for (Entity f : friends) {
                    double damage = fireControl
                            .guessBestFiringPlanUnderHeatWithTwists(e, null, f,
                                    null, (e.getHeatCapacity() - e.heat) + 5, game)
                            .getExpectedDamage();
                    if (damage > max_damage) {
                        max_damage = damage;
                    }

                }
                bestDamageByEnemies.put(e.getId(), max_damage);
            }
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Gives the distance to the closest friendly unit, or zero if none exist
     */
//    public double distanceToClosestFriend(MovePath p, IGame game) {
//        final String METHOD_NAME = "distanceToClosestFriend(MovePath, IGame)";
//        owner.methodBegin(getClass(), METHOD_NAME);
//
//        try {
//            Entity closest = findClosestFriend(p, game);
//            if (closest == null) {
//                return 0;
//            }
//            return closest.getPosition().distance(p.getFinalCoords());
//        } finally {
//            owner.methodEnd(getClass(), METHOD_NAME);
//        }
//    }

    /**
     * Gives the distance to the closest enemy unit, or zero if none exist
     *
     * @param me       Entity who has enemies
     * @param position Coords from which the closest enemy is found
     * @param game     IGame that we're playing
     */
    @Override
    public double distanceToClosestEnemy(Entity me, Coords position, IGame game) {
        final String METHOD_NAME = "distanceToClosestEnemy(Entity, Coords, IGame)";
        owner.methodBegin(BasicPathRanker.class, METHOD_NAME);

        try {
            Entity closest = findClosestEnemy(me, position, game);
            if (closest == null) {
                return 0;
            }
            return closest.getPosition().distance(position);
        } finally {
            owner.methodEnd(BasicPathRanker.class, METHOD_NAME);
        }
    }

    /**
     * Gives the distance to the closest edge
     */
    public int distanceToClosestEdge(Coords position, IGame game) {
        final String METHOD_NAME = "distanceToClosestEdge(Coords, IGame)";
        owner.methodBegin(BasicPathRanker.class, METHOD_NAME);

        try {
            int width = game.getBoard().getWidth();
            int height = game.getBoard().getHeight();
            int minimum = position.x;
            if ((width - position.x) < minimum) {
                minimum = position.x;
            }
            if (position.y < minimum) {
                minimum = position.y;
            }
            if ((height - position.y) < minimum) {
                minimum = height - position.y;
            }
            return minimum;
        } finally {
            owner.methodEnd(BasicPathRanker.class, METHOD_NAME);
        }
    }

    /**
     * Returns distance to the unit's home edge.
     * Gives the distance to the closest edge
     *
     * @param position Final coordinates of the proposed move.
     * @param homeEdge Unit's home edge.
     * @param game
     * @return The distance to the unit's home edge.
     */
    public int distanceToHomeEdge(Coords position, HomeEdge homeEdge, IGame game) {
        final String METHOD_NAME = "distanceToHomeEdge(Coords, HomeEdge, IGame)";
        owner.methodBegin(BasicPathRanker.class, METHOD_NAME);

        try {
            String msg = "Getting distance to home edge: " + homeEdge.toString();

            int width = game.getBoard().getWidth();
            int height = game.getBoard().getHeight();

            int distance;
            switch (homeEdge) {
                case NORTH: {
                    distance = position.y;
                    break;
                }
                case SOUTH: {
                    distance = height - position.y - 1;
                    break;
                }
                case WEST: {
                    distance = position.x;
                    break;
                }
                case EAST: {
                    distance = width - position.x - 1;
                    break;
                }
                default: {
                    owner.log(getClass(), METHOD_NAME, LogLevel.WARNING, "Invalid home edge.  Defaulting to NORTH.");
                    distance = position.y;
                }
            }

            msg += " -> " + distance;
            owner.log(BasicPathRanker.class, METHOD_NAME, msg);
            return distance;
        } finally {
            owner.methodEnd(BasicPathRanker.class, METHOD_NAME);
        }
    }
}
