package megamek.client.ui.advancedsearch;

class TriStateItem {
    public String state;
    public String text;
    public int code;

    public TriStateItem(String state, String text) {
        this.state = state;
        this.text = text;
    }

    public TriStateItem(String state, int code, String text) {
        this.state = state;
        this.code = code;
        this.text = text;
    }

    @Override
    public String toString() {
        return state + " " + text;
    }
}
