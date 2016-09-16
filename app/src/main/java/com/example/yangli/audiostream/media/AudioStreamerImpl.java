
package com.example.yangli.audiostream.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.nfc.Tag;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class AudioStreamerImpl implements AudioStreamer {
    private static final String TAG = "PUB";//RtpAudioSession.class.getSimpleName();
    private static final int AUDIO_SAMPLE_RATE = 8000;
    private static final int MAX_ALLOWABLE_LATENCY = 500; // ms

    private RecordTask mRecordTask;


    private boolean mSendDtmf = false;
    private int mCodecId;

    // Send
    private InetAddress mSendAddress;
    private int         mSendPort;
    private int         mSendSampleRate;
    private DatagramSocket mSendSocket;
    private MulticastSocket mSendMulticastSocket;

    // receive
    private int mLocalFrameSize;

    class ReceiverInfo{
        public InetAddress mRemoteAddress;
        public int         mRemotePort;
        public int         mListeningPort;
        public DatagramSocket mRecieverUnicastSocket;
        public MulticastSocket mReceiverMulticastSocket;
        public List<RtpPacket> mPacketList;
        private PlayTask mPlayTask;
        public ReceiverInfo(String RemoteAddress, int RemotePort, int ListeningPort){
            try {
                mRecieverUnicastSocket = null;
                mReceiverMulticastSocket = null;
                mRemoteAddress = InetAddress.getByName(RemoteAddress);
                mRemotePort = RemotePort;
                mListeningPort = ListeningPort;
                Log.e(TAG,"Address ="+ mRemoteAddress.toString());
                mPacketList = Collections.synchronizedList(new CopyOnWriteArrayList<RtpPacket>());
                if(mRemoteAddress.isMulticastAddress()){

                    Log.e(TAG,"multi Address ="+ mRemoteAddress.toString());
                    mReceiverMulticastSocket = new MulticastSocket(ListeningPort);
                    mReceiverMulticastSocket.joinGroup(mRemoteAddress);

                }else
                {
                    Log.e(TAG,"uni Address ="+ mRemoteAddress.toString());
                    mRecieverUnicastSocket = new DatagramSocket(ListeningPort);
                    //mPlayTask = new PlayTask( AUDIO_SAMPLE_RATE, mLocalFrameSize,mRecieverUnicastSocket );
                }
                mPlayTask = new PlayTask(AUDIO_SAMPLE_RATE, mLocalFrameSize,this );


            } catch (Exception e) {
                Log.e(TAG,"Exception =" +e);
                e.printStackTrace();

            }
        }
        public void start(){
            if(mPlayTask != null)
                mPlayTask.start();
        }
        public void stop(){
            if(mPlayTask != null)
                mPlayTask.stop();
        }
    }
    public ArrayList<ReceiverInfo> mReceiverList = new ArrayList<ReceiverInfo>();
    //public List<RtpPacket> mRtpPacketList =   Collections.synchronizedList(new ArrayList<RtpPacket>());

   /* private ReceiverInfo findReceiverBySSRC(long aSSRC){
        for(ReceiverInfo vInfo:mReceiverList){
            if(vInfo.mSSRC == aSSRC)
                return vInfo;
        }
        return null;
    }*/

    private NoiseGenerator mNoiseGenerator = new NoiseGenerator();

    AudioStreamerImpl(int codecId) {
        mCodecId = codecId;
    }

    void set(int remoteSampleRate) {
        mLocalFrameSize = AUDIO_SAMPLE_RATE / 80; // 80 frames / sec .i.e   "Each RTP packet contains 10ms of microphone audio."
        mRecordTask = new RecordTask(AUDIO_SAMPLE_RATE, mLocalFrameSize);

    }

    public int getCodecId() {
        return mCodecId;
    }

    public int getSampleRate() {
        return AUDIO_SAMPLE_RATE;
    }

    public String getName() {
        switch (mCodecId) {
        case 8: return "PCMA";
        default: return "PCMU";
        }
    }

    public void StartTx(String ipAddress, int port, int sampleRateHz){
        try {
            mSendMulticastSocket = null;
            mSendSocket = null;
            Log.e(TAG,"------------- StartTx");
            mSendAddress = InetAddress.getByName(ipAddress);
            if(mSendAddress.isMulticastAddress()){
                mSendMulticastSocket = new MulticastSocket();
                mSendMulticastSocket.setLoopbackMode(false);
                mSendMulticastSocket.setTimeToLive(64);
                mSendMulticastSocket.joinGroup(mSendAddress);

            }else{
                mSendSocket = new DatagramSocket();
            }
            mSendPort = port;
            mSendSampleRate = sampleRateHz;

            set(sampleRateHz);
            mRecordTask.start();
        } catch (Exception e) {
            Log.e(TAG,"------------- StartTx Exception" + e);
            e.printStackTrace();
        }
    }

    public void StartRx(String remoteIP, int remotePort, int localListeningPort){
        ReceiverInfo vReceiver;
        mLocalFrameSize = AUDIO_SAMPLE_RATE / 80;
        Log.e(TAG,"mReceiverList =" +mReceiverList.size());

        if(mReceiverList.size() == 0) {
          //  set(8000);

            vReceiver = new ReceiverInfo(remoteIP, remotePort, localListeningPort);
            vReceiver.start();
            mReceiverList.add(vReceiver);
        }
        else
            {

                vReceiver = new ReceiverInfo(remoteIP, remotePort, localListeningPort);
                vReceiver.start();

                mReceiverList.add(vReceiver);
                Log.e(TAG,"join success");
            }

        //mPlayTask.start();
    }
    public void StopTx(){
        mRecordTask.stop();
    }

    public void StopRx(String remoteIP, int remotePort, int localListeningPort){
        for(ReceiverInfo vInfo:mReceiverList){
            if(vInfo.mRemoteAddress.toString().substring(1).equalsIgnoreCase(remoteIP) && vInfo.mRemotePort == remotePort && vInfo.mListeningPort == localListeningPort){
                if(vInfo.mReceiverMulticastSocket != null){
                    try {
                        vInfo.mReceiverMulticastSocket.leaveGroup(vInfo.mRemoteAddress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                vInfo.stop();
                mReceiverList.remove(vInfo);
                break;
            }
        }
    }


    public synchronized void stop() {
        mRecordTask.stop();
        /*mPlayTask.stop();
        Log.v(TAG, "stop RtpSession: measured volume = "
                + mNoiseGenerator.mMeasuredVolume);
        // wait until player is stopped
        for (int i = 20; (i > 0) && !mPlayTask.isStopped(); i--) {
            Log.v(TAG, "    wait for player to stop...");
            try {
                wait(20);
            } catch (InterruptedException e) {
                // ignored
            }
        }*/
    }

    public void toggleMute() {
        if (mRecordTask != null) mRecordTask.toggleMute();
    }

    public boolean isMuted() {
        return ((mRecordTask != null) ? mRecordTask.isMuted() : false);
    }

    public void sendDtmf() {
        mSendDtmf = true;
    }
    private class PlayTask implements Runnable {
        private int mSampleRate;
        private int mFrameSize;
        private AudioPlayer mPlayer;
        private boolean mRunning;
        private ReceiverInfo mParent;
        private Decoder mDecoder;
        int mPlayBufferSize;
        short[] mPlayBuffer;// = new short[playBufferSize];
        short[] mTempBuffer;
        PlayTask(int sampleRate, int frameSize, ReceiverInfo parent) {
            Log.e(TAG,"PlayTask -------------------");
            mSampleRate = sampleRate;
            mFrameSize = frameSize;
            mParent = parent;
            mDecoder = RtpFactory.createDecoder(mCodecId);
            //mPlayBufferSize
            mPlayBufferSize = mDecoder.getSampleCount(mFrameSize);
            //mPlayBuffer = new short[vSize * 5];

        }

        void start() {
            if (mRunning) return;
            mRunning = true;
            new Thread(this).start();
        }

        void stop() {
            mRunning = false;
        }

        boolean isStopped() {
            return (mPlayer == null)
                    || (mPlayer.getPlayState() == AudioTrack.PLAYSTATE_STOPPED);
        }

       boolean demultiplexRTP(int offset) {
            Demultiplex vDemultiplex;
            synchronized(mParent.mPacketList) {
               // Log.e(TAG, "mParent.mPacketList size -------->" + mParent.mPacketList.size());
                //mPlayBuffer
                vDemultiplex = new Demultiplex(mPlayBufferSize);
                for (RtpPacket mPacket:mParent.mPacketList ){
                    try {
                        mTempBuffer = new short[mPlayBufferSize];
                        mDecoder.decode(mTempBuffer,mPacket.getRawPacket(),mPacket.getPacketDataLength(),offset);
                        vDemultiplex.push(mPacket.getSscr(),mTempBuffer);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mParent.mPacketList.remove(mPacket);
                }
                //Log.e(TAG, "mParent.mPacketList size -------->  ||" + mParent.mPacketList.size());
            }
           short [] vBuffer = vDemultiplex.getMixedData();
           if(vBuffer == null)
               return false;

           mPlayBuffer = new short[vBuffer.length];
           System.arraycopy(vBuffer,0,mPlayBuffer,0,vBuffer.length);
           return true;
        }

        public void run() {



            RtpReceiver receiver = new RtpReceiver(mFrameSize,mParent);
           // byte[] buffer = receiver.getBuffer();
            int offset = receiver.getPayloadOffset();

            int minBufferSize = AudioTrack.getMinBufferSize(mSampleRate,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT) * 6;
            minBufferSize = Math.max(minBufferSize, mPlayBufferSize);
            int bufferHighMark = minBufferSize / 2 * 8 / 10;
            Log.d(TAG, " play buffer = " + minBufferSize + ", high water mark="
                    + bufferHighMark);
            AudioTrack aplayer = new AudioTrack(AudioManager.STREAM_MUSIC,
                    mSampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, minBufferSize,
                    AudioTrack.MODE_STREAM);
            AudioPlayer player = mPlayer =
                    new AudioPlayer(aplayer, minBufferSize, mFrameSize);

            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            player.play();
            int playState = player.getPlayState();
            long receiveCount = 0;
            long cyclePeriod = mFrameSize * 1000L / mSampleRate;
            long cycleStart = 0;
            int seqNo = 0;
            int packetLossCount = 0;
            int bytesDropped = 0;

            player.flush();
            int writeHead = player.getPlaybackHeadPosition();

            // start measurement after first packet arrival
            try {
                receiver.receive();
                Log.d(TAG, "received first packet");
            } catch (IOException e) {
                Log.e(TAG, "receive error; stop player", e);
                player.stop();
                player.release();
                return;
            }
            cycleStart = System.currentTimeMillis();
            seqNo = receiver.getSequenceNumber();

            long startTime = System.currentTimeMillis();
            long virtualClock = startTime;
            float delta = 0f;
                receiver.start();
            RtpPacket vPacket;
            while (mRunning) {

                if(!demultiplexRTP(12)) {
                    /*try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                   // Log.e(TAG,"escape.........");
                    continue;
                }

                    int buffered = writeHead - player.getPlaybackHeadPosition();
                    if (buffered > bufferHighMark) {
                        player.flush();
                        buffered = 0;
                        writeHead = player.getPlaybackHeadPosition();
                        //if (LogRateLimiter.allowLogging(now))
                        {
                            //Log.d(TAG, " ~~~ flush: set writeHead to " + writeHead);
                        }
                    }
                    Log.e(TAG,"mPlayBuffer.length = "+mPlayBuffer.length);
                    writeHead += player.write(mPlayBuffer, 0, mPlayBuffer.length);

            }

            Log.d(TAG, "     receiveCount = " + receiveCount);
            Log.d(TAG, "     # packets lost =" + packetLossCount);
            Log.d(TAG, "     bytes dropped =" + bytesDropped);
            Log.d(TAG, "stop sound playing...");
            receiver.stop();
            player.stop();
            player.flush();
            player.release();
        }
    }

    private class RecordTask implements Runnable {
        private int mSampleRate;
        private int mFrameSize;
        private boolean mRunning;
        private boolean mMuted = false;

        RecordTask(int sampleRate, int frameSize) {
            mSampleRate = sampleRate;
            mFrameSize = frameSize;
        }

        void start() {
            Log.e(TAG,"start threads"+ mRunning);
            if (mRunning) return;
            mRunning = true;
            Log.e(TAG,"start threads");
            new Thread(this).start();
        }

        void stop() {
            mRunning = false;
        }

        void toggleMute() {
            mMuted = !mMuted;
        }

        boolean isMuted() {
            return mMuted;
        }

        private void adjustMicGain(short[] buf, int len, int factor) {
            int i,j;
            for (i = 0; i < len; i++) {
                j = buf[i];
                if (j > 32768/factor) {
                    buf[i] = 32767;
                } else if (j < -(32768/factor)) {
                    buf[i] = -32767;
                } else {
                    buf[i] = (short)(factor*j);
                }
            }
        }

        public void run() {
            Encoder encoder = RtpFactory.createEncoder(mCodecId);
            int recordBufferSize = encoder.getSampleCount(mFrameSize);
            short[] recordBuffer = new short[recordBufferSize];
            RtpSender sender = new RtpSender(mFrameSize);
            byte[] buffer = sender.getBuffer();
            int offset = sender.getPayloadOffset();

            int bufferSize = AudioRecord.getMinBufferSize(mSampleRate,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT) * 3 / 2;

            AudioRecord recorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, mSampleRate,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);

                recorder.startRecording();
            Log.d(TAG, "start sound recording..." + recorder.getState());

            // skip the first read, kick off read pipeline
            recorder.read(recordBuffer, 0, recordBufferSize);

            long sendCount = 0;
            long startTime = System.currentTimeMillis();

            while (mRunning) {
                int count = recorder.read(recordBuffer, 0, recordBufferSize);
                if (mMuted) continue;

                // TODO: remove the mic gain if the issue is fixed on Passion.
                adjustMicGain(recordBuffer, count, 16);

                int encodeCount =
                        encoder.encode(recordBuffer, count, buffer, offset);
                try {
                    sender.send(encodeCount);
                    if (mSendDtmf) {
                        recorder.stop();
                        sender.sendDtmf();
                        mSendDtmf = false;
                        recorder.startRecording();
                    }
                } catch (IOException e) {
                    if (mRunning) Log.e(TAG, "send error, stop sending", e);
                    break;
                }

                sendCount ++;
            }
            long now = System.currentTimeMillis();
            Log.d(TAG, "     sendCount = " + sendCount);
            Log.d(TAG, "     avg send cycle ="
                    + ((double) (now - startTime) / sendCount));
            Log.d(TAG, "stop sound recording...");
            recorder.stop();
            mMuted = false;
        }
    }

    private class RtpReceiver implements Runnable{
        RtpPacket mPacket;
        DatagramPacket mDatagram;
        ReceiverInfo mParent;
        private boolean mRunning;
        int mPacketSize;
        RtpReceiver(int size,ReceiverInfo parent) {
            mPacketSize = size;
            byte[] buffer = new byte[size + 12];
            Log.e(TAG,"------------------ Const "+size);
            mPacket = new RtpPacket(buffer);
            mPacket.setPayloadType(mCodecId);
            mDatagram = new DatagramPacket(buffer, buffer.length);
            Log.e(TAG,"------------------ Const "+size+ " len = "+buffer.length);
            mParent = parent;
        }

        void start() {
            Log.e(TAG,"start threads"+ mRunning);
            if (mRunning) return;
            mRunning = true;
            Log.e(TAG,"start threads");
            new Thread(this).start();
        }

        void stop() {
            mRunning = false;
        }

        byte[] getBuffer() {
            return mPacket.getRawPacket();
        }

        int getPayloadOffset() {
            return 12;
        }


        // return received payload size
        int receive() throws IOException {
            byte[] buffer = new byte[mPacketSize + 12];
            mPacket = new RtpPacket(buffer);
            mPacket.setPayloadType(mCodecId);
            mDatagram = new DatagramPacket(buffer, buffer.length);
            DatagramPacket datagram = mDatagram;
            if(mParent.mReceiverMulticastSocket != null) {

                mParent.mReceiverMulticastSocket.receive(datagram);
                mParent.mPacketList.add(mPacket);
            }
            else {

                    mParent.mRecieverUnicastSocket.receive(datagram);
                if(datagram.getAddress().toString().equalsIgnoreCase(mParent.mRemoteAddress.toString()))
                    mParent.mPacketList.add(mPacket);
            }

           // Log.e(TAG,"datagram.getLength()" + datagram.getLength());
            return datagram.getLength() - 12;
        }

        InetAddress getRemoteAddress() {
            return mDatagram.getAddress();
        }

        int getRemotePort() {
            return mDatagram.getPort();
        }

        int getSequenceNumber() {
            return mPacket.getSequenceNumber();
        }

        @Override
        public void run() {
            while (mRunning){
                try {
                    receive();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class RtpSender{
        RtpPacket mPacket;
        DatagramPacket mDatagram;
        private int mSequence = 0;
        private long mTimeStamp = 0;

        RtpSender(int size) {
            byte[] buffer = new byte[size + 12];
            mPacket = new RtpPacket(buffer);
            mPacket.setPayloadType(mCodecId);
            mDatagram = new DatagramPacket(mPacket.getRawPacket(), mPacket.getRawPacket().length, mSendAddress,mSendPort);
        }

        byte[] getBuffer() {
            return mPacket.getRawPacket();
        }

        int getPayloadOffset() {
            return 12;
        }

        void sendDtmf() throws IOException {
            byte[] buffer = getBuffer();

            RtpPacket packet = mPacket;
            packet.setPayloadType(101);
            packet.setPayloadLength(4);

            mTimeStamp += 160;
            packet.setTimestamp(mTimeStamp);
            DatagramPacket datagram = mDatagram;
            datagram.setLength(packet.getPacketLength());
            int duration = 480;
            buffer[12] = 1;
            buffer[13] = 0;
            buffer[14] = (byte)(duration >> 8);
            buffer[15] = (byte)duration;
            for (int i = 0; i < 3; i++) {
                packet.setSequenceNumber(mSequence++);
                if(mSendMulticastSocket != null)
                    mSendMulticastSocket.send(datagram);
                else
                     mSendSocket.send(datagram);
                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                }
            }
            mTimeStamp += 480;
            packet.setTimestamp(mTimeStamp);
            buffer[12] = 1;
            buffer[13] = (byte)0x80;
            buffer[14] = (byte)(duration >> 8);
            buffer[15] = (byte)duration;
            for (int i = 0; i < 3; i++) {
                packet.setSequenceNumber(mSequence++);
                if(mSendMulticastSocket != null)
                    mSendMulticastSocket.send(datagram);
                else
                    mSendSocket.send(datagram);
            }
        }

        void send(int count) throws IOException {
            mTimeStamp += count;
            RtpPacket packet = mPacket;
            packet.setSequenceNumber(mSequence++);
            packet.setPayloadType(mCodecId);
            packet.setTimestamp(mTimeStamp);
            packet.setPayloadLength(count);

            DatagramPacket datagram = mDatagram;
            datagram.setLength(packet.getPacketLength());
            if(mSendMulticastSocket != null)
                mSendMulticastSocket.send(datagram);
            else
                mSendSocket.send(datagram);
        }
    }

    private class NoiseGenerator {
        private static final int AMP = 1000;
        private static final int TURN_DOWN_RATE = 80;
        private static final int NOISE_LENGTH = 160;

        private short[] mNoiseBuffer = new short[NOISE_LENGTH];
        private int mMeasuredVolume = 0;

        short[] makeNoise() {
            final int len = NOISE_LENGTH;
            short volume = (short) (mMeasuredVolume / TURN_DOWN_RATE / AMP);
            double volume2 = volume * 2.0;
            int m = 8;
            for (int i = 0; i < len; i+=m) {
                short v = (short) (Math.random() * volume2);
                v -= volume;
                for (int j = 0, k = i; (j < m) && (k < len); j++, k++) {
                    mNoiseBuffer[k] = v;
                }
            }
            return mNoiseBuffer;
        }

        void measureVolume(short[] audioData, int offset, int count) {
            for (int i = 0, j = offset; i < count; i++, j++) {
                mMeasuredVolume = (mMeasuredVolume * 9
                        + Math.abs((int) audioData[j]) * AMP) / 10;
            }
        }

        int getNoiseLength() {
            return mNoiseBuffer.length;
        }
    }

    // Use another thread to play back to avoid playback blocks network
    // receiving thread
    private class AudioPlayer implements Runnable,
            AudioTrack.OnPlaybackPositionUpdateListener {
        private short[] mBuffer;
        private int mStartMarker;
        private int mEndMarker;
        private AudioTrack mTrack;
        private int mFrameSize;
        private int mOffset;
        private boolean mIsPlaying = false;
        private boolean mNotificationStarted = false;

        AudioPlayer(AudioTrack track, int bufferSize, int frameSize) {
            mTrack = track;
            mBuffer = new short[bufferSize];
            mFrameSize = frameSize;
        }

        synchronized int write(short[] buffer, int offset, int count) {
            int bufferSize = mBuffer.length;
            /*while (getBufferedDataSize() + count > bufferSize) {
                try {
                    wait();
                } catch (Exception e) {
                    //
                }
            }
            */
            int end = mEndMarker % bufferSize;
            if (end + count > bufferSize) {
                int partialSize = bufferSize - end;
                System.arraycopy(buffer, offset, mBuffer, end, partialSize);
                System.arraycopy(buffer, offset + partialSize, mBuffer, 0,
                        count - partialSize);
            } else {
                System.arraycopy(buffer, 0, mBuffer, end, count);
            }
            mEndMarker += count;

            return count;
        }

        synchronized void flush() {
            mEndMarker = mStartMarker;
            notify();
        }

        int getBufferedDataSize() {
            return mEndMarker - mStartMarker;
        }

        synchronized void play() {
            if (!mIsPlaying) {
                mTrack.setPositionNotificationPeriod(mFrameSize);
                mTrack.setPlaybackPositionUpdateListener(this);
                mIsPlaying = true;
                mTrack.play();
                mOffset = mTrack.getPlaybackHeadPosition();

                // start initial noise feed, to kick off periodic notification
                new Thread(this).start();
            }
        }

        synchronized void stop() {
            mIsPlaying = false;
            mTrack.stop();
            mTrack.flush();
            mTrack.setPlaybackPositionUpdateListener(null);
        }

        synchronized void release() {
            mTrack.release();
        }

        public synchronized void run() {
            Log.d(TAG, "start initial noise feed");
            int count = 0;
            long waitTime = mNoiseGenerator.getNoiseLength() / 8; // ms
            while (!mNotificationStarted && mIsPlaying) {
                feedNoise();
                count++;
                try {
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    Log.e(TAG, "initial noise feed error: " + e);
                    break;
                }
            }
            Log.d(TAG, "stop initial noise feed: " + count);
        }

        int getPlaybackHeadPosition() {
            return mStartMarker;
        }

        int getPlayState() {
            return mTrack.getPlayState();
        }

        int getState() {
            return mTrack.getState();
        }

        // callback
        public void onMarkerReached(AudioTrack track) {
        }

        // callback
        public synchronized void onPeriodicNotification(AudioTrack track) {
            if (!mNotificationStarted) {
                mNotificationStarted = true;
                Log.d(TAG, " ~~~   notification callback started");
            } else if (!mIsPlaying) {
                Log.d(TAG, " ~x~   notification callback quit");
                return;
            }
            try {
                writeToTrack();
            } catch (IllegalStateException e) {
                Log.e(TAG, "writeToTrack()", e);
            }
        }

        private void feedNoise() {
            short[] noiseBuffer = mNoiseGenerator.makeNoise();
            mOffset += mTrack.write(noiseBuffer, 0, noiseBuffer.length);
        }

        private synchronized void writeToTrack() {
            if (mStartMarker == mEndMarker) {
                int head = mTrack.getPlaybackHeadPosition() - mOffset;
                if ((mStartMarker - head) <= 320) feedNoise();
                return;
            }
;
            int count = mFrameSize;
            if (count < getBufferedDataSize()) count = getBufferedDataSize();

            int bufferSize = mBuffer.length;
            int start = mStartMarker % bufferSize;
            if ((start + count) <= bufferSize) {
                mStartMarker += mTrack.write(mBuffer, start, count);
            } else {
                int partialSize = bufferSize - start;
                mStartMarker += mTrack.write(mBuffer, start, partialSize);
                mStartMarker += mTrack.write(mBuffer, 0, count - partialSize);
            }
            notify();
        }
    }

    private static class LogRateLimiter {
        private static final long MIN_TIME = 1000;
        private static long mLastTime;

        private static boolean allowLogging(long now) {
            if ((now - mLastTime) < MIN_TIME) {
                return false;
            } else {
                mLastTime = now;
                return true;
            }
        }
    }
}
