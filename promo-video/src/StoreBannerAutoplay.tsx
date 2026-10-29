import React from 'react';
import {springTiming, TransitionSeries} from '@remotion/transitions';
import {slide} from '@remotion/transitions/slide';
import {useVideoConfig} from 'remotion';
import {HookScene} from './scenes/HookScene';
import {TreemapScene} from './scenes/TreemapScene';
import {SwipeDedupScene} from './scenes/SwipeDedupScene';
import {CompressorScene} from './scenes/CompressorScene';
import {OutroScene} from './scenes/OutroScene';

/**
 * 18 s store autoplay / promo. Scene lengths include the 20-frame (1/3 s) transition overlaps,
 * so cuts land on 3 s, 7 s, 11 s and 15 s at 60 fps:
 * 200 + 260 + 260 + 260 + 180 − 4 × 20 = 1080 frames.
 */
export const StoreBannerAutoplay: React.FC = () => {
	const {fps} = useVideoConfig();
	const t = Math.round(fps / 3);
	return (
		<TransitionSeries>
			<TransitionSeries.Sequence name="Hero hook" durationInFrames={Math.round(3.333 * fps)}>
				<HookScene />
			</TransitionSeries.Sequence>
			<TransitionSeries.Transition presentation={slide({direction: 'from-right'})} timing={springTiming({config: {damping: 200}, durationInFrames: t})} />
			<TransitionSeries.Sequence name="Treemap zoom" durationInFrames={Math.round(4.333 * fps)}>
				<TreemapScene />
			</TransitionSeries.Sequence>
			<TransitionSeries.Transition presentation={slide({direction: 'from-right'})} timing={springTiming({config: {damping: 200}, durationInFrames: t})} />
			<TransitionSeries.Sequence name="Swipe + AI dedup" durationInFrames={Math.round(4.333 * fps)}>
				<SwipeDedupScene />
			</TransitionSeries.Sequence>
			<TransitionSeries.Transition presentation={slide({direction: 'from-bottom'})} timing={springTiming({config: {damping: 200}, durationInFrames: t})} />
			<TransitionSeries.Sequence name="H.265 compressor" durationInFrames={Math.round(4.333 * fps)}>
				<CompressorScene />
			</TransitionSeries.Sequence>
			<TransitionSeries.Transition presentation={slide({direction: 'from-right'})} timing={springTiming({config: {damping: 200}, durationInFrames: t})} />
			<TransitionSeries.Sequence name="Widgets + outro" durationInFrames={3 * fps}>
				<OutroScene />
			</TransitionSeries.Sequence>
		</TransitionSeries>
	);
};
