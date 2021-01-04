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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.border.BevelBorder;

import megamek.MegaMek;

public class MapPreviewButton extends JButton {
    private static final long serialVersionUID = -80635203255671654L;
    
    private Dimension currentPreviewSize;
    private Image scaledImage;
    private Image baseImage;
    private ChatLounge lobby;
    private MapButtonTransferHandler dndHandler;

    public MapPreviewButton(String text, ChatLounge cl) {
        super(text);
        setBorder(new BevelBorder(BevelBorder.RAISED));
        lobby = cl;
        dndHandler = new MapButtonTransferHandler(lobby, this);
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
    }
    
    public boolean hasBoard() {
        return baseImage != null;
    }

    @Override
    public Dimension getPreferredSize() {
        if (currentPreviewSize.width < 1 || currentPreviewSize.height < 1) {
            setPreviewSize(lobby.maxMapButtonSize());
        }
        return currentPreviewSize;
    }
    
    static int count;

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
        } else {
            super.paintComponent(g);
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
            return DnDConstants.ACTION_COPY_OR_MOVE;
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
                    Transferable source = support.getTransferable();
                    Object value = source.getTransferData(flavor);
                    if (value instanceof String) {
                        Component component = support.getComponent();
                        if (component instanceof MapPreviewButton) {
                            lobby.changeMapDnD((String)value, button);
                            return true;
                        }
                    }
                } catch (Exception exp) {
                    MegaMek.getLogger().error("A problem has occurred with map drag-and-drop.");
                    exp.printStackTrace();
                }
            }
            return false;
        }
    }

}
