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
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
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
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.LauncherReadySignal;
import megamek.client.ui.util.UIUtil;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

/**
 * Shows what changed in the running version of a MegaMek suite application: its published release notes on GitHub,
 * grouped by kind of change. It opens from Help &gt; Release Notes, and once on the first start after an update unless
 * the player turned that off or the MegaMek Launcher already showed the changes.
 *
 * <p>The dialog is not modal and the notes load in the background, so a slow or refused GitHub request never holds
 * up the application.</p>
 */
public final class ReleaseNotesDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 4618843517362904911L;
    private static final MMLogger LOGGER = MMLogger.create(ReleaseNotesDialog.class);
    private static final String GITHUB = "https://github.com/MegaMek/";
    private static final Pattern SEEN_VERSION = Pattern.compile("\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}(\\.\\d{1,6})?");

    private final JEditorPane notes = new JEditorPane("text/html", "");
    private transient SwingWorker<ReleaseNotes, Void> worker;

    /** What the first start after an update does. */
    enum AfterUpdate {
        /** Load the notes and show them; remember the version once they are shown. */
        SHOW,
        /** Show nothing, but remember the version so it is not offered again. */
        REMEMBER,
        /** Show and remember nothing. */
        NOTHING
    }

    private ReleaseNotesDialog(@Nullable Window owner, String repository, Version version) {
        super(owner, Messages.getString("ReleaseNotesDialog.title"), ModalityType.MODELESS);
        setName("releaseNotesDialog");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        URI page = ReleaseNotes.page(repository, version);

        JLabel heading = new JLabel(Messages.getString("ReleaseNotesDialog.heading", productName(repository),
              version.toString()));
        heading.setName("releaseNotesHeading");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D() * 1.4f));

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

        JCheckBox showAfterUpdates = new JCheckBox(Messages.getString("ReleaseNotesDialog.showAfterUpdates"));
        showAfterUpdates.setName("releaseNotesShowAfterUpdates");
        showAfterUpdates.setToolTipText(Messages.getString("ReleaseNotesDialog.showAfterUpdates.tooltip"));
        showAfterUpdates.setSelected(GUIPreferences.getInstance().getShowReleaseNotesAfterUpdate());
        showAfterUpdates.addActionListener(event -> {
            GUIPreferences.getInstance().setShowReleaseNotesAfterUpdate(showAfterUpdates.isSelected());
            PreferenceManager.getInstance().save();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIUtil.scaleForGUI(8), 0));
        buttons.add(new MMButton("releaseNotesViewOnGitHub", Messages.getString("ReleaseNotesDialog.viewOnGitHub"),
              event -> UIUtil.browse(page.toString(), this)));
        MMButton close = new MMButton("releaseNotesClose", Messages.getString("ReleaseNotesDialog.close"),
              event -> dispose());
        buttons.add(close);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(showAfterUpdates, BorderLayout.WEST);
        footer.add(buttons, BorderLayout.EAST);

        int pad = UIUtil.scaleForGUI(12);
        JPanel content = new JPanel(new BorderLayout(0, pad));
        content.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
        content.add(heading, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(close);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Opens the release notes of the running version and loads them in the background, as Help &gt; Release Notes
     * does.
     *
     * @param owner      The window to center on, or null
     * @param repository The application's repository name, e.g. "megamek"
     */
    public static void showReleaseNotes(@Nullable Window owner, String repository) {
        ReleaseNotesDialog dialog = new ReleaseNotesDialog(owner, repository, SuiteConstants.VERSION);
        dialog.worker = new SwingWorker<>() {
            @Override
            protected ReleaseNotes doInBackground() throws Exception {
                return ReleaseNotes.load(repository, SuiteConstants.VERSION);
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

    /**
     * Shows the release notes once on the first start after an update, if the player wants that and the MegaMek
     * Launcher has not already shown them. The notes load in the background; when they cannot be loaded, nothing is
     * shown and the next start tries again. Call this once the application's first window is on screen.
     *
     * @param owner      The window to center on, or null
     * @param repository The application's repository name, e.g. "megamek"
     */
    public static void showAfterUpdate(@Nullable Window owner, String repository) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        GUIPreferences preferences = GUIPreferences.getInstance();
        Version version = SuiteConstants.VERSION;
        String launcherReadyFile = System.getProperty(LauncherReadySignal.READY_FILE_PROPERTY);
        AfterUpdate decision = afterUpdate(preferences.getReleaseNotesSeen(repository), version,
              (launcherReadyFile != null) && !launcherReadyFile.isBlank(),
              preferences.getShowReleaseNotesAfterUpdate());
        if (decision == AfterUpdate.REMEMBER) {
            remember(repository, version);
        } else if (decision == AfterUpdate.SHOW) {
            new SwingWorker<ReleaseNotes, Void>() {
                @Override
                protected ReleaseNotes doInBackground() throws Exception {
                    return ReleaseNotes.load(repository, version);
                }

                @Override
                protected void done() {
                    try {
                        ReleaseNotes loaded = get();
                        remember(repository, version);
                        ReleaseNotesDialog dialog = new ReleaseNotesDialog(owner, repository, version);
                        dialog.showNotes(loaded);
                        dialog.setVisible(true);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException failure) {
                        LOGGER.info("Skipped the release notes after the update: {}", failure.getCause().toString());
                    }
                }
            }.execute();
        }
    }

    /**
     * Decides what the first start of {@code current} does, given the version the notes were last shown or skipped
     * for.
     *
     * @param seen         The remembered version, or an empty string when there is none
     * @param current      The running version
     * @param fromLauncher Whether the MegaMek Launcher started the application; it shows the notes after its updates
     * @param wanted       Whether the player wants the notes after updates
     *
     * @return What to do
     */
    static AfterUpdate afterUpdate(@Nullable String seen, Version current, boolean fromLauncher, boolean wanted) {
        if (!current.hasRevision()) {
            // A development build has no release notes
            return AfterUpdate.NOTHING;
        }
        if ((seen == null) || !SEEN_VERSION.matcher(seen).matches()) {
            // A new installation, with nothing it was updated from, or an unreadable setting
            return AfterUpdate.REMEMBER;
        }
        Version previous = new Version(seen);
        if (!current.isHigherThan(previous) || fromLauncher || !wanted) {
            return current.is(previous) ? AfterUpdate.NOTHING : AfterUpdate.REMEMBER;
        }
        return AfterUpdate.SHOW;
    }

    private static void remember(String repository, Version version) {
        GUIPreferences.getInstance().setReleaseNotesSeen(repository, version.toString());
        PreferenceManager.getInstance().save();
    }

    @Override
    public void dispose() {
        if (worker != null) {
            worker.cancel(true);
        }
        super.dispose();
    }

    private void showNotes(ReleaseNotes releaseNotes) {
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
