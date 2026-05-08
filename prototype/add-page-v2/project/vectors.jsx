// Treasure — vector hero illustrations (museum / botanical-plate style).
// Style rules:
//   - Hairline ink contour (0.6–1.0 stroke), darker than the item
//   - Flat tonal washes inside (lighter, slightly desaturated)
//   - One or two callout lines with a roman numeral and label
//   - No gradients, no glow, no fake 3D
// All shapes are simple SVG primitives — circles, rects, polygons, no
// freehand path acrobatics.

// ── helpers ──
const ink = '#1a1815';          // hairline contour
const wash = (c, a = 0.55) => c; // placeholder — we use opacity directly

// A small label rendered like a museum plate caption.
function PlateLabel({ x, y, text, num, anchor = 'start' }) {
  return (
    <g>
      <text x={x} y={y} fontFamily="'Cormorant Garamond', serif"
        fontStyle="italic" fontSize="9" fill={ink} textAnchor={anchor}
        opacity="0.75">
        {num && <tspan letterSpacing="0.5" style={{ fontVariant: 'small-caps' }}>{num} · </tspan>}
        {text}
      </text>
    </g>
  );
}

// Callout line — small dot at object, line, ending where label sits.
function Callout({ from, to }) {
  return (
    <g stroke={ink} strokeWidth="0.5" opacity="0.45" fill="none">
      <circle cx={from[0]} cy={from[1]} r="0.9" fill={ink}/>
      <line x1={from[0]} y1={from[1]} x2={to[0]} y2={to[1]}/>
    </g>
  );
}

// ── Badminton racket ─────────────────────────────────────────────
function VRacket({ palette = ['#1a1a1a','#3a3a3a','#e8e2d4','#7a7a7a'], style }) {
  const [c0, c1, c2, c3] = palette;
  // c0 = frame primary, c2 = strings tone, c3 = grip
  return (
    <svg viewBox="0 0 220 340" xmlns="http://www.w3.org/2000/svg" style={{ width: '100%', height: '100%', ...style }}>
      <defs>
        <clipPath id="racketHead"><ellipse cx="110" cy="92" rx="64" ry="74"/></clipPath>
      </defs>

      {/* head wash */}
      <ellipse cx="110" cy="92" rx="64" ry="74" fill={c2} opacity="0.18"/>
      {/* strings */}
      <g stroke={ink} strokeWidth="0.4" opacity="0.45" clipPath="url(#racketHead)">
        {Array.from({ length: 18 }).map((_, i) => (
          <line key={'v'+i} x1={50 + i * 7} y1="14" x2={50 + i * 7} y2="172"/>
        ))}
        {Array.from({ length: 20 }).map((_, i) => (
          <line key={'h'+i} x1="40" y1={22 + i * 7} x2="180" y2={22 + i * 7}/>
        ))}
      </g>
      {/* head outer ring (ink) */}
      <ellipse cx="110" cy="92" rx="64" ry="74" fill="none" stroke={ink} strokeWidth="1"/>
      {/* head color band */}
      <ellipse cx="110" cy="92" rx="64" ry="74" fill="none" stroke={c0} strokeWidth="2" opacity="0.55"/>
      {/* T-joint */}
      <rect x="100" y="160" width="20" height="14" fill={c0} opacity="0.7"/>
      <rect x="100" y="160" width="20" height="14" fill="none" stroke={ink} strokeWidth="0.7"/>
      {/* shaft */}
      <rect x="106" y="172" width="8" height="84" fill={c0} opacity="0.55"/>
      <rect x="106" y="172" width="8" height="84" fill="none" stroke={ink} strokeWidth="0.7"/>
      {/* grip */}
      <rect x="98" y="252" width="24" height="74" rx="2" fill={c3} opacity="0.5"/>
      <rect x="98" y="252" width="24" height="74" rx="2" fill="none" stroke={ink} strokeWidth="0.7"/>
      {Array.from({ length: 12 }).map((_, i) => (
        <line key={i} x1="98" y1={258 + i * 6} x2="122" y2={254 + i * 6}
          stroke={ink} strokeWidth="0.4" opacity="0.4"/>
      ))}
      <rect x="96" y="324" width="28" height="6" fill="none" stroke={ink} strokeWidth="0.7"/>

      {/* callouts */}
      <Callout from={[170, 70]} to={[200, 50]}/>
      <PlateLabel x={202} y={52} num="i" text="head"/>
      <Callout from={[114, 168]} to={[40, 210]}/>
      <PlateLabel x={38} y={213} text="t-joint" num="ii" anchor="end"/>
      <Callout from={[120, 290]} to={[195, 290]}/>
      <PlateLabel x={196} y={292} text="grip" num="iii"/>
    </svg>
  );
}

