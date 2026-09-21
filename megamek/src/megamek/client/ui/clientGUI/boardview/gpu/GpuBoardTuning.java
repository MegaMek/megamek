/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;

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

    private static final List<Knob> LIGHTING_KNOBS = List.of(
          new Knob("Time of day", 0, 24, 0.25f, "clock"),
          new Knob("Exposure (EV)", -2, 2, 0.1f, "%+.1f"));

    private static final List<Knob> ATMOSPHERE_KNOBS = List.of(
          new Knob("Cloud cover", 0, 1, 0.01f, "%.2f"),
          new Knob("Ground fog", 0, 1, 0.01f, "%.2f"),
          new Knob("Ground layer height", BoardAtmosphere.MIN_GROUND_LAYER_HEIGHT, 8, 0.25f, "%.2f"),
          new Knob("Haze", 0, 1, 0.05f, "%.2f"));

    private static final List<Knob> VISIBILITY_KNOBS = List.of(
          new Knob("Building opacity", 0, 100, 5, "%.0f%%"),
          new Knob("See-through", 0, 100, 5, "%.0f%%"));

    private static final List<Knob> FOV_KNOBS = List.of(
          new Knob("FoV darkness", 0, 100, 5, "%.0f%%"));

    private static final List<Knob> SENSOR_KNOBS = List.of(
          new Knob("Sensor darkness", 0, 100, 5, "%.0f%%"));

    private static final List<Knob> EFFECT_KNOBS = List.of(
          new Knob("Rain", 0, 1, 0.05f, "%.2f"),
          new Knob("Snow", 0, 1, 0.05f, "%.2f"),
          new Knob("Hail", 0, 1, 0.05f, "%.2f"),
          new Knob("Blowing sand", 0, 1, 0.05f, "%.2f"),
          new Knob("Lightning", 0, 1, 0.05f, "%.2f"),
          new Knob("Wind strength", 0, 1, 0.05f, "%.2f"),
          new Knob("Wind direction", 0, 360, 15, "%.0f"));

    private final Table panel = new Table();
    /** Construction cursor only; each tab owns its own rows and scroll position. */
    private Table rows = new Table();
    private final CheckBox normalMaps;
    private final CheckBox vsync;
    private final List<Control> geometry;
    private final List<Control> familySizes;
    private final CheckBox overviewIcons;
    private final List<Control> overview;
    private final List<Control> visibility;
    private final ButtonGroup<TextButton> fovModes;
    private final List<Control> fieldOfView;
    private final ButtonGroup<TextButton> sensorModes;
    private final List<Control> sensors;
    private final List<Control> daylight;
    private final CheckBox moonlight;
    private final SelectBox<Atmosphere> pressure;
    private final SelectBox<AtmosphericTaint> atmosphericTaint;
    private final List<Control> temperature;
    private final List<Control> gravity;
    private final List<Control> weather;
    private final CheckBox fixedSun;
    private final List<Control> effects;
    private final List<Control> rendering;
    private final CheckBox overrideDamage;
    private final SelectBox<UnitDamageDisplay.Location> damageLocation;
    private final List<Control> damage;
    private BoardAtmosphere.Settings atmosphere = BoardAtmosphere.DEFAULTS;
    private BoardAtmosphere.Settings lastScenario;
    private boolean conditionsPreview;
    private boolean syncing;

    GpuBoardTuning(Skin skin) {
        this(skin, null);
    }

    GpuBoardTuning(Skin skin, GpuBoardSource source) {
        panel.setBackground(skin.getDrawable("menu-panel"));
        panel.setTouchable(Touchable.enabled);
        panel.setName("board-tuning");
        panel.pad(8).top();
        panel.add(new Label("Board tuning", skin)).left().padBottom(6).row();
        Table general = rows;
        rows.top().defaults().pad(0, 3, 0, 3);
        section(skin, "Geometry");
        geometry = controls(skin, KNOBS, this::applyGeometry, 0);
        normalMaps = checkbox(skin, "Normal maps", "tuning-normal-maps");
        vsync = checkbox(skin, "VSync", "tuning-vsync");
        vsync.addListener(new TextTooltip("Synchronize with the monitor's refresh rate. FPS stays capped at 60.",
              skin, "menu"));
        vsync.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.graphics.setVSync(vsync.isChecked());
                Gdx.graphics.setForegroundFPS(0);
                // Gdx.graphics.setForegroundFPS(vsync.isChecked() ? 0 : 60);
            }
        });
        section(skin, "Unit family sizes");
        familySizes = controls(skin, Arrays.stream(UnitFamilyScale.values())
              .map(family -> new Knob(family.label, 0.25f, 3, 0.05f, "%.2f")).toList(), this::applyFamilySizes, 0);
        for (int index = 0; index < familySizes.size(); index++) {
            var slider = familySizes.get(index).slider();
            slider.setName("tuning-size-" + UnitFamilyScale.values()[index].name());
            slider.addListener(new TextTooltip("Uniform size multiplier; 1.00 is neutral. Stacks with Unit scale. "
                  + "Mek weight classes also multiply All Meks; ultralight Meks use Light Meks.", skin, "menu"));
        }
        section(skin, "Overview icons");
        overviewIcons = checkbox(skin, "Distant top-view icons", "tuning-overview-icons");
        overviewIcons.addListener(new TextTooltip("Replace units and trees with flat board artwork when zoomed out "
              + "within 15 degrees of overhead. Also available in the Camera menu.", skin, "menu"));
        overview = controls(skin, List.of(new Knob("Icon switch hex px (0 = always)", 0, 256, 2, "%.0f")),
              this::applyOverview, 0);
        overview.getFirst().slider().addListener(new TextTooltip("Switch to icons when a hex is this many window pixels wide. "
              + "Zoom in 15% further to return to models, avoiding flicker at the boundary.", skin, "menu"));
        section(skin, "Unit visibility");
        visibility = controls(skin, VISIBILITY_KNOBS, this::applyVisibility, 0);
        visibility.get(1).slider().addListener(new TextTooltip(
              "Highlights occluded units with an outline and fill. Set to 0% to turn off.", skin, "menu"));
        section(skin, "Outside field of view");
        fovModes = effectModes(skin, "fov");
        fieldOfView = controls(skin, FOV_KNOBS, this::applyFieldOfView, 0);
        section(skin, "Outside sensor range");
        sensorModes = effectModes(skin, "sensor");
        sensors = controls(skin, SENSOR_KNOBS, this::applyFieldOfView, 0);
        Table atmospheric = new Table();
        rows = atmospheric;
        rows.top().defaults().pad(0, 3, 0, 3);
        TextButton conditions = new TextButton("Planetary conditions...", skin, "menu-control");
        conditions.setName("tuning-planetary-conditions");
        conditions.setDisabled(source == null);
        conditions.setProgrammaticChangeEvents(false);
        conditions.addListener(new TextTooltip("Edit the active visual planetary conditions. Apply always restores their values "
              + "and resets the extra atmosphere effects to their constants, even when conditions are unchanged.", skin, "menu"));
        conditions.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                conditions.setChecked(false);
                conditions.setDisabled(true);
                var application = Gdx.app;
                source.editPlanetaryConditions(settings -> application.postRunnable(() -> {
                    if (source.isClosed()) { return; }
                    conditions.setDisabled(false);
                    if (settings != null) { applyConditions(settings); }
                }));
            }
        });
        section(skin, "Atmosphere presets").add(conditions).height(22).padLeft(8);
        Table presets = new Table();
        for (AtmospherePreset preset : AtmospherePreset.values()) {
            TextButton button = new TextButton(preset.label, skin, "menu-control");
            button.setName("atmosphere-" + preset.name());
            button.setProgrammaticChangeEvents(false);
            button.addListener(new TextTooltip(preset.description(), skin, "menu"));
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    button.setChecked(false);
                    applyConditions(source == null ? preset.settings(0.5) : source.preview(preset));
                }
            });
            presets.add(button).width(94).height(22).padRight(2);
            if (presets.getChildren().size % 3 == 0) { presets.row(); }
        }
        rows.add(presets).colspan(3).left().padBottom(2).row();
        section(skin, "Lighting");
        daylight = controls(skin, LIGHTING_KNOBS, this::applyAtmosphere, 0);
        moonlight = checkbox(skin, "Moonlight at night", "tuning-moonlight");
        moonlight.addListener(new TextTooltip("Enable the directional moon at night. Moonless and Pitch Black disable it; "
              + "Exposure (EV) controls their darkness. This never disables daytime sunlight.", skin, "menu"));
        moonlight.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!syncing) { applyAtmosphere(); }
            }
        });
        fixedSun = checkbox(skin, "Fixed sun/moon", "tuning-fixed-sun");
        fixedSun.addListener(new TextTooltip("Keep the light at the same position on screen when rotating or tilting the board. "
              + "Time of day still sets its color and strength; Moonless and Pitch Black have no moonlight.", skin, "menu"));
        daylight.getFirst().slider().addListener(new TextTooltip(
              "Starts at a random quarter-hour within the scenario's daylight, dawn/dusk or night window. "
                    + "It stays fixed during combat; adjust here to override. Defaults restores the scenario's choice.", skin, "menu"));
        daylight.get(1).slider().addListener(new TextTooltip(
              "Visual brightness offset. The Moonless preset uses -0.6 EV; Pitch Black uses -1 EV, both without moonlight. "
                    + "Glare/Solar Flare use +0.6/+1.2 EV. Time of day also sets the base exposure.", skin, "menu"));
        section(skin, "Planet properties");
        gravity = controls(skin, List.of(new Knob("Gravity (g)", 0, 10, 0.01f, "%.2f")), this::applyAtmosphere, 0);
        gravity.getFirst().slider().addListener(new TextTooltip(
              "Visual gravity controls the height and timing of newly starting jump animations. "
                    + "Moves and gameplay rules stay unchanged; an airborne jump finishes its existing arc.", skin, "menu"));
        pressure = choice(skin, "Air pressure", "tuning-atmosphere-pressure", Atmosphere.values(), this::applyAtmosphere);
        pressure.addListener(new TextTooltip("Visual atmosphere pressure: controls sky scattering, clouds and permitted weather. "
              + "Vacuum clears atmospheric effects; changing pressure never changes the game's rules.", skin, "menu"));
        temperature = controls(skin, List.of(new Knob("Temperature (C)", -200, 200, 1, "%.0f")), this::applyAtmosphere, 0);
        temperature.getFirst().slider().addListener(new TextTooltip(
              "Scenario temperature controls rain wetness: freezing or colder keeps the ground dry. Visual preview only.", skin, "menu"));
        atmosphericTaint = choice(skin, "Atmospheric taint", "tuning-atmospheric-taint", AtmosphericTaint.values(), this::applyAtmosphere);
        atmosphericTaint.addListener(new TextTooltip("Select the atmospheric palette to preview. Taint strength scales its color. "
              + "This does not change gameplay exposure, fire or damage rules.", skin, "menu"));
        section(skin, "Clouds and ground air");
        weather = controls(skin, ATMOSPHERE_KNOBS, this::applyAtmosphere, 0);
        weather.getFirst().slider().addListener(new TextTooltip(
              "Moving cloud shadows and a soft sky gradient. Thin air gives lighter shade; trace atmosphere and vacuum disable clouds.",
              skin, "menu"));
        rows.add(new Label(String.format(Locale.ROOT, "Fog + haze opacity cap: %.0f%%",
              BoardAtmosphere.MAX_FOG_OPACITY * 100), skin, "small")).colspan(3).left().height(18).row();
        section(skin, "Weather effects");
        effects = controls(skin, EFFECT_KNOBS, this::applyAtmosphere, 5);
        effects.get(5).slider().addListener(new TextTooltip(
              "Clouds and fog keep drifting even when wind strength is zero.", skin, "menu"));
        section(skin, "Light and fog effects");
        rendering = controls(skin, List.of(new Knob("God rays", 0, 2, 0.05f, "%.2f"),
              new Knob("Cloud shadow min", 0, 1, 0.05f, "%.2f"),
              new Knob("Cloud shadow max", 0, 1, 0.05f, "%.2f"),
              new Knob("Sun glare", 0, 1, 0.05f, "%.2f"),
              new Knob("Fog height variation", 0, 4, 0.05f, "%.2f"),
              new Knob("Fog density variation", 0, 1, 0.05f, "%.2f"),
              new Knob("Moon shadow contrast", 0, 1, 0.05f, "%.2f"),
              new Knob("Taint strength", 0, 2, 0.05f, "%.2f"),
              new Knob("Fog calm drift", 0, 0.3f, 0.01f, "%.2f")), this::applyRendering, 0);
        rendering.get(0).slider().addListener(new TextTooltip(
              "Sunlight scattered through cloud openings. 0 disables shafts; haze and viewing angle affect visibility.", skin, "menu"));
        rendering.get(1).slider().addListener(new TextTooltip(
              "Cloud-patch opacity at sparse cover. Strength rises with cover toward the maximum; density stays at 0.25.",
              skin, "menu"));
        rendering.get(2).slider().addListener(new TextTooltip(
              "Cloud-patch opacity at full cover. Cannot be lower than the minimum; ambient light remains in shadow.",
              skin, "menu"));
        rendering.get(3).slider().addListener(new TextTooltip(
              "Golden glare and lens reflections when facing the sun, strongest near dawn/dusk. "
                    + "Clouds, fog and source occlusion reduce it. "
                    + "0 disables it; this lens effect also works without an atmosphere.", skin, "menu"));
        effects.get(0).slider().addListener(new TextTooltip(
              "Rain also wets exposed ground above freezing in sufficiently dense air.", skin, "menu"));
        effects.get(3).slider().addListener(new TextTooltip(String.format(Locale.ROOT,
              "Wind-driven dust above the board, with at most %.0f%% opacity even at full strength. "
                    + "Ground layer height controls its falloff; the solid map base stays clear.",
              GpuAtmosphere.MAX_SAND_OPACITY * 100), skin, "menu"));
        weather.get(2).slider().addListener(new TextTooltip(
              "Shared fog and sand falloff height above the board baseline, in terrain levels.", skin, "menu"));
        rendering.get(4).slider().addListener(new TextTooltip(
              "Maximum height deviation of drifting fog banks, in terrain levels. 0 gives a constant height.", skin, "menu"));
        rendering.get(5).slider().addListener(new TextTooltip(
              "Thins the gaps between wind-driven fog banks. 0 gives uniform density; 1 permits clear gaps. "
                    + "The fog opacity cap still applies.", skin, "menu"));
        rendering.get(6).slider().addListener(new TextTooltip(
              "Strengthens Full Moon shadows while preserving the brightness of lit level ground. "
                    + "0 restores the original softer shadows. Moonless and Pitch Black have no moonlight.", skin, "menu"));
        rendering.get(7).slider().addListener(new TextTooltip(
              "Scale the selected taint's sky and existing fog colors. 1 uses its tainted/toxic palette; 0 disables the tint. "
                    + "Keeps brightness and fog density unchanged. Weaker in thin air; disabled in vacuum or breathable air.",
              skin, "menu"));
        rendering.get(8).slider().addListener(new TextTooltip(
              "Intrinsic fog speed in hex widths per second when calm. A gently changing vector blends into the wind. "
                    + "0 disables calm drift; wind still moves the fog.", skin, "menu"));
        rows = general;
        section(skin, "Unit damage");
        overrideDamage = checkbox(skin, "Override visible unit damage", "tuning-override-damage");
        damageLocation = choice(skin, "Damage location", "tuning-damage-location", UnitDamageDisplay.Location.values(), this::applyDamage);
        damageLocation.addListener(new TextTooltip("Applies to matching model locations across the board. Models without locations "
              + "always use All locations. Other locations keep their actual damage.", skin, "menu"));
        damage = controls(skin, List.of(new Knob("Display damage", 0, 1, 0.01f, "%.2f")), this::applyDamage, 0);
        damage.getFirst().slider().addListener(new TextTooltip(
              "Non-Meks: linear damage. Meks: 0-0.50 removes armor; 0.50-1 damages structure. "
                    + "Infantry: proportion of troops fallen (rounded to visible figures). "
                    + "Destroyed parts take priority. Visual preview only.", skin, "menu"));
        overrideDamage.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyDamage();
            }
        });
        ScrollPane generalScroll = scroll(skin, general, "tuning-general-scroll");
        ScrollPane atmosphereScroll = scroll(skin, atmospheric, "tuning-scroll");
        Table tabs = new Table();
        ButtonGroup<TextButton> tabGroup = new ButtonGroup<>();
        tab(skin, tabs, tabGroup, "General", generalScroll, atmosphereScroll);
        tab(skin, tabs, tabGroup, "Atmosphere", atmosphereScroll, generalScroll);
        atmosphereScroll.setVisible(false);
        panel.add(tabs).growX().padBottom(6).row();
        panel.add(new Stack(generalScroll, atmosphereScroll)).minHeight(0).grow().row();
        panel.add(new Image(skin.getDrawable("rule"))).height(1).growX().padTop(6).row();
        TextButton reset = new TextButton("Defaults", skin, "menu-control");
        reset.setName("tuning-defaults");
        reset.addListener(new TextTooltip("Restore both tabs: geometry, family sizes, visibility, VSync, light/fog effects, "
              + "the game's current planetary conditions, and disable damage preview.",
              skin, "menu"));
        reset.setProgrammaticChangeEvents(false);
        reset.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                reset.setChecked(false);
                if (source != null) { source.resetConditionsPreview(); }
                conditionsPreview = false;
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

    private ScrollPane scroll(Skin skin, Table content, String name) {
        ScrollPane scroll = new ScrollPane(content, skin, "menu");
        scroll.setName(name);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setFlickScroll(false);
        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                panel.getStage().setScrollFocus(scroll);
            }
        });
        return scroll;
    }

    private void tab(Skin skin, Table tabs, ButtonGroup<TextButton> group, String label, ScrollPane page, ScrollPane other) {
        TextButton button = new TextButton(label, skin, "menu-control");
        button.setName("tuning-tab-" + label.toLowerCase(Locale.ROOT));
        group.add(button);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!button.isChecked()) { return; }
                if (panel.getStage() != null) {
                    panel.getStage().unfocus(other);
                    panel.getStage().setScrollFocus(page);
                }
                damageLocation.hideList();
                pressure.hideList();
                atmosphericTaint.hideList();
                page.setVisible(true);
                other.setVisible(false);
            }
        });
        tabs.add(button).growX().height(26).padRight(2);
    }

    private Table section(Skin skin, String title) {
        float spacing = rows.hasChildren() ? 8 : 0;
        Table heading = new Table();
        Label label = new Label(title.toUpperCase(Locale.ROOT), skin, "kicker");
        label.setEllipsis(true);
        heading.add(label).minWidth(0).growX();
        rows.add(heading).colspan(3).growX().padTop(spacing).padBottom(3).row();
        return heading;
    }

    private CheckBox checkbox(Skin skin, String label, String name) {
        CheckBox checkbox = new CheckBox(label, skin, "menu");
        checkbox.setName(name);
        checkbox.getImage().setScaling(Scaling.fit);
        checkbox.getImageCell().size(14).padRight(5);
        rows.add(checkbox).colspan(3).left().height(20).row();
        return checkbox;
    }

    private <T> SelectBox<T> choice(Skin skin, String label, String name, T[] values, Runnable apply) {
        SelectBox<T> choice = new SelectBox<>(skin, "menu");
        choice.setName(name);
        choice.setItems(values);
        choice.setMaxListCount(7);
        choice.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!syncing) { apply.run(); }
            }
        });
        rows.add(new Label(label, skin, "menu")).left().width(LABEL_WIDTH);
        rows.add(choice).colspan(2).minWidth(0).growX().height(24).row();
        return choice;
    }

    private ButtonGroup<TextButton> effectModes(Skin skin, String name) {
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        Table modes = new Table();
        for (GpuFieldOfView.Style style : GpuFieldOfView.Style.values()) {
            TextButton button = new TextButton(style.label, skin, "menu-control");
            button.setName(name + "-style-" + style.name());
            button.setUserObject(style);
            group.add(button);
            modes.add(button).width(92).height(22).padRight(2);
            if (modes.getChildren().size % 3 == 0) { modes.row(); }
        }
        rows.add(modes).colspan(3).left().padBottom(2).row();
        return group;
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

    /** Writes the current board values into the sliders, as the initial state and after a reset. */
    private void restoreDefaults() {
        normalMaps.setChecked(true);
        vsync.setChecked(GpuBoardWindow.DEFAULT_VSYNC);
        BoardGeometry.Tuning defaults = BoardGeometry.DEFAULTS;
        float[] values = { defaults.hexScale(), defaults.unitScale(), defaults.unitHeightScale(),
              defaults.levelHeight(), defaults.gridShade(), defaults.multiHexUnitScale() };
        setValues(geometry, values);
        applyGeometry();
        float[] familyDefaults = new float[familySizes.size()];
        for (int index = 0; index < familyDefaults.length; index++) {
            familyDefaults[index] = UnitFamilyScale.values()[index].defaultUnitScale;
        }
        setValues(familySizes, familyDefaults);
        applyFamilySizes();
        overviewIcons.setChecked(GpuUnitIcons.DEFAULT_ENABLED);
        setValues(overview, new float[] { GpuUnitIcons.DEFAULT_HEX_PIXELS });
        updateReadings(overview);
        setValues(visibility, new float[] { GpuTerrain.DEFAULT_BUILDING_OPACITY * 100,
              GpuUnitVisibility.DEFAULT_OUTLINE_INTENSITY * 100 });
        updateReadings(visibility);
        fovModes.getButtons().get(GpuFieldOfView.FOV_STYLE.ordinal()).setChecked(true);
        setValues(fieldOfView, new float[] { GpuFieldOfView.FOV_DARKNESS * 100 });
        sensorModes.getButtons().get(GpuFieldOfView.SENSOR_STYLE.ordinal()).setChecked(true);
        setValues(sensors, new float[] { GpuFieldOfView.SENSOR_DARKNESS * 100 });
        applyFieldOfView();
        setRenderingOptions(GpuAtmosphere.Options.DEFAULTS);
        setAtmosphere(lastScenario == null ? BoardAtmosphere.DEFAULTS : lastScenario);
        overrideDamage.setChecked(false);
        damageLocation.setSelected(UnitDamageDisplay.Location.ALL);
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

    /** Leave captured game gravity intact until a local conditions selection or gravity edit overrides it. */
    float gravityOverride() {
        return conditionsPreview || lastScenario != null && Math.abs(atmosphere.gravity() - lastScenario.gravity()) > 0.00001f
              ? atmosphere.gravity() : Float.NaN;
    }

    GpuAtmosphere.Options atmosphereOptions() {
        return new GpuAtmosphere.Options(value(rendering, 0), fixedSun.isChecked(), value(rendering, 1), value(rendering, 2),
              value(rendering, 3), value(rendering, 4), value(rendering, 5), value(rendering, 6), value(rendering, 7), value(rendering, 8));
    }

    private void applyRendering() {
        setRenderingOptions(atmosphereOptions());
    }

    private void setRenderingOptions(GpuAtmosphere.Options options) {
        fixedSun.setChecked(options.fixedSun());
        setValues(rendering, new float[] { options.rays(), options.minCloudShadow(), options.maxCloudShadow(), options.sunGlare(),
              options.fogHeightVariation(), options.fogDensityVariation(), options.moonShadowContrast(), options.taintStrength(),
              options.fogCalmDrift() });
        updateReadings(rendering);
    }

    boolean normalMaps() {
        return normalMaps.isChecked();
    }

    boolean fixedSun() {
        return fixedSun.isChecked();
    }

    boolean overviewIcons() { return overviewIcons.isChecked(); }

    void setOverviewIcons(boolean enabled) { overviewIcons.setChecked(enabled); }

    float overviewHexPixels() { return value(overview, 0); }

    void setFixedSun(boolean fixed) {
        fixedSun.setChecked(fixed);
    }

    UnitDamageDisplay.Location damageLocation() {
        return damageLocation.getSelected();
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

    GpuFieldOfView.Style sensorStyle() {
        return (GpuFieldOfView.Style) sensorModes.getChecked().getUserObject();
    }

    float sensorDarkness() {
        return value(sensors, 0) / 100;
    }

    /** Follow effective scenario changes while preserving individual visual overrides. */
    void useScenario(BoardAtmosphere.Settings initial, boolean newBoard) {
        if (lastScenario == null || newBoard) {
            conditionsPreview = false;
            setAtmosphere(initial);
        } else if (!conditionsPreview && lastScenario != null && !lastScenario.equals(initial)) {
            setAtmosphere(BoardAtmosphere.followScenario(atmosphere, lastScenario, initial));
        }
        lastScenario = initial;
    }

    /** An explicit Apply is an action, including when the selected conditions have not changed. */
    private void applyConditions(BoardAtmosphere.Settings settings) {
        conditionsPreview = true;
        setRenderingOptions(GpuAtmosphere.Options.DEFAULTS);
        setAtmosphere(settings);
    }

    private void setAtmosphere(BoardAtmosphere.Settings settings) {
        atmosphere = settings;
        syncing = true;
        moonlight.setChecked(settings.moonlight());
        pressure.setSelected(settings.pressure());
        atmosphericTaint.setSelected(settings.taint());
        // Preserve unusual loaded temperatures when editing an unrelated control.
        temperature.getFirst().slider().setRange(Math.min(-200, settings.temperature()), Math.max(200, settings.temperature()));
        gravity.getFirst().slider().setRange(0, Math.max(10, settings.gravity()));
        syncing = false;
        setValues(daylight, new float[] { settings.hour(), settings.exposure() });
        setValues(temperature, new float[] { settings.temperature() });
        setValues(gravity, new float[] { settings.gravity() });
        setValues(weather, new float[] { settings.clouds(), settings.fog(), settings.groundLayerHeight(), settings.haze() });
        BoardAtmosphere.Effects next = settings.effects();
        setValues(effects, new float[] { next.rain(), next.snow(), next.hail(), next.sand(), next.lightning(),
              next.wind(), next.windDirection() });
        boolean weatherAllowed = atmosphere.pressure().isDenserThan(Atmosphere.THIN);
        weather.getFirst().slider().setDisabled(atmosphere.pressure().isLighterThan(Atmosphere.THIN));
        weather.get(1).slider().setDisabled(!weatherAllowed);
        weather.get(2).slider().setDisabled(atmosphere.pressure().isVacuum());
        weather.get(3).slider().setDisabled(atmosphere.pressure().isVacuum());
        for (int index = 0; index < effects.size(); index++) {
            boolean disabled = atmosphere.pressure().isVacuum() || (index < 3 || index == 4) && !weatherAllowed;
            effects.get(index).slider().setDisabled(disabled);
            if (effects.get(index).toggle() != null) { effects.get(index).toggle().setDisabled(disabled); }
        }
        updateReadings(daylight);
        updateReadings(temperature);
        updateReadings(gravity);
        updateReadings(weather);
        updateReadings(effects);
        for (int index = 0; index < rendering.size(); index++) {
            rendering.get(index).slider().setDisabled(index < 3 && atmosphere.pressure().isLighterThan(Atmosphere.THIN)
                  || (index == 4 || index == 5 || index == 8) && !weatherAllowed || index == 6 && !atmosphere.moonlight()
                  || index == 7 && (atmosphere.pressure().isVacuum() || atmosphere.taint().isBreathable()));
        }
        updateReadings(rendering);
    }

    private void applyGeometry() {
        BoardGeometry.tune(new BoardGeometry.Tuning(value(geometry, 0), value(geometry, 1), value(geometry, 2),
              Math.round(value(geometry, 3)), value(geometry, 4), value(geometry, 5)));
        updateReadings(geometry);
    }

    private void applyFamilySizes() {
        for (int index = 0; index < familySizes.size(); index++) {
            UnitFamilyScale.values()[index].UNIT_SCALE = value(familySizes, index);
        }
        updateReadings(familySizes);
    }

    private void applyVisibility() {
        updateReadings(visibility);
    }

    private void applyOverview() { updateReadings(overview); }

    private void applyFieldOfView() {
        updateReadings(fieldOfView);
        updateReadings(sensors);
    }

    private void applyDamage() {
        damage.getFirst().slider().setDisabled(!overrideDamage.isChecked());
        damageLocation.setDisabled(!overrideDamage.isChecked());
        updateReadings(damage);
    }

    private void applyAtmosphere() {
        setAtmosphere(new BoardAtmosphere.Settings(value(daylight, 0), value(weather, 0), value(weather, 1),
              value(weather, 2), value(weather, 3), value(daylight, 1),
              new BoardAtmosphere.Effects(value(effects, 0), value(effects, 1), value(effects, 2), value(effects, 3),
                    value(effects, 4), value(effects, 5), value(effects, 6)), pressure.getSelected(),
              Math.round(value(temperature, 0)), moonlight.isChecked(), atmosphericTaint.getSelected(), value(gravity, 0)));
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
