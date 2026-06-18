-- ======================================
-- 数据库DDL - Boiler Alpha AI
-- 由 class 6.11改.drawio 自动生成
-- ======================================

create database if not exists `boiler-jun`;

use `boiler-jun`;

-- 用户表
CREATE TABLE `user` (
  userId VARCHAR(255) PRIMARY KEY,
  password VARCHAR(255),
  username VARCHAR(255),
  phone VARCHAR(255),
  email VARCHAR(255),
  userType VARCHAR(50),
  creditScore INT,
  registrationDate DATE,
  verificationStatus VARCHAR(50)
);

-- 卖家表
CREATE TABLE `seller` (
  sellerId VARCHAR(255) PRIMARY KEY,
  shopName VARCHAR(255),
  businessLicense VARCHAR(255),
  legalPersonId VARCHAR(255),
  qualificationStatus VARCHAR(50),
  guaranteeDeposit DECIMAL(10,2),
  completedTransactionCount INT,
  positiveRatingRate DECIMAL(10,2)
);

-- 买家表
CREATE TABLE `buyer` (
  buyerId VARCHAR(255) PRIMARY KEY
);

-- 锅炉表
CREATE TABLE `boiler` (
  boilerId VARCHAR(255) PRIMARY KEY,
  model VARCHAR(255),
  brand VARCHAR(255),
  boilerType VARCHAR(255),
  tonnage DECIMAL(10,2),
  fuelType VARCHAR(255),
  workingPressure DECIMAL(10,2),
  noxEmissions DECIMAL(10,2),
  footprintArea DECIMAL(10,2),
  manufactureStartDate DATE,
  manufactureEndDate DATE,
  evaporationCapacity DECIMAL(10,2),
  ratedThermalPower DECIMAL(10,2),
  thermalEfficiency DECIMAL(10,2),
  equipmentCondition VARCHAR(50),
  usageHours DECIMAL(10,2),
  testReport VARCHAR(255),
  ratedOutletWaterTemperature DECIMAL(10,2),
  applicationScenario VARCHAR(255)
);

-- 管理员表
CREATE TABLE `administrator` (
  adminId VARCHAR(255) PRIMARY KEY
);

-- 帖子表
CREATE TABLE `post` (
  postId VARCHAR(255) PRIMARY KEY,
  sellerId VARCHAR(255) NOT NULL,
  title VARCHAR(255),
  price DECIMAL(10,2),
  description TEXT,
  status VARCHAR(50),
  publishTime DATE,
  updateTime DATE,
  viewCount INT,
  mediaFiles TEXT,
  aiValuationRange VARCHAR(255),
  city VARCHAR(255),
  boilerId VARCHAR(255)
);

-- 订单表
CREATE TABLE `order` (
  orderId VARCHAR(255) PRIMARY KEY,
  transactionId VARCHAR(255) NOT NULL,
  orderStatus VARCHAR(50),
  createTime DATE,
  updateTime DATE
);

-- 交易表
CREATE TABLE `transaction` (
  transactionId VARCHAR(255) PRIMARY KEY,
  buyerId VARCHAR(255) NOT NULL,
  sellerId VARCHAR(255) NOT NULL,
  transactionAmount DECIMAL(10,2),
  transactionTime DATE,
  transactionStatus VARCHAR(50),
  bookingStatus VARCHAR(50),
  logisticsInfo VARCHAR(255)
);

-- 评论表
CREATE TABLE `review` (
  reviewId VARCHAR(255) PRIMARY KEY,
  buyerId VARCHAR(255) NOT NULL,
  postId VARCHAR(255) NOT NULL,
  orderId VARCHAR(255) NOT NULL,
  rating INT,
  content VARCHAR(255),
  reviewTime DATE
);

-- 会话表
CREATE TABLE `chatSession` (
  sessionId VARCHAR(255) PRIMARY KEY,
  sellerId VARCHAR(255) NOT NULL,
  buyerId VARCHAR(255) NOT NULL,
  postId VARCHAR(255) NOT NULL,
  createTime DATE,
  lastMessageTime DATE,
  isArchived BOOLEAN,
  messageList TEXT
);

-- ======================================
-- 添加外键约束
-- ======================================

-- 卖家关联到用户
ALTER TABLE `seller` 
ADD CONSTRAINT fk_seller_user 
FOREIGN KEY (sellerId) REFERENCES `user`(userId);

-- 买家关联到用户
ALTER TABLE `buyer` 
ADD CONSTRAINT fk_buyer_user 
FOREIGN KEY (buyerId) REFERENCES `user`(userId);

-- 帖子关联到卖家
ALTER TABLE `post` 
ADD CONSTRAINT fk_post_seller 
FOREIGN KEY (sellerId) REFERENCES `seller`(sellerId);

-- 帖子关联到锅炉
ALTER TABLE `post` 
ADD CONSTRAINT fk_post_boiler 
FOREIGN KEY (boilerId) REFERENCES `boiler`(boilerId);

-- 订单关联到交易
ALTER TABLE `order` 
ADD CONSTRAINT fk_order_transaction 
FOREIGN KEY (transactionId) REFERENCES `transaction`(transactionId);

-- 交易关联到买家
ALTER TABLE `transaction` 
ADD CONSTRAINT fk_transaction_buyer 
FOREIGN KEY (buyerId) REFERENCES `buyer`(buyerId);

-- 交易关联到卖家
ALTER TABLE `transaction` 
ADD CONSTRAINT fk_transaction_seller 
FOREIGN KEY (sellerId) REFERENCES `seller`(sellerId);

-- 评论关联到买家
ALTER TABLE `review` 
ADD CONSTRAINT fk_review_buyer 
FOREIGN KEY (buyerId) REFERENCES `buyer`(buyerId);

-- 评论关联到帖子
ALTER TABLE `review` 
ADD CONSTRAINT fk_review_post 
FOREIGN KEY (postId) REFERENCES `post`(postId);

-- 评论关联到订单
ALTER TABLE `review` 
ADD CONSTRAINT fk_review_order 
FOREIGN KEY (orderId) REFERENCES `order`(orderId);

-- 会话关联到卖家
ALTER TABLE `chatSession` 
ADD CONSTRAINT fk_chatSession_seller 
FOREIGN KEY (sellerId) REFERENCES `seller`(sellerId);

-- 会话关联到买家
ALTER TABLE `chatSession` 
ADD CONSTRAINT fk_chatSession_buyer 
FOREIGN KEY (buyerId) REFERENCES `buyer`(buyerId);

-- 会话关联到帖子
ALTER TABLE `chatSession` 
ADD CONSTRAINT fk_chatSession_post 
FOREIGN KEY (postId) REFERENCES `post`(postId);
