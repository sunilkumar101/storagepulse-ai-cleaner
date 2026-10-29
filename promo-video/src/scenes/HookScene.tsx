import React from 'react';
import {AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Kinetic} from '../components/Kinetic';
import {Treemap} from '../components/Treemap';
import {Badge} from '../components/Ui';
import {C} from '../theme';

/** 3D obsidian storage cube spinning in, then unfolding into the proportional treemap. */
const Cube: React.FC<{readonly size: number; readonly morph: number}> = ({size, morph}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const intro = spring({frame, fps, config: {damping: 16, stiffness: 90}});
	const half = size / 2;
	const faces = [
		{t: `translateZ(${half}px)`},
		{t: `rotateY(180deg) translateZ(${half}px)`},
		{t: `rotateY(90deg) translateZ(${half}px)`},
		{t: `rotateY(-90deg) translateZ(${half}px)`},
		{t: `rotateX(90deg) translateZ(${half}px)`},
		{t: `rotateX(-90deg) translateZ(${half}px)`},
	];
	// Faces fly apart as the cube "unfolds" into the map.
	const explode = morph * size * 0.9;
	return (
		<div style={{width: size, height: size, perspective: size * 4, opacity: 1 - interpolate(morph, [0.5, 1], [0, 1], {extrapolateLeft: 'clamp'})}}>
			<div
				style={{
					width: size,
					height: size,
					position: 'relative',
					transformStyle: 'preserve-3d',
					transform: `rotateX(${-22 + frame * 0.6}deg) rotateY(${30 + frame * 1.6 + morph * 120}deg) scale(${interpolate(intro, [0, 1], [0.8, 1])})`,
				}}
			>
				{faces.map((f, i) => (
					<div
						key={i}
						style={{
							position: 'absolute',
							inset: 0,
							transform: `${f.t} translateZ(${explode}px)`,
							background: `linear-gradient(135deg, ${C.card}ee, #0b0f19ee)`,
							border: `3px solid ${i % 2 ? C.indigo : C.emerald}`,
							boxShadow: `inset 0 0 ${size * 0.25}px ${i % 2 ? C.indigo : C.emerald}66, 0 0 ${size * 0.15}px ${C.emerald}55`,
							borderRadius: size * 0.06,
							display: 'grid',
							gridTemplateColumns: '3fr 2fr',
							gridTemplateRows: '3fr 2fr',
							gap: size * 0.04,
							padding: size * 0.1,
							boxSizing: 'border-box',
						}}
					>
						{[C.violet, C.emerald, C.rose, C.cyan].map((c) => (
							<div key={c} style={{borderRadius: size * 0.03, backgroundColor: `${c}55`, border: `2px solid ${c}`}} />
						))}
					</div>
				))}
			</div>
		</div>
	);
};

export const HookScene: React.FC = () => {
	const frame = useCurrentFrame();
	const {width, height, fps} = useVideoConfig();
	const portrait = height > width;
	const morph = interpolate(frame, [70, 120], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: (t) => t * t * (3 - 2 * t)});
	const mapW = portrait ? width * 0.84 : width * 0.44;
	const mapH = portrait ? height * 0.42 : height * 0.62;
	const cube = portrait ? width * 0.5 : height * 0.46;
	const head = portrait ? 104 : 92;
	const sub = interpolate(spring({frame: frame - 95, fps, config: {damping: 200}}), [0, 1], [0, 1]);

	const text = (
		<div style={{display: 'flex', flexDirection: 'column', gap: 28, alignItems: portrait ? 'center' : 'flex-start'}}>
			<Badge text="STORAGEPULSE AI" color={C.emeraldLight} dot size={portrait ? 30 : 24} />
			{/* negative delay: the hook headline is already readable on frame 0 (thumbnail) */}
			<Kinetic text="Phone storage *full?*" size={head} align={portrait ? 'center' : 'left'} delay={-20} />
			<div style={{opacity: sub, translate: `0 ${(1 - sub) * 30}px`}}>
				<Kinetic text="See where every *GB* went — in seconds." size={head * 0.62} align={portrait ? 'center' : 'left'} delay={95} stagger={3} maxWidth={portrait ? width * 0.86 : width * 0.42} />
			</div>
		</div>
	);

	const visual = (
		<div style={{position: 'relative', width: mapW, height: mapH, display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
			<div style={{position: 'absolute'}}>
				<Cube size={cube} morph={morph} />
			</div>
			<div style={{position: 'absolute', inset: 0, opacity: interpolate(morph, [0.35, 0.8], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})}}>
				<Treemap width={mapW} height={mapH} start={88} />
			</div>
		</div>
	);

	return (
		<AbsoluteFill>
			<Backdrop seed="hook" />
			<AbsoluteFill
				style={{
					display: 'flex',
					flexDirection: portrait ? 'column' : 'row',
					alignItems: 'center',
					justifyContent: 'center',
					gap: portrait ? 90 : 110,
					padding: portrait ? '160px 80px' : '0 120px',
				}}
			>
				{text}
				{visual}
			</AbsoluteFill>
		</AbsoluteFill>
	);
};
