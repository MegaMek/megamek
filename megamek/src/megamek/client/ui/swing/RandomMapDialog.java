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

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.util.VerifyIsPositiveInteger;
import megamek.client.ui.swing.widget.VerifiableTextField;
import megamek.codeUtilities.StringUtility;
import megamek.common.MapSettings;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Set;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 3/13/14 2:41 PM
 */
public class RandomMapDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 7758433698878123806L;
    // Views.
    private static final String VIEW_BASIC = Messages.getString("RandomMapDialog.Normal");
    private static final String VIEW_ADVANCED = Messages.getString("RandomMapDialog.Advanced");

    // External helpers.
    private final JFrame PARENT;
    private final IMapSettingsObserver MAP_SETTINGS_OBSERVER;
    private final Client CLIENT;
    private final GUIPreferences guip = GUIPreferences.getInstance();

    // How the map will be set up.
    private MapSettings mapSettings;

    // View switching objects.
    private final RandomMapPanelBasic basicPanel;
    private final RandomMapPanelAdvanced advancedPanel;
    private final JRadioButton basicButton = new JRadioButton(VIEW_BASIC);
    private final JRadioButton advancedButton = new JRadioButton(VIEW_ADVANCED);
    private final CardLayout cardLayout = new CardLayout(0, 0);
    private final JPanel mainDisplay = new JPanel();

    // General map settings.
    private final JLabel mapSizeLabel = new JLabel(Messages.getString("RandomMapDialog.BoardSize"));
    private final JLabel mapSizeSeperatorLabel = new JLabel("x");
    private final JLabel mapThemeLabel = new JLabel(Messages.getString("RandomMapDialog.labTheme"));
    private final VerifiableTextField mapWidthField = new VerifiableTextField(4);
    private final VerifiableTextField mapHeightField = new VerifiableTextField(4);
    private final JComboBox<String> choTheme = new JComboBox<>();

    // Control buttons
    private final JButton okayButton = new JButton(Messages.getString("Okay"));
    private final JButton loadButton = new JButton(Messages.getString("RandomMapDialog.Load"));
    private final JButton saveButton = new JButton(Messages.getString("RandomMapDialog.Save"));
    private final JButton cancelButton = new JButton(Messages.getString("Cancel"));
    private final JCheckBox showAtStartButton = new JCheckBox(Messages.getString("RandomMapDialog.ShowAtStart"));
    
    // Return value
    private boolean userCancel;

    /**
     * Constructor for this dialog.
     *
     * @param parent              The parent {@link JFrame} invoking this dialog.
     * @param mapSettingsObserver The {@link IMapSettingsObserver} objects to which the map setting will be passed if
     *                            this is a local only game.
     * @param client              The {@link Client} that will send the map settings to the server if this is a
     *                            server-based game.
     * @param mapSettings         The {@link MapSettings} describing the map to be generated.
     */
    public RandomMapDialog(JFrame parent, IMapSettingsObserver mapSettingsObserver, Client client,
                           MapSettings mapSettings) {
        this(parent, mapSettingsObserver, client, mapSettings, Messages.getString("RandomMapDialog.title"));
    }
    
    /**
     * Constructor for this dialog.
     *
     * @param parent              The parent {@link JFrame} invoking this dialog.
     * @param mapSettingsObserver The {@link IMapSettingsObserver} objects to which the map setting will be passed if
     *                            this is a local only game.
     * @param client              The {@link Client} that will send the map settings to the server if this is a
     *                            server-based game.
     * @param mapSettings         The {@link MapSettings} describing the map to be generated.
     */
    public RandomMapDialog(JFrame parent, IMapSettingsObserver mapSettingsObserver, Client client,
                           MapSettings mapSettings, String title) {
        super(parent, title, true);
        this.mapSettings = mapSettings;
        PARENT = parent;
        MAP_SETTINGS_OBSERVER = mapSettingsObserver;
        CLIENT = client;
        basicPanel = new RandomMapPanelBasic(mapSettings);
        advancedPanel = new RandomMapPanelAdvanced(mapSettings);

        initGUI();
        setResizable(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { closeWithoutNewMap(); }
        });

        pack();
        validate();
        setSize(new Dimension(600, 600));
        setLocationRelativeTo(PARENT);
    }

    private void initGUI() {
        setupMainPanel();

        final JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(setupTopPanel(), BorderLayout.NORTH);
        contentPanel.add(mainDisplay, BorderLayout.CENTER);
        contentPanel.add(setupControlsPanel(), BorderLayout.SOUTH);

        add(contentPanel);
        switchView(VIEW_BASIC, true);
    }

    private void switchView(String viewName, boolean initializing) {
        // Copy the updated map settings to the other panel.
        if (!initializing && VIEW_ADVANCED.equalsIgnoreCase(viewName)) {
            mapSettings = basicPanel.getMapSettings();
            if (mapSettings == null) {
                basicButton.setSelected(true);
                return;
            }
            advancedPanel.setMapSettings(mapSettings);
        } else if (!initializing) {
            mapSettings = advancedPanel.getMapSettings();
            if (mapSettings == null) {
                advancedButton.setSelected(true);
                return;
            }
            basicPanel.setMapSettings(mapSettings);
        }

        cardLayout.show(mainDisplay, viewName);
        mainDisplay.revalidate();
    }

    private void setupMainPanel() {
        mainDisplay.setLayout(cardLayout);
        mainDisplay.add(basicPanel, VIEW_BASIC);
        mainDisplay.add(advancedPanel, VIEW_ADVANCED);
        mainDisplay.setBorder(new LineBorder(Color.black, 1));
    }

    private JPanel setupDisplayButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        ButtonGroup displayButtonGroup = new ButtonGroup();

        basicButton.addActionListener(this);
        displayButtonGroup.add(basicButton);
        panel.add(basicButton);

        advancedButton.addActionListener(this);
        displayButtonGroup.add(advancedButton);
        panel.add(advancedButton);

        basicButton.setSelected(true);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1), "View"));

        return panel;
    }

    private JPanel setupTopPanel() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        JPanel panel = new JPanel(layout);

        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(2, 2, 2, 2);

        // Row 1, Column 1.
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 4;
        JPanel displayOptionsPanel = setupDisplayButtons();
        panel.add(displayOptionsPanel, constraints);

        // Row 2, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        panel.add(mapSizeLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        mapWidthField.setSelectAllTextOnGotFocus(true);
        mapWidthField.setRequired(true);
        mapWidthField.setText(String.valueOf(mapSettings.getBoardWidth()));
        mapWidthField.addVerifier(new VerifyIsPositiveInteger());
        mapWidthField.setToolTipText(Messages.getString("RandomMapDialog.mapWidthField.toolTip"));
        panel.add(mapWidthField, constraints);

        // Row 2, Column 3.
        constraints.gridx++;
        panel.add(mapSizeSeperatorLabel, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        mapHeightField.setSelectAllTextOnGotFocus(true);
        mapHeightField.setRequired(true);
        mapHeightField.setText(String.valueOf(mapSettings.getBoardHeight()));
        mapHeightField.addVerifier(new VerifyIsPositiveInteger());
        mapHeightField.setToolTipText(Messages.getString("RandomMapDialog.mapHeightField.toolTip"));
        panel.add(mapHeightField, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        panel.add(mapThemeLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.gridwidth = 3;
        choTheme.addActionListener(this);
        choTheme.setToolTipText(Messages.getString("RandomMapDialog.mapThemeField.toolTip"));
        panel.add(choTheme, constraints);

        return panel;
    }

    private JPanel setupControlsPanel() {
        JPanel outerpanel = new JPanel(new BorderLayout());
        
        // The left-side panel contains only the Show on startup option
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
       
        // Add the option only when in the Map Editor
        if (CLIENT == null) {
            showAtStartButton.addActionListener(this);
            showAtStartButton.setMnemonic(showAtStartButton.getText().charAt(0));
            showAtStartButton.setSelected(guip.getBoardEdRndStart());
            leftPanel.add(showAtStartButton);
        }
        
        // The main panel with the Okay, Cancel etc. buttons
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        
        loadButton.addActionListener(this);
        loadButton.setMnemonic(loadButton.getText().charAt(0));
        panel.add(loadButton);

        saveButton.addActionListener(this);
        saveButton.setMnemonic(saveButton.getText().charAt(0));
        panel.add(saveButton);

        okayButton.addActionListener(this);
        okayButton.setMnemonic(okayButton.getText().charAt(0));
        panel.add(okayButton);
        
        cancelButton.addActionListener(this);
        cancelButton.setMnemonic(cancelButton.getText().charAt(0));
        panel.add(cancelButton);

        outerpanel.add(leftPanel, BorderLayout.LINE_START);
        outerpanel.add(panel, BorderLayout.CENTER);

        return outerpanel;
    }

    private File fileBrowser(String title, String targetDir, String fileName, final String extension,
                             final String description, boolean isSave) {

        // Create a new instance of the file chooser.
        JFileChooser fileChooser = new JFileChooser(targetDir);

        // Only allow selectoin of one file.
        fileChooser.setMultiSelectionEnabled(false);

        // Give the file chooser a title.
        fileChooser.setDialogTitle(title);

        // If we have a file to start with, select it.
        if (!StringUtility.isNullOrBlank(fileName)) {
            fileChooser.setSelectedFile(new File(targetDir + fileName));
        }

        // Put a filter on the files that the user can select the proper file.
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return (f.getPath().toLowerCase().endsWith(extension) || f.isDirectory());
            }

            @Override
            public String getDescription() {
                return description;
            }
        });

        // Turn off the ability to select any file.
        fileChooser.setAcceptAllFileFilterUsed(false);

        // Show the dialog and store the option clicked (open or cancel).
        int option;
        if (isSave) {
            option = fileChooser.showSaveDialog(null);
        } else {
            option = fileChooser.showOpenDialog(null);
        }

        // If the user did chose to open...
        if (JFileChooser.APPROVE_OPTION == option) {
            // Get the file that was selected and return it.
            if (fileChooser.getSelectedFile().getPath().endsWith(extension)) {
                return fileChooser.getSelectedFile();
            } else {
                return new File(fileChooser.getSelectedFile() + extension);
            }            
        }
        return null;
    }

    private void doLoad() {

        // Get the user-selected file.
        File selectedFile = fileBrowser(Messages.getString("RandomMapDialog.FileLoadDialog"),
                                        "data" + File.separator + "mapgen", null, ".xml", "(*.xml)", false);

        // If we don't have a file, there's nothing to load.
        if (selectedFile == null) {
            return;
        }

        // Cache the selected boards, so we can restore them
        ArrayList<String> selectedBoards = mapSettings.getBoardsSelectedVector();
        // Load the file.  If there is an error, log it and return.
        try (InputStream is = new FileInputStream(selectedFile)) {
            mapSettings = MapSettings.getInstance(is);
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return;
        }
        mapSettings.setBoardsSelectedVector(selectedBoards);

        // Pass the loaded settings into the two different views.
        basicPanel.setMapSettings(mapSettings);
        advancedPanel.setMapSettings(mapSettings);
    }

    private boolean doSave() {

        // Apply the changes.
        if (!doApply()) {
            return false;
        }

        // Have the user choose a file to save the new settings to.
        File selectedFile = fileBrowser(
                Messages.getString("RandomMapDialog.FileSaveDialog"), "data"
                        + File.separator + "mapgen", null, ".xml", "(*.xml)",
                true);

        // If no file was selected, we're done.
        if (selectedFile == null) {
            return false;
        }

        // Load the changed settings into the existing map settings object.
        try (OutputStream os = new FileOutputStream(selectedFile)) {
            mapSettings.save(os);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        return true;
    }

    private boolean doApply() {

        // Get the new settings from the basic or advanced view.
        MapSettings newMapSettings;
        if (basicButton.isSelected()) {
            newMapSettings = basicPanel.getMapSettings();
            advancedPanel.setMapSettings(newMapSettings);
        } else {
            newMapSettings = advancedPanel.getMapSettings();
            basicPanel.setMapSettings(newMapSettings);
        }

        // If we have no settings, we're done.
        if (newMapSettings == null) {
            return false;
        }

        // Get the general settings from this panel.
        newMapSettings.setBoardSize(mapWidthField.getAsInt(), mapHeightField.getAsInt());
        newMapSettings.setTheme((String) choTheme.getSelectedItem());
        this.mapSettings = newMapSettings;

        // Sent the map settings to either the server or the observer as needed.
        if (CLIENT != null) {
            CLIENT.sendMapSettings(newMapSettings);
            return true;
        }
        MAP_SETTINGS_OBSERVER.updateMapSettings(newMapSettings);
        return true;
    }
    
    public boolean activateDialog(Set<String> themeList) {
        for (String s : themeList) {
            choTheme.addItem(s);
        }
        choTheme.setSelectedItem(mapSettings.getTheme());
        userCancel = false;
        setVisible(true);
        return userCancel;
    }
    
    private void closeWithoutNewMap() {
        userCancel = true;
        setVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (basicButton.equals(e.getSource())) {
            switchView(VIEW_BASIC, false);
        } else if (advancedButton.equals(e.getSource())) {
            switchView(VIEW_ADVANCED, false);
        } else if (loadButton.equals(e.getSource())) {
            doLoad();
        } else if (saveButton.equals(e.getSource())) {
            if (doSave()) {
                setVisible(false);
            }
        } else if (okayButton.equals(e.getSource())) {
            if (doApply()) {
                setVisible(false);
            }
        } else if (cancelButton.equals(e.getSource())) {
            closeWithoutNewMap();
        } else if (showAtStartButton.equals(e.getSource())) {
            guip.setBoardEdRndStart(showAtStartButton.isSelected());
        }
    }
    
    @Override
    public void setVisible(boolean b) {
        if (b) {
            adaptToGUIScale();
            loadWindowSettings();
        } else {
            saveWindowSettings();
        }
        super.setVisible(b);
    }

    /** Saves the position, size and split of the dialog. */
    private void saveWindowSettings() {
        GUIPreferences guip = GUIPreferences.getInstance();
        guip.setValue(GUIPreferences.RND_MAP_POS_X, getLocation().x);
        guip.setValue(GUIPreferences.RND_MAP_POS_Y, getLocation().y);
        guip.setValue(GUIPreferences.RND_MAP_SIZE_WIDTH, getSize().width);
        guip.setValue(GUIPreferences.RND_MAP_SIZE_HEIGHT, getSize().height);
        guip.setValue(GUIPreferences.RND_MAP_ADVANCED, advancedButton.isSelected());
    }
    
    private void loadWindowSettings() {
        GUIPreferences guip = GUIPreferences.getInstance();
        setSize(guip.getInt(GUIPreferences.RND_MAP_SIZE_WIDTH), 
                guip.getInt(GUIPreferences.RND_MAP_SIZE_HEIGHT));
        setLocation(guip.getInt(GUIPreferences.RND_MAP_POS_X), 
                guip.getInt(GUIPreferences.RND_MAP_POS_Y));
        // Restore the advanced view if it was used last
        if (guip.getBoolean(GUIPreferences.RND_MAP_ADVANCED)) {
            switchView(VIEW_ADVANCED, false);
            advancedButton.setSelected(true);
        } else {
            switchView(VIEW_BASIC, false);
            basicButton.setSelected(true);
        }
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}
