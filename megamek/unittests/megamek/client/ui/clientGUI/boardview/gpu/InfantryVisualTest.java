/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import megamek.common.Configuration;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class InfantryVisualTest {
    private static JsonValue descriptor(String family) {
        return new JsonReader().parse(new FileHandle(Configuration.dataDir().toPath()
              .resolve("models/units/modular/" + family + ".json").toFile()));
    }

    private static UnitModelState.Structure state(EntityMovementMode movement, int alive, List<Integer> members) {
        return new UnitModelState.Structure(movement, List.of(), members, alive, false);
    }

    @ParameterizedTest
    @EnumSource(value = EntityMovementMode.class, names = { "INF_MOTORIZED", "TRACKED", "WHEELED", "HOVER" })
    void transportsTakeOneSlotUpToFourAndTwoAboveThat(EntityMovementMode movement) {
        for (int slots = 0; slots <= 6; slots++) {
            var parts = InfantryVisual.parts(state(movement, slots * slots, List.of()), descriptor("infantry"));
            assertEquals(slots, parts.size());
            assertEquals(slots == 0 ? 0 : slots <= 4 ? 1 : 2,
                  parts.stream().filter(part -> part.id().startsWith("vehicle")).count());
            assertEquals(slots - (slots == 0 ? 0 : slots <= 4 ? 1 : 2),
                  parts.stream().filter(part -> part.id().startsWith("trooper")).count());
        }
        var six = InfantryVisual.parts(state(movement, 28, List.of()), descriptor("infantry"));
        var four = InfantryVisual.parts(state(movement, 16, List.of()), descriptor("infantry"));
        for (var part : four) {
            assertEquals(part, six.stream().filter(before -> before.id().equals(part.id())).findFirst().orElseThrow());
        }
        assertEquals(2, six.stream().filter(part -> part.id().startsWith("vehicle"))
              .map(InfantryVisual.Part::heading).distinct().count());
    }

    @Test
    void battleArmorKeepsEachLivingMembersIdentityAndHasOptionalCompression() {
        assertFalse(BattleArmorVisual.COMPRESS_TROOPS);
        var before = BattleArmorVisual.parts(state(EntityMovementMode.INF_JUMP, 6, List.of(1, 2, 3, 4, 5, 6)),
              descriptor("battle-armor"));
        var after = BattleArmorVisual.parts(state(EntityMovementMode.INF_JUMP, 1, List.of(6)), descriptor("battle-armor"));
        assertEquals(6, before.size());
        assertEquals(List.of(before.get(5)), after);
        assertEquals(0, BattleArmorVisual.parts(state(EntityMovementMode.INF_JUMP, 0, List.of()),
              descriptor("battle-armor")).size());
        assertEquals(5, BattleArmorVisual.figures(5, false));
        assertEquals(3, BattleArmorVisual.figures(5, true));
    }

    @Test
    void jumpInfantryUsesJumpPackMeshesAndFootInfantryStaysCompressed() {
        var jumps = InfantryVisual.parts(state(EntityMovementMode.INF_JUMP, 28, List.of()), descriptor("infantry"));
        assertEquals(6, jumps.size());
        assertEquals(6, jumps.stream().filter(part -> part.asset().contains("/jump-")).count());
        var foot = InfantryVisual.parts(state(EntityMovementMode.INF_LEG, 9, List.of()), descriptor("infantry"));
        assertEquals(3, foot.size());
        assertEquals(3, foot.stream().filter(part -> part.asset().contains("/rifle-")).count());
    }
}
