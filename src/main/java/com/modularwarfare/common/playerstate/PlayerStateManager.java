package com.modularwarfare.common.playerstate;

import java.util.HashMap;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.network.PacketPlayerState;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class PlayerStateManager {
    public static HashMap<String, PlayerState> playerStates = new HashMap<String, PlayerState>();
    public static PlayerState clientPlayerState = new PlayerState();

    public static PlayerState getPlayerState(EntityPlayer player) {
        return getPlayerState(player.getName());
    }

    public static PlayerState getPlayerState(String player) {
        PlayerState state = playerStates.get(player);
        if (state == null) {
            state = new PlayerState();
            playerStates.put(player, state);
        }
        return state;
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent event) {
        if (event.phase == Phase.START) {
            return;
        }
        playerStates.forEach((name, state) -> {
            EntityPlayerMP player=FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(name);
            if(player==null) {
                return;
            }
            AttributeModifier modifier=player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getModifier(state.speedModifier);
            if(modifier==null||modifier.getAmount()!=state.speedAmplifier-1) {
                if(modifier!=null) {
                    player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).removeModifier(modifier);
                }
                modifier=new AttributeModifier(state.speedModifier, "mwf-speed", state.speedAmplifier-1, 2);
                modifier.setSaved(false);
                player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).applyModifier(modifier);  
            }
            if (state.dirty) {
                ModularWarfare.NETWORK.sendTo(new PacketPlayerState(state),
                    player);
                state.dirty = false;
            }
        });
    }
}
