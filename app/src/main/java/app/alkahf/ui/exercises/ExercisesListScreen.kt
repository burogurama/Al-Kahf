package app.alkahf.ui.exercises

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.data.ExerciseSession
import app.alkahf.ui.theme.AlkahfColors
import kotlinx.coroutines.launch

/**
 * The Exercises landing screen: a "New test" action plus the list of saved
 * sessions (the persisted tests). Creating a test is a separate screen
 * ([ExercisesSetupScreen]); from here the user resumes, reviews, or deletes.
 */
@Composable
fun ExercisesListScreen(
    controller: ExercisesController,
    onClose: () -> Unit,
    onNewTest: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sessions by controller.savedSessions.collectAsState()
    LaunchedEffect(Unit) { controller.loadSavedSessions() }

    Column(
        Modifier.fillMaxSize().background(AlkahfColors.Paper).statusBarsPadding().navigationBarsPadding(),
    ) {
        ExercisesTopBar(
            title = stringResource(R.string.ex_setup_title),
            subtitle = stringResource(R.string.ex_setup_subtitle),
            onClose = onClose,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                onClick = onNewTest,
                shape = RoundedCornerShape(15.dp),
                color = AlkahfColors.Accent,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) {
                Row(
                    Modifier.padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Add, null, tint = AlkahfColors.OnAccent, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.ex_new_test),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.OnAccent,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            if (sessions.isEmpty()) {
                Text(
                    text = stringResource(R.string.ex_list_empty),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 36.dp, start = 12.dp, end = 12.dp),
                )
            } else {
                SectionKicker(
                    text = stringResource(R.string.ex_recent_section),
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
                sessions.forEach { session ->
                    SavedSessionRow(
                        session = session,
                        onOpen = { onOpenSession(session.id) },
                        onDelete = { scope.launch { controller.deleteSession(session.id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedSessionRow(session: ExerciseSession, onOpen: () -> Unit, onDelete: () -> Unit) {
    val finished = session.isFinished
    val accent = if (finished) AlkahfColors.AccentDeep else AlkahfColors.GoldText
    val stamp = DateUtils.getRelativeTimeSpanString(
        session.finishedAt ?: session.createdAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 15.dp, top = 13.dp, end = 6.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ex_session_questions, session.total),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = if (finished) {
                        stringResource(R.string.ex_recent_done, session.correct, session.total)
                    } else {
                        stringResource(R.string.ex_recent_in_progress, session.answered, session.total)
                    },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = stamp,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFooter,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.ex_recent_delete),
                    tint = AlkahfColors.InkFaint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
