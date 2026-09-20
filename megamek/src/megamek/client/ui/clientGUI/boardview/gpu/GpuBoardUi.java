/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.KeyCommandBind;
import megamek.common.board.Coords;

/** Target-anchored Scene2D menus; all game actions come from the Swing command adapter. */
final class GpuBoardUi implements Disposable {
    static final int FONT_RESOLUTION = 60;
    private static final int MENU_BAR_HEIGHT = 36;
    static final int TOP_HEIGHT = MENU_BAR_HEIGHT + 48;
    static final int TURN_HEIGHT = 100;
    private static final int MENU_WIDTH = 360;
    private static final int DROPDOWN_WIDTH = 300;
    private final GpuBoardSource source;
    private final BoardCamera camera;
    private final GpuBoardTuning tuning;
    private final GpuAttackPanel attackPanel;
    private final GpuReportPanel reportPanel;
    private final GpuBoardSkin theme = new GpuBoardSkin();
    private final Skin skin = theme.skin;
    private final GpuTextures<String> portraits = new GpuTextures<>();
    final Stage stage;
    private final Table hud = new Table();
    private record HudActor(Image image, GpuTextures<Integer> textures) { }
    private final List<HudActor> hudLayers = new ArrayList<>();
    private GpuBoardSource.Hud hudFrame;
    private final Table completion = new Table();
    private final Table menuBar = new Table();
    private final Table popup = new Table();
    private final Table rows = new Table();
    private final TextField search;
    private final Stack searchBox;
    private final Label title;
    private final Label subtitle;
    private final Label status;
    private final Label fps;
    private final Label phase;
    private final Label actor;
    private final Label actorMeta;
    private final Image portrait = new Image();
    private final Label help;
    private final Label details;
    private final ScrollPane scroll;
    private final TextButton back;
    private final List<String> path = new ArrayList<>();
    private final List<TextButton> rowButtons = new ArrayList<>();
    private final List<BoardScene.Command> rowCommands = new ArrayList<>();
    private List<BoardScene.Command> roots = List.of();
    private List<String> completionIds = List.of();
    private List<String> menuBarIds;
    private List<String> menuSignature = List.of();
    private GpuBoardSource.Frame frame;
    private Coords context;
    private String menu = "";
    private String popupTitle = "";
    private String pendingSearch = "";
    private int keyboardRow = -1;
    private boolean plotting;
    private boolean showingDetails;
    private float scale = 1;
    private float hudScale = 1;
    private float anchorX;
    private float anchorTop;
    private String menuTriggerName;

    GpuBoardUi(GpuBoardSource source, BoardCamera camera, Runnable changeSpeed) {
        this(source, camera, changeSpeed, () -> { });
    }

