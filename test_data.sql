-- ======================================
-- 测试数据 SQL - Transaction & Review 模块
-- 使用方法：mysql -u root -p123456 boiler-jun < test_data.sql
-- ======================================

USE `boiler-jun`;

-- 清除旧测试数据（注意顺序，先删有外键依赖的表）
DELETE FROM `review` WHERE orderId LIKE 'test_%';
DELETE FROM `order` WHERE orderId LIKE 'test_%';
DELETE FROM `transaction` WHERE transactionId LIKE 'test_%';
DELETE FROM `post` WHERE postId LIKE 'test_%';
DELETE FROM `boiler` WHERE boilerId LIKE 'test_%';
DELETE FROM `buyer` WHERE buyerId IN ('test_buyer_001', 'test_seller_001');
DELETE FROM `seller` WHERE sellerId IN ('test_seller_001');
DELETE FROM `user` WHERE userId IN ('test_buyer_001', 'test_seller_001');

-- ======================================
-- 1. 用户表（买家 + 卖家）
-- ======================================
INSERT INTO `user` (userId, password, username, phone, email, userType, creditScore, registrationDate, verificationStatus) VALUES
('test_buyer_001',  '123456', 'test_buyer_001',  '13800000001', 'buyer001@test.com',  'BUYER',  75, CURDATE(), 'VERIFIED'),
('test_seller_001', '123456', 'test_seller_001', '13900000001', 'seller001@test.com', 'SELLER', 80, CURDATE(), 'VERIFIED');

-- 2. 买家表（外键依赖 user）
INSERT INTO `buyer` (buyerId) VALUES
('test_buyer_001'),
('test_seller_001');

-- 3. 卖家表（外键依赖 user）
INSERT INTO `seller` (sellerId, shopName, shopAddress, businessLicense, legalPersonId, qualificationStatus, guaranteeDeposit, completedTransactionCount, positiveRatingRate) VALUES
('test_seller_001', 'Test Boiler Shop', 'Guangzhou', 'BL-TEST-001', '440100199901011237', 'APPROVED', 10000.00, 0, 100.00);

-- ======================================
-- 4. 锅炉表
-- ======================================
INSERT INTO `boiler` (boilerId, model, brand, boilerType, tonnage, fuelType, workingPressure, noxEmissions, footprintArea, manufactureDate, evaporationCapacity, ratedThermalPower, thermalEfficiency, equipmentCondition, usageHours, testReport, ratedOutletWaterTemperature) VALUES
('test_boiler_001', 'WNS2-1.25-Q', 'TestBrand', 'Steam Boiler', 2.00, 'Natural Gas', 1.25, 30.00, 8.00, '2020-06-15', 2.00, 1500.00, 92.00, 'Good', 5000.00, 'R-001', 0);

-- ======================================
-- 5. 帖子表（外键依赖 seller + boiler）
-- ======================================
INSERT INTO `post` (postId, sellerId, title, price, description, status, publishTime, updateTime, viewCount, mediaFiles, aiValuationRange, city, boilerId) VALUES
('test_post_001', 'test_seller_001', '二手WNS2蒸汽锅炉转让', 50000.00, '9成新，使用5000小时，天然气锅炉，性能良好', 'PUBLISHED', CURDATE(), CURDATE(), 10, '', '45000-55000', '广州', 'test_boiler_001');

-- ======================================
-- 验证数据
-- ======================================
SELECT '=== 测试数据已插入 ===' AS info;
SELECT userId, username, userType FROM `user` WHERE userId LIKE 'test_%';
SELECT postId, sellerId, title, price, status FROM `post` WHERE postId LIKE 'test_%';
SELECT boilerId, model, brand FROM `boiler` WHERE boilerId LIKE 'test_%';
