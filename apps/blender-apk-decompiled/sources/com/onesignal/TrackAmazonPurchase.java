package com.onesignal;

import android.content.Context;
import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.RequestId;
import com.amazon.device.iap.model.UserDataResponse;
import com.onesignal.OneSignal;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class TrackAmazonPurchase {
    private boolean canTrack;
    private Context context;
    private Field listenerHandlerField;
    private Object listenerHandlerObject;
    private OSPurchasingListener osPurchasingListener;

    TrackAmazonPurchase(Context context) {
        this.canTrack = false;
        this.context = context;
        try {
            Class<?> cls = Class.forName("com.amazon.device.iap.internal.d");
            this.listenerHandlerObject = cls.getMethod("d", new Class[0]).invoke(null, new Object[0]);
            this.listenerHandlerField = cls.getDeclaredField("f");
            this.listenerHandlerField.setAccessible(true);
            this.osPurchasingListener = new OSPurchasingListener(this, null);
            this.osPurchasingListener.orgPurchasingListener = (PurchasingListener) this.listenerHandlerField.get(this.listenerHandlerObject);
            this.canTrack = true;
            setListener();
        } catch (Throwable th) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error adding Amazon IAP listener.", th);
        }
    }

    private void setListener() {
        PurchasingService.registerListener(this.context, this.osPurchasingListener);
    }

    void checkListener() {
        if (this.canTrack) {
            try {
                PurchasingListener purchasingListener = (PurchasingListener) this.listenerHandlerField.get(this.listenerHandlerObject);
                if (purchasingListener != this.osPurchasingListener) {
                    this.osPurchasingListener.orgPurchasingListener = purchasingListener;
                    setListener();
                }
            } catch (Throwable unused) {
            }
        }
    }

    private class OSPurchasingListener implements PurchasingListener {
        private String currentMarket;
        private RequestId lastRequestId;
        PurchasingListener orgPurchasingListener;

        private OSPurchasingListener() {
        }

        /* synthetic */ OSPurchasingListener(TrackAmazonPurchase trackAmazonPurchase, AnonymousClass1 anonymousClass1) {
            this();
        }

        /* JADX WARN: Removed duplicated region for block: B:53:0x0094  */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        private java.lang.String marketToCurrencyCode(java.lang.String r2) {
            /*
                r1 = this;
                int r1 = r2.hashCode()
                r0 = 2100(0x834, float:2.943E-42)
                if (r1 == r0) goto L89
                r0 = 2128(0x850, float:2.982E-42)
                if (r1 == r0) goto L7e
                r0 = 2142(0x85e, float:3.002E-42)
                if (r1 == r0) goto L74
                r0 = 2177(0x881, float:3.05E-42)
                if (r1 == r0) goto L6a
                r0 = 2222(0x8ae, float:3.114E-42)
                if (r1 == r0) goto L60
                r0 = 2252(0x8cc, float:3.156E-42)
                if (r1 == r0) goto L56
                r0 = 2267(0x8db, float:3.177E-42)
                if (r1 == r0) goto L4c
                r0 = 2347(0x92b, float:3.289E-42)
                if (r1 == r0) goto L42
                r0 = 2374(0x946, float:3.327E-42)
                if (r1 == r0) goto L38
                r0 = 2718(0xa9e, float:3.809E-42)
                if (r1 == r0) goto L2e
                goto L94
            L2e:
                java.lang.String r1 = "US"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L94
                r1 = 0
                goto L95
            L38:
                java.lang.String r1 = "JP"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L94
                r1 = 6
                goto L95
            L42:
                java.lang.String r1 = "IT"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L94
                r1 = 5
                goto L95
            L4c:
                java.lang.String r1 = "GB"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L94
                r1 = 1
                goto L95
            L56:
                java.lang.String r1 = "FR"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L94
                r1 = 3
                goto L95
            L60:
                java.lang.String r1 = "ES"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L94
                r1 = 4
                goto L95
            L6a:
                java.lang.String r1 = "DE"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L94
                r1 = 2
                goto L95
            L74:
                java.lang.String r1 = "CA"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L94
                r1 = 7
                goto L95
            L7e:
                java.lang.String r1 = "BR"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L94
                r1 = 8
                goto L95
            L89:
                java.lang.String r1 = "AU"
                boolean r1 = r2.equals(r1)
                if (r1 == 0) goto L94
                r1 = 9
                goto L95
            L94:
                r1 = -1
            L95:
                switch(r1) {
                    case 0: goto Lad;
                    case 1: goto Laa;
                    case 2: goto La7;
                    case 3: goto La7;
                    case 4: goto La7;
                    case 5: goto La7;
                    case 6: goto La4;
                    case 7: goto La1;
                    case 8: goto L9e;
                    case 9: goto L9b;
                    default: goto L98;
                }
            L98:
                java.lang.String r1 = ""
                return r1
            L9b:
                java.lang.String r1 = "AUD"
                return r1
            L9e:
                java.lang.String r1 = "BRL"
                return r1
            La1:
                java.lang.String r1 = "CDN"
                return r1
            La4:
                java.lang.String r1 = "JPY"
                return r1
            La7:
                java.lang.String r1 = "EUR"
                return r1
            Laa:
                java.lang.String r1 = "GBP"
                return r1
            Lad:
                java.lang.String r1 = "USD"
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.onesignal.TrackAmazonPurchase.OSPurchasingListener.marketToCurrencyCode(java.lang.String):java.lang.String");
        }

        public void onProductDataResponse(ProductDataResponse productDataResponse) {
            if (this.lastRequestId != null && this.lastRequestId.toString().equals(productDataResponse.getRequestId().toString())) {
                try {
                    if (AnonymousClass1.$SwitchMap$com$amazon$device$iap$model$ProductDataResponse$RequestStatus[productDataResponse.getRequestStatus().ordinal()] != 1) {
                        return;
                    }
                    JSONArray jSONArray = new JSONArray();
                    Map productData = productDataResponse.getProductData();
                    Iterator it = productData.keySet().iterator();
                    while (it.hasNext()) {
                        Product product = (Product) productData.get((String) it.next());
                        JSONObject jSONObject = new JSONObject();
                        jSONObject.put("sku", product.getSku());
                        jSONObject.put("iso", marketToCurrencyCode(this.currentMarket));
                        String price = product.getPrice();
                        if (!price.matches("^[0-9]")) {
                            price = price.substring(1);
                        }
                        jSONObject.put("amount", price);
                        jSONArray.put(jSONObject);
                    }
                    OneSignal.sendPurchases(jSONArray, false, null);
                    return;
                } catch (Throwable th) {
                    th.printStackTrace();
                    return;
                }
            }
            if (this.orgPurchasingListener != null) {
                this.orgPurchasingListener.onProductDataResponse(productDataResponse);
            }
        }

        public void onPurchaseResponse(PurchaseResponse purchaseResponse) {
            try {
                if (purchaseResponse.getRequestStatus() == PurchaseResponse.RequestStatus.SUCCESSFUL) {
                    this.currentMarket = purchaseResponse.getUserData().getMarketplace();
                    HashSet hashSet = new HashSet();
                    hashSet.add(purchaseResponse.getReceipt().getSku());
                    this.lastRequestId = PurchasingService.getProductData(hashSet);
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
            if (this.orgPurchasingListener != null) {
                this.orgPurchasingListener.onPurchaseResponse(purchaseResponse);
            }
        }

        public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse purchaseUpdatesResponse) {
            if (this.orgPurchasingListener != null) {
                this.orgPurchasingListener.onPurchaseUpdatesResponse(purchaseUpdatesResponse);
            }
        }

        public void onUserDataResponse(UserDataResponse userDataResponse) {
            if (this.orgPurchasingListener != null) {
                this.orgPurchasingListener.onUserDataResponse(userDataResponse);
            }
        }
    }

    /* JADX INFO: renamed from: com.onesignal.TrackAmazonPurchase$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$amazon$device$iap$model$ProductDataResponse$RequestStatus = new int[ProductDataResponse.RequestStatus.values().length];

        static {
            try {
                $SwitchMap$com$amazon$device$iap$model$ProductDataResponse$RequestStatus[ProductDataResponse.RequestStatus.SUCCESSFUL.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
        }
    }
}
