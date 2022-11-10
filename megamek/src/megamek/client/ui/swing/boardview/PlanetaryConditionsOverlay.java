/*  
* MegaMek - Copyright (C) 2020 - The MegaMek Team  
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.common.Game;
import megamek.common.KeyBindParser;
import megamek.common.PlanetaryConditions;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.ImageUtil;
import org.apache.logging.log4j.LogManager;

/** 
 * An overlay for the Boardview that displays a selection of Planetary Conditions
 * for the current game situation 
 * 
 *
 */
public class PlanetaryConditionsOverlay implements IDisplayable, IPreferenceChangeListener {
    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final int DIST_TOP = 30;
    private static final int DIST_SIDE = 500;
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 5;
    private static final Color SHADOW_COLOR = Color.DARK_GRAY;
    private static final float FADE_SPEED = 0.2f;

    ClientGUI clientGui;

    /** True when the overlay is displayed or fading in. */
    private boolean visible;
    /** True indicates the strings should be redrawn. */
    private boolean changed = true;
    /** The cached image for this Display. */
    Image displayImage;
    /** The current game phase. */
    GamePhase currentPhase;

    Game currentGame;
    /** True while fading in this overlay. */
    private boolean fadingIn = false;
    /** True while fading out this overlay. */
    private boolean fadingOut = false;
    /** The transparency of the overlay. Only used while fading in/out. */
    private float alpha = 1;

    private static final String PC_OVERLAY_HEADING = Messages.getString("PlanetaryConditionsOverlay.heading");
    private static final String PC_OVERLAY_TEMPERATURE = Messages.getString("PlanetaryConditionsOverlay.Temperature");
    private static final String PC_OVERLAY_GRAVITY = Messages.getString("PlanetaryConditionsOverlay.Gravity");
    private static final String PC_OVERLAY_LIGHT = Messages.getString("PlanetaryConditionsOverlay.Light");
    private static final String PC_OVERLAY_ATMOSPHERICPREASSURE = Messages.getString("PlanetaryConditionsOverlay.AtmosphericPressure");
    private static final String PC_OVERLAY_EMI = Messages.getString("PlanetaryConditionsOverlay.EMI");
    private static final String PC_OVERLAY_WEATHER = Messages.getString("PlanetaryConditionsOverlay.Weather");
    private static final String PC_OVERLAY_WIND = Messages.getString("PlanetaryConditionsOverlay.Wind");
    private static final String PC_OVERLAY_DIRECTION = Messages.getString("PlanetaryConditionsOverlay.WindDirection");
    private static final String PC_OVERLAY_FOG = Messages.getString("PlanetaryConditionsOverlay.Fog");
    private static final String PC_OVERLAY_BLOWINGSAND = Messages.getString("PlanetaryConditionsOverlay.BlowingSand");

    /** 
     * An overlay for the Boardview that displays a selection of Planetary Conditions
     * for the current game situation. 
     */
    public PlanetaryConditionsOverlay(Game game, ClientGUI cg) {
        visible = GUIPreferences.getInstance().getBoolean(GUIPreferences.SHOW_PLANETARYCONDITIONS_OVERLAY);
        currentGame = game;
        currentPhase = game.getPhase();
        game.addGameListener(gameListener);
        clientGui = cg;
        KeyBindParser.addPreferenceChangeListener(this);
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
        if (changed) {
            changed = false;
            
            // calculate the size from the text lines, font and padding
            graph.setFont(FONT);
            FontMetrics fm = graph.getFontMetrics(FONT);
            List<String> allLines = assembleTextLines();
            Rectangle r = getSize(graph, allLines, fm);
            r = new Rectangle(r.width + 2 * PADDING_X, r.height + 2 * PADDING_Y);
            
            displayImage = ImageUtil.createAcceleratedImage(r.width, r.height);
            Graphics intGraph = displayImage.getGraphics();
            GUIPreferences.AntiAliasifSet(intGraph);

            // draw a semi-transparent background rectangle
            Color colorBG = GUIPreferences.getInstance().getPlanetaryConditionsColorBackground();
            intGraph.setColor(new Color(colorBG.getRed(), colorBG.getGreen(), colorBG.getBlue(), 200));
            intGraph.fillRoundRect(0, 0, r.width, r.height, PADDING_X, PADDING_X);
            
            // The coordinates to write the texts to
            int x = PADDING_X;
            int y = PADDING_Y + fm.getAscent();
            
            // write the strings
            for (String line: allLines) {
                drawShadowedString(intGraph, line, x, y);
                y += fm.getHeight();
            }
        }

        int DistSide = clientGui.getWidth() - 500;

        // draw the cached image to the boardview
        // uses Composite to draw the image with variable transparency
        if (alpha < 1) {
            // Save the former composite and set an alpha blending composite
            Composite saveComp = ((Graphics2D) graph).getComposite();
            int type = AlphaComposite.SRC_OVER;
            ((Graphics2D) graph).setComposite(AlphaComposite.getInstance(type, alpha));
            graph.drawImage(displayImage, clipBounds.x + DistSide, clipBounds.y + DIST_TOP, null);
            ((Graphics2D) graph).setComposite(saveComp);
        } else {
            graph.drawImage(displayImage, clipBounds.x + DistSide, clipBounds.y + DIST_TOP, null);
        }
    }

