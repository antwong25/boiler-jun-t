package org.example.boilerserver.controller;

import org.example.boilercommon.Result;
import org.example.boilerpojo.TransactionCreateDTO;
import org.example.boilerpojo.TransactionQueryDTO;
import org.example.boilerpojo.TransactionVO;
import org.example.boilerserver.service.TransactionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public Result<TransactionVO> createTransaction(@RequestBody TransactionCreateDTO dto) {
        return Result.success(transactionService.createTransaction(dto));
    }

    @GetMapping("/{transactionId}")
    public Result<TransactionVO> getTransaction(@PathVariable String transactionId) {
        return Result.success(transactionService.getTransaction(transactionId));
    }

    @GetMapping("/my")
    public Result<org.example.boilercommon.PageResult<TransactionVO>> listMyTransactions(TransactionQueryDTO dto) {
        return Result.success(transactionService.listMyTransactions(dto));
    }

    @PutMapping("/{transactionId}/cancel")
    public Result<TransactionVO> cancelTransaction(@PathVariable String transactionId,
                                                    @RequestParam String userId) {
        return Result.success(transactionService.cancelTransaction(transactionId, userId));
    }

    @PutMapping("/{transactionId}/complete")
    public Result<TransactionVO> completeTransaction(@PathVariable String transactionId,
                                                     @RequestParam String userId) {
        return Result.success(transactionService.completeTransaction(transactionId, userId));
    }
}
