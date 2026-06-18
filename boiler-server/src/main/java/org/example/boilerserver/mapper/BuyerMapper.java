package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.boilerpojo.BuyerEntity;

@Mapper
public interface BuyerMapper {
    BuyerEntity getByUserId(String userId);

    BuyerEntity getByBuyerId(String buyerId);

    int insert(BuyerEntity buyerEntity);
}
