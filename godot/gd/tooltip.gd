extends Control

@onready var label:Label = $Label

func _process(_delta: float) -> void:
	follow_mouse()

func set_data(id:String) -> void:
	label.text = id

func follow_mouse() -> void:
	var mouse_pos = get_viewport().get_mouse_position()
	global_position = mouse_pos + Vector2(16,-16)

#---------------------------------------------------
# 显示和隐藏tooltip
func on(id:String) -> void:
	visible = true
	set_data(id)

func off() -> void:
	visible = false