/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.utils.JsonValue;

/** A small shared fighter body per actual squadron member, each with its own live equipment bindings. */
final class SquadronVisual {
    private SquadronVisual() { }

    static GpuUnitModel assemble(GpuUnitModels library, JsonValue descriptor, UnitModelState.Structure structure) {
        var members = structure.bodyForm().fighters();
        var body = library.modular(descriptor.getString("body"));
        if (body == null || body.triangles() * members.size() > UnitModelDescriptor.TRIANGLE_LIMIT) {
            throw new IllegalArgumentException("Missing or over-budget squadron body");
        }
        Model assembled = new Model();
        List<UnitEquipmentAssembly.Binding> bindings = new ArrayList<>();
        List<UnitRig> rigs = new ArrayList<>();
        try {
            int columns = Math.min(3, Math.max(1, members.size()));
            int rows = (members.size() + columns - 1) / columns;
            for (int index = 0; index < members.size(); index++) {
                var member = members.get(index);
                var model = FamilyVisual.assemble(library, descriptor, member.structure());
                try {
                    String prefix = "fighter-" + member.id();
                    Node placement = new Node();
                    placement.id = prefix;
                    placement.translation.set((index % columns - (columns - 1) / 2f) * 25,
                          ((rows - 1) / 2f - index / columns) * 25, 0);
                    placement.scale.set(.24f, .24f, .24f);
                    for (Node node : model.instance.nodes) {
                        UnitEquipmentAssembly.rename(node, prefix);
                        placement.addChild(node);
                    }
                    assembled.nodes.add(placement);
                    model.rigs().forEach(rig -> rigs.add(rig.inside(prefix, prefix + "-")));
                    for (var binding : model.equipment()) {
                        var emitters = binding.emitters().stream().map(emitter -> new UnitModelDescriptor.Emitter(
                              emitter.id(), prefix + "-" + emitter.node(), emitter.position(), emitter.direction(),
                              emitter.role(), emitter.effect())).toList();
                        bindings.add(new UnitEquipmentAssembly.Binding(binding.index(), binding.location(),
                              prefix + "-" + binding.node(), binding.asset(), binding.embedded(), emitters, member.id()));
                    }
                } finally {
                    model.dispose();
                }
            }
            assembled.calculateTransforms();
            return new GpuUnitModel(assembled, null, true, bindings, 1f / 27, null, rigs,
                  UnitFamilyScale.forFamily(descriptor.getString("family")));
        } catch (RuntimeException error) {
            assembled.dispose();
            throw error;
        }
    }
}
