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
import java.util.List;

import static megamek.common.Compute.rollD6;
import static megamek.common.CriticalSlot.TYPE_SYSTEM;
import static megamek.common.autoresolve.damage.EntityFinalState.*;

/**
 * @author Luana Coppio
 */
public record MekDamageApplier(Mek entity, EntityFinalState entityFinalState) implements DamageApplier<Mek> {

    // Target roll to hit the rear arc of the mek randomly
    private static final int REAR_ARC_HIT_CHANCE = 11;

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
            var locationIsNotDestroyed = (entity.getInternal(i) > 0) && !entity.isLocationBlownOff(i);
            var locationIsNotHead = Mek.LOC_HEAD != i;
            var weight = locationIsNotHead ? 6.0 : 1.0;
            if (locationIsNotDestroyed) {
                weightedDoubleMap.add(weight, i);
            }
        }
        return weightedDoubleMap.randomOptionalItem().orElse(-1);
    }

    @Override
    public HitDetails setupHitDetails(HitData hit, int dmg) {
        int currentArmorValue = entity.getArmor(hit);
        int setArmorValueTo = currentArmorValue - dmg;
        int currentInternalArmorValue = entity.getInternal(hit);

        int hitInternal = setArmorValueTo < 0 && (currentInternalArmorValue + setArmorValueTo) > 0 ?
            switch (Compute.d6(2)) {
                case 12 -> 3;
                case 10, 11 -> 2;
                case 8, 9 -> 1;
                default -> 0;
            } : 0 ;

        boolean isHeadHit = (entity.getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED) && (hit.getLocation() == Mek.LOC_HEAD);
        int hitCrew = isHeadHit ? 1 : 0;

        return new HitDetails(hit, dmg, setArmorValueTo, hitInternal, hitCrew);
    }

    @Override
    public HitData getHitData(int hitLocation) {
        boolean hitRearArc = rollD6(2).isTargetRollSuccess(REAR_ARC_HIT_CHANCE);
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
        entity.destroyLocation(Mek.LOC_HEAD, false);
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
        DamageApplier.setEntityDevastated(entity);
        return hitDetails;
    }

    private HitDetails destroyLeg(HitDetails hitDetails) {
        hitDetails = avoidFallingDamage(hitDetails);
        entity.destroyLocation(hitDetails.hit().getLocation(), false);
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
        entity.destroyLocation(hitDetails.hit().getLocation(), false);
        return hitDetails;
    }

    private HitDetails destroySideTorso(HitDetails hitDetails) {
        entity.destroyLocation(hitDetails.hit().getLocation());
        return hitDetails;
    }

    @Override
    public HitDetails destroyLocation(HitDetails hitDetails) {
        var hit = hitDetails.hit();
        return switch (hit.getLocation()) {
            case Mek.LOC_HEAD -> destroyHead(hitDetails);
            case Mek.LOC_CT -> destroyCt(hitDetails);
            case Mek.LOC_LT, Mek.LOC_RT -> destroySideTorso(hitDetails);
            case Mek.LOC_LARM, Mek.LOC_RARM -> destroyArm(hitDetails);
            case Mek.LOC_LLEG, Mek.LOC_RLEG -> destroyLeg(hitDetails);
            default -> hitDetails;
        };
    }


    public int getLifeSupportHits() {
        return entity.getHitCriticals(
            TYPE_SYSTEM,
            Mek.SYSTEM_LIFE_SUPPORT,
            (entity.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) ? Mek.LOC_CT : Mek.LOC_HEAD);
    }

    @Override
    public HitDetails applyDamageToEquipments(HitDetails hitDetails) {
        var hit = hitDetails.hit();
        var entity = entity();

        var critSlots = getCriticalSlots(entity, hit);
        var lifeSupportHits = getLifeSupportHits();

        Collections.shuffle(critSlots);
        var damageToApply = hitDetails.hitInternal();
        while (damageToApply > 0) {
            if (damageToApply == 3
                && List.of(Mek.LOC_HEAD, Mek.LOC_LARM, Mek.LOC_LLEG, Mek.LOC_RLEG, Mek.LOC_RARM).contains(hit.getLocation()))
            {
                return switch (hit.getLocation()) {
                    case Mek.LOC_HEAD -> destroyHead(hitDetails);
                    case Mek.LOC_LARM, Mek.LOC_RARM -> destroyArm(hitDetails);
                    case Mek.LOC_LLEG, Mek.LOC_RLEG -> destroyLeg(hitDetails);
                    default -> throw new IllegalStateException("Unexpected value: " + hit.getLocation());
                };
            }

            damageToApply--;
            var crit = Compute.randomListElement(critSlots);
            if (crit == null || crit.isDamaged()) {
                break;
            }

            if (crit.getType() == TYPE_SYSTEM ) {
                hitDetails = switch (crit.getIndex()) {
                    case Mek.SYSTEM_LIFE_SUPPORT -> hitLifeSupport(hitDetails, lifeSupportHits, critSlots, crit);
                    case Mek.SYSTEM_ENGINE -> hitEngine(hitDetails, critSlots, crit);
                    case Mek.SYSTEM_COCKPIT -> hitCockpit(hitDetails, critSlots, crit);
                    case Mek.SYSTEM_GYRO -> hitGyro(hitDetails, crit); // always hit
                    default -> hitSomeCrit(hitDetails, crit);
                };
            } else {
                hitDetails = hitEquipment(hitDetails, crit, entity);
            }
        }

        return hitDetails;
    }

    private HitDetails hitEquipment(HitDetails hitDetails, CriticalSlot crit, Mek entity) {
        if (crit.getMount().getType() instanceof AmmoType ammoType) {
            if (canHaveAmmoExplosion()) {
                hitDetails = hitAmmo(hitDetails, ammoType, crit, entity);
            }
        } else {
            hitSomeCrit(hitDetails, crit);
        }
        return hitDetails;
    }

    private boolean canHaveAmmoExplosion() {
        return entityFinalState.equals(ENTITY_MUST_BE_DEVASTATED) || entityFinalState.equals(ANY);
    }

    private static ArrayList<CriticalSlot> getCriticalSlots(Mek entity, HitData hit) {
        var critSlots = new ArrayList<CriticalSlot>();

        for (int critIndex = 0; critIndex < entity.getNumberOfCriticals(hit.getLocation()); critIndex++) {
            CriticalSlot slot = entity.getCritical(hit.getLocation(), critIndex);
            critSlots.add(slot);
        }
        return critSlots;
    }

    private static HitDetails hitSomeCrit(HitDetails hitDetails, CriticalSlot crit) {
        crit.setHit(true);
        return hitDetails;
    }

    private HitDetails hitAmmo(HitDetails hitDetails, AmmoType ammoType, CriticalSlot crit, Mek entity) {
        var shots = ammoType.getShots();
        var damagePerShort = ammoType.getDamagePerShot();
        var damage = damagePerShort * shots;
        crit.setHit(true);

        if (damage > 0) {
            hitDetails = hitDetails.withDamage(damage);
            tryToEjectCrew(true);
            applyDamageInClusters(hitDetails, 5);
            if (entity.getInternal(Mek.LOC_CT) == 0 || entity.getArmor(Mek.LOC_LT) == 0 || entity.getArmor(Mek.LOC_RT) == 0) {
                if (!entity.hasCase() && !entity.hasCASEII()) {
                    DamageApplier.setEntityDevastated(entity);
                } else {
                    DamageApplier.setEntityDestroyed(entity);
                }
            }
        }
        return hitDetails;
    }

    private HitDetails hitCockpit(HitDetails hitDetails, ArrayList<CriticalSlot> critSlots, CriticalSlot crit) {
        if (crewMustSurvive()) {
            critSlots.remove(crit);
        } else {
            hitDetails = hitDetails.killsCrew();
            crit.setHit(true);
        }
        return hitDetails;
    }

    private HitDetails hitGyro(HitDetails hitDetails, CriticalSlot crit) {
        crit.setHit(true);
        return avoidFallingDamage(hitDetails);
    }

    private HitDetails hitEngine(HitDetails hitDetails, ArrayList<CriticalSlot> critSlots, CriticalSlot crit) {
        if (entity.getEngineHits() == 2 && entityMustSurvive()) {
            critSlots.remove(crit);
        } else {
            crit.setHit(true);
        }
        return hitDetails;
    }

    private HitDetails hitLifeSupport(HitDetails hitDetails, int lifeSupportHits, ArrayList<CriticalSlot> critSlots, CriticalSlot crit) {
        if (lifeSupportHits == 1 && entityMustSurvive()) {
            critSlots.remove(crit);
        } else {
            crit.setHit(true);
            hitDetails = hitDetails.withIncreasedCrewDamage();
        }
        return hitDetails;
    }

    @Override
    public  HitDetails damageInternals(HitDetails hitDetails) {
        if (hitDetails.setArmorValueTo() > 0) {
            return hitDetails;
        }

        HitData hit = hitDetails.hit();
        var entity = entity();
        int currentInternalValue = entity.getInternal(hit);
        entity.setArmor(0, hit);

        int newInternalValue = getNewInternalValue(hitDetails, currentInternalValue, hit);
        if ((newInternalValue > 0) && newInternalValue != currentInternalValue) {
            entity.setInternal(newInternalValue, hit);
            hitDetails = applyDamageToEquipments(hitDetails);
        } else if (newInternalValue <= 0) {
            entity.setInternal(0, hit);
            hitDetails = destroyLocation(hitDetails);
            hitDetails = hitDetails.redirectedTo(entity.getTransferLocation(hit));
        }

        return hitDetails;
    }

    private int getNewInternalValue(HitDetails hitDetails, int currentInternalValue, HitData hit) {
        int newInternalValue = currentInternalValue + hitDetails.setArmorValueTo();
        if (entityMustSurvive() && newInternalValue <= 0 && !canLoseLocation(hit)) {
            newInternalValue = 1;
        }
        return newInternalValue;
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
        // If the unit must survive, then it may not lose either head of CT
        if (location == Mek.LOC_CT || location == Mek.LOC_HEAD) {
            return false;
        }
        // If the unit must survive, then it cant lose side torso if there is an engine in there
        if (location == Mek.LOC_LT || location == Mek.LOC_RT) {
            return entity.getCritical(location, Mek.SYSTEM_ENGINE) == null;
        }

        // If the unit must survive, then it cant lose both legs
        if (location == Mek.LOC_LLEG || location == Mek.LOC_RLEG) {
            return entity.getInternal(Mek.LOC_LLEG) > 0 && entity.getInternal(Mek.LOC_RLEG) > 0;
        }

        // ok for damage
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

        if (entity.hasFullHeadEject()) {
            entity.destroyLocation(Mek.LOC_HEAD, true);
        } else {
            for (CriticalSlot slot : (entity.getCockpit())) {
                slot.setDestroyed(true);
            }
        }

        if (entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_DEVASTATED) {
            entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_EJECTED);
            entity.setSalvage(true);
            logger.trace("[{}] Entity destroyed by ejection", entity.getDisplayName());
        }

        logger.trace("[{}] Crew ejected", entity().getDisplayName());
        return crewDamage;
    }


    public static int clamp(int value, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        return Math.min(max, Math.max(value, min));
    }

}
