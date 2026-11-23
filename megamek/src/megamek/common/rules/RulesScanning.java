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
package megamek.common.rules;

import java.util.Locale;

import jakarta.annotation.Nullable;
import megamek.common.CriticalSlot;
import megamek.common.enums.BasementType;
import megamek.common.equipment.EquipmentActivation;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;

/**
 * The rules for scanning an objective, a hex or an enemy unit with a unit's sensors, as the Objectives series
 * uses them. The two rulebooks differ on nearly every point: under Core Rules (p.233 Scanning, p.113 Sensor
 * Checks) a scan is a sensor check, a Piloting roll at +3 that ignores every ordinary modifier, reaching 2 hexes
 * or an active probe's range; under the optional TacOps: Advanced Rules scanning rule (p.187) a unit with working sensors scans anything in
 * line of sight with no roll unless the target sits inside hostile ECM. The server's scan pass asks this class
 * two things and does the rest itself: how far a unit can scan, and what it needs to roll against a target.
 */
public abstract class RulesScanning {

    /**
     * The scanning range of a ruleset that puts no distance limit on a scan: under the optional TacOps scanning rule a unit scans
     * whatever its sensors can reach, so the only limits are line of sight and the rules below.
     */
    public static final int UNLIMITED_RANGE = Integer.MAX_VALUE;

    /**
     * @param scanningRange a range from {@link #scanningRange(Entity, Targetable)}
     *
     * @return {@code true} when that range puts no limit on how far a scan may reach
     */
    public static boolean isUnlimitedRange(int scanningRange) {
        return scanningRange >= UNLIMITED_RANGE;
    }

    /** Probe level of a unit with no working active probe. */
    public static final int PROBE_LEVEL_NONE = 0;
    /** Light Active Probe (Core Rules p.233). */
    public static final int PROBE_LEVEL_LIGHT = 1;
    /** Standard active probes, including the Watchdog under Core equipment rules. */
    public static final int PROBE_LEVEL_STANDARD = 2;
    /** Bloodhound Active Probe. */
    public static final int PROBE_LEVEL_BLOODHOUND = 3;

    /**
     * @param scanner the unit that wants to scan
     * @param target  what it wants to scan, or {@code null} for the unit's general reach; an active probe is negated
     *                by hostile ECM at either end (Core Rules p.197), so the reach against a particular target can be
     *                shorter than the unit's own
     *
     * @return the farthest hex distance at which the unit can scan under these rules, or 0 when it cannot scan at
     *       all; line of sight is checked separately by the caller
     */
    public abstract int scanningRange(Entity scanner, @Nullable Targetable target);

    /**
     * The roll a scan needs. The value is {@link TargetRoll#IMPOSSIBLE} with the reason in the description when
     * the unit cannot scan this target at all, {@link TargetRoll#AUTOMATIC_SUCCESS} when no roll is needed, and
     * otherwise the 2d6 target number with every modifier named.
     *
     * @param scanner the scanning unit
     * @param target  the hex, building hex or unit being scanned
     *
     * @return the target roll, never {@code null}
     */
    public abstract TargetRoll scanTargetRoll(Entity scanner, Targetable target);

    /**
     * @param unit the unit to inspect
     *
     * @return the number of critical hits on the unit's sensors: the head sensor slots of a Mek (and the centre
     *       torso slots of a torso-mounted cockpit), the sensor hit counter of a vehicle or aerospace unit, 0 for
     *       every other type
     */
    public static int sensorCriticalHits(Entity unit) {
        if (unit instanceof Mek mek) {
            int sensorHits = mek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD);
            if (mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
                sensorHits += mek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS,
                      Mek.LOC_CENTER_TORSO);
            }
            return sensorHits;
        }
        if (unit instanceof Tank tank) {
            return tank.getSensorHits();
        }
        if (unit instanceof IAero aero) {
            return aero.getSensorHits();
        }
        return 0;
    }

    /**
     * @param unit the unit to inspect
     *
     * @return the level of the unit's active probe that is not negated by hostile ECM:
     *       {@link #PROBE_LEVEL_LIGHT}, {@link #PROBE_LEVEL_STANDARD} or {@link #PROBE_LEVEL_BLOODHOUND}, or
     *       {@link #PROBE_LEVEL_NONE} without one
     */
    public static int activeProbeLevel(Entity unit) {
        if (!unit.hasBAP(true)) {
            return PROBE_LEVEL_NONE;
        }
        MiscMounted probe = EquipmentActivation.activeProbe(unit);
        // A probe supplied by an implant or quirk counts as light.
        return probe == null ? PROBE_LEVEL_LIGHT : probeLevelOf(probe.getType());
    }

    /**
     * @param probeType the probe equipment type
     *
     * @return the Core Rules probe level of that equipment
     */
    private static int probeLevelOf(MiscType probeType) {
        String probeName = probeType.getInternalName().toLowerCase(Locale.ROOT);
        if (probeType.hasFlag(MiscType.F_WATCHDOG)) {
            return Game.rulesManager.getRulesEquipment().watchdogUsesLightProbe()
                  ? PROBE_LEVEL_LIGHT : PROBE_LEVEL_STANDARD;
        }
        if (probeType.hasFlag(MiscType.F_BLOODHOUND)) {
            return PROBE_LEVEL_BLOODHOUND;
        }
        if (probeName.contains("light")) {
            return PROBE_LEVEL_LIGHT;
        }
        return PROBE_LEVEL_STANDARD;
    }

    /**
     * @param scanner the scanning unit
     * @param target  the target, or {@code null}
     *
     * @return {@code true} when the target is the hex the scanner occupies or a building there, not a unit
     */
    protected static boolean isOwnHexOrBuilding(Entity scanner, @Nullable Targetable target) {
        if (target == null || target.getPosition() == null || scanner.getPosition() == null
              || scanner.getBoardId() != target.getBoardId()) {
            return false;
        }
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            return occupiesTargetBuilding(scanner, target);
        }
        return scanner.getPosition().equals(target.getPosition());
    }

    /** A building's footprint can extend beyond the hex stored as its position. */
    public static boolean occupiesTargetBuilding(Entity scanner, Targetable target) {
        if (!(target instanceof BuildingEntity building) || scanner.getBoardId() != building.getBoardId()
              || !building.getOccupiedCoords().contains(scanner.getPosition()) || scanner.isAirborne()) {
            return false;
        }
        int elevation = scanner.getElevation();
        BasementType basement = building.getBasement(scanner.getPosition());
        int depth = basement == null ? 0 : basement.getDepth();
        return elevation >= -depth && elevation <= building.getHeight(scanner.getPosition());
    }

    /** Distance for a scan order, including one made from inside a multi-hex building. */
    public static int scanDistance(Entity scanner, Targetable target) {
        return occupiesTargetBuilding(scanner, target) ? 0 : scanner.getPosition().distance(target.getPosition());
    }
}
