/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
 * 
 * This file (C) 2008 Jörg Walter <j.walter@syntax-k.de>
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

package megamek.client.ui.AWT.boardview3d;

import com.sun.j3d.utils.image.TextureLoader;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import javax.media.j3d.Appearance;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import megamek.client.ui.AWT.TilesetManager;
import megamek.client.ui.AWT.util.ImageCache;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;




class TileTextureManager implements IPreferenceChangeListener {
    private TilesetManager tileManager;
    private Component comp;
    private IGame game;

    // texture image cache
    private static ImageCache<ImageAlpha, Texture2D> textureCache = new ImageCache<ImageAlpha, Texture2D>();
    private static final int TEXSIZE = 256;
    
    static final float HEX_ALPHA = 2/3f;
    static final float HEX_ALPHA2 = .4f;
    
    public TileTextureManager(Component c, IGame g) throws IOException {
        comp = c;
        game = g;
        tileManager = new TilesetManager(comp);
        System.out.println("TileTextureManager: loading images for board"); //$NON-NLS-1$
        tileManager.loadNeededImages(game);
        PreferenceManager.getClientPreferences().addPreferenceChangeListener(this);
    }

    public TilesetManager getTilesetManager() {
        return tileManager;
    }
    
    public Shape3D getModel(Entity entity) {
        return getModel(tileManager.imageFor(entity, 0), 1.0f, true);
    }

    public Shape3D getModel(IHex hex) {
        return getModel(tileManager.baseFor(hex), HEX_ALPHA2, false);
    }

    public List<Shape3D> getModels(IHex hex) {
        List<Image> supers = tileManager.supersFor(hex);
        List<Shape3D> out = new Vector<Shape3D>();
        if (supers != null) {
            for (Image img : supers) {
                out.add(getModel(img, HEX_ALPHA, false));
            }
        }
        return out;
    }

    public Shape3D getModel(Image img, float alpha, boolean scale) {
        MediaTracker tracker = new MediaTracker(comp);

        // Fully load image before continuing
        tracker.addImage(img, 0);
        do {
            try { tracker.waitForID(0); } catch (InterruptedException e) { continue; }
        } while (false);
        tracker.removeImage(img);

        final int w = img.getWidth(null), h = img.getHeight(null);
        BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D tgr = tmp.createGraphics();
        tgr.drawImage(img, null, null);
        tgr.dispose();
        tgr = null;

        tracker.addImage(tmp, 1);
        do {
            try { tracker.waitForID(1); } catch (InterruptedException e) { continue; }
        } while (false);
        tracker.removeImage(tmp);

        Shape3D model = new ImageModel(tmp, scale);
        Appearance app = new Appearance();
        app.setTexture(getTexture(img, false, null, alpha, true));
        if (alpha != 1.0) app.setTransparencyAttributes(C.alphaTexture);
        model.setAppearance(app);

        return model;
    }
    
    public Texture2D getTexture(Entity entity) {
        return getTexture(tileManager.imageFor(entity, 0), false, Color.GRAY, 1.0f, true);
    }

    public Texture2D getTexture(IHex hex, float alpha) {
        return getTexture(tileManager.baseFor(hex), true, null, alpha, true);
    }

    public Node getMinefieldSign() {
        return new SignModel(getTexture(tileManager.getMinefieldSign(), false, null, 1.0f, false));
    }

    Shape3D getArtilleryTarget(int type) {
        return getModel(tileManager.getArtilleryTarget(type), .5f, false);
    }
    
    public Texture2D getTexture(String filename) {
        TextureLoader tl = new TextureLoader(filename, TextureLoader.GENERATE_MIPMAP, null);
        Texture2D tex = (Texture2D)tl.getTexture();
        tex.setMagFilter(Texture.NICEST);
        tex.setMinFilter(Texture.NICEST);
        return tex;
    }

