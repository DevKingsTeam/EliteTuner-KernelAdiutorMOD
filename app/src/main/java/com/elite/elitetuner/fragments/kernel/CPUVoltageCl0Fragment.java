/*
 * Copyright (C) 2015-2016 Willi Ye <williye97@gmail.com>
 *
 * This file is part of Kernel Adiutor.
 *
 * Kernel Adiutor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kernel Adiutor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kernel Adiutor.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.elite.elitetuner.fragments.kernel;

import com.elite.elitetuner.R;
import com.elite.elitetuner.fragments.ApplyOnBootFragment;
import com.elite.elitetuner.fragments.recyclerview.RecyclerViewFragment;
import com.elite.elitetuner.utils.AppSettings;
import com.elite.elitetuner.utils.Utils;
import com.elite.elitetuner.utils.kernel.cpuvoltage.VoltageCl0;
import com.elite.elitetuner.views.recyclerview.CardView;
import com.elite.elitetuner.views.recyclerview.RecyclerViewItem;
import com.elite.elitetuner.views.recyclerview.SeekBarView;
import com.elite.elitetuner.views.recyclerview.SwitchView;
import com.elite.elitetuner.views.recyclerview.TitleView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by willi on 07.05.16.
 */
public class CPUVoltageCl0Fragment extends RecyclerViewFragment {

    private List<SeekBarView> mVoltages = new ArrayList<>();
    private SeekBarView mSeekbarProf = new SeekBarView();

    @Override
    protected void init() {
        super.init();

        addViewPagerFragment(ApplyOnBootFragment.newInstance(this));
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        mVoltages.clear();

        final List<String> freqs = VoltageCl0.getFreqs();
        final List<String> voltages = VoltageCl0.getVoltages();
        final List<String> voltagesStock = VoltageCl0.getStockVoltages();

        if (freqs != null && voltages != null && voltagesStock != null && freqs.size() == voltages.size()) {

            CardView freqCard = new CardView(getActivity());
            freqCard.setTitle(getString(R.string.cpuCl0_volt_control));

            List<String> progress = new ArrayList<>();
            for (float i = -100000f; i < 31250f; i += 6250) {
                String global = String.valueOf(i / VoltageCl0.getOffset());
                progress.add(global);
            }

            seekbarProfInit(mSeekbarProf, freqs, voltages, voltagesStock, progress);

            freqCard.addItem(mSeekbarProf);

            Boolean enableGlobal = AppSettings.getBoolean("CpuCl0_global_volts", true, getActivity());

            SwitchView voltControl = new SwitchView();
            voltControl.setTitle(getString(R.string.cpu_manual_volt));
            voltControl.setSummaryOn(getString(R.string.cpu_manual_volt_summaryOn));
            voltControl.setSummaryOff(getString(R.string.cpu_manual_volt_summaryOff));
            voltControl.setChecked(enableGlobal);
            voltControl.addOnSwitchListener(new SwitchView.OnSwitchListener() {
                @Override
                public void onChanged(SwitchView switchView, boolean isChecked) {
                    if(isChecked) {
                        AppSettings.saveBoolean("CpuCl0_global_volts", true, getActivity());
                        AppSettings.saveBoolean("CpuCl0_individual_volts", false, getActivity());
                        reload();
                    }else{
                        AppSettings.saveBoolean("CpuCl0_global_volts", false, getActivity());
                        AppSettings.saveBoolean("CpuCl0_individual_volts", true, getActivity());
                        AppSettings.saveInt("CpuCl0_seekbarPref_value", 16, getActivity());
                        reload();
                    }
                }
            });

            freqCard.addItem(voltControl);

            if (freqCard.size() > 0) {
                items.add(freqCard);
            }

            TitleView tunables = new TitleView();
            tunables.setText(getString(R.string.cpuCl0_volt));
            items.add(tunables);

            for (int i = 0; i < freqs.size(); i++) {
                SeekBarView seekbar = new SeekBarView();
                seekbarInit(seekbar, freqs.get(i), voltages.get(i), voltagesStock.get(i));
                mVoltages.add(seekbar);
            }
        }
        items.addAll(mVoltages);
    }

