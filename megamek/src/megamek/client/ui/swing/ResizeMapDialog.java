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
import megamek.client.ui.swing.util.VerifyInRange;
import megamek.client.ui.swing.widget.VerifiableTextField;
import megamek.codeUtilities.StringUtility;
import megamek.common.MapSettings;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Set;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 3/13/14 2:41 PM
 */
public class ResizeMapDialog extends JDialog implements ActionListener, KeyListener {

    private static final long serialVersionUID = 7758433698878123806L;

    // External helpers.
    private final JFrame PARENT;
    private final IMapSettingsObserver MAP_SETTINGS_OBSERVER;
    private final Client CLIENT;

    // How the map will be set up.
    private MapSettings mapSettings;

    // View switching objects.
    private final RandomMapPanelBasic basicPanel;
    private final RandomMapPanelAdvanced advancedPanel;
    private final JRadioButton basicButton = new JRadioButton(Messages.getString("RandomMapDialog.Normal"));
    private final JRadioButton advancedButton = new JRadioButton(Messages.getString("RandomMapDialog.Advanced"));
    private final CardLayout cardLayout = new CardLayout(0, 0);
    private final JPanel mainDisplay = new JPanel();

    // General map settings.
    private final JLabel mapNorthLabel = new JLabel(Messages.getString("ExpandMapDialog.labelNorth"));
    private final JLabel mapEastLabel = new JLabel(Messages.getString("ExpandMapDialog.labelEast"));
    private final JLabel mapSouthLabel = new JLabel(Messages.getString("ExpandMapDialog.labelSouth"));
    private final JLabel mapWestLabel = new JLabel(Messages.getString("ExpandMapDialog.labelWest"));
    private final JLabel mapThemeLabel = new JLabel(Messages.getString("RandomMapDialog.labTheme"));
    private final VerifiableTextField mapNorthField = new VerifiableTextField(4);
    private final VerifiableTextField mapEastField = new VerifiableTextField(4);
    private final VerifiableTextField mapSouthField = new VerifiableTextField(4);
    private final VerifiableTextField mapWestField = new VerifiableTextField(4);
    private final JComboBox<String> choTheme = new JComboBox<>();
    private final JPopupMenu westNotice = new JPopupMenu();

    // Control buttons
    private final JButton okayButton = new JButton(Messages.getString("Okay"));
    private final JButton loadButton = new JButton(Messages.getString("RandomMapDialog.Load"));
    private final JButton saveButton = new JButton(Messages.getString("RandomMapDialog.Save"));
    private final JButton cancelButton = new JButton(Messages.getString("Cancel"));
    
