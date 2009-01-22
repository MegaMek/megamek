/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Vector;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.PMUtil;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.util.StringUtil;

public class UnitOverview implements IDisplayable {

    private static final String IMAGE_DIR = "data/images/widgets";

    private static final int UNKNOWN_UNITS_PER_PAGE = -1;

    /**
     * The maximum length of the icon name.
     */
    public static final int ICON_NAME_MAX_LENGTH = 52;

    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
    private static final int DIST_TOP = 5;
    private static final int DIST_SIDE = 5;
    private static final int ICON_WIDTH = 56;
    private static final int ICON_HEIGHT = 48;
    private static final int BUTTON_HEIGHT = 11;
    private static final int BUTTON_PADDING = 2;
    private static final int PADDING = 5;

    private int[] unitIds;
    private boolean isHit = false;
    private boolean visible = true;
    private boolean scroll = false;
    private int unitsPerPage = UNKNOWN_UNITS_PER_PAGE;
    private int actUnitsPerPage = 0;
    private int scrollOffset = 0;

    private ClientGUI clientgui;

    private FontMetrics fm;

    private Image scrollUp;
    private Image scrollDown;
    private Image pageUp;
    private Image pageDown;

    public UnitOverview(ClientGUI clientgui) {
        this.clientgui = clientgui;
        fm = clientgui.getFontMetrics(FONT);

        Toolkit toolkit = clientgui.getToolkit();
        scrollUp = toolkit.getImage(IMAGE_DIR + "/scrollUp.gif"); //$NON-NLS-1$
        PMUtil.setImage(scrollUp, clientgui);
        scrollDown = toolkit.getImage(IMAGE_DIR + "/scrollDown.gif"); //$NON-NLS-1$
        PMUtil.setImage(scrollDown, clientgui);
        pageUp = toolkit.getImage(IMAGE_DIR + "/pageUp.gif"); //$NON-NLS-1$
        PMUtil.setImage(pageUp, clientgui);
        pageDown = toolkit.getImage(IMAGE_DIR + "/pageDown.gif"); //$NON-NLS-1$
        PMUtil.setImage(pageDown, clientgui);
    }

    public void draw(Graphics graph, Point drawRelativeTo, Dimension size) {
        if (!visible) {
            return;
        }

        computeUnitsPerPage(size);

        graph.setFont(FONT);
        ArrayList<Entity> v = clientgui.getClient().game
                .getPlayerEntities(clientgui.getClient().getLocalPlayer(), true);
        unitIds = new int[v.size()];

        scroll = v.size() > unitsPerPage;

        actUnitsPerPage = scroll ? unitsPerPage - 1 : unitsPerPage;

        if (scrollOffset + actUnitsPerPage > unitIds.length) {
            scrollOffset = unitIds.length - actUnitsPerPage;
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
        }

        int x = size.width - DIST_SIDE - ICON_WIDTH;
        int y = DIST_TOP;

        if (scroll) {
            graph.drawImage(pageUp, x, y, null);
            graph.drawImage(scrollUp, x, y + BUTTON_HEIGHT + BUTTON_PADDING,
                    null);
            y += BUTTON_HEIGHT + BUTTON_HEIGHT + BUTTON_PADDING
                    + BUTTON_PADDING;
        }

        for (int i = scrollOffset; i < v.size()
                && i < actUnitsPerPage + scrollOffset; i++) {
            Entity e = v.get(i);
            unitIds[i] = e.getId();
            String name = getIconName(e, fm);
            Image i1 = clientgui.bv.getTilesetManager().iconFor(e);

            graph.drawImage(i1, x, y, null);
            printLine(graph, x + 3, y + 46, name);
            drawBars(graph, e, x, y);
            drawHeat(graph, e, x, y);
            drawConditionStrings(graph, e, x, y);
            graph.setColor(getFrameColor(e));
            graph.drawRect(x, y, ICON_WIDTH, ICON_HEIGHT);

            Entity se = clientgui == null ? null : clientgui.getClient()
                    .getEntity(clientgui.getSelectedEntityNum());
            if (e == se) {
                graph.drawRect(x - 1, y - 1, ICON_WIDTH + 2, ICON_HEIGHT + 2);
            }

            y += ICON_HEIGHT + PADDING;
        }

        if (scroll) {
            y -= PADDING;
            y += BUTTON_PADDING;
            graph.drawImage(scrollDown, x, y, null);
            graph.drawImage(pageDown, x, y + BUTTON_HEIGHT + BUTTON_PADDING,
                    null);
        }

    }