    private void seekbarProfInit(SeekBarView seekbar, final List<String> freqs, final List<String> voltages,
                                 final List<String> voltagesStock, List<String> progress) {

        Boolean enableSeekbar = AppSettings.getBoolean("CpuCl0_global_volts", true, getActivity());
        int global = AppSettings.getInt("CpuCl0_seekbarPref_value", 16, getActivity());

        int value = 0;
        for (int i = 0; i < progress.size(); i++) {
            if (i == global){
                value = i;
                break;
            }
        }

        seekbar.setTitle(getString(R.string.cpu_volt_profile));
        seekbar.setSummary(getString(R.string.cpu_volt_profile_summary));
        seekbar.setUnit(getString(R.string.mv));
        seekbar.setItems(progress);
        seekbar.setProgress(value);
        seekbar.setEnabled(enableSeekbar);
        if(!enableSeekbar) seekbar.setAlpha(0.4f);
        else seekbar.setAlpha(1f);
        seekbar.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                for (int i = 0; i < voltages.size(); i++) {
                    String volt = String.valueOf(Utils.strToFloat(voltagesStock.get(i)) + Utils.strToFloat(value));
                    String freq = freqs.get(i);
                    VoltageCl0.setVoltage(freq, volt, getActivity());
                    AppSettings.saveInt("CpuCl0_seekbarPref_value", position, getActivity());
                }
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        reload();
                    }
                }, 200);
            }
            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {
            }
        });
    }

    private void seekbarInit(SeekBarView seekbar, final String freq, String voltage,
                             String voltageStock) {

        int mStep = 6250;
        int mOffset = VoltageCl0.getOffset();
        float mMin = (Utils.strToFloat(voltageStock) - 100) * mOffset;
        float mMax = ((Utils.strToFloat(voltageStock) + 25) * mOffset) + mStep;

        List<String> progress = new ArrayList<>();
        for(float i = mMin ; i < mMax; i += mStep){
            String string = String.valueOf(i / mOffset);
            progress.add(string);
        }

        int value = 0;
        for (int i = 0; i < progress.size(); i++) {
           if (Objects.equals(progress.get(i), voltage)){
                value = i;
                break;
           }
        }

        Boolean enableSeekbar = AppSettings.getBoolean("CpuCl0_individual_volts", false, getActivity());

        seekbar.setTitle(freq + " " + getString(R.string.mhz));
        seekbar.setSummary(getString(R.string.def) + ": " + voltageStock + " " + getString(R.string.mv));
        seekbar.setUnit(getString(R.string.mv));
        seekbar.setItems(progress);
        seekbar.setProgress(value);
        seekbar.setEnabled(enableSeekbar);
        if(!enableSeekbar) seekbar.setAlpha(0.4f);
        else seekbar.setAlpha(1f);
        seekbar.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {

            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                VoltageCl0.setVoltage(freq, value, getActivity());
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        reload();
                    }
                }, 200);
            }

            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {
            }
        });
    }

    private void reload() {
        List<String> freqs = VoltageCl0.getFreqs();
        List<String> voltages = VoltageCl0.getVoltages();
        List<String> voltagesStock = VoltageCl0.getStockVoltages();

        if (freqs != null && voltages != null && voltagesStock != null) {
            for (int i = 0; i < mVoltages.size(); i++) {
                seekbarInit(mVoltages.get(i), freqs.get(i), voltages.get(i), voltagesStock.get(i));
            }
            List<String> progress = new ArrayList<>();
            for (float i = -100000f; i < 31250f; i += 6250) {
                String global = String.valueOf(i / VoltageCl0.getOffset());
                progress.add(global);
            }
            seekbarProfInit(mSeekbarProf, freqs, voltages, voltagesStock, progress);
        }
    }
}
