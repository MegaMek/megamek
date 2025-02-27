/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.tileset;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import megamek.common.*;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.util.fileUtils.StandardTextfileStreamTokenizer;
import megamek.logging.MMLogger;

/**
 * MekTileset is a misleading name, as this matches any unit, not just Meks
 * with the appropriate image. It requires mekset.txt (in the unit images
 * directory), the
 * format of which is explained in that file.
 *
 * @author Ben
 */
public class MekTileset {
    private static final MMLogger logger = MMLogger.create(MekTileset.class);

    public static final String CHASSIS_KEY = "chassis";
    public static final String MODEL_KEY = "exact";

    private static final String ULTRA_LIGHT_STRING = "default_ultra_light";
    private static final String LIGHT_STRING = "default_light";
    private static final String MEDIUM_STRING = "default_medium";
    private static final String HEAVY_STRING = "default_heavy";
    private static final String ASSAULT_STRING = "default_assault";
    private static final String SUPER_HEAVY_MEK_STRING = "default_super_heavy_mek";
    private static final String QUAD_STRING = "default_quad";
    private static final String QUADVEE_STRING = "default_quadvee";
    private static final String QUADVEE_VEHICLE_STRING = "default_quadvee_vehicle";
    private static final String LAM_MEK_STRING = "default_lam_mek";
    private static final String LAM_AIRMEK_STRING = "default_lam_airmek";
    private static final String LAM_FIGHTER_STRING = "default_lam_fighter";
    private static final String TRIPOD_STRING = "default_tripod";
    private static final String TRACKED_STRING = "default_tracked";
    private static final String TRACKED_HEAVY_STRING = "default_tracked_heavy";
    private static final String TRACKED_ASSAULT_STRING = "default_tracked_assault";
    private static final String WHEELED_STRING = "default_wheeled";
    private static final String WHEELED_HEAVY_STRING = "default_wheeled_heavy";
    private static final String HOVER_STRING = "default_hover";
    private static final String NAVAL_STRING = "default_naval";
    private static final String SUBMARINE_STRING = "default_submarine";
    private static final String HYDROFOIL_STRING = "default_hydrofoil";
    private static final String VTOL_STRING = "default_vtol";
    private static final String INF_STRING = "default_infantry";
    private static final String BA_STRING = "default_ba";
    private static final String PROTO_STRING = "default_proto";
    private static final String GUN_EMPLACEMENT_STRING = "default_gun_emplacement";
    private static final String WIGE_STRING = "default_wige";
    private static final String AERO_STRING = "default_aero";
    private static final String SMALL_CRAFT_AERO_STRING = "default_small_craft_aero";
    private static final String SMALL_CRAFT_SPHERE_STRING = "default_small_craft_sphere";
    private static final String DROPSHIP_AERO_STRING = "default_dropship_aero";
    private static final String DROPSHIP_AERO_STRING_0 = "default_dropship_aero_0";
    private static final String DROPSHIP_AERO_STRING_1 = "default_dropship_aero_1";
    private static final String DROPSHIP_AERO_STRING_2 = "default_dropship_aero_2";
    private static final String DROPSHIP_AERO_STRING_3 = "default_dropship_aero_3";
    private static final String DROPSHIP_AERO_STRING_4 = "default_dropship_aero_4";
    private static final String DROPSHIP_AERO_STRING_5 = "default_dropship_aero_5";
    private static final String DROPSHIP_AERO_STRING_6 = "default_dropship_aero_6";
    private static final String DROPSHIP_SPHERE_STRING = "default_dropship_sphere";
    private static final String DROPSHIP_SPHERE_STRING_0 = "default_dropship_sphere_0";
    private static final String DROPSHIP_SPHERE_STRING_1 = "default_dropship_sphere_1";
    private static final String DROPSHIP_SPHERE_STRING_2 = "default_dropship_sphere_2";
    private static final String DROPSHIP_SPHERE_STRING_3 = "default_dropship_sphere_3";
    private static final String DROPSHIP_SPHERE_STRING_4 = "default_dropship_sphere_4";
    private static final String DROPSHIP_SPHERE_STRING_5 = "default_dropship_sphere_5";
    private static final String DROPSHIP_SPHERE_STRING_6 = "default_dropship_sphere_6";
    private static final String JUMPSHIP_STRING = "default_jumpship";
    private static final String WARSHIP_STRING = "default_warship";
    private static final String SPACE_STATION_STRING = "default_space_station";
    private static final String FIGHTER_SQUADRON_STRING = "default_fighter_squadron";
    private static final String TELE_MISSILE_STRING = "default_tele_missile";
    private static final String HANDHELD_WEAPON_STRING = "default_hhw";
    private static final String UNKNOWN_STRING = "default_unknown";

