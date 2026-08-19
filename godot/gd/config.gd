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
@onready var sort_type = $ConfigWindow/Margin/Scroll/VBox/Config1/OptionButton
@onready var sort_dir = $ConfigWindow/Margin/Scroll/VBox/Config2/OptionButton

func _on_sort_dir_selected(_index: int) -> void:
	EventBus.sort_config_changed.emit(sort_type.selected, sort_dir.selected)

func _on_sort_type_selected(_index: int) -> void:
	EventBus.sort_config_changed.emit(sort_type.selected, sort_dir.selected)

func _on_reflash_time_changed(value: float) -> void:
	EventBus.reflash_time_config_changed.emit(value)
