package com.amazon.whisperlink.port.android.transport;

import android.content.Context;
import com.amazon.whisperlink.android.util.DeviceUtil;
import com.amazon.whisperlink.android.util.RouteUtil;
import com.amazon.whisperlink.annotation.Concurrency;
import com.amazon.whisperlink.service.Device;
import com.amazon.whisperlink.service.Route;
import com.amazon.whisperlink.settings.ConnectionSettings;
import com.amazon.whisperlink.transport.TCommunicationChannelFactory;
import com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory;
import com.amazon.whisperlink.transport.TransportFeatures;
import com.amazon.whisperlink.transport.TransportOptions;
import com.amazon.whisperlink.util.Log;
import com.amazon.whisperlink.util.NetworkStateSnapshot;
import com.amazon.whisperlink.util.StringUtil;
import com.amazon.whisperlink.util.ThreadUtils;
import com.amazon.whisperlink.util.WhisperLinkUtil;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: WhisperPlay.jar:com/amazon/whisperlink/port/android/transport/TExternalSocketFactory.class */
public class TExternalSocketFactory implements TExternalCommunicationChannelFactory {
    private static final String TAG = "TExternalSocketFactory";
    private static final int PRIORITY = 0;
    private static final String SECURE_PORT = "securePort";
    private static final String UNSECURE_PORT = "unsecurePort";
    public static final String COMM_CHANNEL_ID = "inet";
    private TransportFeatures features;
    public Route inetRoute;
    private Context context;
    private static final long refreshInetRouteTimeout = 100;
    @Concurrency.GuardedBy("this")
    protected boolean started;
    protected ConnectionSettings inetConnectionSettings;
    protected final Object inetRouteLock = new Object();
    @Concurrency.GuardedBy("inetRouteLock")
    private int unsecureServerSocketPort = -1;
    @Concurrency.GuardedBy("this")
    private Future<Route> refreshInetRouteFuture = null;

    public TExternalSocketFactory(Context ctx, ConnectionSettings inetConnectionSettings) {
        this.inetConnectionSettings = inetConnectionSettings;
        this.context = ctx.getApplicationContext();
    }

