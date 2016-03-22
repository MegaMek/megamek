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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.VerifyIsPositiveInteger;
import megamek.client.ui.swing.widget.VerifiableTextField;
import megamek.common.MapSettings;
import megamek.common.util.StringUtil;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %Id%
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
    private final VerifiableTextField mapThemeField = new VerifiableTextField(10);

    // Control buttons
    private final JButton okayButton = new JButton(Messages.getString("Okay"));
    private final JButton loadButton = new JButton(Messages.getString("RandomMapDialog.Load"));
    private final JButton saveButton = new JButton(Messages.getString("RandomMapDialog.Save"));

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
        mapThemeField.setSelectAllTextOnGotFocus(true);
        mapThemeField.setText(mapSettings.getTheme());
        mapThemeField.setToolTipText(Messages.getString("RandomMapDialog.mapThemeField.toolTip"));
        panel.add(mapThemeField, constraints);

        return panel;
    }

    private JPanel setupControlsPanel() {
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

        return panel;
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
        if (!StringUtil.isNullOrEmpty(fileName)) {
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
                                        "data" + File.separator + "boards", null, ".xml", "(*.xml)", false);

        // If we don't have a file, there's nothing to load.
        if (selectedFile == null) {
            return;
        }

        // Load the file.  If there is an error, log it and return.
        try(InputStream is = new FileInputStream(selectedFile)) {
            mapSettings = MapSettings.getInstance(is);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

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
                        + File.separator + "boards", null, ".xml", "(*.xml)",
                true);

        // If no file was selected, we're done.
        if (selectedFile == null) {
            return false;
        }

        // Load the changed settings into the existing map settings object.
        try(OutputStream os = new FileOutputStream(selectedFile)) {
            mapSettings.save(os);
        } catch (Exception ex) {
            ex.printStackTrace();
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
        newMapSettings.setTheme(mapThemeField.getText());
        this.mapSettings = newMapSettings;

        // Sent the map settings to either the server or the observer as needed.
        if (CLIENT != null) {
            CLIENT.sendMapSettings(newMapSettings);
            return true;
        }
        MAP_SETTINGS_OBSERVER.updateMapSettings(newMapSettings);
        return true;
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
        }
    }
}
