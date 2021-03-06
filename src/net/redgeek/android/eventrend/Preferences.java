/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.redgeek.android.eventrend;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.method.DigitsKeyListener;
import android.view.Menu;
import android.view.MenuItem;

import net.redgeek.android.eventrend.db.CategoryDbTable;
import net.redgeek.android.eventrend.db.EvenTrendDbAdapter;
import net.redgeek.android.eventrend.primitives.TimeSeries;
import net.redgeek.android.eventrend.primitives.TimeSeriesCollector;
import net.redgeek.android.eventrend.util.DialogUtil;

/**
 * Seriously considering replacing this with a custom preferences screen, adding
 * pop-up help dialogs, etc.
 * 
 * @author barclay
 * 
 */
public class Preferences extends PreferenceActivity {
  private static final int MENU_HELP_ID   = Menu.FIRST;
  private static final int MENU_RECALC_ID = Menu.FIRST + 1;
  
  private static final int HELP_DIALOG_ID = 0;

  public static final String PREFS_NAME = "EvenTrendPrefs";

  public static final String PREFS_DEFAULT_VIEW = "DefaultView";
  public static final String PREFS_DEFAULT_GRAPH_BLACK = "BlackGraphBackground";
  public static final String PREFS_DEFAULT_TO_LAST = "DefaultToLast";
  public static final String PREFS_DECIMAL_PLACES = "DecimalPlaces";
  public static final String PREFS_SMOOTHING_PERCENT = "SmoothingPercentage";
  public static final String PREFS_HISTORY = "History";
  public static final String PREFS_TREND_STDDEV = "DeviationSensitivity";

  public static final String PREFS_VIEW_DEFAULT = "";
  public static final boolean PREFS_GRAPH_BACKGROUND_BLACK = true;
  public static final boolean PREFS_DEFAULT_TO_LAST_DEFAULT = false;
  public static final int PREFS_DECIMAL_PLACES_DEFAULT = 2;
  public static final float PREFS_SMOOTHING_PERCENT_DEFAULT = 0.1f;
  public static final int PREFS_HISTORY_DEFAULT = 20;
  public static final float PREFS_TREND_STDDEV_DEFAULT = 0.5f;

