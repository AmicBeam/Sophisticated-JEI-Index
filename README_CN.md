# Sophisticated JEI Index

**其他语言版本： [English](README.md)**

为 Sophisticated Backpacks 添加一个 JEI 索引升级（JEI Index Upgrade）。安装后，JEI 的配方转移会在考虑玩家物品栏的同时，额外从“按顺位匹配到的所有启用该升级的背包”中取材料。

## 功能

- ✅ 兼容 JEI 原生配方转移场景
- ✅ 材料来源扩展到顺位中所有启用该升级的背包
- ✅ 同时支持以下 Mod 的合成终端（优先级：网络取材 → 玩家物品栏 → 按顺位遍历启用该升级的背包）：AE2、Refined Storage、Tom's Storage
- ✅ 支持 Shift 最大转移与 JEI 的完整套数/回滚语义
- ✅ 在存在匹配 EMI 构建的分支兼容 EMI 配方转移与配方树快捷合成（多人需要服务端也安装 EMI）

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
- **NeoForge（Minecraft 26.1.2）**：使用分支 `neoforge-26.1.2`
  - Minecraft 26.1.2
  - NeoForge 26.1.2.71+
  - Java 25
  - Sophisticated Core 26.1.2-1.4.76+
  - Sophisticated Backpacks 26.1.2-3.25.76+
  - JEI 29.6.x（Just Enough Items，NeoForge）
  - AE2 26.1.x、Refined Storage 3.2.x、Beyond Dimensions 0.7.24+ 和 Tom's Storage 26.1 为可选联动
  - EMI 尚未发布兼容 26.1.2 的 NeoForge 构建，本分支暂不默认打包 EMI 联动

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
- EMI 配方转移需要向服务端发送请求。多人游戏中为了完整功能，服务端也需要安装 EMI。26.1.2 分支会在 EMI 发布兼容构建后再打包该联动。
- 背包的判定顺序与 Sophisticated Backpacks 的 B 键逻辑一致，仅筛选启用该升级的背包。
- 配置项：`maxEnabledBackpacksScanned`（common 配置），用于限制同一个玩家最多检索多少个启用该升级的背包，0 表示不限制。
- 支持 Sophisticated Backpacks 的 Inception Upgrade 暴露出的内层背包材料，前提是外层已启用 JEI 索引升级且 Sophisticated Backpacks 配置允许外层升级访问内层背包。
- 配置项：`backpackSlotIdStride`（common 配置），用于控制每个索引背包来源预留的虚拟槽位 ID 空间。若某个 Inception 组合暴露的槽位数超过默认值，可调大该值；多人游戏中客户端和服务端必须保持一致。
- 配置项：`enableTransferWithoutUpgrade`（common 配置）。开启后，所有已装备背包即使没有安装该升级也可作为配方转移材料来源；升级物品会从创造模式标签页和 JEI 物品列表中隐藏，但仍保持注册，已有仓储中的物品不会丢失。
- 与其它 Mod 的联动会在检测到对应 Mod（且版本兼容）时才启用。
- 26.1.2 分支默认不打包 EMI 联动，因为当前尚无兼容 26.1.2 的 EMI NeoForge 构建。
- Tom's Storage 仅支持 JEI 配方转移，EMI 不支持。

## 许可证

MIT，见 [LICENSE](LICENSE)。
