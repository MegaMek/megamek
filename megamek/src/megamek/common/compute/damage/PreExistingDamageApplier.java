/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.compute.damage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.compute.damage.CritAssignment.AeroFighterCrit;
import megamek.common.compute.damage.CritAssignment.AeroFighterCritKind;
import megamek.common.compute.damage.CritAssignment.EquipmentCrit;
import megamek.common.compute.damage.CritAssignment.MekSystemCrit;
import megamek.common.compute.damage.CritAssignment.VehicleCrit;
import megamek.common.compute.damage.CritAssignment.VehicleCritKind;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.Mounted;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import megamek.logging.MMLogger;

/**
 * Rolls and distributes pre-existing damage per the First Succession War rules (FSW p.144 and the Pre-Existing Damage
 * Table, FSW p.145). The simulation never mutates the entity; it works on copies of the unit's current armor and
 * internal structure values and returns a {@link PreExistingDamageResult} that the unit editor dialog writes into its
 * controls.
 *
 * <p>Damage is applied in 5-point (or fraction thereof) groups to random locations: Meks use the Front/Back column,
 * fighters use the Above/Below column, and combat vehicles first roll an attack direction using the BattleMek Facing
 * After a Fall Table (TW p.68). Damage that penetrates armor reduces internal structure and transfers inward when a
 * location is destroyed.</p>
 *
 * <p>Critical hits (Moderate and Heavy levels only) follow the FSW prose: any result that would destroy the unit or
 * reduce its mobility to zero is rerolled once; if the reroll would also destroy or immobilize the unit, both results
 * are disregarded and an additional 5 points of damage are applied to a random location instead. The same protection is
 * extended to plain damage groups: a group that would destroy the unit rerolls its location.</p>
 *
 * <p>Vehicle and fighter critical hits are approximated with the subset of results the unit editor dialog can
 * represent (weapons, stabilizers, sensors, motive damage, turret lock, avionics, FCS, engine, landing gear) plus the
 * forbidden results of the real tables (crew, ammo, engine destruction, fuel) so the reroll rule still triggers.</p>
 */
public final class PreExistingDamageApplier {

    private static final MMLogger LOGGER = MMLogger.create(PreExistingDamageApplier.class);

    /** Damage group size (FSW p.145: "distributed randomly in 5-point (or fraction thereof) groups"). */
    private static final int DAMAGE_GROUP_SIZE = 5;

    /** Extra damage applied when a critical hit is disregarded twice (FSW p.144). */
    private static final int DISREGARDED_CRIT_DAMAGE = 5;

    /** Safety cap on rerolls of a damage group's location before the remaining damage is dropped. */
    private static final int MAX_REROLL_ATTEMPTS = 25;

    /** Safety cap on disregarded-crit fallback groups, so crit rerolls cannot chain forever. */
    private static final int MAX_FALLBACK_GROUPS = 4;

    /** Engine critical hits that destroy a Mek or fighter (TW p.125, p.240). */
    private static final int ENGINE_HITS_TO_DESTROY = 3;

    /** Gyro critical hits that destroy a standard gyro (TW p.125); heavy-duty gyros take one more (TO). */
    private static final int GYRO_HITS_TO_DESTROY = 2;
    private static final int HEAVY_DUTY_GYRO_HITS_TO_DESTROY = 3;

    /** Motive system hits at which the unit editor dialog marks a vehicle immobile. */
    private static final int VEHICLE_MOTIVE_HITS_TO_IMMOBILIZE = 4;

    /** The unit editor dialog exposes three crit boxes for fighter avionics, FCS, and sensors. */
    private static final int AERO_SYSTEM_HITS_MAX = 3;

    private final Entity entity;
    private final PreExistingDamageLevel level;

    private final int[] armor;
    private final int[] rearArmor;
    private final int[] internal;
    private int structuralIntegrity;
    private final List<CritAssignment> critAssignments = new ArrayList<>();
    private final Deque<Integer> pendingDamageGroups = new ArrayDeque<>();

