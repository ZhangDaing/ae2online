extends Control
#---------------------------------------------------
# 网络请求节点
@onready var http_request = $HTTPRequest
# 物品容器
@onready var card_container = $Background/VBox/Scroll/ScrollContainer/Margin/ItemContainer
# 检索框
@onready var search_box = $Background/VBox/Search/LineEdit
# 刷新定时器
@onready var reflash_timer = $ReflashTimer
# 确认合成
@onready var craft_confirm = $CraftConfirm

# 预加载卡片场景
const CARD_SCENCE = preload("res://tscn/Terminal/item.tscn")

# 后端请求地址
var _url = ""
var _token: Dictionary = {}
var connected = false

# 检索文本
var search_text = ""

# 原始物品数据缓存
var item_cache: Array = []
var terminal_item_cache: Array = [] # 终端物品缓存

# 已加载的卡片缓存
var card_cache: Dictionary = {}

# 排序方式（与 terminal.gd 共用设置）
enum SortType { NAME = 0, AMOUNT = 1, MOD = 2 }
enum SortDir { ASC = 0, DESC = 1 }

var current_sort_type: SortType = 1 as SortType
var current_sort_dir: SortDir = 0 as SortDir

# 请求状态
var requesting = false
#---------------------------------------------------
# ready函数
func _ready():
	# 连接设置信号
	EventBus.sort_config_changed.connect(apply_sort_config)
	EventBus.reflash_time_config_changed.connect(apply_reflash_time_config)
	EventBus.terminal_storage_updated.connect(_on_terminal_storage_updated)
	EventBus.item_clicked.connect(_on_item_clicked)

	# 初始化
	request_data()
#---------------------------------------------------
# 核心函数，请求后端数据（可合成物品）
func request_data() -> void:
	if requesting or !connected:
		return
	requesting = true
	var url = _url + "/craftable"
	var error = http_request.request(url,
		[ "Authorization: Bearer " + _token.get("token", "") ],
		HTTPClient.METHOD_GET)
	if error != OK:
		requesting = false
		print("后端请求失败")

# 请求完成，进行回调
func _on_request_completed(
	result: int,
	response_code: int,
	_headers: PackedStringArray,
	body: PackedByteArray
) -> void:
	requesting = false
	# 检查请求结果
	if result != HTTPRequest.RESULT_SUCCESS:
		print("网络错误")
		return
	if response_code != 200:
		print("请求失败，状态码: ", response_code)
		return

	# 解析JSON数据
	var string = body.get_string_from_utf8()
	var data = JSON.parse_string(string)
	if data == null:
		print("数据解析失败")
		return

	item_cache = data.get("items", []) # 缓存所有物品数据

	#从终端处同步物品数量数据
	# 构建终端物品查找表（rawId -> 物品数据，含 count）
	var terminal_map: Dictionary = {}
	for t_item in terminal_item_cache:
		terminal_map[t_item.get("rawId", "")] = t_item

	# 用终端数据填充 item_cache，终端里没对应数据则跳过
	var merged: Array = []
	for item in item_cache:
		var match = terminal_map.get(item.get("rawId", ""))
		if match != null:
			merged.append(match)
		else:
			var copy = item.duplicate()
			copy["count"] = 0
			merged.append(copy)
	item_cache = merged
	
	refresh_cards(sort_items(item_cache)) # 创建卡片

# 从终端处获取数据
func _on_terminal_storage_updated(items: Array) -> void:
	terminal_item_cache = items
