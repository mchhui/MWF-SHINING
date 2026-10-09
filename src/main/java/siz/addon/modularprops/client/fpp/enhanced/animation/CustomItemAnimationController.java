package siz.addon.modularprops.client.fpp.enhanced.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundEvent;
import siz.addon.modularprops.client.fpp.enhanced.AnimationCustomItemType;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig;
import siz.addon.modularprops.common.custom.CustomItemType;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.common.guns.WeaponSoundType;

/**
 * 自定义道具动画控制器
 */
public class CustomItemAnimationController {
    
    private AnimationCustomItemType currentAnimation = AnimationCustomItemType.DRAW;
    private double animationTime = 0.0;
    private CustomItemEnhancedRenderConfig config;
    private CustomItemType itemType;
    
    // 动画状态
    public double DEFAULT = 0.0;
    public double DRAW = 0.0;
    public double SPRINT = 0.0;
    public double SPRINT_LOOP = 0.0;
    public double SPRINT_RANDOM = 0.0;
    public double INSPECT = 1.0;
    public double USE = 1.0;
    public double ATTACK = 1.0;
    
    // Sprint 冷却时间
    private long sprintCoolTime = 0;
    private long sprintLoopCoolTime = 0;
    private boolean isJumping = false;
    
    // 是否正在播放 draw 动画
    private boolean isDrawing = false;
    
    // 音效相关（参考 AnimationController）
    public boolean hasPlayedDrawSound = false;
    public ISound inspectSound = null;
    public ISound attackSound = null;
    public ISound useSound = null;
    private double lastDefault = 0.0; // 用于跟踪 DEFAULT 循环
    private boolean hasPlayedInspectSound = false; // 用于跟踪 INSPECT 音效是否已播放
    private boolean hasPlayedAttackSound = false; // 用于跟踪 ATTACK 音效是否已播放
    private boolean hasPlayedUseSound = false; // 用于跟踪 USE 音效是否已播放
    
    public CustomItemAnimationController(CustomItemEnhancedRenderConfig config, CustomItemType itemType) {
        this.config = config;
        this.itemType = itemType;
        this.isDrawing = true; // 初始状态为 draw
    }
    
