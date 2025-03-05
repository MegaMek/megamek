/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MegaMek.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.utilities;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread that writes frames to a GIF file.
 * @author Luana Coppio
 */
public class GifWriterThread extends Thread {

    private record Frame(BufferedImage image, long duration) {}

    private final GifWriter gifWriter;
    private final Deque<Frame> imageDeque = new ConcurrentLinkedDeque<>();
    private boolean isLive = true;

    /**
     * Creates a new GifWriterThread.
     * @param gifWriter the GIF writer
     * @param name the thread name
     */
    public GifWriterThread(GifWriter gifWriter, String name) {
        super(name);
        this.gifWriter = gifWriter;
    }

    /**
     * Adds a frame to the GIF.
     * @param image the frame image
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
                } catch (InterruptedException | IOException e) {
                    break;
                }
            }
        } finally {
            gifWriter.close();
            imageDeque.clear();
        }
    }

    /**
     * Stops the thread.
     */
    public void stopThread() {
        isLive = false;
        interrupt();
    }
}
