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

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.security.auth.x500.X500Principal;

/**
 * A collection of miscellaneous utility functions for use in Polo.
 */
public class SslUtil {

	/**
	 * Generates a new RSA key pair.
	 * 
	 * @return the new object
	 * @throws NoSuchAlgorithmException
	 *             if the RSA generator could not be loaded
	 */
	public static KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator kg = KeyPairGenerator.getInstance("RSA");
		KeyPair kp = kg.generateKeyPair();
		return kp;
	}

	/**
	 * Creates a new, empty {@link KeyStore}
	 * 
	 * @return the new KeyStore
	 * @throws GeneralSecurityException
	 *             on error creating the keystore
	 * @throws IOException
	 *             on error loading the keystore
	 */
	public static KeyStore getEmptyKeyStore() throws GeneralSecurityException,
			IOException {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		return ks;
	}

	/**
	 * Generates a new, self-signed X509 V1 certificate for a KeyPair.
	 * 
	 * @param pair
	 *            the {@link KeyPair} to be used
	 * @param name
	 *            X.500 distinguished name
	 * @return the new certificate
	 * @throws GeneralSecurityException
	 *             on error generating the certificate
	 */
	@SuppressWarnings("deprecation")
	public static X509Certificate generateX509V1Certificate(KeyPair pair,
			String name) throws GeneralSecurityException {
		java.security.Security
				.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		Calendar calendar = Calendar.getInstance();
		calendar.set(2009, 0, 1);
		Date startDate = new Date(calendar.getTimeInMillis());
		calendar.set(2029, 0, 1);
		Date expiryDate = new Date(calendar.getTimeInMillis());

		BigInteger serialNumber = BigInteger.valueOf(Math.abs(System
				.currentTimeMillis()));

		X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
		X500Principal dnName = new X500Principal(name);
		certGen.setSerialNumber(serialNumber);
		certGen.setIssuerDN(dnName);
		certGen.setNotBefore(startDate);
		certGen.setNotAfter(expiryDate);
		certGen.setSubjectDN(dnName); // note: same as issuer
		certGen.setPublicKey(pair.getPublic());
		certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

		// This method is deprecated, but Android Eclair does not provide the
		// generate() methods.
		X509Certificate cert = certGen.generate(pair.getPrivate(), "BC");
		return cert;
	}

	/**
	 * Generates a new, self-signed X509 V3 certificate for a KeyPair.
	 * 
	 * @param pair
	 *            the {@link KeyPair} to be used
	 * @param name
	 *            X.500 distinguished name
	 * @param notBefore
	 *            not valid before this date
	 * @param notAfter
	 *            not valid after this date
	 * @param serialNumber
	 *            serial number
	 * @return the new certificate
	 * @throws GeneralSecurityException
	 *             on error generating the certificate
	 */
	@SuppressWarnings("deprecation")
	public static X509Certificate generateX509V3Certificate(KeyPair pair,
			String name, Date notBefore, Date notAfter, BigInteger serialNumber)
			throws GeneralSecurityException {
		java.security.Security
				.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		org.bouncycastle.x509.X509V3CertificateGenerator certGen = new org.bouncycastle.x509.X509V3CertificateGenerator();
		X500Name dnName = new X500Name(name);
		X500Principal principal = new X500Principal(name);

		certGen.setSerialNumber(serialNumber);
		// certGen.setIssuerDN(dnName);
		// certGen.setSubjectDN(dnName); // note: same as issuer
		certGen.setIssuerDN(principal);
		certGen.setSubjectDN(principal);
		certGen.setNotBefore(notBefore);
		certGen.setNotAfter(notAfter);
		certGen.setPublicKey(pair.getPublic());
		certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

		// For self-signed certificates, OpenSSL 0.9.6 has specific requirements
		// about certificate and extension content. Quoting the `man verify`:
		//
		// In OpenSSL 0.9.6 and later all certificates whose subject name
		// matches
		// the issuer name of the current certificate are subject to further
		// tests. The relevant authority key identifier components of the
		// current
		// certificate (if present) must match the subject key identifier (if
		// present) and issuer and serial number of the candidate issuer, in
		// addition the keyUsage extension of the candidate issuer (if present)
		// must permit certificate signing.
		//
		// In the code that follows,
		// - the KeyUsage extension permits cert signing (KeyUsage.keyCertSign);
		// - the Authority Key Identifier extension is added, matching the
		// subject key identifier, and using the issuer, and serial number.

		certGen.addExtension(X509Extension.basicConstraints, true,
				new BasicConstraints(false));

		certGen.addExtension(X509Extension.keyUsage, true, new KeyUsage(
				KeyUsage.digitalSignature | KeyUsage.keyEncipherment
						| KeyUsage.keyCertSign));
		certGen.addExtension(X509Extension.extendedKeyUsage, true,
				new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));

		AuthorityKeyIdentifier authIdentifier = createAuthorityKeyIdentifier(
				pair.getPublic(), dnName, serialNumber);

		certGen.addExtension(X509Extension.authorityKeyIdentifier, true,
				authIdentifier);
		// certGen.addExtension(X509Extension.subjectKeyIdentifier, true,
		// new SubjectKeyIdentifier(pair.getPublic()));

		certGen.addExtension(X509Extension.subjectAlternativeName, false,
				new GeneralNames(new GeneralName(GeneralName.rfc822Name,
						"googletv@test.test")));

		// This method is deprecated, but Android Eclair does not provide the
		// generate() methods.
		//X509Certificate cert = certGen.generate(pair.getPrivate(), "BC");
		X509Certificate cert = certGen.generate(pair.getPrivate()/*, String.valueOf(Security.getProvider("BC"))*/);
		return cert;
	}

	/**
	 * Creates an AuthorityKeyIdentifier from a public key, name, and serial
	 * number.
	 * <p>
	 * {@link AuthorityKeyIdentifierStructure} is <i>almost</i> perfect for
	 * this, but sadly it does not have a constructor suitable for us:
	 * {@link AuthorityKeyIdentifierStructure#AuthorityKeyIdentifierStructure(PublicKey)}
	 * does not set the serial number or name (which is important to us), while
	 * {@link AuthorityKeyIdentifierStructure#AuthorityKeyIdentifierStructure(X509Certificate)}
	 * sets those fields but needs a completed certificate to do so.
	 * <p>
	 * This method addresses the gap in available {@link AuthorityKeyIdentifier}
	 * constructors provided by BouncyCastle; its implementation is derived from
	 * {@link AuthorityKeyIdentifierStructure#AuthorityKeyIdentifierStructure(X509Certificate)}.
	 * 
	 * @param publicKey
	 *            the public key
	 * @param dnName
	 *            the name
	 * @param serialNumber
	 *            the serial number
	 * @return a new {@link AuthorityKeyIdentifier}
	 */
	private static AuthorityKeyIdentifier createAuthorityKeyIdentifier(
			PublicKey publicKey, X500Name dnName,
			BigInteger serialNumber) {
		GeneralName genName = new GeneralName(dnName);
		SubjectPublicKeyInfo info;
		try {
			info = new SubjectPublicKeyInfo((ASN1Sequence) new ASN1InputStream(
					publicKey.getEncoded()).readObject());
		} catch (IOException e) {
			throw new RuntimeException("Error encoding public key");
		}
		return new AuthorityKeyIdentifier(info, new GeneralNames(genName),
				serialNumber);
	}

	/**
	 * Wrapper for
	 * {@link SslUtil#generateX509V3Certificate(KeyPair, String, Date, Date, BigInteger)}
	 * which uses a default validity period and serial number.
	 * <p>
	 * The validity period is Jan 1 2009 - Jan 1 2099. The serial number is the
	 * current system time.
	 */
	public static X509Certificate generateX509V3Certificate(KeyPair pair,
			String name) throws GeneralSecurityException {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2009, 0, 1);
		Date notBefore = new Date(calendar.getTimeInMillis());
		calendar.set(2099, 0, 1);
		Date notAfter = new Date(calendar.getTimeInMillis());

		BigInteger serialNumber = BigInteger.valueOf(Math.abs(System
				.currentTimeMillis()));

		return generateX509V3Certificate(pair, name, notBefore, notAfter,
				serialNumber);
	}

	/**
	 * Wrapper for
	 * {@link SslUtil#generateX509V3Certificate(KeyPair, String, Date, Date, BigInteger)}
	 * which uses a default validity period.
	 * <p>
	 * The validity period is Jan 1 2009 - Jan 1 2099.
	 */
	public static X509Certificate generateX509V3Certificate(KeyPair pair,
			String name, BigInteger serialNumber)
			throws GeneralSecurityException {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2009, 0, 1);
		Date notBefore = new Date(calendar.getTimeInMillis());
		calendar.set(2099, 0, 1);
		Date notAfter = new Date(calendar.getTimeInMillis());

		return generateX509V3Certificate(pair, name, notBefore, notAfter,
				serialNumber);
	}

	/**
	 * Generates a new {@code SSLContext} suitable for a test environment.
	 * <p>
	 * A new {@link KeyPair}, {@link X509Certificate}, {@link DummyTrustManager}
	 * , and an empty {@link KeyStore} are created and used to initialize the
	 * context.
	 * 
	 * @return the new context
	 * @throws GeneralSecurityException
	 *             if an error occurred during initialization
	 * @throws IOException
	 *             if an empty KeyStore could not be generated
	 */
	public SSLContext generateTestSslContext() throws GeneralSecurityException,
			IOException {
		SSLContext sslcontext = SSLContext.getInstance("SSLv3");
		KeyManager[] keyManagers = SslUtil.generateTestServerKeyManager(
				"SunX509", "test");
		sslcontext.init(keyManagers,
				new TrustManager[] { new DummyTrustManager() }, null);
		return sslcontext;
	}

	/**
	 * Creates a new pain of {@link KeyManager}s, backed by a keystore file.
	 * 
	 * @param keyManagerInstanceName
	 *            name of the {@link KeyManagerFactory} to request
	 * @param fileName
	 *            the name of the keystore to load
	 * @param password
	 *            the password for the keystore
	 * @return the new object
	 * @throws GeneralSecurityException
	 *             if an error occurred during initialization
	 * @throws IOException
	 *             if the keystore could not be loaded
	 */
	public static KeyManager[] getFileBackedKeyManagers(
			String keyManagerInstanceName, String fileName, String password)
			throws GeneralSecurityException, IOException {
		KeyManagerFactory km = KeyManagerFactory
				.getInstance(keyManagerInstanceName);
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(new FileInputStream(fileName), password.toCharArray());
		km.init(ks, password.toCharArray());
		return km.getKeyManagers();
	}

	/**
	 * Creates a pair of {@link KeyManager}s suitable for use in testing.
	 * <p>
	 * A new {@link KeyPair} and {@link X509Certificate} are created and used to
	 * initialize the KeyManager.
	 * 
	 * @param keyManagerInstanceName
	 *            name of the {@link KeyManagerFactory}
	 * @param password
	 *            password to apply to the new key store
	 * @return the new key managers
	 * @throws GeneralSecurityException
	 *             if an error occurred during initialization
	 * @throws IOException
	 *             if the keystore could not be generated
	 */
	public static KeyManager[] generateTestServerKeyManager(
			String keyManagerInstanceName, String password)
			throws GeneralSecurityException, IOException {
		KeyManagerFactory km = KeyManagerFactory
				.getInstance(keyManagerInstanceName);
		KeyPair pair = SslUtil.generateRsaKeyPair();
		X509Certificate cert = SslUtil.generateX509V1Certificate(pair,
				"CN=Test Server Cert");
		Certificate[] chain = { cert };

		KeyStore ks = SslUtil.getEmptyKeyStore();
		ks.setKeyEntry("test-server", pair.getPrivate(),
				password.toCharArray(), chain);
		km.init(ks, password.toCharArray());
		return km.getKeyManagers();
	}

}
