extends Control

#---------------------------------------------------
@onready var terminal = $Terminal
@onready var config = $Config
@onready var tooltip = $Tooltip

func _ready() -> void:
	# 连接设置信号
	config.sort_config_changed.connect(terminal.apply_sort_config)
	config.reflash_time_config_changed.connect(terminal.apply_reflash_time_config)

	terminal.tooltip_on.connect(tooltip.on)
	terminal.tooltip_off.connect(tooltip.off)