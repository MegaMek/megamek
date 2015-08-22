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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.Configuration;

/**
 * Panel with elements for viewing and adjusting different SkinSpecification
 * instances.
 * 
 * @author arlith
 */
public class SkinSpecEditor extends JPanel implements ListSelectionListener,
        ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -37452332974426228L;
    
    private SkinEditorMainGUI mainGUI;
    
    private JComboBox<String> currSkinCombo = new JComboBox<>();
    
    /**
     * Adds a new SkinSpecification
     */
    private JButton addButton =
            new JButton(Messages.getString("SkinEditor.AddButton")); //$NON-NLS-1$

    private JButton addCompButton =
            new JButton(Messages.getString("SkinEditor.AddCompButton")); //$NON-NLS-1$

    private JButton removeCompButton =
            new JButton(Messages.getString("SkinEditor.RemoveCompButton")); //$NON-NLS-1$

    private JButton saveSkinButton =
            new JButton(Messages.getString("SkinEditor.SaveSkinButton")); //$NON-NLS-1$

    private JButton resetSkinButton =
            new JButton(Messages.getString("SkinEditorResestSkinButton")); //$NON-NLS-1$

    /**
     * Lists all SkinSpecifications for the current skin.
     */
    private DefaultListModel<String> skinSpecCompModel = 
            new DefaultListModel<>();
    private JList<String> skinSpecCompList = new JList<>(skinSpecCompModel);
    
    private JCheckBox enableBorders = new JCheckBox(
            Messages.getString("SkinEditor.EnableBorders"));
    
    /**
     * Panel that holds UI widgets for editing the selected skin spec.
     */
    private SkinSpecPanel skinEditPanel = new SkinSpecPanel(this);

    /**************************************************************************/
    
    /**
     * 
     */
    public SkinSpecEditor(SkinEditorMainGUI mainGUI) {
        super(new GridBagLayout());
        this.mainGUI = mainGUI;
        
        skinSpecCompList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        skinSpecCompList.setMinimumSize(new Dimension(100, 50));
        skinSpecCompList.setMinimumSize(new Dimension(100, 50));
        
        addCompButton.setToolTipText(Messages
                .getString("SkinEditor.AddCompButtonToolTip")); //$NON-NLS-1$

        JScrollPane compListScroll = new JScrollPane(skinSpecCompList);
        JScrollPane editPanelScroll = new JScrollPane(skinEditPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JPanel tmpHolding;

        enableBorders.setToolTipText(Messages
                .getString("SkinEditor.EnableBordersToolTip")); //$NON-NLS-1$
        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridx = 0; c.gridy = 0;
        
        tmpHolding = new JPanel();
        tmpHolding.add(currSkinCombo);
        tmpHolding.add(addButton);

        add(tmpHolding, c);

        tmpHolding = new JPanel();
        tmpHolding.add(addCompButton);
        tmpHolding.add(removeCompButton);
        tmpHolding.add(Box.createHorizontalStrut(10));
        tmpHolding.add(saveSkinButton);
        tmpHolding.add(resetSkinButton);

        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        add(tmpHolding, c);

        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(compListScroll, c);

        c.gridy++;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        add(enableBorders, c);

        c.gridy++;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1,0,1,0);
        add(editPanelScroll, c);
        
        updateSkinCombo();
        populateSkinSpecComponents();
        setupSkinEditPanel();
        validate();
    }
    
    /**
     * Add this SkinSpecEditor as a listener to all components.
     */
    private void addListeners() {
        skinSpecCompList.addListSelectionListener(this);
        
        enableBorders.addActionListener(this);
        currSkinCombo.addActionListener(this);
        addButton.addActionListener(this);
        addCompButton.addActionListener(this);
        removeCompButton.addActionListener(this);
        saveSkinButton.addActionListener(this);
        resetSkinButton.addActionListener(this);
    }
    
    /**
     * Remove thsi SkinSpecEditor as a listener from all components.
     */
    private void removeListeners() {
        skinSpecCompList.removeListSelectionListener(this);
        
        enableBorders.removeActionListener(this);
        currSkinCombo.removeActionListener(this);
        addButton.removeActionListener(this);
        addCompButton.removeActionListener(this);
        removeCompButton.removeActionListener(this);
        saveSkinButton.removeActionListener(this);
        resetSkinButton.removeActionListener(this);
    }
    
    /**
     * 
     */
    private void updateSkinCombo() {
        removeListeners();
        
        currSkinCombo.removeAllItems();
        String[] xmlFiles = 
            Configuration.configDir().list(new FilenameFilter() {
                public boolean accept(File directory, String fileName) {
                    return fileName.endsWith(".xml");
                } 
            });
        for (String file : xmlFiles) {
            if (SkinXMLHandler.validSkinSpecFile(file)) {
                currSkinCombo.addItem(file);
            }
        }
        // Select the default file first
        currSkinCombo.setSelectedItem(SkinXMLHandler.defaultSkinXML);
        // If this select fails, the default skin will be selected
        currSkinCombo.setSelectedItem(GUIPreferences.getInstance()
                .getSkinFile());
        
        addListeners();
    }
    
    /**
     * Updates the List model to display all of the current components with
     * SkinSpecifications.
     */
    private void populateSkinSpecComponents() {
        removeListeners();
        skinSpecCompModel.removeAllElements();

        for (String comp : SkinXMLHandler.getSkinnedComponents()) {
            skinSpecCompModel.addElement(comp);
        }
        skinSpecCompList.setSelectedIndex(0);
        addListeners();
    }
    
    /**
     * Update the editing panel with the currently selected SkinSpecification.
     */
    private void setupSkinEditPanel() {
        removeListeners();

        // Nothing to do if we selected nothing...
        if (skinSpecCompList.getSelectedIndex() == -1) {
            skinEditPanel.removeAll();
            return;            
        }
        
        saveSkinButton.setEnabled(false);

        SkinSpecification skinSpec = SkinXMLHandler.getSkin(skinSpecCompList
                .getSelectedValue());
        
        enableBorders.setSelected(!skinSpec.noBorder);
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0,0,2,0);

        skinEditPanel.setupSkinEditPanel(skinSpec);
        
        revalidate();
        addListeners();
    }


    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        
        if (e.getSource().equals(skinSpecCompList)) {
            setupSkinEditPanel();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(currSkinCombo)) {
            GUIPreferences gs = GUIPreferences.getInstance();
            String newSkinFile = (String) currSkinCombo.getSelectedItem();
            String oldSkinFile = gs.getSkinFile();
            if (!oldSkinFile.equals(newSkinFile)) {
                boolean success = SkinXMLHandler.initSkinXMLHandler(newSkinFile);
                if (!success) {
                    SkinXMLHandler.initSkinXMLHandler(oldSkinFile);
                    String title = Messages
                            .getString("CommonSettingsDialog.skinFileFail.title"); //$NON-NLS-1$
                    String msg = Messages
                            .getString("CommonSettingsDialog.skinFileFail.msg"); //$NON-NLS-1$
                    JOptionPane.showMessageDialog(this, msg, title,
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    gs.setSkinFile(newSkinFile);
                }
                mainGUI.updateBorder();
                populateSkinSpecComponents();
                setupSkinEditPanel();                
            }
        } else if (e.getSource().equals(enableBorders)) {
            skinEditPanel.setEnabled(enableBorders.isSelected());
        } else if (e.getSource().equals(resetSkinButton)) {
            setupSkinEditPanel();
        } else if (e.getSource().equals(saveSkinButton)) {
            saveSkinButton.setEnabled(false);
            String currComp = (String) skinSpecCompList.getSelectedValue();
            SkinSpecification skinSpec = SkinXMLHandler.getSkin(currComp);
            skinEditPanel.updateSkinSpec(skinSpec, enableBorders.isSelected());
            SkinXMLHandler.writeSkinToFile((String) currSkinCombo
                    .getSelectedItem());
            mainGUI.updateBorder();
        } else if (e.getSource().equals(addCompButton)) {
            String msg = Messages.getString("SkinEditor.AddCompMsg");
            String title = Messages.getString("SkinEditor.AddCompTitle");
            String newComp = JOptionPane.showInputDialog(this, msg, title,
                    JOptionPane.QUESTION_MESSAGE);
            if (newComp == null) {
                return;
            }
            SkinXMLHandler.addNewComp(newComp);
        }
    }

    /**
     * Notifies the SkinSpecEditor that a change has been made to the currently
     * selected component's SkinSpecification.
     */
    public void notifySkinChanges() {
        saveSkinButton.setEnabled(true);
    }

}
