/*
* MegaMek -
* Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018, 2020 The MegaMek Team
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.common.Configuration;
import megamek.common.PlanetaryConditions;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

import static megamek.client.ui.swing.util.UIUtil.*;
import static megamek.common.PlanetaryConditions.*;

/**
 * A dialog that allows for customization of planetary conditions
 *
 * @author Jay Lawson
 * @author Simon
 */
public class PlanetaryConditionsDialog extends ClientDialog {

    private static final long serialVersionUID = -4426594323169113468L;
    
    /** Creates new PlanetaryConditionsDialog and takes the conditions from the client's Game. */
    public PlanetaryConditionsDialog(ClientGUI cl) {
        super(cl.frame, Messages.getString("PlanetaryConditionsDialog.title"), true, true);
        client = cl;
        setupDialog();
        update(client.getClient().getGame().getPlanetaryConditions());
    }
    
    /** Creates new PlanetaryConditionsDialog and sets the given conditions. Used for scenarios. */
    public PlanetaryConditionsDialog(JFrame frame, PlanetaryConditions conditions) {
        super(frame, Messages.getString("PlanetaryConditionsDialog.title"), true, true);
        setupDialog();
        update(conditions);
    }
    
    /** Sets the dialog visible and returns true if the user pressed the Okay button. */
    public boolean showDialog() {
        userResponse = false;
        setVisible(true);
        return userResponse;
    }

    /** Returns the conditions chosen by the player. */
    public PlanetaryConditions getConditions() {
        setConditions();
        return conditions;
    }
    
    /** Stores the given conditions and updates the dialog fields. */
    public void update(PlanetaryConditions planetConditions) {
        conditions = (PlanetaryConditions) planetConditions.clone();
        refreshValues();
        adaptToWeatherAtmo();
    }
    
    // PRIVATE

    private ClientGUI client;
    private PlanetaryConditions conditions;
    
    private static final int TOOLTIP_WIDTH = 300;
    private static final String PCD = "PlanetaryConditionsDialog.";
    private JLabel labLight = new JLabel(Messages.getString(PCD + "labLight"), SwingConstants.RIGHT); 
    private JComboBox<String> comLight = new JComboBox<>();
    private JLabel labWeather = new TipLabel(Messages.getString(PCD + "labWeather"), SwingConstants.RIGHT);
    private JComboBox<String> comWeather = new JComboBox<>();
    private JLabel labWind = new TipLabel(Messages.getString(PCD + "labWind"), SwingConstants.RIGHT);
    private JComboBox<String> comWind = new JComboBox<>();
    private JLabel labMinWind = new JLabel(Messages.getString(PCD + "labMinWind"), SwingConstants.RIGHT); 
    private JComboBox<String> comWindFrom = new JComboBox<>();
    private JLabel labMaxWind = new JLabel(Messages.getString(PCD + "labMaxWind"), SwingConstants.RIGHT); 
    private JComboBox<String> comWindDirection = new JComboBox<>();
    private JLabel labWindDirection = new JLabel(Messages.getString(PCD + "labWindDirection"), SwingConstants.RIGHT);
    private JComboBox<String> comWindTo = new JComboBox<>();
    private JLabel labAtmosphere = new TipLabel(Messages.getString(PCD + "labAtmosphere"), SwingConstants.RIGHT);
    private JComboBox<String> comFog = new JComboBox<>();
    private JLabel labFog = new TipLabel(Messages.getString(PCD + "labFog"), SwingConstants.RIGHT);
    private JComboBox<String> comAtmosphere = new JComboBox<>();
    private JLabel labBlowingSands = new TipLabel(Messages.getString(PCD + "BlowingSands"), SwingConstants.RIGHT);
    private JCheckBox chkBlowingSands = new JCheckBox();
    private JLabel labShiftWindDir = new JLabel(Messages.getString(PCD + "shiftWindDir"), SwingConstants.RIGHT);
    private JCheckBox chkShiftWindDir = new JCheckBox();
    private JLabel labShiftWindStr = new JLabel(Messages.getString(PCD + "shiftWindStr"), SwingConstants.RIGHT);
    private JCheckBox chkShiftWindStr = new JCheckBox();
    private JTextField fldTemp = new JTextField(4);
    private JLabel labTemp = new TipLabel(Messages.getString(PCD + "labTemp"), SwingConstants.RIGHT);
    private JTextField fldGrav = new JTextField(4);
    private JLabel labGrav = new TipLabel(Messages.getString(PCD + "labGrav"), SwingConstants.RIGHT);
    private JLabel labEMI = new JLabel(Messages.getString(PCD + "EMI"), SwingConstants.RIGHT);
    private JCheckBox chkEMI = new JCheckBox();
    private JLabel labTerrainAffected = new JLabel(Messages.getString(PCD + "TerrainAffected"), SwingConstants.RIGHT);
    private JCheckBox chkTerrainAffected = new JCheckBox();

