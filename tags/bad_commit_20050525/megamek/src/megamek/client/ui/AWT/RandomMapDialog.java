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

package megamek.client;

import megamek.client.util.*;
import megamek.common.*;
import java.awt.*;
import java.awt.event.*;

public class RandomMapDialog
    extends Dialog implements ActionListener, FocusListener
{
    private static final String NONE   = Messages.getString("RandomMapDialog.elevNONE"); //$NON-NLS-1$
    private static final String LOW    = Messages.getString("RandomMapDialog.elevLow"); //$NON-NLS-1$
    private static final String MEDIUM = Messages.getString("RandomMapDialog.elevMedium"); //$NON-NLS-1$
    private static final String HIGH   = Messages.getString("RandomMapDialog.elevHigh"); //$NON-NLS-1$

    private static final String INVALID_SETTING  = Messages.getString("RandomMapDialog.InvalidSetting"); //$NON-NLS-1$

    private static final int NORMAL_LINE_WIDTH = 195;
    private static final int ADVANCED_LINE_WIDTH = 295;

    private Button butOK = null;
    private Button butAdvanced = null;
    
    private Panel panButtons = null;
    private Panel panOptions = null;

    private Label labBoardSize = null;
    private Label labBoardDivider = null;
    private TextField texBoardWidth = null;
    private TextField texBoardHeight = null;

    private Choice choElevation = null;
    private Choice choWoods = null;
    private Choice choLakes = null;
    private Choice choRough = null;
    private Choice choRoads = null;
    private Choice choRivers = null;
    private Choice choSwamp = null;

    private Label labElevation = null;
    private Label labWoods = null;
    private Label labLakes = null;
    private Label labRough = null;
    private Label labRoads = null;
    private Label labRivers = null;
    private Label labSwamp = null;
    
    private SimpleLine slElevation = null;
    private SimpleLine slWoods = null;
    private SimpleLine slLakes = null;
    private SimpleLine slRough = null;
    private SimpleLine slRoads = null;
    private SimpleLine slRivers = null;
    private SimpleLine slSwamp = null;
    private SimpleLine slBoardSize = null;

    private SimpleLine slElevationAd = null;
    private SimpleLine slWoodsAd = null;
    private SimpleLine slLakesAd = null;
    private SimpleLine slRoughAd = null;
    private SimpleLine slRoadsAd = null;
    private SimpleLine slRiversAd = null;
    private SimpleLine slSwampAd = null;
    private SimpleLine slBoardSizeAd = null;
    private SimpleLine slCratersAd = null;

    /** how much hills there should be, Range 0..100 */
    private Label labHilliness;
    private TextField texHilliness;
    /** Maximum level of the map */
    private Label labRange;
    private TextField texRange;
    private Label labProbInvert;
    private TextField texProbInvert;
    
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
    
    /** Algorithm */
    private Label labAlgorithmToUse;
    private TextField texAlgorithmToUse;

    GridBagLayout gridbag;
    
    private MapSettings mapSettings = null;
    private Frame frame = null;
    private BoardSelectionDialog bsd = null;

    private boolean advanced = false;
    private boolean initiated = false;

    public RandomMapDialog(Frame parent, BoardSelectionDialog bsd, MapSettings mapSettings) {
        super(parent, Messages.getString("RandomMapDialog.title"), true); //$NON-NLS-1$
        this.mapSettings = mapSettings;
        this.frame = parent;
        this.bsd = bsd;
        setResizable(false);
        
        createComponents();
        loadValues();

        setLayout(new BorderLayout());
        setupOptions();
        add(panOptions, BorderLayout.CENTER);
        setupButtons();
        add(panButtons, BorderLayout.SOUTH);

        validate();
        pack();
        
        butOK.requestFocus();

        setLocation(getParent().getLocation().x + getParent().getSize().width/2 - getSize().width/2,
                    getParent().getLocation().y + getParent().getSize().height/2 - getSize().height/2);
        initiated = true;
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butOK)) {
            if (applyValues()) {
                this.setVisible(false);
            }
        } else {
            advanced = !advanced;
            if (advanced) {
                butAdvanced.setLabel(Messages.getString("RandomMapDialog.Normal")); //$NON-NLS-1$
            } else {
                butAdvanced.setLabel(Messages.getString("RandomMapDialog.Advanced")); //$NON-NLS-1$
            }
            setupOptions();
        }
    }
    
    private void setupOptions() {
        panOptions.removeAll();

        addLabelTextField(labBoardSize, texBoardWidth, texBoardHeight, "x"); //$NON-NLS-1$
        texBoardWidth.requestFocus();

        if (!advanced) {
            
            addSeparator(slBoardSize);

            addOption(labElevation, choElevation, slElevation);
            addOption(labWoods, choWoods, slWoods);
            addOption(labRough, choRough, slRough);
            addOption(labSwamp, choSwamp, slSwamp);
            addOption(labRoads, choRoads, slRoads);
            addOption(labLakes, choLakes, slLakes);
            addOption(labRivers, choRivers, slRivers);

        } else {

            addSeparator(slBoardSizeAd);

            addLabelTextField(labHilliness, texHilliness);
            addLabelTextField(labRange, texRange);
            addLabelTextField(labProbInvert, texProbInvert);
            addLabelTextField(labAlgorithmToUse, texAlgorithmToUse);

            addSeparator(slElevationAd);
            
            addLabelTextField(labForestSpots, texMinForestSpots, texMaxForestSpots, "-"); //$NON-NLS-1$
            addLabelTextField(labForestSize, texMinForestSize, texMaxForestSize, "-"); //$NON-NLS-1$
            addLabelTextField(labProbHeavy, texProbHeavy);

            addSeparator(slWoodsAd);

            addLabelTextField(labRoughSpots, texMinRoughSpots, texMaxRoughSpots, "-"); //$NON-NLS-1$
            addLabelTextField(labRoughSize, texMinRoughSize, texMaxRoughSize, "-"); //$NON-NLS-1$
        
            addSeparator(slRoughAd);
            
            addLabelTextField(labSwampSpots, texMinSwampSpots, texMaxSwampSpots, "-"); //$NON-NLS-1$
            addLabelTextField(labSwampSize, texMinSwampSize, texMaxSwampSize, "-"); //$NON-NLS-1$
        
            addSeparator(slSwampAd);

            addLabelTextField(labProbRoad, texProbRoad);
        
            addSeparator(slRoadsAd);

            addLabelTextField(labWaterSpots, texMinWaterSpots, texMaxWaterSpots, "-"); //$NON-NLS-1$
            addLabelTextField(labWaterSize, texMinWaterSize, texMaxWaterSize, "-"); //$NON-NLS-1$
            addLabelTextField(labProbDeep, texProbDeep);

            addSeparator(slLakesAd);

            addLabelTextField(labProbRiver, texProbRiver);

            addSeparator(slRiversAd);

            addLabelTextField(labProbCrater, texProbCrater);
            addLabelTextField(labMaxCraters, texMinCraters, texMaxCraters, "-"); //$NON-NLS-1$
            addLabelTextField(labRadius, texMinRadius, texMaxRadius, "-"); //$NON-NLS-1$

            addSeparator(slCratersAd);

        }
        
        if (initiated) {
            pack();
            setLocation(getParent().getLocation().x + getParent().getSize().width/2 - getSize().width/2,
                    getParent().getLocation().y + getParent().getSize().height/2 - getSize().height/2);
        }
    }

    private void setupButtons() {

        panButtons.add(butOK);
        panButtons.add(butAdvanced);
        
    }
    
    private void createComponents() {

        butOK = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        butOK.addActionListener(this);
        
        butAdvanced = new Button(Messages.getString("RandomMapDialog.Advanced")); //$NON-NLS-1$
        butAdvanced.addActionListener(this);

        panButtons = new Panel();       
        panButtons.setLayout(new FlowLayout());

        panOptions = new Panel();
        gridbag = new GridBagLayout();
        panOptions.setLayout(gridbag);

        labBoardSize = new Label(Messages.getString("RandomMapDialog.BoardSize"), Label.LEFT); //$NON-NLS-1$
        labBoardDivider = new Label("x", Label.CENTER); //$NON-NLS-1$
        texBoardWidth = new TextField(2);
            texBoardWidth.addFocusListener(this);
        texBoardHeight = new TextField(2);
            texBoardHeight.addFocusListener(this);
        slBoardSize = new SimpleLine(NORMAL_LINE_WIDTH);

        // Normal setting components...
        labElevation = new Label(Messages.getString("RandomMapDialog.labElevation"), Label.LEFT); //$NON-NLS-1$
        choElevation = new Choice();
        fillChoice(choElevation);
        slElevation = new SimpleLine(NORMAL_LINE_WIDTH);

        labWoods = new Label(Messages.getString("RandomMapDialog.labWoods"), Label.LEFT); //$NON-NLS-1$
        choWoods = new Choice();
        fillChoice(choWoods);
        slWoods = new SimpleLine(NORMAL_LINE_WIDTH);

        labLakes = new Label(Messages.getString("RandomMapDialog.labLakes"), Label.LEFT); //$NON-NLS-1$
        choLakes = new Choice();
        fillChoice(choLakes);
        slLakes = new SimpleLine(NORMAL_LINE_WIDTH);

        labRough = new Label(Messages.getString("RandomMapDialog.labRough"), Label.LEFT); //$NON-NLS-1$
        choRough = new Choice();
        fillChoice(choRough);
        slRough = new SimpleLine(NORMAL_LINE_WIDTH);
        
        labSwamp = new Label(Messages.getString("RandomMapDialog.labSwamp"), Label.LEFT); //$NON-NLS-1$
        choSwamp = new Choice();
        fillChoice(choSwamp);
        slSwamp = new SimpleLine(NORMAL_LINE_WIDTH);

        labRivers = new Label(Messages.getString("RandomMapDialog.labRivers"), Label.LEFT); //$NON-NLS-1$
        choRivers = new Choice();
        fillChoice(choRivers);
        slRivers = new SimpleLine(NORMAL_LINE_WIDTH);

        labRoads = new Label(Messages.getString("RandomMapDialog.labRoads"), Label.LEFT); //$NON-NLS-1$
        choRoads = new Choice();
        fillChoice(choRoads);
        slRoads = new SimpleLine(NORMAL_LINE_WIDTH);

        // Advanced setting components...
        /** how much hills there should be, Range 0..1000 */
        labHilliness = new Label(Messages.getString("RandomMapDialog.labHilliness"), Label.LEFT); //$NON-NLS-1$
        texHilliness = new TextField(2);
        texHilliness.addFocusListener(this);
        /** Maximum level of the map */
        labRange = new Label(Messages.getString("RandomMapDialog.labRange"), Label.LEFT); //$NON-NLS-1$
        texRange = new TextField(2);
        texRange.addFocusListener(this);
        labProbInvert = new Label(Messages.getString("RandomMapDialog.labProbInvert"), Label.LEFT); //$NON-NLS-1$
        texProbInvert = new TextField(2);
        texProbInvert.addFocusListener(this);
        
        /** how much Lakes at least */
        labWaterSpots= new Label(Messages.getString("RandomMapDialog.labWaterSpots"), Label.LEFT); //$NON-NLS-1$
        texMinWaterSpots= new TextField(2);
        texMinWaterSpots.addFocusListener(this);
        /** how much Lakes at most */
        texMaxWaterSpots= new TextField(2);
        texMaxWaterSpots.addFocusListener(this);
        /** minimum size of a lake */
        labWaterSize= new Label(Messages.getString("RandomMapDialog.labWaterSize"), Label.LEFT); //$NON-NLS-1$
        texMinWaterSize= new TextField(2);
        texMinWaterSize.addFocusListener(this);
        /** maximum Size of a lake */
        texMaxWaterSize= new TextField(2);
        texMaxWaterSize.addFocusListener(this);
        /** probability for water deeper than lvl1, Range 0..100 */
        labProbDeep= new Label(Messages.getString("RandomMapDialog.labProbDeep"), Label.LEFT); //$NON-NLS-1$
        texProbDeep= new TextField(2);
        texProbDeep.addFocusListener(this);
        
        /** how much forests at least */
        labForestSpots = new Label(Messages.getString("RandomMapDialog.labForestSpots"), Label.LEFT); //$NON-NLS-1$
        texMinForestSpots = new TextField(2);
        texMinForestSpots.addFocusListener(this);
        /** how much forests at most */
        texMaxForestSpots= new TextField(2);
        texMaxForestSpots.addFocusListener(this);
        /** minimum size of a forest */
        labForestSize= new Label(Messages.getString("RandomMapDialog.labForestSize"), Label.LEFT); //$NON-NLS-1$
        texMinForestSize= new TextField(2);
        texMinForestSize.addFocusListener(this);
        /** maximum Size of a forest */
        texMaxForestSize= new TextField(2);
        texMaxForestSize.addFocusListener(this);
        /** probability for heavy wood, Range 0..100 */
        labProbHeavy = new Label(Messages.getString("RandomMapDialog.labProbHeavy"), Label.LEFT); //$NON-NLS-1$
        texProbHeavy = new TextField(2);
        texProbHeavy.addFocusListener(this);
        
        /** rough */
        labRoughSpots= new Label(Messages.getString("RandomMapDialog.labRoughSpots"), Label.LEFT); //$NON-NLS-1$
        texMinRoughSpots= new TextField(2);
        texMinRoughSpots.addFocusListener(this);
        texMaxRoughSpots= new TextField(2);
        texMaxRoughSpots.addFocusListener(this);
        labRoughSize= new Label(Messages.getString("RandomMapDialog.labRoughSize"), Label.LEFT); //$NON-NLS-1$
        texMinRoughSize= new TextField(2);
        texMinRoughSize.addFocusListener(this);
        texMaxRoughSize= new TextField(2);
        texMaxRoughSize.addFocusListener(this);
        
        /** swamp */
        labSwampSpots= new Label(Messages.getString("RandomMapDialog.labSwampSpots"), Label.LEFT); //$NON-NLS-1$
        texMinSwampSpots= new TextField(2);
        texMinSwampSpots.addFocusListener(this);
        texMaxSwampSpots= new TextField(2);
        texMaxSwampSpots.addFocusListener(this);
        labSwampSize= new Label(Messages.getString("RandomMapDialog.labSwampSize"), Label.LEFT); //$NON-NLS-1$
        texMinSwampSize= new TextField(2);
        texMinSwampSize.addFocusListener(this);
        texMaxSwampSize= new TextField(2);
        texMaxSwampSize.addFocusListener(this);
        
        /** probability for a road, range 0..100 */
        labProbRoad= new Label(Messages.getString("RandomMapDialog.labProbRoad"), Label.LEFT); //$NON-NLS-1$
        texProbRoad= new TextField(2);
        texProbRoad.addFocusListener(this);
        /** probability for a river, range 0..100 */
        labProbRiver= new Label(Messages.getString("RandomMapDialog.labProbRiver"), Label.LEFT); //$NON-NLS-1$
        texProbRiver= new TextField(2);
        texProbRiver.addFocusListener(this);
        
        /* Craters */
        labProbCrater= new Label(Messages.getString("RandomMapDialog.labProbCrater"), Label.LEFT); //$NON-NLS-1$
        texProbCrater= new TextField(2);
        texProbCrater.addFocusListener(this);
        labRadius= new Label(Messages.getString("RandomMapDialog.labRadius"), Label.LEFT); //$NON-NLS-1$
        texMinRadius= new TextField(2);
        texMinRadius.addFocusListener(this);
        texMaxRadius= new TextField(2);
        texMaxRadius.addFocusListener(this);
        labMaxCraters= new Label(Messages.getString("RandomMapDialog.labMaxCraters"), Label.LEFT); //$NON-NLS-1$
        texMaxCraters= new TextField(2);    
        texMaxCraters.addFocusListener(this);
        texMinCraters= new TextField(2);
        texMinCraters.addFocusListener(this);
        
        /** Algorithm */
        labAlgorithmToUse = new Label(Messages.getString("RandomMapDialog.labAlgorithmToUse"), Label.LEFT); //$NON-NLS-1$
        texAlgorithmToUse = new TextField(2);
    
        slElevationAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slWoodsAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slLakesAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slRoughAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slRoadsAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slRiversAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slSwampAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slBoardSizeAd = new SimpleLine(ADVANCED_LINE_WIDTH);
        slCratersAd = new SimpleLine(ADVANCED_LINE_WIDTH);

    }
    
    private void addOption(Label label, Choice choice, SimpleLine sl) {
        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;    c.weighty = 1;
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

        c.weightx = 1;    c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(label, c);
        panOptions.add(label);

        c.weightx = 0;    c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(text, c);
        panOptions.add(text);
    }

    private void addLabelTextField(Label label, TextField text, TextField text2, String separator) {
        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;    c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(label, c);
        panOptions.add(label);

        c.weightx = 0;    c.weighty = 0;
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
        texBoardWidth.setText(new Integer(mapSettings.getBoardWidth()).toString());
        texBoardHeight.setText(new Integer(mapSettings.getBoardHeight()).toString());

        texHilliness.setText(new Integer(mapSettings.getHilliness()).toString());
        texRange.setText(new Integer(mapSettings.getRange()).toString());
        texProbInvert.setText(new Integer(mapSettings.getProbInvert()).toString());
        texMinWaterSpots.setText(new Integer(mapSettings.getMinWaterSpots()).toString());
        texMaxWaterSpots.setText(new Integer(mapSettings.getMaxWaterSpots()).toString());
        texMinWaterSize.setText(new Integer(mapSettings.getMinWaterSize()).toString());
        texMaxWaterSize.setText(new Integer(mapSettings.getMaxWaterSize()).toString());
        
        texMinForestSpots.setText(new Integer(mapSettings.getMinForestSpots()).toString());
        texMaxForestSpots.setText(new Integer(mapSettings.getMaxForestSpots()).toString());
        texMinForestSize.setText(new Integer(mapSettings.getMinForestSize()).toString());
        texMaxForestSize.setText(new Integer(mapSettings.getMaxForestSize()).toString());
        
        texMinRoughSpots.setText(new Integer(mapSettings.getMinRoughSpots()).toString());
        texMaxRoughSpots.setText(new Integer(mapSettings.getMaxRoughSpots()).toString());
        texMinRoughSize.setText(new Integer(mapSettings.getMinRoughSize()).toString());
        texMaxRoughSize.setText(new Integer(mapSettings.getMaxRoughSize()).toString());
        
        texMinSwampSpots.setText(new Integer(mapSettings.getMinSwampSpots()).toString());
        texMaxSwampSpots.setText(new Integer(mapSettings.getMaxSwampSpots()).toString());
        texMinSwampSize.setText(new Integer(mapSettings.getMinSwampSize()).toString());
        texMaxSwampSize.setText(new Integer(mapSettings.getMaxSwampSize()).toString());
        
        texProbDeep.setText(new Integer(mapSettings.getProbDeep()).toString());
        texProbHeavy.setText(new Integer(mapSettings.getProbHeavy()).toString());
        texProbRiver.setText(new Integer(mapSettings.getProbRiver()).toString());
        texProbRoad.setText(new Integer(mapSettings.getProbRoad()).toString());
        texProbCrater.setText(new Integer(mapSettings.getProbCrater()).toString());
        texMinRadius.setText(new Integer(mapSettings.getMinRadius()).toString());
        texMaxRadius.setText(new Integer(mapSettings.getMaxRadius()).toString());
        texMaxCraters.setText(new Integer(mapSettings.getMaxCraters()).toString());
        texMinCraters.setText(new Integer(mapSettings.getMinCraters()).toString());
        texAlgorithmToUse.setText(new Integer(mapSettings.getAlgorithmToUse()).toString());
    }
    
    private boolean applyValues() {
        int boardWidth;
        int boardHeight;
        int hilliness, range;
        int minWaterSpots, maxWaterSpots, minWaterSize, maxWaterSize, probDeep;
        int minForestSpots, maxForestSpots, minForestSize, maxForestSize, probHeavy;
        int minRoughSpots, maxRoughSpots, minRoughSize, maxRoughSize;
        int minSwampSpots, maxSwampSpots, minSwampSize, maxSwampSize;
        int probRoad, probRiver, probInvert;
        int minRadius, maxRadius, minCraters, maxCraters, probCrater;
        int algorithmToUse;

        try {
            boardWidth = Integer.parseInt(texBoardWidth.getText());
            boardHeight = Integer.parseInt(texBoardHeight.getText());
        } catch (NumberFormatException ex) {
            new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.OnlyIntegersWarn")).show(); //$NON-NLS-1$
            return false;
        }
        
        if (boardHeight <= 0 || boardHeight <= 0) {
            new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.BoardSizeWarn")).show(); //$NON-NLS-1$
            return false;
        }

        if (advanced) {
            try {
                hilliness = Integer.parseInt(texHilliness.getText());
                range = Integer.parseInt(texRange.getText());
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
            } catch (NumberFormatException ex) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.OnlyIntegersWarn")).show(); //$NON-NLS-1$
                return false;
            }
            
            if (hilliness < 0 || hilliness > 99) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.AmmountOfElevationWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (range < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.elevRangeWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (probInvert < 0 || probInvert > 100) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.depressionWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (minWaterSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MinLakesWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxLakesWarn1")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSpots < minWaterSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxLakesWarn2")).show(); //$NON-NLS-1$
                return false;
            }
            if (minWaterSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MinLakeSizeWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxLakeSizeWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxWaterSize < minWaterSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxLakeSizeWarn1")).show(); //$NON-NLS-1$
                return false;
            }
            if (probDeep < 0 || probDeep > 100) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.DeepWaterProbWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (minForestSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MinForestsWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxForestSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxForestsWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxForestSpots < minForestSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxForestsWarn1")).show(); //$NON-NLS-1$
                return false;
            }
            if (minForestSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MinForestSizeWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxForestSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxForestSizeWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxForestSize < minForestSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxForestSizeWarn1")).show(); //$NON-NLS-1$
                return false;
            }
            if (probHeavy < 0 || probHeavy > 100) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.HeavyForestProbWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (minRoughSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MinRoughsWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxRoughsWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSpots < minRoughSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxRoughsWarn1")).show(); //$NON-NLS-1$
                return false;
            }
            if (minRoughSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MinRoughSizeWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxRoughSizeWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxRoughSize < minRoughSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxRoughSizeWarn1")).show(); //$NON-NLS-1$
                return false;
            }
            if (minSwampSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MinSwampsWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSpots < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxSwampsWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSpots < minSwampSpots) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxSwampsWarn1")).show(); //$NON-NLS-1$
                return false;
            }
            if (minSwampSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MinSwampSizeWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSize < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxSwampSizeWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxSwampSize < minSwampSize) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxSwampSizeWarn1")).show(); //$NON-NLS-1$
                return false;
            }
            if (probRiver < 0 || probRiver > 100) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.RiverProbWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (probRoad < 0 || probRoad > 100) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.RoadProbWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (probCrater < 0 || probCrater > 100) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.CratersProbWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (minRadius < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MinCraterRadiusWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxRadius < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxCraterRadiusWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxRadius < minRadius) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxCraterRadiusWarn1")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxCraters < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxCratersWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (minCraters < 0) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MinCratersWarn")).show(); //$NON-NLS-1$
                return false;
            }
            if (maxCraters < minCraters) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.MaxCratersWarn1")).show(); //$NON-NLS-1$
                return false;
            }
            if (algorithmToUse < 0 || algorithmToUse > 2) {
                new AlertDialog(frame, INVALID_SETTING, Messages.getString("RandomMapDialog.AlgorithmWarn")).show(); //$NON-NLS-1$
                return false;
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
            
            probCrater = 0;
            minRadius = 0;
            maxRadius = 0;
            minCraters = 0;
            maxCraters = 0;
            algorithmToUse = 0;
            probInvert = 0;
        }
        
        mapSettings.setBoardSize(boardWidth, boardHeight);
        mapSettings.setElevationParams(hilliness, range, probInvert);
        mapSettings.setWaterParams(minWaterSpots, maxWaterSpots, 
                                    minWaterSize, maxWaterSize, probDeep);
        mapSettings.setForestParams(minForestSpots, maxForestSpots,
                                    minForestSize, maxForestSize, probHeavy);
        mapSettings.setRoughParams(minRoughSpots, maxRoughSpots,
                                    minRoughSize, maxRoughSize);
        mapSettings.setSwampParams(minSwampSpots, maxSwampSpots,
                                    minSwampSize, maxSwampSize);
        mapSettings.setRiverParam(probRiver);
        mapSettings.setRoadParam(probRoad);
        mapSettings.setCraterParam(probCrater, minCraters, maxCraters, minRadius, maxRadius);
        mapSettings.setAlgorithmToUse(algorithmToUse);
        
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
}
