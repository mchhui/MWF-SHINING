package mchhui.sizvehicle.common.type;

public enum BuiltInComponentTypeCar {
    BODY("body"),//车的主体部分 决定外观、质量和生命值
    ENGINE("engine"),//决定速度
    FUEL_TANK("fuel_tank"),//决定最大载油量
    EXHAUST("exhaust"),//排气管 仅外观作用
    SUSPENSION("suspension"),//决定外观 和转弯性能
    TIRE("tire"),//轮胎 决定转弯、漂移能力和外观
    FRONT_ARMOR("front_armor"),//决定外观 和防御力
    SIDE_ARMOR("side_armor"),//决定外观 和防御力
    TOP_ARMOR("top_armor"),//决定外观 和防御力
    REAR_ARMOR("rear_armor"),//决定外观 和防御力
    THRUSTER("thruster"),//决定外观 和推进能力
    WING("wing"),//决定漂移性能
    INTERIOR_DECORATION("interior_decoration"),//车内摆件或挂饰
    LIGHTING_LIGHT("lighting_light"),//照明车灯
    DECORATIVE_LIGHT("decorative_light");//装饰车灯
    
    private String type;

    private BuiltInComponentTypeCar(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
