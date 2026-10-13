# Workflow Cập Nhật Ứng Dụng CoC Rank Mobile

Mỗi khi người dùng yêu cầu chỉnh sửa, cập nhật tính năng, hoặc sửa lỗi trong ứng dụng `rank-mobile`, trợ lý AI BẮT BUỘC thực hiện đầy đủ chu trình tự động hóa sau:

1. **Thực hiện thay đổi mã nguồn:**
   - Đảm bảo tuân thủ kiến trúc Clean MVVM, Kotlin 2.x, Jetpack Compose Material 3.
   - Thêm/cập nhật Unit Test tương ứng nếu có thay đổi thuật toán.

2. **Chạy kiểm thử và build bản Release:**
   - Thực thi script: `powershell -ExecutionPolicy Bypass -File .\build-release.ps1 -NoOpenFolder`
   - Đảm bảo Unit Tests đạt 100% PASS.
   - File Release APK mới nhất được tự động ghi đè tại `dist\CoCRank-release-latest.apk`.

3. **Phân phối Firebase App Distribution (nếu đã đăng nhập):**
   - Chạy lệnh upload qua Firebase CLI hoặc script: `powershell -ExecutionPolicy Bypass -File .\distribute-firebase.ps1`
   - Nếu chưa đăng nhập, thông báo và cung cấp đường dẫn file APK mới nhất trong thư mục `dist/`.

4. **Bảo mật và Git Commit:**
   - Đảm bảo không bao giờ commit keystore (`*.jks`), file chứa mật khẩu (`keystore.properties`), hoặc file thừa vào git.
   - Commit với thông điệp rõ ràng theo chuẩn Conventional Commits (`feat:`, `fix:`, `chore:`).
