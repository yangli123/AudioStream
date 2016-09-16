/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.yangli.audiostream.rtp;

import android.util.Log;

import java.net.InetAddress;
import java.net.SocketException;

/**
 * The RtpSocket class represents a UDP socket carrying RTP packets. The network
 * port of the local host is assigned automatically to conform with RFC 3550.
 */
public class RtpSocket {
    private int mNative;
    private final InetAddress mLocalAddress;
    private final int mLocalPort;

    private InetAddress mRemoteAddress;
    private int mRemotePort = -1;
    private boolean mInUse = false;

    static {
        Log.e("PUB","load lib audiostream");
        System.loadLibrary("audiostream");
    }

    /**
     * Creates a RtpSocket by specifying the network address of the local host.
     *
     * @param address The network address of the local host to bind to.
     * @throws SocketException if the address cannot be bound or a problem
     *     occurs during binding.
     */
    public RtpSocket(InetAddress address) throws SocketException {
        mLocalPort = create(address.getHostAddress());
        mLocalAddress = address;
    }
    private native int create(String address) throws SocketException;

    synchronized void occupy() {
        if (mRemoteAddress == null) {
            throw new IllegalStateException("RtpSocket is not associated");
        }
        if (mInUse) {
            throw new IllegalStateException("RtpSocket is already in use");
        }
        mInUse = true;
    }

    synchronized void vacate() {
        mInUse = false;
    }

    /**
     * Returns the network address of the local host.
     */
    public InetAddress getLocalAddress() {
        return mLocalAddress;
    }

    /**
     * Returns the network port of the local host.
     */
    public int getLocalPort() {
        return mLocalPort;
    }

    /**
     * Returns the network address of the remote host or {@code null} if the
     * socket is not associated.
     */
    public InetAddress getRemoteAddress() {
        return mRemoteAddress;
    }

    /**
     * Returns the network port of the remote host or {@code -1} if the socket
     * is not associated.
     */
    public int getRemotePort() {
        return mRemotePort;
    }

    /**
     * Associates with a remote host.
     *
     * @param address The network address of the remote host.
     * @param port The network port of the remote host.
     * @throws SocketException if the address family is different or the socket
     *     is already associated.
     */
    public synchronized void associate(InetAddress address, int port)
            throws SocketException {
        associate(address.getHostAddress(), port);
        mRemoteAddress = address;
        mRemotePort = port;
    }
    private native void associate(String address, int port) throws SocketException;

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }
    private native void release();
}
