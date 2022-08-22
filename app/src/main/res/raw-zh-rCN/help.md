# 📇

## 用户手册

本应用可以更改设备的电源状态。

有以下几种操作模式：
* **受限模式**：在无root权限环境下，功能受限。
* **特权模式**：在root权限下，可以使用所有功能。
* **~~强制模式~~**：在root权限下，对于部分项目，强制切断电源。（仅作为兼容早期非标准系统的备用选项，因为有安全风险所以大多数情况下**不推荐使用**）

为了规避常规魔法上固有的缺陷和限制条件，从Android 6.0开始，**强烈推荐**将本应用与[**Shizuku Manager**](https://shizuku.rikka.app/zh-hans/download.html)搭配使用！可以授权本应用的Shizuku权限以享受更佳的操作体验。

![`Shizuku的图标载入中...`](https://shizuku.rikka.app/logo.png)

在root权限下，使用*切换模式*按钮可以在特权模式和强制模式间相互切换。有对应强制模式的项目会**变色加粗**。

受限模式可能需要**常驻**的**无障碍服务**，所以启用后会留有通知以保持前台状态。用户可以通过手动屏蔽通知，在非原生系统中，用户应该尽可能使用其自带机制~~如白名单，最近任务卡片锁定，电池优化等~~保留后台。（可参考[Don't kill my app!](https://dontkillmyapp.com)）

**长按**项目可以创建**启动器快捷方式**。（非原生系统可能需要事先手动授予添加桌面图标权限）从Android7.0开始，为下拉任务栏的快速设置面板提供*电源菜单*和*锁屏*图块。（从CyanogenMod 12.1开始至Android版本7.0之前提供*电源菜单*瓷块）

为防止误操作，在特权模式中，除*锁屏*和*系统电源菜单*外，其他项目均需要再次确认。

在无Shizuku支持的情况下，受限模式和特权模式的锁屏的区别是：受限模式的在Android 9.0下不能用生物传感器（例如指纹）解锁，从9.0开始需要常驻后台（无障碍服务）；特权模式无此限制，但是可能会稍有延迟。

## ⚠ 意外情况

* 无障碍服务

在Android 11下，无障碍服务有着将导致其无法运行的缺陷。

从Android 13开始，无障碍服务归属于“受限制的设置”。用户需要允许受限制的设置才可以使用它，多了一个麻烦的步骤。

在受限模式下，请尽可能使用Shizuku来代替无障碍服务。

* 启动器快捷方式

在部分环境中，应用更新之后，启动器快捷方式可能会失效。删除并重新添加即可。

## 作者
[@ryuunoakaihitomi github.com](https://github.com/ryuunoakaihitomi)

## 隐私声明
本应用开源，代码可供任何人查看。开发者承诺不会使应用做出任何有损用户隐私权等利益的行为。

使用Firebase与AppCenter的相关组件进行崩溃报告收集和使用信息统计，这些信息有益于开发者改进本应用。详情参阅：[Firebase 中的隐私权和安全性](https://firebase.google.cn/support/privacy) [Visual Studio App Center Security and Compliance](https://docs.microsoft.com/en-us/appcenter/general/app-center-security)