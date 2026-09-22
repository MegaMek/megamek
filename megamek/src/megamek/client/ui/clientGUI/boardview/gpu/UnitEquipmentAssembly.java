/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.JsonValue;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.UnitModelEquipment;
import megamek.logging.MMLogger;

/** One attachment/fitting implementation for every family, using the actual immutable loadout. */
final class UnitEquipmentAssembly {
    private static final MMLogger LOGGER = MMLogger.create(UnitEquipmentAssembly.class);
    private static final JsonValue DEFAULT_PLACEMENT = new JsonValue(JsonValue.ValueType.object);
    private static final Pattern HEAT_SINK = Pattern.compile("(?i)heat ?sink");
    private static final List<String> TORSO = List.of("CT", "LT", "RT");
    /** House rule: at most two vents on the front and two on the back. */
    private static final int VENTS_PER_FACE = 2;
    /** Clear space kept around a vent, so a weapon beside it does not sit on its edge. */
    private static final float VENT_MARGIN = .3f;

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

    /** Clear space kept between two stacked neighbours, matching the packer's own gap. */
    private static final float STACK_GAP = .4f;

    private UnitEquipmentAssembly() { }

    static List<Binding> attachAll(GpuUnitModels library, JsonValue descriptor, GpuUnitModels.ModularAsset body,
          UnitModelState.Structure structure, Model assembled) {
        var catalog = new UnitEquipmentModels(library.descriptor(descriptor.getString("equipment")));
        // Two held weapons in one hand share its hard point, so the stacking below sets them over-under like a
        // double-barrelled gun, the larger on top, both leaving from the front of the one gun body.
        List<Pending> pending = centreStacks(arrangeBays(library, prepare(library, descriptor, body, catalog, structure)));
        pending.sort(Comparator.<Pending, Boolean>comparing(item -> !item.placement().getBoolean("bay", false))
              .thenComparing(item -> item.placement().getString("family", "").isEmpty())
              .thenComparingDouble(item -> -area(item.module().descriptor().bounds()))
              .thenComparingInt(item -> item.mount().index()));
        Map<String, MountFrame> areas = new HashMap<>();
        List<Binding> bindings = new ArrayList<>();
        Set<String> holding = new HashSet<>();
        for (Pending item : pending) {
            if (item.visual().held()) {
                holding.add(item.point().location());
            }
        }
        for (Pending item : pending) {
            attach(library, assembled, item, areas, bindings, holding.contains(item.point().location()));
        }
        // An arm holding a gun shows the chassis's gun body in place of its hand; any other arm keeps its hand.
        for (String arm : new String[] { "LA", "RA" }) {
            Node unused = assembled.getNode(arm + (holding.contains(arm) ? "@hand" : "@held"), true);
            if (unused != null) {
                unused.detach();
            }
        }
        chooseVents(descriptor, structure, assembled, bindings);
        return bindings;
    }