    private MekEntry default_ultra_light;
    private MekEntry default_light;
    private MekEntry default_medium;
    private MekEntry default_heavy;
    private MekEntry default_assault;
    private MekEntry default_super_heavy_mek;
    private MekEntry default_quad;
    private MekEntry default_quadvee;
    private MekEntry default_quadvee_vehicle;
    private MekEntry default_lam_mek;
    private MekEntry default_lam_airmek;
    private MekEntry default_lam_fighter;
    private MekEntry default_tripod;
    private MekEntry default_tracked;
    private MekEntry default_tracked_heavy;
    private MekEntry default_tracked_assault;
    private MekEntry default_wheeled;
    private MekEntry default_wheeled_heavy;
    private MekEntry default_hover;
    private MekEntry default_naval;
    private MekEntry default_submarine;
    private MekEntry default_hydrofoil;
    private MekEntry default_vtol;
    private MekEntry default_inf;
    private MekEntry default_ba;
    private MekEntry default_proto;
    private MekEntry default_gun_emplacement;
    private MekEntry default_wige;
    private MekEntry default_aero;
    private MekEntry default_small_craft_aero;
    private MekEntry default_small_craft_sphere;
    private MekEntry default_dropship_aero;
    private MekEntry default_dropship_aero_0;
    private MekEntry default_dropship_aero_1;
    private MekEntry default_dropship_aero_2;
    private MekEntry default_dropship_aero_3;
    private MekEntry default_dropship_aero_4;
    private MekEntry default_dropship_aero_5;
    private MekEntry default_dropship_aero_6;
    private MekEntry default_dropship_sphere;
    private MekEntry default_dropship_sphere_0;
    private MekEntry default_dropship_sphere_1;
    private MekEntry default_dropship_sphere_2;
    private MekEntry default_dropship_sphere_3;
    private MekEntry default_dropship_sphere_4;
    private MekEntry default_dropship_sphere_5;
    private MekEntry default_dropship_sphere_6;
    private MekEntry default_jumpship;
    private MekEntry default_warship;
    private MekEntry default_space_station;
    private MekEntry default_fighter_squadron;
    private MekEntry default_tele_missile;
    private MekEntry default_handheld_weapon;
    private MekEntry default_unknown;

    private final HashMap<String, MekEntry> exact = new HashMap<>();
    private final HashMap<String, MekEntry> chassis = new HashMap<>();

    private final File dir;

    /**
     * Creates new MekTileset.
     *
     * @param dir_path Path to the tileset directory.
     */
    public MekTileset(File dir_path) {
        Objects.requireNonNull(dir_path, "Must provide dir_path");
        dir = dir_path;
    }

    public Image imageFor(Entity entity) {
        return imageFor(entity, -1);
    }

    public Image imageFor(Entity entity, int secondaryPos) {
        // Return the embedded icon, if the unit has one and this is the one-hex icon
        if ((secondaryPos == -1) && entity.hasEmbeddedIcon()) {
            return entity.getIcon();
        }

        MekEntry entry = entryFor(entity, secondaryPos);

        if (entry == null) {
            logger.warn("Entry is null, please make sure that there is a default entry for "
                    + entity.getShortNameRaw() + " in both mekset.txt and wreckset.txt. Defaulting to "
                    + LIGHT_STRING);
            entry = default_light;
        }

        if (entry.getImage() == null) {
            entry.loadImage();
        }
        return entry.getImage();
    }

    /**
     * Returns the MekEntry corresponding to the entity
     */
    public MekEntry entryFor(Entity entity, int secondaryPos) {
        // Some entities (QuadVees, LAMs) use different sprites depending on mode.
        String mode = entity.getTilesetModeString().toUpperCase(Locale.ROOT);
        String addendum = (secondaryPos == -1) ? "" : "_" + secondaryPos;

        // first, check for exact matches
        if (exact.containsKey(entity.getShortNameRaw().toUpperCase(Locale.ROOT) + mode + addendum)) {
            return exact.get(entity.getShortNameRaw().toUpperCase(Locale.ROOT) + mode + addendum);
        }

        // second, check for chassis matches
        if (chassis.containsKey(entity.getFullChassis().toUpperCase(Locale.ROOT) + mode + addendum)) {
            return chassis.get(entity.getFullChassis().toUpperCase(Locale.ROOT) + mode + addendum);
        }

        // last, the generic model
        return genericFor(entity, secondaryPos);
    }

