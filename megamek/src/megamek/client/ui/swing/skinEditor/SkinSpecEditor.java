/*
 * MegaMek - Copyright (C) 2000-2004, 2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2015 Nicholas Walczak (walczak@cs.umn.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.skinEditor;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.CommonSettingsDialog;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinSpecification.UIComponents;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.Configuration;
import megamek.common.preference.PreferenceManager;

import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Panel with elements for viewing and adjusting different SkinSpecification instances.
 * 
 * @author arlith
 */
public class SkinSpecEditor extends JPanel implements ListSelectionListener, ActionListener {
    private static final long serialVersionUID = -37452332974426228L;
    
    private SkinEditorMainGUI mainGUI;
    
    private JComboBox<String> currSkinCombo = new JComboBox<>();
    
    private JButton addCompButton = new JButton(Messages.getString("SkinEditor.AddCompButton"));

    private JButton removeCompButton = new JButton(Messages.getString("SkinEditor.RemoveCompButton"));

    private JButton saveSkinButton = new JButton(Messages.getString("SkinEditor.SaveSkinButton"));

    private JButton resetSkinButton = new JButton(Messages.getString("SkinEditor.ResetSkinButton"));

    /**
     * Lists all SkinSpecifications for the current skin.
     */
    private DefaultListModel<UIComponents> skinSpecCompModel = new DefaultListModel<>();
    private JList<UIComponents> skinSpecCompList = new JList<>(skinSpecCompModel);

    private JCheckBox enablePlain = new JCheckBox(Messages.getString("SkinEditor.EnablePlain"));
    private JCheckBox enableBorders = new JCheckBox(Messages.getString("SkinEditor.EnableBorders"));
    
    private JPanel editPanel = new JPanel();
    /**
     * Panel that holds UI widgets for editing the selected skin spec.
     */
    private SkinSpecPanel skinEditPanel = new SkinSpecPanel(this);

