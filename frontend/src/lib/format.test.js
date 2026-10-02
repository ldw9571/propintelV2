import { describe, expect, it } from 'vitest';
import { manToWon, pct, pctToRatio, short, won } from './format.js';

describe('format', () => {
  it('억·만 단위로 표시한다', () => {
    expect(won(825_624_000)).toBe('8억 2,562만원');
    expect(won(800_000_000)).toBe('8억원');
    expect(won(25_624_000)).toBe('2,562만원');
    expect(won(-133_432_780)).toBe('-1억 3,343만원');
    expect(won(null)).toBe('-');
  });
  it('입력 단위를 변환한다', () => {
    expect(manToWon(30000)).toBe(300_000_000);
    expect(pctToRatio(4)).toBeCloseTo(0.04);
    expect(pct(0.0233)).toBe('2.33%');
    expect(short(927_419_259)).toBe('9.3억');
  });
});
