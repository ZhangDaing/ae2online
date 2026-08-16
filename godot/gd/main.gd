extends Control

#---------------------------------------------------
@onready var terminal = $Terminal
@onready var config = $Config

func _ready() -> void:
	# 连接设置信号
	config.sort_config_changed.connect(terminal.apply_sort_config)
	config.reflash_time_config_changed.connect(terminal.apply_reflash_time_config)
