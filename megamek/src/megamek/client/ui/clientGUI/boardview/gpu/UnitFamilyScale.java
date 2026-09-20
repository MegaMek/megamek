/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import megamek.common.units.EntityWeightClass;

/** Visual fine-tuning on top of BoardGeometry; keys follow the existing asset families. */
enum UnitFamilyScale {
    MEK(1.0f, 1.0f),
    // Weight-class multipliers stack with MEK and the model's authored proportions.
    MEK_LIGHT(MEK, 1.0f, 1.0f),
    MEK_MEDIUM(MEK, 1.0f, 1.0f),
    MEK_HEAVY(MEK, 1.0f, 1.0f),
    MEK_ASSAULT(MEK, 1.0f, 1.0f),
    MEK_SUPER_HEAVY(MEK, 1.0f, 1.0f),
    INFANTRY(1.0f, 1.0f),
    BATTLE_ARMOR(1.0f, 1.0f),
    VEHICLE(1.0f, 1.0f),
    AIRCRAFT(1.0f, 1.0f),
    NAVAL(1.0f, 1.0f),
    PROTOMEK(1.0f, 1.0f),
    STATIC(1.0f, 1.0f),
    DEFAULT(1.0f, 1.0f);

    float UNIT_SCALE;
    float HEIGHT_SCALE;
    private final UnitFamilyScale parent;

    UnitFamilyScale(float unitScale, float heightScale) {
        this(null, unitScale, heightScale);
    }

    UnitFamilyScale(UnitFamilyScale parent, float unitScale, float heightScale) {
        this.parent = parent;
        UNIT_SCALE = unitScale;
        HEIGHT_SCALE = heightScale;
    }

    float unitScale() {
        return UNIT_SCALE * (parent == null ? 1 : parent.UNIT_SCALE);
    }

    float heightScale() {
        return HEIGHT_SCALE * (parent == null ? 1 : parent.HEIGHT_SCALE);
    }

    /** Resolve per placement: legacy meshes can be shared by Meks of different weight classes. */
    UnitFamilyScale forUnit(BoardScene.Unit unit) {
        if (this != MEK || unit.model() == null || unit.model().state() == null) {
            return this;
        }
        var anatomy = unit.model().state().structure().anatomy();
        if (anatomy == null) {
            return this;
        }
        return switch (anatomy.weightClass()) {
            case EntityWeightClass.WEIGHT_ULTRA_LIGHT, EntityWeightClass.WEIGHT_LIGHT -> MEK_LIGHT;
            case EntityWeightClass.WEIGHT_MEDIUM -> MEK_MEDIUM;
            case EntityWeightClass.WEIGHT_HEAVY -> MEK_HEAVY;
            case EntityWeightClass.WEIGHT_ASSAULT -> MEK_ASSAULT;
            case EntityWeightClass.WEIGHT_SUPER_HEAVY -> MEK_SUPER_HEAVY;
            default -> this;
        };
    }

    static UnitFamilyScale forFamily(String family) {
        return switch (family) {
            case "mek", "mek-biped", "mek-tripod", "mek-quad" -> MEK;
            case "infantry" -> INFANTRY;
            case "battle-armor" -> BATTLE_ARMOR;
            case "vehicle" -> VEHICLE;
            case "aircraft" -> AIRCRAFT;
            case "naval" -> NAVAL;
            case "proto" -> PROTOMEK;
            case "static" -> STATIC;
            default -> DEFAULT;
        };
    }
}
