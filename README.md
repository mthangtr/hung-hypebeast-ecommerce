# Hung Hypebeast E-commerce Backend

Backend API cho hệ thống e-commerce "Hung Hypebeast", tập trung vào:
Catalog & SKU, Shopping Cart, Inventory Reservation, Checkout (COD), Tracking đơn hàng,
và Admin Order Management.

## Tech Stack
- Java 21 (Spring Boot 3.x)
- PostgreSQL 16 (Docker)
- Maven Wrapper

## Yêu cầu môi trường
- Java 21
- Docker Desktop
- Node.js: Không yêu cầu cho backend này (frontend tách riêng)

## Thiết lập Database
1. Khởi chạy PostgreSQL bằng Docker:
```
docker compose up -d
```

2. Schema được tự động tạo từ `src/main/resources/schema.sql`
   thông qua `docker-compose.yml`.

## Seed dữ liệu mẫu
Chạy lệnh sau để import `seed-data.sql`:
```
Get-Content src/main/resources/seed-data.sql | docker exec -i 03d20bbdf899 psql -U hung_admin -d hung_hypebeast_ecommerce
```

## Cấu hình Email
Ứng dụng dùng SMTP Gmail. Thiết lập biến môi trường:
```
MAIL_USERNAME=<abc@gmail.com>
MAIL_PASSWORD=<app_password>
```

## Chạy ứng dụng
```
mvnw.cmd spring-boot:run
```

Ứng dụng mặc định chạy tại:
```
http://localhost:8080
```

## API Collection
Postman collection:
`postman/Hung-Hypebeast-Catalog-API.postman_collection.json`