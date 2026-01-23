var KEY_LSHIFT = 42;
var KEY_CTRL = 29;

var potionEffectMap = {
    "ABSORPTION": "absorption",
    "BLINDNESS": "blindness",
    "NAUSEA": "confusion",
    "STRENGTH": "damageBoost",
    "MINING_FATIGUE": "digSlowDown",
    "HASTE": "digSpeed",
    "FIRE_RESISTANCE": "fireResistance",
    "GLOWING": "glowing",
    "INSTANT_DAMAGE": "harm",
    "INSTANT_HEALTH": "heal",
    "HEALTH_BOOST": "healthBoost",
    "HUNGER": "hunger",
    "INVISIBILITY": "invisibility",
    "JUMP_BOOST": "jump",
    "LEVITATION": "levitation",
    "LUCK": "luck",
    "SLOWNESS": "moveSlowdown",
    "SPEED": "moveSpeed",
    "NIGHT_VISION": "nightVision",
    "NONE": "none",
    "POISON": "poison",
    "REGENERATION": "regeneration",
    "RESISTANCE": "resistance",
    "SATURATION": "saturation",
    "UNLUCK": "unluck",
    "WATER_BREATHING": "waterBreathing",
    "WEAKNESS": "weakness",
    "WITHER": "wither",
    "STUN": "modularwarfare.stun"
};

function updateTooltip(stack, tiplist) {
    if (ScriptAPI.Gun.isGun(stack)) {
        updateGunTooltip(stack, tiplist);
    }
    if (ScriptAPI.Ammo.isAmmo(stack)) {
        updateAmmoTooltip(stack, tiplist);
    }
    if (ScriptAPI.Bullet.isBullet(stack)) {
        updateBulletTooltip(stack, tiplist);
    }
    if (ScriptAPI.Attachment.isAttachment(stack)) {
        updateAttachmentTooltip(stack, tiplist);
    }
    if (ScriptAPI.Grenade.isGrenade(stack)) {
        updateGrenadeTooltip(stack, tiplist);
    }
    if (ScriptAPI.Armor.isArmor(stack)) {
        updateArmorTooltip(stack, tiplist);
    }
    if (ScriptAPI.Backpack.isBackpack(stack)) {
        updateBackpackTooltip(stack, tiplist);
    }
    if (ScriptAPI.Melee.isMelee(stack)) {
        updateMeleeTooltip(stack, tiplist);
    }
}

function updateAmmoTooltip(stack, tiplist) {
    if (ScriptAPI.Input.isKeyHolding(KEY_CTRL)) {
        var ammoBullets = ScriptAPI.Ammo.getAcceptedBullet(stack);
        if (ammoBullets.size() > 0) {
            if (ScriptAPI.Ammo.isAmmo(stack)) {
                tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.bullet"));
            } else {
                tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.bullet"));
            }
            for (var i = 0; i < ammoBullets.size(); i++) {
                var bulletName = ammoBullets.get(i);
                if (bulletName != null) {
                    tiplist.add("§7 -" + ScriptAPI.Lang.format("item." + bulletName + ".name"));
                } else {
                    ScriptAPI.Logger.warn("Accepted bullet at index " + i + " is null for ammo: " + ScriptAPI.Stack.getDisplayName(stack));
                }
            }
        }
        return;
    }
    var ammoStack = stack;
    if (!ScriptAPI.Stack.isEmpty(ammoStack)) {
        if (ScriptAPI.Ammo.getMagazineCount(ammoStack) == 1) {
            var ammocount = NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack), "ammocount");
            tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo") + ": §7" + ammocount + " / " + ScriptAPI.Ammo.getAmmoCapacity(ammoStack));
        } else {
            var magcount = NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack), "magcount");
            var ammocount = NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack), "ammocount" + magcount);
            if (ScriptAPI.Input.isKeyHolding(KEY_LSHIFT)) {
                for (var i = 1; i <= ScriptAPI.Ammo.getMagazineCount(ammoStack); i++) {
                    ammocount = NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack), "ammocount" + i);
                    if (i == magcount) {
                        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo") + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo.num" + i) + ": §e" + ammocount + " / " + ScriptAPI.Ammo.getAmmoCapacity(ammoStack));
                    } else {
                        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo") + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo.num" + i) + ": §7" + ammocount + " / " + ScriptAPI.Ammo.getAmmoCapacity(ammoStack));
                    }
                }
            } else {
                tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet") + ": §7" + ammocount + " / " + ScriptAPI.Ammo.getAmmoCapacity(ammoStack));
            }
        }
    }

    var bulletItem = ScriptAPI.Ammo.getUsedBulletItem(stack);
    var bulletStack = ScriptAPI.Stack.getStack(bulletItem);
    if (!ScriptAPI.Stack.isEmpty(bulletStack)) {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:dictionary.bullet") + ": §7" + ScriptAPI.Stack.getDisplayName(bulletStack));
    }

    tiplist.add("§e" + ScriptAPI.Lang.format("mwf:gui.tooltip.help"));
    tiplist.add("§e" + ScriptAPI.Lang.format("mwf:gui.tooltip.reload"));
}

