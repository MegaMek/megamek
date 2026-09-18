/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.MapMenu;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.dialogs.unitDisplay.WeaponPanel;
import megamek.client.ui.panels.phaseDisplay.AbstractPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.ActionPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.AttackPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.DeploymentDisplay;
import megamek.client.ui.panels.phaseDisplay.FiringDisplay;
import megamek.client.ui.panels.phaseDisplay.PhysicalDisplay;
import megamek.client.ui.panels.phaseDisplay.StatusBarPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.TargetingPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.commands.MoveCommand;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.units.Entity;

/** Read-only descriptions of existing controls. Every execution rechecks current Swing/game state. */
final class GpuBoardActions {
    private static final Set<String> MOVEMENT_COMMANDS = java.util.Arrays.stream(MoveCommand.values())
          .map(MoveCommand::getCmd).collect(java.util.stream.Collectors.toUnmodifiableSet());
    private record Turn(JComponent panel, GamePhase phase, int index, int actor) { }

    private final BoardView view;
    private final Supplier<JComponent> panel;
    private final BooleanSupplier closed;
    private final Runnable changed;

    GpuBoardActions(BoardView view, Supplier<JComponent> panel, BooleanSupplier closed, Runnable changed) {
        this.view = view;
        this.panel = panel;
        this.closed = closed;
        this.changed = changed;
    }

    int actorId() {
        Entity actor = panel.get() instanceof ActionPhaseDisplay action ? action.currentEntity()
              : panel.get() instanceof DeploymentDisplay deployment ? deployment.currentEntity() : view.getSelectedEntity();
        return actor == null ? Entity.NONE : actor.getId();
    }

    private Turn turn() {
        return new Turn(panel.get(), view.game.getPhase(), view.game.getTurnIndex(), actorId());
    }

    private boolean current(Turn expected) {
        return !closed.getAsBoolean() && expected.equals(turn())
              && (!(expected.panel() instanceof AbstractPhaseDisplay phase) || !phase.isIgnoringEvents());
    }

    List<BoardScene.Command> phaseCommands() {
        Turn owner = turn();
        if (owner.panel() == null) {
            return List.of();
        }
        Set<AbstractButton> buttons = new LinkedHashSet<>();
        if (owner.panel() instanceof StatusBarPhaseDisplay phase) {
            buttons.addAll(phase.getActionButtons());
        }
        collectButtons(owner.panel(), buttons);
        List<BoardScene.Command> result = new ArrayList<>();
        for (AbstractButton button : buttons) {
            String id = Objects.toString(button.getActionCommand(), button.getText());
            if (id == null || id.toLowerCase(java.util.Locale.ROOT).endsWith("more")) {
                continue;
            }
            boolean completion = owner.panel() instanceof AbstractPhaseDisplay phase
                  && phase.getCompletionButtons().contains(button);
            result.add(describe(id, button, completion, List.of(), () -> {
                if (current(owner) && button.isEnabled() && available(owner.panel(), button)) {
                    button.doClick(0);
                }
            }));
        }
        if (owner.panel() instanceof StatusBarPhaseDisplay phase) {
            result.add(new BoardScene.Command("clear", Messages.getString("GpuBoard.clear"),
                  Messages.getString("GpuBoard.clearHelp"), phase.shouldReceiveKeyCommands(), false, List.of(),
                  dispatch(() -> {
                      if (current(owner) && phase.shouldReceiveKeyCommands()) {
                          phase.clear();
                      }
                  })));
        }
        BoardScene.Command weapons = weaponCommands(owner);
        if (weapons != null) {
            result.add(weapons);
        }
        return result;
    }

    BoardScene.Attack attackState() {
        Turn owner = turn();
        if (!(owner.panel() instanceof AttackPhaseDisplay attack) || owner.actor() == Entity.NONE) {
            return null;
        }
        WeaponPanel weapons = weaponPanel(owner);
        String target = Messages.getString("MekDisplay.NoTarget");
        if (weapons != null) {
            target = weapons.getTargetName();
        } else if (attack instanceof PhysicalDisplay physical && physical.getTarget() != null) {
            target = physical.getTarget().getDisplayName();
        }
        return new BoardScene.Attack(target, weapons == null ? "" : plainText(weapons.getWeaponSummary()),
              weapons == null ? "" : plainText(weapons.getFiringSolution()),
              weapons == null ? -1 : weapons.weaponList.getSelectedIndex(),
              attack.getAttackDescriptions().stream().map(GpuBoardActions::plainText).toList());
    }

