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

package megamek.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GifWriterThread}'s producer/consumer behavior.
 */
class GifWriterThreadTest {

    /**
     * Regression guard: the producer ({@code addFrame}) must not block while the consumer is busy
     * encoding a frame. The old implementation held a lock across the encode, so the game thread
     * stalled on every frame. With a bounded queue the producer only blocks if the encoder falls far
     * behind, never for a single in-progress encode.
     */
    @Test
    void testAddFrameDoesNotBlockWhileEncoding() throws Exception {
        CountDownLatch encodingStarted = new CountDownLatch(1);
        CountDownLatch releaseEncode = new CountDownLatch(1);

        GifWriter gifWriter = mock(GifWriter.class);
        // Make the first encode block until the test releases it, simulating a slow frame.
        doAnswer(invocation -> {
            encodingStarted.countDown();
            releaseEncode.await(5, TimeUnit.SECONDS);
            return null;
        }).when(gifWriter).appendFrame(any(), anyLong());

        GifWriterThread thread = new GifWriterThread(gifWriter, "test-gif-thread");
        thread.start();
        try {
            BufferedImage frame = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);

            // First frame: the consumer picks it up and blocks inside the (mocked) encode.
            thread.addFrame(frame, 100);
            assertTrue(encodingStarted.await(2, TimeUnit.SECONDS), "Encoder should have started");

            // Second frame while the encoder is blocked: this call must return promptly.
            long startNanos = System.nanoTime();
            thread.addFrame(frame, 100);
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
            assertTrue(elapsedMillis < 1000,
                  "addFrame must not block while a frame is encoding (took " + elapsedMillis + " ms)");
        } finally {
            releaseEncode.countDown();
            // forceInterrupt = true skips the interactive save dialog in the thread's shutdown path.
            thread.stopThread(true);
            thread.join(5000);
            assertFalse(thread.isAlive(), "GifWriterThread should terminate within the join timeout");
        }
    }
}
