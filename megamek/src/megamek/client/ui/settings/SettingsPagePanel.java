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

import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.FontHandler.symbolIcon;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;

/**
 * Standard settings-page shell with a header, optional intro, collapsible sections, section search text, and an
 * optional quote. Build pages through {@link Builder}.
 */
public class SettingsPagePanel extends JPanel {
    public static final int DEFAULT_MAXIMUM_PAGE_WIDTH = 950;
    public static final int DEFAULT_SECTION_STACK_WIDTH = 650;

    private static final int INTRO_HORIZONTAL_PADDING = 24;
    private static final int SECTION_CONTROLS_TOP_PADDING = 8;
    private static final int QUOTE_TOP_PADDING = 12;
    private static final int QUOTE_BOTTOM_PADDING = 8;
    private static final int QUOTE_HORIZONTAL_PADDING = 24;
    private static final SettingsTextProvider FRAMEWORK_TEXT = SettingsTextProvider.megaMek();

    private final JPanel pageBody;
    private final boolean showDetailsPanel;
    private final String bodySearchText;
    private final String pageSearchText;
    private final List<SearchableSection> searchableSections;
    private final int maximumPageWidth;

    private SettingsPagePanel(Builder builder) {
        super(null);
        setName("pnl" + builder.name + "Page");
        showDetailsPanel = builder.showDetailsPanel;
        maximumPageWidth = builder.maximumPageWidth;

        pageBody = new JPanel(new BorderLayout());
        pageBody.setName("pnl" + builder.name + "PageBody");
        pageBody.setOpaque(false);

        List<Object> renderItems = new ArrayList<>();
        List<CollapsibleSectionPanel> sections = new ArrayList<>();
        List<SearchableSection> searchable = new ArrayList<>();
        StringBuilder allBodyText = new StringBuilder();
        for (Object bodyItem : builder.bodyItems) {
            if (bodyItem instanceof Section definition) {
                CollapsibleSectionPanel section = createSection(builder, definition);
                sections.add(section);
                renderItems.add(section);
                String text = sectionSearchText(builder.textProvider, definition);
                searchable.add(new SearchableSection(section, text));
                if (!text.isBlank()) {
                    allBodyText.append(' ').append(text);
                }
            } else if (bodyItem instanceof JComponent component) {
                renderItems.add(component);
                String text = SettingsSearchText.collect(component);
                if (!text.isBlank()) {
                    allBodyText.append(' ').append(text);
                }
            }
        }
        searchableSections = List.copyOf(searchable);
        bodySearchText = allBodyText.toString().trim();
        StringBuilder allPageText = new StringBuilder();
        appendSearchText(allPageText, headerSearchText(builder));
        appendSearchText(allPageText, introSearchText(builder));
        appendSearchText(allPageText, bodySearchText);
        appendSearchText(allPageText, quoteSearchText(builder));
        pageSearchText = allPageText.toString();

        JPanel sectionControls = createSectionControls(sections);
        int minimumSectionStackWidth = sections.isEmpty() && !builder.standardContentWidth
              ? 0
              : builder.sectionStackWidth;
        int sectionStackWidth = preferredSectionStackWidth(sections, sectionControls, minimumSectionStackWidth);

        JPanel contentPanel = createContentPanel(builder, renderItems, sections, sectionControls,
              sectionStackWidth, minimumSectionStackWidth);
        pageBody.add(contentPanel, BorderLayout.CENTER);
        int quoteWidth = sectionStackWidth > 0 ? sectionStackWidth : contentPanel.getPreferredSize().width;
        JComponent quotePanel = createQuotePanel(builder, quoteWidth);
        if (quotePanel != null) {
            pageBody.add(quotePanel, BorderLayout.SOUTH);
        }
        add(pageBody);
    }

    public static Builder builder(String name, SettingsTextProvider textProvider,
          String headerTextKey, @Nullable Icon headerIcon) {
        return new Builder(name, textProvider, headerTextKey, headerIcon);
    }

