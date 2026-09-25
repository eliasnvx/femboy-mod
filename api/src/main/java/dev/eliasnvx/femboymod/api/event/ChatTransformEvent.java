package dev.eliasnvx.femboymod.api.event;

/**
 * Client-side: posted after chat transformers ran and before the message is signed. Change
 * {@link #setMessage} or {@link #cancel()} to send the original text instead.
 */
public final class ChatTransformEvent implements CancellableEvent {

    private final String original;
    private String message;
    private boolean cancelled;

    /**
     * @param original    what the player typed
     * @param transformed what the transformers produced
     */
    public ChatTransformEvent(String original, String transformed) {
        this.original = original;
        this.message = transformed;
    }

    /** @return what the player typed */
    public String original() {
        return original;
    }

    /** @return the message that will be sent */
    public String message() {
        return message;
    }

    /** @param message the message to send instead (trimmed to the chat limit afterwards) */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
