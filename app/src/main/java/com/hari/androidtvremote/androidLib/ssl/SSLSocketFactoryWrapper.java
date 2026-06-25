/*
 * Copyright (C) 2009 Google Inc.  All rights reserved.
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

package com.hari.androidtvremote.androidLib.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;


/**
 * A convenience wrapper to generate an {@link SSLSocketFactory} that uses the
 * {@link KeyManager} and {@link TrustManager} instances that are passed in.
 */
public class SSLSocketFactoryWrapper extends SSLSocketFactory {
  
  /**
   * The internal SSLSocketFactory which will be wrapped.
   */
  private SSLSocketFactory mFactory;

  public static SocketFactory getDefault() {
    throw new IllegalStateException("Not implemented.");
  }

  public SSLSocketFactoryWrapper() {
    throw new IllegalStateException("Not implemented.");
  }
  
  public SSLSocketFactoryWrapper(KeyManager[] keyManagers,
      TrustManager[] trustManagers) throws NoSuchAlgorithmException,
      KeyManagementException {
    java.security.Security.addProvider(
        new org.bouncycastle.jce.provider.BouncyCastleProvider());
    
    SSLContext sslcontext = SSLContext.getInstance("TLS");
    sslcontext.init(keyManagers, trustManagers, null);
    mFactory = sslcontext.getSocketFactory();
  }
  
  public static SSLSocketFactoryWrapper CreateWithDummyTrustManager(
      KeyManager[] keyManagers) throws KeyManagementException,
      NoSuchAlgorithmException {
    TrustManager[] trustManagers = { new DummyTrustManager() };
    return new SSLSocketFactoryWrapper(keyManagers, trustManagers);
  }

  @Override
  public Socket createSocket() throws IOException {
    return mFactory.createSocket();
  }


  @Override
  public Socket createSocket(InetAddress inaddr, int i)
      throws IOException {
    return mFactory.createSocket(inaddr, i);
  }

  @Override
  public Socket createSocket(InetAddress inaddr, int i,
      InetAddress inaddr1, int j) throws IOException {
    return mFactory.createSocket(inaddr, i, inaddr1, j);
  }

  @Override
  public Socket createSocket(Socket socket, String s, int i, boolean flag)
      throws IOException {
    return mFactory.createSocket(socket, s, i, flag);
  }

  @Override
  public Socket createSocket(String s, int i) throws IOException {
    return mFactory.createSocket(s, i);
  }

  @Override
  public Socket createSocket(String s, int i, InetAddress inaddr, int j)
      throws IOException {
    return mFactory.createSocket(s, i, inaddr, j);
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return mFactory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return mFactory.getSupportedCipherSuites();
  }
}
