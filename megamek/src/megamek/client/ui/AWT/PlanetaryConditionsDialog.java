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

package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import megamek.client.ui.Messages;
import megamek.common.PlanetaryConditions;

/**
 * A dialog that allows for customization of planetary conditions
 * 
 * @author Jay Lawson
 * @version
 */
public class PlanetaryConditionsDialog extends Dialog implements ActionListener {
    
    private static final long serialVersionUID = -4426594323169113468L;
    
    private ClientGUI client;
    private PlanetaryConditions conditions;
    private Label labLight = new Label(Messages
            .getString("PlanetaryConditionsDialog.labLight"), Label.RIGHT); //$NON-NLS-1$
    private Choice choLight = new Choice();
    private Label labWeather = new Label(Messages
            .getString("PlanetaryConditionsDialog.labWeather"), Label.RIGHT); //$NON-NLS-1$
    private Choice choWeather = new Choice();
    private Label labWind = new Label(Messages
            .getString("PlanetaryConditionsDialog.labWind"), Label.RIGHT); //$NON-NLS-1$
    private Choice choWind = new Choice();
    private Label labAtmosphere = new Label(Messages
            .getString("PlanetaryConditionsDialog.labAtmosphere"), Label.RIGHT); //$NON-NLS-1$
    private Choice choAtmosphere = new Choice();
    private Label labFog = new Label(Messages
            .getString("PlanetaryConditionsDialog.labFog"), Label.RIGHT); //$NON-NLS-1$
    private Choice choFog = new Choice();
    private Checkbox cBlowingSands = new Checkbox(Messages
            .getString("PlanetaryConditionsDialog.BlowingSands"));
    private Checkbox cShiftWindDir = new Checkbox(Messages
            .getString("PlanetaryConditionsDialog.shiftWindDir"));
    private Checkbox cShiftWindStr = new Checkbox(Messages
            .getString("PlanetaryConditionsDialog.shiftWindStr"));
    private Label labTemp = new Label(Messages
            .getString("PlanetaryConditionsDialog.labTemp"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldTemp = new TextField(4);
    private Label labGrav = new Label(Messages
            .getString("PlanetaryConditionsDialog.labGrav"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldGrav = new TextField(4);
    private Checkbox cEMI = new Checkbox(Messages
            .getString("PlanetaryConditionsDialog.EMI"));   
    private Checkbox cTerrainAffected = new Checkbox(Messages
            .getString("PlanetaryConditionsDialog.TerrainAffected"));
            
    private Panel panButtons = new Panel();    
    private Button butOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
    
    private Panel panOptions = new Panel();
    
    /**
     * Initialize this dialog.
     * 
     * @param frame - the <code>Frame</code> parent of this dialog.
     * @param options - the <code>GameOptions</code> to be displayed.
     */
    private void init(Frame frame, PlanetaryConditions conditions) {
        this.conditions = (PlanetaryConditions)conditions.clone();
        
        setupConditions();
        setupButtons();
        
        //layout
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
        super(client.frame, Messages.getString("PlanetaryConditionsDialog.title"), true); //$NON-NLS-1$
        this.client = client;
        this.init(client.frame, client.getClient().game.getPlanetaryConditions());
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
        c.gridwidth = 1;
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
    
    public void update(PlanetaryConditions conditions) {
        this.conditions = (PlanetaryConditions)conditions.clone();
        refreshConditions();
    }
    
    private void refreshConditions() {
        
        choLight.removeAll();
        for(int i = 0; i < PlanetaryConditions.L_SIZE; i++) {
            choLight.add(PlanetaryConditions.getLightDisplayableName(i));
        }
        choLight.select(conditions.getLight());    
        
        choWeather.removeAll();
        for(int i = 0; i < PlanetaryConditions.WE_SIZE; i++) {
            choWeather.add(PlanetaryConditions.getWeatherDisplayableName(i));
        }
        choWeather.select(conditions.getWeather());    
        
        choWind.removeAll();
        for(int i = 0; i < PlanetaryConditions.WI_SIZE; i++) {
            choWind.add(PlanetaryConditions.getWindDisplayableName(i));
        }
        choWind.select(conditions.getWindStrength());    
        
        choAtmosphere.removeAll();
        for(int i = 0; i < PlanetaryConditions.ATMO_SIZE; i++) {
            choAtmosphere.add(PlanetaryConditions.getAtmosphereDisplayableName(i));
        }
        choAtmosphere.select(conditions.getAtmosphere());    
        
        choFog.removeAll();
        for(int i = 0; i < PlanetaryConditions.FOG_SIZE; i++) {
            choFog.add(PlanetaryConditions.getFogDisplayableName(i));
        }
        choFog.select(conditions.getFog());    
        
        cBlowingSands.setState(conditions.isSandBlowing());
        cShiftWindDir.setState(conditions.shiftingWindDirection());
        cShiftWindStr.setState(conditions.shiftingWindStrength());
        
        fldTemp.setText(Integer.toString(conditions.getTemperature()));
        fldGrav.setText(Float.toString(conditions.getGravity()));
        
        cEMI.setState(conditions.hasEMI());
        
        cTerrainAffected.setState(conditions.isTerrainAffected());
    }
    
    public void send() {
        
        //make the changes to the planetary conditions
        conditions.setLight(choLight.getSelectedIndex());
        conditions.setWeather(choWeather.getSelectedIndex());
        conditions.setWindStrength(choWind.getSelectedIndex());
        conditions.setAtmosphere(choAtmosphere.getSelectedIndex());
        conditions.setFog(choFog.getSelectedIndex());
        conditions.setBlowingSand(cBlowingSands.getState());
        conditions.setShiftingWindDirection(cShiftWindDir.getState());
        conditions.setShiftingWindStrength(cShiftWindStr.getState());
        conditions.setTemperature(Integer.parseInt(fldTemp.getText()));
        conditions.setGravity(Float.parseFloat(fldGrav.getText()));
        conditions.setEMI(cEMI.getState());
        conditions.setTerrainAffected(cTerrainAffected.getState());
            
        client.getClient().sendPlanetaryConditions(conditions);
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butOkay) {
            //check for reasonable values and some conditionals
            int temper = 25;
            float grav = (float)1.0;
            try {
                temper = Integer.parseInt(fldTemp.getText());
            } catch (NumberFormatException er) {
                new AlertDialog(
                        client.frame,
                        Messages
                                .getString("PlanetaryConditionsDialog.NumberFormatError"), Messages.getString("PlanetaryConditionsDialog.EnterValidTemperature")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            try {
                grav = Float.parseFloat(fldGrav.getText());
            } catch (NumberFormatException er) {
                new AlertDialog(
                        client.frame,
                        Messages
                                .getString("PlanetaryConditionsDialog.NumberFormatError"), Messages.getString("PlanetaryConditionsDialog.EnterValidGravity")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            
            if(temper > 200 || temper < -200) {
                new AlertDialog(
                        client.frame,
                        Messages
                                .getString("PlanetaryConditionsDialog.NumberFormatError"), Messages.getString("PlanetaryConditionsDialog.EnterValidTemperature")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
   
            if(grav < 0.1 || grav > 10.0) {
                new AlertDialog(
                        client.frame,
                        Messages
                                .getString("PlanetaryConditionsDialog.NumberFormatError"), Messages.getString("PlanetaryConditionsDialog.EnterValidGravity")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            
            //can't combine certain wind conditions with certain atmospheres
            int wind = choWind.getSelectedIndex();
            int atmo = choAtmosphere.getSelectedIndex();
            if(atmo == PlanetaryConditions.ATMO_VACUUM && wind > PlanetaryConditions.WI_NONE) {
                new AlertDialog(
                        client.frame,
                        Messages
                                .getString("PlanetaryConditionsDialog.Incompatible"), Messages.getString("PlanetaryConditionsDialog.VacuumWind")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            if(atmo == PlanetaryConditions.ATMO_TRACE && wind > PlanetaryConditions.WI_STORM) {
                new AlertDialog(
                        client.frame,
                        Messages
                                .getString("PlanetaryConditionsDialog.Incompatible"), Messages.getString("PlanetaryConditionsDialog.TraceWind")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            if(atmo == PlanetaryConditions.ATMO_THIN && wind > PlanetaryConditions.WI_TORNADO_F13) {
                new AlertDialog(
                        client.frame,
                        Messages
                                .getString("PlanetaryConditionsDialog.Incompatible"), Messages.getString("PlanetaryConditionsDialog.ThinWind")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            
            if (client != null) {
                send();
            }
        } else if (e.getSource() == butCancel) {
            refreshConditions();
        }
        this.setVisible(false);
    }
}
    