/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.handlers;

import megamek.common.equipment.INarcPod;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.MiscType;
import megamek.common.units.Entity;

import java.util.Iterator;

/**
 * Utility class for detecting qualifying electronics for ARAD (Anti-Radiation) missiles.
 *
 * ARAD missiles receive bonuses against targets using electronic systems and penalties
 * against targets without electronics. This class centralizes all equipment detection
 * logic used by ARAD weapon handlers.
 *
 * Rules Reference: Tactical Operations: Advanced Units & Equipment, p.180
 * Forum Rulings: Consolidated in docs/issues/features/arad-implementation.md
 *
 * @author MegaMek Team
 * @since 2025-01-16
 */
public class ARADEquipmentDetector {

    /**
     * Determines if a target has qualifying electronics for ARAD missile bonuses.
     *
     * ARAD missiles receive:
     * - Bonuses (-1 to-hit, +1 cluster) against targets with qualifying electronics
     * - Penalties (+2 to-hit, -2 cluster) against targets without electronics
     *
     * Active Stealth Armor blocks ALL internal systems but NOT external Narc pods.
     *
     * @param target The entity being targeted
     * @param friendlyTeam Team ID of the attacking entity (for Narc pod ownership check)
     * @return true if target has qualifying electronics, false otherwise
     */
    public static boolean targetHasQualifyingElectronics(Entity target, int friendlyTeam) {
        if (target == null) {
            return false;
        }

        // Priority 1: Check for active Stealth Armor
        // Stealth blocks ALL internal systems but NOT external Narc pods
        if (hasActiveStealthArmor(target)) {
            // Only external Narc pods count when Stealth is active
            return isNarcTagged(target, friendlyTeam);
        }

        // Priority 2: Check all qualifying systems
        return hasActiveProbe(target) ||
               hasArtemis(target) ||
               hasBlueShield(target) ||
               hasC3(target) ||
               hasHeavyComms(target) ||
               hasECM(target) ||
               hasActiveTAG(target) ||
               hasGhostTargets(target) ||
               isNarcTagged(target, friendlyTeam);
    }

    /**
     * Checks if target has active Stealth Armor.
     *
     * Active Stealth Armor makes a unit "completely non-emitting" by blocking
     * all internal electronic systems. External Narc pods are NOT blocked.
     *
     * @param target The entity to check
     * @return true if Stealth Armor is active
     */
    public static boolean hasActiveStealthArmor(Entity target) {
        return target.isStealthActive();
    }

