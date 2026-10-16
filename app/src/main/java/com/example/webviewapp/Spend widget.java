package com.example.webviewapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SpendWidget extends AppWidgetProvider {

    static final String PREFS = "spend_widget";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) update(c, m, id);
    }

    // Also refresh when the date/time changes so a new day starts at zero
    @Override
    public void onReceive(Context c, Intent intent) {
        super.onReceive(c, intent);
        String a = intent.getAction();
        if (Intent.ACTION_DATE_CHANGED.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) {
            refreshAll(c);
        }
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, SpendWidget.class));
        for (int id : ids) update(c, m, id);
    }

    static void update(Context c, AppWidgetManager m, int id) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String saved = p.getString("date", "");

        String amount, sub;
        int state;
        if (saved.isEmpty()) {
            amount = "—";
            sub = "Open the app to start";
            state = -1;
        } else if (!saved.equals(today)) {
            amount = p.getString("zero", "0");          // new day, nothing logged yet
            sub = p.getString("zeroSub", "New day");
            state = -1;
        } else {
            amount = p.getString("amount", "—");
            sub = p.getString("sub", "");
            state = p.getInt("state", -1);
        }

        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_spend);
        v.setTextViewText(R.id.w_amount, amount);
        v.setTextViewText(R.id.w_sub, sub);
        int color = state == 2 ? 0xFFFF7A66 : state == 1 ? 0xFFE8B04A : state == 0 ? 0xFF4CC79A : 0xFFA9A394;
        v.setTextColor(R.id.w_sub, color);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        Intent open = new Intent(c, MainActivity.class);
        open.setAction("OPEN_APP");
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(c, 0, open, flags));

        Intent add = new Intent(c, MainActivity.class);
        add.setAction("QUICK_ADD");
        add.putExtra("quick_add", true);
        add.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.w_add, PendingIntent.getActivity(c, 1, add, flags));

        m.updateAppWidget(id, v);
    }
}
