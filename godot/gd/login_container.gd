extends VBoxContainer

@onready var LoginCongig = load("res://tscn/login_config.tscn")
var count = 1

# 转发登录
signal login_succeeded(url:String, token:Dictionary)

# 添加配置
func _on_button_pressed() -> void:
	var login_config = LoginCongig.instantiate()
	add_child(login_config)
	login_config.get_node("Label").text = "登录配置" + str(count)
	login_config.login_succeeded.connect(_on_login_succeeded)
	login_config.count = count
	count += 1

# 删除配置
func _on_button_2_pressed() -> void:
	if get_child_count() > 0:
		var last_child = get_child(get_child_count() - 1)
		remove_child(last_child)
		last_child.queue_free()
		count -= 1

# 登录
func _on_login_button_pressed() -> void:
	for child in get_children():
		if child.has_method("login"):
			child.login()

# 转发信号
func _on_login_succeeded(url:String, token:Dictionary) -> void:
	login_succeeded.emit(url, token)