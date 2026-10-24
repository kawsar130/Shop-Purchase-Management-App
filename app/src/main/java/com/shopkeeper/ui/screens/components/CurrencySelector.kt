package com.shopkeeper.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shopkeeper.ui.theme.BlueLight
import com.shopkeeper.ui.theme.BluePrimary
import com.shopkeeper.ui.theme.PureWhite
import com.shopkeeper.ui.theme.Slate100
import com.shopkeeper.ui.theme.Slate200
import com.shopkeeper.ui.theme.Slate500
import com.shopkeeper.ui.theme.Slate700
import com.shopkeeper.ui.theme.Slate900

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String,
    val country: String = ""
)

val POPULAR_CURRENCIES = listOf("$", "৳", "₹", "€", "£", "AED", "SAR")

val ALL_WORLD_CURRENCIES = listOf(
    // Americas
    CurrencyInfo("USD", "$", "US Dollar", "United States"),
    CurrencyInfo("CAD", "C$", "Canadian Dollar", "Canada"),
    CurrencyInfo("MXN", "Mex$", "Mexican Peso", "Mexico"),
    CurrencyInfo("BRL", "R$", "Brazilian Real", "Brazil"),
    CurrencyInfo("ARS", "$", "Argentine Peso", "Argentina"),
    CurrencyInfo("COP", "$", "Colombian Peso", "Colombia"),
    CurrencyInfo("PEN", "S/.", "Peruvian Sol", "Peru"),
    CurrencyInfo("CLP", "$", "Chilean Peso", "Chile"),

    // South Asia
    CurrencyInfo("BDT", "৳", "Bangladeshi Taka", "Bangladesh"),
    CurrencyInfo("INR", "₹", "Indian Rupee", "India"),
    CurrencyInfo("PKR", "₨", "Pakistani Rupee", "Pakistan"),
    CurrencyInfo("LKR", "රු", "Sri Lankan Rupee", "Sri Lanka"),
    CurrencyInfo("NPR", "रू", "Nepalese Rupee", "Nepal"),

    // Middle East
    CurrencyInfo("AED", "AED", "UAE Dirham", "United Arab Emirates Dubai"),
    CurrencyInfo("SAR", "SAR", "Saudi Riyal", "Saudi Arabia"),
    CurrencyInfo("QAR", "QAR", "Qatari Riyal", "Qatar"),
    CurrencyInfo("KWD", "KD", "Kuwaiti Dinar", "Kuwait"),
    CurrencyInfo("OMR", "OMR", "Omani Rial", "Oman"),
    CurrencyInfo("BHD", "BD", "Bahraini Dinar", "Bahrain"),
    CurrencyInfo("JOD", "JD", "Jordanian Dinar", "Jordan"),
    CurrencyInfo("IQD", "IQD", "Iraqi Dinar", "Iraq"),
    CurrencyInfo("ILS", "₪", "Israeli Shekel", "Israel"),

    // Europe
    CurrencyInfo("EUR", "€", "Euro", "European Union Germany France Italy Spain"),
    CurrencyInfo("GBP", "£", "British Pound", "United Kingdom UK London"),
    CurrencyInfo("CHF", "CHF", "Swiss Franc", "Switzerland"),
    CurrencyInfo("PLN", "zł", "Polish Zloty", "Poland"),
    CurrencyInfo("SEK", "kr", "Swedish Krona", "Sweden"),
    CurrencyInfo("NOK", "kr", "Norwegian Krone", "Norway"),
    CurrencyInfo("DKK", "kr", "Danish Krone", "Denmark"),
    CurrencyInfo("TRY", "₺", "Turkish Lira", "Turkey"),
    CurrencyInfo("RUB", "₽", "Russian Ruble", "Russia"),
    CurrencyInfo("HUF", "Ft", "Hungarian Forint", "Hungary"),
    CurrencyInfo("CZK", "Kč", "Czech Koruna", "Czech Republic"),
    CurrencyInfo("RON", "lei", "Romanian Leu", "Romania"),

    // Southeast & East Asia
    CurrencyInfo("SGD", "S$", "Singapore Dollar", "Singapore"),
    CurrencyInfo("MYR", "RM", "Malaysian Ringgit", "Malaysia"),
    CurrencyInfo("IDR", "Rp", "Indonesian Rupiah", "Indonesia"),
    CurrencyInfo("PHP", "₱", "Philippine Peso", "Philippines"),
    CurrencyInfo("THB", "฿", "Thai Baht", "Thailand"),
    CurrencyInfo("VND", "₫", "Vietnamese Dong", "Vietnam"),
    CurrencyInfo("JPY", "¥", "Japanese Yen", "Japan"),
    CurrencyInfo("CNY", "¥", "Chinese Yuan", "China"),
    CurrencyInfo("KRW", "₩", "South Korean Won", "South Korea"),
    CurrencyInfo("HKD", "HK$", "Hong Kong Dollar", "Hong Kong"),
    CurrencyInfo("TWD", "NT$", "New Taiwan Dollar", "Taiwan"),

    // Africa
    CurrencyInfo("NGN", "₦", "Nigerian Naira", "Nigeria"),
    CurrencyInfo("KES", "KSh", "Kenyan Shilling", "Kenya"),
    CurrencyInfo("GHS", "GH₵", "Ghanaian Cedi", "Ghana"),
    CurrencyInfo("ZAR", "R", "South African Rand", "South Africa"),
    CurrencyInfo("EGP", "E£", "Egyptian Pound", "Egypt"),
    CurrencyInfo("MAD", "MAD", "Moroccan Dirham", "Morocco"),
    CurrencyInfo("TZS", "TSh", "Tanzanian Shilling", "Tanzania"),
    CurrencyInfo("UGX", "USh", "Ugandan Shilling", "Uganda"),
    CurrencyInfo("ETB", "Br", "Ethiopian Birr", "Ethiopia"),
    CurrencyInfo("XOF", "CFA", "West African CFA Franc", "Ivory Coast Senegal"),
    CurrencyInfo("XAF", "FCFA", "Central African CFA Franc", "Cameroon"),

    // Oceania
    CurrencyInfo("AUD", "A$", "Australian Dollar", "Australia"),
    CurrencyInfo("NZD", "NZ$", "New Zealand Dollar", "New Zealand")
)

