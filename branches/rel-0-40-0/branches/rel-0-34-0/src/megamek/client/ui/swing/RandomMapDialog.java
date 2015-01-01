/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.SimpleLine;
import megamek.common.MapSettings;

public class RandomMapDialog extends JDialog implements ActionListener,
        FocusListener {
    /**
     *
     */
    private static final long serialVersionUID = -1932447917045308611L;
    private static final String NONE = Messages
            .getString("RandomMapDialog.elevNONE"); //$NON-NLS-1$
    private static final String LOW = Messages
            .getString("RandomMapDialog.elevLow"); //$NON-NLS-1$
    private static final String MEDIUM = Messages
            .getString("RandomMapDialog.elevMedium"); //$NON-NLS-1$
    private static final String HIGH = Messages
            .getString("RandomMapDialog.elevHigh"); //$NON-NLS-1$

    private static final String INVALID_SETTING = Messages
            .getString("RandomMapDialog.InvalidSetting"); //$NON-NLS-1$

    private static final int NORMAL_LINE_WIDTH = 195;
    private static final int ADVANCED_LINE_WIDTH = 295;

    private JScrollPane scrAll;

    private JButton butOK;
    private JButton butAdvanced;
    private JButton butSave;
    private JButton butLoad;

    private JPanel panButtons;
    private JPanel panOptions;

    private JLabel labBoardSize;
    private JTextField texBoardWidth;
    private JTextField texBoardHeight;

    private JComboBox choElevation;
    private JComboBox choCliffs;
    private JComboBox choWoods;
    private JComboBox choLakes;
    private JComboBox choPavement;
    private JComboBox choRubble;
    private JComboBox choFortified;
    private JComboBox choIce;
    private JComboBox choRough;
    private JComboBox choRoads;
    private JComboBox choRivers;
    private JComboBox choSwamp;
    private JComboBox choCraters;
    private JComboBox choCity;
    private JComboBox choMountain;

    private JLabel labElevation;
    private JLabel labCliffs;
    private JLabel labWoods;
    private JLabel labLakes;
    private JLabel labPavement;
    private JLabel labRubble;
    private JLabel labFortified;
    private JLabel labIce;
    private JLabel labRough;
    private JLabel labRoads;
    private JLabel labRivers;
    private JLabel labSwamp;
    private JLabel labTheme;
    private JLabel labCraters;
    private JLabel labCity;
    private JLabel labMountain = null;

    private SimpleLine slElevation;
    private SimpleLine slCliffs;
    private SimpleLine slWoods;
    private SimpleLine slLakes;
    private SimpleLine slPavement;
    private SimpleLine slRubble;
    private SimpleLine slFortified;
    private SimpleLine slIce;
    private SimpleLine slRough;
    private SimpleLine slRoads;
    private SimpleLine slRivers;
    private SimpleLine slSwamp;
    private SimpleLine slBoardSize;
    private SimpleLine slCraters;
    private SimpleLine slCity;
    private SimpleLine slMountain;

    private SimpleLine slElevationAd;
    private SimpleLine slWoodsAd;
    private SimpleLine slLakesAd;
    private SimpleLine slPavementAd;
    private SimpleLine slRubbleAd;
    private SimpleLine slFortifiedAd;
    private SimpleLine slIceAd;
    private SimpleLine slRoughAd;
    private SimpleLine slRoadsAd;
    private SimpleLine slRiversAd;
    private SimpleLine slSwampAd;
    private SimpleLine slBoardSizeAd;
    private SimpleLine slCratersAd;
    private SimpleLine slCityAd;
    private SimpleLine slInvertNegativeAd;

    private JTextField texTheme;

    /**
     * how much hills there should be, Range 0..100
     */
    private JLabel labHilliness;
    private JTextField texHilliness;
    /**
     * invert negative terrain? 1 yes, 0 no
     */
    private JLabel labInvertNegative;
    private JTextField texInvertNegative;
    /**
     * Maximum level of the map
     */
    private JLabel labRange;
    private JTextField texRange;
    private JLabel labProbInvert;
    private JTextField texProbInvert;
    private JLabel labCliffsAd;
    private JTextField texCliffs;

    /**
     * how much Lakes at least
     */
    private JLabel labWaterSpots;
    private JTextField texMinWaterSpots;
    /**
     * how much Lakes at most
     */
    private JTextField texMaxWaterSpots;
    /**
     * minimum size of a lake
     */
    private JLabel labWaterSize;
    private JTextField texMinWaterSize;
    /**
     * maximum Size of a lake
     */
    private JTextField texMaxWaterSize;
    /**
     * probability for water deeper than lvl1, Range 0..100
     */
    private JLabel labProbDeep;
    private JTextField texProbDeep;

    /**
     * how much forests at least
     */
    private JLabel labForestSpots;
    private JTextField texMinForestSpots;
    /**
     * how much forests at most
     */
    private JTextField texMaxForestSpots;
    /**
     * minimum size of a forest
     */
    private JLabel labForestSize;
    private JTextField texMinForestSize;
    /**
     * maximum Size of a forest
     */
    private JTextField texMaxForestSize;
    /**
     * probability for heavy wood, Range 0..100
     */
    private JLabel labProbHeavy;
    private JTextField texProbHeavy;

    /**
     * rough
     */
    private JLabel labRoughSpots;
    private JTextField texMinRoughSpots;
    private JTextField texMaxRoughSpots;
    private JLabel labRoughSize;
    private JTextField texMinRoughSize;
    private JTextField texMaxRoughSize;

    /**
     * swamp
     */
    private JLabel labSwampSpots;
    private JTextField texMinSwampSpots;
    private JTextField texMaxSwampSpots;
    private JLabel labSwampSize;
    private JTextField texMinSwampSize;
    private JTextField texMaxSwampSize;

    /**
     * pavement/ice
     */
    private JLabel labPavementSpots;
    private JTextField texMinPavementSpots;
    private JTextField texMaxPavementSpots;
    private JLabel labPavementSize;
    private JTextField texMinPavementSize;
    private JTextField texMaxPavementSize;
    private JLabel labIceSpots;
    private JTextField texMinIceSpots;
    private JTextField texMaxIceSpots;
    private JLabel labIceSize;
    private JTextField texMinIceSize;
    private JTextField texMaxIceSize;

    /**
     * rubble / fortified
     */
    private JLabel labRubbleSpots;
    private JTextField texMinRubbleSpots;
    private JTextField texMaxRubbleSpots;
    private JLabel labRubbleSize;
    private JTextField texMinRubbleSize;
    private JTextField texMaxRubbleSize;
    private JLabel labFortifiedSpots;
    private JTextField texMinFortifiedSpots;
    private JTextField texMaxFortifiedSpots;
    private JLabel labFortifiedSize;
    private JTextField texMinFortifiedSize;
    private JTextField texMaxFortifiedSize;

    /**
     * probability for a road, range 0..100
     */
    private JLabel labProbRoad;
    private JTextField texProbRoad;
    /**
     * probability for a river, range 0..100
     */
    private JLabel labProbRiver;
    private JTextField texProbRiver;

    /* Craters */
    private JLabel labProbCrater;
    private JTextField texProbCrater;
    private JLabel labRadius;
    private JTextField texMinRadius;
    private JTextField texMaxRadius;
    private JLabel labMaxCraters;
    private JTextField texMaxCraters;
    private JTextField texMinCraters;

    /* FX */
    private JLabel labProbDrought;
    private JTextField texProbDrought;
    private JLabel labProbFire;
    private JTextField texProbFire;
    private JLabel labProbFlood;
    private JTextField texProbFlood;
    private JLabel labProbFreeze;
    private JTextField texProbFreeze;
    private JLabel labFxMod;
    private JTextField texFxMod;

    /* City */
    private JLabel labCityBlocks;
    private JLabel labCityCF;
    private JLabel labCityFloors;
    private JLabel labCityDensity;
    private JLabel labTownSize;
    private JTextField texCityBlocks;
    private JTextField texCityMinCF;
    private JTextField texCityMaxCF;
    private JTextField texCityMinFloors;
    private JTextField texCityMaxFloors;
    private JTextField texCityDensity;
    private JTextField texTownSize;

    // Mountain
    private JLabel labMountainPeaks;
    private JLabel labMountainHeight;
    private JLabel labMountainWidth;
    private JLabel labMountainStyle;
    private JTextField texMountainPeaks;
    private JTextField texMountainStyle;
    private JTextField texMountainHeightMin;
    private JTextField texMountainHeightMax;
    private JTextField texMountainWidthMin;
    private JTextField texMountainWidthMax;
    /**
     * Algorithm
     */
    private JLabel labAlgorithmToUse;
    private JTextField texAlgorithmToUse;

    GridBagLayout gridbag;

    private MapSettings mapSettings;
    private JFrame frame;
    private IMapSettingsObserver bsd;

    private boolean advanced;
    private boolean initiated;

    public RandomMapDialog(JFrame parent, IMapSettingsObserver bsd,
            MapSettings mapSettings) {
        super(parent, Messages.getString("RandomMapDialog.title"), true); //$NON-NLS-1$
        this.mapSettings = mapSettings;
        frame = parent;
        this.bsd = bsd;
        setResizable(true);

        createComponents();
        loadValues();

        getContentPane().setLayout(new BorderLayout());
        setupOptions();
        getContentPane().add(scrAll, BorderLayout.CENTER);
        setupButtons();
        getContentPane().add(panButtons, BorderLayout.SOUTH);

        validate();
        pack();
        setProperSize();

        butOK.requestFocus();

        setProperSize();
        setProperLocation();
        initiated = true;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butOK)) {
            if (applyValues()) {
                setVisible(false);
            }
        } else if (e.getSource().equals(butSave)) {
            JFileChooser fc = new JFileChooser("data" + File.separator
                    + "boards");
            fc.setLocation(frame.getLocation().x + 150,
                    frame.getLocation().y + 100);
            fc.setDialogTitle(Messages
                    .getString("RandomMapDialog.FileSaveDialog"));
            fc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File dir) {
                    return ((null != dir.getName()) && dir.getName().endsWith(
                            ".xml")); //$NON-NLS-1$
                }

                @Override
                public String getDescription() {
                    return ".xml";
                }
            });
            int returnVal = fc.showSaveDialog(frame);
            if ((returnVal != JFileChooser.APPROVE_OPTION)
                    || (fc.getSelectedFile() == null)) {
                // I want a file, y'know!
                return;
            }
            File selectedFile = fc.getSelectedFile();
            // make sure the file ends in xml
            if (!selectedFile.getName().toLowerCase().endsWith(".xml")) { //$NON-NLS-1$
                try {
                    selectedFile = new File(selectedFile.getCanonicalPath()
                            + ".xml"); //$NON-NLS-1$
                } catch (IOException ie) {
                    // failure!
                    return;
                }
            }
            try {
                mapSettings.save(new FileOutputStream(selectedFile));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource().equals(butLoad)) {
            JFileChooser fc = new JFileChooser("data" + File.separator
                    + "boards");
            fc.setLocation(frame.getLocation().x + 150,
                    frame.getLocation().y + 100);
            fc.setDialogTitle(Messages
                    .getString("RandomMapDialog.FileLoadDialog"));
            fc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File dir) {
                    return ((null != dir.getName()) && dir.getName().endsWith(
                            ".xml")); //$NON-NLS-1$
                }

                @Override
                public String getDescription() {
                    return ".xml";
                }
            });
            int returnVal = fc.showOpenDialog(frame);
            if ((returnVal != JFileChooser.APPROVE_OPTION)
                    || (fc.getSelectedFile() == null)) {
                // I want a file, y'know!
                return;
            }
            try {
                mapSettings.load(new FileInputStream(fc.getSelectedFile()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            loadValues();
            if (!advanced) {
                advanced = true;
                butAdvanced.setText(Messages
                        .getString("RandomMapDialog.Normal")); //$NON-NLS-1$
                setupOptions();
                setProperSize();
            }
        } else {
            advanced = !advanced;
            if (advanced) {
                butAdvanced.setText(Messages
                        .getString("RandomMapDialog.Normal")); //$NON-NLS-1$
            } else {
                butAdvanced.setText(Messages
                        .getString("RandomMapDialog.Advanced")); //$NON-NLS-1$
            }
            setupOptions();
            setProperSize();
        }
    }

    private void setupOptions() {
        panOptions.removeAll();

        addLabelTextField(labBoardSize, texBoardWidth, texBoardHeight, "x"); //$NON-NLS-1$
        texBoardWidth.requestFocus();

        if (!advanced) {

            addSeparator(slBoardSize);

            addLabelTextField(labTheme, texTheme);

            addOption(labElevation, choElevation, slElevation);
            addOption(labCliffs, choCliffs, slCliffs);
            addOption(labWoods, choWoods, slWoods);
            addOption(labRough, choRough, slRough);
            addOption(labSwamp, choSwamp, slSwamp);
            addOption(labRoads, choRoads, slRoads);
            addOption(labLakes, choLakes, slLakes);
            addOption(labRivers, choRivers, slRivers);
            addOption(labCraters, choCraters, slCraters);
            addOption(labPavement, choPavement, slPavement);
            addOption(labIce, choIce, slIce);
            addOption(labRubble, choRubble, slRubble);
            addOption(labFortified, choFortified, slFortified);
            addOption(labCity, choCity, slCity);
            addOption(labMountain, choMountain, slMountain);

        } else {

            addSeparator(slBoardSizeAd);

            addLabelTextField(labTheme, texTheme);

            addLabelTextField(labHilliness, texHilliness);
            addLabelTextField(labRange, texRange);
            addLabelTextField(labProbInvert, texProbInvert);
            addLabelTextField(labAlgorithmToUse, texAlgorithmToUse);
            addLabelTextField(labCliffsAd, texCliffs);
            addLabelTextField(labMountainPeaks, texMountainPeaks);
            addLabelTextField(labMountainStyle, texMountainStyle);
            addLabelTextField(labMountainHeight, texMountainHeightMin,
                    texMountainHeightMax, "-");
            addLabelTextField(labMountainWidth, texMountainWidthMin,
                    texMountainWidthMax, "-");

            addSeparator(slElevationAd);

            addLabelTextField(labForestSpots, texMinForestSpots,
                    texMaxForestSpots, "-"); //$NON-NLS-1$
            addLabelTextField(labForestSize, texMinForestSize,
                    texMaxForestSize, "-"); //$NON-NLS-1$
            addLabelTextField(labProbHeavy, texProbHeavy);

            addSeparator(slWoodsAd);

            addLabelTextField(labRoughSpots, texMinRoughSpots,
                    texMaxRoughSpots, "-"); //$NON-NLS-1$
            addLabelTextField(labRoughSize, texMinRoughSize, texMaxRoughSize,
                    "-"); //$NON-NLS-1$

            addSeparator(slRoughAd);

            addLabelTextField(labSwampSpots, texMinSwampSpots,
                    texMaxSwampSpots, "-"); //$NON-NLS-1$
            addLabelTextField(labSwampSize, texMinSwampSize, texMaxSwampSize,
                    "-"); //$NON-NLS-1$

            addSeparator(slSwampAd);

            addLabelTextField(labProbRoad, texProbRoad);

            addSeparator(slRoadsAd);

            addLabelTextField(labWaterSpots, texMinWaterSpots,
                    texMaxWaterSpots, "-"); //$NON-NLS-1$
            addLabelTextField(labWaterSize, texMinWaterSize, texMaxWaterSize,
                    "-"); //$NON-NLS-1$
            addLabelTextField(labProbDeep, texProbDeep);

            addSeparator(slLakesAd);

            addLabelTextField(labProbRiver, texProbRiver);

            addSeparator(slRiversAd);

            addLabelTextField(labProbCrater, texProbCrater);
            addLabelTextField(labMaxCraters, texMinCraters, texMaxCraters, "-"); //$NON-NLS-1$
            addLabelTextField(labRadius, texMinRadius, texMaxRadius, "-"); //$NON-NLS-1$

            addSeparator(slCratersAd);

            addLabelTextField(labPavementSpots, texMinPavementSpots,
                    texMaxPavementSpots, "-");
            addLabelTextField(labPavementSize, texMinPavementSize,
                    texMaxPavementSize, "-");

            addSeparator(slPavementAd);

            addLabelTextField(labRubbleSpots, texMinRubbleSpots,
                    texMaxRubbleSpots, "-");
            addLabelTextField(labRubbleSize, texMinRubbleSize,
                    texMaxRubbleSize, "-");

            addSeparator(slRubbleAd);

            addLabelTextField(labFortifiedSpots, texMinFortifiedSpots,
                    texMaxFortifiedSpots, "-");
            addLabelTextField(labFortifiedSize, texMinFortifiedSize,
                    texMaxFortifiedSize, "-");

            addSeparator(slFortifiedAd);

            addLabelTextField(labIceSpots, texMinIceSpots, texMaxIceSpots, "-");
            addLabelTextField(labIceSize, texMinIceSize, texMaxIceSize, "-");

            addSeparator(slIceAd);

            addLabelTextField(labProbDrought, texProbDrought);
            addLabelTextField(labProbFire, texProbFire);
            addLabelTextField(labProbFreeze, texProbFreeze);
            addLabelTextField(labProbFlood, texProbFlood);
            addLabelTextField(labFxMod, texFxMod);

            addSeparator(slCityAd);

            addOption(labCity, choCity, slCity);
            addLabelTextField(labCityBlocks, texCityBlocks);
            addLabelTextField(labCityCF, texCityMinCF, texCityMaxCF, "-");
            addLabelTextField(labCityFloors, texCityMinFloors,
                    texCityMaxFloors, "-");
            addLabelTextField(labCityDensity, texCityDensity);
            addLabelTextField(labTownSize, texTownSize);

            addSeparator(slInvertNegativeAd);
            addLabelTextField(labInvertNegative, texInvertNegative);
        }
        scrAll = new JScrollPane(panOptions);

        if (initiated) {
            pack();
            setProperSize();
            setProperLocation();
        }
    }

    private void setupButtons() {

        panButtons.add(butOK);
        panButtons.add(butAdvanced);
        panButtons.add(butSave);
        panButtons.add(butLoad);

    }

    private void createComponents() {

        butOK = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        butOK.addActionListener(this);

        butAdvanced = new JButton(Messages
                .getString("RandomMapDialog.Advanced")); //$NON-NLS-1$
        butAdvanced.addActionListener(this);

        butSave = new JButton(Messages.getString("RandomMapDialog.Save")); //$NON-NLS-1$
        butSave.addActionListener(this);

        butLoad = new JButton(Messages.getString("RandomMapDialog.Load")); //$NON-NLS-1$
        butLoad.addActionListener(this);

        panButtons = new JPanel();
        panButtons.setLayout(new FlowLayout());

        panOptions = new JPanel();
        gridbag = new GridBagLayout();
        panOptions.setLayout(gridbag);

        labBoardSize = new JLabel(Messages
                .getString("RandomMapDialog.BoardSize"), SwingConstants.LEFT); //$NON-NLS-1$
        texBoardWidth = new JTextField(2);
        texBoardWidth.addFocusListener(this);
        texBoardHeight = new JTextField(2);
        texBoardHeight.addFocusListener(this);
        slBoardSize = new SimpleLine(NORMAL_LINE_WIDTH);

        // Normal setting components...
        labElevation = new JLabel(Messages
                .getString("RandomMapDialog.labElevation"), SwingConstants.LEFT); //$NON-NLS-1$
        choElevation = new JComboBox();
        fillChoice(choElevation);
        slElevation = new SimpleLine(NORMAL_LINE_WIDTH);

        labCliffs = new JLabel(
                Messages.getString("RandomMapDialog.labCliffs"), SwingConstants.LEFT); //$NON-NLS-1$
        choCliffs = new JComboBox();
        fillChoice(choCliffs);
        slCliffs = new SimpleLine(NORMAL_LINE_WIDTH);

        labWoods = new JLabel(
                Messages.getString("RandomMapDialog.labWoods"), SwingConstants.LEFT); //$NON-NLS-1$
        choWoods = new JComboBox();
        fillChoice(choWoods);
        slWoods = new SimpleLine(NORMAL_LINE_WIDTH);

        labLakes = new JLabel(
                Messages.getString("RandomMapDialog.labLakes"), SwingConstants.LEFT); //$NON-NLS-1$
        choLakes = new JComboBox();
        fillChoice(choLakes);
        slLakes = new SimpleLine(NORMAL_LINE_WIDTH);

        labRough = new JLabel(
                Messages.getString("RandomMapDialog.labRough"), SwingConstants.LEFT); //$NON-NLS-1$
        choRough = new JComboBox();
        fillChoice(choRough);
        slRough = new SimpleLine(NORMAL_LINE_WIDTH);

        labSwamp = new JLabel(
                Messages.getString("RandomMapDialog.labSwamp"), SwingConstants.LEFT); //$NON-NLS-1$
        choSwamp = new JComboBox();
        fillChoice(choSwamp);
        slSwamp = new SimpleLine(NORMAL_LINE_WIDTH);

        labPavement = new JLabel(Messages
                .getString("RandomMapDialog.labPavement"), SwingConstants.LEFT);
        choPavement = new JComboBox();
        fillChoice(choPavement);
        slPavement = new SimpleLine(NORMAL_LINE_WIDTH);

        labRubble = new JLabel(Messages.getString("RandomMapDialog.labRubble"),
                SwingConstants.LEFT);
        choRubble = new JComboBox();
        fillChoice(choRubble);
        slRubble = new SimpleLine(NORMAL_LINE_WIDTH);

        labFortified = new JLabel(Messages
                .getString("RandomMapDialog.labFortified"), SwingConstants.LEFT);
        choFortified = new JComboBox();
        fillChoice(choFortified);
        slFortified = new SimpleLine(NORMAL_LINE_WIDTH);

        labIce = new JLabel(Messages.getString("RandomMapDialog.labIce"),
                SwingConstants.LEFT);
        choIce = new JComboBox();
        fillChoice(choIce);
        slIce = new SimpleLine(NORMAL_LINE_WIDTH);

        labCraters = new JLabel(Messages
                .getString("RandomMapDialog.labCraters"), SwingConstants.LEFT);
        choCraters = new JComboBox();
        fillChoice(choCraters);
        slCraters = new SimpleLine(NORMAL_LINE_WIDTH);

        labRivers = new JLabel(
                Messages.getString("RandomMapDialog.labRivers"), SwingConstants.LEFT); //$NON-NLS-1$
        choRivers = new JComboBox();
        fillChoice(choRivers);
        slRivers = new SimpleLine(NORMAL_LINE_WIDTH);

        labRoads = new JLabel(
                Messages.getString("RandomMapDialog.labRoads"), SwingConstants.LEFT); //$NON-NLS-1$
        choRoads = new JComboBox();
        fillChoice(choRoads);
        slRoads = new SimpleLine(NORMAL_LINE_WIDTH);

        labCity = new JLabel(
                Messages.getString("RandomMapDialog.labCity"), SwingConstants.LEFT); //$NON-NLS-1$
        choCity = new JComboBox();
        choCity.addItem(NONE);
        choCity.addItem("HUB");
        choCity.addItem("GRID");
        choCity.addItem("METRO");
        choCity.addItem("TOWN");
        slCity = new SimpleLine(NORMAL_LINE_WIDTH);
        labMountain = new JLabel(Messages
                .getString("RandomMapDialog.labMountain"), SwingConstants.LEFT); //$NON-NLS-1$
        choMountain = new JComboBox();
        fillChoice(choMountain);
        slMountain = new SimpleLine(NORMAL_LINE_WIDTH);

        // Advanced setting components...
        labTheme = new JLabel(Messages.getString("RandomMapDialog.labTheme"),
                SwingConstants.LEFT);
        texTheme = new JTextField(20);
        /** how much hills there should be, Range 0..99 */
        labHilliness = new JLabel(Messages
                .getString("RandomMapDialog.labHilliness"), SwingConstants.LEFT); //$NON-NLS-1$
        texHilliness = new JTextField(2);
        texHilliness.addFocusListener(this);
        /** Maximum level of the map */
        labRange = new JLabel(
                Messages.getString("RandomMapDialog.labRange"), SwingConstants.LEFT); //$NON-NLS-1$
        texRange = new JTextField(2);
        texRange.addFocusListener(this);
        labProbInvert = new JLabel(
                Messages.getString("RandomMapDialog.labProbInvert"), SwingConstants.LEFT); //$NON-NLS-1$
        texProbInvert = new JTextField(2);
        texProbInvert.addFocusListener(this);
        labCliffsAd = new JLabel(Messages
                .getString("RandomMapDialog.labCliffs"), SwingConstants.LEFT); //$NON-NLS-1$
        texCliffs = new JTextField(2);
        texCliffs.addFocusListener(this);

        // mountain
        labMountainHeight = new JLabel(
                Messages.getString("RandomMapDialog.labMountainHeight"), SwingConstants.LEFT); //$NON-NLS-1$
        labMountainWidth = new JLabel(
                Messages.getString("RandomMapDialog.labMountainWidth"), SwingConstants.LEFT); //$NON-NLS-1$
        labMountainPeaks = new JLabel(
                Messages.getString("RandomMapDialog.labMountainPeaks"), SwingConstants.LEFT); //$NON-NLS-1$
        labMountainStyle = new JLabel(
                Messages.getString("RandomMapDialog.labMountainStyle"), SwingConstants.LEFT); //$NON-NLS-1$
        texMountainPeaks = new JTextField(2);
        texMountainPeaks.addFocusListener(this);
        texMountainHeightMin = new JTextField(2);
        texMountainHeightMin.addFocusListener(this);
        texMountainHeightMax = new JTextField(2);
        texMountainHeightMax.addFocusListener(this);
        texMountainWidthMin = new JTextField(2);
        texMountainWidthMin.addFocusListener(this);
        texMountainWidthMax = new JTextField(2);
        texMountainWidthMax.addFocusListener(this);
        texMountainStyle = new JTextField(2);
        texMountainStyle.addFocusListener(this);

        /** how much Lakes at least */
        labWaterSpots = new JLabel(
                Messages.getString("RandomMapDialog.labWaterSpots"), SwingConstants.LEFT); //$NON-NLS-1$
        texMinWaterSpots = new JTextField(2);
        texMinWaterSpots.addFocusListener(this);
        /** how much Lakes at most */
        texMaxWaterSpots = new JTextField(2);
        texMaxWaterSpots.addFocusListener(this);
        /** minimum size of a lake */
        labWaterSize = new JLabel(Messages
                .getString("RandomMapDialog.labWaterSize"), SwingConstants.LEFT); //$NON-NLS-1$
        texMinWaterSize = new JTextField(2);
        texMinWaterSize.addFocusListener(this);
        /** maximum Size of a lake */
        texMaxWaterSize = new JTextField(2);
        texMaxWaterSize.addFocusListener(this);
        /** probability for water deeper than lvl1, Range 0..100 */
        labProbDeep = new JLabel(Messages
                .getString("RandomMapDialog.labProbDeep"), SwingConstants.LEFT); //$NON-NLS-1$
        texProbDeep = new JTextField(2);
        texProbDeep.addFocusListener(this);

        /** how much forests at least */
        labForestSpots = new JLabel(
                Messages.getString("RandomMapDialog.labForestSpots"), SwingConstants.LEFT); //$NON-NLS-1$
        texMinForestSpots = new JTextField(2);
        texMinForestSpots.addFocusListener(this);
        /** how much forests at most */
        texMaxForestSpots = new JTextField(2);
        texMaxForestSpots.addFocusListener(this);
        /** minimum size of a forest */
        labForestSize = new JLabel(
                Messages.getString("RandomMapDialog.labForestSize"), SwingConstants.LEFT); //$NON-NLS-1$
        texMinForestSize = new JTextField(2);
        texMinForestSize.addFocusListener(this);
        /** maximum Size of a forest */
        texMaxForestSize = new JTextField(2);
        texMaxForestSize.addFocusListener(this);
        /** probability for heavy wood, Range 0..100 */
        labProbHeavy = new JLabel(Messages
                .getString("RandomMapDialog.labProbHeavy"), SwingConstants.LEFT); //$NON-NLS-1$
        texProbHeavy = new JTextField(2);
        texProbHeavy.addFocusListener(this);

        /** rough */
        labRoughSpots = new JLabel(
                Messages.getString("RandomMapDialog.labRoughSpots"), SwingConstants.LEFT); //$NON-NLS-1$
        texMinRoughSpots = new JTextField(2);
        texMinRoughSpots.addFocusListener(this);
        texMaxRoughSpots = new JTextField(2);
        texMaxRoughSpots.addFocusListener(this);
        labRoughSize = new JLabel(Messages
                .getString("RandomMapDialog.labRoughSize"), SwingConstants.LEFT); //$NON-NLS-1$
        texMinRoughSize = new JTextField(2);
        texMinRoughSize.addFocusListener(this);
        texMaxRoughSize = new JTextField(2);
        texMaxRoughSize.addFocusListener(this);

        /** swamp */
        labSwampSpots = new JLabel(
                Messages.getString("RandomMapDialog.labSwampSpots"), SwingConstants.LEFT); //$NON-NLS-1$
        texMinSwampSpots = new JTextField(2);
        texMinSwampSpots.addFocusListener(this);
        texMaxSwampSpots = new JTextField(2);
        texMaxSwampSpots.addFocusListener(this);
        labSwampSize = new JLabel(Messages
                .getString("RandomMapDialog.labSwampSize"), SwingConstants.LEFT); //$NON-NLS-1$
        texMinSwampSize = new JTextField(2);
        texMinSwampSize.addFocusListener(this);
        texMaxSwampSize = new JTextField(2);
        texMaxSwampSize.addFocusListener(this);

        /** pavement */
        labPavementSpots = new JLabel(Messages
                .getString("RandomMapDialog.labPavementSpots"),
                SwingConstants.LEFT);
        texMinPavementSpots = new JTextField(2);
        texMinPavementSpots.addFocusListener(this);
        texMaxPavementSpots = new JTextField(2);
        texMaxPavementSpots.addFocusListener(this);
        labPavementSize = new JLabel(Messages
                .getString("RandomMapDialog.labPavementSize"),
                SwingConstants.LEFT);
        texMinPavementSize = new JTextField(2);
        texMinPavementSize.addFocusListener(this);
        texMaxPavementSize = new JTextField(2);
        texMaxPavementSize.addFocusListener(this);

        /** Rubble */
        labRubbleSpots = new JLabel(Messages
                .getString("RandomMapDialog.labRubbleSpots"),
                SwingConstants.LEFT);
        texMinRubbleSpots = new JTextField(2);
        texMinRubbleSpots.addFocusListener(this);
        texMaxRubbleSpots = new JTextField(2);
        texMaxRubbleSpots.addFocusListener(this);
        labRubbleSize = new JLabel(Messages
                .getString("RandomMapDialog.labRubbleSize"),
                SwingConstants.LEFT);
        texMinRubbleSize = new JTextField(2);
        texMinRubbleSize.addFocusListener(this);
        texMaxRubbleSize = new JTextField(2);
        texMaxRubbleSize.addFocusListener(this);

        /** Fortified */
        labFortifiedSpots = new JLabel(Messages
                .getString("RandomMapDialog.labFortifiedSpots"),
                SwingConstants.LEFT);
        texMinFortifiedSpots = new JTextField(2);
        texMinFortifiedSpots.addFocusListener(this);
        texMaxFortifiedSpots = new JTextField(2);
        texMaxFortifiedSpots.addFocusListener(this);
        labFortifiedSize = new JLabel(Messages
                .getString("RandomMapDialog.labFortifiedSize"),
                SwingConstants.LEFT);
        texMinFortifiedSize = new JTextField(2);
        texMinFortifiedSize.addFocusListener(this);
        texMaxFortifiedSize = new JTextField(2);
        texMaxFortifiedSize.addFocusListener(this);

        /** ice */
        labIceSpots = new JLabel(Messages
                .getString("RandomMapDialog.labIceSpots"), SwingConstants.LEFT);
        texMinIceSpots = new JTextField(2);
        texMinIceSpots.addFocusListener(this);
        texMaxIceSpots = new JTextField(2);
        texMaxIceSpots.addFocusListener(this);
        labIceSize = new JLabel(Messages
                .getString("RandomMapDialog.labIceSize"), SwingConstants.LEFT);
        texMinIceSize = new JTextField(2);
        texMinIceSize.addFocusListener(this);
        texMaxIceSize = new JTextField(2);
        texMaxIceSize.addFocusListener(this);

        /** probability for a road, range 0..100 */
        labProbRoad = new JLabel(Messages
                .getString("RandomMapDialog.labProbRoad"), SwingConstants.LEFT); //$NON-NLS-1$
        texProbRoad = new JTextField(2);
        texProbRoad.addFocusListener(this);
        /** probability for a river, range 0..100 */
        labProbRiver = new JLabel(Messages
                .getString("RandomMapDialog.labProbRiver"), SwingConstants.LEFT); //$NON-NLS-1$
        texProbRiver = new JTextField(2);
        texProbRiver.addFocusListener(this);

        /* Craters */
        labProbCrater = new JLabel(
                Messages.getString("RandomMapDialog.labProbCrater"), SwingConstants.LEFT); //$NON-NLS-1$
        texProbCrater = new JTextField(2);
        texProbCrater.addFocusListener(this);
        labRadius = new JLabel(
                Messages.getString("RandomMapDialog.labRadius"), SwingConstants.LEFT); //$NON-NLS-1$
        texMinRadius = new JTextField(2);
        texMinRadius.addFocusListener(this);
        texMaxRadius = new JTextField(2);
        texMaxRadius.addFocusListener(this);
        labMaxCraters = new JLabel(
                Messages.getString("RandomMapDialog.labMaxCraters"), SwingConstants.LEFT); //$NON-NLS-1$
        texMaxCraters = new JTextField(2);
        texMaxCraters.addFocusListener(this);
        texMinCraters = new JTextField(2);
        texMinCraters.addFocusListener(this);

        /* FX */
        labProbDrought = new JLabel(Messages
                .getString("RandomMapDialog.labProbDrought"),
                SwingConstants.LEFT);
        labProbFire = new JLabel(Messages
                .getString("RandomMapDialog.labProbFire"), SwingConstants.LEFT);
        labProbFreeze = new JLabel(Messages
                .getString("RandomMapDialog.labProbFreeze"),
                SwingConstants.LEFT);
        labProbFlood = new JLabel(Messages
                .getString("RandomMapDialog.labProbFlood"), SwingConstants.LEFT);
        labFxMod = new JLabel(Messages.getString("RandomMapDialog.labFxMod"),
                SwingConstants.LEFT);
        texProbDrought = new JTextField(2);
        texProbFire = new JTextField(2);
        texProbFreeze = new JTextField(2);
        texProbFlood = new JTextField(2);
        texFxMod = new JTextField(2);

        /* Buildings */
        labCityBlocks = new JLabel(Messages
                .getString("RandomMapDialog.labCityBlocks"),
                SwingConstants.LEFT);
        labCityCF = new JLabel(Messages.getString("RandomMapDialog.labCityCF"),
                SwingConstants.LEFT);
        labCityFloors = new JLabel(Messages
                .getString("RandomMapDialog.labCityFloors"),
                SwingConstants.LEFT);
        labCityDensity = new JLabel(Messages
                .getString("RandomMapDialog.labCityDensity"),
                SwingConstants.LEFT);
        labTownSize = new JLabel(Messages
                .getString("RandomMapDialog.labTownSize"), SwingConstants.LEFT);
        texCityBlocks = new JTextField(2);
        texCityMinCF = new JTextField(2);
        texCityMaxCF = new JTextField(2);
        texCityMinFloors = new JTextField(2);
        texCityMaxFloors = new JTextField(2);
        texCityDensity = new JTextField(2);
        texTownSize = new JTextField(2);

        labInvertNegative = new JLabel(
                Messages.getString("RandomMapDialog.labInvertNegative"), SwingConstants.LEFT); //$NON-NLS-1$
        texInvertNegative = new JTextField(1);

        /** Algorithm */
        labAlgorithmToUse = new JLabel(
                Messages.getString("RandomMapDialog.labAlgorithmToUse"), SwingConstants.LEFT); //$NON-NLS-1$
        texAlgorithmToUse = new JTextField(2);

        slElevationAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slWoodsAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slLakesAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slPavementAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slRubbleAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slFortifiedAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slIceAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slRoughAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slRoadsAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slRiversAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slSwampAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slBoardSizeAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slCratersAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slCityAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slInvertNegativeAd = new SimpleLine(ADVANCED_LINE_WIDTH);

    }

    private void addOption(JLabel label, JComboBox choice, SimpleLine sl) {
        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(label, c);
        panOptions.add(label);

        c.gridwidth = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(choice, c);
        panOptions.add(choice);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(2, 0, 2, 0);
        gridbag.setConstraints(sl, c);
        panOptions.add(sl);

    }

    private void fillChoice(JComboBox c) {
        c.addItem(NONE);
        c.addItem(LOW);
        c.addItem(MEDIUM);
        c.addItem(HIGH);
    }

    private void addLabelTextField(JLabel label, JTextField text) {
        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(label, c);
        panOptions.add(label);

        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(text, c);
        panOptions.add(text);
    }

    private void addLabelTextField(JLabel label, JTextField text,
            JTextField text2, String separator) {
        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(label, c);
        panOptions.add(label);

        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(text, c);
        panOptions.add(text);

        JLabel l = new JLabel(separator, SwingConstants.CENTER);
        gridbag.setConstraints(l, c);
        panOptions.add(l);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(text2, c);
        panOptions.add(text2);
    }

    private void addSeparator(SimpleLine sl) {
        GridBagConstraints c = new GridBagConstraints();

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(2, 0, 2, 0);
        gridbag.setConstraints(sl, c);
        panOptions.add(sl);
    }

    private void loadValues() {
        texBoardWidth.setText(Integer.toString(mapSettings.getBoardWidth()));
        texBoardHeight.setText(Integer.toString(mapSettings.getBoardHeight()));
        texTheme.setText(mapSettings.getTheme());

        texHilliness.setText(Integer.toString(mapSettings.getHilliness()));
        texRange.setText(Integer.toString(mapSettings.getRange()));
        texProbInvert.setText(Integer.toString(mapSettings.getProbInvert()));
        texCliffs.setText(Integer.toString(mapSettings.getCliffs()));
        texMinWaterSpots.setText(Integer.toString(mapSettings
                .getMinWaterSpots()));
        texMaxWaterSpots.setText(Integer.toString(mapSettings
                .getMaxWaterSpots()));
        texMinWaterSize
                .setText(Integer.toString(mapSettings.getMinWaterSize()));
        texMaxWaterSize
                .setText(Integer.toString(mapSettings.getMaxWaterSize()));

        texMinForestSpots.setText(Integer.toString(mapSettings
                .getMinForestSpots()));
        texMaxForestSpots.setText(Integer.toString(mapSettings
                .getMaxForestSpots()));
        texMinForestSize.setText(Integer.toString(mapSettings
                .getMinForestSize()));
        texMaxForestSize.setText(Integer.toString(mapSettings
                .getMaxForestSize()));

        texMinRoughSpots.setText(Integer.toString(mapSettings
                .getMinRoughSpots()));
        texMaxRoughSpots.setText(Integer.toString(mapSettings
                .getMaxRoughSpots()));
        texMinRoughSize
                .setText(Integer.toString(mapSettings.getMinRoughSize()));
        texMaxRoughSize
                .setText(Integer.toString(mapSettings.getMaxRoughSize()));

        texMinSwampSpots.setText(Integer.toString(mapSettings
                .getMinSwampSpots()));
        texMaxSwampSpots.setText(Integer.toString(mapSettings
                .getMaxSwampSpots()));
        texMinSwampSize
                .setText(Integer.toString(mapSettings.getMinSwampSize()));
        texMaxSwampSize
                .setText(Integer.toString(mapSettings.getMaxSwampSize()));

        texMinPavementSpots.setText(Integer.toString(mapSettings
                .getMinPavementSpots()));
        texMaxPavementSpots.setText(Integer.toString(mapSettings
                .getMaxPavementSpots()));
        texMinPavementSize.setText(Integer.toString(mapSettings
                .getMinPavementSize()));
        texMaxPavementSize.setText(Integer.toString(mapSettings
                .getMaxPavementSize()));

        texMinRubbleSpots.setText(Integer.toString(mapSettings
                .getMinRubbleSpots()));
        texMaxRubbleSpots.setText(Integer.toString(mapSettings
                .getMaxRubbleSpots()));
        texMinRubbleSize.setText(Integer.toString(mapSettings
                .getMinRubbleSize()));
        texMaxRubbleSize.setText(Integer.toString(mapSettings
                .getMaxRubbleSize()));

        texMinFortifiedSpots.setText(Integer.toString(mapSettings
                .getMinFortifiedSpots()));
        texMaxFortifiedSpots.setText(Integer.toString(mapSettings
                .getMaxFortifiedSpots()));
        texMinFortifiedSize.setText(Integer.toString(mapSettings
                .getMinFortifiedSize()));
        texMaxFortifiedSize.setText(Integer.toString(mapSettings
                .getMaxFortifiedSize()));

        texMinIceSpots.setText(Integer.toString(mapSettings.getMinIceSpots()));
        texMaxIceSpots.setText(Integer.toString(mapSettings.getMaxIceSpots()));
        texMinIceSize.setText(Integer.toString(mapSettings.getMinIceSize()));
        texMaxIceSize.setText(Integer.toString(mapSettings.getMaxIceSize()));

        texProbDeep.setText(Integer.toString(mapSettings.getProbDeep()));
        texProbHeavy.setText(Integer.toString(mapSettings.getProbHeavy()));
        texProbRiver.setText(Integer.toString(mapSettings.getProbRiver()));
        texProbRoad.setText(Integer.toString(mapSettings.getProbRoad()));
        texProbCrater.setText(Integer.toString(mapSettings.getProbCrater()));
        texMinRadius.setText(Integer.toString(mapSettings.getMinRadius()));
        texMaxRadius.setText(Integer.toString(mapSettings.getMaxRadius()));
        texMaxCraters.setText(Integer.toString(mapSettings.getMaxCraters()));
        texMinCraters.setText(Integer.toString(mapSettings.getMinCraters()));

        texProbDrought.setText(Integer.toString(mapSettings.getProbDrought()));
        texProbFire.setText(Integer.toString(mapSettings.getProbForestFire()));
        texProbFreeze.setText(Integer.toString(mapSettings.getProbFreeze()));
        texProbFlood.setText(Integer.toString(mapSettings.getProbFlood()));
        texFxMod.setText(Integer.toString(mapSettings.getFxMod()));

        choCity.setSelectedItem(mapSettings.getCityType());
        texInvertNegative.setText(Integer.toString(mapSettings
                .getInvertNegativeTerrain()));
        texCityBlocks.setText(Integer.toString(mapSettings.getCityBlocks()));
        texCityMinCF.setText(Integer.toString(mapSettings.getCityMinCF()));
        texCityMaxCF.setText(Integer.toString(mapSettings.getCityMaxCF()));
        texCityMinFloors.setText(Integer.toString(mapSettings
                .getCityMinFloors()));
        texCityMaxFloors.setText(Integer.toString(mapSettings
                .getCityMaxFloors()));
        texCityDensity.setText(Integer.toString(mapSettings.getCityDensity()));
        texTownSize.setText(Integer.toString(mapSettings.getTownSize()));

        texMountainPeaks.setText(Integer.toString(mapSettings
                .getMountainPeaks()));
        texMountainStyle.setText(Integer.toString(mapSettings
                .getMountainStyle()));
        texMountainHeightMin.setText(Integer.toString(mapSettings
                .getMountainHeightMin()));
        texMountainHeightMax.setText(Integer.toString(mapSettings
                .getMountainHeightMax()));
        texMountainWidthMin.setText(Integer.toString(mapSettings
                .getMountainWidthMin()));
        texMountainWidthMax.setText(Integer.toString(mapSettings
                .getMountainWidthMax()));
        texAlgorithmToUse.setText(Integer.toString(mapSettings
                .getAlgorithmToUse()));
    }

    private boolean applyValues() {
        int boardWidth;
        int boardHeight;
        int hilliness, range, cliffs;
        int minWaterSpots, maxWaterSpots, minWaterSize, maxWaterSize, probDeep;
        int minForestSpots, maxForestSpots, minForestSize, maxForestSize, probHeavy;
        int minRoughSpots, maxRoughSpots, minRoughSize, maxRoughSize;
        int minPavementSpots, maxPavementSpots, minPavementSize, maxPavementSize;
        int minRubbleSpots, maxRubbleSpots, minRubbleSize, maxRubbleSize;
        int minFortifiedSpots, maxFortifiedSpots, minFortifiedSize, maxFortifiedSize;
        int minIceSpots, maxIceSpots, minIceSize, maxIceSize;
        int minSwampSpots, maxSwampSpots, minSwampSize, maxSwampSize;
        int probRoad, probRiver, probInvert;
        int minRadius, maxRadius, minCraters, maxCraters, probCrater;
        int drought, fire, freeze, flood, fxmod;
        int algorithmToUse;
        String theme = "";
        String cityType;
        int cityBlocks = 4;
        int cityMinCF = 10;
        int cityMaxCF = 100;
        int cityMinFloors = 1;
        int cityMaxFloors = 6;
        int cityDensity = 75;
        int townSize = 60;
        int mountainPeaks, mountainHeightMin, mountainHeightMax;
        int mountainStyle, mountainWidthMin, mountainWidthMax;
        int invertNegative = 0;

        try {
            boardWidth = Integer.parseInt(texBoardWidth.getText());
            boardHeight = Integer.parseInt(texBoardHeight.getText());
        } catch (NumberFormatException ex) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("RandomMapDialog.OnlyIntegersWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            return false;
        }

        if ((boardWidth <= 0) || (boardHeight <= 0)) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages.getString("RandomMapDialog.BoardSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            return false;
        }

        theme = texTheme.getText();

        if (advanced) {
            try {
                hilliness = Integer.parseInt(texHilliness.getText());
                range = Integer.parseInt(texRange.getText());
                cliffs = Integer.parseInt(texCliffs.getText());
                probInvert = Integer.parseInt(texProbInvert.getText());
                minWaterSpots = Integer.parseInt(texMinWaterSpots.getText());
                maxWaterSpots = Integer.parseInt(texMaxWaterSpots.getText());
                minWaterSize = Integer.parseInt(texMinWaterSize.getText());
                maxWaterSize = Integer.parseInt(texMaxWaterSize.getText());
                minForestSpots = Integer.parseInt(texMinForestSpots.getText());
                maxForestSpots = Integer.parseInt(texMaxForestSpots.getText());
                minForestSize = Integer.parseInt(texMinForestSize.getText());
                maxForestSize = Integer.parseInt(texMaxForestSize.getText());
                minRoughSpots = Integer.parseInt(texMinRoughSpots.getText());
                maxRoughSpots = Integer.parseInt(texMaxRoughSpots.getText());
                minRoughSize = Integer.parseInt(texMinRoughSize.getText());
                maxRoughSize = Integer.parseInt(texMaxRoughSize.getText());
                minSwampSpots = Integer.parseInt(texMinSwampSpots.getText());
                maxSwampSpots = Integer.parseInt(texMaxSwampSpots.getText());
                minSwampSize = Integer.parseInt(texMinSwampSize.getText());
                maxSwampSize = Integer.parseInt(texMaxSwampSize.getText());
                minPavementSpots = Integer.parseInt(texMinPavementSpots
                        .getText());
                maxPavementSpots = Integer.parseInt(texMaxPavementSpots
                        .getText());
                minPavementSize = Integer
                        .parseInt(texMinPavementSize.getText());
                maxPavementSize = Integer
                        .parseInt(texMaxPavementSize.getText());
                minRubbleSpots = Integer.parseInt(texMinRubbleSpots.getText());
                maxRubbleSpots = Integer.parseInt(texMaxRubbleSpots.getText());
                minRubbleSize = Integer.parseInt(texMinRubbleSize.getText());
                maxRubbleSize = Integer.parseInt(texMaxRubbleSize.getText());
                minFortifiedSpots = Integer.parseInt(texMinFortifiedSpots
                        .getText());
                maxFortifiedSpots = Integer.parseInt(texMaxFortifiedSpots
                        .getText());
                minFortifiedSize = Integer.parseInt(texMinFortifiedSize
                        .getText());
                maxFortifiedSize = Integer.parseInt(texMaxFortifiedSize
                        .getText());
                minIceSpots = Integer.parseInt(texMinIceSpots.getText());
                maxIceSpots = Integer.parseInt(texMaxIceSpots.getText());
                minIceSize = Integer.parseInt(texMinIceSize.getText());
                maxIceSize = Integer.parseInt(texMaxIceSize.getText());
                probRoad = Integer.parseInt(texProbRoad.getText());
                probRiver = Integer.parseInt(texProbRiver.getText());
                probHeavy = Integer.parseInt(texProbHeavy.getText());
                probDeep = Integer.parseInt(texProbDeep.getText());
                probCrater = Integer.parseInt(texProbCrater.getText());
                minRadius = Integer.parseInt(texMinRadius.getText());
                maxRadius = Integer.parseInt(texMaxRadius.getText());
                maxCraters = Integer.parseInt(texMaxCraters.getText());
                minCraters = Integer.parseInt(texMinCraters.getText());
                algorithmToUse = Integer.parseInt(texAlgorithmToUse.getText());
                drought = Integer.parseInt(texProbDrought.getText());
                fire = Integer.parseInt(texProbFire.getText());
                freeze = Integer.parseInt(texProbFreeze.getText());
                flood = Integer.parseInt(texProbFlood.getText());
                fxmod = Integer.parseInt(texFxMod.getText());
                cityBlocks = Integer.parseInt(texCityBlocks.getText());
                cityMinCF = Integer.parseInt(texCityMinCF.getText());
                cityMaxCF = Integer.parseInt(texCityMaxCF.getText());
                cityMinFloors = Integer.parseInt(texCityMinFloors.getText());
                cityMaxFloors = Integer.parseInt(texCityMaxFloors.getText());
                cityDensity = Integer.parseInt(texCityDensity.getText());
                mountainHeightMin = Integer.parseInt(texMountainHeightMin
                        .getText());
                mountainHeightMax = Integer.parseInt(texMountainHeightMax
                        .getText());
                mountainWidthMin = Integer.parseInt(texMountainWidthMin
                        .getText());
                mountainWidthMax = Integer.parseInt(texMountainWidthMax
                        .getText());
                mountainStyle = Integer.parseInt(texMountainStyle.getText());
                mountainPeaks = Integer.parseInt(texMountainPeaks.getText());
                invertNegative = Integer.parseInt(texInvertNegative.getText());
                townSize = Integer.parseInt(texTownSize.getText());

            } catch (NumberFormatException ex) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.OnlyIntegersWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }

            if ((hilliness < 0) || (hilliness > 99)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.AmmountOfElevationWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if ((cliffs < 0) || (cliffs > 100)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.CliffsWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (range < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.elevRangeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if ((probInvert < 0) || (probInvert > 100)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.depressionWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minWaterSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinLakesWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxLakesWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSpots < minWaterSpots) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxLakesWarn2"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minWaterSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinLakeSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxLakeSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSize < minWaterSize) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxLakeSizeWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if ((probDeep < 0) || (probDeep > 100)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.DeepWaterProbWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minForestSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinForestsWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxForestSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxForestsWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxForestSpots < minForestSpots) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxForestsWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minForestSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinForestSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxForestSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxForestSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxForestSize < minForestSize) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxForestSizeWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if ((probHeavy < 0) || (probHeavy > 100)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.HeavyForestProbWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minRoughSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinRoughsWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxRoughsWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSpots < minRoughSpots) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxRoughsWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minRoughSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinRoughSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxRoughSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSize < minRoughSize) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxRoughSizeWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minSwampSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinSwampsWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxSwampsWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSpots < minSwampSpots) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxSwampsWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minSwampSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinSwampSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxSwampSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSize < minSwampSize) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxSwampSizeWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minPavementSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinPavementWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxPavementSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxPavementWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxPavementSpots < minPavementSpots) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxPavementWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minPavementSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinPavementSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxPavementSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxPavementSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxPavementSize < minPavementSize) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxPavementSizeWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minRubbleSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinRubbleWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxRubbleSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxRubbleWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxRubbleSpots < minRubbleSpots) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxRubbleWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minRubbleSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinRubbleSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxRubbleSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxRubbleSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxRubbleSize < minRubbleSize) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxRubbleSizeWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minFortifiedSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinFortifiedWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxFortifiedSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxFortifiedWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxFortifiedSpots < minFortifiedSpots) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxFortifiedWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minFortifiedSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinFortifiedSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxFortifiedSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxFortifiedSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxFortifiedSize < minFortifiedSize) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxFortifiedSizeWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minIceSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinIceWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxIceSpots < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxIceWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxIceSpots < minIceSpots) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxIceWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minIceSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinIceSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxIceSize < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxIceSizeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxIceSize < minIceSize) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxIceSizeWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if ((probRiver < 0) || (probRiver > 100)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.RiverProbWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if ((probRoad < 0) || (probRoad > 100)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.RoadProbWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if ((probCrater < 0) || (probCrater > 100)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.CratersProbWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minRadius < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinCraterRadiusWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxRadius < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxCraterRadiusWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxRadius < minRadius) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxCraterRadiusWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxCraters < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxCratersWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (minCraters < 0) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MinCratersWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if (maxCraters < minCraters) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MaxCratersWarn1"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }
            if ((algorithmToUse < 0) || (algorithmToUse > 2)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.AlgorithmWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }

            if ((cityMinCF < 1) || (cityMaxCF > 150)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.CFOutOfRangeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }

            if ((cityMinFloors < 1) || (cityMaxFloors > 100)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.FloorsOutOfRangeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }

            if ((cityDensity < 1) || (cityDensity > 100)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.DensityOutOfRangeWarn"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return false;
            }

            if (((mountainWidthMin < 1) || (mountainWidthMax < mountainWidthMin))
                    && (mountainPeaks > 0)) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                Messages
                                        .getString("RandomMapDialog.MountainWidthOutOfRange"), INVALID_SETTING, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            }

        } else {
            String s = (String) choElevation.getSelectedItem();
            if (s.equals(NONE)) {
                hilliness = 0;
                range = 0;
            } else if (s.equals(LOW)) {
                hilliness = 25;
                range = 3;
            } else if (s.equals(MEDIUM)) {
                hilliness = 50;
                range = 5;
            } else {
                hilliness = 75;
                range = 8;
            }
            s = (String) choCliffs.getSelectedItem();
            if (s.equals(NONE)) {
                cliffs = 0;
            } else if (s.equals(LOW)) {
                cliffs = 25;
            } else if (s.equals(MEDIUM)) {
                cliffs = 50;
            } else {
                cliffs = 75;
            }
            s = (String) choWoods.getSelectedItem();
            if (s.equals(NONE)) {
                minForestSize = 0;
                maxForestSize = 0;
                minForestSpots = 0;
                maxForestSpots = 0;
                probHeavy = 0;
            } else if (s.equals(LOW)) {
                minForestSize = 3;
                maxForestSize = 6;
                minForestSpots = 3;
                maxForestSpots = 6;
                probHeavy = 20;
            } else if (s.equals(MEDIUM)) {
                minForestSize = 3;
                maxForestSize = 10;
                minForestSpots = 4;
                maxForestSpots = 8;
                probHeavy = 30;
            } else {
                minForestSize = 8;
                maxForestSize = 13;
                minForestSpots = 6;
                maxForestSpots = 10;
                probHeavy = 45;
            }
            s = (String) choLakes.getSelectedItem();
            if (s.equals(NONE)) {
                minWaterSize = 0;
                maxWaterSize = 0;
                minWaterSpots = 0;
                maxWaterSpots = 0;
                probDeep = 0;
            } else if (s.equals(LOW)) {
                minWaterSize = 1;
                maxWaterSize = 5;
                minWaterSpots = 1;
                maxWaterSpots = 5;
                probDeep = 20;
            } else if (s.equals(MEDIUM)) {
                minWaterSize = 6;
                maxWaterSize = 10;
                minWaterSpots = 2;
                maxWaterSpots = 5;
                probDeep = 30;
            } else {
                minWaterSize = 8;
                maxWaterSize = 15;
                minWaterSpots = 3;
                maxWaterSpots = 6;
                probDeep = 45;
            }
            s = (String) choRough.getSelectedItem();
            if (s.equals(NONE)) {
                minRoughSize = 0;
                maxRoughSize = 0;
                minRoughSpots = 0;
                maxRoughSpots = 0;
            } else if (s.equals(LOW)) {
                minRoughSize = 1;
                maxRoughSize = 2;
                minRoughSpots = 2;
                maxRoughSpots = 6;
            } else if (s.equals(MEDIUM)) {
                minRoughSize = 2;
                maxRoughSize = 5;
                minRoughSpots = 3;
                maxRoughSpots = 8;
            } else {
                minRoughSize = 3;
                maxRoughSize = 7;
                minRoughSpots = 5;
                maxRoughSpots = 10;
            }
            s = (String) choSwamp.getSelectedItem();
            if (s.equals(NONE)) {
                minSwampSize = 0;
                maxSwampSize = 0;
                minSwampSpots = 0;
                maxSwampSpots = 0;
            } else if (s.equals(LOW)) {
                minSwampSize = 1;
                maxSwampSize = 2;
                minSwampSpots = 2;
                maxSwampSpots = 6;
            } else if (s.equals(MEDIUM)) {
                minSwampSize = 2;
                maxSwampSize = 5;
                minSwampSpots = 3;
                maxSwampSpots = 8;
            } else {
                minSwampSize = 3;
                maxSwampSize = 7;
                minSwampSpots = 5;
                maxSwampSpots = 10;
            }
            s = (String) choPavement.getSelectedItem();
            if (s.equals(NONE)) {
                minPavementSize = 0;
                maxPavementSize = 0;
                minPavementSpots = 0;
                maxPavementSpots = 0;
            } else if (s.equals(LOW)) {
                minPavementSize = 1;
                maxPavementSize = 2;
                minPavementSpots = 2;
                maxPavementSpots = 6;
            } else if (s.equals(MEDIUM)) {
                minPavementSize = 2;
                maxPavementSize = 5;
                minPavementSpots = 3;
                maxPavementSpots = 8;
            } else {
                minPavementSize = 3;
                maxPavementSize = 7;
                minPavementSpots = 5;
                maxPavementSpots = 10;
            }
            s = (String) choRubble.getSelectedItem();
            if (s.equals(NONE)) {
                minRubbleSize = 0;
                maxRubbleSize = 0;
                minRubbleSpots = 0;
                maxRubbleSpots = 0;
            } else if (s.equals(LOW)) {
                minRubbleSize = 1;
                maxRubbleSize = 2;
                minRubbleSpots = 2;
                maxRubbleSpots = 6;
            } else if (s.equals(MEDIUM)) {
                minRubbleSize = 2;
                maxRubbleSize = 5;
                minRubbleSpots = 3;
                maxRubbleSpots = 8;
            } else {
                minRubbleSize = 3;
                maxRubbleSize = 7;
                minRubbleSpots = 5;
                maxRubbleSpots = 10;
            }
            s = (String) choFortified.getSelectedItem();
            if (s.equals(NONE)) {
                minFortifiedSize = 0;
                maxFortifiedSize = 0;
                minFortifiedSpots = 0;
                maxFortifiedSpots = 0;
            } else if (s.equals(LOW)) {
                minFortifiedSize = 1;
                maxFortifiedSize = 2;
                minFortifiedSpots = 1;
                maxFortifiedSpots = 2;
            } else if (s.equals(MEDIUM)) {
                minFortifiedSize = 2;
                maxFortifiedSize = 3;
                minFortifiedSpots = 2;
                maxFortifiedSpots = 4;
            } else {
                minFortifiedSize = 2;
                maxFortifiedSize = 4;
                minFortifiedSpots = 3;
                maxFortifiedSpots = 6;
            }
            s = (String) choIce.getSelectedItem();
            if (s.equals(NONE)) {
                minIceSize = 0;
                maxIceSize = 0;
                minIceSpots = 0;
                maxIceSpots = 0;
            } else if (s.equals(LOW)) {
                minIceSize = 1;
                maxIceSize = 2;
                minIceSpots = 2;
                maxIceSpots = 6;
            } else if (s.equals(MEDIUM)) {
                minIceSize = 2;
                maxIceSize = 5;
                minIceSpots = 3;
                maxIceSpots = 8;
            } else {
                minIceSize = 3;
                maxIceSize = 7;
                minIceSpots = 5;
                maxIceSpots = 10;
            }
            s = (String) choRoads.getSelectedItem();
            if (s.equals(NONE)) {
                probRoad = 0;
            } else if (s.equals(LOW)) {
                probRoad = 25;
            } else if (s.equals(MEDIUM)) {
                probRoad = 50;
            } else {
                probRoad = 75;
            }
            s = (String) choRivers.getSelectedItem();
            if (s.equals(NONE)) {
                probRiver = 0;
            } else if (s.equals(LOW)) {
                probRiver = 25;
            } else if (s.equals(MEDIUM)) {
                probRiver = 50;
            } else {
                probRiver = 75;
            }

            s = (String) choCraters.getSelectedItem();
            if (s.equals(NONE)) {
                probCrater = 0;
                minRadius = 0;
                maxRadius = 0;
                minCraters = 0;
                maxCraters = 0;
            } else if (s.equals(LOW)) {
                probCrater = 25;
                minRadius = 1;
                maxRadius = 3;
                minCraters = 1;
                maxCraters = 3;
            } else if (s.equals(MEDIUM)) {
                probCrater = 50;
                minRadius = 1;
                maxRadius = 5;
                minCraters = 3;
                maxCraters = 8;
            } else {
                probCrater = 75;
                minRadius = 3;
                maxRadius = 9;
                minCraters = 6;
                maxCraters = 14;
            }

            s = (String) choMountain.getSelectedItem();
            if (s.equals(NONE)) {
                mountainPeaks = 0;
                mountainHeightMin = 4;
                mountainHeightMax = 6;
                mountainWidthMin = 7;
                mountainWidthMax = 10;
                mountainStyle = 0;
            } else if (s.equals(LOW)) {
                mountainPeaks = 1;
                mountainHeightMin = 4;
                mountainHeightMax = 6;
                mountainWidthMin = 7;
                mountainWidthMax = 10;
                mountainStyle = 0;
            } else if (s.equals(MEDIUM)) {
                mountainPeaks = 2;
                mountainHeightMin = 6;
                mountainHeightMax = 8;
                mountainWidthMin = 7;
                mountainWidthMax = 10;
                mountainStyle = 0;
            } else {
                mountainPeaks = 3;
                mountainHeightMin = 8;
                mountainHeightMax = 10;
                mountainWidthMin = 9;
                mountainWidthMax = 14;
                mountainStyle = MapSettings.MOUNTAIN_SNOWCAPPED;
            }

            cityBlocks = 16;
            cityMinCF = 10;
            cityMaxCF = 100;
            cityMinFloors = 1;
            cityMaxFloors = 6;
            cityDensity = 75;
            townSize = 60;
            algorithmToUse = 0;
            probInvert = 0;
            fxmod = 0;
            flood = 0;
            freeze = 0;
            fire = 0;
            drought = 0;
        }

        cityType = (String) choCity.getSelectedItem();
        if (!advanced && cityType.equals("TOWN")) {
            cityBlocks = 3;
            cityMaxCF = 50;
            cityMaxFloors = 3;
        }

        mapSettings.setBoardSize(boardWidth, boardHeight);
        mapSettings.setElevationParams(hilliness, range, probInvert);
        mapSettings.setCliffParam(cliffs);
        mapSettings.setWaterParams(minWaterSpots, maxWaterSpots, minWaterSize,
                maxWaterSize, probDeep);
        mapSettings.setForestParams(minForestSpots, maxForestSpots,
                minForestSize, maxForestSize, probHeavy);
        mapSettings.setRoughParams(minRoughSpots, maxRoughSpots, minRoughSize,
                maxRoughSize);
        mapSettings.setSwampParams(minSwampSpots, maxSwampSpots, minSwampSize,
                maxSwampSize);
        mapSettings.setPavementParams(minPavementSpots, maxPavementSpots,
                minPavementSize, maxPavementSize);
        mapSettings.setRubbleParams(minRubbleSpots, maxRubbleSpots,
                minRubbleSize, maxRubbleSize);
        mapSettings.setFortifiedParams(minFortifiedSpots, maxFortifiedSpots,
                minFortifiedSize, maxFortifiedSize);
        mapSettings.setIceParams(minIceSpots, maxIceSpots, minIceSize,
                maxIceSize);
        mapSettings.setRiverParam(probRiver);
        mapSettings.setRoadParam(probRoad);
        mapSettings.setCraterParam(probCrater, minCraters, maxCraters,
                minRadius, maxRadius);
        mapSettings.setSpecialFX(fxmod, fire, freeze, flood, drought);
        mapSettings.setAlgorithmToUse(algorithmToUse);
        mapSettings.setCityParams(cityBlocks, cityType, cityMinCF, cityMaxCF,
                cityMinFloors, cityMaxFloors, cityDensity, townSize);
        mapSettings.setMountainParams(mountainPeaks, mountainWidthMin,
                mountainWidthMax, mountainHeightMin, mountainHeightMax,
                mountainStyle);
        mapSettings.setInvertNegativeTerrain(invertNegative);

        mapSettings.setTheme(theme);

        if (!advanced) {
            loadValues();
        }

        bsd.updateMapSettings(mapSettings);

        return true;
    }

    public void focusGained(FocusEvent fe) {
        if (fe.getSource() instanceof JTextField) {
            JTextField tf = (JTextField) fe.getSource();
            tf.selectAll();
        }
    }

    public void focusLost(FocusEvent fe) {
        if (fe.getSource() instanceof JTextField) {
            JTextField tf = (JTextField) fe.getSource();
            tf.select(0, 0);
        }
    }

    public void setMapSettings(MapSettings mapSettings) {
        this.mapSettings = mapSettings;
        loadValues();
    }

    private void setProperSize() {
        validate();
        pack();
        Dimension dopt = panOptions.getPreferredSize();
        Dimension dbt = panButtons.getPreferredSize();
        int width = Math.min(dopt.width + dbt.width + 50,
                getParent().getSize().width);
        int height = Math.min(dopt.height + dbt.height + 50, getParent()
                .getSize().height);
        setSize(width, height);
    }

    private void setProperLocation() {
        int x = (getParent().getSize().width - getSize().width) / 2;
        int y = (getParent().getSize().height - getSize().height) / 2;
        setLocation(x, y);

    }

}
