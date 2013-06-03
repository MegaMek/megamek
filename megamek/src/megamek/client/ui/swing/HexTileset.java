/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

package megamek.client.ui.swing;

import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import megamek.client.ui.swing.util.ImageCache;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.IHex;
import megamek.common.ITerrain;
import megamek.common.Terrains;
import megamek.common.util.StringUtil;

/**
 * Matches each hex with an appropriate image.
 *
 * @author Ben
 */
public class HexTileset {

    private ArrayList<HexEntry> bases = new ArrayList<HexEntry>();
    private ArrayList<HexEntry> supers = new ArrayList<HexEntry>();
    private ArrayList<HexEntry> ortho = new ArrayList<HexEntry>();
    private ImageCache<IHex, Image> hexToImageCache = 
        new ImageCache<IHex, Image>();
    private ImageCache<IHex, List<Image>> hexToImageListCache = 
        new ImageCache<IHex, List<Image>>();
    private ImageCache<IHex, List<Image>> orthoListCache = 
        new ImageCache<IHex, List<Image>>();

    /**
     * Creates new HexTileset
     */
    public HexTileset() {
    }

    public synchronized void clearHex(IHex hex) {
        hexToImageCache.remove(hex);
    }

    /**
     * This assigns images to a hex based on the best matches it can find.
     * <p/>
     * First it assigns any images to be superimposed on a hex. These images
     * must have a match value of 1.0 to be added, and any time a match of this
     * level is achieved, any terrain involved in the match is removed from
     * further consideration.
     * <p/>
     * Any terrain left is used to match a base image for the hex. This time, a
     * match can be any value, and the first, best image is used.
     */
    public synchronized Object[] assignMatch(IHex hex, Component comp) {
        IHex hexCopy = hex.duplicate();
        List<Image> ortho = orthoFor(hexCopy, comp);
        List<Image> supers = supersFor(hexCopy, comp);
        Image base = baseFor(hexCopy, comp);
        Object[] pair = new Object[] { base, supers, ortho };
        hexToImageCache.put(hex, base);
        hexToImageListCache.put(hex, supers);
        orthoListCache.put(hex, ortho);
        return pair;
    }

    public synchronized Image getBase(IHex hex, Component comp) {
        Image i = hexToImageCache.get(hex);
        if (i == null) {
            Object[] pair = assignMatch(hex, comp);
            return (Image) pair[0];
        }
        return i;
    }

    @SuppressWarnings("unchecked")
    public synchronized List<Image> getSupers(IHex hex, Component comp) {
        List<Image> l = hexToImageListCache.get(hex);
        if (l == null) {
            Object[] pair = assignMatch(hex, comp);
            return (List<Image>) pair[1];
        }
        return l;
    }

    @SuppressWarnings("unchecked")
    public synchronized List<Image> getOrtho(IHex hex, Component comp) {
        List<Image> o = orthoListCache.get(hex);
        if (o == null) {
            Object[] pair = assignMatch(hex, comp);
            return (List<Image>) pair[2];
        }
        return o;
    }

    /**
     * Returns a list of orthographic images to be tiled above the hex. As noted
     * above, all matches must be 1.0, and if such a match is achieved, all
     * terrain elements from the tileset hex are removed from the hex. Thus you
     * want to pass a copy of the original to this function.
     */
    private List<Image> orthoFor(IHex hex, Component comp) {
        ArrayList<Image> matches = new ArrayList<Image>();

        // find orthographic image matches
        for (Iterator<HexEntry> i = ortho.iterator(); i.hasNext();) {
            HexEntry entry = i.next();
            if (orthoMatch(hex, entry.getHex()) >= 1.0) {
                matches.add(entry.getImage(comp, hex.getCoords().hashCode()));
                // remove involved terrain from consideration
                int terrTypes[] = entry.getHex().getTerrainTypes();
                for (int j = 0; j < terrTypes.length; j++) {
                    if (entry.getHex().containsTerrain(terrTypes[j])) {
                        hex.removeTerrain(terrTypes[j]);
                    }
                }
            }
        }
        return matches;
    }

