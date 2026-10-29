import React from 'react';
import {interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {C, SANS, SPRING_UI} from '../theme';

/**
 * Kinetic headline: words rise in on staggered springs. Words wrapped in *asterisks* get the
 * emerald gradient accent.
 */
export const Kinetic: React.FC<{
	readonly text: string;
	readonly size: number;
	readonly delay?: number;
	readonly stagger?: number;
	readonly align?: 'left' | 'center';
	readonly maxWidth?: number;
	readonly accent?: string;
}> = ({text, size, delay = 0, stagger = 4, align = 'center', maxWidth, accent = C.emeraldLight}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const words = text.split(' ');
	return (
		<div
			style={{
				fontFamily: SANS,
				fontWeight: 800,
				fontSize: size,
				lineHeight: 1.08,
				letterSpacing: '-0.02em',
				color: C.text,
				textAlign: align,
				maxWidth,
				display: 'flex',
				flexWrap: 'wrap',
				justifyContent: align === 'center' ? 'center' : 'flex-start',
				columnGap: size * 0.26,
			}}
		>
			{words.map((w, i) => {
				const hot = w.startsWith('*');
				const clean = w.replace(/\*/g, '');
				const p = spring({frame: frame - delay - i * stagger, fps, config: SPRING_UI});
				return (
					<span
						key={i}
						style={{
							display: 'inline-block',
							opacity: interpolate(p, [0, 0.4], [0, 1], {extrapolateRight: 'clamp'}),
							translate: `0 ${interpolate(p, [0, 1], [size * 0.6, 0])}px`,
							scale: String(interpolate(p, [0, 1], [0.92, 1])),
							filter: `blur(${interpolate(p, [0, 1], [8, 0])}px)`,
							...(hot
								? {
										backgroundImage: `linear-gradient(90deg, ${accent}, ${C.indigoLight})`,
										WebkitBackgroundClip: 'text',
										color: 'transparent',
									}
								: {}),
						}}
					>
						{clean}
					</span>
				);
			})}
		</div>
	);
};
