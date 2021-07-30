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

package megamek.client.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
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
import megamek.client.ui.swing.boardview.BoardView1;
import megamek.common.Compute;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.HexTarget;
import megamek.common.IGame;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.IPlayer;
import megamek.common.Mounted;
import megamek.common.OffBoardDirection;
import megamek.common.Targetable;

/**
 * This class handles the display and logic for the off board targeting overlay.
 *
 */
public class OffBoardTargetOverlay implements IDisplayable {
    private static final int EDGE_OFFSET = 5;
    private static final int WIDE_EDGE_SIZE = 60;
    private static final int NARROW_EDGE_SIZE = 40;
    private static final String FILENAME_OFFBOARD_TARGET_IMAGE = "OffBoardTarget.png";
    
    private boolean isHit = false;
    private ClientGUI clientgui;
    private Map<OffBoardDirection, Rectangle> buttons = new HashMap<>();
    private TargetingPhaseDisplay targetingPhaseDisplay;
    private Image offBoardTargetImage;
    
    private IGame getCurrentGame() {
        return clientgui.getClient().getGame();
    }
    
    private IPlayer getCurrentPlayer() {
        return clientgui.getClient().getLocalPlayer();
    }
    
    /**
     * Sets a reference to a TargetingPhaseDisplay. Used to communicate a generated attack to it.
     */
    public void setTargetingPhaseDisplay(TargetingPhaseDisplay tpd) {
        targetingPhaseDisplay = tpd;
    }    
    
