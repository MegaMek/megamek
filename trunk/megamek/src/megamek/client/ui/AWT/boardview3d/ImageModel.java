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

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Vector;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Shape3D;
import javax.vecmath.Point3d;

/**
 *
 * @author jwalt
 */
class ImageModel extends Shape3D {
    private static final int UP = 0;
    private static final int RIGHT = 1;
    private static final int DOWN = 2;
    private static final int LEFT = 3;
    private static final int ALPHA_THRESHOLD = 128;
    private static final double MODEL_SIZE_CORRECTION = 1.5;
    
    public ImageModel(BufferedImage img, boolean scale) {
        super();
        GeometryArray geoms[] = tracePixels(img, scale);
        for (GeometryArray geom : geoms) {
            addGeometry(geom);
        }
        double p = .5;
        if (scale) p *= MODEL_SIZE_CORRECTION;
        this.setBounds(new BoundingBox(new Point3d(-p, -p, -p), new Point3d(p, p, p)));
    }
    
    private static final GeometryArray[] tracePixels(BufferedImage img, boolean scale) {
        WritableRaster r = img.getAlphaRaster();
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = new int[w*h];
        r.getPixels(0, 0, w, h, pixels);
        
        boolean[][][] border = new boolean[h+1][w+1][4];
        boolean[] set = new boolean[4];

        // Pass 1: edge detection, creates an array with orthogonal proto-vectors
        for (int y = 0; y <= h; y++) {
            for (int x = 0; x <= w; x++) {
                set[0] = (x > 0 && y > 0 && pixels[(y-1)*w+(x-1)] > ALPHA_THRESHOLD);
                set[1] = (x < w && y > 0 && pixels[(y-1)*w+x] > ALPHA_THRESHOLD);
                set[2] = (x > 0 && y < h && pixels[y*w+(x-1)] > ALPHA_THRESHOLD);
                set[3] = (x < w && y < h && pixels[y*w+x] > ALPHA_THRESHOLD);
                border[y][x][UP] = (!set[0] && set[1]);
                border[y][x][RIGHT] = (set[3] && !set[1]);
                border[y][x][DOWN] = (!set[3] && set[2]);
                border[y][x][LEFT] = (set[0] && !set[2]);
            }
        }

        Vector<Vector<int[]>> contours = new Vector<Vector<int[]>>();
        Vector<Integer> scounts = new Vector<Integer>();
        int total = 0;
        // Pass 2: extract contours, creating diagonal vectors if possible
        for (int y = 0; y <= h; y++) {
            for (int x = 0; x <= w; x++) {
                Vector<int[]> cur, in;
                if (border[y][x][UP]) {
                    int strips = 1;
                    byte[][] removed = new byte[h+1][w+1];
                    cur = getContour(border, x, y, UP, removed);
                    simplify(cur);
                    // add interiors first to get front and back face right
                    while ((in = getInterior(border, x, y, removed)) != null) {
                        simplify(in);
                        total += in.size();
                        contours.add(in);
                        strips++;
                    }
                    total += cur.size();
                    contours.add(cur);
                    scounts.add(new Integer(strips));
                }
            }
        }

        // Pass 3: build geometry arrays
        GeometryArray[] geom = new GeometryArray[3];

        int ccnt = contours.size();
        int[] count = new int[ccnt];

        // sides
        double[] vertices = new double[total*3*2];
        float[] texcoords = new float[total*2*2];
        double vw = w*2, vh = h*2, vo = .5;
        if (scale) {
            vw /= MODEL_SIZE_CORRECTION;
            vh /= MODEL_SIZE_CORRECTION;
            vo *= MODEL_SIZE_CORRECTION;
        }
        float tw = w*2, th = h*2;
        int voff = 0, toff = 0;
        for (int i = 0; i < ccnt; i++) {
            Vector<int[]> contour = contours.elementAt(i);
            count[i] = contour.size()*2;
            for (int j = 0; j < count[i]/2; j++) {
                int[] v = contour.elementAt(j);
                vertices[voff++] = v[0]/vw-vo;
                vertices[voff++] = -v[1]/vh+vo;
                vertices[voff++] = -0.5;
                vertices[voff++] = v[0]/vw-vo;
                vertices[voff++] = -v[1]/vh+vo;
                vertices[voff++] = 0.5;
                texcoords[toff++] = v[0]/tw;
                texcoords[toff++] = 1.0f-v[1]/th;
                texcoords[toff++] = v[0]/tw;
                texcoords[toff++] = 1.0f-v[1]/th;
            }
        }

        GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);

