/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.UnitModelEquipment;
import org.junit.jupiter.api.Test;

class UnitEquipmentModelsTest {
    private final JsonValue placement = new JsonReader().parse("{}");

    @Test
    void mandatoryExclusionsWinOverMappingsAndOnlyWeaponsGetFallbacks() {
        var catalog = catalog("mapped", "custom.json");
        assertNull(catalog.resolve(mount("mapped", EquipmentModelPolicy.NONE), placement));
        assertNull(catalog.resolve(mount("mapped", EquipmentModelPolicy.MEMBERS), placement));
        assertNull(catalog.resolve(mount("missing", EquipmentModelPolicy.OPTIONAL_MISC), placement));
        assertEquals("weapon.json", catalog.resolve(mount("missing", EquipmentModelPolicy.WEAPON), placement).asset());
        assertEquals("physical.json", catalog.resolve(mount("missing", EquipmentModelPolicy.PHYSICAL_WEAPON), placement).asset());
    }

    @Test
    void addingAndReplacingAnEquipmentMappingNeedsNoChassisOrVariantEntry() {
        var item = mount("future-laser", EquipmentModelPolicy.WEAPON);
        assertEquals("weapon.json", catalog("another-item", "first.json").resolve(item, placement).asset());
        assertEquals("first.json", catalog("future-laser", "first.json").resolve(item, placement).asset());
        assertEquals("replacement.json", catalog("future-laser", "replacement.json").resolve(item, placement).asset());
        assertNotNull(catalog("future-misc", "misc.json")
              .resolve(mount("future-misc", EquipmentModelPolicy.OPTIONAL_MISC), placement));
    }

    private static UnitEquipmentModels catalog(String id, String model) {
        return new UnitEquipmentModels(new JsonReader().parse("""
              {"schema":2,"equipment":{"%s":{"model":"%s"}},
              "fallbacks":{"weapon":"weapon.json","physical":"physical.json"}}
              """.formatted(id, model)));
    }

    private static UnitModelEquipment.Mount mount(String id, EquipmentModelPolicy policy) {
        return new UnitModelEquipment.Mount(3, id, "LA", "LT", false, false, 0, policy, "laser", List.of());
    }
}
