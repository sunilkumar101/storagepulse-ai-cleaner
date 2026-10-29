import React from 'react';
import {AbsoluteFill, interpolate, random, useCurrentFrame, useVideoConfig} from 'remotion';
import {C} from '../theme';

/**
 * Obsidian canvas with a slow-drifting telemetry grid and glowing emerald / indigo particles.
 * Everything is a pure function of the frame (deterministic random seeds).
 */
export const Backdrop: React.FC<{readonly particles?: number; readonly seed?: string}> = ({particles = 46, seed = 'sp'}) => {
	const frame = useCurrentFrame();
	const {width, height, fps} = useVideoConfig();
	const t = frame / fps;
	return (
		<AbsoluteFill style={{backgroundColor: C.canvas, overflow: 'hidden'}}>
			{/* soft colour fields */}
			<AbsoluteFill
				style={{
					background: `radial-gradient(60% 50% at ${30 + Math.sin(t * 0.4) * 8}% 20%, rgba(16,185,129,0.16), transparent 70%),
						radial-gradient(55% 45% at ${75 + Math.cos(t * 0.3) * 8}% 85%, rgba(99,102,241,0.20), transparent 70%)`,
				}}
			/>
			{/* telemetry grid */}
			<AbsoluteFill
				style={{
					opacity: 0.18,
					backgroundImage: `linear-gradient(${C.stroke} 1px, transparent 1px), linear-gradient(90deg, ${C.stroke} 1px, transparent 1px)`,
					backgroundSize: '64px 64px',
					backgroundPosition: `${(t * 12) % 64}px ${(t * 20) % 64}px`,
					maskImage: 'radial-gradient(ellipse at center, black 30%, transparent 80%)',
				}}
			/>
			{new Array(particles).fill(0).map((_, i) => {
				const x = random(`${seed}x${i}`) * width;
				const speed = 20 + random(`${seed}s${i}`) * 50;
				const y = (random(`${seed}y${i}`) * (height + 200) - t * speed) % (height + 200);
				const yy = y < -100 ? y + height + 200 : y;
				const size = 2 + random(`${seed}r${i}`) * 5;
				const color = random(`${seed}c${i}`) > 0.45 ? C.emerald : C.indigo;
				const twinkle = interpolate(Math.sin(t * 2 + i), [-1, 1], [0.25, 0.95]);
				return (
					<div
						key={i}
						style={{
							position: 'absolute',
							left: x,
							top: yy,
							width: size,
							height: size,
							borderRadius: '50%',
							backgroundColor: color,
							opacity: twinkle,
							boxShadow: `0 0 ${size * 4}px ${size}px ${color}66`,
						}}
					/>
				);
			})}
		</AbsoluteFill>
	);
};
