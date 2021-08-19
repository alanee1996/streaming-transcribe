package org.streaming.transcribe.on.growing.files.streaming;

import org.streaming.transcribe.JsonConvert;
import org.streaming.transcribe.Settings;
import org.streaming.transcribe.TranscribeStreamingDemoFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AwsStreamingTranscriber {
    private static final Region REGION = Region.US_EAST_1;
    private static TranscribeStreamingAsyncClient client;
    private AudioFileManager manager;
    private CompletableFuture<Void> task;

    public AwsStreamingTranscriber(AudioFileManager manager) {
        this.manager = manager;
        client = TranscribeStreamingAsyncClient.builder()
                .credentialsProvider(getCredential())
                .region(REGION)
                .build();
    }

    public void start() throws UnsupportedAudioFileException, IOException {
        task = client.startStreamTranscription(getRequest(),
                new AudioStreamPublisher(manager),
                getResponseHandler());
    }

    public boolean isDone() {
        return task.isDone();
    }

    private StartStreamTranscriptionResponseHandler getResponseHandler() {
        return StartStreamTranscriptionResponseHandler.builder()
                .onResponse(r -> {
                    System.out.println("Received Initial response");
                })
                .onError(e -> {
                    System.out.println(e.getMessage());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    System.out.println("Error Occurred: " + sw.toString());
                })
                .onComplete(() -> {
                    System.out.println("=== All records stream successfully ===");
                })
                .subscriber(event -> {
                    List<Result> results = ((TranscriptEvent) event).transcript().results();
//                    System.out.println(JsonConvert.GsonSerialize(results));
                    print(results);

                    if (results.size() > 0) {
//                        print(results);
                        if (!results.get(0).alternatives().get(0).transcript().isEmpty()) {
                            System.out.println(results.get(0).alternatives().get(0).transcript());
                        }
                    }
                })
                .build();
    }

    private StartStreamTranscriptionRequest getRequest() throws IOException, UnsupportedAudioFileException {
        return StartStreamTranscriptionRequest.builder()
                .languageCode(LanguageCode.EN_US)
//                .showSpeakerLabel(true)
                .mediaEncoding(MediaEncoding.PCM)
//                .mediaSampleRateHertz(90000)
                .mediaSampleRateHertz(16_000)
                .build();
    }

    private void print(List<Result> results) {
        try {
            System.out.println("-----------------------------------------------------------");
//            CompletableFuture.runAsync(() -> {
//            results.stream().forEach(result -> System.out.println(result.toString()));
            System.out.println(JsonConvert.GsonSerialize(results));

//            });
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void stop() {
        if (task != null && !task.isDone() && !task.isCancelled()) {
            task.cancel(true);
        }
    }

    private AwsCredentialsProvider getCredential() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                Settings.ACCESSKEYID,
                Settings.SECRET_ACCESSKEY);
        return StaticCredentialsProvider.create(awsCreds);
    }
}