    // Misc values
    boolean userCancel; 

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
    public ResizeMapDialog(JFrame parent, IMapSettingsObserver mapSettingsObserver, Client client,
                           MapSettings mapSettings) {
        super(parent, Messages.getString("ExpandMapDialog.title"), true);
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
            public void windowClosing(WindowEvent e) {
                doCancel(); 
            }
            
            // Hide the west edge warning when some other program is brought to foreground
            @Override
            public void windowDeactivated(WindowEvent e) {
                westNotice.setVisible(false);
                super.windowDeactivated(e);
            }

            @Override
            public void windowIconified(WindowEvent e) {
                westNotice.setVisible(false);
                super.windowIconified(e);
            }

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
        switchView(false, true);
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
        panel.add(mapNorthLabel, constraints);

        // Row 2, Column 2.
        constraints.gridx++;
        mapNorthField.setSelectAllTextOnGotFocus(true);
        mapNorthField.setRequired(true);
        mapNorthField.setText("0");
        mapNorthField.addVerifier(new VerifyInRange(Integer.MIN_VALUE, Integer.MAX_VALUE, true));
        mapNorthField.setToolTipText(Messages.getString("ExpandMapDialog.mapNorthField.toolTip"));
        mapNorthField.addKeyListener(this);
        panel.add(mapNorthField, constraints);

        // Row 2, Column 3.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        panel.add(mapEastLabel, constraints);

        // Row 2, Column 4.
        constraints.gridx++;
        mapEastField.setSelectAllTextOnGotFocus(true);
        mapEastField.setRequired(true);
        mapEastField.setText("0");
        mapEastField.addVerifier(new VerifyInRange(Integer.MIN_VALUE, Integer.MAX_VALUE, true));
        mapEastField.setToolTipText(Messages.getString("ExpandMapDialog.mapEastField.toolTip"));
        mapEastField.addKeyListener(this);
        panel.add(mapEastField, constraints);

        // Row 2, Column 5.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        panel.add(mapSouthLabel, constraints);

        // Row 2, Column 6.
        constraints.gridx++;
        mapSouthField.setSelectAllTextOnGotFocus(true);
        mapSouthField.setRequired(true);
        mapSouthField.setText("0");
        mapSouthField.addVerifier(new VerifyInRange(Integer.MIN_VALUE, Integer.MAX_VALUE, true));
        mapSouthField.setToolTipText(Messages.getString("ExpandMapDialog.mapSouthField.toolTip"));
        mapSouthField.addKeyListener(this);
        panel.add(mapSouthField, constraints);

        // Row 2, Column 7.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 1;
        panel.add(mapWestLabel, constraints);

        // Row 2, Column 8.
        constraints.gridx++;
        mapWestField.setSelectAllTextOnGotFocus(true);
        mapWestField.setRequired(true);
        mapWestField.setText("0");
        mapWestField.addVerifier(new VerifyInRange(Integer.MIN_VALUE, Integer.MAX_VALUE, true));
        mapWestField.setToolTipText(Messages.getString("ExpandMapDialog.mapWestField.toolTip"));
        mapWestField.addKeyListener(this);
        panel.add(mapWestField, constraints);

        // Row 3, Column 1.
        constraints.gridx = 0;
        constraints.gridy++;
        panel.add(mapThemeLabel, constraints);

        // Row 3, Column 2.
        constraints.gridx++;
        constraints.gridwidth = 3;
        choTheme.setToolTipText(Messages.getString("RandomMapDialog.mapThemeField.toolTip"));
        panel.add(choTheme, constraints);

        // A warning notice when the west edge expansion is an odd number and the south expansion < 1
        westNotice.add(new JLabel(Messages.getString("ExpandMapDialog.mapWestField.note")));
        westNotice.setBorder(new EmptyBorder(10, 10, 10, 10));

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
        
        cancelButton.addActionListener(this);
        cancelButton.setMnemonic(cancelButton.getText().charAt(0));
        panel.add(cancelButton);

        return panel;
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

        // If the user did chose to open...
        if (JFileChooser.APPROVE_OPTION == option) {
            // Get the file that was selected and return it.
            return fileChooser.getSelectedFile();
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

        // Load the file.  If there is an error, log it and return.
        try (InputStream is = new FileInputStream(selectedFile)) {
            mapSettings = MapSettings.getInstance(is);
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
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
        File selectedFile = fileBrowser(Messages.getString("RandomMapDialog.FileLoadDialog"),
                                        "data" + File.separator + "mapgen", null, ".xml", "(*.xml)", false);

        // If no file was selected, we're done.
        if (selectedFile == null) {
            return false;
        }

        // Load the changed settings into the existing map settings object.
        try (InputStream is = new FileInputStream(selectedFile)) {
            mapSettings = MapSettings.getInstance(is);
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
        newMapSettings.setBoardSize(mapWestField.getAsInt() + mapEastField.getAsInt() + mapSettings.getBoardWidth(),
                mapNorthField.getAsInt() + mapSouthField.getAsInt() + mapSettings.getBoardHeight());
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
    
    private void doCancel() {
        userCancel = true;
        westNotice.setVisible(false);
        setVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (basicButton.equals(e.getSource())) {
            switchView(false, false);
        } else if (advancedButton.equals(e.getSource())) {
            switchView(true, false);
        } else if (loadButton.equals(e.getSource())) {
            doLoad();
        } else if (saveButton.equals(e.getSource())) {
            if (doSave()) {
                westNotice.setVisible(false);
                setVisible(false);
            }
        } else if (okayButton.equals(e.getSource())) {
            if (doApply()) {
                westNotice.setVisible(false);
                setVisible(false);
            }
        } else if (cancelButton.equals(e.getSource())) {
            doCancel();
        }
    }

    public int getExpandNorth() {
        return mapNorthField.getAsInt();
    }

    public int getExpandEast() {
        return mapEastField.getAsInt();
    }

    public int getExpandSouth() {
        return mapSouthField.getAsInt();
    }

    public int getExpandWest() {
        return mapWestField.getAsInt();
    }
    
    private boolean isExpandValid() {
        return mapNorthField.verifyText() &&
                mapEastField.verifyText() &&
                mapSouthField.verifyText() &&
                mapWestField.verifyText();
    }

    private boolean isExpandWestProblem() {
        return mapSouthField.verifyText() &&
                mapWestField.verifyText() &&
                ((getExpandWest() & 1) == 1) &&
                (getExpandSouth() < 1);
    }

    @Override
    public void keyPressed(KeyEvent evt) {

    }

    @Override
    public void keyReleased(KeyEvent evt) {
        // Disable the Okay button when the input is invalid
        okayButton.setEnabled(isExpandValid());

        // Give notice when having an odd west expansion and no south expansion
        if (isExpandWestProblem()) {
            Point loc = mapSouthField.getLocationOnScreen();
            loc.translate(mapSouthField.getWidth()+15, 0);
            westNotice.setLocation(loc);
            westNotice.setVisible(true);
        } else {
            westNotice.setVisible(false);
        }
    }

    @Override
    public void keyTyped(KeyEvent evt) {

    }
    
    /**
     * Updates the theme list and sets the dialog to visible.
     * @return true if the user pressed Cancel.
     */
    public boolean activateDialog(Set<String> themeList) {
        for (String s : themeList) {
            choTheme.addItem(s);
        }
        choTheme.setSelectedItem(mapSettings.getTheme());
        userCancel = false;
        setVisible(true);
        return userCancel;
    }
}
