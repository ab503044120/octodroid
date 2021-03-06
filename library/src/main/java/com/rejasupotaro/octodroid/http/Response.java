package com.rejasupotaro.octodroid.http;

import android.util.Log;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.rejasupotaro.octodroid.GsonProvider;
import com.rejasupotaro.octodroid.models.Event;
import com.squareup.okhttp.Headers;

import java.io.IOException;
import java.util.List;

import rx.Observable;

public class Response<T> {
    private T entity;
    private Headers headers;
    private int code;
    private String body;
    private Observable<Response<T>> next;
    private Pagination pagination;
    private Error error;

    public T entity() {
        return entity;
    }

    public Headers headers() {
        return headers;
    }

    public int code() {
        return code;
    }

    public boolean isSuccessful() {
        return code >= 200 && code < 300;
    }

    public String body() {
        return body;
    }

    public int nextPage() {
        return pagination.getNextLink().getPage();
    }

    public Observable<Response<T>> next() {
        return next;
    }

    public boolean hasNext() {
        return pagination.hasNext();
    }

    public void next(Observable<Response<T>> next) {
        this.next = next;
    }

    public boolean hasError() {
        return error != null;
    }

    public Error getError() {
        return error;
    }

    public static <T> Response<T> parse(com.squareup.okhttp.Response r, TypeToken<T> type) throws IOException {
        return parse(r.headers(), r.code(), r.body().string(), type);
    }

    public static <T> Response<T> parse(Headers headers, int statusCode, String body, TypeToken<T> type) throws IOException {
        Response<T> response = new Response<>();

        try {
            List<Event> events = GsonProvider.get().fromJson(body, new TypeToken<List<Event>>() {
            }.getType());
        } catch (Exception e) {
            Log.e("DEBUG", e.toString());
        }

        if (200 <= statusCode && statusCode < 300) {
            try {
                response.entity = GsonProvider.get().fromJson(body, type.getType());
            } catch (JsonSyntaxException e) {
                throw new JsonSyntaxException("Can't instantiate " + type.getRawType().getName() + " class from " + body, e);
            }
        } else {
            try {
                response.error = GsonProvider.get().fromJson(body, Error.class);
            } catch (JsonSyntaxException e) {
                throw new JsonSyntaxException("Can't instantiate Error class from " + body, e);
            }
        }

        response.headers = headers;
        response.code = statusCode;
        response.body = body;
        response.pagination = PaginationHeaderParser.parse(response);

        return response;
    }

    public static <T> Response<T> parse(Throwable unexpectedException, TypeToken<T> type) {
        Response<T> response = GsonProvider.get().fromJson(
                "{}",
                type.getType());
        response.error = new Error(unexpectedException);
        return response;
    }
}
