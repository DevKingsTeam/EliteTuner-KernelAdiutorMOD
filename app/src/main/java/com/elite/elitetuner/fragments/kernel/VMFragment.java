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

import android.text.InputType;

import com.elite.elitetuner.R;
import com.elite.elitetuner.fragments.ApplyOnBootFragment;

import com.elite.elitetuner.fragments.recyclerview.RecyclerViewFragment;
import com.elite.elitetuner.utils.AppSettings;
import com.elite.elitetuner.utils.Device;
import com.elite.elitetuner.utils.kernel.vm.VM;
import com.elite.elitetuner.utils.kernel.vm.ZRAM;
import com.elite.elitetuner.utils.kernel.vm.ZSwap;
import com.elite.elitetuner.views.recyclerview.CardView;
import com.elite.elitetuner.views.recyclerview.GenericSelectView2;
import com.elite.elitetuner.views.recyclerview.ProgressBarView;
import com.elite.elitetuner.views.recyclerview.RecyclerViewItem;
import com.elite.elitetuner.views.recyclerview.SeekBarView;
import com.elite.elitetuner.views.recyclerview.SwitchView;
import com.elite.elitetuner.views.recyclerview.TitleView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by willi on 29.06.16.
 */
public class VMFragment extends RecyclerViewFragment {

    private List<GenericSelectView2> mVMs = new ArrayList<>();
    private boolean mCompleteList;

    private Device.MemInfo mMemInfo;
    private ProgressBarView swap;
    private ProgressBarView mem;

    @Override
    protected void init() {
        super.init();

        addViewPagerFragment(ApplyOnBootFragment.newInstance(this));
        mMemInfo = Device.MemInfo.getInstance();
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {

        memBarsInit(items);
        if (ZRAM.supported()) {
            zramInit(items);
        }
        zswapInit(items);
        vmTunablesInit(items);
    }

    private void memBarsInit (List<RecyclerViewItem> items){
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.memory));

        long swap_total = mMemInfo.getItemMb("SwapTotal");
        long swap_progress = swap_total - mMemInfo.getItemMb("SwapFree");

        swap = new ProgressBarView();
        swap.setTitle("SWAP");
        swap.setItems(swap_total, swap_progress);
        swap.setUnit(getResources().getString(R.string.mb));
        swap.setProgressColor(getResources().getColor(R.color.blueAccent));
        card.addItem(swap);

        long mem_total = mMemInfo.getItemMb("MemTotal");
        long mem_progress = mem_total - (mMemInfo.getItemMb("Cached") + mMemInfo.getItemMb("MemFree"));

        mem = new ProgressBarView();
        mem.setTitle("RAM");
        mem.setItems(mem_total, mem_progress);
        mem.setUnit(getResources().getString(R.string.mb));
        mem.setProgressColor(getResources().getColor(R.color.orangeAccent));
        card.addItem(mem);