    /**
     * 每帧更新动画状态
     */
    public void onTickRender(float stepTick, EntityPlayerSP player) {
        // 更新 DEFAULT 动画
        if (config.customAnimations.containsKey(AnimationCustomItemType.DEFAULT)) {
            double defaultSpeed = config.customAnimations.get(AnimationCustomItemType.DEFAULT).getSpeed(config.FPS) * stepTick;
            DEFAULT = Math.max(0.0, DEFAULT + defaultSpeed);
            if (DEFAULT > 1.0) {
                DEFAULT = 0.0;
            }
        }
        
        // DEFAULT 动画音效（参考 AnimationController）
        // 当 DEFAULT 从 > 0 循环回 0 时播放音效
        if (DEFAULT == 0.0 && lastDefault > 0.0 && DRAW >= 1.0 && itemType != null && itemType.weaponSoundMap != null) {
            if (player == Minecraft.getMinecraft().player) {
                itemType.playClientSound(player, WeaponSoundType.Idle);
            }
        }
        lastDefault = DEFAULT;
        
        // 更新 DRAW 动画
        if (config.customAnimations.containsKey(AnimationCustomItemType.DRAW)) {
            double drawSpeed = config.customAnimations.get(AnimationCustomItemType.DRAW).getSpeed(config.FPS) * stepTick;
            DRAW = Math.max(0.0, DRAW + drawSpeed);
            if (DRAW > 1.0) {
                DRAW = 1.0;
                isDrawing = false; // Draw 动画完成
            }
        } else {
            isDrawing = false; // 没有 draw 动画，直接完成
        }
        
        // DRAW 动画音效（参考 AnimationController）
        if (!hasPlayedDrawSound && DRAW > 0.0 && itemType != null && itemType.weaponSoundMap != null) {
            if (player == Minecraft.getMinecraft().player) {
                if (Minecraft.getMinecraft().currentScreen == null) {
                    SoundEvent se = itemType.getSound(player, WeaponSoundType.Draw);
                    if (se != null) {
                        ISound drawSound = PositionedSoundRecord.getRecord(se, 1, 1);
                        Minecraft.getMinecraft().getSoundHandler().playSound(drawSound);
                    }
                }
                hasPlayedDrawSound = true;
            }
        }
        
        // 更新 SPRINT 动画（混合效果，参考近战武器）
        if (config.customAnimations.containsKey(AnimationCustomItemType.SPRINT)) {
            float moveDistance = player.distanceWalkedModified - player.prevDistanceWalkedModified;
            long time = System.currentTimeMillis();
            double sprintSpeed = Math.sin(SPRINT * 3.14) * 0.09f;
            if (sprintSpeed < 0.03f) {
                sprintSpeed = 0.03f;
            }
            sprintSpeed *= stepTick;
            double sprintValue = 0;
            
            if (player.movementInput.jump) {
                isJumping = true;
            } else if (player.onGround) {
                isJumping = false;
            }
            
            boolean flag = (player.onGround || player.fallDistance < 2f) && !isJumping;
            
            if (player.isSprinting() && moveDistance > 0.05 && flag) {
                if (time > sprintCoolTime) {
                    sprintValue = SPRINT + sprintSpeed;
                }
            } else {
                sprintCoolTime = time + 100;
                sprintValue = SPRINT - sprintSpeed;
            }
            if (ATTACK < 1.0 || INSPECT < 1.0 || USE < 1.0) {
                sprintValue = SPRINT - sprintSpeed * 2.5f;
            }
            
            SPRINT = Math.max(0.0, Math.min(1.0, sprintValue));
            
            // SPRINT_LOOP
            double sprintLoopSpeed = config.customAnimations.get(AnimationCustomItemType.SPRINT).getSpeed(config.FPS) * stepTick
                    * (moveDistance / 0.15f);
            boolean flagSprintRand = false;
            if (flag) {
                if (time > sprintLoopCoolTime) {
                    if (player.isSprinting()) {
                        SPRINT_LOOP += sprintLoopSpeed;
                        SPRINT_RANDOM += sprintLoopSpeed;
                        flagSprintRand = true;
                    }
                }
            } else {
                sprintLoopCoolTime = time + 100;
            }
            if (!flagSprintRand) {
                SPRINT_RANDOM -= config.customAnimations.get(AnimationCustomItemType.SPRINT).getSpeed(config.FPS) * 3 * stepTick;
            }
            if (SPRINT_LOOP > 1.0) {
                SPRINT_LOOP = 0.0;
            }
            if (SPRINT_RANDOM > 1.0) {
                SPRINT_RANDOM = 0.0;
            }
            if (SPRINT_RANDOM < 0.0) {
                SPRINT_RANDOM = 0.0;
            }
            if (Double.isNaN(SPRINT_RANDOM)) {
                SPRINT_RANDOM = 0.0;
            }
        }
        
        // 更新 INSPECT 动画（参考 AnimationController）
        if (INSPECT == 0.0) {
            // INSPECT 动画开始时播放音效（只播放一次）
            if (DRAW >= 1.0 && !hasPlayedInspectSound && itemType != null && itemType.weaponSoundMap != null && player == Minecraft.getMinecraft().player) {
                if (inspectSound != null) {
                    Minecraft.getMinecraft().getSoundHandler().stopSound(inspectSound);
                    inspectSound = null;
                }
                SoundEvent se = itemType.getSound(player, WeaponSoundType.Inspect);
                if (se != null) {
                    inspectSound = PositionedSoundRecord.getRecord(se, 1, 1);
                    Minecraft.getMinecraft().getSoundHandler().playSound(inspectSound);
                    hasPlayedInspectSound = true;
                }
            }
        }
        if (INSPECT == 1.0) {
            // INSPECT 动画结束时停止音效并重置标志
            if (inspectSound != null) {
                Minecraft.getMinecraft().getSoundHandler().stopSound(inspectSound);
                inspectSound = null;
            }
            hasPlayedInspectSound = false;
        }
        if (!config.customAnimations.containsKey(AnimationCustomItemType.INSPECT)) {
            INSPECT = 1.0;
        } else {
            double inspectSpeed = config.customAnimations.get(AnimationCustomItemType.INSPECT).getSpeed(config.FPS) * stepTick;
            INSPECT += inspectSpeed;
            if (INSPECT >= 1.0) {
                INSPECT = 1.0;
            }
        }
        
        // 更新 USE 动画（参考 INSPECT 音效逻辑）
        if (USE == 0.0) {
            // USE 动画开始时播放音效（只播放一次）
            if (DRAW >= 1.0 && !hasPlayedUseSound && itemType != null && itemType.weaponSoundMap != null && player == Minecraft.getMinecraft().player) {
                if (useSound != null) {
                    Minecraft.getMinecraft().getSoundHandler().stopSound(useSound);
                    useSound = null;
                }
                SoundEvent se = itemType.getSound(player, WeaponSoundType.CustomItemUse);
                if (se != null) {
                    useSound = PositionedSoundRecord.getRecord(se, 1, 1);
                    Minecraft.getMinecraft().getSoundHandler().playSound(useSound);
                    hasPlayedUseSound = true;
                }
            }
        }
        if (USE == 1.0) {
            // USE 动画结束时停止音效并重置标志
            if (useSound != null) {
                Minecraft.getMinecraft().getSoundHandler().stopSound(useSound);
                useSound = null;
            }
            hasPlayedUseSound = false;
        }
        if (config.customAnimations.containsKey(AnimationCustomItemType.USE)) {
            double useSpeed = config.customAnimations.get(AnimationCustomItemType.USE).getSpeed(config.FPS) * stepTick;
            USE += useSpeed;
            if (USE >= 1.0) {
                USE = 1.0;
            }
        } else {
            USE = 1.0;
        }
        
        // 更新 ATTACK 动画（参考 INSPECT 音效逻辑）
        if (ATTACK == 0.0) {
            // ATTACK 动画开始时播放音效（只播放一次）
            if (DRAW >= 1.0 && !hasPlayedAttackSound && itemType != null && itemType.weaponSoundMap != null && player == Minecraft.getMinecraft().player) {
                if (attackSound != null) {
                    Minecraft.getMinecraft().getSoundHandler().stopSound(attackSound);
                    attackSound = null;
                }
                SoundEvent se = itemType.getSound(player, WeaponSoundType.CustomItemAttack);
                if (se != null) {
                    attackSound = PositionedSoundRecord.getRecord(se, 1, 1);
                    Minecraft.getMinecraft().getSoundHandler().playSound(attackSound);
                    hasPlayedAttackSound = true;
                }
            }
        }
        if (ATTACK == 1.0) {
            // ATTACK 动画结束时停止音效并重置标志
            if (attackSound != null) {
                Minecraft.getMinecraft().getSoundHandler().stopSound(attackSound);
                attackSound = null;
            }
            hasPlayedAttackSound = false;
        }
        if (config.customAnimations.containsKey(AnimationCustomItemType.ATTACK)) {
            double attackSpeed = config.customAnimations.get(AnimationCustomItemType.ATTACK).getSpeed(config.FPS) * stepTick;
            ATTACK += attackSpeed;
            if (ATTACK >= 1.0) {
                ATTACK = 1.0;
            }
        } else {
            ATTACK = 1.0;
        }
        
        updateActionAndTime();
    }
    
