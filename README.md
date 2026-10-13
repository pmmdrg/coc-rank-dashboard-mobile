# CoC Rank Mobile 📱🏆

Phiên bản ứng dụng di động Native của **CoC Rank Dashboard**, xây dựng bằng **Kotlin**, **Jetpack Compose**, **Material 3** và **Room Database**.

---

## ✨ Tính Năng Nổi Bật

- 📊 **Thống kê & Dự đoán thứ hạng:**
  - Tính thứ hạng thời gian thực, cúp hiện tại và cúp tối đa có thể đạt (giả định toàn thắng và thủ hoàn hảo +40 cúp mỗi trận).
  - Dự đoán rank thấp nhất có thể đạt (trường hợp xấu nhất).
  - Thống kê số đối thủ có tiềm năng vượt qua bạn và số đối thủ chắc chắn xếp sau bạn.
- 🎯 **Phân hạng kỹ năng theo cúp:**
  - 🟣 **Thống trị (Dominant):** $\ge 1200$ cúp
  - 🟢 **Vượt trội (Superior):** $1000 - 1199$ cúp
  - 🔵 **Tiềm năng (Potential):** $800 - 999$ cúp
  - 🔴 **Báo động (Alarm):** $< 800$ cúp
- 🔄 **Đồng bộ Clash of Clans API:**
  - Nhập mã **Player Tag** (`#TAG`) để tự động tải điểm số, giải đấu, bang hội.
  - Hỗ trợ gọi trực tiếp qua Official Supercell API Bearer Token hoặc Proxy.
  - Chế độ dữ liệu mẫu phong phú (Offline/Sample Mode) giúp trải nghiệm ngay lập tức.
- 📈 **Theo dõi biến động thứ hạng (Rank Changes):**
  - Ghi nhận thay đổi thứ hạng (▲ / ▼), số cúp tăng giảm, tiến độ đánh và phòng thủ giữa các lần đồng bộ.
- 🛡️ **Theo dõi Bang hội (Clan):**
  - Xem danh sách thành viên trong clan, chức vụ, Town Hall level, điểm cúp và chỉ số donate.
- 🎨 **Giao diện Gaming Dark Mode:**
  - Thiết kế đậm chất Clash of Clans với bảng màu sắc nét, hỗ trợ Material 3 Expressive UI.
- 💾 **Lưu trữ ngoại tuyến:**
  - Tự động lưu mùa giải và cài đặt bằng **Room Database** & **Jetpack DataStore**.

---

## 🛠️ Yêu Cầu Hệ Thống

- **Android Studio:** Ladybug (2024.2+) hoặc Meerkat / Iguana trở lên
- **JDK:** Java 21 LTS
- **Android SDK:** Compile SDK 35, Min SDK 26 (Android 8.0+)
- **Gradle:** 8.14 (đã kèm Gradle Wrapper)

---

## 🚀 Khởi Động & Chạy Ứng Dụng

### Cách 1: Sử dụng Android Studio (Khuyên dùng)
1. Mở **Android Studio**.
2. Chọn **Open** và duyệt đến thư mục:
   ```
   C:\Personal\CoC\Rank\rank-mobile
   ```
3. Chờ Android Studio đồng bộ Gradle (Gradle Sync).
4. Chọn thiết bị giả lập (Emulator) hoặc cắm điện thoại Android thật đã bật **USB Debugging**.
5. Nhấn nút **Run ▶** (Shift + F10).

### Cách 2: Tự động hóa Build & Ký số bản Release (1 Lệnh duy nhất)
```powershell
# Tự động chạy Unit Tests -> Ký số Release APK -> Xuất ra dist/
.\build-release.ps1
```
File APK Release tối ưu (~11.89 MB) sau khi build nằm tại:
`dist\CoCRank-release-latest.apk`

---

## ⚡ Tự Động Phân Phối Qua Firebase App Distribution

Bạn có thể đẩy bản build lên **Firebase App Distribution** hoàn toàn tự động bằng dòng lệnh:

### 1. Đẩy bản build bằng 1 script duy nhất:
```powershell
# Chạy build, kiểm thử và tự động đẩy lên Firebase:
.\build-release.ps1 -UploadFirebase

# Hoặc nếu đã có sẵn file APK, chỉ cần đẩy lên:
.\distribute-firebase.ps1
```
> *Lần đầu chạy, script sẽ nhắc bạn nhập **Firebase App ID** (lấy từ Firebase Console > Project Settings > General > Your Apps dạng `1:xxxx:android:xxxx`) và lưu vào `firebase.config.json` để bạn không cần nhập lại những lần sau.*

### 2. Hoặc đẩy qua lệnh Gradle trực tiếp:
```powershell
.\gradlew.bat appDistributionUploadRelease -PFIREBASE_APP_ID="<YOUR_APP_ID>"
```

---

## 📂 Cấu Trúc Dự Án

```
app/src/main/java/com/cocrank/dashboard/
├── CoCRankApp.kt                   # Lớp Application
├── MainActivity.kt                 # Single Activity với Jetpack Compose
├── data/
│   ├── api/                        # Retrofit CoC API Service & Network Client
│   ├── local/                      # Room Database, SeasonDao, DataStore Preferences
│   ├── repository/                 # RankRepository (quản lý đồng bộ và lưu trữ)
│   └── RankedTierMetadata.kt       # Quy định số lượt đánh theo từng Tier giải đấu
├── domain/
│   ├── model/                      # Các Data Class (Player, Season, RatingCategory...)
│   └── usecase/                    # RankingCalculator (thuật toán tính toán rank & cúp)
└── ui/
    ├── components/                 # Các UI Widget (StatCard, PlayerItemCard, Badges...)
    ├── navigation/                 # Jetpack Navigation & Bottom Navigation Bar
    ├── screens/
    │   ├── dashboard/              # Màn hình chính Dashboard & ViewModel
    │   ├── changes/                # Màn hình biến động thứ hạng
    │   ├── clan/                   # Màn hình thông tin bang hội
    │   └── settings/               # Màn hình cài đặt & cấu hình API
    └── theme/                      # Bảng màu Gaming Dark Theme & Typography
```
