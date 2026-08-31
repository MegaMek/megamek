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
package megamek.client.ui.dialogs.unitDisplay;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import megamek.client.ui.Messages;
import megamek.client.ui.widget.picmap.LocationSelectListener;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * A unit drawn as a diagram on the left, with a card for whichever of its locations is chosen on the right.
 * <p>
 * Clicking a location on the diagram, or choosing it in the chooser above the cards, brings that location's card
 * forward. A general card, for what belongs to no one location, sits under the location cards. Subclasses say what
 * goes on each card: the GM damage editor puts its armor, structure and critical-hit controls there; the unit
 * display's Control tab puts the location's equipment there.
 * <p>
 * The diagram is an {@link ArmorPanel}, so it draws every unit type the unit display can, and it draws the unit as it
 * is unless a subclass overrides {@link #refreshDiagram()} to draw something else - the damage editor draws its
 * pending edits.
 * <p>
 * A subclass builds its cards by calling {@link #buildCards()} once its own fields are set - the cards are asked for
 * through overridable methods, so they cannot be built in this constructor.
 */
public abstract class AbstractLocationDiagram extends JSplitPane implements LocationSelectListener {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final double MIN_PAPERDOLL_SCALE = 1.6;
    private static final double MAX_PAPERDOLL_SCALE = 2.5;
    private static final double MAX_SCREEN_FRACTION = 0.7;
    private static final String LOCATION_CARD_PREFIX = "location-";

    private Entity entity;
    private final ArmorPanel paperdoll;
    private final JPanel panCards = new JPanel();
    private final CardLayout cardLayout = new CardLayout();
    private final JComboBox<DiagramChoice> comboLocation = new JComboBox<>();
    private final JPanel panPanels = new JPanel();
    private final List<DiagramChoice> choices = new ArrayList<>();
    private boolean choosing = false;

    /**
     * Creates the diagram for the given unit. Call {@link #buildCards()} from the subclass constructor once the
     * subclass can answer {@link #createLocationCard(int)}.
     *
     * @param entity the unit shown
     * @param game   the game the unit is in, or {@code null} outside a game; the diagram needs it for a few unit
     *               types (a fighter squadron draws its fighters from the game)
     */
    protected AbstractLocationDiagram(Entity entity, @Nullable Game game) {
        super(JSplitPane.HORIZONTAL_SPLIT);
        this.entity = Objects.requireNonNull(entity, "entity");

        panCards.setLayout(cardLayout);
        comboLocation.addActionListener(event -> {
            if (!choosing && (comboLocation.getSelectedItem() instanceof DiagramChoice choice)) {
                showCard(choice);
            }
        });

        // The diagram sizes itself to its drawn content, so it needs no preferred size of its own.
        paperdoll = new ArmorPanel(game, this);

        JPanel panChooser = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panChooser.add(new JLabel(Messages.getString("UnitEditorDialog.location")));
        panChooser.add(comboLocation);

        panPanels.setLayout(new BoxLayout(panPanels, BoxLayout.PAGE_AXIS));
        panCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        panPanels.add(panCards);

        JPanel panRight = new JPanel(new BorderLayout());
        panRight.add(panChooser, BorderLayout.PAGE_START);
        panRight.add(new JScrollPane(panPanels), BorderLayout.CENTER);

        // The user can drag the divider to trade diagram size against panel size; the position is remembered.
        setLeftComponent(new JScrollPane(paperdoll));
        setRightComponent(panRight);
        setResizeWeight(0.0);
        setOneTouchExpandable(true);
    }

    /**
     * Asks the subclass for a card per location and for the general card, and lays them out. Locations with no card
     * are left out of the chooser. Call once, from the subclass constructor.
     */
    protected final void buildCards() {
        for (int location = 0; location < entity.locations(); location++) {
            JComponent card = createLocationCard(location);
            if (card != null) {
                addCard(new DiagramChoice(LOCATION_CARD_PREFIX + location, entity.getLocationName(location), location),
                      card);
            }
        }
        for (DiagramChoice extra : createExtraChoices()) {
            JComponent card = createExtraCard(extra);
            if (card != null) {
                addCard(extra, card);
            }
        }
        // The general card is shown under the chosen location rather than as another location to choose. There is
        // no general part of a unit to click on the diagram, so a card that had to be chosen there was a card nobody
        // would find, and what is on it is always relevant.
        JComponent general = createGeneralCard();
        if (general != null) {
            general.setAlignmentX(Component.LEFT_ALIGNMENT);
            panPanels.add(general);
        }
        if (!choices.isEmpty()) {
            showCard(choices.getFirst());
        }
    }

    private void addCard(DiagramChoice choice, JComponent card) {
        choices.add(choice);
        panCards.add(card, choice.key());
        // an empty chooser selects its first item as it is added, and fires as if the user had chosen it
        choosing = true;
        try {
            comboLocation.addItem(choice);
        } finally {
            choosing = false;
        }
    }

    /**
     * @param location a location of the unit
     *
     * @return the card to show for that location, or {@code null} to leave the location out of the chooser
     */
    protected abstract @Nullable JComponent createLocationCard(int location);

    /**
     * @return the card shown under every location card, or {@code null} for none
     */
    protected @Nullable JComponent createGeneralCard() {
        return null;
    }

    /**
     * Entries of the chooser that are not locations of the unit - a Crew entry on a unit with no head to click, say.
     * Each gets a card from {@link #createExtraCard(DiagramChoice)}.
     *
     * @return the extra chooser entries, in chooser order; none by default
     */
    protected List<DiagramChoice> createExtraChoices() {
        return List.of();
    }

    /**
     * @param choice one of the entries from {@link #createExtraChoices()}
     *
     * @return the card for it, or {@code null} to leave it out after all
     */
    protected @Nullable JComponent createExtraCard(DiagramChoice choice) {
        return null;
    }

    /**
     * Called whenever a card is brought forward, by click, chooser or code.
     *
     * @param choice the entry now showing
     */
    protected void onCardShown(DiagramChoice choice) {
        // nothing by default
    }

    /**
     * Brings the given location's card forward, if it has one.
     *
     * @param location the location
     */
    public void selectLocation(int location) {
        for (DiagramChoice choice : choices) {
            if (choice.location() == location) {
                showCard(choice);
                return;
            }
        }
    }

    /**
     * Brings the card with the given key forward, if there is one.
     *
     * @param key the key of a location card ({@code "location-" + location}) or of an extra entry
     */
    public void selectCard(String key) {
        for (DiagramChoice choice : choices) {
            if (choice.key().equals(key)) {
                showCard(choice);
                return;
            }
        }
    }

    private void showCard(DiagramChoice choice) {
        cardLayout.show(panCards, choice.key());
        choosing = true;
        try {
            comboLocation.setSelectedItem(choice);
        } finally {
            choosing = false;
        }
        onCardShown(choice);
    }

    /**
     * @return the entry whose card is showing, or {@code null} before any card is
     */
    public @Nullable DiagramChoice getShownChoice() {
        return (comboLocation.getSelectedItem() instanceof DiagramChoice choice) ? choice : null;
    }

    /**
     * @return the unit shown
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Replaces the unit shown with a fresh copy of it - the server sends a new object after every change. The cards
     * were built for the unit's locations, so the copy must have the same number of them; a different unit needs a
     * new diagram.
     *
     * @param replacement the unit, as it now is
     *
     * @throws IllegalArgumentException if the replacement does not have the locations the cards were built for
     */
    public void setEntity(Entity replacement) {
        if (replacement.locations() != entity.locations()) {
            throw new IllegalArgumentException("The replacement has " + replacement.locations()
                  + " locations; the cards were built for " + entity.locations());
        }
        entity = replacement;
    }

    /**
     * @return the unit diagram
     */
    protected ArmorPanel getPaperdoll() {
        return paperdoll;
    }

    /**
     * @return the panel that holds the location cards, for a subclass that needs to size against it
     */
    protected JPanel getCardsPanel() {
        return panCards;
    }

    /**
     * Redraws the diagram from the unit. A subclass that shows something other than the unit as it is - pending
     * edits, say - overrides this.
     */
    public void refreshDiagram() {
        // The diagram builds its map sets when it is added to a displayable window, so there is nothing to draw
        // into before then.
        if (paperdoll.isDisplayable()) {
            paperdoll.displayMek(entity);
        }
    }

    /**
     * Enlarges the diagram to the height of its cards, within the screen, so it is not left small beside a tall
     * panel. Call after the containing window has been packed.
     */
    public void enlargeToFillDialog() {
        int drawnHeight = paperdoll.getPreferredSize().height;
        if (drawnHeight <= 0) {
            return;
        }
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        double panelScale = (double) panCards.getPreferredSize().height / drawnHeight;
        double screenScale = (screenHeight * MAX_SCREEN_FRACTION) / drawnHeight;
        double scale = Math.max(panelScale, MIN_PAPERDOLL_SCALE);
        scale = Math.min(scale, Math.min(MAX_PAPERDOLL_SCALE, screenScale));
        if (scale > 1.0) {
            paperdoll.setDisplayScale(scale);
        }
    }

    @Override
    public boolean selectsOnSingleClick() {
        return true;
    }

    @Override
    public void locationSelected(int location) {
        selectLocation(location);
    }

    /**
     * An entry of the chooser: a location of the unit, or an extra entry such as the crew. Equality is by key, so the
     * chooser can be set by key alone.
     *
     * @param key      the card's key in the card layout; {@code "location-" + location} for a location
     * @param name     what the chooser shows
     * @param location the location the card belongs to, or {@link Entity#LOC_NONE} for an extra entry
     */
    public record DiagramChoice(String key, String name, int location) {

        /**
         * @param key  the card's key
         * @param name what the chooser shows
         *
         * @return an entry that belongs to no location
         */
        public static DiagramChoice extra(String key, String name) {
            return new DiagramChoice(key, name, Entity.LOC_NONE);
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof DiagramChoice choice) && choice.key.equals(key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
