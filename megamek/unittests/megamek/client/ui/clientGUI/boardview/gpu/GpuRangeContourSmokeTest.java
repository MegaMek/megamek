/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Actual GPU texture sampling, scrolling, face orientation, rebuilds and resource disposal. */
@Tag("on-demand")
class GpuRangeContourSmokeTest {
    @Test
    void contourLabelsFollowPresentationModeAndKeepTheirPhaseAfterTerrainChanges() {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                GpuFireControl control = new GpuFireControl();
                GpuFireControl splitFrames = new GpuFireControl();
                Pixmap before = null, after = null, split = null, rebuilt = null;
                try {
                    OrthographicCamera camera = new OrthographicCamera(650, 410);
                    var center = BoardGeometry.center(new Coords(4, 3), 0);
                    camera.position.set(center).add(350, -500, 440);
                    camera.up.set(0, 0, 1);
                    camera.lookAt(center);
                    camera.near = 1;
                    camera.far = 2000;
                    camera.update();
                    control.update(scene(false, false));
                    before = draw(control, camera, 0);
                    PixmapIO.writePNG(new FileHandle(new File(output, "range-contours-start.png")), before, -1, true);
                    after = draw(control, camera, 0.75f);
                    PixmapIO.writePNG(new FileHandle(new File(output, "range-contours-scrolled.png")), after, -1, true);
                    if (BoardView.GPU_SCROLLING_RANGE_LABELS) {
                        assertTrue(changedPixels(before, after) > 500, "The contour glyphs must visibly move");
                    } else {
                        assertEquals(0, changedPixels(before, after), "Flat range letters must not spin or scroll");
                    }

                    splitFrames.update(scene(false, false));
                    draw(splitFrames, camera, 0.25f).dispose();
                    split = draw(splitFrames, camera, 0.5f);
                    assertEquals(0, changedPixels(after, split), "Scrolling must depend on elapsed time, not frame count");

                    // Change a tile away from the contours to force disposal and reconstruction of the model.
                    control.update(scene(true, false));
                    rebuilt = draw(control, camera, 0);
                    assertEquals(0, changedPixels(after, rebuilt), "A geometry rebuild must preserve the animation phase");
                    control.update(scene(true, true));
                    Pixmap cleared = draw(control, camera, 0);
                    try {
                        int background = cleared.getPixel(0, 0);
                        for (int y = 0; y < cleared.getHeight(); y++) {
                            for (int x = 0; x < cleared.getWidth(); x++) {
                                assertEquals(background, cleared.getPixel(x, y), "Cleared contours must leave no glyphs");
                            }
                        }
                        verifyMovementVisibility(control, camera, cleared, after);
                    } finally {
                        cleared.dispose();
                    }
                    control.update(scene(false, false));
                    draw(control, camera, 0).dispose();
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    for (Pixmap pixels : new Pixmap[] { before, after, split, rebuilt }) {
                        if (pixels != null) {
                            pixels.dispose();
                        }
                    }
                    control.dispose();
                    splitFrames.dispose();
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static BoardScene scene(boolean changedTerrain, boolean cleared) {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), changedTerrain && x == 0 && y == 0 ? 1 : 0,
                      -1, false, 0, BoardScene.Surface.GRASS, null, null, null, List.of(), List.of()));
            }
        }
        List<BoardScene.RangeBorder> borders = new ArrayList<>();
        List<BoardScene.RangeLabel> markers = new ArrayList<>();
        List<String> labels = List.of("min", "S", "M", "L", "E");
        List<Coords> positions = List.of(new Coords(2, 2), new Coords(4, 2), new Coords(6, 2),
              new Coords(3, 5), new Coords(5, 5));
        if (!cleared) {
            for (int i = 0; i < labels.size(); i++) {
                markers.add(new BoardScene.RangeLabel(positions.get(i),
                      FieldOfFireSprite.getFieldOfFireColor(i).getRGB(), labels.get(i)));
                for (var border : BoardFiringGeometryTest.borders(Set.of(positions.get(i)), labels.get(i))) {
                    borders.add(new BoardScene.RangeBorder(border.coords(), border.edges(),
                          FieldOfFireSprite.getFieldOfFireColor(i).getRGB(), border.label()));
                }
            }
        }
        return new BoardScene(0, 9, 9, tiles, List.of(), List.of(), -1, "Firing", List.of(), null, List.of(), borders,
              List.of(), BoardTactical.EMPTY, markers);
    }

    private static void verifyMovementVisibility(GpuFireControl control, OrthographicCamera camera,
          Pixmap empty, Pixmap visible) {
        var destination = new BoardScene.Waypoint(new Coords(4, 3), 0, 0);
        var unit = new BoardScene.Unit(1, -1, "Moving unit", destination, null, false, null, 2, false);
        BoardScene latest = scene(true, false).withUnits(List.of(unit));
        var movement = new BoardScene.Movement(1, latest.boardId(),
              List.of(new BoardScene.Waypoint(new Coords(1, 3), 0, 0), destination),
              EntityMovementType.MOVE_WALK, 0, 4, unit);
        var playback = new UnitPlayback();
        playback.accept(List.of(movement), latest, ignored -> false);
        control.update(playback.present(latest));
        assertFrame(empty, control, camera, "Ranges and letters must be absent while movement is queued");
        playback.advance(0, UnitMotion.Speed.NORMAL);
        var motion = playback.motions.get(1);
        playback.advance(motion.remainingSeconds() / (2 * UnitMotion.Speed.NORMAL.rate), UnitMotion.Speed.NORMAL);
        control.update(playback.present(latest));
        assertFrame(empty, control, camera, "Receiving the final game position must not reveal ranges mid-move");
        playback.advance(motion.remainingSeconds() / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
        control.update(playback.present(latest));
        assertFrame(visible, control, camera, "Ranges must return on completion without another game update");
    }

    private static void assertFrame(Pixmap expected, GpuFireControl control, OrthographicCamera camera, String message) {
        Pixmap actual = draw(control, camera, 0);
        try {
            assertEquals(0, changedPixels(expected, actual), message);
        } finally {
            actual.dispose();
        }
    }

    private static Pixmap draw(GpuFireControl control, OrthographicCamera camera, float seconds) {
        ScreenUtils.clear(0.025f, 0.035f, 0.05f, 1, true);
        control.render(camera, seconds);
        control.renderLabels(camera);
        return Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    private static int changedPixels(Pixmap a, Pixmap b) {
        int changed = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getPixel(x, y) != b.getPixel(x, y)) {
                    changed++;
                }
            }
        }
        return changed;
    }
}
