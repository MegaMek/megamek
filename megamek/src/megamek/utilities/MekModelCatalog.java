/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import megamek.client.ui.tileset.MekTileset;
import megamek.client.ui.tileset.UnitModelKey;
import megamek.common.Configuration;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Mek;

/** Offline art input. Unit parsing, equipment identity and sprite selection remain owned by MegaMek. */
public final class MekModelCatalog {
    private MekModelCatalog() {
    }

    public record Mount(int index, String name, String internalName, String location, String secondLocation,
          boolean rear, boolean omniPod, String family, double tonnage, int rackSize) {
    }

    /**
     * One catalogued variant.
     *
     * @param hands     the arms that have a hand actuator; an arm without one carries its weapon at the wrist
     * @param lowerArms the arms that have a lower arm actuator; an arm without one carries its weapon at the elbow
     */
    public record Unit(String name, String chassis, String model, String variantKey, String configuration, boolean omni,
          double mass, String sprite, boolean genericSprite, String source, String sourceSha256,
          List<String> hands, List<String> lowerArms, List<Mount> equipment) {
    }

    /** Reads the loose mm-data source tree, including custom Meks; no second MTF or tileset parser. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: MekModelCatalog <mm-data/data> <output.json>");
        }
        Path data = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        Path units = data.resolve("mekfiles/meks");
        if (!Files.isDirectory(units)) {
            throw new IOException("Expected the loose mm-data source tree at " + units);
        }
        Configuration.setDataDir(data.toFile());
        EquipmentType.initializeTypes();
        MekTileset tileset = new MekTileset(data.resolve("images/units").toFile());
        tileset.loadFromFile("mekset.txt");
        List<Unit> catalog = new ArrayList<>();
        List<Map<String, String>> failures = new ArrayList<>();
        try (var paths = Files.walk(units)) {
            for (Path source : paths.filter(Files::isRegularFile)
                  .filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".mtf")).sorted().toList()) {
                String relative = data.relativize(source).toString().replace('\\', '/');
                try {
                    if (!(new MekFileParser(source.toFile()).getEntity() instanceof Mek mek)) {
                        throw new IOException("Not a Mek");
                    }
                    MekTileset.MekEntry entry = tileset.entryFor(mek, -1);
                    if (entry == null) {
                        throw new IOException("No sprite entry");
                    }
                    String sprite = entry.getImageFile().replace('\\', '/');
                    if (!Files.isRegularFile(data.resolve("images/units").resolve(sprite))) {
                        throw new IOException("Missing sprite: " + sprite);
                    }
                    String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                          .digest(Files.readAllBytes(source)));
                    catalog.add(new Unit(mek.getShortNameRaw(), mek.getFullChassis(), mek.getModel(), UnitModelKey.forEntity(mek),
                          mek.getClass().getSimpleName(), mek.isOmni(), mek.getWeight(), sprite,
                          entry == tileset.genericFor(mek, -1), relative, hash,
                          armsWith(mek, Mek.ACTUATOR_HAND), armsWith(mek, Mek.ACTUATOR_LOWER_ARM), mounts(mek)));
                } catch (Exception ex) {
                    failures.add(Map.of("source", relative, "error", String.valueOf(ex.getMessage())));
                }
            }
        }
        catalog.sort(Comparator.comparing(Unit::chassis).thenComparing(Unit::model).thenComparing(Unit::source));
        Files.createDirectories(output.getParent());
        new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
              .writerWithDefaultPrettyPrinter().writeValue(output.toFile(),
              Map.of("schema", 1, "units", catalog, "failures", failures));
        System.out.printf("Exported %d Meks / %d chassis; %d failures to %s%n", catalog.size(),
              catalog.stream().map(Unit::chassis).distinct().count(), failures.size(), output);
        if (!failures.isEmpty()) {
            throw new IOException("Catalog contains failures; inspect its failures array before building assets");
        }
    }

    /** Returns the abbreviations of the arms that have the given actuator, left arm first. */
    static List<String> armsWith(Mek mek, int actuator) {
        List<String> arms = new ArrayList<>();
        for (int location : new int[] { Mek.LOC_LEFT_ARM, Mek.LOC_RIGHT_ARM }) {
            if (mek.hasSystem(actuator, location)) {
                arms.add(mek.getLocationAbbr(location));
            }
        }
        return arms;
    }

    static List<Mount> mounts(Mek mek) {
        return mek.getEquipment().stream().map(mounted -> {
            EquipmentType type = mounted.getType();
            return new Mount(mounted.getEquipmentNum(), type.getName(), type.getInternalName(),
                  location(mek, mounted.getLocation()), location(mek, mounted.getSecondLocation()),
                  mounted.isRearMounted(), mounted.isOmniPodMounted(), family(mounted), mounted.getTonnage(),
                  type instanceof WeaponType weapon ? weapon.getRackSize() : 0);
        }).toList();
    }

    private static String location(Mek mek, int location) {
        return location < 0 ? "" : mek.getLocationAbbr(location);
    }

    /** Broad art families only. Exact dimensions and placement belong to the authored chassis recipe. */
    private static String family(Mounted<?> mounted) {
        EquipmentType type = mounted.getType();
        if (type instanceof WeaponType weapon) {
            if (weapon.hasFlag(WeaponType.F_MGA)) {
                return "internal";
            } else if (weapon.hasFlag(WeaponType.F_MISSILE)) {
                return "missile";
            } else if (weapon.hasFlag(WeaponType.F_PPC)) {
                return "ppc";
            } else if (weapon.hasFlag(WeaponType.F_LASER)) {
                return "laser";
            } else if (weapon.hasFlag(WeaponType.F_MG)) {
                return "machine-gun";
            } else if (weapon.hasFlag(WeaponType.F_BALLISTIC)) {
                return "ballistic";
            } else if (weapon.hasFlag(WeaponType.F_FLAMER)) {
                return "flamer";
            } else if (weapon.hasFlag(WeaponType.F_C3M) || weapon.hasFlag(WeaponType.F_C3MBS)) {
                return "internal";
            } else if (weapon.hasFlag(WeaponType.F_TAG)) {
                return "sensor";
            } else if (weapon.hasFlag(WeaponType.F_PLASMA) || weapon.hasFlag(WeaponType.F_ENERGY)) {
                return "energy";
            }
            return "unmapped-weapon";
        }
        if (type instanceof MiscType misc) {
            if (misc.hasFlag(MiscType.F_JUMP_JET)) {
                return "jump-jet";
            } else if (misc.hasFlag(MiscType.F_CLUB) && misc.hasFlag(MiscTypeFlag.S_HATCHET)) {
                return "hatchet";
            } else if (misc.hasFlag(MiscType.F_CLUB)) {
                return "unmapped-melee";
            }
        }
        return "internal";
    }
}
