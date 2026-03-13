package com.onesignal;

/* JADX INFO: loaded from: classes.dex */
class OSEmailSubscriptionChangedInternalObserver {
    OSEmailSubscriptionChangedInternalObserver() {
    }

    void changed(OSEmailSubscriptionState oSEmailSubscriptionState) {
        fireChangesToPublicObserver(oSEmailSubscriptionState);
    }

    static void fireChangesToPublicObserver(OSEmailSubscriptionState oSEmailSubscriptionState) {
        OSEmailSubscriptionStateChanges oSEmailSubscriptionStateChanges = new OSEmailSubscriptionStateChanges();
        oSEmailSubscriptionStateChanges.from = OneSignal.lastEmailSubscriptionState;
        oSEmailSubscriptionStateChanges.to = (OSEmailSubscriptionState) oSEmailSubscriptionState.clone();
        if (OneSignal.getEmailSubscriptionStateChangesObserver().notifyChange(oSEmailSubscriptionStateChanges)) {
            OneSignal.lastEmailSubscriptionState = (OSEmailSubscriptionState) oSEmailSubscriptionState.clone();
            OneSignal.lastEmailSubscriptionState.persistAsFrom();
        }
    }
}
