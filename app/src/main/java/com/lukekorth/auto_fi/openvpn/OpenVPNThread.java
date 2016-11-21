package com.lukekorth.auto_fi.openvpn;

import android.content.Context;

import com.lukekorth.auto_fi.utilities.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenVPNThread implements Runnable {

    private static final String DUMP_PATH_STRING = "Dump path: ";
    private static final int M_FATAL = (1 << 4);
    private static final int M_NONFATAL = (1 << 5);
    private static final int M_WARN = (1 << 6);
    private static final int M_DEBUG = (1 << 7);

    private Context mContext;
    private OpenVpn mService;
    private Process mProcess;
    private String mDumpPath;

    public OpenVPNThread(Context context, OpenVpn service) {
        mContext = context;
        mService = service;
    }

    @Override
    public void run() {
        try {
            Logger.info("Starting OpenVPN");
            runOpenVpn();
            Logger.info("OpenVPN exited");
        } catch (Exception e) {
            Logger.error("OpenVPN Thread Exception: " + e.getMessage());
        } finally {
            int exitvalue = 0;
            try {
                if (mProcess != null) {
                    exitvalue = mProcess.waitFor();
                }
            } catch (IllegalThreadStateException | InterruptedException e) {
                Logger.error(e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            if (exitvalue != 0) {
                Logger.error("Process exited with exit value " + exitvalue);
            }

            if (mDumpPath != null) {
                Logger.error("OpenVPN crashed unexpectedly");
            }

            mService.getVpnService().shutdown();
            Logger.info("Exiting OpenVPN");
        }
    }

    private void runOpenVpn() {
        String[] command = OpenVpnSetup.getOpenVpnCommand(mContext);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("LD_LIBRARY_PATH", generateLibraryPath(command, processBuilder));
        processBuilder.redirectErrorStream(true);
        try {
            mProcess = processBuilder.start();
            mProcess.getOutputStream().close();

            InputStream in = mProcess.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while (true) {
                String logline = br.readLine();
                if (logline == null) {
                    return;
                }

                if (logline.startsWith(DUMP_PATH_STRING)) {
                    mDumpPath = logline.substring(DUMP_PATH_STRING.length());
                }

                // 1380308330.240114 18000002 Send to HTTP proxy: 'X-Online-Host: bla.blabla.com'
                Pattern p = Pattern.compile("(\\d+).(\\d+) ([0-9a-f])+ (.*)");
                Matcher m = p.matcher(logline);
                if (m.matches()) {
                    int flags = Integer.parseInt(m.group(3), 16);
                    String msg = m.group(4);
                    if ((flags & M_FATAL) != 0) {
                        Logger.error(msg);
                    } else if ((flags & M_NONFATAL) != 0) {
                        Logger.warn(msg);
                    } else if ((flags & M_WARN) != 0) {
                        Logger.warn(msg);
                    } else if ((flags & M_DEBUG) != 0) {
                        Logger.debug(msg);
                    } else {
                        Logger.info(msg);
                    }
                } else {
                    Logger.info("P:" + logline);
                }

                if (Thread.interrupted()) {
                    throw new InterruptedException("OpenVPNThread was interrupted");
                }
            }
        } catch (IOException | InterruptedException e) {
            Logger.error("Error reading from output of OpenVPN process. " + e.getMessage());
            mProcess.destroy();
        }
    }

    private String generateLibraryPath(String[] argv, ProcessBuilder pb) {
        // Hack until I find a good way to get the real library path
        String applibpath = argv[0].replaceFirst("/cache/.*$", "/lib");

        String lbpath = pb.environment().get("LD_LIBRARY_PATH");
        if (lbpath == null) {
            lbpath = applibpath;
        } else {
            lbpath = applibpath + ":" + lbpath;
        }

        String nativeLibraryDir = mContext.getApplicationInfo().nativeLibraryDir;
        if (!applibpath.equals(nativeLibraryDir)) {
            lbpath = nativeLibraryDir + ":" + lbpath;
        }

        return lbpath;
    }
}