// ── Camera body — front, museum plate ─────────────────────────────
function VCamera({ palette = ['#1a1a1a','#3a3a3a','#d8d2c4','#7a7a7a'], style }) {
  const [c0, c1, c2, c3] = palette;
  return (
    <svg viewBox="0 0 340 240" xmlns="http://www.w3.org/2000/svg" style={{ width: '100%', height: '100%', ...style }}>
      {/* body wash */}
      <rect x="40" y="70" width="240" height="130" rx="8" fill={c0} opacity="0.18"/>
      {/* top hump */}
      <path d="M124 70 V46 Q124 38 132 38 H192 Q200 38 200 46 V70 Z" fill={c0} opacity="0.18"/>
      {/* grip */}
      <path d="M40 70 H68 L72 100 L68 180 L60 196 H40 Z" fill={c0} opacity="0.28"/>
      {/* lens mount */}
      <circle cx="160" cy="135" r="56" fill={c1} opacity="0.32"/>
      <circle cx="160" cy="135" r="46" fill={c0} opacity="0.45"/>
      <circle cx="160" cy="135" r="34" fill={c2} opacity="0.4"/>
      <circle cx="160" cy="135" r="22" fill={c0} opacity="0.65"/>
      <circle cx="160" cy="135" r="12" fill={c1} opacity="0.55"/>
      {/* highlight on glass — subtle */}
      <ellipse cx="152" cy="124" rx="6" ry="3" fill={c2} opacity="0.45"/>

      {/* ink contours */}
      <g fill="none" stroke={ink} strokeWidth="0.9">
        <rect x="40" y="70" width="240" height="130" rx="8"/>
        <path d="M124 70 V46 Q124 38 132 38 H192 Q200 38 200 46 V70"/>
        <path d="M40 70 H68 L72 100 L68 180 L60 196"/>
        <circle cx="160" cy="135" r="56"/>
        <circle cx="160" cy="135" r="46"/>
        <circle cx="160" cy="135" r="34"/>
        <circle cx="160" cy="135" r="22"/>
        <circle cx="160" cy="135" r="12"/>
        {/* shutter button */}
        <circle cx="248" cy="86" r="8"/>
        <circle cx="248" cy="86" r="3.5"/>
        {/* dial */}
        <circle cx="222" cy="56" r="13"/>
        <circle cx="222" cy="56" r="9" opacity="0.5"/>
        {/* viewfinder window */}
        <rect x="58" y="86" width="14" height="10" rx="1"/>
        {/* hot shoe */}
        <rect x="148" y="32" width="24" height="10"/>
        {/* model line engraving */}
        <line x1="80" y1="172" x2="124" y2="172" opacity="0.5"/>
        <line x1="80" y1="178" x2="106" y2="178" opacity="0.4"/>
      </g>

      {/* dial ticks */}
      <g stroke={ink} strokeWidth="0.5" opacity="0.55">
        {Array.from({ length: 12 }).map((_, i) => {
          const a = (i / 12) * Math.PI * 2;
          return <line key={i}
            x1={222 + Math.cos(a) * 9} y1={56 + Math.sin(a) * 9}
            x2={222 + Math.cos(a) * 13} y2={56 + Math.sin(a) * 13}/>;
        })}
      </g>

      <Callout from={[160, 80]} to={[160, 14]}/>
      <PlateLabel x={160} y={12} num="i" text="pentaprism" anchor="middle"/>
      <Callout from={[160, 135]} to={[296, 138]}/>
      <PlateLabel x={298} y={140} num="ii" text="lens mount"/>
      <Callout from={[54, 180]} to={[14, 210]}/>
      <PlateLabel x={12} y={213} text="grip" num="iii" anchor="end"/>
    </svg>
  );
}

