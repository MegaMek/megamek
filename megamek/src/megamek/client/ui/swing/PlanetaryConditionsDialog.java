/*
* MegaMek -
* Copyright (C) 2000, 2001, 2002, 2003, 2004 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
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
import javax.swing.JComponent;
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
    private JFrame frame;
    private PlanetaryConditions conditions;
    private int currentWeather;
    public PlanetaryConditions getConditions() {
        return conditions;
    }

    private JLabel labLight = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labLight"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox<String> choLight = new JComboBox<String>();
    private JLabel labWeather = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labWeather"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox<String> choWeather = new JComboBox<String>();
    private JLabel labWind = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labWind"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox<String> choWind = new JComboBox<String>();
    private JLabel labMinWind = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labMinWind"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox<String> choMinWind = new JComboBox<String>();
    private JLabel labMaxWind = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labMaxWind"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox<String> cboWindDirection = new JComboBox<>();
    private JLabel labWindDirection = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labWindDirection"), SwingConstants.RIGHT);
    private JComboBox<String> choMaxWind = new JComboBox<String>();
    private JLabel labAtmosphere = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labAtmosphere"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox<String> choFog = new JComboBox<String>();
    private JLabel labFog = new JLabel(
            Messages.getString("PlanetaryConditionsDialog.labFog"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox<String> choAtmosphere = new JComboBox<String>();
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
     * @param planetConditions
     */
    private void init(JFrame frame, PlanetaryConditions planetConditions) {
        conditions = (PlanetaryConditions) planetConditions.clone();
        this.frame = frame;

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

        choWeather.addActionListener(e -> {
            int weather = choWeather.getSelectedIndex();
            if (currentWeather != weather && 
                    (weather == PlanetaryConditions.WE_LIGHT_HAIL ||
                     weather == PlanetaryConditions.WE_HEAVY_HAIL ||
                     weather == PlanetaryConditions.WE_LIGHT_SNOW || 
                     weather == PlanetaryConditions.WE_SLEET ||
                     weather == PlanetaryConditions.WE_SNOW_FLURRIES ||
                     weather == PlanetaryConditions.WE_HEAVY_SNOW ||
                     weather == PlanetaryConditions.WE_ICE_STORM || 
                     weather == PlanetaryConditions.WE_BLIZZARD ||
                     weather == PlanetaryConditions.WE_MOD_SNOW)) {
                currentWeather = weather;
                conditions.setRunOnce(false);                
                updateForm();
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
        init(client.frame, client.getClient().getGame().getPlanetaryConditions());
    }

    public PlanetaryConditionsDialog(JFrame frame, PlanetaryConditions conditions) {
        super(frame, Messages
                .getString("PlanetaryConditionsDialog.title"), true); //$NON-NLS-1$
        init(frame, conditions);
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
        
        addLabelControlPair(c, labTemp, fldTemp);
        addLabelControlPair(c, labGrav, fldGrav);
        addLabelControlPair(c, labLight, choLight);
        addLabelControlPair(c, labWeather, choWeather);
        addLabelControlPair(c, labWind, choWind);
        addLabelControlPair(c, labWindDirection, cboWindDirection);
        addLabelControlPair(c, labAtmosphere, choAtmosphere);
        addLabelControlPair(c, labFog, choFog);

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
        gridbag.setConstraints(labMinWind, c);
        panOptions.add(labMinWind);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choMinWind, c);
        panOptions.add(choMinWind);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labMaxWind, c);
        panOptions.add(labMaxWind);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choMaxWind, c);
        panOptions.add(choMaxWind);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(cEMI, c);
        panOptions.add(cEMI);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(cTerrainAffected, c);
        panOptions.add(cTerrainAffected);

    }

    /**
     * Worker method that adds a label - control pair to the UI (e.g. wind - wind dropdown)
     * @param c GridBagConstraints to use
     * @param label The label to add
     * @param valueControl The textbox or dropdown to add
     */
    private void addLabelControlPair(GridBagConstraints c, JLabel label, JComponent valueControl) {
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        panOptions.add(label, c);
        label.setLabelFor(valueControl);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        panOptions.add(valueControl, c);
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
        currentWeather = conditions.getWeather();

        choWind.removeAllItems();
        choMinWind.removeAllItems();
        choMaxWind.removeAllItems();
        for (int i = 0; i < PlanetaryConditions.WI_SIZE; i++) {
            choWind.addItem(PlanetaryConditions.getWindDisplayableName(i));
            choMinWind.addItem(PlanetaryConditions.getWindDisplayableName(i));
            choMaxWind.addItem(PlanetaryConditions.getWindDisplayableName(i));
        }
        choWind.setSelectedIndex(conditions.getWindStrength());
        choMinWind.setSelectedIndex(conditions.getMinWindStrength());
        choMaxWind.setSelectedIndex(conditions.getMaxWindStrength());

        cboWindDirection.removeAllItems();
        for(int i = 0; i < PlanetaryConditions.DIR_SIZE; i++) {
            cboWindDirection.addItem(PlanetaryConditions.getWindDirDisplayableName(i));
        }
        cboWindDirection.setSelectedIndex(conditions.getWindDirection());

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

    private void setConditions() {
            setConditions(true);
    }

    private void setConditions(boolean shouldSendToServer) {
        // make the changes to the planetary conditions
        conditions.setLight(choLight.getSelectedIndex());
        conditions.setWeather(choWeather.getSelectedIndex());
        conditions.setWindStrength(choWind.getSelectedIndex());
        conditions.setWindDirection(cboWindDirection.getSelectedIndex());
        conditions.setMinWindStrength(choMinWind.getSelectedIndex());
        conditions.setMaxWindStrength(choMaxWind.getSelectedIndex());
        conditions.setAtmosphere(choAtmosphere.getSelectedIndex());
        conditions.setFog(choFog.getSelectedIndex());
        conditions.setBlowingSand(cBlowingSands.isSelected());
        conditions.setShiftingWindDirection(cShiftWindDir.isSelected());
        conditions.setShiftingWindStrength(cShiftWindStr.isSelected());
        conditions.setTemperature(Integer.parseInt(fldTemp.getText()));
        conditions.setGravity(Float.parseFloat(fldGrav.getText()));
        conditions.setEMI(cEMI.isSelected());
        conditions.setTerrainAffected(cTerrainAffected.isSelected());

        if (client != null && shouldSendToServer) {
            send();
        }
    }

    private void updateForm() {
        if (!validateForm()) {
            return;
        }

        setConditions(false);
        conditions.alterConditions(conditions);
        refreshConditions();
    }

    private void send() {
        client.getClient().sendPlanetaryConditions(conditions);
    }

    private boolean validateForm() {
        // check for reasonable values and some conditionals
        int temper = 25;
        float grav = (float) 1.0;
        try {
            temper = Integer.parseInt(fldTemp.getText());
        } catch (NumberFormatException er) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("PlanetaryConditionsDialog.EnterValidTemperature"),
                            Messages
                                    .getString("PlanetaryConditionsDialog.NumberFormatError"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            grav = Float.parseFloat(fldGrav.getText());
        } catch (NumberFormatException er) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("PlanetaryConditionsDialog.EnterValidGravity"),
                            Messages
                                    .getString("PlanetaryConditionsDialog.NumberFormatError"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if ((temper > 200) || (temper < -200)) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("PlanetaryConditionsDialog.EnterValidTemperature"),
                            Messages
                                    .getString("PlanetaryConditionsDialog.NumberFormatError"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }

        int weather = currentWeather = choWeather.getSelectedIndex();
        if (temper >= 30 && (weather == PlanetaryConditions.WE_LIGHT_HAIL ||
                weather == PlanetaryConditions.WE_HEAVY_HAIL ||
                weather == PlanetaryConditions.WE_LIGHT_SNOW || 
                weather == PlanetaryConditions.WE_SLEET ||
                weather == PlanetaryConditions.WE_SNOW_FLURRIES ||
                weather == PlanetaryConditions.WE_HEAVY_SNOW ||
                weather == PlanetaryConditions.WE_ICE_STORM || 
                weather == PlanetaryConditions.WE_BLIZZARD ||
                weather == PlanetaryConditions.WE_MOD_SNOW)) {
            JOptionPane
                    .showMessageDialog(
                        frame, 
                        Messages.getString("PlanetaryConditionsDialog.EnterValidTemperatureExtreme"), 
                        Messages.getString("PlanetaryConditionsDialog.NumberFormatError"), 
                        JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if ((grav < 0.1) || (grav > 10.0)) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("PlanetaryConditionsDialog.EnterValidGravity"),
                            Messages
                                    .getString("PlanetaryConditionsDialog.NumberFormatError"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }

        //make sure that the minimum and maximum wind conditions fall within the actual
        if(choWind.getSelectedIndex() < choMinWind.getSelectedIndex()) {
            choMinWind.setSelectedIndex(choWind.getSelectedIndex());
        }
        if(choWind.getSelectedIndex() > choMaxWind.getSelectedIndex()) {
            choMaxWind.setSelectedIndex(choWind.getSelectedIndex());
        }

        // can't combine certain wind conditions with certain atmospheres
        int wind = choWind.getSelectedIndex();
        int atmo = choAtmosphere.getSelectedIndex();
        if ((atmo == PlanetaryConditions.ATMO_VACUUM)
                && (wind > PlanetaryConditions.WI_NONE)) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("PlanetaryConditionsDialog.VacuumWind"),
                            Messages
                                    .getString("PlanetaryConditionsDialog.Incompatible"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if ((atmo == PlanetaryConditions.ATMO_TRACE)
                && (wind > PlanetaryConditions.WI_STORM)) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("PlanetaryConditionsDialog.TraceWind"),
                            Messages
                                    .getString("PlanetaryConditionsDialog.Incompatible"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if ((atmo == PlanetaryConditions.ATMO_THIN)
                && (wind > PlanetaryConditions.WI_TORNADO_F13)) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("PlanetaryConditionsDialog.ThinWind"),
                            Messages
                                    .getString("PlanetaryConditionsDialog.Incompatible"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // can't combine certain weather conditions with certain atmospheres
        if ((atmo == PlanetaryConditions.ATMO_VACUUM
                || atmo == PlanetaryConditions.ATMO_TRACE
                || atmo == PlanetaryConditions.ATMO_THIN)
                && (weather > PlanetaryConditions.WE_NONE)) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("PlanetaryConditionsDialog.VacuumWeather"),
                            Messages
                                    .getString("PlanetaryConditionsDialog.Incompatible"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }


    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butOkay) {
            if (!validateForm()) return;
            setConditions();
            setVisible(false);
        } else if (e.getSource() == butCancel) {
            refreshConditions();
            setVisible(false);
        }
    }
}
