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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptGroup;
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.v8.renderscript.Type;
import android.view.Surface;
import android.view.Surface.OutOfResourcesException;
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
    private SurfaceTexture mSurfaceTexture;
    private ScriptGroup mGroup;
    private Surface mSurface;
    private ScriptRunner mScriptRunner;
    private Bitmap mBitmapOut;

    RsYuv(RenderScript rs) {
        mRS = rs;
        mYuv = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(mRS));
    }

    void setupSurface() {
    	if (mSurfaceTexture != null) {
    		mSurface = new Surface(mSurfaceTexture);
    		mScriptRunner = new ScriptRunner() {
				@Override
				public void execute(byte[] input) {
			        mAllocationIn.copyFrom(input);
		            mGroup.setOutput(mYuv.getKernelID(), mAllocationOut);
		            mGroup.execute();
		            
		            Canvas canvas = null;
					try {
					    Rect fill = new Rect(0, 0, mWidth, mHeight);
						canvas = mSurface.lockCanvas(null);
						canvas.drawBitmap(mBitmapOut, null, fill, null);
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (OutOfResourcesException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    finally {
                        mSurface.unlockCanvasAndPost(canvas);
					}
				}
			};
        }
    	else {
    		mSurface = null;
    		mScriptRunner = new ScriptRunner() {
				@Override
				public void execute(byte[] input) {}
			};
    	}
    }

    void reset(int width, int height) {
        if (mAllocationOut != null) {
            mAllocationOut.destroy();
        }

        android.util.Log.v("cpa", "reset " + width + ", " + height);

        if (mWidth != width
        		|| mHeight != height) {
        	if (mBitmapOut != null) {
        		mBitmapOut.recycle();
        	}
        	mBitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        
        mHeight = height;
        mWidth = width;

        mAllocationOut = Allocation.createFromBitmap(mRS, mBitmapOut);

        Type.Builder tb;
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

    interface ScriptRunner {
    	public void execute(byte[] input);
    }
    
    //private long mTiming[] = new long[50];
    //private int mTimingSlot = 0;

    void execute(byte[] yuv) {
    	mScriptRunner.execute(yuv);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        android.util.Log.v("cpa", "onSurfaceTextureAvailable " + surface);
        mSurfaceTexture = surface;
        setupSurface();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        android.util.Log.v("cpa", "onSurfaceTextureSizeChanged " + surface);
        mSurfaceTexture = surface;
        setupSurface();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        android.util.Log.v("cpa", "onSurfaceTextureDestroyed " + surface);
        mSurfaceTexture = surface;
        setupSurface();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}

