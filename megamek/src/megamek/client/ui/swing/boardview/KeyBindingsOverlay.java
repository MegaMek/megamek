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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.common.IGame;
import megamek.common.IGame.Phase;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.util.ImageUtil;

/** 
 * An overlay for the Boardview that displays a selection of keybinds
 * for the current game situation 
 * 
 * @author SJuliez
 */
public class KeyBindingsOverlay implements IDisplayable {
    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final int DIST_TOP = 30;
    private static final int DIST_SIDE = 30;
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 5;
    private static final Color TEXT_COLOR = new Color(200, 250, 200);
    private static final Color SHADOW_COLOR = Color.DARK_GRAY;
    private static final Color BG_COLOR = new Color(80, 80, 80, 200);
    private static final float FADE_SPEED = 0.2f;
    
    /** The keybinds to be shown during the firing phases (incl. physical etc.) */
    private static final List<KeyCommandBind> BINDS_FIRE = Arrays.asList(
            KeyCommandBind.NEXT_WEAPON,
            KeyCommandBind.PREV_WEAPON,
            KeyCommandBind.FIELD_FIRE,
            KeyCommandBind.NEXT_TARGET,
            KeyCommandBind.NEXT_TARGET_VALID,
            KeyCommandBind.NEXT_TARGET_NOALLIES,
            KeyCommandBind.NEXT_TARGET_VALID_NO_ALLIES
            );

    /** The keybinds to be shown during the movement phase */
    private static final List<KeyCommandBind> BINDS_MOVE = Arrays.asList(
            KeyCommandBind.MOVE_ENVELOPE,
            KeyCommandBind.TOGGLE_MOVEMODE,
            KeyCommandBind.TOGGLE_CONVERSIONMODE
            );

    /** The keybinds to be shown in all phases during the local player's turn */
    private static final List<KeyCommandBind> BINDS_MY_TURN = Arrays.asList(
            KeyCommandBind.CANCEL, 
            KeyCommandBind.DONE, 
            KeyCommandBind.NEXT_UNIT,
            KeyCommandBind.PREV_UNIT,
            KeyCommandBind.CENTER_ON_SELECTED
            );

    /** The keybinds to be shown in all phases during any player's turn */
    private static final List<KeyCommandBind> BINDS_ANY_TURN = Arrays.asList(
            KeyCommandBind.TOGGLE_CHAT,
            KeyCommandBind.TOGGLE_ISO,
            KeyCommandBind.TOGGLE_DRAW_LABELS,
            KeyCommandBind.TOGGLE_HEX_COORDS
            );
    
    /** The keybinds to be shown in the Board Editor */
    private static final List<KeyCommandBind> BINDS_BOARD_EDITOR = Arrays.asList(
            KeyCommandBind.TOGGLE_ISO,
            KeyCommandBind.TOGGLE_HEX_COORDS
            );

    private static final List<String> ADDTL_BINDS = Arrays.asList(
            Messages.getString("KeyBindingsDisplay.fixedBinds").split("\n"));
    
    private static final List<String> ADDTL_BINDS_BOARD_EDITOR = Arrays.asList(
            Messages.getString("KeyBindingsDisplay.fixedBindsBoardEd").split("\n"));


    ClientGUI clientGui;

    /** True when the overlay is displayed or fading in. */
    private boolean visible;
    /** True indicates the strings should be redrawn. */
    private boolean changed = true;
    /** The cached image for this Display. */
    Image displayImage;
    /** The current game phase. */
    Phase currentPhase;
    /** True while fading in this overlay. */
    private boolean fadingIn = false;
    /** True while fading out this overlay. */
    private boolean fadingOut = false;
    /** The transparency of the overlay. Only used while fading in/out. */
    private float alpha = 1;

    /** 
     * An overlay for the Boardview that displays a selection of keybinds
     * for the current game situation. 
     */
    public KeyBindingsOverlay(IGame game, ClientGUI cg) {
        visible = GUIPreferences.getInstance().getBoolean(GUIPreferences.SHOW_KEYBINDS_OVERLAY);
        currentPhase = game.getPhase();
        game.addGameListener(gameListener);
        clientGui = cg;
    }