    private UnitDisplaySpecPanel udEditPanel = new UnitDisplaySpecPanel(this);

    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public SkinSpecEditor(SkinEditorMainGUI mainGUI) {
        super(new GridBagLayout());
        this.mainGUI = mainGUI;
        
        skinSpecCompList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        skinSpecCompList.setMinimumSize(new Dimension(100, 50));
        skinSpecCompList.setMinimumSize(new Dimension(100, 50));

        skinSpecCompList.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent evt) {

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int index = skinSpecCompList.locationToIndex(e.getPoint());
                if (index > -1) {
                    skinSpecCompList.setToolTipText(skinSpecCompModel.getElementAt(index).getDescription());
                }
            }
        });

        addCompButton.setToolTipText(Messages.getString("SkinEditor.AddCompButtonToolTip"));
        removeCompButton.setToolTipText(Messages.getString("SkinEditor.RemoveCompButtonToolTip"));
        saveSkinButton.setToolTipText(Messages.getString("SkinEditor.SaveSkinButtonToolTip"));
        resetSkinButton.setToolTipText(Messages.getString("SkinEditor.ResetSkinButtonToolTip"));

        JScrollPane compListScroll = new JScrollPane(skinSpecCompList);
        JScrollPane editPanelScroll = new JScrollPane(editPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel tmpHolding;

        enablePlain.setToolTipText(Messages.getString("SkinEditor.EnablePlainToolTip"));
        enableBorders.setToolTipText(Messages.getString("SkinEditor.EnableBordersToolTip"));
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
        c.weightx = 0.5;
        c.weighty = 0.25;
        c.fill = GridBagConstraints.BOTH;
        add(compListScroll, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        add(enablePlain, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        add(enableBorders, c);

        c.gridy++;
        c.weightx = 1.0;
        c.weighty = 0.75;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 0, 1, 0);
        add(editPanelScroll, c);
        
        updateSkinCombo(Configuration.skinsDir().getPath() + SkinXMLHandler.defaultSkinXML);
        populateSkinSpecComponents();
        setupEditPanel();
        validate();
    }
    
    /**
     * Add this SkinSpecEditor as a listener to all components.
     */
    private void addListeners() {
        skinSpecCompList.addListSelectionListener(this);

        enablePlain.addActionListener(this);
        enableBorders.addActionListener(this);
        currSkinCombo.addActionListener(this);
        addCompButton.addActionListener(this);
        removeCompButton.addActionListener(this);
        saveSkinButton.addActionListener(this);
        resetSkinButton.addActionListener(this);
    }
    
    /**
     * Remove this SkinSpecEditor as a listener from all components.
     */
    private void removeListeners() {
        skinSpecCompList.removeListSelectionListener(this);

        enablePlain.removeActionListener(this);
        enableBorders.removeActionListener(this);
        currSkinCombo.removeActionListener(this);
        addCompButton.removeActionListener(this);
        removeCompButton.removeActionListener(this);
        saveSkinButton.removeActionListener(this);
        resetSkinButton.removeActionListener(this);
    }

    private void updateSkinCombo(String selected) {
        removeListeners();
        
        currSkinCombo.removeAllItems();
        List<String> xmlFiles = new ArrayList<>(CommonSettingsDialog.filteredFiles(Configuration.skinsDir(), ".xml"));

        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if (!userDir.isBlank()) {
            File subDir = new File(userDir, Configuration.skinsDir().toString());
            xmlFiles.addAll(CommonSettingsDialog.filteredFilesWithSubDirs(subDir, ".xml"));
        }

        xmlFiles.removeIf(file -> !SkinXMLHandler.validSkinSpecFile(file));
        Collections.sort(xmlFiles);
        var model = new DefaultComboBoxModel<>(xmlFiles.toArray(new String[0]));
        currSkinCombo.setModel(model);

        addListeners();

        // Select the default file first
        currSkinCombo.setSelectedItem(selected);
    }
    
    /**
     * Updates the List model to display all of the current components with SkinSpecifications.
     */
    private void populateSkinSpecComponents() {
        removeListeners();
        skinSpecCompModel.removeAllElements();

        for (String comp : SkinXMLHandler.getSkinnedComponents()) {
            skinSpecCompModel.addElement(UIComponents.getUIComponent(comp));
        }
        skinSpecCompModel.addElement(UIComponents.UnitDisplay);
        skinSpecCompList.setSelectedIndex(0);
        addListeners();
    }
    
    /**
     * Update the editing panel with the currently selected SkinSpecification.
     */
    private void setupEditPanel() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        removeListeners();
        editPanel.removeAll();
        // Nothing to do if we selected nothing...
        if (skinSpecCompList.getSelectedIndex() == -1) {
            skinEditPanel.removeAll();
            udEditPanel.removeAll();
            return;            
        }
        UIComponents selectedComp = skinSpecCompList.getSelectedValue();

        if ((selectedComp == UIComponents.DefaultButton)
                || (selectedComp == UIComponents.DefaultUIElement)
                || (selectedComp == UIComponents.UnitDisplay)) {
            removeCompButton.setEnabled(false);
        } else {
            removeCompButton.setEnabled(true);
        }

        editPanel.removeAll();
        if (selectedComp == UIComponents.UnitDisplay) {
            enablePlain.setSelected(false);
            enablePlain.setEnabled(false);
            enableBorders.setSelected(true);
            enableBorders.setEnabled(false);
            udEditPanel.setupSkinEditPanel(SkinXMLHandler.getUnitDisplaySkin());
            editPanel.add(udEditPanel);
        } else {
            try {
                SkinSpecification skinSpec = SkinXMLHandler.getSkin(selectedComp.getComp());
                enablePlain.setSelected(skinSpec.plain);
                enablePlain.setEnabled(true);
                enableBorders.setSelected(!skinSpec.noBorder);
                enableBorders.setEnabled(true);
                skinEditPanel.setupSkinEditPanel(skinSpec);
                editPanel.add(skinEditPanel);
            } catch (Exception ignored) {

            }
        }
        
        revalidate();
        addListeners();
        setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        
        if (e.getSource().equals(skinSpecCompList)) {
            setupEditPanel();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(currSkinCombo)) {
            String newSkinFile = (String) currSkinCombo.getSelectedItem();

            boolean success = SkinXMLHandler.initSkinXMLHandler(newSkinFile);
            if (!success) {
                SkinXMLHandler.initSkinXMLHandler(Configuration.skinsDir().getPath() + SkinXMLHandler.defaultSkinXML);
                String title = Messages.getString("CommonSettingsDialog.skinFileFail.title");
                String msg = Messages.getString("CommonSettingsDialog.skinFileFail.msg");
                JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
            }
            mainGUI.updateBorder();
            populateSkinSpecComponents();
            setupEditPanel();
        } else if (e.getSource().equals(enablePlain)) {
            notifySkinChanges(false);
        } else if (e.getSource().equals(enableBorders)) {
            skinEditPanel.setEnabled(enableBorders.isSelected());
            notifySkinChanges(false);
        } else if (e.getSource().equals(resetSkinButton)) {
            setupEditPanel();
        } else if (e.getSource().equals(saveSkinButton)) {
            saveSkinButton.setEnabled(false);
            String currComp = skinSpecCompList.getSelectedValue().getComp();
            String file = saveDialog();
            if (!file.isBlank()) {
                SkinXMLHandler.writeSkinToFile(file);
                updateSkinCombo(file);
                SkinSpecification skinSpec = SkinXMLHandler.getSkin(currComp);
                skinEditPanel.updateSkinSpec(skinSpec, enableBorders.isSelected(), enablePlain.isSelected());
                mainGUI.updateBorder();
            }
        } else if (e.getSource().equals(addCompButton)) {
            ArrayList<UIComponents> newComps = new ArrayList<>();
            for (UIComponents c : UIComponents.values()) {
                if (!skinSpecCompModel.contains(c)) {
                    newComps.add(c);
                }
            }
            String msg = Messages.getString("SkinEditor.AddCompMsg");
            String title = Messages.getString("SkinEditor.AddCompTitle");
            UIComponents choice = (UIComponents) JOptionPane
                    .showInputDialog(this, msg, title,
                            JOptionPane.QUESTION_MESSAGE, null,
                            newComps.toArray(), null);
            if (choice == null) {
                return;
            }
            SkinXMLHandler.addNewComp(choice.getComp());
            populateSkinSpecComponents();
            notifySkinChanges(true);
        } else if (e.getSource().equals(removeCompButton)) {
            UIComponents selectedComp = skinSpecCompList.getSelectedValue();
            // Don't remove defaults - this button shouldn't be enabled in this
            // case, but just to be sure...
            if ((selectedComp == UIComponents.DefaultButton)
                    || (selectedComp == UIComponents.DefaultUIElement)) {
                return;
            } else {
                SkinXMLHandler.removeComp(selectedComp.getComp());
                populateSkinSpecComponents();
                setupEditPanel();
                notifySkinChanges(true);
            }
        }
    }

    private String saveDialog() {
        String userDirName = PreferenceManager.getClientPreferences().getUserDir();
        File userDir = new File(userDirName, Configuration.skinsDir().toString());
        String path;
        if (!userDirName.isBlank() && userDir.isDirectory()) {
            path = userDir.getPath();
        } else {
            path = Configuration.skinsDir().getPath();
        }
        JFileChooser fc = new JFileChooser(path);
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return (dir.getName().endsWith(".xml") || dir.isDirectory());
            }

            @Override
            public String getDescription() {
                return "*.xml";
            }
        });
        fc.setDialogTitle(Messages.getString("ClientGUI.FileSaveDialog.title"));
        String file = "";

        int returnVal = fc.showSaveDialog(mainGUI);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return file;
        }
        if (fc.getSelectedFile() != null) {
             file = path + "/" + fc.getSelectedFile().getName();
        }
        return file;
    }

    /**
     * Notifies the SkinSpecEditor that a change has been made to the currently
     * selected component's SkinSpecification.
     */
    public void notifySkinChanges(boolean setupSkinEditPanel) {
        saveSkinButton.setEnabled(true);

        String currComp = skinSpecCompList.getSelectedValue().getComp();
        if (skinSpecCompList.getSelectedValue() == UIComponents.UnitDisplay) {
            udEditPanel.updateSkinSpec(SkinXMLHandler.getUnitDisplaySkin());
        } else {
            SkinSpecification skinSpec = SkinXMLHandler.getSkin(currComp);
            skinEditPanel.updateSkinSpec(skinSpec, enableBorders.isSelected(), enablePlain.isSelected());
            if (setupSkinEditPanel) {
                try {
                    skinEditPanel.setupSkinEditPanel(skinSpec);
                } catch (Exception ignored) {

                }
            }
        }
        mainGUI.updateBorder();
    }
}