    /**
     * Returns a list of images to be superimposed on the hex. As noted above,
     * all matches must be 1.0, and if such a match is achieved, all terrain
     * elements from the tileset hex are removed from the hex. Thus you want to
     * pass a copy of the original to this function.
     */
    private List<Image> supersFor(IHex hex, Component comp) {
        ArrayList<Image> matches = new ArrayList<Image>();

        // find superimposed image matches
        for (Iterator<HexEntry> i = supers.iterator(); i.hasNext();) {
            HexEntry entry = i.next();
            if (superMatch(hex, entry.getHex()) >= 1.0) {
                matches.add(entry.getImage(comp, hex.getCoords().hashCode()));
                // remove involved terrain from consideration
                int terrTypes[] = entry.getHex().getTerrainTypes();
                for (int j = 0; j < terrTypes.length; j++) {
                    if (entry.getHex().containsTerrain(terrTypes[j])) {
                        hex.removeTerrain(terrTypes[j]);
                    }
                }
            }
        }
        return matches;
    }

    /**
     * Returns the best matching base image for this hex. This works best if any
     * terrain with a "super" image is removed.
     */
    private Image baseFor(IHex hex, Component comp) {
        HexEntry bestMatch = null;
        double match = -1;

        // match a base image to the hex
        Iterator<HexEntry> iter = bases.iterator();

        while (iter.hasNext()) {
            HexEntry entry = iter.next();

            // Metal deposits don't count for visual
            if (entry.getHex().containsTerrain(Terrains.METAL_CONTENT)) {
                hex.removeTerrain(Terrains.METAL_CONTENT);
            }

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

        return bestMatch.getImage(comp, hex.getCoords().hashCode());
    }

    // perfect match
    // all but theme
    // all but elevation
    // all but elevation & theme

    public void loadFromFile(String filename) throws IOException {
        // make input stream for board
        Reader r = new BufferedReader(new FileReader(
                new File(Configuration.hexesDir(), filename)
        ));
        // read board, looking for "size"
        StreamTokenizer st = new StreamTokenizer(r);
        st.eolIsSignificant(true);
        st.commentChar('#');
        st.quoteChar('"');
        st.wordChars('_', '_');
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            int elevation = 0;
            // int levity = 0;
            String terrain = null;
            String theme = null;
            String imageName = null;
            if ((st.ttype == StreamTokenizer.TT_WORD)
                    && (st.sval.equals("base") || st.sval.equals("super") || 
                        st.sval.equals("ortho"))) { //$NON-NLS-1$ //$NON-NLS-2$
                boolean bas = st.sval.equals("base"); //$NON-NLS-1$
                boolean sup = st.sval.equals("super"); //$NON-NLS-1$
                boolean ort = st.sval.equals("ortho"); //$NON-NLS-1$

                if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
                    elevation = (int) st.nval;
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
                if (bas) {
                    bases.add(new HexEntry(new Hex(elevation, terrain, theme),
                            imageName));
                }
                if (sup) {
                    supers.add(new HexEntry(new Hex(elevation, terrain, theme),
                            imageName));
                }
                if (ort) {
                    ortho.add(new HexEntry(new Hex(elevation, terrain, theme),
                            imageName));
                }
            }
            // else if((st.ttype == StreamTokenizer.TT_WORD) &&
            // st.sval.equals("ortho")){}
        }
        r.close();

        System.out
                .println("hexTileset: loaded " + bases.size() + 
                        " base images"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out
                .println("hexTileset: loaded " + supers.size() + 
                        " super images"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out
                .println("hexTileset: loaded " + ortho.size() + 
                        " ortho images"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Initializes all the images in this tileset and adds them to the tracker
     */
    public void loadAllImages(Component comp, MediaTracker tracker) {
        for (Iterator<HexEntry> i = bases.iterator(); i.hasNext();) {
            HexEntry entry = i.next();
            if (entry.getImage() == null) {
                entry.loadImage(comp);
            }
            tracker.addImage(entry.getImage(), 1);
        }
        for (Iterator<HexEntry> i = supers.iterator(); i.hasNext();) {
            HexEntry entry = i.next();
            if (entry.getImage() == null) {
                entry.loadImage(comp);
            }
            tracker.addImage(entry.getImage(), 1);
        }
        for (Iterator<HexEntry> i = ortho.iterator(); i.hasNext();) {
            HexEntry entry = i.next();
            if (entry.getImage() == null) {
                entry.loadImage(comp);
            }
            tracker.addImage(entry.getImage(), 1);
        }
    }

    /**
     * Adds all images associated with the hex to the specified tracker
     */
    public synchronized void trackHexImages(IHex hex, MediaTracker tracker) {

        Image base = hexToImageCache.get(hex);
        List<Image> superImgs = hexToImageListCache.get(hex);
        List<Image> orthoImgs = orthoListCache.get(hex);

        // add base
        tracker.addImage(base, 1);
        // add superImgs
        if (superImgs != null) {
            for (Iterator<Image> i = superImgs.iterator(); i.hasNext();) {
                tracker.addImage(i.next(), 1);
            }
        }
        if (orthoImgs != null) {
            for (Iterator<Image> i = orthoImgs.iterator(); i.hasNext();) {
                tracker.addImage(i.next(), 1);
            }
        }

    }

    /**
     * Loads the image for this hex.
     */
    public void loadHexImage(IHex hex, Component comp, MediaTracker tracker) {

    }

    public synchronized void reset() {
        hexToImageCache = new ImageCache<IHex, Image>();
        hexToImageListCache = new ImageCache<IHex, List<Image>>();
        orthoListCache = new ImageCache<IHex, List<Image>>();
    }

    /**
     * Match the two hexes using the "ortho" super* formula. All matches must be
     * exact, however the match only depends on the original hex matching all
     * the elements of the comparison, not vice versa.
     * <p/>
     * EXCEPTION: a themed original matches any unthemed comparison.
     */
    private double orthoMatch(IHex org, IHex com) {
        // check elevation
        if ((com.getElevation() != ITerrain.WILDCARD)
                && (org.getElevation() != com.getElevation())) {
            return 0;
        }
        
        // A themed original matches any unthemed comparison.
        if ((com.getTheme() != null)
                && !com.getTheme().equalsIgnoreCase(org.getTheme())) {
            return 0.0;
        }
        
        // org terrains must match com terrains
        if (org.terrainsPresent() < com.terrainsPresent())
            return 0.0;
        
        // check terrain
        int cTerrainTypes[] = com.getTerrainTypes();
        for (int i = 0; i < cTerrainTypes.length; i++) {
            int cTerrType = cTerrainTypes[i];
            ITerrain cTerr = com.getTerrain(cTerrType);
            ITerrain oTerr = org.getTerrain(cTerrType);
            if (cTerr == null) {
                continue;
            } else if ((oTerr == null)
                    || ((cTerr.getLevel() != ITerrain.WILDCARD) && (oTerr
                            .getLevel() != cTerr.getLevel()))
                    || (cTerr.hasExitsSpecified() && (oTerr.getExits() != cTerr
                            .getExits()))) {
                return 0;
            }
        }

        return 1.0;
    }

    /**
     * Match the two hexes using the "super" formula. All matches must be exact,
     * however the match only depends on the original hex matching all the
     * elements of the comparision, not vice versa.
     * <p/>
     * EXCEPTION: a themed original matches any unthemed comparason.
     */
    private double superMatch(IHex org, IHex com) {
        // check elevation
        if ((com.getElevation() != ITerrain.WILDCARD)
                && (org.getElevation() != com.getElevation())) {
            return 0;
        }
        
        // A themed original matches any unthemed comparison.
        if ((com.getTheme() != null)
                && !com.getTheme().equalsIgnoreCase(org.getTheme())) {
            return 0.0;
        }
        
        // org terrains must match com terrains
        if (org.terrainsPresent() < com.terrainsPresent())
            return 0.0;
       
        // check terrain
        int cTerrainTypes[] = com.getTerrainTypes();
        for (int i = 0; i < cTerrainTypes.length; i++) {
            int cTerrType = cTerrainTypes[i];
            ITerrain cTerr = com.getTerrain(cTerrType);
            ITerrain oTerr = org.getTerrain(cTerrType);
            if (cTerr == null) {
                continue;
            } else if ((oTerr == null)
                    || ((cTerr.getLevel() != ITerrain.WILDCARD) && (oTerr
                            .getLevel() != cTerr.getLevel()))
                    || (cTerr.hasExitsSpecified() && (oTerr.getExits() != cTerr
                            .getExits()))) {
                return 0;
            }
        }


        return 1.0;
    }

    /**
     * Match the two hexes using the "base" formula.
     * <p/>
     * Returns a value indicating how close of a match the original hex is to
     * the comparison hex. 0 means no match, 1 means perfect match.
     */
    private double baseMatch(IHex org, IHex com) {
        double elevation;
        double terrain;
        double theme;

        // check elevation
        if (com.getElevation() == ITerrain.WILDCARD) {
            elevation = 1.0;
        } else {
            elevation = 1.01 / (Math.abs(org.getElevation()
                    - com.getElevation()) + 1.01);
        }

        // Determine maximum number of terrain matches.
        // Bug 732188: Have a non-zero minimum terrain match.
        double maxTerrains = Math.max(org.terrainsPresent(),
                com.terrainsPresent());
        double matches = 0.0;
        
        int[] orgTerrains = org.getTerrainTypes();
        
        for (int i = 0; i < orgTerrains.length; i++){
            int terrType = orgTerrains[i];
            ITerrain cTerr = com.getTerrain(terrType);
            ITerrain oTerr = org.getTerrain(terrType);
            if ((cTerr == null) || (oTerr == null)) {
                continue;
            }
            double thisMatch = 0;

            if (cTerr.getLevel() == ITerrain.WILDCARD) {
                thisMatch = 1.0;
            } else {
                thisMatch = 1.0 / (Math
                        .abs(oTerr.getLevel() - cTerr.getLevel()) + 1.0);
            }
            // without exit match, terrain counts... um, half?
            if (cTerr.hasExitsSpecified()
                    && (oTerr.getExits() != cTerr.getExits())) {
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
        if ((com.getTheme() == org.getTheme())
                || ((com.getTheme() != null) && com.getTheme()
                        .equalsIgnoreCase(org.getTheme()))) {
            theme = 1.0;
        } else {
            // also don't throw a match entirely out because the theme is off
            theme = 0.0001;
        }

        return elevation * terrain * theme;
    }

    private class HexEntry {
        private IHex hex;
        private Image image;
        private Vector<Image> images;
        private Vector<String> filenames;

        public HexEntry(IHex hex, String imageFile) {
            this.hex = hex;
            filenames = StringUtil.splitString(imageFile, ";"); //$NON-NLS-1$
        }

        public IHex getHex() {
            return hex;
        }

        public Image getImage() {
            return image;
        }

        public Image getImage(Component comp, int seed) {
            if (images == null) {
                loadImage(comp);
            }
            if (images.size() > 1) {
                // Use a "seed" for determining which
                // Image to use from the tileset. Normally the seed is the
                // hashcode of the hex coordinates.
                // Coords.Hashcode() is always a multiple of 4. So mess with the
                // seed a little.
                int betterSeed = (seed >> 3) + (seed);
                int rand = (betterSeed % images.size());
                return images.elementAt(rand);
            }
            return images.firstElement();
        }

        public void loadImage(Component comp) {
            images = new Vector<Image>();
            for (int i = 0; i < filenames.size(); i++) {
                String filename = filenames.elementAt(i);
                images.addElement(comp.getToolkit().getImage(
                        new File(Configuration.hexesDir(), filename).toString()
                ));
            }
        }
    }
}
