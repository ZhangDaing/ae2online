extends VBoxContainer

@onready var url_input = $UrlInput/LineEdit
@onready var username_input = $UsernameInput/LineEdit
@onready var password_input = $PasswordInput/LineEdit
@onready var http = $HTTPRequest

var _url:String = ""
var _username:String = ""
var id:int = 1

# 防止在读取数据时发送更新信号
var _loading: bool = false

signal data_changed()
#---------------------------------------------------
# 读取数据
func get_data() -> Dictionary:
	return {
		"url": url_input.text,
		"username": username_input.text,
		"password": password_input.text
	}

# 写入数据
func set_data(data: Dictionary) -> void:
	_loading = true
	var saved: String = data.get("url", "")
	# 没有保存过地址时，用「智能默认值」：Web 版用当前页面地址，桌面版用本机 https
	url_input.text = saved if saved != "" else default_server_url()
	username_input.text = data.get("username", "")
	password_input.text = data.get("password", "")
	_loading = false

#---------------------------------------------------
# 生成默认服务器地址
#
# Web 版（浏览器里运行）：必须用当前页面的 host。
#   页面从 https://192.168.31.195:11595/ 加载，就用这个地址；
#   若填 127.0.0.1，浏览器会把它解析成「用户自己的设备」，连不上服务器。
# 桌面/移动客户端：用 https://127.0.0.1:11595（本机服务器）。
static func default_server_url() -> String:
	if OS.has_feature("web"):
		# 浏览器环境：取当前页面地址（含协议、主机、端口）
		var href: String = str(JavaScriptBridge.eval("window.location.origin", true))
		if href != "" and href != "null":
			return href
	return "https://127.0.0.1:11595"
#---------------------------------------------------
# 请求token
func login() -> void:
	_url = url_input.text
	_username = username_input.text
	var password = password_input.text

	http.request(
		_url + "/login",
		[ "Content-Type: application/json" ],
		HTTPClient.METHOD_POST,
		JSON.stringify({
			"username": _username,
			"password": password
		}))

func _on_login_completed(
	_result: int, 
	response_code: int, 
	_headers: PackedStringArray, 
	body: PackedByteArray
) -> void:
	if response_code != 200:
		print("登录失败，状态码: ", response_code)
		return
	var data = JSON.parse_string(body.get_string_from_utf8())
	var token = {"token": data.get("token", ""),"id": id}
	if token["token"] == "":
		print("登录失败：未返回 token")
		return
	EventBus.terminal_logged.emit(_url, token)

#---------------------------------------------------
# 监听输入变化
func _on_url_changed() -> void:
	if not _loading:
		data_changed.emit()

func _on_username_changed() -> void:
	if not _loading:
		data_changed.emit()

func _on_password_changed(_new_text: String) -> void:
	if not _loading:
		data_changed.emit()
