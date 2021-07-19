package com.artifex.mupdfdemo;

import android.graphics.PointF;

/**
 * Created by Mi on 3/31/21
 */
public class PointBean {
    private PointF pointF;
    private String tag;

    public PointBean(PointF pointF, String tag) {
        this.pointF = pointF;
        this.tag = tag;
    }

    public PointF getPointF() {
        return pointF;
    }

    public void setPointF(PointF pointF) {
        this.pointF = pointF;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
