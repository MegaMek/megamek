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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import static megamek.common.Compute.rollD6;
import static megamek.common.CriticalSlot.TYPE_SYSTEM;
import static megamek.common.autoresolve.damage.EntityFinalState.*;

/**
 * @author Luana Coppio
 */
public record MekDamageApplier(Mek entity, EntityFinalState entityFinalState) implements DamageApplier<Mek> {

    // Target roll to hit the rear arc of the mek randomly
    private static final int REAR_ARC_HIT_CHANCE = 11;
    private static final Set<Integer> criticalSystems = Set.of(Mek.SYSTEM_ENGINE, Mek.SYSTEM_GYRO, Mek.SYSTEM_LIFE_SUPPORT,
        Mek.SYSTEM_SENSORS, Mek.SYSTEM_COCKPIT);
    private static final Set<Integer> criticalLocations = Set.of(Mek.LOC_CT, Mek.LOC_HEAD, Mek.LOC_LT, Mek.LOC_RT);

    @Override
    public int devastateUnit() {
        var totalDamage = entity().getArmor(Mek.LOC_CT) + entity().getInternal(Mek.LOC_CT);
        entity().setArmor(0, Mek.LOC_CT, Compute.randomFloat() > 0.9);
        entity().setInternal(0, Mek.LOC_CT);
        return totalDamage;
    }

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
        int hitCrew = isHeadHit ? 1 : 0;

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

    private HitDetails destroyHead(HitDetails hitDetails) {
        entity.destroyLocation(Mek.LOC_HEAD);
        if (entity.getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED) {
            tryToEjectCrew(false);
            hitDetails = hitDetails.killsCrew();
            DamageApplier.setEntityDestroyed(entity);
        }
        return hitDetails;
    }

    private void tryToEjectCrew(boolean ammoExplosion) {
        var crewDamage = ejectCrew(ammoExplosion);
        tryToDamageCrew(crewDamage, true);
    }

    private HitDetails destroyCt(HitDetails hitDetails) {
        if (entity.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
            hitDetails = hitDetails.killsCrew();
        }
        entity.destroyLocation(hitDetails.hit().getLocation());
        DamageApplier.setEntityDestroyed(entity);
        return hitDetails;
    }

    private HitDetails destroyLeg(HitDetails hitDetails) {
        hitDetails = avoidFallingDamage(hitDetails);
        entity.destroyLocation(hitDetails.hit().getLocation(), true);
        if (entity.isLocationBlownOff(Mek.LOC_LLEG) && entity.isLocationBlownOff(Mek.LOC_RLEG) && !(entity instanceof QuadMek)) {
            DamageApplier.setEntityDestroyed(entity);
        }
        return hitDetails;
    }

    private HitDetails avoidFallingDamage(HitDetails hitDetails) {
        if (entity().getCrew() != null) {
            TargetRoll targetRoll = new TargetRoll(entity().getCrew().getPiloting() + 1, "Avoid damage when falling");

            if (Compute.d6(2) < targetRoll.getValue()) {
                hitDetails = hitDetails.withCrewDamage(1);
            }
        }
        return hitDetails;
    }

    private HitDetails destroyArm(HitDetails hitDetails) {
        entity.destroyLocation(hitDetails.hit().getLocation(), true);
        return hitDetails;
    }

    private HitDetails destroySideTorso(HitDetails hitDetails) {
        entity.destroyLocation(hitDetails.hit().getLocation());
        return hitDetails;
    }

    @Override
    public HitDetails destroyLocation(HitDetails hitDetails) {
        var hit = hitDetails.hit();
        if (canLoseLocation(hit)) {
            return switch (hit.getLocation()) {
                case Mek.LOC_HEAD -> destroyHead(hitDetails);
                case Mek.LOC_CT -> destroyCt(hitDetails);
                case Mek.LOC_LT, Mek.LOC_RT -> destroySideTorso(hitDetails);
                case Mek.LOC_LARM, Mek.LOC_RARM -> destroyArm(hitDetails);
                case Mek.LOC_LLEG, Mek.LOC_RLEG -> destroyLeg(hitDetails);
                default -> hitDetails;
            };
        }

        return hitDetails;
    }


    public int getLifeSupportHits() {
        return entity.getHitCriticals(
            CriticalSlot.TYPE_SYSTEM,
            Mek.SYSTEM_LIFE_SUPPORT,
            (entity.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) ? Mek.LOC_CT : Mek.LOC_HEAD);
    }

    @Override
    public void applyDamageToEquipments(HitDetails hitDetails) {
        var hit = hitDetails.hit();
        var entity = entity();

        var critSlots = getCriticalSlots(entity, hit);
        var engineHits = entity.getEngineHits();
        var lifeSupportHits = getLifeSupportHits();

        Collections.shuffle(critSlots);

        while (!critSlots.isEmpty()) {

            var crit = Compute.randomListElement(critSlots);

            if (crit.getType() == TYPE_SYSTEM ) {
                var newHitDetails = switch (crit.getIndex()) {
                    case Mek.SYSTEM_LIFE_SUPPORT -> hitLifeSupport(hitDetails, lifeSupportHits, critSlots, crit);
                    case Mek.SYSTEM_ENGINE -> hitEngine(hitDetails, critSlots, crit);
                    case Mek.SYSTEM_COCKPIT -> hitCockpit(hitDetails, critSlots, crit);
                    case Mek.SYSTEM_GYRO -> hitGyro(hitDetails, crit); // always hit
                    default -> hitSomeCrit(hitDetails, crit);
                };
                if (newHitDetails != null) {
                    hitDetails = newHitDetails;
                } else {
                    // when null, this means that we need to try again, see if the next crit slot is good to hit.
                    continue;
                }
            } else {
                if (crit.getMount().getType() instanceof AmmoType ammoType) {
                    if (canHaveAmmoExplosion()) {
                        hitDetails = hitAmmo(hitDetails, ammoType, crit, entity);
                    } else {
                        critSlots.remove(crit);
                        continue;
                    }
                } else {
                    hitSomeCrit(hitDetails, crit);
                }
            }

            critSlots.clear();
        }
    }