// ── Lens — side profile ──────────────────────────────────────────
function VLens({ palette = ['#1a1a1a','#3a3a3a','#d8d2c4','#7a7a7a'], style }) {
  const [c0, c1, c2, c3] = palette;
  return (
    <svg viewBox="0 0 340 240" xmlns="http://www.w3.org/2000/svg" style={{ width: '100%', height: '100%', ...style }}>
      {/* mount */}
      <rect x="60" y="92" width="14" height="68" fill={c1} opacity="0.4"/>
      {/* barrel sections */}
      <rect x="74" y="84" width="46" height="84" fill={c0} opacity="0.22"/>
      <rect x="120" y="78" width="34" height="96" fill={c0} opacity="0.32"/>
      <rect x="154" y="70" width="56" height="112" fill={c0} opacity="0.22"/>
      <rect x="210" y="68" width="22" height="116" fill={c0} opacity="0.36"/>
      {/* hood */}
      <path d="M232 68 L274 60 L274 192 L232 184 Z" fill={c1} opacity="0.32"/>
      {/* front element */}
      <ellipse cx="270" cy="126" rx="7" ry="64" fill={c0} opacity="0.6"/>
      <ellipse cx="273" cy="120" rx="3" ry="40" fill={c2} opacity="0.5"/>

      {/* contours */}
      <g fill="none" stroke={ink} strokeWidth="0.8">
        <rect x="60" y="92" width="14" height="68"/>
        <rect x="74" y="84" width="46" height="84"/>
        <rect x="120" y="78" width="34" height="96"/>
        <rect x="154" y="70" width="56" height="112"/>
        <rect x="210" y="68" width="22" height="116"/>
        <path d="M232 68 L274 60 L274 192 L232 184 Z"/>
        <ellipse cx="270" cy="126" rx="7" ry="64"/>
      </g>
      {/* focus ring grooves */}
      <g stroke={ink} strokeWidth="0.4" opacity="0.55">
        {Array.from({ length: 14 }).map((_, i) => (
          <line key={i} x1={158 + i * 3.5} y1="74" x2={158 + i * 3.5} y2="178"/>
        ))}
      </g>
      {/* aperture/red ring */}
      <rect x="150" y="68" width="3" height="116" fill={c2} opacity="0.7"/>

      <Callout from={[180, 70]} to={[180, 14]}/>
      <PlateLabel x={180} y={12} num="i" text="focus ring" anchor="middle"/>
      <Callout from={[270, 60]} to={[314, 36]}/>
      <PlateLabel x={316} y={38} num="ii" text="hood"/>
      <Callout from={[68, 160]} to={[24, 200]}/>
      <PlateLabel x={22} y={203} text="mount" num="iii" anchor="end"/>
    </svg>
  );
}

