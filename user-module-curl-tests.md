# User Module Curl Tests

本文档整理了当前用户模块已实现接口的 `curl` 测试命令格式。

## Base URL

```bash
BASE_URL="http://127.0.0.1:8080"
```

## 1. 用户注册

### 1.1 买家注册

```bash
curl -X POST "$BASE_URL/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "buyer_test_003",
    "password": "123456",
    "phone": "13800000003",
    "email": "buyer003@example.com",
    "userType": "BUYER"
  }'
```

### 1.2 卖家注册

```bash
curl -X POST "$BASE_URL/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "seller_test_003",
    "password": "123456",
    "phone": "13900000003",
    "email": "seller003@example.com",
    "userType": "SELLER",
    "shopName": "Alpha Boiler Shop 3",
    "businessLicense": "BL-003",
    "legalPersonId": "440100199901011237"
  }'
```

### 1.3 注册说明

- `userType` 目前只支持 `BUYER` 和 `SELLER`
- 卖家注册时，`shopName` 必填
- `shippingAddress` 和 `shopAddress` 虽然 DTO 中有字段，但当前数据库表结构未支持持久化，测试时可先不传

## 2. 用户登录

```bash
curl -X POST "$BASE_URL/user/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "seller_test_003",
    "password": "123456"
  }'
```

## 3. 查询用户个人信息

将下面的 `USER_ID` 替换为注册成功后返回的 `data.userId`。

```bash
USER_ID="请替换为真实userId"

curl "$BASE_URL/user/profile/$USER_ID"
```

## 4. 更新用户个人信息

```bash
curl -X PUT "$BASE_URL/user/profile" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "请替换为真实userId",
    "phone": "13800009999",
    "email": "buyer003_new@example.com"
  }'
```

说明：

- 当前接口实际会更新 `phone` 和 `email`
- `shippingAddress` 字段当前不会落库

## 5. 查询卖家资料

将下面的 `SELLER_USER_ID` 替换为卖家注册成功后返回的 `data.userId`。

```bash
SELLER_USER_ID="请替换为卖家userId"

curl "$BASE_URL/user/seller-profile/$SELLER_USER_ID"
```

## 6. 更新卖家资料

```bash
curl -X PUT "$BASE_URL/user/seller-profile" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "请替换为卖家userId",
    "shopName": "Alpha Boiler Shop 3 Updated",
    "businessLicense": "BL-003-NEW",
    "legalPersonId": "440100199901011238"
  }'
```

说明：

- 当前接口实际会更新 `shopName`、`businessLicense`、`legalPersonId`
- `shopAddress` 字段当前不会落库

## 7. 一组可直接测试的顺序

建议按下面顺序测试：

1. 先调用“卖家注册”或“买家注册”
2. 从返回结果中取出 `data.userId`
3. 调用“用户登录”
4. 调用“查询用户个人信息”
5. 如是买家，调用“更新用户个人信息”
6. 如是卖家，调用“查询卖家资料”和“更新卖家资料”

## 8. 返回格式说明

当前接口统一返回：

```json
{
  "code": 1,
  "msg": null,
  "data": {}
}
```

说明：

- `code = 1` 表示成功
- `code = 0` 表示失败
- 失败原因在 `msg` 字段中

## 9. 异常场景测试

下面这些命令用于测试常见失败场景，便于验证参数校验和业务校验是否正常。

### 9.1 重复用户名注册

先确保数据库里已经存在 `seller_test_005` 或将下面用户名替换为一个已存在用户名。

```bash
curl -X POST "$BASE_URL/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "seller_test_005",
    "password": "123456",
    "phone": "13900000099",
    "email": "duplicate@example.com",
    "userType": "SELLER",
    "shopName": "Duplicate Shop"
  }'
```

预期：

- `code = 0`
- `msg` 类似：`用户名已存在`

### 9.2 注册时缺少用户名

```bash
curl -X POST "$BASE_URL/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "password": "123456",
    "userType": "BUYER"
  }'
```

预期：

- `code = 0`
- `msg` 类似：`用户名不能为空`

### 9.3 注册时缺少密码

```bash
curl -X POST "$BASE_URL/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "buyer_error_001",
    "userType": "BUYER"
  }'
```

预期：

- `code = 0`
- `msg` 类似：`密码不能为空`

### 9.4 注册时用户类型非法

```bash
curl -X POST "$BASE_URL/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user_error_002",
    "password": "123456",
    "userType": "ADMIN"
  }'
```

预期：

- `code = 0`
- `msg` 类似：`用户类型仅支持 BUYER 或 SELLER`

### 9.5 卖家注册缺少店铺名称

```bash
curl -X POST "$BASE_URL/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "seller_error_001",
    "password": "123456",
    "userType": "SELLER"
  }'
```

预期：

- `code = 0`
- `msg` 类似：`卖家注册时店铺名称不能为空`

### 9.6 登录密码错误

```bash
curl -X POST "$BASE_URL/user/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "seller_test_005",
    "password": "wrong-password"
  }'
```

