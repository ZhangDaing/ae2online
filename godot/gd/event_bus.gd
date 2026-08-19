@warning_ignore_start("unused_signal")
extends Node

# 设置信号
signal sort_config_changed(sort_type: int, sort_dir: int)
signal reflash_time_config_changed(value: float)

# Tooltip信号
signal tooltip_on(id: String)
signal tooltip_off()

# 终端信号
signal terminal_logged(url: String, token: Dictionary)
signal terminal_choosed(index: int)