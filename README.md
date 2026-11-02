# Lịch Bảng (`vn.lcta`)

Bảng lịch treo tường cho điện thoại Android **từ 4.0.3 trở lên**, màn hình **nằm ngang**, tự co giãn theo kích thước máy (3,5" đến 6,7").
Lấy dữ liệu **trực tiếp** từ nguồn, không qua bot/máy chủ trung gian. Cập nhật OTA từ GitHub Release.

## Tính năng

**Cột trái (luôn hiện):** đồng hồ lớn, thứ/ngày, ngày âm lịch, can chi, ngày lễ hoặc đếm ngược, thời tiết hiện tại, dự báo vài giờ tới, dòng trạng thái.

**Các trang tự xoay vòng bên phải** (bật/tắt từng trang trong Cài đặt). Chạm để sang trang, vuốt ngang để tiến/lùi. Sau khi chạm, các trang ngừng xoay 1 phút.

| Trang | Nguồn | Làm mới |
|---|---|---|
| Lịch tháng (âm + dương, ngày lễ, chấm xanh = hoàng đạo, ‹ › đổi tháng) | tính trong máy | – |
| Âm lịch chi tiết (can chi, tiết khí, sao, giờ hoàng đạo, ngày lễ sắp tới, đếm ngược Tết) | tính trong máy | – |
| Dự báo thời tiết (theo giờ + 6 ngày) | Open-Meteo | 15 phút |
| Giá vàng (SJC, nhẫn, DOJI, PNJ, Bảo Tín, thế giới) | vang.today | 10 phút |
| Giá xăng dầu Petrolimex (vùng 1/2) | giaxanghomnay.com | 1 giờ |
| Tỷ giá Vietcombank | portal.vietcombank.com.vn | 30 phút |
| Chứng khoán (chỉ số + mã tự chọn, giá trong phiên) | Entrade/DNSE | 1 phút trong phiên, 30 phút ngoài phiên |
| Tin tức (RSS tự chọn) | RSS | 10 phút |

Chỉ tải dữ liệu khi màn hình đang sáng. Dữ liệu được lưu lại, mất mạng vẫn hiện dữ liệu cũ kèm nhãn "dữ liệu cũ".

**Bố cục tự động:**
- Màn nhỏ (3,5"–4", khung khoảng 480–570 × 320 dp): 2 cột, ẩn bớt dòng phụ khi thiếu chỗ (không thu chữ nhỏ hơn mức đọc được).
- Màn vừa (4,5"–5,5"): 2 cột, hiện đủ can chi, dự báo theo giờ.
- Màn dài (6,1"–6,7", tỉ lệ ~20:9): 3 cột (giờ + âm lịch | lịch tháng cố định | trang xoay vòng).
- Có thể ép 2 cột / 3 cột và chỉnh cỡ chữ trong Cài đặt.

**Màn hình ngày/đêm** (giống app Lan Anh, thêm chế độ):
- Giờ ngày: màn luôn sáng.
- Ban đêm, chọn 1 trong 3: chỉ giảm sáng · tắt theo thời gian chờ của máy khi không chạm · tắt hẳn (cần cấp quyền "Quản trị thiết bị", không cần root).
- Đưa tay qua cảm biến tiệm cận để bật màn (dịch vụ chạy nền có thông báo).
- Tự bật màn lúc bắt đầu giờ ngày, tự mở khi khởi động máy, hiện trên màn khoá.
- Chống lưu ảnh: dịch nội dung vài điểm ảnh mỗi 2 phút.

Mở Cài đặt: **giữ lâu vào đồng hồ** hoặc bấm **···** ở góc trên bên phải.

## Hỗ trợ Android cũ

- `minSdk 15` (Android 4.0.3). Không dùng AndroidX/Compose, giao diện dựng bằng View thường.
- Android < 5.0 không có TLS 1.2 dùng được: app nhúng **Conscrypt 2.5.2** (bản cuối hỗ trợ API 9+) và chỉ bật nó trên máy < 5.0.
- Kho chứng chỉ tin cậy = chứng chỉ của máy + bộ gốc Mozilla nhúng sẵn (`res/raw/cacerts.pem`), nên máy cũ vẫn kết nối được tới Let's Encrypt, Sectigo ECC, GTS…
- Font Inter rút gọn (Latin + tiếng Việt, chữ số đều nhau) để dấu hiển thị đồng nhất; biểu tượng thời tiết tự vẽ (4.0 không có emoji màu).
- APK ký cả v1 và v2; có thư viện cho armeabi-v7a, arm64-v8a, x86, x86_64 (máy mới chỉ chạy 64-bit vẫn cài được).
- Lint chạy với `NewApi` là lỗi: build sẽ hỏng nếu lỡ dùng API mới hơn mà không kiểm tra phiên bản.

## Cập nhật OTA qua GitHub Actions

Mỗi lần đẩy tag `vX.Y.Z`, workflow `.github/workflows/android.yml` sẽ: chạy kiểm thử + lint, build bản release ký bằng keystore của bạn, tạo GitHub Release chứa `LichBang-X.Y.Z.apk` và `update.json`.
App đọc `https://github.com/traique/lananh-apk/releases/latest/download/update.json`, tải APK ngầm, kiểm tra SHA-256 và tên gói rồi:
- Android 4.x–6.x: mở trình cài đặt, **cần người bấm "Cài đặt"** (máy không root thì không cài im lặng được). Lần đầu bật *Cài đặt > Bảo mật > Nguồn không xác định*.
- Android 7+: cài qua PackageInstaller; Android 8+ cần cho phép "Cài ứng dụng không rõ nguồn" cho Lịch Bảng; Android 12+ có thể không cần bấm từ lần cập nhật thứ hai.
- Có tuỳ chọn tự mở màn hình cài vào một giờ cố định trong ngày, và dòng trạng thái "Có bản mới … chạm để cài".
- Cài xong app tự mở lại.

