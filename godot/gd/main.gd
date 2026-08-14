extends Control

#---------------------------------------------------
# 网络请求节点
@onready var http_request = $HTTPRequest
# 卡片容器
@onready var scroll_container = $VBoxContainer/ScrollContainer
@onready var card_container = $VBoxContainer/ScrollContainer/CardContainer

# 预加载卡片场景
const CARD_SCENCE = preload("res://tscn/animecard.tscn")
# 后端请求地址
const BACKEND_URL = "http://127.0.0.1:11595"

#---------------------------------------------------
# ready函数
func _ready():
	# 初始化
	request_data()

# process函数
func _process(_delta: float) -> void:
	pass

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
	
	# 根据模式返回内容
	var item:Array = []
	item = data.get("items", [])
	create_cards(item)

#---------------------------------------------------
# 应用返回的数据
# 遍历生成卡片
func create_cards(data: Array) -> void:
	await _clear_cards()
	for item in data:
		var card = CARD_SCENCE.instantiate()
		card.set_data(item)
		card_container.add_child(card)
	cover_loading = true
	load_cover()

# 分页添加卡片
func append_cards(data:Array) -> void:
	for item in data:
		var card = CARD_SCENCE.instantiate()
		card.set_data(item)
		card_container.add_child(card)
	cover_loading = true
	load_cover()

# 加载卡片封面
var cover_loading:bool = false # 标志可以加载封面，防止竞态条件
var cover_loading_task:bool = false # 标志加载封面任务是否在运行
func load_cover() -> void:
	for card in card_container.get_children():
		if not cover_loading:
			return
		if not is_instance_valid(card):
			continue
		card.load_cover()
		await get_tree().create_timer(0.2).timeout

# 清空卡片容器
func _clear_cards() -> void:
	cover_loading = false
	for child in card_container.get_children():
		child.queue_free()
	await get_tree().process_frame
#---------------------------------------------------
