/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.*;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.FlatLafStyleBuilder;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.server.commands.ClientServerCommand;
import megamek.server.commands.arguments.*;

/**
 * Dialog for executing a client command.
 *
 * @author Luana Coppio
 */
public class ClientCommandDialog extends JDialog {

    /** Where a hex spinner starts when the dialog was opened without a hex; board coordinates are one-based. */
    private static final int FIRST_BOARD_COORDINATE = 1;

    /** How many columns the dialog's grid holds, so a full-width row can span all of them. */
    private static final int DIALOG_COLUMNS = 4;

    /** The width the help text wraps to, before the GUI scale is applied. */
    private static final int HELP_TEXT_WIDTH = 460;

    private final ClientServerCommand command;
    private final ClientGUI client;
    /** The hex the dialog acts on, or {@code null} when it was opened from a menu that had no hex to offer. */
    private final Coords coords;
    private int yPosition = 0;

    /**
     * Constructor for the dialog for executing a client command.
     *
     * @param parent  The parent frame.
     * @param client  The client GUI.
     * @param command The command to render.
     * @param coords  The hex the command should start on, or {@code null} when the dialog was opened without a hex.
     *                Hex spinners then start at the first board coordinate and no unit is preselected.
     */
    public ClientCommandDialog(JFrame parent, ClientGUI client, ClientServerCommand command, @Nullable Coords coords) {
        super(parent, command.getLongName() + " /" + command.getName(), true);
        this.command = command;
        this.client = client;
        this.coords = coords;
        initializeUI(parent);
    }

    private void initializeUI(JFrame parent) {
        setLayout(new GridBagLayout());
        addTitleAndDescription();
        Map<String, JComponent> argumentComponents = addArgumentComponents();
        addExecuteButton(argumentComponents);
        pack();
        setLocationRelativeTo(parent);
    }

    private void addTitleAndDescription() {
        JLabel title = new JLabel(command.getLongName());
        new FlatLafStyleBuilder().size(1.5).bold().apply(title);
        add(title, fullWidthRow());

        addTargetHexLabel();

        add(new JSeparator(), fullWidthRow());

        JLabel helpLabel = new JLabel(helpWrappedToDialogWidth());
        new FlatLafStyleBuilder().size(1).apply(helpLabel);
        add(helpLabel, fullWidthRow());

        add(new JSeparator(), fullWidthRow());
    }

    /**
     * Names the hex the command was opened on, so a gamemaster can see at a glance which hex they are about to change
     * without reading it back out of the coordinate spinners. Nothing is shown when the dialog was opened without a
     * hex, because then there is no hex to name.
     */
    private void addTargetHexLabel() {
        if (coords == null) {
            return;
        }
        JLabel targetLabel = new JLabel(Messages.getString("Gamemaster.cmd.targetHex", coords.getBoardNum()));
        new FlatLafStyleBuilder().size(1).bold().apply(targetLabel);
        add(targetLabel, fullWidthRow());
    }

