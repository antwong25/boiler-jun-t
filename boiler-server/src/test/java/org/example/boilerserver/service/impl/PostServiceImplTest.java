package org.example.boilerserver.service.impl;

import org.example.boilercommon.PageResult;
import org.example.boilerpojo.AdminPostQueryDTO;
import org.example.boilerpojo.BoilerEntity;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.PostPageQueryDTO;
import org.example.boilerpojo.PostVO;
import org.example.boilerserver.mapper.BoilerMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.service.PostEmbeddingService;
import org.example.constant.PostConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private BoilerMapper boilerMapper;
    @Mock
    private SellerMapper sellerMapper;
    @Mock
    private PostEmbeddingService postEmbeddingService;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    void listPublishedPosts_returnsPagedRecords() {
        PostPageQueryDTO dto = new PostPageQueryDTO();
        dto.setPageNum(2);
        dto.setPageSize(3);
        dto.setSortField("price");
        dto.setSortOrder("asc");

        PostEntity post = buildPostEntity();
        BoilerEntity boiler = buildBoilerEntity();

        when(postMapper.countPublishedPosts()).thenReturn(7L);
        when(postMapper.listPublishedPosts(3, 3, "price", "asc")).thenReturn(List.of(post));
        when(boilerMapper.getByBoilerId("boiler001")).thenReturn(boiler);

        PageResult<PostVO> result = postService.listPublishedPosts(dto);

        assertEquals(7L, result.getTotal());
        assertEquals(2, result.getPageNum());
        assertEquals(3, result.getPageSize());
        assertEquals(3, result.getTotalPages());
        assertEquals(1, result.getRecords().size());
        assertEquals("post001", result.getRecords().get(0).getPostId());
        verify(postMapper).listPublishedPosts(3, 3, "price", "asc");
    }

    @Test
    void filterPublishedPosts_normalizesConditionsAndReturnsPagedRecords() {
        PostPageQueryDTO dto = new PostPageQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(5);
        dto.setCity("guangzhou");
        dto.setBoilerType("steam boiler");
        dto.setBrand("Test");
        dto.setFuelType("Gas");

        PostEntity post = buildPostEntity();
        BoilerEntity boiler = buildBoilerEntity();

        when(postMapper.countPublishedPostsByFilter(argThat(query ->
                "GUANGZHOU".equals(query.getCity())
                        && PostConstant.BOILER_TYPE_STEAM.equals(query.getBoilerType())
                        && "Test".equals(query.getBrand())
                        && "Gas".equals(query.getFuelType())
                        && query.getOffset() == 0
        ))).thenReturn(1L);
        when(postMapper.listPublishedPostsByFilter(argThat(query ->
                "GUANGZHOU".equals(query.getCity())
                        && PostConstant.BOILER_TYPE_STEAM.equals(query.getBoilerType())
                        && "Test".equals(query.getBrand())
                        && "Gas".equals(query.getFuelType())
                        && query.getOffset() == 0
        ))).thenReturn(List.of(post));
        when(boilerMapper.getByBoilerId("boiler001")).thenReturn(boiler);

        PageResult<PostVO> result = postService.filterPublishedPosts(dto);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("GUANGZHOU", result.getRecords().get(0).getCity());
    }

    @Test
    void filterPublishedPosts_requiresAtLeastOneCondition() {
        PostPageQueryDTO dto = new PostPageQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.filterPublishedPosts(dto)
        );

        assertEquals("至少需要提供一个筛选条件", ex.getMessage());
    }

    @Test
    void delistPost_success() {
        PostEntity post = buildPostEntity();
        when(postMapper.getByPostId("post001")).thenReturn(post);

        postService.delistPost("post001", "seller001");

        verify(postMapper).updateStatus("post001", PostConstant.STATUS_DELISTED);
    }

    @Test
    void delistPost_notOwner_throwsException() {
        PostEntity post = buildPostEntity();
        when(postMapper.getByPostId("post001")).thenReturn(post);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.delistPost("post001", "otherSeller")
        );

        assertEquals("仅发帖卖家本人可以操作该帖子", ex.getMessage());
    }

    @Test
    void delistPost_alreadyDelisted_throwsException() {
        PostEntity post = buildPostEntity();
        post.setStatus(PostConstant.STATUS_DELISTED);
        when(postMapper.getByPostId("post001")).thenReturn(post);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.delistPost("post001", "seller001")
        );

        assertEquals("该帖子已经下架", ex.getMessage());
    }

    @Test
    void banPost_success() {
        PostEntity post = buildPostEntity();
        when(postMapper.getByPostId("post001")).thenReturn(post);

        postService.banPost("post001", "admin001");

        verify(postMapper).updateStatus("post001", PostConstant.STATUS_BANNED);
    }

    @Test
    void banPost_emptyAdminId_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.banPost("post001", "")
        );

        assertEquals("管理员ID不能为空", ex.getMessage());
    }

    @Test
    void banPost_alreadyBanned_throwsException() {
        PostEntity post = buildPostEntity();
        post.setStatus(PostConstant.STATUS_BANNED);
        when(postMapper.getByPostId("post001")).thenReturn(post);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.banPost("post001", "admin001")
        );

        assertEquals("该帖子已经被封禁", ex.getMessage());
    }

    @Test
    void adminListPosts_normalizesConditionsAndReturnsPagedRecords() {
        AdminPostQueryDTO dto = new AdminPostQueryDTO();
        dto.setPageNum(2);
        dto.setPageSize(5);
        dto.setSellerId(" seller001 ");
        dto.setStatus("sold");
        dto.setCity("guangzhou");
        dto.setBoilerType("steam boiler");
        dto.setBrand("Test");
        dto.setFuelType("Gas");
        dto.setSortField("status");
        dto.setSortOrder("asc");

        PostEntity post = buildPostEntity();
        BoilerEntity boiler = buildBoilerEntity();

        when(postMapper.countAdminPosts(argThat(query ->
                "seller001".equals(query.getSellerId())
                        && PostConstant.STATUS_SOLD.equals(query.getStatus())
                        && "GUANGZHOU".equals(query.getCity())
                        && PostConstant.BOILER_TYPE_STEAM.equals(query.getBoilerType())
                        && "Test".equals(query.getBrand())
                        && "Gas".equals(query.getFuelType())
                        && "status".equals(query.getSortField())
                        && "asc".equals(query.getSortOrder())
                        && query.getOffset() == 5
        ))).thenReturn(1L);
        when(postMapper.listAdminPosts(argThat(query ->
                "seller001".equals(query.getSellerId())
                        && PostConstant.STATUS_SOLD.equals(query.getStatus())
                        && "GUANGZHOU".equals(query.getCity())
                        && PostConstant.BOILER_TYPE_STEAM.equals(query.getBoilerType())
                        && "Test".equals(query.getBrand())
                        && "Gas".equals(query.getFuelType())
                        && "status".equals(query.getSortField())
                        && "asc".equals(query.getSortOrder())
                        && query.getOffset() == 5
        ))).thenReturn(List.of(post));
        when(boilerMapper.getByBoilerId("boiler001")).thenReturn(boiler);

        PageResult<PostVO> result = postService.adminListPosts(dto);

        assertEquals(1L, result.getTotal());
        assertEquals(2, result.getPageNum());
        assertEquals(5, result.getPageSize());
        assertEquals(1, result.getRecords().size());
        assertEquals("post001", result.getRecords().get(0).getPostId());
    }

    @Test
    void adminListPosts_invalidStatus_throwsException() {
        AdminPostQueryDTO dto = new AdminPostQueryDTO();
        dto.setStatus("unknown");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> postService.adminListPosts(dto)
        );

        assertEquals("不支持的帖子状态", ex.getMessage());
    }

    @Test
    void adminGetPostDetail_returnsPostWithoutIncrementingViewCount() {
        PostEntity post = buildPostEntity();
        BoilerEntity boiler = buildBoilerEntity();
        when(postMapper.getByPostId("post001")).thenReturn(post);
        when(boilerMapper.getByBoilerId("boiler001")).thenReturn(boiler);

        PostVO result = postService.adminGetPostDetail("post001");

        assertEquals("post001", result.getPostId());
        assertEquals("boiler001", result.getBoilerDetail().getBoilerId());
        verify(postMapper, never()).incrementViewCount(anyString());
    }

    private PostEntity buildPostEntity() {
        PostEntity post = new PostEntity();
        post.setPostId("post001");
        post.setSellerId("seller001");
        post.setTitle("Test Boiler");
        post.setPrice(new BigDecimal("50000"));
        post.setDescription("desc");
        post.setStatus(PostConstant.STATUS_PUBLISHED);
        post.setPublishTime(LocalDate.now());
        post.setUpdateTime(LocalDate.now());
        post.setViewCount(10);
        post.setMediaFiles("a.jpg");
        post.setAiValuationRange("45000-55000");
        post.setCity("GUANGZHOU");
        post.setBoilerId("boiler001");
        return post;
    }

    private BoilerEntity buildBoilerEntity() {
        BoilerEntity boiler = new BoilerEntity();
        boiler.setBoilerId("boiler001");
        boiler.setModel("WNS");
        boiler.setBrand("Test Brand");
        boiler.setBoilerType(PostConstant.BOILER_TYPE_STEAM);
        boiler.setTonnage(new BigDecimal("10"));
        boiler.setFuelType("Gas");
        boiler.setWorkingPressure(new BigDecimal("1.25"));
        boiler.setNoxEmissions(new BigDecimal("30"));
        boiler.setFootprintArea(new BigDecimal("20"));
        boiler.setManufactureYear(LocalDate.of(2023, 1, 1));
        boiler.setEvaporationCapacity(new BigDecimal("10"));
        boiler.setRatedThermalPower(new BigDecimal("700"));
        boiler.setThermalEfficiency(new BigDecimal("95"));
        boiler.setEquipmentCondition("GOOD");
        boiler.setUsageHours(new BigDecimal("5000"));
        boiler.setTestReport("report.pdf");
        boiler.setRatedOutletWaterTemperature(new BigDecimal("95"));
        return boiler;
    }
}
