package siz.addon.modularprops.client.fpp.enhanced.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundEvent;
import siz.addon.modularprops.client.fpp.enhanced.AnimationCustomBlockType;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomBlockEnhancedRenderConfig;
import siz.addon.modularprops.common.custom.CustomBlockType;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.common.guns.WeaponSoundType;

/**
 * 自定义方块动画控制器（第一人称和第三人称）
 */
public class CustomBlockAnimationController {
    
    // 第一人称动画状态
    private AnimationCustomBlockType currentAnimation = AnimationCustomBlockType.DRAW;
    private double animationTime = 0.0;
    private CustomBlockEnhancedRenderConfig config;
    
    // 第一人称动画状态变量
    public double DEFAULT = 0.0;
    public double DRAW = 0.0;
    public double SPRINT = 0.0;
    public double SPRINT_LOOP = 0.0;
    public double SPRINT_RANDOM = 0.0;
    public double INSPECT = 1.0;
    public double USE = 1.0;
    public double ATTACK = 1.0;
    public double PLACE = 1.0;
    
    // Sprint 冷却时间
    private long sprintCoolTime = 0;
    private long sprintLoopCoolTime = 0;
    private boolean isJumping = false;
    
    // 是否正在播放 draw 动画
    private boolean isDrawing = false;
    
    // 第三人称动画状态（用于 TileEntity）
    private AnimationCustomBlockType thirdPersonAnimation = AnimationCustomBlockType.THIRD_DEFAULT;
    private double thirdPersonAnimationTime = 0.0;
    private boolean isPlacing = false;
    private boolean isBreaking = false;
    
    // 方块类型（用于音效，可选）
    private CustomBlockType blockType;
    
    // 音效相关（参考 CustomItemAnimationController）
    public boolean hasPlayedDrawSound = false;
    public ISound inspectSound = null;
    public ISound attackSound = null;
    public ISound useSound = null;
    public ISound placeSound = null;
    private double lastDefault = 0.0; // 用于跟踪 DEFAULT 循环
    private boolean hasPlayedInspectSound = false; // 用于跟踪 INSPECT 音效是否已播放
    private boolean hasPlayedAttackSound = false; // 用于跟踪 ATTACK 音效是否已播放
    private boolean hasPlayedUseSound = false; // 用于跟踪 USE 音效是否已播放
    private boolean hasPlayedPlaceSound = false; // 用于跟踪 PLACE 音效是否已播放
    
    public CustomBlockAnimationController(CustomBlockEnhancedRenderConfig config) {
        this.config = config;
        this.isDrawing = true; // 初始状态为 draw
        this.blockType = null; // 用于第三人称时可以为 null
    }
    
    public CustomBlockAnimationController(CustomBlockEnhancedRenderConfig config, CustomBlockType blockType) {
        this.config = config;
        this.blockType = blockType;
        this.isDrawing = true; // 初始状态为 draw
    }
    
