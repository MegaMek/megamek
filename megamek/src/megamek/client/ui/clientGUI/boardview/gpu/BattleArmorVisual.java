/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.JsonValue;

/** One visible figure per living suit by default. Conventional infantry keeps its own count policy. */
final class BattleArmorVisual {
    static final boolean COMPRESS_TROOPS = false;

    private BattleArmorVisual() { }

    static int figures(int alive) {
        return figures(alive, COMPRESS_TROOPS);
    }

    static int figures(int alive, boolean compress) {
        return compress ? UnitModelSelection.figures(alive, 4) : Math.max(0, alive);
    }

    static List<InfantryVisual.Part> parts(UnitModelState.Structure state, JsonValue components) {
        List<InfantryVisual.Part> result = new ArrayList<>();
        int count = figures(state.activeTroopers());
        for (int index = 0; index < count; index++) {
            int member = state.members().get(index);
            result.add(InfantryVisual.figure(member, member - 1, components.get("poses")));
        }
        return List.copyOf(result);
    }
}
