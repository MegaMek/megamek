/*
* ZellbrigenAdjudicator.java
*
* Copyright (C) 2019 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This class provides adjudication to attack actions as to whether or not
 * the rules of Zellbrigen are still in play.
 * 
 * @implNote This class is not thread-safe.
 */
public class ZellbrigenAdjudicator {

    /**
     * These units are dishonorable and can be freely targetted.
     */
    private final Set<Integer> dishonorableUnits = new HashSet<>();

    /**
     * Maps from TARGET to SHOOTERS
     */
    private final Map<Integer, Set<DeclaredAttack>> targettedBy = new HashMap<>();

    /**
     * Maps from SHOOTER to TARGETS
     */
    private final Map<Integer, Set<DeclaredAttack>> targetting = new HashMap<>();

    /**
     * Maps declared attacks to their order in combat.
     */
    private final Map<DeclaredAttack, Integer> declaredAttacks = new HashMap<>();

    /**
     * This value indicates whether or not the rules of Zellbrigen have been broken.
     */
    private boolean isZellbrigenBroken;

    /**
     * Gets a value indicating whether or not Zellbrigen has been broken,
     * and the fight can become a free-for-all without losing honor.
     * @return A value indicating whether or not Zellbrigen has been broken.
     */
    public boolean isZellbrigenBroken() {
        return isZellbrigenBroken;
    }

    /**
     * Sets a value indicating whether or not Zellbrigen has been broken.
     * @param b True if Zellbrigen has been broken, otherwise false.
     */
    public void setIsZellbrigenBroken(boolean b) {
        isZellbrigenBroken = b;
    }
    
    /**
     * Gets the set of dishonorable units per the rules of Zellbrigen.
     * @return A set of dishonorable units.
     */
    public Set<Integer> getDishonorableUnitIds() {
        return Collections.unmodifiableSet(new HashSet<>(dishonorableUnits));
    }

    /**
     * Resets the adjudicator to its initial conditions.
     */
    public void reset() {
        dishonorableUnits.clear();
        targettedBy.clear();
        targetting.clear();
        declaredAttacks.clear();
        setIsZellbrigenBroken(false);
    }

    /**
     * Gets a value indicating whether or not a given target is honorable under
     * the rules of Zellbrigen.
     * @param shooter The entity doing the shooting.
     * @param target The intended target of the entity.
     * @return A value indicating whether or not shooting the target is honorable.
     */
    public boolean isHonorableTarget(Entity shooter, Entity target) {
        // 0. If the rules of Zellbrigen have been broken, everybody is a target.
        if (isZellbrigenBroken()) {
            return true;
        }

        // 1. If the shooter or target does not participate in Zellbrigen,
        //    they are free to trade shots.
        if (!participatesInZellbrigen(shooter) || !participatesInZellbrigen(target)) {
            return true;
        }

        int shooterId = shooter.getId();
        int targetId = target.getId();

        // 2. You can (and should) shoot dezgra units.
        if (dishonorableUnits.contains(targetId)) {
            return true;
        }

        DeclaredAttack attack = new DeclaredAttack(shooterId, targetId);

        // 3. If we've already declared this attack, let it through.
        if (declaredAttacks.containsKey(attack)) {
            return true;
        }

        Set<DeclaredAttack> whoIsShootingAtMe = getWhoIsTargetting(shooterId);
        
        // 4. You can shoot people shooting at you.
        for (DeclaredAttack shootingAtMe : whoIsShootingAtMe) {
            if (shootingAtMe.shooterId == targetId) {
                return true;
            }
        }

        Set<DeclaredAttack> whoAmIShootingAt = getDeclaredTargetsFor(shooterId);
        Set<DeclaredAttack> whoIsMyTargetShootingAt = getDeclaredTargetsFor(targetId);
        Set<DeclaredAttack> whoIsShootingAtMyTarget = getWhoIsTargetting(targetId);

        // 5. If you're not shooting at anybody...
        if (whoAmIShootingAt.isEmpty()) {
            // ...you can hit anyone who is undeclared
            return whoIsShootingAtMyTarget.isEmpty()
                && whoIsMyTargetShootingAt.isEmpty();
        }

        // 6. Otherwise, this is the hard part about Zellbrigen...
        // If you struck first, you can always add new folks to YOUR fun,
        // but they can't bring somebody else into the foray.

        int myAttackOrder = minimumAttackOrder(whoAmIShootingAt);
        int myAttackersBestOrder = minimumAttackOrder(whoIsShootingAtMe);

        return whoIsMyTargetShootingAt.isEmpty()
            && whoIsShootingAtMyTarget.isEmpty()
            && myAttackOrder < myAttackersBestOrder;
    }

    /**
     * Calculates the minimum attack order within a collection of attacks.
     * @param attacks A collection of attacks.
     * @return The order of the first attack made.
     */
    private int minimumAttackOrder(Collection<DeclaredAttack> attacks) {
        // ASSUMPTION: Every attack in attacks is in declaredAttacks

        int attackOrder = Integer.MAX_VALUE;
        for (DeclaredAttack attack : attacks) {
            attackOrder = Math.min(attackOrder, declaredAttacks.get(attack));
        }
        return attackOrder;
    }

