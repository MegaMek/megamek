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

import megamek.client.ui.dialogs.ASConversionInfoDialog;
import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * This is a JPanel that displays an AlphaStrike unit card and elements to configure the display of
 * the card and allow copying to clipboard. The AlphaStrike element to display can be changed after
 * construction and it may be null.
 */
public class ConfigurableASCardPanel extends JPanel {

    private final JComboBox<String> fontChooser = new JComboBox<>();
    private final JComboBox<Float> sizeChooser = new JComboBox<>();
    private final JButton copyButton = new JButton("Copy to Clipboard");
    private final JButton reportButton = new JButton("Show Conversion Report");
    private final ASCardPanel cardPanel = new ASCardPanel();
    private CalculationReport report;
    private final JFrame parentFrame;

    /**
     * Constructs a panel with the given AlphaStrike element to display.
     *
     * @param element The AlphaStrike element to display
     */
    public ConfigurableASCardPanel(@Nullable AlphaStrikeElement element, JFrame frame) {
        parentFrame = frame;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            fontChooser.addItem(family);
        }
        fontChooser.addActionListener(ev -> updateFont());

        sizeChooser.addItem(2f);
        sizeChooser.addItem(1.5f);
        sizeChooser.addItem(1f);
        sizeChooser.addItem(0.75f);
        sizeChooser.addItem(0.5f);
        sizeChooser.addItem(0.33f);
        sizeChooser.setSelectedItem(0.75f);
        sizeChooser.addActionListener(ev -> updateSize());
        sizeChooser.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(Float.toString(value)));

        copyButton.addActionListener(ev -> copyCardToClipboard());
        reportButton.addActionListener(e -> showConversionReport());

        var chooserLine = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        chooserLine.setBorder(new EmptyBorder(10, 0, 10, 0));
        chooserLine.add(Box.createHorizontalStrut(15));
        chooserLine.add(new JLabel("Font: "));
        chooserLine.add(fontChooser);
        chooserLine.add(Box.createHorizontalStrut(25));
        chooserLine.add(new JLabel("Card Size: "));
        chooserLine.add(sizeChooser);
        chooserLine.add(Box.createHorizontalStrut(25));
        chooserLine.add(copyButton);
        chooserLine.add(Box.createHorizontalStrut(25));
        chooserLine.add(reportButton);

        var cardLine = new JScrollPane(cardPanel);
        cardPanel.setBorder(new EmptyBorder(5, 65, 0, 0));
        cardLine.getVerticalScrollBar().setUnitIncrement(16);

        add(chooserLine);
        add(cardLine);
        updateSize();
        setASElement(element);
    }

    /** Construct a new panel without an AlphaStrike element to display. */
    public ConfigurableASCardPanel(JFrame frame) {
        this(null, frame);
    }

    /**
     * Set the panel to display the given element.
     *
     * @param element The AlphaStrike element to display
     */
    public void setASElement(@Nullable AlphaStrikeElement element) {
        report = (element != null) ? element.getConversionReport() : null;
        cardPanel.setASElement(element);
    }

    /** Set the card to use a newly selected font. */
    private void updateFont() {
        Font font = Font.decode((String) fontChooser.getSelectedItem());
        cardPanel.setCardFont(font);
    }

    /** Set the card to use a newly selected font. */
    private void showConversionReport() {
        if (report != null) {
            new ASConversionInfoDialog(parentFrame, report, null, true).setVisible(true);
        }
    }

    /** Set the card to use a newly selected scale. */
    private void updateSize() {
        if (sizeChooser.getSelectedItem() != null) {
            cardPanel.setScale((Float) sizeChooser.getSelectedItem());
            invalidate();
        }
    }

    // Taken from https://alvinalexander.com/java/java-copy-image-to-clipboard-example/
    private void copyCardToClipboard() {
        ImageSelection imgSel = new ImageSelection(cardPanel.getCardImage());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
    }

    static class ImageSelection implements Transferable {

        private final Image image;

        public ImageSelection(Image image) {
            this.image = image;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}