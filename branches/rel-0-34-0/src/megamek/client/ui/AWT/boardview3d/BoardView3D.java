/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
 *
 * This file also (C) 2008 Jörg Walter <j.walter@syntax-k.de>
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

package megamek.client.ui.AWT.boardview3d;

import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.pickfast.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Vector;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.PickInfo;
import javax.media.j3d.Transform3D;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.event.MechDisplayEvent;
import megamek.client.event.MechDisplayListener;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.AWT.GUIPreferences;
import megamek.client.ui.AWT.TilesetManager;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.MovePath;
import megamek.common.Player;
import megamek.common.UnitLocation;
import megamek.common.actions.AttackAction;
import megamek.common.event.*;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;

// Optimization TODO:
// - set behaviour bounds as small as actually needed
// - explore the use of SharedGroups

/**
 * Displays the board; lets the user scroll around and select points on it.
 */
public class BoardView3D extends Canvas3D implements megamek.client.ui.IBoardView,
        BoardListener, MouseListener, MouseMotionListener, MechDisplayListener,
        IPreferenceChangeListener, GameListener {
    // FIXME: this isn't actually serializable, I guess.
    static final long serialVersionUID = 475073852535962574L;

    // geometry tree / important subtrees
    private SimpleUniverse universe;

    private BoardModel board;

    private AttackGroup attacks;

    private EntityGroup entities;

    private BranchGroup cursors;

    private MoveGroup moves;

    private CursorModel cursor, selectCursor, highlightCursor, firstLOSCursor, secondLOSCursor;

    private ViewTransform currentView;

    private HoverInfo hoverInfo;

    // displayables
    private Vector<IDisplayable> displayables = new Vector<IDisplayable>();

    private BufferedImage bi1, bi2;

    // mouse event control
    private PickCanvas pickBoard, pickEntities;

    boolean dragged;

    private Coords lastCursor;

    private Coords selected;

    private Coords firstLOS;

    private ConnectionModel ruler;

    private static final Color3f LOS_COLOR = C.red;

    // Game data
    private IGame game;

    private TileTextureManager tileManager;

    private Player localPlayer;

    private Vector<BoardViewListener> boardListeners = new Vector<BoardViewListener>();

    static {
        System.setProperty("j3d.implicitAntialiasing", "true");
        // System.setProperty("j3d.rend", "ogl");
    }

    /**
     * Construct a new board view for the specified game
     *
     * @param game
     * @throws java.io.IOException
     */
    public BoardView3D(IGame game) throws java.io.IOException {
        super(SimpleUniverse.getPreferredConfiguration());
        this.game = game;

        universe = new SimpleUniverse(this, ViewTransform.MAX_TRANSFORMS);
        ViewingPlatform vp = universe.getViewingPlatform();

        vp.setNominalViewingTransform();
        View v = universe.getViewer().getView();
        v.setMinimumFrameCycleTime(40); // 40ms = 25 FPS
        v.setSceneAntialiasingEnable(false);
        v.setBackClipDistance(1000 * BoardModel.HEX_DIAMETER);
        v.setBackClipPolicy(View.VIRTUAL_EYE);
        v.setFrontClipDistance(BoardModel.HEX_DIAMETER / 3);
        v.setFrontClipPolicy(View.VIRTUAL_EYE);
        v.setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);

        tileManager = new TileTextureManager(this, game);

        cursors = new BranchGroup();
        cursors.setPickable(false);
        cursors.addChild(cursor = new CursorModel(C.cyan));
        cursors.addChild(highlightCursor = new CursorModel(C.white));
        cursors.addChild(selectCursor = new CursorModel(C.blue));
        cursors.addChild(firstLOSCursor = new CursorModel(LOS_COLOR));
        cursors.addChild(secondLOSCursor = new CursorModel(LOS_COLOR));
        cursors.setCapability(Group.ALLOW_CHILDREN_WRITE);
        cursors.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        cursors.compile();
        universe.addBranchGraph(cursors);

        attacks = new AttackGroup(game, tileManager, currentView);
        universe.addBranchGraph(attacks);

        entities = new EntityGroup(game, tileManager, currentView);
        universe.addBranchGraph(entities);

        board = new BoardModel(tileManager, universe, game);
        universe.addBranchGraph(board);

        moves = new MoveGroup(game, currentView);
        universe.addBranchGraph(moves);

        pickBoard = new PickCanvas(this, board);
        pickBoard.setMode(PickInfo.PICK_BOUNDS);
        pickBoard.setTolerance(0.0f);
        pickBoard.setFlags(PickInfo.NODE | PickInfo.LOCAL_TO_VWORLD);

        pickEntities = new PickCanvas(this, entities);
        pickEntities.setMode(PickInfo.PICK_BOUNDS);
        pickEntities.setTolerance(0.0f);
        pickEntities.setFlags(PickInfo.NODE | PickInfo.LOCAL_TO_VWORLD);

        bi1 = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
        bi2 = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);

        hoverInfo = new HoverInfo(game, this);
        addDisplayable(hoverInfo);

        setView(Integer.parseInt(System.getProperty("megamek.client.ui.AWT.boardview3d.view", "0")));

        game.addGameListener(this);
        game.getBoard().addBoardListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        PreferenceManager.getClientPreferences().addPreferenceChangeListener(this);
        setBackground(Color.DARK_GRAY);
    }

    String setView(int index) {
        if (currentView != null) {
            currentView.remove();
        }
        currentView = ViewTransform.create(index, universe);
        entities.setView(currentView);
        moves.setView(currentView);
        attacks.setView(currentView);
        return currentView.getName();
    }

    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(IClientPreferences.MAP_TILESET)) {
            updateBoard();
        } else {
            entities.update();
        }
    }

    /**
     * Adds the specified board listener to receive board events from this board.
     *
     * @param listener
     *            the board listener.
     */
    public void addBoardViewListener(BoardViewListener listener) {
        if (!boardListeners.contains(listener)) {
            boardListeners.addElement(listener);
        }
    }

    /**
     * Removes the specified board listener.
     *
     * @param listener
     *            the board listener.
     */
    public void removeBoardViewListener(BoardViewListener listener) {
        boardListeners.removeElement(listener);
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

        for (Object name2 : boardListeners) {
            BoardViewListener l = (BoardViewListener) name2;
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
                l.secondLOSHex(event, firstLOS);
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

    public void addDisplayable(IDisplayable disp) {
        displayables.addElement(disp);
        repaint();
    }

    @Override
    public void postRender() {
        try {
            getGraphics2D().drawAndFlushImage(bi1, 0, 0, null);
            getGraphics2D().drawAndFlushImage(bi2, getWidth() - bi2.getWidth(), 0, null);
        } catch (IllegalStateException ise) {
        }
    }

    public void showPopup(Object popup, Coords c) {
        if (((PopupMenu)popup).getParent() == null) {
            add((PopupMenu)popup);
        }

        IHex hex = game.getBoard().getHex(c);
        int level = 0;
        try {
            level = hex.getElevation();
            Entity e = game.getEntities(c).nextElement();
            level += e.getElevation();
        } catch (Exception e) {
        }

        Transform3D v2i = new Transform3D();
        getVworldToImagePlate(v2i);
        Point3d p = BoardModel.getHexLocation(c, level);
        v2i.transform(p);
        Point2d pixel = new Point2d();
        getPixelLocationFromImagePlate(p, pixel);
        ((PopupMenu)popup).show(this, (int) pixel.x, (int) pixel.y);
    }

    private Object pickSomething(MouseEvent me) {
        try {
            if (pickEntities == null || pickBoard == null) {
                return null;
            }

            PickInfo target;
            Node node;

            pickEntities.setShapeLocation(me);
            target = pickEntities.pickClosest();

            if (target == null) {
                pickBoard.setShapeLocation(me);
                target = pickBoard.pickClosest();
            }
            if (target == null) {
                return null;
            }

            node = target.getNode();
            while (node != null && node.getUserData() == null) {
                node = node.getParent();
            }
            if (node == null) {
                return null;
            }

            return node.getUserData();

        } catch (IllegalStateException ise) {
            // ignore early clicks
            return null;
        }
    }

    private Coords pickCoords(MouseEvent me) {
        Object target = pickSomething(me);
        if (target instanceof Coords) {
            return (Coords) target;
        } else if (target instanceof Entity) {
            return ((Entity) target).getPosition();
        }

        return null;
    }

    public void hideTooltip() {
    }

    public boolean isMovingUnits() {
        return entities.isMoving();
    }

    public void redrawEntity(Entity entity) {
        entities.update(entity);
    }

    public void centerOnHex(Coords c) {
        if (c == null || currentView == null || !game.getBoard().contains(c)) {
            return;
        }

        currentView.centerOnHex(c, game.getBoard().getHex(c));
    }

    public void drawMovementData(Entity entity, MovePath md) {
        moves.set(md);
    }

    public void clearMovementData() {
        moves.clear();
    }

    public void setLocalPlayer(Player p) {
        localPlayer = p;
        hoverInfo.setSelected(null, null, p);
        refreshDisplayables();
    }

    public Player getLocalPlayer() {
        return localPlayer;
    }

    public void markDeploymentHexesFor(Player p) {
        board.showDeployment(p);
    }

    public void addAttack(AttackAction aa) {
        attacks.add(aa);
        hoverInfo.add(aa);
        refreshDisplayables();
    }

    public void removeAttacksFor(Entity entity) {
        attacks.remove(entity);
        hoverInfo.remove(entity);
        refreshDisplayables();
    }

    public void refreshAttacks() {
        attacks.update();
        hoverInfo.update();
        refreshDisplayables();
    }

    public void mousePressed(MouseEvent me) {
        Coords c = pickCoords(me);
        if (c == null) {
            return;
        }
        dragged = false;

        if (me.isPopupTrigger() && !me.isControlDown()) {
            processBoardViewEvent(new BoardViewEvent(this, c, null, BoardViewEvent.BOARD_HEX_POPUP,
                    me.getModifiers()));
        }
    }

    public void mouseReleased(MouseEvent me) {
        Coords c = pickCoords(me);
        if (c == null) {
            return;
        }

        if (me.isPopupTrigger() && !me.isControlDown() && !dragged) {
            processBoardViewEvent(new BoardViewEvent(this, c, null, BoardViewEvent.BOARD_HEX_POPUP,
                    me.getModifiers()));
        }
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseClicked(MouseEvent me) {

        for (int i = 0; i < displayables.size(); i++) {
            IDisplayable disp = displayables.elementAt(i);
            if (disp.isHit(me.getPoint(), getSize())) {
                disp.isReleased();
                return;
            }
        }

        Coords c = pickCoords(me);
        if (c == null) {
            return;
        }

        if (me.isPopupTrigger() && !me.isControlDown()) {
            processBoardViewEvent(new BoardViewEvent(this, c, null, BoardViewEvent.BOARD_HEX_POPUP,
                    me.getModifiers()));
        } else if (me.getClickCount() == 1 && me.isControlDown()) {
            if (c.equals(hoverInfo.getLOS())) {
                firstLOSCursor.hide();
                hoverInfo.setLOS(null);
            } else {
                firstLOSCursor.move(c, game.getBoard().getHex(c));
                firstLOSCursor.setColor(LOS_COLOR);
                hoverInfo.setLOS(c);
            }
            secondLOSCursor.hide();
            hoverInfo.setPosition(c);
            refreshDisplayables();
        } else if (me.getClickCount() == 1) {
            processBoardViewEvent(new BoardViewEvent(this, c, null,
                    BoardViewEvent.BOARD_HEX_DRAGGED, me.getModifiers()));
            processBoardViewEvent(new BoardViewEvent(this, c, null,
                    BoardViewEvent.BOARD_HEX_CLICKED, me.getModifiers()));
        } else {
            processBoardViewEvent(new BoardViewEvent(this, c, null,
                    BoardViewEvent.BOARD_HEX_DOUBLECLICKED, me.getModifiers()));
        }
    }

    public void drawRuler(Coords s, Coords e, Color sc, Color ec) {
        if (ruler != null) {
            ruler.detach();
        }
        ruler = null;
        firstLOSCursor.hide();
        secondLOSCursor.hide();
        if (s == null) {
            return;
        }

        IBoard gboard = game.getBoard();
        IHex sh = gboard.getHex(s);

        firstLOSCursor.move(s, sh);
        firstLOSCursor.setColor(new Color3f(sc));

        if (e == null) {
            return;
        }

        IHex eh = gboard.getHex(e);

        ruler = new ConnectionModel(s, e, sh.surface() + 1, eh.surface() + 1, null,
                new Color3f(ec), 0.5f);
        secondLOSCursor.move(e, eh);
        secondLOSCursor.setColor(new Color3f(ec));
        cursors.addChild(ruler);
    }

    /**
     * @return Returns the lastCursor.
     */
    public Coords getLastCursor() {
        return lastCursor;
    }

    /**
     * @return Returns the selected.
     */
    public Coords getSelected() {
        return selected;
    }

    public void setFirstLOS(Coords c) {
        firstLOS = c;
    }

    /**
     * Determines if this Board contains the Coords, and if so, "selects" that Coords.
     *
     * @param coords
     *            the Coords.
     */
    public void select(Coords coords) {
        if (coords != null && !game.getBoard().contains(coords)) {
            return;
        }

        selected = coords;
        selectCursor.move(coords, game.getBoard().getHex(coords));
        firstLOSCursor.hide();
        secondLOSCursor.hide();
        processBoardViewEvent(new BoardViewEvent(this, coords, null,
                BoardViewEvent.BOARD_HEX_SELECTED, 0));
    }

    /**
     * Determines if this Board contains the Coords, and if so, highlights that Coords.
     *
     * @param coords
     *            the Coords.
     */
    public void highlight(Coords coords) {
        if (coords != null && !game.getBoard().contains(coords)) {
            return;
        }

        highlightCursor.move(coords, game.getBoard().getHex(coords));
        firstLOSCursor.hide();
        secondLOSCursor.hide();
        processBoardViewEvent(new BoardViewEvent(this, coords, null,
                BoardViewEvent.BOARD_HEX_HIGHLIGHTED, 0));
    }

    /**
     * Determines if this Board contains the Coords, and if so, "cursors" that Coords.
     *
     * @param coords
     *            the Coords.
     */
    public void cursor(Coords coords) {
        if (coords != null && !game.getBoard().contains(coords)) {
            return;
        }

        if (lastCursor == null || coords == null || !coords.equals(lastCursor)) {
            lastCursor = coords;
            cursor.move(coords, game.getBoard().getHex(coords));
            firstLOSCursor.hide();
            secondLOSCursor.hide();
            processBoardViewEvent(new BoardViewEvent(this, coords, null,
                    BoardViewEvent.BOARD_HEX_CURSOR, 0));
        } else {
            lastCursor = coords;
        }
    }

    public void checkLOS(Coords coords) {
        if (coords != null && !game.getBoard().contains(coords)) {
            return;
        }

        if (hoverInfo.getLOS() == null) {
            hoverInfo.setLOS(coords);
            firstLOSCursor.move(coords, game.getBoard().getHex(coords));
            firstLOSCursor.setColor(LOS_COLOR);
            secondLOSCursor.hide();
        } else {
            secondLOSCursor.move(coords, game.getBoard().getHex(coords));
            secondLOSCursor.setColor(LOS_COLOR);
            hoverInfo.setPosition(coords);
            refreshDisplayables();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.BoardListener#boardNewBoard(megamek.common.BoardEvent)
     */
    public void boardNewBoard(BoardEvent b) {
        updateBoard();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.BoardListener#boardChangedHex(megamek.common.BoardEvent)
     */
    public void boardChangedHex(BoardEvent b) {
        IHex hex = game.getBoard().getHex(b.getCoords());
        tileManager.hexChanged(hex);
        board.update(b.getCoords(), hex, localPlayer);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.BoardListener#boardChangedHex(megamek.common.BoardEvent)
     */
    public void boardChangedAllHexes(BoardEvent b) {
        tileManager.getTilesetManager().loadAllHexes();
        board.update(localPlayer);
    }

    public void gameEntityNew(GameEntityNewEvent e) {
        for (Entity en : e.GetEntities()) {
            redrawEntity(en);
        }
        refreshDisplayables();
    }

    public void gameEntityRemove(GameEntityRemoveEvent e) {
        entities.remove(e.getEntity());
        refreshDisplayables();
    }

    public void gameEntityChange(GameEntityChangeEvent e) {
        Vector<UnitLocation> mp = e.getMovePath();
        if (mp != null && mp.size() > 0 && GUIPreferences.getInstance().getShowMoveStep()) {
            entities.move(e.getEntity(), mp);
        } else {
            entities.update(e.getEntity());
        }
        refreshDisplayables();
    }

    public void gameNewAction(GameNewActionEvent e) {
    }

    public void gameBoardNew(GameBoardNewEvent e) {
        IBoard b = e.getOldBoard();
        if (b != null) {
            b.removeBoardListener(BoardView3D.this);
        }
        b = e.getNewBoard();
        if (b != null) {
            b.addBoardListener(BoardView3D.this);
        }
        updateBoard();
        if (b != null) {
            centerOnHex(new Coords(0, 0));
        }
    }

    public void gameBoardChanged(GameBoardChangeEvent e) {
        updateBoard();
    }

    public void gamePhaseChange(GamePhaseChangeEvent e) {
        refreshAttacks();
        switch (e.getNewPhase()) {
        case PHASE_MOVEMENT:
        case PHASE_FIRING:
        case PHASE_PHYSICAL:
            refreshAttacks();
            break;
        case PHASE_INITIATIVE:
            attacks.clear();
            hoverInfo.clear();
            break;
        case PHASE_END:
        case PHASE_VICTORY:
            attacks.clear();
            hoverInfo.clear();
            clearMovementData();
        default:
        }
        refreshDisplayables();
    }

    private void updateBoard() {
        board.update(localPlayer);
        entities.update();
        refreshDisplayables();
    }

    public void WeaponSelected(MechDisplayEvent b) {
        attacks.setSelected(b.getEntity(), b.getEquip(), localPlayer);
        hoverInfo.setSelected(b.getEntity(), b.getEquip(), localPlayer);
        refreshDisplayables();
    }

    public void zoomIn() {
        currentView.zoom(1);
    }

    public void zoomOut() {
        currentView.zoom(-1);
    }

    public Component getComponent() {
        return this;
    }

    public TilesetManager getTilesetManager() {
        return tileManager.getTilesetManager();
    }

    public void refreshMinefields() {
        updateBoard();
    }

    public void refreshDisplayables() {
        Dimension size = getSize();
        if (size.width <= 0 || size.height <= 0) {
            return;
        }

        // common hardware limitation
        if (size.width > 2048) {
            size.width = 2048;
        }
        if (size.height > 1024) {
            size.height = 1024;
        }

        BufferedImage b = new BufferedImage(size.width, size.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics gr = b.getGraphics();
        for (int i = 0; i < displayables.size(); i++) {
            displayables.elementAt(i).draw(gr, new Point(size.width, size.height), size);
        }
        BufferedImage b1 = new BufferedImage(size.width / 2, size.height,
                BufferedImage.TYPE_4BYTE_ABGR);
        b1.getGraphics().drawImage(b, 0, 0, null);
        BufferedImage b2 = new BufferedImage(size.width / 2, size.height,
                BufferedImage.TYPE_4BYTE_ABGR);
        b2.getGraphics().drawImage(b, -size.width / 2, 0, null);
        bi1 = b1;
        bi2 = b2;
    }

    // Unused GameListener methods
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
    }

    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
    }

    public void gamePlayerChange(GamePlayerChangeEvent e) {
    }

    public void gamePlayerChat(GamePlayerChatEvent e) {
    }

    public void gameTurnChange(GameTurnChangeEvent e) {
    }

    public void gameReport(GameReportEvent e) {
    }

    public void gameEnd(GameEndEvent e) {
    }

    public void gameSettingsChange(GameSettingsChangeEvent e) {
    }

    public void gameMapQuery(GameMapQueryEvent e) {
    }

    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
    }

    public void mouseDragged(MouseEvent me) {
        dragged = true;
    }

    public void mouseMoved(MouseEvent me) {
        Coords c = pickCoords(me);
        if ((c == null ? c != hoverInfo.coords : !c.equals(hoverInfo.coords))) {
            if (hoverInfo.getLOS() != null) {
                secondLOSCursor.move(c, game.getBoard().getHex(c));
                secondLOSCursor.setColor(LOS_COLOR);
            }
            hoverInfo.setPosition(c);
            refreshDisplayables();
        }
    }

}
