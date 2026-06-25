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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;


/**
 * An {@link SSLServerSocketFactory} that performs no verification on client
 * certificates; ie, is all-trusting.
 * 
 * @see DummyTrustManager
 */
public class DummySSLServerSocketFactory extends SSLServerSocketFactoryWrapper {

  DummySSLServerSocketFactory(KeyManager[] keyManagers,
      TrustManager[] trustManagers) throws KeyManagementException,
      NoSuchAlgorithmException {
    super(keyManagers, trustManagers);
  }

  public static DummySSLServerSocketFactory fromKeyManagers(
      KeyManager[] keyManagers) throws KeyManagementException,
      NoSuchAlgorithmException {
    TrustManager[] trustManagers = { new DummyTrustManager() };
    return new DummySSLServerSocketFactory(keyManagers, trustManagers);
  }

}
