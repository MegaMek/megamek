/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.utils.DefaultRenderableSorter;

/** Group opaque shader programs in every scene pass; transparent surfaces retain libGDX's depth ordering. */
final class GpuOpaqueSorter extends DefaultRenderableSorter {
    @Override
    public int compare(Renderable left, Renderable right) {
        var a = left.material.get(BlendingAttribute.class, BlendingAttribute.Type);
        var b = right.material.get(BlendingAttribute.class, BlendingAttribute.Type);
        if ((a == null || !a.blended) && (b == null || !b.blended)) {
            int shader = Integer.compare(System.identityHashCode(left.shader), System.identityHashCode(right.shader));
            if (shader != 0) { return shader; }
        }
        return super.compare(left, right);
    }
}
