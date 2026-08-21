extends VBoxContainer

@onready var LoginCongig = load("res://tscn/login_config.tscn")
var next_id:int = 1

#---------------------------------------------------
# ready函数
func _ready() -> void:
	# 读取保存的账号
	for i in range(ConfigManager.get_account_count()):
		var node = LoginCongig.instantiate()
		add_child(node)
		node.get_node("Label").text = "登录配置" + str(next_id)
		node.id = next_id
		next_id += 1
		node.data_changed.connect(_save_account_config)
		node.set_data(ConfigManager.get_account_config(i))
	_on_login_button_pressed()

# 添加配置
func _on_button_pressed() -> void:
	var login_config = LoginCongig.instantiate()
	add_child(login_config)
	login_config.get_node("Label").text = "登录配置" + str(next_id)
	login_config.id = next_id
	next_id += 1
	login_config.data_changed.connect(_save_account_config)


# 删除配置
func _on_button_2_pressed() -> void:
	for i in range(get_child_count() - 1, -1, -1):
		var child = get_child(i)
		if child.has_method("get_data"):
			remove_child(child)
			child.queue_free()
			next_id -= 1
			_save_account_config()
			break

# 登录
func _on_login_button_pressed() -> void:
	EventBus.account_config_changed.emit()
	await get_tree().process_frame # 等待一帧，确保所有子节点都已准备好
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