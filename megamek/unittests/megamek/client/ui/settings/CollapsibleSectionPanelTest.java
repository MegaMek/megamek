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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import org.junit.jupiter.api.Test;

class CollapsibleSectionPanelTest {
    @Test
    void newPanelIsExpandedByDefault() throws Exception {
        runOnEdt(() -> assertTrue(new CollapsibleSectionPanel("Section").isExpanded()));
    }

    @Test
    void collapsingHidesContentAndFiresExpandedEvent() throws Exception {
        runOnEdt(() -> {
            JLabel content = new JLabel("Body");
            CollapsibleSectionPanel panel = new CollapsibleSectionPanel("Section", content);
            AtomicInteger eventCount = new AtomicInteger();
            AtomicReference<Object> lastNewValue = new AtomicReference<>();
            panel.addPropertyChangeListener(CollapsibleSectionPanel.EXPANDED_PROPERTY, event -> {
                eventCount.incrementAndGet();
                lastNewValue.set(event.getNewValue());
            });

            panel.setExpanded(false);

            assertFalse(panel.isExpanded());
            assertFalse(content.getParent().isVisible());
            assertEquals(1, eventCount.get());
            assertEquals(Boolean.FALSE, lastNewValue.get());
        });
    }

    @Test
    void expandingAgainShowsContentAndFiresExpandedEvent() throws Exception {
        runOnEdt(() -> {
            JLabel content = new JLabel("Body");
            CollapsibleSectionPanel panel = new CollapsibleSectionPanel("Section", content);
            panel.setExpanded(false);
            AtomicInteger eventCount = new AtomicInteger();
            panel.addPropertyChangeListener(CollapsibleSectionPanel.EXPANDED_PROPERTY,
                  event -> eventCount.incrementAndGet());

            panel.setExpanded(true);

            assertTrue(panel.isExpanded());
            assertTrue(content.getParent().isVisible());
            assertEquals(1, eventCount.get());
        });
    }

    @Test
    void redundantSetExpandedDoesNotFireEvent() throws Exception {
        runOnEdt(() -> {
            CollapsibleSectionPanel panel = new CollapsibleSectionPanel("Section");
            AtomicInteger eventCount = new AtomicInteger();
            panel.addPropertyChangeListener(CollapsibleSectionPanel.EXPANDED_PROPERTY,
                  event -> eventCount.incrementAndGet());

            panel.setExpanded(true);

            assertTrue(panel.isExpanded());
            assertEquals(0, eventCount.get());
        });
    }

    @Test
    void toggleActionFlipsExpandedState() throws Exception {
        runOnEdt(() -> {
            CollapsibleSectionPanel panel = new CollapsibleSectionPanel("Section");
            Action toggleAction = panel.getActionMap().get(CollapsibleSectionPanel.TOGGLE_ACTION);

            toggleAction.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED,
                  CollapsibleSectionPanel.TOGGLE_ACTION));
            assertFalse(panel.isExpanded());

            toggleAction.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED,
                  CollapsibleSectionPanel.TOGGLE_ACTION));
            assertTrue(panel.isExpanded());
        });
    }

    @Test
    void contentPassedToConstructorIsMeasured() throws Exception {
        runOnEdt(() -> {
            JPanel content = new JPanel();
            content.setPreferredSize(new Dimension(321, 50));
            CollapsibleSectionPanel panel = new CollapsibleSectionPanel("Section", content);

            assertTrue(panel.getContentPreferredWidth() >= 321);
        });
    }

    @Test
    void headerAndContentUseScaledPadding() throws Exception {
        runOnEdt(() -> {
            CollapsibleSectionPanel panel = new CollapsibleSectionPanel("Section", new JLabel("Body"));
            JPanel header = (JPanel) panel.getComponent(0);
            JPanel content = (JPanel) panel.getComponent(1);

            Insets headerInsets = header.getBorder().getBorderInsets(header);
            assertEquals(UIUtil.scaleForGUI(6), headerInsets.top);
            assertEquals(UIUtil.scaleForGUI(8), headerInsets.left);
            assertEquals(UIUtil.scaleForGUI(6), headerInsets.bottom);
            assertEquals(UIUtil.scaleForGUI(8), headerInsets.right);

            Insets contentInsets = content.getBorder().getBorderInsets(content);
            assertEquals(UIUtil.scaleForGUI(6), contentInsets.top);
            assertEquals(UIUtil.scaleForGUI(24), contentInsets.left);
            assertEquals(UIUtil.scaleForGUI(6), contentInsets.bottom);
            assertEquals(0, contentInsets.right);
        });
    }

    @Test
    void titleStaysLeftAlignedWhenNoSummaryIsSet() throws Exception {
        runOnEdt(() -> {
            CollapsibleSectionPanel panel = new CollapsibleSectionPanel("Section");
            panel.setSize(600, Math.max(40, panel.getPreferredSize().height));
            layoutTree(panel);

            JLabel title = findLabelByText(panel, "Section");
            assertNotNull(title, "the title label should exist");
            Point titleInPanel = SwingUtilities.convertPoint(title.getParent(), title.getLocation(), panel);
            assertTrue(titleInPanel.x < 150,
                  "the title should be left-aligned, but was rendered at x=" + titleInPanel.x);
        });
    }

    private static void layoutTree(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static JLabel findLabelByText(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
            if (child instanceof Container container) {
                JLabel match = findLabelByText(container, text);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void runOnEdt(CheckedRunnable runnable) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            });
        } catch (InvocationTargetException exception) {
            Throwable failure = exception.getCause();
            if (failure instanceof RuntimeException && failure.getCause() != null) {
                failure = failure.getCause();
            }
            if (failure instanceof Error error) {
                throw error;
            }
            if (failure instanceof Exception checkedException) {
                throw checkedException;
            }
            throw exception;
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
