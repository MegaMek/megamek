/*
 * Copyright (c) 2000-2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.widget.picmap;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.JComponent;

import megamek.client.ui.widget.BackGroundDrawer;

/**
 * PicMap is a lightweight component, which area is composed by the set of custom elements added to PicMap Engine. There
 * are three main groups of custom elements<br> 1) BackgroundDrawers<br> 2) Hot areas<br> 3) Labels<br> * Hot areas and
 * labels can be grouped handled together by AreasGroup class. Content of PicMap - Areas group that includes all areas
 * on the stage. <p> Added Elements are placed into several layers within PicMap engine.
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
public abstract class PicMap extends JComponent {
    @Serial
    private static final long serialVersionUID = -1718106533001806675L;
    // Vector of Background Drawers
    private final Vector<BackGroundDrawer> backgroundDrawers = new Vector<>();
    // Group of other areas which does not implement PMHotArea or PMLAbel
    private final PMAreasGroup otherAreas = new PMAreasGroup();
    // Hot areas
    private final PMAreasGroup hotAreas = new PMAreasGroup();
    // Labels
    private final PMAreasGroup labels = new PMAreasGroup();
    // Number of Hot areas on stage
    private int areasCount = 0;
    // Root group of hot areas (required for general operations)
    private final PMAreasGroup rootGroup = new PMAreasGroup();
    // Offscreen image
    private Image offScreenImage;
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

    // Factor the drawn content is enlarged by. Areas and labels are laid out in fixed coordinates, so this scales
    // them at draw time, and mouse coordinates are scaled back when looking up the area under the pointer.
    private double displayScale = 1.0;

    /**
     * creates PicMap engine. If no areas, labels or Background-drawers added this is just transparent layer over
     * container.
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
     * onResize() function is calling every time PicMap is resized. Have to be implemented directly to manage
     * composition of component on resizing.
     */
    public abstract void onResize();

    /**
     * Adds element to PicMap component. Please note, that all objects implementing PMLabel interface will be placed in
     * the topmost layer. All objects implementing PMHotArea will be placed in the middle layer. All others are going to
     * bottom layer. Within same layer objects are drawing by order they added to components.
     */
    public void addElement(PMElement element) {
        switch (element) {
            case PMLabel ignored -> labels.addArea(element);
            case PMHotArea ignored -> {
                hotAreas.addArea(element);
                areasCount++;
            }
            case PMAreasGroup group -> {
                Enumeration<PMElement> groupElements = group.elements();
                while (groupElements.hasMoreElements()) {
                    addElement(groupElements.nextElement());
                }
            }
            case null, default -> otherAreas.addArea(element);
        }

    }

    /**
     * Removes element from PicMap component.
     */

    public void removeElement(PMElement element) {
        if (element instanceof PMLabel) {
            labels.removeArea(element);
        } else if (element instanceof PMHotArea) {
            if (hotAreas.removeArea(element)) {
                areasCount--;
            }
        } else {
            otherAreas.removeArea(element);
        }

    }

    /**
     * Removes all elements from PicMap component.
     */
    @Override
    public void removeAll() {
        otherAreas.removeAll();
        hotAreas.removeAll();
        labels.removeAll();
        backgroundDrawers.removeAllElements();
        areasCount = 0;
        activeHotArea = null;
    }

    /**
     * Adds background drawer to the stage. Background drawers are drawn in order they added to the component.
     */
    public void addBgDrawer(BackGroundDrawer drawer) {
        backgroundDrawers.addElement(drawer);
    }

    /**
     * Sets margins in pixels around Content of component. Does not affect Background Drawers.
     *
     * @param left   Left margin
     * @param top    Top margin
     * @param right  Right margin
     * @param bottom Bottom margin
     */
    public void setContentMargins(int left, int top, int right, int bottom) {
        leftMargin = Math.max(left, 0);
        topMargin = Math.max(top, 0);
        rightMargin = Math.max(right, 0);
        bottomMargin = Math.max(bottom, 0);
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
            int width = Math.max(getSize().width, minWidth);
            int height = Math.max(getSize().height, minHeight);
            offScreenImage = createImage(width, height);
            if (offScreenImage == null) {
                return;
            }
            Graphics offScreenGraphics = offScreenImage.getGraphics();
            drawInto(offScreenGraphics);
            repaint();
            offScreenGraphics.dispose();
        } else {
            repaint();
        }
    }

    @Override
    public void paintComponent(Graphics graphics) {
        if (bgIsOpaque) {
            // If we want to use buffering Component will be with opaque
            // background
            graphics.drawImage(offScreenImage, 0, 0, null);
        } else {
            // Directly drawing to the place (use buffering in container)
            // Makes background of PicMap transparent
            drawInto(graphics);
        }
    }

    /**
     * Sets how much the drawn content is enlarged by. The areas and labels keep their own fixed coordinates; only
     * drawing and mouse lookups are scaled, so a scaled component still reports the right area under the pointer.
     *
     * @param scale the factor to enlarge the content by; 1.0 draws it at its natural size
     */
    public void setDisplayScale(double scale) {
        displayScale = Math.max(0.1, scale);
        onResize();
        update();
    }

    /** @return the factor the drawn content is currently enlarged by; 1.0 is its natural size */
    public double getDisplayScale() {
        return displayScale;
    }

    private void drawInto(Graphics graphics) {
        int width = Math.max(getSize().width, minWidth);
        int height = Math.max(getSize().height, minHeight);

        // Everything is laid out in the content's own coordinates, including the background images that frame the
        // diagram, so it is all drawn through one scaled graphics context and stays aligned.
        Graphics2D contentGraphics = (Graphics2D) graphics.create();
        contentGraphics.scale(displayScale, displayScale);
        // a component not yet laid out can be smaller than one scaled pixel, and a zero size cannot be drawn into
        int contentWidth = Math.max(1, (int) (width / displayScale));
        int contentHeight = Math.max(1, (int) (height / displayScale));

        if (displayScale != 1.0) {
            // The frames around the diagram are bitmaps, and its outlines and text are drawn shapes, so enlarging
            // any of them without smoothing leaves them jagged. Unscaled drawing is left exactly as it was.
            contentGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            contentGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                  RenderingHints.VALUE_ANTIALIAS_ON);
            contentGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        // Background painting
        Enumeration<BackGroundDrawer> drawerElements = backgroundDrawers.elements();
        while (drawerElements.hasMoreElements()) {
            BackGroundDrawer drawer = drawerElements.nextElement();
            drawer.drawInto(contentGraphics, contentWidth, contentHeight);
        }

        Shape oldClip = contentGraphics.getClip();
        contentGraphics.setClip(new Rectangle(leftMargin, topMargin, contentWidth - leftMargin
              - rightMargin, contentHeight - topMargin - bottomMargin));

        // Hot areas painting
        hotAreas.drawInto(contentGraphics);
        if (activeHotArea != null) {
            activeHotArea.drawInto(contentGraphics);
        }
        contentGraphics.setClip(oldClip);
        contentGraphics.dispose();

        // Labels are drawn last and at native resolution: each is positioned and sized by the scale rather than
        // magnified through the content transform, so its text stays crisp when the diagram is enlarged.
        drawLabels(graphics, width, height);
    }

    private void drawLabels(Graphics graphics, int width, int height) {
        Graphics2D labelGraphics = (Graphics2D) graphics.create();
        if (displayScale != 1.0) {
            labelGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        labelGraphics.setClip(new Rectangle((int) (leftMargin * displayScale), (int) (topMargin * displayScale),
              width - (int) ((leftMargin + rightMargin) * displayScale),
              height - (int) ((topMargin + bottomMargin) * displayScale)));
        Enumeration<PMElement> labelElements = labels.elements();
        while (labelElements.hasMoreElements()) {
            PMElement element = labelElements.nextElement();
            if (element instanceof PMLabel label) {
                label.setDrawScale(displayScale);
            }
            element.drawInto(labelGraphics);
        }
        labelGraphics.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    @Override
    public Dimension getMinimumSize() {
        Rectangle contentBounds = rootGroup.getBounds();
        if (contentBounds != null) {
            return new Dimension((int) ((contentBounds.x + contentBounds.width + rightMargin) * displayScale),
                  (int) ((contentBounds.y + contentBounds.height + bottomMargin) * displayScale));
        }
        return new Dimension(minWidth, minHeight);
    }

    /** Returns the size available to the content, in the unscaled coordinates the content is laid out in. */
    protected Dimension getContentSize() {
        return new Dimension((int) (getSize().width / displayScale), (int) (getSize().height / displayScale));
    }

    /**
     * Returns Hot Area under coordinates (x, y)
     */
    public PMHotArea getAreaUnder(int x, int y) {
        // The areas keep their unscaled coordinates, so scale the pointer back into them.
        int contentX = (int) (x / displayScale);
        int contentY = (int) (y / displayScale);
        // Have to check all elements of hotAreas vector
        // from end to start. Compare against zero works faster.
        for (int i = (areasCount - 1); i >= 0; i--) {
            PMHotArea hotArea = (PMHotArea) hotAreas.elementAt(i);
            if ((hotArea != null) && intersects(hotArea.getAreaShape(), contentX, contentY)) {
                return hotArea;
            }
        }
        return null;
    }

    private boolean intersects(Shape shape, int x, int y) {
        if (shape instanceof Rectangle rectangle) {
            return rectangle.contains(x, y);
        } else if (shape instanceof Polygon polygon) {
            return polygon.contains(x, y);
        }
        return false;
    }

    /**
     * Sets background of PicMap to fully opaque or fully transparent. Notes: Setting Background opaque to "false"
     * switch off buffering of PicMap. Please provide appropriate graphic buffering in container. Notes: Setting
     * Background opaque to "false" does not prevent draw of BackgroundDrawers in PicMap component. Notes: It is
     * required only for Java1.1. Under Java 1.3 and up offscreen will be transparent by default.
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public void setBackgroundOpaque(boolean opaque) {
        bgIsOpaque = opaque;
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        PMHotArea hotArea = getAreaUnder(event.getX(), event.getY());
        switch (event.getID()) {
            case MouseEvent.MOUSE_CLICKED:
                if (hotArea != null) {
                    hotArea.onMouseClick(event);
                }
                break;
            case MouseEvent.MOUSE_PRESSED:
                if (hotArea != null) {
                    hotArea.onMouseDown(event);
                }
                break;
            case MouseEvent.MOUSE_RELEASED:
                if (hotArea != null) {
                    hotArea.onMouseUp(event);
                }
                break;
        }
        update();
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent event) {
        if (event.getID() == MouseEvent.MOUSE_MOVED) {
            PMHotArea hotArea = getAreaUnder(event.getX(), event.getY());
            if ((hotArea == null && activeHotArea != null)
                  || (hotArea != null && !hotArea.equals(activeHotArea))) {
                if (activeHotArea != null) {
                    activeHotArea.onMouseExit(event);
                }
                activeHotArea = hotArea;
                if (hotArea != null) {
                    hotArea.onMouseOver(event);
                    setCursor(hotArea.getCursor());
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
                update();
            }
        }
    }

    @Override
    protected void processComponentEvent(ComponentEvent event) {
        if (event.getID() == ComponentEvent.COMPONENT_RESIZED) {
            onResize();
            update();
        }
    }
}
