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
import megamek.common.util.weightedMaps.WeightedDoubleMap;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static megamek.common.Compute.rollD6;
import static megamek.common.CriticalSlot.TYPE_SYSTEM;

/**
 * @author Luana Coppio
 */
public record MekDamageApplier(Mek entity, boolean crewMustSurvive, boolean entityMustSurvive) implements DamageApplier<Mek> {

    // Target roll to hit the rear arc of the mek randomly
    private static final int REAR_ARC_HIT_CHANCE = 11;
    private static final Set<Integer> criticalSystems = Set.of(Mek.SYSTEM_ENGINE, Mek.SYSTEM_GYRO, Mek.SYSTEM_LIFE_SUPPORT,
        Mek.SYSTEM_SENSORS, Mek.SYSTEM_COCKPIT);
    private static final Set<Integer> criticalLocations = Set.of(Mek.LOC_CT, Mek.LOC_HEAD, Mek.LOC_LT, Mek.LOC_RT);

    @Override
    public int getRandomHitLocation() {
        var entity = entity();
        WeightedDoubleMap<Integer> weightedDoubleMap = new WeightedDoubleMap<>();
        for (int i = 0; i < entity.locations(); i++) {
            if (entity.getOArmor(i) <= 0) {
                continue;
            }
            var locationIsNotBlownOff = !entity.isLocationBlownOff(i);
            var locationIsNotDestroyed = entity.getInternal(i) > 0;
            var locationIsNotHead = Mek.LOC_HEAD != i;
            var weight = locationIsNotHead ? 6.0 : 1.0;
            if (locationIsNotBlownOff && locationIsNotDestroyed) {
                weightedDoubleMap.add(weight, i);
            }
        }
        return weightedDoubleMap.randomOptionalItem().orElse(-1);
    }

    @Override
    public HitDetails setupHitDetails(HitData hit, int dmg) {
        int currentArmorValue = entity.getArmor(hit);
        int setArmorValueTo = currentArmorValue - dmg;
        boolean hitInternal = setArmorValueTo < 0;
        boolean isHeadHit = (entity.getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED) && (hit.getLocation() == Mek.LOC_HEAD);
        int hitCrew = isHeadHit || hitInternal ? 1 : 0;

        return new HitDetails(hit, dmg, setArmorValueTo, hitInternal, hitCrew);
    }

    @Override
    public HitData getHitData(int hitLocation) {
        boolean hitRearArc = Compute.rollD6(2).isTargetRollSuccess(REAR_ARC_HIT_CHANCE);
        return getHitData(hitLocation, hitRearArc);
    }

    /**
     * Returns the hit data for the given location, considering if the hit is on the rear arc or not.
     * @param hitLocation the location of the hit
     * @param hitRearArc if the hit is on the rear arc
     * @return the hit data
     */
    public HitData getHitData(int hitLocation, boolean hitRearArc) {
        return new HitData(hitLocation, hitRearArc && hitLocationHasRear(hitLocation), HitData.EFFECT_NONE);
    }

    private boolean hitLocationHasRear(int hitLocation) {
        return switch(hitLocation) {
            case Mek.LOC_CT, Mek.LOC_LT, Mek.LOC_RT -> true;
            default -> false;
        };
    }


    @Override
    public HitDetails destroyLocation(HitDetails hitDetails) {
        var entity = entity();
        var hit = hitDetails.hit();
        entity.destroyLocation(hit.getLocation());
        if (hit.getLocation() == Mek.LOC_CT || hit.getLocation() == Mek.LOC_HEAD) {
            entity.setDestroyed(true);
            if (hit.getLocation() == Mek.LOC_HEAD) {
                if (entity.isEjectionPossible()) {
                    var toHit = new ToHitData();
                    toHit.addModifier(4, "Ejection");
                    if (rollD6(2).isTargetRollSuccess(toHit)) {
                        hitDetails = hitDetails.withCrewDamage(2);
                    } else {
                        hitDetails = hitDetails.killsCrew();
                    }
                } else {
                    hitDetails = hitDetails.killsCrew();
                }
                DamageApplier.setEntityDestroyed(entity);
            } else {
                entity.setSalvage(false);
                entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);
                logger.trace("[{}] Mek devastated!", entity.getDisplayName());
            }
        }
        if (hit.getLocation() == Mek.LOC_RLEG || hit.getLocation() == Mek.LOC_LLEG) {
            // leg destroyed causes a fall which damages the pilot
            hitDetails.withCrewDamage(1);
        }
        return hitDetails;
    }

    @Override
    public void applyDamageToEquipments(HitData hit) {
        var entity = entity();
        var criticalSlots = entity.getCriticalSlots(hit.getLocation());
        Collections.shuffle(criticalSlots);
        if (entityMustSurvive() && criticalLocations.contains(hit.getLocation())) {
            criticalSlots = criticalSlots.stream().filter(Objects::nonNull)
                .filter(slot -> !(slot.getType() == TYPE_SYSTEM && criticalSystems.contains(slot.getIndex())))
                .toList();
        }
        for (CriticalSlot slot : criticalSlots) {
            if (slot != null && slot.isHittable() && !slot.isHit() && !slot.isDestroyed()) {
                slot.setHit(true);
                slot.setDestroyed(true);
                logger.trace("[{}] Slot {} destroyed", entity.getDisplayName(), slot.getIndex());
                break;
            }
        }
        // TODO: check if damage on ammo
        // TODO: check if damage on life support
        // TODO: check if damage on cockpit
    }

    @Override
    public void destroyLocationAfterEjection(){
        var entity = entity();
        entity.destroyLocation(Mek.LOC_HEAD);
    }

    @Override
    public  HitDetails damageInternals(HitDetails hitDetails) {
        HitData hit = hitDetails.hit();
        var entity = entity();
        int currentInternalValue = entity.getInternal(hit);
        int newInternalValue = Math.max(currentInternalValue + hitDetails.setArmorValueTo(), 0);
        entity.setArmor(0, hit);
        if (entityMustSurvive() && !canLoseLocation(hit)) {
            newInternalValue = Math.max(newInternalValue, Compute.d6());
        }
        entity.setInternal(newInternalValue, hit);
        applyDamageToEquipments(hit);
        if (newInternalValue == 0) {
            hitDetails = destroyLocation(hitDetails);
        }
        return hitDetails;
    }

    private boolean canLoseLocation(HitData hitData) {
        var location = hitData.getLocation();
        if (!entityMustSurvive()) {
            return true;
        }

        if (location == Mek.LOC_CT || location == Mek.LOC_HEAD) {
            return false;
        }
        if (location == Mek.LOC_LT || location == Mek.LOC_RT) {
            if (entity.getCritical(location, Mek.SYSTEM_ENGINE) == null) {
                return true;
            }
        }
        if (location == Mek.LOC_LLEG || location == Mek.LOC_RLEG) {
            return !entity.isLocationBlownOff(Mek.LOC_LLEG) && !entity.isLocationBlownOff(Mek.LOC_RLEG);
        }

        return true;
    }
}
