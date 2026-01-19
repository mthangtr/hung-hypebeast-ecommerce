# Flow & Endpoints - Phase 1 Implementation

## 📋 Tổng Quan

Hệ thống e-commerce Hung Hypebeast được thiết kế cho 2 luồng chính:
1. **Customer Flow** - Khách hàng mua hàng, thanh toán, theo dõi đơn
2. **Admin Flow** - Nhân viên quản lý và xử lý đơn hàng

---

## 👥 1. CUSTOMER FLOW - Khách Hàng Đặt Hàng & Theo Dõi

### Flow Tổng Quan
```
Thêm vào Giỏ
    ↓
Vào Trang Checkout (Reserve Hàng)
    ↓
Điền Thông Tin Ship → Chọn Thanh Toán
    ↓
Bấm "Đặt Hàng"
    ↓
Tạo Order (Chốt Đơn)
    ↓
Gửi Email Xác Nhận + Tracking Link
    ↓
Khách Bấm Link Xem Trạng Thái Đơn
```

### 1.1 Bước 1: Thêm Hàng Vào Giỏ
**Usecase:** Khách hàng muốn mua 1 sản phẩm (size, màu cụ thể)

**Endpoint:**
```
POST /api/cart/add
Headers: X-Session-Id: {session_id}
Body:
{
  "variantId": 5,
  "quantity": 2
}
```

**Service:**
- `CartService.addItemToCart(sessionId, variantId, quantity)`
  - ✓ Kiểm tra variant tồn tại
  - ✓ Kiểm tra đủ stock (stockQuantity - reservedQuantity)
  - ✓ Tìm cart theo sessionId, nếu không có thì tạo mới
  - ✓ Thêm hoặc cập nhật CartItem vào cart

---

### 1.2 Bước 2: Vào Trang Checkout - Reserve Hàng (10-15 phút)
**Usecase:** Khách bấm nút "Thanh Toán Ngay" → Hệ thống giữ hàng để người khác không mua được

**Endpoint:**
```
POST /api/checkout/reserve
Headers: X-Session-Id: {session_id}
```

**Response:**
```json
{
  "success": true,
  "message": "Inventory reserved for 15 minutes",
  "data": {
    "sessionId": "abc123",
    "items": [
      {
        "variantId": 5,
        "sku": "DRAGON-BLK-M",
        "productName": "Áo Thun Rồng",
        "size": "M",
        "color": "Đen",
        "quantity": 2
      }
    ],
    "expiresAt": "2026-01-19T14:25:00",
    "remainingSeconds": 900
  }
}
```

**Service:**
- `InventoryReservationService.reserveInventory(sessionId)`
  - ✓ Lấy cart từ sessionId
  - ✓ Hủy reservation cũ nếu có (tránh trùng lặp)
  - ✓ **Cho mỗi item trong cart:**
    - Kiểm tra stock hiện tại = stockQuantity - reservedQuantity
    - Nếu không đủ → rollback & throw INSUFFICIENT_STOCK
    - Tăng reservedQuantity (giữ hàng)
    - Lưu InventoryReservation với expiresAt = now + 15 phút
  - ✓ Trả về danh sách hàng đang giữ + countdown

**Ghi chú:**
- Hàng được giữ trong DB qua `InventoryReservation` entity
- Có scheduler tự động release hàng khi hết 15 phút → `InventoryReservationScheduler`
- Khách có thể check thời gian còn lại bằng: `GET /api/checkout/reservation`

---

### 1.3 Bước 3: Điền Thông Tin Ship & Thanh Toán
**Usecase:** Khách điền địa chỉ, chọn COD hay Chuyển Khoản

**Endpoint:**
```
POST /api/checkout/order
Headers: X-Session-Id: {session_id}
Body:
{
  "customerName": "Nguyễn Văn A",
  "customerEmail": "khach@email.com",
  "customerPhone": "0912345678",
  "shippingAddress": "123 Đường Nguyễn Huệ",
  "shippingCity": "TP. HCM",
  "shippingDistrict": "Quận 1",
  "paymentMethod": "COD",  // hoặc "SEPAY"
  "customerNote": "Giao cho bảo vệ tòa nhà"
}
```

