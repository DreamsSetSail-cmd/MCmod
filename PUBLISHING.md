# 发布到 GitHub

本地仓库已初始化、提交作者已改写为你的身份、`origin` 已配好。
**只剩推送这一步**——它需要浏览器登录 GitHub，由你执行最稳妥。

当前状态：

| 项 | 值 |
| --- | --- |
| 远程 | `origin` → <https://github.com/DreamsSetSail-cmd/MCmod.git> |
| 分支 | `main` |
| 提交 | `63c9a65`（初始）、`0e7fa73`（发布指南） |
| 跟踪文件 | 158 个 |
| 提交身份 | `DreamsSetSail-cmd <colinwangqihang@outlook.com>` |
| 发布产物 | `build/libs/echoes-of-oblivion-1.0.0-mc1.20.6-forge.jar` |

---

## 1. 推送

远程仓库已存在，直接推：

```powershell
cd "J:\mc_mod_dev\forge\1.20.6\Echoes-of-Oblivion"
git push -u origin main
```

**推送会触发 Git Credential Manager 弹出浏览器要求登录 GitHub**
（本机凭据存储里目前没有 GitHub 条目）。登录成功后凭据会存入
Windows 凭据管理器，之后推送无需重复登录。

若用的是便携版 git 而没弹出登录窗口，改用完整版 Git for Windows：

```powershell
$env:Path = "C:\Program Files\Git\cmd;" + $env:Path
git push -u origin main
```

若仓库不是空的（例如你建仓时勾选了 "Add a README file"），
先合并再推：

```bash
git pull --rebase origin main
git push -u origin main
```

### 备选：用 Personal Access Token

不想走浏览器登录的话（GitHub 早已不接受账户密码推送）：

1. 打开 <https://github.com/settings/tokens> → "Generate new token (classic)"
2. 勾选 `repo` 权限，生成后复制
3. 推送时用户名填 GitHub 用户名，**密码处粘贴该 token**

或者用 SSH（若本机已有 SSH key）：

```bash
git remote add origin git@github.com:<你的用户名>/echoes-of-oblivion.git
git push -u origin main
```

---

## 3. 检查自动化构建

推送后打开仓库的 **Actions** 标签页，会看到一个名为 `Build` 的工作流自动运行
（定义在 `.github/workflows/build.yml`）。它会依次：

1. 安装 JDK 21
2. 运行 `python3 tools/check_lang.py` 校验 21 个语言文件
3. `./gradlew build` 编译打包
4. `./gradlew runGameTestServer` 跑 5 项服务端测试
5. 把 jar 上传为构建产物（Artifacts 里可下载）

> 首次运行较慢（ForgeGradle 要下载并反编译 Minecraft，约 5~10 分钟），
> 之后有缓存会快很多。

---

## 4. 发布正式 Release（可选）

工作流配置了「推送 `v*` 标签时自动创建 Release 并附上 jar」。所以：

```bash
git tag v1.0.0-mc1.20.6-forge
git push origin v1.0.0-mc1.20.6-forge
```

之后 GitHub 会自动：
- 从该 tag 构建
- 创建 Release（附带自动生成的更新说明）
- 把 `echoes-of-oblivion-1.0.0-mc1.20.6-forge.jar` 作为 Release 附件

---

## 5. 仓库设置建议

| 设置 | 位置 | 建议 |
| --- | --- | --- |
| Description | 仓库首页右上 | `Psychological horror exploration mod for Minecraft 1.20.6 / Forge` |
| Topics | 同上 | `minecraft` `minecraft-mod` `forge` `minecraft-forge` `horror` `java` |
| License | 应自动识别为 MIT | 若显示 Unknown，检查根目录 `LICENSE` 文件是否被正确识别 |
| Website | 同上 | 可留空 |

---

## 6. 提交身份（已完成）

提交作者与提交者均已改写为：

```
DreamsSetSail-cmd <colinwangqihang@outlook.com>
```

该邮箱是 GitHub 账号绑定的邮箱，因此这些提交会正确计入你的贡献图。
后续提交也会沿用这个身份（已写入本仓库的 `.git/config`，未污染全局配置）。

两个提交的原始哈希在改写后被替换，因此**不要**再引用旧的哈希
（`4142229` / `5bd6e1f` 等）——若你之前克隆过或在别处引用过，需要重新克隆。

---

## 推送前自查清单

- [ ] `git status` 显示工作区干净
- [ ] `run/`、`run-data/`、`build/`、`.gradle/`、`.claude/` 均未被跟踪
      （`run/` 含约 252 MB 开发产物与下载的工具，绝不能进仓库）
- [ ] 根目录有 `LICENSE`（MIT），且 `mods.toml` 里 `license="MIT"`
- [ ] `python tools/check_lang.py` 输出「全部 21 个语言文件通过校验」
- [ ] `./gradlew build` 成功，且 `build/libs/` 里的 jar **包含 55 个 class**
      （可用 `unzip -l` 或压缩软件确认；曾出现过 jar 缺 class 的静默故障）
