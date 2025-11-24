/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons.handlers;

import java.util.Iterator;

import megamek.common.equipment.INarcPod;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for detecting qualifying electronics for ARAD (Anti-Radiation) missiles.
 * <p>
 * ARAD missiles receive bonuses against targets using electronic systems and penalties against targets without
 * electronics. This class centralizes all equipment detection logic used by ARAD weapon handlers.
 * <p>
 * Rules Reference: Tactical Operations: Advanced Units &amp; Equipment, p.180 Forum Rulings: Consolidated in
 * docs/issues/features/arad-implementation.md
 * <p>
 * Key Forum Rulings:
 * <ul>
 *   <li>Stealth blocking: https://battletech.com/forums/index.php?topic=78845.msg1866412#msg1866412</li>
 *   <li>Powered-down equipment: https://battletech.com/forums/index.php?topic=31896.msg1369654#msg1369654</li>
 *   <li>Communications threshold: https://battletech.com/forums/index.php?topic=17456.msg396440#msg396440</li>
 *   <li>Narc/iNarc interaction: https://battletech.com/forums/index.php?topic=26824.msg609067#msg609067</li>
 *   <li>General exclusions: https://battletech.com/forums/index.php?topic=63179.msg1452217#msg1452217</li>
 * </ul>
 *
 * @author Hammer - Built with Claude Code
 * @since 2025-01-16
 */
public class ARADEquipmentDetector {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Determines if a target has qualifying electronics for ARAD missile bonuses.
     * <p>
     * ARAD missiles receive:
     * <ul>
     *   <li>Bonuses (-1 to-hit, +1 cluster) against targets with qualifying electronics</li>
     *   <li>Penalties (+2 to-hit, -2 cluster) against targets without electronics</li>
     * </ul>
     * <p>
     * Active Stealth Armor blocks ALL internal systems but NOT external Narc pods.
     *
     * @param target       The entity being targeted
     * @param friendlyTeam Team ID of the attacking entity (for Narc pod ownership check)
     *
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
     * <p>
     * Active Stealth Armor makes a unit "completely non-emitting" by blocking all internal electronic systems. External
     * Narc pods are NOT blocked.
     * <p>
     * Reference: TO:AUE p.180, Xotl forum ruling:
     * https://battletech.com/forums/index.php?topic=78845.msg1866412#msg1866412 Quote: "Active Stealth Armor makes a
     * unit completely non-emitting"
     *
     * @param target The entity to check
     *
     * @return true if Stealth Armor is active
     */
    public static boolean hasActiveStealthArmor(Entity target) {
        return target.isStealthActive();
    }

