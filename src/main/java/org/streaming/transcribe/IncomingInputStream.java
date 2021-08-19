package org.streaming.transcribe;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

public class IncomingInputStream {

    public static void main(String args[]) {
        try {
            ConconrentStream conconrentStream = new ConconrentStream();
//            SequenceInputStream stream = new SequenceInputStream(f1, f2);
            conconrentStream.add("/Users/alaneemac/Music/rs-test.wav");
            conconrentStream.add("/Users/alaneemac/Music/rs-test2.wav");
            conconrentStream.complete();
            SequenceInputStream stream = new SequenceInputStream(conconrentStream);
            File tempAudio = new File("/Users/alaneemac/Music/test.wav");

//            CompletableFuture.runAsync(() -> {
//                try {
//                    while (stream.available() > 0) {
//                        int data = stream.read();
//                        outputStream.write(data);
//                    }
//                    outputStream.close();
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            });
//            CompletableFuture.runAsync(() -> {
//                try {
//                    Thread.sleep(2000);
//                    conconrentStream.add("/Users/alaneemac/Music/rs-test.wav");
//                    Thread.sleep(40000);
//                    conconrentStream.add("/Users/alaneemac/Music/rs-test2.wav");
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            });
//            CompletableFuture.runAsync(() -> {
//                try {
//                    Thread.sleep(60000);
//                    conconrentStream.complete();
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            });
            try {
//                while (stream.available() >= 0) {
//                    outputStream.write(stream.read());
//                }
//                outputStream.close();
//                stream.close();
//                f1.close();
//                f2.close();
//                while (conconrentStream.hasMoreElements()) {
//                    InputStream stream = conconrentStream.nextElement();
//                    int data;
//                    while ((data = stream.read()) != -1) {
//                        outputStream.write(data);
//                    }
//                    stream.close();
//                }
//                outputStream.close();
                AudioInputStream appendFiles = new AudioInputStream(stream, new AudioFormat(conconrentStream.getHetz(), 16, 1, true, false), conconrentStream.getLength());
                //                AudioSystem.write(appendFiles, AudioFileFormat.Type.WAVE, tempAudio);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
//            Clip clip;
//            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(tempAudio);
//            clip = AudioSystem.getClip();
//            clip.open(audioInputStream);
//            clip.loop(Clip.LOOP_CONTINUOUSLY);
//            clip.start();
//            while (clip.isRunning()) {
//                System.out.println("streaming");
//            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static class ConconrentStream implements Enumeration<AudioInputStream> {

        Vector<AudioInputStream> list = new Vector<>();
        private boolean hasMore = true;
        private long length = 80000;
        private long herz = 16_000;

        public void add(String file) {
            try {
                File inputFile = new File(file);
                AudioInputStream origin = AudioSystem.getAudioInputStream(inputFile);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(new AudioFormat(16000, 16, 1, true, false), origin);
                list.add(audioStream);
                long l = origin.getFrameLength();
                length += l;
                herz = Math.round(audioStream.getFormat().getSampleRate());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean hasMoreElements() {
            return list.size() > 0 || hasMore;
        }

        public long getLength() {
            return length;
        }

        public long getHetz() {
            return herz;
        }

        @Override
        public AudioInputStream nextElement() {
            if (list.size() > 0) {
                AudioInputStream stream = list.firstElement();
                list.remove(0);
                return stream;
            } else {
                return new AudioInputStream(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return -1;
                    }
                }, new AudioFormat(16000, 16, 1, true, false), 0);
            }
        }

        public void complete() {
            hasMore = false;
        }
    }

}