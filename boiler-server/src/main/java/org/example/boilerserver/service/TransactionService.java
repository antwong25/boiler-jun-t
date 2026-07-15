package org.example.boilerserver.service;

import org.example.boilercommon.PageResult;
import org.example.boilerpojo.TransactionCreateDTO;
import org.example.boilerpojo.TransactionQueryDTO;
import org.example.boilerpojo.TransactionVO;

public interface TransactionService {

    TransactionVO createTransaction(TransactionCreateDTO dto);

    TransactionVO getTransaction(String transactionId);

    PageResult<TransactionVO> listMyTransactions(TransactionQueryDTO dto);

    TransactionVO cancelTransaction(String transactionId, String userId);

    TransactionVO completeTransaction(String transactionId, String userId);
}