预期：

- `code = 0`
- `msg` 类似：`用户名或密码错误`

### 9.7 登录时缺少用户名

```bash
curl -X POST "$BASE_URL/user/login" \
  -H "Content-Type: application/json" \
  -d '{
    "password": "123456"
  }'
```

预期：

- `code = 0`
- `msg` 类似：`用户名和密码不能为空`

### 9.8 查询不存在的用户

```bash
curl "$BASE_URL/user/profile/not-exist-user-id"
```

预期：

- `code = 0`
- `msg` 类似：`用户不存在`

### 9.9 更新个人信息时缺少 userId

```bash
curl -X PUT "$BASE_URL/user/profile" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "13800001111",
    "email": "error@example.com"
  }'
```

预期：

- `code = 0`
- `msg` 类似：`用户ID不能为空`

### 9.10 用买家 ID 查询卖家资料

将下面的 `BUYER_USER_ID` 替换为一个真实买家 `userId`。

```bash
BUYER_USER_ID="请替换为真实买家userId"

curl "$BASE_URL/user/seller-profile/$BUYER_USER_ID"
```

预期：

- `code = 0`
- `msg` 类似：`当前用户不是卖家`

### 9.11 用买家 ID 更新卖家资料

将下面的 `BUYER_USER_ID` 替换为一个真实买家 `userId`。

```bash
BUYER_USER_ID="请替换为真实买家userId"

curl -X PUT "$BASE_URL/user/seller-profile" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'"$BUYER_USER_ID"'",
    "shopName": "Should Fail Shop",
    "businessLicense": "FAIL-001",
    "legalPersonId": "440100199901019999"
  }'
```

预期：

- `code = 0`
- `msg` 类似：`当前用户不是卖家`

### 9.12 更新卖家资料时缺少 userId

```bash
curl -X PUT "$BASE_URL/user/seller-profile" \
  -H "Content-Type: application/json" \
  -d '{
    "shopName": "No User Id Shop",
    "businessLicense": "FAIL-002",
    "legalPersonId": "440100199901018888"
  }'
```

预期：

- `code = 0`
- `msg` 类似：`用户ID不能为空`

## 10. 管理员接口

以下接口都需要在请求头中传入管理员身份：

```bash
ADMIN_USER_ID="请替换为真实管理员userId"
ADMIN_HEADER="-H X-Admin-User-Id:$ADMIN_USER_ID"
```

说明：

- 后台接口统一要求请求头 `X-Admin-User-Id`
- 只有 `administrator` 表中存在的用户才能调用

### 10.1 管理员查看用户列表

```bash
curl "$BASE_URL/user/admin/users" \
  $ADMIN_HEADER
```

按条件筛选：

```bash
curl "$BASE_URL/user/admin/users?userType=SELLER&qualificationStatus=PENDING" \
  $ADMIN_HEADER
```

支持的筛选参数：

- `username`
- `userType`：`BUYER` / `SELLER` / `ADMIN`
- `verificationStatus`：`UNVERIFIED` / `VERIFIED` / `SUSPENDED`
- `qualificationStatus`：`PENDING` / `APPROVED` / `REJECTED`

### 10.2 管理员查看用户详情

```bash
TARGET_USER_ID="请替换为真实userId"

curl "$BASE_URL/user/admin/users/$TARGET_USER_ID" \
  $ADMIN_HEADER
```

### 10.3 管理员更新用户信息

```bash
curl -X PUT "$BASE_URL/user/admin/users" \
  -H "Content-Type: application/json" \
  $ADMIN_HEADER \
  -d '{
    "userId": "请替换为真实userId",
    "phone": "13800008888",
    "email": "managed_user@example.com",
    "creditScore": 85,
    "verificationStatus": "VERIFIED"
  }'
```

卖家用户可额外修改资质状态：

```bash
curl -X PUT "$BASE_URL/user/admin/users" \
  -H "Content-Type: application/json" \
  $ADMIN_HEADER \
  -d '{
    "userId": "请替换为真实卖家userId",
    "qualificationStatus": "REJECTED"
  }'
```

### 10.4 管理员审核卖家资质

```bash
curl -X PUT "$BASE_URL/user/admin/sellers/qualification" \
  -H "Content-Type: application/json" \
  $ADMIN_HEADER \
  -d '{
    "sellerId": "请替换为真实卖家sellerId",
    "targetStatus": "APPROVED"
  }'
```

审核说明：

- 只允许对 `PENDING` 状态卖家执行审核
- 审核结果仅支持 `APPROVED` 或 `REJECTED`
- 卖家资质审核通过后，信用分会按初始化规则提升到不低于 `80`

## 11. 信用分规则

- 新注册用户默认信用分为 `60`
- 信用分允许管理员在 `0-100` 范围内调整
- 卖家资质审核通过后，信用分会提升到不低于 `80`
- 已审核卖家更新资质资料后，资质状态会重新回到 `PENDING`
