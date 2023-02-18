/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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

import megamek.MMConstants;
import megamek.client.ui.swing.GUIPreferences;
import megamek.codeUtilities.StringUtility;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.cardDrawer.ASCard;
import megamek.common.alphaStrike.cardDrawer.ASLargeAeroCard;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ASElementPrinter {

    private final List<CardSlot> cardSlots = new ArrayList<>();
    private int row;
    private int column;
    private AffineTransform baseTransform;
    private int columnCount = 2;
    private int rowCount = 4;

    public ASElementPrinter(Collection<? extends ASCardDisplayable> elements) {
        Font userSelectedFont = userSelectedFont();
        for (ASCardDisplayable element : elements) {
            ASCard card = ASCard.createCard(element);
            card.setFont(userSelectedFont);
            cardSlots.add(new CardSlot(card, false));
            if (element.usesArcs()) {
                cardSlots.add(new CardSlot(card, true));
            }
        }
    }

    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
            throws PrinterException {
        double height = 2.5 * 72;
        rowCount = (int) (pageFormat.getImageableHeight() / height);
        columnCount = (int) (pageFormat.getImageableWidth() / (3.5 * 72));
        if (isPageIndexValid(pageIndex)) {
            double fullHeight = rowCount * height;
            Graphics2D g2D = (Graphics2D) graphics;
            double topCardY = pageFormat.getHeight() / 2 - fullHeight / 2;
            g2D.translate(pageFormat.getWidth() / 2, topCardY);
            g2D.scale(ASCard.PRINT_SCALE, ASCard.PRINT_SCALE);
            baseTransform = g2D.getTransform();

            int elementIndex = pageStartSlotIndex(pageIndex);
            while ((elementIndex < pageStartSlotIndex(pageIndex + 1)) && (elementIndex < cardSlots.size())) {
                goToPrintSlot(elementIndex - pageStartSlotIndex(pageIndex), g2D);
                CardSlot cardSlot = cardSlots.get(elementIndex);
                if (cardSlot.flipSide) {
                    ((ASLargeAeroCard) (cardSlot.card)).drawFlipside(g2D);
                } else {
                    cardSlot.card.drawCard(g2D);
                }
                elementIndex++;
            }

            return Printable.PAGE_EXISTS;
        } else {
            return Printable.NO_SUCH_PAGE;
        }
    }

    private Font userSelectedFont() {
        String fontName = GUIPreferences.getInstance().getAsCardFont();
        return (StringUtility.isNullOrBlank(fontName)
                ? new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 14)
                : Font.decode(fontName));
    }

    private int pageStartSlotIndex(int pageIndex) {
        return columnCount * rowCount * pageIndex;
    }

    private boolean isPageIndexValid(int pageIndex) {
        return cardSlots.size() > pageStartSlotIndex(pageIndex);
    }

    private void goToPrintSlot(int slot, Graphics2D g2D) {
        g2D.setTransform(baseTransform);
        column = slot / rowCount;
        row = slot - column * rowCount;
        g2D.translate(-ASCard.WIDTH * columnCount / 2 + ASCard.WIDTH * column, ASCard.HEIGHT * row);
    }

    private static class CardSlot {
        ASCard card;
        boolean flipSide;

        CardSlot(ASCard card, boolean flipSide) {
            this.card = card;
            this.flipSide = flipSide;
        }
    }
}