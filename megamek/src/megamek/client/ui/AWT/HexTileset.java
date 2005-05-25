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
 * HexTileset.java
 *
 * Created on May 9, 2002, 1:33 PM
 */

package megamek.client;

import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;


import megamek.common.Hex;
import megamek.common.IHex;
import megamek.common.ITerrain;
import megamek.common.Terrains;
import megamek.client.util.ImageCache;

import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;

/**
 * Matches each hex with an appropriate image.
 *
 * @author  Ben
 * @version 
 */
public class HexTileset {
    
    private ArrayList bases = new ArrayList();
    private ArrayList supers = new ArrayList();
    private ImageCache hexToBaseImageCache = new ImageCache();
    private ImageCache hexToSupersImageCache = new ImageCache(); 

    /** Creates new HexTileset */
    public HexTileset() {
    }
    
    /**
     * This assigns images to a hex based on the best matches it can find.
     *
     * First it assigns any images to be superimposed on a hex.  These images
     * must have a match value of 1.0 to be added, and any time a match of this
     * level is achieved, any terrain involved in the match is removed from
     * further consideration.
     *
     * Any terrain left is used to match a base image for the hex.  This time,
     * a match can be any value, and the first, best image is used.
     */
    public void assignMatch(IHex hex, Component comp) {
        IHex hexCopy = hex.duplicate();
        hexToBaseImageCache.put(hex, baseFor(hexCopy, comp));
        hexToSupersImageCache.put(hex, supersFor(hexCopy, comp));
        hex.markSeen();
    }
    
    public Image getBase(IHex hex, Component comp) {
    	if (hex.isChanged()) {
          assignMatch(hex, comp);
    	}
    	return (Image) hexToBaseImageCache.get(hex);
    }
    
    public List getSupers(IHex hex, Component comp) {
    	if (hex.isChanged()) {
          assignMatch(hex, comp);
    	}
    	return (List) hexToSupersImageCache.get(hex);
    }
    
    /**
     * Returns a list of images to be superimposed on the hex.  As noted above,
     * all matches must be 1.0, and if such a match is achieved, all terrain
     * elements from the tileset hex are removed from the hex.  Thus you want
     * to pass a copy of the original to this function.
     */
    private List supersFor(IHex hex, Component comp) {
        ArrayList matches = new ArrayList();
        
        // find superimposed image matches
        for (Iterator i = supers.iterator(); i.hasNext();) {
            HexEntry entry = (HexEntry)i.next();
            if (superMatch(hex, entry.getHex()) >= 1.0) {
                matches.add(entry.getImage(comp));
                // remove involved terrain from consideration
                for (int j = 0; j < Terrains.SIZE; j++) {
                    if (entry.getHex().containsTerrain(j)) {
                        hex.removeTerrain(j);
                    }
                }
            }
        }
        
        // assign null, or the matching images to the hex
        return matches.size() > 0 ? matches : null;
    }
    
    /**
     * Returns the best matching base image for this hex.  This works best if 
     * any terrain with a "super" image is removed.
     */
    private Image baseFor(IHex hex, Component comp) {
        HexEntry bestMatch = null;
        double match = -1;

        // match a base image to the hex
        Iterator iter = bases.iterator();

        while ( iter.hasNext() ) {
            HexEntry entry = (HexEntry) iter.next();
            double thisMatch = baseMatch(hex, entry.getHex());
            // stop if perfect match
            if (thisMatch == 1.0) {
                bestMatch = entry;
                break;
            }
            // compare match with best
            if (thisMatch > match) {
                bestMatch = entry;
                match = thisMatch;
            }
        }
        
        return bestMatch.getImage(comp);
    }
    
    // perfect match
    // all but theme
    // all but elevation
    // all but elevation & theme
    
    
    public void loadFromFile(String filename) throws IOException {
        // make inpustream for board
        Reader r = new BufferedReader(new FileReader("data/hexes/" + filename)); //$NON-NLS-1$
        // read board, looking for "size"
        StreamTokenizer st = new StreamTokenizer(r);
        st.eolIsSignificant(true);
        st.commentChar('#');
        st.quoteChar('"');
        st.wordChars('_', '_');
        while(st.nextToken() != StreamTokenizer.TT_EOF) {
            int elevation = 0;
            String terrain = null;
            String theme = null;
            String imageName = null;
            if(st.ttype == StreamTokenizer.TT_WORD && (st.sval.equals("base") || st.sval.equals("super"))) { //$NON-NLS-1$ //$NON-NLS-2$
                boolean base = st.sval.equals("base"); //$NON-NLS-1$

                if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
                    elevation = (int)st.nval;
                } else {
                    elevation = ITerrain.WILDCARD;
                }
                st.nextToken();
                terrain = st.sval;
                st.nextToken();
                theme = st.sval;
                st.nextToken();
                imageName = st.sval;
                // add to list
                if (base) {
                    bases.add(new HexEntry(new Hex(elevation, terrain, theme), imageName));
                } else {
                    supers.add(new HexEntry(new Hex(elevation, terrain, theme), imageName));
                }
            }
        }
        r.close();
        
