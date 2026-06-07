package com.nwe.recipely.ui.cook

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.Step
import com.nwe.recipely.timer.CookTimer
import com.nwe.recipely.timer.TimerService
import com.nwe.recipely.ui.theme.Fraunces
import com.nwe.recipely.ui.theme.Honey
import com.nwe.recipely.ui.theme.Moss
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CookModeScreen(
    recipeId: Long,
    onExit: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as RecipelyApp).container
    val vm: CookModeViewModel = viewModel(
        factory = viewModelFactory { initializer { CookModeViewModel(container.repository, recipeId) } }
    )
    val steps by vm.steps.collectAsState()
    val finished by vm.finished.collectAsState()
    val recipe by vm.recipe.collectAsState()
    val recipeName = recipe?.recipe?.name ?: ""

    // Keep the screen awake while cooking.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (steps.isEmpty()) {
            // No steps to cook (e.g. opened directly) — leave cleanly.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.recipe_not_found))
            }
        } else if (finished) {
            CompletionScreen(
                recipeName = recipeName,
                stepCount = steps.size,
                onDone = onExit,
                onReview = vm::restart,
            )
        } else {
            CookPager(
                recipeName = recipeName,
                steps = steps,
                onClose = onExit,
                onFinish = vm::finish,
            )
        }
    }
}

@Composable
private fun CookPager(
    recipeName: String,
    steps: List<Step>,
    onClose: () -> Unit,
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopBar(recipeName = recipeName, onClose = onClose)
        ProgressBar(current = pagerState.currentPage, total = steps.size)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            StepCard(step = steps[page], number = page + 1, total = steps.size)
        }
        NavBar(
            isFirst = pagerState.currentPage == 0,
            isLast = pagerState.currentPage == steps.lastIndex,
            onPrev = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
            onNext = {
                if (pagerState.currentPage == steps.lastIndex) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
        )
    }
}

@Composable
private fun TopBar(recipeName: String, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cook_close),
                tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = recipeName,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Text(
                text = "👁 " + stringResource(R.string.cook_keep_awake),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = Moss,
            )
        }
        Spacer(Modifier.size(36.dp)) // balances the close button for centring
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ProgressBar(current: Int, total: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        for (i in 0 until total) {
            val color = when {
                i < current -> Moss
                i == current -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun StepCard(step: Step, number: Int, total: Int) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))
            .padding(22.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.cook_step_of, number, total).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (step.text.isNotBlank()) {
            Text(
                text = step.text,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 11.dp),
            )
        }
        if (step.imageUri != null) {
            AsyncImage(
                model = File(step.imageUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
        }
        val seconds = step.text.let { parseTimerSeconds(it) }
        if (seconds != null) {
            TimerPill(stepNumber = number, totalSeconds = seconds, modifier = Modifier.padding(top = 18.dp))
        }
    }
}

@Composable
private fun TimerPill(stepNumber: Int, totalSeconds: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val timer by CookTimer.state.collectAsState()
    val mine = timer?.takeIf { it.stepNumber == stepNumber }
    val running = mine?.running == true
    val remaining = mine?.remainingSeconds ?: totalSeconds

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* started regardless; notification simply suppressed if denied */ }

    fun onTap() {
        if (mine == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            TimerService.start(context, stepNumber, totalSeconds)
        } else {
            TimerService.toggle(context)
        }
    }

    val cs = MaterialTheme.colorScheme
    val bg = if (running) cs.secondary else cs.primary
    val fg = if (running) cs.onSecondary else cs.onPrimary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .clickable { onTap() }
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(if (running) cs.surface else Honey),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (running) "❚❚" else "▶", fontSize = 10.sp,
                color = if (running) cs.secondary else cs.primary)
        }
        Column {
            Text(
                text = if (mine != null) TimerService.formatTime(remaining)
                       else stringResource(R.string.cook_timer_idle, TimerService.formatTime(totalSeconds)),
                color = fg, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
            if (running) {
                Text(stringResource(R.string.cook_timer_pause_hint), color = fg.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun NavBar(isFirst: Boolean, isLast: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                .then(if (isFirst) Modifier else Modifier.clickable(onClick = onPrev)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cook_previous),
                tint = if (isFirst) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val nextBg = if (isLast) Moss else MaterialTheme.colorScheme.secondary
        Box(
            Modifier
                .weight(1f)
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(nextBg)
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isLast) stringResource(R.string.cook_finish) else stringResource(R.string.cook_next) + " ›",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun CompletionScreen(recipeName: String, stepCount: Int, onDone: () -> Unit, onReview: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(104.dp).clip(CircleShape).background(Moss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
        }
        Text(
            text = stringResource(R.string.cook_done_title),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 26.dp),
        )
        Text(
            text = stringResource(R.string.cook_done_subtitle, recipeName, stepCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.height(34.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.secondary)
                .clickable(onClick = onDone),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.cook_finish), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 11.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                .clickable(onClick = onReview),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.cook_done_review), color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold)
        }
    }
}
