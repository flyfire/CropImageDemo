package com.solarexsoft.cropimage.callback;

import android.graphics.Bitmap;

/*
 * Creadted by houruhou on 2023/01/13 16:39
 */
public interface CropCallback extends Callback{
    void onSuccess(Bitmap cropped);
}
