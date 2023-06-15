/*
 * MegaMek - Copyright (C) 2023 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.client.ui.swing.boardview;

import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Game;
import megamek.common.KeyBindParser;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.ImageUtil;
import org.apache.logging.log4j.LogManager;

import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.List;

/**
 * An overlay for the Boardview that displays a selection of Planetary Conditions
 * for the current game situation
 *
 *
 */
public abstract class AbstractBoardViewOverlay implements IDisplayable, IPreferenceChangeListener {
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 5;
    private static final Color SHADOW_COLOR = java.awt.Color.DARK_GRAY;
    private static final float FADE_SPEED = 0.2f;
    ClientGUI clientGui;
    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /** True when the overlay is displayed or fading in. */
    private boolean visible;
    /** True indicates the strings should be redrawn. */
    private boolean dirty = true;
    private boolean hasContents = false;
    /** The cached image for this Display. */
    private Image displayImage;
    /** The current game phase. */
    protected GamePhase currentPhase;
    protected final Game currentGame;

    protected final Font font;

    /** True while fading in this overlay. */
    private boolean fadingIn = false;
    /** True while fading out this overlay. */
    private boolean fadingOut = false;
    /** The transparency of the overlay. Only used while fading in/out. */
    private float alpha = 1;
    private int overlayWidth = 500;
    private int overlayHeight = 500;

    private final String header;
    /**
     * An overlay for the Boardview
     */
    public AbstractBoardViewOverlay(Game game, ClientGUI cg, Font font, String headerText ) {
        this.font = font;
        this.visible =  getVisibilityGUIPreference();
        currentGame = game;
        currentPhase = game.getPhase();
        game.addGameListener(gameListener);
        clientGui = cg;
        KeyBindParser.addPreferenceChangeListener(this);
        GUIP.addPreferenceChangeListener(this);

        Color colorTitle = GUIP.getPlanetaryConditionsColorTitle();
        header = String.format("#%02X%02X%02X %s",
                colorTitle.getRed(), colorTitle.getGreen(), colorTitle.getBlue(), headerText);
    }

    protected void addHeader(List<String> lines) {
        if (GUIP.getPlanetaryConditionsShowHeader()) {
            lines.add(header);
        }
    }

    @Override
    public void draw(Graphics graph, Rectangle clipBounds) {
        if (!visible && !isSliding()) {
            return;
        }

        if ((clientGui == null) || (currentGame == null)) {
            return;
        }

        // At startup, phase and turn change and when the Planetary Conditions change,
        // the cached image is (re)created
        if (dirty) {
            dirty = false;

            // calculate the size from the text lines, font and padding
            Font newFont = font.deriveFont(font.getSize() * GUIP.getGUIScale());
            graph.setFont(newFont);
            FontMetrics fm = graph.getFontMetrics(newFont);
            List<String> allLines = assembleTextLines();
            if (allLines.size() == 0) {
                hasContents = false;
            } else {
                hasContents = true;
                Rectangle r = getSize(graph, allLines, fm);
                r = new Rectangle(r.width + 2 * PADDING_X, r.height + 2 * PADDING_Y);
                overlayWidth = r.width;
                overlayHeight = r.height;

                displayImage = ImageUtil.createAcceleratedImage(r.width, r.height);
                Graphics intGraph = displayImage.getGraphics();
                UIUtil.setHighQualityRendering(intGraph);

                // draw a semi-transparent background rectangle
                Color colorBG = GUIP.getPlanetaryConditionsColorBackground();
                intGraph.setColor(new Color(colorBG.getRed(), colorBG.getGreen(), colorBG.getBlue(), GUIP.getPlanetaryConditionsBackgroundTransparency()));
                intGraph.fillRoundRect(0, 0, r.width, r.height, PADDING_X, PADDING_Y);

                // The coordinates to write the texts to
                int x = PADDING_X;
                int y = PADDING_Y + fm.getAscent();

                // write the strings
                for (String line : allLines) {
                    drawShadowedString(intGraph, line, x, y);
                    y += fm.getHeight();
                }
            }
        }

        if (hasContents) {
            // draw the cached image to the boardview
            // uses Composite to draw the image with variable transparency
            int distSide = getDistSide(clipBounds, overlayWidth);
            int distTop = getDistTop(clipBounds, overlayHeight);

            if (alpha < 1) {
                // Save the former composite and set an alpha blending composite
                Composite saveComp = ((Graphics2D) graph).getComposite();
                int type = AlphaComposite.SRC_OVER;
                ((Graphics2D) graph).setComposite(AlphaComposite.getInstance(type, alpha));
                graph.drawImage(displayImage, clipBounds.x + distSide, clipBounds.y + distTop, null);
                ((Graphics2D) graph).setComposite(saveComp);
            } else {
                graph.drawImage(displayImage, clipBounds.x + distSide, clipBounds.y + distTop, null);
            }
        }
    }

