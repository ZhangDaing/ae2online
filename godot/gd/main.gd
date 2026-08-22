extends Control

#---------------------------------------------------
@onready var terminal = $Terminal
@onready var terminal_choose = $TerminalChoose
@onready var terminal_cpus = $TerminalCPUs
@onready var terminal_craft = $TerminalCraft

var terminals:Dictionary = {}

var max_id:int = 0

func _ready() -> void:
	# 终端信号连接
	EventBus.terminal_logged.connect(_on_terminal_logged)
	EventBus.terminal_choosed.connect(_on_terminal_choosed)
	EventBus.account_config_changed.connect(_on_account_config_changed)

func _on_terminal_logged(url:String, token:Dictionary) -> void:
	var id = int(token.get("id", 1))
	max_id = max(max_id, id)
	terminals[id] = {"url": url, "token": token}
	terminal_choose.refresh_terminal_options(max_id)
	EventBus.terminal_choosed.emit(terminal_choose.get_selected_terminal())

func _on_terminal_choosed(index:int) -> void:
	var id = index + 1
	if not terminals.has(id):
		return
	var t = terminals[id]
	terminal.on_login_succeeded(t["url"], t["token"])
	terminal_cpus.on_login_succeeded(t["url"], t["token"])
	terminal_craft.on_login_succeeded(t["url"], t["token"]) 

func _on_account_config_changed() -> void:
	# 清空终端数据
	terminals.clear()
	max_id = 0
