extends VBoxContainer

@onready var LoginCongig = load("res://tscn/login_config.tscn")
var count = 1

#---------------------------------------------------
# ready函数
func _ready() -> void:
	# 读取保存的账号
	for i in range(ConfigManager.get_account_count()):
		var node = LoginCongig.instantiate()
		add_child(node)
		node.get_node("Label").text = "登录配置" + str(i)
		node.count = i + 1
		node.data_changed.connect(_save_account_config)
		node.set_data(ConfigManager.get_account_config(i))
		count += 1
	_on_login_button_pressed()

# 添加配置
func _on_button_pressed() -> void:
	var login_config = LoginCongig.instantiate()
	add_child(login_config)
	login_config.get_node("Label").text = "登录配置" + str(count)
	login_config.count = count
	login_config.data_changed.connect(_save_account_config)
	count += 1

# 删除配置
func _on_button_2_pressed() -> void:
	for child in get_children():
		if child.has_method("get_data"):
			child.queue_free()
			count -= 1
			break

# 登录
func _on_login_button_pressed() -> void:
	for child in get_children():
		if child.has_method("login"):
			child.login()

# 保存账号配置
func _save_account_config() -> void:
	var accounts: Array = []
	for child in get_children():
		if child.has_method("get_data"):
			accounts.append(child.get_data())
	ConfigManager.set_account_config(accounts)