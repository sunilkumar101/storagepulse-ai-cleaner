import React from 'react';
import {Img, staticFile} from 'remotion';
import {C} from '../theme';

/**
 * 3D phone mockup: obsidian bezel, emerald rim light, glass glare. Pass a screenshot in
 * `screen` (public/ path) or custom content as children.
 */
export const Phone: React.FC<{
	readonly width: number;
	readonly screen?: string;
	readonly rotateY?: number;
	readonly rotateX?: number;
	readonly glow?: string;
	readonly children?: React.ReactNode;
	readonly style?: React.CSSProperties;
}> = ({width, screen, rotateY = 0, rotateX = 0, glow = C.emerald, children, style}) => {
	const bezel = width * 0.03;
	// Inner screen keeps the phone screenshots' exact 720×1600 aspect (no crop).
	const height = (width - 2 * bezel) * (1600 / 720) + 2 * bezel;
	return (
		<div style={{perspective: width * 4, ...style}}>
			<div
				style={{
					width,
					height,
					borderRadius: width * 0.12,
					padding: bezel,
					boxSizing: 'border-box',
					background: 'linear-gradient(145deg, #2a3242 0%, #0b0f19 45%, #1a2130 100%)',
					boxShadow: `0 ${width * 0.08}px ${width * 0.25}px rgba(0,0,0,0.65), 0 0 ${width * 0.18}px ${glow}55, inset 0 0 0 2px #3a4456`,
					rotate: `y ${rotateY}deg`,
					transform: `rotateX(${rotateX}deg)`,
					transformStyle: 'preserve-3d',
					position: 'relative',
				}}
			>
				<div
					style={{
						width: '100%',
						height: '100%',
						borderRadius: width * 0.095,
						overflow: 'hidden',
						backgroundColor: C.canvas,
						position: 'relative',
					}}
				>
					{screen ? <Img src={staticFile(screen)} style={{width: '100%', height: '100%', objectFit: 'cover', objectPosition: 'top'}} /> : null}
					{/* children draw on top of the screenshot (demo-content overlays) or replace it */}
					{children ? <div style={{position: 'absolute', inset: 0}}>{children}</div> : null}
					{/* glass glare */}
					<div
						style={{
							position: 'absolute',
							inset: 0,
							background: 'linear-gradient(115deg, rgba(255,255,255,0.10) 0%, rgba(255,255,255,0.02) 32%, transparent 45%)',
							pointerEvents: 'none',
						}}
					/>
				</div>
				{/* side button */}
				<div style={{position: 'absolute', right: -width * 0.012, top: height * 0.22, width: width * 0.012, height: height * 0.09, borderRadius: 4, background: '#2a3242'}} />
			</div>
		</div>
	);
};
