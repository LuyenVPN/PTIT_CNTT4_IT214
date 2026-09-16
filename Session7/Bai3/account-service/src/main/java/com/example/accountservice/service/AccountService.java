package com.example.accountservice.service;

import com.example.accountservice.entity.Account;
import com.example.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> getAll() {
        return accountRepository.findAll();
    }

    public Account create(Account account) {
        if (account.getAccountNumber() == null ||
                account.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Số tài khoản không được để trống"
            );
        }

        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Số dư không được âm"
            );
        }

        if (accountRepository
                .findByAccountNumber(account.getAccountNumber())
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Tài khoản đã tồn tại: "
                            + account.getAccountNumber()
            );
        }

        return accountRepository.save(account);
    }

    public Account getByAccountNumber(String accountNumber) {
        return accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Tài khoản không tồn tại: "
                                        + accountNumber
                        )
                );
    }

    public BigDecimal getBalance(String accountNumber) {
        return getByAccountNumber(accountNumber).getBalance();
    }

    public Account debit(
            String accountNumber,
            BigDecimal amount) {

        validateAmount(amount);

        Account account = getByAccountNumber(accountNumber);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Tài khoản không đủ số dư"
            );
        }

        account.setBalance(
                account.getBalance().subtract(amount)
        );

        return accountRepository.save(account);
    }

    public Account credit(
            String accountNumber,
            BigDecimal amount) {

        validateAmount(amount);

        Account account = getByAccountNumber(accountNumber);

        account.setBalance(
                account.getBalance().add(amount)
        );

        return accountRepository.save(account);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Số tiền phải lớn hơn 0"
            );
        }
    }
}
