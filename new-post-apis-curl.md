# 本次新增帖子接口 `curl` 使用清单

本文档整理了本次对话新增的帖子相关接口，便于本地联调和接口测试。

## 1. 环境变量

建议先在终端里设置下面几个变量：

```bash
BASE_URL="http://localhost:8080"
SELLER_ID="seller001"
POST_ID="post001"
ADMIN_TOKEN="你的管理员Bearer Token"
```

## 2. 根据 sellerId 查询该卖家所有帖子

接口：`GET /post/seller/{sellerId}`

```bash
curl -X GET "$BASE_URL/post/seller/$SELLER_ID"
```

## 3. 管理员分页查看所有帖子

接口：`GET /post/admin/posts`

```bash
curl -X GET "$BASE_URL/post/admin/posts?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 4. 管理员按状态筛选帖子

可选状态：

- `PUBLISHED`
- `RESERVED`
- `SOLD`
- `DELISTED`
- `BANNED`

```bash
curl -X GET "$BASE_URL/post/admin/posts?pageNum=1&pageSize=10&status=BANNED" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 5. 管理员按卖家筛选帖子

```bash
curl -X GET "$BASE_URL/post/admin/posts?pageNum=1&pageSize=10&sellerId=$SELLER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 6. 管理员按城市筛选帖子

```bash
curl -X GET "$BASE_URL/post/admin/posts?pageNum=1&pageSize=10&city=guangzhou" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 7. 管理员按锅炉类型筛选帖子

支持值示例：`STEAM`、`HOT_WATER`

```bash
curl -X GET "$BASE_URL/post/admin/posts?pageNum=1&pageSize=10&boilerType=STEAM" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 8. 管理员按品牌筛选帖子

```bash
curl -X GET "$BASE_URL/post/admin/posts?pageNum=1&pageSize=10&brand=Test" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 9. 管理员按燃料类型筛选帖子

```bash
curl -X GET "$BASE_URL/post/admin/posts?pageNum=1&pageSize=10&fuelType=Gas" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 10. 管理员组合筛选并排序

支持排序字段：

- `publishTime`
- `updateTime`
- `price`
- `viewCount`
- `status`
- `sellerId`

排序方向：

- `asc`
- `desc`

```bash
curl -X GET "$BASE_URL/post/admin/posts?pageNum=1&pageSize=10&status=PUBLISHED&sellerId=$SELLER_ID&sortField=updateTime&sortOrder=desc" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 11. 管理员查看单条帖子详情

接口：`GET /post/admin/posts/{postId}`

```bash
curl -X GET "$BASE_URL/post/admin/posts/$POST_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 12. 说明

- `/post/seller/{sellerId}` 当前不需要管理员鉴权。
- `/post/admin/**` 需要管理员登录后的 Bearer Token。
- 管理员查看详情接口不会增加帖子浏览量，适合后台管理页面使用。
