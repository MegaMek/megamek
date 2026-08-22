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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.function.Supplier;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import megamek.MMConstants;
import megamek.client.Client;
import megamek.client.ui.BugReportMessages;
import megamek.client.ui.CopySystemDataAction;
import megamek.client.ui.PackageBugReportAction;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.HazardStripeBorder;
import megamek.common.annotations.Nullable;
import megamek.common.util.IssueReportUrl;

public class BugReportDialog {

    private static final int UNSCALED_WIDTH = 600;
    private static final BugReportMessages I18N = new BugReportMessages();

    /** Breathing room between the striped border and the button text, in unscaled pixels. */
    private static final int HAZARD_PADDING = 4;

    private static final int UNSCALED_REPORT_BUTTON_WIDTH = 260;
    private static final int UNSCALED_REPORT_BUTTON_HEIGHT = 48;

    /** How much larger the reporting button's text is than the text on the buttons around it. */
    private static final float REPORT_BUTTON_FONT_FACTOR = 1.4f;

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

    /**
     * Opens the helper with the full set of gathering tools, for the places in the program that are reached while a
     * game may be running.
     *
     * @param parent         the window to centre on, or {@code null}
     * @param clientSupplier supplies the client whose game should be packaged, consulted when the player presses the
     *                       button rather than now; may return {@code null}, which packages the logs alone
     */
    public static void showWithGameTools(@Nullable Window parent, Supplier<Client> clientSupplier) {
        new BugReportDialog(parent, new CopySystemDataAction(),
              new PackageBugReportAction(parent, clientSupplier)).show();
    }

    /**
     * Creates the button that does the actual work, sized and marked so that it reads as the main thing to press.
     *
     * <p>It shares the dialog with nine other buttons, all of which merely open a link, so at the default size it
     * looked like one option among many rather than the step the instructions above it begin with.</p>
     *
     * @return the reporting button
     */
    private JButton createReportButton() {
        JButton reportButton = new JButton(packageBugReportAction);
        int padding = UIUtil.scaleForGUI(HAZARD_PADDING);
        reportButton.setBorder(BorderFactory.createCompoundBorder(new HazardStripeBorder(),
              BorderFactory.createEmptyBorder(padding, padding, padding, padding)));
        reportButton.setFont(reportButton.getFont().deriveFont(Font.BOLD,
              reportButton.getFont().getSize2D() * REPORT_BUTTON_FONT_FACTOR));
        reportButton.setPreferredSize(new Dimension(UIUtil.scaleForGUI(UNSCALED_REPORT_BUTTON_WIDTH),
              UIUtil.scaleForGUI(UNSCALED_REPORT_BUTTON_HEIGHT)));
        return reportButton;
    }

    /**
     * @param labelKey  the message key holding the button's label
     * @param issuesUrl the repository's plain issue link, shown in the tooltip
     *
     * @return a button opening that repository's issue form with the environment fields already filled in
     */
    private static UrlButton prefilledIssueButton(String labelKey, String issuesUrl) {
        return new UrlButton(I18N.get(labelKey), IssueReportUrl.forIssueForm(issuesUrl), issuesUrl);
    }

    /**
     * Lays the buttons out in the order the instructions above them ask the player to use: gather the files first,
     * then go to the repository the problem belongs to.
     */
    private JComponent buttonPanel() {
        JPanel discordRow = new JPanel();
        discordRow.add(new UrlButton(I18N.get("discord.text"), MMConstants.DISCORD_LINK));

        JPanel gatherFilesRow = new JPanel();
        if (packageBugReportAction != null) {
            gatherFilesRow.add(createReportButton());
        } else if (copySystemDataAction != null) {
            // Only worth offering where nothing gathers the files automatically: the archive carries a
            // system-info.txt and the repository buttons fill the same details into the issue form. MegaMekLab and
            // MekHQ reach this dialog without a packaging action, and the Help menu keeps the item in its own right.
            gatherFilesRow.add(new JButton(copySystemDataAction));
        }

        // The three repositories that use the suite bug report template get their environment fields filled in for
        // the player; mm-data has no such template, so its link is left alone.
        JPanel repositoryRow = new JPanel();
        repositoryRow.add(prefilledIssueButton("mm.text", IssueReportUrl.MEGAMEK_ISSUES_URL));
        repositoryRow.add(prefilledIssueButton("mml.text", IssueReportUrl.MEGAMEKLAB_ISSUES_URL));
        repositoryRow.add(prefilledIssueButton("mhq.text", IssueReportUrl.MEKHQ_ISSUES_URL));
        repositoryRow.add(new UrlButton(I18N.get("mmData.text"), REPORT_LINK_MM_DATA));

        JComponent rootPanel = new JPanel(new GridLayout(3, 1, 0, 8));
        rootPanel.add(discordRow);
        rootPanel.add(gatherFilesRow);
        rootPanel.add(repositoryRow);
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
