package engine;

/**
 * Defines the closed set of allowed Splendor actions.
 * Keeps validation and execution explicit and rejects unsupported actions.
 * 
 * Used by Move.getType() to categorize player actions.
 * Consumed by MoveValidator, MoveExecutor, InputParser, and AI logic for dispatching.
 */
public enum MoveType {
    /**
     * Take three different colored tokens (one of each).
     */
    TAKE_THREE_DIFFERENT,
    
    /**
     * Take two tokens of the same color (when bank has sufficient supply).
     */
    TAKE_TWO_SAME,
    
    /**
     * Reserve a development card and optionally receive a gold token bonus.
     */
    RESERVE,
    
    /**
     * Purchase a development card using bonuses, tokens, and/or gold.
     */
    BUY
}
