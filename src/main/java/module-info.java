module org.streaming.transcribe {
    requires javafx.controls;
    requires java.desktop;
    requires software.amazon.awssdk.core;
    requires org.reactivestreams;
    requires software.amazon.awssdk.services.transcribestreaming;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.auth;
    exports org.streaming.transcribe;
}