**Service:**
- `OrderService.createOrder(...)`
  - ✓ Lấy cart từ sessionId
  - ✓ Kiểm tra cart không rỗng
  - ✓ **Cho mỗi CartItem tạo OrderItem:**
    - Tính giá = basePrice + priceAdjustment
    - Tính subtotal = price × quantity
    - Tính total = tổng tất cả subtotal
  - ✓ Tạo Order entity:
    - orderNumber = "ORD-" + timestamp (để unique)
    - trackingToken = UUID (để khách track sau)
    - status = "pending"
    - paymentStatus = "pending" (chưa thanh toán)
  - ✓ **Gọi `InventoryReservationService.completeReservation(sessionId, orderId)`**
    - Convert reservedQuantity → commit vào stock (trừ stockQuantity)
    - Đánh dấu reservation = "completed"
  - ✓ **Xóa cart** (không cần giữ nữa)
  - ✓ **Return Order** với tất cả thông tin

---

### 1.4 Bước 4: Gửi Email Xác Nhận + Tracking Link
**Tự động kích hoạt:** Sau khi order tạo xong

**Service:**
- `EmailService.sendOrderConfirmation(order)`
  - ✓ Build tracking link = `{baseUrl}/track/{trackingToken}`
    - VD: `http://localhost:3000/track/550e8400-e29b-41d4-a716-446655440000`
  - ✓ Build email nội dung:
    ```
    Cảm ơn đã mua hàng!
    Mã đơn: ORD-1705679400000
    Ngày mua: 2026-01-19 13:50:00
    Tổng tiền: 500,000 VND
    
    Địa chỉ giao:
    123 Đường Nguyễn Huệ
    TP. HCM, Quận 1
    
    Theo dõi đơn hàng: http://localhost:3000/track/550e8400...
    ```
  - ✓ Gửi email đến customerEmail (xử lý asynchronously, ko block)
  - ✓ Nếu lỗi gửi → log error (không throw exception)

---

### 1.5 Bước 5: Khách Bấm Link Theo Dõi Đơn Hàng
**Usecase:** Khách nhận email, bấm link → xem trạng thái đơn (không cần đăng nhập)

**Endpoint:**
```
GET /api/orders/track/{token}
Path Variable: token = UUID tracking token

Example:
GET /api/orders/track/550e8400-e29b-41d4-a716-446655440000
```

**Response:**
```json
{
  "success": true,
  "message": "Order retrieved successfully",
  "data": {
    "id": 42,
    "orderNumber": "ORD-1705679400000",
    "trackingToken": "550e8400-e29b-41d4-a716-446655440000",
    "customerName": "Nguyễn Văn A",
    "customerEmail": "khach@email.com",
    "customerPhone": "0912345678",
    "shippingAddress": "123 Đường Nguyễn Huệ",
    "shippingCity": "TP. HCM",
    "shippingDistrict": "Quận 1",
    "paymentMethod": "COD",
    "paymentStatus": "pending",  // pending, paid, failed
    "subtotal": 500000,
    "shippingFee": 0,
    "totalAmount": 500000,
    "status": "pending",  // pending, confirmed, processing, shipping, completed, cancelled
    "createdAt": "2026-01-19T13:50:00",
    "confirmedAt": null,
    "paidAt": null,
    "shippedAt": null,
    "completedAt": null,
    "cancelledAt": null,
    "items": [
      {
        "id": 1,
        "variantId": 5,
        "productName": "Áo Thun Rồng",
        "variantSku": "DRAGON-BLK-M",
        "variantSize": "M",
        "variantColor": "Đen",
        "unitPrice": 250000,
        "quantity": 2,
        "subtotal": 500000
      }
    ]
  }
}
```