// ── Tripod ────────────────────────────────────────────────────────
function VTripod({ palette = ['#1a1a1a','#3a3a3a','#c4b89c','#7a6f56'], style }) {
  const [c0, c1, c2, c3] = palette;
  return (
    <svg viewBox="0 0 240 340" xmlns="http://www.w3.org/2000/svg" style={{ width: '100%', height: '100%', ...style }}>
      {/* head plate */}
      <rect x="100" y="40" width="40" height="14" fill={c0} opacity="0.55"/>
      <circle cx="120" cy="68" r="20" fill={c1} opacity="0.45"/>
      <circle cx="120" cy="68" r="11" fill={c0} opacity="0.7"/>
      {/* center column */}
      <rect x="115" y="84" width="10" height="42" fill={c0} opacity="0.55"/>
      {/* apex */}
      <path d="M84 116 L120 104 L156 116 V134 H84 Z" fill={c1} opacity="0.45"/>

      {/* legs */}
      {[
        { x1: 96, x2: 30 }, { x1: 120, x2: 120 }, { x1: 144, x2: 210 }
      ].map((leg, i) => {
        const seg = (t1, t2) => ({
          x1: leg.x1 + (leg.x2 - leg.x1) * t1, y1: 130 + (308 - 130) * t1,
          x2: leg.x1 + (leg.x2 - leg.x1) * t2, y2: 130 + (308 - 130) * t2,
        });
        const s1 = seg(0, 0.34), s2 = seg(0.32, 0.66), s3 = seg(0.64, 1);
        return (
          <g key={i}>
            <line {...s1} stroke={c0} strokeWidth="9" opacity="0.45" strokeLinecap="butt"/>
            <line {...s2} stroke={c0} strokeWidth="7" opacity="0.55"/>
            <line {...s3} stroke={c0} strokeWidth="5" opacity="0.7"/>
            <line {...s1} stroke={ink} strokeWidth="0.7" fill="none"/>
            <line {...s2} stroke={ink} strokeWidth="0.7"/>
            <line {...s3} stroke={ink} strokeWidth="0.7"/>
            <circle cx={leg.x2} cy="310" r="4" fill={c2} stroke={ink} strokeWidth="0.6"/>
          </g>
        );
      })}

      {/* contours on head + apex */}
      <g fill="none" stroke={ink} strokeWidth="0.8">
        <rect x="100" y="40" width="40" height="14"/>
        <circle cx="120" cy="68" r="20"/>
        <circle cx="120" cy="68" r="11"/>
        <rect x="115" y="84" width="10" height="42"/>
        <path d="M84 116 L120 104 L156 116 V134 H84 Z"/>
      </g>

      <Callout from={[140, 68]} to={[200, 40]}/>
      <PlateLabel x={202} y={42} num="i" text="ball head"/>
      <Callout from={[156, 130]} to={[214, 156]}/>
      <PlateLabel x={216} y={158} num="ii" text="apex"/>
      <Callout from={[26, 310]} to={[6, 332]}/>
      <PlateLabel x={4} y={335} text="rubber foot" num="iii" anchor="end"/>
    </svg>
  );
}

// ── Shoe — side profile ──────────────────────────────────────────
function VShoe({ palette = ['#f5f1e8','#1a1a1a','#c9362f','#7a7a7a'], style }) {
  const [c0, c1, c2, c3] = palette;
  return (
    <svg viewBox="0 0 340 220" xmlns="http://www.w3.org/2000/svg" style={{ width: '100%', height: '100%', ...style }}>
      {/* sole */}
      <path d="M30 156 L300 144 Q314 148 312 166 L300 178 L42 178 Q26 170 30 156 Z" fill={c1} opacity="0.55"/>
      {/* midsole */}
      <path d="M40 134 L300 130 Q310 132 306 146 L40 156 Z" fill={c0} opacity="0.7"/>
      {/* upper */}
      <path d="M50 132 Q60 76 134 68 L218 60 Q272 60 296 100 L302 132 Z" fill={c0} opacity="0.55"/>
      {/* tongue */}
      <path d="M154 70 L218 60 L228 100 L164 110 Z" fill={c0} opacity="0.4"/>

      <g fill="none" stroke={ink} strokeWidth="0.9">
        <path d="M30 156 L300 144 Q314 148 312 166 L300 178 L42 178 Q26 170 30 156 Z"/>
        <path d="M40 134 L300 130 Q310 132 306 146"/>
        <path d="M50 132 Q60 76 134 68 L218 60 Q272 60 296 100 L302 132"/>
        <path d="M154 70 L218 60 L228 100 L164 110 Z"/>
      </g>

      {/* swoosh accent */}
      <path d="M70 124 Q140 94 218 100" stroke={c2} strokeWidth="5" fill="none" opacity="0.7"/>
      <path d="M70 124 Q140 94 218 100" stroke={ink} strokeWidth="0.5" fill="none" opacity="0.5"/>

      {/* laces */}
      {Array.from({ length: 5 }).map((_, i) => (
        <line key={i} x1={176 - i * 4} y1={84 + i * 6} x2={222 - i * 4} y2={80 + i * 6}
          stroke={ink} strokeWidth="1" strokeLinecap="round" opacity="0.7"/>
      ))}

      <Callout from={[230, 70]} to={[300, 36]}/>
      <PlateLabel x={302} y={38} num="i" text="upper"/>
      <Callout from={[170, 178]} to={[170, 208]}/>
      <PlateLabel x={170} y={210} num="ii" text="outsole" anchor="middle"/>
    </svg>
  );
}

