package com.lukekorth.auto_fi.openvpn;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import com.lukekorth.auto_fi.BuildConfig;
import com.lukekorth.auto_fi.R;
import com.lukekorth.auto_fi.models.DataUsage;
import com.lukekorth.auto_fi.network.ProxyDetection;
import com.lukekorth.auto_fi.utilities.Logger;

import junit.framework.Assert;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;

class OpenVpnManagementThread implements Runnable {

    private static final int BYTE_COUNT_INTERVAL = 2;

    private LocalSocket mSocket;
    private OpenVpn mOpenVpn;
    private LinkedList<FileDescriptor> mFDList = new LinkedList<>();
    private LocalServerSocket mServerSocket;
    private long mPreviousKilobytesUsed = 0;
    private boolean mShuttingDown;

    OpenVpnManagementThread(OpenVpn openVpn) {
        mOpenVpn = openVpn;
    }

    boolean openManagementConnection(Context context) {
        int tries = 8;
        String socketName = context.getCacheDir().getAbsolutePath() + "/mgmtsocket";
        LocalSocket serverSocketLocal = new LocalSocket();
        while (tries > 0 && !serverSocketLocal.isBound()) {
            try {
                serverSocketLocal.bind(new LocalSocketAddress(socketName,
                        LocalSocketAddress.Namespace.FILESYSTEM));
            } catch (IOException e) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {}
            }

            tries--;
        }

