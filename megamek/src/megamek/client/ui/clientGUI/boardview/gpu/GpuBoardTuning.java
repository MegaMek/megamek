/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

/**
 * Live board controls. Geometry is shared with picking; presentation settings belong to this GPU window.
 */
final class GpuBoardTuning {
    private static final float SLIDER_WIDTH = 120;
    private static final float LABEL_WIDTH = 120;

    private record Knob(String name, float min, float max, float step, String format) { }
    private record Control(Knob knob, Slider slider, Label reading, TextButton toggle) { }

    /** One row of the panel: the board value it drives, its range and how its reading is written. */
    private static final List<Knob> KNOBS = List.of(
          new Knob("Hex scale", 0.5f, 3f, 0.05f, "%.2f"),
          new Knob("Unit scale", 0.25f, 3f, 0.05f, "%.2f"),
          new Knob("Unit height scale", 0.1f, 2f, 0.05f, "%.2f"),
          new Knob("Base level height", 4, 40, 1, "%.0f"),
          // A value of one hides the grid.
          new Knob("Hex frame shade", 0f, 1f, 0.05f, "%.2f"),
          new Knob("Multi-hex unit scale", 0.25f, 1.5f, 0.05f, "%.2f"));

    private static final List<Knob> ATMOSPHERE_KNOBS = List.of(
          new Knob("Time of day", 0, 24, 0.25f, "clock"),
          new Knob("Cloud cover", 0, 1, 0.05f, "%.2f"),
          new Knob("Ground fog", 0, 1, 0.01f, "%.2f"),
          new Knob("Fog height", BoardAtmosphere.MIN_FOG_HEIGHT, 8, 0.25f, "%.2f"),
          new Knob("Haze", 0, 1, 0.05f, "%.2f"),
          new Knob("Exposure (EV)", -2, 2, 0.1f, "%+.1f"));

    private static final List<Knob> VISIBILITY_KNOBS = List.of(
          new Knob("Building opacity", 0, 100, 5, "%.0f%%"),
          new Knob("See-through", 0, 100, 5, "%.0f%%"));

    private static final List<Knob> FOV_KNOBS = List.of(
          new Knob("FoV darkness", 0, 100, 5, "%.0f%%"));

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
    private final CheckBox normalMaps;
    private final List<Control> geometry;
    private final List<Control> visibility;
    private final ButtonGroup<TextButton> fovModes = new ButtonGroup<>();
    private final List<Control> fieldOfView;
    private final List<Control> weather;
    private final List<Control> effects;
    private final CheckBox overrideDamage;
    private final List<Control> damage;
    private BoardAtmosphere.Settings atmosphere = BoardAtmosphere.DEFAULTS;
    private BoardAtmosphere.Settings scenarioDefaults;
    private boolean syncing;

