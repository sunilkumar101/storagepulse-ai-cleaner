import React from 'react';
import {interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {C, MONO, SANS} from '../theme';

type Block = {name: string; size: string; x: number; y: number; w: number; h: number; color: string; children?: Block[]; id?: string};

// Demo storage map (illustrative numbers), laid out as a squarified treemap in a 100×100 box.
export const BLOCKS: Block[] = [
	{
		name: 'DCIM / Camera',
		size: '42.1 GB',
		x: 0,
		y: 0,
		w: 56,
		h: 58,
		color: C.violet,
		children: [
			{name: 'Camera', size: '34.2 GB', x: 2, y: 12, w: 40, h: 44, color: C.violet},
			{name: 'Bursts', size: '7.9 GB', x: 43, y: 12, w: 11, h: 44, color: C.violet},
		],
	},
	{
		name: 'WhatsApp',
		size: '18.6 GB',
		x: 56,
		y: 0,
		w: 44,
		h: 42,
		color: C.emerald,
		id: 'wa',
		children: [
			{name: 'WA Video', size: '12.1 GB', x: 58, y: 12, w: 21, h: 28, color: C.emerald},
			{name: 'Voice Notes', size: '3.8 GB', x: 80, y: 12, w: 18, h: 13, color: C.emerald},
			{name: 'Sent', size: '2.74 GB', x: 80, y: 26, w: 18, h: 14, color: C.emeraldLight, id: 'sent'},
		],
	},
	{name: 'Movies & 4K', size: '24.6 GB', x: 0, y: 58, w: 56, h: 42, color: C.rose},
	{name: 'Download', size: '11.4 GB', x: 56, y: 42, w: 44, h: 28, color: C.cyan},
	{name: 'Telegram', size: '9.1 GB', x: 56, y: 70, w: 26, h: 30, color: C.amber},
	{name: 'System', size: '6.2 GB', x: 82, y: 70, w: 18, h: 30, color: '#64748B'},
];

/**
 * Proportional treemap. [start] = frame the blocks start popping in (staggered springs).
 * [purge] 0..1 empties the WhatsApp "Sent" block. [highlight] 0..1 glows it.
 */
export const Treemap: React.FC<{
	readonly width: number;
	readonly height: number;
	readonly start?: number;
	readonly purge?: number;
	readonly highlight?: number;
}> = ({width, height, start = 0, purge = 0, highlight = 0}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const font = Math.max(9, width * 0.026);
	const all = BLOCKS.flatMap((b, i) => [{b, i, depth: 0}, ...(b.children ?? []).map((c, j) => ({b: c, i: i + j * 0.5 + 0.5, depth: 1}))]);
	return (
		<div style={{position: 'relative', width, height}}>
			{all.map(({b, i, depth}) => {
				const p = spring({frame: frame - start - i * 5 - depth * 6, fps, config: {damping: 13, stiffness: 120, mass: 0.7}});
				const isSent = b.id === 'sent';
				const gone = isSent ? purge : 0;
				const glow = isSent ? highlight : 0;
				const pad = depth === 0 ? 4 : 3;
				return (
					<div
						key={`${b.name}-${depth}`}
						style={{
							position: 'absolute',
							left: (b.x / 100) * width + pad,
							top: (b.y / 100) * height + pad,
							width: (b.w / 100) * width - pad * 2,
							height: (b.h / 100) * height - pad * 2,
							borderRadius: depth === 0 ? 18 : 12,
							backgroundColor: depth === 0 ? `${b.color}24` : `${b.color}${isSent ? '55' : '33'}`,
							border: `2px solid ${b.color}${depth === 0 ? '88' : 'aa'}`,
							boxShadow: glow > 0 ? `0 0 ${40 * glow}px ${b.color}, inset 0 0 ${30 * glow}px ${b.color}88` : undefined,
							opacity: interpolate(p, [0, 0.5], [0, 1], {extrapolateRight: 'clamp'}) * (1 - gone * 0.85),
							scale: String(interpolate(p, [0, 1], [0.6, 1]) * (1 - gone * 0.25)),
							padding: font * 0.5,
							boxSizing: 'border-box',
							overflow: 'hidden',
						}}
					>
						<div style={{fontFamily: SANS, fontWeight: 700, fontSize: depth === 0 ? font : font * 0.85, color: C.text, whiteSpace: 'nowrap'}}>{b.name}</div>
						<div style={{fontFamily: MONO, fontWeight: 700, fontSize: (depth === 0 ? font : font * 0.85) * 0.9, color: b.color, whiteSpace: 'nowrap', marginTop: 2}}>
							{isSent && gone > 0.5 ? '0 B' : b.size}
						</div>
					</div>
				);
			})}
		</div>
	);
};
