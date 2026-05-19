# DrinkWater

> 你的私人自律管家 — 开源离线 Android 应用提醒工具

## 关于

一名大一学生的个人项目。初衷是自己手机瘾太重，想做一个"管家"式的工具来监督自己。代码写得青涩，但功能实打实地能用。欢迎各位大佬提 Issue、提 PR，一起完善这个项目！

**如果你也是学生，或者正在学习 Android 开发，欢迎一起来折腾！**

## 功能特性

- **应用拦截提醒** — 打开指定应用时弹出提醒，帮你控制手机使用习惯
- **多条随机提醒** — 每个应用可绑定多条提醒文案，随机显示防止免疫
- **全屏 / 悬浮窗** — 两种弹窗模式自由切换
- **延迟提醒** — 支持自定义延迟时长，人性化不强制
- **定时提醒** — 设定时间触发通知提醒
- **生词本** — 导入 Excel/CSV/TXT 词表，支持搜索浏览
- **背词提醒** — 打开指定应用时展示当日待背单词
- **一键导入** — 从备忘录 / 剪贴板批量导入提醒内容
- **本地关键词匹配** — 自动识别高优先级提醒
- **7天趋势统计** — 可视化你的自律变化

## 隐私

**本应用完全离线运行，不联网，不收集任何数据。** 所有数据仅存储在您的设备本地。

## 技术栈

- Kotlin + Jetpack Compose
- Room 本地数据库
- AccessibilityService 应用检测
- WorkManager 定时任务
- DataStore 偏好设置
- Apache POI (Excel 读取)

## 权限说明

| 权限 | 用途 |
|------|------|
| 无障碍服务 | 检测应用启动 |
| 通知权限 | 定时提醒 |
| 悬浮窗权限 | 悬浮窗模式（可选） |
| 文件读取 | 导入词表文件 |

## 构建

```bash
./gradlew assembleDebug
```

## 参与贡献

1. Fork 本仓库
2. 创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的改动 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

有任何想法或建议，欢迎提 Issue！

## License

[MIT](LICENSE)
