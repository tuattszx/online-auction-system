package auction.server.dao;

import auction.common.model.items.Transaction;

import java.util.List;

public interface TransactionDao {
    List<Transaction> getAll();
}
