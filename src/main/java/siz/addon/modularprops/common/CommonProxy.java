package siz.addon.modularprops.common;

import com.modularwarfare.common.type.BaseType;

import java.util.ArrayList;

public class CommonProxy {

    public void preInit() {
        // 服务端预初始化
    }

    public void init() {
        // 服务端初始化
    }

    public void postInit() {
        // 服务端后初始化
    }
    
    /**
     * 生成物品 JSON 模型文件（服务端空实现）
     */
    public void generateJsonModels(ArrayList<BaseType> types) {
        // 服务端不生成
    }
    
    /**
     * 生成语言文件（服务端空实现）
     */
    public void generateLangFiles(ArrayList<BaseType> types, boolean replace) {
        // 服务端不生成
    }
}

