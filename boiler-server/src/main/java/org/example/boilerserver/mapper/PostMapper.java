package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.boilerpojo.PostFilterSearchDTO;
import org.example.boilerpojo.PostEntity;

import java.util.List;

@Mapper
public interface PostMapper {
    PostEntity getByPostId(String postId);

    int insert(PostEntity postEntity);

    int update(PostEntity postEntity);

    int incrementViewCount(String postId);

    int deleteByPostId(String postId);

    int updateStatus(@Param("postId") String postId, @Param("status") String status);

    List<PostEntity> listByFilter(PostFilterSearchDTO dto);
}
