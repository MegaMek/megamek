/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.client.bot.princess.coverage.Builder;
import megamek.client.bot.princess.geometry.ConvexBoardArea;
import megamek.client.bot.princess.geometry.CoordFacingCombo;
import megamek.client.bot.princess.geometry.HexLine;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.Engine;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.util.HazardousLiquidPoolUtil;
import megamek.logging.MMLogger;

/**
 * A path ranker implementation for most units that evaluates and prioritizes movement paths with the highest score.
 *
 * <p>This class evaluates potential movement paths for units controlled by Princess and assigns utility scores based
 * on tactical considerations. It ranks paths according to their overall desirability, considering multiple weighted
 * factors including:</p>
 *
 * <ul>
 *   <li><b>Fall risk</b> - Probability of failing piloting skill rolls and falling</li>
 *   <li><b>Combat effectiveness</b> - Potential damage output vs. damage taken</li>
 *   <li><b>Strategic positioning</b> - Maintaining appropriate distance to enemies</li>
 *   <li><b>Tactical advantage</b> - Facing, movement modifiers, and clustering</li>
 *   <li><b>Self-preservation</b> - Avoiding hazardous terrain and managing retreat</li>
 *   <li><b>Unit cohesion</b> - Maintaining proximity to friendly units</li>
 * </ul>
 *
 * <p>The ranker implements Princess's core movement decision-making logic for most ground units, calculating a final
 * utility score where higher values represent more desirable paths. The relative importance of different factors is
 * determined by the bot's behavior settings (aggression, bravery, herd mentality, etc.).</p>
 *
 * <p>Path evaluation also considers terrain hazards like building collapses, water, magma, ice, swamp, and other that
 * could damage or immobilize the unit.</p>
 */
public class BasicPathRanker extends PathRanker {
    private final static MMLogger logger = MMLogger.create(BasicPathRanker.class);
    public static final int FACING_MOD_MULTIPLIER = 50;

    // this is a value used to indicate how much we value the unit being at its destination
    private final int ARRIVED_AT_DESTINATION_FACTOR = 250;

    // this is a value used to indicate how much we dis-value the unit being destroyed as a result of what it's doing
    private final int UNIT_DESTRUCTION_FACTOR = 1000;

    // penalty for ending a sprint inside enemy weapon range; a sprinting unit forfeits all of its attacks,
    // so this must outweigh the aggression/bravery incentives that would otherwise favor closing distance
    private final int SPRINT_INTO_THREAT_PENALTY = 250;

    private final FacingDiffCalculator facingDiffCalculator;
    private final UnitsMedianCoordinateCalculator unitsMedianCoordinateCalculator;
    protected final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
    protected final NumberFormat LOG_PERCENT = NumberFormat.getPercentInstance();

    private PathEnumerator pathEnumerator;

    // The best damage enemies could expect were I not here. Used to determine whether they would focus on me.
    protected final Map<Integer, Double> bestDamageByEnemies;

    protected int blackIce = -1;

    // Per-spotter memory of the last priority target it positioned for, keyed by the spotter's entity id. Drives the
    // TAG-spotter positioning hysteresis so it keeps painting one high-value target instead of flipping each turn.
    private final Map<Integer, Integer> lastSpotterPriorityTarget = new HashMap<>();

    public BasicPathRanker(Princess owningPrincess) {
        super(owningPrincess);
        bestDamageByEnemies = new TreeMap<>();
        BehaviorSettings behaviorSettings = getOwner().getBehaviorSettings();
        this.unitsMedianCoordinateCalculator = new UnitsMedianCoordinateCalculator(behaviorSettings.getNumberOfEnemiesToConsiderFacing());
        this.facingDiffCalculator = new FacingDiffCalculator(behaviorSettings.getAllowFacingTolerance());
        logger.debug("Using {} behavior.", getOwner().getBehaviorSettings().getDescription());
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
        return (leftBounds.judgeArea(pathEnumerator.getUnitMovableAreas().get(unit.getId())) > 0) &&
              (rightBounds.judgeArea(pathEnumerator.getUnitMovableAreas().get(unit.getId())) < 0);
    }

    /**
     * Determine max damage at range
     *
     * @param shooter         {@link Entity} that is shooting
     * @param range           Range to target
     * @param useExtremeRange Whether to use Extreme Range
     * @param useLOSRange     Line Of Sight Range
     *
     * @return Max damage at range.
     */
    double getMaxDamageAtRange(Entity shooter, int range, boolean useExtremeRange, boolean useLOSRange) {
        // It's a static class with a static method. Call directly, no need to pass.
        return FireControl.getMaxDamageAtRange(shooter, range, useExtremeRange, useLOSRange);
    }

