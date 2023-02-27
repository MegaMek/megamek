/*
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.alphaStrike.cardDrawer;

import megamek.MMConstants;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.codeUtilities.StringUtility;
import megamek.common.alphaStrike.ASCardDisplayable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class prints a collection of one or more Alpha Strike cards. The cards to be printed can be created
 * from either an {@link megamek.common.alphaStrike.AlphaStrikeElement} or a {@link megamek.common.MechSummary}.
 * It shows a progress bar dialog but the printing happens in the background and the calling window is not blocked.
 */
public class ASCardPrinter implements Printable {

    private final JFrame parent;
    private final List<CardSlot> cardSlots = new ArrayList<>();
    private ProgressPopup progressPopup;
    private int row;
    private int column;
    private AffineTransform baseTransform;

    // The column and row count depend on the page format of a given print job and are set anew for each print call
    private int columnCount = 2;
    private int rowCount = 4;

    /**
     * Creates a new ASCardPrinter object for the given ASCardDisplayable elements (either
     * {@link megamek.common.alphaStrike.AlphaStrikeElement} or {@link megamek.common.MechSummary}.
     * The parent is used for the progress dialog. Print the cards by calling {@link #printCards()}.
     */
    public ASCardPrinter(Collection<? extends ASCardDisplayable> elements, JFrame parent) {
        Font userSelectedFont = userSelectedFont();
        this.parent = parent;
        for (ASCardDisplayable element : elements) {
            ASCard card = ASCard.createCard(element);
            card.setFont(userSelectedFont);
            cardSlots.add(new CardSlot(card, false));
            if (element.usesArcs()) {
                cardSlots.add(new CardSlot(card, true));
            }
        }
    }

    /**
     * Starts a printing process for the carsd of this ASCardPrinter. This will display the usual printer
     * selection dialog and a progress dialog. Printing itself happens in the background and the progress
     * dialog can be closed.
     */
    public void printCards() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        boolean doPrint = job.printDialog();
        if (doPrint) {
            progressPopup = new ProgressPopup(cardSlots.size(), parent);
            progressPopup.setVisible(true);
            new PrintCardTask(job).execute();
        }
    }

    private class PrintCardTask extends SwingWorker<Void, Integer> {

        private final PrinterJob job;

        PrintCardTask(PrinterJob job) {
            this.job = job;
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(parent, ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
            return null;
        }

        @Override
        protected void done() {
            progressPopup.setVisible(false);
        }
    }

    private static class ProgressPopup extends JDialog {
        private final JProgressBar progressBar = new JProgressBar();

        ProgressPopup(int maximum, JFrame parent) {
            super(parent, "Print Cards");
            progressBar.setMaximum(maximum);
            progressBar.setStringPainted(true);
            progressBar.setBorder(new EmptyBorder(20, 40, 20, 40));
            getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
            add(Box.createVerticalStrut(20));
            add(new JLabel("Printing Alpha Strike Cards...", JLabel.CENTER));
            add(progressBar);

            UIUtil.adjustDialog(ProgressPopup.this, UIUtil.FONT_SCALE1);
            setLocationRelativeTo(null);
            pack();
        }
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
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

            final int doneCards = elementIndex;
            SwingUtilities.invokeLater(() ->
            progressPopup.progressBar.setValue(doneCards));
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

    /** Sets the translate in the given g2D to the given card slot, going down the first column, then the second... */
    private void goToPrintSlot(int slot, Graphics2D g2D) {
        g2D.setTransform(baseTransform);
        column = slot / rowCount;
        row = slot - column * rowCount;
        g2D.translate(-ASCard.WIDTH * columnCount / 2 + ASCard.WIDTH * column, ASCard.HEIGHT * row);
    }

    /** Holds the card for a card slot on the page together with the info if this is the front or back side. */
    private static class CardSlot {
        ASCard card;
        boolean flipSide;

        CardSlot(ASCard card, boolean flipSide) {
            this.card = card;
            this.flipSide = flipSide;
        }
    }
}