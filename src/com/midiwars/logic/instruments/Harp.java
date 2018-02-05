package com.midiwars.logic.instruments;

/**
 * Tuned in C Major [C3, C6]
 */
public class Harp extends Instrument {

    /* --- DEFINES --- */

    /** {@link Instrument#name Name}. */
    public final static String NAME = "Harp";

    /** {@link Instrument#canHold Can Hold}. */
    public final static boolean CAN_HOLD = false;

    /** {@link Instrument#keybars Key bars}. */
    public final static int[][] KEYBARS = {
            {48, 50, 52, 53, 55, 57, 59, 60},
            {60, 62, 64, 65, 67, 69, 71, 72},
            {72, 74, 76, 77, 79, 81, 83, 84}
    };

    /** {@link Instrument#idleKeybarIndex Idle Key bar Index}. */
    public final static int IDLE_KEYBAR_INDEX = 1;


    /* --- METHODS --- */

    /**
     * Constructor.
     */
    public Harp() {
        super(NAME, CAN_HOLD, KEYBARS, IDLE_KEYBAR_INDEX);
    }
}