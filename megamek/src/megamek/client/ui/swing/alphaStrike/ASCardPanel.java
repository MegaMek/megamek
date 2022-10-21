/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.alphaStrike;

import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.cardDrawer.ASCard;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * This is a JComponent that displays only an AlphaStrike unit card. It supports setting a font and scale
 * for the card to use. The AlphaStrike element to display can be changed after construction and it may be
 * null.
 */
public final class ASCardPanel extends JComponent {

    private ASCard card;
    private float scale = 1;
    private Font cardFont;
    private Image cardImage;
    private ASCardDisplayable element;

    /**
     * Construct a card panel without an AlphaStrike element to display. The created card will show
     * that there is no element to display but will still be a normal card.
     */
    public ASCardPanel() { }

    /**
     * Construct a card panel with the given AlphaStrike element to display.
     *
     * @param element The AlphaStrike element to display
     */
    public ASCardPanel(@Nullable ASCardDisplayable element) {
        setASElement(element);
    }

    /**
     * Set the panel to display the given element.
     *
     * @param element The AlphaStrike element to display
     */
    public void setASElement(@Nullable ASCardDisplayable element) {
        this.element = element;
        initialize();
    }

    /**
     * Set the card to use the given Font.
     *
     * @param font The Font to use for writing the contents of the card.
     */
    public void setCardFont(Font font) {
        this.cardFont = font;
        initialize();
    }

    /**
     * Set the card to be scaled with the given scale. Values smaller than 1 result in a smaller card
     * while values larger than 1 in a greater than normal card. At scale = 1, the card will be the
     * standard size of 1050 x 750 (1050 x 1500 for Large Aerospace in portrait arrangement).
     *
     * @param scale The scale to use for drawing the card
     */
    public void setScale(float scale) {
        this.scale = scale;
        initialize();
    }

    /**
     * Returns the image of the card. If a scale other than 1 has been set before, the image will
     * be sized accordingly.
     *
     * @return The (scaled) card image.
     */
    public Image getCardImage() {
        return cardImage;
    }

    /** Create the card, add settings and draw it to the card image cache. */
    private void initialize() {
        card = ASCard.createCard(element);
        if (cardFont != null) {
            card.setFont(cardFont);
        }
        cardImage = card.getCardImage(scale);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(cardImage.getWidth(this), cardImage.getHeight(this));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(cardImage, 0, 0, this);
    }
}