    /**
     * Builds the constraints for one full-width row of the dialog and moves on to the next row. Every row is built
     * from its own constraints; sharing one between two components put them in the same cell, which is what squeezed
     * the help text into a narrow column down the middle of the dialog.
     *
     * @return The constraints for the next row
     */
    private GridBagConstraints fullWidthRow() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = yPosition++;
        constraints.gridwidth = DIALOG_COLUMNS;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(2, 6, 2, 6);
        return constraints;
    }

    /**
     * The command's help text, held to a readable width. An HTML label with no width to work to lays itself out at
     * whatever width the layout hands it, which for a long help text is a tall thin column of one or two words a line.
     *
     * @return The help text as HTML, wrapped to the dialog's text width
     */
    private String helpWrappedToDialogWidth() {
        String widthStyle = "<html><body style='width: " + UIUtil.scaleForGUI(HELP_TEXT_WIDTH) + "px'>";
        return command.getHelpHtml().replaceFirst("(?i)^<html>", widthStyle);
    }

    private Map<String, JComponent> addArgumentComponents() {
        List<Argument<?>> arguments = command.defineArguments();
        Map<String, JComponent> argumentComponents = new HashMap<>();

        for (Argument<?> argument : arguments) {
            argumentComponents.put(argument.getName(), getArgumentComponent(argument));
        }
        return argumentComponents;
    }

    private JComponent getArgumentComponent(Argument<?> argument) {
        JComponent component = null;
        if (argument instanceof CoordXArgument coordXArgument) {
            JSpinner spinner = createSpinner(coordXArgument);
            // The hex is already settled by the right-click that opened the dialog and is named at the top of it, so
            // asking for it again is a coordinate to get wrong. The spinner is still built, holding the clicked hex,
            // because the command is assembled from these components; it is simply not shown.
            return (coords == null) ? getJSpinner(argument, spinner, 0) : spinner;

        } else if (argument instanceof CoordYArgument coordYArgument) {
            JSpinner spinner = createSpinner(coordYArgument);
            component = (coords == null) ? getJSpinner(argument, spinner, 2) : spinner;

        } else if (argument instanceof PlayerArgument playerArg) {
            component = getStringJComboBox(argument.getName(), createPlayerComboBox(playerArg), argument.getHelp());

        } else if (argument instanceof UnitArgument unitArg) {
            component = getStringJComboBox(argument.getName(), createUnitComboBox(unitArg), argument.getHelp());

        } else if (argument instanceof TeamArgument teamArg) {
            component = getStringJComboBox(argument.getName(), createTeamsComboBox(teamArg), argument.getHelp());

        } else if (argument instanceof IntegerArgument intArg) {
            component = getJSpinner(argument, createSpinner(intArg));

        } else if (argument instanceof OptionalIntegerArgument intArg) {
            component = getJSpinner(argument, createSpinner(intArg));

        } else if (argument instanceof OptionalEnumArgument<?> enumArg) {
            component = getStringJComboBox(argument.getName(), createOptionalEnumComboBox(enumArg), argument.getHelp());

        } else if (argument instanceof EnumArgument<?> enumArg) {
            component = getStringJComboBox(argument.getName(), createEnumComboBox(enumArg), argument.getHelp());

        } else if (argument instanceof OptionalPasswordArgument) {
            component = getJPasswordField(argument);

        } else if (argument instanceof StringArgument stringArg) {
            component = getJTextField(argument.getName(),
                  argument.getHelp(),
                  stringArg.hasDefaultValue(),
                  stringArg.getValue());

        } else if (argument instanceof OptionalStringArgument stringArg) {
            component = getJTextField(argument.getName(),
                  argument.getHelp(),
                  stringArg.getValue().isPresent(),
                  stringArg.getValue().get());

        } else if (argument instanceof BooleanArgument boolArg) {
            component = getJCheckBox(argument, boolArg);
        }
        yPosition++;
        return component;
    }

    private JCheckBox getJCheckBox(Argument<?> argument, BooleanArgument boolArg) {
        JLabel label = new JLabel(argument.getName() + ":");
        var labelConstraintBag = getGridBagConstraints(0, yPosition);
        add(label, labelConstraintBag);
        JCheckBox checkBox = new JCheckBox();
        checkBox.setToolTipText(argument.getHelp());
        if (boolArg.hasDefaultValue()) {
            checkBox.setSelected(boolArg.getValue());
        }
        var gridBagConstraints = getGridBagConstraints(1, yPosition++);
        add(checkBox, gridBagConstraints);
        return checkBox;
    }

    private JTextField getJTextField(String argument, String argument1, boolean stringArg, String stringArg1) {
        JLabel label = new JLabel(argument + ":");
        var labelConstraintBag = getGridBagConstraints(0, yPosition);
        add(label, labelConstraintBag);
        JTextField textField = new JTextField();
        textField.setToolTipText(argument1);
        if (stringArg) {
            textField.setText(stringArg1);
        }
        var gridBagConstraints = getGridBagConstraints(1, yPosition++);
        add(textField, gridBagConstraints);
        return textField;
    }

    private JPasswordField getJPasswordField(Argument<?> argument) {
        JLabel label = new JLabel(argument.getName() + ":");
        var labelConstraintBag = getGridBagConstraints(0, yPosition);
        add(label, labelConstraintBag);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setToolTipText(argument.getHelp());
        var gridBagConstraints = getGridBagConstraints(1, yPosition++);
        add(passwordField, gridBagConstraints);
        return passwordField;
    }

    private JComboBox<String> getStringJComboBox(String argument, JComboBox<String> playerArg, String tooltipText) {
        JLabel label = new JLabel(argument + ":");
        var labelConstraintBag = getGridBagConstraints(0, yPosition);
        add(label, labelConstraintBag);
        playerArg.setToolTipText(tooltipText);
        var gridBagConstraints = getGridBagConstraints(1, yPosition++);
        add(playerArg, gridBagConstraints);
        return playerArg;
    }

    private JSpinner getJSpinner(Argument<?> argument, JSpinner spinner) {
        return getJSpinner(argument, spinner, 0);
    }

    private JSpinner getJSpinner(Argument<?> argument, JSpinner spinner, int startingX) {
        JLabel label = new JLabel(argument.getName() + ":");
        var labelConstraintBag = getGridBagConstraints(startingX, yPosition);
        add(label, labelConstraintBag);

        spinner.setToolTipText(argument.getHelp());
        var gridBagConstraints = getGridBagConstraints(startingX + 1, yPosition);
        add(spinner, gridBagConstraints);

        return spinner;
    }

    private GridBagConstraints getGridBagConstraints(int x, int y) {
        var gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = x;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        return gridBagConstraints;
    }

    private JSpinner createSpinner(OptionalIntegerArgument intArg) {
        return new JSpinner(new SpinnerNumberModel(
              Math.max(intArg.getMinValue(), 0),
              intArg.getMinValue(),
              intArg.getMaxValue(),
              1));
    }

    /**
     * A spinner for a hex X coordinate, starting on the hex the dialog was opened from. Board coordinates are
     * displayed one-based, so the stored value is offset by one for display.
     *
     * <p>The dialog can be opened without a hex, from a menu that has no board position to offer. The spinner then
     * starts at the first column and the gamemaster types the hex they want.</p>
     */
    private JSpinner createSpinner(CoordXArgument coordX) {
        int startingColumn = (coords == null) ? FIRST_BOARD_COORDINATE : coords.getX() + 1;
        return new JSpinner(new SpinnerNumberModel(
              startingColumn,
              0,
              1_000_000,
              1));
    }

    /**
     * A spinner for a hex Y coordinate, starting on the hex the dialog was opened from. Board coordinates are
     * displayed one-based, so the stored value is offset by one for display.
     *
     * <p>The dialog can be opened without a hex, from a menu that has no board position to offer. The spinner then
     * starts at the first row and the gamemaster types the hex they want.</p>
     */
    private JSpinner createSpinner(CoordYArgument coordY) {
        int startingRow = (coords == null) ? FIRST_BOARD_COORDINATE : coords.getY() + 1;
        return new JSpinner(new SpinnerNumberModel(
              startingRow,
              0,
              1_000_000,
              1));
    }

    private JSpinner createSpinner(IntegerArgument intArg) {
        return new JSpinner(new SpinnerNumberModel(
              intArg.hasDefaultValue() ? intArg.getValue() : 0,
              intArg.getMinValue(),
              intArg.getMaxValue(),
              1));
    }

    private JComboBox<String> createPlayerComboBox(PlayerArgument playerArgument) {
        JComboBox<String> comboBox = new JComboBox<>();
        var players = client.getClient().getGame().getPlayersList();
        for (var player : players) {
            comboBox.addItem(player.getId() + ":" + player.getName());
        }

        return comboBox;
    }

    private JComboBox<String> createUnitComboBox(UnitArgument unitArgument) {
        JComboBox<String> comboBox = new JComboBox<>();
        var entities = client.getClient().getGame().getEntitiesVector();
        for (var entity : entities) {
            comboBox.addItem(entity.getId() + ":" + entity.getDisplayName());
        }

        // Opened from a hex, the unit standing there is the one the gamemaster means, so it starts selected. Opened
        // without a hex there is nothing to preselect, and the full list is left for them to pick from.
        if (coords != null) {
            var entitiesAtSpot = client.getClient().getGame().getEntities(coords);
            if (entitiesAtSpot.hasNext()) {
                var selectedEntity = entitiesAtSpot.next();
                comboBox.setSelectedItem(selectedEntity.getId() + ":" + selectedEntity.getDisplayName());
            }
        }

        return comboBox;
    }

    private JComboBox<String> createTeamsComboBox(TeamArgument teamArgument) {
        JComboBox<String> comboBox = new JComboBox<>();
        var teams = client.getClient().getGame().getTeams();
        for (var team : teams) {
            comboBox.addItem(team.getId() + "");
        }

        return comboBox;
    }

    private JComboBox<String> createOptionalEnumComboBox(OptionalEnumArgument<?> enumArg) {
        JComboBox<String> comboBox = new JComboBox<>();
        if (enumArg.getValue() == null) {
            comboBox.addItem("-");
            comboBox.setSelectedItem("-");
        }
        for (var arg : enumArg.getEnumType().getEnumConstants()) {
            comboBox.addItem(arg.ordinal() + ": " + arg);
        }
        if (enumArg.getValue() != null) {
            comboBox.setSelectedItem(enumArg.getValue().ordinal() + ": " + enumArg.getValue().toString());
        }
        return comboBox;
    }

    private JComboBox<String> createEnumComboBox(EnumArgument<?> enumArg) {
        JComboBox<String> comboBox = new JComboBox<>();
        for (Enum<?> constant : enumArg.getEnumType().getEnumConstants()) {
            comboBox.addItem(constant.name());
        }
        if (enumArg.getValue() != null) {
            comboBox.setSelectedItem(enumArg.getValue().name());
        }
        return comboBox;
    }

    private void addExecuteButton(Map<String, JComponent> argumentComponents) {
        var gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
        add(getExecuteButton(argumentComponents), gridBagConstraints);
    }

    private JButton getExecuteButton(Map<String, JComponent> argumentComponents) {
        JButton executeButton = new JButton("Execute");
        executeButton.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(
                  this,
                  "Are you sure you want to execute this command?",
                  "Execute?",
                  JOptionPane.YES_NO_OPTION
            );
            if (response == JOptionPane.YES_OPTION) {
                executeCommand(argumentComponents);
            }
        });
        return executeButton;
    }

    /**
     * Execute the command with the given arguments. It runs the command using the client chat, this way the command is
     * sent to the server. All arguments are loaded as named variables in the form of "argumentName=argumentValue".
     *
     * @param argumentComponents The components that hold the arguments selected.
     */
    private void executeCommand(Map<String, JComponent> argumentComponents) {
        List<Argument<?>> arguments = command.defineArguments();
        String[] args = new String[arguments.size()];

        for (int i = 0; i < arguments.size(); i++) {
            Argument<?> argument = arguments.get(i);
            JComponent component = argumentComponents.get(argument.getName());

            if (component instanceof JSpinner) {
                args[i] = argument.getName() + "=" + ((JSpinner) component).getValue().toString();
            } else if (component instanceof JPasswordField) {
                args[i] = argument.getName() + "=" + new String(((JPasswordField) component).getPassword());
            } else if (component instanceof JTextField) {
                args[i] = argument.getName() + "=" + ((JTextField) component).getText();
            } else if (component instanceof JCheckBox) {
                args[i] = argument.getName() + "=" + (((JCheckBox) component).isSelected());
            } else if (component instanceof JComboBox) {
                if (argument instanceof OptionalEnumArgument<?>) {
                    String selectedItem = (String) ((JComboBox<?>) component).getSelectedItem();
                    if (selectedItem == null || selectedItem.equals("-")) {
                        // If it is null we just set it to an empty string and move on
                        args[i] = "";
                        continue;
                    }
                    var selectedItemValue = selectedItem.split(":")[0].trim();
                    args[i] = argument.getName() + "=" + selectedItemValue;
                } else if (
                      (argument instanceof PlayerArgument) ||
                            (argument instanceof UnitArgument) ||
                            (argument instanceof TeamArgument)) {

                    String selectedItem = (String) ((JComboBox<?>) component).getSelectedItem();
                    if (selectedItem == null || selectedItem.equals("-")) {
                        // If it is null we just set it to an empty string and move on
                        args[i] = "";
                        continue;
                    }
                    var selectedItemValue = selectedItem.split(":")[0].trim();
                    args[i] = argument.getName() + "=" + selectedItemValue;
                } else {
                    args[i] = argument.getName()
                          + "="
                          + Objects.requireNonNull(((JComboBox<?>) component).getSelectedItem());
                }
            }
        }

        client.getClient().sendChat("/" + command.getName() + " " + String.join(" ", args));
        // the command is on its way and the dialog has nothing more to say; leaving it open over the board only hides
        // the result the gamemaster wants to see
        dispose();
    }
}
