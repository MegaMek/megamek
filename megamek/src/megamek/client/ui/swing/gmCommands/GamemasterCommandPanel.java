package megamek.client.ui.swing.gmCommands;

import megamek.client.ui.swing.ClientGUI;
import megamek.server.commands.GamemasterServerCommand;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.EnumArgument;
import megamek.server.commands.arguments.IntegerArgument;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// JPanel wrapper for game master commands
public class GamemasterCommandPanel extends JDialog {
    private final GamemasterServerCommand command;
    private final ClientGUI client;

    public GamemasterCommandPanel(JFrame parent, ClientGUI client, GamemasterServerCommand command) {
        super(parent, command.getName(), true);
        this.command = command;
        this.client = client;
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JLabel helpLabel = new JLabel(command.getHelpHtml());
        add(helpLabel);

        List<Argument<?>> arguments = command.defineArguments();
        Map<String, JComponent> argumentComponents = new HashMap<>();

        for (Argument<?> argument : arguments) {
            JPanel argumentPanel = new JPanel();
            argumentPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

            JLabel label = new JLabel(argument.getName() + ":");
            argumentPanel.add(label);

            if (argument instanceof IntegerArgument) {
                IntegerArgument intArg = (IntegerArgument) argument;
                JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                    intArg.hasDefaultValue() ? intArg.getValue() : 0,
                    intArg.getMinValue(),
                    intArg.getMaxValue(),
                    1));
                argumentPanel.add(spinner);
                argumentComponents.put(argument.getName(), spinner);
            } else if (argument instanceof EnumArgument) {
                EnumArgument<?> enumArg = (EnumArgument<?>) argument;
                JComboBox<String> comboBox = new JComboBox<>();
                for (Enum<?> constant : enumArg.getEnumType().getEnumConstants()) {
                    comboBox.addItem(constant.name());
                }
                if (enumArg.getValue() != null) {
                    comboBox.setSelectedItem(enumArg.getValue().name());
                }
                argumentPanel.add(comboBox);
                argumentComponents.put(argument.getName(), comboBox);
            }

            add(argumentPanel);
        }

        JButton executeButton = new JButton("Execute Command");
        executeButton.addActionListener(e -> executeCommand(argumentComponents));
        add(executeButton);

        pack();
        setLocationRelativeTo(parent);
    }

    private String wrapText(String text, int lineLength) {
        StringBuilder wrappedText = new StringBuilder("<html>");
        int currentIndex = 0;
        while (currentIndex < text.length()) {
            int endIndex = Math.min(currentIndex + lineLength, text.length());
            wrappedText.append(text, currentIndex, endIndex).append("<br>");
            currentIndex = endIndex;
        }
        wrappedText.append("</html>");
        return wrappedText.toString();
    }

    private void executeCommand(Map<String, JComponent> argumentComponents) {
        List<Argument<?>> arguments = command.defineArguments();
        String[] args = new String[arguments.size()];

        for (int i = 0; i < arguments.size(); i++) {
            Argument<?> argument = arguments.get(i);
            JComponent component = argumentComponents.get(argument.getName());

            if (component instanceof JSpinner) {
                args[i] = argument.getName() + "=" + ((JSpinner) component).getValue().toString();
            } else if (component instanceof JComboBox) {
                args[i] = argument.getName() + "=" + Objects.requireNonNull(((JComboBox<?>) component).getSelectedItem());
            }
        }
        client.getClient().sendChat("/" + command.getName() + " " + String.join(" ", args));
    }
}
