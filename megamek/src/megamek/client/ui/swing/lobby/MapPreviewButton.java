/*  
* MegaMek - Copyright (C) 2021 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/
package megamek.client.ui.swing.lobby;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import megamek.MegaMek;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;

/** A specialized JButton for the map preview panel of the Lobby. */
public class MapPreviewButton extends JButton {
    private static final long serialVersionUID = -80635203255671654L;
    
    private final static Color INDEX_COLOR = new Color(200, 200, 200, 190);
    private Dimension currentPreviewSize;
    private Image scaledImage;
    private Image baseImage;
    private ChatLounge lobby;
    private MapButtonTransferHandler dndHandler;
    private int index;

    public MapPreviewButton(String text, ChatLounge cl, int nr) {
        super(text);
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        lobby = cl;
        dndHandler = new MapButtonTransferHandler(lobby, this);
        index = nr;
        setTransferHandler(dndHandler);
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                JButton button = (JButton) e.getSource();
                TransferHandler handle = button.getTransferHandler();
                handle.exportAsDrag(button, e, TransferHandler.COPY);
            }
        });
        currentPreviewSize = lobby.maxMapButtonSize();
    }

    public void setPreviewSize(Dimension size) {
        if (!currentPreviewSize.equals(size)) {
            currentPreviewSize = size;
            revalidate();
        }
    }
    
    public void scheduleRescale() {
        scaledImage = null;
    }
    
    public void setImage(Image image, String name) {
        baseImage = image;
        dndHandler.setBoardName(name);
        setText(name);
    }
    
    public boolean hasBoard() {
        return baseImage != null;
    }
    
    public int getIndex() {
        return index;
    }

    @Override
    public Dimension getPreferredSize() {
        if (currentPreviewSize.width < 1 || currentPreviewSize.height < 1) {
            setPreviewSize(lobby.maxMapButtonSize());
        }
        return currentPreviewSize;
    }
    
    private void scaleImage() {
        Dimension optSize = lobby.maxMapButtonSize();
        if (optSize.width > 1 && optSize.height > 1 && baseImage != null) {
            double factorX = (double)optSize.width / baseImage.getWidth(null);                    
            double factorY = (double)optSize.height / baseImage.getHeight(null);
            double factor = Math.min(factorX, factorY);
            int w = (int)(factor * baseImage.getWidth(null));
            int h = (int)(factor * baseImage.getHeight(null));
            scaledImage = baseImage.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            currentPreviewSize = new Dimension(w, h);
            revalidate();
            lobby.updateMapButtons(new Dimension(w, h));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (scaledImage == null) {
            scaleImage();
        }
        if (scaledImage != null) {
            g2.drawImage(scaledImage, 0, 0, null);
        }
        // Write the index of the button as centered text
        String text = Integer.toString(index);
        int posx = getWidth()/2;
        int posy = getHeight()/2;
        int size = Math.min(posx, posy);
        GUIPreferences.AntiAliasifSet(g2);
        g2.setFont(new Font("Dialog", Font.PLAIN, size));
        FontMetrics fm = g2.getFontMetrics(g2.getFont());
        // Center the text around pos
        int cx = posx - (fm.stringWidth(text) / 2);
        int cy = posy - fm.getAscent()/2-fm.getDescent() / 2+fm.getAscent();

        g2.setColor(new Color(100, 100, 100, 180));
        g2.drawString(text, cx, cy);
        
        // Write the special map text
        if (LobbyUtility.isBoardFile(getText())) {
            text = new File(getText()).getName();
        } else {
            text = getText();
        }
        if (text.length() > 0) {
            posy = getHeight();
            size = Math.min(getWidth(), getHeight())/Math.max(10, text.length()/2);
            GUIPreferences.AntiAliasifSet(g2);
            g2.setFont(new Font("Dialog", Font.PLAIN, size));
            fm = g2.getFontMetrics(g2.getFont());
            cx = posx - (fm.stringWidth(text) / 2);
            cy = posy - fm.getAscent() + fm.getDescent(); 

            g2.setColor(Color.BLACK);
            g2.drawString(text, cx, cy);
        }

        if (hasFocus()) {
            g2.setColor(Color.GREEN);
            g2.drawRect(2, 2, getWidth()-6, getHeight()-6);
        }
        g2.dispose();
    }

    private static class MapButtonTransferHandler extends TransferHandler {
        private static final long serialVersionUID = -1798418800717656572L;

        public final DataFlavor flavor = DataFlavor.stringFlavor;
        private String boardName;
        private MapPreviewButton button;
        private ChatLounge lobby;

        public MapButtonTransferHandler(ChatLounge cl, MapPreviewButton mpButton) {
            lobby = cl;
            button = mpButton;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return DnDConstants.ACTION_COPY;
        }
        
        protected void setBoardName(String name) {
            boardName = name;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection(boardName);
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            return support.isDataFlavorSupported(flavor);
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if (canImport(support)) {
                try {
                    Component component = support.getComponent();
                    Object value = support.getTransferable().getTransferData(flavor);
                    if (!(value instanceof String) || !(component instanceof MapPreviewButton)) {
                        return false;
                    }
                    System.out.println(support.getDropAction());
                    String boardString = (String)value;
                    String[] boards = boardString.split("\n");
                    int rnd = (int)(Math.random() * boards.length);
                    lobby.changeMapDnD(boards[rnd], button);
                    return true;
                } catch (Exception exp) {
                    MegaMek.getLogger().error("A problem has occurred with map drag-and-drop.");
                    exp.printStackTrace();
                }
            }
            return false;
        }
    }

}
