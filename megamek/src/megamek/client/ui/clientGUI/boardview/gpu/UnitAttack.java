/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.math.MathUtils;
import megamek.common.ResolvedAttack;

/** A resolved attack's presentation clock. Outcomes and damage are never calculated here. */
final class UnitAttack {
    static final float ANTICIPATION_SECONDS = .2f;
    static final float RECOVERY_SECONDS = .35f;
    final BoardScene.Combat event;
    final float contactSeconds;
    final float duration;
    private final Map<Integer, Set<Integer>> firingMounts = new HashMap<>();
    float seconds;

    UnitAttack(BoardScene.Combat event) {
        this.event = event;
        event.result().mounts().forEach(mount -> firingMounts
              .computeIfAbsent(mount.entityId(), ignored -> new HashSet<>()).add(mount.equipmentIndex()));
        float distance = BoardGeometry.center(event.attacker().location().coords(), event.attacker().location().elevation())
              .dst(BoardGeometry.center(event.destination().coords(), event.destination().elevation())) / BoardGeometry.HEIGHT;
        contactSeconds = shot() ? ANTICIPATION_SECONDS + MathUtils.clamp(distance * .05f, .18f, .7f) : .65f;
        duration = contactSeconds + RECOVERY_SECONDS;
    }

    boolean shot() {
        return event.result().kind() == ResolvedAttack.Kind.SHOT;
    }

    boolean fires(UnitEquipmentAssembly.Binding binding) {
        int owner = binding.memberId() < 0 ? event.attacker().id() : binding.memberId();
        return firingMounts.getOrDefault(owner, Set.of()).contains(binding.index());
    }

    float flight() {
        return MathUtils.clamp((seconds - ANTICIPATION_SECONDS) / (contactSeconds - ANTICIPATION_SECONDS), 0, 1);
    }

    float impact() {
        return event.result().hit() && seconds >= contactSeconds
              ? Math.max(0, 1 - (seconds - contactSeconds) / RECOVERY_SECONDS) : 0;
    }

    float recoil() {
        float t = (seconds - ANTICIPATION_SECONDS) / .24f;
        return t <= 0 || t >= 1 ? 0 : MathUtils.sin(t * MathUtils.PI);
    }

    /** Smooth anticipation -> contact -> recovery for every physical clip. */
    float strike() {
        if (seconds < ANTICIPATION_SECONDS) {
            return -.2f * smooth(seconds / ANTICIPATION_SECONDS);
        }
        if (seconds < contactSeconds) {
            return MathUtils.lerp(-.2f, 1, smooth(flight()));
        }
        return 1 - smooth((seconds - contactSeconds) / RECOVERY_SECONDS);
    }

    private static float smooth(float value) {
        float t = MathUtils.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
