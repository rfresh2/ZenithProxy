package com.zenith.mcping.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;

//I would not recommend this for non-Windows systems. What would happen if those ports are closed but the host is still alive?
//Instead, use the ICMP protocol, this is more like a mini-port scanner for designated ports.
//InetAddress::isReachable works well too. (very well infact)
class TCPPinger {

    // try different ports in sequence, starting with 80 (which is most probably not filtered)
    private static final int[] PROBE_TCP_PORTS = {80, 443, 8080, 22, 7};
    private final int timeout;

    public TCPPinger(int timeout) {
        this.timeout = timeout;
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException ignore) {
        }
    }

    //A socket is already a closeable, this method is redundant

    public boolean ping(InetAddress address, int count) {
        //PingResult result = new PingResult(subject.getAddress(), count);

        Socket socket;
        for (int i = 0; i < count && !Thread.currentThread().isInterrupted(); i++) {
            socket = new Socket();
            // cycle through different ports until a working one is found
            int probePort = PROBE_TCP_PORTS[i % PROBE_TCP_PORTS.length];
            // change the first port to the requested one, if it is available

            //long startTime = System.currentTimeMillis();
            try {
                // set some optimization options
                socket.setReuseAddress(true);
                socket.setReceiveBufferSize(32);
                //int timeout = result.isTimeoutAdaptationAllowed() ? min(result.getLongestTime() * 2, this.timeout) : this.timeout;
                socket.connect(new InetSocketAddress(address, probePort), timeout);
                if (socket.isConnected()) {
                    // it worked - success
                    //success(result, startTime);
                    closeQuietly(socket);
                    return true;
                    // it worked! - remember the current port
                    //workingPort = probePort;
                }
            } catch (SocketTimeoutException ignore) {
            } catch (NoRouteToHostException e) {
                break;
            } //this means that the host is down
            catch (IOException e) {
                String msg = e.getMessage();

                // RST should result in ConnectException, but not all Java implementations respect that
                if (msg.contains(/*Connection*/"refused")) {
                    // we've got an RST packet from the host - it is alive
                    closeQuietly(socket);
                    return true;
                    //success(result, startTime);
                } else {
                    // this should result in NoRouteToHostException or ConnectException, but not all Java implementation respect that
                    if (msg.contains(/*No*/"route to host") || msg.contains(/*Host is*/"down") || msg.contains(/*Network*/"unreachable") || msg.contains(/*Socket*/"closed")) {
                        // host is down
                        break;
                    }
                }
            } finally {
                closeQuietly(socket);
            }
        }

        return false;
    }

}
