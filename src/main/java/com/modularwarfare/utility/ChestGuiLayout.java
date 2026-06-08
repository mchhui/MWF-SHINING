package com.modularwarfare.utility;

/**
 * 箱子综合界面统一布局（服务端容器槽位坐标与客户端绘制共用，避免缩放错位）。
 * <pre>
 * [模型] [ 箱子 9×6 ] [ 背包容器 6×9 ] [ 护甲/背包/背心 ]
 *        [ 玩家 9×3 ]
 *        [ 快捷栏 9×1 ]
 * </pre>
 */
public final class ChestGuiLayout {

    public static final int SLOT = ModUtil.INVENTORY_SLOT_SIZE_PIXELS;
    public static final int MARGIN = 7;
    /** 物品槽相对背景框右下微调，与绘制边框对齐 */
    public static final int SLOT_OFFSET_X = 1;
    public static final int SLOT_OFFSET_Y = 1;

    public static final int CHEST_COLS = 9;
    public static final int CHEST_ROWS = 6;
    public static final int CHEST_SLOTS = CHEST_COLS * CHEST_ROWS;

    /**
     * 箱子界面右侧「背包容器」网格列/行数。修改此处可调整预置显示格数量。
     * 总格数 = BACKPACK_COLS × BACKPACK_ROWS（当前 6×9 = 54）。
     */
    public static final int BACKPACK_COLS = 6;
    public static final int BACKPACK_ROWS = 9;

    public static final int BACKPACK_DISPLAY_SLOTS = BACKPACK_COLS * BACKPACK_ROWS;

    public static final int CHEST_X = MARGIN;
    public static final int CHEST_Y = 17;

    public static final int BACKPACK_X = CHEST_X + CHEST_COLS * SLOT + 12;
    public static final int BACKPACK_Y = CHEST_Y;

    public static final int EQUIP_X = BACKPACK_X + BACKPACK_COLS * SLOT + 12;
    public static final int EQUIP_Y = CHEST_Y;

    /** 玩家区顶边：始终在固定 6 行箱子区域下方 */
    public static final int PLAYER_Y = CHEST_Y + CHEST_ROWS * SLOT + 14;
    public static final int HOTBAR_Y = PLAYER_Y + 3 * SLOT + 4;

    /**
     * 玩家模型预览：相对 guiLeft 的 X（负值 = 面板左缘外侧），相对 guiTop 的 Y。
     */
    public static final int MODEL_OUTSIDE_OFFSET_X = -24;
    public static final int MODEL_OUTSIDE_OFFSET_Y = CHEST_Y + 48;

    public static final int GUI_WIDTH = EQUIP_X + SLOT + MARGIN + 18;
    public static final int GUI_HEIGHT = HOTBAR_Y + SLOT + MARGIN + 8;

    private ChestGuiLayout() {
    }

    public static int chestSlotBgX(final int col) {
        return CHEST_X + col * SLOT;
    }

    public static int chestSlotBgY(final int row) {
        return CHEST_Y + row * SLOT;
    }

    public static int chestSlotX(final int col) {
        return CHEST_X + col * SLOT + SLOT_OFFSET_X;
    }

    public static int chestSlotY(final int row) {
        return CHEST_Y + row * SLOT + SLOT_OFFSET_Y;
    }

    public static int playerSlotBgX(final int col) {
        return CHEST_X + col * SLOT;
    }

    public static int playerSlotBgY(final int row) {
        return PLAYER_Y + row * SLOT;
    }

    public static int playerSlotX(final int col) {
        return CHEST_X + col * SLOT + SLOT_OFFSET_X;
    }

    public static int playerSlotY(final int row) {
        return PLAYER_Y + row * SLOT + SLOT_OFFSET_Y;
    }

    public static int hotbarSlotBgY() {
        return HOTBAR_Y;
    }

    public static int hotbarSlotY() {
        return HOTBAR_Y + SLOT_OFFSET_Y;
    }

    public static int backpackSlotBgX(final int col) {
        return BACKPACK_X + col * SLOT;
    }

    public static int backpackSlotBgY(final int row) {
        return BACKPACK_Y + row * SLOT;
    }

    public static int backpackSlotX(final int col) {
        return BACKPACK_X + col * SLOT + SLOT_OFFSET_X;
    }

    public static int backpackSlotY(final int row) {
        return BACKPACK_Y + row * SLOT + SLOT_OFFSET_Y;
    }

    public static int equipSlotBgX() {
        return EQUIP_X;
    }

    public static int equipSlotBgY(final int index) {
        return EQUIP_Y + index * SLOT;
    }

    public static int equipSlotX() {
        return EQUIP_X + SLOT_OFFSET_X;
    }

    public static int equipSlotY(final int index) {
        return EQUIP_Y + index * SLOT + SLOT_OFFSET_Y;
    }
}
