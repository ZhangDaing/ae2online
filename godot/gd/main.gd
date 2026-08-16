extends Control

#---------------------------------------------------
@onready var terminal = $Terminal
@onready var config = $Config

func _ready() -> void:
	config.sort_config_changed.connect(terminal.apply_sort_config)
