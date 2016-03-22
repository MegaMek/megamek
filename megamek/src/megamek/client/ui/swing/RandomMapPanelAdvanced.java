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
import megamek.common.util.BoardUtilities;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 3/13/14 3:57 PM
 */
public class RandomMapPanelAdvanced extends JPanel {

    /**
     * 
     */
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
    private final VerifiableTextField elevationAlgorithmField = new VerifiableTextField(4);
    private final VerifiableTextField hillinessField = new VerifiableTextField(4);
    private final VerifiableTextField elevationRangeField = new VerifiableTextField(4);
    private final VerifiableTextField elevationCliffsField = new VerifiableTextField(4);
    private final VerifiableTextField elevationInversionField = new VerifiableTextField(4);
    private final JCheckBox invertNegativeCheck = new JCheckBox();
    private final VerifiableTextField elevationPeaksField = new VerifiableTextField(4);
    private final JComboBox<String> mountainStyleCombo = new JComboBox<>(MountainStyle.getStyleDescriptions());
    private final VerifiableTextField mountainHeightMinField = new VerifiableTextField(4);
    private final VerifiableTextField mountainHeightMaxField = new VerifiableTextField(4);
    private final VerifiableTextField mountainWidthMinField = new VerifiableTextField(4);
    private final VerifiableTextField mountainWidthMaxField = new VerifiableTextField(4);
    private final VerifiableTextField craterChanceField = new VerifiableTextField(4);
    private final VerifiableTextField craterAmountMinField = new VerifiableTextField(4);
    private final VerifiableTextField craterAmountMaxField = new VerifiableTextField(4);
    private final VerifiableTextField craterSizeMinField = new VerifiableTextField(4);
    private final VerifiableTextField craterSizeMaxField = new VerifiableTextField(4);

    // Natural Features
    private final VerifiableTextField roughsMinField = new VerifiableTextField(4);
    private final VerifiableTextField roughsMaxField = new VerifiableTextField(4);
    private final VerifiableTextField roughsMinSizeField = new VerifiableTextField(4);
    private final VerifiableTextField roughsMaxSizeField = new VerifiableTextField(4);
    private final VerifiableTextField sandsMinField = new VerifiableTextField(4);
    private final VerifiableTextField sandsMaxField = new VerifiableTextField(4);
    private final VerifiableTextField sandsSizeMinField = new VerifiableTextField(4);
    private final VerifiableTextField sandsSizeMaxField = new VerifiableTextField(4);
    private final VerifiableTextField swampsMinField = new VerifiableTextField(4);
    private final VerifiableTextField swampsMaxField = new VerifiableTextField(4);
    private final VerifiableTextField swampsMinSizeField = new VerifiableTextField(4);
    private final VerifiableTextField swampsMaxSizeField = new VerifiableTextField(4);
    private final VerifiableTextField woodsMinField = new VerifiableTextField(4);
    private final VerifiableTextField woodsMaxField = new VerifiableTextField(4);
    private final VerifiableTextField woodsMinSizeField = new VerifiableTextField(4);
    private final VerifiableTextField woodsMaxSizeField = new VerifiableTextField(4);
    private final VerifiableTextField woodsHeavyChanceField = new VerifiableTextField(4);

    // Civilized Features.
    private final VerifiableTextField fieldsMinField = new VerifiableTextField(4);
    private final VerifiableTextField fieldsMaxField = new VerifiableTextField(4);
    private final VerifiableTextField fieldSizeMinField = new VerifiableTextField(4);
    private final VerifiableTextField fieldSizeMaxField = new VerifiableTextField(4);
    private final VerifiableTextField fortifiedMinField = new VerifiableTextField(4);
    private final VerifiableTextField fortifiedMaxField = new VerifiableTextField(4);
    private final VerifiableTextField fortifiedSizeMinField = new VerifiableTextField(4);
    private final VerifiableTextField fortifiedSizeMaxField = new VerifiableTextField(4);
    private final VerifiableTextField pavementMinField = new VerifiableTextField(4);
    private final VerifiableTextField pavementMaxField = new VerifiableTextField(4);
    private final VerifiableTextField pavementSizeMinField = new VerifiableTextField(4);
    private final VerifiableTextField pavementSizeMaxField = new VerifiableTextField(4);
    private final VerifiableTextField roadChanceField = new VerifiableTextField(4);
    private final VerifiableTextField rubbleMinField = new VerifiableTextField(4);
    private final VerifiableTextField rubbleMaxField = new VerifiableTextField(4);
    private final VerifiableTextField rubbleSizeMinField = new VerifiableTextField(4);
    private final VerifiableTextField rubbleSizeMaxField = new VerifiableTextField(4);
    private final JComboBox<String> cityTypeCombo = new JComboBox<>(CT_CHOICES);
    private final VerifiableTextField cityBlocks = new VerifiableTextField(4);
    private final VerifiableTextField cityCFMinField = new VerifiableTextField(4);
    private final VerifiableTextField cityCFMaxField = new VerifiableTextField(4);
    private final VerifiableTextField cityFloorsMinField = new VerifiableTextField(4);
    private final VerifiableTextField cityFloorsMaxField = new VerifiableTextField(4);
    private final VerifiableTextField cityDensityField = new VerifiableTextField(4);
    private final VerifiableTextField townSizeField = new VerifiableTextField(4);

    // Water
    private final VerifiableTextField lakesMinField = new VerifiableTextField(4);
    private final VerifiableTextField lakesMaxField = new VerifiableTextField(4);
    private final VerifiableTextField lakeSizeMinField = new VerifiableTextField(4);
    private final VerifiableTextField lakeSizeMaxField = new VerifiableTextField(4);
    private final VerifiableTextField deepChanceField = new VerifiableTextField(4);
    private final VerifiableTextField riverChanceField = new VerifiableTextField(4);
    private final VerifiableTextField iceMinField = new VerifiableTextField(4);
    private final VerifiableTextField iceMaxField = new VerifiableTextField(4);
    private final VerifiableTextField iceSizeMinField = new VerifiableTextField(4);
    private final VerifiableTextField iceSizeMaxField = new VerifiableTextField(4);
    private final VerifiableTextField freezeChanceField = new VerifiableTextField(4);

    // Special effects
    private final VerifiableTextField droughtChanceField = new VerifiableTextField(4);
    private final VerifiableTextField floodChanceField = new VerifiableTextField(4);
    private final VerifiableTextField fireChanceField = new VerifiableTextField(4);
    private final VerifiableTextField specialFxField = new VerifiableTextField(4);

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
        swampsMinField.setText(String.valueOf(mapSettings.getMinSwampSpots()));
        swampsMaxField.setText(String.valueOf(mapSettings.getMaxSwampSpots()));
        swampsMinSizeField.setText(String.valueOf(mapSettings.getMinSwampSize()));
        swampsMaxSizeField.setText(String.valueOf(mapSettings.getMaxSwampSize()));
        sandsMinField.setText(String.valueOf(mapSettings.getMinSandSpots()));
        sandsMaxField.setText(String.valueOf(mapSettings.getMaxSandSpots()));
        sandsSizeMinField.setText(String.valueOf(mapSettings.getMinSandSize()));
        sandsSizeMaxField.setText(String.valueOf(mapSettings.getMaxSandSize()));
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
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabCivilization"), civilizedFeaturesPanel);

