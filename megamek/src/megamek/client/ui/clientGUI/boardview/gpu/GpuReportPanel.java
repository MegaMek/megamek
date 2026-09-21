/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import megamek.client.ui.util.KeyCommandBind;

/** Native report reader. Filters and disclosure state belong to this view; resolution text comes from the client. */
final class GpuReportPanel implements Disposable {
    private static final String ALL_PHASES = "All phases";
    private static final GpuReportLog.Unit ALL_UNITS = new GpuReportLog.Unit(-1, "All units");
    private static final Color AMBER = Color.valueOf("D8BC82");
    private record Turn(int number) {
        @Override
        public String toString() {
            return number < 0 ? "Latest turn" : number == 0 ? "All turns" : "Turn " + number;
        }
    }

    private final Skin skin;
    private final IntConsumer inspectUnit;
    private final GpuTextures<Integer> icons = new GpuTextures<>();
    private final Table panel = new Table();
    private final Table filters = new Table();
    private final Table rows = new Table();
    private final ScrollPane scroll;
    private final SelectBox<Turn> turns;
    private final SelectBox<String> phases;
    private final SelectBox<GpuReportLog.Unit> units;
    private final SelectBox<String> keywords;
    private final SelectBox<String> filterKeywords;
    private final TextButton keywordFilter;
    private final TextField search;
    private final Label status;
    private final Label current;
    private final TextButton latest;
    private final TextButton selected;
    private final Set<String> collapsed = new HashSet<>();
    private final Map<GpuReportLog.Entry, Boolean> detailOverrides = new LinkedHashMap<>();
    private final Map<GpuReportLog.Entry, GpuReportLog.Link> detailLinks = new LinkedHashMap<>();
    private final Map<Integer, Table> eventRows = new LinkedHashMap<>();
    private GpuBoardSource.UiPreferences keywordPreferences;
    private int keywordMatch = -1;
    private GpuReportLog.Snapshot snapshot = GpuReportLog.Snapshot.EMPTY;
    private List<GpuReportLog.Entry> filtered = List.of();
    private int selectedId = -1;
    private boolean changing;
    private boolean followLatest = true;
    private boolean showDetails;
    private boolean narrowFilters;
    private boolean keywordFilterEnabled;

