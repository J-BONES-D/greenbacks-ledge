import java.time.LocalDate

enum class TransactionType {
    OPENING_BALANCE,
    DEPOSIT,
    PAYMENT
}

data class Transaction(
    val accountId: Int,
    val type: TransactionType,
    val description: String,
    val amountCents: Long,
    val balanceCents: Long = 0L,
    val date: LocalDate = LocalDate.now(),
    val id: Int? = null
)