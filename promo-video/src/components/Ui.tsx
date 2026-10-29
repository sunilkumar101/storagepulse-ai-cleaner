import React from 'react';
import {interpolate, useCurrentFrame} from 'remotion';
import {C, MONO, SANS} from '../theme';

/** Uppercase mono pill, like the app's TelemetryBadge. */
export const Badge: React.FC<{readonly text: string; readonly color: string; readonly size?: number; readonly dot?: boolean; readonly style?: React.CSSProperties}> = ({
	text,
	color,
	size = 22,
	dot,
	style,
}) => (
	<div
		style={{
			display: 'inline-flex',
			alignItems: 'center',
			gap: size * 0.4,
			padding: `${size * 0.3}px ${size * 0.7}px`,
			borderRadius: 999,
			backgroundColor: `${color}22`,
			border: `1px solid ${color}55`,
			color,
			fontFamily: MONO,
			fontWeight: 700,
			fontSize: size,
			letterSpacing: '0.08em',
			whiteSpace: 'nowrap',
			...style,
		}}
	>
		{dot ? <span style={{width: size * 0.4, height: size * 0.4, borderRadius: '50%', backgroundColor: color, boxShadow: `0 0 ${size * 0.6}px ${color}`}} /> : null}
		{text}
	</div>
);

/** Finger tap: a dot plus two expanding rings starting at [at] (frames, local). */
export const Ripple: React.FC<{readonly x: number; readonly y: number; readonly at: number; readonly size?: number}> = ({x, y, at, size = 90}) => {
	const frame = useCurrentFrame();
	const f = frame - at;
	if (f < -12 || f > 40) return null;
	const press = interpolate(f, [-12, 0, 8], [0, 1, 0.7], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
	return (
		<div style={{position: 'absolute', left: x - size / 2, top: y - size / 2, width: size, height: size, pointerEvents: 'none'}}>
			{[0, 10].map((d) => {
				const p = interpolate(f - d, [0, 28], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
				return (
					<div
						key={d}
						style={{
							position: 'absolute',
							inset: 0,
							borderRadius: '50%',
							border: `4px solid ${C.text}`,
							scale: String(0.4 + p * 1.8),
							opacity: f - d < 0 ? 0 : 1 - p,
						}}
					/>
				);
			})}
			<div
				style={{
					position: 'absolute',
					inset: size * 0.3,
					borderRadius: '50%',
					backgroundColor: 'rgba(255,255,255,0.85)',
					opacity: press,
					scale: String(0.6 + press * 0.4),
					boxShadow: '0 0 30px rgba(255,255,255,0.6)',
				}}
			/>
		</div>
	);
};

export const fmtBytes = (gb: number): string => (gb >= 1 ? `${gb.toFixed(2)} GB` : `${Math.round(gb * 1000)} MB`);

/** Number that counts between two values over [from,to] frames. */
export const Counter: React.FC<{
	readonly from: number;
	readonly to: number;
	readonly start: number;
	readonly end: number;
	readonly format: (v: number) => string;
	readonly size: number;
	readonly color: string;
}> = ({from, to, start, end, format, size, color}) => {
	const frame = useCurrentFrame();
	const v = interpolate(frame, [start, end], [from, to], {
		extrapolateLeft: 'clamp',
		extrapolateRight: 'clamp',
		easing: (t) => 1 - Math.pow(1 - t, 3),
	});
	return <span style={{fontFamily: MONO, fontWeight: 700, fontSize: size, color, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap'}}>{format(v)}</span>;
};

export const Label: React.FC<{readonly children: React.ReactNode; readonly size: number; readonly color?: string; readonly weight?: number; readonly mono?: boolean; readonly style?: React.CSSProperties}> = ({
	children,
	size,
	color = C.text,
	weight = 600,
	mono,
	style,
}) => <div style={{fontFamily: mono ? MONO : SANS, fontSize: size, fontWeight: weight, color, lineHeight: 1.2, ...style}}>{children}</div>;

/** Original vector "photo" art (no stock images): sunset / beach / mountain scenes. */
export const SceneArt: React.FC<{readonly kind: 'beach' | 'mountain' | 'portrait' | 'city'; readonly hue?: number; readonly style?: React.CSSProperties}> = ({kind, hue = 0, style}) => {
	const base: React.CSSProperties = {position: 'absolute', inset: 0, overflow: 'hidden', ...style};
	if (kind === 'beach')
		return (
			<div style={{...base, background: 'linear-gradient(180deg,#ffb86b 0%,#ff7e6b 38%,#5b8bd9 39%,#2a5ca8 70%,#e9cf9c 71%,#d9b77a 100%)'}}>
				<div style={{position: 'absolute', left: '58%', top: '18%', width: '22%', aspectRatio: '1', borderRadius: '50%', background: '#fff3c4', boxShadow: '0 0 60px #ffe08a'}} />
				<div style={{position: 'absolute', left: '-10%', right: '-10%', top: '62%', height: '6%', background: 'rgba(255,255,255,0.35)', borderRadius: '50%'}} />
			</div>
		);
	if (kind === 'mountain')
		return (
			<div style={{...base, background: 'linear-gradient(180deg,#8ec5ff 0%,#cfe6ff 45%,#9fb7c9 100%)'}}>
				<svg viewBox="0 0 400 225" preserveAspectRatio="xMidYMid slice" style={{position: 'absolute', inset: 0, width: '100%', height: '100%'}}>
					<polygon points="0,170 70,80 120,130 190,40 260,120 310,70 400,150 400,225 0,225" fill="#4a5d73" />
					<polygon points="190,40 215,72 200,70 185,85 170,66" fill="#f4f8ff" />
					<polygon points="310,70 330,95 315,92 300,100 292,90" fill="#f4f8ff" />
					<polygon points="0,190 90,130 160,175 240,135 330,180 400,160 400,225 0,225" fill="#2f4a3a" />
					<rect x="0" y="200" width="400" height="25" fill="#3d6e8f" opacity="0.85" />
				</svg>
			</div>
		);
	if (kind === 'city')
		return (
			<div style={{...base, background: 'linear-gradient(180deg,#1b1f4b 0%,#6b3fa0 60%,#f47c7c 100%)'}}>
				{[8, 22, 35, 50, 63, 78].map((x, i) => (
					<div key={x} style={{position: 'absolute', left: `${x}%`, bottom: 0, width: '11%', height: `${35 + ((i * 17) % 40)}%`, background: '#141733', boxShadow: 'inset 0 0 0 2px #23285a'}} />
				))}
			</div>
		);
	// portrait: abstract silhouette on a warm backdrop, tinted by hue
	return (
		<div style={{...base, background: `linear-gradient(160deg, hsl(${25 + hue},70%,62%), hsl(${330 + hue},45%,35%))`}}>
			<div style={{position: 'absolute', left: '32%', top: '20%', width: '36%', aspectRatio: '1', borderRadius: '50%', background: `hsl(${20 + hue},35%,22%)`}} />
			<div style={{position: 'absolute', left: '18%', top: '55%', width: '64%', height: '60%', borderRadius: '45% 45% 0 0', background: `hsl(${20 + hue},35%,22%)`}} />
		</div>
	);
};
