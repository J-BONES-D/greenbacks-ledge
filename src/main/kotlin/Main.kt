fun main() {
    println("HELLO FROM GREENBACK HARVESTER")
    val databaseManager = DatabaseManager()
    databaseManager.createAccountsTable()

    println("Database and accounts table are ready!")

    println("================================")
    println("        GREENBACKS LEDGE")
    println("================================")
    println()

    println("Welcome to Greenbacks Ledge!")
    println()

    val accountManager = AccountManager()

    val checking = Account(
        name = "My Checking",
        type = AccountType.CHECKING,
        openingBalanceCents = 50000L
    )

    accountManager.addAccount(checking)

    println("Test account added successfully!")

    var running = true

    while (running) {
        println()
        println("MAIN MENU")
        println("1. Manage Accounts")
        println("2. View Transactions")
        println("3. Enter a Transaction")
        println("4. Exit")
        println()

        print("Choose an option: ")

        when (readln()) {
            "1" -> {
                var managingAccounts = true

                while (managingAccounts) {
                    println()
                    println("MANAGE ACCOUNTS")
                    println("1. Add Account")
                    println("2. View Accounts")
                    println("3. Remove Account")
                    println("4. Return to Main Menu")
                    print("Choose an option: ")

                    when (readln()) {
                        "1" -> accountManager.createAccount()
                        "2" -> accountManager.displayAccounts()
                        "3" -> println("Remove account selected.")
                        "4" -> managingAccounts = false
                        else -> println("Invalid selection.")
                    }
                }
            }

            "2" -> println("Transaction register selected.")
            "3" -> println("Enter transaction selected.")
            "4" -> {
                println("Goodbye!")
                running = false
            }

            else -> println("Invalid selection.")
        }
    }
}