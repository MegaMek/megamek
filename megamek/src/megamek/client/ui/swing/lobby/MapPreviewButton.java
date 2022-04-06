/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.lobby;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.MapSettings;
import megamek.common.util.ImageUtil;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static megamek.client.ui.swing.lobby.LobbyUtility.cleanBoardName;
import static megamek.client.ui.swing.lobby.LobbyUtility.drawMinimapLabel;
import static megamek.client.ui.swing.util.UIUtil.scaleStringForGUI;

/** A specialized JButton for the map preview panel of the Lobby. */
public class MapPreviewButton extends JButton {
    private static final long serialVersionUID = -80635203255671654L;
    
    private final static Color INDEX_COLOR = new Color(100, 100, 100, 180);
    private Dimension currentPreviewSize;
    private Image scaledImage;
    private Image baseImage;
    private ChatLounge lobby;
    private MapButtonTransferHandler dndHandler;
    private int index;
    private boolean isExample = false;
    private String boardName = "";

    /** A specialized JButton for the map preview panel of the Lobby. */
    public MapPreviewButton(ChatLounge cl, int nr) {
        super("");
        lobby = cl;
        dndHandler = new MapButtonTransferHandler(lobby, this);
        index = nr;
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
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

    /** A specialized JButton for the map preview panel of the Lobby. */
    public MapPreviewButton(ChatLounge cl) {
        this(cl, 0);
    }
    
    /** Sets the size of the button to the given size. */
    public void setPreviewSize(Dimension size) {
        if (!currentPreviewSize.equals(size)) {
            currentPreviewSize = size;
            revalidate();
        }
    }
    
    /** Deletes the scaled minimap image for this button, making it rescale and redraw. */
    public void scheduleRescale() {
        scaledImage = null;
        generateTooltip();
    }
    
    /** Sets the minimap image of the button to the given base image and stores the name for DnD */
    public void setImage(Image image, String name) {
        isExample = name.startsWith(MapSettings.BOARD_SURPRISE) || name.startsWith(MapSettings.BOARD_GENERATED);
        baseImage = image;
        boardName = name;
        setText(name);
        generateTooltip();
        scheduleRescale();
    }
    
    public void reset() {
        baseImage = null;
        boardName = "";
    }
    
    private void generateTooltip() {
        setToolTipText(scaleStringForGUI(lobby.createBoardTooltip(boardName)));
    }
    
    /** Returns true if this button has a base image stored, i.e. if a board file is set for it. */
    public boolean hasBoard() {
        return baseImage != null;
    }
    
    /** Returns the map board index of this button; e.g. 0 for the upper left map. */
    public int getIndex() {
        return index;
    }
    
    public void setIndex(int newIndex) {
        index = newIndex;
    }
    
    public String getBoard() {
        return boardName;
    }

    @Override
    public Dimension getPreferredSize() {
        if (currentPreviewSize.width < 1 || currentPreviewSize.height < 1) {
            setPreviewSize(lobby.maxMapButtonSize());
        }
        return currentPreviewSize;
    }
    
    /** 
     * Scales the present baseImage so that it fits inside the maximum button size
     * allowed by the dimensions of the preview panel while preserving the aspect ratio
     * of the base image. Also, signals the lobby that all preview buttons should be redrawn
     * with the same resulting size regardless of whether they have a board image or not. 
     * Adds the necessary labels to the image as well. 
     */
    private void scaleImage() {
        Dimension optSize = lobby.maxMapButtonSize();
        if (optSize.width > 1 && optSize.height > 1 && baseImage != null) {
            // Scale to the maximum size keeping aspect ratio
            double factorX = (double) optSize.width / baseImage.getWidth(null);
            double factorY = (double) optSize.height / baseImage.getHeight(null);
            double factor = Math.min(factorX, factorY);
            int w = (int) (factor * baseImage.getWidth(null));
            int h = (int) (factor * baseImage.getHeight(null));
            scaledImage = baseImage.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            // Add the labels (index, name, example)
            BufferedImage drawableImage = ImageUtil.createAcceleratedImage(scaledImage);
            Graphics g = drawableImage.getGraphics();
            GUIPreferences.AntiAliasifSet(g);
            if (lobby.isMultipleBoards()) {
                drawIndex(g, w, h);
            }
            if (isExample && lobby.mapSettings.getMedium() != MapSettings.MEDIUM_SPACE) {
                drawExample(g, w, h);
            }
            if (lobby.mapSettings.getMedium() != MapSettings.MEDIUM_SPACE) {
                String text = cleanBoardName(getText(), lobby.mapSettings);
                drawMinimapLabel(text, w, h, g, lobby.hasInvalidBoard(getText()));
            }
            g.dispose();
            // Store the image and notify other buttons to redraw with the calculated size
            scaledImage = drawableImage;
            currentPreviewSize = new Dimension(w, h);
            revalidate();
            lobby.updateMapButtons(currentPreviewSize);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (scaledImage == null) {
            scaleImage();
        }
        if (scaledImage != null) {
            g.drawImage(scaledImage, 0, 0, null);
        }
    }
    
    private void drawIndex(Graphics g, int w, int h) {
        String text = Integer.toString(index + 1);
        int fontSize = Math.min(w, h) / 4;
        fontSize = Math.min(fontSize, UIUtil.scaleForGUI(45));
        g.setFont(new Font("Dialog", Font.PLAIN, fontSize));
        FontMetrics fm = g.getFontMetrics(g.getFont());
        int cx = (w - fm.stringWidth(text)) / 2;
        int cy = (h + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(INDEX_COLOR);
        g.drawString(text, cx, cy);
    }
    
    private void drawExample(Graphics g, int w, int h) {
        String text = "Example board";
        int fontSize = Math.min(w / 10, UIUtil.scaleForGUI(25));
        g.setFont(new Font("Dialog", Font.ITALIC, fontSize));
        FontMetrics fm = g.getFontMetrics(g.getFont());
        int cx = (w - fm.stringWidth(text)) / 2;
        int cy = h / 10 + fm.getAscent();
        g.setColor(Color.BLACK);
        g.drawString(text, cx, cy);
    }

    /** 
     * The TransferHandler manages drag-and-drop for the preview button.
     * The preview buttons can import boards from other preview buttons and from
     * the available bords list. They can also export boards (to other preview buttons). 
     */
    private static class MapButtonTransferHandler extends TransferHandler {
        private static final long serialVersionUID = -1798418800717656572L;

        public final DataFlavor flavor = DataFlavor.stringFlavor;
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
        
        @Override
        protected Transferable createTransferable(JComponent c) {
            // When multiple boards come from the available boards list, they 
            // are just the board names separated by newlines; replicate this for the button
            // by removing the "Surprise" prefix.
            String selection = button.boardName;
            if (selection.startsWith(MapSettings.BOARD_SURPRISE)) {
                selection = selection.substring(MapSettings.BOARD_SURPRISE.length());
            }
            return new StringSelection(selection);
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
                    if ((value instanceof String) && (component instanceof MapPreviewButton)) {
                        lobby.changeMapDnD((String) value, button);
                        return true;
                    } else {
                        return false;
                    }
                } catch (Exception ex) {
                    LogManager.getLogger().error("A problem has occurred with map drag-and-drop.", ex);
                }
            }
            return false;
        }
    }

}
