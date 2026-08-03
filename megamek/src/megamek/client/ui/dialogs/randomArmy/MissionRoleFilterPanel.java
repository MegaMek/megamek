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
package megamek.client.ui.dialogs.randomArmy;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.units.UnitType;

/**
 * The mission-role filters that qualify what the Force Generator produces.
 *
 * <p>Three sets of filters share this panel - ground, infantry and aerospace - and only the set matching the
 * selected unit type is ever shown. They are filters rather than requests: ticking one restricts the unit table to
 * models that can fill that role.</p>
 *
 * <p>Only the roles that genuinely filter the table are offered. Recon, Fire Support, Urban, Cavalry, Raider,
 * Incendiary, Anti-Aircraft, Anti-Infantry and Infantry Support merely nudged availability weights, which on a table
 * of a few dozen entries was close to noise, and the battlefield shape they were reached for is what the formation
 * mix expresses directly.</p>
 */
public class MissionRoleFilterPanel extends JPanel {

    /**
     * A filter checkbox and the mission role it contributes when it is ticked.
     *
     * @param role     the role added to the force description
     * @param checkBox the control the player ticks
     */
    private record RoleFilter(MissionRole role, JCheckBox checkBox) {}

    private final JPanel groundPanel = createGroupPanel();
    private final JPanel infantryPanel = createGroupPanel();
    private final JPanel airPanel = createGroupPanel();

    private final List<RoleFilter> groundFilters = new ArrayList<>();
    private final List<RoleFilter> infantryFilters = new ArrayList<>();
    private final List<RoleFilter> airFilters = new ArrayList<>();

    /** Builds the three filter groups, with only the ground group visible. */
    public MissionRoleFilterPanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        addFilter(groundPanel, groundFilters, MissionRole.ARTILLERY, 0, 0);
        addFilter(groundPanel, groundFilters, MissionRole.MISSILE_ARTILLERY, 1, 0);
        addFilter(groundPanel, groundFilters, MissionRole.CARGO, 2, 0);
        addFilter(groundPanel, groundFilters, MissionRole.ENGINEER, 3, 0);

        addFilter(infantryPanel, infantryFilters, MissionRole.FIELD_GUN, 0, 0);
        addFilter(infantryPanel, infantryFilters, MissionRole.ARTILLERY, 1, 0);
        addFilter(infantryPanel, infantryFilters, MissionRole.MISSILE_ARTILLERY, 2, 0);
        addFilter(infantryPanel, infantryFilters, MissionRole.ENGINEER, 0, 1);
        addFilter(infantryPanel, infantryFilters, MissionRole.FIELDWORKS, 1, 1);
        addFilter(infantryPanel, infantryFilters, MissionRole.BRIDGE_LAYER, 2, 1);
        addFilter(infantryPanel, infantryFilters, MissionRole.DEMOLITION, 0, 2);
        addFilter(infantryPanel, infantryFilters, MissionRole.FIREFIGHTER, 1, 2);

        addFilter(airPanel, airFilters, MissionRole.RECON, 0, 0);
        addFilter(airPanel, airFilters, MissionRole.GROUND_SUPPORT, 1, 0);
        addFilter(airPanel, airFilters, MissionRole.INTERCEPTOR, 2, 0);
        // Escort and Bomber are shown but deliberately left out of the applied list, because that is what the code
        // this panel replaces did - it created them as locals and never read them back. Adding them to the list
        // would change which units the generator draws, which does not belong in an extraction.
        addFilter(airPanel, new ArrayList<>(), MissionRole.ESCORT, 0, 1);
        addFilter(airPanel, new ArrayList<>(), MissionRole.BOMBER, 1, 1);
        addFilter(airPanel, airFilters, MissionRole.ASSAULT, 0, 2);
        addFilter(airPanel, airFilters, MissionRole.CARGO, 1, 2);

        // Only one group is ever visible, so they stack in the same cell and the parent's insets apply once.
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridx = 0;
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.gridy = 0;
        add(groundPanel, constraints);
        constraints.gridy = 1;
        add(infantryPanel, constraints);
        constraints.gridy = 2;
        add(airPanel, constraints);

        infantryPanel.setVisible(false);
        airPanel.setVisible(false);
    }

    private static JPanel createGroupPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("ForceGeneratorDialog.missionRoles.title")));
        return panel;
    }

    /**
     * Creates one filter checkbox, places it in its group and registers it as applied.
     *
     * @param group   the group panel to place it in
     * @param applied the list of filters read back into the force description
     * @param role    the role this filter selects for
     * @param column  the grid column within the group
     * @param row     the grid row within the group
     */
    private static void addFilter(JPanel group, List<RoleFilter> applied, MissionRole role, int column, int row) {
        String messageKey = "MissionRole." + role.toString().toLowerCase();
        JCheckBox checkBox = new JCheckBox(Messages.getString(messageKey));
        checkBox.setToolTipText(Messages.getString(messageKey + ".tooltip"));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.gridx = column;
        constraints.gridy = row;
        group.add(checkBox, constraints);
        applied.add(new RoleFilter(role, checkBox));
    }

    /**
     * Shows the filter group that belongs to the selected unit type, and hides the others.
     *
     * @param unitType the selected unit type, or {@code null} for a combined-arms force
     */
    public void showFor(@Nullable Integer unitType) {
        boolean isGround = (unitType != null) && ((unitType == UnitType.MEK) || (unitType == UnitType.TANK));
        boolean isInfantry = (unitType != null)
              && ((unitType == UnitType.INFANTRY) || (unitType == UnitType.BATTLE_ARMOR));
        boolean isAerospace = (unitType != null)
              && ((unitType == UnitType.AEROSPACE_FIGHTER) || (unitType == UnitType.CONV_FIGHTER));
        groundPanel.setVisible(isGround);
        infantryPanel.setVisible(isInfantry);
        airPanel.setVisible(isAerospace);
    }

    /**
     * Adds the ticked filters for the given unit type to the force description.
     *
     * <p>Only the group matching the unit type contributes, so filters left ticked on a group the player has since
     * navigated away from do not leak into an unrelated force.</p>
     *
     * @param forceDescriptor the description to add the roles to
     * @param unitType        the selected unit type, or {@code null} to add nothing
     */
    public void applyTo(ForceDescriptor forceDescriptor, @Nullable Integer unitType) {
        if (unitType == null) {
            return;
        }
        List<RoleFilter> applicable = switch (unitType) {
            case UnitType.MEK, UnitType.TANK -> groundFilters;
            case UnitType.INFANTRY, UnitType.BATTLE_ARMOR -> infantryFilters;
            case UnitType.AERO, UnitType.AEROSPACE_FIGHTER -> airFilters;
            default -> List.of();
        };
        for (RoleFilter filter : applicable) {
            if (filter.checkBox().isSelected()) {
                forceDescriptor.getRoles().add(filter.role());
            }
        }
    }

    /** Clears every filter in every group, so a reset leaves nothing ticked behind a hidden panel. */
    public void clearSelections() {
        for (JPanel group : List.of(groundPanel, infantryPanel, airPanel)) {
            for (Component child : group.getComponents()) {
                if (child instanceof JCheckBox checkBox) {
                    checkBox.setSelected(false);
                }
            }
        }
    }
}