    private JButton butOkay = new DialogButton(Messages.getString("Okay")); 
    private JButton butCancel = new DialogButton(Messages.getString("Cancel")); 

    private boolean userResponse;
    
    private void setupDialog() {
        JPanel mainPanel = new JPanel();
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel(), BorderLayout.PAGE_END);
        
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(headerSection());
        mainPanel.add(generalSection());
        mainPanel.add(weatherSection());
        mainPanel.add(dynamicSection());
        mainPanel.add(Box.createVerticalGlue());
        
        setupCombos();
    }
    
    private JPanel headerSection() {
        JPanel result = new FixedYPanel();
        result.setAlignmentX(Component.LEFT_ALIGNMENT);
        File iconFile = new MegaMekFile(Configuration.widgetsDir(), "Planetary.png").getFile();
        Image image = ImageUtil.loadImageFromFile(iconFile.toString());
        Icon planetIcon = new ImageIcon(image.getScaledInstance(scaleForGUI(40), -1, Image.SCALE_SMOOTH));
        JLabel planetLabel = new JLabel(Messages.getString("PlanetaryConditionsDialog.title"), 
                planetIcon, SwingConstants.CENTER);
        planetLabel.setIconTextGap(scaleForGUI(12));
        planetLabel.setBorder(new EmptyBorder(15, 0, 10, 0));
        result.add(planetLabel);
        return result;
    }

    private JPanel generalSection() {
        JPanel result = new OptionPanel("PlanetaryConditionsDialog.header.general");
        Content panContent = new Content(new GridLayout(6, 2, 10, 5));
        result.add(panContent);
        panContent.add(labTemp);
        panContent.add(fldTemp);
        panContent.add(labGrav);
        panContent.add(fldGrav);
        panContent.add(labLight);
        panContent.add(comLight);
        panContent.add(labAtmosphere);
        panContent.add(comAtmosphere);
        panContent.add(labEMI);
        panContent.add(chkEMI);
        panContent.add(labTerrainAffected);
        panContent.add(chkTerrainAffected);
        return result;
    }
    
    private JPanel weatherSection() {
        JPanel result = new OptionPanel("PlanetaryConditionsDialog.header.weather");
        Content panContent = new Content(new GridLayout(5, 2, 10, 5));
        result.add(panContent);
        panContent.add(labWeather);
        panContent.add(comWeather);
        panContent.add(labWind);
        panContent.add(comWind);
        panContent.add(labWindDirection);
        panContent.add(comWindDirection);
        panContent.add(labFog);
        panContent.add(comFog);
        panContent.add(labBlowingSands);
        panContent.add(chkBlowingSands);
        return result;
    }
    
    private JPanel dynamicSection() {
        JPanel result = new OptionPanel("PlanetaryConditionsDialog.header.dynamic");
        Content panContent = new Content(new GridLayout(4, 2, 10, 5));
        result.add(panContent);
        panContent.add(labShiftWindDir);
        panContent.add(chkShiftWindDir);
        panContent.add(labShiftWindStr);
        panContent.add(chkShiftWindStr);
        panContent.add(labMinWind);
        panContent.add(comWindFrom);
        panContent.add(labMaxWind);
        panContent.add(comWindTo);
        return result;
    }
    
    private JPanel buttonPanel() {
        JPanel result = new JPanel(new FlowLayout());
        butOkay.addActionListener(listener);
        result.add(butOkay);
        result.add(new DialogButton(new CancelAction(this)));
        return result;
    }
    
    /** Fills the dialog comboboxes. */
    private void setupCombos() {
        for (int i = 0; i < PlanetaryConditions.L_SIZE; i++) {
            comLight.addItem(PlanetaryConditions.getLightDisplayableName(i));
        }
        for (int i = 0; i < PlanetaryConditions.WE_SIZE; i++) {
            comWeather.addItem(PlanetaryConditions.getWeatherDisplayableName(i));
        }
        for (int i = 0; i < PlanetaryConditions.WI_SIZE; i++) {
            comWind.addItem(PlanetaryConditions.getWindDisplayableName(i));
            comWindFrom.addItem(PlanetaryConditions.getWindDisplayableName(i));
            comWindTo.addItem(PlanetaryConditions.getWindDisplayableName(i));
        }
        for (int i = 0; i < PlanetaryConditions.DIR_SIZE; i++) {
            comWindDirection.addItem(PlanetaryConditions.getWindDirDisplayableName(i));
        }
        for (int i = 0; i < PlanetaryConditions.ATMO_SIZE; i++) {
            comAtmosphere.addItem(PlanetaryConditions.getAtmosphereDisplayableName(i));
        }
        for (int i = 0; i < PlanetaryConditions.FOG_SIZE; i++) {
            comFog.addItem(PlanetaryConditions.getFogDisplayableName(i));
        }
    }
    
    /** Adds all required listeners for the dialog fields. */
    private void addListeners() {
        comAtmosphere.addActionListener(listener);
        fldTemp.addFocusListener(focusListener);
        comLight.addActionListener(listener);
        comAtmosphere.addActionListener(listener);
        fldGrav.addFocusListener(focusListener);
        comWind.addActionListener(listener);
        comWeather.addActionListener(listener);
        comFog.addActionListener(listener);
        chkShiftWindStr.addActionListener(listener);
        chkBlowingSands.addActionListener(listener);
        comWindFrom.addActionListener(listener);
        comWindTo.addActionListener(listener);
    }
    
    /** Removes all listeners from the dialog fields. */
    private void removeListeners() {
        comAtmosphere.removeActionListener(listener);
        fldTemp.removeFocusListener(focusListener);
        comLight.removeActionListener(listener);
        comAtmosphere.removeActionListener(listener);
        fldGrav.removeFocusListener(focusListener);
        comWind.removeActionListener(listener);
        comWeather.removeActionListener(listener);
        comFog.removeActionListener(listener);
        chkShiftWindStr.removeActionListener(listener);
        chkBlowingSands.removeActionListener(listener);
        comWindFrom.removeActionListener(listener);
        comWindTo.removeActionListener(listener);
    }

    /** Updates the dialog fields with values from the stored conditions. */
    private void refreshValues() {
        removeListeners();
        comLight.setSelectedIndex(conditions.getLight());
        comWeather.setSelectedIndex(conditions.getWeather());
        comWind.setSelectedIndex(conditions.getWindStrength());
        comWindFrom.setSelectedIndex(conditions.getMinWindStrength());
        comWindTo.setSelectedIndex(conditions.getMaxWindStrength());
        comWindDirection.setSelectedIndex(conditions.getWindDirection());
        comAtmosphere.setSelectedIndex(conditions.getAtmosphere());
        comFog.setSelectedIndex(conditions.getFog());
        chkBlowingSands.setSelected(conditions.isSandBlowing());
        chkShiftWindDir.setSelected(conditions.shiftingWindDirection());
        chkShiftWindStr.setSelected(conditions.shiftingWindStrength());
        fldTemp.setText(Integer.toString(conditions.getTemperature()));
        fldGrav.setText(Float.toString(conditions.getGravity()));
        chkEMI.setSelected(conditions.hasEMI());
        chkTerrainAffected.setSelected(conditions.isTerrainAffected());
        addListeners();
        refreshWindShift();
    }

    /** 
     * Updates the stored conditions from the dialog fields. 
     */
    private void setConditions() {
        // make the changes to the planetary conditions
        conditions.setLight(comLight.getSelectedIndex());
        conditions.setWeather(comWeather.getSelectedIndex());
        conditions.setWindStrength(comWind.getSelectedIndex());
        conditions.setWindDirection(comWindDirection.getSelectedIndex());
        refreshWindRange();
        conditions.setAtmosphere(comAtmosphere.getSelectedIndex());
        conditions.setFog(comFog.getSelectedIndex());
        conditions.setBlowingSand(chkBlowingSands.isSelected());
        conditions.setShiftingWindDirection(chkShiftWindDir.isSelected());
        conditions.setShiftingWindStrength(chkShiftWindStr.isSelected());
        conditions.setTemperature(Integer.parseInt(fldTemp.getText()));
        conditions.setGravity(Float.parseFloat(fldGrav.getText()));
        conditions.setEMI(chkEMI.isSelected());
        conditions.setTerrainAffected(chkTerrainAffected.isSelected());
    }

    /** 
     * Validates the current entries in the dialog. Any conflicting entries are marked
     * and a helper tooltip attached. Does not change entries.
     */
    private boolean validateEntries() {
        StringBuilder tempTip = new StringBuilder();
        StringBuilder wthrTip = new StringBuilder();
        StringBuilder gravTip = new StringBuilder();
        StringBuilder windTip = new StringBuilder();
        StringBuilder atmoTip = new StringBuilder();
        StringBuilder sandTip = new StringBuilder();
        int weather = comWeather.getSelectedIndex();
        int temp = 0;
        float grav = (float) 1.0;
        try {
            temp = Integer.parseInt(fldTemp.getText());
        } catch (NumberFormatException er) {
            tempTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.integer"));
        }
        if ((temp > 200) || (temp < -200)) {
            tempTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.tempRange"));
        }
        
        // Currently, MM does not automatically include the effects of -40, -50 or -60 °C
        // with snowy weather and instead relies on the temperature itself being set correctly.
        // I believe that the rules allow a temp of e.g. -5 °C with snow (and why not?) and
        // that the "includes the effects" statements are meant to reduce repetition. If the temp
        // were fixed to -40 °C, the text of the rules saying "cannot be used with temp of 30 °C 
        // or more" would be unnecessary. With the current rules handling, temp has to be set to 
        // the necessary values. Therefore the following check for 30 °C is not needed.
        //        if (temp >= 30 && requiresLowTemp(weather)) {
        //            tempValid = false;
        //            wthrValid = false;
        //            tempTip.append("The Temperature cannot be 30 °C or more in snowy weather.<BR>");
        //            wthrTip.append("The Temperature cannot be 30 °C or more in snowy weather.<BR>");
        //        }
        
        try {
            grav = Float.parseFloat(fldGrav.getText());
        } catch (NumberFormatException er) {
            gravTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.number"));
        }
        if ((grav < 0.1) || (grav > 10.0)) {
            gravTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.gravRange"));
        }
        
        int wind = comWind.getSelectedIndex();
        int atmo = comAtmosphere.getSelectedIndex();
        
        if ((chkBlowingSands.isSelected()) && (wind < WI_MOD_GALE) 
                && (!chkShiftWindStr.isSelected() 
                        || (conditions.getMinWindStrength() > WI_MOD_GALE) 
                        || (conditions.getMaxWindStrength() < WI_MOD_GALE))) {
            windTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.sandsLost"));
            sandTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.sandsLost"));
        }

        if ((atmo == ATMO_TRACE) && (wind == WI_LIGHT_GALE)) {
            atmoTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.traceLightGale"));
            windTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.traceLightGale"));
        }
        
        // The following temperature checks are not exactly what the rules demand, but see the comment above.
        if (((weather == WE_LIGHT_SNOW) || (weather == WE_SLEET)
                || (weather == WE_LIGHT_HAIL) || (weather == WE_HEAVY_HAIL))
                && (temp > -40)) {
            tempTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.lightSnowTemp"));
            wthrTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.lightSnowTemp"));
        }
        
        if (((weather == WE_HEAVY_SNOW) || (weather == WE_MOD_SNOW)
                || (weather == WE_SNOW_FLURRIES))
                && (temp > -50)) {
            tempTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.modSnowTemp"));
            wthrTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.modSnowTemp"));
        }
        
        if ((weather == WE_ICE_STORM) && (temp > -60)) {
            tempTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.iceStormTemp"));
            wthrTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.iceStormTemp"));
        }
        
        if (chkShiftWindStr.isSelected()) {
            if ((comWind.getSelectedIndex() < conditions.getMinWindStrength()) 
                    || (comWind.getSelectedIndex() > conditions.getMaxWindStrength())) {
                windTip.append(Messages.getString("PlanetaryConditionsDialog.invalid.windRange"));
            }
        }
    
        refreshWarning(labTemp, tempTip);
        refreshWarning(labWeather, wthrTip);
        refreshWarning(labAtmosphere, atmoTip);
        refreshWarning(labGrav, gravTip);
        refreshWarning(labWind, windTip);
        refreshWarning(labBlowingSands, sandTip);
        
        return (tempTip.length() == 0) && (wthrTip.length() == 0) && (atmoTip.length() == 0) 
                && (sandTip.length() == 0) && (windTip.length() == 0) && (gravTip.length() == 0);
    }
    
    /** 
     * Marks the given label red and adds the given tooltip text if isValid is false,
     * otherwise resets the label color and removes the tooltip. 
     */
    private void refreshWarning(JLabel label, StringBuilder text) {
        if (text.length() == 0) {
            label.setForeground(null);
            label.setToolTipText(null);
        } else {
            label.setForeground(GUIPreferences.getInstance().getWarningColor());
            label.setToolTipText(text.toString());
        }
    }
    
    /** 
     * Updates the enabled state of some fields based on the atmosphere setting. 
     * Also resets the state for some settings, e.g. vacuum will set wind and
     * weather to none.
     */
    private void adaptToWeatherAtmo() {
        boolean isVacuum = comAtmosphere.getSelectedIndex() == ATMO_VACUUM;
        boolean isTraceThin = comAtmosphere.getSelectedIndex() == ATMO_THIN 
                | comAtmosphere.getSelectedIndex() == ATMO_TRACE;
        boolean isDense = !isVacuum && !isTraceThin;
        int weather = comWeather.getSelectedIndex();
        boolean specificWind = (weather == WE_SNOW_FLURRIES) || (weather == WE_ICE_STORM) 
                || (weather == WE_GUSTING_RAIN) || (weather == WE_LIGHTNING_STORM);
              
        removeListeners();
        if (isTraceThin) {
            comWeather.setSelectedIndex(PlanetaryConditions.WE_NONE);
            comFog.setSelectedIndex(PlanetaryConditions.FOG_NONE);
        }
        if (isVacuum) {
            comWind.setSelectedIndex(PlanetaryConditions.WI_NONE);
            chkBlowingSands.setSelected(false);
            chkShiftWindDir.setSelected(false);
            chkShiftWindStr.setSelected(false);
            comWind.setSelectedIndex(WI_NONE);
            comWeather.setSelectedIndex(WE_NONE);
            comFog.setSelectedIndex(FOG_NONE);
        }
        if (specificWind) {
            chkShiftWindStr.setSelected(false);
            switch (weather) {
                case WE_LIGHTNING_STORM:
                case WE_SNOW_FLURRIES:
                case WE_ICE_STORM:
                    comWind.setSelectedIndex(WI_MOD_GALE);
                    break;
                case WE_GUSTING_RAIN:
                    comWind.setSelectedIndex(WI_STRONG_GALE);
            }
        }
        addListeners();
        labWeather.setEnabled(isDense);
        comWeather.setEnabled(isDense);
        labFog.setEnabled(isDense);
        comFog.setEnabled(isDense);
        labWind.setEnabled(!isVacuum && !specificWind);
        comWind.setEnabled(!isVacuum && !specificWind);
        labBlowingSands.setEnabled(!isVacuum);
        chkBlowingSands.setEnabled(!isVacuum);
        labShiftWindDir.setEnabled(!isVacuum);
        chkShiftWindDir.setEnabled(!isVacuum);
        labShiftWindStr.setEnabled(!isVacuum && !specificWind);
        chkShiftWindStr.setEnabled(!isVacuum && !specificWind);
        comWindDirection.setEnabled(!isVacuum);
        labWindDirection.setEnabled(!isVacuum);
        refreshWindShift();
    }
    
    /** Sets the temperature to at most -40, -50 or -60 for snow conditions. */
    private void adaptTemperature() {
        int weather = comWeather.getSelectedIndex();
        int maxTemp = 200;
        switch (weather) {
            case WE_LIGHT_SNOW:
            case WE_SLEET:
            case WE_LIGHT_HAIL:
            case WE_HEAVY_HAIL:
                maxTemp = -40;
                break;
            case WE_HEAVY_SNOW:
            case WE_MOD_SNOW:
            case WE_SNOW_FLURRIES:
                maxTemp = -50;
                break;
            case WE_ICE_STORM:
                maxTemp = -60;
        }
        setMaximumTemperature(maxTemp);
    }
    
    /** Sets the wind to at least moderate gale if Blowing Sands is activated. */
    private void adaptWindToBlowingSands() {
        if (chkBlowingSands.isSelected()) {
            setMinimumWind(WI_MOD_GALE);
        }
    }
    
    /** Updates the enabled state of the shifting wind strength fields. */
    private void refreshWindShift() {
        labMinWind.setEnabled(chkShiftWindStr.isSelected());
        comWindFrom.setEnabled(chkShiftWindStr.isSelected());
        labMaxWind.setEnabled(chkShiftWindStr.isSelected());
        comWindTo.setEnabled(chkShiftWindStr.isSelected());
    }
    
    /** Sets wind strength to Moderate Gale if it is less than that. */
    private void setMinimumWind(int minWind) {
        if (comWind.getSelectedIndex() < minWind) {
            removeListeners();
            comWind.setSelectedIndex(minWind);
            addListeners();
        }
    }
    
    /** Sets the temperature to the given value if it is higher than that. */
    private void setMaximumTemperature(int maxTemp) {
        int currentTemp;
        try {
            currentTemp = Integer.parseInt(fldTemp.getText());
        } catch (NumberFormatException er) {
            currentTemp = 200;
        }
        if (currentTemp > maxTemp) {
            removeListeners();
            fldTemp.setText(Integer.toString(maxTemp));
            addListeners();
        }
    }

    ActionListener listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            
            if (e.getSource() == butOkay) {
                userResponse = true;
                setConditions();
                setVisible(false);
                
            } else if (e.getSource() == butCancel) {
                setVisible(false);

            } else if ((e.getSource() instanceof JComboBox<?>)
             || (e.getSource() instanceof JCheckBox))  {
                if (e.getSource() == chkBlowingSands) {
                    adaptWindToBlowingSands();
                }
                if (e.getSource() == comAtmosphere) {
                    adaptToWeatherAtmo();
                }
                if (e.getSource() == comWeather) {
                    adaptToWeatherAtmo();
                    adaptTemperature();
                }
                if (e.getSource() == chkShiftWindStr) {
                    refreshWindShift();
                }
                if ((e.getSource() == comWindFrom) || (e.getSource() == comWindTo)) {
                    refreshWindRange();
                }
                butOkay.setEnabled(validateEntries());
            }
        }
    };
    
    /**
     * Extracts the minimum and maximum wind from the two comboboxes. Also,
     * if the current wind is outside that range, sets the current wind to the
     * closer border of that range.
     */
    private void refreshWindRange() {
        int min = Math.min(comWindFrom.getSelectedIndex(), comWindTo.getSelectedIndex());
        int max = Math.max(comWindFrom.getSelectedIndex(), comWindTo.getSelectedIndex());
        conditions.setMinWindStrength(min);
        conditions.setMaxWindStrength(max);
        removeListeners();
        if (comWind.getSelectedIndex() < min) {
            comWind.setSelectedIndex(min);
        }
        if (comWind.getSelectedIndex() > max) {
            comWind.setSelectedIndex(max);
        }
        addListeners();
    }

    /** validate the entries whenever something is selected or focus changes. */
    FocusListener focusListener = new FocusListener() {
        
        @Override
        public void focusLost(FocusEvent e) {
            butOkay.setEnabled(validateEntries());
        }
        
        @Override
        public void focusGained(FocusEvent e) { }
    };

}
