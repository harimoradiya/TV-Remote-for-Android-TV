package com.hari.androidtvremote.androidLib.ssl;



import com.hari.androidtvremote.androidLib.AndroidRemoteContext;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

/* renamed from: sensustech.android.tv.remote.control.manager.keystore.KeyStoreManager */
/* loaded from: classes3.dex */
public final class KeyStoreManager {
    private static String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final boolean DEBUG = false;
    //    public static final String KEYSTORE_FILENAME = "androidtv.keystore";
//    static final char[] KEYSTORE_PASSWORD = "KeyStore_Password".toCharArray();
    private static final String LOCAL_IDENTITY_ALIAS = "androidtv-remote";
    private static final String REMOTE_IDENTITY_ALIAS_PATTERN = "androidtv-remote-%s";
    private static final String SERVER_IDENTITY_ALIAS = "androidtv-local";
    private static final String TAG = "KeyStoreManager";
    private DynamicTrustManager mDynamicTrustManager;
    private KeyStore mKeyStore;

    private AndroidRemoteContext androidRemoteContext = AndroidRemoteContext.getInstance();

    /* renamed from: sensustech.android.tv.remote.control.manager.keystore.KeyStoreManager$DynamicTrustManager */
    /* loaded from: classes3.dex */
    public static class DynamicTrustManager implements X509TrustManager {
        private X509TrustManager trustManager;

        @Override // javax.net.ssl.X509TrustManager
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public DynamicTrustManager(KeyStore keyStore) {
            reloadTrustManager(keyStore);
        }

        @Override // javax.net.ssl.X509TrustManager
        public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
            this.trustManager.checkClientTrusted(x509CertificateArr, str);
        }

