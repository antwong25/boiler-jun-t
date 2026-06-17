package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.boilerpojo.SellerEntity;

@Mapper
public interface SellerMapper {
    SellerEntity getByUserId(String userId);

    SellerEntity getBySellerId(String sellerId);

    int insert(SellerEntity sellerEntity);

    int update(SellerEntity sellerEntity);
}
