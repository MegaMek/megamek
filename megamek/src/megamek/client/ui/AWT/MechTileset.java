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
 * This will match a mech with the appropriate image
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
    
    
    private MechEntry default_light;
    private MechEntry default_medium;
    private MechEntry default_heavy;
    private MechEntry default_assault;
    private MechEntry default_quad;
    
    private HashMap exact = new HashMap();
    private HashMap wildcard = new HashMap();
    
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
        String model = entity.getModel();
        int dash;
        String beforeTheDash;
        
        // first, check for exact matches
        if (exact.containsKey(model)) {
            return (MechEntry)exact.get(model);
        }
        
        // next, before-the-dash wildcard matches
        dash = model.indexOf('-');
        beforeTheDash = dash == -1 ? null : model.substring(0, dash); 
        if (beforeTheDash != null && wildcard.containsKey(beforeTheDash)) {
            return (MechEntry)wildcard.get(beforeTheDash);
        }
        
        // next, the generic model
//        if (entity is a quad) {
//            return default_quad;
//        }
        // weight?
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
                String model = null;
                String imageName = null;
                if(st.ttype == StreamTokenizer.TT_WORD && st.sval.equalsIgnoreCase("mech")) {
                    st.nextToken();
                    model = st.sval;
                    st.nextToken();
                    imageName = st.sval;
                    // add to list
                    addEntry(model, imageName);
                 }
            }
            r.close();
        } catch (IOException ex) {
            ;
        }
        
        default_light = (MechEntry)exact.get(LIGHT_STRING);
        default_medium = (MechEntry)exact.get(MEDIUM_STRING);
        default_heavy = (MechEntry)exact.get(HEAVY_STRING);
        default_assault = (MechEntry)exact.get(ASSAULT_STRING);
        default_quad = (MechEntry)exact.get(QUAD_STRING);
    }
    
    private void addEntry(String model, String imageName) {
        if (model.endsWith("-*")) {
            wildcard.put(model.substring(0, model.indexOf("-*")), new MechEntry(model, imageName));
        } else {
            exact.put(model, new MechEntry(model, imageName));
        }
    }
    
    
    private class MechEntry {
        private String model;
        private String imageFile;
        private Image image;
        
        public MechEntry(String model, String imageFile) {
            this.model = model;
            this.imageFile = imageFile;
            this.image = null;
        }
        
        public String getModel() {
            return model;
        }
        
        public Image getImage() {
            return image;
        }
        
        public void loadImage(Component comp) {
            image = comp.getToolkit().getImage("data/mex/" + imageFile);
        }        
    }
}
