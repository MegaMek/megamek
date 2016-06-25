/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007,2008 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import megamek.common.Configuration;

/**
 * A subclass of JButton that supports specifying the look and feel of the
 * button via a SkinSpecification.
 *
 * @author arlith
 */
public class MegamekButton extends JButton {

    private static final long serialVersionUID = -3271105050872007863L;

    // Default values for button text colors
    private static final Color defaultColor = new Color(250, 250, 250);
    private static final Color defaultActiveColor = new Color(255, 255, 0);
    private static final Color defaultDisabledColor = new Color(128, 128, 128);

    /**
     * The default background image for the button,
     */
    protected ImageIcon backgroundIcon;

    /**
     * The background image to display when the button is pressed
     */
    protected ImageIcon backgroundPressedIcon;

    protected BufferedImage bgBuffer = null;
    protected BufferedImage bgPressedBuffer = null;

    /**
     * Keeps track of whether there are images to display for this button, or if
     * the default rendering for JButtons should be used
     */
    boolean iconsLoaded = false;

    /**
     * Keeps track of if the button is pressed or not. This is used for
     * determining if which image icon should be displayed.
     */
    boolean isPressed = false;

    /**
     * Keeps track of whether the mouse cursor is currently over this button.
     * Used to adjust the font of the button text.
     */
    boolean isMousedOver = false;

    /**
     * Determines if the background images should be tiled or not. If this is
     * false and the background images are smaller than the button size, they
     * will be scaled to the button size.
     */
    boolean isBGTiled = true;

    /**
     * The color of the button text.
     */
    private Color buttonColor;
    /**
     * The color of the button text when activated.
     */
    private Color activeColor;
    /**
     * The color of the button text when the button is disabled.
     */
    private Color disabledColor;

    /**
     *
     * @param text
     *            The button text
     * @param component
     *            The name of the SkinSpecification entry
     */
    public MegamekButton(String text, String component) {
        super(text);
        initialize(component);
    }

    /**
     * Default text constructor, the button will use the DefaultButton
     * SkinSpecification.
     *
     * @param text
     */
    public MegamekButton(String text) {
        super(text);
        initialize(SkinSpecification.UIComponents.DefaultButton.getComp());
    }

    /**
     * Default constructor with no button text and DefaultButton
     * SkinSpecification.
     */
    public MegamekButton() {
        super();
        initialize(SkinSpecification.UIComponents.DefaultButton.getComp());
    }

    /**
     * Initialize the state of the button, using the SkinSpecification linked to
     * the given string.
     *
     * @param component
     *            String key to get the SkinSpecification.
     */
    private void initialize(String component) {
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(component, true);
        setBorder(new MegamekBorder(skinSpec));
        loadIcon(skinSpec);
        isBGTiled = skinSpec.tileBackground;

        if (skinSpec.fontColors.size() >= 1) {
            buttonColor = skinSpec.fontColors.get(0);
        } else {
            buttonColor = defaultColor;
        }
        if (skinSpec.fontColors.size() >= 2) {
            disabledColor = skinSpec.fontColors.get(1);
        } else {
            disabledColor = defaultDisabledColor;
        }
        if (skinSpec.fontColors.size() >= 3) {
            activeColor = skinSpec.fontColors.get(2);
        } else {
            activeColor = defaultActiveColor;
        }
    }

    /**
     * Use the supplied SkinSpecification to load the background images.
     *
     * @param spec
     */
    public void loadIcon(SkinSpecification spec) {
        iconsLoaded = true;
        // If there were no background paths loaded, there's nothing to do
        if (!spec.hasBackgrounds()) {
            iconsLoaded = false;
            return;
        }
        // Setting this to false helps with transparent images
        setContentAreaFilled(false);
        // Otherwise, try to load in all of the images.
        try {
            if (spec.backgrounds.size() < 2) {
                System.out.println("Error: skin specification for a "
                        + "Megamek Button does not contain at least "
                        + "2 background images!");
                iconsLoaded = false;
            }
            java.net.URI imgURL = new File(Configuration.widgetsDir(),
                    spec.backgrounds.get(0)).toURI();
            backgroundIcon = new ImageIcon(imgURL.toURL());
            imgURL = new File(Configuration.widgetsDir(),
                    spec.backgrounds.get(1)).toURI();
            backgroundPressedIcon = new ImageIcon(imgURL.toURL());
        } catch (Exception e) {
            System.out.println("Error: loading background icons for "
                    + "a Megamekbutton!");
            System.out.println("Error: " + e.getMessage());
            iconsLoaded = false;
        }
    }

    protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_EXITED) {
            isMousedOver = false;
            repaint();
        } else if (e.getID() == MouseEvent.MOUSE_ENTERED) {
            isMousedOver = true;
        } else if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            isPressed = true;
        } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            isPressed = false;
        }
        super.processMouseEvent(e);
    }

    protected void paintComponent(Graphics g) {
        // Call super, so this components plays well with Swing
        super.paintComponent(g);
        // If none of the icons are loaded, treat this is a regular JButton
        if (!iconsLoaded) {
            return;
        }

        // If the BG icon is tiled, draw it in
        if (isBGTiled) {
            int w = getWidth();
            int h = getHeight();
            int iW = isPressed ? backgroundPressedIcon.getIconWidth()
                    : backgroundIcon.getIconWidth();
            int iH = isPressed ? backgroundPressedIcon.getIconHeight()
                    : backgroundIcon.getIconHeight();
            for (int x = 0; x < w; x += iW) {
                for (int y = 0; y < h; y += iH) {
                    if (isPressed) {
                        g.drawImage(backgroundPressedIcon.getImage(), x, y,
                                backgroundPressedIcon.getImageObserver());
                    } else {
                        g.drawImage(backgroundIcon.getImage(), x, y,
                                backgroundIcon.getImageObserver());
                    }
                }
            }
        } else { // Otherwise, treat the BG Icon as one image to overlay
            int w = getWidth();
            int h = getHeight();
            if (isPressed) {
                if (bgPressedBuffer == null || bgPressedBuffer.getWidth() != w
                        || bgPressedBuffer.getHeight() != h) {
                    bgPressedBuffer = new BufferedImage(w, h,
                            BufferedImage.TYPE_INT_ARGB);
                    bgPressedBuffer.getGraphics().drawImage(
                            backgroundPressedIcon.getImage(), 0, 0, w, h, null);
                }
                g.drawImage(bgPressedBuffer, 0, 0, null);
            } else {
                if (bgBuffer == null || bgBuffer.getWidth() != w
                        || bgBuffer.getHeight() != h) {
                    bgBuffer = new BufferedImage(w, h,
                            BufferedImage.TYPE_INT_ARGB);
                    bgBuffer.getGraphics().drawImage(backgroundIcon.getImage(),
                            0, 0, w, h, null);
                }
                g.drawImage(bgBuffer, 0, 0, null);
            }
        }

        JLabel textLabel = new JLabel(getText(), SwingConstants.CENTER);
        textLabel.setSize(getSize());
        if (this.isEnabled()) {
            if (isMousedOver) {
                Font font = textLabel.getFont();
                // same font but bold
                Font boldFont = new Font(font.getFontName(), Font.BOLD,
                        font.getSize() + 2);
                textLabel.setFont(boldFont);
                textLabel.setForeground(activeColor);
            } else {
                textLabel.setForeground(buttonColor);
            }
        } else {
            textLabel.setForeground(disabledColor);
        }
        textLabel.paint(g);
    }

    public String toString() {
        return getActionCommand();
    }

    public boolean isIconsLoaded() {
        return iconsLoaded;
    }

}
