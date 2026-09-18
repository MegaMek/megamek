/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * Live geometry and atmosphere controls. Geometry is shared with picking; atmosphere belongs to this GPU window.
 */
final class GpuBoardTuning {
    private static final float SLIDER_WIDTH = 220;
    private static final float LABEL_WIDTH = 150;

    private record Knob(String name, float min, float max, float step, String format) { }
    private record Control(Knob knob, Slider slider, Label reading, TextButton toggle) { }

    /** One row of the panel: the board value it drives, its range and how its reading is written. */
    private static final List<Knob> KNOBS = List.of(
          new Knob("Hex scale", 0.5f, 3f, 0.05f, "%.2f"),
          new Knob("Unit scale", 0.25f, 3f, 0.05f, "%.2f"),
          new Knob("Unit height scale", 0.1f, 2f, 0.05f, "%.2f"),
          new Knob("Base level height", 4, 40, 1, "%.0f"),
          // A value of one hides the grid.
          new Knob("Hex frame shade", 0f, 1f, 0.05f, "%.2f"));

    private static final List<Knob> ATMOSPHERE_KNOBS = List.of(
          new Knob("Time of day", 0, 24, 0.25f, "clock"),
          new Knob("Cloud cover", 0, 1, 0.05f, "%.2f"),
          new Knob("Ground fog", 0, 1, 0.01f, "%.2f"),
          new Knob("Fog height", 0.5f, 8, 0.25f, "%.2f"),
          new Knob("Haze", 0, 1, 0.05f, "%.2f"),
          new Knob("Exposure (EV)", -2, 2, 0.1f, "%+.1f"),
          new Knob("Light shafts", 0, 1, 0.05f, "%.2f"));

    private static final List<Knob> VISIBILITY_KNOBS = List.of(
          new Knob("Building opacity", 0, 100, 5, "%.0f%%"),
          new Knob("Tree opacity", 0, 100, 5, "%.0f%%"),
          new Knob("See-through", 0, 100, 5, "%.0f%%"));

    private static final List<Knob> EFFECT_KNOBS = List.of(
          new Knob("Rain", 0, 1, 0.05f, "%.2f"),
          new Knob("Snow", 0, 1, 0.05f, "%.2f"),
          new Knob("Hail", 0, 1, 0.05f, "%.2f"),
          new Knob("Blowing sand", 0, 1, 0.05f, "%.2f"),
          new Knob("Lightning", 0, 1, 0.05f, "%.2f"),
          new Knob("Wind strength", 0, 1, 0.05f, "%.2f"),
          new Knob("Wind direction", 0, 360, 15, "%.0f"));

    private final Table panel = new Table();
    private final Table rows = new Table();
    private final ScrollPane scroll;
    private final List<Control> geometry;
    private final List<Control> visibility;
    private final List<Control> weather;
    private final List<Control> effects;
    private BoardAtmosphere.Settings atmosphere = BoardAtmosphere.DEFAULTS;
    private BoardAtmosphere.Settings scenarioDefaults;
    private boolean syncing;