/**
 * Reusable Currency Selector with 2-row wrapped popular chips (100% visible, no scrolling needed)
 * and a wide, high-contrast searchable all-currencies dialog with custom symbol input.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencySelector(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPickerModal by remember { mutableStateOf(false) }

    val chips = remember(selectedCurrency) {
        if (selectedCurrency in POPULAR_CURRENCIES) {
            POPULAR_CURRENCIES
        } else {
            POPULAR_CURRENCIES + selectedCurrency
        }
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { curr ->
            val isSelected = curr == selectedCurrency
            FilterChip(
                selected = isSelected,
                onClick = { onCurrencySelected(curr) },
                label = { Text(curr, fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BluePrimary,
                    selectedLabelColor = PureWhite
                )
            )
        }

        // "+ More..." chip - always visible on screen without scrolling
        FilterChip(
            selected = false,
            onClick = { showPickerModal = true },
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("More...", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Slate100,
                labelColor = Slate700
            )
        )
    }

    if (showPickerModal) {
        CurrencyPickerDialog(
            currentCurrency = selectedCurrency,
            onDismiss = { showPickerModal = false },
            onSelectCurrency = {
                onCurrencySelected(it)
                showPickerModal = false
            }
        )
    }
}

@Composable
fun CurrencyPickerDialog(
    currentCurrency: String,
    onDismiss: () -> Unit,
    onSelectCurrency: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customSymbolInput by remember { mutableStateOf("") }

    val filteredCurrencies = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            ALL_WORLD_CURRENCIES
        } else {
            val q = searchQuery.trim().lowercase()
            ALL_WORLD_CURRENCIES.filter {
                it.code.lowercase().contains(q) ||
                it.symbol.lowercase().contains(q) ||
                it.name.lowercase().contains(q) ||
                it.country.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Title & Close Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = BluePrimary
                        )
                        Text(
                            text = "Select Shop Currency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Custom Currency Input with Dark Readable Slate900 Text
                Text(
                    text = "Type Custom Symbol / Code:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customSymbolInput,
                        onValueChange = { customSymbolInput = it },
                        placeholder = {
                            Text(
                                text = "e.g. Tk, Rs, KSh",
                                fontSize = 13.sp,
                                color = Slate500,
                                maxLines = 1
                            )
                        },
                        singleLine = true,
                        maxLines = 1,
                        textStyle = TextStyle(
                            color = Slate900,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite,
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Slate200,
                            cursorColor = BluePrimary
                        )
                    )
                    Button(
                        onClick = {
                            val trimmed = customSymbolInput.trim()
                            if (trimmed.isNotEmpty()) {
                                onSelectCurrency(trimmed)
                            }
                        },
                        enabled = customSymbolInput.trim().isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BluePrimary,
                            contentColor = PureWhite,
                            disabledContainerColor = Slate100,
                            disabledContentColor = Slate500
                        )
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Slate200)
                Spacer(modifier = Modifier.height(12.dp))

                // 2. Search Field - strictly single line with dark text
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search country, currency or code...",
                            fontSize = 13.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Slate500)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Slate500)
                            }
                        }
                    },
                    singleLine = true,
                    maxLines = 1,
                    textStyle = TextStyle(
                        color = Slate900,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite,
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Slate200,
                        cursorColor = BluePrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Scrollable list of currencies
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCurrencies, key = { it.code }) { curr ->
                        val isSelected = curr.symbol == currentCurrency || curr.code == currentCurrency
                        Surface(
                            onClick = { onSelectCurrency(curr.symbol) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BlueLight else PureWhite,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) BluePrimary else Slate100),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = curr.symbol,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) PureWhite else Slate900
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "${curr.name} (${curr.code})",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (curr.country.isNotEmpty()) {
                                            Text(
                                                text = curr.country,
                                                fontSize = 11.sp,
                                                color = Slate500,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Cancel Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = Slate700,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
