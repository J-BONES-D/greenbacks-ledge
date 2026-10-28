fun main() {
    println("================================")
    println("        GREENBACKS LEDGE")
    println("================================")

    val accountManager = AccountManager()

    var running = true

    while (running) {
        println()
        println("MAIN MENU")
        println("1. Manage Accounts")
        println("2. Exit")
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
                    println("2. View Account Ledger")
                    println("3. Deactivate Account")
                    println("4. Reactivate Account")
                    println("5. Return to Main Menu")
                    print("Choose an option: ")

                    when (readln()) {
                        "1" -> accountManager.createAccount()
                        "2" -> accountManager.selectAccountForLedge()
                        "3" -> accountManager.removeAccount()
                        "4" -> accountManager.reactivateAccount()
                        "5" -> managingAccounts = false
                        else -> println("Invalid selection.")
                    }
                }
            }

            "2" -> {
                println("Goodbye!")
                running = false
            }

            else -> println("Invalid selection.")
        }
    }
}