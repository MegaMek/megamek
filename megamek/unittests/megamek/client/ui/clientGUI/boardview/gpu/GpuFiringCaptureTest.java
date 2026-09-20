/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.io.File;
import java.util.List;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.clientGUI.boardview.sprite.SensorRangeSprite;
import megamek.client.ui.clientGUI.boardview.sprite.TextMarkerSprite;
import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.RangeType;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.loaders.MekFileParser;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.weapons.artillery.LongTom;
import megamek.common.weapons.lrms.innerSphere.ISLRM20;
import org.junit.jupiter.api.Test;

class GpuFiringCaptureTest {
    static Board board() {
        Hex[] hexes = new Hex[81];
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                hexes[y * 9 + x] = new Hex(x >= 5 ? 2 : x == 4 && y >= 3 && y <= 5 ? 3 : 0);
            }
        }
        return new Board(9, 9, hexes);
    }

    /** Real attack sprites combine a direct weapon and an indirect LRM at the same target. Swing thread only. */
    static void attacks(GpuBoardFixture fixture) {
        try {
            fixture.game.setPhase(GamePhase.FIRING);
            fixture.entity.setPosition(new Coords(2, 4));
            Entity target = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
            target.setId(42);
            target.setOwner(fixture.player);
            target.setPosition(new Coords(7, 4));
            target.setDeployed(true);
            fixture.game.addEntity(target, false);
            int direct = fixture.entity.getEquipmentNum(fixture.entity.getWeaponList().getFirst());
            fixture.view.addAttack(new WeaponAttackAction(fixture.entity.getId(), target.getId(), direct));
            fixture.view.addAttack(new WeaponAttackAction(fixture.entity.getId(), target.getId(), direct));
            fixture.game.getOptions().getOption(OptionsConstants.BASE_INDIRECT_FIRE).setValue(true);
            ISLRM20 type = new ISLRM20();
            type.adaptToGameOptions(fixture.game.getOptions());
            var indirect = fixture.entity.addEquipment(type, Mek.LOC_LEFT_ARM);
            indirect.setMode("Indirect");
            assertTrue(indirect.curMode().isIndirect());
            fixture.view.addAttack(new WeaponAttackAction(fixture.entity.getId(), target.getId(),
                  fixture.entity.getEquipmentNum(indirect)));
            assertEquals(1, fixture.view.getAttackSprites().size(), "The classic board must accept the queued attacks");
            assertEquals(3, fixture.view.getAttackSprites().getFirst().getActions().size());
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    @Test
    void nativeLinesKeepBothModesAndEntityElevationsWithoutGroundArrowPixels() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            SwingUtilities.invokeAndWait(() -> {
                attacks(fixture);
                fixture.source.refresh();
                assertEquals(1, fixture.view.getAttackSprites().size(), "Capture must preserve classic attack sprites");
                BoardScene scene = fixture.source.takeFrame().scene();
                assertEquals(2, scene.firingLines().size());
                assertEquals(List.of(false, true), scene.firingLines().stream().map(BoardScene.FiringLine::indirect).toList());
                for (var line : scene.firingLines()) {
                    assertEquals(1, line.source().elevation());
                    assertEquals(3, line.target().elevation());
                }
                fixture.view.clearAllAttacks();
                fixture.source.refresh();
                BoardScene cleared = fixture.source.takeFrame().scene();
                assertTrue(cleared.firingLines().isEmpty());
                assertEquals(scene.tiles().stream().map(BoardScene.Tile::tactical).toList(),
                      cleared.tiles().stream().map(BoardScene.Tile::tactical).toList(),
                      "Attack arrows must never be baked into per-hex ground markings");
                // An already published scene remains immutable when orders are cleared on Swing.
                assertEquals(2, scene.firingLines().size());
            });
        }
    }

    @Test
    void artilleryUsesItsAttackPhaseAndHexTargetsUseTheirTerrainHeight() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    var artillery = fixture.entity.addEquipment(new LongTom(), Entity.LOC_NONE);
                    artillery.setLocation(Mek.LOC_LEFT_ARM);
                    HexTarget target = new HexTarget(new Coords(7, 7), 0, Targetable.TYPE_HEX_ARTILLERY);
                    for (GamePhase phase : List.of(GamePhase.FIRING, GamePhase.FIRING_REPORT, GamePhase.TARGETING,
                          GamePhase.TARGETING_REPORT, GamePhase.OFFBOARD, GamePhase.OFFBOARD_REPORT)) {
                        fixture.game.setPhase(phase);
                        fixture.view.clearAllAttacks();
                        fixture.view.addAttack(new ArtilleryAttackAction(fixture.entity.getId(), target.getTargetType(),
                              target.getId(), fixture.entity.getEquipmentNum(artillery), fixture.game));
                        fixture.source.refresh();
                        var lines = fixture.source.takeFrame().scene().firingLines();
                        assertEquals(1, lines.size());
                        assertEquals(phase != GamePhase.FIRING && phase != GamePhase.FIRING_REPORT, lines.getFirst().indirect());
                        assertEquals(2.15f, lines.getFirst().target().elevation(), 0.001f);
                    }
                } catch (Exception error) {
                    throw new IllegalStateException(error);
                }
            });
        }
    }

    @Test
    void weaponBordersAndSurfaceRegionsUseTheirOwnNativeGeometry() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            SwingUtilities.invokeAndWait(() -> {
                BoardScene before = fixture.source.takeFrame().scene();
                Coords weaponHex = new Coords(2, 2), sensorHex = new Coords(6, 2), objectiveHex = new Coords(6, 6);
                FieldOfFireSprite weapon = new FieldOfFireSprite(fixture.view, RangeType.RANGE_SHORT, weaponHex, 63);
                var label = new TextMarkerSprite(fixture.view, weaponHex, RangeType.RANGE_SHORT);
                var sensor = new SensorRangeSprite(fixture.view, SensorRangeSprite.SENSORS, sensorHex, 63);
                var objective = new FieldOfFireSprite(fixture.view, Color.CYAN, objectiveHex, 63);
                fixture.view.addSprites(List.of(weapon, sensor, objective));
                fixture.source.refresh();
                BoardScene scene = fixture.source.takeFrame().scene();
                assertEquals(1, scene.rangeBorders().size());
                assertEquals(63, scene.rangeBorders().getFirst().edges());
                assertEquals(weaponHex, scene.rangeBorders().getFirst().coords());
                assertEquals("S", scene.rangeBorders().getFirst().label());
                assertEquals(before.tile(weaponHex).tactical(), scene.tile(weaponHex).tactical());
                assertEquals(before.tile(sensorHex).tactical(), scene.tile(sensorHex).tactical());
                assertEquals(before.tile(objectiveHex).tactical(), scene.tile(objectiveHex).tactical());
                assertFalse(scene.tactical().fills().isEmpty(), "Sensor and objective borders must survive as native geometry");
                fixture.view.addSprites(List.of(label));
                fixture.source.refresh();
                BoardScene withLabel = fixture.source.takeFrame().scene();
                assertEquals(before.tile(weaponHex).tactical(), withLabel.tile(weaponHex).tactical(),
                      "Camera-facing lettering must not also be baked into the terrain");
                assertEquals(scene.tactical(), withLabel.tactical());
                assertEquals(BoardView.GPU_SCROLLING_RANGE_LABELS ? List.of() : List.of(
                      new BoardScene.RangeLabel(weaponHex,
                            FieldOfFireSprite.getFieldOfFireColor(RangeType.RANGE_SHORT).getRGB(), "S")),
                      withLabel.rangeLabels());
                assertEquals(withLabel.rangeLabels(), withLabel.withUnits(List.of()).rangeLabels(),
                      "Playback must retain the range lettering");
                assertEquals(withLabel.tactical(), withLabel.withUnits(List.of()).tactical());
                Coords neighbor = weaponHex.translated(0);
                assertEquals(scene.tile(neighbor).tactical(), withLabel.tile(neighbor).tactical(),
                      "The flat marker must stay in the handler's original hex");
                weapon.setHidden(true);
                label.setHidden(true);
                fixture.source.refresh();
                assertTrue(fixture.source.takeFrame().scene().rangeBorders().isEmpty());
                assertTrue(fixture.source.takeFrame().scene().rangeLabels().isEmpty());
                assertEquals(BoardView.GPU_SCROLLING_RANGE_LABELS ? 0 : 1, withLabel.rangeLabels().size(),
                      "Hiding the Swing sprite must not mutate an already published snapshot");
                fixture.view.removeSprites(List.of(weapon, label, sensor, objective));
                fixture.source.refresh();
                assertTrue(fixture.source.takeFrame().scene().rangeBorders().isEmpty());
                assertTrue(fixture.source.takeFrame().scene().rangeLabels().isEmpty());
                assertEquals(before.tile(weaponHex).tactical(), fixture.source.takeFrame().scene().tile(weaponHex).tactical());
            });
        }
    }

    @Test
    void everyBracketPublishesItsLabelAndColorAndFlatMarkersUseTheSamePalette() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            SwingUtilities.invokeAndWait(() -> {
                List<String> labels = List.of("min", "S", "M", "L", "E");
                for (int bracket = RangeType.RANGE_MINIMUM; bracket <= RangeType.RANGE_EXTREME; bracket++) {
                    Coords coords = new Coords(bracket + 1, 2);
                    var border = new FieldOfFireSprite(fixture.view, bracket, coords, 63);
                    var marker = new TextMarkerSprite(fixture.view, coords, bracket);
                    fixture.view.addSprites(List.of(border, marker));
                    fixture.source.refresh();
                    var captured = fixture.source.takeFrame().scene().rangeBorders().stream()
                          .filter(range -> range.coords().equals(coords)).findFirst().orElseThrow();
                    assertEquals(labels.get(bracket), captured.label());
                    int rgb = FieldOfFireSprite.getFieldOfFireColor(bracket).getRGB();
                    assertEquals(rgb, captured.rgb(), "Native contours must use the shared range color");
                    if (!BoardView.GPU_SCROLLING_RANGE_LABELS) {
                        var label = fixture.source.takeFrame().scene().rangeLabels().stream()
                              .filter(range -> range.coords().equals(coords)).findFirst().orElseThrow();
                        assertEquals(captured.label(), label.label());
                        assertEquals(rgb, label.rgb(), "Flat letters and contours must use the same color");
                    }
                    border.setHidden(true);
                    fixture.source.refresh();
                    assertTrue(fixture.source.takeFrame().scene().rangeBorders().isEmpty());
                    fixture.view.removeSprites(List.of(border, marker));
                }
                Coords coords = new Coords(2, 2);
                BoardScene before = fixture.source.takeFrame().scene();
                fixture.view.addSprites(List.of(new TextMarkerSprite(fixture.view, coords, "X", Color.CYAN)));
                fixture.source.refresh();
                assertFalse(java.util.Objects.equals(before.tile(coords).tactical(),
                      fixture.source.takeFrame().scene().tile(coords).tactical()));
            });
        }
    }
}
