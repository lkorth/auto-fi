package de.blinkt.openvpn.core;

public interface OpenVPNManagement {

    interface PausedStateCallback {
        boolean shouldBeRunning();
    }

    enum PauseReason {
        NO_NETWORK
    }

    int mBytecountInterval = 2;

    void reconnect();

    void pause(PauseReason reason);

    void resume();

    /**
     * @param replaceConnection True if the VPN is connected by a new connection.
     * @return true if there was a process that has been send a stop signal
     */
    boolean stopVPN(boolean replaceConnection);

    /*
     * Rebind the interface
     */
    void networkChange(boolean sameNetwork);

    void setPauseCallback(PausedStateCallback callback);
}