// ── Car — 3-quarter view ─────────────────────────────────────────
function VCar({ palette = ['#1a1a1a','#2a2a2a','#c9362f','#a8a8a8'], style }) {
  const [c0, c1, c2, c3] = palette;
  return (
    <svg viewBox="0 0 380 220" xmlns="http://www.w3.org/2000/svg" style={{ width: '100%', height: '100%', ...style }}>
      {/* shadow */}
      <ellipse cx="190" cy="190" rx="158" ry="5" fill={ink} opacity="0.12"/>
      {/* body wash */}
      <path d="M40 152 L60 120 Q82 98 122 92 L162 76 Q204 70 246 76 L286 92 Q328 96 342 124 L350 152 V168 H40 Z" fill={c0} opacity="0.42"/>
      {/* roof */}
      <path d="M122 92 Q162 76 218 76 L264 90 Q280 94 290 116 V128 H122 Z" fill={c0} opacity="0.55"/>
      {/* windows */}
      <path d="M132 100 Q162 84 200 82 L246 92 Q260 96 268 116 V124 H132 Z" fill={c2} opacity="0.32"/>

      <g fill="none" stroke={ink} strokeWidth="0.9">
        <path d="M40 152 L60 120 Q82 98 122 92 L162 76 Q204 70 246 76 L286 92 Q328 96 342 124 L350 152 V168 H40 Z"/>
        <path d="M122 92 Q162 76 218 76 L264 90 Q280 94 290 116 V128"/>
        <path d="M132 100 Q162 84 200 82 L246 92 Q260 96 268 116 V124"/>
        <line x1="200" y1="82" x2="200" y2="124"/>
        <line x1="60" y1="128" x2="342" y2="128" opacity="0.55"/>
        {/* door cuts */}
        <line x1="158" y1="128" x2="162" y2="168" opacity="0.55"/>
        <line x1="244" y1="128" x2="248" y2="168" opacity="0.55"/>
      </g>

      {/* lights */}
      <ellipse cx="52" cy="148" rx="10" ry="6" fill={c2} opacity="0.6"/>
      <ellipse cx="338" cy="148" rx="9" ry="5" fill={c2} opacity="0.7"/>
      <ellipse cx="52" cy="148" rx="10" ry="6" fill="none" stroke={ink} strokeWidth="0.6"/>
      <ellipse cx="338" cy="148" rx="9" ry="5" fill="none" stroke={ink} strokeWidth="0.6"/>

      {/* wheels */}
      {[110, 290].map((cx) => (
        <g key={cx}>
          <circle cx={cx} cy="170" r="22" fill={c1} opacity="0.85"/>
          <circle cx={cx} cy="170" r="22" fill="none" stroke={ink} strokeWidth="0.9"/>
          <circle cx={cx} cy="170" r="13" fill={c0} opacity="0.6"/>
          <circle cx={cx} cy="170" r="13" fill="none" stroke={ink} strokeWidth="0.6"/>
          <circle cx={cx} cy="170" r="4" fill={c3}/>
          {Array.from({ length: 5 }).map((_, i) => {
            const a = (i / 5) * Math.PI * 2 - Math.PI / 2;
            return <line key={i} x1={cx} y1="170"
              x2={cx + Math.cos(a) * 12} y2={170 + Math.sin(a) * 12}
              stroke={ink} strokeWidth="0.6" opacity="0.7"/>;
          })}
        </g>
      ))}

      <Callout from={[200, 78]} to={[200, 14]}/>
      <PlateLabel x={200} y={12} num="i" text="cabin" anchor="middle"/>
      <Callout from={[342, 124]} to={[376, 100]}/>
      <PlateLabel x={376} y={100} num="ii" text="rear quarter" anchor="end"/>
      <Callout from={[110, 192]} to={[28, 212]}/>
      <PlateLabel x={26} y={215} text="wheel" num="iii" anchor="end"/>
    </svg>
  );
}

