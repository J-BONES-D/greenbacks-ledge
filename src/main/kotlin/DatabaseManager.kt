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
                opening_balance_cents INTEGER NOT NULL
            )
        """.trimIndent()

        connection.createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    fun saveAccount(account: Account) {
        val sql = """
        INSERT INTO accounts (name, type, opening_balance_cents)
        VALUES (?, ?, ?)
    """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, account.name)
            statement.setString(2, account.type.name)
            statement.setLong(3, account.openingBalanceCents)

            statement.executeUpdate()
        }
    }
}