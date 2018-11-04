package com.studio4plus.homerplayer.util;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


// From: https://blog.dev-area.net/2015/08/13/android-4-1-enable-tls-1-1-and-tls-1-2/
public class TlsSSLSocketFactory extends SSLSocketFactory {

    @NonNull
    private final SSLSocketFactory factory;

    public TlsSSLSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        factory = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException {
        return enableTLSOnSocket(factory.createSocket(socket, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTLSOnSocket(factory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress inetAddress, int localPort)
            throws IOException, UnknownHostException {
        return enableTLSOnSocket(factory.createSocket(host, port, inetAddress, localPort));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int port) throws IOException {
        return enableTLSOnSocket(factory.createSocket(inetAddress, port));
    }

    @Override
    public Socket createSocket(
            InetAddress inetAddress, int port, InetAddress localInetAddress, int localPort)
            throws IOException {
        return enableTLSOnSocket(
                factory.createSocket(inetAddress, port, localInetAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if(socket instanceof SSLSocket) {
            ((SSLSocket)socket).setEnabledProtocols(new String[] {"TLSv1.1", "TLSv1.2"});
        }
        return socket;
    }
}
