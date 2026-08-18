extends Control


signal terminal_choosed(index:int)

@onready var option_button = $Panel/OptionButton

# 刷新终端选项
func refresh_terminal_options(count:int) -> void:
	option_button.clear()
	for i in range(count):
		option_button.add_item("终端 " + str(i + 1))

# 获取当前选中的终端
func get_selected_terminal() -> int:
	return option_button.get_selected()

# 选项变化时
func _on_terminal_choosed(index:int) -> void:
	terminal_choosed.emit(index + 1)

