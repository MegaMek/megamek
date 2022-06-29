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

import megamek.common.alphaStrike.cardDrawer.ASCard;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ASCardPanel extends JComponent {

    private ASCard card;
    private Font cardFont;
    private Image cardImage;
    private AlphaStrikeElement element;

    public ASCardPanel() { }

    public ASCardPanel(@Nullable AlphaStrikeElement element) {
        setASElement(element);
    }

    public void setASElement(@Nullable AlphaStrikeElement element) {
        this.element = element;
        initialize();
        repaint();
    }

    public void setCardFont(Font font) {
        this.cardFont = font;
        initialize();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(cardImage.getWidth(this), cardImage.getHeight(this));
    }

    private void initialize() {
        card = ASCard.createCard(element);
        if (cardFont != null) {
            card.setFont(cardFont);
        }
        cardImage = card.getCardImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(cardImage, 0, 0, this);
    }
}