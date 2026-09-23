/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.utils.JsonValue;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.UnitModelEquipment;

/** Canonical equipment mappings compiled by the art tools. Type policy is applied before any asset lookup. */
final class UnitEquipmentModels {
    private final JsonValue equipment;
    private final JsonValue fallbacks;

    /**
     * @param held {@code true} when the asset is the weapon drawn as a gun gripped in the fist. That shape is built
     *             at its finished size, so a mount's barrel length does not stretch it.
     */
    record Visual(String asset, String compact, String family, boolean fallback, boolean held) {
        Visual(String asset, String compact, String family, boolean fallback) {
            this(asset, compact, family, fallback, false);
        }
    }

    /** The placement profile that asks for a weapon to be drawn held in the fist. */
    static final String HELD = "held";

    UnitEquipmentModels(JsonValue catalog) {
        if (catalog.getInt("schema", 0) != 2 || catalog.get("equipment") == null || catalog.get("fallbacks") == null) {
            throw new IllegalArgumentException("Invalid equipment model catalog");
        }
        equipment = catalog.get("equipment");
        fallbacks = catalog.get("fallbacks");
    }

    String family(UnitModelEquipment.Mount mount) {
        JsonValue entry = equipment.get(mount.internalName());
        return entry == null ? mount.family() : entry.getString("bankFamily", mount.family());
    }

    /** Shared props use the same canonical mapping as mounted equipment, without a location-specific style. */
    String asset(String internalName) {
        JsonValue entry = equipment.get(internalName);
        return entry == null ? null : entry.getString("model", null);
    }

    Visual resolve(UnitModelEquipment.Mount mount, JsonValue placement) {
        if (mount.policy() == EquipmentModelPolicy.NONE || mount.policy() == EquipmentModelPolicy.MEMBERS) {
            return null;
        }
        JsonValue entry = equipment.get(mount.internalName());
        if (entry == null) {
            String fallback = fallback(mount);
            return fallback == null ? null : new Visual(fallback, null, mount.family(), true);
        }
        String model = entry.getString("model");
        String style = placement.getString("style", mount.location().endsWith("A") ? "long" : "");
        // A light weapon (small and medium lasers) can take a style of its own at a mount, so one socket can draw
        // its heavy lasers long and its light ones short.
        if (entry.getBoolean("light", false) && placement.has("lightStyle")) {
            style = placement.getString("lightStyle");
        }
        JsonValue styles = entry.get("styles");
        if (styles != null) {
            model = styles.getString(style, model);
        }
        JsonValue profiles = entry.get("profiles");
        boolean held = false;
        if (profiles != null) {
            String profile = placement.getString("profile", "");
            String profiled = profiles.getString(profile, null);
            if (profiled != null) {
                model = profiled;
                held = HELD.equals(profile);
            }
        }
        return new Visual(model, entry.getString("lowDetail", fallback(mount)), family(mount),
              entry.getBoolean("fallback", false), held);
    }

    String fallback(UnitModelEquipment.Mount mount) {
        if (!mount.policy().allowsFallback()) {
            return null;
        }
        String family = mount.policy() == EquipmentModelPolicy.PHYSICAL_WEAPON ? "physical" : mount.family();
        return fallbacks.getString(family, fallbacks.getString("weapon"));
    }
}
