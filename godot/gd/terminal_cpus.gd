extends Control

#---------------------------------------------------
@onready var http_request = $HTTPRequest
@onready var cpu_container = $Background/ScrollContainer/CPUContainer
@onready var reflash_timer = $ReflashTimer

const CPU_SCENCE = preload("res://tscn/cpu.tscn")

var _url = ""
var _token: Dictionary = {}
var connected = false
var requesting = false

var cpu_cache: Dictionary = {}
#---------------------------------------------------
# ready函数
func _ready() -> void:
	EventBus.reflash_time_config_changed.connect(apply_reflash_time_config)

#---------------------------------------------------
func on_login_succeeded(url: String, token: Dictionary) -> void:
	_url = url
	_token = token
	connected = true
	request_data()

func apply_reflash_time_config(time: float) -> void:
	reflash_timer.wait_time = time

func _on_reflash_timeout() -> void:
	request_data()
#---------------------------------------------------
# 请求cpu状态
func request_data() -> void:
	if requesting or not connected:
		return
	requesting = true
	var url = _url + "/cpu"
	var error = http_request.request(url,
		[ "Authorization: Bearer " + _token.get("token", "") ],
		HTTPClient.METHOD_GET)
	if error != OK:
		requesting = false
		print("CPU状态请求失败")

# 请求完成回调
func _on_request_completed(
	result: int, 
	response_code: int, 
	_headers: PackedStringArray, 
	body: PackedByteArray
) -> void:
	requesting = false
	if result != HTTPRequest.RESULT_SUCCESS:
		print("网络错误")
		return
	var data = JSON.parse_string(body.get_string_from_utf8())
	if response_code != 200:
		print("请求失败，状态码: ", response_code)
		return
	refresh_cpus(data.get("cpus", []))
#---------------------------------------------------
# 刷新cpu状态
func refresh_cpus(cpus: Array) -> void:
	# 记录仍然存在的cpu卡片
	var keep_cards: Dictionary = {}
	
	# 遍历生成新的cpu卡片
	for cpu in cpus:
		var cpu_name = cpu.get("name", "")
		keep_cards[cpu_name] = true
		if cpu_name in cpu_cache:
			cpu_cache[cpu_name].update_data(cpu)
		else:
			var cpu_card = CPU_SCENCE.instantiate()
			cpu_card.set_data(cpu)
			cpu_container.add_child(cpu_card)
			cpu_cache[cpu_name] = cpu_card

	# 移除不再存在的cpu卡片
	for cpu_name in cpu_cache:
		if not keep_cards.has(cpu_name):
			cpu_cache[cpu_name].queue_free()
			cpu_cache.erase(cpu_name)