    GpuBoardUi(GpuBoardSource source, BoardCamera camera, Runnable changeSpeed, Runnable togglePlayback) {
        this.source = source;
        this.camera = camera;
        stage = new Stage(new ScreenViewport()) {
            @Override
            public boolean touchDown(int x, int y, int pointer, int button) {
                Vector2 point = screenToStageCoordinates(new Vector2(x, y));
                dismissMenuOutside(hit(point.x, point.y, true));
                return super.touchDown(x, y, pointer, button);
            }

            @Override
            public boolean mouseMoved(int x, int y) {
                Vector2 point = screenToStageCoordinates(new Vector2(x, y));
                Actor hovered = hit(point.x, point.y, true);
                while (hovered != null && !(hovered instanceof ScrollPane)) {
                    hovered = hovered.getParent();
                }
                setScrollFocus(hovered);
                return super.mouseMoved(x, y);
            }

            @Override
            public boolean scrolled(float x, float y) {
                Vector2 point = screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
                dismissMenuOutside(hit(point.x, point.y, true));
                return GpuBoardUi.this.hit(Gdx.input.getX(), Gdx.input.getY()) && super.scrolled(x, y);
            }
        };
        hud.setTouchable(Touchable.disabled);
        hud.setClip(true);
        stage.addActor(hud);
        Table root = new Table();
        root.setFillParent(true);
        root.setTouchable(Touchable.childrenOnly);
        stage.addActor(root);
        menuBar.setBackground(skin.getDrawable("menu-panel"));
        menuBar.setTouchable(Touchable.enabled);
        menuBar.pad(0, 12, 0, 12).left().defaults().height(26).padRight(4);
        phase = new Label("", skin, "kicker");
        phase.setEllipsis(true);
        root.add(menuBar).height(MENU_BAR_HEIGHT).growX().row();
        Table toolbar = panel();
        toolbar.setBackground(skin.getDrawable("menu-panel"));
        toolbar.pad(0, 12, 0, 12).defaults().height(30).padRight(4);
        toolbar.add(new Label("VIEW", skin, "muted")).padRight(12);
        toolbar.add(namedButton("top", Messages.getString("GpuBoard.top"), () -> camera.setIsometric(false))).width(78);
        toolbar.add(namedButton("iso", Messages.getString("GpuBoard.isometric"), () -> camera.setIsometric(true))).width(82);
        toolbar.add(button(Messages.getString("GpuBoard.fit"), () -> {
            if (frame != null) {
                camera.fit(frame.scene());
            }
        })).width(76);
        toolbar.add(button("-", () -> camera.zoom(1.2f))).width(30);
        toolbar.add(button("+", () -> camera.zoom(1 / 1.2f))).width(30);
        TextButton cameraControls = namedButton("camera", Messages.getString("GpuBoard.camera"),
              () -> open("camera", Messages.getString("GpuBoard.camera"), "camera"));
        cameraControls.addListener(new TextTooltip(Messages.getString("GpuBoard.cameraHelp"), skin));
        toolbar.add(cameraControls).width(70).padRight(12);
        toolbar.add(new Image(skin.getDrawable("rule"))).width(1).height(20).padRight(12);
        toolbar.add(namedButton("speed", "", changeSpeed)).width(88);
        toolbar.add(namedButton("playback", Messages.getString("GpuBoard.pausePlayback"), togglePlayback)).width(76);
        toolbar.add(namedButton("tuning", "Tuning", this::toggleTuning)).width(70);
        toolbar.add(namedButton("battle-report-toggle", "Report", this::toggleReport)).width(70);
        toolbar.add().expandX();
        status = new Label("", skin, "muted");
        status.setEllipsis(true);
        toolbar.add(status).minWidth(0).maxWidth(180).padLeft(8);
        root.add(toolbar).height(TOP_HEIGHT - MENU_BAR_HEIGHT).growX().row();
        root.add().grow().row();
        Table turn = panel();
        turn.pad(10, 14, 7, 14);
        Table unit = new Table();
        Table portraitFrame = new Table();
        portraitFrame.setBackground(skin.getDrawable("inset"));
        portraitFrame.pad(8);
        portrait.setScaling(Scaling.fit);
        portrait.setTouchable(Touchable.disabled);
        portraitFrame.add(portrait).size(44, 48);
        unit.add(portraitFrame).size(62, 64).padRight(12);
        Table identification = new Table();
        identification.add(new Label("ACTING UNIT", skin, "kicker")).growX().left().row();
        actor = new Label("", skin, "heading");
        actor.setEllipsis(true);
        actor.setName("acting-unit");
        identification.add(actor).minWidth(0).growX().left().padTop(2).row();
        actorMeta = new Label("", skin, "small");
        actorMeta.setEllipsis(true);
        identification.add(actorMeta).minWidth(0).growX().left().padTop(2);
        unit.add(identification).minWidth(0).growX();
        turn.add(unit).minWidth(180).growX().padRight(16);
        turn.add(actionButton("all-actions", "All actions  /  F10",
              () -> open("all", "All actions", "all-actions")))
              .width(132).height(42).padRight(6);
        turn.add(actionButton("orders", "Orders",
              () -> open("orders", "Planned orders", "orders")))
              .width(80).height(42).padRight(6);
        turn.add(actionButton("clear", Messages.getString("GpuBoard.clear"), () -> {
            if (frame != null) {
                frame.scene().commands().stream().filter(command -> command.id().equals("clear"))
                      .filter(BoardScene.Command::enabled).findFirst().ifPresent(command -> command.action().run());
            }
        })).width(104).height(42).padRight(6);
        turn.add(completion).right().row();
        help = new Label("", skin, "small");
        help.setEllipsis(true);
        Table footer = new Table();
        footer.add(help).minWidth(0).growX().left();
        fps = new Label("", skin, "small");
        fps.setName("fps");
        fps.setAlignment(Align.right);
        fps.setTouchable(Touchable.disabled);
        footer.add(fps).width(76).padLeft(8).right();
        turn.add(footer).colspan(5).minWidth(0).growX().height(16).padTop(4);
        root.add(turn).height(TURN_HEIGHT).growX();

        popup.setBackground(skin.getDrawable("menu-panel"));
        popup.setName("tactical-menu");
        popup.setTouchable(Touchable.enabled);
        popup.pad(8).top();
        Table cap = new Table();
        search = new TextField("", skin);
        search.setName("command-search");
        search.setProgrammaticChangeEvents(true);
        back = button("< Back", () -> {
            if (!path.isEmpty()) {
                path.removeLast();
            }
            search.setText("");
            resetRows();
        });
        back.setName("menu-back");
        cap.add(back).width(54).height(28).padRight(6);
        title = new Label("", new Label.LabelStyle(skin.getFont("bold-font"), GpuBoardSkin.TEXT));
        title.setWrap(true);
        cap.add(title).minWidth(0).growX().left().padLeft(4);
        TextButton dismiss = button("", this::closeMenu);
        dismiss.setName("close-menu");
        dismiss.clearChildren();
        dismiss.pad(0);
        dismiss.add(icon("close", GpuBoardSkin.MUTED)).size(14);
        dismiss.addListener(new TextTooltip("Close  /  Esc", skin));
        cap.add(dismiss).size(28).padLeft(8);
        popup.add(cap).growX().row();
        subtitle = new Label("", skin, "small");
        subtitle.setEllipsis(true);
        popup.add(subtitle).minWidth(0).growX().left().padLeft(4).row();
        Label searchHint = new Label("Search commands...", skin, "small");
        searchHint.setName("command-search-hint");
        searchHint.setEllipsis(true);
        Table fieldOverlay = new Table();
        fieldOverlay.setTouchable(Touchable.disabled);
        fieldOverlay.pad(0, 12, 0, 12);
        fieldOverlay.add(icon("search", GpuBoardSkin.MUTED)).size(16).padRight(10);
        fieldOverlay.add(searchHint).minWidth(0).growX().left();
        search.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                searchHint.setVisible(search.getText().isEmpty());
                resetRows();
            }
        });
        searchBox = new Stack(search, fieldOverlay);
        popup.add(searchBox).growX().height(30).padTop(6).row();
        rows.top();
        scroll = new ScrollPane(rows, skin, "menu");
        scroll.setName("command-scroll");
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setFlickScroll(false);
        popup.add(scroll).minHeight(0).grow().padTop(6).row();
        popup.add(new Image(skin.getDrawable("rule"))).height(1).growX().padTop(6).row();
        Label shortcuts = new Label("Up/Down Navigate   Enter Select   Esc Close", skin, "muted");
        shortcuts.setEllipsis(true);
        popup.add(shortcuts).minWidth(0).growX().left().pad(6, 4, 2, 4).row();
        details = new Label("", skin, "small");
        details.setWrap(true);
        popup.setVisible(false);
        attackPanel = new GpuAttackPanel(skin, this::executeCommand, id -> {
            open("weapons", "Weapons and ammunition", "attack:" + id);
            path.add("weapons");
            path.add(id);
            updateMenu();
        }, () -> open("all", "All actions", "attack-more"),
              () -> open("orders", "Planned orders", "attack-review-orders"));
        stage.addActor(attackPanel.panel());
        reportPanel = new GpuReportPanel(skin, this::toggleReport, source::reportUnit);
        stage.addActor(reportPanel.panel());
        stage.addActor(popup);
        tuning = new GpuBoardTuning(skin, source);
        tuning.panel().setVisible(false);
        stage.addActor(tuning.panel());
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int key) {
                if (!popup.isVisible()) {
                    if (key == Input.Keys.ESCAPE && reportPanel.panel().isVisible()) {
                        toggleReport();
                        return true;
                    }
                    return false;
                }
                if (key == Input.Keys.ESCAPE) {
                    closeMenu();
                    return true;
                }
                if (key == Input.Keys.UP || key == Input.Keys.DOWN || key == Input.Keys.TAB) {
                    focusRow(key == Input.Keys.UP ? -1 : 1);
                    return true;
                }
                if (key == Input.Keys.ENTER && keyboardRow >= 0 && keyboardRow < rowCommands.size()) {
                    choose(rowCommands.get(keyboardRow));
                    return true;
                }
                return false;
            }
        });
    }

    private Image icon(String name, Color color) {
        Image image = new Image(skin.getDrawable("icon-" + name));
        image.setColor(color);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private Table panel() {
        Table result = new Table();
        result.setTouchable(Touchable.enabled);
        result.setBackground(skin.getDrawable("bar"));
        return result;
    }

    private TextButton namedButton(String name, String text, Runnable action) {
        TextButton result = button(text, action);
        result.setName(name);
        return result;
    }

    private TextButton actionButton(String name, String text, Runnable action) {
        TextButton result = namedButton(name, text, action);
        result.setStyle(skin.get("toolbar", TextButton.TextButtonStyle.class));
        return result;
    }

    private TextButton button(String text, Runnable action) {
        TextButton result = new TextButton(text, skin, "menu-control");
        result.pad(4, 10, 4, 10);
        result.setProgrammaticChangeEvents(false);
        result.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                result.setChecked(false);
                action.run();
            }
        });
        return result;
    }

    void resize(int width, int height, float displayScale) {
        scale = displayScale;
        hudScale = scale / source.uiPreferences.scale();
        ((ScreenViewport) stage.getViewport()).setUnitsPerPixel(1 / scale);
        stage.getViewport().update(width, height, true);
        attackPanel.resize(stage.getWidth(), stage.getHeight());
        reportPanel.resize(stage.getWidth(), stage.getHeight());
        // Match the integer board viewport so native HUD pixels are not resampled at fractional edges.
        hud.setBounds(0, bottomPixels() / scale, stage.getWidth(),
              Math.max(1, height - topPixels() - bottomPixels()) / scale);
        if (popup.isVisible()) {
            menuSignature = List.of();
            updateMenu();
        }
        if (tuning.visible()) {
            tuning.resize(stage.getWidth(), stage.getHeight(), TOP_HEIGHT, TURN_HEIGHT);
        }
    }

    private void toggleTuning() {
        if (!tuning.visible()) {
            tuning.resize(stage.getWidth(), stage.getHeight(), TOP_HEIGHT, TURN_HEIGHT);
        }
        tuning.toggle();
    }

    private void toggleReport() {
        closeMenu();
        reportPanel.toggle();
        stage.setKeyboardFocus(null);
        stage.setScrollFocus(null);
        if (frame != null) {
            attackPanel.panel().setVisible(frame.attack() != null && !reportPanel.panel().isVisible());
        }
    }

    BitmapFont font() {
        return skin.getFont("default-font");
    }

    BoardAtmosphere.Settings atmosphere() {
        return tuning.atmosphere();
    }

    boolean normalMaps() {
        return tuning.normalMaps();
    }

    double speedGainPerHex() { return tuning.speedGainPerHex(); }

    float damageOverride() {
        return tuning.damageOverride();
    }

    float buildingOpacity() {
        return tuning.buildingOpacity();
    }

    float seeThrough() {
        return tuning.seeThrough();
    }

    GpuFieldOfView.Style fovStyle() {
        return tuning.fovStyle();
    }

    float fovDarkness() {
        return tuning.fovDarkness();
    }

    GpuFieldOfView.Style sensorStyle() {
        return tuning.sensorStyle();
    }

    float sensorDarkness() {
        return tuning.sensorDarkness();
    }

    float hudScale() {
        // Existing overlay painters already apply the user's GUI-scale preference themselves.
        return hudScale;
    }

    int topPixels() {
        return Math.round(TOP_HEIGHT * scale);
    }

    int bottomPixels() {
        return Math.round(TURN_HEIGHT * scale);
    }

    /** Unobstructed board width in window pixels; both side panels keep their actual, possibly resized bounds. */
    float cameraWidth() {
        float right = stage.getWidth();
        for (var panel : List.of(attackPanel.panel(), reportPanel.panel())) {
            if (panel.isVisible()) { right = Math.min(right, panel.getX() - 8); }
        }
        return Math.max(1, right * scale);
    }

    void updateHud(GpuBoardSource.Hud next, long now) {
        if (next == null) {
            return;
        }
        if (hudFrame != next) {
            while (hudLayers.size() > next.layers().size()) {
                HudActor removed = hudLayers.removeLast();
                removed.image().remove();
                removed.textures().dispose();
            }
            while (hudLayers.size() < next.layers().size()) {
                Image layer = new Image();
                layer.setTouchable(Touchable.disabled);
                hudLayers.add(new HudActor(layer, new GpuTextures<>()));
                hud.addActor(layer);
            }
            for (int index = 0; index < hudLayers.size(); index++) {
                HudActor layer = hudLayers.get(index);
                // Adding or expiring a toast must not repack and upload every other HUD panel's texture.
                layer.textures().update(Map.of(0, next.layers().get(index).pixels()));
                layer.image().setDrawable(new TextureRegionDrawable(layer.textures().region(0)));
            }
            hudFrame = next;
        }
        float scaleX = hud.getWidth() / next.width();
        float scaleY = hud.getHeight() / next.height();
        for (int index = 0; index < hudLayers.size(); index++) {
            GpuBoardSource.HudLayer layer = next.layers().get(index);
            Image actor = hudLayers.get(index).image();
            actor.setBounds(layer.x() * scaleX,
                  hud.getHeight() - (layer.y() + layer.shiftY().value(now) + layer.pixels().height()) * scaleY,
                  layer.pixels().width() * scaleX, layer.pixels().height() * scaleY);
            actor.getColor().a = layer.fade().opacity(now);
        }
    }

    void setPlaybackPaused(boolean paused) {
        ((TextButton) stage.getRoot().findActor("playback")).setText(Messages.getString(
              paused ? "GpuBoard.resumePlayback" : "GpuBoard.pausePlayback"));
    }

    void update(GpuBoardSource.Frame next, String speed) {
        tuning.useScenario(next.scenarioAtmosphere(), frame == null || frame.boardGeneration() != next.boardGeneration()
              || frame.scene().boardId() != next.scene().boardId());
        if (frame != null && (frame.scene().selectedId() != next.scene().selectedId()
              || frame.boardGeneration() != next.boardGeneration()
              || !frame.scene().phase().equals(next.scene().phase()))) {
            closeMenu();
            plotting = false;
        }
        frame = next;
        attackPanel.update(frame);
        boolean reportWasVisible = reportPanel.panel().isVisible();
        reportPanel.update(frame.reports(), frame.scene().selectedId());
        if (reportWasVisible && !reportPanel.panel().isVisible()) {
            stage.setKeyboardFocus(null);
            stage.setScrollFocus(null);
        }
        attackPanel.panel().setVisible(frame.attack() != null && !reportPanel.panel().isVisible());
        ((TextButton) stage.getRoot().findActor("battle-report-toggle")).setChecked(reportPanel.panel().isVisible());
        updateMenuBar();
        updateHud(frame.hud(), System.nanoTime());
        phase.setText(frame.scene().phase().toUpperCase(Locale.ROOT) + "  /  PHASE");
        status.setText("MAP " + (frame.scene().boardId() + 1) + "  /  " + frame.scene().width() + " \u00d7 " + frame.scene().height());
        ((TextButton) stage.getRoot().findActor("top")).setChecked(camera.isTopDown());
        ((TextButton) stage.getRoot().findActor("iso")).setChecked(camera.isIsometric());
        ((TextButton) stage.getRoot().findActor("speed")).setText(speed);
        actor.setText(frame.actorName().isEmpty() ? "No unit selected" : frame.actorName());
        updateActor();
        help.setText(plotting ? "BOARD TOOL ACTIVE   /   Click to plot or select   \u00b7   Right-click: commands   \u00b7   Esc: exit tool"
              : "Click: commands   \u00b7   Right-drag: pan   \u00b7   Shift + right-drag: orbit   \u00b7   Wheel: zoom");
        help.setColor(plotting ? GpuBoardSkin.ACCENT : Color.WHITE);
        List<BoardScene.Command> commits = frame.scene().commands().stream().filter(BoardScene.Command::commit).toList();
        List<String> ids = commits.stream().map(command -> command.id() + command.label()).toList();
        if (!ids.equals(completionIds)) {
            completionIds = ids;
            completion.clearChildren();
            for (int i = 0; i < commits.size(); i++) {
                String id = commits.get(i).id();
                completion.add(button(commits.get(i).label(), () -> frame.scene().commands().stream()
                      .filter(command -> command.commit() && command.enabled() && command.id().equals(id))
                      .findFirst().ifPresent(command -> command.action().run())))
                      .width(126).height(46).padLeft(6);
            }
        }
        for (int i = 0; i < commits.size(); i++) {
            TextButton commit = (TextButton) completion.getChildren().get(i);
            commit.setStyle(skin.get("primary", TextButton.TextButtonStyle.class));
            commit.setDisabled(!commits.get(i).enabled());
        }
        ((TextButton) stage.getRoot().findActor("clear")).setDisabled(frame.scene().commands().stream()
              .noneMatch(command -> command.id().equals("clear") && command.enabled()));
        if (popup.isVisible()) {
            updateMenu();
        }
    }

    private void updateActor() {
        BoardScene.Unit selected = frame.scene().units().stream()
              .filter(unit -> unit.id() == frame.scene().selectedId() && !unit.sensorContact()).findFirst().orElse(null);
        Map<String, BoardScene.Pixels> images = new HashMap<>();
        if (selected != null && selected.image() != null && !frame.actorName().isEmpty()) {
            images.put("actor", selected.image());
        }
        portraits.update(images);
        portrait.setDrawable(images.isEmpty() ? skin.getDrawable("icon-unit")
              : new TextureRegionDrawable(portraits.region("actor")));
        portrait.setColor(images.isEmpty() ? GpuBoardSkin.MUTED : Color.WHITE);
        String location = selected == null ? frame.scene().phase()
              : "HEX " + selected.location().coords().getBoardNum() + "  /  " + frame.scene().phase();
        actorMeta.setText(frame.actorName().isEmpty() ? Messages.getString("GpuBoard.selectUnit") : location);
    }

    private void updateMenuBar() {
        List<BoardScene.Command> commands = frame.globalCommands();
        List<String> ids = commands.stream().map(command -> command.id() + command.label()).toList();
        if (!ids.equals(menuBarIds)) {
            menuBarIds = ids;
            menuBar.clearChildren();
            menuBar.add(icon("unit", GpuBoardSkin.ACCENT)).size(18).padRight(8);
            menuBar.add(new Label("MEGAMEK", skin, "kicker")).padRight(16);
            for (BoardScene.Command command : commands) {
                TextButton item = namedButton("menu:" + command.id(), command.label(), () -> openGlobalMenu(command.id()));
                item.addListener(new InputListener() {
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        if (pointer == -1 && popup.isVisible() && menu.equals("global")
                              && !path.isEmpty() && !path.getFirst().equals(command.id())) {
                            openGlobalMenu(command.id());
                        }
                    }
                });
                menuBar.add(item).minWidth(52).padRight(4);
            }
            menuBar.add().growX();
            menuBar.add(phase).minWidth(0).maxWidth(240).right();
        }
        for (BoardScene.Command command : commands) {
            TextButton item = menuBar.findActor("menu:" + command.id());
            item.setDisabled(!command.enabled());
            item.setChecked(popup.isVisible() && menu.equals("global") && !path.isEmpty()
                  && path.getFirst().equals(command.id()));
        }
    }

    private void openGlobalMenu(String id) {
        BoardScene.Command command = frame.globalCommands().stream().filter(item -> item.id().equals(id))
              .findFirst().orElse(null);
        if (command == null || !command.enabled()) {
            return;
        }
        if (command.children().isEmpty()) {
            command.action().run();
            closeMenu();
            return;
        }
        open("global", command.label(), "menu:" + id);
        path.add(id);
        updateMenu();
    }

    private void updateMenu() {
        details.setText(frame.tooltip());
        roots = switch (menu) {
            case "all", "weapons" -> frame.scene().commands().stream().filter(command -> !command.commit()).toList();
            case "global" -> frame.globalCommands();
            case "camera" -> cameraCommands();
            case "orders" -> List.of();
            default -> contextCommands();
        };
        List<BoardScene.Command> commands = roots;
        String heading = popupTitle;
        String description = "";
        if (menu.equals("context")) {
            List<String> names = frame.scene().units().stream().filter(unit -> unit.footprint().contains(context))
                  .map(BoardScene.Unit::name).distinct().toList();
            if (!names.isEmpty()) {
                heading = names.size() == 1 ? names.getFirst() : names.size() + " units in hex";
            }
            BoardScene.Tile tile = frame.scene().tile(context);
            description = popupTitle + "  /  " + frame.scene().phase()
                  + (tile == null ? "" : "  /  Elevation " + tile.elevation());
        }
        for (String id : path) {
            BoardScene.Command group = commands.stream().filter(command -> command.id().equals(id)).findFirst()
                  .orElse(null);
            if (group == null) {
                path.clear();
                commands = roots;
                break;
            }
            heading = group.label();
            commands = group.children();
        }
        title.setText(heading);
        subtitle.setText(description);
        subtitle.setVisible(!description.isEmpty());
        popup.getCell(subtitle).height(description.isEmpty() ? 0 : subtitle.getPrefHeight())
              .padBottom(description.isEmpty() ? 0 : 2);
        boolean canGoBack = path.size() > (menu.equals("global") ? 1 : 0);
        back.setDisabled(!canGoBack);
        back.setVisible(canGoBack);
        ((Table) back.getParent()).getCell(back).width(canGoBack ? 54 : 0).height(canGoBack ? 28 : 0)
              .padRight(canGoBack ? 6 : 0);
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            commands = search(roots, query, "");
        }
        List<String> signature = new ArrayList<>(commands.stream()
              .map(command -> command.id() + command.label() + command.enabled() + command.detail()
                    + command.boardTool() + command.children().isEmpty()).toList());
        signature.add(menu + path + query + showingDetails + (menu.equals("orders") ? orderSummary() : ""));
        signature.add(heading);
        if (showingDetails) {
            signature.add(frame.tooltip());
        }
        if (signature.equals(menuSignature) && query.equals(pendingSearch)) {
            return;
        }
        boolean sameQuery = pendingSearch.equals(query);
        pendingSearch = query;
        menuSignature = signature;
        float scrollY = scroll.getScrollY();
        String focusedId = sameQuery && keyboardRow >= 0 && keyboardRow < rowCommands.size()
              ? rowCommands.get(keyboardRow).id() : "";
        boolean rowFocused = rowButtons.contains(stage.getKeyboardFocus());
        rows.clearChildren();
        rowButtons.clear();
        rowCommands.clear();
        keyboardRow = -1;
        if (commands.isEmpty()) {
            Label information = new Label(menu.equals("orders") ? orderSummary() : "No matching commands.\nTry a different search.", skin, "small");
            information.setWrap(true);
            rows.add(information).width(rowWidth()).pad(12, 8, 12, 8).left().row();
        }
        for (BoardScene.Command command : commands) {
            if (command.id().equals("Details") && query.isEmpty()) {
                rows.add(new Image(skin.getDrawable("rule"))).width(rowWidth()).height(1).pad(5, 0, 5, 0).row();
            }
            TextButton row = commandRow(command);
            rows.add(row).width(rowWidth()).minHeight(32).row();
            rowButtons.add(row);
            rowCommands.add(command);
            if (command.id().equals(focusedId) && command.enabled()) {
                keyboardRow = rowButtons.size() - 1;
            }
        }
        if (showingDetails && menu.equals("context")) {
            details.setText(frame.tooltip());
            Table intel = new Table();
            intel.setBackground(skin.getDrawable("inset"));
            intel.pad(12);
            intel.add(new Label("FIELD INTELLIGENCE", skin, "kicker")).left().row();
            intel.add(details).width(rowWidth() - 24).padTop(6).left();
            rows.add(intel).width(rowWidth()).padTop(8).row();
        }
        positionPopup();
        scroll.setScrollY(scrollY);
        if (rowFocused) {
            if (keyboardRow >= 0) {
                rowButtons.get(keyboardRow).setChecked(true);
                stage.setKeyboardFocus(rowButtons.get(keyboardRow));
            } else {
                focusMenu();
            }
        }
    }

    private float rowWidth() {
        return menuWidth() - 24;
    }

    private void resetRows() {
        menuSignature = List.of();
        keyboardRow = -1;
        scroll.setScrollY(0);
    }

    private TextButton commandRow(BoardScene.Command command) {
        TextButton row = button(command.label(), () -> choose(command));
        row.setName(command.id());
        row.setStyle(skin.get("menu-row", TextButton.TextButtonStyle.class));
        row.clearChildren();
        row.pad(6, 10, 6, 10);
        String symbol = command.boardTool() ? "move" : null;
        if (command.id().equals("board.los") || command.id().startsWith("weapon")) {
            symbol = "target";
        } else if (command.id().equals("Details")) {
            symbol = "info";
        } else if (command.id().equals("All actions")) {
            symbol = "group";
        }
        if (!menu.equals("global") && !menu.equals("camera")) {
            row.add(symbol == null ? null : icon(symbol,
                  command.enabled() ? GpuBoardSkin.ACCENT : GpuBoardSkin.DISABLED)).size(16).padRight(8);
        }
        Table caption = new Table();
        row.getLabel().setWrap(true);
        row.getLabel().setAlignment(Align.left);
        caption.add(row.getLabel()).minWidth(0).growX().left().row();
        String detail = command.detail().strip();
        if (!command.enabled() && detail.isEmpty()) {
            detail = "Unavailable for the current unit or phase.";
        }
        if (!detail.isEmpty()) {
            if (command.enabled() && !menu.equals("global")) {
                Label summary = new Label(detail.replaceAll("\\s+", " "), skin, "small");
                summary.setEllipsis(true);
                caption.add(summary).minWidth(0).growX().left().padTop(2);
            }
            row.addListener(new TextTooltip(detail, skin));
        }
        row.add(caption).minWidth(0).growX();
        if (!command.enabled() || !command.children().isEmpty()) {
            row.add(icon(command.enabled() ? "arrow" : "lock",
                  command.enabled() ? GpuBoardSkin.ACCENT : GpuBoardSkin.DISABLED)).size(16).padLeft(8);
        }
        row.setDisabled(!command.enabled());
        return row;
    }

    private String orderSummary() {
        List<String> information = new ArrayList<>();
        if (frame.attack() != null) {
            information.addAll(frame.attack().orders());
        }
        if (frame.scene().plannedPath().size() > 1) {
            information.add((frame.scene().plannedPath().size() - 1) + " movement steps; destination "
                  + frame.scene().plannedPath().getLast().coords().getBoardNum());
        }
        if (frame.attack() == null) {
            frame.scene().commands().stream().filter(BoardScene.Command::commit).filter(BoardScene.Command::enabled)
                  .map(BoardScene.Command::detail).filter(detail -> !detail.isBlank()).distinct().forEach(information::add);
        }
        return information.isEmpty() ? "No planned orders to display." : String.join("\n\n", information);
    }

    private List<BoardScene.Command> contextCommands() {
        List<BoardScene.Command> result = new ArrayList<>();
        if (frame.context() != null && Objects.equals(frame.context().coords(), context)) {
            result.addAll(frame.context().commands());
        }
        result.add(new BoardScene.Command("Details", true, () -> {
            showingDetails = !showingDetails;
            menuSignature = List.of();
            updateMenu();
            if (showingDetails) {
                scroll.setScrollPercentY(1);
            }
        }));
        result.add(new BoardScene.Command("All actions", true,
              () -> open("all", "All actions", anchorX, anchorTop, menuTriggerName)));
        return result;
    }

    private List<BoardScene.Command> cameraCommands() {
        return List.of(
              new BoardScene.Command(Messages.getString("GpuBoard.rotateLeft"), true, () -> camera.orbit(-15, 0)),
              new BoardScene.Command(Messages.getString("GpuBoard.rotateRight"), true, () -> camera.orbit(15, 0)),
              new BoardScene.Command(Messages.getString("GpuBoard.tiltUp"), true, () -> camera.orbit(0, -10)),
              new BoardScene.Command(Messages.getString("GpuBoard.tiltDown"), true, () -> camera.orbit(0, 10)),
              new BoardScene.Command(Messages.getString("GpuBoard.resetCamera"), true, () -> camera.reset(frame.scene())));
    }

    private static List<BoardScene.Command> search(List<BoardScene.Command> commands, String query, String parent) {
        List<BoardScene.Command> found = new ArrayList<>();
        for (BoardScene.Command command : commands) {
            String label = parent + command.label();
            if (!command.children().isEmpty()) {
                found.addAll(search(command.children(), query, label + " / "));
            } else if ((label + " " + command.detail()).toLowerCase(Locale.ROOT).contains(query)) {
                found.add(new BoardScene.Command(command.id(), label, command.detail(), command.enabled(),
                      command.commit(), command.boardTool(), List.of(), command.action()));
            }
        }
        return found;
    }

    private void choose(BoardScene.Command command) {
        BoardScene.Command current = find(roots, command.id());
        if (current == null) {
            return;
        }
        command = current;
        if (!command.enabled()) {
            return;
        }
        if (!command.children().isEmpty()) {
            path.add(command.id());
            search.setText("");
            resetRows();
            focusMenu();
            return;
        }
        if (menu.equals("global") && cameraCommand(command)) {
            return;
        }
        executeCommand(command);
        // Weapon choices can stay open for salvos. Board plotting remains an explicit tool choice.
    }

    private void executeCommand(BoardScene.Command command) {
        String leaf = command.id().substring(command.id().lastIndexOf('/') + 1);
        if (leaf.equals("reportReport") || leaf.startsWith(ClientGUI.VIEW_ROUND_REPORT + ":")) {
            toggleReport();
            return;
        }
        command.action().run();
        if (command.boardTool()) {
            plotting = true;
        }
        if (command.boardTool() || command.id().equals("board.useHex")) {
            closeMenu();
        }
    }

    private boolean cameraCommand(BoardScene.Command command) {
        // Global camera menu entries address the active renderer, using the existing client action IDs.
        String leaf = command.id().substring(command.id().lastIndexOf('/') + 1);
        String action = leaf.substring(0, Math.max(0, leaf.indexOf(':')));
        switch (action) {
            case ClientGUI.VIEW_ZOOM_IN -> camera.zoom(1 / 1.2f);
            case ClientGUI.VIEW_ZOOM_OUT -> camera.zoom(1.2f);
            case ClientGUI.VIEW_ZOOM_OVERVIEW_TOGGLE -> camera.toggleOverview(frame.scene());
            case ClientGUI.VIEW_TOGGLE_ISOMETRIC -> camera.setIsometric(!camera.isIsometric());
            default -> { return false; }
        }
        return true;
    }

    static BoardScene.Command find(List<BoardScene.Command> commands, String id) {
        for (BoardScene.Command command : commands) {
            if (command.id().equals(id)) {
                return command;
            }
            BoardScene.Command child = find(command.children(), id);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    void inspect(Coords coords, int screenX, int screenY) {
        if (coords == null) {
            closeMenu();
            return;
        }
        context = coords;
        source.inspect(coords);
        Vector2 point = stage.screenToStageCoordinates(new Vector2(screenX, screenY));
        open("context", "Hex " + coords.getBoardNum(), point.x + 4, point.y, null);
    }

    private void open(String kind, String heading, String triggerName) {
        open(kind, heading, 0, 0, triggerName);
    }

    private void open(String kind, String heading, float x, float top, String triggerName) {
        source.stopKeys();
        menu = kind;
        anchorX = x;
        anchorTop = top;
        menuTriggerName = triggerName;
        popupTitle = heading;
        path.clear();
        search.setText("");
        boolean searchable = menu.equals("all");
        searchBox.setVisible(searchable);
        popup.getCell(searchBox).height(searchable ? 30 : 0).padTop(searchable ? 6 : 0);
        showingDetails = false;
        resetRows();
        popup.setVisible(true);
        popup.clearActions();
        popup.getColor().a = 0;
        popup.addAction(Actions.fadeIn(0.1f));
        positionPopup();
        stage.setScrollFocus(scroll);
        focusMenu();
        if (frame != null) {
            updateMenu();
        }
    }

    private float menuWidth() {
        int preferred = menu.equals("global") || menu.equals("camera") || menu.equals("weapons")
              ? DROPDOWN_WIDTH : MENU_WIDTH;
        return Math.min(preferred, Math.max(160, stage.getWidth() - 16));
    }

    private void focusMenu() {
        stage.setKeyboardFocus(searchBox.isVisible() ? search : popup);
    }

    private void positionPopup() {
        popup.setWidth(menuWidth());
        popup.invalidateHierarchy();
        // Let wrapped titles and command descriptions determine the height before clamping the scroll area.
        popup.validate();
        float height = Math.min(popup.getPrefHeight(), 620);
        float x;
        float y;
        Actor menuTrigger = menuTriggerName == null ? null : stage.getRoot().findActor(menuTriggerName);
        if (menuTrigger != null) {
            // Resolve the bars' new layout before reading a trigger's position after a resize.
            ((Table) menuBar.getParent()).validate();
            Vector2 bottom = menuTrigger.localToStageCoordinates(new Vector2());
            Vector2 top = menuTrigger.localToStageCoordinates(new Vector2(menuTrigger.getWidth(), menuTrigger.getHeight()));
            float below = Math.max(1, bottom.y - 6);
            float above = Math.max(1, stage.getHeight() - top.y - 6);
            boolean openAbove = below < height && above > below;
            height = Math.min(height, openAbove ? above : below);
            y = openAbove ? top.y + 2 : bottom.y - height - 2;
            x = bottom.x + menuWidth() <= stage.getWidth() - 4 ? bottom.x : top.x - menuWidth();
            x = MathUtils.clamp(x, 4, Math.max(4, stage.getWidth() - menuWidth() - 4));
            y = MathUtils.clamp(y, 4, Math.max(4, stage.getHeight() - height - 4));
        } else {
            float upperEdge = stage.getHeight() - TOP_HEIGHT - 8;
            height = Math.min(height, Math.max(1, upperEdge - TURN_HEIGHT - 8));
            float right = attackPanel.panel().isVisible() ? attackPanel.panel().getX() - 8 : stage.getWidth();
            x = MathUtils.clamp(anchorX, 8, Math.max(8, right - menuWidth() - 8));
            y = MathUtils.clamp(anchorTop - height, TURN_HEIGHT + 8, upperEdge - height);
        }
        popup.setBounds(x, y, menuWidth(), height);
        popup.validate();
    }

    private void dismissMenuOutside(Actor target) {
        if (popup.isVisible() && (target == null || !target.isDescendantOf(popup))) {
            Actor scrolling = stage.getScrollFocus();
            closeMenu();
            // Let the same wheel gesture continue scrolling the panel under the pointer.
            if (scrolling != null && !scrolling.isDescendantOf(popup)) {
                stage.setScrollFocus(scrolling);
            }
        }
    }

    void closeMenu() {
        popup.clearActions();
        popup.setVisible(false);
        Actor focus = stage.getKeyboardFocus();
        if (focus == null || !reportPanel.panel().isVisible() || !focus.isDescendantOf(reportPanel.panel())) {
            stage.setKeyboardFocus(null);
        }
        stage.setScrollFocus(null);
        source.inspect(null);
    }

    private void focusRow(int direction) {
        if (rowButtons.isEmpty()) {
            return;
        }
        int next = keyboardRow;
        if (next < 0 && direction < 0) {
            next = 0;
        }
        for (int count = 0; count < rowButtons.size(); count++) {
            next = Math.floorMod(next + direction, rowButtons.size());
            if (!rowButtons.get(next).isDisabled()) {
                keyboardRow = next;
                break;
            }
        }
        if (keyboardRow < 0 || rowButtons.get(keyboardRow).isDisabled()) {
            return;
        }
        for (int i = 0; i < rowButtons.size(); i++) {
            rowButtons.get(i).setChecked(i == keyboardRow);
        }
        TextButton row = rowButtons.get(keyboardRow);
        stage.setKeyboardFocus(row);
        scroll.scrollTo(0, row.getY(), row.getWidth(), row.getHeight());
    }

    boolean key(int key, boolean down) {
        int modifiers = GpuBattleView.modifiers();
        List<KeyCommandBind> bindings = KeyCommandBind.getAllBindsByKey(GpuBattleView.awtKey(key), modifiers);
        boolean unbound = bindings.isEmpty();
        if (down && bindings.contains(KeyCommandBind.CANCEL)) {
            plotting = false;
        }
        if (down && bindings.contains(KeyCommandBind.ROUND_REPORT)) {
            toggleReport();
            return true;
        }
        if (down && unbound && modifiers == 0 && key == Input.Keys.F10) {
            open("all", "All actions", "all-actions");
            return true;
        }
        if (down && unbound && modifiers == 0 && key == Input.Keys.F9) {
            toggleTuning();
            return true;
        }
        if (down && key == Input.Keys.ESCAPE && modifiers == 0) {
            plotting = false;
            if (popup.isVisible()) {
                closeMenu();
                return true;
            }
            if (tuning.visible()) {
                tuning.toggle();
                return true;
            }
            if (reportPanel.panel().isVisible()) {
                toggleReport();
                return true;
            }
        }
        return false;
    }

    boolean plotting() {
        return plotting;
    }

    boolean acceptsCameraKeys() {
        return !popup.isVisible() && !reportPanel.panel().isVisible();
    }

    boolean hit(int x, int y) {
        Vector2 point = stage.screenToStageCoordinates(new Vector2(x, y));
        return stage.hit(point.x, point.y, true) != null;
    }

    void draw() {
        fps.setText(Gdx.graphics.getFramesPerSecond() + " FPS");
        stage.getViewport().apply();
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 0.1f));
        stage.draw();
    }
    @Override
    public void dispose() {
        stage.dispose();
        theme.dispose();
        hudLayers.forEach(layer -> layer.textures().dispose());
        portraits.dispose();
        reportPanel.dispose();
    }
}
