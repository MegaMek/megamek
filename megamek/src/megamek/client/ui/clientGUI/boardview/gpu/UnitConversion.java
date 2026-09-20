/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.math.MathUtils;
import megamek.common.units.UnitLocation;

/** A bounded fold -> form switch -> deploy interval on UnitPlayback's clock. No game conversion rules live here. */
final class UnitConversion {
    static final float DURATION_SECONDS = .8f;
    final BoardScene.Conversion event;
    float seconds;

    UnitConversion(BoardScene.Conversion event) { this.event = event; }

    static UnitLocation.Form form(BoardScene.Unit unit) {
        return unit == null || unit.sensorContact() || unit.model() == null || unit.model().state() == null
              ? null : unit.model().state().pose().form();
    }

    static boolean changes(BoardScene.Unit before, BoardScene.Unit after) {
        var from = form(before);
        var to = form(after);
        return from != null && to != null && from.mode() != to.mode();
    }

    BoardScene.Unit displayed() {
        return seconds < DURATION_SECONDS * .5f ? event.before() : event.after();
    }

    float fold() {
        float t = 1 - Math.abs(MathUtils.clamp(seconds / DURATION_SECONDS, 0, 1) * 2 - 1);
        return t * t * (3 - 2 * t);
    }

    static boolean quadVee(BoardScene.Unit unit) {
        return form(unit) != null && unit.model().state().structure().anatomy() != null
              && "quad".equals(unit.model().state().structure().anatomy().configuration());
    }

    float progress() {
        float t = MathUtils.clamp(seconds / DURATION_SECONDS, 0, 1);
        return t * t * (3 - 2 * t);
    }

    static float vehiclePose(BoardScene.Unit unit) {
        return quadVee(unit) && form(unit).mode() == megamek.common.units.QuadVee.CONV_MODE_VEHICLE ? 1 : 0;
    }
}
