/*
 * Copyright (C) 2022-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.advancedsearch;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.formdev.flatlaf.FlatClientProperties;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ButtonEsc;
import megamek.client.ui.buttons.DialogButton;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;

/**
 * This is the dialog for advanced unit filtering, mostly for the unit selector. It contains the TW advanced search in
 * one tab and the AlphaStrike search in another tab. Both searches can be used simultaneously.
 */
public class AdvancedSearchDialog extends AbstractButtonDialog {

    private static final String SEARCH_FOLDER = "mmconf/searches";
    private static final String RECENT_SEARCH_STORE = "mmconf/recent-advanced-searches.json";

    private static final ObjectMapper MAPPER = JsonMapper.builder()
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(SerializationFeature.INDENT_OUTPUT)
          .build();

    private final TWAdvancedSearchPanel totalWarTab;
    private final ASAdvancedSearchPanel alphaStrikeTab = new ASAdvancedSearchPanel();
    private final JTabbedPane advancedSearchPane = new JTabbedPane();

    public AdvancedSearchDialog(JFrame parent, int allowedYear) {
        super(parent, true, "AdvancedSearchDialog", "AdvancedSearchDialog.title");
        totalWarTab = new TWAdvancedSearchPanel(allowedYear);
        advancedSearchPane.addTab("Total Warfare", totalWarTab);
        advancedSearchPane.addTab("Alpha Strike", new TWAdvancedSearchPanel.StandardScrollPane(alphaStrikeTab));
        initialize();
    }

    @Override
    public void setVisible(boolean b) {
        alphaStrikeTab.saveValues();
        super.setVisible(b);
    }

    @Override
    protected void okAction() {
        super.okAction();
        totalWarTab.prepareFilter();
    }

    @Override
    protected void cancelAction() {
        alphaStrikeTab.resetValues();
        super.cancelAction();
    }

    @Override
    protected JPanel createButtonPanel() {
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveSearchStateAs());

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(this::showLoadPopup);

        JButton cancelButton = new ButtonEsc(new CloseAction(this));
        JButton okButton = new DialogButton(Messages.getString("Ok.text"));
        okButton.addActionListener(this::okButtonActionPerformed);
        getRootPane().setDefaultButton(okButton);

        JPanel notePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        notePanel.add(Box.createHorizontalStrut(20));
        var noteLabel = new JLabel(Messages.getString("MekSelectorDialog.Search.Combine"));
        noteLabel.putClientProperty(FlatClientProperties.STYLE, "foreground: mix($Label.foreground, #afa, 60%)");
        notePanel.add(noteLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        JPanel outerPanel = new JPanel(new GridLayout(1, 1));
        outerPanel.setBorder(BorderFactory.createCompoundBorder(
              new MatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
              new EmptyBorder(10, 0, 10, 0)));
        outerPanel.add(notePanel);
        outerPanel.add(buttonPanel);

        return outerPanel;
    }

