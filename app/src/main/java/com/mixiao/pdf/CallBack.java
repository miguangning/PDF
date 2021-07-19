package com.mixiao.pdf;

/**
 * Created by Mi on 7/19/21
 */
public interface CallBack<T> {
    void onSuccess(T t);

    void onError(String error);
}
