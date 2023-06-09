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
import megamek.client.ui.swing.util.VerifyInRange;
import megamek.client.ui.swing.util.VerifyIsInteger;
import megamek.client.ui.swing.util.VerifyIsPositiveInteger;
import megamek.client.ui.swing.widget.VerifiableTextField;
import megamek.common.MapSettings;
import megamek.common.annotations.Nullable;
import megamek.common.util.BoardUtilities;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 3/13/14 3:57 PM
 */
public class RandomMapPanelAdvanced extends JPanel {
    private static final long serialVersionUID = 7904626306929132645L;

    // City Type constants.
    private static final String CT_NONE = "None";
    private static final String CT_HUB = "HUB";
    private static final String CT_GRID = "GRID";
    private static final String CT_METRO = "METRO";
    private static final String CT_TOWN = "TOWN";
    private static final String[] CT_CHOICES = new String[]{CT_NONE, CT_GRID, CT_HUB, CT_METRO, CT_TOWN};

    // Using tabs to keep the data organized.
    private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);

    // The settings for the map to be generated.
    private MapSettings mapSettings;

    // Elevation
    private final VerifiableTextField elevationAlgorithmField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, BoardUtilities.getAmountElevationGenerators() - 1, true));
    private final VerifiableTextField hillinessField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 99, true));
    private final VerifiableTextField elevationRangeField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField elevationCliffsField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));
    private final VerifiableTextField elevationInversionField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 99, true));
    private final JCheckBox invertNegativeCheck = new JCheckBox();
    private final VerifiableTextField elevationPeaksField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final JComboBox<String> mountainStyleCombo = new JComboBox<>(MountainStyle.getStyleDescriptions());
    private final VerifiableTextField mountainHeightMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField mountainHeightMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField mountainWidthMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField mountainWidthMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField craterChanceField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));
    private final VerifiableTextField craterAmountMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField craterAmountMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField craterSizeMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField craterSizeMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());

    // Natural Features
    private final VerifiableTextField roughsMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField roughsMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField roughsMinSizeField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField roughsMaxSizeField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField sandsMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField sandsMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField sandsSizeMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField sandsSizeMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField swampsMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField swampsMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField swampsMinSizeField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField swampsMaxSizeField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField woodsMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField woodsMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField woodsMinSizeField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField woodsMaxSizeField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField woodsHeavyChanceField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));

    private final VerifiableTextField woodsUltraChanceField =
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));
    private final VerifiableTextField foliageMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField foliageMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField foliageMinSizeField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField foliageMaxSizeField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField foliageHeavyChanceField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));

    private final VerifiableTextField snowMinField =
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField snowMaxField =
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField snowSizeMinField =
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField snowSizeMaxField =
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());

    // Civilized Features.
    private final VerifiableTextField fieldsMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField fieldsMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField fieldSizeMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField fieldSizeMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField fortifiedMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField fortifiedMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField fortifiedSizeMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField fortifiedSizeMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField pavementMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField pavementMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField pavementSizeMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField pavementSizeMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField roadChanceField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));
    private final VerifiableTextField rubbleMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField rubbleMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField rubbleSizeMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField rubbleSizeMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final JComboBox<String> cityTypeCombo = new JComboBox<>(CT_CHOICES);
    private final VerifiableTextField cityBlocks = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField cityCFMinField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(1, 150, true));
    private final VerifiableTextField cityCFMaxField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(1, 150, true));
    private final VerifiableTextField cityFloorsMinField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(1, 100, true));
    private final VerifiableTextField cityFloorsMaxField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(1, 100, true));
    private final VerifiableTextField cityDensityField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(1, 100, true));
    private final VerifiableTextField townSizeField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(1, 100, true));

    // Water
    private final VerifiableTextField lakesMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField lakesMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField lakeSizeMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField lakeSizeMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField deepChanceField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));
    private final VerifiableTextField riverChanceField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));
    private final VerifiableTextField iceMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField iceMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField iceSizeMinField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField iceSizeMaxField = 
            new VerifiableTextField(4, true, true, new VerifyIsPositiveInteger());
    private final VerifiableTextField freezeChanceField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));

    // Special effects
    private final VerifiableTextField droughtChanceField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));
    private final VerifiableTextField floodChanceField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));
    private final VerifiableTextField fireChanceField = 
            new VerifiableTextField(4, true, true, new VerifyInRange(0, 100, true));
    private final VerifiableTextField specialFxField = 
            new VerifiableTextField(4, true, true, new VerifyIsInteger());

    /**
     * Constructor for the advanced map settings panel.  This gives more detailed control over how the map will be
     * generated.
     *
     * @param mapSettings The settings for the map to be generated.
     */
    public RandomMapPanelAdvanced(MapSettings mapSettings) {
        setMapSettings(mapSettings);

        initGUI();
        validate();
    }

    /**
     * Loads the given map settings onto this form.
     *
     * @param mapSettings The new map settings to be loaded.
     */
    public void setMapSettings(MapSettings mapSettings) {
        if (mapSettings == null) {
            return;
        }
        this.mapSettings = mapSettings;
        loadMapSettings();
    }

    /**
     * Update the panel controls with the values from the {@link MapSettings} object.
     */
    private void loadMapSettings() {
        riverChanceField.setText(String.valueOf(mapSettings.getProbRiver()));
        iceMinField.setText(String.valueOf(mapSettings.getMinIceSpots()));
        iceMaxField.setText(String.valueOf(mapSettings.getMaxIceSpots()));
        iceSizeMinField.setText(String.valueOf(mapSettings.getMinIceSize()));
        iceSizeMaxField.setText(String.valueOf(mapSettings.getMaxIceSize()));
        freezeChanceField.setText(String.valueOf(mapSettings.getProbFreeze()));
        lakesMinField.setText(String.valueOf(mapSettings.getMinWaterSpots()));
        lakesMaxField.setText(String.valueOf(mapSettings.getMaxWaterSpots()));
        lakeSizeMinField.setText(String.valueOf(mapSettings.getMinWaterSize()));
        lakeSizeMaxField.setText(String.valueOf(mapSettings.getMaxWaterSize()));
        deepChanceField.setText(String.valueOf(mapSettings.getProbDeep()));
        fieldsMinField.setText(String.valueOf(mapSettings.getMinPlantedFieldSpots()));
        fieldsMaxField.setText(String.valueOf(mapSettings.getMaxPlantedFieldSpots()));
        fieldSizeMinField.setText(String.valueOf(mapSettings.getMinPlantedFieldSize()));
        fieldSizeMaxField.setText(String.valueOf(mapSettings.getMaxPlantedFieldSize()));
        rubbleMinField.setText(String.valueOf(mapSettings.getMinRubbleSpots()));
        rubbleMaxField.setText(String.valueOf(mapSettings.getMaxRubbleSpots()));
        rubbleSizeMinField.setText(String.valueOf(mapSettings.getMinRubbleSize()));
        rubbleSizeMaxField.setText(String.valueOf(mapSettings.getMaxRubbleSize()));
        roadChanceField.setText(String.valueOf(mapSettings.getProbRoad()));
        pavementMinField.setText(String.valueOf(mapSettings.getMinPavementSpots()));
        pavementMaxField.setText(String.valueOf(mapSettings.getMaxPavementSpots()));
        pavementSizeMinField.setText(String.valueOf(mapSettings.getMinPavementSize()));
        pavementSizeMaxField.setText(String.valueOf(mapSettings.getMaxPavementSize()));
        fortifiedMinField.setText(String.valueOf(mapSettings.getMinFortifiedSpots()));
        fortifiedMaxField.setText(String.valueOf(mapSettings.getMaxFortifiedSpots()));
        fortifiedSizeMinField.setText(String.valueOf(mapSettings.getMinFortifiedSize()));
        fortifiedSizeMaxField.setText(String.valueOf(mapSettings.getMaxFortifiedSize()));
        cityTypeCombo.setSelectedItem(mapSettings.getCityType());
        cityBlocks.setText(String.valueOf(mapSettings.getCityBlocks()));
        cityCFMinField.setText(String.valueOf(mapSettings.getCityMinCF()));
        cityCFMaxField.setText(String.valueOf(mapSettings.getCityMaxCF()));
        cityDensityField.setText(String.valueOf(mapSettings.getCityDensity()));
        cityFloorsMinField.setText(String.valueOf(mapSettings.getCityMinFloors()));
        cityFloorsMaxField.setText(String.valueOf(mapSettings.getCityMaxFloors()));
        townSizeField.setText(String.valueOf(mapSettings.getTownSize()));
        droughtChanceField.setText(String.valueOf(mapSettings.getProbDrought()));
        fireChanceField.setText(String.valueOf(mapSettings.getProbForestFire()));
        floodChanceField.setText(String.valueOf(mapSettings.getProbFlood()));
        specialFxField.setText(String.valueOf(mapSettings.getFxMod()));
        woodsMinField.setText(String.valueOf(mapSettings.getMinForestSpots()));
        woodsMaxField.setText(String.valueOf(mapSettings.getMaxForestSpots()));
        woodsMinSizeField.setText(String.valueOf(mapSettings.getMinForestSize()));
        woodsMaxSizeField.setText(String.valueOf(mapSettings.getMaxForestSize()));
        woodsHeavyChanceField.setText(String.valueOf(mapSettings.getProbHeavy()));
        woodsUltraChanceField.setText(String.valueOf(mapSettings.getProbUltra()));
        foliageMinField.setText(String.valueOf(mapSettings.getMinFoliageSpots()));
        foliageMaxField.setText(String.valueOf(mapSettings.getMaxFoliageSpots()));
        foliageMinSizeField.setText(String.valueOf(mapSettings.getMinFoliageSize()));
        foliageMaxSizeField.setText(String.valueOf(mapSettings.getMaxFoliageSize()));
        foliageHeavyChanceField.setText(String.valueOf(mapSettings.getProbFoliageHeavy()));
        swampsMinField.setText(String.valueOf(mapSettings.getMinSwampSpots()));
        swampsMaxField.setText(String.valueOf(mapSettings.getMaxSwampSpots()));
        swampsMinSizeField.setText(String.valueOf(mapSettings.getMinSwampSize()));
        swampsMaxSizeField.setText(String.valueOf(mapSettings.getMaxSwampSize()));
        sandsMinField.setText(String.valueOf(mapSettings.getMinSandSpots()));
        sandsMaxField.setText(String.valueOf(mapSettings.getMaxSandSpots()));
        sandsSizeMinField.setText(String.valueOf(mapSettings.getMinSandSize()));
        sandsSizeMaxField.setText(String.valueOf(mapSettings.getMaxSandSize()));
        snowMinField.setText(String.valueOf(mapSettings.getMinSnowSpots()));
        snowMaxField.setText(String.valueOf(mapSettings.getMaxSnowSpots()));
        snowSizeMinField.setText(String.valueOf(mapSettings.getMinSnowSize()));
        snowSizeMaxField.setText(String.valueOf(mapSettings.getMaxSnowSize()));
        roughsMinField.setText(String.valueOf(mapSettings.getMinRoughSpots()));
        roughsMaxField.setText(String.valueOf(mapSettings.getMaxRoughSpots()));
        roughsMinSizeField.setText(String.valueOf(mapSettings.getMinRoughSize()));
        roughsMaxSizeField.setText(String.valueOf(mapSettings.getMaxRoughSize()));
        craterChanceField.setText(String.valueOf(mapSettings.getProbCrater()));
        craterAmountMinField.setText(String.valueOf(mapSettings.getMinCraters()));
        craterAmountMaxField.setText(String.valueOf(mapSettings.getMaxCraters()));
        craterSizeMinField.setText(String.valueOf(mapSettings.getMinRadius()));
        craterSizeMaxField.setText(String.valueOf(mapSettings.getMaxRadius()));
        elevationPeaksField.setText(String.valueOf(mapSettings.getMountainPeaks()));
        MountainStyle style = MountainStyle.getMountainStyle(mapSettings.getMountainStyle());
        mountainStyleCombo.setSelectedItem(style.getDescription());
        mountainHeightMinField.setText(String.valueOf(mapSettings.getMountainHeightMin()));
        mountainHeightMaxField.setText(String.valueOf(mapSettings.getMountainHeightMax()));
        mountainWidthMinField.setText(String.valueOf(mapSettings.getMountainWidthMin()));
        mountainWidthMaxField.setText(String.valueOf(mapSettings.getMountainWidthMax()));
        elevationAlgorithmField.setText(String.valueOf(mapSettings.getAlgorithmToUse()));
        hillinessField.setText(String.valueOf(mapSettings.getHilliness()));
        elevationRangeField.setText(String.valueOf(mapSettings.getRange()));
        elevationCliffsField.setText(String.valueOf(mapSettings.getCliffs()));
        elevationInversionField.setText(String.valueOf(mapSettings.getProbInvert()));
        invertNegativeCheck.setSelected(mapSettings.getInvertNegativeTerrain() == 1);
    }

    private void initGUI() {
        // Let each tab have it's own scroll bar.

        JScrollPane civilizedFeaturesPanel = setupCivilizedPanel();
        civilizedFeaturesPanel.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabCivilization"), civilizedFeaturesPanel);

        JScrollPane elevationPanel = setupElevationPanel();
        elevationPanel.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabElevation"), elevationPanel);

        JScrollPane naturalFeaturesPanel = setupNaturalFeaturesPanel();
        naturalFeaturesPanel.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabNatural"), naturalFeaturesPanel);

        JScrollPane effectsPanel = setupEffectsPanel();
        effectsPanel.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabEffects"), effectsPanel);

        JScrollPane waterPanel = setupWaterPanel();
        waterPanel.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabWater"), waterPanel);

        tabbedPane.setSelectedComponent(civilizedFeaturesPanel);
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JScrollPane setupWaterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        panel.add(setupLakesPanel());
        panel.add(setupRiverPanel());
        panel.add(setupIcePanel());

        return new JScrollPane(panel);
    }

    private JPanel setupRiverPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());
        
        JLabel riverChanceLabel = new JLabel(Messages.getString("RandomMapDialog.labProbRiver"));
        panel.add(riverChanceLabel);
        riverChanceField.setToolTipText(Messages.getString("RandomMapDialog.riverChanceField.toolTip"));
        panel.add(riverChanceField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderRiver")));
        RandomMapPanelBasic.makeCompactGrid(panel, 1, 2, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupIcePanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel iceLabel = new JLabel(Messages.getString("RandomMapDialog.labIceSpots"));
        panel.add(iceLabel);
        iceMinField.setToolTipText(Messages.getString("RandomMapDialog.iceMinField.toolTip"));
        panel.add(iceMinField);

        JLabel iceToLabel = new JLabel(Messages.getString("to"));
        panel.add(iceToLabel);
        iceMaxField.setToolTipText(Messages.getString("RandomMapDialog.iceMaxField.toolTip"));
        panel.add(iceMaxField);

        JLabel iceSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labIceSize"));
        panel.add(iceSizeLabel);
        iceSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.iceSizeMinField.toolTip"));
        panel.add(iceSizeMinField);

        JLabel iceSizeToLabel = new JLabel(Messages.getString("to"));
        panel.add(iceSizeToLabel);
        iceSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.iceSizeMaxField.toolTip"));
        panel.add(iceSizeMaxField);

        JLabel frozenWaterLabel = new JLabel(Messages.getString("RandomMapDialog.labProbFreeze"));
        panel.add(frozenWaterLabel);
        freezeChanceField.setToolTipText(Messages.getString("RandomMapDialog.freezeChanceField.toolTip"));
        panel.add(freezeChanceField);
        panel.add(new JLabel());
        panel.add(new JLabel());

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderIce")));

        RandomMapPanelBasic.makeCompactGrid(panel, 3, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupLakesPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel lakesLabel = new JLabel(Messages.getString("RandomMapDialog.labWaterSpots"));
        panel.add(lakesLabel);
        lakesMinField.setToolTipText(Messages.getString("RandomMapDialog.lakesMinField.toolTip"));
        panel.add(lakesMinField);
        JLabel lakesToLabel = new JLabel(Messages.getString("to"));
        panel.add(lakesToLabel);
        lakesMaxField.setToolTipText(Messages.getString("RandomMapDialog.lakesMaxField.toolTip"));
        panel.add(lakesMaxField);

        JLabel lakeSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labWaterSize"));
        panel.add(lakeSizeLabel);
        lakeSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.lakeSizeMinField.toolTip"));
        panel.add(lakeSizeMinField);
        JLabel lakeSizeToLabel = new JLabel(Messages.getString("to"));
        panel.add(lakeSizeToLabel);
        lakeSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.lakeSizeMaxField.toolTip"));
        panel.add(lakeSizeMaxField);
        JLabel deepWaterLabel = new JLabel(Messages.getString("RandomMapDialog.labProbDeep"));
        panel.add(deepWaterLabel);

        deepChanceField.setToolTipText(Messages.getString("RandomMapDialog.deepChanceField.toolTip"));
        panel.add(deepChanceField);
        panel.add(new JLabel());
        panel.add(new JLabel());

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderLakes")));

        RandomMapPanelBasic.makeCompactGrid(panel, 3, 4, 6, 6, 6, 6);
        return panel;
    }

    private JScrollPane setupCivilizedPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        panel.add(setupCityPanel());
        panel.add(setupFortifiedPanel());
        panel.add(setupPavementPanel());
        panel.add(setupFieldsPanel());
        panel.add(setupRoadPanel());
        panel.add(setupRubblePanel());

        return new JScrollPane(panel);
    }

    private JPanel setupFieldsPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel fieldsSpotsLabel = new JLabel(Messages.getString("RandomMapDialog.labPlantedFieldSpots"));
        panel.add(fieldsSpotsLabel);
        fieldsMinField.setToolTipText(Messages.getString("RandomMapDialog.fieldsMinField.toolTip"));
        panel.add(fieldsMinField);
        JLabel fieldsSpotsToLabel = new JLabel(Messages.getString("to"));
        panel.add(fieldsSpotsToLabel);
        fieldsMaxField.setToolTipText(Messages.getString("RandomMapDialog.fieldsMaxField.toolTip"));
        panel.add(fieldsMaxField);

        JLabel fieldsSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labPlantedFieldSize"));
        panel.add(fieldsSizeLabel);
        fieldSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.fieldSizeMinField.toolTip"));
        panel.add(fieldSizeMinField);
        JLabel fieldSizeToLabel = new JLabel(Messages.getString("to"));
        panel.add(fieldSizeToLabel);
        fieldSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.fieldSizeMaxField.toolTip"));
        panel.add(fieldSizeMaxField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderFields")));

        RandomMapPanelBasic.makeCompactGrid(panel, 2, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupRubblePanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel rubbleSpotsLabel = new JLabel(Messages.getString("RandomMapDialog.labRubbleSpots"));
        panel.add(rubbleSpotsLabel);
        rubbleMinField.setToolTipText(Messages.getString("RandomMapDialog.rubbleMinField.toolTip"));
        panel.add(rubbleMinField);
        JLabel rubbleSpotsToLabel = new JLabel(Messages.getString("to"));
        panel.add(rubbleSpotsToLabel);
        rubbleMaxField.setToolTipText(Messages.getString("RandomMapDialog.rubbleMaxField.toolTip"));
        panel.add(rubbleMaxField);

        JLabel rubbleSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labRubbleSize"));
        panel.add(rubbleSizeLabel);
        rubbleSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.rubbleSizeMinField.toolTip"));
        panel.add(rubbleSizeMinField);
        JLabel rubbleSizeToLabel = new JLabel(Messages.getString("to"));
        panel.add(rubbleSizeToLabel);
        rubbleSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.rubbleSizeMaxField.toolTip"));
        panel.add(rubbleSizeMaxField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderRubble")));

        RandomMapPanelBasic.makeCompactGrid(panel, 2, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupRoadPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel roadChanceLabel = new JLabel(Messages.getString("RandomMapDialog.labProbRoad"));
        panel.add(roadChanceLabel);
        roadChanceField.setToolTipText(Messages.getString("RandomMapDialog.roadChanceField.toolTip"));
        panel.add(roadChanceField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderRoad")));
        RandomMapPanelBasic.makeCompactGrid(panel, 1, 2, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupPavementPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel pavementSpotsLabel = new JLabel(Messages.getString("RandomMapDialog.labPavementSpots"));
        panel.add(pavementSpotsLabel);
        pavementMinField.setToolTipText(Messages.getString("RandomMapDialog.pavementMinField.toolTip"));
        panel.add(pavementMinField);
        JLabel pavementSpotsToLabel = new JLabel(Messages.getString("to"));
        panel.add(pavementSpotsToLabel);
        pavementMaxField.setToolTipText(Messages.getString("RandomMapDialog.pavementMaxField.toolTip"));
        panel.add(pavementMaxField);

        JLabel pavementSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labPavementSize"));
        panel.add(pavementSizeLabel);
        pavementSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.pavementSizeMinField.toolTip"));
        panel.add(pavementSizeMinField);
        JLabel pavementSizeToLabel = new JLabel(Messages.getString("to"));
        panel.add(pavementSizeToLabel);
        pavementSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.pavementSizeMaxField.toolTip"));
        panel.add(pavementSizeMaxField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderPavement")));

        RandomMapPanelBasic.makeCompactGrid(panel, 2, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupFortifiedPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel numberFortifiedLabel = new JLabel(Messages.getString("RandomMapDialog.labFortifiedSpots"));
        panel.add(numberFortifiedLabel);
        fortifiedMinField.setToolTipText(Messages.getString("RandomMapDialog.fortifiedMinField.toolTip"));
        panel.add(fortifiedMinField);
        JLabel numberFortifiedToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberFortifiedToLabel);
        fortifiedMaxField.setToolTipText(Messages.getString("RandomMapDialog.fortifiedMaxField.toolTip"));
        panel.add(fortifiedMaxField);

        JLabel sizeFortifiedLabel = new JLabel(Messages.getString("RandomMapDialog.labFortifiedSize"));
        panel.add(sizeFortifiedLabel);
        fortifiedSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.fortifiedSizeMinField.toolTip"));
        panel.add(fortifiedSizeMinField);
        JLabel sizeFortifiedToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeFortifiedToLabel);
        fortifiedSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.fortifiedSizeMaxField.toolTip"));
        panel.add(fortifiedSizeMaxField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderFortified")));

        RandomMapPanelBasic.makeCompactGrid(panel, 2, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupCityPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel cityTypeLabel = new JLabel(Messages.getString("RandomMapDialog.labCity"));
        panel.add(cityTypeLabel);
        cityTypeCombo.setToolTipText(Messages.getString("RandomMapDialog.cityTypeCombo.toolTip"));

        // don't nag the user about city parameters if the city is NONE
        cityTypeCombo.addActionListener(e -> {
            setCityPanelState();
        });
        panel.add(cityTypeCombo);
        panel.add(new JLabel());
        panel.add(new JLabel());

        JLabel cityBlocksLabel = new JLabel(Messages.getString("RandomMapDialog.labCityBlocks"));
        panel.add(cityBlocksLabel);
        cityBlocks.addVerifier(new VerifyIsPositiveInteger());
        cityBlocks.setToolTipText(Messages.getString("RandomMapDialog.cityBlocks.toolTip"));
        panel.add(cityBlocks);
        panel.add(new JLabel());
        panel.add(new JLabel());

        JLabel cityCFLabel = new JLabel(Messages.getString("RandomMapDialog.labCityCF"));
        panel.add(cityCFLabel);
        cityCFMinField.setToolTipText(Messages.getString("RandomMapDialog.cityCFMinField.toolTip"));
        panel.add(cityCFMinField);

        JLabel cityCFToLabel = new JLabel(Messages.getString("to"));
        panel.add(cityCFToLabel);
        cityCFMaxField.setToolTipText(Messages.getString("RandomMapDialog.cityCFMaxField.toolTip"));
        panel.add(cityCFMaxField);

        JLabel cityDensityLabel = new JLabel(Messages.getString("RandomMapDialog.labCityDensity"));
        panel.add(cityDensityLabel);
        cityDensityField.setToolTipText(Messages.getString("RandomMapDialog.cityDensityField.toolTip"));
        panel.add(cityDensityField);
        panel.add(new JLabel());
        panel.add(new JLabel());

        JLabel cityFloorsLabel = new JLabel(Messages.getString("RandomMapDialog.labCityFloors"));
        panel.add(cityFloorsLabel);
        cityFloorsMinField.setToolTipText(Messages.getString("RandomMapDialog.cityFloorsMinField.toolTip"));
        panel.add(cityFloorsMinField);

        JLabel cityFloorsToLabel = new JLabel(Messages.getString("to"));
        panel.add(cityFloorsToLabel);
        cityFloorsMaxField.setToolTipText(Messages.getString("RandomMapDialog.cityFloorsMaxField.toolTip"));
        panel.add(cityFloorsMaxField);

        JLabel townSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labTownSize"));
        panel.add(townSizeLabel);
        townSizeField.setToolTipText(Messages.getString("RandomMapDialog.townSizeField.toolTip"));
        panel.add(townSizeField);
        panel.add(new JLabel());
        panel.add(new JLabel());

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderCity")));

        RandomMapPanelBasic.makeCompactGrid(panel, 6, 4, 6, 6, 6, 6);
        setCityPanelState();
        return panel;
    }

    /**
     * Worker function that sets up the state of the city-related textboxes
     */
    private void setCityPanelState() {
        boolean enableCityControls = cityTypeCombo.getSelectedIndex() != 0;

        cityBlocks.setEnabled(enableCityControls);
        cityCFMaxField.setEnabled(enableCityControls);
        cityCFMinField.setEnabled(enableCityControls);
        cityFloorsMaxField.setEnabled(enableCityControls);
        cityFloorsMinField.setEnabled(enableCityControls);
        cityDensityField.setEnabled(enableCityControls);
        townSizeField.setEnabled(enableCityControls);
    }

    private JScrollPane setupEffectsPanel() {
        JPanel panel = new JPanel(new SpringLayout());

        JLabel droughtLabel = new JLabel(Messages.getString("RandomMapDialog.labProbDrought"));
        panel.add(droughtLabel);
        droughtChanceField.setToolTipText(Messages.getString("RandomMapDialog.droughtChanceField.toolTip"));
        panel.add(droughtChanceField);
        
        JLabel fireLabel = new JLabel(Messages.getString("RandomMapDialog.labProbFire"));
        panel.add(fireLabel);
        fireChanceField.setToolTipText(Messages.getString("RandomMapDialog.fireChanceField.toolTip"));
        panel.add(fireChanceField);
        
        JLabel floodLabel = new JLabel(Messages.getString("RandomMapDialog.labProbFlood"));
        panel.add(floodLabel);
        floodChanceField.setToolTipText(Messages.getString("RandomMapDialog.floodChanceField.toolTip"));
        panel.add(floodChanceField);
        
        JLabel fxLabel = new JLabel(Messages.getString("RandomMapDialog.labFxMod"));
        panel.add(fxLabel);
        specialFxField.setToolTipText(Messages.getString("RandomMapDialog.specialFxField.toolTip"));
        panel.add(specialFxField);

        RandomMapPanelBasic.makeCompactGrid(panel, 4, 2, 6, 6, 6, 6);
        return new JScrollPane(panel);
    }

    private JScrollPane setupNaturalFeaturesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        panel.add(setupRoughsPanel());
        panel.add(setupSandsPanel());
        panel.add(setupSwampsPanel());
        panel.add(setupWoodsPanel());
        panel.add(setupFoliagePanel());
        panel.add(setupSnowPanel());

        return new JScrollPane(panel);
    }

    private JPanel setupWoodsPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());
        
        JLabel numberWoodsLabel = new JLabel(Messages.getString("RandomMapDialog.labForestSpots"));
        panel.add(numberWoodsLabel);
        woodsMinField.setToolTipText(Messages.getString("RandomMapDialog.woodsMinField.toolTip"));
        panel.add(woodsMinField);
        JLabel numberWoodsToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberWoodsToLabel);
        woodsMaxField.setToolTipText(Messages.getString("RandomMapDialog.woodsMaxField.toolTip"));
        panel.add(woodsMaxField);

        JLabel sizesWoodsLabel = new JLabel(Messages.getString("RandomMapDialog.labForestSize"));
        panel.add(sizesWoodsLabel);
        woodsMinSizeField.setToolTipText(Messages.getString("RandomMapDialog.woodsMinSizeField.toolTip"));
        panel.add(woodsMinSizeField);
        JLabel sizeWoodsToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeWoodsToLabel);
        woodsMaxSizeField.setToolTipText(Messages.getString("RandomMapDialog.woodsMaxSizeField.toolTip"));
        panel.add(woodsMaxSizeField);

        JLabel heavyWoodsLabel = new JLabel(Messages.getString("RandomMapDialog.labProbHeavy"));
        panel.add(heavyWoodsLabel);
        woodsHeavyChanceField.setToolTipText(Messages.getString("RandomMapDialog.woodsHeavyChanceField.toolTip"));
        panel.add(woodsHeavyChanceField);
        panel.add(new JLabel());
        panel.add(new JLabel());

        JLabel ultraWoodsLabel = new JLabel(Messages.getString("RandomMapDialog.labProbUltra"));
        panel.add(ultraWoodsLabel);
        woodsUltraChanceField.setToolTipText(Messages.getString("RandomMapDialog.woodsUltraChanceField.toolTip"));
        panel.add(woodsUltraChanceField);
        panel.add(new JLabel());
        panel.add(new JLabel());

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderWoods")));
        RandomMapPanelBasic.makeCompactGrid(panel, 4, 4, 6, 6, 6, 6);
        return panel;
    }
    
    private class FeaturePanel extends JPanel {
        private static final long serialVersionUID = 2064014325837995657L;

        public FeaturePanel(LayoutManager layout) {
            super(layout);
        }
        
        @Override
        public Dimension getMaximumSize() {
            // Make this Panel not stretch vertically
            Dimension size = getPreferredSize();
            Dimension maxSize = super.getMaximumSize();
            return new Dimension(maxSize.width, size.height);
        }        
    }
    
    private JPanel setupFoliagePanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());
        
        JLabel numberWoodsLabel = new JLabel(Messages.getString("RandomMapDialog.labFoliageSpots"));
        panel.add(numberWoodsLabel);
        foliageMinField.setToolTipText(Messages.getString("RandomMapDialog.foliageMinField.toolTip"));
        panel.add(foliageMinField);
        panel.add(new JLabel(Messages.getString("to")));
        foliageMaxField.setToolTipText(Messages.getString("RandomMapDialog.foliageMaxField.toolTip"));
        panel.add(foliageMaxField);

        JLabel sizesWoodsLabel = new JLabel(Messages.getString("RandomMapDialog.labFoliageSize"));
        panel.add(sizesWoodsLabel);
        foliageMinSizeField.setToolTipText(Messages.getString("RandomMapDialog.foliageMinSizeField.toolTip"));
        panel.add(foliageMinSizeField);
        panel.add(new JLabel(Messages.getString("to")));
        foliageMaxSizeField.setToolTipText(Messages.getString("RandomMapDialog.foliageMaxSizeField.toolTip"));
        panel.add(foliageMaxSizeField);

        JLabel heavyWoodsLabel = new JLabel(Messages.getString("RandomMapDialog.labProbHeavy"));
        panel.add(heavyWoodsLabel);
        foliageHeavyChanceField.setToolTipText(Messages.getString("RandomMapDialog.foliageHeavyChanceField.toolTip"));
        panel.add(foliageHeavyChanceField);
        panel.add(new JLabel());
        panel.add(new JLabel());

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderFoliage")));

        RandomMapPanelBasic.makeCompactGrid(panel, 3, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupSwampsPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());
        
        JLabel numberSwampsLabel = new JLabel(Messages.getString("RandomMapDialog.labSwampSpots"));
        panel.add(numberSwampsLabel);
        swampsMinField.setToolTipText(Messages.getString("RandomMapDialog.swampsMinField.toolTip"));
        panel.add(swampsMinField);

        JLabel numberSwampsToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberSwampsToLabel);
        swampsMaxField.setToolTipText(Messages.getString("RandomMapDialog.swampsMaxField.toolTip"));
        panel.add(swampsMaxField);

        JLabel sizeSwampsLabel = new JLabel(Messages.getString("RandomMapDialog.labSwampSize"));
        panel.add(sizeSwampsLabel);
        swampsMinSizeField.setToolTipText(Messages.getString("RandomMapDialog.swampsMinSizeField.toolTip"));
        panel.add(swampsMinSizeField);

        JLabel sizeSwampsToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeSwampsToLabel);
        swampsMaxSizeField.setToolTipText(Messages.getString("RandomMapDialog.swampsMaxSizeField.toolTip"));
        panel.add(swampsMaxSizeField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderSwamp")));
        
        RandomMapPanelBasic.makeCompactGrid(panel, 2, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupSandsPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel numberSandsLabel = new JLabel(Messages.getString("RandomMapDialog.labSandSpots"));
        panel.add(numberSandsLabel);
        sandsMinField.setToolTipText(Messages.getString("RandomMapDialog.sandsMinField.toolTip"));
        panel.add(sandsMinField);
        JLabel numberSandsToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberSandsToLabel);
        sandsMaxField.setToolTipText(Messages.getString("RandomMapDialog.sandsMaxField.toolTip"));
        panel.add(sandsMaxField);

        JLabel sizeSandsLabel = new JLabel(Messages.getString("RandomMapDialog.labSandSize"));
        panel.add(sizeSandsLabel);
        sandsSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.sandsSizeMinField.toolTip"));
        panel.add(sandsSizeMinField);
        JLabel sizeSandsToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeSandsToLabel);
        sandsSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.sandsSizeMaxField.toolTip"));
        panel.add(sandsSizeMaxField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderSand")));

        RandomMapPanelBasic.makeCompactGrid(panel, 2, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupSnowPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel numberSnowLabel = new JLabel(Messages.getString("RandomMapDialog.labSnowSpots"));
        panel.add(numberSnowLabel);
        snowMinField.setToolTipText(Messages.getString("RandomMapDialog.snowMinField.toolTip"));
        panel.add(snowMinField);
        JLabel numberSnowToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberSnowToLabel);
        snowMaxField.setToolTipText(Messages.getString("RandomMapDialog.snowMaxField.toolTip"));
        panel.add(snowMaxField);

        JLabel sizeSnowLabel = new JLabel(Messages.getString("RandomMapDialog.labSnowSize"));
        panel.add(sizeSnowLabel);
        snowSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.snowSizeMinField.toolTip"));
        panel.add(snowSizeMinField);
        JLabel sizeSnowToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeSnowToLabel);
        snowSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.snowSizeMaxField.toolTip"));
        panel.add(snowSizeMaxField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                Messages.getString("RandomMapDialog.borderSnow")));

        RandomMapPanelBasic.makeCompactGrid(panel, 2, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupRoughsPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        JLabel numberRoughsLabel = new JLabel(Messages.getString("RandomMapDialog.labRoughSpots"));
        panel.add(numberRoughsLabel);
        roughsMinField.setToolTipText(Messages.getString("RandomMapDialog.roughsMinField.toolTip"));
        panel.add(roughsMinField);
        JLabel numberRoughsToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberRoughsToLabel);
        roughsMaxField.setToolTipText(Messages.getString("RandomMapDialog.roughsMaxField.toolTip"));
        panel.add(roughsMaxField);

        JLabel sizeRoughsLabel = new JLabel(Messages.getString("RandomMapDialog.labRoughSize"));
        panel.add(sizeRoughsLabel);
        roughsMinSizeField.setToolTipText(Messages.getString("RandomMapDialog.roughsMinSizeField.toolTip"));
        panel.add(roughsMinSizeField);
        JLabel sizeRoughsToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeRoughsToLabel);
        roughsMaxSizeField.setToolTipText(Messages.getString("RandomMapDialog.roughsMaxSizeField.toolTip"));
        panel.add(roughsMaxSizeField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderRough")));

        RandomMapPanelBasic.makeCompactGrid(panel, 2, 4, 6, 6, 6, 6);
        return panel;
    }

    private JScrollPane setupElevationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        panel.add(setupElevationGeneralPanel());
        panel.add(setupMountainsPanel());
        panel.add(setupCratersPanel());

        return new JScrollPane(panel);
    }

    private JPanel setupCratersPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());
        
        final JLabel craterChanceLabel = new JLabel(Messages.getString("RandomMapDialog.labProbCrater"));
        panel.add(craterChanceLabel);
        craterChanceField.setToolTipText(Messages.getString("RandomMapDialog.craterChanceField.toolTip"));
        panel.add(craterChanceField);
        panel.add(new JLabel());
        panel.add(new JLabel());

        final JLabel craterNumberLabel = new JLabel(Messages.getString("RandomMapDialog.labMaxCraters"));
        panel.add(craterNumberLabel);
        craterAmountMinField.setToolTipText(Messages.getString("RandomMapDialog.craterAmountMinField.toolTip"));
        panel.add(craterAmountMinField);

        final JLabel craterNumberToField = new JLabel(Messages.getString("to"));
        panel.add(craterNumberToField);
        craterAmountMaxField.setToolTipText(Messages.getString("RandomMapDialog.craterAmountMaxField.toolTip"));
        panel.add(craterAmountMaxField);

        final JLabel craterSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labRadius"));
        panel.add(craterSizeLabel);
        craterSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.craterSizeMinField.toolTip"));
        panel.add(craterSizeMinField);

        final JLabel craterSizeToField = new JLabel(Messages.getString("to"));
        panel.add(craterSizeToField);
        craterSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.craterSizeMaxField.toolTip"));
        panel.add(craterSizeMaxField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderCrater")));
        RandomMapPanelBasic.makeCompactGrid(panel, 3, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupMountainsPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        final JLabel peaksLabel = new JLabel(Messages.getString("RandomMapDialog.labMountainPeaks"));
        panel.add(peaksLabel);
        elevationPeaksField.setToolTipText(Messages.getString("RandomMapDialog.elevationPeaksField.toolTip"));
        panel.add(elevationPeaksField);
        panel.add(new JLabel());
        panel.add(new JLabel());

        final JLabel mountainStyleLabel = new JLabel(Messages.getString("RandomMapDialog.labMountainStyle"));
        panel.add(mountainStyleLabel);
        mountainStyleCombo.setToolTipText(Messages.getString("RandomMapDialog.mountainStyleCombo.toolTip"));
        panel.add(mountainStyleCombo);
        panel.add(new JLabel());
        panel.add(new JLabel());

        final JLabel mountainHeightLabel = new JLabel(Messages.getString("RandomMapDialog.labMountainHeight"));
        panel.add(mountainHeightLabel);
        mountainHeightMinField.setToolTipText(Messages.getString("RandomMapDialog.mountainHeightMinField.toolTip"));
        panel.add(mountainHeightMinField);
        final JLabel mountainHeightToLabel = new JLabel(Messages.getString("to"));
        panel.add(mountainHeightToLabel);
        mountainHeightMaxField.setToolTipText(Messages.getString("RandomMapDialog.mountainHeightMaxField.toolTip"));
        panel.add(mountainHeightMaxField);

        final JLabel mountainWidthLabel = new JLabel(Messages.getString("RandomMapDialog.labMountainWidth"));
        panel.add(mountainWidthLabel);
        mountainWidthMinField.setToolTipText(Messages.getString("RandomMapDialog.mountainWidthMinField.toolTip"));
        panel.add(mountainWidthMinField);
        final JLabel mountainWidthToLabel = new JLabel(Messages.getString("to"));
        panel.add(mountainWidthToLabel);
        mountainWidthMaxField.setToolTipText(Messages.getString("RandomMapDialog.mountainWidthMaxField.toolTip"));
        panel.add(mountainWidthMaxField);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderMountain")));

        RandomMapPanelBasic.makeCompactGrid(panel, 4, 4, 6, 6, 6, 6);
        return panel;
    }

    private JPanel setupElevationGeneralPanel() {
        JPanel panel = new FeaturePanel(new SpringLayout());

        final JLabel algorithmLabel = new JLabel(Messages.getString("RandomMapDialog.labAlgorithmToUse"));
        panel.add(algorithmLabel);
        elevationAlgorithmField.setToolTipText(Messages.getString("RandomMapDialog.elevationAlgorithmField.toolTip"));
        panel.add(elevationAlgorithmField);

        final JLabel hillinessLabel = new JLabel(Messages.getString("RandomMapDialog.labHilliness"));
        panel.add(hillinessLabel);
        hillinessField.setToolTipText(Messages.getString("RandomMapDialog.hillinessField.toolTip"));
        panel.add(hillinessField);

        final JLabel elevationRangeLabel = new JLabel(Messages.getString("RandomMapDialog.labRange"));
        panel.add(elevationRangeLabel);
        elevationRangeField.setToolTipText(Messages.getString("RandomMapDialog.elevationRangeField.toolTip"));
        panel.add(elevationRangeField);

        final JLabel elevationCliffsLabel = new JLabel(Messages.getString("RandomMapDialog.labCliffs"));
        panel.add(elevationCliffsLabel);
        elevationCliffsField.setToolTipText(Messages.getString("RandomMapDialog.elevationCliffsField.toolTip"));
        panel.add(elevationCliffsField);

        final JLabel elevationInversionLabel = new JLabel(Messages.getString("RandomMapDialog.labProbInvert"));
        panel.add(elevationInversionLabel);
        elevationInversionField.setToolTipText(Messages.getString("RandomMapDialog.elevationInversionField.toolTip"));
        panel.add(elevationInversionField);

        final JLabel invertNegativeLabel = new JLabel(Messages.getString("RandomMapDialog.labInvertNegative"));
        panel.add(invertNegativeLabel);
        invertNegativeCheck.setText("");
        invertNegativeCheck.setToolTipText(Messages.getString("RandomMapDialog.invertNegativeCheck.toolTip"));
        panel.add(invertNegativeCheck);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderGeneral")));

        RandomMapPanelBasic.makeCompactGrid(panel, 6, 2, 6, 6, 6, 6);
        return panel;
    }

    // Enum containing the various valid mountain styles.
    private enum MountainStyle {
        PLAIN(MapSettings.MOUNTAIN_PLAIN, Messages.getString("RandomMapDialog.mountainPlain")),
        VOLCANO_EXTINCT(MapSettings.MOUNTAIN_VOLCANO_EXTINCT, Messages.getString("RandomMapDialog.volcanoExtinct")),
        VOLCANO_DORMANT(MapSettings.MOUNTAIN_VOLCANO_DORMANT, Messages.getString("RandomMapDialog.volcanoDormant")),
        VOLCANO_ACTIVE(MapSettings.MOUNTAIN_VOLCANO_ACTIVE, Messages.getString("RandomMapDialog.volcanoActive")),
        SNOWCAPPED(MapSettings.MOUNTAIN_SNOWCAPPED, Messages.getString("RandomMapDialog.mountainSnowcapped")),
        LAKE(MapSettings.MOUNTAIN_LAKE, Messages.getString("RandomMapDialog.mountainLake"));

        private final int code;
        private final String description;

        MountainStyle(int code, String description) {
            this.code = code;
            this.description = description;
        }

        private int getCode() {
            return code;
        }

        private String getDescription() {
            return description;
        }

        private static @Nullable MountainStyle getMountainStyle(String description) {
            for (MountainStyle ms : values()) {
                if (ms.getDescription().equals(description)) {
                    return ms;
                }
            }
            return null;
        }

        private static @Nullable MountainStyle getMountainStyle(int code) {
            for (MountainStyle ms : values()) {
                if (ms.getCode() == code) {
                    return ms;
                }
            }
            return null;
        }

        private static String[] getStyleDescriptions() {
            String[] styles = new String[values().length];
            for (int i = 0; i < styles.length; i++) {
                styles[i] = values()[i].getDescription();
            }
            return styles;
        }
    }

    private void showDataValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "Data Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    // Has the given field validate itself.
    private boolean isFieldVerified(VerifiableTextField field) {
        String result = field.verifyTextS();
        if (result != null) {
            result = field.getName() + ": " + result;
            LogManager.getLogger().error(result, new RuntimeException());
            field.requestFocus();
            showDataValidationError(result);
        }
        return (result == null);
    }

    // Takes in a min and max field, makes sure the data is generally valid in each then compares them to make sure
    // the minimum value does not exceed the maximum.
    private boolean isMinMaxVerified(VerifiableTextField min, VerifiableTextField max) {
        if (!isFieldVerified(min) || !isFieldVerified(max)) {
            return false;
        }

        final String INVALID = "Minimum cannot exceed maximum.";
        if (min.getAsInt() > max.getAsInt()) {
            LogManager.getLogger().error("", new RuntimeException(INVALID));
            min.setOldToolTip(min.getToolTipText());
            max.setOldToolTip(max.getToolTipText());
            min.setBackground(VerifiableTextField.getInvalidColor());
            max.setBackground(VerifiableTextField.getInvalidColor());
            min.setToolTipText(INVALID);
            max.setToolTipText(INVALID);
            min.requestFocus();
            showDataValidationError(INVALID);
            return false;
        }
        return true;
    }

    // Verifies all the fields on this panel.
    private boolean areMapSettingsVerified() {
        if (!isFieldVerified(elevationAlgorithmField)) {
            return false;
        }

        if (!isFieldVerified(hillinessField)) {
            return false;
        }

        if (!isFieldVerified(elevationRangeField)) {
            return false;
        }

        if (!isFieldVerified(elevationCliffsField)) {
            return false;
        }

        if (!isFieldVerified(elevationInversionField)) {
            return false;
        }

        if (!isFieldVerified(elevationPeaksField)) {
            return false;
        }

        if (!isFieldVerified(mountainHeightMinField)) {
            return false;
        }

        if (!isMinMaxVerified(mountainHeightMinField, mountainHeightMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(mountainWidthMinField, mountainWidthMaxField)) {
            return false;
        }

        if (!isFieldVerified(craterChanceField)) {
            return false;
        }

        if (!isMinMaxVerified(craterAmountMinField, craterAmountMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(craterSizeMinField, craterSizeMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(roughsMinField, roughsMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(roughsMinSizeField, roughsMaxSizeField)) {
            return false;
        }

        if (!isMinMaxVerified(sandsMinField, sandsMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(sandsSizeMinField, sandsSizeMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(snowMinField, snowMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(snowSizeMinField, snowSizeMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(swampsMinField, swampsMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(swampsMinSizeField, swampsMaxSizeField)) {
            return false;
        }

        if (!isMinMaxVerified(woodsMinField, woodsMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(woodsMinSizeField, woodsMaxSizeField)) {
            return false;
        }

        if (!isFieldVerified(woodsHeavyChanceField)) {
            return false;
        }

        if (!isFieldVerified(woodsUltraChanceField)) {
            return false;
        }
        
        if (!isMinMaxVerified(foliageMinField, foliageMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(foliageMinSizeField, foliageMaxSizeField)) {
            return false;
        }

        if (!isFieldVerified(foliageHeavyChanceField)) {
            return false;
        }

        if (!isMinMaxVerified(fieldsMinField, fieldsMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(fieldSizeMinField, fieldSizeMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(fortifiedMinField, fortifiedMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(fortifiedSizeMinField, fortifiedSizeMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(pavementMinField, pavementMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(pavementSizeMinField, pavementSizeMaxField)) {
            return false;
        }

        if (!isFieldVerified(roadChanceField)) {
            return false;
        }

        if (!isMinMaxVerified(rubbleMinField, rubbleMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(rubbleSizeMinField, rubbleSizeMaxField)) {
            return false;
        }

        if (!isFieldVerified(cityBlocks)) {
            return false;
        }

        if (!isMinMaxVerified(cityCFMinField, cityCFMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(cityFloorsMinField, cityFloorsMaxField)) {
            return false;
        }

        if (!isFieldVerified(cityDensityField)) {
            return false;
        }

        if (!isFieldVerified(townSizeField)) {
            return false;
        }

        if (!isMinMaxVerified(lakesMinField, lakesMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(lakeSizeMinField, lakeSizeMaxField)) {
            return false;
        }

        if (!isFieldVerified(deepChanceField)) {
            return false;
        }

        if (!isFieldVerified(riverChanceField)) {
            return false;
        }

        if (!isMinMaxVerified(iceMinField, iceMaxField)) {
            return false;
        }

        if (!isMinMaxVerified(iceSizeMinField, iceSizeMaxField)) {
            return false;
        }

        if (!isFieldVerified(freezeChanceField)) {
            return false;
        }

        if (!isFieldVerified(droughtChanceField)) {
            return false;
        }

        if (!isFieldVerified(floodChanceField)) {
            return false;
        }

        if (!isFieldVerified(fireChanceField)) {
            return false;
        }

        if (!isFieldVerified(specialFxField)) {
            return false;
        }

        return true;
    }

    /**
     * Returns a {@link MapSettings} object reflecting the values set on this panel after verifying that all the fields
     * are valid.  If they are not, a NULL will be returned.
     *
     * @return The new {@link MapSettings} object or NULL if there are any invalid settings on the panel.
     */
    public MapSettings getMapSettings() {
        if (!areMapSettingsVerified()) {
            return null;
        }

        // Build a new MapSettings objects.
        MapSettings newMapSettings = MapSettings.getInstance(mapSettings);

        // Update the settings with the values from the fields.
        newMapSettings.setAlgorithmToUse(elevationAlgorithmField.getAsInt());
        newMapSettings.setElevationParams(hillinessField.getAsInt(),
                                          elevationRangeField.getAsInt(),
                                          elevationInversionField.getAsInt());
        newMapSettings.setCliffParam(elevationCliffsField.getAsInt());

        // No idea why this is an int instead of a boolean.
        newMapSettings.setInvertNegativeTerrain(invertNegativeCheck.isSelected() ? 1 : 0);
        newMapSettings.setMountainParams(elevationPeaksField.getAsInt(),
                                         mountainWidthMinField.getAsInt(),
                                         mountainWidthMaxField.getAsInt(),
                                         mountainHeightMinField.getAsInt(),
                                         mountainHeightMaxField.getAsInt(),
                                         MountainStyle.getMountainStyle((String) mountainStyleCombo.getSelectedItem())
                                                      .getCode());
        newMapSettings.setCraterParam(craterChanceField.getAsInt(),
                                      craterAmountMinField.getAsInt(),
                                      craterAmountMaxField.getAsInt(),
                                      craterSizeMinField.getAsInt(),
                                      craterSizeMaxField.getAsInt());
        newMapSettings.setRoughParams(roughsMinField.getAsInt(),
                                      roughsMaxField.getAsInt(),
                                      roughsMinSizeField.getAsInt(),
                                      roughsMaxSizeField.getAsInt());
        newMapSettings.setSandParams(sandsMinField.getAsInt(),
                                     sandsMaxField.getAsInt(),
                                     sandsSizeMinField.getAsInt(),
                                     sandsSizeMaxField.getAsInt());
        newMapSettings.setSnowParams(snowMinField.getAsInt(),
                                    snowMaxField.getAsInt(),
                                    snowSizeMinField.getAsInt(),
                                    snowSizeMaxField.getAsInt());
        newMapSettings.setSwampParams(swampsMinField.getAsInt(),
                                      swampsMaxField.getAsInt(),
                                      swampsMinSizeField.getAsInt(),
                                      swampsMaxSizeField.getAsInt());
        newMapSettings.setForestParams(woodsMinField.getAsInt(),
                                       woodsMaxField.getAsInt(),
                                       woodsMinSizeField.getAsInt(),
                                       woodsMaxSizeField.getAsInt(),
                                       woodsHeavyChanceField.getAsInt(),
                                        woodsUltraChanceField.getAsInt());
        newMapSettings.setFoliageParams(foliageMinField.getAsInt(),
                                       foliageMaxField.getAsInt(),
                                       foliageMinSizeField.getAsInt(),
                                       foliageMaxSizeField.getAsInt(),
                                       foliageHeavyChanceField.getAsInt());
        newMapSettings.setPlantedFieldParams(fieldsMinField.getAsInt(),
                                             fieldsMaxField.getAsInt(),
                                             fieldSizeMinField.getAsInt(),
                                             fieldSizeMaxField.getAsInt());
        newMapSettings.setFortifiedParams(fortifiedMinField.getAsInt(),
                                          fortifiedMaxField.getAsInt(),
                                          fortifiedSizeMinField.getAsInt(),
                                          fortifiedSizeMaxField.getAsInt());
        newMapSettings.setPavementParams(pavementMinField.getAsInt(),
                                         pavementMaxField.getAsInt(),
                                         pavementSizeMinField.getAsInt(),
                                         pavementSizeMaxField.getAsInt());
        newMapSettings.setRubbleParams(rubbleMinField.getAsInt(),
                                       rubbleMaxField.getAsInt(),
                                       rubbleSizeMinField.getAsInt(),
                                       rubbleSizeMaxField.getAsInt());
        newMapSettings.setRoadParam(roadChanceField.getAsInt());
        newMapSettings.setCityParams(cityBlocks.getAsInt(),
                                     (String) cityTypeCombo.getSelectedItem(),
                                     cityCFMinField.getAsInt(),
                                     cityCFMaxField.getAsInt(),
                                     cityFloorsMinField.getAsInt(),
                                     cityFloorsMaxField.getAsInt(),
                                     cityDensityField.getAsInt(),
                                     townSizeField.getAsInt());
        newMapSettings.setWaterParams(lakesMinField.getAsInt(),
                                      lakesMaxField.getAsInt(),
                                      lakeSizeMinField.getAsInt(),
                                      lakeSizeMaxField.getAsInt(),
                                      deepChanceField.getAsInt());
        newMapSettings.setRiverParam(riverChanceField.getAsInt());
        newMapSettings.setIceParams(iceMinField.getAsInt(),
                                    iceMaxField.getAsInt(),
                                    iceSizeMinField.getAsInt(),
                                    iceSizeMaxField.getAsInt());
        newMapSettings.setSpecialFX(specialFxField.getAsInt(),
                                    fireChanceField.getAsInt(),
                                    freezeChanceField.getAsInt(),
                                    floodChanceField.getAsInt(),
                                    droughtChanceField.getAsInt());

        return newMapSettings;
    }
}
