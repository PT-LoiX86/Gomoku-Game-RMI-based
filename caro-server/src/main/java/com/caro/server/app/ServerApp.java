package com.caro.server.app;

import com.caro.common.util.GameConstants;
import com.caro.server.service.GameServiceImpl;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;

import com.caro.server.manager.HeartbeatMonitor;

public class ServerApp {
    public static void main(String[] args) {
        try {
            String hostIp = getLocalIpAddress();
            System.setProperty("java.rmi.server.hostname", hostIp);

            // 1. Create the Implementation
            GameServiceImpl gameService = new GameServiceImpl();
            
            // 2. Start the RMI Registry
            Registry registry = LocateRegistry.createRegistry(GameConstants.RMI_PORT);
            
            // 3. Bind the service
            registry.rebind(GameConstants.RMI_ID, gameService);
            
            System.out.println("Server is running on port " + GameConstants.RMI_PORT);
            
            HeartbeatMonitor monitor = new HeartbeatMonitor(gameService);
            monitor.start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                
                // Filter out loopback (127.0.0.1) and inactive interfaces
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    
                    // We only want IPv4 (e.g., 192.168.1.50), not IPv6
                    if (!addr.isLinkLocalAddress() && !addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Fallback if no LAN IP found
        return "127.0.0.1";
    }
}