        @Override // javax.net.ssl.X509TrustManager
        public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
            this.trustManager.checkServerTrusted(x509CertificateArr, str);
        }

        public void reloadTrustManager(KeyStore keyStore) {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                for (int i = 0; i < trustManagers.length; i++) {
                    if (trustManagers[i] instanceof X509TrustManager) {
                        this.trustManager = (X509TrustManager) trustManagers[i];
                        return;
                    }
                }
                throw new IllegalStateException("No trust manager found");
            } catch (KeyStoreException | NoSuchAlgorithmException unused) {
            }
        }
    }

    public KeyStoreManager() {
        KeyStore load = load();
        this.mKeyStore = load;
        this.mDynamicTrustManager = new DynamicTrustManager(load);
    }

    private void clearKeyStore() {
        try {
            Enumeration<String> aliases = this.mKeyStore.aliases();
            while (aliases.hasMoreElements()) {
                this.mKeyStore.deleteEntry(aliases.nextElement());
            }
        } catch (KeyStoreException unused) {
        }
        store();
    }

    private static String createAlias(String str) {
        return String.format(REMOTE_IDENTITY_ALIAS_PATTERN, str);
    }

    private void createIdentity(KeyStore keyStore) throws GeneralSecurityException {
        createIdentity(keyStore, SERVER_IDENTITY_ALIAS);
    }

    private void createIdentity(KeyStore keyStore, String str) throws GeneralSecurityException {
        createIdentity(keyStore, str, getUniqueId());
    }

    private void setLocale(Locale locale) {
        try {
            Locale.setDefault(locale);
        } catch (Exception unused) {
        }
    }

    private void createIdentity(KeyStore keyStore, String str, String str2) throws GeneralSecurityException {
        KeyPair generateKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        try {
            keyStore.setKeyEntry(str, generateKeyPair.getPrivate(), null, new Certificate[]{SslUtil.generateX509V3Certificate(generateKeyPair, getCertificateName(str2))});
        } catch (IllegalArgumentException unused) {
            Locale locale = Locale.getDefault();
            setLocale(Locale.ENGLISH);
            keyStore.setKeyEntry(str, generateKeyPair.getPrivate(), null, new Certificate[]{SslUtil.generateX509V3Certificate(generateKeyPair, getCertificateName(str2))});
            setLocale(locale);
        }
    }

    private KeyStore createIdentityKeyStore() throws GeneralSecurityException {
        KeyStore keyStore;
        if (!useAndroidKeyStore()) {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try {
                keyStore.load(null, androidRemoteContext.getKeyStorePass());
            } catch (IOException e) {
                throw new GeneralSecurityException("Unable to create empty keyStore", e);
            }
        } else {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            try {
                keyStore.load(null);
            } catch (IOException e2) {
                throw new GeneralSecurityException("Unable to create empty keyStore", e2);
            }
        }
        createIdentity(keyStore);
        return keyStore;
    }

    private static final String getCertificateName() {
        return getCertificateName(getUniqueId());
    }

    private static final String getCertificateName(String str) {
        return "CN=androidtv/livingTV";
    }

    private static String getSubjectDN(Certificate certificate) {
        X500Principal subjectX500Principal;
        if (!(certificate instanceof X509Certificate) || (subjectX500Principal = ((X509Certificate) certificate).getSubjectX500Principal()) == null) {
            return null;
        }
        return subjectX500Principal.getName();
    }

    private static final String getUniqueId() {
        return UUID.randomUUID().toString();
    }

    private boolean hasServerIdentityAlias(KeyStore keyStore) {
        try {
            return keyStore.containsAlias(SERVER_IDENTITY_ALIAS);
        } catch (KeyStoreException unused) {
            return false;
        }
    }

    private KeyStore load() {
        KeyStore keyStore;
        KeyStore keyStore2 = null;
        try {
            if (!useAndroidKeyStore()) {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(Files.newInputStream(androidRemoteContext.getKeyStoreFile().toPath()), androidRemoteContext.getKeyStorePass());
            } else {
                keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
                keyStore.load(null);
            }
            keyStore2 = keyStore;
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Unable to get default instance of KeyStore", e);
        } catch (IOException | GeneralSecurityException unused) {
        }
        if (keyStore2 == null || !hasServerIdentityAlias(keyStore2)) {
            try {
                KeyStore createIdentityKeyStore = createIdentityKeyStore();
                store(createIdentityKeyStore);
                return createIdentityKeyStore;
            } catch (GeneralSecurityException e2) {
                throw new IllegalStateException("Unable to create identity KeyStore", e2);
            }
        }
        return keyStore2;
    }

    private void store(KeyStore keyStore) {
        if (!useAndroidKeyStore()) {
            try {
                FileOutputStream openFileOutput = new FileOutputStream(androidRemoteContext.getKeyStoreFile());
                keyStore.store(openFileOutput, androidRemoteContext.getKeyStorePass());
                openFileOutput.close();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to store keyStore", e);
            } catch (GeneralSecurityException e2) {
                throw new IllegalStateException("Unable to store keyStore", e2);
            }
        }
    }

    private boolean useAndroidKeyStore() {
        return true;
    }

    public void clear() {
        clearKeyStore();
        try {
            createIdentity(this.mKeyStore);
        } catch (GeneralSecurityException unused) {
        }
        store();
    }

    public KeyManager[] getKeyManagers() throws GeneralSecurityException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(this.mKeyStore, "".toCharArray());
        return keyManagerFactory.getKeyManagers();
    }

    public TrustManager[] getTrustManagers() throws GeneralSecurityException {
        try {
            return new DynamicTrustManager[]{this.mDynamicTrustManager};
        } catch (Exception e) {
            throw new GeneralSecurityException(e);
        }
    }

    public boolean hasServerIdentityAlias() {
        return hasServerIdentityAlias(this.mKeyStore);
    }

    public void initializeKeyStore() {
        initializeKeyStore(getUniqueId());
    }

    public void initializeKeyStore(String str) {
        try {
            createIdentity(this.mKeyStore, LOCAL_IDENTITY_ALIAS, str);
            store();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to create identity KeyStore", e);
        }
    }

    public Certificate removeCertificate(String str) {
        try {
            String createAlias = createAlias(str);
            if (!this.mKeyStore.containsAlias(createAlias)) {
                return null;
            }
            Certificate certificate = this.mKeyStore.getCertificate(createAlias);
            this.mKeyStore.deleteEntry(createAlias);
            store();
            return certificate;
        } catch (KeyStoreException unused) {
            return null;
        }
    }

    public void store() {
        this.mDynamicTrustManager.reloadTrustManager(this.mKeyStore);
        store(this.mKeyStore);
    }

    public void storeCertificate(Certificate certificate) {
        storeCertificate(certificate, Integer.toString(certificate.hashCode()));
    }

    public void storeCertificate(Certificate certificate, String str) {
        try {
            String createAlias = createAlias(str);
            String subjectDN = getSubjectDN(certificate);
            if (this.mKeyStore.containsAlias(createAlias)) {
                this.mKeyStore.deleteEntry(createAlias);
            }
            if (subjectDN != null) {
                Enumeration<String> aliases = this.mKeyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String nextElement = aliases.nextElement();
                    String subjectDN2 = getSubjectDN(this.mKeyStore.getCertificate(nextElement));
                    if (subjectDN2 != null && subjectDN2.equals(subjectDN)) {
                        this.mKeyStore.deleteEntry(nextElement);
                    }
                }
            }
            this.mKeyStore.setCertificateEntry(createAlias, certificate);
            store();
        } catch (KeyStoreException unused) {
        }
    }
}