// ── Laptop — open clamshell ──────────────────────────────────────
function VLaptop({ palette = ['#3a3a3c','#1a1a1c','#d8d2c4','#8a8378'], style }) {
  const [c0, c1, c2, c3] = palette;
  return (
    <svg viewBox="0 0 340 240" xmlns="http://www.w3.org/2000/svg" style={{ width: '100%', height: '100%', ...style }}>
      {/* lid */}
      <path d="M76 38 L262 38 L272 156 L66 156 Z" fill={c0} opacity="0.5"/>
      {/* screen */}
      <path d="M82 46 L256 46 L264 150 L74 150 Z" fill={c1} opacity="0.7"/>
      {/* screen content — abstract document blocks */}
      <g opacity="0.45">
        <rect x="98" y="62" width="58" height="6" fill={c2}/>
        <rect x="98" y="74" width="120" height="3" fill={c2}/>
        <rect x="98" y="82" width="86" height="3" fill={c2}/>
        <rect x="98" y="100" width="36" height="28" fill={c2}/>
        <rect x="138" y="100" width="36" height="28" fill={c2}/>
        <rect x="178" y="100" width="36" height="28" fill={c2}/>
      </g>
      {/* hinge */}
      <path d="M66 156 L272 156 L280 162 L60 162 Z" fill={c1} opacity="0.7"/>
      {/* base */}
      <path d="M30 162 L308 162 L320 200 L20 200 Z" fill={c0} opacity="0.42"/>
      {/* trackpad */}
      <rect x="124" y="180" width="86" height="3" fill={c1} opacity="0.55"/>

      <g fill="none" stroke={ink} strokeWidth="0.9">
        <path d="M76 38 L262 38 L272 156 L66 156 Z"/>
        <path d="M82 46 L256 46 L264 150 L74 150 Z"/>
        <path d="M66 156 L272 156 L280 162 L60 162 Z"/>
        <path d="M30 162 L308 162 L320 200 L20 200 Z"/>
        <rect x="124" y="180" width="86" height="3" opacity="0.6"/>
      </g>
      {/* keyboard rows */}
      {Array.from({ length: 4 }).map((_, r) => (
        <line key={r} x1={50 + r * 2} y1={170 + r * 4} x2={290 - r * 4} y2={170 + r * 4}
          stroke={ink} strokeWidth="0.4" opacity="0.5"/>
      ))}

      <Callout from={[170, 46]} to={[170, 14]}/>
      <PlateLabel x={170} y={12} num="i" text="display" anchor="middle"/>
      <Callout from={[170, 200]} to={[330, 222]}/>
      <PlateLabel x={332} y={224} num="ii" text="keyboard"/>
    </svg>
  );
}

