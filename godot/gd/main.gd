extends Control

#---------------------------------------------------
# 网络请求节点
@onready var http_request = $HTTPRequest
# 物品容器
@onready var card_container = $Background/VBoxContainer/ScrollContainer/ScrollContainer/CardContainer
# 检索框
@onready var search_box = $Background/VBoxContainer/SortSearch/LineEdit

# 预加载卡片场景
const CARD_SCENCE = preload("res://tscn/item.tscn")
# 后端请求地址
const BACKEND_URL = "http://127.0.0.1:11595"

# 物品缓存(用于检索)
var item_cache:Array = []
#---------------------------------------------------
# ready函数
func _ready():
	# 初始化
	request_data()
	search_box.text_changed.connect(_on_search_changed)
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
#---------------------------------------------------
# 检索功能
func _on_search_changed(new_text:String) -> void:
	if new_text == "":
		create_cards(item_cache)
		return
	var filtered_items:Array = []
	for item in item_cache:
		if new_text.to_lower() in item.get("name", "").to_lower():
			filtered_items.append(item)
	create_cards(filtered_items)