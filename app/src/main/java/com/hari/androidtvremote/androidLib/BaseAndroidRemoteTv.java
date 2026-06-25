package com.hari.androidtvremote.androidLib;

import java.io.File;

abstract class BaseAndroidRemoteTv {

    private final AndroidRemoteContext androidRemoteContext;

    BaseAndroidRemoteTv() {
        androidRemoteContext = AndroidRemoteContext.getInstance();
    }


    public String getServiceName() {
        return androidRemoteContext.getServiceName();
    }

    public void setServiceName(String serviceName) {
        androidRemoteContext.setServiceName(serviceName);
    }

    public String getClientName() {
        return androidRemoteContext.getClientName();
    }

    public void setClientName(String clientName) {
        androidRemoteContext.setClientName(clientName);
    }

    public File getKeyStoreFile() {
        return androidRemoteContext.getKeyStoreFile();
    }

    public void setKeyStoreFile(File keyStoreFile) {
        androidRemoteContext.setKeyStoreFile(keyStoreFile);
    }

    public char[] getKeyStorePass() {
        return androidRemoteContext.getKeyStorePass();
    }

    public void setKeyStorePass(String keyStorePass) {
        androidRemoteContext.setKeyStorePass(keyStorePass.toCharArray());
    }
}
