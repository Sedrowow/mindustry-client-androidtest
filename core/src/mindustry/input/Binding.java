package mindustry.input;

import arc.KeyBinds.*;
import arc.input.InputDevice.*;
import arc.input.*;
import mindustry.*;

public enum Binding implements KeyBind{
    move_x(new Axis(KeyCode.a, KeyCode.d), "general"),
    move_y(new Axis(KeyCode.s, KeyCode.w)),
    mouse_move(KeyCode.mouseBack),
    pan(KeyCode.mouseForward),

    boost(KeyCode.shiftLeft),
    respawn(KeyCode.v),
    control(KeyCode.controlLeft),
    select(KeyCode.mouseLeft),
    deselect(KeyCode.mouseRight),
    break_block(KeyCode.mouseRight),

    pickupCargo(KeyCode.leftBracket),
    dropCargo(KeyCode.rightBracket),

    clear_building(KeyCode.q),
    pause_building(KeyCode.e),
    rotate(new Axis(KeyCode.scroll)),
    rotateplaced(KeyCode.r),
    diagonal_placement(KeyCode.controlLeft),
    pick(KeyCode.mouseMiddle),

    rebuild_select(KeyCode.b),
    schematic_select(KeyCode.f),
    schematic_flip_x(KeyCode.z),
    schematic_flip_y(KeyCode.x),
    schematic_menu(KeyCode.t),


    command_mode(KeyCode.shiftLeft, "command"),
    command_queue(KeyCode.mouseMiddle),
    create_control_group(KeyCode.controlLeft),

    select_all_units(KeyCode.g),
    select_all_unit_factories(KeyCode.h),

    cancel_orders(KeyCode.unset),

    unit_stance_shoot(KeyCode.unset),
    unit_stance_hold_fire(KeyCode.unset),
    unit_stance_pursue_target(KeyCode.unset),
    unit_stance_patrol(KeyCode.unset),
    unit_stance_ram(KeyCode.unset),

    unit_command_move(KeyCode.unset),
    unit_command_repair(KeyCode.unset),
    unit_command_rebuild(KeyCode.unset),
    unit_command_assist(KeyCode.unset),
    unit_command_mine(KeyCode.unset),
    unit_command_boost(KeyCode.unset),
    unit_command_enter_payload(KeyCode.unset),
    unit_command_load_units(KeyCode.unset),
    unit_command_load_blocks(KeyCode.unset),
    unit_command_unload_payload(KeyCode.unset),
    unit_command_loop_payload(KeyCode.unset),

    category_prev(KeyCode.comma, "blocks"),
    category_next(KeyCode.period),

    block_select_left(KeyCode.left),
    block_select_right(KeyCode.right),
    block_select_up(KeyCode.up),
    block_select_down(KeyCode.down),
    block_select_01(KeyCode.num1),
    block_select_02(KeyCode.num2),
    block_select_03(KeyCode.num3),
    block_select_04(KeyCode.num4),
    block_select_05(KeyCode.num5),
    block_select_06(KeyCode.num6),
    block_select_07(KeyCode.num7),
    block_select_08(KeyCode.num8),
    block_select_09(KeyCode.num9),
    block_select_10(KeyCode.num0),

    zoom(new Axis(KeyCode.scroll), "view"),
    detach_camera(KeyCode.unset),
    menu(Vars.android ? KeyCode.back : KeyCode.escape),
    fullscreen(KeyCode.f11),
    pause(KeyCode.space),
    minimap(KeyCode.m),
    research(KeyCode.j),
    planet_map(KeyCode.n),
    block_info(KeyCode.f1),
    toggle_menus(KeyCode.c),
    screenshot(KeyCode.p),
    toggle_power_lines(KeyCode.f5),
    toggle_block_status(KeyCode.f6),
    player_list(KeyCode.tab, "multiplayer"),
    chat(KeyCode.enter),
    chat_history_prev(KeyCode.up),
    chat_history_next(KeyCode.down),
    chat_scroll(new Axis(KeyCode.scroll)),
    chat_mode(KeyCode.tab),
    console(KeyCode.f8),

    tile_actions_menu_modifier(KeyCode.altLeft, "client.name"),
    freecam_modifier(KeyCode.altLeft),
    reset_camera(KeyCode.r),
    place_waypoint(KeyCode.y),
    drop_payload(KeyCode.backslash),
    navigate_to_cursor(KeyCode.z),
    stop_following_path(KeyCode.minus),
    show_turret_ranges(KeyCode.backtick),
    hide_blocks(KeyCode.i),
    invisible_units(KeyCode.o),
    show_reactor_and_dome_ranges(KeyCode.f9),
    toggle_placement_modifiers(KeyCode.shiftLeft),
    chat_autocomplete(KeyCode.tab),
    auto_build(KeyCode.semicolon),
    auto_repair(KeyCode.l),
    auto_mine(KeyCode.k),
    toggle_strict_mode(KeyCode.f7),
    find_modifier(KeyCode.controlLeft),
    find(KeyCode.f),
    send_build_queue(KeyCode.apostrophe),
    toggle_auto_target(KeyCode.x),
    show_massdriver_configs(KeyCode.f3),
    force_place_modifier(KeyCode.altLeft),
    run_js(KeyCode.u),
    ;

    private final KeybindValue defaultValue;
    private final String category;

    Binding(KeybindValue defaultValue, String category){
        this.defaultValue = defaultValue;
        this.category = category;
    }

    Binding(KeybindValue defaultValue){
        this(defaultValue, null);
    }

    @Override
    public KeybindValue defaultValue(DeviceType type){
        return defaultValue;
    }

    @Override
    public String category(){
        return category;
    }
}