    /** Mek crit slots already assigned this simulation, keyed by {@code location * 100 + slotIndex}. */
    private final Set<Integer> assignedSlotKeys = new HashSet<>();
    /** Equipment numbers already assigned a crit on vehicles/fighters (their dialog panels hold one box). */
    private final Set<Integer> assignedEquipmentNumbers = new HashSet<>();
    private final Set<Integer> assignedStabilizerLocations = new HashSet<>();

    private int engineHits;
    private int gyroHits;
    private int sensorHits;
    private int motiveHits;
    private int avionicsHits;
    private int fcsHits;
    private boolean gearHit;
    private boolean turretLocked;
    private int fallbackGroupsRemaining = MAX_FALLBACK_GROUPS;

    private PreExistingDamageApplier(Entity entity, PreExistingDamageLevel level) {
        this.entity = entity;
        this.level = level;
        armor = new int[entity.locations()];
        rearArmor = new int[entity.locations()];
        internal = new int[entity.locations()];
        for (int location = 0; location < entity.locations(); location++) {
            armor[location] = Math.max(entity.getArmor(location, false), 0);
            if (entity.hasRearArmor(location)) {
                rearArmor[location] = Math.max(entity.getArmor(location, true), 0);
            }
            internal[location] = Math.max(entity.getInternal(location), 0);
        }
        if (entity instanceof Aero aero) {
            structuralIntegrity = Math.max(aero.getSI(), 0);
        }
        initExistingCritCounts();
    }

    /**
     * @param entity the unit to check
     *
     * @return {@code true} if the pre-existing damage rules cover this unit type (Meks, combat vehicles, and
     *       aerospace or conventional fighters, per FSW p.144)
     */
    public static boolean isSupported(Entity entity) {
        if (entity instanceof Mek) {
            return true;
        }
        if (entity instanceof Tank) {
            return !(entity instanceof GunEmplacement);
        }
        return (entity instanceof AeroSpaceFighter) && !(entity instanceof FighterSquadron);
    }

    /**
     * Simulates pre-existing damage at the given level for the given unit. The entity is only read, never modified.
     *
     * @param entity the unit to roll damage for
     * @param level  the Pre-Existing Damage Table result to apply
     *
     * @return the rolled damage and critical hits as absolute remaining values
     */
    public static PreExistingDamageResult simulate(Entity entity, PreExistingDamageLevel level) {
        PreExistingDamageApplier applier = new PreExistingDamageApplier(entity, level);
        if (entity instanceof Aero aero) {
            // Aero.rollHitLocation() stores a potential crit on the unit as a side effect; preserve it
            int savedPotentialCrit = aero.getPotCrit();
            try {
                applier.run();
            } finally {
                aero.setPotCrit(savedPotentialCrit);
            }
        } else {
            applier.run();
        }
        return applier.buildResult();
    }