    private boolean canHaveAmmoExplosion() {
        return entityFinalState.equals(ENTITY_MUST_BE_DEVASTATED) || entityFinalState.equals(DAMAGE_ONLY_THE_ENTITY)
            || entityFinalState.equals(ANY);
    }

    private static ArrayList<CriticalSlot> getCriticalSlots(Mek entity, HitData hit) {
        var critSlots = new ArrayList<CriticalSlot>();

        for (int critIndex = 0; critIndex < entity.getNumberOfCriticals(hit.getLocation()); critIndex++) {
            CriticalSlot slot = entity.getCritical(hit.getLocation(), critIndex);
            if (slot != null && slot.isHittable()) {
                critSlots.add(slot);
            }
        }
        return critSlots;
    }

    private static HitDetails hitSomeCrit(HitDetails hitDetails, CriticalSlot crit) {
        if (Compute.randomFloat() > 0.5) {
            crit.setDestroyed(true);
        } else {
            crit.setHit(true);
        }
        return hitDetails;
    }

    private HitDetails hitAmmo(HitDetails hitDetails, AmmoType ammoType, CriticalSlot crit, Mek entity) {
        var shots = ammoType.getShots();
        var damagePerShort = ammoType.getDamagePerShot();
        var damage = damagePerShort * shots;
        crit.setDestroyed(true);
        if (damage > 0) {
            hitDetails = hitDetails.withDamage(damage);
            tryToEjectCrew(true);
            applyDamageInClusters(hitDetails, 5);
            if (entity.isDestroyed()) {
                DamageApplier.setEntityDevastated(entity);
            }
        }
        return hitDetails;
    }

    private HitDetails hitCockpit(HitDetails hitDetails, ArrayList<CriticalSlot> critSlots, CriticalSlot crit) {
        if (crewMustSurvive()) {
            critSlots.remove(crit);
            return null;
        }
        hitDetails = hitDetails.killsCrew();
        crit.setDestroyed(true);
        return hitDetails;
    }

    private HitDetails hitGyro(HitDetails hitDetails, CriticalSlot crit) {
        crit.setDestroyed(true);
        return avoidFallingDamage(hitDetails);
    }

    private HitDetails hitEngine(HitDetails hitDetails, ArrayList<CriticalSlot> critSlots, CriticalSlot crit) {
        if (entity.getEngineHits() == 2 && entityMustSurvive()) {
            critSlots.remove(crit);
            return null;
        }
        crit.setHit(true);
        return hitDetails;
    }

    private HitDetails hitLifeSupport(HitDetails hitDetails, int lifeSupportHits, ArrayList<CriticalSlot> critSlots, CriticalSlot crit) {
        if (lifeSupportHits == 1 && entityMustSurvive()) {
            critSlots.remove(crit);
            return null;
        }
        crit.setHit(true);
        hitDetails = hitDetails.withIncreasedCrewDamage();
        return hitDetails;
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
        applyDamageToEquipments(hitDetails);
        if (newInternalValue == 0) {
            hitDetails = destroyLocation(hitDetails);
        }
        return hitDetails;
    }

    private boolean canLoseLocation(HitData hitData) {
        var location = hitData.getLocation();

        if (location == Mek.LOC_CT
            && (entityFinalState().equals(CREW_AND_ENTITY_MUST_SURVIVE) || entityFinalState().equals(ENTITY_MUST_SURVIVE))) {
            return false;
        }

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

    /**
     * Tries to eject the crew of the entity if possible.
     *
     * @return crewDamage amount of damage the crew received
     */
    private int ejectCrew(boolean ammoExplosion) {
        if (!entity.isEjectionPossible()) {
            if (ammoExplosion) {
                return 2;
            }
        }
        var crewDamage = ammoExplosion ? 2 : 0;
        var crew = entity.getCrew();

        var toHit = new ToHitData();
        toHit.addModifier(crew.getPiloting(), "Ejecting");
        var headDamage = entity.getOInternal(Mek.LOC_HEAD) - entity.getInternal(Mek.LOC_HEAD);
        toHit.addModifier(headDamage, "Head Internal Structure Damage");

        var mos = rollD6(2).getMarginOfSuccess(toHit);
        if (mos < 0) {
            crewDamage += (int) Math.floor(mos / 2.0d);
        }

        entity.setDestroyed(true);

        if (entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_DEVASTATED) {
            entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_EJECTED);
            entity.setSalvage(true);
            logger.trace("[{}] Entity destroyed by ejection", entity.getDisplayName());
        }

        logger.trace("[{}] Crew ejected", entity().getDisplayName());
        return crewDamage;
    }
}
