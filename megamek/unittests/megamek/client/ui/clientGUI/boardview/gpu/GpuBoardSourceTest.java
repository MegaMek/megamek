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
import java.awt.image.BufferedImage;
import java.io.File;
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
import megamek.client.ui.clientGUI.boardview.sprite.CursorSprite;
import megamek.client.ui.clientGUI.boardview.sprite.MovementEnvelopeSprite;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.PlayerColour;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.SpecialHexDisplay;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.loaders.MekFileParser;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GpuBoardSourceTest {
    @Test
    void deploymentEcmFillsTheWholeHexForFriendlyAndEnemyUnits() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            fixture.addEcm();
            Player enemy = new Player(1, "Enemy deployment");
            enemy.setTeam(2);
            for (Player owner : List.of(fixture.player, enemy)) {
                SwingUtilities.invokeAndWait(() -> {
                    if (owner == enemy) {
                        fixture.game.addPlayer(enemy.getId(), enemy);
                    }
                    fixture.entity.setOwner(owner);
                    owner.setStartingPos(Board.START_ANY);
                    fixture.game.setPhase(GamePhase.DEPLOYMENT);
                    fixture.view.markDeploymentHexesFor(fixture.entity);
                    fixture.view.updateEcmList();
                    fixture.source.refresh();
                });
                BoardScene.Pixels markings = fixture.source.takeFrame().scene().tile(new Coords(4, 4)).tactical();
                assertTrue(markings != null);
                for (Point point : List.of(new Point(42, 36), new Point(42, 12), new Point(42, 60),
                      new Point(14, 36), new Point(70, 36))) {
                    int x = point.x * markings.width() / 84;
                    int y = point.y * markings.height() / 72;
                    assertTrue((markings.rgba(y * markings.width() + x) & 255) > 0,
                          "ECM must cover the entire hex, not an unscaled corner patch: " + owner.getName() + " at " + point);
                }
            }
        }
    }

    @Test
    void measurementOutlineMatchesTheMarkedHexAtCaptureResolution() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Coords coords = new Coords(4, 4);
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.checkLOS(coords);
                fixture.source.refresh();
            });
            BoardScene.Pixels markings = fixture.source.takeFrame().scene().tile(coords).tactical();
            Rectangle outline = null;
            for (int y = 0; y < markings.height(); y++) {
                for (int x = 0; x < markings.width(); x++) {
                    int pixel = markings.rgba(y * markings.width() + x);
                    if ((pixel >>> 24) > 220 && ((pixel >>> 16) & 255) < 40
                          && ((pixel >>> 8) & 255) < 40 && (pixel & 255) > 0) {
                        if (outline == null) {
                            outline = new Rectangle(x, y, 1, 1);
                        } else {
                            outline.add(x, y);
                        }
                    }
                }
            }
            assertTrue(outline != null, "The measurement cursor must be captured");
            assertTrue(outline.width >= markings.width() - 6, "The red cursor must reach both sides of the marked hex");
            assertTrue(outline.height >= markings.height() - 6, "The red cursor must reach the top and bottom edges");
        }
    }

    @Test
    void buildingRoofUsesTheIndependentSaxarbaSelectionAndItsHeightLabelIsRaised() throws Exception {
        var preferences = PreferenceManager.getClientPreferences();
        String originalTileset = preferences.getMapTileset();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setMapTileset("classic.tileset");
                Coords coords = new Coords(2, 2);
                Hex hex = new Hex(0);
                hex.addTerrain(new Terrain(Terrains.BUILDING, 4, true, 0));
                hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, 3));
                hex.addTerrain(new Terrain(Terrains.BLDG_CF, 100));
                fixture.game.getBoard().setHex(coords, hex);
                fixture.source.refresh();
                var tile = fixture.source.takeFrame().scene().tile(coords);
                HexTileset tileset = new HexTileset(fixture.game, new File(Configuration.dataDir(), "models/board/tileset"));
                try {
                    tileset.loadFromFile("saxarba.tileset");
                } catch (java.io.IOException error) {
                    throw new IllegalStateException(error);
                }
                var sources = tileset.getSupers(fixture.game.getBoard().getHex(coords)).stream()
                      .map(tileset::imageSource).map(path -> "buildings/" + path.substring(0, path.lastIndexOf('.'))).toList();
                assertEquals(1, tile.features().size());
                assertTrue(sources.contains(tile.features().getFirst().asset()));
                assertTrue(tile.text().stream().anyMatch(label -> label.elevation() == 3));
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> preferences.setMapTileset(originalTileset));
        }
    }

    @Test
    void coexistingStructuresUseTheirSelectedSaxarbaArtworkAndOwnHeights() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                Coords coords = new Coords(2, 2);
                Hex hex = new Hex(0);
                hex.addTerrain(new Terrain(Terrains.BUILDING, 4, true, 0));
                hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, 4));
                hex.addTerrain(new Terrain(Terrains.BLDG_CF, 100));
                hex.addTerrain(new Terrain(Terrains.FUEL_TANK, 2, true, 0));
                hex.addTerrain(new Terrain(Terrains.FUEL_TANK_ELEV, 2));
                hex.addTerrain(new Terrain(Terrains.FUEL_TANK_CF, 40));
                hex.addTerrain(new Terrain(Terrains.FUEL_TANK_MAGN, 100));
                hex.addTerrain(new Terrain(Terrains.INDUSTRIAL, 3));
                fixture.game.getBoard().setHex(coords, hex);
                fixture.source.refresh();
                var tile = fixture.source.takeFrame().scene().tile(coords);
                assertEquals(3, tile.features().size());
                // Default single-hex buildings may select a prefab rather than the connected-building family.
                HexTileset tileset = new HexTileset(fixture.game, new File(Configuration.dataDir(), "models/board/tileset"));
                try {
                    tileset.loadFromFile("saxarba.tileset");
                } catch (java.io.IOException error) {
                    throw new IllegalStateException(error);
                }
                var buildingSources = tileset.getSupers(fixture.game.getBoard().getHex(coords)).stream()
                      .filter(image -> tileset.imageHasTerrain(image, Terrains.BUILDING))
                      .map(tileset::imageSource).map(path -> "buildings/" + path.substring(0, path.lastIndexOf('.'))).toList();
                assertTrue(tile.features().stream().anyMatch(feature -> buildingSources.contains(feature.asset())
                      && feature.height() == 4 && feature.kind() == BoardScene.FeatureKind.BUILDING));
                assertTrue(tile.features().stream().anyMatch(feature -> feature.asset().contains("/fuel_tanks/fuel_tank_medium_")
                      && feature.height() == 2 && feature.kind() == BoardScene.FeatureKind.PROP));
                assertTrue(tile.features().stream().anyMatch(feature -> feature.asset().contains("/misc/heavy_industrial_")
                      && feature.height() == 3 && feature.kind() == BoardScene.FeatureKind.PROP));
            });
        }
    }

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
            assertTrue(samePixels(labeled.ground(), unlabeled.ground()));
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
            assertTrue(samePixels(before.tile(new Coords(0, 0)).ground(), after.tile(new Coords(0, 0)).ground()));
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
            assertEquals((int) BoardGeometry.TILE_WIDTH, after.ground().width());
            assertEquals((int) BoardGeometry.TILE_HEIGHT, after.ground().height());
            assertTrue(samePixels(tile.ground(), after.ground()));
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
    void airborneMeeplesFloatAtTheirFlightHeight() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Entity vtol = new MekFileParser(new File("testresources/megamek/common/units/Cobra Transport VTOL.blk"))
                  .getEntity();
            Entity fighter = new MekFileParser(new File("testresources/megamek/common/units/Cheetah F-11.blk"))
                  .getEntity();
            vtol.setId(2);
            fighter.setId(3);
            SwingUtilities.invokeAndWait(() -> {
                for (Entity entity : List.of(vtol, fighter)) {
                    entity.setOwner(fixture.player);
                    entity.setDeployed(true);
                    fixture.game.addEntity(entity, false);
                }
                vtol.setPosition(new Coords(4, 4));
                vtol.setElevation(3);
                fighter.setPosition(new Coords(6, 6));
                fighter.setAltitude(5);
                fixture.source.refresh();
            });
            BoardScene scene = fixture.source.takeFrame().scene();
            BoardScene.Unit flyingVtol = unit(scene, 2);
            assertTrue(flyingVtol.airborne(), "A VTOL at elevation 3 flies");
            assertEquals(1, flyingVtol.height(), "A flying VTOL keeps its own one-level token");
            assertEquals(3 + fixture.game.getBoard().getHex(new Coords(4, 4)).getLevel(),
                  flyingVtol.location().elevation(), 0.001f, "A VTOL floats on its hex-relative elevation");
            BoardScene.Unit flyingFighter = unit(scene, 3);
            assertTrue(flyingFighter.airborne(), "A fighter at altitude 5 flies");
            assertEquals(1, flyingFighter.height());
            assertEquals(5, flyingFighter.location().elevation(), 0.001f,
                  "Aerospace altitude is absolute; the airborne elevation sentinel is never drawn");

            SwingUtilities.invokeAndWait(() -> {
                vtol.setElevation(0);
                ((Aero) fighter).land();
                fixture.source.refresh();
            });
            BoardScene landed = fixture.source.takeFrame().scene();
            assertFalse(unit(landed, 2).airborne(), "A landed VTOL does not float");
            assertFalse(unit(landed, 3).airborne(), "A landed fighter does not float");
            assertEquals(fixture.game.getBoard().getHex(new Coords(4, 4)).getLevel(),
                  unit(landed, 2).location().elevation(), 0.001f);
            assertEquals(fixture.game.getBoard().getHex(new Coords(6, 6)).getLevel(),
                  unit(landed, 3).location().elevation(), 0.001f);
        }
    }

    private static BoardScene.Unit unit(BoardScene scene, int id) {
        return scene.units().stream().filter(unit -> unit.id() == id).findFirst().orElseThrow();
    }

    @Test
    void airborneFighterMovementPlaysAtItsAltitude() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Entity fighter = new MekFileParser(new File("testresources/megamek/common/units/Cheetah F-11.blk"))
                  .getEntity();
            fighter.setId(4);
            SwingUtilities.invokeAndWait(() -> {
                fighter.setOwner(fixture.player);
                fighter.setDeployed(true);
                fixture.game.addEntity(fighter, false);
                fighter.setPosition(new Coords(6, 6));
                fighter.setAltitude(5);
                fixture.source.refresh();
            });
            // Path steps of a flying aerospace carry the airborne elevation sentinel, as the server sends them.
            Vector<UnitLocation> path = new Vector<>();
            path.add(new UnitLocation(4, new Coords(6, 6), 0, Aero.AERO_EFFECTIVE_ELEVATION, 0));
            path.add(new UnitLocation(4, new Coords(7, 6), 1, Aero.AERO_EFFECTIVE_ELEVATION, 0));
            SwingUtilities.invokeAndWait(() -> {
                fighter.setPosition(new Coords(7, 6));
                fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, fighter, path));
            });
            SwingUtilities.invokeAndWait(() -> { });
            GpuBoardSource.Frame frame = fixture.source.takeFrame();
            assertEquals(1, frame.movements().size());
            for (BoardScene.Waypoint point : frame.movements().getFirst().path()) {
                assertEquals(5, point.elevation(), 0.001f,
                      "A flying fighter's path plays at its altitude, not the airborne sentinel");
            }
        }
    }

    @Test
    void landingFighterPlaybackDescendsFromItsFlightAltitude() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Entity fighter = new MekFileParser(new File("testresources/megamek/common/units/Cheetah F-11.blk"))
                  .getEntity();
            Entity flying = new MekFileParser(new File("testresources/megamek/common/units/Cheetah F-11.blk"))
                  .getEntity();
            fighter.setId(5);
            flying.setId(5);
            flying.setPosition(new Coords(5, 5));
            flying.setAltitude(5);
            SwingUtilities.invokeAndWait(() -> {
                fighter.setOwner(fixture.player);
                fighter.setDeployed(true);
                fixture.game.addEntity(fighter, false);
                fighter.setPosition(new Coords(6, 6));
                ((Aero) fighter).land();
                fixture.source.refresh();
            });
            // The whole move carries the airborne sentinel except the step that sets the fighter down.
            Vector<UnitLocation> path = new Vector<>();
            path.add(new UnitLocation(5, new Coords(5, 5), 0, Aero.AERO_EFFECTIVE_ELEVATION, 0));
            path.add(new UnitLocation(5, new Coords(6, 5), 1, Aero.AERO_EFFECTIVE_ELEVATION, 0));
            path.add(new UnitLocation(5, new Coords(6, 6), 1, 0, 0));
            SwingUtilities.invokeAndWait(() -> fixture.game.fireGameEvent(
                  new GameEntityChangeEvent(fixture.game, fighter, path, flying)));
            SwingUtilities.invokeAndWait(() -> { });
            GpuBoardSource.Frame frame = fixture.source.takeFrame();
            assertEquals(1, frame.movements().size());
            List<BoardScene.Waypoint> played = frame.movements().getFirst().path();
            assertEquals(5, played.getFirst().elevation(), 0.001f,
                  "A landing move starts at the altitude the fighter flew at");
            for (BoardScene.Waypoint point : played) {
                assertTrue(point.elevation() < Aero.AERO_EFFECTIVE_ELEVATION,
                      "No playback point keeps the airborne sentinel");
            }
            assertEquals(fixture.game.getBoard().getHex(new Coords(6, 6)).getLevel(),
                  played.getLast().elevation(), 0.001f, "The landing step plays at its hex elevation");
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
            assertTrue(samePixels(occupied.ground(), empty.ground()));
            assertTrue(samePixels(occupied.tactical(), empty.tactical()));
        }
    }

    @Test
    void movingUnitGhostIsNotBakedIntoTiles() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        boolean moveStep = preferences.getShowMoveStep();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Coords destination = new Coords(6, 5);
            BoardScene.Tile before = fixture.source.takeFrame().scene().tile(destination);
            Vector<UnitLocation> path = new Vector<>();
            path.add(new UnitLocation(fixture.entity.getId(), fixture.entity.getPosition(), 0, 0, 0));
            path.add(new UnitLocation(fixture.entity.getId(), destination, 1, 0, 0));
            SwingUtilities.invokeAndWait(() -> {
                preferences.setShowMoveStep(true);
                fixture.entity.setPosition(destination);
                fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, fixture.entity, path));
                fixture.source.refresh();
            });
            BoardScene.Tile after = fixture.source.takeFrame().scene().tile(destination);
            assertTrue(samePixels(before.ground(), after.ground()),
                  "The classic moving-unit ghost must not be baked into the captured terrain");
            assertTrue(samePixels(before.tactical(), after.tactical()),
                  "The classic moving-unit ghost must not be baked into the captured layers");
        } finally {
            SwingUtilities.invokeAndWait(() -> preferences.setShowMoveStep(moveStep));
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
            assertTrue(before.ground() != ranged.ground() || before.tactical() != ranged.tactical());
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.removeSprites(List.of(range));
                fixture.player.setStartingPos(Board.START_ANY);
                fixture.view.markDeploymentHexesFor(fixture.entity);
                fixture.source.refresh();
            });
            BoardScene.Tile deployment = fixture.source.takeFrame().scene().tile(coords);
            assertFalse(samePixels(before.tactical(), deployment.tactical()),
                  "Deployment markings belong to the tactical layer, not the terrain artwork");
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
            assertTrue(samePixels(before.ground(), cleared.ground()));
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
                CursorSprite cursor = new CursorSprite(fixture.view, Color.RED);
                cursor.setHexLocation(coords);
                fixture.view.addSprites(List.of(range, cursor));
                range.prepare();
                cursor.prepare();
                Rectangle before = new Rectangle(range.getBounds());
                Rectangle cursorBefore = new Rectangle(cursor.getBounds());
                int offset = fixture.view.getVerticalOffset();
                Point location = fixture.view.getHexLocation(coords);
                fixture.view.capturePlanarHexes(new Rectangle(0, 0, 16, 17));
                assertEquals(offset, fixture.view.getVerticalOffset());
                assertEquals(location, fixture.view.getHexLocation(coords));
                assertEquals(before, range.getBounds());
                assertEquals(cursorBefore, cursor.getBounds());
                assertEquals(fixture.view.getHexSize(), cursor.getBounds().getSize());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> preferences.setIsometricEnabled(original));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = { 1, 1.5, 2 })
    void screenOverlayCapturesNativePixelsAndConsumesTheWholeClickGesture(double density) throws Exception {
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
                            fixture.view.centerOnHex(fixture.entity.getPosition());
                            return true;
                        }
                        return false;
                    }
                });
                fixture.source.setViewport(200, 100, (int) (200 * density), (int) (100 * density));
                fixture.source.refresh();
            });
            BoardScene.Pixels hud = fixture.source.takeFrame().hud();
            assertEquals((int) (200 * density), hud.width());
            assertEquals((int) (100 * density), hud.height());
            assertEquals(0x00ffffff, hud.rgba((int) (40 * density) * hud.width() + (int) (40 * density)));
            assertEquals(0, hud.rgba((int) (20 * density) * hud.width() + (int) (40 * density)));
            fixture.source.overlayInput(MouseEvent.MOUSE_PRESSED, 40, 40, board::incrementAndGet);
            fixture.source.overlayInput(MouseEvent.MOUSE_RELEASED, 40, 40, board::incrementAndGet);
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(1, hit.get());
            assertEquals(0, board.get());
            assertEquals(fixture.view.getCenterRequest(), fixture.source.takeFrame().centerRequest(),
                  "An overlay focus request must be published immediately with its handled input");
            fixture.source.overlayInput(MouseEvent.MOUSE_PRESSED, 150, 80, board::incrementAndGet);
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(1, board.get());
        }
    }

    @Test
    void groundIgnoresLooseSurfaceDecals() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Coords coords = new Coords(0, 16);
            AtomicReference<BufferedImage> plain = new AtomicReference<>();
            AtomicReference<BufferedImage> rough = new AtomicReference<>();
            AtomicReference<BufferedImage> rubble = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                Hex hex = fixture.game.getBoard().getHex(coords);
                hex.setTheme("desert");
                fixture.view.clearHexImageCache();
                plain.set(groundArt(fixture, coords));
                hex.addTerrain(new Terrain(Terrains.ROUGH, 1));
                fixture.view.clearHexImageCache();
                rough.set(groundArt(fixture, coords));
                hex.removeTerrain(Terrains.ROUGH);
                hex.addTerrain(new Terrain(Terrains.RUBBLE, 1));
                fixture.view.clearHexImageCache();
                rubble.set(groundArt(fixture, coords));
                hex.removeTerrain(Terrains.RUBBLE);
            });
            assertTrue(samePixels(new BoardScene.Pixels(plain.get()), new BoardScene.Pixels(rough.get())),
                  "Loose rock must not reach the ground artwork");
            assertTrue(samePixels(new BoardScene.Pixels(plain.get()), new BoardScene.Pixels(rubble.get())),
                  "Rubble must not reach the ground artwork");
        }
    }

    @Test
    void waterDepthIsCopiedWithoutBakingTheWaterIntoTheGround() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Coords coords = new Coords(3, 2);
            SwingUtilities.invokeAndWait(fixture.source::refresh);
            BoardScene.Tile tile = fixture.source.takeFrame().scene().tile(coords);
            assertTrue(tile.water());
            assertEquals(fixture.game.getBoard().getHex(coords).terrainLevel(Terrains.WATER), tile.waterDepth());
            float bed = BoardGeometry.groundZ(tile);
            SwingUtilities.invokeAndWait(() -> {
                Hex hex = fixture.game.getBoard().getHex(coords).duplicate();
                hex.addTerrain(new Terrain(Terrains.ICE, 1));
                fixture.game.getBoard().setHex(coords, hex);
                fixture.source.refresh();
            });
            BoardScene.Tile frozen = fixture.source.takeFrame().scene().tile(coords);
            assertTrue(frozen.frozen());
            assertEquals(tile.waterDepth(), frozen.waterDepth(), "Ice must preserve the riverbed");
            assertEquals(bed, BoardGeometry.groundZ(frozen));
            assertEquals(frozen.elevation() * BoardGeometry.LEVEL, BoardGeometry.surfaceZ(frozen));
        }
    }

    @Test
    void specialHexMarkersRemainTacticalAndClearWithoutChangingTheTerrain() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Coords coords = new Coords(3, 4);
            BoardScene.Tile before = fixture.source.takeFrame().scene().tile(coords);
            SpecialHexDisplay marker = SpecialHexDisplay.createArtyAutoHit(fixture.player);
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.getBoard().addSpecialHexDisplay(coords, marker, true);
                fixture.source.refresh();
            });
            BoardScene.Tile marked = fixture.source.takeFrame().scene().tile(coords);
            assertTrue(samePixels(before.ground(), marked.ground()));
            assertFalse(samePixels(before.tactical(), marked.tactical()));
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.getBoard().removeSpecialHexDisplay(coords, marker, true);
                fixture.source.refresh();
            });
            assertTrue(samePixels(before.tactical(), fixture.source.takeFrame().scene().tile(coords).tactical()));
        }
    }

    private static BufferedImage groundArt(GpuBoardFixture fixture, Coords coords) {
        Rectangle area = new Rectangle(coords.getX(), coords.getY(), 1, 1);
        return fixture.view.capturePlanarHexes(area).getFirst().terrain();
    }

    /**
     * Feature heights belong to the feature that carries them: a building's stories and a woods canopy raise
     * their own models, while painted incline and cliff shading is replaced by actual terrain faces.
     */
    @Test
    void featuresUseAuthoredModelsAndTheirOwnHeights() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            Coords building = new Coords(0, 0);
            Coords woods = new Coords(1, 0);
            Coords cliff = new Coords(2, 0);
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.getBoard().setHex(building, hexWith(fixture, building,
                      new Terrain(Terrains.BUILDING, 1), new Terrain(Terrains.BLDG_ELEV, 3),
                      new Terrain(Terrains.BLDG_CF, 15)));
                fixture.game.getBoard().setHex(woods, hexWith(fixture, woods, new Terrain(Terrains.WOODS, 2),
                      new Terrain(Terrains.FOLIAGE_ELEV, 2)));
                fixture.game.getBoard().setHex(cliff, new Hex(2));
                fixture.source.refresh();
            });
            assertTrue(fixture.game.getBoard().getHex(cliff).containsAnyTerrainOf(Terrains.INCLINE_TOP,
                  Terrains.INCLINE_BOTTOM, Terrains.INCLINE_HIGH_TOP, Terrains.INCLINE_HIGH_BOTTOM,
                  Terrains.CLIFF_BOTTOM), "The board must shade the hex beside a drop by itself");
            BoardScene scene = fixture.source.takeFrame().scene();
            assertTrue(scene.tile(cliff).features().isEmpty());
            assertFalse(hasOpaque(scene.tile(cliff).decals()), "Painted slopes must not duplicate the 3D faces");
            assertTrue(scene.tile(building).features().stream()
                  .allMatch(feature -> feature.asset().startsWith("building") && feature.height() == 3));
            assertTrue(scene.tile(woods).features().size() >= 4);
            assertTrue(scene.tile(woods).features().stream()
                  .allMatch(feature -> feature.height() <= 2 && feature.height() > 1
                        && feature.kind() == BoardScene.FeatureKind.TREE));
        }
    }

    private static boolean hasOpaque(BoardScene.Pixels pixels) {
        if (pixels == null) {
            return false;
        }
        for (int index = 0; index < pixels.width() * pixels.height(); index++) {
            if ((pixels.rgba(index) & 0xff) > 0) {
                return true;
            }
        }
        return false;
    }

    /** The hex's own terrain plus the given layers, so the capture sees them without a board listener. */
    private static Hex hexWith(GpuBoardFixture fixture, Coords coords, Terrain... terrains) {
        Hex hex = fixture.game.getBoard().getHex(coords).duplicate();
        for (Terrain terrain : terrains) {
            hex.addTerrain(terrain);
        }
        return hex;
    }

    private static boolean samePixels(BoardScene.Pixels first, BoardScene.Pixels second) {
        if (first == null || second == null) {
            return first == second;
        }
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
    void unitOutlinesFollowTeamAndPlayerColorPreferences() throws Exception {
        GUIPreferences preferences = GUIPreferences.getInstance();
        boolean teamColoring = preferences.getTeamColoring();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                Player ally = new Player(1, "Ally");
                ally.setTeam(fixture.player.getTeam());
                Player enemy = new Player(2, "Enemy");
                enemy.setTeam(2);
                enemy.setColour(PlayerColour.RED);
                fixture.game.addPlayer(ally.getId(), ally);
                fixture.game.addPlayer(enemy.getId(), enemy);
                preferences.setTeamColoring(true);
                for (Player owner : List.of(fixture.player, ally, enemy)) {
                    fixture.entity.setOwner(owner);
                    fixture.source.refresh();
                    Color expected = owner == fixture.player ? preferences.getMyUnitColor()
                          : owner == ally ? preferences.getAllyUnitColor() : preferences.getEnemyUnitColor();
                    assertEquals(expected.getRGB(), fixture.source.takeFrame().scene().units().getFirst().outlineRgb());
                }
                preferences.setTeamColoring(false);
                fixture.source.refresh();
                assertEquals(enemy.getColour().getColour(false).getRGB(),
                      fixture.source.takeFrame().scene().units().getFirst().outlineRgb());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> preferences.setTeamColoring(teamColoring));
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
            assertEquals(Color.LIGHT_GRAY.getRGB(), contact.outlineRgb(), "Sensor contacts must not reveal team colors");
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
