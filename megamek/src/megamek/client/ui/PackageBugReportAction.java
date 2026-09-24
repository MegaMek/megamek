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

package megamek.client.ui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

import jakarta.annotation.Nullable;
import megamek.MMConstants;
import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.UIUtil;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.BugReportBundle;
import megamek.common.util.IssueReportUrl;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/**
 * Saves the current game, gathers the relevant logs, and writes them into a single zip archive the player can attach
 * to a GitHub issue.
 *
 * <p>The action is deliberately usable with no game running. A crash during start-up is one of the cases where a bug
 * report is most valuable and where {@code megamek.log} is the entire story, so a {@code null} client produces a
 * logs-and-system-information archive rather than an error.</p>
 *
 * <p>Saving in MegaMek is asynchronous - see {@link megamek.client.AbstractClient#setSaveCompletionCallback} - so the
 * archive cannot be built until the server has returned the save. This action therefore requests the save, waits for
 * the completion callback, and falls back to a logs-only archive if the save does not arrive within
 * {@link #SAVE_TIMEOUT_MILLISECONDS}. The player always ends up with a file.</p>
 *
 * @see BugReportBundle
 */
public class PackageBugReportAction extends AbstractAction {
    private static final MMLogger LOGGER = MMLogger.create(PackageBugReportAction.class);
    private static final BugReportMessages I18N = new BugReportMessages();

    /**
     * How long to wait for the server to return a save before giving up and packaging the logs alone.
     *
     * <p>A wait is genuinely needed because the save round-trips through the server, but it must be bounded: the
     * server refuses local saves outright in a double-blind game with local saving disabled, and in that case it
     * replies with a chat message and never sends a save at all.</p>
     */
    private static final int SAVE_TIMEOUT_MILLISECONDS = 30_000;

    private static final String ARCHIVE_EXTENSION = ".zip";
    private static final String DEFAULT_ARCHIVE_NAME = "MegaMek-BugReport.zip";

    /** Width of the result message before the user's GUI scale is applied, matching {@code BugReportDialog}. */
    private static final int UNSCALED_MESSAGE_WIDTH = 420;

    private final Component parent;
    private final Supplier<Client> clientSupplier;

    /**
     * Creates the action.
     *
     * @param parent         the component to parent the file chooser and result dialogs to, or {@code null} to centre
     *                       them on screen
     * @param clientSupplier supplies the client whose game should be saved, resolved when the action is invoked
     *                       rather than when it is created, because the menu bar is built before any game exists. May
     *                       be {@code null}, and may return {@code null}; both mean "no game running".
     */
    public PackageBugReportAction(@Nullable Component parent, @Nullable Supplier<Client> clientSupplier) {
        super(I18N.get("package.text"));
        this.parent = parent;
        this.clientSupplier = clientSupplier;
        putValue(AbstractAction.SHORT_DESCRIPTION, I18N.get("package.tooltip"));
    }