    /**
     * Checks if target has an active Active Probe.
     *
     * @param target The entity to check
     * @return true if entity has a functional Active Probe
     */
    public static boolean hasActiveProbe(Entity target) {
        if (!target.hasBAP()) {
            return false;
        }

        // Verify at least one Active Probe is functional and powered
        for (Mounted<?> equipment : target.getEquipment()) {
            if (equipment.getType().hasFlag(MiscType.F_BAP) &&
                isValidEquipment(equipment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if target has functional Artemis fire-control system.
     *
     * @param target The entity to check
     * @return true if entity has functional Artemis IV or Artemis V
     */
    public static boolean hasArtemis(Entity target) {
        for (Mounted<?> equipment : target.getEquipment()) {
            if ((equipment.getType().hasFlag(MiscType.F_ARTEMIS) ||
                 equipment.getType().hasFlag(MiscType.F_ARTEMIS_V)) &&
                isValidEquipment(equipment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if target has functional Blue Shield system.
     *
     * @param target The entity to check
     * @return true if entity has functional Blue Shield
     */
    public static boolean hasBlueShield(Entity target) {
        for (Mounted<?> equipment : target.getEquipment()) {
            if (equipment.getType().hasFlag(MiscType.F_BLUE_SHIELD) &&
                isValidEquipment(equipment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if target has functional C3 system.
     *
     * C3 systems qualify if they are powered on. Network status is IRRELEVANT -
     * an isolated C3 computer still emits electronic signals.
     *
     * Note: C3 Master and C3 Company Master are weapons, not equipment, so we
     * check Entity methods hasC3M() and hasC3MM() instead of equipment flags.
     *
     * @param target The entity to check
     * @return true if entity has functional C3 (any type)
     */
    public static boolean hasC3(Entity target) {
        // Quick check: does entity have any C3 system?
        // hasC3() returns true for C3 Slave/Master/Company Master
        // hasC3i() returns true for C3i systems
        if (!target.hasC3() && !target.hasC3i()) {
            return false;
        }

        // C3 Master and Company Master are weapons - check via Entity methods
        if (target.hasC3M() || target.hasC3MM()) {
            return true;
        }

        // Check equipment-based C3 systems (Slave, Boosted Slave, C3i, Nova CEWS)
        for (Mounted<?> equipment : target.getEquipment()) {
            if ((equipment.getType().hasFlag(MiscType.F_C3S) ||
                 equipment.getType().hasFlag(MiscType.F_C3I) ||
                 equipment.getType().hasFlag(MiscType.F_C3SBS) ||
                 equipment.getType().hasFlag(MiscType.F_NOVA)) &&
                isValidEquipment(equipment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if target has heavy dedicated communications equipment.
     *
     * Only dedicated communications equipment >= 3.5 tons qualifies.
     * Built-in cockpit communications (standard 1-ton equivalent) does NOT count.
     *
     * @param target The entity to check
     * @return true if entity has >= 3.5 tons of dedicated comms
     */
    public static boolean hasHeavyComms(Entity target) {
        for (Mounted<?> equipment : target.getEquipment()) {
            if (equipment.getType().hasFlag(MiscType.F_COMMUNICATIONS) &&
                equipment.getType().getTonnage(target) >= 3.5 &&
                isValidEquipment(equipment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if target has active ECM suite.
     *
     * @param target The entity to check
     * @return true if entity has functional ECM
     */
    public static boolean hasECM(Entity target) {
        if (!target.hasECM()) {
            return false;
        }

        // Verify at least one ECM suite is functional and powered
        for (Mounted<?> equipment : target.getEquipment()) {
            if (equipment.getType().hasFlag(MiscType.F_ECM) &&
                isValidEquipment(equipment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if target has been TAG'd earlier this turn.
     *
     * TAG fires during the OFFBOARD phase. ARAD checks TAG state during
     * the FIRING phase, so TAG state is available when needed.
     *
     * @param target The entity to check
     * @return true if target was TAG'd this turn
     */
    public static boolean hasActiveTAG(Entity target) {
        return target.getTaggedBy() != -1;
    }

    /**
     * Checks if target is generating Ghost Targets.
     *
     * Ghost Targets are generated by ECM equipment in specific modes.
     *
     * @param target The entity to check
     * @return true if target is actively generating Ghost Targets
     */
    public static boolean hasGhostTargets(Entity target) {
        return target.hasGhostTargets(true);
    }

    /**
     * Checks if target has been tagged by friendly Narc or iNarc pods.
     *
     * Only FRIENDLY pods count:
     * - Standard Narc Missile Beacons (friendly team)
     * - iNarc Homing Pods (friendly team)
     * - iNarc Nemesis Pods (friendly team only)
     *
     * EXCLUDED:
     * - iNarc Haywire Pods (do not trigger ARAD)
     * - Enemy Narc/iNarc pods (team ownership check fails)
     *
     * External Narc pods are NOT blocked by Stealth Armor.
     *
     * @param target The entity to check
     * @param friendlyTeam Team ID of the attacking entity
     * @return true if target has friendly Narc/iNarc pod
     */
    public static boolean isNarcTagged(Entity target, int friendlyTeam) {
        // Check standard Narc
        if (target.isNarcedBy(friendlyTeam)) {
            return true;
        }

        // Check iNarc pods (Homing and Nemesis only, must be friendly)
        Iterator<INarcPod> iNarcPods = target.getINarcPodsAttached();
        while (iNarcPods.hasNext()) {
            INarcPod pod = iNarcPods.next();
            if (pod.team() == friendlyTeam &&
                (pod.type() == INarcPod.HOMING || pod.type() == INarcPod.NEMESIS)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validates that equipment is functional and powered.
     *
     * Equipment must meet ALL criteria:
     * - Not destroyed
     * - Not missing
     * - Not breached
     *
     * Note: Power state (shut down) is checked via equipment modes, not this method.
     *
     * @param equipment The equipment to validate
     * @return true if equipment is functional
     */
    private static boolean isValidEquipment(Mounted<?> equipment) {
        return !equipment.isDestroyed() &&
               !equipment.isMissing() &&
               !equipment.isBreached();
    }
}
