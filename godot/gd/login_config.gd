extends VBoxContainer

signal login_succeeded(url:String, token:Dictionary)

@onready var url_input = $UrlInput/LineEdit
@onready var username_input = $UsernameInput/LineEdit
@onready var password_input = $PasswordInput/LineEdit
@onready var http = $HTTPRequest

var _url:String = ""
var _username:String = ""
var count = 0

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
	var token = {"token": data.get("token", ""),"count": count}
	if token["token"] == "":
		print("登录失败：未返回 token")
		return
	login_succeeded.emit(_url, token)
