/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitSelectorDialogs;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Collection;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import megamek.client.ui.entityreadout.EntityReadout;
import megamek.client.ui.entityreadout.ReadoutSections;
import megamek.client.ui.util.FluffImageHelper;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.FixedXPanel;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.Report;
import megamek.common.preference.PreferenceManager;
import megamek.common.templates.TROView;
import megamek.common.units.Entity;

/**
 * @author Jay Lawson
 * @since November 2, 2009
 */
public class EntityReadoutPanel extends JPanel {

    private final JTextPane readoutTextComponent = new JTextPane();
    private final JLabel fluffImageComponent = new JLabel();
    private final JScrollPane scrollPane = new JScrollPane(readoutTextComponent);

    public static final int DEFAULT_WIDTH = 360;

    public EntityReadoutPanel() {
        this(-1, -1);
    }

    public EntityReadoutPanel(int width, int height) {
        Report.setupStylesheet(readoutTextComponent);
        readoutTextComponent.setEditable(false);
        readoutTextComponent.setBorder(new EmptyBorder(5, 10, 0, 0));
        if (width != -1) {
            readoutTextComponent.setMinimumSize(new Dimension(width, height));
            readoutTextComponent.setPreferredSize(new Dimension(width, height));
        }

        readoutTextComponent.addHyperlinkListener(pe -> {
            if (HyperlinkEvent.EventType.ACTIVATED == pe.getEventType()) {

                boolean isHttpAddress = pe.getURL().toString().startsWith("http");
                if (isHttpAddress) {
                    UIUtil.browse(pe.getURL().toString(), this);
                } else {
                    String reference = pe.getDescription();
                    if (reference != null && reference.startsWith("#")) {
                        reference = reference.substring(1);
                        String finalReference = reference;
                        SwingUtilities.invokeLater(() -> readoutTextComponent.scrollToReference(finalReference));
                    }
                }
            }
        });

        // Add mouse motion listener to show tooltips for links.
        readoutTextComponent.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int pos = readoutTextComponent.viewToModel2D(e.getPoint());
                if (pos >= 0 && readoutTextComponent.getDocument() instanceof HTMLDocument doc) {
                    var elem = doc.getCharacterElement(pos);
                    if (elem != null) {
                        // The Element’s attributes may point us to a <SPAN> tag
                        var attrs = elem.getAttributes();
                        Object attrsAttribute = attrs.getAttribute(HTML.Tag.SPAN);

                        if (attrsAttribute instanceof AttributeSet attributeSet) {
                            String title = (String) attributeSet.getAttribute(HTML.getAttributeKey("title"));
                            readoutTextComponent.setToolTipText(title);
                        }
                    }
                }
            }
        });
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        scrollPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        var textPanel = new JPanel(new GridLayout(1, 1));
        if (width != -1) {
            textPanel.setMinimumSize(new Dimension(width, height));
            textPanel.setPreferredSize(new Dimension(width, height));
        }
        textPanel.add(scrollPane);

        var fluffPanel = new FixedXPanel();
        if (width != -1) {
            fluffPanel.setMinimumSize(new Dimension(width, height));
            fluffPanel.setPreferredSize(new Dimension(width, height));
        }
        fluffPanel.add(fluffImageComponent);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
        p.add(textPanel);
        p.add(fluffPanel);
        p.add(Box.createHorizontalGlue());
        setLayout(new BorderLayout());
        add(p);
        addMouseWheelListener(wheelForwarder);
    }

    public void showEntity(Entity entity, EntityReadout mekView) {
        readoutTextComponent.setText(mekView.getFullReadout());
        readoutTextComponent.setCaretPosition(0);
        setFluffImage(entity);
    }

    public void showEntity(Entity entity, EntityReadout mekView, String fontName) {
        readoutTextComponent.setText(mekView.getFullReadout(fontName, ViewFormatting.HTML));
        readoutTextComponent.setCaretPosition(0);
        setFluffImage(entity);
    }

    public void showEntity(Entity entity, EntityReadout mekView, String fontName,
          Collection<ReadoutSections> sections) {
        showReadout(mekView, fontName, sections);
        setFluffImage(entity);
    }

    private void showReadout(EntityReadout readout, String fontName, Collection<ReadoutSections> sections) {
        readoutTextComponent.setText(readout.getReadout(fontName, ViewFormatting.HTML, sections));
        readoutTextComponent.setCaretPosition(0);
    }

    public void showEntity(Entity entity, TROView troView) {
        readoutTextComponent.setText(troView.processTemplate());
        readoutTextComponent.setCaretPosition(0);
        setFluffImage(entity);
    }

    public void showEntity(Entity entity, boolean useAlternateCost) {
        EntityReadout mekView = EntityReadout.createReadout(entity, false, useAlternateCost);
        showEntity(entity, mekView);
    }

    public void showEntity(Entity entity, String fontName) {
        EntityReadout mekView = EntityReadout.createReadout(entity,
              false,
              false,
              (entity.isUncrewed())
        );
        showEntity(entity, mekView, fontName);
    }

    public void showEntity(Entity entity, boolean showDetail, boolean useAlternateCost,
          boolean ignorePilotBV, String fontName, Collection<ReadoutSections> sections) {

        EntityReadout mekView = EntityReadout.createReadout(entity, showDetail, useAlternateCost,
              ignorePilotBV);
        showEntity(entity, mekView, fontName, sections);
    }

    private void setFluffImage(Entity entity) {
        boolean isSpritesOnly = PreferenceManager.getClientPreferences().getSpritesOnly();
        Image image = isSpritesOnly ? null : FluffImageHelper.getFluffImage(entity);
        // Scale down to the default width if the image is wider than that
        if (null != image) {
            if (image.getWidth(this) > DEFAULT_WIDTH) {
                image = image.getScaledInstance(DEFAULT_WIDTH, -1, Image.SCALE_SMOOTH);
            }
            fluffImageComponent.setIcon(new ImageIcon(image));
        } else {
            fluffImageComponent.setIcon(null);
        }
    }

    public void reset() {
        readoutTextComponent.setText("");
        fluffImageComponent.setIcon(null);
    }

    /** Forwards a mouse wheel scroll on the fluff image or free space to the TRO entry. */
    MouseWheelListener wheelForwarder = e -> {
        MouseWheelEvent converted = (MouseWheelEvent) SwingUtilities.convertMouseEvent(EntityReadoutPanel.this, e,
              scrollPane);
        for (MouseWheelListener listener : scrollPane.getMouseWheelListeners()) {
            listener.mouseWheelMoved(converted);
        }
    };
}
