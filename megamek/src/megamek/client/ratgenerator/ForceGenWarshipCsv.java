/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ratgenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Appends large-craft (WarShip, DropShip, JumpShip, Space Station) results to a small CSV in the logs directory, so the
 * naval structure of a generated force can be verified from machine-readable data instead of scrolling the ToE tree.
 *
 * <p>The file is {@code logs/forcegen_warships.csv}. Each generated force appends one row per large-craft element. The
 * focus is STRUCTURE: the {@code path} column records the full chain of named parent formations (e.g.
 * {@code "Beta Galaxy > Beta Naval Reserve"}), so it is easy to confirm each ship is attached to the right galaxy /
 * naval reserve and that nothing is duplicated. Large craft are absent from {@code forcegen_weights.csv} because they
 * have no L/M/H/A weight class, so this is the only machine-readable record of them.</p>
 *
 * <p>The {@code status} column is {@code EMPTY} when a large-craft slot generated no unit (an empty WarShip Point in
 * the
 * tree) and {@code OK} otherwise - so empty points surface with a grep instead of a visual scan. The file is appended
 * to across runs; delete it to start a fresh batch. Purely diagnostic; no effect on generation.</p>
 */
final class ForceGenWarshipCsv {
    private static final MMLogger LOGGER = MMLogger.create(ForceGenWarshipCsv.class);
    private static final Path FILE = Path.of("logs", "forcegen_warships.csv");
    private static final String HEADER = "faction,year,path,shipName,chassis,model,commander,skill,status";

    private ForceGenWarshipCsv() {}

    /**
     * Walks the whole force tree from {@code root} and appends one row per large-craft element (including empty
     * large-craft slots). Writes the header first if the file does not yet exist. Any I/O failure is logged and
     * swallowed so diagnostics never break generation.
     *
     * @param root the root descriptor of a generated force (Touman, Galaxy, Cluster, or smaller)
     */
    static void append(ForceDescriptor root) {
        if (!PreferenceManager.getClientPreferences().getForceGeneratorDiagnostics()) {
            return;
        }
        if (root == null) {
            return;
        }
        StringBuilder rows = new StringBuilder();
        collect(root, "", rows);
        if (rows.isEmpty()) {
            return;
        }
        try {
            boolean writeHeader = !Files.exists(FILE);
            if (writeHeader && (FILE.getParent() != null)) {
                Files.createDirectories(FILE.getParent());
            }
            String content = writeHeader ? (HEADER + "\n" + rows) : rows.toString();
            Files.writeString(FILE, content, StandardCharsets.UTF_8,
                  StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            LOGGER.warn("Could not append force-gen warship rows to {}: {}", FILE, ex.getMessage());
        }
    }

    private static void collect(ForceDescriptor forceDescriptor, String path, StringBuilder rows) {
        if (forceDescriptor.isElement()) {
            if (isLargeCraft(forceDescriptor)) {
                rows.append(row(forceDescriptor, path)).append("\n");
            }
            return;
        }
        // Extend the structural path with this formation's name (Galaxy, Naval Reserve, etc.) so each
        // ship row carries the full chain of named parents it lives under.
        String name = forceDescriptor.parseName();
        String childPath = ((name != null) && !name.isBlank())
              ? (path.isEmpty() ? name : path + " > " + name)
              : path;
        for (ForceDescriptor sub : forceDescriptor.getSubForces()) {
            collect(sub, childPath, rows);
        }
        for (ForceDescriptor attachedForce : forceDescriptor.getAttached()) {
            collect(attachedForce, childPath, rows);
        }
    }

    private static boolean isLargeCraft(ForceDescriptor forceDescriptor) {
        Entity entity = forceDescriptor.getEntity();
        if (entity != null) {
            return entity.isLargeCraft();
        }
        // No entity generated: treat it as a large-craft slot (an empty point) if the descriptor was
        // asking for one, so empty WarShip points still get logged.
        Integer unitType = forceDescriptor.getUnitType();
        return (unitType != null) && ((unitType == UnitType.WARSHIP) || (unitType == UnitType.DROPSHIP)
              || (unitType == UnitType.JUMPSHIP) || (unitType == UnitType.SPACE_STATION));
    }

    private static String row(ForceDescriptor forceDescriptor, String path) {
        Entity entity = forceDescriptor.getEntity();
        String chassis = (entity != null) ? entity.getChassis() : "";
        String model = (entity != null) ? entity.getModel() : "";
        String commander = "";
        String skill = "";
        if (forceDescriptor.getCo() != null) {
            commander = forceDescriptor.getCo().getName();
            skill = forceDescriptor.getCo().getGunnery() + "/" + forceDescriptor.getCo().getPiloting();
        }
        String status = (entity != null) ? "OK" : "EMPTY";
        return String.join(",",
              safe(forceDescriptor.getFaction()),
              (forceDescriptor.getYear() != null) ? forceDescriptor.getYear().toString() : "-1",
              safe(path),
              safe(forceDescriptor.getFluffName()),
              safe(chassis),
              safe(model),
              safe(commander),
              safe(skill),
              status);
    }

    private static String safe(String value) {
        return (value == null) ? "" : value.replace(',', ' ');
    }
}
