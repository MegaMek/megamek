/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.boardview;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.text.MessageFormat;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.EntityWreckHelper;
import megamek.common.*;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.ImageUtil;

/**
 * Contains common functionality for wreck sprites (currently isometric and regular)
 * @author NickAragua
 */
public abstract class AbstractWreckSprite extends Sprite {
    protected Entity entity;

    protected Rectangle modelRect;

    protected int secondaryPos;
    
    public AbstractWreckSprite(BoardView1 boardView1) {
        super(boardView1);
    }
    
    @Override
    public Rectangle getBounds() {
        // Start with the hex and add the label
        bounds = new Rectangle(0, 0, bv.hex_size.width, bv.hex_size.height);
        
        // Move to board position, save this origin for correct drawing
        Point hexOrigin = bounds.getLocation();
        Point ePos;
        if ((secondaryPos < 0) || (secondaryPos >= entity.getSecondaryPositions().size())) {
            ePos = bv.getHexLocation(entity.getPosition());
        } else {
            ePos = bv.getHexLocation(entity.getSecondaryPositions().get(secondaryPos));
        }
        bounds.setLocation(hexOrigin.x + ePos.x, hexOrigin.y + ePos.y);

        return bounds;
    }

    /**
     * Creates the sprite for this entity. It is an extra pain to create
     * transparent images in AWT.
     */
    @Override
    public void prepare() {
        // create image for buffer
        image = ImageUtil.createAcceleratedImage(BoardView1.HEX_W, BoardView1.HEX_H);
        Graphics2D graph = (Graphics2D) image.getGraphics();
        
        // if the entity is underwater or would sink underwater, we want to make the wreckage translucent
        // so it looks like it sunk
        boolean entityIsUnderwater = (entity.relHeight() < 0) ||
                ((entity.relHeight() >= 0) && entity.getGame().getBoard().getHex(entity.getPosition()).containsTerrain(Terrains.WATER)) &&
                !EntityWreckHelper.entityOnBridge(entity);
        
        if (entityIsUnderwater) {
            graph.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.35f));
        }

        // draw the 'destroyed decal' where appropriate
        boolean displayDestroyedDecal = EntityWreckHelper.displayDestroyedDecal(entity);
        
        if (displayDestroyedDecal) {
            Image destroyed = bv.tileManager.bottomLayerWreckMarkerFor(entity, 0);
            if (null != destroyed) {
                graph.drawImage(destroyed, 0, 0, this);
            }
        }
        
        // draw the 'fuel leak' decal where appropriate
        boolean drawFuelLeak = EntityWreckHelper.displayFuelLeak(entity);
        
        if (drawFuelLeak) {
            Image fuelLeak = bv.tileManager.bottomLayerFuelLeakMarkerFor(entity);
            if (null != fuelLeak) {
                graph.drawImage(fuelLeak, 0, 0, this);
            }
        }
        
        // draw the 'tires' or 'tracks' decal where appropriate
        boolean drawMotiveWreckage = EntityWreckHelper.displayMotiveDamage(entity);
        
        if (drawMotiveWreckage) {
            Image motiveWreckage = bv.tileManager.bottomLayerMotiveMarkerFor(entity);
            if (null != motiveWreckage) {
                graph.drawImage(motiveWreckage, 0, 0, this);
            }
        }
        
        // Draw wreck image, if we've got one.
        Image wreck;
        
        if (EntityWreckHelper.displayDevastation(entity)) {
            // objects in space should not have craters
            wreck = entity.getGame().getBoard().inSpace() ?
                    bv.tileManager.wreckMarkerFor(entity, secondaryPos) :
                    bv.tileManager.getCraterFor(entity, secondaryPos);
        } else {
            wreck = EntityWreckHelper.useExplicitWreckImage(entity) ? 
                        bv.tileManager.wreckMarkerFor(entity, secondaryPos) :
                        bv.tileManager.imageFor(entity, secondaryPos);
        }

        if (null != wreck) {
            graph.drawImage(wreck, 0, 0, this);
        }
        
        if (entityIsUnderwater) {
            graph.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 1.0f));
        }
        
        // create final image
        image = bv.getScaledImage(image, false);
        graph.dispose();
    }

    /**
     * Overrides to provide for a smaller sensitive area.
     */
    @Override
    public boolean isInside(Point point) {
        return false;
    }
    
    public Coords getPosition() {
        if (secondaryPos < 0 || secondaryPos >= entity.getSecondaryPositions().size()) {
            return entity.getPosition();
        } else {
            return entity.getSecondaryPositions().get(secondaryPos);
        }
    }
    
    @Override
    public StringBuffer getTooltip() {
        StringBuffer result = new StringBuffer();
        result.append(Messages.getString("BoardView1.Tooltip.Wreckof"));
        result.append(entity.getChassis());
        result.append(MessageFormat.format(" ({0})", entity.getOwner().getName()));
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(MessageFormat.format(" [ID: {0}]", entity.getId()));
        }
        return result;
    }
}