        items.add(card);
    }

    private void vmTunablesInit (List<RecyclerViewItem> items){
        final CardView CardVm = new CardView(getActivity());
        CardVm.setTitle(getString(R.string.vm_tunables));

        CardVmTunablesInit(CardVm);

        if (CardVm.size() > 0) {
            items.add(CardVm);
        }
    }

    private void CardVmTunablesInit(final CardView card) {
        card.clearItems();
        mVMs.clear();

        mCompleteList = AppSettings.getBoolean("vm_show_complete_list", false, getActivity());

        SwitchView sv = new SwitchView();
        sv.setTitle(getString(R.string.vm_tun_switch_title));
        sv.setSummary(getString(R.string.vm_tun_switch_summary));
        sv.setChecked(mCompleteList);
        sv.addOnSwitchListener((switchView, isChecked) -> {
            mCompleteList = isChecked;
            AppSettings.saveBoolean("vm_show_complete_list", mCompleteList, getActivity());
            getHandler().postDelayed(() -> CardVmTunablesInit(card), 250);
        });

        card.addItem(sv);


        TitleView tit = new TitleView();
        if (mCompleteList) {
            tit.setText(getString(R.string.vm_tun_tit_all));
        }
        else {
            tit.setText(getString(R.string.vm_tun_tit_common));
        }

        card.addItem(tit);

        for (int i = 0; i < VM.size(mCompleteList); i++) {
            GenericSelectView2 vm = new GenericSelectView2();
            vm.setTitle(VM.getName(i, mCompleteList));
            vm.setValue(VM.getValue(i, mCompleteList));
            vm.setValueRaw(vm.getValue());
            vm.setInputType(InputType.TYPE_CLASS_NUMBER);

            final int position = i;
            vm.setOnGenericValueListener((genericSelectView, value) -> {
                VM.setValue(value, position, getActivity(), mCompleteList);
                genericSelectView.setValue(value);
                refreshVMs();
            });

            card.addItem(vm);
            mVMs.add(vm);
        }
    }

    private void zramInit(List<RecyclerViewItem> items) {
        CardView zramCard = new CardView(getActivity());
        zramCard.setTitle(getString(R.string.zram));

        SeekBarView zram = new SeekBarView();
        zram.setTitle(getString(R.string.disksize));
        zram.setSummary(getString(R.string.disksize_summary));
        zram.setUnit(getString(R.string.mb));
        zram.setMax(2048);
        zram.setOffset(10);
        zram.setProgress(ZRAM.getDisksize() / 10);
        zram.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                ZRAM.setDisksize(position * 10, getActivity());
            }

            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {
            }
        });

        zramCard.addItem(zram);

        if (zramCard.size() > 0) {
            items.add(zramCard);
        }
    }

    private void zswapInit(List<RecyclerViewItem> items) {
        CardView zswapCard = new CardView(getActivity());
        zswapCard.setTitle(getString(R.string.zswap));

        if (ZSwap.hasEnable()) {
            SwitchView zswap = new SwitchView();
            zswap.setTitle(getString(R.string.zswap));
            zswap.setSummary(getString(R.string.zswap_summary));
            zswap.setChecked(ZSwap.isEnabled());
            zswap.addOnSwitchListener((switchView, isChecked)
                    -> ZSwap.enable(isChecked, getActivity()));

            zswapCard.addItem(zswap);
        }

        if (ZSwap.hasMaxPoolPercent()) {
            if(!AppSettings.getBoolean("memory_pool_percent", false, getActivity())) {
                SeekBarView maxPoolPercent = new SeekBarView();
                maxPoolPercent.setTitle(getString(R.string.memory_pool));
                maxPoolPercent.setSummary(getString(R.string.memory_pool_summary));
                maxPoolPercent.setUnit("%");
                maxPoolPercent.setMax(ZSwap.getStockMaxPoolPercent() / 10);
                maxPoolPercent.setProgress(ZSwap.getMaxPoolPercent() / 10);
                maxPoolPercent.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                    @Override
                    public void onStop(SeekBarView seekBarView, int position, String value) {
                        ZSwap.setMaxPoolPercent(position * 10, getActivity());
                    }

                    @Override
                    public void onMove(SeekBarView seekBarView, int position, String value) {
                    }
                });

                zswapCard.addItem(maxPoolPercent);

            } else {
                SeekBarView maxPoolPercent = new SeekBarView();
                maxPoolPercent.setTitle(getString(R.string.memory_pool));
                maxPoolPercent.setSummary(getString(R.string.memory_pool_summary));
                maxPoolPercent.setUnit("%");
                maxPoolPercent.setMax(ZSwap.getStockMaxPoolPercent());
                maxPoolPercent.setProgress(ZSwap.getMaxPoolPercent());
                maxPoolPercent.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                    @Override
                    public void onStop(SeekBarView seekBarView, int position, String value) {
                        ZSwap.setMaxPoolPercent(position, getActivity());
                    }

                    @Override
                    public void onMove(SeekBarView seekBarView, int position, String value) {
                    }
                });

                zswapCard.addItem(maxPoolPercent);
            }
        }

        if (ZSwap.hasMaxCompressionRatio()) {
            SeekBarView maxCompressionRatio = new SeekBarView();
            maxCompressionRatio.setTitle(getString(R.string.maximum_compression_ratio));
            maxCompressionRatio.setSummary(getString(R.string.maximum_compression_ratio_summary));
            maxCompressionRatio.setUnit("%");
            maxCompressionRatio.setProgress(ZSwap.getMaxCompressionRatio());
            maxCompressionRatio.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    ZSwap.setMaxCompressionRatio(position, getActivity());
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });

            zswapCard.addItem(maxCompressionRatio);
        }

        if (zswapCard.size() > 0) {
            items.add(zswapCard);
        }
    }

    private void refreshVMs() {
        getHandler().postDelayed(() -> {
            for (int i = 0; i < mVMs.size(); i++) {
                mVMs.get(i).setValue(VM.getValue(i, mCompleteList));
                mVMs.get(i).setValueRaw(mVMs.get(i).getValue());
            }
        }, 250);
    }
/*
    protected void refresh() {
        super.refresh();

        if (swap != null) {
            long total = mMemInfo.getItemMb("SwapTotal");
            long progress = total - mMemInfo.getItemMb("SwapFree");
            swap.setItems(total, progress);
        }
        if (mem != null) {
            long total = mMemInfo.getItemMb("MemTotal");
            long progress = total - (mMemInfo.getItemMb("Cached") + mMemInfo.getItemMb("MemFree"));
            mem.setItems(total, progress);
        }
    }
*/
}
