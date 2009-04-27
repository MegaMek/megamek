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

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.List;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import megamek.client.Client;
import megamek.client.ui.MechView;
import megamek.client.ui.Messages;
import megamek.common.Entity;

/**
 * This class displays a window that displays the forces currently selected in
 * the lounge.
 * 
 * @author Ryan McConnell (oscarmm)
 * @version $Revision$
 * @since 0.31
 */
public class MechGroupView extends Dialog implements ActionListener,
        ItemListener {

    /**
     * 
     */
    private static final long serialVersionUID = 85778571937208805L;
    List entities = new List(20);
    Button closeButton = new Button(Messages.getString("Close"));
    TextArea ta = new TextArea();

    Client client;
    int[] entityArray;

    MechGroupView(Frame frame, Client c, int[] eA) {
        super(frame, Messages.getString("MechGroupView.title"));
        client = c;
        entityArray = eA;

        boolean rpgSkills = client.game.getOptions().booleanOption(
                "rpg_gunnery");

        for (int i = 0; i < entityArray.length; i++) {
            Entity entity = client.game.getEntity(entityArray[i]);
            // Handle the "Blind Drop" option.
            if (entity == null)
                continue;
            if (!entity.getOwner().equals(client.getLocalPlayer())
                    && client.game.getOptions().booleanOption("blind_drop")
                    && !client.game.getOptions().booleanOption(
                            "real_blind_drop")) {
                entities.add(ChatLounge.formatUnit(entity, true, rpgSkills));
            } else if (entity.getOwner().equals(client.getLocalPlayer())
                    || (!client.game.getOptions().booleanOption("blind_drop") && !client.game
                            .getOptions().booleanOption("real_blind_drop"))) {
                entities.add(ChatLounge.formatUnit(entity, false, rpgSkills));
            }
        }
        this.setLayout(new BorderLayout());
        this.add(entities, BorderLayout.WEST);
        this.add(closeButton, BorderLayout.SOUTH);

        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.add(ta, BorderLayout.CENTER);

        entities.addItemListener(this);
        closeButton.addActionListener(this);

        this.setSize(550, 600);
        this.setLocation(frame.getLocation().x + frame.getSize().width / 2
                - this.getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 10);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                e.getWindow().setVisible(false);
            }
        });
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == closeButton) {
            this.setVisible(false);
        }
    }

    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource() == entities) {
            int selected = entities.getSelectedIndex();
            if (selected == -1) {
                ta.setText("");
                return;
            } else if (!client.game.getEntity(entityArray[selected]).getOwner()
                    .equals(client.getLocalPlayer())) {
                ta.setText("(enemy unit)");
            } else {
                Entity entity = client.game.getEntity(entityArray[selected]);
                MechView mechView = new MechView(entity, client.game.getOptions().booleanOption("show_bay_detail"));
                ta.setText(mechView.getMechReadout());
            }
        }
    }

}
