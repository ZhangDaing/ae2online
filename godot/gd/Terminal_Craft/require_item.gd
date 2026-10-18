extends Control

@onready var icon_texture = $IconTexture
@onready var require_label = $RequireLabel

# 后端地址：由 craft_confirm 在创建耗材行时注入
var base_url: String = ""

var require_data: Dictionary = {}
#---------------------------------------------------
# ready函数
func _ready() -> void:
    # 节点进入场景树后，如果已有数据则刷新 UI
    if not require_data.is_empty():
        _refresh()

# 设置数据
func set_data(data: Dictionary) -> void:
    require_data = data
    if is_node_ready():
        _refresh()

func _refresh() -> void:
    require_label.text = "可用数量*%d\n合成数量*%d\n缺失数量*%d" % [
        int(require_data.get("count", 0)),
        int(require_data.get("crafting", 0)),
        int(require_data.get("missing", 0)),
    ]
    _load_icon(require_data.get("icon", ""))
#---------------------------------------------------
# 加载图标
func _load_icon(icon_url: String) -> void:
    if icon_url == "":
        return
    if base_url == "":
        return
    var loader = HTTPRequest.new()
    add_child(loader)
    loader.request_completed.connect(_on_icon_loaded.bind(loader))
    var error = loader.request(base_url + icon_url)
    if error != OK:
        loader.queue_free()

func _on_icon_loaded(
    result: int,
    response_code: int,
    _headers: PackedStringArray,
    body: PackedByteArray,
    loader: HTTPRequest
) -> void:
    loader.queue_free()
    if result != HTTPRequest.RESULT_SUCCESS or response_code != 200:
        return
    var image = Image.new()
    if image.load_png_from_buffer(body) == OK:
        icon_texture.texture = ImageTexture.create_from_image(image)