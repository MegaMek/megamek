/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

/** A native attack console. Its only inputs are presentation snapshots and the existing phase commands. */
final class GpuAttackPanel {
    static final int WIDTH = 362;
    private static final Set<String> QUICK_ACTIONS = Set.of("fireNextTarg", "fireTwist", "fireMode");
    private static final Color AMBER = Color.valueOf("D8BC82");
    private final Skin skin;
    private final Consumer<BoardScene.Command> execute;
    private final Consumer<String> openWeapons;
    private final Table panel = new Table();
    private final Table content = new Table();
    private final Table weapons = new Table();
    private final Table ammunition = new Table();
    private final Table controls = new Table();
    private final Table firing = new Table();
    private final ScrollPane scroll;
    private final ScrollPane weaponScroll;
    private final Label target;
    private final Label solution;
    private final Label statistics;
    private final Label orders;
    private final TextTooltip orderTooltip;
    private final TextButton.TextButtonStyle rowStyle;
    private final TextButton.TextButtonStyle controlStyle;
    private List<BoardScene.Command> commands = List.of();
    private List<String> signature = List.of();
    private int selectedWeapon = -1;
    private int actorId = -1;

    GpuAttackPanel(Skin skin, Consumer<BoardScene.Command> execute, Consumer<String> openWeapons,
          Runnable moreControls, Runnable reviewOrders) {
        this.skin = skin;
        this.execute = execute;
        this.openWeapons = openWeapons;
        panel.setName("attack-panel");
        panel.setBackground(skin.getDrawable("panel"));
        panel.setTouchable(Touchable.enabled);
        panel.pad(12).top();

        // Flat instrument rows keep the list quiet; reserve the authored metal button for firing.
        rowStyle = new TextButton.TextButtonStyle(skin.get("toolbar", TextButton.TextButtonStyle.class));
        rowStyle.up = skin.newDrawable("white", Color.valueOf("1C2326"));
        rowStyle.over = skin.newDrawable("white", Color.valueOf("303D41"));
        rowStyle.down = skin.newDrawable("white", Color.valueOf("405052"));
        rowStyle.checked = skin.newDrawable("white", Color.valueOf("364748"));
        rowStyle.checkedOver = rowStyle.checked;
        rowStyle.disabled = rowStyle.up;
        controlStyle = new TextButton.TextButtonStyle(rowStyle);
        controlStyle.up = skin.newDrawable("white", Color.valueOf("252D30"));

        Table heading = new Table();
        Image reticle = new Image(skin.getDrawable("icon-target"));
        reticle.setColor(AMBER);
        heading.add(reticle).size(18).padRight(9);
        heading.add(new Label("ATTACK CONTROL", skin, "kicker")).left();
        heading.add().growX();
        heading.add(button("attack-more", "Controls  >", controlStyle, moreControls)).width(82).height(26);
        panel.add(heading).growX().row();
        panel.add(rule()).growX().height(1).padTop(6).padBottom(8).row();
        target = new Label("", new Label.LabelStyle(skin.getFont("bold-font"), GpuBoardSkin.TEXT));
        target.setName("attack-target");
        target.setEllipsis(true);
        panel.add(target).minWidth(0).growX().padBottom(8).row();

        content.top().defaults().growX();
        content.setBackground(skin.newDrawable("white", Color.valueOf("141B1E")));
        content.pad(6);
        solution = text("attack-solution");
        content.add(solution).padBottom(6).row();
        content.add(controls).padBottom(8).row();
        statistics = text("attack-statistics");
        content.add(statistics).padBottom(8).row();
        Table weaponHeading = new Table();
        weaponHeading.add(new Label("WEAPONS", skin, "kicker")).left();
        weaponHeading.add().growX();
        weaponHeading.add(new Label("SELECT TO AIM", skin, "small")).right();
        weaponHeading.setName("attack-weapon-heading");
        content.add(weaponHeading).padBottom(6).row();
        weapons.top();
        weaponScroll = new ScrollPane(weapons, scrollStyle());
        configureScroll(weaponScroll, "attack-weapons");
        content.add(weaponScroll).height(90).row();
        scroll = new ScrollPane(content, scrollStyle());
        configureScroll(scroll, "attack-scroll");
        panel.add(scroll).minHeight(0).grow().row();
        panel.add(ammunition).growX().row();
        TextButton review = button("attack-review-orders", "", rowStyle, reviewOrders);
        orders = review.getLabel();
        orders.setName("attack-orders");
        orders.setWrap(false);
        orders.setEllipsis(true);
        orders.setAlignment(Align.left);
        orderTooltip = new TextTooltip("", skin);
        review.addListener(orderTooltip);
        panel.add(review).growX().height(26).padTop(6).row();
        panel.add(firing).growX().padTop(8);
        panel.setVisible(false);
    }

