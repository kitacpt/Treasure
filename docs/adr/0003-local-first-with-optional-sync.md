# ADR-0003 · Local-first，FastAPI 同步可选

- **状态：** Accepted
- **日期：** 2026-05-06

## 背景

Treasure 是私人收藏柜，数据归用户。一个手机的图鉴可能装着发烧友几年的购入记录、序列号、价格、租车路线 —— 不应该是"必须登录账号才能看自己的东西"。

参考：[github.com/kryiea/drinking](https://github.com/kryiea/drinking)（Yinzhi）也是 local-first，搭了 FastAPI 后端做同步与管理面。

## 决定

**Room 是权威源（source of truth）**。所有 UI 都从 Room 读，所有写都先写 Room。

**FastAPI 同步层是可选的**：

- cycle 0001 不接通，只在 `backend/README.md` 占位
- 未来某个 cycle（最早 0003）才搭起来
- 用户关掉同步、删账号 → 本地数据**不变**

## 数据模型（cycle 0001 范围）

两张表：

```sql
items (
  id            TEXT PRIMARY KEY,            -- 形如 "racket-vt-zf2"
  category      TEXT NOT NULL,               -- badminton/photo/cars/tech
  brand         TEXT,
  model         TEXT,
  nickname      TEXT,
  acquired      TEXT,                        -- "YYYY-MM-DD"
  parted        TEXT,
  status        TEXT,                        -- owned/parted/rented
  one_liner     TEXT,
  hero_specs    TEXT,                        -- JSON: [{label,value}, …]
  specs         TEXT,                        -- JSON: {key: value}
  palette       TEXT,                        -- JSON: ["#…","#…","#…","#…"]
  hero_vector   TEXT,                        -- bundled vector id, e.g. "racket-default"
  created_at    INTEGER NOT NULL,            -- epoch ms
  updated_at    INTEGER NOT NULL             -- epoch ms（同步用）
)

history_events (
  id            TEXT PRIMARY KEY,
  item_id       TEXT NOT NULL,
  date          TEXT NOT NULL,               -- "YYYY-MM-DD"
  kind          TEXT NOT NULL,               -- acquired/milestone/maintain/mod/parted
  title         TEXT,
  note          TEXT,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
)
```

种子数据：从 [`../../prototype/project/data.jsx`](../../prototype/project/data.jsx) 移植 6–10 条作为首次启动预置数据，写在 `core/seed/SeedItems.kt`。

## 同步协议（先写下来，cycle 0003 实现）

- 端点：`GET /sync?since=<epoch_ms>` → 返回 `updated_at > since` 的 items + history_events
- 端点：`POST /sync` → 上传本地 dirty 行（按 `updated_at` 比对）
- 冲突：**last-writer-wins，按 `updated_at` 决定**。物品级粒度（不到字段级）。
- 实拍照片：**走单独端点**（`POST /assets`），不在主同步流里。
- 同步失败 → 不抛异常给 UI，记入本地失败队列，下次启动重试。

## 不做的

- ❌ CRDT。粒度是物品级，文档不会高频并发编辑，CRDT 是过度工程。
- ❌ 必须登录。可以匿名设备唯一 id（DataStore 里存）+ 用户加 server token 才同步。
- ❌ E2E 加密（cycle 0003+ 再考虑）。

## 相关

- [`../architecture.md`](../architecture.md)
- 参考：Yinzhi 的 ADR-0003（也是 local-first），文档结构参考它
