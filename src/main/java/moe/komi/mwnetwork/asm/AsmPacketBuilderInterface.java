package moe.komi.mwnetwork.asm;

import com.modularwarfare.common.network.PacketAimingRequest;
import com.modularwarfare.common.network.PacketBase;

public interface AsmPacketBuilderInterface {
    public PacketBase newPacket();
}