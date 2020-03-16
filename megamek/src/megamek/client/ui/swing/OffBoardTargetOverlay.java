package megamek.client.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IGame.Phase;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.IPlayer;
import megamek.common.OffBoardDirection;
import megamek.common.Targetable;

public class OffBoardTargetOverlay implements IDisplayable {
    private static final int EDGE_OFFSET = 5;
    private static final int WIDE_EDGE_SIZE = 60;
    private static final int NARROW_EDGE_SIZE = 40;
    
    private boolean isHit = false;
    private ClientGUI clientgui;
    private Map<OffBoardDirection, Rectangle> buttons = new HashMap<>();
    private TargetingPhaseDisplay targetingPhaseDisplay;
    
    private IGame getCurrentGame() {
        return clientgui.getClient().getGame();
    }
    
    private IPlayer getCurrentPlayer() {
        return clientgui.getClient().getLocalPlayer();
    }
    
    public void setTargetingPhaseDisplay(TargetingPhaseDisplay tpd) {
        targetingPhaseDisplay = tpd;
    }
    
    
    public OffBoardTargetOverlay(ClientGUI clientgui) {
        this.clientgui = clientgui;
    }
    
    /**
     * Logic that determines if this overlay should be visible.
     */
    private boolean shouldBeVisible() {
        // only show these if there are any actual enemy units eligible for off board targeting
        for(OffBoardDirection direction : OffBoardDirection.values()) {
            if(direction != OffBoardDirection.NONE && showDirectionalElement(direction)) {
                return true; 
            }
        }
        
        return false;
    }
    