    public OffBoardTargetOverlay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        
        offBoardTargetImage = ImageUtil.loadImageFromFile(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_OFFBOARD_TARGET_IMAGE)
                        .toString());
        
        //Maybe TODO: display dimmed version of off-board icon during movement phase OR targeting phase when weapon is ineligible to fire 
        //Maybe TODO: maybe tooltips?
    }
    
    /**
     * Logic that determines if this overlay should be visible.
     */
    private boolean shouldBeVisible() {
        // only relevant if it's our turn in the targeting phase
        boolean visible = clientgui.getClient().isMyTurn() &&
                (getCurrentGame().getPhase() == IGame.Phase.PHASE_TARGETING);
        
        if(!visible) {
            return false;
        }
        
        Mounted selectedArtilleryWeapon = clientgui.getBoardView().getSelectedArtilleryWeapon();
        
        // only relevant if we've got an artillery weapon selected for one of our own units
        if(selectedArtilleryWeapon == null) {
            return false;
        }
        
        // the artillery weapon needs to be using non-homing ammo
        Mounted ammo = selectedArtilleryWeapon.getLinked();
        if(ammo.isHomingAmmoInHomingMode()) {
            return false;
        }
        
        // only show these if there are any actual enemy units eligible for off board targeting
        for(OffBoardDirection direction : OffBoardDirection.values()) {
            if(showDirectionalElement(direction, selectedArtilleryWeapon)) {
                return true; 
            }
        }
        
        return false;
    }
    
    /**
     * Logic that determines whether to show a specific directional indicator
     */
    private boolean showDirectionalElement(OffBoardDirection direction, Mounted selectedArtilleryWeapon) {
        for(Entity entity : getCurrentGame().getAllOffboardEnemyEntities(getCurrentPlayer())) {
            if(entity.isOffBoardObserved(getCurrentPlayer().getTeam()) && 
                    (entity.getOffBoardDirection() == direction) &&
                        (targetingPhaseDisplay.ce().isOffBoard() ||
                        weaponFacingInDirection(selectedArtilleryWeapon, direction))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Worker function that determines if the given weapon is facing in the correct off-board direction.
     */
    private boolean weaponFacingInDirection(Mounted artilleryWeapon, OffBoardDirection direction) {
        Coords checkCoords = artilleryWeapon.getEntity().getPosition();
        int translationDistance;
        
        // little hack: we project a point 10 hexes off board to the north/south/east/west and declare a hex target with it
        // then use Compute.isInArc, as that takes into account all the logic including torso/turret twists.
        switch(direction) {
        case NORTH:
            checkCoords = checkCoords.translated(0, checkCoords.getY() + 10);
            break;
        case SOUTH:
            checkCoords = checkCoords.translated(3, getCurrentGame().getBoard().getHeight() - checkCoords.getY() + 10);
            break;
        case EAST:
            translationDistance = ((getCurrentGame().getBoard().getWidth() - checkCoords.getX()) / 2) + 5;
            checkCoords = checkCoords.translated(1, translationDistance).translated(2, translationDistance);
            break;
        case WEST:
            translationDistance = checkCoords.getX() + 5;
            checkCoords = checkCoords.translated(4, translationDistance).translated(5, translationDistance);
            break;
        default:
            return false;
        }
        
        Targetable checkTarget = new HexTarget(checkCoords, Targetable.TYPE_HEX_ARTILLERY);
        
        return Compute.isInArc(getCurrentGame(), artilleryWeapon.getEntity().getId(), 
                artilleryWeapon.getEntity().getEquipmentNum(artilleryWeapon), checkTarget);
    }
    
    @Override
    public boolean isHit(Point point, Dimension size) {
        Point actualPoint = point;
        actualPoint.x = (int) (point.getX() + clientgui.getBoardView().getDisplayablesRect().getX());
        actualPoint.y = (int) (point.getY() + clientgui.getBoardView().getDisplayablesRect().getY());
        
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
        // pre-store the selected artillery weapon as it carries out a bunch of computations
        Mounted selectedArtilleryWeapon = clientgui.getBoardView().getSelectedArtilleryWeapon();
        
        // draw top icon, if necessary
        if(showDirectionalElement(OffBoardDirection.NORTH, selectedArtilleryWeapon)) {
            button = generateRectangle(OffBoardDirection.NORTH, rect);
            buttons.put(OffBoardDirection.NORTH, button);
            graph.drawImage(offBoardTargetImage, button.x, button.y, button.width, button.height, (BoardView1) clientgui.getBoardView());
        }
        
        // draw left icon, if necessary
        if(showDirectionalElement(OffBoardDirection.WEST, selectedArtilleryWeapon)) {
            button = generateRectangle(OffBoardDirection.WEST, rect);
            buttons.put(OffBoardDirection.WEST, button);
            graph.drawImage(offBoardTargetImage, button.x, button.y, button.width, button.height, (BoardView1) clientgui.getBoardView());
        }
        
        // draw bottom icon, if necessary
        if(showDirectionalElement(OffBoardDirection.SOUTH, selectedArtilleryWeapon)) {
            button = generateRectangle(OffBoardDirection.SOUTH, rect);
            buttons.put(OffBoardDirection.SOUTH, button);
            graph.drawImage(offBoardTargetImage, button.x, button.y, button.width, button.height, (BoardView1) clientgui.getBoardView());
        }
        
        // draw right icon, if necessary. This one is hairy because of the unit overview pane
        if(showDirectionalElement(OffBoardDirection.EAST, selectedArtilleryWeapon)) {
            button = generateRectangle(OffBoardDirection.EAST, rect);
            buttons.put(OffBoardDirection.EAST, button);
            graph.drawImage(offBoardTargetImage, button.x, button.y, button.width, button.height, (BoardView1) clientgui.getBoardView());
        }
        
        // be nice, leave the color as we found it
        graph.setColor(push);
    }
    
    /**
     * Worker function that generates a rectangle that can be drawn on screen
     * or evaluated for hit detection.
     */
    private Rectangle generateRectangle(OffBoardDirection direction, Rectangle boundingRectangle) {
        int xPosition;
        int yPosition;        
        
        switch(direction) {
        // north rectangle is wider than narrower, and at the top of the board view
        case NORTH:
            xPosition = boundingRectangle.x + (int) (boundingRectangle.width / 2) - (int) (WIDE_EDGE_SIZE / 2);
            yPosition = boundingRectangle.y + EDGE_OFFSET;
            return new Rectangle(xPosition, yPosition, WIDE_EDGE_SIZE, NARROW_EDGE_SIZE);
        // west rectangle is narrower than wider, and at the left of the board view
        case WEST:
            xPosition = boundingRectangle.x + EDGE_OFFSET;
            yPosition = boundingRectangle.y + (int) (boundingRectangle.height / 2) - (int) (WIDE_EDGE_SIZE / 2);
            return new Rectangle(xPosition, yPosition, WIDE_EDGE_SIZE, NARROW_EDGE_SIZE); // used to be NARROW_EDGE_SIZE, WIDE_EDGE_SIZE);
        // south rectangle is wider than narrower, and at the bottom of the board view
        case SOUTH:
            xPosition = boundingRectangle.x + (int) (boundingRectangle.width / 2) - (int) (WIDE_EDGE_SIZE / 2);
            yPosition = boundingRectangle.y + boundingRectangle.height - EDGE_OFFSET - NARROW_EDGE_SIZE;
            return new Rectangle(xPosition, yPosition, WIDE_EDGE_SIZE, NARROW_EDGE_SIZE);
        // east rectangle is narrower than wider, and at the right of the board view, but to the left of the unit overview panel
        case EAST:
            int extraXOffset = GUIPreferences.getInstance().getShowUnitOverview() ? UnitOverview.getUIWidth() : 0;
            xPosition = boundingRectangle.x + boundingRectangle.width - WIDE_EDGE_SIZE - EDGE_OFFSET - extraXOffset;
            yPosition = boundingRectangle.y + (int) (boundingRectangle.height / 2) - (int) (NARROW_EDGE_SIZE / 2);
            return new Rectangle(xPosition, yPosition, WIDE_EDGE_SIZE, NARROW_EDGE_SIZE); // used to be NARROW_EDGE_SIZE, WIDE_EDGE_SIZE);
        default:
            return null;
                
        }
    }

    /**
     * Worker function that handles a click on a 'counterbattery fire' overlay button.
     * Possibly shows a target selection popup
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
        } else if ((eligibleTargets.size() == 1) && (eligibleTargets.get(0) != null)) {
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
    }
}
