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
        for (account in accounts) {
            println(
                "${account.name} - ${account.type} - " +
                        centsToDollars(account.openingBalanceCents)
            )
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

        addAccount(account)
        databaseManager.saveAccount(account)

        println()
        println("Account added successfully!")
        println("${account.name} - ${account.type} - ${centsToDollars(account.openingBalanceCents)}")
    }
}