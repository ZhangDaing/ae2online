extends Control

#---------------------------------------------------
@onready var terminal = $Terminal
@onready var terminal_choose = $TerminalChoose

var terminals:Dictionary = {}

func _ready() -> void:
	# 终端信号连接
	EventBus.terminal_logged.connect(_on_terminal_logged)
	EventBus.terminal_choosed.connect(_on_terminal_choosed)

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
