/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.client.ui.swing;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.MiscType;
import megamek.common.Mounted;

/**
 * A dialog displayed to the player when they want to lay mines with their BA
 * unit.
 */
public class MineLayingDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -1067865530113792340L;
    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
    private JLabel labMessage;
    private boolean okay = true;

    /**
     * The <code>int</code> ID of the entity that lays the mine.
     */
    private Entity entity;
    private JComboBox chMines = new JComboBox();
    private ArrayList<Mounted> vMines = new ArrayList<Mounted>();

    /**
     * Display a dialog that shows the mines on the entity, and allows the
     * player to choose one.
     * 
     * @param parent the <code>Frame</code> parent of this dialog
     * @param entity the <code>Entity</code> that carries the mines.
     */
    public MineLayingDialog(JFrame parent, Entity entity) {
        super(parent, Messages.getString("MineLayingDialog.title"), true); //$NON-NLS-1$
        this.entity = entity;

        labMessage = new JLabel(
                Messages
                        .getString(
                                "MineLayingDialog.selectMineToLay", new Object[] { entity.getDisplayName() })); //$NON-NLS-1$

        // Walk through the entity's misc equipment, looking for mines.
        for (Mounted mount : entity.getMisc()) {

            // Is this a Mine that can be layed?
            if (mount.getType().hasFlag(MiscType.F_MINE) && mount.canFire()) {

                StringBuffer message = new StringBuffer();
                message.append(entity.getLocationName(mount.getLocation()))
                        .append(' ')//$NON-NLS-1$
                        .append(mount.getDesc());
                chMines.addItem(message.toString());
                vMines.add(mount);

            } // End found-mine

        } // Look at the next piece of equipment.

        // buttons
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 10, 10, 10);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labMessage, c);
        getContentPane().add(labMessage);

        gridbag.setConstraints(chMines, c);
        getContentPane().add(chMines);

        // Allow the player to confirm or abort the choice.
        getContentPane().add(butOkay);
        getContentPane().add(butCancel);
        butOkay.requestFocus();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        Dimension size = getSize();
        boolean updateSize = false;
        if (size.width < GUIPreferences.getInstance().getMinimumSizeWidth()) {
            size.width = GUIPreferences.getInstance().getMinimumSizeWidth();
        }
        if (size.height < GUIPreferences.getInstance().getMinimumSizeHeight()) {
            size.height = GUIPreferences.getInstance().getMinimumSizeHeight();
        }
        if (updateSize) {
            setSize(size);
            size = getSize();
        }
        setResizable(false);
        setLocation(parent.getLocation().x + parent.getSize().width / 2
                - size.width / 2, parent.getLocation().y
                + parent.getSize().height / 2 - size.height / 2);
    }

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
        Mounted mine = vMines.get(chMines.getSelectedIndex());
        return entity.getEquipmentNum(mine);
    }
}