### Thiết lập một lần

1. Tạo keystore (giữ thật kỹ, mất là **không cập nhật được nữa**; dùng RSA để Android 4.x đọc được chữ ký):
   ```
   keytool -genkeypair -v -keystore lichbang.jks -alias lichbang -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 36500
   base64 -w0 lichbang.jks > lichbang.jks.b64
   ```
2. Trong GitHub repo: *Settings > Secrets and variables > Actions*, thêm 4 secret:
   `KEYSTORE_BASE64` (nội dung file `.b64`), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (vd `lichbang`), `KEY_PASSWORD`.
3. Phát hành bản mới (làm được cả trên điện thoại):
   *Actions > Android > Run workflow*, chọn nhánh chứa mã mới, ô **Phiên bản** nhập `1.0.1` (lớn hơn bản trước) rồi bấm *Run workflow*.
   Workflow tự tạo tag `v1.0.1` và Release kèm APK + `update.json`. Cách khác: đẩy tag bằng git (`git tag v1.0.1 && git push origin v1.0.1`).
   `versionCode = X*10000 + Y*100 + Z`, nên mỗi bản sau phải có số phiên bản lớn hơn.
4. Cài bản đầu tiên bằng tay: tải `LichBang-1.0.0.apk` trong mục Releases về máy và mở để cài. Các bản sau tự cập nhật.

Đẩy lên nhánh thường (không phải tag) chỉ build bản debug (`vn.lcta.debug`, cài song song, không tự cập nhật) để thử; tải trong tab *Actions > Artifacts*.

## Build trên máy

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```
Cần JDK 17+ và Android SDK (platform 34). Bản release ký bằng biến môi trường `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; tên phiên bản lấy từ `VERSION_NAME`.
Muốn thử OTA với máy chủ khác (vd. máy ảo gọi `http://10.0.2.2:8000/update.json`), đặt biến `UPDATE_URL` khi build.

## Đã kiểm tra

- Kiểm thử đơn vị: âm lịch (Tết 2020–2027 và 1985, tháng nhuận 2023/2025, Trung thu 2026, đổi qua lại 1990–2060), bộ đọc vàng/xăng/tỷ giá/thời tiết/RSS trên dữ liệu thật.
- Máy ảo **Android 4.1.2 (API 16)**: cài APK ký v1, TLS qua Conscrypt, tải đủ 6 nguồn dữ liệu, font tiếng Việt, xoay trang, vẫy tay qua cảm biến tiệm cận để bật màn, màn Cài đặt, và trọn quy trình OTA (tự tải → chạm dòng trạng thái → trình cài của máy → app tự mở lại ở bản mới).
- Bố cục được dựng thử ở 480×320 dp (3,5"), 640×360 dp (5,5") và 915×411 dp (6,7").
- Chưa thử trên máy thật Android 4.0.3 và máy 6,7" thật (cảm biến tiệm cận, quyền tắt màn hình).

## Cấu trúc mã

```
app/src/main/java/vn/lcta/
  App.java, Prefs.java, Fmt.java, Theme.java
  net/      Tls (Conscrypt + kho chứng chỉ), Http (theo chuyển hướng, giới hạn kích thước)
  lunar/    Lunar (thuật toán Hồ Ngọc Đức, múi giờ +7), LunarDate
  data/     DataHub (lịch tải, bộ nhớ đệm), các nguồn Weather/Gold/Fuel/Fx/Stock/News
  ui/       MainActivity (bảng), SettingsActivity, Pages, FitColumn, MonthView, WeatherIcon
  sys/      WakeService (cảm biến tiệm cận), BootReceiver, Scheduler/ScheduleReceiver, AdminReceiver
  update/   Updater, InstallReceiver
```

## Lưu ý

- Các nguồn vang.today, giaxanghomnay.com, Entrade/DNSE là API/trang **không chính thức**, có thể thay đổi. Lỗi ở nguồn nào chỉ ảnh hưởng trang đó và hiện trên dòng trạng thái.
- Máy 6,7" đời mới: nhiều hãng dùng cảm biến tiệm cận "ảo" chỉ chạy khi đang gọi, nên vẫy tay có thể không bật được màn đã tắt.
- Muốn bảng tự hiện lại sau khi tắt màn: đặt khoá màn hình là "Không" hoặc "Vuốt".
- Trước khi gỡ app, gỡ quyền quản trị trong Cài đặt của app (nếu đã cấp).
- Điện thoại cũ cắm sạc 24/7 dễ phồng pin: giữ độ sáng đêm thấp và thỉnh thoảng kiểm tra máy.

## Giấy phép thành phần bên thứ ba

- Font Inter (bản rút gọn): SIL Open Font License 1.1, xem `app/src/main/assets/fonts/OFL.txt`.
- Conscrypt: Apache License 2.0.
- Bộ chứng chỉ gốc trong `res/raw/cacerts.pem`: lấy từ kho chứng chỉ Mozilla (MPL 2.0).
