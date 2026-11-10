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
package megamek.client.ui.dialogs.releaseNotes;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;

import jakarta.annotation.Nullable;
import megamek.SuiteConstants;
import megamek.Version;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.util.UIUtil;
import megamek.logging.MMLogger;

/**
 * Shows what changed in the running version of a MegaMek suite application, from Help &gt; Release Notes: the
 * published release notes on GitHub, grouped by kind of change, of every release of the running version's channel
 * since the version the player ran before. It only opens when the player asks for it.
 *
 * <p>The dialog is not modal and the notes load in the background, so a slow or refused GitHub request never holds
 * up the application.</p>
 */
public final class ReleaseNotesDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 4618843517362904911L;
    private static final MMLogger LOGGER = MMLogger.create(ReleaseNotesDialog.class);
    private static final String GITHUB = "https://github.com/MegaMek/";

    private final JEditorPane notes = new JEditorPane("text/html", "");
    private final JLabel since = new JLabel();
    private URI page;
    private transient SwingWorker<ReleaseNotes, Void> worker;

    private ReleaseNotesDialog(@Nullable Window owner, String repository, Version version) {
        super(owner, Messages.getString("ReleaseNotesDialog.title"), ModalityType.MODELESS);
        setName("releaseNotesDialog");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        page = ReleaseNotes.page(repository, version);

        JLabel heading = new JLabel(Messages.getString("ReleaseNotesDialog.heading", productName(repository),
              version.toString()));
        heading.setName("releaseNotesHeading");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D() * 1.4f));
        since.setName("releaseNotesSince");
        since.setVisible(false);
        JPanel header = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(4)));
        header.add(heading, BorderLayout.NORTH);
        header.add(since, BorderLayout.SOUTH);

        notes.setName("releaseNotesContent");
        notes.setEditable(false);
        notes.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        notes.setText(page(paragraph(Messages.getString("ReleaseNotesDialog.loading"))));
        notes.addHyperlinkListener(event -> {
            if ((event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) && isSafe(event.getDescription())) {
                UIUtil.browse(event.getDescription(), this);
            }
        });
        JScrollPane scroll = new JScrollPane(notes);
        scroll.setName("releaseNotesScroll");
        scroll.setPreferredSize(UIUtil.scaleForGUI(620, 440));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIUtil.scaleForGUI(8), 0));
        buttons.add(new MMButton("releaseNotesViewOnGitHub", Messages.getString("ReleaseNotesDialog.viewOnGitHub"),
              event -> UIUtil.browse(page.toString(), this)));
        MMButton close = new MMButton("releaseNotesClose", Messages.getString("ReleaseNotesDialog.close"),
              event -> dispose());
        buttons.add(close);

        int pad = UIUtil.scaleForGUI(12);
        JPanel content = new JPanel(new BorderLayout(0, pad));
        content.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
        content.add(header, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(close);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Opens what changed in the running version since the version the player ran before it, within the same release
     * channel, and loads it in the background, as Help &gt; Release Notes does.
     *
     * @param owner      The window to center on, or {@code null}
     * @param repository The application's repository name, e.g. "megamek"
     */
    public static void showReleaseNotes(@Nullable Window owner, String repository) {
        Version previous = ReleaseNotes.previousVersion();
        ReleaseNotesDialog dialog = new ReleaseNotesDialog(owner, repository, SuiteConstants.VERSION);
        dialog.worker = new SwingWorker<>() {
            @Override
            protected ReleaseNotes doInBackground() throws Exception {
                return ReleaseNotes.load(repository, previous, SuiteConstants.VERSION);
            }

            @Override
            protected void done() {
                if (isCancelled() || !dialog.isDisplayable()) {
                    return;
                }
                try {
                    dialog.showNotes(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failure) {
                    LOGGER.warn("Could not load the release notes: {}", failure.getCause().toString());
                    dialog.showText(failedHtml(ReleaseNotes.page(repository, SuiteConstants.VERSION)));
                }
            }
        };
        dialog.worker.execute();
        dialog.setVisible(true);
    }

    @Override
    public void dispose() {
        if (worker != null) {
            worker.cancel(true);
        }
        super.dispose();
    }

    private void showNotes(ReleaseNotes releaseNotes) {
        page = releaseNotes.page();
        if (releaseNotes.since() != null) {
            since.setText(Messages.getString("ReleaseNotesDialog.since", releaseNotes.since()));
            since.setVisible(true);
            getContentPane().revalidate();
        }
        showText(html(releaseNotes));
    }

    private void showText(String html) {
        notes.setText(html);
        notes.setCaretPosition(0);
    }

    /** The notes as HTML for the notes pane. */
    static String html(ReleaseNotes releaseNotes) {
        if (releaseNotes.sections().isEmpty()) {
            return page(paragraph(Messages.getString("ReleaseNotesDialog.empty")));
        }
        StringBuilder html = new StringBuilder();
        for (ReleaseNotes.Section section : releaseNotes.sections()) {
            String heading = ReleaseNotes.UNGROUPED.equals(section.heading())
                  ? Messages.getString("ReleaseNotesDialog.changes") : section.heading();
            html.append("<h3>").append(escape(heading)).append("</h3><ul>");
            for (ReleaseNotes.Change change : section.changes()) {
                html.append("<li>").append(escape(change.title()));
                if (change.link() != null) {
                    html.append(" <a href='").append(escape(change.link().toString())).append("'>#")
                          .append(change.pullRequest()).append("</a>");
                }
                html.append("</li>");
            }
            html.append("</ul>");
        }
        return page(html.toString());
    }

    static String failedHtml(URI page) {
        return page(paragraph(Messages.getString("ReleaseNotesDialog.failed")) + "<p><a href='"
              + escape(page.toString()) + "'>" + escape(Messages.getString("ReleaseNotesDialog.viewOnGitHub"))
              + "</a></p>");
    }

    static String productName(String repository) {
        return switch (repository) {
            case "megameklab" -> "MegaMekLab";
            case "mekhq" -> "MekHQ";
            default -> "MegaMek";
        };
    }

    /** Only pages of the MegaMek organization on GitHub are opened. */
    static boolean isSafe(@Nullable String link) {
        return (link != null) && link.startsWith(GITHUB) && !link.contains("..") && !link.contains("@");
    }

    private static String page(String body) {
        return "<html><body>" + body + "</body></html>";
    }

    private static String paragraph(String text) {
        return "<p>" + escape(text) + "</p>";
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
              .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
