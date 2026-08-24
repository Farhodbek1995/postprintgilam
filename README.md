# Gilam Yuvish POS - Loyiha Tuzilmasi

```
psotprint/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/themes.xml
│       │   └── xml/device_filter.xml
│       └── java/uz/carpet/washer/pos/
│           ├── MainActivity.kt
│           ├── data/
│           │   ├── model/
│           │   │   └── Models.kt           ← Order, Carpet, OrderStatus
│           │   ├── db/
│           │   │   ├── AppDatabase.kt      ← Room singleton
│           │   │   └── Dao.kt             ← OrderDao, CarpetDao
│           │   └── repository/
│           │       └── OrderRepository.kt  ← Barcha DB amallar + Export/Import
│           ├── printer/
│           │   ├── UsbPrinterManager.kt    ← USB Host API, timeout, queue
│           │   └── EscPosHelper.kt         ← ESC/POS chek formati
│           └── ui/
│               ├── theme/
│               │   └── Theme.kt            ← Ranglar va Material3 mavzu
│               ├── navigation/
│               │   └── AppNavigation.kt    ← Barcha ekranlar yo'nalishlari
│               └── screens/
│                   ├── dashboard/
│                   │   ├── DashboardViewModel.kt
│                   │   └── DashboardScreen.kt
│                   ├── neworder/
│                   │   ├── NewOrderViewModel.kt
│                   │   └── NewOrderScreen.kt
│                   ├── orderdetail/
│                   │   ├── OrderDetailViewModel.kt
│                   │   └── OrderDetailScreen.kt
│                   ├── printer/
│                   │   ├── PrinterViewModel.kt
│                   │   └── PrinterSettingsScreen.kt
│                   └── statistics/
│                       ├── StatisticsViewModel.kt
│                       └── StatisticsScreen.kt
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

## Android Studio'da ochish

1. Android Studio'ni oching
2. **File → Open** → `i:\projects\psotprint` papkasini tanlang
3. Gradle sync tugashini kuting
4. Telefoningizni USB Debug rejimida ulang yoki emulator ishga tushiring
5. **Run** bosing

## Muhim eslatmalar

- **Printer Test:** Avval `Printer Sozlamalari` ekraniga kiring → USB qurilmani tanlang → `Test Print` bosing
- **OTG:** Telefonda USB OTG qo'llab-quvvatlanishini tekshiring
- **Android versiyasi:** Minimum Android 8.0 (API 26)
