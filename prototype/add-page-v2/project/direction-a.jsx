// Direction A — 博物馆图鉴风 v2
// 改造点：
//   1. 首页底部浮动控制岛（图鉴 / 录入 / 设置）
//   2. 详情页：默认仅展示矢量 + 标题 + 关键信息；底部上滑抽屉装"历史/参数/影集"三个 tab
//   3. 矢量图右下角"明信片翻面"按钮 → 展示真实照片网格
//   4. 录入页：对话式 LLM 输入
//   5. 设置页：AI provider 配置 + 默认图风
// 抽屉行为参考网易云：默认半隐藏，往上滑展开到 ~85% 屏高，再下滑收起。

function DirectionA({ tweaks, seed }) {
  // uiTab: 'portal' | 'grid' | 'add' | 'settings'
  const initialTab = seed?.uiTab || 'portal';
  const [uiTab, setUiTab] = React.useState(initialTab);
  const [view, setView] = React.useState(seed?.screen === 'detail' ? { screen: 'detail', id: seed.id } : { screen: 'home' });
  const [activeCat, setActiveCat] = React.useState(seed?.cat || 'badminton');
  const [drawerOpen, setDrawerOpen] = React.useState(seed?.drawer || false);
  const [drawerTab, setDrawerTab] = React.useState(seed?.tab || 'history');
  const [flipped, setFlipped] = React.useState(seed?.flipped || false);

  const palette = tweaks.paletteA;
  const accent = palette[0];
  const bg = tweaks.dark ? '#1a1815' : '#f4f1ea';
  const card = tweaks.dark ? '#27241f' : '#fbf9f4';
  const ink = tweaks.dark ? '#eae5d8' : '#1a1815';
  const sub = tweaks.dark ? 'rgba(234,229,216,0.55)' : 'rgba(26,24,21,0.55)';
  const line = tweaks.dark ? 'rgba(234,229,216,0.12)' : 'rgba(26,24,21,0.1)';
  const serif = tweaks.serif;
  const sans = tweaks.sans;

  const cats = CATEGORIES;
  const items = itemsByCategory(activeCat);

  // ─ Bottom control island (floating, glassmorphic) ─
  const Island = () => (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 18,
      display: 'flex', justifyContent: 'center', pointerEvents: 'none', zIndex: 50,
    }}>
      <div style={{
        pointerEvents: 'auto',
        display: 'flex', gap: 4, padding: 5,
        background: tweaks.dark ? 'rgba(40,36,30,0.78)' : 'rgba(26,24,21,0.85)',
        backdropFilter: 'blur(20px) saturate(1.3)',
        WebkitBackdropFilter: 'blur(20px) saturate(1.3)',
        borderRadius: 999,
        border: `0.5px solid rgba(255,255,255,0.08)`,
        boxShadow: '0 12px 40px rgba(0,0,0,0.18), 0 1px 0 rgba(255,255,255,0.05) inset',
      }}>
        {[
          { id: 'portal', label: '门厅', icon: 'door' },
          { id: 'grid', label: '图鉴', icon: 'book' },
          { id: 'add', label: '录入', icon: 'plus' },
          { id: 'settings', label: '设置', icon: 'gear' },
        ].map((b) => {
          const on = uiTab === b.id;
          return (
            <button key={b.id} onClick={() => { setUiTab(b.id); setView({ screen: 'home' }); }}
              style={{
                background: on ? bg : 'transparent',
                color: on ? ink : '#f4f1ea',
                border: 0, padding: '9px 16px', borderRadius: 999,
                fontFamily: sans, fontSize: 12.5, fontWeight: 500,
                display: 'flex', alignItems: 'center', gap: 7, cursor: 'pointer',
                transition: 'background 180ms ease',
              }}>
              <IslandIcon name={b.icon} size={14}/>
              {b.label}
            </button>
          );
        })}
      </div>
    </div>
  );

  // ─ Home / list ─
  const Home = () => (
    <div style={{ background: bg, minHeight: '100%', fontFamily: sans, color: ink, paddingBottom: 90 }}>
      <div style={{ padding: '20px 22px 8px' }}>
        <div style={{ fontFamily: serif, fontSize: 36, lineHeight: 1.05, fontWeight: 500, letterSpacing: '-0.02em' }}>
          Treasure
        </div>
        <div style={{ fontSize: 11.5, color: sub, marginTop: 6, letterSpacing: '0.06em', textTransform: 'uppercase' }}>
          A catalogue of things owned · {ITEMS.length} items
        </div>
      </div>

      <div style={{ padding: '14px 22px 6px', display: 'flex', gap: 6, overflowX: 'auto' }}>
        {cats.map((c) => {
          const on = c.id === activeCat;
          return (
            <button key={c.id} onClick={() => setActiveCat(c.id)}
              style={{
                flexShrink: 0, padding: '7px 12px', borderRadius: 999,
                border: `0.5px solid ${on ? ink : line}`,
                background: on ? ink : 'transparent',
                color: on ? bg : ink,
                fontSize: 12, fontFamily: sans, fontWeight: 500,
                display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer',
              }}>
              <CatIcon id={c.id} size={13} color={on ? bg : ink}/>
              {c.name}
              <span style={{ opacity: 0.5, fontVariantNumeric: 'tabular-nums', fontSize: 11 }}>{c.count}</span>
            </button>
          );
        })}
      </div>

      <div style={{ padding: '12px 22px 0', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        {items.map((it) => (
          <button key={it.id} onClick={() => setView({ screen: 'detail', id: it.id })}
            style={{ background: 'transparent', border: 0, padding: 0, textAlign: 'left', cursor: 'pointer' }}>
            <div style={{
              aspectRatio: '1 / 1.1', background: card, borderRadius: 2,
              boxShadow: tweaks.dark ? 'none' : '0 0.5px 0 rgba(26,24,21,0.06), 0 1px 3px rgba(26,24,21,0.04)',
              border: `0.5px solid ${line}`,
              padding: 14, display: 'flex', alignItems: 'center', justifyContent: 'center',
              position: 'relative', overflow: 'hidden',
            }}>
              <div style={{ width: '92%', height: '82%' }}>
                <ItemVector item={it} />
              </div>
              <div style={{
                position: 'absolute', top: 8, left: 8,
                fontFamily: 'ui-monospace, monospace', fontSize: 9,
                color: sub, letterSpacing: '0.05em',
              }}>№ {String(items.indexOf(it) + 1).padStart(3, '0')}</div>
              {(it.status === 'parted' || it.status === 'rented') && (
                <div style={{
                  position: 'absolute', top: 8, right: 8, padding: '2px 6px',
                  fontSize: 9, color: sub, border: `0.5px solid ${line}`, borderRadius: 1,
                  letterSpacing: '0.06em', textTransform: 'uppercase',
                }}>{it.status === 'parted' ? 'parted' : 'rental'}</div>
              )}
            </div>
            <div style={{ paddingTop: 10 }}>
              <div style={{ fontSize: 10, color: sub, letterSpacing: '0.08em', textTransform: 'uppercase' }}>{it.brand}</div>
              <div style={{ fontFamily: serif, fontSize: 16, lineHeight: 1.2, marginTop: 2, fontWeight: 500 }}>
                {it.nickname}
              </div>
              <div style={{ fontSize: 11, color: sub, marginTop: 3 }}>{it.one_liner}</div>
            </div>
          </button>
        ))}
      </div>
    </div>
  );

  // ─ Detail ─ 仅展示页 + 抽屉
  const Detail = () => {
    const it = itemById(view.id);
    if (!it) return null;
    return (
      <DetailScreen
        it={it}
        bg={bg} card={card} ink={ink} sub={sub} line={line}
        serif={serif} sans={sans} accent={accent} tweaks={tweaks}
        flipped={flipped} setFlipped={setFlipped}
        drawerOpen={drawerOpen} setDrawerOpen={setDrawerOpen}
        drawerTab={drawerTab} setDrawerTab={setDrawerTab}
        onBack={() => { setDrawerOpen(false); setFlipped(false); setView({ screen: 'home' }); }}
      />
    );
  };

  // ─ Add (LLM-conversational input) ─
  const Add = () => (
    <AddScreen bg={bg} card={card} ink={ink} sub={sub} line={line} serif={serif} sans={sans} accent={accent} seed={seed}/>
  );

  // ─ Settings ─
  const Settings = () => (
    <SettingsScreen bg={bg} card={card} ink={ink} sub={sub} line={line} serif={serif} sans={sans} accent={accent}/>
  );

  // route
  let body;
  if (view.screen === 'detail') body = <Detail/>;
  else if (uiTab === 'add') body = <Add/>;
  else if (uiTab === 'settings') body = <Settings/>;
  else if (uiTab === 'grid') body = <Home/>;
  else body = <Portal
    bg={bg} card={card} ink={ink} sub={sub} line={line} accent={accent}
    serif={serif} sans={sans} tweaks={tweaks}
    onEnterCategory={(cid) => { setActiveCat(cid); setUiTab('grid'); }}
    onOpenItem={(id) => setView({ screen: 'detail', id })}
  />;

  // hide island on detail (detail has its own back chrome and drawer)
  const showIsland = view.screen !== 'detail';

  return (
    <div style={{ height: '100%', position: 'relative' }}>
      {body}
      {showIsland && <Island/>}
    </div>
  );
}

// ── Detail screen as standalone so we can manage drawer state cleanly ──
function DetailScreen({ it, bg, card, ink, sub, line, serif, sans, accent, tweaks, flipped, setFlipped, drawerOpen, setDrawerOpen, drawerTab, setDrawerTab, onBack }) {
  // Real photos — placeholder set per item; rendered as warm-tone rectangles
  // for now (user adds real ones later).
  const photos = mockPhotosFor(it);

  return (
    <div style={{ background: bg, height: '100%', position: 'relative', overflow: 'hidden', fontFamily: sans, color: ink }}>
      {/* nav */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 18px 8px' }}>
        <button onClick={onBack}
          style={{ background: 'transparent', border: 0, color: ink, fontSize: 13, padding: '6px 8px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}>
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"><path d="M8 3l-4 4 4 4"/></svg>
          <span style={{ fontFamily: sans }}>{CATEGORIES.find((c) => c.id === it.category).name}</span>
        </button>
        <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, color: sub, letterSpacing: '0.05em' }}>
          № {String(itemsByCategory(it.category).indexOf(it) + 1).padStart(3, '0')} / {itemsByCategory(it.category).length.toString().padStart(3, '0')}
        </div>
      </div>

      {/* hero card — flippable (vector ↔ photos) */}
      <div style={{ padding: '12px 22px 16px', perspective: 1200 }}>
        <div style={{
          position: 'relative', aspectRatio: '1 / 1.05', borderRadius: 2,
          transformStyle: 'preserve-3d',
          transition: 'transform 600ms cubic-bezier(.4,.0,.2,1)',
          transform: flipped ? 'rotateY(180deg)' : 'rotateY(0deg)',
        }}>
          {/* front — vector */}
          <div style={{
            position: 'absolute', inset: 0, backfaceVisibility: 'hidden',
            background: card, border: `0.5px solid ${line}`,
            padding: 24, display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <div style={{ width: '78%', height: '78%' }}>
              <ItemVector item={it}/>
            </div>
            {/* corner palette */}
            <div style={{ position: 'absolute', bottom: 14, left: 14, display: 'flex', gap: 0 }}>
              {it.palette.map((c) => (
                <div key={c} style={{ width: 10, height: 10, background: c, border: `0.5px solid rgba(0,0,0,0.1)` }}/>
              ))}
            </div>
            {/* flip handle — postcard corner */}
            <button onClick={() => setFlipped(true)}
              title="翻面看实拍"
              style={{
                position: 'absolute', bottom: 12, right: 12, padding: '5px 9px',
                background: 'transparent', color: sub,
                border: `0.5px solid ${line}`, borderRadius: 1, cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: 6,
                fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: '0.08em',
              }}>
              <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke="currentColor" strokeWidth="1"><rect x="1" y="2" width="8" height="6"/><path d="M3 4h4M3 6h2"/></svg>
              {photos.length} PHOTOS
            </button>
          </div>

          {/* back — real photos (mock placeholders) */}
          <div style={{
            position: 'absolute', inset: 0, backfaceVisibility: 'hidden',
            transform: 'rotateY(180deg)',
            background: card, border: `0.5px solid ${line}`,
            padding: 14, display: 'flex', flexDirection: 'column',
          }}>
            <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, color: sub, letterSpacing: '0.08em', marginBottom: 8 }}>
              REAL PHOTOS · {photos.length}
            </div>
            <div style={{ flex: 1, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6 }}>
              {photos.map((p, i) => (
                <div key={i} style={{
                  background: p.bg, position: 'relative', overflow: 'hidden',
                  border: `0.5px solid ${line}`,
                }}>
                  {/* subtle striped placeholder */}
                  <div style={{ position: 'absolute', inset: 0, background: `repeating-linear-gradient(${p.angle}deg, ${p.shade1} 0 6px, ${p.shade2} 6px 12px)`, opacity: 0.5 }}/>
                  <div style={{ position: 'absolute', bottom: 5, left: 6, fontFamily: 'ui-monospace, monospace', fontSize: 8, color: 'rgba(255,255,255,0.85)', letterSpacing: '0.04em', textShadow: '0 1px 2px rgba(0,0,0,0.4)' }}>{p.caption}</div>
                </div>
              ))}
              <button style={{
                background: 'transparent', border: `0.5px dashed ${line}`,
                color: sub, fontSize: 10, fontFamily: sans,
                display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 4,
                cursor: 'pointer',
              }}>
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1"><path d="M7 3v8M3 7h8"/></svg>
                添加照片
              </button>
            </div>
            <button onClick={() => setFlipped(false)}
              style={{
                marginTop: 8, padding: '5px 9px', alignSelf: 'flex-end',
                background: 'transparent', color: sub,
                border: `0.5px solid ${line}`, borderRadius: 1, cursor: 'pointer',
                fontFamily: 'ui-monospace, monospace', fontSize: 9, letterSpacing: '0.08em',
              }}>← BACK TO COVER</button>
          </div>
        </div>

        {/* title block */}
        <div style={{ marginTop: 22 }}>
          <div style={{ fontSize: 10.5, color: sub, letterSpacing: '0.12em', textTransform: 'uppercase' }}>
            {it.brand} · {fmtYear(it.acquired)}
          </div>
          <div style={{ fontFamily: serif, fontSize: 30, lineHeight: 1.1, fontWeight: 500, marginTop: 6, letterSpacing: '-0.01em' }}>
            {it.nickname}
          </div>
          <div style={{ fontSize: 13, color: sub, marginTop: 6, fontStyle: 'italic', fontFamily: serif }}>
            {it.model}
          </div>

          <div style={{
            display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px 18px',
            marginTop: 22, padding: '18px 0', borderTop: `0.5px solid ${line}`, borderBottom: `0.5px solid ${line}`,
          }}>
            {it.hero_specs.slice(0, 4).map((s) => (
              <div key={s.label}>
                <div style={{ fontSize: 9.5, color: sub, letterSpacing: '0.08em', textTransform: 'uppercase' }}>{s.label}</div>
                <div style={{ fontFamily: serif, fontSize: 14, marginTop: 3, fontWeight: 500 }}>{s.value}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* drawer pull tab — sits at bottom, click/drag to expand */}
      <Drawer
        open={drawerOpen} setOpen={setDrawerOpen}
        tab={drawerTab} setTab={setDrawerTab}
        bg={bg} card={card} ink={ink} sub={sub} line={line}
        serif={serif} sans={sans} accent={accent}
        it={it}
      />
    </div>
  );
}

// ── Drawer (网易云风格) ──
function Drawer({ open, setOpen, tab, setTab, bg, card, ink, sub, line, serif, sans, accent, it }) {
  return (
    <>
      {/* dim backdrop when open */}
      {open && (
        <div onClick={() => setOpen(false)}
          style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.18)', zIndex: 4 }}/>
      )}

      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        height: open ? '78%' : 64,
        background: bg,
        borderTop: `0.5px solid ${line}`,
        borderRadius: '14px 14px 0 0',
        boxShadow: open ? '0 -20px 60px rgba(0,0,0,0.18)' : '0 -8px 22px rgba(0,0,0,0.06)',
        transition: 'height 320ms cubic-bezier(.4,.0,.2,1)',
        zIndex: 5,
        overflow: 'hidden',
      }}>
        {/* handle */}
        <button onClick={() => setOpen(!open)}
          style={{
            width: '100%', height: 64, background: 'transparent', border: 0, cursor: 'pointer',
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 6,
            fontFamily: sans, color: sub, fontSize: 11, letterSpacing: '0.1em', textTransform: 'uppercase',
          }}>
          <div style={{ width: 36, height: 3, borderRadius: 2, background: line }}/>
          {!open && <span>上滑 · 历史 · 参数 · 影集</span>}
        </button>

        {/* tabs (when open) */}
        {open && (
          <div style={{ padding: '0 22px 4px', display: 'flex', gap: 22, borderBottom: `0.5px solid ${line}` }}>
            {[
              { id: 'history', label: '历史', sub: it.history.length },
              { id: 'specs', label: '参数', sub: Object.keys(it.specs).length },
              { id: 'album', label: '影集', sub: 6 },
            ].map((t) => {
              const on = tab === t.id;
              return (
                <button key={t.id} onClick={() => setTab(t.id)}
                  style={{
                    background: 'transparent', border: 0,
                    borderBottom: on ? `1.5px solid ${ink}` : '1.5px solid transparent',
                    padding: '12px 0',
                    color: on ? ink : sub, cursor: 'pointer',
                    fontFamily: serif, fontSize: 16, fontWeight: 500,
                    display: 'flex', alignItems: 'baseline', gap: 6,
                  }}>
                  {t.label}
                  <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, opacity: 0.6 }}>{t.sub}</span>
                </button>
              );
            })}
          </div>
        )}

        {/* drawer content */}
        {open && (
          <div style={{ height: 'calc(100% - 64px - 38px)', overflowY: 'auto' }}>
            {tab === 'history' && <HistoryPane it={it} sub={sub} ink={ink} line={line} card={card} accent={accent} serif={serif} sans={sans}/>}
            {tab === 'specs' && <SpecsPane it={it} sub={sub} ink={ink} line={line} serif={serif} sans={sans}/>}
            {tab === 'album' && <AlbumPane it={it} sub={sub} ink={ink} line={line} card={card} sans={sans}/>}
          </div>
        )}
      </div>
    </>
  );
}

