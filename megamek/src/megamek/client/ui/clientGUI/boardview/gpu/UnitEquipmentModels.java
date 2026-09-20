/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.utils.JsonValue;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.UnitModelEquipment;

/** Canonical equipment mappings compiled by the art tools. Type policy is applied before any asset lookup. */
final class UnitEquipmentModels {
    private final JsonValue equipment;
    private final JsonValue fallbacks;

    record Visual(String asset, String compact, String family, boolean fallback) { }

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
        JsonValue styles = entry.get("styles");
        if (styles != null) {
            model = styles.getString(style, model);
        }
        JsonValue profiles = entry.get("profiles");
        if (profiles != null) {
            model = profiles.getString(placement.getString("profile", ""), model);
        }
        return new Visual(model, entry.getString("lowDetail", fallback(mount)), family(mount),
              entry.getBoolean("fallback", false));
    }

    String fallback(UnitModelEquipment.Mount mount) {
        if (!mount.policy().allowsFallback()) {
            return null;
        }
        String family = mount.policy() == EquipmentModelPolicy.PHYSICAL_WEAPON ? "physical" : mount.family();
        return fallbacks.getString(family, fallbacks.getString("weapon"));
    }
}
