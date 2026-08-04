# 火珠林六爻排盘 App

基于京房纳甲筮法（火珠林）的 Android 六爻排盘应用，使用 **Kotlin + Jetpack Compose + Room** 开发。

## 功能
- **七种起卦方式**（`engine/CastEngine.kt`）
  - 随机卦：软件模拟手摇，随机生成 6 爻（老阴/老阳为动爻）
  - 铜钱摇卦：三枚铜钱正反面，完全模拟实物摇卦，自动判动爻
  - 指定卦：手动指定上卦、下卦与动爻（历史卦可补充干支）
  - 数字卦：以字数 / 数字和除 8 定上下卦，总和除 6 定动爻（可加时辰数）
  - 日期卦：按农历 / 阳历日期起卦
  - 时分卦：时数、分钟数除 8 定上下卦，和数除 6 定动爻
  - 终身卦：以生日年干支 + 农历月 + 农历日起卦
- **完整装卦排盘**（`engine/PaiPanEngine.kt`）
  - 纳甲（天干地支）、安六亲、安六神、定世应、标动爻、生成变卦
  - 伏神：补全本宫伏藏之爻（以本宫五行为「我」）
  - 旬空（依日干支）、旺相休囚死（依月建，未填月建则不显示旺衰）
- **经文参考**：内置《周易》六十四卦卦辞 / 爻辞（`data/Yijing.kt`，王弼本）
- **历史记录**：Room 本地保存每次排盘，可回看与删除

## 目录结构
```
app/src/main/java/com/example/huozhulin/
├── data/
│   ├── model/        # 五行/干支/六亲/六神/八卦/六十四卦/排盘结构
│   ├── Yijing.kt     # 周易卦辞爻辞
│   └── local/        # Room 实体/DAO/数据库
├── engine/
│   ├── CastEngine.kt     # 七种起卦引擎
│   ├── PaiPanEngine.kt   # 装卦排盘引擎（纳甲/六亲/六神/世应/动变/伏神）
│   ├── GanZhiCalendar.kt # 公历→年月日时干支
│   ├── LunarCalendar.kt  # 农历/节气换算
│   └── ShenSha.kt        # 旬空等神煞计算
├── ui/
│   ├── screens/      # 起卦(CastScreens)/结果(ResultScreen)/历史(HistoryScreen)
│   ├── components/   # HexagramView、PlateTable 爻图与排盘表格
│   └── theme/        # 主题
├── viewmodel/        # PaiPanViewModel
└── MainActivity.kt   # 导航宿主
```

## 本地逻辑验证
根目录下的 `verify_fix.py`、`verify_gz.py` 为**纯 Python 参考实现**，用于在本机对照校验引擎逻辑（纳甲 / 六亲 / 六神 / 世应、干支推算），不参与 Android 打包。运行：

```bash
python verify_fix.py   # 校验装卦排盘（对照标准火珠林）
python verify_gz.py    # 校验干支（年月日时）推算
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
