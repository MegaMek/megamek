/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoresolve.damage;

import megamek.common.*;
import megamek.logging.MMLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Luana Coppio
 */
public interface DamageApplier<E extends Entity> {
    MMLogger logger = MMLogger.create(DamageApplier.class);

    record HitDetails(HitData hit, int damageToApply, int setArmorValueTo, boolean hitInternal, int hitCrew) {
        public HitDetails withCrewDamage(int crewDamage) {
            return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, crewDamage);
        }
        public HitDetails killsCrew() {
            return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, Crew.DEATH);
        }
    }

    E entity();

    EntityFinalState entityFinalState();

    default boolean crewMayDie() {
        return !crewMustSurvive() && !noCrewDamage();
    }

    default boolean entityMustSurvive() {
        return entityFinalState().entityMustSurvive;
    }

    default boolean noCrewDamage() {
        return entityFinalState().noCrewDamage;
    }

    default boolean crewMustSurvive() {
        return entityFinalState().crewMustSurvive;
    }

    default boolean entityMystBeDevastated() {
        return entityFinalState().entityMystBeDevastated;
    }

    /**
     * Applies damage to the entity in clusters of a given size.
     * This is USUALLY the function you will want to use.
     *
     * @param dmg         the total damage to apply
     * @param clusterSize the size of the clusters
     */
    default int applyDamageInClusters(int dmg, int clusterSize) {
        int totalDamage = dmg;
        int damageApplied = 0;
        while (totalDamage > 0) {
            if (entity().isCrippled() && entity().getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED) {
                // devastated units don't need to take any damage anymore
                break;
            }
            var clusterDamage = Math.min(totalDamage, clusterSize);
            applyDamage(clusterDamage);
            totalDamage -= clusterDamage;
            damageApplied += clusterDamage;
        }
        if (entityMystBeDevastated()) {
            damageApplied += devastateUnit();
        }
        return damageApplied;
    }

    int devastateUnit();

    /**
     * Applies damage to the entity.
     *
     * @param dmg the total damage to apply
     */
    default void applyDamage(int dmg) {
        int hitLocation = getRandomHitLocation();
        if (hitLocation == -1) {
            entity().setDestroyed(true);
            return;
        }
        HitData hit = getHitData(hitLocation);
        HitDetails hitDetails = setupHitDetails(hit, dmg);
        applyDamage(hitDetails);
    }

    /**
     * Returns the location to hit.
     *
     * @return returns a valid random location to be hit.
     */
    default int getRandomHitLocation() {
        var entity = entity();
        List<Integer> validLocations = new ArrayList<>();
        for (int i = 0; i < entity.locations(); i++) {
            if (entity.getOArmor(i) <= 0) {
                continue;
            }
            var locationIsNotBlownOff = !entity.isLocationBlownOff(i);
            var locationIsNotDestroyed = entity.getInternal(i) > 0;
            if (locationIsNotBlownOff && locationIsNotDestroyed) {
                validLocations.add(i);
            }
        }
        Collections.shuffle(validLocations);
        return validLocations.isEmpty() ? -1 : validLocations.get(0);
    }

    /**
     * Hits the entity with the given hit details.
     *
     * @param hitDetails the hit details
     */
    default void applyDamage(HitDetails hitDetails) {
        hitDetails = damageArmor(hitDetails);
        if (hitDetails.hitInternal()) {
            hitDetails = damageInternals(hitDetails);
        }
        tryToDamageCrew(hitDetails.hitCrew());
    }

    /**
     * Destroys the location after the crew has been ejected.
     */
    default void destroyLocationAfterEjection() {
        // default implementation does nothing
    }

    /**
     * Applies damage to the internals of the entity.
     *
     * @param hitDetails the hit details
     */
    default HitDetails damageInternals(HitDetails hitDetails) {
        HitData hit = hitDetails.hit();
        var entity = entity();
        int currentInternalValue = entity.getInternal(hit);
        int newInternalValue = Math.max(currentInternalValue + hitDetails.setArmorValueTo(), entityMustSurvive() ? 1 : 0);
        entity.setArmor(0, hit);
        logger.trace("[{}] Damage: {} - Internal at: {}", entity.getDisplayName(), hitDetails.damageToApply(), newInternalValue);
        entity.setInternal(newInternalValue, hit);
        applyDamageToEquipments(hit);
        if (newInternalValue == 0) {
            hitDetails = destroyLocation(hitDetails);
        }
        return hitDetails;
    }

    /**
     * Destroys the location of the entity. This one is used when you have the HitData.
     *
     * @param hitDetails the hit details with information about the location
     */
    default HitDetails destroyLocation(HitDetails hitDetails) {
        destroyLocation(hitDetails.hit().getLocation());
        return hitDetails.killsCrew();
    }

    /**
     * Destroys the location of the entity. This one is used when the location is already known.
     *
     * @param location the location index in the entity
     */
    default void destroyLocation(int location) {
        var entity = entity();
        logger.trace("[{}] Destroying location {}", entity.getDisplayName(), location);
        entity.destroyLocation(location);
        entity.setDestroyed(true);
        setEntityDestroyed(entity);
    }

    /**
     * Tries to damage the crew of the entity. If the crew is dead, the entity is marked as destroyed.
     * The crew won't be damaged if they are already ejected or already dead.
     * This function also does try to not outright kill the crew, as it has proven to be a bit too deadly.
     * @param hitCrew the amount of hits to apply to ALL crew
     */
    default void tryToDamageCrew(int hitCrew) {
        if (hitCrew == 0 || noCrewDamage()) {
            return;
        }

        var entity = entity();
        Crew crew = entity.getCrew();
        if (crew == null || crew.isEjected() || crew.isDead()) {
            return;
        }
        var hits = tryToNotKillTheCrew(hitCrew, crew);

        crew.setHits(hits, 0);
        logger.trace("[{}] Crew hit ({} hits)", entity().getDisplayName(), crew.getHits());
        if (crew.isDead()) {
            logger.trace("[{}] Crew died", entity().getDisplayName());
            entity.setDestroyed(true);
            setEntityDestroyed(entity);
        }
    }

    /**
     * Tries to not kill the crew. This function will set the amount of hits the crew will take.
     * If the crew must survive, the crew will try to eject instead, and if they can't be
     * ejected, it will set the total hits to DEATH - 1 (so... 5).
     * @param hitCrew the amount of hits to apply to ALL crew
     * @param crew the crew to apply the hits to
     * @return the amount of hits to apply to the crew
     */
    private Integer tryToNotKillTheCrew(int hitCrew, Crew crew) {
        if (noCrewDamage()) {
            return 0;
        }
        var hits = Math.min(crew.getHits() + hitCrew, Crew.DEATH);

        if (hits == Crew.DEATH) {
            if (crewMustSurvive()) {
                hits = Compute.randomIntInclusive(4) + 1;
            }
            if (tryToEjectCrew()) {
                destroyLocationAfterEjection();
            }
        }
        return hits;
    }

    /**
     * Sets the entity as destroyed.
     * @param entity entity to be set as destroyed
     * @param <E> the type of the entity
     */
    static <E extends Entity> void setEntityDestroyed(E entity) {
        if (entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_DEVASTATED) {
            entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
            entity.setSalvage(true);
            logger.trace("[{}] Entity destroyed", entity.getDisplayName());
        }
    }

    /**
     * Sets the entity as destroyed by ejection if possible.
     * @param entity entity to be set as destroyed
     * @param <E> the type of the entity
     */
    static <E extends Entity> void setEntityDestroyedByEjection(E entity) {
        if (entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_DEVASTATED) {
            entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_EJECTED);
            entity.setSalvage(true);
            logger.trace("[{}] Entity destroyed by ejection", entity.getDisplayName());
        }
    }

    /**
     * Tries to eject the crew of the entity if possible.
     *
     * @return true if the crew was ejected
     */
    default boolean tryToEjectCrew() {
        var entity = entity();
        var crew = entity.getCrew();
        if (crew == null || crew.isEjected() || !entity().isEjectionPossible() || entityMustSurvive()) {
            return false;
        }
        crew.setEjected(true);
        entity.setDestroyed(true);
        setEntityDestroyedByEjection(entity);
        logger.trace("[{}] Crew ejected", entity().getDisplayName());
        return true;
    }

    /**
     * Applies damage to the equipments of the entity.
     *
     * @param hit the hit data with information about the location
     */
    default void applyDamageToEquipments(HitData hit) {
        var entity = entity();
        var criticalSlots = entity.getCriticalSlots(hit.getLocation());
        Collections.shuffle(criticalSlots);
        for (CriticalSlot slot : criticalSlots) {
            if (slot != null && slot.isHittable() && !slot.isHit() && !slot.isDestroyed()) {
                slot.setHit(true);
                slot.setDestroyed(true);
                logger.trace("[{}] Equipment destroyed: {}", entity.getDisplayName(), slot);
                break;
            }
        }
    }

    /**
     * Applies damage only to the armor of the entity.
     *
     * @param hitDetails the hit details
     */
    default HitDetails damageArmor(HitDetails hitDetails) {
        var currentArmorValue = Math.max(hitDetails.setArmorValueTo(), 0);
        entity().setArmor(currentArmorValue, hitDetails.hit());
        logger.trace("[{}] Damage: {} - Armor at: {}", entity().getDisplayName(), hitDetails.damageToApply(), currentArmorValue);
        return hitDetails;
    }

    /**
     * Returns the hit data for the given hit location.
     *
     * @param hitLocation the hit location
     * @return the hit data
     */
    default HitData getHitData(int hitLocation) {
        return new HitData(hitLocation, false, HitData.EFFECT_NONE);
    }

    /**
     * Sets up the hit details for the given hit and damage.
     *
     * @param hit           the hit data
     * @param damageToApply the damage to apply
     * @return the hit details
     */
    default HitDetails setupHitDetails(HitData hit, int damageToApply) {
        int currentArmorValue = entity().getArmor(hit);
        int setArmorValueTo = currentArmorValue - damageToApply;
        boolean hitInternal = setArmorValueTo < 0;
        int hitCrew = hitInternal ? 1 : 0;

        return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, hitCrew);
    }
}
