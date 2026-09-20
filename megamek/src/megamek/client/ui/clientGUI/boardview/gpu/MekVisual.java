/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.utils.JsonValue;

/** Mek anatomy and size; equipment fitting is shared with the other families. */
final class MekVisual {
    private MekVisual() { }

    static GpuUnitModel assemble(GpuUnitModels library, JsonValue descriptor, UnitModelState.Structure structure) {
        if (structure.anatomy() != null && !descriptor.getString("configuration").equals(structure.anatomy().configuration())) {
            throw new IllegalArgumentException("Body topology does not match this Mek's configuration");
        }
        var body = library.modular(descriptor.getString("body"));
        if (body == null || !"body".equals(body.descriptor().kind())) {
            throw new IllegalArgumentException("Missing Mek body");
        }
        Model assembled = new Model();
        try {
            for (Node node : new ModelInstance(body.model()).nodes) {
                filterAnatomy(node, structure.anatomy());
                assembled.nodes.add(node);
            }
            assembled.calculateTransforms();
            var bindings = UnitEquipmentAssembly.attachAll(library, descriptor, body, structure, assembled);
            assembled.calculateTransforms();
            return new GpuUnitModel(assembled, body.descriptor().joints().get("torso"), true, bindings, 1f / 27, null,
                  java.util.List.of(new UnitRig(body.descriptor())), UnitFamilyScale.forFamily(body.descriptor().family()));
        } catch (RuntimeException error) {
            assembled.dispose();
            throw error;
        }
    }

    private static void filterAnatomy(Node node, UnitModelState.MekAnatomy anatomy) {
        for (int index = node.getChildCount() - 1; index >= 0; index--) {
            Node child = node.getChild(index);
            int separator = child.id.indexOf('@');
            if (separator >= 0) {
                String arm = child.id.substring(0, separator);
                String part = child.id.substring(separator + 1);
                String form = UnitEquipmentAssembly.armForm(anatomy, arm);
                boolean keep = part.equals(form) || (part.equals("forearm") && !form.equals("elbow"));
                if (!keep) {
                    node.removeChild(child);
                    continue;
                }
            }
            filterAnatomy(child, anatomy);
        }
    }
}