    public MekEntry genericFor(Entity entity, int secondaryPos) {
        if (entity instanceof BattleArmor) {
            return default_ba;
        } else if (entity instanceof Infantry) {
            return default_inf;
        } else if (entity instanceof ProtoMek) {
            return default_proto;
        } else if (entity instanceof TripodMek) {
            return default_tripod;
        } else if (entity instanceof QuadVee) {
            return entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE
                    ? default_quadvee_vehicle
                    : default_quadvee;
        } else if (entity instanceof LandAirMek) {
            switch (entity.getConversionMode()) {
                case LandAirMek.CONV_MODE_FIGHTER:
                    return default_lam_fighter;
                case LandAirMek.CONV_MODE_AIRMEK:
                    return default_lam_airmek;
                default:
                    return default_lam_mek;
            }
        } else if (entity instanceof Mek) {
            if (entity.getMovementMode() == EntityMovementMode.QUAD) {
                return default_quad;
            } else {
                switch (entity.getWeightClass()) {
                    case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                        return default_ultra_light;
                    case EntityWeightClass.WEIGHT_LIGHT:
                        return default_light;
                    case EntityWeightClass.WEIGHT_MEDIUM:
                        return default_medium;
                    case EntityWeightClass.WEIGHT_HEAVY:
                        return default_heavy;
                    case EntityWeightClass.WEIGHT_SUPER_HEAVY:
                        return default_super_heavy_mek;
                    case EntityWeightClass.WEIGHT_ASSAULT:
                    default:
                        return default_assault;
                }
            }
        } else if (entity.getMovementMode() == EntityMovementMode.NAVAL) {
            return default_naval;
        } else if (entity.getMovementMode() == EntityMovementMode.SUBMARINE) {
            return default_submarine;
        } else if (entity.getMovementMode() == EntityMovementMode.HYDROFOIL) {
            return default_hydrofoil;
        } else if (entity instanceof GunEmplacement) {
            return default_gun_emplacement;
        } else if (entity instanceof Tank) {
            switch (entity.getMovementMode()) {
                case WHEELED:
                    if (entity.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                        return default_wheeled_heavy;
                    } else {
                        return default_wheeled;
                    }
                case HOVER:
                    return default_hover;
                case VTOL:
                    return default_vtol;
                case WIGE:
                    return default_wige;
                case TRACKED:
                default:
                    switch (entity.getWeightClass()) {
                        case EntityWeightClass.WEIGHT_HEAVY:
                            return default_tracked_heavy;
                        case EntityWeightClass.WEIGHT_ASSAULT:
                            return default_tracked_assault;
                        default:
                            return default_tracked;
                    }
            }
        } else if (entity instanceof Aero) {
            if (entity instanceof SpaceStation) {
                return default_space_station;
            } else if (entity instanceof Warship) {
                return default_warship;
            } else if (entity instanceof Jumpship) {
                return default_jumpship;
            } else if (entity instanceof Dropship) {
                Dropship ds = (Dropship) entity;
                if (ds.isSpheroid()) {
                    switch (secondaryPos) {
                        case 0:
                            return default_dropship_sphere_0;
                        case 1:
                            return default_dropship_sphere_1;
                        case 2:
                            return default_dropship_sphere_2;
                        case 3:
                            return default_dropship_sphere_3;
                        case 4:
                            return default_dropship_sphere_4;
                        case 5:
                            return default_dropship_sphere_5;
                        case 6:
                            return default_dropship_sphere_6;
                        case -1:
                        default:
                            return default_dropship_sphere;
                    }
                } else {
                    switch (secondaryPos) {
                        case 0:
                            return default_dropship_aero_0;
                        case 1:
                            return default_dropship_aero_1;
                        case 2:
                            return default_dropship_aero_2;
                        case 3:
                            return default_dropship_aero_3;
                        case 4:
                            return default_dropship_aero_4;
                        case 5:
                            return default_dropship_aero_5;
                        case 6:
                            return default_dropship_aero_6;
                        case -1:
                        default:
                            return default_dropship_aero;
                    }
                }
            } else if (entity instanceof FighterSquadron) {
                return default_fighter_squadron;
            } else if (entity instanceof SmallCraft) {
                SmallCraft sc = (SmallCraft) entity;
                if (sc.isSpheroid()) {
                    return default_small_craft_sphere;
                } else {
                    return default_small_craft_aero;
                }
            } else if (entity instanceof TeleMissile) {
                return default_tele_missile;
            } else {
                return default_aero;
            }
        } else if (entity instanceof HandheldWeapon) {
            return default_handheld_weapon;
        }

        return default_unknown;
    }

