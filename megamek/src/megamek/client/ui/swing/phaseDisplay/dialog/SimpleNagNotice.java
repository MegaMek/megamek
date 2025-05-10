/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.phaseDisplay.dialog;

import megamek.client.ui.swing.ClientGUI;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;

import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.Objects;

public abstract class SimpleNagNotice {

    private static final ClientPreferences preferences = PreferenceManager.getClientPreferences();

    private static final int DEFAULT_WIDTH = 500;

    private final JPanel contentPanel = new JPanel();
    private final JCheckBox dontShowAgain = new JCheckBox("Don't show again");
    private final ClientGUI clientGui;

    private boolean initialized = false;

    protected SimpleNagNotice(ClientGUI clientGui) {
        this.clientGui = Objects.requireNonNull(clientGui);
    }

    /**
     * Override this method to provide the (localized) message to show in the notice. The message will be shown as HTML
     * and HTML and BODY tags will be added. Therefore, HTML tags can be used in the message but it should not include
     * HTML, HEAD or BODY tags.
     *
     * @return A message to show in the dialog.
     */
    protected abstract String message();

    /**
     * Override this method to provide the (localized) title for the notice dialog.
     *
     * @return A title for the dialog
     */
    protected abstract String title();

    /**
     * This method may be overridden to provide a non-empty and unique key String for the Client Preferences. When this
     * is done, a "Dont show again" checkbox will be shown and the result will be stored in the Client Preferences using
     * the given key. When this is done and the checkbox is activated, this notice will no longer be shown when show()
     * is called.
     *
     * @return A key to store a "Dont show again" value in the Client Preferences
     */
    protected String preferenceKey() {
        return "";
    }

    /**
     * This method may be overridden to provide a width for the notice dialog. The default width is 500. The value is
     * internally scaled with the present GUI scaling.
     *
     * @return A width value for the dialog
     */
    protected int getWidth() {
        return DEFAULT_WIDTH;
    }

    private void initialize() {
        JEditorPane messagePane = new JEditorPane();
        messagePane.setContentType("text/html");
        messagePane.setEditable(false);
        String message = "<HTML><BODY WIDTH=%d>%s</BODY></HTML>".formatted(getWidth(), message());
        messagePane.setText(message);

        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(messagePane, BorderLayout.CENTER);

        if (usesPreference()) {
            JPanel checkboxPanel = new JPanel();
            checkboxPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
            checkboxPanel.add(dontShowAgain);
            contentPanel.add(checkboxPanel, BorderLayout.SOUTH);
        }
    }

    private boolean usesPreference() {
        return (preferenceKey() != null) && !preferenceKey().isBlank();
    }

    /**
     * Shows this notice dialog unless there is a Client Preferences entry for it that says it should not be shown.
     * In that case, this method returns without doing anything. Note that while the dialog is shown, BoardView
     * tooltips are suspended so they don't overlap the dialog.
     */
    public void show() {
        // Show the notice unless the preference key is used and has been stored previously, saying not to show it
        if (usesPreference() && preferences.hasProperty(preferenceKey()) && !preferences.getBoolean(preferenceKey())) {
            return;
        }

        if (!initialized) {
            initialized = true;
            initialize();
        }

        clientGui.suspendBoardTooltips();

        JOptionPane.showMessageDialog(clientGui.getFrame(), contentPanel, title(), JOptionPane.PLAIN_MESSAGE);
        if (usesPreference()) {
            preferences.setValue(preferenceKey(), !dontShowAgain.isSelected());
        }

        clientGui.activateBoardTooltips();
    }
}
