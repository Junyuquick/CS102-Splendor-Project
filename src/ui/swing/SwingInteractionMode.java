package ui.swing;

/**
 * Interaction modes supported by the shared Swing game frame.
 */
final class SwingInteractionMode {
    static final SwingInteractionMode IDLE =
            new SwingInteractionMode("IDLE");
    static final SwingInteractionMode TAKE_THREE =
            new SwingInteractionMode("TAKE_THREE");
    static final SwingInteractionMode TAKE_TWO =
            new SwingInteractionMode("TAKE_TWO");
    static final SwingInteractionMode RESERVE =
            new SwingInteractionMode("RESERVE");
    static final SwingInteractionMode BUY =
            new SwingInteractionMode("BUY");
    static final SwingInteractionMode RETURN_TOKENS =
            new SwingInteractionMode("RETURN_TOKENS");
    static final SwingInteractionMode PASS =
            new SwingInteractionMode("PASS");

    private final String name;

    private SwingInteractionMode(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