function updateBulletTooltip(stack, tiplist) {
    tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.basicinfo"));
    tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.damage") + ": §7 x" + ScriptAPI.Bullet.getDamageFactor(stack));

    var bulletProperties = ScriptAPI.Bullet.getBulletProperties(stack);
    if (bulletProperties != null) {
        var allProperty = bulletProperties.get("All");
        if (allProperty != null) {
            if (allProperty.fireLevel > 0 || allProperty.explosionLevel > 0 || 
                allProperty.knockLevel > 0 || allProperty.banShield ||
                (allProperty.potionEffects != null && allProperty.potionEffects.length > 0)) {
                
                tiplist.add("");
                tiplist.add("§9§l" + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.target") + ": " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.all"));
                
                if (allProperty.fireLevel > 0) {
                    tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.fire") + ": §c" + allProperty.fireLevel);
                }
                
                if (allProperty.explosionLevel > 0) {
                    tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.explosion") + ": §e" + allProperty.explosionLevel.toFixed(2));
                    if (allProperty.explosionBroken) {
                        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.explosion.broken"));
                    }
                }
                
                if (allProperty.knockLevel > 0) {
                    tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.knock") + ": §9" + allProperty.knockLevel);
                }
                
                if (allProperty.banShield) {
                    tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.banshield"));
                }

                if (allProperty.potionEffects != null && allProperty.potionEffects.length > 0) {
                    tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.effects") + ":");
                    for (var i = 0; i < allProperty.potionEffects.length; i++) {
                        var potionEntry = allProperty.potionEffects[i];
                        var effectKey = String(potionEntry.potionEffect);
                        var mappedEffect = potionEffectMap[effectKey] || effectKey.toLowerCase();
                        var effectName = ScriptAPI.Lang.format("effect." + mappedEffect);
                        var duration = (potionEntry.duration / 20.0).toFixed(1);
                        var level = potionEntry.level > 0 ? " " + ScriptAPI.Lang.format("mwf:gui.tooltip.effects.level") + " " + (potionEntry.level + 1) : "";
                        tiplist.add("§7- " + effectName + level + " " + duration + "s");
                    }
                }
            }
        }

        if (ScriptAPI.Input.isKeyHolding(KEY_LSHIFT)) {
            var keys = bulletProperties.keySet().toArray();
            for (var i = 0; i < keys.length; i++) {
                var key = keys[i];
                if (key != "All") {
                    var bulletProperty = bulletProperties.get(key);

                    var entityKey = "entity." + key.toLowerCase() + ".name";
                    var targetName = ScriptAPI.Lang.format(entityKey);
 
                    if (targetName == entityKey) {
                        targetName = key;
                    }


                    tiplist.add("");
                    tiplist.add("§9§l" + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.target") + ": " + targetName);


                    tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.damage") + ": §e" + bulletProperty.bulletDamageFactor + "x");


                    if (bulletProperty.fireLevel > 0) {
                        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.fire") + ": §c" + bulletProperty.fireLevel);
                    }
                    
                    if (bulletProperty.explosionLevel > 0) {
                        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.explosion") + ": §e" + bulletProperty.explosionLevel.toFixed(2));
                        if (bulletProperty.explosionBroken) {
                            tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.explosion.broken"));
                        }
                    }
                    
                    if (bulletProperty.knockLevel > 0) {
                        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.knock") + ": §9" + bulletProperty.knockLevel);
                    }
                    
                    if (bulletProperty.banShield) {
                        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet.property.banshield"));
                    }


                    if (bulletProperty.potionEffects != null && bulletProperty.potionEffects.length > 0) {
                        tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.effects") + ":");
                        for (var j = 0; j < bulletProperty.potionEffects.length; j++) {
                            var potionEntry = bulletProperty.potionEffects[j];
                            var effectKey = String(potionEntry.potionEffect);
                            var mappedEffect = potionEffectMap[effectKey] || effectKey.toLowerCase();
                            var effectName = ScriptAPI.Lang.format("effect." + mappedEffect);
                            var duration = (potionEntry.duration / 20.0).toFixed(1);
                            var level = potionEntry.level > 0 ? " " + ScriptAPI.Lang.format("mwf:gui.tooltip.effects.level") + " " + (potionEntry.level + 1) : "";
                            tiplist.add("§7- " + effectName + level + " " + duration + "s");
                        }
                    }
                }
            }
        } else {
            tiplist.add("§e" + ScriptAPI.Lang.format("mwf:gui.tooltip.seemore"));
        }
    }
}

