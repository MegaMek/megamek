/**
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.swing.widget;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;

/**
 * BufferedPanel required for use PicMap with AWT. To avoid
 * flicker.
 * It is possible to add set of Background Drawers to the JPanel.
 */

public class BufferedPanel extends JPanel implements ComponentListener {

    //Vector of Background Drawers
    private ArrayList<BackGroundDrawer> bgDrawers = new ArrayList<BackGroundDrawer>();

    public BufferedPanel() {
        addComponentListener(this);
    }

    public BufferedPanel(LayoutManager layout) {
        super(layout);
        addComponentListener(this);
    }

    /**
     * Adds background drawer.
     * Background drawers are drawn in order they added to the component.
     */

    public void addBgDrawer(BackGroundDrawer bd) {
        bgDrawers.add(bd);
    }

    /**
     * Paint the panel. Must call super.paintComponent() from any subclass that
     * wished to override this to ensure any contained lightweight components
     * get repainted.
     *
     * @param g - the <code>Graphics</code> to draw onto.
     *          This value may be <code>null</code>.
     */
    public void paintComponent(Graphics g) {
        // No Graphics, no painting.
        if (g == null) {
            return;
        }
        // create an off-screen image
        Image offScr = createImage(getSize().width, getSize().height);
        // Get a Graphics object to draw with.
        Graphics offG = offScr.getGraphics();
        // set clipping to current size.
        offG.setClip(0, 0, getSize().width, getSize().height);
        // Clear the panel as needed 
        clearGraphics(offG);
        //Draw background
        for (BackGroundDrawer bgd : bgDrawers) {
            bgd.drawInto(offG, getSize().width, getSize().height);
        }
        // Let the parent panel repaint the components inside.
        super.paintComponent(offG);
        // draw the off-screen image to the sreen.
        g.drawImage(offScr, 0, 0, null);

        //crean up the local graphics reference.
        offG.dispose();
    }

    private void clearGraphics(Graphics offG) {
        Color c = offG.getColor();
        offG.setColor(getBackground());
        offG.fillRect(0, 0, getSize().width, getSize().height);
        offG.setColor(c);
    }

    // Required component listener methods...

    public void componentResized(ComponentEvent e) {
        repaint();
    }

    public void componentMoved(ComponentEvent e) {
        repaint();
    }

    public void componentShown(ComponentEvent e) {
        repaint();
    }

    public void componentHidden(ComponentEvent e) {
        repaint();
    }
}