    /**
     * Weapons first, vents after. The body offers vent spots: the ones the chassis author drew, listed first, and
     * spares on the flat of each torso face. Vents go in the torsos holding this variant's slotted heat sinks, the two
     * with the most, or both in one when only one holds any; a variant whose sinks all sit in the engine keeps the
     * author's vents where the author put them. Each vent takes the first spot of its torso that no weapon covers and
     * no other vent has taken. A vent with no free spot is left off, and every unused spot is removed.
     */
    private static void chooseVents(JsonValue descriptor, UnitModelState.Structure structure, Model assembled,
          List<Binding> bindings) {
        JsonValue vents = descriptor.get("vents");
        if (vents == null) {
            return;
        }
        assembled.calculateTransforms();
        List<BoundingBox> weapons = new ArrayList<>();
        for (Binding binding : bindings) {
            Node node = binding.embedded() ? null : assembled.getNode(binding.node(), true);
            if (node != null) {
                BoundingBox box = new BoundingBox().inf();
                node.extendBoundingBox(box, true);
                if (box.isValid()) {
                    weapons.add(box);
                }
            }
        }
        Map<String, Integer> sinks = new HashMap<>();
        for (var mount : structure.equipment()) {
            if (TORSO.contains(mount.location()) && HEAT_SINK.matcher(mount.internalName()).find()) {
                sinks.merge(mount.location(), 1, Integer::sum);
            }
        }
        Set<String> kept = new HashSet<>();
        for (String side : new String[] { "front", "rear" }) {
            List<BoundingBox> placed = new ArrayList<>();
            for (String location : ventLocations(vents, side, sinks)) {
                boolean found = false;
                for (JsonValue vent : vents) {
                    String name = vent.getString("node");
                    if (!side.equals(vent.getString("side")) || !location.equals(vent.getString("location"))
                          || kept.contains(name)) {
                        continue;
                    }
                    BoundingBox box = ventBox(assembled, vent);
                    if (box == null || overlapsAny(box, weapons)) {
                        LOGGER.debug("Vent spot {} is covered by a weapon", name);
                        continue;
                    }
                    if (overlapsAny(box, placed)) {
                        LOGGER.debug("Vent spot {} is taken by another vent", name);
                        continue;
                    }
                    kept.add(name);
                    placed.add(box);
                    found = true;
                    LOGGER.debug("Vent kept at {} ({} {}, {})", name, location, side,
                          vent.getBoolean("authored", false) ? "authored spot" : "spare spot");
                    break;
                }
                if (!found) {
                    LOGGER.debug("Vent for {} {} left off: every spot is covered", location, side);
                }
            }
        }
        for (JsonValue vent : vents) {
            String name = vent.getString("node");
            Node node = assembled.getNode(name, true);
            if (node != null && !kept.contains(name)) {
                node.detach();
            }
        }
    }

    /**
     * The torso each vent on this face belongs in, one entry per vent. The torsos with the most slotted heat sinks
     * win; with none slotted, the author's own vents keep their places.
     */
    private static List<String> ventLocations(JsonValue vents, String side, Map<String, Integer> sinks) {
        List<String> wanted = new ArrayList<>();
        if (sinks.isEmpty()) {
            for (JsonValue vent : vents) {
                if (side.equals(vent.getString("side")) && vent.getBoolean("authored", false)
                      && wanted.size() < VENTS_PER_FACE) {
                    wanted.add(vent.getString("location"));
                }
            }
            return wanted;
        }
        List<String> ranked = new ArrayList<>(TORSO);
        ranked.removeIf(location -> !sinks.containsKey(location));
        ranked.sort(Comparator.comparingInt(location -> -sinks.get(location)));
        for (String location : ranked) {
            if (wanted.size() < VENTS_PER_FACE) {
                wanted.add(location);
            }
        }
        if (wanted.size() == 1) {
            wanted.add(wanted.getFirst());
        }
        return wanted;
    }

    private static BoundingBox ventBox(Model assembled, JsonValue vent) {
        Node node = assembled.getNode(vent.getString("node"), true);
        if (node == null) {
            return null;
        }
        float[] low = vent.get("min").asFloatArray();
        float[] high = vent.get("max").asFloatArray();
        return new BoundingBox(new Vector3(low[0] - VENT_MARGIN, low[1], low[2] - VENT_MARGIN),
              new Vector3(high[0] + VENT_MARGIN, high[1], high[2] + VENT_MARGIN)).mul(node.globalTransform);
    }

