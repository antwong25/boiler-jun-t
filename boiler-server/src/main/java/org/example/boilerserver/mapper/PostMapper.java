package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.boilerpojo.PostFilterSearchDTO;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.PostPageQueryDTO;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostMapper {
    PostEntity getByPostId(String postId);

    int insert(PostEntity postEntity);

    int update(PostEntity postEntity);

    int incrementViewCount(String postId);

    int deleteByPostId(String postId);

    int updateStatus(@Param("postId") String postId, @Param("status") String status);

    List<PostEntity> listByFilter(PostFilterSearchDTO dto);

    long countPublishedPosts();

    List<PostEntity> listPublishedPosts(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    long countPublishedPostsByFilter(PostPageQueryDTO dto);

    List<PostEntity> listPublishedPostsByFilter(PostPageQueryDTO dto);

    List<Map<String, Object>> listAiValuationTrainingRows(@Param("limit") int limit);
}
