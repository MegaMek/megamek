/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;

/** GL-owned target passes on the playback clock. Grouping changes presentation, never resolution order. */
final class UnitVolley {
    static final float TARGET_SWITCH_SECONDS = .3f;

    static final class Group {
        final List<UnitAttack> shots = new ArrayList<>();
        final Group previous;
        float start = Float.POSITIVE_INFINITY;
        float clock;

        Group(Group previous) { this.previous = previous; }

        UnitAttack first() { return shots.getFirst(); }

        float turnSeconds() { return previous == null ? UnitAttack.ANTICIPATION_SECONDS : TARGET_SWITCH_SECONDS; }

        float fireEnd() {
            float end = 0;
            for (var shot : shots) { end = Math.max(end, shot.delay + shot.firingEndSeconds()); }
            return end;
        }

        float recovery(float time) { return 1 - UnitAttack.smooth((time - fireEnd()) / UnitAttack.RECOVERY_SECONDS); }

        float turn() { return UnitAttack.smooth((clock - start) / turnSeconds()); }
    }

    private final List<Group> groups = new ArrayList<>();

    void add(UnitAttack shot, float clock) {
        Group previous = null, matching = null;
        for (var group : groups) {
            if (group.first().event.entityId() != shot.event.entityId()) { continue; }
            previous = group;
            // Never reopen an old target after its successor has begun turning/firing.
            if (group.start <= clock) { matching = null; }
            if (sameTarget(group.first(), shot) && clock <= group.fireEnd()) { matching = group; }
        }
        if (matching == null) {
            matching = new Group(previous);
            groups.add(matching);
        }
        shot.group = matching;
        shot.receivedAt = clock < UnitAttack.ANTICIPATION_SECONDS ? 0 : clock;
        shot.jitter = matching.shots.isEmpty() ? 0
              : (shot.event.result().id().hashCode() & 0xFFFF) / 65535f * UnitPlayback.VOLLEY_JITTER_SECONDS;
        matching.shots.add(shot);
        schedule(clock);
    }

    private void schedule(float clock) {
        for (var group : groups) {
            if (clock <= group.start) {
                group.start = Math.max(group.first().receivedAt, group.previous == null ? 0 : group.previous.fireEnd());
            }
            for (var shot : group.shots) {
                // Captured launch times are immutable once anticipation has begun.
                if (shot.seconds < 0 || clock == 0 || shot.receivedAt == clock) {
                    shot.delay = Math.max(shot.receivedAt,
                          group.start + group.turnSeconds() - UnitAttack.ANTICIPATION_SECONDS) + shot.jitter;
                    shot.seconds = clock - shot.delay;
                }
            }
            group.clock = clock;
        }
        // Point defense accompanies the incoming target pass, including when its packet arrived first.
        for (var group : groups) {
            if (!group.first().defensive() || clock > group.start) { continue; }
            for (var incoming : groups) {
                if (incoming.first().defensive() || incoming.first().event.entityId() != group.first().event.result().target().entityId()
                      || incoming.first().event.result().target().entityId() != group.first().event.entityId()) { continue; }
                float shift = Math.max(0, incoming.start - group.start);
                group.start += shift;
                for (var shot : group.shots) { shot.delay += shift; shot.seconds = clock - shot.delay; }
                break;
            }
        }
    }

    private static boolean sameTarget(UnitAttack first, UnitAttack next) {
        return first.event.result().targetType() == next.event.result().targetType()
              && first.event.result().target().entityId() == next.event.result().target().entityId()
              && first.event.destination().coords().equals(next.event.destination().coords());
    }

    void advance(float clock) { groups.forEach(group -> group.clock = clock); }

    void clear() { groups.clear(); }
}
