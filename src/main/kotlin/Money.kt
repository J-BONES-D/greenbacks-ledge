import java.math.BigDecimal
import java.math.RoundingMode

fun dollarsToCents(amount: String): Long {
    val cleanedAmount = amount.trim().removePrefix("$").trim()

    val decimalAmount = BigDecimal(cleanedAmount)
        .setScale(2, RoundingMode.UNNECESSARY)

    return decimalAmount
        .movePointRight(2)
        .longValueExact()
}

fun centsToDollars(cents: Long): String {
    val amount = BigDecimal.valueOf(cents, 2)

    return "$" + amount.toPlainString()
}