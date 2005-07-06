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

package megamek.client.ui.AWT;

import com.sun.java.util.collections.*;
import java.awt.*;
import java.io.*;

import megamek.common.*;

/**
 * MechTileset is a misleading name, as this matches any unit, not just mechs
 * with the appropriate image.  It requires data/images/units/mechset.txt, 
 * the format of which is explained in that file.
 *
 * @author  Ben
 * @version
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
    private String HYDROFOIL_STRING = "default_hydrofoil"; //$NON-NLS-1$
    private String VTOL_STRING = "default_vtol"; //$NON-NLS-1$
    private String INF_STRING = "default_infantry"; //$NON-NLS-1$
    private String BA_STRING = "default_ba"; //$NON-NLS-1$
    private String PROTO_STRING = "default_proto"; //$NON-NLS-1$
    
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
    private MechEntry default_hydrofoil;
    private MechEntry default_vtol;
    private MechEntry default_inf;
    private MechEntry default_ba;
    private MechEntry default_proto;
    
    private HashMap exact = new HashMap();
    private HashMap chassis = new HashMap();
    
    private String dir;

    /** Creates new MechTileset */
    public MechTileset(String dir) {
        this.dir = dir;
    }
    
    public Image imageFor(Entity entity, Component comp) {
        MechEntry entry = entryFor(entity);
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
        if (exact.containsKey(entity.getShortName().toUpperCase())) {
            return (MechEntry)exact.get(entity.getShortName().toUpperCase());
        }
        
        // next, chassis matches
        if (chassis.containsKey(entity.getChassis().toUpperCase())) {
            return (MechEntry)chassis.get(entity.getChassis().toUpperCase());
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
        if (entity.getMovementMode() == IEntityMovementMode.HYDROFOIL) {
            return default_hydrofoil;
        }
        if (entity instanceof Tank) {
            if (entity.getMovementMode() == IEntityMovementMode.TRACKED) {
                if (entity.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                    return default_tracked_heavy;
                } else if (entity.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                    return default_tracked_assault;
                } else return default_tracked;
            }
            if (entity.getMovementMode() == IEntityMovementMode.WHEELED) {
                if (entity.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                    return default_wheeled_heavy;
                } else return default_wheeled;
            }
            if (entity.getMovementMode() == IEntityMovementMode.HOVER) {
                return default_hover;
            }
            if (entity.getMovementMode() == IEntityMovementMode.VTOL) {
                return default_vtol;
            }
            
        }
        
        //TODO: better exception?
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
        while(st.nextToken() != StreamTokenizer.TT_EOF) {
            String name = null;
            String imageName = null;
            if ( st.ttype == StreamTokenizer.TT_WORD
                 && st.sval.equalsIgnoreCase("include") ) { //$NON-NLS-1$
                st.nextToken();
                name = st.sval;
                System.out.print( "Loading more unit images from " ); //$NON-NLS-1$
                System.out.print( name );
                System.out.println( "..." ); //$NON-NLS-1$
                try {
                    this.loadFromFile(name);
                System.out.print( "... finished " ); //$NON-NLS-1$
                System.out.print( name );
                System.out.println( "." ); //$NON-NLS-1$
                }
                catch (IOException ioerr) {
                    System.out.print( "... failed: " ); //$NON-NLS-1$
                    System.out.print( ioerr.getMessage() );
                    System.out.println( "." ); //$NON-NLS-1$
                }
            } else if ( st.ttype == StreamTokenizer.TT_WORD
                        && st.sval.equalsIgnoreCase("chassis") ) { //$NON-NLS-1$
                st.nextToken();
                name = st.sval;
                st.nextToken();
                imageName = st.sval;
                // add to list
                chassis.put(name.toUpperCase(), new MechEntry(name, imageName));
            } else if ( st.ttype == StreamTokenizer.TT_WORD
                        && st.sval.equalsIgnoreCase("exact") ) { //$NON-NLS-1$
                st.nextToken();
                name = st.sval;
                st.nextToken();
                imageName = st.sval;
                // add to list
                exact.put(name.toUpperCase(), new MechEntry(name, imageName));
            }
        }
        r.close();
        
        default_light = (MechEntry)exact.get(LIGHT_STRING.toUpperCase());
        default_medium = (MechEntry)exact.get(MEDIUM_STRING.toUpperCase());
        default_heavy = (MechEntry)exact.get(HEAVY_STRING.toUpperCase());
        default_assault = (MechEntry)exact.get(ASSAULT_STRING.toUpperCase());
        default_quad = (MechEntry)exact.get(QUAD_STRING.toUpperCase());
        default_tracked = (MechEntry)exact.get(TRACKED_STRING.toUpperCase());
        default_tracked_heavy = (MechEntry)exact.get(TRACKED_HEAVY_STRING.toUpperCase());
        default_tracked_assault = (MechEntry)exact.get(TRACKED_ASSAULT_STRING.toUpperCase());
        default_wheeled = (MechEntry)exact.get(WHEELED_STRING.toUpperCase());
        default_wheeled_heavy = (MechEntry)exact.get(WHEELED_HEAVY_STRING.toUpperCase());
        default_hover = (MechEntry)exact.get(HOVER_STRING.toUpperCase());
        default_naval = (MechEntry)exact.get(NAVAL_STRING.toUpperCase());
        default_hydrofoil = (MechEntry)exact.get(HYDROFOIL_STRING.toUpperCase());
        default_vtol = (MechEntry)exact.get(VTOL_STRING.toUpperCase());
        default_inf = (MechEntry)exact.get(INF_STRING.toUpperCase());
        default_ba = (MechEntry)exact.get(BA_STRING.toUpperCase());
        default_proto = (MechEntry)exact.get(PROTO_STRING.toUpperCase());
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
            this.image = null;
        }
        
        public String getName() {
            return name;
        }
        
        public Image getImage() {
            return image;
        }
        
        public void loadImage(Component comp) {
            //            System.out.println("loading mech image...");
            image = comp.getToolkit().getImage(dir + imageFile);
        }
    }
}
