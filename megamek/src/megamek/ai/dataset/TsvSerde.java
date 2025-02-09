package megamek.ai.dataset;

public abstract class TsvSerde<T> {
    public abstract String toTsv(T obj);

    public abstract String getHeaderLine();
}