    /**
     * @return this page's definitive details-panel policy, which takes precedence over its route fallback
     */
    public boolean shouldShowDetailsPanel() {
        return showDetailsPanel;
    }

    public String getSectionSearchText() {
        return bodySearchText;
    }

    public String getPageSearchText() {
        return pageSearchText;
    }

    /** Expands matching sections and collapses the others, leaving the page unchanged when nothing matches. */
    public boolean expandSectionsMatching(Predicate<String> sectionTextMatcher) {
        boolean[] matches = new boolean[searchableSections.size()];
        boolean anyMatch = false;
        for (int index = 0; index < searchableSections.size(); index++) {
            matches[index] = sectionTextMatcher.test(searchableSections.get(index).searchText());
            anyMatch |= matches[index];
        }
        if (!anyMatch) {
            return false;
        }
        for (int index = 0; index < searchableSections.size(); index++) {
            searchableSections.get(index).panel().setExpanded(matches[index]);
        }
        return true;
    }

    public void expandAllSections() {
        setExpanded(true, searchableSections.stream().map(SearchableSection::panel).toList());
    }

    List<Boolean> getSectionExpansionState() {
        return searchableSections.stream().map(section -> section.panel().isExpanded()).toList();
    }

    void restoreSectionExpansionState(List<Boolean> expansionState) {
        int count = Math.min(searchableSections.size(), expansionState.size());
        for (int index = 0; index < count; index++) {
            searchableSections.get(index).panel().setExpanded(expansionState.get(index));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = getContentPreferredSize();
        return new Dimension(Math.min(preferredSize.width, UIUtil.scaleForGUI(maximumPageWidth)),
              preferredSize.height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, pageBody.getMinimumSize().height);
    }

    @Override
    public void doLayout() {
        Dimension preferredSize = getContentPreferredSize();
        int contentWidth = preferredSize.width;
        int x = Math.max(0, (getWidth() - contentWidth) / 2);
        pageBody.setBounds(x, 0, contentWidth, preferredSize.height);
    }

    Dimension getContentPreferredSize() {
        return pageBody.getPreferredSize();
    }

    private JPanel createContentPanel(Builder builder, List<Object> renderItems,
            List<CollapsibleSectionPanel> sections, JPanel sectionControls, int sectionStackWidth,
            int minimumSectionStackWidth) {
        JComponent header = builder.headerComponent != null
              ? builder.headerComponent
              : createDefaultHeader(builder);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("pnl" + builder.name);
        panel.setOpaque(false);

        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = 0;
        layout.anchor = GridBagConstraints.CENTER;
        layout.fill = GridBagConstraints.NONE;
        panel.add(header, layout);

        if (builder.introComponent != null || builder.introTextKey != null) {
            layout.gridy++;
            int introWidth = sectionStackWidth > 0 ? sectionStackWidth : header.getPreferredSize().width;
            introWidth = Math.min(introWidth, UIUtil.scaleForGUI(builder.maximumPageWidth));
            panel.add(createIntroPanel(builder, introWidth), layout);
        }

        if (!renderItems.isEmpty()) {
            layout.gridy++;
            layout.anchor = GridBagConstraints.NORTHWEST;
            panel.add(createSectionStackPanel(renderItems, sections, sectionControls, minimumSectionStackWidth),
                  layout);
        }
        return panel;
    }

    private JComponent createDefaultHeader(Builder builder) {
        String headerText = builder.textProvider.getText(builder.headerTextKey);
        String bodyText = builder.headerBodyTextKey == null
              ? null
              : builder.textProvider.getText(builder.headerBodyTextKey);
        return new SettingsHeaderPanel(builder.name, headerText, builder.headerIcon, bodyText,
              builder.headerBodyTextWidth);
    }

    private JPanel createSectionStackPanel(List<Object> renderItems, List<CollapsibleSectionPanel> sections,
            JPanel sectionControls, int minimumStackWidth) {
        JPanel stack = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferredSize = super.getPreferredSize();
                return new Dimension(Math.max(preferredSize.width, UIUtil.scaleForGUI(minimumStackWidth)),
                      preferredSize.height);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, super.getMinimumSize().height);
            }
        };
        stack.setOpaque(false);
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = -1;
        layout.weightx = 1.0;
        layout.fill = GridBagConstraints.HORIZONTAL;

        boolean controlsAdded = false;
        for (Object renderItem : renderItems) {
            if (renderItem instanceof CollapsibleSectionPanel section) {
                if (!controlsAdded && !sections.isEmpty()) {
                    layout.gridy++;
                    layout.anchor = GridBagConstraints.EAST;
                    stack.add(sectionControls, layout);
                    controlsAdded = true;
                }
                layout.gridy++;
                layout.anchor = GridBagConstraints.NORTHWEST;
                stack.add(section, layout);
            } else if (renderItem instanceof JComponent component) {
                layout.gridy++;
                layout.anchor = GridBagConstraints.NORTHWEST;
                stack.add(component, layout);
            }
        }
        return stack;
    }

    private JPanel createIntroPanel(Builder builder, int textWidth) {
        int horizontalPadding = horizontalPadding(textWidth, INTRO_HORIZONTAL_PADDING);
        JPanel intro;
        if (builder.introComponent != null) {
            intro = new JPanel(new BorderLayout());
            intro.setName(builder.name + "Intro");
            intro.setOpaque(false);
            intro.add(builder.introComponent, BorderLayout.CENTER);
        } else {
            intro = new SettingsIntroPanel(builder.name + "Intro",
                  builder.textProvider.getText(builder.introTextKey), textWidth - (horizontalPadding * 2));
        }
        intro.setBorder(BorderFactory.createEmptyBorder(0, horizontalPadding, 0, horizontalPadding));
        return intro;
    }

    private CollapsibleSectionPanel createSection(Builder builder, Section definition) {
        String title = definition.literal
              ? definition.title
              : builder.textProvider.getText(definition.title);
        String badgeHtml = SettingsBadge.formatHtml(definition.badges);
        if (!badgeHtml.isBlank()) {
            title = "<html><nobr>" + title + badgeHtml + "</nobr></html>";
        }
        CollapsibleSectionPanel section = new CollapsibleSectionPanel(title, definition.content);
        section.setSummary(sectionSummary(builder.textProvider, definition));
        section.setExpanded(builder.sectionsExpandedByDefault);
        if (definition.content instanceof SectionHeaderControlProvider provider) {
            section.setTrailingComponent(provider.getSectionHeaderControl());
            section.setTitleMuted(!provider.isSectionEnabled());
            provider.setSectionStateListener(() -> section.setTitleMuted(!provider.isSectionEnabled()));
        }
        return section;
    }

    private static String sectionSearchText(SettingsTextProvider textProvider, Section definition) {
        String title = SettingsSearchText.renderedText(
              definition.literal ? definition.title : textProvider.getText(definition.title));
        String summary = SettingsSearchText.renderedText(sectionSummary(textProvider, definition));
        String contentText = SettingsSearchText.collect(definition.content);
        return (title + ' ' + summary + ' ' + contentText).trim();
    }

    private static String headerSearchText(Builder builder) {
        if (builder.headerComponent != null) {
            return SettingsSearchText.collect(builder.headerComponent);
        }
        StringBuilder text = new StringBuilder(SettingsSearchText.renderedText(
              builder.textProvider.getText(builder.headerTextKey)));
        if (builder.headerBodyTextKey != null) {
            appendSearchText(text, SettingsSearchText.renderedText(
                  builder.textProvider.getText(builder.headerBodyTextKey)));
        }
        return text.toString();
    }

    private static String introSearchText(Builder builder) {
        if (builder.introComponent != null) {
            return SettingsSearchText.collect(builder.introComponent);
        }
        return builder.introTextKey == null
              ? ""
              : SettingsSearchText.renderedText(builder.textProvider.getText(builder.introTextKey));
    }

    private static String quoteSearchText(Builder builder) {
        return builder.quoteTextKey == null || !builder.textProvider.containsKey(builder.quoteTextKey)
              ? ""
              : SettingsSearchText.renderedText(builder.textProvider.getText(builder.quoteTextKey));
    }

    private static void appendSearchText(StringBuilder destination, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!destination.isEmpty()) {
            destination.append(' ');
        }
        destination.append(text.trim());
    }

    private static String sectionSummary(SettingsTextProvider textProvider, Section definition) {
        if (definition.summary == null) {
            return "";
        }
        return definition.literal ? definition.summary : textProvider.getText(definition.summary);
    }

    private static int preferredSectionStackWidth(List<CollapsibleSectionPanel> sections,
          JPanel controls, int minimumWidth) {
        if (sections.isEmpty()) {
                        return UIUtil.scaleForGUI(minimumWidth);
        }
        int contentWidth = controls.getPreferredSize().width;
        for (CollapsibleSectionPanel section : sections) {
            contentWidth = Math.max(contentWidth, section.getPreferredSize().width);
        }
        return Math.max(contentWidth, UIUtil.scaleForGUI(minimumWidth));
    }

    private JPanel createSectionControls(List<CollapsibleSectionPanel> sections) {
        JButton expandAll = sectionActionButton("SettingsPagePanel.expandAll.text", 0xE5D7, 0);
        expandAll.addActionListener(event -> setExpanded(true, sections));
        JButton collapseAll = sectionActionButton("SettingsPagePanel.collapseAll.text", 0xE5D6,
              UIUtil.scaleForGUI(2));
        collapseAll.addActionListener(event -> setExpanded(false, sections));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIUtil.scaleForGUI(5), 0));
        controls.setName("settingsSectionControls");
        controls.setOpaque(false);
        controls.setBorder(BorderFactory.createEmptyBorder(
              UIUtil.scaleForGUI(SECTION_CONTROLS_TOP_PADDING), 0, 0, 0));
        controls.add(expandAll);
        controls.add(collapseAll);
        return controls;
    }

    private JButton sectionActionButton(String textKey, int codePoint, int iconSizeBoost) {
        JButton button = new JButton(FRAMEWORK_TEXT.getText(textKey));
        button.putClientProperty("JComponent.sizeVariant", "small");
        button.setIcon(symbolIcon(codePoint, button.getFont().getSize() + iconSizeBoost, button.getForeground()));
        return button;
    }

    private JComponent createQuotePanel(Builder builder, int contentWidth) {
        if (builder.quoteTextKey == null || !builder.textProvider.containsKey(builder.quoteTextKey)) {
            return null;
        }
        int panelWidth = Math.max(1,
              Math.min(contentWidth, UIUtil.scaleForGUI(builder.maximumPageWidth)));
        int padding = horizontalPadding(panelWidth, QUOTE_HORIZONTAL_PADDING);
        int textWidth = panelWidth - (padding * 2);
        JPanel quotePanel = new JPanel(new GridBagLayout());
        quotePanel.setName("pnl" + builder.name + "QuotePanel");
        quotePanel.setOpaque(false);
        quotePanel.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(QUOTE_TOP_PADDING),
              padding, UIUtil.scaleForGUI(QUOTE_BOTTOM_PADDING), padding));

        String quoteHtml = "<html><body style='margin: 0; padding: 0; text-align: center;'>"
              + builder.textProvider.getText(builder.quoteTextKey) + "</body></html>";
        JEditorPane quote = new JEditorPane("text/html", quoteHtml);
        quote.setName("txt" + builder.name + "Quote");
        quote.setEditable(false);
        quote.setFocusable(false);
        quote.setOpaque(false);
        quote.setBorder(BorderFactory.createEmptyBorder());
        quote.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        setFontScaling(quote, false, 1);
        quote.setSize(textWidth, Short.MAX_VALUE);
        Dimension quoteSize = new Dimension(textWidth, quote.getPreferredSize().height);
        quote.setPreferredSize(quoteSize);
        quote.setMinimumSize(quoteSize);
        quotePanel.add(quote);
        return quotePanel;
    }

    private static int horizontalPadding(int width, int requestedPadding) {
        return Math.min(UIUtil.scaleForGUI(requestedPadding), Math.max(0, (width - 1) / 2));
    }

    private static void setExpanded(boolean expanded, List<CollapsibleSectionPanel> sections) {
        for (CollapsibleSectionPanel section : sections) {
            section.setExpanded(expanded);
        }
    }

    private record SearchableSection(CollapsibleSectionPanel panel, String searchText) {
    }

    private record Section(String title, @Nullable String summary, JComponent content,
                           Collection<SettingsBadge> badges, boolean literal) {
    }

    public static final class Builder {
        private final String name;
        private final SettingsTextProvider textProvider;
        private final String headerTextKey;
        private final Icon headerIcon;
        private final List<Object> bodyItems = new ArrayList<>();
        private JComponent headerComponent;
        private String headerBodyTextKey;
        private int headerBodyTextWidth = SettingsHeaderPanel.DEFAULT_BODY_TEXT_WIDTH;
        private String introTextKey;
        private JComponent introComponent;
        private String quoteTextKey;
        private boolean sectionsExpandedByDefault;
        private boolean showDetailsPanel = true;
        private boolean standardContentWidth;
        private int maximumPageWidth = DEFAULT_MAXIMUM_PAGE_WIDTH;
        private int sectionStackWidth = DEFAULT_SECTION_STACK_WIDTH;

        private Builder(String name, SettingsTextProvider textProvider, String headerTextKey,
              @Nullable Icon headerIcon) {
            this.name = Objects.requireNonNull(name);
            this.textProvider = Objects.requireNonNull(textProvider);
            this.headerTextKey = Objects.requireNonNull(headerTextKey);
            this.headerIcon = headerIcon;
        }

        public Builder header(JComponent component) {
            headerComponent = Objects.requireNonNull(component);
            return this;
        }

        public Builder headerBody(String textKey) {
            headerBodyTextKey = Objects.requireNonNull(textKey);
            return this;
        }

        public Builder headerBodyWidth(int width) {
            headerBodyTextWidth = width;
            return this;
        }

        public Builder intro(String textKey) {
            introTextKey = Objects.requireNonNull(textKey);
            introComponent = null;
            return this;
        }

        public Builder introComponent(JComponent component) {
            introComponent = Objects.requireNonNull(component);
            introTextKey = null;
            return this;
        }

        public Builder quote(String textKey) {
            quoteTextKey = Objects.requireNonNull(textKey);
            return this;
        }

        public Builder showDetailsPanel(boolean show) {
            showDetailsPanel = show;
            return this;
        }

        public Builder standardContentWidth() {
            standardContentWidth = true;
            return this;
        }

        public Builder maximumPageWidth(int width) {
            maximumPageWidth = width;
            return this;
        }

        public Builder sectionStackWidth(int width) {
            sectionStackWidth = width;
            return this;
        }

        public Builder sectionsExpandedByDefault(boolean expanded) {
            sectionsExpandedByDefault = expanded;
            return this;
        }

        public Builder section(String titleKey, @Nullable String summaryKey, JComponent content) {
            return section(titleKey, summaryKey, content, List.of());
        }

        public Builder section(String titleKey, @Nullable String summaryKey, JComponent content,
              Collection<SettingsBadge> badges) {
            bodyItems.add(new Section(titleKey, summaryKey, content, List.copyOf(badges), false));
            return this;
        }

        public Builder literalSection(String title, @Nullable String summary, JComponent content) {
            bodyItems.add(new Section(title, summary, content, List.of(), true));
            return this;
        }

        public Builder component(JComponent component) {
            bodyItems.add(Objects.requireNonNull(component));
            return this;
        }

        public SettingsPagePanel build() {
            return new SettingsPagePanel(this);
        }
    }
}
