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
import megamek.common.board.Coords;

/** Target-anchored Scene2D menus; all game actions come from the Swing command adapter. */
final class GpuBoardUi implements Disposable {
    static final int FONT_RESOLUTION = 60;
    private static final int MENU_BAR_HEIGHT = 36;
    static final int TOP_HEIGHT = MENU_BAR_HEIGHT + 48;
    static final int TURN_HEIGHT = 100;
    private static final int MENU_WIDTH = 400;
    private final GpuBoardSource source;
    private final BoardCamera camera;
    private final GpuBoardTuning tuning;
    private final GpuBoardSkin theme = new GpuBoardSkin();
    private final Skin skin = theme.skin;
    private final GpuTextures<String> hudTextures = new GpuTextures<>();
    private final GpuTextures<String> portraits = new GpuTextures<>();
    final Stage stage;
    private final Image hud = new Image();
    private final Table completion = new Table();
    private final Table menuBar = new Table();
    private final Table popup = new Table();
    private final Table rows = new Table();
    private final TextField search;
    private final Label title;
    private final Label eyebrow;
    private final Label subtitle;
    private final Label section;
    private final Label status;
    private final Label phase;
    private final Label actor;
    private final Label actorMeta;
    private final Image portrait = new Image();
    private final Image contextIcon;
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

    GpuBoardUi(GpuBoardSource source, BoardCamera camera, Runnable changeSpeed) {
        this.source = source;
        this.camera = camera;
        stage = new Stage(new ScreenViewport()) {
            @Override
            public boolean scrolled(float x, float y) {
                return GpuBoardUi.this.hit(Gdx.input.getX(), Gdx.input.getY()) && super.scrolled(x, y);
            }
        };
        hud.setTouchable(Touchable.disabled);
        stage.addActor(hud);
        Table root = new Table();
        root.setFillParent(true);
        root.setTouchable(Touchable.childrenOnly);
        stage.addActor(root);
        menuBar.setBackground(skin.getDrawable("bar"));
        menuBar.setTouchable(Touchable.enabled);
        menuBar.pad(0, 12, 0, 12).left().defaults().height(26).padRight(4);
        phase = new Label("", skin, "kicker");
        phase.setEllipsis(true);
        root.add(menuBar).height(MENU_BAR_HEIGHT).growX().row();
        Table toolbar = panel();
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
              () -> open("camera", Messages.getString("GpuBoard.camera"), 280, stage.getHeight() - TOP_HEIGHT));
        cameraControls.addListener(new TextTooltip(Messages.getString("GpuBoard.cameraHelp"), skin));
        toolbar.add(cameraControls).width(70).padRight(12);
        toolbar.add(new Image(skin.getDrawable("rule"))).width(1).height(20).padRight(12);
        toolbar.add(namedButton("speed", "", changeSpeed)).width(88);
        toolbar.add(namedButton("tuning", "Tuning", this::toggleTuning)).width(70);
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
        turn.add(namedButton("all-actions", "All actions  /  F10",
              () -> open("all", "All actions", 16, stage.getHeight() - TOP_HEIGHT - 12)))
              .width(132).height(42).padRight(6);
        turn.add(button("Orders", () -> open("orders", "Planned orders", 16, stage.getHeight() - TOP_HEIGHT - 12)))
              .width(80).height(42).padRight(6);
        turn.add(namedButton("clear", Messages.getString("GpuBoard.clear"), () -> {
            if (frame != null) {
                frame.scene().commands().stream().filter(command -> command.id().equals("clear"))
                      .filter(BoardScene.Command::enabled).findFirst().ifPresent(command -> command.action().run());
            }
        })).width(104).height(42).padRight(6);
        turn.add(completion).right().row();
        help = new Label("", skin, "small");
        help.setEllipsis(true);
        turn.add(help).colspan(5).minWidth(0).growX().height(16).padTop(4).left();
        root.add(turn).height(TURN_HEIGHT).growX();

