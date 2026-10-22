# 从核心备份恢复开发

适用：2026-10-22 的 Extreme Winter 26.0.7 核心开发备份。可以在原项目目录已经不存在的情况下恢复；恢复过程只读取备份，不读取旧项目或依赖当前聊天。

## 需要保留

保留 `D:\迅雷下载\Minecraft_Mod_cold_26.0.7_核心开发备份_2026-10-22` 整个小型备份目录和同级的 `Minecraft_Mod_cold_极寒生存_续开发指针_2026-10-22.md`。ZIP 是真实数据，指针是入口说明，不能只保留指针。

备份目录包含 ZIP、`SHA256SUMS.txt`、`Restore-Project.ps1`、`restore_core.py`、进度文档和 `backup-verification.json`。ZIP 内还包含一份恢复脚本、逐文件 SHA-256 清单、完整可达 Git bundle 和项目记忆快照。

## 恢复步骤

1. 准备 Git、Python 3.12 和 JDK 26。当前机器的 Python/JDK 位于 `D:\Program Files\Develop\Python312` 与 `D:\Program Files\Develop\Java\jdk-26.0.1`；这些外部工具不会因删除原项目目录而被删除，也未包含在核心备份内。其他机器应自行安装等效版本，并确认 `git`、`python`、`java`、`javac` 可用。
2. 确认目标目录不存在。脚本拒绝覆盖已有目录；不要为了恢复而覆盖已有的新改动。
3. 在 PowerShell 运行：

```powershell
& 'D:\迅雷下载\Minecraft_Mod_cold_26.0.7_核心开发备份_2026-10-22\Restore-Project.ps1' `
  -Target 'C:\Users\skksjdu\Documents\Codex\projects\Minecraft_Mod_cold'
```

4. 脚本核验整包和每个文件的 SHA-256，用 bundle 重建 Git 历史，将当前文件覆盖层写入新目录，恢复 GitHub remote，并进行 Git 对象检查。它不会推送、安装模组或修改真实游戏存档。项目记忆快照会写入恢复项目的 `docs/project-memory-snapshot`。
5. 在 Codex 中打开恢复目录并读 `docs/PROJECT_STATUS.md` 与记忆快照。若恢复到了其他路径，核验后更新外部项目记忆的根目录绑定，并将新目录设为 Codex 项目入口；不要按旧路径猜测另一个项目。
6. 检查 `java -version` / `javac -version`，配置 JDK 26，再首次联网构建：

```powershell
Set-Location 'C:\Users\skksjdu\Documents\Codex\projects\Minecraft_Mod_cold'
$env:JAVA_HOME = 'D:\Program Files\Develop\Java\jdk-26.0.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\scripts\Build.ps1 -Tasks @('test','build','compileGametestJava')
python tools/audit_package.py
```

`Build.ps1` 将 Gradle 下载和缓存重新建在恢复项目的 `work/gradle-home`。核心包没有依赖缓存，所以第一次构建需要网络。普通源码构建和单元测试不需要第三方兼容模组/光影；如需重跑兼容矩阵，应按 `scripts/Prepare-Compat.ps1`、`scripts/Test-Matrix.ps1` 和 `scripts/Test-Extended.ps1` 检查并重新准备其依赖。

## 固定开发基线

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.1.2 |
| Extreme Winter | 26.0.7 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.155.3+26.1.2 |
| Gradle Wrapper | 9.8.0，发行 SHA-256 已固定 |
| Fabric Loom | 1.18.2 |
| 编译 JDK / 输出 | JDK 26 / Java 25 字节码 |
| Git 基线 | 164dbd45a1fb7ba3d6bc16ac8733cacdb7af1730 |

正式 JAR 位于 `outputs/releases/26.0.7`，SHA-256 应为 `2fc9c705bd4c48e0783a81a4d470fdc14d7c4c5db33b881f60641fb55ee17059`。它可以独立安装，但继续开发仍需要 ZIP 中的源码和构建入口。

若只有 GitHub 可用，也可以克隆 `https://github.com/skksjdu/extreme-winter.git` 获得已推送源码；GitHub 不包含本地忽略的成果和本次新增交接文件，优先使用这份核心备份。

历史截图、日志、测试存档与运行夹具按用户要求没有备份，恢复后不能据报告路径声称原始证据仍然存在；需要时重新生成。历史报告里的绝对原路径仅用于出处定位。
