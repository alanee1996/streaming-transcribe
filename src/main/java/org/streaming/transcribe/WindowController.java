//package org.streaming.transcribe;
//import javax.sound.sampled.AudioFormat;
//import javax.sound.sampled.AudioSystem;
//import javax.sound.sampled.DataLine;
//import javax.sound.sampled.TargetDataLine;
//import javax.sound.sampled.AudioInputStream;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
//import software.amazon.awssdk.services.transcribestreaming.model.TranscribeStreamingException ;
//import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;
//import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
//import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
//import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;
//import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
//
//public class WindowController {
//
//    public static void convertAudio(TranscribeStreamingAsyncClient client) throws Exception {
//
//        try {
//
//            StartStreamTranscriptionRequest request = StartStreamTranscriptionRequest.builder()
//                    .mediaEncoding(MediaEncoding.PCM)
//                    .languageCode(LanguageCode.EN_US)
//                    .mediaSampleRateHertz(16_000).build();
//
//            TargetDataLine mic = Microphone.get();
//            mic.start();
//
//            AudioStreamPublisher publisher = new AudioStreamPublisher(new AudioInputStream(mic));
//
//            StartStreamTranscriptionResponseHandler response =
//                    StartStreamTranscriptionResponseHandler.builder().subscriber(e -> {
//                        TranscriptEvent event = (TranscriptEvent) e;
//                        event.transcript().results().forEach(r -> r.alternatives().forEach(a -> System.out.println(a.transcript())));
//                    }).build();
//
//            // Keeps Streaming until you end the Java program
//            client.startStreamTranscription(request, publisher, response);
//
//        } catch (TranscribeStreamingException e) {
//            System.err.println(e.awsErrorDetails().errorMessage());
//            System.exit(1);
//        }
//    }
//}
