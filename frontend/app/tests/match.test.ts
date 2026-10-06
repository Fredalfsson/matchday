import { describe, it, expect } from 'vitest';
import { isMatch } from '../utils/matchUtils';
import { MatchStatus } from '../lib/interfaces/matchInterface';
import MOCK_MATCH from '../_MOCKDATA/MOCK_MATCH.json';
describe('isMatch type guard', () => {
  const validMatch = MOCK_MATCH;

  it('returns true if value is a valid match', () => {
    expect(isMatch(validMatch)).toBe(true);
  });

  it('returns true when nullable fields are null', () => {
    const matchWithNulls = {
      ...validMatch,
      kickoffAt: null,
      homeScore: null,
      awayScore: null,
      venueName: null,
    };

    expect(isMatch(matchWithNulls)).toBe(true);
  });

  describe('isMatch', () => {
    it.each(Object.values(MatchStatus))(
      'returns true for status: %s',
      (status) => {
        expect(isMatch({ ...validMatch, status })).toBe(true);
      },
    );
  });

  describe('Non valid values', () => {
    it('returns false for primitive types and null', () => {
      expect(isMatch(null)).toBe(false);
      expect(isMatch(undefined)).toBe(false);
      expect(isMatch('en sträng')).toBe(false);
      expect(isMatch(123)).toBe(false);
      expect(isMatch(true)).toBe(false);
    });

    it('returns false if a required field is missing or the wrong type', () => {
      // id missing or wrong type
      expect(isMatch({ ...validMatch, id: 123 })).toBe(false);

      // seson is a string instead of number
      expect(isMatch({ ...validMatch, season: '2024' })).toBe(false);

      // homeTeamName missing
      const { homeTeamName, ...missingHomeTeam } = validMatch;
      expect(isMatch(missingHomeTeam)).toBe(false);
    });

    it('returns false if a nullable field is the wrong type (f.ex undefined instead of null)', () => {
      // homeScore is undefined instead of number/null
      expect(isMatch({ ...validMatch, homeScore: undefined })).toBe(false);

      // kickoffAt is a number
      expect(isMatch({ ...validMatch, kickoffAt: 1600000000 })).toBe(false);
    });

    it('sreturns false for non-valid status strings', () => {
      expect(isMatch({ ...validMatch, status: 'CANCELLED' })).toBe(false);
      expect(isMatch({ ...validMatch, status: 'scheduled' })).toBe(false); // Small letters
      expect(isMatch({ ...validMatch, status: null })).toBe(false);
    });
  });
});