    /**
     * Logic that determines whether to show a specific directional indicator
     */
    private boolean showDirectionalElement(OffBoardDirection direction) {
        // only relevant if it's our turn in the targeting phase
        boolean visible = clientgui.getClient().isMyTurn() &&
                (clientgui.getClient().getGame().getPhase() == IGame.Phase.PHASE_TARGETING);
        
        if(!visible) {
            return false;
        }
        
        // only relevant if we've got an artillery weapon selected for one of our own units
        if(clientgui.getBoardView().getSelectedArtilleryWeapon() == null) {
            return false;
        }
        
        // if we are "facing in the right direction". This means:
        // grounded dropship firing nose-mounted artillery
        // selected weapon facing 0 for north
        // 1-2 for east
        // 3 for south
        // 4-5 for west
        
        // this is horribly inefficient, so we should cache the results of this computation, either here or in Game
        for(Entity entity : getCurrentGame().getAllOffboardEnemyEntities(getCurrentPlayer())) {
            if(entity.isOffBoardObserved(getCurrentPlayer().getTeam()) && (entity.getOffBoardDirection() == direction)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean isBeingDragged() {
        return false;
    }

    @Override
    public boolean isDragged(Point point, Dimension backSize) {
        return false;
    }

    @Override
    public boolean isHit(Point point, Dimension size) {
        for(OffBoardDirection direction : OffBoardDirection.values()) {
            if(direction != OffBoardDirection.NONE) {
                if(buttons.containsKey(direction) &&
                        buttons.get(direction).contains(point)) {
                    isHit = true;
                    handleButtonClick(direction);
                    return true;
                }
            }
        }
                
        return false;
    }

    @Override
    public boolean isMouseOver(Point point, Dimension backSize) {
        return false;
    }

    @Override
    public boolean isReleased() {
        if (!shouldBeVisible()) {
            return false;
        }

        if (isHit) {
            isHit = false;
            return true;
        }
        return false;
    }

    @Override
    public void draw(Graphics graph, Rectangle rect) {
        if(!shouldBeVisible()) {
            return;
        }
        
        Rectangle button;
        buttons.clear();
        
        Color push = graph.getColor();
        graph.setColor(GUIPreferences.getInstance().getColor(GUIPreferences.ADVANCED_UNITOVERVIEW_VALID_COLOR));
        
        // each of these draws the relevant icon and stores the coordinates for retrieval when checking hit box
        
        // draw top icon, if necessary
        if(showDirectionalElement(OffBoardDirection.NORTH)) {
            //xPosition = rect.x + (int) (rect.width / 2) - (int) (elementWidth / 2);
            //yPosition = rect.y + EDGE_OFFSET;
            button = generateRectangle(OffBoardDirection.NORTH, rect);
            buttons.put(OffBoardDirection.NORTH, button);
            graph.drawRect(button.x, button.y, button.width, button.height);
        }
        
        // draw left icon, if necessary
        if(showDirectionalElement(OffBoardDirection.WEST)) {
            button = generateRectangle(OffBoardDirection.WEST, rect);
            buttons.put(OffBoardDirection.WEST, button);
            graph.drawRect(button.x, button.y, button.width, button.height);
        }
        
        // draw bottom icon, if necessary
        if(showDirectionalElement(OffBoardDirection.SOUTH)) {
            button = generateRectangle(OffBoardDirection.SOUTH, rect);
            buttons.put(OffBoardDirection.SOUTH, button);
            graph.drawRect(button.x, button.y, button.width, button.height);
        }
        
        // draw right icon, if necessary. This one is hairy because of the unit overview pane
        if(showDirectionalElement(OffBoardDirection.EAST)) {
            button = generateRectangle(OffBoardDirection.EAST, rect);
            buttons.put(OffBoardDirection.EAST, button);
            graph.drawRect(button.x, button.y, button.width, button.height);
        }
        
        // be nice, leave the color as we found it
        graph.setColor(push);
    }
    
    /**
     * Worker function that generates a rectangle that can be drawn on screen
     * or evaluated for hit detection.
     */
    private Rectangle generateRectangle(OffBoardDirection direction, Rectangle rect) {
        int xPosition;
        int yPosition;        
        
        switch(direction) {
        // north rectangle is wider than narrower, and at the top of the board view
        case NORTH:
            xPosition = rect.x + (int) (rect.width / 2) - (int) (WIDE_EDGE_SIZE / 2);
            yPosition = rect.y + EDGE_OFFSET;
            return new Rectangle(xPosition, yPosition, WIDE_EDGE_SIZE, NARROW_EDGE_SIZE);
        // west rectangle is narrower than wider, and at the left of the board view
        case WEST:
            xPosition = rect.x + EDGE_OFFSET;
            yPosition = rect.y + (int) (rect.height / 2) - (int) (WIDE_EDGE_SIZE / 2);
            return new Rectangle(xPosition, yPosition, NARROW_EDGE_SIZE, WIDE_EDGE_SIZE);
        // south rectangle is wider than narrower, and at the bottom of the board view
        case SOUTH:
            xPosition = rect.x + (int) (rect.width / 2) - (int) (WIDE_EDGE_SIZE / 2);
            yPosition = rect.y + rect.height - EDGE_OFFSET - NARROW_EDGE_SIZE;
            return new Rectangle(xPosition, yPosition, WIDE_EDGE_SIZE, NARROW_EDGE_SIZE);
        /// east rectangle is narrower than wider, and at the right of the board view, but to the left of the unit overview panel
        case EAST:
            int extraXOffset = GUIPreferences.getInstance().getShowUnitOverview() ? UnitOverview.getUIWidth() : 0;
            xPosition = rect.x + rect.width - WIDE_EDGE_SIZE - EDGE_OFFSET - extraXOffset;
            yPosition = rect.y + (int) (rect.height / 2) - (int) (NARROW_EDGE_SIZE / 2);
            return new Rectangle(xPosition, yPosition, NARROW_EDGE_SIZE, WIDE_EDGE_SIZE);
        default:
            return null;
                
        }
    }

    @Override
    public boolean isSliding() {
        return false;
    }

    @Override
    public void setIdleTime(long l, boolean b) {
    }

    @Override
    public boolean slide() {
        return false;
    }
    
    /**
     * Worker function that handles a click on a 'counterbattery fire' overlay button.
     * Possible shows a target selection popup
     * Generates an artillery attack action that is fed back to the targeting display.
     */
    private void handleButtonClick(OffBoardDirection direction) {
        List<Targetable> eligibleTargets = new ArrayList<>();
        
        for (Entity ent : this.getCurrentGame().getAllOffboardEnemyEntities(getCurrentPlayer())) {
            if(ent.getOffBoardDirection() == direction &&
                    ent.isOffBoardObserved(getCurrentPlayer().getTeam())) {
                eligibleTargets.add(ent);
            }
        }
        
        Targetable choice;
        
        if (eligibleTargets.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(clientgui,
                            Messages.getString("FiringDisplay.ChooseCounterbatteryTargetDialog.message"),
                            Messages.getString("FiringDisplay.ChooseTargetDialog.title"),
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(eligibleTargets), null);
            choice = SharedUtility.getTargetPicked(eligibleTargets, input);
        } else if (eligibleTargets.size() == 1) {
            choice = eligibleTargets.get(0);
        } else {
            return;
        }
        
        // display dropdown containing all observed offboard enemy entities in given direction
        // upon selection, generate an ArtilleryAttackAction vs selected entity as per  TargetingPhaseDisplay, like so:
        WeaponAttackAction waa = new ArtilleryAttackAction(targetingPhaseDisplay.ce().getId(), choice.getTargetType(),
                choice.getTargetId(), 
                targetingPhaseDisplay.ce().getEquipmentNum(clientgui.getBoardView().getSelectedArtilleryWeapon()), 
                clientgui.getClient().getGame());
        
        targetingPhaseDisplay.updateDisplayForPendingAttack(clientgui.getBoardView().getSelectedArtilleryWeapon(), waa);
        // where cen = clientgui.getBoardView().getSelectedArtilleryWeapon().getEntity()
        // and target = selected target entity from dropdown
        // and weaponNum = clientgui.getBoardView().getSelectedArtilleryWeapon().getEntity().getEquipmentNum(
        //      getBoardView().getSelectedArtilleryWeapon())
        // then access TargetingPhaseDisplay.houseKeepingForAttack function
    }

}
