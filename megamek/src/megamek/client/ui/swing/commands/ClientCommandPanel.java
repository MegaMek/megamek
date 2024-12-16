/*
 * MegaMek - Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.commands;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.FlatLafStyleBuilder;
import megamek.common.Coords;
import megamek.common.annotations.Nullable;
import megamek.server.commands.ClientServerCommand;
import megamek.server.commands.arguments.*;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dialog for executing a client command.
 * @author Luana Coppio
 */
public class ClientCommandPanel extends JDialog {
    private final ClientServerCommand command;
    private final ClientGUI client;
    private final Coords coords;
    private int yPosition = 0;

    /**
     * Constructor for the dialog for executing a client command.
     *
     * @param parent    The parent frame.
     * @param client    The client GUI.
     * @param command   The command to render.
     */
    public ClientCommandPanel(JFrame parent, ClientGUI client, ClientServerCommand command, @Nullable Coords coords) {
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

        var title = new JLabel(command.getLongName());
        new FlatLafStyleBuilder().size(1.5).bold().apply(title);
        var gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(title, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yPosition++;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

        add(new JSeparator(), gridBagConstraints);

        var helpLabel = new JLabel(command.getHelpHtml());
        new FlatLafStyleBuilder().size(1).apply(helpLabel);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yPosition++;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.gridheight = 3;
        yPosition += 2;

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yPosition++;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.gridheight = 1;
        add(new JSeparator(), gridBagConstraints);

        add(helpLabel, gridBagConstraints);
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
        if (argument instanceof CoordXArgument intArg) {
            return getJSpinner(argument, createSpinner(intArg), 0);

        } else if (argument instanceof CoordYArgument intArg) {
            return getJSpinner(argument, createSpinner(intArg), 2);

        } else if (argument instanceof PlayerArgument playerArg) {
            return getStringJComboBox(argument.getName(), createPlayerComboBox(playerArg), argument.getHelp());

        } else if (argument instanceof UnitArgument unitArg) {
            return getStringJComboBox(argument.getName(), createUnitComboBox(unitArg), argument.getHelp());

        } else if (argument instanceof TeamArgument teamArg) {
            return getStringJComboBox(argument.getName(), createTeamsComboBox(teamArg), argument.getHelp());

        } else if (argument instanceof IntegerArgument intArg) {
            return getJSpinner(argument, createSpinner(intArg));

        } else if (argument instanceof OptionalIntegerArgument intArg) {
            return getJSpinner(argument, createSpinner(intArg));

        } else if (argument instanceof OptionalEnumArgument<?> enumArg) {
            return getStringJComboBox(argument.getName(), createOptionalEnumComboBox(enumArg), argument.getHelp());

        } else if (argument instanceof EnumArgument<?> enumArg) {
            return getStringJComboBox(argument.getName(), createEnumComboBox(enumArg), argument.getHelp());

        } else if (argument instanceof OptionalPasswordArgument) {
            return getJPasswordField(argument);

        } else if (argument instanceof StringArgument stringArg) {
            return getJTextField(argument.getName(), argument.getHelp(), stringArg.hasDefaultValue(), stringArg.getValue());

        } else if (argument instanceof OptionalStringArgument stringArg) {
            return getJTextField(argument.getName(), argument.getHelp(), stringArg.getValue().isPresent(), stringArg.getValue().get());

        } else if (argument instanceof BooleanArgument boolArg) {
            return getJCheckBox(argument, boolArg);
        }

        return null;
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
        var gridBagConstraints = getGridBagConstraints(startingX+1, yPosition);
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

    private JSpinner createSpinner(CoordXArgument coordX) {
        return new JSpinner(new SpinnerNumberModel(
            coords.getX()+1,
            0,
            1_000_000,
            1));
    }

    private JSpinner createSpinner(CoordYArgument coordY) {
        return new JSpinner(new SpinnerNumberModel(
            coords.getY()+1,
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

        var entitiesAtSpot = client.getClient().getGame().getEntities(coords);
        if (entitiesAtSpot.hasNext()) {
            var selectedEntity = entitiesAtSpot.next();
            comboBox.setSelectedItem(selectedEntity.getId() + ":" + selectedEntity.getDisplayName());
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
     * Execute the command with the given arguments.
     * It runs the command using the client chat, this way the command is sent to the server.
     * All arguments are loaded as named variables in the form of "argumentName=argumentValue".
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
                args[i] = argument.getName() + "=" + (((JCheckBox) component).isSelected() ? "true" : "false");
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
                    args[i] = argument.getName() + "=" + Objects.requireNonNull(((JComboBox<?>) component).getSelectedItem());
                }
            }
        }

        client.getClient().sendChat("/" + command.getName() + " " + String.join(" ", args));
    }
}
