import React from 'react';
import {AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Kinetic} from '../components/Kinetic';
import {Badge, Counter, Label, SceneArt, fmtBytes} from '../components/Ui';
import {C, MONO, SANS} from '../theme';

/** Swipe card: dragged up, released, tossed into Safe Trash; the freed counter jumps. */
const SwipePanel: React.FC<{readonly w: number}> = ({w}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const h = w * 1.25;
	const drag = interpolate(frame, [20, 55], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: (t) => t * t});
	const toss = spring({frame: frame - 55, fps, config: {damping: 18, stiffness: 70}});
	const y = -drag * h * 0.18 - toss * h * 3.2;
	const rot = drag * -4 + toss * -18;
	const next = spring({frame: frame - 62, fps, config: {damping: 15}});
	const bin = spring({frame: frame - 70, fps, config: {damping: 8, stiffness: 160}});
	const card = (z: number, kind: 'beach' | 'city', name: string, size: string, style: React.CSSProperties) => (
		<div
			style={{
				position: 'absolute',
				inset: 0,
				borderRadius: 34,
				overflow: 'hidden',
				border: `2px solid ${C.stroke}`,
				boxShadow: '0 30px 70px rgba(0,0,0,0.55)',
				backgroundColor: C.card,
				zIndex: z,
				...style,
			}}
		>
			<div style={{position: 'absolute', left: 0, right: 0, top: 0, height: '70%'}}>
				<SceneArt kind={kind} />
				<div style={{position: 'absolute', left: 20, top: 20}}>
					<Badge text={`#1 LARGEST · ${size}`} color={C.rose} size={w * 0.034} dot />
				</div>
				<div style={{position: 'absolute', left: '50%', top: '50%', width: w * 0.16, height: w * 0.16, marginLeft: -w * 0.08, marginTop: -w * 0.08, borderRadius: '50%', background: 'rgba(0,0,0,0.45)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: C.emeraldLight, fontSize: w * 0.07}}>▶</div>
			</div>
			<div style={{position: 'absolute', left: 26, right: 26, bottom: 24}}>
				<Label size={w * 0.05} weight={700}>{name}</Label>
				<Label size={w * 0.034} mono color={C.textDim} style={{marginTop: 8}}>4K 60fps · 04:18 · HEVC</Label>
			</div>
		</div>
	);
	return (
		<div style={{position: 'relative', width: w, height: h + w * 0.32}}>
			<div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20}}>
				<Badge text="SWIPE DECK" color={C.indigoLight} size={w * 0.036} />
				<div style={{display: 'flex', alignItems: 'baseline', gap: 10, scale: String(1 + bin * 0.12)}}>
					<Counter from={0} to={3.42} start={58} end={95} format={(v) => fmtBytes(v)} size={w * 0.07} color={C.emeraldLight} />
					<span style={{fontFamily: MONO, fontWeight: 700, fontSize: w * 0.04, color: C.emeraldLight}}>FREED</span>
				</div>
			</div>
			<div style={{position: 'relative', width: w, height: h}}>
				{card(1, 'city', 'VID_City_Night.mp4', '488 MB', {scale: String(0.92 + next * 0.08), translate: `0 ${(1 - next) * 30}px`})}
				{card(2, 'beach', 'VID_4K_Beach_Trip.mp4', '612 MB', {translate: `0 ${y}px`, rotate: `${rot}deg`, opacity: interpolate(toss, [0, 0.6], [1, 0], {extrapolateRight: 'clamp'})})}
				<div style={{position: 'absolute', left: 0, right: 0, bottom: -w * 0.2, display: 'flex', justifyContent: 'center', zIndex: 3}}>
					<div style={{display: 'flex', alignItems: 'center', gap: 14, padding: '14px 26px', borderRadius: 999, backgroundColor: `${C.red}26`, border: `1px solid ${C.red}66`, color: '#fff', fontFamily: SANS, fontWeight: 700, fontSize: w * 0.04, scale: String(0.9 + bin * 0.1)}}>
						🗑 Safe Trash · restorable 30 days
					</div>
				</div>
			</div>
		</div>
	);
};

/** Burst group: 4 near-identical shots get sharpness scores; the sharpest locks as KEEP. */
const BurstPanel: React.FC<{readonly w: number; readonly start: number}> = ({w, start}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const scores = [99.4, 71.2, 64.8, 52.3];
	const tile = (w - 24) / 2;
	const lock = spring({frame: frame - start - 50, fps, config: {damping: 10, stiffness: 150}});
	return (
		<div style={{width: w}}>
			<div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20}}>
				<Label size={w * 0.055} weight={800}>Burst Portrait Group</Label>
				<Badge text="dHASH MATCH" color={C.indigoLight} size={w * 0.032} />
			</div>
			<div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24}}>
				{scores.map((s, i) => {
					const p = spring({frame: frame - start - i * 6, fps, config: {damping: 14}});
					const keep = i === 0;
					const scan = interpolate(frame - start - 18 - i * 5, [0, 22], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
					return (
						<div
							key={s}
							style={{
								position: 'relative',
								height: tile * 0.82,
								borderRadius: 24,
								overflow: 'hidden',
								opacity: p,
								scale: String(interpolate(p, [0, 1], [0.8, 1]) * (keep ? 1 + lock * 0.04 : 1)),
								border: `3px solid ${keep && lock > 0.3 ? C.emeraldLight : C.stroke}`,
								boxShadow: keep ? `0 0 ${60 * lock}px ${C.emerald}aa` : undefined,
								filter: keep ? undefined : `blur(${(i * 1.2) * scan}px)`,
							}}
						>
							<SceneArt kind="portrait" hue={i * 4} />
							<div style={{position: 'absolute', left: 14, top: 14}}>
								<Badge text={keep ? (lock > 0.3 ? '🔒 KEEP' : 'SCORING') : 'DUPLICATE'} color={keep ? C.emeraldLight : C.red} size={w * 0.028} />
							</div>
							<div style={{position: 'absolute', left: 14, bottom: 12, fontFamily: MONO, fontWeight: 700, fontSize: w * 0.032, color: '#fff', textShadow: '0 2px 8px #000'}}>
								Sharpness {(s * scan).toFixed(1)}
							</div>
						</div>
					);
				})}
			</div>
			<div style={{marginTop: 22, opacity: lock, fontFamily: SANS, fontWeight: 700, fontSize: w * 0.045, color: C.emeraldLight}}>Sharpest shot kept · 3 copies flagged</div>
		</div>
	);
};

export const SwipeDedupScene: React.FC = () => {
	const {width, height} = useVideoConfig();
	const portrait = height > width;
	const panelW = portrait ? width * 0.44 : width * 0.2;
	return (
		<AbsoluteFill>
			<Backdrop seed="swipe" />
			<AbsoluteFill style={{display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: portrait ? 70 : 50, padding: portrait ? '140px 50px' : '60px 100px'}}>
				<Kinetic text="Swipe away *heavy* videos. Keep the *sharpest* shot." size={portrait ? 84 : 64} delay={2} stagger={3} maxWidth={portrait ? width * 0.9 : width * 0.9} />
				<div style={{display: 'flex', flexDirection: 'row', gap: portrait ? 40 : 120, alignItems: 'flex-start'}}>
					<SwipePanel w={panelW} />
					<BurstPanel w={portrait ? width * 0.44 : width * 0.34} start={90} />
				</div>
			</AbsoluteFill>
		</AbsoluteFill>
	);
};
