package org.streaming.transcribe.on.growing.files.streaming;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.InputStream;
import java.util.Vector;

public class AudioFileManager {
    private Vector<InputStream> audioFiles = new Vector<>();
    private boolean isCompleted = false;
    private static final int sampleRate = 16_000;
    private static final int sampleSizeBits = 16;
    private static final int defaultChannels = 1;

    private static AudioFormat getDefaultAudioFormat() {
        return new AudioFormat(sampleRate, sampleSizeBits, defaultChannels, true, false);
    }

    public void add(String file) {
        try {
            File inputFile = new File(file);
            AudioInputStream origin = AudioSystem.getAudioInputStream(inputFile);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(getDefaultAudioFormat(), origin);
            audioFiles.add(audioStream);
            long l = origin.getFrameLength();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int length() {
        return audioFiles.size();
    }

    public boolean isCompleted() {
//        return this.isCompleted;
        return this.isCompleted && audioFiles.size() == 0;
    }

    public void complete() {
        isCompleted = true;
    }

    public InputStream getNext() {
        try {
            if (length() > 0) {
                InputStream stream = audioFiles.firstElement();
                audioFiles.remove(0);
                return stream;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
