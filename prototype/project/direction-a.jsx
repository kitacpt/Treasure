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
    <AddScreen bg={bg} card={card} ink={ink} sub={sub} line={line} serif={serif} sans={sans} accent={accent}/>
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

// ── Add screen — 对话式 ──
function AddScreen({ bg, card, ink, sub, line, serif, sans, accent }) {
  const messages = [
    { role: 'assistant', text: '你好。把新东西的照片发给我，或者直接说说它是什么。' },
    { role: 'user', kind: 'photo', caption: '一张相机的照片' },
    { role: 'assistant', text: '看起来是 Fujifilm X-T5，黑色机身。是吗？' },
    { role: 'user', text: '对' },
    { role: 'assistant', text: '什么时候买的，多少钱？' },
    { role: 'user', text: '2023 年情人节，¥12500' },
    { role: 'assistant', text: '好。我已经填好了基本信息，正在生成像素风插画…' },
    { role: 'card', preview: true },
  ];
  return (
    <div style={{ background: bg, minHeight: '100%', fontFamily: sans, color: ink, paddingBottom: 110 }}>
      <div style={{ padding: '20px 22px 14px', borderBottom: `0.5px solid ${line}` }}>
        <div style={{ fontFamily: serif, fontSize: 28, lineHeight: 1.05, fontWeight: 500, letterSpacing: '-0.01em' }}>
          录入新物件
        </div>
        <div style={{ fontSize: 11.5, color: sub, marginTop: 6, letterSpacing: '0.06em', textTransform: 'uppercase' }}>
          Tell me about it · powered by Claude
        </div>
      </div>

      <div style={{ padding: '18px 22px 0', display: 'flex', flexDirection: 'column', gap: 14 }}>
        {messages.map((m, i) => {
          if (m.role === 'card') {
            return (
              <div key={i} style={{
                background: card, border: `0.5px solid ${line}`, borderRadius: 2,
                padding: 14, display: 'flex', gap: 12, alignItems: 'center',
              }}>
                <div style={{ width: 64, height: 64, background: bg, border: `0.5px solid ${line}`, padding: 6 }}>
                  <ItemVector item={itemById('cam-fuji-xt5')}/>
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 10, color: sub, letterSpacing: '0.08em', textTransform: 'uppercase' }}>FUJIFILM · 2023</div>
                  <div style={{ fontFamily: serif, fontSize: 17, fontWeight: 500, marginTop: 2 }}>X-T5</div>
                  <div style={{ fontSize: 11, color: sub, marginTop: 2 }}>¥12,500 · 摄影</div>
                </div>
                <button style={{
                  padding: '8px 12px', background: ink, color: bg,
                  border: 0, borderRadius: 999, cursor: 'pointer',
                  fontFamily: sans, fontSize: 12, fontWeight: 500,
                }}>收入图鉴</button>
              </div>
            );
          }
          const isUser = m.role === 'user';
          return (
            <div key={i} style={{ display: 'flex', justifyContent: isUser ? 'flex-end' : 'flex-start' }}>
              {m.kind === 'photo' ? (
                <div style={{
                  width: 120, height: 120, background: '#3a3530',
                  borderRadius: 6, position: 'relative', overflow: 'hidden',
                  border: `0.5px solid ${line}`,
                }}>
                  <div style={{ position: 'absolute', inset: 0, background: 'repeating-linear-gradient(35deg, #3a3530 0 7px, #2a2520 7px 14px)' }}/>
                  <div style={{ position: 'absolute', bottom: 6, left: 7, fontFamily: 'ui-monospace, monospace', fontSize: 8, color: '#fff', opacity: 0.7 }}>{m.caption}</div>
                </div>
              ) : (
                <div style={{
                  maxWidth: '78%', padding: '10px 14px',
                  background: isUser ? ink : card,
                  color: isUser ? bg : ink,
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

      {/* composer */}
      <div style={{
        position: 'absolute', left: 14, right: 14, bottom: 80,
        background: card, border: `0.5px solid ${line}`, borderRadius: 22,
        padding: '8px 8px 8px 16px', display: 'flex', alignItems: 'center', gap: 8,
        boxShadow: '0 6px 20px rgba(0,0,0,0.06)',
      }}>
        <button style={{ background: 'transparent', border: 0, color: sub, padding: 6, cursor: 'pointer' }}>
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"><rect x="2.5" y="4" width="13" height="10" rx="1.5"/><circle cx="9" cy="9" r="2.5"/><path d="M6 4l1-1.5h4L12 4"/></svg>
        </button>
        <div style={{ flex: 1, fontSize: 13, color: sub }}>说说这件东西…</div>
        <button style={{
          width: 32, height: 32, borderRadius: 999, background: ink, color: bg,
          border: 0, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><path d="M7 11V3M3.5 6.5L7 3l3.5 3.5"/></svg>
        </button>
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
