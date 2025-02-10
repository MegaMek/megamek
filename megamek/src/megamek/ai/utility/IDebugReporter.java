package megamek.ai.utility;

public interface IDebugReporter {

    IDebugReporter noOp = new IDebugReporter() {
        @Override
        public IDebugReporter append(String s) {
            return this;
        }

        @Override
        public IDebugReporter append(Consideration<?, ?> consideration) {
            return this;
        }

        @Override
        public IDebugReporter newLine(int i) {
            return this;
        }

        @Override
        public IDebugReporter indent(int i) {
            return this;
        }

        @Override
        public IDebugReporter newLine() {
            return this;
        }

        @Override
        public IDebugReporter indent() {
            return this;
        }

        @Override
        public IDebugReporter newLineIndent(int indent) {
            return this;
        }

        @Override
        public IDebugReporter newLineIndent() {
            return this;
        }

        @Override
        public IDebugReporter append(double s) {
            return this;
        }

        @Override
        public IDebugReporter append(int s) {
            return this;
        }

        @Override
        public IDebugReporter append(float s) {
            return this;
        }

        @Override
        public IDebugReporter append(boolean s) {
            return this;
        }

        @Override
        public IDebugReporter append(Object s) {
            return this;
        }

        @Override
        public IDebugReporter append(long s) {
            return this;
        }

        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public String getReport() {
            return "";
        }
    };

    IDebugReporter append(String s);

    IDebugReporter append(Consideration<?,?> consideration);

    IDebugReporter newLine(int i);

    IDebugReporter indent(int i);

    IDebugReporter newLine();

    IDebugReporter indent();

    IDebugReporter newLineIndent(int indent);

    IDebugReporter newLineIndent();

    IDebugReporter append(double s);

    IDebugReporter append(int s);

    IDebugReporter append(float s);

    IDebugReporter append(boolean s);

    IDebugReporter append(Object s);

    IDebugReporter append(long s);

    String getReport();

    default boolean enabled() {
        return true;
    }

    @Override
    String toString();
}
