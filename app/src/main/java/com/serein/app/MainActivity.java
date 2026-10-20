package com.serein.app;

import android.Manifest;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.HashMap;
import java.util.Map;

public final class MainActivity extends Activity {
    private SessionState state;
    private Palette colors;
    private LinearLayout root, body, player;
    private FrameLayout content;
    private TextView title, subtitle, timerText, sheetTimer;
    private LinearLayout timerButton;
    private Symbols.Icon playIcon;
    private Dialog mixSheet;
    private final LinearLayout[] navItems = new LinearLayout[3];
    private final Map<Integer, SoundCard> cards = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int page = 0, filter = 0;
    private long breathStart = 0;
    private BreathView breath;
    private boolean registered = false;
    private final Runnable refreshAudio = () -> {
        if (state.playing) startService(new Intent(this, PlaybackService.class).setAction(PlaybackService.REFRESH));
    };
    private final Runnable clock = new Runnable() {
        @Override public void run() {
            updatePlayer();
            if (breath != null && breathStart > 0) breath.invalidate();
            handler.postDelayed(this, 500);
        }
    };
    private final BroadcastReceiver changes = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            updatePlayer();
            for (SoundCard card : cards.values()) card.update();
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        state = SessionState.get(this);
        if (saved != null) { page = saved.getInt("page", 0); filter = saved.getInt("filter", 0); }
        build();
    }
    @Override protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(changes, new IntentFilter(PlaybackService.STATE), Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(changes, new IntentFilter(PlaybackService.STATE));
        registered = true;
        handler.post(clock);
        updatePlayer();
        SereinWidget.updateAll(this);
    }
    @Override protected void onStop() {
        super.onStop();
        handler.removeCallbacks(clock);
        handler.removeCallbacks(refreshAudio);
        if (registered) { unregisterReceiver(changes); registered = false; }
        breathStart = 0;
        if (mixSheet != null) mixSheet.dismiss();
        state.save();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("page", page);
        out.putInt("filter", filter);
    }
    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private TextView text(String s, float sp, int color, boolean medium) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(color);
        v.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        v.setIncludeFontPadding(false);
        return v;
    }
    private void oneLine(TextView v) { v.setSingleLine(true); v.setEllipsize(TextUtils.TruncateAt.END); }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }
    private void space(LinearLayout l, int height) { l.addView(new View(this), lp(1, height)); }
    private void surface(View v, int color, int radius, int stroke) { v.setBackground(Palette.shape(color, dp(radius), stroke)); }
    private void ripple(View v, int color, int radius, int stroke) {
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x226f8660), Palette.shape(color, dp(radius), stroke), Palette.shape(0xffdddddd, dp(radius), 0)));
    }
    private Symbols.Icon icon(String name, String description, int size, int color, Runnable click) {
        Symbols.Icon i = new Symbols.Icon(this, name, color);
        i.setContentDescription(description); i.setLayoutParams(lp(size, size));
        ripple(i, Color.TRANSPARENT, size / 2, 0); i.setOnClickListener(v -> click.run());
        return i;
    }
    private TextView button(String label, boolean filled, Runnable action) {
        TextView t = text(label, 14, filled ? colors.greenInk : colors.ink, true);
        t.setGravity(Gravity.CENTER); t.setMinHeight(dp(48)); t.setPadding(dp(18), dp(12), dp(18), dp(12));
        ripple(t, filled ? colors.green : colors.bg, 14, filled ? 0 : colors.line);
        t.setOnClickListener(v -> action.run());
        return t;
    }
    private void build() {
        colors = new Palette(state.dark);
        Window w = getWindow();
        w.setDecorFitsSystemWindows(false); w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT); w.setNavigationBarContrastEnforced(false);
        int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        w.getInsetsController().setSystemBarsAppearance(state.dark ? 0 : mask, mask);
        root = column(); root.setBackgroundColor(colors.bg); setContentView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            root.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        LinearLayout top = row();
        top.setPadding(dp(20), dp(10), dp(12), dp(8));
        top.setMinimumHeight(dp(70));
        TextView brand = text("Serein.", 29, colors.ink, true);
        android.text.SpannableString wordmark = new android.text.SpannableString("Serein.");
        wordmark.setSpan(new android.text.style.ForegroundColorSpan(colors.accent), 6, 7, 0);
        brand.setText(wordmark); brand.setLetterSpacing(-.045f); oneLine(brand);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(icon("more", "App options", 48, colors.muted, this::optionsDialog));
        root.addView(top, lp(-1, -2));
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        buildPlayer();
        buildNavigation();
        renderContent();
        root.requestApplyInsets();
    }
    private void buildNavigation() {
        LinearLayout navigation = row();
        navigation.setPadding(dp(12), dp(3), dp(12), dp(5));
        String[] labels = {"Home", "Mixes", "Breathe"};
        String[] symbols = {"home", "mix", "breathe"};
        for (int i = 0; i < 3; i++) {
            final int destination = i;
            LinearLayout item = column(); item.setGravity(Gravity.CENTER);
            item.setPadding(dp(6), dp(5), dp(6), dp(5)); item.setMinimumHeight(dp(60));
            Symbols.Icon mark = new Symbols.Icon(this, symbols[i], colors.muted);
            item.addView(mark, lp(42, 34));
            TextView label = text(labels[i], 11, colors.muted, false); label.setGravity(Gravity.CENTER);
            item.addView(label, lp(-1, -2));
            item.setContentDescription(labels[i]); item.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            item.setOnClickListener(v -> showPage(destination));
            ripple(item, Color.TRANSPARENT, 16, 0);
            navigation.addView(item, new LinearLayout.LayoutParams(0, -2, 1)); navItems[i] = item;
        }
        root.addView(navigation, lp(-1, -2));
        updateNavigation();
    }
    private void updateNavigation() {
        String[] symbols = {"home", "mix", "breathe"};
        for (int i = 0; i < 3; i++) {
            if (navItems[i] == null) continue;
            navItems[i].setSelected(page == i);
            ((Symbols.Icon)navItems[i].getChildAt(0)).symbol(symbols[i], page == i ? colors.green : colors.muted);
            ((TextView)navItems[i].getChildAt(1)).setTextColor(page == i ? colors.ink : colors.muted);
        }
    }
    private void showPage(int destination) {
        if (page == destination) return;
        page = destination; breathStart = 0;
        renderContent(); updateNavigation();
    }
    private void renderContent() {
        cards.clear(); breath = null; content.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false); scroll.setPadding(dp(20), dp(10), dp(20), dp(20));
        body = column(); scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        content.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        if (page == 0) home(); else if (page == 1) mixer(); else breathing();
    }
    private void heading(String name) {
        TextView label = text(name, 29, colors.ink, false); label.setLetterSpacing(-.035f);
        body.addView(label, lp(-1, -2)); space(body, 22);
    }
    private void home() {
        String[] names = {"Sleep", "Focus", "Unwind"}, symbols = {"moon", "sun", "sunset"};
        LinearLayout presets = row(); presets.setGravity(Gravity.TOP);
        for (int i = 0; i < 3; i++) {
            final int preset = i;
            LinearLayout tile = column(); tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(6), dp(17), dp(6), dp(15)); tile.setMinimumHeight(dp(124));
            ripple(tile, colors.surface, 18, colors.line);
            Symbols.Icon art = new Symbols.Icon(this, symbols[i], colors.green);
            tile.addView(art, lp(64, 55));
            TextView name = text(names[i], 14, colors.ink, true); name.setGravity(Gravity.CENTER);
            name.setMaxLines(2); tile.addView(name, lp(-1, -2));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
            if (i > 0) p.leftMargin = dp(8);
            presets.addView(tile, p);
            tile.setContentDescription("Play " + names[i]);
            tile.setOnClickListener(v -> { state.preset(preset); startPlayback(); updatePlayer(); });
        }
        body.addView(presets, lp(-1, -2));
        space(body, 32);
        LinearLayout section = row();
        TextView label = text("Your mixes", 19, colors.ink, true);
        section.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        if (!state.mixes.isEmpty()) section.addView(icon("plus", "Create a mix", 48, colors.green, () -> showPage(1)));
        section.setMinimumHeight(dp(48));
        body.addView(section, lp(-1, -2)); space(body, 8);
        for (SessionState.SavedMix mix : state.mixes) savedMix(mix);
        if (state.mixes.isEmpty()) {
            TextView add = button("Create mix", false, () -> showPage(1));
            body.addView(add, lp(-1, -2));
        }
    }
    private void savedMix(SessionState.SavedMix mix) {
        LinearLayout item = row(); item.setPadding(dp(14), dp(15), dp(16), dp(15)); item.setMinimumHeight(dp(84));
        ripple(item, colors.surface, 17, colors.line);
        Symbols.Icon mark = new Symbols.Icon(this, "mix", colors.green);
        item.addView(mark, lp(40, 40));
        LinearLayout info = column(); info.setPadding(dp(12), 0, dp(12), 0);
        TextView name = text(mix.name, 16, colors.ink, true); oneLine(name); info.addView(name, lp(-1, -2));
        int count = 0; for (int level : mix.levels) if (level > 0) count++;
        TextView countLabel = text(count + " sounds", 11, colors.muted, false); countLabel.setPadding(0, dp(6), 0, 0);
        info.addView(countLabel, lp(-1, -2)); item.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        Symbols.Icon play = new Symbols.Icon(this, "play", colors.green); item.addView(play, lp(26, 30));
        item.setContentDescription("Play " + mix.name + ", " + count + " sounds");
        item.setOnClickListener(v -> loadMix(mix));
        item.setOnLongClickListener(v -> { savedMixOptions(mix); return true; });
        LinearLayout.LayoutParams p = lp(-1, -2); p.bottomMargin = dp(10); body.addView(item, p);
    }
    private void savedMixOptions(SessionState.SavedMix mix) {
        AlertDialog d = dialog(mix.name).setItems(new String[]{"Edit mix", "Delete mix"}, (dialog, choice) -> {
            if (choice == 0) { loadMix(mix); showPage(1); }
            else {
                AlertDialog confirm = dialog("Delete " + mix.name + "?").setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (confirmDialog, which) -> { state.mixes.remove(mix); state.save(); renderContent(); }).create();
                styleDialog(confirm);
            }
        }).setNegativeButton("Cancel", null).create();
        styleDialog(d);
    }
    private void mixer() {
        LinearLayout header = row();
        TextView label = text("Mixes", 29, colors.ink, false); label.setLetterSpacing(-.035f);
        header.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        TextView save = text("Save", 14, colors.green, true); save.setPadding(dp(16), dp(14), dp(4), dp(14));
        save.setGravity(Gravity.CENTER); save.setMinHeight(dp(48)); save.setContentDescription("Save current mix");
        save.setOnClickListener(v -> saveDialog()); ripple(save, Color.TRANSPARENT, 12, 0);
        header.addView(save, lp(-2, -2)); body.addView(header, lp(-1, -2)); space(body, 18);
        LinearLayout filters = row();
        String[] labels = {"All", "Noise", "Nature", "Tone"};
        for (int i = 0; i < 4; i++) {
            final int selected = i;
            LinearLayout tab = column(); tab.setGravity(Gravity.CENTER);
            TextView t = text(labels[i], 12, filter == i ? colors.ink : colors.muted, filter == i);
            t.setGravity(Gravity.CENTER); t.setPadding(dp(2), dp(13), dp(2), dp(13)); t.setMinHeight(dp(46));
            tab.addView(t, lp(-1, -2));
            View line = new View(this); line.setBackgroundColor(filter == i ? colors.green : colors.line);
            tab.addView(line, lp(-1, filter == i ? 2 : 1));
            tab.setSelected(filter == i); tab.setContentDescription(labels[i]);
            tab.setOnClickListener(v -> { filter = selected; renderContent(); });
            filters.addView(tab, new LinearLayout.LayoutParams(0, -2, 1));
        }
        body.addView(filters, lp(-1, -2)); space(body, 18);
        int usable = getResources().getConfiguration().screenWidthDp - 40;
        float fontScale = getResources().getConfiguration().fontScale;
        int columns = usable >= 560 ? 4 : usable < 300 || fontScale >= 1.5f ? 2 : 3;
        LinearLayout line = null; int n = 0;
        for (int i = 0; i < 12; i++) {
            if (filter != 0 && SessionState.GROUPS[i] != filter) continue;
            if (n % columns == 0) {
                line = row(); line.setGravity(Gravity.TOP);
                LinearLayout.LayoutParams p = lp(-1, -2); p.bottomMargin = dp(9); body.addView(line, p);
            }
            SoundCard card = new SoundCard(i); cards.put(i, card);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
            if (n % columns != 0) p.leftMargin = dp(9);
            line.addView(card, p); n++;
        }
        if (line != null && n % columns != 0) for (int i = n % columns; i < columns; i++) {
            View spacer = new View(this); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 1, 1);
            p.leftMargin = dp(9); line.addView(spacer, p);
        }
    }
    private final class SoundCard extends LinearLayout {
        final int index;
        final Symbols.Icon check;
        SoundCard(int index) {
            super(MainActivity.this); this.index = index;
            setOrientation(VERTICAL); setGravity(Gravity.CENTER_HORIZONTAL);
            setPadding(dp(10), dp(10), dp(10), dp(16)); setMinimumHeight(dp(142));
            LinearLayout head = row();
            head.addView(new View(MainActivity.this), new LinearLayout.LayoutParams(0, 1, 1));
            check = new Symbols.Icon(MainActivity.this, "check", colors.green); head.addView(check, lp(20, 20));
            addView(head, lp(-1, 20));
            Symbols.Sound art = new Symbols.Sound(MainActivity.this, index); addView(art, lp(52, 52));
            TextView name = text(SessionState.NAMES[index], 12, colors.ink, true);
            name.setGravity(Gravity.CENTER); name.setMaxLines(2); name.setPadding(0, dp(9), 0, 0);
            addView(name, lp(-1, -2));
            setOnClickListener(v -> toggleSound(index));
            setFocusable(true);
            update();
        }
        void update() {
            boolean active = state.levels[index] > 0;
            ripple(this, colors.surface, 17, active ? colors.green : colors.line);
            check.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
            setSelected(active);
            setStateDescription(active ? "Selected" : "Not selected");
            setContentDescription(SessionState.NAMES[index]);
        }
    }
    private void toggleSound(int index) {
        state.levels[index] = state.levels[index] > 0 ? 0 : 50; state.name = ""; state.save();
        for (SoundCard card : cards.values()) card.update();
        if (state.active() == 0 && state.playing) PlaybackService.command(this, PlaybackService.PAUSE);
        else if (!state.playing && state.levels[index] > 0) startPlayback();
        else scheduleRefresh();
        updatePlayer();
    }
    private SeekBar seek(int progress) {
        SeekBar seek = new SeekBar(this); seek.setMax(100); seek.setProgress(progress);
        seek.setProgressTintList(ColorStateList.valueOf(colors.green));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(colors.line));
        GradientDrawable thumb = Palette.shape(colors.green, dp(10), 0); thumb.setSize(dp(13), dp(13));
        seek.setThumb(thumb); seek.setThumbOffset(dp(6)); seek.setSplitTrack(false);
        seek.setPadding(dp(7), 0, dp(7), 0);
        return seek;
    }
    private void buildPlayer() {
        player = row(); player.setPadding(dp(10), dp(10), dp(10), dp(10)); player.setMinimumHeight(dp(72));
        surface(player, colors.surface, 20, colors.line);
        ImageView artwork = new ImageView(this); artwork.setImageBitmap(Symbols.artwork(dp(44)));
        artwork.setClipToOutline(true); surface(artwork, colors.bg, 12, 0);
        player.addView(artwork, lp(40, 40));
        LinearLayout names = column(); names.setPadding(dp(10), dp(4), dp(5), dp(4)); names.setGravity(Gravity.CENTER_VERTICAL);
        names.setMinimumHeight(dp(48));
        title = text(state.title(), 14, colors.ink, true); oneLine(title);
        subtitle = text(state.summary(), 10, colors.muted, false); oneLine(subtitle); subtitle.setPadding(0, dp(6), 0, 0);
        names.addView(title, lp(-1, -2)); names.addView(subtitle, lp(-1, -2));
        names.setContentDescription("Open current mix controls"); names.setOnClickListener(v -> mixDialog());
        ripple(names, Color.TRANSPARENT, 10, 0);
        player.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        artwork.setOnClickListener(v -> mixDialog()); artwork.setContentDescription("Open current mix controls");
        timerButton = row(); timerButton.setGravity(Gravity.CENTER); timerButton.setMinimumHeight(dp(48));
        timerButton.setPadding(dp(8), dp(5), dp(8), dp(5));
        timerText = text("", 12, colors.green, true); timerText.setFontFeatureSettings("tnum");
        timerText.setGravity(Gravity.CENTER); timerButton.addView(timerText, lp(-2, -2));
        ripple(timerButton, colors.bg, 14, 0); timerButton.setOnClickListener(v -> timerDialog());
        LinearLayout.LayoutParams timerParams = lp(-2, -2); timerParams.rightMargin = dp(8); player.addView(timerButton, timerParams);
        playIcon = icon("play", "Play", 48, colors.greenInk, () -> {
            if (state.playing) PlaybackService.command(this, PlaybackService.PAUSE); else startPlayback();
        });
        ripple(playIcon, colors.green, 24, 0); player.addView(playIcon, lp(48, 48));
        LinearLayout.LayoutParams p = lp(-1, -2); p.leftMargin = dp(12); p.rightMargin = dp(12); p.topMargin = dp(4); p.bottomMargin = dp(6);
        root.addView(player, p); updatePlayer();
    }
    private void updatePlayer() {
        if (title == null) return;
        String name = state.title(); if (!title.getText().toString().equals(name)) title.setText(name);
        String detail = state.error.isEmpty() ? (state.timerMinutes > 0 && !state.playing ? "Paused · " : "") + state.summary() : state.error;
        if (!subtitle.getText().toString().equals(detail)) subtitle.setText(detail);
        playIcon.symbol(state.playing ? "pause" : "play", colors.greenInk);
        playIcon.setContentDescription(state.playing ? "Pause" : "Play");
        String countdown = state.timerMinutes > 0 ? MixMath.time(state.remainingNow()) : "Off";
        timerText.setText(countdown); timerButton.setVisibility(state.timerMinutes > 0 ? View.VISIBLE : View.GONE);
        timerButton.setContentDescription("Sleep timer " + countdown + (state.playing ? " remaining" : " paused"));
        timerButton.setStateDescription(state.playing ? "Running" : "Paused");
        if (sheetTimer != null) sheetTimer.setText(countdown + (state.timerMinutes > 0 && !state.playing ? " · paused" : ""));
    }
    private void mixDialog() {
        if (mixSheet != null && mixSheet.isShowing()) return;
        mixSheet = new Dialog(this, state.dark ? R.style.SereinDialog : R.style.SereinDialogLight);
        mixSheet.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = column(); box.setPadding(dp(22), dp(12), dp(22), dp(18));
        surface(box, colors.surface, 25, 0);
        LinearLayout header = row();
        TextView label = text("Mix", 25, colors.ink, false); header.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(icon("close", "Close mix controls", 48, colors.muted, () -> mixSheet.dismiss())); box.addView(header, lp(-1, -2));
        space(box, 12);
        addVolumeControl(box, "Volume", -1);
        space(box, 14);
        for (int i = 0; i < 12; i++) if (state.levels[i] > 0) addVolumeControl(box, SessionState.NAMES[i], i);
        if (state.active() == 0) {
            TextView empty = text("No sounds selected", 14, colors.muted, false); empty.setPadding(0, dp(12), 0, dp(16));
            box.addView(empty, lp(-1, -2));
        }
        LinearLayout timer = row(); timer.setPadding(0, dp(10), 0, dp(10)); timer.setMinimumHeight(dp(56));
        timer.addView(new Symbols.Icon(this, "timer", colors.muted), lp(34, 36));
        TextView timerLabel = text("Sleep timer", 14, colors.ink, false);
        timer.addView(timerLabel, new LinearLayout.LayoutParams(0, -2, 1));
        sheetTimer = text("", 13, colors.green, true); timer.addView(sheetTimer, lp(-2, -2));
        timer.setOnClickListener(v -> timerDialog()); ripple(timer, Color.TRANSPARENT, 12, 0); box.addView(timer, lp(-1, -2));
        space(box, 12);
        LinearLayout actions = row();
        TextView edit = button("Edit sounds", false, () -> { mixSheet.dismiss(); showPage(1); });
        actions.addView(edit, new LinearLayout.LayoutParams(0, -2, 1));
        TextView save = button("Save mix", true, () -> { mixSheet.dismiss(); saveDialog(); });
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, -2, 1); saveParams.leftMargin = dp(10); actions.addView(save, saveParams);
        box.addView(actions, lp(-1, -2));
        SheetScroll scroll = new SheetScroll(); scroll.setVerticalScrollBarEnabled(false); scroll.addView(box);
        mixSheet.setContentView(scroll);
        mixSheet.setOnDismissListener(d -> { sheetTimer = null; state.save(); });
        Window window = mixSheet.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); window.setDimAmount(.25f);
        window.setGravity(Gravity.BOTTOM); window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        window.setDecorFitsSystemWindows(false);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            box.setPadding(dp(22) + safe.left, dp(12), dp(22) + safe.right, dp(18) + safe.bottom);
            return insets;
        });
        mixSheet.show(); window.setLayout(-1, -2); scroll.requestApplyInsets(); updatePlayer();
    }
    private final class SheetScroll extends ScrollView {
        SheetScroll() { super(MainActivity.this); }
        @Override protected void onMeasure(int width, int height) {
            int max = (int)(getWindowManager().getCurrentWindowMetrics().getBounds().height() * .82f);
            int limit = View.MeasureSpec.getMode(height) == View.MeasureSpec.UNSPECIFIED ? max : Math.min(max, View.MeasureSpec.getSize(height));
            super.onMeasure(width, View.MeasureSpec.makeMeasureSpec(limit, View.MeasureSpec.AT_MOST));
        }
    }
    private void addVolumeControl(LinearLayout box, String name, int index) {
        LinearLayout labels = row();
        TextView label = text(name, 13, colors.ink, index < 0); labels.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        TextView value = text((index < 0 ? state.master : state.levels[index]) + "%", 11, colors.muted, false);
        value.setPadding(dp(10), 0, 0, 0); labels.addView(value, lp(-2, -2)); box.addView(labels, lp(-1, -2));
        SeekBar volume = seek(index < 0 ? state.master : state.levels[index]); volume.setContentDescription(name + " volume");
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int progress, boolean user) {
                if (!user) return;
                if (index < 0) state.master = progress;
                else { state.levels[index] = progress; state.name = ""; }
                value.setText(progress + "%"); scheduleRefresh(); updatePlayer();
                for (SoundCard card : cards.values()) card.update();
            }
            @Override public void onStartTrackingTouch(SeekBar b) {}
            @Override public void onStopTrackingTouch(SeekBar b) {
                state.save();
                if (state.playing && state.active() == 0) PlaybackService.command(MainActivity.this, PlaybackService.PAUSE);
            }
        });
        box.addView(volume, lp(-1, 48));
    }
    private void scheduleRefresh() { handler.removeCallbacks(refreshAudio); handler.postDelayed(refreshAudio, 45); }
    private void startPlayback() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("askedNotifications", false)) {
            getPreferences(0).edit().putBoolean("askedNotifications", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        PlaybackService.command(this, PlaybackService.PLAY);
    }
    private AlertDialog.Builder dialog(String title) {
        return new AlertDialog.Builder(new ContextThemeWrapper(this, state.dark ? R.style.SereinDialog : R.style.SereinDialogLight)).setTitle(title);
    }
    private void styleDialog(AlertDialog d) {
        d.show(); d.getWindow().setBackgroundDrawable(Palette.shape(colors.surface, dp(25), 0));
        if (d.getButton(-1) != null) d.getButton(-1).setTextColor(colors.green);
        if (d.getButton(-2) != null) d.getButton(-2).setTextColor(colors.muted);
    }
    private void saveDialog() {
        if (state.active() == 0) { Toast.makeText(this, "Choose a sound first", Toast.LENGTH_SHORT).show(); return; }
        if (state.mixes.size() >= 50) { Toast.makeText(this, "50 saved mixes", Toast.LENGTH_SHORT).show(); return; }
        EditText name = new EditText(this); name.setTextColor(colors.ink); name.setHintTextColor(colors.muted);
        name.setHint("Mix name"); name.setSingleLine(true); name.setText(state.name);
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});
        LinearLayout input = column(); input.setPadding(dp(24), dp(10), dp(24), 0); input.addView(name, lp(-1, -2));
        AlertDialog d = dialog("Save mix").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create(); styleDialog(d);
        d.getButton(-1).setOnClickListener(v -> {
            String label = name.getText().toString().trim();
            if (label.isEmpty()) { name.setError("Name required"); return; }
            state.mixes.add(0, new SessionState.SavedMix(label, state.levels)); state.name = label; state.save();
            if (page == 0) renderContent(); updatePlayer(); if (state.playing) scheduleRefresh(); d.dismiss();
        });
        name.requestFocus(); d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
    private void loadMix(SessionState.SavedMix mix) {
        System.arraycopy(mix.levels, 0, state.levels, 0, 12); state.name = mix.name; state.save();
        startPlayback(); updatePlayer(); for (SoundCard card : cards.values()) card.update();
    }
    private void timerDialog() {
        LinearLayout box = column(); box.setPadding(dp(20), dp(15), dp(20), dp(4));
        Spinner fade = new Spinner(new ContextThemeWrapper(this, state.dark ? R.style.SereinDialog : R.style.SereinDialogLight));
        String[] choices = {"Off", "15 seconds", "30 seconds", "1 minute", "2 minutes"}; int[] values = {0, 15, 30, 60, 120};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(new ContextThemeWrapper(this, state.dark ? R.style.SereinDialog : R.style.SereinDialogLight), android.R.layout.simple_spinner_dropdown_item, choices);
        fade.setAdapter(adapter); for (int i = 0; i < values.length; i++) if (values[i] == state.fadeSeconds) fade.setSelection(i);
        AlertDialog[] holder = new AlertDialog[1]; int[] minutes = {15, 30, 45, 60, 90, 0};
        for (int r = 0; r < 2; r++) {
            LinearLayout line = row();
            for (int col = 0; col < 3; col++) {
                final int value = minutes[r * 3 + col];
                TextView t = button(value == 0 ? "Off" : value + " min", state.timerMinutes == value, () -> {
                    state.fadeSeconds = values[fade.getSelectedItemPosition()]; applyTimer(value); holder[0].dismiss();
                });
                t.setTextSize(12); t.setPadding(dp(5), dp(14), dp(5), dp(14));
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); if (col > 0) p.leftMargin = dp(8); line.addView(t, p);
            }
            LinearLayout.LayoutParams p = lp(-1, -2); p.bottomMargin = dp(10); box.addView(line, p);
        }
        LinearLayout custom = row(); custom.addView(text("Minutes", 13, colors.muted, false), new LinearLayout.LayoutParams(0, -2, 1));
        EditText input = new EditText(this); input.setInputType(InputType.TYPE_CLASS_NUMBER); input.setTextColor(colors.ink); input.setHintTextColor(colors.muted);
        input.setHint("Custom"); input.setSingleLine(true); input.setTextSize(14); input.setGravity(Gravity.END); input.setMinHeight(dp(48));
        custom.addView(input, lp(110, -2)); box.addView(custom, lp(-1, -2));
        LinearLayout fading = row(); fading.setMinimumHeight(dp(56));
        fading.addView(text("Fade", 13, colors.muted, false), new LinearLayout.LayoutParams(0, -2, 1));
        fading.addView(fade, lp(160, -2)); box.addView(fading, lp(-1, -2));
        ScrollView scroll = new ScrollView(this); scroll.addView(box);
        AlertDialog d = dialog("Sleep timer").setView(scroll).setNegativeButton("Cancel", null).setPositiveButton("Set", null).create(); holder[0] = d; styleDialog(d);
        d.getButton(-1).setOnClickListener(v -> {
            int n; try { n = Integer.parseInt(input.getText().toString()); } catch (Exception e) { n = -1; }
            if (n < 1 || n > 720) { input.setError("1–720 minutes"); return; }
            state.fadeSeconds = values[fade.getSelectedItemPosition()]; applyTimer(n); d.dismiss();
        });
    }
    private void applyTimer(int minutes) {
        state.setTimer(minutes);
        if (state.playing) startService(new Intent(this, PlaybackService.class).setAction(PlaybackService.TIMER).putExtra("minutes", minutes));
        updatePlayer(); SereinWidget.updateAll(this);
    }
    private void optionsDialog() {
        AlertDialog d = dialog("Serein").setItems(new String[]{state.dark ? "Light theme" : "Night theme", "Add widget", "About"}, (dialog, which) -> {
            if (which == 0) { state.dark = !state.dark; state.save(); build(); }
            else if (which == 1) widgetDialog();
            else { AlertDialog about = dialog("Serein").setMessage("1.1.0\n12 sounds. Available offline.").setPositiveButton("Done", null).create(); styleDialog(about); }
        }).setNegativeButton("Close", null).create();
        styleDialog(d);
    }
    private void widgetDialog() {
        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        if (!manager.isRequestPinAppWidgetSupported()) { Toast.makeText(this, "Long-press your home screen, then choose Widgets", Toast.LENGTH_LONG).show(); return; }
        AlertDialog d = dialog("Add widget").setItems(new String[]{"Player · 4 × 2", "Sleep · 2 × 2"}, (dialog, which) -> manager.requestPinAppWidget(new ComponentName(this, which == 0 ? SereinWidget.class : SleepWidget.class), null, null)).setNegativeButton("Cancel", null).create();
        styleDialog(d);
    }
    private void breathing() {
        heading("Breathe");
        LinearLayout panel = column(); panel.setGravity(Gravity.CENTER_HORIZONTAL); space(panel, 12);
        breath = new BreathView();
        int available = getResources().getConfiguration().screenWidthDp - 48;
        int diameter = Math.min(290, available);
        panel.addView(breath, lp(diameter, diameter));
        TextView pattern = text("4 / 4 / 6", 12, colors.muted, false); pattern.setGravity(Gravity.CENTER); pattern.setLetterSpacing(.15f);
        pattern.setPadding(0, dp(12), 0, dp(12)); panel.addView(pattern, lp(-1, -2));
        TextView start = button("Start", false, () -> {});
        start.setOnClickListener(v -> { breathStart = breathStart == 0 ? SystemClock.elapsedRealtime() : 0; start.setText(breathStart == 0 ? "Start" : "Finish"); breath.invalidate(); });
        LinearLayout.LayoutParams p = lp(160, -2); p.topMargin = dp(20); panel.addView(start, p); body.addView(panel, lp(-1, -2));
    }
    private final class BreathView extends View {
        final Paint paint = new Paint(3);
        final boolean reducedMotion;
        BreathView() {
            super(MainActivity.this);
            reducedMotion = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0;
            setContentDescription("Breathing guide: 4 seconds in, 4 hold, 6 out");
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            double seconds = breathStart == 0 ? 0 : (SystemClock.elapsedRealtime() - breathStart) / 1000d, phase = seconds % 14;
            float scale = breathStart == 0 ? 1 : phase < 4 ? .85f + (float)phase / 4 * .25f : phase < 8 ? 1.1f : 1.1f - (float)(phase - 8) / 6 * .25f;
            if (reducedMotion) scale = 1;
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float unit = Math.min(getWidth(), getHeight()) / 290f, r = 88 * unit * scale;
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(colors.line);
            canvas.drawCircle(cx, cy, r + 24 * unit, paint); canvas.drawCircle(cx, cy, r + 12 * unit, paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(colors.surface); canvas.drawCircle(cx, cy, r, paint);
            paint.setColor(colors.green); paint.setStyle(Paint.Style.STROKE); canvas.drawCircle(cx, cy, r, paint);
            String instruction = breathStart == 0 ? "Breathe" : phase < 4 ? "In" : phase < 8 ? "Hold" : "Out";
            String number = breathStart == 0 ? "" : String.valueOf((int)Math.ceil(phase < 4 ? 4 - phase : phase < 8 ? 8 - phase : 14 - phase));
            paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER); paint.setColor(colors.ink); paint.setTypeface(Typeface.create("sans-serif", 0));
            paint.setTextSize(23 * unit); canvas.drawText(instruction, cx, cy - 3 * unit, paint);
            paint.setTextSize(13 * unit); paint.setColor(colors.muted); canvas.drawText(number, cx, cy + 27 * unit, paint);
            if (breathStart > 0 && !reducedMotion && getWindowVisibility() == View.VISIBLE) postInvalidateOnAnimation();
        }
    }
}
