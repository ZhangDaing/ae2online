extends Control

#---------------------------------------------------
@onready var terminal = $Terminal
@onready var config = $Config
@onready var tooltip = $Tooltip
@onready var login_container = $Config/ConfigWindow/Margin/Scroll/VBox/LoginContainer
@onready var terminal_choose = $TerminalChoose

var terminals:Dictionary = {}

func _ready() -> void:
	# 连接设置信号
	config.sort_config_changed.connect(terminal.apply_sort_config)
	config.reflash_time_config_changed.connect(terminal.apply_reflash_time_config)

	# tooltip信号连接
	terminal.tooltip_on.connect(tooltip.on)
	terminal.tooltip_off.connect(tooltip.off)

	# 登录信号连接
	login_container.login_succeeded.connect(_on_terminal_logged)

	# 终端选择信号连接
	terminal_choose.terminal_choosed.connect(_on_terminal_choosed)

func _on_terminal_logged(url:String, token:Dictionary) -> void:
	var count = token.get("count", terminals.size() + 1)
	terminals[count] = {"url": url, "token": token}
	terminal_choose.refresh_terminal_options(terminals.size())
	terminal.on_login_succeeded(url,token)

func _on_terminal_choosed(index:int) -> void:
	if index < 0 or index > terminals.size():
		return
	var t = terminals[index]
	terminal.on_login_succeeded(t["url"], t["token"])
