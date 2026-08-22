extends LineEdit

# 修改右键，清除文本
func _on_gui_input(event: InputEvent) -> void:
	if (
	event is InputEventMouseButton and
	event.button_index == MOUSE_BUTTON_RIGHT and 
	event.pressed
	):
		clear()