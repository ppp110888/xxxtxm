import { describe, it, expect } from 'vitest'
import {
  JUDGE_STATUS_MAP,
  DIFFICULTY_MAP,
  LANGUAGE_OPTIONS,
  PROGRESS_STATUS,
} from '@/utils/constants'

describe('JUDGE_STATUS_MAP', () => {
  const requiredStatuses = ['PENDING', 'COMPILING', 'RUNNING', 'JUDGING', 'AC', 'WA', 'TLE', 'MLE', 'RE', 'CE', 'SE']

  requiredStatuses.forEach((status) => {
    it(`has required key: ${status}`, () => {
      expect(JUDGE_STATUS_MAP).toHaveProperty(status)
    })
  })

  it('each status has label, color, icon, class', () => {
    Object.values(JUDGE_STATUS_MAP).forEach((entry) => {
      expect(entry).toHaveProperty('label')
      expect(entry).toHaveProperty('color')
      expect(entry).toHaveProperty('icon')
      expect(entry).toHaveProperty('class')
      expect(typeof entry.label).toBe('string')
      expect(typeof entry.color).toBe('string')
    })
  })
})

describe('DIFFICULTY_MAP', () => {
  it('has easy, medium, hard keys', () => {
    expect(DIFFICULTY_MAP).toHaveProperty('easy')
    expect(DIFFICULTY_MAP).toHaveProperty('medium')
    expect(DIFFICULTY_MAP).toHaveProperty('hard')
  })

  it('each difficulty has label and color', () => {
    Object.values(DIFFICULTY_MAP).forEach((entry) => {
      expect(entry).toHaveProperty('label')
      expect(entry).toHaveProperty('color')
    })
  })
})

describe('LANGUAGE_OPTIONS', () => {
  it('is a non-empty array', () => {
    expect(Array.isArray(LANGUAGE_OPTIONS)).toBe(true)
    expect(LANGUAGE_OPTIONS.length).toBeGreaterThan(0)
  })

  it('each option has label and value', () => {
    LANGUAGE_OPTIONS.forEach((opt) => {
      expect(opt).toHaveProperty('label')
      expect(opt).toHaveProperty('value')
      expect(typeof opt.label).toBe('string')
      expect(typeof opt.value).toBe('string')
    })
  })

  it('contains Python and Java', () => {
    const values = LANGUAGE_OPTIONS.map(o => o.value)
    expect(values).toContain('python')
    expect(values).toContain('java')
  })
})

describe('PROGRESS_STATUS', () => {
  it('has LOCKED, IN_PROGRESS, CLEARED keys', () => {
    expect(PROGRESS_STATUS).toHaveProperty('LOCKED')
    expect(PROGRESS_STATUS).toHaveProperty('IN_PROGRESS')
    expect(PROGRESS_STATUS).toHaveProperty('CLEARED')
  })

  it('each status has label, color, icon', () => {
    Object.values(PROGRESS_STATUS).forEach((entry) => {
      expect(entry).toHaveProperty('label')
      expect(entry).toHaveProperty('color')
      expect(entry).toHaveProperty('icon')
    })
  })
})
