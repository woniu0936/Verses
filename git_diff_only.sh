#!/bin/bash

# 1. 检查参数
if [ -z "$1" ]; then
    echo "使用方法: ./get_clean_diff.sh <Commit-Hash>"
    exit 1
fi

COMMIT_ID=$1
OUTPUT_FILE="diff_clean_${COMMIT_ID}.txt"

# 2. 检查是否在 git 仓库
git rev-parse --is-inside-work-tree > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ 错误: 当前目录不是 git 仓库"
    exit 1
fi

echo "⏳ 正在提取并清洗代码 (Commit: $COMMIT_ID)..."

# 3. 核心管道命令
# (A) git diff-tree: 获取纯净的代码 Diff，限制为 .java 和 .kt
# (B) sed: 去除行尾的 // 注释 (匹配 "空格+//"，避免误删 "http://")
# (C) grep: 过滤掉常见的注释行 (行注释、块注释开头、Javadoc 星号)
# (D) grep: 过滤掉处理后变为空的 Diff 行

git diff-tree -p --no-commit-id --root "$COMMIT_ID" -- "*.java" "*.kt" | \
    sed 's| //.*||' | \
    grep -v -E '^[+-][[:space:]]*//' | \
    grep -v -E '^[+-][[:space:]]*/\*' | \
    grep -v -E '^[+-][[:space:]]*\*' | \
    grep -v -E '^[+-][[:space:]]*$' > "$OUTPUT_FILE" 2>/dev/null

# 4. 检查结果
if [ $? -eq 0 ] && [ -s "$OUTPUT_FILE" ]; then
    echo "✅ 成功: 已生成纯净代码差异"
    echo "📄 文件名: $OUTPUT_FILE"
    echo "🧹 已过滤: .java/.kt 文件中的行注释、块注释及文档注释"
else
    echo "⚠️ 警告: 未找到有效代码修改 (可能只有非代码文件或只有注释修改)"
    rm -f "$OUTPUT_FILE"
fi