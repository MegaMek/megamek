/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.common.Configuration;
import megamek.common.board.Coords;

/** Target-anchored Scene2D menus; all game actions come from the Swing command adapter. */
final class GpuBoardUi implements Disposable {
    static final int FONT_RESOLUTION = 60;
    private static final int MENU_BAR_HEIGHT = 32;
    static final int TOP_HEIGHT = MENU_BAR_HEIGHT + 48;
    static final int TURN_HEIGHT = 72;
    private static final int MENU_WIDTH = 340;
    private static final String[] HINT_MODES = { "Smart", "Hold", "Always" };
    private final GpuBoardSource source;
    private final BoardCamera camera;
    private final FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(
          new FileHandle(new File(Configuration.fontsDir(), "Noto Sans/NotoSans-Regular.ttf")));
    private final Skin skin = createSkin();
    private final GpuTextures<String> hudTextures = new GpuTextures<>();
    final Stage stage;
    private final Image hud = new Image();
    private final Table completion = new Table();
    private final Table menuBar = new Table();
    private final Table popup = new Table();
    private final Table rows = new Table();
    private final TextField search;
    private final Label title;
    private final Label status;
    private final Label actor;
    private final Label help;
    private final Label details;
    private final ScrollPane scroll;
    private final TextButton back;
    private final TextButton hints;
    private final List<String> path = new ArrayList<>();
    private final List<TextButton> rowButtons = new ArrayList<>();
    private final List<BoardScene.Command> rowCommands = new ArrayList<>();
    private List<BoardScene.Command> roots = List.of();
    private List<String> completionIds = List.of();
    private List<String> menuBarIds = List.of();
    private List<String> menuSignature = List.of();
    private GpuBoardSource.Frame frame;
    private Coords context;
    private String menu = "";
    private String popupTitle = "";
    private String pendingSearch = "";
    private int hintMode;
    private int hintKey = Input.Keys.SPACE;
    private int keyboardRow = -1;
    private boolean hintHeld;
    private boolean rebinding;
    private boolean plotting;
    private boolean showingDetails;
    private float scale = 1;
    private float hudScale = 1;
    private float anchorX;
    private float anchorTop;

