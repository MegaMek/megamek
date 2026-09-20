/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.JsonValue;
import megamek.common.units.EntityMovementMode;

/** Runtime composition of conventional infantry. Each part remains a separate rigid subtree for animation. */
final class InfantryVisual {
    private static final float[][] FOOT_SLOTS = { { -12, 9 }, { 12, 9 }, { 0, -10 }, { -17, -12 }, { 17, -12 }, { 0, 17 } };
    // Fixed slots preserve survivor placements when a casualty also reduces the number of transports.
    private static final float[][] VEHICLE_SLOTS = { { -24, 10, .30f }, { 24, -10, -.27f } };
    private static final float[][] PASSENGER_SLOTS = { { 6, 32, .5f }, { -4, -33, -.45f }, { -42, -24, .5f }, { 42, 26, -.5f } };

    private InfantryVisual() { }

    record Part(String id, String asset, float x, float y, float heading, float scale) { }

    static List<Part> parts(UnitModelState.Structure state, JsonValue components) {
        return parts(state, components, UnitModelSelection.figures(state.activeTroopers(), 6));
    }

    static List<Part> parts(UnitModelState.Structure state, JsonValue components, int count) {
        JsonValue vehicles = components.get("vehicles");
        String vehicle = vehicles == null ? null : vehicles.getString(state.movement().name(), null);
        String trooper = state.movement() == EntityMovementMode.INF_JUMP
              ? components.getString("jumpTrooper", components.getString("trooper")) : components.getString("trooper");
        if (vehicle == null) {
            List<Part> result = new ArrayList<>();
            for (int slot = 0; slot < count; slot++) {
                result.add(figure(slot, slot, trooper));
            }
            return List.copyOf(result);
        }
        int transports = count == 0 ? 0 : count <= 4 ? 1 : 2;
        List<Part> result = new ArrayList<>();
        for (int slot = 0; slot < transports; slot++) {
            float[] place = VEHICLE_SLOTS[slot];
            result.add(new Part("vehicle-" + slot, vehicle, place[0], place[1], place[2], 2));
        }
        for (int slot = 0; slot < count - transports; slot++) {
            float[] place = PASSENGER_SLOTS[slot];
            result.add(new Part("trooper-" + slot, trooper,
                  place[0], place[1], place[2], 1));
        }
        return List.copyOf(result);
    }

    static Part figure(int member, int slot, String trooper) {
        float[] position = FOOT_SLOTS[Math.floorMod(slot, FOOT_SLOTS.length)];
        return new Part("trooper-" + member, trooper,
              position[0], position[1], ((slot % 3) - 1) * .12f, 1);
    }
}