    GpuBoardTuning(Skin skin) {
        panel.setBackground(skin.getDrawable("menu-panel"));
        panel.setTouchable(Touchable.enabled);
        panel.setName("board-tuning");
        panel.pad(8).top();
        panel.add(new Label("Board tuning", skin)).left().padBottom(6).row();
        rows.top().defaults().pad(0, 3, 0, 3);
        section(skin, "Geometry");
        geometry = controls(skin, KNOBS, this::applyGeometry, 0);
        normalMaps = checkbox(skin, "Normal maps", "tuning-normal-maps");
        section(skin, "Unit visibility");
        visibility = controls(skin, VISIBILITY_KNOBS, this::applyVisibility, 0);
        visibility.get(1).slider().addListener(new TextTooltip(
              "Highlights occluded units with an outline and fill. Set to 0% to turn off.", skin, "menu"));
        section(skin, "Field of view");
        Table modes = new Table();
        for (GpuFieldOfView.Style style : GpuFieldOfView.Style.values()) {
            TextButton button = new TextButton(style.label, skin, "menu-control");
            button.setName("fov-style-" + style.name());
            button.setUserObject(style);
            fovModes.add(button);
            modes.add(button).width(92).height(22).padRight(2);
        }
        rows.add(modes).colspan(3).left().padBottom(2).row();
        fieldOfView = controls(skin, FOV_KNOBS, this::applyFieldOfView, 0);
        section(skin, "Daylight & atmosphere");
        Table presets = new Table();
        for (BoardAtmosphere.Weather preset : BoardAtmosphere.Weather.values()) {
            TextButton button = new TextButton(preset.label, skin, "menu-control");
            button.setName("atmosphere-" + preset.name());
            button.setProgrammaticChangeEvents(false);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    button.setChecked(false);
                    setAtmosphere(preset.apply(atmosphere));
                }
            });
            presets.add(button).width(68).height(22).padRight(2);
        }
        rows.add(presets).colspan(3).left().padBottom(2).row();
        weather = controls(skin, ATMOSPHERE_KNOBS, this::applyAtmosphere, 0);
        rows.add(new Label(String.format(Locale.ROOT, "Fog + haze opacity cap: %.0f%%",
              BoardAtmosphere.MAX_FOG_OPACITY * 100), skin, "small")).colspan(3).left().height(18).row();
        section(skin, "Weather effects");
        effects = controls(skin, EFFECT_KNOBS, this::applyAtmosphere, 5);
        section(skin, "Unit damage");
        overrideDamage = checkbox(skin, "Override visible unit damage", "tuning-override-damage");
        damage = controls(skin, List.of(new Knob("Display damage", 0, 1, 0.01f, "%.2f")), this::applyDamage, 0);
        damage.getFirst().slider().addListener(new TextTooltip(
              "Non-Meks: linear damage. Meks: 0-0.50 removes armor; 0.50-1 damages structure. "
                    + "Destroyed parts take priority. Visual preview only.", skin, "menu"));
        overrideDamage.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyDamage();
            }
        });
        scroll = new ScrollPane(rows, skin, "menu");
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
        panel.add(scroll).minHeight(0).grow().row();
        panel.add(new Image(skin.getDrawable("rule"))).height(1).growX().padTop(6).row();
        TextButton reset = new TextButton("Defaults", skin, "menu-control");
        reset.setName("tuning-defaults");
        reset.addListener(new TextTooltip("Restore geometry, visibility, the scenario's starting atmosphere, and disable damage preview.",
              skin, "menu"));
        reset.setProgrammaticChangeEvents(false);
        reset.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                reset.setChecked(false);
                restoreDefaults();
            }
        });
        Table buttons = new Table();
        buttons.add(reset).width(76).height(22);
        buttons.add(new Label("Visual preview only", skin, "small")).padLeft(10).expandX().left();
        buttons.add(new Label("F9 to close", skin, "small")).right();
        panel.add(buttons).growX().padTop(4).row();
        restoreDefaults();
    }

    private void section(Skin skin, String title) {
        float spacing = rows.hasChildren() ? 8 : 0;
        rows.add(new Label(title.toUpperCase(Locale.ROOT), skin, "kicker"))
              .colspan(3).left().padTop(spacing).padBottom(3).row();
    }

    private CheckBox checkbox(Skin skin, String label, String name) {
        CheckBox checkbox = new CheckBox(label, skin, "menu");
        checkbox.setName(name);
        checkbox.getImage().setScaling(Scaling.fit);
        checkbox.getImageCell().size(14).padRight(5);
        rows.add(checkbox).colspan(3).left().height(20).row();
        return checkbox;
    }

    private List<Control> controls(Skin skin, List<Knob> knobs, Runnable apply, int toggleCount) {
        List<Control> result = new ArrayList<>();
        for (Knob knob : knobs) {
            Slider slider = new Slider(knob.min(), knob.max(), knob.step(), false, skin, "menu");
            slider.setName(knob.name());
            Label reading = new Label("", skin, "small");
            reading.setAlignment(Align.right);
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
                toggle = new TextButton(knob.name(), skin, "menu-control");
                toggle.setName("weather-toggle-" + knob.name());
                toggle.getLabel().setAlignment(Align.left);
                toggle.setProgrammaticChangeEvents(false);
                toggle.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        slider.setValue(slider.getValue() > 0 ? 0 : 0.5f);
                    }
                });
            }
            result.add(new Control(knob, slider, reading, toggle));
            rows.add(toggle == null ? new Label(knob.name(), skin, "menu") : toggle).left().width(LABEL_WIDTH);
            rows.add(slider).minWidth(60).prefWidth(SLIDER_WIDTH).growX().height(20);
            rows.add(reading).width(38).right().row();
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
        panel.setSize(Math.min(panel.getPrefWidth(), Math.max(1, stageWidth - 24)),
              Math.min(panel.getPrefHeight(), Math.max(1, stageHeight - topHeight - bottomHeight - 24)));
        panel.setPosition(Math.max(8, stageWidth - panel.getWidth() - 12),
              Math.max(bottomHeight + 8, stageHeight - topHeight - panel.getHeight() - 12));
    }

    /** Writes the current board values into the sliders, as the initial state and after a reset. */
    private void restoreDefaults() {
        normalMaps.setChecked(true);
        BoardGeometry.Tuning defaults = BoardGeometry.DEFAULTS;
        float[] values = { defaults.hexScale(), defaults.unitScale(), defaults.unitHeightScale(),
              defaults.levelHeight(), defaults.gridShade(), defaults.multiHexUnitScale() };
        setValues(geometry, values);
        applyGeometry();
        setValues(visibility, new float[] { GpuTerrain.DEFAULT_BUILDING_OPACITY * 100,
              GpuUnitVisibility.DEFAULT_OUTLINE_INTENSITY * 100 });
        updateReadings(visibility);
        fovModes.getButtons().get(GpuFieldOfView.STYLE.ordinal()).setChecked(true);
        setValues(fieldOfView, new float[] { GpuFieldOfView.DARKNESS * 100 });
        applyFieldOfView();
        setAtmosphere(scenarioDefaults == null ? BoardAtmosphere.DEFAULTS : scenarioDefaults);
        overrideDamage.setChecked(false);
        setValues(damage, new float[] { 0 });
        applyDamage();
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

    boolean normalMaps() {
        return normalMaps.isChecked();
    }

    /** Negative means the preview is disabled; otherwise this is the displayed loss from zero to one. */
    float damageOverride() {
        return overrideDamage.isChecked() ? value(damage, 0) : -1;
    }

    float buildingOpacity() {
        return value(visibility, 0) / 100;
    }

    float seeThrough() {
        return value(visibility, 1) / 100;
    }

    GpuFieldOfView.Style fovStyle() {
        return (GpuFieldOfView.Style) fovModes.getChecked().getUserObject();
    }

    float fovDarkness() {
        return value(fieldOfView, 0) / 100;
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
              settings.haze(), settings.exposure() });
        BoardAtmosphere.Effects next = settings.effects();
        setValues(effects, new float[] { next.rain(), next.snow(), next.hail(), next.sand(), next.lightning(),
              next.wind(), next.windDirection() });
        applyAtmosphere();
    }

    private void applyGeometry() {
        BoardGeometry.tune(new BoardGeometry.Tuning(value(geometry, 0), value(geometry, 1), value(geometry, 2),
              Math.round(value(geometry, 3)), value(geometry, 4), value(geometry, 5)));
        updateReadings(geometry);
    }

    private void applyVisibility() {
        updateReadings(visibility);
    }

    private void applyFieldOfView() {
        updateReadings(fieldOfView);
    }

    private void applyDamage() {
        damage.getFirst().slider().setDisabled(!overrideDamage.isChecked());
        updateReadings(damage);
    }

    private void applyAtmosphere() {
        atmosphere = new BoardAtmosphere.Settings(value(weather, 0), value(weather, 1), value(weather, 2),
              value(weather, 3), value(weather, 4), value(weather, 5),
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