    public void loadFromFile(String filename) throws IOException {
        logger.info("Loading unit icons from {}", filename);
        try (Reader r = new BufferedReader(new FileReader(new MegaMekFile(dir, filename).getFile(), StandardCharsets.UTF_8))) {
            StandardTextfileStreamTokenizer tokenizer = new StandardTextfileStreamTokenizer(r);
            while (true) {
                List<String> tokens = tokenizer.getLineTokens();
                if (tokenizer.isFinished()) {
                    break;
                } else if (MekSetTest.isValidLine(tokens)) {
                    if (StandardTextfileStreamTokenizer.isValidIncludeLine(tokens)) {
                        try {
                            loadFromFile(tokens.get(1));
                        } catch (IOException e) {
                            logger.error("... failed: {}.", e.getMessage(), e);
                        }
                    } else if (tokens.get(0).equals(CHASSIS_KEY)) {
                        chassis.put(tokens.get(1).toUpperCase(Locale.ROOT), new MekEntry(tokens.get(2)));
                    } else {
                        exact.put(tokens.get(1).toUpperCase(Locale.ROOT), new MekEntry(tokens.get(2)));
                    }
                } else {
                    logger.warn("Malformed line in {}: {}", filename, tokens.toString());
                }
            }
        }

        default_ultra_light = exact.get(ULTRA_LIGHT_STRING.toUpperCase(Locale.ROOT));
        default_light = exact.get(LIGHT_STRING.toUpperCase(Locale.ROOT));
        default_medium = exact.get(MEDIUM_STRING.toUpperCase(Locale.ROOT));
        default_heavy = exact.get(HEAVY_STRING.toUpperCase(Locale.ROOT));
        default_assault = exact.get(ASSAULT_STRING.toUpperCase(Locale.ROOT));
        default_super_heavy_mek = exact.get(SUPER_HEAVY_MEK_STRING.toUpperCase(Locale.ROOT));
        default_quad = exact.get(QUAD_STRING.toUpperCase(Locale.ROOT));
        default_quadvee = exact.get(QUADVEE_STRING.toUpperCase(Locale.ROOT));
        default_quadvee_vehicle = exact.get(QUADVEE_VEHICLE_STRING.toUpperCase(Locale.ROOT));
        default_lam_mek = exact.get(LAM_MEK_STRING.toUpperCase(Locale.ROOT));
        default_lam_airmek = exact.get(LAM_AIRMEK_STRING.toUpperCase(Locale.ROOT));
        default_lam_fighter = exact.get(LAM_FIGHTER_STRING.toUpperCase(Locale.ROOT));
        default_tripod = exact.get(TRIPOD_STRING.toUpperCase(Locale.ROOT));
        default_tracked = exact.get(TRACKED_STRING.toUpperCase(Locale.ROOT));
        default_tracked_heavy = exact.get(TRACKED_HEAVY_STRING.toUpperCase(Locale.ROOT));
        default_tracked_assault = exact.get(TRACKED_ASSAULT_STRING.toUpperCase(Locale.ROOT));
        default_wheeled = exact.get(WHEELED_STRING.toUpperCase(Locale.ROOT));
        default_wheeled_heavy = exact.get(WHEELED_HEAVY_STRING.toUpperCase(Locale.ROOT));
        default_hover = exact.get(HOVER_STRING.toUpperCase(Locale.ROOT));
        default_naval = exact.get(NAVAL_STRING.toUpperCase(Locale.ROOT));
        default_submarine = exact.get(SUBMARINE_STRING.toUpperCase(Locale.ROOT));
        default_hydrofoil = exact.get(HYDROFOIL_STRING.toUpperCase(Locale.ROOT));
        default_vtol = exact.get(VTOL_STRING.toUpperCase(Locale.ROOT));
        default_inf = exact.get(INF_STRING.toUpperCase(Locale.ROOT));
        default_ba = exact.get(BA_STRING.toUpperCase(Locale.ROOT));
        default_proto = exact.get(PROTO_STRING.toUpperCase(Locale.ROOT));
        default_gun_emplacement = exact.get(GUN_EMPLACEMENT_STRING.toUpperCase(Locale.ROOT));
        default_wige = exact.get(WIGE_STRING.toUpperCase(Locale.ROOT));
        default_aero = exact.get(AERO_STRING.toUpperCase(Locale.ROOT));
        default_small_craft_aero = exact.get(SMALL_CRAFT_AERO_STRING.toUpperCase(Locale.ROOT));
        default_dropship_aero = exact.get(DROPSHIP_AERO_STRING.toUpperCase(Locale.ROOT));
        default_dropship_aero_0 = exact.get(DROPSHIP_AERO_STRING_0.toUpperCase(Locale.ROOT));
        default_dropship_aero_1 = exact.get(DROPSHIP_AERO_STRING_1.toUpperCase(Locale.ROOT));
        default_dropship_aero_2 = exact.get(DROPSHIP_AERO_STRING_2.toUpperCase(Locale.ROOT));
        default_dropship_aero_3 = exact.get(DROPSHIP_AERO_STRING_3.toUpperCase(Locale.ROOT));
        default_dropship_aero_4 = exact.get(DROPSHIP_AERO_STRING_4.toUpperCase(Locale.ROOT));
        default_dropship_aero_5 = exact.get(DROPSHIP_AERO_STRING_5.toUpperCase(Locale.ROOT));
        default_dropship_aero_6 = exact.get(DROPSHIP_AERO_STRING_6.toUpperCase(Locale.ROOT));
        default_small_craft_sphere = exact.get(SMALL_CRAFT_SPHERE_STRING.toUpperCase(Locale.ROOT));
        default_dropship_sphere = exact.get(DROPSHIP_SPHERE_STRING.toUpperCase(Locale.ROOT));
        default_dropship_sphere_0 = exact.get(DROPSHIP_SPHERE_STRING_0.toUpperCase(Locale.ROOT));
        default_dropship_sphere_1 = exact.get(DROPSHIP_SPHERE_STRING_1.toUpperCase(Locale.ROOT));
        default_dropship_sphere_2 = exact.get(DROPSHIP_SPHERE_STRING_2.toUpperCase(Locale.ROOT));
        default_dropship_sphere_3 = exact.get(DROPSHIP_SPHERE_STRING_3.toUpperCase(Locale.ROOT));
        default_dropship_sphere_4 = exact.get(DROPSHIP_SPHERE_STRING_4.toUpperCase(Locale.ROOT));
        default_dropship_sphere_5 = exact.get(DROPSHIP_SPHERE_STRING_5.toUpperCase(Locale.ROOT));
        default_dropship_sphere_6 = exact.get(DROPSHIP_SPHERE_STRING_6.toUpperCase(Locale.ROOT));
        default_jumpship = exact.get(JUMPSHIP_STRING.toUpperCase(Locale.ROOT));
        default_warship = exact.get(WARSHIP_STRING.toUpperCase(Locale.ROOT));
        default_space_station = exact.get(SPACE_STATION_STRING.toUpperCase(Locale.ROOT));
        default_fighter_squadron = exact.get(FIGHTER_SQUADRON_STRING.toUpperCase(Locale.ROOT));
        default_tele_missile = exact.get(TELE_MISSILE_STRING.toUpperCase(Locale.ROOT));
        default_handheld_weapon = exact.get(HANDHELD_WEAPON_STRING.toUpperCase(Locale.ROOT));
        default_unknown = exact.get(UNKNOWN_STRING.toUpperCase(Locale.ROOT));
    }

    boolean hasOnlyChassisMatch(Entity entity) {
        return !exact.containsKey(entity.getShortNameRaw().toUpperCase(Locale.ROOT))
                && chassis.containsKey(entity.getShortNameRaw().toUpperCase(Locale.ROOT));
    }

    /**
     * Stores the name, image file name, and image (once loaded) for a mek or
     * other entity
     */
    public class MekEntry {
        private String imageFile;
        private Image image;

        public MekEntry(String imageFile) {
            this.imageFile = imageFile;
            image = null;
        }

        public Image getImage() {
            return image;
        }

        public void loadImage() {
            File fin = new MegaMekFile(dir, imageFile).getFile();
            image = ImageUtil.loadImageFromFile(fin.toString());
            if (image == null) {
                logger.warn("Received null image from ImageUtil.loadImageFromFile! File: %s", fin);
            }
        }
    }
}
