/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

/** Visual fine-tuning on top of BoardGeometry; keys follow the existing asset families. */
enum UnitFamilyScale {
    MEK(1.0f, 1.0f),
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

    UnitFamilyScale(float unitScale, float heightScale) {
        UNIT_SCALE = unitScale;
        HEIGHT_SCALE = heightScale;
    }

    static UnitFamilyScale forFamily(String family) {
        return switch (family) {
            case "mek" -> MEK;
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
