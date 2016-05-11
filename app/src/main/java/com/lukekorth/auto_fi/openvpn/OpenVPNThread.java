package com.lukekorth.auto_fi.openvpn;

import android.util.Log;

import com.lukekorth.auto_fi.utilities.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenVPNThread implements Runnable {

    private static final String DUMP_PATH_STRING = "Dump path: ";
    private static final String TAG = "OpenVPN";
    public static final int M_FATAL = (1 << 4);
    public static final int M_NONFATAL = (1 << 5);
    public static final int M_WARN = (1 << 6);
    public static final int M_DEBUG = (1 << 7);
    private String[] mArgv;
    private Process mProcess;
    private String mNativeDir;
    private OpenVpn mService;
    private String mDumpPath;
    private Map<String, String> mProcessEnv;

    public OpenVPNThread(OpenVpn service, String[] argv, Map<String, String> processEnv, String nativelibdir) {
        mArgv = argv;
        mNativeDir = nativelibdir;
        mService = service;
        mProcessEnv = processEnv;
    }

    public void stopProcess() {
        mProcess.destroy();
    }

    @Override
    public void run() {
        try {
            Log.i(TAG, "Starting openvpn");
            startOpenVPNThreadArgs(mArgv, mProcessEnv);
            Log.i(TAG, "OpenVPN process exited");
        } catch (Exception e) {
            Logger.error("Starting OpenVPN Thread " + e.getMessage());
        } finally {
            int exitvalue = 0;
            try {
                if (mProcess != null) {
                    exitvalue = mProcess.waitFor();
                }
            } catch (IllegalThreadStateException ite) {
                Logger.error("Illegal Thread state: " + ite.getMessage());
            } catch (InterruptedException ie) {
                Logger.error("InterruptedException: " + ie.getMessage());
            }

            if (exitvalue != 0) {
                Logger.error("Process exited with exit value " + exitvalue);
            }

            if (mDumpPath != null) {
                Logger.error("OpenVPN crashed unexpectedly.");
            }

            mService.getVpnService().shutdown();
            Log.i(TAG, "Exiting");
        }
    }

    private void startOpenVPNThreadArgs(String[] argv, Map<String, String> env) {
        LinkedList<String> argvlist = new LinkedList<String>();

        Collections.addAll(argvlist, argv);

        ProcessBuilder pb = new ProcessBuilder(argvlist);

        String lbpath = genLibraryPath(argv, pb);

        pb.environment().put("LD_LIBRARY_PATH", lbpath);

        // Add extra variables
        for (Entry<String, String> e : env.entrySet()) {
            pb.environment().put(e.getKey(), e.getValue());
        }
        pb.redirectErrorStream(true);
        try {
            mProcess = pb.start();
            // Close the output, since we don't need it
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
                    int logLevel = flags & 0x0F;

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
            }
        } catch (IOException e) {
            Logger.error("Error reading from output of OpenVPN process. " + e.getMessage());
            stopProcess();
        }
    }

    private String genLibraryPath(String[] argv, ProcessBuilder pb) {
        // Hack until I find a good way to get the real library path
        String applibpath = argv[0].replaceFirst("/cache/.*$", "/lib");

        String lbpath = pb.environment().get("LD_LIBRARY_PATH");
        if (lbpath == null) {
            lbpath = applibpath;
        } else {
            lbpath = applibpath + ":" + lbpath;
        }

        if (!applibpath.equals(mNativeDir)) {
            lbpath = mNativeDir + ":" + lbpath;
        }

        return lbpath;
    }
}
