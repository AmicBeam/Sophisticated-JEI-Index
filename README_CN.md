# Sophisticated JEI Index

**其他语言版本： [English](README.md)**

为 Sophisticated Backpacks 添加一个 JEI 索引升级（JEI Index Upgrade）。安装后，JEI 的配方转移会在考虑玩家物品栏的同时，额外从“按顺位匹配到的所有启用该升级的背包”中取材料。

## 功能

- ✅ 兼容 JEI 原生配方转移场景
- ✅ 材料来源扩展到顺位中所有启用该升级的背包
- ✅ 同时支持以下 Mod 的合成终端（优先级：网络取材 → 玩家物品栏 → 按顺位遍历启用该升级的背包）：AE2、Refined Storage
- ✅ 支持 Shift 最大转移与 JEI 的完整套数/回滚语义
- ✅ 兼容 EMI 的配方转移与配方树快捷合成（多人需要服务端也安装 EMI）

## 依赖

本项目按 MC/加载器版本拆分分支维护：

- **Forge（Minecraft 1.20.1）**：使用分支 `forge-1.20.1`
  - Minecraft 1.20.1
  - Forge 47.x
  - Sophisticated Core 1.20.1-1.3.6+（Sophisticated Backpacks 的前置）
  - Sophisticated Backpacks 3.24+
  - JEI 15.x（Just Enough Items，Forge）
  - EMI（可选）
  - Curios（可选：仅在需要使用 Curios 饰品栏放背包时需要）
- **NeoForge（Minecraft 1.21.x）**：使用分支 `neoforge-1.21.1`
  - Minecraft 1.21.x
  - NeoForge 21.1+
  - Sophisticated Core 1.21.1+（Sophisticated Backpacks 的前置）
  - Sophisticated Backpacks 1.21.1+
  - JEI 19.x（Just Enough Items，NeoForge）
  - EMI（可选）
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
- EMI 配方转移需要向服务端发送请求。多人游戏中为了完整功能，服务端也需要安装 EMI。
- 背包的判定顺序与 Sophisticated Backpacks 的 B 键逻辑一致，仅筛选启用该升级的背包。
- 配置项：`maxEnabledBackpacksScanned`（common 配置），用于限制同一个玩家最多检索多少个启用该升级的背包，0 表示不限制。
- 不支持将嵌套升级（如 Inception Upgrade）打开的内层背包作为材料来源。
- 与其它 Mod 的联动会在检测到对应 Mod（且版本兼容）时才启用。
- Tom's Storage 与 Beyond Dimensions 暂未兼容，欢迎提交 PR。

## 许可证

MIT，见 [LICENSE](LICENSE)。
