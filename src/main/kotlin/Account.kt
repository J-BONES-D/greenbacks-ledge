enum class AccountType {
    CHECKING,
    SAVINGS,
    CREDIT_CARD,
    CASH,
    DIGITAL_WALLET
}

data class Account(
    val name: String,
    val type: AccountType,
    val openingBalanceCents: Long,
    val isActive: Boolean = true,
    val id: Int? = null
)