    @Override // com.amazon.whisperlink.transport.TCommunicationChannelFactory
    public String getCommunicationChannelId() {
        return "inet";
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public TServerTransport getSecureServerTransport() throws TTransportException {
        throw new TTransportException("Secure server transport not supported");
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public TTransport getSecureTransport(TransportOptions options) throws TTransportException {
        throw new TTransportException("Secure transport not supported");
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public TServerTransport getServerTransport() throws TTransportException {
        TServerSocket unsecureServerSocket;
        int originalPort = this.unsecureServerSocketPort;
        synchronized (this.inetRouteLock) {
            try {
                unsecureServerSocket = new TServerSocket(this.unsecureServerSocketPort > 0 ? this.unsecureServerSocketPort : 0, this.inetConnectionSettings.getReadTimeOut());
            } catch (TTransportException tte) {
                Log.info(TAG, "Exception when attempting to get secure server socket on port :" + this.unsecureServerSocketPort + ". Creating socket on new port.", tte);
                this.unsecureServerSocketPort = -1;
                unsecureServerSocket = new TServerSocket(0, this.inetConnectionSettings.getReadTimeOut());
            }
            this.unsecureServerSocketPort = unsecureServerSocket.getServerSocket().getLocalPort();
            Log.info(TAG, "Server Transport created on port :" + this.unsecureServerSocketPort);
        }
        if (originalPort != this.unsecureServerSocketPort) {
            submitRefreshInetRouteTask();
        }
        return unsecureServerSocket;
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public TTransport getTransport(TransportOptions options) throws TTransportException {
        if (options == null) {
            throw new TTransportException("No transport options specified");
        }
        Route route = options.getConnInfo();
        if (route == null) {
            throw new TTransportException("Route not supported for this device");
        }
        String ipv4 = route.ipv4;
        String ipv6 = route.ipv6;
        if (StringUtil.isEmpty(ipv4) && StringUtil.isEmpty(ipv6)) {
            return null;
        }
        if (!StringUtil.isEmpty(ipv4)) {
            return new TSocket(ipv4, route.getUnsecurePort(), options.getConnectTimeout(), options.getReadTimeout());
        }
        if (!StringUtil.isEmpty(ipv6)) {
            return new TSocket(ipv6, route.getUnsecurePort(), options.getConnectTimeout(), options.getReadTimeout());
        }
        return null;
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public void updateTransport(TTransport transport, TransportOptions options) {
        if (transport instanceof TSocket) {
            TSocket socket = (TSocket) transport;
            socket.setReadTimeout(options.getReadTimeout());
            Log.debug(TAG, "updateTransport(): read timeout is " + options.getReadTimeout());
            return;
        }
        Log.warning(TAG, "updateTransport(): transport is not a TSocket");
    }

    @Override // com.amazon.whisperlink.transport.TCommunicationChannelFactory
    public boolean isDiscoverable() {
        return true;
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public synchronized Route getLocalConnInfo() {
        if (this.refreshInetRouteFuture == null || this.refreshInetRouteFuture.isCancelled()) {
            Log.warning(TAG, "Inet route refresh task cancelled or hasn't been scheduled");
            submitRefreshInetRouteTask();
        }
        try {
            return this.refreshInetRouteFuture.get(refreshInetRouteTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.warning(TAG, "Inet route refresh task interrupted");
            return null;
        } catch (CancellationException e2) {
            Log.warning(TAG, "Inet route refresh task cancelled");
            return null;
        } catch (ExecutionException e3) {
            Log.warning(TAG, "Inet route refresh task execution exception");
            return null;
        } catch (TimeoutException e4) {
            Log.warning(TAG, "Inet route refresh task timed out");
            return null;
        }
    }

    @Override // com.amazon.whisperlink.transport.TCommunicationChannelFactory
    public void start() {
        synchronized (this) {
            if (!this.started) {
                this.started = true;
                submitRefreshInetRouteTask();
            }
        }
    }

    @Override // com.amazon.whisperlink.transport.TCommunicationChannelFactory
    public void stop() {
        synchronized (this) {
            if (this.started) {
                this.started = false;
                cancelRefreshInetRouteTaskIfNeeded();
            }
        }
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public void onNetworkEvent(NetworkStateSnapshot networkStateSnapshot) {
        if (networkStateSnapshot.isWifiOrEthernetConnected()) {
            synchronized (this) {
                if (!this.started) {
                    Log.debug(TAG, "Skip inet route refreshing if socket factory is not started");
                    return;
                } else {
                    submitRefreshInetRouteTask();
                    return;
                }
            }
        }
        cancelRefreshInetRouteTaskIfNeeded();
    }

    synchronized void cancelRefreshInetRouteTaskIfNeeded() {
        if (this.refreshInetRouteFuture != null) {
            Log.debug(TAG, "Cancel the existing task of refreshing route info");
            this.refreshInetRouteFuture.cancel(true);
            this.refreshInetRouteFuture = null;
        }
    }

    protected synchronized void submitRefreshInetRouteTask() {
        cancelRefreshInetRouteTaskIfNeeded();
        Log.debug(TAG, "Submitting a new task to refresh inet route info");
        this.refreshInetRouteFuture = ThreadUtils.submitToWorker(TAG, new RefreshInetRouteCallable());
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public boolean isChannelReady() {
        return getLocalConnInfo() != null;
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public boolean isAvailableOnSleep() {
        return false;
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public String getConnectionMetadata(Route route) {
        JSONObject connectionMetadataJson = new JSONObject();
        try {
            connectionMetadataJson.put(UNSECURE_PORT, route.getUnsecurePort());
            connectionMetadataJson.put(SECURE_PORT, route.getSecurePort());
        } catch (JSONException e) {
            Log.error(TAG, "Could not create connection metadata", e);
        }
        return connectionMetadataJson.toString();
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public Route getRouteFromConnectionMetadata(String connectionMetadata, TTransport transport) {
        if (StringUtil.isEmpty(connectionMetadata)) {
            Log.warning(TAG, "Empty connection metadata. Cannot create route.");
            return null;
        }
        try {
            JSONObject connectionMetadataJson = new JSONObject(connectionMetadata);
            Route routeToRemoteDevice = new Route();
            String ipAddress = transport.getRemoteEndpointIdentifier();
            if (ipAddress == null) {
                throw new IllegalArgumentException("Could not obtain IP for remote device");
            }
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            if (inetAddress instanceof Inet6Address) {
                routeToRemoteDevice.setIpv6(ipAddress);
            } else {
                routeToRemoteDevice.setIpv4(ipAddress);
            }
            routeToRemoteDevice.setUnsecurePort(connectionMetadataJson.getInt(UNSECURE_PORT));
            routeToRemoteDevice.setSecurePort(connectionMetadataJson.getInt(SECURE_PORT));
            return routeToRemoteDevice;
        } catch (UnknownHostException e) {
            Log.error(TAG, "Could not construct InetAddress", e);
            return null;
        } catch (JSONException e2) {
            Log.error(TAG, "Could not parse connection metadata", e2);
            return null;
        }
    }

    @Override // com.amazon.whisperlink.transport.TCommunicationChannelFactory
    public TransportFeatures getTransportFeatures() {
        if (this.features == null) {
            this.features = new TransportFeatures();
            this.features.setPriority(0);
        }
        return this.features;
    }

    @Override // java.lang.Comparable
    public int compareTo(TCommunicationChannelFactory target) {
        return getTransportFeatures().compareTo(target.getTransportFeatures());
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public Route parseRoute(String connInfo) throws TTransportException {
        if (StringUtil.isEmpty(connInfo)) {
            return null;
        }
        URI uri = URI.create(connInfo);
        if (!getCommunicationChannelId().equals(uri.getScheme())) {
            throw new TTransportException("Failed to parse connection information. Communication channel id :" + uri.getScheme() + " is not supported");
        }
        String remoteDeviceUUID = uri.getHost();
        Device device = WhisperLinkUtil.getDevice(remoteDeviceUUID);
        if (device == null || device.getRoutes() == null || !device.getRoutes().containsKey("inet")) {
            throw new TTransportException("Device :" + remoteDeviceUUID + " does not have " + getCommunicationChannelId() + "route for direct connection");
        }
        Route inetRoute = device.getRoutes().get("inet");
        Route route = new Route(inetRoute);
        if (SECURE_PORT.equals(uri.getFragment())) {
            route.setUnsecurePort(-1);
            route.setSecurePort(uri.getPort());
        } else {
            route.setUnsecurePort(uri.getPort());
            route.setSecurePort(-1);
        }
        return route;
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public String getLocalTransportConnInfo(TTransport transport) throws TTransportException {
        throw new TTransportException("Operation not yet implemented");
    }

    @Override // com.amazon.whisperlink.transport.TExternalCommunicationChannelFactory
    public String getServerTransportConnInfo(TServerTransport tServerTransport, boolean isSecure) throws TTransportException {
        if (tServerTransport == null || !(tServerTransport instanceof TServerSocket)) {
            throw new TTransportException("Unsupported class for TServerTransport");
        }
        try {
            int port = ((TServerSocket) tServerTransport).getServerSocket().getLocalPort();
            String fragment = isSecure ? SECURE_PORT : UNSECURE_PORT;
            return new URI(getCommunicationChannelId(), null, WhisperLinkUtil.getLocalDeviceUUID(), port, null, null, fragment).toString();
        } catch (URISyntaxException exception) {
            Log.error(TAG, "Could not create the direct application connection info", exception);
            throw new TTransportException("Could not get connection information from the server transport");
        }
    }

    Route getCurrentInetRoute() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (RouteUtil.supportInterface(networkInterface.getName())) {
                    byte[] hardwareAddress = networkInterface.getHardwareAddress();
                    if (hardwareAddress != null) {
                        String ipv4 = getIpv4AddressFromInterface(networkInterface);
                        if (!StringUtil.isEmpty(ipv4) || !StringUtil.isEmpty(null)) {
                            String macAddr = DeviceUtil.getMacAddress(hardwareAddress);
                            Route newRoute = setupNewRoute(macAddr, ipv4, null);
                            CloudInetUri inetUri = new CloudInetUri(newRoute, this.context);
                            newRoute.setUri(inetUri.getUri());
                            Log.debug(TAG, "Current SSID=" + inetUri.getSsid());
                            Log.info(TAG, "Valid inet route retrived on interface " + networkInterface.getName());
                            return newRoute;
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.wtf(TAG, "Can't find local address", e);
        }
        Log.warning(TAG, "No valid inet route available");
        return null;
    }

    protected Route setupNewRoute(String macAddr, String ipv4, String ipv6) {
        Route newRoute = new Route();
        newRoute.setHardwareAddr(macAddr);
        newRoute.setIpv4(ipv4);
        newRoute.setIpv6(ipv6);
        synchronized (this.inetRouteLock) {
            newRoute.setUnsecurePort(this.unsecureServerSocketPort);
        }
        return newRoute;
    }

    private String getIpv4AddressFromInterface(NetworkInterface networkInterface) {
        Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses();
        while (enumIpAddr.hasMoreElements()) {
            InetAddress inetAddress = enumIpAddr.nextElement();
            if (!isInternalIpAddress(inetAddress)) {
                String address = inetAddress.getHostAddress();
                int index = address.lastIndexOf(networkInterface.getName());
                if (index != -1) {
                    address = address.substring(0, index);
                }
                if (inetAddress instanceof Inet4Address) {
                    return address;
                }
            }
        }
        return null;
    }

    private boolean isInternalIpAddress(InetAddress inetAddress) {
        return inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: WhisperPlay.jar:com/amazon/whisperlink/port/android/transport/TExternalSocketFactory$RefreshInetRouteCallable.class */
    public class RefreshInetRouteCallable implements Callable<Route> {
        private RefreshInetRouteCallable() {
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.util.concurrent.Callable
        public Route call() throws Exception {
            return TExternalSocketFactory.this.getCurrentInetRoute();
        }
    }
}