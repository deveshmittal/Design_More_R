package com.app.designmore.mvp.presenter;

import android.content.Context;
import android.util.Log;
import com.app.designmore.mvp.ExecutorCallback;
import com.app.designmore.mvp.model.AddressInteractor;
import com.app.designmore.mvp.model.AddressInteractorImp;
import com.app.designmore.mvp.viewinterface.AddressView;
import com.app.designmore.retrofit.entity.Province;
import com.app.designmore.rxAndroid.schedulers.AndroidSchedulers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

/**
 * Created by Joker on 2015/9/9.
 */
public class AddressPresenterImp implements AddressPresenter, ExecutorCallback {

  private static final String TAG = AddressPresenterImp.class.getSimpleName();
  private Context context;
  private AddressView addressView;
  private List<Province> provinces = new ArrayList<>();
  private Subscription subscribe = Subscriptions.empty();

  private AddressInteractor addressInteractor;

  public AddressPresenterImp() {
  }

  @Override public void attach(Context context, AddressView addressView) {
    this.context = context;
    this.addressView = addressView;
    this.addressInteractor = new AddressInteractorImp(context);
  }

  /*@Override public void showPicker() {
    if (provinces.size() > 0) {
      addressView.onInflateFinish(provinces);
    } else {

      provinces = Observable.create(new Observable.OnSubscribe<List<Province>>() {
        @Override public void call(final Subscriber<? super List<Province>> subscriber) {

          subscribe = Schedulers.io().createWorker().schedule(new Action0() {
            @Override public void call() {
              InputStream inputStream = null;
              try {
                inputStream = context.getResources().getAssets().open("address.txt");
                byte[] arrayOfByte = new byte[inputStream.available()];
                inputStream.read(arrayOfByte);
                JSONArray jsonList = new JSONArray(new String(arrayOfByte, "UTF-8"));
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                    .serializeNulls()
                    .create();
                for (int i = 0; i < jsonList.length(); i++) {
                  provinces.add(gson.fromJson(jsonList.getString(i), Province.class));
                }
              } catch (Exception e) {
                subscriber.onError(e);
              } finally {
                if (inputStream != null) {
                  try {
                    inputStream.close();
                  } catch (IOException e) {
                    Observable.error(e);
                  }
                }
              }
              subscriber.onNext(provinces);
              subscriber.onCompleted();
            }
          });
        }
      }).toBlocking().single();

      if (provinces != null && provinces.size() > 0) {
        addressView.onInflateFinish(provinces);
      } else {
        addressView.showError();
      }
    }
  }*/

  @Override public void showPicker() {

    if (provinces.size() > 0) {
      addressView.onInflateFinish(provinces);
    } else {
      this.addressInteractor.inflateAddressItems(AddressPresenterImp.this);
    }
  }

  @Override public void onDataFinish(Observable<List<Province>> observable) {


    /*doOnSubscribe() 执行线程取决于最近的下行subscribeOn()*/
    subscribe = observable.doOnSubscribe(new Action0() {
      @Override public void call() {
        addressView.showProgress();
      }
    })
        .finallyDo(new Action0() {
          @Override public void call() {

            addressView.hideProgress();
          }
        })
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<List<Province>>() {
          @Override public void call(List<Province> provinces) {

            AddressPresenterImp.this.provinces.clear();
            AddressPresenterImp.this.provinces.addAll(provinces);
            addressView.onInflateFinish(provinces);
          }
        }, new Action1<Throwable>() {
          @Override public void call(Throwable throwable) {
            addressView.showError();
          }
        });
  }

  @Override public void detach() {
    if (subscribe != null && !subscribe.isUnsubscribed()) subscribe.unsubscribe();
  }
}
