extends Control

@onready var sort_type = $ConfigWindow/Margin/Scroll/VBox/Config1/OptionButton
@onready var sort_dir = $ConfigWindow/Margin/Scroll/VBox/Config2/OptionButton
@onready var reflash_time = $ConfigWindow/Margin/Scroll/VBox/Config3/SpinBox
@onready var theme_option = $ConfigWindow/Margin/Scroll/VBox/Config4/OptionButton

# 初始化中标志：避免 _ready 里给控件赋值时触发 on_change 提前保存/广播
var _loading := false
#---------------------------------------------------
# ready函数
func _ready():
	_loading = true
	# 读取保存的配置
	sort_type.select(int(ConfigManager.get_config("sort_type", 1)))
	sort_dir.select(int(ConfigManager.get_config("sort_dir", 0)))
	reflash_time.value = float(ConfigManager.get_config("reflash_time", 1.0))
	theme_option.select(int(ConfigManager.get_config("theme", 0)))
	_loading = false
	# 启动时统一应用一次（只广播，不重复写回配置）
	_apply_all()
#---------------------------------------------------
# 排序方式
func _on_sort_type_selected(index: int) -> void:
	if _loading:
		return
	EventBus.sort_config_changed.emit(index, sort_dir.selected)
	ConfigManager.set_config("sort_type", index)

# 排序方向
func _on_sort_dir_selected(index: int) -> void:
	if _loading:
		return
	EventBus.sort_config_changed.emit(sort_type.selected, index)
	ConfigManager.set_config("sort_dir", index)

# 刷新时间
func _on_reflash_time_changed(value: float) -> void:
	if _loading:
		return
	EventBus.reflash_time_config_changed.emit(value)
	ConfigManager.set_config("reflash_time", value)

# 主题
func _on_theme_selected(index: int) -> void:
	if _loading:
		return
	EventBus.theme_config_changed.emit(index)
	ConfigManager.set_config("theme", index)

# 启动时统一应用（仅广播到订阅者，不覆盖已保存的配置）
func _apply_all() -> void:
	EventBus.sort_config_changed.emit(sort_type.selected, sort_dir.selected)
	EventBus.reflash_time_config_changed.emit(reflash_time.value)
	EventBus.theme_config_changed.emit(theme_option.selected)


