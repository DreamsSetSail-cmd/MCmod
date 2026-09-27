#!/usr/bin/env python3
"""生成 gameTest 需要的空结构模板（NBT，gzip 压缩）。

为什么需要它
------------
``@GameTest(template = "echoesofoblivion:empty")`` 会让测试框架去数据包里找
一个**结构 NBT 文件**。原版并没有名为 ``minecraft:empty`` 的结构，直接引用它会在
启动测试时抛::

    IllegalStateException: Missing test structure: minecraft:empty

本脚本用**纯 Python 标准库**写出该结构文件（NBT 是 Mojang 的公开二进制格式，
gzip 容器；Minecraft 侧由 ``NbtIo.readCompressed`` 读取，会自动解 gzip）。

用法::

    python tools/make_empty_structure.py <输出目录>

输出::

    <输出目录>/empty.nbt
"""

from __future__ import annotations

import gzip
import struct
import sys
from pathlib import Path

# NBT 标签类型
TAG_END = 0
TAG_INT = 3
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10

# 与 Minecraft 1.20.6 一致，避免结构被数据修复器判为过期
DATA_VERSION = 3953

# 结构尺寸：5x5x5 的空区域，足够放下测试实体与方块
SIZE = 5


def _name(text: str) -> bytes:
    """NBT 字符串：2 字节大端长度 + UTF-8 内容。"""
    raw = text.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _int(value: int) -> bytes:
    return struct.pack(">i", value)


def _int_list(values: list[int], name: str) -> bytes:
    """TAG_List，元素类型为 TAG_Int。"""
    out = bytes([TAG_LIST]) + _name(name)
    out += struct.pack(">b", TAG_INT)  # 元素类型
    out += struct.pack(">i", len(values))
    for v in values:
        out += _int(v)
    return out


def _empty_compound_list(name: str) -> bytes:
    """TAG_List，元素类型为 TAG_Compound，长度为 0。"""
    out = bytes([TAG_LIST]) + _name(name)
    out += struct.pack(">b", TAG_COMPOUND)
    out += struct.pack(">i", 0)
    return out


def _compound(name: str, payload: bytes, tag_type: int = TAG_COMPOUND) -> bytes:
    return bytes([tag_type]) + _name(name) + payload


def build_structure_nbt() -> bytes:
    """构造一个合法的空结构模板。"""
    # palette: 单个空气条目。索引 0 必须是 air 或结构方块
    air = _compound("", _name("") + _name("minecraft:air") + _name(""))  # 占位，下面重建
    air = bytes([TAG_COMPOUND]) + _name("") + _name("Name") + _name("minecraft:air")

    palette = bytes([TAG_LIST]) + _name("palette")
    palette += struct.pack(">b", TAG_COMPOUND)
    palette += struct.pack(">i", 1)
    palette += air

    body = b""
    body += _int(DATA_VERSION)
    body += _int_list([SIZE, SIZE, SIZE], "size")
    body += palette
    body += _empty_compound_list("blocks")
    body += _empty_compound_list("entities")

    # 根标签：TAG_Compound + 空名字
    root = bytes([TAG_COMPOUND]) + _name("") + body + bytes([TAG_END])
    return root


def main() -> int:
    out_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(".")
    out_dir.mkdir(parents=True, exist_ok=True)
    target = out_dir / "empty.nbt"

    raw = build_structure_nbt()
    with gzip.open(target, "wb", compresslevel=9) as fh:
        fh.write(raw)

    print(f"已生成 {target}  ({target.stat().st_size} 字节, 未压缩 {len(raw)} 字节)")
    print(f"DataVersion={DATA_VERSION}  size={SIZE}x{SIZE}x{SIZE}  palette=[minecraft:air]")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
