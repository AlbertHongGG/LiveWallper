# LiveWallpaper - 高效能影片動態桌布 Android App (Light Mode)

本專案採用全新現代化架構（Kotlin 2.0+、Jetpack Compose、Media3 ExoPlayer、Preferences DataStore、yt-dlp）打造，100% 移植 `GeminiFlowApp` 與 `YTFlow` 的核心設計系統與互動元件，全面以 **Light Mode (淺色模式)** 為唯一視覺風格。

## 核心設計特色

### 1. 淺色設計系統與成熟導航
- **Light Mode 視覺**：採用柔和灰白畫布 (`#F5F7FA`) 與純白分組卡片 (`#FFFFFF`)，搭配沉穩深色文字與細緻微邊框。
- **CupertinoSwipeBackContainer 手勢導航**：100% 移植 `GeminiFlowApp` 的物理邊緣手勢引擎，所有子頁面（設定頁、配置工作台）皆支援從左側邊緣向右滑動平滑返回，徹底移除生硬的左上角叉叉按鈕。
- **GlobalNotificationOverlay 頂部浮動通知**：取代粗糙的 Toast，所有解析、下載、套用成功與更新提示皆以平滑滑入的頂部卡片呈現。

### 2. 主畫面 (HomeScreen)
- **YTFlow 風格 UrlInputField**：56dp 圓角膠囊輸入框，支援手動輸入或一鍵貼上剪貼簿中的 Pinterest 連結；下載中平滑展開進度卡片（含即時百分比、下載速率與取消下載按鈕）。
- **單一視覺主角 WallpaperStageCard**：
  - 未設定時：中央引導卡片，清晰標示「尚未設定動態桌布」，點擊卡片任何區域皆可直接開啟系統相簿挑選影片。
  - 已設定時：以手機螢幕比例即時循環預覽當前桌布，附帶綠色「系統桌布運行中」狀態標籤、解析度與時長標籤；底部提供「調整桌布設定」與「更換相簿影片」兩個明確操作按鈕。

### 3. 桌布配置工作台 (WallpaperEditorScreen)
- **模擬桌面即時預覽視窗**：9:16 手機比例視窗，模擬鎖定桌面時鐘，即時反應縮放模式、桌面壓暗遮罩與時長裁剪效果。
- **畫面比例與視覺分組**：
  - 顯示模式切換：`SlidingSegmentedControl`（滿版裁切 / 完整顯示 / 延展填滿）。
  - 桌面壓暗遮罩：`0% ~ 60%` 滑桿，保護桌面應用程式圖示的閱讀清晰度。
- **片段剪輯與播放分組**：
  - 片段剪輯開關 (`AppSwitch`)：開啟後展開 `ProTimelineSlider` 雙端珍珠滑桿，精準限制循環起訖時間。
  - 播放速度切換：`0.5x` / `0.75x` / `1.0x` / `1.25x` / `1.5x`。
  - 開啟桌布音效 (`AppSwitch`)：開啟後展開音量滑桿 (`0% ~ 100%`)。
- **無縫套用狀態流**：若本服務已是系統桌布，點擊「套用桌布設定」直接背景熱更新並自動退棧回首頁；若為首次啟用，喚起系統對話框，確認返回後自動同步狀態並退回首頁。

### 4. 獨立系統設定頁 (SettingsPage)
- **PremiumConfigHeader**：包含「系統設定」大標題與右側 **ThinkingOrb 3D 旋轉球體粒子動畫**。
- **分組卡片 (SettingsSection)**：
  - 動態桌布與節能：桌面雙擊暫停開關 (`AppSwitch`)、深度節能釋放延遲時間設定。
  - 儲存與快取管理：顯示暫存快取容量與檔案數，點擊觸發 `SwipeToConfirmDrawer`（滑動清空暫存）。
  - 核心引擎與關於：YT-DLP 核心版本檢視與線上更新機制（整合 `YtDlpMediaExtractor`）、App 版本號。

---

## 專案目錄結構

```text
com.livewallpaper.app/
├── domain/                    # 領域層
│   ├── model/                 # WallpaperConfig, VideoSource, VideoMetadata, ScalingMode, DownloadProgress
│   ├── parser/                # PinterestUrlParser (策略模式)
│   ├── repository/            # WallpaperRepository, MediaExtractor, MediaStorageManager
│   └── usecase/               # ProcessVideoSourceUseCase, ApplyWallpaperUseCase
├── data/                      # 基礎設施與實作層
│   ├── datastore/             # WallpaperPreferencesDataSource
│   ├── extractor/             # YtDlpMediaExtractor (封裝 youtubedl-android)
│   ├── metadata/              # VideoMetadataRetriever
│   ├── storage/               # ScopedMediaStorageManager
│   └── repository/            # WallpaperRepositoryImpl
├── service/                   # 系統動態桌布服務
│   ├── VideoWallpaperService.kt
│   └── engine/                # WallpaperPlayerController (兩階段節能狀態機 + 雙擊暫停手勢)
└── presentation/              # UI 表現層 (Jetpack Compose Light Mode)
    ├── MainActivity.kt        # 根容器 (CupertinoSwipeBackContainer + GlobalNotificationOverlay)
    ├── navigation/            # CupertinoNavigator, CupertinoSwipeBackContainer, AppRoute
    ├── notification/          # NotificationController, NotificationToast, GlobalNotificationOverlay
    ├── components/            # ThinkingOrb, PremiumConfigHeader, AppSwitch, SwipeToConfirmDrawer, GfButton
    ├── features/
    │   ├── home/              # HomeScreen, UrlInputField, WallpaperStageCard
    │   ├── editor/            # WallpaperEditorScreen, SlidingSegmentedControl, ProTimelineSlider
    │   └── settings/          # SettingsPage, SettingsViewModel, SettingsSection, SettingsTile
    └── theme/                 # AppColors, Theme
```
