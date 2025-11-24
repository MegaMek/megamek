/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay.lobby;

import java.text.MessageFormat;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import megamek.client.ui.Messages;

/** Contains static methods that show common info/error messages for the lobby. */
public final class LobbyErrors {

    private static final String SINGLE_OWNER = "For this action, the selected units must have a single owner.";
    private static final String CONFIG_ENEMY = "Cannot configure units of other players except units of your bots.";
    private static final String VIEW_HIDDEN = "Cannot view or set details on hidden units.";
    private static final String SINGLE_UNIT = "Cannot {0} for more than one unit at a time.";
    private static final String SINGLE_UNIT_OR_FORCE = "Please select a single unit or a single force.";
    private static final String TEN_UNITS = "Please select fewer than 10 units.";
    private static final String HEAT_TRACKING = "Cannot apply a heat setting to units that do not track heat.";
    private static final String ONLY_MEKS = "This setting can only be applied to Meks.";
    private static final String ONLY_C3M = "Only units with a C3M can be set to be Company Masters.";
    private static final String SAME_C3 = "The C3 systems of the selected units don't match. Select only the same type of C3 units.";
    private static final String EXCEED_C3_CAPACITY = "Connecting the selected units exceed this C3 system's capacity.";
    private static final String LOAD_ONLY_ALLIED = "Can only load units that are allied with each other.";
    private static final String ONLY_FIGHTERS = "Only aerospace and conventional fighters can join squadrons.";
    private static final String NO_BAY = "The unit does not have that bay.";
    private static final String ONLY_OWN_BOT = "Can only remove bots that were added in this lobby.";
    private static final String NO_DUAL_LOAD = "It is not possible to re-load two units to a new transport where one unit currently carries the other. Please unload the units first.";
    private static final String ONLY_TEAM = "Combinations like loading, C3 connections and shared forces are only valid within a team.";
    private static final String ENTITY_OR_FORCE = "Please select either only forces or only units.";
    private static final String FORCE_EMPTY = "Please select only empty forces.";
    private static final String FORCE_ASSIGN_ONLY_TEAM = "Can only reassign a force to a teammate when reassigning without units.";
    private static final String FORCE_ATTACH_TO_SUB_FORCE = "Cannot attach a force to its own sub force.";
    private static final String PLAYER_DONE = "Cannot edit units while your status is Done.";
    private static final String SBF_CONVERSION_ERROR = "At least some of the forces you selected cannot be " +
          "converted to SBF Formations. Please select only the topmost forces to be converted, no subForces. " +
          "A converted force must conform to the rules given in Interstellar Operations. Conversion " +
          "will typically work with companies created in the Force Generator.";
    private static final String NO_DUAL_TOW = "Both units must have an open appropriate tow hitch.";

    public static void showOnlyOwnBot(JFrame owner) {
        JOptionPane.showMessageDialog(owner, ONLY_OWN_BOT);
    }

    public static void showOnlySingleEntityOrForce(JFrame owner) {
        JOptionPane.showMessageDialog(owner, SINGLE_UNIT_OR_FORCE);
    }

    public static void showSingleOwnerRequired(JFrame owner) {
        JOptionPane.showMessageDialog(owner, SINGLE_OWNER);
    }

    public static void showForceNoAttachSubForce(JFrame owner) {
        JOptionPane.showMessageDialog(owner, FORCE_ATTACH_TO_SUB_FORCE);
    }

    public static void showOnlyTeam(JFrame owner) {
        JOptionPane.showMessageDialog(owner, ONLY_TEAM);
    }

    public static void showOnlyC3M(JFrame owner) {
        JOptionPane.showMessageDialog(owner, ONLY_C3M);
    }

    public static void showNoDualLoad(JFrame owner) {
        JOptionPane.showMessageDialog(owner, NO_DUAL_LOAD);
    }

    public static void showNoDualTow(JFrame owner) {
        JOptionPane.showMessageDialog(owner, NO_DUAL_TOW);
    }

    public static void showNoSuchBay(JFrame owner) {
        JOptionPane.showMessageDialog(owner, NO_BAY);
    }

    public static void showSquadronTooMany(JFrame owner) {
        JOptionPane.showMessageDialog(owner, Messages.getString("FighterSquadron.toomany"));
    }

    public static void showOnlyFighter(JFrame owner) {
        JOptionPane.showMessageDialog(owner, ONLY_FIGHTERS);
    }

    public static void showLoadOnlyAllied(JFrame owner) {
        JOptionPane.showMessageDialog(owner, LOAD_ONLY_ALLIED);
    }

    public static void showExceedC3Capacity(JFrame owner) {
        JOptionPane.showMessageDialog(owner, EXCEED_C3_CAPACITY);
    }

    public static void showSameC3(JFrame owner) {
        JOptionPane.showMessageDialog(owner, SAME_C3);
    }

    public static void showCannotConfigEnemies(JFrame owner) {
        JOptionPane.showMessageDialog(owner, CONFIG_ENEMY);
    }

    public static void showCannotViewHidden(JFrame owner) {
        JOptionPane.showMessageDialog(owner, VIEW_HIDDEN);
    }

    public static void showSingleUnit(JFrame owner, String action) {
        JOptionPane.showMessageDialog(owner, MessageFormat.format(SINGLE_UNIT, action));
    }

    public static void showTenUnits(JFrame owner) {
        JOptionPane.showMessageDialog(owner, TEN_UNITS);
    }

    public static void showHeatTracking(JFrame owner) {
        JOptionPane.showMessageDialog(owner, HEAT_TRACKING);
    }

    public static void showOnlyMeks(JFrame owner) {
        JOptionPane.showMessageDialog(owner, ONLY_MEKS);
    }

    public static void showOnlyTeammate(JFrame owner) {
        JOptionPane.showMessageDialog(owner, FORCE_ASSIGN_ONLY_TEAM);
    }

    public static void showOnlyEntityOrForce(JFrame owner) {
        JOptionPane.showMessageDialog(owner, ENTITY_OR_FORCE);
    }

    public static void showOnlyEmptyForce(JFrame owner) {
        JOptionPane.showMessageDialog(owner, FORCE_EMPTY);
    }

    public static void showSBFConversion(JFrame owner) {
        JOptionPane.showMessageDialog(owner, SBF_CONVERSION_ERROR);
    }

    public static void showCannotEditWhileDone(JFrame owner) {
        JOptionPane.showMessageDialog(owner, PLAYER_DONE);
    }

    public static void showCannotDisconnectMasterUnit(JFrame owner) {
        JOptionPane.showMessageDialog(owner, Messages.getString("LobbyErrors.cannotDisconnectMaster"));
    }

    private LobbyErrors() {}
}
