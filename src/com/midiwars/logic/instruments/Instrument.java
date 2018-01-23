package com.midiwars.logic.instruments;

import com.midiwars.logic.Keymap;
import com.midiwars.logic.midi.MidiTimeline;
import com.midiwars.logic.midi.Note;
import com.midiwars.logic.midi.NoteEvent;

import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.util.ArrayList;

/**
 * Represents a musical instrument.
 */
public abstract class Instrument {

    /* --- DEFINES --- */

    /** Amount of time robot sleeps after a key bar change (ms). */
    public static int ROBOT_SLEEP = 50;

    /** Minimum amount of time needed in-between key bar changes (ms). */
    public static int KEYBAR_COOLDOWN = 150;


    /* --- ATTRIBUTES --- */

    /** Note of the instrument. */
    private final String name;

    /** True if the instrument can hold notes (ie note duration matters). */
    private final boolean canHold;

    /** Each line represents a key bar (in-game skill bar - usually an octave) and its slots. */
    private final int[][] keybars;

    /** The active key bar before and after the midi timeline is played. */
    private final int idleKeybarIndex;

    /** The currently active key bar. */
    private int activeKeybarIndex;

    /** Robot used to simulate system inputs. */
    private Robot robot;

    /** Timestamp of the previous key bar change (ms). */
    private int previousKeybarChange;

    /** Amount of time playback delayed (ms). */
    private int millisecondsBehind;


    /* --- METHODS --- */

    /**
     * Constructor.
     *
     * @param name Note of the instrument.
     * @param canHold If the instrument can hold notes.
     * @param keybars Each line represents a skill bar (usually an octave).
     * @param idleKeybarIndex The active key bar before and after the midi timeline is played.
     */
    public Instrument(String name, boolean canHold, int [][] keybars, int idleKeybarIndex) {

        this.name = name;
        this.canHold = canHold;
        this.keybars = keybars;
        this.idleKeybarIndex = idleKeybarIndex;
        activeKeybarIndex = idleKeybarIndex;
        robot = null;
        previousKeybarChange = -1 * KEYBAR_COOLDOWN;
        millisecondsBehind = 0;
    }


    /**
     * Plays the given midi timeline.
     *
     * @param midiTimeline Midi Timeline.
     *
     * @throws AWTException If the platform configuration does not allow low-level input control.
     */
    public void play(MidiTimeline midiTimeline) throws AWTException {

        ArrayList<NoteEvent> timeline = midiTimeline.getTimeline();

        robot = new Robot();

        for (int i = 0; i < timeline.size(); i++) {

            NoteEvent noteEvent = timeline.get(i);

            int keybarIndex = getKeybarIndex(noteEvent.getKey());

            // ignore if note can't be played
            if (keybarIndex < 0) {
                continue;
            }

            // amount to sleep until next noteEvent
            int delay = 0;

            // check if this isn't the last noteEvent
            if (i < timeline.size() - 1) {
                delay = timeline.get(i+1).getTimestamp() - noteEvent.getTimestamp();
            }

            int keybind;

            // case NOTE_ON
            if (noteEvent.getType() == ShortMessage.NOTE_ON) {

                // change keybars if needed
                delay -= changeKeybars(keybarIndex, noteEvent.getTimestamp());

                // play note
                keybind = Keymap.KEYBINDS[getKeyIndex(noteEvent.getKey())];
                robot.keyPress(keybind);

                System.out.println("debug: Played: " + noteEvent);

                // if can't hold notes, release key
                if (!canHold) {
                    robot.keyRelease(keybind);
                }
            }

            // case NOTE_OFF
            else if (canHold) {
                // assuming note positions are the same between keybars
                keybind = Keymap.KEYBINDS[getKeyIndex(noteEvent.getKey())];
                robot.keyRelease(keybind);
            }

            // sleep until next event
            if (delay > 0) {

                /*// correct delay if behind tempo
                delay -= millisecondsBehind;

                // wait until next event
                if (delay > 0) {
                    millisecondsBehind = 0;
                    robot.delay(delay);
                }
                // try to catch up with tempo
                else {
                    millisecondsBehind = -1 * delay;
                }*/
                robot.delay(delay);
            }
        }

        // return to idle keybar
        changeKeybars(idleKeybarIndex, previousKeybarChange);
    }


