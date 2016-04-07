package com.lukekorth.auto_fi.openvpn;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.lukekorth.auto_fi.BuildConfig;
import com.lukekorth.auto_fi.R;

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
import java.util.Vector;

public class OpenVpnManagementThread implements Runnable, OpenVPNManagement {

    private static final String TAG = "openvpn";
    private LocalSocket mSocket;
    private OpenVPNService mOpenVPNService;
    private LinkedList<FileDescriptor> mFDList = new LinkedList<>();
    private LocalServerSocket mServerSocket;
    private boolean mWaitingForRelease = false;
    private long mLastHoldRelease = 0;

    private static final Vector<OpenVpnManagementThread> active = new Vector<>();

    private PauseReason lastPauseReason = PauseReason.NO_NETWORK;
    private PausedStateCallback mPauseCallback;
    private boolean mShuttingDown;

    public OpenVpnManagementThread(OpenVPNService openVPNService) {
        mOpenVPNService = openVPNService;
    }

    public boolean openManagementInterface(Context context) {
        String socketName = (context.getCacheDir().getAbsolutePath() + "/" + "mgmtsocket");

        int tries = 8;
        LocalSocket serverSocketLocal = new LocalSocket();
        while (tries > 0 && !serverSocketLocal.isBound()) {
            try {
                serverSocketLocal.bind(new LocalSocketAddress(socketName, LocalSocketAddress.Namespace.FILESYSTEM));
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
            VpnStatus.logException(e);
        }

        return false;
    }

    public void managementCommand(String cmd) {
        try {
            if (mSocket != null && mSocket.getOutputStream() != null) {
                mSocket.getOutputStream().write(cmd.getBytes());
                mSocket.getOutputStream().flush();
            }
        } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        byte[] buffer = new byte[2048];

        String pendingInput = "";
        synchronized (active) {
            active.add(this);
        }

        try {
            mSocket = mServerSocket.accept();
            InputStream instream = mSocket.getInputStream();
            mServerSocket.close();

            while (true) {
                int numbytesread = instream.read(buffer);
                if (numbytesread == -1) {
                    return;
                }

                FileDescriptor[] fds = null;
                try {
                    fds = mSocket.getAncillaryFileDescriptors();
                } catch (IOException e) {
                    VpnStatus.logException("Error reading fds from socket", e);
                }

                if (fds != null) {
                    Collections.addAll(mFDList, fds);
                }

                String input = new String(buffer, 0, numbytesread, "UTF-8");

                pendingInput += input;
                pendingInput = processInput(pendingInput);
            }
        } catch (IOException e) {
            if (!e.getMessage().equals("socket closed") && !e.getMessage().equals("Connection reset by peer")) {
                VpnStatus.logException(e);
            }
        }

        synchronized (active) {
            active.remove(this);
        }
    }

    private void protectFileDescriptor(FileDescriptor fd) {
        try {
            Method getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
            int fdint = (Integer) getInt.invoke(fd);

            boolean result = mOpenVPNService.protect(fdint);
            if (!result) {
                VpnStatus.logWarning("Could not protect VPN socket");
            }

            NativeUtils.jniclose(fdint);
            return;
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException | NullPointerException e) {
            VpnStatus.logException("Failed to retrieve fd from socket (" + fd + ")", e);
        }

        Log.d("Openvpn", "Failed to retrieve fd from socket: " + fd);
    }

    private String processInput(String pendingInput) {
        while (pendingInput.contains("\n")) {
            String[] tokens = pendingInput.split("\\r?\\n", 2);
            processCommand(tokens[0]);
            if (tokens.length == 1) {
                // No second part, newline was at the end
                pendingInput = "";
            } else {
                pendingInput = tokens[1];
            }
        }

        return pendingInput;
    }

    private void processCommand(String command) {
        if (command.startsWith(">") && command.contains(":")) {
            String[] parts = command.split(":", 2);
            String cmd = parts[0].substring(1);
            String argument = parts[1];

            switch (cmd) {
                case "INFO": case "BYTECOUNT":
                    // ignore
                    return;
                case "HOLD":
                    handleHold();
                    break;
                case "NEED-OK":
                    processNeedCommand(argument);
                    break;
                case "STATE":
                    if (!mShuttingDown)
                        processState(argument);
                    break;
                case "PROXY":
                    processProxyCMD(argument);
                    break;
                case "LOG":
                    processLogMessage(argument);
                    break;
                default:
                    VpnStatus.logWarning("MGMT: Got unrecognized command" + command);
                    Log.i(TAG, "Got unrecognized command" + command);
                    break;
            }
        } else if (command.startsWith("SUCCESS:")) {
            // ignore
        } else if (command.startsWith("PROTECTFD: ")) {
            FileDescriptor fdtoprotect = mFDList.pollFirst();
            if (fdtoprotect != null) {
                protectFileDescriptor(fdtoprotect);
            }
        } else {
            Log.i(TAG, "Got unrecognized line from managment" + command);
            VpnStatus.logWarning("MGMT: Got unrecognized line from management:" + command);
        }
    }

    private void processLogMessage(String argument) {
        String[] args = argument.split(",", 4);
        // 0 unix time stamp
        // 1 log level N,I,E etc.
                /*
                  (b) zero or more message flags in a single string:
          I -- informational
          F -- fatal error
          N -- non-fatal error
          W -- warning
          D -- debug, and
                 */
        // 2 log message

        Log.d("OpenVPN", argument);

        VpnStatus.LogLevel level;
        switch (args[1]) {
            case "I":
                level = VpnStatus.LogLevel.INFO;
                break;
            case "W":
                level = VpnStatus.LogLevel.WARNING;
                break;
            case "D":
                level = VpnStatus.LogLevel.VERBOSE;
                break;
            case "F":
                level = VpnStatus.LogLevel.ERROR;
                break;
            default:
                level = VpnStatus.LogLevel.INFO;
                break;
        }

        int ovpnlevel = Integer.parseInt(args[2]) & 0x0F;
        String msg = args[3];

        if (msg.startsWith("MANAGEMENT: CMD")) {
            ovpnlevel = Math.max(4, ovpnlevel);
        }

        VpnStatus.logMessageOpenVPN(level, ovpnlevel, msg);
    }

    boolean shouldBeRunning() {
        return mPauseCallback != null && mPauseCallback.shouldBeRunning();
    }

    private void handleHold() {
        if (shouldBeRunning()) {
            releaseHoldCmd();
        } else {
            mWaitingForRelease = true;

            VpnStatus.updateStatePause(lastPauseReason);
        }
    }

    private void releaseHoldCmd() {
        if ((System.currentTimeMillis() - mLastHoldRelease) < 5000) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}
        }

