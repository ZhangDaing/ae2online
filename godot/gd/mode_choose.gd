extends Control

@onready var modes: Array = [
	$"../Terminal",
	$"../TerminalCPUs",
	$"../TerminalCraft"
]

func set_mode(mode: int) -> void:
	for i in range(modes.size()):
		modes[i].visible = (i == mode)

func _on_button1_pressed() -> void:
	set_mode(0)
func _on_button2_pressed() -> void:
	set_mode(1)
func _on_button3_pressed() -> void:
	set_mode(2)