    private static boolean overlapsAny(BoundingBox box, List<BoundingBox> others) {
        for (BoundingBox other : others) {
            if (box.intersects(other)) {
                return true;
            }
        }
        return false;
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
            if (structure.anatomy() != null && "partial-wing".equals(visual.family())) {
                var module = library.modular(visual.asset());
                if (validModule(module, mount)) {
                    pending.add(partialWing(body, mount, visual, module));
                }
                continue;
            }
            Pending ruled = ruled(library, descriptor, points, catalog, mount);
            if (ruled != null) {
                pending.add(ruled);
                continue;
            }
            String location = attachmentLocation(structure.anatomy(), mount);
            String form = armForm(structure.anatomy(), location);
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
                if ((!anyLocation && !point.location().equals(location))
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
            String bank = location + ":" + mount.rear() + ":" + visual.family() + ":" + form;
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
                point = new UnitModelDescriptor.Hardpoint(point.id() + ":" + location, location, point.side(),
                      point.node(), point.position(), point.rotation(), point.size(), point.minScale(), point.maxScale(), point.roles());
            }
            pending.add(new Pending(mount, point, settings, visual, module));
        }
        return pending;
    }

    /**
     * A chassis rule draws one weapon with another weapon's art at a spot of its own, whichever location carries it:
     * every Atlas LRM 20 is drawn as a five-tube rack stood on the waist. The weapon keeps its own location, so it
     * still goes with that location when the location is destroyed.
     *
     * @return the placed weapon, or {@code null} when no rule of this chassis covers it
     */
    private static Pending ruled(GpuUnitModels library, JsonValue descriptor,
          Map<String, UnitModelDescriptor.Hardpoint> points, UnitEquipmentModels catalog, UnitModelEquipment.Mount mount) {
        JsonValue rules = descriptor.get("rules");
        if (rules == null || mount.rear()) {
            return null;
        }
        for (JsonValue rule : rules) {
            String exclude = rule.getString("exclude", "");
            if (!Pattern.compile(rule.getString("match")).matcher(mount.internalName()).find()
                  || (!exclude.isEmpty() && Pattern.compile(exclude).matcher(mount.internalName()).find())) {
                continue;
            }
            JsonValue placement = rule.get("placement");
            var drawnAs = new UnitModelEquipment.Mount(mount.index(), rule.getString("drawAs"), mount.location(),
                  mount.secondLocation(), mount.rear(), mount.omniPod(), mount.size(), mount.policy(), mount.family(),
                  mount.members());
            var visual = catalog.resolve(drawnAs, placement);
            GpuUnitModels.ModularAsset module = visual == null ? null : library.modular(visual.asset());
            var point = points.get(placement.getString("hardpoint"));
            if (point == null || !validModule(module, mount)) {
                LOGGER.warn("Chassis rule for {} has no usable art or spot; placing it normally", mount.internalName());
                return null;
            }
            LOGGER.debug("Chassis rule draws {} at {} as {}", mount.internalName(), point.id(), rule.getString("drawAs"));
            return new Pending(mount, point, placement, visual, module);
        }
        return null;
    }

    /** A spreadable wing is one paired assembly on the back, independent of its first critical-slot location. */
    private static Pending partialWing(GpuUnitModels.ModularAsset body, UnitModelEquipment.Mount mount,
          UnitEquipmentModels.Visual visual, GpuUnitModels.ModularAsset module) {
        String torso = body.descriptor().joints().get("torso");
        var torsoTransform = body.model().getNode(torso).globalTransform;
        var bodyBounds = body.descriptor().bounds();
        var position = torsoTransform.getTranslation(new Vector3());
        position.set(position.x, bodyBounds.min().get(1) - 1,
              bodyBounds.min().get(2) + (bodyBounds.max().get(2) - bodyBounds.min().get(2)) * .7f);
        // Reuse the torso mount's height, then find the actual back surface (also for tapered/air-Mek bodies).
        body.descriptor().hardpoints().stream().filter(point -> point.location().equals("CT") && point.side().equals("rear"))
              .findFirst().ifPresent(point -> position.z = UnitModelDescriptor.vector(point.position())
                    .mul(body.model().getNode(point.node()).globalTransform).z);
        float distance = new UnitPicking().distance(new ModelInstance(body.model()), new Ray(position, Vector3.Y));
        position.y += Float.isFinite(distance) ? (float) Math.sqrt(distance) : 1;
        position.mul(new Matrix4(torsoTransform).inv());
        var wing = module.descriptor().bounds();
        float span = (bodyBounds.max().get(0) - bodyBounds.min().get(0)) * 1.2f;
        float scale = span / (wing.max().get(0) - wing.min().get(0));
        var point = new UnitModelDescriptor.Hardpoint("partial-wing", mount.location(), "rear", torso,
              List.of(position.x, position.y, position.z), List.of(0f, 0f, 0f, 1f),
              List.of(span + 1, (wing.max().get(1) - wing.min().get(1)) * scale + 1,
                    (wing.max().get(2) - wing.min().get(2)) * scale + 1), scale, scale, List.of("misc"));
        return new Pending(mount, point, DEFAULT_PLACEMENT, visual, module, scale, 0, 0);
    }

    /**
     * @param holdingGun {@code true} when this arm holds a gun, so the chassis's gun body stands where the fist was
     */
    private static void attach(GpuUnitModels library, Model assembled, Pending item,
          Map<String, MountFrame> areas, List<Binding> bindings, boolean holdingGun) {
        var point = item.point();
        Node parent = assembled.getNode(point.node());
        Matrix4 socket = new Matrix4(parent.globalTransform);
        if (holdingGun) {
            // Every weapon in a hand that holds a gun leaves from the front face of the gun body, not the centre of
            // the fist: the held barrel, and any other weapon in that hand, which would otherwise sit buried inside
            // the body. Weapons on the arm's other sockets carry no step and stay put. The step is in the arm's own
            // frame, before the socket's aim is applied.
            JsonValue step = item.placement().get("heldOffset");
            if (step != null) {
                float[] offset = step.asFloatArray();
                socket.translate(offset[0], offset[1], offset[2]);
            }
        }
        socket.mul(point.transform());
        socket.translate(item.offsetX(), 0, item.offsetZ());
        float scale = item.scale();
        if (!Float.isFinite(scale) || scale <= 0 || scale > point.maxScale()) {
            throw new IllegalArgumentException("Invalid mount scale: " + point.id());
        }
        // Wings span the torso and clear its rear face; they must not compete with guns or exhaust for face space.
        // A chassis rule's spot is its own, so it neither takes room from nor gives room to that location's face.
        var area = point.id().equals("partial-wing") || item.placement().getBoolean("rule", false)
              ? new MountFrame(socket)
              // Two locations can share one face, as a Locust's head and centre weapons share its chin turret; the
              // packer then keeps them apart instead of drawing one over the other.
              : areas.computeIfAbsent(item.placement().getString("area", point.location()) + ":" + point.side(),
                    ignored -> new MountFrame(socket));
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
    /**
     * Centres the weapons that share one hard point on that point's facing. Without this the first
     * weapon takes the socket and the rest are pushed clear of it, so a pair hangs below the middle
     * of its facing instead of straddling it. The largest sits at the top of the stack.
     */
    private static List<Pending> centreStacks(List<Pending> pending) {
        Map<String, List<Pending>> stacks = new java.util.LinkedHashMap<>();
        List<Pending> result = new ArrayList<>();
        for (Pending item : pending) {
            // A bay already arranged its own rows and carries the offsets for them.
            if (item.placement().getBoolean("bay", false)) {
                result.add(item);
            } else {
                stacks.computeIfAbsent(item.point().id(), ignored -> new ArrayList<>()).add(item);
            }
        }
        for (List<Pending> stack : stacks.values()) {
            if (stack.size() < 2) {
                result.addAll(stack);
                continue;
            }
            stack.sort(Comparator.comparingDouble((Pending item) -> -area(item.module().descriptor().bounds()))
                  .thenComparingInt(item -> item.mount().index()));
            if ("rows".equals(stack.getFirst().placement().getString("stack", ""))) {
                result.addAll(inRows(stack));
                continue;
            }
            float totalHeight = STACK_GAP * (stack.size() - 1);
            for (Pending item : stack) {
                totalHeight += dimension(item, 2);
            }
            float z = totalHeight / 2;
            for (Pending item : stack) {
                float height = dimension(item, 2);
                z -= height / 2;
                result.add(new Pending(item.mount(), item.point(), item.placement(), item.visual(), item.module(),
                      item.scale(), item.offsetX(), item.offsetZ() + z));
                z -= height / 2 + STACK_GAP;
            }
        }
        return result;
    }

    /**
     * Lays the weapons sharing a hard point out in rows, side by side, as a chassis asks for with a stack setting of
     * "rows": a pair of lasers on a gun pod sits across its face rather than one above the other. A row takes weapons,
     * largest first, while they fit the face's width, and the next starts below it. The block is centred on the face,
     * each row across it, and each row runs outward from the centre line so the left and right sides mirror.
     */
    private static List<Pending> inRows(List<Pending> stack) {
        float faceWidth = stack.getFirst().point().size().get(0);
        List<List<Pending>> rows = new ArrayList<>();
        List<Pending> row = new ArrayList<>();
        float rowWidth = 0;
        for (Pending item : stack) {
            float width = dimension(item, 0);
            if (!row.isEmpty() && rowWidth + STACK_GAP + width > faceWidth) {
                rows.add(row);
                row = new ArrayList<>();
                rowWidth = 0;
            }
            rowWidth += (row.isEmpty() ? 0 : STACK_GAP) + width;
            row.add(item);
        }
        rows.add(row);
        float totalHeight = STACK_GAP * (rows.size() - 1);
        for (List<Pending> line : rows) {
            totalHeight += rowHeight(line);
        }
        // A face's x runs the same world direction on both sides of the Mek, so the left side lays its row the
        // other way to run outward from the centre line too.
        boolean left = stack.getFirst().point().location().startsWith("L");
        List<Pending> result = new ArrayList<>();
        float z = totalHeight / 2;
        for (List<Pending> line : rows) {
            float height = rowHeight(line);
            z -= height / 2;
            float width = STACK_GAP * (line.size() - 1);
            for (Pending item : line) {
                width += dimension(item, 0);
            }
            float x = -width / 2;
            for (Pending item : line) {
                float itemWidth = dimension(item, 0);
                float offset = x + itemWidth / 2;
                result.add(new Pending(item.mount(), item.point(), item.placement(), item.visual(), item.module(),
                      item.scale(), item.offsetX() + (left ? -offset : offset), item.offsetZ() + z));
                x += itemWidth + STACK_GAP;
            }
            z -= height / 2 + STACK_GAP;
        }
        return result;
    }

    private static float rowHeight(List<Pending> row) {
        float height = 0;
        for (Pending item : row) {
            height = Math.max(height, dimension(item, 2));
        }
        return height;
    }

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
        float z = center.z;
        if (item.placement().getBoolean("hang", false)) {
            // A hanging socket marks an underside: the weapon hangs from it, its top against the surface above,
            // however tall it is, instead of being centred on the socket.
            z = transform.getTranslation(new Vector3()).mul(frame.inverse()).z - size.z / 2;
        }
        return frame.area().place(center.x, z, size.x, size.z, item.point().size().get(0), item.point().size().get(2),
              Math.min(1, item.point().minScale() / scale), towardCentreLine(frame, transform));
    }

    /**
     * Which way along a face's own x points at the Mek's centre line. A crowded weapon steps that way, so the left
     * and right torsos pack their weapons as mirror images rather than as copies of one another. A socket already
     * on the centre line keeps the default.
     */
    private static float towardCentreLine(MountFrame frame, Matrix4 socket) {
        float x = socket.getTranslation(new Vector3()).x;
        if (Math.abs(x) < .01f) {
            return -1;
        }
        Vector3 inward = new Vector3(-Math.signum(x), 0, 0).rot(frame.inverse());
        return inward.x >= 0 ? 1 : -1;
    }

    private static Matrix4 moduleTransform(Matrix4 socket, Pending item, GpuUnitModels.ModularAsset module, float scale) {
        // A held gun is authored at its finished size; a barrel-length override would crush it front to back.
        float length = item.visual().held() ? 0 : item.placement().getFloat("length", 0);
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

    /** Split torso/arm guns use torso firing arcs, but their visible barrel belongs to the outer arm. */
    private static String attachmentLocation(UnitModelState.MekAnatomy anatomy, UnitModelEquipment.Mount mount) {
        if (anatomy != null && (("LT".equals(mount.location()) && "LA".equals(mount.secondLocation()))
              || ("RT".equals(mount.location()) && "RA".equals(mount.secondLocation())))) {
            return mount.secondLocation();
        }
        return mount.location();
    }

}