    /**
     * Gets the set of entities targetting the given entity.
     * @param entityId The unique identifier of the entity.
     * @return A set of entity IDs targetting the given entity.
     */
    private Set<DeclaredAttack> getWhoIsTargetting(int entityId) {
        return targettedBy.getOrDefault(entityId, Collections.emptySet());
    }

    /**
     * Gets the set of entities the given entity is targetting.
     * @param entityId The unique identifier of the entity.
     * @return A set of entity IDs the given entity is targetting.
     */
    private Set<DeclaredAttack> getDeclaredTargetsFor(int entityId) {
        return targetting.getOrDefault(entityId, Collections.emptySet());
    }
    
    /**
     * Tracks a target declared by an entity.
     * 
     * This method should be used to track targets explicitly made by other players.
     * 
     * @param shooter The shooter declaring the target.
     * @param target The target of the shooter.
     */
    public void trackDeclaredTarget(Entity shooter, Entity target) {
        tryDeclaringTarget(shooter, target, isHonorableTarget -> true);
    }

    /**
     * Tries to declare an attack on a target, but only if the target is honorable.
     * 
     * This method can be used to ensure only honorable targets are attacked by a bot,
     * or to warn a user that their desired action will bring them dishonor.
     * 
     * @param shooter The entity doing the shooting.
     * @param target The entity potentially being attacked.
     * @return A value indicating whether or not the target was declared by the shooter.
     */
    public boolean tryDeclaringHonorableTarget(Entity shooter, Entity target) {
        return tryDeclaringTarget(shooter, target, isHonorableTarget -> isHonorableTarget);
    }

    /**
     * Tries to declare an attack on a target, but only if the given predicate indicates
     * the target should be attacked.
     * 
     * This method can be used to implement dezgra actions by a bot with some probability.
     * 
     * @param shooter The entity doing the shooting.
     * @param target The entity potentially being attacked.
     * @param shouldDeclarePredicate A predicate which recieves a value indicating if the target
     *                               is honorable, and returns a value indicating if the shooter
     *                               will be attacking the target.
     * @return A value indicating whether or not the target was declared by the shooter.
     */
    public boolean tryDeclaringTarget(Entity shooter, Entity target, Predicate<Boolean> shouldDeclarePredicate) {
        boolean isHonorableTarget = isHonorableTarget(shooter, target);

        if (shouldDeclarePredicate.test(isHonorableTarget)) {
            trackDeclareTargetInternal(shooter, target);

            if (!isHonorableTarget) {
                trackDishonorableTarget(shooter);
                setIsZellbrigenBroken(true);
            }

            return true;
        }

        return false;
    }

    /**
     * Gets a value indicating if the entity participates in Zellbrigen rules.
     * @param entity The entity in question.
     * @return True if the entity should participate in Zellbrigen rules,
     *         otherwise false.
     */
    private boolean participatesInZellbrigen(Entity entity) {
        return !entity.hasETypeFlag(Entity.ETYPE_INFANTRY)
            && !entity.hasETypeFlag(Entity.ETYPE_TANK);
    }

    private void trackDeclareTargetInternal(Entity shooter, Entity target) {
        if (!participatesInZellbrigen(shooter) || !participatesInZellbrigen(target)) {
            return;
        }

        DeclaredAttack attack = new DeclaredAttack(shooter.getId(), target.getId());
        if (!declaredAttacks.containsKey(attack)) {
            declaredAttacks.put(attack, declaredAttacks.size());
            targettedBy.computeIfAbsent(target.getId(), id -> new HashSet<>())
                .add(attack);
            targetting.computeIfAbsent(shooter.getId(), id -> new HashSet<>())
                .add(attack);
        }
    }

    /**
     * Tracks an entity which has brought dishonor upon themselves
     * and their clan.
     * @param dezgra The dezgra entity.
     */
    public void trackDishonorableTarget(Entity dezgra) {
        dishonorableUnits.add(dezgra.getId());
    }

    /**
     * Tracks an entity which has been destroyed.
     * @param target The entity which has been destroyed.
     */
    public void trackDestroyedTarget(Entity target) {
        int id = target.getId();

        targettedBy.remove(id);
        for (Set<DeclaredAttack> shooters : targettedBy.values()) {
            shooters.removeIf(a -> a.shooterId == id);
        }

        targetting.remove(id);
        for (Set<DeclaredAttack> targets : targetting.values()) {
            targets.removeIf(a -> a.targetId == id);
        }
    }

    /**
     * Tracks a declared attack.
     */
    private class DeclaredAttack {
        public final int shooterId, targetId;

        public DeclaredAttack(int shooterId, int targetId) {
            this.shooterId = shooterId;
            this.targetId = targetId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(shooterId, targetId);
        }

        public boolean equals(Object o) {
            if (!(o instanceof DeclaredAttack)) {
                return false;
            }
            DeclaredAttack other = (DeclaredAttack)o;
            return shooterId == other.shooterId
                && targetId == other.targetId;
        }
    }
}
