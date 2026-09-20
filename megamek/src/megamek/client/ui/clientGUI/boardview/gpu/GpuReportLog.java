/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.Report;
import megamek.common.enums.GamePhase;

/** EDT-owned formatting cache of the client's already visibility-filtered reports. No live entities cross to GL. */
final class GpuReportLog {
    private static final Pattern UNIT_LINK = Pattern.compile(
          "(?is)<a\\b[^>]*href\\s*=\\s*['\"]#entity:(\\d+)['\"][^>]*>(.*?)</a>");
    private static final Pattern LINK = Pattern.compile(
          "(?is)<a\\b[^>]*href\\s*=\\s*(['\"])(#entity:\\d+|#tooltip:.*?)\\1\\s*>(.*?)</a>");
    private static final Set<Integer> ACTOR_HEADERS = Set.of(3100, 3101, 3102, 4005, 5630);
    private static final Set<Integer> ATTACK_STARTS = Set.of(3115, 3116, 3117, 3119, 3120, 3124, 3126,
          4010, 4055, 4070, 4085, 4110, 4145, 4146, 4155, 4210, 4212, 4246, 4280, 4290, 4295, 4305,
          4550, 4560, 4580);

    record Unit(int id, String name) {
        @Override
        public String toString() { return id < 0 ? name : name + "  [" + id + "]"; }
    }

    record Link(int start, int end, int unitId, String detail) { }
    record Text(String text, List<Link> links) { }

