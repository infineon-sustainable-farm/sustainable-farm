// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useLater } from './useLater';

afterEach(() => vi.useRealTimers());

describe('useLater', () => {
  it('runs the callback after the delay', () => {
    vi.useFakeTimers();
    const fn = vi.fn();
    const { result } = renderHook(() => useLater());
    result.current(fn, 4000);
    vi.advanceTimersByTime(3999);
    expect(fn).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it('cancels the pending callbacks when the screen closes', () => {
    vi.useFakeTimers();
    const fn = vi.fn();
    const { result, unmount } = renderHook(() => useLater());
    result.current(fn, 4000);
    unmount();
    vi.advanceTimersByTime(5000);
    expect(fn).not.toHaveBeenCalled();
  });
});
