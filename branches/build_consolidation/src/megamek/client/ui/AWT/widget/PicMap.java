/**
 * MegaMek - Copyright (C) 2000,2001,2002,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT.widget;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Vector;

/**
 * PicMap is a lightweight component, which area is composed by the set of cutom
 * elements added to PicMap Engine. There are three main groups of cutom
 * elements<br>
 * 1) BackgroundDrawers<br>
 * 2) Hot areas<br>
 * 3) Labels<br> * Hot areas and labels can be grouped handled together by
 * AreasGroup class. Content of PicMap - Areas group that includes all areas on
 * the stage. Added Elements are placed into several layers within PicMap
 * engine.
 * <ul>
 * <li>Bottom layer is BackgroundDrawers.
 * <li>Next is layer of all elements that not implements PMHotArea or PMLAbel
 * interfaces.
 * <li>On top of that is layer of Hot Areas - elements with extended
 * functionality.
 * <li>Topmost layer is layer of labels.
 * </ul>
 * Within single layer elements are drawing in the order they added to PicMap.
 */
public abstract class PicMap extends Component {
    /**
     *
     */
    private static final long serialVersionUID = -822818427765592128L;
    // Vector of Background Drawers
    private Vector<BackGroundDrawer> bgDrawers = new Vector<BackGroundDrawer>();
    // Group of other areas which does not implement PMHotArea or PMLAbel
    private PMAreasGroup otherAreas = new PMAreasGroup();
    // Hot areas
    private PMAreasGroup hotAreas = new PMAreasGroup();
    // Labels
    private PMAreasGroup labels = new PMAreasGroup();
    // Number of Hot areas on stage
    private int areascount = 0;
    // Root groop of hot areas (required for general operations)
    private PMAreasGroup rootGroup = new PMAreasGroup();
    // Offscreen image
    private Image offScr;
    // Margins
    private int topMargin = 0;
    private int leftMargin = 0;
    private int bottomMargin = 0;
    private int rightMargin = 0;

    // Pointer to Hot Area under mouse
    private PMHotArea activeHotArea = null;

    // Minimum size
    int minWidth = 1;
    int minHeight = 1;

    // Is background opaque
    private boolean bgIsOpaque = true;

