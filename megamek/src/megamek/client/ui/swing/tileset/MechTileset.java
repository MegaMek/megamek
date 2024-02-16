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

import megamek.common.*;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Objects;

/**
 * MechTileset is a misleading name, as this matches any unit, not just mechs
 * with the appropriate image. It requires mechset.txt (in the unit images directory), the
 * format of which is explained in that file.
 *
 * @author Ben
 */
public class MechTileset {
    private static final String ULTRA_LIGHT_STRING = "default_ultra_light";
    private static final String LIGHT_STRING = "default_light";
    private static final String MEDIUM_STRING = "default_medium";
    private static final String HEAVY_STRING = "default_heavy";
    private static final String ASSAULT_STRING = "default_assault";
    private static final String SUPER_HEAVY_MECH_STRING = "default_super_heavy_mech";
    private static final String QUAD_STRING = "default_quad";
    private static final String QUADVEE_STRING = "default_quadvee";
    private static final String QUADVEE_VEHICLE_STRING = "default_quadvee_vehicle";
    private static final String LAM_MECH_STRING = "default_lam_mech";
    private static final String LAM_AIRMECH_STRING = "default_lam_airmech";
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
    private static final String UNKNOWN_STRING = "default_unknown";

    private MechEntry default_ultra_light;
    private MechEntry default_light;
    private MechEntry default_medium;
    private MechEntry default_heavy;
    private MechEntry default_assault;
    private MechEntry default_super_heavy_mech;
    private MechEntry default_quad;
    private MechEntry default_quadvee;
    private MechEntry default_quadvee_vehicle;
    private MechEntry default_lam_mech;
    private MechEntry default_lam_airmech;
    private MechEntry default_lam_fighter;
    private MechEntry default_tripod;
    private MechEntry default_tracked;
    private MechEntry default_tracked_heavy;
    private MechEntry default_tracked_assault;
    private MechEntry default_wheeled;
    private MechEntry default_wheeled_heavy;
    private MechEntry default_hover;
    private MechEntry default_naval;
    private MechEntry default_submarine;
    private MechEntry default_hydrofoil;
    private MechEntry default_vtol;
    private MechEntry default_inf;
    private MechEntry default_ba;
    private MechEntry default_proto;
    private MechEntry default_gun_emplacement;
    private MechEntry default_wige;
    private MechEntry default_aero;
    private MechEntry default_small_craft_aero;
    private MechEntry default_small_craft_sphere;
    private MechEntry default_dropship_aero;
    private MechEntry default_dropship_aero_0;
    private MechEntry default_dropship_aero_1;
    private MechEntry default_dropship_aero_2;
    private MechEntry default_dropship_aero_3;
    private MechEntry default_dropship_aero_4;
    private MechEntry default_dropship_aero_5;
    private MechEntry default_dropship_aero_6;
    private MechEntry default_dropship_sphere;
    private MechEntry default_dropship_sphere_0;
    private MechEntry default_dropship_sphere_1;
    private MechEntry default_dropship_sphere_2;
    private MechEntry default_dropship_sphere_3;
    private MechEntry default_dropship_sphere_4;
    private MechEntry default_dropship_sphere_5;
    private MechEntry default_dropship_sphere_6;
    private MechEntry default_jumpship;
    private MechEntry default_warship;
    private MechEntry default_space_station;
    private MechEntry default_fighter_squadron;
    private MechEntry default_tele_missile;
    private MechEntry default_unknown;

    private final HashMap<String, MechEntry> exact = new HashMap<>();
    private final HashMap<String, MechEntry> chassis = new HashMap<>();

    private final File dir;

    /**
     * Creates new MechTileset.
     *
     * @param dir_path Path to the tileset directory.
     */
    public MechTileset(File dir_path) {
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

        MechEntry entry = entryFor(entity, secondaryPos);

        if (entry == null) {
            LogManager.getLogger().warn("Entry is null, please make sure that there is a default entry for "
                    + entity.getShortNameRaw() + " in both mechset.txt and wreckset.txt. Defaulting to "
                    + LIGHT_STRING);
            entry = default_light;
        }

        if (entry.getImage() == null) {
            entry.loadImage();
        }
        return entry.getImage();
    }

