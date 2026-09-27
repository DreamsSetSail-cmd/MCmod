# 发布到 GitHub

本地仓库已经初始化并完成首次提交，但**推送需要你的 GitHub 凭据**——
本机没有可用的登录态（`gh` CLI 未安装，也没有配置 remote）。
下面按顺序执行即可完成发布。

当前状态：

| 项 | 值 |
| --- | --- |
| 分支 | `main` |
| 首次提交 | `4142229` |
| 跟踪文件 | 157 个 |
| 提交作者 | `DreamColin <dreamcolin@users.noreply.github.com>` |
| 发布产物 | `build/libs/echoes-of-oblivion-1.0.0-mc1.20.6-forge.jar` |

---

## 1. 在 GitHub 上创建空仓库

打开 <https://github.com/new>，填写：

- **Repository name**：`echoes-of-oblivion`
- **Visibility**：按需选 Public 或 Private
- ⚠️ **不要**勾选 "Add a README file"、".gitignore"、"license" ——
  本地已经有这些文件，勾选会导致首次推送冲突

创建后记下仓库地址，形如
`https://github.com/<你的用户名>/echoes-of-oblivion.git`

---

## 2. 关联远程并推送

把下面的 `<你的用户名>` 换成实际值，在项目根目录执行：

```bash
git remote add origin https://github.com/<你的用户名>/echoes-of-oblivion.git
git push -u origin main
```

推荐用 **Personal Access Token** 代替密码（GitHub 早已不接受账户密码推送）：

1. 打开 <https://github.com/settings/tokens> → "Generate new token (classic)"
2. 勾选 `repo` 权限，生成后复制
3. 推送时用户名填 GitHub 用户名，密码处粘贴该 token

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

## 6. 提交身份（可选）

首次提交用的是临时身份 `DreamColin <dreamcolin@users.noreply.github.com>`。
若希望之后的提交带上你自己的邮箱（这样 GitHub 才会把它们算到你账下）：

```bash
git config user.name "DreamColin"
git config user.email "<你的 GitHub 邮箱>"
```

---

## 推送前自查清单

- [ ] `git status` 显示工作区干净
- [ ] `run/`、`run-data/`、`build/`、`.gradle/`、`.claude/` 均未被跟踪
      （`run/` 含约 252 MB 开发产物与下载的工具，绝不能进仓库）
- [ ] 根目录有 `LICENSE`（MIT），且 `mods.toml` 里 `license="MIT"`
- [ ] `python tools/check_lang.py` 输出「全部 21 个语言文件通过校验」
- [ ] `./gradlew build` 成功，且 `build/libs/` 里的 jar **包含 55 个 class**
      （可用 `unzip -l` 或压缩软件确认；曾出现过 jar 缺 class 的静默故障）
