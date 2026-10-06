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

import megamek.common.CriticalSlot;
import megamek.common.compute.damage.CritAssignment.AeroFighterCrit;
import megamek.common.compute.damage.CritAssignment.AeroFighterCritKind;
import megamek.common.compute.damage.CritAssignment.EquipmentCrit;
import megamek.common.compute.damage.CritAssignment.MekSystemCrit;
import megamek.common.compute.damage.CritAssignment.VehicleCrit;
import megamek.common.compute.damage.CritAssignment.VehicleCritKind;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.units.*;

/**
 * Rolls pre-existing damage for a unit and commits it straight to the unit, without the unit editor dialog. This is
 * the headless counterpart of the editor's Pre-Existing Damage roll followed by Okay, for callers such as MekHQ that
 * damage units outside a game.
 *
 * <p>The roll comes from {@link PreExistingDamageApplier#simulate(Entity, PreExistingDamageLevel)}, so the same FSW
 * rules apply, including the guarantee that the damage never destroys or immobilizes the unit. The rolled values are
 * then turned into a {@link DamageEditSpec} and applied with {@link DamageEditApplier}, exactly as the editor commits
 * its controls. Only the values the roll changed are put in the spec, so everything else about the unit is left as it
 * was.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class PreExistingDamageCommitter {
    /** Hits on a vehicle's or fighter's one-box systems (turret lock, stabilizers, landing gear). */
    private static final int SINGLE_HIT_MAXIMUM = 1;

    /** Hits the unit editor allows on a fighter's avionics, fire control, and sensors. */
    private static final int FIGHTER_SYSTEM_HITS_MAXIMUM = 3;

    /** Motive hits at which the unit editor treats a vehicle as immobile. */
    private static final int VEHICLE_MOTIVE_HITS_MAXIMUM = 4;
    private static final int VEHICLE_HEAVY_MOTIVE_HITS = 3;
    private static final int VEHICLE_MODERATE_MOTIVE_HITS = 2;
    private static final int VEHICLE_MINOR_MOTIVE_HITS = 1;

    /** The actuator columns carried per limb: hip or shoulder through foot or hand, plus a QuadVee's conversion gear. */
    private static final int ACTUATOR_COLUMNS = DamageEditSpec.CONVERSION_GEAR_INDEX + 1;

    private PreExistingDamageCommitter() {}

    /**
     * Rolls pre-existing damage at the given level and applies it to the unit.
     *
     * @param entity the unit to damage; must be {@link PreExistingDamageApplier#isSupported(Entity) supported}
     * @param level  the Pre-Existing Damage Table result to apply
     *
     * @return {@code true} if damage was applied; {@code false} if the level is {@link PreExistingDamageLevel#NONE} or
     *       the unit type isn't covered by the pre-existing damage rules
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean rollAndApply(Entity entity, PreExistingDamageLevel level) {
        if ((level == PreExistingDamageLevel.NONE) || !PreExistingDamageApplier.isSupported(entity)) {
            return false;
        }

        PreExistingDamageResult result = PreExistingDamageApplier.simulate(entity, level);
        new DamageEditApplier(entity, buildSpec(entity, result)).applyToEntity();
        return true;
    }

    /**
     * Turns a simulated result into the edit the unit editor would have committed for it.
     *
     * @param entity the unit the result was rolled for, still undamaged by it
     * @param result the simulated damage, as absolute remaining values plus critical hits to add
     *
     * @return the edit, holding only the values the result changes
     *
     * @author Illiani
     * @since 0.51.01
     */
    static DamageEditSpec buildSpec(Entity entity, PreExistingDamageResult result) {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = entity.getId();
        addStructureAndArmor(entity, result, spec);

        for (CritAssignment assignment : result.critAssignments()) {
            switch (assignment) {
                case EquipmentCrit(int equipmentNumber) -> addEquipmentHit(entity, equipmentNumber, spec);
                case MekSystemCrit(int system, int location) -> addMekSystemHit(entity, system, location, spec);
                case VehicleCrit(VehicleCritKind kind, int location) -> addVehicleHit(entity, kind, location, spec);
                case AeroFighterCrit(AeroFighterCritKind kind) -> addFighterHit(entity, kind, spec);
            }
        }

        return spec;
    }

    /**
     * Copies each armor, rear armor, and structure value the result changed into the spec. Unchanged locations stay
     * {@code null}, so a location without armor isn't rewritten as destroyed armor.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void addStructureAndArmor(Entity entity, PreExistingDamageResult result, DamageEditSpec spec) {
        int locations = entity.locations();
        spec.armor = new Integer[locations];
        spec.rearArmor = new Integer[locations];
        spec.internal = new Integer[locations];

        for (int location = 0; location < locations; location++) {
            spec.armor[location] = changedValue(entity.getArmor(location, false), result.armor()[location]);
            if (entity.hasRearArmor(location)) {
                spec.rearArmor[location] = changedValue(entity.getArmor(location, true),
                      result.rearArmor()[location]);
            }
        }

        // The editor carries a fighter's structural integrity in its first structure value
        if (entity instanceof Aero aero) {
            spec.internal[0] = changedValue(aero.getSI(), result.structuralIntegrity());
            return;
        }

        for (int location = 0; location < locations; location++) {
            spec.internal[location] = changedValue(entity.getInternal(location), result.internal()[location]);
        }
    }

    /**
     * @return the rolled value if it differs from the unit's current value, otherwise {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static Integer changedValue(int currentValue, int rolledValue) {
        return (Math.max(currentValue, 0) == rolledValue) ? null : rolledValue;
    }

    /**
     * @return the running value plus one, starting from {@code currentHits} the first time, and never above
     *       {@code maximumHits}
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static int addHit(Integer runningHits, int currentHits, int maximumHits) {
        int hits = (runningHits == null) ? currentHits : runningHits;
        return Math.min(hits + 1, maximumHits);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static void addEquipmentHit(Entity entity, int equipmentNumber, DamageEditSpec spec) {
        Mounted<?> mounted = entity.getEquipment(equipmentNumber);
        if (mounted == null) {
            return;
        }

        // Counted as the unit editor counts them, so the committed total matches what the editor would show
        int currentHits;
        if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
            currentHits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, equipmentNumber,
                  Mek.LOC_LEFT_TORSO)
                  + entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, equipmentNumber, Mek.LOC_RIGHT_TORSO);
        } else {
            currentHits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, equipmentNumber,
                  mounted.getLocation());
            if (mounted.isSplit()) {
                currentHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, equipmentNumber,
                      mounted.getSecondLocation());
            }
        }

        int maximumHits = (entity instanceof Mek) ? mounted.getNumCriticalSlots() : SINGLE_HIT_MAXIMUM;
        currentHits = Math.min(currentHits, maximumHits);
        spec.equipmentHits.put(equipmentNumber,
              addHit(spec.equipmentHits.get(equipmentNumber), currentHits, maximumHits));
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static void addMekSystemHit(Entity entity, int system, int location, DamageEditSpec spec) {
        if (entity instanceof LandAirMek) {
            if (system == LandAirMek.LAM_AVIONICS) {
                int currentHits = entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, system, location);
                spec.lamAvionicsHits.put(location, addHit(spec.lamAvionicsHits.get(location), currentHits,
                      Integer.MAX_VALUE));
                return;
            }
            if (system == LandAirMek.LAM_LANDING_GEAR) {
                int currentHits = entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, system, location);
                spec.lamLandingGearHits.put(location, addHit(spec.lamLandingGearHits.get(location), currentHits,
                      Integer.MAX_VALUE));
                return;
            }
        }

        if ((entity instanceof QuadVee) && (system == QuadVee.SYSTEM_CONVERSION_GEAR)) {
            addActuatorHit(entity, system, location, DamageEditSpec.CONVERSION_GEAR_INDEX, spec);
            return;
        }

        switch (system) {
            case Mek.SYSTEM_ENGINE -> {
                int currentHits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, system, location);
                switch (location) {
                    case Mek.LOC_LEFT_TORSO ->
                          spec.leftEngineHits = addHit(spec.leftEngineHits, currentHits, Integer.MAX_VALUE);
                    case Mek.LOC_RIGHT_TORSO ->
                          spec.rightEngineHits = addHit(spec.rightEngineHits, currentHits, Integer.MAX_VALUE);
                    default -> spec.centerEngineHits = addHit(spec.centerEngineHits,
                          entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, system, Mek.LOC_CENTER_TORSO),
                          Integer.MAX_VALUE);
                }
            }
            case Mek.SYSTEM_GYRO -> spec.gyroHits = addHit(spec.gyroHits, countSystemHits(entity, system),
                  Integer.MAX_VALUE);
            case Mek.SYSTEM_SENSORS -> spec.sensorHits = addHit(spec.sensorHits, countSystemHits(entity, system),
                  Integer.MAX_VALUE);
            case Mek.SYSTEM_LIFE_SUPPORT -> spec.lifeSupportHits = addHit(spec.lifeSupportHits,
                  countSystemHits(entity, system), Integer.MAX_VALUE);
            default -> {
                if ((system < Mek.ACTUATOR_SHOULDER) || (system > Mek.ACTUATOR_FOOT)) {
                    return;
                }
                int firstActuator = ((location >= Mek.LOC_RIGHT_LEG) || (entity instanceof QuadMek))
                      ? Mek.ACTUATOR_HIP : Mek.ACTUATOR_SHOULDER;
                addActuatorHit(entity, system, location, system - firstActuator, spec);
            }
        }
    }

    /**
     * @return the damaged slots of a system across every location, as the unit editor counts them
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static int countSystemHits(Entity entity, int system) {
        int hits = 0;
        for (int location = 0; location < entity.locations(); location++) {
            hits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, system, location);
        }
        return hits;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static void addActuatorHit(Entity entity, int system, int location, int column, DamageEditSpec spec) {
        int row = location - Mek.LOC_RIGHT_ARM;
        int limbCount = entity.locations() - Mek.LOC_RIGHT_ARM;
        if ((row < 0) || (row >= limbCount) || (column < 0) || (column >= ACTUATOR_COLUMNS)) {
            return;
        }

        if (spec.actuatorHits == null) {
            spec.actuatorHits = new Integer[limbCount][ACTUATOR_COLUMNS];
        }

        int currentHits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, system, location);
        // Each actuator is a single box in the editor
        spec.actuatorHits[row][column] = addHit(spec.actuatorHits[row][column], Math.min(currentHits, 1),
              SINGLE_HIT_MAXIMUM);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static void addVehicleHit(Entity entity, VehicleCritKind kind, int location, DamageEditSpec spec) {
        if (!(entity instanceof Tank tank)) {
            return;
        }

        switch (kind) {
            case TURRET_LOCK -> spec.turretLockHits = SINGLE_HIT_MAXIMUM;
            case SENSORS -> spec.sensorHits = addHit(spec.sensorHits, tank.getSensorHits(), Tank.CRIT_SENSOR_MAX);
            case MOTIVE -> spec.motiveHits = addHit(spec.motiveHits, currentMotiveHits(tank),
                  VEHICLE_MOTIVE_HITS_MAXIMUM);
            case STABILIZER -> {
                if ((tank instanceof VTOL) && (location == VTOL.LOC_ROTOR)) {
                    spec.flightStabilizerHits = SINGLE_HIT_MAXIMUM;
                } else if ((location >= 0) && (location < tank.locations())) {
                    if (spec.stabilizerHits == null) {
                        spec.stabilizerHits = new Integer[tank.locations()];
                    }
                    spec.stabilizerHits[location] = SINGLE_HIT_MAXIMUM;
                }
            }
        }
    }

    /**
     * @return the vehicle's motive damage as the unit editor's four-box count
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static int currentMotiveHits(Tank tank) {
        // Do not check the crew when determining if we're immobile here, matching the unit editor
        if (tank.isImmobile(false)) {
            return VEHICLE_MOTIVE_HITS_MAXIMUM;
        } else if (tank.hasHeavyMovementDamage()) {
            return VEHICLE_HEAVY_MOTIVE_HITS;
        } else if (tank.hasModerateMovementDamage()) {
            return VEHICLE_MODERATE_MOTIVE_HITS;
        } else if (tank.hasMinorMovementDamage()) {
            return VEHICLE_MINOR_MOTIVE_HITS;
        }
        return 0;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static void addFighterHit(Entity entity, AeroFighterCritKind kind, DamageEditSpec spec) {
        if (!(entity instanceof Aero aero)) {
            return;
        }

        switch (kind) {
            case AVIONICS -> spec.avionicsHits = addHit(spec.avionicsHits, aero.getAvionicsHits(),
                  FIGHTER_SYSTEM_HITS_MAXIMUM);
            case FIRE_CONTROL_SYSTEM -> spec.fcsHits = addHit(spec.fcsHits, aero.getFCSHits(),
                  FIGHTER_SYSTEM_HITS_MAXIMUM);
            case SENSORS -> spec.sensorHits = addHit(spec.sensorHits, aero.getSensorHits(),
                  FIGHTER_SYSTEM_HITS_MAXIMUM);
            case ENGINE -> spec.engineHits = addHit(spec.engineHits, aero.getEngineHits(),
                  FIGHTER_SYSTEM_HITS_MAXIMUM);
            case LANDING_GEAR -> spec.gearHits = SINGLE_HIT_MAXIMUM;
        }
    }
}