        popup.setBackground(skin.getDrawable("panel"));
        popup.setName("tactical-menu");
        popup.setTouchable(Touchable.enabled);
        popup.pad(12, 16, 10, 16).top();
        Table cap = new Table();
        eyebrow = new Label("", skin, "kicker");
        eyebrow.setEllipsis(true);
        cap.add(eyebrow).minWidth(0).growX().left();
        TextButton dismiss = button("", this::closeMenu);
        dismiss.setStyle(skin.get("icon", TextButton.TextButtonStyle.class));
        dismiss.setName("close-menu");
        dismiss.clearChildren();
        dismiss.pad(0);
        dismiss.add(icon("close", GpuBoardSkin.MUTED)).size(14);
        dismiss.addListener(new TextTooltip("Close  /  Esc", skin));
        cap.add(dismiss).size(24).padLeft(8);
        popup.add(cap).growX().row();
        Table heading = new Table();
        contextIcon = icon("hex", GpuBoardSkin.ACCENT);
        heading.add(contextIcon).size(32).padRight(12);
        Table description = new Table();
        title = new Label("", skin, "heading");
        title.setWrap(true);
        description.add(title).minWidth(0).growX().left().row();
        subtitle = new Label("", skin, "small");
        subtitle.setEllipsis(true);
        description.add(subtitle).minWidth(0).growX().left().padTop(3);
        heading.add(description).minWidth(0).growX();
        popup.add(heading).growX().padTop(4).padBottom(8).row();
        popup.add(new Image(skin.getDrawable("rule"))).height(1).growX().row();
        search = new TextField("", skin);
        search.setName("command-search");
        search.setProgrammaticChangeEvents(true);
        back = button("BACK", () -> {
            if (!path.isEmpty()) {
                path.removeLast();
            }
            search.setText("");
            resetRows();
        });
        back.setName("menu-back");
        Table navigation = new Table();
        navigation.add(back).width(60).height(24).padRight(10);
        section = new Label("", skin, "muted");
        navigation.add(section).growX().left();
        popup.add(navigation).growX().padTop(8).row();
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
        popup.add(new Stack(search, fieldOverlay)).growX().height(36).padTop(6).row();
        rows.top();
        scroll = new ScrollPane(rows, skin);
        scroll.setName("command-scroll");
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setFlickScroll(false);
        popup.add(scroll).minHeight(0).grow().padTop(8).row();
        popup.add(new Image(skin.getDrawable("rule"))).height(1).growX().padTop(6).row();
        Label shortcuts = new Label("UP / DOWN  NAVIGATE     ENTER  SELECT     ESC  CLOSE", skin, "muted");
        popup.add(shortcuts).left().padTop(6).row();
        details = new Label("", skin, "small");
        details.setWrap(true);
        popup.setVisible(false);
        stage.addActor(popup);
        tuning = new GpuBoardTuning(skin);
        tuning.panel().setVisible(false);
        stage.addActor(tuning.panel());
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int key) {
                if (!popup.isVisible()) {
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

    private TextButton button(String text, Runnable action) {
        TextButton result = new TextButton(text, skin, "toolbar");
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
        hud.setBounds(0, TURN_HEIGHT, stage.getWidth(), Math.max(1, stage.getHeight() - TOP_HEIGHT - TURN_HEIGHT));
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

    BitmapFont boldFont() {
        return skin.getFont("bold-font");
    }

    BoardAtmosphere.Settings atmosphere() {
        return tuning.atmosphere();
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
        updateMenuBar();
        if (frame.hud() != null) {
            hudTextures.update(Map.of("hud", frame.hud()));
            hud.setDrawable(new TextureRegionDrawable(hudTextures.region("hud")));
        }
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
            ((TextButton) menuBar.findActor("menu:" + command.id())).setDisabled(!command.enabled());
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
        TextButton item = menuBar.findActor("menu:" + id);
        Vector2 point = item.localToStageCoordinates(new Vector2(0, 0));
        open("global", command.label(), point.x, point.y);
        path.add(id);
        updateMenu();
    }

    private void updateMenu() {
        details.setText(frame.tooltip());
        roots = switch (menu) {
            case "all" -> frame.scene().commands().stream().filter(command -> !command.commit()).toList();
            case "global" -> frame.globalCommands();
            case "camera" -> cameraCommands();
            case "orders" -> List.of();
            default -> contextCommands();
        };
        List<BoardScene.Command> commands = roots;
        String heading = popupTitle;
        String description = frame.scene().phase() + "  /  Command console";
        String symbol = menu.equals("orders") ? "orders" : "group";
        eyebrow.setText(menu.equals("context") ? "TACTICAL COMMAND  /  " + popupTitle.toUpperCase(Locale.ROOT)
              : "TACTICAL COMMAND  /  " + menu.toUpperCase(Locale.ROOT));
        if (menu.equals("context")) {
            List<String> names = frame.scene().units().stream().filter(unit -> unit.location().coords().equals(context))
                  .map(BoardScene.Unit::name).distinct().toList();
            if (!names.isEmpty()) {
                heading = names.size() == 1 ? names.getFirst() : names.size() + " units in hex";
            }
            symbol = names.isEmpty() ? "hex" : "target";
            BoardScene.Tile tile = frame.scene().tile(context);
            description = frame.scene().phase() + (tile == null ? "" : "  /  Elevation " + tile.elevation());
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
        contextIcon.setDrawable(skin.getDrawable("icon-" + symbol));
        back.setDisabled(path.isEmpty());
        back.setVisible(!path.isEmpty());
        ((Table) back.getParent()).getCell(back).width(path.isEmpty() ? 0 : 60).height(path.isEmpty() ? 0 : 24)
              .padRight(path.isEmpty() ? 0 : 10);
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            commands = search(roots, query, "");
        }
        section.setText((query.isEmpty() ? "COMMANDS" : "SEARCH RESULTS") + "  /  "
              + String.format(Locale.ROOT, "%02d", commands.size()));
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
                rows.add(new Label("INTELLIGENCE & CONTROLS", skin, "muted")).left().padTop(7).padBottom(5).row();
            }
            TextButton row = commandRow(command);
            rows.add(row).width(rowWidth()).minHeight(command.detail().isBlank() && command.enabled() ? 44 : 54)
                  .row();
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
        positionPopup(anchorX, anchorTop);
        scroll.setScrollY(scrollY);
        if (rowFocused) {
            if (keyboardRow >= 0) {
                rowButtons.get(keyboardRow).setChecked(true);
                stage.setKeyboardFocus(rowButtons.get(keyboardRow));
            } else {
                stage.setKeyboardFocus(search);
            }
        }
    }

    private float rowWidth() {
        return menuWidth() - 44;
    }

    private void resetRows() {
        menuSignature = List.of();
        keyboardRow = -1;
        scroll.setScrollY(0);
    }

    private TextButton commandRow(BoardScene.Command command) {
        TextButton row = button(command.label(), () -> choose(command));
        row.setName(command.id());
        row.setStyle(skin.get("action", TextButton.TextButtonStyle.class));
        row.clearChildren();
        row.pad(10, 20, 10, 20);
        String symbol = !command.children().isEmpty() ? "group" : command.boardTool() ? "move" : "arrow";
        if (command.id().equals("board.los") || command.id().startsWith("weapon")) {
            symbol = "target";
        } else if (command.id().equals("Details")) {
            symbol = "info";
        } else if (command.id().equals("All actions")) {
            symbol = "group";
        }
        row.add(icon(symbol, command.enabled() ? GpuBoardSkin.ACCENT : GpuBoardSkin.DISABLED)).size(22).padRight(12);
        Table caption = new Table();
        row.getLabel().setWrap(true);
        row.getLabel().setAlignment(Align.left);
        caption.add(row.getLabel()).minWidth(0).growX().left().row();
        String detail = command.detail().strip();
        if (!command.enabled() && detail.isEmpty()) {
            detail = "Unavailable for the current unit or phase.";
        }
        if (!detail.isEmpty()) {
            Label summary = new Label(detail.replaceAll("\\s+", " "), skin, "small");
            summary.setEllipsis(true);
            caption.add(summary).minWidth(0).growX().left().padTop(3);
            row.addListener(new TextTooltip(detail, skin));
        }
        row.add(caption).minWidth(0).growX();
        if (!command.enabled() || !command.children().isEmpty()) {
            row.add(icon(command.enabled() ? "arrow" : "lock", GpuBoardSkin.MUTED)).size(14).padLeft(6);
        }
        row.setDisabled(!command.enabled());
        return row;
    }

    private String orderSummary() {
        List<String> information = new ArrayList<>();
        if (frame.scene().plannedPath().size() > 1) {
            information.add((frame.scene().plannedPath().size() - 1) + " movement steps; destination "
                  + frame.scene().plannedPath().getLast().coords().getBoardNum());
        }
        frame.scene().commands().stream().filter(BoardScene.Command::commit).filter(BoardScene.Command::enabled)
              .map(BoardScene.Command::detail).filter(detail -> !detail.isBlank()).distinct().forEach(information::add);
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
        result.add(new BoardScene.Command("All actions", true, () -> open("all", "All actions", popup.getX(), popup.getTop())));
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
            stage.setKeyboardFocus(search);
            return;
        }
        if (menu.equals("global") && cameraCommand(command)) {
            return;
        }
        command.action().run();
        if (command.boardTool()) {
            plotting = true;
            closeMenu();
        }
        // Weapon and target menus stay open for salvos. Board plotting remains an explicit tool choice.
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

    private static BoardScene.Command find(List<BoardScene.Command> commands, String id) {
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
        open("context", "Hex " + coords.getBoardNum(), point.x + 14, point.y);
    }

    private void open(String kind, String heading, float x, float top) {
        source.stopKeys();
        menu = kind;
        anchorX = x;
        anchorTop = top;
        popupTitle = heading;
        path.clear();
        search.setText("");
        showingDetails = false;
        resetRows();
        popup.setVisible(true);
        popup.clearActions();
        popup.getColor().a = 0;
        popup.addAction(Actions.fadeIn(0.1f));
        positionPopup(x, top);
        stage.setScrollFocus(scroll);
        stage.setKeyboardFocus(search);
        if (frame != null) {
            updateMenu();
        }
    }

    private float menuWidth() {
        return Math.min(MENU_WIDTH, Math.max(160, stage.getWidth() - 16));
    }

    private void positionPopup(float x, float top) {
        float upperEdge = stage.getHeight() - (menu.equals("global") ? MENU_BAR_HEIGHT : TOP_HEIGHT);
        popup.setWidth(menuWidth());
        popup.invalidateHierarchy();
        // Let wrapped titles and command descriptions determine the height before clamping the scroll area.
        popup.validate();
        float height = Math.min(popup.getPrefHeight(), Math.min(620, Math.max(1, upperEdge - TURN_HEIGHT - 16)));
        popup.setSize(menuWidth(), height);
        popup.setPosition(MathUtils.clamp(x, 8, stage.getWidth() - menuWidth() - 8),
              MathUtils.clamp(top - height, TURN_HEIGHT + 8, upperEdge - height - 8));
        popup.validate();
    }

    void closeMenu() {
        popup.clearActions();
        popup.setVisible(false);
        stage.setKeyboardFocus(null);
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
        if (down && key == Input.Keys.F10) {
            open("all", "All actions", 16, stage.getHeight() - TOP_HEIGHT - 12);
            return true;
        }
        if (down && key == Input.Keys.F9) {
            toggleTuning();
            return true;
        }
        if (down && key == Input.Keys.ESCAPE) {
            plotting = false;
            closeMenu();
            return true;
        }
        return false;
    }

    boolean plotting() {
        return plotting;
    }

    boolean acceptsCameraKeys() {
        return !popup.isVisible();
    }

    boolean hit(int x, int y) {
        Vector2 point = stage.screenToStageCoordinates(new Vector2(x, y));
        return stage.hit(point.x, point.y, true) != null;
    }

    void draw() {
        stage.getViewport().apply();
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 0.1f));
        stage.draw();
    }
    @Override
    public void dispose() {
        stage.dispose();
        theme.dispose();
        hudTextures.dispose();
        portraits.dispose();
    }
}