    /**
     * Checks if target has an active Active Probe.
     *
     * @param target The entity to check
     *
     * @return true if entity has a functional Active Probe
     */
    public static boolean hasActiveProbe(Entity target) {
        if (!target.hasBAP()) {
            return false;
        }

        // Verify at least one Active Probe is functional and powered
        for (Mounted<?> equipment : target.getEquipment()) {
            if (equipment.getType() instanceof MiscType miscType &&
                  miscType.hasFlag(MiscType.F_BAP) &&
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
     *
     * @return true if entity has functional Artemis IV or Artemis V
     */
    public static boolean hasArtemis(Entity target) {
        for (Mounted<?> equipment : target.getEquipment()) {
            if (equipment.getType() instanceof MiscType miscType &&
                  (miscType.hasFlag(MiscType.F_ARTEMIS) ||
                        miscType.hasFlag(MiscType.F_ARTEMIS_V)) &&
                  isValidEquipment(equipment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if target has active Blue Shield system.
     * <p>
     * Blue Shield must be powered on (mode "On") to count as qualifying electronics. Powered-down Blue Shield does not
     * emit detectable signals.
     *
     * @param target The entity to check
     *
     * @return true if entity has Blue Shield in "On" mode and functional
     */
    public static boolean hasBlueShield(Entity target) {
        for (Mounted<?> equipment : target.getEquipment()) {
            if (equipment.getType() instanceof MiscType miscType &&
                  miscType.hasFlag(MiscType.F_BLUE_SHIELD) &&
                  equipment.curMode().equals("On") &&
                  isValidEquipment(equipment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if target has functional C3 system.
     * <p>
     * C3 systems qualify if they are powered on. Network status is IRRELEVANT - an isolated C3 computer still emits
     * electronic signals.
     * <p>
     * Note: C3 Master and C3 Company Master are weapons, not equipment, so we check Entity methods hasC3M() and
     * hasC3MM() instead of equipment flags.
     *
     * @param target The entity to check
     *
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
            if (equipment.getType() instanceof MiscType miscType &&
                  (miscType.hasFlag(MiscType.F_C3S) ||
                        miscType.hasFlag(MiscType.F_C3I) ||
                        miscType.hasFlag(MiscType.F_C3SBS) ||
                        miscType.hasFlag(MiscType.F_NOVA)) &&
                  isValidEquipment(equipment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if target has heavy dedicated communications equipment.
     * <p>
     * Only dedicated communications equipment >= 3.5 tons qualifies. Built-in cockpit communications (standard 1-ton
     * equivalent) does NOT count.
     * <p>
     * Reference: Welshman (Catalyst Freelancer) forum ruling:
     * https://battletech.com/forums/index.php?topic=17456.msg396440#msg396440 Quote: "Built-in 1-ton communications do
     * not count, only dedicated equipment >= 3.5 tons"
     *
     * @param target The entity to check
     *
     * @return true if entity has >= 3.5 tons of dedicated comms
     */
    public static boolean hasHeavyComms(Entity target) {
        LOGGER.debug("[ARAD] Checking hasHeavyComms() for target: {}", target.getDisplayName());
        int equipmentCount = 0;
        for (Mounted<?> equipment : target.getEquipment()) {
            equipmentCount++;
            boolean hasFlag = equipment.getType() instanceof MiscType miscType &&
                  miscType.hasFlag(MiscType.F_COMMUNICATIONS);
            double tonnage = equipment.getTonnage();  // Use Mounted.getTonnage(), not EquipmentType.getTonnage()
            boolean isValid = isValidEquipment(equipment);

            LOGGER.debug("[ARAD]   Equipment #{}: {} (hasFlag={}, tonnage={}, isValid={})",
                  equipmentCount, equipment.getName(), hasFlag, tonnage, isValid);

            if (hasFlag && tonnage >= 3.5 && isValid) {
                LOGGER.debug("[ARAD]   MATCH FOUND: Heavy comms detected!");
                return true;
            }
        }
        LOGGER.debug("[ARAD] No heavy comms found (checked {} equipment items)", equipmentCount);
        return false;
    }

    /**
     * Checks if target has active ECM suite.
     * <p>
     * ECM equipment qualifies regardless of mode (ECM/ECCM/Ghost Targets). The ECM field blocking effect is handled
     * separately in cluster calculation.
     *
     * @param target The entity to check
     *
     * @return true if entity has functional ECM
     */
    public static boolean hasECM(Entity target) {
        if (!target.hasECM()) {
            return false;
        }

        // Verify at least one ECM suite is functional and powered
        // Use getMisc() instead of getEquipment() to match Entity.hasECM() behavior
        for (Mounted<?> equipment : target.getEquipment()) {
            if (equipment.getType() instanceof MiscType miscType &&
                  miscType.hasFlag(MiscType.F_ECM) &&
                  isValidEquipment(equipment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if target has been TAG'd earlier this turn.
     * <p>
     * TAG fires during the OFFBOARD phase. ARAD checks TAG state during the FIRING phase, so TAG state is available
     * when needed.
     *
     * @param target The entity to check
     *
     * @return true if target was TAG'd this turn
     */
    public static boolean hasActiveTAG(Entity target) {
        return target.getTaggedBy() != -1;
    }

    /**
     * Checks if target is generating Ghost Targets.
     * <p>
     * Ghost Targets are generated by ECM equipment in specific modes.
     *
     * @param target The entity to check
     *
     * @return true if target is actively generating Ghost Targets
     */
    public static boolean hasGhostTargets(Entity target) {
        return target.hasGhostTargets(true);
    }

    /**
     * Checks if target has been tagged by friendly Narc or iNarc pods.
     * <p>
     * Only FRIENDLY pods count:
     * <ul>
     *   <li>Standard Narc Missile Beacons (friendly team)</li>
     *   <li>iNarc Homing Pods (friendly team)</li>
     *   <li>iNarc Nemesis Pods (friendly team only)</li>
     * </ul>
     * <p>
     * EXCLUDED:
     * <ul>
     *   <li>iNarc Haywire Pods (do not trigger ARAD)</li>
     *   <li>Enemy Narc/iNarc pods (team ownership check fails)</li>
     * </ul>
     * <p>
     * External Narc pods are NOT blocked by Stealth Armor.
     * <p>
     * Reference: TO:AUE p.180, Xotl forum ruling:
     * https://battletech.com/forums/index.php?topic=26824.msg609067#msg609067 Quote: "ARAD receives standard bonus when
     * targeting Narc-tagged units, but does NOT receive additional Narc-specific bonuses (no stacking)"
     * <p>
     * Friendly vs enemy Nemesis distinction confirmed by same forum ruling.
     *
     * @param target       The entity to check
     * @param friendlyTeam Team ID of the attacking entity
     *
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
     * <p>
     * Equipment must meet ALL criteria:
     * <ul>
     *   <li>Not destroyed</li>
     *   <li>Not missing</li>
     *   <li>Not breached</li>
     * </ul>
     * <p>
     * Note: Power state (shut down) is checked via equipment modes, not this method. Powered-down equipment does NOT
     * emit and will not trigger ARAD bonuses.
     * <p>
     * Reference: Xotl ruling (November 2017, Errata to TO p.99):
     * https://battletech.com/forums/index.php?topic=31896.msg1369654#msg1369654 Quote: "Players may power down
     * equipment during End Phase. Powered-down electronics do not function and are not valid targets for ARAD
     * bonuses."
     *
     * @param equipment The equipment to validate
     *
     * @return true if equipment is functional
     */
    private static boolean isValidEquipment(Mounted<?> equipment) {
        return !equipment.isDestroyed() &&
              !equipment.isMissing() &&
              !equipment.isBreached();
    }
}
