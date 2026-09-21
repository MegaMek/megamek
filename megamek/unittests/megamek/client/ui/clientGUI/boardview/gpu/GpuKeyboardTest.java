/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.badlogic.gdx.Input;
import megamek.client.Client;
import megamek.client.ui.clientGUI.ChatterBox;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.CommonMenuBar;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.overlay.ChatterBoxOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.KeyBindingsOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.PlanetaryConditionsOverlay;
import megamek.client.ui.dialogs.BotCommands.BotCommandsPanel;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.common.KeyBindParser;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GpuKeyboardTest {
    private final GUIPreferences preferences = GUIPreferences.getInstance();
    private GpuBoardFixture fixture;
    private GpuBoardSource source;
    private BoardView view;
    private ClientGUI gui;
    private Client client;
    private CommonMenuBar menu;
    private TestController controller;
    private KeyBindingsOverlay keys;
    private PlanetaryConditionsOverlay conditions;
    private ChatterBoxOverlay chat;
    private boolean originalKeys;
    private boolean originalConditions;

    private static class TestController extends MegaMekController {
        void close() {
            stopAllRepeating();
            keyRepeatTimer.cancel();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        fixture = GpuBoardFixture.create();
        SwingUtilities.invokeAndWait(() -> {
            try {
                fixture.source.close();
                originalKeys = preferences.getShowKeybindsOverlay();
                originalConditions = preferences.getShowPlanetaryConditionsOverlay();
                preferences.setValue(GUIPreferences.SHOW_KEYBINDS_OVERLAY, true);
                preferences.setValue(GUIPreferences.SHOW_PLANETARY_CONDITIONS_OVERLAY, true);
                menu = CommonMenuBar.getMenuBarForGame();
                menu.setPhase(GamePhase.MOVEMENT);
                client = mock(Client.class);
                when(client.getGame()).thenReturn(fixture.game);
                when(client.getLocalPlayer()).thenReturn(fixture.player);
                when(client.isMyTurn()).thenReturn(true);
                gui = mock(ClientGUI.class);
                when(gui.getClient()).thenReturn(client);
                when(gui.getMenuBar()).thenReturn(menu);
                when(gui.getMainPanel()).thenReturn(new JPanel());
                controller = new TestController();
                controller.clientGUI = gui;
                gui.controller = controller;
                for (KeyCommandBind bind : KeyCommandBind.values()) {
                    controller.registerKeyCommandBind(bind);
                }
                view = new BoardView(fixture.game, controller, gui, 0);
                view.setLocalPlayer(fixture.player);
                keys = new KeyBindingsOverlay(view);
                conditions = new PlanetaryConditionsOverlay(view);
                view.addOverlay(keys);
                view.addOverlay(conditions);
                source = new GpuBoardSource(view, () -> fixture.panel);
                source.setViewport(1400, 900, 1400, 900);
                source.refresh();
            } catch (Exception error) {
                throw new IllegalStateException(error);
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (source != null) {
                source.close();
            }
            if (chat != null) {
                chat.dispose();
                preferences.removePreferenceChangeListener(chat);
            }
            if (view != null) {
                view.dispose();
            }
            if (menu != null) {
                menu.die();
            }
            if (keys != null) {
                preferences.removePreferenceChangeListener(keys);
                KeyBindParser.removePreferenceChangeListener(keys);
            }
            if (conditions != null) {
                preferences.removePreferenceChangeListener(conditions);
                KeyBindParser.removePreferenceChangeListener(conditions);
            }
            preferences.setValue(GUIPreferences.SHOW_KEYBINDS_OVERLAY, originalKeys);
            preferences.setValue(GUIPreferences.SHOW_PLANETARY_CONDITIONS_OVERLAY, originalConditions);
            if (controller != null) {
                controller.close();
            }
        });
        fixture.close();
    }

    @Test
    void bothOverlaysToggleAndFinishFadingWhileTheClassicWindowIsHidden() throws Exception {
        assertFalse(view.getPanel().isShowing());
        press(KeyCommandBind.KEY_BINDS);
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(keys.isVisible());
            assertTrue(conditions.isVisible());
        });
        press(KeyCommandBind.PLANETARY_CONDITIONS);
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(keys.isVisible());
            assertFalse(conditions.isVisible());
        });
        CompletableFuture<Void> faded = new CompletableFuture<>();
        Timer check = new Timer(20, event -> {
            if (!keys.isSliding() && !conditions.isSliding()) {
                faded.complete(null);
            }
        });
        SwingUtilities.invokeAndWait(check::start);
        try {
            faded.get(3, TimeUnit.SECONDS);
        } finally {
            SwingUtilities.invokeAndWait(check::stop);
        }
        SwingUtilities.invokeAndWait(source::refresh);
        GpuBoardSource.Hud hud = source.takeFrame().hud();
        for (GpuBoardSource.HudLayer layer : hud.layers()) {
            assertEquals(0, layer.fade().opacity(System.nanoTime()), "Hidden overlays must disappear from the GPU HUD");
        }
        press(KeyCommandBind.KEY_BINDS);
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(keys.isVisible());
            assertFalse(conditions.isVisible());
        });
        press(KeyCommandBind.PLANETARY_CONDITIONS);
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(keys.isVisible());
            assertTrue(conditions.isVisible());
        });
    }

    @Test
    void everyEnabledMenuAcceleratorReachesItsExistingItemExactlyOnce() throws Exception {
        List<JMenuItem> items = new ArrayList<>();
        AtomicInteger clicks = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> collectAccelerators(menu, items));
        assertTrue(items.size() > 20, "Audit the whole game menu, not just the two overlay toggles");
        for (JMenuItem item : items) {
            if (ClientGUI.VIEW_UNIT_OVERVIEW.equals(item.getActionCommand())) {
                continue;
            }
            SwingUtilities.invokeAndWait(() -> {
                for (var listener : item.getActionListeners()) {
                    item.removeActionListener(listener);
                }
                item.addActionListener(event -> {
                    assertTrue(SwingUtilities.isEventDispatchThread());
                    clicks.incrementAndGet();
                });
            });
            int before = clicks.get();
            KeyStroke accelerator = item.getAccelerator();
            press(accelerator.getKeyCode(), accelerator.getModifiers());
            assertEquals(before + 1, clicks.get(), item.getText());
        }
    }

    @Test
    void nativeUnitOverviewHasNoToggleAndPreservesTheClassicPreference() throws Exception {
        boolean original = preferences.getShowUnitOverview();
        AtomicInteger clicks = new AtomicInteger();
        try {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setShowUnitOverview(false);
                findItem(ClientGUI.VIEW_UNIT_OVERVIEW).addActionListener(event -> clicks.incrementAndGet());
                source.refresh();
            });
            press(KeyCommandBind.UNIT_OVERVIEW);
            assertEquals(0, clicks.get());
            assertFalse(preferences.getShowUnitOverview());
            assertFalse(source.takeFrame().globalCommands().stream().flatMap(command -> command.children().stream())
                  .anyMatch(command -> command.id().contains(ClientGUI.VIEW_UNIT_OVERVIEW + ":")));
        } finally {
            SwingUtilities.invokeAndWait(() -> preferences.setShowUnitOverview(original));
        }
    }

    @Test
    void remappedMenuShortcutsCheckLiveAvailabilityAndDialogSuppression() throws Exception {
        JMenuItem keysItem = findItem(ClientGUI.VIEW_KEYBINDS_OVERLAY);
        SwingUtilities.invokeAndWait(() -> keysItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F8,
              InputEvent.ALT_DOWN_MASK)));
        press(KeyCommandBind.KEY_BINDS);
        assertTrue(preferences.getShowKeybindsOverlay(), "The old accelerator no longer applies");
        press(KeyEvent.VK_F8, InputEvent.ALT_DOWN_MASK);
        assertFalse(preferences.getShowKeybindsOverlay());
        SwingUtilities.invokeAndWait(() -> keysItem.setEnabled(false));
        press(KeyEvent.VK_F8, InputEvent.ALT_DOWN_MASK);
        assertFalse(preferences.getShowKeybindsOverlay());
        SwingUtilities.invokeAndWait(() -> {
            keysItem.setEnabled(true);
            when(gui.shouldIgnoreHotKeys()).thenReturn(true);
        });
        press(KeyEvent.VK_F8, InputEvent.ALT_DOWN_MASK);
        assertFalse(preferences.getShowKeybindsOverlay());
        SwingUtilities.invokeAndWait(() -> when(gui.shouldIgnoreHotKeys()).thenReturn(false));
        press(KeyEvent.VK_F8, InputEvent.ALT_DOWN_MASK);
        assertTrue(preferences.getShowKeybindsOverlay());
        SwingUtilities.invokeAndWait(source::close);
        press(KeyEvent.VK_F8, InputEvent.ALT_DOWN_MASK);
        assertTrue(preferences.getShowKeybindsOverlay());
    }

    @Test
    void phaseCommandsAndTheBotPanelUseTheRegisteredControllerActions() throws Exception {
        for (KeyCommandBind bind : List.of(KeyCommandBind.NEXT_UNIT, KeyCommandBind.PREV_UNIT, KeyCommandBind.CANCEL,
              KeyCommandBind.DONE, KeyCommandBind.DONE_NO_ACTION, KeyCommandBind.MOVE_STEP_FORWARD,
              KeyCommandBind.PREV_MODE, KeyCommandBind.NEXT_MODE, KeyCommandBind.BOT_COMMANDS)) {
            AtomicInteger performed = new AtomicInteger();
            SwingUtilities.invokeAndWait(() -> {
                controller.registerCommandAction(bind.cmd, performed::incrementAndGet);
                if (bind == KeyCommandBind.BOT_COMMANDS) {
                    controller.registerCommandAction(bind.cmd, performed::incrementAndGet);
                }
            });
            press(bind);
            assertEquals(1, performed.get(), bind.cmd);
        }
    }

    @Test
    void botPauseShortcutsUseTheSameAvailabilityAsTheActualPanel() throws Exception {
        SwingUtilities.invokeAndWait(() -> new BotCommandsPanel(client, null, controller, gui));
        press(KeyCommandBind.PAUSE);
        verify(client, never()).sendPause();
        SwingUtilities.invokeAndWait(() -> {
            fixture.player.setBot(true);
            fixture.game.setPhase(GamePhase.FIRING);
        });
        press(KeyCommandBind.PAUSE);
        verify(client).sendPause();
        press(KeyCommandBind.UNPAUSE);
        verify(client).sendUnpause();
    }

    @Test
    void commandBarActionsAreAvailableAndRecheckTheGameMasterBeforeExecuting() throws Exception {
        BoardScene.Command commands = source.takeFrame().globalCommands().stream()
              .filter(command -> command.id().equals("game-commands")).findFirst().orElseThrow();
        assertTrue(commands.children().stream().anyMatch(command -> command.id().contains("Report a Bug")));
        BoardScene.Command checkBv = commands.children().stream()
              .filter(command -> command.label().equals(megamek.client.ui.Messages.getString("GameCommands.CheckBvPlayers.title")))
              .findFirst().orElseThrow();
        checkBv.action().run();
        SwingUtilities.invokeAndWait(() -> verify(client).sendChat("/checkbv"));
        BoardScene.Command skip = commands.children().stream()
              .filter(command -> command.label().equals(megamek.client.ui.Messages.getString("GameCommands.SkipTurn.title")))
              .findFirst().orElseThrow();
        SwingUtilities.invokeAndWait(() -> {
            Player gameMaster = new Player(77, "Referee");
            gameMaster.setGameMaster(true);
            fixture.game.addPlayer(gameMaster.getId(), gameMaster);
        });
        skip.action().run();
        SwingUtilities.invokeAndWait(() -> {
            verify(client, never()).sendChat("/skip");
            source.refresh();
        });
        assertFalse(source.takeFrame().globalCommands().stream()
              .filter(command -> command.id().equals("game-commands")).flatMap(command -> command.children().stream())
              .anyMatch(command -> command.id().equals(skip.id())), "A stale privileged command disappears immediately");
    }

    @Test
    void chatUsesExistingEditingAndSendingWithoutTypingItsOpeningShortcutTwice() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ChatterBox history = mock(ChatterBox.class);
            history.history = new LinkedList<>();
            chat = new ChatterBoxOverlay(gui, view, controller, history);
            view.addOverlay(chat);
            view.getPanel().addKeyListener(chat);
        });
        source.key(KeyEvent.VK_SLASH, true, 0);
        source.keyTyped('/');
        source.key(KeyEvent.VK_SLASH, false, 0);
        SwingUtilities.invokeAndWait(() -> assertEquals("/", chat.getMessage()));
        source.key(KeyEvent.VK_W, true, 0);
        source.keyTyped('w');
        source.key(KeyEvent.VK_W, false, 0);
        source.key(KeyEvent.VK_E, true, 0);
        source.keyTyped('\u00e9');
        source.key(KeyEvent.VK_E, false, 0);
        SwingUtilities.invokeAndWait(() -> assertEquals("/w\u00e9", chat.getMessage()));
        press(KeyEvent.VK_BACK_SPACE, 0);
        SwingUtilities.invokeAndWait(() -> assertEquals("/w", chat.getMessage()));
        press(KeyEvent.VK_ENTER, 0);
        SwingUtilities.invokeAndWait(() -> verify(client).sendChat("/w"));
        press(KeyCommandBind.CANCEL);
        assertFalse(source.chatActive());
    }

    @Test
    void everyDefaultBindingHasADesktopKeyMappingIncludingKeypadNavigation() {
        Set<Integer> mapped = new HashSet<>();
        for (int key = 0; key <= Input.Keys.MAX_KEYCODE; key++) {
            mapped.add(GpuBattleView.awtKey(key, true));
            mapped.add(GpuBattleView.awtKey(key, false));
        }
        for (KeyCommandBind bind : KeyCommandBind.values()) {
            assertTrue(mapped.contains(bind.keyDefault), "No desktop key can invoke " + bind.cmd);
        }
        assertEquals(KeyEvent.VK_KP_UP, GpuBattleView.awtKey(Input.Keys.NUMPAD_8, false));
        assertEquals(KeyEvent.VK_NUMPAD8, GpuBattleView.awtKey(Input.Keys.NUMPAD_8, true));
        assertEquals(KeyEvent.VK_QUOTE, GpuBattleView.awtKey(Input.Keys.APOSTROPHE, true));
        assertEquals(KeyEvent.VK_F24, GpuBattleView.awtKey(Input.Keys.F24, true));
    }

    private void press(KeyCommandBind bind) throws Exception {
        press(bind.key, bind.modifiers);
    }

    private void press(int key, int modifiers) throws Exception {
        source.key(key, true, modifiers);
        source.key(key, false, modifiers);
        SwingUtilities.invokeAndWait(() -> { });
    }

    private JMenuItem findItem(String command) {
        List<JMenuItem> items = new ArrayList<>();
        collectAccelerators(menu, items);
        return items.stream().filter(item -> command.equals(item.getActionCommand())).findFirst().orElseThrow();
    }

    private static void collectAccelerators(Container parent, List<JMenuItem> items) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JMenuItem item && item.isEnabled() && item.isVisible()) {
                if (item instanceof JMenu group) {
                    collectAccelerators(group.getPopupMenu(), items);
                } else if (item.getAccelerator() != null) {
                    items.add(item);
                }
            }
        }
    }
}
