extends Control

#---------------------------------------------------
@onready var name_label = $VBox/HBox/Name
@onready var job_label = $VBox/HBox/JobName
@onready var time_label = $VBox/HBox/Time
@onready var progress_bar = $VBox/ProgressBar
@onready var cancel_button = $Cancel

var cpu_data: Dictionary = {}

signal cancel_requested(cpu_index: int)
#---------------------------------------------------
# ready函数
func _ready() -> void:
	# 如果有数据，则更新显示
	if not cpu_data.is_empty():
		set_data(cpu_data)

#---------------------------------------------------
# 设置CPU数据
func set_data(data: Dictionary) -> void:
	cpu_data = data
	if is_node_ready():
		_refresh_display()

func update_data(data: Dictionary) -> void:
	cpu_data = data
	if is_node_ready():
		_refresh_display()

func _refresh_display() -> void:
	var cpu_name = cpu_data.get("name")
	if cpu_name == null:
		cpu_name = "CPU"
	name_label.text = cpu_name
	var job = cpu_data.get("job")
	if cpu_data.get("busy", false) and job != null:
		job_label.text = "%s x%d" % [
			job.get("what", ""), int(job.get("amount", 0))]
		time_label.text = _format_time(job.get("elapsedTimeNanos", 0))
		var amount: int = maxi(int(job.get("amount", 0)), 1)
		progress_bar.max_value = amount
		progress_bar.value = int(job.get("progress", 0) )
	else:
		job_label.text = "空闲"
		time_label.text = ""
		progress_bar.value = 0

#---------------------------------------------------
# tooltip显示
func _on_mouse_entered() -> void:
	EventBus.tooltip_on.emit("%s\n并行*%d" % [
		_format_bytes(cpu_data.get("storage", 0)),int(cpu_data.get("coProcessors", 0))
	])

func _on_mouse_exited() -> void:
	EventBus.tooltip_off.emit()

#---------------------------------------------------
@warning_ignore_start("narrowing_conversion")
@warning_ignore_start("integer_division")
# 格式化时间
func _format_time(nanos) -> String:
	var total_seconds:int = int(nanos) / 1_000_000_000.0
	var hours:int = total_seconds / 3600
	var minutes:int = (total_seconds % 3600) / 60
	var seconds:int = total_seconds % 60
	return "%02d:%02d:%02d" % [hours, minutes, seconds]

# 格式化字节
func _format_bytes(value) -> String:
	var b = float(value)
	if b >= 1048576.0:
		return "%.1fMB" % (b / 1048576.0)
	if b >= 1024.0:
		return "%.0fKB" % (b / 1024.0)
	return "%dB" % int(b)

@warning_ignore_restore("narrowing_conversion")
@warning_ignore_restore("integer_division")

#---------------------------------------------------
func _on_gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed:
		#如果取消按钮不可见
		if event.button_index == MOUSE_BUTTON_RIGHT and cpu_data.get("busy", false):
			cancel_button.global_position = event.global_position   # 将菜单移到鼠标位置
			cancel_button.show()

func _on_cancel_pressed() -> void:
	if cpu_data.get("busy", false): 
		cancel_requested.emit(int(cpu_data.get("index", -1)))
		cancel_button.hide()

func _input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed:
		# 按钮不可见时返回
		if not cancel_button.visible:
			return
		# 如果点击在按钮上返回
		if cancel_button.get_global_rect().has_point(event.global_position):
			return
		cancel_button.hide()