    GpuBoardUi(GpuBoardSource source, BoardCamera camera, Runnable changeSpeed) {
        this.source = source;
        this.camera = camera;
        hintMode = MathUtils.clamp(source.uiPreferences.hintMode(), 0, HINT_MODES.length - 1);
        if (source.uiPreferences.hintKey() > 0) {
            hintKey = source.uiPreferences.hintKey();
        }
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
        menuBar.setBackground(skin.newDrawable("white", Color.valueOf("0D1720")));
        menuBar.setTouchable(Touchable.enabled);
        menuBar.left().defaults().height(MENU_BAR_HEIGHT - 4).pad(2);
        root.add(menuBar).height(MENU_BAR_HEIGHT).growX().row();
        Table toolbar = panel();
        toolbar.defaults().height(34).pad(4);
        toolbar.add(namedButton("top", Messages.getString("GpuBoard.top"), () -> camera.setIsometric(false))).width(82);
        toolbar.add(namedButton("iso", Messages.getString("GpuBoard.isometric"), () -> camera.setIsometric(true))).width(88);
        toolbar.add(button(Messages.getString("GpuBoard.fit"), () -> {
            if (frame != null) {
                camera.fit(frame.scene());
            }
        })).width(80);
        toolbar.add(button("-", () -> camera.zoom(1.2f))).width(30);
        toolbar.add(button("+", () -> camera.zoom(1 / 1.2f))).width(30);
        TextButton cameraControls = namedButton("camera", Messages.getString("GpuBoard.camera"),
              () -> open("camera", Messages.getString("GpuBoard.camera"), 280, stage.getHeight() - TOP_HEIGHT));
        cameraControls.addListener(new TextTooltip(Messages.getString("GpuBoard.cameraHelp"), skin));
        toolbar.add(cameraControls).width(76);
        toolbar.add(namedButton("speed", "", changeSpeed)).width(94);
        hints = button("", () -> open("hints", "Interaction hints", 500, stage.getHeight() - TOP_HEIGHT));
        toolbar.add(hints).width(118);
        toolbar.add().expandX();
        status = new Label("", skin);
        status.setEllipsis(true);
        toolbar.add(status).minWidth(0).maxWidth(180).padRight(12);
        root.add(toolbar).height(TOP_HEIGHT - MENU_BAR_HEIGHT).growX().row();
        root.add().grow().row();
        Table turn = panel();
        turn.defaults().pad(6).height(34);
        actor = new Label("", skin);
        actor.setEllipsis(true);
        turn.add(actor).minWidth(130).growX().left();
        turn.add(namedButton("all-actions", "All actions", () -> open("all", "All actions", 16, 480))).width(116);
        turn.add(button("Orders", () -> open("orders", "Planned orders", 16, 480))).width(80);
        turn.add(namedButton("clear", Messages.getString("GpuBoard.clear"), () -> {
            if (frame != null) {
                frame.scene().commands().stream().filter(command -> command.id().equals("clear"))
                      .filter(BoardScene.Command::enabled).findFirst().ifPresent(command -> command.action().run());
            }
        })).width(110);
        turn.add(completion).right().row();
        help = new Label("", skin);
        help.setEllipsis(true);
        turn.add(help).colspan(5).growX().height(18).padTop(0).left();
        root.add(turn).height(TURN_HEIGHT).growX();

        popup.setBackground(skin.newDrawable("white", Color.valueOf("172431")));
        popup.setTouchable(Touchable.enabled);
        popup.pad(12).top();
        Table heading = new Table();
        title = new Label("", skin);
        title.setWrap(true);
        heading.add(title).width(MENU_WIDTH - 78).growX().left();
        heading.add(button("X", this::closeMenu)).size(30);
        popup.add(heading).growX().row();
        search = new TextField("", skin);
        back = button("< Back", () -> {
            if (!path.isEmpty()) {
                path.removeLast();
            }
            search.setText("");
            menuSignature = List.of();
        });
        popup.add(back).growX().height(30).padTop(8).row();
        search.setMessageText("Search actions...");
        search.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                menuSignature = List.of();
            }
        });
        popup.add(search).growX().height(34).padTop(8).row();
        rows.top();
        scroll = new ScrollPane(rows, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        popup.add(scroll).grow().padTop(10).row();
        details = new Label("", skin);
        details.setWrap(true);
        popup.setVisible(false);
        stage.addActor(popup);
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

    private Table panel() {
        Table result = new Table();
        result.setTouchable(Touchable.enabled);
        result.setBackground(skin.newDrawable("white", Color.valueOf("111B26")));
        return result;
    }

    private TextButton namedButton(String name, String text, Runnable action) {
        TextButton result = button(text, action);
        result.setName(name);
        return result;
    }

    private TextButton button(String text, Runnable action) {
        TextButton result = new TextButton(text, skin);
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
            positionPopup(popup.getX(), popup.getTop());
        }
    }

    BitmapFont font() {
        return skin.getFont("default-font");
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
        status.setText(frame.scene().phase() + " | " + Gdx.graphics.getFramesPerSecond() + " FPS");
        ((TextButton) stage.getRoot().findActor("top")).setChecked(camera.isTopDown());
        ((TextButton) stage.getRoot().findActor("iso")).setChecked(camera.isIsometric());
        ((TextButton) stage.getRoot().findActor("speed")).setText(speed);
        hints.setText("Hints: " + HINT_MODES[hintMode]);
        actor.setText(frame.actorName().isEmpty() ? Messages.getString("GpuBoard.selectUnit") : "Acting: " + frame.actorName());
        help.setText(rebinding ? "Press the key to hold for hints. Esc cancels."
              : plotting ? "Board tool active | Click to plot / select a hex | Right-click for choices | Esc exits tool"
                    : "Click: choices | Right-drag: pan | Shift+right-drag: rotate/tilt | Wheel: zoom | "
                          + Input.Keys.toString(hintKey) + ": hints");
        List<BoardScene.Command> commits = frame.scene().commands().stream().filter(BoardScene.Command::commit).toList();
        List<String> ids = commits.stream().map(command -> command.id() + command.label()).toList();
        if (!ids.equals(completionIds)) {
            completionIds = ids;
            completion.clearChildren();
            for (int i = 0; i < commits.size(); i++) {
                int index = i;
                completion.add(button(commits.get(i).label(), () -> frame.scene().commands().stream()
                      .filter(BoardScene.Command::commit).toList().get(index).action().run()))
                      .width(130).height(34).padLeft(6);
            }
        }
        for (int i = 0; i < commits.size(); i++) {
            ((TextButton) completion.getChildren().get(i)).setDisabled(!commits.get(i).enabled());
        }
        ((TextButton) stage.getRoot().findActor("clear")).setDisabled(frame.scene().commands().stream()
              .noneMatch(command -> command.id().equals("clear") && command.enabled()));
        if (popup.isVisible()) {
            updateMenu();
        }
    }

    private void updateMenuBar() {
        List<BoardScene.Command> commands = frame.globalCommands();
        List<String> ids = commands.stream().map(command -> command.id() + command.label()).toList();
        if (!ids.equals(menuBarIds)) {
            menuBarIds = ids;
            menuBar.clearChildren();
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
                menuBar.add(item).minWidth(60).padRight(5);
            }
            menuBar.add().growX();
            menuBar.add(new Label("MegaMek | 3D board", skin)).padRight(12);
        }
        for (int i = 0; i < commands.size(); i++) {
            ((TextButton) menuBar.getChildren().get(i)).setDisabled(!commands.get(i).enabled());
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
            case "hints" -> hintCommands();
            case "camera" -> cameraCommands();
            case "orders" -> List.of();
            default -> contextCommands();
        };
        List<BoardScene.Command> commands = roots;
        String heading = popupTitle;
        if (menu.equals("context")) {
            List<String> names = frame.scene().units().stream().filter(unit -> unit.location().coords().equals(context))
                  .map(BoardScene.Unit::name).distinct().toList();
            if (!names.isEmpty()) {
                heading = (names.size() == 1 ? names.getFirst() : names.size() + " units") + "\n" + popupTitle;
            }
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
        back.setDisabled(path.isEmpty());
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            commands = search(roots, query, "");
        }
        List<String> signature = new ArrayList<>(commands.stream()
              .map(command -> command.id() + command.label() + command.enabled() + command.detail()).toList());
        signature.add(menu + path + query + showingDetails + (menu.equals("orders") ? orderSummary() : ""));
        if (signature.equals(menuSignature) && query.equals(pendingSearch)) {
            return;
        }
        pendingSearch = query;
        menuSignature = signature;
        float scrollY = scroll.getScrollY();
        rows.clearChildren();
        rowButtons.clear();
        rowCommands.clear();
        keyboardRow = -1;
        if (commands.isEmpty()) {
            Label information = new Label(menu.equals("orders") ? orderSummary() : "No matching actions", skin);
            information.setWrap(true);
            rows.add(information).width(MENU_WIDTH - 36).left().row();
        }
        for (BoardScene.Command command : commands) {
            TextButton row = button(command.label() + (command.children().isEmpty() ? "" : "  >"), () -> choose(command));
            row.setName(command.id());
            row.getLabel().setWrap(true);
            row.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
            row.setDisabled(!command.enabled());
            String detail = command.detail() + (command.enabled() ? "" : "\nUnavailable for the current unit or phase.");
            if (!detail.isBlank()) {
                row.addListener(new TextTooltip(detail.strip(), skin));
            }
            rows.add(row).width(MENU_WIDTH - 28).minHeight(38).padBottom(5).row();
            if (!command.enabled()) {
                Label reason = new Label(command.detail().isBlank() ? "Unavailable for the current unit or phase"
                      : command.detail(), skin);
                reason.setWrap(true);
                reason.setColor(Color.valueOf("9BAABA"));
                rows.add(reason).width(MENU_WIDTH - 36).padBottom(8).left().row();
            }
            rowButtons.add(row);
            rowCommands.add(command);
        }
        if (showingDetails && menu.equals("context")) {
            details.setText(frame.tooltip());
            rows.add(details).width(MENU_WIDTH - 28).padTop(10).row();
        }
        scroll.layout();
        scroll.setScrollY(scrollY);
        positionPopup(anchorX, anchorTop);
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
        }));
        result.add(new BoardScene.Command("All actions", true, () -> open("all", "All actions", popup.getX(), popup.getTop())));
        return result;
    }

    private List<BoardScene.Command> hintCommands() {
        List<BoardScene.Command> result = new ArrayList<>();
        for (int i = 0; i < HINT_MODES.length; i++) {
            int mode = i;
            result.add(new BoardScene.Command(HINT_MODES[i] + (hintMode == i ? " (active)" : ""), true,
                  () -> {
                      hintMode = mode;
                      source.saveHints(hintMode, hintKey);
                  }));
        }
        result.add(new BoardScene.Command("Change hold key: " + Input.Keys.toString(hintKey), true, () -> {
            rebinding = true;
            closeMenu();
        }));
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
            menuSignature = List.of();
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
        menuSignature = List.of();
        popup.setVisible(true);
        positionPopup(x, top);
        stage.setScrollFocus(scroll);
        stage.setKeyboardFocus(search);
        if (frame != null) {
            updateMenu();
        }
    }

    private void positionPopup(float x, float top) {
        float upperEdge = stage.getHeight() - (menu.equals("global") ? MENU_BAR_HEIGHT : TOP_HEIGHT);
        float height = Math.min(Math.max(180, rows.getPrefHeight() + 146),
              Math.min(560, upperEdge - TURN_HEIGHT - 16));
        popup.setSize(MENU_WIDTH, height);
        popup.setPosition(MathUtils.clamp(x, 8, stage.getWidth() - MENU_WIDTH - 8),
              MathUtils.clamp(top - height, TURN_HEIGHT + 8, upperEdge - height - 8));
    }

    void closeMenu() {
        popup.setVisible(false);
        stage.setKeyboardFocus(null);
        stage.setScrollFocus(null);
        source.inspect(null);
    }

    private void focusRow(int direction) {
        if (rowButtons.isEmpty()) {
            return;
        }
        keyboardRow = Math.floorMod(keyboardRow + direction, rowButtons.size());
        for (int i = 0; i < rowButtons.size(); i++) {
            rowButtons.get(i).setChecked(i == keyboardRow);
        }
        TextButton row = rowButtons.get(keyboardRow);
        stage.setKeyboardFocus(row);
        scroll.scrollTo(0, row.getY(), row.getWidth(), row.getHeight());
    }

    boolean key(int key, boolean down) {
        if (rebinding && down) {
            if (key != Input.Keys.ESCAPE) {
                hintKey = key;
                source.saveHints(hintMode, hintKey);
            }
            rebinding = false;
            return true;
        }
        if (key == hintKey) {
            hintHeld = down;
            return true;
        }
        if (down && key == Input.Keys.F10) {
            open("all", "All actions", 16, 480);
            return true;
        }
        if (down && key == Input.Keys.ESCAPE) {
            plotting = false;
            closeMenu();
            return true;
        }
        return false;
    }

    boolean hintsFor(BoardScene.Unit unit, Coords hovered) {
        return hintHeld || hintMode == 2 || hintMode == 0
              && (unit.id() == frame.scene().selectedId() || unit.location().coords().equals(hovered));
    }

    boolean allHints() {
        return hintHeld || hintMode == 2;
    }

    boolean plotting() {
        return plotting;
    }

    boolean acceptsCameraKeys() {
        return !popup.isVisible();
    }

    void releaseInput() {
        hintHeld = false;
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

    private Skin createSkin() {
        Skin result = new Skin();
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        result.add("white", new Texture(pixel));
        pixel.dispose();
        FreeTypeFontGenerator.FreeTypeFontParameter fontSettings = new FreeTypeFontGenerator.FreeTypeFontParameter();
        // Rasterize above the logical font size so DPI scaling keeps glyphs legible.
        fontSettings.size = FONT_RESOLUTION;
        fontSettings.incremental = true;
        fontSettings.minFilter = Texture.TextureFilter.Linear;
        fontSettings.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = fontGenerator.generateFont(fontSettings);
        font.getData().setScale(0.25f);
        result.add("default-font", font);
        result.add("default", new Label.LabelStyle(font, Color.valueOf("E4EAF2")));
        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = font;
        button.fontColor = Color.valueOf("E4EAF2");
        button.disabledFontColor = Color.valueOf("8899AB");
        button.up = result.newDrawable("white", Color.valueOf("253344"));
        button.over = result.newDrawable("white", Color.valueOf("354B62"));
        button.down = result.newDrawable("white", Color.valueOf("156A77"));
        button.checked = button.down;
        button.disabled = result.newDrawable("white", Color.valueOf("18222D"));
        result.add("default", button);
        ScrollPane.ScrollPaneStyle scrolling = new ScrollPane.ScrollPaneStyle();
        scrolling.vScroll = result.newDrawable("white", Color.valueOf("111B26"));
        scrolling.vScrollKnob = result.newDrawable("white", Color.valueOf("60788D"));
        scrolling.vScroll.setMinWidth(6);
        scrolling.vScrollKnob.setMinWidth(6);
        scrolling.vScrollKnob.setMinHeight(24);
        result.add("default", scrolling);
        TextField.TextFieldStyle field = new TextField.TextFieldStyle(font, Color.WHITE,
              result.newDrawable("white", Color.CYAN), result.newDrawable("white", Color.valueOf("354B62")),
              result.newDrawable("white", Color.valueOf("0D1720")));
        field.messageFontColor = Color.valueOf("9BAABA");
        field.background.setLeftWidth(8);
        field.background.setRightWidth(8);
        result.add("default", field);
        result.add("default", new TextTooltip.TextTooltipStyle(result.get(Label.LabelStyle.class),
              result.newDrawable("white", Color.valueOf("111B26"))));
        return result;
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        fontGenerator.dispose();
        hudTextures.dispose();
    }
}