    private void saveSearchStateAs() {
        String name = JOptionPane.showInputDialog(this, "Choose a name for the search");
        if ((name == null) || name.isBlank()) {
            return;
        }

        String fileName = sanitizeFileName(name);
        if (fileName == null) {
            JOptionPane.showMessageDialog(this,
                  "Could not create a valid file name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File directory = new File(SEARCH_FOLDER);
        if (!directory.exists() && !directory.mkdir()) {
            JOptionPane.showMessageDialog(this,
                  "Could not create directory " + SEARCH_FOLDER,
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
        }

        saveSearchState(new File(SEARCH_FOLDER, fileName + ".json"), name);
    }

    private static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot create a filename from a null or empty name");
        }
        String sanitized = name;
        sanitized = sanitized.replaceAll("[\\<>:\"/\\\\|?*\\p{Cntrl}]", "");
        sanitized = sanitized.replaceAll("[. ]+$", "");
        sanitized = sanitized.replaceAll("^_|_$", "");
        if (sanitized.isEmpty()) {
            return null;
        }
        return sanitized;
    }

    private void saveSearchState(File file, String name) {
        var state = new AdvSearchState();
        state.name = name;
        state.twState = totalWarTab.getState();
        state.asState = alphaStrikeTab.getState();
        try {
            save(file, state);
            new RecentFilesStore(Path.of(RECENT_SEARCH_STORE)).touch(file.toPath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                  "Error saving search state: " + e.getMessage(),
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showLoadPopup(ActionEvent e) {
        var popup = createPopupMenu(path -> loadSearchState(path.toFile()));
        popup.addSeparator();
        JMenuItem fileItem = new JMenuItem("from File...");
        fileItem.addActionListener(ev -> loadFile());
        popup.add(fileItem);
        Dimension popupSize = popup.getPreferredSize();
        Dimension sourceSize = ((JComponent) e.getSource()).getPreferredSize();
        popup.show((Component) e.getSource(), (sourceSize.width - popupSize.width) / 2, -popupSize.height - 15);
    }

    private void loadFile() {
        JFileChooser fc = new JFileChooser(SEARCH_FOLDER);
        fc.setDialogTitle(Messages.getString("BoardEditor.loadBoard"));
        fc.setFileFilter(new FileNameExtensionFilter("Search Files (.json)", "json"));
        int returnVal = fc.showOpenDialog(this);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return;
        }
        loadSearchState(fc.getSelectedFile());
    }

    private void loadSearchState(File file) {
        try {
            var state = load(file);
            clearSearches();
            totalWarTab.applyState(state.twState);
            alphaStrikeTab.applyState(state.asState);
            new RecentFilesStore(Path.of(RECENT_SEARCH_STORE)).touch(file.toPath());
        } catch (IOException|IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Error loading search state: " + e.getMessage(),
                  "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected Container createCenterPane() {
        return advancedSearchPane;
    }

    /** Deactivates the search fields in both search tabs so that no units are filtered out. */
    public void clearSearches() {
        totalWarTab.clearValues();
        alphaStrikeTab.clearValues();
    }

    public ASAdvancedSearchPanel getASAdvancedSearch() {
        return alphaStrikeTab;
    }

    public TWAdvancedSearchPanel getTWAdvancedSearch() {
        return totalWarTab;
    }

    /**
     * Creates a popup menu containing up to 10 most recently modified JSON files in the given folder. Each menu item
     * uses the JSON field "name" as its label and calls the provided handler with the corresponding file when clicked.
     */
    private JPopupMenu createPopupMenu(Consumer<Path> onFileSelected) {
        JPopupMenu popup = new JPopupMenu();

        try {
            var store = new RecentFilesStore(Path.of(RECENT_SEARCH_STORE));
            List<Path> menuEntries = store.getRecentFiles();
            for (Path file : menuEntries) {
                String name = readNameField(file);
                if (name == null || name.isBlank()) {
                    continue;
                }

                JMenuItem item = new JMenuItem(name);
                item.addActionListener(e -> onFileSelected.accept(file));
                popup.add(item);
            }
        } catch (IOException ignored) {
            JMenuItem errorItem = new JMenuItem("Error retrieving recent files");
            errorItem.setEnabled(false);
            popup.add(errorItem);
        }

        if (popup.getComponentCount() == 0) {
            JMenuItem noRecentItem = new JMenuItem("No recent files");
            noRecentItem.setEnabled(false);
            popup.add(noRecentItem);
        }

        return popup;
    }

    private String readNameField(Path file) {
        try {
            JsonNode root = MAPPER.readTree(file.toFile());
            JsonNode nameNode = root.get("name");
            return nameNode != null && nameNode.isTextual() ? nameNode.asText() : null;
        } catch (IOException e) {
            return null;
        }
    }

    static boolean isAdvancedSearchFile(File file) {
        try {
            JsonNode root = MAPPER.readTree(file);
            JsonNode contentNode = root.get("content");
            return contentNode != null && contentNode.asText().equals(AdvSearchState.CONTENT);
        } catch (IOException e) {
            return false;
        }
    }

    static AdvSearchState load(File file) throws IOException, IllegalArgumentException {
        if (!isAdvancedSearchFile(file)) {
            throw new IllegalArgumentException("The specified file is not an advanced search file.");
        }
        return MAPPER.readValue(file, AdvSearchState.class);
    }

    private void save(File file, AdvSearchState state) throws IOException {
        MAPPER.writeValue(file, state);
    }
}