**Service:**
- `OrderService.getOrderByTrackingToken(token)`
  - ✓ Tìm Order theo trackingToken
  - ✓ Return order với tất cả details (items, timestamps, status)
  - ✓ Nếu không tìm thấy → throw ORDER_NOT_FOUND

---

## 👨‍💼 2. ADMIN FLOW - Nhân Viên Quản Lý Đơn Hàng

### Flow Tổng Quan
```
Nhân Viên Đăng Nhập
    ↓
Xem Danh Sách Đơn
    ↓
Chọn Đơn (Xem Chi Tiết)
    ↓
Cập Nhật Trạng Thái
    ↓
Lưu Thay Đổi + Log History
```

### 2.1 Bước 1: Đăng Nhập
**Usecase:** Nhân viên kho/admin nhập username/password

**Endpoint:**
```
POST /api/admin/auth/login
Body:
{
  "username": "nhan_vien_kho",
  "password": "secure_password"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "id": 1,
    "username": "nhan_vien_kho",
    "fullName": "Nguyễn Văn B"
  }
}
```

**Session:**
- Server lưu session với:
  - `adminId = 1`
  - `username = "nhan_vien_kho"`

**Service:**
- `AdminAuthService.authenticate(username, password)`
  - ✓ Tìm AdminUser theo username
  - ✓ So sánh password bằng BCrypt (passwordHash)
  - ✓ Nếu sai → throw INVALID_INPUT
  - ✓ Return AdminUser entity
- `AdminAuthService.recordLogin(adminId)`
  - ✓ Cập nhật lastLoginAt = now

---

### 2.2 Bước 2: Xem Danh Sách Đơn Hàng
**Usecase:** Admin xem tất cả đơn hàng, có thể filter theo status, phân trang

**Endpoint:**
```
GET /api/admin/orders
Query Parameters:
  - page=0 (số trang, mặc định 0)
  - size=10 (số đơn/trang, mặc định 10)
  - status=pending (optional: pending, confirmed, processing, shipping, completed, cancelled)

Example:
GET /api/admin/orders?page=0&size=10&status=pending
```

**Response:**
```json
{
  "success": true,
  "message": "Orders retrieved successfully",
  "data": {
    "content": [
      {
        "id": 42,
        "orderNumber": "ORD-1705679400000",
        "customerName": "Nguyễn Văn A",
        "customerEmail": "khach@email.com",
        "totalAmount": 500000,
        "status": "pending",
        "paymentStatus": "pending",
        "createdAt": "2026-01-19T13:50:00"
      },
      {
        "id": 41,
        "orderNumber": "ORD-1705679300000",
        "customerName": "Trần Thị C",
        "customerEmail": "khach2@email.com",
        "totalAmount": 750000,
        "status": "confirmed",
        "paymentStatus": "paid",
        "createdAt": "2026-01-19T13:45:00"
      }
    ],
    "totalPages": 5,
    "totalElements": 47
  }
}
```

**Service:**
- `OrderService.listOrders(status, pageable)`
  - ✓ Nếu có filter status → `findByStatus(status, pageable)`
  - ✓ Nếu không → `findAll(pageable)`
  - ✓ Return Page<Order> đã sắp xếp

---

### 2.3 Bước 3: Xem Chi Tiết Đơn Hàng
**Usecase:** Admin chọn 1 đơn để xem đầy đủ thông tin

**Endpoint:**
```
GET /api/admin/orders/{orderId}
Path Variable: orderId = 42

Example:
GET /api/admin/orders/42
```

