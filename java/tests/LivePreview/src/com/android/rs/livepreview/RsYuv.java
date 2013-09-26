/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.rs.livepreview;


import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.renderscript.*;
import android.view.Surface;
import android.view.TextureView;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RsYuv implements TextureView.SurfaceTextureListener
{
    private int mHeight;
    private int mWidth;
    private RenderScript mRS;
    private Allocation mAllocationOut;
    private Allocation mAllocationIn;
    private ScriptIntrinsicYuvToRGB mYuv;
    private boolean mHaveSurface;
    private SurfaceTexture mSurface;
    private ScriptGroup mGroup;

    RsYuv(RenderScript rs) {
        mRS = rs;
        mYuv = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(mRS));
    }

    void setupSurface() {
        if (mAllocationOut != null) {
            mAllocationOut.setSurface(new Surface(mSurface));
        }
        if (mSurface != null) {
            mHaveSurface = true;
        } else {
            mHaveSurface = false;
        }
    }

    void reset(int width, int height) {
        if (mAllocationOut != null) {
            mAllocationOut.destroy();
        }

        android.util.Log.v("cpa", "reset " + width + ", " + height);
        mHeight = height;
        mWidth = width;

        Type.Builder tb = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        tb.setX(mWidth);
        tb.setY(mHeight);
        Type t = tb.create();
        mAllocationOut = Allocation.createTyped(mRS, t, Allocation.USAGE_SCRIPT |
                                                        Allocation.USAGE_IO_OUTPUT);


        tb = new Type.Builder(mRS, Element.createPixel(mRS, Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV));
        tb.setX(mWidth);
        tb.setY(mHeight);
        tb.setYuvFormat(android.graphics.ImageFormat.NV21);
        mAllocationIn = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);
        mYuv.setInput(mAllocationIn);
        setupSurface();


        ScriptGroup.Builder b = new ScriptGroup.Builder(mRS);
        b.addKernel(mYuv.getKernelID());
        mGroup = b.create();
    }

    public int getWidth() {
        return mWidth;
    }
    public int getHeight() {
        return mHeight;
    }

    //private long mTiming[] = new long[50];
    //private int mTimingSlot = 0;

    void execute(byte[] yuv) {
        mAllocationIn.copyFrom(yuv);
        if (mHaveSurface) {
            mGroup.setOutput(mYuv.getKernelID(), mAllocationOut);
            mGroup.execute();

            //mYuv.forEach(mAllocationOut);
            //mScript.forEach_root(mAllocationOut, mAllocationOut);
            mAllocationOut.ioSend();
        }
    }



    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        android.util.Log.v("cpa", "onSurfaceTextureAvailable " + surface);
        mSurface = surface;
        setupSurface();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        android.util.Log.v("cpa", "onSurfaceTextureSizeChanged " + surface);
        mSurface = surface;
        setupSurface();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        android.util.Log.v("cpa", "onSurfaceTextureDestroyed " + surface);
        mSurface = surface;
        setupSurface();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}

