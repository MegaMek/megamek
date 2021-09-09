/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.ui.IBoardView;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.swing.boardview.BoardView1;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.PMUtil;
import megamek.common.Configuration;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;

/**
 *  A graphical chatterbox within the boardview.
 * @author beerockxs2
 *
 */
public class ChatterBox2 implements KeyListener, IDisplayable {

    private static final String FILENAME_BUTTON_UP = "upbutton.gif"; //$NON-NLS-1$
    private static final String FILENAME_BUTTON_DOWN = "downbutton.gif"; //$NON-NLS-1$
    private static final String FILENAME_BUTTON_MINIMISE = "minbutton.gif"; //$NON-NLS-1$
    private static final String FILENAME_BUTTON_MAXIMISE = "maxbutton.gif"; //$NON-NLS-1$
    private static final String FILENAME_BUTTON_RESIZE = "resizebutton.gif"; //$NON-NLS-1$
    private static final Font FONT_CHAT = new Font("SansSerif", Font.BOLD, GUIPreferences.getInstance().getInt("AdvancedChatbox2Fontsize"));  //$NON-NLS-2$
    private static final Color COLOR_TEXT_BACK = Color.black;
    private static final Color COLOR_TEXT_FRONT = Color.white;
    private static final Color COLOR_BACKGROUND;
    private ChatterBox cb;

    static {
        Color temp;
        try {
            temp = GUIPreferences.getInstance().getColor("AdvancedChatbox2BackColor"); //$NON-NLS-1$
            temp = new Color(temp.getRed(), temp.getGreen(), temp.getBlue(), GUIPreferences.getInstance().getInt("AdvancedChatbox2Transparancy")); //$NON-NLS-1$
        } catch (Throwable err) {
            temp = Color.gray;
        }
        COLOR_BACKGROUND = temp;
    }

    private static final int SLIDING_SPEED = 5;
    private static final int MIN_SLIDE_OFFSET = 0;

    private static final int DIST_BOTTOM = 0;
    private static final int DIST_SIDE = 5;

    private static final long MAX_IDLE_TIME = 10000;

    private int height = 150;
    private int width = 400;
    private int max_nbr_rows = 7;

    private boolean isHit = false;
    private boolean resizing = false;
    private boolean scrolling = false;
    private boolean increasedChatScroll = false;
    private boolean decreasedChatScroll = false;
    private boolean overTheTop = false;
    private boolean underTheBottom = false;
    private boolean slidingDown = false;
    private boolean slidingUp = false;
    private boolean lockOpen = false;
    private int chatScroll = 0;
    private int scrollBarHeight;
    private int scrollBarOffset;
    private int slideOffset = 0;
    private long idleTime = 0;
    private float scrollBarStep;
    private float scrollBarDragPos;
    private String message = "";
    private String visibleMessage = "";

    private Point lastScrollPoint;

    private Vector<String> messages = new Vector<String>();

    private Client client;
    private BoardView1 bv;

    private Image upbutton;
    private Image downbutton;
    private Image maxbutton;
    private Image minbutton;
    private Image resizebutton;

    private FontMetrics fm;

