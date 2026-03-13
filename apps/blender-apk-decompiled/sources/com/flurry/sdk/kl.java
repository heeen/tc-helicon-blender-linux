package com.flurry.sdk;

import com.flurry.sdk.kn;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class kl<RequestObjectType, ResponseObjectType> extends kn {
    public a<RequestObjectType, ResponseObjectType> a;
    public RequestObjectType b;
    public kz<RequestObjectType> c;
    public kz<ResponseObjectType> d;
    private ResponseObjectType x;

    public interface a<RequestObjectType, ResponseObjectType> {
        void a(kl<RequestObjectType, ResponseObjectType> klVar, ResponseObjectType responseobjecttype);
    }

    @Override // com.flurry.sdk.kn, com.flurry.sdk.lw
    public final void a() {
        this.k = new kn.c() { // from class: com.flurry.sdk.kl.1
            @Override // com.flurry.sdk.kn.c
            public final void a(OutputStream outputStream) throws Exception {
                if (kl.this.b == null || kl.this.c == null) {
                    return;
                }
                kl.this.c.a(outputStream, kl.this.b);
            }

            @Override // com.flurry.sdk.kn.c
            public final void a(kn knVar, InputStream inputStream) throws Exception {
                if (knVar.d() && kl.this.d != null) {
                    kl.this.x = kl.this.d.a(inputStream);
                }
            }

            @Override // com.flurry.sdk.kn.c
            public final void a(kn knVar) {
                kl.d(kl.this);
            }
        };
        super.a();
    }

    static /* synthetic */ void d(kl klVar) {
        if (klVar.a == null || klVar.b()) {
            return;
        }
        klVar.a.a(klVar, klVar.x);
    }
}
