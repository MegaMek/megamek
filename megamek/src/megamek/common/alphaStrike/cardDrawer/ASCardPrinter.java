/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.alphaStrike.cardDrawer;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.codeUtilities.StringUtility;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.loaders.MekSummary;

/**
 * This class prints a collection of one or more Alpha Strike cards. The cards to be printed can be created from either
 * an {@link megamek.common.alphaStrike.AlphaStrikeElement} or a {@link MekSummary}. It shows a progress bar dialog but
 * the printing happens in the background and the calling window is not blocked.
 */
public class ASCardPrinter implements Printable {

    private final JFrame parent;
    private final List<CardSlot> cardSlots = new ArrayList<>();
    private ProgressPopup progressPopup;
    private AffineTransform baseTransform;

    // The column and row count depend on the page format of a given print job and are set anew for each print call
    private int columnCount = 2;
    private int rowCount = 4;

    /**
     * Creates a new ASCardPrinter object for the given ASCardDisplayable elements either
     * {@link megamek.common.alphaStrike.AlphaStrikeElement} or {@link MekSummary}. The parent is used for the progress
     * dialog. Print the cards by calling {@link #printCards()}.
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
     * Starts a printing process for the cards of this ASCardPrinter. This will display the usual printer selection
     * dialog and a progress dialog. Printing itself happens in the background and the progress dialog can be closed.
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
        protected Void doInBackground() {
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

    /** Sets the translation in the given g2D to the given card slot, going down the first column, then the second... */
    private void goToPrintSlot(int slot, Graphics2D g2D) {
        g2D.setTransform(baseTransform);
        int column = slot / rowCount;
        int row = slot - column * rowCount;
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
