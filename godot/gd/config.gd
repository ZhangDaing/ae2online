extends Control


#---------------------------------------------------
#按钮控制显示
@onready var window = $ConfigWindow

func _on_button_pressed() -> void:
	if window.visible:
		window.hide()
	else:
		window.show()

#---------------------------------------------------
#返回设置数据
signal sort_config_changed(sort_type, sort_dir)

@onready var sort_type = $ConfigWindow/Margin/Scroll/VBox/Config1/OptionButton
@onready var sort_dir = $ConfigWindow/Margin/Scroll/VBox/Config2/OptionButton

func _on_sort_dir_selected(_index: int) -> void:
	_emit_change()

func _on_sort_type_selected(_index: int) -> void:
	_emit_change()

func _emit_change() -> void:
	sort_config_changed.emit(sort_type.selected, sort_dir.selected)