    public void setIdleTime(long timeIdle, boolean add) {
    }

    public boolean isHit(Point p, Dimension size) {
        if (!visible) {
            return false;
        }

        int actUnits = scroll ? unitsPerPage - 1 : unitsPerPage;

        int x = p.x;
        int y = p.y;
        int xOffset = size.width - DIST_SIDE - ICON_WIDTH;
        int yOffset = DIST_TOP;

        if (x < xOffset || x > xOffset + ICON_WIDTH || y < yOffset
                || y > yOffset + (unitsPerPage * (ICON_HEIGHT + PADDING))) {
            return false;
        }

        if (scroll) {
            if (y > yOffset && y < yOffset + BUTTON_HEIGHT) {
                pageUp();
                return true;
            }
            yOffset += BUTTON_HEIGHT + BUTTON_PADDING;
            if (y > yOffset && y < yOffset + BUTTON_HEIGHT) {
                scrollUp();
                return true;
            }
            yOffset += BUTTON_HEIGHT + BUTTON_PADDING;
        }

        for (int i = scrollOffset; i < unitIds.length
                && i < actUnits + scrollOffset; i++) {
            if (y > yOffset && y < yOffset + ICON_HEIGHT) {
                clientgui.bv.processBoardViewEvent(new BoardViewEvent(
                        clientgui.bv, BoardViewEvent.SELECT_UNIT, unitIds[i]));
                isHit = true;
                return true;
            }
            yOffset += ICON_HEIGHT + PADDING;
        }

        if (scroll) {
            yOffset -= PADDING;
            yOffset += BUTTON_PADDING;
            if (y > yOffset && y < yOffset + BUTTON_HEIGHT) {
                scrollDown();
                return true;
            }
            yOffset += BUTTON_HEIGHT + BUTTON_PADDING;
            if (y > yOffset && y < yOffset + BUTTON_HEIGHT) {
                pageDown();
                return true;
            }
        }

        return false;
    }

    public boolean isMouseOver(Point p, Dimension size) {
        return false;
    }

    public boolean isSliding() {
        return false;
    }

    public boolean slide() {
        return false;
    }

    public boolean isDragged(Point p, Dimension size) {
        return false;
    }

    public boolean isBeingDragged() {
        return false;
    }

