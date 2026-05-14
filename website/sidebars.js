const sidebars = {
  docsSidebar: [
    {
      type: "category",
      label: "Overview",
      items: [
        "overview/product-summary",
        "overview/current-status",
        "overview/repository-map",
        "overview/documentation-scope",
      ],
    },
    {
      type: "category",
      label: "User Guide",
      items: [
        "user-guide/authentication",
        "user-guide/projects-and-navigation",
        "user-guide/ranking",
        "user-guide/project-statistics",
        "user-guide/profile-settings-feedback",
      ],
    },
    {
      type: "category",
      label: "Screens",
      items: [
        "screens/onboarding",
        "screens/projects",
        "screens/project-detail",
        "screens/ranking",
        "screens/project-statistics",
        "screens/user-statistics",
        "screens/information-and-settings",
      ],
    },
    {
      type: "category",
      label: "Developer Guide",
      items: [
        "developer-guide/quick-start",
        "developer-guide/local-config",
        "developer-guide/local-backend-stack",
        "developer-guide/platform-development",
        "developer-guide/ci-cd",
      ],
    },
    {
      type: "category",
      label: "Architecture",
      items: [
        "architecture/client-architecture",
        "architecture/navigation-and-shell",
        "architecture/auth-flow",
        "architecture/networking",
        "architecture/ranking-and-stats",
        "architecture/analytics-and-observability",
      ],
    },
    {
      type: "category",
      label: "Reference",
      items: [
        "reference/api-endpoints",
        "reference/backend-gaps",
        "reference/update-delivery",
        "reference/troubleshooting",
      ],
    },
  ],
};

module.exports = sidebars;
