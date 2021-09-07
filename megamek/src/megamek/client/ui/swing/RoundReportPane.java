/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.ToolTipManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import megamek.MegaMek;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.DetachablePane;
import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.Report;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameReportEvent;

/**
 * Displays game round reports to the player.
 */
class RoundReportPane extends DetachablePane {


    private class RoundReport extends JComponent {


        private JTextPane report;
        private JScrollPane scroller;


        private RoundReport() {
            setLayout(new BorderLayout());

            this.report = UIUtil.setupForHtml(new JTextPane() {
                    @Override
                    public String getToolTipText(MouseEvent event) {
                        Optional<String> text = Optional.empty();
                        var offset = viewToModel2D(event.getPoint());
                        if (offset >= 0) {
                            var doc = (HTMLDocument) getDocument();
                            var element = doc.getCharacterElement(offset);
                            if (element != null) {
                                text = UIUtil.getHtmlAttribute(
                                    element, HTML.Attribute.TITLE
                                );
                            }
                        }
                        return (text.isPresent())
                            ? text.get() : super.getToolTipText(event);
                    }
                });
            this.report.addHyperlinkListener(
                (e) -> RoundReportPane.this.linkClicked(e)
            );
            this.report.setEditable(false);

            this.scroller = new JScrollPane(
                this.report,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            );

            add(this.scroller, BorderLayout.CENTER);

            ToolTipManager.sharedInstance().registerComponent(this.report);
        }

        public void append(String html) {
            var doc = (HTMLDocument) this.report.getDocument();
            var body = doc.getElement(
                doc.getDefaultRootElement(),
                StyleConstants.NameAttribute,
                HTML.Tag.BODY
            );
            try {
                doc.insertBeforeEnd(body, html);
            } catch (Exception e) {
                MegaMek.getLogger().error(
                    "Error appending round report: " + e.toString(), e //$NON-NLS-1$
                );
            }
        }

    }


    private ClientGUI gui;
    private Game game;
    private Map<Integer,String> entityImageCache;
    private JTabbedPane rounds;


    RoundReportPane(ClientGUI gui, Game game, Map<Integer,String> entityImageCache) {
        super(
            "Round reports",
            new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
        );
        this.gui = gui;
        this.game = game;
        this.entityImageCache = entityImageCache;
        this.rounds = (JTabbedPane) getContent();

        game.addGameListener(new GameListenerAdapter() {
                @Override
                public void gameReport(GameReportEvent e) {
                    append(e.getRound(), e.getReports());
                }
            }
        );

        for (int i = 0; i < game.getRoundCount(); i++) {
            append(i, game.getReports(i));
        }
    }

    private void append(int round, Iterable<Report> reports) {
        for (int i = this.rounds.getTabCount(); i <= round; i++) {
            var label = (i == 0) ? "Deployment" : "Round " + i;
            this.rounds.insertTab(label, null, new RoundReport(), null, i);
        }

        this.rounds.setSelectedIndex(round);
        var html = new StringBuilder("<div>");
        for (var report: reports) {
            html.append(report.getHtml(this.entityImageCache));
        }
        html.append("</div>");

        var reportUi = (RoundReport) this.rounds.getComponentAt(round);
        reportUi.append(html.toString());
    }

    private void linkClicked(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                var uri = new URI(e.getDescription());
                var remainder = uri.getSchemeSpecificPart();
                int id = -1;
                switch (uri.getScheme()) {
                    case Report.ENTITY_SCHEME:
                        id = Integer.parseInt(remainder);
                        var entity = this.game.getEntityFromAllSources(id);
                        if (entity != null) {
                            this.gui.selectEntity(entity);
                        }
                        break;

                    case Report.HEX_SCHEME:
                        var parts = remainder.split(",");
                        this.gui.showHex(new Coords(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1])
                        ));
                        break;

                    case Report.PLAYER_SCHEME:
                        id = Integer.parseInt(remainder);
                        var player = this.game.getPlayer(id);
                        if (player != null) {
                            this.gui.chatTo(player);
                        }
                        break;
                }
            } catch (Exception ex) {
                MegaMek.getLogger().error(
                    "Error handing round report link activation", ex //$NON-NLS-1$
                );
            }
        }
    }

}