    /**
     * creates PicMap engine. If no areas, labels or Backround-drawers added
     * this is just transparent layer over container.
     */
    public PicMap() {
        rootGroup.addArea(otherAreas);
        rootGroup.addArea(hotAreas);
        rootGroup.addArea(labels);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK
                | AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.COMPONENT_EVENT_MASK);
    }

    /**
     * onResize() function is calling every time PicMap is resized. Have to be
     * implemented directly to manage composition of component on resizing.
     */

    public abstract void onResize();

    /**
     * Adds element to PicMap component. Please note, that all objects
     * implementing PMLabel interface will be placed in the topmost layer. All
     * objects implementing PMHotArea will be placed in the middle layer. All
     * others are going to bottom layer. Within same layer objects are drawing
     * by order they added to components.
     */
    public void addElement(PMElement e) {
        if (e instanceof PMLabel) {
            labels.addArea(e);
        } else if (e instanceof PMHotArea) {
            hotAreas.addArea(e);
            areascount++;
        } else if (e instanceof PMAreasGroup) {
            PMAreasGroup ag = (PMAreasGroup) e;
            Enumeration<PMElement> iter = ag.elements();
            while (iter.hasMoreElements()) {
                addElement(iter.nextElement());
            }
        } else {
            otherAreas.addArea(e);
        }

    }

    /**
     * Removes element from PicMap component.
     */

    public void removeElement(PMElement e) {
        if (e instanceof PMLabel) {
            labels.removeArea(e);
        } else if (e instanceof PMHotArea) {
            if (hotAreas.removeArea(e)) {
                areascount--;
            }
        } else {
            otherAreas.removeArea(e);
        }

    }

    /**
     * Removes all elements from PicMap component.
     */

    public void removeAll() {
        otherAreas.removeAll();
        hotAreas.removeAll();
        labels.removeAll();
        bgDrawers.removeAllElements();
        areascount = 0;
        activeHotArea = null;
    }

    /**
     * Adds background drawer to the stage. Background drawers are drawn in
     * order they added to the component.
     */

    public void addBgDrawer(BackGroundDrawer bd) {
        bgDrawers.addElement(bd);

    }

    /**
     * Removes Background drawer from component.
     */

    public void removeBgDrawer(BackGroundDrawer bd) {
        bgDrawers.removeElement(bd);
    }

    /**
     * Sets margins in pixels around Content of component. Does not affect
     * Backgroun Drawers.
     *
     * @param l Left margin
     * @param t Top margin
     * @param r Right margin
     * @param b Bottom margin
     */

    public void setContentMargins(int l, int t, int r, int b) {
        leftMargin = (l < 0) ? 0 : l;
        topMargin = (t < 0) ? 0 : t;
        rightMargin = (r < 0) ? 0 : r;
        bottomMargin = (b < 0) ? 0 : b;
        Rectangle rect = rootGroup.getBounds();
        rootGroup.translate(leftMargin - rect.x, topMargin - rect.y);

    }

    /**
     * Returns Rectangle bounding content of component
     */

    public Rectangle getContentBounds() {
        return rootGroup.getBounds();
    }

    /**
     * Please remember to add super.addNotify() when overriding
     */

    @Override
    public void addNotify() {
        super.addNotify();
        update();
    }

    /**
     * Updates all changes in areas state and repaints component.
     */

    public void update() {
        if (bgIsOpaque) {
            int w = Math.max(getSize().width, minWidth);
            int h = Math.max(getSize().height, minHeight);
            offScr = createImage(w, h);
            if (offScr == null) {
                return;
            }
            Graphics g = offScr.getGraphics();
            drawInto(g);
            repaint();
            g.dispose();
        } else {
            repaint();
        }
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        if (bgIsOpaque) {
            // If we want to use buffering Component will be with opaque
            // background
            g.drawImage(offScr, 0, 0, null);
        } else {
            // Disrectly drawing to the place (use buffering in conainer)
            // Makes background of PicMap transparent
            drawInto(g);
        }
    }

    private void drawInto(Graphics g) {
        int w = Math.max(getSize().width, minWidth);
        int h = Math.max(getSize().height, minHeight);
        // Background painting
        Enumeration<BackGroundDrawer> iter = bgDrawers.elements();
        while (iter.hasMoreElements()) {
            BackGroundDrawer bgd = iter.nextElement();
            bgd.drawInto(g, w, h);
        }
        Shape oldClip = g.getClip();
        g.setClip(new Rectangle(leftMargin, topMargin, w - leftMargin
                - rightMargin, h - topMargin - bottomMargin));

        // Hot areas painting
        hotAreas.drawInto(g);
        if (activeHotArea != null) {
            activeHotArea.drawInto(g);
        }
        labels.drawInto(g);
        g.setClip(oldClip);

    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    @Override
    public Dimension getMinimumSize() {
        Rectangle r = rootGroup.getBounds();
        if (r != null) {
            return new Dimension(r.x + r.width + rightMargin, r.y + r.height
                    + bottomMargin);
        }
        return new Dimension(minWidth, minHeight);
    }

    /**
     * Returns Hot Area under coordinates (x, y)
     */

    public PMHotArea getAreaUnder(int x, int y) {
        // Have to check all elements of hotAreas vector
        // from end to start. Compare against zero works faster.
        for (int i = (areascount - 1); i >= 0; i--) {
            PMHotArea ha = (PMHotArea) hotAreas.elementAt(i);
            if ((ha != null) && intersects(ha.getAreaShape(), x, y)) {
                return ha;
            }
        }
        return null;
    }

    private boolean intersects(Shape sh, int x, int y) {
        if (sh instanceof Rectangle) {
            Rectangle r = (Rectangle) sh;
            return r.contains(x, y);
        } else if (sh instanceof Polygon) {
            Polygon p = (Polygon) sh;
            return p.contains(x, y);
        }
        return false;
    }

    /**
     * Sets background of PicMap to fully opaque or fully transparent. Notes:
     * Setting Background opaque to "false" switch off buffering of PicMap.
     * Please provide appropriate graphic buffering in container. Notes: Setting
     * Background opaque to "false" does not prevent draw of BackgroundDrawers
     * in PicMap component. Notes: It is required only for Java1.1. Under
     * Java1.3 and up offscreen will be transparent by default.
     */

    public void setBackgroundOpaque(boolean v) {
        bgIsOpaque = v;
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        PMHotArea ha = getAreaUnder(e.getX(), e.getY());
        switch (e.getID()) {
            case MouseEvent.MOUSE_CLICKED:
                if (ha != null) {
                    ha.onMouseClick(e);
                }
                break;
            case MouseEvent.MOUSE_PRESSED:
                if (ha != null) {
                    ha.onMouseDown(e);
                }
                break;
            case MouseEvent.MOUSE_RELEASED:
                if (ha != null) {
                    ha.onMouseUp(e);
                }
                break;
        }
        update();
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        switch (e.getID()) {
            case MouseEvent.MOUSE_MOVED:
                PMHotArea ha = getAreaUnder(e.getX(), e.getY());
                if (ha != activeHotArea) {
                    if (activeHotArea != null) {
                        activeHotArea.onMouseExit(e);
                    }
                    activeHotArea = ha;
                    if (ha != null) {
                        ha.onMouseOver(e);
                        setCursor(ha.getCursor());
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }
                    update();
                }
                break;
        }
    }

    @Override
    protected void processComponentEvent(ComponentEvent e) {
        switch (e.getID()) {
            case ComponentEvent.COMPONENT_RESIZED:
                onResize();
                update();
                break;
        }
    }
}