    public boolean isReleased() {
        if (!visible) {
            return false;
        }

        if (isHit) {
            isHit = false;
            return true;
        }
        return false;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    private void drawHeat(Graphics graph, Entity entity, int x, int y) {
        if (!(entity instanceof Mech || entity instanceof Aero)) {
            return;
        }
        boolean mtHeat = false;
        int mHeat = 30;
        if (entity.getGame() != null
                && entity.getGame().getOptions().booleanOption("tacops_heat")) {
            mHeat = 50;
            mtHeat = true;
        }
        int heat = Math.min(mHeat, entity.heat);

        graph.setColor(Color.darkGray);
        graph.fillRect(x + 52, y + 4, 2, 30);
        graph.setColor(Color.lightGray);
        graph.fillRect(x + 51, y + 3, 2, 30);
        graph.setColor(Color.red);
        if (mtHeat) {
            graph.fillRect(x + 51, y + 3 + (30 - (int) (heat * 0.6)), 2,
                    (int) (heat * 0.6));
        } else {
            graph.fillRect(x + 51, y + 3 + (30 - heat), 2, heat);
        }
    }

    private void drawBars(Graphics graph, Entity entity, int x, int y) {
        // Lets draw our armor and internal status bars
        int baseBarLength = 23;
        int barLength = 0;
        double percentRemaining = 0.00;

        percentRemaining = entity.getArmorRemainingPercent();
        barLength = (int) (baseBarLength * percentRemaining);

        graph.setColor(Color.darkGray);
        graph.fillRect(x + 4, y + 4, 23, 2);
        graph.setColor(Color.lightGray);
        graph.fillRect(x + 3, y + 3, 23, 2);
        graph.setColor(getStatusBarColor(percentRemaining));
        graph.fillRect(x + 3, y + 3, barLength, 2);

        percentRemaining = entity.getInternalRemainingPercent();
        barLength = (int) (baseBarLength * percentRemaining);

        graph.setColor(Color.darkGray);
        graph.fillRect(x + 4, y + 7, 23, 2);
        graph.setColor(Color.lightGray);
        graph.fillRect(x + 3, y + 6, 23, 2);
        graph.setColor(getStatusBarColor(percentRemaining));
        graph.fillRect(x + 3, y + 6, barLength, 2);

    }

    private Color getStatusBarColor(double percentRemaining) {
        if (percentRemaining <= .25) {
            return Color.red;
        } else if (percentRemaining <= .75) {
            return Color.yellow;
        } else {
            return new Color(16, 196, 16);
        }
    }

    private Color getFrameColor(Entity entity) {
        if (!clientgui.getClient().isMyTurn() || !entity.isSelectableThisTurn()) {
            return Color.gray;
        }
        return Color.black;
    }

    private void printLine(Graphics g, int x, int y, String s) {
        if (s == null || s.trim().length() < 0) {
            return; // some Graphics objects don't like empty strings
        }
        g.setColor(Color.black);
        g.drawString(s, x + 1, y);
        g.drawString(s, x - 1, y);
        g.drawString(s, x, y + 1);
        g.drawString(s, x, y - 1);
        g.setColor(Color.white);
        g.drawString(s, x, y);
    }

    private void drawConditionStrings(Graphics graph, Entity entity, int x,
            int y) {

        //out of control conditions for ASF
        if(entity instanceof Aero) {
            Aero a = (Aero)entity;


            //draw altitude if Aero in atmosphere
            if(clientgui.getClient().game.getBoard().inAtmosphere()) {
                graph.setColor(Color.darkGray);
                graph.drawString(Integer.toString(a.getElevation()), x + 36, y + 15);
                graph.setColor(Color.PINK);
                graph.drawString(Integer.toString(a.getElevation()), x + 35, y + 14);
            }

            if(a.isRolled()) {
                // draw "rolled"
                graph.setColor(Color.darkGray);
                graph.drawString(Messages.getString("BoardView1.ROLLED"), x + 11, y+29); //$NON-NLS-1$
                graph.setColor(Color.red);
                graph.drawString(Messages.getString("BoardView1.ROLLED"), x + 10, y+28); //$NON-NLS-1$
            }

            if(a.isOutControlTotal() && a.isRandomMove()) {
                graph.setColor(Color.darkGray);
                graph.drawString(Messages.getString("UnitOverview.RANDOM"), x + 11, y + 24); //$NON-NLS-1$
                graph.setColor(Color.red);
                graph.drawString(Messages.getString("UnitOverview.RANDOM"), x + 10, y + 23); //$NON-NLS-1$
            } else if(a.isOutControlTotal()) {
                // draw "CONTROL"
                graph.setColor(Color.darkGray);
                graph.drawString(Messages.getString("UnitOverview.CONTROL"), x + 11, y + 24); //$NON-NLS-1$
                graph.setColor(Color.red);
                graph.drawString(Messages.getString("UnitOverview.CONTROL"), x + 10, y + 23); //$NON-NLS-1$
            }

            //is the unit evading? - can't evade and be out of control so just draw on top
            if(a.isEvading()) {
                //draw evasion
                graph.setColor(Color.darkGray);
                graph.drawString(Messages.getString("UnitOverview.EVADE"), x +11, y + 24); //$NON-NLS-1$
                graph.setColor(Color.red);
                graph.drawString(Messages.getString("UnitOverview.EVADE"), x + 10, y + 23); //$NON-NLS-1$
            }

        }

        // draw condition strings
        if (entity.isImmobile() && !entity.isProne()) {
            // draw "IMMOB"
            graph.setColor(Color.darkGray);
            graph.drawString(
                    Messages.getString("UnitOverview.IMMOB"), x + 11, y + 29); //$NON-NLS-1$
            graph.setColor(Color.red);
            graph.drawString(
                    Messages.getString("UnitOverview.IMMOB"), x + 10, y + 28); //$NON-NLS-1$
        } else if (!entity.isImmobile() && entity.isProne()) {
            // draw "PRONE"
            graph.setColor(Color.darkGray);
            graph.drawString(
                    Messages.getString("UnitOverview.PRONE"), x + 11, y + 29); //$NON-NLS-1$
            graph.setColor(Color.yellow);
            graph.drawString(
                    Messages.getString("UnitOverview.PRONE"), x + 10, y + 28); //$NON-NLS-1$
        } else if (entity.isImmobile() && entity.isProne()) {
            // draw "IMMOB" and "PRONE"
            graph.setColor(Color.darkGray);
            graph.drawString(
                    Messages.getString("UnitOverview.IMMOB"), x + 11, y + 24); //$NON-NLS-1$
            graph.drawString(
                    Messages.getString("UnitOverview.PRONE"), x + 11, y + 34); //$NON-NLS-1$
            graph.setColor(Color.red);
            graph.drawString(
                    Messages.getString("UnitOverview.IMMOB"), x + 10, y + 23); //$NON-NLS-1$
            graph.setColor(Color.yellow);
            graph.drawString(
                    Messages.getString("UnitOverview.PRONE"), x + 10, y + 33); //$NON-NLS-1$
        } else if (!entity.isImmobile() && entity.isHullDown()) {
            // draw "PRONE"
            graph.setColor(Color.darkGray);
            graph.drawString(
                    Messages.getString("UnitOverview.HULLDOWN"), x-1, y + 29); //$NON-NLS-1$
            graph.setColor(Color.yellow);
            graph.drawString(
                    Messages.getString("UnitOverview.HULLDOWN"), x-2, y + 28); //$NON-NLS-1$
        } else if (entity.isImmobile() && entity.isHullDown()) {
            // draw "IMMOB" and "PRONE"
            graph.setColor(Color.darkGray);
            graph.drawString(
                    Messages.getString("UnitOverview.IMMOB"), x + 11, y + 24); //$NON-NLS-1$
            graph.drawString(
                    Messages.getString("UnitOverview.HULLDOWN"), x - 1 , y + 34); //$NON-NLS-1$
            graph.setColor(Color.red);
            graph.drawString(
                    Messages.getString("UnitOverview.IMMOB"), x + 10, y + 23); //$NON-NLS-1$
            graph.setColor(Color.yellow);
            graph.drawString(
                    Messages.getString("UnitOverview.HULLDOWN"), x - 2, y + 33); //$NON-NLS-1$
        } else if (!entity.isDeployed()) {
            int roundsLeft = entity.getDeployRound()
                    - clientgui.getClient().game.getRoundCount();
            if (roundsLeft > 0) {
                printLine(graph, x + 25, y + 28, Integer.toString(roundsLeft));
            }
        }
    }

    private void computeUnitsPerPage(Dimension size) {
        unitsPerPage = (size.height - DIST_TOP) / (ICON_HEIGHT + PADDING);
    }

    private void pageUp() {
        if (scrollOffset > 0) {
            scrollOffset -= actUnitsPerPage;
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
            clientgui.bv.refreshDisplayables();
        }
    }

    private void pageDown() {
        if (scrollOffset < unitIds.length - actUnitsPerPage) {
            scrollOffset += actUnitsPerPage;
            if (scrollOffset > unitIds.length - actUnitsPerPage) {
                scrollOffset = unitIds.length - actUnitsPerPage;
            }
            clientgui.bv.refreshDisplayables();
        }
    }

    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
            clientgui.bv.refreshDisplayables();
        }
    }

    private void scrollDown() {
        if (scrollOffset < unitIds.length - actUnitsPerPage) {
            scrollOffset++;
            clientgui.bv.refreshDisplayables();
        }
    }

    protected String getIconName(Entity e, FontMetrics metrics) {

        if (e instanceof BattleArmor) {
            String iconName = e.getShortName();
            if (metrics.stringWidth(iconName) > ICON_NAME_MAX_LENGTH) {
                Vector<String> v = StringUtil.splitString(iconName, " "); //$NON-NLS-1$
                iconName = v.elementAt(0);
                if (iconName.equals("Clan")) {
                    iconName = v.elementAt(1);
                }
            }
            return adjustString(iconName, metrics);
        } else if (e instanceof Protomech) {
            String iconName = e.getChassis() + " " + e.getModel(); //$NON-NLS-1$
            return adjustString(iconName, metrics);
        } else if (e instanceof Tank) {
            String iconName = e.getShortName();

            if (metrics.stringWidth(iconName) > ICON_NAME_MAX_LENGTH) {
                Vector<String> v = StringUtil.splitString(iconName, " "); //$NON-NLS-1$
                iconName = v.elementAt(0);
            }
            return adjustString(iconName, metrics);
        } else if (e instanceof Infantry || e instanceof Mech
                || e instanceof GunEmplacement ||
                e instanceof Aero) {
            String iconName = e.getModel();
            return adjustString(iconName, metrics);
        }
        return "!!Unknown!!";
    }

    protected String adjustString(String s, FontMetrics metrics) {
        while (metrics.stringWidth(s) > ICON_NAME_MAX_LENGTH) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

}
