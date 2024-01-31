/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.utilities;

import megamek.common.*;
import megamek.common.loaders.MtfFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is not a functional tool, just some template code to use when changing unit files programmatically.
 * Last used to move the unit roles into the unit files.
 * I leave this in so I don't have to reinvent the wheel.
 */
public class UnitFileMigrationTool {

    /*

    Dragonfly (Viper)
Mad Cat (Timber Wolf)
Ryoken III (Skinwalker)
Kraken (Bane)
Baboon (Howler)
Koshi (Mist Lynx)
Vulture (Mad Dog)
Hellcat (Hellhound II)
Cauldron-Born (Ebon Jaguar)
Puma (Adder)
Mad Cat Mk IV (Savage Wolf)
Roadrunner (Emerald Harrier)
Dasher (Fire Moth)
Ryoken III-XP (Skinwalker)
Goshawk (Vapor Eagle)
Viper (Black Python)
Vixen (Incubus)
#####################Pariah (Septicemia)
Black Hawk (Nova)
Fenris (Ice Ferret)
Nobori-nin (Huntsman)
Thor (Summoner)
####################Snow Fox (Omni)
Gladiator (Executioner)
Hellhound (Conjurer)
Galahad (Glass Spider)
Behemoth (Stone Rhino)
Peregrine (Horned Owl)
Star Adder (Blood Asp)
Ryoken (Stormcrow)
Loki Mk II (Hel)
Hellhound II-P (Hellcat-P)
Daishi (Dire Wolf)
#####################Black Hawk (Standard)
Butcherbird (Ion Sparrow)
Hankyu (Arctic Cheetah)
Masakari (Warhawk)
Vulture Mk IV (Mad Dog Mk IV)
Uller (Kit Fox)
####################Koshi (Standard)
Vulture Mk III (Mad Dog Mk III)
Thor II (Grand Summoner)
Grendel (Mongrel)
Gladiator-B (Executioner-B)
Man O' War (Gargoyle)
Loki (Hellbringer)


     */
    public static void main(String... args) throws IOException {
        MechSummaryCache cache = MechSummaryCache.getInstance(true);
        MechSummary[] units = cache.getAllMechs();

        for (MechSummary unit : units) {
            if (!unit.isClan() || !unit.isMek() || unit.getFullChassis().contains("Standard")
                    ||unit.getFullChassis().contains("Pariah")||unit.getFullChassis().contains("Omni")) {
                continue;
            }
            File file = unit.getSourceFile();
            if (file.toString().toLowerCase().endsWith(".mtf")) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                int chassisLine = -1;
                for (String line : lines) {
                    if (line.startsWith("chassis:") && line.contains("(")) {
                        chassisLine = lines.indexOf(line);
                    }
                }
                if (chassisLine > -1) {
                    int bracket = unit.getFullChassis().indexOf("(");
                    String chassis = unit.getFullChassis().substring(0, bracket).trim();
                    String clanchassis = unit.getFullChassis().substring(bracket)
                            .replace(")", "")
                            .replace("(", "")
                            .trim();
                    lines.add(chassisLine, "chassis:" + chassis);
                    lines.remove(chassisLine+1);
                    lines.add(chassisLine + 1, "clanname:" + clanchassis);
                    Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
                }
            }
        }
    }
}