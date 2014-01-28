/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007,2008 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.swing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneLayout;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import megamek.client.TimerSingleton;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.event.MechDisplayEvent;
import megamek.client.ui.IBoardView;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.ImageCache;
import megamek.client.ui.swing.util.ImprovedAveragingScaleFilter;
import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.client.ui.swing.util.StraightArrowPolygon;
import megamek.client.ui.swing.widget.MegamekBorder;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.Aero;
import megamek.common.ArtilleryTracker;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityMovementType;
import megamek.common.GunEmplacement;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IGame.Phase;
import megamek.common.IHex;
import megamek.common.IPlayer;
import megamek.common.ITerrain;
import megamek.common.Infantry;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.Minefield;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.PlanetaryConditions;
import megamek.common.Protomech;
import megamek.common.SpecialHexDisplay;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.UnitLocation;
import megamek.common.WeaponType;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.actions.ProtomechPhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.PushAttackAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.BoardEvent;
import megamek.common.event.BoardListener;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameNewActionEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.options.PilotOptions;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;

/**
 * Displays the board; lets the user scroll around and select points on it.
 */
public class BoardView1 extends JPanel implements IBoardView, Scrollable,
        BoardListener, MouseListener, IPreferenceChangeListener {

    private static final long serialVersionUID = -5582195884759007416L;

    private static final int TRANSPARENT = 0xFFFF00FF;

    private static final int BOARD_HEX_CLICK = 1;
    private static final int BOARD_HEX_DOUBLECLICK = 2;
    private static final int BOARD_HEX_DRAG = 3;
    private static final int BOARD_HEX_POPUP = 4;

    // the dimensions of megamek's hex images
    private static final int HEX_W = 84;
    private static final int HEX_H = 72;
    private static final int HEX_WC = HEX_W - (HEX_W / 4);
    private static final int HEX_ELEV = 12;
    

    private static final float[] ZOOM_FACTORS = { 0.30f, 0.41f, 0.50f, 0.60f,
            0.68f, 0.79f, 0.90f, 1.00f, 1.09f, 1.17f };

    // Set to TRUE to draw hexes with isometric elevation.
    private boolean drawIsometric = GUIPreferences.getInstance()
            .getIsometricEnabled();

    private int DROPSHDW_DIST = 20;

    // the index of zoom factor 1.00f
    private static final int BASE_ZOOM_INDEX = 7;

    // Initial zoom index
    public int zoomIndex = BASE_ZOOM_INDEX;

    // line width of the c3 network lines
    private static final int C3_LINE_WIDTH = 1;

    // line width of the fly over lines
    private static final int FLY_OVER_LINE_WIDTH = 3;

    private static Font FONT_7 = new Font("SansSerif", Font.PLAIN, 7); //$NON-NLS-1$
    private static Font FONT_8 = new Font("SansSerif", Font.PLAIN, 8); //$NON-NLS-1$
    private static Font FONT_9 = new Font("SansSerif", Font.PLAIN, 9); //$NON-NLS-1$
    private static Font FONT_10 = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
    private static Font FONT_12 = new Font("SansSerif", Font.PLAIN, 12); //$NON-NLS-1$

    Dimension hex_size = null;

    private Font font_note = FONT_10;
    private Font font_hexnum = FONT_10;
    private Font font_elev = FONT_9;
    private Font font_minefield = FONT_12;

    IGame game;

    private Dimension boardSize;

    // we keep track of the rectangle we drew, and keep a separate image of just
    // the hexes, so we don't have to paint each hex individually during each
    // redraw, but only when the visible hexes actually change
    private Rectangle drawRect;
    
    /**
     * An image representation of the board area used in non-isometric
     * rendering.  With isometric turned off, the image for the current view
     * is generated and then reused to render the board view.
     */
    private Image boardImage;
    
    /**
     * The graphics object of <code>boardImage</code>, used in non-isometric
     * rendering.
     */
    private Graphics boardGraph;
    
    private boolean redrawWholeBoard = false;

    // scrolly stuff:
    private JScrollPane scrollpane = null;
    private JScrollBar vbar;
    private JScrollBar hbar;
    private int scrollXDifference = 0;
    private int scrollYDifference = 0;
    // are we drag-scrolling?
    private boolean dragging = false;
    // should we scroll when the mouse is dragged?
    private boolean shouldScroll = false;

    // entity sprites
    private ArrayList<EntitySprite> entitySprites = new ArrayList<EntitySprite>();
    private ArrayList<IsometricSprite> isometricSprites = new ArrayList<IsometricSprite>();

    private HashMap<ArrayList<Integer>, EntitySprite> entitySpriteIds = new HashMap<ArrayList<Integer>, EntitySprite>();
    private HashMap<ArrayList<Integer>, IsometricSprite> isometricSpriteIds = new HashMap<ArrayList<Integer>, IsometricSprite>();

    // sprites for the three selection cursors
    private CursorSprite cursorSprite;
    private CursorSprite highlightSprite;
    private CursorSprite selectedSprite;
    private CursorSprite firstLOSSprite;
    private CursorSprite secondLOSSprite;

    // sprite for current movement
    private ArrayList<StepSprite> pathSprites = new ArrayList<StepSprite>();

    private ArrayList<FiringSolutionSprite> firingSprites = new ArrayList<FiringSolutionSprite>();

    private ArrayList<MovementEnvelopeSprite> moveEnvSprites = new ArrayList<MovementEnvelopeSprite>();

    // vector of sprites for all firing lines
    ArrayList<AttackSprite> attackSprites = new ArrayList<AttackSprite>();

    // vector of sprites for all movement paths (using vectored movement)
    private ArrayList<MovementSprite> movementSprites = new ArrayList<MovementSprite>();

    // vector of sprites for C3 network lines
    private ArrayList<C3Sprite> c3Sprites = new ArrayList<C3Sprite>();

    // vector of sprites for aero flyover lines
    private ArrayList<FlyOverSprite> flyOverSprites = new ArrayList<FlyOverSprite>();

    TilesetManager tileManager = null;

    // polygons for a few things
    Polygon hexPoly;
    Polygon[] facingPolys;
    Polygon[] movementPolys;

    // the player who owns this BoardView's client
    private IPlayer localPlayer = null;

    /**
     * Stores the currently deploying entity, used for highlighting deployment
     * hexes.
     */
    private Entity en_Deployer = null;

    // should be able to turn it off(board editor)
    private boolean useLOSTool = true;

    // Initial scale factor for sprites and map
    float scale = 1.00f;
    private ImageCache<Integer, Image> scaledImageCache = 
            new ImageCache<Integer, Image>();

    // Displayables (Chat box, etc.)
    ArrayList<IDisplayable> displayables = new ArrayList<IDisplayable>();

    // Move units step by step
    private ArrayList<MovingUnit> movingUnits = new ArrayList<MovingUnit>();

    private long moveWait = 0;

    // moving entity sprites
    private ArrayList<MovingEntitySprite> movingEntitySprites = new ArrayList<MovingEntitySprite>();
    private HashMap<Integer, MovingEntitySprite> movingEntitySpriteIds = new HashMap<Integer, MovingEntitySprite>();
    private ArrayList<GhostEntitySprite> ghostEntitySprites = new ArrayList<GhostEntitySprite>();

    protected transient ArrayList<BoardViewListener> boardListeners = new ArrayList<BoardViewListener>();

    // wreck sprites
    private ArrayList<WreckSprite> wreckSprites = new ArrayList<WreckSprite>();
    private ArrayList<IsometricWreckSprite> isometricWreckSprites = new ArrayList<IsometricWreckSprite>();

    private Coords rulerStart;
    private Coords rulerEnd;
    private Color rulerStartColor;
    private Color rulerEndColor;

    private Coords lastCursor;
    private Coords highlighted;
    private Coords selected;
    private Coords firstLOS;

    // selected entity and weapon for artillery display
    private Entity selectedEntity = null;
    private Mounted selectedWeapon = null;

    // hexes with ECM effect
    private HashMap<Coords, Integer> ecmHexes = null;

    // reference to our timertask for redraw
    private TimerTask ourTask = null;
    
    ImageIcon bvBgIcon = null;
    ImageIcon scrollPaneBgIcon = null;
    
    
    /**
     * Keeps track of whether we have an active ChatterBox2
     */
    public boolean chatterBoxActive = false;

    /**
     * Construct a new board view for the specified game
     */
    public BoardView1(final IGame game, final MegaMekController controller) 
    		throws java.io.IOException {
        this.game = game;
        
        tileManager = new TilesetManager(this);
        ToolTipManager.sharedInstance().registerComponent(this);

        game.addGameListener(gameListener);
        game.getBoard().addBoardListener(this);
        ourTask = scheduleRedrawTimer();// call only once
        addMouseListener(this);
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent we) {
                Point mousePoint = we.getPoint();
                for (int i = 0; i < displayables.size(); i++) {
                    IDisplayable disp = displayables.get(i);
                    if (!(disp instanceof ChatterBox2)) {
                        break;
                    }
                    double width = Math.min(boardSize.getWidth(), scrollpane
                            .getViewport().getSize().getWidth());
                    double height = Math.min(boardSize.getHeight(), scrollpane
                            .getViewport().getSize().getHeight());
                    Dimension drawDimension = new Dimension();
                    drawDimension.setSize(width, height);
                    // we need to adjust the point, because it should be against
                    // the displayable dimension
                    Point dispPoint = new Point();
                    dispPoint.setLocation(mousePoint.x + getBounds().x,
                            mousePoint.y + getBounds().y);
                    if (disp.isMouseOver(dispPoint, drawDimension)) {
                        ChatterBox2 cb2 = (ChatterBox2) disp;
                        if (we.getWheelRotation() > 0) {
                            cb2.scrollDown();
                        } else {
                            cb2.scrollUp();
                        }
                        refreshDisplayables();
                        return;
                    }
                }
                if (GUIPreferences.getInstance().getMouseWheelZoom()) {
                    boolean zoomIn = ((we.getWheelRotation() > 0) && !GUIPreferences
                            .getInstance().getMouseWheelZoomFlip())
                            || ((we.getWheelRotation() <= 0) && GUIPreferences
                                    .getInstance().getMouseWheelZoomFlip());
                    if (zoomIn) {
                        zoomIn();
                    } else {
                        zoomOut();
                    }
                } else {
                    if (we.isControlDown()) {
                        boolean zoomIn = ((we.getWheelRotation() > 0) && !GUIPreferences
                                .getInstance().getMouseWheelZoomFlip())
                                || ((we.getWheelRotation() <= 0) && GUIPreferences
                                        .getInstance().getMouseWheelZoomFlip());
                        if (zoomIn) {
                            zoomOut();
                        } else {
                            zoomIn();
                        }
                    } else if (we.isShiftDown()) {
                        int notches = we.getWheelRotation();
                        if (notches < 0) {
                            hbar.setValue((int) (hbar.getValue() - (HEX_H
                                    * scale * (-1 * notches))));

                        } else {
                            hbar.setValue((int) (hbar.getValue() + (HEX_H
                                    * scale * (notches))));
                        }
                    } else {
                        int notches = we.getWheelRotation();
                        if (notches < 0) {
                            vbar.setValue((int) (vbar.getValue() - (HEX_H
                                    * scale * (-1 * notches))));

                        } else {
                            vbar.setValue((int) (vbar.getValue() + (HEX_H
                                    * scale * (notches))));
                        }
                    }
                }

            }
        });

        final BoardView1 bv = this;
        // Register the action for TOGGLE_CHAT
        controller.registerCommandAction(KeyCommandBind.TOGGLE_CHAT.cmd,
        		new CommandAction(){

        			@Override
        			public boolean shouldPerformAction(){
        				if (chatterBoxActive || !bv.isVisible()
        						|| game.getPhase() == Phase.PHASE_LOUNGE){
        					return false;
        				} else {
        					return true;
        				}
        			}
        			
					@Override
					public void performAction() {
						if (!chatterBoxActive){
							chatterBoxActive = true;
							for (IDisplayable disp : displayables){
			            		if (disp instanceof ChatterBox2){
			            			((ChatterBox2)disp).slideUp();
	            				}
		            		}
							requestFocus();
						}
					}
        	
        });
        
        // Register the action for TOGGLE_CHAT
        controller.registerCommandAction(KeyCommandBind.TOGGLE_CHAT_CMD.cmd,
        		new CommandAction(){

        			@Override
        			public boolean shouldPerformAction(){
        				if (chatterBoxActive || !bv.isVisible()
        						|| game.getPhase() == Phase.PHASE_LOUNGE){
        					return false;
        				} else {
        					return true;
        				}
        			}
        			
					@Override
					public void performAction() {
						if (!chatterBoxActive){
							chatterBoxActive = true;
							for (IDisplayable disp : displayables){
			            		if (disp instanceof ChatterBox2){
			            			((ChatterBox2)disp).slideUp();
			            			((ChatterBox2)disp).setMessage("/");
	            				}
		            		}
							requestFocus();
						}
					}
        	
        });        

        // Register the action for CENTER_ON_SELECTED        
        controller.registerCommandAction(KeyCommandBind.CENTER_ON_SELECTED.cmd,
        		new CommandAction(){

					@Override
					public boolean shouldPerformAction(){
        				if (chatterBoxActive || !bv.isVisible()
        						|| game.getPhase() == Phase.PHASE_LOUNGE
        						|| selectedEntity == null){
        					return false;
        				} else {
        					return true;
        				}
					}        	
        	
					@Override
					public void performAction() {
						if (selectedEntity != null) {
		                    centerOnHex(selectedEntity.getPosition());
		                }
					}
        	
        });
        
        // Register the action for SCROLL_NORTH        
        controller.registerCommandAction(KeyCommandBind.SCROLL_NORTH.cmd,
        		new CommandAction(){

					@Override
					public boolean shouldPerformAction(){
        				if (chatterBoxActive || !bv.isVisible()
        						|| game.getPhase() == Phase.PHASE_LOUNGE){
							return false;
						} else {
							return true;
						}
					}    
					
					@Override
					public void performAction() {
						controller.stopRepeating(KeyCommandBind.SCROLL_SOUTH);
						vbar.setValue((int)
                   			 (vbar.getValue() - (HEX_H * scale)));
					}
        	
        });
        
        // Register the action for SCROLL_SOUTH        
        controller.registerCommandAction(KeyCommandBind.SCROLL_SOUTH.cmd,
        		new CommandAction(){

					@Override
					public boolean shouldPerformAction(){
        				if (chatterBoxActive || !bv.isVisible()
        						|| game.getPhase() == Phase.PHASE_LOUNGE){
							return false;
						} else {
							return true;
						}
					}           	
        	
					@Override
					public void performAction() {
						controller.stopRepeating(KeyCommandBind.SCROLL_NORTH);
						vbar.setValue((int)
                   			 (vbar.getValue() + (HEX_H * scale)));
					}
        	
        });
        
        // Register the action for SCROLL_EAST        
        controller.registerCommandAction(KeyCommandBind.SCROLL_EAST.cmd,
        		new CommandAction(){

		        	@Override
					public boolean shouldPerformAction(){
        				if (chatterBoxActive || !bv.isVisible()
        						|| game.getPhase() == Phase.PHASE_LOUNGE){
							return false;
						} else {
							return true;
						}
					} 
        	
					@Override
					public void performAction() {
						controller.stopRepeating(KeyCommandBind.SCROLL_WEST);
						hbar.setValue((int) 
                    			(hbar.getValue() + (HEX_W * scale)));
					}
        	
        });
        
        // Register the action for SCROLL_WEST        
        controller.registerCommandAction(KeyCommandBind.SCROLL_WEST.cmd,
        		new CommandAction(){

		        	@Override
					public boolean shouldPerformAction(){
        				if (chatterBoxActive || !bv.isVisible()
        						|| game.getPhase() == Phase.PHASE_LOUNGE){
							return false;
						} else {
							return true;
						}
					}         	
        	
					@Override
					public void performAction() {
						controller.stopRepeating(KeyCommandBind.SCROLL_EAST);
						hbar.setValue((int) 
                    			(hbar.getValue() - (HEX_W * scale)));
					}
        	
        });        

        
        MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point point = e.getPoint();
                if (null == point) {
                    return;
                }

                for (int i = 0; i < displayables.size(); i++) {
                    IDisplayable disp = displayables.get(i);
                    if (disp.isBeingDragged()) {
                        return;
                    }
                    double width = Math.min(boardSize.getWidth(), scrollpane
                            .getViewport().getSize().getWidth());
                    double height = Math.min(boardSize.getHeight(), scrollpane
                            .getViewport().getSize().getHeight());
                    Dimension drawDimension = new Dimension();
                    drawDimension.setSize(width, height);
                    disp.isMouseOver(point, drawDimension);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point point = e.getPoint();
                if (null == point) {
                    return;
                }
                for (int i = 0; i < displayables.size(); i++) {
                    IDisplayable disp = displayables.get(i);
                    Point adjustPoint = new Point((int) Math.min(
                            boardSize.getWidth(), -getBounds().getX()),
                            (int) Math.min(boardSize.getHeight(), -getBounds()
                                    .getY()));
                    Point dispPoint = new Point();
                    dispPoint.x = point.x - adjustPoint.x;
                    dispPoint.y = point.y - adjustPoint.y;
                    double width = Math.min(boardSize.getWidth(), scrollpane
                            .getViewport().getSize().getWidth());
                    double height = Math.min(boardSize.getHeight(), scrollpane
                            .getViewport().getSize().getHeight());
                    Dimension drawDimension = new Dimension();
                    drawDimension.setSize(width, height);
                    if (disp.isDragged(dispPoint, drawDimension)) {
                        repaint();
                        return;
                    }
                }
                // only scroll when we should
                if (!shouldScroll) {
                    mouseAction(getCoordsAt(point), BOARD_HEX_DRAG,
                            e.getModifiers());
                    return;
                }
                // if we have not yet been dragging, set the var so popups don't
                // appear when we stop scrolling
                if (!dragging) {
                    dragging = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                Point p = scrollpane.getViewport().getViewPosition();
                int newX = p.x - (e.getX() - scrollXDifference);
                int newY = p.y - (e.getY() - scrollYDifference);
                int maxX = getWidth() - scrollpane.getViewport().getWidth();
                int maxY = getHeight() - scrollpane.getViewport().getHeight();
                if (newX < 0) {
                    newX = 0;
                }
                if (newX > maxX) {
                    newX = maxX;
                }
                if (newY < 0) {
                    newY = 0;
                }
                if (newY > maxY) {
                    newY = maxY;
                }
                // don't scroll horizontally if the board fits into the window
                if (scrollpane.getViewport().getWidth() >= getWidth()) {
                    newX = scrollpane.getViewport().getViewPosition().x;
                }
                scrollpane.getViewport().setViewPosition(new Point(newX, newY));
            }
        };
        addMouseMotionListener(mouseMotionListener);

        // setAutoscrolls(true);

        updateBoardSize();

        hex_size = new Dimension((int) (HEX_W * scale), (int) (HEX_H * scale));

        initPolys();

        cursorSprite = new CursorSprite(Color.cyan);
        highlightSprite = new CursorSprite(Color.white);
        selectedSprite = new CursorSprite(Color.blue);
        firstLOSSprite = new CursorSprite(Color.red);
        secondLOSSprite = new CursorSprite(Color.red);

        PreferenceManager.getClientPreferences().addPreferenceChangeListener(
                this);

        SpecialHexDisplay.Type.ARTILLERY_HIT.init(getToolkit());
        SpecialHexDisplay.Type.ARTILLERY_INCOMING.init(getToolkit());
        SpecialHexDisplay.Type.ARTILLERY_TARGET.init(getToolkit());
        SpecialHexDisplay.Type.ARTILLERY_ADJUSTED.init(getToolkit());
        SpecialHexDisplay.Type.ARTILLERY_AUTOHIT.init(getToolkit());
    }

    protected final RedrawWorker redrawWorker = new RedrawWorker();

    /**
     * this should only be called once!! this will cause a timer to schedule
     * constant screen updates every 20 milliseconds!
     */
    protected TimerTask scheduleRedrawTimer() {
        final TimerTask redraw = new TimerTask() {
            @Override
            public void run() {
                try {
                    SwingUtilities.invokeLater(redrawWorker);
                } catch (Exception ie) {
                    System.err.print("Error scheduleRedrawTimer "); //$NON-NLS-1$
                    System.err.print(ie.getMessage());
                    System.err.print(": "); //$NON-NLS-1$
                    System.err.println("ignoring");
                }
            }
        };
        TimerSingleton.getInstance().schedule(redraw, 20, 20);
        return redraw;
    }

    protected void scheduleRedraw() {
        try {
            SwingUtilities.invokeLater(redrawWorker);
        } catch (Exception ie) {
            System.err.print("Error scheduleRedraw "); //$NON-NLS-1$
            System.err.print(ie.getMessage());
            System.err.print(": "); //$NON-NLS-1$
            System.err.println("ignoring");
        }
    }

    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(IClientPreferences.MAP_TILESET)) {
            updateBoard();
        }
    }

    /**
     * Adds the specified board listener to receive board events from this
     * board.
     * 
     * @param listener
     *            the board listener.
     */
    public void addBoardViewListener(BoardViewListener listener) {
        if (!boardListeners.contains(listener)) {
            boardListeners.add(listener);
        }
    }

    /**
     * Removes the specified board listener.
     * 
     * @param listener
     *            the board listener.
     */
    public void removeBoardViewListener(BoardViewListener listener) {
        boardListeners.remove(listener);
    }

    /**
     * Notifies attached board listeners of the event.
     * 
     * @param event
     *            the board event.
     */
    public void processBoardViewEvent(BoardViewEvent event) {
        if (boardListeners == null) {
            return;
        }
        for (BoardViewListener l : boardListeners) {
            switch (event.getType()) {
                case BoardViewEvent.BOARD_HEX_CLICKED:
                case BoardViewEvent.BOARD_HEX_DOUBLECLICKED:
                case BoardViewEvent.BOARD_HEX_DRAGGED:
                case BoardViewEvent.BOARD_HEX_POPUP:
                    l.hexMoused(event);
                    break;
                case BoardViewEvent.BOARD_HEX_CURSOR:
                    l.hexCursor(event);
                    break;
                case BoardViewEvent.BOARD_HEX_HIGHLIGHTED:
                    l.boardHexHighlighted(event);
                    break;
                case BoardViewEvent.BOARD_HEX_SELECTED:
                    l.hexSelected(event);
                    break;
                case BoardViewEvent.BOARD_FIRST_LOS_HEX:
                    l.firstLOSHex(event);
                    break;
                case BoardViewEvent.BOARD_SECOND_LOS_HEX:
                    l.secondLOSHex(event, getFirstLOS());
                    break;
                case BoardViewEvent.FINISHED_MOVING_UNITS:
                    l.finishedMovingUnits(event);
                    break;
                case BoardViewEvent.SELECT_UNIT:
                    l.unitSelected(event);
                    break;
            }
        }
    }

    void addMovingUnit(Entity entity, Vector<UnitLocation> movePath) {
        if (!movePath.isEmpty()) {
            MovingUnit m = new MovingUnit(entity, movePath);
            movingUnits.add(m);

            GhostEntitySprite ghostSprite = new GhostEntitySprite(entity);
            ghostEntitySprites.add(ghostSprite);

            // Center on the starting hex of the moving unit.
            UnitLocation loc = movePath.get(0);
            centerOnHex(loc.getCoords());
        }
    }

    public void addDisplayable(IDisplayable disp) {
        displayables.add(disp);
    }

    public void removeDisplayable(IDisplayable disp) {
        displayables.remove(disp);
    }

    /**
     * Draw the screen!
     */
    @Override
    public synchronized void paintComponent(Graphics g) {
    	
        if (!isTileImagesLoaded()) {
            g.drawString(Messages.getString("BoardView1.loadingImages"), 20, 50); //$NON-NLS-1$
            if (!tileManager.isStarted()) {
                System.out.println("boardview1: loading images for board"); //$NON-NLS-1$
                tileManager.loadNeededImages(game);
            }
            // wait 1 second, then repaint
            repaint(1000);
            return;
        }
        
        if (bvBgIcon != null){
	        int w = getWidth();
	        int h = getHeight();
	        int iW = bvBgIcon.getIconWidth();
	        int iH = bvBgIcon.getIconHeight();
	        for (int x = g.getClipBounds().x; x < w; x+=iW){
	            for (int y = g.getClipBounds().y; y < h; y+=iH){
	                g.drawImage(bvBgIcon.getImage(), x, y, 
	                        bvBgIcon.getImageObserver());
	            }
        }
        }

        if (useIsometric()) {
            drawHexes(g, g.getClipBounds());
        } else {
            updateBoardImage();
            g.drawImage(boardImage,
                    scrollpane.getViewport().getViewPosition().x, scrollpane
                            .getViewport().getViewPosition().y, this);
        }

        // draw wrecks
        if (GUIPreferences.getInstance().getShowWrecks() && !useIsometric()) {
            drawSprites(g, wreckSprites);
        }

        if ((game.getPhase() == Phase.PHASE_MOVEMENT) && !useIsometric()) {
            drawSprites(g, moveEnvSprites);
        }

        // Minefield signs all over the place!
        drawMinefields(g);

        // Artillery targets
        drawArtilleryHexes(g);

        // draw highlight border
        drawSprite(g, highlightSprite);

        // draw cursors
        drawSprite(g, cursorSprite);
        drawSprite(g, selectedSprite);
        drawSprite(g, firstLOSSprite);
        drawSprite(g, secondLOSSprite);

        // draw deployment indicators.
        // For Isometric rendering, this is done during drawHexes
        if ((en_Deployer != null) && !useIsometric()) {
            drawDeployment(g);
        }

        // draw C3 links
        drawSprites(g, c3Sprites);

        // draw flyover routes
        if (game.getBoard().onGround()) {
            drawSprites(g, flyOverSprites);
        }

        // draw onscreen entities
        drawSprites(g, entitySprites);

        // draw moving onscreen entities
        drawSprites(g, movingEntitySprites);

        // draw ghost onscreen entities
        drawSprites(g, ghostEntitySprites);

        // draw onscreen attacks
        drawSprites(g, attackSprites);

        // draw movement vectors.
        if (game.useVectorMove()
                && (game.getPhase() == IGame.Phase.PHASE_MOVEMENT)) {
            drawSprites(g, movementSprites);
        }

        // draw movement, if valid
        drawSprites(g, pathSprites);

        // draw firing solution sprites, but only during the firing phase
        if (game.getPhase() == Phase.PHASE_FIRING ||
                game.getPhase() == Phase.PHASE_OFFBOARD) {
            drawSprites(g, firingSprites);
        }

        // draw the ruler line
        if (rulerStart != null) {
            Point start = getCentreHexLocation(rulerStart);
            if (rulerEnd != null) {
                Point end = getCentreHexLocation(rulerEnd);
                g.setColor(Color.yellow);
                g.drawLine(start.x, start.y, end.x, end.y);

                g.setColor(rulerEndColor);
                g.fillRect(end.x - 1, end.y - 1, 2, 2);
            }

            g.setColor(rulerStartColor);
            g.fillRect(start.x - 1, start.y - 1, 2, 2);
        }
        // draw all the "displayables"
        Rectangle rect = new Rectangle();
        rect.x = -getX();
        rect.y = -getY();
        rect.width = Math.min(scrollpane.getViewport().getViewRect().width,
                boardSize.width);
        rect.height = Math.min(scrollpane.getViewport().getViewRect().height,
                boardSize.height);
        for (int i = 0; i < displayables.size(); i++) {
            IDisplayable disp = displayables.get(i);
            disp.draw(g, rect);
        }
    }

    /**
     * Updates the boardSize variable with the proper values for this board.
     */
    private void updateBoardSize() {
        int width = (game.getBoard().getWidth() * (int) (HEX_WC * scale))
                + (int) ((HEX_W / 4) * scale);
        int height = (game.getBoard().getHeight() * (int) (HEX_H * scale))
                + (int) ((HEX_H / 2) * scale);
        boardSize = new Dimension(width, height);
    }

    /**
     * Looks through a vector of buffered images and draws them if they're
     * onscreen.
     */
    private synchronized void drawSprites(Graphics g,
            ArrayList<? extends Sprite> spriteArrayList) {
        for (Sprite sprite : spriteArrayList) {
            drawSprite(g, sprite);
        }
    }

    private synchronized void drawMovementEnvelopeSpritesForHex(Coords c,
            Graphics g, ArrayList<MovementEnvelopeSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (MovementEnvelopeSprite sprite : spriteArrayList) {
            Coords cp = sprite.getPosition();
            if (cp.equals(c) && view.intersects(sprite.getBounds())
                    && !sprite.hidden) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, sprite.getBounds().x, sprite.getBounds().y,
                        this, false);
            }
        }
    }

    /**
     * Draws the Entity for the given hex. This function is used by the
     * isometric rendering process so that sprites are drawn in the order that
     * hills are rendered to create the appearance that the sprite is behind the
     * hill.
     * 
     * @param c
     *            The Coordinates of the hex that the sprites should be drawn
     *            for.
     * @param g
     *            The Graphics object for this board.
     * @param spriteArrayList
     *            The complete list of all IsometricSprite on the board.
     */
    private synchronized void drawIsometricSpritesForHex(Coords c, Graphics g,
            ArrayList<IsometricSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (IsometricSprite sprite : spriteArrayList) {
            Coords cp = sprite.getPosition();
            if (cp.equals(c) && view.intersects(sprite.getBounds())
                    && !sprite.hidden) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, sprite.getBounds().x, sprite.getBounds().y,
                        this, false);
            }
        }
    }

    /**
     * Draws the wrecksprites for the given hex. This function is used by the
     * isometric rendering process so that sprites are drawn in the order that
     * hills are rendered to create the appearance that the sprite is behind the
     * hill.
     * 
     * @param c
     *            The Coordinates of the hex that the sprites should be drawn
     *            for.
     * @param g
     *            The Graphics object for this board.
     * @param spriteArrayList
     *            The complete list of all IsometricSprite on the board.
     */
    private synchronized void drawIsometricWreckSpritesForHex(Coords c,
            Graphics g, ArrayList<IsometricWreckSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (IsometricWreckSprite sprite : spriteArrayList) {
            Coords cp = sprite.getEntity().getPosition();
            if (cp.equals(c) && view.intersects(sprite.getBounds())
                    && !sprite.hidden) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, sprite.getBounds().x, sprite.getBounds().y,
                        this, false);
            }
        }
    }

    /**
     * Draws a translucent sprite without any of the companion graphics, if it
     * is in the current view. This is used only when performing isometric
     * rending. This function is used to show units (with 50% transparency) that
     * are hidden behind a hill.
     * 
     * TODO: Optimize this function so that it is only applied to sprites that
     * are actually hidden. This implementation performs the second rendering
     * for all sprites.
     */
    private final void drawIsometricSprites(Graphics g,
            ArrayList<IsometricSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (IsometricSprite sprite : spriteArrayList) {
            if (view.intersects(sprite.getBounds()) && !sprite.hidden) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, sprite.getBounds().x, sprite.getBounds().y,
                        this, true);
            }
        }
    }

    /**
     * Draws a sprite, if it is in the current view
     */
    private final void drawSprite(Graphics g, Sprite sprite) {
        Rectangle view = g.getClipBounds();
        if (view.intersects(sprite.getBounds()) && !sprite.hidden) {
            if (!sprite.isReady()) {
                sprite.prepare();
            }
            sprite.drawOnto(g, sprite.getBounds().x, sprite.getBounds().y, this);
        }
    }

    /**
     * Draw an outline around legal deployment hexes
     */
    private void drawDeployment(Graphics g) {
        Rectangle view = g.getClipBounds();
        // only update visible hexes
        int drawX = (view.x / (int) (HEX_WC * scale)) - 1;
        int drawY = (view.y / (int) (HEX_H * scale)) - 1;

        int drawWidth = (view.width / (int) (HEX_WC * scale)) + 3;
        int drawHeight = (view.height / (int) (HEX_H * scale)) + 3;

        IBoard board = game.getBoard();
        // loop through the hexes
        for (int i = 0; i < drawHeight; i++) {
            for (int j = 0; j < drawWidth; j++) {
                Coords c = new Coords(j + drawX, i + drawY);
                if (board.isLegalDeployment(c, en_Deployer.getStartingPos())) {
                    drawHexBorder(getHexLocation(c), g, Color.yellow);
                }
            }
        }
    }

    /**
     * Darkens a hexes in the viewing area if there is no line of sight between
     * them and the supplied source hex. Used in non-isometric view.
     * 
     * @param src
     *            The source hex for which line of sight originates
     * @param g
     *            The graphics object to draw on. / private void drawLos(Coords
     *            src, Graphics g) { Rectangle view = g.getClipBounds(); // only
     *            update visible hexes int drawX = (view.x / (int) (HEX_WC *
     *            scale)) - 1; int drawY = (view.y / (int) (HEX_H * scale)) - 1;
     * 
     *            int drawWidth = (view.width / (int) (HEX_WC * scale)) + 3; int
     *            drawHeight = (view.height / (int) (HEX_H * scale)) + 3;
     * 
     *            // loop through the hexes for (int i = 0; i < drawHeight; i++)
     *            { for (int j = 0; j < drawWidth; j++) { Coords c = new
     *            Coords(j + drawX, i + drawY); if
     *            (game.getBoard().contains(c)){ drawLos(src,c,g); } } } }
     * 
     * 
     *            /** Darkens a destination hex if there is no line of sight
     *            between it and a source hex.
     * 
     * @param src
     *            The source hex for which line of sight originates
     * @param dest
     *            The destination hex for computing the line of sight
     * @param g
     *            The graphics object to draw on. / private void drawLos(Coords
     *            src, Coords dest, Graphics g){ int max_dist; if (src == null
     *            || dest == null){ return; } if (selectedEntity != null){
     *            max_dist = game.getPlanetaryConditions().
     *            getVisualRange(selectedEntity, false); } else { max_dist = 30;
     *            } int dist = src.distance(dest); Color transparent_gray = new
     *            Color(0,0,0,100); if (dist <= max_dist && !(getLosEffects(src,
     *            dest).canSee())){ drawHexLayer(new Point(dest.x,dest.y), g,
     *            transparent_gray); } }
     */

    /**
     * Draw a layer of a solid color (alpha possible) on the hex at Point p no
     * padding by default
     */
    private void drawHexLayer(Point p, Graphics g, Color col) {
        drawHexLayer(p, g, col, 0);
    }

    /**
     * Draw a layer of a solid color (alpha possible) on the hex at Point p with
     * some padding around the border
     */
    private void drawHexLayer(Point p, Graphics g, Color col, double pad) {
        g.setColor(col);

        final double[] x = { 0, 21. * scale, 62. * scale, 83. * scale };
        final double[] y = { 0, 35. * scale, 36. * scale, 71. * scale };
        g.setColor(col);

        if (pad > 0.1) {
            final double cos60 = 0.5;
            final double cos30 = 0.8660254;

            final double pd = pad * scale;
            final double a = cos60 * pad * scale;
            final double b = cos30 * pad * scale;

            int[] xcoords = { p.x + (int) (x[1] + a), p.x + (int) (x[2] - a),
                    p.x + (int) (x[3] - pd), p.x + (int) (x[3] - pd),
                    p.x + (int) (x[2] - a), p.x + (int) (x[1] + a),
                    p.x + (int) (x[0] + pd), p.x + (int) (x[0] + pd) };
            int[] ycoords = { p.y + (int) (y[0] + b), p.y + (int) (y[0] + b),
                    p.y + (int) (y[1]), p.y + (int) (y[2]),
                    p.y + (int) (y[3] - b), p.y + (int) (y[3] - b),
                    p.y + (int) (y[2]), p.y + (int) (y[1]) };

            g.fillPolygon(xcoords, ycoords, 8);

        } else {

            int[] xcoords = { p.x + (int) (x[1]), p.x + (int) (x[2]),
                    p.x + (int) (x[3]), p.x + (int) (x[3]), p.x + (int) (x[2]),
                    p.x + (int) (x[1]), p.x + (int) (x[0]), p.x + (int) (x[0]) };
            int[] ycoords = { p.y + (int) (y[0]), p.y + (int) (y[0]),
                    p.y + (int) (y[1]), p.y + (int) (y[2]), p.y + (int) (y[3]),
                    p.y + (int) (y[3]), p.y + (int) (y[2]), p.y + (int) (y[1]) };

            g.fillPolygon(xcoords, ycoords, 8);
        }
    }

    /**
     * Draw an outline around the hex at Point p no padding and a width of 1
     */
    private void drawHexBorder(Point p, Graphics g, Color col) {
        drawHexBorder(p, g, col, 0);
    }

    /**
     * Draw an outline around the hex at Point p padded around the border by pad
     * and a line-width of 1
     */
    private void drawHexBorder(Point p, Graphics g, Color col, double pad) {
        drawHexBorder(p, g, col, pad, 1);
    }

    /**
     * Draw a thick outline around the hex at Coords c padded around the border
     * by pad and a line-width of linewidth
     */
    private void drawHexBorder(Point p, Graphics g, Color col, double pad,
            double linewidth) {

        g.setColor(col);

        final double[] x = { 0, 21. * scale, 62. * scale, 83. * scale };
        final double[] y = { 0, 35. * scale, 36. * scale, 71. * scale };

        final double cos60 = 0.5;
        final double cos30 = 0.8660254;

        final double pd = pad * scale;
        final double a = cos60 * pd;
        final double b = cos30 * pd;

        if (linewidth < 1.5) {

            int[] xcoords = { p.x + (int) (x[1] + a), p.x + (int) (x[2] - a),
                    p.x + (int) (x[3] - pd), p.x + (int) (x[3] - pd),
                    p.x + (int) (x[2] - a), p.x + (int) (x[1] + a),
                    p.x + (int) (x[0] + pd), p.x + (int) (x[0] + pd) };
            int[] ycoords = { p.y + (int) (y[0] + b), p.y + (int) (y[0] + b),
                    p.y + (int) (y[1]), p.y + (int) (y[2]),
                    p.y + (int) (y[3] - b), p.y + (int) (y[3] - b),
                    p.y + (int) (y[2]), p.y + (int) (y[1]) };

            g.drawPolygon(xcoords, ycoords, 8);

        } else {

            final double pl = (pad * scale) + (linewidth * scale);
            final double c = cos60 * pl;
            final double d = cos30 * pl;

            int[] xcoords = { p.x + (int) (x[1] + a), p.x + (int) (x[2] - a),
                    p.x + (int) (x[3] - pd), p.x + (int) (x[3] - pd),
                    p.x + (int) (x[2] - a), p.x + (int) (x[1] + a),
                    p.x + (int) (x[0] + pd), p.x + (int) (x[0] + pd),
                    p.x + (int) (x[1] + a), p.x + (int) (x[1] + c),
                    p.x + (int) (x[0] + pl), p.x + (int) (x[0] + pl),
                    p.x + (int) (x[1] + c), p.x + (int) (x[2] - c),
                    p.x + (int) (x[3] - pl), p.x + (int) (x[3] - pl),
                    p.x + (int) (x[2] - c), p.x + (int) (x[1] + c) };
            int[] ycoords = { p.y + (int) (y[0] + b), p.y + (int) (y[0] + b),
                    p.y + (int) (y[1]), p.y + (int) (y[2]),
                    p.y + (int) (y[3] - b), p.y + (int) (y[3] - b),
                    p.y + (int) (y[2]), p.y + (int) (y[1]),
                    p.y + (int) (y[0] + b), p.y + (int) (y[0] + d),
                    p.y + (int) (y[1]), p.y + (int) (y[2]),
                    p.y + (int) (y[3] - d), p.y + (int) (y[3] - d),
                    p.y + (int) (y[2]), p.y + (int) (y[1]),
                    p.y + (int) (y[0] + d), p.y + (int) (y[0] + d) };

            g.fillPolygon(xcoords, ycoords, 18);
        }
    }

    /**
     * returns the weapon selected in the mech display, or null if none selected
     * or it is not artillery or null if the selected entity is not owned
     */
    private Mounted getSelectedArtilleryWeapon() {
        // We don't want to display artillery auto-hit/adjusted fire hexes
        // during
        // the artyautohithexes phase. These could be displayed if the player
        // uses the /reset command in some situations
        if (game.getPhase() == IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES) {
            return null;
        }

        if ((selectedEntity == null) || (selectedWeapon == null)) {
            return null;
        }

        if (!selectedEntity.getOwner().equals(localPlayer)) {
            return null; // Not my business to see this
        }

        if (selectedEntity.getEquipmentNum(selectedWeapon) == -1) {
            return null; // inconsistent state - weapon not on entity
        }

        if (!((selectedWeapon.getType() instanceof WeaponType) && selectedWeapon
                .getType().hasFlag(WeaponType.F_ARTILLERY))) {
            return null; // not artillery
        }

        // otherwise, a weapon is selected, and it is artillery
        return selectedWeapon;
    }

    /**
     * Display artillery modifier in pretargeted hexes
     */
    private void drawArtilleryHexes(Graphics g) {
        Mounted weapon = getSelectedArtilleryWeapon();
        Rectangle view = g.getClipBounds();

        // Compute the origin of the viewing area
        int drawX = (view.x / (int) (HEX_WC * scale)) - 1;
        int drawY = (view.y / (int) (HEX_H * scale)) - 1;

        // Compute size of viewing area
        int drawWidth = (view.width / (int) (HEX_WC * scale)) + 3;
        int drawHeight = (view.height / (int) (HEX_H * scale)) + 3;

        Image scaledImage;

        // Draw incoming artillery sprites - requires server to update client's
        // view of game
        for (Enumeration<ArtilleryAttackAction> attacks = game
                .getArtilleryAttacks(); attacks.hasMoreElements();) {
            ArtilleryAttackAction a = attacks.nextElement();
            Coords c = a.getTarget(game).getPosition();
            // Is the Coord within the viewing area?
            if ((c.x >= drawX) && (c.x <= (drawX + drawWidth))
                    && (c.y >= drawY) && (c.y <= (drawY + drawHeight))) {

                Point p = getHexLocation(c);
                scaledImage = tileManager
                        .getArtilleryTarget(TilesetManager.ARTILLERY_INCOMING);
                g.drawImage(scaledImage, p.x, p.y, this);
            }
        }

        // Draw pre-designated auto-hit hexes
        if (localPlayer != null) // Could be null, like in map-editor
        {
            for (Coords c : localPlayer.getArtyAutoHitHexes()) {
                // Is the Coord within the viewing area?
                if ((c.x >= drawX) && (c.x <= (drawX + drawWidth))
                        && (c.y >= drawY) && (c.y <= (drawY + drawHeight))) {

                    Point p = getHexLocation(c);
                    scaledImage = tileManager
                            .getArtilleryTarget(TilesetManager.ARTILLERY_AUTOHIT);
                    g.drawImage(scaledImage, p.x, p.y, this);
                }
            }
        }

        // Draw modifiers for selected entity and weapon
        if (weapon != null) {
            // Loop through all of the attack modifiers for this weapon
            for (ArtilleryTracker.ArtilleryModifier attackMod : selectedEntity.aTracker
                    .getWeaponModifiers(weapon)) {
                Coords c = attackMod.getCoords();
                // Is the Coord within the viewing area?
                if ((c.x >= drawX) && (c.x <= (drawX + drawWidth))
                        && (c.y >= drawY) && (c.y <= (drawY + drawHeight))) {

                    Point p = getHexLocation(c);
                    // draw the crosshairs
                    if (attackMod.getModifier() == TargetRoll.AUTOMATIC_SUCCESS) {
                        // predesignated or already hit
                        scaledImage = tileManager
                                .getArtilleryTarget(TilesetManager.ARTILLERY_AUTOHIT);
                    } else {
                        scaledImage = tileManager
                                .getArtilleryTarget(TilesetManager.ARTILLERY_ADJUSTED);
                    }
                    g.drawImage(scaledImage, p.x, p.y, this);
                }
            }
        }
    }

    /*
     * NOTENOTENOTE: (itmo) wouldnt this be simpler with two arrays. One with
     * the strings {"BoardView1.thunderblaablaa","BoardView1.Conventi.."} one
     * with the offsets {51,51,42} etc Preferably indexed by an enum: enum{
     * Conventional, Thunder; } or something?
     */
    /**
     * Writes "MINEFIELD" in minefield hexes...
     */
    private void drawMinefields(Graphics g) {
        Rectangle view = g.getClipBounds();
        // only update visible hexes
        int drawX = (view.x / (int) (HEX_WC * scale)) - 1;
        int drawY = (view.y / (int) (HEX_H * scale)) - 1;

        int drawWidth = (view.width / (int) (HEX_WC * scale)) + 3;
        int drawHeight = (view.height / (int) (HEX_H * scale)) + 3;

        IBoard board = game.getBoard();
        // loop through the hexes
        for (int i = 0; i < drawHeight; i++) {
            for (int j = 0; j < drawWidth; j++) {
                Coords c = new Coords(j + drawX, i + drawY);
                Point p = getHexLocation(c);

                if (!board.contains(c)) {
                    continue;
                }
                if (!game.containsMinefield(c)) {
                    continue;
                }

                Minefield mf = game.getMinefields(c).get(0);

                Image tmpImage = getScaledImage(tileManager.getMinefieldSign(),true);
                g.drawImage(tmpImage, p.x + (int) (13 * scale), p.y
                        + (int) (13 * scale), this);

                g.setColor(Color.black);
                int nbrMfs = game.getNbrMinefields(c);
                if (nbrMfs > 1) {
                    drawCenteredString(
                            Messages.getString("BoardView1.Multiple"), //$NON-NLS-1$
                            p.x, p.y + (int) (51 * scale), font_minefield, g);
                } else if (nbrMfs == 1) {
                    switch (mf.getType()) {
                        case (Minefield.TYPE_CONVENTIONAL):
                            drawCenteredString(
                                    Messages.getString("BoardView1.Conventional") + mf.getDensity() + ")", //$NON-NLS-1$
                                    p.x, p.y + (int) (51 * scale),
                                    font_minefield, g);
                            break;
                        case (Minefield.TYPE_INFERNO):
                            drawCenteredString(
                                    Messages.getString("BoardView1.Inferno") + mf.getDensity() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                                    p.x, p.y + (int) (51 * scale),
                                    font_minefield, g);
                            break;
                        case (Minefield.TYPE_ACTIVE):
                            drawCenteredString(
                                    Messages.getString("BoardView1.Active") + mf.getDensity() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                                    p.x, p.y + (int) (51 * scale),
                                    font_minefield, g);
                            break;
                        case (Minefield.TYPE_COMMAND_DETONATED):
                            drawCenteredString(
                                    Messages.getString("BoardView1.Command-"), //$NON-NLS-1$
                                    p.x, p.y + (int) (51 * scale),
                                    font_minefield, g);
                            drawCenteredString(
                                    Messages.getString("BoardView1.detonated" + mf.getDensity() + ")"), //$NON-NLS-1$
                                    p.x, p.y + (int) (60 * scale),
                                    font_minefield, g);
                            break;
                        case (Minefield.TYPE_VIBRABOMB):
                            drawCenteredString(
                                    Messages.getString("BoardView1.Vibrabomb"), //$NON-NLS-1$
                                    p.x, p.y + (int) (51 * scale),
                                    font_minefield, g);
                            if (mf.getPlayerId() == localPlayer.getId()) {
                                drawCenteredString(
                                        "(" + mf.getSetting() + ")", //$NON-NLS-1$ //$NON-NLS-2$
                                        p.x, p.y + (int) (60 * scale),
                                        font_minefield, g);
                            }
                            break;
                    }
                }
            }
        }
    }

    private void drawCenteredString(String string, int x, int y, Font font,
            Graphics graph) {
        FontMetrics currentMetrics = getFontMetrics(font);
        int stringWidth = currentMetrics.stringWidth(string);

        x += ((hex_size.width - stringWidth) / 2);

        graph.setFont(font);
        graph.drawString(string, x, y);
    }

    /**
     * This method creates an image the size of the entire board (all
     * mapsheets), draws the hexes onto it, and returns that image.
     */
    public Image getEntireBoardImage() {
        Image entireBoard = createImage(boardSize.width, boardSize.height);
        Graphics boardGraph = entireBoard.getGraphics();
        drawRect = new Rectangle(0, 0);
        drawHexes(boardGraph, new Rectangle(boardSize));
        boardGraph.dispose();
        return entireBoard;
    }

    /**
     * Redraws all hexes in the specified rectangle
     */
    private void drawHexes(Graphics g, Rectangle view) {
        // only update visible hexes
        int drawX = (int) (view.x / (HEX_WC * scale)) - 1;
        int drawY = (int) (view.y / (HEX_H * scale)) - 1;

        int drawWidth = (int) (view.width / (HEX_WC * scale)) + 3;
        int drawHeight = (int) (view.height / (HEX_H * scale)) + 3;
        
        if (!useIsometric()) {
            // clear, if we need to
            if (view.x < (21 * scale)) {
                boardGraph.clearRect(view.x - drawRect.x, view.y - drawRect.y,
                        (int) (21 * scale) - view.x, view.height);
            }
            if (view.y < (36 * scale)) {
                boardGraph.clearRect(view.x - drawRect.x, view.y - drawRect.y,
                        view.width, (int) (36 * scale) - view.y);
            }
            if (view.x > (boardSize.width - view.width - (21 * scale))) {
                boardGraph.clearRect(Math.min(drawRect.width, boardSize.width)
                        - (int) (21 * scale), view.y - drawRect.y,
                        (int) (21 * scale), view.height);
            }
            if (view.y > (boardSize.height - view.height - (int) (36 * scale))) {
                boardGraph.clearRect(view.x - drawRect.x,
                        Math.min(drawRect.height, boardSize.height)
                                - (int) (36 * scale), view.width,
                        (int) (36 * scale));
            }
        }
        // draw some hexes.
        if (useIsometric()) {
            //g.clearRect(view.x, view.y, view.width, view.height);
            // When using isometric rendering, hexes within a given row
            // must be drawn from lowest to highest elevation.
            IBoard board = game.getBoard();
            final int minElev = board.getMinElevation();
            for (int i = 0; i < drawHeight; i++) {
                for (int x = minElev; x <= game.getBoard().getMaxElevation(); x++) {
                    for (int j = 0; j < drawWidth; j++) {
                        Coords c = new Coords(j + drawX, i + drawY);
                        IHex hex = game.getBoard().getHex(c);
                        if ((hex != null) && (hex.getElevation() == x)) {
                            drawHex(c, g);
                            drawMovementEnvelopeSpritesForHex(c, g,
                                    moveEnvSprites);
                            if ((en_Deployer != null)
                                    && board.isLegalDeployment(c,
                                            en_Deployer.getStartingPos())) {
                                drawHexBorder(getHexLocation(c), g,
                                        Color.yellow);
                            }
                        }
                    }
                }
                for (int k = 0; k < drawWidth; k++) {
                    Coords c = new Coords(k + drawX, i + drawY);
                    IHex hex = game.getBoard().getHex(c);
                    if (hex != null) {
                        drawOrthograph(c, g);
                        drawIsometricWreckSpritesForHex(c, g,
                                isometricWreckSprites);
                        drawIsometricSpritesForHex(c, g, isometricSprites);
                    }
                }
            }
            // If we are using Isometric rendering, redraw the entity
            // sprites at 50% transparent so sprites hidden behind hills can
            // still be seen by the user.
            drawIsometricSprites(g, isometricSprites);
        } else {
            // Draw hexes without regard to elevation when
            // not using Isometric, since it does not matter.
            for (int i = 0; i < drawHeight; i++) {
                for (int j = 0; j < drawWidth; j++) {
                    Coords c = new Coords(j + drawX, i + drawY);
                    drawHex(c, g);
                }
            }
        }
    }

    /**
     * Draws a hex onto the board buffer. This assumes that drawRect is current,
     * and does not check if the hex is visible.
     */
    private void drawHex(Coords c, Graphics boardGraph) {
        if (!game.getBoard().contains(c)) {
            return;
        }

        final IHex hex = game.getBoard().getHex(c);
        final Point hexLoc = getHexLocation(c);

        int level = hex.getElevation();

        int depth = hex.depth(false);

        ITerrain basement = hex.getTerrain(Terrains.BLDG_BASEMENT_TYPE);
        if (basement != null) {
            depth = 0;

        }
        int height = Math.max(hex.terrainLevel(Terrains.BLDG_ELEV),
                hex.terrainLevel(Terrains.BRIDGE_ELEV));
        height = Math.max(height, hex.terrainLevel(Terrains.INDUSTRIAL));

        // offset drawing point
        int drawX = hexLoc.x;
        int drawY = hexLoc.y;
        if (!useIsometric()) {
            drawX -= drawRect.x;
            drawY -= drawRect.y;
        }
        // draw picture
        Image baseImage = tileManager.baseFor(hex);
        Image scaledImage = getScaledImage(baseImage,true);

        boardGraph.drawImage(scaledImage, drawX, drawY, this);

        if (tileManager.supersFor(hex) != null) {
            for (Image image : tileManager.supersFor(hex)) {
                scaledImage = getScaledImage(image,true);
                boardGraph.drawImage(scaledImage, drawX, drawY, this);
            }
        }

        if (tileManager.orthoFor(hex) != null) {
            for (Image image : tileManager.orthoFor(hex)) {
                scaledImage = getScaledImage(image,true);
                if (!useIsometric()) {
                    boardGraph.drawImage(scaledImage, drawX, drawY, this);
                }
                // draw a shadow for bridge hex.
                if (useIsometric()
                        && (hex.terrainLevel(Terrains.BRIDGE_ELEV) > 0)) {
                    Image shadow = createShadowMask(scaledImage);

                    shadow = createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(TRANSPARENT)));
                    boardGraph.drawImage(shadow, hexLoc.x, hexLoc.y, this);
                }
            }
        }

        if (ecmHexes != null) {
            Integer tint = ecmHexes.get(c);
            if (tint != null) {
                scaledImage = getScaledImage(tileManager.getEcmShade(tint
                        .intValue()),true);
                boardGraph.drawImage(scaledImage, drawX, drawY, this);
            }
        }

        if (GUIPreferences.getInstance().getBoolean(
                GUIPreferences.ADVANCED_DARKEN_MAP_AT_NIGHT)
                && (game.getPlanetaryConditions().getLight() > PlanetaryConditions.L_DAY)
                && !game.isPositionIlluminated(c)) {
            scaledImage = getScaledImage(tileManager.getNightFog(),true);
            boardGraph.drawImage(scaledImage, drawX, drawY, this);
        }
        boardGraph.setColor(GUIPreferences.getInstance().getMapTextColor());
        if (game.getBoard().inSpace()) {
            boardGraph.setColor(Color.LIGHT_GRAY);
        }
        
        // draw special stuff for the hex
        final Collection<SpecialHexDisplay> shdList = game.getBoard()
                .getSpecialHexDisplay(c);
        try {
            if (shdList != null) {
                for (SpecialHexDisplay shd : shdList) {
                    if (shd.drawNow(game.getPhase(), game.getRoundCount(),
                            localPlayer)) {
                        scaledImage = getScaledImage(shd.getType()
                                .getDefaultImage(),true);
                        boardGraph.drawImage(scaledImage, drawX, drawY, this);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Illegal argument exception, probably " +
            		"can't load file.");
            e.printStackTrace();
            drawCenteredString("Loading Error", drawX, drawY
                    + (int) (50 * scale), font_note, boardGraph);
            return;
        }        

        // draw hex number
        if (scale >= 0.5) {
            drawCenteredString(c.getBoardNum(), drawX, drawY
                    + (int) (12 * scale), font_hexnum, boardGraph);
        }

        // draw terrain level / water depth / building height
        if (scale > 0.5f) {
            int ypos = 70;
            if (level != 0) {
                drawCenteredString(
                        Messages.getString("BoardView1.LEVEL") + level, //$NON-NLS-1$
                        drawX, drawY + (int) (ypos * scale), font_elev,
                        boardGraph);
                ypos -= 10;
            }
            if (depth != 0) {
                drawCenteredString(
                        Messages.getString("BoardView1.DEPTH") + depth, //$NON-NLS-1$
                        drawX, drawY + (int) (ypos * scale), font_elev,
                        boardGraph);
                ypos -= 10;
            }
            if (height > 0) {
                boardGraph.setColor(GUIPreferences.getInstance().getColor(
                        "AdvancedBuildingTextColor"));
                drawCenteredString(
                        Messages.getString("BoardView1.HEIGHT") + height, //$NON-NLS-1$
                        drawX, drawY + (int) (ypos * scale), font_elev,
                        boardGraph);
                ypos -= 10;
            }
        }

        // draw elevation borders
        boardGraph.setColor(Color.black);
        if (drawElevationLine(c, 0)) {
            final int x1 = drawX + (int) (21 * scale);
            final int x2 = drawX + (int) (62 * scale);
            final int y1 = drawY;
            final int y2 = drawY;

            drawIsometricElevation(c, Color.GRAY, new Point(x2, y2), new Point(
                    x1, y1), 0, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }
        if (drawElevationLine(c, 1)) {
            final int x1 = drawX + (int) (62 * scale);
            final int x2 = drawX + (int) (83 * scale);
            final int y1 = drawY;
            final int y2 = drawY + (int) (35 * scale);

            drawIsometricElevation(c, Color.DARK_GRAY, new Point(x2, y2),
                    new Point(x1, y1), 1, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }
        if (drawElevationLine(c, 2)) {
            final int x1 = drawX + (int) (83 * scale);
            final int x2 = drawX + (int) (62 * scale);
            final int y1 = drawY + (int) (36 * scale);
            final int y2 = drawY + (int) (71 * scale);

            drawIsometricElevation(c, Color.LIGHT_GRAY, new Point(x1, y1),
                    new Point(x2, y2), 2, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }
        if (drawElevationLine(c, 3)) {
            final int x1 = drawX + (int) (62 * scale);
            final int x2 = drawX + (int) (21 * scale);
            final int y1 = drawY + (int) (71 * scale);
            final int y2 = drawY + (int) (71 * scale);

            drawIsometricElevation(c, Color.GRAY, new Point(x2, y2), new Point(
                    x1, y1), 3, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }
        if (drawElevationLine(c, 4)) {
            final int x1 = drawX + (int) (21 * scale);
            final int x2 = drawX;
            final int y1 = drawY + (int) (71 * scale);
            final int y2 = drawY + (int) (36 * scale);

            drawIsometricElevation(c, Color.DARK_GRAY, new Point(x2, y2),
                    new Point(x1, y1), 4, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }
        if (drawElevationLine(c, 5)) {
            final int x1 = drawX;
            final int x2 = drawX + (int) (21 * scale);
            final int y1 = drawY + (int) (35 * scale);
            final int y2 = drawY;

            drawIsometricElevation(c, Color.LIGHT_GRAY, new Point(x1, y1),
                    new Point(x2, y2), 5, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }

        Coords src;
        if (selected != null) {
            src = selected;
        } else if (selectedEntity != null) {
            src = selectedEntity.getPosition();
        } else {
            src = null;
        }
        if (src != null && game.getBoard().contains(src)) {
            Point p = new Point(drawX, drawY);
            GUIPreferences gs = GUIPreferences.getInstance();
            boolean highlight = gs.getBoolean(GUIPreferences.FOV_HIGHLIGHT);
            boolean darken = gs.getBoolean(GUIPreferences.FOV_DARKEN);

            if ((darken || highlight)
                    && (game.getPhase() == Phase.PHASE_MOVEMENT)) {

                final int pad = 0;
                final int lw = 7;

                final int highlight_alpha = gs
                        .getInt(GUIPreferences.FOV_HIGHLIGHT_ALPHA);
                final Color cols[] = {
                        new Color(150, 150, 40, highlight_alpha),
                        new Color(40, 150, 40, highlight_alpha),
                        new Color(150, 40, 40, highlight_alpha),
                        new Color(150, 95, 150, highlight_alpha),
                        new Color(40, 40, 150, highlight_alpha) };

                final int max_dist = 30;
                final int d[] = { 4, 7, 10, 13, max_dist };

                final Color transparent_gray = new Color(0, 0, 0,
                        gs.getInt(GUIPreferences.FOV_DARKEN_ALPHA));
                final Color transparent_light_gray = new Color(0, 0, 0,
                        gs.getInt(GUIPreferences.FOV_DARKEN_ALPHA) / 2);
                final Color selected_color = new Color(50, 80, 150, 70);

                int dist = src.distance(c);

                int visualRange = 30;
                int minSensorRange = 0;
                int maxSensorRange = 0;
                LosEffects los = getLosEffects(src, c);
                if (null != selectedEntity) {
                    // TODO: how do we make adjustments for target spotlights
                    // illuminating intervening terrain?
                    visualRange = Compute.getVisualRange(game, selectedEntity,
                            los, false);
                    int bracket = Compute.getSensorRangeBracket(selectedEntity,
                            null);
                    int range = Compute.getSensorRangeByBracket(game,
                            selectedEntity, null, los);

                    maxSensorRange = bracket * range;
                    minSensorRange = Math.max((bracket - 1) * range, 0);
                    if (game.getOptions().booleanOption(
                            "inclusive_sensor_range")) {
                        minSensorRange = 0;
                    }
                }

                if (dist == 0) {
                    drawHexBorder(p, boardGraph, selected_color, pad, lw);
                } else if (dist < max_dist) {
                    if (!los.canSee() || (dist > visualRange)) {
                        if (darken) {
                            if (game.getOptions().booleanOption(
                                    "tacops_sensors")
                                    && (dist >= minSensorRange)
                                    && (dist <= maxSensorRange)) {
                                drawHexLayer(p, boardGraph,
                                        transparent_light_gray);
                            } else {
                                drawHexLayer(p, boardGraph, transparent_gray);
                            }
                        }
                    } else if (highlight) {
                        for (int k = 0; k < cols.length; k++) {
                            if (dist < d[k]) {
                                drawHexLayer(p, boardGraph, cols[k]);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // draw mapsheet borders
        if (GUIPreferences.getInstance().getShowMapsheets()) {
            boardGraph.setColor(GUIPreferences.getInstance().getColor(
                    GUIPreferences.ADVANCED_MAPSHEET_COLOR));
            if ((c.x % 16) == 0) {
                // left edge of sheet (edge 4 & 5)
                boardGraph
                        .drawLine(drawX + (int) (21 * scale), drawY
                                + (int) (71 * scale), drawX, drawY
                                + (int) (36 * scale));
                boardGraph.drawLine(drawX, drawY + (int) (35 * scale), drawX
                        + (int) (21 * scale), drawY);
            } else if ((c.x % 16) == 15) {
                // right edge of sheet (edge 1 & 2)
                boardGraph.drawLine(drawX + (int) (62 * scale), drawY, drawX
                        + (int) (83 * scale), drawY + (int) (35 * scale));
                boardGraph.drawLine(drawX + (int) (83 * scale), drawY
                        + (int) (36 * scale), drawX + (int) (62 * scale), drawY
                        + (int) (71 * scale));
            }
            if ((c.y % 17) == 0) {
                // top edge of sheet (edge 0 and possible 1 & 5)
                boardGraph.drawLine(drawX + (int) (21 * scale), drawY, drawX
                        + (int) (62 * scale), drawY);
                if ((c.x % 2) == 0) {
                    boardGraph.drawLine(drawX + (int) (62 * scale), drawY,
                            drawX + (int) (83 * scale), drawY
                                    + (int) (35 * scale));
                    boardGraph.drawLine(drawX, drawY + (int) (35 * scale),
                            drawX + (int) (21 * scale), drawY);
                }
            } else if ((c.y % 17) == 16) {
                // bottom edge of sheet (edge 3 and possible 2 & 4)
                boardGraph.drawLine(drawX + (int) (62 * scale), drawY
                        + (int) (71 * scale), drawX + (int) (21 * scale), drawY
                        + (int) (71 * scale));
                if ((c.x % 2) == 1) {
                    boardGraph.drawLine(drawX + (int) (83 * scale), drawY
                            + (int) (36 * scale), drawX + (int) (62 * scale),
                            drawY + (int) (71 * scale));
                    boardGraph.drawLine(drawX + (int) (21 * scale), drawY
                            + (int) (71 * scale), drawX, drawY
                            + (int) (36 * scale));
                }
            }
            boardGraph.setColor(Color.black);
        }
    }

    /**
     * Draws a orthographic hex onto the board buffer. This assumes that
     * drawRect is current, and does not check if the hex is visible.
     */
    private void drawOrthograph(Coords c, Graphics boardGraph) {
        if (!game.getBoard().contains(c)) {
            return;
        }

        final IHex oHex = game.getBoard().getHex(c);
        final Point oHexLoc = getHexLocation(c);

        int orthX = oHexLoc.x;
        int orthY = (oHexLoc.y - (int) (HEX_ELEV * scale * oHex
                .terrainLevel(Terrains.BRIDGE_ELEV)));
        if (!useIsometric()) {
            orthY = oHexLoc.y;
        }
        if (tileManager.orthoFor(oHex) != null) {
            for (Image image : tileManager.orthoFor(oHex)) {
                Image scaledImage = getScaledImage(image,true);

                // draw orthogonal
                boardGraph.drawImage(scaledImage, orthX, orthY, this);
            }
        }
    }

    private final boolean useIsometric() {
        return drawIsometric;
    }

    /**
     * Draws the Isometric elevation for the hex at the given coordinates (c) on
     * the side indicated by the direction (dir). This method only draws a
     * triangle for the elevation, the companion triangle representing the
     * adjacent hex is also needed. The two triangles when drawn together make a
     * complete rectangle representing the complete elevated hex side.
     * 
     * By drawing the elevated hex as two separate triangles we avoid clipping
     * problems with other hexes because the lower elevation is rendered before
     * the higher elevation. Thus any hexes that have a higher elevation than
     * the lower hex will overwrite the lower hex.
     * 
     * The Triangle for each hex side is formed by points p1, p2, and p3. Where
     * p1 and p2 are the original hex edges, and p3 has the same X value as p1,
     * but the y value has been increased (or decreased) based on the difference
     * in elevation between the given hex and the adjacent hex.
     * 
     * @param c
     *            Coordinates of the source hex.
     * @param color
     *            Color to use for the elevation polygons.
     * @param p1
     *            The First point on the edge of the hex.
     * @param p2
     *            The second point on the edge of the hex.
     * @param dir
     *            The side of the hex to have the elevation drawn on.
     * @param boardGraph
     */
    private final void drawIsometricElevation(Coords c, Color color, Point p1,
            Point p2, int dir, Graphics boardGraph) {
        final IHex dest = game.getBoard().getHexInDir(c, dir);
        final IHex src = game.getBoard().getHex(c);

        if (!useIsometric()) {
            return;
        }

        // Pad the polygon size slightly to avoid rounding errors from the scale
        // float.
        int fudge = -1;
        if ((dir == 2) || (dir == 4) || (dir == 3)) {
            fudge = 1;
        }

        final int elev = src.getElevation();
        // If the Destination is null, draw the complete elevation side.
        if ((dest == null) && (elev > 0)
                && ((dir == 2) || (dir == 3) || (dir == 4))) {

            // Determine the depth of the edge that needs to be drawn.
            int height = elev;
            IHex southHex = game.getBoard().getHexInDir(c, 3);
            if ((dir != 3) && (southHex != null)
                    && (elev > southHex.getElevation())) {
                height = elev - southHex.getElevation();
            }

            Polygon p = new Polygon(new int[] { p1.x, p2.x, p2.x, p1.x },
                    new int[] { p1.y + fudge, p2.y + fudge,
                            p2.y + (int) (HEX_ELEV * scale * height),
                            p1.y + (int) (HEX_ELEV * scale * height) }, 4);
            boardGraph.setColor(color);
            boardGraph.drawPolygon(p);
            boardGraph.fillPolygon(p);

            boardGraph.setColor(Color.BLACK);
            if ((dir == 2) || (dir == 4)) {
                boardGraph.drawLine(p1.x, p1.y, p1.x, p1.y
                        + (int) (HEX_ELEV * scale * height));
            }
            return;
        } else if (dest == null) {
            return;
        }

        int delta = elev - dest.getElevation();
        // Don't draw the elevation if there is no exposed edge for the player
        // to see.
        if ((delta == 0)
                || (((dir == 0) || (dir == 1) || (dir == 5)) && (delta > 0))
                || (((dir == 2) || (dir == 3) || (dir == 4)) && (delta < 0))) {
            return;
        }

        if (dir == 1) {
            // Draw a little bit of shadow to improve the 3d isometric effect.
            Polygon shadow1 = new Polygon(new int[] { p1.x, p2.x,
                    p2.x - (int) (HEX_ELEV * scale) }, new int[] { p1.y, p2.y,
                    p2.y }, 3);
            boardGraph.setColor(new Color(0, 0, 0, 0.4f));
            boardGraph.fillPolygon(shadow1);
        }

        if ((dir == 2) || (dir == 3) || (dir == 4)) {

            Point p3 = new Point(p1.x, p1.y + (int) (HEX_ELEV * scale * delta)
                    + fudge);

            Polygon p = new Polygon(new int[] { p1.x, p2.x, p2.x, p1.x },
                    new int[] { p1.y + fudge, p2.y + fudge,
                            p2.y + fudge + (int) (HEX_ELEV * scale * delta),
                            p1.y + fudge + (int) (HEX_ELEV * scale * delta) },
                    4);

            boardGraph.setColor(color);
            boardGraph.drawPolygon(p);
            boardGraph.fillPolygon(p);

            boardGraph.setColor(Color.BLACK);
            if ((dir == 1) || (dir == 2) || (dir == 5) || (dir == 4)) {
                boardGraph.drawLine(p1.x, p1.y, p3.x, p3.y);
            }
        }
    }

    /**
     * Returns true if an elevation line should be drawn between the starting
     * hex and the hex in the direction specified. Results should be transitive,
     * that is, if a line is drawn in one direction, it should be drawn in the
     * opposite direction as well.
     */
    private final boolean drawElevationLine(Coords src, int direction) {
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHexInDir(src, direction);
        if ((destHex == null) && (srcHex.getElevation() != 0)) {
            return true;
        } else if (destHex == null) {
            return false;
        } else if (srcHex.getElevation() != destHex.getElevation()) {
            return true;
        } else {
            return (srcHex.floor() != destHex.floor());
        }
    }

    /**
     * Returns the absolute position of the upper-left hand corner of the hex
     * graphic
     */
    private Point getHexLocation(int x, int y) {
        float elevationAdjust = 0.0f;

        IHex hex = game.getBoard().getHex(x, y);
        if ((hex != null) && useIsometric()) {
            int level = hex.getElevation();
            if (level != 0) {
                elevationAdjust = level * HEX_ELEV * scale * -1.0f;
            }
        }
        int ypos = (y * (int) (HEX_H * scale))
                + ((x & 1) == 1 ? (int) ((HEX_H / 2) * scale) : 0);
        return new Point(x * (int) (HEX_WC * scale), ypos
                + (int) elevationAdjust);
    }

    Point getHexLocation(Coords c) {
        return getHexLocation(c.x, c.y);
    }

    /**
     * Returns the absolute position of the centre of the hex graphic
     */
    private Point getCentreHexLocation(int x, int y) {
        Point p = getHexLocation(x, y);
        p.x += ((HEX_W / 2) * scale);
        p.y += ((HEX_H / 2) * scale);
        return p;
    }

    private Point getCentreHexLocation(Coords c) {
        return getCentreHexLocation(c.x, c.y);
    }

    public void drawRuler(Coords s, Coords e, Color sc, Color ec) {
        rulerStart = s;
        rulerEnd = e;
        rulerStartColor = sc;
        rulerEndColor = ec;

        repaint();
    }

    /**
     * Returns the coords at the specified point
     */
    Coords getCoordsAt(Point p) {
        final int x = (p.x) / (int) (HEX_WC * scale);
        final int y = ((p.y) - ((x & 1) == 1 ? (int) ((HEX_H / 2) * scale) : 0))
                / (int) (HEX_H * scale);
        Coords cOriginal = new Coords(x, y);
        if (useIsometric()) {
            // When using isometric rendering, a lower hex can obscure the
            // normal hex.
            // Iterate over all hexes from highest to lowest, looking for a hex
            // that
            // falls within the selected mouse click point.
            final int minElev = Math.min(0, game.getBoard().getMinElevation());
            final int maxElev = Math.max(0, game.getBoard().getMaxElevation());
            final int delta = (int) Math
                    .ceil(((double) maxElev - minElev) / 3.0f);
            final int minHexSpan = Math.max(y - delta, 0);
            final int maxHexSpan = Math.min(y + delta, game.getBoard()
                    .getHeight());
            for (int elev = maxElev; elev >= minElev; elev--) {
                for (int i = minHexSpan; i <= maxHexSpan; i++) {
                    Coords c1 = new Coords(x, i);
                    Point pAlt = getHexLocation(c1);
                    IHex hexAlt = game.getBoard().getHex(c1);
                    if ((p.y > pAlt.y) && (p.y < (pAlt.y + HEX_H))
                            && (hexAlt != null)
                            && (hexAlt.getElevation() == elev)) {
                        // This hex's location falls under the point the user
                        // selected.
                        return c1;
                    }
                }
            }
        }
        return cOriginal;

    }

    public void redrawMovingEntity(Entity entity, Coords position, int facing,
            int elevation) {
        Integer entityId = new Integer(entity.getId());
        EntitySprite sprite = entitySpriteIds.get(entityId);
        IsometricSprite isoSprite = isometricSpriteIds.get(entityId);

        ArrayList<EntitySprite> newSprites;
        ArrayList<IsometricSprite> isoSprites;
        HashMap<ArrayList<Integer>, EntitySprite> newSpriteIds;
        HashMap<ArrayList<Integer>, IsometricSprite> newIsoSpriteIds;

        if (sprite != null) {
            newSprites = new ArrayList<EntitySprite>(entitySprites);
            newSpriteIds = new HashMap<ArrayList<Integer>, EntitySprite>(
                    entitySpriteIds);

            newSprites.remove(sprite);

            entitySprites = newSprites;
            entitySpriteIds = newSpriteIds;
        }

        if (isoSprite != null) {
            isoSprites = new ArrayList<IsometricSprite>(isometricSprites);
            newIsoSpriteIds = new HashMap<ArrayList<Integer>, IsometricSprite>(
                    isometricSpriteIds);

            isoSprites.remove(isoSprite);

            isometricSprites = isoSprites;
            isometricSpriteIds = newIsoSpriteIds;
        }

        MovingEntitySprite mSprite = movingEntitySpriteIds.get(entityId);
        ArrayList<MovingEntitySprite> newMovingSprites = new ArrayList<MovingEntitySprite>(
                movingEntitySprites);
        HashMap<Integer, MovingEntitySprite> newMovingSpriteIds = new HashMap<Integer, MovingEntitySprite>(
                movingEntitySpriteIds);

        if (mSprite != null) {
            newMovingSprites.remove(mSprite);
        }

        if (entity.getPosition() != null) {
            mSprite = new MovingEntitySprite(entity, position, facing,
                    elevation);
            newMovingSprites.add(mSprite);
            newMovingSpriteIds.put(entityId, mSprite);
        }

        movingEntitySprites = newMovingSprites;
        movingEntitySpriteIds = newMovingSpriteIds;
    }

    public boolean isMovingUnits() {
        return movingUnits.size() > 0;
    }

    /**
     * Clears the sprite for an entity and prepares it to be re-drawn. Replaces
     * the old sprite with the new! Try to prevent annoying
     * ConcurrentModificationExceptions
     */
    public void redrawEntity(Entity entity) {
        redrawEntity(entity, null);
    }

    /**
     * Clears the sprite for an entity and prepares it to be re-drawn. Replaces
     * the old sprite with the new! Takes a reference to the Entity object
     * before changes, in case it contained important state information, like
     * Dropships taking off (airborne dropships lose their secondary hexes). Try
     * to prevent annoying ConcurrentModificationExceptions
     */
    public void redrawEntity(Entity entity, Entity oldEntity) {
        Integer entityId = new Integer(entity.getId());
        if (oldEntity == null) {
            oldEntity = entity;
        }

        if (entity.getPosition() == null) {
            for (Iterator<EntitySprite> spriteIter = entitySprites.iterator(); spriteIter
                    .hasNext();) {
                EntitySprite sprite = spriteIter.next();
                if (sprite.entity.equals(entity)) {
                    spriteIter.remove();
                }
            }
            for (Iterator<EntitySprite> spriteIter = entitySpriteIds.values()
                    .iterator(); spriteIter.hasNext();) {
                EntitySprite sprite = spriteIter.next();
                if (sprite.entity.equals(entity)) {
                    spriteIter.remove();
                }
            }
            for (Iterator<IsometricSprite> spriteIter = isometricSprites
                    .iterator(); spriteIter.hasNext();) {
                IsometricSprite sprite = spriteIter.next();
                if (sprite.entity.equals(entity)) {
                    spriteIter.remove();
                }
            }
            for (Iterator<IsometricSprite> spriteIter = isometricSpriteIds
                    .values().iterator(); spriteIter.hasNext();) {
                IsometricSprite sprite = spriteIter.next();
                if (sprite.entity.equals(entity)) {
                    spriteIter.remove();
                }
            }
        }

        ArrayList<EntitySprite> newSprites = new ArrayList<EntitySprite>(
                entitySprites);
        HashMap<ArrayList<Integer>, EntitySprite> newSpriteIds = new HashMap<ArrayList<Integer>, EntitySprite>(
                entitySpriteIds);
        ArrayList<IsometricSprite> isoSprites = new ArrayList<IsometricSprite>(
                isometricSprites);
        HashMap<ArrayList<Integer>, IsometricSprite> newIsoSpriteIds = new HashMap<ArrayList<Integer>, IsometricSprite>(
                isometricSpriteIds);

        ArrayList<Integer> temp = new ArrayList<Integer>();
        temp.add(entityId);
        temp.add(-1);
        EntitySprite sprite = entitySpriteIds.get(temp);
        IsometricSprite isoSprite = isometricSpriteIds.get(temp);
        if (sprite != null) {
            newSprites.remove(sprite);
        }
        if (isoSprite != null) {
            isoSprites.remove(isoSprite);
        }
        for (int secondaryPos : oldEntity.getSecondaryPositions().keySet()) {
            temp = new ArrayList<Integer>();
            temp.add(entityId);
            temp.add(secondaryPos);
            sprite = entitySpriteIds.get(temp);
            if (sprite != null) {
                newSprites.remove(sprite);
            }
            isoSprite = isometricSpriteIds.get(temp);
            if (isoSprite != null) {
                isoSprites.remove(isoSprite);
            }

        }

        Coords position = entity.getPosition();
        if (position != null) {
            // If no secondary positions, add a sprite for the central position
            if (entity.getSecondaryPositions().isEmpty()) {
                sprite = new EntitySprite(entity, -1);
                newSprites.add(sprite);
                temp = new ArrayList<Integer>();
                temp.add(entityId);
                temp.add(-1);
                newSpriteIds.put(temp, sprite);
            } else { // Add all secondary position sprites, which includes a
                     // sprite for the central hex
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    sprite = new EntitySprite(entity, secondaryPos);
                    newSprites.add(sprite);
                    temp = new ArrayList<Integer>();
                    temp.add(entityId);
                    temp.add(secondaryPos);
                    newSpriteIds.put(temp, sprite);
                }
            }

            // If no secondary positions, add a sprite for the central position
            if (entity.getSecondaryPositions().isEmpty()) {
                isoSprite = new IsometricSprite(entity, -1);
                isoSprites.add(isoSprite);
                temp = new ArrayList<Integer>();
                temp.add(entityId);
                temp.add(-1);
                newIsoSpriteIds.put(temp, isoSprite);
            } else { // Add all secondary position sprites, which includes a
                     // sprite for the central hex
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    isoSprite = new IsometricSprite(entity, secondaryPos);
                    isoSprites.add(isoSprite);
                    temp = new ArrayList<Integer>();
                    temp.add(entityId);
                    temp.add(secondaryPos);
                    newIsoSpriteIds.put(temp, isoSprite);
                }
            }
        }

        entitySprites = newSprites;
        entitySpriteIds = newSpriteIds;
        isometricSprites = isoSprites;
        isometricSpriteIds = newIsoSpriteIds;

        for (Iterator<C3Sprite> i = c3Sprites.iterator(); i.hasNext();) {
            final C3Sprite c3sprite = i.next();
            if ((c3sprite.entityId == entity.getId())
                    || (c3sprite.masterId == entity.getId())) {
                i.remove();
            }
        }
        // WOR
        if (entity.hasC3() || entity.hasC3i() || entity.hasActiveNovaCEWS()) {
            addC3Link(entity);
        }

        for (Iterator<FlyOverSprite> i = flyOverSprites.iterator(); i.hasNext();) {
            final FlyOverSprite flyOverSprite = i.next();
            if (flyOverSprite.getEntityId() == entity.getId()) {
                i.remove();
            }
        }
        if (entity.isAirborne() && (entity.getPassedThrough().size() > 1)) {
            addFlyOverPath(entity);
        }

        scheduleRedraw();
    }

    /**
     * Clears all old entity sprites out of memory and sets up new ones.
     */
    void redrawAllEntities() {
        ArrayList<EntitySprite> newSprites = new ArrayList<EntitySprite>(
                game.getNoOfEntities());
        ArrayList<IsometricSprite> newIsometricSprites = new ArrayList<IsometricSprite>(
                game.getNoOfEntities());
        HashMap<ArrayList<Integer>, EntitySprite> newSpriteIds = new HashMap<ArrayList<Integer>, EntitySprite>(
                game.getNoOfEntities());
        HashMap<ArrayList<Integer>, IsometricSprite> newIsoSpriteIds = new HashMap<ArrayList<Integer>, IsometricSprite>(
                game.getNoOfEntities());

        ArrayList<WreckSprite> newWrecks = new ArrayList<WreckSprite>();
        ArrayList<IsometricWreckSprite> newIsometricWrecks = new ArrayList<IsometricWreckSprite>();

        Enumeration<Entity> e = game.getWreckedEntities();
        while (e.hasMoreElements()) {
            Entity entity = e.nextElement();
            if (!(entity instanceof Infantry) && (entity.getPosition() != null)) {
                WreckSprite ws;
                IsometricWreckSprite iws;
                if (entity.getSecondaryPositions().isEmpty()) {
                    ws = new WreckSprite(entity, -1);
                    newWrecks.add(ws);
                    iws = new IsometricWreckSprite(entity, -1);
                    newIsometricWrecks.add(iws);
                } else {
                    for (int secondaryPos : entity.getSecondaryPositions()
                            .keySet()) {
                        ws = new WreckSprite(entity, secondaryPos);
                        newWrecks.add(ws);
                        iws = new IsometricWreckSprite(entity, secondaryPos);
                        newIsometricWrecks.add(iws);
                    }
                }
            }
        }

        clearC3Networks();
        clearFlyOverPaths();
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = i.nextElement();
            if (entity.getPosition() == null) {
                continue;
            }
            if (entity.getSecondaryPositions().isEmpty()) {
                EntitySprite sprite = new EntitySprite(entity, -1);
                newSprites.add(sprite);
                ArrayList<Integer> temp = new ArrayList<Integer>();
                temp.add(entity.getId());
                temp.add(-1);
                newSpriteIds.put(temp, sprite);
                IsometricSprite isosprite = new IsometricSprite(entity, -1);
                newIsometricSprites.add(isosprite);
                temp = new ArrayList<Integer>();
                temp.add(entity.getId());
                temp.add(-1);
                newIsoSpriteIds.put(temp, isosprite);
            } else {
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    EntitySprite sprite = new EntitySprite(entity, secondaryPos);
                    newSprites.add(sprite);
                    ArrayList<Integer> temp = new ArrayList<Integer>();
                    temp.add(entity.getId());
                    temp.add(secondaryPos);
                    newSpriteIds.put(temp, sprite);

                    IsometricSprite isosprite = new IsometricSprite(entity,
                            secondaryPos);
                    newIsometricSprites.add(isosprite);
                    temp = new ArrayList<Integer>();
                    temp.add(entity.getId());
                    temp.add(secondaryPos);
                    newIsoSpriteIds.put(temp, isosprite);
                }
            }
            // WOR
            if (entity.hasC3() || entity.hasC3i() || entity.hasActiveNovaCEWS()) {
                addC3Link(entity);
            }

            if (entity.isAirborne() && (entity.getPassedThrough().size() > 1)) {
                addFlyOverPath(entity);
            }
        }

        entitySprites = newSprites;
        entitySpriteIds = newSpriteIds;

        isometricSprites = newIsometricSprites;
        isometricSpriteIds = newIsoSpriteIds;

        wreckSprites = newWrecks;
        isometricWreckSprites = newIsometricWrecks;

        scheduleRedraw();
    }

    /**
     * Moves the cursor to the new position, or hides it, if newPos is null
     */
    private void moveCursor(CursorSprite cursor, Coords newPos) {
        final Rectangle oldBounds = new Rectangle(cursor.getBounds());
        if (newPos != null) {
            cursor.setHexLocation(newPos);
        } else {
            cursor.setOffScreen();
        }
        // repaint affected area
        repaint(oldBounds);
        repaint(cursor.getBounds());
    }

    public void centerOnHex(Coords c) {
        if (null == c) {
            return;
        }
        // the scrollbars auto-correct if we try to set a value that's out of
        // bounds
        Point hexPoint = getCentreHexLocation(c);
        JScrollBar vscroll = scrollpane.getVerticalScrollBar();
        vscroll.setValue(hexPoint.y - (vscroll.getVisibleAmount() / 2));
        JScrollBar hscroll = scrollpane.getHorizontalScrollBar();
        hscroll.setValue(hexPoint.x - (hscroll.getVisibleAmount() / 2));
        repaint();
    }

    /**
     * Clears the old movement data and draws the new. Since it's less expensive
     * to check for and reuse old step sprites than to make a whole new one, we
     * do that.
     */
    public void drawMovementData(Entity entity, MovePath md) {
        ArrayList<StepSprite> temp = pathSprites;
        MoveStep previousStep = null;

        clearMovementData();

        // Nothing to do if we don't have a MovePath
        if (md == null) {
            return;
        }
        // need to update the movement sprites based on the move path for this
        // entity
        // only way to do this is to clear and refresh (seems wasteful)

        // first get the color for the vector
        Color col = Color.blue;
        if (md.getLastStep() != null) {
            switch (md.getLastStep().getMovementType()) {
                case MOVE_RUN:
                case MOVE_VTOL_RUN:
                case MOVE_OVER_THRUST:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveRunColor");
                    break;
                case MOVE_SPRINT:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveSprintColor");
                    break;
                case MOVE_JUMP:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveJumpColor");
                    break;
                case MOVE_ILLEGAL:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveIllegalColor");
                    break;
                default:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveDefaultColor");
                    break;
            }
        }

        refreshMoveVectors(entity, md, col);

        for (Enumeration<MoveStep> i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = i.nextElement();
            // check old movement path for reusable step sprites
            boolean found = false;
            for (StepSprite sprite : temp) {
                if (sprite.getStep().canReuseSprite(step)
                        && !(entity instanceof Aero)) {
                    pathSprites.add(sprite);
                    found = true;
                }
            }
            if (!found) {
                if ((null != previousStep)
                        && ((step.getType() == MoveStepType.UP)
                                || (step.getType() == MoveStepType.DOWN)
                                || (step.getType() == MoveStepType.ACC)
                                || (step.getType() == MoveStepType.DEC)
                                || (step.getType() == MoveStepType.ACCN) || (step
                                .getType() == MoveStepType.DECN))) {
                    // Mark the previous elevation change sprite hidden
                    // so that we can draw a new one in it's place without
                    // having overlap.
                    pathSprites.get(pathSprites.size() - 1).hidden = true;
                }

                // for advanced movement, we always need to hide prior
                // because costs will overlap and we only want the current
                // facing
                if ((previousStep != null) && game.useVectorMove()) {
                    pathSprites.get(pathSprites.size() - 1).hidden = true;
                }

                pathSprites.add(new StepSprite(step));
            }
            previousStep = step;
        }
        repaint(100);
    }

    /**
     * Clears current movement data from the screen
     */
    public void clearMovementData() {
        pathSprites = new ArrayList<StepSprite>();
        repaint();
        refreshMoveVectors();
    }

    public void setFiringSolutions(Entity attacker,
            Hashtable<Integer, ToHitData> firingSolutions) {

        clearFiringSolutionData();
        if (firingSolutions == null) {
            return;
        }
        for (ToHitData thd : firingSolutions.values()) {
            FiringSolutionSprite sprite = new FiringSolutionSprite(
                    thd.getValue(), thd.getRange(), thd.getLocation());
            firingSprites.add(sprite);
        }
    }

    public void clearFiringSolutionData() {
        firingSprites.clear();
        repaint();
    }

    public void setMovementEnvelope(Hashtable<Coords, Integer> mvEnvData,
            int walk, int run, int jump, int gear) {
        clearMovementEnvelope();

        if (mvEnvData == null) {
            return;
        }

        GUIPreferences guip = GUIPreferences.getInstance();
        for (Coords loc : mvEnvData.keySet()) {
            Color spriteColor = null;
            if (gear == MovementDisplay.GEAR_JUMP) {
                if (mvEnvData.get(loc) <= jump) {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_JUMP_COLOR);
                }
            } else {
                if (mvEnvData.get(loc) <= walk) {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_DEFAULT_COLOR);
                } else if (mvEnvData.get(loc) <= run) {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_RUN_COLOR);
                } else {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_SPRINT_COLOR);
                }
            }
            if (spriteColor != null) {
                MovementEnvelopeSprite mvSprite = new MovementEnvelopeSprite(
                        spriteColor, loc);
                moveEnvSprites.add(mvSprite);
            }
        }

    }

    public void clearMovementEnvelope() {
        moveEnvSprites.clear();
        repaint();
    }

    public void setLocalPlayer(IPlayer p) {
        localPlayer = p;
    }

    public IPlayer getLocalPlayer() {
        return localPlayer;
    }

    /**
     * Specifies that this should mark the deployment hexes for a player. If the
     * player is set to null, no hexes will be marked.
     */
    public void markDeploymentHexesFor(Entity ce) {
        en_Deployer = ce;
        repaint(100);
    }

    /**
     * Returns the entity that is currently being deployed
     */
    public Entity getDeployingEntity() {
        return en_Deployer;
    }

    /**
     * add a fly over path to the sprite list
     */
    public void addFlyOverPath(Entity e) {
        if (e.getPosition() == null) {
            return;
        }

        flyOverSprites.add(new FlyOverSprite(e));
    }

    /**
     * Adds a c3 line to the sprite list.
     */
    public void addC3Link(Entity e) {
        if (e.getPosition() == null) {
            return;
        }

        if (e.hasC3i()) {
            for (Enumeration<Entity> i = game.getEntities(); i
                    .hasMoreElements();) {
                final Entity fe = i.nextElement();
                if (fe.getPosition() == null) {
                    return;
                }
                if (e.onSameC3NetworkAs(fe)
                        && !fe.equals(e)
                        && !Compute.isAffectedByECM(e, e.getPosition(),
                                fe.getPosition())) {
                    c3Sprites.add(new C3Sprite(e, fe));
                }
            }
        } else if (e.hasActiveNovaCEWS()) {
            // WOR Nova CEWS
            for (Enumeration<Entity> i = game.getEntities(); i
                    .hasMoreElements();) {
                final Entity fe = i.nextElement();
                if (fe.getPosition() == null) {
                    return;
                }
                if (e.onSameC3NetworkAs(fe)
                        && !fe.equals(e)
                        && !Compute.isAffectedByNovaECM(e, e.getPosition(),
                                fe.getPosition())) {
                    c3Sprites.add(new C3Sprite(e, fe));
                }
            }
        } else if (e.getC3Master() != null) {
            Entity eMaster = e.getC3Master();
            if (eMaster.getPosition() == null) {
                return;
            }

            // ECM cuts off the network
            boolean blocked = false;
            if (e.hasBoostedC3() && eMaster.hasBoostedC3()) {
                blocked = Compute.isAffectedByAngelECM(e, e.getPosition(),
                        eMaster.getPosition())
                        || Compute.isAffectedByAngelECM(eMaster,
                                eMaster.getPosition(), eMaster.getPosition());
            } else {
                blocked = Compute.isAffectedByECM(e, e.getPosition(),
                        eMaster.getPosition())
                        || Compute.isAffectedByECM(eMaster,
                                eMaster.getPosition(), eMaster.getPosition());
            }

            if (!blocked) {
                c3Sprites.add(new C3Sprite(e, e.getC3Master()));
            }
        }
    }

    /**
     * Adds an attack to the sprite list.
     */
    public synchronized void addAttack(AttackAction aa) {
        // do not make a sprite unless we're aware of both entities
        // this is not a great solution but better than a crash
        Entity ae = game.getEntity(aa.getEntityId());
        Targetable t = game.getTarget(aa.getTargetType(), aa.getTargetId());
        if ((ae == null) || (t == null)
                || (t.getTargetType() == Targetable.TYPE_INARC_POD)
                || (t.getPosition() == null) || (ae.getPosition() == null)) {
            return;
        }
        repaint(100);
        for (AttackSprite sprite : attackSprites) {
            // can we just add this attack to an existing one?
            if ((sprite.getEntityId() == aa.getEntityId())
                    && (sprite.getTargetId() == aa.getTargetId())) {
                // use existing attack, but add this weapon
                if (aa instanceof WeaponAttackAction) {
                    WeaponAttackAction waa = (WeaponAttackAction) aa;
                    if (aa.getTargetType() != Targetable.TYPE_HEX_ARTILLERY) {
                        sprite.addWeapon(waa);
                    } else if (waa.getEntity(game).getOwner().getId() == localPlayer
                            .getId()) {
                        sprite.addWeapon(waa);
                    }
                }
                if (aa instanceof KickAttackAction) {
                    sprite.addWeapon((KickAttackAction) aa);
                }
                if (aa instanceof PunchAttackAction) {
                    sprite.addWeapon((PunchAttackAction) aa);
                }
                if (aa instanceof PushAttackAction) {
                    sprite.addWeapon((PushAttackAction) aa);
                }
                if (aa instanceof ClubAttackAction) {
                    sprite.addWeapon((ClubAttackAction) aa);
                }
                if (aa instanceof ChargeAttackAction) {
                    sprite.addWeapon((ChargeAttackAction) aa);
                }
                if (aa instanceof DfaAttackAction) {
                    sprite.addWeapon((DfaAttackAction) aa);
                }
                if (aa instanceof ProtomechPhysicalAttackAction) {
                    sprite.addWeapon((ProtomechPhysicalAttackAction) aa);
                }
                if (aa instanceof SearchlightAttackAction) {
                    sprite.addWeapon((SearchlightAttackAction) aa);
                }
                return;
            }
        }
        // no re-use possible, add a new one
        // don't add a sprite for an artillery attack made by the other player
        if (aa instanceof WeaponAttackAction) {
            WeaponAttackAction waa = (WeaponAttackAction) aa;
            if (aa.getTargetType() != Targetable.TYPE_HEX_ARTILLERY) {
                attackSprites.add(new AttackSprite(aa));
            } else if (waa.getEntity(game).getOwner().getId() == localPlayer
                    .getId()) {
                attackSprites.add(new AttackSprite(aa));
            }
        } else {
            attackSprites.add(new AttackSprite(aa));
        }
    }

    /** Removes all attack sprites from a certain entity */
    public synchronized void removeAttacksFor(Entity e) {
        if (e == null) {
            return;
        }
        int entityId = e.getId();
        for (Iterator<AttackSprite> i = attackSprites.iterator(); i.hasNext();) {
            AttackSprite sprite = i.next();
            if (sprite.getEntityId() == entityId) {
                i.remove();
            }
        }
        repaint(100);
    }

    /**
     * Clears out all attacks and re-adds the ones in the current game.
     */
    public void refreshAttacks() {
        clearAllAttacks();
        for (Enumeration<EntityAction> i = game.getActions(); i
                .hasMoreElements();) {
            EntityAction ea = i.nextElement();
            if (ea instanceof AttackAction) {
                addAttack((AttackAction) ea);
            }
        }
        for (Enumeration<AttackAction> i = game.getCharges(); i
                .hasMoreElements();) {
            EntityAction ea = i.nextElement();
            if (ea instanceof PhysicalAttackAction) {
                addAttack((AttackAction) ea);
            }
        }
        repaint(100);
    }

    public void refreshMoveVectors() {
        clearAllMoveVectors();
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity e = i.nextElement();
            if (e.getPosition() != null) {
                movementSprites.add(new MovementSprite(e, e.getVectors(),
                        Color.gray, false));
            }
        }
    }

    public void refreshMoveVectors(Entity en, MovePath md, Color col) {
        clearAllMoveVectors();
        // same as normal but when I find the active entity I used the MovePath
        // to get vector
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity e = i.nextElement();
            if (e.getPosition() != null) {
                if ((en != null) && (e.getId() == en.getId())) {
                    movementSprites.add(new MovementSprite(e, md
                            .getFinalVectors(), col, true));
                } else {
                    movementSprites.add(new MovementSprite(e, e.getVectors(),
                            col, false));
                }
            }
        }
    }

    public void clearC3Networks() {
        c3Sprites.clear();
    }

    public void clearFlyOverPaths() {
        flyOverSprites.clear();
    }

    /**
     * Clears out all attacks that were being drawn
     */
    public void clearAllAttacks() {
        attackSprites.clear();
    }

    /**
     * Clears out all movement vectors that were being drawn
     */
    public void clearAllMoveVectors() {
        movementSprites.clear();
    }

    protected void firstLOSHex(Coords c) {
        if (useLOSTool) {
            moveCursor(secondLOSSprite, null);
            moveCursor(firstLOSSprite, c);
        }
    }

    protected void secondLOSHex(Coords c2, Coords c1) {
        if (useLOSTool) {

            Entity ae = chooseEntity(c1);
            Entity te = chooseEntity(c2);

            StringBuffer message = new StringBuffer();
            LosEffects le;
            if ((ae == null) || (te == null)) {
                boolean mechInFirst = GUIPreferences.getInstance()
                        .getMechInFirst();
                boolean mechInSecond = GUIPreferences.getInstance()
                        .getMechInSecond();
                LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
                ai.attackPos = c1;
                ai.targetPos = c2;
                ai.attackHeight = mechInFirst ? 1 : 0;
                ai.targetHeight = mechInSecond ? 1 : 0;
                ai.targetIsMech = mechInSecond;
                ai.attackerIsMech = mechInFirst;
                ai.attackAbsHeight = game.getBoard().getHex(c1).floor()
                        + ai.attackHeight;
                ai.targetAbsHeight = game.getBoard().getHex(c2).floor()
                        + ai.targetHeight;
                le = LosEffects.calculateLos(game, ai);
                message.append(Messages
                        .getString(
                                "BoardView1.Attacker", new Object[] { //$NON-NLS-1$
                                        mechInFirst ? Messages
                                                .getString("BoardView1.Mech") : Messages.getString("BoardView1.NonMech"), //$NON-NLS-1$ //$NON-NLS-2$
                                        c1.getBoardNum() }));
                message.append(Messages
                        .getString(
                                "BoardView1.Target", new Object[] { //$NON-NLS-1$
                                        mechInSecond ? Messages
                                                .getString("BoardView1.Mech") : Messages.getString("BoardView1.NonMech"), //$NON-NLS-1$ //$NON-NLS-2$
                                        c2.getBoardNum() }));
            } else {
                le = LosEffects.calculateLos(game, ae.getId(), te);
                message.append(Messages.getString(
                        "BoardView1.Attacker", new Object[] { //$NON-NLS-1$
                        ae.getDisplayName(), c1.getBoardNum() }));
                message.append(Messages.getString(
                        "BoardView1.Target", new Object[] { //$NON-NLS-1$
                        te.getDisplayName(), c2.getBoardNum() }));
            }
            // Check to see if LoS is blocked
            if (!le.canSee()) {
                message.append(Messages.getString("BoardView1.LOSBlocked",
                        new Object[] { //$NON-NLS-1$
                        new Integer(c1.distance(c2)) }));
                ToHitData thd = le.losModifiers(game);
                message.append("\t" + thd.getDesc() + "\n");
            } else {
                message.append(Messages.getString("BoardView1.LOSNotBlocked",
                        new Object[] { //$NON-NLS-1$
                        new Integer(c1.distance(c2)) }));
                if (le.getHeavyWoods() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.HeavyWoods", new Object[] { //$NON-NLS-1$
                            new Integer(le.getHeavyWoods()) }));
                }
                if (le.getLightWoods() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.LightWoods", new Object[] { //$NON-NLS-1$
                            new Integer(le.getLightWoods()) }));
                }
                if (le.getLightSmoke() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.LightSmoke", new Object[] { //$NON-NLS-1$
                            new Integer(le.getLightSmoke()) }));
                }
                if (le.getHeavySmoke() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.HeavySmoke", new Object[] { //$NON-NLS-1$
                            new Integer(le.getHeavySmoke()) }));
                }
                if (le.isTargetCover() && le.canSee()) {
                    message.append(Messages
                            .getString(
                                    "BoardView1.TargetPartialCover", new Object[] { //$NON-NLS-1$
                                    LosEffects.getCoverName(
                                            le.getTargetCover(), true) }));
                }
                if (le.isAttackerCover() && le.canSee()) {
                    message.append(Messages.getString(
                            "BoardView1.AttackerPartialCover", new Object[] { //$NON-NLS-1$
                            LosEffects.getCoverName(le.getAttackerCover(),
                                    false) }));
                }
            }
            JOptionPane.showMessageDialog(getRootPane(), message.toString(),
                    Messages.getString("BoardView1.LOSTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Calculate the LosEffects between the given Coords. Unit height for the
     * source hex is determined by the selectedEntity if present otherwise the
     * GUIPreference 'mechInFirst' is used. Unit height for the destination hex
     * is determined by the tallest unit present in that hex. If no units are
     * present, the GUIPreference 'mechInSecond' is used.
     */
    protected LosEffects getLosEffects(Coords src, Coords dest) {
        LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
        ai.attackPos = src;
        ai.targetPos = dest;
        // First, we check for a selected unit and use its height. If there's
        // no selected mech we use the mechInFirst GUIPref.
        if (selectedEntity != null) {
            ai.attackHeight = selectedEntity.getHeight();
            ai.attackAbsHeight = selectedEntity.absHeight()
                    + selectedEntity.elevationOccupied(game.getBoard().getHex(
                            src));
            EntityMovementMode movementMode = selectedEntity.getMovementMode();
            // We will have double counted elevation for VTOL and WIGE movement
            if ((movementMode == EntityMovementMode.VTOL)
                    || (movementMode == EntityMovementMode.WIGE)) {
                ai.attackAbsHeight -= selectedEntity.getElevation();
            }
        } else {
            ai.attackHeight = GUIPreferences.getInstance().getMechInFirst() ? 1
                    : 0;
            ai.attackAbsHeight = game.getBoard().getHex(src).surface()
                    + ai.attackHeight;
        }
        // First, we take the tallest unit in the destination hex, if no units
        // are present we use the mechInSecond GUIPref.
        Enumeration<Entity> destEntities = game.getEntities(dest);
        ai.targetHeight = ai.targetAbsHeight = Integer.MIN_VALUE;
        while (destEntities.hasMoreElements()) {
            Entity ent = destEntities.nextElement();
            int attAbsheight = ent.absHeight()
                    + ent.elevationOccupied(game.getBoard().getHex(dest));
            EntityMovementMode movementMode = ent.getMovementMode();
            // We will have double counted elevation for VTOL and WIGE movement
            if ((movementMode == EntityMovementMode.VTOL)
                    || (movementMode == EntityMovementMode.WIGE)) {
                attAbsheight -= ent.getElevation();
            }
            if (attAbsheight > ai.targetAbsHeight) {
                ai.targetHeight = ent.getHeight();
                ai.targetAbsHeight = attAbsheight;
            }
        }
        if ((ai.targetHeight == Integer.MIN_VALUE)
                && (ai.targetAbsHeight == Integer.MIN_VALUE)) {
            ai.targetHeight = GUIPreferences.getInstance().getMechInSecond() ? 1
                    : 0;
            ai.targetAbsHeight = game.getBoard().getHex(dest).surface()
                    + ai.targetHeight;
        }
        return LosEffects.calculateLos(game, ai);
    }

    /**
     * Initializes the various overlay polygons with their vertices.
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

    synchronized boolean doMoveUnits(long idleTime) {
        boolean movingSomething = false;

        if (movingUnits.size() > 0) {

            moveWait += idleTime;

            if (moveWait > GUIPreferences.getInstance().getInt(
                    "AdvancedMoveStepDelay")) {

                ArrayList<MovingUnit> spent = new ArrayList<MovingUnit>();

                for (MovingUnit move : movingUnits) {
                    movingSomething = true;
                    Entity ge = game.getEntity(move.entity.getId());
                    if (move.path.size() > 0) {

                        UnitLocation loc = move.path.get(0);

                        if (ge != null) {
                            redrawMovingEntity(move.entity, loc.getCoords(),
                                    loc.getFacing(), loc.getElevation());
                        }
                        move.path.remove(0);
                    } else {
                        if (ge != null) {
                            redrawEntity(ge);
                        }
                        spent.add(move);
                    }

                }

                for (MovingUnit move : spent) {
                    movingUnits.remove(move);
                }
                moveWait = 0;

                if (movingUnits.size() == 0) {
                    movingEntitySpriteIds.clear();
                    movingEntitySprites.clear();
                    ghostEntitySprites.clear();
                    processBoardViewEvent(new BoardViewEvent(this,
                            BoardViewEvent.FINISHED_MOVING_UNITS));
                }
            }
        }
        return movingSomething;
    }

    //
    // MouseListener
    //
    public void mousePressed(MouseEvent me) {
        requestFocusInWindow();
        Point point = me.getPoint();
        if (null == point) {
            return;
        }
        // we clicked the right mouse button,
        // remember the position if we start to scroll
        // if we drag, we should scroll
        if (SwingUtilities.isRightMouseButton(me)) {
            scrollXDifference = me.getX();
            scrollYDifference = me.getY();
            shouldScroll = true;
        }

        if (me.isPopupTrigger() && !dragging) {
            mouseAction(getCoordsAt(point), BOARD_HEX_POPUP, me.getModifiers());
            return;
        }
        for (int i = 0; i < displayables.size(); i++) {
            IDisplayable disp = displayables.get(i);
            double width = Math.min(boardSize.getWidth(), scrollpane
                    .getViewport().getSize().getWidth());
            double height = Math.min(boardSize.getHeight(), scrollpane
                    .getViewport().getSize().getHeight());
            Dimension dispDimension = new Dimension();
            dispDimension.setSize(width, height);
            // we need to adjust the point, because it should be against the
            // displayable dimension
            Point dispPoint = new Point();
            dispPoint.setLocation(point.x + getBounds().x, point.y
                    + getBounds().y);
            if (disp.isHit(dispPoint, dispDimension)) {
                return;
            }
        }
        mouseAction(getCoordsAt(point), BOARD_HEX_DRAG, me.getModifiers());
    }

    public void mouseReleased(MouseEvent me) {
        // don't show the popup if we are drag-scrolling
        if (me.isPopupTrigger() && !dragging) {
            mouseAction(getCoordsAt(me.getPoint()), BOARD_HEX_POPUP,
                    me.getModifiers());
            // stop scrolling
            shouldScroll = false;
            return;
        }

        // if we released the right mouse button, there's no more
        // scrolling
        if (SwingUtilities.isRightMouseButton(me)) {
            scrollXDifference = 0;
            scrollYDifference = 0;
            dragging = false;
            shouldScroll = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        for (int i = 0; i < displayables.size(); i++) {
            IDisplayable disp = displayables.get(i);
            if (disp.isReleased()) {
                return;
            }
        }

        if (me.getClickCount() == 1) {
            mouseAction(getCoordsAt(me.getPoint()), BOARD_HEX_CLICK,
                    me.getModifiers());
        } else {
            mouseAction(getCoordsAt(me.getPoint()), BOARD_HEX_DOUBLECLICK,
                    me.getModifiers());
        }
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseClicked(MouseEvent me) {
    }

    private class MovingUnit {
        public Entity entity;

        public ArrayList<UnitLocation> path;

        MovingUnit(Entity entity, Vector<UnitLocation> path) {
            this.entity = entity;
            this.path = new ArrayList<UnitLocation>(path);
        }
    }

    /**
     * Everything in the main map view is either the board or it's a sprite
     * displayed on top of the board. Most sprites store a transparent image
     * which they draw onto the screen when told to. Sprites keep a bounds
     * rectangle, so it's easy to tell when they'return onscreen.
     */
    abstract class Sprite implements ImageObserver {
        protected Rectangle bounds;

        protected Image image;

        // Set this to true if you don't want the sprite to be drawn.
        protected boolean hidden = false;

        /**
         * Do any necessary preparation. This is called after creation, but
         * before drawing, when a device context is ready to draw with.
         */
        public abstract void prepare();

        /**
         * When we draw our buffered images, it's necessary to implement the
         * ImageObserver interface. This provides the necesasry functionality.
         */
        public boolean imageUpdate(Image image, int infoflags, int x, int y,
                int width, int height) {
            if (infoflags == ImageObserver.ALLBITS) {
                prepare();
                repaint();
                return false;
            }
            return true;
        }

        /**
         * Returns our bounding rectangle. The coordinates here are stored with
         * the top left corner of the _board_ being 0, 0, so these do not always
         * correspond to screen coordinates.
         */
        public Rectangle getBounds() {
            return bounds;
        }

        /**
         * Are we ready to draw? By default, checks to see that our buffered
         * image has been created.
         */
        public boolean isReady() {
            return image != null;
        }

        /**
         * Draws this sprite onto the specified graphics context.
         */
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
            drawOnto(g, x, y, observer, false);
        }

        public void drawOnto(Graphics g, int x, int y, ImageObserver observer,
                boolean makeTranslucent) {
            if (isReady()) {
                if (makeTranslucent) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 0.5f));
                    g2.drawImage(image, x, y, observer);
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 1.0f));
                } else {
                    g.drawImage(image, x, y, observer);
                }
            } else {
                // grrr... we'll be ready next time!
                prepare();
            }
        }

        /**
         * Returns true if the point is inside this sprite. Uses board
         * coordinates, not screen coordinates. By default, just checks our
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
        public String[] getTooltip() {
            return null;
        }
    }

    /**
     * Sprite used for isometric rendering to render an entity partially hidden
     * behind a hill.
     * 
     */
    private class IsometricSprite extends Sprite {
        private Entity entity;
        private Rectangle modelRect;
        private int secondaryPos;

        public IsometricSprite(Entity entity, int secondaryPos) {
            this.entity = entity;
            this.secondaryPos = secondaryPos;
            String shortName = entity.getShortName();
            Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
            modelRect = new Rectangle(47, 55, getFontMetrics(font).stringWidth(
                    shortName) + 1, getFontMetrics(font).getAscent());

            int altAdjust = 0;
            if (useIsometric()
                    && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
                altAdjust = (int) (DROPSHDW_DIST * scale);
            } else if (useIsometric() && (entity.getElevation() != 0)
                    && !(entity instanceof GunEmplacement)) {
                altAdjust = (int) (entity.getElevation() * HEX_ELEV * scale);
            }

            Dimension dim = new Dimension(hex_size.width, hex_size.height
                    + altAdjust);
            Rectangle tempBounds = new Rectangle(dim).union(modelRect);

            if (secondaryPos == -1) {
                tempBounds.setLocation(getHexLocation(entity.getPosition()));
            } else {
                tempBounds.setLocation(getHexLocation(entity
                        .getSecondaryPositions().get(secondaryPos)));
            }
            if (entity.getElevation() > 0) {
                tempBounds.y = tempBounds.y - altAdjust;
            }
            bounds = tempBounds;
            image = null;
        }

        public Coords getPosition() {
            if (secondaryPos == -1) {
                return entity.getPosition();
            } else {
                return entity.getSecondaryPositions().get(secondaryPos);
            }
        }

        /**
         *
         */
        @Override
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer,
                boolean makeTranslucent) {
            if (isReady()) {
                Point p;
                if (secondaryPos == -1) {
                    p = getHexLocation(entity.getPosition());
                } else {
                    p = getHexLocation(entity.getSecondaryPositions().get(
                            secondaryPos));
                }
                Graphics2D g2 = (Graphics2D) g;
                if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
                    Image shadow = createShadowMask(tileManager.imageFor(
                            entity, entity.getFacing(), secondaryPos));

                    if (zoomIndex == BASE_ZOOM_INDEX) {
                        shadow = createImage(new FilteredImageSource(
                                shadow.getSource(), new KeyAlphaFilter(
                                        TRANSPARENT)));
                    } else {
                        shadow = getScaledImage(createImage(new FilteredImageSource(
                                shadow.getSource(), new KeyAlphaFilter(
                                        TRANSPARENT))),false);
                    }
                    // Draw airborne units in 2 passes. Shadow is rendered
                    // during the opaque pass, and the
                    // Actual unit is rendered during the transparent pass.
                    // However the unit is always drawn
                    // opaque.
                    if (makeTranslucent) {
                        g.drawImage(image, p.x, p.y
                                - (int) (DROPSHDW_DIST * scale), this);
                    } else {
                        g.drawImage(shadow, p.x, p.y, this);
                    }

                } else if ((entity.getElevation() != 0)
                        && !(entity instanceof GunEmplacement)) {
                    Image shadow = createShadowMask(tileManager.imageFor(
                            entity, entity.getFacing(), secondaryPos));

                    if (zoomIndex == BASE_ZOOM_INDEX) {
                        shadow = createImage(new FilteredImageSource(
                                shadow.getSource(), new KeyAlphaFilter(
                                        TRANSPARENT)));
                    } else {
                        shadow = getScaledImage(createImage(new FilteredImageSource(
                                shadow.getSource(), new KeyAlphaFilter(
                                        TRANSPARENT))),false);
                    }
                    // Entities on a bridge hex or submerged in water.
                    int altAdjust = (int) (entity.getElevation() * HEX_ELEV * scale);

                    if (makeTranslucent) {
                        if (entity.absHeight() < 0) {
                            g2.setComposite(AlphaComposite.getInstance(
                                    AlphaComposite.SRC_OVER, 0.35f));
                            g2.drawImage(image, p.x, p.y - altAdjust, observer);
                            g2.setComposite(AlphaComposite.getInstance(
                                    AlphaComposite.SRC_OVER, 1.0f));
                        } else {
                            g.drawImage(image, p.x, p.y - altAdjust, this);
                        }
                    } else {
                        g.drawImage(shadow, p.x, p.y, this);
                    }

                } else if (makeTranslucent) {
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 0.35f));
                    g2.drawImage(image, x, y, observer);
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 1.0f));
                } else {
                    g.drawImage(image, x, y, observer);
                }
            } else {
                prepare();
            }
        }

        @Override
        public void prepare() {
            Image tempImage;
            Graphics graph;
            try {
                tempImage = createImage(bounds.width, bounds.height);
                graph = tempImage.getGraphics();
            } catch (NullPointerException ex) {
                // argh! but I want it!
                return;
            }

            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);

            // draw entity image
            graph.drawImage(tileManager.imageFor(entity, secondaryPos), 0, 0,
                    this);

            // create final image
            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
            }
            graph.dispose();
            tempImage.flush();
        }

    }

    /**
     * Sprite for a cursor. Just a hexagon outline in a specified color.
     */
    private class CursorSprite extends Sprite {
        private Color color;

        private Coords hexLoc;

        public CursorSprite(final Color color) {
            this.color = color;
            bounds = new Rectangle(hexPoly.getBounds().width + 1,
                    hexPoly.getBounds().height + 1);
            image = null;

            // start offscreen
            setOffScreen();
        }

        @Override
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
            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
            }
            graph.dispose();
            tempImage.flush();
        }

        public void setOffScreen() {
            bounds.setLocation(-100, -100);
            hexLoc = new Coords(-2, -2);
        }

        public void setHexLocation(Coords hexLoc) {
            this.hexLoc = hexLoc;
            bounds.setLocation(getHexLocation(hexLoc));
        }

        @Override
        public Rectangle getBounds() {
            bounds = new Rectangle(hexPoly.getBounds().width + 1,
                    hexPoly.getBounds().height + 1);
            bounds.setLocation(getHexLocation(hexLoc));

            return bounds;
        }
    }

    private class GhostEntitySprite extends Sprite {
        private Entity entity;

        private Rectangle modelRect;

        public GhostEntitySprite(final Entity entity) {
            this.entity = entity;

            String shortName = entity.getShortName();
            Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
            modelRect = new Rectangle(47, 55, getFontMetrics(font).stringWidth(
                    shortName) + 1, getFontMetrics(font).getAscent());
            Rectangle tempBounds = new Rectangle(hex_size).union(modelRect);
            tempBounds.setLocation(getHexLocation(entity.getPosition()));

            bounds = tempBounds;
            image = null;
        }

        /**
         * Creates the sprite for this entity. It is an extra pain to create
         * transparent images in AWT.
         */
        @Override
        public void prepare() {
            // create image for buffer
            Image tempImage;
            Graphics graph;
            try {
                tempImage = createImage(bounds.width, bounds.height);
                graph = tempImage.getGraphics();
            } catch (NullPointerException ex) {
                // argh! but I want it!
                return;
            }

            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);

            // draw entity image
            graph.drawImage(tileManager.imageFor(entity), 0, 0, this);

            // create final image
            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
            }
            graph.dispose();
            tempImage.flush();
        }

        @Override
        public Rectangle getBounds() {
            Rectangle tempBounds = new Rectangle(hex_size).union(modelRect);
            tempBounds.setLocation(getHexLocation(entity.getPosition()));
            bounds = tempBounds;

            return bounds;
        }

        @Override
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
            drawOnto(g, x, y, observer, true);
        }

    }

    private class MovingEntitySprite extends Sprite {
        private int facing;

        private Entity entity;

        private Rectangle modelRect;

        private int elevation;

        public MovingEntitySprite(final Entity entity, final Coords position,
                final int facing, final int elevation) {
            this.entity = entity;
            this.facing = facing;
            this.elevation = elevation;

            String shortName = entity.getShortName();
            Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
            modelRect = new Rectangle(47, 55, getFontMetrics(font).stringWidth(
                    shortName) + 1, getFontMetrics(font).getAscent());

            int altAdjust = 0;
            if (useIsometric()
                    && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
                altAdjust = (int) (DROPSHDW_DIST * scale);
            } else if (useIsometric() && (elevation != 0)) {
                altAdjust = (int) (elevation * HEX_ELEV * scale);
            }

            Dimension dim = new Dimension(hex_size.width, hex_size.height
                    + altAdjust);
            Rectangle tempBounds = new Rectangle(dim).union(modelRect);

            tempBounds.setLocation(getHexLocation(position));
            if (elevation > 0) {
                tempBounds.y = tempBounds.y - altAdjust;
            }
            bounds = tempBounds;
            image = null;
        }

        @Override
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
            // If this is an airborne unit, render the shadow.
            if (useIsometric()
                    && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
                Image shadow = createShadowMask(tileManager.imageFor(entity,
                        facing, -1));

                if (zoomIndex == BASE_ZOOM_INDEX) {
                    shadow = createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(TRANSPARENT)));
                } else {
                    shadow = getScaledImage(createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
                }

                g.drawImage(shadow, x, y + (int) (DROPSHDW_DIST * scale),
                        observer);
            } else if (elevation > 0) {
                Image shadow = createShadowMask(tileManager.imageFor(entity,
                        facing, -1));

                if (zoomIndex == BASE_ZOOM_INDEX) {
                    shadow = createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(TRANSPARENT)));
                } else {
                    shadow = getScaledImage(createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
                }

                g.drawImage(shadow, x,
                        y + (int) (elevation * HEX_ELEV * scale), observer);
            }
            // submerged?
            if (useIsometric() && ((elevation + entity.getHeight()) < 0)) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.35f));
                g2.drawImage(image, x,
                        y - (int) (elevation * HEX_ELEV * scale), observer);
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 1.0f));
            } else {
                // create final image
                drawOnto(g, x, y, observer, false);
            }
            // If this is a submerged unit, render the shadow after the unit.
            if (useIsometric() && (elevation < 0)) {
                Image shadow = createShadowMask(tileManager.imageFor(entity,
                        facing, -1));

                if (zoomIndex == BASE_ZOOM_INDEX) {
                    shadow = createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(TRANSPARENT)));
                } else {
                    shadow = getScaledImage(createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
                }

                g.drawImage(shadow, x, y, observer);
            }
        }

        /**
         * Creates the sprite for this entity. It is an extra pain to create
         * transparent images in AWT.
         */
        @Override
        public void prepare() {
            // create image for buffer
            Image tempImage;
            Graphics graph;
            try {
                tempImage = createImage(bounds.width, bounds.height);
                graph = tempImage.getGraphics();
            } catch (NullPointerException ex) {
                // argh! but I want it!
                return;
            }

            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);
            graph.drawImage(tileManager.imageFor(entity, facing, -1), 0, 0,
                    this);

            // create final image
            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
            }
            graph.dispose();
            tempImage.flush();
        }
    }

    /**
     * Sprite for an wreck. Consists of an image, drawn from the Tile Manager
     * and an identification label.
     */
    private class IsometricWreckSprite extends Sprite {
        private Entity entity;

        private Rectangle modelRect;

        private int secondaryPos;

        public IsometricWreckSprite(final Entity entity, int secondaryPos) {
            this.entity = entity;
            this.secondaryPos = secondaryPos;

            String shortName = entity.getShortName();

            Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
            modelRect = new Rectangle(47, 55, getFontMetrics(font).stringWidth(
                    shortName) + 1, getFontMetrics(font).getAscent());
            int altAdjust = 0;
            if (useIsometric()
                    && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
                altAdjust = (int) (DROPSHDW_DIST * scale);
            } else if (useIsometric() && (entity.getElevation() != 0)) {
                altAdjust = (int) (entity.getElevation() * HEX_ELEV * scale);
            }

            Dimension dim = new Dimension(hex_size.width, hex_size.height
                    + altAdjust);
            Rectangle tempBounds = new Rectangle(dim).union(modelRect);

            if (secondaryPos == -1) {
                tempBounds.setLocation(getHexLocation(entity.getPosition()));
            } else {
                tempBounds.setLocation(getHexLocation(entity
                        .getSecondaryPositions().get(secondaryPos)));
            }
            if (entity.getElevation() > 0) {
                tempBounds.y = tempBounds.y - altAdjust;
            }
            bounds = tempBounds;
            image = null;
        }

        @Override
        public Rectangle getBounds() {
            Rectangle tempBounds = new Rectangle(hex_size).union(modelRect);
            tempBounds.setLocation(getHexLocation(entity.getPosition()));
            bounds = tempBounds;

            return bounds;
        }

        /**
        *
        */
        @Override
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer,
                boolean makeTranslucent) {
            if (isReady()) {
                Graphics2D g2 = (Graphics2D) g;
                if (makeTranslucent) {
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 0.35f));
                    g2.drawImage(image, x, y, observer);
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 1.0f));
                } else {
                    g.drawImage(image, x, y, observer);
                }
            } else {
                prepare();
            }
        }

        public Entity getEntity() {
            return entity;
        }

        /**
         * Creates the sprite for this entity. It is an extra pain to create
         * transparent images in AWT.
         */
        @Override
        public void prepare() {
            // figure out size
            String shortName = entity.getShortName();
            Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
            Rectangle tempRect = new Rectangle(47, 55, getFontMetrics(font)
                    .stringWidth(shortName) + 1, getFontMetrics(font)
                    .getAscent());

            // create image for buffer
            Image tempImage;
            Graphics graph;
            try {
                tempImage = createImage(bounds.width, bounds.height);
                graph = tempImage.getGraphics();
            } catch (NullPointerException ex) {
                // argh! but I want it!
                return;
            }

            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);

            // Draw wreck image,if we've got one.
            Image wreck = tileManager.wreckMarkerFor(entity, -1);
            if (null != wreck) {
                graph.drawImage(wreck, 0, 0, this);
            }

            if (secondaryPos == -1) {
                // draw box with shortName
                Color text = Color.lightGray;
                Color bkgd = Color.darkGray;
                Color bord = Color.black;

                graph.setFont(font);
                graph.setColor(bord);
                graph.fillRect(tempRect.x, tempRect.y, tempRect.width,
                        tempRect.height);
                tempRect.translate(-1, -1);
                graph.setColor(bkgd);
                graph.fillRect(tempRect.x, tempRect.y, tempRect.width,
                        tempRect.height);
                graph.setColor(text);
                graph.drawString(shortName, tempRect.x + 1,
                        (tempRect.y + tempRect.height) - 1);
            }

            // create final image
            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
            }
            graph.dispose();
            tempImage.flush();
        }

        /**
         * Overrides to provide for a smaller sensitive area.
         */
        @Override
        public boolean isInside(Point point) {
            return false;
        }

    }

    /**
     * Sprite for an wreck. Consists of an image, drawn from the Tile Manager
     * and an identification label.
     */
    private class WreckSprite extends Sprite {
        private Entity entity;

        private Rectangle modelRect;

        private int secondaryPos;

        public WreckSprite(final Entity entity, int secondaryPos) {
            this.entity = entity;
            this.secondaryPos = secondaryPos;

            String shortName = entity.getShortName();

            Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
            modelRect = new Rectangle(47, 55, getFontMetrics(font).stringWidth(
                    shortName) + 1, getFontMetrics(font).getAscent());
            Rectangle tempBounds = new Rectangle(hex_size).union(modelRect);
            if (secondaryPos == -1) {
                tempBounds.setLocation(getHexLocation(entity.getPosition()));
            } else {
                tempBounds.setLocation(getHexLocation(entity
                        .getSecondaryPositions().get(secondaryPos)));
            }

            bounds = tempBounds;
            image = null;
        }

        @Override
        public Rectangle getBounds() {
            Rectangle tempBounds = new Rectangle(hex_size).union(modelRect);
            tempBounds.setLocation(getHexLocation(entity.getPosition()));
            bounds = tempBounds;

            return bounds;
        }

        /**
         * Creates the sprite for this entity. It is an extra pain to create
         * transparent images in AWT.
         */
        @Override
        public void prepare() {
            // figure out size
            String shortName = entity.getShortName();
            Font font = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
            Rectangle tempRect = new Rectangle(47, 55, getFontMetrics(font)
                    .stringWidth(shortName) + 1, getFontMetrics(font)
                    .getAscent());

            // create image for buffer
            Image tempImage;
            Graphics graph;
            try {
                tempImage = createImage(bounds.width, bounds.height);
                graph = tempImage.getGraphics();
            } catch (NullPointerException ex) {
                // argh! but I want it!
                return;
            }

            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);

            // Draw wreck image,if we've got one.
            Image wreck = tileManager.wreckMarkerFor(entity, -1);
            if (null != wreck) {
                graph.drawImage(wreck, 0, 0, this);
            }

            if (secondaryPos == -1) {
                // draw box with shortName
                Color text = Color.lightGray;
                Color bkgd = Color.darkGray;
                Color bord = Color.black;

                graph.setFont(font);
                graph.setColor(bord);
                graph.fillRect(tempRect.x, tempRect.y, tempRect.width,
                        tempRect.height);
                tempRect.translate(-1, -1);
                graph.setColor(bkgd);
                graph.fillRect(tempRect.x, tempRect.y, tempRect.width,
                        tempRect.height);
                graph.setColor(text);
                graph.drawString(shortName, tempRect.x + 1,
                        (tempRect.y + tempRect.height) - 1);
            }

            // create final image
            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
            }
            graph.dispose();
            tempImage.flush();
        }

        /**
         * Overrides to provide for a smaller sensitive area.
         */
        @Override
        public boolean isInside(Point point) {
            return false;
        }

    }

    /**
     * Sprite for an entity. Changes whenever the entity changes. Consists of an
     * image, drawn from the Tile Manager; facing and possibly secondary facing
     * arrows; armor and internal bars; and an identification label.
     */
    private class EntitySprite extends Sprite {
        private Entity entity;

        private Rectangle entityRect;

        private Rectangle modelRect;

        private int secondaryPos;

        public EntitySprite(final Entity entity, int secondaryPos) {
            this.entity = entity;
            this.secondaryPos = secondaryPos;

            String shortName = entity.getShortName();

            if (entity.getMovementMode() == EntityMovementMode.VTOL) {
                shortName = shortName.concat(" (FL: ")
                        .concat(Integer.toString(entity.getElevation()))
                        .concat(")");
            }
            if (entity.getMovementMode() == EntityMovementMode.SUBMARINE) {
                shortName = shortName.concat(" (Depth: ")
                        .concat(Integer.toString(entity.getElevation()))
                        .concat(")");
            }
            int face = entity.isCommander() ? Font.ITALIC : Font.PLAIN;
            Font font = new Font("SansSerif", face, 10); //$NON-NLS-1$
            modelRect = new Rectangle(47, 55, getFontMetrics(font).stringWidth(
                    shortName) + 1, getFontMetrics(font).getAscent());

            int altAdjust = 0;
            if (useIsometric()
                    && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
                altAdjust = (int) (DROPSHDW_DIST * scale);
            } else if (useIsometric() && (entity.getElevation() != 0)
                    && !(entity instanceof GunEmplacement)) {
                altAdjust = (int) (entity.getElevation() * HEX_ELEV * scale);
            }

            Dimension dim = new Dimension(hex_size.width, hex_size.height
                    + altAdjust);
            Rectangle tempBounds = new Rectangle(dim).union(modelRect);
            if (secondaryPos == -1) {
                tempBounds.setLocation(getHexLocation(entity.getPosition()));
            } else {
                tempBounds.setLocation(getHexLocation(entity
                        .getSecondaryPositions().get(secondaryPos)));
            }

            if (entity.getElevation() > 0) {
                tempBounds.y = tempBounds.y - altAdjust;
            }
            bounds = tempBounds;
            entityRect = new Rectangle(bounds.x + (int) (20 * scale), bounds.y
                    + (int) (14 * scale), (int) (44 * scale),
                    (int) (44 * scale));
            image = null;
        }

        @Override
        public Rectangle getBounds() {

            int altAdjust = 0;
            if (useIsometric()
                    && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
                altAdjust = (int) (DROPSHDW_DIST * scale);
            } else if (useIsometric() && (entity.getElevation() != 0)
                    && !(entity instanceof GunEmplacement)) {
                altAdjust = (int) (entity.getElevation() * HEX_ELEV * scale);
            }

            Dimension dim = new Dimension(hex_size.width, hex_size.height
                    + altAdjust);
            Rectangle tempBounds = new Rectangle(dim).union(modelRect);
            if (secondaryPos == -1) {
                tempBounds.setLocation(getHexLocation(entity.getPosition()));
            } else {
                tempBounds.setLocation(getHexLocation(entity
                        .getSecondaryPositions().get(secondaryPos)));
            }
            if (entity.getElevation() > 0) {
                tempBounds.y = tempBounds.y - altAdjust;
            }
            bounds = tempBounds;
            entityRect = new Rectangle(bounds.x + (int) (20 * scale), bounds.y
                    + (int) (14 * scale), (int) (44 * scale),
                    (int) (44 * scale));

            return bounds;
        }

        @Override
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
            if ((trackThisEntitiesVisibilityInfo(entity)
                    && !entity.isVisibleToEnemy() && GUIPreferences
                    .getInstance().getBoolean(
                            GUIPreferences.ADVANCED_TRANSLUCENT_HIDDEN_UNITS))
                    || (entity.absHeight() < 0)) {
                // create final image with translucency
                drawOnto(g, x, y, observer, true);
            } else {
                drawOnto(g, x, y, observer, false);
            }
        }

        /**
         * Creates the sprite for this entity. It is an extra pain to create
         * transparent images in AWT.
         */
        @Override
        public void prepare() {
            // figure out size
            String shortName = entity.getShortName();
            if (entity.getMovementMode() == EntityMovementMode.VTOL) {
                shortName = shortName.concat(" (FL: ")
                        .concat(Integer.toString(entity.getElevation()))
                        .concat(")");
            }
            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                shortName += (Messages.getString("BoardView1.ID") + entity.getId()); //$NON-NLS-1$
            }
            int face = entity.isCommander() ? Font.ITALIC : Font.PLAIN;
            Font font = new Font("SansSerif", face, 10); //$NON-NLS-1$
            Rectangle tempRect = new Rectangle(47, 55, getFontMetrics(font)
                    .stringWidth(shortName) + 1, getFontMetrics(font)
                    .getAscent());

            // create image for buffer
            Image tempImage;
            Graphics graph;
            try {
                tempImage = createImage(bounds.width, bounds.height);
                // fill with key color
                graph = tempImage.getGraphics();
            } catch (NullPointerException ex) {
                // argh! but I want it!
                return;
            }

            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);
            if (!useIsometric()) {
                // The entity sprite is drawn when the hexes are rendered.
                // So do not include the sprite info here.
                graph.drawImage(tileManager.imageFor(entity, secondaryPos), 0,
                        0, this);
            }
            if ((secondaryPos == -1) || (secondaryPos == 6)) {
                // draw box with shortName
                Color text, bkgd, bord;
                if (entity.isDone()) {
                    text = Color.lightGray;
                    bkgd = Color.darkGray;
                    bord = Color.black;
                } else if (entity.isImmobile()) {
                    text = Color.darkGray;
                    bkgd = Color.black;
                    bord = Color.lightGray;
                } else {
                    text = Color.black;
                    bkgd = Color.lightGray;
                    bord = Color.darkGray;
                }
                graph.setFont(font);
                graph.setColor(bord);
                graph.fillRect(tempRect.x, tempRect.y, tempRect.width,
                        tempRect.height);
                tempRect.translate(-1, -1);
                graph.setColor(bkgd);
                graph.fillRect(tempRect.x, tempRect.y, tempRect.width,
                        tempRect.height);
                graph.setColor(text);
                graph.drawString(shortName, tempRect.x + 1,
                        (tempRect.y + tempRect.height) - 1);

                // draw facing
                graph.setColor(Color.white);
                if ((entity.getFacing() != -1)
                        && !((entity instanceof Infantry) && (((Infantry) entity)
                                .getDugIn() == Infantry.DUG_IN_NONE))
                        && !((entity instanceof Aero)
                                && ((Aero) entity).isSpheroid() && !game
                                .getBoard().inSpace())) {
                    graph.drawPolygon(facingPolys[entity.getFacing()]);
                }

                // determine secondary facing for non-mechs & flipped arms
                int secFacing = entity.getFacing();
                if (!((entity instanceof Mech) || (entity instanceof Protomech))) {
                    secFacing = entity.getSecondaryFacing();
                } else if (entity.getArmsFlipped()) {
                    secFacing = (entity.getFacing() + 3) % 6;
                }
                // draw red secondary facing arrow if necessary
                if ((secFacing != -1) && (secFacing != entity.getFacing())) {
                    graph.setColor(Color.red);
                    graph.drawPolygon(facingPolys[secFacing]);
                }
                if ((entity instanceof Aero) && game.useVectorMove()) {
                    for (int head : entity.getHeading()) {
                        graph.setColor(Color.red);
                        graph.drawPolygon(facingPolys[head]);
                    }
                }

                // Determine if the entity has a locked turret,
                // and if it is a gun emplacement
                boolean turretLocked = false;
                // boolean turretJammed = false;
                int crewStunned = 0;
                boolean ge = false;
                if (entity instanceof Tank) {
                    turretLocked = !((Tank) entity).hasNoTurret()
                            && !entity.canChangeSecondaryFacing();
                    crewStunned = ((Tank) entity).getStunnedTurns();
                    ge = entity instanceof GunEmplacement;
                }

                // draw condition strings

                // draw elevation/altitude if non-zero
                if (entity.isAirborne()) {
                    if (!game.getBoard().inSpace()) {
                        graph.setColor(Color.darkGray);
                        graph.drawString(Integer.toString(entity.getAltitude())
                                + "A", 26, 15);
                        graph.setColor(Color.PINK);
                        graph.drawString(Integer.toString(entity.getAltitude())
                                + "A", 25, 14);
                    }
                } else if (entity.getElevation() != 0) {
                    graph.setColor(Color.darkGray);
                    graph.drawString(Integer.toString(entity.getElevation()),
                            26, 15);
                    graph.setColor(Color.PINK);
                    graph.drawString(Integer.toString(entity.getElevation()),
                            25, 14);
                }

                if (entity instanceof Aero) {
                    Aero a = (Aero) entity;

                    if (a.isRolled()) {
                        // draw "rolled"
                        graph.setColor(Color.darkGray);
                        graph.drawString(
                                Messages.getString("BoardView1.ROLLED"), 18, 15); //$NON-NLS-1$
                        graph.setColor(Color.red);
                        graph.drawString(
                                Messages.getString("BoardView1.ROLLED"), 17, 14); //$NON-NLS-1$
                    }

                    if (a.isOutControlTotal() & a.isRandomMove()) {
                        // draw "RANDOM"
                        graph.setColor(Color.darkGray);
                        graph.drawString(
                                Messages.getString("BoardView1.RANDOM"), 18, 35); //$NON-NLS-1$
                        graph.setColor(Color.red);
                        graph.drawString(
                                Messages.getString("BoardView1.RANDOM"), 17, 34); //$NON-NLS-1$
                    } else if (a.isOutControlTotal()) {
                        // draw "CONTROL"
                        graph.setColor(Color.darkGray);
                        graph.drawString(
                                Messages.getString("BoardView1.CONTROL"), 18, 39); //$NON-NLS-1$
                        graph.setColor(Color.red);
                        graph.drawString(
                                Messages.getString("BoardView1.CONTROL"), 17, 38); //$NON-NLS-1$
                    }

                    if (a.isEvading()) {
                        // draw "EVADE" - can't overlap with out of control
                        graph.setColor(Color.darkGray);
                        graph.drawString(
                                Messages.getString("BoardView1.EVADE"), 18, 39); //$NON-NLS-1$
                        graph.setColor(Color.red);
                        graph.drawString(
                                Messages.getString("BoardView1.EVADE"), 17, 38); //$NON-NLS-1$
                    }
                }

                if (entity.getCrew().isDead()) {
                    // draw "CREW DEAD"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.CrewDead"), 18, 39); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.CrewDead"), 17, 38); //$NON-NLS-1$
                } else if (!ge && entity.isImmobile()) {
                    if (entity.isProne()) {
                        // draw "IMMOBILE" and "PRONE"
                        graph.setColor(Color.darkGray);
                        graph.drawString(
                                Messages.getString("BoardView1.IMMOBILE"), 18, 35); //$NON-NLS-1$
                        graph.drawString(
                                Messages.getString("BoardView1.PRONE"), 26, 48); //$NON-NLS-1$
                        graph.setColor(Color.red);
                        graph.drawString(
                                Messages.getString("BoardView1.IMMOBILE"), 17, 34); //$NON-NLS-1$
                        graph.setColor(Color.yellow);
                        graph.drawString(
                                Messages.getString("BoardView1.PRONE"), 25, 47); //$NON-NLS-1$
                    } else if (crewStunned > 0) {
                        // draw IMMOBILE and STUNNED
                        graph.setColor(Color.darkGray);
                        graph.drawString(
                                Messages.getString("BoardView1.IMMOBILE"), 18, 35); //$NON-NLS-1$
                        graph.drawString(
                                Messages.getString(
                                        "BoardView1.STUNNED", new Object[] { crewStunned }), 22, 48); //$NON-NLS-1$
                        graph.setColor(Color.red);
                        graph.drawString(
                                Messages.getString("BoardView1.IMMOBILE"), 17, 34); //$NON-NLS-1$
                        graph.setColor(Color.yellow);
                        graph.drawString(
                                Messages.getString(
                                        "BoardView1.STUNNED", new Object[] { crewStunned }), 21, 47); //$NON-NLS-1$
                    } else if (turretLocked) {
                        // draw "IMMOBILE" and "LOCKED"
                        graph.setColor(Color.darkGray);
                        graph.drawString(
                                Messages.getString("BoardView1.IMMOBILE"), 18, 35); //$NON-NLS-1$
                        graph.drawString(
                                Messages.getString("BoardView1.LOCKED"), 22, 48); //$NON-NLS-1$
                        graph.setColor(Color.red);
                        graph.drawString(
                                Messages.getString("BoardView1.IMMOBILE"), 17, 34); //$NON-NLS-1$
                        graph.setColor(Color.yellow);
                        graph.drawString(
                                Messages.getString("BoardView1.LOCKED"), 21, 47); //$NON-NLS-1$
                    } else {
                        // draw "IMMOBILE"
                        graph.setColor(Color.darkGray);
                        graph.drawString(
                                Messages.getString("BoardView1.IMMOBILE"), 18, 39); //$NON-NLS-1$
                        graph.setColor(Color.red);
                        graph.drawString(
                                Messages.getString("BoardView1.IMMOBILE"), 17, 38); //$NON-NLS-1$
                    }
                } else if (entity.isProne()) {
                    // draw "PRONE"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.PRONE"), 26, 39); //$NON-NLS-1$
                    graph.setColor(Color.yellow);
                    graph.drawString(
                            Messages.getString("BoardView1.PRONE"), 25, 38); //$NON-NLS-1$
                } else if (crewStunned > 0) {
                    // draw STUNNED
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString(
                                    "BoardView1.STUNNED", new Object[] { crewStunned }), 22, 48); //$NON-NLS-1$
                    graph.setColor(Color.yellow);
                    graph.drawString(
                            Messages.getString(
                                    "BoardView1.STUNNED", new Object[] { crewStunned }), 21, 47); //$NON-NLS-1$
                } else if (turretLocked) {
                    // draw "LOCKED"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.LOCKED"), 22, 39); //$NON-NLS-1$
                    graph.setColor(Color.yellow);
                    graph.drawString(
                            Messages.getString("BoardView1.LOCKED"), 21, 38); //$NON-NLS-1$
                }

                // If this unit is shutdown, say so.
                if (entity.isManualShutdown()) {
                    // draw "SHUTDOWN" for manual
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.SHUTDOWN"), 50, 71); //$NON-NLS-1$
                    graph.setColor(Color.yellow);
                    graph.drawString(
                            Messages.getString("BoardView1.SHUTDOWN"), 49, 70); //$NON-NLS-1$
                } else if (entity.isShutDown()) {
                    // draw "SHUTDOWN" for manual
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.SHUTDOWN"), 50, 71); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.SHUTDOWN"), 49, 70); //$NON-NLS-1$
                }

                // If this unit is being swarmed or is swarming another, say so.
                if (Entity.NONE != entity.getSwarmAttackerId()) {
                    // draw "SWARMED"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.SWARMED"), 17, 22); //$NON-NLS-1$
                    graph.setColor(Color.red);
                    graph.drawString(
                            Messages.getString("BoardView1.SWARMED"), 16, 21); //$NON-NLS-1$
                }

                // If this unit is transporting another, say so.
                if ((entity.getLoadedUnits()).size() > 0) {
                    // draw "T"
                    graph.setColor(Color.darkGray);
                    graph.drawString("T", 20, 71); //$NON-NLS-1$
                    graph.setColor(Color.black);
                    graph.drawString("T", 19, 70); //$NON-NLS-1$
                }

                // If this unit is stuck, say so.
                if ((entity.isStuck())) {
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("BoardView1.STUCK"), 26, 61); //$NON-NLS-1$
                    graph.setColor(Color.orange);
                    graph.drawString(
                            Messages.getString("BoardView1.STUCK"), 25, 60); //$NON-NLS-1$

                }

                // If this unit is currently unknown to the enemy, say so.
                if (trackThisEntitiesVisibilityInfo(entity)) {
                    if (!entity.isSeenByEnemy()) {
                        // draw "U"
                        graph.setColor(Color.darkGray);
                        graph.drawString("U", 30, 71); //$NON-NLS-1$
                        graph.setColor(Color.black);
                        graph.drawString("U", 29, 70); //$NON-NLS-1$
                    } else if (!entity.isVisibleToEnemy()
                            && !GUIPreferences
                                    .getInstance()
                                    .getBoolean(
                                            GUIPreferences.ADVANCED_TRANSLUCENT_HIDDEN_UNITS)) {
                        // If this unit is currently hidden from the enemy, say
                        // so.
                        // draw "H"
                        graph.setColor(Color.darkGray);
                        graph.drawString("H", 30, 71); //$NON-NLS-1$
                        graph.setColor(Color.black);
                        graph.drawString("H", 29, 70); //$NON-NLS-1$
                    }
                }

                // If hull down, show
                if (entity.isHullDown()) {
                    // draw "D"
                    graph.setColor(Color.darkGray);
                    graph.drawString(
                            Messages.getString("UnitOverview.HULLDOWN"), 15, 39); //$NON-NLS-1$
                    graph.setColor(Color.yellow);
                    graph.drawString(
                            Messages.getString("UnitOverview.HULLDOWN"), 14, 38); //$NON-NLS-1$
                } else if (entity instanceof Infantry) {
                    int dig = ((Infantry) entity).getDugIn();
                    if (dig == Infantry.DUG_IN_COMPLETE) {
                        // draw "D"
                        graph.setColor(Color.darkGray);
                        graph.drawString("D", 40, 71); //$NON-NLS-1$
                        graph.setColor(Color.black);
                        graph.drawString("D", 39, 70); //$NON-NLS-1$
                    } else if (dig != Infantry.DUG_IN_NONE) {
                        // draw "W"
                        graph.setColor(Color.darkGray);
                        graph.drawString("W", 40, 71); //$NON-NLS-1$
                        graph.setColor(Color.black);
                        graph.drawString("W", 39, 70); //$NON-NLS-1$
                    }
                }

                // Lets draw our armor and internal status bars
                int baseBarLength = 23;
                int barLength = 0;
                double percentRemaining = 0.00;

                percentRemaining = entity.getArmorRemainingPercent();
                barLength = (int) (baseBarLength * percentRemaining);

                graph.setColor(Color.darkGray);
                graph.fillRect(56, 7, 23, 3);
                graph.setColor(Color.lightGray);
                graph.fillRect(55, 6, 23, 3);
                graph.setColor(getStatusBarColor(percentRemaining));
                graph.fillRect(55, 6, barLength, 3);

                if (!ge) {
                    // Gun emplacements don't have internal structure
                    percentRemaining = entity.getInternalRemainingPercent();
                    barLength = (int) (baseBarLength * percentRemaining);

                    graph.setColor(Color.darkGray);
                    graph.fillRect(56, 11, 23, 3);
                    graph.setColor(Color.lightGray);
                    graph.fillRect(55, 10, 23, 3);
                    graph.setColor(getStatusBarColor(percentRemaining));
                    graph.fillRect(55, 10, barLength, 3);
                }

                if (game.getOptions().booleanOption("show_dmg_level")) {
                    Color damageColor = getDamageColor();
                    if (damageColor != null) {
                        graph.setColor(damageColor);
                        graph.fillOval(20, 15, 12, 12);
                    }
                }
            }

            // create final image
            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
            }
            graph.dispose();
            tempImage.flush();
        }

        private Color getDamageColor() {
            switch (entity.getDamageLevel()) {
                case Entity.DMG_CRIPPLED:
                    return Color.black;
                case Entity.DMG_HEAVY:
                    return Color.red;
                case Entity.DMG_MODERATE:
                    return Color.yellow;
                case Entity.DMG_LIGHT:
                    return Color.green;
            }
            return null;
        }

        /**
         * We only want to show double-blind visibility indicators on our own
         * mechs and teammates mechs (assuming team vision option).
         */
        private boolean trackThisEntitiesVisibilityInfo(Entity e) {
            if (getLocalPlayer() == null) {
                return false;
            }

            if (game.getOptions().booleanOption("double_blind") //$NON-NLS-1$
                    && ((e.getOwner().getId() == getLocalPlayer().getId()) || (game
                            .getOptions().booleanOption("team_vision") //$NON-NLS-1$
                    && (e.getOwner().getTeam() == getLocalPlayer().getTeam())))) {
                return true;
            }
            return false;
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

        /**
         * Overrides to provide for a smaller sensitive area.
         */
        @Override
        public boolean isInside(Point point) {
            return entityRect.contains(point.x, point.y);
        }

        @Override
        public String[] getTooltip() {
            String[] tipStrings = new String[4];
            StringBuffer buffer;

            buffer = new StringBuffer();
            buffer.append(entity.getChassis()).append(" (") //$NON-NLS-1$
                    .append(entity.getOwner().getName()).append(")"); //$NON-NLS-1$
            tipStrings[0] = buffer.toString();

            boolean hasNick = ((null != entity.getCrew().getNickname()) && !entity
                    .getCrew().getNickname().equals(""));
            buffer = new StringBuffer();
            buffer.append(Messages.getString("BoardView1.pilot"));
            if (hasNick) {
                buffer.append(" '").append(entity.getCrew().getNickname())
                        .append("'");
            }
            buffer.append(" (").append(entity.getCrew().getGunnery())
                    .append("/") //$NON-NLS-1$
                    .append(entity.getCrew().getPiloting()).append(")")
                    .append("; ").append(entity.getCrew().getStatusDesc());
            int numAdv = entity.getCrew().countOptions(
                    PilotOptions.LVL3_ADVANTAGES);
            boolean isMD = entity.getCrew().countOptions(
                    PilotOptions.MD_ADVANTAGES) > 0;
            if (numAdv > 0) {
                buffer.append(" <") //$NON-NLS-1$
                        .append(numAdv)
                        .append(Messages.getString("BoardView1.advs")); //$NON-NLS-1$
            }
            if (isMD) {
                buffer.append(Messages.getString("BoardView1.md")); //$NON-NLS-1$
            }
            tipStrings[1] = buffer.toString();

            GunEmplacement ge = null;
            if (entity instanceof GunEmplacement) {
                ge = (GunEmplacement) entity;
            }

            buffer = new StringBuffer();
            if (ge == null) {
                buffer.append(Messages.getString("BoardView1.move")) //$NON-NLS-1$
                        .append(entity.getMovementAbbr(entity.moved))
                        .append(":") //$NON-NLS-1$
                        .append(entity.delta_distance)
                        .append(" (+") //$NON-NLS-1$
                        .append(Compute.getTargetMovementModifier(game,
                                entity.getId()).getValue()).append(")") //$NON-NLS-1$
                        .append(entity.isEvading() ? Messages
                                .getString("BoardView1.Evade") : "")//$NON-NLS-1$ //$NON-NLS-2$
                        .append(";") //$NON-NLS-1$
                        .append(Messages.getString("BoardView1.Heat")) //$NON-NLS-1$
                        .append(entity.heat);
                if (entity.isCharging()) {
                    buffer.append(" ") //$NON-NLS-1$
                            .append(Messages.getString("BoardView1.charge1")); //$NON-NLS-1$
                }
                if (entity.isMakingDfa()) {
                    buffer.append(" ") //$NON-NLS-1$
                            .append(Messages.getString("BoardView1.DFA1")); //$NON-NLS-1$
                }
            } else {
                if (ge.isTurret() && ge.isTurretLocked(ge.getLocTurret())) {
                    buffer.append(Messages.getString("BoardView1.TurretLocked"));
                    if (ge.getFirstWeapon() == -1) {
                        buffer.append(",");
                        buffer.append(Messages
                                .getString("BoardView1.WeaponsDestroyed"));
                    }
                } else if (ge.getFirstWeapon() == -1) {
                    buffer.append(Messages
                            .getString("BoardView1.WeaponsDestroyed"));
                } else {
                    buffer.append(Messages.getString("BoardView1.Operational"));
                }
            }
            if (entity.isDone()) {
                buffer.append(" (")
                        .append(Messages.getString("BoardView1.done"))
                        .append(")");
            }
            tipStrings[2] = buffer.toString();

            buffer = new StringBuffer();
            if (ge == null) {
                buffer.append(Messages.getString("BoardView1.Armor")) //$NON-NLS-1$
                        .append(entity.getTotalArmor())
                        .append(Messages.getString("BoardView1.internal")) //$NON-NLS-1$
                        .append(entity.getTotalInternal());
            }
            /*
             * else { buffer.append(Messages.getString("BoardView1.cf"))
             * //$NON-NLS-1$ .append(ge.getCurrentCF()).append(
             * Messages.getString("BoardView1.turretArmor")) //$NON-NLS-1$
             * .append(ge.getCurrentTurretArmor()); }
             */
            tipStrings[3] = buffer.toString();

            return tipStrings;
        }
    }

    /**
     * Sprite for a step in a movement path. Only one sprite should exist for
     * any hex in a path. Contains a colored number, and arrows indicating
     * entering, exiting or turning.
     */
    private class StepSprite extends Sprite {
        private MoveStep step;
        private Image baseScaleImage;

        public StepSprite(final MoveStep step) {
            this.step = step;

            // step is the size of the hex that this step is in
            bounds = new Rectangle(getHexLocation(step.getPosition()), hex_size);
            image = null;
            baseScaleImage = null;
        }

        /**
         * Refreshes this StepSprite's image to handle changes in the zoom
         * level.
         */
        public void refreshZoomLevel() {

            if (baseScaleImage == null) {
                return;
            }

            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        baseScaleImage.getSource(), new KeyAlphaFilter(
                                TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        baseScaleImage.getSource(), new KeyAlphaFilter(
                                TRANSPARENT))),false);
            }
        }

        @Override
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
                case MOVE_RUN:
                case MOVE_VTOL_RUN:
                case MOVE_OVER_THRUST:
                    if (step.isUsingMASC()) {
                        col = GUIPreferences.getInstance().getColor(
                                "AdvancedMoveMASCColor");
                    } else {
                        col = GUIPreferences.getInstance().getColor(
                                "AdvancedMoveRunColor");
                    }
                    break;
                case MOVE_JUMP:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveJumpColor");
                    break;
                case MOVE_SPRINT:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveSprintColor");
                    break;
                case MOVE_ILLEGAL:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveIllegalColor");
                    break;
                default:
                    if (step.getType() == MoveStepType.BACKWARDS) {
                        col = GUIPreferences.getInstance().getColor(
                                "AdvancedMoveBackColor");
                    } else {
                        col = GUIPreferences.getInstance().getColor(
                                "AdvancedMoveDefaultColor");
                    }
                    break;
            }

            if (game.useVectorMove()) {
                drawActiveVectors(step, stepPos, graph);
            }

            drawConditions(step, stepPos, graph, col);

            // draw arrows and cost for the step
            switch (step.getType()) {
                case FORWARDS:
                case SWIM:
                case BACKWARDS:
                case CHARGE:
                case DFA:
                case LATERAL_LEFT:
                case LATERAL_RIGHT:
                case LATERAL_LEFT_BACKWARDS:
                case LATERAL_RIGHT_BACKWARDS:
                case DEC:
                case DECN:
                case ACC:
                case ACCN:
                case LOOP:
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
                    drawRemainingVelocity(step, stepPos, graph, true);
                    break;
                case GO_PRONE:
                case HULL_DOWN:
                case DOWN:
                case DIG_IN:
                case FORTIFY:
                    // draw arrow indicating dropping prone
                    // also doubles as the descent indication
                    Polygon downPoly = movementPolys[7];
                    myPoly = new Polygon(downPoly.xpoints, downPoly.ypoints,
                            downPoly.npoints);
                    graph.setColor(Color.darkGray);
                    myPoly.translate(stepPos.x, stepPos.y);
                    graph.drawPolygon(myPoly);
                    graph.setColor(col);
                    myPoly.translate(-1, -1);
                    graph.drawPolygon(myPoly);
                    offsetCostPos = new Point(stepPos.x + 1, stepPos.y + 15);
                    drawMovementCost(step, offsetCostPos, graph, col, false);
                    drawRemainingVelocity(step, stepPos, graph, true);
                    break;
                case GET_UP:
                case UP:
                case CAREFUL_STAND:
                    // draw arrow indicating standing up
                    // also doubles as the climb indication
                    Polygon upPoly = movementPolys[6];
                    myPoly = new Polygon(upPoly.xpoints, upPoly.ypoints,
                            upPoly.npoints);
                    graph.setColor(Color.darkGray);
                    myPoly.translate(stepPos.x, stepPos.y);
                    graph.drawPolygon(myPoly);
                    graph.setColor(col);
                    myPoly.translate(-1, -1);
                    graph.drawPolygon(myPoly);
                    offsetCostPos = new Point(stepPos.x, stepPos.y + 15);
                    drawMovementCost(step, offsetCostPos, graph, col, false);
                    drawRemainingVelocity(step, stepPos, graph, true);
                    break;
                case CLIMB_MODE_ON:
                    // draw climb mode indicator
                    String climb;
                    if (step.getParent().getEntity().getMovementMode() == EntityMovementMode.WIGE) {
                        climb = Messages.getString("BoardView1.WIGEClimb"); //$NON-NLS-1$
                    } else {
                        climb = Messages.getString("BoardView1.Climb"); //$NON-NLS-1$
                    }
                    if (step.isPastDanger()) {
                        climb = "(" + climb + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                    int climbX = (stepPos.x + 42)
                            - (graph.getFontMetrics(graph.getFont())
                                    .stringWidth(climb) / 2);
                    graph.setColor(Color.darkGray);
                    graph.drawString(climb, climbX, stepPos.y + 39);
                    graph.setColor(col);
                    graph.drawString(climb, climbX - 1, stepPos.y + 38);
                    break;
                case CLIMB_MODE_OFF:
                    // cancel climb mode indicator
                    String climboff;
                    if (step.getParent().getEntity().getMovementMode() == EntityMovementMode.WIGE) {
                        climboff = Messages
                                .getString("BoardView1.WIGEClimbOff"); //$NON-NLS-1$
                    } else {
                        climboff = Messages.getString("BoardView1.ClimbOff"); //$NON-NLS-1$
                    }
                    if (step.isPastDanger()) {
                        climboff = "(" + climboff + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                    int climboffX = (stepPos.x + 42)
                            - (graph.getFontMetrics(graph.getFont())
                                    .stringWidth(climboff) / 2);
                    graph.setColor(Color.darkGray);
                    graph.drawString(climboff, climboffX, stepPos.y + 39);
                    graph.setColor(col);
                    graph.drawString(climboff, climboffX - 1, stepPos.y + 38);

                    break;
                case TURN_LEFT:
                case TURN_RIGHT:
                case THRUST:
                case YAW:
                case EVADE:
                case ROLL:
                    // draw arrows showing the facing
                    myPoly = new Polygon(facingPoly.xpoints,
                            facingPoly.ypoints, facingPoly.npoints);
                    graph.setColor(Color.darkGray);
                    myPoly.translate(stepPos.x + 1, stepPos.y + 1);
                    graph.drawPolygon(myPoly);
                    graph.setColor(col);
                    myPoly.translate(-1, -1);
                    graph.drawPolygon(myPoly);
                    if (game.useVectorMove()) {
                        drawMovementCost(step, stepPos, graph, col, false);
                    }
                    break;
                case LOAD:
                    // Announce load.
                    String load = Messages.getString("BoardView1.Load"); //$NON-NLS-1$
                    if (step.isPastDanger()) {
                        load = "(" + load + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                    int loadX = (stepPos.x + 42)
                            - (graph.getFontMetrics(graph.getFont())
                                    .stringWidth(load) / 2);
                    graph.setColor(Color.darkGray);
                    graph.drawString(load, loadX, stepPos.y + 39);
                    graph.setColor(col);
                    graph.drawString(load, loadX - 1, stepPos.y + 38);
                    break;
                case LAUNCH:
                case UNDOCK:
                    // announce launch
                    String launch = Messages.getString("BoardView1.Launch"); //$NON-NLS-1$
                    if (step.isPastDanger()) {
                        launch = "(" + launch + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                    int launchX = (stepPos.x + 42)
                            - (graph.getFontMetrics(graph.getFont())
                                    .stringWidth(launch) / 2);
                    int launchY = stepPos.y + 38
                            + graph.getFontMetrics(graph.getFont()).getHeight();
                    graph.setColor(Color.darkGray);
                    graph.drawString(launch, launchX, launchY + 1);
                    graph.setColor(col);
                    graph.drawString(launch, launchX - 1, launchY);
                    break;
                case DROP:
                    // announce drop
                    String drop = Messages.getString("BoardView1.Drop"); //$NON-NLS-1$
                    if (step.isPastDanger()) {
                        drop = "(" + drop + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                    int dropX = (stepPos.x + 42)
                            - (graph.getFontMetrics(graph.getFont())
                                    .stringWidth(drop) / 2);
                    int dropY = stepPos.y + 38
                            + graph.getFontMetrics(graph.getFont()).getHeight();
                    graph.setColor(Color.darkGray);
                    graph.drawString(drop, dropX, dropY + 1);
                    graph.setColor(col);
                    graph.drawString(drop, dropX - 1, dropY);
                    break;
                case RECOVER:
                    // announce launch
                    String recover = Messages.getString("BoardView1.Recover"); //$NON-NLS-1$
                    if (step.isDocking()) {
                        recover = Messages.getString("BoardView1.Dock"); //$NON-NLS-1$
                    }
                    if (step.isPastDanger()) {
                        launch = "(" + recover + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                    int recoverX = (stepPos.x + 42)
                            - (graph.getFontMetrics(graph.getFont())
                                    .stringWidth(recover) / 2);
                    int recoverY = stepPos.y + 38
                            + graph.getFontMetrics(graph.getFont()).getHeight();
                    graph.setColor(Color.darkGray);
                    graph.drawString(recover, recoverX, recoverY + 1);
                    graph.setColor(col);
                    graph.drawString(recover, recoverX - 1, recoverY);
                    break;
                case JOIN:
                    // announce launch
                    String join = Messages.getString("BoardView1.Join"); //$NON-NLS-1$
                    if (step.isPastDanger()) {
                        launch = "(" + join + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                    int joinX = (stepPos.x + 42)
                            - (graph.getFontMetrics(graph.getFont())
                                    .stringWidth(join) / 2);
                    int joinY = stepPos.y + 38
                            + graph.getFontMetrics(graph.getFont()).getHeight();
                    graph.setColor(Color.darkGray);
                    graph.drawString(join, joinX, joinY + 1);
                    graph.setColor(col);
                    graph.drawString(join, joinX - 1, joinY);
                    break;
                case UNLOAD:
                    // Announce unload.
                    String unload = Messages.getString("BoardView1.Unload"); //$NON-NLS-1$
                    if (step.isPastDanger()) {
                        unload = "(" + unload + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                    int unloadX = (stepPos.x + 42)
                            - (graph.getFontMetrics(graph.getFont())
                                    .stringWidth(unload) / 2);
                    int unloadY = stepPos.y + 38
                            + graph.getFontMetrics(graph.getFont()).getHeight();
                    graph.setColor(Color.darkGray);
                    graph.drawString(unload, unloadX, unloadY + 1);
                    graph.setColor(col);
                    graph.drawString(unload, unloadX - 1, unloadY);
                    break;
                case HOVER:
                    // announce launch
                    String hover = Messages.getString("BoardView1.Hover"); //$NON-NLS-1$
                    if (step.isPastDanger()) {
                        hover = "(" + hover + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                    int hoverX = (stepPos.x + 42)
                            - (graph.getFontMetrics(graph.getFont())
                                    .stringWidth(hover) / 2);
                    int hoverY = stepPos.y + 38
                            + graph.getFontMetrics(graph.getFont()).getHeight();
                    graph.setColor(Color.darkGray);
                    graph.drawString(hover, hoverX, hoverY + 1);
                    graph.setColor(col);
                    graph.drawString(hover, hoverX - 1, hoverY);
                    drawMovementCost(step, stepPos, graph, col, false);
                    break;
                case LAND:
                    // announce land
                    String land = Messages.getString("BoardView1.Land"); //$NON-NLS-1$
                    if (step.isPastDanger()) {
                        land = "(" + land + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                    int landX = (stepPos.x + 42)
                            - (graph.getFontMetrics(graph.getFont())
                                    .stringWidth(land) / 2);
                    int landY = stepPos.y + 38
                            + graph.getFontMetrics(graph.getFont()).getHeight();
                    graph.setColor(Color.darkGray);
                    graph.drawString(land, landX, landY + 1);
                    graph.setColor(col);
                    graph.drawString(land, landX - 1, landY);
                    break;
                default:
                    break;
            }

            baseScaleImage = createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            // create final image
            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
            }
            graph.dispose();
            tempImage.flush();
        }

        /**
         * draw conditions separate from the step, This allows me to keep
         * conditions on the Aero even when that step is erased (as per advanced
         * movement). For now, just evading and rolling. eventually loading and
         * unloading as well
         */
        private void drawConditions(MoveStep step, Point stepPos,
                Graphics graph, Color col) {

            if (step.isEvading()) {
                String evade = Messages.getString("BoardView1.Evade"); //$NON-NLS-1$
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int evadeX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont()).stringWidth(
                                evade) / 2);
                graph.setColor(Color.darkGray);
                graph.drawString(evade, evadeX, stepPos.y + 64);
                graph.setColor(col);
                graph.drawString(evade, evadeX - 1, stepPos.y + 63);
            }

            if (step.isRolled()) {
                // Announce roll
                String roll = Messages.getString("BoardView1.Roll"); //$NON-NLS-1$
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                int rollX = (stepPos.x + 42)
                        - (graph.getFontMetrics(graph.getFont()).stringWidth(
                                roll) / 2);
                graph.setColor(Color.darkGray);
                graph.drawString(roll, rollX, stepPos.y + 18);
                graph.setColor(col);
                graph.drawString(roll, rollX - 1, stepPos.y + 17);
            }

        }

        private void drawActiveVectors(MoveStep step, Point stepPos,
                Graphics graph) {

            /*
             * TODO: it might be better to move this to the MovementSprite so
             * that it is visible before first step and you can't see it for all
             * entities
             */

            int[] activeXpos = { 39, 59, 59, 40, 19, 19 };
            int[] activeYpos = { 20, 28, 52, 59, 52, 28 };

            int[] v = step.getVectors();
            for (int i = 0; i < 6; i++) {

                String active = Integer.toString(v[i]);
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
                graph.setColor(Color.darkGray);
                graph.drawString(active, activeXpos[i] + stepPos.x,
                        activeYpos[i] + stepPos.y);
                graph.setColor(Color.red);
                graph.drawString(active, (activeXpos[i] + stepPos.x) - 1,
                        (activeYpos[i] + stepPos.y) - 1);

            }

        }

        @Override
        public Rectangle getBounds() {
            bounds = new Rectangle(getHexLocation(step.getPosition()), hex_size);
            return bounds;
        }

        public MoveStep getStep() {
            return step;
        }

        public Font getMovementFont() {

            String fontName = GUIPreferences.getInstance().getString(
                    GUIPreferences.ADVANCED_MOVE_FONT_TYPE);
            int fontStyle = GUIPreferences.getInstance().getInt(
                    GUIPreferences.ADVANCED_MOVE_FONT_STYLE);
            int fontSize = GUIPreferences.getInstance().getInt(
                    GUIPreferences.ADVANCED_MOVE_FONT_SIZE);

            return new Font(fontName, fontStyle, fontSize);
        }

        private void drawRemainingVelocity(MoveStep step, Point stepPos,
                Graphics graph, boolean shiftFlag) {
            String velString = null;
            StringBuffer velStringBuf = new StringBuffer();

            if (game.useVectorMove()) {
                return;
            }

            if (!step.getParent().getEntity().isAirborne()
                    || !(step.getParent().getEntity() instanceof Aero)) {
                return;
            }

            if (((Aero) step.getParent().getEntity()).isSpheroid()) {
                return;
            }

            int distTraveled = step.getDistance();
            int velocity = step.getVelocity();
            if (game.getBoard().onGround()) {
                velocity *= 16;
            }

            velStringBuf.append("(").append(distTraveled).append("/")
                    .append(velocity).append(")");

            Color col = Color.GREEN;
            if (step.getVelocityLeft() > 0) {
                col = Color.RED;
            }

            // Convert the buffer to a String and draw it.
            velString = velStringBuf.toString();
            graph.setFont(new Font("SansSerif", Font.PLAIN, 12)); //$NON-NLS-1$
            int costX = stepPos.x + 42;
            if (shiftFlag) {
                costX -= (graph.getFontMetrics(graph.getFont()).stringWidth(
                        velString) / 2);
            }
            graph.setColor(Color.darkGray);
            graph.drawString(velString, costX, stepPos.y + 28);
            graph.setColor(col);
            graph.drawString(velString, costX - 1, stepPos.y + 27);

            // if we are in atmosphere, then report the free turn status as well
            if (!game.getBoard().inSpace()) {
                String turnString = null;
                StringBuffer turnStringBuf = new StringBuffer();
                turnStringBuf.append("<").append(step.getNStraight())
                        .append(">");

                col = Color.RED;
                if (step.dueFreeTurn()) {
                    col = Color.GREEN;
                } else if (step.canAeroTurn(game)) {
                    col = Color.YELLOW;
                }
                // Convert the buffer to a String and draw it.
                turnString = turnStringBuf.toString();
                graph.setFont(new Font("SansSerif", Font.PLAIN, 10)); //$NON-NLS-1$
                costX = stepPos.x + 50;
                graph.setColor(Color.darkGray);
                graph.drawString(turnString, costX, stepPos.y + 15);
                graph.setColor(col);
                graph.drawString(turnString, costX - 1, stepPos.y + 14);
            }
        }

        private void drawMovementCost(MoveStep step, Point stepPos,
                Graphics graph, Color col, boolean shiftFlag) {
            String costString = null;
            StringBuffer costStringBuf = new StringBuffer();
            costStringBuf.append(step.getMpUsed());

            // If the step is using a road bonus, mark it.
            if (step.isOnlyPavement()
                    && (step.getParent().getEntity() instanceof Tank)) {
                costStringBuf.append("+"); //$NON-NLS-1$
            }

            // If the step is dangerous, mark it.
            if (step.isDanger()) {
                costStringBuf.append("*"); //$NON-NLS-1$
            }

            // If the step is past danger, mark that.
            if (step.isPastDanger()) {
                costStringBuf.insert(0, "("); //$NON-NLS-1$
                costStringBuf.append(")"); //$NON-NLS-1$
            }

            if (step.isUsingMASC()
                    && !step.getParent().getEntity()
                            .hasWorkingMisc(MiscType.F_JET_BOOSTER)) {
                costStringBuf.append("["); //$NON-NLS-1$
                costStringBuf.append(step.getTargetNumberMASC());
                costStringBuf.append("+]"); //$NON-NLS-1$
            }

            if ((step.getMovementType() == EntityMovementType.MOVE_VTOL_WALK)
                    || (step.getMovementType() == EntityMovementType.MOVE_VTOL_RUN)
                    || (step.getMovementType() == EntityMovementType.MOVE_SUBMARINE_WALK)
                    || (step.getMovementType() == EntityMovementType.MOVE_SUBMARINE_RUN)) {
                costStringBuf.append("{").append(step.getElevation())
                        .append("}");
            }

            if (step.getParent().getEntity().isAirborne()) {
                costStringBuf.append("{").append(step.getAltitude())
                        .append("}");
            }

            // Convert the buffer to a String and draw it.
            costString = costStringBuf.toString();
            graph.setFont(getMovementFont()); //$NON-NLS-1$
            int costX = stepPos.x + 42;
            if (shiftFlag) {
                costX -= (graph.getFontMetrics(graph.getFont()).stringWidth(
                        costString) / 2);
            }
            graph.setColor(Color.darkGray);
            graph.drawString(costString, costX, stepPos.y + 39);
            graph.setColor(col);
            graph.drawString(costString, costX - 1, stepPos.y + 38);
        }

    }

    /**
     * Sprite and info for a C3 network. Does not actually use the image buffer
     * as this can be horribly inefficient for long diagonal lines.
     */
    private class C3Sprite extends Sprite {
        private Polygon c3Poly;

        protected int entityId;

        protected int masterId;

        protected Entity entityE;

        protected Entity entityM;

        Color spriteColor;

        public C3Sprite(final Entity e, final Entity m) {
            entityE = e;
            entityM = m;
            entityId = e.getId();
            masterId = m.getId();
            spriteColor = PlayerColors.getColor(e.getOwner().getColorIndex());

            if ((e.getPosition() == null) || (m.getPosition() == null)) {
                c3Poly = new Polygon();
                c3Poly.addPoint(0, 0);
                c3Poly.addPoint(1, 0);
                c3Poly.addPoint(0, 1);
                bounds = new Rectangle(c3Poly.getBounds());
                bounds.setSize(bounds.getSize().width + 1,
                        bounds.getSize().height + 1);
                image = null;
                return;
            }

            makePoly();

            // set bounds
            bounds = new Rectangle(c3Poly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);

            // move poly to upper right of image
            c3Poly.translate(-bounds.getLocation().x, -bounds.getLocation().y);

            // set names & stuff

            // nullify image
            image = null;
        }

        @Override
        public void prepare() {
        }

        private void makePoly() {
            // make a polygon
            final Point a = getHexLocation(entityE.getPosition());
            final Point t = getHexLocation(entityM.getPosition());

            final double an = (entityE.getPosition().radian(
                    entityM.getPosition()) + (Math.PI * 1.5))
                    % (Math.PI * 2); // angle
            final double lw = scale * C3_LINE_WIDTH; // line width

            c3Poly = new Polygon();
            c3Poly.addPoint(
                    a.x
                            + (int) ((scale * (HEX_W / 2)) - (int) Math
                                    .round(Math.sin(an) * lw)),
                    a.y
                            + (int) ((scale * (HEX_H / 2)) + (int) Math
                                    .round(Math.cos(an) * lw)));
            c3Poly.addPoint(
                    a.x
                            + (int) ((scale * (HEX_W / 2)) + (int) Math
                                    .round(Math.sin(an) * lw)),
                    a.y
                            + (int) ((scale * (HEX_H / 2)) - (int) Math
                                    .round(Math.cos(an) * lw)));
            c3Poly.addPoint(
                    t.x
                            + (int) ((scale * (HEX_W / 2)) + (int) Math
                                    .round(Math.sin(an) * lw)),
                    t.y
                            + (int) ((scale * (HEX_H / 2)) - (int) Math
                                    .round(Math.cos(an) * lw)));
            c3Poly.addPoint(
                    t.x
                            + (int) ((scale * (HEX_W / 2)) - (int) Math
                                    .round(Math.sin(an) * lw)),
                    t.y
                            + (int) ((scale * (HEX_H / 2)) + (int) Math
                                    .round(Math.cos(an) * lw)));
        }

        @Override
        public Rectangle getBounds() {
            makePoly();
            // set bounds
            bounds = new Rectangle(c3Poly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);

            // move poly to upper right of image
            c3Poly.translate(-bounds.getLocation().x, -bounds.getLocation().y);
            image = null;

            return bounds;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {

            Polygon drawPoly = new Polygon(c3Poly.xpoints, c3Poly.ypoints,
                    c3Poly.npoints);
            drawPoly.translate(x, y);

            g.setColor(spriteColor);
            g.fillPolygon(drawPoly);
            g.setColor(Color.black);
            g.drawPolygon(drawPoly);
        }

        /**
         * Return true if the point is inside our polygon
         */
        @Override
        public boolean isInside(Point point) {
            return c3Poly.contains(point.x - bounds.x, point.y - bounds.y);
        }

    }

    /**
     * Sprite and info for an aero flyover route. Does not actually use the
     * image buffer as this can be horribly inefficient for long diagonal lines.
     */
    private class FlyOverSprite extends Sprite {
        private Polygon flyOverPoly;

        protected Entity en;

        Color spriteColor;

        public FlyOverSprite(final Entity e) {
            en = e;
            spriteColor = PlayerColors.getColor(e.getOwner().getColorIndex());

            if ((e.getPosition() == null) || (e.getPassedThrough().size() < 2)) {
                flyOverPoly = new Polygon();
                flyOverPoly.addPoint(0, 0);
                flyOverPoly.addPoint(1, 0);
                flyOverPoly.addPoint(0, 1);
                bounds = new Rectangle(flyOverPoly.getBounds());
                bounds.setSize(bounds.getSize().width + 1,
                        bounds.getSize().height + 1);
                image = null;
                return;
            }

            makePoly();

            // set bounds
            bounds = new Rectangle(flyOverPoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);

            // move poly to upper right of image
            flyOverPoly.translate(-bounds.getLocation().x,
                    -bounds.getLocation().y);

            // set names & stuff

            // nullify image
            image = null;
        }

        @Override
        public void prepare() {
        }

        private void makePoly() {
            // make a polygon
            flyOverPoly = new Polygon();
            for (Coords c : en.getPassedThrough()) {
                Coords prev = en.passedThroughPrevious(c);
                if (prev.equals(c)) {
                    continue;
                }
                Point a = getHexLocation(prev);
                Point t = getHexLocation(c);

                final double an = (prev.radian(c) + (Math.PI * 1.5))
                        % (Math.PI * 2); // angle
                final double lw = scale * FLY_OVER_LINE_WIDTH; // line width

                flyOverPoly.addPoint(
                        a.x
                                + (int) ((scale * (HEX_W / 2)) - (int) Math
                                        .round(Math.sin(an) * lw)),
                        a.y
                                + (int) ((scale * (HEX_H / 2)) + (int) Math
                                        .round(Math.cos(an) * lw)));
                // flyOverPoly.addPoint(
                // a.x + (int) (scale * (HEX_W / 2) + (int)
                // Math.round(Math.sin(an) * lw)), a.y
                // + (int) (scale * (HEX_H / 2) - (int) Math.round(Math.cos(an)
                // * lw)));
                // flyOverPoly.addPoint(
                // t.x + (int) (scale * (HEX_W / 2) + (int)
                // Math.round(Math.sin(an) * lw)), t.y
                // + (int) (scale * (HEX_H / 2) - (int) Math.round(Math.cos(an)
                // * lw)));
                flyOverPoly.addPoint(
                        t.x
                                + (int) ((scale * (HEX_W / 2)) - (int) Math
                                        .round(Math.sin(an) * lw)),
                        t.y
                                + (int) ((scale * (HEX_H / 2)) + (int) Math
                                        .round(Math.cos(an) * lw)));

            }

            // now loop through backwards
            for (int i = (en.getPassedThrough().size() - 1); i > 0; i--) {
                Coords c = en.getPassedThrough().elementAt(i);
                Coords next = en.getPassedThrough().elementAt(i - 1);
                Point a = getHexLocation(c);
                Point t = getHexLocation(next);

                final double an = (c.radian(next) + (Math.PI * 1.5))
                        % (Math.PI * 2); // angle
                final double lw = scale * FLY_OVER_LINE_WIDTH; // line width
                // flyOverPoly.addPoint(
                // a.x + (int) (scale * (HEX_W / 2) + (int)
                // Math.round(Math.sin(an) * lw)), a.y
                // + (int) (scale * (HEX_H / 2) - (int) Math.round(Math.cos(an)
                // * lw)));
                // flyOverPoly.addPoint(
                // t.x + (int) (scale * (HEX_W / 2) + (int)
                // Math.round(Math.sin(an) * lw)), t.y
                // + (int) (scale * (HEX_H / 2) - (int) Math.round(Math.cos(an)
                // * lw)));

                flyOverPoly.addPoint(
                        a.x
                                + (int) ((scale * (HEX_W / 2)) - (int) Math
                                        .round(Math.sin(an) * lw)),
                        a.y
                                + (int) ((scale * (HEX_H / 2)) + (int) Math
                                        .round(Math.cos(an) * lw)));
                flyOverPoly.addPoint(
                        t.x
                                + (int) ((scale * (HEX_W / 2)) - (int) Math
                                        .round(Math.sin(an) * lw)),
                        t.y
                                + (int) ((scale * (HEX_H / 2)) + (int) Math
                                        .round(Math.cos(an) * lw)));

            }

        }

        @Override
        public Rectangle getBounds() {
            makePoly();
            // set bounds
            bounds = new Rectangle(flyOverPoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);

            // move poly to upper right of image
            flyOverPoly.translate(-bounds.getLocation().x,
                    -bounds.getLocation().y);
            image = null;

            return bounds;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        public int getEntityId() {
            return en.getId();
        }

        @Override
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {

            Polygon drawPoly = new Polygon(flyOverPoly.xpoints,
                    flyOverPoly.ypoints, flyOverPoly.npoints);
            drawPoly.translate(x, y);

            g.setColor(spriteColor);
            g.fillPolygon(drawPoly);
            g.setColor(Color.black);
            g.drawPolygon(drawPoly);
        }

        /**
         * Return true if the point is inside our polygon
         */
        @Override
        public boolean isInside(Point point) {
            return flyOverPoly.contains(point.x - bounds.x, point.y - bounds.y);
        }

    }

    /**
     * Sprite and info for an attack. Does not actually use the image buffer as
     * this can be horribly inefficient for long diagonal lines. Appears as an
     * arrow. Arrow becoming cut in half when two Meks attacking each other.
     */
    private class AttackSprite extends Sprite {
        private ArrayList<AttackAction> attacks = new ArrayList<AttackAction>();

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

        ArrayList<String> weaponDescs = new ArrayList<String>();

        private final Entity ae;

        private final Targetable target;

        public AttackSprite(final AttackAction attack) {
            attacks.add(attack);
            entityId = attack.getEntityId();
            targetType = attack.getTargetType();
            targetId = attack.getTargetId();
            ae = game.getEntity(attack.getEntityId());
            target = game.getTarget(targetType, targetId);

            // color?
            attackColor = PlayerColors.getColor(ae.getOwner().getColorIndex());
            // angle of line connecting two hexes
            an = (ae.getPosition().radian(target.getPosition()) + (Math.PI * 1.5))
                    % (Math.PI * 2); // angle
            makePoly();

            // set bounds
            bounds = new Rectangle(attackPoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);
            // move poly to upper right of image
            attackPoly.translate(-bounds.getLocation().x,
                    -bounds.getLocation().y);

            // set names & stuff
            attackerDesc = ae.getDisplayName();
            targetDesc = target.getDisplayName();
            if (attack instanceof WeaponAttackAction) {
                addWeapon((WeaponAttackAction) attack);
            }
            if (attack instanceof KickAttackAction) {
                addWeapon((KickAttackAction) attack);
            }
            if (attack instanceof PunchAttackAction) {
                addWeapon((PunchAttackAction) attack);
            }
            if (attack instanceof PushAttackAction) {
                addWeapon((PushAttackAction) attack);
            }
            if (attack instanceof ClubAttackAction) {
                addWeapon((ClubAttackAction) attack);
            }
            if (attack instanceof ChargeAttackAction) {
                addWeapon((ChargeAttackAction) attack);
            }
            if (attack instanceof DfaAttackAction) {
                addWeapon((DfaAttackAction) attack);
            }
            if (attack instanceof ProtomechPhysicalAttackAction) {
                addWeapon((ProtomechPhysicalAttackAction) attack);
            }
            if (attack instanceof SearchlightAttackAction) {
                addWeapon((SearchlightAttackAction) attack);
            }

            // nullify image
            image = null;
        }

        private void makePoly() {
            // make a polygon
            a = getHexLocation(ae.getPosition());
            t = getHexLocation(target.getPosition());
            // OK, that is actually not good. I do not like hard coded figures.
            // HEX_W/2 - x distance in pixels from origin of hex bounding box to
            // the center of hex.
            // HEX_H/2 - y distance in pixels from origin of hex bounding box to
            // the center of hex.
            // 18 - is actually 36/2 - we do not want arrows to start and end
            // directly
            // in the centes of hex and hiding mek under.

            a.x = a.x + (int) ((HEX_W / 2) * scale)
                    + (int) Math.round(Math.cos(an) * (int) (18 * scale));
            t.x = (t.x + (int) ((HEX_W / 2) * scale))
                    - (int) Math.round(Math.cos(an) * (int) (18 * scale));
            a.y = a.y + (int) ((HEX_H / 2) * scale)
                    + (int) Math.round(Math.sin(an) * (int) (18 * scale));
            t.y = (t.y + (int) ((HEX_H / 2) * scale))
                    - (int) Math.round(Math.sin(an) * (int) (18 * scale));

            // Checking if given attack is mutual. In this case we building
            // halved arrow
            if (isMutualAttack()) {
                attackPoly = new StraightArrowPolygon(a, t, (int) (8 * scale),
                        (int) (12 * scale), true);
            } else {
                attackPoly = new StraightArrowPolygon(a, t, (int) (4 * scale),
                        (int) (8 * scale), false);
            }
        }

        @Override
        public Rectangle getBounds() {
            makePoly();
            // set bounds
            bounds = new Rectangle(attackPoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);
            // move poly to upper right of image
            attackPoly.translate(-bounds.getLocation().x,
                    -bounds.getLocation().y);

            return bounds;
        }

        /**
         * If we have build full arrow already with single attack and have got
         * counter attack from our target lately - lets change arrow to halved.
         */
        public void rebuildToHalvedPolygon() {
            attackPoly = new StraightArrowPolygon(a, t, (int) (8 * scale),
                    (int) (12 * scale), true);
            // set bounds
            bounds = new Rectangle(attackPoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);
            // move poly to upper right of image
            attackPoly.translate(-bounds.getLocation().x,
                    -bounds.getLocation().y);
        }

        /**
         * Cheking if attack is mutual and changing target arrow to half-arrow
         */
        private boolean isMutualAttack() {
            for (AttackSprite sprite : attackSprites) {
                if ((sprite.getEntityId() == targetId)
                        && (sprite.getTargetId() == entityId)) {
                    sprite.rebuildToHalvedPolygon();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void prepare() {
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
            Polygon drawPoly = new Polygon(attackPoly.xpoints,
                    attackPoly.ypoints, attackPoly.npoints);
            drawPoly.translate(x, y);

            g.setColor(attackColor);
            g.fillPolygon(drawPoly);
            g.setColor(Color.gray.darker());
            g.drawPolygon(drawPoly);
        }

        /**
         * Return true if the point is inside our polygon
         */
        @Override
        public boolean isInside(Point point) {
            return attackPoly.contains(point.x - bounds.x, point.y - bounds.y);
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
            final WeaponType wtype = (WeaponType) entity.getEquipment(
                    attack.getWeaponId()).getType();
            final String roll = attack.toHit(game).getValueAsString();
            final String table = attack.toHit(game).getTableDesc();
            weaponDescs
                    .add(wtype.getName()
                            + Messages.getString("BoardView1.needs") + roll + " " + table); //$NON-NLS-1$
        }

        public void addWeapon(KickAttackAction attack) {
            String bufer = ""; //$NON-NLS-1$
            String rollLeft = ""; //$NON-NLS-1$
            String rollRight = ""; //$NON-NLS-1$
            final int leg = attack.getLeg();
            switch (leg) {
                case KickAttackAction.BOTH:
                    rollLeft = KickAttackAction.toHit(
                            game,
                            attack.getEntityId(),
                            game.getTarget(attack.getTargetType(),
                                    attack.getTargetId()),
                            KickAttackAction.LEFT).getValueAsString();
                    rollRight = KickAttackAction.toHit(
                            game,
                            attack.getEntityId(),
                            game.getTarget(attack.getTargetType(),
                                    attack.getTargetId()),
                            KickAttackAction.RIGHT).getValueAsString();
                    bufer = Messages
                            .getString(
                                    "BoardView1.kickBoth", new Object[] { rollLeft, rollRight }); //$NON-NLS-1$
                    break;
                case KickAttackAction.LEFT:
                    rollLeft = KickAttackAction.toHit(
                            game,
                            attack.getEntityId(),
                            game.getTarget(attack.getTargetType(),
                                    attack.getTargetId()),
                            KickAttackAction.LEFT).getValueAsString();
                    bufer = Messages.getString(
                            "BoardView1.kickLeft", new Object[] { rollLeft }); //$NON-NLS-1$
                    break;
                case KickAttackAction.RIGHT:
                    rollRight = KickAttackAction.toHit(
                            game,
                            attack.getEntityId(),
                            game.getTarget(attack.getTargetType(),
                                    attack.getTargetId()),
                            KickAttackAction.RIGHT).getValueAsString();
                    bufer = Messages.getString(
                            "BoardView1.kickRight", new Object[] { rollRight }); //$NON-NLS-1$
                    break;
            }
            weaponDescs.add(bufer);
        }

        public void addWeapon(PunchAttackAction attack) {
            String bufer = ""; //$NON-NLS-1$
            String rollLeft = ""; //$NON-NLS-1$
            String rollRight = ""; //$NON-NLS-1$
            final int arm = attack.getArm();
            switch (arm) {
                case PunchAttackAction.BOTH:
                    rollLeft = PunchAttackAction.toHit(
                            game,
                            attack.getEntityId(),
                            game.getTarget(attack.getTargetType(),
                                    attack.getTargetId()),
                            PunchAttackAction.LEFT).getValueAsString();
                    rollRight = PunchAttackAction.toHit(
                            game,
                            attack.getEntityId(),
                            game.getTarget(attack.getTargetType(),
                                    attack.getTargetId()),
                            PunchAttackAction.RIGHT).getValueAsString();
                    bufer = Messages
                            .getString(
                                    "BoardView1.punchBoth", new Object[] { rollLeft, rollRight }); //$NON-NLS-1$
                    break;
                case PunchAttackAction.LEFT:
                    rollLeft = PunchAttackAction.toHit(
                            game,
                            attack.getEntityId(),
                            game.getTarget(attack.getTargetType(),
                                    attack.getTargetId()),
                            PunchAttackAction.LEFT).getValueAsString();
                    bufer = Messages.getString(
                            "BoardView1.punchLeft", new Object[] { rollLeft }); //$NON-NLS-1$
                    break;
                case PunchAttackAction.RIGHT:
                    rollRight = PunchAttackAction.toHit(
                            game,
                            attack.getEntityId(),
                            game.getTarget(attack.getTargetType(),
                                    attack.getTargetId()),
                            PunchAttackAction.RIGHT).getValueAsString();
                    bufer = Messages
                            .getString(
                                    "BoardView1.punchRight", new Object[] { rollRight }); //$NON-NLS-1$
                    break;
            }
            weaponDescs.add(bufer);
        }

        public void addWeapon(PushAttackAction attack) {
            final String roll = attack.toHit(game).getValueAsString();
            weaponDescs.add(Messages.getString(
                    "BoardView1.push", new Object[] { roll })); //$NON-NLS-1$
        }

        public void addWeapon(ClubAttackAction attack) {
            final String roll = attack.toHit(game).getValueAsString();
            final String club = attack.getClub().getName();
            weaponDescs.add(Messages.getString(
                    "BoardView1.hit", new Object[] { club, roll })); //$NON-NLS-1$
        }

        public void addWeapon(ChargeAttackAction attack) {
            final String roll = attack.toHit(game).getValueAsString();
            weaponDescs.add(Messages.getString(
                    "BoardView1.charge", new Object[] { roll })); //$NON-NLS-1$
        }

        public void addWeapon(DfaAttackAction attack) {
            final String roll = attack.toHit(game).getValueAsString();
            weaponDescs.add(Messages.getString(
                    "BoardView1.DFA", new Object[] { roll })); //$NON-NLS-1$
        }

        public void addWeapon(ProtomechPhysicalAttackAction attack) {
            final String roll = attack.toHit(game).getValueAsString();
            weaponDescs.add(Messages.getString(
                    "BoardView1.proto", new Object[] { roll })); //$NON-NLS-1$
        }

        public void addWeapon(SearchlightAttackAction attack) {
            weaponDescs.add(Messages.getString("BoardView1.Searchlight"));
        }

        @Override
        public String[] getTooltip() {
            String[] tipStrings = new String[1 + weaponDescs.size()];
            int tip = 1;
            tipStrings[0] = attackerDesc
                    + " " + Messages.getString("BoardView1.on") + " " + targetDesc; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            for (Iterator<String> i = weaponDescs.iterator(); i.hasNext();) {
                tipStrings[tip++] = i.next();
            }
            return tipStrings;
        }
    }

    /**
     * Sprite and info for movement vector (AT2 advanced movement). Does not
     * actually use the image buffer as this can be horribly inefficient for
     * long diagonal lines.
     * 
     * Appears as an arrow pointing to the hex this entity will move to based on
     * current movement vectors. TODO: Different color depending upon whether
     * entity has already moved this turn
     * 
     */
    private class MovementSprite extends Sprite {
        private Point a;

        private Point t;

        private double an;

        private StraightArrowPolygon movePoly;

        private Color moveColor;

        // private MovementVector mv;
        private int[] vectors;

        private Coords start;

        private Coords end;

        private Entity en;

        private int vel;

        public MovementSprite(Entity e, int[] v, Color col, boolean isCurrent) {
            // this.mv = en.getMV();

            en = e;
            vectors = v;// en.getVectors();
            // get the starting and ending position
            start = en.getPosition();
            end = Compute.getFinalPosition(start, vectors);

            // what is the velocity
            vel = 0;
            for (int element : v) {
                vel += element;
            }

            // color?
            // player colors
            moveColor = PlayerColors.getColor(en.getOwner().getColorIndex());
            // TODO: Its not going transparent. Oh well, it is a minor issue at
            // the moment
            /*
             * if(isCurrent) { int colour = col.getRGB(); int transparency =
             * GUIPreferences.getInstance().getInt(GUIPreferences.
             * ADVANCED_ATTACK_ARROW_TRANSPARENCY); moveColor = new Color(colour
             * | (transparency << 24), true); }
             */
            // red if offboard
            if (!game.getBoard().contains(end)) {
                int colour = 0xff0000; // red
                int transparency = GUIPreferences.getInstance().getInt(
                        GUIPreferences.ADVANCED_ATTACK_ARROW_TRANSPARENCY);
                moveColor = new Color(colour | (transparency << 24), true);
            }
            // dark gray if done
            if (en.isDone()) {
                int colour = 0x696969; // gray
                int transparency = GUIPreferences.getInstance().getInt(
                        GUIPreferences.ADVANCED_ATTACK_ARROW_TRANSPARENCY);
                moveColor = new Color(colour | (transparency << 24), true);
            }

            // moveColor = PlayerColors.getColor(en.getOwner().getColorIndex());
            // angle of line connecting two hexes
            an = (start.radian(end) + (Math.PI * 1.5)) % (Math.PI * 2); // angle
            makePoly();

            // set bounds
            bounds = new Rectangle(movePoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);
            // move poly to upper right of image
            movePoly.translate(-bounds.getLocation().x, -bounds.getLocation().y);

            // nullify image
            image = null;
        }

        private void makePoly() {
            // make a polygon
            a = getHexLocation(start);
            t = getHexLocation(end);
            // OK, that is actually not good. I do not like hard coded figures.
            // HEX_W/2 - x distance in pixels from origin of hex bounding box to
            // the center of hex.
            // HEX_H/2 - y distance in pixels from origin of hex bounding box to
            // the center of hex.
            // 18 - is actually 36/2 - we do not want arrows to start and end
            // directly
            // in the centes of hex and hiding mek under.

            a.x = a.x + (int) ((HEX_W / 2) * scale)
                    + (int) Math.round(Math.cos(an) * (int) (18 * scale));
            t.x = (t.x + (int) ((HEX_W / 2) * scale))
                    - (int) Math.round(Math.cos(an) * (int) (18 * scale));
            a.y = a.y + (int) ((HEX_H / 2) * scale)
                    + (int) Math.round(Math.sin(an) * (int) (18 * scale));
            t.y = (t.y + (int) ((HEX_H / 2) * scale))
                    - (int) Math.round(Math.sin(an) * (int) (18 * scale));
            movePoly = new StraightArrowPolygon(a, t, (int) (4 * scale),
                    (int) (8 * scale), false);
        }

        @Override
        public Rectangle getBounds() {
            makePoly();
            // set bounds
            bounds = new Rectangle(movePoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);
            // move poly to upper right of image
            movePoly.translate(-bounds.getLocation().x, -bounds.getLocation().y);

            return bounds;
        }

        @Override
        public void prepare() {

        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
            // don't draw anything if the unit has no velocity

            if (vel == 0) {
                return;
            }

            Polygon drawPoly = new Polygon(movePoly.xpoints, movePoly.ypoints,
                    movePoly.npoints);
            drawPoly.translate(x, y);

            g.setColor(moveColor);
            g.fillPolygon(drawPoly);
            g.setColor(Color.gray.darker());
            g.drawPolygon(drawPoly);

        }

        /**
         * Return true if the point is inside our polygon
         */
        @Override
        public boolean isInside(Point point) {
            return movePoly.contains(point.x - bounds.x, point.y - bounds.y);
        }

        /*
         * public String[] getTooltip() { String[] tipStrings = new String[1 +
         * weaponDescs.size()]; int tip = 1; tipStrings[0] = attackerDesc +
         * " "+Messages.getString("BoardView1.on")+" " + targetDesc;
         * //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ for (Iterator<String> i =
         * weaponDescs.iterator(); i.hasNext();) { tipStrings[tip++] = i.next();
         * } return tipStrings; }
         */
    }

    /**
     * Sprite for displaying generic firing information. This is used for
     */
    private class FiringSolutionSprite extends Sprite {
        private int toHitMod, range;
        private Coords loc;
        private Image baseScaleImage;

        public FiringSolutionSprite(final int thm, final int r, final Coords l) {
            toHitMod = thm;
            loc = l;
            range = r;
            bounds = new Rectangle(getHexLocation(loc), hex_size);
            image = null;
            baseScaleImage = null;
        }

        /**
         * Refreshes this StepSprite's image to handle changes in the zoom
         * level.
         */
        public void refreshZoomLevel() {

            if (baseScaleImage == null) {
                return;
            }

            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        baseScaleImage.getSource(), new KeyAlphaFilter(
                                TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        baseScaleImage.getSource(), new KeyAlphaFilter(
                                TRANSPARENT))),false);
            }
        }

        @Override
        public void prepare() {
            // create image for buffer
            Image tempImage = createImage(bounds.width, bounds.height);
            Graphics graph = tempImage.getGraphics();

            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);

            // Draw firing information
            Point p = getHexLocation(loc);
            p.translate(-bounds.x, -bounds.y);
            graph.setFont(getFiringFont());

            if ((toHitMod != TargetRoll.IMPOSSIBLE)
                    && (toHitMod != TargetRoll.AUTOMATIC_FAIL)) {
                int xOffset = 25;
                int yOffset = 30;
                FontMetrics metrics = graph.getFontMetrics();
                // Draw to-hit modifier

                String modifier;
                if (toHitMod >= 0) {
                    modifier = "+" + toHitMod;
                } else {
                    modifier = "" + toHitMod;
                }
                Graphics2D g2 = (Graphics2D) graph;
                GlyphVector gv = getFiringFont().createGlyphVector(
                        g2.getFontRenderContext(), modifier);
                g2.translate(xOffset, yOffset);
                for (int i = 0; i < modifier.length(); i++){
                    Shape gs = gv.getGlyphOutline(i);
                    g2.setPaint(GUIPreferences.getInstance().getColor(
                            GUIPreferences.ADVANCED_FIRE_SOLN_CANSEE_COLOR)); 
                    g2.fill(gs); // Fill the shape
                    g2.setPaint(Color.black); // Switch to solid black
                    g2.draw(gs); // And draw the outline
                }
                g2.translate(-13, metrics.getHeight());
                yOffset += metrics.getHeight();
                modifier = "rng: " + range;
                gv = getFiringFont().createGlyphVector(
                        g2.getFontRenderContext(), modifier);
                for (int i = 0; i < modifier.length(); i++){
                    Shape gs = gv.getGlyphOutline(i);
                    g2.setPaint(GUIPreferences.getInstance().getColor(
                            GUIPreferences.ADVANCED_FIRE_SOLN_CANSEE_COLOR)); 
                    g2.fill(gs); // Fill the shape
                    g2.setPaint(Color.black); // Switch to solid black
                    g2.draw(gs); // And draw the outline
                }
            } else {
                String modifier = "X";
                Graphics2D g2 = (Graphics2D) graph;
                GlyphVector gv = getFiringFont().createGlyphVector(
                        g2.getFontRenderContext(), modifier);
                g2.translate(35, 39);
                for (int i = 0; i < modifier.length(); i++){
                    Shape gs = gv.getGlyphOutline(i);
                    g2.setPaint(GUIPreferences.getInstance().getColor(
                            GUIPreferences.ADVANCED_FIRE_SOLN_NOSEE_COLOR)); 
                    g2.fill(gs); // Fill the shape
                    g2.setPaint(Color.black); // Switch to solid black
                    g2.draw(gs); // And draw the outline
                }
            }

            // graph.setColor(drawColor);
            // graph.drawPolygon(hexPoly);

            baseScaleImage = createImage(new FilteredImageSource(
                    tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            // create final image
            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
            }
            graph.dispose();
            tempImage.flush();
        }

        @Override
        public Rectangle getBounds() {
            bounds = new Rectangle(getHexLocation(loc), hex_size);
            return bounds;
        }

        public Font getFiringFont() {

            String fontName = GUIPreferences.getInstance().getString(
                    GUIPreferences.ADVANCED_MOVE_FONT_TYPE);
            int fontStyle = GUIPreferences.getInstance().getInt(
                    GUIPreferences.ADVANCED_MOVE_FONT_STYLE);
            int fontSize = 16;

            return new Font(fontName, fontStyle, fontSize);
        }
    }

    /**
     * Sprite for displaying information about where a unit can move to.
     */
    private class MovementEnvelopeSprite extends Sprite {

        Color drawColor;
        Coords loc;

        public MovementEnvelopeSprite(Color c, Coords l) {
            drawColor = c;
            loc = l;
        }

        @Override
        public void prepare() {
            // create image for buffer
            Image tempImage = createImage(bounds.width, bounds.height);
            Graphics graph = tempImage.getGraphics();

            // fill with key color
            graph.setColor(new Color(TRANSPARENT));
            graph.fillRect(0, 0, bounds.width, bounds.height);
            // draw attack poly
            graph.setColor(drawColor);
            Stroke st = ((Graphics2D) graph).getStroke();
            ((Graphics2D) graph).setStroke(new BasicStroke(2));
            graph.drawPolygon(hexPoly);
            ((Graphics2D) graph).setStroke(st);

            // create final image
            if (zoomIndex == BASE_ZOOM_INDEX) {
                image = createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT)));
            } else {
                image = getScaledImage(createImage(new FilteredImageSource(
                        tempImage.getSource(), new KeyAlphaFilter(TRANSPARENT))),false);
            }
            graph.dispose();
            tempImage.flush();
        }

        @Override
        public Rectangle getBounds() {
            bounds = new Rectangle(getHexLocation(loc), hex_size);
            return bounds;
        }

        public Coords getPosition() {
            return loc;
        }

    }

    /**
     * Determine if the tile manager's images have been loaded.
     * 
     * @return <code>true</code> if all images have been loaded.
     *         <code>false</code> if more need to be loaded.
     */
    public boolean isTileImagesLoaded() {
        return tileManager.isLoaded();
    }

    public void setUseLOSTool(boolean use) {
        useLOSTool = use;
    }

    public TilesetManager getTilesetManager() {
        return tileManager;
    }

    /**
     * @param lastCursor
     *            The lastCursor to set.
     */
    public void setLastCursor(Coords lastCursor) {
        this.lastCursor = lastCursor;
    }

    /**
     * @return Returns the lastCursor.
     */
    public Coords getLastCursor() {
        return lastCursor;
    }

    /**
     * @param highlighted
     *            The highlighted to set.
     */
    public void setHighlighted(Coords highlighted) {
        this.highlighted = highlighted;
    }

    /**
     * @return Returns the highlighted.
     */
    public Coords getHighlighted() {
        return highlighted;
    }

    /**
     * @param selected
     *            The selected to set.
     */
    public void setSelected(Coords selected) {
        if (this.selected != selected) {
            this.selected = selected;

            // force a repaint of the board
            boardGraph = null;
            updateBoard();
        }
    }

    /**
     * @return Returns the selected.
     */
    public Coords getSelected() {
        return selected;
    }

    /**
     * @param firstLOS
     *            The firstLOS to set.
     */
    public void setFirstLOS(Coords firstLOS) {
        this.firstLOS = firstLOS;
    }

    /**
     * @return Returns the firstLOS.
     */
    public Coords getFirstLOS() {
        return firstLOS;
    }

    /**
     * Determines if this Board contains the Coords, and if so, "selects" that
     * Coords.
     * 
     * @param coords
     *            the Coords.
     */
    public void select(Coords coords) {
        if ((coords == null) || game.getBoard().contains(coords)) {
            setSelected(coords);
            moveCursor(selectedSprite, coords);
            moveCursor(firstLOSSprite, null);
            moveCursor(secondLOSSprite, null);
            processBoardViewEvent(new BoardViewEvent(this, coords, null,
                    BoardViewEvent.BOARD_HEX_SELECTED, 0));
        }
    }

    /**
     * "Selects" the specified Coords.
     * 
     * @param x
     *            the x coordinate.
     * @param y
     *            the y coordinate.
     */
    public void select(int x, int y) {
        select(new Coords(x, y));
    }

    /**
     * Determines if this Board contains the Coords, and if so, highlights that
     * Coords.
     * 
     * @param coords
     *            the Coords.
     */
    public void highlight(Coords coords) {
        if ((coords == null) || game.getBoard().contains(coords)) {
            setHighlighted(coords);
            moveCursor(highlightSprite, coords);
            moveCursor(firstLOSSprite, null);
            moveCursor(secondLOSSprite, null);
            processBoardViewEvent(new BoardViewEvent(this, coords, null,
                    BoardViewEvent.BOARD_HEX_HIGHLIGHTED, 0));
        }
    }

    /**
     * Highlights the specified Coords.
     * 
     * @param x
     *            the x coordinate.
     * @param y
     *            the y coordinate.
     */
    public void highlight(int x, int y) {
        highlight(new Coords(x, y));
    }

    /**
     * Determines if this Board contains the Coords, and if so, "cursors" that
     * Coords.
     * 
     * @param coords
     *            the Coords.
     */
    public void cursor(Coords coords) {
        if ((coords == null) || game.getBoard().contains(coords)) {
            if ((getLastCursor() == null) || (coords == null)
                    || !coords.equals(getLastCursor())) {
                setLastCursor(coords);
                moveCursor(cursorSprite, coords);
                moveCursor(firstLOSSprite, null);
                moveCursor(secondLOSSprite, null);
                processBoardViewEvent(new BoardViewEvent(this, coords, null,
                        BoardViewEvent.BOARD_HEX_CURSOR, 0));
            } else {
                setLastCursor(coords);
            }
        }
    }

    /**
     * "Cursors" the specified Coords.
     * 
     * @param x
     *            the x coordinate.
     * @param y
     *            the y coordinate.
     */
    public void cursor(int x, int y) {
        cursor(new Coords(x, y));
    }

    public void checkLOS(Coords c) {
        if ((c == null) || game.getBoard().contains(c)) {
            if (getFirstLOS() == null) {
                setFirstLOS(c);
                firstLOSHex(c);
                processBoardViewEvent(new BoardViewEvent(this, c, null,
                        BoardViewEvent.BOARD_FIRST_LOS_HEX, 0));
            } else {
                secondLOSHex(c, getFirstLOS());
                processBoardViewEvent(new BoardViewEvent(this, c, null,
                        BoardViewEvent.BOARD_SECOND_LOS_HEX, 0));
                setFirstLOS(null);
            }
        }
    }

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, notifies
     * listeners about the specified mouse action.
     */
    public void mouseAction(int x, int y, int mtype, int modifiers) {
        if (game.getBoard().contains(x, y)) {
            Coords c = new Coords(x, y);
            switch (mtype) {
                case BOARD_HEX_CLICK:
                    if ((modifiers & java.awt.event.InputEvent.CTRL_MASK) != 0) {
                        checkLOS(c);
                    } else {
                        processBoardViewEvent(new BoardViewEvent(this, c, null,
                                BoardViewEvent.BOARD_HEX_CLICKED, modifiers));
                    }
                    break;
                case BOARD_HEX_DOUBLECLICK:
                    processBoardViewEvent(new BoardViewEvent(this, c, null,
                            BoardViewEvent.BOARD_HEX_DOUBLECLICKED, modifiers));
                    break;
                case BOARD_HEX_DRAG:
                    processBoardViewEvent(new BoardViewEvent(this, c, null,
                            BoardViewEvent.BOARD_HEX_DRAGGED, modifiers));
                    break;
                case BOARD_HEX_POPUP:
                    processBoardViewEvent(new BoardViewEvent(this, c, null,
                            BoardViewEvent.BOARD_HEX_POPUP, modifiers));
                    break;
            }
        }
    }

    /**
     * Notifies listeners about the specified mouse action.
     * 
     * @param coords
     *            the Coords.
     */
    public void mouseAction(Coords coords, int mtype, int modifiers) {
        mouseAction(coords.x, coords.y, mtype, modifiers);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.BoardListener#boardNewBoard(megamek.common.BoardEvent)
     */
    public void boardNewBoard(BoardEvent b) {
        updateBoard();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.BoardListener#boardChangedHex(megamek.common.BoardEvent)
     */
    public synchronized void boardChangedHex(BoardEvent b) {
        IHex hex = game.getBoard().getHex(b.getCoords());
        tileManager.clearHex(hex);
        tileManager.waitForHex(hex);
        redrawWholeBoard = true;
        repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.BoardListener#boardChangedHex(megamek.common.BoardEvent)
     */
    public synchronized void boardChangedAllHexes(BoardEvent b) {
        tileManager.loadAllHexes();
        redrawWholeBoard = true;
        repaint();
    }

    private GameListener gameListener = new GameListenerAdapter() {

        @Override
        public void gameEntityNew(GameEntityNewEvent e) {
            updateEcmList();
            redrawAllEntities();
            if (game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
                refreshMoveVectors();
            }
        }

        @Override
        public void gameEntityRemove(GameEntityRemoveEvent e) {
            updateEcmList();
            redrawAllEntities();
            if (game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
                refreshMoveVectors();
            }
        }

        @Override
        public void gameEntityChange(GameEntityChangeEvent e) {
            Vector<UnitLocation> mp = e.getMovePath();
            updateEcmList();
            if (e.getEntity().hasActiveECM()) {
                // this might disrupt c3/c3i lines, so redraw all
                redrawAllEntities();
            }
            if (game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
                refreshMoveVectors();
            }
            if ((mp != null) && (mp.size() > 0)
                    && GUIPreferences.getInstance().getShowMoveStep()) {
                addMovingUnit(e.getEntity(), mp);
            } else {
                redrawEntity(e.getEntity(), e.getOldEntity());
            }
        }

        @Override
        public void gameNewAction(GameNewActionEvent e) {
            EntityAction ea = e.getAction();
            if (ea instanceof AttackAction) {
                addAttack((AttackAction) ea);
            }
        }

        @Override
        public void gameBoardNew(GameBoardNewEvent e) {
            IBoard b = e.getOldBoard();
            if (b != null) {
                b.removeBoardListener(BoardView1.this);
            }
            b = e.getNewBoard();
            if (b != null) {
                b.addBoardListener(BoardView1.this);
            }
            updateBoard();
        }

        @Override
        public void gameBoardChanged(GameBoardChangeEvent e) {
            boardChanged();
        }

        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            refreshAttacks();
            switch (e.getNewPhase()) {
                case PHASE_MOVEMENT:
                    refreshMoveVectors();
                case PHASE_FIRING:
                    clearAllMoveVectors();
                case PHASE_PHYSICAL:
                    refreshAttacks();
                    break;
                case PHASE_INITIATIVE:
                    clearAllAttacks();
                    break;
                case PHASE_END:
                case PHASE_VICTORY:
                    clearSprites();
                case PHASE_LOUNGE:
                    clearAllMoveVectors();
                    clearAllAttacks();
                    clearSprites();
                    select(null);
                    cursor(null);
                    highlight(null);
                default:
            }
        }
    };

    synchronized void boardChanged() {
        redrawAllEntities();
    }

    void clearSprites() {
        pathSprites.clear();
        firingSprites.clear();
        attackSprites.clear();
        c3Sprites.clear();
        flyOverSprites.clear();
        movementSprites.clear();

    }

    protected synchronized void updateBoard() {
        updateBoardSize();
        redrawAllEntities();
    }

    /**
     * the old redrawworker converted to a runnable which is called now and then
     * from the event thread
     */
    protected class RedrawWorker implements Runnable {

        protected long lastTime = System.currentTimeMillis();

        protected long currentTime = System.currentTimeMillis();

        public void run() {
            currentTime = System.currentTimeMillis();
            if (isShowing()) {
                boolean redraw = false;
                for (int i = 0; i < displayables.size(); i++) {
                    IDisplayable disp = displayables.get(i);
                    if (!disp.isSliding()) {
                        disp.setIdleTime(currentTime - lastTime, true);
                    } else {
                        redraw = redraw || disp.slide();
                    }
                }
                redraw = redraw || doMoveUnits(currentTime - lastTime);
                if (redraw) {
                    repaint();
                }
            }
            lastTime = currentTime;
        }
    }

    public synchronized void selectEntity(Entity e) {
        selectedEntity = e;
        // If we don't do this, the selectedWeapon might not correspond to this
        // entity
        selectedWeapon = null;
    }

    public synchronized void weaponSelected(MechDisplayEvent b) {
        selectedEntity = b.getEntity();
        selectedWeapon = b.getEquip();
        repaint();
    }

    private class EcmBubble extends Coords {
        /**
         *
         */
        private static final long serialVersionUID = 3304636458460529324L;

        int range;
        int tint;
        int direction;

        public EcmBubble(Coords c, int range, int tint) {
            super(c);
            this.range = range;
            this.tint = tint;
            direction = -1;
        }

        public EcmBubble(Coords c, int range, int tint, int direction) {
            super(c);
            this.range = range;
            this.tint = tint;
            this.direction = direction;
        }

    }

    // This is expensive, so precalculate when entity changes
    public void updateEcmList() {
        ArrayList<EcmBubble> list = new ArrayList<EcmBubble>();
        for (Enumeration<Entity> e = game.getEntities(); e.hasMoreElements();) {
            Entity ent = e.nextElement();
            Coords entPos = ent.getPosition();
            int range = ent.getECMRange();
            boolean deployed = ent.isDeployed();
            boolean offboard = ent.isOffBoard();
            if ((entPos == null) && (ent.getTransportId() != Entity.NONE)) {
                Entity carrier = game.getEntity(ent.getTransportId());
                if ((null != carrier) && carrier.loadedUnitsHaveActiveECM()) {
                    entPos = carrier.getPosition();
                    deployed = carrier.isDeployed();
                    offboard = carrier.isOffBoard();
                }
            }
            if ((entPos == null) || !deployed || offboard) {
                continue;
            }
            if (range != Entity.NONE) {
                int tint = PlayerColors.getColorRGB(ent.getOwner()
                        .getColorIndex());
                list.add(new EcmBubble(entPos, range, tint));
            }
            if (game.getBoard().inSpace()) {
                // then BAP is also ECCM so it needs a bubble
                range = ent.getBAPRange();
                int direction = -1;
                if (range != Entity.NONE) {
                    if (range > 6) {
                        direction = ent.getFacing();
                    }
                    int tint = PlayerColors.getColorRGB(ent.getOwner()
                            .getColorIndex());
                    list.add(new EcmBubble(entPos, range, tint, direction));
                }
            }
        }
        HashMap<Coords, Integer> table = new HashMap<Coords, Integer>();
        for (EcmBubble b : list) {
            Integer col = new Integer(b.tint);
            for (int x = -b.range; x <= b.range; x++) {
                for (int y = -b.range; y <= b.range; y++) {
                    Coords c = new Coords(x + b.x, y + b.y);
                    // clip rectangle to hexagon
                    if ((b.distance(c) <= b.range)
                            && ((b.direction == -1) || Compute.isInArc(b,
                                    b.direction, c, Compute.ARC_NOSE))) {
                        Integer tint = table.get(c);
                        if (tint == null) {
                            table.put(c, col);
                        } else if (tint.intValue() != b.tint) {
                            int red1 = (tint.intValue() >> 16) & 0xff;
                            int green1 = (tint.intValue() >> 8) & 0xff;
                            int blue1 = tint.intValue() & 0xff;
                            int red2 = (b.tint >> 16) & 0xff;
                            int green2 = (b.tint >> 8) & 0xff;
                            int blue2 = b.tint & 0xff;
                            red1 = (red1 + red2) / 2;
                            green1 = (green1 + green2) / 2;
                            blue1 = (blue1 + blue2) / 2;
                            table.put(c, new Integer((red1 << 16)
                                    | (green1 << 8) | blue1));
                        }
                    }
                }
            }
        }
        synchronized (this) {
            ecmHexes = table;
            redrawWholeBoard = true;
        }
        repaint();
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
        final Dimension size = scrollpane.getViewport().getSize();
        if (arg1 == SwingConstants.VERTICAL) {
            return size.height;
        }
        return size.width;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
        if (arg1 == SwingConstants.VERTICAL) {
            return (int) ((scale * HEX_H) / 2.0);
        }
        return (int) ((scale * HEX_W) / 2.0);
    }

//    @Override
//    public Dimension getPreferredSize() {
//        return clientgui.frame
//    	//return boardSize;
//    }

    /**
     * Have the player select an Entity from the entities at the given coords.
     * 
     * @param pos
     *            - the <code>Coords</code> containing targets.
     */
    private Entity chooseEntity(Coords pos) {

        // Assume that we have *no* choice.
        Entity choice = null;

        // Get the available choices.
        Enumeration<Entity> choices = game.getEntities(pos);

        // Convert the choices into a List of targets.
        Vector<Entity> entities = new Vector<Entity>();
        while (choices.hasMoreElements()) {
            entities.addElement(choices.nextElement());
        }

        // Do we have a single choice?
        if (entities.size() == 1) {
            // Return that choice.
            choice = entities.elementAt(0);
        }

        // If we have multiple choices, display a selection dialog.
        else if (entities.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(
                            null,
                            Messages.getString(
                                    "BoardView1.ChooseEntityDialog.message", new Object[] { pos.getBoardNum() }), //$NON-NLS-1$
                            Messages.getString("BoardView1.ChooseEntityDialog.title"), //$NON-NLS-1$
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(entities), null);
            choice = (Entity) SharedUtility.getTargetPicked(entities, input);
        } // End have-choices

        // Return the chosen unit.
        return choice;
    }


    /**
     * The text to be displayed when the mouse is at a certain point.
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        // If new instance of mouse event, redraw obscured hexes and elevations.
        repaint();

        StringBuffer txt = new StringBuffer();
        IHex mhex = null;
        final Point point = e.getPoint();
        final Coords mcoords = getCoordsAt(point);
        final ArrayList<ArtilleryAttackAction> artilleryAttacks = 
                getArtilleryAttacksAtLocation(mcoords);
        final Mounted curWeapon = getSelectedArtilleryWeapon();
        
        if (GUIPreferences.getInstance().getShowMapHexPopup()
                && game.getBoard().contains(mcoords)) {
            mhex = game.getBoard().getHex(mcoords);
        }
        
        txt.append("<html>"); //$NON-NLS-1$

        // are we on a hex?
        if (mhex != null) {
            txt.append(Messages.getString("BoardView1.Hex") + //$NON-NLS-1$ 
                    mcoords.getBoardNum()); 
            txt.append(Messages.getString("BoardView1.level") + //$NON-NLS-1$ 
                    mhex.getElevation()); 
            txt.append("<br>"); //$NON-NLS-1$

            // cycle through the terrains and report types found
            // this will skip buildings and other constructed units
            int terrainTypes[] = mhex.getTerrainTypes();
            for (int i = 0; i < terrainTypes.length; i++) {
                int terType = terrainTypes[i];
                if (mhex.containsTerrain(terType)) {
                    int tf = mhex.getTerrain(terType).getTerrainFactor();
                    int ttl = mhex.getTerrain(terType).getLevel();
                    String name = Terrains.getDisplayName(terType, ttl);
                    if (tf > 0) {
                        name = name + " (" + tf + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    if (null != name) {
                        txt.append(name);
                        txt.append("<br>"); //$NON-NLS-1$
                    }
                }
            }

            // Do we have a building?
            if (mhex.containsTerrain(Terrains.FUEL_TANK)) {
                // Get the building.
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                StringBuffer buf = new StringBuffer(
                        Messages.getString("BoardView1.Height")); //$NON-NLS-1$
                // Each hex of a building has its own elevation.
                buf.append(mhex.terrainLevel(Terrains.FUEL_TANK_ELEV));
                buf.append(" "); //$NON-NLS-1$
                buf.append(bldg.toString());
                buf.append(Messages.getString("BoardView1.CF")); //$NON-NLS-1$
                buf.append(bldg.getCurrentCF(mcoords));
                txt.append(buf.toString());
                txt.append("<br>"); //$NON-NLS-1$
            }
            if (mhex.containsTerrain(Terrains.BUILDING)) {
                // Get the building.
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                // in the map editor, the building might not exist
                if (bldg != null) {
                    StringBuffer buf = new StringBuffer(
                            Messages.getString("BoardView1.Height")); //$NON-NLS-1$
                    // Each hex of a building has its own elevation.
                    buf.append(mhex.terrainLevel(Terrains.BLDG_ELEV));
                    buf.append(" "); //$NON-NLS-1$
                    buf.append(bldg.toString());
                    buf.append(Messages.getString("BoardView1.CF")); //$NON-NLS-1$
                    buf.append(bldg.getCurrentCF(mcoords));
                    buf.append(Messages.getString("BoardView1.BldgArmor")); //$NON-NLS-1$
                    buf.append(bldg.getArmor(mcoords));
                    buf.append(Messages.getString("BoardView1.BldgBasement"));
                    buf.append(bldg.getBasement(mcoords).getDesc());
                    if (bldg.getBasementCollapsed(mcoords)) {
                        buf.append(Messages
                                .getString("BoardView1.BldgBasementCollapsed")); //$NON-NLS-1$
                    }
                    txt.append(buf.toString());
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }

            // Do we have a bridge?
            if (mhex.containsTerrain(Terrains.BRIDGE)) {
                // Get the building.
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                StringBuffer buf = new StringBuffer(
                        Messages.getString("BoardView1.Height")); //$NON-NLS-1$
                // Each hex of a building has its own elevation.
                buf.append(mhex.terrainLevel(Terrains.BRIDGE_ELEV));
                buf.append(" "); //$NON-NLS-1$
                buf.append(bldg.toString());
                buf.append(Messages.getString("BoardView1.CF")); //$NON-NLS-1$
                buf.append(bldg.getCurrentCF(mcoords));
                txt.append(buf.toString());
                txt.append("<br>"); //$NON-NLS-1$
            }

            if (game.containsMinefield(mcoords)) {
                Vector<Minefield> minefields = game.getMinefields(mcoords);
                for (int i = 0; i < minefields.size(); i++) {
                    Minefield mf = minefields.elementAt(i);
                    String owner = " (" //$NON-NLS-1$
                    + game.getPlayer(mf.getPlayerId()).getName() 
                    + ")"; //$NON-NLS-1$

                    switch (mf.getType()) {
                    case (Minefield.TYPE_CONVENTIONAL):
                        txt.append(mf.getName()
                                + Messages.getString("BoardView1.minefield") //$NON-NLS-1$
                                + "(" + mf.getDensity() + ")" + " " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        break;
                    case (Minefield.TYPE_COMMAND_DETONATED):
                        txt.append(mf.getName()
                                + Messages.getString("BoardView1.minefield") //$NON-NLS-1$
                                + "(" + mf.getDensity() + ")" + " " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        break;
                    case (Minefield.TYPE_VIBRABOMB):
                        if (mf.getPlayerId() == localPlayer.getId()) {
                            txt.append(mf.getName()
                                    + Messages
                                            .getString("BoardView1.minefield") //$NON-NLS-1$
                                    + "(" + mf.getDensity() + ")" + "(" //$NON-NLS-1$
                                    + mf.getSetting() + ") " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        } else {
                            txt.append(mf.getName()
                                    + Messages
                                            .getString("BoardView1.minefield") //$NON-NLS-1$
                                    + "(" + mf.getDensity() + ")" + " " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        break;
                    case (Minefield.TYPE_ACTIVE):
                        txt.append(mf.getName()
                                + Messages.getString("BoardView1.minefield") //$NON-NLS-1$
                                + "(" + mf.getDensity() + ")" + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        break;
                    case (Minefield.TYPE_INFERNO):
                        txt.append(mf.getName()
                                + Messages.getString("BoardView1.minefield") //$NON-NLS-1$
                                + "(" + mf.getDensity() + ")" + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        break;
                    }
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }
        
        }
        // check if it's on any entities
        for (EntitySprite eSprite : entitySprites) {
            if (eSprite.isInside(point)) {
                final String[] entityStrings = eSprite.getTooltip();
                for (String entString : entityStrings){
                    txt.append(entString);
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }
        }

        // check if it's on any attacks
        for (AttackSprite aSprite : attackSprites) {
            if (aSprite.isInside(point)) {
                final String[] attackStrings = aSprite.getTooltip();
                for (String attString : attackStrings){
                    txt.append(attString);
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }
        }

        // check artillery attacks
        for (ArtilleryAttackAction aaa : artilleryAttacks) {
            final Entity ae = game.getEntity(aaa.getEntityId());
            String s = null;
            if (ae != null) {
                if (aaa.getWeaponId() > -1) {
                    Mounted weap = ae.getEquipment(aaa.getWeaponId());
                    s = weap.getName();
                    if (aaa.getAmmoId() > -1) {
                        Mounted ammo = ae.getEquipment(aaa.getAmmoId());
                        s += " (" + ammo.getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            }
            if (s == null) {
                s = Messages.getString("BoardView1.Artillery");
            }
            txt.append(Messages.getString(
                    "BoardView1.ArtilleryAttack", new Object[] { s, //$NON-NLS-1$
                            new Integer(aaa.turnsTilHit)}));
            txt.append("<br>"); //$NON-NLS-1$
        }

        // check artillery fire adjustment
        if ((curWeapon != null) && (selectedEntity != null)) {
            // process targetted hexes
            int amod = 0;
            // Check the predesignated hexes
            if (selectedEntity.getOwner().getArtyAutoHitHexes()
                    .contains(mcoords)) {
                amod = TargetRoll.AUTOMATIC_SUCCESS;
            } else {
                amod = selectedEntity.aTracker.getModifier(curWeapon, mcoords);
            }

            if (amod == TargetRoll.AUTOMATIC_SUCCESS) {
                txt.append(Messages
                        .getString("BoardView1.ArtilleryAutohit")); //$NON-NLS-1$
                txt.append("<br>"); //$NON-NLS-1$
            } else {
                txt.append(Messages.getString(
                        "BoardView1.ArtilleryAdjustment", //$NON-NLS-1$
                        new Object[] { new Integer(amod) }));
                txt.append("<br>"); //$NON-NLS-1$
            }
        }
        
        final Collection<SpecialHexDisplay> shdList = game.getBoard()
                .getSpecialHexDisplay(mcoords);
        if (shdList != null) {
            for (SpecialHexDisplay shd : shdList) {
                if (!shd.isObscured(localPlayer)) {
                    if (shd.getType() == SpecialHexDisplay.Type.PLAYER_NOTE){
                        if (localPlayer.equals(shd.getOwner())){
                            txt.append("Note: ");
                        } else {
                            txt.append("Note (" + shd.getOwner().getName()
                                    + "): ");
                        }
                    }
                    String buf = shd.getInfo();
                    buf = buf.replaceAll("\\n", "<br>");
                    txt.append(buf);
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }
        }        

        txt.append("</html>"); //$NON-NLS-1$
        return txt.toString();
    }

    private ArrayList<ArtilleryAttackAction> getArtilleryAttacksAtLocation(
            Coords c) {
        ArrayList<ArtilleryAttackAction> v = new ArrayList<ArtilleryAttackAction>();
        for (Enumeration<ArtilleryAttackAction> attacks = game
                .getArtilleryAttacks(); attacks.hasMoreElements();) {
            ArtilleryAttackAction a = attacks.nextElement();
            if (a.getTarget(game).getPosition().equals(c)) {
                v.add(a);
            }
        }
        return v;
    }

    public Component getComponent() {
    	return getComponent(false);
    }
    
    public Component getComponent(boolean scrollBars) {
        if (scrollpane != null) {
            return scrollpane;
        }
        
        SkinSpecification bvSkinSpec = 
        		SkinXMLHandler.getSkin(SkinXMLHandler.BOARDVIEW);
        
        try {
            java.net.URI imgURL;
            File file;
            if (bvSkinSpec.backgrounds.size() > 0){
            	file = new File(Configuration.widgetsDir(), 
            			bvSkinSpec.backgrounds.get(0));
    			imgURL = file.toURI();
    			if (!file.exists()){
    				System.err.println("BoardView1 Error: icon doesn't exist: "
    						+ file.getAbsolutePath());
    			} else {
    				bvBgIcon = new ImageIcon(imgURL.toURL());
    			}
            }
            if (bvSkinSpec.backgrounds.size() > 1){
            	file = new File(Configuration.widgetsDir(), 
            			bvSkinSpec.backgrounds.get(1));
    			imgURL = file.toURI();
    			if (!file.exists()){
    				System.err.println("BoardView1 Error: icon doesn't exist: "
    						+ file.getAbsolutePath());
    			} else {
    				scrollPaneBgIcon = new ImageIcon(imgURL.toURL());
    			}
            }
        } catch (Exception e){
        	System.out.println("Error loading BoardView background images!");
        	System.out.println(e.getMessage());
        }
        
        // Place the board viewer in a set of scrollbars.
        scrollpane = new JScrollPane(this){
            
        	/**
			 * 
			 */
			private static final long serialVersionUID = 5973610449428194319L;

			protected void paintComponent(Graphics g) {
        		if (scrollPaneBgIcon == null){
        			super.paintComponent(g);
        		}
                int w = getWidth();
                int h = getHeight();
                int iW = scrollPaneBgIcon.getIconWidth();
                int iH = scrollPaneBgIcon.getIconHeight();
                for (int x = 0; x < w; x+=iW){
                    for (int y = 0; y < h; y+=iH){
                        g.drawImage(scrollPaneBgIcon.getImage(), x, y, 
                        		scrollPaneBgIcon.getImageObserver());
                    }
                }
            }
        };
        scrollpane.setBorder(new MegamekBorder(bvSkinSpec));
        scrollpane.setLayout(new ScrollPaneLayout());
        // we need to use the simple scroll mode because otherwise the
        // IDisplayables that are drawn in fixed positions in the viewport
        // leave artifacts when scrolling
        scrollpane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        vbar = scrollpane.getVerticalScrollBar();
        
        hbar = scrollpane.getHorizontalScrollBar();
        if (!scrollBars){
        	vbar.setPreferredSize(new Dimension(0, vbar.getHeight()));
        	hbar.setPreferredSize(new Dimension(hbar.getWidth(),0));
        }

        return scrollpane;
    }

    /**
     * refresh the IDisplayables
     */
    public void refreshDisplayables() {
        repaint();
    }

    public void showPopup(Object popup, Coords c) {
        Point p = getHexLocation(c);
        p.x += (int) (HEX_WC * scale) - scrollpane.getX();
        p.y += (int) ((HEX_H * scale) / 2) - scrollpane.getY();
        if (((JPopupMenu) popup).getParent() == null) {
            add((JPopupMenu) popup);
        }
        ((JPopupMenu) popup).show(this, p.x, p.y);
    }

    public void refreshMinefields() {
        repaint();
    }

    /**
     * Increases zoomIndex and refreshes the map.
     */
    public void zoomIn() {
        if (zoomIndex == (ZOOM_FACTORS.length - 1)) {
            return;
        }
        zoomIndex++;
        zoom();
    }

    /**
     * Decreases zoomIndex and refreshes the map.
     */
    public void zoomOut() {
        if (zoomIndex == 0) {
            return;
        }
        zoomIndex--;
        zoom();
    }

    public void hideTooltip() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void checkZoomIndex() {
        if (zoomIndex > (ZOOM_FACTORS.length - 1)) {
            zoomIndex = ZOOM_FACTORS.length - 1;
        }
        if (zoomIndex < 0) {
            zoomIndex = 0;
        }
    }

    /**
     * Changes hex dimensions and refreshes the map with the new scale
     */
    private void zoom() {
        checkZoomIndex();
        scale = ZOOM_FACTORS[zoomIndex];
        GUIPreferences.getInstance().setMapZoomIndex(zoomIndex);

        hex_size = new Dimension((int) (HEX_W * scale), (int) (HEX_H * scale));

        scaledImageCache = new ImageCache<Integer, Image>();

        cursorSprite.prepare();
        highlightSprite.prepare();
        selectedSprite.prepare();
        firstLOSSprite.prepare();
        secondLOSSprite.prepare();

        updateFontSizes();
        updateBoard();
        for (StepSprite sprite : pathSprites) {
            sprite.refreshZoomLevel();
        }
        for (FiringSolutionSprite sprite : firingSprites) {
            sprite.refreshZoomLevel();
        }
        this.setSize(boardSize);
        redrawWholeBoard = true;

        repaint();
    }

    private void updateFontSizes() {
        if (zoomIndex <= 4) {
            font_elev = FONT_7;
            font_hexnum = FONT_7;
            font_minefield = FONT_7;
        }
        if ((zoomIndex <= 5) & (zoomIndex > 4)) {
            font_elev = FONT_8;
            font_hexnum = FONT_8;
            font_minefield = FONT_8;
        }
        if (zoomIndex > 5) {
            font_elev = FONT_9;
            font_hexnum = FONT_9;
            font_minefield = FONT_9;
        }
    }

    /**
     * Return a scaled version of the input.  If the useCache flag is set, the
     * scaled image will be stored in an image cache for later retrieval.
     * 
     * @param base   The image to get a scaled copy of.  The current zoom level
     *                  is used to determine the scale.
     *                  
     * @param useCache  This flag determines whether the scaled image should
     *                      be stored in a cache for later retrieval.                    
     */
    Image getScaledImage(Image base, boolean useCache) {
        if (base == null) {
            return null;
        }
        if (zoomIndex == BASE_ZOOM_INDEX) {
            return base;
        }

        
        Image scaled;
        if (useCache){
            // Check the cache
            scaled = scaledImageCache.get(base.hashCode());
        } else {
            scaled = null;
        }
        // Compute the scaled image
        if (scaled == null) {
            MediaTracker tracker = new MediaTracker(this);
            if ((base.getWidth(null) == -1) || (base.getHeight(null) == -1)) {
                tracker.addImage(base, 0);
                try {
                    tracker.waitForID(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tracker.removeImage(base);
            }
            int width = (int) (base.getWidth(null) * scale);
            int height = (int) (base.getHeight(null) * scale);

            // TODO: insert a check that width and height are > 0.

            scaled = scale(base, width, height);
            tracker.addImage(scaled, 1);
            // Wait for image to load
            try {
                tracker.waitForID(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tracker.removeImage(scaled);
            // Cache the image if the flag is set
            if (useCache){
                scaledImageCache.put(base.hashCode(), scaled);
            }            
        }
        return scaled;
    }

    /**
     * The actual scaling code.
     */
    private Image scale(Image img, int width, int height) {
        ImageFilter filter;
        filter = new ImprovedAveragingScaleFilter(img.getWidth(null),
                img.getHeight(null), width, height);

        ImageProducer prod;
        prod = new FilteredImageSource(img.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(prod);
    }

    public boolean toggleIsometric() {
        drawIsometric = !drawIsometric;
        updateBoard();
        repaint();
        redrawWholeBoard = true;
        return drawIsometric;
    }

    private BufferedImage createShadowMask(Image image) {
        BufferedImage mask = new BufferedImage(image.getWidth(null),
                image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        float opacity = 0.4f;
        Graphics2D g2d = mask.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN,
                opacity));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
        g2d.dispose();
        return mask;
    }

    /**
     * Moves the board view to another area.
     */
    private void moveBoardImage() {
        Rectangle viewRect = scrollpane.getViewport().getViewRect();
        // salvage the old
        boardGraph.setClip(0, 0, drawRect.width, drawRect.height);

        boardGraph.copyArea(0, 0, drawRect.width, drawRect.height, drawRect.x
                - viewRect.x, drawRect.y - viewRect.y);

        // what's left to paint?
        int midX = Math.max(viewRect.x, drawRect.x);
        int midWidth = viewRect.width - Math.abs(viewRect.x - drawRect.x);
        Rectangle unLeft = new Rectangle(viewRect.x, viewRect.y, drawRect.x
                - viewRect.x, viewRect.height);
        Rectangle unRight = new Rectangle(viewRect.x - drawRect.x, viewRect.y,
                drawRect.x + drawRect.width, viewRect.height);
        Rectangle unTop = new Rectangle(midX, viewRect.y, midWidth, drawRect.y
                - viewRect.y);
        Rectangle unBottom = new Rectangle(midX, drawRect.y + drawRect.height,
                midWidth, viewRect.y - drawRect.y);

        // update drawRect
        drawRect = new Rectangle(viewRect);

        // paint needed areas
        if (unLeft.width > 0) {
            drawHexes(boardGraph, unLeft);
        } else if (unRight.width > 0) {
            drawHexes(boardGraph, unRight);
        }
        if (unTop.height > 0) {
            drawHexes(boardGraph, unTop);
        } else if (unBottom.height > 0) {
            drawHexes(boardGraph, unBottom);
        }
    }

    private void updateBoardImage() {
        // draw bord only if we moved the viewport
        Rectangle viewRect = scrollpane.getViewport().getViewRect();
        if ((boardGraph == null) || (viewRect.width > drawRect.width)
                || (viewRect.height > drawRect.height) || redrawWholeBoard) {
            drawRect = scrollpane.getViewport().getViewRect();
            boardImage = createImage(drawRect.width, drawRect.height);
            if (boardGraph != null) {
                boardGraph.dispose();
            }
            boardGraph = boardImage.getGraphics();
            boardGraph.setClip(0, 0, drawRect.width, drawRect.height);
            drawHexes(boardGraph, drawRect);
            redrawWholeBoard = false;
        }
        if (!drawRect.union(viewRect).equals(drawRect)) {
            moveBoardImage();
        }
    }

    public void die() {
        ourTask.cancel();
    }
    
    /**
     * Returns true if the BoardView has an active chatter box else false.
     * @return
     */
    public boolean getChatterBoxActive(){
    	return chatterBoxActive;
    }

    /**
     * Sets whether the BoardView has an active chatter box or not.
     * @param cba
     */
    public void setChatterBoxActive(boolean cba){
    	chatterBoxActive = cba;
    }
}