    /**
     * Copies this action, matching the behaviour of the other actions in the suite.
     *
     * <p>Both fields are immutable references that the copy is meant to share - the same parent window and the same
     * client supplier - so the shallow copy {@link Object#clone()} makes is exactly what is wanted here.</p>
     *
     * @return the copy, or {@code null} if it could not be made
     */
    @Override
    public @Nullable PackageBugReportAction clone() {
        try {
            return (PackageBugReportAction) super.clone();
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            LOGGER.error("Failed to clone PackageBugReportAction. State of the object: {}", this,
                  cloneNotSupportedException);
            return null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        File archiveFile = chooseArchiveFile();
        if (archiveFile == null) {
            LOGGER.debug("[BugReport] Player cancelled the archive location chooser");
            return;
        }

        Client client = (clientSupplier == null) ? null : clientSupplier.get();
        if ((client == null) || !client.isConnected()) {
            LOGGER.info("[BugReport] No connected game; packaging logs and system information only");
            buildArchive(archiveFile, null, I18N.get("package.noGame"));
            return;
        }

        requestSaveThenBuildArchive(client, archiveFile);
    }

    /**
     * Asks the player where the archive should go.
     *
     * @return the chosen file, with a {@code .zip} extension guaranteed, or {@code null} if the player cancelled
     */
    private @Nullable File chooseArchiveFile() {
        File saveGameDirectory = new File(MMConstants.SAVEGAME_DIR);
        JFileChooser fileChooser = new JFileChooser(saveGameDirectory);
        fileChooser.setDialogTitle(I18N.get("package.chooser.title"));
        fileChooser.setFileFilter(new FileNameExtensionFilter(I18N.get("package.chooser.filter"), "zip"));
        fileChooser.setSelectedFile(new File(saveGameDirectory, StringUtil.addDateTimeStamp(DEFAULT_ARCHIVE_NAME)));

        if (fileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File chosenFile = fileChooser.getSelectedFile();
        if (chosenFile == null) {
            return null;
        }
        if (!chosenFile.getName().toLowerCase(Locale.ROOT).endsWith(ARCHIVE_EXTENSION)) {
            chosenFile = new File(chosenFile.getParentFile(), chosenFile.getName() + ARCHIVE_EXTENSION);
        }
        // Absolute, so the containing directory is always resolvable; a relative choice would leave
        // getParentFile() null and the save request would have nowhere to point.
        return chosenFile.getAbsoluteFile();
    }

    /**
     * Requests a local save and builds the archive once it arrives, or once waiting for it has been given up on.
     *
     * <p>The save is written beside the chosen archive rather than into the player's save folder, so a bug report
     * never leaves a stray save behind among their real ones.</p>
     *
     * @param client      the connected client to save through
     * @param archiveFile the archive to write
     */
    private void requestSaveThenBuildArchive(Client client, File archiveFile) {
        File archiveDirectory = archiveFile.getParentFile();
        String temporarySaveName = temporarySaveNameFor(archiveFile);

        // Whichever of the callback and the timeout arrives first wins; the other must become a no-op.
        AtomicBoolean alreadyProceeded = new AtomicBoolean(false);

        Timer timeoutTimer = new Timer(SAVE_TIMEOUT_MILLISECONDS, timeoutEvent -> {
            if (alreadyProceeded.compareAndSet(false, true)) {
                LOGGER.warn("[BugReport] No save returned within {} ms; packaging logs only. "
                      + "The server refuses local saves in a double-blind game when local saving is disabled.",
                      SAVE_TIMEOUT_MILLISECONDS);
                client.setSaveCompletionCallback(null);
                client.setAwaitingSave(false);
                buildArchive(archiveFile, null, I18N.get("package.saveTimedOut"));
            }
        });
        timeoutTimer.setRepeats(false);

        client.setSaveCompletionCallback(savedFile -> SwingUtilities.invokeLater(() -> {
            if (alreadyProceeded.compareAndSet(false, true)) {
                timeoutTimer.stop();
                if (savedFile == null) {
                    LOGGER.warn("[BugReport] The save could not be written; packaging logs only");
                    buildArchive(archiveFile, null, I18N.get("package.saveFailed"));
                } else {
                    LOGGER.info("[BugReport] Save returned as {}; building archive", savedFile.getName());
                    buildArchive(archiveFile, savedFile, null);
                }
            }
        }));

        // The server splits this command on spaces, so the path is sent with spaces escaped as pipes; see
        // GameManagerSaveHelper, which reverses it. Sending an unescaped path silently saves to the wrong place.
        String escapedDirectory = archiveDirectory.getPath().replace(" ", "|");
        LOGGER.debug("[BugReport] Requesting local save '{}' into {}", temporarySaveName, archiveDirectory.getPath());
        client.sendChat(ClientGUI.CG_CHAT_COMMAND_LOCAL_SAVE + " " + temporarySaveName + " " + escapedDirectory);
        client.setAwaitingSave(true);
        timeoutTimer.start();
    }

    /**
     * Writes the archive on a background thread and reports the outcome.
     *
     * <p>Zipping a large log directory takes long enough to freeze the interface if done on the event dispatch
     * thread, so the work happens on a {@link SwingWorker}.</p>
     *
     * @param archiveFile   the archive to write
     * @param saveGameFile  the save to include, or {@code null} for a logs-only archive
     * @param cautionNotice a note explaining why the save is missing, shown with the result, or {@code null} when
     *                      nothing went wrong
     */
    private void buildArchive(File archiveFile, @Nullable File saveGameFile, @Nullable String cautionNotice) {
        ClientPreferences clientPreferences = PreferenceManager.getClientPreferences();
        File logDirectory = new File(clientPreferences.getLogDirectory());
        String systemInformation = MegaMek.getUnderlyingInformation(MegaMek.getOriginProject(),
              MMConstants.PROJECT_NAME);
        BugReportBundle bundle = new BugReportBundle(logDirectory, saveGameFile, systemInformation,
              clientPreferences.getGameLogFilename());

        new SwingWorker<BugReportBundle.BundleResult, Void>() {
            @Override
            protected BugReportBundle.BundleResult doInBackground() throws IOException {
                return bundle.writeTo(archiveFile);
            }

            @Override
            protected void done() {
                // The temporary save has been copied into the archive, so the loose copy is no longer wanted. On
                // failure it is deliberately left behind, so the player still has something to attach.
                try {
                    BugReportBundle.BundleResult result = get();
                    deleteTemporarySave(saveGameFile);
                    showResult(result, cautionNotice);
                } catch (Exception exception) {
                    LOGGER.error(exception, "[BugReport] Could not write the archive to {}", archiveFile.getPath());
                    JOptionPane.showMessageDialog(parent, I18N.get("package.failed"), I18N.get("package.result.title"),
                          JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Removes the save written purely to be archived.
     *
     * @param saveGameFile the temporary save, or {@code null} if none was made
     */
    private static void deleteTemporarySave(@Nullable File saveGameFile) {
        if ((saveGameFile == null) || !saveGameFile.isFile()) {
            return;
        }
        if (!saveGameFile.delete()) {
            LOGGER.warn("[BugReport] Could not delete the temporary save {}", saveGameFile.getName());
        }
    }

    /**
     * Tells the player what was collected and offers to open the folder or put the archive on the clipboard.
     *
     * @param result        what the bundle wrote
     * @param cautionNotice a note about anything that did not go to plan, or {@code null}
     */
    private void showResult(BugReportBundle.BundleResult result, @Nullable String cautionNotice) {
        int messageWidth = UIUtil.scaleForGUI(UNSCALED_MESSAGE_WIDTH);
        StringBuilder message = new StringBuilder("<html><body width=%d>".formatted(messageWidth));
        if (cautionNotice != null) {
            message.append("<p><b>").append(cautionNotice).append("</b></p>");
        }
        message.append("<p>").append(I18N.get("package.result.included", result.archiveFile().getPath()))
              .append("</p><ul>");
        for (String entryName : result.includedEntries()) {
            message.append("<li>").append(entryName).append("</li>");
        }
        message.append("</ul>");
        if (result.hasSkippedEntries()) {
            message.append("<p>").append(I18N.get("package.result.skipped")).append("</p><ul>");
            for (String entryName : result.skippedEntries()) {
                message.append("<li>").append(entryName).append("</li>");
            }
            message.append("</ul>");
        }
        message.append("<p>").append(I18N.get("package.result.next")).append("</p>");
        message.append("</body></html>");

        Object[] options = { I18N.get("mm.text"), I18N.get("package.result.copyFile"),
                             I18N.get("package.result.close") };
        int choice = JOptionPane.showOptionDialog(parent, message.toString(), I18N.get("package.result.title"),
              JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            openIssueForm(result.archiveFile());
        } else if (choice == 1) {
            copyToClipboard(result.archiveFile());
        }
    }

    /**
     * Opens the MegaMek issue form, with the archive already on the clipboard.
     *
     * <p>The archive is copied without being asked for because this button closes the dialog, which would otherwise
     * leave the player at the GitHub upload box with the file still sitting in a folder they would have to go and
     * find. Pasting is then the whole of the remaining work.</p>
     *
     * @param archiveFile the archive the player has just built
     */
    private static void openIssueForm(File archiveFile) {
        copyToClipboard(archiveFile);
        String issueFormUrl = IssueReportUrl.forIssueForm(IssueReportUrl.MEGAMEK_ISSUES_URL);
        LOGGER.info("[BugReport] Opening the issue form for {}", archiveFile.getName());
        UIUtil.browse(issueFormUrl);
    }

    /**
     * Puts the archive on the system clipboard as a file, so it can be pasted straight into a GitHub upload box
     * instead of being hunted for in a file dialog.
     *
     * @param archiveFile the archive to copy
     */
    private static void copyToClipboard(File archiveFile) {
        Transferable fileTransferable = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { DataFlavor.javaFileListFlavor };
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.javaFileListFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) {
                return List.of(archiveFile);
            }
        };

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(fileTransferable, null);
            LOGGER.debug("[BugReport] Copied {} to the clipboard", archiveFile.getName());
        } catch (Exception exception) {
            LOGGER.error(exception, "[BugReport] Could not copy {} to the clipboard", archiveFile.getName());
        }
    }

    /**
     * Derives the temporary save's name from the archive's name.
     *
     * <p>The server splits the {@code /localsave} chat command on spaces, so a name containing one would be read as
     * two separate arguments and the save written somewhere unintended - the player is free to type
     * "Bug Report.zip" into the file chooser. The directory is escaped and unescaped either side of the wire, but
     * the filename has no such mechanism, so whitespace is replaced outright.</p>
     *
     * @param archiveFile the archive the player chose
     *
     * @return a save name safe to send as a single chat-command argument
     */
    private static String temporarySaveNameFor(File archiveFile) {
        return stripExtension(archiveFile.getName()).replaceAll("\\s+", "_");
    }

    /**
     * @param fileName a file name, with or without an extension
     *
     * @return the name with any trailing extension removed
     */
    private static String stripExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        return (extensionIndex > 0) ? fileName.substring(0, extensionIndex) : fileName;
    }
}
