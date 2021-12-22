/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import megamek.client.bot.princess.BotGeometry.ConvexBoardArea;
import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.client.bot.princess.BotGeometry.HexLine;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.common.*;
import megamek.common.options.OptionsConstants;
import org.apache.logging.log4j.LogManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;

/**
 * A very "basic" pathranker
 */
public class BasicPathRanker extends PathRanker implements IPathRanker {

    // this is a value used to indicate how much we value the unit being at its destination
    private final int ARRIVED_AT_DESTINATION_FACTOR = 250;
    
    // this is a value used to indicate how much we dis-value the unit being destroyed as a result of
    // what it's doing
    private final int UNIT_DESTRUCTION_FACTOR = 1000;
    
    protected final DecimalFormat LOG_DECIMAL =
            new DecimalFormat("0.00", DecimalFormatSymbols.getInstance());
    private final NumberFormat LOG_INT = NumberFormat.getIntegerInstance();
    protected final NumberFormat LOG_PERCENT = NumberFormat.getPercentInstance();

    private PathEnumerator pathEnumerator;

    // the best damage enemies could expect were I not here. Used to determine 
    // whether they will target me.
    private Map<Integer, Double> bestDamageByEnemies;
    
    public BasicPathRanker(Princess owningPrincess) {
        super(owningPrincess);
        
        bestDamageByEnemies = new TreeMap<>();
        
        LogManager.getLogger().debug("Using " + getOwner().getBehaviorSettings().getDescription()
                        + " behavior");
    }
    
    FireControl getFireControl(Entity entity) {
        return getOwner().getFireControl(entity);
    }

    void setPathEnumerator(PathEnumerator pathEnumerator) {
        this.pathEnumerator = pathEnumerator;
    }
    
    PathEnumerator getPathEnumerator() {
        return pathEnumerator;
    }

    Map<Integer, Double> getBestDamageByEnemies() {
        return bestDamageByEnemies;
    }

    Coords getClosestCoordsTo(int unitId, Coords location) {
        ConvexBoardArea box = pathEnumerator.getUnitMovableAreas().get(unitId);
        if (box == null) {
            return null;
        }
        return box.getClosestCoordsTo(location);
    }

    boolean isInMyLoS(Entity unit, HexLine leftBounds, HexLine rightBounds) {
        return (leftBounds.judgeArea(pathEnumerator.getUnitMovableAreas().get(unit.getId())) > 0)
               && (rightBounds.judgeArea(pathEnumerator.getUnitMovableAreas().get(unit.getId())) < 0);
    }

    double getMaxDamageAtRange(FireControl fireControl, Entity shooter,
                               int range, boolean useExtremeRange,
                               boolean useLOSRange) {
        return fireControl.getMaxDamageAtRange(shooter, range, useExtremeRange,
                                               useLOSRange);
    }

