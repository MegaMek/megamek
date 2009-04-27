/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * MechTileset.java
 *
 * Created on April 15, 2002, 9:53 PM
 */

package megamek.client.ui.swing;

import java.awt.Component;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.HashMap;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.FighterSquadron;
import megamek.common.GunEmplacement;
import megamek.common.IEntityMovementMode;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.SpaceStation;
import megamek.common.Tank;
import megamek.common.Warship;

/**
 * MechTileset is a misleading name, as this matches any unit, not just mechs
 * with the appropriate image. It requires data/images/units/mechset.txt, the
 * format of which is explained in that file.
 * 
 * @author Ben
 */
public class MechTileset {
    private String LIGHT_STRING = "default_light"; //$NON-NLS-1$
    private String MEDIUM_STRING = "default_medium"; //$NON-NLS-1$
    private String HEAVY_STRING = "default_heavy"; //$NON-NLS-1$
    private String ASSAULT_STRING = "default_assault"; //$NON-NLS-1$
    private String QUAD_STRING = "default_quad"; //$NON-NLS-1$
    private String TRACKED_STRING = "default_tracked"; //$NON-NLS-1$
    private String TRACKED_HEAVY_STRING = "default_tracked_heavy"; //$NON-NLS-1$
    private String TRACKED_ASSAULT_STRING = "default_tracked_assault"; //$NON-NLS-1$
    private String WHEELED_STRING = "default_wheeled"; //$NON-NLS-1$
    private String WHEELED_HEAVY_STRING = "default_wheeled_heavy"; //$NON-NLS-1$
    private String HOVER_STRING = "default_hover"; //$NON-NLS-1$
    private String NAVAL_STRING = "default_naval"; //$NON-NLS-1$
    private String SUBMARINE_STRING = "default_submarine"; //$NON-NLS-1$
    private String HYDROFOIL_STRING = "default_hydrofoil"; //$NON-NLS-1$
    private String VTOL_STRING = "default_vtol"; //$NON-NLS-1$
    private String INF_STRING = "default_infantry"; //$NON-NLS-1$
    private String BA_STRING = "default_ba"; //$NON-NLS-1$
    private String PROTO_STRING = "default_proto"; //$NON-NLS-1$
    private String GUN_EMPLACEMENT_STRING = "default_gun_emplacement"; //$NON-NLS-1$
    private String WIGE_STRING = "default_wige"; //$NON-NLS-1$
    private String AERO_STRING = "default_aero"; //$NON-NLS-1$
    private String SMALL_CRAFT_AERO_STRING = "default_small_craft_aero"; //$NON-NLS-1$
    private String SMALL_CRAFT_SPHERE_STRING = "default_small_craft_sphere"; //$NON-NLS-1$
    private String DROPSHIP_AERO_STRING = "default_dropship_aero"; //$NON-NLS-1$
    private String DROPSHIP_SPHERE_STRING = "default_dropship_sphere"; //$NON-NLS-1$
    private String JUMPSHIP_STRING = "default_jumpship"; //$NON-NLS-1$
    private String WARSHIP_STRING = "default_warship"; //$NON-NLS-1$
    private String SPACE_STATION_STRING = "default_space_station"; //$NON-NLS-1$
    private String FIGHTER_SQUADRON_STRING = "default_fighter_squadron"; //$NON-NLS-1$

    private MechEntry default_light;
    private MechEntry default_medium;
    private MechEntry default_heavy;
    private MechEntry default_assault;
    private MechEntry default_quad;
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
    private MechEntry default_dropship_sphere;
    private MechEntry default_jumpship;
    private MechEntry default_warship;
    private MechEntry default_space_station;
    private MechEntry default_fighter_squadron;
    
    private HashMap<String, MechEntry> exact = new HashMap<String, MechEntry>();
    private HashMap<String, MechEntry> chassis = new HashMap<String, MechEntry>();

    private String dir;

    /**
     * Creates new MechTileset
     */
    public MechTileset(String dir) {
        this.dir = dir;
    }