function updateGunTooltip(stack, tiplist) {
    if (!ScriptAPI.Gun.isGun(stack)) {
        return;
    }

    if (ScriptAPI.Input.isKeyHolding(KEY_CTRL)) {

        var ammoBullets = ScriptAPI.Gun.getAcceptedAmmoOrBullet(stack);
        if (ammoBullets.size() > 0) {
            if (ScriptAPI.Gun.isBulletGun(stack)) {
                tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.bullet"));
            } else {
                tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.ammo"));
            }
            for (var i = 0; i < ammoBullets.size(); i++) {
                var ammoOrBulletName = ammoBullets.get(i);
                if (ammoOrBulletName != null) {
                    tiplist.add("§7 -" + ScriptAPI.Lang.format("item." + ammoOrBulletName + ".name"));
                } else {
                    ScriptAPI.Logger.warn("Accepted ammo/bullet at index " + i + " is null for gun: " + ScriptAPI.Stack.getDisplayName(stack));
                }
            }
        }

        var attmap = ScriptAPI.Gun.getAcceptedAttachment(stack);
        if (!attmap.isEmpty()) {
            tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.attachment"));
            var types = attmap.keySet().toArray();
            for (var i = 0; i < types.length; i++) {
                var typeName = types[i];
                if (typeName != null) {
                    tiplist.add("§3" + ScriptAPI.Lang.format("mwf:dictionary." + typeName) + ":");
                    var atts = attmap.get(typeName);
                    if (atts != null) {
                        for (var x = 0; x < atts.size(); x++) {
                            var attachmentName = atts.get(x);
                            if (attachmentName != null) {
                                tiplist.add("§7 -" + ScriptAPI.Lang.format("item." + attachmentName + ".name"));
                            } else {
                                ScriptAPI.Logger.warn("Attachment at index " + x + " is null for type " + typeName + " in gun: " + ScriptAPI.Stack.getDisplayName(stack));
                            }
                        }
                    } else {
                        ScriptAPI.Logger.warn("Attachment list is null for type " + typeName + " in gun: " + ScriptAPI.Stack.getDisplayName(stack));
                    }
                } else {
                    ScriptAPI.Logger.warn("Attachment type at index " + i + " is null for gun: " + ScriptAPI.Stack.getDisplayName(stack));
                }
            }
        }

        return;
    }

    if (ScriptAPI.Input.isKeyHolding(KEY_LSHIFT)) {
        tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.basicinfo"));
    }
    var text_firemode = ScriptAPI.Lang.format("mwf:dictionary.firemode");
    var text_firemode_vaule = ScriptAPI.Lang.format("mwf:dictionary.firemode.full");
    /**
     * Firemode
     */
    switch (ScriptAPI.Gun.getFireMode(stack)) {
        case WeaponFireMode.SEMI:
            text_firemode_vaule = ScriptAPI.Lang.format("mwf:dictionary.firemode.semi");
            break;
        case WeaponFireMode.BRUST:
            text_firemode_vaule = ScriptAPI.Lang.format("mwf:dictionary.firemode.brust");
            break;
    }
    tiplist.add("§3" + text_firemode + ": §7" + text_firemode_vaule);

    /**
     * Ammo
     */
    if (ScriptAPI.Gun.hasAmmoLoaded(stack)) {
        var ammoStack = ScriptAPI.Gun.getAmmoStack(stack);
        if (!ScriptAPI.Stack.isEmpty(ammoStack)) {
            if (ScriptAPI.Ammo.getMagazineCount(ammoStack) == 1) {
                var ammocount = NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack), "ammocount");
                tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo") + ": §7" + ammocount + " / " + ScriptAPI.Ammo.getAmmoCapacity(ammoStack));
            } else {
                var magcount = NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack), "magcount");
                var ammocount = NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack), "ammocount" + magcount);
                if (ScriptAPI.Input.isKeyHolding(KEY_LSHIFT)) {
                    for (var i = 1; i <= ScriptAPI.Ammo.getMagazineCount(ammoStack); i++) {
                        ammocount = NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack), "ammocount" + i);
                        if (i == magcount) {
                            tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo") + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo.num" + i) + ": §e" + ammocount + " / " + ScriptAPI.Ammo.getAmmoCapacity(ammoStack));
                        } else {
                            tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo") + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo.num" + i) + ": §7" + ammocount + " / " + ScriptAPI.Ammo.getAmmoCapacity(ammoStack));
                        }
                    }
                } else {
                    tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.bullet") + ": §7" + ammocount + " / " + ScriptAPI.Ammo.getAmmoCapacity(ammoStack));
                }
            }
        }
    }

    //子弹枪
    if (ScriptAPI.Gun.isBulletGun(stack)) {
        var ammocount = NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(stack), "ammocount");
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.ammo") + ": §7" + ammocount + " / " + ScriptAPI.Gun.getAmmoStorage(stack));
    }

    var bulletItem = ScriptAPI.Gun.getUsedBulletItem(stack);
    var bulletStack = ScriptAPI.Stack.getStack(bulletItem);
    if (!ScriptAPI.Stack.isEmpty(bulletStack)) {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:dictionary.bullet") + ": §7" + ScriptAPI.Stack.getDisplayName(bulletStack));
    }

    var finalDamage = ScriptAPI.Gun.getGunDamage(stack);
    if (!ScriptAPI.Stack.isEmpty(bulletStack)) {
        finalDamage *= ScriptAPI.Bullet.getDamageFactor(bulletStack);
    }
    finalDamage = finalDamage.toFixed(1);
    if (ScriptAPI.Gun.getGunNumBullets(stack) > 1) {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.damage") + ": §7" + finalDamage + " x " + ScriptAPI.Gun.getGunNumBullets(stack));
    } else {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.damage") + ": §7" + finalDamage);
    }

    var finalAccuracy = 1 / ScriptAPI.Gun.getGunBulletSpread(stack) * 100;
    if (!ScriptAPI.Stack.isEmpty(bulletStack)) {
        finalAccuracy /= ScriptAPI.Bullet.getAccuracyFactor(bulletStack);
    }

    tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.accuracy") + ": §7" + finalAccuracy.toFixed(1) + "%");

    if (!ScriptAPI.Input.isKeyHolding(KEY_LSHIFT)) {
        tiplist.add("§e" + ScriptAPI.Lang.format("mwf:gui.tooltip.seemore"));
        tiplist.add("§e" + ScriptAPI.Lang.format("mwf:gui.tooltip.help"));
    } else {
        var attachmentList = ScriptAPI.Gun.getInstalledAttachments(stack);
        if (attachmentList.size() > 0) {
            tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.attachmentinfo"));
            for (var i = 0; i < attachmentList.size(); i++) {
                var attachmentName = attachmentList.get(i);
                if (attachmentName != null) {
                    tiplist.add("§7- " + ScriptAPI.Lang.format("item." + attachmentName + ".name"));
                } else {
                    ScriptAPI.Logger.warn("Installed attachment at index " + i + " is null for gun: " + ScriptAPI.Stack.getDisplayName(stack));
                }
            }
        }
    }

    if (ScriptAPI.Gun.getGunExtraLore(stack) != null) {
        tiplist.add(ScriptAPI.Gun.getGunExtraLore(stack));
    }
}