    /** Calculates the pixel size of the display from the necessary text lines. */
    private Rectangle getSize(Graphics graph, List<String> lines, FontMetrics fm) {
        int width = 0;
        for (String line: lines) {
            if (fm.stringWidth(line) > width) {
                if (line.startsWith("#") && line.length() > 7) {
                    line = line.substring(7);
                }

                width = fm.stringWidth(line);
            }
        }
        int height = fm.getHeight() * lines.size();
        return new Rectangle(width, height);
    }

    /** Returns an ArrayList of all text lines to be shown. */
    protected abstract List<String> assembleTextLines();

    /**
     * Draws the String s to the Graphics graph at position x, y
     * with a shadow. If the string starts with #789ABC then 789ABC
     * is converted to a color to write the rest of the text,
     * otherwise TEXT_COLOR is used.
     */
    private void drawShadowedString(Graphics graph, String s, int x, int y) {
        Color textColor = getTextColor();
        // Extract a color code from the start of the string
        // used to display headlines if it's there
        if (s.startsWith("#") && s.length() > 7) {
            try {
                int red = Integer.parseInt(s.substring(1, 3), 16);
                int grn = Integer.parseInt(s.substring(3, 5), 16);
                int blu = Integer.parseInt(s.substring(5, 7), 16);
                textColor = new Color(red, grn, blu);
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
            s = s.substring(7);
        }

        if (s.length() > 0) {
            AttributedString text = new AttributedString(s);
            text.addAttribute(TextAttribute.FONT, new Font(font.getFontName(), Font.PLAIN, (int) (font.getSize() * GUIP.getGUIScale())), 0, s.length());

            graph.setColor(SHADOW_COLOR);
            graph.drawString(text.getIterator(), x + 1, y + 1);
            graph.setColor(textColor);
            graph.drawString(text.getIterator(), x, y);
        }
    }

    /**
     * Activates or deactivates the overlay, fading it in or out.
     * Also saves the visibility to the GUIPreferences so
     * MegaMek remembers it.
     * */
    public void setVisible(boolean vis) {
        visible = vis;
        setVisibilityGUIPreference(vis);

        if (vis) {
            fadingIn = true;
            fadingOut = false;
        } else {
            fadingIn = false;
            fadingOut = true;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isSliding() {
        return fadingOut || fadingIn;
    }

    @Override
    public boolean slide() {
        if (fadingIn) {
            alpha += FADE_SPEED;
            if (alpha > 1) {
                alpha = 1;
                fadingIn = false;
            }
            return true;
        } else if (fadingOut) {
            alpha -= FADE_SPEED;
            if (alpha < 0) {
                alpha = 0;
                fadingOut = false;
            }
            return true;
        }
        return false;
    }

    /** Detects phase and turn changes to display*/
    private GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            currentPhase = e.getNewPhase();
            gameTurnOrPhaseChange();
        }

        @Override
        public void gameTurnChange(GameTurnChangeEvent e) {
            // The active player has changed
            gameTurnOrPhaseChange();
        }
    };

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        String name = e.getName();
        if (getVisibilityGUIPreference() != visible) {
            visible = getVisibilityGUIPreference();
            setDirty();
        }
    }

    protected void setDirty() {
        dirty = true;
        //TODO force boardview redraw
        clientGui.getBoardView().boardChanged();
    }

    protected void gameTurnOrPhaseChange() {
        setDirty();
    }

    public static Color getTextColor() {
        return GUIP.getPlanetaryConditionsColorText();
    }

    protected abstract void setVisibilityGUIPreference(boolean value);
    protected abstract boolean getVisibilityGUIPreference();
    protected abstract int getDistTop(Rectangle clipBounds,  int overlayHeight);
    protected abstract int getDistSide(Rectangle clipBounds, int overlayWidth);

    public static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                color.getRed(), color.getGreen(),color.getBlue(), color.getAlpha());
    }

    public static String colorToHex(Color color, float brightnessMultiplier) {
        return String.format("#%02X%02X%02X",
                (int)(color.getRed() * brightnessMultiplier), (int)(color.getGreen() *  brightnessMultiplier),
                (int)(color.getBlue() *  brightnessMultiplier), (int)(color.getAlpha()));
    }

}
