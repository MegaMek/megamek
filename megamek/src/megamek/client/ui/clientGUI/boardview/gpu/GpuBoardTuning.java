/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * The board geometry values as sliders, applied to {@link BoardGeometry} while the board is on screen, so a
 * padding or level height can be judged where it shows instead of through an edit and a rebuild.
 */
final class GpuBoardTuning {
    private static final float SLIDER_WIDTH = 260;
    private static final float LABEL_WIDTH = 150;

    private record Knob(String name, float min, float max, float step, String format) { }

    /** One row of the panel: the board value it drives, its range and how its reading is written. */
    private static final List<Knob> KNOBS = List.of(
          new Knob("Padding", 0f, 0.5f, 0.01f, "%.2f"),
          new Knob("Hex scale", 0.5f, 3f, 0.05f, "%.2f"),
          new Knob("Interpolation inset", 0f, 0.5f, 0.01f, "%.2f"),
          new Knob("Corner bevel", 0f, 0.5f, 0.01f, "%.2f"),
          new Knob("Band normal flatness", 0f, 1f, 0.05f, "%.2f"),
          new Knob("Unit scale", 0.25f, 3f, 0.05f, "%.2f"),
          new Knob("Unit height scale", 0.1f, 2f, 0.05f, "%.2f"),
          new Knob("Base level height", 4, 40, 1, "%.0f"),
          // 1 is full transparency: the frame repeats the surface and GpuTerrain leaves it out.
          new Knob("Hex frame shade", 0f, 1f, 0.05f, "%.2f"));

    private final Table panel = new Table();
    private final List<Slider> sliders = new ArrayList<>();
    private final List<Label> readings = new ArrayList<>();
    private final Runnable applied;
    private boolean syncing;

    GpuBoardTuning(Skin skin, Runnable applied) {
        this.applied = applied;
        panel.setBackground(skin.newDrawable("white", Color.valueOf("172431")));
        panel.pad(12).top();
        panel.defaults().pad(2);
        panel.add(new Label("Board tuning", skin)).colspan(2).left().row();
        for (Knob knob : KNOBS) {
            Slider slider = new Slider(knob.min(), knob.max(), knob.step(), false, skin, "default");
            Label reading = new Label("", skin);
            slider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    apply();
                }
            });
            sliders.add(slider);
            readings.add(reading);
            panel.add(new Label(knob.name(), skin)).left().width(LABEL_WIDTH);
            panel.add(slider).width(SLIDER_WIDTH).height(20);
            panel.add(reading).width(52).left().row();
        }
        TextButton reset = new TextButton("Defaults", skin);
        reset.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                reset.setChecked(false);
                restoreDefaults();
            }
        });
        Table buttons = new Table();
        buttons.add(reset).width(96).height(28);
        buttons.add(new Label("F9 toggles this panel", skin)).padLeft(10).row();
        panel.add(buttons).colspan(3).left().padTop(8).row();
        restoreDefaults();
    }

    Table panel() {
        return panel;
    }

    void toggle() {
        panel.setVisible(!panel.isVisible());
    }

    boolean visible() {
        return panel.isVisible();
    }

    void resize(float stageWidth, float stageHeight, float topHeight, float bottomHeight) {
        panel.pack();
        panel.setPosition(Math.max(8, stageWidth - panel.getWidth() - 12),
              Math.max(bottomHeight + 8, stageHeight - topHeight - panel.getHeight() - 12));
    }

    /** Writes the current board values into the sliders, as the initial state and after a reset. */
    private void restoreDefaults() {
        BoardGeometry.Tuning defaults = BoardGeometry.DEFAULTS;
        float[] values = { defaults.padding(), defaults.hexScale(), defaults.interpolationInset(),
              defaults.cornerBevel(), defaults.bandNormalFlatness(), defaults.unitScale(), defaults.unitHeightScale(),
              defaults.baseLevelHeight(), defaults.hexFrameShade() };
        syncing = true;
        for (int index = 0; index < sliders.size(); index++) {
            sliders.get(index).setValue(values[index]);
        }
        syncing = false;
        apply();
    }

    private void apply() {
        if (syncing) {
            return;
        }
        BoardGeometry.tune(new BoardGeometry.Tuning(value(0), value(1), value(2), value(3), value(4), value(5),
              value(6), Math.round(value(7)), value(8)));
        for (int index = 0; index < readings.size(); index++) {
            readings.get(index).setText(String.format(Locale.ROOT, KNOBS.get(index).format(), value(index)));
        }
        applied.run();
    }

    private float value(int index) {
        return sliders.get(index).getValue();
    }
}