    /** Calculates the pixel size of the display from the necessary text lines. */ 
    private Rectangle getSize(Graphics graph, List<String> lines, FontMetrics fm) {
        int width = 0;
        for (String line: lines) {
            if (fm.stringWidth(line) > width) {
                width = fm.stringWidth(line);
            }
        }
        int height = fm.getHeight() * lines.size();
        return new Rectangle(width, height);
    }
    
    /** Returns an ArrayList of all text lines to be shown. */
    private List<String> assembleTextLines() {
        List<String> result = new ArrayList<>();

        Color colorTitle = GUIPreferences.getInstance().getPlanetaryConditionsColorTitle();
        Color colorHot = GUIPreferences.getInstance().getPlanetaryConditionsColorHot();
        Color colorCold = GUIPreferences.getInstance().getPlanetaryConditionsColorCold();

        KeyCommandBind kcb = KeyCommandBind.PLANETARY_CONDITIONS;
        String mod = KeyEvent.getModifiersExText(kcb.modifiers);
        String key = KeyEvent.getKeyText(kcb.key);
        String toggleKey = (mod.isEmpty() ? "" : mod + "+") + key;
        result.add(String.format("#%02X%02X%02X", colorTitle.getRed(), colorTitle.getGreen(), colorTitle.getBlue()) + MessageFormat.format(PC_OVERLAY_HEADING, toggleKey));
        
        if (clientGui != null) {
            // In a game, not the Board Editor

            String tempColor = "";
            int temp = currentGame.getPlanetaryConditions().getTemperature();

            if (currentGame.getPlanetaryConditions().isExtremeTemperatureHeat()) {
                tempColor = String.format("#%02X%02X%02X", colorHot.getRed(), colorHot.getGreen(), colorHot.getBlue());
            } else if (currentGame.getPlanetaryConditions().isExtremeTemperatureCold()) {
                tempColor = String.format("#%02X%02X%02X", colorCold.getRed(), colorCold.getGreen(), colorCold.getBlue());
            }

            boolean hideDefaultConditions = GUIPreferences.getInstance().getAdvancedPlanetaryConditionsHideDefaults();

            if (((!hideDefaultConditions) || ((hideDefaultConditions) && (currentGame.getPlanetaryConditions().isExtremeTemperature())))) {
                result.add(tempColor + PC_OVERLAY_TEMPERATURE + "  " + temp + "  " + currentGame.getPlanetaryConditions().getTemperatureIndicator());
            }

            if (((!hideDefaultConditions) || ((hideDefaultConditions) && (currentGame.getPlanetaryConditions().getGravity() != 1.0)))) {
                result.add(PC_OVERLAY_GRAVITY + "  " + currentGame.getPlanetaryConditions().getGravity() + "  " + currentGame.getPlanetaryConditions().getGravityIndicator());
            }

            if (((!hideDefaultConditions) || ((hideDefaultConditions) && (currentGame.getPlanetaryConditions().getLight() != PlanetaryConditions.L_DAY)))) {
                result.add(PC_OVERLAY_LIGHT + "  " + currentGame.getPlanetaryConditions().getLightDisplayableName() + "  " + currentGame.getPlanetaryConditions().getLightIndicator());
            }

            if (((!hideDefaultConditions) || ((hideDefaultConditions) && (currentGame.getPlanetaryConditions().getAtmosphere() != PlanetaryConditions.ATMO_STANDARD)))) {
                result.add(PC_OVERLAY_ATMOSPHERICPREASSURE + "  " + currentGame.getPlanetaryConditions().getAtmosphereDisplayableName() + "  " + currentGame.getPlanetaryConditions().getAtmosphereIndicator());
            }

            if (((!hideDefaultConditions) || ((hideDefaultConditions) && (currentGame.getPlanetaryConditions().hasEMI())))) {
                result.add(PC_OVERLAY_EMI + "  " + currentGame.getPlanetaryConditions().getEMIDisplayableValue() + "  " + currentGame.getPlanetaryConditions().getEMIIndicator());
            }

            if (((!hideDefaultConditions) || ((hideDefaultConditions) && (currentGame.getPlanetaryConditions().getWeather() != PlanetaryConditions.WE_NONE)))) {
                result.add(PC_OVERLAY_WEATHER + "  " + currentGame.getPlanetaryConditions().getWeatherDisplayableName() + "  " + currentGame.getPlanetaryConditions().getWeatherIndicator());
            }

            if (((!hideDefaultConditions) || ((hideDefaultConditions) && (currentGame.getPlanetaryConditions().getWindStrength() != PlanetaryConditions.WI_NONE)))) {
                result.add(PC_OVERLAY_WIND + "  " + currentGame.getPlanetaryConditions().getWindDisplayableName() + "   " + currentGame.getPlanetaryConditions().getWindStrengthIndicator());
                result.add(PC_OVERLAY_DIRECTION + "  " + currentGame.getPlanetaryConditions().getWindDirDisplayableName() + "  " + currentGame.getPlanetaryConditions().getWindDirectionIndicator());
            }

            if (((!hideDefaultConditions) || ((hideDefaultConditions) && (currentGame.getPlanetaryConditions().getFog() != PlanetaryConditions.FOG_NONE)))) {
                result.add(PC_OVERLAY_FOG + "  " + currentGame.getPlanetaryConditions().getFogDisplayableName() + "  " + currentGame.getPlanetaryConditions().getFogIndicator());
            }

            if (((!hideDefaultConditions) || ((hideDefaultConditions) && (currentGame.getPlanetaryConditions().isSandBlowing())))) {
                result.add(PC_OVERLAY_BLOWINGSAND + "  " + currentGame.getPlanetaryConditions().getSandBlowingDisplayableValue() + "  " + currentGame.getPlanetaryConditions().getSandBlowingIndicator());
            }
        }

        return result;
    }