function HistoryPane({ it, sub, ink, line, card, accent, serif, sans }) {
  return (
    <div style={{ padding: '20px 22px 100px' }}>
      <div style={{ position: 'relative' }}>
        <div style={{ position: 'absolute', left: 7, top: 8, bottom: 8, width: 0.5, background: line }}/>
        {it.history.slice().reverse().map((h, i) => (
          <div key={i} style={{ display: 'flex', gap: 14, marginBottom: 22, position: 'relative' }}>
            <div style={{
              width: 14, height: 14, borderRadius: 7, marginTop: 4, flexShrink: 0,
              background: h.kind === 'milestone' ? accent : card,
              border: `1px solid ${h.kind === 'milestone' ? accent : ink}`,
              zIndex: 1,
            }}/>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 10, color: sub, fontFamily: 'ui-monospace, monospace', letterSpacing: '0.05em' }}>
                {fmtDate(h.date)}
              </div>
              <div style={{ fontFamily: serif, fontSize: 16, fontWeight: 500, marginTop: 2, lineHeight: 1.25, color: ink }}>
                {h.title}
              </div>
              <div style={{ fontSize: 12.5, color: sub, marginTop: 4, lineHeight: 1.5 }}>
                {h.note}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function SpecsPane({ it, sub, ink, line, serif, sans }) {
  return (
    <div style={{ padding: '20px 22px 100px' }}>
      {Object.entries(it.specs).map(([k, v], i) => (
        <div key={k} style={{
          display: 'flex', justifyContent: 'space-between', gap: 16,
          padding: '12px 0',
          borderTop: i === 0 ? `0.5px solid ${line}` : 0,
          borderBottom: `0.5px solid ${line}`, alignItems: 'baseline',
        }}>
          <div style={{ fontSize: 12, color: sub, flexShrink: 0 }}>{k}</div>
          <div style={{ fontSize: 13, fontFamily: serif, color: ink, textAlign: 'right' }}>{v}</div>
        </div>
      ))}
    </div>
  );
}

function AlbumPane({ it, sub, ink, line, card, sans }) {
  const photos = mockPhotosFor(it);
  return (
    <div style={{ padding: '20px 22px 100px' }}>
      <div style={{ fontSize: 11, color: sub, fontFamily: sans, marginBottom: 12 }}>
        实拍照片 · 自动按时间排序
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
        {photos.map((p, i) => (
          <div key={i} style={{
            aspectRatio: '1 / 1.1', position: 'relative', overflow: 'hidden',
            border: `0.5px solid ${line}`, background: p.bg,
          }}>
            <div style={{ position: 'absolute', inset: 0, background: `repeating-linear-gradient(${p.angle}deg, ${p.shade1} 0 7px, ${p.shade2} 7px 14px)`, opacity: 0.55 }}/>
            <div style={{ position: 'absolute', bottom: 6, left: 7, right: 7, fontFamily: 'ui-monospace, monospace', fontSize: 9, color: 'rgba(255,255,255,0.92)', letterSpacing: '0.04em', textShadow: '0 1px 2px rgba(0,0,0,0.5)' }}>
              {p.caption}
            </div>
          </div>
        ))}
        <button style={{
          aspectRatio: '1 / 1.1', background: 'transparent',
          border: `0.5px dashed ${line}`, color: sub, fontSize: 11,
          fontFamily: sans, display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center', gap: 6, cursor: 'pointer',
        }}>
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1"><path d="M8 3v10M3 8h10"/></svg>
          添加照片
        </button>
      </div>
    </div>
  );
}

// ── Add screen — 多模式录入 ──
//   chooser: 入口选择（AI / 手动）
//   chat:    与助手对话（含语音）
//   voice:   语音录入进行中（蒙层）
//   preview: AI 生成草稿，用户预览/编辑/确认
//   manual:  手动表单录入
function AddScreen({ bg, card, ink, sub, line, serif, sans, accent, seed }) {
  // 默认进入 AI 对话页；手动录入从对话页右上角入口进入
  const [mode, setMode] = React.useState(seed?.addMode || 'chat');
  const [voiceOn, setVoiceOn] = React.useState(seed?.voiceOn || false);
  const [historyOpen, setHistoryOpen] = React.useState(seed?.historyOpen || false);

  const Header = ({ title, sub: subtitle, onBack }) => (
    <div style={{ padding: '18px 22px 14px', borderBottom: `0.5px solid ${line}`, display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12 }}>
      <div>
        <div style={{ fontFamily: serif, fontSize: 26, lineHeight: 1.05, fontWeight: 500, letterSpacing: '-0.01em' }}>
          {title}
        </div>
        {subtitle && <div style={{ fontSize: 11, color: sub, marginTop: 6, letterSpacing: '0.08em', textTransform: 'uppercase' }}>{subtitle}</div>}
      </div>
      {onBack && (
        <button onClick={onBack}
          style={{ background: 'transparent', border: 0, color: sub, fontSize: 11, fontFamily: sans, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4, padding: '6px 0' }}>
          <svg width="11" height="11" viewBox="0 0 11 11" fill="none" stroke="currentColor" strokeWidth="1.2"><path d="M7 2L3 5.5L7 9"/></svg>
          换一种
        </button>
      )}
    </div>
  );

  if (mode === 'chat') return (
    <AddChat bg={bg} card={card} ink={ink} sub={sub} line={line} serif={serif} sans={sans} accent={accent}
      voiceOn={voiceOn} setVoiceOn={setVoiceOn}
      historyOpen={historyOpen} setHistoryOpen={setHistoryOpen}
      onSeePreview={() => setMode('preview')}
      onManual={() => setMode('manual')}/>
  );
  if (mode === 'preview') return (
    <AddPreview bg={bg} card={card} ink={ink} sub={sub} line={line} serif={serif} sans={sans} accent={accent}
      onBack={() => setMode('chat')} Header={Header}/>
  );
  if (mode === 'manual') return (
    <AddManual bg={bg} card={card} ink={ink} sub={sub} line={line} serif={serif} sans={sans} accent={accent}
      onBack={() => setMode('chat')} Header={Header}/>
  );
  return null;
}

// ── Chooser landing ──
function AddChooser({ bg, card, ink, sub, line, serif, sans, accent, onPick, Header }) {
  return (
    <div style={{ background: bg, minHeight: '100%', fontFamily: sans, color: ink, paddingBottom: 110 }}>
      <Header title="录入新物件" sub="选一种方式 · CHOOSE A METHOD"/>
      <div style={{ padding: '24px 18px 0', display: 'flex', flexDirection: 'column', gap: 14 }}>
        {/* AI option (推荐) */}
        <button onClick={() => onPick('chat')}
          style={{
            background: card, border: `0.5px solid ${line}`, borderRadius: 2,
            padding: '20px 18px', textAlign: 'left', cursor: 'pointer', color: ink,
            display: 'flex', alignItems: 'flex-start', gap: 14, position: 'relative',
          }}>
          <div style={{ position: 'absolute', top: 10, right: 12, fontFamily: 'ui-monospace, monospace', fontSize: 9, color: sub, letterSpacing: '0.1em' }}>I · RECOMMENDED</div>
          <div style={{
            width: 52, height: 52, flexShrink: 0, border: `0.5px solid ${ink}`, borderRadius: 26,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <svg width="22" height="22" viewBox="0 0 22 22" fill="none" stroke={ink} strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M4 7l3-3 3 3M11 4v9M18 15l-3 3-3-3M15 18V9"/>
            </svg>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: serif, fontSize: 20, fontWeight: 500, lineHeight: 1.15, marginTop: 6 }}>与助手对话</div>
            <div style={{ fontSize: 12, color: sub, marginTop: 6, lineHeight: 1.5 }}>
              拍一张照片，或说一句话。我会问几个简单的问题，
              然后替你写好型号、参数、价格——你再过目确认。
            </div>
            <div style={{ display: 'flex', gap: 6, marginTop: 12, flexWrap: 'wrap' }}>
              {['📷 拍照', '🎙 语音', '⌨ 文字'].map((t) => (
                <span key={t} style={{
                  fontSize: 10, padding: '4px 9px', border: `0.5px solid ${line}`,
                  borderRadius: 999, color: sub, fontFamily: sans, letterSpacing: '0.04em',
                }}>{t}</span>
              ))}
            </div>
          </div>
        </button>

        {/* Manual option */}
        <button onClick={() => onPick('manual')}
          style={{
            background: 'transparent', border: `0.5px solid ${line}`, borderRadius: 2,
            padding: '20px 18px', textAlign: 'left', cursor: 'pointer', color: ink,
            display: 'flex', alignItems: 'flex-start', gap: 14, position: 'relative',
          }}>
          <div style={{ position: 'absolute', top: 10, right: 12, fontFamily: 'ui-monospace, monospace', fontSize: 9, color: sub, letterSpacing: '0.1em' }}>II</div>
          <div style={{
            width: 52, height: 52, flexShrink: 0, border: `0.5px solid ${sub}`, borderRadius: 2,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <svg width="22" height="22" viewBox="0 0 22 22" fill="none" stroke={sub} strokeWidth="1.1" strokeLinecap="round" strokeLinejoin="round">
              <path d="M3 3h13M3 8h16M3 13h11M3 18h14"/>
            </svg>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: serif, fontSize: 20, fontWeight: 500, lineHeight: 1.15, marginTop: 6 }}>手动录入</div>
            <div style={{ fontSize: 12, color: sub, marginTop: 6, lineHeight: 1.5 }}>
              自己一项一项填。适合细节很多、或者想完全掌控的物件——
              比如老物件、传家信物、定制装备。
            </div>
          </div>
        </button>
      </div>

      {/* tiny ornament */}
      <div style={{ padding: '34px 24px 0', textAlign: 'center', color: sub, fontSize: 10, letterSpacing: '0.3em' }}>
        ✦ &nbsp; OR PICK ANY METHOD ABOVE &nbsp; ✦
      </div>
    </div>
  );
}

// ── Chat with voice ──
function AddChat({ bg, card, ink, sub, line, serif, sans, accent, voiceOn, setVoiceOn, historyOpen, setHistoryOpen, onSeePreview, onManual }) {
  const conversations = [
    { id: 'c1', title: 'Fujifilm X-T5', date: '今天 14:32', current: true },
    { id: 'c2', title: 'Wilson Pro Staff 97', date: '昨天' },
    { id: 'c3', title: '他山杒0-1', date: '5月2日' },
    { id: 'c4', title: 'AirPods Pro 2', date: '4月28日' },
  ];
  const messages = [
    { role: 'assistant', text: '你好。把新东西的照片发给我，或者直接说说它是什么。' },
    { role: 'user', kind: 'photo', caption: '一张相机的照片' },
    { role: 'assistant', text: '看起来是 Fujifilm X-T5，黑色机身。是吗？' },
    { role: 'user', text: '对' },
    { role: 'assistant', text: '什么时候买的，多少钱？' },
    { role: 'user', kind: 'voice', text: '2023 年情人节，一万二千五', dur: '0:04' },
    { role: 'assistant', text: '好。我已经替你写好了一份草稿——要不要先看看？' },
    { role: 'preview-cta' },
  ];
  return (
    <div style={{ background: bg, minHeight: '100%', fontFamily: sans, color: ink, paddingBottom: 170, position: 'relative' }}>
      {/* header with title + new-chat + history */}
      <div style={{ padding: '16px 18px 12px', borderBottom: `0.5px solid ${line}`, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
          <div style={{ fontFamily: serif, fontSize: 22, lineHeight: 1.05, fontWeight: 500, letterSpacing: '-0.01em' }}>录入</div>
          <button onClick={() => setHistoryOpen(!historyOpen)}
            style={{ background: 'transparent', border: 0, color: sub, cursor: 'pointer', padding: '4px 6px', display: 'flex', alignItems: 'center', gap: 5, fontSize: 11, fontFamily: sans, fontStyle: 'italic' }}>
            <span style={{ fontFamily: serif }}>Fujifilm X-T5</span>
            <svg width="9" height="9" viewBox="0 0 9 9" fill="none" stroke="currentColor" strokeWidth="1.1"><path d="M2 3.5l2.5 2.5L7 3.5"/></svg>
          </button>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
          <button title="历史对话" onClick={() => setHistoryOpen(!historyOpen)}
            style={{ width: 30, height: 30, background: 'transparent', border: `0.5px solid ${line}`, color: ink, borderRadius: 999, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="13" height="13" viewBox="0 0 13 13" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"><circle cx="6.5" cy="6.5" r="4.5"/><path d="M6.5 4v2.7l1.6 1"/></svg>
          </button>
          <button title="新对话"
            style={{ width: 30, height: 30, background: 'transparent', border: `0.5px solid ${line}`, color: ink, borderRadius: 999, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"><path d="M6 2v8M2 6h8"/></svg>
          </button>
          <button title="手动录入" onClick={onManual}
            style={{ height: 30, padding: '0 10px', background: 'transparent', border: `0.5px solid ${line}`, color: ink, borderRadius: 999, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5, fontSize: 11, fontFamily: sans }}>
            <svg width="11" height="11" viewBox="0 0 11 11" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"><path d="M2 2.5h7M2 5.5h8M2 8.5h6"/></svg>
            手动
          </button>
        </div>
      </div>

      {/* history drawer */}
      {historyOpen && (
        <>
          <div onClick={() => setHistoryOpen(false)} style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.18)', zIndex: 30 }}/>
          <div style={{ position: 'absolute', top: 60, right: 14, width: 240, background: bg, border: `0.5px solid ${line}`, boxShadow: '0 10px 30px rgba(0,0,0,0.18)', zIndex: 31, borderRadius: 2 }}>
            <div style={{ padding: '10px 14px', borderBottom: `0.5px solid ${line}`, fontSize: 9.5, color: sub, letterSpacing: '0.18em', textTransform: 'uppercase' }}>历史对话</div>
            {conversations.map((c) => (
              <button key={c.id} onClick={() => setHistoryOpen(false)}
                style={{ width: '100%', textAlign: 'left', padding: '10px 14px', background: c.current ? card : 'transparent', border: 0, borderBottom: `0.5px solid ${line}`, cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: 2, color: ink }}>
                <span style={{ fontFamily: serif, fontSize: 13, fontWeight: 500 }}>{c.title}</span>
                <span style={{ fontSize: 10, color: sub, fontFamily: 'ui-monospace, monospace', letterSpacing: '0.05em' }}>{c.date}{c.current ? ' · 当前' : ''}</span>
              </button>
            ))}
            <button style={{ width: '100%', textAlign: 'left', padding: '10px 14px', background: 'transparent', border: 0, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, color: ink, fontSize: 12, fontFamily: sans }}>
              <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke="currentColor" strokeWidth="1.2"><path d="M5 2v6M2 5h6"/></svg>
              新对话
            </button>
          </div>
        </>
      )}

      <div style={{ padding: '18px 22px 0', display: 'flex', flexDirection: 'column', gap: 12 }}>
        {messages.map((m, i) => {
          if (m.role === 'preview-cta') {
            return (
              <button key={i} onClick={onSeePreview}
                style={{
                  background: card, border: `0.5px solid ${line}`, borderRadius: 2,
                  padding: '14px 16px', cursor: 'pointer', color: ink,
                  display: 'flex', alignItems: 'center', gap: 14, marginTop: 4,
                  position: 'relative', textAlign: 'left',
                }}>
                <div style={{ width: 48, height: 48, background: bg, border: `0.5px solid ${line}`, padding: 5, flexShrink: 0 }}>
                  <ItemVector item={itemById('cam-fuji-xt5')}/>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontFamily: 'ui-monospace, monospace', fontSize: 9, color: sub, letterSpacing: '0.18em' }}>DRAFT • 8 FIELDS</div>
                  <div style={{ fontFamily: serif, fontSize: 16, fontWeight: 500, marginTop: 2, lineHeight: 1.15 }}>草稿已就绪</div>
                  <div style={{ fontSize: 11, color: sub, marginTop: 2, fontStyle: 'italic', fontFamily: serif }}>轻点过目，确认后收入图鉴</div>
                </div>
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke={ink} strokeWidth="1.2"><path d="M4 2l4 4-4 4"/></svg>
              </button>
            );
          }
          const isUser = m.role === 'user';
          return (
            <div key={i} style={{ display: 'flex', justifyContent: isUser ? 'flex-end' : 'flex-start' }}>
              {m.kind === 'photo' ? (
                <div style={{ width: 120, height: 120, background: '#3a3530', borderRadius: 6, position: 'relative', overflow: 'hidden', border: `0.5px solid ${line}` }}>
                  <div style={{ position: 'absolute', inset: 0, background: 'repeating-linear-gradient(35deg, #3a3530 0 7px, #2a2520 7px 14px)' }}/>
                  <div style={{ position: 'absolute', bottom: 6, left: 7, fontFamily: 'ui-monospace, monospace', fontSize: 8, color: '#fff', opacity: 0.7 }}>{m.caption}</div>
                </div>
              ) : m.kind === 'voice' ? (
                <div style={{
                  maxWidth: '78%', padding: '10px 14px', background: ink, color: bg,
                  borderRadius: '14px 14px 4px 14px', fontSize: 13, lineHeight: 1.4,
                  display: 'flex', flexDirection: 'column', gap: 6,
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <svg width="11" height="11" viewBox="0 0 11 11" fill="currentColor"><path d="M2 5l1 0 0 1 1 0 0 1 1 0 0-3 1 0 0 5 1 0 0-7 1 0 0 5 1 0 0-3 1 0 0 1z"/></svg>
                    <div style={{ display: 'flex', gap: 2, alignItems: 'flex-end', height: 14 }}>
                      {[6, 9, 12, 7, 11, 5, 10, 13, 8, 6, 11, 9].map((h, k) => (
                        <div key={k} style={{ width: 1.5, height: h, background: bg, opacity: 0.7 }}/>
                      ))}
                    </div>
                    <span style={{ fontFamily: 'ui-monospace, monospace', fontSize: 10, opacity: 0.7 }}>{m.dur}</span>
                  </div>
                  <div style={{ fontStyle: 'italic', opacity: 0.85 }}>"{m.text}"</div>
                </div>
              ) : (
                <div style={{
                  maxWidth: '78%', padding: '10px 14px',
                  background: isUser ? ink : card, color: isUser ? bg : ink,
                  borderRadius: isUser ? '14px 14px 4px 14px' : '14px 14px 14px 4px',
                  fontSize: 13.5, lineHeight: 1.5,
                  border: isUser ? 'none' : `0.5px solid ${line}`,
                  fontFamily: m.role === 'assistant' ? serif : sans,
                  fontStyle: m.role === 'assistant' ? 'italic' : 'normal',
                }}>
                  {m.text}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* composer (with voice) — sits above the floating island */}
      <div style={{
        position: 'absolute', left: 14, right: 14, bottom: 88,
        background: card, border: `0.5px solid ${line}`, borderRadius: 22,
        padding: '7px 7px 7px 14px', display: 'flex', alignItems: 'center', gap: 8,
        boxShadow: '0 6px 20px rgba(0,0,0,0.06)',
      }}>
        <button style={{ background: 'transparent', border: 0, color: sub, padding: 6, cursor: 'pointer' }} title="拍照">
          <svg width="17" height="17" viewBox="0 0 17 17" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"><rect x="2.5" y="4" width="12" height="9.5" rx="1.5"/><circle cx="8.5" cy="8.5" r="2.4"/><path d="M5.5 4l1-1.4h4l1 1.4"/></svg>
        </button>
        <div style={{ flex: 1, fontSize: 13, color: sub }}>说说这件东西…</div>
        <button onClick={() => setVoiceOn(true)}
          title="按住说话"
          style={{
            width: 32, height: 32, borderRadius: 999, background: voiceOn ? accent : 'transparent',
            border: `0.5px solid ${voiceOn ? accent : ink}`, color: voiceOn ? bg : ink,
            cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
            transition: 'all 160ms',
          }}>
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round">
            <rect x="5" y="2" width="4" height="7" rx="2"/><path d="M3 7a4 4 0 008 0M7 11v2"/>
          </svg>
        </button>
        <button style={{
          width: 32, height: 32, borderRadius: 999, background: ink, color: bg,
          border: 0, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
        }} title="发送">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><path d="M7 11V3M3.5 6.5L7 3l3.5 3.5"/></svg>
        </button>
      </div>

      {/* voice overlay */}
      {voiceOn && (
        <div onClick={() => setVoiceOn(false)}
          style={{
            position: 'absolute', inset: 0, background: 'rgba(26,24,21,0.55)',
            backdropFilter: 'blur(8px)', zIndex: 60,
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end',
            paddingBottom: 130, cursor: 'pointer',
          }}>
          <div style={{ display: 'flex', gap: 4, alignItems: 'flex-end', height: 60 }}>
            {[18, 32, 46, 28, 54, 40, 22, 50, 36, 26, 42, 30, 48, 34, 20].map((h, k) => (
              <div key={k} style={{
                width: 4, height: h + Math.sin(Date.now() / 200 + k) * 6,
                background: '#f4f1ea', borderRadius: 2, opacity: 0.85,
              }}/>
            ))}
          </div>
          <div style={{ marginTop: 22, fontFamily: serif, fontStyle: 'italic', color: '#f4f1ea', fontSize: 18, textAlign: 'center', padding: '0 32px' }}>
            "二零二三年情人节，一万二千五…"
          </div>
          <div style={{ marginTop: 14, fontSize: 11, color: 'rgba(244,241,234,0.6)', letterSpacing: '0.18em', textTransform: 'uppercase' }}>
            松开发送 · TAP TO STOP
          </div>
        </div>
      )}
    </div>
  );
}

// ── Preview & confirm ──
function AddPreview({ bg, card, ink, sub, line, serif, sans, accent, onBack, Header }) {
  // Mock fields the AI inferred. In production these come from the assistant's
  // tool call. Each row has a confidence indicator: high / med / low.
  const initial = [
    { k: '品类', v: '摄影 · 相机', c: 'high' },
    { k: '品牌', v: 'Fujifilm', c: 'high' },
    { k: '型号', v: 'X-T5', c: 'high' },
    { k: '昵称', v: '银盐之眼', c: 'low', placeholder: '（可选）' },
    { k: '颜色', v: '黑色 Black', c: 'high' },
    { k: '入手日期', v: '2023-02-14', c: 'med' },
    { k: '入手价格', v: '¥12,500', c: 'med' },
    { k: '入手渠道', v: '京东自营', c: 'low' },
    { k: '一句话', v: 'APS-C · 4020 万像素 · 经典对焦旋钮', c: 'med' },
  ];
  const [fields, setFields] = React.useState(initial);
  const [editing, setEditing] = React.useState(null);

  const ConfDot = ({ c }) => {
    const map = { high: ink, med: accent, low: sub };
    return <span title={c} style={{
      width: 5, height: 5, borderRadius: 999,
      background: map[c], display: 'inline-block', marginRight: 8, opacity: c === 'low' ? 0.45 : 1,
    }}/>;
  };

  return (
    <div style={{ background: bg, minHeight: '100%', fontFamily: sans, color: ink, paddingBottom: 130 }}>
      <Header title="草稿预览" sub="REVIEW · EDIT · CONFIRM" onBack={onBack}/>

      {/* hero card */}
      <div style={{ padding: '20px 22px 0' }}>
        <div style={{
          background: card, border: `0.5px solid ${line}`, borderRadius: 2,
          padding: 18, display: 'flex', gap: 16, alignItems: 'center', position: 'relative',
        }}>
          <div style={{ position: 'absolute', top: 8, left: 10, fontFamily: 'ui-monospace, monospace', fontSize: 9, color: sub, letterSpacing: '0.1em' }}>DRAFT №024</div>
          <div style={{ position: 'absolute', top: 8, right: 10, fontFamily: 'ui-monospace, monospace', fontSize: 9, color: sub, letterSpacing: '0.1em' }}>UNCONFIRMED</div>
          <div style={{ width: 92, height: 92, background: bg, border: `0.5px solid ${line}`, padding: 8, flexShrink: 0, marginTop: 12 }}>
            <ItemVector item={itemById('cam-fuji-xt5')}/>
          </div>
          <div style={{ flex: 1, minWidth: 0, marginTop: 12 }}>
            <div style={{ fontSize: 10, color: sub, letterSpacing: '0.1em', textTransform: 'uppercase' }}>FUJIFILM · 2023</div>
            <div style={{ fontFamily: serif, fontSize: 22, fontWeight: 500, marginTop: 2, lineHeight: 1.1 }}>X-T5</div>
            <div style={{ fontSize: 11.5, color: sub, marginTop: 4, fontStyle: 'italic', fontFamily: serif }}>APS-C · 4020 万像素</div>
          </div>
        </div>
        <button style={{
          marginTop: 10, background: 'transparent', border: 0, color: sub,
          fontSize: 11, fontFamily: sans, cursor: 'pointer', padding: '4px 0',
          display: 'flex', alignItems: 'center', gap: 6,
        }}>
          <svg width="11" height="11" viewBox="0 0 11 11" fill="none" stroke="currentColor" strokeWidth="1.2"><path d="M2 5.5L9 5.5M5.5 2L9 5.5L5.5 9"/></svg>
          重绘插画
        </button>
      </div>

      {/* legend */}
      <div style={{ padding: '20px 22px 8px', display: 'flex', alignItems: 'center', gap: 14, fontSize: 10, color: sub, letterSpacing: '0.05em' }}>
        <span style={{ display: 'flex', alignItems: 'center' }}><ConfDot c="high"/>确定</span>
        <span style={{ display: 'flex', alignItems: 'center' }}><ConfDot c="med"/>可能</span>
        <span style={{ display: 'flex', alignItems: 'center' }}><ConfDot c="low"/>需补充</span>
      </div>

      {/* fields */}
      <div style={{ borderTop: `0.5px solid ${line}` }}>
        {fields.map((f, i) => {
          const isEditing = editing === i;
          return (
            <div key={f.k}
              onClick={() => setEditing(i)}
              style={{
                display: 'flex', alignItems: 'center', gap: 10,
                padding: '14px 22px', borderBottom: `0.5px solid ${line}`,
                cursor: 'pointer', background: isEditing ? card : 'transparent',
              }}>
              <ConfDot c={f.c}/>
              <div style={{ width: 76, fontSize: 11.5, color: sub, flexShrink: 0, letterSpacing: '0.04em' }}>{f.k}</div>
              {isEditing ? (
                <input autoFocus
                  defaultValue={f.v}
                  onBlur={(e) => { setFields(fields.map((x, j) => j === i ? { ...x, v: e.target.value, c: 'high' } : x)); setEditing(null); }}
                  onKeyDown={(e) => { if (e.key === 'Enter') e.target.blur(); }}
                  style={{
                    flex: 1, border: 0, background: 'transparent', outline: 'none',
                    fontFamily: serif, fontSize: 14, color: ink, fontWeight: 500,
                  }}/>
              ) : (
                <div style={{
                  flex: 1, fontFamily: serif, fontSize: 14, fontWeight: 500,
                  color: f.c === 'low' && !f.v ? sub : ink, fontStyle: f.c === 'low' && !f.v ? 'italic' : 'normal',
                }}>
                  {f.v || f.placeholder}
                </div>
              )}
              <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke={sub} strokeWidth="1"><path d="M2.5 6.5L6.5 2.5M5 2.5h1.5V4"/></svg>
            </div>
          );
        })}
      </div>

      {/* footer actions — sticky-ish, sits above the island */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 78,
        padding: '12px 18px', background: bg,
        borderTop: `0.5px solid ${line}`,
        display: 'flex', gap: 10, alignItems: 'center',
      }}>
        <button onClick={onBack}
          style={{
            flex: '0 0 auto', padding: '12px 16px', background: 'transparent',
            border: `0.5px solid ${line}`, color: ink, cursor: 'pointer',
            fontFamily: sans, fontSize: 12.5, borderRadius: 2,
          }}>继续修改</button>
        <button style={{
          flex: 1, padding: '12px 16px', background: ink, color: bg,
          border: 0, cursor: 'pointer',
          fontFamily: sans, fontSize: 13, fontWeight: 500, borderRadius: 2,
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        }}>
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5"><path d="M2 6l3 3 5-6"/></svg>
          确认收入图鉴
        </button>
      </div>
    </div>
  );
}

// ── Manual entry form ──
function AddManual({ bg, card, ink, sub, line, serif, sans, accent, onBack, Header }) {
  const fields = [
    { k: '品类', v: '', placeholder: '羽毛球 / 摄影 / 汽车 / 电子' },
    { k: '品牌', v: '' },
    { k: '型号', v: '' },
    { k: '昵称', v: '', placeholder: '（可选）给它起个名字' },
    { k: '颜色', v: '' },
    { k: '入手日期', v: '' },
    { k: '入手价格', v: '' },
    { k: '入手渠道', v: '' },
    { k: '一句话', v: '', placeholder: '一句话描述这件东西' },
  ];

  return (
    <div style={{ background: bg, minHeight: '100%', fontFamily: sans, color: ink, paddingBottom: 130 }}>
      <Header title="手动录入" sub="MANUAL ENTRY" onBack={onBack}/>

      {/* photo slot */}
      <div style={{ padding: '20px 22px 0' }}>
        <div style={{ fontSize: 10, color: sub, letterSpacing: '0.18em', textTransform: 'uppercase', marginBottom: 8 }}>I · 照片</div>
        <button style={{
          width: '100%', aspectRatio: '4 / 3', background: card,
          border: `0.5px dashed ${line}`, borderRadius: 2,
          color: sub, fontSize: 12, fontFamily: sans, cursor: 'pointer',
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 8,
        }}>
          <svg width="22" height="22" viewBox="0 0 22 22" fill="none" stroke="currentColor" strokeWidth="1"><rect x="3" y="5.5" width="16" height="12" rx="1.5"/><circle cx="11" cy="11.5" r="3"/><path d="M8 5.5l1.4-2h3.2l1.4 2"/></svg>
          <span>添加照片 · 后续 AI 会据此生成博物馆插画</span>
        </button>
      </div>

      {/* fields */}
      <div style={{ padding: '24px 22px 0' }}>
        <div style={{ fontSize: 10, color: sub, letterSpacing: '0.18em', textTransform: 'uppercase', marginBottom: 8 }}>II · 基本信息</div>
        <div style={{ borderTop: `0.5px solid ${line}` }}>
          {fields.map((f) => (
            <div key={f.k} style={{
              display: 'flex', alignItems: 'baseline', gap: 12,
              padding: '13px 0', borderBottom: `0.5px solid ${line}`,
            }}>
              <div style={{ width: 76, fontSize: 11.5, color: sub, flexShrink: 0, letterSpacing: '0.04em' }}>{f.k}</div>
              <div style={{ flex: 1, fontFamily: serif, fontSize: 14, color: f.v ? ink : sub, fontStyle: f.v ? 'normal' : 'italic', minHeight: 18 }}>
                {f.v || f.placeholder || '—'}
              </div>
              <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke={sub} strokeWidth="1"><path d="M2.5 6.5L6.5 2.5M5 2.5h1.5V4"/></svg>
            </div>
          ))}
        </div>
      </div>

      {/* assistive: AI fill button */}
      <div style={{ padding: '18px 22px 0' }}>
        <button style={{
          width: '100%', padding: '11px 14px', background: 'transparent',
          border: `0.5px solid ${line}`, borderRadius: 2,
          color: sub, fontSize: 12, fontFamily: sans, cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        }}>
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.2"><path d="M2 4l1.5-1.5L5 4M3.5 2.5V8M7 8l1.5 1.5L10 8M8.5 9.5V4"/></svg>
          让 AI 帮我补全空白字段
        </button>
      </div>

      {/* footer */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 78,
        padding: '12px 18px', background: bg,
        borderTop: `0.5px solid ${line}`,
        display: 'flex', gap: 10, alignItems: 'center',
      }}>
        <button onClick={onBack}
          style={{
            flex: '0 0 auto', padding: '12px 16px', background: 'transparent',
            border: `0.5px solid ${line}`, color: ink, cursor: 'pointer',
            fontFamily: sans, fontSize: 12.5, borderRadius: 2,
          }}>取消</button>
        <button style={{
          flex: 1, padding: '12px 16px', background: ink, color: bg,
          border: 0, cursor: 'pointer',
          fontFamily: sans, fontSize: 13, fontWeight: 500, borderRadius: 2,
        }}>收入图鉴</button>
      </div>
    </div>
  );
}

// ── Settings — AI provider + default art style ──
function SettingsScreen({ bg, card, ink, sub, line, serif, sans, accent }) {
  const Section = ({ label, children }) => (
    <div style={{ marginBottom: 28 }}>
      <div style={{ fontSize: 10, color: sub, letterSpacing: '0.12em', textTransform: 'uppercase', marginBottom: 10, padding: '0 22px' }}>{label}</div>
      <div style={{ background: card, borderTop: `0.5px solid ${line}`, borderBottom: `0.5px solid ${line}` }}>
        {children}
      </div>
    </div>
  );
  const Row = ({ label, value, last, action }) => (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      padding: '14px 22px',
      borderBottom: last ? 'none' : `0.5px solid ${line}`,
      cursor: action ? 'pointer' : 'default',
    }}>
      <div style={{ fontSize: 13.5, color: ink, fontFamily: sans }}>{label}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: sub, fontSize: 12.5, fontFamily: serif }}>
        {value}
        {action && (
          <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke="currentColor" strokeWidth="1.2"><path d="M3 1l4 4-4 4"/></svg>
        )}
      </div>
    </div>
  );

  const styles = ['像素风', '线稿', '水彩', '低多边', '写实'];
  const [activeStyle, setActiveStyle] = React.useState(0);

  return (
    <div style={{ background: bg, minHeight: '100%', fontFamily: sans, color: ink, paddingBottom: 110 }}>
      <div style={{ padding: '20px 22px 14px' }}>
        <div style={{ fontFamily: serif, fontSize: 28, lineHeight: 1.05, fontWeight: 500, letterSpacing: '-0.01em' }}>
          设置
        </div>
        <div style={{ fontSize: 11.5, color: sub, marginTop: 6, letterSpacing: '0.06em', textTransform: 'uppercase' }}>
          AI · 外观 · 关于
        </div>
      </div>

      <Section label="AI 服务">
        <Row label="服务商" value="Anthropic" action/>
        <Row label="模型" value="claude-haiku-4.5" action/>
        <Row label="API Key" value="sk-ant-···7f3a" action/>
        <Row label="录入助手" value="开启" last/>
      </Section>

      <Section label="默认插画风格">
        <div style={{ padding: '14px 22px' }}>
          <div style={{ display: 'flex', gap: 8, overflowX: 'auto' }}>
            {styles.map((s, i) => {
              const on = i === activeStyle;
              return (
                <button key={s} onClick={() => setActiveStyle(i)}
                  style={{
                    flexShrink: 0, padding: '8px 14px', borderRadius: 999,
                    border: `0.5px solid ${on ? ink : line}`,
                    background: on ? ink : 'transparent',
                    color: on ? bg : ink, cursor: 'pointer',
                    fontFamily: sans, fontSize: 12, fontWeight: 500,
                  }}>{s}</button>
              );
            })}
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginTop: 14 }}>
            {[itemById('cam-fuji-xt5'), itemById('racket-vt-zf2'), itemById('car-911')].map((it) => (
              <div key={it.id} style={{
                aspectRatio: '1', background: bg, border: `0.5px solid ${line}`,
                padding: 8, display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <div style={{ width: '85%', height: '85%' }}>
                  <ItemVector item={it}/>
                </div>
              </div>
            ))}
          </div>
          <div style={{ fontSize: 11, color: sub, marginTop: 10, lineHeight: 1.5 }}>
            录入新物件时会按这个风格生成插画。已有物件可以在详情页"重绘"。
          </div>
        </div>
      </Section>

      <Section label="外观">
        <Row label="主题" value="浅色" action/>
        <Row label="字体" value="Cormorant Garamond" action/>
        <Row label="语言" value="简体中文" last/>
      </Section>

      <Section label="数据">
        <Row label="导出 JSON" value="" action/>
        <Row label="iCloud 同步" value="已开启" last/>
      </Section>
    </div>
  );
}

// ── Helpers ──
function IslandIcon({ name, size = 14 }) {
  const s = { width: size, height: size, fill: 'none', stroke: 'currentColor', strokeWidth: 1.4, strokeLinecap: 'round', strokeLinejoin: 'round' };
  if (name === 'book') return <svg viewBox="0 0 14 14" {...s}><path d="M2 2h4a2 2 0 012 2v8a2 2 0 00-2-2H2zM12 2H8a2 2 0 00-2 2v8a2 2 0 012-2h4z"/></svg>;
  if (name === 'door') return <svg viewBox="0 0 14 14" {...s}><path d="M3 12V3a1 1 0 011-1h6a1 1 0 011 1v9M2 12h10"/><circle cx="9" cy="7.5" r="0.5" fill="currentColor"/></svg>;
  if (name === 'plus') return <svg viewBox="0 0 14 14" {...s}><path d="M7 3v8M3 7h8"/></svg>;
  if (name === 'gear') return <svg viewBox="0 0 14 14" {...s}><circle cx="7" cy="7" r="2.2"/><path d="M7 1.5v1.5M7 11v1.5M1.5 7H3M11 7h1.5M3 3l1 1M10 10l1 1M3 11l1-1M10 4l1-1"/></svg>;
  return null;
}

// Mock photo placeholders — striped cards in the item's palette.
function mockPhotosFor(item) {
  const p = item.palette;
  const captions = {
    'racket-vt-zf2':    ['场馆 · 北京', '挂袋', '比赛后', '换线特写'],
    'racket-arc11':     ['用了三年', '出手前', '断线瞬间'],
    'racket-astrox-99': ['新拍合影', '俱乐部联赛'],
    'shoes-shb65':      ['鞋盒开箱', '场地', '磨损'],
    'cam-fuji-xt5':     ['京都岚山', '伊豆海岸', '机身特写', '23mm 挂机', '冰岛极光'],
    'lens-23f2':        ['挂机', '街拍'],
    'lens-56f12':       ['婚礼现场', '人像'],
    'cam-ricoh-gr3':    ['曼谷夜市', '东京中野', '口袋日常'],
    'cam-leica-m6':     ['北京胡同', '第一卷 Portra', 'CLA 后', '镜头合影'],
    'tripod-rrs':       ['冰岛 -22℃', '海边长曝'],
    'car-bmw-m2':       ['张家口公路', '草原天路', '停车场'],
    'car-911':          ['Big Sur', '金门大桥', '日落 Carmel', 'SFO 提车'],
    'car-defender':     ['稻城牛奶海', '新都桥', '高原营地', '泥泞段'],
    'tech-mbp':         ['工位', '咖啡馆', '外接屏'],
    'tech-airpods':     ['开箱'],
    'tech-kindle':      ['深夜读书', '咖啡馆'],
    'tech-watch':       ['半马', '游泳'],
    'tech-ipad':        ['画图', '会议'],
  }[item.id] || ['场景一', '场景二', '场景三', '场景四', '场景五'];
  return captions.map((c, i) => ({
    caption: c,
    bg: p[i % p.length],
    shade1: p[i % p.length],
    shade2: p[(i + 1) % p.length],
    angle: 25 + (i * 17) % 60,
  }));
}

// ── Portal — 大门 / 门厅 ──
function Portal({ bg, card, ink, sub, line, accent, serif, sans, tweaks, onEnterCategory, onOpenItem }) {
  const today = new Date('2026-05-06');
  const totalItems = ITEMS.length;
  const owned = ITEMS.filter((i) => i.status === 'owned').length;
  const recent = ITEMS.slice().sort((a, b) => (b.acquired || '').localeCompare(a.acquired || ''))[0];
  const monthLabel = today.toLocaleDateString('en', { month: 'long' }).toUpperCase();

  return (
    <div style={{ background: bg, minHeight: '100%', fontFamily: sans, color: ink, paddingBottom: 110, position: 'relative' }}>
      {/* top date strip */}
      <div style={{ display: 'flex', justifyContent: 'space-between', padding: '18px 24px 0', fontFamily: 'ui-monospace, monospace', fontSize: 9.5, color: sub, letterSpacing: '0.18em' }}>
        <span>EST. 2020</span>
        <span>{monthLabel} VI · MMXXVI</span>
      </div>

      {/* ornamental rule */}
      <div style={{ padding: '38px 24px 0' }}>
        <Ornament color={ink} sub={sub}/>
      </div>

      {/* grand title */}
      <div style={{ padding: '20px 24px 0', textAlign: 'center' }}>
        <div style={{ fontFamily: serif, fontSize: 64, lineHeight: 1, fontWeight: 500, letterSpacing: '-0.03em' }}>
          Treasure
        </div>
        <div style={{ fontFamily: serif, fontStyle: 'italic', fontSize: 14, color: sub, marginTop: 10, letterSpacing: '0.02em' }}>
          a private cabinet of things owned, used, & remembered
        </div>
      </div>

      {/* tally */}
      <div style={{ display: 'flex', justifyContent: 'center', gap: 28, marginTop: 26, padding: '14px 0', borderTop: `0.5px solid ${line}`, borderBottom: `0.5px solid ${line}`, marginInline: 32 }}>
        {[
          { n: totalItems, l: 'items' },
          { n: owned, l: 'owned' },
          { n: CATEGORIES.length, l: 'rooms' },
        ].map((t) => (
          <div key={t.l} style={{ textAlign: 'center' }}>
            <div style={{ fontFamily: serif, fontSize: 22, fontWeight: 500, fontVariantNumeric: 'tabular-nums' }}>{String(t.n).padStart(2, '0')}</div>
            <div style={{ fontSize: 9, color: sub, letterSpacing: '0.18em', textTransform: 'uppercase', marginTop: 2 }}>{t.l}</div>
          </div>
        ))}
      </div>

      {/* the four doorways */}
      <div style={{ padding: '26px 22px 0' }}>
        <div style={{ fontSize: 9.5, color: sub, letterSpacing: '0.2em', textTransform: 'uppercase', textAlign: 'center', marginBottom: 14 }}>
          ✦ The Rooms ✦
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          {CATEGORIES.map((c, i) => {
            const latest = itemsByCategory(c.id).slice().sort((a, b) => (b.acquired || '').localeCompare(a.acquired || ''))[0];
            return (
              <button key={c.id} onClick={() => onEnterCategory(c.id)}
                style={{
                  textAlign: 'left', cursor: 'pointer', background: card,
                  border: `0.5px solid ${line}`, borderRadius: 2, padding: 14,
                  display: 'flex', flexDirection: 'column', gap: 8, position: 'relative',
                  fontFamily: sans, color: ink,
                }}>
                {/* roman numeral corner */}
                <div style={{ position: 'absolute', top: 10, right: 12, fontFamily: 'ui-monospace, monospace', fontSize: 9, color: sub, letterSpacing: '0.1em' }}>
                  {['I','II','III','IV'][i]}
                </div>
                <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: 0.75 }}>
                  {latest ? <div style={{ width: '88%', height: '100%' }}><ItemVector item={latest}/></div> : <CatIcon id={c.id} size={36} color={ink}/>}
                </div>
                <div style={{ borderTop: `0.5px solid ${line}`, paddingTop: 8 }}>
                  <div style={{ fontFamily: serif, fontSize: 18, fontWeight: 500, lineHeight: 1.1 }}>{c.name}</div>
                  <div style={{ fontSize: 10, color: sub, marginTop: 3, letterSpacing: '0.05em' }}>
                    {c.count} pcs · {c.name_en}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* latest entry */}
      {recent && (
        <div style={{ padding: '24px 22px 0' }}>
          <div style={{ fontSize: 9.5, color: sub, letterSpacing: '0.2em', textTransform: 'uppercase', marginBottom: 10 }}>
            ✦ Latest entry
          </div>
          <button onClick={() => onOpenItem(recent.id)}
            style={{
              width: '100%', textAlign: 'left', cursor: 'pointer', background: 'transparent',
              border: `0.5px solid ${line}`, padding: '14px 16px',
              display: 'flex', alignItems: 'center', gap: 14, color: ink,
            }}>
            <div style={{ width: 56, height: 56, background: card, padding: 6, flexShrink: 0 }}>
              <ItemVector item={recent}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 9.5, color: sub, letterSpacing: '0.1em', textTransform: 'uppercase' }}>{fmtDate(recent.acquired)} · {recent.brand}</div>
              <div style={{ fontFamily: serif, fontSize: 17, fontWeight: 500, marginTop: 2, lineHeight: 1.15 }}>{recent.nickname}</div>
              <div style={{ fontSize: 11, color: sub, marginTop: 2, fontStyle: 'italic', fontFamily: serif }}>{recent.model}</div>
            </div>
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1"><path d="M4 2l4 4-4 4"/></svg>
          </button>
        </div>
      )}

      {/* bottom ornament */}
      <div style={{ padding: '32px 24px 0' }}>
        <Ornament color={ink} sub={sub} flip/>
      </div>
    </div>
  );
}

function Ornament({ color, sub, flip }) {
  return (
    <svg viewBox="0 0 320 22" style={{ width: '100%', height: 22, display: 'block', transform: flip ? 'scaleY(-1)' : 'none' }}>
      <line x1="0" y1="11" x2="120" y2="11" stroke={sub} strokeWidth="0.5"/>
      <line x1="200" y1="11" x2="320" y2="11" stroke={sub} strokeWidth="0.5"/>
      <g stroke={color} strokeWidth="0.7" fill="none" opacity="0.85">
        <circle cx="160" cy="11" r="3"/>
        <circle cx="160" cy="11" r="6.5"/>
        <line x1="146" y1="11" x2="153" y2="11"/>
        <line x1="167" y1="11" x2="174" y2="11"/>
        <path d="M160 4 L162 8 L160 11 L158 8 Z" fill={color}/>
        <path d="M160 18 L162 14 L160 11 L158 14 Z" fill={color}/>
      </g>
    </svg>
  );
}

window.DirectionA = DirectionA;
