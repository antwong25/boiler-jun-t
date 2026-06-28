package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.boilerpojo.PostEntity;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostMapper {
    PostEntity getByPostId(String postId);

    int insert(PostEntity postEntity);

    int update(PostEntity postEntity);

    int incrementViewCount(String postId);

    int deleteByPostId(String postId);
    @MapKey("id")
    List<Map<String, Object>> listAiValuationTrainingRows(@Param("limit") int limit);
}
