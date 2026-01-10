/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.containers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

import static megamek.common.equipment.AmmoType.INCENDIARY_MOD;

public class MunitionTree {
    private static final MMLogger LOGGER = MMLogger.create(MunitionTree.class);

    // Validated munition names that will work in ADF files.
    // All LRM munitions can be combined with Incendiary to add some fire-starting and
    // anti-infantry/BA damage, at the cost of regular damage.  But let's create those
    // options programmatically instead of by hand.
    // We do, however, need a single overall "Incendiary" entry to track computed overall weight.
    public static final List<String> LRM_MUNITION_NAMES =
          addIncendiary(new ArrayList<>(List.of(
                "Dead-Fire",
                "Standard",
                "Swarm-I",
                "Swarm",
                "Heat-Seeking",
                "Semi-Guided",
                "Artemis-capable",
                "Narc-capable",
                "Follow The Leader",
                "Fragmentation",
                "Thunder",
                "Thunder-Active",
                "Thunder-Augmented",
                "Thunder-Vibrabomb",
                "Thunder-Inferno",
                "Anti-TSM",
                "Listen-Kill",
                "Smoke",
                "Mine Clearance",
                "Anti-Radiation",
                "Incendiary"
          )));

    private static List<String> addIncendiary(ArrayList<String> strings) {
        ArrayList<String> newStrings = new ArrayList<>();

        for (String s : strings) {
            if (!s.toLowerCase().contains("incendiary")) {
                newStrings.add(s + " " + INCENDIARY_MOD);
            }
        }
        strings.addAll(newStrings);
        return strings;
    }

    public static final List<String> SRM_MUNITION_NAMES = new ArrayList<>(List.of("Dead-Fire",
          "Standard",
          "Tandem-Charge",
          "Inferno",
          "Heat-Seeking",
          "Artemis-capable",
          "Narc-capable",
          "Fragmentation",
          "Acid",
          "Anti-TSM",
          "Listen-Kill",
          "Mine Clearance",
          "Smoke",
          "Anti-Radiation"));

    public static final List<String> AC_MUNITION_NAMES = new ArrayList<>(List.of("Precision",
          "Standard",
          "Armor-Piercing",
          "Caseless",
          "Flak",
          "Tracer",
          "Flechette",
          "Armor-Piercing Playtest",
          "Precision Playtest"));

    public static final List<String> ATM_MUNITION_NAMES = new ArrayList<>(List.of("HE", "ER", "Standard"));
    public static final List<String> iATM_MUNITION_NAMES = new ArrayList<>(List.of("HE", "ER", "Standard", "IIW",
          "IMP"));

    public static final List<String> ARROW_MUNITION_NAMES = new ArrayList<>(List.of("Fuel-Air",
          "Standard",
          "ADA",
          "Cluster",
          "Inferno-IV",
          "Homing",
          "Thunder",
          "Thunder Vibrabomb-IV",
          "Illumination",
          "Smoke",
          "Laser Inhibiting",
          "Davy Crockett-M"));

    public static final List<String> ARTILLERY_MUNITION_NAMES = new ArrayList<>(List.of("Fuel-Air",
          "Standard",
          "Cluster",
          "Copperhead",
          "FASCAM",
          "Flechette",
          "Illumination",
          "Smoke",
          "Davy Crockett-M"));

    public static final List<String> ARTILLERY_CANNON_MUNITION_NAMES = new ArrayList<>(List.of("Fuel-Air", "Standard"));

    public static final List<String> MORTAR_MUNITION_NAMES = new ArrayList<>(List.of("SC",
          "SG",
          "AP",
          "AB",
          "FL",
          "SM"));

    public static final List<String> NARC_MUNITION_NAMES = new ArrayList<>(List.of("Narc Explosive", "Standard"));

    // Shorter, guaranteed to work in lookups
    public static final List<String> BOMB_MUNITION_NAMES = Arrays.stream(BombTypeEnum.values())
          .filter(type -> type != BombTypeEnum.NONE)
          .map(BombTypeEnum::getInternalName)
          .collect(Collectors.toList());

    private static final String HEADER = String.join(System.lineSeparator(),
          "# ADF (AutoConfiguration Data File) from MegaMek.",
          "# Lines are formatted as",
          "#      '<Chassis>:<Model>:<Pilot>::<Weapon type>:Munition1[:Munition2[:...]]][::AmmoType2...]'",
          "# Values for <Chassis>, <Model>, <Pilot>, and <Weapon Type> may be 'any', or actual values.",
          "# Values for <Weapon Type> may also be specific or general, e.g. 'AC/20' ~ 'AC', 'SRM6' ~ 'SRM'",
          "# e.g. 'Shadow Hawk:any:Grayson Carlyle::LRM:Swarm::SRM:Inferno::AC:Precision:Flak'.",
          "# ",
          "# Left-most Munition is highest priority; if any ammo slots are unaccounted for, they will be filled",
          "# with this munition type (unless it is invalid for the time / faction, in which case the next valid",
          "# Munition type will be used).  If no imperative matches, the current munitions will be left in place.",
          "# An 'any:any:any::...' directive will be applied to any and all units, but is superseded by more",
          "# particular entries.  If no match is found at the <Pilot> level, the 'any' entry at that level will",
          "# be tried first, and onwards up until 'any:any:any' (if defined).",
          "######################################################################################################");

    private LoadNode root = new LoadNode();

    public MunitionTree() {
    }

    public MunitionTree(MunitionTree mt) {
        this.root = new LoadNode(mt.root);
    }