    @Override
    public void draw(Graphics graph, Rectangle clipBounds) {
        if (!visible && !isSliding()) {
            return;
        }
        
        // At startup, phase and turn change and when the keybinds change, 
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
            intGraph.setColor(BG_COLOR);
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
        
        // draw the cached image to the boardview
        // uses Composite to draw the image with variable transparency
        if (alpha < 1) {
            // Save the former composite and set an alpha blending composite
            Composite saveComp = ((Graphics2D) graph).getComposite();
            int type = AlphaComposite.SRC_OVER;
            ((Graphics2D) graph).setComposite(AlphaComposite.getInstance(type, alpha));
            graph.drawImage(displayImage, clipBounds.x + DIST_SIDE, clipBounds.y + DIST_TOP, null);
            ((Graphics2D) graph).setComposite(saveComp);
        } else {
            graph.drawImage(displayImage, clipBounds.x + DIST_SIDE, clipBounds.y + DIST_TOP, null);
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
        
        KeyCommandBind kcb = KeyCommandBind.TOGGLE_KEYBIND_DISPLAY;
        String mod = KeyEvent.getKeyModifiersText(kcb.modifiers);
        String key = KeyEvent.getKeyText(kcb.key);
        String toggleKey = (mod.isEmpty() ? "" : mod + "+") + key;
        result.add(Messages.getString("KeyBindingsDisplay.heading", toggleKey));
        
        if (clientGui != null) {
            // In a game, not the Board Editor
            // Most of the keybinds are only active during the local player's turn 
            if ((clientGui.getClient() != null) && (clientGui.getClient().isMyTurn())) {
                List<KeyCommandBind> listForPhase = new ArrayList<>();
                switch (currentPhase) {
                case PHASE_MOVEMENT:
                    listForPhase = BINDS_MOVE;
                    break;
                case PHASE_FIRING:
                case PHASE_OFFBOARD:
                case PHASE_PHYSICAL:
                    listForPhase = BINDS_FIRE;
                    break;
                default:
                }

                result.addAll(convertToStrings(listForPhase));
                result.addAll(convertToStrings(BINDS_MY_TURN));
            }
            result.addAll(convertToStrings(BINDS_ANY_TURN));
            result.addAll(ADDTL_BINDS);
        } else {
            // Board Editor
            result.addAll(convertToStrings(BINDS_BOARD_EDITOR));
            result.addAll(ADDTL_BINDS_BOARD_EDITOR);
        }

        return result;
    }
    
    /** Converts a list of KeyCommandBinds to a list of formatted strings. */
    private List<String> convertToStrings(List<KeyCommandBind> kcbs) {
        List<String> result = new ArrayList<>();
        for (KeyCommandBind kcb: kcbs) {
            String label = Messages.getString("KeyBinds.cmdNames." + kcb.cmd);
            String mod = KeyEvent.getKeyModifiersText(kcb.modifiers);
            String key = KeyEvent.getKeyText(kcb.key);
            result.add(label + ": " + (mod.isEmpty() ? "" : mod + "+") + key);
        }
        return result;
    }
    
    /** 
     * Draws the String s to the Graphics graph at position x,y 
     * with a shadow. If the string starts with #789ABC then 789ABC 
     * is converted to a color to write the rest of the text,
     * otherwise TEXT_COLOR is used.
     */
    private void drawShadowedString(Graphics graph, String s, int x, int y) {
        Color textColor = TEXT_COLOR;
        // Extract a color code from the start of the string
        // used to display headlines if it's there
        if (s.startsWith("#") && s.length() > 7) {
            try {
                int red = Integer.parseInt(s.substring(1,3), 16);
                int grn = Integer.parseInt(s.substring(3,5), 16);
                int blu = Integer.parseInt(s.substring(5,7), 16);
                textColor = new Color(red, grn, blu);
            } catch (NumberFormatException e) {
                e.printStackTrace();
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
        GUIPreferences.getInstance().setValue(GUIPreferences.SHOW_KEYBINDS_OVERLAY, vis);
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
    
    /** Detects phase and turn changes to display only relevant keybinds. */
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

}