    public Image imageFor(Entity entity, Component comp) {
        MechEntry entry = entryFor(entity);

        if (entry == null) {
            System.err
                    .println("Entry is null make sure that their is a default entry for "
                            + entity.getShortNameRaw()
                            + " in both mechset.txt and wreckset.txt.  Default to "
                            + LIGHT_STRING);
            System.err.flush();
            entry = default_light;
        }

        if (entry.getImage() == null) {
            entry.loadImage(comp);
        }
        return entry.getImage();
    }

    /**
     * Returns the MechEntry corresponding to the entity
     */
    private MechEntry entryFor(Entity entity) {
        // first, check for exact matches
        if (exact.containsKey(entity.getShortNameRaw().toUpperCase())) {
            return exact.get(entity.getShortNameRaw().toUpperCase());
        }

        // next, chassis matches
        if (chassis.containsKey(entity.getChassis().toUpperCase())) {
            return chassis.get(entity.getChassis().toUpperCase());
        }

        // last, the generic model
        return genericFor(entity);
    }

    public MechEntry genericFor(Entity entity) {
        if (entity instanceof BattleArmor) {
            return default_ba;
        }
        if (entity instanceof Infantry) {
            return default_inf;
        }
        if (entity instanceof Protomech) {
            return default_proto;
        }
        // mech, by weight
        if (entity instanceof Mech) {
            if (entity.getMovementMode() == IEntityMovementMode.QUAD) {
                return default_quad;
            }
            if (entity.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                return default_light;
            } else if (entity.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                return default_medium;
            } else if (entity.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                return default_heavy;
            } else if (entity.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                return default_assault;
            }
        }
        if (entity.getMovementMode() == IEntityMovementMode.NAVAL) {
            return default_naval;
        }
        if (entity.getMovementMode() == IEntityMovementMode.SUBMARINE) {
            return default_submarine;
        }        
        if (entity.getMovementMode() == IEntityMovementMode.HYDROFOIL) {
            return default_hydrofoil;
        }
        if (entity instanceof Tank) {
            if (entity.getMovementMode() == IEntityMovementMode.TRACKED) {
                if (entity.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                    return default_tracked_heavy;
                } else if (entity.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                    return default_tracked_assault;
                } else
                    return default_tracked;
            }
            if (entity.getMovementMode() == IEntityMovementMode.WHEELED) {
                if (entity.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                    return default_wheeled_heavy;
                }
                return default_wheeled;
            }
            if (entity.getMovementMode() == IEntityMovementMode.HOVER) {
                return default_hover;
            }
            if (entity.getMovementMode() == IEntityMovementMode.VTOL) {
                return default_vtol;
            }
            if (entity.getMovementMode() == IEntityMovementMode.WIGE) {
                return default_wige;
            }
        }
        if (entity instanceof GunEmplacement) {
            return default_gun_emplacement;
        }
        
if (entity instanceof Aero) {
            
            if(entity instanceof SpaceStation)
                return default_space_station;
            
            if(entity instanceof Warship)
                return default_warship;
                
            if(entity instanceof Jumpship)
                return default_jumpship;
            
            if(entity instanceof Dropship) {
                Dropship ds = (Dropship)entity;
                if(ds.isSpheroid()) 
                    return default_dropship_sphere;
                else
                    return default_dropship_aero;
            }
            
            if(entity instanceof FighterSquadron)
                return default_fighter_squadron;
            
            if(entity instanceof SmallCraft) {
                SmallCraft sc = (SmallCraft)entity;
                if(sc.isSpheroid())
                    return default_small_craft_sphere;
                else
                    return default_small_craft_aero;
            }
            
            return default_aero;
        }

        // TODO: better exception?
        throw new IndexOutOfBoundsException("can't find an image for that mech"); //$NON-NLS-1$
    }