    private WeaponPanel weaponPanel(Turn owner) {
        if (view.getClientgui() == null || !(owner.panel() instanceof FiringDisplay
              || owner.panel() instanceof TargetingPhaseDisplay)) {
            return null;
        }
        WeaponPanel weapons = view.getClientgui().getUnitDisplay().wPan;
        return weapons.getSelectedEntityId() == owner.actor() && owner.actor() != Entity.NONE ? weapons : null;
    }

    private BoardScene.Command weaponCommands(Turn owner) {
        WeaponPanel weapons = weaponPanel(owner);
        if (weapons == null) {
            return null;
        }
        var model = weapons.weaponList.getModel();
        List<BoardScene.Command> choices = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            int index = i;
            String label = model.getElementAt(index);
            boolean selected = weapons.weaponList.getSelectedIndex() == index;
            choices.add(new BoardScene.Command("weapon:" + index, (selected ? "* " : "") + plainText(label),
                  selected ? plainText(weapons.getTargetSummary()) : "", weapons.weaponList.isEnabled(), false,
                  List.of(), dispatch(() -> {
                      if (current(owner) && weapons.weaponList.isEnabled() && weapons.getSelectedEntityId() == owner.actor()
                            && weapons.weaponList.getModel() == model && index < model.getSize()
                            && Objects.equals(label, model.getElementAt(index))) {
                          weapons.weaponList.setSelectedIndex(index);
                      }
                  })));
        }
        addAmmoChoices(choices, weapons.getAmmoSelector(), "Ammunition", owner, weapons);
        addAmmoChoices(choices, weapons.m_chBayWeapon, "Bay weapon", owner, weapons);
        return new BoardScene.Command("weapons", "Weapons and ammunition", plainText(weapons.getTargetSummary()),
              true, false, choices, () -> { });
    }

    private void addAmmoChoices(List<BoardScene.Command> choices, JComboBox<String> selector, String title, Turn owner,
          WeaponPanel weapons) {
        if (!selector.isVisible()) {
            return;
        }
        List<BoardScene.Command> items = new ArrayList<>();
        var model = selector.getModel();
        var selectedWeapon = weapons.getSelectedWeapon();
        int selectedIndex = weapons.weaponList.getSelectedIndex();
        for (int i = 0; i < model.getSize(); i++) {
            int index = i;
            String label = model.getElementAt(index);
            items.add(new BoardScene.Command(title + ":" + index,
                  (selector.getSelectedIndex() == index ? "* " : "") + plainText(label), "", selector.isEnabled(),
                  false, List.of(), dispatch(() -> {
                      if (current(owner) && selector.isEnabled() && selector.isVisible() && selector.getModel() == model
                            && weapons.getSelectedEntityId() == owner.actor()
                            && weapons.weaponList.getSelectedIndex() == selectedIndex
                            && weapons.getSelectedWeapon() == selectedWeapon
                            && index < model.getSize() && Objects.equals(label, model.getElementAt(index))) {
                          selector.setSelectedIndex(index);
                      }
                  })));
        }
        if (!items.isEmpty()) {
            choices.add(new BoardScene.Command(title, title, "", selector.isEnabled(), false, items, () -> { }));
        }
    }

    private static boolean available(JComponent owner, AbstractButton button) {
        return owner instanceof StatusBarPhaseDisplay phase && phase.getActionButtons().contains(button)
              || button.isVisible() && SwingUtilities.isDescendingFrom(button, owner);
    }

    private static void collectButtons(Container container, Set<AbstractButton> result) {
        for (Component child : container.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            if (child instanceof AbstractButton button && button.getText() != null && !button.getText().isBlank()) {
                result.add(button);
            } else if (child instanceof Container nested) {
                collectButtons(nested, result);
            }
        }
    }

    List<BoardScene.Command> contextCommands(Coords coords) {
        if (coords == null || !view.getBoard().contains(coords)) {
            return List.of();
        }
        Turn owner = turn();
        List<BoardScene.Command> result = new ArrayList<>();
        boolean canUse = owner.phase().isOnMap()
              && (view.getClientgui() == null || view.getClientgui().getClient().isMyTurn());
        String hexAction = switch (owner.phase()) {
            case MOVEMENT -> "Plot movement here";
            case DEPLOYMENT -> "Deploy here";
            case FIRING, PHYSICAL, TARGETING, OFFBOARD -> "Choose target here";
            default -> "Select this hex";
        };
        result.add(new BoardScene.Command("board.useHex", hexAction, "Use the current phase tool at this location.",
              canUse, false, !(owner.panel() instanceof AttackPhaseDisplay), List.of(), dispatch(() -> {
                  if (current(owner) && owner.phase().isOnMap()
                        && (view.getClientgui() == null || view.getClientgui().getClient().isMyTurn())) {
                      view.mouseAction(coords, BoardView.BOARD_HEX_DRAG, java.awt.event.InputEvent.BUTTON1_DOWN_MASK, 1);
                      view.mouseAction(coords, BoardView.BOARD_HEX_CLICK, 0, 1);
                  }
              })));
        result.add(new BoardScene.Command("board.los", "Measure line of sight", "Choose the start and end hexes.",
              true, false, List.of(), dispatch(() -> {
                  if (current(owner)) {
                      view.mouseAction(coords, BoardView.BOARD_HEX_CLICK, java.awt.event.InputEvent.CTRL_DOWN_MASK, 1);
                  }
              })));
        if (view.getClientgui() != null) {
            result.addAll(menuCommands(new MapMenu(coords, view.getBoardId(), owner.panel(), view.getClientgui()),
                  owner, coords, List.of()));
            BoardScene.Command weapons = weaponCommands(owner);
            if (weapons != null) {
                result.add(weapons);
            }
        }
        return result;
    }

    List<BoardScene.Command> globalCommands() {
        return view.getClientgui() == null || view.getClientgui().getMenuBar() == null ? List.of()
              : menuCommands(view.getClientgui().getMenuBar(), turn(), null, List.of());
    }

    private List<BoardScene.Command> menuCommands(Container menu, Turn owner, Coords coords, List<String> parents) {
        List<BoardScene.Command> result = new ArrayList<>();
        for (Component component : menu.getComponents()) {
            if (!(component instanceof JMenuItem item) || !item.isVisible()) {
                continue;
            }
            String key = menuKey(item);
            List<String> path = new ArrayList<>(parents);
            path.add(key);
            List<BoardScene.Command> children = item instanceof JMenu group
                  ? menuCommands(group.getPopupMenu(), owner, coords, path) : List.of();
            result.add(describe(String.join("/", path), item, false, children, () -> {
                if (coords != null && !current(owner)) {
                    return;
                }
                // Rebuild contextual choices to check visibility, targets and availability at execution time.
                Container fresh = coords == null ? view.getClientgui().getMenuBar()
                      : new MapMenu(coords, view.getBoardId(), owner.panel(), view.getClientgui());
                JMenuItem action = findItem(fresh, path);
                if (action != null && action.isEnabled()) {
                    action.doClick(0);
                }
            }));
        }
        return result;
    }

    private static String menuKey(JMenuItem item) {
        return Objects.toString(item.getActionCommand(), "") + ":" + item.getText();
    }

    private static JMenuItem findItem(Container menu, List<String> path) {
        for (Component component : menu.getComponents()) {
            if (component instanceof JMenuItem item && item.isVisible() && item.isEnabled()
                  && menuKey(item).equals(path.getFirst())) {
                if (path.size() == 1) {
                    return item;
                }
                return item instanceof JMenu group ? findItem(group.getPopupMenu(), path.subList(1, path.size())) : null;
            }
        }
        return null;
    }

    private BoardScene.Command describe(String id, AbstractButton button, boolean commit,
          List<BoardScene.Command> children, Runnable action) {
        return new BoardScene.Command(id, plainText(button.getText()), plainText(button.getToolTipText()),
              button.isEnabled(), commit, MOVEMENT_COMMANDS.contains(button.getActionCommand())
                    || Set.of("fireTwist", "fireStrafe").contains(button.getActionCommand()), children, dispatch(action));
    }

    private Runnable dispatch(Runnable action) {
        return () -> SwingUtilities.invokeLater(() -> {
            if (!closed.getAsBoolean()) {
                action.run();
                changed.run();
            }
        });
    }

    static String plainText(String text) {
        return text == null ? "" : text.replaceAll("(?is)<head>.*?</head>", "")
              .replaceAll("(?i)<(?:br\\s*/?|/tr|/p|/div)>", "\n")
              .replaceAll("(?i)</t[dh]>", "  ").replaceAll("<[^>]*>", "")
              .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").trim();
    }
}
