package megamek.server.commands.arguments;

// A generic Argument class that can be extended for different argument types
public abstract class Argument<T> {
    protected T value;
    private final String name;
    private final String description;

    public Argument(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public T getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getRepr() {
        return "<" + getName() + "=#>";
    }

    public abstract String getHelp();

    public abstract void parse(String input) throws IllegalArgumentException;
}
