extends Control

@onready var sort_type = $ConfigWindow/Margin/Scroll/VBox/Config1/OptionButton
@onready var sort_dir = $ConfigWindow/Margin/Scroll/VBox/Config2/OptionButton
@onready var reflash_time = $ConfigWindow/Margin/Scroll/VBox/Config3/SpinBox
#---------------------------------------------------
# ready函数
func _ready():
	# 读取保存的配置
	sort_type.select(int(ConfigManager.get_config("sort_type", 1)))
	sort_dir.select(int(ConfigManager.get_config("sort_dir", 0)))
	reflash_time.value = float(ConfigManager.get_config("reflash_time", 1.0))
	# 应用到终端（main.tscn 中 Terminal 先于 Config ready，已订阅 EventBus）
	_emit_change()

#---------------------------------------------------
#按钮控制显示
@onready var window = $ConfigWindow

func _on_button_pressed() -> void:
	if window.visible:
		window.hide()
	else:
		window.show()

#---------------------------------------------------
func _on_sort_dir_selected(_index: int) -> void:
	_emit_change()

func _on_sort_type_selected(_index: int) -> void:
	_emit_change()

func _on_reflash_time_changed(_value: float) -> void:
	_emit_change()

func _emit_change() -> void:
	EventBus.sort_config_changed.emit(sort_type.selected, sort_dir.selected)
	EventBus.reflash_time_config_changed.emit(reflash_time.value)
	ConfigManager.set_config("sort_type", sort_type.selected)
	ConfigManager.set_config("sort_dir", sort_dir.selected)
	ConfigManager.set_config("reflash_time", reflash_time.value)
