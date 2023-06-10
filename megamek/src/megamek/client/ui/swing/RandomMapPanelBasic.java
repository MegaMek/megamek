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
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 3/13/14 3:55 PM
 */
public class RandomMapPanelBasic extends JPanel {

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
    private final CheckpointComboBox<String> jungleCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> foliageCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> snowCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);
    private final CheckpointComboBox<String> tundraCombo = new CheckpointComboBox<>(LOW_HIGH_CHOICES);

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

        jungleCombo.setSelectedItem(woodsToRange(this.mapSettings.getMinJungleSize(),
                this.mapSettings.getMinJungleSpots(),
                this.mapSettings.getProbHeavyJungle()));
        jungleCombo.checkpoint();

        foliageCombo.setSelectedItem(woodsToRange(this.mapSettings.getMinFoliageSize(),
                this.mapSettings.getMinFoliageSpots(),
                this.mapSettings.getProbFoliageHeavy()));
        foliageCombo.checkpoint();

        snowCombo.setSelectedItem(sandsToRange(this.mapSettings.getMinSnowSize(),
                this.mapSettings.getMinSnowSpots()));
        snowCombo.checkpoint();

        tundraCombo.setSelectedItem(sandsToRange(this.mapSettings.getMinTundraSize(),
                this.mapSettings.getMinTundraSpots()));
        tundraCombo.checkpoint();

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

        JPanel panel = new JPanel(new SpringLayout());

        final JLabel lakesLabel = new JLabel(Messages.getString("RandomMapDialog.labLakes"));
        panel.add(lakesLabel);
        lakesCombo.setToolTipText(Messages.getString("RandomMapDialog.lakesCombo.toolTip"));
        panel.add(lakesCombo);

        final JLabel riversLabel = new JLabel(Messages.getString("RandomMapDialog.labRivers"));
        panel.add(riversLabel);
        riversCombo.setToolTipText(Messages.getString("RandomMapDialog.riversCombo.toolTip"));
        panel.add(riversCombo);
        
        final JLabel iceLabel = new JLabel(Messages.getString("RandomMapDialog.labIce"));
        panel.add(iceLabel);
        iceCombo.setToolTipText(Messages.getString("RandomMapDialog.iceCombo.toolTip"));
        panel.add(iceCombo);

        makeCompactGrid(panel, 3, 2, 6, 6, 6, 6);
        return new JScrollPane(panel);
    }
    

    private String iceToRange(int minSize, int minSpots) {
        return convert(minSize + minSpots, 2, 4, 6);
    }

    private String lakesToRange(int minSize, int minSpots, int percentDeep) {
        int range = percentDeep + (minSize * 5) + (minSpots * 10);
        return convert(range, 20, 65, 100);
    }
    
    private String convert(int value, int low, int med, int high) {
        if (value >= high) {
            return HIGH;
        } else if (value >= med) {
            return MEDIUM;
        } else if (value >= low) {
            return LOW;
        }
        return NONE;
    }

    private JScrollPane setupNaturalFeaturesPanel() {
        JPanel panel = new JPanel(new SpringLayout());
        
        final JLabel roughsLabel = new JLabel(Messages.getString("RandomMapDialog.labRough"));
        panel.add(roughsLabel);
        roughsCombo.setToolTipText(Messages.getString("RandomMapDialog.roughsCombo.toolTip"));
        panel.add(roughsCombo);

        final JLabel sandsLabel = new JLabel(Messages.getString(("RandomMapDialog.labSand")));
        panel.add(sandsLabel);
        sandsCombo.setToolTipText(Messages.getString("RandomMapDialog.sandsCombo.toolTip"));
        panel.add(sandsCombo);

        final JLabel swampsLabel = new JLabel(Messages.getString("RandomMapDialog.labSwamp"));
        panel.add(swampsLabel);
        swampsCombo.setToolTipText(Messages.getString("RandomMapDialog.swampsCombo.toolTip"));
        panel.add(swampsCombo);

        final JLabel woodsLabel = new JLabel(Messages.getString("RandomMapDialog.labWoods"));
        panel.add(woodsLabel);
        woodsCombo.setToolTipText(Messages.getString("RandomMapDialog.woodsCombo.toolTip"));
        panel.add(woodsCombo);

        final JLabel jungleLabel = new JLabel(Messages.getString("RandomMapDialog.labJungle"));
        panel.add(jungleLabel);
        jungleCombo.setToolTipText(Messages.getString("RandomMapDialog.jungleCombo.toolTip"));
        panel.add(jungleCombo);

        final JLabel foliageLabel = new JLabel(Messages.getString("RandomMapDialog.labFoliage"));
        panel.add(foliageLabel);
        foliageCombo.setToolTipText(Messages.getString("RandomMapDialog.foliageCombo.toolTip"));
        panel.add(foliageCombo);

        final JLabel snowLabel = new JLabel(Messages.getString(("RandomMapDialog.labSnow")));
        panel.add(snowLabel);
        snowCombo.setToolTipText(Messages.getString("RandomMapDialog.snowCombo.toolTip"));
        panel.add(snowCombo);

        final JLabel tundraLabel = new JLabel(Messages.getString(("RandomMapDialog.labTundra")));
        panel.add(tundraLabel);
        tundraCombo.setToolTipText(Messages.getString("RandomMapDialog.tundraCombo.toolTip"));
        panel.add(tundraCombo);
        
        makeCompactGrid(panel, 8, 2, 6, 6, 6, 6);
        return new JScrollPane(panel);
    }

    private String roughsToRange(int minSize, int minSpots) {
        return convert(minSize + minSpots, 2, 4, 6);
    }

    private String sandsToRange(int minSize, int minSpots) {
        return convert(minSize + minSpots, 2, 4, 6);
    }

    private String swampsToRange(int minSize, int minSpots) {
        return convert(minSize + minSpots, 2, 4, 6);
    }

    private String woodsToRange(int minSize, int minSpots, int percentHeavy) {
        return convert(percentHeavy + (minSize * 5) + (minSpots * 6), 50, 65, 100);
    }

    private JScrollPane setupElevationPanel() {
        JPanel panel = new JPanel(new SpringLayout());

        final JLabel cliffsLabel = new JLabel(Messages.getString("RandomMapDialog.labCliffs"));
        panel.add(cliffsLabel);
        cliffsCombo.setToolTipText(Messages.getString("RandomMapDialog.cliffsCombo.toolTip"));
        panel.add(cliffsCombo);

        final JLabel cratersLabel = new JLabel(Messages.getString("RandomMapDialog.labCraters"));
        panel.add(cratersLabel);
        cratersCombo.setToolTipText(Messages.getString("RandomMapDialog.cratersCombo.toolTip"));
        panel.add(cratersCombo);

        final JLabel hillinessLabel = new JLabel(Messages.getString("RandomMapDialog.labElevation"));
        panel.add(hillinessLabel);
        hillinessCombo.setToolTipText(Messages.getString("RandomMapDialog.hillinessCombo.toolTip"));
        panel.add(hillinessCombo);

        final JLabel mountainLabel = new JLabel(Messages.getString("RandomMapDialog.labMountain"));
        panel.add(mountainLabel);
        mountainCombo.setToolTipText(Messages.getString("RandomMapDialog.mountainCombo.toolTip"));
        panel.add(mountainCombo);
        
        makeCompactGrid(panel, 4, 2, 6, 6, 6, 6);
        return new JScrollPane(panel);
    }

    private String cratersToRange(int craterProbability) {
        return convert(craterProbability, 15, 40, 60);
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
        return convert(value, 15, 40, 60);
    }

    private String fortifiedToRange(int minSize, int minSpots) {
        return convert(minSize + minSpots, 2, 3, 5);
    }

    private String pavementToRange(int minSize, int minSpots) {
        return convert(minSize + minSpots, 2, 4, 6);
    }

    private String plantedFieldsToRange(int minSize, int minSpots) {
        return convert(minSize + minSpots, 2, 4, 6);
    }

    private String rubbleToRange(int minSize, int minSpots) {
        return convert(minSize + minSpots, 2, 4, 6);
    }

    private JScrollPane setupCivilizationPanel() {
        JPanel panel = new JPanel(new SpringLayout());
        
        final JLabel cityTypeLabel = new JLabel(Messages.getString("RandomMapDialog.labCity"));
        panel.add(cityTypeLabel);
        cityTypeCombo.setToolTipText(Messages.getString("RandomMapDialog.cityTypeCombo.toolTip"));
        panel.add(cityTypeCombo);

        final JLabel fortifiedLabel = new JLabel(Messages.getString("RandomMapDialog.labFortified"));
        panel.add(fortifiedLabel);
        fortifiedCombo.setToolTipText(Messages.getString("RandomMapDialog.fortifiedCombo.toolTip"));
        panel.add(fortifiedCombo);

        final JLabel pavementLabel = new JLabel(Messages.getString("RandomMapDialog.labPavement"));
        panel.add(pavementLabel);
        pavementCombo.setToolTipText(Messages.getString("RandomMapDialog.pavementCombo.toolTip"));
        panel.add(pavementCombo);

        final JLabel plantedFieldsLabel = new JLabel(Messages.getString("RandomMapDialog.labPlantedField"));
        panel.add(plantedFieldsLabel);
        plantedFieldsCombo.setToolTipText(Messages.getString("RandomMapDialog.plantedFieldsCombo.toolTip"));
        panel.add(plantedFieldsCombo);

        final JLabel roadsLabel = new JLabel(Messages.getString("RandomMapDialog.labRoads"));
        panel.add(roadsLabel);
        roadsCombo.setToolTipText(Messages.getString("RandomMapDialog.roadsCombo.toolTip"));
        panel.add(roadsCombo);

        final JLabel rubbleLabel = new JLabel(Messages.getString("RandomMapDialog.labRubble"));
        panel.add(rubbleLabel);
        rubbleCombo.setToolTipText(Messages.getString("RandomMapDialog.rubbleCombo.toolTip"));
        panel.add(rubbleCombo);
        
        makeCompactGrid(panel, 6, 2, 6, 6, 6, 6);
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

        if (jungleCombo.hasChanged()) {
            value = (String) jungleCombo.getSelectedItem();
            setupJungle(value, newMapSettings);
            anyChanges = true;
        }
        
        if (foliageCombo.hasChanged()) {
            value = (String) foliageCombo.getSelectedItem();
            setupFoliage(value, newMapSettings);
            anyChanges = true;
        }

        if (snowCombo.hasChanged()) {
            value = (String) snowCombo.getSelectedItem();
            setupSnow(value, newMapSettings);
            anyChanges = true;
        }

        if (tundraCombo.hasChanged()) {
            value = (String) tundraCombo.getSelectedItem();
            setupTundra(value, newMapSettings);
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

    private void setupSnow(String snowsValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(snowsValue)) {
            mapSettings.setSnowParams(0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(snowsValue)) {
            mapSettings.setSnowParams(2, 6, 1, 2);
        } else if (MEDIUM.equalsIgnoreCase(snowsValue)) {
            mapSettings.setSnowParams(3, 8, 2, 5);
        } else {
            mapSettings.setSnowParams(5, 10, 3, 7);
        }
    }

    private void setupTundra(String snowsValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(snowsValue)) {
            mapSettings.setTundraParams(0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(snowsValue)) {
            mapSettings.setTundraParams(2, 6, 1, 2);
        } else if (MEDIUM.equalsIgnoreCase(snowsValue)) {
            mapSettings.setTundraParams(3, 8, 2, 5);
        } else {
            mapSettings.setTundraParams(5, 10, 3, 7);
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
            mapSettings.setForestParams(0, 0, 0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(woodsValue)) {
            mapSettings.setForestParams(3, 6, 3, 6, 20, 0);
        } else if (MEDIUM.equalsIgnoreCase(woodsValue)) {
            mapSettings.setForestParams(4, 8, 3, 10, 30, 0);
        } else {
            mapSettings.setForestParams(6, 10, 8, 13, 45, 5);
        }
    }

    private void setupJungle(String jungleValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(jungleValue)) {
            mapSettings.setJungleParams(0, 0, 0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(jungleValue)) {
            mapSettings.setJungleParams(3, 6, 3, 6, 20, 0);
        } else if (MEDIUM.equalsIgnoreCase(jungleValue)) {
            mapSettings.setJungleParams(4, 8, 3, 10, 30, 0);
        } else {
            mapSettings.setJungleParams(6, 10, 8, 13, 45, 5);
        }
    }
    
    private void setupFoliage(String foliageValue, MapSettings mapSettings) {
        if (NONE.equalsIgnoreCase(foliageValue)) {
            mapSettings.setFoliageParams(0, 0, 0, 0, 0);
        } else if (LOW.equalsIgnoreCase(foliageValue)) {
            mapSettings.setFoliageParams(3, 6, 3, 6, 20);
        } else if (MEDIUM.equalsIgnoreCase(foliageValue)) {
            mapSettings.setFoliageParams(4, 8, 3, 10, 30);
        } else {
            mapSettings.setFoliageParams(6, 10, 8, 13, 45);
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
    
    /**
     * From https://docs.oracle.com/javase/tutorial/uiswing/examples/layout/SpringGridProject/src/layout/SpringUtilities.java
     * 
     * Aligns the first <code>rows</code> * <code>cols</code>
     * components of <code>parent</code> in
     * a grid. Each component in a column is as wide as the maximum
     * preferred width of the components in that column;
     * height is similarly determined for each row.
     * The parent is made just big enough to fit them all.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param initialX x location to start the grid at
     * @param initialY y location to start the grid at
     * @param xPad x padding between cells
     * @param yPad y padding between cells
     */
    public static void makeCompactGrid(Container parent,
            int rows, int cols,
            int initialX, int initialY,
            int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout) parent.getLayout();
        } catch (Exception ex) {
            LogManager.getLogger().error("The first argument to makeCompactGrid must use SpringLayout.", ex);
            return;
        }

        // Align all cells in each column and make them the same width.
        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width,
                        getConstraintsForCell(r, c, parent, cols).
                        getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints =
                        getConstraintsForCell(r, c, parent, cols);
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }

        //Align all cells in each row and make them the same height.
        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height,
                        getConstraintsForCell(r, c, parent, cols).
                        getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints =
                        getConstraintsForCell(r, c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }

        //Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH, y);
        pCons.setConstraint(SpringLayout.EAST, x);
    }

    private static SpringLayout.Constraints getConstraintsForCell(
            int row, int col,
            Container parent,
            int cols) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }


}
