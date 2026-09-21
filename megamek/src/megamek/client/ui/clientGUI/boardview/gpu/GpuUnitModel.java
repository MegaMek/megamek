/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;
import megamek.common.annotations.Nullable;

/** One owned unit model with shared placement and annotation geometry. */
final class GpuUnitModel implements Disposable {
    private final Model model;
    final ModelInstance instance;
    private final BoundingBox bounds;
    /** The arm nodes an authored Mek body carries, named as the game names those locations. */
    private static final String[] ARM_NODES = { "LA", "RA" };

    private final String upperBodyNode;
    private final boolean modularCoordinates;
    private final List<UnitEquipmentAssembly.Binding> equipment;
    private final List<UnitRig> rigs;
    private final float levelsPerModelUnit;
    private final Vector3 restDimensions;
    private final UnitFamilyScale familyScale;
    private final boolean damageLocations;

    GpuUnitModel(Model model) {
        this(model, null);
    }

    /**
     * @param upperBodyNode the part of the model that carries everything above the waist, as named by the model's
     *                      descriptor, or {@code null} for a model that turns as one piece
     */
    GpuUnitModel(Model model, @Nullable String upperBodyNode) {
        this(model, upperBodyNode, false);
    }

    GpuUnitModel(Model model, @Nullable String upperBodyNode, UnitFamilyScale familyScale) {
        this(model, upperBodyNode, false, List.of(), upperBodyNode == null ? 1f / 54 : 1f / 27, null, List.of(), familyScale);
    }

    GpuUnitModel(Model model, @Nullable String upperBodyNode, boolean modularCoordinates) {
        this(model, upperBodyNode, modularCoordinates, List.of());
    }

    GpuUnitModel(Model model, @Nullable String upperBodyNode, boolean modularCoordinates, List<UnitEquipmentAssembly.Binding> equipment) {
        this(model, upperBodyNode, modularCoordinates, equipment, upperBodyNode == null ? 1f / 54 : 1f / 27, null, List.of());
    }

    GpuUnitModel(Model model, @Nullable String upperBodyNode, boolean modularCoordinates,
          List<UnitEquipmentAssembly.Binding> equipment, float levelsPerModelUnit, @Nullable Vector3 restDimensions,
          List<UnitRig> rigs) {
        this(model, upperBodyNode, modularCoordinates, equipment, levelsPerModelUnit, restDimensions, rigs, UnitFamilyScale.DEFAULT);
    }

    GpuUnitModel(Model model, @Nullable String upperBodyNode, boolean modularCoordinates,
          List<UnitEquipmentAssembly.Binding> equipment, float levelsPerModelUnit, @Nullable Vector3 restDimensions,
          List<UnitRig> rigs, UnitFamilyScale familyScale) {
        this.modularCoordinates = modularCoordinates;
        this.familyScale = familyScale;
        this.equipment = List.copyOf(equipment);
        this.rigs = List.copyOf(rigs);
        this.levelsPerModelUnit = levelsPerModelUnit;
        this.upperBodyNode = ((upperBodyNode != null) && (model.getNode(upperBodyNode) != null))
              ? upperBodyNode : null;
        this.model = model;
        damageLocations = java.util.Arrays.stream(UnitDamageDisplay.Location.values())
              .anyMatch(location -> location != UnitDamageDisplay.Location.ALL && model.getNode(location.node) != null);
        instance = new ModelInstance(model);
        bounds = instance.calculateBoundingBox(new BoundingBox());
        if (!bounds.isValid()) {
            bounds.set(Vector3.Zero, Vector3.Zero);
        }
        this.restDimensions = restDimensions == null ? bounds.getDimensions(new Vector3()) : new Vector3(restDimensions);
    }