    /**
     * 更新当前播放的动画动作
     */
    private void updateActionAndTime() {
        if (isDrawing) {
            currentAnimation = AnimationCustomItemType.DRAW;
        } else if (ATTACK < 1.0) {
            currentAnimation = AnimationCustomItemType.ATTACK;
        } else if (USE < 1.0) {
            currentAnimation = AnimationCustomItemType.USE;
        } else if (INSPECT < 1.0) {
            currentAnimation = AnimationCustomItemType.INSPECT;
        } else {
            currentAnimation = AnimationCustomItemType.DEFAULT;
        }
    }
    
    /**
     * 触发攻击动画
     */
    public void triggerAttack() {
        if (ATTACK >= 1.0 && !isDrawing) {
            ATTACK = 0.0;
            // 如果正在使用，取消使用动画
            if (USE < 1.0) {
                USE = 1.0;
            }
        }
    }
    
    /**
     * 触发使用动画
     */
    public void triggerUse() {
        if (USE >= 1.0 && !isDrawing && ATTACK >= 1.0) {
            USE = 0.0;
        }
    }
    
    /**
     * 触发视检动画
     */
    public void triggerInspect() {
        if (INSPECT >= 1.0 && !isDrawing && ATTACK >= 1.0 && USE >= 1.0) {
            INSPECT = 0.0;
        }
    }
    