    /** 
     * Draws the String s to the Graphics graph at position x, y 
     * with a shadow. If the string starts with #789ABC then 789ABC 
     * is converted to a color to write the rest of the text,
     * otherwise TEXT_COLOR is used.
     */
    private void drawShadowedString(Graphics graph, String s, int x, int y) {
        Color textColor = GUIPreferences.getInstance().getPlanetaryConditionsColorText();
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

        graph.setColor(SHADOW_COLOR);
        graph.drawString(s, x + 1, y + 1);
        graph.setColor(textColor);
        graph.drawString(s, x, y);
    }
    
    /** 
     * Activates or deactivates the overlay, fading it in or out.
     * Also saves the visibility to the GUIPreferences so 
     * MegaMek remembers it. 
     * */
    public void setVisible(boolean vis) {
        visible = vis;
        GUIPreferences.getInstance().setValue(GUIPreferences.SHOW_PLANETARYCONDITIONS_OVERLAY, vis);
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
    
    /** Detects phase and turn changes to display Planetary Conditions. */
    private GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            currentPhase = e.getNewPhase();
            changed = true;
        }
        
        @Override
        public void gameTurnChange(GameTurnChangeEvent e) {
            // The active player has changed
            changed = true;
        }
    };

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(KeyBindParser.KEYBINDS_CHANGED)) {
            changed = true;
        }
    }
}
