/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.utilities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/**
 * Thread that writes frames to a GIF file.
 *
 * @author Luana Coppio
 */
public class GifWriterThread extends Thread {
    private static final MMLogger logger = MMLogger.create(GifWriter.class);

    private record Frame(BufferedImage image, long duration) {
    }

    private final GifWriter gifWriter;
    private final Deque<Frame> imageDeque = new ConcurrentLinkedDeque<>();
    private boolean isLive = true;
    private boolean forceInterrupt = false;
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
        synchronized (this) {
            imageDeque.add(new Frame(image, durationMillis));
            notifyAll();
        }
    }

    @Override
    public void run() {
        try {
            while (isLive) {
                try {
                    synchronized (this) {
                        while (imageDeque.isEmpty() && gifWriter.isLive() && isLive) {
                            wait();
                        }
                        if (!gifWriter.isLive()) {
                            break;
                        }
                        Frame frame = imageDeque.pollFirst();
                        if (frame == null) {
                            continue;
                        }
                        gifWriter.appendFrame(frame.image(), frame.duration());
                    }
                } catch (InterruptedException | IOException ignored) {
                    break;
                }
            }
        } finally {
            gifWriter.close();
            imageDeque.clear();
            if (!forceInterrupt) {
                try {
                    saveGifNag();
                } catch (Exception e) {
                    logger.error(e, "Error deleting gif or opening JOptionPane");
                }
            }
            isLive = false;
            forceInterrupt = false;
        }
    }

    private void saveGifNag() {
        if (GUIPreferences.getInstance().getGifGameSummaryMinimap()) {
            int response = JOptionPane.showConfirmDialog(null,
                  Messages.getString("ClientGUI.SaveGifDialog.message"),
                  Messages.getString("ClientGUI.SaveGifDialog.title"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.INFORMATION_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
                saveGif();
                return;
            }
        }
        deleteGif();
    }

    private void saveGif() {
        SaveDialogResult result = getSaveDialog();
        if ((result.returnVal() != JFileChooser.APPROVE_OPTION) || (result.saveDialog().getSelectedFile() == null)) {
            // Without a file there is no saving for the file, which means we can't save the GIF and instead we
            // delete it
            deleteGif();
            return;
        }

        // Did the player select a file?
        File gifFile = result.saveDialog().getSelectedFile();
        if (gifFile != null) {
            if (!gifFile.getName().toLowerCase().endsWith(CG_FILE_EXTENSION_GIF)) {
                try {
                    gifFile = new File(gifFile.getCanonicalPath() + CG_FILE_EXTENSION_GIF);
                } catch (Exception ignored) {
                    // Without a file there is no saving for the file, which means we can't save the GIF and instead
                    // we delete it
                    deleteGif();
                    return;
                }
            }
            File finalGifFile = gifFile;
            try {
                if (gifWriter.getOutputFile().renameTo(finalGifFile)) {
                    logger.info("Game summary GIF saved to {}", finalGifFile);
                } else {
                    logger.errorDialog("Unable to save GIF in destination",
                          "Unable to save file {} at {}",
                          gifWriter.getOutputFile(),
                          finalGifFile);
                }
            } catch (Exception ex) {
                logger.errorDialog(ex,
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

    private void deleteGif() {
        if (gifWriter.delete()) {
            logger.info("Deleted temporary game summary GIF {}", gifWriter.getOutputFile());
        } else {
            logger.error("Failed to delete temporary game summary GIF {}", gifWriter.getOutputFile());
        }
    }

    /**
     * Stops the thread.
     */
    public void stopThread() {
        stopThread(false);
    }

    public void stopThread(boolean forceInterrupt) {
        isLive = false;
        this.forceInterrupt = forceInterrupt;
        interrupt();
    }
}
