/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.sprite.MovementEnvelopeSprite;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.units.EntityMovementType;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Test;

class GpuBoardSourceTest {
    @Test
    void hexTextIsSeparateFromTerrainPixelsAndTracksPreferences() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        boolean coords = preferences.getCoordsEnabled();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setCoordsEnabled(true);
                fixture.source.refresh();
            });
            BoardScene.Tile labeled = fixture.source.takeFrame().scene().tile(new Coords(0, 0));
            assertTrue(labeled.text().stream().anyMatch(label -> label.text().equals("0101")));
            SwingUtilities.invokeAndWait(() -> {
                preferences.setCoordsEnabled(false);
                fixture.source.refresh();
            });
            BoardScene.Tile unlabeled = fixture.source.takeFrame().scene().tile(new Coords(0, 0));
            assertFalse(unlabeled.text().stream().anyMatch(label -> label.text().equals("0101")));
            assertTrue(samePixels(labeled.image(), unlabeled.image()));
        } finally {
            SwingUtilities.invokeAndWait(() -> preferences.setCoordsEnabled(coords));
        }
    }

    @Test
    void gpuArtworkDoesNotIncludeClassicGeneratedShadows() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        boolean shadows = preferences.getShadowMap();
        boolean ambient = preferences.getAOHexShadows();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setShadowMap(true);
                preferences.setAOHexShadows(true);
                fixture.source.refresh();
            });
            BoardScene before = fixture.source.takeFrame().scene();
            SwingUtilities.invokeAndWait(() -> {
                preferences.setShadowMap(false);
                preferences.setAOHexShadows(false);
                fixture.source.refresh();
            });
            BoardScene after = fixture.source.takeFrame().scene();
            assertTrue(samePixels(before.units().getFirst().image(), after.units().getFirst().image()));
            assertTrue(samePixels(before.tile(new Coords(0, 0)).image(), after.tile(new Coords(0, 0)).image()));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setShadowMap(shadows);
                preferences.setAOHexShadows(ambient);
            });
        }
    }

    @Test
    void annotationResolutionDoesNotDependOnClassicZoom() throws Exception {
        int originalZoom = GUIPreferences.getInstance().getMapZoomIndex();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            BoardScene.Pixels before = fixture.source.takeFrame().scene().units().getFirst().annotations();
            BoardScene.Tile tile = fixture.source.takeFrame().scene().tile(new Coords(0, 0));
            assertTrue(before.height() >= 80);
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.getComponent();
                fixture.view.zoomOut();
                float scale = fixture.view.getScale();
                Dimension size = new Dimension(fixture.view.getHexSize());
                fixture.source.refresh();
                assertEquals(scale, fixture.view.getScale());
                assertEquals(size, fixture.view.getHexSize());
            });
            assertSame(before, fixture.source.takeFrame().scene().units().getFirst().annotations());
            BoardScene.Tile after = fixture.source.takeFrame().scene().tile(new Coords(0, 0));
            assertEquals((int) BoardGeometry.WIDTH, after.image().width());
            assertEquals((int) BoardGeometry.HEIGHT, after.image().height());
            assertTrue(samePixels(tile.image(), after.image()));
            assertEquals(GpuBattleView.annotationScale(1), GpuBattleView.annotationScale(0.5f));
        } finally {
            SwingUtilities.invokeAndWait(() -> GUIPreferences.getInstance().setMapZoomIndex(originalZoom));
        }
    }

    @Test
    void capturesCurrentOccupiedHeightWithoutChangingArtwork() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            BoardScene.Unit standing = fixture.source.takeFrame().scene().units().getFirst();
            assertEquals(2, standing.height());
            SwingUtilities.invokeAndWait(() -> {
                fixture.entity.setProne(true);
                fixture.source.refresh();
            });
            BoardScene.Unit prone = fixture.source.takeFrame().scene().units().getFirst();
            assertEquals(fixture.entity.height() + 1, prone.height());
            assertEquals(1, prone.height());
            assertSame(standing.image(), prone.image());
            SwingUtilities.invokeAndWait(() -> {
                fixture.entity.setProne(false);
                fixture.entity.setWeight(150);
                fixture.source.refresh();
            });
            assertEquals(3, fixture.source.takeFrame().scene().units().getFirst().height());
        }
    }

    @Test
    void unitArtworkAndAnnotationsAreNotBakedIntoTiles() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Coords original = fixture.entity.getPosition();
            BoardScene.Tile occupied = fixture.source.takeFrame().scene().tile(original);
            SwingUtilities.invokeAndWait(() -> {
                fixture.entity.setPosition(new Coords(10, 10));
                fixture.view.redrawEntity(fixture.entity);
                fixture.source.refresh();
            });
            BoardScene.Tile empty = fixture.source.takeFrame().scene().tile(original);
            assertTrue(samePixels(occupied.image(), empty.image()));
            assertTrue(samePixels(occupied.tactical(), empty.tactical()));
        }
    }

    @Test
    void carriesRangeDeploymentAndStrafingPaintersAndRemovesStaleMarkings() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Coords coords = new Coords(3, 4);
            BoardScene.Tile before = fixture.source.takeFrame().scene().tile(coords);
            MovementEnvelopeSprite range = new MovementEnvelopeSprite(fixture.view, Color.MAGENTA, coords, 63);
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.addSprites(List.of(range));
                fixture.source.refresh();
            });
            BoardScene.Tile ranged = fixture.source.takeFrame().scene().tile(coords);
            assertTrue(before.image() != ranged.image() || before.tactical() != ranged.tactical());
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.removeSprites(List.of(range));
                fixture.player.setStartingPos(Board.START_ANY);
                fixture.view.markDeploymentHexesFor(fixture.entity);
                fixture.source.refresh();
            });
            BoardScene.Tile deployment = fixture.source.takeFrame().scene().tile(coords);
            assertFalse(samePixels(before.image(), deployment.image()));
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.markDeploymentHexesFor(null);
                fixture.game.setPhase(GamePhase.FIRING);
                fixture.view.addStrafingCoords(coords);
                fixture.source.refresh();
            });
            BoardScene.Tile strafing = fixture.source.takeFrame().scene().tile(coords);
            assertFalse(samePixels(before.tactical(), strafing.tactical()));
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.clearStrafingCoords();
                fixture.source.refresh();
            });
            BoardScene.Tile cleared = fixture.source.takeFrame().scene().tile(coords);
            assertTrue(samePixels(before.image(), cleared.image()));
            assertTrue(samePixels(before.tactical(), cleared.tactical()));
        }
    }

    @Test
    void planarCaptureRestoresClassicProjectionAndSpriteBounds() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        boolean original = preferences.getIsometricEnabled();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setIsometricEnabled(true);
                Coords coords = new Coords(7, 5);
                MovementEnvelopeSprite range = new MovementEnvelopeSprite(fixture.view, Color.CYAN, coords, 63);
                fixture.view.addSprites(List.of(range));
                range.prepare();
                Rectangle before = new Rectangle(range.getBounds());
                int offset = fixture.view.getVerticalOffset();
                Point location = fixture.view.getHexLocation(coords);
                fixture.view.capturePlanarHexes(new Rectangle(0, 0, 16, 17));
                assertEquals(offset, fixture.view.getVerticalOffset());
                assertEquals(location, fixture.view.getHexLocation(coords));
                assertEquals(before, range.getBounds());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> preferences.setIsometricEnabled(original));
        }
    }

    @Test
    void screenOverlayCapturesPixelsAndConsumesTheWholeClickGesture() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            AtomicInteger hit = new AtomicInteger();
            AtomicInteger board = new AtomicInteger();
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.addOverlay(new IDisplayable() {
                    @Override
                    public void draw(Graphics graphics, Rectangle rect) {
                        graphics.setColor(Color.CYAN);
                        graphics.fillRect(20, 30, 80, 40);
                    }

                    @Override
                    public boolean isHit(Point point, Dimension size) {
                        if (new Rectangle(20, 30, 80, 40).contains(point)) {
                            hit.incrementAndGet();
                            return true;
                        }
                        return false;
                    }
                });
                fixture.source.setViewport(200, 100);
                fixture.source.refresh();
            });
            BoardScene.Pixels hud = fixture.source.takeFrame().hud();
            assertEquals(0x00ffffff, hud.rgba(40 * hud.width() + 40));
            fixture.source.overlayInput(MouseEvent.MOUSE_PRESSED, 40, 40, board::incrementAndGet);
            fixture.source.overlayInput(MouseEvent.MOUSE_RELEASED, 40, 40, board::incrementAndGet);
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(1, hit.get());
            assertEquals(0, board.get());
            fixture.source.overlayInput(MouseEvent.MOUSE_PRESSED, 150, 80, board::incrementAndGet);
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(1, board.get());
        }
    }

    private static boolean samePixels(BoardScene.Pixels first, BoardScene.Pixels second) {
        if (first.width() != second.width() || first.height() != second.height()) {
            return false;
        }
        for (int i = 0; i < first.width() * first.height(); i++) {
            if (first.rgba(i) != second.rgba(i)) {
                return false;
            }
        }
        return true;
    }
    @Test
    void usesTilesetFacingWithoutCopyingArtworkForTorsoTwists() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            BoardScene.Unit before = fixture.source.takeFrame().scene().units().getFirst();
            SwingUtilities.invokeAndWait(() -> {
                fixture.entity.setSecondaryFacing(1);
                fixture.source.refresh();
            });
            BoardScene.Unit after = fixture.source.takeFrame().scene().units().getFirst();
            assertEquals(fixture.view.getTileManager().facingFor(fixture.entity), after.location().facing());
            assertEquals(1, after.location().facing());
            assertSame(before.image(), after.image());
        }
    }

    @Test
    void reusesAssetsUntilTerrainChangesAndCopiesPixels() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            BoardScene first = fixture.source.takeFrame().scene();
            SwingUtilities.invokeAndWait(fixture.source::refresh);
            BoardScene second = fixture.source.takeFrame().scene();
            assertSame(first.tiles(), second.tiles());
            assertSame(first.units().getFirst().image(), second.units().getFirst().image());
            assertTrue(second.units().getFirst().image().width() > 1);
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.getBoard().setHex(new Coords(0, 0), new Hex(5));
                fixture.source.refresh();
            });
            BoardScene changed = fixture.source.takeFrame().scene();
            assertNotSame(first.tiles(), changed.tiles());
            assertEquals(5, changed.tile(new Coords(0, 0)).elevation());
            assertEquals(0, first.tile(new Coords(0, 0)).elevation());
        }
    }

    @Test
    void hiddenEnemiesAndSensorContactsUseExistingVisibilityRules() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                Player enemy = new Player(1, "Opponent");
                enemy.setTeam(2);
                fixture.game.addPlayer(enemy.getId(), enemy);
                fixture.entity.setOwner(enemy);
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS).setValue(true);
                fixture.source.refresh();
            });
            assertTrue(fixture.source.takeFrame().scene().units().isEmpty());
            SwingUtilities.invokeAndWait(() -> {
                fixture.entity.addBeenDetectedBy(fixture.player);
                fixture.source.refresh();
            });
            BoardScene.Unit contact = fixture.source.takeFrame().scene().units().getFirst();
            assertTrue(contact.sensorContact());
            assertEquals(1, contact.height());
            assertEquals(Messages.getString("BoardView1.sensorReturn"), contact.name());
            assertEquals(0, contact.location().facing());
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_HIDDEN_UNITS).setValue(true);
                fixture.entity.setHidden(true);
                fixture.source.refresh();
            });
            assertTrue(fixture.source.takeFrame().scene().units().isEmpty());
        }
    }

    @Test
    void actionsUseOriginalButtonsAndRejectDisabledOrOldPhaseControls() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Runnable action = fixture.source.takeFrame().scene().commands().getFirst().action();
            action.run();
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(1, fixture.clicks.get());
            SwingUtilities.invokeAndWait(() -> fixture.button.setEnabled(false));
            action.run();
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(1, fixture.clicks.get());
            SwingUtilities.invokeAndWait(() -> {
                fixture.button.setEnabled(true);
                fixture.panel = new JPanel();
            });
            action.run();
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(1, fixture.clicks.get());
        }
    }

    @Test
    void forwardsBoardCoordinatesAndDoesNotDispatchAfterClose() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            AtomicReference<BoardViewEvent> received = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> fixture.view.addBoardViewListener(new BoardViewListenerAdapter() {
                @Override
                public void hexMoused(BoardViewEvent event) {
                    received.set(event);
                }
            }));
            Coords target = new Coords(3, 4);
            fixture.source.click(target, false, 0);
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(target, received.get().getCoords());
            assertEquals(BoardViewEvent.BOARD_HEX_CLICKED, received.get().getType());
            assertEquals(0, received.get().getModifiers());
            received.set(null);
            SwingUtilities.invokeAndWait(fixture.source::close);
            fixture.source.click(target, false, 0);
            SwingUtilities.invokeAndWait(() -> { });
            assertTrue(fixture.source.isClosed());
            assertEquals(null, received.get());
        }
    }

    @Test
    void publishesMovementOnceAndDoesNotConsumeTheGamePath() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Vector<UnitLocation> path = new Vector<>();
            path.add(new UnitLocation(fixture.entity.getId(), new Coords(5, 5), 0, 0, 0));
            path.add(new UnitLocation(fixture.entity.getId(), new Coords(6, 5), 1, 0, 0));
            SwingUtilities.invokeAndWait(() -> {
                fixture.entity.moved = EntityMovementType.MOVE_JUMP;
                fixture.entity.setPosition(new Coords(6, 5));
                fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, fixture.entity, path));
            });
            SwingUtilities.invokeAndWait(() -> { });
            GpuBoardSource.Frame frame = fixture.source.takeFrame();
            assertEquals(1, frame.movements().size());
            assertEquals(EntityMovementType.MOVE_JUMP, frame.movements().getFirst().type());
            assertEquals(2, frame.movements().getFirst().path().size());
            assertEquals(2, path.size());
            assertEquals(new Coords(6, 5), frame.scene().units().getFirst().location().coords());
            assertTrue(fixture.source.takeFrame().movements().isEmpty());
            assertFalse(frame.movements().getFirst().path().isEmpty());
            assertEquals(new Coords(6, 5), fixture.entity.getPosition());
        }
    }
}
