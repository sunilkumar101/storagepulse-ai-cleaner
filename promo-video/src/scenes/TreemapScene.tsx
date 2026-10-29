import React from 'react';
import {AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Kinetic} from '../components/Kinetic';
import {Treemap} from '../components/Treemap';
import {Badge, Counter, Label, Ripple} from '../components/Ui';
import {C, MONO, SANS} from '../theme';

export const TreemapScene: React.FC = () => {
	const frame = useCurrentFrame();
	const {width, height, fps} = useVideoConfig();
	const portrait = height > width;

	const mapW = portrait ? width * 0.8 : width * 0.4;
	const mapH = portrait ? height * 0.36 : height * 0.6;

	// Camera: tilt in 3D, then push toward the WhatsApp block (top-right).
	const cam = interpolate(frame, [10, 120], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: (t) => 1 - Math.pow(1 - t, 3)});
	// Gentle centred push-in + tilt: the map stays inside its own column and the safe area.
	const zoom = 1 + cam * 0.1;
	const panX = 0;
	const panY = 0;

	const TAP = 150;
	const purge = interpolate(frame, [TAP + 6, TAP + 40], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
	const highlight = interpolate(frame, [100, 125], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}) * (1 - purge);
	const card = spring({frame: frame - 95, fps, config: {damping: 15, stiffness: 120}});
	const done = spring({frame: frame - TAP - 10, fps, config: {damping: 14}});

	const cardW = portrait ? width * 0.86 : width * 0.36;
	const btnH = portrait ? 120 : 92;
	const fs = portrait ? 34 : 28;

	const inspector = (
		<div
			style={{
				width: cardW,
				borderRadius: 32,
				backgroundColor: `${C.card}f2`,
				border: `1px solid ${C.stroke}`,
				boxShadow: `0 30px 80px rgba(0,0,0,0.5), 0 0 60px ${C.emerald}33`,
				padding: 34,
				boxSizing: 'border-box',
				display: 'flex',
				flexDirection: 'column',
				gap: 22,
				opacity: card,
				translate: `0 ${(1 - card) * 80}px`,
				position: 'relative',
			}}
		>
			<div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12}}>
				<Label size={fs} mono color={C.textDim}>…/WhatsApp/Media/Sent</Label>
				<Badge text="SAFE TO PURGE" color={C.emeraldLight} size={fs * 0.62} />
			</div>
			<div style={{display: 'flex', gap: 40}}>
				<div>
					<Label size={fs * 0.62} mono color={C.muted}>WASTED VOLUME</Label>
					<Counter from={2.74} to={0} start={TAP + 6} end={TAP + 40} format={(v) => `${v.toFixed(2)} GB`} size={fs * 1.7} color={C.emeraldLight} />
				</div>
				<div>
					<Label size={fs * 0.62} mono color={C.muted}>DUPLICATE SENDS</Label>
					<Label size={fs * 1.2} weight={700}>318 videos</Label>
				</div>
			</div>
			<div
				style={{
					height: btnH,
					borderRadius: 22,
					display: 'flex',
					alignItems: 'center',
					justifyContent: 'center',
					fontFamily: SANS,
					fontWeight: 800,
					fontSize: fs * 1.05,
					color: done > 0.5 ? C.onEmerald : '#fff',
					backgroundColor: done > 0.5 ? C.emeraldLight : C.red,
					scale: String(interpolate(frame, [TAP - 4, TAP, TAP + 8], [1, 0.95, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})),
					boxShadow: `0 0 40px ${done > 0.5 ? C.emerald : C.red}66`,
					position: 'relative',
				}}
			>
				{done > 0.5 ? '✓ Moved to 30-day Safe Trash' : 'Purge 2.74 GB Sent Duplicates'}
				<Ripple x={(cardW - 68) / 2} y={btnH / 2} at={TAP} size={portrait ? 130 : 110} />
			</div>
		</div>
	);

	return (
		<AbsoluteFill>
			<Backdrop seed="map" />
			<AbsoluteFill
				style={{
					display: 'flex',
					flexDirection: portrait ? 'column' : 'row',
					alignItems: 'center',
					justifyContent: 'center',
					gap: portrait ? 60 : 80,
					padding: portrait ? '150px 70px' : '0 100px',
				}}
			>
				{portrait ? <Kinetic text="Tap. *Purge.* Done." size={100} delay={4} /> : null}
				<div style={{width: mapW, height: mapH, perspective: 1800, overflow: 'visible'}}>
					<div
						style={{
							width: mapW,
							height: mapH,
							transformStyle: 'preserve-3d',
							// Positive rotateY: the left edge recedes, so the tilted map never leaves the frame.
							transform: `rotateX(${16 - cam * 8}deg) rotateY(${6 - cam * 3}deg) translate(${panX}px, ${panY}px) scale(${zoom * 0.94})`,
							transformOrigin: '50% 50%',
						}}
					>
						<Treemap width={mapW} height={mapH} start={-60} purge={purge} highlight={highlight} />
					</div>
				</div>
				<div style={{display: 'flex', flexDirection: 'column', gap: 36, alignItems: portrait ? 'center' : 'flex-start'}}>
					{portrait ? null : <Kinetic text="Tap. *Purge.* Done." size={88} delay={4} align="left" />}
					{inspector}
					<div style={{opacity: done, fontFamily: MONO, fontSize: fs * 0.8, color: C.textDim}}>Restorable for 30 days · nothing deleted instantly</div>
				</div>
			</AbsoluteFill>
		</AbsoluteFill>
	);
};
