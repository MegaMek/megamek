/**
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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.image.*;

import megamek.common.*;
import megamek.common.actions.*;

/**
 * Displays the board; lets the user scroll around and select points on it.
 */
public class BoardView1
    extends Canvas
    implements BoardListener, MouseListener, MouseMotionListener, Runnable
{
    private static final int    PIC_MAX				= 10;
    private static final int    PIC_MECH_LIGHT		= 1;
    private static final int    PIC_MECH_MEDIUM		= 2;
    private static final int    PIC_MECH_HEAVY		= 3;
    private static final int    PIC_MECH_ASSAULT	= 4;
    
    private static final int    TRANSPARENT = 0xFFFF000F;
    private Game game;
    private Frame frame;
    
    private Point mousePos = new Point();
    
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

    private Dimension boardSize;
    
    // entity buffers
    private Vector entitySprites = new Vector();
    private Hashtable entitySpriteIds = new Hashtable();
    
    // images for the three selection cursors
    private CursorSprite cursorSprite;
    private CursorSprite highlightSprite;
    private CursorSprite selectedSprite;
    
    // buffer for current movement
    private PathSprite movementSprite;
    
    // vector of buffers for all firing lines
    private Vector attackSprites = new Vector();
    
    // tooltip stuff
    private Window tipWindow;
    private boolean isTipPossible = false;
    private long lastIdle;
    
    
    // make sure those .gifs get decoded!
    private MediaTracker        tracker = new MediaTracker(this);
    private boolean             imagesLoading = false;
    private boolean             imagesLoaded = false;

    // player-tinted mech image cache
    private Image[][]			imageCache = new Image[PIC_MAX][6]; // [type][facing]
    private Image[][][]			tintCache = new Image[PIC_MAX][6][Player.colorRGBs.length]; // [type][facing][color]
    
    // polygons for a few things
    private Polygon				hexPoly;
    private Polygon[]			facingPolys;
    private Polygon[]			movementPolys;
    
    
    /**
     * Construct a new board view for the specified game
     */
    public BoardView1(Game game, Frame frame) {
        this.game = game;
        this.frame = frame;
        
        game.board.addBoardListener(this);
		scroller.start();
		addMouseListener(this);
		addMouseMotionListener(this);
        
        // tooltip
        tipWindow = new Window(frame);
        
        initPolys();
        
        cursorSprite = new CursorSprite(Color.pink);
        highlightSprite = new CursorSprite(Color.white);
        selectedSprite = new CursorSprite(Color.magenta);
    }
 
    
	/**
	 * loadAllImages
	 */
	public void loadAllImages() {
		imagesLoaded = false;
		
		for (int i = 0; i < 6; i++) {
			imageCache[PIC_MECH_LIGHT][i] = getToolkit().getImage("data/mex/light-" + i + ".gif");
			imageCache[PIC_MECH_MEDIUM][i] = getToolkit().getImage("data/mex/medium-" + i + ".gif");
			imageCache[PIC_MECH_HEAVY][i] = getToolkit().getImage("data/mex/heavy-" + i + ".gif");
			imageCache[PIC_MECH_ASSAULT][i] = getToolkit().getImage("data/mex/assault-" + i + ".gif");
			
			tracker.addImage(imageCache[PIC_MECH_LIGHT][i], 1);
			tracker.addImage(imageCache[PIC_MECH_MEDIUM][i], 1);
			tracker.addImage(imageCache[PIC_MECH_HEAVY][i], 1);
			tracker.addImage(imageCache[PIC_MECH_ASSAULT][i], 1);
		}
    
        for (int i = 0; i < game.board.terrains.length; i++) {
            tracker.addImage( game.board.terrains[i].getImage(this), 2);
        }
        
		imagesLoading = true;
	}

    public void paint(Graphics g) {
		update(g);
    }

    public void update(Graphics g) {
        final long start = System.currentTimeMillis();
        
        final Dimension size = getSize();
        final Rectangle view = new Rectangle(scroll, size);
        
		if (!imagesLoading) {
			g.drawString("loading images...", 20, 50);
			loadAllImages();
			return;
		} else if (!imagesLoaded) {
			imagesLoaded = tracker.checkAll(true);
			return;
		}

        // make sure back buffer is valid
        if (backGraph == null || !size.equals(backSize)) {
            backSize = size;
            backImage = createImage(size.width, size.height);
            backGraph = backImage.getGraphics();
        }
        
        // make sure board rectangle contains our current view rectangle
        if (boardImage == null || !boardRect.union(view).equals(boardRect)) {
            updateBoardImage(view);
        }
        
        // draw the board onto the back buffer
        backGraph.drawImage(boardImage, 0, 0, this);
        
        // draw onscreen entities
        drawSprites(entitySprites, view);
        
        // draw onscreen attacks
        drawSprites(attackSprites, view);
        
        // draw cursors
        drawSprite(cursorSprite, view);
        drawSprite(highlightSprite, view);
        drawSprite(selectedSprite, view);
        
        // draw the back buffer onto the screen
        g.drawImage(backImage, 0, 0, this);
        
        final long finish = System.currentTimeMillis();
        System.out.println("BoardView1: updated screen in " + (finish - start) + " ms.");
        //view = " + view);
    }
    
    /**
     * Looks through a vector of buffered images and draws them if they're
     * onscreen.
     */
    private void drawSprites(Vector spriteVector, Rectangle view) {
        for (final Enumeration i = spriteVector.elements(); i.hasMoreElements();) {
            final Sprite sprite = (Sprite)i.nextElement();
            drawSprite(sprite, view);
        }
    }
    
    /**
     * Draws a sprite, if it is in the current view
     */
    private void drawSprite(Sprite sprite, Rectangle view) {
        if (view.intersects(sprite.getBounds())) {
            final int drawX = sprite.getBounds().x - view.x;
            final int drawY = sprite.getBounds().y - view.y;
            if (sprite.getImage() == null) {
                sprite.draw(this);
            }
            backGraph.drawImage(sprite.getImage(), drawX, drawY, this);
        }
    }
    
    /**
     * Updates the board buffer to contain all the hexes needed by the view.
     */
    private void updateBoardImage(Rectangle view) {
        // check to make sure image is big enough
        if (boardGraph == null) {
            boardImage = createImage(view.width, view.height);
            boardGraph = boardImage.getGraphics();
            
            System.out.println("boardview1: made a new board buffer " + boardRect);
        }
        boardRect = new Rectangle(view);
        redrawBoard();
    }
    
    /**
     * Redraws the whole board, based on boardRect
     */
    private void redrawBoard() {
        int drawX = boardRect.x / 63;
        int drawY = boardRect.y / 72;
        
        int drawWidth = boardRect.width / 63 + 1;
        int drawHeight = boardRect.height / 72 + 1;
        
        // draw some hexes
        for (int i = 0; i < drawHeight; i++) {
            for (int j = 0; j < drawWidth; j++) {
                drawHex(new Coords(j + drawX, i + drawY));
            }
        }
        
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
        
        // offset drawing point
        int drawX = hexLoc.x - boardRect.x;
        int drawY = hexLoc.y - boardRect.y;
        
		// draw picture
		boardGraph.drawImage(hex.getImage(this), drawX, drawY, this);
		// draw text stuff
		boardGraph.setColor(Color.black);
		boardGraph.setFont(new Font("SansSerif", Font.PLAIN, 10));
		boardGraph.drawString(c.getBoardNum(), drawX + 30, drawY + 12);
		if (hex.getElevation() != 0) {
			boardGraph.setFont(new Font("SansSerif", Font.PLAIN, 9));
			boardGraph.drawString("LEVEL " + hex.getElevation(), drawX + 24, drawY + 70);
		}
		// draw elevation borders
		Hex tHex;
		if ((tHex = game.board.getHex(c.translated(0))) != null && tHex.getElevation() != hex.getElevation()) {
			boardGraph.drawLine(drawX + 21, drawY, drawX + 62, drawY);
		}
		if ((tHex = game.board.getHex(c.translated(1))) != null && tHex.getElevation() != hex.getElevation()) {
			boardGraph.drawLine(drawX + 62, drawY, drawX + 83, drawY + 35);
		}
		if ((tHex = game.board.getHex(c.translated(2))) != null && tHex.getElevation() != hex.getElevation()) {
			boardGraph.drawLine(drawX + 83, drawY + 36, drawX + 62, drawY + 71);
		}
		if ((tHex = game.board.getHex(c.translated(3))) != null && tHex.getElevation() != hex.getElevation()) {
			boardGraph.drawLine(drawX + 62, drawY + 71, drawX + 21, drawY + 71);
		}
		if ((tHex = game.board.getHex(c.translated(4))) != null && tHex.getElevation() != hex.getElevation()) {
			boardGraph.drawLine(drawX + 21, drawY + 71, drawX, drawY + 36);
		}
		if ((tHex = game.board.getHex(c.translated(5))) != null && tHex.getElevation() != hex.getElevation()) {
			boardGraph.drawLine(drawX, drawY + 35, drawX + 21, drawY);
		}
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
    private Coords getCoordsAt(Point p) {
		final int x = (p.x + scroll.x) / 63;
		final int y = ((p.y + scroll.y) - ((x & 1) == 1 ? 36 : 0)) / 72;
		return new Coords(x, y);
    }
    
    
    
    
    
    /**
     * Shows the tooltip thinger
     */
    private void showTooltip() {
        // set tip location
        final Point tipLoc = new Point(getLocationOnScreen());
        tipLoc.translate(mousePos.x, mousePos.y + 20);
        tipWindow.setLocation(tipLoc);
        
        // update tip text
        tipWindow.setBackground(SystemColor.info);
        tipWindow.removeAll();
        tipWindow.add(new TooltipCanvas(getTipText(mousePos)));
        tipWindow.pack();
        
        tipWindow.show();
    }
    
    /**
     * The text to be displayed when the mouse is at a certain point
     */
    private String getTipText(Point point) {
        // check if it's on an attack
        for (final Enumeration i = attackSprites.elements(); i.hasMoreElements();) {
            final Sprite sprite = (Sprite)i.nextElement();
            if (sprite.isInside(point)) {
                return sprite.getTooltip();
            }
        }
        
        // check if it's on an entity
        for (final Enumeration i = entitySprites.elements(); i.hasMoreElements();) {
            final Sprite sprite = (Sprite)i.nextElement();
            if (sprite.isInside(point)) {
                return sprite.getTooltip();
            }
        }
        
        // then return a tip for the hex it's on
        Coords mcoords = getCoordsAt(point);
        Hex mhex = game.board.getHex(mcoords);
        String tipString = "Hex " + mcoords.getBoardNum() 
                          + "; level " + mhex.getElevation()
                          + "; " + Terrain.TERRAIN_NAMES[mhex.getTerrainType()];
        return tipString;
    }
    
    /**
     * Hides the tooltip thinger
     */
    private void hideTooltip() {
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
        } else if (isTipPossible && System.currentTimeMillis() - lastIdle > 1000) {
            showTooltip();
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    public void redrawEntity(Entity entity) {
        EntitySprite buff = (EntitySprite)entitySpriteIds.get(new Integer(entity.getId()));
        
        if (buff != null) {
            entitySprites.removeElement(buff);
        }
        
        buff = new EntitySprite(entity);
        entitySprites.addElement(buff);
        entitySpriteIds.put(new Integer(entity.getId()), buff);
        repaint();
    }

    private void redrawAllEntities() {
        entitySprites.removeAllElements();
        entitySpriteIds.clear();
        
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            redrawEntity(entity);
        }
        repaint();
    }
    
    
    public void centerOnHex(Coords c) {
        ;
    }
    
    public void drawMovementData(Entity entity, MovementData md) {
        ;
    }
    public void clearMovementData() {
        ;
    }
    
    public void addAttack(AttackAction aa) {
        // one already exists, from attacker to target, use that
        for (final Enumeration i = attackSprites.elements(); i.hasMoreElements();) {
            final AttackSprite buff = (AttackSprite)i.nextElement();
            
            if (buff.getAttack().getEntityId() == aa.getEntityId() 
                && buff.getAttack().getTargetId() == aa.getTargetId()) {
                return;
            }
        }
        
        // okay, add a new one
        attackSprites.addElement(new AttackSprite(aa));
    }
    public void clearAllAttacks() {
        attackSprites.removeAllElements();
    }
    
    
    
	/**
	 * Returns the image type index for the specified entity
	 */
	public int getEntityImageIndex(Entity en) {
		if (en instanceof Mech && en.getWeight() <= Mech.WEIGHT_LIGHT) {
			return PIC_MECH_LIGHT;
		} else if (en instanceof Mech && en.getWeight() <= Mech.WEIGHT_MEDIUM) {
			return PIC_MECH_MEDIUM;
		} else if (en instanceof Mech && en.getWeight() <= Mech.WEIGHT_HEAVY) {
			return PIC_MECH_HEAVY;
		} else if (en instanceof Mech && en.getWeight() <= Mech.WEIGHT_ASSAULT) {
			return PIC_MECH_ASSAULT;
        } else {
    		return -1;
        }
	}
	
	/**
	 * Returns the image from the cache
	 */
	public Image getEntityImage(Entity en) {
		final int type = getEntityImageIndex(en);
		final int facing = en.getSecondaryFacing();
		final int cindex = en.getOwner().getColorIndex();
		
		// check cache in image
		if (tintCache[type][facing][cindex] == null) {
			tintCache[type][facing][cindex] = tint(imageCache[type][facing], 
                                                   en.getOwner().getColorRGB());
		}
		
		return tintCache[type][facing][cindex];
	}
	
	/**
	 * Tints an image to a certain color
	 */
	public Image tint(Image img, int color) {
		int[] pixels = new int[84 * 72];
		
		int cred   = (color >> 16) & 0xff;
		int cgreen = (color >>  8) & 0xff;
		int cblue  = (color      ) & 0xff;
		
		PixelGrabber pg = new PixelGrabber(img, 0, 0, 84, 72, pixels, 0, 84);
		try {
		    pg.grabPixels();
		} catch (InterruptedException e) {
			System.err.println("graphics: interrupted waiting for pixels!");
		    return null;
		}
		if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
		    System.err.println("graphics: image fetch aborted or errored");
		    return null;
		}
		for (int i = 0; i < pixels.length; i++) {
			int alpha = (pixels[i] >> 24) & 0xff;
			int black = (pixels[i]) & 0xff;  // assume black & white
			if (alpha != 0xff) {
                continue;
            }
			// alter pixel to tint
			int red   = (cred   * black) / 255;
			int green = (cgreen * black) / 255;
			int blue  = (cblue  * black) / 255;
					
			pixels[i] = (alpha << 24) + (red << 16) + (green << 8) + blue;
		}
		return createImage(new MemoryImageSource(84, 72, pixels, 0, 84));
	}
    
    /**
     * Returns a new image, where any pixels of the specified color are
     * transparent.  (Stupid AWT)
     */
    private Image makeTransparent(Image image, int color, int width, int height) {
        final int[] pixels = makeTransparentPixels(image, color, width, height);
        return createImage(new MemoryImageSource(width, height, pixels, 0, width));
    }
    
    /**
     * Returns a new image, where any pixels of the specified color are
     * transparent.  (Stupid AWT)
     */
    private int[] makeTransparentPixels(Image image, int color, int width, int height) {
        int[] pixels = new int[width * height];
        
		PixelGrabber pg = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);
		try {
		    pg.grabPixels();
		} catch (InterruptedException e) {
			System.err.println("graphics: interrupted waiting for pixels!");
		    return null;
		}
		if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
		    System.err.println("graphics: image fetch aborted or errored");
		    return null;
		}
        
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == color) {
                pixels[i] &= 0x00ffffff;
            }
        }
        
        return pixels;
    }

    
    
	public void boardHexMoused(BoardEvent b) {
		;
	}
	public void boardHexCursor(BoardEvent b) {
        if (b.getCoords() != null) {
		    cursorSprite.setLocation(getHexLocation(b.getCoords()));
        } else {
		    cursorSprite.setLocation(-100, -100);
        }
        repaint();
	}
	public void boardHexSelected(BoardEvent b) {
        System.out.println("hex selected = " + b.getCoords());
        if (b.getCoords() != null) {
		    selectedSprite.setLocation(getHexLocation(b.getCoords()));
        } else {
		    selectedSprite.setLocation(-100, -100);
        }
        repaint();
	}
	public void boardHexHighlighted(BoardEvent b) {
        if (b.getCoords() != null) {
		    highlightSprite.setLocation(getHexLocation(b.getCoords()));
        } else {
		    highlightSprite.setLocation(-100, -100);
        }
        repaint();
	}
	public void boardChangedHex(BoardEvent b) {
		;
	}
	public void boardNewVis(BoardEvent b) {
		;
	}
	public void boardNewBoard(BoardEvent b) {
		boardSize = new Dimension(game.board.width * 63 + 21, 
                                  game.board.height * 72 + 36);
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
		} else if (scroll.x > (boardSize.width - backSize.width)) {
			scroll.x = (boardSize.width - backSize.width);
		}

		if (scroll.y < 0) {
			scroll.y = 0;
		} else if (scroll.y > (boardSize.height - backSize.height)) {
			scroll.y = (boardSize.height - backSize.height);
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
		movementPolys = new Polygon[6];
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
	}
    
    
	//
	// Runnable
	//
	public void run() {
		final Thread currentThread = Thread.currentThread();
		while (scroller == currentThread) {
			try {
				Thread.sleep(10);
			} catch(InterruptedException ex) {
				// duh?
			}
            if (!isShowing()) {
                continue;
            }
			if (backGraph != null) {
				doScroll();
                checkTooltip();
			} else {
				repaint();
			}
		}
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
		//board.drag(hexAt(me.getPoint()));
	}
	public void mouseReleased(MouseEvent me) {
        isScrolling = false;
        isTipPossible = true;
		//System.out.println("moure click count: " + me.getClickCount());
		if (me.getClickCount() == 1) {
			game.board.mouseAction(getCoordsAt(me.getPoint()), Board.BOARD_HEX_CLICK, me.getModifiers()); 
			//board.select(hexAt(me.getPoint()));
		} else {
			game.board.mouseAction(getCoordsAt(me.getPoint()), Board.BOARD_HEX_DOUBLECLICK, me.getModifiers()); 
			//board.select(hexAt(me.getPoint()));
		}
		//mousePos.setLocation(getSize().width / 2, getSize().height / 2);
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
		} else {
			repaint();
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
        private String string;
        private Dimension size;
        
        public TooltipCanvas(String string) {
            this.string = string;
            
            // setup
            setFont(new Font("SansSerif", Font.PLAIN, 12));
		    setBackground(SystemColor.info);
		    setForeground(SystemColor.infoText);
            
            // determine size
            FontMetrics fm = getFontMetrics(getFont());
		    size = new Dimension(fm.stringWidth(string) + 5, fm.getAscent() + 4);
            setSize(size);
        }
        
        public void paint(Graphics g) {
		    g.setColor(getBackground());
            g.fillRect(0, 0, size.width, size.height);
            g.setColor(getForeground());
            g.drawRect(0, 0, size.width - 1, size.height - 1);
            g.drawString(string, 2, size.height - 3);
        }
    }
    
    
    /**
     * Stores a buffered image to draw
     */
    private abstract class Sprite
    {
        private Rectangle bounds;
        private Image image;
        
        public abstract void draw(ImageObserver observer);
        
        public Rectangle getBounds() {
            return bounds;
        }
        
        public Image getImage() {
            return image;
        }
        
        public boolean isInside(Point point) {
            return bounds.contains(point);
        }
        
        private String getTooltip() {
            return null;
        }
    }
    
    /**
     * Sprite for a cursor
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
        
        public void draw(ImageObserver observer) {
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
            this.image = makeTransparent(tempImage, TRANSPARENT, 
                                         bounds.width, bounds.height);
        }
        
        public void setLocation(int x, int y) {
            bounds.setLocation(x, y);
        }
        public void setLocation(Point point) {
            bounds.setLocation(point);
        }
    }
    
    /**
     * Sprite for an entity
     */
    private class EntitySprite extends Sprite
    {
        private Entity entity;
        private int[] pixels;
        
        public EntitySprite(Entity entity) {
            this.entity = entity;

		    String model = entity.getModel();
		    Font font = new Font("SansSerif", Font.PLAIN, 10);
		    Rectangle modelRect = new Rectangle(47, 55,  
                                 getFontMetrics(font).stringWidth(model) + 1, 
                                 getFontMetrics(font).getAscent());
            Rectangle tempBounds = new Rectangle(0, 0, 84, 72).union(modelRect);
            tempBounds.setLocation(getHexLocation(entity.getPosition()));
            
            this.bounds = tempBounds;
            this.image = null;
        }
        
        /**
         * Creates the sprite for this entity.  It is an extra pain to 
         * create transparent images in AWT.
         */
        public void draw(ImageObserver observer) {
            // figure out size
		    String model = entity.getModel();
		    Font font = new Font("SansSerif", Font.PLAIN, 10);
		    Rectangle modelRect = new Rectangle(47, 55,  
                                 getFontMetrics(font).stringWidth(model) + 1, 
                                 getFontMetrics(font).getAscent());
            Rectangle wholeRect = new Rectangle(0, 0, 84, 72).union(modelRect);
            
            // create image for buffer
            Image tempImage = createImage(bounds.width, bounds.height);
            Graphics graph = tempImage.getGraphics();
            
            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);
            
            // draw entity image
            graph.drawImage(getEntityImage(entity), 0, 0, observer);
            
            // draw box with model
            Color col;
            Color bcol;
		    if (entity.isImmobile()) {
		    	col = Color.black;
		    	bcol = Color.lightGray;
		    } else if (!entity.ready) {
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
		    graph.setColor(entity.ready ? Color.black : Color.lightGray);
		    graph.drawString(model, modelRect.x + 1, modelRect.y + modelRect.height - 1);
            
		    // draw facing
		    graph.setColor(Color.white);
		    if (entity.getFacing() != -1) {
                graph.drawPolygon(facingPolys[entity.getFacing()]);
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
            
            // create final image
            this.pixels = makeTransparentPixels(tempImage, TRANSPARENT, bounds.width, bounds.height);
            this.image = createImage(new MemoryImageSource(bounds.width, bounds.height, pixels, 0, bounds.width));
        }
        
        /**
         * Return true if the point is on a non-transparent pixel
         */
        public boolean isInside(Point point) {
            final int index = point.x - bounds.x + (bounds.width * (point.y - bounds.y));
            return super.isInside(point) && (pixels[index] & 0xFF000000) != 0;
        }
        
        public String getTooltip() {
            return entity.getDisplayName();
        }
    }
    
    /**
     * Sprite for a movement path
     */
    private class PathSprite extends Sprite
    {
        public void draw(ImageObserver observer) {
            final int TRANSPARENT = 0xFFFF00FF;
        }
    }
    
    /**
     * Sprite and info for an attack
     */
    private class AttackSprite extends Sprite
    {
        private AttackAction attack;
        private Polygon attackPoly;
        
        public AttackSprite(AttackAction attack) {
            this.attack = attack;
            
            final Entity ae = game.getEntity(attack.getEntityId());
            final Entity te = game.getEntity(attack.getTargetId());
            final Point a = getHexLocation(ae.getPosition());
		    final Point t = getHexLocation(te.getPosition());
		
		    final double an = (ae.getPosition().radian(te.getPosition()) + (Math.PI * 1.5)) % (Math.PI * 2); // angle
		    final double lw = 3; // line width
		
            // make a polygon
		    attackPoly = new Polygon();
		    attackPoly.addPoint(a.x + 42 - (int)Math.round(Math.sin(an) * lw), a.y + 36 + (int)Math.round(Math.cos(an) * lw));
		    attackPoly.addPoint(a.x + 42 + (int)Math.round(Math.sin(an) * lw), a.y + 36 - (int)Math.round(Math.cos(an) * lw));
		    attackPoly.addPoint(t.x + 42 + (int)Math.round(Math.sin(an) * lw), t.y + 36 - (int)Math.round(Math.cos(an) * lw));
		    attackPoly.addPoint(t.x + 42 - (int)Math.round(Math.sin(an) * lw), t.y + 36 + (int)Math.round(Math.cos(an) * lw));
            
            // set bounds
            this.bounds = new Rectangle(attackPoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1, bounds.getSize().height + 1);
            
            // move poly to upper right of image
            attackPoly.translate(-bounds.getLocation().x, -bounds.getLocation().y);
            
            // nullify image
            this.image = null;
        }
        
        public void draw(ImageObserver observer) {
            // create image for buffer
            Image tempImage = createImage(bounds.width, bounds.height);
            Graphics graph = tempImage.getGraphics();
            
            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);
            // draw attack poly
		    graph.setColor(game.getEntity(attack.getEntityId()).getOwner().getColor());
		    graph.fillPolygon(attackPoly);
		    graph.setColor(Color.white);
		    graph.drawPolygon(attackPoly);
            
            // create final image
            this.image = makeTransparent(tempImage, TRANSPARENT, 
                                         bounds.width, bounds.height);
        }
        
        /**
         * Return true if the point is inside our polygon
         */
        public boolean isInside(Point point) {
            return super.isInside(point) 
                   && attackPoly.contains(point.x - bounds.x, point.y - bounds.y);
        }
        
        public AttackAction getAttack() {
            return attack;
        }
        
        public String getTooltip() {
            return "attack: " + attack.getEntityId() + " on " + attack.getTargetId();
        }
    }
}