**Response:**
```json
{
  "success": true,
  "message": "Order retrieved successfully",
  "data": {
    "id": 42,
    "orderNumber": "ORD-1705679400000",
    "trackingToken": "550e8400-e29b-41d4-a716-446655440000",
    "customerName": "Nguyễn Văn A",
    "customerEmail": "khach@email.com",
    "customerPhone": "0912345678",
    "shippingAddress": "123 Đường Nguyễn Huệ",
    "shippingCity": "TP. HCM",
    "shippingDistrict": "Quận 1",
    "paymentMethod": "COD",
    "paymentStatus": "pending",
    "subtotal": 500000,
    "shippingFee": 0,
    "totalAmount": 500000,
    "status": "pending",
    "customerNote": "Giao cho bảo vệ",
    "adminNote": null,
    "createdAt": "2026-01-19T13:50:00",
    "updatedAt": "2026-01-19T13:50:00",
    "confirmedAt": null,
    "paidAt": null,
    "shippedAt": null,
    "completedAt": null,
    "items": [
      {
        "id": 1,
        "variantId": 5,
        "productName": "Áo Thun Rồng",
        "variantSku": "DRAGON-BLK-M",
        "variantSize": "M",
        "variantColor": "Đen",
        "unitPrice": 250000,
        "quantity": 2,
        "subtotal": 500000
      }
    ]
  }
}
```

**Service:**
- `OrderService.getOrderById(orderId)`
  - ✓ Tìm Order theo ID
  - ✓ Eager-load items + history
  - ✓ Return order entity

---

### 2.4 Bước 4: Cập Nhật Trạng Thái Đơn Hàng
**Usecase:** Nhân viên kho xác nhận → xử lý → gửi hàng → hoàn thành

**Endpoint:**
```
PATCH /api/admin/orders/{orderId}/status
Path Variable: orderId = 42
Body:
{
  "status": "confirmed",
  "adminNote": "Đã xác nhận - Hàng sẵn sàng"
}
```

**Valid Status Transitions (State Machine):**
```
pending ──→ confirmed ──→ processing ──→ shipping ──→ completed
   ↓           ↓              ↓            ↓
   └─→ cancelled (at any point)
```

**Response:**
```json
{
  "success": true,
  "message": "Order status updated successfully",
  "data": {
    "id": 42,
    "orderNumber": "ORD-1705679400000",
    "status": "confirmed",
    "adminNote": "Đã xác nhận - Hàng sẵn sàng",
    "confirmedAt": "2026-01-19T14:00:00",
    "updatedAt": "2026-01-19T14:00:00",
    ...
  }
}
```

**Service:**
- `OrderService.updateOrderStatus(orderId, newStatus, adminNote, changedBy)`
  - ✓ Lấy order từ DB
  - ✓ Kiểm tra status transition hợp lệ (`isValidStatusTransition`)
  - ✓ Cập nhật:
    - order.status = newStatus
    - order.adminNote = adminNote
    - Timestamp tương ứng (confirmedAt, shippedAt, completedAt, cancelledAt)
  - ✓ **Gọi `recordStatusHistory(order, oldStatus, newStatus, changedBy, adminNote)`**
    - Lưu OrderStatusHistory:
      - fromStatus = "pending"
      - toStatus = "confirmed"
      - changedBy = "admin:1" (admin ID)
      - changedAt = now
      - note = "Đã xác nhận - Hàng sẵn sàng"
  - ✓ Save order + history
  - ✓ Return updated order

---

### 2.5 Bonus: Xem Lịch Sử Thay Đổi Đơn Hàng
**Data được lưu tự động** trong `OrderStatusHistory` mỗi khi admin cập nhật status

**Dữ liệu lưu:**
```
[
  {
    "id": 1,
    "order_id": 42,
    "from_status": "pending",
    "to_status": "confirmed",
    "changed_by": "admin:1",  // ai thay đổi
    "changed_at": "2026-01-19T14:00:00",
    "note": "Đã xác nhận - Hàng sẵn sàng"
  },
  {
    "id": 2,
    "order_id": 42,
    "from_status": "confirmed",
    "to_status": "processing",
    "changed_by": "admin:1",
    "changed_at": "2026-01-19T14:15:00",
    "note": "Đang xếp hàng"
  }
]
```

---

## 📊 Sơ Đồ Tương Tác Toàn Bộ

