package com.google.android.gms.dynamic;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
@SuppressLint({"NewApi"})
public final class zzj extends zzl {
    private Fragment zzhcy;

    private zzj(Fragment fragment) {
        this.zzhcy = fragment;
    }

    public static zzj zza(Fragment fragment) {
        if (fragment != null) {
            return new zzj(fragment);
        }
        return null;
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final Bundle getArguments() {
        return this.zzhcy.getArguments();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final int getId() {
        return this.zzhcy.getId();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final boolean getRetainInstance() {
        return this.zzhcy.getRetainInstance();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final String getTag() {
        return this.zzhcy.getTag();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final int getTargetRequestCode() {
        return this.zzhcy.getTargetRequestCode();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final boolean getUserVisibleHint() {
        return this.zzhcy.getUserVisibleHint();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final IObjectWrapper getView() {
        return zzn.zzz(this.zzhcy.getView());
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final boolean isAdded() {
        return this.zzhcy.isAdded();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final boolean isDetached() {
        return this.zzhcy.isDetached();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final boolean isHidden() {
        return this.zzhcy.isHidden();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final boolean isInLayout() {
        return this.zzhcy.isInLayout();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final boolean isRemoving() {
        return this.zzhcy.isRemoving();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final boolean isResumed() {
        return this.zzhcy.isResumed();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final boolean isVisible() {
        return this.zzhcy.isVisible();
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final void setHasOptionsMenu(boolean z) {
        this.zzhcy.setHasOptionsMenu(z);
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final void setMenuVisibility(boolean z) {
        this.zzhcy.setMenuVisibility(z);
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final void setRetainInstance(boolean z) {
        this.zzhcy.setRetainInstance(z);
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final void setUserVisibleHint(boolean z) {
        this.zzhcy.setUserVisibleHint(z);
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final void startActivity(Intent intent) {
        this.zzhcy.startActivity(intent);
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final void startActivityForResult(Intent intent, int i) {
        this.zzhcy.startActivityForResult(intent, i);
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final IObjectWrapper zzarh() {
        return zzn.zzz(this.zzhcy.getActivity());
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final zzk zzari() {
        return zza(this.zzhcy.getParentFragment());
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final IObjectWrapper zzarj() {
        return zzn.zzz(this.zzhcy.getResources());
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final zzk zzark() {
        return zza(this.zzhcy.getTargetFragment());
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final void zzw(IObjectWrapper iObjectWrapper) {
        this.zzhcy.registerForContextMenu((View) zzn.zzy(iObjectWrapper));
    }

    @Override // com.google.android.gms.dynamic.zzk
    public final void zzx(IObjectWrapper iObjectWrapper) {
        this.zzhcy.unregisterForContextMenu((View) zzn.zzy(iObjectWrapper));
    }
}
