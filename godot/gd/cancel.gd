extends Button

@onready var window = get_parent()

func _on_pressed() -> void:
	window.hide()

# 监听键盘输入：按 Esc 键也执行隐藏
func _unhandled_input(event: InputEvent):
	if event is InputEventKey and event.keycode == KEY_ESCAPE and event.pressed and not event.echo:
		# 确保该节点本身是可见的，且父节点存在
		if is_visible_in_tree() and get_parent():
			_on_pressed()
			get_viewport().set_input_as_handled()  # 防止事件继续传播