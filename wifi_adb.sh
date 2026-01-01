#!/bin/bash

# --- Android Wi-Fi ADB 助手 (生产环境最终版 v2.1) ---
#
# v2.1 更新:
# - 修复了在正常退出(如 -h, disconnect)时依然触发 cleanup 函数的 bug。
#
# 特性:
# - 智能连接与断开
# - 自动处理单/多 USB 设备场景
# - 多种方式获取 IP，兼容性更强
# - 连接超时与重试机制
# - 脚本异常退出时自动恢复 USB 调试模式
# - 支持通过参数自定义端口
# - 统一的日志函数与详细的帮助信息

# --- 安全设置 ---
set -euo pipefail

# --- 全局变量与常量 ---
DEFAULT_PORT=5555
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
SCRIPT_NAME=$(basename "$0")

# --- 日志函数 ---
log_info() { echo -e "${GREEN}✅ $1${NC}"; }
log_warn() { echo -e "${YELLOW}ℹ️ $1${NC}"; }
log_error() { echo -e "${RED}❌ $1${NC}"; }

# --- 异常处理 ---
cleanup() {
    log_warn "脚本退出，正在尝试恢复设备到 USB 模式..."
    # 查找所有通过 tcpip 启动的设备并切换回 usb 模式
    adb devices -l | grep "model:" | awk '{print $1}' | while read -r device_id; do
        if adb -s "$device_id" shell getprop service.adb.tcp.port | grep -qE '[0-9]+'; then
            log_warn "正在恢复设备 $device_id ..."
            adb -s "$device_id" usb > /dev/null 2>&1 || true
        fi
    done
    log_info "清理完成。"
}

# 设置 trap，捕获 EXIT, INT, TERM 信号并执行 cleanup 函数
# EXIT 会在任何退出时触发，我们将在正常流程中手动移除它
trap cleanup EXIT INT TERM

# --- 函数定义 ---

show_help() {
    echo -e "${YELLOW}📱 智能 Android Wi-Fi ADB 助手 (v2.1)${NC}"
    echo "用法: $SCRIPT_NAME [command] [-p port]"
    echo ""
    echo "Commands:"
    echo "  connect       (默认) 智能连接到设备。需要先通过 USB 连接一次。"
    echo "  disconnect    断开所有通过 Wi-Fi 连接的设备。"
    echo "  -h, --help    显示此帮助信息。"
    echo ""
    echo "Options:"
    echo "  -p, --port    指定一个自定义端口 (默认: $DEFAULT_PORT)。"
    echo ""
    # 【修复点】在正常显示帮助并退出前，禁用 cleanup
    trap - EXIT
    exit 0
}

do_connect() {
    local port="$1"
    log_warn "--- 正在执行智能连接 (端口: $port) ---"

    USB_DEVICE=$(adb devices | awk 'NR>1 && /device$/ {print $1; exit}')
    [ -z "$USB_DEVICE" ] && { log_error "没有检测到通过 USB 连接的设备。请先连接手机。"; exit 1; }
    log_info "检测到 USB 设备: $USB_DEVICE"

    log_warn "正在获取设备 IP 地址..."
    DEVICE_IP=$(adb -s "$USB_DEVICE" shell 'ip -f inet addr show wlan0 2>/dev/null' | awk '/inet /{print $2}' | cut -d/ -f1)
    [ -z "$DEVICE_IP" ] && DEVICE_IP=$(adb -s "$USB_DEVICE" shell 'getprop dhcp.wlan0.ipaddress' 2>/dev/null | tr -d '\r')
    [ -z "$DEVICE_IP" ] && { log_error "无法获取设备的 IP 地址。请确保手机已连接到 Wi-Fi。"; exit 1; }
    log_info "获取到目标设备 IP: $DEVICE_IP"

    if adb devices | grep -q "$DEVICE_IP:$port"; then
        log_info "设备已经通过 Wi-Fi 连接 ($DEVICE_IP:$port)。"
        trap - EXIT
        exit 0
    fi

    log_warn "正在启动 TCP/IP 模式..."
    adb -s "$USB_DEVICE" tcpip "$port" > /dev/null
    sleep 2

    log_warn "正在尝试连接到 $DEVICE_IP:$port (最多重试3次)..."
    for i in {1..3}; do
        if nc -z "$DEVICE_IP" "$port" 2>/dev/null; then
            if adb connect "$DEVICE_IP:$port" | grep -q "connected"; then
                log_info "🎉 连接成功！现在可以拔掉 USB 数据线了。"
                # 【修复点】在成功连接并准备正常退出前，禁用 cleanup
                trap - EXIT
                exit 0
            fi
        fi
        log_warn "第 $i 次尝试失败，2秒后重试..."
        sleep 2
    done

    log_error "连接失败。请检查网络或重试。"
    # 脚本将在这里因错误退出，此时 trap 依然有效，会触发 cleanup
    exit 1
}

do_disconnect() {
    log_warn "--- 正在断开所有 Wi-Fi 连接 ---"
    if adb disconnect > /dev/null 2>&1; then
        log_info "所有 Wi-Fi 设备已断开连接。"
    else
        log_warn "执行 'adb disconnect' 失败或没有设备可断开。"
    fi
    # 【修复点】在正常断开并准备退出前，禁用 cleanup
    trap - EXIT
    exit 0
}

# --- 主逻辑 ---

PORT=$DEFAULT_PORT
COMMAND="connect"

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        -p|--port)
        PORT="$2"
        shift
        shift
        ;;
        -h|--help)
        show_help
        ;;
        connect|disconnect)
        COMMAND="$key"
        shift
        ;;
        *)
        log_error "未知的选项或命令: $1"
        show_help
        ;;
    esac
done

# 执行命令
case "$COMMAND" in
    connect)
        do_connect "$PORT"
        ;;
    disconnect)
        do_disconnect
        ;;
esac

# 正常执行到脚本末尾（只可能在 disconnect 之后），也禁用 cleanup
trap - EXIT