    public void loadFromFile(String filename) throws IOException {
        // make inpustream for board
        Reader r = new BufferedReader(new FileReader(dir + filename));
        // read board, looking for "size"
        StreamTokenizer st = new StreamTokenizer(r);
        st.eolIsSignificant(true);
        st.commentChar('#');
        st.quoteChar('"');
        st.wordChars('_', '_');
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            String name = null;
            String imageName = null;
            if (st.ttype == StreamTokenizer.TT_WORD
                    && st.sval.equalsIgnoreCase("include")) { //$NON-NLS-1$
                st.nextToken();
                name = st.sval;
                System.out.print("Loading more unit images from "); //$NON-NLS-1$
                System.out.print(name);
                System.out.println("..."); //$NON-NLS-1$
                try {
                    loadFromFile(name);
                    System.out.print("... finished "); //$NON-NLS-1$
                    System.out.print(name);
                    System.out.println("."); //$NON-NLS-1$
                } catch (IOException ioerr) {
                    System.out.print("... failed: "); //$NON-NLS-1$
                    System.out.print(ioerr.getMessage());
                    System.out.println("."); //$NON-NLS-1$
                }
            } else if (st.ttype == StreamTokenizer.TT_WORD
                    && st.sval.equalsIgnoreCase("chassis")) { //$NON-NLS-1$
                st.nextToken();
                name = st.sval;
                st.nextToken();
                imageName = st.sval;
                // add to list
                chassis.put(name.toUpperCase(), new MechEntry(name, imageName));
            } else if (st.ttype == StreamTokenizer.TT_WORD
                    && st.sval.equalsIgnoreCase("exact")) { //$NON-NLS-1$
                st.nextToken();
                name = st.sval;
                st.nextToken();
                imageName = st.sval;
                // add to list
                exact.put(name.toUpperCase(), new MechEntry(name, imageName));
            }
        }
        r.close();

        default_light = exact.get(LIGHT_STRING.toUpperCase());
        default_medium = exact.get(MEDIUM_STRING.toUpperCase());
        default_heavy = exact.get(HEAVY_STRING.toUpperCase());
        default_assault = exact.get(ASSAULT_STRING.toUpperCase());
        default_quad = exact.get(QUAD_STRING.toUpperCase());
        default_tracked = exact.get(TRACKED_STRING.toUpperCase());
        default_tracked_heavy = exact.get(TRACKED_HEAVY_STRING.toUpperCase());
        default_tracked_assault = exact.get(TRACKED_ASSAULT_STRING
                .toUpperCase());
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
        default_gun_emplacement = exact.get(GUN_EMPLACEMENT_STRING
                .toUpperCase());
        default_wige = exact.get(WIGE_STRING.toUpperCase());
        default_aero = exact.get(AERO_STRING.toUpperCase());
        default_small_craft_aero = exact.get(SMALL_CRAFT_AERO_STRING.toUpperCase());
        default_dropship_aero = exact.get(DROPSHIP_AERO_STRING.toUpperCase());
        default_small_craft_sphere = exact.get(SMALL_CRAFT_SPHERE_STRING.toUpperCase());
        default_dropship_sphere = exact.get(DROPSHIP_SPHERE_STRING.toUpperCase());
        default_jumpship = exact.get(JUMPSHIP_STRING.toUpperCase());
        default_warship = exact.get(WARSHIP_STRING.toUpperCase());
        default_space_station = exact.get(SPACE_STATION_STRING.toUpperCase());
        default_fighter_squadron = exact.get(FIGHTER_SQUADRON_STRING.toUpperCase());
    }

    /**
     * Stores the name, image file name, and image (once loaded) for a mech or
     * other entity
     */
    private class MechEntry {
        private String name;
        private String imageFile;
        private Image image;

        public MechEntry(String name, String imageFile) {
            this.name = name;
            this.imageFile = imageFile;
            image = null;
        }

        public String getName() {
            return name;
        }

        public Image getImage() {
            return image;
        }

        public void loadImage(Component comp) {
            // System.out.println("loading mech image...");
            image = comp.getToolkit().getImage(dir + imageFile);
        }
    }
}
