package com.midiwars.logic.midi;

import javax.sound.midi.ShortMessage;

import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;

/**
 * Collection of handlers called when dealing with MidiMessages of type ShortMessage.
 */
public abstract class ShortMessageHandler {

    /**
     * Called when a ShortMessage is received.
     *
     * @param midiTimeline midi timeline.
     * @param shortMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     */
    public static void shortMessageHandler(MidiTimeline midiTimeline, ShortMessage shortMessage, long tick) {

        switch (shortMessage.getCommand()) {

            case NOTE_ON: {

                noteOn(midiTimeline, shortMessage, tick);
                break;
            }

            case ShortMessage.NOTE_OFF: {

                noteOff(midiTimeline, shortMessage, tick);
                break;
            }

            default: {

                System.out.println("debug: Command:" + shortMessage.getCommand());
                break;
            }
        }
    }


    /**
     * Called when ShortMessage is of type NOTE_ON.
     *
     * @param midiTimeline midi timeline.
     * @param shortMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     *
     * @see #shortMessageHandler(MidiTimeline, ShortMessage, long)
     */
    private static void noteOn(MidiTimeline midiTimeline, ShortMessage shortMessage, long tick) {

        // get velocity
        int velocity = shortMessage.getData2();

        // message should have been NOTE_OFF
        if (velocity == 0) {
            noteOff(midiTimeline, shortMessage, tick);
            return;
        }

        // get key number [0-127]
        int key = shortMessage.getData1();

        // add note to timeline
        midiTimeline.getTimeline().add(new NoteEvent(NOTE_ON, key, tick, midiTimeline.getSequence().getResolution(), midiTimeline.getTempo()));
    }


    /**
     * Called when ShortMessage is of type NOTE_OFF.
     *
     * @param midiTimeline midi timeline.
     * @param shortMessage Message received.
     * @param tick MidiEvent time-stamp (ticks).
     *
     * @see #shortMessageHandler(MidiTimeline, ShortMessage, long)
     */
    private static void noteOff(MidiTimeline midiTimeline, ShortMessage shortMessage, long tick) {

        // get key number [0-127]
        int key = shortMessage.getData1();

        // add note release event to timeline
        midiTimeline.getTimeline().add(new NoteEvent(NOTE_OFF, key, tick, midiTimeline.getSequence().getResolution(), midiTimeline.getTempo()));

        // set duration of released note
        for (NoteEvent noteEvent : midiTimeline.getTimeline()) {
            if (noteEvent.getType() == NOTE_ON && noteEvent.getKey() == key && noteEvent.getNote().duration == 0) {
                noteEvent.setNoteDuration(tick, midiTimeline.getSequence().getResolution(), midiTimeline.getTempo());
                return;
            }
        }
    }
}