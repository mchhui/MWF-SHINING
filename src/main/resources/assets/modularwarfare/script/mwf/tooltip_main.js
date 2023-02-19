var KEY_LSHIFT=42;

function updateTooltip(stack, tiplist) {
    if (ScriptAPI.Gun.isGun(stack)) {
        updateGunTooltip(stack, tiplist)
    }
}

function updateGunTooltip(stack, tiplist) {
    if (!ScriptAPI.Gun.isGun(stack)) {
        return;
    }
    var text_firemode=ScriptAPI.Lang.format("mwf:dictionary.firemode");
    var text_firemode_vaule=ScriptAPI.Lang.format("mwf:dictionary.firemode.full");
    /**
     * Firemode
     */
    switch(ScriptAPI.Gun.getFireMode(stack)){
        case WeaponFireMode.SEMI:
            text_firemode_vaule=ScriptAPI.Lang.format("mwf:dictionary.firemode.semi");
            break;
        case WeaponFireMode.BRUST:
            text_firemode_vaule=ScriptAPI.Lang.format("mwf:dictionary.firemode.brust");
            break;
    }
    tiplist.add("§3"+text_firemode+": §7"+text_firemode_vaule);
    
    /**
     * Ammo
     */
    if(ScriptAPI.Gun.hasAmmoLoaded(stack)){
        var ammoStack=ScriptAPI.Gun.getAmmoStack(stack);
        if(!ammoStack.isEmpty()){
            var ammoType=ScriptAPI.Ammo.getAmmoType(ammoStack);
            if (ammoType.magazineCount == 1) {
                var ammocount=NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack),"ammocount");
                tiplist.add("§3"+ScriptAPI.Lang.format("mwf:gui.tooltip.ammo")+": §7"+ammocount+" / "+ammoType.ammoCapacity);
            }else{
                var magcount=NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack),"magcount");
                var ammocount=NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack),"ammocount"+magcount);
                if(ScriptAPI.Input.isKeyHolding(KEY_LSHIFT)){
                    for(var i=1;i<=ammoType.magazineCount;i++){
                        ammocount=NBTSearcher.searchInt(ScriptAPI.Stack.getNbt(ammoStack),"ammocount"+i);
                        if(i==magcount){
                            tiplist.add("§3"+ScriptAPI.Lang.format("mwf:gui.tooltip.ammo")+ScriptAPI.Lang.format("mwf:gui.tooltip.ammo.num"+i)+": §e"+ammocount+" / "+ammoType.ammoCapacity);
                        }else{
                            tiplist.add("§3"+ScriptAPI.Lang.format("mwf:gui.tooltip.ammo")+ScriptAPI.Lang.format("mwf:gui.tooltip.ammo.num"+i)+": §7"+ammocount+" / "+ammoType.ammoCapacity);
                        }
                    }
                }else{
                    tiplist.add("§3"+ScriptAPI.Lang.format("mwf:gui.tooltip.ammo")+": §7"+ammocount+" / "+ammoType.ammoCapacity);
                }
            }
        }
    }
    var bulletItem=ScriptAPI.Gun.getUsedBulletItem(stack);
    
    if(!ScriptAPI.Input.isKeyHolding(KEY_LSHIFT)){
        tiplist.add("§e"+ScriptAPI.Lang.format("mwf:gui.tooltip.seemore"));
    }
}