// ── Earbuds (case) ───────────────────────────────────────────────
function VEarbuds({ palette = ['#f6f4ef','#dcd8d0','#1a1a1a','#a8a39a'], style }) {
  const [c0, c1, c2, c3] = palette;
  return (
    <svg viewBox="0 0 300 220" xmlns="http://www.w3.org/2000/svg" style={{ width: '100%', height: '100%', ...style }}>
      {/* case body */}
      <rect x="68" y="56" width="164" height="116" rx="22" fill={c0} opacity="0.7"/>
      {/* hinge line */}
      <line x1="68" y1="114" x2="232" y2="114" stroke={ink} strokeWidth="0.7" opacity="0.65"/>
      {/* earbud heads */}
      <ellipse cx="116" cy="92" rx="20" ry="22" fill={c0} opacity="0.85"/>
      <ellipse cx="184" cy="92" rx="20" ry="22" fill={c0} opacity="0.85"/>
      <circle cx="116" cy="86" r="7" fill={c1} opacity="0.7"/>
      <circle cx="184" cy="86" r="7" fill={c1} opacity="0.7"/>
      {/* stems */}
      <rect x="111" y="108" width="10" height="46" rx="3" fill={c0} opacity="0.85"/>
      <rect x="179" y="108" width="10" height="46" rx="3" fill={c0} opacity="0.85"/>
      {/* led */}
      <circle cx="150" cy="138" r="2.5" fill={c2} opacity="0.55"/>

      <g fill="none" stroke={ink} strokeWidth="0.8">
        <rect x="68" y="56" width="164" height="116" rx="22"/>
        <ellipse cx="116" cy="92" rx="20" ry="22"/>
        <ellipse cx="184" cy="92" rx="20" ry="22"/>
        <circle cx="116" cy="86" r="7"/>
        <circle cx="184" cy="86" r="7"/>
        <rect x="111" y="108" width="10" height="46" rx="3"/>
        <rect x="179" y="108" width="10" height="46" rx="3"/>
      </g>

      <Callout from={[150, 56]} to={[150, 18]}/>
      <PlateLabel x={150} y={16} num="i" text="case" anchor="middle"/>
      <Callout from={[116, 154]} to={[40, 198]}/>
      <PlateLabel x={38} y={202} text="stem" num="ii" anchor="end"/>
    </svg>
  );
}

// ── Tablet / e-reader ────────────────────────────────────────────
function VTablet({ palette = ['#5a5854','#3a3835','#d4cdb8','#a8a39a'], style }) {
  const [c0, c1, c2, c3] = palette;
  return (
    <svg viewBox="0 0 240 320" xmlns="http://www.w3.org/2000/svg" style={{ width: '100%', height: '100%', ...style }}>
      <rect x="40" y="28" width="160" height="264" rx="10" fill={c0} opacity="0.55"/>
      <rect x="50" y="40" width="140" height="226" fill={c2} opacity="0.55"/>

      {/* page text */}
      {Array.from({ length: 18 }).map((_, i) => (
        <rect key={i} x="62" y={56 + i * 12} width={i % 5 === 4 ? 80 : 116} height="2"
          fill={ink} opacity="0.4"/>
      ))}

      <g fill="none" stroke={ink} strokeWidth="0.9">
        <rect x="40" y="28" width="160" height="264" rx="10"/>
        <rect x="50" y="40" width="140" height="226"/>
        <circle cx="120" cy="280" r="6"/>
      </g>

      <Callout from={[200, 60]} to={[228, 30]}/>
      <PlateLabel x={228} y={32} num="i" text="screen"/>
      <Callout from={[120, 290]} to={[12, 310]}/>
      <PlateLabel x={10} y={313} text="home" num="ii" anchor="end"/>
    </svg>
  );
}

