extends Control
const HttpUtil = preload("res://gd/http_util.gd")

# 节点引用
@onready var what_label = $What
@onready var byte_label = $Byte
@onready var item_container = $ScrollPanel/ScrollContainer/BoxContainer
@onready var plan_button = $Plan/PlanButton
@onready var craft_button = $Craft/Craft
@onready var cpu_option = $CPUs/OptionButton
@onready var count_spinbox = $Count/SpinBox

# 网络请求节点
@onready var _http_plan = $HTTPPlan
@onready var _http_craft = $HTTPCraft

# 预加载耗材行场景
const REQUIRE_ITEM_SCENE = preload("res://tscn/Terminal_Craft/require_item.tscn")

# 数据
var _url = ""
var _token: Dictionary = {}
var _item: Dictionary = {}
var _cpus_cache: Array = []
var count: int = 1
#---------------------------------------------------
# ready函数
func _ready() -> void:
	EventBus.terminal_cpus_updated.connect(_on_cpus_updated)
	# 按钮
	craft_button.disabled = true
	hide()

# 打开确认窗口
func open(url: String, token: Dictionary, item: Dictionary) -> void:
	_url = url
	_token = token
	_item = item
	count_spinbox.value = 1
	count = 1

	what_label.text = "正在合成 %s" % [_item.get("id", "")]
	byte_label.text = ""
	for child in item_container.get_children():
		child.queue_free()
	craft_button.disabled = true

	_refresh_cpus()
	show()

# CPU 下拉
func _refresh_cpus() -> void:
	cpu_option.clear()
	cpu_option.add_item("自动分配", -1)
	for i in range(_cpus_cache.size()):
		var cpu = _cpus_cache[i]
		var raw_name = cpu.get("name", "")
		var cpu_name: String
		if raw_name != "" and raw_name is String:
			cpu_name = raw_name
		else:
			cpu_name = "CPU %d" % i
		var state: String = "忙" if cpu.get("busy", false) else "空闲"
		cpu_option.add_item("%s [%s]" % [cpu_name, state], i)

func _on_cpus_updated(cpus: Array) -> void:
	_cpus_cache = cpus
#---------------------------------------------------
# 计算按钮
func _on_plan_pressed() -> void:
	plan_button.disabled = true
	byte_label.text = "计算中..."
	what_label.text = "正在合成 %s" % [_item.get("id", "")]
	count = int(count_spinbox.value)
	var body = JSON.stringify({
		"rawId": _item.get("rawId", ""),
		"count": int(count_spinbox.value),
	})
	HttpUtil.allow_self_signed(_http_plan)
	_http_plan.request(_url + "/craft/plan",
		[ "Authorization: Bearer " + _token.get("token", ""), "Content-Type: application/json" ],
		HTTPClient.METHOD_POST, body)

func _on_plan_completed(
	result: int,
	response_code: int,
	_headers: PackedStringArray,
	body: PackedByteArray
) -> void:
	plan_button.disabled = false
	if result != HTTPRequest.RESULT_SUCCESS:
		byte_label.text = "网络错误"
		return
	var data = JSON.parse_string(body.get_string_from_utf8())
	if response_code != 200 or data == null:
		byte_label.text = "计算失败"
		return

	byte_label.text = _format_bytes(int(data.get("bytes", 0)))
	_fill_items(data)
	# 缺料时禁止合成
	craft_button.disabled = data.get("simulation", false)
#---------------------------------------------------
# 生成耗材行
func _fill_items(data: Dictionary) -> void:
	for child in item_container.get_children():
		child.queue_free()

	# 可用数量 = usedItems（从存储能提取的量）
	for m in data.get("used", []):
		var row = REQUIRE_ITEM_SCENE.instantiate()
		row.base_url = _url
		row.set_data(m)
		item_container.add_child(row)
	# 缺失的材料（若需要单独展示）
	for m in data.get("missing", []):
		var row = REQUIRE_ITEM_SCENE.instantiate()
		row.base_url = _url
		row.set_data(m)
		item_container.add_child(row)
#---------------------------------------------------
# 合成按钮
func _on_craft_pressed() -> void:
	craft_button.disabled = true
	var body = JSON.stringify({
		"rawId": _item.get("rawId", ""),
		"count": int(count),
		"cpu": int(cpu_option.get_selected_id())
	})
	HttpUtil.allow_self_signed(_http_craft)
	_http_craft.request(_url + "/craft",
		[ "Authorization: Bearer " + _token.get("token", ""), "Content-Type: application/json" ],
		HTTPClient.METHOD_POST, body)

func _on_craft_completed(
	result: int,
	response_code: int,
	_headers: PackedStringArray,
	body: PackedByteArray
) -> void:
	craft_button.disabled = false
	if result != HTTPRequest.RESULT_SUCCESS:
		byte_label.text = "网络错误"
		return
	var data = JSON.parse_string(body.get_string_from_utf8())
	if response_code == 200:
		hide()
	else:
		var msg = "合成失败"
		if data != null:
			msg = "合成失败: %s" % data.get("error", "")
		byte_label.text = msg
#---------------------------------------------------
# 格式化字节
func _format_bytes(value: int) -> String:
	var b = float(value)
	if b >= 1048576.0:
		return "%.1fMB" % (b / 1048576.0)
	if b >= 1024.0:
		return "%.0fKB" % (b / 1024.0)
	return "%dB" % int(b)
