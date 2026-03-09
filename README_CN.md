# Sophisticated JEI Index

**其他语言版本： [English](README.md)**

为 Sophisticated Backpacks 添加一个 JEI 索引升级（JEI Index Upgrade）。安装后，JEI 的配方转移会在考虑玩家物品栏的同时，额外从“按顺位找到的第一个启用该升级的背包”中取材料。

## 功能

- ✅ 复用 JEI 原生配方转移逻辑（支持任意 JEI 转移场景）
- ✅ 材料来源扩展到顺位中第一个启用该升级的背包
- ✅ 优先级遵循 Sophisticated Backpacks 的背包扫描顺序（armor/offhand/main，含 Curios 等兼容槽位）
- ✅ 支持 Shift 最大转移与 JEI 的完整套数/回滚语义

## 依赖

- Minecraft 1.20.1
- Forge 47.x
- Sophisticated Core（Sophisticated Backpacks 的前置）
- Sophisticated Backpacks
- JEI（Just Enough Items）
- Curios（可选：仅在需要使用 Curios 饰品栏放背包时需要）

## 安装

1. 将本 Mod 放入 `mods` 文件夹
2. 安装并确认依赖齐全
3. 启动游戏

## 使用方法

1. 将 Sophisticated Backpack 装备到 Sophisticated Backpacks 支持的任意位置（armor/offhand/main 或兼容槽位）
2. 在该背包中放入 JEI 索引升级（JEI Index Upgrade）
3. 打开任意支持 JEI 配方转移的合成/加工界面
4. 在 JEI 中点击 `+` 按钮进行配方转移

## 说明

- JEI 配方转移需要向服务端发送请求。多人游戏中为了完整功能，服务端也需要安装 JEI。
- “第一个背包”的判定顺序与 Sophisticated Backpacks 的 B 键逻辑一致，仅筛选启用该升级的背包。

## 许可证

MIT，见 [LICENSE](LICENSE)。
