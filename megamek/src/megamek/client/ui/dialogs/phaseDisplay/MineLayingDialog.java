/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.phaseDisplay;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;

/**
 * A dialog displayed to the player when they want to lay mines with their BA unit.
 */
public class MineLayingDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = -1067865530113792340L;
    private final JButton butCancel = new JButton(Messages.getString("Cancel"));
    private boolean okay = true;

    /**
     * The <code>int</code> ID of the entity that lays the mine.
     */
    private final Entity entity;
    private final JComboBox<String> chMines = new JComboBox<>();
    private final ArrayList<Mounted<?>> vMines = new ArrayList<>();

    /**
     * Display a dialog that shows the mines on the entity, and allows the player to choose one.
     *
     * @param parent the <code>Frame</code> parent of this dialog
     * @param entity the <code>Entity</code> that carries the mines.
     */
    public MineLayingDialog(JFrame parent, Entity entity) {
        super(parent, Messages.getString("MineLayingDialog.title"), true);
        this.entity = entity;

        JLabel labMessage = new JLabel(Messages.getString("MineLayingDialog.selectMineToLay",
              entity.getDisplayName()));

        // Walk through the entity's misc equipment, looking for mines.
        for (Mounted<?> mount : entity.getMisc()) {

            // Is this a Mine that can be laid?
            EquipmentType type = mount.getType();
            if ((type.hasFlag(MiscType.F_MINE) ||
                  type.hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) &&
                  mount.canFire()) {
                String message = entity.getLocationName(mount.getLocation())
                      + ' '
                      + mount.getDesc();
                chMines.addItem(message);
                vMines.add(mount);

            } // End found-mine

        } // Look at the next piece of equipment.

        // buttons
        JButton butOkay = new JButton(Messages.getString("Okay"));
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        // layout
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridBagLayout);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 10, 10, 10);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBagLayout.setConstraints(labMessage, c);
        getContentPane().add(labMessage);

        gridBagLayout.setConstraints(chMines, c);
        getContentPane().add(chMines);

        // Allow the player to confirm or abort the choice.
        getContentPane().add(butOkay);
        getContentPane().add(butCancel);
        butOkay.requestFocus();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        Dimension size = getSize();
        if (size.width < GUIPreferences.getInstance().getMinimumSizeWidth()) {
            size.width = GUIPreferences.getInstance().getMinimumSizeWidth();
        }
        if (size.height < GUIPreferences.getInstance().getMinimumSizeHeight()) {
            size.height = GUIPreferences.getInstance().getMinimumSizeHeight();
        }

        setResizable(false);
        setLocation(parent.getLocation().x + parent.getSize().width / 2
                    - size.width / 2,
              parent.getLocation().y
                    + parent.getSize().height / 2 - size.height / 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butCancel)) {
            okay = false;
        }
        setVisible(false);
    }

    public boolean getAnswer() {
        return okay;
    }

    /**
     * Get the id of the mine the player wants to use.
     *
     * @return the <code>int</code> id of the mine to lay
     */
    public int getMine() {
        Mounted<?> mine = vMines.get(chMines.getSelectedIndex());
        return entity.getEquipmentNum(mine);
    }
}
