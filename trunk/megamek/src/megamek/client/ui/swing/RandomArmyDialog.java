/*
 * MegaMek -
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.TechConstants;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.util.RandomArmyCreator;

public class RandomArmyDialog extends JDialog implements ActionListener,
        WindowListener {

    /**
     * 
     */
    private static final long serialVersionUID = 4072453002423681675L;
    private ClientGUI m_clientgui;
    private Client m_client;
    private boolean includeMaxTech;

    private JLabel m_labelPlayer = new JLabel(Messages
            .getString("RandomArmyDialog.Player"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JComboBox m_chPlayer = new JComboBox();
    private JComboBox m_chType = new JComboBox();

    private JPanel m_pParameters = new JPanel();
    private JPanel m_pPreview = new JPanel();
    private JPanel m_pButtons = new JPanel();
    private JButton m_bOK = new JButton(Messages.getString("Okay"));
    private JButton m_bCancel = new JButton(Messages.getString("Cancel"));
    private JButton m_bRoll = new JButton(Messages
            .getString("RandomArmyDialog.Roll"));

    private JList m_lMechs = new JList();

    private JLabel m_labBV = new JLabel(Messages
            .getString("RandomArmyDialog.BV"));
    private JLabel m_labYear = new JLabel(Messages
            .getString("RandomArmyDialog.Year"));
    private JLabel m_labMechs = new JLabel(Messages
            .getString("RandomArmyDialog.Mechs"));
    private JLabel m_labVees = new JLabel(Messages
            .getString("RandomArmyDialog.Vees"));
    private JLabel m_labBA = new JLabel(Messages
            .getString("RandomArmyDialog.BA"));
    private JLabel m_labInfantry = new JLabel(Messages
            .getString("RandomArmyDialog.Infantry"));
    private JLabel m_labTech = new JLabel(Messages
            .getString("RandomArmyDialog.Tech"));

    private JTextField m_tBVmin = new JTextField(6);
    private JTextField m_tBVmax = new JTextField(6);
    private JTextField m_tMinYear = new JTextField(4);
    private JTextField m_tMaxYear = new JTextField(4);
    private JTextField m_tMechs = new JTextField(3);
    private JTextField m_tVees = new JTextField(3);
    private JTextField m_tBA = new JTextField(3);
    private JTextField m_tInfantry = new JTextField(3);
    private JCheckBox m_chkPad = new JCheckBox(Messages
            .getString("RandomArmyDialog.Pad"));
    private JCheckBox m_chkCanon = new JCheckBox(Messages
            .getString("RandomArmyDialog.Canon"));
    private ArrayList<MechSummary> army = new ArrayList<MechSummary>(0);

    public RandomArmyDialog(ClientGUI cl) {
        super(cl.frame, Messages.getString("RandomArmyDialog.title"), true); //$NON-NLS-1$
        m_clientgui = cl;
        m_client = cl.getClient();
        updatePlayerChoice();

        // set defaults
        m_tMechs.setText("4");
        m_tBVmin.setText("5800");
        m_tBVmax.setText("6000");
        m_tVees.setText("0");
        m_tBA.setText("0");
        m_tMinYear.setText("2500");
        m_tMaxYear.setText("3100");
        m_tInfantry.setText("0");
        m_chkCanon.setSelected(m_client.game.getOptions().booleanOption(
                "canon_only"));
        updateTechChoice(true);

        // construct the buttons panel
        m_pButtons.setLayout(new FlowLayout(FlowLayout.CENTER));
        m_pButtons.add(m_bOK);
        m_bOK.addActionListener(this);
        m_pButtons.add(m_bRoll);
        m_bRoll.addActionListener(this);
        m_pButtons.add(m_bCancel);
        m_bCancel.addActionListener(this);
        m_pButtons.add(m_labelPlayer);
        m_pButtons.add(m_chPlayer);

        // construct the parameters panel
        GridBagLayout layout = new GridBagLayout();
        m_pParameters.setLayout(layout);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.anchor = GridBagConstraints.LINE_START;
        layout.setConstraints(m_labTech, constraints);
        m_pParameters.add(m_labTech);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(m_chType, constraints);
        m_pParameters.add(m_chType);
        constraints.gridwidth = 1;
        layout.setConstraints(m_labBV, constraints);
        m_pParameters.add(m_labBV);
        m_pParameters.add(m_tBVmin);
        JLabel dash = new JLabel("-");
        layout.setConstraints(dash, constraints);
        m_pParameters.add(dash);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(m_tBVmax, constraints);
        m_pParameters.add(m_tBVmax);
        constraints.gridwidth = 1;
        layout.setConstraints(m_labMechs, constraints);
        m_pParameters.add(m_labMechs);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(m_tMechs, constraints);
        m_pParameters.add(m_tMechs);
        constraints.gridwidth = 1;
        layout.setConstraints(m_labVees, constraints);
        m_pParameters.add(m_labVees);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(m_tVees, constraints);
        m_pParameters.add(m_tVees);
        constraints.gridwidth = 1;
        layout.setConstraints(m_labBA, constraints);
        m_pParameters.add(m_labBA);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(m_tBA, constraints);
        m_pParameters.add(m_tBA);
        constraints.gridwidth = 1;
        layout.setConstraints(m_labInfantry, constraints);
        m_pParameters.add(m_labInfantry);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(m_tInfantry, constraints);
        m_pParameters.add(m_tInfantry);
        constraints.gridwidth = 1;
        layout.setConstraints(m_labYear, constraints);
        m_pParameters.add(m_labYear);
        layout.setConstraints(m_tMinYear, constraints);
        m_pParameters.add(m_tMinYear);
        dash = new JLabel("-");
        layout.setConstraints(dash, constraints);
        m_pParameters.add(dash);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(m_tMaxYear, constraints);
        m_pParameters.add(m_tMaxYear);
        layout.setConstraints(m_chkPad, constraints);
        m_pParameters.add(m_chkPad);
        layout.setConstraints(m_chkCanon, constraints);
        m_pParameters.add(m_chkCanon);

        // construct the preview panel
        m_pPreview.setLayout(new GridLayout(1, 1));
        JScrollPane scoll = new JScrollPane(m_lMechs);
        m_pPreview.add(scoll);

        // contruct the main dialog
        setLayout(new BorderLayout());
        add(m_pButtons, BorderLayout.SOUTH);
        add(m_pParameters, BorderLayout.WEST);
        add(m_pPreview, BorderLayout.EAST);
        validate();
        pack();
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource().equals(m_bOK)) {
            for (MechSummary ms : army) {
                try {
                    Entity e = new MechFileParser(ms.getSourceFile(), ms
                            .getEntryName()).getEntity();
                    Client c = null;
                    if (m_chPlayer.getSelectedIndex() > 0) {
                        String name = (String) m_chPlayer.getSelectedItem();
                        c = m_clientgui.getBots().get(name);
                    }
                    if (c == null) {
                        c = m_client;
                    }
                    // autoSetSkills(e);
                    e.setOwner(c.getLocalPlayer());
                    c.sendAddEntity(e);
                } catch (EntityLoadingException ex) {
                    System.out
                            .println("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    ex.printStackTrace();
                    return;
                }
            }
            setVisible(false);
        } else if (ev.getSource().equals(m_bCancel)) {
            setVisible(false);
        } else if (ev.getSource().equals(m_bRoll)) {
            try {
                RandomArmyCreator.Parameters p = new RandomArmyCreator.Parameters();
                p.mechs = Integer.parseInt(m_tMechs.getText());
                p.tanks = Integer.parseInt(m_tVees.getText());
                p.ba = Integer.parseInt(m_tBA.getText());
                p.infantry = Integer.parseInt(m_tInfantry.getText());
                p.canon = m_chkCanon.isSelected();
                p.maxBV = Integer.parseInt(m_tBVmax.getText());
                p.minBV = Integer.parseInt(m_tBVmin.getText());
                p.padWithInfantry = m_chkPad.isSelected();
                p.tech = m_chType.getSelectedIndex();
                p.minYear = Integer.parseInt(m_tMinYear.getText());
                p.maxYear = Integer.parseInt(m_tMaxYear.getText());
                army = RandomArmyCreator.generateArmy(p);
                Vector<String> mechs = new Vector<String>();
                for (MechSummary m : army) {
                    mechs.add(m.getName());
                }
                m_lMechs.setListData(mechs);
                m_lMechs.validate();
                pack();
            } catch (NumberFormatException ex) {

            }
        }
    }

    public void windowActivated(WindowEvent arg0) {
    }

    public void windowClosed(WindowEvent arg0) {
    }

    public void windowClosing(WindowEvent arg0) {
        this.setVisible(false);
    }

    public void windowDeactivated(WindowEvent arg0) {
    }

    public void windowDeiconified(WindowEvent arg0) {
    }

    public void windowIconified(WindowEvent arg0) {
    }

    public void windowOpened(WindowEvent arg0) {
    }

    private void updatePlayerChoice() {
        String lastChoice = (String) m_chPlayer.getSelectedItem();
        String clientName = m_clientgui.getClient().getName();
        m_chPlayer.removeAllItems();
        m_chPlayer.setEnabled(true);
        m_chPlayer.addItem(clientName);
        for (Iterator<Client> i = m_clientgui.getBots().values().iterator(); i
                .hasNext();) {
            m_chPlayer.addItem(i.next().getName());
        }
        if (m_chPlayer.getItemCount() == 1) {
            m_chPlayer.setEnabled(false);
        }
        m_chPlayer.setSelectedItem(lastChoice);
        if (m_chPlayer.getSelectedIndex() < 0)
            m_chPlayer.setSelectedIndex(0);
    }

    private void updateTechChoice(boolean force) {
        boolean maxTechOption = m_client.game.getOptions().booleanOption(
                "allow_advanced_units");
        int maxTech = (maxTechOption ? TechConstants.SIZE
                : TechConstants.SIZE_LEVEL_2);
        if (includeMaxTech == maxTechOption && !force) {
            return;
        }
        includeMaxTech = maxTechOption;
        m_chType.removeAll();
        for (int i = 0; i < maxTech; i++) {
            m_chType.addItem(TechConstants.getLevelDisplayableName(i));
        }
        if (maxTechOption) {
            m_chType.setSelectedItem(TechConstants.T_IS_ADVANCED);
        } else {
            m_chType.setSelectedItem(TechConstants.T_IS_TW_NON_BOX);
        }
    }

    public void setVisible(boolean show) {
        if (show) {
            updatePlayerChoice();
            updateTechChoice(false);
        }
        super.setVisible(show);
    }

}
