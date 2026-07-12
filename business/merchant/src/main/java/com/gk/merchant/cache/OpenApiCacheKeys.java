package com.gk.merchant.cache;
public final class OpenApiCacheKeys {
 public static String nonce(String m,String k){return "openapi:nonce:"+m+":"+k;} public static String rate(String m,long k){return "openapi:rate:"+m+":"+k;}
 public static String merchantApp(String id){return "openapi:merchant-app:"+id;} public static String auth(String id){return "openapi:auth:app:"+id;} private OpenApiCacheKeys(){}
}
