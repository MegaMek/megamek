package megamek.client.ui.swing.gmCommands;

import megamek.client.ui.swing.ClientGUI;
import megamek.common.Coords;
import megamek.common.annotations.Nullable;
import megamek.server.commands.GamemasterServerCommand;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.EnumArgument;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.commands.arguments.OptionalEnumArgument;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dialog for executing a gamemaster command.
 */
public class GamemasterCommandPanel extends JDialog {
    private final GamemasterServerCommand command;
    private final ClientGUI client;
    private final Coords coords;

    /**
     * Constructor for the dialog for executing a gamemaster command.
     *
     * @param parent    The parent frame.
     * @param client    The client GUI.
     * @param command   The command to render.
     */
    public GamemasterCommandPanel(JFrame parent, ClientGUI client, GamemasterServerCommand command, @Nullable Coords coords) {
        super(parent, command.getName(), true);
        this.command = command;
        this.client = client;
        this.coords = coords;
        initializeUI(parent);
    }

    private void initializeUI(JFrame parent) {
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        addTitleAndDescription();
        Map<String, JComponent> argumentComponents = addArgumentComponents();
        addExecuteButton(argumentComponents);
        pack();
        setLocationRelativeTo(parent);
    }

    private void addTitleAndDescription() {
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(command.getLongName());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanel.add(titleLabel);

        JLabel helpLabel = new JLabel(command.getHelpHtml());
        helpLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        titlePanel.add(helpLabel);

        add(titlePanel);
    }

    private Map<String, JComponent> addArgumentComponents() {
        List<Argument<?>> arguments = command.defineArguments();
        Map<String, JComponent> argumentComponents = new HashMap<>();

        for (Argument<?> argument : arguments) {
            JPanel argumentPanel = createArgumentPanel(argument);
            add(argumentPanel);
            argumentComponents.put(argument.getName(), getArgumentComponent(argument, argumentPanel));
        }
        return argumentComponents;
    }

    private JPanel createArgumentPanel(Argument<?> argument) {
        JPanel argumentPanel = new JPanel();
        argumentPanel.setLayout(new FlowLayout());
        JLabel label = new JLabel(argument.getName() + ":");
        argumentPanel.add(label);
        return argumentPanel;
    }

    private JComponent getArgumentComponent(Argument<?> argument, JPanel argumentPanel) {
        if (argument instanceof IntegerArgument intArg) {
            JSpinner spinner = createSpinner(intArg);
            argumentPanel.add(spinner);
            return spinner;
        } else if (argument instanceof OptionalEnumArgument<?> enumArg) {
            JComboBox<String> comboBox = createOptionalEnumComboBox(enumArg);
            argumentPanel.add(comboBox);
            return comboBox;
        } else if (argument instanceof EnumArgument<?> enumArg) {
            JComboBox<String> comboBox = createEnumComboBox(enumArg);
            argumentPanel.add(comboBox);
            return comboBox;
        }
        return null;
    }

    private boolean isArgumentX(Argument<?> argument) {
        return argument.getName().equals("x");
    }

    private boolean isArgumentY(Argument<?> argument) {
        return argument.getName().equals("y");
    }

    private int getIntArgumentDefaultValue(IntegerArgument intArg) {
        return intArg.hasDefaultValue() ? intArg.getValue() : isArgumentX(intArg) ? coords.getX()+1 :
            isArgumentY(intArg) ? coords.getY()+1 : 0;
    }

    private JSpinner createSpinner(IntegerArgument intArg) {
        return new JSpinner(new SpinnerNumberModel(
            getIntArgumentDefaultValue(intArg),
            intArg.getMinValue(),
            intArg.getMaxValue(),
            1));
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
        add(getExecuteButton(argumentComponents));
    }

    private JButton getExecuteButton(Map<String, JComponent> argumentComponents) {
        JButton executeButton = new JButton("Execute Command");
        executeButton.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to execute this command?",
                "Execute Command",
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
                } else {
                    args[i] = argument.getName() + "=" + Objects.requireNonNull(((JComboBox<?>) component).getSelectedItem());
                }
            }
        }

        client.getClient().sendChat("/" + command.getName() + " " + String.join(" ", args));
    }
}