```
┌─────────────────────────────────────────────────────────────────┐
│                      CUSTOMER (PUBLIC)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Cart (Session-based)                                            │
│    ├─ POST /api/cart/add ──→ CartService.addItemToCart()        │
│    ├─ PATCH /api/cart/{id} ──→ CartService.updateItem()         │
│    └─ DELETE /api/cart/{id} ──→ CartService.removeItem()        │
│                                                                   │
│  Checkout Flow                                                    │
│    ├─ POST /api/checkout/reserve                                │
│    │   ──→ InventoryReservationService.reserveInventory()       │
│    │   (Giữ hàng 10-15 phút, cập nhật reservedQuantity)        │
│    │                                                              │
│    ├─ POST /api/checkout/order                                  │
│    │   ──→ OrderService.createOrder()                           │
│    │       ├─ Tạo Order + OrderItems                            │
│    │       ├─ InventoryReservationService.completeReservation() │
│    │       │  (Convert reserved → committed stock)              │
│    │       ├─ Delete Cart                                       │
│    │       └─ EmailService.sendOrderConfirmation()              │
│    │          (Gửi email với tracking link)                     │
│    │                                                              │
│    └─ GET /api/orders/track/{token}                             │
│        ──→ OrderService.getOrderByTrackingToken()               │
│        (Xem trạng thái đơn - không cần đăng nhập)              │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        ADMIN (PROTECTED)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Authentication                                                  │
│    ├─ POST /api/admin/auth/login                                │
│    │   ──→ AdminAuthService.authenticate()                      │
│    │   (Verify username/password, lưu session)                  │
│    │                                                              │
│    ├─ GET /api/admin/auth/check                                 │
│    │   ──→ Verify session còn valid                             │
│    │                                                              │
│    └─ POST /api/admin/auth/logout                               │
│        ──→ Invalidate session                                    │
│                                                                   │
│  Order Management                                                │
│    ├─ GET /api/admin/orders?page=0&size=10&status=pending      │
│    │   ──→ OrderService.listOrders()                            │
│    │   (List tất cả orders, có filter + pagination)            │
│    │                                                              │
│    ├─ GET /api/admin/orders/{orderId}                           │
│    │   ──→ OrderService.getOrderById()                          │
│    │   (Xem đầy đủ details + items + history)                   │
│    │                                                              │
│    └─ PATCH /api/admin/orders/{orderId}/status                  │
│        ──→ OrderService.updateOrderStatus()                     │
│            ├─ Validate state transition                         │
│            ├─ Update status + timestamps                        │
│            └─ recordStatusHistory()                             │
│               (Log who changed, what changed, when)             │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    BACKGROUND SERVICES                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Scheduler (Chạy định kỳ)                                        │
│    └─ InventoryReservationScheduler                             │
│       ├─ Mỗi 1 phút check expired reservations                  │
│       └─ Release hàng nếu quá 15 phút chưa checkout            │
│          (Gọi: releaseExpiredReservations())                    │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔒 Security & Constraints

### Customer (Public)
- ✓ Không cần authentication
- ✓ Dùng Session ID (từ Frontend) để quản lý cart
- ✓ Dùng UUID tracking token để xem đơn (không thể guess)

### Admin (Protected)
- ✓ Phải login trước (username/password)
- ✓ Session-based authentication
- ✓ Mỗi request cần valid session
- ✓ Mỗi status change được log (ai thay đổi, khi nào, cái gì)

---

## 💾 Database Changes

### Entities Tạo/Update:
1. `Order` - Đơn hàng chính
2. `OrderItem` - Chi tiết sản phẩm trong đơn
3. `OrderStatusHistory` - Lịch sử thay đổi status
4. `InventoryReservation` - Giữ hàng tạm thời
5. `AdminUser` - Tài khoản nhân viên
6. `Cart`, `CartItem` - Giỏ hàng (existed)
7. `Product`, `ProductVariant` - Sản phẩm (existed)

---

## 📝 Lưu Ý Quan Trọng

1. **Inventory Lock (10-15 phút)**
   - Khi khách bấm "Checkout", hệ thống **reserve** hàng
   - Người khác không mua được trong thời gian này
   - Nếu quá 15 phút không thanh toán → tự động release (scheduler)

2. **Order Creation**
   - Mới tạo order thì status = "pending"
   - Stock chỉ trừ khi `completeReservation` được gọi
   - Không gọi delete cart trước khi order save thành công

3. **Email & Tracking**
   - Email gửi asynchronously (không block order creation)
   - Tracking link không cần login - dùng UUID token
   - Frontend lưu token để dùng sau

4. **Admin Status Flow**
   - pending → confirmed (xác nhận có hàng)
   - confirmed → processing (bắt đầu chuẩn bị)
   - processing → shipping (đang giao)
   - shipping → completed (giao thành công)
   - Bất kỳ status nào cũng có thể → cancelled

5. **Payment Status**
   - Phase 1: Chỉ hỗ trợ COD (paymentStatus luôn "pending")
   - Phase 2: Sẽ integrate SePay (tự động update khi nhận webhook)

---

## 🚀 Ví Dụ Thực Tế

### Scenario 1: Khách Mua Áo Thun
```
1. Khách thêm vào cart:
   POST /api/cart/add (X-Session-Id: abc123)
   → CartItem (DRAGON-BLK-M, quantity=2) được thêm

