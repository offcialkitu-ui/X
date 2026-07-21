

package iad1tya.echo.music.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.screens.OptionStats

import androidx.compose.ui.graphics.Brush
import iad1tya.echo.music.ui.theme.LocalPurpleTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.drawBehind
import iad1tya.echo.music.utils.rememberPreference

@Composable
fun <E> ChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val isPurple = LocalPurpleTheme.current
    val partyMode by rememberPreference(iad1tya.echo.music.constants.PartyModeKey, defaultValue = false)
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .horizontalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Spacer(Modifier.width(12.dp))

        chips.forEach { (value, label) ->
            val isSelected = currentValue == value
            val isPartyChip = partyMode && label.equals("Party", ignoreCase = true)

            
            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 20.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "corner_radius"
            )

            FilterChip(
                label = { 
                    Text(
                        text = label,
                        color = if ((isSelected && isPurple) || isPartyChip) Color.White else MaterialTheme.colorScheme.onSurface
                    ) 
                },
                selected = isSelected,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = containerColor,
                    selectedContainerColor = if (isPurple || isPartyChip) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
                ),
                onClick = { onValueUpdate(value) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                            tint = if (isPurple || isPartyChip) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(cornerRadius),
                border = if (isPartyChip) BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFFFF00FF), Color(0xFF00FFFF)))) else null,
                modifier = Modifier
                    .animateContentSize()
                    .let { 
                        if (isSelected && isPurple) {
                            it.background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF8B5CF6),
                                        Color(0xFF7C3AED)
                                    )
                                ),
                                shape = RoundedCornerShape(cornerRadius)
                            )
                        } else it
                    }
            )

            Spacer(Modifier.width(8.dp))
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun <Int> ChoiceChipsRow(
    chips: List<Pair<Int, String>>,
    options: List<Pair<OptionStats, String>>,
    selectedOption: OptionStats,
    onSelectionChange: (OptionStats) -> Unit,
    currentValue: Int,
    onValueUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    var expandIconDegree by remember { mutableFloatStateOf(0f) }
    val rotationAnimation by animateFloatAsState(
        targetValue = expandIconDegree,
        animationSpec = tween(durationMillis = 400),
        label = "rotation",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        ) {
            Spacer(Modifier.width(12.dp))
            Box(contentAlignment = Alignment.Center) {
                FilterChip(
                    selected = false,
                    modifier = Modifier
                        .padding(horizontal = 4.dp),
                    onClick = {
                        menuExpanded = !menuExpanded
                        expandIconDegree -= 180
                    },
                    label = {
                        Text(
                            text = when (selectedOption) {
                                OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                                OptionStats.MONTHS -> stringResource(id = R.string.months)
                                OptionStats.YEARS -> stringResource(id = R.string.years)
                                OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.expand_more),
                            contentDescription = null,
                            modifier = Modifier
                                .graphicsLayer(rotationZ = rotationAnimation)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = containerColor,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                        expandIconDegree += 180
                    },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.second) },
                            onClick = {
                                onSelectionChange(option.first)
                                expandIconDegree += 180
                                menuExpanded = false
                            },
                        )
                    }
                }
            }

            Box(
                Modifier
                    .height(FilterChipDefaults.Height)
                    .padding(horizontal = 4.dp)
                    .align(Alignment.CenterVertically)
            ) {
                VerticalDivider()
            }

            chips.forEach { (value, label) ->
                val isSelected = currentValue == value

                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 20.dp else 8.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "corner_radius"
                )

                FilterChip(
                    label = { Text(label) },
                    selected = isSelected,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = containerColor,
                    ),
                    onClick = { onValueUpdate(value) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(cornerRadius),
                    border = null,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                )
            }
        }
    }
}