    boolean canFlankAndKick(Entity enemy, Coords behind, Coords leftFlank, Coords rightFlank, int myFacing) {
        Set<CoordFacingCombo> enemyFacingSet = pathEnumerator.getUnitPotentialLocations().get(enemy.getId());

        if (enemyFacingSet == null) {
            logger.debug("no facing set for {}", enemy.getDisplayName());
            return false;
        }

        return enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(behind, myFacing)) ||
              enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(behind, (myFacing + 1) % 6)) ||
              enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(behind, (myFacing + 5) % 6)) ||
              enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(leftFlank, myFacing)) ||
              enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(leftFlank, (myFacing + 4) % 6)) ||
              enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(leftFlank, (myFacing + 5) % 6)) ||
              enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(rightFlank, myFacing)) ||
              enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(rightFlank, (myFacing + 1) % 6)) ||
              enemyFacingSet.contains(CoordFacingCombo.createCoordFacingCombo(rightFlank, (myFacing + 2) % 6));
    }

    /**
     * Guesses a number of things about an enemy that has not yet moved
     * TODO estimated damage is sloppy. Improve for missile attacks, gun skill, and
     * range
     */
    EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path, boolean useExtremeRange,
          boolean useLOSRange) {
        // some preliminary calculations
        final double damageDiscount = 0.25;
        EntityEvaluationResponse returnResponse = new EntityEvaluationResponse();

        // Airborne AeroSpace on ground maps always move after other units, and would require an entirely different
        // evaluation
        // TODO (low priority) implement a way to see if I can dodge aero units
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

        // I would prefer if the enemy must end its move in my line of fire if so, I can
        // guess that I may do some damage to it (cover notwithstanding). At the very
        // least, I can force the enemy to take cover on its move.
        HexLine leftBounds;
        HexLine rightBounds;

        if (path.getEntity().canChangeSecondaryFacing()) {
            leftBounds = new HexLine(behind, (myFacing + 2) % 6);
            rightBounds = new HexLine(behind, (myFacing + 4) % 6);
        } else {
            leftBounds = new HexLine(behind, (myFacing + 1) % 6);
            rightBounds = new HexLine(behind, (myFacing + 5) % 6);
        }

        // A unit that sprinted may not make any deliberate attacks this turn, so a sprinting
        // path contributes no damage of mine no matter who ends up in my line of fire.
        if (!isSprintingPath(path) && isInMyLoS(enemy, leftBounds, rightBounds)) {
            returnResponse.addToMyEstimatedDamage(getMaxDamageAtRange(path.getEntity(),
                  range,
                  useExtremeRange,
                  useLOSRange) * damageDiscount);
        }

        // in general, if an enemy can end its position in range, it can hit me
        returnResponse.addToEstimatedEnemyDamage(getMaxDamageAtRange(enemy, range, useExtremeRange, useLOSRange) *
              damageDiscount);

        // It is especially embarrassing if the enemy can move behind or flank me and then kick me
        if (canFlankAndKick(enemy, behind, leftFlank, rightFlank, myFacing)) {
            returnResponse.addToEstimatedEnemyDamage(Math.ceil(enemy.getWeight() / 5.0) * damageDiscount);
        }

        return returnResponse;
    }

    @Override
    protected List<TargetRoll> getPSRList(MovePath path) {
        return super.getPSRList(path);
    }

    /**
     * Calculates a penalty modifier for paths with a risk of causing the unit to fall over.
     * <p>
     * This method evaluates the risk associated with a movement path based on the probability of failing any required
     * piloting skill rolls (PSRs). The penalty is calculated by multiplying the probability of failure by a "shame
     * factor" that represents how much the AI dislikes falling.
     * <p>
     * The penalty formula is:
     * <pre>
     *    penalty = pilotingFailureChance * (guaranteed failure ? UNIT_DESTRUCTION_FACTOR : fallShameValue)
     * </pre>
     * <p>
     * This creates a graduated response where:
     * <ul>
     *   <li>Paths with no chance of falling have no penalty (0)</li>
     *   <li>Paths with some chance of falling have a proportional penalty</li>
     *   <li>Paths with guaranteed falling are heavily penalized (treated as potential unit destruction)</li>
     * </ul>
     * <p>
     * The method accounts for the AI's risk tolerance through the fallShame setting - higher values
     * make the AI more cautious about potential falls.
     *
     * @param successProbability The probability (0.0 to 1.0) that the unit will successfully complete all piloting
     *                           skill rolls required by the movement path
     *
     * @return The calculated fall penalty value to be subtracted from the path's utility
     */
    private double calculateFallMod(double successProbability) {
        double pilotingFailure = (1 - successProbability);
        double fallShame = getOwner().getBehaviorSettings().getFallShameValue();
        double fallMod = pilotingFailure * (pilotingFailure == 1 ? UNIT_DESTRUCTION_FACTOR : fallShame);
        logger.trace("fall mod [{} = {} * {}]", fallMod, pilotingFailure, fallShame);
        return fallMod;
    }

    double calculateDamagePotential(Entity enemy, EntityState shooterState, MovePath path, EntityState targetState,
          int distance, Game game) {

        // If they don't have the range, they can't do damage.
        int maxRange = getOwner().getMaxWeaponRange(enemy, path.getEntity().isAirborne());
        if (distance > maxRange) {
            return 0;
        }

        // If they don't have LoS, they can't do damage.
        final LosEffects losEffects = LosEffects.calculateLOS(game,
              enemy,
              path.getEntity(),
              shooterState.getPosition(),
              targetState.getPosition(),
              false);

        if (!losEffects.canSee()) {
            return 0;
        }

        Targetable actualTarget = path.getEntity();

        // if the target is infantry protected by a building, we have to fire at the
        // building instead.
        if (losEffects.infantryProtected()) {
            actualTarget = new BuildingTarget(targetState.getPosition(), game.getBoard(enemy), false);
            targetState = new EntityState(actualTarget);
        }

        int maxHeat = (enemy.getHeatCapacity() - enemy.heat) + (enemy.isAero() ? 0 : 5);
        FiringPlanCalculationParameters guess = new Builder().buildGuess(enemy,
              shooterState,
              actualTarget,
              targetState,
              maxHeat,
              null);
        return getFireControl(path.getEntity()).determineBestFiringPlan(guess).getUtility();
    }

    double calculateKickDamagePotential(Entity enemy, MovePath path, Game game) {
        if (!(enemy instanceof Mek)) {
            return 0.0;
        }

        // if they can kick me, and probably hit, they probably will.
        PhysicalInfo theirKick = new PhysicalInfo(enemy,
              null,
              path.getEntity(),
              new EntityState(path),
              PhysicalAttackType.RIGHT_KICK,
              game,
              getOwner(),
              true);

        if (theirKick.getProbabilityToHit() <= 0.5) {
            return 0.0;
        }

        return theirKick.getExpectedDamageOnHit() * theirKick.getProbabilityToHit();
    }

    double calculateMyDamagePotential(MovePath path, Entity enemy, int distance, Game game) {
        Entity me = path.getEntity();

        // If I don't have a range, I can't do damage.
        // exception: I might, if I'm an aero on a ground map attacking a ground unit
        // because aero unit ranges are a "special case"
        boolean aeroAttackingGroundUnitOnGroundMap = me.isAirborne() && !enemy.isAero() && game.getBoard(enemy)
              .isGround();

        int maxRange = getOwner().getMaxWeaponRange(me, enemy.isAirborne());
        if (distance > maxRange && !aeroAttackingGroundUnitOnGroundMap) {
            return 0;
        }

        // If I don't have LoS, I can't do damage. ToDo: Account for indirect fire.
        LosEffects losEffects = LosEffects.calculateLOS(game,
              me,
              enemy,
              path.getFinalCoords(),
              enemy.getPosition(),
              false);
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
        // we're only going to do air to ground attack plans if we're an airborne aero
        // attacking a ground unit
        if (aeroAttackingGroundUnitOnGroundMap) {
            myFiringPlan = getFireControl(path.getEntity()).guessFullAirToGroundPlan(me,
                  enemy,
                  new EntityState(enemy),
                  path,
                  game,
                  false);
        } else {
            EntityState shooterState = new EntityState(path);
            // Heat this path would add (walk/run/jump) is not yet committed to the unit, so pass it
            // explicitly; calcHeatTolerance lowers the firing budget by it so a hot move throttles fire.
            int predictedMovementHeat = shooterState.getHeat() - me.getHeat();
            FiringPlanCalculationParameters guess = new Builder().buildGuess(path.getEntity(),
                  shooterState,
                  enemy,
                  null,
                  getFireControl(me).calcHeatTolerance(me, me.isAero(), predictedMovementHeat),
                  null);
            myFiringPlan = getFireControl(me).determineBestFiringPlan(guess);
        }
        return myFiringPlan.getUtility();
    }

    double calculateMyKickDamagePotential(MovePath path, Entity enemy, Game game) {
        if (!(path.getEntity() instanceof Mek)) {
            return 0.0;
        }

        PhysicalInfo myKick = new PhysicalInfo(path.getEntity(),
              new EntityState(path),
              enemy,
              null,
              PhysicalAttackType.RIGHT_KICK,
              game,
              getOwner(),
              true);

        if (myKick.getProbabilityToHit() <= 0.5) {
            return 0.0;
        }

        return myKick.getExpectedDamageOnHit() * myKick.getProbabilityToHit();
    }

    EntityEvaluationResponse evaluateMovedEnemy(Entity enemy, MovePath path, Game game) {
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
            theirDamagePotential += calculateKickDamagePotential(enemy, path, game);
        }

        returnResponse.setEstimatedEnemyDamage(theirDamagePotential);

        // A unit that sprinted may not make any deliberate attacks this turn, weapon or physical,
        // so a sprinting path contributes no damage of mine.
        if (!isSprintingPath(path)) {
            // How much damage can I do to them?
            returnResponse.setMyEstimatedDamage(calculateMyDamagePotential(path, enemy, distance, game));

            // How much physical damage can I do to them?
            if (distance <= 1) {
                returnResponse.setMyEstimatedPhysicalDamage(calculateMyKickDamagePotential(path, enemy, game));
            }
        }

        return returnResponse;
    }

    /**
     * Determines whether an enemy can be disregarded when evaluating a move path: it is on another board, is an ejected
     * crew, is off the board or has no position, or is broken and fleeing the field.
     *
     * @param movingUnit the unit whose move path is being evaluated
     * @param enemy      the enemy to check
     * @param game       the current {@link Game}
     *
     * @return true if the enemy has no bearing on the path evaluation
     */
    boolean isIgnorableEnemy(Entity movingUnit, Entity enemy, Game game) {
        // For now, disregard enemy units that are not on the same board
        if (!game.onTheSameBoard(movingUnit, enemy)) {
            return true;
        }

        // Skip ejected pilots.
        if (enemy instanceof EjectedCrew) {
            return true;
        }

        // Skip units not on the board.
        if (enemy.isOffBoard() || (enemy.getPosition() == null) || !game.getBoard(enemy)
              .contains(enemy.getPosition())) {
            return true;
        }

        // Skip broken enemies
        return getOwner().getHonorUtil()
              .isEnemyBroken(enemy.getId(), enemy.getOwnerId(), getOwner().getForcedWithdrawal());
    }

    /**
     * Returns true if the given path ends in sprinting movement. A unit that sprinted may not make any deliberate
     * attacks or spot for indirect fire that turn (TacOps Sprinting rules).
     *
     * @param path the move path to check
     *
     * @return true if the path's final movement type is a ground or VTOL sprint
     */
    static boolean isSprintingPath(MovePath path) {
        EntityMovementType movementType = path.getLastStepMovementType();
        return (EntityMovementType.MOVE_SPRINT == movementType)
              || (EntityMovementType.MOVE_VTOL_SPRINT == movementType);
    }

    /**
     * Describes the enemy that makes ending a sprint at a given position dangerous.
     *
     * @param enemy             the threatening enemy
     * @param firingPosition    the position the enemy can bring weapons to bear from
     * @param distance          hexes from the path's final position to the firing position
     * @param maxWeaponRange    the threatening enemy's maximum weapon range
     * @param positionPredicted true if the firing position is a prediction (the enemy has not moved yet this turn and
     *                          the position is the closest one it can reach), false if it is where the enemy currently
     *                          stands
     */
    record SprintThreat(Entity enemy, Coords firingPosition, int distance, int maxWeaponRange,
          boolean positionPredicted) {
    }

    /**
     * Finds an enemy that can bring weapons to bear on the given path's final position, if any. For enemies that can
     * still move this turn, range is measured from the closest position they can reach instead of where they currently
     * stand. Returns the first threatening enemy found, not necessarily the closest one.
     *
     * @param path    the move path to check
     * @param enemies the known enemy units
     * @param game    the current {@link Game}
     *
     * @return the threat assessment for one enemy in weapon range of the path's final position, or empty if none
     */
    Optional<SprintThreat> findSprintThreat(MovePath path, List<Entity> enemies, Game game) {
        Coords finalCoords = path.getFinalCoords();
        Entity movingUnit = path.getEntity();

        for (Entity enemy : enemies) {
            if (isIgnorableEnemy(movingUnit, enemy, game)) {
                continue;
            }

            boolean positionPredicted = !evaluateAsMoved(enemy);
            Coords firingPosition = positionPredicted ? getClosestCoordsTo(enemy.getId(), finalCoords)
                  : enemy.getPosition();
            if (firingPosition == null) {
                // no movable-area data for this enemy; fall back to where it currently stands
                firingPosition = enemy.getPosition();
                positionPredicted = false;
            }

            int distance = finalCoords.distance(firingPosition);
            int maxWeaponRange = getOwner().getMaxWeaponRange(enemy, movingUnit.isAirborne());
            if (distance <= maxWeaponRange) {
                return Optional.of(new SprintThreat(enemy, firingPosition, distance, maxWeaponRange,
                      positionPredicted));
            }
        }

        return Optional.empty();
    }

    /**
     * Calculates a penalty for paths that end a sprint inside enemy weapon range. A sprinting unit forfeits all of its
     * attacks and suffers an additional -1 to-hit modifier against it, so ending a sprint where the enemy can shoot
     * back gives up firepower for no defensive gain. Units performing a forced withdrawal are exempt: they do not
     * intend to attack, and the extra movement serves the withdrawal.
     *
     * <p>The decision is traced two ways so it can be reconstructed after a battle: the sprint-related entries in
     * {@code scores} become columns in the {@link megamek.client.bot.BotLogger} TSV output, and {@code sprintFormula}
     * receives a human-readable explanation that ends up in the ranked path's reason string.</p>
     *
     * @param path          the move path to evaluate
     * @param enemies       the known enemy units
     * @param game          the current {@link Game}
     * @param scores        the path's score map; receives {@code isSprinting}, {@code sprintThreatEnemyId},
     *                      {@code sprintThreatDistance} and {@code sprintThreatRange} entries (-1 where not
     *                      applicable)
     * @param sprintFormula receives a human-readable explanation of the decision; left empty for non-sprint paths
     *
     * @return {@code SPRINT_INTO_THREAT_PENALTY} if the path sprints into enemy weapon range, otherwise 0
     */
    double calculateSprintExposurePenalty(MovePath path, List<Entity> enemies, Game game,
          Map<String, Double> scores, StringBuilder sprintFormula) {
        boolean sprinting = isSprintingPath(path);
        scores.put("isSprinting", sprinting ? 1.0 : 0.0);
        scores.put("sprintThreatEnemyId", -1.0);
        scores.put("sprintThreatDistance", -1.0);
        scores.put("sprintThreatRange", -1.0);

        if (!sprinting) {
            return 0;
        }

        Entity movingUnit = path.getEntity();
        boolean isWithdrawing = BehaviorType.ForcedWithdrawal ==
              getOwner().getUnitBehaviorTracker().getBehaviorType(movingUnit, getOwner());
        if (isWithdrawing) {
            sprintFormula.append("sprintExposurePenalty [0: withdrawing, sprint always allowed]");
            return 0;
        }

        Optional<SprintThreat> sprintThreat = findSprintThreat(path, enemies, game);
        if (sprintThreat.isEmpty()) {
            sprintFormula.append("sprintExposurePenalty [0: no enemy can reach ")
                  .append(path.getFinalCoords().toFriendlyString())
                  .append(" with weapons]");
            return 0;
        }

        SprintThreat threat = sprintThreat.get();
        scores.put("sprintThreatEnemyId", (double) threat.enemy().getId());
        scores.put("sprintThreatDistance", (double) threat.distance());
        scores.put("sprintThreatRange", (double) threat.maxWeaponRange());
        sprintFormula.append("sprintExposurePenalty [")
              .append(SPRINT_INTO_THREAT_PENALTY)
              .append(": sprint ends at ")
              .append(path.getFinalCoords().toFriendlyString())
              .append(", ")
              .append(threat.enemy().getDisplayName())
              .append(threat.positionPredicted() ? " can reach " : " stands at ")
              .append(threat.firingPosition().toFriendlyString())
              .append(", distance ")
              .append(threat.distance())
              .append(" <= weapon range ")
              .append(threat.maxWeaponRange())
              .append("]");
        return SPRINT_INTO_THREAT_PENALTY;
    }

    /**
     * Calculates an aggression modifier that penalizes paths keeping the unit far from enemies.
     *
     * <p>This method implements the tactical preference for closing with the enemy based on:
     * <ul>
     *   <li>The distance to the closest enemy from the path's final position</li>
     *   <li>The AI's configured hyper-aggression value</li>
     * </ul>
     *
     * <p>The aggression modifier follows this formula:
     * <pre>
     * aggressionMod = distanceToClosestEnemy * hyperAggressionValue
     * </pre>
     *
     * <p>Since this value is subtracted in the final utility calculation, higher values represent
     * stronger penalties for staying distant from enemies. A high hyperAggressionValue will strongly
     * push units toward closing with enemies regardless of other tactical considerations.
     *
     * @param movingUnit The entity being moved
     * @param path       The movement path being evaluated
     * @param game       The current game state
     *
     * @return An aggression modifier value (higher is worse) to be used in path ranking
     */
    protected double calculateAggressionMod(Entity movingUnit, MovePath path, Game game) {

        double distToEnemy = distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game);

        if ((distToEnemy == 0) && !(movingUnit instanceof Infantry)) {
            distToEnemy = 2;
        }

        double aggression = getOwner().getBehaviorSettings().getHyperAggressionValue();
        double aggressionMod = distToEnemy * aggression;
        logger.trace("aggression mod [ -{} = {} * {}]", aggressionMod, distToEnemy, aggression);
        return aggressionMod;
    }

    // Indirect artillery (targeting phase) is impossible at one mapsheet or closer - TooShortForIndirectArty,
    // ComputeToHitIsImpossible - so a tube must hold at least one hex beyond that (18+) to be able to fire at all.
    private static final int ARTILLERY_INDIRECT_MIN_RANGE = Board.DEFAULT_BOARD_HEIGHT + 1;

    // How sharply a tube is pulled back once it is inside its minimum indirect range: steep, so it overrides the herd
    // pull toward the advancing friendly line rather than drifting into a range from which it cannot fire.
    private static final double ARTILLERY_STANDOFF_URGENCY = 4.0;

    // Closest a TAG spotter will ever willingly get to an enemy - clear of the artillery blast and never point-blank -
    // even when it is hunting for a line of sight to designate.
    private static final int MIN_TAG_STANDOFF_DISTANCE = 4;

    // Per-hex penalty that keeps a safely-positioned artillery tube in its current hex instead of shuffling sideways
    // (which gains it nothing and can incur attacker-movement firing penalties). Large enough to dominate the small
    // movement/herding pull; the tube may still turn in place to face the enemy.
    private static final double ARTILLERY_HOLD_STILL_WEIGHT = 100.0;

    // Penalty added to a TAG spotter's path when it jumps to a hex it could otherwise designate from. Jumping piles a
    // large attacker-movement modifier onto the TAG to-hit, likely wasting a designation a homing round relies on, so
    // the bot prefers a non-jumping path and only jumps when that is the only way to reach a designating position.
    private static final double TAG_JUMP_PENALTY = 10.0;

    // Hysteresis: the spotter keeps painting its current (persisted) priority target across turns unless a different
    // enemy is worth at least this multiple of it - stops a round-to-round flip onto a marginally higher-value unit.
    private static final double SPOTTER_PRIORITY_SWITCH_MARGIN = 1.5;

    // Penalty for a candidate hex that cannot designate the priority target (no line of sight or out of TAG range). It
    // is added to a pull toward the target, so the spotter moves to regain a shot; large enough that any hex that CAN
    // designate the priority target is preferred. Tunable from the [TagPos] log.
    private static final double TAG_NO_PRIORITY_LOS_PENALTY = 200.0;

    /**
     * @param unit The unit being moved
     *
     * @return The distance (hexes) this unit prefers to hold from the nearest enemy because of its role - a tube's
     *       artillery standoff, or a spotter's TAG short-range bracket (the "line between short and medium" that gives
     *       the best TAG to-hit) - or 0 if it has no standoff role and should close normally
     */
    private int desiredStandoffDistance(Entity unit) {
        if (hasOperationalArtillery(unit)) {
            return ARTILLERY_INDIRECT_MIN_RANGE;
        }
        // A spotter wants the short/medium range line of its TAG: the longest distance that still gives the short-range
        // to-hit bonus. It holds here when it can; the existing movement (TMM) and incoming-damage terms then pull it
        // back toward whatever distance is actually survivable, so it ends up as close as it can safely afford.
        int tagShortRange = maxOperationalTagShortRange(unit);
        if (tagShortRange > 0) {
            // Never hold closer than the minimum safe distance, even if the TAG's short bracket is very short.
            return Math.max(MIN_TAG_STANDOFF_DISTANCE, tagShortRange);
        }
        return 0;
    }

    /**
     * @param enemies The enemy units
     *
     * @return The average run MP of the deployed, on-board enemies (the rate at which the enemy force closes), or 0 if
     *       there are none
     */
    private int averageEnemyRunMP(List<Entity> enemies) {
        int total = 0;
        int count = 0;
        for (Entity enemy : enemies) {
            if (enemy.isDeployed() && !enemy.isOffBoard() && (enemy.getPosition() != null)) {
                total += enemy.getRunMP();
                count++;
            }
        }
        return (count > 0) ? total / count : 0;
    }

    /**
     * The highest-BV enemy cluster anchor for the current ranking pass, computed once per pass in
     * {@link #rankPaths(List, Game, int, double, List, List)} and read by every {@link #rankPath} call of that
     * pass, or {@code null} when there are no deployed on-board enemies.
     */
    private Coords rankingPassClusterAnchor;

    @Override
    public TreeSet<RankedPath> rankPaths(List<MovePath> movePaths, Game game, int maxRange, double fallTolerance,
          List<Entity> enemies, List<Entity> friends) {
        // The highest-BV cluster anchor and the TAG-spotter priority target depend only on the enemy list, which
        // does not change during one ranking pass. Computing them once per pass instead of per candidate path
        // avoids O(paths x enemies) Battle Value recalculations - each of which, for C3/C3i/Nova units, also
        // rescans the whole network and its ECM state (issue #8443).
        rankingPassBattleValueCache.clear();
        rankingPassClusterAnchor = highestBvClusterPosition(enemies);
        rankingPassSpotterPriority = null;
        return super.rankPaths(movePaths, game, maxRange, fallTolerance, enemies, friends);
    }

    /**
     * Finds the position at the highest concentration of enemy battle value - the deployed on-board enemy whose
     * neighbourhood (within {@link Compute#HOMING_RADIUS}) holds the most total current BV. Standoff artillery and TAG
     * spotters use this cluster as their anchor (to stand off from, and to point at) instead of the nearest single
     * unit, so a lone low-value scout cannot decoy them and the homing's 8-hex radius covers the densest high-value
     * group.
     *
     * @param enemies The enemy units
     *
     * @return The cluster anchor position, or {@code null} if there are no deployed on-board enemies
     */
    private @Nullable Coords highestBvClusterPosition(List<Entity> enemies) {
        // Battle Value is expensive to compute (for C3 units it scans the whole network), so look it up in the
        // per-pass cache instead of recalculating per enemy.
        List<Entity> deployedEnemies = new ArrayList<>();
        Map<Integer, Double> battleValueByEntityId = new HashMap<>();
        for (Entity enemy : enemies) {
            if (enemy.isDeployed() && !enemy.isOffBoard() && (enemy.getPosition() != null)) {
                deployedEnemies.add(enemy);
                battleValueByEntityId.put(enemy.getId(), cachedBattleValue(enemy));
            }
        }

        Coords clusterAnchor = null;
        double bestClusterValue = -1.0;
        for (Entity center : deployedEnemies) {
            double clusterValue = 0.0;
            for (Entity other : deployedEnemies) {
                if (center.getPosition().distance(other.getPosition()) <= Compute.HOMING_RADIUS) {
                    clusterValue += battleValueByEntityId.get(other.getId());
                }
            }
            if (clusterValue > bestClusterValue) {
                bestClusterValue = clusterValue;
                clusterAnchor = center.getPosition();
            }
        }
        return clusterAnchor;
    }

    /** Battle Values cached for the current ranking pass; see {@link #cachedBattleValue(Entity)}. */
    private final Map<Integer, Double> rankingPassBattleValueCache = new HashMap<>();

    /**
     * Returns the entity's current Battle Value (at least 1), cached for the current ranking pass (the cache is
     * cleared in {@link #rankPaths(List, Game, int, double, List, List)}). A BV calculation is expensive - for
     * C3/C3i/Nova units it scans the whole network and recalculates every member - and nothing that changes a BV
     * (damage, C3/ECM connectivity) can change between the candidate paths of one pass, so each pass computes each
     * enemy's BV at most once and the next unit's pass sees fresh values.
     *
     * @param entity The unit whose Battle Value is wanted
     *
     * @return The unit's current Battle Value, minimum 1
     */
    private double cachedBattleValue(Entity entity) {
        return rankingPassBattleValueCache.computeIfAbsent(entity.getId(),
              entityId -> Math.max(1.0, entity.calculateBattleValue()));
    }

    /**
     * The spotter's priority target this turn and where the choice came from.
     *
     * @param target The enemy to position for and paint, or {@code null} if there is no on-board enemy
     * @param source {@code "PERSISTED"} (held from a previous turn), {@code "VALUE"} (this turn's highest-value enemy),
     *               or {@code "none"} (no target)
     */
    private record SpotterPriority(@Nullable Entity target, String source) {}

    /**
     * @param enemies The enemy units
     *
     * @return The deployed on-board enemy with the highest {@link ArtilleryTargetingControl#tagTargetValue}, or
     *       {@code null} if there is none
     */
    private @Nullable Entity highestValueEnemy(List<Entity> enemies) {
        Entity best = null;
        double bestValue = -1.0;
        for (Entity enemy : enemies) {
            if (!enemy.isDeployed() || enemy.isOffBoard() || (enemy.getPosition() == null)) {
                continue;
            }
            double value = ArtilleryTargetingControl.tagTargetValue(enemy, cachedBattleValue(enemy));
            if (value > bestValue) {
                bestValue = value;
                best = enemy;
            }
        }
        return best;
    }

    /**
     * Picks the enemy a TAG spotter should position for and paint this turn. It keeps last turn's target (persistence)
     * while that target is still a live, on-board enemy and no other enemy is worth at least
     * {@link #SPOTTER_PRIORITY_SWITCH_MARGIN} times as much - otherwise it locks onto the current highest-value enemy.
     * Because the homing tubes also commit to the highest-value enemy, the spotter and the missiles converge on the
     * same target from shared game state, without messaging.
     *
     * @param spotter The TAG-carrying spotter being moved
     * @param enemies The enemy units
     * @param game    The current game
     *
     * @return The chosen priority target and its source (never {@code null}; its {@code target()} is {@code null} only
     *       when there is no on-board enemy)
     */
    private SpotterPriority determineSpotterPriorityTarget(Entity spotter, List<Entity> enemies, Game game) {
        Entity highestValue = highestValueEnemy(enemies);
        if (highestValue == null) {
            lastSpotterPriorityTarget.remove(spotter.getId());
            return new SpotterPriority(null, "none");
        }
        Integer persistedId = lastSpotterPriorityTarget.get(spotter.getId());
        if (persistedId != null) {
            Entity persisted = game.getEntity(persistedId);
            boolean persistedValid = (persisted != null) && persisted.isDeployed() && !persisted.isOffBoard()
                  && (persisted.getPosition() != null) && persisted.isEnemyOf(spotter) && !persisted.isDestroyed();
            if (persistedValid
                  && (ArtilleryTargetingControl.tagTargetValue(highestValue, cachedBattleValue(highestValue))
                  < (SPOTTER_PRIORITY_SWITCH_MARGIN
                  * ArtilleryTargetingControl.tagTargetValue(persisted, cachedBattleValue(persisted))))) {
                lastSpotterPriorityTarget.put(spotter.getId(), persisted.getId());
                return new SpotterPriority(persisted, "PERSISTED");
            }
        }
        lastSpotterPriorityTarget.put(spotter.getId(), highestValue.getId());
        return new SpotterPriority(highestValue, "VALUE");
    }

    /**
     * The TAG-spotter priority target for the current ranking pass, computed on first use per pass and reset in
     * {@link #rankPaths(List, Game, int, double, List, List)}. The choice depends only on the enemy list and the
     * persisted target, none of which change between the candidate paths of one pass - and computing it per path
     * repeated an expensive Battle Value scan of every enemy (issue #8443).
     */
    private SpotterPriority rankingPassSpotterPriority;

    /**
     * @param spotter The TAG-carrying spotter being moved
     * @param enemies The enemy units
     * @param game    The current game
     *
     * @return The {@link #determineSpotterPriorityTarget(Entity, List, Game)} result, computed once per ranking pass
     */
    private SpotterPriority spotterPriorityForThisPass(Entity spotter, List<Entity> enemies, Game game) {
        if (rankingPassSpotterPriority == null) {
            rankingPassSpotterPriority = determineSpotterPriorityTarget(spotter, enemies, game);
        }
        return rankingPassSpotterPriority;
    }

    /**
     * @param tagUnit  The TAG-carrying spotter
     * @param from     The hex it would designate from (a path's final position)
     * @param target   The specific enemy it wants to designate
     * @param tagRange The spotter's TAG range
     * @param game     The current game
     *
     * @return {@code true} if, from the given hex, the spotter has both TAG range and line of sight to that specific
     *       target (so it can actually paint it for the homing rounds)
     */
    private boolean canDesignateTarget(Entity tagUnit, @Nullable Coords from, Entity target, int tagRange, Game game) {
        Coords targetPosition = target.getPosition();
        if ((from == null) || (tagRange <= 0) || (targetPosition == null)) {
            return false;
        }
        return (from.distance(targetPosition) <= tagRange)
              && LosEffects.calculateLOS(game, tagUnit, target, from, targetPosition, true).canSee();
    }

    /**
     * The result of evaluating one candidate path against the artillery / TAG-spotter standoff doctrine: the aggression
     * modifier to apply plus the diagnostics surfaced in the {@code [Move]} breakdown.
     *
     * @param aggressionMod           the standoff aggression penalty for this path
     * @param holdingArtilleryInPlace {@code true} if the tube is holding position (skip the movement-TMM reward)
     * @param standoffArtillery       {@code true} for the artillery-tube branch (ignores the herd pull, faces the
     *                                cluster)
     * @param branch                  the branch that fired (e.g. {@code TAG_DESIGNATE}, {@code FALL_BACK},
     *                                {@code none})
     * @param deficit                 how far inside its standoff the tube ends (artillery branch), else {@code 0}
     * @param currentDist             the tube's current distance to the nearest enemy (artillery branch), else
     *                                {@code -1}
     * @param prioritySource          where the spotter's priority came from
     *                                ({@code PERSISTED}/{@code VALUE}/{@code none})
     * @param priorityTargetName      the spotter's priority target display name, or {@code "-"}
     * @param priorityTargetId        the spotter's priority target id, or {@code -1}
     * @param distToPriority          distance from the path end to the priority target, or {@code -1}
     * @param losToPriority           {@code true} if the path end can designate the priority target
     */
    private record StandoffEvaluation(double aggressionMod, boolean holdingArtilleryInPlace, boolean standoffArtillery,
          String branch, double deficit, double currentDist, String prioritySource, String priorityTargetName,
          int priorityTargetId, double distToPriority, boolean losToPriority) {}

    /**
     * Evaluates one candidate path against the artillery / TAG-spotter standoff doctrine. Extracted from
     * {@link #rankPath} to keep that method readable: artillery tubes hold or fall back to a standoff distance and
     * never advance the tube; TAG spotters position in a designation zone around their highest-value priority target.
     *
     * @param movingUnit       the unit being moved
     * @param path             the candidate path
     * @param pathCopy         a clone of the path (used by the non-standoff aggression calculation)
     * @param game             the current game
     * @param enemies          the enemy units
     * @param isNotAirborne    {@code false} for an airborne aero on a ground map (which ignores aggression)
     * @param standoffDistance the desired standoff distance (already including the closure buffer), or {@code 0}
     * @param distToEnemy      this path's distance to the nearest enemy
     * @param distToCluster    this path's distance to the highest-BV enemy cluster
     * @param braveryMod       this path's bravery modifier (neutralized when a tube falls back inside its standoff)
     *
     * @return the standoff evaluation for this path
     */
    private StandoffEvaluation evaluateStandoff(Entity movingUnit, MovePath path, MovePath pathCopy, Game game,
          List<Entity> enemies, boolean isNotAirborne, int standoffDistance, double distToEnemy, double distToCluster,
          double braveryMod) {
        boolean holdingArtilleryInPlace = false;
        // True whenever this unit is being ranked by the artillery standoff branch (whether safely holding or falling
        // back). Such a tube ignores the herd pull entirely so it never creeps toward the advancing friendly line.
        boolean standoffArtillery = false;
        double aggressionMod;
        // Diagnostics for the artillery/TAG standoff decision, surfaced in the [Move] breakdown so a playtest can see
        // which branch drove a tube's move and pinpoint a 2-step shuffle (the branch that fires each turn + the deficit).
        String standoffBranch = "none";
        double standoffDeficit = 0.0;
        double standoffCurrentDist = -1.0;
        // TAG-spotter positioning diagnostics ([TagPos]): which target it positions for, where it came from, and the
        // shot it can get from the chosen hex - so a playtest can see why the spotter moved where it did.
        String prioritySource = "none";
        String priorityTargetName = "-";
        int priorityTargetId = -1;
        double distToPriority = -1.0;
        boolean losToPriority = false;
        if (!isNotAirborne) {
            aggressionMod = 0;
        } else if (standoffDistance > 0) {
            boolean tagSpotter = !hasOperationalArtillery(movingUnit);
            if (tagSpotter) {
                // Position for a TAG shot on the PRIORITY target (the highest-value enemy, held across turns), not just
                // any enemy. Every hex with line of sight + TAG range to it is acceptable (a designation ZONE, not a
                // single ring); within the safe-and-accurate band [MIN_TAG_STANDOFF_DISTANCE, standoffDistance] the
                // positioning penalty is flat, so survivability/herding choose the hex. A hex that cannot designate the
                // priority target is penalized and pulled toward regaining line of sight on it.
                int tagRange = maxOperationalTagRange(movingUnit);
                double hyperAggression = getOwner().getBehaviorSettings().getHyperAggressionValue();
                SpotterPriority priority = spotterPriorityForThisPass(movingUnit, enemies, game);
                Entity priorityTarget = priority.target();
                prioritySource = priority.source();
                if ((priorityTarget != null) && (priorityTarget.getPosition() != null)) {
                    priorityTargetName = priorityTarget.getShortName();
                    priorityTargetId = priorityTarget.getId();
                    distToPriority = path.getFinalCoords().distance(priorityTarget.getPosition());
                    losToPriority = canDesignateTarget(movingUnit,
                          path.getFinalCoords(),
                          priorityTarget,
                          tagRange,
                          game);
                    if (losToPriority) {
                        standoffBranch = "TAG_DESIGNATE";
                        if (distToPriority > standoffDistance) {
                            aggressionMod = (distToPriority - standoffDistance) * hyperAggression;
                        } else if (distToPriority < MIN_TAG_STANDOFF_DISTANCE) {
                            aggressionMod = (MIN_TAG_STANDOFF_DISTANCE - distToPriority) * hyperAggression;
                        } else {
                            // Inside the zone: any hex here is an equally good TAG position - let other terms decide.
                            aggressionMod = 0;
                        }
                        // Prefer to walk/run when this hex can already designate: jumping piles a large attacker-moved
                        // penalty onto the TAG roll and likely wastes the designation a homing round relies on.
                        if (path.isJumping()) {
                            aggressionMod += TAG_JUMP_PENALTY;
                        }
                    } else {
                        // Cannot designate the priority target from here: move toward line of sight / range on it.
                        standoffBranch = "TAG_CLOSE";
                        aggressionMod = TAG_NO_PRIORITY_LOS_PENALTY
                              + (Math.max(0, distToPriority - standoffDistance) * hyperAggression);
                    }
                } else {
                    // No identifiable priority target (no on-board enemy): fall back to the cluster-distance band.
                    boolean canDesignate = canDesignateFrom(movingUnit, path.getFinalCoords(), enemies, tagRange, game);
                    standoffBranch = canDesignate ? "TAG_DESIGNATE" : "TAG_CLOSE";
                    int band = canDesignate ? standoffDistance : MIN_TAG_STANDOFF_DISTANCE;
                    aggressionMod = Math.abs(distToCluster - band) * hyperAggression;
                }
            } else {
                // Artillery holds position - it fires from range and the enemy closes on its own, so it should never
                // walk its tube forward. Its floor is the larger of how far back it already is and the minimum indirect
                // range.
                standoffArtillery = true;
                double hexesMoved = (movingUnit.getPosition() != null)
                      ? movingUnit.getPosition().distance(path.getFinalCoords())
                      : 0;
                if (distToEnemy < 0) {
                    // No on-board enemy to stand off from (e.g. they all fled off-board): the tube can already deliver
                    // indirect and counter-battery fire on off-board targets from where it stands, so it holds position
                    // instead of re-pathing toward an off-board enemy it can shell but never reach. Without this, every
                    // path scored the same (distToEnemy == -1 for all of them) and the tube shuffled aimlessly back and
                    // forth - the "2-step".
                    standoffBranch = "NO_ONBOARD_ENEMY_HOLD";
                    holdingArtilleryInPlace = true;
                    aggressionMod = hexesMoved * ARTILLERY_HOLD_STILL_WEIGHT;
                } else {
                    double currentDistToEnemy = (movingUnit.getPosition() != null)
                          ? distanceToClosestEnemy(movingUnit, movingUnit.getPosition(), game)
                          : standoffDistance;
                    double hyperAggression = getOwner().getBehaviorSettings().getHyperAggressionValue();
                    double deficit = standoffDistance - distToEnemy;
                    standoffDeficit = deficit;
                    standoffCurrentDist = currentDistToEnemy;
                    boolean startedSafelyBack = currentDistToEnemy >= standoffDistance;
                    if (deficit > 0) {
                        // This path ends inside the standoff: fall back to regain it. Neutralize any positive bravery
                        // (close-range damage) pull so the tube is not lured forward by the damage it could deal there -
                        // a negative bravery (it would take damage) is left intact, since that also argues for falling
                        // back. This is what stops an artillery tube from wading inside its own minimum range.
                        standoffBranch = "FALL_BACK";
                        aggressionMod = (deficit * ARTILLERY_STANDOFF_URGENCY * hyperAggression)
                              + Math.max(0, braveryMod);
                    } else if (startedSafelyBack) {
                        // Already at or beyond standoff and staying there: hold position so the tube does not shuffle
                        // sideways (which gains nothing and can incur attacker-movement firing penalties); it may still
                        // turn in place to face the enemy. Mirrors setting artillery to Hold Position.
                        standoffBranch = "HOLD_SAFELY_BACK";
                        holdingArtilleryInPlace = true;
                        aggressionMod = hexesMoved * ARTILLERY_HOLD_STILL_WEIGHT;
                    } else {
                        // Was inside the standoff and this path reaches safety - exactly the retreat we want, so do not
                        // penalize the move; bravery then settles the tube at the standoff edge, its best firing spot.
                        standoffBranch = "REACHED_SAFETY";
                        holdingArtilleryInPlace = true;
                        aggressionMod = 0;
                    }
                    // Never advance the tube: if this path ends closer to the nearest enemy than where it stands now, add
                    // a hard penalty regardless of branch. A legitimate fall-back only ever increases distance, so this
                    // never punishes a real retreat - it kills the standoff "creep" where bravery/movement terms lured a
                    // closer hex over the farther-back option.
                    if (distToEnemy < currentDistToEnemy) {
                        aggressionMod += (currentDistToEnemy - distToEnemy) * ARTILLERY_HOLD_STILL_WEIGHT;
                    }
                }
            }
        } else {
            aggressionMod = calculateAggressionMod(movingUnit, pathCopy, game);
        }
        return new StandoffEvaluation(aggressionMod, holdingArtilleryInPlace, standoffArtillery, standoffBranch,
              standoffDeficit, standoffCurrentDist, prioritySource, priorityTargetName, priorityTargetId,
              distToPriority, losToPriority);
    }

    /**
     * @param unit The unit to check
     *
     * @return {@code true} if the unit has an undamaged artillery weapon (so it should fight from standoff range as a tube)
     */
    private boolean hasOperationalArtillery(Entity unit) {
        return unit.getWeaponList().stream()
              .anyMatch(weapon -> weapon.getType().hasFlag(WeaponType.F_ARTILLERY) && !weapon.isDestroyed());
    }

    /**
     * @param tagUnit  The TAG-carrying spotter
     * @param from     The hex it would be designating from (a path's final position)
     * @param enemies  The enemies it might designate
     * @param tagRange The spotter's TAG range
     * @param game     The current game
     *
     * @return {@code true} if, from the given hex, the spotter has both TAG range and line of sight to at least one enemy (so
     *       it can actually designate from there)
     */
    private boolean canDesignateFrom(Entity tagUnit, Coords from, List<Entity> enemies, int tagRange, Game game) {
        if ((from == null) || (tagRange <= 0)) {
            return false;
        }
        for (Entity enemy : enemies) {
            Coords enemyPosition = enemy.getPosition();
            if ((enemyPosition != null) && (from.distance(enemyPosition) <= tagRange)
                  && LosEffects.calculateLOS(game, tagUnit, enemy, from, enemyPosition, true).canSee()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param unit The unit to check
     *
     * @return The longest range of the unit's undamaged TAG weapons, or 0 if it carries no operational TAG
     */
    private int maxOperationalTagRange(Entity unit) {
        int range = 0;
        for (WeaponMounted weapon : unit.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_TAG) && !weapon.isDestroyed()) {
                range = Math.max(range, weapon.getType().getLongRange());
            }
        }
        return range;
    }

    /**
     * @param unit The unit to check
     *
     * @return The short-range bracket of the unit's undamaged TAG weapon - the distance giving the best TAG to-hit - or
     *       0 if it carries no operational TAG
     */
    private int maxOperationalTagShortRange(Entity unit) {
        int shortRange = 0;
        for (WeaponMounted weapon : unit.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_TAG) && !weapon.isDestroyed()) {
                shortRange = Math.max(shortRange, weapon.getType().getShortRange());
            }
        }
        return shortRange;
    }

    /**
     * Calculates a herding modifier that penalizes paths taking the unit away from friendly forces.
     *
     * <p>This method implements the tactical preference for maintaining formation with friendly units based on:
     * <ul>
     *   <li>The distance from the path's final position to the center of friendly forces</li>
     *   <li>The AI's configured herd mentality value</li>
     * </ul>
     *
     * <p>The herding modifier follows this formula:
     * <pre>
     * herdingMod = distanceToFriends * herdMentalityValue
     * </pre>
     *
     * <p>Since this value is subtracted in the final utility calculation, higher values represent
     * stronger penalties for straying from the friendly force. If no friendly forces are present
     * (friendsCoords is null), the method returns 0, applying no penalty.
     *
     * @param friendsCoords The coordinate representing the center of friendly forces, or null if no friends
     * @param path          The movement path being evaluated
     *
     * @return A herding modifier value (higher is worse) to be used in path ranking
     */
    protected double calculateHerdingMod(Coords friendsCoords, MovePath path) {
        if (friendsCoords == null) {
            logger.trace(" herdingMod [-0 no friends]");
            return 0;
        }

        double finalDistance = friendsCoords.distance(path.getFinalCoords());
        double herding = getOwner().getBehaviorSettings().getHerdMentalityValue();
        double herdingMod = finalDistance * herding;

        logger.trace("herding mod [-{} = {} * {}]", herdingMod, finalDistance, herding);
        return herdingMod;
    }

    /**
     * Calculates a facing modifier that penalizes paths where the unit is not facing threats.
     *
     * <p>This method evaluates how well the unit's final facing aligns with the direction
     * it should optimally face to confront enemies. The method:
     * <ul>
     *   <li>Determines the ideal facing based on the position of the closest enemies</li>
     *   <li>Calculates how far off the unit's final facing is from this ideal</li>
     *   <li>Applies a penalty proportional to how far off the facing is</li>
     *   <li>Considers armor distribution to bias facing toward better-protected sides</li>
     * </ul>
     *
     * <p>Facing differences are measured in hexside rotations (0-3), where:
     * <ul>
     *   <li>0 = Perfect facing toward threat</li>
     *   <li>1 = Off by one hexside (60°)</li>
     *   <li>2 = Off by two hexsides (120°)</li>
     *   <li>3 = Facing directly away (180°)</li>
     * </ul>
     *
     * <p>The facing penalty is calculated as: 50 * (facingDiff - 1), with a minimum of 0.
     * This means perfect facing has no penalty, while the worst facing incurs a -100 penalty.
     *
     * @param movingUnit           The entity being moved
     * @param game                 The current game state
     * @param path                 The movement path being evaluated
     * @param enemyMedianPosition  The coordinates of the median position of enemies, or null to use the board center
     * @param closestEnemyPosition The coordinates of the closest enemy, or null to use the board center
     *
     * @return A facing modifier value (higher is worse) to be used in path ranking
     */
    protected double calculateFacingMod(Entity movingUnit, Game game, final MovePath path,
          @Nullable Coords enemyMedianPosition, @Nullable Coords closestEnemyPosition) {
        int facingDiff = facingDiffCalculator.getFacingDiff(movingUnit,
              path,
              game.getBoard(movingUnit).getCenter(),
              enemyMedianPosition,
              closestEnemyPosition);
        double facingMod = FACING_MOD_MULTIPLIER * facingDiff;

        logger.trace("facing mod [(-){} = {} * {}]", facingMod, FACING_MOD_MULTIPLIER, facingDiff);
        return facingMod;
    }

    /**
     * Calculates a self-preservation modifier that encourages units to retreat when appropriate.
     *
     * <p>This method applies penalties or bonuses based on the unit's movement toward safety:
     * <ul>
     *   <li>For units in forced withdrawal or moving to a destination, encourages movement toward home edge</li>
     *   <li>Applies a significant bonus ({@link #ARRIVED_AT_DESTINATION_FACTOR}) if the path reaches the home edge</li>
     *   <li>Applies a penalty proportional to distance from home edge otherwise</li>
     * </ul>
     *
     * <p>The method only applies these modifiers when the unit is in forced withdrawal or deliberately
     * moving to a designated location. For normal combat operations, it returns 0 to indicate
     * self-preservation is not a priority factor.
     *
     * @param movingUnit The entity being moved
     * @param path       The movement path being evaluated
     * @param game       The current game state
     *
     * @return A self-preservation modifier value (negative values are better) to be used in path ranking
     */
    protected double calculateSelfPreservationMod(Entity movingUnit, MovePath path, Game game) {
        BehaviorType behaviorType = getOwner().getUnitBehaviorTracker().getBehaviorType(movingUnit, getOwner());

        if (behaviorType == BehaviorType.ForcedWithdrawal || behaviorType == BehaviorType.MoveToDestination) {
            int newDistanceToHome = distanceToHomeEdge(path.getFinalCoords(), path.getFinalBoardId(),
                  getOwner().getHomeEdge(movingUnit),
                  game);
            double selfPreservation = getOwner().getBehaviorSettings().getSelfPreservationValue();
            double selfPreservationMod;

            // normally, we favor being closer to the edge we're trying to get to
            if (newDistanceToHome > 0) {
                selfPreservationMod = newDistanceToHome * selfPreservation;
                // if this path gets us to the edge, we value it considerably more than we do
                // paths that don't get us there
            } else {
                selfPreservationMod = -ARRIVED_AT_DESTINATION_FACTOR;
            }

            logger.trace("self preservation mod [-{} = {} * {}]",
                  selfPreservationMod,
                  newDistanceToHome,
                  selfPreservation);
            return selfPreservationMod;
        }
        logger.trace("self preservation mod [0] - not moving nor forced to withdraw");
        return 0.0;
    }

    /**
     * Calculates a modifier that penalizes paths risking an aerospace unit flying off the board.
     *
     * <p>This method evaluates the risk of aerospace units inadvertently leaving the game board
     * on subsequent turns due to their movement characteristics. The method:
     * <ul>
     *   <li>Analyzes the path's final position, velocity, and facing</li>
     *   <li>Calculates whether the unit might be forced off-board on the following turns</li>
     *   <li>Returns a multiplier that severely penalizes paths with high off-board risk</li>
     * </ul>
     *
     * <p>For non-aerospace units, this method always returns 0 as they don't face this particular risk.
     * For aerospace units, a non-zero return value serves as a multiplier to other penalties,
     * effectively eliminating dangerous paths from consideration.
     *
     * @param path The movement path being evaluated
     *
     * @return A multiplier value (0 for no risk, positive for risk of flying off-board)
     */
    protected double calculateOffBoardMod(MovePath path) {
        return 0.0;
    }


    protected void checkBlackIce(Game game) {
        blackIce = ((game.getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE) &&
              game.getPlanetaryConditions().getTemperature() <= PlanetaryConditions.BLACK_ICE_TEMP) ||
              game.getPlanetaryConditions().getWeather().isIceStorm()) ? 1 : 0;
    }


    /**
     * Evaluates and ranks movement paths for MegaMek units based on multiple strategic factors. Returns a utility score
     * where higher values represent more desirable paths.
     *
     * <p>The utility score calculation combines several weighted factors:</p>
     * <pre>
     *   utility = -fallMod + braveryMod - aggressionMod - herdingMod + movementMod
     *             - crowdingTolerance - facingMod - selfPreservationMod - (utility * offBoardMod)
     * </pre>
     *
     * <p><strong>Key Components:</strong></p>
     * <ul>
     *   <li><strong>fallMod</strong>: Penalty for paths with risk of failing piloting rolls
     *     <ul><li>Calculated as {@code pilotingFailure * fallShame}</li>
     *         <li>Higher values = worse paths (more likely to fall)</li></ul>
     *   </li>
     *   <li><strong>braveryMod</strong>: Reward for paths balancing damage output vs. incoming damage
     *     <ul><li>Based on potential damage to enemies vs. expected damage received</li>
     *         <li>Higher values = better paths (can do more damage than take)</li></ul>
     *   </li>
     *   <li><strong>aggressionMod</strong>: Penalty for staying far from enemies
     *     <ul><li>Calculated as {@code distanceToEnemy * aggressionValue}</li>
     *         <li>Higher values = worse paths (too far from enemies when aggression is high)</li></ul>
     *   </li>
     *   <li><strong>herdingMod</strong>: Penalty for moving away from friendly units
     *     <ul><li>Calculated as {@code distanceToFriends * herdingValue}</li>
     *         <li>Higher values = worse paths (isolated from allies)</li></ul>
     *   </li>
     *   <li><strong>facingMod</strong>: Penalty for not facing toward enemies
     *     <ul><li>Based on facing direction relative to enemies</li>
     *         <li>Higher values = worse paths (facing away from threats)</li></ul>
     *   </li>
     *   <li><strong>selfPreservationMod</strong>: Penalty for not moving toward retreat edge when needed
     *     <ul><li>Special case: if reaching the home edge, gives a large bonus of {@code ARRIVED_AT_DESTINATION_FACTOR}</li>
     *         <li>Higher values = worse paths (not retreating when damaged)</li></ul>
     *   </li>
     *   <li><strong>movementMod</strong>: Reward for using movement effectively
     *     <ul><li>Based on multiple strategic factors</li>
     *         <li>Higher values = better paths (using speed effectively)</li></ul>
     *   </li>
     *   <li><strong>crowdingTolerance</strong>: Penalty for ending in crowded areas
     *     <ul><li>Based on nearby threats relative to maximum weapon range</li>
     *         <li>Higher values = worse paths (too many enemies nearby)</li></ul>
     *   </li>
     *   <li><strong>offBoardMod</strong>: Penalty for aerospace units at risk of flying off the board
     *     <ul><li>Applies as a multiplier to existing penalties</li>
     *         <li>Higher values = worse paths (likely to fly off the board)</li></ul>
     *   </li>
     * </ul>
     *
     * <p>The function uses behavior settings like bravery, aggression, and herd mentality to adjust
     * the relative importance of these factors based on the AI's configured personality.</p>
     *
     * @param path          The movement path to be evaluated
     * @param game          The current game state
     * @param maxRange      Entity max weapon range
     * @param fallTolerance a maximum PSR failure chance to be acceptable
     * @param enemies       List of enemy units visible
     * @param friendsCoords Center of Gravity of friendly units (average coordinate position)
     *
     * @return A double representing the utility/desirability of the path (higher is better)
     */
    @Override
    protected RankedPath rankPath(MovePath path, Game game, int maxRange, double fallTolerance, List<Entity> enemies,
          Coords friendsCoords) {
        Entity movingUnit = path.getEntity();

        if (blackIce == -1) {
            checkBlackIce(game);
        }

        Map<String, Double> scores = new HashMap<>();
        // Copy the path to avoid inadvertent changes.
        MovePath pathCopy = path.clone();

        // Worry about failed piloting rolls (weighted by Fall Shame).
        double successProbability = getMovePathSuccessProbability(pathCopy);
        double fallMod = calculateFallMod(successProbability);
        scores.put("fallMod", fallMod);
        scores.put("successProbability", successProbability);
        scores.put("maxRange", (double) maxRange);
        scores.put("fallTolerance", fallTolerance);
        scores.put("blackIce", (double) blackIce);
        scores.put("enemies", (double) enemies.size());
        scores.put("friendsCoords_x", friendsCoords == null ? -1.0 : friendsCoords.getX());
        scores.put("friendsCoords_y", friendsCoords == null ? -1.0 : friendsCoords.getY());
        scores.put("entityId", (double) movingUnit.getId());
        scores.put("entityBehaviorState",
              (double) getOwner().getUnitBehaviorTracker().getBehaviorType(movingUnit, getOwner()).ordinal());

        // Worry about how badly we can damage ourselves on this path!
        double expectedDamageTaken = calculateMovePathPSRDamage(movingUnit, pathCopy);
        expectedDamageTaken += checkPathForHazards(pathCopy, movingUnit, game);
        expectedDamageTaken += MinefieldUtil.checkPathForMinefieldHazards(pathCopy);
        scores.put("damageExpectedPath", expectedDamageTaken);
        // look at all of my enemies
        FiringPhysicalDamage damageEstimate = new FiringPhysicalDamage();

        boolean extremeRange = isExtremeRange(game);
        boolean losRange = isLosRange(game);
        for (Entity enemy : enemies) {
            if (isIgnorableEnemy(movingUnit, enemy, game)) {
                continue;
            }

            EntityEvaluationResponse eval;

            if (evaluateAsMoved(enemy)) {
                // For units that have already moved
                eval = evaluateMovedEnemy(enemy, pathCopy, game);
            } else {
                // For units that have not moved this round
                eval = evaluateUnmovedEnemy(enemy, path, extremeRange, losRange);
            }

            // if we're not ignoring the enemy, we consider the damage that we may do to them;
            // however, just because we're ignoring them doesn't mean they won't shoot at
            // us.
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
            double friendlyArtilleryDamage;
            Map<Coords, Double> artyDamage = getOwner().getPathRankerState().getIncomingFriendlyArtilleryDamage();

            if (!artyDamage.containsKey(path.getFinalCoords())) {
                friendlyArtilleryDamage = ArtilleryTargetingControl.evaluateIncomingArtilleryDamage(path.getFinalCoords(),
                      getOwner());
                artyDamage.put(path.getFinalCoords(), friendlyArtilleryDamage);
            } else {
                friendlyArtilleryDamage = artyDamage.get(path.getFinalCoords());
            }

            expectedDamageTaken += friendlyArtilleryDamage;
        }

        damageEstimate = calcDamageToStrategicTargets(pathCopy, game, getOwner().getFireControlState(), damageEstimate);

        // If I cannot kick because I am a clan unit and "No physical attacks for the clans"
        // is enabled, set maximum physical damage for this path to zero.
        if (game.getOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL) &&
              path.getEntity().getCrew().isClanPilot()) {
            damageEstimate.physicalDamage = 0;
        }

        // In case we are ignoring damage output, set the damage to zero.
        if (getOwner().getBehaviorSettings().isIgnoreDamageOutput()) {
            damageEstimate.physicalDamage = 0;
            damageEstimate.firingDamage = 0;
        }

        scores.put("ignoreDamageOutput", getOwner().getBehaviorSettings().isIgnoreDamageOutput() ? 1.0 : 0.0);
        scores.put("damageExpectedTotal", expectedDamageTaken);
        scores.put("myAttackFiring", damageEstimate.firingDamage);
        scores.put("myAttackPhysical", damageEstimate.physicalDamage);
        // I can kick a different target than I shoot, so add physical to
        // total damage after I've looked at all enemies

        double braveryMod = getBraveryMod(successProbability, damageEstimate, expectedDamageTaken);
        scores.put("braveryValue", getOwner().getBehaviorSettings().getBraveryValue());
        scores.put("braveryIndex", (double) getOwner().getBehaviorSettings().getBraveryIndex());
        scores.put("braveryMod", braveryMod);
        var isNotAirborne = !path.getEntity().isAirborneAeroOnGroundMap();
        // The only critters not subject to aggression and herding mods are
        // airborne aeros on ground maps, as they move incredibly fast.
        // The further I am from a target, the lower this path ranks
        // (weighted by Aggression slider).
        double distToEnemy = distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game);
        // Highest enemy BV concentration: standoff artillery and TAG spotters position and face relative to this cluster
        // rather than the nearest single unit, so a lone low-value scout cannot decoy them. Computed once per
        // ranking pass in rankPaths - see rankingPassClusterAnchor.
        Coords highValueClusterPosition = rankingPassClusterAnchor;
        double distToCluster = (highValueClusterPosition != null)
              ? path.getFinalCoords().distance(highValueClusterPosition)
              : distToEnemy;
        // Standoff units (artillery tubes, TAG spotters) prefer to hold at a standoff distance instead of closing to
        // contact. Artillery just wants to be at least that far back (it fires indirect from anywhere, has a minimum
        // range, and is fragile); a TAG spotter wants to sit in a band - close enough to designate, far enough to keep
        // clear of its own side's barrage. Reuse the aggression weight so the magnitude matches normal movement.
        int standoffDistance = isNotAirborne ? desiredStandoffDistance(movingUnit) : 0;
        if (standoffDistance > 0) {
            // Closure buffer: a standoff unit slower than the enemy force cannot just back away to keep its distance,
            // so it must hold farther out to still be effective - and alive - after the enemy closes the gap. Buffer =
            // how much faster the average enemy is than this unit (0 if it can outrun them).
            standoffDistance += Math.max(0, averageEnemyRunMP(enemies) - movingUnit.getRunMP());
        }
        StandoffEvaluation standoff = evaluateStandoff(movingUnit, path, pathCopy, game, enemies, isNotAirborne,
              standoffDistance, distToEnemy, distToCluster, braveryMod);
        double aggressionMod = standoff.aggressionMod();
        boolean holdingArtilleryInPlace = standoff.holdingArtilleryInPlace();
        boolean standoffArtillery = standoff.standoffArtillery();
        String standoffBranch = standoff.branch();
        double standoffDeficit = standoff.deficit();
        double standoffCurrentDist = standoff.currentDist();
        String prioritySource = standoff.prioritySource();
        String priorityTargetName = standoff.priorityTargetName();
        int priorityTargetId = standoff.priorityTargetId();
        double distToPriority = standoff.distToPriority();
        boolean losToPriority = standoff.losToPriority();
        scores.put("closestEnemyDistance", distToEnemy);
        scores.put("standoffDistance", (double) standoffDistance);
        scores.put("standoffDeficit", standoffDeficit);
        scores.put("distToCluster", distToCluster);
        scores.put("distToPriority", distToPriority);
        scores.put("priorityTargetId", (double) priorityTargetId);
        scores.put("aggressionValue", getOwner().getBehaviorSettings().getHyperAggressionValue());
        scores.put("aggressionIndex", (double) getOwner().getBehaviorSettings().getHyperAggressionIndex());
        scores.put("aggressionMod", aggressionMod);

        // The further I am from my teammates, the lower this path
        // ranks (weighted by Herd Mentality).
        // Standoff artillery (whether holding at range or falling back when the enemy breaches its standoff) ignores the
        // herd pull, so it never gets dragged toward the advancing friendly line instead of keeping its distance.
        double herdingMod = (isNotAirborne && !standoffArtillery)
              ? calculateHerdingMod(friendsCoords, pathCopy)
              : 0;

        // Movement is good, it gives defense and extends a player power in the game.
        if (movingUnit.getPosition() != null && friendsCoords != null) {
            scores.put("friendsDistance", (double) friendsCoords.distance(movingUnit.getPosition()));
        }
        scores.put("herdingValue", getOwner().getBehaviorSettings().getHerdMentalityValue());
        scores.put("herdingIndex", (double) getOwner().getBehaviorSettings().getHerdMentalityIndex());
        scores.put("herdingMod", herdingMod);

        var movementModFormula = new StringBuilder(64);

        // A holding artillery tube does not chase movement TMM - staying put is the point - so skip the reward.
        double movementMod = holdingArtilleryInPlace ? 0.0
              : calculateMovementMod(pathCopy, game, enemies, movementModFormula);
        scores.put("enemyHotSpotCount", (double) getOwner().getEnemyHotSpots().size());
        scores.put("selfPreservationValue", getOwner().getBehaviorSettings().getSelfPreservationValue());
        scores.put("selfPreservationIndex", (double) getOwner().getBehaviorSettings().getSelfPreservationIndex());
        scores.put("movementMod", movementMod);
        // Try to face the enemy.
        Coords medianEnemyPosition = unitsMedianCoordinateCalculator.getEnemiesMedianCoordinate(enemies,
              path.getFinalCoords(), path.getFinalBoardId());
        Coords closestEnemyPositionNotZeroDistance = Optional.ofNullable(findClosestEnemy(movingUnit,
              pathCopy.getFinalCoords(),
              game,
              false,
              1)).map(Targetable::getPosition).orElse(null);
        // Standoff artillery points at the highest BV concentration (the cluster) rather than the enemy median/nearest
        // unit: a stable heading toward the dense high-value group keeps its Arrow IV firing arc on target and its rear
        // protected, and stops it spinning to re-face a fast-moving lone scout each turn. When it is already facing the
        // cluster (within tolerance) facingMod is 0, so it neither moves nor turns - it only re-faces if the cluster
        // genuinely shifts.
        Coords facingTarget = (standoffArtillery && (highValueClusterPosition != null))
              ? highValueClusterPosition
              : null;
        double facingMod = calculateFacingMod(movingUnit,
              game,
              pathCopy,
              (facingTarget != null) ? facingTarget : medianEnemyPosition,
              (facingTarget != null) ? facingTarget : closestEnemyPositionNotZeroDistance);
        scores.put("finalFacing", (double) pathCopy.getFinalFacing());
        scores.put("facingDiff", facingMod / FACING_MOD_MULTIPLIER);
        scores.put("facingMod", facingMod);

        var formula = new StringBuilder(256);
        var crowdingToleranceFormula = new StringBuilder(64);

        double crowdingTolerance = calculateCrowdingTolerance(pathCopy, enemies, maxRange, crowdingToleranceFormula);

        double selfPreservationMod = calculateSelfPreservationMod(movingUnit, pathCopy, game);

        StringBuilder sprintFormula = new StringBuilder(64);
        double sprintExposurePenalty = calculateSprintExposurePenalty(pathCopy, enemies, game, scores,
              sprintFormula);
        scores.put("sprintExposurePenalty", sprintExposurePenalty);

        double offBoardMod = calculateOffBoardMod(pathCopy);
        // if we're an aircraft, we want to devalue paths that will force us off the board on the subsequent turn.
        double utility = -fallMod;
        utility += braveryMod;
        utility -= aggressionMod;
        utility -= herdingMod;
        utility += movementMod;
        utility -= crowdingTolerance;
        utility -= facingMod;
        utility -= selfPreservationMod;
        utility -= sprintExposurePenalty;
        utility -= utility * offBoardMod;

        formula.append("Calculation: {fall mod [")
              .append(LOG_DECIMAL.format(fallMod))
              .append(" = ")
              .append(LOG_DECIMAL.format(1 - successProbability))
              .append(" * ")
              .append(LOG_DECIMAL.format(getOwner().getBehaviorSettings().getFallShameValue()))
              .append("] + braveryMod [")
              .append(LOG_DECIMAL.format(braveryMod))
              .append(" = ")
              .append(LOG_PERCENT.format(successProbability))
              .append(" * ((")
              .append(LOG_DECIMAL.format(damageEstimate.getMaximumDamageEstimate()))
              .append(" * ")
              .append(LOG_DECIMAL.format(getOwner().getBehaviorSettings().getBraveryValue()))
              .append(") - ")
              .append(LOG_DECIMAL.format(expectedDamageTaken))
              .append(")] - aggressionMod [")
              .append(LOG_DECIMAL.format(aggressionMod))
              .append(" = ")
              .append(LOG_DECIMAL.format(distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game)))
              .append(" * ")
              .append(LOG_DECIMAL.format(getOwner().getBehaviorSettings().getHyperAggressionValue()))
              .append("; standoff: ").append(standoffBranch)
              .append(" standoffDist=").append(standoffDistance)
              .append(" distToEnemy=").append(LOG_DECIMAL.format(distToEnemy))
              .append(" curDist=").append(LOG_DECIMAL.format(standoffCurrentDist))
              .append(" deficit=").append(LOG_DECIMAL.format(standoffDeficit));
        if (!"none".equals(prioritySource)) {
            formula.append(" [TagPos] priority=").append(priorityTargetName)
                  .append("/").append(prioritySource)
                  .append(" distToPriority=").append(LOG_DECIMAL.format(distToPriority))
                  .append(" distToCluster=").append(LOG_DECIMAL.format(distToCluster))
                  .append(" losPriority=").append(losToPriority);
        }
        formula.append("] - herdingMod [");
        if (friendsCoords != null) {
            formula.append(LOG_DECIMAL.format(herdingMod))
                  .append(" = ")
                  .append(LOG_DECIMAL.format(friendsCoords.distance(path.getFinalCoords())))
                  .append(" * ")
                  .append(LOG_DECIMAL.format(getOwner().getBehaviorSettings().getHerdMentalityValue()));
        } else {
            formula.append("0 no friends");
        }
        formula.append("]");
        if (movementMod != 0.0) {
            formula.append(" + ").append(movementModFormula);
        }
        if (crowdingTolerance != 0.0) {
            formula.append(" - ").append(crowdingToleranceFormula);
        }

        formula.append(" - facingMod [")
              .append((int) facingMod)
              .append(" = ")
              .append(FACING_MOD_MULTIPLIER)
              .append(" * ")
              .append((int) (facingMod / FACING_MOD_MULTIPLIER))
              .append("]");

        if (!sprintFormula.isEmpty()) {
            formula.append(" - ").append(sprintFormula);
        }

        logger.trace("{}", formula);

        RankedPath rankedPath = new RankedPath(utility, pathCopy, formula.toString());
        rankedPath.setExpectedDamage(damageEstimate.getMaximumDamageEstimate());
        rankedPath.getScores().putAll(scores);
        return rankedPath;
    }

    protected boolean isLosRange(Game game) {
        return game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE);
    }

    protected boolean isExtremeRange(Game game) {
        return game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE);
    }

    /**
     * Calculates a bravery modifier for path evaluation based on potential damage and risk.
     *
     * <p>This method determines how "brave" the AI should be when considering a path by weighing:
     * <ul>
     *   <li>The probability of successfully completing the move (avoiding falls)</li>
     *   <li>The maximum potential damage the unit can inflict from the final position</li>
     *   <li>The expected damage the unit might receive</li>
     * </ul>
     *
     * <p>The bravery modifier follows this formula:
     * <pre>
     * braveryMod = (successProbability * maximumDamageDone * braveryValue) - expectedDamageTaken
     * </pre>
     *
     * <p>Higher bravery values make the AI more willing to accept risks if it can deal significant damage.
     * A positive bravery modifier indicates the path is tactically advantageous despite potential risks.
     *
     * @param successProbability  The probability (0.0 to 1.0) of making all required piloting rolls
     * @param damageEstimate      Container with estimates of damage that can be done
     * @param expectedDamageTaken Expected damage to be received in this position
     *
     * @return A bravery modifier value (higher is better) to be used in path ranking
     */
    protected double getBraveryMod(double successProbability, FiringPhysicalDamage damageEstimate,
          double expectedDamageTaken) {
        double maximumDamageDone = damageEstimate.getMaximumDamageEstimate();
        // My bravery modifier is based on my chance of getting to the
        // firing position (successProbability), how much damage I can do
        // (weighted by bravery), less the damage I might take.
        double braveryValue = getOwner().getBehaviorSettings().getBraveryValue();
        double braveryMod = (successProbability * maximumDamageDone * braveryValue) - expectedDamageTaken;
        logger.trace("bravery mod [{} = {} * (({} * {}) - {})]",
              braveryMod,
              successProbability,
              maximumDamageDone,
              braveryValue,
              expectedDamageTaken);
        return braveryMod;
    }

    /**
     * Calculates a movement modifier that rewards paths making units harder to hit. This serves as a key positive
     * modifier in the overall utility calculation.
     *
     * <p>The function encourages units to:</p>
     * <ul>
     *   <li>Keep moving when tactically beneficial</li>
     *   <li>Choose paths with optimal movement types</li>
     *   <li>Use movement as a defensive strategy</li>
     * </ul>
     *
     * <p>The modifier is calculated as:</p>
     * <pre>
     *   movementFactor = TMM × (selfPreservation + favorHigherTMM)
     * </pre>
     *
     * <p>Where:</p>
     * <ul>
     *   <li><strong>TMM</strong>: Target Movement Modifier - higher values make units harder to hit</li>
     *   <li><strong>selfPreservation</strong>: AI setting for valuing survival</li>
     *   <li><strong>favorHigherTMM</strong>: AI setting specifically for valuing movement as defense</li>
     * </ul>
     *
     * <p>The modifier is applied when either:</p>
     * <ul>
     *   <li>No enemies are visible (encouraging exploration/positioning)</li>
     *   <li>OR {@code favorHigherTMM} setting is enabled (encouraging defensive movement)</li>
     * </ul>
     *
     * @param pathCopy The movement path to evaluate
     * @param game     The current game state
     * @param enemies  List of enemy units visible
     * @param formula  StringBuilder to append the formula for logging
     *
     * @return A positive double value representing the movement modifier
     *
     * @author Luana Coppio
     */
    protected double calculateMovementMod(MovePath pathCopy, Game game, List<Entity> enemies, StringBuilder formula) {
        var favorHigherTMM = getOwner().getBehaviorSettings().getFavorHigherTMM();
        boolean noEnemiesInSight = enemies.isEmpty() && getOwner().getEnemyHotSpots().isEmpty();
        boolean disabledFavorHigherTMM = favorHigherTMM == 0;
        if (noEnemiesInSight || !disabledFavorHigherTMM) {
            var tmm = Compute.getTargetMovementModifier(pathCopy.getHexesMoved(),
                  pathCopy.isJumping(),
                  pathCopy.isAirborne(),
                  game);
            double selfPreservation = getOwner().getBehaviorSettings().getSelfPreservationValue();
            var tmmValue = tmm.getValue();
            var movementFactor = tmmValue * (selfPreservation + favorHigherTMM);
            formula.append("movementMod [")
                  .append(movementFactor)
                  .append(" = ")
                  .append(tmmValue)
                  .append(" * (")
                  .append(selfPreservation)
                  .append(" + ")
                  .append(favorHigherTMM)
                  .append(")]");
            logger.trace("movement mod [{} = {} * ({} + {})]",
                  movementFactor,
                  tmmValue,
                  selfPreservation,
                  favorHigherTMM);
            return movementFactor;
        }
        return 0.0;
    }

    /**
     * Calculates a crowding tolerance modifier that penalizes paths ending in densely populated areas.
     *
     * <p>This method evaluates the tactical risks of positioning in areas with many units nearby:
     * <ul>
     *   <li>Counts friendly units within a short radius (depends on antiCrowding setting)</li>
     *   <li>Counts enemy units within a medium radius (typically 60% of max weapon range)</li>
     *   <li>Applies penalties based on these counts and the AI's antiCrowding preference</li>
     * </ul>
     *
     * <p>The crowding penalty increases as more units (friendly or enemy) are present near the final position.
     * This encourages units to maintain tactical spacing rather than clumping together, which helps avoid:
     * <ul>
     *   <li>Becoming easy targets for area effect weapons</li>
     *   <li>Blocking each other's line of fire</li>
     *   <li>Creating movement bottlenecks</li>
     * </ul>
     *
     * <p>The crowding penalty is only applied to Meks and Tanks, as other unit types have different
     * tactical positioning requirements.
     *
     * @param movePath The movement path being evaluated
     * @param enemies  List of enemy units visible
     * @param maxRange Entity's maximum weapon range
     * @param formula  StringBuilder to append the formula explanation for logging
     *
     * @return A crowding tolerance value (higher is worse) to be used in path ranking
     *
     * @author Luana Coppio
     */
    protected double calculateCrowdingTolerance(MovePath movePath, List<Entity> enemies, double maxRange,
          StringBuilder formula) {
        var self = movePath.getEntity();
        formula.append(" crowdingTolerance ");
        if (!(self instanceof Mek) && !(self instanceof Tank)) {
            formula.append("[0 not a Mek or Tank]}");
            return 0.0;
        }

        var antiCrowding = getOwner().getBehaviorSettings().getAntiCrowding();
        if (antiCrowding == 0) {
            formula.append("[0 antiCrowding is disabled]}");
            return 0;
        }

        var antiCrowdingFactor = (10.0 / (11 - antiCrowding));
        final double herdingDistance = Math.ceil(antiCrowding * 1.3);
        final double closingDistance = Math.ceil(Math.max(3.0, maxRange * 0.6));

        var crowdingFriends = getOwner().getFriendEntities()
              .stream()
              .filter(e -> movePath.getFinalBoardId() == e.getBoardId())
              .filter(e -> e instanceof Mek || e instanceof Tank)
              .filter(Entity::isDeployed)
              .map(Entity::getPosition)
              .filter(Objects::nonNull)
              .filter(c -> c.distance(movePath.getFinalCoords()) <= herdingDistance)
              .count();

        var crowdingEnemies = enemies.stream()
              .filter(e -> movePath.getFinalBoardId() == e.getBoardId())
              .filter(e -> e instanceof Mek || e instanceof Tank)
              .filter(Entity::isDeployed)
              .map(Entity::getPosition)
              .filter(Objects::nonNull)
              .filter(c -> c.distance(movePath.getFinalCoords()) <= closingDistance)
              .count();

        double friendsCrowdingTolerance = antiCrowdingFactor * crowdingFriends;
        double enemiesCrowdingTolerance = antiCrowdingFactor * crowdingEnemies;
        formula.append("[")
              .append(friendsCrowdingTolerance + enemiesCrowdingTolerance)
              .append(" = (")
              .append(antiCrowdingFactor)
              .append(" * ")
              .append(crowdingFriends)
              .append(" friends) + (")
              .append(antiCrowdingFactor)
              .append(" * ")
              .append(crowdingEnemies)
              .append(" enemies)]");
        return friendsCrowdingTolerance + enemiesCrowdingTolerance;
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
                if (f == unit) {
                    // Docstring says ignore self, so ignore self.
                    max_damage = 0;
                } else {
                    FiringPlanCalculationParameters guess = new Builder().buildGuess(e,
                          null,
                          f,
                          null,
                          (e.getHeatCapacity() - e.getHeat()) + 5,
                          null);
                    double damage = getFireControl(f).determineBestFiringPlan(guess).getExpectedDamage();
                    if (damage > max_damage) {
                        max_damage = damage;
                    }
                }
            }
            bestDamageByEnemies.put(e.getId(), max_damage);
        }
    }

    protected FiringPhysicalDamage calcDamageToStrategicTargets(MovePath path, Game game,
          FireControlState fireControlState, FiringPhysicalDamage damageStructure) {

        for (int i = 0; i < fireControlState.getAdditionalTargets().size(); i++) {
            Targetable target = fireControlState.getAdditionalTargets().get(i);

            if (target.isOffBoard() ||
                  (target.getPosition() == null) ||
                  !game.getBoard(target).contains(target.getPosition())) {
                continue; // Skip targets not actually on the board.
            }

            FiringPlanCalculationParameters guess = new Builder().buildGuess(path.getEntity(),
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

            if (path.getEntity().isMek()) {
                PhysicalInfo myKick = new PhysicalInfo(path.getEntity(),
                      new EntityState(path),
                      target,
                      null,
                      PhysicalAttackType.RIGHT_KICK,
                      game,
                      getOwner(),
                      true);
                double expectedKickDamage = myKick.getExpectedDamageOnHit() * myKick.getProbabilityToHit();
                if (expectedKickDamage > damageStructure.physicalDamage) {
                    damageStructure.physicalDamage = expectedKickDamage;
                }
            }
        }
        return damageStructure;
    }

    /**
     * Gives the distance to the closest enemy unit, or -1 if none exist. The reason being that the closest enemy unit
     * may be 0 away.
     *
     * @param me       {@link Entity} who has enemies
     * @param position {@link Coords} from which the closest enemy is found
     * @param game     The current {@link Game}
     */
    @Override
    public double distanceToClosestEnemy(Entity me, Coords position, Game game) {
        Targetable closest = findClosestEnemy(me, position, game);
        if (closest == null) {
            return -1;
        }
        return closest.getPosition().distance(position);
    }

    public double checkPathForHazards(MovePath path, Entity movingUnit, Game game) {
        logger.trace("Checking Path ({}) for hazards.", path);

        // If we're flying or swimming, we don't care about ground hazards.
        if (EntityMovementType.MOVE_FLYING.equals(path.getLastStepMovementType()) ||
              EntityMovementType.MOVE_OVER_THRUST.equals(path.getLastStepMovementType()) ||
              EntityMovementType.MOVE_SAFE_THRUST.equals(path.getLastStepMovementType()) ||
              EntityMovementType.MOVE_VTOL_WALK.equals(path.getLastStepMovementType()) ||
              EntityMovementType.MOVE_VTOL_RUN.equals(path.getLastStepMovementType()) ||
              EntityMovementType.MOVE_VTOL_SPRINT.equals(path.getLastStepMovementType()) ||
              EntityMovementType.MOVE_SUBMARINE_WALK.equals(path.getLastStepMovementType()) ||
              EntityMovementType.MOVE_SUBMARINE_RUN.equals(path.getLastStepMovementType())) {

            logger.trace("Move Type ({}) ignores ground hazards.", path.getLastStepMovementType());
            return 0;
        }

        // If we're jumping, we only care about where we land.
        if (path.isJumping()) {
            logger.trace("Jumping, only checking landing hex.");
            Coords endCoords = path.getFinalCoords();
            Hex endHex = game.getBoard(path.getFinalBoardId()).getHex(endCoords);
            return checkHexForHazards(endHex,
                  movingUnit,
                  true,
                  path.getLastStep(),
                  true,
                  path,
                  game.getBoard(path.getFinalBoardId()));
        }

        double totalHazard = 0;
        Coords previousCoords = null;
        MoveStep lastStep = path.getLastStep();
        for (MoveStep step : path.getStepVector()) {
            Coords coords = step.getPosition();
            if ((coords == null) || coords.equals(previousCoords)) {
                continue;
            }
            Hex hex = game.getBoard(step.getBoardId()).getHex(coords);
            totalHazard += checkHexForHazards(hex,
                  movingUnit,
                  lastStep.equals(step),
                  step,
                  false,
                  path,
                  game.getBoard(step.getBoardId()));
            previousCoords = coords;
        }
        logger.trace("Total Hazard = {}", totalHazard);
        return totalHazard;
    }

    private double checkHexForHazards(Hex hex, Entity movingUnit, boolean endHex, MoveStep step, boolean jumpLanding,
          MovePath movePath, Board board) {
        // Building Entities are quirky, let's prefer an empty hex if the entity is one.
        double hazardValue = 0;


        logger.trace("Checking Hex ({}) for hazards.", hex.getCoords());
        Set<Integer> hazards = getHazardTerrainIds(hex);
        // No hazards were found, so nothing to worry about.
        if (hazards.isEmpty()) {
            logger.trace("No hazards found.");
            return 0;
        }

        // Calculate hazard value by terrain type.

        for (int hazard : hazards) {
            switch (hazard) {
                case Terrains.FIRE:
                    hazardValue += calcFireHazard(movingUnit, endHex);
                    break;
                case Terrains.MAGMA:
                    hazardValue += calcMagmaHazard(hex, endHex, movingUnit, jumpLanding, step);
                    break;
                case Terrains.BLACK_ICE:
                case Terrains.ICE:
                    hazardValue += calcIceHazard(movingUnit, hex, step, movePath, jumpLanding);
                    break;
                case Terrains.WATER:
                    if (!hazards.contains(Terrains.ICE)) {
                        hazardValue += calcWaterHazard(movingUnit, hex, step, movePath);
                    }
                    break;
                case Terrains.BUILDING:
                    hazardValue += calcBuildingHazard(step, movingUnit, jumpLanding, board);
                    break;
                case Terrains.BRIDGE:
                    hazardValue += calcBridgeHazard(movingUnit, hex, step, jumpLanding, board);
                    break;
                case Terrains.SNOW:
                    hazardValue += calcSnowHazard(hex, endHex, movingUnit);
                    break;
                case Terrains.RUBBLE:
                    hazardValue += calcRubbleHazard(hex, endHex, movingUnit, jumpLanding);
                    break;
                case Terrains.SWAMP:
                    hazardValue += calcSwampHazard(hex, endHex, movingUnit, jumpLanding);
                    break;
                case Terrains.MUD:
                    hazardValue += calcMudHazard(endHex, movingUnit);
                    break;
                case Terrains.TUNDRA:
                    hazardValue += calcTundraHazard(endHex, jumpLanding, movingUnit);
                    break;
                case Terrains.PAVEMENT:
                    // 1 in 3 chance to hit Black Ice on any given Pavement hex
                    hazardValue += calcIceHazard(movingUnit, hex, step, movePath, jumpLanding) / 3.0;
                    break;
                case Terrains.HAZARDOUS_LIQUID:
                    hazardValue += calcHazardousLiquidHazard(hex, endHex, movingUnit, step);
                    break;
                case Terrains.ULTRA_SUBLEVEL:
                    hazardValue += calcUltraSublevelHazard(endHex, movingUnit);
            }
        }

        logger.trace("Total Hazard = {}", hazardValue);

        return hazardValue;
    }

    private Set<Integer> getHazardTerrainIds(Hex hex) {
        var hazards = hex.getTerrainTypesSet();
        // Black Ice can appear if the conditions are favorable
        if (blackIce > 0) {
            hazards.retainAll(Terrains.HAZARDS_WITH_BLACK_ICE);
        } else {
            hazards.retainAll(Terrains.HAZARDS);
        }

        return hazards;
    }

    // Building collapse and basements are handled in PathRanker.validatePaths.
    private double calcBuildingHazard(MoveStep step, Entity movingUnit, boolean jumpLanding, Board board) {
        logger.trace("Checking Building ({}) for hazards.", step.getPosition());
        // Protos, BA and Infantry move through buildings freely.
        if (movingUnit.isProtoMek() ||
              movingUnit.isInfantry() ||
              movingUnit.isConventionalInfantry() ||
              movingUnit.isBattleArmor()) {
            logger.trace("Safe for infantry and protos (0).");
            return 0;
        }

        // Jumping onto a building is handled in PathRanker validatePaths.
        if (jumpLanding) {
            return 0;
        }

        // Get the odds of failing the piloting roll while moving through the building.
        double odds = (1.0 - (Compute.oddsAbove(movingUnit.getCrew().getPiloting()) / 100));
        logger.trace("Chance to fail piloting roll: {}", odds);
        // Hazard is based on potential damage taken.
        double dmg = board.getBuildingAt(step.getPosition()).getCurrentCF(step.getPosition()) / 10D;
        logger.trace("Potential building damage: {}", dmg);
        double hazard = dmg * odds;
        logger.trace("Total Hazard = {}", hazard);
        return hazard;
    }

    private double calcBridgeHazard(Entity movingUnit, Hex hex, MoveStep step, boolean jumpLanding, Board board) {
        logger.trace("Checking Bridge ({}) for hazards.", hex.getCoords());
        // If we are going to BWONGGG into a bridge from below, then it's treated as a
        // building.
        // Otherwise, bridge collapse checks have already been handled in validatePaths
        int bridgeElevation = hex.terrainLevel(Terrains.BRIDGE_ELEV);
        if ((bridgeElevation > step.getElevation()) &&
              (bridgeElevation <= (step.getElevation() + movingUnit.getHeight()))) {
            return calcBuildingHazard(step, movingUnit, jumpLanding, board);
        }

        return 0;
    }

    private double calcIceHazard(Entity movingUnit, Hex hex, MoveStep step, MovePath movePath, boolean jumpLanding) {
        logger.trace("Checking Ice ({}) for hazards.", hex.getCoords());
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above ice (0).");
            return 0;
        }

        // Infantry don't break ice.
        if (EntityMovementMode.INF_LEG == movingUnit.getMovementMode() ||
              EntityMovementMode.INF_MOTORIZED == movingUnit.getMovementMode() ||
              EntityMovementMode.INF_JUMP == movingUnit.getMovementMode() ||
              EntityMovementMode.INF_UMU == movingUnit.getMovementMode()) {
            logger.trace("Infantry on Ice (0).");
            return 0;
        }

        // Categorize a chance to skid / fall
        double hazard = 0.0;
        if (!movePath.isCareful()) {
            // Most falling and skidding damage is weight-based...
            double arbitraryHazard = movingUnit.getWeight();
            hazard += Math.round(arbitraryHazard *
                  (1 - (Compute.oddsAbove(movingUnit.getCrew().getPiloting()) / 100.0)));
            if (movingUnit.isReckless()) {
                // Double the hazard for Reckless
                hazard *= 2;
            }
        }

        // If there is no water under the ice, don't worry about breaking through.
        if (hex.depth() < 1) {
            logger.trace("No water under ice (0).");
            return hazard;
        }

        // Hazard is based on a chance to break through to the water underneath.
        double breakthroughMod = jumpLanding ? 0.5 : 0.1667;
        logger.trace("Chance to break through ice: {}", breakthroughMod);
        hazard += calcWaterHazard(movingUnit, hex, step, movePath) * breakthroughMod;
        logger.trace("Total Hazard = {}", hazard);
        // Changed this to UNIT_DESTRUCTION_FACTOR because she suicided too often.
        // No reason to be on the ice at all except as an absolute last resort.
        return UNIT_DESTRUCTION_FACTOR;
    }

    private double calcWaterHazard(Entity movingUnit, Hex hex, MoveStep step, MovePath movePath) {
        logger.trace("Checking Water ({}) for hazards.", hex.getCoords());
        // Puddles don't count.
        if (hex.depth() == 0) {
            logger.trace("Puddles don't count (0).");
            return 0;
        }

        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode() ||
              EntityMovementMode.NAVAL == movingUnit.getMovementMode()) {
            logger.trace("Hovering or swimming above water (0).");
            return 0;
        }

        // Amphibious units are safe (kind of the point).
        if (movingUnit.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ||
              movingUnit.hasWorkingMisc(MiscType.F_AMPHIBIOUS)) {
            logger.trace("Amphibious units are safe (0).");
            return 0;
        }

        // Submarine units should be fine; Orca-riding Infantry goes here.
        if (EntityMovementMode.SUBMARINE == movingUnit.getMovementMode()) {
            logger.trace("Submarine units are safe (0).");
            return 0;
        }

        // If we are crossing a bridge, then we'll be fine. Trust me.
        // 1. Determine bridge elevation
        // 2. If unit elevation is equal to bridge elevation, skip.
        if (hex.containsTerrain(Terrains.BRIDGE_ELEV)) {
            int bridgeElevation = hex.terrainLevel(Terrains.BRIDGE_ELEV);
            if (bridgeElevation == step.getElevation()) {
                logger.trace("Bridge elevation matches unit elevation (0).");
                return 0;
            }
        }

        // Most other units are automatically destroyed. UMU-equipped units _may_ not drown immediately, but all
        // other hazards (e.g., breaches, crush depth) still apply.
        if (!(movingUnit instanceof Mek ||
              movingUnit instanceof ProtoMek ||
              movingUnit instanceof BattleArmor ||
              movingUnit.hasUMU())) {
            logger.trace("Drowning (1000).");
            return UNIT_DESTRUCTION_FACTOR;
        }

        MoveStep lastStep = movePath.getLastStep();
        // Unsealed unit will drown.
        if (movingUnit instanceof Mek &&
              ((Mek) movingUnit).isIndustrial() &&
              !movingUnit.hasEnvironmentalSealing() &&
              (movingUnit.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) &&
              hex.depth() >= 1 &&
              step.equals(lastStep)) {
            double destructionFactor = hex.depth() >= 2 ? UNIT_DESTRUCTION_FACTOR : UNIT_DESTRUCTION_FACTOR * 0.5d;
            logger.trace("Industrial Meks drown too ({}).", destructionFactor);
            return destructionFactor;
        }

        // TODO: implement crush depth calcs (TO:AR pg. 40)

        // Find the submerged locations.
        Set<Integer> submergedLocations = new HashSet<>();
        for (int loc = 0; loc < movingUnit.locations(); loc++) {
            if (Mek.LOC_CENTER_LEG == loc && !(movingUnit instanceof TripodMek)) {
                continue;
            }

            if ((hex.depth() >= 2) || step.isProne() || !(movingUnit instanceof Mek)) {
                submergedLocations.add(loc);
                continue;
            }

            if (Mek.LOC_RIGHT_LEG == loc || Mek.LOC_LEFT_LEG == loc || Mek.LOC_CENTER_LEG == loc) {
                submergedLocations.add(loc);
                continue;
            }

            if ((movingUnit instanceof QuadMek) && (Mek.LOC_RIGHT_ARM == loc || Mek.LOC_LEFT_ARM == loc)) {
                submergedLocations.add(loc);
            }
        }
        logger.trace("Submerged locations: {}", submergedLocations);

        int hazardValue = 0;
        for (int loc : submergedLocations) {
            // Only locations without armor can breach in the movement phase.
            if (movingUnit.getArmor(loc) > 0) {
                logger.trace("Location {} is not breached (0).", loc);
                continue;
            }

            // Meks or ProtoMeks having a head or torso breach is deadly.
            // For other units, any breach is deadly.
            // noinspection ConstantConditions
            if ((Mek.LOC_HEAD == loc) ||
                  (Mek.LOC_CENTER_TORSO == loc) ||
                  (ProtoMek.LOC_HEAD == loc) ||
                  (ProtoMek.LOC_TORSO == loc) ||
                  (!movingUnit.isMek() && !movingUnit.isProtoMek())) {
                logger.trace("Location {} breached and critical (1000).", loc);
                return UNIT_DESTRUCTION_FACTOR;
            }

            // Add 50 points per potential breach location.
            logger.trace("Location {} breached (50).", loc);
            hazardValue += 50;
        }

        return hazardValue;
    }

    private double calcFireHazard(Entity movingUnit, boolean endHex) {
        logger.trace("Calculating fire hazard.");
        double hazardValue = 0;

        // Fireproof BA ignores fire.
        if ((movingUnit instanceof BattleArmor) && ((BattleArmor) movingUnit).isFireResistant()) {
            logger.trace("Fireproof BA ignores fire.");
            return 0;
        }

        // Tanks risk critical hits.
        if (movingUnit instanceof Tank) {
            logger.trace("Tank risks critical hit (25).");
            return 25;
        }

        // ProtoMeks risk location destruction.
        if (movingUnit instanceof ProtoMek) {
            logger.trace("ProtoMek risks location destruction (50).");
            return 50;
        }

        // Infantry and BA risk total destruction.
        if (movingUnit instanceof Infantry) {
            logger.trace("Infantry risks total destruction (1000).");
            return UNIT_DESTRUCTION_FACTOR;
        }

        // If this unit tracks heat, add the heat gain to the hazard value.
        if (movingUnit.getHeatCapacity() != Entity.DOES_NOT_TRACK_HEAT) {
            hazardValue += endHex ? 5 : 2;
            logger.trace("Heat gain ({}).", hazardValue);
        }

        return hazardValue;
    }

    private double calcMagmaHazard(Hex hex, boolean endHex, Entity movingUnit, boolean jumpLanding, MoveStep step) {
        logger.trace("Calculating magma hazard.");
        // Hovers / WiGE are normally unaffected.
        if ((EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode()) && !endHex) {
            logger.trace("Hovering above magma (0).");
            return 0;
        }

        double hazardValue = 0;
        int magmaLevel = hex.terrainLevel(Terrains.MAGMA);

        // Liquid magma.
        if (magmaLevel == 2) {
            return calcLavaHazard(endHex, jumpLanding, movingUnit, step);
        } else {
            double breakThroughMod = jumpLanding ? 0.5 : 0.1667;
            logger.trace("Chance to break through crust = {}", breakThroughMod);
            // Factor in the chance to break through.
            double lavalHazard = Math.round(calcLavaHazard(endHex, jumpLanding, movingUnit, step) * breakThroughMod);
            logger.trace("Lava hazard = {}", lavalHazard);
            hazardValue += lavalHazard;

            // Factor in heat.
            if (movingUnit.getHeatCapacity() != Entity.DOES_NOT_TRACK_HEAT) {
                double heatMod = (endHex ? 5 : 2) * (1 - breakThroughMod);
                hazardValue += heatMod;
                logger.trace("Heat gain = {}", heatMod);
            }
        }

        return hazardValue;
    }

    private double calcLavaHazard(boolean endHex, boolean jumpLanding, Entity movingUnit, MoveStep step) {
        logger.trace("Calculating lava hazard.");
        int unitDamageLevel = movingUnit.getDamageLevel();
        double dmg;

        // Hovers/VTOLs are unaffected _unless_ they end on the hex and are in danger of
        // losing mobility.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            if (!endHex) {
                logger.trace("Hovering/VTOL while traversing lava (0).");
                return 0;
            } else {
                // Estimate the chance of being disabled or immobilized over open lava; this is fatal!
                // Calc expected damage as ((current damage level [0 ~ 4]) / 4) * UNIT_DESTRUCTION_FACTOR
                dmg = (unitDamageLevel / 4.0) * UNIT_DESTRUCTION_FACTOR;
                logger.trace("Ending hover/VTOL movement over lava ({}).", dmg);
                return dmg;
            }
        }

        // Non-Mek units auto-destroyed.
        if (!(movingUnit instanceof Mek)) {
            logger.trace("Non-Mek instant destruction (1000).");
            return UNIT_DESTRUCTION_FACTOR;
        }

        double hazardValue = 0;
        double psrFactor = 1.0;

        // Adjust hazard by chance of getting stuck
        if (endHex && jumpLanding) {
            // Chance of getting stuck in magma is the chance of failing one PSR. Factor applied to damage should
            // also include the expected number of turns _not_ escaping.
            // Former is: %chance _not_ passing PSR
            // Latter is: N = log(desired failure to escape chance, e.g., 10%) / log(%chance fail PSR)
            logger.trace("Jumping onto lava hex, may get bogged down.");
            int pilotSkill = movingUnit.getCrew().getPiloting();
            int psrMod = 4;
            double oddsPSR = Compute.oddsAbove(pilotSkill + psrMod) / 100;
            double oddsBogged = (1.0 - oddsPSR);
            double expectedTurns = Math.log10(0.10) / Math.log10(oddsBogged);
            logger.trace("Chance to bog down = {}, expected turns = {}", oddsBogged, expectedTurns);
            psrFactor = 1.0 + oddsBogged + (expectedTurns);
        }

        // Factor in heat.
        double heat = endHex ? 10.0 : 5.0;
        hazardValue += heat;
        logger.trace("Heat gain = {}", heat);
        // Factor in the potential to suffer fatal damage.
        // Dependent on expected average damage / exposed remaining armor * UNIT_DESTRUCTION_FACTOR
        int exposedArmor;
        if (step.isProne()) {
            dmg = 7 * movingUnit.locations();
            exposedArmor = movingUnit.getTotalArmor();
            logger.trace("Prone Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else if (movingUnit instanceof BipedMek) {
            dmg = 14;
            exposedArmor = Stream.of(Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG).mapToInt(movingUnit::getArmor).sum();
            logger.trace("Biped Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else if (movingUnit instanceof TripodMek) {
            exposedArmor = Stream.of(Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG, Mek.LOC_CENTER_LEG)
                  .mapToInt(movingUnit::getArmor)
                  .sum();
            dmg = 21;
            logger.trace("Tripod Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else {
            exposedArmor = Stream.of(Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_ARM, Mek.LOC_RIGHT_ARM)
                  .mapToInt(movingUnit::getArmor)
                  .sum();
            dmg = 28;
            logger.trace("Quad Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        }
        hazardValue += (UNIT_DESTRUCTION_FACTOR * (dmg / Math.max(exposedArmor, 1)));

        // Multiply the total hazard value by the chance of getting stuck for 1 or more additional turns
        logger.trace("Total hazard = {}", hazardValue * psrFactor);
        return Math.round(hazardValue * psrFactor);
    }

    private double calcHazardousLiquidHazard(Hex hex, boolean endHex, Entity movingUnit, MoveStep step) {
        logger.trace("Calculating hazardous liquid hazard.");
        int unitDamageLevel = movingUnit.getDamageLevel();
        double dmg;

        // Hovers/VTOLs are unaffected _unless_ they end on the hex and are in danger of
        // losing mobility.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            if (!endHex) {
                logger.trace("Hovering/VTOL while traversing hazardous liquids (0).");
                return 0;
            } else {
                // Estimate the chance of being disabled or immobilized over open lava; this is fatal!
                // Calc expected damage as ((current damage level [0 ~ 4]) / 4) * UNIT_DESTRUCTION_FACTOR
                dmg = (unitDamageLevel / 4.0) * UNIT_DESTRUCTION_FACTOR;
                logger.trace("Ending hover/VTOL movement over lava ({}).", dmg);
                return dmg;
            }
        }

        dmg = (HazardousLiquidPoolUtil.AVERAGE_DAMAGE_HAZARDOUS_LIQUID_POOL *
              HazardousLiquidPoolUtil.getHazardousLiquidPoolDamageMultiplierForUnsealed(movingUnit)) /
              (HazardousLiquidPoolUtil.getHazardousLiquidPoolDamageDivisorForInfantry(movingUnit));

        // After all that math, let's make sure we do at least 1 damage
        // (.6 repeating when normalized for the HLP doing no damage 1/3 of the time)
        dmg = Math.max(dmg, 2.0 / 3.0);

        // Factor in the potential to suffer fatal damage.
        // Dependent on expected average damage / exposed remaining armor *
        // UNIT_DESTRUCTION_FACTOR
        int exposedArmor;
        double hazardValue = 0;
        if (step.isProne() || (hex.containsTerrain(Terrains.WATER) && hex.terrainLevel(Terrains.WATER) > 1)) {
            exposedArmor = movingUnit.getTotalArmor();
            logger.trace("Fully Submerged damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else if (movingUnit instanceof BipedMek) {
            exposedArmor = Stream.of(Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG).mapToInt(movingUnit::getArmor).sum();
            logger.trace("Biped Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else if (movingUnit instanceof TripodMek) {
            exposedArmor = Stream.of(Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG, Mek.LOC_CENTER_LEG)
                  .mapToInt(movingUnit::getArmor)
                  .sum();
            logger.trace("Tripod Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else if (movingUnit instanceof QuadMek) {
            exposedArmor = Stream.of(Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_ARM, Mek.LOC_RIGHT_ARM)
                  .mapToInt(movingUnit::getArmor)
                  .sum();
            logger.trace("Quad Mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        } else {
            exposedArmor = movingUnit.getTotalArmor();
            logger.trace("Fully Submerged non-mek damage = {}, exposed armor = {}", dmg, exposedArmor);
        }
        hazardValue += (UNIT_DESTRUCTION_FACTOR * (dmg / Math.max(exposedArmor, 1)));

        logger.trace("Total hazard = {}", hazardValue);
        return Math.round(hazardValue);
    }

    private double calcUltraSublevelHazard(boolean endHex, Entity movingUnit) {
        logger.trace("Calculating ultra sublevel hazard.");
        int unitDamageLevel = movingUnit.getDamageLevel();
        double dmg;

        // Hovers/VTOLs are unaffected _unless_ they end on the hex and are in danger of
        // losing mobility.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            if (!endHex) {
                logger.trace("Hovering/VTOL while traversing ultra sublevel (0).");
                return 0;
            } else if (movingUnit.getElevation() >
                  0) {  //elevation of 0 is on the ground (not airborne), which would be destroyed
                // Estimate chance of being disabled or immobilized over ultra sublevel; this is
                // fatal!
                // Calc expected damage as ((current damage level [0 ~ 4]) / 4) *
                // UNIT_DESTRUCTION_FACTOR
                dmg = (unitDamageLevel / 4.0) * UNIT_DESTRUCTION_FACTOR;
                logger.trace("Ending hover/VTOL movement over ultra sublevel ({}).", dmg);
                return dmg;
            }
        }
        logger.trace("Ground unit instant destruction from ultra sublevel ({}).", UNIT_DESTRUCTION_FACTOR);
        return UNIT_DESTRUCTION_FACTOR;
    }

    private double calcBogDownFactor(String name, boolean endHex, boolean jumpLanding, int pilotSkill, int modifier) {
        return calcBogDownFactor(name, endHex, jumpLanding, pilotSkill, modifier, true);
    }

    /**
     * Calculate a PSR-related factor for increasing the hazard of terrain where bogging down is possible
     *
     * @param name        Name of a terrain type, for logging.
     * @param endHex      If this is checking the final hex of a movement path or not.
     * @param jumpLanding Whether the unit will be jumping into the end hex or not.
     * @param pilotSkill  base pilot/driver/etc. skill used for the PSR checks to escape bogging down.
     * @param modifier    Modifier, based on unit type and terrain type
     * @param bogPossible whether the unit can actually get bogged own in this terrain type, or just calculating
     *
     * @return double Factor to multiply by terrain hazards.
     */
    private double calcBogDownFactor(String name, boolean endHex, boolean jumpLanding, int pilotSkill, int modifier,
          boolean bogPossible) {
        double factor;
        int effectiveSkill = pilotSkill + modifier;
        double oddsPSR = Math.max((Compute.oddsAbove(effectiveSkill) / 100.0), 0.0);
        double oddsBogged = 0.0;

        // Adjust hazard by chance of getting stuck
        if (endHex && jumpLanding) {
            // The chance of getting stuck in swamp/mud is the chance of failing one PSR, or
            // 100% if jumping.
            oddsBogged = 1.0;
            logger.trace("Jumping onto {} hex, would get bogged down.", name);
        } else if (!jumpLanding) {
            oddsBogged = 1.0 - oddsPSR;
            logger.trace("Entering onto {} hex, chance to bog down = {}", name, oddsBogged);
        }
        // (Reuse PSR odds to avoid infinite trapped time on turns when jumping into
        // terrain causes 100% bog-down)
        double expectedTurns = ((1 - oddsPSR) < 1.0) ?
              Math.log10(0.10) / Math.log10(1 - oddsPSR) :
              UNIT_DESTRUCTION_FACTOR;

        if (bogPossible) {
            logger.trace("Chance to bog down = {}, expected turns = {}", oddsBogged, expectedTurns);
        }
        factor = 1.0 + oddsBogged + (expectedTurns);

        return factor;
    }

    private double calcSnowHazard(Hex hex, boolean endHex, Entity movingUnit) {
        logger.trace("Checking Snow ({}) for hazards.", hex.getCoords());
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above snow (0).");
            return 0;
        }

        if (hex.terrainLevel(Terrains.SNOW) > 1) {
            // PSR checks _to bog down_ and _escape bogged down_ are at (mod - 1); all
            // others are at a +1 mod
            int psrMod = 0;
            // Infantry use 4+ check instead of Pilot / Driving skill
            int pilotSkill = (movingUnit.isInfantry()) ? 4 : movingUnit.getCrew().getPiloting();
            double hazard;

            // Base hazard is arbitrarily set to 10
            hazard = 10 * calcBogDownFactor("Deep Snow", endHex, false, pilotSkill, psrMod);

            logger.trace("Deep snow hazard = {}", hazard);
            return Math.round(hazard);
        }

        // Thin snow poses no hazard; MP malus accounted for elsewhere.
        return 0;
    }

    private double calcSwampHazard(Hex hex, boolean endHex, Entity movingUnit, boolean jumpLanding) {
        logger.trace("Checking Swamp ({}) for hazards.", hex.getCoords());
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above swamp (0).");
            return 0;
        }

        // Base hazard is the chance of becoming Quicksand and destroying this unit
        // If currently Swamp, could become Quicksand if 12 on 2d6 is rolled.
        // If already Quicksand...
        boolean quicksand = hex.terrainLevel(Terrains.SWAMP) > 1;
        String type = (quicksand) ? "Quicksand" : "Swamp";
        double quicksandChance = (quicksand) ? 1.0 : 1 / 36.0;
        // Height + 1 turns to fully sink and be destroyed
        double hazard = quicksandChance * UNIT_DESTRUCTION_FACTOR / (1 + movingUnit.getHeight());
        logger.trace("Base hazard value: {}", hazard);
        // Mod is to difficulty, not to PSR roll results
        // Quicksand makes PSRs an additional +3! Otherwise +1 for Meks, +2 for all
        // other types
        int psrMod = (quicksand) ? +3 : (movingUnit instanceof Mek) ? +1 : +2;

        // Infantry use 4+ check instead of Pilot / Driving skill
        int pilotSkill = (movingUnit.isInfantry()) ? 4 : movingUnit.getCrew().getPiloting();

        double factor = calcBogDownFactor(type, endHex, jumpLanding, pilotSkill, psrMod);
        logger.trace("Factor applied to hazard value: {}", factor);
        // The danger is increased if pilot skill is low, as the chance of succumbing or
        // getting
        // permanently stuck increases!
        hazard = hazard * factor;

        return Math.round(hazard);
    }

    private double calcMudHazard(boolean endHex, Entity movingUnit) {
        logger.trace("Checking Mud for hazards.");
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above Mud (0).");
            return 0;
        }

        // PSR checks _to bog down_ and _escape bogged down_ are at (mod - 1); all
        // others are at +1 mod
        int psrMod = 0;
        // Infantry use 4+ check instead of Pilot / Driving skill
        int pilotSkill = (movingUnit.isInfantry()) ? 4 : movingUnit.getCrew().getPiloting();
        double hazard;

        if (movingUnit.isMek()) {
            // The only hazard is the +1 to PSRs, which are challenging to quantify.
            // Even jumping Meks cannot bog down in mud.
            hazard = calcBogDownFactor("Mud", endHex, false, pilotSkill, psrMod, false);
        } else {
            // Mud is more dangerous for units that can actually bog down
            // Base hazard is arbitrarily set to 10
            hazard = 10 * calcBogDownFactor("Mud", endHex, false, pilotSkill, psrMod);
        }
        logger.trace("Mud hazard = {}", hazard);
        return Math.round(hazard);
    }

    private double calcTundraHazard(boolean endHex, boolean jumpLanding, Entity movingUnit) {
        logger.trace("Checking Tundra for hazards.");
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above Tundra (0).");
            return 0;
        }

        // PSR checks _to bog down_ and _escape bogged down_ are at (mod - 1); all
        // others are at +1 mod
        int psrMod = 0;
        // Infantry use 4+ check instead of Pilot / Driving skill
        int pilotSkill = (movingUnit.isInfantry()) ? 4 : movingUnit.getCrew().getPiloting();
        double hazard;

        // Base hazard is arbitrarily set to 10
        hazard = 10 * calcBogDownFactor("Tundra", endHex, jumpLanding, pilotSkill, psrMod);
        logger.trace("Tundra hazard = {}", hazard);
        return Math.round(hazard);
    }

    private double calcRubbleHazard(Hex hex, boolean endHex, Entity movingUnit, boolean jumpLanding) {
        logger.trace("Checking Rubble ({}) for hazards.", hex.getCoords());
        // Hover units are above the surface.
        if (EntityMovementMode.HOVER == movingUnit.getMovementMode() ||
              EntityMovementMode.WIGE == movingUnit.getMovementMode()) {
            logger.trace("Hovering above Rubble (0).");
            return 0;
        }

        double hazard = 0;

        boolean caresAboutRubble = ((!jumpLanding || endHex) &&
              (hex.terrainLevel(Terrains.RUBBLE) > 0) &&
              (hex.terrainLevel(Terrains.PAVEMENT) == Terrain.LEVEL_NONE) &&
              movingUnit.canFall());

        if (caresAboutRubble) {
            // PSR checks are at +0 for Rubble levels up to 6, Ultra, which is +1
            int psrMod = (hex.terrainLevel(Terrains.RUBBLE) < 6) ? 0 : 1;
            if (movingUnit.hasAbility(OptionsConstants.PILOT_TM_MOUNTAINEER)) {
                psrMod -= 1;
            }
            int pilotSkill = movingUnit.getCrew().getPiloting();

            // The only hazard is the +1 to PSRs, which are challenging to quantify
            hazard = calcBogDownFactor("Rubble", endHex, jumpLanding, pilotSkill, psrMod, false);
        }
        logger.trace("Total Hazard = {}", hazard);
        return Math.round(hazard);
    }

    /**
     * Simple data structure that holds a separate firing and physical damage number.
     */
    public static class FiringPhysicalDamage {
        public double firingDamage;
        public double physicalDamage;

        public double getMaximumDamageEstimate() {
            return firingDamage + physicalDamage;
        }
    }
}
