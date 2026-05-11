package com.spbu.projecttrack.rating.presentation.projectstats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts

private val StatsDropdownMenuShape = RoundedCornerShape(10.dp)
private val StatsDropdownMenuItemShape = RoundedCornerShape(7.dp)
private val StatsDropdownMenuBorder = Color(0xFFE4E4E7)
private val StatsDropdownSelectedRow = AppColors.Color3.copy(alpha = 0.08f)

@Composable
internal fun StatsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    maxHeight: Dp = 260.dp,
    offset: DpOffset = DpOffset(0.dp, 6.dp),
    selectedKey: String? = null,
    selectedLabel: String? = null,
    itemFontWeight: FontWeight = FontWeight.Normal,
    selectedItemFontWeight: FontWeight = FontWeight.SemiBold,
) {

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = StatsDropdownMenuShape,
        containerColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, StatsDropdownMenuBorder),
        modifier = modifier.then(
            if (width != null) {
                Modifier
                    .width(width)
                    .heightIn(max = maxHeight)
            } else {
                Modifier.heightIn(max = maxHeight)
            }
        ),
    ) {
        options.forEach { (key, value) ->
            val isSelected = when {
                selectedKey != null -> key == selectedKey
                selectedLabel != null -> value == selectedLabel
                else -> false
            }

            DropdownMenuItem(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clip(StatsDropdownMenuItemShape)
                    .background(if (isSelected) StatsDropdownSelectedRow else Color.Transparent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                text = {
                    Text(
                        text = value,
                        fontFamily = AppFonts.OpenSans,
                        fontWeight = if (isSelected) selectedItemFontWeight else itemFontWeight,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = if (isSelected) AppColors.Color3 else AppColors.Color2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp),
                    )
                },
                onClick = {
                    onDismissRequest()
                    onSelected(key)
                },
            )
        }
    }
}