function addPropertyLine(tiplist, labelKey, factor, isInverse) {
    if (factor == 1.0) return false;
    
    var changePercent;
    var isPositive;
    
    if (isInverse) {
        isPositive = factor < 1.0;
        if (labelKey.indexOf("accuracy") >= 0) {
            changePercent = ((1.0 - factor) * 100).toFixed(1);
        } else {
            changePercent = ((factor - 1.0) * 100).toFixed(1);
        }
    } else {
        changePercent = ((factor - 1.0) * 100).toFixed(1);
        isPositive = factor > 1.0;
    }
    
    var colorCode = isPositive ? "§a" : "§c";
    var sign = (changePercent > 0) ? "+" : "";
    
    tiplist.add("§7- " + ScriptAPI.Lang.format(labelKey) + ": " + colorCode + sign + changePercent + "%");
    return true;
}

// Helper function to convert RGB to hex color
function rgbToHex(r, g, b) {
    var toHex = function(val) {
        var hex = Math.round(val).toString(16);
        return hex.length == 1 ? "0" + hex : hex;
    };
    return "#" + toHex(r) + toHex(g) + toHex(b);
}

// Helper function to get closest Minecraft color code
function getClosestColorCode(r, g, b) {
    // Normalize to 0-1
    var nr = r / 255.0;
    var ng = g / 255.0;
    var nb = b / 255.0;
    
    // Determine dominant color
    if (nr > ng && nr > nb && nr > 0.5) {
        if (ng > 0.3 && nb < 0.3) return "§6"; // Gold (orange-red)
        return "§c"; // Red
    } else if (ng > nr && ng > nb && ng > 0.5) {
        if (nr > 0.3) return "§e"; // Yellow
        if (nb > 0.3) return "§a"; // Green (cyan-ish)
        return "§a"; // Green
    } else if (nb > nr && nb > ng && nb > 0.5) {
        if (ng > 0.3) return "§b"; // Aqua
        return "§9"; // Blue
    } else if (nr > 0.6 && ng > 0.6 && nb > 0.6) {
        return "§f"; // White
    } else if (nr < 0.3 && ng < 0.3 && nb < 0.3) {
        return "§8"; // Dark gray
    } else if (nr > 0.4 && ng < 0.4 && nb > 0.4) {
        return "§5"; // Purple
    }
    return "§7"; // Gray (default)
}

