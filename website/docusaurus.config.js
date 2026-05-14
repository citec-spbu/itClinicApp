const config = {
  title: "CiteC Docs",
  tagline: "Актуальная документация по мобильному клиенту CiteC",
  url: "https://citec-spbu.github.io",
  baseUrl: "/CiteC/",
  onBrokenLinks: "throw",
  onBrokenAnchors: "warn",
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: "throw",
    },
  },
  i18n: {
    defaultLocale: "ru",
    locales: ["ru", "en"],
  },
  presets: [
    [
      "classic",
      {
        docs: {
          path: "docs",
          routeBasePath: "docs",
          sidebarPath: require.resolve("./sidebars.js"),
          editUrl: undefined,
        },
        blog: false,
        theme: {
          customCss: require.resolve("./src/css/custom.css"),
        },
      },
    ],
  ],
  themeConfig: {
    navbar: {
      title: "CiteC Docs",
      items: [
        {
          type: "docSidebar",
          sidebarId: "docsSidebar",
          position: "left",
          label: "Docs",
        },
        {
          type: "localeDropdown",
          position: "right",
        },
      ],
    },
    footer: {
      style: "dark",
      links: [
        {
          title: "Документация",
          items: [
            {
              label: "Обзор",
              to: "/docs/overview/product-summary",
            },
            {
              label: "Разработка",
              to: "/docs/developer-guide/quick-start",
            },
          ],
        },
        {
          title: "Архитектура",
          items: [
            {
              label: "Клиентская архитектура",
              to: "/docs/architecture/client-architecture",
            },
            {
              label: "Ranking и Statistics",
              to: "/docs/architecture/ranking-and-stats",
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} CiteC`,
    },
    colorMode: {
      defaultMode: "light",
      disableSwitch: false,
      respectPrefersColorScheme: true,
    },
  },
};

module.exports = config;
