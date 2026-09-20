/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNode;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Schema 2: independent rigid assets, +Y forward / +Z up, with no legacy Z compression. No GL resources. */
record UnitModelDescriptor(int schema, String kind, String family, String mesh, Bounds bounds, String rig,
          Map<String, String> joints, Map<String, String> locations, List<Hardpoint> hardpoints, List<Emitter> emitters,
          List<LandingSupport> landingSupports, Map<String, String> legBends) {
    private static final ObjectMapper JSON = new ObjectMapper();
    /** Bare body/troop geometry only. Loadout modules have a separate measured cost. */
    static final int TRIANGLE_LIMIT = 1500;
    /** Equipment is reviewed separately: ideally under 100, always strictly under 150 triangles per module. */
    static final int EQUIPMENT_TRIANGLE_LIMIT = 149;

    UnitModelDescriptor {
        require(schema == 2, "Unsupported modular model schema: " + schema);
        require(Set.of("body", "troop", "equipment").contains(kind), "Unknown model kind: " + kind);
        require((family != null) && !family.isBlank(), "Missing model family");
        require((mesh != null) && mesh.endsWith(".g3dj"), "Expected a G3DJ mesh");
        require(bounds != null, "Missing rest bounds");
        require((rig != null) && !rig.isBlank(), "Missing rig identifier");
        joints = Map.copyOf(joints);
        locations = Map.copyOf(locations);
        hardpoints = List.copyOf(hardpoints);
        emitters = List.copyOf(emitters);
        landingSupports = landingSupports == null ? List.of() : List.copyOf(landingSupports);
        legBends = legBends == null ? Map.of() : Map.copyOf(legBends);
        require(joints.containsKey("root"), "Rig needs a root role");
        for (var bend : legBends.entrySet()) {
            require(Set.of("forward", "reverse").contains(bend.getValue()), "Unknown leg bend: " + bend.getValue());
            var leg = java.util.Arrays.stream(UnitRig.LEGS).filter(roles -> roles[0].equals(bend.getKey()))
                  .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown leg role: " + bend.getKey()));
            require(joints.keySet().containsAll(List.of(leg)), "Leg bend needs hip, knee and foot roles: " + bend.getKey());
        }
        require(hardpoints.stream().map(Hardpoint::id).distinct().count() == hardpoints.size(), "Duplicate hardpoint ID");
        require(emitters.stream().map(Emitter::id).distinct().count() == emitters.size(), "Duplicate emitter ID");
        require(landingSupports.isEmpty() || "body".equals(kind) && "aircraft".equals(family),
              "Landing supports belong to aircraft bodies");
        require(landingSupports.stream().map(LandingSupport::id).distinct().count() == landingSupports.size(),
              "Duplicate landing support ID");
    }

    static UnitModelDescriptor read(Path file) throws IOException {
        return JSON.readValue(file.toFile(), UnitModelDescriptor.class);
    }

    /** Check both lexical and real paths, including symlinks, before reading any asset. */
    static Path contained(Path root, Path file) throws IOException {
        Path resolved = file.toAbsolutePath().normalize();
        require(resolved.startsWith(root.toAbsolutePath().normalize()), "Asset escapes model directory: " + file);
        require(resolved.toRealPath().startsWith(root.toRealPath()), "Asset link escapes model directory: " + file);
        return resolved;
    }

    int validate(ModelData data) {
        Map<String, ModelNode> modelNodes = new HashMap<>();
        for (ModelNode node : data.nodes) {
            collect(node, modelNodes);
        }
        Set<String> nodes = modelNodes.keySet();
        require(nodes.containsAll(joints.values()), "Rig references missing nodes");
        require(nodes.containsAll(locations.keySet()), "Location references missing nodes");
        for (var leg : UnitRig.LEGS) {
            if (!legBends.containsKey(leg[0])) { continue; }
            for (int index = 0; index < 2; index++) {
                var parent = modelNodes.get(joints.get(leg[index]));
                var child = modelNodes.get(joints.get(leg[index + 1]));
                require(parent.children != null && java.util.Arrays.asList(parent.children).contains(child),
                      "Leg bend requires a hip / knee / foot chain: " + leg[0]);
            }
        }
        hardpoints.forEach(point -> require(nodes.contains(point.node()), "Missing hardpoint node: " + point.id()));
        emitters.forEach(emitter -> require(nodes.contains(emitter.node()), "Missing emitter node: " + emitter.id()));
        Set<String> supportNodes = new HashSet<>();
        for (var support : landingSupports) {
            support.validate(modelNodes);
            for (String node : List.of(support.node(), support.shaft(), support.foot())) {
                require(joints.containsValue(node), "Landing support must belong to the rig: " + node);
                require(supportNodes.add(node), "Landing supports cannot share controls: " + node);
            }
        }
        for (var meshData : data.meshes) {
            long attributes = new VertexAttributes(meshData.attributes).getMask();
            long expected = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
                  | VertexAttributes.Usage.ColorUnpacked | VertexAttributes.Usage.TextureCoordinates;
            require((attributes == expected) && (meshData.attributes.length == 4),
                  "Modular meshes need POSITION, NORMAL, COLOR and TEXCOORD0");
            int stride = new VertexAttributes(meshData.attributes).vertexSize / Float.BYTES;
            require(meshData.vertices.length % stride == 0, "Incomplete vertex");
            for (float vertex : meshData.vertices) {
                require(Float.isFinite(vertex), "Non-finite mesh vertex");
            }
            for (var part : meshData.parts) {
                require(part.primitiveType == GL20.GL_TRIANGLES, "Expected triangle mesh parts");
                require(part.indices.length % 3 == 0, "Incomplete triangle");
                for (short index : part.indices) {
                    require(Short.toUnsignedInt(index) < meshData.vertices.length / stride, "Invalid vertex index");
                }
            }
        }
        int triangles = triangleCount(data);
        require(triangles > 0, "Empty modular asset");
        int limit = "equipment".equals(kind) ? EQUIPMENT_TRIANGLE_LIMIT : TRIANGLE_LIMIT;
        require(triangles <= limit, kind + " asset exceeds triangle hard cap " + limit + ": " + triangles);
        for (var material : data.materials) {
            require(Set.of("paint", "detail", "bark").contains(material.id), "Unknown material role: " + material.id);
            require((material.textures == null) || material.textures.isEmpty(), "Textures are owned by unit appearance");
        }
        return triangles;
    }

    /** Count repeated node references as rendered geometry, and unused mesh data as resident geometry. */
    static int triangleCount(ModelData data) {
        Map<String, Integer> parts = new HashMap<>();
        int stored = 0;
        for (var mesh : data.meshes) {
            for (var part : mesh.parts) {
                int count = part.indices.length / 3;
                require(parts.put(part.id, count) == null, "Duplicate mesh part: " + part.id);
                stored += count;
            }
        }
        int rendered = 0;
        for (var node : data.nodes) {
            rendered += triangleCount(node, parts);
        }
        return Math.max(stored, rendered);
    }

    private static int triangleCount(ModelNode node, Map<String, Integer> parts) {
        int count = 0;
        if (node.parts != null) {
            for (var part : node.parts) {
                require(parts.containsKey(part.meshPartId), "Missing mesh part: " + part.meshPartId);
                count += parts.get(part.meshPartId);
            }
        }
        if (node.children != null) {
            for (var child : node.children) {
                count += triangleCount(child, parts);
            }
        }
        return count;
    }

    private static void collect(ModelNode node, Map<String, ModelNode> nodes) {
        require(nodes.put(node.id, node) == null, "Duplicate model node: " + node.id);
        if (node.children != null) {
            for (ModelNode child : node.children) {
                collect(child, nodes);
            }
        }
    }

    /** A vertical shaft pivots at its top; the rigid foot is its sibling, never a child of the scaled shaft. */
    record LandingSupport(String id, String node, String shaft, String foot, float length, List<Float> contact,
          List<Float> stowedOffset) {
        LandingSupport {
            require(id != null && !id.isBlank(), "Missing landing support ID");
            require(node != null && shaft != null && foot != null, "Missing landing support control");
            require(Set.of(node, shaft, foot).size() == 3, "Landing support needs separate controls");
            require(Float.isFinite(length) && length > 0, "Landing support needs a positive rest shaft length");
            contact = components(contact, 3);
            stowedOffset = components(stowedOffset, 3);
            require(stowedOffset.get(2) > 0, "Landing supports must retract upward into the hull");
        }

        void validate(Map<String, ModelNode> nodes) {
            require(nodes.containsKey(node) && nodes.containsKey(shaft) && nodes.containsKey(foot),
                  "Missing landing support node: " + id);
            var parent = nodes.get(node);
            require(parent.children != null && java.util.Arrays.stream(parent.children).anyMatch(child -> child.id.equals(shaft))
                        && java.util.Arrays.stream(parent.children).anyMatch(child -> child.id.equals(foot)),
                  "Landing shaft and foot must be children of their deployment node: " + id);
            var upper = nodes.get(shaft);
            var pad = nodes.get(foot);
            require((upper.children == null || upper.children.length == 0) && (pad.children == null || pad.children.length == 0),
                  "Landing shaft and foot must be independent leaf nodes: " + id);
            require((upper.rotation == null || upper.rotation.isIdentity())
                        && (upper.scale == null || upper.scale.epsilonEquals(1, 1, 1, .0001f)),
                  "Landing shaft uses unscaled +Z-up local axes: " + id);
            var top = upper.translation == null ? Vector3.Zero : upper.translation;
            var bottom = pad.translation == null ? Vector3.Zero : pad.translation;
            require(new Vector3(top).sub(bottom).epsilonEquals(0, 0, length, .001f),
                  "Landing shaft rest length must reach its foot pivot: " + id);
        }

        LandingSupport prefixed(String prefix) {
            return new LandingSupport(id, prefix + node, prefix + shaft, prefix + foot, length, contact, stowedOffset);
        }
    }

    record Bounds(List<Float> min, List<Float> max) {
        Bounds {
            min = components(min, 3);
            max = components(max, 3);
            for (int axis = 0; axis < 3; axis++) {
                require(max.get(axis) > min.get(axis), "Rest bounds must have positive dimensions");
            }
        }
    }

    record Hardpoint(String id, String location, String side, String node, List<Float> position, List<Float> rotation,
          List<Float> size, float minScale, float maxScale, List<String> roles) {
        Hardpoint {
            require((id != null) && !id.isBlank(), "Missing hardpoint ID");
            require((location != null) && !location.isBlank(), "Missing hardpoint location");
            require(Set.of("front", "rear", "side").contains(side), "Unknown hardpoint side");
            position = components(position, 3);
            rotation = components(rotation, 4);
            float length = 0;
            for (float component : rotation) {
                length += component * component;
            }
            require(Math.abs(length - 1) < .001, "Hardpoint quaternion must be normalized");
            size = components(size, 3);
            require(size.stream().allMatch(value -> value > 0), "Hardpoint needs a positive mounting area");
            require(Float.isFinite(minScale) && Float.isFinite(maxScale) && (minScale > 0) && (maxScale >= minScale),
                  "Invalid hardpoint scale range");
            roles = List.copyOf(roles);
            require(!roles.isEmpty() && Set.of("weapon", "physical", "misc").containsAll(roles), "Unknown visual role");
        }

        Matrix4 transform() {
            return new Matrix4().set(vector(position), new Quaternion(rotation.get(0), rotation.get(1),
                  rotation.get(2), rotation.get(3)));
        }
    }

    record Emitter(String id, String node, List<Float> position, List<Float> direction, String role, String effect) {
        Emitter {
            require((id != null) && !id.isBlank(), "Missing emitter ID");
            position = components(position, 3);
            direction = components(direction, 3);
            require(Math.abs(vector(direction).len2() - 1) < .001, "Emitter direction must be normalized");
            require(Set.of("muzzle", "beam", "launcher", "exhaust", "lamp", "contact").contains(role),
                  "Unknown emitter role");
            require(Set.of("laser", "ppc", "energy", "bullet", "missile", "cluster", "flame", "spray", "screen", "bomb", "mine",
                        "lamp", "exhaust", "melee", "none")
                  .contains(effect), "Unknown emitter effect");
        }
    }

    static Vector3 vector(List<Float> values) {
        return new Vector3(values.get(0), values.get(1), values.get(2));
    }

    private static List<Float> components(List<Float> values, int count) {
        require((values != null) && (values.size() == count), "Wrong transform component count");
        require(values.stream().allMatch(value -> (value != null) && Float.isFinite(value)), "Non-finite transform");
        return List.copyOf(values);
    }

    private static void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }
}
