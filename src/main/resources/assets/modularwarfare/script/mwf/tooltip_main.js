var KEY_LSHIFT = 42;
var KEY_CTRL = 29;

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
                tiplist.add(ScriptAPI.Lang.format("§7 -" + ammoBullets.get(i)));
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
                        var effectName = ScriptAPI.Lang.format(potionEntry.potionEffect.getPotion().getName());
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
                            var effectName = ScriptAPI.Lang.format(potionEntry.potionEffect.getPotion().getName());
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
                tiplist.add(ScriptAPI.Lang.format("§7 -" + ammoBullets.get(i)));
            }
        }

        var attmap = ScriptAPI.Gun.getAcceptedAttachment(stack);
        if (!attmap.isEmpty()) {
            tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.attachment"));
            var types = attmap.keySet().toArray();
            for (var i = 0; i < types.length; i++) {
                tiplist.add("§3" + ScriptAPI.Lang.format("mwf.dictionary." + types[i]) + ":");
                var atts = attmap.get(types[i]);
                for (var x = 0; x < atts.size(); x++) {
                    tiplist.add(ScriptAPI.Lang.format("§7 -" + atts.get(x)));
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
        finalAccuracy *= ScriptAPI.Bullet.getAccuracyFactor(bulletStack);
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
                tiplist.add("§7- " + attachmentList.get(i));
            }
        }
    }

    if (ScriptAPI.Gun.getGunExtraLore(stack) != null) {
        tiplist.add(ScriptAPI.Gun.getGunExtraLore(stack));
    }
}