        JScrollPane elevationPanel = setupElevationPanel();
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabElevation"), elevationPanel);

        JScrollPane naturalFeaturesPanel = setupNaturalFeaturesPanel();
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabNatural"), naturalFeaturesPanel);

        JScrollPane effectsPanel = setupEffectsPanel();
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabEffects"), effectsPanel);

        JScrollPane waterPanel = setupWaterPanel();
        tabbedPane.addTab(Messages.getString("RandomMapDialog.tabWater"), waterPanel);

        tabbedPane.setSelectedComponent(civilizedFeaturesPanel);
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    // Try to avoid too much code repetition.
    private GridBagConstraints setupConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(2, 2, 2, 2);
        return constraints;
    }

    private JScrollPane setupWaterPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        panel.add(setupLakesPanel(), constraints);

        // Row 2
        constraints.gridy++;
        panel.add(setupRiverPanel(), constraints);

        // Row 3.
        constraints.gridy++;
        constraints.weighty = 1;
        panel.add(setupIcePanel(), constraints);

        return new JScrollPane(panel);
    }

    private JPanel setupRiverPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel riverChanceLabel = new JLabel(Messages.getString("RandomMapDialog.labProbRiver"));
        panel.add(riverChanceLabel, constraints);

        // Row 1, Columns 2-4.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        riverChanceField.setRequired(true);
        riverChanceField.setSelectAllTextOnGotFocus(true);
        riverChanceField.addVerifier(new VerifyInRange(0, 100, true));
        riverChanceField.setToolTipText(Messages.getString("RandomMapDialog.riverChanceField.toolTip"));
        riverChanceField.setName(riverChanceLabel.getText());
        panel.add(riverChanceField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderRiver")));

        return panel;
    }

    private JPanel setupIcePanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel iceLabel = new JLabel(Messages.getString("RandomMapDialog.labIceSpots"));
        panel.add(iceLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        iceMinField.setRequired(true);
        iceMinField.setSelectAllTextOnGotFocus(true);
        iceMinField.addVerifier(new VerifyIsPositiveInteger());
        iceMinField.setToolTipText(Messages.getString("RandomMapDialog.iceMinField.toolTip"));
        iceMinField.setName(iceLabel.getText());
        panel.add(iceMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel iceToLabel = new JLabel(Messages.getString("to"));
        panel.add(iceToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        iceMaxField.setRequired(true);
        iceMaxField.setSelectAllTextOnGotFocus(true);
        iceMaxField.addVerifier(new VerifyIsPositiveInteger());
        iceMaxField.setToolTipText(Messages.getString("RandomMapDialog.iceMaxField.toolTip"));
        iceMaxField.setName(iceLabel.getText());
        panel.add(iceMaxField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        JLabel iceSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labIceSize"));
        panel.add(iceSizeLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        iceSizeMinField.setRequired(true);
        iceSizeMinField.setSelectAllTextOnGotFocus(true);
        iceSizeMinField.addVerifier(new VerifyIsPositiveInteger());
        iceSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.iceSizeMinField.toolTip"));
        iceSizeMinField.setName(iceSizeLabel.getText());
        panel.add(iceSizeMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel iceSizeToLabel = new JLabel(Messages.getString("to"));
        panel.add(iceSizeToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        iceSizeMaxField.setRequired(true);
        iceSizeMaxField.setSelectAllTextOnGotFocus(true);
        iceSizeMaxField.addVerifier(new VerifyIsPositiveInteger());
        iceSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.iceSizeMaxField.toolTip"));
        iceSizeMaxField.setName(iceSizeLabel.getText());
        panel.add(iceSizeMaxField, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        JLabel frozenWaterLabel = new JLabel(Messages.getString("RandomMapDialog.labProbFreeze"));
        panel.add(frozenWaterLabel, constraints);

        // Row 3, Column 2-4.
        constraints.gridx++;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 1;
        freezeChanceField.setRequired(true);
        freezeChanceField.setSelectAllTextOnGotFocus(true);
        freezeChanceField.addVerifier(new VerifyInRange(0, 100, true));
        freezeChanceField.setToolTipText(Messages.getString("RandomMapDialog.freezeChanceField.toolTip"));
        freezeChanceField.setName(frozenWaterLabel.getText());
        panel.add(freezeChanceField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderIce")));

        return panel;
    }

    private JPanel setupLakesPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel lakesLabel = new JLabel(Messages.getString("RandomMapDialog.labWaterSpots"));
        panel.add(lakesLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        lakesMinField.setRequired(true);
        lakesMinField.setSelectAllTextOnGotFocus(true);
        lakesMinField.addVerifier(new VerifyIsPositiveInteger());
        lakesMinField.setToolTipText(Messages.getString("RandomMapDialog.lakesMinField.toolTip"));
        lakesMinField.setName(lakesLabel.getText());
        panel.add(lakesMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel lakesToLabel = new JLabel(Messages.getString("to"));
        panel.add(lakesToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        lakesMaxField.setRequired(true);
        lakesMaxField.setSelectAllTextOnGotFocus(true);
        lakesMaxField.addVerifier(new VerifyIsPositiveInteger());
        lakesMaxField.setToolTipText(Messages.getString("RandomMapDialog.lakesMaxField.toolTip"));
        lakesMaxField.setName(lakesLabel.getText());
        panel.add(lakesMaxField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        JLabel lakeSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labWaterSize"));
        panel.add(lakeSizeLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        lakeSizeMinField.setRequired(true);
        lakeSizeMinField.setSelectAllTextOnGotFocus(true);
        lakeSizeMinField.addVerifier(new VerifyIsPositiveInteger());
        lakeSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.lakeSizeMinField.toolTip"));
        lakeSizeMinField.setName(lakeSizeLabel.getText());
        panel.add(lakeSizeMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel lakeSizeToLabel = new JLabel(Messages.getString("to"));
        panel.add(lakeSizeToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        lakeSizeMaxField.setRequired(true);
        lakeSizeMaxField.setSelectAllTextOnGotFocus(true);
        lakeSizeMaxField.addVerifier(new VerifyIsPositiveInteger());
        lakeSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.lakeSizeMaxField.toolTip"));
        lakeSizeMaxField.setName(lakeSizeLabel.getText());
        panel.add(lakeSizeMaxField, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        JLabel deepWaterLabel = new JLabel(Messages.getString("RandomMapDialog.labProbDeep"));
        panel.add(deepWaterLabel, constraints);

        // Row 3, Column 2-4.
        constraints.gridx++;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 1;
        deepChanceField.setRequired(true);
        deepChanceField.setSelectAllTextOnGotFocus(true);
        deepChanceField.addVerifier(new VerifyInRange(0, 100, true));
        deepChanceField.setToolTipText(Messages.getString("RandomMapDialog.deepChanceField.toolTip"));
        deepChanceField.setName(deepWaterLabel.getText());
        panel.add(deepChanceField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderLakes")));

        return panel;
    }

    private JScrollPane setupCivilizedPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        panel.add(setupCityPanel(), constraints);

        // Row 2
        constraints.gridy++;
        panel.add(setupFortifiedPanel(), constraints);

        // Row 3
        constraints.gridy++;
        panel.add(setupPavementPanel(), constraints);

        // Row 4
        constraints.gridy++;
        panel.add(setupFieldsPanel(), constraints);

        // Row 5
        constraints.gridy++;
        panel.add(setupRoadPanel(), constraints);

        // Row 6
        constraints.gridy++;
        constraints.weighty = 1;
        panel.add(setupRubblePanel(), constraints);

        return new JScrollPane(panel);
    }

    private JPanel setupFieldsPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel fieldsSpotsLabel = new JLabel(Messages.getString("RandomMapDialog.labPlantedFieldSpots"));
        panel.add(fieldsSpotsLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        fieldsMinField.setRequired(true);
        fieldsMinField.setSelectAllTextOnGotFocus(true);
        fieldsMinField.addVerifier(new VerifyIsPositiveInteger());
        fieldsMinField.setToolTipText(Messages.getString("RandomMapDialog.fieldsMinField.toolTip"));
        fieldsMinField.setName(fieldsSpotsLabel.getText());
        panel.add(fieldsMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel fieldsSpotsToLabel = new JLabel(Messages.getString("to"));
        panel.add(fieldsSpotsToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        fieldsMaxField.setRequired(true);
        fieldsMaxField.setSelectAllTextOnGotFocus(true);
        fieldsMaxField.addVerifier(new VerifyIsPositiveInteger());
        fieldsMaxField.setToolTipText(Messages.getString("RandomMapDialog.fieldsMaxField.toolTip"));
        fieldsMaxField.setName(fieldsSpotsLabel.getText());
        panel.add(fieldsMaxField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel fieldsSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labPlantedFieldSize"));
        panel.add(fieldsSizeLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        fieldSizeMinField.setRequired(true);
        fieldSizeMinField.setSelectAllTextOnGotFocus(true);
        fieldSizeMinField.addVerifier(new VerifyIsPositiveInteger());
        fieldSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.fieldSizeMinField.toolTip"));
        fieldSizeMinField.setName(fieldsSizeLabel.getText());
        panel.add(fieldSizeMinField, constraints);

        // Row 2, Column 3.
        constraints.gridx++;
        JLabel fieldSizeToLabel = new JLabel(Messages.getString("to"));
        panel.add(fieldSizeToLabel, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        fieldSizeMaxField.setRequired(true);
        fieldSizeMaxField.setSelectAllTextOnGotFocus(true);
        fieldSizeMaxField.addVerifier(new VerifyIsPositiveInteger());
        fieldSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.fieldSizeMaxField.toolTip"));
        fieldSizeMaxField.setName(fieldsSizeLabel.getText());
        panel.add(fieldSizeMaxField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderFields")));

        return panel;
    }

    private JPanel setupRubblePanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel rubbleSpotsLabel = new JLabel(Messages.getString("RandomMapDialog.labRubbleSpots"));
        panel.add(rubbleSpotsLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        rubbleMinField.setRequired(true);
        rubbleMinField.setSelectAllTextOnGotFocus(true);
        rubbleMinField.addVerifier(new VerifyIsPositiveInteger());
        rubbleMinField.setToolTipText(Messages.getString("RandomMapDialog.rubbleMinField.toolTip"));
        rubbleMinField.setName(rubbleSpotsLabel.getText());
        panel.add(rubbleMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel rubbleSpotsToLabel = new JLabel(Messages.getString("to"));
        panel.add(rubbleSpotsToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        rubbleMaxField.setRequired(true);
        rubbleMaxField.setSelectAllTextOnGotFocus(true);
        rubbleMaxField.addVerifier(new VerifyIsPositiveInteger());
        rubbleMaxField.setToolTipText(Messages.getString("RandomMapDialog.rubbleMaxField.toolTip"));
        rubbleMaxField.setName(rubbleSpotsLabel.getText());
        panel.add(rubbleMaxField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel rubbleSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labRubbleSize"));
        panel.add(rubbleSizeLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        rubbleSizeMinField.setRequired(true);
        rubbleSizeMinField.setSelectAllTextOnGotFocus(true);
        rubbleSizeMinField.addVerifier(new VerifyIsPositiveInteger());
        rubbleSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.rubbleSizeMinField.toolTip"));
        rubbleSizeMinField.setName(rubbleSizeLabel.getText());
        panel.add(rubbleSizeMinField, constraints);

        // Row 2, Column 3.
        constraints.gridx++;
        JLabel rubbleSizeToLabel = new JLabel(Messages.getString("to"));
        panel.add(rubbleSizeToLabel, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        rubbleSizeMaxField.setRequired(true);
        rubbleSizeMaxField.setSelectAllTextOnGotFocus(true);
        rubbleSizeMaxField.addVerifier(new VerifyIsPositiveInteger());
        rubbleSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.rubbleSizeMaxField.toolTip"));
        rubbleSizeMaxField.setName(rubbleSizeLabel.getText());
        panel.add(rubbleSizeMaxField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderRubble")));

        return panel;
    }

    private JPanel setupRoadPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel roadChanceLabel = new JLabel(Messages.getString("RandomMapDialog.labProbRoad"));
        panel.add(roadChanceLabel, constraints);

        // Row 1, Columns 2-4.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        roadChanceField.setRequired(true);
        roadChanceField.setSelectAllTextOnGotFocus(true);
        roadChanceField.addVerifier(new VerifyInRange(0, 100, true));
        roadChanceField.setToolTipText(Messages.getString("RandomMapDialog.roadChanceField.toolTip"));
        roadChanceField.setName(roadChanceLabel.getText());
        panel.add(roadChanceField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderRoad")));

        return panel;
    }

    private JPanel setupPavementPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel pavementSpotsLabel = new JLabel(Messages.getString("RandomMapDialog.labPavementSpots"));
        panel.add(pavementSpotsLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        pavementMinField.setRequired(true);
        pavementMinField.setSelectAllTextOnGotFocus(true);
        pavementMinField.addVerifier(new VerifyIsPositiveInteger());
        pavementMinField.setToolTipText(Messages.getString("RandomMapDialog.pavementMinField.toolTip"));
        pavementMinField.setName(pavementSpotsLabel.getText());
        panel.add(pavementMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel pavementSpotsToLabel = new JLabel(Messages.getString("to"));
        panel.add(pavementSpotsToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        pavementMaxField.setRequired(true);
        pavementMaxField.setSelectAllTextOnGotFocus(true);
        pavementMaxField.addVerifier(new VerifyIsPositiveInteger());
        pavementMaxField.setToolTipText(Messages.getString("RandomMapDialog.pavementMaxField.toolTip"));
        pavementMaxField.setName(pavementSpotsLabel.getText());
        panel.add(pavementMaxField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel pavementSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labPavementSize"));
        panel.add(pavementSizeLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        pavementSizeMinField.setRequired(true);
        pavementSizeMinField.setSelectAllTextOnGotFocus(true);
        pavementSizeMinField.addVerifier(new VerifyIsPositiveInteger());
        pavementSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.pavementSizeMinField.toolTip"));
        pavementSizeMinField.setName(pavementSizeLabel.getText());
        panel.add(pavementSizeMinField, constraints);

        // Row 2, Column 3.
        constraints.gridx++;
        JLabel pavementSizeToLabel = new JLabel(Messages.getString("to"));
        panel.add(pavementSizeToLabel, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        pavementSizeMaxField.setRequired(true);
        pavementSizeMaxField.setSelectAllTextOnGotFocus(true);
        pavementSizeMaxField.addVerifier(new VerifyIsPositiveInteger());
        pavementSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.pavementSizeMaxField.toolTip"));
        pavementSizeMaxField.setName(pavementSizeLabel.getText());
        panel.add(pavementSizeMaxField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderPavement")));

        return panel;
    }

    private JPanel setupFortifiedPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel numberFortifiedLabel = new JLabel(Messages.getString("RandomMapDialog.labFortifiedSpots"));
        panel.add(numberFortifiedLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        fortifiedMinField.setRequired(true);
        fortifiedMinField.setSelectAllTextOnGotFocus(true);
        fortifiedMinField.addVerifier(new VerifyIsPositiveInteger());
        fortifiedMinField.setToolTipText(Messages.getString("RandomMapDialog.fortifiedMinField.toolTip"));
        fortifiedMinField.setName(numberFortifiedLabel.getText());
        panel.add(fortifiedMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel numberFortifiedToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberFortifiedToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        fortifiedMaxField.setRequired(true);
        fortifiedMaxField.setSelectAllTextOnGotFocus(true);
        fortifiedMaxField.addVerifier(new VerifyIsPositiveInteger());
        fortifiedMaxField.setToolTipText(Messages.getString("RandomMapDialog.fortifiedMaxField.toolTip"));
        fortifiedMaxField.setName(numberFortifiedLabel.getText());
        panel.add(fortifiedMaxField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel sizeFortifiedLabel = new JLabel(Messages.getString("RandomMapDialog.labFortifiedSize"));
        panel.add(sizeFortifiedLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        fortifiedSizeMinField.setRequired(true);
        fortifiedSizeMinField.setSelectAllTextOnGotFocus(true);
        fortifiedSizeMinField.addVerifier(new VerifyIsPositiveInteger());
        fortifiedSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.fortifiedSizeMinField.toolTip"));
        fortifiedSizeMinField.setName(sizeFortifiedLabel.getText());
        panel.add(fortifiedSizeMinField, constraints);

        // Row 2, Column 3.
        constraints.gridx++;
        JLabel sizeFortifiedToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeFortifiedToLabel, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        fortifiedSizeMaxField.setRequired(true);
        fortifiedSizeMaxField.setSelectAllTextOnGotFocus(true);
        fortifiedSizeMaxField.addVerifier(new VerifyIsPositiveInteger());
        fortifiedSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.fortifiedSizeMaxField.toolTip"));
        fortifiedSizeMaxField.setName(sizeFortifiedLabel.getText());
        panel.add(fortifiedSizeMaxField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 2),
                                         Messages.getString("RandomMapDialog.borderFortified")));

        return panel;
    }

    private JPanel setupCityPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel cityTypeLabel = new JLabel(Messages.getString("RandomMapDialog.labCity"));
        panel.add(cityTypeLabel, constraints);

        // Row 1, Column 2-4.
        constraints.gridx++;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        cityTypeCombo.setToolTipText(Messages.getString("RandomMapDialog.cityTypeCombo.toolTip"));
        cityTypeCombo.setName(cityTypeLabel.getText());
        panel.add(cityTypeCombo, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        JLabel cityBlocksLabel = new JLabel(Messages.getString("RandomMapDialog.labCityBlocks"));
        panel.add(cityBlocksLabel, constraints);

        // Row 2, Column 2-4.
        constraints.gridx++;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        cityBlocks.setRequired(true);
        cityBlocks.setSelectAllTextOnGotFocus(true);
        cityBlocks.addVerifier(new VerifyIsPositiveInteger());
        cityBlocks.setToolTipText(Messages.getString("RandomMapDialog.cityBlocks.toolTip"));
        cityBlocks.setName(cityBlocksLabel.getText());
        panel.add(cityBlocks, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        JLabel cityCFLabel = new JLabel(Messages.getString("RandomMapDialog.labCityCF"));
        panel.add(cityCFLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.weightx = 0.5;
        cityCFMinField.setRequired(true);
        cityCFMinField.setSelectAllTextOnGotFocus(true);
        cityCFMinField.addVerifier(new VerifyInRange(1, 150, true));
        cityCFMinField.setToolTipText(Messages.getString("RandomMapDialog.cityCFMinField.toolTip"));
        cityCFMinField.setName(cityCFLabel.getText());
        panel.add(cityCFMinField, constraints);

        // Row 3, Column 3.
        constraints.gridx++;
        constraints.weightx = 0;
        JLabel cityCFToLabel = new JLabel(Messages.getString("to"));
        panel.add(cityCFToLabel, constraints);

        // Row 3, Column 4.
        constraints.gridx++;
        constraints.weightx = 0.5;
        cityCFMaxField.setRequired(true);
        cityCFMaxField.setSelectAllTextOnGotFocus(true);
        cityCFMaxField.addVerifier(new VerifyInRange(1, 150, true));
        cityCFMaxField.setToolTipText(Messages.getString("RandomMapDialog.cityCFMaxField.toolTip"));
        cityCFMaxField.setName(cityCFLabel.getText());
        panel.add(cityCFMaxField, constraints);

        // Row 4, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel cityDensityLabel = new JLabel(Messages.getString("RandomMapDialog.labCityDensity"));
        panel.add(cityDensityLabel, constraints);

        // Row 4, Column 2-4.
        constraints.gridx++;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        cityDensityField.setRequired(true);
        cityDensityField.setSelectAllTextOnGotFocus(true);
        cityDensityField.addVerifier(new VerifyInRange(1, 100, true));
        cityDensityField.setToolTipText(Messages.getString("RandomMapDialog.cityDensityField.toolTip"));
        cityDensityField.setName(cityDensityLabel.getText());
        panel.add(cityDensityField, constraints);

        // Row 5, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        JLabel cityFloorsLabel = new JLabel(Messages.getString("RandomMapDialog.labCityFloors"));
        panel.add(cityFloorsLabel, constraints);

        // Row 5, Column 2.
        constraints.gridx++;
        constraints.weightx = 0.5;
        cityFloorsMinField.setRequired(true);
        cityFloorsMinField.setSelectAllTextOnGotFocus(true);
        cityFloorsMinField.addVerifier(new VerifyInRange(1, 100, true));
        cityFloorsMinField.setToolTipText(Messages.getString("RandomMapDialog.cityFloorsMinField.toolTip"));
        cityFloorsMinField.setName(cityFloorsLabel.getText());
        panel.add(cityFloorsMinField, constraints);

        // Row 5, Column 3.
        constraints.gridx++;
        constraints.weightx = 0;
        JLabel cityFloorsToLabel = new JLabel(Messages.getString("to"));
        panel.add(cityFloorsToLabel, constraints);

        // Row 5, Column 4.
        constraints.gridx++;
        constraints.weightx = 0.5;
        cityFloorsMaxField.setRequired(true);
        cityFloorsMaxField.setSelectAllTextOnGotFocus(true);
        cityFloorsMaxField.addVerifier(new VerifyInRange(1, 100, true));
        cityFloorsMaxField.setToolTipText(Messages.getString("RandomMapDialog.cityFloorsMaxField.toolTip"));
        cityFloorsMaxField.setName(cityFloorsLabel.getText());
        panel.add(cityFloorsMaxField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderCity")));

        // Row 6, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel townSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labTownSize"));
        panel.add(townSizeLabel, constraints);

        // Row 6, Column 2-4.
        constraints.gridx++;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 1;
        townSizeField.setRequired(true);
        townSizeField.setSelectAllTextOnGotFocus(true);
        townSizeField.addVerifier(new VerifyInRange(1, 100, true));
        townSizeField.setToolTipText(Messages.getString("RandomMapDialog.townSizeField.toolTip"));
        townSizeField.setName(townSizeLabel.getText());
        panel.add(townSizeField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderCity")));

        return panel;
    }

    private JScrollPane setupEffectsPanel() {
        GridBagLayout layout = new GridBagLayout();
        JPanel panel = new JPanel(layout);
        GridBagConstraints constraints = setupConstraints();

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel droughtLabel = new JLabel(Messages.getString("RandomMapDialog.labProbDrought"));
        panel.add(droughtLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        droughtChanceField.setRequired(true);
        droughtChanceField.setSelectAllTextOnGotFocus(true);
        droughtChanceField.addVerifier(new VerifyInRange(0, 100, true));
        droughtChanceField.setToolTipText(Messages.getString("RandomMapDialog.droughtChanceField.toolTip"));
        droughtChanceField.setName(droughtLabel.getText());
        panel.add(droughtChanceField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel fireLabel = new JLabel(Messages.getString("RandomMapDialog.labProbFire"));
        panel.add(fireLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        fireChanceField.setRequired(true);
        fireChanceField.setSelectAllTextOnGotFocus(true);
        fireChanceField.addVerifier(new VerifyInRange(0, 100, true));
        fireChanceField.setToolTipText(Messages.getString("RandomMapDialog.fireChanceField.toolTip"));
        fireChanceField.setName(fireLabel.getText());
        panel.add(fireChanceField, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel floodLabel = new JLabel(Messages.getString("RandomMapDialog.labProbFlood"));
        panel.add(floodLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        floodChanceField.setRequired(true);
        floodChanceField.setSelectAllTextOnGotFocus(true);
        floodChanceField.addVerifier(new VerifyInRange(0, 100, true));
        floodChanceField.setToolTipText(Messages.getString("RandomMapDialog.floodChanceField.toolTip"));
        floodChanceField.setName(floodLabel.getText());
        panel.add(floodChanceField, constraints);

        // Row 4, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel fxLabel = new JLabel(Messages.getString("RandomMapDialog.labFxMod"));
        panel.add(fxLabel, constraints);

        // Row 4, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        specialFxField.setRequired(true);
        specialFxField.setSelectAllTextOnGotFocus(true);
        specialFxField.addVerifier(new VerifyIsInteger());
        specialFxField.setToolTipText(Messages.getString("RandomMapDialog.specialFxField.toolTip"));
        specialFxField.setName(fxLabel.getText());
        panel.add(specialFxField, constraints);

        return new JScrollPane(panel);
    }

    private JScrollPane setupNaturalFeaturesPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        panel.add(setupRoughsPanel(), constraints);

        // Row 2
        constraints.gridy++;
        panel.add(setupSandsPanel(), constraints);

        // Row 3
        constraints.gridy++;
        panel.add(setupSwampsPanel(), constraints);

        // Row 4
        constraints.gridy++;
        constraints.weighty = 1;
        panel.add(setupWoodsPanel(), constraints);

        return new JScrollPane(panel);
    }

    private JPanel setupWoodsPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel numberWoodsLabel = new JLabel(Messages.getString("RandomMapDialog.labForestSpots"));
        panel.add(numberWoodsLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        woodsMinField.setRequired(true);
        woodsMinField.setSelectAllTextOnGotFocus(true);
        woodsMinField.addVerifier(new VerifyIsPositiveInteger());
        woodsMinField.setToolTipText(Messages.getString("RandomMapDialog.woodsMinField.toolTip"));
        woodsMinField.setName(numberWoodsLabel.getText());
        panel.add(woodsMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel numberWoodsToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberWoodsToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        woodsMaxField.setRequired(true);
        woodsMaxField.setSelectAllTextOnGotFocus(true);
        woodsMaxField.addVerifier(new VerifyIsPositiveInteger());
        woodsMaxField.setToolTipText(Messages.getString("RandomMapDialog.woodsMaxField.toolTip"));
        woodsMaxField.setName(numberWoodsLabel.getText());
        panel.add(woodsMaxField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel sizesWoodsLabel = new JLabel(Messages.getString("RandomMapDialog.labSwampSize"));
        panel.add(sizesWoodsLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        woodsMinSizeField.setRequired(true);
        woodsMinSizeField.setSelectAllTextOnGotFocus(true);
        woodsMinSizeField.addVerifier(new VerifyIsPositiveInteger());
        woodsMinSizeField.setToolTipText(Messages.getString("RandomMapDialog.woodsMinSizeField.toolTip"));
        woodsMinSizeField.setName(sizesWoodsLabel.getText());
        panel.add(woodsMinSizeField, constraints);

        // Row 2, Column 3.
        constraints.gridx++;
        JLabel sizeWoodsToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeWoodsToLabel, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        woodsMaxSizeField.setRequired(true);
        woodsMaxSizeField.setSelectAllTextOnGotFocus(true);
        woodsMaxSizeField.addVerifier(new VerifyIsPositiveInteger());
        woodsMaxSizeField.setToolTipText(Messages.getString("RandomMapDialog.woodsMaxSizeField.toolTip"));
        woodsMaxSizeField.setName(sizesWoodsLabel.getText());
        panel.add(woodsMaxSizeField, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel heavyWoodsLabel = new JLabel(Messages.getString("RandomMapDialog.labProbHeavy"));
        panel.add(heavyWoodsLabel, constraints);

        // Row 3, Columns 2-4
        constraints.gridx++;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 1;
        woodsHeavyChanceField.setRequired(true);
        woodsHeavyChanceField.setSelectAllTextOnGotFocus(true);
        woodsHeavyChanceField.addVerifier(new VerifyInRange(0, 100, true));
        woodsHeavyChanceField.setToolTipText(Messages.getString("RandomMapDialog.woodsHeavyChanceField.toolTip"));
        woodsHeavyChanceField.setName(heavyWoodsLabel.getText());
        panel.add(woodsHeavyChanceField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderWoods")));

        return panel;
    }

    private JPanel setupSwampsPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel numberSwampsLabel = new JLabel(Messages.getString("RandomMapDialog.labSwampSpots"));
        panel.add(numberSwampsLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        swampsMinField.setRequired(true);
        swampsMinField.setSelectAllTextOnGotFocus(true);
        swampsMinField.addVerifier(new VerifyIsPositiveInteger());
        swampsMinField.setToolTipText(Messages.getString("RandomMapDialog.swampsMinField.toolTip"));
        swampsMinField.setName(numberSwampsLabel.getText());
        panel.add(swampsMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel numberSwampsToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberSwampsToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        swampsMaxField.setRequired(true);
        swampsMaxField.setSelectAllTextOnGotFocus(true);
        swampsMaxField.addVerifier(new VerifyIsPositiveInteger());
        swampsMaxField.setToolTipText(Messages.getString("RandomMapDialog.swampsMaxField.toolTip"));
        swampsMaxField.setName(numberSwampsLabel.getText());
        panel.add(swampsMaxField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel sizeSwampsLabel = new JLabel(Messages.getString("RandomMapDialog.labSwampSize"));
        panel.add(sizeSwampsLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        swampsMinSizeField.setRequired(true);
        swampsMinSizeField.setSelectAllTextOnGotFocus(true);
        swampsMinSizeField.addVerifier(new VerifyIsPositiveInteger());
        swampsMinSizeField.setToolTipText(Messages.getString("RandomMapDialog.swampsMinSizeField.toolTip"));
        swampsMinSizeField.setName(sizeSwampsLabel.getText());
        panel.add(swampsMinSizeField, constraints);

        // Row 2, Column 3.
        constraints.gridx++;
        JLabel sizeSwampsToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeSwampsToLabel, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        swampsMaxSizeField.setRequired(true);
        swampsMaxSizeField.setSelectAllTextOnGotFocus(true);
        swampsMaxSizeField.addVerifier(new VerifyIsPositiveInteger());
        swampsMaxSizeField.setToolTipText(Messages.getString("RandomMapDialog.swampsMaxSizeField.toolTip"));
        swampsMaxSizeField.setName(sizeSwampsLabel.getText());
        panel.add(swampsMaxSizeField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderSwamp")));

        return panel;
    }

    private JPanel setupSandsPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel numberSandsLabel = new JLabel(Messages.getString("RandomMapDialog.labSandSpots"));
        panel.add(numberSandsLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        sandsMinField.setRequired(true);
        sandsMinField.setSelectAllTextOnGotFocus(true);
        sandsMinField.addVerifier(new VerifyIsPositiveInteger());
        sandsMinField.setToolTipText(Messages.getString("RandomMapDialog.sandsMinField.toolTip"));
        sandsMinField.setName(numberSandsLabel.getText());
        panel.add(sandsMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel numberSandsToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberSandsToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        sandsMaxField.setRequired(true);
        sandsMaxField.setSelectAllTextOnGotFocus(true);
        sandsMaxField.addVerifier(new VerifyIsPositiveInteger());
        sandsMaxField.setToolTipText(Messages.getString("RandomMapDialog.sandsMaxField.toolTip"));
        sandsMaxField.setName(numberSandsLabel.getText());
        panel.add(sandsMaxField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.gridy++;
        JLabel sizeSandsLabel = new JLabel(Messages.getString("RandomMapDialog.labSandSize"));
        panel.add(sizeSandsLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        sandsSizeMinField.setRequired(true);
        sandsSizeMinField.setSelectAllTextOnGotFocus(true);
        sandsSizeMinField.addVerifier(new VerifyIsPositiveInteger());
        sandsSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.sandsSizeMinField.toolTip"));
        sandsSizeMinField.setName(sizeSandsLabel.getText());
        panel.add(sandsSizeMinField, constraints);

        // Row 2, Column 3.
        constraints.gridx++;
        JLabel sizeSandsToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeSandsToLabel, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        sandsSizeMaxField.setRequired(true);
        sandsSizeMaxField.setSelectAllTextOnGotFocus(true);
        sandsSizeMaxField.addVerifier(new VerifyIsPositiveInteger());
        sandsSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.sandsSizeMaxField.toolTip"));
        sandsSizeMaxField.setName(sizeSandsLabel.getText());
        panel.add(sandsSizeMaxField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderSand")));

        return panel;
    }

    private JPanel setupRoughsPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        JLabel numberRoughsLabel = new JLabel(Messages.getString("RandomMapDialog.labRoughSpots"));
        panel.add(numberRoughsLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        roughsMinField.setRequired(true);
        roughsMinField.setSelectAllTextOnGotFocus(true);
        roughsMinField.addVerifier(new VerifyIsPositiveInteger());
        roughsMinField.setToolTipText(Messages.getString("RandomMapDialog.roughsMinField.toolTip"));
        roughsMinField.setName(numberRoughsLabel.getText());
        panel.add(roughsMinField, constraints);

        // Row 1, Column 3.
        constraints.gridx++;
        JLabel numberRoughsToLabel = new JLabel(Messages.getString("to"));
        panel.add(numberRoughsToLabel, constraints);

        // Row 1, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        roughsMaxField.setRequired(true);
        roughsMaxField.setSelectAllTextOnGotFocus(true);
        roughsMaxField.addVerifier(new VerifyIsPositiveInteger());
        roughsMaxField.setToolTipText(Messages.getString("RandomMapDialog.roughsMaxField.toolTip"));
        roughsMaxField.setName(numberRoughsLabel.getText());
        panel.add(roughsMaxField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        JLabel sizeRoughsLabel = new JLabel(Messages.getString("RandomMapDialog.labRoughSize"));
        panel.add(sizeRoughsLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        roughsMinSizeField.setRequired(true);
        roughsMinSizeField.setSelectAllTextOnGotFocus(true);
        roughsMinSizeField.addVerifier(new VerifyIsPositiveInteger());
        roughsMinSizeField.setToolTipText(Messages.getString("RandomMapDialog.roughsMinSizeField.toolTip"));
        roughsMinSizeField.setName(sizeRoughsLabel.getText());
        panel.add(roughsMinSizeField, constraints);

        // Row 2, Column 3.
        constraints.gridx++;
        JLabel sizeRoughsToLabel = new JLabel(Messages.getString("to"));
        panel.add(sizeRoughsToLabel, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        roughsMaxSizeField.setRequired(true);
        roughsMaxSizeField.setSelectAllTextOnGotFocus(true);
        roughsMaxSizeField.addVerifier(new VerifyIsPositiveInteger());
        roughsMaxSizeField.setToolTipText(Messages.getString("RandomMapDialog.roughsMaxSizeField.toolTip"));
        roughsMaxSizeField.setName(sizeRoughsLabel.getText());
        panel.add(roughsMaxSizeField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderRough")));

        return panel;
    }

    private JScrollPane setupElevationPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        panel.add(setupElevationGeneralPanel(), constraints);

        // Row 2
        constraints.gridy++;
        panel.add(setupMountainsPanel(), constraints);

        // Row 3
        constraints.gridy++;
        constraints.weighty = 1;
        panel.add(setupCratersPanel(), constraints);

        return new JScrollPane(panel);
    }

    private JPanel setupCratersPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        final JLabel craterChanceLabel = new JLabel(Messages.getString("RandomMapDialog.labProbCrater"));
        panel.add(craterChanceLabel, constraints);

        // Row 1, Columns 2-5.
        constraints.gridx++;
        constraints.gridwidth = 4;
        constraints.weightx = 1;
        craterChanceField.setRequired(true);
        craterChanceField.setSelectAllTextOnGotFocus(true);
        craterChanceField.addVerifier(new VerifyInRange(0, 100, true));
        craterChanceField.setToolTipText(Messages.getString("RandomMapDialog.craterChanceField.toolTip"));
        craterChanceField.setName(craterChanceLabel.getText());
        panel.add(craterChanceField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel craterNumberLabel = new JLabel(Messages.getString("RandomMapDialog.labMaxCraters"));
        panel.add(craterNumberLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        constraints.weightx = 0.5;
        craterAmountMinField.setRequired(true);
        craterAmountMinField.setSelectAllTextOnGotFocus(true);
        craterAmountMinField.addVerifier(new VerifyIsPositiveInteger());
        craterAmountMinField.setToolTipText(Messages.getString("RandomMapDialog.craterAmountMinField.toolTip"));
        craterAmountMinField.setName(craterNumberLabel.getText());
        panel.add(craterAmountMinField, constraints);

        // Row 2, Column 3.
        constraints.gridx++;
        constraints.weightx = 0;
        final JLabel craterNumberToField = new JLabel(Messages.getString("to"));
        panel.add(craterNumberToField, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        constraints.weightx = 0.5;
        craterAmountMaxField.setRequired(true);
        craterAmountMaxField.setSelectAllTextOnGotFocus(true);
        craterAmountMaxField.addVerifier(new VerifyIsPositiveInteger());
        craterAmountMaxField.setToolTipText(Messages.getString("RandomMapDialog.craterAmountMaxField.toolTip"));
        craterAmountMaxField.setName(craterNumberLabel.getText());
        panel.add(craterAmountMaxField, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel craterSizeLabel = new JLabel(Messages.getString("RandomMapDialog.labRadius"));
        panel.add(craterSizeLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.weightx = 0.5;
        craterSizeMinField.setRequired(true);
        craterSizeMinField.setSelectAllTextOnGotFocus(true);
        craterSizeMinField.addVerifier(new VerifyIsPositiveInteger());
        craterSizeMinField.setToolTipText(Messages.getString("RandomMapDialog.craterSizeMinField.toolTip"));
        craterSizeMinField.setName(craterSizeLabel.getText());
        panel.add(craterSizeMinField, constraints);

        // Row 3, Column 3.
        constraints.gridx++;
        constraints.weightx = 0;
        final JLabel craterSizeToField = new JLabel(Messages.getString("to"));
        panel.add(craterSizeToField, constraints);

        // Row 3, Column 4.
        constraints.gridx++;
        constraints.weightx = 0.5;
        constraints.weighty = 1;
        craterSizeMaxField.setRequired(true);
        craterSizeMaxField.setSelectAllTextOnGotFocus(true);
        craterSizeMaxField.addVerifier(new VerifyIsPositiveInteger());
        craterSizeMaxField.setToolTipText(Messages.getString("RandomMapDialog.craterSizeMaxField.toolTip"));
        craterSizeMaxField.setName(craterSizeLabel.getText());
        panel.add(craterSizeMaxField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderCrater")));

        return panel;
    }

    private JPanel setupMountainsPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        final JLabel peaksLabel = new JLabel(Messages.getString("RandomMapDialog.labMountainPeaks"));
        panel.add(peaksLabel, constraints);

        // Row 1, Column 2-5.
        constraints.gridx++;
        constraints.gridwidth = 4;
        constraints.weightx = 1;
        elevationPeaksField.setRequired(true);
        elevationPeaksField.setSelectAllTextOnGotFocus(true);
        elevationPeaksField.addVerifier(new VerifyIsPositiveInteger());
        elevationPeaksField.setToolTipText(Messages.getString("RandomMapDialog.elevationPeaksField.toolTip"));
        elevationPeaksField.setName(peaksLabel.getText());
        panel.add(elevationPeaksField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        final JLabel mountainStyleLabel = new JLabel(Messages.getString("RandomMapDialog.labMountainStyle"));
        panel.add(mountainStyleLabel, constraints);

        // Row 2, Columns 2-5.
        constraints.gridx++;
        constraints.gridwidth = 4;
        constraints.weightx = 1;
        mountainStyleCombo.setToolTipText(Messages.getString("RandomMapDialog.mountainStyleCombo.toolTip"));
        panel.add(mountainStyleCombo, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        final JLabel mountainHeightLabel = new JLabel(Messages.getString("RandomMapDialog.labMountainHeight"));
        panel.add(mountainHeightLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.weightx = 0.5;
        mountainHeightMinField.setRequired(true);
        mountainHeightMinField.setSelectAllTextOnGotFocus(true);
        mountainHeightMinField.addVerifier(new VerifyIsPositiveInteger());
        mountainHeightMinField.setToolTipText(Messages.getString("RandomMapDialog.mountainHeightMinField.toolTip"));
        mountainHeightMinField.setName(mountainHeightLabel.getText());
        panel.add(mountainHeightMinField, constraints);

        // Row 3, Column 3.
        constraints.gridx++;
        constraints.weightx = 0;
        final JLabel mountainHeightToLabel = new JLabel(Messages.getString("to"));
        panel.add(mountainHeightToLabel, constraints);

        // Row 3, Column 4.
        constraints.gridx++;
        constraints.weightx = 0.5;
        mountainHeightMaxField.setRequired(true);
        mountainHeightMaxField.setSelectAllTextOnGotFocus(true);
        mountainHeightMaxField.addVerifier(new VerifyIsPositiveInteger());
        mountainHeightMaxField.setToolTipText(Messages.getString("RandomMapDialog.mountainHeightMaxField.toolTip"));
        mountainHeightMaxField.setName(mountainHeightLabel.getText());
        panel.add(mountainHeightMaxField, constraints);

        // Row 4, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        final JLabel mountainWidthLabel = new JLabel(Messages.getString("RandomMapDialog.labMountainWidth"));
        panel.add(mountainWidthLabel, constraints);

        // Row 4, Column 2.
        constraints.gridx++;
        constraints.weightx = 0.5;
        mountainWidthMinField.setRequired(true);
        mountainWidthMinField.setSelectAllTextOnGotFocus(true);
        mountainWidthMinField.addVerifier(new VerifyIsPositiveInteger());
        mountainWidthMinField.setToolTipText(Messages.getString("RandomMapDialog.mountainWidthMinField.toolTip"));
        mountainWidthMinField.setName(mountainWidthLabel.getText());
        panel.add(mountainWidthMinField, constraints);

        // Row 4, Column 3.
        constraints.gridx++;
        constraints.weightx = 0;
        final JLabel mountainWidthToLabel = new JLabel(Messages.getString("to"));
        panel.add(mountainWidthToLabel, constraints);

        // Row 4, Column 4.
        constraints.gridx++;
        constraints.weightx = 0.5;
        constraints.weighty = 1;
        mountainWidthMaxField.setRequired(true);
        mountainWidthMaxField.setSelectAllTextOnGotFocus(true);
        mountainWidthMaxField.addVerifier(new VerifyIsPositiveInteger());
        mountainWidthMaxField.setToolTipText(Messages.getString("RandomMapDialog.mountainWidthMaxField.toolTip"));
        mountainWidthMaxField.setName(mountainWidthLabel.getText());
        panel.add(mountainWidthMaxField, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderMountain")));

        return panel;
    }

    private JPanel setupElevationGeneralPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = setupConstraints();
        JPanel panel = new JPanel(layout);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        final JLabel algorithmLabel = new JLabel(Messages.getString("RandomMapDialog.labAlgorithmToUse"));
        panel.add(algorithmLabel, constraints);

        // Row 1, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        elevationAlgorithmField.setRequired(true);
        elevationAlgorithmField.setSelectAllTextOnGotFocus(true);
        elevationAlgorithmField.addVerifier(new VerifyInRange(0, BoardUtilities.getAmountElevationGenerators() - 1, true));
        elevationAlgorithmField.setToolTipText(Messages.getString("RandomMapDialog.elevationAlgorithmField.toolTip"));
        elevationAlgorithmField.setName(algorithmLabel.getText());
        panel.add(elevationAlgorithmField, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel hillinessLabel = new JLabel(Messages.getString("RandomMapDialog.labHilliness"));
        panel.add(hillinessLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        hillinessField.setRequired(true);
        hillinessField.setSelectAllTextOnGotFocus(true);
        hillinessField.addVerifier(new VerifyInRange(0, 99, true));
        hillinessField.setToolTipText(Messages.getString("RandomMapDialog.hillinessField.toolTip"));
        hillinessField.setName(hillinessLabel.getText());
        panel.add(hillinessField, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel elevationRangeLabel = new JLabel(Messages.getString("RandomMapDialog.labRange"));
        panel.add(elevationRangeLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        elevationRangeField.setRequired(true);
        elevationRangeField.setSelectAllTextOnGotFocus(true);
        elevationRangeField.addVerifier(new VerifyIsPositiveInteger());
        elevationRangeField.setToolTipText(Messages.getString("RandomMapDialog.elevationRangeField.toolTip"));
        elevationRangeField.setName(elevationRangeLabel.getText());
        panel.add(elevationRangeField, constraints);

        // Row 4, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel elevationCliffsLabel = new JLabel(Messages.getString("RandomMapDialog.labCliffs"));
        panel.add(elevationCliffsLabel, constraints);

        // Row 4, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        elevationCliffsField.setRequired(true);
        elevationCliffsField.setSelectAllTextOnGotFocus(true);
        elevationCliffsField.addVerifier(new VerifyInRange(0, 100, true));
        elevationCliffsField.setToolTipText(Messages.getString("RandomMapDialog.elevationCliffsField.toolTip"));
        elevationCliffsField.setName(elevationCliffsLabel.getText());
        panel.add(elevationCliffsField, constraints);

        // Row 5, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel elevationInversionLabel = new JLabel(Messages.getString("RandomMapDialog.labProbInvert"));
        panel.add(elevationInversionLabel, constraints);

        // Row 5, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        elevationInversionField.setRequired(true);
        elevationInversionField.setSelectAllTextOnGotFocus(true);
        elevationInversionField.addVerifier(new VerifyInRange(0, 99, true));
        elevationInversionField.setToolTipText(Messages.getString("RandomMapDialog.elevationInversionField.toolTip"));
        elevationInversionField.setName(elevationInversionLabel.getText());
        panel.add(elevationInversionField, constraints);

        // Row 6, Columns 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0;
        final JLabel invertNegativeLabel = new JLabel(Messages.getString("RandomMapDialog.labInvertNegative"));
        panel.add(invertNegativeLabel, constraints);

        // Row 6, Column 2.
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        invertNegativeCheck.setText("");
        invertNegativeCheck.setToolTipText(Messages.getString("RandomMapDialog.invertNegativeCheck.toolTip"));
        panel.add(invertNegativeCheck, constraints);

        // Set up border.
        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1),
                                         Messages.getString("RandomMapDialog.borderGeneral")));

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

        private static MountainStyle getMountainStyle(String description) {
            for (MountainStyle ms : values()) {
                if (ms.getDescription().equals(description)) {
                    return ms;
                }
            }
            return null;
        }

        private static MountainStyle getMountainStyle(int code) {
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
        String result = field.verifyText();
        if (result != null) {
            result = field.getName() + ": " + result;
            new RuntimeException(result).printStackTrace();
            field.requestFocus();
            showDataValidationError(result);
        }
        return (result == null);
    }

    // Takes in a min and max field, makes shure the data is generally valid in each then compares them to make sure
    // the minimum value does not exceed the maximum.
    private boolean isMinMaxVerified(VerifiableTextField min, VerifiableTextField max) {
        if (!isFieldVerified(min) || !isFieldVerified(max)) {
            return false;
        }

        final String INVALID = "Minimum cannot exceed maximum.";
        if (min.getAsInt() > max.getAsInt()) {
            new RuntimeException(INVALID).printStackTrace();
            min.setOldToolTip(min.getToolTipText());
            max.setOldToolTip(max.getToolTipText());
            min.setBackground(VerifiableTextField.BK_INVALID);
            max.setBackground(VerifiableTextField.BK_INVALID);
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
        newMapSettings.setSwampParams(swampsMinField.getAsInt(),
                                      swampsMaxField.getAsInt(),
                                      swampsMinSizeField.getAsInt(),
                                      swampsMaxSizeField.getAsInt());
        newMapSettings.setForestParams(woodsMinField.getAsInt(),
                                       woodsMaxField.getAsInt(),
                                       woodsMinSizeField.getAsInt(),
                                       woodsMaxSizeField.getAsInt(),
                                       woodsHeavyChanceField.getAsInt());
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
