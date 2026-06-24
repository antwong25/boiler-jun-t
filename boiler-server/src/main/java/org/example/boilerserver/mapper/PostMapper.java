package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.boilerpojo.PostEntity;

@Mapper
public interface PostMapper {
    PostEntity getByPostId(String postId);

    int updateStatus(@Param("postId") String postId, @Param("status") String status);
}
