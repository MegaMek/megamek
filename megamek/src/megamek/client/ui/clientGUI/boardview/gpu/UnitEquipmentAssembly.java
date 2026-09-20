/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.JsonValue;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.UnitModelEquipment;
import megamek.logging.MMLogger;

/** One attachment/fitting implementation for every family, using the actual immutable loadout. */
final class UnitEquipmentAssembly {
    private static final MMLogger LOGGER = MMLogger.create(UnitEquipmentAssembly.class);
    private static final JsonValue DEFAULT_PLACEMENT = new JsonValue(JsonValue.ValueType.object);

    /** Pack in the mount's cross-section, so a side-facing gun reserves its width, not its barrel length. */
    private record MountFrame(UnitModelMountArea area, Matrix4 rotation, Matrix4 inverse) {
        MountFrame(Matrix4 socket) {
            this(new UnitModelMountArea(), new Matrix4(socket.getRotation(new Quaternion(), true)));
        }

        MountFrame(UnitModelMountArea area, Matrix4 rotation) {
            this(area, rotation, new Matrix4(rotation).inv());
        }
    }

    /** One real mounted item, even for split criticals or a multi-barrel launcher. Emitter nodes are assembly-local. */
    record Binding(int index, String location, String node, String asset, boolean embedded,
          List<UnitModelDescriptor.Emitter> emitters, int memberId) {
        Binding(int index, String location, String node, String asset, boolean embedded,
              List<UnitModelDescriptor.Emitter> emitters) {
            this(index, location, node, asset, embedded, emitters, -1);
        }
        Binding {
            emitters = List.copyOf(emitters);
        }
    }

    private record Pending(UnitModelEquipment.Mount mount, UnitModelDescriptor.Hardpoint point,
          JsonValue placement, UnitEquipmentModels.Visual visual, GpuUnitModels.ModularAsset module,
          float scale, float offsetX, float offsetZ) {
        Pending(UnitModelEquipment.Mount mount, UnitModelDescriptor.Hardpoint point, JsonValue placement,
              UnitEquipmentModels.Visual visual, GpuUnitModels.ModularAsset module) {
            this(mount, point, placement, visual, module, placement.getFloat("scale", 1), 0, 0);
        }
    }

    private UnitEquipmentAssembly() { }

    static List<Binding> attachAll(GpuUnitModels library, JsonValue descriptor, GpuUnitModels.ModularAsset body,
          UnitModelState.Structure structure, Model assembled) {
        var catalog = new UnitEquipmentModels(library.descriptor(descriptor.getString("equipment")));
        List<Pending> pending = arrangeBays(library, prepare(library, descriptor, body, catalog, structure));
        pending.sort(Comparator.<Pending, Boolean>comparing(item -> !item.placement().getBoolean("bay", false))
              .thenComparing(item -> item.placement().getString("family", "").isEmpty())
              .thenComparingDouble(item -> -area(item.module().descriptor().bounds()))
              .thenComparingInt(item -> item.mount().index()));
        Map<String, MountFrame> areas = new HashMap<>();
        List<Binding> bindings = new ArrayList<>();
        for (Pending item : pending) {
            attach(library, assembled, item, areas, bindings);
        }
        return bindings;
    }