    GpuReportPanel(Skin skin, Runnable close, IntConsumer inspectUnit) {
        this.skin = skin;
        this.inspectUnit = inspectUnit;
        panel.setName("battle-report");
        panel.setBackground(skin.getDrawable("panel"));
        panel.setTouchable(Touchable.enabled);
        panel.pad(12).top();
        Table heading = new Table();
        heading.add(new Label("BATTLE REPORT", skin, "kicker")).left();
        current = new Label("", skin, "small");
        current.setEllipsis(true);
        heading.add(current).minWidth(0).growX().padLeft(12);
        latest = button("report-latest", "Latest", this::latest);
        latest.addListener(new TextTooltip("Follow the latest reported turn and phase. Clears the filters.", skin));
        heading.add(latest).height(26).padRight(6);
        heading.add(button("report-close", "Close", close)).height(26);
        panel.add(heading).growX().padBottom(8).row();

        var listStyle = new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle(skin.getFont("small-font"),
              GpuBoardSkin.TEXT, GpuBoardSkin.MUTED, skin.get("menu-row", TextButton.TextButtonStyle.class).checked);
        listStyle.background = skin.getDrawable("menu-panel");
        NinePatchDrawable selectorBackground = new NinePatchDrawable((NinePatchDrawable) skin.getDrawable("inset"));
        selectorBackground.setRightWidth(24);
        var selectStyle = new SelectBox.SelectBoxStyle(skin.getFont("small-font"), GpuBoardSkin.TEXT,
              selectorBackground, skin.get("menu", ScrollPane.ScrollPaneStyle.class), listStyle);
        turns = new SelectBox<>(selectStyle);
        phases = new SelectBox<>(selectStyle);
        units = new SelectBox<>(selectStyle);
        keywords = new SelectBox<>(selectStyle);
        filterKeywords = new SelectBox<>(selectStyle);
        search = new TextField("", skin);
        turns.setName("report-turn");
        phases.setName("report-phase");
        units.setName("report-unit");
        turns.setItems(new Turn(-1), new Turn(0));
        phases.setItems(ALL_PHASES);
        units.setItems(ALL_UNITS);
        for (SelectBox<?> box : List.of(turns, phases, units)) {
            box.setMaxListCount(12);
            box.getSelection().setProgrammaticChangeEvents(false);
            box.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (!changing) {
                        followLatest = false;
                        rebuild(false);
                    }
                }
            });
        }
        filters.defaults().height(30);
        filters.add(selector(turns)).width(112).padRight(4);
        filters.add(selector(phases)).width(160).padRight(4);
        filters.add(selector(units)).minWidth(0).growX();
        turns.addListener(new TextTooltip("Filter by turn", skin));
        phases.addListener(new TextTooltip("Filter by phase", skin));
        units.addListener(new TextTooltip("Filter by unit; includes events as attacker or target", skin));
        panel.add(filters).growX().padBottom(4).row();
        Table searchRow = new Table();
        searchRow.add(search).minWidth(0).growX().height(30).padRight(4);
        selected = button("report-selected", "Selected unit", () -> filterUnit(selectedId));
        selected.setDisabled(true);
        selected.addListener(new TextTooltip("Show events involving the unit selected on the board.", skin));
        searchRow.add(selected).height(28);
        searchRow.add(button("report-reset", "Clear", () -> {
            changing = true;
            turns.setSelected(new Turn(0));
            phases.setSelected(ALL_PHASES);
            units.setSelected(ALL_UNITS);
            search.setText("");
            changing = false;
            followLatest = false;
            collapsed.clear();
            keywordFilterEnabled = false;
            keywordMatch = -1;
            rebuild(false);
        })).height(28).padLeft(6);
        panel.add(searchRow).growX().padBottom(4).row();
        search.setName("report-search");
        search.setProgrammaticChangeEvents(true);
        search.setMessageText("Search weapons, damage, rolls, locations...");
        search.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!changing) {
                    followLatest = false;
                    rebuild(false);
                }
            }
        });
        keywords.setName("report-keyword");
        filterKeywords.setName("report-filter-keyword");
        for (SelectBox<String> box : List.of(keywords, filterKeywords)) {
            box.setMaxListCount(8);
            box.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (!changing) {
                        keywordMatch = -1;
                        rebuild(true);
                    }
                }
            });
        }
        Table keywordRow = new Table();
        keywordRow.add(selector(keywords)).minWidth(0).growX().height(28).padRight(4);
        keywordRow.add(button("report-keyword-prev", "Previous", () -> findKeyword(-1))).height(26).padRight(4);
        keywordRow.add(button("report-keyword-next", "Next", () -> findKeyword(1))).height(26);
        panel.add(keywordRow).growX().padBottom(4).row();
        Table filterRow = new Table();
        filterRow.add(selector(filterKeywords)).minWidth(0).growX().height(28).padRight(4);
        keywordFilter = button("report-keyword-filter", "Keyword filter", this::toggleKeywordFilter);
        filterRow.add(keywordFilter).height(26);
        panel.add(filterRow).growX().padBottom(4).row();
        Table tools = new Table();
        status = new Label("", skin, "small");
        status.setName("report-status");
        status.setEllipsis(true);
        tools.add(status).minWidth(0).growX().left();
        TextButton details = button("report-details", "Details", () -> {
            showDetails = !showDetails;
            detailOverrides.clear();
            detailLinks.clear();
            rebuild(true);
        });
        details.addListener(new TextTooltip("Show all linked explanations, modifiers and dice breakdowns.", skin));
        tools.add(details).height(24).padRight(6);
        tools.add(button("report-copy", "Copy", () -> Gdx.app.getClipboard().setContents(copyText()))).height(24);
        panel.add(tools).growX().padBottom(6).row();
        rows.top();
        scroll = new ScrollPane(rows, skin, "menu");
        scroll.setName("report-scroll");
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setFlickScroll(false);
        panel.add(scroll).minHeight(0).grow();
        panel.setVisible(false);
    }

    Table panel() { return panel; }

    void updateKeywords(GpuBoardSource.UiPreferences preferences) {
        if (keywordPreferences == preferences) {
            return;
        }
        keywordPreferences = preferences;
        changing = true;
        setKeywords(keywords, preferences.reportKeywords());
        setKeywords(filterKeywords, preferences.reportFilterKeywords());
        changing = false;
        rebuild(true);
    }

    private static void setKeywords(SelectBox<String> box, String preference) {
        String selected = box.getSelected();
        box.setItems(new Array<>(preference.lines().map(String::strip).filter(word -> !word.isEmpty())
              .distinct().toArray(String[]::new)));
        if (selected != null && box.getItems().contains(selected, false)) {
            box.setSelected(selected);
        }
        box.setDisabled(box.getItems().isEmpty());
    }

    /** The legacy report shortcuts act on the native reader and use the same user-configured keyword lists. */
    boolean key(List<KeyCommandBind> bindings) {
        if (!panel.isVisible()) {
            return false;
        }
        for (KeyCommandBind binding : bindings) {
            switch (binding) {
                case REPORT_KEY_NEXT -> findKeyword(1);
                case REPORT_KEY_PREV -> findKeyword(-1);
                case REPORT_KEY_SELECT_NEXT -> cycleKeyword(keywords, 1);
                case REPORT_KEY_SELECT_PREVIOUS -> cycleKeyword(keywords, -1);
                case REPORT_FILTER_KEY_SELECT_NEXT -> cycleKeyword(filterKeywords, 1);
                case REPORT_KEY_FILTER -> toggleKeywordFilter();
                default -> { continue; }
            }
            return true;
        }
        return false;
    }

    private static void cycleKeyword(SelectBox<String> box, int direction) {
        if (box.getItems().notEmpty()) {
            box.setSelectedIndex(Math.floorMod(box.getSelectedIndex() + direction, box.getItems().size));
        }
    }

    private void toggleKeywordFilter() {
        keywordFilterEnabled = !keywordFilterEnabled && filterKeywords.getSelected() != null;
        followLatest = false;
        keywordMatch = -1;
        rebuild(false);
    }

    private void findKeyword(int direction) {
        String keyword = keywords.getSelected();
        if (keyword == null) {
            return;
        }
        List<Integer> matches = java.util.stream.IntStream.range(0, filtered.size())
              .filter(entry -> filtered.get(entry).matches(0, "", -1, keyword)).boxed().toList();
        int index = matches.indexOf(keywordMatch);
        keywordMatch = matches.isEmpty() ? -1 : matches.get(index < 0 ? (direction > 0 ? 0 : matches.size() - 1)
              : Math.floorMod(index + direction, matches.size()));
        if (keywordMatch >= 0) {
            GpuReportLog.Entry entry = filtered.get(keywordMatch);
            collapsed.remove("Turn " + entry.round() + "  /  " + entry.phase());
        }
        rebuild(true);
        Table row = eventRows.get(keywordMatch);
        if (row != null) {
            scroll.scrollTo(row.getX(), row.getY(), row.getWidth(), row.getHeight(), false, true);
            scroll.updateVisualScroll();
        }
        status.setText(filtered.size() + " events  /  " + (matches.isEmpty() ? "No matches"
              : "Match " + (matches.indexOf(keywordMatch) + 1) + " of " + matches.size()) + " · " + keyword);
    }

    void layout() {
        boolean narrow = panel.getWidth() < 520;
        if (narrow != narrowFilters) {
            Actor turnField = turns.getParent();
            Actor phaseField = phases.getParent();
            Actor unitField = units.getParent();
            filters.clearChildren();
            filters.add(turnField).width(112).padRight(4);
            if (narrow) {
                filters.add(phaseField).minWidth(0).growX().row();
                filters.add(unitField).colspan(2).minWidth(0).growX().padTop(4);
            } else {
                filters.add(phaseField).width(160).padRight(4);
                filters.add(unitField).minWidth(0).growX();
            }
            narrowFilters = narrow;
        }
        turns.hideList();
        phases.hideList();
        units.hideList();
        keywords.hideList();
        filterKeywords.hideList();
        panel.validate();
    }

    void update(GpuReportLog.Snapshot next, int actorId) {
        boolean changedActor = selectedId != actorId;
        selectedId = actorId;
        if (next == snapshot) {
            if (changedActor) {
                selected.setDisabled(snapshot.entries().stream().flatMap(entry -> entry.units().stream())
                      .noneMatch(unit -> unit.id() == actorId));
            }
            return;
        }
        String oldPhase = phases.getSelected();
        snapshot = next;
        icons.update(next.icons());
        detailOverrides.keySet().retainAll(next.entries());
        detailLinks.keySet().retainAll(next.entries());
        changing = true;
        Turn oldTurn = turns.getSelected();
        GpuReportLog.Unit oldUnit = units.getSelected();
        List<Turn> turnChoices = new ArrayList<>(List.of(new Turn(-1), new Turn(0)));
        next.entries().stream().map(GpuReportLog.Entry::round).distinct().sorted(Comparator.reverseOrder())
              .forEach(round -> turnChoices.add(new Turn(round)));
        turns.setItems(new Array<>(turnChoices.toArray(Turn[]::new)));
        turns.setSelected(oldTurn);
        List<String> phaseChoices = new ArrayList<>(List.of(ALL_PHASES));
        next.entries().stream().map(GpuReportLog.Entry::phase).distinct().forEach(phaseChoices::add);
        phases.setItems(new Array<>(phaseChoices.toArray(String[]::new)));
        phases.setSelected(oldPhase);
        Map<Integer, GpuReportLog.Unit> known = new LinkedHashMap<>();
        next.entries().forEach(entry -> entry.units().forEach(unit -> known.put(unit.id(), unit)));
        List<GpuReportLog.Unit> unitChoices = new ArrayList<>(List.of(ALL_UNITS));
        known.values().stream().sorted(Comparator.comparing(GpuReportLog.Unit::name, String.CASE_INSENSITIVE_ORDER)
              .thenComparingInt(GpuReportLog.Unit::id)).forEach(unitChoices::add);
        units.setItems(new Array<>(unitChoices.toArray(GpuReportLog.Unit[]::new)));
        units.setSelected(known.getOrDefault(oldUnit.id(), ALL_UNITS));
        selected.setDisabled(!known.containsKey(selectedId));
        if (followLatest) {
            selectLatest();
        }
        changing = false;
        current.setText("Turn " + next.round() + "  /  " + next.phase().localizedName());
        rebuild(oldPhase.equals(phases.getSelected()));
    }

    private void latest() {
        changing = true;
        followLatest = true;
        selectLatest();
        units.setSelected(ALL_UNITS);
        search.setText("");
        keywordFilterEnabled = false;
        keywordMatch = -1;
        collapsed.clear();
        changing = false;
        rebuild(false);
    }

    private void selectLatest() {
        turns.setSelected(new Turn(-1));
        phases.setSelected(snapshot.entries().isEmpty() ? ALL_PHASES : snapshot.entries().getLast().phase());
    }

    private void filterUnit(int id) {
        for (GpuReportLog.Unit unit : units.getItems()) {
            if (unit.id() == id) {
                units.setSelected(unit);
                followLatest = false;
                rebuild(false);
                return;
            }
        }
    }

    private void rebuild(boolean preserveScroll) {
        float position = preserveScroll ? scroll.getScrollY() : 0;
        int round = turns.getSelected().number();
        if (round < 0) {
            round = snapshot.entries().isEmpty() ? Math.max(1, snapshot.round())
                  : snapshot.entries().getLast().round();
        }
        int chosenRound = round;
        String phase = phases.getSelected().equals(ALL_PHASES) ? "" : phases.getSelected();
        List<GpuReportLog.Entry> previous = filtered;
        filtered = snapshot.entries().stream()
              .filter(entry -> entry.matches(chosenRound, phase, units.getSelected().id(), search.getText()))
              .filter(entry -> !keywordFilterEnabled || filterKeywords.getSelected() == null
                    || java.util.Arrays.stream(filterKeywords.getSelected().split("\\s+"))
                          .anyMatch(word -> entry.matches(0, "", -1, word))).toList();
        if (!filtered.equals(previous)) {
            keywordMatch = -1;
        }
        status.setText(filtered.size() + " events  /  " + filtered.stream().flatMap(entry -> entry.units().stream())
              .map(GpuReportLog.Unit::id).distinct().count() + " units  ·  Click a unit to filter");
        latest.setChecked(followLatest);
        keywordFilter.setChecked(keywordFilterEnabled);
        ((TextButton) panel.findActor("report-details")).setChecked(showDetails);
        rows.clearChildren();
        eventRows.clear();
        Map<String, List<Integer>> sections = new LinkedHashMap<>();
        for (int index = 0; index < filtered.size(); index++) {
            GpuReportLog.Entry entry = filtered.get(index);
            sections.computeIfAbsent("Turn " + entry.round() + "  /  " + entry.phase(), key -> new ArrayList<>()).add(index);
        }
        int index = 0;
        for (var section : sections.entrySet()) {
            String key = section.getKey();
            TextButton header = button("report-section:" + key,
                  (collapsed.contains(key) ? "+  " : "−  ") + key + "  ·  " + section.getValue().size() + " events", () -> {
                      if (!collapsed.remove(key)) {
                          collapsed.add(key);
                      }
                      rebuild(true);
                  });
            header.getLabel().setAlignment(Align.left);
            header.getLabel().setEllipsis(true);
            header.getLabelCell().minWidth(0);
            header.getLabel().setColor(AMBER);
            rows.add(header).minWidth(0).growX().height(28).padTop(index == 0 ? 0 : 8).row();
            if (collapsed.contains(key)) {
                continue;
            }
            for (int entryIndex : section.getValue()) {
                GpuReportLog.Entry entry = filtered.get(entryIndex);
                Table event = new Table();
                event.setName("report-event:" + index);
                event.setBackground(skin.newDrawable("white", entryIndex == keywordMatch ? Color.valueOf("3B3A2D")
                      : Color.valueOf(index % 2 == 0 ? "171F23" : "1C262A")));
                index++;
                eventRows.put(entryIndex, event);
                event.pad(5, 8, 6, 8).defaults().growX().left();
                if (!entry.heading().isBlank() && entry.units().isEmpty()) {
                    Label heading = new Label(entry.heading(), skin, "kicker");
                    heading.setWrap(true);
                    event.add(heading).minWidth(0).padBottom(3).row();
                }
                HorizontalGroup links = new HorizontalGroup().wrap().left().rowLeft().space(6).wrapSpace(2);
                for (GpuReportLog.Unit unit : entry.units()) {
                    Table chip = new Table();
                    TextButton readout = button("report-readout:" + unit.id(), "i", () -> inspectUnit.accept(unit.id()));
                    if (snapshot.icons().containsKey(unit.id())) {
                        Image icon = new Image(new TextureRegionDrawable(icons.region(unit.id())));
                        icon.setScaling(Scaling.fit);
                        readout.clearChildren();
                        readout.add(icon).size(32, 28);
                        readout.pad(0, 2, 0, 2);
                    }
                    readout.addListener(new TextTooltip("Open unit readout: " + unit.name(), skin));
                    chip.add(readout).height(28).padRight(2);
                    TextButton link = button("report-unit:" + unit.id(), unit.toString(), () -> filterUnit(unit.id()));
                    link.getLabel().setColor(AMBER);
                    link.getLabel().setEllipsis(true);
                    link.getCell(link.getLabel()).width(Math.min(234, link.getLabel().getPrefWidth()));
                    link.addListener(new TextTooltip("Filter reports involving " + unit.name(), skin));
                    chip.add(link);
                    links.addActor(chip);
                }
                boolean detailed = detailOverrides.getOrDefault(entry, showDetails);
                if (links.getChildren().notEmpty()) {
                    event.add(links).minWidth(0).padBottom(2).row();
                }
                Label body = new GpuReportText(entry, skin, link -> {
                    if (link.unitId() >= 0) {
                        inspectUnit.accept(link.unitId());
                    } else {
                        detailOverrides.put(entry, !link.equals(detailLinks.get(entry)) || !detailed);
                        detailLinks.put(entry, link);
                        rebuild(true);
                    }
                });
                body.setName("report-text:" + (index - 1));
                body.setWrap(true);
                event.add(body).minWidth(0).row();
                if (detailed && !entry.rolls().isBlank()) {
                    GpuReportLog.Link link = detailLinks.get(entry);
                    Label rolls = new Label(link == null ? entry.rolls()
                          : entry.text().substring(link.start(), link.end()) + ": " + link.detail(), skin, "small");
                    rolls.setName("report-rolls:" + (index - 1));
                    rolls.setWrap(true);
                    Table explanation = new Table();
                    explanation.add(rolls).minWidth(0).growX().left();
                    explanation.add(button("report-roll-toggle:" + (index - 1), "Hide", () -> {
                        detailOverrides.put(entry, false);
                        detailLinks.remove(entry);
                        rebuild(true);
                    })).top().padLeft(4);
                    event.add(explanation).minWidth(0).padTop(3).row();
                }
                rows.add(event).minWidth(0).growX().row();
            }
        }
        if (filtered.isEmpty()) {
            Label empty = new Label(snapshot.entries().isEmpty()
                  ? "No battle reports yet.\nResolved events will appear here as play progresses."
                  : "No events match these filters.\nClear the filters or choose another turn, phase or unit.", skin, "small");
            empty.setWrap(true);
            rows.add(empty).growX().minWidth(0).pad(16).row();
        }
        panel.validate();
        scroll.setScrollY(position);
        scroll.updateVisualScroll();
    }

    private String copyText() {
        StringBuilder text = new StringBuilder();
        for (GpuReportLog.Entry entry : filtered) {
            text.append("Turn ").append(entry.round()).append(" / ").append(entry.phase()).append('\n');
            if (!entry.heading().isBlank()) {
                text.append(entry.heading()).append('\n');
            }
            text.append(entry.text()).append("\n\n");
            if (!entry.rolls().isBlank()) {
                text.append(entry.rolls()).append("\n\n");
            }
        }
        return text.toString();
    }

    private TextButton button(String name, String text, Runnable action) {
        TextButton button = new TextButton(text, skin, "menu-control");
        button.setName(name);
        button.pad(3, 6, 3, 6);
        button.setProgrammaticChangeEvents(false);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                button.setChecked(false);
                action.run();
            }
        });
        return button;
    }

    private Stack selector(SelectBox<?> box) {
        Table arrow = new Table();
        arrow.setTouchable(Touchable.disabled);
        arrow.right().padRight(8);
        Image chevron = new Image(skin.getDrawable("icon-arrow"));
        chevron.setSize(10, 10);
        chevron.setOrigin(Align.center);
        chevron.setRotation(-90);
        chevron.setColor(GpuBoardSkin.MUTED);
        arrow.add(chevron).size(10);
        return new Stack(box, arrow);
    }

    @Override
    public void dispose() { icons.dispose(); }
}
