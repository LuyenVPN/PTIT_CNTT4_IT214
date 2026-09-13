package com.example.transactionservice.service;

import com.example.transactionservice.dto.AccountResponse;
import com.example.transactionservice.dto.AmountRequest;
import com.example.transactionservice.dto.TransferRequest;
import com.example.transactionservice.entity.Transaction;
import com.example.transactionservice.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private static final String ACCOUNT_SERVICE =
            "http://account-service/api/accounts/";

    private final RestTemplate restTemplate;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            RestTemplate restTemplate,
            TransactionRepository transactionRepository) {
        this.restTemplate = restTemplate;
        this.transactionRepository = transactionRepository;
    }

    public Transaction transfer(TransferRequest request) {

        Transaction transaction = buildTransaction(request);

        try {
            validateRequest(request);

            // 1. kiểm tra tài khoản nguồn
            AccountResponse fromAccount = restTemplate.getForObject(
                    ACCOUNT_SERVICE + request.getFromAccountNumber(),
                    AccountResponse.class
            );

            // 2. kiểm tra tài khoản đích
            restTemplate.getForObject(
                    ACCOUNT_SERVICE + request.getToAccountNumber(),
                    AccountResponse.class
            );

            // 3. kiểm tra số dư trước khi debit
            if (fromAccount == null || fromAccount.getBalance() == null) {
                throw new IllegalStateException(
                        "Không đọc được số dư tài khoản nguồn"
                );
            }

            if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
                throw new IllegalArgumentException(
                        "Tài khoản nguồn không đủ số dư"
                );
            }

            AmountRequest amountRequest = new AmountRequest();
            amountRequest.setAmount(request.getAmount());

            // 4. trừ tiền tài khoản nguồn
            restTemplate.put(
                    ACCOUNT_SERVICE
                            + request.getFromAccountNumber()
                            + "/debit",
                    amountRequest
            );

            try {
                // 5. cộng tiền tài khoản đích
                restTemplate.put(
                        ACCOUNT_SERVICE
                                + request.getToAccountNumber()
                                + "/credit",
                        amountRequest
                );
            } catch (Exception creditException) {
                // Best-effort compensation: nếu credit thất bại,
                // cố gắng hoàn lại tiền cho tài khoản nguồn.
                try {
                    restTemplate.put(
                            ACCOUNT_SERVICE
                                    + request.getFromAccountNumber()
                                    + "/credit",
                            amountRequest
                    );
                } catch (Exception compensationException) {
                    throw new IllegalStateException(
                            "Credit tài khoản đích thất bại và không thể hoàn tiền tài khoản nguồn: "
                                    + compensationException.getMessage()
                    );
                }

                throw new IllegalStateException(
                        "Credit tài khoản đích thất bại: "
                                + creditException.getMessage()
                );
            }

            transaction.setStatus("SUCCESS");
            transaction.setMessage("Chuyển tiền thành công");

        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transaction.setMessage(resolveErrorMessage(e));
        }

        return transactionRepository.save(transaction);
    }

    public java.util.List<Transaction> getAll() {
        return transactionRepository.findAll();
    }

    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy giao dịch: " + id
                        )
                );
    }

    private void validateRequest(TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request không được null");
        }

        if (request.getFromAccountNumber() == null
                || request.getFromAccountNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Tài khoản nguồn không được để trống"
            );
        }

        if (request.getToAccountNumber() == null
                || request.getToAccountNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Tài khoản đích không được để trống"
            );
        }

        if (request.getFromAccountNumber()
                .equals(request.getToAccountNumber())) {
            throw new IllegalArgumentException(
                    "Tài khoản nguồn và tài khoản đích phải khác nhau"
            );
        }

        BigDecimal amount = request.getAmount();

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Số tiền phải lớn hơn 0"
            );
        }
    }

    private Transaction buildTransaction(TransferRequest request) {
        Transaction transaction = new Transaction();

        if (request != null) {
            transaction.setFromAccountNumber(
                    request.getFromAccountNumber()
            );
            transaction.setToAccountNumber(
                    request.getToAccountNumber()
            );
            transaction.setAmount(request.getAmount());
            transaction.setDescription(request.getDescription());
        }

        return transaction;
    }

    private String resolveErrorMessage(Exception e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return e.getClass().getSimpleName();
        }

        return e.getMessage();
    }
}
