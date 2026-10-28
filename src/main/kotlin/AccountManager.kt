class AccountManager {
    private val databaseManager = DatabaseManager()

    private val accounts = mutableListOf<Account>()

    init {
        databaseManager.createAccountsTable()
        databaseManager.createTransactionsTable()
        databaseManager.ensureTransactionBalanceColumn()
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
            println("This account is inactive.")
            println("Its ledger is available in read-only mode.")
        }

        openAccountLedge(account)
    }

    fun calculateRunningBalances(
        transactions: List<Transaction>
    ): List<Long> {
        var runningBalance = 0L
        val balances = mutableListOf<Long>()

        for (transaction in transactions) {
            when (transaction.type) {
                TransactionType.OPENING_BALANCE,
                TransactionType.DEPOSIT -> {
                    runningBalance += transaction.amountCents
                }

                TransactionType.PAYMENT -> {
                    runningBalance -= transaction.amountCents
                }
            }

            balances.add(runningBalance)
        }

        return balances
    }

    fun enterTransaction(account: Account) {
        println()
        println("ENTER TRANSACTION")
        println("1. Deposit")
        println("2. Payment")
        print("Choose transaction type: ")

        val type = when (readln().trim()) {
            "1" -> TransactionType.DEPOSIT
            "2" -> TransactionType.PAYMENT
            else -> {
                println("Invalid transaction type.")
                return
            }
        }

        print("Description: ")
        val description = readln().trim()

        if (description.isBlank()) {
            println("Description cannot be blank.")
            return
        }

        print("Amount (e.g. 12.50): ")
        val amountInput = readln().trim()

        val amountCents = try {
            dollarsToCents(amountInput)
        } catch (exception: Exception) {
            println("Invalid amount. Enter dollars and cents, such as 12.50.")
            return
        }

        if (amountCents <= 0L) {
            println("Amount must be greater than zero.")
            return
        }

        val transaction = Transaction(
            accountId = account.id!!,
            type = type,
            description = description,
            amountCents = amountCents
        )

        try {
            DatabaseManager().saveTransactionWithBalance(transaction)
            println("Transaction saved successfully.")
        } catch (exception: Exception) {
            println("Could not save transaction: ${exception.message}")
        }
    }

    fun openAccountLedge(account: Account) {
        var viewingLedge = true

        while (viewingLedge) {
            println()
            println("================================")
            println("       ${account.name.uppercase()} LEDGE")
            println("================================")
            if (account.isActive) {
                println("Status: ACTIVE")
                println("1. View Transactions")
                println("2. Enter a Transaction")
                println("3. Edit a Transaction")
            } else {
                println("Status: INACTIVE - READ ONLY")
                println("1. View Transactions")
                println("2. Enter a Transaction (Unavailable)")
                println("3. Edit a Transaction (Unavailable)")
            }

            println("4. Account Details")
            println("5. Return to Account Selection")
            print("Choose an option: ")

            when (readln()) {
                "1" -> {
                    val databaseManager = DatabaseManager()
                    val transactions = databaseManager.loadTransactions(account.id!!)

                    println()
                    println("TRANSACTIONS FOR ${account.name.uppercase()}")

                    if (transactions.isEmpty()) {
                        println("No transactions found.")
                    } else {
                        println()
                        println("%-12s %-22s %12s %12s".format(
                            "DATE", "DESCRIPTION", "AMOUNT", "BALANCE"
                        ))
                        println("-".repeat(62))

                        for ((index, transaction) in transactions.withIndex()) {
                            val amount = when (transaction.type) {
                                TransactionType.PAYMENT ->
                                    "-" + centsToDollars(transaction.amountCents)

                                else -> centsToDollars(transaction.amountCents)
                            }

                            println(
                                "%-12s %-22s %12s %12s".format(
                                    transaction.date,
                                    transaction.description,
                                    amount,
                                    centsToDollars(transaction.balanceCents)
                                )
                            )
                        }
                    }
                }
                "2" -> {
                    if (account.isActive) {
                        enterTransaction(account)
                    } else {
                        println("This account is inactive. Transactions cannot be entered.")
                    }
                }
                "3" -> {
                    if (account.isActive) {
                        println("Transaction editing will be implemented next.")
                    } else {
                        println("This account is inactive. Transactions cannot be edited.")
                    }
                }
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

        val accountId = databaseManager.saveAccountWithOpeningBalance(account)

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

    fun reactivateAccount() {
        val inactiveAccounts = accounts.filter { !it.isActive }

        if (inactiveAccounts.isEmpty()) {
            println("There are no inactive accounts to reactivate.")
            return
        }

        println()
        println("SELECT ACCOUNT TO REACTIVATE")

        for (account in inactiveAccounts) {
            println("${account.id}. ${account.name} - ${account.type}")
        }

        print("Enter account ID (or 0 to cancel): ")

        val accountId = readln().toIntOrNull()

        if (accountId == null) {
            println("Please enter a valid account ID.")
            return
        }

        if (accountId == 0) {
            println("Reactivation cancelled.")
            return
        }

        val account = inactiveAccounts.find { it.id == accountId }

        if (account == null) {
            println("Account ID not found.")
            return
        }

        print("Reactivate '${account.name}'? (y/n): ")

        when (readln().trim().lowercase()) {
            "y", "yes" -> {
                databaseManager.reactivateAccount(accountId)

                val accountIndex = accounts.indexOfFirst {
                    it.id == accountId
                }

                if (accountIndex != -1) {
                    accounts[accountIndex] =
                        accounts[accountIndex].copy(isActive = true)
                }

                println("Account reactivated successfully!")
            }

            "n", "no" -> println("Reactivation cancelled.")

            else -> println("Invalid response. Reactivation cancelled.")
        }
    }
}