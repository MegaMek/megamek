package megamek.utilities;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GifWriterThread extends Thread {

    private record Frame(BufferedImage image, long duration) {}

    private final GifWriter gifWriter;
    private final Deque<Frame> imageDeque = new ConcurrentLinkedDeque<>();
    private boolean isLive = true;

    public GifWriterThread(GifWriter gifWriter, String name) {
        super(name);
        this.gifWriter = gifWriter;
    }

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

    public void stopThread() {
        isLive = false;
        interrupt();
    }
}