    /**
     * 每帧更新第一人称动画状态
     */
    public void onTickRenderFirstPerson(float stepTick, EntityPlayerSP player) {
        // 更新 DEFAULT 动画
        if (config.customAnimations.containsKey(AnimationCustomBlockType.DEFAULT)) {
            double defaultSpeed = config.customAnimations.get(AnimationCustomBlockType.DEFAULT).getSpeed(config.FPS) * stepTick;
            DEFAULT = Math.max(0.0, DEFAULT + defaultSpeed);
            if (DEFAULT > 1.0) {
                DEFAULT = 0.0;
            }
        }
        
        // DEFAULT 动画音效（参考 CustomItemAnimationController）
        // 当 DEFAULT 从 > 0 循环回 0 时播放音效
        if (DEFAULT == 0.0 && lastDefault > 0.0 && DRAW >= 1.0 && blockType != null && blockType.weaponSoundMap != null) {
            if (player == Minecraft.getMinecraft().player) {
                blockType.playClientSound(player, WeaponSoundType.Idle);
            }
        }
        lastDefault = DEFAULT;
        
        // 更新 DRAW 动画
        if (config.customAnimations.containsKey(AnimationCustomBlockType.DRAW)) {
            double drawSpeed = config.customAnimations.get(AnimationCustomBlockType.DRAW).getSpeed(config.FPS) * stepTick;
            DRAW = Math.max(0.0, DRAW + drawSpeed);
            if (DRAW > 1.0) {
                DRAW = 1.0;
                isDrawing = false; // Draw 动画完成
            }
        } else {
            isDrawing = false; // 没有 draw 动画，直接完成
        }
        
        // DRAW 动画音效（参考 CustomItemAnimationController）
        if (!hasPlayedDrawSound && DRAW > 0.0 && blockType != null && blockType.weaponSoundMap != null) {
            if (player == Minecraft.getMinecraft().player) {
                if (Minecraft.getMinecraft().currentScreen == null) {
                    SoundEvent se = blockType.getSound(player, WeaponSoundType.Draw);
                    if (se != null) {
                        ISound drawSound = PositionedSoundRecord.getRecord(se, 1, 1);
                        Minecraft.getMinecraft().getSoundHandler().playSound(drawSound);
                    }
                }
                hasPlayedDrawSound = true;
            }
        }
        
        // 更新 SPRINT 动画（混合效果，参考道具）
        if (config.customAnimations.containsKey(AnimationCustomBlockType.SPRINT)) {
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
            if (ATTACK < 1.0 || INSPECT < 1.0 || USE < 1.0 || PLACE < 1.0) {
                sprintValue = SPRINT - sprintSpeed * 2.5f;
            }
            
            SPRINT = Math.max(0.0, Math.min(1.0, sprintValue));
            
            // SPRINT_LOOP
            double sprintLoopSpeed = config.customAnimations.get(AnimationCustomBlockType.SPRINT).getSpeed(config.FPS) * stepTick
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
                SPRINT_RANDOM -= config.customAnimations.get(AnimationCustomBlockType.SPRINT).getSpeed(config.FPS) * 3 * stepTick;
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
        
        // 更新 INSPECT 动画（参考 CustomItemAnimationController）
        if (INSPECT == 0.0) {
            // INSPECT 动画开始时播放音效（只播放一次）
            if (DRAW >= 1.0 && !hasPlayedInspectSound && blockType != null && blockType.weaponSoundMap != null && player == Minecraft.getMinecraft().player) {
                if (inspectSound != null) {
                    Minecraft.getMinecraft().getSoundHandler().stopSound(inspectSound);
                    inspectSound = null;
                }
                SoundEvent se = blockType.getSound(player, WeaponSoundType.Inspect);
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
        updateAnimationState(AnimationCustomBlockType.INSPECT, INSPECT, stepTick);
        
        // 更新 USE 动画（参考 CustomItemAnimationController）
        if (USE == 0.0) {
            // USE 动画开始时播放音效（只播放一次）
            if (DRAW >= 1.0 && !hasPlayedUseSound && blockType != null && blockType.weaponSoundMap != null && player == Minecraft.getMinecraft().player) {
                if (useSound != null) {
                    Minecraft.getMinecraft().getSoundHandler().stopSound(useSound);
                    useSound = null;
                }
                SoundEvent se = blockType.getSound(player, WeaponSoundType.CustomItemUse);
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
        updateAnimationState(AnimationCustomBlockType.USE, USE, stepTick);
        
        // 更新 ATTACK 动画（参考 CustomItemAnimationController）
        if (ATTACK == 0.0) {
            // ATTACK 动画开始时播放音效（只播放一次）
            if (DRAW >= 1.0 && !hasPlayedAttackSound && blockType != null && blockType.weaponSoundMap != null && player == Minecraft.getMinecraft().player) {
                if (attackSound != null) {
                    Minecraft.getMinecraft().getSoundHandler().stopSound(attackSound);
                    attackSound = null;
                }
                SoundEvent se = blockType.getSound(player, WeaponSoundType.CustomItemAttack);
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
        updateAnimationState(AnimationCustomBlockType.ATTACK, ATTACK, stepTick);
        
        // 更新 PLACE 动画（参考其他动画音效逻辑）
        if (PLACE == 0.0) {
            // PLACE 动画开始时播放音效（只播放一次）
            if (DRAW >= 1.0 && !hasPlayedPlaceSound && blockType != null && blockType.weaponSoundMap != null && player == Minecraft.getMinecraft().player) {
                if (placeSound != null) {
                    Minecraft.getMinecraft().getSoundHandler().stopSound(placeSound);
                    placeSound = null;
                }
                // PLACE 使用 CustomItemUse 音效类型（或者可以添加专门的类型）
                SoundEvent se = blockType.getSound(player, WeaponSoundType.CustomItemUse);
                if (se != null) {
                    placeSound = PositionedSoundRecord.getRecord(se, 1, 1);
                    Minecraft.getMinecraft().getSoundHandler().playSound(placeSound);
                    hasPlayedPlaceSound = true;
                }
            }
        }
        if (PLACE == 1.0) {
            // PLACE 动画结束时停止音效并重置标志
            if (placeSound != null) {
                Minecraft.getMinecraft().getSoundHandler().stopSound(placeSound);
                placeSound = null;
            }
            hasPlayedPlaceSound = false;
        }
        updateAnimationState(AnimationCustomBlockType.PLACE, PLACE, stepTick);
        
        updateActionAndTime();
    }
    
    /**
     * 更新动画状态
     */
    private void updateAnimationState(AnimationCustomBlockType animType, double state, float stepTick) {
        if (config.customAnimations.containsKey(animType)) {
            double speed = config.customAnimations.get(animType).getSpeed(config.FPS) * stepTick;
            if (animType == AnimationCustomBlockType.INSPECT) {
                INSPECT += speed;
                if (INSPECT >= 1.0) {
                    INSPECT = 1.0;
                }
            } else if (animType == AnimationCustomBlockType.USE) {
                USE += speed;
                if (USE >= 1.0) {
                    USE = 1.0;
                }
            } else if (animType == AnimationCustomBlockType.ATTACK) {
                ATTACK += speed;
                if (ATTACK >= 1.0) {
                    ATTACK = 1.0;
                }
            } else if (animType == AnimationCustomBlockType.PLACE) {
                PLACE += speed;
                if (PLACE >= 1.0) {
                    PLACE = 1.0;
                }
            }
        }
    }
    
    /**
     * 更新当前播放的动画动作
     */
    private void updateActionAndTime() {
        if (isDrawing) {
            currentAnimation = AnimationCustomBlockType.DRAW;
        } else if (ATTACK < 1.0) {
            currentAnimation = AnimationCustomBlockType.ATTACK;
        } else if (USE < 1.0) {
            currentAnimation = AnimationCustomBlockType.USE;
        } else if (PLACE < 1.0) {
            currentAnimation = AnimationCustomBlockType.PLACE;
        } else if (INSPECT < 1.0) {
            currentAnimation = AnimationCustomBlockType.INSPECT;
        } else {
            currentAnimation = AnimationCustomBlockType.DEFAULT;
        }
    }
    
    /**
     * 触发攻击动画
     */
    public void triggerAttack() {
        if (ATTACK >= 1.0 && !isDrawing) {
            ATTACK = 0.0;
            if (USE < 1.0) {
                USE = 1.0;
            }
            if (PLACE < 1.0) {
                PLACE = 1.0;
            }
        }
    }
    
    /**
     * 触发使用动画
     */
    public void triggerUse() {
        if (USE >= 1.0 && !isDrawing && ATTACK >= 1.0) {
            USE = 0.0;
            if (PLACE < 1.0) {
                PLACE = 1.0;
            }
        }
    }
    
    /**
     * 触发视检动画
     */
    public void triggerInspect() {
        if (INSPECT >= 1.0 && !isDrawing && ATTACK >= 1.0 && USE >= 1.0 && PLACE >= 1.0) {
            INSPECT = 0.0;
        }
    }
    
    /**
     * 触发放置动画
     */
    public void triggerPlace() {
        if (PLACE >= 1.0 && !isDrawing && ATTACK >= 1.0 && USE >= 1.0) {
            PLACE = 0.0;
        }
    }
    
    /**
     * 获取第一人称动画时间
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
            case PLACE:
                progress = PLACE;
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
        if (!config.customAnimations.containsKey(AnimationCustomBlockType.SPRINT)) {
            return 0.0f;
        }
        EnhancedRenderConfig.Animation anim = config.customAnimations.get(AnimationCustomBlockType.SPRINT);
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
     * 更新第三人称动画（用于 TileEntity）
     */
    public void updateThirdPerson(float partialTicks) {
        if (isPlacing && config.customAnimations.containsKey(AnimationCustomBlockType.THIRD_PLACE)) {
            EnhancedRenderConfig.Animation anim = config.customAnimations.get(AnimationCustomBlockType.THIRD_PLACE);
            thirdPersonAnimationTime += partialTicks * anim.getSpeed(config.FPS);
            double endTime = anim.getEndTime(config.FPS);
            if (thirdPersonAnimationTime >= endTime) {
                isPlacing = false;
                thirdPersonAnimation = AnimationCustomBlockType.THIRD_DEFAULT;
                thirdPersonAnimationTime = 0.0;
            }
        } else if (isBreaking && config.customAnimations.containsKey(AnimationCustomBlockType.BREAK)) {
            EnhancedRenderConfig.Animation anim = config.customAnimations.get(AnimationCustomBlockType.BREAK);
            thirdPersonAnimationTime += partialTicks * anim.getSpeed(config.FPS);
            double endTime = anim.getEndTime(config.FPS);
            if (thirdPersonAnimationTime >= endTime) {
                isBreaking = false;
            }
        } else {
            // 默认循环播放 THIRD_DEFAULT
            if (config.customAnimations.containsKey(AnimationCustomBlockType.THIRD_DEFAULT)) {
                EnhancedRenderConfig.Animation anim = config.customAnimations.get(AnimationCustomBlockType.THIRD_DEFAULT);
                thirdPersonAnimationTime += partialTicks * anim.getSpeed(config.FPS);
                double endTime = anim.getEndTime(config.FPS);
                if (thirdPersonAnimationTime > endTime) {
                    thirdPersonAnimationTime = anim.getStartTime(config.FPS);
                }
            }
        }
    }
    
    /**
     * 获取第三人称动画时间
     */
    public float getThirdPersonTime() {
        if (!config.customAnimations.containsKey(thirdPersonAnimation)) {
            return 0.0f;
        }
        EnhancedRenderConfig.Animation anim = config.customAnimations.get(thirdPersonAnimation);
        double startTime = anim.getStartTime(config.FPS);
        return (float) (startTime + thirdPersonAnimationTime);
    }
    
    /**
     * 触发第三人称放置动画
     */
    public void triggerThirdPlace() {
        isPlacing = true;
        thirdPersonAnimation = AnimationCustomBlockType.THIRD_PLACE;
        thirdPersonAnimationTime = 0.0;
    }
    
    /**
     * 触发第三人称破坏动画
     */
    public void triggerBreak() {
        isBreaking = true;
        thirdPersonAnimation = AnimationCustomBlockType.BREAK;
        thirdPersonAnimationTime = 0.0;
    }
    
    /**
     * 获取当前第一人称动画类型
     */
    public AnimationCustomBlockType getCurrentAnimation() {
        return currentAnimation;
    }
    
    /**
     * 获取当前第三人称动画类型
     */
    public AnimationCustomBlockType getThirdPersonAnimation() {
        return thirdPersonAnimation;
    }
    
    /**
     * 是否正在绘制
     */
    public boolean isDrawing() {
        return isDrawing;
    }
    
    /**
     * 是否正在破坏
     */
    public boolean isBreaking() {
        return isBreaking;
    }
    
    /**
     * 重置动画状态（参考 CustomItemAnimationController）
     */
    public void reset(boolean resetSprint) {
        DEFAULT = 0.0;
        DRAW = 0.0;
        hasPlayedDrawSound = false;
        hasPlayedInspectSound = false;
        hasPlayedAttackSound = false;
        hasPlayedUseSound = false;
        hasPlayedPlaceSound = false;
        lastDefault = 0.0;
        if (resetSprint) {
            SPRINT = 0.0;
        }
        SPRINT_LOOP = 0.0;
        INSPECT = 1.0;
        USE = 1.0;
        ATTACK = 1.0;
        PLACE = 1.0;
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
        if (placeSound != null) {
            Minecraft.getMinecraft().getSoundHandler().stopSound(placeSound);
            placeSound = null;
        }
        isDrawing = true;
    }
    
    /**
     * 获取配置
     */
    public CustomBlockEnhancedRenderConfig getConfig() {
        return config;
    }
    
    /**
     * 获取方块类型
     */
    public CustomBlockType getBlockType() {
        return blockType;
    }
}
