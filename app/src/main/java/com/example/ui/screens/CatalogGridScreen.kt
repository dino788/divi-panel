package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CategoryEntity
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.data.local.PlayerSettings
import com.example.ui.components.DiviSearchField
import com.example.ui.components.DiviTopScreenBar
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.ParentalPinDialog
import com.example.ui.theme.DiviCyan
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.VIRTUAL_CAT_ALL
import com.example.ui.viewmodel.VIRTUAL_CAT_FAVORITES
import com.example.ui.viewmodel.VIRTUAL_CAT_RECENT

@Composable
fun CatalogGridScreen(
    mediaType: MediaType,
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    items: List<MediaItemEntity>,
    searchQuery: String,
    sortOption: SortOption,
    settings: PlayerSettings,
    unlockedCategories: Set<String>,
    onBack: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onUnlockCategory: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onItemClick: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit
) {
    val isSeries = mediaType == MediaType.SERIES
    val title = stringResource(if (isSeries) R.string.nav_series else R.string.nav_movies)
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var pendingLockedCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    fun handleCategoryTap(catId: String, categoryObj: CategoryEntity?) {
        if (categoryObj != null &&
            categoryObj.isLocked &&
            settings.parentalPin.isNotBlank() &&
            !unlockedCategories.contains(categoryObj.categoryId)
        ) {
            pendingLockedCategory = categoryObj
        } else {
            onSelectCategory(catId)
        }
    }

    if (pendingLockedCategory != null) {
        val cat = pendingLockedCategory!!
        ParentalPinDialog(
            expectedPin = settings.parentalPin,
            categoryName = cat.categoryName,
            onDismiss = { pendingLockedCategory = null },
            onUnlocked = {
                onUnlockCategory(cat.categoryId)
                onSelectCategory(cat.categoryId)
                pendingLockedCategory = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        DiviTopScreenBar(
            title = title,
            subtitle = "${items.size} ${if (isSeries) "shows" else "titles"} • ${sortOption.label}",
            onBack = onBack,
            actions = {
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = DiviCyan
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        containerColor = ObsidianSurfaceElevated
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (sortOption == option) DiviCyan else TextPrimary
                                    )
                                },
                                onClick = {
                                    onSortChange(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DiviSearchField(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = if (isSeries) "Search TV series..." else "Search movies...",
                testTag = if (isSeries) "series_search_input" else "movies_search_input"
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                item {
                    CategoryChipItem(
                        label = "All",
                        selected = selectedCategoryId == VIRTUAL_CAT_ALL,
                        locked = false,
                        onClick = { handleCategoryTap(VIRTUAL_CAT_ALL, null) }
                    )
                }
                item {
                    CategoryChipItem(
                        label = "★ Favorites",
                        selected = selectedCategoryId == VIRTUAL_CAT_FAVORITES,
                        locked = false,
                        onClick = { handleCategoryTap(VIRTUAL_CAT_FAVORITES, null) }
                    )
                }
                item {
                    CategoryChipItem(
                        label = "Recent",
                        selected = selectedCategoryId == VIRTUAL_CAT_RECENT,
                        locked = false,
                        onClick = { handleCategoryTap(VIRTUAL_CAT_RECENT, null) }
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    val showLock = cat.isLocked && settings.parentalPin.isNotBlank() && !unlockedCategories.contains(cat.categoryId)
                    CategoryChipItem(
                        label = if (cat.itemCount > 0) "${cat.categoryName} (${cat.itemCount})" else cat.categoryName,
                        selected = selectedCategoryId == cat.categoryId,
                        locked = showLock,
                        onClick = { handleCategoryTap(cat.categoryId, cat) }
                    )
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isSeries) Icons.Filled.Tv else Icons.Filled.Movie,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(46.dp)
                        )
                        Text(
                            text = stringResource(if (isSeries) R.string.empty_series_title else R.string.empty_movies_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.empty_channels_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 136.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 22.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { item ->
                        MediaPosterCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onToggleFavorite = { onToggleFavorite(item) }
                        )
                    }
                }
            }
        }
    }
}
