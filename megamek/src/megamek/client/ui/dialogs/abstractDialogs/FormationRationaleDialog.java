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
package megamek.client.ui.dialogs.abstractDialogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import megamek.client.formation.AssemblyUnit;
import megamek.client.formation.FormationRationale;
import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitRole;

/**
 * Explains why one formation holds the units it holds: the doctrine name it earned, the ledger that
 * scored it, what could not be split apart, and the closest grouping the assembler passed over.
 * Reached from the lobby force tree's right-click menu.
 */
public class FormationRationaleDialog extends AbstractDialog {

    private final transient FormationRationale rationale;

    public FormationRationaleDialog(final JFrame frame, final FormationRationale rationale) {
        super(frame, false, "FormationRationaleDialog", "FormationRationaleDialog.title");
        this.rationale = Objects.requireNonNull(rationale);
        initialize();
    }

    @Override
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();
        setTitle(getTitle() + " (" + rationale.formationName() + ")");
        pack();
        Dimension screenSize = UIUtil.getScaledScreenSize(this);
        setSize(new Dimension(Math.min(getWidth(), (int) (screenSize.getWidth() * 0.6)),
              Math.min(getHeight(), (int) (screenSize.getHeight() * 0.8))));
    }

    @Override
    protected Container createCenterPane() {
        String report = buildReport();

        JEditorPane reportPane = new JEditorPane("text/html", report);
        reportPane.setEditable(false);
        reportPane.setCaretPosition(0);
        reportPane.setBorder(new EmptyBorder(0, 0, 0, UIUtil.scaleForGUI(10)));

        JButton copyText = new JButton(Messages.getString("FormationRationaleDialog.copy"));
        copyText.addActionListener(evt -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(report), null);
        });

        JScrollPane scrollPane = new JScrollPane(reportPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(new EmptyBorder(UIUtil.scaleForGUI(10), 0, 0, 0));
        scrollPane.setPreferredSize(UIUtil.scaleForGUI(620, 560));

        JPanel buttonPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(copyText);

        Box centerPanel = Box.createVerticalBox();
        centerPanel.setBorder(new EmptyBorder(UIUtil.scaleForGUI(15), UIUtil.scaleForGUI(15),
              UIUtil.scaleForGUI(15), UIUtil.scaleForGUI(15)));
        centerPanel.add(buttonPanel);
        centerPanel.add(scrollPane);
        return centerPanel;
    }

    /** Builds the whole report: the plain-language answer first, the supporting detail after. */
    private String buildReport() {
        StringBuilder report = new StringBuilder("<html><body>");
        report.append("<h2>").append(rationale.formationName()).append("</h2>");

        int unitCount = rationale.units().size();
        report.append("<p><b>");
        if (rationale.type() != null) {
            report.append(Messages.getString("FormationRationale.summary.typed", unitCount,
                  rationale.type().getName() + " " + rationale.organization().getElementWord()));
        } else {
            report.append(Messages.getString("FormationRationale.summary.untyped", unitCount));
        }
        report.append("</b><br>").append(Messages.getString("FormationRationale.organization",
              Messages.getString("Organization." + rationale.organization().name()))).append("</p>");

        appendMembers(report);
        appendRequirements(report);
        appendReasons(report);
        appendBindings(report);
        appendAlternatives(report);

        return report.append("</body></html>").toString();
    }

    /**
     * The rulebook against the roster: the ideal role and its waiver first, then a row per
     * requirement and a column per unit, so a player can see exactly which unit carries which
     * requirement and where a formation falls short.
     */
    private void appendRequirements(StringBuilder report) {
        if (rationale.type() == null) {
            return;
        }
        report.append("<h3>").append(Messages.getString("FormationRationale.requirements"))
              .append("</h3>");

        if (rationale.idealRole() != UnitRole.UNDETERMINED) {
            report.append("<p><b>")
                  .append(Messages.getString("FormationRationale.idealRole", rationale.idealRole()))
                  .append("</b><br>");
            report.append(Messages.getString(rationale.idealRoleWaived()
                  ? "FormationRationale.idealRole.waived"
                  : "FormationRationale.idealRole.notWaived", rationale.units().size()));
            report.append("</p>");
        }

        if (rationale.requirements().isEmpty()) {
            return;
        }

        report.append("<table cellpadding='3'><tr><th align='left'>")
              .append(Messages.getString("FormationRationale.col.requirement")).append("</th>");
        // Columns are the unit numbers from the members table above: model designations repeat
        // within a formation ("Stalking Spider II" twice) and would label two columns the same.
        for (int number = 1; number <= rationale.units().size(); number++) {
            report.append("<th align='center'>").append(number).append("</th>");
        }
        report.append("<th align='center'>")
              .append(Messages.getString("FormationRationale.col.metNeeded")).append("</th></tr>");

        for (FormationRationale.Requirement requirement : rationale.requirements()) {
            report.append("<tr><td>").append(describe(requirement));
            if (!requirement.waivable()) {
                report.append(" <i>(")
                      .append(Messages.getString("FormationRationale.neverWaived")).append(")</i>");
            }
            report.append("</td>");
            for (Boolean matched : requirement.perUnit()) {
                report.append("<td align='center'>").append(mark(matched)).append("</td>");
            }
            boolean carried = requirement.satisfied() || (rationale.idealRoleWaived()
                  && requirement.waivable());
            report.append("<td align='center'>").append(carried ? "<b>" : "")
                  .append(requirement.met()).append(" / ").append(requirement.required())
                  .append(carried ? "</b>" : "").append("</td></tr>");
        }
        report.append("</table>");
    }

    /** Met or not, as text: the report must stay plain ASCII, so no tick marks. */
    private static String mark(boolean matched) {
        return matched
              ? "<b>" + Messages.getString("FormationRationale.met") + "</b>"
              : Messages.getString("FormationRationale.notMet");
    }

    /** Turns a requirement's facts into a sentence, so the wording lives in one place. */
    private String describe(FormationRationale.Requirement requirement) {
        int size = rationale.units().size();
        return switch (requirement.kind()) {
            case UNIT_TYPE -> Messages.getString("FormationRationale.req.unitType",
                  requirement.description());
            case WEIGHT_CLASS -> Messages.getString("FormationRationale.req.weight",
                  requirement.description());
            case EVERY_UNIT -> Messages.getString("FormationRationale.req.everyUnit",
                  requirement.description());
            case AT_LEAST -> Messages.getString("FormationRationale.req.atLeast",
                  requirement.required(), size, requirement.description());
            case AT_LEAST_ALTERNATIVE -> Messages.getString("FormationRationale.req.atLeastAlternative",
                  requirement.required(), size, requirement.description());
            case GROUPING -> Messages.getString("FormationRationale.req.grouping",
                  requirement.description());
        };
    }

    private void appendMembers(StringBuilder report) {
        report.append("<h3>").append(Messages.getString("FormationRationale.members")).append("</h3>");
        report.append("<table cellpadding='3'><tr><th align='left'>#</th>")
              .append(headerCell("FormationRationale.col.unit"))
              .append(headerCell("FormationRationale.col.role"))
              .append(headerCell("FormationRationale.col.weight"))
              .append(headerCell("FormationRationale.col.walk"))
              .append(headerCell("FormationRationale.col.bv"))
              .append("</tr>");
        int number = 1;
        for (AssemblyUnit unit : rationale.units()) {
            report.append("<tr><td>").append(number++).append("</td><td>")
                  .append(unit.displayName()).append("</td><td>")
                  .append(roleName(unit.role())).append("</td><td>")
                  .append(EntityWeightClass.getClassName(unit.weightClass())).append("</td><td>")
                  .append(unit.walkMp()).append("</td><td>")
                  .append(unit.battleValue())
                  .append(unit.carriesEcm() ? " (ECM)" : "")
                  .append("</td></tr>");
        }
        report.append("</table>");
    }

    private static String headerCell(String key) {
        return "<th align='left'>" + Messages.getString(key) + "</th>";
    }

    private void appendReasons(StringBuilder report) {
        report.append("<h3>").append(Messages.getString("FormationRationale.why")).append("</h3><ul>");

        if (rationale.type() != null) {
            report.append("<li>").append(Messages.getString(rationale.idealRoleWaived()
                  ? "FormationRationale.why.typeByIdealRole"
                  : "FormationRationale.why.type", rationale.type().getName())).append("</li>");
        } else if (!rationale.unknownToCatalog().isEmpty()) {
            report.append("<li>").append(Messages.getString("FormationRationale.why.noTypeUnknown",
                  String.join(", ", rationale.unknownToCatalog()))).append("</li>");
        } else {
            report.append("<li>").append(Messages.getString("FormationRationale.why.noType"))
                  .append("</li>");
        }

        if (rationale.modalRole() == UnitRole.UNDETERMINED) {
            report.append("<li>").append(Messages.getString("FormationRationale.why.noRole"))
                  .append("</li>");
        } else {
            report.append("<li>").append(Messages.getString("FormationRationale.why.role",
                  roleName(rationale.modalRole()), rationale.modalRoleCount(),
                  rationale.units().size())).append("</li>");
        }

        if (rationale.speedSpread() == 0) {
            report.append("<li>").append(Messages.getString("FormationRationale.why.speedSame",
                  rationale.slowestWalkMp())).append("</li>");
        } else {
            report.append("<li>").append(Messages.getString("FormationRationale.why.speedSpread",
                  rationale.slowestWalkMp(), rationale.fastestWalkMp())).append("</li>");
        }

        report.append("<li>").append(Messages.getString("FormationRationale.why.bv",
              rationale.battleValue())).append("</li>");
        if (rationale.ecmCarriers() > 0) {
            report.append("<li>").append(Messages.getString("FormationRationale.why.ecm",
                  rationale.ecmCarriers())).append("</li>");
        }
        report.append("</ul>");
    }

    private void appendBindings(StringBuilder report) {
        if (rationale.bindings().isEmpty()) {
            return;
        }
        report.append("<h3>").append(Messages.getString("FormationRationale.bindings"))
              .append("</h3><ul>");
        for (String binding : rationale.bindings()) {
            report.append("<li>").append(binding).append("</li>");
        }
        report.append("</ul>");
    }

    private void appendAlternatives(StringBuilder report) {
        report.append("<h3>").append(Messages.getString("FormationRationale.alternatives"))
              .append("</h3>");
        if (rationale.closestAlternatives().isEmpty()) {
            report.append("<p>").append(Messages.getString("FormationRationale.alternatives.none"))
                  .append("</p>");
            return;
        }
        report.append("<ul>");
        for (FormationRationale.AlternativeSwap swap : rationale.closestAlternatives()) {
            String key = (swap.cost() >= 0)
                  ? "FormationRationale.alternatives.row"
                  : "FormationRationale.alternatives.rowBetter";
            report.append("<li>").append(Messages.getString(key, swap.unitName(), swap.otherUnitName(),
                  swap.otherFormation(), Math.round(Math.abs(swap.cost())))).append("</li>");
        }
        report.append("</ul>");
    }

    private static String roleName(UnitRole role) {
        return (role == UnitRole.UNDETERMINED) ? "-" : role.toString();
    }
}