    private void initExistingCritCounts() {
        if (entity instanceof Mek) {
            for (int location = 0; location < entity.locations(); location++) {
                engineHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, location);
                gyroHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, location);
            }
        } else if (entity instanceof Tank tank) {
            sensorHits = tank.getSensorHits();
            turretLocked = !tank.hasNoTurret() && tank.isTurretLocked(tank.getLocTurret());
            if (tank.isImmobile(false)) {
                motiveHits = VEHICLE_MOTIVE_HITS_TO_IMMOBILIZE;
            } else if (tank.hasHeavyMovementDamage()) {
                motiveHits = 3;
            } else if (tank.hasModerateMovementDamage()) {
                motiveHits = 2;
            } else if (tank.hasMinorMovementDamage()) {
                motiveHits = 1;
            }
        } else if (entity instanceof Aero aero) {
            engineHits = aero.getEngineHits();
            avionicsHits = aero.getAvionicsHits();
            fcsHits = aero.getFCSHits();
            sensorHits = aero.getSensorHits();
            gearHit = aero.isGearHit();
        }
    }

    private void run() {
        if (level == PreExistingDamageLevel.NONE) {
            return;
        }
        int remainingTotal = level.totalDamage(entity.getWeight());
        while (remainingTotal > 0) {
            int groupSize = Math.min(DAMAGE_GROUP_SIZE, remainingTotal);
            pendingDamageGroups.add(groupSize);
            remainingTotal -= groupSize;
        }
        processPendingDamageGroups();
        for (int critNumber = 0; critNumber < level.getGuaranteedCritCount(); critNumber++) {
            assignGuaranteedCrit();
        }
        // crits disregarded twice queue an extra 5-point group (FSW p.144)
        processPendingDamageGroups();
    }

    private void processPendingDamageGroups() {
        while (!pendingDamageGroups.isEmpty()) {
            applyDamageGroup(pendingDamageGroups.poll());
        }
    }

    private PreExistingDamageResult buildResult() {
        return new PreExistingDamageResult(armor.clone(), rearArmor.clone(), internal.clone(), structuralIntegrity,
              List.copyOf(critAssignments));
    }

    // ----- damage groups -----

    private void applyDamageGroup(int points) {
        for (int attempt = 0; attempt < MAX_REROLL_ATTEMPTS; attempt++) {
            if (entity instanceof Aero) {
                if (tryApplyFighterGroup(points)) {
                    return;
                }
            } else if (tryApplyGroundGroup(points)) {
                return;
            }
        }
        LOGGER.warn("Could not place a {}-point pre-existing damage group on {} without destroying it; dropping it",
              points, entity.getDisplayName());
    }

    /** Fighters: damage beyond armor reduces structural integrity point for point (TW p.239). */
    private boolean tryApplyFighterGroup(int points) {
        HitData hit = rollGroupHit();
        int location = hit.getLocation();
        int armorDamage = Math.min(points, armor[location]);
        int structureDamage = points - armorDamage;
        if (structureDamage >= structuralIntegrity) {
            // would destroy the fighter; reroll the location
            return false;
        }
        armor[location] -= armorDamage;
        structuralIntegrity -= structureDamage;
        if (structureDamage > 0) {
            rollInternalStructureCrits(location);
        }
        return true;
    }

    /** Meks and vehicles: damage transfers inward when a location is destroyed (TW p.127). */
    private boolean tryApplyGroundGroup(int points) {
        int[] scratchArmor = armor.clone();
        int[] scratchRear = rearArmor.clone();
        int[] scratchInternal = internal.clone();
        List<Integer> structureDamagedLocations = new ArrayList<>();
        List<Integer> destroyedLocations = new ArrayList<>();

        HitData hit = rollGroupHit();
        int remaining = points;
        while (remaining > 0) {
            int location = hit.getLocation();
            if (location < 0) {
                // transferred off the unit with damage left over; the unit would be destroyed
                return false;
            }
            boolean rear = hit.isRear() && entity.hasRearArmor(location);
            int armorRemaining = rear ? scratchRear[location] : scratchArmor[location];
            int armorDamage = Math.min(remaining, armorRemaining);
            if (rear) {
                scratchRear[location] -= armorDamage;
            } else {
                scratchArmor[location] -= armorDamage;
            }
            remaining -= armorDamage;
            if (remaining == 0) {
                break;
            }
            if (scratchInternal[location] == 0) {
                // location already destroyed; damage passes through
                hit = entity.getTransferLocation(hit);
                continue;
            }
            if (remaining >= scratchInternal[location]) {
                if (isForbiddenLocationDestruction(location)) {
                    return false;
                }
                remaining -= scratchInternal[location];
                scratchInternal[location] = 0;
                structureDamagedLocations.add(location);
                destroyedLocations.add(location);
                hit = entity.getTransferLocation(hit);
            } else {
                scratchInternal[location] -= remaining;
                structureDamagedLocations.add(location);
                remaining = 0;
            }
        }

        // Losing a location destroys the engine slots it carried, so bank them before any later crit is rolled.
        // Without this the engine hit count goes stale and a later engine crit elsewhere could be the third hit,
        // destroying the unit. A destroyed location can never be picked for a crit, so this cannot double count.
        for (int location : destroyedLocations) {
            engineHits += survivingEngineSlotsIn(location);
        }
        System.arraycopy(scratchArmor, 0, armor, 0, armor.length);
        System.arraycopy(scratchRear, 0, rearArmor, 0, rearArmor.length);
        System.arraycopy(scratchInternal, 0, internal, 0, internal.length);
        for (int location : structureDamagedLocations) {
            rollInternalStructureCrits(location);
        }
        return true;
    }

    /**
     * @return {@code true} if destroying this location would destroy the unit or reduce its mobility to zero, which
     *       pre-existing damage may not do (FSW p.144)
     */
    private boolean isForbiddenLocationDestruction(int location) {
        if (entity instanceof Mek mek) {
            if ((location == Mek.LOC_HEAD) || (location == Mek.LOC_CENTER_TORSO) || mek.locationIsLeg(location)) {
                return true;
            }
            // Destroying a side torso destroys the engine slots it still carries; forbid it if that kills the engine.
            int engineSlotsHere = survivingEngineSlotsIn(location);
            return (engineSlotsHere > 0) && ((engineHits + engineSlotsHere) >= ENGINE_HITS_TO_DESTROY);
        }
        // destroying any combat vehicle location destroys or immobilizes the vehicle (incl. VTOL rotors)
        return true;
    }

    /**
     * Counts the engine critical slots in a location that are not hit yet, and so would newly be lost if the location
     * were destroyed.
     *
     * <p>Slots already hit are excluded twice over: those damaged on the entity itself, and those this simulation has
     * assigned a crit to. The latter matters because {@link #engineHits} already counts them and the entity is never
     * mutated, so counting them again here would overstate the engine damage.</p>
     *
     * @param location the location to count in
     *
     * @return the number of undamaged engine critical slots in the location, {@code 0} for non-Mek units
     */
    private int survivingEngineSlotsIn(int location) {
        if (!(entity instanceof Mek)) {
            return 0;
        }
        int simulatedHits = 0;
        for (int slotIndex = 0; slotIndex < entity.getNumberOfCriticalSlots(location); slotIndex++) {
            if (!assignedSlotKeys.contains(slotKey(location, slotIndex))) {
                continue;
            }
            CriticalSlot slot = entity.getCritical(location, slotIndex);
            if ((slot != null) && (slot.getType() == CriticalSlot.TYPE_SYSTEM)
                  && (slot.getIndex() == Mek.SYSTEM_ENGINE)) {
                simulatedHits++;
            }
        }
        return entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, location)
              - entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, location)
              - simulatedHits;
    }

    /** Rolls the hit location for one damage group, using the column the FSW rules assign to each unit type. */
    private HitData rollGroupHit() {
        if (entity instanceof Mek) {
            int side = (Compute.d6() <= 3) ? ToHitData.SIDE_FRONT : ToHitData.SIDE_REAR;
            return entity.rollHitLocation(ToHitData.HIT_NORMAL, side);
        }
        if (entity instanceof Aero) {
            int table = (Compute.d6() <= 3) ? ToHitData.HIT_ABOVE : ToHitData.HIT_BELOW;
            return entity.rollHitLocation(table, ToHitData.SIDE_FRONT);
        }
        return entity.rollHitLocation(ToHitData.HIT_NORMAL, rollVehicleAttackDirection());
    }

    /**
     * Rolls the attack direction for a combat vehicle using the BattleMek Facing After a Fall Table (TW p.68), as FSW
     * p.144 directs: 1 front, 2-3 right, 4 rear, 5-6 left.
     */
    private int rollVehicleAttackDirection() {
        return switch (Compute.d6()) {
            case 2, 3 -> ToHitData.SIDE_RIGHT;
            case 4 -> ToHitData.SIDE_REAR;
            case 5, 6 -> ToHitData.SIDE_LEFT;
            default -> ToHitData.SIDE_FRONT;
        };
    }

    // ----- critical hits -----

    /** Determining Critical Hits roll (TW p.124) made when a damage group reaches internal structure. */
    private void rollInternalStructureCrits(int location) {
        if (!level.rollsInternalStructureCrits()) {
            return;
        }
        int roll = Compute.d6(2);
        int critCount;
        if (roll <= 7) {
            critCount = 0;
        } else if (roll <= 9) {
            critCount = 1;
        } else if (roll <= 11) {
            critCount = 2;
        } else {
            critCount = 3;
        }
        for (int critNumber = 0; critNumber < critCount; critNumber++) {
            assignCrit(location);
        }
    }

    /** One guaranteed critical hit to a random location (FSW p.145, Moderate and Heavy results). */
    private void assignGuaranteedCrit() {
        for (int attempt = 0; attempt < MAX_REROLL_ATTEMPTS; attempt++) {
            HitData hit = rollGroupHit();
            int location = hit.getLocation();
            if ((entity instanceof Aero) || (internal[location] > 0)) {
                assignCrit(location);
                return;
            }
        }
    }

    /**
     * Assigns one critical hit in or near the given location, applying the FSW reroll rule: a result that would destroy
     * or immobilize the unit is rerolled once; if the reroll would too, both are disregarded and an extra 5-point
     * damage group is queued instead.
     */
    private void assignCrit(int location) {
        if (entity instanceof Mek) {
            assignMekCrit(location);
        } else if (entity instanceof Tank) {
            assignVehicleCrit(location);
        } else {
            // fighter crits are unit-wide (avionics, FCS, engine, gear), so the location is not used
            assignFighterCrit();
        }
    }

    private void queueFallbackGroup() {
        if (fallbackGroupsRemaining > 0) {
            fallbackGroupsRemaining--;
            pendingDamageGroups.add(DISREGARDED_CRIT_DAMAGE);
        }
    }

    // ----- Mek crits -----

    private void assignMekCrit(int location) {
        List<Integer> candidateSlots = pickableSlotIndices(location);
        if (candidateSlots.isEmpty()) {
            // no functional slots left; the in-game crit would transfer inward
            HitData transfer = entity.getTransferLocation(new HitData(location));
            if (transfer.getLocation() >= 0) {
                candidateSlots = pickableSlotIndices(transfer.getLocation());
                location = transfer.getLocation();
            }
            if (candidateSlots.isEmpty()) {
                return;
            }
        }
        int firstSlotIndex = candidateSlots.get(Compute.randomInt(candidateSlots.size()));
        if (!isForbiddenMekSlot(location, firstSlotIndex)) {
            recordMekSlotCrit(location, firstSlotIndex);
            return;
        }
        int secondSlotIndex = candidateSlots.get(Compute.randomInt(candidateSlots.size()));
        if (!isForbiddenMekSlot(location, secondSlotIndex)) {
            recordMekSlotCrit(location, secondSlotIndex);
            return;
        }
        queueFallbackGroup();
    }

    private List<Integer> pickableSlotIndices(int location) {
        List<Integer> candidates = new ArrayList<>();
        if (internal[location] == 0) {
            return candidates;
        }
        for (int slotIndex = 0; slotIndex < entity.getNumberOfCriticalSlots(location); slotIndex++) {
            CriticalSlot slot = entity.getCritical(location, slotIndex);
            if ((slot == null) || !slot.isHittable() || slot.isHit() || slot.isDestroyed()
                  || slot.isBreached() || slot.isMissing()) {
                continue;
            }
            if (assignedSlotKeys.contains(slotKey(location, slotIndex))) {
                continue;
            }
            candidates.add(slotIndex);
        }
        return candidates;
    }

    private int slotKey(int location, int slotIndex) {
        return (location * 100) + slotIndex;
    }

    private boolean isForbiddenMekSlot(int location, int slotIndex) {
        CriticalSlot slot = entity.getCritical(location, slotIndex);
        if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            // ammunition hits are forbidden (FSW p.145)
            return slot.getMount().getType() instanceof AmmoType;
        }
        int system = slot.getIndex();
        if (system == Mek.SYSTEM_COCKPIT) {
            return true;
        }
        if (system == Mek.SYSTEM_ENGINE) {
            return (engineHits + 1) >= ENGINE_HITS_TO_DESTROY;
        }
        if (system == Mek.SYSTEM_GYRO) {
            int hitsToDestroy = (((Mek) entity).getGyroType() == Mek.GYRO_HEAVY_DUTY)
                  ? HEAVY_DUTY_GYRO_HITS_TO_DESTROY : GYRO_HITS_TO_DESTROY;
            return (gyroHits + 1) >= hitsToDestroy;
        }
        return false;
    }

    private void recordMekSlotCrit(int location, int slotIndex) {
        assignedSlotKeys.add(slotKey(location, slotIndex));
        CriticalSlot slot = entity.getCritical(location, slotIndex);
        if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
            critAssignments.add(new EquipmentCrit(entity.getEquipmentNum(slot.getMount())));
            return;
        }
        int system = slot.getIndex();
        if (system == Mek.SYSTEM_ENGINE) {
            engineHits++;
        } else if (system == Mek.SYSTEM_GYRO) {
            gyroHits++;
        }
        critAssignments.add(new MekSystemCrit(system, location));
    }

    // ----- vehicle crits -----

    /**
     * Combat vehicle critical hit candidates. The allowed entries are the results of the vehicle critical hit tables
     * (TW p.194) that the unit editor dialog can represent; the forbidden entries stand in for the destroying or
     * immobilizing results so the FSW reroll rule still triggers.
     */
    private enum VehiclePoolEntry {
        WEAPON(false),
        STABILIZER(false),
        SENSORS(false),
        MOTIVE(false),
        TURRET_LOCK(false),
        ENGINE_DESTROYED(true),
        AMMO(true),
        CREW_KILLED(true),
        MOTIVE_IMMOBILIZING(true);

        private final boolean forbidden;

        VehiclePoolEntry(boolean forbidden) {
            this.forbidden = forbidden;
        }
    }

    /**
     * Rolls one combat vehicle critical hit and records it.
     *
     * <p>A forbidden pick (one that would destroy or immobilize the unit) is never recorded. Per the FSW rules it is
     * rerolled once; if the reroll is also forbidden the crit is discarded and a fallback damage group is queued
     * instead, so the unit still takes the damage without being killed.</p>
     *
     * @param location the location the damage group hit, used to place a stabilizer crit when one is rolled
     */
    private void assignVehicleCrit(int location) {
        List<VehiclePoolEntry> pool = buildVehiclePool();
        if (pool.isEmpty()) {
            return;
        }
        VehiclePoolEntry firstPick = pool.get(Compute.randomInt(pool.size()));
        if (!firstPick.forbidden) {
            recordVehicleCrit(firstPick, location);
            return;
        }
        VehiclePoolEntry secondPick = pool.get(Compute.randomInt(pool.size()));
        if (!secondPick.forbidden) {
            recordVehicleCrit(secondPick, location);
            return;
        }
        queueFallbackGroup();
    }

    /**
     * Builds the candidate pool for one combat vehicle critical hit roll.
     *
     * <p>The pool deliberately still contains the destroying and immobilizing results ({@code ENGINE_DESTROYED},
     * {@code CREW_KILLED}, {@code AMMO}, {@code MOTIVE_IMMOBILIZING}). They are not filtered out here because they
     * must keep their share of the roll probability, otherwise the remaining crits would come up far more often than
     * the vehicle critical hit table allows. {@link #assignVehicleCrit(int)} is what rejects them: a forbidden pick is
     * never recorded, it triggers the FSW reroll rule instead.</p>
     *
     * @return the entries that may be rolled for this vehicle, forbidden entries included
     */
    private List<VehiclePoolEntry> buildVehiclePool() {
        Tank tank = (Tank) entity;
        List<VehiclePoolEntry> pool = new ArrayList<>();
        if (pickableEquipment() != null) {
            pool.add(VehiclePoolEntry.WEAPON);
        }
        if (!stabilizerCandidateLocations().isEmpty()) {
            pool.add(VehiclePoolEntry.STABILIZER);
        }
        if (sensorHits < Tank.CRIT_SENSOR_MAX) {
            pool.add(VehiclePoolEntry.SENSORS);
        }
        if (motiveHits < (VEHICLE_MOTIVE_HITS_TO_IMMOBILIZE - 1)) {
            pool.add(VehiclePoolEntry.MOTIVE);
        }
        if (!tank.hasNoTurret() && !turretLocked) {
            pool.add(VehiclePoolEntry.TURRET_LOCK);
        }
        pool.add(VehiclePoolEntry.ENGINE_DESTROYED);
        pool.add(VehiclePoolEntry.CREW_KILLED);
        if (!entity.getAmmo().isEmpty()) {
            pool.add(VehiclePoolEntry.AMMO);
        }
        // a further motive hit would immobilize the vehicle; make it a reroll trigger instead
        if (motiveHits == (VEHICLE_MOTIVE_HITS_TO_IMMOBILIZE - 1)) {
            pool.add(VehiclePoolEntry.MOTIVE_IMMOBILIZING);
        }
        return pool;
    }

    private void recordVehicleCrit(VehiclePoolEntry pick, int location) {
        switch (pick) {
            case WEAPON -> {
                Mounted<?> equipment = pickableEquipment();
                if (equipment != null) {
                    int equipmentNumber = entity.getEquipmentNum(equipment);
                    assignedEquipmentNumbers.add(equipmentNumber);
                    critAssignments.add(new EquipmentCrit(equipmentNumber));
                }
            }
            case STABILIZER -> {
                List<Integer> candidates = stabilizerCandidateLocations();
                int stabilizerLocation = candidates.contains(location)
                      ? location : candidates.get(Compute.randomInt(candidates.size()));
                assignedStabilizerLocations.add(stabilizerLocation);
                critAssignments.add(new VehicleCrit(VehicleCritKind.STABILIZER, stabilizerLocation));
            }
            case SENSORS -> {
                sensorHits++;
                critAssignments.add(new VehicleCrit(VehicleCritKind.SENSORS, Entity.LOC_NONE));
            }
            case MOTIVE -> {
                motiveHits++;
                critAssignments.add(new VehicleCrit(VehicleCritKind.MOTIVE, Entity.LOC_NONE));
            }
            case TURRET_LOCK -> {
                turretLocked = true;
                critAssignments.add(new VehicleCrit(VehicleCritKind.TURRET_LOCK, Entity.LOC_NONE));
            }
            default -> LOGGER.warn("Unexpected vehicle crit pick: {}", pick);
        }
    }

    private List<Integer> stabilizerCandidateLocations() {
        Tank tank = (Tank) entity;
        List<Integer> candidates = new ArrayList<>();
        for (int location = 0; location < tank.locations(); location++) {
            if ((location == Tank.LOC_BODY) || (location == tank.getLocTurret()) || (location
                  == tank.getLocTurret2())) {
                continue;
            }
            if ((entity instanceof VTOL) && (location == VTOL.LOC_ROTOR)) {
                // the rotor's flight stabilizer is a separate dialog control but still a stabilizer hit
                if (!tank.isStabiliserHit(location) && !assignedStabilizerLocations.contains(location)) {
                    candidates.add(location);
                }
                continue;
            }
            if (!tank.isStabiliserHit(location) && !assignedStabilizerLocations.contains(location)) {
                candidates.add(location);
            }
        }
        return candidates;
    }

    // ----- fighter crits -----

    /**
     * Fighter critical hit candidates: the results of the aerospace critical hits tables (TW p.238) the unit editor
     * dialog can represent, plus stand-ins for the destroying results so the FSW reroll rule still triggers.
     */
    private enum FighterPoolEntry {
        WEAPON(false),
        AVIONICS(false),
        FIRE_CONTROL_SYSTEM(false),
        SENSORS(false),
        ENGINE(false),
        LANDING_GEAR(false),
        ENGINE_DESTROYED(true),
        FUEL_TANK(true),
        CREW_KILLED(true),
        AMMO(true);

        private final boolean forbidden;

        FighterPoolEntry(boolean forbidden) {
            this.forbidden = forbidden;
        }
    }

    /**
     * Rolls one aerospace fighter critical hit and records it.
     *
     * <p>A forbidden pick (one that would destroy the unit) is never recorded. Per the FSW rules it is rerolled once;
     * if the reroll is also forbidden the crit is discarded and a fallback damage group is queued instead, so the unit
     * still takes the damage without being killed.</p>
     */
    private void assignFighterCrit() {
        List<FighterPoolEntry> pool = buildFighterPool();
        if (pool.isEmpty()) {
            return;
        }
        FighterPoolEntry firstPick = pool.get(Compute.randomInt(pool.size()));
        if (!firstPick.forbidden) {
            recordFighterCrit(firstPick);
            return;
        }
        FighterPoolEntry secondPick = pool.get(Compute.randomInt(pool.size()));
        if (!secondPick.forbidden) {
            recordFighterCrit(secondPick);
            return;
        }
        queueFallbackGroup();
    }

    /**
     * Builds the candidate pool for one aerospace fighter critical hit roll.
     *
     * <p>As with the vehicle pool, the destroying results ({@code ENGINE_DESTROYED}, {@code FUEL_TANK},
     * {@code CREW_KILLED}, {@code AMMO}) stay in the pool on purpose so they keep their share of the roll probability.
     * {@link #assignFighterCrit()} rejects them: a forbidden pick is never recorded, it triggers the FSW reroll rule
     * instead.</p>
     *
     * @return the entries that may be rolled for this fighter, forbidden entries included
     */
    private List<FighterPoolEntry> buildFighterPool() {
        List<FighterPoolEntry> pool = new ArrayList<>();
        if (pickableEquipment() != null) {
            pool.add(FighterPoolEntry.WEAPON);
        }
        if (avionicsHits < AERO_SYSTEM_HITS_MAX) {
            pool.add(FighterPoolEntry.AVIONICS);
        }
        if (fcsHits < AERO_SYSTEM_HITS_MAX) {
            pool.add(FighterPoolEntry.FIRE_CONTROL_SYSTEM);
        }
        if (sensorHits < AERO_SYSTEM_HITS_MAX) {
            pool.add(FighterPoolEntry.SENSORS);
        }
        if ((engineHits + 1) < ENGINE_HITS_TO_DESTROY) {
            pool.add(FighterPoolEntry.ENGINE);
        } else {
            pool.add(FighterPoolEntry.ENGINE_DESTROYED);
        }
        if (!gearHit) {
            pool.add(FighterPoolEntry.LANDING_GEAR);
        }
        pool.add(FighterPoolEntry.FUEL_TANK);
        pool.add(FighterPoolEntry.CREW_KILLED);
        if (!entity.getAmmo().isEmpty()) {
            pool.add(FighterPoolEntry.AMMO);
        }
        return pool;
    }

    private void recordFighterCrit(FighterPoolEntry pick) {
        switch (pick) {
            case WEAPON -> {
                Mounted<?> equipment = pickableEquipment();
                if (equipment != null) {
                    int equipmentNumber = entity.getEquipmentNum(equipment);
                    assignedEquipmentNumbers.add(equipmentNumber);
                    critAssignments.add(new EquipmentCrit(equipmentNumber));
                }
            }
            case AVIONICS -> {
                avionicsHits++;
                critAssignments.add(new AeroFighterCrit(AeroFighterCritKind.AVIONICS));
            }
            case FIRE_CONTROL_SYSTEM -> {
                fcsHits++;
                critAssignments.add(new AeroFighterCrit(AeroFighterCritKind.FIRE_CONTROL_SYSTEM));
            }
            case SENSORS -> {
                sensorHits++;
                critAssignments.add(new AeroFighterCrit(AeroFighterCritKind.SENSORS));
            }
            case ENGINE -> {
                engineHits++;
                critAssignments.add(new AeroFighterCrit(AeroFighterCritKind.ENGINE));
            }
            case LANDING_GEAR -> {
                gearHit = true;
                critAssignments.add(new AeroFighterCrit(AeroFighterCritKind.LANDING_GEAR));
            }
            default -> LOGGER.warn("Unexpected fighter crit pick: {}", pick);
        }
    }

    // ----- shared helpers -----

    /**
     * @return a random hittable, undamaged, not-yet-assigned piece of equipment (weapons first come to mind, but any
     *       hittable equipment qualifies, matching the dialog's equipment list), or {@code null} if none is left.
     *       Ammunition is excluded; ammo hits are forbidden results (FSW p.145).
     */
    private @Nullable Mounted<?> pickableEquipment() {
        List<Mounted<?>> candidates = new ArrayList<>();
        for (Mounted<?> mounted : entity.getEquipment()) {
            if ((mounted.getLocation() == Entity.LOC_NONE) || !mounted.getType().isHittable()
                  || mounted.isWeaponGroup() || (mounted.getType() instanceof AmmoType)) {
                continue;
            }
            if (mounted.isDestroyed() || mounted.isHit()) {
                continue;
            }
            if (assignedEquipmentNumbers.contains(entity.getEquipmentNum(mounted))) {
                continue;
            }
            candidates.add(mounted);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(Compute.randomInt(candidates.size()));
    }
}
