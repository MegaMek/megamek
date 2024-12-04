package megamek.server.commands.arguments;

import java.util.Map;

public class Arguments {

    private final Map<String, Argument<?>> arguments;

    public Arguments(Map<String, Argument<?>> arguments) {
        this.arguments = arguments;
    }

    public Argument<?> get(String name) {
        return arguments.get(name);
    }

    public boolean hasArg(String name) {
        return arguments.containsKey(name);
    }

    @Override
    public String toString() {
        return "Arguments{" +
                "arguments=" + arguments +
                '}';
    }
}
