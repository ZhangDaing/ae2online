extends Control

@onready var cover = $ClipGround/VBoxContainer/Cover
@onready var title = $ClipGround/VBoxContainer/Title

# 后端请求地址
const BACKEND_URL = "http://127.0.0.1:11595"

var card_data: Dictionary = {}

func _ready() -> void:
	# 节点进入场景树后，如果已有数据则刷新 UI
	if not card_data.is_empty():
		set_data(card_data)

# 设置卡片数据
func set_data(data: Dictionary) -> void:
	card_data = data
	if is_node_ready():
		title.text = card_data.get("id","未知物品")
		load_cover()

# 加载封面
func load_cover() -> void:
	var icon_url = card_data.get("icon"," ")
	if icon_url == "":
		return
	var full_url = BACKEND_URL + icon_url
	var loader = HTTPRequest.new()
	add_child(loader)
	loader.request_completed.connect(_on_cover_loaded.bind(loader))
	var error = loader.request(full_url)
	if error != OK:
		loader.queue_free()

# 加载封面以后的回调函数
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
		print("封面网络错误")
		return
	if response_code != 200:
		print("封面请求失败，状态码: ", response_code)
		return

	var image = Image.new()
	var err = image.load_png_from_buffer(body)
	if err != OK:
		print("封面图片解析失败")
		return
	
	var texture = ImageTexture.create_from_image(image)
	cover.texture = texture