    private static List<Pending> prepare(GpuUnitModels library, JsonValue descriptor, GpuUnitModels.ModularAsset body,
          UnitEquipmentModels catalog, UnitModelState.Structure structure) {
        Map<String, UnitModelDescriptor.Hardpoint> points = new HashMap<>();
        body.descriptor().hardpoints().forEach(point -> points.put(point.id(), point));
        List<UnitModelEquipment.Mount> equipment = new ArrayList<>(structure.equipment());
        // Entity's external light is separate from Mounted searchlights. Give each physical source one binding.
        if (structure.externalSearchlight()) {
            var point = points.get("external-searchlight");
            if (point != null) {
                equipment.add(new UnitModelEquipment.Mount(-1, "Searchlight", point.location(), "", false, false, 0,
                      EquipmentModelPolicy.OPTIONAL_MISC, "internal", List.of()));
            }
        }
        equipment.sort(Comparator.comparingInt(UnitModelEquipment.Mount::index));
        List<Pending> pending = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        Map<String, Integer> counters = new HashMap<>();
        for (var mount : equipment) {
            var visual = catalog.resolve(mount, DEFAULT_PLACEMENT);
            if (visual == null || !seen.add(mount.index())) {
                continue;
            }
            String form = armForm(structure.anatomy(), mount.location());
            List<JsonValue> candidates = new ArrayList<>();
            int best = Integer.MIN_VALUE;
            for (JsonValue settings : descriptor.get("mounts")) {
                var point = points.get(settings.getString("hardpoint"));
                if (point == null) {
                    throw new IllegalArgumentException("Mount preference references a missing hardpoint");
                }
                String family = settings.getString("family", "");
                String requiredForm = settings.getString("form", "");
                String role = switch (mount.policy()) {
                    case PHYSICAL_WEAPON -> "physical";
                    case OPTIONAL_MISC -> "misc";
                    default -> "weapon";
                };
                boolean anyLocation = point.location().equals("*");
                if ((!anyLocation && !point.location().equals(mount.location()))
                      || !point.side().equals(mount.rear() ? "rear" : "front")
                      || !point.roles().contains(role) || (!family.isEmpty() && !family.equals(visual.family()))
                      || (!requiredForm.isEmpty() && !requiredForm.equals(form))) {
                    continue;
                }
                int priority = (anyLocation ? -1000 : 0) + (requiredForm.isEmpty() ? 0 : 100) + (family.isEmpty() ? 0 : 10);
                if (priority > best) {
                    candidates.clear();
                    best = priority;
                }
                if (priority == best) {
                    candidates.add(settings);
                }
            }
            if (candidates.isEmpty()) {
                if (!mount.policy().allowsFallback()) {
                    LOGGER.warn("No optional equipment socket for {} at {}", mount.internalName(), mount.location());
                    continue;
                }
                throw new IllegalArgumentException("No hardpoint for " + mount.internalName() + " at " + mount.location());
            }
            String bank = mount.location() + ":" + mount.rear() + ":" + visual.family() + ":" + form;
            int slot = counters.merge(bank, 1, Integer::sum) - 1;
            JsonValue settings = candidates.get(Math.min(slot, candidates.size() - 1));
            visual = catalog.resolve(mount, settings);
            GpuUnitModels.ModularAsset module = library.modular(visual.asset());
            if (!validModule(module, mount)) {
                String fallback = catalog.fallback(mount);
                if (fallback == null) {
                    continue;
                }
                LOGGER.warn("Missing valid mesh/emitter for {}; using its weapon-family fallback", mount.internalName());
                visual = new UnitEquipmentModels.Visual(fallback, null, visual.family(), true);
                module = library.modular(fallback);
            }
            if (!validModule(module, mount)) {
                throw new IllegalArgumentException("Missing equipment mesh for " + mount.internalName());
            }
            if (visual.fallback()) {
                LOGGER.warn("Using equipment fallback for {} at {}", mount.internalName(), mount.location());
            }
            var point = points.get(settings.getString("hardpoint"));
            if (point.location().equals("*")) {
                point = new UnitModelDescriptor.Hardpoint(point.id() + ":" + mount.location(), mount.location(), point.side(),
                      point.node(), point.position(), point.rotation(), point.size(), point.minScale(), point.maxScale(), point.roles());
            }
            pending.add(new Pending(mount, point, settings, visual, module));
        }
        return pending;
    }