    /** Flat artwork for disabled/unavailable models; never extrude a sprite into a unit-shaped solid. */
    static GpuUnitModel sprite(BoardScene.Pixels pixels, TextureRegion region) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder mesh = builder.part("sprite", GL20.GL_TRIANGLES,
              VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.Normal,
              new Material(TextureAttribute.createDiffuse(region.getTexture()), new BlendingAttribute(1f),
                    FloatAttribute.createAlphaTest(.1f), IntAttribute.createCullFace(GL20.GL_NONE)));
        mesh.setUVRange(region);
        float x = pixels.width() * .5f, y = pixels.height() * .5f;
        mesh.rect(-x, -y, 0, x, -y, 0, x, y, 0, -x, y, 0, 0, 0, 1);
        return new GpuUnitModel(builder.end());
    }

    /** @return {@code true} if the upper body can turn on its own, so a torso twist leaves the legs where they are */
    boolean turnsUpperBody() {
        return upperBodyNode != null;
    }

    boolean modularCoordinates() {
        return modularCoordinates;
    }

    List<UnitEquipmentAssembly.Binding> equipment() {
        return equipment;
    }

    List<UnitRig> rigs() {
        return rigs;
    }

    boolean infantry() {
        return familyScale == UnitFamilyScale.INFANTRY || familyScale == UnitFamilyScale.BATTLE_ARMOR;
    }

    String damageLocation(UnitDamageDisplay.Location selected) {
        if (!damageLocations || selected == UnitDamageDisplay.Location.ALL) { return "*"; }
        return model.getNode(selected.node) == null ? null : selected.node;
    }

    /** Apply to a fresh instance, alongside location damage, so repairs restore the original shared artwork. */
    void showEquipment(ModelInstance placed, UnitModelState.Appearance appearance) {
        for (var binding : equipment) {
            var effective = binding.memberId() < 0 ? appearance : appearance.fighters().get(binding.memberId());
            if (effective == null) {
                continue;
            }
            boolean broken = effective.inoperableEquipment().contains(binding.index());
            boolean lamp = binding.emitters().stream().anyMatch(emitter -> "lamp".equals(emitter.role()));
            if (broken || lamp) {
                showEquipment(placed.getNode(binding.node()), broken, lamp && effective.searchlightOn());
            }
        }
    }

    private static void showEquipment(Node node, boolean broken, boolean lit) {
        for (var part : node.parts) {
            var material = part.material.copy();
            boolean wrecked = broken || material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX);
            if (wrecked) {
                material = UnitDamageDisplay.wrecked(material);
            } else if ("detail".equals(material.id)) {
                material.set(ColorAttribute.createEmissive(lit ? .8f : 0, lit ? .75f : 0, lit ? .45f : 0, 1));
            }
            part.material = material;
        }
        node.getChildren().forEach(child -> showEquipment(child, broken, lit));
    }

    /**
     * Shows a torso twist: turns the upper body about its own pivot and leaves the rest of the model alone.
     *
     * @param placed  an instance of this model
     * @param degrees the turn in degrees, clockwise seen from above, measured from the legs
     */
    void turnUpperBody(ModelInstance placed, float degrees) {
        Node upperBody = (upperBodyNode == null) ? null : placed.getNode(upperBodyNode);
        if (upperBody == null) {
            return;
        }
        // Facings run clockwise and rotation about Z runs the other way, as in place().
        upperBody.rotation.set(Vector3.Z, -degrees);
        placed.calculateTransforms();
    }

    /**
     * Shows flipped arms: swings both arms about their own shoulder pivots and leaves the rest of the model alone.
     * The rotation is about each arm node's left-right axis, so an arm rises forward, passes over the shoulder and
     * comes to rest reaching behind the unit - the movement a Mek makes to bring its arm weapons onto a rear arc.
     * <p>
     * Half a turn maps each arm onto the space it already occupied, mirrored front to back, so the flipped pose
     * needs no more room around the shoulder than the resting pose does.
     * </p>
     *
     * @param placed  an instance of this model
     * @param degrees how far over the arms are swung, {@code 0} for forward and {@code 180} for fully flipped
     */
    void flipArms(ModelInstance placed, float degrees) {
        // Zero means the arms are forward, and the animator owns them: leave its walk sway alone
        // rather than pinning both arms to their rest pose on every unflipped unit, every frame.
        if (degrees == 0) {
            return;
        }
        boolean posed = false;
        for (String arm : ARM_NODES) {
            Node node = placed.getNode(arm);
            Node rest = model.getNode(arm);
            if (node != null && rest != null) {
                // Build from the authored rest pose, not from whatever the animator left behind, so
                // running every frame holds the arm at one angle instead of winding it further round.
                node.rotation.set(rest.rotation).mul(new Quaternion(Vector3.X, degrees));
                posed = true;
            }
        }
        if (posed) {
            placed.calculateTransforms();
        }
    }

    /** @return {@code true} if this model has arms that can be shown flipped */
    boolean flipsArms() {
        for (String arm : ARM_NODES) {
            if (instance.getNode(arm) != null) {
                return true;
            }
        }
        return false;
    }

    Vector3 place(ModelInstance placed, Camera camera, Vector3 ground, float facing, int height, boolean multiHex) {
        return place(placed, camera, ground, facing, height, multiHex, familyScale);
    }

    private Vector3 place(ModelInstance placed, Camera camera, Vector3 ground, float facing, int height, boolean multiHex,
          UnitFamilyScale scaleTuning) {
        // Schema-1 sprite sections retain their original placement while external compatibility is supported.
        float scale = (multiHex ? 1 : BoardGeometry.UNIT_SCALE) * scaleTuning.unitScale();
        float thickness = (modularCoordinates ? levelsPerModelUnit : height)
              * BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE * scaleTuning.unitScale() * scaleTuning.heightScale();
        return placeScaled(placed, camera, ground, facing, scale, thickness);
    }

    Vector3 place(ModelInstance placed, Camera camera, Vector3 ground, float facing, BoardScene.Unit unit) {
        var scaleTuning = familyScale.forUnit(unit);
        if (!modularCoordinates) {
            return place(placed, camera, ground, facing, unit.height(), unit.part() >= 0, scaleTuning);
        }
        var footprint = unit.footprint().size() > 1 ? UnitFootprint.layout(unit.location().coords(), unit.footprint(), facing) : null;
        float scale = horizontalScale(footprint, scaleTuning);
        if (footprint != null) {
            ground = new Vector3(ground).add(footprint.offsetX(), footprint.offsetY(), 0);
        }
        // Rest geometry controls shape. Gameplay height changes when prone/flying and must not flatten the rig.
        float thickness = verticalScale(scale, scaleTuning);
        return placeScaled(placed, camera, ground, facing, scale, thickness);
    }

    /** Horizontal distance conversion shared by placement and the distance-driven gait/wheel animation. */
    float horizontalScale(BoardScene.Unit unit) {
        return horizontalScale(unit.footprint().size() > 1
              ? UnitFootprint.layout(unit.location().coords(), unit.footprint(), unit.location().facing() * 60) : null,
              familyScale.forUnit(unit));
    }

    float verticalScale(float horizontalScale, BoardScene.Unit unit) {
        return verticalScale(horizontalScale, familyScale.forUnit(unit));
    }

    private float verticalScale(float horizontalScale, UnitFamilyScale scaleTuning) {
        return levelsPerModelUnit * BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE
              * horizontalScale / (BoardGeometry.HEX_SCALE * BoardGeometry.DEFAULTS.unitScale()) * scaleTuning.heightScale();
    }

    private float horizontalScale(UnitFootprint.Layout footprint, UnitFamilyScale scaleTuning) {
        if (footprint == null) {
            return BoardGeometry.UNIT_SCALE * BoardGeometry.HEX_SCALE * scaleTuning.unitScale();
        }
        return BoardGeometry.MULTI_HEX_UNIT_SCALE * scaleTuning.unitScale() * Math.min(footprint.width() / Math.max(1, restDimensions.x),
              footprint.depth() / Math.max(1, restDimensions.y));
    }

    private Vector3 placeScaled(ModelInstance placed, Camera camera, Vector3 ground, float facing, float scale,
          float thickness) {
        // Authored Z is in nominal occupied-height units. Keep feet half a world unit above the ground.
        placed.transform.set(ground, new Quaternion(Vector3.Z, -facing))
              .translate(0, 0, 0.5f)
              .scale(scale, scale, thickness);
        return anchor(placed, camera);
    }

    Vector3 anchor(ModelInstance placed, Camera camera) {
        BoundingBox posed = UnitBounds.local(placed);
        if (!posed.isValid()) {
            posed.set(bounds);
        }
        float top = -Float.MAX_VALUE;
        for (float horizontal : new float[] { posed.min.x, posed.max.x }) {
            for (float vertical : new float[] { posed.min.y, posed.max.y }) {
                for (float depth : new float[] { posed.min.z, posed.max.z }) {
                    top = Math.max(top, new Vector3(horizontal, vertical, depth).rot(placed.transform).dot(camera.up));
                }
            }
        }
        return placed.transform.getTranslation(new Vector3()).mulAdd(camera.up, top);
    }

    @Override
    public void dispose() {
        model.dispose();
    }
}
