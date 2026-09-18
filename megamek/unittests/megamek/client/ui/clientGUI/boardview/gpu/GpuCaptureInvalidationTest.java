/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;

class GpuCaptureInvalidationTest {
    @Test
    void unchangedRefreshSkipsPaintersAndInvalidationRefreshesTheirPixels() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            AtomicInteger paints = new AtomicInteger();
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.addHexDrawPlugin((graphics, hex, game, coords, view) -> {
                    paints.incrementAndGet();
                    graphics.setColor(Color.RED);
                    graphics.fillRect(90, 90, 20, 20);
                });
                fixture.view.clearHexImageCache();
                fixture.source.refresh();
                int first = paints.get();
                assertTrue(first > 0);
                fixture.source.refresh();
                assertEquals(first, paints.get(), "The timer must not repaint an unchanged board");
                fixture.view.clearHexImageCache();
                fixture.source.refresh();
                assertTrue(paints.get() > first, "Shared painter invalidation must reach the GPU snapshot");
                fixture.source.setVisibleArea(new Rectangle(0, 16, 1, 1));
                fixture.source.refresh();
                BoardScene.Pixels pixels = fixture.source.takeFrame().scene().tile(new Coords(0, 16)).tactical();
                assertEquals(0xff0000ff, pixels.rgba(95 * pixels.width() + 95),
                      "Panning must capture the destination at full tactical resolution");
            });
        }
    }
}
