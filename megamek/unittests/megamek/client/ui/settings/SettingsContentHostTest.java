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
package megamek.client.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ListResourceBundle;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.Scrollable;

import megamek.client.ui.util.UIUtil;
import org.junit.jupiter.api.Test;

class SettingsContentHostTest {
    private static final SettingsTextProvider TEXT = SettingsTextProvider.fromResourceBundle(new ListResourceBundle() {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                  { "field.text", "Field" },
                  { "field.tooltip", "Raw field help" }
            };
        }
    });

    @Test
    void hostRoutesRawHelpTextAndRestoresTooltipWhenContentChanges() {
        SettingsLabel label = new SettingsLabel(TEXT, "field");
        String originalTooltip = label.getToolTipText();
        SettingsContentHost host = new SettingsContentHost(label, "Details", true);

        assertNull(label.getToolTipText());
        fireMouseEntered(label);

        JEditorPane helpPane = findComponent(host, "settingsHelpText", JEditorPane.class);
        assertTrue(helpPane.getText().contains("Raw field help"), helpPane.getText());

        host.setContent(new JLabel("Replacement"), false);

        assertNotNull(label.getToolTipText());
        assertTrue(label.getToolTipText().equals(originalTooltip));
    }

    @Test
    void explicitHelpUpdateTargetsNearestHost() {
        JLabel source = new JLabel("Source");
        SettingsContentHost host = new SettingsContentHost(source, "Details", true);

        SettingsContentHost nearestHost = SettingsContentHost.findHost(source);
        assertSame(host, nearestHost);
        nearestHost.setHelpText("Replacement help");

        JEditorPane helpPane = findComponent(host, "settingsHelpText", JEditorPane.class);
        assertTrue(helpPane.getText().contains("Replacement help"), helpPane.getText());
    }

    @Test
    void plainHelpTextIsEscapedBeforeRenderingAsHtml() {
        SettingsContentHost host = new SettingsContentHost(new JLabel("Source"), "Details", true);

        host.setHelpText("Use A < B & C > D");

        JEditorPane helpPane = findComponent(host, "settingsHelpText", JEditorPane.class);
        assertTrue(helpPane.getText().contains("A &lt; B &amp; C &gt; D"), helpPane.getText());
    }

    @Test
    void uppercaseHtmlHelpTextIsPreservedAsMarkup() {
        SettingsContentHost host = new SettingsContentHost(new JLabel("Source"), "Details", true);

        host.setHelpText("<HTML><B>Important</B></HTML>");

        JEditorPane helpPane = findComponent(host, "settingsHelpText", JEditorPane.class);
        assertTrue(helpPane.getText().contains("<b>Important</b>"), helpPane.getText());
        assertFalse(helpPane.getText().contains("&lt;HTML&gt;"), helpPane.getText());
    }

    @Test
    void widePageKeepsCappedViewportAndScrollableContentWidth() {
        JLabel wideContent = new JLabel();
        wideContent.setPreferredSize(new Dimension(2_000, 20));
        SettingsPagePanel page = SettingsPagePanel.builder("Test", TEXT, "field.text", null)
              .maximumPageWidth(100)
              .literalSection("Wide Section", null, wideContent)
              .build();
        SettingsContentHost host = new SettingsContentHost(page, "Details", false);

        JPanel contentPanel = findComponent(host, "settingsContentPanel", JPanel.class);
        Scrollable scrollableContent = (Scrollable) contentPanel;
        assertEquals(100, page.getPreferredSize().width);
        assertTrue(contentPanel.getPreferredSize().width > page.getPreferredSize().width);
        assertEquals(page.getPreferredSize().width,
              scrollableContent.getPreferredScrollableViewportSize().width);
    }

    @Test
    void hostRebindsHelpAfterNotifyLifecycle() {
        SettingsLabel label = new SettingsLabel(TEXT, "field");
        SettingsContentHost host = new SettingsContentHost(label, "Details", true);

        host.removeNotify();
        assertNotNull(label.getToolTipText());

        host.addNotify();
        assertNull(label.getToolTipText());
    }

    @Test
    void pageHelpPolicyOverridesHostFallback() {
        SettingsPagePanel page = SettingsPagePanel.builder("Test", TEXT, "field.text", null)
              .showDetailsPanel(false)
              .build();
        SettingsContentHost host = new SettingsContentHost(page, "Details", true);

        SettingsHelpPanel helpPanel = findComponent(host, "settingsHelpPanel", SettingsHelpPanel.class);
        assertFalse(helpPanel.isVisible());
    }

    @Test
    void helpPanelUsesScaledHeightAndTextPadding() {
        SettingsHelpPanel helpPanel = new SettingsHelpPanel("Details");
        JEditorPane helpPane = findComponent(helpPanel, "settingsHelpText", JEditorPane.class);

        assertEquals(UIUtil.scaleForGUI(120), helpPanel.getPreferredSize().height);
        assertEquals(UIUtil.scaleForGUI(120), helpPanel.getMinimumSize().height);
        Insets insets = helpPane.getBorder().getBorderInsets(helpPane);
        assertEquals(UIUtil.scaleForGUI(4), insets.top);
        assertEquals(UIUtil.scaleForGUI(8), insets.left);
        assertEquals(UIUtil.scaleForGUI(4), insets.bottom);
        assertEquals(UIUtil.scaleForGUI(8), insets.right);
    }

    @Test
    void compositeEditorFocusUsesParentHelpText() {
        SettingsSpinner spinner = new SettingsSpinner(TEXT, "field", 1, 0, 10, 1);
        SettingsContentHost host = new SettingsContentHost(spinner, "Details", true);
        JTextField editor = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();

        fireFocusGained(editor);

        JEditorPane helpPane = findComponent(host, "settingsHelpText", JEditorPane.class);
        assertTrue(helpPane.getText().contains("Raw field help"), helpPane.getText());
    }

    @Test
    void compositeEditorMouseEntryUsesParentHelpText() {
        SettingsSpinner spinner = new SettingsSpinner(TEXT, "field", 1, 0, 10, 1);
        SettingsContentHost host = new SettingsContentHost(spinner, "Details", true);
        JTextField editor = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();

        fireMouseEntered(editor);

        JEditorPane helpPane = findComponent(host, "settingsHelpText", JEditorPane.class);
        assertTrue(helpPane.getText().contains("Raw field help"), helpPane.getText());
    }

    private static void fireMouseEntered(Component component) {
        MouseEvent event = new MouseEvent(component, MouseEvent.MOUSE_ENTERED, 0, 0, 0, 0, 0, false);
        for (MouseListener listener : component.getMouseListeners()) {
            listener.mouseEntered(event);
        }
    }

    private static void fireFocusGained(Component component) {
        FocusEvent event = new FocusEvent(component, FocusEvent.FOCUS_GAINED);
        for (FocusListener listener : component.getFocusListeners()) {
            listener.focusGained(event);
        }
    }

    private static <T extends Component> T findComponent(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = findComponentOrNull(container, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName() + " named " + name);
    }

    private static <T extends Component> T findComponentOrNull(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = findComponentOrNull(container, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
