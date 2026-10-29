import React from 'react';
import {AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Kinetic} from '../components/Kinetic';
import {Badge, Counter, Label, SceneArt, fmtBytes} from '../components/Ui';
import {C, MONO} from '../theme';

/** Before/after wipe across a 4K mountain shot while 1.84 GB shrinks to 294 MB. */
export const CompressorScene: React.FC = () => {
	const frame = useCurrentFrame();
	const {width, height, fps} = useVideoConfig();
	const portrait = height > width;
	const fw = portrait ? width * 0.86 : width * 0.5;
	const fh = fw * 0.5625;
	// Wipe sweeps right→left→center, like a finger on the compare slider.
	const wipe = interpolate(frame, [20, 80, 130, 175], [0.85, 0.15, 0.6, 0.5], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: (t) => t * t * (3 - 2 * t)});
	const enter = spring({frame, fps, config: {damping: 16}});
	const delta = spring({frame: frame - 120, fps, config: {damping: 9, stiffness: 140}});
	const fs = portrait ? 34 : 28;
	return (
		<AbsoluteFill>
			<Backdrop seed="cx" />
			<AbsoluteFill style={{display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: portrait ? 70 : 46, padding: portrait ? '140px 60px' : '50px 100px'}}>
				<Kinetic text="Shrink 4K videos *−84%*" size={portrait ? 104 : 88} delay={2} />
				<div
					style={{
						position: 'relative',
						width: fw,
						height: fh,
						borderRadius: 30,
						overflow: 'hidden',
						border: `2px solid ${C.stroke}`,
						boxShadow: `0 40px 100px rgba(0,0,0,0.6), 0 0 80px ${C.indigo}44`,
						scale: String(interpolate(enter, [0, 1], [0.85, 1])),
						opacity: enter,
						rotate: `x ${interpolate(enter, [0, 1], [18, 0])}deg`,
					}}
				>
					{/* "original": slightly over-saturated; "H.265": identical framing */}
					<SceneArt kind="mountain" />
					<div style={{position: 'absolute', inset: 0, clipPath: `inset(0 0 0 ${wipe * 100}%)`}}>
						<SceneArt kind="mountain" style={{filter: 'saturate(0.96) contrast(0.98)'}} />
					</div>
					<div style={{position: 'absolute', top: 0, bottom: 0, left: `${wipe * 100}%`, width: 5, marginLeft: -2.5, background: C.emeraldLight, boxShadow: `0 0 30px ${C.emerald}`}}>
						<div style={{position: 'absolute', top: '50%', left: '50%', width: 64, height: 64, marginLeft: -32, marginTop: -32, borderRadius: '50%', background: C.canvas, border: `4px solid ${C.emeraldLight}`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: C.emeraldLight, fontFamily: MONO, fontWeight: 700, fontSize: 26}}>⇆</div>
					</div>
					<div style={{position: 'absolute', left: 22, top: 22}}>
						<Badge text="ORIGINAL · 4K" color={C.text} size={fs * 0.65} style={{backgroundColor: 'rgba(9,13,22,0.85)'}} />
					</div>
					<div style={{position: 'absolute', right: 22, top: 22}}>
						<Badge text="H.265 · 4K" color={C.emeraldLight} size={fs * 0.65} style={{backgroundColor: 'rgba(9,13,22,0.85)'}} />
					</div>
				</div>
				<div style={{display: 'flex', alignItems: 'center', gap: portrait ? 34 : 60}}>
					<div>
						<Label size={fs * 0.7} mono color={C.muted}>ORIGINAL</Label>
						<Label size={fs * 1.9} mono weight={700} color={C.rose}>1.84 GB</Label>
					</div>
					<div style={{scale: String(0.6 + delta * 0.4), opacity: delta}}>
						<Badge text="↓ −84%" color={C.indigoLight} size={fs * 1.1} />
					</div>
					<div>
						<Label size={fs * 0.7} mono color={C.muted}>COMPRESSED</Label>
						<Counter from={1.84} to={0.294} start={60} end={140} format={fmtBytes} size={fs * 1.9} color={C.emeraldLight} />
					</div>
				</div>
				<div style={{fontFamily: MONO, fontSize: fs * 0.85, color: C.textDim, opacity: delta}}>Visually lossless H.265 · 100% on-device</div>
			</AbsoluteFill>
		</AbsoluteFill>
	);
};
