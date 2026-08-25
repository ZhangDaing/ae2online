extends Panel

@onready var config = $"../Config"

func _on_button_pressed() -> void:
	if config.visible:
		config.hide()
	else:
		config.show()