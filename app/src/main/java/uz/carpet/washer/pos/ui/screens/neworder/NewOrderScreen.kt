package uz.carpet.washer.pos.ui.screens.neworder

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.carpet.washer.pos.ui.screens.dashboard.formatMoney
import uz.carpet.washer.pos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderScreen(
    onBack: () -> Unit,
    onOrderSaved: () -> Unit,
    vm: NewOrderViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Buyurtma saqlanganda orqaga qayt
    LaunchedEffect(state.savedOrderId) {
        if (state.savedOrderId != null) onOrderSaved()
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearError() }
    }

    Scaffold(
        containerColor = BackgroundApp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditMode) "Buyurtmani Tahrirlash" else "Yangi Buyurtma",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Orqaga") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mijoz ma'lumotlari
            SectionCard(title = "Mijoz Ma'lumotlari", icon = Icons.Rounded.Person) {
                PosTextField(
                    value = state.customerName,
                    onValueChange = vm::onNameChange,
                    label = "Mijoz ismi *",
                    icon = Icons.Rounded.Person
                )
                Spacer(Modifier.height(12.dp))
                PosTextField(
                    value = state.customerPhone,
                    onValueChange = vm::onPhoneChange,
                    label = "Telefon raqami *",
                    icon = Icons.Rounded.Phone,
                    keyboardType = KeyboardType.Phone,
                    placeholder = "+998 90 000 00 00"
                )
                Spacer(Modifier.height(12.dp))
                PosTextField(
                    value = state.customerAddress,
                    onValueChange = vm::onAddressChange,
                    label = "Manzil",
                    icon = Icons.Rounded.LocationOn
                )
            }

            // Gilamlar
            SectionCard(title = "Gilamlar (${state.carpets.size} ta)", icon = Icons.Rounded.Layers) {
                state.carpets.forEachIndexed { index, carpet ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)
                    }
                    CarpetInputBlock(
                        index = index + 1,
                        carpet = carpet,
                        showRemove = state.carpets.size > 1,
                        onUpdate = { vm.updateCarpet(carpet.id, it) },
                        onRemove = { vm.removeCarpet(carpet.id) }
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = vm::addCarpet,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Yana Gilam Qo'shish")
                }
            }

            // Moliyaviy hisob
            SectionCard(title = "Hisob-Kitob", icon = Icons.Rounded.Payments) {
                SummaryRow("Jami m²:", "${"%.2f".format(state.totalArea)} m²")
                SummaryRow("Umumiy summa:", "${formatMoney(state.totalAmount)} so'm", isHighlight = true)
                Spacer(Modifier.height(12.dp))
                PosTextField(
                    value = state.advanceAmount,
                    onValueChange = vm::onAdvanceChange,
                    label = "Avans (so'm)",
                    icon = Icons.Rounded.Payments,
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.remaining > 0) Color(0xFFFFFBEB) else Color(0xFFECFDF5)
                    )
                ) {
                    SummaryRow(
                        "Qoldiq:",
                        "${formatMoney(state.remaining)} so'm",
                        isHighlight = true,
                        highlightColor = if (state.remaining > 0) Warning else Secondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Saqlash tugmasi
            Button(
                onClick = vm::saveOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = !state.isLoading && state.isValid
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.isEditMode) "O'zgarishlarni Saqlash" else "Saqlash va Chek Chiqarish",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun CarpetInputBlock(
    index: Int,
    carpet: CarpetInput,
    showRemove: Boolean,
    onUpdate: (CarpetInput) -> Unit,
    onRemove: () -> Unit
) {
    val types = listOf("Gilam", "Adyal", "Parda", "Yostiq", "Boshqa")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Buyum $index", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Primary)
        if (showRemove) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Danger, modifier = Modifier.size(20.dp))
            }
        }
    }
    // Type tanlash chiplari
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.lazy.items(types) { type ->
            FilterChip(
                selected = carpet.type == type,
                onClick = { onUpdate(carpet.copy(type = type)) },
                label = { Text(type, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryLight,
                    selectedLabelColor = Primary
                )
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PosTextField(
            modifier = Modifier.weight(1f),
            value = carpet.width,
            onValueChange = { onUpdate(carpet.copy(width = it)) },
            label = if (carpet.type == "Gilam") "Eni (m)" else "Soni / Eni",
            keyboardType = KeyboardType.Decimal
        )
        PosTextField(
            modifier = Modifier.weight(1f),
            value = carpet.length,
            onValueChange = { onUpdate(carpet.copy(length = it)) },
            label = if (carpet.type == "Gilam") "Bo'yi (m)" else "Uzunligi (ixtiyoriy)",
            keyboardType = KeyboardType.Decimal
        )
    }
    Spacer(Modifier.height(8.dp))
    PosTextField(
        value = carpet.pricePerSqm,
        onValueChange = { onUpdate(carpet.copy(pricePerSqm = it)) },
        label = if (carpet.type == "Gilam") "1 m² narxi (so'm)" else "1 dona/m² narxi (so'm)",
        icon = Icons.Rounded.AttachMoney,
        keyboardType = KeyboardType.Number
    )
    if (carpet.area > 0) {
        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = PrimaryLight) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val calcText = if (carpet.type == "Gilam" || carpet.lengthDouble > 0) {
                    "${"%.2f".format(carpet.widthDouble)} × ${"%.2f".format(carpet.lengthDouble)} = ${"%.2f".format(carpet.area)}"
                } else {
                    "${"%.2f".format(carpet.widthDouble)} dona/m"
                }
                Text(
                    calcText,
                    fontSize = 13.sp, color = Primary
                )
                Text(
                    "${formatMoney(carpet.total)} so'm",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = icon?.let { { Icon(it, null, tint = Primary, modifier = Modifier.size(18.dp)) } },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = TextSecondary, fontSize = 13.sp) }) else null,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Divider,
            unfocusedContainerColor = BackgroundApp,
            focusedContainerColor = CardSurface
        ),
        singleLine = true
    )
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    highlightColor: Color = Primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(
            value,
            fontSize = if (isHighlight) 16.sp else 14.sp,
            fontWeight = if (isHighlight) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (isHighlight) highlightColor else TextPrimary
        )
    }
}
