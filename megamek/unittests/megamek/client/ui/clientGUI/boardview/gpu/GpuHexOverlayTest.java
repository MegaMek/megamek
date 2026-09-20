/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.ECMEffects;
import megamek.client.ui.util.PlayerColour;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.MiscType;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Test;

class GpuHexOverlayTest {
    static final class Options implements AutoCloseable {
        private final GUIPreferences prefs = GUIPreferences.getInstance();
        private final boolean sheets = prefs.getShowMapSheets();
        private final Color sheetColor = prefs.getMapsheetColor();
        private final int alpha = prefs.getECMTransparency(), zoom = prefs.getMapZoomIndex();

        Options() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                prefs.setShowMapSheets(false);
                prefs.setECMTransparency(96);
            });
        }

        @Override
        public void close() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                prefs.setShowMapSheets(sheets);
                prefs.setMapSheetColor(sheetColor);
                prefs.setECMTransparency(alpha);
                prefs.setMapZoomIndex(zoom);
            });
        }
    }

    @Test
    void sheetSeamsAndEmbeddedBoardsUseNativeGeometryAndRefreshWithoutRasterChanges() throws Exception {
        Hex[] hexes = new Hex[18 * 19];
        for (int y = 0; y < 19; y++) {
            for (int x = 0; x < 18; x++) {
                hexes[y * 18 + x] = new Hex(x == 15 ? 3 : 0);
            }
        }
        try (Options options = new Options(); GpuBoardFixture fixture = GpuBoardFixture.create(new Board(18, 19, hexes))) {
            SwingUtilities.invokeAndWait(() -> {
                fixture.source.refresh();
                BoardScene before = fixture.source.takeFrame().scene();
                assertTrue(before.tactical().fills().isEmpty());
                var prefs = GUIPreferences.getInstance();
                prefs.setMapSheetColor(Color.CYAN);
                prefs.setShowMapSheets(true);
                Coords embedded = new Coords(15, 8);
                fixture.game.getBoard().setEmbeddedBoard(1, embedded);
                fixture.source.refresh();
                BoardScene marked = fixture.source.takeFrame().scene();
                assertEquals(raster(before), raster(marked));
                assertTrue(covers(marked.tactical(), Color.CYAN, new Coords(16, 8), 10.5, 18),
                      "The second sheet's left edge must be drawn at the actual hex seam");
                assertTrue(covers(marked.tactical(), Color.CYAN, new Coords(8, 17), 42, 0),
                      "The second row of sheets needs its top edge");
                assertFalse(covers(marked.tactical(), Color.CYAN, new Coords(8, 8), 42, 0),
                      "Interior hexes must not acquire sheet borders");
                assertTrue(covers(marked.tactical(), new Color(0, 140, 0, 120), embedded, 42, 36));
                List<BoardTacticalGeometry.Triangle> triangles = new ArrayList<>();
                BoardTacticalGeometry.drape(marked, triangles::add);
                assertTrue(triangles.stream().filter(t -> t.argb() == new Color(0, 140, 0, 120).getRGB())
                      .allMatch(t -> t.a().z > 3 * BoardGeometry.LEVEL && t.b().z > 3 * BoardGeometry.LEVEL
                            && t.c().z > 3 * BoardGeometry.LEVEL));
                fixture.view.getComponent();
                fixture.view.zoomOut();
                float scale = fixture.view.getScale();
                assertEquals(marked.tactical(), fixture.view.captureTacticalGeometry());
                assertEquals(scale, fixture.view.getScale());
                prefs.setMapSheetColor(Color.YELLOW);
                fixture.source.refresh();
                assertTrue(covers(fixture.source.takeFrame().scene().tactical(), Color.YELLOW, new Coords(8, 17), 42, 0));
                prefs.setShowMapSheets(false);
                fixture.game.getBoard().embeddedBoardCoords().clear();
                fixture.source.refresh();
                assertTrue(fixture.source.takeFrame().scene().tactical().fills().isEmpty());
                assertEquals(raster(before), raster(fixture.source.takeFrame().scene()));
                assertFalse(marked.tactical().fills().isEmpty(), "Published snapshots must remain immutable");
            });
        }
    }

    @Test
    void ecmAndEccmReuseTerrainAndRespondToModeColorOpacityAndPosition() throws Exception {
        try (Options options = new Options(); GpuBoardFixture fixture = GpuBoardFixture.create()) {
            fixture.addEcm();
            SwingUtilities.invokeAndWait(() -> {
                fixture.source.refresh();
                BoardScene ecm = fixture.source.takeFrame().scene();
                Coords sample = new Coords(4, 4);
                Color tint = ECMEffects.getECMColor(fixture.player);
                assertTrue(covers(ecm.tactical(), tint, sample, 42, 36));
                assertTrue(covers(ecm.tactical(), tint, sample, 1, 36), "Native tint reaches the full hex edge");
                BoardScene.Pixels noise = ecm.tile(sample).tactical();
                assertTrue(noise != null, "ECM static remains in the raster compatibility layer");
                int clear = 0, opaque = 0;
                for (int y = noise.height() / 3; y < noise.height() * 2 / 3; y++) {
                    for (int x = noise.width() / 3; x < noise.width() * 2 / 3; x++) {
                        if ((noise.rgba(y * noise.width() + x) & 255) == 0) {
                            clear++;
                        } else {
                            opaque++;
                        }
                    }
                }
                assertTrue(clear > 0 && opaque > 0, "Raster noise must not retain a duplicate solid tint");
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_ECCM).setValue(true);
                fixture.entity.setGameOptions();
                var suite = fixture.entity.getMisc().stream().filter(m -> m.getType().hasFlag(MiscType.F_ECM)).findFirst().orElseThrow();
                assertTrue(suite.setModeImmediately("ECCM") >= 0);
                fixture.view.updateEcmList();
                fixture.source.refresh();
                BoardScene eccm = fixture.source.takeFrame().scene();
                assertTrue(covers(eccm.tactical(), tint, sample, 42, 36));
                assertTrue(eccm.tile(sample).tactical() == null, "ECCM needs no raster artwork");

                BoardView.PlanarHex cached = artwork(fixture, sample);
                GUIPreferences.getInstance().setECMTransparency(48);
                assertSame(cached.terrain(), artwork(fixture, sample).terrain(), "Opacity must not repaint terrain");
                fixture.source.refresh();
                BoardScene faded = fixture.source.takeFrame().scene();
                assertTrue(covers(faded.tactical(), ECMEffects.getECMColor(fixture.player), sample, 42, 36));
                assertNotEquals(eccm.tactical(), faded.tactical());
                assertSame(eccm.tile(sample).ground(), faded.tile(sample).ground());
                fixture.player.setColour(PlayerColour.RED);
                fixture.entity.setPosition(new Coords(14, 14));
                fixture.view.updateEcmList();
                fixture.source.refresh();
                BoardScene moved = fixture.source.takeFrame().scene();
                assertTrue(covers(moved.tactical(), ECMEffects.getECMColor(fixture.player), new Coords(13, 13), 42, 36));
                assertFalse(covers(moved.tactical(), ECMEffects.getECMColor(fixture.player), sample, 42, 36));
                assertTrue(covers(ecm.tactical(), tint, sample, 42, 36), "Old field snapshots must not move with the emitter");
                GUIPreferences.getInstance().setECMTransparency(0);
                fixture.source.refresh();
                assertTrue(fixture.source.takeFrame().scene().tactical().fills().isEmpty());
            });
        }
    }

    @Test
    void hiddenUnseenAndOtherBoardEmittersDoNotLeakFieldsOrSourceOutlines() throws Exception {
        try (Options options = new Options(); GpuBoardFixture fixture = GpuBoardFixture.create()) {
            fixture.addEcm();
            SwingUtilities.invokeAndWait(() -> {
                Player enemy = new Player(1, "Opponent");
                enemy.setTeam(2);
                fixture.game.addPlayer(enemy.getId(), enemy);
                fixture.entity.setOwner(enemy);
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS).setValue(true);
                fixture.view.updateEcmList();
                assertTrue(fixture.view.captureTacticalGeometry().fills().isEmpty());
                fixture.entity.addBeenDetectedBy(fixture.player);
                fixture.view.updateEcmList();
                assertFalse(fixture.view.captureTacticalGeometry().fills().isEmpty(), "Shared rules allow detected ECM");
                fixture.entity.setHidden(true);
                fixture.view.updateEcmList();
                assertTrue(fixture.view.captureTacticalGeometry().fills().isEmpty());
                fixture.entity.setHidden(false);
                fixture.game.setBoard(1, new Board(16, 17));
                fixture.entity.setBoardId(1);
                fixture.view.updateEcmList();
                assertTrue(fixture.view.captureTacticalGeometry().fills().isEmpty(),
                      "An emitter on another board must not leave a source outline at matching coordinates");
            });
        }
    }

    private static BoardView.PlanarHex artwork(GpuBoardFixture fixture, Coords coords) {
        List<BoardView.PlanarHex> result = new ArrayList<>();
        fixture.view.capturePlanarHexes(new Rectangle(coords.getX(), coords.getY(), 1, 1), false, result::add);
        return result.getFirst();
    }

    private static List<BoardScene.Pixels> raster(BoardScene scene) {
        return scene.tiles().stream().map(BoardScene.Tile::tactical).toList();
    }

    static boolean covers(BoardTactical tactical, Color color, Coords coords, double x, double y) {
        double boardX = coords.getX() * 63 + x;
        double boardY = coords.getY() * 72 + (coords.getX() & 1) * 36 + y;
        return tactical.fills().stream().anyMatch(fill -> fill.argb() == color.getRGB() && fill.shape().contains(boardX, boardY));
    }
}
