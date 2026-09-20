/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import megamek.client.ui.util.KeyCommandBind;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Exercises the native input multiplexer so Scene2D cannot silently swallow configured board commands. */
@Tag("on-demand")
class GpuKeyboardSmokeTest {
    @Test
    void nativeInputForwardsConfiguredCommandsAndKeepsCameraAndTextFocusLocal() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            GpuBoardSource source = mock(GpuBoardSource.class);
            source.uiPreferences = fixture.source.uiPreferences;
            when(source.takeFrame()).thenAnswer(invocation -> fixture.source.takeFrame());
            new Lwjgl3Application(new GpuBattleView(source) {
                private int tick;

                @Override
                public void render() {
                    try {
                        super.render();
                        if (++tick != 3) {
                            return;
                        }
                        Input realInput = Gdx.input;
                        InputProcessor processor = realInput.getInputProcessor();
                        Input keyboard = mock(Input.class);
                        when(keyboard.getInputProcessor()).thenReturn(processor);
                        Gdx.input = keyboard;
                        try {
                            EnumSet<KeyCommandBind> camera = EnumSet.of(KeyCommandBind.SCROLL_NORTH,
                                  KeyCommandBind.SCROLL_SOUTH, KeyCommandBind.SCROLL_EAST, KeyCommandBind.SCROLL_WEST,
                                  KeyCommandBind.CAMERA_ROTATE_LEFT, KeyCommandBind.CAMERA_ROTATE_RIGHT,
                                  KeyCommandBind.CAMERA_TILT_UP, KeyCommandBind.CAMERA_TILT_DOWN,
                                  KeyCommandBind.CAMERA_RESET, KeyCommandBind.CAMERA_FIT_BOARD,
                                  KeyCommandBind.TOGGLE_ISO, KeyCommandBind.ZOOM_IN, KeyCommandBind.ZOOM_OUT,
                                  KeyCommandBind.ZOOM_OVERVIEW_TOGGLE);
                            for (KeyCommandBind bind : KeyCommandBind.values()) {
                                if (camera.contains(bind)) {
                                    continue;
                                }
                                int nativeKey = nativeKey(bind.key);
                                // The unit test covers both Num Lock states without changing the desktop's lock state.
                                if (nativeKey == Input.Keys.UNKNOWN) {
                                    continue;
                                }
                                setModifiers(keyboard, bind.modifiers);
                                clearInvocations(source);
                                processor.keyDown(nativeKey);
                                processor.keyUp(nativeKey);
                                verify(source).key(bind.key, true, bind.modifiers);
                                verify(source).key(bind.key, false, bind.modifiers);
                            }

                            setModifiers(keyboard, InputEvent.SHIFT_DOWN_MASK);
                            clearInvocations(source);
                            processor.keyDown(Input.Keys.W);
                            setModifiers(keyboard, 0);
                            processor.keyUp(Input.Keys.W);
                            verify(source).key(KeyEvent.VK_W, false, InputEvent.SHIFT_DOWN_MASK);

                            int oldKey = KeyCommandBind.KEY_BINDS.key;
                            int oldModifiers = KeyCommandBind.KEY_BINDS.modifiers;
                            try {
                                KeyCommandBind.KEY_BINDS.key = KeyEvent.VK_F9;
                                KeyCommandBind.KEY_BINDS.modifiers = 0;
                                clearInvocations(source);
                                processor.keyDown(Input.Keys.F9);
                                processor.keyUp(Input.Keys.F9);
                                verify(source).key(KeyEvent.VK_F9, true, 0);
                                verify(source).key(KeyEvent.VK_F9, false, 0);
                            } finally {
                                KeyCommandBind.KEY_BINDS.key = oldKey;
                                KeyCommandBind.KEY_BINDS.modifiers = oldModifiers;
                            }

                            when(source.chatActive()).thenReturn(true);
                            clearInvocations(source);
                            processor.keyDown(Input.Keys.Q);
                            processor.keyTyped('q');
                            processor.keyUp(Input.Keys.Q);
                            verify(source).key(KeyEvent.VK_Q, true, 0);
                            verify(source).keyTyped('q');
                            assertFalse(boardCamera.isRotating(), "Typing in chat must not rotate the camera");
                            when(source.chatActive()).thenReturn(false);

                            setModifiers(keyboard, InputEvent.SHIFT_DOWN_MASK);
                            clearInvocations(source);
                            processor.keyDown(Input.Keys.W);
                            setModifiers(keyboard, 0);
                            processor.keyDown(Input.Keys.F10);
                            Table menu = GpuBoardTestUi.stage().getRoot().findActor("tactical-menu");
                            processor.keyUp(Input.Keys.W);
                            verify(source).key(KeyEvent.VK_W, false, InputEvent.SHIFT_DOWN_MASK);
                            processor.keyDown(Input.Keys.ESCAPE);
                            assertFalse(menu.isVisible());
                            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        } finally {
                            Gdx.input = realInput;
                        }
                        Gdx.app.exit();
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
        }
        if (failure.get() != null) {
            throw new AssertionError("Native keyboard routing failed", failure.get());
        }
    }

    private static int nativeKey(int awt) {
        for (int key = 1; key <= Input.Keys.MAX_KEYCODE; key++) {
            if (GpuBattleView.awtKey(key) == awt) {
                return key;
            }
        }
        return Input.Keys.UNKNOWN;
    }

    private static void setModifiers(Input keyboard, int modifiers) {
        when(keyboard.isKeyPressed(Input.Keys.CONTROL_LEFT)).thenReturn((modifiers & InputEvent.CTRL_DOWN_MASK) != 0);
        when(keyboard.isKeyPressed(Input.Keys.SHIFT_LEFT)).thenReturn((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0);
        when(keyboard.isKeyPressed(Input.Keys.ALT_LEFT)).thenReturn((modifiers & InputEvent.ALT_DOWN_MASK) != 0);
        when(keyboard.isKeyPressed(Input.Keys.SYM)).thenReturn((modifiers & InputEvent.META_DOWN_MASK) != 0);
    }
}
