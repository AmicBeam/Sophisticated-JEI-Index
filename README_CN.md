# Sophisticated JEI Index

**其他语言版本： [English](README.md)**

为 Sophisticated Backpacks 添加一个 JEI 索引升级（JEI Index Upgrade）。安装后，JEI 的配方转移会在考虑玩家物品栏的同时，额外从“按顺位匹配到的所有启用该升级的背包”中取材料。

本仓库现在集中在一条 `main` 分支上。每个 Minecraft/加载器目标都是 `versions/` 下的独立 Gradle 项目。仓库根目录没有默认版本的构建入口。

## 版本矩阵

| 目录 | Minecraft | 加载器 | JDK | 来源提交 | 嵌套背包 | 默认打包 EMI |
| --- | --- | --- | --- | --- | --- | --- |
| [`versions/1.20.1`](versions/1.20.1) | 1.20.1 | Forge 47.x | 17 | `b5653c3`（`forge-1.20.1`） | 否 | 是 |
| [`versions/1.21.1`](versions/1.21.1) | 1.21.1 | NeoForge 21.1+ | 21 | `c302040`（`neoforge-1.21.1`） | 否 | 是 |
| [`versions/26.1.2`](versions/26.1.2) | 26.1.2 | NeoForge 26.1.2.71+ | 25 | `4d246dc`（`neoforge-26.1.2`） | 是（Inception，需保留） | 否（`include_emi_compat=false`） |

所有版本都有的配置：

- `maxEnabledBackpacksScanned`（common）：限制同一个玩家最多检索多少个启用该升级的背包，`0` 表示不限制。
- `enableTransferWithoutUpgrade`（common）：开启后，所有已装备背包即使没有安装该升级也可作为配方转移材料来源。升级物品会从创造模式标签页和 JEI 物品列表中隐藏，但仍保持注册，已有仓储中的物品不会丢失。

仅 `26.1.2`：

- 支持 Sophisticated Backpacks 的 Inception Upgrade 暴露出的内层背包材料，前提是外层已启用 JEI 索引升级且 Sophisticated Backpacks 配置允许外层升级访问内层背包。
- `backpackSlotIdStride`（common）：用于控制每个索引背包来源预留的虚拟槽位 ID 空间。若某个 Inception 组合暴露的槽位数超过默认值，可调大该值；多人游戏中客户端和服务端必须保持一致。

精确来源提交和已知版本差异见 [docs/multiversion-migration.md](docs/multiversion-migration.md)。这次布局迁移没有改动 gameplay / JEI 行为。

## 功能

- 兼容 JEI 原生配方转移场景
- 材料来源扩展到顺位中所有启用该升级的背包
- 同时支持 AE2、Refined Storage、Tom's Storage、Beyond Dimensions 的合成终端（优先级：网络取材 → 玩家物品栏 → 按顺位遍历启用该升级的背包）
- 支持 Shift 最大转移与 JEI 的完整套数/回滚语义
- 1.20.1 和 1.21.1 打包 EMI 配方转移与配方树快捷合成（多人需要服务端也安装 EMI）。26.1.2 在 EMI 发布兼容构建前保持关闭。

## 依赖

### `versions/1.20.1`

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- Sophisticated Core 1.20.1-1.3.6+（Sophisticated Backpacks 的前置）
- Sophisticated Backpacks 3.24+
- JEI 15.58.0.209+（Just Enough Items，Forge）
- EMI（可选）
- Curios（可选：仅在需要使用 Curios 饰品栏放背包时需要）

### `versions/1.21.1`

- Minecraft 1.21.1
- NeoForge 21.1+
- Java 21
- Sophisticated Core 1.21.1-1.4.89+
- Sophisticated Backpacks 1.21.1-3.25.78+
- JEI 19.53.0.426+（Just Enough Items，NeoForge）
- EMI（可选）
- Curios（可选：仅在需要使用 Curios 饰品栏放背包时需要）

### `versions/26.1.2`

