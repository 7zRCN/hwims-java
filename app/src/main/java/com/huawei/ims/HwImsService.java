/*
 * This file is part of HwIms
 * Copyright (C) 2019 Penn Mackintosh
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.huawei.ims;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsService;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsConfigImplBase;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.util.Log;

public class HwImsService extends ImsService {
    private static final String LOG_TAG = "HwImsService";
    private static HwImsService mInstance = null;
    private final HwMmTelFeature[] mmTelFeatures = {null, null};
    private final HwImsRegistration[] registrations = {null, null};
    private final HwImsConfig[] configs = new HwImsConfig[3];
    private SharedPreferences prefs;

    public static HwImsService getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        Log.v(LOG_TAG, "HwImsService created!");
        prefs = createDeviceProtectedStorageContext().getSharedPreferences("config", MODE_PRIVATE);
    }

    @Override
    public void enableIms(int slotId) {
        ((HwMmTelFeature) createMmTelFeature(slotId)).registerIms();
    }

    @Override
    public void disableIms(int slotId) {
        ((HwMmTelFeature) createMmTelFeature(slotId)).unregisterIms();
    }

    @Override
    public void readyForFeatureCreation() {
        if (mInstance != null && mInstance != this) {
            throw new RuntimeException();
        }
        mInstance = this;
    }

    public static boolean supportsDualIms(Context context) {
        return HwModemCapability.isCapabilitySupport(21) && context.getSystemService(TelephonyManager.class).getPhoneCount() > 1;
    }

    @Override
    public ImsFeatureConfiguration querySupportedImsFeatures() {
        ImsFeatureConfiguration.Builder builder = new ImsFeatureConfiguration.Builder();
        if (prefs.getBoolean("ims0", true)) {
            builder.addFeature(0, ImsFeature.FEATURE_MMTEL);
        }
        if (supportsDualIms(this) && prefs.getBoolean("ims1", false)) {
            builder.addFeature(1, ImsFeature.FEATURE_MMTEL);
        }
        return builder.build();
    }

    @Override
    public MmTelFeature createMmTelFeature(int slotId) {
        if (slotId > 0 && !supportsDualIms(this)) {
            return null;
        }
        if (mmTelFeatures[slotId] == null) {
            mmTelFeatures[slotId] = HwMmTelFeature.getInstance(slotId);
            registrations[slotId] = new HwImsRegistration(slotId);
        }
        return mmTelFeatures[slotId];
    }

    @Override
    public ImsConfigImplBase getConfig(int slotId) {
        if (slotId > 0 && !supportsDualIms(this)) {
            return null;
        }
        if (configs[slotId] == null) {
            configs[slotId] = new HwImsConfig();
        }
        return configs[slotId];
    }

    @Override
    public HwImsRegistration getRegistration(int slotId) {
        if (slotId > 0 && !supportsDualIms(this)) {
            return null;
        }
        if (this.registrations[slotId] == null) {
            registrations[slotId] = new HwImsRegistration(slotId);
        }
        return this.registrations[slotId];
    }

}
