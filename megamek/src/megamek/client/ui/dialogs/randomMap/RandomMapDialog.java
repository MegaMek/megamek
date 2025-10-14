/*
 * Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs.randomMap;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.IMapSettingsObserver;
import megamek.client.ui.util.verifier.VerifyIsPositiveInteger;
import megamek.client.ui.widget.VerifiableTextField;
import megamek.codeUtilities.StringUtility;
import megamek.common.loaders.MapSettings;
import megamek.logging.MMLogger;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class RandomMapDialog extends JDialog implements ActionListener {

    private static final MMLogger LOGGER = MMLogger.create(RandomMapDialog.class);

    private final IMapSettingsObserver mapSettingsObserver;
    private final Client client;
    private final GUIPreferences guip = GUIPreferences.getInstance();

    // How the map will be set up.
    private MapSettings mapSettings;

    // View switching objects.
    private final RandomMapPanelBasicPanel basicPanel;
    private final RandomMapPanelAdvancedPanel advancedPanel;
    private final JRadioButton basicButton = new JRadioButton(Messages.getString("RandomMapDialog.Normal"));
    private final JRadioButton advancedButton = new JRadioButton(Messages.getString("RandomMapDialog.Advanced"));
    private final CardLayout cardLayout = new CardLayout(0, 0);
    private final JPanel mainDisplay = new JPanel();

    // General map settings.
    private final JLabel mapSizeLabel = new JLabel(Messages.getString("RandomMapDialog.BoardSize"));
    private final JLabel mapSizeSeparatorLabel = new JLabel("x");
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
     * @param parent              The parent JFrame invoking this dialog.
     * @param mapSettingsObserver The {@link IMapSettingsObserver} objects to which the map setting will be passed if
     *                            this is a local only game.
     * @param client              The {@link Client} that will send the map settings to the server if this is a
     *                            server-based game.
     * @param mapSettings         The {@link MapSettings} describing the map to be generated.
     */
    public RandomMapDialog(JFrame parent, IMapSettingsObserver mapSettingsObserver, Client client,
          MapSettings mapSettings) {
        super(parent, Messages.getString("RandomMapDialog.title"), true);
        this.mapSettings = mapSettings;
        // External helpers.
        this.mapSettingsObserver = mapSettingsObserver;
        this.client = client;
        basicPanel = new RandomMapPanelBasicPanel(mapSettings);
        advancedPanel = new RandomMapPanelAdvancedPanel(mapSettings);

        initGUI();
        setResizable(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWithoutNewMap();
            }
        });

        pack();
        validate();
        setSize(new Dimension(600, 600));
        setLocationRelativeTo(parent);

        String closeAction = "closeAction";
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, closeAction);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, closeAction);
        getRootPane().getActionMap().put(closeAction, new CloseAction(this));
    }

    private void initGUI() {
        setupMainPanel();

        final JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(setupTopPanel(), BorderLayout.NORTH);
        contentPanel.add(mainDisplay, BorderLayout.CENTER);
        contentPanel.add(setupControlsPanel(), BorderLayout.SOUTH);

        add(contentPanel);
        switchView(true, true);
    }

    private void switchView(boolean advanced, boolean initializing) {
        // Copy the updated map settings to the other panel.
        if (!initializing && !advanced) {
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

        cardLayout.show(mainDisplay, (advanced ? advancedButton : basicButton).getText());
        mainDisplay.revalidate();
    }

    private void setupMainPanel() {
        mainDisplay.setLayout(cardLayout);
        mainDisplay.add(basicPanel, Messages.getString("RandomMapDialog.Normal"));
        mainDisplay.add(advancedPanel, Messages.getString("RandomMapDialog.Advanced"));
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
        panel.add(mapSizeSeparatorLabel, constraints);

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
        JPanel outerPanel = new JPanel(new BorderLayout());

        // The left-side panel contains only the Show on startup option
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

        // Add the option only when in the Map Editor
        if (client == null) {
            showAtStartButton.addActionListener(this);
            showAtStartButton.setMnemonic(showAtStartButton.getText().charAt(0));
            showAtStartButton.setSelected(guip.getBoardEdRndStart());
            leftPanel.add(showAtStartButton);
        }

        // The main panel with the Okay, Cancel etc. buttons
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

        loadButton.addActionListener(e -> doLoad());
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

        outerPanel.add(leftPanel, BorderLayout.LINE_START);
        outerPanel.add(panel, BorderLayout.CENTER);

        return outerPanel;
    }

    private File fileBrowser(String title, String targetDir, String fileName, final String extension,
          final String description, boolean isSave) {

        // Create a new instance of the file chooser.
        JFileChooser fileChooser = new JFileChooser(targetDir);

        // Only allow selection of one file.
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

        // If the user did choose to open...
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
        if (selectedFile == null) {
            return;
        }

        // Load the file. If there is an error, log it and return.
        MapSettings newMapSettings;
        try (InputStream is = new FileInputStream(selectedFile)) {
            newMapSettings = MapSettings.getInstance(is);
        } catch (Exception e) {
            LOGGER.error(e, "");
            return;
        }

        if (mapSettings != null) {
            newMapSettings.setMapSize(mapSettings.getMapWidth(), mapSettings.getMapHeight());
            newMapSettings.setBoardsSelectedVector(mapSettings.getBoardsSelectedVector());
        }
        mapSettings = newMapSettings;

        // Pass the loaded settings into the two different views.
        choTheme.setSelectedItem(mapSettings.getTheme());
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
                    + File.separator + "mapgen",
              null, ".xml", "(*.xml)",
              true);

        // If no file was selected, we're done.
        if (selectedFile == null) {
            return false;
        }

        // Load the changed settings into the existing map settings object.
        try (OutputStream os = new FileOutputStream(selectedFile)) {
            mapSettings.save(os);
        } catch (Exception ex) {
            LOGGER.error(ex, "");
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
        mapSettings = newMapSettings;

        // Sent the map settings to either the server or the observer as needed.
        if (client != null) {
            client.sendMapSettings(newMapSettings);
            return true;
        }
        mapSettingsObserver.updateMapSettings(newMapSettings);
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
            switchView(false, false);
        } else if (advancedButton.equals(e.getSource())) {
            switchView(true, false);
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
            switchView(true, false);
            advancedButton.setSelected(true);
        } else {
            switchView(false, false);
            basicButton.setSelected(true);
        }
    }
}
