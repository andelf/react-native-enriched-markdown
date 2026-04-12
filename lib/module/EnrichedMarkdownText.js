"use strict";

import { useMemo, useCallback, useRef } from 'react';
import EnrichedMarkdownTextNativeComponent from './EnrichedMarkdownTextNativeComponent';
import EnrichedMarkdownNativeComponent from './EnrichedMarkdownNativeComponent';
import { normalizeMarkdownStyle } from "./normalizeMarkdownStyle.js";

/**
 * MD4C parser flags configuration.
 * Controls how the markdown parser interprets certain syntax.
 */
import { jsx as _jsx } from "react/jsx-runtime";
const defaultMd4cFlags = {
  underline: false,
  latexMath: true
};
export const EnrichedMarkdownText = ({
  markdown,
  markdownStyle = {},
  containerStyle,
  onLinkPress,
  onLinkLongPress,
  onTaskListItemPress,
  enableLinkPreview,
  selectable = true,
  md4cFlags = defaultMd4cFlags,
  allowFontScaling = true,
  maxFontSizeMultiplier,
  allowTrailingMargin = false,
  flavor = 'commonmark',
  streamingAnimation = false,
  ...rest
}) => {
  const normalizedStyleRef = useRef(null);
  const normalized = normalizeMarkdownStyle(markdownStyle);
  // normalizeMarkdownStyle returns cached objects for structurally equal inputs,
  // so this referential check is sufficient to preserve a stable prop reference.
  if (normalizedStyleRef.current !== normalized) {
    normalizedStyleRef.current = normalized;
  }
  const normalizedStyle = normalizedStyleRef.current;
  const normalizedMd4cFlags = useMemo(() => ({
    underline: md4cFlags.underline ?? false,
    latexMath: md4cFlags.latexMath ?? true
  }), [md4cFlags]);
  const handleLinkPress = useCallback(e => {
    const {
      url
    } = e.nativeEvent;
    onLinkPress?.({
      url
    });
  }, [onLinkPress]);
  const handleLinkLongPress = useCallback(e => {
    const {
      url
    } = e.nativeEvent;
    onLinkLongPress?.({
      url
    });
  }, [onLinkLongPress]);
  const handleTaskListItemPress = useCallback(e => {
    const {
      index,
      checked,
      text
    } = e.nativeEvent;
    onTaskListItemPress?.({
      index,
      checked,
      text
    });
  }, [onTaskListItemPress]);
  const sharedProps = {
    markdown,
    markdownStyle: normalizedStyle,
    onLinkPress: handleLinkPress,
    onLinkLongPress: handleLinkLongPress,
    onTaskListItemPress: handleTaskListItemPress,
    enableLinkPreview: onLinkLongPress == null && (enableLinkPreview ?? true),
    selectable,
    md4cFlags: normalizedMd4cFlags,
    allowFontScaling,
    maxFontSizeMultiplier,
    allowTrailingMargin,
    streamingAnimation,
    style: containerStyle,
    ...rest
  };
  if (flavor === 'github') {
    return /*#__PURE__*/_jsx(EnrichedMarkdownNativeComponent, {
      ...sharedProps
    });
  }
  return /*#__PURE__*/_jsx(EnrichedMarkdownTextNativeComponent, {
    ...sharedProps
  });
};
export default EnrichedMarkdownText;
//# sourceMappingURL=EnrichedMarkdownText.js.map