extends RefCounted

# 局域网工具：服务端使用自签名证书，Godot 的 HTTPRequest 默认会严格验证
# 证书链并直接失败。这里统一关闭验证，供所有请求复用。
#
# 用法（不依赖 class_name 全局注册，避免依赖项目扫描顺序）：
#   const HttpUtil = preload("res://gd/http_util.gd")
#   HttpUtil.allow_self_signed(http_request)

## 让 HTTPRequest 接受自签名证书。
## Godot 4.2+ 才有 set_tls_options / TLSOptions，低版本自动跳过。
## 注意：方法名是 client_unsafe()（不是 unsafe_client）。
static func allow_self_signed(req) -> void:
	if req == null:
		return
	if not req.has_method("set_tls_options"):
		return
	var opts = TLSOptions.client_unsafe()
	if opts != null:
		req.set_tls_options(opts)


## 把 HTTPRequest 结果码转成可读文字
static func result_text(result: int) -> String:
	match result:
		HTTPRequest.RESULT_CANT_CONNECT:
			return "无法连接（地址/端口错误，或服务未启动）"
		HTTPRequest.RESULT_CANT_RESOLVE:
			return "无法解析主机名"
		HTTPRequest.RESULT_CONNECTION_ERROR:
			return "连接中断"
		HTTPRequest.RESULT_TLS_HANDSHAKE_ERROR:
			return "TLS 握手失败（证书问题）"
		HTTPRequest.RESULT_NO_RESPONSE:
			return "服务器无响应"
		HTTPRequest.RESULT_TIMEOUT:
			return "请求超时"
		_:
			return "错误码 %d" % result