        try {
            mServerSocket = new LocalServerSocket(serverSocketLocal.getFileDescriptor());
            return true;
        } catch (IOException e) {
            Logger.error(e);
            return false;
        }
    }

    void stopVPN() {
        mShuttingDown = true;

        sendManagementCommand("signal SIGINT\n");
        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        try {
            mSocket = mServerSocket.accept();
            InputStream inputStream = mSocket.getInputStream();
            mServerSocket.close();

            byte[] buffer = new byte[2048];
            while (!mShuttingDown) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    return;
                }

                FileDescriptor[] fds = null;
                try {
                    fds = mSocket.getAncillaryFileDescriptors();
                } catch (IOException e) {
                    Logger.error("Error reading fds from socket." + e.getMessage());
                }

                if (fds != null) {
                    Collections.addAll(mFDList, fds);
                }

                processInput(new String(buffer, 0, bytesRead, "UTF-8"));
            }
        } catch (IOException e) {
            if (!e.getMessage().equals("socket closed") && !e.getMessage().equals("Connection reset by peer")) {
                Logger.error(e);
            }
        }
    }

    private void protectFileDescriptor(FileDescriptor fd) {
        try {
            Method getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
            int fdint = (Integer) getInt.invoke(fd);

            boolean result = mOpenVpn.getVpnService().protect(fdint);
            if (!result) {
                Logger.warn("Could not protect VPN socket");
            }

            NativeMethods.jniclose(fdint);
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException |
                IllegalAccessException | NullPointerException e) {
            Logger.error("Failed to retrieve fd from socket (" + fd + ")." + e.getMessage());
        }
    }

    private void processInput(String input) {
        while (input.contains("\n")) {
            String[] tokens = input.split("\\r?\\n", 2);
            processCommand(tokens[0]);
            if (tokens.length == 1) {
                input = "";
            } else {
                input = tokens[1];
            }
        }
    }

    private void processCommand(String command) {
        if (command.startsWith(">") && command.contains(":")) {
            String[] parts = command.split(":", 2);
            String cmd = parts[0].substring(1);
            String argument = parts[1];

            switch (cmd) {
                case "INFO":
                    // ignore
                    break;
                case "BYTECOUNT":
                    processByteCount(argument);
                    break;
                case "HOLD":
                    handleHold(argument);
                    break;
                case "NEED-OK":
                    processNeedCommand(argument);
                    break;
                case "STATE":
                    if (!mShuttingDown) {
                        processState(argument);
                    }
                    break;
                case "PROXY":
                    processProxyCommand(argument);
                    break;
                case "LOG":
                    processLogMessage(argument);
                    break;
                default:
                    Logger.warn("MGMT: Got unrecognized command" + command);
                    break;
            }
        } else if (command.startsWith("PROTECTFD: ")) {
            FileDescriptor fdtoprotect = mFDList.pollFirst();
            if (fdtoprotect != null) {
                protectFileDescriptor(fdtoprotect);
            }
        } else if (!command.startsWith("SUCCESS:")) {
            Logger.warn("MGMT: Got unrecognized line from management: " + command);
        }
    }

    /**
     * @param argument Format of >BYTECOUNT:{BYTES_IN},{BYTES_OUT}
     */
    private void processByteCount(String argument) {
        int comma = argument.indexOf(',');
        long kilobytes = Long.parseLong(argument.substring(0, comma)) / 1024;
        kilobytes += Long.parseLong(argument.substring(comma + 1)) / 1024;

        if (kilobytes - mPreviousKilobytesUsed > 1024) {
            DataUsage.addUsage(kilobytes - mPreviousKilobytesUsed);
            mPreviousKilobytesUsed = kilobytes;
        }
    }

    private void processLogMessage(String argument) {
        String[] args = argument.split(",", 4);
        switch (args[1]) {
            case "W":
                Logger.warn(args[3]);
                break;
            case "D":
                Logger.debug(args[3]);
                break;
            case "F":
                Logger.error(args[3]);
                break;
            default:
                Logger.info(args[3]);
                break;
        }
    }

    private void handleHold(String argument) {
        int waitTime = 0;
        try {
            waitTime = Integer.parseInt(argument.split(":")[1]);
        } catch (Exception ignored) {}
        if (waitTime > 1) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    releaseHoldCommand();
                }
            }, waitTime * 1000);
        } else {
            releaseHoldCommand();
        }
    }

    private void releaseHoldCommand() {
        sendManagementCommand("hold release\n");
        sendManagementCommand("bytecount " + BYTE_COUNT_INTERVAL + "\n");
        sendManagementCommand("state on\n");
    }

    private void processProxyCommand(String argument) {
        String[] args = argument.split(",", 3);
        SocketAddress proxyaddr = ProxyDetection.detectProxy();

        if (args.length >= 2) {
            String proto = args[1];
            if (proto.equals("UDP")) {
                proxyaddr = null;
            }
        }

        if (proxyaddr instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) proxyaddr;
            String proxycmd = String.format(Locale.ENGLISH, "proxy HTTP %s %d\n", isa.getHostName(), isa.getPort());
            sendManagementCommand(proxycmd);

            Logger.info("Using proxy " + isa.getHostName() + " " + isa.getPort());
        } else {
            sendManagementCommand("proxy NONE\n");
        }
    }

    private void processState(String argument) {
        String[] args = argument.split(",", 3);
        String currentState = args[1];

        mOpenVpn.getVpnService().setNotificationMessage(getLocalizedState(currentState));

        switch (currentState) {
            case "CONNECTED":
                mOpenVpn.getVpnService().successfullyConnected();
                break;
            case "DISCONNECTED":
                checkWifiConnection();
                break;
            case "RECONNECTING":
                checkWifiConnection();
                break;
        }
    }

    private void checkWifiConnection() {
        if (!mOpenVpn.getVpnService().getWifiHelper().isConnectedToWifi()) {
            Logger.debug("Disconnected or reconnecting VPN with no wifi connection. Stopping VPN.");
            stopVPN();
        }
    }

    private void processNeedCommand(String argument) {
        int p1 = argument.indexOf('\'');
        int p2 = argument.indexOf('\'', p1 + 1);

        String needed = argument.substring(p1 + 1, p2);
        String extra = argument.split(":", 2)[1];

        String status = "ok";

        switch (needed) {
            case "PROTECTFD":
                FileDescriptor fdtoprotect = mFDList.pollFirst();
                protectFileDescriptor(fdtoprotect);
                break;
            case "DNSSERVER":
            case "DNS6SERVER":
                mOpenVpn.addDNS(extra);
                break;
            case "DNSDOMAIN":
                mOpenVpn.setDomain(extra);
                break;
            case "ROUTE":
                String[] routeparts = extra.split(" ");

                if (routeparts.length == 5) {
                    if (BuildConfig.DEBUG) Assert.assertEquals("dev", routeparts[3]);
                    mOpenVpn.addRoute(routeparts[0], routeparts[1], routeparts[2], routeparts[4]);
                } else if (routeparts.length >= 3) {
                    mOpenVpn.addRoute(routeparts[0], routeparts[1], routeparts[2], null);
                } else {
                    Logger.error("Unrecognized ROUTE cmd:" + Arrays.toString(routeparts) + " | " + argument);
                }

                break;
            case "ROUTE6":
                String[] routeparts6 = extra.split(" ");
                mOpenVpn.addRouteV6(routeparts6[0], routeparts6[1]);
                break;
            case "IFCONFIG":
                String[] ifconfigparts = extra.split(" ");
                int mtu = Integer.parseInt(ifconfigparts[2]);
                mOpenVpn.setLocalIP(ifconfigparts[0], ifconfigparts[1], mtu, ifconfigparts[3]);
                break;
            case "IFCONFIG6":
                mOpenVpn.setLocalIPv6(extra);
                break;
            case "PERSIST_TUN_ACTION":
                status = mOpenVpn.getTunReopenStatus();
                break;
            case "OPENTUN":
                if (sendTunFD(needed, extra)) {
                    return;
                } else {
                    status = "cancel";
                }
                break;
            default:
                Logger.error("Unknown needok command " + argument);
                return;
        }

        String cmd = String.format("needok '%s' %s\n", needed, status);
        sendManagementCommand(cmd);
    }

    private boolean sendTunFD(String needed, String extra) {
        if (!extra.equals("tun")) {
            Logger.error("Device type " + extra + " requested, but only tun is accepted");
            return false;
        }

        ParcelFileDescriptor pfd = mOpenVpn.openTun();
        if (pfd == null) {
            return false;
        }

        Method setInt;
        int fdint = pfd.getFd();
        try {
            setInt = FileDescriptor.class.getDeclaredMethod("setInt$", int.class);
            FileDescriptor fdtosend = new FileDescriptor();

            setInt.invoke(fdtosend, fdint);

            FileDescriptor[] fds = {fdtosend};
            mSocket.setFileDescriptorsForSend(fds);

            // Trigger a send so we can close the fd on our side of the channel
            // The API documentation fails to mention that it will not reset the file descriptor to
            // be send and will happily send the file descriptor on every write ...
            String cmd = String.format("needok '%s' %s\n", needed, "ok");
            sendManagementCommand(cmd);

            // Set the FileDescriptor to null to stop this behavior
            mSocket.setFileDescriptorsForSend(null);

            pfd.close();

            return true;
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException |
                IOException | IllegalAccessException exp) {
            Logger.error("Could not send fd over socket" + exp.getMessage());
        }

        return false;
    }

    private void sendManagementCommand(String command) {
        try {
            if (mSocket != null && mSocket.getOutputStream() != null) {
                mSocket.getOutputStream().write(command.getBytes());
                mSocket.getOutputStream().flush();
            }
        } catch (IOException ignored) {}
    }

    private int getLocalizedState(String state) {
        switch (state) {
            case "CONNECTING":
                return R.string.state_connecting;
            case "WAIT":
                return R.string.state_wait;
            case "AUTH":
                return R.string.state_auth;
            case "GET_CONFIG":
                return R.string.state_get_config;
            case "ASSIGN_IP":
                return R.string.state_assign_ip;
            case "ADD_ROUTES":
                return R.string.state_add_routes;
            case "CONNECTED":
                return R.string.state_connected;
            case "DISCONNECTED":
                return R.string.state_disconnected;
            case "RECONNECTING":
                return R.string.state_reconnecting;
            case "EXITING":
                return R.string.state_exiting;
            case "RESOLVE":
                return R.string.state_resolve;
            case "TCP_CONNECT":
                return R.string.state_tcp_connect;
            default:
                return R.string.unknown_state;
        }
    }
}