        mWaitingForRelease = false;
        mLastHoldRelease = System.currentTimeMillis();
        managementCommand("hold release\n");
        managementCommand("bytecount " + mBytecountInterval + "\n");
        managementCommand("state on\n");
    }

    public void releaseHold() {
        if (mWaitingForRelease) {
            releaseHoldCmd();
        }
    }

    private void processProxyCMD(String argument) {
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

            VpnStatus.logInfo(R.string.using_proxy, isa.getHostName(), isa.getPort());

            String proxycmd = String.format(Locale.ENGLISH, "proxy HTTP %s %d\n", isa.getHostName(), isa.getPort());
            managementCommand(proxycmd);
        } else {
            managementCommand("proxy NONE\n");
        }
    }

    private void processState(String argument) {
        String[] args = argument.split(",", 3);
        String currentstate = args[1];

        if (args[2].equals(",,")) {
            VpnStatus.updateStateString(currentstate, "");
        } else {
            VpnStatus.updateStateString(currentstate, args[2]);
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
                mOpenVPNService.addDNS(extra);
                break;
            case "DNSDOMAIN":
                mOpenVPNService.setDomain(extra);
                break;
            case "ROUTE":
                String[] routeparts = extra.split(" ");

                if (routeparts.length == 5) {
                    if (BuildConfig.DEBUG) Assert.assertEquals("dev", routeparts[3]);
                    mOpenVPNService.addRoute(routeparts[0], routeparts[1], routeparts[2], routeparts[4]);
                } else if (routeparts.length >= 3) {
                    mOpenVPNService.addRoute(routeparts[0], routeparts[1], routeparts[2], null);
                } else {
                    VpnStatus.logError("Unrecognized ROUTE cmd:" + Arrays.toString(routeparts) + " | " + argument);
                }

                break;
            case "ROUTE6":
                String[] routeparts6 = extra.split(" ");
                mOpenVPNService.addRoutev6(routeparts6[0], routeparts6[1]);
                break;
            case "IFCONFIG":
                String[] ifconfigparts = extra.split(" ");
                int mtu = Integer.parseInt(ifconfigparts[2]);
                mOpenVPNService.setLocalIP(ifconfigparts[0], ifconfigparts[1], mtu, ifconfigparts[3]);
                break;
            case "IFCONFIG6":
                mOpenVPNService.setLocalIPv6(extra);
                break;
            case "PERSIST_TUN_ACTION":
                status = mOpenVPNService.getTunReopenStatus();
                break;
            case "OPENTUN":
                if (sendTunFD(needed, extra)) {
                    return;
                } else {
                    status = "cancel";
                }
                break;
            default:
                Log.e(TAG, "Unknown needok command " + argument);
                return;
        }

        String cmd = String.format("needok '%s' %s\n", needed, status);
        managementCommand(cmd);
    }

    private boolean sendTunFD(String needed, String extra) {
        if (!extra.equals("tun")) {
            VpnStatus.logError(String.format("Device type %s requested, but only tun is accepted", extra));
            return false;
        }

        ParcelFileDescriptor pfd = mOpenVPNService.openTun();
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
            managementCommand(cmd);

            // Set the FileDescriptor to null to stop this mad behavior
            mSocket.setFileDescriptorsForSend(null);

            pfd.close();

            return true;
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException |
                IOException | IllegalAccessException exp) {
            VpnStatus.logException("Could not send fd over socket", exp);
        }

        return false;
    }

    private static boolean stopOpenVPN() {
        synchronized (active) {
            boolean sendCMD = false;
            for (OpenVpnManagementThread mt : active) {
                mt.managementCommand("signal SIGINT\n");
                sendCMD = true;
                try {
                    if (mt.mSocket != null) {
                        mt.mSocket.close();
                    }
                } catch (IOException ignored) {}
            }

            return sendCMD;
        }
    }

    @Override
    public void networkChange(boolean samenetwork) {
        if (mWaitingForRelease) {
            releaseHold();
        } else if (samenetwork) {
            managementCommand("network-change samenetwork\n");
        } else {
            managementCommand("network-change\n");
        }
    }

    @Override
    public void setPauseCallback(PausedStateCallback callback) {
        mPauseCallback = callback;
    }

    public void signalusr1() {
        if (!mWaitingForRelease) {
            managementCommand("signal SIGUSR1\n");
        } else {
            // If signalusr1 is called update the state string
            // if there is another for stopping
            VpnStatus.updateStatePause(lastPauseReason);
        }
    }

    public void reconnect() {
        signalusr1();
        releaseHold();
    }

    @Override
    public void pause(PauseReason reason) {
        lastPauseReason = reason;
        signalusr1();
    }

    @Override
    public void resume() {
        releaseHold();
        lastPauseReason = PauseReason.NO_NETWORK;
    }

    @Override
    public boolean stopVPN(boolean replaceConnection) {
        mShuttingDown = true;
        return stopOpenVPN();
    }
}
