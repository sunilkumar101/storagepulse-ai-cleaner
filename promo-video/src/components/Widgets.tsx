import React from 'react';
import {C, MONO, SANS} from '../theme';

// Visual twins of the 3 Glance widgets (demo numbers).
const card = (s: number): React.CSSProperties => ({
	backgroundColor: C.card,
	border: `1px solid ${C.stroke}`,
	borderRadius: 26 * s,
	padding: 22 * s,
	boxSizing: 'border-box',
	boxShadow: '0 24px 60px rgba(0,0,0,0.5)',
	fontFamily: SANS,
	color: C.text,
});
const cats: [string, string, number][] = [
	['Videos', C.rose, 0.2],
	['Photos', C.violet, 0.16],
	['WA', C.emerald, 0.14],
	['Apps', C.cyan, 0.13],
	['Sys', C.amber, 0.26],
];

export const BoostWidget: React.FC<{readonly s?: number; readonly width: number}> = ({s = 1, width}) => (
	<div style={{...card(s), width}}>
		<div style={{display: 'flex', alignItems: 'center', gap: 10 * s}}>
			<span style={{color: C.emeraldLight, fontSize: 26 * s}}>〜</span>
			<span style={{fontWeight: 700, fontSize: 26 * s, flex: 1}}>StoragePulse</span>
			<span style={{fontFamily: MONO, fontWeight: 700, fontSize: 18 * s, color: C.amberLight, background: C.raised, padding: `${5 * s}px ${12 * s}px`, borderRadius: 99}}>114.2/128 GB · 89%</span>
		</div>
		<div style={{display: 'flex', height: 12 * s, borderRadius: 99, overflow: 'hidden', background: C.stroke, margin: `${18 * s}px 0 ${10 * s}px`}}>
			{cats.map(([n, c, f]) => (
				<div key={n} style={{width: `${f * 100}%`, background: c}} />
			))}
		</div>
		<div style={{display: 'flex', justifyContent: 'space-between', fontFamily: MONO, fontSize: 15 * s, color: C.textDim}}>
			{cats.map(([n, c]) => (
				<span key={n}>
					<span style={{color: c}}>● </span>
					{n}
				</span>
			))}
		</div>
		<div style={{display: 'flex', alignItems: 'center', marginTop: 18 * s}}>
			<div style={{flex: 1}}>
				<div style={{fontFamily: MONO, fontSize: 14 * s, color: C.muted}}>RECOVERABLE CACHE</div>
				<div style={{fontFamily: MONO, fontWeight: 700, fontSize: 24 * s, color: C.emeraldLight}}>6.42 GB Ready</div>
			</div>
			<div style={{background: C.emeraldLight, color: C.onEmerald, fontWeight: 800, fontSize: 20 * s, padding: `${12 * s}px ${20 * s}px`, borderRadius: 16 * s}}>⚡ Boost 6.4 GB</div>
		</div>
	</div>
);

export const ClutterWidget: React.FC<{readonly s?: number; readonly width: number}> = ({s = 1, width}) => (
	<div style={{...card(s), width}}>
		<div style={{fontFamily: MONO, fontSize: 14 * s, color: C.muted}}>DAILY CLUTTER</div>
		<div style={{fontFamily: MONO, fontWeight: 700, fontSize: 38 * s}}>380 MB</div>
		{['12 Screenshots', '3 Heavy Videos'].map((t) => (
			<div key={t} style={{marginTop: 10 * s, background: C.raised, borderRadius: 10 * s, padding: `${7 * s}px ${12 * s}px`, fontFamily: MONO, fontSize: 16 * s, color: C.textDim}}>
				<span style={{color: C.amberLight}}>● </span>
				{t}
			</div>
		))}
		<div style={{marginTop: 16 * s, background: C.indigo, borderRadius: 14 * s, padding: `${12 * s}px ${16 * s}px`, fontWeight: 700, fontSize: 19 * s, display: 'flex', justifyContent: 'space-between'}}>
			<span>Swipe to Purge</span>
			<span>→</span>
		</div>
	</div>
);

export const StripWidget: React.FC<{readonly s?: number; readonly width: number}> = ({s = 1, width}) => (
	<div style={{...card(s), width, display: 'flex', alignItems: 'center', gap: 18 * s, padding: `${16 * s}px ${22 * s}px`}}>
		{[
			['FREE SPACE', '13.8 GB', C.emeraldLight],
			['SAFE TRASH', '1.2 GB', C.amberLight],
		].map(([l, v, c]) => (
			<div key={l} style={{flex: 1, display: 'flex', alignItems: 'center', gap: 10 * s}}>
				<span style={{color: c, fontSize: 14 * s}}>●</span>
				<div>
					<div style={{fontFamily: MONO, fontSize: 13 * s, color: C.muted}}>{l}</div>
					<div style={{fontFamily: MONO, fontWeight: 700, fontSize: 22 * s}}>{v}</div>
				</div>
			</div>
		))}
		<div style={{background: C.raised, color: C.emeraldLight, fontWeight: 700, fontSize: 18 * s, padding: `${10 * s}px ${16 * s}px`, borderRadius: 14 * s}}>↻ Rescan</div>
	</div>
);
