package com.lyldz.ab.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.lyldz.ab.AbApp
import com.lyldz.ab.EXTRA_CUSTOM
import com.lyldz.ab.MainActivity
import com.lyldz.ab.R
import com.lyldz.ab.data.AbRepository
import com.lyldz.ab.data.SOURCE_WIDGET
import com.lyldz.ab.ui.fmtMl

private val BG = ColorProvider(Color(0xFFF8FAFC), Color(0xFFF8FAFC))
private val TEXT = ColorProvider(Color(0xFF0F203C), Color(0xFF0F203C))
private val PRIMARY = ColorProvider(Color(0xFF88D9E6), Color(0xFF88D9E6))
private val ON_PRIMARY = ColorProvider(Color(0xFF0F203C), Color(0xFF0F203C))
private val TRACK = ColorProvider(Color(0x4488D9E6), Color(0x4488D9E6))

private val ML_KEY = ActionParameters.Key<Int>("ml")

class AbWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = (context.applicationContext as AbApp).repo.todayNow()
        provideContent { Content(context, today) }
    }
}

class AbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AbWidget()
}

/** Uygulamayı açmadan kayıt: ortak yol AbApp.addWater (bildirimi kapatır, alarmı ve widget'ı tazeler). */
class AddWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val ml = parameters[ML_KEY] ?: return
        (context.applicationContext as AbApp).addWater(ml, SOURCE_WIDGET)
    }
}

@Composable
private fun Content(ctx: Context, t: AbRepository.Today) {
    val open = actionStartActivity(Intent(ctx, MainActivity::class.java))
    val custom = actionStartActivity(Intent(ctx, MainActivity::class.java).putExtra(EXTRA_CUSTOM, true))
    val amounts = listOf(t.settings.cupMl, 500, 1000).distinct()

    Column(
        GlanceModifier.fillMaxSize().background(BG).cornerRadius(20.dp).padding(14.dp).clickable(open),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${fmtMl(t.drankMl)} / ${fmtMl(t.goalMl)} ml",
            style = TextStyle(color = TEXT, fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.height(8.dp))
        LinearProgressIndicator(
            progress = (t.drankMl.toFloat() / t.goalMl).coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth(),
            color = PRIMARY,
            backgroundColor = TRACK,
        )
        Spacer(GlanceModifier.height(10.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            amounts.forEach { ml ->
                Pill("+${fmtMl(ml)}", GlanceModifier.defaultWeight(), actionRunCallback<AddWaterAction>(actionParametersOf(ML_KEY to ml)))
                Spacer(GlanceModifier.width(6.dp))
            }
            Pill(ctx.getString(R.string.add_custom), GlanceModifier.defaultWeight(), custom)
        }
    }
}

@Composable
private fun Pill(label: String, modifier: GlanceModifier, action: Action) {
    Text(
        label,
        modifier = modifier.background(PRIMARY).cornerRadius(16.dp).padding(vertical = 12.dp).clickable(action),
        style = TextStyle(color = ON_PRIMARY, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        maxLines = 1,
    )
}
