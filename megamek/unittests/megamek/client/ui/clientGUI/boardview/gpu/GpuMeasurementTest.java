/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GpuMeasurementTest {
    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = { "MOVEMENT", "LOUNGE" })
    void measurementGesturesReachSharedToolsWithoutPlotting(GamePhase phase) throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            List<BoardViewEvent> events = new ArrayList<>();
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.setPhase(phase);
                fixture.view.addBoardViewListener(new BoardViewListenerAdapter() {
                    @Override
                    public void hexMoused(BoardViewEvent event) {
                        events.add(event);
                    }

                    @Override
                    public void firstLOSHex(BoardViewEvent event) {
                        events.add(event);
                    }

                    @Override
                    public void secondLOSHex(BoardViewEvent event) {
                        events.add(event);
                    }
                });
            });
            Coords start = new Coords(4, 4), end = new Coords(8, 6);
            for (int modifiers : List.of(InputEvent.ALT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK)) {
                fixture.source.hover(start, modifiers);
                fixture.source.click(start, false, modifiers);
                fixture.source.hover(end, modifiers);
                fixture.source.click(end, false, modifiers);
                SwingUtilities.invokeAndWait(() -> { });
            }
            assertEquals(List.of(BoardViewEvent.BOARD_HEX_CLICKED, BoardViewEvent.BOARD_HEX_CLICKED,
                        BoardViewEvent.BOARD_FIRST_LOS_HEX, BoardViewEvent.BOARD_SECOND_LOS_HEX),
                  events.stream().map(BoardViewEvent::getType).toList(),
                  "Measurement must not leak movement-preview drag events");
            assertEquals(List.of(start, end, start, end), events.stream().map(BoardViewEvent::getCoords).toList());
            assertEquals(InputEvent.ALT_DOWN_MASK, events.getFirst().getModifiers());
            assertEquals(InputEvent.ALT_DOWN_MASK, events.get(1).getModifiers());
            assertNull(fixture.view.getFirstLOS(), "The second LOS endpoint completes the shared gesture");
            assertFalse(fixture.source.takeFrame().scene().tactical().fills().isEmpty());

            SwingUtilities.invokeAndWait(() -> {
                events.clear();
                fixture.source.close();
            });
            fixture.source.click(start, false, InputEvent.ALT_DOWN_MASK);
            fixture.source.click(start, false, InputEvent.CTRL_DOWN_MASK);
            SwingUtilities.invokeAndWait(() -> { });
            assertTrue(events.isEmpty(), "Closed boards must reject both measurement gestures");
        }
    }

    @Test
    void rulerAndLosUpdateAndClearWithoutChangingRasterTiles() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                fixture.source.refresh();
                BoardScene before = fixture.source.takeFrame().scene();
                Coords start = new Coords(4, 4), end = new Coords(8, 6);
                fixture.view.drawRuler(start, null, Color.CYAN, Color.ORANGE);
                fixture.source.refresh();
                var first = fixture.source.takeFrame().scene().tactical();
                assertTrue(first.fills().stream().anyMatch(fill -> fill.argb() == Color.CYAN.getRGB()));
                assertTrue(first.fills().stream().noneMatch(fill -> fill.argb() == Color.ORANGE.getRGB()));

                fixture.view.drawRuler(start, end, Color.CYAN, Color.ORANGE);
                fixture.view.checkLOS(start);
                fixture.view.checkLOS(end);
                fixture.source.refresh();
                BoardScene measured = fixture.source.takeFrame().scene();
                var colors = measured.tactical().fills().stream().map(fill -> fill.argb()).toList();
                assertTrue(colors.containsAll(List.of(Color.CYAN.getRGB(), Color.ORANGE.getRGB(),
                      Color.YELLOW.getRGB(), Color.RED.getRGB())));
                assertEquals(before.tiles().stream().map(BoardScene.Tile::tactical).toList(),
                      measured.tiles().stream().map(BoardScene.Tile::tactical).toList());

                fixture.view.drawRuler(null, null, Color.CYAN, Color.ORANGE);
                fixture.view.select(null);
                fixture.source.refresh();
                assertEquals(before.tactical(), fixture.source.takeFrame().scene().tactical());
                assertFalse(measured.tactical().fills().isEmpty(), "Clearing must not mutate a published frame");
            });
        }
    }
}