    boolean canFlankAndKick(Entity enemy, Coords behind, Coords leftFlank,
                            Coords rightFlank, int myFacing) {
        Set<CoordFacingCombo> enemyFacingSet =
                pathEnumerator.getUnitPotentialLocations().get(enemy.getId());
        if (enemyFacingSet == null) {
            LogManager.getLogger().warn("no facing set for " + enemy.getDisplayName());
            return false;
        }
        return enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(behind, myFacing))
               || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(behind, (myFacing + 1) % 6))
               || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(behind, (myFacing + 5) % 6))
               || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(leftFlank, myFacing))
               || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(leftFlank, (myFacing + 4) % 6))
               || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(leftFlank, (myFacing + 5) % 6))
               || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(rightFlank, myFacing))
               || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(rightFlank, (myFacing + 1) % 6))
               || enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(rightFlank, (myFacing + 2) % 6));
    }

    /**
     * Guesses a number of things about an enemy that has not yet moved
     * TODO estimated damage is sloppy.  Improve for missile attacks, gun skill, and range
     */
    EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path,
                                                  boolean useExtremeRange,
                                                  boolean useLOSRange) {
        //some preliminary calculations
        final double damageDiscount = 0.25;
        EntityEvaluationResponse returnResponse =
                new EntityEvaluationResponse();

        //Airborne aeros on ground maps always move after other units, and would require an 
        // entirely different evaluation
        //TODO (low priority) implement a way to see if I can dodge aero units
        if (enemy.isAirborneAeroOnGroundMap()) {
            return returnResponse;
        }
        
        Coords finalCoords = path.getFinalCoords();
        int myFacing = path.getFinalFacing();
        Coords behind = finalCoords.translated((myFacing + 3) % 6);
        Coords leftFlank = finalCoords.translated((myFacing + 2) % 6);
        Coords rightFlank = finalCoords.translated((myFacing + 4) % 6);
        Coords closest = getClosestCoordsTo(enemy.getId(), finalCoords);
        if (closest == null) {
            return returnResponse;
        }
        int range = closest.distance(finalCoords);

        // I would prefer if the enemy must end its move in my line of fire 
        // if so, I can guess that I may do some damage to it (cover 
        // notwithstanding).  At the very least, I can force the enemy to 
        // take cover on its move.
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
            returnResponse.addToMyEstimatedDamage(
                    getMaxDamageAtRange(getFireControl(path.getEntity()),
                                        path.getEntity(),
                                        range,
                                        useExtremeRange,
                                        useLOSRange) * damageDiscount);
        }

        //in general if an enemy can end its position in range, it can hit me
        returnResponse.addToEstimatedEnemyDamage(
                getMaxDamageAtRange(getFireControl(enemy),
                                    enemy,
                                    range,
                                    useExtremeRange,
                                    useLOSRange)
                                                 * damageDiscount);

        //It is especially embarrassing if the enemy can move behind or flank me and then kick me
        if (canFlankAndKick(enemy, behind, leftFlank, rightFlank, myFacing)) {
            returnResponse.addToEstimatedEnemyDamage(
                    Math.ceil(enemy.getWeight() / 5.0) *
                    damageDiscount);
        }
        
        return returnResponse;
    }

    @Override
    protected List<TargetRoll> getPSRList(MovePath path) {
        return super.getPSRList(path);
    }

    @Override
    public double getMovePathSuccessProbability(MovePath movePath,
                                                StringBuilder msg) {
        return super.getMovePathSuccessProbability(movePath, msg);
    }

    private double calculateFallMod(double successProbability,
                                    StringBuilder formula) {
        double pilotingFailure = (1 - successProbability);
        double fallShame = getOwner().getBehaviorSettings().getFallShameValue();
        double fallMod = pilotingFailure * (pilotingFailure == 1 ? -UNIT_DESTRUCTION_FACTOR : fallShame);
        formula.append("fall mod [").append(LOG_DECIMAL.format(fallMod)).append(" = ")
               .append(LOG_DECIMAL.format(pilotingFailure)).append(" * ").append(LOG_DECIMAL.format(fallShame))
               .append("]");
        return fallMod;
    }

    double calculateDamagePotential(Entity enemy,
                                    EntityState shooterState,
                                    MovePath path,
                                    EntityState targetState,
                                    int distance,
                                    Game game) {

        // If they don't have the range, they can't do damage.
        int maxRange = getOwner().getMaxWeaponRange(enemy, path.getEntity().isAirborne());
        if (distance > maxRange) {
            return 0;
        }

        // If they don't have LoS, they can't do damage.
        final LosEffects losEffects = LosEffects.calculateLOS(game, enemy, path.getEntity(),
                shooterState.getPosition(), targetState.getPosition(), false);

        if (!losEffects.canSee()) {
            return 0;
        }
        
        Targetable actualTarget = path.getEntity();
        
        // if the target is infantry protected by a building, we have to fire at the building instead. 
        if (losEffects.infantryProtected()) {
            actualTarget = new BuildingTarget(targetState.getPosition(), game.getBoard(), false);
            targetState = new EntityState(actualTarget);            
        }

        int maxHeat = (enemy.getHeatCapacity() - enemy.heat) + (enemy.isAero() ? 0 : 5);
        FiringPlanCalculationParameters guess =
                new FiringPlanCalculationParameters.Builder()
                        .buildGuess(enemy,
                                    shooterState,
                                    actualTarget,
                                    targetState,
                                    maxHeat,
                                    null);
        return getFireControl(path.getEntity()).determineBestFiringPlan(guess).getUtility();
    }

    double calculateKickDamagePotential(Entity enemy, MovePath path, Game game) {
        if (!(enemy instanceof Mech)) {
            return 0.0;
        }

        // if they can kick me, and probably hit, they probably will.
        PhysicalInfo theirKick = new PhysicalInfo(enemy, null,
                path.getEntity(), new EntityState(path),
                PhysicalAttackType.RIGHT_KICK, game, getOwner(), true);

        if (theirKick.getProbabilityToHit() <= 0.5) {
            return 0.0;
        }
        return theirKick.getExpectedDamageOnHit() * theirKick.getProbabilityToHit();
    }

    double calculateMyDamagePotential(MovePath path, Entity enemy,
                                      int distance, Game game) {
        Entity me = path.getEntity();

        // If I don't have range, I can't do damage.
        // exception: I might, if I'm an aero on a ground map attacking a ground unit because aero unit ranges are a "special case"
        boolean aeroAttackingGroundUnitOnGroundMap = me.isAirborne() && !enemy.isAero() && game.getBoard().onGround();

        int maxRange = getOwner().getMaxWeaponRange(me, enemy.isAirborne());
        if (distance > maxRange && !aeroAttackingGroundUnitOnGroundMap) {
            return 0;
        }

        // If I don't have LoS, I can't do damage.  ToDo: Account for indirect fire.
        LosEffects losEffects = LosEffects.calculateLOS(game, me, enemy, path.getFinalCoords(),
                enemy.getPosition(), false);
        if (!losEffects.canSee()) {
            return 0;
        }

        // If I am an infantry unit that cannot both move and fire, and I am 
        // moving, I can't do damage.
        boolean isZeroMpInfantry = me instanceof Infantry && (me.getWalkMP() == 0);
        if (isZeroMpInfantry && path.getMpUsed() > 0) {
            return 0;
        }

        FiringPlan myFiringPlan;
        // we're only going to do air to ground attack plans if we're an airborne aero attacking a ground unit
        if (aeroAttackingGroundUnitOnGroundMap) {
            myFiringPlan = getFireControl(path.getEntity()).guessFullAirToGroundPlan(me, enemy,
                                                                     new EntityState(enemy), path, game, false);
        } else {
            FiringPlanCalculationParameters guess =
                    new FiringPlanCalculationParameters.Builder()
                            .buildGuess(path.getEntity(),
                                        new EntityState(path),
                                        enemy,
                                        null,
                                        getFireControl(me).calcHeatTolerance(me, me.isAero()),
                                        null);
            myFiringPlan = getFireControl(me).determineBestFiringPlan(guess);
        }
        return myFiringPlan.getUtility();
    }

    double calculateMyKickDamagePotential(MovePath path, Entity enemy,
                                          Game game) {
        if (!(path.getEntity() instanceof Mech)) {
            return 0.0;
        }

        PhysicalInfo myKick = new PhysicalInfo(path.getEntity(),
                new EntityState(path), enemy, null,
                PhysicalAttackType.RIGHT_KICK, game, getOwner(), true);
        if (myKick.getProbabilityToHit() <= 0.5) {
            return 0;
        }
        return myKick.getExpectedDamageOnHit() * myKick.getProbabilityToHit();
    }

    EntityEvaluationResponse evaluateMovedEnemy(Entity enemy, MovePath path,
                                                Game game) {

        EntityEvaluationResponse returnResponse = new EntityEvaluationResponse();

        int distance = enemy.getPosition().distance(path.getFinalCoords());
        
        // How much damage can they do to me?
        double theirDamagePotential = calculateDamagePotential(enemy,
                                                               new EntityState(enemy),
                                                               path,
                                                               new EntityState(path),
                                                               distance,
                                                               game);

        // if they can kick me, and probably hit, they probably will.
        if (distance <= 1) {
            theirDamagePotential += calculateKickDamagePotential(enemy, path,
                                                                 game);
        }
        returnResponse.setEstimatedEnemyDamage(theirDamagePotential);

        // How much damage can I do to them?
        returnResponse.setMyEstimatedDamage(calculateMyDamagePotential(path,
                                                                       enemy,
                                                                       distance,
                                                                       game));
       
        // How much physical damage can I do to them?
        if (distance <= 1) {
            returnResponse.setMyEstimatedPhysicalDamage(
                    calculateMyKickDamagePotential(path, enemy, game));
        }

        return returnResponse;
    }

    // The further I am from a target, the lower this path ranks (weighted by 
    // Hyper Aggression.
    protected double calculateAggressionMod(Entity movingUnit, MovePath path,
                                          Game game, StringBuilder formula) {
        double distToEnemy = distanceToClosestEnemy(movingUnit,
                                                    path.getFinalCoords(),
                                                    game);
        if ((distToEnemy == 0) && !(movingUnit instanceof Infantry)) {
            distToEnemy = 2;
        }
        double aggression = getOwner().getBehaviorSettings()
                                      .getHyperAggressionValue();
        double aggressionMod = distToEnemy * aggression;
        formula.append(" - aggressionMod [")
               .append(LOG_DECIMAL.format(aggressionMod)).append(" = ")
               .append(LOG_DECIMAL.format(distToEnemy)).append(" * ")
               .append(LOG_DECIMAL.format(aggression)).append("]");
        return aggressionMod;
    }

    // The further I am from my teammates, the lower this path ranks (weighted 
    // by Herd Mentality).
    protected double calculateHerdingMod(Coords friendsCoords, MovePath path,
                                       StringBuilder formula) {
        if (friendsCoords == null) {
            formula.append(" - herdingMod [0 no friends]");
            return 0;
        }

        double distanceToAllies = friendsCoords.distance(path.getFinalCoords());
        double herding = getOwner().getBehaviorSettings()
                                   .getHerdMentalityValue();
        double herdingMod = distanceToAllies * herding;
        formula.append(" - herdingMod [").append(LOG_DECIMAL.format(herdingMod))
               .append(" = ")
               .append(LOG_DECIMAL.format(distanceToAllies)).append(" * ")
               .append(LOG_DECIMAL.format(herding))
               .append("]");
        return herdingMod;
    }

    // todo account for damaged locations and face those away from enemy.
    private double calculateFacingMod(Entity movingUnit, Game game,
                                      final MovePath path,
                                      StringBuilder formula) {

        Targetable closest = findClosestEnemy(movingUnit, movingUnit.getPosition(),
                                          game, false);
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
        formula.append(" - facingMod [").append(LOG_DECIMAL.format(facingMod))
               .append(" = max(")
               .append(LOG_INT.format(0)).append(", ")
               .append(LOG_INT.format(50)).append(" * {")
               .append(LOG_INT.format(facingDiff)).append(" - ")
               .append(LOG_INT.format(1)).append("})]");
        return facingMod;
    }

    /**
     * If intentionally attempting to reach some board edge, favor paths that take me closer to it.
     */
    protected double calculateSelfPreservationMod(Entity movingUnit,
                                                MovePath path,
                                                Game game,
                                                StringBuilder formula) {
        BehaviorType behaviorType = getOwner().getUnitBehaviorTracker().getBehaviorType(movingUnit, getOwner()); 
        
        if (behaviorType == BehaviorType.ForcedWithdrawal ||
                behaviorType == BehaviorType.MoveToDestination) {
            int newDistanceToHome = distanceToHomeEdge(path.getFinalCoords(),
                                                       getOwner().getHomeEdge(movingUnit),
                                                       game);
            double selfPreservation = getOwner().getBehaviorSettings()
                                                .getSelfPreservationValue();
            
            double selfPreservationMod = 0;
            
            // normally, we favor being closer to the edge we're trying to get to
            if (newDistanceToHome > 0) {
                selfPreservationMod = newDistanceToHome * selfPreservation;
            // if this path gets us to the edge, we value it considerably more than we do paths that don't get us there    
            } else {
                selfPreservationMod = -ARRIVED_AT_DESTINATION_FACTOR;
            }
            
            formula.append(" - selfPreservationMod [")
                   .append(LOG_DECIMAL.format(selfPreservationMod))
                   .append(" = ").append(LOG_DECIMAL.format(newDistanceToHome))
                   .append(" * ")
                   .append(LOG_DECIMAL.format(selfPreservation)).append("]");
            return selfPreservationMod;
        }
        return 0.0;
    }
    
    /**
     * Tells me whether this path will result in me flying to a location
     * from which there is absolutely no way to remain on the board the following turn.
     * 
     * Not applicable for ground units, so the default behavior is to return 0.
     */
    protected double calculateOffBoardMod(MovePath path) {
        return 0.0;
    }

    /**
     * A path ranking
     */
    @Override
    protected RankedPath rankPath(MovePath path, Game game, int maxRange,
                               double fallTolerance,
                               List<Entity> enemies, Coords friendsCoords) {
        Entity movingUnit = path.getEntity();
        StringBuilder formula = new StringBuilder("Calculation: {");

        // Copy the path to avoid inadvertent changes.
        MovePath pathCopy = path.clone();

        // Worry about failed piloting rolls (weighted by Fall Shame).
        double successProbability = getMovePathSuccessProbability(pathCopy,
                                                                  formula);
        double utility = -calculateFallMod(successProbability, formula);

        // look at all of my enemies          
        FiringPhysicalDamage damageEstimate = new FiringPhysicalDamage();
        
        double expectedDamageTaken = checkPathForHazards(pathCopy,
                                                         movingUnit,
                                                         game);
        
        expectedDamageTaken += MinefieldUtil.checkPathForMinefieldHazards(pathCopy);
        
        boolean extremeRange = game.getOptions()
                                   .booleanOption(
                                           OptionsConstants.ADVCOMBAT_TACOPS_RANGE);
        boolean losRange = game.getOptions()
                               .booleanOption(
                                       OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE);
        for (Entity enemy : enemies) {

            // Skip ejected pilots.
            if (enemy instanceof MechWarrior) {
                continue;
            }

            // Skip units not actually on the board.
            if (enemy.isOffBoard() || (enemy.getPosition() == null)
                || !game.getBoard().contains(enemy.getPosition())) {
                continue;
            }

            //skip broken enemies
            if (getOwner().getHonorUtil().isEnemyBroken(enemy.getId(),
                    enemy.getOwnerId(), getOwner().getForcedWithdrawal())) {
                continue;
            }

            EntityEvaluationResponse eval;

            if (evaluateAsMoved(enemy)) { //For units that have already moved
                eval = evaluateMovedEnemy(enemy, pathCopy, game);
            } else { //for units that have not moved this round
                eval = evaluateUnmovedEnemy(enemy, path, extremeRange,
                                            losRange);
            }
            
            // if we're not ignoring the enemy, we consider damage that we may do to them;
            // however, just because we're ignoring them doesn't mean they won't shoot at us.
            if (!getOwner().getBehaviorSettings().getIgnoredUnitTargets().contains(enemy.getId())) {
                if (damageEstimate.firingDamage < eval.getMyEstimatedDamage()) {
                    damageEstimate.firingDamage = eval.getMyEstimatedDamage();
                }
                if (damageEstimate.physicalDamage < eval.getMyEstimatedPhysicalDamage()) {
                    damageEstimate.physicalDamage = eval.getMyEstimatedPhysicalDamage();
                }
            }
            
            expectedDamageTaken += eval.getEstimatedEnemyDamage();
        }

        // if we're not in the air, we may get hit by friendly artillery
        if (!path.getEntity().isAirborne() && !path.getEntity().isAirborneVTOLorWIGE()) {
            double friendlyArtilleryDamage = 0;
            Map<Coords, Double> artyDamage = getOwner().getPathRankerState().getIncomingFriendlyArtilleryDamage();
            
            if (!artyDamage.containsKey(path.getFinalCoords())) {
                friendlyArtilleryDamage = ArtilleryTargetingControl.evaluateIncomingArtilleryDamage(path.getFinalCoords(), getOwner());
                artyDamage.put(path.getFinalCoords(), friendlyArtilleryDamage);
            } else {
                friendlyArtilleryDamage = artyDamage.get(path.getFinalCoords());
            }
            
            expectedDamageTaken += friendlyArtilleryDamage;
        }

        calcDamageToStrategicTargets(pathCopy, game, getOwner().getFireControlState(), damageEstimate);

        // If I cannot kick because I am a clan unit and "No physical 
        // attacks for the clans"
        // is enabled, set maximum physical damage for this path to zero.
        if (game.getOptions()
                .booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL) &&
            path.getEntity().isClan()) {
            damageEstimate.physicalDamage = 0;
        }

        // I can kick a different target than I shoot, so add physical to 
        // total damage after I've looked at all enemies
        double maximumDamageDone = damageEstimate.firingDamage + damageEstimate.physicalDamage;

        // My bravery modifier is based on my chance of getting to the 
        // firing position (successProbability), how much damage I can do 
        // (weighted by bravery), less the damage I might take.
        double braveryValue =
                getOwner().getBehaviorSettings().getBraveryValue();
        double braveryMod = successProbability *
                            ((maximumDamageDone *
                              braveryValue) -
                             expectedDamageTaken);
        formula.append(" + braveryMod [")
               .append(LOG_DECIMAL.format(braveryMod)).append(" = ")
               .append(LOG_PERCENT.format(successProbability))
               .append(" * ((")
               .append(LOG_DECIMAL.format(maximumDamageDone)).append(" * ")
               .append(LOG_DECIMAL.format(braveryValue)).append(") - ")
               .append(LOG_DECIMAL.format(expectedDamageTaken)).append("]");
        utility += braveryMod;

        // the only critters not subject to aggression and herding mods are
        // airborne aeros on ground maps, as they move incredibly fast
        if (!path.getEntity().isAirborneAeroOnGroundMap()) {
            // The further I am from a target, the lower this path ranks 
            // (weighted by Aggression slider).
            utility -= calculateAggressionMod(movingUnit, pathCopy, game,
                                              formula);

            // The further I am from my teammates, the lower this path 
            // ranks (weighted by Herd Mentality).
            utility -= calculateHerdingMod(friendsCoords, pathCopy, formula);
        }

        // Try to face the enemy.
        double facingMod = calculateFacingMod(movingUnit, game, pathCopy,
                                              formula);
        if (facingMod == -10000) {
            return new RankedPath(facingMod, pathCopy, formula.toString());
        }
        utility -= facingMod;
        
        // If I need to flee the board, I want to get closer to my home edge.
        utility -= calculateSelfPreservationMod(movingUnit, pathCopy, game, formula);
        
        // if we're an aircraft, we want to de-value paths that will force us off the board
        // on the subsequent turn.
        utility -= utility * calculateOffBoardMod(pathCopy);

        RankedPath rankedPath = new RankedPath(utility, pathCopy, formula.toString());
        rankedPath.setExpectedDamage(maximumDamageDone);
        return rankedPath;
    }
    
    /**
     * Worker function that determines if a given enemy entity should be evaluated as if it has moved.
     */
    protected boolean evaluateAsMoved(Entity enemy) {
        // Aerospace units on ground maps can go pretty much anywhere they want, so it's
        // somewhat pointless to try to predict their movement.
        return !enemy.isSelectableThisTurn() || enemy.isImmobile() || enemy.isAirborneAeroOnGroundMap();
    }
    
    /**
     * Calculate who all other units would shoot at if I weren't around
     */
    @Override
    public void initUnitTurn(Entity unit, Game game) {
        bestDamageByEnemies.clear();
        List<Entity> enemies = getOwner().getEnemyEntities();
        List<Entity> friends = getOwner().getFriendEntities();
        for (Entity e : enemies) {
            double max_damage = 0;
            for (Entity f : friends) {
                FiringPlanCalculationParameters guess =
                        new FiringPlanCalculationParameters.Builder()
                                .buildGuess(e,
                                            null,
                                            f,
                                            null,
                                            (e.getHeatCapacity() - e.getHeat()) + 5,
                                            null);
                double damage = getFireControl(f).determineBestFiringPlan(guess)
                                           .getExpectedDamage();
                if (damage > max_damage) {
                    max_damage = damage;
                }

            }
            bestDamageByEnemies.put(e.getId(), max_damage);
        }
    }


    protected void calcDamageToStrategicTargets(MovePath path, Game game,
            FireControlState fireControlState, FiringPhysicalDamage damageStructure) {
                
        for (int i = 0; i < fireControlState.getAdditionalTargets().size(); i++) {
            Targetable target = fireControlState.getAdditionalTargets().get(i);
            
            if (target.isOffBoard() || (target.getPosition() == null)
                || !game.getBoard().contains(target.getPosition())) {
                continue; // Skip targets not actually on the board.
            }
            
            FiringPlanCalculationParameters guess =
                    new FiringPlanCalculationParameters.Builder()
                            .buildGuess(path.getEntity(),
                                        new EntityState(path),
                                        target,
                                        null,
                                        Entity.DOES_NOT_TRACK_HEAT,
                                        null);
            FiringPlan myFiringPlan = getFireControl(path.getEntity()).determineBestFiringPlan(guess);
            
            double myDamagePotential = myFiringPlan.getUtility();
            if (myDamagePotential > damageStructure.firingDamage) {
                damageStructure.firingDamage = myDamagePotential;
            }
            
            if (path.getEntity() instanceof Mech) {
                PhysicalInfo myKick = new PhysicalInfo(
                        path.getEntity(), new EntityState(path), target,
                        null,
                        PhysicalAttackType.RIGHT_KICK, game, getOwner(),
                        true);
                double expectedKickDamage =
                        myKick.getExpectedDamageOnHit() *
                        myKick.getProbabilityToHit();
                if (expectedKickDamage > damageStructure.physicalDamage) {
                    damageStructure.physicalDamage = expectedKickDamage;
                }
            }
        }
    }
    
    /**
     * Gives the distance to the closest enemy unit, or -1 if none exist.
     * The reason being that the closest enemy unit may be 0 away.
     *
     * @param me       Entity who has enemies
     * @param position Coords from which the closest enemy is found
     * @param game     Game that we're playing
     */
    @Override
    public double distanceToClosestEnemy(Entity me, Coords position, Game game) {
        Targetable closest = findClosestEnemy(me, position, game);
        if (closest == null) {
            return -1;
        }
        return closest.getPosition().distance(position);
    }

    /**
     * Gives the distance to the closest edge
     */
    int distanceToClosestEdge(Coords position, Game game) {
       int width = game.getBoard().getWidth();
        int height = game.getBoard().getHeight();
        int minimum = position.getX();
        if ((width - position.getX()) < minimum) {
            minimum = position.getX();
        }
        if (position.getY() < minimum) {
            minimum = position.getY();
        }
        if ((height - position.getY()) < minimum) {
            minimum = height - position.getY();
        }
        return minimum;
    }

    double checkPathForHazards(MovePath path, Entity movingUnit, Game game) {
        StringBuilder logMsg = new StringBuilder("Checking Path (")
                .append(path.toString()).append(") for hazards.");

        try {
            // If we're flying or swimming, we don't care about ground hazards.
            if (EntityMovementType.MOVE_FLYING.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_OVER_THRUST.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_SAFE_THRUST.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_VTOL_WALK.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_VTOL_RUN.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_VTOL_SPRINT.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_SUBMARINE_WALK.equals(path.getLastStepMovementType()) ||
                EntityMovementType.MOVE_SUBMARINE_RUN.equals(path.getLastStepMovementType())) {

                logMsg.append("\n\tMove Type (").append(path.getLastStepMovementType().toString())
                      .append(") ignores ground hazards.");
                return 0;
            }

            // If we're jumping, we only care about where we land.
            if (path.isJumping()) {
                logMsg.append("\n\tJumping");
                Coords endCoords = path.getFinalCoords();
                Hex endHex = game.getBoard().getHex(endCoords);
                return checkHexForHazards(endHex, movingUnit, true,
                                          path.getLastStep(), true,
                                          path, game.getBoard(), logMsg);
            }

            double totalHazard = 0;
            Coords previousCoords = null;
            MoveStep lastStep = path.getLastStep();
            for (MoveStep step : path.getStepVector()) {
                Coords coords = step.getPosition();
                if ((coords == null) || coords.equals(previousCoords)) {
                    continue;
                }
                Hex hex = game.getBoard().getHex(coords);
                totalHazard += checkHexForHazards(hex, movingUnit,
                                                  lastStep.equals(step), step,
                                                  false, path,
                                                  game.getBoard(), logMsg);
                previousCoords = coords;
            }

            return totalHazard;
        } finally {
            LogManager.getLogger().debug(logMsg.toString());
        }
    }

    private double checkHexForHazards(Hex hex, Entity movingUnit, boolean endHex, MoveStep step,
                                      boolean jumpLanding, MovePath movePath, Board board,
                                      StringBuilder logMsg) {
        logMsg.append("\n\tHex ").append(hex.getCoords().toFriendlyString());

        final List<Integer> HAZARDS =
                new ArrayList<>(Arrays.asList(Terrains.FIRE,
                                              Terrains.MAGMA,
                                              Terrains.ICE,
                                              Terrains.WATER,
                                              Terrains.BUILDING,
                                              Terrains.BRIDGE));

        int[] terrainTypes = hex.getTerrainTypes();
        Set<Integer> hazards = new HashSet<>();
        for (int type : terrainTypes) {
            if (HAZARDS.contains(type)) {
                hazards.add(type);
            }
        }

        // No hazards were found, so nothing to worry about.
        if (hazards.isEmpty()) {
            logMsg.append(" has no hazards.");
            return 0;
        }

        // Calculate hazard value by terrain type.
        double hazardValue = 0;
        for (int hazard : hazards) {
            switch (hazard) {
                case Terrains.FIRE:
                    hazardValue += calcFireHazard(movingUnit, endHex, logMsg);
                    break;
                case Terrains.MAGMA:
                    hazardValue += calcMagmaHazard(hex, endHex, movingUnit,
                                                   jumpLanding, step, logMsg);
                    break;
                case Terrains.ICE:
                    hazardValue += calcIceHazard(movingUnit, hex, step,
                                                 jumpLanding, logMsg);
                    break;
                case Terrains.WATER:
                    if (!hazards.contains(Terrains.ICE)) {
                        hazardValue += calcWaterHazard(movingUnit, hex, step,
                                                       logMsg);
                    }
                    break;
                case Terrains.BUILDING:
                    hazardValue += calcBuildingHazard(step, movingUnit, jumpLanding, board, logMsg);
                    break;
                case Terrains.BRIDGE:
                    hazardValue += calcBridgeHazard(movingUnit, hex, step, jumpLanding, board, logMsg);
                    break;
            }
        }
        
        logMsg.append("\n\tTotal Hazard = ")
              .append(LOG_DECIMAL.format(hazardValue));

        return hazardValue;
    }
    
    // Building collapse and basements are handled in PathRanker.validatePaths.
    private double calcBuildingHazard(MoveStep step, Entity movingUnit, boolean jumpLanding,
                                      Board board, StringBuilder logMsg) {
        logMsg.append("\n\tCalculating building hazard:  ");

        // Protos, BA and Infantry move through buildings freely.
        if (movingUnit instanceof Protomech || movingUnit instanceof Infantry) {
            logMsg.append("Safe for infantry and protos.");
            return 0;
        }

        // Jumping onto a building is handled in PathRanker validatePaths.
        if (jumpLanding) {
            return 0;
        }

        // Get the odds of failing the piloting roll while moving through the building.
        double odds = (1.0 - (Compute.oddsAbove(movingUnit.getCrew()
                                                          .getPiloting()) / 100));
        logMsg.append("\n\t\tChance to fail piloting roll: ")
              .append(LOG_PERCENT.format(odds));

        // Hazard is based on potential damage taken.
        double dmg = board.getBuildingAt(step.getPosition())
                          .getCurrentCF(step.getPosition()) / 10D;
        logMsg.append("\n\t\tPotential building damage: ")
              .append(LOG_DECIMAL.format(dmg));

        double hazard = dmg * odds;
        logMsg.append("\n\t\tHazard value (")
              .append(LOG_DECIMAL.format(hazard)).append(").");
        return hazard;
    }
    
    private double calcBridgeHazard(Entity movingUnit, Hex hex, MoveStep step, boolean jumpLanding,
                                    Board board, StringBuilder logMsg) {
        logMsg.append("\n\tCalculating bridge hazard:  ");
        
        // if we are going to BWONGGG into a bridge from below, then it's treated as a building.
        // Otherwise, bridge collapse checks have already been handled in validatePaths
        int bridgeElevation = hex.terrainLevel(Terrains.BRIDGE_ELEV);
        if ((bridgeElevation > step.getElevation()) &&
                (bridgeElevation <= (step.getElevation() + movingUnit.getHeight()))) {
            return calcBuildingHazard(step, movingUnit, jumpLanding, board, logMsg);
        }
        
        return 0;
    }

    private double calcIceHazard(Entity movingUnit, Hex hex, MoveStep step, boolean jumpLanding,
                                 StringBuilder logMsg) {
        logMsg.append("\n\tCalculating ice hazard:  ");

        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
            EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logMsg.append("Hovering above ice (0).");
            return 0;
        }

        // If there is no water under the ice, don't worry about breaking 
        // through.
        if (hex.depth() < 1) {
            logMsg.append("No water under ice (0).");
            return 0;
        }

        // Hazard is based on chance to break through to the water underneath.
        double breakthroughMod = jumpLanding ? 0.5 : 0.1667;
        logMsg.append("\n\t\tChance to break through ice: ")
              .append(LOG_PERCENT.format(breakthroughMod));

        double hazard = calcWaterHazard(movingUnit, hex, step, logMsg) *
                        breakthroughMod;
        logMsg.append("\n\t\tHazard value (")
              .append(LOG_DECIMAL.format(hazard)).append(").");
        return hazard;
    }

    private double calcWaterHazard(Entity movingUnit, Hex hex, MoveStep step,
                                   StringBuilder logMsg) {
        logMsg.append("\n\tCalculating water hazard:  ");

        // Puddles don't count.
        if (hex.depth() == 0) {
            logMsg.append("Puddles don't count (0).");
            return 0;
        }

        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
            EntityMovementMode.WIGE == movingUnit.getMovementMode() ||
            EntityMovementMode.NAVAL == movingUnit.getMovementMode()) {
            logMsg.append("Hovering or swimming above water (0).");
            return 0;
        }

        // Amphibious units are safe (kind of the point).
        if (movingUnit.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ||
            movingUnit.hasWorkingMisc(MiscType.F_AMPHIBIOUS)) {
            logMsg.append("Amphibious unit (0).");
            return 0;
        }

        // if we are crossing a bridge, then we'll be fine. Trust me.
        // 1. Determine bridge elevation
        // 2. If unit elevation is equal to bridge elevation, skip.
        if (hex.containsTerrain(Terrains.BRIDGE_ELEV)) {
            int bridgeElevation = hex.terrainLevel(Terrains.BRIDGE_ELEV);
            if (bridgeElevation == step.getElevation()) {
                logMsg.append("Unit (0) crossing bridge.");
                return 0;
            }
        }
        
        // Most other units are automatically destroyed.
        if (!(movingUnit instanceof Mech || movingUnit instanceof Protomech ||
              movingUnit instanceof BattleArmor)) {
            logMsg.append("Ill drown (1000).");
            return UNIT_DESTRUCTION_FACTOR;
        }

        // Unsealed unit will drown.
        if (movingUnit instanceof Mech && ((Mech) movingUnit).isIndustrial()) {
            logMsg.append("Industrial mechs drown too (1000).");
            return UNIT_DESTRUCTION_FACTOR;
        }

        // Find the submerged locations.
        Set<Integer> submergedLocations = new HashSet<>();
        for (int loc = 0; loc < movingUnit.locations(); loc++) {
            if (Mech.LOC_CLEG == loc && !(movingUnit instanceof TripodMech)) {
                continue;
            }

            if ((hex.depth() >= 2) || step.isProne() ||
                !(movingUnit instanceof Mech)) {
                submergedLocations.add(loc);
                continue;
            }

            if (Mech.LOC_RLEG == loc || Mech.LOC_LLEG == loc ||
                Mech.LOC_CLEG == loc) {
                submergedLocations.add(loc);
                continue;
            }

            if ((movingUnit instanceof QuadMech) && (Mech.LOC_RARM == loc ||
                                                     Mech.LOC_LARM == loc)) {
                submergedLocations.add(loc);
            }
        }
        logMsg.append("\n\t\tSubmerged locations: ")
              .append(submergedLocations.size());

        int hazardValue = 0;
        for (int loc : submergedLocations) {
            logMsg.append("\n\t\t\tLocation ").append(loc).append(" is ");

            // Only locations withou armor can breach in movement phase.
            if (movingUnit.getArmor(loc) > 0) {
                logMsg.append(" not breached (0).");
                continue;
            }

            // Mechs or Protomechs having a head or torso breach is deadly.  
            // For other units, any breach is deadly.
            //noinspection ConstantConditions
            if (Mech.LOC_HEAD == loc ||
                Mech.LOC_CT == loc ||
                Protomech.LOC_HEAD == loc ||
                Protomech.LOC_TORSO == loc ||
                (!(movingUnit instanceof Mech) &&
                 !(movingUnit instanceof Protomech))) {
                logMsg.append(" breached and critical (1000).");
                return UNIT_DESTRUCTION_FACTOR;
            }

            // Add 50 points per potential breach location.
            logMsg.append(" breached (50).");
            hazardValue += 50;
        }

        return hazardValue;
    }

    private double calcFireHazard(Entity movingUnit, boolean endHex,
                                  StringBuilder logMsg) {
        logMsg.append("\n\tCalculating fire hazard:  ");

        double hazardValue = 0;

        // Fireproof BA ignores fire.
        if ((movingUnit instanceof BattleArmor) &&
            ((BattleArmor) movingUnit).isFireResistant()) {
            logMsg.append("Ignored by fire resistant armor (0).");
            return 0;
        }

        // Tanks risk critical hits.
        if (movingUnit instanceof Tank) {
            logMsg.append("Possible crit on tank (25).");
            return 25;
        }

        // Protomechs risk location destruction.
        if (movingUnit instanceof Protomech) {
            logMsg.append("Possible location destruction (50).");
            return 50;
        }

        // Infantry and BA risk total destruction.
        if (movingUnit instanceof Infantry) {
            logMsg.append(("Possible unit destruction (1000)."));
            return UNIT_DESTRUCTION_FACTOR;
        }

        // If this unit tracks heat, add the heat gain to the hazard value.
        if (movingUnit.getHeatCapacity() != Entity.DOES_NOT_TRACK_HEAT) {
            hazardValue += endHex ? 5 : 2;
            logMsg.append("Heat gain (").append(hazardValue).append(").");
        }

        return hazardValue;
    }

    private double calcMagmaHazard(Hex hex, boolean endHex, Entity movingUnit,
                                   boolean jumpLanding, MoveStep step,
                                   StringBuilder logMsg) {
        logMsg.append("\n\tCalculating magma hazard:  ");

        // Hovers are unaffected.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
            EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logMsg.append("Hovering above magma (0).");
            return 0;
        }

        double hazardValue = 0;
        int magmaLevel = hex.terrainLevel(Terrains.MAGMA);

        // Liquid magma.
        if (magmaLevel == 2) {
            return calcLavaHazard(endHex, movingUnit, step, logMsg);
        } else {
            double breakThroughMod = jumpLanding ? 0.5 : 0.1667;
            logMsg.append("\n\t\tChance to break through crust = ")
                  .append(LOG_PERCENT.format(breakThroughMod));

            // Factor in the chance to break through.
            double lavalHazard = calcLavaHazard(endHex, movingUnit, step,
                                                logMsg) * breakThroughMod;
            logMsg.append("\n\t\t\tLava hazard (")
                  .append(LOG_DECIMAL.format(lavalHazard)).append(").");
            hazardValue += lavalHazard;

            // Factor in heat.
            if (movingUnit.getHeatCapacity() != Entity.DOES_NOT_TRACK_HEAT) {
                double heatMod = (endHex ? 5 : 2) * (1 - breakThroughMod);
                hazardValue += heatMod;
                logMsg.append("\n\t\tHeat gain (")
                      .append(LOG_DECIMAL.format(heatMod)).append(").");
            }
        }

        return hazardValue;
    }

    private double calcLavaHazard(boolean endHex, Entity movingUnit,
                                  MoveStep step, StringBuilder logMsg) {
        logMsg.append("\n\tCalculating laval hazard:  ");


        // Hovers are unaffected.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
            EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logMsg.append("Hovering above lava (0).");
            return 0;
        }

        // Non-mech units auto-destroyed.
        if (!(movingUnit instanceof Mech)) {
            logMsg.append("Non-mech instant destruction (1000).");
            return UNIT_DESTRUCTION_FACTOR;
        }

        double hazardValue = 0;

        // Factor in heat.
        double heat = endHex ? 10.0 : 5.0;
        hazardValue += heat;
        logMsg.append("\n\t\tHeat gain (").append(heat)
              .append(LOG_DECIMAL.format(heat)).append(").");

        // Factor in potential damage.
        double dmg;
        logMsg.append("\n\t\tDamage to ");
        if (step.isProne()) {
            dmg = 7 * movingUnit.locations();
            logMsg.append("everything [prone] (");
        } else if (movingUnit instanceof BipedMech) {
            dmg = 14;
            logMsg.append("legs (");
        } else if (movingUnit instanceof TripodMech) {
            dmg = 21;
            logMsg.append("legs (");
        } else {
            dmg = 28;
            logMsg.append("legs (");
        }
        logMsg.append(LOG_DECIMAL.format(dmg)).append(").");
        hazardValue += dmg;

        return hazardValue;
    }
    
    /**
     * Simple data structure that holds a separate firing and physical damage number.
     *
     */
    protected class FiringPhysicalDamage {
        public double firingDamage;
        public double physicalDamage;
    }
}
