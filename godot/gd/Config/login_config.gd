extends VBoxContainer
const HttpUtil = preload("res://gd/http_util.gd")

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

	# 服务端使用自签名证书，浏览器会自动提示「继续前往」，
	# 但 Godot 的 HTTPRequest 默认严格验证证书链，会直接连接失败。
	# 这里是局域网内部工具，证书由模组自己生成，故跳过验证。
	_apply_insecure_tls(http)

	var err = http.request(
		_url + "/login",
		[ "Content-Type: application/json" ],
		HTTPClient.METHOD_POST,
		JSON.stringify({
			"username": _username,
			"password": password
		}))
	if err != OK:
		EventBus.login_failed.emit("请求发送失败（错误码 %d），请检查地址格式" % err)

# 让 HTTPRequest 接受自签名证书（Godot 4.2+ 才有 set_tls_options）
static func _apply_insecure_tls(req: HTTPRequest) -> void:
	HttpUtil.allow_self_signed(req)

func _on_login_completed(
	result: int, 
	response_code: int, 
	_headers: PackedStringArray, 
	body: PackedByteArray
) -> void:
	# HTTPRequest 层面的失败（连不上、证书被拒、超时等）
	if result != HTTPRequest.RESULT_SUCCESS:
		var why := _result_text(result)
		print("登录失败：", why, "  URL=", _url)
		EventBus.login_failed.emit("连接失败：%s" % why)
		return

	if response_code != 200:
		var detail := ""
		if body.size() > 0:
			detail = "（%s）" % body.get_string_from_utf8()
		print("登录失败，状态码: ", response_code, detail)
		if response_code == 401:
			EventBus.login_failed.emit("用户名或密码错误")
		elif response_code == 503:
			EventBus.login_failed.emit("服务器无在线终端：请确认终端已接入 ME 网络并有频道")
		else:
			EventBus.login_failed.emit("登录失败，状态码 %d%s" % [response_code, detail])
		return

	var data = JSON.parse_string(body.get_string_from_utf8())
	if data == null:
		EventBus.login_failed.emit("返回数据解析失败")
		return
	var token = {"token": data.get("token", ""),"id": id}
	if token["token"] == "":
		EventBus.login_failed.emit("登录失败：服务器未返回 token")
		return
	EventBus.terminal_logged.emit(_url, token)

# 把 HTTPRequest 的结果码转成可读文字
static func _result_text(result: int) -> String:
	match result:
		HTTPRequest.RESULT_CANT_CONNECT:
			return "无法连接（地址/端口错误，或服务未启动）"
		HTTPRequest.RESULT_CANT_RESOLVE:
			return "无法解析主机名"
		HTTPRequest.RESULT_CONNECTION_ERROR:
			return "连接中断"
		HTTPRequest.RESULT_TLS_HANDSHAKE_ERROR:
			return "TLS 握手失败（证书问题）"
		HTTPRequest.RESULT_NO_RESPONSE:
			return "服务器无响应"
		HTTPRequest.RESULT_TIMEOUT:
			return "请求超时"
		_:
			return "错误码 %d" % result

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
