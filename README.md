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
主 key（如 stock:2330）可以快速查最新資料。
帶時間的 key 可作為歷史記錄或查詢、追蹤用途。
命名有規則便於查詢：可以使用 Redis 指令如 keys stock:2330:* 拿到所有歷史快取。

## Redis 的使用時機與方式
✅ 功能目的：
1. 查詢特定股票即時價格若 10 分鐘內已有快取資料，則直接從 Redis 回傳，避免頻繁打外部 API（例如 twse.com.tw）
2. 定時推播股價	每日 12:15 自動寫入快取，同時用 Kafka + Telegram 推播
3. 查詢歷史股價	使用時間版本的快取 Key 進行歷史資料查詢或排序，用於生成圖表（保存每日、每次價格）
## 使用方式：
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

## ▋ STEP 1: 安裝 Docker

前往 Docker 官網: https://www.docker.com/

根據自己的作業系統選擇對應的版本下載。

![img](./img/init_n8n/install_docker.png)

## ▋ STEP 2: 使用 docker-compose.yml 安裝 n8n

你可以直接 git clone 筆者的 GitHub 專案，或者建立一個 `n8n` 的資料夾，新增 `docker-compose.yml` 檔案。

```yml
version: '3.8'

services:
  n8n:
    image: n8nio/n8n
    ports:
      - "5678:5678"
    env_file:
      - .env
    volumes:
      - n8n_data:/home/node/.n8n
    restart: always

  spring-app:
    build: ./spring-app
    ports:
      - "8080:8080"
    restart: always
    depends_on:
      - n8n

volumes:
  n8n_data:

```

貼上內容後，在終端機（Terminal）輸入 `docker compose up -d` 即可啟動

![img](./img/init_n8n/docker_compose_up.png)

## ▋ STEP 3: 建立與啟動
### 編譯 Spring Boot 專案
cd autoDemo
./mvnw clean package

### 回到專案根目錄
cd ..

### 啟動容器
docker-compose up --build

## ▋ STEP 4: n8n 範例流程設定
啟動 n8n，開啟 http://localhost:5678

建立一個新 Workflow，拖拉 Webhook 節點

設定：

HTTP Method: POST

Path: /myworkflow

Authentication: None（或 Basic）

接著加入你想要的自動化流程（例如寄信、呼叫 API）

部署並複製該 Webhook URL，例如 http://localhost:5678/webhook/myworkflow

# 開始做省錢版通知
## Step 1：準備 Telegram Bot
    開 Telegram 搜尋 @BotFather    輸入 /newbot，依指示建立一個 bot
    拿到你的 Bot Token（長得像 123456:ABC-DEF...）
    尋找你想接收通知的 Telegram 聊天 ID：
    打開你的 bot 聊天視窗，先傳一句話，然後用這個網址查看你的 chat ID
👉  https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates
    在回傳的 JSON 裡找 chat.id，記住id

## Step 2：n8n Workflow 範例（抓 PTT 省錢板）
    範例流程：
    Cron：每 10 分鐘執行一次
    HTTP Request：抓 PTT 省錢板的 HTML（https://www.ptt.cc/bbs/Lifeismoney/index.html）
    HTML Extract：抓出每篇文章的標題 + 連結
    IF 判斷：判斷是否是新文章（可比對儲存的上次結果）
    Telegram：推送訊息

### 📦 範例 n8n 節點設定（純文字範本）
    1. Cron（定時觸發）
       Trigger every 10 minutes
        Use built-in Cron node
    2. HTTP Request 項目	設定
        URL	https://www.ptt.cc/bbs/Lifeismoney/index.html
        Method	GET
        Response Format	String
    3. HTML Extract（用 HTML Extract 或 Cheerio 節點）
       CSS selector：div.r-ent a
        這可以抓出所有文章連結的 <a> 元素
    4. Set（把資料組成你要的格式）
       你可以用 Set node 將抓到的文章組成： json 複製 編輯
        {
        "title": "這家超商又有買一送一",
        "link": "https://www.ptt.cc/bbs/Lifeismoney/M.12345678.A.html"
        }
    5. Deduplicate（去重，避免重複通知）
       使用 n8n 的 Data Store（或 Redis）儲存已通知過的文章連結，避免重複推播。
    
    6. Telegram Node
          Bot Token：填剛剛建立的 Bot Token
        Chat ID：剛剛查到的 chat id 
    Message Text：
        text
        複製
        編輯
        📢 PTT 省錢板有新消息！
            📰 {{ $json["title"] }}
            🔗 {{ $json["link"] }}