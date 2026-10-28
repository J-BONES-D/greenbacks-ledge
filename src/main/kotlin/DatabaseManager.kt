import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate

class DatabaseManager {

    private val connection: Connection =
        DriverManager.getConnection("jdbc:sqlite:greenback-harvester.db")

    init {
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
        }
    }

    fun createAccountsTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                opening_balance_cents INTEGER NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent()

        connection.createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    fun createTransactionsTable() {
        val sql = """
        CREATE TABLE IF NOT EXISTS transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            account_id INTEGER NOT NULL,
            balance_cents INTEGER NOT NULL DEFAULT 0,
            type TEXT NOT NULL,
            description TEXT NOT NULL,
            amount_cents INTEGER NOT NULL,
            transaction_date TEXT NOT NULL,

            FOREIGN KEY (account_id)
                REFERENCES accounts(id)
        )
    """.trimIndent()

        connection.createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    fun ensureTransactionBalanceColumn() {
        var columnExists = false

        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(transactions)").use { resultSet ->
                while (resultSet.next()) {
                    if (resultSet.getString("name") == "balance_cents") {
                        columnExists = true
                    }
                }
            }
        }

        if (!columnExists) {
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                ALTER TABLE transactions
                ADD COLUMN balance_cents INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
                )
            }

            // Reconstruct historical running balances in date order.
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                WITH calculated AS (
                    SELECT
                        id,
                        SUM(
                            CASE
                                WHEN type IN ('OPENING_BALANCE', 'DEPOSIT')
                                    THEN amount_cents
                                WHEN type = 'PAYMENT'
                                    THEN -amount_cents
                                ELSE 0
                            END
                        ) OVER (
                            PARTITION BY account_id
                            ORDER BY transaction_date, id
                            ROWS BETWEEN UNBOUNDED PRECEDING
                                     AND CURRENT ROW
                        ) AS running_balance
                    FROM transactions
                )
                UPDATE transactions
                SET balance_cents = (
                    SELECT running_balance
                    FROM calculated
                    WHERE calculated.id = transactions.id
                )
                """.trimIndent()
                )
            }
        }
    }

    fun saveAccount(account: Account): Int {
        val sql = """
        INSERT INTO accounts (name, type, opening_balance_cents, is_active)
        VALUES (?, ?, ?, ?)
    """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, account.name)
            statement.setString(2, account.type.name)
            statement.setLong(3, account.openingBalanceCents)
            statement.setInt(4, if (account.isActive) 1 else 0)

            statement.executeUpdate()
        }

        connection.createStatement().use { idStatement ->
            idStatement.executeQuery("SELECT last_insert_rowid()").use { resultSet ->
                if (resultSet.next()) {
                    return resultSet.getInt(1)
                } else {
                    error("Failed to retrieve the new account ID.")
                }
            }
        }
    }

    fun saveTransaction(transaction: Transaction) {
        val sql = """
        INSERT INTO transactions (
            account_id,
            type,
            description,
            amount_cents,
            balance_cents,
            transaction_date
        )
        VALUES (?, ?, ?, ?, ?, ?)
    """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, transaction.accountId)
            statement.setString(2, transaction.type.name)
            statement.setString(3, transaction.description)
            statement.setLong(4, transaction.amountCents)
            statement.setLong(5, transaction.balanceCents)
            statement.setString(6, transaction.date.toString())

            statement.executeUpdate()
        }
    }

    fun saveTransactionWithBalance(transaction: Transaction) {
        connection.autoCommit = false

        try {
            // Find the most recent transaction balance for this account.
            val balanceSql = """
            SELECT balance_cents
            FROM transactions
            WHERE account_id = ?
            ORDER BY transaction_date DESC, id DESC
            LIMIT 1
        """.trimIndent()

            val currentBalance = connection.prepareStatement(balanceSql).use { statement ->
                statement.setInt(1, transaction.accountId)

                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getLong("balance_cents")
                    } else {
                        error("No opening balance transaction was found.")
                    }
                }
            }

            // Calculate the new balance.
            val newBalance = when (transaction.type) {
                TransactionType.DEPOSIT ->
                    currentBalance + transaction.amountCents

                TransactionType.PAYMENT ->
                    currentBalance - transaction.amountCents

                TransactionType.OPENING_BALANCE ->
                    error("Opening balances must be saved when creating an account.")
            }

            // Save the transaction with its resulting balance.
            val transactionWithBalance = transaction.copy(
                balanceCents = newBalance
            )

            saveTransaction(transactionWithBalance)

            connection.commit()
        } catch (exception: Exception) {
            connection.rollback()
            throw exception
        } finally {
            connection.autoCommit = true
        }
    }

    fun saveAccountWithOpeningBalance(account: Account): Int {
        connection.autoCommit = false

        try {
            val accountId = saveAccount(account)

            val openingTransaction = Transaction(
                accountId = accountId,
                type = TransactionType.OPENING_BALANCE,
                description = "Opening Balance",
                amountCents = account.openingBalanceCents,
                balanceCents = account.openingBalanceCents
            )

            saveTransaction(openingTransaction)

            connection.commit()
            return accountId

        } catch (exception: Exception) {
            connection.rollback()
            throw exception

        } finally {
            connection.autoCommit = true
        }
    }

    fun deactivateAccount(accountId: Int) {
        val sql = """
        UPDATE accounts
        SET is_active = 0
        WHERE id = ?
    """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, accountId)
            statement.executeUpdate()
        }
    }

    fun reactivateAccount(accountId: Int) {
        val sql = """
        UPDATE accounts
        SET is_active = 1
        WHERE id = ?
    """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, accountId)
            statement.executeUpdate()
        }
    }

    fun loadAccounts(): List<Account> {
        val accounts = mutableListOf<Account>()

        val sql = """
        SELECT id, name, type, opening_balance_cents, is_active
        FROM accounts
    """.trimIndent()

        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { resultSet ->

                while (resultSet.next()) {
                    val account = Account(
                        name = resultSet.getString("name"),
                        type = AccountType.valueOf(
                            resultSet.getString("type")
                        ),
                        openingBalanceCents =
                            resultSet.getLong("opening_balance_cents"),
                        isActive = resultSet.getInt("is_active") == 1,
                        id = resultSet.getInt("id")
                    )

                    accounts.add(account)
                }
            }
        }

        return accounts
    }

    fun loadTransactions(accountId: Int): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        val sql = """
            SELECT id, account_id, type, description,
            amount_cents, balance_cents, transaction_date
            FROM transactions
            WHERE account_id = ?
            ORDER BY transaction_date, id
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, accountId)

            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val transaction = Transaction(
                        accountId = resultSet.getInt("account_id"),
                        type = TransactionType.valueOf(
                            resultSet.getString("type")
                        ),
                        description = resultSet.getString("description"),
                        amountCents = resultSet.getLong("amount_cents"),
                        balanceCents = resultSet.getLong("balance_cents"),
                        date = LocalDate.parse(
                            resultSet.getString("transaction_date")
                        ),
                        id = resultSet.getInt("id")
                    )

                    transactions.add(transaction)
                }
            }
        }

        return transactions
    }
}