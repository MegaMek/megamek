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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.UnitDisplaySkinSpecification;
import megamek.common.Configuration;

/**
 * Panel with elements for viewing and adjusting a specific 
 * UnitDisplaySkinSpecification.
 * 
 * @author arlith
 */
public class UnitDisplaySpecPanel extends JPanel {

    /**
     * A UI widget for displaying path information
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

        JButton pathLbl;

       JTextField path;

        JFileChooser fileChooser = new JFileChooser(Configuration.widgetsDir());

        UnitDisplaySpecPanel udPanel;

        /**
         * Constructor for BorderElements that only have one image (like
         * corners). The option to tile the image is not present, nor are the
         * add and remove buttons.
         *
         * @param elementName
         * @param imgPath
         */
        BorderElement(UnitDisplaySpecPanel udPanel, String elementName,
                String imgPath) {
            super(new GridBagLayout());
            this.udPanel = udPanel;
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(), elementName,
                    TitledBorder.LEFT, TitledBorder.TOP));

            pathLbl = new JButton(
                    Messages.getString("SkinEditor.Path")); //$NON-NLS-1$
            pathLbl.setMargin(new Insets(1, 1, 1, 1));
            pathLbl.setToolTipText(Messages.getString(
                    "SkinEditor.PathToolTip", //$NON-NLS-1$
                    new Object[] { Configuration.widgetsDir().getPath() }));
            pathLbl.addActionListener(this);
            path = new JTextField(imgPath, TEXTFIELD_COLS);
            path.getDocument().addDocumentListener(this);

            layoutPanel();
        }

        void layoutPanel() {
            removeAll();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(1, 1, 1, 1);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = gbc.gridy = 0;

            gbc.gridx = 0;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            add(pathLbl, gbc);

            gbc.gridx++;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(path, gbc);
            gbc.gridy++;
            
        }
        
        public String getPath() {
            return path.getText();
        }

        @Override
        public void setEnabled(boolean en) {
            super.setEnabled(en);
            pathLbl.setEnabled(en);
            path.setEnabled(en);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Did we press a pathLbl button?
            if (e.getSource().equals(pathLbl)) {
                chooseFile();
                udPanel.notifySkinChanges();
                return;
            }
        }

        /**
         * Handles the pressing of a pathLbl button: display the file chooser
         * and update the path if a file is selected
         *
         * @param pathIdx
         */
        private void chooseFile() {
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
            path.setText(relativePath);
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) {
            udPanel.notifySkinChanges();
        }

        @Override
        public void insertUpdate(DocumentEvent arg0) {
            udPanel.notifySkinChanges();
        }

        @Override
        public void removeUpdate(DocumentEvent arg0) {
            udPanel.notifySkinChanges();
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = -37452332974426228L;
    
    private BorderElement generalTabIdle;
    private BorderElement pilotTabIdle;
    private BorderElement armorTabIdle;
    private BorderElement systemsTabIdle;
    private BorderElement weaponsTabIdle;
    private BorderElement extrasTabIdle;
    private BorderElement generalTabActive;
    private BorderElement pilotTabActive;
    private BorderElement armorTabActive;
    private BorderElement systemsTabActive;
    private BorderElement weaponsTabActive;
    private BorderElement extraTabActive;
    private BorderElement cornerIdle;
    private BorderElement cornerActive;

    private BorderElement backgroundTile;
    private BorderElement topLine;
    private BorderElement bottomLine;
    private BorderElement leftLine;
    private BorderElement rightLine;
    private BorderElement topLeftCorner;
    private BorderElement bottomLeftCorner;
    private BorderElement topRightCorner;
    private BorderElement bottomRightCorner;

    private BorderElement mechOutline;
    
    SkinSpecEditor skinEditor;

    /**
     *
     */
    public UnitDisplaySpecPanel(SkinSpecEditor skinEditor) {
        super(new GridBagLayout());
        this.skinEditor = skinEditor;
    }

    /**
     * Update the given UnitDisplaySkinSpecification based on the state of the
     * UI elements.
     *
     * @param skinSpec
     * @return
     */
    public void updateSkinSpec(UnitDisplaySkinSpecification udSpec) {

        udSpec.setGeneralTabIdle(generalTabIdle.getPath());
        udSpec.setPilotTabIdle(pilotTabIdle.getPath());
        udSpec.setArmorTabIdle(armorTabIdle.getPath());
        udSpec.setSystemsTabIdle(systemsTabIdle.getPath());
        udSpec.setWeaponsTabIdle(weaponsTabIdle.getPath());
        udSpec.setExtrasTabIdle(extrasTabIdle.getPath());
        udSpec.setGeneralTabActive(generalTabActive.getPath());
        udSpec.setPilotTabActive(pilotTabActive.getPath());
        udSpec.setArmorTabActive(armorTabActive.getPath());
        udSpec.setSystemsTabActive(systemsTabActive.getPath());
        udSpec.setWeaponsTabActive(weaponsTabActive.getPath());
        udSpec.setExtraTabActive(extraTabActive.getPath());
        udSpec.setCornerIdle(cornerIdle.getPath());
        udSpec.setCornerActive(cornerActive.getPath());

        udSpec.setBackgroundTile(backgroundTile.getPath());
        udSpec.setTopLine(topLine.getPath());
        udSpec.setBottomLine(bottomLine.getPath());
        udSpec.setLeftLine(leftLine.getPath());
        udSpec.setRightLine(rightLine.getPath());
        udSpec.setTopLeftCorner(topLeftCorner.getPath());
        udSpec.setBottomLeftCorner(bottomLeftCorner.getPath());
        udSpec.setTopRightCorner(topRightCorner.getPath());
        udSpec.setBottomRightCorner(bottomRightCorner.getPath());
        
        udSpec.setMechOutline(mechOutline.getPath());
    }

    /**
     * Update the editing panel with the currently selected SkinSpecification.
     */
    public void setupSkinEditPanel(UnitDisplaySkinSpecification udSpec) {
        removeAll();

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 4, 0);

        JPanel tabsPanel = new JPanel(new GridBagLayout());
        // borderPanel.setLayout(new BoxLayout(borderPanel, BoxLayout.Y_AXIS));
        tabsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                Messages.getString("SkinEditor.TabImages"), TitledBorder.TOP, //$NON-NLS-1$
                TitledBorder.DEFAULT_POSITION));

        // General Tab
        generalTabIdle = new BorderElement(this,
                Messages.getString("SkinEditor.generalTabIdle"), //$NON-NLS-1$
                udSpec.getGeneralTabIdle());
        tabsPanel.add(generalTabIdle, gbc);
        gbc.gridx++;
        generalTabActive = new BorderElement(this,
                Messages.getString("SkinEditor.generalTabActive"), //$NON-NLS-1$
                udSpec.getGeneralTabActive());
        tabsPanel.add(generalTabActive, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Pilot Tab
        pilotTabIdle = new BorderElement(this,
                Messages.getString("SkinEditor.pilotTabIdle"), //$NON-NLS-1$
                udSpec.getPilotTabIdle());
        tabsPanel.add(pilotTabIdle, gbc);
        gbc.gridx++;
        pilotTabActive = new BorderElement(this,
                Messages.getString("SkinEditor.pilotTabActive"), //$NON-NLS-1$
                udSpec.getPilotTabActive());
        tabsPanel.add(pilotTabActive, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        
        // Pilot Tab
        armorTabIdle = new BorderElement(this,
                Messages.getString("SkinEditor.armorTabIdle"), //$NON-NLS-1$
                udSpec.getArmorTabIdle());
        tabsPanel.add(armorTabIdle, gbc);
        gbc.gridx++;
        armorTabActive = new BorderElement(this,
                Messages.getString("SkinEditor.armorTabActive"), //$NON-NLS-1$
                udSpec.getArmorTabActive());
        tabsPanel.add(armorTabActive, gbc);
        gbc.gridx = 0;
        gbc.gridy++;        

        // Systems Tab
        systemsTabIdle = new BorderElement(this,
                Messages.getString("SkinEditor.systemsTabIdle"), //$NON-NLS-1$
                udSpec.getSystemsTabIdle());
        tabsPanel.add(systemsTabIdle, gbc);
        gbc.gridx++;
        systemsTabActive = new BorderElement(this,
                Messages.getString("SkinEditor.systemsTabActive"), //$NON-NLS-1$
                udSpec.getSystemsTabActive());
        tabsPanel.add(systemsTabActive, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Weapons Tab
        weaponsTabIdle = new BorderElement(this,
                Messages.getString("SkinEditor.weaponsTabIdle"), //$NON-NLS-1$
                udSpec.getWeaponsTabIdle());
        tabsPanel.add(weaponsTabIdle, gbc);
        gbc.gridx++;
        weaponsTabActive = new BorderElement(this,
                Messages.getString("SkinEditor.weaponsTabActive"), //$NON-NLS-1$
                udSpec.getWeaponsTabActive());
        tabsPanel.add(weaponsTabActive, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Extras Tab
        extrasTabIdle = new BorderElement(this,
                Messages.getString("SkinEditor.extrasTabIdle"), //$NON-NLS-1$
                udSpec.getExtrasTabIdle());
        tabsPanel.add(extrasTabIdle, gbc);
        gbc.gridx++;
        extraTabActive = new BorderElement(this,
                Messages.getString("SkinEditor.extraTabActive"), //$NON-NLS-1$
                udSpec.getExtraTabActive());
        tabsPanel.add(extraTabActive, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // General Tab
        cornerIdle = new BorderElement(this,
                Messages.getString("SkinEditor.cornerIdle"), //$NON-NLS-1$
                udSpec.getCornerIdle());
        tabsPanel.add(cornerIdle, gbc);
        gbc.gridx++;
        cornerActive = new BorderElement(this,
                Messages.getString("SkinEditor.cornerActive"), //$NON-NLS-1$
                udSpec.getCornerActive());
        tabsPanel.add(cornerActive, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Border
        JPanel borderPanel = new JPanel(new GridBagLayout());
        borderPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                Messages.getString("SkinEditor.Borders"), TitledBorder.TOP, //$NON-NLS-1$
                TitledBorder.DEFAULT_POSITION));

        gbc.gridx = gbc.gridy = 0;

        // Top Corners Tab
        topLeftCorner = new BorderElement(this,
                Messages.getString("SkinEditor.topLeftCorner"), //$NON-NLS-1$
                udSpec.getTopLeftCorner());
        borderPanel.add(topLeftCorner, gbc);
        gbc.gridx++;
        topRightCorner = new BorderElement(this,
                Messages.getString("SkinEditor.topRightCorner"), //$NON-NLS-1$
                udSpec.getTopRightCorner());
        borderPanel.add(topRightCorner, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Bottom Corners Tab
        bottomLeftCorner = new BorderElement(this,
                Messages.getString("SkinEditor.bottomLeftCorner"), //$NON-NLS-1$
                udSpec.getBottomLeftCorner());
        borderPanel.add(bottomLeftCorner, gbc);
        gbc.gridx++;
        bottomRightCorner = new BorderElement(this,
                Messages.getString("SkinEditor.bottomRightCorner"), //$NON-NLS-1$
                udSpec.getBottomRightCorner());
        borderPanel.add(bottomRightCorner, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Top/Bottom Lines
        topLine = new BorderElement(this,
                Messages.getString("SkinEditor.topLine"), //$NON-NLS-1$
                udSpec.getTopLine());
        borderPanel.add(topLine, gbc);
        gbc.gridx++;
        bottomLine = new BorderElement(this,
                Messages.getString("SkinEditor.bottomLine"), //$NON-NLS-1$
                udSpec.getBottomLine());
        borderPanel.add(bottomLine, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        
        // Left/Right Lines
        leftLine = new BorderElement(this,
                Messages.getString("SkinEditor.leftLine"), //$NON-NLS-1$
                udSpec.getLeftLine());
        borderPanel.add(leftLine, gbc);
        gbc.gridx++;
        rightLine = new BorderElement(this,
                Messages.getString("SkinEditor.rightLine"), //$NON-NLS-1$
                udSpec.getRightLine());
        borderPanel.add(rightLine, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Background Image
        gbc.gridwidth = 2;
        backgroundTile = new BorderElement(this,
                Messages.getString("SkinEditor.backgroundTile"), //$NON-NLS-1$
                udSpec.getBackgroundTile());
        borderPanel.add(backgroundTile, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++;
        
        // Mech Outline
        gbc.gridwidth = 2;
        mechOutline = new BorderElement(this,
                Messages.getString("SkinEditor.mechOutline"), //$NON-NLS-1$
                udSpec.getMechOutline());
        borderPanel.add(mechOutline, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++;

        gbc.gridx = gbc.gridy = 0;
        add(tabsPanel, gbc);
        gbc.gridy++;
        add(borderPanel, gbc);

        revalidate();
    }

    /**
     * Override the base setEnabled to also set the state of all members.
     */
    @Override
    public void setEnabled(boolean enabled) {
        generalTabIdle.setEnabled(enabled);
        pilotTabIdle.setEnabled(enabled);
        armorTabIdle.setEnabled(enabled);
        systemsTabIdle.setEnabled(enabled);
        weaponsTabIdle.setEnabled(enabled);
        extrasTabIdle.setEnabled(enabled);
        generalTabActive.setEnabled(enabled);
        pilotTabActive.setEnabled(enabled);
        armorTabActive.setEnabled(enabled);
        systemsTabActive.setEnabled(enabled);
        weaponsTabActive.setEnabled(enabled);
        extraTabActive.setEnabled(enabled);
        cornerIdle.setEnabled(enabled);
        cornerActive.setEnabled(enabled);

        backgroundTile.setEnabled(enabled);
        topLine.setEnabled(enabled);
        bottomLine.setEnabled(enabled);
        leftLine.setEnabled(enabled);
        rightLine.setEnabled(enabled);
        topLeftCorner.setEnabled(enabled);
        bottomLeftCorner.setEnabled(enabled);
        topRightCorner.setEnabled(enabled);
        bottomRightCorner.setEnabled(enabled);
        
        mechOutline.setEnabled(enabled);
    }

    public void signalValidate() {
        skinEditor.validate();
    }

    public void notifySkinChanges() {
        skinEditor.notifySkinChanges(false);
    }

}
