/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2015 Nicholas Walczak (walczak@cs.umn.edu)
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

package megamek.client.ui.swing.skinEditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.Configuration;

/**
 * Panel with elements for viewing and adjusting a specific SkinSpecification.
 * 
 * @author arlith
 */
public class SkinSpecPanel extends JPanel implements ListSelectionListener,
        ActionListener {

    JFileChooser fileChooser = new JFileChooser(Configuration.widgetsDir());

    /**
     * A UI widget for displaying information related to a border widget (image
     * path and whether the image is tiled or not). Also supports a flag to
     * determine if the image should be allowed to tile (ie, corners should
     * never be tiled)
     * 
     * @author arlith
     *
     */
    private class BorderElement extends JPanel implements ActionListener,
            DocumentListener {

        /**
         * 
         */
        private static final long serialVersionUID = -2004313765932049794L;

        /**
         * Specifies the width of text fields
         */
        private static final int TEXTFIELD_COLS = 20;

        List<JButton> pathLbl = new ArrayList<>();

        List<JTextField> path = new ArrayList<>();

        List<JCheckBox> tiled = new ArrayList<>();

        List<JButton> removeButtons = new ArrayList<>();

        JButton addButton = new JButton(Messages.getString("SkinEditor.Add")); //$NON-NLS-1$

        boolean displayTiled = false;

        SkinSpecPanel skinPanel;

        /**
         * Constructor for BorderElements that only have one image (like
         * corners). The option to tile the image is not present, nor are the
         * add and remove buttons.
         *
         * @param elementName
         * @param imgPath
         */
        BorderElement(SkinSpecPanel skinPanel, String elementName,
                String imgPath) {
            super(new GridBagLayout());
            this.skinPanel = skinPanel;
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(), elementName,
                    TitledBorder.LEFT, TitledBorder.TOP));

            displayTiled = false;

            JButton newPathLbl = new JButton(
                    Messages.getString("SkinEditor.Path")); //$NON-NLS-1$
            newPathLbl.setMargin(new Insets(1, 1, 1, 1));
            newPathLbl.setToolTipText(Messages.getString(
                    "SkinEditor.PathToolTip", //$NON-NLS-1$
                    new Object[] { Configuration.widgetsDir().getPath() }));
            newPathLbl.addActionListener(this);
            pathLbl.add(newPathLbl);
            JTextField newPath = new JTextField(imgPath, TEXTFIELD_COLS);
            newPath.getDocument().addDocumentListener(this);
            path.add(newPath);
            JCheckBox newTiled = new JCheckBox(
                    Messages.getString("SkinEditor.Tiled"), //$NON-NLS-1$
                    false);
            newTiled.addActionListener(this);
            tiled.add(newTiled);
            removeButtons.add(new JButton());

            layoutPanel();
        }

        /**
         * Constructor for BorderElements that can have multiple images, like
         * edges. An add button is used to allow more images to be added and
         * each image path added has the option to be tiled or not. There is
         * also a remove button added for each entry to allow them to be
         * removed. Remove buttons are enabled if there are more than one image,
         * otherwise if only one image is specified then the remove button is
         * disabled.
         *
         * @param elementName
         * @param imgPath
         * @param isTiled
         */
        BorderElement(SkinSpecPanel skinPanel, String elementName,
                List<String> imgPath, List<Boolean> isTiled) {
            this(skinPanel, elementName, imgPath, isTiled, imgPath.size() > 1);
        }

        /**
         * Constructor for BorderElements that can have multiple images, like
         * edges. An add button is used to allow more images to be added and
         * each image path added has the option to be tiled or not. There is
         * also a remove button added for each entry to allow them to be
         * removed.
         *
         * @param elementName
         * @param imgPath
         * @param isTiled
         * @param removeEnabled
         *            Determines if remove buttons are enabled
         */
        BorderElement(SkinSpecPanel skinPanel, String elementName,
                List<String> imgPath, List<Boolean> isTiled,
                boolean removeEnabled) {
            super(new GridBagLayout());
            this.skinPanel = skinPanel;
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(), elementName,
                    TitledBorder.LEFT, TitledBorder.TOP));

            displayTiled = true;
            assert (imgPath.size() == isTiled.size());
            for (int i = 0; i < imgPath.size(); i++) {
                addPathRow(imgPath.get(i), isTiled.get(i), removeEnabled);
            }
            addButton.setToolTipText(Messages
                    .getString("SkinEditor.AddButtonToolTip")); //$NON-NLS-1$
            addButton.setMargin(new Insets(1, 1, 1, 1));
            addButton.setMaximumSize(new Dimension(40, 14));
            addButton.setPreferredSize(new Dimension(40, 14));
            addButton.addActionListener(this);
            layoutPanel();
        }

        /**
         * Used for BorderElements that have multiple entries, this method adds
         * a row of PathButton, Path text field, tiled checkbox and remove
         * button
         */
        protected void addPathRow(String imgPath, boolean isTiled,
                boolean removeEnabled) {
            JButton newPathLbl = new JButton(
                    Messages.getString("SkinEditor.Path")); //$NON-NLS-1$
            newPathLbl.setMargin(new Insets(1, 1, 1, 1));
            newPathLbl.setToolTipText(Messages.getString(
                    "SkinEditor.PathToolTip", //$NON-NLS-1$
                    new Object[] { Configuration.widgetsDir().getPath() }));
            newPathLbl.addActionListener(this);
            pathLbl.add(newPathLbl); //$NON-NLS-1$
            JTextField newPath = new JTextField(imgPath, TEXTFIELD_COLS);
            newPath.getDocument().addDocumentListener(this);
            path.add(newPath);
            JCheckBox newTiled = new JCheckBox(
                    Messages.getString("SkinEditor.Tiled"), //$NON-NLS-1$
                    isTiled);
            newTiled.setToolTipText(Messages
                    .getString("SkinEditor.TiledToolTip")); //$NON-NLS-1$
            newTiled.addActionListener(this);
            tiled.add(newTiled);
            JButton newRemoveButton = new JButton(
                    Messages.getString("SkinEditor.RemoveButton")); //$NON-NLS-1$
            newRemoveButton.setToolTipText(Messages
                    .getString("SkinEditor.RemoveButtonToolTip")); //$NON-NLS-1$
            newRemoveButton.setMargin(new Insets(0, 0, 1, 0));
            newRemoveButton.setPreferredSize(new Dimension(14, 14));
            newRemoveButton.setMaximumSize(new Dimension(14, 14));
            newRemoveButton.setEnabled(removeEnabled);
            newRemoveButton.addActionListener(this);
            removeButtons.add(newRemoveButton);
        }

        void layoutPanel() {
            removeAll();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(1, 1, 1, 1);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = gbc.gridy = 0;
            if (displayTiled) {
                add(addButton, gbc);
                gbc.gridy++;
            }
            for (int i = 0; i < path.size(); i++) {
                gbc.gridx = 0;
                gbc.gridwidth = 1;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                add(pathLbl.get(i), gbc);

                gbc.gridx++;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                if (!displayTiled) {
                    gbc.gridwidth = 2;
                }
                add(path.get(i), gbc);

                if (displayTiled) {
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.gridwidth = 1;
                    gbc.weightx = 0;
                    gbc.gridx++;
                    add(tiled.get(i), gbc);
                    gbc.gridx++;
                    add(removeButtons.get(i), gbc);
                }
                gbc.gridy++;
            }
        }

        @Override
        public void setEnabled(boolean en) {
            super.setEnabled(en);
            addButton.setEnabled(en);
            for (int i = 0; i < path.size(); i++) {
                pathLbl.get(i).setEnabled(en);
                path.get(i).setEnabled(en);
                tiled.get(i).setEnabled(en);
                removeButtons.get(i).setEnabled((path.size() > 1) && en);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(addButton)) {
                addPathRow("", false, true);
                for (JButton removeButton : removeButtons) {
                    removeButton.setEnabled(path.size() > 1);
                }
                layoutPanel();
                skinPanel.notifySkinChanges(true);
                skinPanel.signalValidate();
            } else {
                // Did we press a remove button?
                for (int i = 0; i < removeButtons.size(); i++) {
                    // Find the button pressed, and remove the entry
                    if (e.getSource().equals(removeButtons.get(i))) {
                        // Remove Listeners
                        pathLbl.get(i).removeActionListener(this);
                        path.get(i).getDocument().removeDocumentListener(this);
                        tiled.get(i).removeActionListener(this);
                        removeButtons.get(i).removeActionListener(this);

                        // Remove UI elements
                        pathLbl.remove(i);
                        path.remove(i);
                        tiled.remove(i);
                        removeButtons.remove(i);
                        for (JButton removeButton : removeButtons) {
                            removeButton.setEnabled(path.size() > 1);
                        }
                        layoutPanel();
                        skinPanel.notifySkinChanges(true);
                        skinPanel.signalValidate();
                        // We're done
                        return;
                    }
                }
                // Did we press a pathLbl button?
                for (int i = 0; i < pathLbl.size(); i++) {
                    if (e.getSource().equals(pathLbl.get(i))) {
                        chooseFile(i);
                        skinPanel.notifySkinChanges(false);
                        skinPanel.signalValidate();
                        return;
                    }
                }
                // Did we press a tile button?
                for (int i = 0; i < tiled.size(); i++) {
                    if (e.getSource().equals(tiled.get(i))) {
                        skinPanel.notifySkinChanges(false);
                        return;
                    }
                }
            }
        }

        /**
         * Handles the pressing of a pathLbl button: display the file chooser
         * and update the path if a file is selected
         *
         * @param pathIdx
         */
        private void chooseFile(int pathIdx) {
            int returnVal = fileChooser.showOpenDialog(this);
            // Did the user choose valid input?
            if ((returnVal != JFileChooser.APPROVE_OPTION)
                    || (fileChooser.getSelectedFile() == null)) {
                return;
            }
            // Get relative path
            String relativePath = Configuration.widgetsDir().toURI()
                    .relativize(fileChooser.getSelectedFile().toURI())
                    .getPath();
            // Set text
            path.get(pathIdx).getDocument().removeDocumentListener(this);
            path.get(pathIdx).setText(relativePath);
            path.get(pathIdx).getDocument().addDocumentListener(this);
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) {
            skinPanel.notifySkinChanges(false);
        }

        @Override
        public void insertUpdate(DocumentEvent arg0) {
            skinPanel.notifySkinChanges(false);
        }

        @Override
        public void removeUpdate(DocumentEvent arg0) {
            skinPanel.notifySkinChanges(false);
        }
    }

    /**
     * Essentially the same thing as the BorderElement, but used for backgrounds
     * where each background image shares the same shouldTile state.
     * 
     * @author arlith
     *
     */
    private class BackgroundElement extends BorderElement {

        /**
         * 
         */
        private static final long serialVersionUID = 3448867645483831732L;

        BackgroundElement(SkinSpecPanel skinPanel, List<String> imgPath,
                List<Boolean> isTiled) {
            super(skinPanel, Messages.getString("SkinEditor.Background"), //$NON-NLS-1$
                    imgPath, isTiled, true);
        }

        public void actionPerformed(ActionEvent e) {
            boolean tiledChecked = false;
            boolean newValue = false;
            if (e.getSource().equals(addButton)) {
                addPathRow("", false, true);
                for (JButton removeButton : removeButtons) {
                    removeButton.setEnabled(true);
                }
                layoutPanel();
                skinPanel.notifySkinChanges(true);
                skinPanel.signalValidate();
                return;
            }
            for (JCheckBox tileChk : tiled) {
                if (e.getSource().equals(tileChk)) {
                    tiledChecked = true;
                    newValue = tileChk.isSelected();
                }
            }
            if (tiledChecked) {
                for (JCheckBox tileChk : tiled) {
                    tileChk.setSelected(newValue);
                }
                skinPanel.notifySkinChanges(false);
                return;
            }
            super.actionPerformed(e);
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = -37452332974426228L;

    BorderElement tlCorner, trCorner, blCorner, brCorner;

    BorderElement topEdge, bottomEdge, leftEdge, rightEdge;

    BackgroundElement background;

    JCheckBox showScrollBars = new JCheckBox(
            Messages.getString("SkinEditor.ShowScrollBars")); //$NON-NLS-1$

    ArrayList<JButton> colorButtons = new ArrayList<>();

    JButton addColor = new JButton(
            Messages.getString("SkinEditor.AddColor.Text")); //$NON-NLS-1$

    JButton removeColor = new JButton(
            Messages.getString("SkinEditor.RemoveColor.Text")); //$NON-NLS-1$

    JLabel colorLbl = new JLabel(Messages.getString("SkinEditor.Color")); //$NON-NLS-1$

    SkinSpecEditor skinEditor;

    /**
     * 
     */
    public SkinSpecPanel(SkinSpecEditor skinEditor) {
        super(new GridBagLayout());
        this.skinEditor = skinEditor;
        addColor.setToolTipText(Messages
                .getString("SkinEditor.AddColor.ToolTip"));
        removeColor.setToolTipText(Messages
                .getString("SkinEditor.RemoveColor.ToolTip"));
    }

    /**
     * Add this SkinSpecEditor as a listener to all components.
     */
    private void addListeners() {
        for (JButton colorButton : colorButtons) {
            colorButton.addActionListener(this);
        }
        addColor.addActionListener(this);
        removeColor.addActionListener(this);
    }

    /**
     * Remove thsi SkinSpecEditor as a listener from all components.
     */
    private void removeListeners() {
        for (JButton colorButton : colorButtons) {
            colorButton.removeActionListener(this);
        }
        addColor.removeActionListener(this);
        removeColor.removeActionListener(this);
    }

    private void resetColorButtons(SkinSpecification skinSpec) {
        // Listeners must already be removed before calling this!
        colorButtons.clear();
        for (Color c : skinSpec.fontColors) {
            JButton colorButton = new JButton();
            colorButton.setMaximumSize(new Dimension(14, 14));
            colorButton.setPreferredSize(new Dimension(14, 14));
            colorButton.setForeground(c);
            colorButton.setBackground(c);
            colorButtons.add(colorButton);
        }
        // Don't add listeners, to prevent double add
    }

    /**
     * Update the given SkinSpecification based on the state of the UI elements.
     *
     * @param skinSpec
     * @return
     */
    public void updateSkinSpec(SkinSpecification skinSpec, boolean enableBorders) {

        skinSpec.noBorder = !enableBorders;
        skinSpec.tl_corner = tlCorner.path.get(0).getText();
        skinSpec.tr_corner = trCorner.path.get(0).getText();
        skinSpec.bl_corner = blCorner.path.get(0).getText();
        skinSpec.br_corner = brCorner.path.get(0).getText();

        // Top Edge
        skinSpec.topEdge.clear();
        skinSpec.topShouldTile.clear();
        for (int i = 0; i < topEdge.path.size(); i++) {
            skinSpec.topEdge.add(topEdge.path.get(i).getText());
            skinSpec.topShouldTile.add(topEdge.tiled.get(i).isSelected());
        }
        // Bottom Edge
        skinSpec.bottomEdge.clear();
        skinSpec.bottomShouldTile.clear();
        for (int i = 0; i < bottomEdge.path.size(); i++) {
            skinSpec.bottomEdge.add(bottomEdge.path.get(i).getText());
            skinSpec.bottomShouldTile.add(bottomEdge.tiled.get(i).isSelected());
        }
        // Left Edge
        skinSpec.leftEdge.clear();
        skinSpec.leftShouldTile.clear();
        for (int i = 0; i < leftEdge.path.size(); i++) {
            skinSpec.leftEdge.add(leftEdge.path.get(i).getText());
            skinSpec.leftShouldTile.add(leftEdge.tiled.get(i).isSelected());
        }
        // Right Edge
        skinSpec.rightEdge.clear();
        skinSpec.rightShouldTile.clear();
        for (int i = 0; i < rightEdge.path.size(); i++) {
            skinSpec.rightEdge.add(rightEdge.path.get(i).getText());
            skinSpec.rightShouldTile.add(rightEdge.tiled.get(i).isSelected());
        }

        // Background
        skinSpec.backgrounds.clear();
        for (int i = 0; i < background.path.size(); i++) {
            skinSpec.backgrounds.add(background.path.get(i).getText());
        }
        skinSpec.tileBackground = false;
        if (background.tiled.size() > 0) {
            skinSpec.tileBackground = background.tiled.get(0).isSelected();
        }

        // Font Color
        skinSpec.fontColors.clear();
        for (JButton colorButton : colorButtons) {
            skinSpec.fontColors.add(colorButton.getBackground());
        }

        // Show Scroll Bars
        skinSpec.showScrollBars = showScrollBars.isSelected();

    }

    /**
     * Update the editing panel with the currently selected SkinSpecification.
     */
    public void setupSkinEditPanel(SkinSpecification skinSpec) {
        removeListeners();
        removeAll();

        boolean enableBorders = !skinSpec.noBorder;

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 4, 0);

        JPanel borderPanel = new JPanel(new GridBagLayout());
        // borderPanel.setLayout(new BoxLayout(borderPanel, BoxLayout.Y_AXIS));
        borderPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                Messages.getString("SkinEditor.Borders"), TitledBorder.TOP, //$NON-NLS-1$
                TitledBorder.DEFAULT_POSITION));
        borderPanel.setEnabled(enableBorders);

        // Top Left Corner
        tlCorner = new BorderElement(this,
                Messages.getString("SkinEditor.TLC"), //$NON-NLS-1$
                skinSpec.tl_corner);
        tlCorner.setEnabled(enableBorders);
        borderPanel.add(tlCorner, gbc);
        gbc.gridx++;
        // Top Right Corner
        trCorner = new BorderElement(this,
                Messages.getString("SkinEditor.TRC"), //$NON-NLS-1$
                skinSpec.tr_corner);
        trCorner.setEnabled(enableBorders);
        borderPanel.add(trCorner, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        // Bottom Left Corner
        blCorner = new BorderElement(this,
                Messages.getString("SkinEditor.BLC"), //$NON-NLS-1$
                skinSpec.bl_corner);
        blCorner.setEnabled(enableBorders);
        borderPanel.add(blCorner, gbc);
        gbc.gridx++;
        // Bottom Right Corner
        brCorner = new BorderElement(this,
                Messages.getString("SkinEditor.BRC"), //$NON-NLS-1$
                skinSpec.br_corner);
        brCorner.setEnabled(enableBorders);
        borderPanel.add(brCorner, gbc);
        gbc.gridy++;
        gbc.gridx = 0;

        // Top Edge
        topEdge = new BorderElement(this,
                Messages.getString("SkinEditor.TopEdge"), //$NON-NLS-1$
                skinSpec.topEdge, skinSpec.topShouldTile);
        topEdge.setEnabled(enableBorders);
        borderPanel.add(topEdge, gbc);
        gbc.gridx++;

        // Bottom Edge
        bottomEdge = new BorderElement(this,
                Messages.getString("SkinEditor.BottomEdge"), //$NON-NLS-1$
                skinSpec.bottomEdge, skinSpec.bottomShouldTile);
        bottomEdge.setEnabled(enableBorders);
        borderPanel.add(bottomEdge, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Left Edge
        leftEdge = new BorderElement(this,
                Messages.getString("SkinEditor.LeftEdge"), //$NON-NLS-1$
                skinSpec.leftEdge, skinSpec.leftShouldTile);
        leftEdge.setEnabled(enableBorders);
        borderPanel.add(leftEdge, gbc);
        gbc.gridx++;

        // Right Edge
        rightEdge = new BorderElement(this,
                Messages.getString("SkinEditor.RightEdge"), //$NON-NLS-1$
                skinSpec.rightEdge, skinSpec.rightShouldTile);
        rightEdge.setEnabled(enableBorders);
        borderPanel.add(rightEdge, gbc);
        gbc.gridy++;
        gbc.gridx = 0;

        gbc.gridx = gbc.gridy = 0;
        add(borderPanel, gbc);

        background = new BackgroundElement(this, skinSpec.backgrounds,
                Collections.nCopies(skinSpec.backgrounds.size(),
                        skinSpec.tileBackground));

        gbc.gridy++;

        add(background, gbc);

        JPanel misc = new JPanel(new GridBagLayout());
        gbc.gridy++;
        add(misc, gbc);

        gbc.gridx = gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        misc.add(showScrollBars);

        resetColorButtons(skinSpec);
        JPanel glue = new JPanel();
        glue.add(colorLbl);
        for (JButton colorButton : colorButtons) {
            glue.add(colorButton);
        }
        glue.add(addColor);
        glue.add(removeColor);
        gbc.gridy++;
        misc.add(glue, gbc);

        revalidate();
        addListeners();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        removeListeners();
        boolean notify = false;
        if (e.getSource() instanceof JButton) {
            if (addColor.equals(e.getSource())) {
                if (colorButtons.size() < SkinSpecification.MAX_NUM_COLORS) {
                    JButton colorButton = new JButton();
                    colorButton.setMaximumSize(new Dimension(14, 14));
                    colorButton.setPreferredSize(new Dimension(14, 14));
                    colorButton.setForeground(Color.BLACK);
                    colorButton.setBackground(Color.BLACK);
                    colorButtons.add(colorButton);
                    notify = true;
                }
            } else if (removeColor.equals(e.getSource())) {
                if (colorButtons.size() > 1) {
                    colorButtons.remove(colorButtons.size() - 1);
                    notify = true;
                }
            } else if (colorButtons.contains(e.getSource())) {
                JButton colorButton = (JButton) e.getSource();
                Color newColor = JColorChooser.showDialog(this,
                        Messages.getString("SkinEditor.ColorChoice"), //$NON-NLS-1$
                        colorButton.getBackground());
                if (newColor != null) {
                    colorButton.setBackground(newColor);
                    notify = true;
                }
            }
        }
        if (notify) {
            notifySkinChanges(true);
        } else { // If we notify, listeners are added, don't double-add
            addListeners();
        }
    }

    /**
     * Override the base setEnabled to also set the state of all members.
     */
    @Override
    public void setEnabled(boolean enabled) {
        // Corners
        tlCorner.setEnabled(enabled);
        trCorner.setEnabled(enabled);
        blCorner.setEnabled(enabled);
        brCorner.setEnabled(enabled);

        // Edges
        topEdge.setEnabled(enabled);
        bottomEdge.setEnabled(enabled);
        leftEdge.setEnabled(enabled);
        rightEdge.setEnabled(enabled);
    }

    public void signalValidate() {
        skinEditor.validate();
    }

    public void notifySkinChanges(boolean setupSkinEditPanel) {
        skinEditor.notifySkinChanges(setupSkinEditPanel);
    }

}
