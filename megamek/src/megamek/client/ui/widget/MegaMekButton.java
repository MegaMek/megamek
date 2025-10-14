/*
 * Copyright (C) 2000-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.Serial;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

/**
 * A subclass of JButton that supports specifying the look and feel of the button via a SkinSpecification.
 *
 * @author arlith
 */
public class MegaMekButton extends JButton implements MouseListener {
    private static final MMLogger logger = MMLogger.create(MegaMekButton.class);

    @Serial
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
     * Keeps track of whether there are images to display for this button, or if the default rendering for JButtons
     * should be used
     */
    boolean iconsLoaded = false;

    /**
     * Keeps track of if the button is pressed or not. This is used for determining which image icon should be
     * displayed.
     */
    boolean isPressed = false;

    /**
     * Keeps track of whether the mouse cursor is currently over this button. Used to adjust the font of the button
     * text.
     */
    boolean isMousedOver = false;

    /**
     * Determines if the background images should be tiled or not. If this is false and the background images are
     * smaller than the button size, they will be scaled to the button size.
     */
    boolean isBGTiled = true;

    /**
     * Determines if the button should bold the button text on mouseover.
     */
    boolean shouldBold = true;

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

    private Font specificFont;

    /**
     * @param text      The button text
     * @param component The name of the SkinSpecification entry
     */
    public MegaMekButton(String text, String component, boolean defaultToPlain) {
        super(text);
        initialize(component, defaultToPlain);
    }

    /**
     * @param text      The button text
     * @param component The name of the SkinSpecification entry
     */
    public MegaMekButton(String text, String component) {
        super(text);
        initialize(component);
    }

    /**
     * Default text constructor, the button will use the DefaultButton SkinSpecification.
     *
     */
    public MegaMekButton(String text) {
        super(text);
        initialize(SkinSpecification.UIComponents.DefaultButton.getComp());
    }

    /**
     * Default constructor with no button text and DefaultButton SkinSpecification.
     */
    public MegaMekButton() {
        super();
        initialize(SkinSpecification.UIComponents.DefaultButton.getComp());
    }

    /**
     * Initialize the state of the button, using the SkinSpecification linked to the given string.
     *
     * @param component String key to get the SkinSpecification.
     */
    private void initialize(String component) {
        initialize(component, false);
    }

    /**
     * Initialize the state of the button, using the SkinSpecification linked to the given string.
     *
     * @param component String key to get the SkinSpecification.
     */
    private void initialize(String component, boolean defaultToPlain) {
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(component, defaultToPlain, true);
        if (!skinSpec.noBorder) {
            setBorder(new MegaMekBorder(skinSpec));
        }
        loadIcon(skinSpec);
        isBGTiled = skinSpec.tileBackground;

        if (!skinSpec.fontColors.isEmpty()) {
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
        shouldBold = skinSpec.shouldBoldMouseOver;

        if (skinSpec.fontName != null) {
            specificFont = new Font(skinSpec.fontName, Font.PLAIN, skinSpec.fontSize);
            setFont(specificFont);
        }
    }

    /**
     * Use the supplied SkinSpecification to load the background images.
     *
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
        // Otherwise, try to load in all the images.
        try {
            if (spec.backgrounds.size() < 2) {
                logger.error(
                      "Skin specification for a MegamekButton does not contain at least 2 background images!");
                iconsLoaded = false;
            }
            java.net.URI imgURL = new MegaMekFile(Configuration.widgetsDir(),
                  spec.backgrounds.get(0)).getFile().toURI();
            backgroundIcon = new ImageIcon(imgURL.toURL());
            imgURL = new MegaMekFile(Configuration.widgetsDir(),
                  spec.backgrounds.get(1)).getFile().toURI();
            backgroundPressedIcon = new ImageIcon(imgURL.toURL());
        } catch (Exception ex) {
            logger.error("Loading background icons for a MegamekButton!", ex);
            iconsLoaded = false;
        }
    }

    @Override
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

    @Override
    protected void paintComponent(Graphics g) {
        // Call super, so these components plays well with Swing
        super.paintComponent(g);
        // If none of the icons are loaded, treat this is a regular JButton
        if (!iconsLoaded) {
            return;
        }

        // If the BG icon is tiled, draw it in
        int w = getWidth();
        int h = getHeight();
        if (isBGTiled) {
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
        if (specificFont != null) {
            textLabel.setFont(specificFont);
        }
        if (this.isEnabled()) {
            if (isMousedOver || hasFocus()) {
                Font font = textLabel.getFont();
                if (shouldBold) {
                    // same font but bold
                    Font boldFont = new Font(font.getFontName(), Font.BOLD,
                          font.getSize() + 2);
                    textLabel.setFont(boldFont);
                }
                textLabel.setForeground(activeColor);
            } else {
                textLabel.setForeground(buttonColor);
            }
        } else {
            textLabel.setForeground(disabledColor);
        }
        textLabel.paint(g);
    }

    @Override
    public String toString() {
        return getActionCommand();
    }

    public boolean isIconsLoaded() {
        return iconsLoaded;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
