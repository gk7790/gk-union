package com.gk.psp.support;
public final class PspCacheKeys {
 public static String methodDict(Long id){return "psp:method-code:dict:"+all(id);} public static String methodDictPattern(){return "psp:method-code:dict:*";}
 public static String accountDict(Long id){return "psp:account:dict:"+all(id);} public static String accountDictPattern(){return "psp:account:dict:*";}
 public static String callbackWhitelist(String c){return "psp:callback-ip-whitelist:"+all(c);} public static String callbackWhitelistPattern(){return "psp:callback-ip-whitelist:*";}
 public static String callbackAccount(String n){return "psp:callback-account:"+all(n);} public static String callbackAccountPattern(){return "psp:callback-account:*";}
 public static String balance(Long t,Long a){return "payment:psp-account:balance:"+all(t)+":"+all(a);} public static String balancePattern(){return "payment:psp-account:balance:*";}
 public static String deeplinkTest(String token){return "tools:deeplink-test:"+token;} private static String all(Object v){return v==null?"*":String.valueOf(v);} private PspCacheKeys(){}
}
