/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.CheckpointComboBox;
import megamek.common.MapSettings;

import javax.swing.*;
import java.awt.*;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 3/13/14 3:55 PM
 */
public class RandomMapPanelBasic extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = -6971330721623187856L;
    private static final String NONE = Messages.getString("RandomMapDialog.uiNONE");
    private static final String LOW = Messages.getString("RandomMapDialog.uiLow");
    private static final String MEDIUM = Messages.getString("RandomMapDialog.uiMedium");
    private static final String HIGH = Messages.getString("RandomMapDialog.uiHigh");
    private static final String[] LOW_HIGH_CHOICES = new String[]{NONE, LOW, MEDIUM, HIGH};

    private MapSettings mapSettings;

    private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);

    // Civilization
    private static final String CT_HUB = "HUB";
    private static final String CT_GRID = "GRID";
    private static final String CT_METRO = "METRO";
    private static final String CT_TOWN = "TOWN";
    private static final String[] CT_CHOICES = new String[]{NONE, CT_GRID, CT_HUB, CT_METRO, CT_TOWN};
    private final CheckpointComboBox<String> cityTypeCombo = new CheckpointComboBox<>(CT_CHOICES);
    private final CheckpointComboBox<String> fortifiedCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> pavementCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> plantedFieldsCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> roadsCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> rubbleCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);

    // Elevation
    private final CheckpointComboBox<String> cliffsCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> cratersCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> hillinessCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> mountainCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);

    // Natural Features
    private final CheckpointComboBox<String> roughsCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> sandsCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> swampsCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> woodsCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);

    // Water
    private final CheckpointComboBox<String> lakesCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> riversCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> iceCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);

    public RandomMapPanelBasic(MapSettings mapSettings) {
        setMapSettings(mapSettings);

        initGUI();
        validate();
    }

    private void initGUI() {
        JScrollPane civilizationPanel = setupCivilizationPanel();
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabCivilization"), civilizationPanel);

        JScrollPane elevationPanel = setupElevationPanel();
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabElevation"), elevationPanel);

        JScrollPane naturalFeaturesPanel = setupNaturalFeaturesPanel();
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabNatural"), naturalFeaturesPanel);

        JScrollPane waterPanel = setupWaterPanel();
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabWater"), waterPanel);

        tabbedPane.setSelectedComponent(civilizationPanel);

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    public void setMapSettings(MapSettings mapSettings) {
        if (mapSettings == null) {
            return;
        }
        this.mapSettings = mapSettings;
        loadMapSettings();
    }

    private void loadMapSettings() {
        lakesCombo.setSelectedItem(lakesToRange(this.mapSettings.getMinWaterSize(),
                                                this.mapSettings.getMinWaterSpots(),
                                                this.mapSettings.getProbDeep()));
        lakesCombo.checkpoint();
        riversCombo.setSelectedItem(percentageToRange(this.mapSettings.getProbRiver()));
        riversCombo.checkpoint();
        iceCombo.setSelectedItem(iceToRange(this.mapSettings.getMinIceSize(), this.mapSettings.getMinIceSpots()));
        iceCombo.checkpoint();
        roughsCombo.setSelectedItem(roughsToRange(this.mapSettings.getMinRoughSize(),
                                                  this.mapSettings.getMinRoughSpots()));
        roughsCombo.checkpoint();
        sandsCombo.setSelectedItem(sandsToRange(this.mapSettings.getMinSandSize(),
                                                this.mapSettings.getMinSandSpots()));
        sandsCombo.checkpoint();
        swampsCombo.setSelectedItem(swampsToRange(this.mapSettings.getMinSwampSize(),
                                                  this.mapSettings.getMinSwampSpots()));
        swampsCombo.checkpoint();
        woodsCombo.setSelectedItem(woodsToRange(this.mapSettings.getMinForestSize(),
                                                this.mapSettings.getMinForestSpots(),
                                                this.mapSettings.getProbHeavy()));
        woodsCombo.checkpoint();
        cliffsCombo.setSelectedItem(percentageToRange(this.mapSettings.getCliffs()));
        cliffsCombo.checkpoint();
        cratersCombo.setSelectedItem(cratersToRange(this.mapSettings.getProbCrater()));
        cratersCombo.checkpoint();
        hillinessCombo.setSelectedItem(percentageToRange(this.mapSettings.getHilliness()));
        hillinessCombo.checkpoint();
        mountainCombo.setSelectedItem(convertMountain(this.mapSettings.getMountainPeaks(),
                                                      this.mapSettings.getMountainHeightMin(),
                                                      this.mapSettings.getMountainWidthMin(),
                                                      this.mapSettings.getMountainStyle()));
        mountainCombo.checkpoint();
        cityTypeCombo.setSelectedItem(this.mapSettings.getCityType());
        cityTypeCombo.checkpoint();
        fortifiedCombo.setSelectedItem(fortifiedToRange(this.mapSettings.getMinFortifiedSize(),
                                                        this.mapSettings.getMinFortifiedSpots()));
        fortifiedCombo.checkpoint();
        pavementCombo.setSelectedItem(pavementToRange(this.mapSettings.getMinPavementSize(),
                                                      this.mapSettings.getMinPavementSpots()));
        pavementCombo.checkpoint();
        plantedFieldsCombo.setSelectedItem(plantedFieldsToRange(this.mapSettings.getMinPlantedFieldSize(),
                                                                this.mapSettings.getMinPlantedFieldSpots()));
        plantedFieldsCombo.checkpoint();
        roadsCombo.setSelectedItem(percentageToRange(this.mapSettings.getProbRoad()));
        roadsCombo.checkpoint();
        rubbleCombo.setSelectedItem(rubbleToRange(this.mapSettings.getMinRubbleSize(),
                                                  this.mapSettings.getMinRubbleSpots()));
        rubbleCombo.checkpoint();
    }

    private JScrollPane setupWaterPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        JPanel panel = new JPanel(layout);

        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(2, 2, 2, 2);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        final JLabel lakesLabel = new JLabel(Messages.getString("RandomMapDialog.labLakes"));
        panel.add(lakesLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        lakesCombo.setToolTipText(Messages.getString("RandomMapDialog.lakesCombo.toolTip"));
        panel.add(lakesCombo, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel riversLabel = new JLabel(Messages.getString("RandomMapDialog.labRivers"));
        panel.add(riversLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        riversCombo.setToolTipText(Messages.getString("RandomMapDialog.riversCombo.toolTip"));
        panel.add(riversCombo, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        constraints.weighty = 1;
        final JLabel iceLabel = new JLabel(Messages.getString("RandomMapDialog.labIce"));
        panel.add(iceLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        iceCombo.setToolTipText(Messages.getString("RandomMapDialog.iceCombo.toolTip"));
        panel.add(iceCombo, constraints);

        return new JScrollPane(panel);
    }

    private String iceToRange(int minSize, int minSpots) {
        int range = minSize + minSpots;
        if (range >= 6) {
            return HIGH;
        }
        if (range >= 4) {
            return MEDIUM;
        }
        if (range >= 2) {
            return LOW;
        }
        return NONE;
    }

    private String lakesToRange(int minSize, int minSpots, int percentDeep) {
        int range = percentDeep + (minSize * 5) + (minSpots * 10);
        if (range >= 100) {
            return HIGH;
        }
        if (range >= 65) {
            return MEDIUM;
        }
        if (range >= 20) {
            return LOW;
        }
        return NONE;
    }

    private JScrollPane setupNaturalFeaturesPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        JPanel panel = new JPanel(layout);

        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(2, 2, 2, 2);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        final JLabel roughsLabel = new JLabel(Messages.getString("RandomMapDialog.labRough"));
        panel.add(roughsLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        roughsCombo.setToolTipText(Messages.getString("RandomMapDialog.roughsCombo.toolTip"));
        panel.add(roughsCombo, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel sandsLabel = new JLabel(Messages.getString(("RandomMapDialog.labSand")));
        panel.add(sandsLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        sandsCombo.setToolTipText(Messages.getString("RandomMapDialog.sandsCombo.toolTip"));
        panel.add(sandsCombo, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel swampsLabel = new JLabel(Messages.getString("RandomMapDialog.labSwamp"));
        panel.add(swampsLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        swampsCombo.setToolTipText(Messages.getString("RandomMapDialog.swampsCombo.toolTip"));
        panel.add(swampsCombo, constraints);

        // Row 4, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        constraints.weighty = 1;
        final JLabel woodsLabel = new JLabel(Messages.getString("RandomMapDialog.labWoods"));
        panel.add(woodsLabel, constraints);

        // Row 4, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        woodsCombo.setToolTipText(Messages.getString("RandomMapDialog.woodsCombo.toolTip"));
        panel.add(woodsCombo, constraints);

        return new JScrollPane(panel);
    }

    private String roughsToRange(int minSize, int minSpots) {
        int range = minSize + minSpots;
        if (range >= 6) {
            return HIGH;
        }
        if (range >= 4) {
            return MEDIUM;
        }
        if (range >= 2) {
            return LOW;
        }
        return NONE;
    }

    private String sandsToRange(int minSize, int minSpots) {
        int range = minSize + minSpots;
        if (range >= 6) {
            return HIGH;
        }
        if (range >= 4) {
            return MEDIUM;
        }
        if (range >= 2) {
            return LOW;
        }
        return NONE;
    }

    private String swampsToRange(int minSize, int minSpots) {
        int range = minSize + minSpots;
        if (range >= 6) {
            return HIGH;
        }
        if (range >= 4) {
            return MEDIUM;
        }
        if (range >= 2) {
            return LOW;
        }
        return NONE;
    }

    private String woodsToRange(int minSize, int minSpots, int percentHeavy) {
        int range = percentHeavy + (minSize * 5) + (minSpots * 6);
        if (range >= 100) {
            return HIGH;
        }
        if (range >= 65) {
            return MEDIUM;
        }
        if (range >= 50) {
            return LOW;
        }
        return NONE;
    }

    private JScrollPane setupElevationPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        JPanel panel = new JPanel(layout);

        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(2, 2, 2, 2);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        final JLabel cliffsLabel = new JLabel(Messages.getString("RandomMapDialog.labCliffs"));
        panel.add(cliffsLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        cliffsCombo.setToolTipText(Messages.getString("RandomMapDialog.cliffsCombo.toolTip"));
        panel.add(cliffsCombo, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel cratersLabel = new JLabel(Messages.getString("RandomMapDialog.labCraters"));
        panel.add(cratersLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        cratersCombo.setToolTipText(Messages.getString("RandomMapDialog.cratersCombo.toolTip"));
        panel.add(cratersCombo, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel hillsLabel = new JLabel(Messages.getString("RandomMapDialog.labElevation"));
        panel.add(hillsLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        hillinessCombo.setToolTipText(Messages.getString("RandomMapDialog.hillinessCombo.toolTip"));
        panel.add(hillinessCombo, constraints);

        // Row 4, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weighty = 1;
        constraints.weightx = 0;
        final JLabel mountainsLabel = new JLabel(Messages.getString("RandomMapDialog.labMountain"));
        panel.add(mountainsLabel, constraints);

        // Row 4, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        mountainCombo.setToolTipText(Messages.getString("RandomMapDialog.mountainCombo.toolTip"));
        panel.add(mountainCombo, constraints);

        return new JScrollPane(panel);
    }

    private String cratersToRange(int craterProbability) {
        if (craterProbability >= 60) {
            return HIGH;
        }
        if (craterProbability >= 40) {
            return MEDIUM;
        }
        if (craterProbability >= 15) {
            return LOW;
        }
        return NONE;
    }

    private String convertMountain(int peaks, int minHeight, int minWidth, int style) {
        if (MapSettings.MOUNTAIN_SNOWCAPPED == style) {
            return HIGH;
        }

        if (minWidth >= 10) {
            return HIGH;
        }

        if (minHeight >= 8) {
            return HIGH;
        }
        if (minHeight >= 6) {
            return MEDIUM;
        }

        if (peaks < 1) {
            return NONE;
        }

        return LOW;
    }

    private String percentageToRange(int value) {
        if (value >= 60) {
            return HIGH;
        }
        if (value >= 40) {
            return MEDIUM;
        }
        if (value >= 15) {
            return LOW;
        }
        return NONE;
    }

    private String fortifiedToRange(int minSize, int minSpots) {
        int range = minSize + minSpots;
        if (range >= 5) {
            return HIGH;
        }
        if (range >= 3) {
            return MEDIUM;
        }
        if (range >= 2) {
            return LOW;
        }
        return NONE;
    }

    private String pavementToRange(int minSize, int minSpots) {
        int range = minSize + minSpots;
        if (range >= 6) {
            return HIGH;
        }
        if (range >= 4) {
            return MEDIUM;
        }
        if (range >= 2) {
            return LOW;
        }
        return NONE;
    }

    private String plantedFieldsToRange(int minSize, int minSpots) {
        int range = minSize + minSpots;
        if (range >= 6) {
            return HIGH;
        }
        if (range >= 4) {
            return MEDIUM;
        }
        if (range >= 2) {
            return LOW;
        }
        return NONE;
    }

    private String rubbleToRange(int minSize, int minSpots) {
        int range = minSize + minSpots;
        if (range >= 6) {
            return HIGH;
        }
        if (range >= 4) {
            return MEDIUM;
        }
        if (range >= 2) {
            return LOW;
        }
        return NONE;
    }

    private JScrollPane setupCivilizationPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        JPanel panel = new JPanel(layout);

        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(2, 2, 2, 2);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        final JLabel cityTypeLabel = new JLabel(Messages.getString("RandomMapDialog.labCity"));
        panel.add(cityTypeLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        cityTypeCombo.setToolTipText(Messages.getString("RandomMapDialog.cityTypeCombo.toolTip"));
        panel.add(cityTypeCombo, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel fortifiedLabel = new JLabel(Messages.getString("RandomMapDialog.labFortified"));
        panel.add(fortifiedLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        fortifiedCombo.setToolTipText(Messages.getString("RandomMapDialog.fortifiedCombo.toolTip"));
        panel.add(fortifiedCombo, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel pavementLabel = new JLabel(Messages.getString("RandomMapDialog.labPavement"));
        panel.add(pavementLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        pavementCombo.setToolTipText(Messages.getString("RandomMapDialog.pavementCombo.toolTip"));
        panel.add(pavementCombo, constraints);

        // Row 4, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel planetedFieldsLabel = new JLabel(Messages.getString("RandomMapDialog.labPlantedField"));
        panel.add(planetedFieldsLabel, constraints);

        // Row 4, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        plantedFieldsCombo.setToolTipText(Messages.getString("RandomMapDialog.plantedFieldsCombo.toolTip"));
        panel.add(plantedFieldsCombo, constraints);

        // Row 5, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel roadsLabel = new JLabel(Messages.getString("RandomMapDialog.labRoads"));
        panel.add(roadsLabel, constraints);

        // Row 5, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        roadsCombo.setToolTipText(Messages.getString("RandomMapDialog.roadsCombo.toolTip"));
        panel.add(roadsCombo, constraints);

        // Row 6, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weighty = 1;
        constraints.weightx = 0;
        final JLabel rubbleLabel = new JLabel(Messages.getString("RandomMapDialog.labRubble"));
        panel.add(rubbleLabel, constraints);

        // Row 6, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        rubbleCombo.setToolTipText(Messages.getString("RandomMapDialog.rubbleCombo.toolTip"));
        panel.add(rubbleCombo, constraints);

        return new JScrollPane(panel);
    }

    public MapSettings getMapSettings() {
        MapSettings newMapSettings = MapSettings.getInstance(mapSettings);
        String value;
        boolean anyChanges = false;

        if (hillinessCombo.hasChanged()) {
            value = (String) hillinessCombo.getSelectedItem();
            setupHills(value, newMapSettings);
            anyChanges = true;
        }

        if (cliffsCombo.hasChanged()) {
            value = (String) cliffsCombo.getSelectedItem();
            newMapSettings.setCliffParam(rangeToPercentage(value));
            anyChanges = true;
        }

        if (woodsCombo.hasChanged()) {
            value = (String) woodsCombo.getSelectedItem();
            setupWoods(value, newMapSettings);
            anyChanges = true;
        }

        if (lakesCombo.hasChanged()) {
            value = (String) lakesCombo.getSelectedItem();
            setupLakes(value, newMapSettings);
            anyChanges = true;
        }

        if (roughsCombo.hasChanged()) {
            value = (String) roughsCombo.getSelectedItem();
            setupRoughs(value, newMapSettings);
            anyChanges = true;
        }

        if (sandsCombo.hasChanged()) {
            value = (String) sandsCombo.getSelectedItem();
            setupSand(value, newMapSettings);
            anyChanges = true;
        }

        if (plantedFieldsCombo.hasChanged()) {
            value = (String) plantedFieldsCombo.getSelectedItem();
            setupPlantedFields(value, newMapSettings);
            anyChanges = true;
        }

        if (swampsCombo.hasChanged()) {
            value = (String) swampsCombo.getSelectedItem();
            setupSwamps(value, newMapSettings);
            anyChanges = true;
        }

        if (pavementCombo.hasChanged()) {
            value = (String) pavementCombo.getSelectedItem();
            setupPavement(value, newMapSettings);
            anyChanges = true;
        }

        if (rubbleCombo.hasChanged()) {
            value = (String) rubbleCombo.getSelectedItem();
            setupRubble(value, newMapSettings);
            anyChanges = true;
        }

        if (fortifiedCombo.hasChanged()) {
            value = (String) fortifiedCombo.getSelectedItem();
            setupFortified(value, newMapSettings);
            anyChanges = true;
        }

        if (iceCombo.hasChanged()) {
            value = (String) iceCombo.getSelectedItem();
            setupIce(value, newMapSettings);
            anyChanges = true;
        }

        if (roadsCombo.hasChanged()) {
            value = (String) roadsCombo.getSelectedItem();
            newMapSettings.setRoadParam(rangeToPercentage(value));
            anyChanges = true;
        }

        if (riversCombo.hasChanged()) {
            value = (String) riversCombo.getSelectedItem();
            newMapSettings.setRiverParam(rangeToPercentage(value));
            anyChanges = true;
        }

        if (cratersCombo.hasChanged()) {
            value = (String) cratersCombo.getSelectedItem();
            setupCraters(value, newMapSettings);
            anyChanges = true;
        }

        if (mountainCombo.hasChanged()) {
            value = (String) mountainCombo.getSelectedItem();
            setupMountains(value, newMapSettings);
            anyChanges = true;
        }

        if (cityTypeCombo.hasChanged()) {
            value = (String) cityTypeCombo.getSelectedItem();
            setupCity(value, newMapSettings);
            anyChanges = true;
        }

        if (anyChanges) {
            newMapSettings.setAlgorithmToUse(0);
            newMapSettings.setSpecialFX(0, 0, 0, 0, 0);
            newMapSettings.setInvertNegativeTerrain(0);
            return newMapSettings;
        }

        return mapSettings;
    }

    private void setupCity(String cityType, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(cityType)) {
            mapSettings.setCityParams(0, cityType, 0, 0, 0, 0, 0, 0);
        } else if (NONE.equalsIgnoreCase(cityType)) {
            mapSettings.setCityParams(3, cityType, 10, 50, 1, 3, 75, 60);
        } else {
            mapSettings.setCityParams(16, cityType, 10, 100, 1, 6, 75, 60);
        }
    }

    private void setupMountains(String mountainsValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(mountainsValue)) {
            mapSettings.setMountainParams(0, 7, 10, 4, 6, 0);
        } else if (LOW.equalsIgnoreCase(mountainsValue)) {
            mapSettings.setMountainParams(1, 7, 10, 4, 6, 0);
        } else if (MEDIUM.equalsIgnoreCase(mountainsValue)) {
            mapSettings.setMountainParams(2, 7, 10, 6, 8, 0);
        } else {
            mapSettings.setMountainParams(3, 9, 14, 8, 10, MapSettings.MOUNTAIN_SNOWCAPPED);
        }
    }

    private void setupCraters(String cratersValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(cratersValue)) {
            mapSettings.setCraterParam(0, 0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(cratersValue)) {
            mapSettings.setCraterParam(25, 1, 3, 1, 3);
        } else if (MEDIUM.equalsIgnoreCase(cratersValue)) {
            mapSettings.setCraterParam(50, 3, 8, 1, 5);
        } else {
            mapSettings.setCraterParam(75, 6, 14, 3, 9);
        }
    }

    private void setupIce(String iceValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(iceValue)) {
            mapSettings.setIceParams(0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(iceValue)) {
            mapSettings.setIceParams(2, 6, 1, 2);
        } else if (MEDIUM.equalsIgnoreCase(iceValue)) {
            mapSettings.setIceParams(2, 5, 3, 8);
        } else {
            mapSettings.setIceParams(5, 10, 3, 7);
        }
    }

    private void setupFortified(String fortifiedValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(fortifiedValue)) {
            mapSettings.setFortifiedParams(0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(fortifiedValue)) {
            mapSettings.setFortifiedParams(1, 2, 1, 2);
        } else if (MEDIUM.equalsIgnoreCase(fortifiedValue)) {
            mapSettings.setFortifiedParams(2, 4, 2, 3);
        } else {
            mapSettings.setFortifiedParams(2, 4, 3, 6);
        }
    }

    private void setupRubble(String rubbleValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(rubbleValue)) {
            mapSettings.setRubbleParams(0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(rubbleValue)) {
            mapSettings.setRubbleParams(2, 6, 1, 2);
        } else if (MEDIUM.equalsIgnoreCase(rubbleValue)) {
            mapSettings.setRubbleParams(3, 8, 2, 5);
        } else {
            mapSettings.setRubbleParams(5, 10, 3, 7);
        }
    }

    private void setupPavement(String pavementValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(pavementValue)) {
            mapSettings.setPavementParams(0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(pavementValue)) {
            mapSettings.setPavementParams(2, 6, 1, 2);
        } else if (MEDIUM.equalsIgnoreCase(pavementValue)) {
            mapSettings.setPavementParams(3, 8, 2, 5);
        } else {
            mapSettings.setPavementParams(5, 10, 3, 7);
        }
    }

    private void setupSwamps(String swampsValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(swampsValue)) {
            mapSettings.setSwampParams(0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(swampsValue)) {
            mapSettings.setSwampParams(2, 6, 1, 2);
        } else if (MEDIUM.equalsIgnoreCase(swampsValue)) {
            mapSettings.setSwampParams(3, 8, 2, 5);
        } else {
            mapSettings.setSwampParams(5, 10, 3, 7);
        }
    }

    private void setupPlantedFields(String fieldsValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(fieldsValue)) {
            mapSettings.setPlantedFieldParams(0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(fieldsValue)) {
            mapSettings.setPlantedFieldParams(2, 6, 1, 2);
        } else if (MEDIUM.equalsIgnoreCase(fieldsValue)) {
            mapSettings.setPlantedFieldParams(3, 8, 2, 5);
        } else {
            mapSettings.setPlantedFieldParams(5, 10, 3, 7);
        }
    }

    private void setupSand(String sandsValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(sandsValue)) {
            mapSettings.setSandParams(0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(sandsValue)) {
            mapSettings.setSandParams(2, 6, 1, 2);
        } else if (MEDIUM.equalsIgnoreCase(sandsValue)) {
            mapSettings.setSandParams(3, 8, 2, 5);
        } else {
            mapSettings.setSandParams(5, 10, 3, 7);
        }
    }

    private void setupRoughs(String roughsValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(roughsValue)) {
            mapSettings.setRoughParams(0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(roughsValue)) {
            mapSettings.setRoughParams(2, 6, 1, 2);
        } else if (MEDIUM.equalsIgnoreCase(roughsValue)) {
            mapSettings.setRoughParams(3, 8, 2, 5);
        } else {
            mapSettings.setRoughParams(5, 10, 3, 7);
        }
    }

    private void setupLakes(String lakesValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(lakesValue)) {
            mapSettings.setWaterParams(0, 0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(lakesValue)) {
            mapSettings.setWaterParams(1, 5, 1, 5, 20);
        } else if (MEDIUM.equalsIgnoreCase(lakesValue)) {
            mapSettings.setWaterParams(2, 5, 6, 10, 30);
        } else {
            mapSettings.setWaterParams(3, 6, 8, 15, 45);
        }
    }

    private void setupWoods(String woodsValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(woodsValue)) {
            mapSettings.setForestParams(0, 0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(woodsValue)) {
            mapSettings.setForestParams(3, 6, 3, 6, 20);
        } else if (MEDIUM.equalsIgnoreCase(woodsValue)) {
            mapSettings.setForestParams(4, 8, 3, 10, 30);
        } else {
            mapSettings.setForestParams(6, 10, 8, 13, 45);
        }
    }

    private void setupHills(String hillsValue, MapSettings mapSettings) {
        int percent = rangeToPercentage(hillsValue);
        int range = 0;
        if (LOW.equalsIgnoreCase(hillsValue)) {
            range = 3;
        } else if (MEDIUM.equalsIgnoreCase(hillsValue)) {
            range = 5;
        } else if (HIGH.equalsIgnoreCase(hillsValue)) {
            range = 8;
        }
        mapSettings.setElevationParams(percent, range, 0);
    }

    private int rangeToPercentage(String range) {
        if (NONE.equalsIgnoreCase(range)) {
            return 0;
        }
        if (LOW.equals(range)) {
            return 25;
        }
        if (MEDIUM.equals(range)) {
            return 50;
        }
        return 75;
    }
}