    /**
     * 重置动画状态（参考 AnimationController）
     */
    public void reset(boolean resetSprint) {
        DEFAULT = 0.0;
        DRAW = 0.0;
        hasPlayedDrawSound = false;
        hasPlayedInspectSound = false;
        hasPlayedAttackSound = false;
        hasPlayedUseSound = false;
        lastDefault = 0.0;
        if (resetSprint) {
            SPRINT = 0.0;
        }
        SPRINT_LOOP = 0.0;
        INSPECT = 1.0;
        USE = 1.0;
        ATTACK = 1.0;
        if (inspectSound != null) {
            Minecraft.getMinecraft().getSoundHandler().stopSound(inspectSound);
            inspectSound = null;
        }
        if (attackSound != null) {
            Minecraft.getMinecraft().getSoundHandler().stopSound(attackSound);
            attackSound = null;
        }
        if (useSound != null) {
            Minecraft.getMinecraft().getSoundHandler().stopSound(useSound);
            useSound = null;
        }
        isDrawing = true;
    }
    
    /**
     * 获取当前动画类型
     */
    public AnimationCustomItemType getCurrentAnimation() {
        return currentAnimation;
    }
    
    /**
     * 获取配置
     */
    public CustomItemEnhancedRenderConfig getConfig() {
        return config;
    }
    
    /**
     * 获取道具类型
     */
    public CustomItemType getItemType() {
        return itemType;
    }
    
    /**
     * 获取当前动画时间
     */
    public float getTime() {
        if (!config.customAnimations.containsKey(currentAnimation)) {
            return 0.0f;
        }
        
        EnhancedRenderConfig.Animation anim = config.customAnimations.get(currentAnimation);
        double startTime = anim.getStartTime(config.FPS);
        double endTime = anim.getEndTime(config.FPS);
        double progress = 0.0;
        
        switch (currentAnimation) {
            case DEFAULT:
                progress = DEFAULT;
                break;
            case DRAW:
                progress = DRAW;
                break;
            case INSPECT:
                progress = INSPECT;
                break;
            case USE:
                progress = USE;
                break;
            case ATTACK:
                progress = ATTACK;
                break;
            default:
                progress = 0.0;
                break;
        }
        
        return (float) (startTime + (endTime - startTime) * progress);
    }
    
    /**
     * 获取 Sprint 混合时间
     */
    public float getSprintTime() {
        if (!config.customAnimations.containsKey(AnimationCustomItemType.SPRINT)) {
            return 0.0f;
        }
        EnhancedRenderConfig.Animation anim = config.customAnimations.get(AnimationCustomItemType.SPRINT);
        double startTime = anim.getStartTime(config.FPS);
        double endTime = anim.getEndTime(config.FPS);
        double result = startTime + (endTime - startTime) * SPRINT_LOOP;
        if (Double.isNaN(result)) {
            return 0.0f;
        }
        return (float) result;
    }
    
    /**
     * 获取 Sprint 混合强度
     */
    public float getSprintAlpha() {
        return (float) SPRINT;
    }
    
    /**
     * 是否正在绘制
     */
    public boolean isDrawing() {
        return isDrawing;
    }
    
    /**
     * 是否正在使用
     */
    public boolean isUsing() {
        return USE < 1.0;
    }
    
    /**
     * USE 动画是否完成
     */
    public boolean isUseComplete() {
        return USE >= 1.0 && !isDrawing && ATTACK >= 1.0;
    }
}
