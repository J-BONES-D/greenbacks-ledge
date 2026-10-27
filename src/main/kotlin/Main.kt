fun main() {
    println("================================")
    println("        GREENBACKS LEDGE")
    println("================================")
    println()

    println("Welcome to Greenbacks Ledge!")
    println()

    val accountManager = AccountManager()

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
                    accountManager.displayAccounts()
                    println()
                    println("MANAGE ACCOUNTS")
                    println("1. Add Account")
                    println("2. View Account Ledge")
                    println("3. Deactivate Account")
                    println("4. Return to Main Menu")
                    print("Choose an option: ")

                    when (readln()) {
                        "1" -> accountManager.createAccount()
                        "2" -> accountManager.selectAccountForLedge()
                        "3" -> accountManager.removeAccount()
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