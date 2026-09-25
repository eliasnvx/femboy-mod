package dev.eliasnvx.femboymod.api.event;

/** Order in which listeners are called; {@link #HIGHEST} first. */
public enum EventPriority {
    /** Called first. */
    HIGHEST,
    /** Called after {@link #HIGHEST}. */
    HIGH,
    /** Default priority. */
    NORMAL,
    /** Called after {@link #NORMAL}. */
    LOW,
    /** Called last. */
    LOWEST
}
