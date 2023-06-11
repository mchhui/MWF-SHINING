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
        var ammoBullets=ScriptAPI.Ammo.getAcceptedBullet(stack);
        if(ammoBullets.size()>0){
            if(ScriptAPI.Gun.isBulletGun(stack)){
                tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.bullet"));
            }else{
                tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.ammo"));
            }
            for(var i=0;i<ammoBullets.size();i++){
                tiplist.add(ScriptAPI.Lang.format("§7 -"+ammoBullets.get(i)));
            }
        }
        return;
    }
    var ammoStack=stack;
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
    tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.damage") + ": §7 x" + ScriptAPI.Bullet.getDamageFactor(stack));
    tiplist.add("§3" + ScriptAPI.Lang.format("mwf:gui.tooltip.accuracy") + ": §7 x" + ScriptAPI.Bullet.getAccuracyFactor(stack));
}

function updateGunTooltip(stack, tiplist) {
    if (!ScriptAPI.Gun.isGun(stack)) {
        return;
    }

    if (ScriptAPI.Input.isKeyHolding(KEY_CTRL)) {

        var ammoBullets=ScriptAPI.Gun.getAcceptedAmmoOrBullet(stack);
        if(ammoBullets.size()>0){
            if(ScriptAPI.Gun.isBulletGun(stack)){
                tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.bullet"));
            }else{
                tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.ammo"));
            }
            for(var i=0;i<ammoBullets.size();i++){
                tiplist.add(ScriptAPI.Lang.format("§7 -"+ammoBullets.get(i)));
            }
        }

        var attmap=ScriptAPI.Gun.getAcceptedAttachment(stack);
        if(!attmap.isEmpty()){
            tiplist.add(ScriptAPI.Lang.format("mwf:gui.tooltip.helpinfo.attachment"));
            var types=attmap.keySet().toArray();
            for(var i=0;i<types.length;i++){
                tiplist.add("§3"+ScriptAPI.Lang.format("mwf.dictionary."+types[i])+":");
                var atts=attmap.get(types[i]);
                for(var x=0;x<atts.size();x++){
                    tiplist.add(ScriptAPI.Lang.format("§7 -"+atts.get(x)));
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

    if (!ScriptAPI.Input.isKeyHolding(KEY_LSHIFT)) {
        tiplist.add("§e" + ScriptAPI.Lang.format("mwf:gui.tooltip.seemore"));
        tiplist.add("§e" + ScriptAPI.Lang.format("mwf:gui.tooltip.help"));
    } else {
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
