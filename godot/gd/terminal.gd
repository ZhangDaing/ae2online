extends Control

#---------------------------------------------------
# 网络请求节点
@onready var http_request = $HTTPRequest
# 物品容器
@onready var card_container = $Background/VBox/Scroll/ScrollContainer/Margin/ItemContainer
# 检索框
@onready var search_box = $Background/VBox/Search/LineEdit

# 预加载卡片场景
const CARD_SCENCE = preload("res://tscn/item.tscn")
# 后端请求地址
const BACKEND_URL = "http://127.0.0.1:11595"

# 物品缓存(用于检索)
var item_cache:Array = []

# 排序方式
enum SortType {NAME = 0,AMOUNT = 1,MOD = 2}
enum SortDir{ASC = 0,DESC = 1}

var current_sort_type:SortType = 1 as SortType
var current_sort_dir:SortDir = 0 as SortDir
#---------------------------------------------------
# ready函数
func _ready():
	# 初始化
	request_data()
#---------------------------------------------------
# 核心函数，请求后端数据
# 请求数据
func request_data() -> void:
	var url = BACKEND_URL + "/storage"
	var error = http_request.request(url)
	if error != OK:
		print("后端请求失败")

# 请求完成，进行回调
func _on_request_completed(
	result:int, 
	response_code:int, 
	_headers:PackedStringArray, 
	body:PackedByteArray
) -> void:
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
	item_cache = sort_items(item_cache)
	create_cards(item_cache) # 创建卡片
#---------------------------------------------------
# 应用返回的数据
# 遍历生成卡片
func create_cards(data: Array) -> void:
	await _clear_cards()
	for item in data:
		var card = CARD_SCENCE.instantiate()
		card.set_data(item)
		card_container.add_child(card)

# 清空卡片容器
func _clear_cards() -> void:
	for child in card_container.get_children():
		child.queue_free()
	await get_tree().process_frame

func _refresh() -> void:
	request_data() 

#---------------------------------------------------
# 检索功能
func _on_search_changed(new_text:String) -> void:
	if new_text == "":
		create_cards(item_cache)
		return
	var filtered_items:Array = []
	for item in item_cache:
		if new_text.to_lower() in item.get("id", "").to_lower():
			filtered_items.append(item)
	filtered_items = sort_items(filtered_items)
	create_cards(filtered_items)

# 排序算法
func sort_items(items:Array) -> Array:
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
	var amt_a = float(a["count"]) / float(a.get("amountPerUnit", 1))
	var amt_b = float(b["count"]) / float(b.get("amountPerUnit", 1))
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

# 忽略大小写
func _cmp_ignore_case(a:String, b:String) -> int:
	var lower_a = a.to_lower()
	var lower_b = b.to_lower()
	if lower_a < lower_b:
		return -1
	if lower_a > lower_b:
		return 1
	return 0

#---------------------------------------------------
# 配置窗口信号回调
func apply_sort_config(sort_type:int, sort_dir:int) -> void:
	current_sort_type = sort_type as SortType
	current_sort_dir = sort_dir as SortDir
	_refresh()