#---------------------------------------------------
# 应用返回的数据
# 遍历生成卡片
func refresh_cards(items: Array) -> void:
	# 处理检索前的卡片缓存
	var keep_cards: Dictionary = {}

	for item in items:
		var item_id = item.get("rawId", "")
		keep_cards[item_id] = true
		if item_id in card_cache:
			# 如果卡片已存在，则更新数据
			card_cache[item_id].update_data(item)
		else:
			# 如果卡片不存在，则创建新的卡片
			var new_card = CARD_SCENCE.instantiate()
			new_card.set_data(item)
			card_container.add_child(new_card)
			card_cache[item_id] = new_card

	# 移除不需要的卡片
	for item_id in card_cache.keys():
		if not keep_cards.has(item_id):
			card_cache[item_id].queue_free()
			card_cache.erase(item_id)

	# 重新排列卡片节点
	for item in items:
		var card = card_cache.get(item.get("rawId", ""))
		card_container.move_child(card, card_container.get_child_count())

	_apply_search_filter() # 应用检索过滤

func _on_reflash_timeout() -> void:
	request_data()

#---------------------------------------------------
# 检索功能
func _on_search_changed(new_text: String) -> void:
	search_text = new_text
	_apply_search_filter()

func _apply_search_filter() -> void:
	var filtered_items = filter(item_cache)
	for item_id in card_cache:
		if item_id in filtered_items:
			card_cache[item_id].visible = true
		else:
			card_cache[item_id].visible = false

func filter(items: Array) -> Dictionary:
	var filtered: Dictionary = {}
	if search_text == "":
		# 检索为空，返回所有物品
		for item in items:
			filtered[item.get("rawId", "")] = true
		return filtered
	# 否则，返回匹配的物品
	for item in items:
		if search_text.to_lower() in item.get("id", "").to_lower():
			filtered[item.get("rawId", "")] = true
	return filtered

#---------------------------------------------------
# 排序算法（与 terminal.gd 共用设置）
func sort_items(items: Array) -> Array:
	var sorted_items = items.duplicate()
	match current_sort_type:
		SortType.NAME:
			sorted_items.sort_custom(_sort_by_name)
		SortType.AMOUNT:
			sorted_items.sort_custom(_sort_by_amount)
		SortType.MOD:
			sorted_items.sort_custom(_sort_by_mod)
	return sorted_items

func _sort_by_name(a, b) -> bool:
	var cmp = _cmp_ignore_case(a.get("id", ""), b.get("id", ""))
	if current_sort_dir == SortDir.DESC:
		cmp = -cmp
	return cmp < 0

func _sort_by_amount(a, b) -> bool:
	# 可合成物品没有 count 字段，退化为按名称排序
	var amt_a = float(a.get("count", 0))
	var amt_b = float(b.get("count", 0))
	var cmp = 0

	if amt_a != amt_b:
		cmp = amt_a - amt_b
	else:
		cmp = _cmp_ignore_case(a.get("id", ""), b.get("id", ""))

	if current_sort_dir == SortDir.DESC:
		cmp = -cmp
	return cmp < 0

func _sort_by_mod(a, b) -> bool:
	var cmp = _cmp_ignore_case(a.get("modid", ""), b.get("modid", ""))
	if cmp == 0:
		cmp = _cmp_ignore_case(a.get("id", ""), b.get("id", ""))
	if current_sort_dir == SortDir.DESC:
		cmp = -cmp
	return cmp < 0

# 忽略大小写比较
func _cmp_ignore_case(a: String, b: String) -> int:
	var lower_a = a.to_lower()
	var lower_b = b.to_lower()
	if lower_a < lower_b:
		return -1
	if lower_a > lower_b:
		return 1
	return 0

#---------------------------------------------------
# 配置设置信号回调
func apply_sort_config(sort_type: int, sort_dir: int) -> void:
	current_sort_type = sort_type as SortType
	current_sort_dir = sort_dir as SortDir
	# 重新排序并刷新卡片
	refresh_cards(sort_items(item_cache))

func apply_reflash_time_config(value: float) -> void:
	reflash_timer.wait_time = value

# 配置登录信号回调
func on_login_succeeded(url: String, token: Dictionary) -> void:
	# 登录成功后，刷新数据
	_url = url
	_token = token
	connected = true
	request_data()

# 配置合成信号回调
func _on_item_clicked(item_data: Dictionary) -> void:
	# 显示确认合成窗口
	craft_confirm.open(_url, _token, item_data)
