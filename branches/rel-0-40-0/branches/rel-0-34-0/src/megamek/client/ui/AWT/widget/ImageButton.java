/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Shape;

/**
 * This class implements buttons that display a <code>java.awt.Image</code>
 * instead of a text label.
 * 
 * @author James Damour
 */
public class ImageButton extends SizedButton {

    /**
     * 
     */
    private static final long serialVersionUID = 6321828904190965119L;

    /**
     * The image that the button displays.
     */
    private Image image = null;

    /**
     * The dimensions of the image.
     */
    private Dimension imageSize = new Dimension();

    /**
     * The object that makes sure thet the image is fully rendered.
     */
    private MediaTracker tracker = new MediaTracker(this);

    /**
     * The size of the edge of the button.
     */
    public static final int EDGE = 4;

    /**
     * Construct a button that uses an image instead of a text label.
     */
    public ImageButton() {
        super();
    }

    /**
     * Construct a button uses the given image instead of a text label.
     * 
     * @param img - the <code>Image</code> the button displays. This value may
     *            be <code>null</code>.
     */
    public ImageButton(Image img) {
        super();
        if (null != img)
            setImage(img);
    }

    /**
     * Get the image displayed on this button.
     * 
     * @return the <code>Image</code> that this button displays. This value
     *         may be <code>null</code>.
     */
    public Image getImage() {
        return image;
    }

    /**
     * Set the image displayed on this button. <p/> Please note!!! Do not call
     * this method within an event handler for the same <code>ImageButton</code>
     * object or the image may not get drawn properly.
     * 
     * @param img - the <code>Image</code> that this button displays. This
     *            value may be <code>null</code>.
     */
    public synchronized void setImage(Image img) {

        // Save the passed image.
        image = img;

        // We need to repaint ourself.
        repaint();

        // Return if we are clearing the image.
        if (null == image)
            return;

        // Start a tracker to see if the image is fully rendered.
        tracker.addImage(image, 0);
    }

    /**
     * Paint the image as the base of the button (if we have one). <p/>
     * Overrides <code>java.awt.Component#paint(Graphics).
     *
     * @param   g - the <code>Graphics</code> needing update.
     */
    public void paint(Graphics g) {

        // Paint the button default.
        super.paint(g);

        // Do we have an image to paint?
        if (null != image) {

            try {
                // Render the image when necessary.
                // Resize the button as needed.
                if (!tracker.checkID(0)) {
                    tracker.waitForID(0);

                    // Get the size of the image.
                    imageSize.width = image.getWidth(this);
                    imageSize.height = image.getHeight(this);

                    // Set the preferred size of this
                    // button to the size of the image.
                    setPreferredSize(imageSize);

                }

                // Get the *actual* size of this button
                // (which may differ from the image size).
                Dimension size = getSize();

                // If the button is larger than the image, center the image.
                Point start = new Point(0, 0);
                if (size.width > imageSize.width) {
                    start.x = (size.width - imageSize.width) / 2;
                }
                if (size.height > imageSize.height) {
                    start.y = (size.height - imageSize.height) / 2;
                }

                // Make sure that there's room to paint the edge.
                Shape oldClip = g.getClip();
                g.setClip(EDGE, EDGE, size.width - 2 * EDGE, size.height - 2
                        * EDGE);

                // Paint the image.
                g.drawImage(image, start.x, start.y, imageSize.width,
                        imageSize.height, this);
                g.setClip(oldClip);

            } catch (InterruptedException err) {
                // Print an error message and clear the image.
                System.err.println("Could not load image:"); //$NON-NLS-1$
                System.err.println(err.getMessage());
                image = null;
            }

        } // End have-image

    }

}
