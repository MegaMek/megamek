/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

/*
 * PlanetaryConditionsDialog.java
 */

package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.common.PlanetaryConditions;

/**
 * A dialog that allows for customization of planetary conditions
 * 
 * @author Jay Lawson
 * @version
 */
public class PlanetaryConditionsDialog extends JDialog implements
        ActionListener {

    private static final long serialVersionUID = -4426594323169113468L;

    private ClientGUI client;
    private PlanetaryConditions conditions;
    private JLabel labLight = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labLight"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox choLight = new JComboBox();
    private JLabel labWeather = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labWeather"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox choWeather = new JComboBox();
    private JLabel labWind = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labWind"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox choWind = new JComboBox();
    private JLabel labAtmosphere = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labAtmosphere"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox choFog = new JComboBox();
    private JLabel labFog = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labFog"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox choAtmosphere = new JComboBox();
    private JCheckBox cBlowingSands = new JCheckBox(Messages
            .getString("PlanetaryConditionsDialog.BlowingSands"));
    private JCheckBox cShiftWindDir = new JCheckBox(Messages
            .getString("PlanetaryConditionsDialog.shiftWindDir"));
    private JCheckBox cShiftWindStr = new JCheckBox(Messages
            .getString("PlanetaryConditionsDialog.shiftWindStr"));
    private JTextField fldTemp = new JTextField(4);
    private JLabel labTemp = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labTemp"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JTextField fldGrav = new JTextField(4);
    private JLabel labGrav = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labGrav"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JCheckBox cEMI = new JCheckBox(Messages
            .getString("PlanetaryConditionsDialog.EMI"));
    private JCheckBox cTerrainAffected = new JCheckBox(Messages
            .getString("PlanetaryConditionsDialog.TerrainAffected"));

    private JPanel panButtons = new JPanel();
    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

    private JPanel panOptions = new JPanel();

    /**
     * Initialize this dialog.
     * 
     * @param frame
     *            - the <code>Frame</code> parent of this dialog.
     * @param options
     *            - the <code>GameOptions</code> to be displayed.
     */
    private void init(JFrame frame, PlanetaryConditions planetConditions) {
        conditions = (PlanetaryConditions) planetConditions.clone();

        setupConditions();
        setupButtons();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(panOptions, c);
        add(panOptions);

        c.weightx = 1.0;
        c.weighty = 0.0;

        gridbag.setConstraints(panButtons, c);
        add(panButtons);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        setSize(getSize().width, Math.max(getSize().height, 400));
        setResizable(true);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);

    }

    /** Creates new PlanetaryConditionsDialog */
    public PlanetaryConditionsDialog(ClientGUI client) {
        super(client.frame, Messages
                .getString("PlanetaryConditionsDialog.title"), true); //$NON-NLS-1$
        this.client = client;
        init(client.frame, client.getClient().game.getPlanetaryConditions());
    }

    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);

        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);

        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }

    private void setupConditions() {

        refreshConditions();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panOptions.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labTemp, c);
        panOptions.add(labTemp);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldTemp, c);
        panOptions.add(fldTemp);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labLight, c);
        panOptions.add(labLight);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choLight, c);
        panOptions.add(choLight);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labWeather, c);
        panOptions.add(labWeather);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choWeather, c);
        panOptions.add(choWeather);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labWind, c);
        panOptions.add(labWind);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choWind, c);
        panOptions.add(choWind);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labAtmosphere, c);
        panOptions.add(labAtmosphere);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choAtmosphere, c);
        panOptions.add(choAtmosphere);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labFog, c);
        panOptions.add(labFog);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choFog, c);
        panOptions.add(choFog);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(cBlowingSands, c);
        panOptions.add(cBlowingSands);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(cShiftWindDir, c);
        panOptions.add(cShiftWindDir);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(cShiftWindStr, c);
        panOptions.add(cShiftWindStr);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labGrav, c);
        panOptions.add(labGrav);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldGrav, c);
        panOptions.add(fldGrav);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(cEMI, c);
        panOptions.add(cEMI);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(cTerrainAffected, c);
        panOptions.add(cTerrainAffected);

    }

    public void update(PlanetaryConditions planetConditions) {
        conditions = (PlanetaryConditions) planetConditions.clone();
        refreshConditions();
    }

    private void refreshConditions() {

        choLight.removeAllItems();
        for (int i = 0; i < PlanetaryConditions.L_SIZE; i++) {
            choLight.addItem(PlanetaryConditions.getLightDisplayableName(i));
        }
        choLight.setSelectedIndex(conditions.getLight());

        choWeather.removeAllItems();
        for (int i = 0; i < PlanetaryConditions.WE_SIZE; i++) {
            choWeather
                    .addItem(PlanetaryConditions.getWeatherDisplayableName(i));
        }
        choWeather.setSelectedIndex(conditions.getWeather());

        choWind.removeAllItems();
        for (int i = 0; i < PlanetaryConditions.WI_SIZE; i++) {
            choWind.addItem(PlanetaryConditions.getWindDisplayableName(i));
        }
        choWind.setSelectedIndex(conditions.getWindStrength());

        choAtmosphere.removeAllItems();
        for (int i = 0; i < PlanetaryConditions.ATMO_SIZE; i++) {
            choAtmosphere.addItem(PlanetaryConditions
                    .getAtmosphereDisplayableName(i));
        }
        choAtmosphere.setSelectedIndex(conditions.getAtmosphere());

        choFog.removeAllItems();
        for (int i = 0; i < PlanetaryConditions.FOG_SIZE; i++) {
            choFog.addItem(PlanetaryConditions.getFogDisplayableName(i));
        }
        choFog.setSelectedIndex(conditions.getFog());

        cBlowingSands.setSelected(conditions.isSandBlowing());

        cShiftWindDir.setSelected(conditions.shiftingWindDirection());
        cShiftWindStr.setSelected(conditions.shiftingWindStrength());

        fldTemp.setText(Integer.toString(conditions.getTemperature()));
        fldGrav.setText(Float.toString(conditions.getGravity()));

        cEMI.setSelected(conditions.hasEMI());

        cTerrainAffected.setSelected(conditions.isTerrainAffected());

    }

    public void send() {

        // make the changes to the planetary conditions
        conditions.setLight(choLight.getSelectedIndex());
        conditions.setWeather(choWeather.getSelectedIndex());
        conditions.setWindStrength(choWind.getSelectedIndex());
        conditions.setAtmosphere(choAtmosphere.getSelectedIndex());
        conditions.setFog(choFog.getSelectedIndex());
        conditions.setBlowingSand(cBlowingSands.isSelected());
        conditions.setShiftingWindDirection(cShiftWindDir.isSelected());
        conditions.setShiftingWindStrength(cShiftWindStr.isSelected());
        conditions.setTemperature(Integer.parseInt(fldTemp.getText()));
        conditions.setGravity(Float.parseFloat(fldGrav.getText()));
        conditions.setEMI(cEMI.isSelected());
        conditions.setTerrainAffected(cTerrainAffected.isSelected());

        client.getClient().sendPlanetaryConditions(conditions);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butOkay) {
            // check for reasonable values and some conditionals
            int temper = 25;
            float grav = (float) 1.0;
            try {
                temper = Integer.parseInt(fldTemp.getText());
            } catch (NumberFormatException er) {
                JOptionPane
                        .showMessageDialog(
                                client.frame,
                                Messages
                                        .getString("PlanetaryConditionsDialog.EnterValidTemperature"),
                                Messages
                                        .getString("PlanetaryConditionsDialog.NumberFormatError"),
                                JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                grav = Float.parseFloat(fldGrav.getText());
            } catch (NumberFormatException er) {
                JOptionPane
                        .showMessageDialog(
                                client.frame,
                                Messages
                                        .getString("PlanetaryConditionsDialog.EnterValidGravity"),
                                Messages
                                        .getString("PlanetaryConditionsDialog.NumberFormatError"),
                                JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (temper > 200 || temper < -200) {
                JOptionPane
                        .showMessageDialog(
                                client.frame,
                                Messages
                                        .getString("PlanetaryConditionsDialog.EnterValidTemperature"),
                                Messages
                                        .getString("PlanetaryConditionsDialog.NumberFormatError"),
                                JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (grav < 0.1 || grav > 10.0) {
                JOptionPane
                        .showMessageDialog(
                                client.frame,
                                Messages
                                        .getString("PlanetaryConditionsDialog.EnterValidGravity"),
                                Messages
                                        .getString("PlanetaryConditionsDialog.NumberFormatError"),
                                JOptionPane.ERROR_MESSAGE);
                return;
            }

            // can't combine certain wind conditions with certain atmospheres
            int wind = choWind.getSelectedIndex();
            int atmo = choAtmosphere.getSelectedIndex();
            if (atmo == PlanetaryConditions.ATMO_VACUUM
                    && wind > PlanetaryConditions.WI_NONE) {
                JOptionPane
                        .showMessageDialog(
                                client.frame,
                                Messages
                                        .getString("PlanetaryConditionsDialog.VacuumWind"),
                                Messages
                                        .getString("PlanetaryConditionsDialog.Incompatible"),
                                JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (atmo == PlanetaryConditions.ATMO_TRACE
                    && wind > PlanetaryConditions.WI_STORM) {
                JOptionPane
                        .showMessageDialog(
                                client.frame,
                                Messages
                                        .getString("PlanetaryConditionsDialog.TraceWind"),
                                Messages
                                        .getString("PlanetaryConditionsDialog.Incompatible"),
                                JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (atmo == PlanetaryConditions.ATMO_THIN
                    && wind > PlanetaryConditions.WI_TORNADO_F13) {
                JOptionPane
                        .showMessageDialog(
                                client.frame,
                                Messages
                                        .getString("PlanetaryConditionsDialog.ThinWind"),
                                Messages
                                        .getString("PlanetaryConditionsDialog.Incompatible"),
                                JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (client != null) {
                send();
            }
        } else if (e.getSource() == butCancel) {
            refreshConditions();
        }
        setVisible(false);
    }
}