2. Khách bấm "Thanh Toán":
   POST /api/checkout/reserve (X-Session-Id: abc123)
   → InventoryReservation tạo, reservedQuantity += 2
   → Trả countdown 15 phút

3. Khách điền info + bấm "Đặt Hàng":
   POST /api/checkout/order (X-Session-Id: abc123)
   → Order tạo (status=pending)
   → Stock trừ (-2): stockQuantity = 8 (từ 10)
   → Email gửi: "Cảm ơn đã mua! Link: /track/550e8400-..."
   → Cart delete

4. Khách nhận email, bấm link:
   GET /api/orders/track/550e8400-...
   → Xem order details: status=pending, items, giá tiền
```

### Scenario 2: Admin Xử Lý Đơn
```
1. Admin đăng nhập:
   POST /api/admin/auth/login
   → Session: adminId=1, username=nhan_vien_kho

2. Admin xem danh sách đơn chờ xác nhận:
   GET /api/admin/orders?status=pending
   → Thấy 5 đơn, mỗi trang 10 đơn

3. Admin chọn đơn #42:
   GET /api/admin/orders/42
   → Xem: khách tên A, 2 cái áo, địa chỉ giao

4. Admin xác nhận & cập nhật status:
   PATCH /api/admin/orders/42/status
   Body: {"status": "confirmed", "adminNote": "Đã xác nhận"}
   → OrderStatusHistory log:
     from=pending, to=confirmed, changedBy=admin:1, time=14:00

5. Admin sau đó xử lý & gửi:
   PATCH /api/admin/orders/42/status
   Body: {"status": "shipping"}
   → OrderStatusHistory log: from=confirmed, to=shipping
   → Order.shippedAt = now
```

---

## 📚 Mapping Usecase → Endpoint → Service

| Usecase | Endpoint | Service Function | Kết Quả |
|---------|----------|------------------|--------|
| Thêm vào giỏ | POST /api/cart/add | CartService.addItemToCart() | CartItem tạo/update |
| Reserve hàng | POST /api/checkout/reserve | InventoryReservationService.reserveInventory() | Reservation tạo, reservedQuantity tăng |
| Tạo đơn | POST /api/checkout/order | OrderService.createOrder() | Order tạo, stock trừ, email gửi |
| Track đơn | GET /api/orders/track/{token} | OrderService.getOrderByTrackingToken() | Trả order details |
| Admin login | POST /api/admin/auth/login | AdminAuthService.authenticate() | Session lưu |
| List đơn | GET /api/admin/orders | OrderService.listOrders() | Trả Page<Order> |
| View đơn | GET /api/admin/orders/{id} | OrderService.getOrderById() | Trả order details |
| Update status | PATCH /api/admin/orders/{id}/status | OrderService.updateOrderStatus() | Status update, history log |