    private static void configureScroll(ScrollPane pane, String name) {
        pane.setName(name);
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);
        pane.setFlickScroll(false);
    }

    private ScrollPane.ScrollPaneStyle scrollStyle() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        style.vScroll = skin.newDrawable("white", Color.valueOf("1C2427"));
        style.vScrollKnob = skin.newDrawable("white", Color.valueOf("607375"));
        style.vScroll.setMinWidth(3);
        style.vScrollKnob.setMinWidth(3);
        style.vScrollKnob.setMinHeight(24);
        return style;
    }

    private Image rule() {
        return new Image(skin.getDrawable("rule"));
    }

    private Label text(String name) {
        Label label = new Label("", skin, "small");
        label.setName(name);
        label.setWrap(true);
        return label;
    }

    private TextButton button(String name, String title, TextButton.TextButtonStyle style, Runnable action) {
        TextButton button = new TextButton(title, style);
        button.setName(name);
        button.setProgrammaticChangeEvents(false);
        button.getLabel().setWrap(true);
        button.pad(6, 10, 6, 10);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                button.setChecked(false);
                action.run();
            }
        });
        return button;
    }

    Table panel() {
        return panel;
    }

    void resize(float width, float height) {
        panel.setBounds(width - WIDTH - 12, GpuBoardUi.TURN_HEIGHT + 12, WIDTH,
              Math.max(1, height - GpuBoardUi.TOP_HEIGHT - GpuBoardUi.TURN_HEIGHT - 24));
    }

    void update(GpuBoardSource.Frame frame) {
        BoardScene.Attack attack = frame.attack();
        panel.setVisible(attack != null);
        commands = frame.scene().commands();
        if (attack == null) {
            actorId = -1;
            return;
        }
        boolean newActor = actorId != frame.scene().selectedId();
        actorId = frame.scene().selectedId();
        target.setText(attack.targetName());
        solution.setText(compact(attack.targetDetails()));
        statistics.setText(compact(attack.weaponDetails()));
        orders.setText(attack.orders().isEmpty() ? "No attacks queued."
              : "Queued: " + compact(attack.orders().getFirst()).replace('\n', ' '));
        orderTooltip.getActor().setText(attack.orders().isEmpty() ? "No attacks queued."
              : String.join("\n\n", attack.orders()));

        List<String> next = new ArrayList<>();
        describe(commands, next);
        next.add(Integer.toString(attack.selectedWeapon()));
        if (!next.equals(signature) || newActor) {
            signature = List.copyOf(next);
            rebuild(attack);
        }
        for (Actor row : weapons.getChildren()) {
            ((TextButton) row).setChecked(row.getName().equals("attack:weapon:" + attack.selectedWeapon()));
        }
        panel.validate();
        boolean listResized = false;
        if (weaponScroll.isVisible()) {
            // Give the weapon list the remaining space, keeping ammo and queued orders in reach on small windows.
            float otherHeight = content.getPrefHeight() - weaponScroll.getHeight();
            float height = Math.max(48, scroll.getHeight() - otherHeight);
            if (Math.abs(weaponScroll.getHeight() - height) > 0.5f) {
                content.getCell(weaponScroll).height(height);
                content.invalidateHierarchy();
                panel.validate();
                listResized = true;
            }
        }
        if (newActor) {
            scroll.setScrollY(0);
        }
        if (selectedWeapon != attack.selectedWeapon() || newActor || listResized) {
            weaponScroll.validate();
            weapons.validate();
            Actor selected = weapons.findActor("attack:weapon:" + attack.selectedWeapon());
            if (selected != null) {
                weaponScroll.setScrollY(weapons.getHeight() - selected.getY()
                      - (weaponScroll.getScrollHeight() + selected.getHeight()) / 2);
                weaponScroll.updateVisualScroll();
            }
            selectedWeapon = attack.selectedWeapon();
        }
    }

    private static String compact(String text) {
        return text.replaceAll("[\\t ]+", " ").replaceAll(" *\\n\\s*", "\n").strip();
    }

    private static void describe(List<BoardScene.Command> commands, List<String> result) {
        for (BoardScene.Command command : commands) {
            result.add(command.id() + command.label() + command.enabled() + command.detail());
            describe(command.children(), result);
        }
    }

    private void rebuild(BoardScene.Attack attack) {
        weapons.clearChildren();
        ammunition.clearChildren();
        controls.clearChildren();
        firing.clearChildren();
        BoardScene.Command choices = GpuBoardUi.find(commands, "weapons");
        boolean hasWeapons = choices != null;
        content.findActor("attack-weapon-heading").setVisible(hasWeapons);
        content.getCell(content.findActor("attack-weapon-heading")).height(hasWeapons ? 18 : 0).padBottom(hasWeapons ? 6 : 0);
        weaponScroll.setVisible(hasWeapons);
        content.getCell(weaponScroll).height(hasWeapons ? 90 : 0);
        if (hasWeapons) {
            for (BoardScene.Command choice : choices.children()) {
                if (choice.children().isEmpty()) {
                    TextButton row = commandButton(choice, rowStyle);
                    row.getLabel().setAlignment(Align.left);
                    row.setChecked(choice.id().equals("weapon:" + attack.selectedWeapon()));
                    weapons.add(row).growX().minHeight(28).padBottom(1).row();
                } else {
                    String selected = choice.children().stream().map(BoardScene.Command::label)
                          .filter(label -> label.startsWith("* ")).map(label -> label.substring(2)).findFirst().orElse("—");
                    TextButton selector = button("attack:" + choice.id(), choice.label() + ": " + selected + "  >",
                          controlStyle, () -> openWeapons.accept(choice.id()));
                    selector.setDisabled(!choice.enabled());
                    ammunition.add(selector).growX().minHeight(32).padBottom(3).row();
                }
            }
        }
        int count = 0;
        for (BoardScene.Command command : commands) {
            if (command.commit() || !command.children().isEmpty() || command.id().equals("clear")) {
                continue;
            }
            if (command.id().equals("fireFire") || command.id().equals("fireSkip")) {
                TextButton fire = commandButton(command, skin.get("toolbar", TextButton.TextButtonStyle.class));
                if (command.id().equals("fireFire")) {
                    fire.getLabel().setColor(AMBER);
                }
                firing.add(fire).growX().height(44).padRight(4);
            } else if (!hasWeapons || QUICK_ACTIONS.contains(command.id())) {
                controls.add(commandButton(command, controlStyle)).growX().uniformX().minHeight(28).pad(1);
                if (++count % (hasWeapons ? 3 : 2) == 0) {
                    controls.row();
                }
            }
        }
    }

    private TextButton commandButton(BoardScene.Command command, TextButton.TextButtonStyle style) {
        String label = command.label().startsWith("* ") ? command.label().substring(2) : command.label();
        if (command.id().equals("fireFire")) {
            label = "Fire weapon";
        } else if (command.id().equals("fireSkip")) {
            label = "Next weapon";
        }
        TextButton button = button("attack:" + command.id(), label, style, () -> {
            BoardScene.Command current = GpuBoardUi.find(commands, command.id());
            if (current != null && current.enabled()) {
                execute.accept(current);
            }
        });
        button.setDisabled(!command.enabled());
        if (!command.detail().isBlank()) {
            button.addListener(new TextTooltip(command.detail(), skin));
        }
        return button;
    }

}
