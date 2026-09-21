/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.LabelDisplayStyle;
import megamek.client.ui.clientGUI.boardview.overlay.UnitOverviewOverlay;
import megamek.client.ui.clientGUI.boardview.sprite.EntitySprite;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.loaders.MekFileParser;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GpuUnitHudTest {
    @ParameterizedTest
    @ValueSource(doubles = { 1, 1.5, 2 })
    void enemyStripSharesCardsAndNavigationWithoutExposingHiddenUnitsOrSensorDetails(double density) throws Exception {
        var prefs = GUIPreferences.getInstance();
        boolean shown = prefs.getShowUnitOverview();
        try (var fixture = GpuBoardFixture.create()) {
            var units = addUnits(fixture, 4);
            SwingUtilities.invokeAndWait(() -> {
                prefs.setShowUnitOverview(true);
                ClientGUI gui = gui(fixture);
                var own = new UnitOverviewOverlay(gui);
                var enemies = new UnitOverviewOverlay(gui, true);
                try {
                    Player enemy = new Player(2, "Enemy");
                    enemy.setTeam(2);
                    fixture.game.addPlayer(enemy.getId(), enemy);
                    units.forEach(unit -> unit.setOwner(enemy));
                    units.get(0).addBeenSeenBy(fixture.player);
                    units.get(1).addBeenDetectedBy(fixture.player);
                    units.get(3).addBeenSeenBy(fixture.player);
                    units.get(3).setBoardId(1);
                    fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
                    fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS).setValue(true);
                    fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_HIDDEN_UNITS).setValue(true);
                    fixture.view.addOverlay(own);
                    fixture.view.addOverlay(enemies);
                    Dimension logical = new Dimension(800, 400);
                    Dimension pixels = new Dimension((int) (800 * density), (int) (400 * density));
                    var cards = fixture.view.captureOverlayLayers(logical, pixels);
                    assertEquals(3, cards.size(), "Own unit plus the visible enemy and sensor contact, on this board only");
                    assertTrue(cards.get(0).x() > 700 * density);
                    assertTrue(cards.get(1).x() < 10 * density);
                    assertEquals(cards.get(1).x(), cards.get(2).x());
                    fixture.source.setViewport(logical.width, logical.height, pixels.width, pixels.height);
                    fixture.source.refresh();
                    var hud = fixture.source.takeFrame().hud();
                    assertEquals(66 * density, hud.leftPanelInset(), .01);
                    assertEquals(hud.sidePanelInset(), hud.leftPanelInset());

                    assertTrue(enemies.isHit(new Point(30, 25), logical));
                    verify(gui).centerOnUnit(units.get(0));
                    assertTrue(enemies.isHit(new Point(30, 75), logical));
                    verify(gui).centerOnUnit(units.get(1));
                    units.get(1).setProne(true);
                    units.get(1).heat = 25;
                    units.get(1).setArmor(0, Mek.LOC_CENTER_TORSO);
                    var changed = fixture.view.captureOverlayLayers(logical, pixels);
                    assertSame(cards.get(2).image(), changed.get(2).image(),
                          "Sensor portraits must not contain the unit's identity, armor, heat or condition");
                    units.get(1).addBeenSeenBy(fixture.player);
                    assertNotSame(changed.get(2).image(), fixture.view.captureOverlayLayers(logical, pixels).get(2).image());

                    clearInvocations(gui);
                    units.get(0).setHidden(true);
                    assertTrue(enemies.isHit(new Point(30, 25), logical));
                    verify(gui, never()).centerOnUnit(units.get(0));
                    units.get(1).setHidden(true);
                    fixture.source.refresh();
                    assertEquals(0, fixture.source.takeFrame().hud().leftPanelInset());
                    assertFalse(enemies.isDragged(new Point(30, 25), logical));
                    assertTrue(fixture.view.sidePanelInset() > 0, "The friendly strip stays independent");
                } finally {
                    prefs.removePreferenceChangeListener(own);
                    prefs.removePreferenceChangeListener(enemies);
                    prefs.setShowUnitOverview(shown);
                }
            });
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = { 1, 1.5, 2 })
    void overviewUsesSmallIndependentImagesAndKeepsClicksAligned(double density) throws Exception {
        GUIPreferences prefs = GUIPreferences.getInstance();
        boolean wasVisible = prefs.getShowUnitOverview();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            List<Entity> units = addUnits(fixture, 1);
            SwingUtilities.invokeAndWait(() -> {
                prefs.setShowUnitOverview(true);
                ClientGUI gui = gui(fixture);
                UnitOverviewOverlay overview = new UnitOverviewOverlay(gui);
                try {
                    fixture.view.addOverlay(overview);
                    fixture.source.refresh();
                    var initialHud = fixture.source.takeFrame().hud();
                    assertEquals(1, initialHud.width());
                    assertEquals(0, initialHud.sidePanelInset(), "No sidebar layout exists before the native viewport arrives");
                    assertTrue(initialHud.layers().isEmpty());
                    Dimension logical = new Dimension(800, 400);
                    Dimension nativeSize = new Dimension((int) (800 * density), (int) (400 * density));
                    var before = fixture.view.captureOverlayLayers(logical, nativeSize);
                    assertEquals(2, before.size());
                    for (var layer : before) {
                        assertTrue(layer.image().getWidth() <= 64 * density);
                        assertTrue(layer.image().getHeight() <= 56 * density);
                    }
                    fixture.source.setViewport(logical.width, logical.height, nativeSize.width, nativeSize.height);
                    fixture.source.refresh();
                    var pixels = fixture.source.takeFrame().hud();
                    var card = pixels.layers().getFirst();
                    float outsideGap = pixels.width() - card.x() - card.pixels().width();
                    assertEquals(outsideGap, card.x() - (pixels.width() - pixels.sidePanelInset()), 1,
                          "The captured sidebar reservation includes a matching inner gap at every pixel density");
                    fixture.source.refresh();
                    assertSame(pixels.layers().getFirst().pixels(), fixture.source.takeFrame().hud().layers().getFirst().pixels());

                    fixture.entity.heat = 15;
                    var changed = fixture.view.captureOverlayLayers(logical, nativeSize);
                    assertNotSame(before.getFirst().image(), changed.getFirst().image());
                    assertSame(before.get(1).image(), changed.get(1).image(), "Changing one unit must not repaint its neighbour");

                    var wider = fixture.view.captureOverlayLayers(new Dimension(900, 400),
                          new Dimension((int) (900 * density), nativeSize.height));
                    assertSame(changed.getFirst().image(), wider.getFirst().image(), "Moving the strip must only move GPU quads");
                    assertEquals((int) (100 * density), wider.getFirst().x() - changed.getFirst().x());
                    assertTrue(overview.isHit(new Point(875, 75), new Dimension(900, 400)));
                    verify(gui).centerOnUnit(units.getFirst());

                    prefs.setShowUnitOverview(false);
                    assertTrue(fixture.view.captureOverlayLayers(logical, nativeSize).isEmpty());
                    fixture.source.refresh();
                    assertEquals(0, fixture.source.takeFrame().hud().sidePanelInset());
                    prefs.setShowUnitOverview(true);
                    assertEquals(2, fixture.view.captureOverlayLayers(logical, nativeSize).size());
                } finally {
                    prefs.removePreferenceChangeListener(overview);
                    prefs.setShowUnitOverview(wasVisible);
                }
            });
        }
    }

    @Test
    void overviewScrollsCachedCardsAndSelectsTheDisplayedUnit() throws Exception {
        GUIPreferences prefs = GUIPreferences.getInstance();
        boolean wasVisible = prefs.getShowUnitOverview();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            List<Entity> units = addUnits(fixture, 8);
            SwingUtilities.invokeAndWait(() -> {
                prefs.setShowUnitOverview(true);
                ClientGUI gui = gui(fixture);
                UnitOverviewOverlay overview = new UnitOverviewOverlay(gui);
                try {
                    fixture.view.addOverlay(overview);
                    Dimension size = new Dimension(800, 300);
                    var firstPage = fixture.view.captureOverlayLayers(size, size);
                    assertEquals(7, firstPage.size(), "Two buttons above three cards and two buttons below");
                    assertTrue(overview.isHit(new Point(775, 207), size));
                    var scrolled = fixture.view.captureOverlayLayers(size, size);
                    assertSame(firstPage.get(3).image(), scrolled.get(2).image(), "Scrolling repositions the existing second card");
                    assertSame(firstPage.get(4).image(), scrolled.get(3).image());
                    assertTrue(overview.isHit(new Point(775, 65), size));
                    verify(gui).centerOnUnit(units.getFirst());

                    fixture.game.setPhase(GamePhase.DEPLOYMENT);
                    units.getFirst().setDeployed(false);
                    units.getFirst().setDeployRound(3);
                    var deployment = fixture.view.captureOverlayLayers(size, size);
                    assertNotSame(scrolled.get(2).image(), deployment.get(2).image(), "Deployment countdown changes the card");
                    assertTrue(overview.isHit(new Point(775, 227), size));
                    var nextPage = fixture.view.captureOverlayLayers(size, size);
                    assertTrue(overview.isHit(new Point(775, 65), size));
                    verify(gui).centerOnUnit(units.get(3));
                    assertNotSame(deployment.get(2).image(), nextPage.get(2).image());
                } finally {
                    prefs.removePreferenceChangeListener(overview);
                    prefs.setShowUnitOverview(wasVisible);
                }
            });
        }
    }

    @Test
    void labelsReuseArtworkButRefreshVisibleStatusPreferencesAndSelection() throws Exception {
        GUIPreferences prefs = GUIPreferences.getInstance();
        Color validColor = prefs.getUnitValidColor();
        LabelDisplayStyle style = prefs.getUnitLabelStyle();
        int pipMode = prefs.getTMMPipMode();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    prefs.setTMMPipMode(1);
                    prefs.setUnitLabelStyle(LabelDisplayStyle.ABBREV);
                    EntitySprite.Annotations first = fixture.view.captureUnitAnnotations(fixture.entity, -1, null);
                    fixture.entity.setPosition(new Coords(7, 7));
                    fixture.entity.setFacing(3);
                    fixture.entity.setSecondaryFacing(3);
                    var same = fixture.view.captureUnitAnnotations(fixture.entity, -1, first);
                    assertSame(first, same, "Position and facing cannot force a fresh label raster");
                    assertSame(first.image(), same.image());

                    fixture.entity.setProne(true);
                    var prone = fixture.view.captureUnitAnnotations(fixture.entity, -1, same);
                    assertChanged(same, prone);
                    fixture.entity.setArmor(0, Mek.LOC_CENTER_TORSO);
                    var damaged = fixture.view.captureUnitAnnotations(fixture.entity, -1, prone);
                    assertChanged(prone, damaged);
                    prefs.setUnitValidColor(Color.MAGENTA);
                    var recolored = fixture.view.captureUnitAnnotations(fixture.entity, -1, damaged);
                    assertChanged(damaged, recolored);
                    fixture.view.redrawAllEntities();
                    fixture.view.highlightSelectedEntity(fixture.entity);
                    var selected = fixture.view.captureUnitAnnotations(fixture.entity, -1, recolored);
                    assertChanged(recolored, selected);
                    prefs.setUnitLabelStyle(LabelDisplayStyle.FULL);
                    var renamed = fixture.view.captureUnitAnnotations(fixture.entity, -1, selected);
                    assertChanged(selected, renamed);
                    fixture.game.setPhase(GamePhase.FIRING);
                    fixture.entity.delta_distance = 5;
                    fixture.entity.moved = EntityMovementType.MOVE_WALK;
                    var pips = fixture.view.captureUnitAnnotations(fixture.entity, -1, renamed);
                    assertChanged(renamed, pips);
                    assertSame(pips, fixture.view.captureUnitAnnotations(fixture.entity, -1, pips));

                    fixture.source.refresh();
                    var published = fixture.source.takeFrame().scene().units().getFirst().annotations();
                    fixture.source.refresh();
                    assertSame(published, fixture.source.takeFrame().scene().units().getFirst().annotations(),
                          "The immutable render snapshot must reuse its pixel storage too");
                } finally {
                    prefs.setUnitValidColor(validColor);
                    prefs.setUnitLabelStyle(style);
                    prefs.setTMMPipMode(pipMode);
                }
            });
        }
    }

    @Test
    void cachedLabelsDropPrivateStatusWhenAVisibleEnemyBecomesASensorContact() throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                Player enemy = new Player(1, "Opponent");
                enemy.setTeam(2);
                fixture.game.addPlayer(enemy.getId(), enemy);
                fixture.entity.setOwner(enemy);
                fixture.entity.setProne(true);
                fixture.source.refresh();
                var visible = fixture.source.takeFrame().scene().units().getFirst().annotations();
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS).setValue(true);
                fixture.entity.addBeenDetectedBy(fixture.player);
                fixture.source.refresh();
                var sensor = fixture.source.takeFrame().scene().units().getFirst();
                assertTrue(sensor.sensorContact());
                assertNotEquals(visible, sensor.annotations());
                fixture.entity.setProne(false);
                fixture.entity.setArmor(0, Mek.LOC_CENTER_TORSO);
                fixture.source.refresh();
                assertSame(sensor.annotations(), fixture.source.takeFrame().scene().units().getFirst().annotations(),
                      "Hidden damage and status must neither appear nor invalidate a sensor-only label");
                fixture.entity.addBeenSeenBy(fixture.player);
                fixture.source.refresh();
                assertNotEquals(sensor.annotations(), fixture.source.takeFrame().scene().units().getFirst().annotations());
                fixture.entity.clearSeenBy();
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_HIDDEN_UNITS).setValue(true);
                fixture.entity.setHidden(true);
                fixture.source.refresh();
                assertTrue(fixture.source.takeFrame().scene().units().isEmpty());
            });
        }
    }

    private static void assertChanged(EntitySprite.Annotations before, EntitySprite.Annotations after) {
        assertNotSame(before.image(), after.image());
        assertNotEquals(new BoardScene.Pixels(before.image()), new BoardScene.Pixels(after.image()));
    }

    static ClientGUI gui(GpuBoardFixture fixture) {
        Client client = mock(Client.class);
        when(client.getGame()).thenReturn(fixture.game);
        when(client.getLocalPlayer()).thenReturn(fixture.player);
        when(client.isMyTurn()).thenReturn(true);
        ClientGUI gui = mock(ClientGUI.class);
        when(gui.getClient()).thenReturn(client);
        when(gui.getMainPanel()).thenReturn(new JPanel());
        when(gui.getCurrentBoardView()).thenReturn(Optional.of(fixture.view));
        when(gui.getBoardView()).thenReturn(fixture.view);
        return gui;
    }

    static List<Entity> addUnits(GpuBoardFixture fixture, int count) throws Exception {
        FutureTask<List<Entity>> task = new FutureTask<>(() -> {
            List<Entity> units = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                Entity unit = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
                unit.setId(index + 2);
                unit.setOwner(fixture.player);
                unit.setPosition(new Coords(7 + index % 4, 5 + index / 4));
                unit.setDeployed(true);
                fixture.game.addEntity(unit, false);
                units.add(unit);
            }
            return units;
        });
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
