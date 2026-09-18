/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/** Shared native UI input and artwork assertions; all calls run on the GL thread. */
final class GpuBoardTestUi {
    private GpuBoardTestUi() { }

    static Stage stage() {
        return (Stage) ((InputMultiplexer) Gdx.input.getInputProcessor()).getProcessors().first();
    }

    static void click(String name) {
        clickActor(stage().getRoot().findActor(name));
    }

    static void clickText(String label) {
        clickActor(buttonWithText(stage().getRoot(), label));
    }

    private static Actor buttonWithText(Group group, String label) {
        for (Actor actor : group.getChildren()) {
            if (actor.isVisible() && actor instanceof TextButton button && (button.getText().toString().equals(label)
                  || button.getText().toString().endsWith(" / " + label))) {
                return button;
            }
            if (actor.isVisible() && actor instanceof Group nested) {
                Actor match = buttonWithText(nested, label);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void clickActor(Actor actor) {
        assertTrue(actor != null, "Missing UI action");
        // Controls can move below the fold as the tuning panel grows. Scroll them into view before real input.
        for (Actor parent = actor.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ScrollPane scroll) {
                Vector2 position = actor.localToAscendantCoordinates(scroll.getWidget(), new Vector2());
                scroll.scrollTo(position.x, position.y, actor.getWidth(), actor.getHeight(), false, true);
                scroll.updateVisualScroll();
                stage().draw();
            }
        }
        Vector2 point = actor.localToStageCoordinates(new Vector2(actor.getWidth() / 2, actor.getHeight() / 2));
        stage().stageToScreenCoordinates(point);
        Gdx.input.getInputProcessor().touchDown((int) point.x, (int) point.y, 0, Input.Buttons.LEFT);
        Gdx.input.getInputProcessor().touchUp((int) point.x, (int) point.y, 0, Input.Buttons.LEFT);
    }

    static long capture(File file) {
        Pixmap image = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            PixmapIO.writePNG(new FileHandle(file), image, -1, true);
            long hash = 1;
            int changes = 0;
            int first = image.getPixel(10, 10);
            for (int y = 80; y < image.getHeight() - 70; y += 8) {
                for (int x = 20; x < image.getWidth() - 290; x += 8) {
                    int pixel = image.getPixel(x, y);
                    hash = hash * 31 + pixel;
                    if (pixel != first) {
                        changes++;
                    }
                }
            }
            assertTrue(changes > 1000, "The board must contain rendered artwork");
            return hash;
        } finally {
            image.dispose();
        }
    }
}
