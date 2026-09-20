/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.CommonMenuBar;
import megamek.client.ui.clientGUI.MegaMekGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayDialog;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.panels.phaseDisplay.FiringDisplay;
import megamek.client.ui.util.MegaMekController;
import megamek.common.Player;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.game.GameTurn;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Native input drives the real Swing firing display and its weapon model; no server is required. */
@Tag("on-demand")
class GpuAttackSmokeTest {
    @Test
    void nativeAttackPanelSelectsTargetsQueuesAndClearsRealWeaponAttacks() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<GpuBoardSource> source = new AtomicReference<>();
        AtomicReference<FiringDisplay> firing = new AtomicReference<>();
        AtomicReference<UnitDisplayPanel> display = new AtomicReference<>();
        AtomicReference<Client> network = new AtomicReference<>();
        AtomicReference<MockedStatic<MegaMekGUI>> keyDispatcher = new AtomicReference<>();
        File output = new File(System.getProperty("megamek.gpu.screenshots"));
        assertTrue(output.isDirectory() || output.mkdirs());
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        fixture.source.close();
                        fixture.game.setPhase(GamePhase.FIRING);
                        Client client = mock(Client.class);
                        network.set(client);
                        when(client.getGame()).thenReturn(fixture.game);
                        when(client.getLocalPlayer()).thenReturn(fixture.player);
                        when(client.isMyTurn()).thenReturn(true);
                        when(client.getMyTurn()).thenReturn(new GameTurn(fixture.player.getId()));
                        ClientGUI gui = mock(ClientGUI.class);
                        gui.controller = mock(MegaMekController.class);
                        keyDispatcher.set(mockStatic(MegaMekGUI.class, CALLS_REAL_METHODS));
                        keyDispatcher.get().when(MegaMekGUI::getKeyDispatcher).thenReturn(gui.controller);
                        when(gui.getClient()).thenReturn(client);
                        CommonMenuBar menu = mock(CommonMenuBar.class);
                        when(menu.getComponents()).thenReturn(new Component[0]);
                        when(gui.getMenuBar()).thenReturn(menu);
                        when(gui.getDisplayedUnit()).thenReturn(fixture.entity);
                        when(gui.getUnitDisplayDialog()).thenReturn(mock(UnitDisplayDialog.class));
                        BoardView view = spy(fixture.view);
                        doReturn(gui).when(view).getClientgui();
                        display.set(new UnitDisplayPanel(gui, null));
                        when(gui.getUnitDisplay()).thenReturn(display.get());
                        firing.set(new FiringDisplay(gui));
                        view.addBoardViewListener(firing.get());
                        when(gui.getCurrentPanel()).thenReturn(firing.get());
                        Player enemy = new Player(2, "Enemy");
                        enemy.setTeam(2);
                        fixture.game.addPlayer(enemy.getId(), enemy);
                        Entity target = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf"))
                              .getEntity();
                        target.setId(42);
                        target.setOwner(enemy);
                        target.setPosition(new Coords(5, 3));
                        target.setDeployed(true);
                        fixture.game.addEntity(target, false);
                        target.addBeenSeenBy(fixture.player);
                        firing.get().selectEntity(fixture.entity.getId());
                        source.set(new GpuBoardSource(view, firing::get));
                    } catch (Exception error) {
                        throw new IllegalStateException(error);
                    }
                });
                new Lwjgl3Application(new GpuBattleView(source.get()) {
                    private int tick;
                    private int initialWeapon;

                    @Override
                    public void render() {
                        super.render();
                        try {
                            tick++;
                            if (tick == 1) {
                                boardCamera.setIsometric(true);
                            } else if (tick == 8) {
                                assertBounds();
                                assertTrue(button("fireFire").isDisabled(), "A target must be chosen before firing");
                                boardUi().inspect(new Coords(5, 3), 180, 180);
                                SwingUtilities.invokeAndWait(source.get()::refresh);
                            } else if (tick == 16) {
                                assertTrue(popup().isVisible());
                                GpuBoardTestUi.click("board.useHex");
                                assertFalse(popup().isVisible(), "Choosing a target dismisses the popup immediately");
                                assertFalse(boardUi().plotting(), "Target selection must not enter a movement/twist tool");
                                SwingUtilities.invokeAndWait(source.get()::refresh);
                            } else if (tick == 18) {
                                assertTrue(source.get().takeFrame().attack().targetName().contains("Atlas"));
                                initialWeapon = source.get().takeFrame().attack().selectedWeapon();
                                GpuBoardTestUi.click("camera");
                                assertTrue(popup().isVisible());
                                GpuBoardTestUi.click("attack:fireSkip");
                                assertFalse(popup().isVisible(), "Next weapon dismisses another open popup");
                                SwingUtilities.invokeAndWait(source.get()::refresh);
                            } else if (tick == 20) {
                                assertNotEquals(initialWeapon, source.get().takeFrame().attack().selectedWeapon(),
                                      "The dismissing click must also select the next weapon");
                                GpuBoardTestUi.click("camera");
                                selectWeapon(2);
                                assertFalse(popup().isVisible(), "Weapon-list clicks dismiss another open popup");
                                SwingUtilities.invokeAndWait(source.get()::refresh);
                            } else if (tick == 24) {
                                assertEquals(2, source.get().takeFrame().attack().selectedWeapon());
                                assertTrue(button("Ammunition").isVisible());
                                GpuBoardTestUi.capture(new File(output, "attack-panel-ammunition.png"));
                                assertSelectedWeaponVisible();
                                selectWeapon(initialWeapon);
                                SwingUtilities.invokeAndWait(source.get()::refresh);
                                Gdx.graphics.setWindowedMode(1280, 1040);
                            } else if (tick == 28) {
                                ScrollPane weapons = GpuBoardTestUi.stage().getRoot().findActor("attack-weapons");
                                ScrollPane content = GpuBoardTestUi.stage().getRoot().findActor("attack-scroll");
                                assertTrue(weapons.getHeight() > 156, "Weapons must use the available tall-panel space");
                                assertTrue(content.getMaxY() < 1, "The weapon list should fit the available content well");
                                Vector2 bottom = weapons.localToAscendantCoordinates(content, new Vector2());
                                assertTrue(bottom.y < 8, "No empty reserved area below the weapon list: " + bottom.y);
                                assertTrue(label("attack-target").getText().toString().contains("Atlas"));
                                assertTrue(label("attack-solution").getText().toString().contains("To Hit"));
                                assertFalse(button("fireFire").isDisabled());
                                GpuBoardTestUi.capture(new File(output, "attack-panel-isometric.png"));
                                GpuBoardTestUi.click("camera");
                                assertTrue(popup().isVisible());
                                GpuBoardTestUi.click("attack:fireFire");
                                assertFalse(popup().isVisible(), "Fire weapon dismisses another open popup");
                                SwingUtilities.invokeAndWait(source.get()::refresh);
                            } else if (tick == 40) {
                                assertFalse(source.get().takeFrame().attack().orders().isEmpty());
                                assertFalse(label("attack-orders").getText().toString().equals("No attacks queued."));
                                GpuBoardTestUi.capture(new File(output, "attack-panel-queued.png"));
                                GpuBoardTestUi.click("clear");
                                SwingUtilities.invokeAndWait(source.get()::refresh);
                            } else if (tick == 52) {
                                assertTrue(source.get().takeFrame().attack().orders().isEmpty());
                                Gdx.graphics.setWindowedMode(900, 600);
                                boardCamera.setIsometric(false);
                            } else if (tick == 70) {
                                assertBounds();
                                assertSelectedWeaponVisible();
                                GpuBoardTestUi.capture(new File(output, "attack-panel-small-top.png"));
                                // Bottom controls remain clickable while the information pane scrolls.
                                ScrollPane pane = GpuBoardTestUi.stage().getRoot().findActor("attack-scroll");
                                pane.setScrollPercentY(1);
                                pane.updateVisualScroll();
                            } else if (tick == 78) {
                                assertBounds();
                                GpuBoardTestUi.capture(new File(output, "attack-panel-small-orders.png"));
                                GpuBoardTestUi.click("attack:fireFire");
                                SwingUtilities.invokeAndWait(source.get()::refresh);
                            } else if (tick == 84) {
                                SwingUtilities.invokeAndWait(() -> assertTrue(fixture.game.getActionsVector().stream()
                                      .anyMatch(WeaponAttackAction.class::isInstance)));
                                GpuBoardTestUi.clickText("Done Firing");
                                SwingUtilities.invokeAndWait(() -> {
                                    verify(network.get()).sendAttackData(eq(fixture.entity.getId()), any());
                                    source.get().refresh();
                                });
                                Gdx.app.exit();
                            }
                        } catch (Throwable error) {
                            failure.set(error);
                            Gdx.app.exit();
                        }
                    }

                    private TextButton button(String id) {
                        return GpuBoardTestUi.stage().getRoot().findActor("attack:" + id);
                    }

                    private GpuBoardUi boardUi() throws ReflectiveOperationException {
                        var field = GpuBattleView.class.getDeclaredField("ui");
                        field.setAccessible(true);
                        return (GpuBoardUi) field.get(this);
                    }

                    private Table popup() { return GpuBoardTestUi.stage().getRoot().findActor("tactical-menu"); }

                    private void selectWeapon(int index) {
                        ScrollPane pane = GpuBoardTestUi.stage().getRoot().findActor("attack-weapons");
                        Actor row = button("weapon:" + index);
                        pane.scrollTo(0, row.getY(), row.getWidth(), row.getHeight());
                        pane.updateVisualScroll();
                        GpuBoardTestUi.click("attack:weapon:" + index);
                    }

                    private Label label(String id) {
                        return GpuBoardTestUi.stage().getRoot().findActor(id);
                    }

                    private void assertSelectedWeaponVisible() {
                        ScrollPane pane = GpuBoardTestUi.stage().getRoot().findActor("attack-weapons");
                        Actor row = button("weapon:" + source.get().takeFrame().attack().selectedWeapon());
                        Vector2 bottom = row.localToAscendantCoordinates(pane, new Vector2());
                        assertTrue(bottom.y >= -1 && bottom.y + row.getHeight() <= pane.getHeight() + 1,
                              "The selected weapon must stay visible: row y=" + bottom.y + ", height=" + row.getHeight()
                                    + ", viewport=" + pane.getHeight() + ", scroll=" + pane.getScrollY());
                    }

                    private void assertBounds() {
                        Table panel = GpuBoardTestUi.stage().getRoot().findActor("attack-panel");
                        assertTrue(panel.isVisible());
                        assertTrue(panel.getTop() <= GpuBoardTestUi.stage().getHeight() - GpuBoardUi.TOP_HEIGHT);
                        assertTrue(panel.getY() >= GpuBoardUi.TURN_HEIGHT);
                        assertTrue(panel.getRight() <= GpuBoardTestUi.stage().getWidth());
                        Actor fire = button("fireFire");
                        assertTrue(fire.getWidth() > 100 && fire.getHeight() >= 40);
                        assertEquals(44, fire.getHeight());
                    }
                }, GpuBoardWindow.configuration(false));
            } finally {
                SwingUtilities.invokeAndWait(() -> {
                    if (source.get() != null) {
                        source.get().close();
                    }
                    if (firing.get() != null) {
                        firing.get().removeAllListeners();
                    }
                    if (keyDispatcher.get() != null) {
                        keyDispatcher.get().close();
                    }
                });
            }
        }
        if (failure.get() != null) {
            throw new AssertionError("Native attack panel failed", failure.get());
        }
    }
}