    /**
     * Returns the MechEntry corresponding to the entity
     */
    public MechEntry entryFor(Entity entity, int secondaryPos) {
        //Some entities (QuadVees, LAMs) use different sprites depending on mode.
        String mode = entity.getTilesetModeString().toUpperCase();

        String addendum = (secondaryPos == -1) ? "" : "_" + secondaryPos;

        // first, check for exact matches
        if (exact.containsKey(entity.getShortNameRaw().toUpperCase() + mode + addendum)) {
            return exact.get(entity.getShortNameRaw().toUpperCase() + mode + addendum);
        }

        // second, check for chassis matches
        if (chassis.containsKey(entity.getFullChassis().toUpperCase() + mode + addendum)) {
            return chassis.get(entity.getFullChassis().toUpperCase() + mode + addendum);
        }

        // last, the generic model
        return genericFor(entity, secondaryPos);
    }

    public MechEntry genericFor(Entity entity, int secondaryPos) {
        if (entity instanceof BattleArmor) {
            return default_ba;
        } else if (entity instanceof Infantry) {
            return default_inf;
        } else if (entity instanceof Protomech) {
            return default_proto;
        } else if (entity instanceof TripodMech) {
            return default_tripod;
        } else if (entity instanceof QuadVee) {
            return entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE
                    ? default_quadvee_vehicle : default_quadvee;
        } else if (entity instanceof LandAirMech) {
            switch (entity.getConversionMode()) {
                case LandAirMech.CONV_MODE_FIGHTER:
                    return default_lam_fighter;
                case LandAirMech.CONV_MODE_AIRMECH:
                    return default_lam_airmech;
                default:
                    return default_lam_mech;
            }
        } else if (entity instanceof Mech) {
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
                        return default_super_heavy_mech;
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
        }

        return default_unknown;
    }

    public void loadFromFile(String filename) throws IOException {
        Reader r = new BufferedReader(new FileReader(new MegaMekFile(dir, filename).getFile()));
        // read board, looking for "size"
        StreamTokenizer st = new StreamTokenizer(r);
        st.eolIsSignificant(true);
        st.commentChar('#');
        st.quoteChar('"');
        st.wordChars('_', '_');
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            String name;
            String imageName;
            if ((st.ttype == StreamTokenizer.TT_WORD)
                    && st.sval.equalsIgnoreCase("include")) {
                st.nextToken();
                name = st.sval;
                LogManager.getLogger().debug("Loading more unit images from " + name + "...");
                try {
                    loadFromFile(name);
                    LogManager.getLogger().debug("... finished " + name + ".");
                } catch (IOException e) {
                    LogManager.getLogger().debug("... failed: " + e.getMessage() + ".", e);
                }
            } else if ((st.ttype == StreamTokenizer.TT_WORD)
                    && st.sval.equalsIgnoreCase("chassis")) {
                st.nextToken();
                name = st.sval;
                st.nextToken();
                imageName = st.sval;
                // add to list
                chassis.put(name.toUpperCase(), new MechEntry(imageName));
            } else if ((st.ttype == StreamTokenizer.TT_WORD)
                    && st.sval.equalsIgnoreCase("exact")) {
                st.nextToken();
                name = st.sval;
                st.nextToken();
                imageName = st.sval;
                // add to list
                exact.put(name.toUpperCase(), new MechEntry(imageName));
            }
        }
        r.close();

