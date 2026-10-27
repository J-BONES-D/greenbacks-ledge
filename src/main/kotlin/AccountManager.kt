class AccountManager {
    private val databaseManager = DatabaseManager()

    private val accounts = mutableListOf<Account>()

    init {
        databaseManager.createAccountsTable()
        accounts.addAll(databaseManager.loadAccounts())
    }

    fun addAccount(account: Account) {
        accounts.add(account)
    }

    fun getAccounts(): List<Account> {
        return accounts
    }

    fun displayAccounts() {
        if (accounts.isEmpty()) {
            println("No accounts have been created.")
            return
        }

        println()
        println("ACCOUNTS")

        for (account in accounts) {
            val status = if (account.isActive) "ACTIVE" else "INACTIVE"

            println(
                "${account.name} - ${account.type} - " +
                        "${centsToDollars(account.openingBalanceCents)} - $status"
            )
        }
    }

    fun selectAccountForLedge() {
        if (accounts.isEmpty()) {
            println("No accounts have been created.")
            return
        }

        println()
        println("SELECT AN ACCOUNT")

        for ((index, account) in accounts.withIndex()) {
            val status = if (account.isActive) "ACTIVE" else "INACTIVE"

            println(
                "${index + 1}. ${account.name} (Acct ID: ${account.id}) - " +
                        "${account.type} - $status"
            )
        }

        print("Enter account selection (or 0 to return): ")

        val selection = readln().toIntOrNull()

        if (selection == null || selection < 0) {
            println("Please enter a valid selection.")
            return
        }

        if (selection == 0) {
            println("Returning to account menu.")
            return
        }

        val account = accounts.getOrNull(selection - 1)

        if (account == null) {
            println("Account selection not found.")
            return
        }

        if (!account.isActive) {
            println("This account is inactive. Its history has been preserved.")
            return
        }

        openAccountLedge(account)
    }

    fun openAccountLedge(account: Account) {
        var viewingLedge = true

        while (viewingLedge) {
            println()
            println("================================")
            println("       ${account.name.uppercase()} LEDGE")
            println("================================")
            println("1. View Transactions")
            println("2. Enter a Transaction")
            println("3. Edit a Transaction")
            println("4. Account Details")
            println("5. Return to Account Selection")
            print("Choose an option: ")

            when (readln()) {
                "1" -> println("View transactions selected.")
                "2" -> println("Enter a transaction selected.")
                "3" -> println("Edit a transaction selected.")
                "4" -> {
                    println()
                    println("ACCOUNT DETAILS")
                    println("Name: ${account.name}")
                    println("Type: ${account.type}")
                    println(
                        "Opening Balance: " +
                                centsToDollars(account.openingBalanceCents)
                    )
                    println("Status: ${if (account.isActive) "ACTIVE" else "INACTIVE"}")
                }
                "5" -> viewingLedge = false
                else -> println("Invalid selection.")
            }
        }
    }

    fun createAccount() {
        println()
        println("CREATE ACCOUNT")

        print("Enter account name: ")
        val name = readln().trim()

        if (name.isBlank()) {
            println("Account name cannot be blank.")
            return
        }

        println()
        println("Select account type:")
        println("1. Checking")
        println("2. Savings")
        println("3. Cash")
        println("4. Digital Wallet")

        print("Choose account type: ")
        val typeChoice = readln()

        val type = when (typeChoice) {
            "1" -> AccountType.CHECKING
            "2" -> AccountType.SAVINGS
            "3" -> AccountType.CASH
            "4" -> AccountType.DIGITAL_WALLET
            else -> {
                println("Invalid account type.")
                return
            }
        }

        print("Enter opening balance (example: 125.50): ")
        val balanceInput = readln()

        val balanceCents = try {
            dollarsToCents(balanceInput)
        } catch (e: NumberFormatException) {
            println("Invalid balance. Please enter a number.")
            return
        } catch (e: ArithmeticException) {
            println("Enter an amount with no more than two decimal places.")
            return
        }

        val account = Account(
            name = name,
            type = type,
            openingBalanceCents = balanceCents
        )

        val accountId = databaseManager.saveAccount(account)

        val savedAccount = account.copy(id = accountId)

        addAccount(savedAccount)

        println()
        println("Account added successfully!")
        println("${account.name} - ${account.type} - ${centsToDollars(account.openingBalanceCents)}")
    }

    fun deactivateAccount(accountId: Int) {
        databaseManager.deactivateAccount(accountId)

        val accountIndex = accounts.indexOfFirst { it.id == accountId }

        if (accountIndex != -1) {
            accounts[accountIndex] =
                accounts[accountIndex].copy(isActive = false)
        }
    }

    fun removeAccount() {
        val activeAccounts = accounts.filter { it.isActive }

        if (activeAccounts.isEmpty()) {
            println("There are no active accounts to deactivate.")
            return
        }

        println()
        println("SELECT ACCOUNT TO DEACTIVATE")

        for (account in activeAccounts) {
            println("${account.id}. ${account.name} - ${account.type}")
        }

        print("Enter account ID (or 0 to cancel): ")

        val accountId = readln().toIntOrNull()

        if (accountId == null) {
            println("Please enter a valid account ID.")
            return
        }

        if (accountId == 0) {
            println("Deactivation cancelled.")
            return
        }

        val account = activeAccounts.find { it.id == accountId }

        if (account == null) {
            println("Account ID not found.")
            return
        }

        print("Deactivate '${account.name}'? (y/n): ")

        when (readln().trim().lowercase()) {
            "y", "yes" -> {
                deactivateAccount(accountId)
                println("Account deactivated. Its history has been preserved.")
            }
            "n", "no" -> println("Deactivation cancelled.")
            else -> println("Invalid response. Deactivation cancelled.")
        }
    }
}