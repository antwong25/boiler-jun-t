# Post API Curl 测试清单

## 基础说明

- 默认服务地址：`http://localhost:8080`
- 如果你的服务端口不是 `8080`，请先修改下面变量：

```bash
BASE_URL="http://localhost:8080"
```

## 1. 分页查看所有帖子

### 首页默认分页

```bash
curl "$BASE_URL/post/page?pageNum=1&pageSize=10"
```

### 查看第 2 页，每页 5 条

```bash
curl "$BASE_URL/post/page?pageNum=2&pageSize=5"
```

### 按发布时间升序

```bash
curl "$BASE_URL/post/page?pageNum=1&pageSize=10&sortField=publishTime&sortOrder=asc"
```

### 按更新时间倒序

```bash
curl "$BASE_URL/post/page?pageNum=1&pageSize=10&sortField=updateTime&sortOrder=desc"
```

### 按价格升序

```bash
curl "$BASE_URL/post/page?pageNum=1&pageSize=10&sortField=price&sortOrder=asc"
```

### 按浏览量倒序

```bash
curl "$BASE_URL/post/page?pageNum=1&pageSize=10&sortField=viewCount&sortOrder=desc"
```

## 2. 按条件筛选帖子

### 只按城市筛选

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10&city=guangzhou"
```

### 按锅炉类型筛选

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10&boilerType=steam%20boiler"
```

### 按品牌筛选

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10&brand=WNS"
```

### 按燃料类型筛选

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10&fuelType=gas"
```

### 城市 + 锅炉类型组合筛选

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10&city=guangzhou&boilerType=steam%20boiler"
```

### 城市 + 品牌 + 燃料类型组合筛选

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10&city=guangzhou&brand=WNS&fuelType=gas"
```

### 全条件组合筛选

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10&city=guangzhou&boilerType=steam%20boiler&brand=WNS&fuelType=gas"
```

### 筛选结果按价格升序

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10&city=guangzhou&sortField=price&sortOrder=asc"
```

## 3. 异常场景测试

### 筛选接口不传任何条件

预期：返回报错，提示至少提供一个筛选条件。

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10"
```

### 分页参数为非法值

预期：观察后端分页兜底行为。

```bash
curl "$BASE_URL/post/page?pageNum=0&pageSize=0"
```

### 非法排序字段

预期：返回报错，提示排序字段不支持。

```bash
curl "$BASE_URL/post/page?pageNum=1&pageSize=10&sortField=abc&sortOrder=desc"
```

### 非法锅炉类型

预期：返回报错，提示锅炉类型不支持。

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10&boilerType=abc"
```

## 4. 美化输出

如果本机安装了 `python3`，可以使用下面命令美化 JSON 返回结果：

```bash
curl "$BASE_URL/post/page?pageNum=1&pageSize=10" | python3 -m json.tool
```

```bash
curl "$BASE_URL/post/filter?pageNum=1&pageSize=10&city=guangzhou" | python3 -m json.tool
```

## 5. 联调检查点

- `city=guangzhou` 是否能命中数据库中以大写保存的城市
- `boilerType=steam boiler` 是否被正确规范化为系统内部类型
- 返回结构是否包含 `records`、`total`、`pageNum`、`pageSize`、`totalPages`
- 列表是否只返回 `PUBLISHED` 状态的帖子
