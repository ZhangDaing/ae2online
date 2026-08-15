extends ScrollContainer

const STEP = 72

func _on_gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed:
		match event.button_index:
			MOUSE_BUTTON_WHEEL_UP:
				set_v_scroll(scroll_vertical - STEP)
				accept_event()
			MOUSE_BUTTON_WHEEL_DOWN:
				set_v_scroll(scroll_vertical + STEP)
				accept_event()
	if event is InputEventPanGesture:
        # 触摸板双指滚动：delta.y > 0 表示向下滚
		if event.delta.y != 0.0:
			set_v_scroll(scroll_vertical + int(event.delta.y * STEP))
			accept_event()