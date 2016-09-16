

package com.example.yangli.audiostream.media;

import java.io.IOException;
import java.net.DatagramSocket;

public interface AudioStreamer {
    int getCodecId();
    String getName();
    int getSampleRate();
    void StartTx(String ipAddress, int port, int sampleRateHz);
    void StopTx();
    void StartRx(String remoteIP, int remotePort, int localListeningPort);
    void StopRx(String remoteIP, int remotePort, int localListeningPort);

    void toggleMute();
    boolean isMuted();
    void sendDtmf();
}
