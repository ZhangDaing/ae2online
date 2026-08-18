extends Control

#---------------------------------------------------
@onready var terminal = $Terminal
@onready var config = $Config
@onready var tooltip = $Tooltip
@onready var login_container = $Config/ConfigWindow/Margin/Scroll/VBox/LoginContainer

func _ready() -> void:
	# 连接设置信号
	config.sort_config_changed.connect(terminal.apply_sort_config)
	config.reflash_time_config_changed.connect(terminal.apply_reflash_time_config)

	# tooltip信号连接
	terminal.tooltip_on.connect(tooltip.on)
	terminal.tooltip_off.connect(tooltip.off)

	# 连接登录信号
	login_container.login_succeeded.connect(terminal.on_login_succeeded)
