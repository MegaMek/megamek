/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.client;

// Defines Iterator class for JDK v1.1
import com.sun.java.util.collections.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.Enumeration;

import megamek.client.util.*;
import megamek.common.*;
import megamek.common.actions.*;

/**
 * Displays the board; lets the user scroll around and select points on it.
 */
public class BoardView1
    extends Canvas
    implements BoardListener, MouseListener, MouseMotionListener, KeyListener,
    Runnable
{
    private static final int        TIP_DELAY = 1000;
    private static final int        TRANSPARENT = 0xFFFF00FF;
    private static final Dimension  HEX_SIZE = new Dimension(84, 72);

    private static final Font       FONT_HEXNUM = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font       FONT_ELEV = new Font("SansSerif", Font.PLAIN, 9);

    private Game game;
    private Frame frame;

    private Point mousePos = new Point();
    private Rectangle view = new Rectangle();
    private Point offset = new Point();
    private Dimension boardSize;

    // scrolly stuff:
    private boolean isScrolling = false;
    private Thread scroller = new Thread(this);
    private Point scroll = new Point();

    // back buffer to draw to
    private Image backImage;
    private Dimension backSize;
    private Graphics backGraph;

    // buffer for all the hexes you can possibly see
    private Image boardImage;
    private Rectangle boardRect;
    private Graphics boardGraph;

    // entity sprites
    private Vector entitySprites = new Vector();
    private Hashtable entitySpriteIds = new Hashtable();

    // sprites for the three selection cursors
    private CursorSprite cursorSprite;
    private CursorSprite highlightSprite;
    private CursorSprite selectedSprite;

    // sprite for current movement
    private Vector pathSprites = new Vector();

    // vector of sprites for all firing lines
    private Vector attackSprites = new Vector();

    // vector of sprites for C3 network lines
    private Vector C3Sprites = new Vector();

    // tooltip stuff
    private Window tipWindow;
    private boolean isTipPossible = false;
    private long lastIdle;

    private TilesetManager tileManager = new TilesetManager(this);
    
    // polygons for a few things
    private Polygon              hexPoly;
    private Polygon[]            facingPolys;
    private Polygon[]            movementPolys;

    // should we mark deployment hexes for a player?
    private Player               m_plDeployer = null;

    /**
     * Construct a new board view for the specified game
     */
    public BoardView1(Game game, Frame frame) {
        this.game = game;
        this.frame = frame;

        game.getBoard().addBoardListener(this);
        scroller.start();
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        updateBoardSize();

        // tooltip
        tipWindow = new Window(frame);

        initPolys();

        cursorSprite = new CursorSprite(Color.cyan);
        highlightSprite = new CursorSprite(Color.white);
        selectedSprite = new CursorSprite(Color.blue);
    }

    public void paint(Graphics g) {
        update(g);
    }

    /**
     * Draw the screen!
     */
    public void update(Graphics g) {
        final Dimension size = getSize();
//        final long start = System.currentTimeMillis();

        // update view, offset
        view.setLocation(scroll);
        view.setSize(getOptimalView(size));
        offset.setLocation(getOptimalOffset(size));

        if (!this.isTileImagesLoaded()) {
            g.drawString("loading images...", 20, 50);
            if (!tileManager.isStarted()) {
                System.out.println("boardview1: load all images called");
                tileManager.loadNeededImages(game);
            }
            return;
        }

        // make sure back buffer is valid
        if (backGraph == null || !view.getSize().equals(backSize)) {
            // make new back buffer
            backSize = view.getSize();
            backImage = createImage(backSize.width, backSize.height);
            backGraph = backImage.getGraphics();
            // clear current graphics
            g.clearRect(0, 0, size.width, size.height);
        }

        // make sure board rectangle contains our current view rectangle
        if (boardImage == null || !boardRect.union(view).equals(boardRect)) {
            updateBoardImage();
        }

        // draw onto the back buffer:

        // draw the board
        backGraph.drawImage(boardImage, 0, 0, this);

        // draw highlight border
        drawSprite(highlightSprite);

        // draw C3 links
        drawSprites(C3Sprites);

        // draw onscreen entities
        drawSprites(entitySprites);

        // draw onscreen attacks
        drawSprites(attackSprites);

        // draw movement, if valid
        drawSprites(pathSprites);

        // draw cursors
        drawSprite(cursorSprite);
        drawSprite(selectedSprite);

        // draw deployment indicators
        if (m_plDeployer != null) {
            drawDeployment();
        }

        // draw the back buffer onto the screen
        g.drawImage(backImage, offset.x, offset.y, this);


//        final long finish = System.currentTimeMillis();
//        System.out.println("BoardView1: updated screen in " + (finish - start) + " ms.");
    }

    /**
     * Updates the boardSize variable with the proper values for this board.
     */
    private void updateBoardSize() {
        int width = game.getBoard().width * 63 + 21;
        int height = game.getBoard().height * 72 + 36;
        boardSize = new Dimension(width, height);
    }

    /**
     * Think up the size of the view rectangle based on the size of the component
     * and the size of board
     */
    private Dimension getOptimalView(Dimension size) {
        return new Dimension(Math.min(size.width, boardSize.width), Math.min(size.height, boardSize.height));
    }

    /**
     * Where should the offset be for this screen size?
     */
    private Point getOptimalOffset(Dimension size) {
            int ox = 0;
            int oy = 0;
            if (size.width > boardSize.width) {
                ox = (size.width - boardSize.width) / 2;
            }
            if (size.height > boardSize.height) {
                oy = (size.height - boardSize.height) / 2;
            }
            return new Point(ox, oy);
    }

    /**
     * Repaint the bounds of a sprite, offset by view
     */
    private void repaintBounds(Rectangle bounds) {
        if (view != null) {
            repaint(bounds.x - view.x + offset.x, bounds.y - view.y + offset.y, bounds.width, bounds.height);
        }
    }

    /**
     * Looks through a vector of buffered images and draws them if they're
     * onscreen.
     */
    private synchronized void drawSprites(Vector spriteVector) {
        for (final Iterator i = spriteVector.iterator(); i.hasNext();) {
            final Sprite sprite = (Sprite)i.next();
            drawSprite(sprite);
        }
    }

    /**
     * Draws a sprite, if it is in the current view
     */
    private final void drawSprite(Sprite sprite) {
        if (view.intersects(sprite.getBounds())) {
            final int drawX = sprite.getBounds().x - view.x;
            final int drawY = sprite.getBounds().y - view.y;
            if (!sprite.isReady()) {
                sprite.prepare();
            }
            sprite.drawOnto(backGraph, drawX, drawY, this);
        }
    }

    /**
     * Draw an outline around legal deployment hexes
     */
    private void drawDeployment() {
        // only update visible hexes
        int drawX = view.x / 63 - 1;
        int drawY = view.y / 72 - 1;

        int drawWidth = view.width / 63 + 3;
        int drawHeight = view.height / 72 + 3;

        // loop through the hexes
        for (int i = 0; i < drawHeight; i++) {
            for (int j = 0; j < drawWidth; j++) {
                Coords c = new Coords(j + drawX, i + drawY);
                Point p = getHexLocation(c);
                p.translate(-(view.x), -(view.y));
                if (game.board.isLegalDeployment(c, m_plDeployer)) {
                    backGraph.setColor(Color.yellow);
                    int[] xcoords = { p.x + 21, p.x + 62, p.x + 83, p.x + 83,
                            p.x + 62, p.x + 21, p.x, p.x };
                    int[] ycoords = { p.y, p.y, p.y + 35, p.y + 36, p.y + 71,
                            p.y + 71, p.y + 36, p.y + 35 };
                    backGraph.drawPolygon(xcoords, ycoords, 8);
                }
            }
        }
    }

    /**
     * Updates the board buffer to contain all the hexes needed by the view.
     */
    private void updateBoardImage() {
        // check to make sure image is big enough
        if (boardGraph == null || view.width > boardRect.width
            || view.height > boardRect.height) {
            boardImage = createImage(view.width, view.height);
            boardGraph = boardImage.getGraphics();

            System.out.println("boardview1: made a new board buffer " + boardRect);
            boardRect = new Rectangle(view);
            drawHexes(view);
        }
        if (!boardRect.union(view).equals(boardRect)) {
            moveBoardImage();
        }
    }

    /**
     * Moves the board view to another area.
     */
    private void moveBoardImage() {
        // salvage the old
        boardGraph.setClip(0, 0, boardRect.width, boardRect.height);
        boardGraph.copyArea(0, 0, boardRect.width, boardRect.height,
                            boardRect.x - view.x, boardRect.y - view.y);

        // what's left to paint?
        int midX = Math.max(view.x, boardRect.x);
        int midWidth = view.width - Math.abs(view.x - boardRect.x);
        Rectangle unLeft = new Rectangle(view.x, view.y, boardRect.x - view.x, view.height);
        Rectangle unRight = new Rectangle(boardRect.x + boardRect.width, view.y, view.x -boardRect.x, view.height);
        Rectangle unTop = new Rectangle(midX, view.y, midWidth, boardRect.y - view.y);
        Rectangle unBottom = new Rectangle(midX, boardRect.y + boardRect.height, midWidth, view.y - boardRect.y);

        // update boardRect
        boardRect = new Rectangle(view);

        // paint needed areas
        if (unLeft.width > 0) {
            drawHexes(unLeft);
        } else if (unRight.width > 0) {
            drawHexes(unRight);
        }
        if (unTop.height > 0) {
            drawHexes(unTop);
        } else if (unBottom.height > 0) {
            drawHexes(unBottom);
        }
    }

    /**
     * Redraws all hexes in the specified rectangle
     */
    private void drawHexes(Rectangle rect) {
        int drawX = rect.x / 63 - 1;
        int drawY = rect.y / 72 - 1;

        int drawWidth = rect.width / 63 + 3;
        int drawHeight = rect.height / 72 + 3;

        // only draw what we came to draw
        boardGraph.setClip(rect.x - boardRect.x, rect.y - boardRect.y,
                           rect.width, rect.height);

        // clear, if we need to
        if (rect.x < 21) {
            boardGraph.clearRect(rect.x - boardRect.x, rect.y - boardRect.y, 21 - rect.x, rect.height);
        }
        if (rect.y < 36) {
            boardGraph.clearRect(rect.x - boardRect.x, rect.y - boardRect.y, rect.width, 36 - rect.y);
        }
        if (rect.x > boardSize.width - view.width - 21) {
            boardGraph.clearRect(boardRect.width - 21, rect.y - boardRect.y, 21, rect.height);
        }
        if (rect.y > boardSize.height - view.height - 36) {
            boardGraph.clearRect(rect.x - boardRect.x, boardRect.height - 36, rect.width, 36);
        }

        // draw some hexes
        for (int i = 0; i < drawHeight; i++) {
            for (int j = 0; j < drawWidth; j++) {
                drawHex(new Coords(j + drawX, i + drawY));
            }
        }
    }

    /**
     * Redraws a hex and all the hexes immediately around it.  Used when the
     * hex is on the screen, as opposed to when it is scrolling onto the screen,
     * so it resets the clipping rectangle before drawing.
     */
    private void redrawAround(Coords c) {
        boardGraph.setClip(0, 0, boardRect.width, boardRect.height);
        drawHex(c);
        drawHex(c.translated(0));
        drawHex(c.translated(1));
        drawHex(c.translated(2));
        drawHex(c.translated(3));
        drawHex(c.translated(4));
        drawHex(c.translated(5));
    }

    /**
     * Draws a hex onto the board buffer.  This assumes that boardRect is
     * current, and does not check if the hex is visible.
     */
    private void drawHex(Coords c) {
        if (!game.board.contains(c)) {
            return;
        }

        final Hex hex = game.board.getHex(c);
        final Point hexLoc = getHexLocation(c);

        int level = hex.getElevation();
        int depth = hex.depth();

        // offset drawing point
        int drawX = hexLoc.x - boardRect.x;
        int drawY = hexLoc.y - boardRect.y;

        // draw picture
        boardGraph.drawImage(tileManager.baseFor(hex), drawX, drawY, this);
        if (tileManager.supersFor(hex) != null) {
            for (Iterator i = tileManager.supersFor(hex).iterator(); i.hasNext();) {
                boardGraph.drawImage((Image)i.next(), drawX, drawY, this);
            }
        }

        // draw hex number
        boardGraph.setColor(Settings.mapTextColor);
        boardGraph.setFont(FONT_HEXNUM);
        boardGraph.drawString(c.getBoardNum(), drawX + 30, drawY + 12);
        // level | depth
        boardGraph.setFont(FONT_ELEV);
        if (level != 0 && depth == 0) {
            boardGraph.drawString("LEVEL " + level, drawX + 24, drawY + 70);
        } else if (depth != 0 && level == 0) {
            boardGraph.drawString("DEPTH " + depth, drawX + 24, drawY + 70);
        } else if (level != 0 && depth != 0) {
            boardGraph.drawString("LEVEL " + level, drawX + 24, drawY + 60);
            boardGraph.drawString("DEPTH " + depth, drawX + 24, drawY + 70);
        }
        // draw elevation borders
        boardGraph.setColor(Color.black);
        if (drawElevationLine(c, 0)) {
            boardGraph.drawLine(drawX + 21, drawY, drawX + 62, drawY);
        }
        if (drawElevationLine(c, 1)) {
            boardGraph.drawLine(drawX + 62, drawY, drawX + 83, drawY + 35);
        }
        if (drawElevationLine(c, 2)) {
            boardGraph.drawLine(drawX + 83, drawY + 36, drawX + 62, drawY + 71);
        }
        if (drawElevationLine(c, 3)) {
            boardGraph.drawLine(drawX + 62, drawY + 71, drawX + 21, drawY + 71);
        }
        if (drawElevationLine(c, 4)) {
            boardGraph.drawLine(drawX + 21, drawY + 71, drawX, drawY + 36);
        }
        if (drawElevationLine(c, 5)) {
            boardGraph.drawLine(drawX, drawY + 35, drawX + 21, drawY);
        }

    }

    /**
     * Returns true if an elevation line should be drawn between the starting
     * hex and the hex in the direction specified.  Results should be
     * transitive, that is, if a line is drawn in one direction, it should be
     * drawn in the opposite direction as well.
     */
    private final boolean drawElevationLine(Coords src, int direction) {
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHexInDir(src, direction);
        return destHex != null && srcHex.floor() != destHex.floor();
    }

    /**
     * Returns the absolute position of the upper-left hand corner
     * of the hex graphic
     */
    private Point getHexLocation(int x, int y) {
        return new Point(x * 63, y * 72 + ((x & 1) == 1 ? 36 : 0));
    }
    private Point getHexLocation(Coords c) {
        return getHexLocation(c.x, c.y);
    }

    /**
     * Returns the coords at the specified point
     */
    Coords getCoordsAt(Point p) {
        final int x = (p.x + scroll.x - offset.x) / 63;
        final int y = ((p.y + scroll.y - offset.y) - ((x & 1) == 1 ? 36 : 0)) / 72;
        return new Coords(x, y);
    }

    /**
     * Shows the tooltip thinger
     */
    private void showTooltip() {
      try {
        final Point tipLoc = new Point(getLocationOnScreen());
        // retrieve tip text
        String[] tipText = getTipText(mousePos);
        if (tipText == null) {
            return;
        }

        // update tip text
        tipWindow.removeAll();
        tipWindow.add(new TooltipCanvas(tipText));
        tipWindow.pack();

        tipLoc.translate(mousePos.x, mousePos.y + 20);

        // adjust horizontal location for the tipWindow if it goes off the frame
        if (frame.getLocation().x + frame.getSize().width < tipLoc.x + tipWindow.getSize().width + 10) {
            if (frame.getSize().width > tipWindow.getSize().width) {
                // bound it by the right edge of the frame
                tipLoc.x -= tipLoc.x + tipWindow.getSize().width + 10 - frame.getSize().width - frame.getLocation().x;
            }
            else {
                // too big to fit, left justify to the frame (roughly).
                // how do I extract the first term of HEX_SIZE to use here?--LDE
                tipLoc.x = getLocationOnScreen().x + 84;
            }
        }

        // set tip location
        tipWindow.setLocation(tipLoc);

        tipWindow.show();
      } catch (Exception e) {
        tipWindow = new Window(frame);
      }
    }

    /**
     * The text to be displayed when the mouse is at a certain point
     */
    private String[] getTipText(Point point) {

        int stringsSize = 0;
        Hex mhex = null;

        // first, we have to determine how much text we are going to have
        // are we on a hex?
        final Coords mcoords = getCoordsAt(point);
        if (game.board.contains(mcoords)) {
            mhex = game.board.getHex(mcoords);
            stringsSize += 1;
        }

        // check if it's on any entities
        for (Iterator i = entitySprites.iterator(); i.hasNext();) {
            final EntitySprite eSprite = (EntitySprite)i.next();
            if (eSprite.isInside(point)) {
                stringsSize += 3;
            }
        }

        // check if it's on any attacks
        for (Iterator i = attackSprites.iterator(); i.hasNext();) {
            final AttackSprite aSprite = (AttackSprite)i.next();
            if (aSprite.isInside(point)) {
                stringsSize += 1 + aSprite.weaponDescs.size();
            }
        }

 	// If the hex contains a building or rubble, make more space.
 	if ( mhex != null &&
 	     (mhex.contains(Terrain.RUBBLE) ||
 	      mhex.contains(Terrain.BUILDING)) ) {
            stringsSize += 1;
 	}
        
        // if the size is zip, you must a'quit
        if (stringsSize == 0) {
            return null;
        }

        // now we can allocate an array of strings
        String[] strings = new String[stringsSize];
        int stringsIndex = 0;

        // are we on a hex?
        if (mhex != null) {
            strings[stringsIndex] = "Hex " + mcoords.getBoardNum()
                        + "; level " + mhex.getElevation();
            stringsIndex += 1;

	    // Do we have rubble?
	    if ( mhex.contains(Terrain.RUBBLE) ) {
		strings[stringsIndex] = "Rubble";
		stringsIndex += 1;
            }

            // Do we have a building?
            else if ( mhex.contains(Terrain.BUILDING) ) {
                // Get the building.
                Building bldg = game.board.getBuildingAt( mcoords );
                StringBuffer buf = new StringBuffer( "Height " );
                // Each hex of a building has its own elevation.
                buf.append( mhex.levelOf(Terrain.BLDG_ELEV) );
                buf.append( " " );
                buf.append( bldg.toString() );
                buf.append( ", CF: " );
                buf.append( bldg.getCurrentCF() );
                strings[stringsIndex] = buf.toString();
                stringsIndex += 1;
            }
        }

        // check if it's on any entities
        for (Iterator i = entitySprites.iterator(); i.hasNext();) {
            final EntitySprite eSprite = (EntitySprite)i.next();
            if (eSprite.isInside(point)) {
                final String[] entityStrings = eSprite.getTooltip();
                java.lang.System.arraycopy(entityStrings, 0, strings, stringsIndex, entityStrings.length);
                stringsIndex += entityStrings.length;
            }
        }

        // check if it's on any attacks
        for (Iterator i = attackSprites.iterator(); i.hasNext();) {
            final AttackSprite aSprite = (AttackSprite)i.next();
            if (aSprite.isInside(point)) {
                final String[] attackStrings = aSprite.getTooltip();
                java.lang.System.arraycopy(attackStrings, 0, strings, stringsIndex, attackStrings.length);
                stringsIndex += 1 + aSprite.weaponDescs.size();
            }
        }

        return strings;
    }

    /**
     * Hides the tooltip thinger
     */
    public void hideTooltip() {
        tipWindow.setVisible(false);
    }

    /**
     * Returns true if the tooltip is showing
     */
    private boolean isTipShowing() {
        return tipWindow.isShowing();
    }

    /**
     * Checks if the mouse has been idling for a while and if so, shows the
     * tooltip window
     */
    private void checkTooltip() {
        if (isTipShowing()) {
            if (!isTipPossible) {
                hideTooltip();
            }
        } else if (isTipPossible && System.currentTimeMillis() - lastIdle > TIP_DELAY) {
            showTooltip();
        }
    }

    /**
     * Clears the sprite for an entity and prepares it to be re-drawn.  Replaces
     * the old sprite with the new!
     *
     *  Try to prevent annoying ConcurrentModificationExceptions
     */
    public void redrawEntity(Entity entity) {
        EntitySprite sprite = (EntitySprite)entitySpriteIds.get(new Integer(entity.getId()));
        Vector newSprites = new Vector(entitySprites);
        Hashtable newSpriteIds = new Hashtable(entitySpriteIds);


        if (sprite != null) {
            newSprites.removeElement(sprite);
        }

        if (entity.getPosition() != null) {
            sprite = new EntitySprite(entity);
            newSprites.addElement(sprite);
            newSpriteIds.put(new Integer(entity.getId()), sprite);
        }

        entitySprites = newSprites;
        entitySpriteIds = newSpriteIds;

        for (java.util.Enumeration i = C3Sprites.elements(); i.hasMoreElements();) {
          final C3Sprite c3sprite = (C3Sprite)i.nextElement();
          if (c3sprite.entityId == entity.getId())
              C3Sprites.removeElement(c3sprite);
          else if(c3sprite.masterId == entity.getId()) {
              if(entity.hasC3()) { // only redraw client-to-master; otherwise we leave stray lines when we move
                  C3Sprites.addElement(new C3Sprite(game.getEntity(c3sprite.entityId), game.getEntity(c3sprite.masterId)));
              }
              C3Sprites.removeElement(c3sprite);

          }
        }

        if(entity.hasC3() || entity.hasC3i()) addC3Link(entity);

        repaint(100);
    }

    /**
     * Clears all old entity sprites out of memory and sets up new ones.
     */
    private void redrawAllEntities() {
        Vector newSprites = new Vector(game.getNoOfEntities());
        Hashtable newSpriteIds = new Hashtable(game.getNoOfEntities());

        clearC3Networks();
        for (java.util.Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (entity.getPosition() == null) continue;

            EntitySprite sprite = new EntitySprite(entity);
            newSprites.add(sprite);
            newSpriteIds.put(new Integer(entity.getId()), sprite);

            if(entity.hasC3() || entity.hasC3i()) addC3Link(entity);
        }

        entitySprites = newSprites;
        entitySpriteIds = newSpriteIds;

        repaint(100);
    }

    /**
     * Moves the cursor to the new position, or hides it, if newPos is null
     */
    private void moveCursor(CursorSprite cursor, Coords newPos) {
        final Rectangle oldBounds = new Rectangle(cursor.getBounds());
        if (newPos != null) {
            cursor.setLocation(getHexLocation(newPos));
        } else {
            cursor.setLocation(-100, -100);
        }
        // repaint affected area
        repaintBounds(oldBounds);
        repaintBounds(cursor.getBounds());
    }


    public void centerOnHex(Coords c) {
        scroll.setLocation(getHexLocation(c));
        scroll.translate(42 - (view.width / 2), 36 - (view.height / 2));

        isScrolling = false;
        checkScrollBounds();
        repaint();
    }

    /**
     * Clears the old movement data and draws the new.  Since it's less
     * expensive to check for and reuse old step sprites than to make a whole
     * new one, we do that.
     */
    public void drawMovementData(Entity entity, MovementData md) {
        Vector temp = pathSprites;

        clearMovementData();

        md.clearAllFlags();
        Compute.compile(game, entity.getId(), md);

        for (java.util.Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MovementData.Step step = (MovementData.Step)i.nextElement();
            // check old movement path for reusable step sprites
            boolean found = false;
            for (Iterator j = temp.iterator(); j.hasNext();) {
                final StepSprite sprite = (StepSprite)j.next();
                if (sprite.getStep().equals(step)) {
                    pathSprites.addElement(sprite);
                    found = true;
                }
            }
            if (!found) {
                pathSprites.addElement(new StepSprite(step));
            }
        }
    }

    /**
     * Clears current movement data from the screen
     */
    public void clearMovementData() {
        Vector temp = pathSprites;
        pathSprites = new Vector();
        for (Iterator i = temp.iterator(); i.hasNext();) {
            final Sprite sprite = (Sprite)i.next();
            repaintBounds(sprite.getBounds());
        }
    }

    /**
     * Specifies that this should mark the deployment hexes for a player.  If
     * the player is set to null, no hexes will be marked.
     */
    public void markDeploymentHexesFor(Player p)
    {
        m_plDeployer = p;
    }

    /**
     * Adds a c3 line to the sprite list.
     */
    public void addC3Link(Entity e) {
        if (e.getPosition() == null) return;

        if(e.hasC3i()) {
            for (java.util.Enumeration i = game.getEntities(); i.hasMoreElements();) {
                final Entity fe = (Entity)i.nextElement();
                if (fe.getPosition() == null) return;
                if ( e.onSameC3NetworkAs(fe)) {
                    C3Sprites.addElement(new C3Sprite(e, fe));
                }
            }
        }
        else if(e.getC3Master() != null) {
            Entity eMaster = e.getC3Master();
            if (eMaster.getPosition() == null) return;

            // ECM cuts off the network
            if (!Compute.isAffectedByECM(e, e.getPosition(), eMaster.getPosition())) {
                C3Sprites.addElement(new C3Sprite(e, e.getC3Master()));
            }
        }
    }

    /**
     * Adds an attack to the sprite list.
     */
    public void addAttack(AttackAction aa) {
        // do not make a sprite unless we're aware of both entities
        // this is not a great solution but better than a crash
        Entity ae = game.getEntity(aa.getEntityId());
        Targetable t = game.getTarget(aa.getTargetType(), aa.getTargetId());
        if (ae == null || t == null) {
            return;
        }

        for (final Iterator i = attackSprites.iterator(); i.hasNext();) {
            final AttackSprite sprite = (AttackSprite)i.next();

            // can we just add this attack to an existing one?
            if (sprite.getEntityId() == aa.getEntityId()
                && sprite.getTargetId() == aa.getTargetId()) {
                // use existing attack, but add this weapon
                if (aa instanceof WeaponAttackAction) {
                    sprite.addWeapon((WeaponAttackAction)aa);
                }
                if (aa instanceof KickAttackAction) {
                    sprite.addWeapon((KickAttackAction)aa);
                }
                if (aa instanceof PunchAttackAction) {
                    sprite.addWeapon((PunchAttackAction)aa);
                }
                if (aa instanceof PushAttackAction) {
                    sprite.addWeapon((PushAttackAction)aa);
                }
                if (aa instanceof ClubAttackAction) {
                    sprite.addWeapon((ClubAttackAction)aa);
                }
                if (aa instanceof ChargeAttackAction) {
                    sprite.addWeapon((ChargeAttackAction)aa);
                }
                if (aa instanceof DfaAttackAction) {
                    sprite.addWeapon((DfaAttackAction)aa);
                }                
                return;
            }
        }

        // no re-use possible, add a new one
        attackSprites.addElement(new AttackSprite(aa));
    }
    
    /** Removes all attack sprites from a certain entity */
    public void removeAttacksFor(int entityId) {
        // or rather, only keep sprites NOT for that entity
        Vector toKeep = new Vector(attackSprites.size());
        for (Iterator i = attackSprites.iterator(); i.hasNext();) {
            AttackSprite sprite = (AttackSprite)i.next();
            if (sprite.getEntityId() != entityId) {
                toKeep.addElement(sprite);
            }
        }
        this.attackSprites = toKeep;
    }

    /**
     * Clears out all attacks and re-adds the ones in the current game.
     */
    public void refreshAttacks() {
        clearAllAttacks();
        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();
            if (ea instanceof AttackAction) {
                addAttack((AttackAction)ea);
            }
        }
        for (Enumeration i = game.getCharges(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();
            if (ea instanceof AttackAction) {
                addAttack((AttackAction)ea);
            }
        }
    }

    public void clearC3Networks() {
        C3Sprites.removeAllElements();
    }

    /**
     * Clears out all attacks that were being drawn
     */
    public void clearAllAttacks() {
        attackSprites.removeAllElements();
    }

    public Image baseFor(Hex hex) {
        return tileManager.baseFor(hex);
    }

    public com.sun.java.util.collections.List supersFor(Hex hex) {
        return tileManager.supersFor(hex);
    }








    public void boardHexMoused(BoardEvent b) {
        ;
    }
    public void boardHexCursor(BoardEvent b) {
        moveCursor(cursorSprite, b.getCoords());
    }
    public void boardHexSelected(BoardEvent b) {
        moveCursor(selectedSprite, b.getCoords());
    }
    public void boardHexHighlighted(BoardEvent b) {
        moveCursor(highlightSprite, b.getCoords());
    }
    public void boardChangedHex(BoardEvent b) {
        tileManager.waitForHex( game.getBoard().getHex(b.getCoords()) );
        if (boardGraph != null) {
            boardGraph.setClip(0, 0, boardRect.width, boardRect.height);
            redrawAround(b.getCoords());
        }
    }
    public void boardNewBoard(BoardEvent b) {
        updateBoardSize();
        backGraph = null;
        backImage = null;
        backSize = null;
        boardImage = null;
        boardGraph = null;
        tileManager.reset();
    }

    public void boardNewEntities(BoardEvent e) {
        redrawAllEntities();
    }
    public void boardChangedEntity(BoardEvent e) {
        redrawEntity(e.getEntity());
    }



    /**
     * If the mouse is at the edges of the screen, this
     * scrolls the board image on the canvas.
     */
    public void doScroll() {
        if (!isScrolling) {
            return;
        }
        final Point oldScroll = new Point(scroll);
        final int sf = 3; // scroll factor
        // adjust x scroll
        // scroll when the mouse is at the edges
        if (mousePos.x < 100) {
            scroll.x -= (100 - mousePos.x) / sf;
        } else if (mousePos.x > (backSize.width - 100)) {
            scroll.x -= ((backSize.width - 100) - mousePos.x) / sf;
        }
        // scroll when the mouse is at the edges
        if (mousePos.y < 100) {
            scroll.y -= (100 - mousePos.y) / sf;
        } else if (mousePos.y > (backSize.height - 100)) {
            scroll.y -= ((backSize.height - 100) - mousePos.y) / sf;
        }
        checkScrollBounds();
        if (!oldScroll.equals(scroll)) {
            repaint();
        }
    }

    /**
     * Makes sure that the scroll dimensions stay in bounds
     */
    public void checkScrollBounds() {
        if (scroll.x < 0) {
            scroll.x = 0;
        } else if (scroll.x > (boardSize.width - view.width)) {
            scroll.x = (boardSize.width - view.width);
        }

        if (scroll.y < 0) {
            scroll.y = 0;
        } else if (scroll.y > (boardSize.height - view.height)) {
            scroll.y = (boardSize.height - view.height);
        }
    }


    /**
     * Initializes the various overlay polygons with their
     * vertices.
     */
    public void initPolys() {
        // hex polygon
        hexPoly = new Polygon();
        hexPoly.addPoint(21, 0);
        hexPoly.addPoint(62, 0);
        hexPoly.addPoint(83, 35);
        hexPoly.addPoint(83, 36);
        hexPoly.addPoint(62, 71);
        hexPoly.addPoint(21, 71);
        hexPoly.addPoint(0, 36);
        hexPoly.addPoint(0, 35);

        // facing polygons
        facingPolys = new Polygon[6];
        facingPolys[0] = new Polygon();
        facingPolys[0].addPoint(41, 3);
        facingPolys[0].addPoint(38, 6);
        facingPolys[0].addPoint(45, 6);
        facingPolys[0].addPoint(42, 3);
        facingPolys[1] = new Polygon();
        facingPolys[1].addPoint(69, 17);
        facingPolys[1].addPoint(64, 17);
        facingPolys[1].addPoint(68, 23);
        facingPolys[1].addPoint(70, 19);
        facingPolys[2] = new Polygon();
        facingPolys[2].addPoint(69, 53);
        facingPolys[2].addPoint(68, 49);
        facingPolys[2].addPoint(64, 55);
        facingPolys[2].addPoint(68, 54);
        facingPolys[3] = new Polygon();
        facingPolys[3].addPoint(41, 68);
        facingPolys[3].addPoint(38, 65);
        facingPolys[3].addPoint(45, 65);
        facingPolys[3].addPoint(42, 68);
        facingPolys[4] = new Polygon();
        facingPolys[4].addPoint(15, 53);
        facingPolys[4].addPoint(18, 54);
        facingPolys[4].addPoint(15, 48);
        facingPolys[4].addPoint(14, 52);
        facingPolys[5] = new Polygon();
        facingPolys[5].addPoint(13, 19);
        facingPolys[5].addPoint(15, 23);
        facingPolys[5].addPoint(19, 17);
        facingPolys[5].addPoint(17, 17);

        // movement polygons
        movementPolys = new Polygon[8];
        movementPolys[0] = new Polygon();
        movementPolys[0].addPoint(41, 65);
        movementPolys[0].addPoint(38, 68);
        movementPolys[0].addPoint(45, 68);
        movementPolys[0].addPoint(42, 65);
        movementPolys[1] = new Polygon();
        movementPolys[1].addPoint(17, 48);
        movementPolys[1].addPoint(12, 48);
        movementPolys[1].addPoint(16, 54);
        movementPolys[1].addPoint(17, 49);
        movementPolys[2] = new Polygon();
        movementPolys[2].addPoint(18, 19);
        movementPolys[2].addPoint(17, 15);
        movementPolys[2].addPoint(13, 21);
        movementPolys[2].addPoint(17, 20);
        movementPolys[3] = new Polygon();
        movementPolys[3].addPoint(41, 6);
        movementPolys[3].addPoint(38, 3);
        movementPolys[3].addPoint(45, 3);
        movementPolys[3].addPoint(42, 6);
        movementPolys[4] = new Polygon();
        movementPolys[4].addPoint(67, 15);
        movementPolys[4].addPoint(66, 19);
        movementPolys[4].addPoint(67, 20);
        movementPolys[4].addPoint(71, 20);
        movementPolys[5] = new Polygon();
        movementPolys[5].addPoint(69, 55);
        movementPolys[5].addPoint(66, 50);
        movementPolys[5].addPoint(67, 49);
        movementPolys[5].addPoint(72, 48);

        movementPolys[6] = new Polygon(); // up arrow with tail
        movementPolys[6].addPoint(35, 44);
        movementPolys[6].addPoint(30, 49);
        movementPolys[6].addPoint(33, 49);
        movementPolys[6].addPoint(33, 53);
        movementPolys[6].addPoint(38, 53);
        movementPolys[6].addPoint(38, 49);
        movementPolys[6].addPoint(41, 49);
        movementPolys[6].addPoint(36, 44);
        movementPolys[7] = new Polygon(); // down arrow with tail
        movementPolys[7].addPoint(34, 53);
        movementPolys[7].addPoint(29, 48);
        movementPolys[7].addPoint(32, 48);
        movementPolys[7].addPoint(32, 44);
        movementPolys[7].addPoint(37, 44);
        movementPolys[7].addPoint(37, 48);
        movementPolys[7].addPoint(40, 48);
        movementPolys[7].addPoint(35, 53);
    }


    //
    // Runnable
    //
    public void run() {
        final Thread currentThread = Thread.currentThread();
        while (scroller == currentThread) {
            try {
                Thread.sleep(20);
            } catch(InterruptedException ex) {
                // duh?
            }
            if (!isShowing()) {
                continue;
            }
            if (backSize != null) {
                doScroll();
                checkTooltip();
            } else {
                repaint(100);
            }
        }
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ke) {
        switch(ke.getKeyCode()) {
        case KeyEvent.VK_UP :
            scroll.y -= 36;
            break;
        case KeyEvent.VK_DOWN :
            scroll.y += 36;
            break;
        case KeyEvent.VK_LEFT :
            scroll.x -= 36;
            break;
        case KeyEvent.VK_RIGHT :
            scroll.x += 36;
            break;
        }
        if (isTipShowing()) {
            hideTooltip();
        }
        lastIdle = System.currentTimeMillis();
        checkScrollBounds();
        repaint();
    }
    public void keyReleased(KeyEvent ke) {
        ;
    }
    public void keyTyped(KeyEvent ke) {
        ;
    }

    //
    // MouseListener
    //
    public void mousePressed(MouseEvent me) {
        isScrolling = true;
        isTipPossible = false;
        if (isTipShowing()) {
            hideTooltip();
        }
        game.board.mouseAction(getCoordsAt(me.getPoint()), Board.BOARD_HEX_DRAG, me.getModifiers());
    }
    public void mouseReleased(MouseEvent me) {
        isScrolling = false;
        isTipPossible = true;
        if (me.getClickCount() == 1) {
            game.board.mouseAction(getCoordsAt(me.getPoint()), Board.BOARD_HEX_CLICK, me.getModifiers());
        } else {
            game.board.mouseAction(getCoordsAt(me.getPoint()), Board.BOARD_HEX_DOUBLECLICK, me.getModifiers());
        }
    }
    public void mouseEntered(MouseEvent me) {
        ;
    }
    public void mouseExited(MouseEvent me) {
        isTipPossible = false;
    }
    public void mouseClicked(MouseEvent me) {
        ;
    }

    //
    // MouseMotionListener
    //
    public void mouseDragged(MouseEvent me) {
        mousePos = me.getPoint();
        isTipPossible = false;
        isScrolling = true;
        if (backSize != null) {
            doScroll();
        }
        game.board.mouseAction(getCoordsAt(me.getPoint()), Board.BOARD_HEX_DRAG, me.getModifiers());
    }
    public void mouseMoved(MouseEvent me) {
        mousePos = me.getPoint();
        if (isTipShowing()) {
            hideTooltip();
        }
        lastIdle = System.currentTimeMillis();
        isTipPossible = true;
    }

    /**
     * Displays a bit of text in a box.
     *
     * TODO: make multi-line
     */
    private class TooltipCanvas extends Canvas
    {
        private String[] tipStrings;
        private Dimension size;

        public TooltipCanvas(String[] tipStrings) {
            this.tipStrings = tipStrings;

            // setup
            setFont(new Font("SansSerif", Font.PLAIN, 12));
            setBackground(SystemColor.info);
            setForeground(SystemColor.infoText);

            // determine size
            final FontMetrics fm = getFontMetrics(getFont());
            int width = 0;
            for (int i = 0; i < tipStrings.length; i++) {
                if (fm.stringWidth(tipStrings[i]) > width) {
                    width = fm.stringWidth(tipStrings[i]);
                }
            }
            size = new Dimension(width + 5, fm.getAscent() * tipStrings.length + 4);
            setSize(size);
        }

        public void paint(Graphics g) {
            final FontMetrics fm = getFontMetrics(getFont());
            g.setColor(getBackground());
            g.fillRect(0, 0, size.width, size.height);
            g.setColor(getForeground());
            g.drawRect(0, 0, size.width - 1, size.height - 1);
            for (int i = 0; i < tipStrings.length; i++) {
                g.drawString(tipStrings[i], 2, (i + 1) * fm.getAscent());
            }
        }
    }


    /**
     * Everything in the main map view is either the board or it's a sprite
     * displayed on top of the board.  Most sprites store a transparent image
     * which they draw onto the screen when told to.  Sprites keep a bounds
     * rectangle, so it's easy to tell when they'return onscreen.
     */
    private abstract class Sprite implements ImageObserver
    {
        protected Rectangle bounds;
        protected Image image;

        /**
         * Do any necessary preparation.  This is called after creation,
         * but before drawing, when a device context is ready to draw with.
         */
        public abstract void prepare();

        /**
         * When we draw our buffered images, it's necessary to implement
         * the ImageObserver interface.  This provides the necesasry
         * functionality.
         */
        public boolean imageUpdate(Image image, int infoflags, int x, int y,
                                   int width, int height) {
            if (infoflags == ImageObserver.ALLBITS) {
                prepare();
                repaint();
                return false;
            } else {
                return true;
            }
        }

        /**
         * Returns our bounding rectangle.  The coordinates here are stored
         * with the top left corner of the _board_ being 0, 0, so these do
         * not always correspond to screen coordinates.
         */
        public Rectangle getBounds() {
            return bounds;
        }

        /**
         * Are we ready to draw?  By default, checks to see that our buffered
         * image has been created.
         */
        public boolean isReady() {
            return image != null;
        }

        /**
         * Draws this sprite onto the specified graphics context.
         */
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
            if (isReady()) {
                g.drawImage(image, x, y, observer);
            } else {
                // grrr... well be ready next time!
                prepare();
            }
        }

        /**
         * Returns true if the point is inside this sprite.  Uses board
         * coordinates, not screen coordinates.   By default, just checks our
         * bounding rectangle, though some sprites override this for a smaller
         * sensitive area.
         */
        public boolean isInside(Point point) {
            return bounds.contains(point);
        }

        /**
         * Since most sprites being drawn correspond to something in the game,
         * this returns a little info for a tooltip.
         */
        private String[] getTooltip() {
            return null;
        }
    }

    /**
     * Sprite for a cursor.  Just a hexagon outline in a specified color.
     */
    private class CursorSprite extends Sprite
    {
        private Color color;

        public CursorSprite(Color color) {
            this.color = color;
            this.bounds = new Rectangle(hexPoly.getBounds().width + 1,
                                        hexPoly.getBounds().height + 1);
            this.image = null;

            // start offscreen
            bounds.setLocation(-100, -100);
        }

        public void prepare() {
            // create image for buffer
            Image tempImage = createImage(bounds.width, bounds.height);
            Graphics graph = tempImage.getGraphics();

            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);
            // draw attack poly
            graph.setColor(color);
            graph.drawPolygon(hexPoly);

            // create final image
            this.image = createImage(new FilteredImageSource(tempImage.getSource(),
                    new KeyAlphaFilter(TRANSPARENT)));
        }

        public void setLocation(int x, int y) {
            bounds.setLocation(x, y);
        }
        public void setLocation(Point point) {
            bounds.setLocation(point);
        }
    }

    /**
     * Sprite for an entity.  Changes whenever the entity changes.  Consists
     * of an image, drawn from the Tile Manager; facing and possibly secondary
     * facing arrows; armor and internal bars; and an identification label.
     */
    private class EntitySprite extends Sprite
    {
        private Entity entity;
        private Rectangle entityRect;

        public EntitySprite(Entity entity) {
            this.entity = entity;

            String shortName = entity.getShortName();
            Font font = new Font("SansSerif", Font.PLAIN, 10);
            Rectangle modelRect = new Rectangle(47, 55,
                                 getFontMetrics(font).stringWidth(shortName) + 1,
                                 getFontMetrics(font).getAscent());
            Rectangle tempBounds = new Rectangle(HEX_SIZE).union(modelRect);
            tempBounds.setLocation(getHexLocation(entity.getPosition()));

            this.bounds = tempBounds;
            this.entityRect = new Rectangle(bounds.x + 20, bounds.y + 14, 44, 44);
            this.image = null;
        }

        /**
         * Creates the sprite for this entity.  It is an extra pain to
         * create transparent images in AWT.
         */
        public void prepare() {
            // figure out size
            String shortName = entity.getShortName();
            Font font = new Font("SansSerif", Font.PLAIN, 10);
            Rectangle modelRect = new Rectangle(47, 55,
                                 getFontMetrics(font).stringWidth(shortName) + 1,
                                 getFontMetrics(font).getAscent());

            // create image for buffer
            Image tempImage;
            Graphics graph;
            try {
                tempImage = createImage(bounds.width, bounds.height);
                graph = tempImage.getGraphics();
            } catch (NullPointerException ex) {
                // argh!  but I want it!
                return;
            }

            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);

            // draw entity image
            graph.drawImage(tileManager.imageFor(entity), 0, 0, this);

            // draw box with shortName
            Color col;
            Color bcol;
            if (entity.isImmobile()) {
                col = Color.black;
                bcol = Color.lightGray;
            } else if (entity.isDone()) {
                col = Color.darkGray;
                bcol = Color.black;
            } else {
                col = Color.lightGray;
                bcol = Color.darkGray;
            }
            graph.setFont(font);
            graph.setColor(bcol);
            graph.fillRect(modelRect.x, modelRect.y, modelRect.width, modelRect.height);
            modelRect.translate(-1, -1);
            graph.setColor(col);
            graph.fillRect(modelRect.x, modelRect.y, modelRect.width, modelRect.height);
            graph.setColor(entity.isDone() ? Color.lightGray : Color.black);
            graph.drawString(shortName, modelRect.x + 1, modelRect.y + modelRect.height - 1);

            // draw facing
            graph.setColor(Color.white);
            if (entity.getFacing() != -1) {
                graph.drawPolygon(facingPolys[entity.getFacing()]);
            }

            // determine secondary facing for non-mechs & flipped arms
            int secFacing = entity.getFacing();
            if (!(entity instanceof Mech)) {
                secFacing = entity.getSecondaryFacing();
            } else if (entity.getArmsFlipped()) {
                secFacing = (entity.getFacing() + 3) % 6;
            }
            // draw red secondary facing arrow if necessary
            if (secFacing != -1 && secFacing != entity.getFacing()) {
                graph.setColor(Color.red);
                graph.drawPolygon(facingPolys[secFacing]);
            }

            // draw condition strings
            if (entity.isImmobile() && !entity.isProne()) {
                // draw "IMMOBILE"
                graph.setColor(Color.darkGray);
                graph.drawString("IMMOBILE", 18, 39);
                graph.setColor(Color.red);
                graph.drawString("IMMOBILE", 17, 38);
            } else if (!entity.isImmobile() && entity.isProne()) {
                // draw "PRONE"
                graph.setColor(Color.darkGray);
                graph.drawString("PRONE", 26, 39);
                graph.setColor(Color.yellow);
                graph.drawString("PRONE", 25, 38);
            } else if (entity.isImmobile() && entity.isProne()) {
                // draw "IMMOBILE" and "PRONE"
                graph.setColor(Color.darkGray);
                graph.drawString("IMMOBILE", 18, 35);
                graph.drawString("PRONE", 26, 48);
                graph.setColor(Color.red);
                graph.drawString("IMMOBILE", 17, 34);
                graph.setColor(Color.yellow);
                graph.drawString("PRONE", 25, 47);
            }

            // If this unit is being swarmed or is swarming another, say so.
            if ( Entity.NONE != entity.getSwarmAttackerId() ) {
                // draw "SWARMED"
                graph.setColor(Color.darkGray);
                graph.drawString("SWARMED", 17, 22);
                graph.setColor(Color.red);
                graph.drawString("SWARMED", 16, 21);
            }

            //Lets draw our armor and internal status bars
              int baseBarLength = 23;
              int barLength = 0;
              double percentRemaining = 0.00;

              percentRemaining = entity.getArmorRemainingPercent();
              barLength = (int)(baseBarLength * percentRemaining);

              graph.setColor(Color.darkGray);
              graph.fillRect(56, 7, 23, 3);
              graph.setColor(Color.lightGray);
              graph.fillRect(55, 6, 23, 3);
              graph.setColor(getStatusBarColor(percentRemaining));
              graph.fillRect(55, 6, barLength, 3);

              percentRemaining = entity.getInternalRemainingPercent();
              barLength = (int)(baseBarLength * percentRemaining);

              graph.setColor(Color.darkGray);
              graph.fillRect(56, 11, 23, 3);
              graph.setColor(Color.lightGray);
              graph.fillRect(55, 10, 23, 3);
              graph.setColor(getStatusBarColor(percentRemaining));
              graph.fillRect(55, 10, barLength, 3);

            // create final image
            this.image = createImage(new FilteredImageSource(tempImage.getSource(),
                    new KeyAlphaFilter(TRANSPARENT)));
        }

        private Color getStatusBarColor(double percentRemaining) {
          if ( percentRemaining <= .25 )
            return Color.red;
          else if ( percentRemaining <= .75 )
            return Color.yellow;
          else
            return new Color(16, 196, 16);
        }

        /**
         * Overrides to provide for a smaller sensitive area.
         */
        public boolean isInside(Point point) {
            return entityRect.contains(point.x + view.x - offset.x, point.y + view.y - offset.y);
        }

        private String[] getTooltip() {
            String[] tipStrings = new String[3];
            tipStrings[0] = entity.getChassis() + " (" + entity.getOwner().getName() + "); "
            + entity.getCrew().getGunnery() + "/" + entity.getCrew().getPiloting() + " pilot";
            tipStrings[1] = "Move " + entity.getMovementAbbr(entity.moved) + ":" + entity.delta_distance
            + " (+" + Compute.getTargetMovementModifier(game, entity.getId()).getValue() + ");"
            + " Heat " + entity.heat;
            tipStrings[2] = "Armor " + entity.getTotalArmor()
                            + "; Internal " + entity.getTotalInternal();
            return tipStrings;
        }
    }

    /**
     * Sprite for a step in a movement path.  Only one sprite should exist for
     * any hex in a path.  Contains a colored number, and arrows indicating
     * entering, exiting or turning.
     */
    private class StepSprite extends Sprite
    {
        private MovementData.Step step;

        public StepSprite(MovementData.Step step) {
            this.step = (MovementData.Step)step.clone();

            // step is the size of the hex that this step is in
            bounds = new Rectangle(getHexLocation(step.getPosition()), HEX_SIZE);
            this.image = null;
        }

        public void prepare() {
            // create image for buffer
            Image tempImage = createImage(bounds.width, bounds.height);
            Graphics graph = tempImage.getGraphics();

            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);

            // setup some variables
            final Point stepPos = getHexLocation(step.getPosition());
            stepPos.translate(-bounds.x, -bounds.y);
            final Polygon facingPoly = facingPolys[step.getFacing()];
            final Polygon movePoly = movementPolys[step.getFacing()];
            Point offsetCostPos;
            Polygon myPoly;
            Color col;
            // set color
            switch (step.getMovementType()) {
            case Entity.MOVE_RUN :
                col = Settings.moveRunColor;
                break;
            case Entity.MOVE_JUMP :
                col = Settings.moveJumpColor;
                break;
            case Entity.MOVE_ILLEGAL :
                col = Settings.moveIllegalColor;
                break;
            default :
                col = Settings.moveDefaultColor;
                break;
            }
            if (step.isUsingMASC()) {
                col = Settings.moveMASCColor;
            }

            // draw arrows and cost for the step
            switch (step.getType()) {
            case MovementData.STEP_FORWARDS :
            case MovementData.STEP_BACKWARDS :
            case MovementData.STEP_CHARGE :
            case MovementData.STEP_DFA :
            case MovementData.STEP_LATERAL_LEFT :
            case MovementData.STEP_LATERAL_RIGHT :
            case MovementData.STEP_LATERAL_LEFT_BACKWARDS :
            case MovementData.STEP_LATERAL_RIGHT_BACKWARDS :
                // draw arrows showing them entering the next
                myPoly = new Polygon(movePoly.xpoints, movePoly.ypoints,
                                     movePoly.npoints);
                graph.setColor(Color.darkGray);
                myPoly.translate(stepPos.x + 1, stepPos.y + 1);
                graph.drawPolygon(myPoly);
                graph.setColor(col);
                myPoly.translate(-1, -1);
                graph.drawPolygon(myPoly);
                // draw movement cost
                drawMovementCost(step, stepPos, graph, col, true);
                break;
            case MovementData.STEP_GO_PRONE:
                // draw arrow indicating dropping prone
                Polygon downPoly = movementPolys[7];
                myPoly = new Polygon(downPoly.xpoints, downPoly.ypoints, downPoly.npoints);
                graph.setColor(Color.darkGray);
                myPoly.translate(stepPos.x, stepPos.y);
                graph.drawPolygon(myPoly);
                graph.setColor(col);
                myPoly.translate(-1, -1);
                graph.drawPolygon(myPoly);
                offsetCostPos = new Point(stepPos.x + 1, stepPos.y + 15);
                drawMovementCost(step, offsetCostPos, graph, col, false);
                break;
            case MovementData.STEP_GET_UP:
                // draw arrow indicating standing up
                Polygon upPoly = movementPolys[6];
                myPoly = new Polygon(upPoly.xpoints, upPoly.ypoints, upPoly.npoints);
                graph.setColor(Color.darkGray);
                myPoly.translate(stepPos.x, stepPos.y);
                graph.drawPolygon(myPoly);
                graph.setColor(col);
                myPoly.translate(-1, -1);
                graph.drawPolygon(myPoly);
                offsetCostPos = new Point(stepPos.x, stepPos.y + 15);
                drawMovementCost(step, offsetCostPos, graph, col, false);
                break;
            case MovementData.STEP_TURN_LEFT:
            case MovementData.STEP_TURN_RIGHT:
                // draw arrows showing the facing
                myPoly = new Polygon(facingPoly.xpoints, facingPoly.ypoints,
                                     facingPoly.npoints);
                graph.setColor(Color.darkGray);
                myPoly.translate(stepPos.x + 1, stepPos.y + 1);
                graph.drawPolygon(myPoly);
                graph.setColor(col);
                myPoly.translate(-1, -1);
                graph.drawPolygon(myPoly);
                break;
            case MovementData.STEP_LOAD:
                // Announce load.
                String load = "Load";
                if (step.isPastDanger()) {
                    load = "(" + load + ")";
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12));
                int loadX = stepPos.x + 42 - (graph.getFontMetrics(graph.getFont()).stringWidth(load) / 2);
                graph.setColor(Color.darkGray);
                graph.drawString(load, loadX, stepPos.y + 39);
                graph.setColor(col);
                graph.drawString(load, loadX - 1, stepPos.y + 38);
                break;
            case MovementData.STEP_UNLOAD:
                // Announce unload.
                String unload = "Unload";
                if (step.isPastDanger()) {
                    unload = "(" + unload + ")";
                }
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12));
                int unloadX = stepPos.x + 42 - (graph.getFontMetrics(graph.getFont()).stringWidth(unload) / 2);
                int unloadY = stepPos.y + 38 + graph.getFontMetrics(graph.getFont()).getHeight();
                graph.setColor(Color.darkGray);
                graph.drawString(unload, unloadX, unloadY + 1);
                graph.setColor(col);
                graph.drawString(unload, unloadX - 1, unloadY);
                break;

            default :
                break;
            }

            // create final image
            this.image = createImage(new FilteredImageSource(tempImage.getSource(),
                    new KeyAlphaFilter(TRANSPARENT)));
        }

        public MovementData.Step getStep() {
            return step;
        }

        private void drawMovementCost(MovementData.Step step, Point stepPos, Graphics graph, Color col, boolean shiftFlag) {
            String costString = null;
            StringBuffer costStringBuf = new StringBuffer();
            costStringBuf.append( step.getMpUsed() );

            // If the step is using a road bonus, mark it.
            if ( step.isOnPavement() ) {
                costStringBuf.append( "+" );
            }

            // If the step is dangerous, mark it.
            if ( step.isDanger() ) {
                costStringBuf.append( "*" );
            }

            // If the step is past danger, mark that.
            if (step.isPastDanger()) {
                costStringBuf.insert( 0, "(" );
                costStringBuf.append( ")" );
            }

            if (step.isUsingMASC()) {
                costStringBuf.append("[");
                costStringBuf.append(step.getMASCNumber());
                costStringBuf.append("+]");
            }

            // Convert the buffer to a String and draw it.
            costString = costStringBuf.toString();
            graph.setFont(new Font("SansSerif", Font.PLAIN, 12));
            int costX = stepPos.x + 42;
            if (shiftFlag) {
                costX -= (graph.getFontMetrics(graph.getFont()).stringWidth(costString) / 2);
            }
            graph.setColor(Color.darkGray);
            graph.drawString(costString, costX, stepPos.y + 39);
            graph.setColor(col);
            graph.drawString(costString, costX - 1, stepPos.y + 38);
        }

    }

    /**
     * Sprite and info for a C3 network.  Does not actually use the image buffer
     * as this can be horribly inefficient for long diagonal lines.
     */
    private class C3Sprite extends Sprite
    {
        private Polygon C3Poly;

        protected int entityId;
        protected int masterId;
        
        Color spriteColor;

        public C3Sprite(Entity e, Entity m) {
            this.entityId = e.getId();
            this.masterId = m.getId();
            this.spriteColor = e.getOwner().getColor();

            if(e.getPosition() == null || m.getPosition() == null) {
                C3Poly = new Polygon();
                C3Poly.addPoint(0, 0);
                C3Poly.addPoint(1,0);
                C3Poly.addPoint(0,1);
                this.bounds = new Rectangle(C3Poly.getBounds());
                bounds.setSize(bounds.getSize().width + 1, bounds.getSize().height + 1);
                this.image = null;
                return;
            }
            final Point a = getHexLocation(e.getPosition());
            final Point t = getHexLocation(m.getPosition());

            final double an = (e.getPosition().radian(m.getPosition()) + (Math.PI * 1.5)) % (Math.PI * 2); // angle
            final double lw = 1; // line width

            // make a polygon
            C3Poly = new Polygon();
            C3Poly.addPoint(a.x + 42 - (int)Math.round(Math.sin(an) * lw), a.y + 36 + (int)Math.round(Math.cos(an) * lw));
            C3Poly.addPoint(a.x + 42 + (int)Math.round(Math.sin(an) * lw), a.y + 36 - (int)Math.round(Math.cos(an) * lw));
            C3Poly.addPoint(t.x + 42 + (int)Math.round(Math.sin(an) * lw), t.y + 36 - (int)Math.round(Math.cos(an) * lw));
            C3Poly.addPoint(t.x + 42 - (int)Math.round(Math.sin(an) * lw), t.y + 36 + (int)Math.round(Math.cos(an) * lw));

            // set bounds
            this.bounds = new Rectangle(C3Poly.getBounds());
            bounds.setSize(bounds.getSize().width + 1, bounds.getSize().height + 1);

            // move poly to upper right of image
            C3Poly.translate(-bounds.getLocation().x, -bounds.getLocation().y);

            // set names & stuff

            // nullify image
            this.image = null;
        }

        public void prepare() {
            ;
        }

        public boolean isReady() {
            return true;
        }

        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
            Polygon drawPoly = new Polygon(C3Poly.xpoints, C3Poly.ypoints, C3Poly.npoints);
            drawPoly.translate(x, y);

            g.setColor(spriteColor);
            g.fillPolygon(drawPoly);
            g.setColor(Color.black);
            g.drawPolygon(drawPoly);
        }

        /**
         * Return true if the point is inside our polygon
         */
        public boolean isInside(Point point) {
            return C3Poly.contains(point.x + view.x - bounds.x - offset.x,
                                          point.y + view.y - bounds.y - offset.y);
        }

    }

    /**
     * Sprite and info for an attack.  Does not actually use the image buffer
     * as this can be horribly inefficient for long diagonal lines.
     *
     * Appears as an arrow. Arrow becoming cut in half when two Meks attacking
     * each other.
     */
    private class AttackSprite extends Sprite
    {
        private java.util.Vector attacks = new java.util.Vector();
        private Point a;
        private Point t;
        private double an;
        private StraightArrowPolygon attackPoly;
        private Color attackColor;
        private int entityId;
        private int targetType;
        private int targetId;
        private String attackerDesc;
        private String targetDesc;
        private Vector weaponDescs = new Vector();

        public AttackSprite(AttackAction attack) {

            this.attacks.addElement(attack);
            this.entityId = attack.getEntityId();
            this.targetType = attack.getTargetType();
            this.targetId = attack.getTargetId();
            final Entity ae = game.getEntity(attack.getEntityId());
            final Targetable target = game.getTarget(targetType, targetId);
            this.a = getHexLocation(ae.getPosition());
            this.t = getHexLocation(target.getPosition());
            // color?
            attackColor = ae.getOwner().getColor();
            //angle of line connecting two hexes
            this.an = (ae.getPosition().radian(target.getPosition()) + (Math.PI * 1.5)) % (Math.PI * 2); // angle
            // make a polygon

            // OK, that is actually not good. I do not like hard coded figures.
            // 42 - x distance in pixels from origin of hex bounding box to the center of hex.
            // 36 - y distance in pixels from origin of hex bounding box to the center of hex.
            // 18 - is actually 36/2 - we do not want arrows to start and end directly
            // in the centes of hex and hiding mek under.

            a.x = a.x + 42 + (int)Math.round(Math.cos(an) * 18);
            t.x = t.x + 42 - (int)Math.round(Math.cos(an) * 18);
            a.y = a.y + 36 + (int)Math.round(Math.sin(an) * 18);
            t.y = t.y + 36 - (int)Math.round(Math.sin(an) * 18);

            // Checking if given attack is mutual. In this case we building halved arrow
            if (isMutualAttack()){
                attackPoly = new StraightArrowPolygon(a, t, 8, 12, true);
            } else {
                attackPoly = new StraightArrowPolygon(a, t, 4, 8, false);
            }

            // set bounds
            this.bounds = new Rectangle(attackPoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1, bounds.getSize().height + 1);
            // move poly to upper right of image
            attackPoly.translate(-bounds.getLocation().x, -bounds.getLocation().y);


            // set names & stuff
            attackerDesc = ae.getDisplayName();
            targetDesc = target.getDisplayName();
            if (attack instanceof WeaponAttackAction) {
                addWeapon((WeaponAttackAction)attack);
            }
            if (attack instanceof KickAttackAction) {
                addWeapon((KickAttackAction)attack);
            }
            if (attack instanceof PunchAttackAction) {
                addWeapon((PunchAttackAction)attack);
            }
            if (attack instanceof PushAttackAction) {
                addWeapon((PushAttackAction)attack);
            }
            if (attack instanceof ClubAttackAction) {
                addWeapon((ClubAttackAction)attack);
            }
            if (attack instanceof ChargeAttackAction) {
                addWeapon((ChargeAttackAction)attack);
            }
            if (attack instanceof DfaAttackAction) {
                addWeapon((DfaAttackAction)attack);
            }

            // nullify image
            this.image = null;
        }


        /** If we have build full arrow already with single attack and have got
         * counter attack from our target lately - lets change arrow to halved.
         */
        public void rebuildToHalvedPolygon(){
           attackPoly = new StraightArrowPolygon(a, t, 8, 12, true);
           // set bounds
           this.bounds = new Rectangle(attackPoly.getBounds());
           bounds.setSize(bounds.getSize().width + 1, bounds.getSize().height + 1);
           // move poly to upper right of image
           attackPoly.translate(-bounds.getLocation().x, -bounds.getLocation().y);
        }
        /** Cheking if attack is mutual and changing target arrow to half-arrow
         */
        private boolean isMutualAttack(){
            for (final Iterator i = attackSprites.iterator(); i.hasNext();) {
                 final AttackSprite sprite = (AttackSprite)i.next();
                 if (sprite.getEntityId() == this.targetId && sprite.getTargetId() == this.entityId) {
                     sprite.rebuildToHalvedPolygon();
                     return true;
                 }
            }
            return false;
        }

        public void prepare() {
            ;
        }

        public boolean isReady() {
            return true;
        }

        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
            Polygon drawPoly = new Polygon(attackPoly.xpoints, attackPoly.ypoints, attackPoly.npoints);
            drawPoly.translate(x, y);

            g.setColor(attackColor);
            g.fillPolygon(drawPoly);
            g.setColor(Color.gray.darker());
            g.drawPolygon(drawPoly);
        }

        /**
         * Return true if the point is inside our polygon
         */
        public boolean isInside(Point point) {
            return attackPoly.contains(point.x + view.x - bounds.x - offset.x,
                                          point.y + view.y - bounds.y - offset.y);
        }

        public int getEntityId() {
            return entityId;
        }

        public int getTargetId() {
            return targetId;
        }

        /**
         * Adds a weapon to this attack
         */
        public void addWeapon(WeaponAttackAction attack) {
            final Entity entity = game.getEntity(attack.getEntityId());
            final WeaponType wtype = (WeaponType)entity.getEquipment(attack.getWeaponId()).getType();
            final String roll = Compute.toHitWeapon(game, attack).getValueAsString();
            weaponDescs.addElement( wtype.getName() + "; needs " + roll );
        }

        public void addWeapon(KickAttackAction attack) {
            String bufer = "";
            String rollLeft = "";
            String rollRight = "";
            final int leg = attack.getLeg();
            switch (leg){
                case KickAttackAction.BOTH:
                    rollLeft = Compute.toHitKick( game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), KickAttackAction.LEFT).getValueAsString();
                    rollRight = Compute.toHitKick( game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), KickAttackAction.RIGHT).getValueAsString();
                    bufer = "Kicks with both legs. Left needs " + rollLeft + "; Right needs " + rollRight;
                    break;
                case KickAttackAction.LEFT:
                    rollLeft = Compute.toHitKick( game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), KickAttackAction.LEFT).getValueAsString();
                    bufer = "Kicks with left leg. Needs " + rollLeft;
                    break;
                case KickAttackAction.RIGHT:
                    rollRight = Compute.toHitKick( game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), KickAttackAction.RIGHT).getValueAsString();
                    bufer = "Kicks with right leg. Needs " + rollRight;
                    break;
            }
            weaponDescs.addElement(bufer);
        }

        public void addWeapon(PunchAttackAction attack) {
            String bufer = "";
            String rollLeft = "";
            String rollRight = "";
            final int arm = attack.getArm();
            switch (arm){
                case PunchAttackAction.BOTH:
                    rollLeft = Compute.toHitPunch( game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), PunchAttackAction.LEFT).getValueAsString();
                    rollRight = Compute.toHitPunch( game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), PunchAttackAction.RIGHT).getValueAsString();
                    bufer = "Punches with both arms. Left needs " + rollLeft + "; Right needs " + rollRight;
                    break;
                case PunchAttackAction.LEFT:
                    rollLeft = Compute.toHitPunch( game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), PunchAttackAction.LEFT).getValueAsString();
                    bufer = "Punches with left arm. Needs " + rollLeft;
                    break;
                case PunchAttackAction.RIGHT:
                    rollRight = Compute.toHitPunch( game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), PunchAttackAction.RIGHT).getValueAsString();
                    bufer = "Punches with right arm. Needs " + rollRight;
                    break;
            }
            weaponDescs.addElement(bufer);
        }

        public void addWeapon(PushAttackAction attack) {
              final String roll = Compute.toHitPush(game, attack).getValueAsString();
              weaponDescs.addElement("Pushes. Needs " + roll);
        }
        
        public void addWeapon(ClubAttackAction attack) {
              final String roll = Compute.toHitClub(game, attack).getValueAsString();
              weaponDescs.addElement("Hits with club. Needs " + roll);
        }
        
        public void addWeapon(ChargeAttackAction attack) {
              final String roll = Compute.toHitCharge(game, attack).getValueAsString();
              weaponDescs.addElement("Charges. Needs " + roll);
        }
        public void addWeapon(DfaAttackAction attack) {
              final String roll = Compute.toHitDfa(game, attack).getValueAsString();
              weaponDescs.addElement("DFA. Needs " + roll);
        }
                

        private String[] getTooltip() {
            String[] tipStrings = new String[1 + weaponDescs.size()];
            int tip = 1;
            tipStrings[0] = attackerDesc + " on " + targetDesc;
            for (Iterator i = weaponDescs.iterator(); i.hasNext();) {
                tipStrings[tip++] = (String)i.next();
            }
            return tipStrings;
        }
    }

    /**
     * Determine if the tile manager's images have been loaded.
     *
     * @return  <code>true</code> if all images have been loaded.
     *          <code>false</code> if more need to be loaded.
     */
    public boolean isTileImagesLoaded() {
        return this.tileManager.isLoaded();
    }

}
