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

public class BoardView 
	extends Canvas 
	implements MouseMotionListener, MouseListener, 
	KeyListener, BoardListener, Runnable
{
    public static final int		PIC_MAX				= 10;
    public static final int		PIC_MECH_LIGHT		= 1;
    public static final int		PIC_MECH_MEDIUM		= 2;
    public static final int		PIC_MECH_HEAVY		= 3;
    public static final int		PIC_MECH_ASSAULT	= 4;
    	
    public Game                 game;
    public Board				board;
        	
    public Polygon				hexPoly;
    public Polygon[]			facingPolys;
    public Polygon[]			movementPolys;
        	
    // make sure those .gifs get decoded!
    public MediaTracker			tracker;
    public boolean				imagesLoading;
    public boolean				imagesLoaded;
        	
    // here's the buffer image and associates
    public boolean				busy;
    int							busyCount;
    public BoardBuffer[]		bb;
        	
    public int					NUM_BUFFERS = 2;
    public int					cb;
    	
    public Dimension			boardSize;
    	
    // where are we scrolled to?
    public int					scrollx;
    public int					scrolly;
    public Point				mPos;
    public Thread				scroller;
    	
    // what hex is highlighted?
    public Coords			    cursor;
    public Color				cursorColor;
    public Coords			    highlighted;
    public Color				highColor;
    public Coords			    selected;
    public Color				selectColor;
    	
    // "dirty" hexes
    //public Vector				entityHexes;
    public Vector				moveDataHexes;
    public Vector				attacks;
    	
    // player-tinted mech image cache
    public Image[][]			imageCache;  // [type][facing]
    public Image[][][]			tintCache;   // [type][facing][color]
    
    long                        hexesDrawn;

	public BoardView(Game game) {
        this.game = game;
		this.board = game.board;
		board.addBoardListener(this);
		
		boardSize = new Dimension(board.width * 63 + 21, board.height * 72 + 36);

		initPolys();

		tracker = new MediaTracker(this);
		imagesLoading = false;
		imagesLoaded = false;
		
		
		bb = new BoardBuffer[NUM_BUFFERS];
		cb = 0;
		busy = false;
		
		mPos = new Point();
		
		cursorColor = Color.pink;
		highColor = Color.white;
		selectColor = Color.magenta;
		
		moveDataHexes = new Vector();
		attacks = new Vector();
		
		imageCache = new Image[PIC_MAX][6];
		tintCache = new Image[PIC_MAX][6][Player.colorRGBs.length];
		
		scroller = new Thread(this);
		scroller.start();

		addMouseMotionListener(this);
		addMouseListener(this);
		addKeyListener(this);
	}
	
	/**
	 * loadAllImages
	 */
	public void loadAllImages() {
		imagesLoaded = false;
		
		for(int i = 0; i < 6; i++) {
			imageCache[PIC_MECH_LIGHT][i] = getToolkit().getImage("data/mex/light-" + i + ".gif");
			imageCache[PIC_MECH_MEDIUM][i] = getToolkit().getImage("data/mex/medium-" + i + ".gif");
			imageCache[PIC_MECH_HEAVY][i] = getToolkit().getImage("data/mex/heavy-" + i + ".gif");
			imageCache[PIC_MECH_ASSAULT][i] = getToolkit().getImage("data/mex/assault-" + i + ".gif");
			
			tracker.addImage(imageCache[PIC_MECH_LIGHT][i], 1);
			tracker.addImage(imageCache[PIC_MECH_MEDIUM][i], 1);
			tracker.addImage(imageCache[PIC_MECH_HEAVY][i], 1);
			tracker.addImage(imageCache[PIC_MECH_ASSAULT][i], 1);
		}
    
    for(int i = 0; i < board.terrains.length; i++) {
      tracker.addImage(board.terrains[i].getImage(this), 2);
    }
		
    /* old and slow!
		for(int y = 0; y < board.height; y++) {
			for(int x = 0; x < board.width; x++) {
				// add image to media tracker
				tracker.addImage(board.getHex(x, y).getImage(this), 2);
			}
		}
    */
    
		imagesLoading = true;
	}
	
	/**
	 * Refreshes every hex image onto the board image.
	 */
	public void drawWholeBoard(BoardBuffer b) {
		for(int y = 0; y < board.height; y++) {
			for(int x = 0; x < board.width; x++) {
				drawHex(b, x, y);
			}
		}
		drawAllEntities(b);
	}
	
	/**
	 * Refreshes every hex image onto the buffer image.
	 */
	public void redrawBuffer(BoardBuffer b, Point n) {
		// we're busy now
		busy = true;
		
		bb[cb].o = new Point(n);

		// hexes
		int xo = (int)Math.floor((b.o.x - 21) / 63.0);
		int xs = (int)Math.ceil((b.s.width + 21) / 63.0);
		int yo = (int)Math.floor((b.o.y - 36) / 72.0);
		int ys = (int)Math.ceil((b.s.height + 36) / 72.0);
		
		redrawBuffer(b, xo, yo, xs, ys);
		
		// should not be busy any more
		busy = false;
	}
	
	/**
	 * Refreshes the specified hexes onto the buffer
	 */
	public void redrawBuffer(BoardBuffer b, int x, int y, int width, int height) {
		if (x < 0) {
			width += x;
			x = 0;
		}
		if (y < 0) {
			height += y;
			y = 0;
		}
//		System.err.println("graphics: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ", area=" + (width * height));
		for(int j = y; j <= y + height; j++) {
			for(int i = x; i <= x + width; i++) {
				redrawHex(b, i, j);
			}
		}
	}
	
	/**
	 * Refreshes every entity.
	 */
	public void drawAllEntities(BoardBuffer b) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		for(Enumeration e = game.getEntities(); e.hasMoreElements();) {
			Entity en = (Entity)e.nextElement();
			if (en.isTargetable()) {
				redrawHex(b, en.getPosition());
			} else {
				redrawHexFull(b, en.getPosition());
			}
		}
		
		// as you were.
		busy = oldBusy;
	}
	
	
	/**
	 * Refreshes onscreen entities.
	 */
	public void drawOnscreenEntities(BoardBuffer b) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		for(Enumeration e = game.getEntities(); e.hasMoreElements();) {
			Entity en = (Entity)e.nextElement();
			if (onscreen(en.getPosition())) {
				if (en.isTargetable()) {
					redrawHex(b, en.getPosition());
				} else {
					redrawHexFull(b, en.getPosition());
				}
			}
		}
		
		// as you were.
		busy = oldBusy;
	}
	
	
	/**
	 * Draws the specified hex onto the board image.
	 */
	public void drawHex(BoardBuffer b, Coords c) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		Hex hex = board.getHex(c);
		if (hex == null) {
			// don't draw, then.
			return;
		}
		Image hexpic = hex.getImage(this);
		Point p = getHexLocation(c.x, c.y);
		// draw picture
		b.g.drawImage(hexpic, p.x, p.y, this);
		// draw elevation
		b.g.setColor(Color.black);
		b.g.setFont(new Font("SansSerif", Font.PLAIN, 10));
		b.g.drawString(c.getBoardNum(), p.x + 30, p.y + 12);
		if (hex.getElevation() != 0) {
			b.g.setFont(new Font("SansSerif", Font.PLAIN, 9));
			b.g.drawString("LEVEL " + hex.getElevation(), p.x + 24, p.y + 70);
		}
		b.g.setFont(getFont());
		// draw elevation borders
		Hex tHex;
		if ((tHex = board.getHex(c.translated(0))) != null && tHex.getElevation() != hex.getElevation()) {
			b.g.drawLine(p.x + 21, p.y, p.x + 62, p.y);
		}
		if ((tHex = board.getHex(c.translated(1))) != null && tHex.getElevation() != hex.getElevation()) {
			b.g.drawLine(p.x + 62, p.y, p.x + 83, p.y + 35);
		}
		if ((tHex = board.getHex(c.translated(2))) != null && tHex.getElevation() != hex.getElevation()) {
			b.g.drawLine(p.x + 83, p.y + 36, p.x + 62, p.y + 71);
		}
		if ((tHex = board.getHex(c.translated(3))) != null && tHex.getElevation() != hex.getElevation()) {
			b.g.drawLine(p.x + 62, p.y + 71, p.x + 21, p.y + 71);
		}
		if ((tHex = board.getHex(c.translated(4))) != null && tHex.getElevation() != hex.getElevation()) {
			b.g.drawLine(p.x + 21, p.y + 71, p.x, p.y + 36);
		}
		if ((tHex = board.getHex(c.translated(5))) != null && tHex.getElevation() != hex.getElevation()) {
			b.g.drawLine(p.x, p.y + 35, p.x + 21, p.y);
		}
        
        hexesDrawn++;
		
		// as you were.
		busy = oldBusy;
	}
	public void drawHex(BoardBuffer b, int x, int y) {
		drawHex(b, new Coords(x, y));
	}
  
  /**
   * Re-draw an entity
   */
  public void redrawEntity(Entity e) {
    redrawHexFull(bb[cb], e.getPosition());
  }
	
	/**
	 * Draw the entity onto the board, on top of whatever's
	 * already there (an empty hex, presumably.)
	 */
	public void drawEntity(BoardBuffer b, Entity e) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		// don't draw non-existant entities
		if (e == null || !e.isTargetable()) {
			return;
		}
		Point p = getHexLocation(e.getPosition());
		// draw picture
		b.g.drawImage(getEntityImage(e), p.x, p.y, this);
		// draw mech code
		String code = e.getModel();
		Font f = new Font("SansSerif", Font.PLAIN, 10);
		b.g.setFont(f);
		Dimension cs = new Dimension(b.g.getFontMetrics(f).stringWidth(code) + 1, b.g.getFontMetrics(f).getAscent());
		Color col, bcol;
		if (e.isImmobile()) {
			col = Color.black;
			bcol = Color.lightGray;
		} else {
			col = e.ready ? Color.lightGray : Color.darkGray;
			bcol = e.ready ? Color.darkGray : Color.black;
		}
		b.g.setColor(bcol);
		b.g.fillRect(p.x + 47, p.y + 55, cs.width, cs.height);
		b.g.setColor(col);
		b.g.fillRect(p.x + 46, p.y + 54, cs.width, cs.height);
		b.g.setColor(e.ready ? Color.black : Color.lightGray);
		b.g.drawString(code, p.x + 46, p.y + 54 + cs.height - 1);
		// draw facing
		b.g.setColor(Color.white);
		if (e.getFacing() != -1) {
			drawPolygon(b.g, facingPolys[e.getFacing()], p.x, p.y);
		}
		if (e.isImmobile() && !e.isProne()) {
			// draw "IMMOBILE"
			b.g.setColor(Color.darkGray);
			b.g.drawString("IMMOBILE", p.x + 11, p.y + 39);
			b.g.setColor(Color.red);
			b.g.drawString("IMMOBILE", p.x + 10, p.y + 38);
		} else if (!e.isImmobile() && e.isProne()) {
			// draw "PRONE"
			b.g.setColor(Color.darkGray);
			b.g.drawString("PRONE", p.x + 26, p.y + 39);
			b.g.setColor(Color.yellow);
			b.g.drawString("PRONE", p.x + 25, p.y + 38);
		} else if (e.isImmobile() && e.isProne()) {
			// draw "IMMOBILE" and "PRONE"
			b.g.setColor(Color.darkGray);
			b.g.drawString("IMMOBILE", p.x + 11, p.y + 35);
			b.g.setColor(Color.red);
			b.g.drawString("IMMOBILE", p.x + 10, p.y + 34);
			b.g.setColor(Color.darkGray);
			b.g.drawString("PRONE", p.x + 26, p.y + 48);
			b.g.setColor(Color.yellow);
			b.g.drawString("PRONE", p.x + 25, p.y + 47);
		}	
		// restore drawing color
		b.g.setColor(getForeground());
		
		// as you were.
		busy = oldBusy;
	}
	
	/**
	 * Draws the movement data for an entity onto the board.
	 */
	public void drawMovementData(Entity e, MovementData md) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		Compute.compile(game, e.getId(), md);

		boolean up = !e.isProne();
		// add current hex to move data hex vector
		moveDataHexes.addElement(e.getPosition());
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MovementData.Step step = (MovementData.Step)i.nextElement();
			Point p = getHexLocation(step.getPosition());
			if (step.getType() == MovementData.STEP_TURN_LEFT
                || step.getType() == MovementData.STEP_TURN_RIGHT) {
				if (onscreen(step.getPosition())) {
					// set the color for the arrows
					Color col = Color.green;
					if (step.getMovementType() == Entity.MOVE_RUN) {
						col = Color.yellow;
					}
					if (step.getMovementType() == Entity.MOVE_JUMP) {
						col = Color.cyan;
					}
					if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
						col = Color.red;
					}
					// draw arrows showing the facing
					bb[cb].g.setColor(Color.darkGray);
					drawPolygon(bb[cb].g, facingPolys[step.getFacing()], p.x + 1, p.y + 1);
					bb[cb].g.setColor(col);
					drawPolygon(bb[cb].g, facingPolys[step.getFacing()], p.x, p.y);
				}
			}
			if (step.getType() == MovementData.STEP_FORWARDS 
                || step.getType() == MovementData.STEP_BACKWARDS) {
				// do entering...
				p = getHexLocation(step.getPosition());
				if (onscreen(step.getPosition())) {
					// set the color for the arrows
					Color col = Color.green;
					if (step.getMovementType() == Entity.MOVE_RUN) {
						col = Color.yellow;
					}
					if (step.getMovementType() == Entity.MOVE_JUMP) {
						col = Color.cyan;
					}
					if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
						col = Color.red;
					}
					// draw arrows showing them entering the next
					bb[cb].g.setColor(Color.darkGray);
					drawPolygon(bb[cb].g, movementPolys[step.getFacing()], p.x + 1, p.y + 1);
					bb[cb].g.setColor(col);
					drawPolygon(bb[cb].g, movementPolys[step.getFacing()], p.x, p.y);
					// draw movement cost
                    String costString = new Integer(step.getMpUsed()).toString()
                                        + (step.isDanger() ? "*" : "");
                    if (step.isPastDanger()) {
					    costString = "(" + costString + ")";
                    }
					bb[cb].g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    int costX = p.x + 42 - (bb[cb].g.getFontMetrics(bb[cb].g.getFont()).stringWidth(costString) / 2);
					bb[cb].g.setColor(Color.darkGray);
					bb[cb].g.drawString(costString, costX, p.y + 39);
					bb[cb].g.setColor(col);
					bb[cb].g.drawString(costString, costX - 1, p.y + 38);
				}
				// add current hex to move data hex vector
				moveDataHexes.addElement(step.getPosition());
			}
		}
			
		// as you were.
		busy = oldBusy;
	}
	
	/**
	 * Clears off movement data from the board.
	 */
	public void clearMovementData(BoardBuffer b) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		for(Enumeration e = moveDataHexes.elements(); e.hasMoreElements();) {
			redrawHex(b, (Coords)e.nextElement());
		}
		moveDataHexes.removeAllElements();
			
		// as you were.
		busy = oldBusy;
	}
	
	/**
	 * Clears off movement data from the board.
	 */
	public void clearMovementData() {
		clearMovementData(bb[cb]);
	}
	
	/**
	 * Adds the data required to draw the lines representing attacks
	 */
	public void addAttack(AttackAction attack) {
		// check if there's already a line there.
		for (int i = 0; i < attacks.size(); i++) {
			AttackAction caa = (AttackAction)attacks.elementAt(i);
			if (caa.getEntityId() == attack.getEntityId()
               && caa.getTargetId() == attack.getTargetId()) {
				return;
			}
		}
		drawAttack(attack);
		attacks.addElement(attack);
	}
	
	/**
	 * Draws a "firing line" between two hexes.
	 */
	public void drawAttack(AttackAction attack) {
        final Entity ae = game.getEntity(attack.getEntityId());
        final Entity te = game.getEntity(attack.getTargetId());
        
        if (ae == null || te == null) {
            System.err.println("BoardView.drawAttack: got null entity");
            return;
        }
        
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;

        Point a = getHexLocation(ae.getPosition());
		Point t = getHexLocation(te.getPosition());
		
		double an = (ae.getPosition().radian(te.getPosition()) + (Math.PI * 1.5)) % (Math.PI * 2);
		double ll = 3;
		
		Polygon p = new Polygon();
		p.addPoint(a.x + 42 - (int)Math.round(Math.sin(an) * ll), a.y + 36 + (int)Math.round(Math.cos(an) * ll));
		p.addPoint(a.x + 42 + (int)Math.round(Math.sin(an) * ll), a.y + 36 - (int)Math.round(Math.cos(an) * ll));
		p.addPoint(t.x + 42 + (int)Math.round(Math.sin(an) * ll), t.y + 36 - (int)Math.round(Math.cos(an) * ll));
		p.addPoint(t.x + 42 - (int)Math.round(Math.sin(an) * ll), t.y + 36 + (int)Math.round(Math.cos(an) * ll));
		
		bb[cb].g.setColor(game.getEntity(attack.getEntityId()).getOwner().getColor());
		bb[cb].g.fillPolygon(p);
		bb[cb].g.setColor(Color.white);
		bb[cb].g.drawPolygon(p);
			
		// as you were.
		busy = oldBusy;
	}
	
	/**
	 * Redraws all firing lines
	 */
	public void redrawAllAttacks() {
		for(int i = 0; i < attacks.size(); i++) {
			drawAttack((AttackAction)attacks.elementAt(i));
		}
	}
	
	/**
	 * Clears all firing lines
	 */
	public void clearAllAttacks(BoardBuffer b) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		// we need iterate thru a clone to avoid the lines being redrawn as they are cleared
		final Vector tempData = (Vector)attacks.clone();
		attacks.removeAllElements();
		for (int i = 0; i < tempData.size(); i++) {
            final AttackAction attack = (AttackAction)tempData.elementAt(i);
            final Entity ae = game.getEntity(attack.getEntityId());
            final Entity te = game.getEntity(attack.getTargetId());
        
            // check for validity
            if (ae == null || te == null) {
                // aww, what the heck, just return
                System.err.println("BoardView.clearAllAttacks: got null entity");
                return;
                //throw new IllegalSomethingException("attacker or target are null!");
            }
            
			Coords[] fc = Compute.intervening(ae.getPosition(), te.getPosition());
			for (int j = 0; j < fc.length; j++) {
				redrawHexFull(b, fc[j]);
			}
		}
		
		// as you were.
		busy = oldBusy;
	}
	
	/**
	 * Clears all firing lines
	 */
	public void clearAllAttacks() {
		clearAllAttacks(bb[cb]);
	}
	
	/**
	 * Centers the view on a certain hex, or close to it.
	 */
	public void centerOnHex(Coords c) {
    if (bb[cb] == null) {
      return;
    }
    
    scrollx = c.x * 63 - (bb[cb].s.width / 2) - 42;
    scrolly = (c.y * 72 + (c.isXOdd() ? 36 : 0)) - (bb[cb].s.height / 2) - 36;
		
		mPos = new Point(getSize().width / 2, getSize().height / 2);
		doScroll();
	}
	
	/**
	 * "Cursors" the hex.
	 */
	public void cursorHex(Coords c) {
		cursorHex(c, cursorColor);
	}

	/**
	 * "Cursors" the hex.
	 */
	public void cursorHex(Coords c, Color color) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		cursor = c;
		if (bb[cb] != null && c != null) {
			Point p = getHexLocation(c.x, c.y);
			cursorColor = color;
			bb[cb].g.setColor(color);
			drawPolygon(bb[cb].g, hexPoly, p.x, p.y);
			bb[cb].g.setColor(getForeground());
		}
		
		// as you were.
		busy = oldBusy;
	}
	
	/**
	 * Highlights the hex.
	 */
	public void highlightHex(Coords c) {
		highlightHex(c, highColor);
	}

	/**
	 * Highlights the hex.
	 */
	public void highlightHex(Coords c, Color color) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		highlighted = c;
		if (bb[cb] != null && c != null) {
			Point p = getHexLocation(c.x, c.y);
			highColor = color;
			bb[cb].g.setColor(color);
			drawPolygon(bb[cb].g, hexPoly, p.x, p.y);
			bb[cb].g.setColor(getForeground());
		}
		
		// as you were.
		busy = oldBusy;
	}
	
	/**
	 * Selects the hex.
	 */
	public void selectHex(Coords c) {
		selectHex(c, selectColor);
	}
	
	/**
	 * Selects the hex.
	 */
	public void selectHex(Coords c, Color color) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		selected = c;
		if (bb[cb] != null && c != null) {
			Point p = getHexLocation(c.x, c.y);
			selectColor = color;
			bb[cb].g.setColor(color);
			// lines!
			bb[cb].g.drawLine(p.x + 21, p.y, p.x + 62, p.y);
			bb[cb].g.drawLine(p.x + 62, p.y, p.x + 83, p.y + 35);
			bb[cb].g.drawLine(p.x + 83, p.y + 36, p.x + 62, p.y + 71);
			bb[cb].g.drawLine(p.x + 62, p.y + 71, p.x + 21, p.y + 71);
			bb[cb].g.drawLine(p.x + 21, p.y + 71, p.x, p.y + 36);
			bb[cb].g.drawLine(p.x, p.y + 35, p.x + 21, p.y);
			bb[cb].g.setColor(getForeground());
		}
		
		// as you were.
		busy = oldBusy;
	}
	
	/**
	 * Redraws the hex, restoring any overlays on it.
	 */
	public void redrawHex(BoardBuffer b, int x, int y) {
		redrawHex(b, new Coords(x, y));
	}
	
	/**
	 * Redraws the hex, restoring any overlays on it.
	 */
	public void redrawHex(BoardBuffer b, Coords c) {
		// whatever you're doing, you're busy now.
		boolean oldBusy = busy;
		busy = true;
		
		drawHex(b, c);
		if (cursor != null && cursor.equals(c)) {
			cursorHex(c);
		}
		if (highlighted != null && highlighted.equals(c)) {
			highlightHex(c);
		}
		if (selected != null && selected.equals(c)) {
			selectHex(c);
		}
		drawEntity(bb[cb], game.getEntity(c));
		drawEntity(bb[cb], game.getEntity(c.translated(5)));
		redrawAllAttacks();
		
		// as you were.
		busy = oldBusy;
	}
	
	/**
	 * Redraws the hex, restoring any overlays on it.
	 */
	public void redrawHexFull(BoardBuffer b, Coords c) {
		redrawHex(b, c);
		for(int i = 0; i < 6; i++) {
			redrawHex(b, c.translated(i));
		}
	}
	
	/**
	 * Draws the outline of a polygon defined by the specified 
	 * Polygon object, offset by the specified x and y.
	 */
	public void drawPolygon(Graphics g, Polygon p, int x, int y) {
		Polygon p2 = new Polygon(p.xpoints, p.ypoints, p.npoints);
		p2.translate(x, y);
		g.drawPolygon(p2);
	}
	
	/**
	 * Override Canvas.paint(g);
	 */
	public void paint(Graphics g) {
		update(g);
	}
	
	/**
	 * Override Component.update(g);
	 *
	public void update(Graphics g) {
		Dimension size = getSize();
		if (bb[cb].i == null) {
			// make new buffer image
			bb[cb].s = new Dimension(board.getWidth * 63 + 21, board.getHeight * 72 + 36);
			bb[cb].i = createImage(bb[cb].s.width, bb[cb].s.height);
			bb[cb].g = bb[cb].i.getGraphics();
			// load images
			g.drawString("loading images...", 20, 50);
			imagesLoaded = false;
			loadAllImages();
			return;
		}
		if (!imagesLoaded) {
			imagesLoaded = tracker.checkAll(true);
			if (imagesLoaded) {
				g.drawString("drawing board...", 20, 70);
				drawWholeBoard();
			}
		} else {
			if (bb[cb].s.width < size.width) {
				int side = (size.width - bb[cb].s.width) / 2;
				g.clearRect(0, 0, side, size.height);
				g.clearRect(0, size.width - side, side, size.height);
			}
			g.drawImage(bImg, scrollx, scrolly, this);
			//g.drawImage(bImg, 0, 0, size.width, size.height, -scrollx, -scrolly, (size.width * 2) - scrollx, (size.height * 2) - scrolly, this);
		}
	}
*/
	/**
	 * Override Component.update(g);
	 */
	public void update(Graphics g) {
		Dimension size = getSize();
		if (bb[cb] != null && !size.equals(bb[cb].s)) {
			// check for resizing, and if so, nullify current buffer
			bb[cb] = null;
		}
		if (!imagesLoading) {
			g.drawString("loading images...", 20, 50);
			loadAllImages();
			return;
		}
		if (!imagesLoaded) {
			imagesLoaded = tracker.checkAll(true);
			return;
		}
		if (!busy) {
			// buffer flip
			int lb = cb;
			cb = (cb + 1) % NUM_BUFFERS;
			if (bb[cb] == null) {
				// make new buffer image
				bb[cb] = new BoardBuffer(size, this);
			}
 			if (bb[lb] == null || bb[lb].o == null) {
				redrawBuffer(bb[cb], new Point(scrollx, scrolly));
			} else {
				moveBuffer(lb, new Point(scrollx, scrolly));
			}
		} else {
			if (busyCount++ > 20) {
				// what's the holdup?
				
				System.err.println("update: forced not busy.");
				
				busy = false;
			}
		}
		g.drawImage(bb[cb].i, 0, 0, this);
	}
	
	/**
	 * Salvages what it can of the old buffer and draws the new one.
	 */
	public void moveBuffer(int lb, Point n) {
		// busy now
		busy = true;
		
		Point o = bb[lb].o;
		bb[cb].o = new Point(n);
		// clear the edges, if necessary
		if (n.x < 21) {
			bb[cb].g.clearRect(0, 0, 21 - n.x, bb[cb].s.height);
		}
		if (n.y < 36) {
			bb[cb].g.clearRect(0, 0, bb[cb].s.width, 36 - n.y);
		}
		if (n.x > (boardSize.width - bb[cb].s.width - 21)) {
			bb[cb].g.clearRect(bb[cb].s.width - 21, 0, bb[cb].s.width, bb[cb].s.height);
		}
		if (n.y > (boardSize.height - bb[cb].s.height - 36)) {
			bb[cb].g.clearRect(0,  bb[cb].s.height - 36, bb[cb].s.width, bb[cb].s.height);
		}
		// copy the old image
		bb[cb].g.drawImage(bb[lb].i, o.x - n.x, o.y - n.y, this);
		// fill in the missing hexes
		//TODO: clean this up
		int xo, xs, yo, ys;
		if (n.x < o.x) {
			xo = (int)Math.floor((n.x - 21) / 63.0);
			xs = (int)Math.ceil((o.x - n.x + 21) / 63.0);
			yo = (int)Math.floor((n.y - 36) / 72.0);
			ys = (int)Math.ceil((bb[cb].s.height + 36) / 72.0);
			redrawBuffer(bb[cb], xo, yo, xs, ys);
			if (n.y < o.y) {
				xo = (int)Math.floor((o.x - 21) / 63.0);
				xs = (int)Math.ceil((bb[cb].s.width - (o.x - n.x) + 21) / 63.0);
				yo = (int)Math.floor((n.y - 36) / 72.0);
				ys = (int)Math.ceil(((o.y - n.y)  + 36) / 72.0);
				redrawBuffer(bb[cb], xo, yo, xs, ys);
			}
			if (n.y > o.y) {
				xo = (int)Math.floor((o.x - 21) / 63.0);
				xs = (int)Math.ceil((bb[cb].s.width - (o.x - n.x) + 21) / 63.0);
				yo = (int)Math.floor((o.y + bb[cb].s.height - 36) / 72.0);
				ys = (int)Math.ceil(((n.y - o.y)  + 36) / 72.0);
				redrawBuffer(bb[cb], xo, yo, xs, ys);
			}
		}
		if (n.x > o.x) {
			xo = (int)Math.floor((o.x + bb[cb].s.width - 21) / 63.0);
			xs = (int)Math.ceil((n.x - o.x + 21) / 63.0) + 1;
			yo = (int)Math.floor((n.y - 36) / 72.0);
			ys = (int)Math.ceil((bb[cb].s.height + 36) / 72.0) + 1;
			redrawBuffer(bb[cb], xo, yo, xs, ys);
			if (n.y < o.y) {
				xo = (int)Math.floor((n.x - 21) / 63.0);
				xs = (int)Math.ceil((bb[cb].s.width - (n.x - o.x) + 21) / 63.0);
				yo = (int)Math.floor((n.y - 36) / 72.0);
				ys = (int)Math.ceil(((o.y - n.y)  + 36) / 72.0);
				redrawBuffer(bb[cb], xo, yo, xs, ys);
			}
			if (n.y > o.y) {
				xo = (int)Math.floor((n.x - 21) / 63.0);
				xs = (int)Math.ceil((bb[cb].s.width - (n.x - o.x) + 21) / 63.0);
				yo = (int)Math.floor((o.y + bb[cb].s.height - 36) / 72.0);
				ys = (int)Math.ceil(((n.y - o.y)  + 36) / 72.0);
				redrawBuffer(bb[cb], xo, yo, xs, ys);
			}
		}
		if (n.x == o.x) {
			if (n.y < o.y) {
				xo = (int)Math.floor((n.x - 21) / 63.0);
				xs = (int)Math.ceil((bb[cb].s.width + 21) / 63.0);
				yo = (int)Math.floor((n.y - 36) / 72.0);
				ys = (int)Math.ceil(((o.y - n.y)  + 36) / 72.0);
				redrawBuffer(bb[cb], xo, yo, xs, ys);
			}
			if (n.y > o.y) {
				xo = (int)Math.floor((n.x - 21) / 63.0);
				xs = (int)Math.ceil((bb[cb].s.width + 21) / 63.0);
				yo = (int)Math.floor((o.y + bb[cb].s.height - 36) / 72.0);
				ys = (int)Math.ceil(((n.y - o.y)  + 36) / 72.0);
				redrawBuffer(bb[cb], xo, yo, xs, ys);
			}
		}
		
		// okay start updating again
		busy = false;
	}
	
	/**
	 * Returns where a hex would be on the buffer board
	 */
	public Coords hexAt(Point p) {
		int x = (p.x + scrollx) / 63;
		int y = ((p.y + scrolly) - ((x & 1) == 1 ? 36 : 0)) / 72;
		return new Coords(x, y);
	}
	
	/**
	 * Returns whether a hex is onscreen or not
	 */
	public boolean onscreen(int x, int y) {
		Point p = getHexLocation(x, y);
		return p.x > -84 && p.y > -72 && p.x < bb[cb].s.width && p.y < bb[cb].s.height;
	}
	
	/**
	 * Returns whether a hex is onscreen or not
	 */
	public boolean onscreen(Coords c) {
		return onscreen(c.x, c.y);
	}
	
	/**
	 * Returns a Point indicating the hex's upper-left corner
	 * on the board image.
	 */
	public Point getHexLocation1(int x, int y) {
		return new Point(x * 63, y * 72 + ((x & 1) == 1 ? 36 : 0));
	}
	
	/**
	 * Returns a Point indicating the hex's upper-left corner
	 * on the buffer image.
	 */
	public Point getHexLocation(int x, int y) {
		return new Point(x * 63 - bb[cb].o.x, y * 72 + ((x & 1) == 1 ? 36 : 0) - bb[cb].o.y);
	}
	
	/**
	 * Returns a Point indicating the coordinate's upper-left corner
	 * on the board image.
	 */
	public Point getHexLocation(Coords c) {
		return getHexLocation(c.x, c.y);
	}
	
	/**
	 * Returns the image type index for the specified entity
	 */
	public int getEntityImageIndex(Entity en) {
		if (en instanceof Mech && en.getWeight() <= Mech.WEIGHT_LIGHT) {
			return PIC_MECH_LIGHT;
		}
		if (en instanceof Mech && en.getWeight() <= Mech.WEIGHT_MEDIUM) {
			return PIC_MECH_MEDIUM;
		}
		if (en instanceof Mech && en.getWeight() <= Mech.WEIGHT_HEAVY) {
			return PIC_MECH_HEAVY;
		}
		if (en instanceof Mech && en.getWeight() <= Mech.WEIGHT_ASSAULT) {
			return PIC_MECH_ASSAULT;
		}
		
		// else
		return -1;
	}
	
	/**
	 * Returns the image from the cache
	 */
	public Image getEntityImage(Entity en) {
		int type = getEntityImageIndex(en);
		
		// check cache for image
		if (tintCache[type][en.getSecondaryFacing()][en.getOwner().getColorIndex()] == null) {
			tintCache[type][en.getSecondaryFacing()][en.getOwner().getColorIndex()] = tint(imageCache[type][en.getSecondaryFacing()], en.getOwner().getColorRGB());
			//System.err.println("display: generating new image for cache");
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
		} catch(InterruptedException e) {
			System.err.println("graphics: interrupted waiting for pixels!");
		    return null;
		}
		if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
		    System.err.println("graphics: image fetch aborted or errored");
		    return null;
		}
		for(int j = 0; j < 72; j++) {
		    for(int i = 0; i < 84; i++) {
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
	 * If the mouse is at the edges of the screen, this
	 * scrolls the board image on the canvas.
	 */
	public void doScroll() {
		int sf = 3; // scroll factor
		// adjust x scroll
		// scroll when the mouse is at the edges
		if (mPos.x < 100) {
			scrollx -= (100 - mPos.x) / sf;
		}
		if (mPos.x > (bb[cb].s.width - 100)) {
			scrollx -= ((bb[cb].s.width - 100) - mPos.x) / sf;
		}
		// scroll when the mouse is at the edges
		if (mPos.y < 100) {
			scrolly -= (100 - mPos.y) / sf;
		}
		if (mPos.y > (bb[cb].s.height - 100)) {
			scrolly -= ((bb[cb].s.height - 100) - mPos.y) / sf;
		}
		checkScrollBounds();			
		repaint();
	}
	
	/**
	 * Makes sure that the scroll dimensions stay in bounds
	 */
	public void checkScrollBounds() {
		if (scrollx < 0) {
			scrollx = 0;
		}
		if (scrollx > (boardSize.width - bb[cb].s.width)) {
			scrollx = (boardSize.width - bb[cb].s.width);
		}

		if (scrolly < 0) {
			scrolly = 0;
		}
		if (scrolly > (boardSize.height - bb[cb].s.height)) {
			scrolly = (boardSize.height - bb[cb].s.height);
		}
	}
	
	//
	// Runnable
	//
	public void run() {
		final Thread currentThread = Thread.currentThread();
        long lastTick = System.currentTimeMillis();
		while (scroller == currentThread) {
			try {
				Thread.sleep(50);
			} catch(InterruptedException ex) {
				// duh?
			}
			if (bb[cb] != null) {
				doScroll();
			} else {
				repaint();
			}
            /*
            final long curTime = System.currentTimeMillis();
            if (curTime - lastTick > 5000) {
                System.out.println("BoardView: drew " + hexesDrawn + " hexes in " + (curTime - lastTick) + " ms.");
                hexesDrawn = 0;
                lastTick = curTime;
            }
            */
		}
	}
	
	//
	// BoardListener
	//
	public void boardHexMoused(BoardEvent b) {
		;
	}
	public void boardHexCursor(BoardEvent b) {
		Coords newcur = b.getCoords(), oldcur = cursor;
		cursorHex(newcur);
		if (oldcur != null) {
			redrawHex(bb[cb], oldcur);
		}
	}
	public void boardHexSelected(BoardEvent b) {
		Coords newsel = b.getCoords(), oldsel = selected;
		selectHex(newsel);
		if (oldsel != null) {
			redrawHex(bb[cb], oldsel);
		}
	}
	public void boardHexHighlighted(BoardEvent b) {
		Coords newhi = b.getCoords(), oldhi = highlighted;
		highlightHex(newhi);
		if (oldhi != null) {
			redrawHex(bb[cb], oldhi);
		}
	}
	public void boardChangedHex(BoardEvent b) {
		redrawHexFull(bb[cb], b.getCoords());
	}
	public void boardChangedEntity(BoardEvent b) {
		redrawHexFull(bb[cb], b.getCoords());
		drawOnscreenEntities(bb[cb]);
	}
	public void boardNewEntities(BoardEvent b) {
		if (imagesLoaded) {
			drawOnscreenEntities(bb[cb]);
		}
	}
	public void boardNewVis(BoardEvent b) {
		// nullify board image
		bb[cb] = null;
	}
	public void boardNewBoard(BoardEvent b) {
		bb[cb] = null;
		boardSize = new Dimension(board.width * 63 + 21, board.height * 72 + 36);
    //loadAllImages();
	}

	
	//
	// KeyListener
	//
	public void keyPressed(KeyEvent ke) {
		switch(ke.getKeyCode()) {
		case KeyEvent.VK_UP :
			scrolly -= 36;
			break;
		case KeyEvent.VK_DOWN :
			scrolly += 36;
			break;
		case KeyEvent.VK_LEFT :
			scrollx -= 36;
			break;
		case KeyEvent.VK_RIGHT :
			scrollx += 36;
			break;
		case KeyEvent.VK_B :
			busy = true;
			break;
		}
		checkScrollBounds();
		mPos = new Point(getSize().width / 2, getSize().height / 2);
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
		board.mouseAction(hexAt(me.getPoint()), Board.BOARD_HEX_DRAG, me.getModifiers()); 
		//board.drag(hexAt(me.getPoint()));
	}
	public void mouseReleased(MouseEvent me) {
		//System.out.println("moure click count: " + me.getClickCount());
		if (me.getClickCount() == 1) {
			board.mouseAction(hexAt(me.getPoint()), Board.BOARD_HEX_CLICK, me.getModifiers()); 
			//board.select(hexAt(me.getPoint()));
		} else {
			board.mouseAction(hexAt(me.getPoint()), Board.BOARD_HEX_DOUBLECLICK, me.getModifiers()); 
			//board.select(hexAt(me.getPoint()));
		}
		mPos = new Point(getSize().width / 2, getSize().height / 2);
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
		mPos = me.getPoint();
		board.mouseAction(hexAt(me.getPoint()), Board.BOARD_HEX_DRAG, me.getModifiers()); 
		//board.drag(hexAt(me.getPoint()));
	}
	public void mouseMoved(MouseEvent me) {
		//mPos = me.getPoint();
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
}

class BoardBuffer
{
	public Dimension			s;
	public Image				i;
	public Graphics				g;
    public Point				o;  //origin
	
	public BoardBuffer(Dimension size, Component c) {
		s = new Dimension(size);
		i = c.createImage(s.width, s.height);
		g = i.getGraphics();
	}
}
