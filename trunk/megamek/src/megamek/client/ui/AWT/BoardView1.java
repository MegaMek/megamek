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
    implements BoardListener, MouseMotionListener, Runnable
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
    
    // make sure those .gifs get decoded!
    private MediaTracker        tracker = new MediaTracker(this);
    private boolean             imagesLoading = false;
    private boolean             imagesLoaded = false;

    // player-tinted mech image cache
    private Image[][]			imageCache = new Image[PIC_MAX][6]; // [type][facing]
    private Image[][][]			tintCache = new Image[PIC_MAX][6][Player.colorRGBs.length]; // [type][facing][color]
    
    public BoardView1(Game game) {
        this.game = game;
        
        game.board.addBoardListener(this);
		scroller.start();
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
        for (final Enumeration i = entityBuffers.elements(); i.hasMoreElements();) {
            final EntityBuffer ebuff = (EntityBuffer)i.nextElement();
            
            if (view.intersects(ebuff.getBounds())) {
                final int drawX = ebuff.getBounds().x - view.x;
                final int drawY = ebuff.getBounds().y - view.y;
                if (ebuff.getImage() == null) {
                    ebuff.draw(this);
                }
                backGraph.drawImage(ebuff.getImage(), drawX, drawY, this);
            }
        }
        
        // draw the back buffer onto the screen
        g.drawImage(backImage, 0, 0, this);
        
        final long finish = System.currentTimeMillis();
        
        System.out.println("BoardView1: updated screen in " + (finish - start) + " ms.  view = " + view);
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
		int type = getEntityImageIndex(en);
		
		// check cache in image
		if (tintCache[type][en.getSecondaryFacing()][en.getOwner().getColorIndex()] == null) {
			tintCache[type][en.getSecondaryFacing()][en.getOwner().getColorIndex()] = tint(imageCache[type][en.getSecondaryFacing()], en.getOwner().getColorRGB());
		}
		
		return tintCache[type][en.getSecondaryFacing()][en.getOwner().getColorIndex()];
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
		for (int j = 0; j < 72; j++) {
		    for (int i = 0; i < 84; i++) {
				int alpha = (pixels[j * 84 + i] >> 24) & 0xff;
				int black = (pixels[j * 84 + i]) & 0xff;  // assume black & white
				if (alpha == 0xff) {
					// alter pixel to tint
					int red   = (cred   * black) / 255;
					int green = (cgreen * black) / 255;
					int blue  = (cblue  * black) / 255;
					
					pixels[j * 84 + i] = (alpha << 24) + (red << 16) + (green << 8) + blue;
				}
		    }
		}
		return createImage(new MemoryImageSource(84, 72, pixels, 0, 84));
	}
    
    /**
     * Returns a new image, where any pixels of the specified color are
     * transparent
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
                pixels[i] = 0x00000000;
            }
        }
        
        return createImage(new MemoryImageSource(width, height, pixels, 0, width));
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public void redrawEntity(Entity entity) {
        ;
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
        ;
    }
    public void clearAllAttacks() {
        ;
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
        entityBuffers.removeAllElements();
        
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            entityBuffers.addElement(new EntityBuffer(entity));
        }
    }
    public void boardChangedEntity(BoardEvent e) {
        ;
    }
    
    
    
	/**
	 * If the mouse is at the edges of the screen, this
	 * scrolls the board image on the canvas.
	 */
	public void doScroll() {
		int sf = 3; // scroll factor
		// adjust x scroll
		// scroll when the mouse is at the edges
		if (mousePos.x < 100) {
			scroll.x -= (100 - mousePos.x) / sf;
		}
		if (mousePos.x > (backSize.width - 100)) {
			scroll.x -= ((backSize.width - 100) - mousePos.x) / sf;
		}
		// scroll when the mouse is at the edges
		if (mousePos.y < 100) {
			scroll.y -= (100 - mousePos.y) / sf;
		}
		if (mousePos.y > (backSize.height - 100)) {
			scroll.y -= ((backSize.height - 100) - mousePos.y) / sf;
		}
		checkScrollBounds();			
		repaint();
	}
	
	/**
	 * Makes sure that the scroll dimensions stay in bounds
	 */
	public void checkScrollBounds() {
		if (scroll.x < 0) {
			scroll.x = 0;
		}
		if (scroll.x > (boardSize.width - backSize.width)) {
			scroll.x = (boardSize.width - backSize.width);
		}

		if (scroll.y < 0) {
			scroll.y = 0;
		}
		if (scroll.y > (boardSize.height - backSize.height)) {
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
				Thread.sleep(50);
			} catch(InterruptedException ex) {
				// duh?
			}
		}
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
	}
	public void mouseMoved(MouseEvent me) {
		;
	}
    
    
    
    
    /**
     * A class that stores info about the graphical representation of each 
     * entity
     */
    private class EntityBuffer
    {
        private Entity entity;
        
        private Rectangle entityBounds;
        private Image entityImage;
        private Graphics entityGraph;
        
        public EntityBuffer(Entity entity) {
            this.entity = entity;

		    String model = entity.getModel();
		    Font font = new Font("SansSerif", Font.PLAIN, 10);
		    Rectangle modelRect = new Rectangle(47, 55,  
                                 getFontMetrics(font).stringWidth(model) + 1, 
                                 getFontMetrics(font).getAscent());
            Rectangle bounds = new Rectangle(0, 0, 84, 72).union(modelRect);
            bounds.setLocation(getHexLocation(entity.getPosition()));
            entityBounds = bounds;
            
            entityImage = null;
        }
        
        /**
         * Creates the sprite for this entity.  It is an extra pain in the 
         * butt to create transparent images in AWT.
         */
        public void draw(ImageObserver observer) {
            int TRANSPARENT = 0xFFFF0000;
            // figure out size
		    String model = entity.getModel();
		    Font font = new Font("SansSerif", Font.PLAIN, 10);
		    Rectangle modelRect = new Rectangle(47, 55,  
                                 getFontMetrics(font).stringWidth(model) + 1, 
                                 getFontMetrics(font).getAscent());
            Rectangle bounds = new Rectangle(0, 0, 84, 72).union(modelRect);
            
            // create image for buffer
            Image tempImage = createImage(bounds.getSize().width, 
                                          bounds.getSize().height);
            entityGraph = tempImage.getGraphics();
            
            // draw everything in
            entityGraph.setColor(new Color(TRANSPARENT));
            entityGraph.fillRect(0, 0, bounds.getSize().width, 
                                 bounds.getSize().height);
            
            entityGraph.drawImage(getEntityImage(entity), 0, 0, observer);
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
            entityGraph.setFont(font);
		    entityGraph.setColor(bcol);
		    entityGraph.fillRect(modelRect.x, modelRect.y, 
                                 modelRect.width, modelRect.height);
            modelRect.translate(-1, -1);
		    entityGraph.setColor(col);
		    entityGraph.fillRect(modelRect.x, modelRect.y,
                                 modelRect.width, modelRect.height);
		    entityGraph.setColor(entity.ready ? Color.black : Color.lightGray);
		    entityGraph.drawString(model, modelRect.x + 1, modelRect.y + getFontMetrics(font).getAscent() - 1);
            
            // create final image
            entityImage = makeTransparent(tempImage, TRANSPARENT, 
                                          bounds.getSize().width, 
                                          bounds.getSize().height);
        }
        
        public Rectangle getBounds() {
            return entityBounds;
        }
        
        public Image getImage() {
            return entityImage;
        }
        
    }
}