    private static void attach(GpuUnitModels library, Model assembled, Pending item,
          Map<String, MountFrame> areas, List<Binding> bindings) {
        var point = item.point();
        Node parent = assembled.getNode(point.node());
        Matrix4 socket = new Matrix4(parent.globalTransform).mul(point.transform());
        socket.translate(item.offsetX(), 0, item.offsetZ());
        float scale = item.scale();
        if (!Float.isFinite(scale) || scale <= 0 || scale > point.maxScale()) {
            throw new IllegalArgumentException("Invalid mount scale: " + point.id());
        }
        var area = areas.computeIfAbsent(point.location() + ":" + point.side(), ignored -> new MountFrame(socket));
        var module = item.module();
        String moduleAsset = item.visual().asset();
        Vector3 size = new Vector3();
        Vector3 center = new Vector3();
        Matrix4 transform = moduleTransform(socket, item, module, scale);
        UnitModelMountArea.Fit fit = fit(area, module, transform, item, scale, center, size);
        if (fit == null && item.visual().compact() != null) {
            var compact = library.modular(item.visual().compact());
            if (validModule(compact, item.mount())) {
                module = compact;
                moduleAsset = item.visual().compact();
                transform = moduleTransform(socket, item, module, scale);
                fit = fit(area, module, transform, item, scale, center, size);
            }
        }
        String prefix = point.location() + "-equipment-" + item.mount().index();
        Node placement = new Node();
        placement.id = prefix;
        boolean embedded = fit == null;
        if (embedded) {
            LOGGER.warn("No mesh space for {} at {}; retaining its embedded emitter binding",
                  item.mount().internalName(), point.id());
            transform = socket;
        } else {
            // Scale about the socket, then move the projected footprint to the free position.
            Vector3 origin = socket.getTranslation(new Vector3()).mul(area.inverse());
            transform.scale(fit.scale(), fit.scale(), fit.scale());
            Vector3 shiftedCenter = new Vector3(center).sub(origin).scl(fit.scale()).add(origin);
            transform.trn(new Vector3(fit.x() - shiftedCenter.x, 0, fit.z() - shiftedCenter.z).rot(area.rotation()));
        }
        Matrix4 local = new Matrix4(parent.globalTransform).inv().mul(transform);
        local.getTranslation(placement.translation);
        local.getRotation(placement.rotation, true);
        local.getScale(placement.scale);
        parent.addChild(placement);
        List<UnitModelDescriptor.Emitter> emitters = new ArrayList<>();
        if (embedded) {
            for (var emitter : module.descriptor().emitters()) {
                emitters.add(new UnitModelDescriptor.Emitter(emitter.id(), placement.id, List.of(0f, .1f, 0f),
                      emitter.direction(), emitter.role(), emitter.effect()));
            }
        } else {
            var instance = new ModelInstance(module.model());
            for (Node node : instance.nodes) {
                rename(node, prefix);
                placement.addChild(node);
            }
            for (var emitter : module.descriptor().emitters()) {
                emitters.add(new UnitModelDescriptor.Emitter(emitter.id(), prefix + "-" + emitter.node(),
                      emitter.position(), emitter.direction(), emitter.role(), emitter.effect()));
            }
        }
        bindings.add(new Binding(item.mount().index(), item.mount().location(), placement.id, moduleAsset,
              embedded, emitters));
    }

    /** A bay's launchers are sized together, so its first large launcher cannot consume every remaining slot. */
    private static List<Pending> arrangeBays(GpuUnitModels library, List<Pending> pending) {
        Map<String, List<Pending>> bays = new java.util.LinkedHashMap<>();
        List<Pending> result = new ArrayList<>();
        for (var item : pending) {
            if (item.placement().getBoolean("bay", false)) {
                bays.computeIfAbsent(item.point().id(), ignored -> new ArrayList<>()).add(item);
            } else {
                result.add(item);
            }
        }
        for (var bay : bays.values()) {
            int columns = bay.size() <= 2 ? 1 : Math.min(bay.size(), bay.getFirst().placement().getInt("bayColumns", 1));
            float fit = bayFit(bay, columns);
            float initialFit = fit;
            if (bay.stream().anyMatch(item -> item.scale() * initialFit < item.point().minScale())) {
                for (int index = 0; index < bay.size(); index++) {
                    var item = bay.get(index);
                    if (item.visual().compact() != null) {
                        var compact = library.modular(item.visual().compact());
                        if (validModule(compact, item.mount())) {
                            var visual = new UnitEquipmentModels.Visual(item.visual().compact(), null,
                                  item.visual().family(), item.visual().fallback());
                            bay.set(index, new Pending(item.mount(), item.point(), item.placement(), visual, compact));
                        }
                    }
                }
                fit = bayFit(bay, columns);
            }
            float[] heights = bayHeights(bay, columns);
            float width = (float) bay.stream().mapToDouble(item -> dimension(item, 0)).max().orElse(0) * fit;
            float totalHeight = .4f * (heights.length - 1);
            for (float height : heights) {
                totalHeight += height * fit;
            }
            float z = -totalHeight / 2;
            for (int row = 0; row < heights.length; row++) {
                int count = Math.min(columns, bay.size() - row * columns);
                for (int col = 0; col < count; col++) {
                    var item = bay.get(row * columns + col);
                    // If even compact modules exceed the authored minimum, the normal fitter retains explicit emitters.
                    float scale = Math.max(item.point().minScale(), item.scale() * fit);
                    result.add(new Pending(item.mount(), item.point(), item.placement(), item.visual(), item.module(),
                          scale, (col - (count - 1) / 2f) * (width + .4f), z + heights[row] * fit / 2));
                }
                z += heights[row] * fit + .4f;
            }
        }
        return result;
    }

