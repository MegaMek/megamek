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
package megamek.client.ui.dialogs.unitEditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import megamek.common.units.DamageEditSpec;

/**
 * The controls of the unit damage editor, held in one place so that the classes that build them, the classes that
 * read them back onto the unit, and the dialog that lays them out all work on the same set.
 * <p>
 * A control is {@code null} when the unit has nothing for it to edit, so every user must check before reaching for
 * one:
 * a unit without rear armor has no rear armor spinner, and a unit that does not track heat has no heat spinner.
 * </p>
 */
public class UnitDamageControls {

    /** Where a QuadVee's conversion gear sits in its leg's actuator controls, after the hip to foot actuators. */
    public static final int CONVERSION_GEAR_INDEX = DamageEditSpec.CONVERSION_GEAR_INDEX;

    /** One panel per unit location, holding that location's armor, structure, systems and equipment. */
    public JPanel[] locationPanels;
    /** Panel for systems that have no single location, such as a tank's engine or an aero's avionics. */
    public JPanel panGeneral;
    /** The next free row in each panel, used when appending label and control rows. */
    public final Map<JPanel, Integer> panelRows = new HashMap<>();

    public JSpinner[] spnInternal;
    public JSpinner[] spnArmor;
    public JSpinner[] spnRear;
    /** Hits taken by each crew member; the entry of a missing crew member stays null. */
    public JSpinner[] spnCrewHits;
    /** The unit's current heat, for the unit types that track it. */
    public JSpinner spnHeat;

    /* the unit's conditions: what state it is in, rather than how it was built */
    public JCheckBox chkShutdown;
    public JCheckBox chkProne;
    public JCheckBox chkHullDown;
    public JCheckBox chkHidden;
    public JCheckBox chkStealth;
    public JCheckBox chkDugIn;
    /** The fuel left in an aero. */
    public JSpinner spnFuel;

    /** The shots left in each ammo bin, by its equipment number. Only a gamemaster edits these. */
    public final Map<Integer, JSpinner> ammoShots = new HashMap<>();

    /** Burst fire on each machine gun, by its equipment number. Only a gamemaster's in-game editor builds these. */
    public final Map<Integer, JCheckBox> mgBurst = new HashMap<>();
    /** Hot-loading on each ammo bin allowing it, by equipment number. Only a gamemaster's in-game editor builds these. */
    public final Map<Integer, JCheckBox> hotLoadedAmmo = new HashMap<>();

    /*
     * The gamemaster's temporary skill modifiers; null when the editor is not the gamemaster's, or out of game.
     * Each modifier carries its own duration: a rounds spinner, disabled while its Permanent checkbox is on.
     */
    public JSpinner spnGunneryModifier;
    public JSpinner spnGunneryRounds;
    public JCheckBox chkGunneryPermanent;
    public JSpinner spnPilotingModifier;
    public JSpinner spnPilotingRounds;
    public JCheckBox chkPilotingPermanent;
    public JSpinner spnInitiativeModifier;
    public JSpinner spnInitiativeRounds;
    public JCheckBox chkInitiativePermanent;

    /** The crits of each piece of equipment, by its equipment number. */
    public Map<Integer, CheckCritPanel> equipCrits = new HashMap<>();

    /* system crits */
    public CheckCritPanel engineCrit;
    public CheckCritPanel leftEngineCrit;
    public CheckCritPanel rightEngineCrit;
    public CheckCritPanel centerEngineCrit;
    public CheckCritPanel gyroCrit;
    public CheckCritPanel sensorCrit;
    public CheckCritPanel lifeSupportCrit;
    public CheckCritPanel cockpitCrit;
    public Map<Integer, CheckCritPanel> lamAvionicsCrit;
    public Map<Integer, CheckCritPanel> lamLandingGearCrit;
    public CheckCritPanel[][] actuatorCrits;
    public CheckCritPanel turretLockCrit;
    public CheckCritPanel motiveCrit;
    public CheckCritPanel[] stabilizerCrits;
    public CheckCritPanel flightStabilizerCrit;
    public CheckCritPanel avionicsCrit;
    public CheckCritPanel fcsCrit;
    public CheckCritPanel cicCrit;
    public CheckCritPanel gearCrit;
    public CheckCritPanel leftThrusterCrit;
    public CheckCritPanel rightThrusterCrit;
    public CheckCritPanel kfBoomCrit;
    public CheckCritPanel dockCollarCrit;
    public CheckCritPanel gravDeckCrit;
    public JSpinner[] bayDamage;
    public CheckCritPanel[] bayDoorCrit;
    public JSpinner collarDamage;
    public JSpinner kfDamage;
    public CheckCritPanel driveCoilCrit;
    public CheckCritPanel chargingSystemCrit;
    public CheckCritPanel fieldInitiatorCrit;
    public CheckCritPanel driveControllerCrit;
    public CheckCritPanel heliumTankCrit;
    public CheckCritPanel lfBatteryCrit;
    public JSpinner sailDamage;
    public CheckCritPanel[] protoCrits;

    /** The crit controls of each location, used to shade the locations that have taken a crit. */
    public final Map<Integer, List<CheckCritPanel>> critsByLocation = new HashMap<>();

    /** Remembers that the given crit control belongs to the given location. */
    public void addCritOfLocation(int location, CheckCritPanel crit) {
        critsByLocation.computeIfAbsent(location, key -> new ArrayList<>()).add(crit);
    }

    /** The locations that currently have at least one critical hit set on them. */
    public Set<Integer> locationsWithCrits() {
        Set<Integer> locations = new HashSet<>();
        for (Map.Entry<Integer, List<CheckCritPanel>> locationCrits : critsByLocation.entrySet()) {
            for (CheckCritPanel crit : locationCrits.getValue()) {
                if (crit.getHits() > 0) {
                    locations.add(locationCrits.getKey());
                    break;
                }
            }
        }
        return locations;
    }

    /* the location name labels, colored by how damaged the location is */
    public JLabel[] locationLabels;
    public JLabel structuralIntegrityLabel;
}
