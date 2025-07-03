 package mchhui.sizvehicle.network;

import mchhui.sizvehicle.network.client.PacketPlayerDriveInput;

public class ClientSIZVehicle {
     public static void uploadInput(float powerFactor, float angleFactor, boolean brake) {
         NetworkManager.getInstance().sendToServer(new PacketPlayerDriveInput(powerFactor, angleFactor, brake));
     }
}
