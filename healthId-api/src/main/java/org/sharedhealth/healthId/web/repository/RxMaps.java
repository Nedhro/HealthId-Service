package org.sharedhealth.healthId.web.repository;

import com.datastax.driver.core.ResultSet;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

public class RxMaps {

    public static <T> Func1<ResultSet, Observable<? extends T>> respondOnNext(final T value) {
        return new Func1<ResultSet, Observable<? extends T>>() {
            @Override
            public Observable<? extends T> call(ResultSet rows) {
                return Observable.just(value);
            }
        };
    }

    public static <T> Func0<Observable<? extends T>> completeResponds() {
        return new Func0<Observable<? extends T>>() {
            @Override
            public Observable<? extends T> call() {
                return Observable.empty();
            }
        };
    }

    public static <T> Func1<Throwable, Observable<? extends T>> forwardError() {
        return new Func1<Throwable, Observable<? extends T>>() {
            @Override
            public Observable<? extends T> call(Throwable throwable) {
                return Observable.error(throwable);
            }
        };
    }
}
