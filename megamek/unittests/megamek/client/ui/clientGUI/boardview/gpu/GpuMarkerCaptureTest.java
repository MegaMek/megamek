/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardMarker;
import megamek.client.ui.clientGUI.boardview.sprite.BridgeBuildSprite;
import megamek.client.ui.clientGUI.boardview.sprite.BridgeRepairedSprite;
import megamek.client.ui.clientGUI.boardview.sprite.DugInSprite;
import megamek.client.ui.clientGUI.boardview.sprite.FlareSprite;
import megamek.client.ui.clientGUI.boardview.sprite.FortifyBuildSprite;
import megamek.client.ui.clientGUI.boardview.sprite.GroundObjectSprite;
import megamek.client.ui.clientGUI.boardview.sprite.HexFlagSprite;
import megamek.client.ui.clientGUI.boardview.sprite.RubbleClearSprite;
import megamek.client.ui.clientGUI.boardview.sprite.SawClearingSprite;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.SpecialHexDisplay;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.Flare;
import megamek.common.equipment.Minefield;
import megamek.server.props.OrbitalBombardment;
import org.junit.jupiter.api.Test;

class GpuMarkerCaptureTest {
    private static final Coords ROOF = new Coords(2, 2);

    private static Board board() {
        Hex[] hexes = new Hex[42];
        Arrays.setAll(hexes, index -> new Hex(0));
        hexes[2 + 2 * 7] = new Hex(1, "building:1;bldg_elev:3;bldg_cf:30", "");
        Board board = new Board();
        board.newData(7, 6, hexes, null);
        return board;
    }