function updateAttachmentTooltip(stack, tiplist) {
    if (!ScriptAPI.Attachment.isAttachment(stack)) {
        return;
    }

    var attachmentTypeName = ScriptAPI.Attachment.getAttachmentTypeName(stack);
    
    tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.basicinfo"));
    tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.type") + ": §7" + ScriptAPI.Lang.format("mwf:dictionary." + attachmentTypeName));

    var hasProperties = false;

    if (attachmentTypeName === "sight") {
        var fovZoom = ScriptAPI.Attachment.getSightFovZoom(stack);
        var fovZoomMin = ScriptAPI.Attachment.getSightFovZoomMin(stack);
        var fovZoomMax = ScriptAPI.Attachment.getSightFovZoomMax(stack);
        var fovZoomStage = ScriptAPI.Attachment.getSightFovZoomStage(stack);
        
        if (fovZoomStage != null && fovZoomStage.length > 0) {
            hasProperties = true;
            if (fovZoomStage.length == 1) {
                tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.zoom") + ": §e" + fovZoomStage[0].toFixed(1) + "x");
            } else {
                var zoomText = "";
                for (var i = 0; i < fovZoomStage.length; i++) {
                    if (i > 0) zoomText += " / ";
                    zoomText += fovZoomStage[i].toFixed(1) + "x";
                }
                tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.zoom") + ": §e" + zoomText);
                tiplist.add("§7  " + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.zoom.stage"));
            }
        } else if (fovZoomMin > 0 && fovZoomMax > 0) {
            hasProperties = true;
            var minMag = fovZoomMax.toFixed(1);
            var maxMag = fovZoomMin.toFixed(1);
            tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.zoom") + ": §e" + minMag + "x - " + maxMag + "x");
            tiplist.add("§7  " + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.zoom.variable"));
        } else if (fovZoom != 1.0) {
            hasProperties = true;
            var magnification = fovZoom.toFixed(1);
            tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.zoom") + ": §e" + magnification + "x");
        }
        
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.aimspeed", 
            ScriptAPI.Attachment.getSightAimSpeedFactor(stack), false);
        
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.mousesensitivity", 
            ScriptAPI.Attachment.getSightMouseSensitivityFactor(stack), true);
    }

    if (attachmentTypeName === "barrel") {
        if (ScriptAPI.Attachment.isSuppressor(stack)) {
            hasProperties = true;
            tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.suppressor"));
        }
        if (ScriptAPI.Attachment.hideFlash(stack)) {
            hasProperties = true;
            tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.hideflash"));
        }
        
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.pitch", 
            ScriptAPI.Attachment.getBarrelRecoilPitchFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.yaw", 
            ScriptAPI.Attachment.getBarrelRecoilYawFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.accuracy", 
            ScriptAPI.Attachment.getBarrelAccuracyFactor(stack), true);
    }

    if (attachmentTypeName === "grip") {
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.pitch", 
            ScriptAPI.Attachment.getGripRecoilPitchFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.yaw", 
            ScriptAPI.Attachment.getGripRecoilYawFactor(stack), true);
    }

    if (attachmentTypeName === "stock") {
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.aimspeed", 
            ScriptAPI.Attachment.getStockAimSpeedFactor(stack), false);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.pitch", 
            ScriptAPI.Attachment.getStockRecoilPitchFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.yaw", 
            ScriptAPI.Attachment.getStockRecoilYawFactor(stack), true);
        
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.modelshake.backwards", 
            ScriptAPI.Attachment.getStockModelRecoilBackwardsFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.modelshake.upwards", 
            ScriptAPI.Attachment.getStockModelRecoilUpwardsFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.modelshake.shake", 
            ScriptAPI.Attachment.getStockModelRecoilShakeFactor(stack), true);
    }

    if (attachmentTypeName === "laser") {
        var laserColor = ScriptAPI.Attachment.getLaserColor(stack);
        if (laserColor != null && laserColor.length >= 3) {
            hasProperties = true;
            var r = laserColor[0] * 255;
            var g = laserColor[1] * 255;
            var b = laserColor[2] * 255;
            var hexColor = rgbToHex(r, g, b);
            var colorCode = getClosestColorCode(r, g, b);
            tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.laser.color") + ": " + colorCode + "███ §7" + hexColor);
        }
        
        var maxDistance = ScriptAPI.Attachment.getLaserMaxDistance(stack);
        if (maxDistance > 0) {
            hasProperties = true;
            tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.laser.distance") + ": §e" + maxDistance.toFixed(0) + "m");
        }
        
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.accuracy", 
            ScriptAPI.Attachment.getLaserAccuracyFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.aimspeed", 
            ScriptAPI.Attachment.getLaserAimSpeedFactor(stack), false);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.pitch", 
            ScriptAPI.Attachment.getLaserRecoilPitchFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.yaw", 
            ScriptAPI.Attachment.getLaserRecoilYawFactor(stack), true);
    }

    if (attachmentTypeName === "pistolgrip") {
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.aimspeed", 
            ScriptAPI.Attachment.getPistolgripAimSpeedFactor(stack), false);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.pitch", 
            ScriptAPI.Attachment.getPistolgripRecoilPitchFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.yaw", 
            ScriptAPI.Attachment.getPistolgripRecoilYawFactor(stack), true);
        
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.modelshake.backwards", 
            ScriptAPI.Attachment.getPistolgripModelRecoilBackwardsFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.modelshake.upwards", 
            ScriptAPI.Attachment.getPistolgripModelRecoilUpwardsFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.modelshake.shake", 
            ScriptAPI.Attachment.getPistolgripModelRecoilShakeFactor(stack), true);
    }

    if (attachmentTypeName === "handguard") {
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.aimspeed", 
            ScriptAPI.Attachment.getHandguardAimSpeedFactor(stack), false);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.pitch", 
            ScriptAPI.Attachment.getHandguardRecoilPitchFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.recoil.yaw", 
            ScriptAPI.Attachment.getHandguardRecoilYawFactor(stack), true);
        
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.modelshake.backwards", 
            ScriptAPI.Attachment.getHandguardModelRecoilBackwardsFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.modelshake.upwards", 
            ScriptAPI.Attachment.getHandguardModelRecoilUpwardsFactor(stack), true);
        hasProperties |= addPropertyLine(tiplist, "mwf:gui.tooltip.attachment.modelshake.shake", 
            ScriptAPI.Attachment.getHandguardModelRecoilShakeFactor(stack), true);
    }

    if (!hasProperties) {
        tiplist.add("§7" + ScriptAPI.Lang.format("mwf:gui.tooltip.attachment.noproperties"));
    }
}

