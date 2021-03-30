const {scan} = require('../helpers');

test(
  'gitleaks should find 1 credential in the testfiles',
  async () => {
    const {categories, severities, count} = await scan(
      'gitleaks-dummy-scan',
      'gitleaks',
      ['-r', 'https://github.com/secureCodeBox/secureCodeBox', '--commit=ec0fe179ccf178b56fcd51d1730448bc64bb9ab5', '--config', '/home/config_all.toml'],
      90
    );

    expect(count).toBe(1);
    expect(categories).toEqual(
      {
        'Potential Secret': 1
      }
    );
    expect(severities).toEqual(
      {
        'high': 1
      }
    );
  },
  3 * 60 * 1000
);