    GpuBoardTuning(Skin skin) {
        panel.setBackground(skin.getDrawable("panel"));
        panel.setTouchable(Touchable.enabled);
        panel.setName("board-tuning");
        panel.pad(12).top();
        panel.defaults().pad(2);
        panel.add(new Label("Board tuning", skin, "heading")).left().row();
        rows.top().defaults().pad(3);
        rows.add(new Label("Geometry", skin, "heading")).colspan(3).left().row();
        geometry = controls(skin, KNOBS, this::applyGeometry, 0);
        rows.add(new Label("Unit visibility", skin, "heading")).colspan(3).left().padTop(12).row();
        visibility = controls(skin, VISIBILITY_KNOBS, this::applyVisibility, 0);
        rows.add(new Label("See-through: occluded unit outline + fill (0% off)", skin)).colspan(3).left().row();
        rows.add(new Label("Daylight & atmosphere", skin, "heading")).colspan(3).left().padTop(12).row();
        rows.add(new Label("Visual preview - game conditions stay unchanged", skin))
              .colspan(3).left().padBottom(6).row();
        rows.add(new Label("Defaults restores the scenario's starting atmosphere", skin))
              .colspan(3).left().padBottom(6).row();
        Table presets = new Table();
        for (BoardAtmosphere.Weather preset : BoardAtmosphere.Weather.values()) {
            TextButton button = new TextButton(preset.label, skin);
            button.setName("atmosphere-" + preset.name());
            button.setProgrammaticChangeEvents(false);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    button.setChecked(false);
                    setAtmosphere(preset.apply(atmosphere));
                }
            });
            presets.add(button).width(100).height(28).padRight(4);
        }
        rows.add(presets).colspan(3).left().padBottom(5).row();
        weather = controls(skin, ATMOSPHERE_KNOBS, this::applyAtmosphere, 0);
        rows.add(new Label("Weather effects", skin, "heading")).colspan(3).left().padTop(12).row();
        effects = controls(skin, EFFECT_KNOBS, this::applyAtmosphere, 5);
        scroll = new ScrollPane(rows, skin);
        scroll.setName("tuning-scroll");
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setFlickScroll(false);
        // Keep wheel gestures in this panel and allow sliders to retain their complete drag gesture.
        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                panel.getStage().setScrollFocus(scroll);
            }
        });
        panel.add(scroll).grow().row();
        TextButton reset = new TextButton("Defaults", skin);
        reset.setName("tuning-defaults");
        reset.setProgrammaticChangeEvents(false);
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
        panel.add(buttons).left().padTop(8).row();
        restoreDefaults();
    }

    private List<Control> controls(Skin skin, List<Knob> knobs, Runnable apply, int toggleCount) {
        List<Control> result = new ArrayList<>();
        for (Knob knob : knobs) {
            Slider slider = new Slider(knob.min(), knob.max(), knob.step(), false, skin, "default");
            slider.setName(knob.name());
            Label reading = new Label("", skin);
            slider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (!syncing) {
                        apply.run();
                    }
                }
            });
            TextButton toggle = null;
            if (result.size() < toggleCount) {
                toggle = new TextButton(knob.name(), skin);
                toggle.setName("weather-toggle-" + knob.name());
                toggle.setProgrammaticChangeEvents(false);
                toggle.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        slider.setValue(slider.getValue() > 0 ? 0 : 0.5f);
                    }
                });
            }
            result.add(new Control(knob, slider, reading, toggle));
            rows.add(toggle == null ? new Label(knob.name(), skin) : toggle).left().width(LABEL_WIDTH);
            rows.add(slider).minWidth(60).prefWidth(SLIDER_WIDTH).growX().height(20);
            rows.add(reading).width(52).left().row();
        }
        return result;
    }

    Table panel() {
        return panel;
    }

    void toggle() {
        panel.setVisible(!panel.isVisible());
        if (panel.getStage() != null) {
            panel.getStage().setScrollFocus(panel.isVisible() ? scroll : null);
        }
    }

    boolean visible() {
        return panel.isVisible();
    }

    void resize(float stageWidth, float stageHeight, float topHeight, float bottomHeight) {
        panel.setSize(Math.min(rows.getPrefWidth() + 34, Math.max(1, stageWidth - 24)),
              Math.min(panel.getPrefHeight(), Math.max(1, stageHeight - topHeight - bottomHeight - 24)));
        panel.setPosition(Math.max(8, stageWidth - panel.getWidth() - 12),
              Math.max(bottomHeight + 8, stageHeight - topHeight - panel.getHeight() - 12));
    }

    /** Writes the current board values into the sliders, as the initial state and after a reset. */
    private void restoreDefaults() {
        BoardGeometry.Tuning defaults = BoardGeometry.DEFAULTS;
        float[] values = { defaults.hexScale(), defaults.unitScale(), defaults.unitHeightScale(),
              defaults.levelHeight(), defaults.gridShade() };
        setValues(geometry, values);
        applyGeometry();
        setValues(visibility, new float[] { GpuTerrain.DEFAULT_BUILDING_OPACITY * 100,
              GpuTerrain.DEFAULT_TREE_OPACITY * 100, GpuUnitVisibility.DEFAULT_INTENSITY * 100 });
        updateReadings(visibility);
        setAtmosphere(scenarioDefaults == null ? BoardAtmosphere.DEFAULTS : scenarioDefaults);
    }

    private void setValues(List<Control> controls, float[] values) {
        syncing = true;
        for (int index = 0; index < controls.size(); index++) {
            controls.get(index).slider().setValue(values[index]);
        }
        syncing = false;
    }

    BoardAtmosphere.Settings atmosphere() {
        return atmosphere;
    }

    float buildingOpacity() {
        return value(visibility, 0) / 100;
    }

    float treeOpacity() {
        return value(visibility, 1) / 100;
    }

    float seeThrough() {
        return value(visibility, 2) / 100;
    }

    /** Capture once per opened board; routine frame publication must not overwrite a user's preview. */
    void useScenario(BoardAtmosphere.Settings initial, boolean newBoard) {
        if (scenarioDefaults == null || newBoard) {
            scenarioDefaults = initial;
            setAtmosphere(initial);
        }
    }

    private void setAtmosphere(BoardAtmosphere.Settings settings) {
        setValues(weather, new float[] { settings.hour(), settings.clouds(), settings.fog(), settings.fogHeight(),
              settings.haze(), settings.exposure(), settings.shafts() });
        BoardAtmosphere.Effects next = settings.effects();
        setValues(effects, new float[] { next.rain(), next.snow(), next.hail(), next.sand(), next.lightning(),
              next.wind(), next.windDirection() });
        applyAtmosphere();
    }

    private void applyGeometry() {
        BoardGeometry.tune(new BoardGeometry.Tuning(value(geometry, 0), value(geometry, 1), value(geometry, 2),
              Math.round(value(geometry, 3)), value(geometry, 4)));
        updateReadings(geometry);
    }

    private void applyVisibility() {
        updateReadings(visibility);
    }

    private void applyAtmosphere() {
        atmosphere = new BoardAtmosphere.Settings(value(weather, 0), value(weather, 1), value(weather, 2),
              value(weather, 3), value(weather, 4), value(weather, 5), value(weather, 6),
              new BoardAtmosphere.Effects(value(effects, 0), value(effects, 1), value(effects, 2), value(effects, 3),
                    value(effects, 4), value(effects, 5), value(effects, 6)));
        updateReadings(weather);
        updateReadings(effects);
    }

    private static void updateReadings(List<Control> controls) {
        for (Control control : controls) {
            float value = control.slider().getValue();
            if (control.toggle() != null) {
                control.toggle().setChecked(value > 0);
                control.toggle().setText(control.knob().name() + (value > 0 ? " On" : " Off"));
            }
            int minutes = Math.round(value * 60);
            control.reading().setText(control.knob().format().equals("clock")
                  ? String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)
                  : String.format(Locale.ROOT, control.knob().format(), value));
        }
    }

    private static float value(List<Control> controls, int index) {
        return controls.get(index).slider().getValue();
    }
}
