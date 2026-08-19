extends Node

const CONFIG_PATH = "user://setting.json"

const SECTION_SETTINGS := "settings"
const SECTION_ACCOUNTS := "accounts"
const ACCOUNT_SECTION_PREFIX := "account_"

var _config: ConfigFile = ConfigFile.new()

var _passphrase = "ae2online_local_salt_" + OS.get_unique_id()

#---------------------------------------------------
# ready函数
func _ready():
	load_config()

#---------------------------------------------------
# 加载配置文件
func load_config():
	_config = ConfigFile.new()
	var err = _config.load_encrypted_pass(CONFIG_PATH, _passphrase)
	if err != OK and err != ERR_FILE_NOT_FOUND:
		print("Failed to load config file: ", err)

# 保存配置文件
func save_config():
	var err = _config.save_encrypted_pass(CONFIG_PATH, _passphrase)
	if err != OK:
		print("Failed to save config file: ", err)
#---------------------------------------------------
# 读值
func get_value(section: String, key: String, default_value = null):
	return _config.get_value(section, key, default_value)

# 写值
func set_value(section: String, key: String, value):
	_config.set_value(section, key, value)
	save_config()
#---------------------------------------------------
# 普通配置设置
func get_config(key: String, default_value = null):
	return _config.get_value(SECTION_SETTINGS, key, default_value)
func set_config(key: String, value):
	_config.set_value(SECTION_SETTINGS, key, value)
	save_config()

# 账号设置
func get_account_config(index:int) -> Dictionary:
	var section = ACCOUNT_SECTION_PREFIX + str(index)
	return {
		"url": get_value(section, "url", ""),
		"username": get_value(section, "username", ""),
		"password": get_value(section, "password", ""),
	}

func get_account_count() -> int:
	return _config.get_value(SECTION_ACCOUNTS, "count", 0)

func set_account_config(accounts: Array):
	# 清空旧账号
	for i in range(get_account_count()):
		_config.erase_section(ACCOUNT_SECTION_PREFIX + str(i))
	_config.set_value(SECTION_ACCOUNTS, "count", accounts.size())
	# 写入新账号
	for i in range(accounts.size()):
		var acc: Dictionary = accounts[i]
		var section = ACCOUNT_SECTION_PREFIX + str(i)
		_config.set_value(section, "url", acc.get("url", ""))
		_config.set_value(section, "username", acc.get("username", ""))
		_config.set_value(section, "password", acc.get("password", ""))
	save_config()