- Minecraft 26.1.2
- NeoForge 26.1.2.71+
- Java 25
- Sophisticated Core 26.1.2-1.4.76+
- Sophisticated Backpacks 26.1.2-3.25.76+
- JEI 29.37.0.98+（Just Enough Items，NeoForge）
- AE2 26.1.x、Refined Storage 3.2.x、Beyond Dimensions 0.7.24+ 和 Tom's Storage 26.1 为可选联动
- EMI 尚未发布兼容 26.1.2 的 NeoForge 构建，本版本暂不默认打包 EMI 联动

## 构建

每个版本目录都是完整的 Gradle 项目，各自带 wrapper、`build.gradle`、`settings.gradle`、`gradle.properties` 和 `src/`。请进入对应目录构建。不要在仓库根目录运行 Gradle；根目录没有项目。

```sh
cd versions/1.20.1 && ./gradlew build
cd versions/1.21.1 && ./gradlew build
cd versions/26.1.2 && ./gradlew build
```

一次性构建全部版本，使用 [scripts/build_all_versions.sh](scripts/build_all_versions.sh)。优先使用的 JDK 环境变量：

| 变量 | JDK | 版本目录 |
| --- | --- | --- |
| `SJI_JAVA_17_HOME` | 17 | `versions/1.20.1` |
| `SJI_JAVA_21_HOME` | 21 | `versions/1.21.1` |
| `SJI_JAVA_25_HOME` | 25 | `versions/26.1.2` |

如果对应的 `SJI_JAVA_*_HOME` 未设置，该版本会回退到 `JAVA_HOME`。只有当 `JAVA_HOME` 已经是该版本所需的主版本时，这个回退才有效。若要在同一个 shell 中构建全部版本，请同时设置三个 `SJI_JAVA_*_HOME`。

```sh
SJI_JAVA_17_HOME=/path/to/jdk-17 \
SJI_JAVA_21_HOME=/path/to/jdk-21 \
SJI_JAVA_25_HOME=/path/to/jdk-25 \
./scripts/build_all_versions.sh
```

只构建一个版本：

```sh
SJI_JAVA_21_HOME=/path/to/jdk-21 ./scripts/build_all_versions.sh 1.21.1
```

脚本会在调用该版本的 Gradle wrapper 之前检查 `bin/java` 以及报告的主版本号。

## 安装

1. 将对应 `versions/<mc>/build/libs` 目录中的 jar 放入 `mods` 文件夹
2. 安装并确认依赖齐全
3. 启动游戏

## 使用方法

1. 将 Sophisticated Backpack 装备到 Sophisticated Backpacks 支持的任意位置（armor/offhand/main 或兼容槽位）
2. 在该背包中放入 JEI 索引升级（JEI Index Upgrade）
3. 打开任意支持 JEI 配方转移的合成/加工界面
4. 在 JEI 中点击 `+` 按钮进行配方转移

## 说明

- JEI 配方转移需要向服务端发送请求。多人游戏中为了完整功能，服务端也需要安装 JEI。
- EMI 配方转移需要向服务端发送请求。多人游戏中为了完整功能，服务端也需要安装 EMI。26.1.2 项目会在 EMI 发布兼容构建后再打包该联动。
- 背包的判定顺序与 Sophisticated Backpacks 的 B 键逻辑一致，仅筛选启用该升级的背包。
- 1.20.1 和 1.21.1 不支持将嵌套升级（如 Inception Upgrade）打开的内层背包作为材料来源。除非后续明确要求移植，否则保持该限制。26.1.2 已经支持嵌套背包，必须保留。
- 与其它 Mod 的联动会在检测到对应 Mod（且版本兼容）时才启用。
- Tom's Storage 仅支持 JEI 配方转移，EMI 不支持。
- 历史分支引用（`forge-1.20.1`、`neoforge-1.21.1`、`neoforge-26.1.2`）仍作为来源快照保留。新工作应在 `main` 的 `versions/` 下进行。

## 许可证

MIT，见 [LICENSE](LICENSE)。