    /**
     * Constructor for reading in files containing load out imperatives.
     *
     * @param fName file name
     */
    public MunitionTree(String fName) throws IllegalArgumentException {
        File fd = new File(fName);
        if (fd.canRead() && fd.exists()) {
            try (FileReader fr = new FileReader(fName)) {
                if (fd.getAbsoluteFile().toString().toLowerCase().endsWith("adf")) {
                    readFromADF(new BufferedReader(fr));
                } else {
                    throw new IllegalArgumentException("Invalid filename: " + fName);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void readFromADF(BufferedReader br) throws IOException {
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            // Ignore comments
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }

            try {
                String[] parts = line.split("::");
                String[] keys = parts[0].split(":");
                HashMap<String, String> imperatives = new HashMap<>();
                String imperative;

                for (int idx = 1; idx < parts.length; idx++) {
                    // Populate imperatives by splitting at first ":" instance
                    imperative = parts[idx];
                    imperatives.put(imperative.substring(0, imperative.indexOf(':')),
                          imperative.substring(imperative.indexOf(':') + 1));
                }
                insertImperatives(keys[0], keys[1], keys[2], imperatives);
            } catch (IndexOutOfBoundsException e) {
                LOGGER.error("Failed to read an imperative!", e);
            }
        }
    }

    public void writeToADFFilename(String fName) {
        File fd = new File(fName);
        if (!fd.exists()) {
            try {
                fd.createNewFile();
            } catch (IOException e) {
                LOGGER.error("Failed to create new file: {}", fName);
                return;
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fName))) {
            writeToADFFormat(bw);
        } catch (FileNotFoundException ignored) {
            LOGGER.error("Unable to find file {} to write to", fName);
        } catch (IOException ignored) {
            LOGGER.error("Failed to write file: {}", fName);
        }
    }

    public void writeToADFFormat(BufferedWriter bw) throws IOException {
        bw.write(HEADER);
        bw.write(System.lineSeparator());
        bw.write(System.lineSeparator());
        bw.flush();
        bw.write(root.dumpTextFormat().toString());
        bw.flush();
    }

    /**
     * Convert List of Entities into a set of specific imperatives for each unit. Used for backing up original load
     * out.
     *
     * @param el {@link Entity} list
     */
    public void loadEntityList(List<Entity> el) {
        for (Entity e : el) {
            HashMap<String, String> imperatives = new HashMap<>();
            for (Mounted<?> m : e.getAmmo()) {
                AmmoType aType = (AmmoType) m.getType();
                String baseName = aType.getBaseName();
                String munition = (aType.getSubMunitionName().equals(baseName)) ?
                      "Standard" :
                      aType.getSubMunitionName();
                if (!(imperatives.containsKey(baseName))) {
                    imperatives.put(baseName, munition);
                } else {
                    imperatives.put(baseName, imperatives.get(baseName) + ':' + munition);
                }
            }

            root.insert(imperatives, e.getFullChassis(), e.getModel(), e.getCrew().getName(0));
        }
    }

    // Can take multiple separate ammoType strings (e.g. "Standard", "HE", "ER") or one pre-defined imperative set in
    // priority order ("Standard:HE:ER")
    public void insertImperative(String chassis, String variant, String pilot, String binType, String... ammoTypes)
          throws IllegalArgumentException {

        // Need ammoTypes populated
        if (ammoTypes.length == 0) {
            throw new IllegalArgumentException("Must include at least one munition type (e.g. 'Standard'");
        }

        HashMap<String, String> imperatives = new HashMap<>();

        imperatives.put(binType, String.join(":", ammoTypes));
        insertImperatives(chassis, variant, pilot, imperatives);
    }

    public void insertImperatives(String chassis, String variant, String pilot, HashMap<String, String> imperatives) {

        // Start insertions from root
        root.insert(imperatives, chassis, variant, pilot);
    }

    public void insertMangledImperatives(String chassis, String variant, String pilot,
          HashMap<String, String> imperatives) {
        // switch imperative keys to lowercase to avoid case-based matching issues strip out extraneous characters
        // for ammo with sizes, e.g. LRM[ -/]15 -> LRM15
        HashMap<String, String> lcImp = new HashMap<>(imperatives.size());
        for (Map.Entry<String, String> e : imperatives.entrySet()) {
            lcImp.put(e.getKey().toLowerCase().replaceAll(LoadNode.SIZE_REGEX, ""), e.getValue());
        }

        // Start insertions from root
        root.insert(lcImp, chassis, variant, pilot);
    }

    public HashMap<String, Integer> getCountsOfAmmunitionForKey(String chassis, String variant, String pilot,
          String binType) {
        return root.retrieveAmmoCounts(chassis, variant, pilot, binType);
    }

    public List<String> getPriorityList(String chassis, String variant, String pilot, String binType) {
        return root.retrievePriorityList(chassis, variant, pilot, binType);
    }

    /**
     * @return entire imperative string that would act on the provided key set
     */
    public String getEffectiveImperative(String chassis, String variant, String pilot, String binType) {
        LoadNode node = root.retrieve(chassis, variant, pilot);
        if (null != node) {
            return node.getImperative(binType).get(1);
        }
        return "";
    }

    /**
     * Return the actual, or effective, desired count of ammo bins for the given binType and ammoType
     *
     * @return int count of bins requested for this binType:ammoType set.
     */
    public int getCountOfAmmoForKey(String chassis, String variant, String pilot, String binType, String ammoType) {
        return root.retrieveAmmoCount(chassis, variant, pilot, binType, ammoType);
    }
}

