/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Optional;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.boardview.overlay.OffBoardTargetOverlay;
import megamek.client.ui.panels.phaseDisplay.TargetingPhaseDisplay;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.units.Mek;
import megamek.common.weapons.artillery.LongTom;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GpuOffBoardOverlayTest {
    @ParameterizedTest
    @ValueSource(doubles = { 1, 1.5, 2 })
    void sharesSmallNativeTextureAndRemovesHiddenButtons(double density) throws Exception {
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            var target = GpuUnitHudTest.addUnits(fixture, 1).getFirst();
            SwingUtilities.invokeAndWait(() -> {
                var graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
                try {
                    Player enemy = new Player(2, "Off-board target");
                    enemy.setTeam(2);
                    fixture.game.addPlayer(enemy.getId(), enemy);
                    target.setOwner(enemy);
                    target.setOffBoard(20, OffBoardDirection.WEST);
                    target.addOffBoardObserver(fixture.player.getTeam());
                    fixture.entity.setOffBoard(20, OffBoardDirection.SOUTH);
                    var artillery = fixture.entity.addEquipment(new LongTom(), Mek.LOC_LEFT_ARM);
                    var gui = GpuUnitHudTest.gui(fixture);
                    var widgetView = spy(fixture.view);
                    doReturn(artillery).when(widgetView).getSelectedArtilleryWeapon();
                    doReturn(new Rectangle(800, 600)).when(widgetView).getDisplayablesRect();
                    when(gui.getBoardView()).thenReturn(widgetView);
                    when(gui.getCurrentBoardView()).thenReturn(Optional.of(widgetView));
                    var targeting = mock(TargetingPhaseDisplay.class);
                    when(targeting.currentEntity()).thenReturn(fixture.entity);
                    var overlay = new OffBoardTargetOverlay(gui);
                    overlay.setTargetingPhaseDisplay(targeting);
                    Rectangle bounds = new Rectangle(800, 600);
                    graphics.scale(density, density);
                    assertTrue(overlay.captureLayers(graphics, bounds).isEmpty());
                    fixture.game.setPhase(GamePhase.TARGETING);
                    var first = overlay.captureLayers(graphics, bounds);
                    assertEquals(1, first.size());
                    assertEquals((int) (60 * density), first.getFirst().image().getWidth());
                    assertEquals((int) (40 * density), first.getFirst().image().getHeight());
                    var moved = overlay.captureLayers(graphics, new Rectangle(1000, 700));
                    assertSame(first.getFirst().image(), moved.getFirst().image());
                    assertEquals((int) (50 * density), moved.getFirst().y() - first.getFirst().y(),
                          "Resizing moves the native icon while preserving logical layout");
                    fixture.game.setPhase(GamePhase.MOVEMENT);
                    assertTrue(overlay.captureLayers(graphics, bounds).isEmpty());
                    assertFalse(overlay.isHit(new Point(10, 280), new Dimension(800, 600)));
                } catch (Exception error) {
                    throw new IllegalStateException(error);
                } finally {
                    graphics.dispose();
                }
            });
        }
    }
}
