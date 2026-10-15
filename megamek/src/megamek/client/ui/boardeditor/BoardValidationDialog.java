/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.boardeditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;

/**
 * A report of a board's validation errors and notes. Clicking a line that names a hex takes the editor to that hex.
 * The report is not modal, so the map can still be scrolled, dragged and edited while it is open.
 */
class BoardValidationDialog extends JDialog {

    /**
     * One line of the report.
     *
     * @param text   the line as shown
     * @param coords the hex the line concerns, or {@code null} for headings and blank lines
     * @param fix    a recommended fix, shown when hovering over the line, or {@code null} if there is none
     */
    record ReportLine(String text, @Nullable Coords coords, @Nullable String fix) {
        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * @param owner   the board editor frame
     * @param heading the line shown above the list
     * @param lines   the report lines
     * @param goToHex called with the hex of a clicked line
     */
    BoardValidationDialog(JFrame owner, String heading, List<ReportLine> lines, Consumer<Coords> goToHex) {
        super(owner, Messages.getString("BoardEditor.invalidBoard.title"), false);

        JList<ReportLine> reportList = new JList<>(lines.toArray(new ReportLine[0])) {
            @Override
            public String getToolTipText(MouseEvent event) {
                int index = locationToIndex(event.getPoint());
                String fix = (index < 0) ? null : getModel().getElementAt(index).fix();
                return (fix == null) ? null : Messages.getString("BoardView1.recommendedFix") + fix;
            }
        };
        // Registers the list with the tooltip manager so the fixes show on hover
        reportList.setToolTipText("");
        reportList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reportList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int index = reportList.locationToIndex(event.getPoint());
                if (index < 0) {
                    return;
                }
                Coords coords = reportList.getModel().getElementAt(index).coords();
                if (coords != null) {
                    goToHex.accept(coords);
                }
            }
        });

        int gap = UIUtil.scaleForGUI(8);
        JPanel content = new JPanel(new BorderLayout(gap, gap));
        int margin = UIUtil.scaleForGUI(10);
        content.setBorder(new EmptyBorder(margin, margin, margin, margin));
        content.add(new JLabel(heading + " " + Messages.getString("BoardEditor.validation.clickHint")),
              BorderLayout.NORTH);
        content.add(new JScrollPane(reportList), BorderLayout.CENTER);

        JButton closeButton = new JButton(Messages.getString("Close"));
        closeButton.addActionListener(event -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(content);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(UIUtil.scaleForGUI(760, 420));
        setLocationRelativeTo(owner);
    }
}
