杩?6 寮犺〃鍙互鐞嗚В鎴?PSP 妯″潡鐨?3 灞傦細

```text
閰嶇疆灞傦細psp_provider / psp_method / psp_account / psp_route_rule
鏃ュ織灞傦細psp_request_log / psp_callback_log
涓氬姟灞傦細pay_order / payout_order / ledger_* 鍚庨潰鎵挎帴
```

**psp_provider**
琛ㄧず鈥滀笂娓镐笁鏂规敮浠樺叕鍙糕€濄€?

姣斿锛?

```text
GCASH_PSP
MAYA_PSP
XENDIT
PAYMONGO
```

瀹冭褰?PSP 鐨勫熀纭€璧勬枡锛?

```text
psp_code
psp_name
base_url
api_version
鏄惁鏀寔浠ｆ敹
鏄惁鏀寔浠ｄ粯
鐘舵€?
```

浣滅敤锛氬憡璇夌郴缁熲€滄湁鍝簺 PSP 鍙互鎺モ€濄€?

**psp_method**
琛ㄧず鈥滄煇涓?PSP 鏀寔鐨勫钩鍙版敮浠樻柟寮忔槧灏勨€濄€?

姣斿骞冲彴缁熶竴鏀粯鏂瑰紡鍙細

```text
GCASH
MAYA
BANK_TRANSFER
```

浣?PSP 閭ｈ竟鍙兘鍙細

```text
GCASH_WALLET
PH_GCASH
ewallet_gcash
```

鎵€浠ヨ鏄犲皠锛?

```text
骞冲彴 method_code = GCASH
PSP psp_method_code = GCASH_WALLET
country = PH
currency = PHP
direction = PAYIN
```

浣滅敤锛氭妸骞冲彴缁熶竴 API 鐨勬敮浠樻柟寮忕炕璇戞垚 PSP 鑷繁鐨勭紪鐮併€?

**psp_account**
琛ㄧず鈥滃钩鍙板湪 PSP 閭ｈ竟寮€鐨勫晢鎴峰彿/瀵嗛挜閰嶇疆鈥濄€?

涓€涓?PSP 鍏徃涓嬮潰锛屼綘鍙兘鏈変笉鍚屽晢鎴峰彿锛?

```text
绉熸埛绾?PSP 鍟嗘埛鍙?
鏌愪釜鍟嗘埛鐙珛 PSP 鍟嗘埛鍙?
涓嶅悓鍥藉/涓氬姟绾?PSP 鍟嗘埛鍙?
```

杩欓噷鏀撅細

```text
psp_account_no
api_key
api_secret
psp_public_key
callback_secret
```

浣滅敤锛氳皟鐢?PSP 鎺ュ彛鏃剁煡閬撶敤鍝釜 PSP 鍟嗘埛鍙枫€佸摢濂楀瘑閽ャ€?

**psp_route_rule**
琛ㄧず鈥滆繖绗旇鍗曞簲璇ヨ蛋鍝釜 PSP鈥濄€?

姣斿锛?

```text
tenant_id = 鑿插緥瀹剧鎴?
country = PH
currency = PHP
method_code = GCASH
direction = PAYIN
amount = 100
```

绯荤粺鏍规嵁璺敱瑙勫垯閫夊嚭锛?

```text
psp_provider
psp_method
psp_account
```

浣滅敤锛氭妸璁㈠崟浠庘€滃钩鍙扮粺涓€鏀粯鏂瑰紡鈥濊矾鐢卞埌鍏蜂綋 PSP銆?

V1 鍙互绠€鍗曟寜锛?

```text
tenant_id
country_code
currency
method_code
direction
status = 1
amount 鍦?min/max 涔嬮棿
priority 鏈€灏?
```

鍙栫涓€鏉°€?

**psp_request_log**
璁板綍鈥滃钩鍙拌姹?PSP鈥濈殑瀹屾暣鏃ュ織銆?

姣斿锛?

```text
鍒涘缓浠ｆ敹璁㈠崟
鍒涘缓浠ｄ粯璁㈠崟
鏌ヨ璁㈠崟鐘舵€?
閫€娆捐姹?
```

浼氳褰曪細

```text
璇锋眰URL
璇锋眰澶?
璇锋眰浣?
鍝嶅簲鐘舵€佺爜
鍝嶅簲浣?
鑰楁椂
鏄惁鎴愬姛
閿欒鐮?
PSP璁㈠崟鍙?
```

浣滅敤锛氭帓鏌?PSP 鎺ュ彛闂銆佸璐︺€佽拷韪鍗曘€?

娉ㄦ剰锛氳姹備綋鍜屽搷搴斾綋閲岀殑瀵嗛挜銆侀摱琛屽崱銆佹墜鏈哄彿绛夋晱鎰熶俊鎭鑴辨晱銆?

