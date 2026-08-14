extends Control

@onready var icon_texture = $IconTexture
@onready var count_label = $CountLabel

# 后端请求地址
const BACKEND_URL = "http://127.0.0.1:11595"

var card_data: Dictionary = {}
#---------------------------------------------------
# ready函数
func _ready() -> void:
	# 节点进入场景树后，如果已有数据则刷新 UI
	if not card_data.is_empty():
		set_data(card_data)
#---------------------------------------------------
# 设置卡片数据
func set_data(data: Dictionary) -> void:
	card_data = data
	if is_node_ready():
		count_label.text = str(card_data.get("count", 0))
		load_cover()

# 加载图标
func load_cover() -> void:
	var icon_url = card_data.get("icon","")
	if icon_url == "":
		return
	var full_url = BACKEND_URL + icon_url
	var loader = HTTPRequest.new()
	add_child(loader)
	loader.request_completed.connect(_on_cover_loaded.bind(loader))
	var error = loader.request(full_url)
	if error != OK:
		loader.queue_free()

# 加载图标以后的回调函数
func _on_cover_loaded(
	result: int,
	response_code: int,
	_headers: PackedStringArray, 
	body: PackedByteArray, 
	loader: HTTPRequest
) -> void:

	# 释放loader节点
	loader.queue_free()

	if result != HTTPRequest.RESULT_SUCCESS:
		print("图标网络错误")
		return
	if response_code != 200:
		print("图标请求失败，状态码: ", response_code)
		return

	var image = Image.new()
	var err = image.load_png_from_buffer(body)
	if err != OK:
		print("图标图片解析失败")
		return
	
	var texture = ImageTexture.create_from_image(image)
	icon_texture.texture = texture
