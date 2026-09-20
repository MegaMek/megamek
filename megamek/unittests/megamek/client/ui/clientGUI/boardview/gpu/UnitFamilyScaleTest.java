/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxNativesLoader;
import megamek.common.board.Coords;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UnitFamilyScaleTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void familyScaleStacksWithBoardTuningWithoutMovingTheUnitOrChangingOtherFamilies(boolean multiHex) {
        GdxNativesLoader.load();
        var original = BoardGeometry.tuning();
        var family = UnitFamilyScale.INFANTRY;
        float originalSize = family.UNIT_SCALE, originalHeight = family.HEIGHT_SCALE;
        var model = model(family);
        var other = model(UnitFamilyScale.BATTLE_ARMOR);
        var coords = new Coords(2, 2);
        var footprint = new ArrayList<>(List.of(coords));
        if (multiHex) {
            for (int direction = 0; direction < 6; direction++) { footprint.add(coords.translated(direction)); }
        }
        var unit = new BoardScene.Unit(1, -1, "Scale review", new BoardScene.Waypoint(coords, 2, 0),
              null, false, null, 1, false, null, 0, footprint);
        try {
            BoardGeometry.tune(new BoardGeometry.Tuning(1.3f, .55f, 1.2f, 20, .8f, .81f));
            Vector3 baseline = place(model, unit);
            Vector3 otherBaseline = place(other, unit);
            Vector3 anchor = model.instance.transform.getTranslation(new Vector3());
            family.UNIT_SCALE = originalSize * 1.25f;
            family.HEIGHT_SCALE = originalHeight * 1.4f;
            assertScale(new Vector3(baseline).scl(1.25f, 1.25f, 1.75f), place(model, unit));
            assertScale(otherBaseline, place(other, unit));
            assertScale(anchor, model.instance.transform.getTranslation(new Vector3()));
            assertEquals(model.instance.transform.getScaleX(), model.horizontalScale(unit), .0001f,
                  "Animation distance conversion must match actual placement");
            assertEquals(model.instance.transform.getScaleZ(), model.verticalScale(model.horizontalScale(unit)), .0001f);

            var board = BoardGeometry.tuning();
            BoardGeometry.tune(new BoardGeometry.Tuning(board.hexScale(), board.unitScale() * 1.2f,
                  board.unitHeightScale() * .8f, board.levelHeight(), board.gridShade(), board.multiHexUnitScale() * .9f));
            float horizontal = multiHex ? .9f : 1.2f;
            assertScale(new Vector3(baseline).scl(1.25f * horizontal, 1.25f * horizontal, 1.75f * horizontal * .8f),
                  place(model, unit));
            assertScale(anchor, model.instance.transform.getTranslation(new Vector3()));
        } finally {
            family.UNIT_SCALE = originalSize;
            family.HEIGHT_SCALE = originalHeight;
            BoardGeometry.tune(original);
            model.dispose();
            other.dispose();
        }
    }

    private static GpuMeeple model(UnitFamilyScale family) {
        return new GpuMeeple(new Model(), null, true, List.of(), 1f / 54, new Vector3(100, 100, 54), List.of(), family);
    }

    private static Vector3 place(GpuMeeple model, BoardScene.Unit unit) {
        model.place(model.instance, new OrthographicCamera(), BoardGeometry.center(unit.location().coords(), 2), 0, unit);
        return model.instance.transform.getScale(new Vector3());
    }

    private static void assertScale(Vector3 expected, Vector3 actual) {
        assertEquals(expected.x, actual.x, .0001f);
        assertEquals(expected.y, actual.y, .0001f);
        assertEquals(expected.z, actual.z, .0001f);
    }
}
