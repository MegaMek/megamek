/*
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitDisplay;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.Enumeration;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToggleButton;

import megamek.client.ui.Messages;
import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.mapset.PilotMapSet;
import megamek.client.ui.widget.picmap.PicMap;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;

/**
 * The pilot panel contains all the information about the pilot/crew of this unit.
 */
class PilotPanel extends PicMap {
    @Serial
    private static final long serialVersionUID = 8284603003897415518L;

    private final PilotMapSet pilotMapSet;

    private final int minTopMargin = 8;
    private final int minLeftMargin = 8;
    private final JComboBox<String> cbCrewSlot = new JComboBox<>();
    private final JToggleButton btnSwapRoles = new JToggleButton();

    // We need to hold onto the entity in case the crew slot changes.
    private Entity entity;

    PilotPanel(final UnitDisplayPanel unitDisplayPanel) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(minTopMargin, minLeftMargin, minTopMargin, minLeftMargin);
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        add(cbCrewSlot, gbc);
        cbCrewSlot.addActionListener(e -> selectCrewSlot());

        btnSwapRoles.setToolTipText(Messages.getString("PilotMapSet.swapRoles.toolTip"));
        gbc.gridy = 1;
        add(btnSwapRoles, gbc);
        btnSwapRoles.addActionListener(e -> {
            if (null != entity) {
                entity.getCrew().setSwapConsoleRoles(btnSwapRoles.isSelected());
                unitDisplayPanel.getClientGUI().getClient().sendUpdateEntity(entity);
                updateSwapButtonText();
            }
        });

        // Hack to keep controls at the top of the screen when the bottom one is not
        // always visible.
        // There is probably a better way to do this.
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        add(new JLabel(), gbc);

        pilotMapSet = new PilotMapSet(this);
        addElement(pilotMapSet.getContentGroup());
        Enumeration<BackGroundDrawer> iter = pilotMapSet.getBackgroundDrawers().elements();
        while (iter.hasMoreElements()) {
            addBgDrawer(iter.nextElement());
        }
        onResize();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        update();
    }

    @Override
    public void onResize() {
        int dx = minLeftMargin;
        int dy = minTopMargin;
        setContentMargins(dx, dy, dx, dy);
    }

    /**
     * updates fields for the specified mek
     */
    public void displayMek(Entity en) {
        entity = en;
        pilotMapSet.setEntity(en);
        if (en.getCrew().getSlotCount() > 1) {
            cbCrewSlot.removeAllItems();
            for (int i = 0; i < en.getCrew().getSlotCount(); i++) {
                cbCrewSlot.addItem(en.getCrew().getCrewType().getRoleName(i));
            }
            cbCrewSlot.setVisible(true);
        } else {
            cbCrewSlot.setVisible(false);
        }
        if (entity.getCrew().getCrewType().equals(CrewType.COMMAND_CONSOLE)) {
            btnSwapRoles.setSelected(entity.getCrew().getSwapConsoleRoles());
            btnSwapRoles.setEnabled(entity.getCrew().isActive(0) && entity.getCrew().isActive(1));
            btnSwapRoles.setVisible(true);
            updateSwapButtonText();
        } else {
            btnSwapRoles.setVisible(false);
        }

        onResize();
        update();
    }

    private void selectCrewSlot() {
        if (null != entity && cbCrewSlot.getSelectedIndex() >= 0) {
            pilotMapSet.setEntity(entity, cbCrewSlot.getSelectedIndex());
            onResize();
            update();
        }
    }

    private void updateSwapButtonText() {
        if (btnSwapRoles.isSelected()) {
            btnSwapRoles.setText(Messages.getString("PilotMapSet.keepRoles.text"));
        } else {
            btnSwapRoles.setText(Messages.getString("PilotMapSet.swapRoles.text"));
        }
    }
}
