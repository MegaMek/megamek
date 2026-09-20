/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2003, 2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import megamek.MMConstants;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.common.Configuration;
import megamek.common.battleArmor.BattleArmor;
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

    private static final int CARD_MARGIN = 3;

    /** Derived presentation only. The client remains responsible for selection and turn validity. */
    private record Text(String value, int x, int y, Color color, Color shadow, boolean outlined) {
        void draw(Graphics2D graph) {
            graph.setColor(shadow);
            if (outlined) {
                graph.drawString(value, x + 1, y);
                graph.drawString(value, x - 1, y);
                graph.drawString(value, x, y + 1);
                graph.drawString(value, x, y - 1);
            } else {
                graph.drawString(value, x + 1, y + 1);
            }
            graph.setColor(color);
            graph.drawString(value, x, y);
        }
    }

    private record Bar(int length, Color color) { }
    private record Card(Image icon, List<Text> texts, Bar armor, Bar internal, int heat,
          Color frame, Color border) { }
    private record CardImage(Card card, BufferedImage image) { }

    // Swing owns these small immutable images. GPU snapshots copy them only when the image identity changes.
    private final Map<Integer, CardImage> cards = new HashMap<>();
    private final Map<Image, BufferedImage> buttons = new IdentityHashMap<>();
    private double imageScaleX;
    private double imageScaleY;

    private int[] unitIds = new int[0];
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

    /** Space for the visible unit strip and the same gap on either side, in overlay layout units. */
    public int sidePanelInset() {
        return visible && unitIds.length > 0 ? getUIWidth() + DIST_SIDE : 0;
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
        PMUtil.setImage(scrollUpG, clientgui.getMainPanel());
        scrollDownG = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "scrollDown2_G.png").toString());
        PMUtil.setImage(scrollDownG, clientgui.getMainPanel());
        pageUpG = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "pageUp2_G.png").toString());
        PMUtil.setImage(pageUpG, clientgui.getMainPanel());
        pageDownG = toolkit.getImage(new MegaMekFile(Configuration.widgetsDir(), "pageDown2_G.png").toString());
        PMUtil.setImage(pageDownG, clientgui.getMainPanel());

        visible = GUIP.getShowUnitOverview();
        GUIP.addPreferenceChangeListener(this);
    }

    @Override
    public void draw(Graphics graph, Rectangle clipBounds) {
        long now = System.nanoTime();
        for (OverlayImage layer : captureLayers((Graphics2D) graph, clipBounds)) {
            layer.draw((Graphics2D) graph, now);
        }
    }

    @Override
    public List<OverlayImage> captureLayers(Graphics2D graph, Rectangle clipBounds) {
        if (!visible) {
            cards.clear();
            buttons.clear();
            return List.of();
        }
        AffineTransform transform = graph.getTransform();
        double scaleX = Math.hypot(transform.getScaleX(), transform.getShearY());
        double scaleY = Math.hypot(transform.getScaleY(), transform.getShearX());
        if (imageScaleX != scaleX || imageScaleY != scaleY) {
            imageScaleX = scaleX;
            imageScaleY = scaleY;
            cards.clear();
            buttons.clear();
        }
        computeUnitsPerPage(clipBounds.getSize());
        List<Entity> units = clientgui.getClient().getGame()
              .getPlayerEntities(clientgui.getClient().getLocalPlayer(), true);
        unitIds = units.stream().mapToInt(Entity::getId).toArray();
        Set<Integer> retained = new HashSet<>();
        units.forEach(entity -> retained.add(entity.getId()));
        cards.keySet().retainAll(retained);
        scroll = units.size() > unitsPerPage;
        actUnitsPerPage = Math.max(0, scroll ? unitsPerPage - 2 : unitsPerPage);
        scrollOffset = Math.max(0, Math.min(scrollOffset, units.size() - actUnitsPerPage));

        List<OverlayImage> layers = new ArrayList<>();
        int x = clipBounds.x + clipBounds.width - DIST_SIDE - ICON_WIDTH;
        int y = clipBounds.y + DIST_TOP;
        if (scroll) {
            layers.add(buttonLayer(graph, scrollOffset > 0 ? pageUp : pageUpG, x, y));
            layers.add(buttonLayer(graph, scrollOffset > 0 ? scrollUp : scrollUpG,
                  x, y + BUTTON_HEIGHT + BUTTON_PADDING));
            y += 2 * (BUTTON_HEIGHT + BUTTON_PADDING);
        }
        for (int i = scrollOffset; i < units.size() && i < actUnitsPerPage + scrollOffset; i++) {
            Entity entity = units.get(i);
            Card card = card(entity);
            CardImage cached = cards.get(entity.getId());
            if (cached == null || !cached.card().equals(card)) {
                cached = new CardImage(card, paintCard(card));
                cards.put(entity.getId(), cached);
            }
            layers.add(layer(graph, cached.image(), x - CARD_MARGIN, y - CARD_MARGIN));
            y += ICON_HEIGHT + PADDING;
        }
        if (scroll) {
            y += BUTTON_PADDING - PADDING;
            boolean atBottom = scrollOffset == units.size() - actUnitsPerPage;
            layers.add(buttonLayer(graph, atBottom ? scrollDownG : scrollDown, x, y));
            layers.add(buttonLayer(graph, atBottom ? pageDownG : pageDown,
                  x, y + BUTTON_HEIGHT + BUTTON_PADDING));
        }
        return List.copyOf(layers);
    }

    private OverlayImage buttonLayer(Graphics2D graph, Image button, int x, int y) {
        BufferedImage image = buttons.computeIfAbsent(button, artwork -> {
            BufferedImage result = image(artwork.getWidth(null), artwork.getHeight(null));
            Graphics2D painter = painter(result);
            try {
                painter.drawImage(artwork, 0, 0, null);
            } finally {
                painter.dispose();
            }
            return result;
        });
        return layer(graph, image, x, y);
    }

    private OverlayImage layer(Graphics2D graph, BufferedImage image, int x, int y) {
        Point2D point = graph.getTransform().transform(new Point(x, y), null);
        return new OverlayImage(image, (int) Math.round(point.getX()), (int) Math.round(point.getY()),
              OverlayImage.Fade.OPAQUE);
    }

    private BufferedImage image(int width, int height) {
        return new BufferedImage(Math.max(1, (int) Math.ceil(width * imageScaleX)),
              Math.max(1, (int) Math.ceil(height * imageScaleY)), BufferedImage.TYPE_INT_ARGB);
    }

    private Graphics2D painter(BufferedImage image) {
        Graphics2D graph = image.createGraphics();
        graph.scale(imageScaleX, imageScaleY);
        UIUtil.setHighQualityRendering(graph);
        graph.setFont(FONT);
        return graph;
    }

    private Card card(Entity entity) {
        Image icon = clientgui.getCurrentBoardView()
              .map(bv -> ((BoardView) bv).getTilesetManager().iconFor(entity)).orElse(null);
        List<Text> texts = new ArrayList<>();
        texts.add(outlinedText(getIconName(entity, fm), 3, 46));
        texts.addAll(conditionStrings(entity));
        Game game = clientgui.getClient().getGame();
        GameTurn turn = game.getPhase().isSimultaneous(game)
              ? game.getTurnForPlayer(clientgui.getClient().getLocalPlayer().getId()) : game.getTurn();
        Color border = turn != null && turn.isValidEntity(entity, game) ? GUIP.getUnitValidColor() : null;
        if (entity == clientgui.getDisplayedUnit() && game.getTurn() != null && game.getTurn().isValidEntity(entity, game)) {
            border = GUIP.getUnitSelectedColor();
        }
        double armor = entity.getArmorRemainingPercent();
        return new Card(icon, List.copyOf(texts), armor == IArmorState.ARMOR_NA ? null : bar(armor),
              bar(entity.getInternalRemainingPercent()), heat(entity), getFrameColor(entity), border);
    }

    private BufferedImage paintCard(Card card) {
        // Localized condition strings can extend beyond the portrait. Preserve their original unclipped extent.
        int width = Math.max(ICON_WIDTH, card.texts().stream()
              .mapToInt(text -> text.x() + fm.stringWidth(text.value()) + 1).max().orElse(0));
        int height = Math.max(ICON_HEIGHT, card.texts().stream()
              .mapToInt(text -> text.y() + fm.getDescent() + 1).max().orElse(0));
        BufferedImage result = image(width + 2 * CARD_MARGIN, height + 2 * CARD_MARGIN);
        Graphics2D graph = painter(result);
        try {
            graph.translate(CARD_MARGIN, CARD_MARGIN);
            graph.drawImage(card.icon(), 0, 0, null);
            card.texts().getFirst().draw(graph);
            drawBar(graph, card.armor(), 3);
            drawBar(graph, card.internal(), 6);
            if (card.heat() >= 0) {
                graph.setColor(Color.darkGray);
                graph.fillRect(52, 4, 2, 30);
                graph.setColor(Color.lightGray);
                graph.fillRect(51, 3, 2, 30);
                graph.setColor(Color.red);
                graph.fillRect(51, 33 - card.heat(), 2, card.heat());
            }
            card.texts().stream().skip(1).forEach(text -> text.draw(graph));
            graph.setColor(card.frame());
            graph.setStroke(new BasicStroke(1f));
            graph.drawRect(0, 0, ICON_WIDTH, ICON_HEIGHT);
            if (card.border() != null) {
                graph.setColor(card.border());
                graph.drawRect(-1, -1, ICON_WIDTH + 2, ICON_HEIGHT + 2);
            }
        } finally {
            graph.dispose();
        }
        return result;
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
                // Navigation must work even when this phase cannot select the clicked unit to act.
                clientgui.centerOnUnit(clientgui.getClient().getGame().getEntity(unitIds[i]));
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

    private int heat(Entity entity) {
        if (!(entity instanceof Mek || entity instanceof Aero)) {
            return -1;
        }
        boolean extended = entity.getGame() != null
              && entity.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT);
        return extended ? (int) (Math.min(50, entity.heat) * 0.6) : Math.min(30, entity.heat);
    }

    private Bar bar(double percent) {
        return new Bar((int) (23 * percent), getStatusBarColor(percent));
    }

    private void drawBar(Graphics graph, Bar bar, int y) {
        if (bar == null) {
            return;
        }
        graph.setColor(Color.darkGray);
        graph.fillRect(4, y + 1, 23, 2);
        graph.setColor(Color.lightGray);
        graph.fillRect(3, y, 23, 2);
        graph.setColor(bar.color());
        graph.fillRect(3, y, bar.length(), 2);
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

    private Text outlinedText(String text, int x, int y) {
        return new Text(text, x, y, GUIP.getUnitTextColor(), GUIP.getUnitOverviewTextShadowColor(), true);
    }

    private Text condition(String key, int x, int y, Color color) {
        return new Text(Messages.getString(key), x, y, color, GUIP.getUnitOverviewConditionShadowColor(), false);
    }

    private List<Text> conditionStrings(Entity entity) {
        List<Text> texts = new ArrayList<>();
        if (entity.isAero()) {
            IAero aero = (IAero) entity;
            if (aero.isRolled()) {
                texts.add(condition("BoardView1.ROLLED", 10, 28, GUIP.getWarningColor()));
            }
            if (aero.isOutControlTotal() && aero.isRandomMove()) {
                texts.add(condition("UnitOverview.RANDOM", 10, 23, GUIP.getWarningColor()));
            } else if (aero.isOutControlTotal()) {
                texts.add(condition("UnitOverview.CONTROL", 10, 23, GUIP.getWarningColor()));
            }
            if (entity.isEvading()) {
                texts.add(condition("UnitOverview.EVADE", 10, 23, GUIP.getWarningColor()));
            }
        }
        if (entity.isImmobile() && !entity.isProne() && !entity.isBuildingEntityOrGunEmplacement()) {
            texts.add(condition("UnitOverview.IMMOB", 10, 28, GUIP.getWarningColor()));
        } else if (!entity.isImmobile() && entity.isProne()) {
            texts.add(condition("UnitOverview.PRONE", 10, 28, GUIP.getCautionColor()));
        } else if (entity.isImmobile() && entity.isProne()) {
            texts.add(condition("UnitOverview.IMMOB", 10, 23, GUIP.getWarningColor()));
            texts.add(condition("UnitOverview.PRONE", 10, 33, GUIP.getCautionColor()));
        } else if (!entity.isImmobile() && entity.isHullDown()) {
            texts.add(condition("UnitOverview.HULLDOWN", -2, 28, GUIP.getPrecautionColor()));
        } else if (entity.isImmobile() && entity.isHullDown()) {
            texts.add(condition("UnitOverview.IMMOB", 10, 23, GUIP.getWarningColor()));
            texts.add(condition("UnitOverview.HULLDOWN", -2, 33, GUIP.getPrecautionColor()));
        } else if (!entity.isDeployed()) {
            int roundsLeft = entity.getDeployRound() - clientgui.getClient().getGame().getRoundCount();
            if (roundsLeft > 0) {
                texts.add(outlinedText(Integer.toString(roundsLeft), 25, 28));
            }
        }
        return texts;
    }

    private void computeUnitsPerPage(Dimension size) {
        unitsPerPage = Math.max(0, (size.height - DIST_TOP) / (ICON_HEIGHT + PADDING));
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
