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

public class BoardView1
    extends Canvas
    implements BoardListener, MouseListener, MouseMotionListener, Runnable
{
    private static final int    PIC_MAX				= 10;
    private static final int    PIC_MECH_LIGHT		= 1;
    private static final int    PIC_MECH_MEDIUM		= 2;
    private static final int    PIC_MECH_HEAVY		= 3;
    private static final int    PIC_MECH_ASSAULT	= 4;

    private Game game;
    
    // srcolly stuff:
    private Point mousePos = new Point();
    private Thread scroller = new Thread(this);
    private Point scroll = new Point();;
    
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
    private Vector entityBuffers = new Vector();
    private Hashtable entityBufferIds = new Hashtable();
    
    // images for the three selection cursors
    
    // buffer for current movement
    
    // vector of buffers for all firing lines
    private Vector attackBuffers = new Vector();
    
    // make sure those .gifs get decoded!
    private MediaTracker        tracker = new MediaTracker(this);
    private boolean             imagesLoading = false;
    private boolean             imagesLoaded = false;

    // player-tinted mech image cache
    private Image[][]			imageCache = new Image[PIC_MAX][6]; // [type][facing]
    private Image[][][]			tintCache = new Image[PIC_MAX][6][Player.colorRGBs.length]; // [type][facing][color]
    
    /**
     * Construct a new board view for the specified game
     */
    public BoardView1(Game game) {
        this.game = game;
        
        game.board.addBoardListener(this);
		scroller.start();
		addMouseListener(this);
		addMouseMotionListener(this);
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
        drawVisibleBuffers(entityBuffers, view);
        
        // draw onscreen attacks
        drawVisibleBuffers(attackBuffers, view);
        
        // draw the back buffer onto the screen
        g.drawImage(backImage, 0, 0, this);
        
        final long finish = System.currentTimeMillis();
        
        System.out.println("BoardView1: updated screen in " + (finish - start) + " ms.  view = " + view);
    }
    
    /**
     * Looks through a vector of buffered images and draws them if they're
     * onscreen.
     */
    private void drawVisibleBuffers(Vector bufferVector, Rectangle view) {
        for (final Enumeration i = bufferVector.elements(); i.hasMoreElements();) {
            final ImageBuffer buff = (ImageBuffer)i.nextElement();
            
            if (view.intersects(buff.getBounds())) {
                final int drawX = buff.getBounds().x - view.x;
                final int drawY = buff.getBounds().y - view.y;
                if (buff.getImage() == null) {
                    buff.draw(this);
                }
                backGraph.drawImage(buff.getImage(), drawX, drawY, this);
            }
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
        
        int drawWidth = boardRect.width / 63;
        int drawHeight = boardRect.height / 72;
        
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
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public void redrawEntity(Entity entity) {
        EntityBuffer buff = (EntityBuffer)entityBufferIds.get(new Integer(entity.getId()));
        
        if (buff != null) {
            entityBuffers.removeElement(buff);
        }
        
        buff = new EntityBuffer(entity);
        entityBuffers.addElement(buff);
        entityBufferIds.put(new Integer(entity.getId()), buff);
    }

    private void redrawAllEntities() {
        entityBuffers.removeAllElements();
        entityBufferIds.clear();
        
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            redrawEntity(entity);
        }
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
        for (final Enumeration i = attackBuffers.elements(); i.hasMoreElements();) {
            final AttackBuffer buff = (AttackBuffer)i.nextElement();
            
            if (buff.getAttack().getEntityId() == aa.getEntityId() 
                && buff.getAttack().getTargetId() == aa.getTargetId()) {
                return;
            }
        }
        
        // okay, add a new one
        attackBuffers.addElement(new AttackBuffer(aa));
    }
    public void clearAllAttacks() {
        attackBuffers.removeAllElements();
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
        
        return createImage(new MemoryImageSource(width, height, pixels, 0, width));
    }

    
    
	public void boardHexMoused(BoardEvent b) {
		;
	}
	public void boardHexCursor(BoardEvent b) {
		;
	}
	public void boardHexSelected(BoardEvent b) {
		;
	}
	public void boardHexHighlighted(BoardEvent b) {
		;
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
			} else {
				repaint();
			}
		}
	}
    
	//
	// MouseListener
	//
	public void mousePressed(MouseEvent me) {
		game.board.mouseAction(getCoordsAt(me.getPoint()), Board.BOARD_HEX_DRAG, me.getModifiers()); 
		//board.drag(hexAt(me.getPoint()));
	}
	public void mouseReleased(MouseEvent me) {
		//System.out.println("moure click count: " + me.getClickCount());
		if (me.getClickCount() == 1) {
			game.board.mouseAction(getCoordsAt(me.getPoint()), Board.BOARD_HEX_CLICK, me.getModifiers()); 
			//board.select(hexAt(me.getPoint()));
		} else {
			game.board.mouseAction(getCoordsAt(me.getPoint()), Board.BOARD_HEX_DOUBLECLICK, me.getModifiers()); 
			//board.select(hexAt(me.getPoint()));
		}
		mousePos = new Point(getSize().width / 2, getSize().height / 2);
	}
	public void mouseEntered(MouseEvent me) {
		;
	}
	public void mouseExited(MouseEvent me) {
		;
	}
	public void mouseClicked(MouseEvent me) {
		;
	}
	
	//
	// MouseMotionListener
	//
	public void mouseDragged(MouseEvent me) {
		mousePos = me.getPoint();
		if (backSize != null) {
			doScroll();
		} else {
			repaint();
		}
		game.board.mouseAction(getCoordsAt(me.getPoint()), Board.BOARD_HEX_DRAG, me.getModifiers()); 
	}
	public void mouseMoved(MouseEvent me) {
		;
	}
    
    
    
    
    /**
     * Stores a buffered image to draw
     */
    private abstract class ImageBuffer
    {
        private Rectangle bounds;
        private Image image;
        private Graphics graph;
        
        public abstract void draw(ImageObserver observer);
        
        public Rectangle getBounds() {
            return bounds;
        }
        
        public Image getImage() {
            return image;
        }
    }
    
    /**
     * A class that stores info about the graphical representation of each 
     * entity
     */
    private class EntityBuffer extends ImageBuffer
    {
        private Entity entity;
        
        public EntityBuffer(Entity entity) {
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
         * Creates the sprite for this entity.  It is an extra pain in the 
         * butt to create transparent images in AWT.
         */
        public void draw(ImageObserver observer) {
            final int TRANSPARENT = 0xFFFF00FF;
            // figure out size
		    String model = entity.getModel();
		    Font font = new Font("SansSerif", Font.PLAIN, 10);
		    Rectangle modelRect = new Rectangle(47, 55,  
                                 getFontMetrics(font).stringWidth(model) + 1, 
                                 getFontMetrics(font).getAscent());
            Rectangle wholeRect = new Rectangle(0, 0, 84, 72).union(modelRect);
            
            // create image for buffer
            Image tempImage = createImage(bounds.getSize().width, 
                                          bounds.getSize().height);
            this.graph = tempImage.getGraphics();
            
            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.getSize().width, 
                                 bounds.getSize().height);
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
		    graph.fillRect(modelRect.x, modelRect.y, 
                                 modelRect.width, modelRect.height);
            modelRect.translate(-1, -1);
		    graph.setColor(col);
		    graph.fillRect(modelRect.x, modelRect.y,
                                 modelRect.width, modelRect.height);
		    graph.setColor(entity.ready ? Color.black : Color.lightGray);
		    graph.drawString(model, modelRect.x + 1, modelRect.y + getFontMetrics(font).getAscent() - 1);
            
            // create final image
            this.image = makeTransparent(tempImage, TRANSPARENT, 
                                    bounds.getSize().width, 
                                    bounds.getSize().height);
        }
    }
    
    /**
     * Holds graphics and info for an attack
     */
    private class AttackBuffer extends ImageBuffer
    {
        private AttackAction attack;
        private Polygon attackPoly;
        
        public AttackBuffer(AttackAction attack) {
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
            
            System.out.println("BoardView.AttackBuffer: new poly = " + attackPoly);
            System.out.println("BoardView.AttackBuffer: bounds = " + attackPoly.getBounds());
            
            // set bounds
            this.bounds = new Rectangle(attackPoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1, bounds.getSize().height + 1);
            
            // move poly to upper right of image
            attackPoly.translate(-bounds.getLocation().x, -bounds.getLocation().y);
            
            // nullify image
            this.image = null;
        }
        
        /**
         * Creates the sprite for this entity.  It is an extra pain in the 
         * butt to create transparent images in AWT.
         */
        public void draw(ImageObserver observer) {
            final int TRANSPARENT = 0xFFFF00FF;
            // create image for buffer
            Image tempImage = createImage(bounds.getSize().width, 
                                          bounds.getSize().height);
            this.graph = tempImage.getGraphics();
            
            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.getSize().width, 
                                 bounds.getSize().height);
            // draw attack poly
		    graph.setColor(game.getEntity(attack.getEntityId()).getOwner().getColor());
		    graph.fillPolygon(attackPoly);
		    graph.setColor(Color.white);
		    graph.drawPolygon(attackPoly);
            
            // create final image
            this.image = makeTransparent(tempImage, TRANSPARENT, 
                                         bounds.getSize().width, 
                                         bounds.getSize().height);
        }
        
        public AttackAction getAttack() {
            return attack;
        }
    }
}
