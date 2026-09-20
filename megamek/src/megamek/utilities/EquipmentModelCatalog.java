/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.utilities;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.common.Configuration;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.equipment.enums.MiscTypeFlag;

/** Full registered-equipment inventory for art coverage; does not depend on which unit variants were catalogued. */
public final class EquipmentModelCatalog {
    private EquipmentModelCatalog() { }

    public record Entry(String internalName, String name, String javaType, EquipmentModelPolicy policy,
          boolean allowsFallback, String family, int rackSize, List<String> flags) { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: EquipmentModelCatalog <mm-data/data> <output.json>");
        }
        Configuration.setDataDir(Path.of(args[0]).toAbsolutePath().normalize().toFile());
        List<Entry> entries = entries();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
              .writerWithDefaultPrettyPrinter().writeValue(output.toFile(), Map.of("schema", 1, "equipment", entries));
        System.out.printf("Exported %d registered equipment types to %s%n", entries.size(), output);
    }

    static List<Entry> entries() {
        return EquipmentType.allTypes().stream().map(EquipmentModelCatalog::describe)
              .sorted(Comparator.comparing(Entry::internalName).thenComparing(Entry::javaType)).toList();
    }

    static Entry describe(EquipmentType type) {
        EquipmentModelPolicy policy = EquipmentModelPolicy.forType(type);
        List<String> flags = type instanceof WeaponType
              ? Arrays.stream(WeaponTypeFlag.values()).filter(type::hasFlag).map(Enum::name).toList()
              : type instanceof MiscType
                    ? Arrays.stream(MiscTypeFlag.values()).filter(type::hasFlag).map(Enum::name).toList() : List.of();
        return new Entry(type.getInternalName(), type.getName(), type.getClass().getName(), policy,
              policy.allowsFallback(), family(type, policy),
              type instanceof WeaponType weapon ? weapon.getRackSize() : 0, flags);
    }

    /** Reuse the established weapon recipe families; policy remains separate from the shape's family. */
    private static String family(EquipmentType type, EquipmentModelPolicy policy) {
        if ((policy == EquipmentModelPolicy.NONE) || (policy == EquipmentModelPolicy.MEMBERS)) {
            return policy == EquipmentModelPolicy.NONE ? "none" : "members";
        }
        String legacy = MekModelCatalog.family(type);
        if (!legacy.equals("internal")) {
            return legacy;
        }
        if (policy == EquipmentModelPolicy.PHYSICAL_WEAPON) {
            return "physical";
        }
        if (type instanceof MiscType misc) {
            if (misc.hasFlag(MiscType.F_SEARCHLIGHT)) {
                return "searchlight";
            }
            return misc.hasFlag(MiscType.F_ECM) ? "ecm" : "misc";
        }
        // An unsupported legacy label must not make an otherwise eligible weapon disappear.
        return "unmapped-weapon";
    }
}
