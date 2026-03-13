package com.onesignal;

/* JADX INFO: loaded from: classes.dex */
class UserStateEmail extends UserState {
    @Override // com.onesignal.UserState
    protected void addDependFields() {
    }

    @Override // com.onesignal.UserState
    boolean isSubscribed() {
        return true;
    }

    UserStateEmail(String str, boolean z) {
        super("email" + str, z);
    }

    @Override // com.onesignal.UserState
    UserState newInstance(String str) {
        return new UserStateEmail(str, false);
    }
}
