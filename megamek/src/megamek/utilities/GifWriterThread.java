/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.utilities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import megamek.client.ui.Messages;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/**
 * Thread that writes frames to a GIF file.
 *
 * @author Luana Coppio
 */
public class GifWriterThread extends Thread {
    private static final MMLogger LOGGER = MMLogger.create(GifWriter.class);

    private record Frame(BufferedImage image, long duration) {
    }

    /** Caps queued frames so a slow encoder applies backpressure instead of growing memory without bound. */
    private static final int MAX_PENDING_FRAMES = 32;

    private final GifWriter gifWriter;
    private final BlockingQueue<Frame> frameQueue = new LinkedBlockingQueue<>(MAX_PENDING_FRAMES);
    private volatile boolean isLive = true;
    private volatile boolean forceInterrupt = false;
    public static final String CG_FILE_EXTENSION_GIF = ".gif";
    public static final String CG_FILE_PATH_GIF = "gif";

    /**
     * Creates a new GifWriterThread.
     *
     * @param gifWriter the GIF writer
     * @param name      the thread name
     */
    public GifWriterThread(GifWriter gifWriter, String name) {
        super(name);
        this.gifWriter = gifWriter;
    }

    /**
     * Adds a frame to the GIF.
     *
     * @param image          the frame image
     * @param durationMillis the frame duration in milliseconds
     */
    public void addFrame(BufferedImage image, long durationMillis) {
        if (!isLive) {
            return;
        }
        try {
            // Blocks only if the encoder has fallen MAX_PENDING_FRAMES behind; normally returns immediately.
            // Crucially, the producer no longer waits on a lock the encoder holds during a frame.
            frameQueue.put(new Frame(image, durationMillis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        try {
            while (isLive) {
                // take() blocks without holding any lock, so the encode below never stalls addFrame().
                Frame frame = frameQueue.take();
                gifWriter.appendFrame(frame.image(), frame.duration());
            }
        } catch (InterruptedException interruptedException) {
            // Stop requested while waiting for a frame. Nothing to restore: this thread ends below.
            LOGGER.debug("GIF writer interrupted while waiting for a frame");
        } catch (IOException e) {
            LOGGER.error(e, "Error appending frame to GIF");
        } finally {
            // stopThread() interrupts this thread. A modal dialog shown while the interrupt flag is set
            // closes at once and returns CLOSED_OPTION, so the GIF is deleted before the player can answer.
            Thread.interrupted();
            if (!forceInterrupt) {
                encodeQueuedFrames();
            }
            gifWriter.close();
            frameQueue.clear();
            if (!forceInterrupt) {
                try {
                    saveGifNag();
                } catch (Exception e) {
                    LOGGER.error(e, "Error deleting gif or opening JOptionPane");
                }
            }
            isLive = false;
            forceInterrupt = false;
        }
    }

    /**
     * Encodes the frames still waiting in the queue when a normal stop arrives, so the last turns of the game
     * are not cut from the GIF. The final frames are queued just before the victory phase stops this thread.
     */
    private void encodeQueuedFrames() {
        int framesEncoded = 0;
        Frame frame = frameQueue.poll();
        while (frame != null) {
            try {
                gifWriter.appendFrame(frame.image(), frame.duration());
                framesEncoded++;
            } catch (IOException ioException) {
                LOGGER.error(ioException, "Error appending a queued frame to GIF");
                return;
            }
            frame = frameQueue.poll();
        }
        LOGGER.info("Encoded {} queued frames before closing the GIF", framesEncoded);
    }

    /**
     * Asks the player whether to keep the recorded GIF, saves it if they say yes, and then deletes the
     * temporary file. Package-private so tests can observe the thread state it runs under.
     */
    void saveGifNag() {
        // Recording only ever runs with the player's up-front consent (see GifRecordingMode), so this dialog
        // needs no preference gate: the player chose to record and now only decides whether to keep the result.
        int response = JOptionPane.showConfirmDialog(null,
              Messages.getString("ClientGUI.SaveGifDialog.message"),
              Messages.getString("ClientGUI.SaveGifDialog.title"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.INFORMATION_MESSAGE);
        LOGGER.info("Save GIF prompt answered: {}", describeResponse(response));
        if (response == JOptionPane.YES_OPTION) {
            saveGif();
        }
        deleteGif();
    }

    private void saveGif() {
        SaveDialogResult result = getSaveDialog();
        if ((result.returnVal() != JFileChooser.APPROVE_OPTION) || (result.saveDialog().getSelectedFile() == null)) {
            // without a file there is no saving for the file, which them means we can't save the gif
            LOGGER.warn("No file selected for saving GIF, skipping save operation.");
            return;
        }

        // Did the player select a file?
        File gifFile = result.saveDialog().getSelectedFile();
        if (gifFile != null) {
            if (!gifFile.getName().toLowerCase().endsWith(CG_FILE_EXTENSION_GIF)) {
                try {
                    gifFile = new File(gifFile.getCanonicalPath() + CG_FILE_EXTENSION_GIF);
                } catch (Exception ex) {
                    // without a file there is no saving for the file, which them means we can't save the gif
                    LOGGER.errorDialog(
                          ex, "Unable to get canonical path for file {}", "Error when trying to save GIF",
                          gifFile);
                    return;
                }
            }
            File finalGifFile = gifFile;
            try {
                // Files.move copies across drives and replaces a file the player chose to overwrite;
                // File.renameTo fails silently in both cases.
                Files.move(gifWriter.getOutputFile().toPath(), finalGifFile.toPath(),
                      StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Game summary GIF saved to {}", finalGifFile);
            } catch (Exception ex) {
                LOGGER.errorDialog(ex,
                      "Unable to save file {} at {}",
                      "Unable to save GIF in destination",
                      gifWriter.getOutputFile(),
                      finalGifFile);
            }
        }
    }

    private static SaveDialogResult getSaveDialog() {
        String filename = StringUtil.addDateTimeStamp("combat_summary_");
        JFileChooser saveDialog = new JFileChooser(".");
        var frame = JOptionPane.getRootFrame();
        saveDialog.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        saveDialog.setDialogTitle(Messages.getString("ClientGUI.saveGameSummaryGifFileDialog.title"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(Messages.getString("ClientGUI.descriptionGIFFiles"),
              CG_FILE_PATH_GIF);
        saveDialog.setFileFilter(filter);

        saveDialog.setSelectedFile(new File(filename + CG_FILE_EXTENSION_GIF));

        int returnVal = saveDialog.showSaveDialog(frame);
        return new SaveDialogResult(saveDialog, returnVal);
    }

    private record SaveDialogResult(JFileChooser saveDialog, int returnVal) {
    }

    private static String describeResponse(int response) {
        return switch (response) {
            case JOptionPane.YES_OPTION -> "yes";
            case JOptionPane.NO_OPTION -> "no";
            default -> "closed without an answer";
        };
    }

    private void deleteGif() {
        if (!gifWriter.getOutputFile().exists()) {
            // Already moved to the player's chosen location.
            return;
        }
        if (gifWriter.delete()) {
            LOGGER.info("Deleted temporary game summary GIF {}", gifWriter.getOutputFile());
        } else {
            LOGGER.error("Failed to delete temporary game summary GIF {}", gifWriter.getOutputFile());
        }
    }

    /**
     * Stops the thread.
     */
    public void stopThread() {
        stopThread(false);
    }

    /**
     * Stops recording. Only the first call has an effect: once a stop is under way the thread may be showing
     * the save prompt, and a later stop (MekHQ closes the client right after the victory phase) must not
     * interrupt it, or the prompt closes before the player can answer.
     *
     * @param forceInterrupt {@code true} to discard the GIF without asking the player
     */
    public synchronized void stopThread(boolean forceInterrupt) {
        if (!isLive) {
            LOGGER.info("GIF writer already stopping; ignoring stop request (discard={})", forceInterrupt);
            return;
        }
        LOGGER.info("Stopping GIF writer (discard={})", forceInterrupt);
        this.forceInterrupt = forceInterrupt;
        isLive = false;
        interrupt();
    }
}
