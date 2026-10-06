/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.panels.battlefieldSupport;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.cardDrawer.BattlefieldSupportCard;
import megamek.common.battlefieldSupport.cardDrawer.BattlefieldSupportCard.ColorMode;

/**
 * A JComponent that displays a single Battlefield Support (BFS) Asset card. It supports setting a font, scale and color
 * mode for the card. In {@link #setFitToPanel fit-to-panel} mode it instead scales the card to fill its own bounds
 * (preserving aspect ratio). Right-clicking the card shows a popup menu offering to copy the card image to the
 * clipboard (always at the card's natural 1:1 scale), mirroring the record-sheet preview pane. The asset to display can
 * be changed after construction and may be {@code null}, in which case a blank placeholder card of the correct size is
 * shown.
 */
public final class BFSCardPanel extends JComponent {

    private BattlefieldSupportAsset asset;
    private float scale = 1;
    private boolean fitToPanel;
    private Font cardFont;
    private ColorMode colorMode = ColorMode.NONE;
    private Color damageColor;
    private Image cardImage;

    public BFSCardPanel() {
        addMouseListener(new PopupListener());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (fitToPanel) {
                    repaint();
                }
            }
        });
        initialize();
    }

    /**
     * Sets the panel to display the given asset.
     *
     * @param asset the Battlefield Support Asset to display, or {@code null}
     */
    public void setAsset(@Nullable BattlefieldSupportAsset asset) {
        this.asset = asset;
        initialize();
    }

    /**
     * Sets the card to use the given font for its text.
     *
     * @param font the font to use for the card contents
     */
    public void setCardFont(Font font) {
        this.cardFont = font;
        initialize();
    }

    /**
     * Sets the card scale. Values smaller than 1 result in a smaller card; larger than 1 in a bigger one. At scale 1
     * the card is the standard 1050 x 750. Ignored while {@link #setFitToPanel fit-to-panel} mode is on.
     *
     * @param scale the scale to draw the card at
     */
    public void setScale(float scale) {
        this.scale = scale;
        initialize();
    }

    /**
     * Enables or disables fit-to-panel mode. When enabled, the card is always rendered at its natural 1:1 scale and
     * drawn scaled to fill the component's bounds (preserving aspect ratio, centered), so the preview grows and shrinks
     * with the panel. When disabled (the default), the card is drawn at the fixed {@link #setScale scale}.
     *
     * @param fitToPanel whether to scale the card to the panel size
     */
    public void setFitToPanel(boolean fitToPanel) {
        this.fitToPanel = fitToPanel;
        initialize();
    }

    /**
     * Sets the card color mode, controlling whether the BATTLETECH logo and accents are colored.
     *
     * @param colorMode the color mode to draw the card with
     */
    public void setColorMode(ColorMode colorMode) {
        this.colorMode = (colorMode != null) ? colorMode : ColorMode.NONE;
        initialize();
    }

    /**
     * Sets the color used for the current Destroy Check of a damaged asset in a color mode (see
     * {@link BattlefieldSupportCard#setDamageColor}). {@code null} restores the default red.
     *
     * @param damageColor the damage color, or {@code null} for the default
     */
    public void setDamageColor(@Nullable Color damageColor) {
        this.damageColor = damageColor;
        initialize();
    }

    /** @return the card image at natural 1:1 scale in fit-to-panel mode, otherwise at the configured scale */
    public Image getCardImage() {
        return cardImage;
    }

    private void initialize() {
        BattlefieldSupportCard card = new BattlefieldSupportCard(asset);
        if (cardFont != null) {
            card.setFont(cardFont);
        }
        card.setColorMode(colorMode);
        card.setDamageColor(damageColor);
        // In fit mode render at natural 1:1 (scaled to the panel at paint time) so copies are always full resolution.
        cardImage = card.getCardImage(fitToPanel ? 1f : scale);
        revalidate();
        repaint();
    }

    private void copyCardToClipboard() {
        if (cardImage != null) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new CardImageTransferable(cardImage), null);
        }
    }

    /** Shows a copy-to-clipboard popup on right-click, mirroring the record-sheet preview pane. */
    private class PopupListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (!e.isPopupTrigger() || (asset == null)) {
                return;
            }
            JPopupMenu popup = new JPopupMenu();
            JMenuItem copyItem = new JMenuItem(Messages.getString("CASCardPanel.copyCard"));
            copyItem.addActionListener(l -> copyCardToClipboard());
            popup.add(copyItem);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /** A minimal {@link Transferable} that puts a single image onto the clipboard. */
    private record CardImageTransferable(Image image) implements Transferable {

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (fitToPanel) {
            // Small default so the enclosing split/layout can freely size us; actual size comes from the layout.
            return new Dimension(BattlefieldSupportCard.WIDTH / 4, BattlefieldSupportCard.HEIGHT / 4);
        }
        return new Dimension(cardImage.getWidth(this), cardImage.getHeight(this));
    }

    @Override
    public Dimension getMinimumSize() {
        return fitToPanel ? new Dimension(50, 36) : super.getMinimumSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!fitToPanel) {
            g.drawImage(cardImage, 0, 0, this);
            return;
        }
        int imgW = cardImage.getWidth(this);
        int imgH = cardImage.getHeight(this);
        if ((imgW <= 0) || (imgH <= 0)) {
            return;
        }
        double fit = Math.min((double) getWidth() / imgW, (double) getHeight() / imgH);
        int w = (int) Math.round(imgW * fit);
        int h = (int) Math.round(imgH * fit);
        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(cardImage, x, y, w, h, this);
        g2.dispose();
    }
}