    public ChatterBox2(ClientGUI client, IBoardView boardview,
            MegaMekController controller) {
        this.client = client.getClient();
        client.getClient().getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePlayerChat(GamePlayerChatEvent e) {
                addChatMessage(e.getMessage());
            }
            @Override
            public void gameEntityNew(GameEntityNewEvent e) {
                if (PreferenceManager.getClientPreferences()
                        .getPrintEntityChange()) {
                    addChatMessage("MegaMek: " + e.getNumberOfEntities() + 
                            " Entities added.");
                }
            }

            @Override
            public void gameEntityChange(GameEntityChangeEvent e) {
                if (PreferenceManager.getClientPreferences()
                        .getPrintEntityChange()) {
                    addChatMessage("Megamek: " + e.toString());
                }
            }
        });

        bv = (BoardView1)boardview;
        fm = bv.getFontMetrics(FONT_CHAT);

        Toolkit toolkit = bv.getToolkit();
        upbutton = toolkit.getImage(new File(Configuration.widgetsDir(), 
                FILENAME_BUTTON_UP).toString());
        PMUtil.setImage(upbutton, client);
        downbutton = toolkit.getImage(new File(Configuration.widgetsDir(), 
                FILENAME_BUTTON_DOWN).toString());
        PMUtil.setImage(downbutton, client);
        minbutton = toolkit.getImage(new File(Configuration.widgetsDir(), 
                FILENAME_BUTTON_MINIMISE).toString());
        PMUtil.setImage(minbutton, client);
        maxbutton = toolkit.getImage(new File(Configuration.widgetsDir(), 
                FILENAME_BUTTON_MAXIMISE).toString());
        PMUtil.setImage(maxbutton, client);
        resizebutton = toolkit.getImage(new File(Configuration.widgetsDir(), 
                FILENAME_BUTTON_RESIZE).toString());
        PMUtil.setImage(resizebutton, client);

        registerKeyboardCommands(controller);
    }

    private void registerKeyboardCommands(MegaMekController controller) {
        if (controller == null) {
            return;
        }

        // Register the action for CLEAR
        controller.registerCommandAction(KeyCommandBind.CANCEL.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!bv.getChatterBoxActive()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        clearMessage();
                        slideDown();
                    }
                });
    }

    public boolean isReleased() {
        if (scrolling) {
            stopScrolling();
            return true;
        }
        if (resizing) {
            stopScrolling();
            resizing = false;
            lockOpen = false;
            return true;
        }
        
        if (isHit){
            isHit = false;
            return true;
        }            
        return false;
    }

    public boolean isSliding() {
        return slidingDown || slidingUp;
    }

    public void slideUp() {
        setIdleTime(0, false);
        slidingUp = true;
        slidingDown = false;
    }

    public void slideDown() {
        setIdleTime(0, false);
        slidingUp = false;
        slidingDown = true;
        bv.setChatterBoxActive(false);
    }

    private void stopSliding() {
        setIdleTime(0, false);
        slidingUp = false;
        slidingDown = false;
    }

    private boolean isDown() {
        return !isSliding() && (slideOffset == getMaxSlideOffset());
    }

    private boolean isUp() {
        return !isSliding() && (slideOffset == MIN_SLIDE_OFFSET);
    }

    public synchronized void setIdleTime(long timeIdle, boolean add) {
        if (!lockOpen) {
            if (add) {
                idleTime += timeIdle;
            } else {
                idleTime = timeIdle;
            }

            if ((idleTime > MAX_IDLE_TIME) && !isSliding() && 
                    GUIPreferences.getInstance().getBoolean(
                            "AdvancedChatbox2AutoSlidedown")) {
                slideDown();
            }
        }
    }

    private void pageUp() {
        setIdleTime(0, false);
        if (!(chatScroll >= (messages.size() - max_nbr_rows - (max_nbr_rows - 1)))) {
            chatScroll += max_nbr_rows - 1;
        } else {
            chatScroll = messages.size() - max_nbr_rows;
        }
        computeScrollBarOffset();
    }

    private void pageDown() {
        setIdleTime(0, false);
        if (chatScroll > (max_nbr_rows - 1)) {
            chatScroll -= max_nbr_rows - 1;
        } else {
            chatScroll = 0;
        }
        computeScrollBarOffset();
    }

    public boolean slide() {
        if (slidingDown) {
            if (slideOffset < getMaxSlideOffset()) {
                slideOffset += SLIDING_SPEED;
            } else {
                stopSliding();
            }
            slideOffset = Math.min(slideOffset, getMaxSlideOffset());
            return true;
        } else if (slidingUp) {
            if (slideOffset > MIN_SLIDE_OFFSET) {
                slideOffset -= SLIDING_SPEED;
            } else {
                stopSliding();
            }
            slideOffset = Math.max(slideOffset, MIN_SLIDE_OFFSET);
            return true;
        }
        return false;
    }

    private void stopScrolling() {
        scrollBarDragPos = 0;
        lastScrollPoint = null;
        scrolling = false;
        computeScrollBarOffset();
        bv.refreshDisplayables();
        increasedChatScroll = false;
        decreasedChatScroll = false;
    }

    public boolean isDragged(Point p, Dimension size) {
        if (scrolling) {
            scroll(p, size);
            return true;
        }
        if (resizing) {
            resize(p, size);
            return true;
        }
        
        int xMin = DIST_SIDE;
        int xMax = xMin + width;
        int yMin = ((size.height) - height - DIST_BOTTOM) + slideOffset;
        int yMax = yMin + height;

        boolean mouseOver = (p.x > xMin) && (p.x < xMax) && (p.y > yMin)
                && (p.y < yMax);
        return mouseOver;
    }

    public boolean isBeingDragged() {
        return scrolling || resizing ;
    }

    public boolean isMouseOver(Point p, Dimension size) {
        int xMin = DIST_SIDE;
        int xMax = xMin + width;
        int yMin = ((size.height) - height - DIST_BOTTOM) + slideOffset;
        int yMax = yMin + height;

        boolean mouseOver = (p.x > xMin) && (p.x < xMax) && (p.y > yMin)
                && (p.y < yMax);


        // Don't open on mouse over, it is annoying.
        /*if (mouseOver && isDown()) {
            slideUp();
        }*/

        if (mouseOver && isUp()) {
            lockOpen = true;
        }

        if (!mouseOver && isUp()) {
            lockOpen = false;
        }

        return mouseOver;
    }

    public boolean isHit(Point p, Dimension size) {
        if (isSliding()) {
            return false;
        }

        if (message != null) {
            bv.refreshDisplayables();
        }

        int x = p.x;
        int y = p.y;
        int yOffset = ((size.height) - height - DIST_BOTTOM) + slideOffset;

        if ((x < DIST_SIDE) || (x > (DIST_SIDE + width)) || (y < yOffset)
                || (y > (yOffset + height))) {
            bv.setChatterBoxActive(false);
            return false;
        }
        isHit = true;
        // Hide button
        if ((x > 9) && (x < 25) && (y > (yOffset + 2)) && (y < (yOffset + 18)) 
                && !isDown()) {
            slideDown();
            return true;
        }
        
        bv.setChatterBoxActive(true);
        if (isDown()){
            slideUp();
        }

        // Scroll up
        if ((x > (width - 17)) && (x < (width - 1)) && (y > (yOffset + 2 + 14))
                && (y < (yOffset + 32))) {
            scrollUp();
            bv.refreshDisplayables();
            return true;
        }
        // resize
        if ((x > (width - 17)) && (x < (width - 1)) && (y > (yOffset + 2))
                && (y < (yOffset + 18))) {
            if (isUp()) {
                resizing = true;
                lockOpen = true;
                return true;
            }
        }
        // Above scrollbar
        if ((x > (width - 17)) && (x < (width - 1)) && (y > (yOffset + 31))
                && (y < ((yOffset + 18 + scrollBarOffset) - 1))) {
            pageUp();
            bv.refreshDisplayables();
            return true;
        }
        // Scroll bar
        if ((x > (width - 15)) && (x < (width - 5))
                && (y > ((yOffset + 18 + scrollBarOffset) - 1))
                && (y < (yOffset + 18 + scrollBarOffset + scrollBarHeight))) {
            lastScrollPoint = p;
            scrolling = true;
            return true;
        }
        // Below scrollbar
        if ((x > (width - 17)) && (x < (width - 2))
                && (y > (yOffset + 18 + scrollBarOffset + scrollBarHeight))
                && (y < (yOffset + 18 + getScrollbarOuterHeight()))) {
            pageDown();
            bv.refreshDisplayables();
            return true;
        }
        // Scroll down
        if ((x > (width - 17)) && (x < (width - 1)) && (y > ((size.height) - 25))
                && (y < ((size.height) - 11))) {
            scrollDown();
            bv.refreshDisplayables();
            return true;
        }
        // Message box
        if ((x > 10) && (x < (width - 40)) && (y > ((size.height) - 25))
                && (y < ((size.height) - 11))) {
            bv.refreshDisplayables();
            return true;
        }
        return true;
    }

    /**
     * Draws the chatter box.
     */
    public void draw(Graphics graph, Rectangle clipBounds) {
        graph.setColor(COLOR_BACKGROUND);
        graph.setFont(FONT_CHAT);

        // Draw box.
        int yOffset = ((clipBounds.height) - height - DIST_BOTTOM) + slideOffset + clipBounds.y;
        //graph.fillRoundRect(DIST_SIDE + clipBounds.x, yOffset, width, height, 20, 20);
        graph.fillRect(DIST_SIDE + clipBounds.x, yOffset, width, height);
        graph.setColor(COLOR_TEXT_BACK);

        // Min/max button
        if (slideOffset == getMaxSlideOffset()) {
            graph.drawImage(maxbutton, 10 + clipBounds.x, yOffset + 3, bv);
        } else {
            graph.drawImage(minbutton, 10 + clipBounds.x , yOffset + 3, bv);
        }

        // Title
        printLine(graph, "Incoming messages...", 29 + clipBounds.x, yOffset + 15);

        // resize button
        graph.drawImage(resizebutton, (width - 16) + clipBounds.x, yOffset + 3, bv);

        // Scroll up button
        graph.drawImage(upbutton, (width - 16) + clipBounds.x, yOffset + 16, bv);

        // Scroll bar outer
        graph.drawRect((width - 16) + clipBounds.x, yOffset + 30, 13, getScrollbarOuterHeight());

        // Scroll bar inner
        graph.drawRect((width - 14) + clipBounds.x, yOffset + 31 + scrollBarOffset, 9, scrollBarHeight);

        // Scroll down button
        graph.drawImage(downbutton, (width - 16) + clipBounds.x, (yOffset + height) - 20, bv);

        // Message box
        graph.drawRect(10 + clipBounds.x, (yOffset + height) - 21, width - 50, 17);
        if (message != null && bv.getChatterBoxActive()) {
            printLine(graph, visibleMessage + "_", 13 + clipBounds.x, (yOffset + height) - 7);
        }

        // Text rows
        int rows = messages.size();
        if (rows <= max_nbr_rows) {
            for (int i = 0; i < messages.size(); i++) {
                printLine(graph, messages.elementAt(i), 10 + clipBounds.x, yOffset
                        + 15 + ((i + 1) * 15));
            }
        } else {
            int row = 1;
            for (int i = rows - max_nbr_rows - chatScroll; i < (messages
                    .size()
                    - chatScroll); i++) {
                if (i > -1) {
                    printLine(graph, messages.elementAt(i), 10 + clipBounds.x, yOffset
                            + 15 + (row * 15));
                    row++;
                }
            }
        }
    }

    private void printLine(Graphics graph, String text, int x, int y) {
        graph.drawString(text, x, y + 1);
        graph.drawString(text, x, y - 1);
        graph.drawString(text, x + 1, y);
        graph.drawString(text, x - 1, y);
        graph.setColor(COLOR_TEXT_FRONT);
        graph.drawString(text, x, y);
        graph.setColor(COLOR_TEXT_BACK);
    }

    /**
     *  Adds a line to the chat, and performs line breaking if
     *  necessary
     * @param line
     */
    public void addChatMessage(String line) {
        setIdleTime(0, false);
        if (!isUp()) {
            slideUp();
        }
        int stringWidth = fm.stringWidth(line);
        int lineWidth = width - 5 - 20;

        chatScroll = 0;

        if (stringWidth <= lineWidth) {
            messages.addElement(line);
            computeScrollBarHeight();
            bv.refreshDisplayables();
            return;
        }

        Enumeration<String> words = StringUtil.splitString(line, " ").elements();

        String nextLine = "";
        while (words.hasMoreElements()) {
            String nextWord = words.nextElement();
            if (fm.stringWidth(nextLine + " " + nextWord) < lineWidth) {
                nextLine = (nextLine.equals("")) ? nextWord : nextLine + " "
                        + nextWord;
            } else {
                messages.addElement(nextLine);
                nextLine = nextWord;
            }
        }
        messages.addElement(nextLine);
        computeScrollBarHeight();
        bv.refreshDisplayables();
    }

    /**
     *  Scrolls up one line.
     */
    public void scrollUp() {
        setIdleTime(0, false);
        if (!(chatScroll >= (messages.size() - max_nbr_rows))) {
            chatScroll++;
            computeScrollBarOffset();
        }
    }

    /**
     *  Scrolls down one line.
     */
    public void scrollDown() {
        setIdleTime(0, false);
        if (chatScroll > 0) {
            chatScroll--;
            computeScrollBarOffset();
        }
    }

    /**
     * resizing
     * @param p
     * @param size
     */
    private void resize(Point p, Dimension size) {
        width = p.x;
        height = Math.max(size.height - p.y, 10);
        max_nbr_rows = (height / fm.getHeight()) - 2;
        computeScrollBarHeight();
    }

    /**
     * Scrolling...
     * @param p
     * @param size
     */
    private void scroll(Point p, Dimension size) {
        setIdleTime(0, false);
        int yOffset = (size.height) - height - DIST_BOTTOM;
        int dY;
        if (p.y < (yOffset + 3 + 14 + 14)) {
            if (overTheTop) {
                return;
            } else {
                p = new Point(0, yOffset + 3 + 14 + 14);
                overTheTop = true;
            }
        } else if (p.y > ((yOffset + 150) - 20)) {
            if (underTheBottom) {
                return;
            } else {
                p = new Point(0, (yOffset + 150) - 20);
                underTheBottom = true;
            }
        } else {
            underTheBottom = false;
            overTheTop = false;
        }

        dY = (int) (p.y - lastScrollPoint.getY());
        lastScrollPoint = p;

        scrollBarDragPos += dY;
        scrollBarOffset += dY;

        if (scrollBarOffset < 0) {
            scrollBarOffset = 0;
        } else if (scrollBarOffset > (getMaxScrollbarHeight() - scrollBarHeight)) {
            scrollBarOffset = getMaxScrollbarHeight() - scrollBarHeight;
        }

        while (Math.abs(scrollBarDragPos) >= scrollBarStep) {
            if (Math.abs(scrollBarDragPos) >= ((scrollBarStep) / 2.0f)) {
                if (scrollBarDragPos < 0) {
                    if (!increasedChatScroll
                            && !(chatScroll >= (messages.size() - 6))) {
                        chatScroll++;
                        decreasedChatScroll = false;
                        increasedChatScroll = true;
                    }
                } else {
                    if (!decreasedChatScroll && (chatScroll > 0)) {
                        chatScroll--;
                        decreasedChatScroll = true;
                        increasedChatScroll = false;
                    }
                }
            }
            if (Math.abs(scrollBarDragPos) >= scrollBarStep) {
                if (scrollBarDragPos < 0) {
                    scrollBarDragPos += scrollBarStep;
                } else {
                    scrollBarDragPos -= scrollBarStep;
                }
                decreasedChatScroll = false;
                increasedChatScroll = false;
            }
        }
        if (Math.abs(scrollBarDragPos) >= ((scrollBarStep) / 2.0f)) {
            if (scrollBarDragPos < 0) {
                if (!increasedChatScroll
                        && !(chatScroll >= (messages.size() - 6))) {
                    chatScroll++;
                    decreasedChatScroll = false;
                    increasedChatScroll = true;
                }
            } else {
                if (!decreasedChatScroll && (chatScroll > 0)) {
                    chatScroll--;
                    decreasedChatScroll = true;
                    increasedChatScroll = false;
                }
            }
        }
    }

    private void computeScrollBarOffset() {
        if (messages.size() <= max_nbr_rows) {
            scrollBarOffset = 0;
        } else {
            scrollBarOffset = (int) (((getMaxScrollbarHeight() - scrollBarHeight)) * (1.0f - (chatScroll / ((float) (messages
                    .size() - max_nbr_rows)))));
        }
    }

    private void computeScrollBarHeight() {
        if (messages.size() <= max_nbr_rows) {
            scrollBarHeight = getMaxScrollbarHeight();
            scrollBarStep = 1;
        } else {
            scrollBarHeight = Math
                    .max(3, (int) (((float) max_nbr_rows / messages
                            .size()) * getMaxScrollbarHeight()));
            scrollBarStep = ((getMaxScrollbarHeight() - scrollBarHeight) / (float) (messages
                    .size() - max_nbr_rows));
        }
        computeScrollBarOffset();
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ke) {

        if (!bv.getChatterBoxActive()){
            return;
        }
        
        if (ke.isControlDown() && (ke.getKeyCode() == KeyEvent.VK_V)) {
            Transferable content = Toolkit.getDefaultToolkit().
                    getSystemClipboard().getContents(null);
            boolean hasTransferableText = (content != null) && 
                    content.isDataFlavorSupported(DataFlavor.stringFlavor);
            if (hasTransferableText) {
                try {
                    addChatMessage((String)content.getTransferData(
                            DataFlavor.stringFlavor));
                  }
                  catch (UnsupportedFlavorException ex){
                    //highly unlikely since we are using a standard DataFlavor
                    System.out.println(ex);
                    ex.printStackTrace();
                  }
                  catch (IOException ex) {
                      System.out.println(ex);
                      ex.printStackTrace();
                  }
            }            
            return;
        }
        
        if (ke.isAltDown() || ke.isControlDown()) {
            return;
        }

        ke.consume();
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_UP:
                cb.historyBookmark++;
                cb.fetchHistory();
                bv.repaint();
                return;
            case KeyEvent.VK_DOWN:
                cb.historyBookmark--;
                cb.fetchHistory();
                bv.repaint();
                return;
            case KeyEvent.VK_ALT:
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_TAB:
            case KeyEvent.VK_CAPS_LOCK:
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_PAGE_UP:
            case KeyEvent.VK_PAGE_DOWN:
            case KeyEvent.VK_END:
            case KeyEvent.VK_HOME:
            case KeyEvent.VK_NUM_LOCK:
            case KeyEvent.VK_SCROLL_LOCK:
            case KeyEvent.VK_F1:
            case KeyEvent.VK_F2:
            case KeyEvent.VK_F3:
            case KeyEvent.VK_F4:
            case KeyEvent.VK_F5:
            case KeyEvent.VK_F6:
            case KeyEvent.VK_F7:
            case KeyEvent.VK_F8:
            case KeyEvent.VK_F9:
            case KeyEvent.VK_F10:
            case KeyEvent.VK_F11:
            case KeyEvent.VK_F12:
            case KeyEvent.VK_PRINTSCREEN:
            case KeyEvent.VK_INSERT:
            case KeyEvent.VK_DELETE:
                return;
        }      
        
        if ((isDown() || isSliding()) && (ke.getKeyCode() != KeyEvent.VK_ENTER)
                && (ke.getKeyCode() != KeyEvent.VK_BACK_SPACE)
                && (ke.getKeyCode() != KeyEvent.VK_ESCAPE)) {
            if (!slidingUp) {
                slideUp();
            }
        }

        setIdleTime(0, false);
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                if ((message != null) && (message.length() > 0)) {
                    cb.history.addFirst(message);
                    cb.historyBookmark = -1;

                    if (cb.history.size() > ChatterBox.MAX_HISTORY) {
                        cb.history.removeLast();
                    }
                    client.sendChat(message);
                    clearMessage();
                    cb.setMessage("");
                }
                bv.setChatterBoxActive(false);
                break;
            case KeyEvent.VK_ESCAPE:
                clearMessage();
                bv.setChatterBoxActive(false);
                slideDown();
                break;
            case KeyEvent.VK_BACK_SPACE:
                if ((message == null) || message.equals("")) {
                    return;
                }

                message = message.substring(0, message.length() - 1);
                int i = 0;
                if (fm.stringWidth(message) > (width - 60)) {
                    boolean noFit = true;
                    while (noFit) {
                        i++;
                        String s = message.substring(i);
                        noFit = fm.stringWidth(s) > (width - 60);
                    }
                }
                visibleMessage = message.substring(i);
                cb.setMessage(message);
                break;
            default:
                if (message == null) {
                    message = "" + ke.getKeyChar();
                } else {
                    message += ke.getKeyChar();
                }
                cb.setMessage(message);
                i = 0;
                if (fm.stringWidth(message) > (width - 60)) {
                    boolean noFit = true;
                    while (noFit) {
                        i++;
                        String s = message.substring(i);
                        noFit = fm.stringWidth(s) > (width - 60);
                    }
                }
                visibleMessage = message.substring(i);
        }
        bv.refreshDisplayables();
    }

    public void keyReleased(KeyEvent ke) {
    }

    public void keyTyped(KeyEvent ke) {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        int i = 0;
        if (fm.stringWidth(message) > 240) {
            boolean noFit = true;
            while (noFit) {
                i++;
                String s = message.substring(i);
                noFit = fm.stringWidth(s) > 240;
            }
        }
        visibleMessage = message.substring(i);
    }

    public void setChatterBox(ChatterBox cb) {
        this.cb = cb;
    }

    private int getScrollbarOuterHeight() {
        return height - 49;
    }

    private int getMaxScrollbarHeight() {
        return height - 54;
    }

    private int getMaxSlideOffset() {
        return height - 20;
    }
    
    public void clearMessage(){
        message = "";
        visibleMessage ="";
    }

}
