# backend/ · FastAPI 同步服务（占位）

**当前状态：未启动。**

按 [ADR-0003](../docs/adr/0003-local-first-with-optional-sync.md)，本应用是 local-first，Room 是权威源。这个目录将来会放一个 FastAPI 服务，给愿意开同步的用户提供：

- 增量同步（按 `item_id + updated_at` 拉 delta）
- 跨设备共享同一份图鉴
- 可选的真实照片云端备份

启动时机：cycle 0003+。在那之前，这里只有这份 README。

参考实现：[github.com/kryiea/drinking](https://github.com/kryiea/drinking) 的 `backend/`（Yinzhi 的 FastAPI 同步层，77% Python）。
