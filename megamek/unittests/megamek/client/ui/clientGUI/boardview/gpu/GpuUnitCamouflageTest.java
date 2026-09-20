/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GpuUnitCamouflageTest {
    @Test
    void copiesKeepEqualPaintAndMaterials() {
        var paint = new GpuUnitCamouflage.Paint(45, mock(Texture.class),
              new Matrix4().setToTranslation(1, 2, 3), new Matrix3());
        var copy = (GpuUnitCamouflage.Paint) paint.copy();

        assertEquals(paint, copy);
        assertEquals(copy, paint);
        assertEquals(paint.hashCode(), copy.hashCode());
        assertEquals(0, paint.compareTo(copy));
        assertNotSame(paint.transform, copy.transform);
        assertNotSame(paint.normalMatrix, copy.normalMatrix);
        assertEquals(1, new HashSet<>(List.of(paint, copy)).size());
        assertEquals(new Material("paint", paint), new Material("paint", copy));
        assertNotEquals(paint, null);
        assertNotEquals(paint, new Object());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void collidingMatricesStayDistinctInCollectionsAndMaterials(boolean normalMatrix) {
        var first = new GpuUnitCamouflage.Paint(0, null, new Matrix4(), new Matrix3());
        float[] firstValues = normalMatrix ? first.normalMatrix.val : first.transform.val;
        firstValues[0] = 1f;
        firstValues[1] = 1f;
        var second = (GpuUnitCamouflage.Paint) first.copy();
        float[] secondValues = normalMatrix ? second.normalMatrix.val : second.transform.val;
        // Adjacent float hashes contribute 31 * h1 + h2, so +1 and -31 leave the array hash unchanged.
        int one = Float.floatToIntBits(1f);
        secondValues[0] = Float.intBitsToFloat(one + 1);
        secondValues[1] = Float.intBitsToFloat(one - 31);

        assertEquals(first.hashCode(), second.hashCode(), "The fixture must contain a real hash collision");
        assertNotEquals(0, first.compareTo(second));
        assertNotEquals(first, second);
        assertNotEquals(second, first);
        assertEquals(2, new HashSet<>(List.of(first, second)).size());
        var firstMaterial = new Material("paint", first);
        var secondMaterial = new Material("paint", second);
        assertFalse(firstMaterial.same(secondMaterial, true));
        assertNotEquals(firstMaterial, secondMaterial);
    }

    @Test
    void differentMarkerTexturesStayDistinctEvenWithTheSameHandle() {
        Texture firstMarker = mock(Texture.class);
        Texture secondMarker = mock(Texture.class);
        when(firstMarker.getTextureObjectHandle()).thenReturn(7);
        when(secondMarker.getTextureObjectHandle()).thenReturn(7);
        var first = new GpuUnitCamouflage.Paint(0, firstMarker, new Matrix4(), new Matrix3());
        var second = new GpuUnitCamouflage.Paint(0, secondMarker, new Matrix4(), new Matrix3());

        assertEquals(0, first.compareTo(second), "Rendering order compares texture handles");
        assertNotEquals(first, second, "Equality must use the same texture identity as hashCode");
        assertNotEquals(new Material("paint", first), new Material("paint", second));
    }
}
