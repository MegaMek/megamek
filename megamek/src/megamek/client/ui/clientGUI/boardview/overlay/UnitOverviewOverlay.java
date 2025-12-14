/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2003 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview.overlay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Vector;

import megamek.MMConstants;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.common.Configuration;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.IArmorState;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.util.StringUtil;
import megamek.common.util.fileUtils.MegaMekFile;

public class UnitOverviewOverlay implements IDisplayable, IPreferenceChangeListener {
    private static final int UNKNOWN_UNITS_PER_PAGE = -1;

    /**
     * The maximum length of the icon name.
     */
    public static final int ICON_NAME_MAX_LENGTH = 52;

    private static final Font FONT = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 10);
    private static final int DIST_TOP = 5;
    private static final int DIST_SIDE = 5;
    private static final int ICON_WIDTH = 56;
    private static final int ICON_HEIGHT = 48;
    private static final int BUTTON_HEIGHT = 15;
    private static final int BUTTON_PADDING = 4;
    private static final int PADDING = 5;

    private int[] unitIds;
    private boolean isHit = false;
    private boolean visible;
    private boolean scroll = false;
    private int unitsPerPage = UNKNOWN_UNITS_PER_PAGE;
    private int actUnitsPerPage = 0;
    private int scrollOffset = 0;

    private final ClientGUI clientgui;

    private final FontMetrics fm;

    private final Image scrollUp;
    private final Image scrollDown;
    private final Image pageUp;
    private final Image pageDown;

    public static int getUIWidth() {
        return ICON_WIDTH + DIST_SIDE;
    }

    private final Image scrollUpG;
    private final Image scrollDownG;
    private final Image pageUpG;
    private final Image pageDownG;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public UnitOverviewOverlay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        fm = clientgui.getMainPanel().getFontMetrics(FONT);

        Toolkit toolkit = clientgui.getMainPanel().getToolkit();
        scrollUp = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "scrollUp2.png").toString());
        PMUtil.setImage(scrollUp, clientgui.getMainPanel());
        scrollDown = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "scrollDown2.png").toString());
        PMUtil.setImage(scrollDown, clientgui.getMainPanel());
        pageUp = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "pageUp2.png").toString());
        PMUtil.setImage(pageUp, clientgui.getMainPanel());
        pageDown = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "pageDown2.png").toString());
        PMUtil.setImage(pageDown, clientgui.getMainPanel());
        scrollUpG = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "scrollUp2_G.png").toString());
        PMUtil.setImage(scrollUp, clientgui.getMainPanel());
        scrollDownG = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "scrollDown2_G.png").toString());
        PMUtil.setImage(scrollDown, clientgui.getMainPanel());
        pageUpG = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "pageUp2_G.png").toString());
        PMUtil.setImage(pageUp, clientgui.getMainPanel());
        pageDownG = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "pageDown2_G.png").toString());
        PMUtil.setImage(pageDown, clientgui.getMainPanel());

        visible = GUIP.getShowUnitOverview();
        GUIP.addPreferenceChangeListener(this);
    }

    @Override
    public void draw(Graphics graph, Rectangle clipBounds) {
        if (!visible) {
            return;
        }

        computeUnitsPerPage(clipBounds.getSize());

        graph.setFont(FONT);
        ArrayList<Entity> v = clientgui.getClient().getGame()
              .getPlayerEntities(clientgui.getClient().getLocalPlayer(), true);
        unitIds = new int[v.size()];

        scroll = v.size() > unitsPerPage;

        actUnitsPerPage = scroll ? unitsPerPage - 2 : unitsPerPage;

        if (scrollOffset + actUnitsPerPage > unitIds.length) {
            scrollOffset = unitIds.length - actUnitsPerPage;
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
        }

        int x = clipBounds.x + clipBounds.width - DIST_SIDE - ICON_WIDTH;
        int y = clipBounds.y + DIST_TOP;

        if (scroll) {
            if (scrollOffset > 0) {
                graph.drawImage(pageUp, x, y, null);
                graph.drawImage(scrollUp, x, y + BUTTON_HEIGHT + BUTTON_PADDING,
                      null);
            } else {
                graph.drawImage(pageUpG, x, y, null);    // Top of list = greyed out buttons
                graph.drawImage(scrollUpG, x, y + BUTTON_HEIGHT + BUTTON_PADDING,
                      null);
            }
            y += BUTTON_HEIGHT + BUTTON_HEIGHT + BUTTON_PADDING
                  + BUTTON_PADDING;
        }

        for (int i = scrollOffset; (i < v.size())
              && (i < actUnitsPerPage + scrollOffset); i++) {
            Entity e = v.get(i);
            unitIds[i] = e.getId();
            String name = getIconName(e, fm);
            Image i1 = clientgui.getCurrentBoardView()
                  .map(bv -> ((BoardView) bv).getTilesetManager().iconFor(e))
                  .orElse(null);

            graph.drawImage(i1, x, y, null);
            printLine(graph, x + 3, y + 46, name);
            drawBars(graph, e, x, y);
            drawHeat(graph, e, x, y);
            drawConditionStrings(graph, e, x, y);
            graph.setColor(getFrameColor(e));
            graph.drawRect(x, y, ICON_WIDTH, ICON_HEIGHT);

            Game game = clientgui.getClient().getGame();
            GameTurn turn = game.getPhase().isSimultaneous(game)
                  ? game.getTurnForPlayer(clientgui.getClient().getLocalPlayer().getId())
                  : game.getTurn();

            if ((turn != null) && turn.isValidEntity(e, game)) {
                Color oldColor = graph.getColor();
                graph.setColor(GUIP.getUnitValidColor());
                graph.drawRect(x - 1, y - 1, ICON_WIDTH + 2, ICON_HEIGHT + 2);
                graph.setColor(oldColor);
            }

            Entity se = clientgui.getDisplayedUnit();
            if ((e == se) && (game.getTurn() != null) && game.getTurn().isValidEntity(e, game)) {
                Color oldColor = graph.getColor();
                graph.setColor(GUIP.getUnitSelectedColor());
                graph.drawRect(x - 1, y - 1, ICON_WIDTH + 2, ICON_HEIGHT + 2);
                graph.setColor(oldColor);
            }

            y += ICON_HEIGHT + PADDING;
        }

        if (scroll) {
            y -= PADDING;
            y += BUTTON_PADDING;
            if (scrollOffset == unitIds.length - actUnitsPerPage) {
                graph.drawImage(scrollDownG, x, y, null);   // Bottom of list = greyed out buttons
                graph.drawImage(pageDownG, x, y + BUTTON_HEIGHT + BUTTON_PADDING,
                      null);
            } else {
                graph.drawImage(scrollDown, x, y, null);
                graph.drawImage(pageDown, x, y + BUTTON_HEIGHT + BUTTON_PADDING,
                      null);
            }

        }

    }

    @Override
    public boolean isHit(Point p, Dimension size) {
        if (!visible) {
            return false;
        }

        int actUnits = scroll ? unitsPerPage - 2 : unitsPerPage;

        int x = p.x;
        int y = p.y;
        int xOffset = size.width - DIST_SIDE - ICON_WIDTH;
        int yOffset = DIST_TOP;

        if ((x < xOffset) || (x > xOffset + ICON_WIDTH) || (y < yOffset)
              || (y > yOffset + (unitsPerPage * (ICON_HEIGHT + PADDING)))) {
            return false;
        }

        if (scroll) {
            if ((y > yOffset) && (y < yOffset + BUTTON_HEIGHT)) {
                pageUp();
                return true;
            }
            yOffset += BUTTON_HEIGHT + BUTTON_PADDING;
            if ((y > yOffset) && (y < yOffset + BUTTON_HEIGHT)) {
                scrollUp();
                return true;
            }
            yOffset += BUTTON_HEIGHT + BUTTON_PADDING;
        }

        for (int i = scrollOffset; (i < unitIds.length)
              && (i < actUnits + scrollOffset); i++) {
            if ((y > yOffset) && (y < yOffset + ICON_HEIGHT)) {
                clientgui.getBoardView().processBoardViewEvent(new BoardViewEvent(
                      clientgui.getBoardView(), BoardViewEvent.SELECT_UNIT, unitIds[i]));
                isHit = true;
                return true;
            }
            yOffset += ICON_HEIGHT + PADDING;
        }

        if (scroll) {
            yOffset -= PADDING;
            yOffset += BUTTON_PADDING;
            if ((y > yOffset) && (y < yOffset + BUTTON_HEIGHT)) {
                scrollDown();
                return true;
            }
            yOffset += BUTTON_HEIGHT + BUTTON_PADDING;
            if ((y > yOffset) && (y < yOffset + BUTTON_HEIGHT)) {
                pageDown();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isDragged(Point p, Dimension size) {
        int x = p.x;
        int y = p.y;
        int xOffset = size.width - DIST_SIDE - ICON_WIDTH;
        int yOffset = DIST_TOP;

        return (x >= xOffset) && (x <= xOffset + ICON_WIDTH) && (y >= yOffset)
              && (y <= yOffset + (unitsPerPage * (ICON_HEIGHT + PADDING)));
    }

    @Override
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

    private void drawHeat(Graphics graph, Entity entity, int x, int y) {
        if (!((entity instanceof Mek) || (entity instanceof Aero))) {
            return;
        }
        boolean mtHeat = false;
        int mHeat = 30;
        if ((entity.getGame() != null)
              && entity.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT)) {
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
        // Let's draw our armor and internal status bars
        int baseBarLength = 23;
        int barLength;
        double percentRemaining;

        percentRemaining = entity.getArmorRemainingPercent();
        if (percentRemaining != IArmorState.ARMOR_NA) {
            barLength = (int) (baseBarLength * percentRemaining);

            graph.setColor(Color.darkGray);
            graph.fillRect(x + 4, y + 4, 23, 2);
            graph.setColor(Color.lightGray);
            graph.fillRect(x + 3, y + 3, 23, 2);
            graph.setColor(getStatusBarColor(percentRemaining));
            graph.fillRect(x + 3, y + 3, barLength, 2);

        }
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
            return Color.DARK_GRAY;
        }
        return Color.black;
    }

    private void printLine(Graphics g, int x, int y, String s) {
        g.setColor(GUIP.getUnitOverviewTextShadowColor());
        g.drawString(s, x + 1, y);
        g.drawString(s, x - 1, y);
        g.drawString(s, x, y + 1);
        g.drawString(s, x, y - 1);
        g.setColor(GUIP.getUnitTextColor());
        g.drawString(s, x, y);
    }

    private void drawConditionStrings(Graphics graph, Entity entity, int x, int y) {
        // out of control conditions for ASF
        if (entity.isAero()) {
            IAero a = (IAero) entity;

            if (a.isRolled()) {
                // draw "rolled"
                graph.setColor(GUIP.getUnitOverviewConditionShadowColor());
                graph.drawString(Messages.getString("BoardView1.ROLLED"), x + 11, y + 29);
                graph.setColor(GUIP.getWarningColor());
                graph.drawString(Messages.getString("BoardView1.ROLLED"), x + 10, y + 28);
            }

            if (a.isOutControlTotal() && a.isRandomMove()) {
                graph.setColor(GUIP.getUnitOverviewConditionShadowColor());
                graph.drawString(Messages.getString("UnitOverview.RANDOM"), x + 11, y + 24);
                graph.setColor(GUIP.getWarningColor());
                graph.drawString(Messages.getString("UnitOverview.RANDOM"), x + 10, y + 23);
            } else if (a.isOutControlTotal()) {
                // draw "CONTROL"
                graph.setColor(GUIP.getUnitOverviewConditionShadowColor());
                graph.drawString(Messages.getString("UnitOverview.CONTROL"), x + 11, y + 24);
                graph.setColor(GUIP.getWarningColor());
                graph.drawString(Messages.getString("UnitOverview.CONTROL"), x + 10, y + 23);
            }

            //is the unit evading? - can't evade and be out of control so just draw on top
            if (entity.isEvading()) {
                // draw evasion
                graph.setColor(GUIP.getUnitOverviewConditionShadowColor());
                graph.drawString(Messages.getString("UnitOverview.EVADE"), x + 11, y + 24);
                graph.setColor(GUIP.getWarningColor());
                graph.drawString(Messages.getString("UnitOverview.EVADE"), x + 10, y + 23);
            }

        }

        // draw condition strings
        if (entity.isImmobile() && !entity.isProne() && !(entity.isBuildingEntityOrGunEmplacement())) {
            // draw "IMMOB"
            graph.setColor(GUIP.getUnitOverviewConditionShadowColor());
            graph.drawString(Messages.getString("UnitOverview.IMMOB"), x + 11, y + 29);
            graph.setColor(GUIP.getWarningColor());
            graph.drawString(Messages.getString("UnitOverview.IMMOB"), x + 10, y + 28);
        } else if (!entity.isImmobile() && entity.isProne()) {
            // draw "PRONE"
            graph.setColor(GUIP.getUnitOverviewConditionShadowColor());
            graph.drawString(Messages.getString("UnitOverview.PRONE"), x + 11, y + 29);
            graph.setColor(GUIP.getCautionColor());
            graph.drawString(Messages.getString("UnitOverview.PRONE"), x + 10, y + 28);
        } else if (entity.isImmobile() && entity.isProne()) {
            // draw "IMMOB" and "PRONE"
            graph.setColor(GUIP.getUnitOverviewConditionShadowColor());
            graph.drawString(Messages.getString("UnitOverview.IMMOB"), x + 11, y + 24);
            graph.drawString(Messages.getString("UnitOverview.PRONE"), x + 11, y + 34);
            graph.setColor(GUIP.getWarningColor());
            graph.drawString(Messages.getString("UnitOverview.IMMOB"), x + 10, y + 23);
            graph.setColor(GUIP.getCautionColor());
            graph.drawString(Messages.getString("UnitOverview.PRONE"), x + 10, y + 33);
        } else if (!entity.isImmobile() && entity.isHullDown()) {
            // draw "HullDown"
            graph.setColor(GUIP.getUnitOverviewConditionShadowColor());
            graph.drawString(Messages.getString("UnitOverview.HULLDOWN"), x - 1, y + 29);
            graph.setColor(GUIP.getPrecautionColor());
            graph.drawString(Messages.getString("UnitOverview.HULLDOWN"), x - 2, y + 28);
        } else if (entity.isImmobile() && entity.isHullDown()) {
            // draw "IMMOB" and "HullDown"
            graph.setColor(GUIP.getUnitOverviewConditionShadowColor());
            graph.drawString(Messages.getString("UnitOverview.IMMOB"), x + 11, y + 24);
            graph.drawString(Messages.getString("UnitOverview.HULLDOWN"), x - 1, y + 34);
            graph.setColor(GUIP.getWarningColor());
            graph.drawString(Messages.getString("UnitOverview.IMMOB"), x + 10, y + 23);
            graph.setColor(GUIP.getPrecautionColor());
            graph.drawString(Messages.getString("UnitOverview.HULLDOWN"), x - 2, y + 33);
        } else if (!entity.isDeployed()) {
            int roundsLeft = entity.getDeployRound()
                  - clientgui.getClient().getGame().getRoundCount();
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
            clientgui.getCurrentBoardView().ifPresent(IBoardView::refreshDisplayables);
        }
    }

    private void pageDown() {
        if (scrollOffset < unitIds.length - actUnitsPerPage) {
            scrollOffset += actUnitsPerPage;
            if (scrollOffset > unitIds.length - actUnitsPerPage) {
                scrollOffset = unitIds.length - actUnitsPerPage;
            }
            clientgui.getCurrentBoardView().ifPresent(IBoardView::refreshDisplayables);
        }
    }

    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
            clientgui.getCurrentBoardView().ifPresent(IBoardView::refreshDisplayables);
        }
    }

    private void scrollDown() {
        if (scrollOffset < unitIds.length - actUnitsPerPage) {
            scrollOffset++;
            clientgui.getCurrentBoardView().ifPresent(IBoardView::refreshDisplayables);
        }
    }

    protected String getIconName(Entity e, FontMetrics metrics) {
        if (e instanceof BattleArmor) {
            String iconName = e.getShortName();
            if (metrics.stringWidth(iconName) > ICON_NAME_MAX_LENGTH) {
                Vector<String> v = StringUtil.splitString(iconName, " ");
                iconName = v.elementAt(0);
                if (iconName.equals("Clan")) {
                    iconName = v.elementAt(1);
                }
            }
            return adjustString(iconName, metrics);
        } else if (e instanceof ProtoMek) {
            String iconName = e.getChassis() + " " + e.getModel();
            return adjustString(iconName, metrics);
        } else if ((e instanceof Infantry) || (e instanceof Mek) || (e.isBuildingEntityOrGunEmplacement())
              || (e instanceof Aero)) {
            return adjustString(e.getModel(), metrics);
        } else if (e instanceof Tank) {
            String iconName = e.getShortName();

            if (metrics.stringWidth(iconName) > ICON_NAME_MAX_LENGTH) {
                Vector<String> v = StringUtil.splitString(iconName, " ");
                iconName = "";
                for (String tok : v) {
                    String newName = iconName + " " + tok;
                    if (metrics.stringWidth(newName) <= ICON_NAME_MAX_LENGTH) {
                        iconName = newName;
                    } else {
                        break;
                    }
                }
            }
            return adjustString(iconName, metrics);
        } else {
            return "!!Unknown!!";
        }
    }

    protected String adjustString(String s, FontMetrics metrics) {
        while (metrics.stringWidth(s) > ICON_NAME_MAX_LENGTH) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.SHOW_UNIT_OVERVIEW)) {
            visible = GUIP.getShowUnitOverview();
            clientgui.getCurrentBoardView().ifPresent(IBoardView::refreshDisplayables);
        }
    }
}
