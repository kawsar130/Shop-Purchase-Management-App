# ShopKeeper 📱🛒

An independent, zero-setup, offline-first Android application designed for retail shop owners and staff to instantly look up purchase and sales prices, check profit margins, manage inventory, and synchronize product data across multiple authorized Google accounts using **Cloud Firestore directly from inside the app**.

---

## 🌟 Key Highlights

1. **100% In-App Automation (Zero Manual Setup)**:
   - Anyone installs the app, signs in with their Google Account, sets their Shop Name & Currency, and taps **"Create Shop Database & Start"**.
   - Ready to use immediately with zero manual configuration.
2. **Instant Customer-Facing Price Lookups (<10ms)**:
   - Uses an offline-first **Room (SQLite)** local cache on Android.
   - When a customer asks for a price, typing the product code or name immediately reveals the sales price, purchase price, and profit margin with zero network delay.
3. **Multi-User Access & In-App Staff Management**:
   - The shop owner adds staff Gmail addresses right inside the app (**Settings** > **Staff & Multi-User Access**).
   - Staff members install the app on their phone, sign in with their Gmail, and the app automatically links to the shop inventory in real time!
4. **Transparent Purchase & Sales Pricing**:
   - Displays both **Sales Price** and **Purchase Price** clearly for logged-in staff members.
   - Automatically computes unit profit and margin percentage in real-time.
5. **Zero Server Costs**:
   - 100% free with no monthly hosting bills or domains on Firebase Spark tier.
   - 1-tap **"Export to Excel / Google Sheets (CSV)"** lets owners generate a full spreadsheet backup anytime.

---

## 📁 Project Architecture

```
ShopKeeper/
├── app/
│   ├── build.gradle.kts          # Dependencies (Jetpack Compose, Room, Firebase Firestore & Auth)
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions & FileProvider
│       ├── java/com/shopkeeper/
│       │   ├── ShopApplication.kt
│       │   ├── data/
│       │   │   ├── export/       # CsvExporter (Export to Excel/Sheets/Drive/WhatsApp)
│       │   │   ├── local/        # Room Database, DAO & ProductEntity (Instant offline cache)
│       │   │   ├── model/        # Product, ShopInfo, StaffMember, SyncStatus
│       │   │   ├── remote/       # FirestoreService (Cloud Firestore real-time sync)
│       │   │   └── repository/   # ProductRepository (offline-first sync) & UserSessionRepository
│       │   └── ui/
│       │       ├── MainActivity.kt             # Navigation: Auth -> Setup -> Products -> Categories / Settings / Staff
│       │       ├── theme/                      # Material 3 colors, typography, theme
│       │       ├── viewmodel/                  # ProductViewModel, AuthViewModel, SettingsViewModel
│       │       └── screens/
│       │           ├── AuthScreen.kt           # One-tap Google Sign-In
│       │           ├── ShopSetupScreen.kt      # In-app shop database creator
│       │           ├── ProductListScreen.kt    # Instant search & price check dashboard
│       │           ├── AddEditProductDialog.kt # Form with live profit calculation preview
│       │           ├── CategoryScreen.kt       # Category grouping & stock valuation breakdown
│       │           ├── SettingsScreen.kt       # Shop info, currency selector & cloud export
│       │           ├── StaffManagementScreen.kt# In-app team member invitations
│       │           └── components/
│       │               ├── ProductCard.kt          # Large sales & purchase prices, profit markup
│       │               ├── SearchBarComponent.kt   # High-speed search bar
│       │               ├── CategoryChips.kt        # Dynamic category filter chips
│       │               └── SyncStatusIndicator.kt  # Real-time sync pill
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 How to Run and Use the App

### Step 1: Open in Android Studio
1. Open **Android Studio**.
2. Select **Open** and select: `/Users/kawsar/Desktop/projects/personal/Android/shop-purchase-management-app`.
3. Let Gradle sync and press **Run** on your connected Android device or emulator.

---

### Step 2: First-Time Onboarding (Owner)
1. Launch the app and tap **"Sign in with Google"**.
2. Enter your **Shop Name** (e.g. *Al-Madina Store*) and choose your **Currency** (e.g. `$`, `৳`, `₹`, etc.).
3. Tap **"Create Shop Database & Start"**.
4. The app automatically provisions your dedicated Shop Database in Cloud Firestore and syncs seamlessly!

---

### Step 3: Daily Shop Operations
- **Look Up Prices**: Type the Product Code (e.g. `P-101`) or Product Name in the search bar. The card immediately displays:
  - **Sales Price** (large, bold, customer-facing)
  - **Purchase Price** (cost price for staff)
  - **Profit Margin** (e.g. `+$60.00 (+16.7%)`)
  - **In-Stock Quantity** (with low-stock warning if <= 5 units)
- **Add / Edit Products**: Tap the `+` floating action button, fill in the details, and watch the profit margin compute live as you type the purchase and sales prices.
- **Category Breakdown**: Tap the Category icon to view inventory grouped by category with total purchase and sales valuations.

---

### Step 4: Adding Staff Members (Multi-User)
1. Go to **Settings** (gear icon) > **Manage Staff Members**.
2. Type the staff member's Gmail address (e.g. `employee@gmail.com`) and tap **"Add Staff to Shop"**.
3. When the staff member installs the app on their phone and signs in with their Gmail, the app **automatically detects the shop inventory** and connects them in real time!
