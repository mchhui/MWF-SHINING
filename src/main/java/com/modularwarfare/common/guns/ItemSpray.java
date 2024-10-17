package com.modularwarfare.common.guns;

import com.modularwarfare.common.type.BaseItem;

public class ItemSpray extends BaseItem {

    public SprayType type;

    public ItemSpray(SprayType type) {
        super(type);
        this.type = type;
        this.render3d = false;
        this.setMaxDamage(type.usableMaxAmount);
    }


    @Override
    public boolean getShareTag() {
        return true;
    }

}