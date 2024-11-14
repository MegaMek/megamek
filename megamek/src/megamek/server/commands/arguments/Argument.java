package megamek.server.commands.arguments;

// A generic Argument class that can be extended for different argument types
public abstract class Argument<T> {
    protected T value;

    private final String name;

    public Argument(String name) {
        this.name = name;
    }

    public T getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public abstract void parse(String input) throws IllegalArgumentException;
}
