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

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.SimpleLine;
import megamek.common.MapSettings;

public class RandomMapDialog extends Dialog implements ActionListener,
        FocusListener {
    /**
     *
     */
    private static final long serialVersionUID = -1676096571134662220L;
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

    private ScrollPane scrAll = new ScrollPane();

    private Button butOK = null;
    private Button butAdvanced = null;
    private Button butSave = null;
    private Button butLoad = null;

    private Panel panButtons = null;
    private Panel panOptions = null;

    private Label labBoardSize = null;
    private TextField texBoardWidth = null;
    private TextField texBoardHeight = null;

    private Choice choElevation = null;
    private Choice choCliffs = null;
    private Choice choWoods = null;
    private Choice choLakes = null;
    private Choice choPavement = null;
    private Choice choRubble = null;
    private Choice choFortified = null;
    private Choice choIce = null;
    private Choice choRough = null;
    private Choice choRoads = null;
    private Choice choRivers = null;
    private Choice choSwamp = null;
    private Choice choCraters = null;
    private Choice choCity = null;
    private Choice choMountain = null;

    private Label labElevation = null;
    private Label labCliffs = null;
    private Label labWoods = null;
    private Label labLakes = null;
    private Label labPavement = null;
    private Label labRubble = null;
    private Label labFortified = null;
    private Label labIce = null;
    private Label labRough = null;
    private Label labRoads = null;
    private Label labRivers = null;
    private Label labSwamp = null;
    private Label labTheme = null;
    private Label labCraters = null;
    private Label labCity = null;
    private Label labMountain = null;

    private SimpleLine slElevation = null;
    private SimpleLine slCliffs = null;
    private SimpleLine slWoods = null;
    private SimpleLine slLakes = null;
    private SimpleLine slPavement = null;
    private SimpleLine slRubble = null;
    private SimpleLine slFortified = null;
    private SimpleLine slIce = null;
    private SimpleLine slRough = null;
    private SimpleLine slRoads = null;
    private SimpleLine slRivers = null;
    private SimpleLine slSwamp = null;
    private SimpleLine slBoardSize = null;
    private SimpleLine slCraters = null;
    private SimpleLine slCity = null;
    private SimpleLine slMountain = null;

    private SimpleLine slElevationAd = null;
    private SimpleLine slWoodsAd = null;
    private SimpleLine slLakesAd = null;
    private SimpleLine slPavementAd = null;
    private SimpleLine slRubbleAd = null;
    private SimpleLine slFortifiedAd = null;
    private SimpleLine slIceAd = null;
    private SimpleLine slRoughAd = null;
    private SimpleLine slRoadsAd = null;
    private SimpleLine slRiversAd = null;
    private SimpleLine slSwampAd = null;
    private SimpleLine slBoardSizeAd = null;
    private SimpleLine slCratersAd = null;
    private SimpleLine slCityAd = null;
    private SimpleLine slInvertNegativeAd = null;

    private TextField texTheme;

    /** how much hills there should be, Range 0..100 */
    private Label labHilliness;
    private TextField texHilliness;
    /** invert negative terrain? 1 yes, 0 no */
    private Label labInvertNegative = null;
    private TextField texInvertNegative = null;
    /** Maximum level of the map */
    private Label labRange;
    private TextField texRange;
    private Label labProbInvert;
    private TextField texProbInvert;
    private Label labCliffsAd;
    private TextField texCliffs;

    /** how much Lakes at least */
    private Label labWaterSpots;
    private TextField texMinWaterSpots;
    /** how much Lakes at most */
    private TextField texMaxWaterSpots;
    /** minimum size of a lake */
    private Label labWaterSize;
    private TextField texMinWaterSize;
    /** maximum Size of a lake */
    private TextField texMaxWaterSize;
    /** probability for water deeper than lvl1, Range 0..100 */
    private Label labProbDeep;
    private TextField texProbDeep;

    /** how much forests at least */
    private Label labForestSpots;
    private TextField texMinForestSpots;
    /** how much forests at most */
    private TextField texMaxForestSpots;
    /** minimum size of a forest */
    private Label labForestSize;
    private TextField texMinForestSize;
    /** maximum Size of a forest */
    private TextField texMaxForestSize;
    /** probability for heavy wood, Range 0..100 */
    private Label labProbHeavy;
    private TextField texProbHeavy;

    /** rough */
    private Label labRoughSpots;
    private TextField texMinRoughSpots;
    private TextField texMaxRoughSpots;
    private Label labRoughSize;
    private TextField texMinRoughSize;
    private TextField texMaxRoughSize;

    /** swamp */
    private Label labSwampSpots;
    private TextField texMinSwampSpots;
    private TextField texMaxSwampSpots;
    private Label labSwampSize;
    private TextField texMinSwampSize;
    private TextField texMaxSwampSize;

    /** pavement/ice */
    private Label labPavementSpots;
    private TextField texMinPavementSpots;
    private TextField texMaxPavementSpots;
    private Label labPavementSize;
    private TextField texMinPavementSize;
    private TextField texMaxPavementSize;
    private Label labIceSpots;
    private TextField texMinIceSpots;
    private TextField texMaxIceSpots;
    private Label labIceSize;
    private TextField texMinIceSize;
    private TextField texMaxIceSize;

    /** rubble / fortified */
    private Label labRubbleSpots;
    private TextField texMinRubbleSpots;
    private TextField texMaxRubbleSpots;
    private Label labRubbleSize;
    private TextField texMinRubbleSize;
    private TextField texMaxRubbleSize;
    private Label labFortifiedSpots;
    private TextField texMinFortifiedSpots;
    private TextField texMaxFortifiedSpots;
    private Label labFortifiedSize;
    private TextField texMinFortifiedSize;
    private TextField texMaxFortifiedSize;

    /** probability for a road, range 0..100 */
    private Label labProbRoad;
    private TextField texProbRoad;
    /** probability for a river, range 0..100 */
    private Label labProbRiver;
    private TextField texProbRiver;

    /* Craters */
    private Label labProbCrater;
    private TextField texProbCrater;
    private Label labRadius;
    private TextField texMinRadius;
    private TextField texMaxRadius;
    private Label labMaxCraters;
    private TextField texMaxCraters;
    private TextField texMinCraters;

    /* FX */
    private Label labProbDrought;
    private TextField texProbDrought;
    private Label labProbFire;
    private TextField texProbFire;
    private Label labProbFlood;
    private TextField texProbFlood;
    private Label labProbFreeze;
    private TextField texProbFreeze;
    private Label labFxMod;
    private TextField texFxMod;

    /* City */
    private Label labCityBlocks;
    private Label labCityCF;
    private Label labCityFloors;
    private Label labCityDensity;
    private Label labTownSize;
    private TextField texCityBlocks;
    private TextField texCityMinCF;
    private TextField texCityMaxCF;
    private TextField texCityMinFloors;
    private TextField texCityMaxFloors;
    private TextField texCityDensity;
    private TextField texTownSize;

    // Mountain
    private Label labMountainPeaks;
    private Label labMountainHeight;
    private Label labMountainWidth;
    private Label labMountainStyle;
    private TextField texMountainPeaks;
    private TextField texMountainStyle;
    private TextField texMountainHeightMin;
    private TextField texMountainHeightMax;
    private TextField texMountainWidthMin;
    private TextField texMountainWidthMax;

    /** Algorithm */
    private Label labAlgorithmToUse;
    private TextField texAlgorithmToUse;

    GridBagLayout gridbag;

    private MapSettings mapSettings = null;
    private Frame frame = null;
    private IMapSettingsObserver bsd = null;

    private boolean advanced = false;
    private boolean initiated = false;

    public RandomMapDialog(Frame parent, IMapSettingsObserver bsd,
            MapSettings mapSettings) {
        super(parent, Messages.getString("RandomMapDialog.title"), true); //$NON-NLS-1$
        this.mapSettings = mapSettings;
        frame = parent;
        this.bsd = bsd;
        setResizable(true);

        createComponents();
        loadValues();

        setLayout(new BorderLayout());
        setupOptions();
        add(scrAll, BorderLayout.CENTER);
        setupButtons();
        add(panButtons, BorderLayout.SOUTH);

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
            FileDialog fd = new FileDialog(
                    frame,
                    Messages.getString("RandomMapDialog.FileSaveDialog"), FileDialog.SAVE); //$NON-NLS-1$
            fd.setDirectory("data" + File.separatorChar + "boards");
            fd.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File f, String s) {
                    return s.endsWith(".xml");
                }
            });
            fd.setModal(true);
            fd.setVisible(true);
            String filename = fd.getDirectory() + File.separator + fd.getFile();
            if (filename.indexOf('.') == -1) {
                filename = filename + ".xml";
            }
            File f = new File(filename);
            try {
                mapSettings.save(new FileOutputStream(f));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource().equals(butLoad)) {
            FileDialog fd = new FileDialog(
                    frame,
                    Messages.getString("RandomMapDialog.FileLoadDialog"), FileDialog.LOAD); //$NON-NLS-1$
            fd.setDirectory("./data/boards/");
            fd.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File f, String s) {
                    return s.endsWith(".xml");
                }
            });
            fd.setModal(true);
            fd.setVisible(true);
            File f = new File(fd.getDirectory() + File.separator + fd.getFile());
            try {
                mapSettings.load(new FileInputStream(f));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            loadValues();
            if (!advanced) {
                advanced = true;
                butAdvanced.setLabel(Messages
                        .getString("RandomMapDialog.Normal")); //$NON-NLS-1$
                setupOptions();
                setProperSize();
            }
        } else {
            advanced = !advanced;
            if (advanced) {
                butAdvanced.setLabel(Messages
                        .getString("RandomMapDialog.Normal")); //$NON-NLS-1$
            } else {
                butAdvanced.setLabel(Messages
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
        scrAll.add(panOptions);

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

        butOK = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        butOK.addActionListener(this);

        butAdvanced = new Button(Messages.getString("RandomMapDialog.Advanced")); //$NON-NLS-1$
        butAdvanced.addActionListener(this);

        butSave = new Button(Messages.getString("RandomMapDialog.Save")); //$NON-NLS-1$
        butSave.addActionListener(this);

        butLoad = new Button(Messages.getString("RandomMapDialog.Load")); //$NON-NLS-1$
        butLoad.addActionListener(this);

        panButtons = new Panel();
        panButtons.setLayout(new FlowLayout());

        panOptions = new Panel();
        gridbag = new GridBagLayout();
        panOptions.setLayout(gridbag);

        labBoardSize = new Label(Messages
                .getString("RandomMapDialog.BoardSize"), Label.LEFT); //$NON-NLS-1$
        texBoardWidth = new TextField(2);
        texBoardWidth.addFocusListener(this);
        texBoardHeight = new TextField(2);
        texBoardHeight.addFocusListener(this);
        slBoardSize = new SimpleLine(NORMAL_LINE_WIDTH);

        // Normal setting components...
        labElevation = new Label(Messages
                .getString("RandomMapDialog.labElevation"), Label.LEFT); //$NON-NLS-1$
        choElevation = new Choice();
        fillChoice(choElevation);
        slElevation = new SimpleLine(NORMAL_LINE_WIDTH);

        labCliffs = new Label(
                Messages.getString("RandomMapDialog.labCliffs"), Label.LEFT); //$NON-NLS-1$
        choCliffs = new Choice();
        fillChoice(choCliffs);
        slCliffs = new SimpleLine(NORMAL_LINE_WIDTH);

        labWoods = new Label(
                Messages.getString("RandomMapDialog.labWoods"), Label.LEFT); //$NON-NLS-1$
        choWoods = new Choice();
        fillChoice(choWoods);
        slWoods = new SimpleLine(NORMAL_LINE_WIDTH);

        labLakes = new Label(
                Messages.getString("RandomMapDialog.labLakes"), Label.LEFT); //$NON-NLS-1$
        choLakes = new Choice();
        fillChoice(choLakes);
        slLakes = new SimpleLine(NORMAL_LINE_WIDTH);

        labRough = new Label(
                Messages.getString("RandomMapDialog.labRough"), Label.LEFT); //$NON-NLS-1$
        choRough = new Choice();
        fillChoice(choRough);
        slRough = new SimpleLine(NORMAL_LINE_WIDTH);

        labSwamp = new Label(
                Messages.getString("RandomMapDialog.labSwamp"), Label.LEFT); //$NON-NLS-1$
        choSwamp = new Choice();
        fillChoice(choSwamp);
        slSwamp = new SimpleLine(NORMAL_LINE_WIDTH);

        labPavement = new Label(Messages
                .getString("RandomMapDialog.labPavement"), Label.LEFT);
        choPavement = new Choice();
        fillChoice(choPavement);
        slPavement = new SimpleLine(NORMAL_LINE_WIDTH);

        labRubble = new Label(Messages.getString("RandomMapDialog.labRubble"),
                Label.LEFT);
        choRubble = new Choice();
        fillChoice(choRubble);
        slRubble = new SimpleLine(NORMAL_LINE_WIDTH);

        labFortified = new Label(Messages
                .getString("RandomMapDialog.labFortified"), Label.LEFT);
        choFortified = new Choice();
        fillChoice(choFortified);
        slFortified = new SimpleLine(NORMAL_LINE_WIDTH);

        labIce = new Label(Messages.getString("RandomMapDialog.labIce"),
                Label.LEFT);
        choIce = new Choice();
        fillChoice(choIce);
        slIce = new SimpleLine(NORMAL_LINE_WIDTH);

        labCraters = new Label(
                Messages.getString("RandomMapDialog.labCraters"), Label.LEFT);
        choCraters = new Choice();
        fillChoice(choCraters);
        slCraters = new SimpleLine(NORMAL_LINE_WIDTH);

        labRivers = new Label(
                Messages.getString("RandomMapDialog.labRivers"), Label.LEFT); //$NON-NLS-1$
        choRivers = new Choice();
        fillChoice(choRivers);
        slRivers = new SimpleLine(NORMAL_LINE_WIDTH);

        labRoads = new Label(
                Messages.getString("RandomMapDialog.labRoads"), Label.LEFT); //$NON-NLS-1$
        choRoads = new Choice();
        fillChoice(choRoads);
        slRoads = new SimpleLine(NORMAL_LINE_WIDTH);

        labCity = new Label(
                Messages.getString("RandomMapDialog.labCity"), Label.LEFT); //$NON-NLS-1$
        choCity = new Choice();
        choCity.add(NONE);
        choCity.add("HUB");
        choCity.add("GRID");
        choCity.add("METRO");
        choCity.add("TOWN");
        slCity = new SimpleLine(NORMAL_LINE_WIDTH);

        labMountain = new Label(Messages
                .getString("RandomMapDialog.labMountain"), Label.LEFT); //$NON-NLS-1$
        choMountain = new Choice();
        fillChoice(choMountain);
        slMountain = new SimpleLine(NORMAL_LINE_WIDTH);

        // Advanced setting components...
        labTheme = new Label(Messages.getString("RandomMapDialog.labTheme"),
                Label.LEFT);
        texTheme = new TextField(20);
        /** how much hills there should be, Range 0..99 */
        labHilliness = new Label(Messages
                .getString("RandomMapDialog.labHilliness"), Label.LEFT); //$NON-NLS-1$
        texHilliness = new TextField(2);
        texHilliness.addFocusListener(this);
        /** Maximum level of the map */
        labRange = new Label(
                Messages.getString("RandomMapDialog.labRange"), Label.LEFT); //$NON-NLS-1$
        texRange = new TextField(2);
        texRange.addFocusListener(this);
        labProbInvert = new Label(Messages
                .getString("RandomMapDialog.labProbInvert"), Label.LEFT); //$NON-NLS-1$
        texProbInvert = new TextField(2);
        texProbInvert.addFocusListener(this);
        labCliffsAd = new Label(
                Messages.getString("RandomMapDialog.labCliffs"), Label.LEFT); //$NON-NLS-1$
        texCliffs = new TextField(2);
        texCliffs.addFocusListener(this);

        // mountain
        labMountainHeight = new Label(Messages
                .getString("RandomMapDialog.labMountainHeight"), Label.LEFT); //$NON-NLS-1$
        labMountainWidth = new Label(Messages
                .getString("RandomMapDialog.labMountainWidth"), Label.LEFT); //$NON-NLS-1$
        labMountainPeaks = new Label(Messages
                .getString("RandomMapDialog.labMountainPeaks"), Label.LEFT); //$NON-NLS-1$
        labMountainStyle = new Label(Messages
                .getString("RandomMapDialog.labMountainStyle"), Label.LEFT); //$NON-NLS-1$
        texMountainPeaks = new TextField(2);
        texMountainPeaks.addFocusListener(this);
        texMountainHeightMin = new TextField(2);
        texMountainHeightMin.addFocusListener(this);
        texMountainHeightMax = new TextField(2);
        texMountainHeightMax.addFocusListener(this);
        texMountainWidthMin = new TextField(2);
        texMountainWidthMin.addFocusListener(this);
        texMountainWidthMax = new TextField(2);
        texMountainWidthMax.addFocusListener(this);
        texMountainStyle = new TextField(2);
        texMountainStyle.addFocusListener(this);

        /** how much Lakes at least */
        labWaterSpots = new Label(Messages
                .getString("RandomMapDialog.labWaterSpots"), Label.LEFT); //$NON-NLS-1$
        texMinWaterSpots = new TextField(2);
        texMinWaterSpots.addFocusListener(this);
        /** how much Lakes at most */
        texMaxWaterSpots = new TextField(2);
        texMaxWaterSpots.addFocusListener(this);
        /** minimum size of a lake */
        labWaterSize = new Label(Messages
                .getString("RandomMapDialog.labWaterSize"), Label.LEFT); //$NON-NLS-1$
        texMinWaterSize = new TextField(2);
        texMinWaterSize.addFocusListener(this);
        /** maximum Size of a lake */
        texMaxWaterSize = new TextField(2);
        texMaxWaterSize.addFocusListener(this);
        /** probability for water deeper than lvl1, Range 0..100 */
        labProbDeep = new Label(Messages
                .getString("RandomMapDialog.labProbDeep"), Label.LEFT); //$NON-NLS-1$
        texProbDeep = new TextField(2);
        texProbDeep.addFocusListener(this);

        /** how much forests at least */
        labForestSpots = new Label(Messages
                .getString("RandomMapDialog.labForestSpots"), Label.LEFT); //$NON-NLS-1$
        texMinForestSpots = new TextField(2);
        texMinForestSpots.addFocusListener(this);
        /** how much forests at most */
        texMaxForestSpots = new TextField(2);
        texMaxForestSpots.addFocusListener(this);
        /** minimum size of a forest */
        labForestSize = new Label(Messages
                .getString("RandomMapDialog.labForestSize"), Label.LEFT); //$NON-NLS-1$
        texMinForestSize = new TextField(2);
        texMinForestSize.addFocusListener(this);
        /** maximum Size of a forest */
        texMaxForestSize = new TextField(2);
        texMaxForestSize.addFocusListener(this);
        /** probability for heavy wood, Range 0..100 */
        labProbHeavy = new Label(Messages
                .getString("RandomMapDialog.labProbHeavy"), Label.LEFT); //$NON-NLS-1$
        texProbHeavy = new TextField(2);
        texProbHeavy.addFocusListener(this);

        /** rough */
        labRoughSpots = new Label(Messages
                .getString("RandomMapDialog.labRoughSpots"), Label.LEFT); //$NON-NLS-1$
        texMinRoughSpots = new TextField(2);
        texMinRoughSpots.addFocusListener(this);
        texMaxRoughSpots = new TextField(2);
        texMaxRoughSpots.addFocusListener(this);
        labRoughSize = new Label(Messages
                .getString("RandomMapDialog.labRoughSize"), Label.LEFT); //$NON-NLS-1$
        texMinRoughSize = new TextField(2);
        texMinRoughSize.addFocusListener(this);
        texMaxRoughSize = new TextField(2);
        texMaxRoughSize.addFocusListener(this);

        /** swamp */
        labSwampSpots = new Label(Messages
                .getString("RandomMapDialog.labSwampSpots"), Label.LEFT); //$NON-NLS-1$
        texMinSwampSpots = new TextField(2);
        texMinSwampSpots.addFocusListener(this);
        texMaxSwampSpots = new TextField(2);
        texMaxSwampSpots.addFocusListener(this);
        labSwampSize = new Label(Messages
                .getString("RandomMapDialog.labSwampSize"), Label.LEFT); //$NON-NLS-1$
        texMinSwampSize = new TextField(2);
        texMinSwampSize.addFocusListener(this);
        texMaxSwampSize = new TextField(2);
        texMaxSwampSize.addFocusListener(this);

        /** pavement */
        labPavementSpots = new Label(Messages
                .getString("RandomMapDialog.labPavementSpots"), Label.LEFT);
        texMinPavementSpots = new TextField(2);
        texMinPavementSpots.addFocusListener(this);
        texMaxPavementSpots = new TextField(2);
        texMaxPavementSpots.addFocusListener(this);
        labPavementSize = new Label(Messages
                .getString("RandomMapDialog.labPavementSize"), Label.LEFT);
        texMinPavementSize = new TextField(2);
        texMinPavementSize.addFocusListener(this);
        texMaxPavementSize = new TextField(2);
        texMaxPavementSize.addFocusListener(this);

        /** Rubble */
        labRubbleSpots = new Label(Messages
                .getString("RandomMapDialog.labRubbleSpots"), Label.LEFT);
        texMinRubbleSpots = new TextField(2);
        texMinRubbleSpots.addFocusListener(this);
        texMaxRubbleSpots = new TextField(2);
        texMaxRubbleSpots.addFocusListener(this);
        labRubbleSize = new Label(Messages
                .getString("RandomMapDialog.labRubbleSize"), Label.LEFT);
        texMinRubbleSize = new TextField(2);
        texMinRubbleSize.addFocusListener(this);
        texMaxRubbleSize = new TextField(2);
        texMaxRubbleSize.addFocusListener(this);

        /** Fortified */
        labFortifiedSpots = new Label(Messages
                .getString("RandomMapDialog.labFortifiedSpots"), Label.LEFT);
        texMinFortifiedSpots = new TextField(2);
        texMinFortifiedSpots.addFocusListener(this);
        texMaxFortifiedSpots = new TextField(2);
        texMaxFortifiedSpots.addFocusListener(this);
        labFortifiedSize = new Label(Messages
                .getString("RandomMapDialog.labFortifiedSize"), Label.LEFT);
        texMinFortifiedSize = new TextField(2);
        texMinFortifiedSize.addFocusListener(this);
        texMaxFortifiedSize = new TextField(2);
        texMaxFortifiedSize.addFocusListener(this);

        /** ice */
        labIceSpots = new Label(Messages
                .getString("RandomMapDialog.labIceSpots"), Label.LEFT);
        texMinIceSpots = new TextField(2);
        texMinIceSpots.addFocusListener(this);
        texMaxIceSpots = new TextField(2);
        texMaxIceSpots.addFocusListener(this);
        labIceSize = new Label(
                Messages.getString("RandomMapDialog.labIceSize"), Label.LEFT);
        texMinIceSize = new TextField(2);
        texMinIceSize.addFocusListener(this);
        texMaxIceSize = new TextField(2);
        texMaxIceSize.addFocusListener(this);

        /** probability for a road, range 0..100 */
        labProbRoad = new Label(Messages
                .getString("RandomMapDialog.labProbRoad"), Label.LEFT); //$NON-NLS-1$
        texProbRoad = new TextField(2);
        texProbRoad.addFocusListener(this);
        /** probability for a river, range 0..100 */
        labProbRiver = new Label(Messages
                .getString("RandomMapDialog.labProbRiver"), Label.LEFT); //$NON-NLS-1$
        texProbRiver = new TextField(2);
        texProbRiver.addFocusListener(this);

        /* Craters */
        labProbCrater = new Label(Messages
                .getString("RandomMapDialog.labProbCrater"), Label.LEFT); //$NON-NLS-1$
        texProbCrater = new TextField(2);
        texProbCrater.addFocusListener(this);
        labRadius = new Label(
                Messages.getString("RandomMapDialog.labRadius"), Label.LEFT); //$NON-NLS-1$
        texMinRadius = new TextField(2);
        texMinRadius.addFocusListener(this);
        texMaxRadius = new TextField(2);
        texMaxRadius.addFocusListener(this);
        labMaxCraters = new Label(Messages
                .getString("RandomMapDialog.labMaxCraters"), Label.LEFT); //$NON-NLS-1$
        texMaxCraters = new TextField(2);
        texMaxCraters.addFocusListener(this);
        texMinCraters = new TextField(2);
        texMinCraters.addFocusListener(this);

        /* FX */
        labProbDrought = new Label(Messages
                .getString("RandomMapDialog.labProbDrought"), Label.LEFT);
        labProbFire = new Label(Messages
                .getString("RandomMapDialog.labProbFire"), Label.LEFT);
        labProbFreeze = new Label(Messages
                .getString("RandomMapDialog.labProbFreeze"), Label.LEFT);
        labProbFlood = new Label(Messages
                .getString("RandomMapDialog.labProbFlood"), Label.LEFT);
        labFxMod = new Label(Messages.getString("RandomMapDialog.labFxMod"),
                Label.LEFT);
        texProbDrought = new TextField(2);
        texProbFire = new TextField(2);
        texProbFreeze = new TextField(2);
        texProbFlood = new TextField(2);
        texFxMod = new TextField(2);

        /* Buildings */
        labCityBlocks = new Label(Messages
                .getString("RandomMapDialog.labCityBlocks"), Label.LEFT);
        labCityCF = new Label(Messages.getString("RandomMapDialog.labCityCF"),
                Label.LEFT);
        labCityFloors = new Label(Messages
                .getString("RandomMapDialog.labCityFloors"), Label.LEFT);
        labCityDensity = new Label(Messages
                .getString("RandomMapDialog.labCityDensity"), Label.LEFT);
        labTownSize = new Label(Messages
                .getString("RandomMapDialog.labTownSize"), Label.LEFT);
        texCityBlocks = new TextField(2);
        texCityMinCF = new TextField(2);
        texCityMaxCF = new TextField(2);
        texCityMinFloors = new TextField(2);
        texCityMaxFloors = new TextField(2);
        texCityDensity = new TextField(2);
        texTownSize = new TextField(2);

        labInvertNegative = new Label(Messages
                .getString("RandomMapDialog.labInvertNegative"), Label.LEFT); //$NON-NLS-1$
        texInvertNegative = new TextField(1);

        /** Algorithm */
        labAlgorithmToUse = new Label(Messages
                .getString("RandomMapDialog.labAlgorithmToUse"), Label.LEFT); //$NON-NLS-1$
        texAlgorithmToUse = new TextField(2);

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

    private void addOption(Label label, Choice choice, SimpleLine sl) {
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

    private void fillChoice(Choice c) {
        c.add(NONE);
        c.add(LOW);
        c.add(MEDIUM);
        c.add(HIGH);
    }

    private void addLabelTextField(Label label, TextField text) {
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

    private void addLabelTextField(Label label, TextField text,
            TextField text2, String separator) {
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

        Label l = new Label(separator, Label.CENTER);
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
        texBoardWidth.setText(new Integer(mapSettings.getBoardWidth())
                .toString());
        texBoardHeight.setText(new Integer(mapSettings.getBoardHeight())
                .toString());
        texTheme.setText(mapSettings.getTheme());

        texHilliness
                .setText(new Integer(mapSettings.getHilliness()).toString());
        texRange.setText(new Integer(mapSettings.getRange()).toString());
        texProbInvert.setText(new Integer(mapSettings.getProbInvert())
                .toString());
        texCliffs.setText(new Integer(mapSettings.getCliffs()).toString());
        texMinWaterSpots.setText(new Integer(mapSettings.getMinWaterSpots())
                .toString());
        texMaxWaterSpots.setText(new Integer(mapSettings.getMaxWaterSpots())
                .toString());
        texMinWaterSize.setText(new Integer(mapSettings.getMinWaterSize())
                .toString());
        texMaxWaterSize.setText(new Integer(mapSettings.getMaxWaterSize())
                .toString());

        texMinForestSpots.setText(new Integer(mapSettings.getMinForestSpots())
                .toString());
        texMaxForestSpots.setText(new Integer(mapSettings.getMaxForestSpots())
                .toString());
        texMinForestSize.setText(new Integer(mapSettings.getMinForestSize())
                .toString());
        texMaxForestSize.setText(new Integer(mapSettings.getMaxForestSize())
                .toString());

        texMinRoughSpots.setText(new Integer(mapSettings.getMinRoughSpots())
                .toString());
        texMaxRoughSpots.setText(new Integer(mapSettings.getMaxRoughSpots())
                .toString());
        texMinRoughSize.setText(new Integer(mapSettings.getMinRoughSize())
                .toString());
        texMaxRoughSize.setText(new Integer(mapSettings.getMaxRoughSize())
                .toString());

        texMinSwampSpots.setText(new Integer(mapSettings.getMinSwampSpots())
                .toString());
        texMaxSwampSpots.setText(new Integer(mapSettings.getMaxSwampSpots())
                .toString());
        texMinSwampSize.setText(new Integer(mapSettings.getMinSwampSize())
                .toString());
        texMaxSwampSize.setText(new Integer(mapSettings.getMaxSwampSize())
                .toString());

        texMinPavementSpots.setText(new Integer(mapSettings
                .getMinPavementSpots()).toString());
        texMaxPavementSpots.setText(new Integer(mapSettings
                .getMaxPavementSpots()).toString());
        texMinPavementSize
                .setText(new Integer(mapSettings.getMinPavementSize())
                        .toString());
        texMaxPavementSize
                .setText(new Integer(mapSettings.getMaxPavementSize())
                        .toString());

        texMinRubbleSpots.setText(new Integer(mapSettings.getMinRubbleSpots())
                .toString());
        texMaxRubbleSpots.setText(new Integer(mapSettings.getMaxRubbleSpots())
                .toString());
        texMinRubbleSize.setText(new Integer(mapSettings.getMinRubbleSize())
                .toString());
        texMaxRubbleSize.setText(new Integer(mapSettings.getMaxRubbleSize())
                .toString());

        texMinFortifiedSpots.setText(new Integer(mapSettings
                .getMinFortifiedSpots()).toString());
        texMaxFortifiedSpots.setText(new Integer(mapSettings
                .getMaxFortifiedSpots()).toString());
        texMinFortifiedSize.setText(new Integer(mapSettings
                .getMinFortifiedSize()).toString());
        texMaxFortifiedSize.setText(new Integer(mapSettings
                .getMaxFortifiedSize()).toString());

        texMinIceSpots.setText(new Integer(mapSettings.getMinIceSpots())
                .toString());
        texMaxIceSpots.setText(new Integer(mapSettings.getMaxIceSpots())
                .toString());
        texMinIceSize.setText(new Integer(mapSettings.getMinIceSize())
                .toString());
        texMaxIceSize.setText(new Integer(mapSettings.getMaxIceSize())
                .toString());

        texProbDeep.setText(new Integer(mapSettings.getProbDeep()).toString());
        texProbHeavy
                .setText(new Integer(mapSettings.getProbHeavy()).toString());
        texProbRiver
                .setText(new Integer(mapSettings.getProbRiver()).toString());
        texProbRoad.setText(new Integer(mapSettings.getProbRoad()).toString());
        texProbCrater.setText(new Integer(mapSettings.getProbCrater())
                .toString());
        texMinRadius
                .setText(new Integer(mapSettings.getMinRadius()).toString());
        texMaxRadius
                .setText(new Integer(mapSettings.getMaxRadius()).toString());
        texMaxCraters.setText(new Integer(mapSettings.getMaxCraters())
                .toString());
        texMinCraters.setText(new Integer(mapSettings.getMinCraters())
                .toString());

        texProbDrought.setText(new Integer(mapSettings.getProbDrought())
                .toString());
        texProbFire.setText(new Integer(mapSettings.getProbForestFire())
                .toString());
        texProbFreeze.setText(new Integer(mapSettings.getProbFreeze())
                .toString());
        texProbFlood
                .setText(new Integer(mapSettings.getProbFlood()).toString());
        texFxMod.setText(new Integer(mapSettings.getFxMod()).toString());

        choCity.select(mapSettings.getCityType());
        texInvertNegative.setText(new Integer(mapSettings
                .getInvertNegativeTerrain()).toString());
        texCityBlocks.setText(new Integer(mapSettings.getCityBlocks())
                .toString());
        texCityMinCF
                .setText(new Integer(mapSettings.getCityMinCF()).toString());
        texCityMaxCF
                .setText(new Integer(mapSettings.getCityMaxCF()).toString());
        texCityMinFloors.setText(new Integer(mapSettings.getCityMinFloors())
                .toString());
        texCityMaxFloors.setText(new Integer(mapSettings.getCityMaxFloors())
                .toString());
        texCityDensity.setText(new Integer(mapSettings.getCityDensity())
                .toString());
        texTownSize.setText(new Integer(mapSettings.getTownSize()).toString());

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
        texAlgorithmToUse.setText(new Integer(mapSettings.getAlgorithmToUse())
                .toString());
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
            new AlertDialog(frame, INVALID_SETTING, Messages
                    .getString("RandomMapDialog.OnlyIntegersWarn")).setVisible(true); //$NON-NLS-1$
            return false;
        }

        if ((boardWidth <= 0) || (boardHeight <= 0)) {
            new AlertDialog(frame, INVALID_SETTING, Messages
                    .getString("RandomMapDialog.BoardSizeWarn")).setVisible(true); //$NON-NLS-1$
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
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.OnlyIntegersWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }

            if ((hilliness < 0) || (hilliness > 99)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.AmmountOfElevationWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if ((cliffs < 0) || (cliffs > 100)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.CliffsWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (range < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.elevRangeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if ((probInvert < 0) || (probInvert > 100)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.depressionWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minWaterSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinLakesWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxLakesWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSpots < minWaterSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxLakesWarn2")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minWaterSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinLakeSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxLakeSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSize < minWaterSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxLakeSizeWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if ((probDeep < 0) || (probDeep > 100)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.DeepWaterProbWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minForestSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinForestsWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxForestSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxForestsWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxForestSpots < minForestSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxForestsWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minForestSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinForestSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxForestSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxForestSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxForestSize < minForestSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxForestSizeWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if ((probHeavy < 0) || (probHeavy > 100)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.HeavyForestProbWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minRoughSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinRoughsWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxRoughsWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSpots < minRoughSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxRoughsWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minRoughSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinRoughSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxRoughSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSize < minRoughSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxRoughSizeWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minSwampSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinSwampsWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxSwampsWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSpots < minSwampSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxSwampsWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minSwampSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinSwampSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxSwampSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSize < minSwampSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxSwampSizeWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minPavementSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinPavementWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxPavementSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxPavementWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxPavementSpots < minPavementSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxPavementWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minPavementSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinPavementSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxPavementSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxPavementSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxPavementSize < minPavementSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxPavementSizeWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minRubbleSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinRubbleWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxRubbleSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxRubbleWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxRubbleSpots < minRubbleSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxRubbleWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minRubbleSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinRubbleSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxRubbleSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxRubbleSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxRubbleSize < minRubbleSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxRubbleSizeWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minFortifiedSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinFortifiedWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxFortifiedSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxFortifiedWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxFortifiedSpots < minFortifiedSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxFortifiedWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minFortifiedSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinFortifiedSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxFortifiedSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxFortifiedSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxFortifiedSize < minFortifiedSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxFortifiedSizeWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minIceSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinIceWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxIceSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxIceWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxIceSpots < minIceSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxIceWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minIceSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinIceSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxIceSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxIceSizeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxIceSize < minIceSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxIceSizeWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if ((probRiver < 0) || (probRiver > 100)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.RiverProbWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if ((probRoad < 0) || (probRoad > 100)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.RoadProbWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if ((probCrater < 0) || (probCrater > 100)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.CratersProbWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minRadius < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinCraterRadiusWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxRadius < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxCraterRadiusWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxRadius < minRadius) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxCraterRadiusWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxCraters < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxCratersWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (minCraters < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MinCratersWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if (maxCraters < minCraters) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.MaxCratersWarn1")).setVisible(true); //$NON-NLS-1$
                return false;
            }
            if ((algorithmToUse < 0) || (algorithmToUse > 2)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.AlgorithmWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }

            if ((cityMinCF < 1) || (cityMaxCF > 150)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.CFOutOfRangeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }

            if ((cityMinFloors < 1) || (cityMaxFloors > 100)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.FloorsOutOfRangeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }

            if ((cityDensity < 1) || (cityDensity > 100)) {
                new AlertDialog(frame, INVALID_SETTING, Messages
                        .getString("RandomMapDialog.DensityOutOfRangeWarn")).setVisible(true); //$NON-NLS-1$
                return false;
            }

            if (((mountainWidthMin < 1) || (mountainWidthMax < mountainWidthMin))
                    && (mountainPeaks > 0)) {
                new AlertDialog(
                        frame,
                        INVALID_SETTING,
                        Messages
                                .getString("RandomMapDialog.MountainWidthOutOfRangeWarn")).setVisible(true); //$NON-NLS-1$
            }

        } else {
            String s = choElevation.getSelectedItem();
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
            s = choCliffs.getSelectedItem();
            if (s.equals(NONE)) {
                cliffs = 0;
            } else if (s.equals(LOW)) {
                cliffs = 25;
            } else if (s.equals(MEDIUM)) {
                cliffs = 50;
            } else {
                cliffs = 75;
            }
            s = choWoods.getSelectedItem();
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
            s = choLakes.getSelectedItem();
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
            s = choRough.getSelectedItem();
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
            s = choSwamp.getSelectedItem();
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
            s = choPavement.getSelectedItem();
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
            s = choRubble.getSelectedItem();
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
            s = choFortified.getSelectedItem();
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
            s = choIce.getSelectedItem();
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
            s = choRoads.getSelectedItem();
            if (s.equals(NONE)) {
                probRoad = 0;
            } else if (s.equals(LOW)) {
                probRoad = 25;
            } else if (s.equals(MEDIUM)) {
                probRoad = 50;
            } else {
                probRoad = 75;
            }
            s = choRivers.getSelectedItem();
            if (s.equals(NONE)) {
                probRiver = 0;
            } else if (s.equals(LOW)) {
                probRiver = 25;
            } else if (s.equals(MEDIUM)) {
                probRiver = 50;
            } else {
                probRiver = 75;
            }

            s = choCraters.getSelectedItem();
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

            s = choMountain.getSelectedItem();
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

        cityType = choCity.getSelectedItem();
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
        if (fe.getSource() instanceof TextField) {
            TextField tf = (TextField) fe.getSource();
            tf.selectAll();
        }
    }

    public void focusLost(FocusEvent fe) {
        if (fe.getSource() instanceof TextField) {
            TextField tf = (TextField) fe.getSource();
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
