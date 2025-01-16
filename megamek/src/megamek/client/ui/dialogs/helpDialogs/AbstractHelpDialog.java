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
package megamek.client.ui.dialogs.helpDialogs;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.logging.MMLogger;

/**
 * This class ensures that every Help dialog in MegaMek has an identical
 * look-and-feel.
 */
public abstract class AbstractHelpDialog extends AbstractDialog {
    private final static MMLogger logger = MMLogger.create(AbstractHelpDialog.class);

    // region Variable Declarations
    private String helpFilePath;

    // endregion Variable Declarations

    // region Constructors
    protected AbstractHelpDialog(final JFrame frame, final String name, final String helpFilePath) {
        super(frame, name, "AbstractHelpDialog.helpFile");
        setHelpFilePath(helpFilePath);
        initialize();
    }
    // endregion Constructors

    // region Getters/Setters
    public String getHelpFilePath() {
        return helpFilePath;
    }

    public void setHelpFilePath(final String helpFilePath) {
        this.helpFilePath = helpFilePath;
    }
    // endregion Getters/Setters

    @Override
    protected Container createCenterPane() {
        var pane = new JEditorPane();
        var scrollPane = new JScrollPane(pane);

        pane.setContentType("text/html");
        pane.setName("helpPane");
        pane.setEditable(false);
        pane.addHyperlinkListener(pe -> {
            if (HyperlinkEvent.EventType.ACTIVATED == pe.getEventType()) {
                String reference = pe.getDescription();
                if (reference != null && reference.startsWith("#")) {
                    reference = reference.substring(1);
                    String finalReference = reference;
                    SwingUtilities.invokeLater(() -> pane.scrollToReference(finalReference));
                }
            }
        });


        // Add mouse motion listener to show tooltips for links.
        pane.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int pos = pane.viewToModel2D(e.getPoint());
                if (pos >= 0 && pane.getDocument() instanceof HTMLDocument doc) {
                    var elem = doc.getCharacterElement(pos);
                    if (elem != null) {
                        // The Element’s attributes may point us to a <SPAN> tag
                        var attrs = elem.getAttributes();
                        Object attrsAttribute = attrs.getAttribute(HTML.Tag.A);

                        if (attrsAttribute instanceof AttributeSet nAttrs) {
                            // Try retrieving your custom data-value attribute.
                            // "data-value" isn’t part of the standard HTML.Attribute enum,
                            // so we can use HTML.getAttributeKey("data-value").
                            String dataValue = (String) nAttrs.getAttribute(HTML.getAttributeKey("data-value"));

                            if (dataValue != null) {
                                // We found our custom attribute, so show it in the tooltip
                                pane.setToolTipText(dataValue);
                            }
                        }
                    }
                }
            }
        });
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        final File helpFile = new File(getHelpFilePath());

        // Get the help content file if possible
        try {
            setTitle(getTitle() + helpFile.getName());
            pane.setPage(helpFile.toURI().toURL());
        } catch (Exception e) {
            setTitle(Messages.getString("AbstractHelpDialog.noHelp.title"));
            pane.setText(Messages.getString("AbstractHelpDialog.errorReading") + e.getMessage());
            logger.error(e, "createCenterPane");
        }

        return scrollPane;
    }
}
