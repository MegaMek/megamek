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

package megamek.client;

import com.sun.java.util.collections.*;
import java.awt.*;
import java.io.*;

import megamek.common.*;

/**
 * MechTileset is a misleading name, as this matches any unit, not just mechs
 * with the appropriate image.  It requires data/mex/mechset.txt, the format of
 * which is explained in that file.
 *
 * @author  Ben
 * @version
 */
public class MechTileset {
    private String LIGHT_STRING = "default_light";
    private String MEDIUM_STRING = "default_medium";
    private String HEAVY_STRING = "default_heavy";
    private String ASSAULT_STRING = "default_assault";
    private String QUAD_STRING = "default_quad";
    private String TANK_STRING = "default_tank";
    private String INF_STRING = "default_infantry";
    
    
    private MechEntry default_light;
    private MechEntry default_medium;
    private MechEntry default_heavy;
    private MechEntry default_assault;
    private MechEntry default_quad;
    private MechEntry default_tank;
    private MechEntry default_inf;
    
    private HashMap exact = new HashMap();
    private HashMap chassis = new HashMap();
    
    /** Creates new MechTileset */
    public MechTileset() {
        ;
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
        if (entity.entityIsQuad()) {
            return default_quad;
        }
        if (entity instanceof Tank) {
            return default_tank;
        }
        if (entity instanceof Infantry) {
            return default_inf;
        }
        // mech, by weight
        if (entity.getWeight() <= Mech.WEIGHT_LIGHT) {
            return default_light;
        } else if (entity.getWeight() <= Mech.WEIGHT_MEDIUM) {
            return default_medium;
        } else if (entity.getWeight() <= Mech.WEIGHT_HEAVY) {
            return default_heavy;
        } else if (entity.getWeight() <= Mech.WEIGHT_ASSAULT) {
            return default_assault;
        }
        
        //TODO: better exception?
        throw new IndexOutOfBoundsException("can't find an image for that mech");
    }
    
    public void loadFromFile(String filename) {
        try {
            // make inpustream for board
            Reader r = new BufferedReader(new FileReader("data/mex/" + filename));
            // read board, looking for "size"
            StreamTokenizer st = new StreamTokenizer(r);
            st.eolIsSignificant(true);
            st.commentChar('#');
            st.quoteChar('"');
            st.wordChars('_', '_');
            while(st.nextToken() != StreamTokenizer.TT_EOF) {
                String name = null;
                String imageName = null;
                if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("chassis")) {
                    st.nextToken();
                    name = st.sval;
                    st.nextToken();
                    imageName = st.sval;
                    // add to list
                    chassis.put(name.toUpperCase(), new MechEntry(name, imageName));
                } else if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("exact")) {
                    st.nextToken();
                    name = st.sval;
                    st.nextToken();
                    imageName = st.sval;
                    // add to list
                    exact.put(name.toUpperCase(), new MechEntry(name, imageName));
                }
            }
            r.close();
        } catch (IOException ex) {
            ;
        }
        
        default_light = (MechEntry)exact.get(LIGHT_STRING.toUpperCase());
        default_medium = (MechEntry)exact.get(MEDIUM_STRING.toUpperCase());
        default_heavy = (MechEntry)exact.get(HEAVY_STRING.toUpperCase());
        default_assault = (MechEntry)exact.get(ASSAULT_STRING.toUpperCase());
        default_quad = (MechEntry)exact.get(QUAD_STRING.toUpperCase());
        default_tank = (MechEntry)exact.get(TANK_STRING.toUpperCase());
        default_inf = (MechEntry)exact.get(INF_STRING.toUpperCase());
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
            image = comp.getToolkit().getImage("data/mex/" + imageFile);
        }
    }
}
