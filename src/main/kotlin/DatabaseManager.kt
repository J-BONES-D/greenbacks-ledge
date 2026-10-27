import java.sql.Connection
import java.sql.DriverManager

class DatabaseManager {

    private val connection: Connection =
        DriverManager.getConnection("jdbc:sqlite:greenback-harvester.db")

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
}