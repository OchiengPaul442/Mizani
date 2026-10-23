package com.mizani.money.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mizani.money.core.model.WalletType
import android.graphics.Color as AndroidColor

object MizaniIcons {

    fun category(key: String): ImageVector = when (key) {
        "home" -> Icons.Rounded.Home
        "food" -> Icons.Rounded.Restaurant
        "transport" -> Icons.Rounded.DirectionsBus
        "fuel" -> Icons.Rounded.LocalGasStation
        "phone" -> Icons.Rounded.Smartphone
        "fees" -> Icons.Rounded.ReceiptLong
        "bolt" -> Icons.Rounded.Bolt
        "school" -> Icons.Rounded.School
        "health" -> Icons.Rounded.Favorite
        "family" -> Icons.Rounded.FamilyRestroom
        "debt" -> Icons.Rounded.AccountBalance
        "savings" -> Icons.Rounded.Savings
        "giving" -> Icons.Rounded.VolunteerActivism
        "business" -> Icons.Rounded.BusinessCenter
        "personal" -> Icons.Rounded.Spa
        "entertainment" -> Icons.Rounded.Movie
        "income" -> Icons.Rounded.ArrowDownward
        "freelance" -> Icons.Rounded.Work
        else -> Icons.Rounded.Category
    }

    fun wallet(type: WalletType): ImageVector = when (type) {
        WalletType.CASH -> Icons.Rounded.Payments
        WalletType.MOBILE_MONEY -> Icons.Rounded.Smartphone
        WalletType.BANK -> Icons.Rounded.AccountBalance
        WalletType.CARD -> Icons.Rounded.CreditCard
        WalletType.SAVINGS -> Icons.Rounded.Savings
        WalletType.DEBT -> Icons.Rounded.Balance
        WalletType.OTHER -> Icons.Rounded.AccountBalanceWallet
    }

    fun walletIconKey(key: String, fallback: WalletType): ImageVector = when (key) {
        "wallet" -> wallet(fallback)
        "cash" -> Icons.Rounded.Payments
        "momo" -> Icons.Rounded.Smartphone
        "bank" -> Icons.Rounded.AccountBalance
        "card" -> Icons.Rounded.CreditCard
        "savings" -> Icons.Rounded.Savings
        "atm" -> Icons.Rounded.LocalAtm
        "goal" -> Icons.Rounded.Flag
        else -> wallet(fallback)
    }

    fun goal(icon: String): ImageVector = when (icon) {
        "school" -> Icons.Rounded.School
        "home" -> Icons.Rounded.Home
        "savings" -> Icons.Rounded.Savings
        "phone" -> Icons.Rounded.Smartphone
        "business" -> Icons.Rounded.BusinessCenter
        "health" -> Icons.Rounded.Favorite
        "travel" -> Icons.Rounded.Flag
        "gift" -> Icons.Rounded.Paid
        else -> Icons.Rounded.Flag
    }

    fun flowIcon(isIncome: Boolean): ImageVector =
        if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF6D6D6D)): Color = try {
    Color(AndroidColor.parseColor(hex))
} catch (_: Exception) {
    fallback
}