function updateGrenadeTooltip(stack, tiplist) {
    if (!ScriptAPI.Grenade.isGrenade(stack)) {
        return;
    }

    var grenadeTypeName = ScriptAPI.Grenade.getGrenadeTypeName(stack);
    
    tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.basicinfo"));
    tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.type") + ": §7" + ScriptAPI.Lang.format("mwf:dictionary.grenade." + grenadeTypeName));

    var fuseTime = ScriptAPI.Grenade.getFuseTime(stack);
    var throwStrength = ScriptAPI.Grenade.getThrowStrength(stack);
    var impactDamage = ScriptAPI.Grenade.getImpactDamage(stack);
    
    if (!ScriptAPI.Grenade.isInstantExplode(stack) && fuseTime > 0) {
        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.fusetime") + ": §e" + fuseTime.toFixed(1) + "s");
    }
    
    if (throwStrength != 1.0) {
        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.throwstrength") + ": §e" + (throwStrength * 100).toFixed(0) + "%");
    }
    
    if (impactDamage > 0) {
        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.impactdamage") + ": §c" + impactDamage.toFixed(1));
    }
    
    if (ScriptAPI.Grenade.isSticky(stack)) {
        tiplist.add("§7- §a" + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.sticky"));
    }
    
    if (ScriptAPI.Grenade.isInstantExplode(stack)) {
        tiplist.add("§7- §c" + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.instant"));
    }

    if (grenadeTypeName === "frag") {
        tiplist.add("");
        tiplist.add("§c§l" + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.explosion"));
        
        var explosionDamage = ScriptAPI.Grenade.getExplosionDamage(stack);
        var explosionRange = ScriptAPI.Grenade.getExplosionRange(stack);
        var explosionKnockback = ScriptAPI.Grenade.getExplosionKnockback(stack);
        
        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.damage") + ": §c" + explosionDamage.toFixed(1));
        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.range") + ": §e" + explosionRange.toFixed(1) + "m");
        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.knockback") + ": §9" + explosionKnockback.toFixed(1));
        
        if (ScriptAPI.Grenade.getDamageWorld(stack)) {
            tiplist.add("§7- §c" + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.damageworld"));
        }
        
        if (ScriptAPI.Grenade.getExplosionThroughWalls(stack)) {
            tiplist.add("§7- §6" + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.throughwalls"));
        }
        
        if (ScriptAPI.Grenade.getBanShield(stack)) {
            tiplist.add("§7- §c" + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.banshield"));
        }
        
        if (ScriptAPI.Grenade.getCausesFire(stack)) {
            var fireDamage = ScriptAPI.Grenade.getFireDamage(stack);
            var fireDuration = ScriptAPI.Grenade.getFireDuration(stack);
            tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.fire") + ": §c" + fireDamage.toFixed(1) + " (" + fireDuration + "s)");
        }
        
        var potionEffects = ScriptAPI.Grenade.getExplosionPotionEffects(stack);
        if (potionEffects != null && potionEffects.length > 0) {
            tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.effects") + ":");
            for (var i = 0; i < potionEffects.length; i++) {
                var potionEntry = potionEffects[i];
                var effectKey = String(potionEntry.potionEffect);
                var mappedEffect = potionEffectMap[effectKey] || effectKey.toLowerCase();
                var effectName = ScriptAPI.Lang.format("effect." + mappedEffect);
                var duration = (potionEntry.duration / 20.0).toFixed(1);
                var level = potionEntry.level > 0 ? " " + ScriptAPI.Lang.format("mwf:gui.tooltip.effects.level") + " " + (potionEntry.level + 1) : "";
                tiplist.add("§7- " + effectName + level + " " + duration + "s");
            }
        }
    } else if (grenadeTypeName === "smoke") {
        tiplist.add("");
        tiplist.add("§7§l" + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.smoke"));
        
        var smokeTime = ScriptAPI.Grenade.getSmokeTime(stack);
        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.duration") + ": §e" + smokeTime.toFixed(1) + "s");
    } else if (grenadeTypeName === "stun") {
        tiplist.add("");
        tiplist.add("§e§l" + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.stun"));
        
        var explosionRange = ScriptAPI.Grenade.getExplosionRange(stack);
        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.range") + ": §e" + explosionRange.toFixed(1) + "m");
        
        var potionEffects = ScriptAPI.Grenade.getExplosionPotionEffects(stack);
        if (potionEffects != null && potionEffects.length > 0) {
            tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.effects") + ":");
            for (var i = 0; i < potionEffects.length; i++) {
                var potionEntry = potionEffects[i];
                var effectKey = String(potionEntry.potionEffect);
                var mappedEffect = potionEffectMap[effectKey] || effectKey.toLowerCase();
                var effectName = ScriptAPI.Lang.format("effect." + mappedEffect);
                var duration = (potionEntry.duration / 20.0).toFixed(1);
                var level = potionEntry.level > 0 ? " " + ScriptAPI.Lang.format("mwf:gui.tooltip.effects.level") + " " + (potionEntry.level + 1) : "";
                tiplist.add("§7- " + effectName + level + " " + duration + "s");
            }
        }
    } else if (grenadeTypeName === "gas") {
        tiplist.add("");
        tiplist.add("§2§l" + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.gas"));
        
        var smokeTime = ScriptAPI.Grenade.getSmokeTime(stack);
        tiplist.add("§7- " + ScriptAPI.Lang.format("mwf:gui.tooltip.grenade.duration") + ": §e" + smokeTime.toFixed(1) + "s");
        
        var potionEffects = ScriptAPI.Grenade.getExplosionPotionEffects(stack);
        if (potionEffects != null && potionEffects.length > 0) {
            tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.effects") + ":");
            for (var i = 0; i < potionEffects.length; i++) {
                var potionEntry = potionEffects[i];
                var effectKey = String(potionEntry.potionEffect);
                var mappedEffect = potionEffectMap[effectKey] || effectKey.toLowerCase();
                var effectName = ScriptAPI.Lang.format("effect." + mappedEffect);
                var duration = (potionEntry.duration / 20.0).toFixed(1);
                var level = potionEntry.level > 0 ? " " + ScriptAPI.Lang.format("mwf:gui.tooltip.effects.level") + " " + (potionEntry.level + 1) : "";
                tiplist.add("§7- " + effectName + level + " " + duration + "s");
            }
        }
    }
}

function updateArmorTooltip(stack, tiplist) {
    if (!ScriptAPI.Armor.isArmor(stack)) {
        return;
    }

    tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.basicinfo"));
    
    var isSuit = ScriptAPI.Armor.isSuit(stack);
    var armorSlot = ScriptAPI.Armor.getArmorSlot(stack);
    
    if (isSuit) {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.armor.type") + ": §6" + ScriptAPI.Lang.format("mwf:gui.tooltip.armor.suit"));
    }
    
    if (armorSlot && armorSlot !== "") {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.armor.slot") + ": §7" + ScriptAPI.Lang.format("mwf:dictionary.armor.slot." + armorSlot));
    }
    
    var durability = ScriptAPI.Armor.getDurability(stack);
    if (durability > 0) {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.armor.maxdurability") + ": §7" + durability);
    }
    
    var defense = ScriptAPI.Armor.getDefense(stack);
    if (defense > 0) {
        var reductionPercent = (defense * 100).toFixed(1);
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.armor.bodyreduction") + ": §a" + reductionPercent + "%");
        tiplist.add("§8" + ScriptAPI.Lang.format("mwf:gui.tooltip.armor.bodyonly"));
    }
}

function updateBackpackTooltip(stack, tiplist) {
    if (!ScriptAPI.Backpack.isBackpack(stack)) {
        return;
    }

    tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.basicinfo"));
    
    var isElytra = ScriptAPI.Backpack.isElytra(stack);
    var isJet = ScriptAPI.Backpack.isJet(stack);
    
    if (isElytra && isJet) {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.type") + ": §d" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.jetelytra"));
    } else if (isElytra) {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.type") + ": §b" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.elytra"));
    } else if (isJet) {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.type") + ": §6" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.jet"));
    } else {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.type") + ": §7" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.storage"));
    }
    
    var size = ScriptAPI.Backpack.getSize(stack);
    if (size > 0) {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.capacity") + ": §7" + size + " " + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.slots"));
    }
    
    var maxWeaponStorage = ScriptAPI.Backpack.getMaxWeaponStorage(stack);
    if (maxWeaponStorage > 0) {
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.maxweapons") + ": §7" + maxWeaponStorage);
    }
    
    if (ScriptAPI.Backpack.getAllowSmallerBackpackStorage(stack)) {
        tiplist.add("§7▪ §a" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.allowsmaller"));
    }
    
    if (isElytra) {
        tiplist.add("");
        tiplist.add("§b§l" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.elytrafeatures"));
        
        if (ScriptAPI.Backpack.isElytraStoppable(stack)) {
            tiplist.add("§7▪ §a" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.elytrastoppable"));
        }
    }
    
    if (isJet) {
        tiplist.add("");
        tiplist.add("§6§l" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.jetfeatures"));
        
        var jetWorkForce = ScriptAPI.Backpack.getJetWorkForce(stack);
        if (jetWorkForce > 0) {
            tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.jetworkforce") + ": §7" + jetWorkForce.toFixed(3));
        }
        
        var jetMaxForce = ScriptAPI.Backpack.getJetMaxForce(stack);
        if (jetMaxForce > 0) {
            tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.jetmaxforce") + ": §7" + jetMaxForce.toFixed(3));
        }
        
        var jetIdleForce = ScriptAPI.Backpack.getJetIdleForce(stack);
        tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.jetidleforce") + ": §7" + jetIdleForce.toFixed(3));
        
        if (ScriptAPI.Backpack.getJetSneakHover(stack)) {
            tiplist.add("§7▪ §a" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.jetsneakhover"));
        }
        
        if (ScriptAPI.Backpack.getJetGroundDust(stack)) {
            tiplist.add("§7▪ §a" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.jetgrounddust"));
        }
        
        if (isElytra) {
            var jetElytraBoost = ScriptAPI.Backpack.getJetElytraBoost(stack);
            if (jetElytraBoost > 0) {
                tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.jetelytraboost") + ": §7" + jetElytraBoost.toFixed(1) + "x");
                
                var boostDuration = ScriptAPI.Backpack.getJetElytraBoostDuration(stack);
                var boostCooldown = ScriptAPI.Backpack.getJetElytraBoostCoolTime(stack);
                tiplist.add("§7  - " + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.duration") + ": " + (boostDuration / 20).toFixed(1) + "s");
                tiplist.add("§7  - " + ScriptAPI.Lang.format("mwf:gui.tooltip.backpack.cooldown") + ": " + (boostCooldown / 20).toFixed(1) + "s");
            }
        }
    }
}

function updateMeleeTooltip(stack, tiplist) {
    if (!ScriptAPI.Melee.isMelee(stack)) {
        return;
    }
    
    var lightCount = ScriptAPI.Melee.getLightAttackCount(stack);
    var heavyCount = ScriptAPI.Melee.getHeavyAttackCount(stack);
    
    // Light attack info
    if (lightCount > 0) {
        tiplist.add("§9§l" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.lightattack"));
        for (var i = 0; i < lightCount; i++) {
            var damage = ScriptAPI.Melee.getLightAttackDamage(stack, i);
            var range = ScriptAPI.Melee.getLightAttackRange(stack, i);
            var penetration = ScriptAPI.Melee.getLightAttackPenetration(stack, i);
            var canBounced = ScriptAPI.Melee.getLightAttackCanBounced(stack, i);
            
            if (lightCount > 1) {
                var phaseNum = (i + 1) | 0; // Convert to integer
                tiplist.add("§7" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.phase") + " " + phaseNum);
            }
            tiplist.add("§3  " + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.damage") + ": §7" + Math.round(damage));
            tiplist.add("§3  " + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.range") + ": §7" + Math.round(range) + "m");
            
            if (penetration) {
                tiplist.add("§7  ▪ §a" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.area"));
            } else {
                tiplist.add("§7  ▪ §c" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.single"));
            }
            
            if (canBounced) {
                tiplist.add("§7  ▪ §c" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.blocked"));
            } else {
                tiplist.add("§7  ▪ §a" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.ignoreblock"));
            }
        }
    }
    
    // Heavy attack info
    if (heavyCount > 0) {
        tiplist.add("§9§l" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.heavyattack"));
        for (var i = 0; i < heavyCount; i++) {
            var damage = ScriptAPI.Melee.getHeavyAttackDamage(stack, i);
            var range = ScriptAPI.Melee.getHeavyAttackRange(stack, i);
            var penetration = ScriptAPI.Melee.getHeavyAttackPenetration(stack, i);
            var canBounced = ScriptAPI.Melee.getHeavyAttackCanBounced(stack, i);
            
            if (heavyCount > 1) {
                var phaseNum = (i + 1) | 0; // Convert to integer
                tiplist.add("§7" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.phase") + " " + phaseNum);
            }
            tiplist.add("§3  " + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.damage") + ": §7" + Math.round(damage));
            tiplist.add("§3  " + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.range") + ": §7" + Math.round(range) + "m");
            
            if (penetration) {
                tiplist.add("§7  ▪ §a" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.area"));
            } else {
                tiplist.add("§7  ▪ §c" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.single"));
            }
            
            if (canBounced) {
                tiplist.add("§7  ▪ §c" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.blocked"));
            } else {
                tiplist.add("§7  ▪ §a" + ScriptAPI.Lang.format("mwf:gui.tooltip.melee.ignoreblock"));
            }
        }
    }
}
