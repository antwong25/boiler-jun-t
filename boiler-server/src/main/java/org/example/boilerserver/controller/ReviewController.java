package org.example.boilerserver.controller;

import org.example.boilercommon.Result;
import org.example.boilerpojo.ReviewCreateDTO;
import org.example.boilerpojo.ReviewVO;
import org.example.boilerpojo.SellerRatingVO;
import org.example.boilerserver.service.ReviewService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public Result<ReviewVO> submitReview(@RequestBody ReviewCreateDTO dto) {
        return Result.success(reviewService.submitReview(dto));
    }

    @GetMapping("/post/{postId}")
    public Result<org.example.boilercommon.PageResult<ReviewVO>> listByPost(
            @PathVariable String postId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return Result.success(reviewService.listByPost(postId, pageNum, pageSize, sortField, sortOrder));
    }

    @GetMapping("/order/{orderId}")
    public Result<org.example.boilercommon.PageResult<ReviewVO>> listByOrder(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return Result.success(reviewService.listByOrder(orderId, pageNum, pageSize, sortField, sortOrder));
    }

    @GetMapping("/user/{userId}")
    public Result<org.example.boilercommon.PageResult<ReviewVO>> listReceivedByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return Result.success(reviewService.listReceivedByUser(userId, pageNum, pageSize, sortField, sortOrder));
    }

    @GetMapping("/seller/{sellerId}/rating")
    public Result<SellerRatingVO> getSellerRating(@PathVariable String sellerId) {
        return Result.success(reviewService.getSellerRating(sellerId));
    }
}
