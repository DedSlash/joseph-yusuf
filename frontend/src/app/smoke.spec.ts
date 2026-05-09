describe('CI smoke test', () => {
  it('garantit que Karma + Jasmine sont opérationnels', () => {
    expect(true).toBe(true);
  });

  it('arithmétique de base', () => {
    expect(1 + 1).toBe(2);
  });
});
