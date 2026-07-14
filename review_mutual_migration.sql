USE `boiler-jun`;

-- 将旧版 review.buyerId 升级为通用的 reviewerId / revieweeId 结构
ALTER TABLE `review` DROP FOREIGN KEY fk_review_buyer;
ALTER TABLE `review` CHANGE COLUMN `buyerId` `reviewerId` VARCHAR(255) NOT NULL;
ALTER TABLE `review` ADD COLUMN `revieweeId` VARCHAR(255) NULL AFTER `reviewerId`;

-- 历史数据默认按“买家评价卖家”回填被评价人
UPDATE `review` r
INNER JOIN `order` o ON r.orderId = o.orderId
INNER JOIN `transaction` t ON o.transactionId = t.transactionId
SET r.revieweeId = t.sellerId
WHERE r.revieweeId IS NULL;

ALTER TABLE `review` MODIFY COLUMN `revieweeId` VARCHAR(255) NOT NULL;
ALTER TABLE `review` ADD CONSTRAINT fk_review_reviewer FOREIGN KEY (`reviewerId`) REFERENCES `user`(`userId`);
ALTER TABLE `review` ADD CONSTRAINT fk_review_reviewee FOREIGN KEY (`revieweeId`) REFERENCES `user`(`userId`);
ALTER TABLE `review` ADD UNIQUE KEY uk_review_order_reviewer (`orderId`, `reviewerId`);
ALTER TABLE `review` ADD KEY idx_review_post (`postId`);
ALTER TABLE `review` ADD KEY idx_review_reviewee (`revieweeId`);
