const { cascadingScan } = require('../helpers')

test(
  "Cascading Scan nmap -> sslyze on unsafe-https",
  async () => {
    const { categories, severities, count } = await cascadingScan(
      "nmap-unsafe-https-sslyze",
      "nmap",
      ["-Pn", "-sV", "unsafe-https"],
      {
        nameCascade: "https-tls-scan",
        matchLabels: {
          "securecodebox.io/invasive": "non-invasive",
          "securecodebox.io/intensive": "light"
        }
      },
      4 * 60
    );

    expect(count).toBe(4);
    expect(categories).toMatchInlineSnapshot(`
      Object {
        "Invalid Certificate": 1,
        "Outdated TLS Version": 2,
        "TLS Service Info": 1,
      }
    `);
    expect(severities).toMatchInlineSnapshot(`
      Object {
        "informational": 1,
        "medium": 3,
      }
    `);
  },
  3 * 60 * 1000
);
