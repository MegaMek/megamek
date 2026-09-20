/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.awt.Rectangle;
import java.util.Vector;
import java.util.concurrent.FutureTask;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardFieldOfView;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.Sensor;
import megamek.common.enums.GamePhase;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Test;

class GpuFieldOfViewTest {
    /** Keep native comparisons deterministic without persisting different user preferences. */
    static final class Options implements AutoCloseable {
        private final GUIPreferences prefs = GUIPreferences.getInstance();
        private final boolean darken = prefs.getFovDarken(), highlight = prefs.getFovHighlight();
        private final boolean grayscale = prefs.getFovGrayscale(), spotting = prefs.getFovSpottingMode();
        private final int alpha = prefs.getFovDarkenAlpha();

        Options() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                prefs.setFovDarken(true);
                prefs.setFovHighlight(false);
                prefs.setFovGrayscale(false);
                prefs.setFovSpottingMode(false);
                prefs.setFovDarkenAlpha(100);
            });
        }

        @Override
        public void close() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                prefs.setFovDarken(darken);
                prefs.setFovHighlight(highlight);
                prefs.setFovGrayscale(grayscale);
                prefs.setFovSpottingMode(spotting);
                prefs.setFovDarkenAlpha(alpha);
            });
        }
    }

    @Test
    void sharedLosBlocksBehindARidgeAndRefreshesWhenTerrainChangesWithoutRasterFov() throws Exception {
        Hex[] hexes = new Hex[9 * 9];
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                hexes[y * 9 + x] = new Hex(x == 4 ? 5 : 0);
            }
        }
        try (Options options = new Options(); GpuBoardFixture fixture = GpuBoardFixture.create(new Board(9, 9, hexes))) {
            SwingUtilities.invokeAndWait(() -> {
                fixture.source.setVisibleArea(new Rectangle(0, 0, 9, 9));
                fixture.source.refresh();
                BoardScene before = fixture.source.takeFrame().scene();
                assertFalse(before.fieldOfView().active());
                fixture.view.select(new Coords(2, 4));
                fixture.source.refresh();
                BoardScene measured = fixture.source.takeFrame().scene();
                BoardFieldOfView mask = measured.fieldOfView();
                assertTrue(mask.active());
                assertEquals(BoardFieldOfView.Visibility.VISIBLE, at(mask, new Coords(2, 2)).visibility());
                Coords behindRidge = new Coords(6, 4);
                assertEquals(BoardFieldOfView.Visibility.BLOCKED, at(mask, behindRidge).visibility());
                assertFalse(at(mask, behindRidge).outsideSensorRange(),
                      "A ridge blocking a hex-only LOS preview must not select the sensor effect");
                assertEquals(fixture.view.getFovHighlighting().evaluate(behindRidge), at(mask, behindRidge));
                assertEquals(before.tiles().stream().map(BoardScene.Tile::tactical).toList(),
                      measured.tiles().stream().map(BoardScene.Tile::tactical).toList());
                assertSame(mask, measured.withUnits(measured.units()).fieldOfView(),
                      "Animation snapshots must retain the authoritative visibility result");
                Coords unitHex = fixture.entity.getPosition();
                assertEquals(BoardFieldOfView.Visibility.BLOCKED, at(mask, unitHex).visibility());
                fixture.entity.setElevation(12);
                fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, fixture.entity, new Vector<>()));
                fixture.source.refresh();
                assertEquals(BoardFieldOfView.Visibility.VISIBLE,
                      at(fixture.source.takeFrame().scene().fieldOfView(), unitHex).visibility(),
                      "A target raised above the ridge must refresh cached LOS during the same turn");
                fixture.entity.setElevation(0);
                fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, fixture.entity, new Vector<>()));
                fixture.source.refresh();
                assertEquals(BoardFieldOfView.Visibility.BLOCKED,
                      at(fixture.source.takeFrame().scene().fieldOfView(), unitHex).visibility());
                for (int y = 0; y < 9; y++) {
                    fixture.game.getBoard().setHex(4, y, new Hex(0));
                }
                fixture.source.refresh();
                assertEquals(BoardFieldOfView.Visibility.VISIBLE,
                      at(fixture.source.takeFrame().scene().fieldOfView(), behindRidge).visibility());
                assertEquals(BoardFieldOfView.Visibility.BLOCKED, at(mask, behindRidge).visibility(),
                      "Terrain updates must not mutate published snapshots");
            });
        }
    }

    @Test
    void reportPhasePreferencesAndClearedSelectionRemoveOrRefreshVisibility() throws Exception {
        try (Options options = new Options(); GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.select(new Coords(1, 6));
                fixture.source.refresh();
                assertTrue(fixture.source.takeFrame().scene().fieldOfView().active());
                fixture.game.setPhase(GamePhase.MOVEMENT_REPORT);
                fixture.source.refresh();
                assertFalse(fixture.source.takeFrame().scene().fieldOfView().active());
                fixture.game.setPhase(GamePhase.MOVEMENT);
                fixture.source.refresh();
                assertTrue(fixture.source.takeFrame().scene().fieldOfView().active());
                var prefs = GUIPreferences.getInstance();
                prefs.setFovSpottingMode(true);
                fixture.source.refresh();
                assertTrue(fixture.source.takeFrame().scene().fieldOfView().spotting());
                prefs.setFovDarken(false);
                fixture.source.refresh();
                assertFalse(fixture.source.takeFrame().scene().fieldOfView().active());
                prefs.setFovDarken(true);
                fixture.view.select(null);
                fixture.source.refresh();
                assertFalse(fixture.source.takeFrame().scene().fieldOfView().active());
            });
        }
    }

    @Test
    void sensorRangeIsSeparateFromLosAndUsesTheSameRangesAsTheSensorOverlay() throws Exception {
        Sensor sensor = new Sensor(Sensor.TYPE_MEK_RADAR);
        int range = sensor.getRangeByBracket();
        int height = range + 5;
        Hex[] hexes = new Hex[9 * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < 9; x++) {
                hexes[y * 9 + x] = new Hex(y == 3 ? 5 : 0);
            }
        }
        try (Options options = new Options(); GpuBoardFixture fixture = GpuBoardFixture.create(new Board(9, height, hexes))) {
            FutureTask<Void> check = new FutureTask<>(() -> {
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS).setValue(true);
                fixture.entity.setPosition(new Coords(4, 1));
                var viewer = spy(fixture.entity);
                doReturn(sensor).when(viewer).getActiveSensor();
                doReturn(7).when(viewer).getSensorCheck();
                ClientGUI gui = mock(ClientGUI.class);
                doReturn(viewer).when(gui).getDisplayedUnit();
                BoardView view = new BoardView(fixture.game, null, gui, 0);
                try {
                    view.select(viewer.getPosition());
                    Rectangle area = new Rectangle(0, 0, 9, height);
                    BoardFieldOfView mask = view.captureFieldOfView(area);
                    assertEquals(range, Compute.getSensorRanges(fixture.game, viewer).maxSensorRange);
                    Coords inRange = new Coords(4, range);
                    Coords atLimit = new Coords(4, range + 1);
                    Coords beyond = new Coords(4, range + 2);
                    assertEquals(BoardFieldOfView.Visibility.SENSOR, at(mask, inRange).visibility(),
                          "LOS-blocked hexes inside the displayed sensor range must use the FoV effect");
                    assertFalse(at(mask, inRange).outsideSensorRange());
                    assertFalse(at(mask, atLimit).outsideSensorRange());
                    assertEquals(BoardFieldOfView.Visibility.BLOCKED, at(mask, beyond).visibility());
                    assertTrue(at(mask, beyond).outsideSensorRange());

                    fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS).setValue(false);
                    BoardFieldOfView disabled = view.captureFieldOfView(area);
                    assertEquals(BoardFieldOfView.Visibility.BLOCKED, at(disabled, inRange).visibility());
                    assertFalse(at(disabled, beyond).outsideSensorRange(), "Disabled sensor rules must leave LOS dimming active");

                    fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS).setValue(true);
                    doReturn(null).when(viewer).getActiveSensor();
                    assertFalse(at(view.captureFieldOfView(area), beyond).outsideSensorRange(),
                          "Without an active sensor, no sensor-range mask may be inferred from BLOCKED");
                    assertTrue(at(mask, beyond).outsideSensorRange(), "Published snapshots must remain immutable");
                } finally {
                    view.dispose();
                }
                return null;
            });
            SwingUtilities.invokeAndWait(check);
            check.get();
        }
    }

    static BoardFieldOfView.Hex at(BoardFieldOfView mask, Coords coords) {
        return mask.hexes().get(coords.getX() * mask.height() + coords.getY());
    }
}
