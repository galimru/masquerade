package com.haulmont.masquerade.restapi;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.annotation.Nonnull;

public class ServiceGenerator {

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    private static LoadingCache<String, Retrofit.Builder> builders = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, Retrofit.Builder>() {
                @Override
                public Retrofit.Builder load(@Nonnull String baseUrl) {
                    return new Retrofit.Builder()
                                    .baseUrl(baseUrl)
                                    .addConverterFactory(GsonConverterFactory.create());
                }
            });

    public static <S> S createService(String baseUrl, Class<S> serviceClass, String clientId, String clientSecret) {
        if (!Strings.isNullOrEmpty(clientId)
                && !Strings.isNullOrEmpty(clientSecret)) {
            String authToken = Credentials.basic(clientId, clientSecret);
            return createService(baseUrl, serviceClass, authToken);
        }

        return createService(baseUrl, serviceClass, null, null);
    }

    public static <S> S createService(String baseUrl, Class<S> serviceClass, AccessToken accessToken) {
        return createService(baseUrl, serviceClass, accessToken.toAuthorizationToken());
    }

    public static <S> S createService(String baseUrl, Class<S> serviceClass, final String authToken) {
        Retrofit.Builder builder = builders.getUnchecked(baseUrl);

        if (!Strings.isNullOrEmpty(authToken)) {
            AuthenticationInterceptor interceptor =
                    new AuthenticationInterceptor(authToken);

            if (!httpClient.interceptors().contains(interceptor)) {
                httpClient.addInterceptor(interceptor);

                builder.client(httpClient.build());
                Retrofit retrofit = builder.build();
                return retrofit.create(serviceClass);
            }
        }

        return builder.build().create(serviceClass);
    }
}