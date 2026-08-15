/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.clientGUI;

import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import megamek.MMConstants;
import megamek.client.ui.BugReportMessages;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.util.IssueReportUrl;

public class BugReportDialog {

    private static final int UNSCALED_WIDTH = 600;
    private static final BugReportMessages I18N = new BugReportMessages();

    private static final String REPORT_LINK_MM = "https://github.com/MegaMek/megamek/issues/new/choose";
    private static final String REPORT_LINK_MML = "https://github.com/MegaMek/megameklab/issues/new/choose";
    private static final String REPORT_LINK_MHQ = "https://github.com/MegaMek/mekhq/issues/new/choose";
    private static final String REPORT_LINK_MM_DATA = "https://github.com/MegaMek/mm-data/issues/new";

    private final Window parent;
    private final JComponent content;

    private final Action copySystemDataAction;
    private final Action packageBugReportAction;

    /**
     * Creates the dialog without a bug report packaging button.
     *
     * @param parent               the window to centre on, or {@code null}
     * @param copySystemDataAction shown as a button when not {@code null}
     */
    public BugReportDialog(@Nullable Window parent, @Nullable Action copySystemDataAction) {
        this(parent, copySystemDataAction, null);
    }

    /**
     * Creates the dialog.
     *
     * @param parent                 the window to centre on, or {@code null}
     * @param copySystemDataAction   shown as a button when not {@code null}
     * @param packageBugReportAction shown as a button when not {@code null}; collects the save and logs into a single
     *                               archive the player can attach to an issue
     */
    public BugReportDialog(@Nullable Window parent, @Nullable Action copySystemDataAction,
          @Nullable Action packageBugReportAction) {
        this.parent = parent;
        this.copySystemDataAction = copySystemDataAction;
        this.packageBugReportAction = packageBugReportAction;
        content = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        int width = UIUtil.scaleForGUI(UNSCALED_WIDTH);
        String firstText = "<html><body width=%d>%s</body></html>".formatted(width, I18N.get("mainText"));
        content.add(new JLabel(firstText), gbc);
        String secondText = "<html><body width=%d>%s</body></html>".formatted(width, I18N.get("secondaryText"));
        content.add(new JLabel(secondText), gbc);
        content.add(buttonPanel(), gbc);
    }

    public void show() {
        JOptionPane.showMessageDialog(parent, content, I18N.get("title"), JOptionPane.PLAIN_MESSAGE, null);
    }

    private JComponent buttonPanel() {
        JPanel row1 = new JPanel();
        row1.add(new UrlButton(I18N.get("discord.text"), MMConstants.DISCORD_LINK));

        // The three repositories that use the suite bug report template get their environment fields filled in for
        // the player; mm-data has no such template, so its link is left alone.
        JPanel row2 = new JPanel();
        row2.add(new UrlButton(I18N.get("mm.text"), IssueReportUrl.forIssueForm(REPORT_LINK_MM, null),
              REPORT_LINK_MM));
        row2.add(new UrlButton(I18N.get("mml.text"), IssueReportUrl.forIssueForm(REPORT_LINK_MML, null),
              REPORT_LINK_MML));
        row2.add(new UrlButton(I18N.get("mhq.text"), IssueReportUrl.forIssueForm(REPORT_LINK_MHQ, null),
              REPORT_LINK_MHQ));
        row2.add(new UrlButton(I18N.get("mmData.text"), REPORT_LINK_MM_DATA));

        JPanel row3 = new JPanel();
        if (packageBugReportAction != null) {
            row3.add(new JButton(packageBugReportAction));
        }
        if (copySystemDataAction != null) {
            row3.add(new JButton(copySystemDataAction));
        }

        JComponent rootPanel = new JPanel(new GridLayout(3, 1, 0, 8));
        rootPanel.add(row1);
        rootPanel.add(row2);
        rootPanel.add(row3);
        return rootPanel;
    }

    private static class UrlButton extends JButton {
        UrlButton(String text, String address) {
            this(text, address, address);
        }

        /**
         * @param text            the button label
         * @param address         the address opened when the button is pressed
         * @param displayedAddress the address shown in the tooltip; a prefilled issue-form URL carries a long query
         *                        string that is of no use to the reader, so the plain repository link is shown
         *                        instead
         */
        UrlButton(String text, String address, String displayedAddress) {
            super(text);
            setToolTipText(displayedAddress);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(event -> UIUtil.browse(address));
        }
    }
}