        default_ultra_light = exact.get(ULTRA_LIGHT_STRING.toUpperCase());
        default_light = exact.get(LIGHT_STRING.toUpperCase());
        default_medium = exact.get(MEDIUM_STRING.toUpperCase());
        default_heavy = exact.get(HEAVY_STRING.toUpperCase());
        default_assault = exact.get(ASSAULT_STRING.toUpperCase());
        default_super_heavy_mech = exact.get(SUPER_HEAVY_MECH_STRING.toUpperCase());
        default_quad = exact.get(QUAD_STRING.toUpperCase());
        default_quadvee = exact.get(QUADVEE_STRING.toUpperCase());
        default_quadvee_vehicle = exact.get(QUADVEE_VEHICLE_STRING.toUpperCase());
        default_lam_mech = exact.get(LAM_MECH_STRING.toUpperCase());
        default_lam_airmech = exact.get(LAM_AIRMECH_STRING.toUpperCase());
        default_lam_fighter = exact.get(LAM_FIGHTER_STRING.toUpperCase());
        default_tripod = exact.get(TRIPOD_STRING.toUpperCase());
        default_tracked = exact.get(TRACKED_STRING.toUpperCase());
        default_tracked_heavy = exact.get(TRACKED_HEAVY_STRING.toUpperCase());
        default_tracked_assault = exact.get(TRACKED_ASSAULT_STRING.toUpperCase());
        default_wheeled = exact.get(WHEELED_STRING.toUpperCase());
        default_wheeled_heavy = exact.get(WHEELED_HEAVY_STRING.toUpperCase());
        default_hover = exact.get(HOVER_STRING.toUpperCase());
        default_naval = exact.get(NAVAL_STRING.toUpperCase());
        default_submarine = exact.get(SUBMARINE_STRING.toUpperCase());
        default_hydrofoil = exact.get(HYDROFOIL_STRING.toUpperCase());
        default_vtol = exact.get(VTOL_STRING.toUpperCase());
        default_inf = exact.get(INF_STRING.toUpperCase());
        default_ba = exact.get(BA_STRING.toUpperCase());
        default_proto = exact.get(PROTO_STRING.toUpperCase());
        default_gun_emplacement = exact.get(GUN_EMPLACEMENT_STRING.toUpperCase());
        default_wige = exact.get(WIGE_STRING.toUpperCase());
        default_aero = exact.get(AERO_STRING.toUpperCase());
        default_small_craft_aero = exact.get(SMALL_CRAFT_AERO_STRING.toUpperCase());
        default_dropship_aero = exact.get(DROPSHIP_AERO_STRING.toUpperCase());
        default_dropship_aero_0 = exact.get(DROPSHIP_AERO_STRING_0.toUpperCase());
        default_dropship_aero_1 = exact.get(DROPSHIP_AERO_STRING_1.toUpperCase());
        default_dropship_aero_2 = exact.get(DROPSHIP_AERO_STRING_2.toUpperCase());
        default_dropship_aero_3 = exact.get(DROPSHIP_AERO_STRING_3.toUpperCase());
        default_dropship_aero_4 = exact.get(DROPSHIP_AERO_STRING_4.toUpperCase());
        default_dropship_aero_5 = exact.get(DROPSHIP_AERO_STRING_5.toUpperCase());
        default_dropship_aero_6 = exact.get(DROPSHIP_AERO_STRING_6.toUpperCase());
        default_small_craft_sphere = exact.get(SMALL_CRAFT_SPHERE_STRING.toUpperCase());
        default_dropship_sphere = exact.get(DROPSHIP_SPHERE_STRING.toUpperCase());
        default_dropship_sphere_0 = exact.get(DROPSHIP_SPHERE_STRING_0.toUpperCase());
        default_dropship_sphere_1 = exact.get(DROPSHIP_SPHERE_STRING_1.toUpperCase());
        default_dropship_sphere_2 = exact.get(DROPSHIP_SPHERE_STRING_2.toUpperCase());
        default_dropship_sphere_3 = exact.get(DROPSHIP_SPHERE_STRING_3.toUpperCase());
        default_dropship_sphere_4 = exact.get(DROPSHIP_SPHERE_STRING_4.toUpperCase());
        default_dropship_sphere_5 = exact.get(DROPSHIP_SPHERE_STRING_5.toUpperCase());
        default_dropship_sphere_6 = exact.get(DROPSHIP_SPHERE_STRING_6.toUpperCase());
        default_jumpship = exact.get(JUMPSHIP_STRING.toUpperCase());
        default_warship = exact.get(WARSHIP_STRING.toUpperCase());
        default_space_station = exact.get(SPACE_STATION_STRING.toUpperCase());
        default_fighter_squadron = exact.get(FIGHTER_SQUADRON_STRING.toUpperCase());
        default_tele_missile = exact.get(TELE_MISSILE_STRING.toUpperCase());
        default_unknown = exact.get(UNKNOWN_STRING.toUpperCase());
    }

    /**
     * Stores the name, image file name, and image (once loaded) for a mech or
     * other entity
     */
    public class MechEntry {
        private String imageFile;
        private Image image;

        public MechEntry(String imageFile) {
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
                LogManager.getLogger().warn("Received null image from ImageUtil.loadImageFromFile! File: "
                        + fin);
            }
        }
    }
}
