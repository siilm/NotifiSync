# Notification Forwarder for Android

![App Icon](https://github.com/siilm/NotifiSync/blob/main/icon.png?raw=true)

一个轻量级Android应用，用于将设备通知实时转发至[Bark](https://github.com/Finb/Bark)服务器，实现跨设备通知同步。
目前只对接Bark，功能专一且轻量化。

## 主要功能

- 实时监听Android设备通知
- 自动转发至配置的Bark服务器

## TODO LIST

- [ ] 支持更多通知推送服务（考虑Gotify或其它）
- [ ] 实现消息收端，真正完成"Sync"

### 问题改进

- [ ] 前台服务突然反复重启
  - 原因不明。
  - 解决方案: 请前往设置->"跳转到权限授予界面"->手动取消App的读取通知权限即可强行停止该服务。停止后可再次赋予读取权限，前台服务会自动重启。
  
- [ ] 应用访问控制界面白屏
  - 初次启动时没有做好回调导致的。
  - 解决方案: 重进一下。

## 安装

1. 下载最新APK [从Release页面](https://github.com/siilm/NotifiSync/releases)
2. 在Android设备上安装
3. 授予必要的通知访问权限

## ⚙配置使用

1. 进入首页并授予相关权限后，点击右下角的“创建新连接”按钮。
2. 输入该连接的每次、你的Bark服务器URL（如 `https://api.day.app/yourkey`）、以及加密相关选项
3. 前往设置页面配置黑名单/白名单（必须）
4. 开启通知监听服务（授予读取通知权限后自动启动）

欢迎提交Pull Request或Issue！

---

**联系作者**: dea405352@gmail.com
**项目主页**: https://github.com/siilm/NotifiSync