// ── Watch — face on ──────────────────────────────────────────────
function VWatch({ palette = ['#7a7165','#3a3530','#c4b89c','#d8d2c4'], style }) {
  const [c0, c1, c2, c3] = palette;
  return (
    <svg viewBox="0 0 240 320" xmlns="http://www.w3.org/2000/svg" style={{ width: '100%', height: '100%', ...style }}>
      {/* upper band */}
      <path d="M76 0 L164 0 L172 76 L68 76 Z" fill={c2} opacity="0.55"/>
      {/* lower band */}
      <path d="M68 244 L172 244 L164 320 L76 320 Z" fill={c2} opacity="0.55"/>
      {/* case */}
      <rect x="44" y="64" width="152" height="192" rx="34" fill={c0} opacity="0.6"/>
      <rect x="52" y="74" width="136" height="172" rx="26" fill={c1} opacity="0.7"/>
      {/* face */}
      <rect x="60" y="82" width="120" height="156" rx="20" fill="#0a0a0a"/>
      <text x="120" y="168" textAnchor="middle" fill={c3}
        style={{ font: "bold 36px ui-monospace, monospace" }}>9:30</text>
      <text x="120" y="194" textAnchor="middle" fill={c3} opacity="0.6"
        style={{ font: "11px ui-sans-serif, sans-serif" }}>MAY 6</text>

      <g fill="none" stroke={ink} strokeWidth="0.8">
        <path d="M76 0 L164 0 L172 76 L68 76 Z"/>
        <path d="M68 244 L172 244 L164 320 L76 320 Z"/>
        <rect x="44" y="64" width="152" height="192" rx="34"/>
        <rect x="60" y="82" width="120" height="156" rx="20"/>
        {/* crown */}
        <rect x="194" y="142" width="12" height="22" rx="2"/>
      </g>

      <Callout from={[120, 82]} to={[120, 14]}/>
      <PlateLabel x={120} y={12} num="i" text="display" anchor="middle"/>
      <Callout from={[206, 150]} to={[232, 132]}/>
      <PlateLabel x={234} y={134} num="ii" text="crown"/>
    </svg>
  );
}

// Map: pick the right vector for an item.
function ItemVector({ item, style }) {
  const p = item.palette;
  const id = item.id;
  if (item.category === 'badminton') {
    if (id.startsWith('shoes')) return <VShoe palette={p} style={style}/>;
    return <VRacket palette={p} style={style}/>;
  }
  if (item.category === 'photo') {
    if (id.startsWith('lens')) return <VLens palette={p} style={style}/>;
    if (id.startsWith('tripod')) return <VTripod palette={p} style={style}/>;
    return <VCamera palette={p} style={style}/>;
  }
  if (item.category === 'cars') return <VCar palette={p} style={style}/>;
  if (item.category === 'tech') {
    if (id.includes('mbp')) return <VLaptop palette={p} style={style}/>;
    if (id.includes('airpods')) return <VEarbuds palette={p} style={style}/>;
    if (id.includes('kindle') || id.includes('ipad')) return <VTablet palette={p} style={style}/>;
    if (id.includes('watch')) return <VWatch palette={p} style={style}/>;
    return <VLaptop palette={p} style={style}/>;
  }
  return null;
}

// Tiny category icon (line art) for tab bars and chips.
function CatIcon({ id, size = 18, color = 'currentColor' }) {
  const s = { width: size, height: size, fill: 'none', stroke: color, strokeWidth: 1.4, strokeLinecap: 'round', strokeLinejoin: 'round' };
  if (id === 'badminton') return (
    <svg viewBox="0 0 24 24" {...s}><ellipse cx="9" cy="9" rx="6" ry="6.5"/><line x1="13" y1="13" x2="20" y2="20"/></svg>
  );
  if (id === 'photo') return (
    <svg viewBox="0 0 24 24" {...s}><rect x="3" y="7" width="18" height="13" rx="2"/><circle cx="12" cy="13.5" r="3.5"/><path d="M9 7l1.5-2h3L15 7"/></svg>
  );
  if (id === 'cars') return (
    <svg viewBox="0 0 24 24" {...s}><path d="M3 14l1.5-5a2 2 0 012-1.5h11a2 2 0 012 1.5L21 14v4H3z"/><circle cx="7" cy="18" r="1.5"/><circle cx="17" cy="18" r="1.5"/></svg>
  );
  if (id === 'tech') return (
    <svg viewBox="0 0 24 24" {...s}><rect x="3" y="5" width="18" height="12" rx="1.5"/><path d="M2 19h20"/></svg>
  );
  return <svg viewBox="0 0 24 24" {...s}><circle cx="12" cy="12" r="8"/></svg>;
}

Object.assign(window, {
  PlateLabel, Callout,
  VRacket, VCamera, VLens, VTripod, VShoe, VCar, VLaptop, VEarbuds, VTablet, VWatch,
  ItemVector, CatIcon,
});