    private final Texture2D getTexture(Image base, boolean border, Color col, float alpha, boolean tile) {
        if (base == null) return null;

        Texture2D tex = textureCache.get(new ImageAlpha(base, alpha));
        if (tex == null) {
            MediaTracker tracker = new MediaTracker(comp);

            // Fully load image before continuing
            tracker.addImage(base, 0);
            do {
                try { tracker.waitForID(0); } catch (InterruptedException e) { continue; }
            } while (false);
            tracker.removeImage(base);

            final int w = base.getWidth(null), h = base.getHeight(null);
            BufferedImage tmp = new BufferedImage(w*2, h*2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D tgr = tmp.createGraphics();
            if (col != null) {
                tgr.setColor(col);
                tgr.fillRect(0, 0, w*2, h*2);
            }
            tgr.translate(w/2, h/2);
            tgr.drawImage(base, null, null);
            if (tile) {
                // create an image of a hex with all 6 surrounding hexes to get
                // better interpolation at the borders
                tgr.translate(0, -h);
                tgr.drawImage(base, null, null);
                tgr.translate(0, 2*h);
                tgr.drawImage(base, null, null);
                tgr.translate(-3*w/4, -h/2);
                tgr.drawImage(base, null, null);
                tgr.translate(0, -h);
                tgr.drawImage(base, null, null);
                tgr.translate(3*w/2, 0);
                tgr.drawImage(base, null, null);
                tgr.translate(0, h);
                tgr.drawImage(base, null, null);
            }

            tracker.addImage(tmp, 1);
            do {
                try { tracker.waitForID(1); } catch (InterruptedException e) { continue; }
            } while (false);
            tracker.removeImage(tmp);

            // TODO: Make this configurable.
            // Cut outer pixel border? This removes the distinct tile border present in most tiles.
            int border_cut = 1;
            // Add our own pixel border? Higher quality (resolution) than the image's tile border.
            int border_add = 3;

            if (!border) {
                border_cut = 0;
                border_add = 0;
            }

            // Scale texture image, cutting border if needed
            BufferedImage src = new BufferedImage(TEXSIZE, TEXSIZE, BufferedImage.TYPE_INT_ARGB);
            AffineTransform at = new AffineTransform();
            at.scale(TEXSIZE/(double)(w-border_cut*2), TEXSIZE/(double)(h-border_cut*2));
            at.translate(-w/2-border_cut, -h/2-border_cut);
            AffineTransformOp ato = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
            ato.filter(tmp, src);

            if (border_add > 0) {
                // Add border if needed
                Graphics2D gr = src.createGraphics();
                gr.setColor(Color.DARK_GRAY);
                gr.setStroke(new BasicStroke(border_add));
                gr.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
                int[] xvals = { TEXSIZE/4, 3*TEXSIZE/4-1, TEXSIZE-1, 3*TEXSIZE/4-1, TEXSIZE/4, 0, TEXSIZE/4 };
                int[] yvals = { 0, 0, TEXSIZE/2, TEXSIZE-1, TEXSIZE-1, TEXSIZE/2, 0 };
                gr.drawPolygon(xvals, yvals, xvals.length);
            }
            
            if (alpha < 1.0f) {
                WritableRaster wr = src.getAlphaRaster();
                for (int y = 0; y < src.getHeight(); y++) {
                    for (int x = 0; x < src.getWidth(); x++)
                        wr.setSample(x, y, 0, alpha*wr.getSampleFloat(x, y, 0));
                }
            }

            // Finally create actual texture
            TextureLoader tl = new TextureLoader(src, TextureLoader.GENERATE_MIPMAP);
            tex = (Texture2D)tl.getTexture();
            tex.setMagFilter(Texture.NICEST);
            tex.setMinFilter(Texture.NICEST);

            if (tileManager.isLoaded()) textureCache.put(new ImageAlpha(base, alpha), tex);
        }
        return tex;
    }
    
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(IClientPreferences.MAP_TILESET)) {
            System.out.println("BoardView3D: loading images for board"); //$NON-NLS-1$
            tileManager.loadNeededImages(game);
        }
    }

    void hexChanged(IHex hex) {
        tileManager.clearHex(hex);
        tileManager.waitForHex(hex);
    }
}
class ImageAlpha {
    Image img;
    float alpha;

    public ImageAlpha(Image i, float a) { img = i; alpha = a; }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) return false;
        ImageAlpha a = (ImageAlpha)o;
        return  a.alpha == alpha && a.img.equals(img);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + (this.img != null ? this.img.hashCode() : 0);
        hash = 71 * hash + Float.floatToIntBits(this.alpha);
        return hash;
    }
}