    @Test
    void capturesHandlerSymbolsAndLabelsWithOwnerColorsAndNoDuplicateFlatIcons() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            BoardScene before = fixture.source.takeFrame().scene();
            var flag = new HexFlagSprite(fixture.view, ROOF, Color.MAGENTA, "Defend", "2/3");
            List<Sprite> sprites = List.of(flag, new GroundObjectSprite(fixture.view, ROOF),
                  new FlareSprite(fixture.view, new Flare(ROOF, 0, 4, 2, Flare.F_IGNITED)),
                  new SawClearingSprite(fixture.view, ROOF, 3), new BridgeRepairedSprite(fixture.view, ROOF));
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.addSprites(sprites);
                fixture.source.refresh();
            });
            BoardScene scene = fixture.source.takeFrame().scene();
            assertEquals(5, scene.markers().size());
            assertEquals(before.tile(ROOF).tactical(), scene.tile(ROOF).tactical());
            assertTrue(scene.markers().stream().allMatch(marker -> marker.elevation() == 4));
            BoardMarker objective = scene.markers().stream().filter(marker -> marker.kind() == BoardMarker.Kind.OBJECTIVE)
                  .findFirst().orElseThrow();
            assertEquals("Defend 2/3", objective.label());
            assertEquals(Color.MAGENTA.getRGB() & 0xFFFFFF, objective.rgb());
            assertTrue(scene.markers().stream().anyMatch(marker -> marker.kind() == BoardMarker.Kind.FLARE
                  && marker.label().equals("4")));
            SwingUtilities.invokeAndWait(() -> {
                sprites.forEach(sprite -> sprite.setHidden(true));
                fixture.source.refresh();
            });
            assertTrue(fixture.source.takeFrame().scene().markers().isEmpty());
        }
    }

    @Test
    void mixedEngineeringSpritesKeepTerrainAndRestoreClassicLabelsAfterCapture() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            List<Sprite> sprites = List.of(new BridgeBuildSprite(fixture.view, ROOF, 2, 6, 9),
                  new FortifyBuildSprite(fixture.view, new Coords(3, 2), 1, 3),
                  new DugInSprite(fixture.view, new Coords(4, 2), 0.5f, "Digging in"),
                  new RubbleClearSprite(fixture.view, new Coords(3, 3), 1, 6, false),
                  new RubbleClearSprite(fixture.view, new Coords(3, 3), 1, 6, true));
            SwingUtilities.invokeAndWait(() -> {
                fixture.view.addSprites(sprites);
                sprites.forEach(Sprite::prepare);
                List<BoardScene.Pixels> classic = sprites.stream().map(GpuMarkerCaptureTest::spritePixels).toList();
                fixture.source.refresh();
                assertFalse(fixture.view.isGpuCapture());
                assertEquals(classic, sprites.stream().map(GpuMarkerCaptureTest::spritePixels).toList(),
                      "A 3D capture must restore the classic sprite artwork, including its progress text");
            });
            var markers = fixture.source.takeFrame().scene().markers();
            assertEquals(Set.of(BoardMarker.Kind.BRIDGE_BUILD, BoardMarker.Kind.FORTIFY_BUILD,
                  BoardMarker.Kind.DUG_IN, BoardMarker.Kind.RUBBLE_CLEAR), markers.stream().map(BoardMarker::kind)
                  .collect(Collectors.toSet()));
            assertEquals(4, markers.size(), "The rubble terrain and counter sprites must yield only one point marker");
            assertTrue(markers.stream().anyMatch(marker -> marker.kind() == BoardMarker.Kind.RUBBLE_CLEAR
                  && marker.label().equals("2/6")));
            assertTrue(fixture.source.takeFrame().scene().tile(ROOF).tactical() != null,
                  "The unfinished bridge's terrain preview must survive point-marker conversion");
        }
    }

    private static BoardScene.Pixels spritePixels(Sprite sprite) {
        var bounds = sprite.getBounds();
        BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        sprite.drawOnto(graphics, 0, 0, null);
        graphics.dispose();
        return new BoardScene.Pixels(image);
    }

    @Test
    void mineDetailsAndDemolitionChargesKeepTheirExistingPrivacyRules() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            Coords ownMine = new Coords(1, 1), enemyMine = new Coords(2, 1), multiple = new Coords(3, 1);
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.addMinefield(Minefield.createMinefield(ownMine, 0, Minefield.TYPE_VIBRABOMB, 20, 65));
                fixture.game.addMinefield(Minefield.createMinefield(enemyMine, 1, Minefield.TYPE_VIBRABOMB, 20, 95));
                fixture.game.addMinefield(Minefield.createMinefield(multiple, 0, Minefield.TYPE_CONVENTIONAL, 10));
                fixture.game.addMinefield(Minefield.createMinefield(multiple, 0, Minefield.TYPE_INFERNO, 20));
                fixture.game.getBoard().getBuildingAt(ROOF).addDemolitionCharge(0, 15, ROOF);
                fixture.game.getBoard().getBuildingAt(ROOF).addDemolitionCharge(1, 99, ROOF);
                fixture.source.refresh();
            });
            var markers = fixture.source.takeFrame().scene().markers();
            assertEquals(4, markers.size());
            assertTrue(markers.stream().filter(marker -> marker.coords().equals(ownMine))
                  .allMatch(marker -> marker.label().contains("65")));
            assertFalse(markers.stream().anyMatch(marker -> marker.label().contains("95") || marker.label().contains("99")));
            BoardMarker charge = markers.stream().filter(marker -> marker.kind() == BoardMarker.Kind.DEMOLITION_CHARGE)
                  .findFirst().orElseThrow();
            assertTrue(charge.label().contains("15"));
            assertEquals(4, charge.elevation());
            assertEquals(GUIPreferences.getInstance().getDemolitionChargeColor().getRGB() & 0xFFFFFF, charge.rgb());
        }
    }

    @Test
    void specialDisplaysReusePhaseOwnerAndHeatMapFiltering() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            Player enemy = new Player(1, "Enemy");
            enemy.setTeam(2);
            List<SpecialHexDisplay.Type> types = List.of(SpecialHexDisplay.Type.ARTILLERY_INCOMING,
                  SpecialHexDisplay.Type.ARTILLERY_TARGET, SpecialHexDisplay.Type.ARTILLERY_ADJUSTED,
                  SpecialHexDisplay.Type.ARTILLERY_AUTO_HIT, SpecialHexDisplay.Type.ORBITAL_BOMBARDMENT_INCOMING,
                  SpecialHexDisplay.Type.NUKE_INCOMING, SpecialHexDisplay.Type.PLAYER_NOTE);
            Coords privateNote = new Coords(4, 4), heatMap = new Coords(1, 4);
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.addPlayer(enemy.getId(), enemy);
                for (var type : types) {
                    fixture.game.getBoard().addSpecialHexDisplay(ROOF,
                          new SpecialHexDisplay(type, SpecialHexDisplay.NO_ROUND, enemy, type.name()));
                }
                fixture.game.getBoard().addSpecialHexDisplay(privateNote, new SpecialHexDisplay(
                      SpecialHexDisplay.Type.PLAYER_NOTE, SpecialHexDisplay.NO_ROUND, enemy, "Private note",
                      SpecialHexDisplay.SHD_VISIBLE_TO_OWNER));
                fixture.game.getBoard().addSpecialHexDisplay(heatMap, new SpecialHexDisplay(
                      SpecialHexDisplay.Type.PLAYER_NOTE, SpecialHexDisplay.NO_ROUND, fixture.player,
                      SpecialHexDisplay.HEAT_MAP_PREFIX + "1:2:P prediction"));
                fixture.source.refresh();
            });
            var markers = fixture.source.takeFrame().scene().markers();
            assertEquals(7, markers.size());
            assertTrue(markers.stream().allMatch(marker -> marker.coords().equals(ROOF)));
            assertFalse(markers.stream().anyMatch(marker -> marker.coords().equals(privateNote)));
            assertTrue(fixture.source.takeFrame().scene().tile(heatMap).tactical() != null
                        || !fixture.source.takeFrame().scene().tactical().fills().isEmpty(),
                  "Heat-map fills must remain terrain overlays");
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.getBoard().getSpecialHexDisplay(ROOF).clear();
                fixture.game.getBoard().addSpecialHexDisplay(ROOF, new SpecialHexDisplay(
                      SpecialHexDisplay.Type.ARTILLERY_TARGET, fixture.game.getRoundCount() - 5, enemy, "Expired"));
                fixture.source.refresh();
            });
            assertTrue(fixture.source.takeFrame().scene().markers().isEmpty(), "Expired markers must disappear");
        }
    }

    @Test
    void orbitalStrikeKeepsItsFootprintAndHasOnlyOneRaisedCenter() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            BoardScene before = fixture.source.takeFrame().scene();
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.setOrbitalBombardmentVector(new Vector<>(List.of(new OrbitalBombardment.Builder()
                      .x(3).y(3).radius(1).build())));
                fixture.source.refresh();
            });
            BoardScene after = fixture.source.takeFrame().scene();
            assertEquals(1, after.markers().size());
            assertEquals(BoardMarker.Kind.ORBITAL_INCOMING, after.markers().getFirst().kind());
            Coords neighbor = new Coords(3, 2);
            assertTrue(!java.util.Objects.equals(before.tile(neighbor).tactical(), after.tile(neighbor).tactical())
                        || !before.tactical().fills().equals(after.tactical().fills()),
                  "The raised centre must retain the blast footprint on the board");
        }
    }
}
