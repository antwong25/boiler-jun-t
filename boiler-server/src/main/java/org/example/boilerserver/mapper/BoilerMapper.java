package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.boilerpojo.BoilerEntity;

@Mapper
public interface BoilerMapper {
    BoilerEntity getByBoilerId(String boilerId);

    int insert(BoilerEntity boilerEntity);

    int update(BoilerEntity boilerEntity);

    int deleteByBoilerId(String boilerId);
}
