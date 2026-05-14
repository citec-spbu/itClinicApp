import React from "react";
import Link from "@docusaurus/Link";
import Layout from "@theme/Layout";
import Translate, {translate} from "@docusaurus/Translate";
import clsx from "clsx";

const cards = [
  {
    title: "Overview",
    description: translate({
      id: "homepage.card.overview.description",
      message: "Что это за приложение, что уже реализовано и как устроен репозиторий.",
    }),
    to: "/docs/overview/product-summary",
  },
  {
    title: "User Guide",
    description: translate({
      id: "homepage.card.userGuide.description",
      message: "Авторизация, проекты, рейтинг, статистика, профиль и обратная связь.",
    }),
    to: "/docs/user-guide/authentication",
  },
  {
    title: "Screens",
    description: translate({
      id: "homepage.card.screens.description",
      message: "Подробные описания экранов: дизайн, состояния, данные и запросы.",
    }),
    to: "/docs/screens/onboarding",
  },
  {
    title: "Developer Guide",
    description: translate({
      id: "homepage.card.developerGuide.description",
      message: "Локальный запуск, конфигурация, Docker stack, Android/iOS и CI/CD.",
    }),
    to: "/docs/developer-guide/quick-start",
  },
  {
    title: "Architecture",
    description: translate({
      id: "homepage.card.architecture.description",
      message: "Клиентская архитектура, auth flow, networking и статистика.",
    }),
    to: "/docs/architecture/client-architecture",
  },
];

function DocCard({title, description, to}) {
  return (
    <Link className={clsx("docCard")} to={to}>
      <h3>{title}</h3>
      <p>{description}</p>
    </Link>
  );
}

export default function Home() {
  return (
    <Layout
      title="CiteC Docs"
      description={translate({
        id: "homepage.meta.description",
        message: "Актуальная документация по мобильному клиенту CiteC",
      })}>
      <main className="heroMain">
        <section className="heroBlock">
          <div className="container">
            <div className="heroCopy">
              <span className="heroKicker">
                <Translate id="homepage.hero.kicker">Internal documentation</Translate>
              </span>
              <h1>CiteC Docs</h1>
              <p>
                <Translate id="homepage.hero.description">
                  Документация по мобильному клиенту CiteC с фокусом на реальные
                  пользовательские сценарии, экранные flow, код, networking,
                  CI/CD и эксплуатацию.
                </Translate>
              </p>
              <div className="heroActions">
                <Link className="button button--primary button--lg" to="/docs/overview/product-summary">
                  <Translate id="homepage.hero.primaryCta">Открыть документацию</Translate>
                </Link>
                <Link className="button button--secondary button--lg" to="/docs/developer-guide/quick-start">
                  <Translate id="homepage.hero.secondaryCta">Быстрый старт</Translate>
                </Link>
              </div>
            </div>
          </div>
        </section>
        <section className="cardsSection container">
          <div className="cardsGrid">
            {cards.map((card) => (
              <DocCard key={card.title} {...card} />
            ))}
          </div>
        </section>
      </main>
    </Layout>
  );
}
