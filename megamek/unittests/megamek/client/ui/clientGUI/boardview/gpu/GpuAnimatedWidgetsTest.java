/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import megamek.client.Client;
import megamek.client.ui.clientGUI.ChatterBox;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.overlay.BoardToastOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.ChatterBoxOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.ToastLevel;
import org.junit.jupiter.api.Test;

class GpuAnimatedWidgetsTest {
    @Test
    void chatSlidesAndReversesWithoutBakingPositionIntoItsArtwork() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                Client client = mock(Client.class);
                when(client.getGame()).thenReturn(fixture.game);
                ClientGUI gui = mock(ClientGUI.class);
                when(gui.getClient()).thenReturn(client);
                when(gui.getMainPanel()).thenReturn(new JPanel());
                ChatterBox history = mock(ChatterBox.class);
                history.history = new LinkedList<>();
                ChatterBoxOverlay chat = new ChatterBoxOverlay(gui, fixture.view, null, history);
                try {
                    fixture.view.addOverlay(chat);
                    Dimension size = new Dimension(800, 600);
                    Dimension pixels = new Dimension(1200, 900);
                    chat.slideDown();
                    var closing = fixture.view.captureOverlayLayers(size, pixels).getFirst();
                    var motion = closing.shiftY();
                    assertEquals(0, motion.from());
                    assertTrue(motion.to() > 0);
                    assertEquals(motion.to() / 2, motion.value(motion.startedNanos() + 100_000_000), 0.001f);
                    assertEquals(motion.to(), motion.value(motion.startedNanos() + 200_000_000));
                    assertTrue(closing.image().getWidth() < pixels.width);
                    assertTrue(closing.image().getHeight() < pixels.height);
                    var same = fixture.view.captureOverlayLayers(size, pixels).getFirst();
                    assertSame(closing.image(), same.image(), "Sliding must reuse the unshifted chat texture");
                    chat.slideUp();
                    var opening = fixture.view.captureOverlayLayers(size, pixels).getFirst().shiftY();
                    assertEquals(motion.value(opening.startedNanos()), opening.from(), 0.001f);
                    assertEquals(0, opening.value(opening.startedNanos() + 200_000_000));
                } finally {
                    chat.dispose();
                    GUIPreferences.getInstance().removePreferenceChangeListener(chat);
                }
            });
        }
    }

    @Test
    void toastSnapshotIncludesItsWholeLifetimeAndStackReflowKeepsCachedArtwork() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                BoardToastOverlay toasts = new BoardToastOverlay(fixture.view, null);
                fixture.view.addOverlay(toasts);
                fixture.source.setViewport(1200, 800, 1200, 800);
                toasts.show(ToastLevel.INFO, "Short notification", null, 0);
                toasts.show(ToastLevel.INFO, "Long notification", null, 5000);
                fixture.source.refresh();
            });
            var initial = fixture.source.takeFrame().hud();
            assertEquals(2, initial.layers().size());
            var first = initial.layers().getFirst();
            var fade = first.fade();
            long start = fade.transition().startedNanos();
            assertEquals(0, fade.opacity(start));
            assertEquals(1, fade.opacity(start + 300_000_000));
            assertEquals(0.5f, fade.opacity(start + 800_000_000), 0.001f);
            assertEquals(0, fade.opacity(start + 1_300_000_000));
            assertTrue(first.pixels().width() < initial.width());
            assertTrue(first.pixels().height() < initial.height());
            SwingUtilities.invokeAndWait(fixture.source::refresh);
            assertSame(first.pixels(), fixture.source.takeFrame().hud().layers().getFirst().pixels());

            CompletableFuture<GpuBoardSource.Hud> remaining = new CompletableFuture<>();
            Timer check = new Timer(20, event -> {
                var hud = fixture.source.takeFrame().hud();
                if (hud.layers().size() == 1) {
                    remaining.complete(hud);
                }
            });
            SwingUtilities.invokeAndWait(check::start);
            GpuBoardSource.Hud reflow;
            try {
                reflow = remaining.get(5, TimeUnit.SECONDS);
            } finally {
                SwingUtilities.invokeAndWait(check::stop);
            }
            var second = initial.layers().get(1);
            var moved = reflow.layers().getFirst();
            assertSame(second.pixels(), moved.pixels(), "Expiring a neighbour must not redraw a toast");
            assertEquals(second.fade(), moved.fade(), "Stack reflow must not restart its lifetime");
            assertTrue(moved.shiftY().from() > moved.shiftY().to());
            long halfway = moved.shiftY().startedNanos() + 100_000_000;
            assertTrue(moved.shiftY().value(halfway) > moved.shiftY().to());
            assertTrue(moved.shiftY().value(halfway) < moved.shiftY().from());
        }
    }
}
