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

/**
 * One critical hit assigned by the pre-existing damage rules (First Succession War, p.144). Each implementation targets
 * one control of the unit editor dialog; the dialog translates assignments into checked crit boxes.
 *
 * <p>These records are transient dialog data and are never serialized or stored in game state, so they need no
 * {@code SerializationHelper} converter.</p>
 */
public sealed interface CritAssignment {

    /**
     * A critical hit on a piece of equipment (weapon, heat sink, other hittable equipment), identified by its equipment
     * number on the unit. Used by all unit types.
     */
    record EquipmentCrit(int equipmentNumber) implements CritAssignment {}

    /**
     * A critical hit on a Mek system slot: engine, gyro, sensors, life support, actuators, LAM avionics/landing gear,
     * or QuadVee conversion gear. The system value is the {@code CriticalSlot} index constant of the owning class (Mek,
     * LandAirMek, or QuadVee).
     */
    record MekSystemCrit(int system, int location) implements CritAssignment {}

    /**
     * A critical hit on a combat vehicle system. The location is only meaningful for {@link VehicleCritKind#STABILIZER}
     * (a VTOL rotor stabilizer hit uses the rotor location).
     */
    record VehicleCrit(VehicleCritKind kind, int location) implements CritAssignment {}

    /** A critical hit on an aerospace or conventional fighter system. */
    record AeroFighterCrit(AeroFighterCritKind kind) implements CritAssignment {}

    /** Combat vehicle critical hit results that the unit editor dialog can represent. */
    enum VehicleCritKind {
        TURRET_LOCK,
        STABILIZER,
        SENSORS,
        MOTIVE
    }

    /** Fighter critical hit results that the unit editor dialog can represent. */
    enum AeroFighterCritKind {
        AVIONICS,
        FIRE_CONTROL_SYSTEM,
        SENSORS,
        ENGINE,
        LANDING_GEAR
    }
}