    record Entry(int round, String phase, String heading, String text, List<Unit> units, String rolls, List<Link> links) {
        boolean matches(int turn, String phaseFilter, int unitId, String query) {
            return (turn == 0 || round == turn) && (phaseFilter.isEmpty() || phase.equals(phaseFilter))
                  && (unitId < 0 || units.stream().anyMatch(unit -> unit.id() == unitId))
                  && (query.isBlank() || (heading + " " + text + " " + rolls).toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT).strip()));
        }
    }

    record Snapshot(int round, GamePhase phase, List<Entry> entries, Map<Integer, BoardScene.Pixels> icons) {
        Snapshot(int round, GamePhase phase, List<Entry> entries) { this(round, phase, entries, Map.of()); }
        static final Snapshot EMPTY = new Snapshot(0, GamePhase.UNKNOWN, List.of());
    }

    private record Round(List<Report> source, int size, List<Entry> entries) { }
    private List<Round> rounds = List.of();
    private Snapshot snapshot = Snapshot.EMPTY;
    private String liveHtml = "";
    private int liveRound;
    private GamePhase livePhase = GamePhase.UNKNOWN;
    private boolean liveChanged;

    /** Special reports replace the accumulated, provisional phase text; they are not appended as new events. */
    void live(String html, int round, GamePhase phase) {
        if (!phase.isReport() && !html.equals(liveHtml)) {
            liveHtml = html;
            liveRound = Math.max(1, round);
            livePhase = phase;
            liveChanged = true;
        }
    }

    Snapshot capture(List<List<Report>> history, int round, GamePhase phase) {
        return capture(history, round, phase, id -> null);
    }

    Snapshot capture(List<List<Report>> history, int round, GamePhase phase, IntFunction<BoardScene.Pixels> icon) {
        boolean changed = history.size() != rounds.size();
        List<Round> next = new ArrayList<>();
        for (int index = 0; index < history.size(); index++) {
            List<Report> reports = history.get(index);
            Round old = index < rounds.size() ? rounds.get(index) : null;
            if (old != null && old.source() == reports && old.size() == reports.size()) {
                next.add(old);
            } else {
                changed = true;
                next.add(new Round(reports, reports.size(), format(index + 1, reports)));
            }
        }
        if (!liveHtml.isEmpty() && (livePhase != phase || liveRound != Math.max(1, round) || changed)) {
            liveHtml = "";
            liveChanged = true;
        }
        if (changed || liveChanged || snapshot.round() != round || snapshot.phase() != phase) {
            rounds = List.copyOf(next);
            List<Entry> entries = new ArrayList<>();
            rounds.forEach(saved -> entries.addAll(saved.entries()));
            if (!liveHtml.isBlank()) {
                Text text = linkedText(liveHtml);
                entries.add(new Entry(liveRound, phase.localizedName(), "Live resolution", text.text(),
                      List.copyOf(units(liveHtml).values()), rolls(text), text.links()));
            }
            Map<Integer, BoardScene.Pixels> icons = new LinkedHashMap<>();
            entries.stream().flatMap(entry -> entry.units().stream()).map(Unit::id).distinct().forEach(id -> {
                // A historical thumbnail remains stable when the unit disappears or is destroyed later.
                BoardScene.Pixels image = snapshot.icons().get(id);
                if (image == null) {
                    image = icon.apply(id);
                }
                if (image != null) {
                    icons.put(id, image);
                }
            });
            snapshot = new Snapshot(round, phase, List.copyOf(entries), Map.copyOf(icons));
            liveChanged = false;
        }
        return snapshot;
    }

    static List<Entry> format(int round, List<Report> reports) {
        // The subject field is transient on the wire. Use only identities actually disclosed in the report text.
        List<String> rendered = reports.stream().map(report -> new Report(report).text()).toList();
        Map<Integer, Unit> known = new LinkedHashMap<>();
        rendered.forEach(html -> known.putAll(units(html)));
        List<Unit> aliases = known.values().stream().filter(unit -> known.values().stream()
                    .filter(other -> other.name().equals(unit.name())).count() == 1)
              .sorted(Comparator.comparingInt((Unit unit) -> unit.name().length()).reversed()).toList();
        List<Entry> entries = new ArrayList<>();
        String phase = "General";
        String heading = "";
        Map<Integer, Unit> actor = Map.of();
        Map<Integer, Unit> involved = new LinkedHashMap<>();
        StringBuilder body = new StringBuilder();
        boolean lineEnded = true;
        for (int index = 0; index < reports.size(); index++) {
            Report report = reports.get(index);
            String html = rendered.get(index);
            String nextPhase = phase(report.messageId, html);
            Map<Integer, Unit> named = units(html);
            boolean header = ACTOR_HEADERS.contains(report.messageId) || report.messageId == 3900
                  || report.messageId == 3901;
            boolean newSubject = actor.isEmpty() && lineEnded && !named.isEmpty()
                  && !involved.keySet().containsAll(named.keySet());
            if (nextPhase != null || header || ATTACK_STARTS.contains(report.messageId) || newSubject) {
                add(entries, round, phase, heading, body, involved, aliases);
                involved.clear();
            }
            if (nextPhase != null) {
                phase = nextPhase;
                heading = "";
                actor = Map.of();
                continue;
            }
            if (header) {
                heading = compact(html);
                actor = named;
                involved.putAll(actor);
                continue;
            }
            involved.putAll(actor);
            involved.putAll(named);
            body.append(html).append(' ');
            lineEnded = report.newlines > 0;
            if (report.newlines > 1 || report.messageId == 1210) {
                add(entries, round, phase, heading, body, involved, aliases);
                involved.clear();
            }
        }
        add(entries, round, phase, heading, body, involved, aliases);
        return List.copyOf(entries);
    }

    private static void add(List<Entry> entries, int round, String phase, String heading, StringBuilder body,
          Map<Integer, Unit> involved, List<Unit> aliases) {
        Text linked = linkedText(body.toString());
        String text = linked.text();
        String rolls = rolls(linked);
        body.setLength(0);
        if (text.isBlank()) {
            return;
        }
        // Some older physical-attack messages contain plain names. Match only unambiguous names already disclosed
        // in this round, never an entity lookup that could reveal a concealed or sensor-only contact.
        String remaining = text;
        for (Unit unit : aliases) {
            Matcher named = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(unit.name()) + "(?![\\p{L}\\p{N}])")
                  .matcher(remaining);
            if (named.find()) {
                involved.putIfAbsent(unit.id(), unit);
                remaining = named.replaceAll("");
            }
        }
        entries.add(new Entry(round, phase, heading, text, List.copyOf(involved.values()), rolls, linked.links()));
    }

    /** Keep link ranges through HTML cleanup and whitespace compaction, including links with HTML in their tooltip. */
    static Text linkedText(String html) {
        Matcher anchors = LINK.matcher(html);
        List<String> targets = new ArrayList<>();
        String marked = anchors.replaceAll(match -> {
            targets.add(match.group(2));
            return Matcher.quoteReplacement("\uE000" + match.group(3) + "\uE001");
        });
        String plain = compact(marked);
        StringBuilder text = new StringBuilder();
        List<Link> links = new ArrayList<>();
        int start = 0;
        int index = 0;
        for (char character : plain.toCharArray()) {
            if (character == '\uE000') {
                start = text.length();
            } else if (character == '\uE001') {
                String target = targets.get(index++);
                int id = target.startsWith(Report.ENTITY_LINK)
                      ? Integer.parseInt(target.substring(Report.ENTITY_LINK.length())) : -1;
                String detail = id < 0 ? compact(target.substring(Report.TOOLTIP_LINK.length())) : "";
                if (!text.substring(start).contains(Report.OBSCURED_STRING)) {
                    links.add(new Link(start, text.length(), id, detail));
                }
            } else {
                text.append(character);
            }
        }
        return new Text(text.toString(), List.copyOf(links));
    }

    private static String phase(int id, String html) {
        return switch (id) {
            case 1000, 1005, 1010 -> GamePhase.INITIATIVE.localizedName();
            case 1035 -> GamePhase.TARGETING.localizedName();
            case 1100 -> GamePhase.OFFBOARD.localizedName();
            case 2000 -> GamePhase.MOVEMENT.localizedName();
            case 3000 -> GamePhase.FIRING.localizedName();
            case 4000 -> GamePhase.PHYSICAL.localizedName();
            case 5000 -> compact(html);
            case 5005 -> GamePhase.END.localizedName();
            case 7000 -> GamePhase.VICTORY.localizedName();
            default -> null;
        };
    }

    private static Map<Integer, Unit> units(String html) {
        Map<Integer, Unit> result = new LinkedHashMap<>();
        Matcher matcher = UNIT_LINK.matcher(html);
        while (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            String name = compact(matcher.group(2));
            if (!name.isBlank() && !name.contains(Report.OBSCURED_STRING)) {
                result.putIfAbsent(id, new Unit(id, name));
            }
        }
        return result;
    }

    private static String compact(String html) {
        return GpuBoardActions.plainText(LINK.matcher(html).replaceAll("$3"))
              .replace("&quot;", "\"").replace("&#39;", "'")
              .replace("&apos;", "'").replaceAll("(?m)^\\s*-{3,}\\s*$", "")
              .replaceAll("[\\t\\x0B\\f\\r ]+", " ").replaceAll(" *\\n\\s*", "\n").strip();
    }

    private static String rolls(Text text) {
        Set<String> details = new LinkedHashSet<>();
        for (Link link : text.links()) {
            if (!link.detail().isBlank()) {
                details.add(text.text().substring(link.start(), link.end()) + ": " + link.detail());
            }
        }
        return String.join("\n", details);
    }
}