**psp_callback_log**
璁板綍鈥淧SP 鍥炶皟骞冲彴鈥濈殑鏃ュ織銆?

PSP 鍥炶皟鍙兘浼氾細

```text
閲嶅鍥炶皟
涔卞簭鍥炶皟
绛惧悕澶辫触
璁㈠崟涓嶅瓨鍦?
鐘舵€佸啿绐?
```

鎵€浠ュ繀椤讳繚瀛橈細

```text
headers
body
signature
楠岀鐘舵€?
澶勭悊鐘舵€?
PSP璁㈠崟鍙?
callback_id
body_hash
閿欒淇℃伅
```

浣滅敤锛氬箓绛夈€侀獙绛捐拷韪€佹帓閿欍€佸璁°€?

**浠ｆ敹涓氬姟娴佺▼**

```text
1. 鍟嗘埛璋冪敤缁熶竴 API 鍒涘缓浠ｆ敹璁㈠崟
   鈫?
2. 骞冲彴鍒涘缓 pay_order
   鈫?
3. 鏍规嵁璁㈠崟淇℃伅鍖归厤 psp_route_rule
   鈫?
4. 鎵惧埌 psp_provider
   鈫?
5. 鎵惧埌 psp_method锛屾妸骞冲彴 method_code 杞垚 PSP method_code
   鈫?
6. 鎵惧埌 psp_account锛屾嬁 PSP 鍟嗘埛鍙峰拰瀵嗛挜
   鈫?
7. 璋冪敤 PSP 涓嬪崟鎺ュ彛
   鈫?
8. 鍐?psp_request_log
   鈫?
9. PSP 杩斿洖鏀堕摱鍙伴摼鎺?/ 鏀粯鍙傛暟 / PSP璁㈠崟鍙?
   鈫?
10. 鏇存柊 pay_order 涓?PROCESSING
   鈫?
11. PSP 鏀粯鎴愬姛鍚庡洖璋冨钩鍙?
   鈫?
12. 鍐?psp_callback_log
   鈫?
13. 楠岀銆佸箓绛夈€佹牎楠岄噾棰濆拰璁㈠崟
   鈫?
14. 鏇存柊 pay_order 涓?SUCCESS
   鈫?
15. 璋冪敤 ledger 鍏ヨ处
   鈫?
16. 鍐?mq_outbox
   鈫?
17. 閫氱煡鍟嗘埛
```

**浠ｄ粯涓氬姟娴佺▼**

```text
1. 鍟嗘埛璋冪敤缁熶竴 API 鍒涘缓浠ｄ粯璁㈠崟
   鈫?
2. 骞冲彴鍒涘缓 payout_order
   鈫?
3. 鏍￠獙鍟嗘埛浣欓
   鈫?
4. ledger 鍐荤粨浣欓锛岀敓鎴?ledger_hold
   鈫?
5. 鍖归厤 psp_route_rule
   鈫?
6. 鎵惧埌 psp_provider / psp_method / psp_account
   鈫?
7. 璋冪敤 PSP 浠ｄ粯鎺ュ彛
   鈫?
8. 鍐?psp_request_log
   鈫?
9. 鏇存柊 payout_order 涓?PROCESSING
   鈫?
10. PSP 鍥炶皟鎴栧畾鏃舵煡璇㈢粨鏋?
   鈫?
11. 鍐?psp_callback_log 鎴?psp_request_log
   鈫?
12. 鎴愬姛锛歱ayout_order = SUCCESS锛宭edger 娑堣€楀喕缁?
   鈫?
13. 澶辫触锛歱ayout_order = FAILED锛宭edger 瑙ｅ喕
   鈫?
14. 鍐?mq_outbox
   鈫?
15. 閫氱煡鍟嗘埛
```

**杩欏嚑寮犺〃涔嬮棿鐨勫叧绯?*

```text
psp_provider
  鈫?
psp_method
  鈫?
psp_route_rule
  鈫?
psp_account
```

鍙互鐞嗚В鎴愶細

```text
psp_provider锛氳繖鏄皝
psp_method锛氬畠鏀寔浠€涔堟敮浠樻柟寮?
psp_account锛氭垜鐢ㄤ粈涔堣处鍙峰拰瀵嗛挜璋冪敤瀹?
psp_route_rule锛氫粈涔堣鍗曡蛋瀹?
psp_request_log锛氭垜璇锋眰瀹冨彂鐢熶簡浠€涔?
psp_callback_log锛氬畠鍥炶皟鎴戝彂鐢熶簡浠€涔?
```

PSP 妯″潡鍙礋璐ｈ繛鎺ヤ笂娓稿拰璁板綍浜や簰銆? 
鐪熸鐨勯挶璐﹀彉鍖栧繀椤昏蛋 `ledger_journal / ledger_entry / ledger_balance`锛屼笉瑕佽 PSP 妯″潡鐩存帴鏀逛綑棰濄€
