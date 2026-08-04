# 火珠林六爻排盘 App

基于京房纳甲筮法（火珠林）的 Android 六爻排盘应用，使用 **Kotlin + Jetpack Compose + Room** 开发。

## 功能
- **三种起卦方式**
  - 铜钱摇卦：三枚铜钱随机（老阳/少阴/少阳/老阴，自动判动爻）
  - 手动点爻：逐爻指定阴阳与动静
  - 时间 / 数字起卦：按数推演上下卦与动爻（梅花易数式简化）
- **完整装卦排盘**
  - 纳甲（天干地支）、安六亲、安六神、定世应、标动爻、生成变卦
  - 可选日干支（定六神 / 旬空）、月建（定旺相休囚死）
- **经文参考**：内置《周易》六十四卦卦辞 / 爻辞（王弼本）
- **历史记录**：Room 本地保存每次排盘，可回看与删除

## 目录结构
```
app/src/main/java/com/example/huozhulin/
├── data/
│   ├── model/        # 五行/干支/六亲/六神/八卦/六十四卦/排盘结构
│   ├── Yijing.kt     # 周易卦辞爻辞
│   └── local/        # Room 实体/DAO/数据库
├── engine/           # 起卦引擎、装卦排盘引擎
├── ui/               # 主题与各页面、爻图组件
├── viewmodel/        # PaiPanViewModel
└── MainActivity.kt   # 导航宿主
```

## 在 Android Studio 中打开与运行
1. 用 **Android Studio (Hedgehog / Iguana 及以上)** 打开本目录（`File → Open` 选择 `HuozhulinLiuyao`）。
2. 首次打开会触发 Gradle 同步：
   - 若提示缺少 Gradle wrapper，选择「使用 Android Studio 自带的 Gradle」或本地已安装的 Gradle 同步即可；
   - 也可在已安装 Gradle 的终端执行 `gradle wrapper` 生成 `gradle-wrapper.jar` 后再同步。
3. 连接安卓设备或启动模拟器（minSdk 24，Android 7.0+）。
4. 点击 **Run ▶** 或 `Shift+F10` 安装运行。

## 构建 APK
- 调试包：`Build → Build Bundle(s) / APK(s) → Build APK(s)`
- 或命令行：`./gradlew assembleDebug`

## 说明
- 排盘结果中的五行旺衰/旬空依赖所选「日干支 / 月建」，可在排盘结果页调整；不填月建则不显示旺衰。
- 时间起卦采用简化算法（以 年月日时 构造数字），与严格农历节气起卦略有差异，仅供娱乐参考。
- 本应用为传统文化工具，结果仅供参考，不构成任何决策建议。
