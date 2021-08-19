package org.streaming.transcribe.on.growing.files.streaming;

import java.util.concurrent.CompletableFuture;

public class DemoApp {

    public static void main(String args[]) {
        try{
            AudioFileManager manager = new AudioFileManager();
            CompletableFuture.runAsync(()-> {
                try{
                    System.out.println("----------------task start");
                    Thread.sleep(2000);
                    System.out.println("----------------wav one insert");
                    manager.add("/Users/alaneemac/Music/st-test.wav");
                    Thread.sleep(8000);
                    System.out.println("----------------wav two insert");
                    manager.add("/Users/alaneemac/Music/rs-test2.wav");
                    System.out.println("----------------execute complete");
                    manager.complete();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.out.println("----------------task ended");
            });
            AwsStreamingTranscriber transcriber = new AwsStreamingTranscriber(manager);
            transcriber.start();
            while (!transcriber.isDone()) {

            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
