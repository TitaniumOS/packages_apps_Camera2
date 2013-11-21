/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.camera;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.android.camera.CameraActivity;
import com.android.camera.CameraPreference.OnPreferenceChangedListener;
import com.android.camera.drawable.TextDrawable;
import com.android.camera.settings.SettingsManager;
import com.android.camera.ui.PieItem;
import com.android.camera.ui.PieItem.OnClickListener;
import com.android.camera.ui.PieRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PieController {

    private static String TAG = "CAM_piecontrol";

    protected static final int MODE_PHOTO = 0;
    protected static final int MODE_VIDEO = 1;

    protected static float CENTER = (float) Math.PI / 2;
    protected static final float SWEEP = 0.06f;

    protected CameraActivity mActivity;
    protected PreferenceGroup mPreferenceGroup;
    protected OnPreferenceChangedListener mListener;
    protected PieRenderer mRenderer;
    private List<IconListPreference> mPreferences;
    private Map<IconListPreference, PieItem> mPreferenceMap;
    private Map<IconListPreference, String> mOverrides;

    public void setListener(OnPreferenceChangedListener listener) {
        mListener = listener;
    }

    public PieController(CameraActivity activity, PieRenderer pie) {
        mActivity = activity;
        mRenderer = pie;
        mPreferences = new ArrayList<IconListPreference>();
        mPreferenceMap = new HashMap<IconListPreference, PieItem>();
        mOverrides = new HashMap<IconListPreference, String>();
    }

    public void initialize(PreferenceGroup group) {
        mRenderer.clearItems();
        mPreferenceMap.clear();
        setPreferenceGroup(group);
    }

    public void onSettingChanged(ListPreference pref) {
        if (mListener != null) {
            mListener.onSharedPreferenceChanged();
        }
    }

    protected void setCameraId(int cameraId) {
        ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_CAMERA_ID);
        SettingsManager settingsManager = mActivity.getSettingsManager();
        settingsManager.setValueFromPreference(pref, "" + cameraId);
    }

    protected PieItem makeItem(int resId) {
        // We need a mutable version as we change the alpha
        Drawable d = mActivity.getResources().getDrawable(resId).mutate();
        return new PieItem(d, 0);
    }

    protected PieItem makeItem(CharSequence value) {
        TextDrawable drawable = new TextDrawable(mActivity.getResources(), value);
        return new PieItem(drawable, 0);
    }

    public PieItem makeItem(String prefKey) {
        final SettingsManager settingsManager = mActivity.getSettingsManager();

        final IconListPreference pref =
                (IconListPreference) mPreferenceGroup.findPreference(prefKey);
        if (pref == null) return null;
        int[] iconIds = pref.getLargeIconIds();
        int resid = -1;
        if (!pref.getUseSingleIcon() && iconIds != null) {
            // Each entry has a corresponding icon.
            String value = settingsManager.getValueFromPreference(pref);
            int index = pref.findIndexOfValue(value);
            resid = iconIds[index];
        } else {
            // The preference only has a single icon to represent it.
            resid = pref.getSingleIcon();
        }
        PieItem item = makeItem(resid);
        item.setLabel(pref.getTitle().toUpperCase());
        mPreferences.add(pref);
        mPreferenceMap.put(pref, item);
        int nOfEntries = pref.getEntries().length;
        if (nOfEntries > 1) {
            for (int i = 0; i < nOfEntries; i++) {
                PieItem inner = null;
                if (iconIds != null) {
                    inner = makeItem(iconIds[i]);
                } else {
                    inner = makeItem(pref.getEntries()[i]);
                }
                inner.setLabel(pref.getLabels()[i]);
                item.addItem(inner);
                final int index = i;
                inner.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(PieItem item) {
                        settingsManager.setValueIndexFromPreference(pref, index);
                        reloadPreference(pref);
                        onSettingChanged(pref);
                    }
                });
            }
        }
        return item;
    }

    public PieItem makeSwitchItem(final String prefKey, boolean addListener) {
        final IconListPreference pref =
                (IconListPreference) mPreferenceGroup.findPreference(prefKey);
        if (pref == null) return null;
        int[] iconIds = pref.getLargeIconIds();
        int resid = -1;
        SettingsManager settingsManager = mActivity.getSettingsManager();
        String value = settingsManager.getValueFromPreference(pref);
        int index = pref.findIndexOfValue(value);

        if (!pref.getUseSingleIcon() && iconIds != null) {
            // Each entry has a corresponding icon.
            resid = iconIds[index];
        } else {
            // The preference only has a single icon to represent it.
            resid = pref.getSingleIcon();
        }
        PieItem item = makeItem(resid);
        item.setLabel(pref.getLabels()[index]);
        item.setImageResource(mActivity, resid);
        mPreferences.add(pref);
        mPreferenceMap.put(pref, item);
        if (addListener) {
            final PieItem fitem = item;
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(PieItem item) {
                    if (!item.isEnabled()) {
                        return;
                    }
                    IconListPreference pref = (IconListPreference) mPreferenceGroup
                            .findPreference(prefKey);

                    SettingsManager settingsManager = mActivity.getSettingsManager();
                    String value = settingsManager.getValueFromPreference(pref);
                    int index = pref.findIndexOfValue(value);

                    CharSequence[] values = pref.getEntryValues();
                    index = (index + 1) % values.length;
                    settingsManager.setValueIndexFromPreference(pref, index);
                    fitem.setLabel(pref.getLabels()[index]);
                    fitem.setImageResource(mActivity,
                            ((IconListPreference) pref).getLargeIconIds()[index]);
                    reloadPreference(pref);
                    onSettingChanged(pref);
                }
            });
        }
        return item;
    }


    public PieItem makeDialItem(ListPreference pref, int iconId, float center, float sweep) {
        PieItem item = makeItem(iconId);
        return item;
    }

    public void addItem(String prefKey) {
        PieItem item = makeItem(prefKey);
        mRenderer.addItem(item);
    }

    public void updateItem(PieItem item, String prefKey) {
        IconListPreference pref = (IconListPreference) mPreferenceGroup
                .findPreference(prefKey);
        if (pref != null) {
            SettingsManager settingsManager = mActivity.getSettingsManager();
            String value = settingsManager.getValueFromPreference(pref);
            int index = pref.findIndexOfValue(value);

            item.setLabel(pref.getLabels()[index]);
            item.setImageResource(mActivity,
                    ((IconListPreference) pref).getLargeIconIds()[index]);
        }
    }

    public void setPreferenceGroup(PreferenceGroup group) {
        mPreferenceGroup = group;
    }

    public void reloadPreferences() {
        mPreferenceGroup.reloadValue();
        for (IconListPreference pref : mPreferenceMap.keySet()) {
            reloadPreference(pref);
        }
    }

    private void reloadPreference(IconListPreference pref) {
        if (pref.getUseSingleIcon()) return;
        PieItem item = mPreferenceMap.get(pref);
        String overrideValue = mOverrides.get(pref);
        int[] iconIds = pref.getLargeIconIds();
        if (iconIds != null) {
            // Each entry has a corresponding icon.
            int index;
            if (overrideValue == null) {
                SettingsManager settingsManager = mActivity.getSettingsManager();
                String value = settingsManager.getValueFromPreference(pref);
                index = pref.findIndexOfValue(value);
            } else {
                index = pref.findIndexOfValue(overrideValue);
                if (index == -1) {
                    // Avoid the crash if camera driver has bugs.
                    Log.e(TAG, "Fail to find override value=" + overrideValue);
                    pref.print();
                    return;
                }
            }
            item.setImageResource(mActivity, iconIds[index]);
        } else {
            // The preference only has a single icon to represent it.
            item.setImageResource(mActivity, pref.getSingleIcon());
        }
    }

    // Scene mode may override other camera settings (ex: flash mode).
    public void overrideSettings(final String ... keyvalues) {
        if (keyvalues.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        for (IconListPreference pref : mPreferenceMap.keySet()) {
            override(pref, keyvalues);
        }
    }

    private void override(IconListPreference pref, final String ... keyvalues) {
        mOverrides.remove(pref);
        for (int i = 0; i < keyvalues.length; i += 2) {
            String key = keyvalues[i];
            String value = keyvalues[i + 1];
            if (key.equals(pref.getKey())) {
                mOverrides.put(pref, value);
                PieItem item = mPreferenceMap.get(pref);
                item.setEnabled(value == null);
                break;
            }
        }
        reloadPreference(pref);
    }
}
