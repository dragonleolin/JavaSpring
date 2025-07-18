# 可呼叫API
1. 取得今日股票資訊: http://localhost:8082/stock
    ```
        body:{ "codes": ["2330","2454","2912","00830","0050","0056","0052","00919","006208"] }
    ```
2. 取歷史資料組成圖表: http://localhost:8082/stock/chart/0050?from=20250630&to=20250707
3. 查詢所有 Redis 快取: http://localhost:8082/stock/cache
4. 查詢指定代碼所有快取紀錄: http://localhost:8082/stock/cache/0050
5. 查詢最新一筆快取（依代碼）: http://localhost:8082/stock/cache/0050/latest
6. 刪除指定快取: http://localhost:8082/stock/cache/0050/20250630
7. 清空所有快取: http://localhost:8082/stock/cache/clear
8. 取搜尋紀錄產圓餅圖: http://localhost:8082/stock/chart/query-count

# [Redis 教學]
1. 直接到 https://github.com/MicrosoftArchive/redis/releases 抓取 Redis-x64-3.0.504.msi 安裝
2. 修改配置檔案，編輯配置檔案redis.windows.conf，修改以下內容：
    ``` bind 127.0.0.1 ---> # bind 127.0.0.1
        protected-mode yes ---> protected-mode no
        # requirepass foobared ---> requirepass 123456  #123456為密碼可任意更換
        port 6379 ---> port 7379  # 將埠改為 7379
        maxmemory 4294967296  # 配置記憶體為 4G 單位是 byte，也可以配置成其他大小，推薦大小為4G（需新增內容）
        maxmemory-policy noeviction # 代表Redis記憶體達到最大限制時，Redis不會自動清理或刪除任何鍵來釋放記憶體，新的寫入請求將會被拒絕
    ```  
 3. 啟動 Redis: cmd 進入 Redis 目錄，執行 redis-server.exe redis.windows.conf 語句，出現以下內容，則代表啟動成功：
       ![img.png](img.png)

## Redis 快取命名建議（規則回顧）
類型	Key 格式	範例
個別股票	stock:股票代碼:時間	stock:2330:20250627-1215
所有快取查詢	stock:*	自動匹配所有快取
主 key（如 stock:2330、stock:data）可以快速查最新資料。
帶時間的 key 可作為歷史記錄或查詢、追蹤用途。
命名有規則便於查詢：可以使用 Redis 指令如 keys stock:2330:* 拿到所有歷史快取。

## Redis 的使用時機與方式
✅ 功能目的：
1. 查詢特定股票即時價格若 10 分鐘內已有快取資料，則直接從 Redis 回傳，避免頻繁打外部 API（例如 twse.com.tw）
2. 定時推播股價	每日 12:15 自動寫入快取，同時用 Kafka + Telegram 推播
3. 查詢歷史股價	使用時間版本的快取 Key 進行歷史資料查詢或排序，用於生成圖表（保存每日、每次價格）
## 使用方式：
saveToCache(): 把資料存入快取
RedisService.java：包裝所有 Redis 快取邏輯 
set(), get(), delete(), clearAll(), getByCode(), getLatestByCode()
RedisTemplate<String, Object> 被用於與 Redis 互動

# Kafka 應用說明
## ✅ Kafka 的用途
* 用途定位：用作「股票資料訊息的中介通訊平台」。
* 解耦資料流程：Stock 抓資料 → 發送訊息到 Kafka → 消費端處理推播。
* Kafka 的 Producer 會將股票資料推送出去，未來可讓多個 Consumer 同步處理，例如推播、入庫、AI 分析等。
  Kafka 的應用
* 更容易擴展，例如未來要接入 Line Notify、Email 等多通道推播時，只要新增 Consumer 即可
## 使用方式：
KafkaProducerService.java：將股票資料封裝後送出
KafkaConsumerService.java（可選）：接收推送資料並轉給 Telegram
Kafka 設定於 application.properties


# [n8n 教學]在本地建立能串連不同服務的自動化工具

n8n 是款能把你從重複的例行性任務中，拯救出來的自動化工具。

使用者可以透過視覺化的介面，用拖拉節點、設定參數的方式來建立符合自己需求的工作流。

他有雲端與本地（local）的版本，考量到許多工作流程需要放上私鑰（ex: Google API Key、OpenAI API Key），以及雲端版本至少要付費 20 歐元（限制工作流程執行 2500 次）；所以筆者選擇了本地部署的方案，這篇文章會分享詳細的操作步驟。