  private EvenTrendDbAdapter mDbh;
  private DialogUtil         mDialogUtil;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    mDialogUtil = new DialogUtil(this);
    setPreferenceScreen(createPreferenceHierarchy());
  }

  private PreferenceScreen createPreferenceHierarchy() {
    mDbh = new EvenTrendDbAdapter.SqlAdapter(this);
    mDbh.open();

    DigitsKeyListener integer = new DigitsKeyListener(false, false);
    DigitsKeyListener decimal = new DigitsKeyListener(false, true);

    // Root
    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    // Data input prefs
    PreferenceCategory dataInput = new PreferenceCategory(this);
    dataInput.setTitle("Data Input");
    root.addPreference(dataInput);

    // default group displayed pref
    ListPreference defaultGroup = new ListPreference(this);

    Cursor c = mDbh.fetchAllGroups();
    c.moveToFirst();
    String[] values = new String[c.getCount()];

    for (int i = 0; i < c.getCount(); i++) {
      String group = c.getString(c
          .getColumnIndexOrThrow(CategoryDbTable.KEY_GROUP_NAME));
      values[i] = new String(group);
      c.moveToNext();
    }
    c.close();

    defaultGroup.setEntries(values);
    defaultGroup.setEntryValues(values);
    defaultGroup.setDialogTitle("Default Group");
    defaultGroup.setKey(PREFS_DEFAULT_VIEW);
    defaultGroup.setTitle("Default Group");
    defaultGroup.setSummary("Default group to display when launched");
    dataInput.addPreference(defaultGroup);

    // Black or white graph background
    CheckBoxPreference defaultGraphBlack = new CheckBoxPreference(this);
    defaultGraphBlack.setKey(PREFS_DEFAULT_GRAPH_BLACK);
    defaultGraphBlack.setTitle("Graph Background");
    defaultGraphBlack
        .setSummary("Change the graph background from black to white.");
    defaultGraphBlack
        .setDefaultValue(new Boolean(PREFS_GRAPH_BACKGROUND_BLACK));
    dataInput.addPreference(defaultGraphBlack);

    // Default value or last value
    // CheckBoxPreference defaultIsLastValue = new CheckBoxPreference(this);
    // defaultIsLastValue.setKey(PREFS_DEFAULT_TO_LAST);
    // defaultIsLastValue.setTitle("Default to Last Value");
    // defaultIsLastValue.setSummary("Set the default value to the last value entered");
    // defaultIsLastValue.setDefaultValue(PREFS_DEFAULT_TO_LAST_DEFAULT);
    // dataInput.addPreference(defaultIsLastValue);

    // Decimal places
    EditTextPreference decimalPlaces = new EditTextPreference(this);
    decimalPlaces.setDialogTitle("Number of Decimal Places");
    decimalPlaces.setKey(PREFS_DECIMAL_PLACES);
    decimalPlaces.setTitle("Decimal Places");
    decimalPlaces.setSummary("The number of decimal places to round to");
    decimalPlaces.setDefaultValue(new Integer(PREFS_DECIMAL_PLACES_DEFAULT)
        .toString());
    decimalPlaces.getEditText().setKeyListener(integer);
    dataInput.addPreference(decimalPlaces);

    // Trending prefs
    PreferenceCategory trendingPrefs = new PreferenceCategory(this);
    trendingPrefs.setTitle("Trending Parameters");
    root.addPreference(trendingPrefs);

    // History
    EditTextPreference history = new EditTextPreference(this);
    history.setDialogTitle("Trending History");
    history.setKey(PREFS_HISTORY);
    history.setTitle("History");
    history
        .setSummary("The number of datapoints to include in weighted averaging.");
    history.setDefaultValue(new Integer(PREFS_HISTORY_DEFAULT).toString());
    history.getEditText().setKeyListener(integer);
    trendingPrefs.addPreference(history);

    // Standard Deviation Sensitivity
    EditTextPreference sensitivity = new EditTextPreference(this);
    sensitivity.setDialogTitle("Deviation Sensitivity");
    sensitivity.setKey(PREFS_TREND_STDDEV);
    sensitivity.setTitle("Standard Deviation Sensitivity");
    sensitivity
        .setSummary("A scaling influencing trend icons.  Bigger == less sensitive.");
    sensitivity.setDefaultValue(new Float(PREFS_TREND_STDDEV_DEFAULT)
        .toString());
    sensitivity.getEditText().setKeyListener(decimal);
    trendingPrefs.addPreference(sensitivity);

    // Smoothing
    EditTextPreference smoothing = new EditTextPreference(this);
    smoothing.setDialogTitle("Smoothing Constant");
    smoothing.setKey(PREFS_SMOOTHING_PERCENT);
    smoothing.setTitle("Smoothing Constant");
    smoothing.setSummary("Weight to decay moving average weighting by.");
    smoothing.setDefaultValue(new Float(PREFS_SMOOTHING_PERCENT_DEFAULT)
        .toString());
    smoothing.getEditText().setKeyListener(decimal);
    trendingPrefs.addPreference(smoothing);

    return root;
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    boolean result = super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_HELP_ID, 0, R.string.menu_app_help).setIcon(
        android.R.drawable.ic_menu_help);
    menu.add(0, MENU_RECALC_ID, 0, R.string.menu_prefs_recalc)
        .setIcon(R.drawable.refresh);
    return result;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case MENU_HELP_ID:
        showDialog(HELP_DIALOG_ID);
        return true;
      case MENU_RECALC_ID:
        recalcTrends();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
  
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case HELP_DIALOG_ID:
        String str = getResources().getString(R.string.prefs_overview);
        return mDialogUtil.newOkDialog("Help", str);
    }
    return null;
  }

  private void recalcTrends() {
    TimeSeriesCollector tsc = new TimeSeriesCollector(mDbh);
    tsc.setHistory(getHistory(this));
    tsc.setSmoothing(getSmoothingConstant(this));
    tsc.setSensitivity(getStdDevSensitivity(this));
    tsc.setInterpolators(EvenTrendActivity.getInterpolatorsCopy());    
    tsc.updateTimeSeriesMetaLocking(true);

    for (int i = 0; i < tsc.numSeries(); i++) {
      TimeSeries ts = tsc.getSeries(i);
      tsc.updateCategoryTrend(ts.getDbRow().getId());
    }
  }
  
  public static String getDefaultGroup(Context ctx) {
    SharedPreferences settings = PreferenceManager
        .getDefaultSharedPreferences(ctx);
    return settings.getString(PREFS_DEFAULT_VIEW, PREFS_VIEW_DEFAULT);
  }

  public static boolean getDefaultGraphIsBlack(Context ctx) {
    SharedPreferences settings = PreferenceManager
        .getDefaultSharedPreferences(ctx);
    return settings.getBoolean(PREFS_DEFAULT_GRAPH_BLACK, new Boolean(
        PREFS_GRAPH_BACKGROUND_BLACK));
  }

  public static boolean getDefaultIsLastValue(Context ctx) {
    SharedPreferences settings = PreferenceManager
        .getDefaultSharedPreferences(ctx);
    return settings.getBoolean(PREFS_DEFAULT_TO_LAST, new Boolean(
        PREFS_DEFAULT_TO_LAST_DEFAULT));
  }

  public static int getDecimalPlaces(Context ctx) {
    SharedPreferences settings = PreferenceManager
        .getDefaultSharedPreferences(ctx);
    String s = settings.getString(PREFS_DECIMAL_PLACES, new Integer(
        PREFS_DECIMAL_PLACES_DEFAULT).toString());
    int i;
    try {
      i = Integer.parseInt(s);
    } catch (Exception e) {
      i = PREFS_DECIMAL_PLACES_DEFAULT;
    }
    return i;
  }

  public static float getSmoothingConstant(Context ctx) {
    SharedPreferences settings = PreferenceManager
        .getDefaultSharedPreferences(ctx);
    String s = settings.getString(PREFS_SMOOTHING_PERCENT, new Float(
        PREFS_SMOOTHING_PERCENT_DEFAULT).toString());
    float f;
    try {
      f = Float.parseFloat(s);
    } catch (Exception e) {
      f = PREFS_SMOOTHING_PERCENT_DEFAULT;
    }
    return f;
  }

  public static int getHistory(Context ctx) {
    SharedPreferences settings = PreferenceManager
        .getDefaultSharedPreferences(ctx);
    String s = settings.getString(PREFS_HISTORY, new Integer(
        PREFS_HISTORY_DEFAULT).toString());
    int i;
    try {
      i = Integer.parseInt(s);
    } catch (Exception e) {
      i = PREFS_HISTORY_DEFAULT;
    }
    return i;
  }

  public static float getStdDevSensitivity(Context ctx) {
    SharedPreferences settings = PreferenceManager
        .getDefaultSharedPreferences(ctx);
    String s = settings.getString(PREFS_TREND_STDDEV, new Float(
        PREFS_TREND_STDDEV_DEFAULT).toString());
    float f;
    try {
      f = Float.parseFloat(s);
    } catch (Exception e) {
      f = PREFS_TREND_STDDEV_DEFAULT;
    }
    return f;
  }
}