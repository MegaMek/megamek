/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.buttonDialogs;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;
import java.util.StringJoiner;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.TipTextField;
import megamek.common.util.UnitIdListParser;
import megamek.logging.MMLogger;
import org.apache.commons.text.StringEscapeUtils;

/** A dialog for entering one or more entity IDs that are to be used as strategic targets by Princess. */
public class BotConfigTargetUnitDialog extends AbstractButtonDialog {

    private static final MMLogger LOGGER = MMLogger.create(BotConfigTargetUnitDialog.class);

    private static final String OK_ACTION = "Ok_Action";

    /** How many IDs the preview spells out before it shortens the list with an ellipsis. */
    private static final int PREVIEW_ID_LIMIT = 12;

    private final TipTextField unitIDField = new TipTextField(5, "3-7, 10");
    private final JLabel unitIDLabel = new JLabel(Messages.getString("BotConfigDialog.unitIdLabel"));
    private final JLabel noteLabel = new JLabel("<HTML><CENTER>" + Messages.getString("BotConfigDialog.noteLabel"));
    private final JLabel previewLabel = new JLabel();

    protected BotConfigTargetUnitDialog(JFrame frame) {
        super(frame, "BotConfigTargetUnitDialog", "BotConfigDialog.bctudTitle");
        initialize();
        // Catch the Enter key as "OK"
        final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enter, OK_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(enter, OK_ACTION);
        getRootPane().getActionMap().put(OK_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                unitIDField.requestFocus();
            }
        });
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel unitIDPanel = new UIUtil.FixedYPanel();
        unitIDField.setToolTipText(Messages.getString("BotConfigDialog.unitIdTip"));
        unitIDLabel.setLabelFor(unitIDField);
        unitIDLabel.setDisplayedMnemonic(KeyEvent.VK_I);
        unitIDPanel.add(unitIDLabel);
        unitIDPanel.add(unitIDField);

        previewLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        unitIDField.getDocument().addDocumentListener(new PreviewUpdater());
        updatePreview();

        noteLabel.setBorder(new EmptyBorder(0, 20, 0, 20));
        noteLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        result.add(Box.createVerticalStrut(15));
        result.add(unitIDPanel);
        result.add(Box.createVerticalStrut(5));
        result.add(previewLabel);
        result.add(Box.createVerticalStrut(15));
        result.add(noteLabel);
        return result;
    }

    /**
     * Shows under the box which units the typed text will add, and anything in it that could not be read, so a typo
     * no longer quietly adds fewer units than the player meant.
     */
    private void updatePreview() {
        UnitIdListParser.Result result = UnitIdListParser.parse(unitIDField.getEnteredText());
        StringBuilder preview = new StringBuilder("<HTML><CENTER>");
        if (result.unitIds().isEmpty()) {
            preview.append(Messages.getString("BotConfigDialog.unitIdPreviewNone"));
        } else {
            preview.append(UIUtil.fontHTML(UIUtil.uiGreen()))
                  .append(Messages.getString("BotConfigDialog.unitIdPreview", result.unitIds().size(),
                        previewIdList(result.unitIds())))
                  .append("</FONT>");
        }
        if (!result.isFullyRead()) {
            preview.append("<BR>")
                  .append(UIUtil.fontHTML(GUIPreferences.getInstance().getWarningColor()))
                  // typed text goes into an HTML label: escape it so a stray < or & is shown, not interpreted
                  .append(Messages.getString("BotConfigDialog.unitIdUnreadable",
                        StringEscapeUtils.escapeHtml4(String.join(", ", result.unreadableEntries()))))
                  .append("</FONT>");
        }
        previewLabel.setText(preview.toString());
    }

    /**
     * @param unitIds the IDs to list, in ascending order
     *
     * @return the IDs separated by commas, cut short with an ellipsis after {@link #PREVIEW_ID_LIMIT} of them
     */
    private static String previewIdList(Set<Integer> unitIds) {
        StringJoiner listed = new StringJoiner(", ");
        int shown = 0;
        for (int unitId : unitIds) {
            if (shown == PREVIEW_ID_LIMIT) {
                listed.add("...");
                break;
            }
            listed.add(String.valueOf(unitId));
            shown++;
        }
        return listed.toString();
    }

    /**
     * Returns the entered unit IDs, reading single IDs and ranges such as {@code 3-7}. Entries that could not be read
     * are left out; the preview has already shown them to the player.
     *
     * @return the IDs entered; may be empty but never {@code null}
     */
    public Set<Integer> getSelectedIDs() {
        UnitIdListParser.Result result = UnitIdListParser.parse(unitIDField.getEnteredText());
        if (!result.isFullyRead()) {
            LOGGER.debug("[PriorityTargets] ignored unreadable unit ID entries {}", result.unreadableEntries());
        }
        LOGGER.debug("[PriorityTargets] unit ID box adds {}", result.unitIds());
        return result.unitIds();
    }

    /** Refreshes the preview on every keystroke in the ID box. */
    private class PreviewUpdater implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent event) {
            updatePreview();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            updatePreview();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            updatePreview();
        }
    }
}