        gi.setCoordinates(vertices);
        gi.setStripCounts(count);
        gi.setTextureCoordinateParams(1, 2);
        gi.setTextureCoordinates(0, texcoords);

        NormalGenerator ng = new NormalGenerator();
        ng.generateNormals(gi);
        
        Stripifier st = new Stripifier();
        st.stripify(gi);

        geom[0] = gi.getGeometryArray();
        
        // top
        int[] strips = new int[scounts.size()];
        for (int i = 0; i < strips.length; i++) {
            strips[i] = scounts.elementAt(i).intValue();
        }
        count = new int[ccnt];
        vertices = new double[total*3];
        texcoords = new float[total*2];
        float[] normals = new float[total*3];
        voff = 0;
        toff = 0;
        int noff = 0;
        for (int i = 0; i < ccnt; i++) {
            Vector<int[]> contour = contours.elementAt(i);
            count[i] = contour.size();
            // reverse path to get front and back face right
            for (int j = count[i]-1; j >= 0; j--) {
                int[] v = contour.elementAt(j);
                vertices[voff++] = v[0]/vw-vo;
                vertices[voff++] = -v[1]/vh+vo;
                vertices[voff++] = 0.5;
                normals[noff++] = 0.0f;
                normals[noff++] = 0.0f;
                normals[noff++] = 1.0f;
                texcoords[toff++] = v[0]/tw;
                texcoords[toff++] = 1.0f-v[1]/th;
            }
        }

        gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);

        gi.setCoordinates(vertices);
        gi.setNormals(normals);
        gi.setContourCounts(strips);
        gi.setStripCounts(count);
        gi.setTextureCoordinateParams(1, 2);
        gi.setTextureCoordinates(0, texcoords);

        st.stripify(gi);

        geom[1] = gi.getIndexedGeometryArray(false);

        voff = toff = noff = 0;
        // bottom
        for (int i = 0; i < ccnt; i++) {
            Vector<int[]> contour = contours.elementAt(i);
            count[i] = contour.size();
            // don't reverse path to get front and back face right
            for (int j = 0; j < count[i]; j++) {
                int[] v = contour.elementAt(j);
                vertices[voff++] = v[0]/vw-vo;
                vertices[voff++] = -v[1]/vh+vo;
                vertices[voff++] = -0.5;
                normals[noff++] = 0.0f;
                normals[noff++] = 0.0f;
                normals[noff++] = -1.0f;
                texcoords[toff++] = v[0]/tw;
                texcoords[toff++] = 1.0f-v[1]/th;
            }
        }

        gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);

        gi.setCoordinates(vertices);
        gi.setNormals(normals);
        gi.setContourCounts(strips);
        gi.setStripCounts(count);
        gi.setTextureCoordinateParams(1, 2);
        gi.setTextureCoordinates(0, texcoords);

        st.stripify(gi);

        geom[2] = gi.getIndexedGeometryArray(false);
        
        return geom;
    }

    // join all adjacent colinear segments
    private static final void simplify(Vector<int[]> contour) {
        contour.removeElementAt(0);
        int[] v = contour.elementAt(contour.size()-2);
        int[] nv = contour.elementAt(contour.size()-1);
        int dX = nv[0]-v[0];
        int dY = nv[1]-v[1];
        for (int j = 0; j < contour.size();) {
            v = nv;
            nv = contour.elementAt(j);
            int ndX = nv[0]-v[0];
            int ndY = nv[1]-v[1];
            if (dX == ndX && dY == ndY) contour.removeElementAt(j>0? j-1 : contour.size()-1);
            else j++;
            dX = ndX;
            dY = ndY;
        }
        contour.add(contour.elementAt(0));
    }

    private static final int[][] next = {
        { 0, -1 },
        { 1, 0 },
        { 0, 1 },
        { -1, 0 },
    };

    private static final int RUP = 1;
    private static final int RDOWN = 2;
    private static final byte[] remove = { RUP, 0, RDOWN, 0 };

    /**
     *  trace bitmap, remove one contour from border list, note removed borders 
     *  separately, return contour vertices
     */
    private static final Vector<int[]> getContour(boolean[][][] border, int x, int y, int start, byte[][] removed) {
        Vector<int[]> contour = new Vector<int[]>();
        int nx, ny, ortho, dir = start;//, sx = x, sy = y;
        
        // find start of current segment
        nx = x - next[dir][0];
        ny = y - next[dir][1];
        while (nx >= 0 && ny >= 0 && ny < border.length && nx < border[ny].length && border[ny][nx][dir]) {
            x = nx;
            y = ny;
            nx -= next[dir][0];
            ny -= next[dir][1];
        }
        
        // check if we started on a sloped segment, adjust start
        ortho = (dir-1)&3;
        nx = x + next[ortho][0];
        ny = y + next[ortho][1];
        if (nx >= 0 && ny >= 0 && ny < border.length && nx < border[ny].length && border[ny][nx][(dir+1)&3]) {
            nx -= next[dir][0];
            ny -= next[dir][1];
            if (nx >= 0 && ny >= 0 && ny < border.length && nx < border[ny].length && border[ny][nx][dir]) {
                x += next[ortho][0];
                y += next[ortho][1];
            } else {
                dir = ortho;
            }
        } else {
            dir = ortho;
        }
        contour.add(new int[] { 2*x, 2*y });
        
        // main loop
        while (true) {
            // assumption at start: border[dir] is false
            int length = 0;
            boolean sslope = false;
            int[] eoff = { 0, 0 };

            if (border[y][x][(dir+1)&3]) dir = (dir+1)&3;
            else if (border[y][x][ortho]) dir = ortho;
            else break;
            ortho = (dir-1)&3;

            // follow new segment as long as it lasts
            while (border[y][x][dir]) {
                border[y][x][dir] = false;
                if (removed != null) removed[y][x] |= remove[dir];
                x += next[dir][0];
                y += next[dir][1];
                length++;
            }

            // this is actually a sloped segment
            if (length == 1 && border[y][x][ortho]) {
                sslope = true;
                dir = ortho;
                ortho = (dir-1)&3;
                contour.lastElement()[0] -= next[dir][0];
                contour.lastElement()[1] -= next[dir][1];
                
                // follow new segment as long as it lasts
                length = 0;
                while (border[y][x][dir]) {
                    border[y][x][dir] = false;
                    if (removed != null) removed[y][x] |= remove[dir];
                    x += next[dir][0];
                    y += next[dir][1];
                    length++;
                }
            }
            
            // check if the end is sloped
            if (border[y][x][ortho]) {
                nx = x + next[ortho][0];
                ny = y + next[ortho][1];
                if (!border[ny][nx][ortho] && !border[ny][nx][(ortho-1)&3] && (border[ny][nx][dir] || start == dir)) {
                    // add two segments if this is a concave slope
                    if (sslope) contour.add(new int[] { 2*x-length*next[dir][0], 2*y-length*next[dir][1] });
                    border[y][x][ortho] = false;
                    if (removed != null) removed[y][x] |= remove[ortho];
                    x = nx;
                    y = ny;
                    eoff = next[dir];
                    dir = ortho;
                }
            }
            contour.add(new int[] { 2*x+eoff[0], 2*y+eoff[1] });
        }

        return contour;
    }

    // find holes by scanning the inside of a removed contour, return first hole contour
    private static final Vector<int[]> getInterior(boolean[][][] border, int x, int y, byte[][] removed) {
        int inside = 0;
        for (; y < removed.length; y++) {
            for (; x < removed[y].length; x++) {
                if (inside == 1 && border[y-1][x][DOWN]) return getContour(border, x, y-1, DOWN, null);

                if (inside > 0) {
                    if (border[y][x][UP]) inside++;
                    else if (border[y-1][x][DOWN]) inside--;
                }

                if ((removed[y][x]&RUP) == RUP) inside = 1;
                else if ((removed[y-1][x]&RDOWN) == RDOWN) inside = 0;
            }
            x = 0;
        }
        return null;
    }

}