    private static boolean validModule(GpuUnitModels.ModularAsset module, UnitModelEquipment.Mount mount) {
        return module != null && "equipment".equals(module.descriptor().kind())
              && (!mount.policy().allowsFallback() || !module.descriptor().emitters().isEmpty());
    }

    private static float dimension(Pending item, int axis) {
        var bounds = item.module().descriptor().bounds();
        return (bounds.max().get(axis) - bounds.min().get(axis)) * item.scale();
    }

    private static float[] bayHeights(List<Pending> bay, int columns) {
        float[] heights = new float[(bay.size() + columns - 1) / columns];
        for (int index = 0; index < bay.size(); index++) {
            heights[index / columns] = Math.max(heights[index / columns], dimension(bay.get(index), 2));
        }
        return heights;
    }

    private static float bayFit(List<Pending> bay, int columns) {
        float[] heights = bayHeights(bay, columns);
        float height = 0;
        for (float row : heights) {
            height += row;
        }
        float width = (float) bay.stream().mapToDouble(item -> dimension(item, 0)).max().orElseThrow() * columns;
        var bounds = bay.getFirst().point().size();
        return Math.max(0, Math.min(1, Math.min((bounds.get(0) - .4f * (columns - 1)) / width,
              (bounds.get(2) - .4f * (heights.length - 1)) / height)));
    }

    private static UnitModelMountArea.Fit fit(MountFrame frame, GpuUnitModels.ModularAsset module,
          Matrix4 transform, Pending item, float scale, Vector3 center, Vector3 size) {
        var bounds = module.descriptor().bounds();
        var box = new BoundingBox(UnitModelDescriptor.vector(bounds.min()), UnitModelDescriptor.vector(bounds.max()))
              .mul(new Matrix4(frame.inverse()).mul(transform));
        box.getDimensions(size);
        box.getCenter(center);
        // A held physical weapon extends beyond its mount; reserve the grip rather than shrinking its blade/head.
        if (item.mount().policy() == EquipmentModelPolicy.PHYSICAL_WEAPON) {
            transform.getTranslation(center).mul(frame.inverse());
            size.x = Math.min(size.x, 3 * scale);
            size.z = Math.min(size.z, 3 * scale);
        }
        return frame.area().place(center.x, center.z, size.x, size.z, item.point().size().get(0), item.point().size().get(2),
              Math.min(1, item.point().minScale() / scale));
    }

    private static Matrix4 moduleTransform(Matrix4 socket, Pending item, GpuUnitModels.ModularAsset module, float scale) {
        float length = item.placement().getFloat("length", 0);
        float depthScale = length > 0 ? length / module.descriptor().bounds().max().get(1) : scale;
        return new Matrix4(socket).scale(scale, depthScale, scale);
    }

    private static double area(UnitModelDescriptor.Bounds bounds) {
        return (bounds.max().get(0) - bounds.min().get(0)) * (bounds.max().get(2) - bounds.min().get(2));
    }

    static void rename(Node node, String prefix) {
        node.id = prefix + "-" + node.id;
        node.getChildren().forEach(child -> rename(child, prefix));
    }

    static String armForm(UnitModelState.MekAnatomy anatomy, String location) {
        if (anatomy == null || anatomy.hands().contains(location)) {
            return "hand";
        }
        return anatomy.lowerArms().contains(location) ? "wrist" : "elbow";
    }

}
