/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.dialogs.unitDisplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import javax.swing.JMenuItem;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.Document;
import javax.swing.text.View;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import org.junit.jupiter.api.Test;

class WeaponPanelTest {
    @Test
    void targetTooltipRendersTheEntireFiringSolution() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            WeaponPanel panel = new WeaponPanel(mock(UnitDisplayPanel.class), null);
            try {
                panel.setTarget(null, "<b>Extra information &amp; modifiers</b>");
                panel.wRangeR.setText("8");
                panel.setToHit("7+ to hit<br>Movement +2");

                String rendered = tooltipText(panel.getTargetSummary());
                assertTrue(rendered.contains(Messages.getString("MekDisplay.NoTarget")), rendered);
                assertTrue(rendered.contains(Messages.getString("MekDisplay.Range") + " 8"), rendered);
                assertTrue(rendered.contains("7+ to hit"), rendered);
                assertTrue(rendered.contains("Movement +2"), rendered);
                assertTrue(rendered.contains("Extra information & modifiers"), rendered);

                panel.clearToHit();
                panel.wRangeR.setText(null);
                panel.wTargetExtraInfo.setText(null);
                rendered = tooltipText(panel.getTargetSummary());
                assertTrue(rendered.contains("---"), rendered);
                assertFalse(rendered.contains("null"), rendered);
            } finally {
                GUIPreferences.getInstance().removePreferenceChangeListener(panel);
            }
        });
    }

    private static String tooltipText(String summary) {
        JMenuItem item = new JMenuItem();
        item.setToolTipText(summary);
        JToolTip tooltip = item.createToolTip();
        tooltip.setTipText(item.getToolTipText());
        View view = assertInstanceOf(View.class, tooltip.getClientProperty(BasicHTML.propertyKey));
        Document document = view.getDocument();
        try {
            return document.getText(0, document.getLength());
        } catch (javax.swing.text.BadLocationException exception) {
            throw new AssertionError(exception);
        }
    }
}
