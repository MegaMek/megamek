/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;

/** One right-hand dock: shared bounds and visibility, with an optional resizable report panel. */
final class GpuPanelDock {
    static final float WIDTH = 362;
    static final float MARGIN = 12;
    static final float CAMERA_GAP = 8;
    private final List<Table> panels;
    private final Runnable changed;
    private final Table resizable;
    private Table active;
    private Table fallback;
    private float preferredWidth = WIDTH;
    private float left, right, bottom, height;

    GpuPanelDock(Skin skin, Runnable changed, Table resizable, Table... panels) {
        this.panels = List.of(panels);
        this.changed = changed;
        this.resizable = resizable;
        for (Table panel : panels) {
            panel.setVisible(false);
        }
        if (resizable != null) {
            addResizeHandle(skin, resizable);
        }
    }

    boolean isShowing(Table panel) { return active == panel; }

    void fallback(Table panel) {
        boolean following = active == fallback;
        fallback = panel;
        if (following) { show(panel); }
    }

    void toggle(Table panel) { show(active == panel ? fallback : panel); }

    void show(Table panel) {
        if (active == panel) { return; }
        active = panel;
        for (Table candidate : panels) { candidate.setVisible(candidate == active); }
        var stage = panels.getFirst().getStage();
        if (stage != null) {
            stage.setKeyboardFocus(null);
            stage.setScrollFocus(null);
        }
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
        changed.run();
    }

    void restore() { show(fallback); }

    void resize(float width, float height, float top, float bottom, float leftInset, float rightInset) {
        left = Math.max(MARGIN, leftInset);
        right = Math.max(left + 1, width - Math.max(MARGIN, rightInset));
        this.bottom = bottom + MARGIN;
        this.height = Math.max(1, height - top - bottom - 2 * MARGIN);
        layout();
    }

    private void layout() {
        for (Table panel : panels) {
            float width = Math.min(panel == resizable ? preferredWidth : WIDTH, right - left);
            panel.setBounds(right - width, bottom, width, height);
            if (panel == resizable) {
                panel.findActor("dock-resize").setBounds(0, 0, 10, height);
            }
            panel.validate();
        }
        changed.run();
    }

    float cameraRight(float width, float rightInset) {
        return active == null ? width - rightInset : active.getX() - CAMERA_GAP;
    }

    private void addResizeHandle(Skin skin, Table panel) {
        Table handle = new Table();
        handle.setName("dock-resize");
        handle.setTouchable(Touchable.enabled);
        Image grip = new Image(skin.getDrawable("white"));
        grip.setColor(GpuBoardSkin.MUTED);
        handle.add(grip).size(2, 32);
        handle.addListener(new TextTooltip("Drag the left edge to resize the panel", skin));
        handle.addListener(new InputListener() {
            private float startX, startWidth;
            private boolean dragging;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button != Input.Buttons.LEFT) { return false; }
                startX = event.getStageX();
                startWidth = panel.getWidth();
                dragging = true;
                cursor(true);
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                float maximum = right - left;
                preferredWidth = MathUtils.clamp(startWidth + startX - event.getStageX(), Math.min(WIDTH, maximum), maximum);
                layout();
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                dragging = false;
                cursor(false);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) { cursor(true); }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1 && !dragging) { cursor(false); }
            }

            private void cursor(boolean resize) {
                grip.setColor(resize ? GpuBoardSkin.ACCENT : GpuBoardSkin.MUTED);
                Gdx.graphics.setSystemCursor(resize ? SystemCursor.HorizontalResize : SystemCursor.Arrow);
            }
        });
        panel.addActor(handle);
    }
}
