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

import megamek.client.util.widget.PMUtil;
import megamek.common.util.StringUtil;

import java.awt.*;
import java.awt.event.*;

import java.util.Vector;
import java.util.Enumeration;

// A graphical chatterbox within the boardview.
public class ChatterBox2 
    implements GameListener, KeyListener, Displayable {

    private static final Font   FONT_CHAT = new Font("SansSerif", Font.BOLD, 12);
    private static final Color  COLOR_TEXT_BACK = Color.black;
    private static final Color  COLOR_TEXT_FRONT = Color.white;
    private static final Color  COLOR_BACKGROUND;

    static{
        Color temp;
        try {
            temp = new Color(255, 255, 255, 50);
        }
        catch ( Throwable err ) {
            temp = Color.gray;
        }
        COLOR_BACKGROUND = temp;
    }

    private static final int    SLIDING_SPEED = 5;
    private static final int    MAX_SLIDE_OFFSET = 130;
    private static final int    MIN_SLIDE_OFFSET = 0;

    private static final int    HEIGHT = 150;
    private static final int    WIDTH = 300;
    private static final int    DIST_BOTTOM = 5;
    private static final int    DIST_SIDE = 5;

    private static final int    MAX_NBR_ROWS = 6;
    
    private static final int    SCROLLBAR_MAX_HEIGHT = 110;     
    private static final int    SCROLLBAR_OUTER_HEIGHT = 114;   

    private static final long   MAX_IDLE_TIME = 10000;

    private boolean                 scrolling           = false;
    private boolean                 increasedChatScroll = false;
    private boolean                 decreasedChatScroll = false;
    private boolean                 overTheTop              = false;
    private boolean                 underTheBottom          = false;
    private boolean                 slidingDown         = false;
    private boolean                 slidingUp           = false;
    private boolean                 lockOpen            = false;
    private int                     chatScroll              = 0;
    private int                     messageExcessChars      = 0;
    private int                 scrollBarHeight;
    private int                 scrollBarOffset;
    private int                     slideOffset         = 0;
    private long                idleTime            = 0;
    private float               scrollBarStep;
    private float               scrollBarDragPos;
    private String              message;
    private String              visibleMessage;

    private Point               lastScrollPoint;

    private Vector messages         = new Vector();

    private Client              client;
    private ClientGUI           clientgui;
    
    private Image               upbutton;
    private Image               downbutton;
    private Image               maxbutton;
    private Image               minbutton;

    private FontMetrics             fm;

    public ChatterBox2(ClientGUI clientgui) {
        this.client = clientgui.getClient();
        this.clientgui = clientgui;
        
        fm = clientgui.bv.getFontMetrics(FONT_CHAT);

        Toolkit toolkit = clientgui.bv.getToolkit();
        upbutton = toolkit.getImage("data/widgets/upbutton.gif");
        PMUtil.setImage(upbutton, clientgui);
        downbutton = toolkit.getImage("data/widgets/downbutton.gif");
        PMUtil.setImage(downbutton, clientgui);
        minbutton = toolkit.getImage("data/widgets/minbutton.gif");
        PMUtil.setImage(minbutton, clientgui);
        maxbutton = toolkit.getImage("data/widgets/maxbutton.gif");
        PMUtil.setImage(maxbutton, clientgui);
    }
    
    public boolean isReleased() {
        if (scrolling) {
            stopScrolling();
            return true;
        }
        return false;
    }
    
    public boolean isSliding() {
        return slidingDown || slidingUp;
    }
    
    private void slideUp() {
        setIdleTime(0, false);
        slidingUp = true;
        slidingDown = false;
    }
    
    private void slideDown() {
        setIdleTime(0, false);
        slidingUp = false;
        slidingDown = true;
//      clearMessageBox();
    }
    
    private void stopSliding() {
        setIdleTime(0, false);
        slidingUp = false;
        slidingDown = false;
    }
    
    private boolean isDown() {
        return !isSliding() && slideOffset == MAX_SLIDE_OFFSET;
    }
    
    private boolean isUp() {
        return !isSliding() && slideOffset == MIN_SLIDE_OFFSET;
    }
    
    private void clearMessageBox() {
        message = null;
        visibleMessage = null;
        messageExcessChars = 0;
    }
    
    public synchronized void setIdleTime(long timeIdle, boolean add) {
        if (!lockOpen) {
            if (add) { 
                idleTime += timeIdle;
            } else {
                idleTime = timeIdle;
            }
            
            if (idleTime > MAX_IDLE_TIME && !isSliding()) {
                slideDown();
            }
        }
    }

    private void pageUp() {
        setIdleTime(0, false);
        if (!(chatScroll >= messages.size() - MAX_NBR_ROWS - (MAX_NBR_ROWS - 1))) {
            chatScroll += MAX_NBR_ROWS - 1;
        } else {
            chatScroll = messages.size() - MAX_NBR_ROWS;
        }
        computeScrollBarOffset();
    }
    
    private void pageDown() {
        setIdleTime(0, false);
        if (chatScroll > MAX_NBR_ROWS - 1) {
            chatScroll -= MAX_NBR_ROWS - 1;
        } else {
            chatScroll = 0;
        }
        computeScrollBarOffset();
    }
    
    public boolean slide() {
        if (slidingDown) {
            if (slideOffset < MAX_SLIDE_OFFSET) {
                slideOffset += SLIDING_SPEED;
            } else {
                stopSliding();
            }
            return true;
        } else if (slidingUp) {
            if (slideOffset > MIN_SLIDE_OFFSET) {
                slideOffset -= SLIDING_SPEED;
            } else {
                stopSliding();
            }
            return true;
        }
        return false;
    }
    
    private void stopScrolling() {
        scrollBarDragPos = 0;
        lastScrollPoint = null;
        scrolling = false;
        computeScrollBarOffset();
        clientgui.bv.repaint();
        increasedChatScroll = false;
        decreasedChatScroll = false;
    }

    public boolean isDragged(Point p, Dimension size) {
        if (scrolling) {
            scroll(p, size);
            return true;
        }
        return false;
    }
    
    public boolean isBeingDragged() {
        return scrolling;
    }

    public boolean isMouseOver(Point p, Dimension size) {
        int xMin = DIST_SIDE;
        int xMax = xMin + WIDTH;
        int yMin = ((int) size.height) - HEIGHT - DIST_BOTTOM + slideOffset;
        int yMax = yMin + HEIGHT;
        
        boolean mouseOver = p.x > xMin &&
                            p.x < xMax &&
                            p.y > yMin &&
                            p.y < yMax;
                        
/*      // Don't open on mouse over, it is annoying.
        if (mouseOver && isDown()) {
            slideUp();
        }
*/                          
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

/*      if (message != null) {
            clearMessageBox();
            clientgui.bv.repaint();
        }
*/

        int x = p.x;
        int y = p.y;
        int yOffset = ((int) size.height) - HEIGHT - DIST_BOTTOM + slideOffset;

        if (x < DIST_SIDE ||
            x > DIST_SIDE + WIDTH ||
            y < yOffset ||
            y > yOffset + HEIGHT) {
            return false;
        }
        // Hide button
        if (x > 9 &&
            x < 25 &&
            y > (yOffset + 2) &&
            y < (yOffset + 18)) {

            if (isDown()) {
                slideUp();
            } else {
                slideDown();
            }
            return true;
        }
        // Scroll up
        if (x > 283 &&
            x < 299 &&
            y > (yOffset + 2) &&
            y < (yOffset + 18)) {
            scrollUp();
            clientgui.bv.repaint();
            return true;
        }
        // Above scrollbar
        if (x > 283 &&
            x < 298 &&
            y > (yOffset + 15) &&
            y < (yOffset + 18 + scrollBarOffset - 1)) {
            pageUp();
            clientgui.bv.repaint();
            return true;
        }
        // Scroll bar
        if (x > 285 &&
            x < 295 &&
            y > (yOffset + 18 + scrollBarOffset - 1) &&
            y < (yOffset + 18 + scrollBarOffset + scrollBarHeight)) {
            lastScrollPoint = p;
            scrolling = true;
            return true;
        }
        // Below scrollbar
        if (x > 283 &&
            x < 298 &&
            y > (yOffset + 18 + scrollBarOffset + scrollBarHeight) &&
            y < (yOffset + 18 + SCROLLBAR_OUTER_HEIGHT)) {
            pageDown();
            clientgui.bv.repaint();
            return true;
        }
        // Scroll down
        if (x > 283 &&
            x < 299 &&
            y > (((int) size.height) - 25) &&
            y < (((int) size.height) - 11)) {
            scrollDown();
            clientgui.bv.repaint();
            return true;
        }
        // Message box
        if (x > 10 &&
            x < 260 &&
            y > (((int) size.height) - 25) &&
            y < (((int) size.height) - 11)) {
            if (message == null) {
                message = "";
                visibleMessage = "";
            }
            clientgui.bv.repaint();
            return true;
        }
        return false;
    }
    
    // Draws the chatter box.
    public void draw(Graphics graph, Dimension size) {
        graph.setColor(COLOR_BACKGROUND);
        graph.setFont(FONT_CHAT);

        // Draw box.
        int yOffset = ((int) size.height) - HEIGHT - DIST_BOTTOM + slideOffset;
        graph.fillRoundRect(DIST_SIDE, yOffset, WIDTH, HEIGHT, 20, 20);
        graph.setColor(COLOR_TEXT_BACK);
        
        // Min/max button
        if (slideOffset == 130) {
            graph.drawImage(maxbutton, 10, yOffset + 3, clientgui.bv);
        } else {
            graph.drawImage(minbutton, 10, yOffset + 3, clientgui.bv);
        }
        
        // Title
        printLine(graph, "Incoming messages...", 29, yOffset + 15);

        // Scroll up button
        graph.drawImage(upbutton, 284, yOffset + 3, clientgui.bv);

        // Scroll bar outer
        graph.drawRect(284, yOffset + 16, 13, SCROLLBAR_OUTER_HEIGHT);
        
        // Scroll bar inner
        graph.drawRect(286, yOffset + 18 + scrollBarOffset, 9, scrollBarHeight);

        // Scroll down button
        graph.drawImage(downbutton, 284, yOffset + 150 - 20, clientgui.bv);

        // Message box
        graph.drawRect(10, yOffset + 150 - 21, 250, 17);
        if (message != null) {
            printLine(graph, visibleMessage + "_", 13, yOffset + 150 - 7);
        }
        
        // Text rows
        int rows = messages.size();
        if (rows <= MAX_NBR_ROWS) {
            for (int i = 0; i < messages.size(); i++) {
                printLine(graph, (String) messages.elementAt(i), 10, yOffset + 15 + ((i + 1) * 15));
            }
        } else {
            int row = 1;
            for (int i = rows - (MAX_NBR_ROWS + chatScroll); i < messages.size() - chatScroll; i++) {
                printLine(graph, (String) messages.elementAt(i), 10, yOffset + 15 + (row * 15));
                row++;
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
    
    // Adds a line to the chat, and performs line breaking if 
    // necessary.
    private void addChatMessage(String line) {
        setIdleTime(0, false);
        if (!isUp()) {
            slideUp();
        }
        int stringWidth = fm.stringWidth(line);
        int lineWidth = WIDTH - 5 - 20;

        chatScroll = 0;

        if (stringWidth <= lineWidth) {
            messages.addElement(line);
            computeScrollBarHeight();
            return;
        }

        Enumeration words = StringUtil.splitString(line, " ").elements();

        int nbrLines = stringWidth / lineWidth + 1;

        String nextLine = "";
        while (words.hasMoreElements()) {
            String nextWord = (String) words.nextElement();
            if (fm.stringWidth(nextLine + " " + nextWord) < lineWidth) {
                nextLine = (nextLine.equals("")) ? nextWord : nextLine + " " + nextWord;
            } else {
                messages.addElement(nextLine);
                nextLine = nextWord;
            }
        }
        messages.addElement(nextLine);
        computeScrollBarHeight();
    }

    // Scrolls up one line.
    private void scrollUp() {
        setIdleTime(0, false);
        if (!(chatScroll >= messages.size() - MAX_NBR_ROWS)) {
            chatScroll++;
            computeScrollBarOffset();
        }
    }

    // Scrolls down one line.
    private void scrollDown() {
        setIdleTime(0, false);
        if (chatScroll > 0) {
            chatScroll--;
            computeScrollBarOffset();
        }
    }

    // Scrolling...
    private void scroll(Point p, Dimension size) {
        setIdleTime(0, false);
        int yOffset = ((int) size.height) - HEIGHT - DIST_BOTTOM;
        int dY;
        if (p.y < yOffset + 3 + 14) {
            if (overTheTop) {
                return;
            } else {
                p = new Point(0, yOffset + 3 + 14);
                overTheTop = true;
            }
        } else if (p.y > yOffset + 150 - 20) {
            if (underTheBottom) {
                return;
            } else {
                p = new Point(0, yOffset + 150 - 20);
                underTheBottom = true;
            }
        } else {
            underTheBottom = false;
            overTheTop = false;
        }

        dY = (int) (p.y - lastScrollPoint.y);
        lastScrollPoint = p;

        scrollBarDragPos += dY;
        scrollBarOffset += dY;

        if (scrollBarOffset < 0) {
            scrollBarOffset = 0;
        } else if (scrollBarOffset > (SCROLLBAR_MAX_HEIGHT - scrollBarHeight)) {
            scrollBarOffset = SCROLLBAR_MAX_HEIGHT - scrollBarHeight;
        }

        while (Math.abs(scrollBarDragPos) >= scrollBarStep) {
            if (Math.abs(scrollBarDragPos) >= ((scrollBarStep) / 2.0f)) {
                if (scrollBarDragPos < 0) {
                    if (!increasedChatScroll && !(chatScroll >= messages.size() - 6)) {
                        chatScroll++;
                        decreasedChatScroll = false;
                        increasedChatScroll = true;
                    }
                } else {
                    if (!decreasedChatScroll && chatScroll > 0) {
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
                if (!increasedChatScroll && !(chatScroll >= messages.size() - 6)) {
                    chatScroll++;
                    decreasedChatScroll = false;
                    increasedChatScroll = true;
                }
            } else {
                if (!decreasedChatScroll && chatScroll > 0) {
                    chatScroll--;
                    decreasedChatScroll = true;
                    increasedChatScroll = false;
                }
            }
        }
    }

    private void computeScrollBarOffset() {
        if (messages.size() <= MAX_NBR_ROWS) {
            scrollBarOffset = 0;
        } else {
            scrollBarOffset = (int) (((float)(SCROLLBAR_MAX_HEIGHT - scrollBarHeight)) * (1.0f - ((float) chatScroll / ((float)(messages.size() - MAX_NBR_ROWS)))));
        }
    }

    private void computeScrollBarHeight() {
        if (messages.size() <= MAX_NBR_ROWS) {
            scrollBarHeight = SCROLLBAR_MAX_HEIGHT;
            scrollBarStep = 0;
        } else {
            scrollBarHeight = Math.max(3, (int) (((float)MAX_NBR_ROWS / (float)messages.size()) * (float)SCROLLBAR_MAX_HEIGHT));
            scrollBarStep = ((float)(SCROLLBAR_MAX_HEIGHT - scrollBarHeight) / (float)(messages.size() - MAX_NBR_ROWS));
        }
        computeScrollBarOffset();
    }

    //
    // GameListener
    //
    
    // Update the chat one a message is received.
    public void gamePlayerChat(GameEvent ev) {
        addChatMessage(ev.getMessage());
        if (clientgui.bv.getGraphics() != null) {
            clientgui.bv.repaint();
        }
    }
    public void gamePlayerStatusChange(GameEvent ev) {
    }
    public void gameTurnChange(GameEvent ev) {
    }
    public void gamePhaseChange(GameEvent ev) {
    }
    public void gameNewEntities(GameEvent ev) {
    }
    public void gameNewSettings(GameEvent ev) {
    }
    public void gameReport(GameEvent e) {
        ;
    }
    public void gameEnd(GameEvent e) {
        ;
    }
    public void gameBoardChanged(GameEvent e) {
        ;
    }
    public void gameDisconnected(GameEvent e) {
        ;
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ke) {
        
        if (ke.isControlDown() || ke.isAltDown()) {
            return;
        }
        
        switch(ke.getKeyCode()) {
        case KeyEvent.VK_ALT :
        case KeyEvent.VK_SHIFT :
        case KeyEvent.VK_TAB :
        case KeyEvent.VK_CAPS_LOCK :
        case KeyEvent.VK_CONTROL :
        case KeyEvent.VK_UP :
        case KeyEvent.VK_DOWN :
        case KeyEvent.VK_RIGHT :
        case KeyEvent.VK_LEFT :
        case KeyEvent.VK_PAGE_UP :
        case KeyEvent.VK_PAGE_DOWN :
        case KeyEvent.VK_END :
        case KeyEvent.VK_HOME :
        case KeyEvent.VK_NUM_LOCK :
        case KeyEvent.VK_SCROLL_LOCK :
        case KeyEvent.VK_F1 :
        case KeyEvent.VK_F2 :
        case KeyEvent.VK_F3 :
        case KeyEvent.VK_F4 :
        case KeyEvent.VK_F5 :
        case KeyEvent.VK_F6 :
        case KeyEvent.VK_F7 :
        case KeyEvent.VK_F8 :
        case KeyEvent.VK_F9 :
        case KeyEvent.VK_F10 :
        case KeyEvent.VK_F11 :
        case KeyEvent.VK_F12 :
        case KeyEvent.VK_PRINTSCREEN :
        case KeyEvent.VK_INSERT :
        case KeyEvent.VK_DELETE :
            return;
        }

        if ((isDown() || isSliding()) && 
            ke.getKeyCode() != KeyEvent.VK_ENTER && 
            ke.getKeyCode() != KeyEvent.VK_BACK_SPACE && 
            ke.getKeyCode() != KeyEvent.VK_ESCAPE) {
            if (!slidingUp) {
                slideUp();
            }
        }
        
        setIdleTime(0, false);
        switch(ke.getKeyCode()) {
        case KeyEvent.VK_ENTER :
            if (message != null && message.length() > 0) {
                client.sendChat(message);
                visibleMessage = "";
                message = "";
                messageExcessChars = 0;
            }
            break;
        case KeyEvent.VK_ESCAPE :
            clearMessageBox();
            break;
        case KeyEvent.VK_BACK_SPACE:
            if (message == null || message.equals("")) {
                return;
            }
        
            message = message.substring(0, message.length() - 1);
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
        
            break;
        default :
            if (message == null) {
                message = "" + ke.getKeyChar();
            } else {
                message += ke.getKeyChar();
            }
        
            i = 0;
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
    }
    
    public void keyReleased(KeyEvent ke) {
    }

    public void keyTyped(KeyEvent ke) {
    }
    
    public void gameMapQuery(GameEvent e) {
        ;
    }


}
