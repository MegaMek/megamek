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
package megamek.common.units;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The damage editor's edits as plain values, detached from the dialog that collected them. The dialog builds one of
 * these from its controls and either applies it to its local unit (in the lobby, or outside a game as in MekHQ) or
 * sends it to the server, which applies it to its own authoritative copy of the unit. Sending the values instead of
 * the edited unit keeps the server's copy authoritative: a full unit sent back from a client carries whatever stale
 * state the client held, which has to be guarded against field by field.
 * <p>
 * A field is {@code null} when the dialog had nothing to edit for it, mirroring the dialog's controls: a unit
 * without rear armor has no rear armor values, and only a gamemaster's in-game editor has skill modifier values.
 * The maps are always present and empty when there is nothing in them.
 * </p>
 * <p>
 * This class only travels inside packets and is never written to a save, so it stays a plain {@link Serializable}
 * class with no XStream concerns.
 * </p>
 */
public class DamageEditSpec implements Serializable {

    private static final long serialVersionUID = 5218757312851293668L;

    /** Where a QuadVee's conversion gear sits in its leg's actuator values, after the hip to foot actuators. */
    public static final int CONVERSION_GEAR_INDEX = Mek.ACTUATOR_FOOT - Mek.ACTUATOR_HIP + 1;

    /** The unit these edits apply to. */
    public int entityId;

    /* per-location structure and armor; a null element means that location was not edited */
    public Integer[] internal;
    public Integer[] armor;
    public Integer[] rearArmor;

    /** Hits taken by each crew member; the entry of a missing crew member stays null. */
    public Integer[] crewHits;
    /** The unit's current heat, for the unit types that track it. */
    public Integer heat;

    /* the unit's conditions: what state it is in, rather than how it was built */
    public Boolean shutdown;
    public Boolean prone;
    public Boolean hullDown;
    public Boolean hidden;
    public Boolean stealth;
    public Boolean dugIn;
    /** The fuel left in an aero. */
    public Integer fuel;

    /** The shots left in each ammo bin, by its equipment number. Only a gamemaster edits these. */
    public final Map<Integer, Integer> ammoShots = new HashMap<>();

    /** The crit hits on each piece of equipment, by its equipment number. */
    public final Map<Integer, Integer> equipmentHits = new HashMap<>();

    /** Burst fire on each machine gun, by its equipment number; only carried by a gamemaster's in-game edit. */
    public final Map<Integer, Boolean> mgBurst = new HashMap<>();
    /** Hot-loading on each ammo bin, by its equipment number; only carried by a gamemaster's in-game edit. */
    public final Map<Integer, Boolean> hotLoadedAmmo = new HashMap<>();

    /*
     * the gamemaster's temporary skill modifiers, each with a duration of its own (rounds, ignored while its
     * permanent flag is on). Gunnery and piloting travel together; the initiative trio is present only where the
     * editor offered its row, which is a game using individual initiative.
     */
    public Integer gunneryModifier;
    public Integer gunneryRounds;
    public boolean gunneryPermanent;
    public Integer pilotingModifier;
    public Integer pilotingRounds;
    public boolean pilotingPermanent;
    public Integer initiativeModifier;
    public Integer initiativeRounds;
    public boolean initiativePermanent;

    /* Mek system crit hits */
    public Integer centerEngineHits;
    public Integer leftEngineHits;
    public Integer rightEngineHits;
    public Integer gyroHits;
    public Integer sensorHits;
    public Integer lifeSupportHits;
    public Integer cockpitHits;
    /** A land-air Mek's avionics crit hits, by location. */
    public final Map<Integer, Integer> lamAvionicsHits = new HashMap<>();
    /** A land-air Mek's landing gear crit hits, by location. */
    public final Map<Integer, Integer> lamLandingGearHits = new HashMap<>();
    /**
     * Actuator crit hits by limb and actuator, in the damage editor's layout: the first index counts limbs from
     * {@link Mek#LOC_RIGHT_ARM}, the second counts actuators from the shoulder (arms) or hip (legs and quads),
     * with a QuadVee's conversion gear after the foot.
     */
    public Integer[][] actuatorHits;

    /* Tank system crit hits; the engine and sensor fields above are shared the way the dialog's controls are */
    public Integer engineHits;
    public Integer turretLockHits;
    public Integer motiveHits;
    /** Per-location stabilizer crit hits; a null element means that location has no stabilizer to edit. */
    public Integer[] stabilizerHits;
    /** A VTOL's flight stabilizer crit hits. */
    public Integer flightStabilizerHits;

    /* Aero system crit hits */
    public Integer avionicsHits;
    public Integer fcsHits;
    public Integer cicHits;
    public Integer gearHits;
    public Integer leftThrusterHits;
    public Integer rightThrusterHits;
    public Integer kfBoomHits;
    public Integer dockCollarHits;
    public Integer gravDeckHits;
    /** The undamaged capacity left in each transport bay, in the order the unit lists its bays. */
    public Double[] bayCapacityRemaining;
    /** Crit hits on each transport bay's doors, in the same order as {@link #bayCapacityRemaining}. */
    public Integer[] bayDoorHits;
    /** How many of a Jumpship's docking collars are still working. */
    public Integer workingDockingCollars;
    /** A Jumpship's K-F drive integrity. */
    public Integer kfIntegrity;
    public Integer chargingSystemHits;
    public Integer driveCoilHits;
    public Integer driveControllerHits;
    public Integer fieldInitiatorHits;
    public Integer heliumTankHits;
    public Integer lfBatteryHits;
    /** A Jumpship's jump sail integrity. */
    public Integer sailIntegrity;

    /** A ProtoMek's per-location system crit hits. */
    public Integer[] protoHits;
}
