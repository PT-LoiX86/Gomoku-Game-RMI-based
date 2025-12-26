package com.caro.client.rmi;

import com.caro.common.service.ClientCallback;
import com.caro.common.service.GameService;
import com.caro.common.util.GameConstants;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RmiClientManager {
    private static RmiClientManager instance;
    
    private GameService gameService;
    private ClientCallback callbackStub;
    private String currentUsername;
    private ScheduledExecutorService heartbeatScheduler;

    private RmiClientManager() {}

    public static synchronized RmiClientManager getInstance() {
        if (instance == null) instance = new RmiClientManager();
        return instance;
    }

    public boolean connect(String serverAddress) {
        try {
            // ---------------------------------------------------------------
            // FIX: Set Hostname to LAN IP so Server can call us back!
            // ---------------------------------------------------------------
            String myIp = getLocalIpAddress();
            System.setProperty("java.rmi.server.hostname", myIp);
            System.out.println("Client configured to receive callbacks at: " + myIp);
            // ---------------------------------------------------------------

            Registry registry = LocateRegistry.getRegistry(serverAddress, GameConstants.RMI_PORT);
            gameService = (GameService) registry.lookup(GameConstants.RMI_ID);
            
            // Prepare the callback object
            ClientCallbackImpl callbackImpl = new ClientCallbackImpl();
            // When we export here, RMI uses the property we set above
            callbackStub = (ClientCallback) UnicastRemoteObject.exportObject(callbackImpl, 0);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean login(String username) {
        try {
            boolean success = gameService.login(username, callbackStub);
            if (success) {
                this.currentUsername = username;
                startHeartbeat();
            }
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- Heartbeat Sender ---
    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                if (gameService != null && currentUsername != null) {
                    gameService.sendHeartbeat(currentUsername);
                }
            } catch (Exception e) {
                System.err.println("Failed to send heartbeat: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    public void stop() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdownNow();
        }
        currentUsername = null;
    }

    public GameService getService() { return gameService; }
    public String getUsername() { return currentUsername; }

    // -----------------------------------------------------------------------
    // HELPER: Find correct LAN IP (Same logic as Server)
    // -----------------------------------------------------------------------
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLinkLocalAddress() && !addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }
}
