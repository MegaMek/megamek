/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005
 * Ben Mazur (bmazur@sev.org)
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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.MechView;
import megamek.common.options.OptionsConstants;

/**
 * This class displays a window that displays the forces currently selected in
 * the lounge.
 *
 * @author Ryan McConnell (oscarmm)
 * @version $Revision$
 * @since 0.31
 */
public class MechGroupView extends JDialog implements ActionListener, ListSelectionListener {

    /**
     *
     */
    private static final long serialVersionUID = -6128402142715924422L;
    private JList<String> entities = new JList<String>();
    private JButton closeButton = new JButton(Messages.getString("Close"));
    private JTextArea ta = new JTextArea();

    private Client client;
    private int[] entityArray;

    MechGroupView(JFrame frame, Client c, int[] eA) {
        super(frame, Messages.getString("MechGroupView.title"));
        client = c;
        entityArray = eA;
        String[] entityStrings = new String[entityArray.length];
        int index = 0;

        boolean rpgSkills = client.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY);

        for (final int newVar : entityArray) {
            Entity entity = client.getGame().getEntity(newVar);
            // Handle the "Blind Drop" option.
            if (entity == null) {
                continue;
            }
            if (!entity.getOwner().equals(client.getLocalPlayer())
                    && client.getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP)
                    && !client.getGame().getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP)) {
                entityStrings[index++] = ChatLounge.formatUnit(entity, true, rpgSkills);
            } else if (entity.getOwner().equals(client.getLocalPlayer())
                    || (!client.getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP)
                            && !client.getGame().getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP))) {
                entityStrings[index++] = ChatLounge.formatUnit(entity, false, rpgSkills);
            }
        }
        entities = new JList<String>(entityStrings);
        entities.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(entities, BorderLayout.WEST);
        getContentPane().add(closeButton, BorderLayout.SOUTH);

        ta.setEditable(false);
        ta.setOpaque(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        getContentPane().add(ta, BorderLayout.CENTER);

        entities.addListSelectionListener(this);
        closeButton.addActionListener(this);

        setSize(550, 600);
        setLocation((frame.getLocation().x + (frame.getSize().width / 2)) - (getSize().width / 2),
                frame.getLocation().y + (frame.getSize().height / 10));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                e.getWindow().setVisible(false);
            }
        });
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(closeButton)) {
            setVisible(false);
        }
    }

    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (event.getSource().equals(entities)) {
            int selected = entities.getSelectedIndex();
            if (selected == -1) {
                ta.setText("");
                return;
            } else if (!client.getGame().getEntity(entityArray[selected]).getOwner().equals(client.getLocalPlayer())) {
                ta.setText("(enemy unit)");
            } else {
                Entity entity = client.getGame().getEntity(entityArray[selected]);
                MechView mechView = new MechView(entity,
                        client.getGame().getOptions().booleanOption(OptionsConstants.BASE_SHOW_BAY_DETAIL));
                ta.setText(mechView.getMechReadout());
            }
        }
    }
}
