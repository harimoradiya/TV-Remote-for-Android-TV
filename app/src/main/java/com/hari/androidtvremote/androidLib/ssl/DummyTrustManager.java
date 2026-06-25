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

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * A trust manager that accepts all X509 client and server certificates.
 * 
 * @see "http://java.sun.com/products/javamail/SSLNOTES.txt"
 */
public class DummyTrustManager implements X509TrustManager {

  public void checkClientTrusted(X509Certificate[] chain, String authType) {
    // Does not throw CertificateException: all chains trusted
    return;
  }

  public void checkServerTrusted(X509Certificate[] chain, String authType) {
    // Does not throw CertificateException: all chains trusted
    return;
  }

  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[0];
  }

}
