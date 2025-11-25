package mchhui.mwfplugin;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.modularwarfare.BukkitEvents.BukkitEntityHeadShotEvent;
import com.modularwarfare.BukkitEvents.BukkitGunHitEntityEvent;
import com.modularwarfare.BukkitEvents.BukkitWeaponAttachmentEvent;

public class ExamplePlugin extends JavaPlugin implements Listener{
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        
    }
    
    @EventHandler
    public void onGunHitEntity(BukkitGunHitEntityEvent event) {
        getLogger().info("========== 武器命中实体事件 ==========");
        getLogger().info("射击者: " + (event.shooter != null ? event.shooter.getName() : "null"));
        getLogger().info("受害者: " + (event.victim != null ? event.victim.getName() : "null"));
        getLogger().info("武器ID: " + event.gunId);
        getLogger().info("命中部位: " + event.hitbox);
        getLogger().info("命中坐标 X: " + event.hitX);
        getLogger().info("命中坐标 Y: " + event.hitY);
        getLogger().info("命中坐标 Z: " + event.hitZ);
        getLogger().info("是否爆头: " + event.isHeadshot);
        getLogger().info("伤害: " + event.damage);
        getLogger().info("是否已取消: " + event.isCanceled);
        getLogger().info("=====================================");
    }
    
    @EventHandler
    public void onHeadshot(BukkitEntityHeadShotEvent event) {
        getLogger().info("========== 爆头事件 ==========");
        getLogger().info("受害者: " + (event.victim != null ? event.victim.getName() : "null"));
        getLogger().info("射击者: " + (event.shooter != null ? event.shooter.getName() : "null"));
        getLogger().info("=============================");
    }
    
    @EventHandler
    public void onAttachment(BukkitWeaponAttachmentEvent event) {
        getLogger().info("========== 武器配件事件 ==========");
        getLogger().info("玩家: " + (event.player != null ? event.player.getName() : "null"));
        getLogger().info("是否卸载: " + event.isUnload);
        getLogger().info("是否全部卸载: " + event.isUnloadAll);
        getLogger().info("卸载配件类型: " + (event.unloadAttachmentType != null ? event.unloadAttachmentType : "null"));
        getLogger().info("武器: " + (event.gun != null ? event.gun.toString() : "null"));
        getLogger().info("加载的配件: " + (event.loadAttach != null ? event.loadAttach.toString() : "null"));
        getLogger().info("是否已取消: " + event.isCanceled);
        getLogger().info("===================================");
    }
}
