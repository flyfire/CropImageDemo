package com.solarexsoft.cropimage.callback;

import android.net.Uri;

/*
 * Creadted by houruhou on 2023/01/13 16:41
 */
public interface SaveCallback extends Callback {
    void onSuccess(Uri uri);
}
