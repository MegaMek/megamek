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
import megamek.common.equipment.WeaponMounted;
import megamek.logging.MMLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Luana Coppio
 */
public interface DamageApplier<E extends Entity> {
    MMLogger logger = MMLogger.create(DamageApplier.class);

    record HitDetails(HitData hit, int damageToApply, int setArmorValueTo, int hitInternal, int hitCrew) {
        public HitDetails withCrewDamage(int crewDamage) {
            return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, crewDamage);
        }
        public HitDetails withInternalDamage(int internalDamage) {
            return new HitDetails(hit, damageToApply, setArmorValueTo, internalDamage, hitCrew);
        }
        public HitDetails killsCrew() {
            return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, Crew.DEATH);
        }
        public HitDetails withIncreasedCrewDamage() {
            return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, Math.min(hitCrew + 1, Crew.DEATH));
        }
        public HitDetails withDamage(int damage) {
            return new HitDetails(hit, damage, setArmorValueTo, hitInternal, hitCrew);
        }
        public HitDetails redirectedTo(HitData nextLocation) {
            return new HitDetails(nextLocation, damageToApply, setArmorValueTo, hitInternal, hitCrew);
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

    default boolean entityMustBeDevastated() {
        return entityFinalState().entityMustBeDevastated;
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
            var clusterDamage = clusterSize;
            if (clusterSize == -1) {
                clusterDamage = Compute.randomInt(16) + 5;
            }
            clusterDamage = Math.min(totalDamage, clusterDamage);
            applyDamage(clusterDamage);
            totalDamage -= clusterDamage;
            damageApplied += clusterDamage;
        }
        if (entityMustBeDevastated()) {
            damageApplied += devastateUnit();
        }
        return damageApplied;
    }


    /**
     * Applies damage to the entity in clusters of a given size.
     * This is USUALLY the function you will want to use.
     *
     * @param hitDetails the details of the hit
     * @param clusterSize the size of the clusters
     */
    default int applyDamageInClusters(HitDetails hitDetails, int clusterSize) {
        int totalDamage = hitDetails.damageToApply();
        int damageApplied = 0;
        while (totalDamage > 0) {
            var clusterDamage = Math.min(totalDamage, clusterSize);
            hitDetails = applyDamage(hitDetails.withDamage(clusterDamage));
            totalDamage -= clusterDamage;
            damageApplied += clusterDamage;
        }
        if (entityMustBeDevastated()) {
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
    default HitDetails applyDamage(HitDetails hitDetails) {
        hitDetails = damageArmor(hitDetails);
        hitDetails = damageInternals(hitDetails);
        tryToDamageCrew(hitDetails.hitCrew());
        entity().applyDamage();
        if (entity().isDoomed()) {
            setEntityDestroyed(entity());
        }
        return hitDetails;
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
        if (hitDetails.setArmorValueTo() > 0) {
            return hitDetails;
        }

        entity.setArmor(0, hit);
        int newInternalValue = currentInternalValue + hitDetails.setArmorValueTo();
        if (newInternalValue <= 0 && entityMustSurvive()) {
            newInternalValue = 1;
        }
        logger.trace("[{}] Damage: {} - Internal at: {}", entity.getDisplayName(), hitDetails.damageToApply(), newInternalValue);
        if (newInternalValue > 0) {
            entity.setInternal(newInternalValue, hit);
            hitDetails = applyDamageToEquipments(hitDetails);
        } else {
            entity.setInternal(0, hit);
            hitDetails = destroyLocation(hitDetails);
            if (newInternalValue < 0) {
                hitDetails = hitDetails.redirectedTo(entity.getTransferLocation(hit));
                hitDetails = hitDetails.withDamage(newInternalValue * -1);
            }
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
        setEntityDestroyed(entity);
    }

    /**
     * Tries to damage the crew of the entity. If the crew is dead, the entity is marked as destroyed.
     * The crew won't be damaged if they are already ejected or already dead.
     * This function also does try to not outright kill the crew, as it has proven to be a bit too deadly.
     * @param hitCrew the amount of hits to apply to ALL crew
     */
    default void tryToDamageCrew(int hitCrew) {
        tryToDamageCrew(hitCrew, false);
    }

    /**
     * Tries to damage the crew of the entity. If the crew is dead, the entity is marked as destroyed.
     * The crew won't be damaged if they are already ejected or already dead.
     * This function also does try to not outright kill the crew, as it has proven to be a bit too deadly.
     * @param hitCrew the amount of hits to apply to ALL crew
     * @param applyDamagePostEjection if the damage should be applied after the crew is ejected
     */
    default void tryToDamageCrew(int hitCrew, boolean applyDamagePostEjection) {
        if (hitCrew == 0 || noCrewDamage()) {
            return;
        }

        var entity = entity();
        Crew crew = entity.getCrew();
        if (crew == null || (crew.isEjected() && !applyDamagePostEjection) || crew.isDead()) {
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

        if (hits == Crew.DEATH && crewMustSurvive()) {
            hits = Compute.randomIntInclusive(4) + 1;
        }
        return hits;
    }

    /**
     * Sets the entity as destroyed.
     * @param entity entity to be set as destroyed
     * @param <E> the type of the entity
     */
    static <E extends Entity> void setEntityDestroyed(E entity) {
        entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
        boolean salvageable = entity.getRemovalCondition() < IEntityRemovalConditions.REMOVE_DEVASTATED;
        entity.setSalvage(salvageable);
        entity.setDestroyed(true);
    }

    /**
     * Sets the entity as devastated.
     * @param entity entity to be set as destroyed
     * @param <E> the type of the entity
     */
    static <E extends Entity> void setEntityDevastated(E entity) {
        entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);
        entity.setSalvage(false);
        entity.setDestroyed(true);
    }

    /**
     * Applies damage to the equipments of the entity.
     *
     * @param hitDetails the hit data with information about the location
     */
    default HitDetails applyDamageToEquipments(HitDetails hitDetails) {
        var entity = entity();
        var hit = hitDetails.hit();
        var criticalSlots = entity.getCriticalSlots(hit.getLocation());
        Collections.shuffle(criticalSlots);
        for (CriticalSlot slot : criticalSlots) {
            if (slot != null && slot.isHittable() && !slot.isHit() && !slot.isDestroyed()) {
                slot.setDestroyed(true);
                logger.trace("[{}] Equipment destroyed: {}", entity.getDisplayName(), slot);
                break;
            }
        }
        return hitDetails;
    }

    /**
     * Applies damage only to the armor of the entity.
     *
     * @param hitDetails the hit details
     */
    default HitDetails damageArmor(HitDetails hitDetails) {
        var hit = hitDetails.hit();
        var currentArmorValue = Math.max(hitDetails.setArmorValueTo(), 0);
        if (hit.getLocation() < 0 || hit.getLocation() > 7) {
            hit.setLocation(getRandomHitLocation());
        }
        if (hit.getLocation() < 0 || hit.getLocation() > 7) {
            hit.setLocation(0);
        }
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
        int hitInternal = setArmorValueTo < 0 ?
            switch (Compute.d6(2)) {
                case 12 -> 3;
                case 10, 11 -> 2;
                case 8, 9 -> 1;
                default -> 0;
            } : 0 ;
        int hitCrew = hitInternal > 1 ? 1 : 0;

        return new HitDetails(hit, damageToApply, setArmorValueTo, hitInternal, hitCrew);
    }
}