        System.out.println("hexTileset: loaded " + bases.size() + " base images"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.println("hexTileset: loaded " + supers.size() + " super images"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /**
     * Initializes all the images in this tileset and adds them to the tracker
     */
    public void loadAllImages(Component comp, MediaTracker tracker) {
        for (Iterator i = bases.iterator(); i.hasNext();) {
            HexEntry entry = (HexEntry)i.next();
            if (entry.getImage() == null) {
                entry.loadImage(comp);
            }
            tracker.addImage(entry.getImage(), 1);
        }
        for (Iterator i = supers.iterator(); i.hasNext();) {
            HexEntry entry = (HexEntry)i.next();
            if (entry.getImage() == null) {
                entry.loadImage(comp);
            }
            tracker.addImage(entry.getImage(), 1);
        }
    }
    
    /**
     * Adds all images associated with the hex to the specified tracker
     */
    public void trackHexImages(IHex hex, MediaTracker tracker) {
        // add base
        tracker.addImage((Image) hexToBaseImageCache.get(hex), 1);
        
        // add superImgs
        List superImgs = (List) hexToSupersImageCache.get(hex);
        if (superImgs != null) {
            for (Iterator i = superImgs.iterator(); i.hasNext();) {
                tracker.addImage((Image)i.next(), 1);
            }
        }
    }
    
    /**
     * Loads the image for this hex.
     */
    public void loadHexImage(IHex hex, Component comp, MediaTracker tracker) {
        
    }
    
    /**
     * Match the two hexes using the "super" formula.  All matches must be
     * exact, however the match only depends on the original hex matching
     * all the elements of the comparision, not vice versa.
     *
     * EXCEPTION: a themed original matches any unthemed comparason.
     */
    private double superMatch(IHex org, IHex com) {
        // check elevation
        if (com.getElevation() != ITerrain.WILDCARD && org.getElevation() != com.getElevation()) {
            return 0;
        }
        // check terrain
        for (int i = 0; i < Terrains.SIZE; i++) {
            ITerrain cTerr = com.getTerrain(i);
            ITerrain oTerr = org.getTerrain(i);
            if (cTerr == null) {
                continue;
            } else if (oTerr == null 
                    || (cTerr.getLevel() != ITerrain.WILDCARD 
                        && oTerr.getLevel() != cTerr.getLevel())
                    || (cTerr.hasExitsSpecified() 
                        && oTerr.getExits() != cTerr.getExits())) {
                return 0;
            }
        }
        // A themed original matches any unthemed comparason.
        if ( com.getTheme() != null &&
             !com.getTheme().equalsIgnoreCase(org.getTheme()) ) {
            return 0.0;
        } 
        
        return 1.0;
    }
    
    /**
     * Match the two hexes using the "base" formula.  
     *
     * Returns a value indicating how close of a match the original hex is to
     * the comparison hex.  0 means no match, 1 means perfect match.
     */
    private double baseMatch(IHex org, IHex com) {
        double elevation;
        double terrain;
        double theme;
        
        // check elevation
        if (com.getElevation() == ITerrain.WILDCARD) {
            elevation = 1.0;
        } else {
            elevation = 1.01 / (Math.abs(org.getElevation() - com.getElevation()) + 1.01);
        }
        
        // Determine maximum number of terrain matches.
        // Bug 732188: Have a non-zero minimum terrain match.
        double maxTerrains = Math.max(org.terrainsPresent(), com.terrainsPresent());
        double matches = 0.0;
        for (int i = 0; i < Terrains.SIZE; i++) {
            ITerrain cTerr = com.getTerrain(i);
            ITerrain oTerr = org.getTerrain(i);
            if (cTerr == null || oTerr == null) {
                continue;
            }
            double thisMatch = 0;
            
            if (cTerr.getLevel() == ITerrain.WILDCARD) {
                thisMatch = 1.0;
            } else {
                thisMatch = 1.0 / (Math.abs(oTerr.getLevel() - cTerr.getLevel()) + 1.0);
            }
            // without exit match, terrain counts... um, half?
            if (cTerr.hasExitsSpecified() && oTerr.getExits() != cTerr.getExits()) {
                thisMatch *= 0.5;
            }
            // add up match value
            matches += thisMatch;
        }
        if (maxTerrains == 0) {
            terrain = 1.0;
        } else {
            terrain = matches / maxTerrains;
        }
        
        // check theme
        if ( com.getTheme() == org.getTheme() || 
             ( com.getTheme() != null &&
               com.getTheme().equalsIgnoreCase(org.getTheme()) ) ) {
            theme = 1.0;
        } else {
            // also don't throw a match entirely out because the theme is off
            theme = 0.0001;
        }
        
        return elevation * terrain * theme;
    }

}