    /**
     * Checks if the given midi timeline can be played by this instrument
     * (ie if every note is contained in this instrument's key bars).
     *
     * @param midiTimeline Timeline to assess.
     *
     * @return True if instrument can play it, False otherwise.
     */
    public boolean canPlay(MidiTimeline midiTimeline) {

        for (NoteEvent noteEvent : midiTimeline.getTimeline()) {

            boolean found = false;

            search:
            for (int[] keybar : keybars) {
                for (int key : keybar) {
                    if (key == noteEvent.getKey()) {
                        found = true;
                        break search;
                    }
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }


    /**
     * Changes the active keybar to the given one.
     *
     * @param keybarIndex New active keybar.
     * @param timestamp Moment in time event was generated (ms).
     *
     * @return Returns the amount of time (ms) robot slept.
     */
    private int changeKeybars(int keybarIndex, int timestamp) {

        int sleptAmount = 0;

        // how many key bars are necessary to change
        int deltaKeybarIndex = keybarIndex - activeKeybarIndex;

        int keybind;
        for (int j = 0; j < Math.abs(deltaKeybarIndex); j++) {

            // how much time passed since the previous key bar change
            int deltaKeybarChange = timestamp - previousKeybarChange;

            // correct delta in case playback is behind in time
            if (deltaKeybarChange <= 0) {
                deltaKeybarChange = ROBOT_SLEEP;
            }

            // amount of time needed to wait before changing key bar
            int wait = 0;

            // check if it needs to wait before changing key bar
            if (deltaKeybarChange < KEYBAR_COOLDOWN) {

                wait = KEYBAR_COOLDOWN - deltaKeybarChange;

                // lagging behind, add to global delay
                millisecondsBehind += wait + deltaKeybarChange;
            }

            robot.delay(wait);

            // up
            if (deltaKeybarIndex > 0) {

                keybind = Keymap.OCTAVEUP_KEYBIND;
                robot.keyPress(keybind);
                robot.keyRelease(keybind);

                System.out.println("debug: KEYBAR_UP");
            }
            // down
            else {

                keybind = Keymap.OCTAVEDOWN_KEYBIND;
                robot.keyPress(keybind);
                robot.keyRelease(keybind);

                System.out.println("debug: KEYBAR_DOWN");
            }

            // update previous key bar change timestamp
            previousKeybarChange += deltaKeybarChange + wait;

            // needed for key bar change to take effect
            robot.delay(ROBOT_SLEEP);

            // update delay
            sleptAmount += ROBOT_SLEEP + wait;
        }

        // update active keybar index
        activeKeybarIndex = keybarIndex;

        return sleptAmount;
    }


    /**
     * Returns the index of the keybar given key (note) belongs to,
     * taking into account active key bar, in order to avoid
     * unnecessary keybar (octave) changes.
     *
     * @param key Key to assess.
     *
     * @return Key bar index. -1 if note can't be played.
     */
    private int getKeybarIndex(int key) {

        for (int k : keybars[activeKeybarIndex]) {
            if (k == key) {
                return activeKeybarIndex;
            }
        }

        for (int i = 0; i < keybars.length; i++) {
            // this was already tested above
            if (i == activeKeybarIndex) {
                continue;
            }

            for (int k : keybars[i]) {
                if (k == key) {
                    return i;
                }
            }
        }

        // can't play this note
        return -1;
    }


    /**
     * Returns the index of given key (note) in the active keybar.
     *
     * @param key Key to assess.
     *
     * @return Key index. -1 if key doesn't belong to active key bar.
     */
    private int getKeyIndex(int key) {

        for (int i = 0; i < keybars[activeKeybarIndex].length; i++) {
            if (key == keybars[activeKeybarIndex][i]) {
                return i;
            }
        }

        return -1;
    }
}
