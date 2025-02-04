/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */
package megamek.client.bot.princess;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tracks enemy units and calculates threat scores based on their positions and capabilities.
 * @author Luana Coppio
 */
public class EnemyTracker {
    private final Map<Integer, EnemyProfile> enemyProfiles = new HashMap<>();

    private final Princess owner;

    public EnemyTracker(Princess owner) {
        this.owner = owner;
    }

    private Princess getOwner() {
        return owner;
    }

    /**
     * Returns a list of enemy entities with the highest threat score adjusted to your position.
     * @param coords The position to calculate the threat score from.
     * @param limit The maximum number of entities to return.
     * @return A list of enemy entities.
     */
    public List<Entity> getPriorityTargets(Coords coords, int limit) {
        if (enemyProfiles.isEmpty()) {
            updateThreatAssessment(coords);
        }
        if (enemyProfiles.isEmpty()) {
            return Collections.emptyList();
        }
        return enemyProfiles.values().stream()
            .sorted(Comparator.comparingDouble(e -> ((EnemyProfile) e).getThreatScoreAdjusted(coords)).reversed())
            .map(e -> owner.getGame().getEntity(e.id()))
            .filter(Objects::nonNull)
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Returns a list of enemy entities with the highest threat score adjusted to the targetable.
     * @param targetable The targetable position to calculate the threat score from.
     * @return A list of enemy entities with 25% or higher chance of hitting the attack in that target.
     */
    public List<Entity> getPriorityTargets(Targetable targetable) {
        if (enemyProfiles.isEmpty()) {
            updateThreatAssessment(targetable.getPosition());
        }
        if (enemyProfiles.isEmpty()) {
            return Collections.emptyList();
        }
        return enemyProfiles.values().stream()
            .sorted(Comparator.comparingDouble(e -> ((EnemyProfile) e).getThreatScoreAdjusted(targetable.getPosition())).reversed())
            .map(e -> owner.getGame().getEntity(e.id()))
            .filter(Objects::nonNull)
            .filter(e -> hitChance(owner.getGame(), e, targetable) > 0.25)
            .collect(Collectors.toList());
    }

    /**
     * Calculates the hit chance of an attack from the entity to the target.
     * @param game The game instance.
     * @param entity The entity that is attacking.
     * @param target The targetable that is being attacked.
     * @return The hit chance of the attack.
     */
    public static double hitChance(Game game, Entity entity, Targetable target) {
        ToHitData toHit = WeaponAttackAction.toHit(
            game, entity.getId(), target);
        if (toHit.getValue() <= 12) {
            return 1 - (1 - toHit.getValue() / 36.0);
        } else {
            return 0.0;
        }
    }

    /**
     * Calculates the threat score of against a cluster to determine how exposed they are
     * @param clusterCenter The center of the cluster.
     * @return The threat score against the cluster.
     */
    public double getClusterThreatScore(Coords clusterCenter) {
        return getPriorityTargets(clusterCenter, 5).stream()
            .mapToDouble(e -> {
                double distance = e.getPosition().distance(clusterCenter);
                double damagePotential = Compute.computeTotalDamage(e.getWeaponList());
                double mobilityThreat = e.getRunMP() / 10.0;
                return (damagePotential + mobilityThreat) / (distance + 1);
            })
            .sum();
    }

    private int lastRoundUpdate = -1;

    /**
     * Updates the threat assessment of the enemy units.
     * @param currentSwarmCenter The current center of the swarm.
     */
    public void updateThreatAssessment(Coords currentSwarmCenter) {
        var visibleEnemies = getOwner().getEnemyEntities();
        var currentRound = getOwner().getGame().getCurrentRound();
        if (lastRoundUpdate == getOwner().getGame().getCurrentRound() && enemyProfiles.size() == visibleEnemies.size()) {
            return;
        }
        lastRoundUpdate = getOwner().getGame().getCurrentRound();
        // 1. Update known enemy positions/states
        visibleEnemies.forEach(enemy -> {
            EnemyProfile profile = enemyProfiles.computeIfAbsent(enemy.getId(),
                id -> new EnemyProfile(enemy));

            var averageDamagePotential = Stream.of(
                FireControl.getMaxDamageAtRange(enemy, enemy.getMaxWeaponRange(), false, false),
                FireControl.getMaxDamageAtRange(enemy, 1, false, false))
                .mapToDouble(d -> d)
                .average()
                .orElse(0.0);

            profile.update(
                enemy.getPosition(),
                averageDamagePotential,
                currentRound
            );
        });

        // remove all the dead entities and the entities that have not been seen for 3 turns
        enemyProfiles.entrySet().removeIf(entry ->
            (owner.getGame().getEntity(entry.getValue().id()) == null)
                || (entry.getValue().getLastSeenTurn() < currentRound - 3)
        );

        // 3. Calculate threat scores
        enemyProfiles.values().forEach(profile ->
            profile.calculateThreatScore(currentSwarmCenter)
        );
    }

    private static class EnemyProfile implements Comparable<Double> {
        private final int entityId;
        private Coords lastPosition;
        private double damagePotential;
        private int lastSeenTurn;
        private double threatScore;
        private final List<Coords> movementHistory = new Vector<>();

        public EnemyProfile(Entity entity) {
            this.entityId = entity.getId();
        }

        public int id() {
            return entityId;
        }

        public Coords getLastPosition() {
            return lastPosition;
        }

        public double getDamagePotential() {
            return damagePotential;
        }

        public int getLastSeenTurn() {
            return lastSeenTurn;
        }

        public double getThreatScore() {
            return threatScore;
        }

        public double getThreatScoreAdjusted(Coords coords) {
            return threatScore * (1.0 / (1 + coords.distance(lastPosition)));
        }

        public void update(Coords newPosition, double newDamage, int currentTurn) {
            movementHistory.add(newPosition);
            if(movementHistory.size() > 5) {
                movementHistory.remove(0);
            }

            lastPosition = newPosition;
            damagePotential = newDamage;
            lastSeenTurn = currentTurn;
        }

        public void calculateThreatScore(Coords swarmCenter) {
            // Distance-based threat
            double distanceMod = 1.0 / (1 + lastPosition.distance(swarmCenter));

            // Damage potential
            double damageMod = damagePotential * 0.8;

            // Movement pattern analysis
            double movementThreat = analyzeMovementPattern();

            threatScore = (distanceMod * 2.0) + damageMod + movementThreat;
        }

        private double analyzeMovementPattern() {
            if(movementHistory.size() < 3) {
                return 0;
            }

            // Calculate movement direction consistency
            Coords oldest = movementHistory.get(0);
            Coords newest = movementHistory.get(movementHistory.size()-1);
            double directDistance = oldest.distance(newest);
            double actualDistance = movementHistory.stream()
                .mapToDouble(c -> c.distance(movementHistory.get(0)))
                .sum();

            // Higher score for direct approaches
            return (directDistance / actualDistance) * 2.0;
        }

        @Override
        public int compareTo(Double o) {
            return Double.compare(threatScore, o);
        }
    }
}
